<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { createApiLogStore } from '../api/apiLogStore';
import { createHttpClient } from '../api/httpClient';
import { createTrainingApi } from '../api/trainingApiFactory';
import { createAuthenticatedApiClient } from '../api/v1/authenticatedApiClient';
import { createWorkflowDefinitionApi } from '../api/v1/workflowDefinitionApi';
import ApiLogPanel from '../components/ApiLogPanel.vue';
import BusinessFrame from '../components/BusinessFrame.vue';
import CaptureToolbar from '../components/CaptureToolbar.vue';
import GuideImageEditor from '../components/GuideImageEditor.vue';
import SegmentFlowPanel from '../components/SegmentFlowPanel.vue';
import SegmentSwitchPanel from '../components/SegmentSwitchPanel.vue';
import TeachingOverlay from '../components/TeachingOverlay.vue';
import { trainingConfig } from '../config/trainingConfig';
import {
  createBusinessActionSignature,
  shouldSkipDuplicateBusinessAction,
  type LastBusinessActionSignature
} from '../engine/businessActionDeduper';
import {
  getVisibleOverlayStep,
  isSameBusinessPage
} from '../engine/businessPageState';
import type {
  BusinessReadyMessage,
  ElementPickedMessage
} from '../engine/captureProtocol';
import {
  publishRecordedStage,
  publishTeachingPoint,
  reportAction,
  saveSegment,
  startCapture,
  switchSegment,
  type RecordedStageBinding,
  type SwitchSegmentInput
} from '../engine/captureOrchestrator';
import {
  createCaptureWorkspace,
  flattenRecordedSteps,
  getActiveSegment,
  getCaptureCommandState,
  insertGuideStep,
  moveStepWithinSegment,
  patchStep,
  truncateSegmentFromStep,
  undoLastLocalStep
} from '../engine/captureWorkspace';
import {
  completionMethodOptions
} from '../engine/displayLabels';
import { prepareGuideImage } from '../engine/guideImage';
import {
  clearPendingRecordedStageWriteback,
  loadPendingRecordedStageWriteback,
  savePendingRecordedStageWriteback,
  savePublishedRecordedFlow,
  type PendingRecordedStageWriteback
} from '../engine/recordingStorage';
import { createEmptyRecordedFlow } from '../mock/lessonFlows';
import type {
  BusinessActionPayload,
  CompletionMethod,
  Rect,
  RecordedFlow,
  RecordedStep
} from '../types/domain';
import type {
  TaskWorkflowDraft,
  WorkflowStageInput
} from '../types/workflow';

const initialFlow = createEmptyRecordedFlow() as RecordedFlow;

const route = useRoute();
const router = useRouter();
const apiLogStore = createApiLogStore();
const sharedHttpClient = createHttpClient(trainingConfig);
const api = createTrainingApi(
  trainingConfig,
  apiLogStore,
  window.localStorage,
  sharedHttpClient
);
const workflowApi = createWorkflowDefinitionApi(
  createAuthenticatedApiClient(trainingConfig, apiLogStore, {
    httpClient: sharedHttpClient
  })
);
const workflowDraftId = computed(() =>
  String(route.params.workflowDraftId ?? '')
);
const stageId = computed(() => String(route.params.stageId ?? ''));
const isStageRecording = computed(
  () => Boolean(workflowDraftId.value && stageId.value)
);
const workflowContext = ref<{
  draft: TaskWorkflowDraft;
  stage: WorkflowStageInput;
}>();

const effectiveTaskId = computed(() =>
  workflowContext.value?.draft.taskId ?? ''
);

async function openWorkflowDesigner() {
  await router.push(
    effectiveTaskId.value
      ? `/teacher/tasks/${encodeURIComponent(effectiveTaskId.value)}/workflow`
      : '/teacher/tasks/'
  );
}

async function openRunBatch(kind: 'exam' | 'practice') {
  await router.push({
    path:
      kind === 'exam' ? '/teacher/exams/new' : '/teacher/practices/new',
    query: effectiveTaskId.value ? { taskId: effectiveTaskId.value } : {}
  });
}
const pendingStageBinding = ref<PendingRecordedStageWriteback>();
const workspace = ref(createCaptureWorkspace(initialFlow));
const businessFrameRef = ref<InstanceType<typeof BusinessFrame> | null>(null);
const isRecording = ref(false);
const busyAction = ref('');
const statusText = ref('点击“开始备案”后先创建采集会话，再开始记录业务操作。');
const showStepPanel = ref(
  typeof window === 'undefined' ? true : window.innerWidth > 760
);
const showEditorPanel = ref(false);
const showApiLog = ref(false);
const showSegmentSwitch = ref(false);
const controlsCollapsed = ref(false);
const latestBusinessUrl = ref('');
const runtimeSelectedRect = ref<Rect>();
const lastRecordedAction = ref<LastBusinessActionSignature>();
const isGuideImageProcessing = ref(false);
let actionQueue = Promise.resolve();
let resizeTimer: number | undefined;
let resolutionGeneration = 0;

interface ReadyWaiter {
  targetUrl?: string;
  resolve: () => void;
  reject: (error: Error) => void;
  timer: number;
}

const readyWaiters: ReadyWaiter[] = [];

const segments = computed(() => workspace.value.flow.segments ?? []);
const activeSegment = computed(() => getActiveSegment(workspace.value));
const selectedStep = computed(() =>
  flattenRecordedSteps(workspace.value.flow).find(
    (step) => step.id === workspace.value.selectedStepId
  )
);
const selectedOverlayStep = computed<RecordedStep | undefined>(() => {
  const step = selectedStep.value;
  return getVisibleOverlayStep({
    step,
    frameReady: Boolean(latestBusinessUrl.value),
    currentUrl: latestBusinessUrl.value,
    runtimeRect: runtimeSelectedRect.value
  });
});
const baseCommandState = computed(() => getCaptureCommandState(workspace.value));
const isWorkflowReadOnly = computed(
  () => Boolean(
    isStageRecording.value
    && workflowContext.value
    && workflowContext.value.draft.versionStatus !== 'DRAFT'
  )
);
const editableToolbarCommandState = computed(() => ({
  ...baseCommandState.value,
  canPause: isRecording.value && baseCommandState.value.canPause,
  canResume:
    !isRecording.value &&
    workspace.value.sessionStatus === 'running' &&
    Boolean(workspace.value.session),
  canInsertGuide:
    Boolean(latestBusinessUrl.value) && baseCommandState.value.canInsertGuide,
  canSwitchSegment:
    !isStageRecording.value && baseCommandState.value.canSwitchSegment,
  canPublish:
    Boolean(pendingStageBinding.value) || baseCommandState.value.canPublish
}));
const toolbarCommandState = computed(() => {
  if (isWorkflowReadOnly.value) {
    return {
      canStart: false,
      canPause: false,
      canResume: false,
      canUndo: false,
      canInsertGuide: false,
      canRerecord: false,
      canSaveSegment: false,
      canSwitchSegment: false,
      canPublish: false
    };
  }
  return editableToolbarCommandState.value;
});

function ensureWorkflowEditable() {
  if (isWorkflowReadOnly.value) {
    throw new Error('已发布工作流为只读版本，请返回编排页创建新草稿');
  }
}

async function runBusy(key: string, operation: () => Promise<void>) {
  if (busyAction.value) return;
  busyAction.value = key;
  try {
    await operation();
  } catch (error) {
    statusText.value = error instanceof Error ? error.message : '操作失败';
  } finally {
    busyAction.value = '';
  }
}

function handleBusinessReady(payload: BusinessReadyMessage) {
  latestBusinessUrl.value = payload.url;
  businessFrameRef.value?.setRecording(
    isRecording.value,
    workspace.value.session?.id,
    maxSequenceNo()
  );

  for (let index = readyWaiters.length - 1; index >= 0; index -= 1) {
    const waiter = readyWaiters[index];
    if (
      !waiter.targetUrl ||
      isSameBusinessPage(waiter.targetUrl, payload.url)
    ) {
      window.clearTimeout(waiter.timer);
      readyWaiters.splice(index, 1);
      waiter.resolve();
    }
  }
  void resolveSelectedTarget();
}

function invalidateBusinessFrame() {
  latestBusinessUrl.value = '';
  runtimeSelectedRect.value = undefined;
  resolutionGeneration += 1;
}

async function resolveSelectedTarget(): Promise<void> {
  const step = selectedStep.value;
  runtimeSelectedRect.value = undefined;
  if (!step || !latestBusinessUrl.value) return;
  if (step.kind === 'guide' && step.anchor?.mode !== 'element') return;
  if (!isSameBusinessPage(latestBusinessUrl.value, step.url)) return;

  const generation = ++resolutionGeneration;
  try {
    const result = await businessFrameRef.value?.resolveStepTarget(step);
    if (
      result?.rect &&
      generation === resolutionGeneration &&
      selectedStep.value?.id === step.id
    ) {
      runtimeSelectedRect.value = result.rect;
    }
  } catch {
    if (generation === resolutionGeneration) {
      statusText.value = `当前页面未找到节点元素：${step.title}`;
    }
  }
}

function handleViewportResize() {
  if (resizeTimer) window.clearTimeout(resizeTimer);
  resizeTimer = window.setTimeout(() => void resolveSelectedTarget(), 180);
}

function waitForBusinessReady(targetUrl?: string): Promise<void> {
  if (
    latestBusinessUrl.value &&
    (!targetUrl || isSameBusinessPage(targetUrl, latestBusinessUrl.value))
  ) {
    return Promise.resolve();
  }
  return new Promise((resolve, reject) => {
    const waiter: ReadyWaiter = {
      targetUrl,
      resolve,
      reject,
      timer: window.setTimeout(() => {
        const index = readyWaiters.indexOf(waiter);
        if (index >= 0) readyWaiters.splice(index, 1);
        reject(new Error(`业务页面未在 5 秒内就绪${targetUrl ? `：${targetUrl}` : ''}`));
      }, 5000)
    };
    readyWaiters.push(waiter);
  });
}

async function startRecording() {
  await runBusy('start', async () => {
    ensureWorkflowEditable();
    workspace.value = { ...workspace.value, sessionStatus: 'creating' };
    try {
      workspace.value = await startCapture(
        api,
        trainingConfig,
        workspace.value
      );
    } catch (error) {
      workspace.value = { ...workspace.value, sessionStatus: 'error' };
      throw error;
    }
    isRecording.value = true;
    businessFrameRef.value?.setRecording(
      true,
      workspace.value.session?.id,
      maxSequenceNo()
    );
    statusText.value = `采集会话已创建：${workspace.value.session?.id}，正在录制第 1 段。`;
  });
}

function pauseRecording() {
  isRecording.value = false;
  businessFrameRef.value?.setRecording(false, workspace.value.session?.id);
  statusText.value = '录制已暂停，可编辑节点、保存分段或调整顺序。';
}

function resumeRecording() {
  if (!workspace.value.session) return;
  isRecording.value = true;
  businessFrameRef.value?.setRecording(
    true,
    workspace.value.session.id,
    maxSequenceNo()
  );
  statusText.value = `继续录制第 ${activeSegment.value?.segmentNo ?? 1} 段。`;
}

function handleBusinessAction(payload: BusinessActionPayload) {
  if (!isRecording.value || !workspace.value.session) {
    statusText.value = `检测到业务操作“${payload.text}”，开始备案后才会生成采集节点。`;
    return;
  }

  const now = Date.now();
  if (shouldSkipDuplicateBusinessAction(payload, lastRecordedAction.value, now)) {
    return;
  }
  lastRecordedAction.value = {
    signature: createBusinessActionSignature(payload),
    at: now
  };

  actionQueue = actionQueue.then(async () => {
    workspace.value = await reportAction(
      api,
      trainingConfig,
      workspace.value,
      payload
    );
    const step = selectedStep.value;
    statusText.value = step?.persistence?.status === 'error'
      ? `节点已保留，但接口上报失败：${step.persistence.errorMessage}`
      : `已采集并上报：${step?.title ?? payload.text}`;
  });
}

function selectSegment(segmentId: string) {
  const segment = segments.value.find((item) => item.id === segmentId);
  if (!segment) return;
  workspace.value = {
    ...workspace.value,
    activeSegmentId: segmentId,
    selectedStepId: segment.steps[0]?.id
  };
  if (!isSameBusinessPage(latestBusinessUrl.value, segment.targetUrl)) {
    invalidateBusinessFrame();
    businessFrameRef.value?.navigate(segment.targetUrl);
  }
}

function selectStep(segmentId: string, stepId: string) {
  workspace.value = {
    ...workspace.value,
    activeSegmentId: segmentId,
    selectedStepId: stepId
  };
}

function updateSelectedStep(patch: Partial<RecordedStep>) {
  ensureWorkflowEditable();
  if (!selectedStep.value) return;
  workspace.value = patchStep(workspace.value, selectedStep.value.id, patch);
}

async function handleGuideImageSelected(file: File) {
  const stepId =
    selectedStep.value?.kind === 'guide' ? selectedStep.value.id : undefined;
  if (!stepId || isGuideImageProcessing.value) return;

  isGuideImageProcessing.value = true;
  statusText.value = '正在处理说明图片…';
  try {
    const guideImage = await prepareGuideImage(file);
    workspace.value = patchStep(workspace.value, stepId, { guideImage });
    statusText.value = '说明图片已添加，可在当前弹框中预览。';
  } catch (error) {
    statusText.value =
      error instanceof Error ? error.message : '说明图片处理失败。';
  } finally {
    isGuideImageProcessing.value = false;
  }
}

function removeGuideImage() {
  if (selectedStep.value?.kind !== 'guide') return;
  updateSelectedStep({ guideImage: undefined });
  statusText.value = '说明图片已删除。';
}

function updateCompletionMethod(value: string) {
  updateSelectedStep({ completionMethod: value as CompletionMethod });
}

function moveSelectedStep(direction: 'up' | 'down') {
  if (!selectedStep.value) return;
  workspace.value = moveStepWithinSegment(
    workspace.value,
    selectedStep.value.id,
    direction
  );
}

function undoStep() {
  const previousCount = activeSegment.value?.steps.length ?? 0;
  workspace.value = undoLastLocalStep(workspace.value);
  statusText.value =
    (activeSegment.value?.steps.length ?? 0) < previousCount
      ? '已撤销当前分段最后一个本地节点。'
      : '最后一个节点已经上报或确认，不能直接撤销。';
}

function addGuideStep() {
  if (!latestBusinessUrl.value) {
    statusText.value = '业务页面正在加载，请等待页面就绪后再插入说明节点。';
    return;
  }
  workspace.value = insertGuideStep(
    workspace.value,
    workspace.value.selectedStepId,
    latestBusinessUrl.value
  );
  showEditorPanel.value = true;
  statusText.value = '已插入说明节点，可编辑文字、上传图片或绑定页面元素。';
}

function bindGuideElement() {
  if (selectedStep.value?.kind !== 'guide') return;
  businessFrameRef.value?.startElementPick();
  statusText.value = '元素选取已开启，请点击业务页面中需要绑定说明的元素。';
}

function handleElementPicked(payload: ElementPickedMessage) {
  if (selectedStep.value?.kind !== 'guide') return;
  updateSelectedStep({
    url: payload.url,
    selector: payload.selector,
    selectorCandidates: payload.selectorCandidates,
    text: payload.text,
    rect: payload.rect,
    anchor: {
      mode: 'element',
      selector: payload.selector,
      selectorCandidates: payload.selectorCandidates,
      rect: payload.rect
    },
    recordedViewport: payload.recordedViewport
  });
  resolutionGeneration += 1;
  runtimeSelectedRect.value = payload.rect;
  statusText.value = `说明节点已绑定元素：${payload.text || payload.selector}`;
}

async function rerecordFromSelected() {
  ensureWorkflowEditable();
  const step = selectedStep.value;
  const segment = activeSegment.value;
  if (!step || !segment) return;
  await runBusy('rerecord', async () => {
    const index = segment.steps.findIndex((item) => item.id === step.id);
    const priorSteps = segment.steps
      .slice(0, index)
      .filter((item) => item.kind !== 'guide');
    pauseRecording();
    workspace.value = truncateSegmentFromStep(workspace.value, step.id);
    invalidateBusinessFrame();
    const resetReady = waitForBusinessReady(workspace.value.flow.businessUrl);
    businessFrameRef.value?.reload(workspace.value.flow.businessUrl);
    await resetReady;

    for (const priorStep of priorSteps) {
      if (!isSameBusinessPage(latestBusinessUrl.value, priorStep.url)) {
        invalidateBusinessFrame();
        const pageReady = waitForBusinessReady(priorStep.url);
        businessFrameRef.value?.navigate(priorStep.url);
        await pageReady;
      }
      await businessFrameRef.value?.playStep(priorStep);
    }
    resumeRecording();
    statusText.value = `已回放 ${priorSteps.length} 个前置节点，可从当前位置继续录制。`;
  });
}

async function saveCurrentSegment() {
  ensureWorkflowEditable();
  const segment = activeSegment.value;
  if (!segment) return;
  await runBusy('save', async () => {
    pauseRecording();
    await actionQueue;
    workspace.value = await saveSegment(
      api,
      trainingConfig,
      workspace.value,
      segment.id
    );
    statusText.value = `备案第 ${segment.segmentNo} 段已保存，共 ${segment.steps.length} 个动作草稿。`;
  });
}

function openSegmentSwitch() {
  if (isStageRecording.value) {
    statusText.value = '当前为单阶段录制，请返回工作流编排页配置下一业务阶段。';
    return;
  }
  showSegmentSwitch.value = true;
  pauseRecording();
}

async function confirmSegmentSwitch(input: SwitchSegmentInput) {
  await runBusy('switch', async () => {
    await actionQueue;
    workspace.value = await switchSegment(
      api,
      trainingConfig,
      workspace.value,
      input
    );
    showSegmentSwitch.value = false;
    invalidateBusinessFrame();
    const pageReady = waitForBusinessReady(input.targetUrl);
    businessFrameRef.value?.navigate(input.targetUrl);
    await pageReady;
    resumeRecording();
    statusText.value = `已进入备案第 ${input.nextSegmentNo} 段：${input.requiredExternalRoleName || input.requiredExternalRoleId}`;
  });
}

async function publishFlow() {
  await runBusy('publish', async () => {
    ensureWorkflowEditable();
    pauseRecording();
    await actionQueue;

    if (isStageRecording.value) {
      if (pendingStageBinding.value) {
        await saveRecordedStageBinding(pendingStageBinding.value);
        return;
      }

      const context = workflowContext.value;
      if (!context) {
        throw new Error('工作流阶段尚未加载，请刷新页面后重试。');
      }
      const result = await publishRecordedStage(
        api,
        trainingConfig,
        workspace.value,
        {
          workflowDraftId: workflowDraftId.value,
          stageId: stageId.value,
          taskId: context.draft.taskId,
          stageSequenceNo: context.stage.sequenceNo,
          stageMaxScore: context.stage.stageMaxScore,
          actorType: context.stage.stageKey,
          externalOrgId: context.stage.externalOrgId,
          externalRoleId: context.stage.externalRoleId,
          pointCode: `STAGE_${context.stage.stageKey.slice(0, 32)}_${Date.now()}`,
          pointName: `${context.draft.name} - ${context.stage.name}`,
          description: `工作流阶段 ${context.stage.sequenceNo} 录制资产`
        }
      );
      workspace.value = result.workspace;
      savePublishedRecordedFlow(workspace.value.flow);
      const pending = createPendingStageWriteback(context.draft, result.binding);
      pendingStageBinding.value = savePendingRecordedStageWriteback(
        pending,
        window.localStorage
      );
      await saveRecordedStageBinding(pending);
      return;
    }

    const result = await publishTeachingPoint(
      api,
      trainingConfig,
      workspace.value,
      {
        pointCode: `POINT_${Date.now()}`,
        pointName: workspace.value.flow.title,
        description: '由录制式教案采集工作台发布'
      }
    );
    workspace.value = result.workspace;
    savePublishedRecordedFlow(workspace.value.flow);
    statusText.value = `教学点发布成功：${result.teachingPoint.id}，采集会话已结束。`;
  });
}

function createPendingStageWriteback(
  draft: TaskWorkflowDraft,
  binding: RecordedStageBinding
): PendingRecordedStageWriteback {
  const clientRequestId = workflowApi.createClientRequestId();
  const stages = draft.stages.map((stage) =>
    stage.id === binding.stageId
      ? {
          ...stage,
          steps: binding.steps.map((step) => ({
            id: `binding-${step.taskStepId}`,
            taskStepId: step.taskStepId,
            recordedSegmentId: step.recordedSegmentId,
            recordedAssetVersion: step.recordedAssetVersion,
            sequenceNo: step.sequenceNo,
            stepKind: step.required ? 'REQUIRED' as const : 'OPTIONAL' as const,
            completionMethod: step.completionMethod,
            failurePolicy: step.failurePolicy,
            score: step.score,
            matcher: step.matcher
          }))
        }
      : stage
  );
  return {
    workflowDraftId: binding.workflowDraftId,
    stageId: binding.stageId,
    taskId: draft.taskId,
    clientRequestId,
    binding,
    request: {
      clientRequestId,
      expectedLockVersion: draft.lockVersion,
      name: draft.name,
      teachingDataTemplateId: draft.teachingDataTemplateId,
      dataScenarioVersion: draft.dataScenarioVersion,
      dataGenerationDefaults: draft.dataGenerationDefaults,
      objectiveMaxScore: draft.objectiveMaxScore,
      subjectiveMaxScore: draft.subjectiveMaxScore,
      roleGroups: draft.roleGroups,
      stages,
      rubricItems: draft.rubricItems
    }
  };
}

async function saveRecordedStageBinding(
  pending: PendingRecordedStageWriteback
) {
  const saved = await workflowApi.saveDraft(pending.taskId, pending.request);
  clearPendingRecordedStageWriteback(
    pending.workflowDraftId,
    pending.stageId,
    window.localStorage
  );
  pendingStageBinding.value = undefined;
  statusText.value = `阶段“${workflowContext.value?.stage.name ?? pending.stageId}”录制资产已写入工作流草稿。`;
  await router.push(`/teacher/tasks/${encodeURIComponent(saved.taskId)}/workflow?stageRecorded=${encodeURIComponent(pending.stageId)}`);
}

async function loadStageContext() {
  if (!isStageRecording.value) return;
  const draft = await workflowApi.getVersion(workflowDraftId.value);
  const stage = draft.stages.find((item) => item.id === stageId.value);
  if (!stage) throw new Error('工作流草稿中不存在指定业务阶段。');

  workflowContext.value = { draft, stage };
  pendingStageBinding.value = loadPendingRecordedStageWriteback(
    workflowDraftId.value,
    stageId.value,
    window.localStorage
  );
  const businessUrl = String(
    stage.launchConfig.path || workspace.value.flow.businessUrl
  );
  const firstSegment = workspace.value.flow.segments?.[0];
  workspace.value = {
    ...workspace.value,
    flow: {
      ...workspace.value.flow,
      title: `${draft.name} - ${stage.name}`,
      businessUrl,
      segments: firstSegment
        ? [{
            ...firstSegment,
            title: stage.name,
            segmentNo: stage.sequenceNo,
            actorType: stage.stageKey,
            externalOrgId: stage.externalOrgId,
            externalRoleId: stage.externalRoleId,
            targetUrl: businessUrl
          }]
        : []
    }
  };
  statusText.value = pendingStageBinding.value
    ? '检测到上次响应不确定的阶段回写，点击发布可使用原请求编号重试。'
    : draft.versionStatus === 'DRAFT'
      ? `正在录制第 ${stage.sequenceNo} 阶段“${stage.name}”，完成后将步骤写回工作流草稿。`
      : '当前为已发布工作流版本，仅可查看；请返回编排页创建新草稿后再录制。';
}

async function retryFailedStep() {
  await saveCurrentSegment();
}

function maxSequenceNo() {
  return flattenRecordedSteps(workspace.value.flow).reduce(
    (max, step) => Math.max(max, step.sequenceNo ?? 0),
    0
  );
}

watch(
  () => selectedStep.value?.id,
  () => {
    resolutionGeneration += 1;
    runtimeSelectedRect.value = undefined;
    void resolveSelectedTarget();
  },
  { flush: 'post' }
);

onMounted(() => {
  window.addEventListener('resize', handleViewportResize);
  void loadStageContext().catch((error) => {
    statusText.value = error instanceof Error ? error.message : '阶段加载失败';
  });
});

onBeforeUnmount(() => {
  for (const waiter of readyWaiters) {
    window.clearTimeout(waiter.timer);
    waiter.reject(new Error('编排页面已关闭'));
  }
  readyWaiters.splice(0);
  if (resizeTimer) window.clearTimeout(resizeTimer);
  window.removeEventListener('resize', handleViewportResize);
});
</script>

<template>
  <section class="immersive-workspace capture-workspace">
    <main class="immersive-business-layer">
      <BusinessFrame
        ref="businessFrameRef"
        :src="workspace.flow.businessUrl"
        title="采购申请业务系统"
        @business-action="handleBusinessAction"
        @business-ready="handleBusinessReady"
        @frame-navigating="invalidateBusinessFrame"
        @frame-load="invalidateBusinessFrame"
        @element-picked="handleElementPicked"
      />
      <TeachingOverlay
        :step="selectedOverlayStep"
        :visible="Boolean(selectedOverlayStep)"
        :show-hint="selectedStep?.kind === 'guide'"
        compact
      />
      <div v-if="isWorkflowReadOnly" class="workflow-read-only-shield">
        <strong>已发布版本仅供查看</strong>
        <span>需要修改或重新录制时，请返回编排页创建新草稿。</span>
      </div>
    </main>

    <button type="button" class="floating-collapse-toggle" @click="controlsCollapsed = !controlsCollapsed">
      {{ controlsCollapsed ? '展开操作' : '隐藏全部' }}
    </button>

    <CaptureToolbar
      v-if="!controlsCollapsed"
      :state="toolbarCommandState"
      :recording="isRecording"
      :busy-action="busyAction"
      :api-mode="trainingConfig.apiMode"
      :session-id="workspace.session?.id"
      @start="startRecording"
      @pause="pauseRecording"
      @resume="resumeRecording"
      @undo="undoStep"
      @insert-guide="addGuideStep"
      @rerecord="rerecordFromSelected"
      @save-segment="saveCurrentSegment"
      @switch-segment="openSegmentSwitch"
      @publish="publishFlow"
    />

    <div v-if="!controlsCollapsed" class="floating-glass floating-status capture-status">
      <span>{{ statusText }}</span>
      <span>
        {{ workspace.sessionStatus === 'finished' ? '已结束' : isRecording ? '录制中' : '已暂停' }} ·
        {{ trainingConfig.apiMode.toUpperCase() }} ·
        第 {{ activeSegment?.segmentNo ?? 1 }} 段 ·
        {{ flattenRecordedSteps(workspace.flow).length }} 节点
      </span>
    </div>

    <div v-if="!controlsCollapsed" class="floating-glass floating-quickbar capture-quickbar">
      <button type="button" class="secondary-action" @click="showStepPanel = !showStepPanel">
        {{ showStepPanel ? '隐藏分段' : '显示分段' }}
      </button>
      <button type="button" class="secondary-action" @click="showEditorPanel = !showEditorPanel">
        {{ showEditorPanel ? '隐藏配置' : '节点配置' }}
      </button>
      <button type="button" class="secondary-action" @click="showApiLog = !showApiLog">
        {{ showApiLog ? '隐藏日志' : '接口日志' }}
      </button>
      <button
        type="button"
        class="secondary-action workflow-designer-entry"
        @click="openWorkflowDesigner"
      >
        工作流编排
      </button>
      <button
        type="button"
        class="secondary-action run-batch-entry"
        @click="openRunBatch('exam')"
      >
        创建考试批次
      </button>
      <button
        type="button"
        class="secondary-action run-batch-entry"
        @click="openRunBatch('practice')"
      >
        创建练习轮次
      </button>
    </div>

    <SegmentFlowPanel
      v-if="!controlsCollapsed && showStepPanel"
      :segments="segments"
      :active-segment-id="workspace.activeSegmentId"
      :selected-step-id="workspace.selectedStepId"
      @select-segment="selectSegment"
      @select-step="selectStep"
      @retry="retryFailedStep"
    />

    <aside
      v-if="!controlsCollapsed && selectedStep && showEditorPanel"
      class="floating-glass floating-editor-panel step-editor capture-step-editor"
      :inert="isWorkflowReadOnly ? true : undefined"
    >
      <div class="panel-header">
        <div>
          <p class="eyebrow">{{ selectedStep.kind === 'guide' ? '说明节点' : '动作节点' }}</p>
          <h3>节点配置</h3>
        </div>
        <div class="mini-actions">
          <button type="button" @click="moveSelectedStep('up')">上移</button>
          <button type="button" @click="moveSelectedStep('down')">下移</button>
        </div>
      </div>
      <label>
        节点名称
        <input :value="selectedStep.title" @input="updateSelectedStep({ title: ($event.target as HTMLInputElement).value })" />
      </label>
      <label>
        学习说明
        <textarea :value="selectedStep.teachingText" rows="4" @input="updateSelectedStep({ teachingText: ($event.target as HTMLTextAreaElement).value })" />
      </label>
      <label>
        练习提示
        <textarea :value="selectedStep.practiceHint" rows="3" @input="updateSelectedStep({ practiceHint: ($event.target as HTMLTextAreaElement).value })" />
      </label>
      <label>
        考试目标
        <textarea :value="selectedStep.examGoal" rows="2" @input="updateSelectedStep({ examGoal: ($event.target as HTMLTextAreaElement).value })" />
      </label>

      <template v-if="selectedStep.kind === 'guide'">
        <GuideImageEditor
          :image="selectedStep.guideImage"
          :busy="isGuideImageProcessing"
          @file-selected="handleGuideImageSelected"
          @remove="removeGuideImage"
        />
        <label>
          弹框锚定
          <select :value="selectedStep.anchor?.mode ?? 'center'" @change="updateSelectedStep({ anchor: { ...selectedStep.anchor, mode: ($event.target as HTMLSelectElement).value as 'element' | 'viewport' | 'center' } })">
            <option value="center">屏幕居中</option>
            <option value="element">页面元素</option>
            <option value="viewport">屏幕方位</option>
          </select>
        </label>
        <button type="button" class="secondary-action bind-element-button" @click="bindGuideElement">点击业务页面绑定元素</button>
      </template>

      <template v-else>
        <label>页面地址<input :value="selectedStep.url" @input="updateSelectedStep({ url: ($event.target as HTMLInputElement).value })" /></label>
        <label>元素定位<input :value="selectedStep.selector" @input="updateSelectedStep({ selector: ($event.target as HTMLInputElement).value })" /></label>
        <label>
          完成方式
          <select :value="selectedStep.completionMethod" @change="updateCompletionMethod(($event.target as HTMLSelectElement).value)">
            <option v-for="option in completionMethodOptions" :key="option.value" :value="option.value">{{ option.label }}</option>
          </select>
        </label>
      </template>

      <div class="editor-grid-two">
        <label>
          继续方式
          <select :value="selectedStep.advanceMode ?? 'manual'" @change="updateSelectedStep({ advanceMode: ($event.target as HTMLSelectElement).value as 'manual' | 'auto' })">
            <option value="manual">手动继续</option>
            <option value="auto">自动继续</option>
          </select>
        </label>
        <label>
          展示时长(ms)
          <input type="number" min="500" step="100" :value="selectedStep.durationMs ?? 2600" @input="updateSelectedStep({ durationMs: Number(($event.target as HTMLInputElement).value) })" />
        </label>
      </div>
      <label>
        失败策略
        <select :value="selectedStep.failurePolicy ?? 'stop'" @change="updateSelectedStep({ failurePolicy: ($event.target as HTMLSelectElement).value as 'stop' | 'retry' | 'skip' })">
          <option value="stop">停止等待处理</option>
          <option value="retry">自动重试</option>
          <option value="skip">允许跳过</option>
        </select>
      </label>
      <label class="checkbox-line">
        <input type="checkbox" :checked="selectedStep.required ?? true" @change="updateSelectedStep({ required: ($event.target as HTMLInputElement).checked })" />
        必做节点
      </label>
      <div v-if="selectedStep.value" class="recorded-value"><strong>录制值</strong><span>{{ selectedStep.value }}</span></div>
      <div class="persistence-summary">
        <span>接口状态</span>
        <strong>{{ selectedStep.persistence?.status ?? 'local' }}</strong>
        <small v-if="selectedStep.persistence?.actionDraftId">{{ selectedStep.persistence.actionDraftId }}</small>
      </div>
    </aside>

    <SegmentSwitchPanel
      v-if="!controlsCollapsed && showSegmentSwitch"
      :current-segment-no="activeSegment?.segmentNo ?? 1"
      :busy="busyAction === 'switch'"
      @submit="confirmSegmentSwitch"
      @cancel="showSegmentSwitch = false"
    />

    <ApiLogPanel
      v-if="!controlsCollapsed && showApiLog"
      :entries="apiLogStore.entries.value"
      @clear="apiLogStore.clear"
      @close="showApiLog = false"
    />
  </section>
</template>

<style scoped>
.workflow-read-only-shield {
  position: absolute;
  z-index: 30;
  inset: 0;
  display: grid;
  place-content: center;
  gap: 8px;
  padding: 24px;
  color: #78350f;
  text-align: center;
  background: rgb(255 251 235 / 78%);
  backdrop-filter: blur(2px);
}

.workflow-read-only-shield strong {
  font-size: 20px;
}
</style>
