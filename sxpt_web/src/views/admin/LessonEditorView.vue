<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch
} from 'vue';
import { useRoute } from 'vue-router';
import type {
  BusinessPageSnapshot,
  CaptureRect,
  CompletionMethod,
  LessonStage,
  RecordedStep,
  RunMode
} from '../../domain/models';
import BusinessCaptureFrame from '../../components/lesson/BusinessCaptureFrame.vue';
import { useAuthoringBusinessLaunch } from '../../composables/useAuthoringBusinessLaunch';
import { useTrainingStore } from '../../stores/trainingStore';
import {
  captureBusinessPageSnapshot,
  normalizeBusinessPageSnapshot
} from '../../utils/businessSnapshot';
import {
  createTrainingAttachments,
  formatAttachmentSize
} from '../../utils/trainingAttachments';
import {
  buildStableElementSelector,
  describePickedElement
} from '../../utils/elementSelector';
import { sanitizeRecordedBusinessUrl } from '../../utils/businessLaunch';

type PanelTab = 'stage' | 'step' | 'lesson' | 'publish';
type PickerToolbarPosition = 'top-right' | 'bottom-right' | 'bottom-left' | 'top-left';
type TargetKey =
  | 'create'
  | 'subject'
  | 'category'
  | 'counterparty'
  | 'amount'
  | 'date'
  | 'reason'
  | 'save'
  | 'approve'
  | 'submit';

interface BusinessScenario {
  systemName: string;
  pageTitle: string;
  documentTitle: string;
  subjectLabel: string;
  subjectPlaceholder: string;
  categoryLabel: string;
  categoryOptions: string[];
  counterpartyLabel: string;
  counterpartyPlaceholder: string;
  amountLabel: string;
  dateLabel: string;
  reasonLabel: string;
}

interface CaptureTarget {
  key: TargetKey;
  title: string;
  actionLabel: string;
  selector: string;
  pageTitle: string;
  note: string;
}

interface PickedElementPayload {
  actionType: 'click' | 'input' | 'select' | 'submit';
  url: string;
  pageTitle: string;
  selector: string;
  selectorCandidates?: string[];
  text: string;
  rect: CaptureRect;
  recordedViewport?: {
    width: number;
    height: number;
  };
  pageSnapshot?: BusinessPageSnapshot;
}

interface BusinessCaptureFrameApi {
  setRecording: (enabled: boolean) => void;
  startElementPick: () => void;
  cancelElementPick: () => void;
  previewStep: (
    selector: string,
    url?: string,
    selectorCandidates?: string[]
  ) => void;
}

const route = useRoute();
const store = useTrainingStore();
const lessonId = computed(() => String(route.params.lessonId ?? ''));
const lesson = computed(() => store.getLesson(lessonId.value));
const businessPlatform = computed(() =>
  store.getBusinessPlatform(lesson.value?.businessPlatformId ?? '')
);
const businessPlatformModule = computed(() =>
  store.getBusinessPlatformModule(
    lesson.value?.businessPlatformId ?? '',
    lesson.value?.businessPlatformModuleId ?? ''
  )
);
const effectiveBusinessPlatformUrl = computed(() =>
  resolveBusinessPlatformModuleUrl(
    businessPlatform.value?.baseUrl ?? '',
    businessPlatformModule.value?.path ?? ''
  )
);

function resolveRecordedBusinessUrl(value?: string) {
  const platformUrl = effectiveBusinessPlatformUrl.value;
  if (!platformUrl || platformUrl.startsWith('internal://')) {
    return value || platformUrl;
  }
  return sanitizeRecordedBusinessUrl(
    value || platformUrl,
    authoringLaunch.result.value?.launchUrl ?? '',
    platformUrl
  );
}

const useEmbeddedBusinessSimulation = computed(
  () =>
    !effectiveBusinessPlatformUrl.value ||
    effectiveBusinessPlatformUrl.value.startsWith('internal://')
);
const authoringLaunch = useAuthoringBusinessLaunch();
const businessFrameUrl = computed(() =>
  useEmbeddedBusinessSimulation.value
    ? effectiveBusinessPlatformUrl.value
    : authoringLaunch.frameUrl.value
);
const authoringAllowedOrigins = computed(() =>
  [
    businessPlatform.value?.baseUrl,
    effectiveBusinessPlatformUrl.value,
    authoringLaunch.result.value?.redirectUrl,
    authoringLaunch.result.value?.launchUrl
  ].filter((url): url is string => Boolean(url))
);
const authoringLaunchContextKey = computed(() => {
  const currentLessonId = lesson.value?.id ?? '';
  const platformId = businessPlatform.value?.id ?? '';
  const moduleId = businessPlatformModule.value?.id ?? '';
  if (
    !currentLessonId ||
    !platformId ||
    !moduleId ||
    useEmbeddedBusinessSimulation.value
  ) {
    return '';
  }
  return `${currentLessonId}\u0000${platformId}\u0000${moduleId}`;
});
const selectedStageId = ref('');
const selectedStepId = ref('');
const recording = ref(false);
const controlsCollapsed = ref(false);
const showStagePanel = ref(false);
const showConfigPanel = ref(false);
const panelTab = ref<PanelTab>('stage');
const pickerToolbarPosition = ref<PickerToolbarPosition>('top-right');
const feedback = ref('');
const feedbackTone = ref<'success' | 'danger'>('success');
const activityText = ref(
  '选择教学点后，点击“开始录制”将直接进入元素选择模式。'
);
const workspaceRef = ref<HTMLElement | null>(null);
const businessLayerRef = ref<HTMLElement | null>(null);
const captureFrameRef = ref<BusinessCaptureFrameApi | null>(null);
const elementPicking = ref(false);
const pickTargetStepId = ref('');
const pickedElementLabel = ref('');
const elementPickerStyle = ref<Record<string, string>>({});
const selectedStepPreviewStyle = ref<Record<string, string>>({});
let continuousPickResumeToken = 0;

const configurationLocked = computed(() =>
  store.state.publishedTasks.some((task) => task.lessonId === lessonId.value)
);

const basicForm = reactive({
  code: '',
  title: '',
  moduleName: '',
  businessPlatformId: '',
  businessPlatformModuleId: '',
  description: '',
  objectiveMaxScore: 0,
  subjectiveMaxScore: 0,
  tags: ''
});
const availableBasicBusinessModules = computed(() =>
  (
    store.getBusinessPlatform(basicForm.businessPlatformId)?.modules ?? []
  ).filter(
    (businessModule) =>
      businessModule.status === 'ENABLED' ||
      businessModule.id === basicForm.businessPlatformModuleId
  )
);

const businessForm = reactive({
  subject: '',
  category: '',
  counterparty: '',
  amount: '',
  date: '',
  reason: ''
});

type StageDraft = Omit<LessonStage, 'recordedSteps'>;
const stageForm = ref<StageDraft | null>(null);
const selectedStage = computed(() =>
  lesson.value?.stages.find((stage) => stage.id === selectedStageId.value)
);
const selectedStep = computed(() =>
  selectedStage.value?.recordedSteps.find((step) => step.id === selectedStepId.value)
);

const objectiveStageScore = computed(
  () => lesson.value?.stages.reduce((total, stage) => total + Number(stage.score), 0) ?? 0
);
const totalScore = computed(
  () =>
    Number(lesson.value?.objectiveMaxScore ?? 0) +
    Number(lesson.value?.subjectiveMaxScore ?? 0)
);
const recordedStepCount = computed(
  () =>
    lesson.value?.stages.reduce((total, stage) => total + stage.recordedSteps.length, 0) ??
    0
);
const activeStageIndex = computed(
  () => lesson.value?.stages.findIndex((stage) => stage.id === selectedStageId.value) ?? -1
);
const validationMessages = computed<string[]>(() => {
  if (!lesson.value) return ['教案不存在'];
  try {
    return store.validateLesson(lesson.value.id);
  } catch (error) {
    return [error instanceof Error ? error.message : '教案校验失败'];
  }
});

const completionMethods: Array<{ value: CompletionMethod; label: string }> = [
  { value: 'click', label: '完成关键点击' },
  { value: 'business_check', label: '业务状态校验' },
  { value: 'submission', label: '完成提交' },
  { value: 'manual', label: '教师人工确认' },
  { value: 'mixed', label: '流程操作综合判断' }
];

const modes: Array<{ key: RunMode; label: string }> = [
  { key: 'LEARNING', label: '演示学习' },
  { key: 'PRACTICE', label: '练习' },
  { key: 'EXAM', label: '考试' }
];

const businessScenario = computed<BusinessScenario>(() => {
  const moduleName = lesson.value?.moduleName ?? '';
  if (moduleName.includes('报销') || moduleName.includes('财务')) {
    return {
      systemName: '企业财务共享系统',
      pageTitle: '费用报销',
      documentTitle: '费用报销单',
      subjectLabel: '报销事由',
      subjectPlaceholder: '请输入本次报销事由',
      categoryLabel: '费用类型',
      categoryOptions: ['差旅费', '办公费', '业务招待费'],
      counterpartyLabel: '收款人',
      counterpartyPlaceholder: '请输入收款人姓名',
      amountLabel: '报销金额',
      dateLabel: '费用发生日期',
      reasonLabel: '费用说明'
    };
  }
  if (moduleName.includes('合同')) {
    return {
      systemName: '合同全生命周期系统',
      pageTitle: '合同起草',
      documentTitle: '合同审批单',
      subjectLabel: '合同名称',
      subjectPlaceholder: '请输入合同名称',
      categoryLabel: '合同类型',
      categoryOptions: ['采购合同', '服务合同', '框架协议'],
      counterpartyLabel: '合同相对方',
      counterpartyPlaceholder: '请输入合同相对方',
      amountLabel: '合同金额',
      dateLabel: '计划签署日期',
      reasonLabel: '合同事项说明'
    };
  }
  return {
    systemName: '企业采购业务系统',
    pageTitle: '采购申请',
    documentTitle: '采购申请单',
    subjectLabel: '申请主题',
    subjectPlaceholder: '请输入采购申请主题',
    categoryLabel: '采购品类',
    categoryOptions: ['办公设备', '信息化服务', '行政物资'],
    counterpartyLabel: '意向供应商',
    counterpartyPlaceholder: '请输入意向供应商',
    amountLabel: '申请金额',
    dateLabel: '期望到货日期',
    reasonLabel: '申请原因'
  };
});

const captureTargets = computed<Record<TargetKey, CaptureTarget>>(() => {
  const pageTitle = businessScenario.value.pageTitle;
  return {
    create: {
      key: 'create',
      title: `新建${businessScenario.value.documentTitle}`,
      actionLabel: '新建业务单',
      selector: '[data-action="create"]',
      pageTitle,
      note: '进入业务单据新增页面，确认页面已经完成初始化。'
    },
    subject: {
      key: 'subject',
      title: `填写${businessScenario.value.subjectLabel}`,
      actionLabel: businessScenario.value.subjectLabel,
      selector: '[data-business-field="subject"]',
      pageTitle,
      note: `填写${businessScenario.value.subjectLabel}并检查输入结果。`
    },
    category: {
      key: 'category',
      title: `选择${businessScenario.value.categoryLabel}`,
      actionLabel: businessScenario.value.categoryLabel,
      selector: '[data-business-field="category"]',
      pageTitle,
      note: `根据业务要求选择${businessScenario.value.categoryLabel}。`
    },
    counterparty: {
      key: 'counterparty',
      title: `填写${businessScenario.value.counterpartyLabel}`,
      actionLabel: businessScenario.value.counterpartyLabel,
      selector: '[data-business-field="counterparty"]',
      pageTitle,
      note: `录入${businessScenario.value.counterpartyLabel}。`
    },
    amount: {
      key: 'amount',
      title: `填写${businessScenario.value.amountLabel}`,
      actionLabel: businessScenario.value.amountLabel,
      selector: '[data-business-field="amount"]',
      pageTitle,
      note: `填写${businessScenario.value.amountLabel}并核对金额格式。`
    },
    date: {
      key: 'date',
      title: `选择${businessScenario.value.dateLabel}`,
      actionLabel: businessScenario.value.dateLabel,
      selector: '[data-business-field="date"]',
      pageTitle,
      note: `选择${businessScenario.value.dateLabel}。`
    },
    reason: {
      key: 'reason',
      title: `填写${businessScenario.value.reasonLabel}`,
      actionLabel: businessScenario.value.reasonLabel,
      selector: '[data-business-field="reason"]',
      pageTitle,
      note: `补充${businessScenario.value.reasonLabel}，确保说明完整。`
    },
    save: {
      key: 'save',
      title: '保存业务草稿',
      actionLabel: '保存草稿',
      selector: '[data-action="save"]',
      pageTitle,
      note: '保存当前业务数据，并确认页面出现保存成功反馈。'
    },
    approve: {
      key: 'approve',
      title: '完成业务审批',
      actionLabel: '审批通过',
      selector: '[data-action="approve"]',
      pageTitle,
      note: '执行审批通过，并检查业务状态是否正确更新。'
    },
    submit: {
      key: 'submit',
      title: `提交${businessScenario.value.documentTitle}`,
      actionLabel: '提交审批',
      selector: '[data-action="submit"]',
      pageTitle,
      note: '提交业务单据，并确认流程已进入下一处理环节。'
    }
  };
});

const selectedStepIndex = computed(
  () => selectedStage.value?.recordedSteps.findIndex((step) => step.id === selectedStepId.value) ?? -1
);
const statusSummary = computed(() => {
  if (!selectedStage.value) return '尚未选择教学点';
  const state = recording.value ? '录制中' : '已暂停';
  return `${state} · 第 ${activeStageIndex.value + 1} 教学点 · ${selectedStage.value.recordedSteps.length} 个节点`;
});

async function initializeAuthoringLaunch() {
  const currentLesson = lesson.value;
  const platform = businessPlatform.value;
  const businessModule = businessPlatformModule.value;
  if (
    !currentLesson ||
    !platform ||
    !businessModule ||
    useEmbeddedBusinessSimulation.value
  ) {
    return;
  }
  await authoringLaunch.start({
    lessonId: currentLesson.id,
    connectorSystemId: platform.id,
    businessModuleId: businessModule.id
  });
}

watch(
  lesson,
  (current) => {
    if (!current) return;
    basicForm.code = current.code;
    basicForm.title = current.title;
    basicForm.moduleName = current.moduleName;
    basicForm.businessPlatformId = current.businessPlatformId;
    basicForm.businessPlatformModuleId = current.businessPlatformModuleId;
    basicForm.description = current.description;
    basicForm.objectiveMaxScore = current.objectiveMaxScore;
    basicForm.subjectiveMaxScore = current.subjectiveMaxScore;
    basicForm.tags = current.tags.join('，');
    if (!current.stages.some((stage) => stage.id === selectedStageId.value)) {
      selectedStageId.value = current.stages[0]?.id ?? '';
    }
    const currentStage = current.stages.find((stage) => stage.id === selectedStageId.value);
    if (!currentStage?.recordedSteps.some((step) => step.id === selectedStepId.value)) {
      selectedStepId.value = currentStage?.recordedSteps[0]?.id ?? '';
    }
    loadStageDraft();
  },
  { immediate: true }
);

watch(
  () => basicForm.businessPlatformId,
  () => {
    if (
      !availableBasicBusinessModules.value.some(
        (businessModule) =>
          businessModule.id === basicForm.businessPlatformModuleId
      )
    ) {
      basicForm.businessPlatformModuleId =
        availableBasicBusinessModules.value[0]?.id ?? '';
    }
  }
);

watch(selectedStageId, () => {
  continuousPickResumeToken += 1;
  if (elementPicking.value) cancelElementPick();
  loadStageDraft();
  selectedStepId.value = selectedStage.value?.recordedSteps[0]?.id ?? '';
});

watch(
  authoringLaunchContextKey,
  (contextKey) => {
    if (contextKey) {
      void initializeAuthoringLaunch();
    }
  },
  { immediate: true }
);

onMounted(async () => {
  window.addEventListener('message', handleBusinessPlatformMessage);
  window.addEventListener('keydown', handleElementPickerKeydown);
  if (!store.remote.enabled) return;
  try {
    await store.syncBusinessPlatforms();
    const current = lesson.value;
    if (current) {
      basicForm.businessPlatformId = current.businessPlatformId;
      basicForm.businessPlatformModuleId = current.businessPlatformModuleId;
    }
  } catch (error) {
    activityText.value =
      error instanceof Error ? error.message : '业务平台同步失败，请稍后重试。';
  }
});

onBeforeUnmount(() => {
  continuousPickResumeToken += 1;
  if (elementPicking.value) captureFrameRef.value?.cancelElementPick();
  window.removeEventListener('message', handleBusinessPlatformMessage);
  window.removeEventListener('keydown', handleElementPickerKeydown);
});

function loadStageDraft() {
  const stage = selectedStage.value;
  stageForm.value = stage
    ? {
        id: stage.id,
        stageKey: stage.stageKey,
        name: stage.name,
        groupKey: stage.groupKey,
        description: stage.description,
        required: stage.required,
        score: stage.score,
        completionMethod: stage.completionMethod,
        visibility: { ...stage.visibility },
        attachments: [...(stage.attachments ?? [])]
      }
    : null;
}

function startElementPick(targetStepId = '') {
  if (!lesson.value || !selectedStage.value) {
    showFeedback('请先选择或添加一个教学点，再选取页面元素。', 'danger');
    return;
  }
  if (configurationLocked.value) return;
  pickTargetStepId.value = targetStepId;
  pickedElementLabel.value = '';
  elementPickerStyle.value = {};
  closeAuthoringDrawers();
  elementPicking.value = true;
  if (!useEmbeddedBusinessSimulation.value) {
    captureFrameRef.value?.startElementPick();
  }
  activityText.value = targetStepId
    ? '正在重新绑定节点：移动鼠标预览，点击业务页面中的任意元素，按 Esc 取消。'
    : '元素选择模式已开启：移动鼠标预览，点击任意元素后填写该元素的讲解说明。';
}

function finishElementPickState() {
  elementPicking.value = false;
  pickTargetStepId.value = '';
  pickedElementLabel.value = '';
  elementPickerStyle.value = {};
}

function resumeContinuousElementPick(label: string) {
  const resumeToken = ++continuousPickResumeToken;
  void nextTick(() => {
    if (
      resumeToken !== continuousPickResumeToken ||
      !recording.value ||
      elementPicking.value ||
      configurationLocked.value ||
      !lesson.value ||
      !selectedStage.value
    ) {
      return;
    }
    showConfigPanel.value = false;
    startElementPick();
    activityText.value = `已绑定“${label}”，连续选取已保持开启，请继续选择下一个业务元素。`;
  });
}

function cancelElementPick() {
  continuousPickResumeToken += 1;
  if (!elementPicking.value) return;
  if (!useEmbeddedBusinessSimulation.value) {
    captureFrameRef.value?.cancelElementPick();
  }
  finishElementPickState();
  activityText.value = '已取消元素选取，未修改教案节点。';
}

function handleElementPickCancelled() {
  continuousPickResumeToken += 1;
  if (!elementPicking.value) return;
  finishElementPickState();
  activityText.value = '业务页面已取消元素选取，未修改教案节点。';
}

function handleElementPickerKeydown(event: KeyboardEvent) {
  if (elementPicking.value && event.key === 'Escape') {
    event.preventDefault();
    cancelElementPick();
  }
}

function handleInternalPickMove(event: PointerEvent) {
  if (!elementPicking.value || !useEmbeddedBusinessSimulation.value) return;
  const workspace = workspaceRef.value;
  const businessLayer = businessLayerRef.value;
  const target = event.target;
  if (
    !workspace ||
    !businessLayer ||
    !(target instanceof Element) ||
    target === businessLayer
  ) {
    pickedElementLabel.value = '';
    elementPickerStyle.value = {};
    return;
  }
  const targetRect = target.getBoundingClientRect();
  if (targetRect.width < 2 || targetRect.height < 2) return;
  const workspaceRect = workspace.getBoundingClientRect();
  const selectorData = buildStableElementSelector(target, businessLayer);
  pickedElementLabel.value = `${target.tagName.toLowerCase()}  ${selectorData.selector}  ${Math.round(
    targetRect.width
  )} × ${Math.round(targetRect.height)}`;
  elementPickerStyle.value = {
    left: `${targetRect.left - workspaceRect.left}px`,
    top: `${targetRect.top - workspaceRect.top}px`,
    width: `${targetRect.width}px`,
    height: `${targetRect.height}px`
  };
}

function handleInternalPickClick(event: MouseEvent) {
  if (!elementPicking.value || !useEmbeddedBusinessSimulation.value) return;
  const businessLayer = businessLayerRef.value;
  const target = event.target;
  if (
    !businessLayer ||
    !(target instanceof Element) ||
    target === businessLayer
  ) {
    return;
  }
  event.preventDefault();
  event.stopImmediatePropagation();
  const targetRect = target.getBoundingClientRect();
  const businessRect = businessLayer.getBoundingClientRect();
  const selectorData = buildStableElementSelector(target, businessLayer);
  const pageTitle = businessScenario.value.pageTitle;
  const pageUrl = effectiveBusinessPlatformUrl.value;
  handleElementPicked({
    actionType: 'click',
    url: pageUrl,
    pageTitle,
    selector: selectorData.selector,
    selectorCandidates: selectorData.selectorCandidates,
    text: describePickedElement(target),
    rect: {
      x: Math.round(targetRect.left - businessRect.left),
      y: Math.round(targetRect.top - businessRect.top),
      width: Math.round(targetRect.width),
      height: Math.round(targetRect.height)
    },
    recordedViewport: {
      width: Math.round(businessRect.width),
      height: Math.round(businessRect.height)
    },
    pageSnapshot: captureBusinessPageSnapshot(businessLayer, {
      pageUrl,
      pageTitle,
      viewport: {
        width: Math.round(businessRect.width),
        height: Math.round(businessRect.height)
      }
    })
  });
}

function handleElementPicked(payload: PickedElementPayload) {
  if (
    !elementPicking.value ||
    !lesson.value ||
    !selectedStage.value ||
    configurationLocked.value
  ) {
    return;
  }
  const label = payload.text?.trim() || '页面元素';
  const pageUrl = resolveRecordedBusinessUrl(payload.url);
  const pageTitle = payload.pageTitle || businessScenario.value.pageTitle;
  const normalizedPageSnapshot = normalizeBusinessPageSnapshot(payload.pageSnapshot, {
    pageUrl,
    pageTitle,
    viewport: payload.recordedViewport
  });
  const pageSnapshot = normalizedPageSnapshot
    ? {
        ...normalizedPageSnapshot,
        pageUrl: resolveRecordedBusinessUrl(normalizedPageSnapshot.pageUrl)
      }
    : undefined;
  const targetStepId = pickTargetStepId.value;

  if (targetStepId) {
    continuousPickResumeToken += 1;
    updateRecordedStep(targetStepId, {
      selector: payload.selector,
      selectorCandidates: payload.selectorCandidates,
      rect: payload.rect,
      url: pageUrl,
      pageTitle,
      pageSnapshot,
      recordedViewport: payload.recordedViewport
    });
    selectedStepId.value = targetStepId;
    finishElementPickState();
    showFeedback(`已将节点重新绑定到“${label}”。`);
    queueStepSync(selectedStage.value.id, targetStepId);
    return;
  }

  const order = selectedStage.value.recordedSteps.length + 1;
  const explanation = `请说明“${label}”在当前业务流程中的作用和操作要求。`;
  const step: RecordedStep = {
    id: `guide-${selectedStage.value.id}-${Date.now()}`,
    title: `元素说明 ${order}：${label}`,
    pageTitle,
    actionLabel: `查看“${label}”说明`,
    selector: payload.selector,
    selectorCandidates: payload.selectorCandidates,
    durationSeconds: 8,
    note: explanation,
    kind: 'guide',
    actionType: 'guide',
    url: pageUrl,
    rect: payload.rect,
    pageSnapshot,
    recordedViewport: payload.recordedViewport,
    teachingText: explanation,
    practiceHint: '观察高亮元素并阅读本节点说明。',
    examGoal: `理解“${label}”的业务含义`,
    required: false,
    failurePolicy: 'skip'
  };
  store.updateStage(lesson.value.id, selectedStage.value.id, {
    recordedSteps: [...selectedStage.value.recordedSteps, step]
  });
  selectedStepId.value = step.id;
  const shouldContinuePicking = recording.value;
  finishElementPickState();
  if (shouldContinuePicking) {
    showConfigPanel.value = false;
    showFeedback(`已绑定“${label}”，可继续选择下一个业务元素。`);
  } else {
    openPanel('step');
    showFeedback(`已绑定“${label}”，请在节点配置中完善逐步讲解。`);
  }
  queueStepSync(selectedStage.value.id, step.id);
  if (shouldContinuePicking) resumeContinuousElementPick(label);
}

function handleBusinessPlatformMessage(event: MessageEvent) {
  const platform = businessPlatform.value;
  const message = event.data as Record<string, unknown> | null;
  const messageType = String(message?.type ?? '');
  if (
    useEmbeddedBusinessSimulation.value ||
    !platform ||
    !recording.value ||
    configurationLocked.value ||
    !lesson.value ||
    !selectedStage.value ||
    !message ||
    !['BUSINESS_ACTION', 'SXPT_BUSINESS_ACTION'].includes(messageType)
  ) {
    return;
  }

  try {
    const platformOrigin = new URL(
      effectiveBusinessPlatformUrl.value,
      window.location.origin
    ).origin;
    if (event.origin !== platformOrigin) return;
  } catch {
    return;
  }

  const payload =
    messageType === 'SXPT_BUSINESS_ACTION' &&
    message.payload &&
    typeof message.payload === 'object'
      ? (message.payload as Record<string, unknown>)
      : message;
  const title = String(payload.title ?? payload.text ?? payload.actionLabel ?? '业务操作');
  const actionLabel = String(payload.actionLabel ?? payload.text ?? title);
  const pageUrl = resolveRecordedBusinessUrl(String(payload.url ?? ''));
  const pageTitle = String(payload.pageTitle ?? platform.name);
  const recordedViewport =
    payload.recordedViewport &&
    typeof payload.recordedViewport === 'object'
      ? {
          width: Number(
            (payload.recordedViewport as Record<string, unknown>).width
          ),
          height: Number(
            (payload.recordedViewport as Record<string, unknown>).height
          )
        }
      : undefined;
  const normalizedPageSnapshot = normalizeBusinessPageSnapshot(payload.pageSnapshot, {
    pageUrl,
    pageTitle,
    viewport: recordedViewport
  });
  const pageSnapshot = normalizedPageSnapshot
    ? {
        ...normalizedPageSnapshot,
        pageUrl: resolveRecordedBusinessUrl(normalizedPageSnapshot.pageUrl)
      }
    : undefined;
  const step: RecordedStep = {
    id: `record-${selectedStage.value.id}-${Date.now()}`,
    title,
    pageTitle,
    actionLabel,
    selector: String(payload.selector ?? ''),
    durationSeconds: Number(payload.durationSeconds ?? 8),
    note: String(payload.note ?? `在${platform.name}中完成“${actionLabel}”。`),
    kind: 'action',
    actionType: String(payload.actionType ?? 'click') as RecordedStep['actionType'],
    url: pageUrl,
    pageSnapshot,
    recordedViewport,
    teachingText: String(payload.teachingText ?? ''),
    practiceHint: String(payload.practiceHint ?? ''),
    examGoal: String(payload.examGoal ?? ''),
    required: payload.required !== false,
    failurePolicy: 'stop'
  };
  store.updateStage(lesson.value.id, selectedStage.value.id, {
    recordedSteps: [...selectedStage.value.recordedSteps, step]
  });
  selectedStepId.value = step.id;
  showFeedback(`已从“${platform.name}”采集节点：${title}`);
  queueStepSync(selectedStage.value.id, step.id);
}

function showFeedback(message: string, tone: 'success' | 'danger' = 'success') {
  feedback.value = message;
  feedbackTone.value = tone;
  activityText.value = message;
}

function handleBusinessFrameLoad() {
  activityText.value = `业务模块“${
    businessPlatformModule.value?.name ?? businessPlatform.value?.name ?? ''
  }”已加载，可开始录制。`;
  if (!recording.value || !elementPicking.value) return;
  captureFrameRef.value?.setRecording(true);
  captureFrameRef.value?.startElementPick();
  activityText.value = '业务页面已恢复，元素连续选取仍保持开启。';
}

async function selectRecordedStep(step: RecordedStep) {
  selectedStepId.value = step.id;
  selectedStepPreviewStyle.value = {};
  if (!step.selector) return;
  if (!useEmbeddedBusinessSimulation.value) {
    captureFrameRef.value?.previewStep(
      step.selector,
      step.url,
      step.selectorCandidates
    );
    return;
  }

  await nextTick();
  const workspace = workspaceRef.value;
  const businessLayer = businessLayerRef.value;
  if (!workspace || !businessLayer) return;
  const selectors = [step.selector, ...(step.selectorCandidates ?? [])].filter(
    (selector, index, candidates) =>
      Boolean(selector) && candidates.indexOf(selector) === index
  );
  let target: Element | null = null;
  for (const selector of selectors) {
    try {
      target = businessLayer.querySelector(selector);
    } catch {
      target = null;
    }
    if (target) break;
  }
  if (!target) return;
  const targetRect = target.getBoundingClientRect();
  const workspaceRect = workspace.getBoundingClientRect();
  selectedStepPreviewStyle.value = {
    left: `${targetRect.left - workspaceRect.left}px`,
    top: `${targetRect.top - workspaceRect.top}px`,
    width: `${targetRect.width}px`,
    height: `${targetRect.height}px`
  };
}

function openPanel(tab: PanelTab) {
  panelTab.value = tab;
  showConfigPanel.value = true;
  showStagePanel.value = false;
}

function toggleStagePanel() {
  showStagePanel.value = !showStagePanel.value;
  if (showStagePanel.value) showConfigPanel.value = false;
}

function toggleConfigPanel(tab: PanelTab = 'step') {
  if (showConfigPanel.value && panelTab.value === tab) {
    showConfigPanel.value = false;
    return;
  }
  openPanel(tab);
}

function closeAuthoringDrawers() {
  showStagePanel.value = false;
  showConfigPanel.value = false;
}

function cyclePickerToolbarPosition() {
  const positions: PickerToolbarPosition[] = [
    'top-right',
    'bottom-right',
    'bottom-left',
    'top-left'
  ];
  const currentIndex = positions.indexOf(pickerToolbarPosition.value);
  pickerToolbarPosition.value = positions[(currentIndex + 1) % positions.length];
}

function saveBasicInformation() {
  if (!lesson.value) return;
  try {
    store.updateLesson(lesson.value.id, {
      code: basicForm.code.trim(),
      title: basicForm.title.trim(),
      moduleName:
        store.getBusinessPlatformModule(
          basicForm.businessPlatformId,
          basicForm.businessPlatformModuleId
        )?.name ?? basicForm.moduleName.trim(),
      businessPlatformId: basicForm.businessPlatformId,
      businessPlatformModuleId: basicForm.businessPlatformModuleId,
      description: basicForm.description.trim(),
      objectiveMaxScore: Number(basicForm.objectiveMaxScore),
      subjectiveMaxScore: Number(basicForm.subjectiveMaxScore),
      tags: basicForm.tags
        .split(/[,，]/)
        .map((tag) => tag.trim())
        .filter(Boolean)
    });
    showFeedback('基础信息已保存。');
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '保存失败', 'danger');
  }
}

function saveStage() {
  if (!lesson.value || !stageForm.value) return;
  try {
    store.updateStage(lesson.value.id, stageForm.value.id, {
      stageKey: stageForm.value.stageKey.trim(),
      name: stageForm.value.name.trim(),
      groupKey: stageForm.value.groupKey.trim(),
      description: stageForm.value.description.trim(),
      required: stageForm.value.required,
      score: Number(stageForm.value.score),
      completionMethod: stageForm.value.completionMethod,
      visibility: { ...stageForm.value.visibility },
      attachments: [...(stageForm.value.attachments ?? [])]
    });
    showFeedback(`“${stageForm.value.name}”教学点规则已保存。`);
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '教学点保存失败', 'danger');
  }
}

function addStage() {
  if (!lesson.value || configurationLocked.value) return;
  const index = lesson.value.stages.length + 1;
  const remaining = Math.max(
    0,
    lesson.value.objectiveMaxScore - objectiveStageScore.value
  );
  try {
    const stage = store.addStage(lesson.value.id, {
      stageKey: `stage_${index}`,
      name: `教学点 ${index}`,
      groupKey: `role_${index}`,
      description: '说明本教学点的业务目标、移交条件和操作注意事项。',
      required: true,
      score: remaining,
      completionMethod: 'mixed',
      visibility: { LEARNING: true, PRACTICE: true, EXAM: true },
      recordedSteps: [],
      attachments: []
    });
    selectedStageId.value = stage.id;
    openPanel('stage');
    showFeedback(`已添加“${stage.name}”。`);
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '教学点添加失败', 'danger');
  }
}

async function uploadTeachingPointAttachments(event: Event) {
  const input = event.target as HTMLInputElement;
  if (
    !lesson.value ||
    !selectedStage.value ||
    !input.files?.length ||
    configurationLocked.value
  ) {
    input.value = '';
    return;
  }
  try {
    showFeedback('正在上传教学点附件，请稍候…');
    const additions = await createTrainingAttachments(input.files);
    const attachments = [
      ...(selectedStage.value.attachments ?? []),
      ...additions
    ];
    store.updateStage(lesson.value.id, selectedStage.value.id, {
      attachments
    });
    if (stageForm.value) stageForm.value.attachments = [...attachments];
    showFeedback(`已为教学点添加 ${additions.length} 个附件。`);
  } catch (error) {
    showFeedback(
      error instanceof Error ? error.message : '教学点附件上传失败',
      'danger'
    );
  } finally {
    input.value = '';
  }
}

function removeTeachingPointAttachment(attachmentId: string) {
  if (!lesson.value || !selectedStage.value || configurationLocked.value) return;
  const attachments = (selectedStage.value.attachments ?? []).filter(
    (attachment) => attachment.id !== attachmentId
  );
  store.updateStage(lesson.value.id, selectedStage.value.id, { attachments });
  if (stageForm.value) stageForm.value.attachments = [...attachments];
  showFeedback('教学点附件已移除。');
}

async function uploadStepAttachments(event: Event) {
  const input = event.target as HTMLInputElement;
  if (!selectedStep.value || !input.files?.length || configurationLocked.value) {
    input.value = '';
    return;
  }
  try {
    showFeedback('正在上传节点附件，请稍候…');
    const additions = await createTrainingAttachments(input.files);
    updateRecordedStep(selectedStep.value.id, {
      attachments: [...(selectedStep.value.attachments ?? []), ...additions]
    });
    showFeedback(`已为节点添加 ${additions.length} 个附件。`);
  } catch (error) {
    showFeedback(
      error instanceof Error ? error.message : '节点附件上传失败',
      'danger'
    );
  } finally {
    input.value = '';
  }
}

function removeStepAttachment(attachmentId: string) {
  if (!selectedStep.value || configurationLocked.value) return;
  updateRecordedStep(selectedStep.value.id, {
    attachments: (selectedStep.value.attachments ?? []).filter(
      (attachment) => attachment.id !== attachmentId
    )
  });
  showFeedback('节点附件已移除。');
}

function removeSelectedStage() {
  if (!lesson.value || !selectedStage.value || configurationLocked.value) return;
  if (!window.confirm(`确认删除“${selectedStage.value.name}”及其录制步骤吗？`)) return;
  const currentIndex = lesson.value.stages.findIndex(
    (stage) => stage.id === selectedStageId.value
  );
  try {
    store.removeStage(lesson.value.id, selectedStage.value.id);
    selectedStageId.value =
      lesson.value.stages[Math.max(0, currentIndex - 1)]?.id ??
      lesson.value.stages[0]?.id ??
      '';
    showFeedback('教学点已删除。');
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '教学点删除失败', 'danger');
  }
}

function moveSelectedStage(direction: 'up' | 'down') {
  if (!lesson.value || !selectedStage.value || configurationLocked.value) return;
  try {
    store.moveStage(lesson.value.id, selectedStage.value.id, direction);
    showFeedback(direction === 'up' ? '教学点已上移。' : '教学点已下移。');
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '教学点排序失败', 'danger');
  }
}

async function startRecording() {
  if (!selectedStage.value) {
    showFeedback('请先选择或添加一个教学点。', 'danger');
    return;
  }
  if (configurationLocked.value) return;
  if (!useEmbeddedBusinessSimulation.value && !businessFrameUrl.value) {
    showFeedback(
      authoringLaunch.error.value ||
        (authoringLaunch.loading.value
          ? '正在生成新的编排业务数据，请稍候。'
          : '业务系统单点启动尚未就绪，请重试。'),
      'danger'
    );
    return;
  }
  try {
    if (!lesson.value) return;
    continuousPickResumeToken += 1;
    await store.startCaptureSessionRemote(
      lesson.value.id,
      effectiveBusinessPlatformUrl.value
    );
    recording.value = true;
    closeAuthoringDrawers();
    captureFrameRef.value?.setRecording(true);
    await nextTick();
    startElementPick();
    activityText.value = `正在录制“${selectedStage.value.name}”：已进入连续元素选择模式，请依次点击需要绑定说明的业务元素。`;
  } catch (error) {
    showFeedback(
      error instanceof Error ? error.message : '后端采集会话创建失败',
      'danger'
    );
  }
}

function pauseRecording() {
  recording.value = false;
  continuousPickResumeToken += 1;
  if (elementPicking.value) cancelElementPick();
  captureFrameRef.value?.setRecording(false);
  activityText.value = '录制已暂停，可编辑节点、调整顺序或保存当前教学点。';
}

async function captureBusinessAction(targetKey: TargetKey) {
  const target = captureTargets.value[targetKey];
  if (!recording.value || !lesson.value || !selectedStage.value || configurationLocked.value) {
    activityText.value = `已执行业务操作“${target.actionLabel}”；开始录制后该操作会生成教案节点。`;
    return;
  }
  await nextTick();
  const businessLayer = businessLayerRef.value;
  const businessRect = businessLayer?.getBoundingClientRect();
  const recordedViewport = businessRect
    ? {
        width: Math.round(businessRect.width),
        height: Math.round(businessRect.height)
      }
    : undefined;
  const pageSnapshot = businessLayer
    ? captureBusinessPageSnapshot(businessLayer, {
        pageUrl: effectiveBusinessPlatformUrl.value,
        pageTitle: target.pageTitle,
        viewport: recordedViewport
      })
    : undefined;
  const step: RecordedStep = {
    id: `record-${selectedStage.value.id}-${Date.now()}`,
    title: target.title,
    pageTitle: target.pageTitle,
    actionLabel: target.actionLabel,
    selector: target.selector,
    durationSeconds: 8,
    note: target.note,
    kind: 'action',
    actionType: targetKey === 'submit' ? 'submit' : targetKey === 'category' ? 'select' : 'click',
    url: effectiveBusinessPlatformUrl.value,
    pageSnapshot,
    recordedViewport,
    teachingText: target.note,
    practiceHint: `请完成“${target.actionLabel}”操作。`,
    examGoal: `正确完成${target.title}`,
    required: true,
    failurePolicy: 'stop'
  };
  store.updateStage(lesson.value.id, selectedStage.value.id, {
    recordedSteps: [...selectedStage.value.recordedSteps, step]
  });
  selectedStepId.value = step.id;
  showFeedback(`已采集节点：${step.title}`);
  queueStepSync(selectedStage.value.id, step.id);
}

function addRecordedStep() {
  if (!lesson.value || !selectedStage.value || configurationLocked.value) return;
  const order = selectedStage.value.recordedSteps.length + 1;
  const step: RecordedStep = {
    id: `guide-${selectedStage.value.id}-${Date.now()}`,
    title: `操作说明 ${order}`,
    pageTitle: businessScenario.value.pageTitle,
    actionLabel: '查看操作说明',
    selector: selectedStep.value?.selector || '[data-business-target="form"]',
    durationSeconds: 8,
    note: '说明当前业务步骤的操作要求和预期结果。',
    kind: 'guide',
    actionType: 'guide',
    url: effectiveBusinessPlatformUrl.value,
    teachingText: '请按照说明完成高亮区域中的业务操作。',
    practiceHint: '观察高亮区域后再进行操作。',
    examGoal: '理解本步骤业务要求',
    required: false,
    failurePolicy: 'skip'
  };
  store.updateStage(lesson.value.id, selectedStage.value.id, {
    recordedSteps: [...selectedStage.value.recordedSteps, step]
  });
  selectedStepId.value = step.id;
  openPanel('step');
  showFeedback('已在当前页面插入说明节点。');
  queueStepSync(selectedStage.value.id, step.id);
}

function queueStepSync(stageId: string, stepId: string) {
  if (!lesson.value || !store.remote.enabled) return;
  void store
    .syncRecordedStep(lesson.value.id, stageId, stepId)
    .then(() => {
      showFeedback('录制节点已同步到后端并生成动作草稿。');
    })
    .catch((error) => {
      showFeedback(
        `节点已保存在本地，但后端同步失败：${
          error instanceof Error ? error.message : '未知错误'
        }`,
        'danger'
      );
    });
}

function updateRecordedStep(stepId: string, patch: Partial<RecordedStep>) {
  if (!lesson.value || !selectedStage.value || configurationLocked.value) return;
  store.updateStage(lesson.value.id, selectedStage.value.id, {
    recordedSteps: selectedStage.value.recordedSteps.map((step) =>
      step.id === stepId ? { ...step, ...patch } : step
    )
  });
}

function removeRecordedStep(stepId: string) {
  if (!lesson.value || !selectedStage.value || configurationLocked.value) return;
  const index = selectedStage.value.recordedSteps.findIndex((step) => step.id === stepId);
  const nextSteps = selectedStage.value.recordedSteps.filter((step) => step.id !== stepId);
  store.updateStage(lesson.value.id, selectedStage.value.id, {
    recordedSteps: nextSteps
  });
  selectedStepId.value = nextSteps[Math.max(0, index - 1)]?.id ?? nextSteps[0]?.id ?? '';
  showFeedback('录制步骤已移除。');
}

function moveRecordedStep(direction: 'up' | 'down') {
  if (!lesson.value || !selectedStage.value || !selectedStep.value || configurationLocked.value) {
    return;
  }
  const steps = [...selectedStage.value.recordedSteps];
  const currentIndex = steps.findIndex((step) => step.id === selectedStep.value?.id);
  const targetIndex = direction === 'up' ? currentIndex - 1 : currentIndex + 1;
  if (currentIndex < 0 || targetIndex < 0 || targetIndex >= steps.length) return;
  [steps[currentIndex], steps[targetIndex]] = [steps[targetIndex], steps[currentIndex]];
  store.updateStage(lesson.value.id, selectedStage.value.id, { recordedSteps: steps });
  showFeedback(direction === 'up' ? '节点已上移。' : '节点已下移。');
}

function undoLastStep() {
  const lastStep = selectedStage.value?.recordedSteps.at(-1);
  if (!lastStep) {
    showFeedback('当前教学点没有可撤销的节点。', 'danger');
    return;
  }
  removeRecordedStep(lastStep.id);
}

function saveCurrentStage() {
  saveStage();
  pauseRecording();
}

async function publishLesson() {
  if (!lesson.value) return;
  try {
    await store.publishLessonRemote(lesson.value.id);
    showFeedback(
      store.remote.enabled
        ? '教案已发布到后端，正式资源、动作草稿和教学点均已完成关联。'
        : '教案发布成功，现在可以进入考试设置。'
    );
  } catch (error) {
    showFeedback(error instanceof Error ? error.message : '发布校验未通过', 'danger');
    openPanel('publish');
  }
}

function inputValue(event: Event) {
  return (event.target as HTMLInputElement).value;
}

function resolveBusinessPlatformModuleUrl(baseUrl: string, path: string) {
  if (!baseUrl) return '';
  if (/^[a-z][a-z\d+.-]*:\/\//i.test(path)) return path;
  if (baseUrl.startsWith('internal://')) {
    return `${baseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`;
  }
  try {
    return new URL(path, baseUrl).toString();
  } catch {
    return `${baseUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`;
  }
}

function numberValue(event: Event) {
  return Number((event.target as HTMLInputElement).value);
}
</script>

<template>
  <section v-if="lesson" ref="workspaceRef" class="authoring-workspace">
    <main
      ref="businessLayerRef"
      class="business-layer"
      :class="{ 'element-picking': elementPicking }"
      aria-label="下层业务系统操作界面"
      @pointermove.capture="handleInternalPickMove"
      @click.capture="handleInternalPickClick"
    >
      <template v-if="!useEmbeddedBusinessSimulation && businessPlatform">
        <BusinessCaptureFrame
          v-if="businessFrameUrl"
          ref="captureFrameRef"
          class="configured-business-frame"
          fit-mode="fill"
          :src="businessFrameUrl"
          :allowed-origins="authoringAllowedOrigins"
          :title="`${businessPlatform.name}${businessPlatformModule ? ` / ${businessPlatformModule.name}` : ''}业务界面`"
          @frame-load="handleBusinessFrameLoad"
          @element-picked="handleElementPicked"
          @element-pick-cancelled="handleElementPickCancelled"
        />
        <div v-else class="authoring-launch-state">
          <span>{{ authoringLaunch.loading.value ? '…' : '!' }}</span>
          <h2>
            {{ authoringLaunch.loading.value ? '正在创建编排业务数据' : '业务系统启动失败' }}
          </h2>
          <p>
            {{
              authoringLaunch.loading.value
                ? '正在生成新的 RECORD 数据实例并建立单点登录上下文，请稍候。'
                : authoringLaunch.error.value || '请检查业务平台、业务模块和 RECORD 数据策略配置。'
            }}
          </p>
          <button
            v-if="!authoringLaunch.loading.value"
            type="button"
            @click="initializeAuthoringLaunch"
          >
            重新生成并打开
          </button>
        </div>
      </template>
      <template v-else>
      <header class="business-header">
        <div class="business-brand">
          <span class="business-brand__mark">B</span>
          <span>
            <strong>{{ businessScenario.systemName }}</strong>
            <small>BUSINESS OPERATION SYSTEM</small>
          </span>
        </div>
        <label class="business-search">
          <span>⌕</span>
          <input aria-label="搜索业务功能" placeholder="搜索功能、单据或流程" />
        </label>
        <div class="business-user">
          <span class="business-user__status">业务系统已连接</span>
          <span class="business-avatar">张</span>
          <span><strong>张老师</strong><small>业务操作员</small></span>
        </div>
      </header>

      <div class="business-body">
        <aside class="business-sidebar">
          <span class="business-nav-title">业务工作台</span>
          <nav>
            <button type="button"><i>⌂</i>工作首页</button>
            <button class="active" type="button"><i>▤</i>{{ businessScenario.pageTitle }}</button>
            <button type="button"><i>⌛</i>待办中心 <em>3</em></button>
            <button type="button"><i>✓</i>已办业务</button>
            <button type="button"><i>◈</i>业务档案</button>
            <button type="button"><i>▥</i>统计报表</button>
          </nav>
          <div class="business-sidebar__help">
            <strong>业务操作环境</strong>
            <span>当前页面用于真实界面上的教案步骤采集。</span>
          </div>
        </aside>

        <section class="business-content">
          <div class="business-breadcrumb">业务工作台 / {{ businessScenario.pageTitle }} / 新建单据</div>
          <div class="business-page-title">
            <div>
              <h1>{{ businessScenario.pageTitle }}</h1>
              <p>填写业务信息并提交审批，所有操作均可由上层教案编排工具采集。</p>
            </div>
            <button
              class="business-primary target-create"
              data-action="create"
              type="button"
              @click="captureBusinessAction('create')"
            >
              ＋ 新建业务单
            </button>
          </div>

          <div class="business-stats">
            <span><small>待提交</small><strong>12</strong><em>本周 +3</em></span>
            <span><small>审批中</small><strong>7</strong><em>平均 1.6 天</em></span>
            <span><small>已完成</small><strong>36</strong><em>完成率 92%</em></span>
            <span><small>本月金额</small><strong>286.4 万</strong><em>预算内</em></span>
          </div>

          <div class="business-document-grid">
            <article class="business-card business-form-card" data-business-target="form">
              <div class="business-card__header">
                <div>
                  <span class="business-tag">待提交</span>
                  <h2>{{ businessScenario.documentTitle }}</h2>
                </div>
                <span class="business-document-no">单据编号：系统自动生成</span>
              </div>

              <div class="business-form">
                <label class="target-subject">
                  <span>{{ businessScenario.subjectLabel }} <i>*</i></span>
                  <input
                    v-model="businessForm.subject"
                    data-business-field="subject"
                    :placeholder="businessScenario.subjectPlaceholder"
                    @change="captureBusinessAction('subject')"
                  />
                </label>
                <label class="target-category">
                  <span>{{ businessScenario.categoryLabel }} <i>*</i></span>
                  <select
                    v-model="businessForm.category"
                    data-business-field="category"
                    @change="captureBusinessAction('category')"
                  >
                    <option value="" disabled>请选择</option>
                    <option
                      v-for="option in businessScenario.categoryOptions"
                      :key="option"
                      :value="option"
                    >
                      {{ option }}
                    </option>
                  </select>
                </label>
                <label class="target-counterparty">
                  <span>{{ businessScenario.counterpartyLabel }} <i>*</i></span>
                  <input
                    v-model="businessForm.counterparty"
                    data-business-field="counterparty"
                    :placeholder="businessScenario.counterpartyPlaceholder"
                    @change="captureBusinessAction('counterparty')"
                  />
                </label>
                <label class="target-amount">
                  <span>{{ businessScenario.amountLabel }} <i>*</i></span>
                  <div class="amount-input">
                    <b>¥</b>
                    <input
                      v-model="businessForm.amount"
                      data-business-field="amount"
                      type="number"
                      placeholder="0.00"
                      @change="captureBusinessAction('amount')"
                    />
                  </div>
                </label>
                <label class="target-date">
                  <span>{{ businessScenario.dateLabel }} <i>*</i></span>
                  <input
                    v-model="businessForm.date"
                    data-business-field="date"
                    type="date"
                    @change="captureBusinessAction('date')"
                  />
                </label>
                <label class="wide target-reason">
                  <span>{{ businessScenario.reasonLabel }}</span>
                  <textarea
                    v-model="businessForm.reason"
                    data-business-field="reason"
                    rows="5"
                    placeholder="请输入详细说明"
                    @change="captureBusinessAction('reason')"
                  />
                </label>
              </div>

              <div class="business-form-actions">
                <button
                  class="target-save"
                  data-action="save"
                  type="button"
                  @click="captureBusinessAction('save')"
                >
                  保存草稿
                </button>
                <button
                  class="business-dark target-approve"
                  data-action="approve"
                  type="button"
                  @click="captureBusinessAction('approve')"
                >
                  审批通过
                </button>
                <button
                  class="business-primary target-submit"
                  data-action="submit"
                  type="button"
                  @click="captureBusinessAction('submit')"
                >
                  提交审批
                </button>
              </div>
            </article>

            <aside class="business-card business-summary">
              <span class="business-tag">流程预览</span>
              <h2>单据处理进度</h2>
              <div class="business-process">
                <span class="done"><i>✓</i><b>申请人填报</b><small>当前操作</small></span>
                <span><i>2</i><b>部门负责人审批</b><small>等待处理</small></span>
                <span><i>3</i><b>业务部门复核</b><small>等待处理</small></span>
                <span><i>4</i><b>归档完成</b><small>流程结束</small></span>
              </div>
              <div class="business-summary__notice">
                <strong>操作提示</strong>
                <p>请确认必填信息完整。提交后，单据将自动流转至下一审批角色。</p>
              </div>
            </aside>
          </div>
        </section>
      </div>
      </template>
    </main>

    <div
      v-if="elementPicking && useEmbeddedBusinessSimulation && elementPickerStyle.width"
      class="element-picker-highlight"
      :style="elementPickerStyle"
      aria-hidden="true"
    >
      <span>{{ pickedElementLabel }}</span>
    </div>

    <div
      v-if="!elementPicking && useEmbeddedBusinessSimulation && selectedStepPreviewStyle.width"
      class="element-picker-highlight selected-step-preview-highlight"
      :style="selectedStepPreviewStyle"
      aria-hidden="true"
    >
      <span>已绑定元素</span>
    </div>

    <div
      v-if="elementPicking"
      class="element-picker-toolbar"
      :class="`position-${pickerToolbarPosition}`"
    >
      <span>⌖ 元素选择模式</span>
      <strong>移动鼠标预览，点击任意业务元素进行绑定</strong>
      <small>录制中绑定后会自动继续选取；按 Esc 可结束连续选取</small>
      <button type="button" @click="cyclePickerToolbarPosition">换个角落</button>
      <button v-if="recording" type="button" @click="pauseRecording">暂停录制</button>
      <button type="button" @click="cancelElementPick">结束选取</button>
    </div>

    <header
      v-if="!controlsCollapsed && !recording && !elementPicking && !showStagePanel && !showConfigPanel"
      class="authoring-commandbar"
    >
      <div class="authoring-heading">
        <span class="recording-dot" :class="{ active: recording }" />
        <div>
          <small>教案编排 · 业务系统实时采集</small>
          <strong>{{ lesson.title }}</strong>
        </div>
        <span class="version-badge">V{{ lesson.version }}</span>
        <span v-if="businessPlatform" class="platform-context-badge">
          {{ businessPlatform.name }}
          <template v-if="businessPlatformModule">
            / {{ businessPlatformModule.name }}
          </template>
        </span>
      </div>
      <div class="command-actions">
        <RouterLink class="command-link" :to="{ name: 'lesson-list' }">返回教案</RouterLink>
        <button
          v-if="!recording"
          type="button"
          :disabled="configurationLocked || !selectedStage"
          @click="startRecording"
        >
          ● 开始录制并选取元素
        </button>
        <button v-else type="button" @click="pauseRecording">Ⅱ 暂停</button>
        <button type="button" :disabled="configurationLocked" @click="undoLastStep">
          ↶ 撤销节点
        </button>
        <button type="button" :disabled="configurationLocked" @click="addRecordedStep">
          ＋ 插入说明
        </button>
        <button
          type="button"
          :class="{ active: elementPicking }"
          :disabled="configurationLocked || !selectedStage"
          @click="elementPicking ? cancelElementPick() : startElementPick()"
        >
          ⌖ {{ elementPicking ? '取消选取' : '选取元素' }}
        </button>
        <button type="button" :disabled="configurationLocked || !selectedStage" @click="saveCurrentStage">
          保存本教学点
        </button>
        <button class="command-primary" type="button" :disabled="configurationLocked" @click="publishLesson">
          发布教案
        </button>
        <RouterLink
          class="command-link"
          :to="{ name: 'lesson-recording', params: { lessonId: lesson.id } }"
        >
          录制回看
        </RouterLink>
        <a
          v-if="businessPlatform && !useEmbeddedBusinessSimulation"
          class="command-link"
          :href="effectiveBusinessPlatformUrl"
          target="_blank"
          rel="noreferrer"
        >
          单独打开业务模块 ↗
        </a>
      </div>
    </header>

    <aside v-if="!controlsCollapsed && showStagePanel" class="glass-panel stage-panel">
      <div class="panel-header">
        <div>
          <small>FLOW & STEPS</small>
          <h2>教学点与节点</h2>
        </div>
        <div class="panel-header-actions">
          <button type="button" :disabled="configurationLocked" @click="addStage">＋ 教学点</button>
          <button type="button" aria-label="关闭教学点目录" @click="showStagePanel = false">×</button>
        </div>
      </div>
      <div class="stage-list">
        <article
          v-for="(stage, index) in lesson.stages"
          :key="stage.id"
          class="stage-item"
          :class="{ active: stage.id === selectedStageId }"
        >
          <button type="button" @click="selectedStageId = stage.id">
            <span>{{ index + 1 }}</span>
            <span>
              <strong>{{ stage.name }}</strong>
              <small>{{ stage.groupKey || '未指定角色' }} · {{ stage.score }} 分</small>
            </span>
            <em>{{ stage.recordedSteps.length }}</em>
          </button>
          <div v-if="stage.id === selectedStageId" class="step-list">
            <button
              v-for="(step, stepIndex) in stage.recordedSteps"
              :key="step.id"
              type="button"
              :class="{ active: step.id === selectedStepId }"
              @click="selectRecordedStep(step)"
              @dblclick="openPanel('step')"
            >
              <i>{{ stepIndex + 1 }}</i>
              <span>
                <strong>{{ step.title }}</strong>
                <small>{{ step.kind === 'guide' ? '说明节点' : step.actionLabel }}</small>
              </span>
              <b v-if="step.id === selectedStepId">●</b>
            </button>
            <div v-if="!stage.recordedSteps.length" class="step-empty">
              开始录制后会进入连续元素选择模式，可依次点击目标元素生成说明节点。
            </div>
          </div>
        </article>
        <div v-if="!lesson.stages.length" class="stage-empty">
          暂无教学点，请先添加教学点。
        </div>
      </div>
      <div class="stage-panel-actions">
        <button type="button" :disabled="activeStageIndex <= 0" @click="moveSelectedStage('up')">
          ↑ 上移
        </button>
        <button
          type="button"
          :disabled="activeStageIndex < 0 || activeStageIndex >= lesson.stages.length - 1"
          @click="moveSelectedStage('down')"
        >
          ↓ 下移
        </button>
        <button type="button" @click="openPanel('stage')">教学点配置</button>
      </div>
    </aside>

    <aside v-if="!controlsCollapsed && showConfigPanel" class="glass-panel config-panel">
      <div class="config-tabs">
        <button :class="{ active: panelTab === 'stage' }" type="button" @click="panelTab = 'stage'">教学点</button>
        <button :class="{ active: panelTab === 'step' }" type="button" @click="panelTab = 'step'">节点</button>
        <button :class="{ active: panelTab === 'lesson' }" type="button" @click="panelTab = 'lesson'">教案</button>
        <button :class="{ active: panelTab === 'publish' }" type="button" @click="panelTab = 'publish'">校验</button>
        <button class="config-close" type="button" aria-label="关闭配置" @click="showConfigPanel = false">×</button>
      </div>

      <div v-if="panelTab === 'stage' && stageForm" class="config-content">
        <div class="config-title">
          <div><small>TEACHING POINT</small><h2>教学点规则</h2></div>
          <button class="danger-text" type="button" @click="removeSelectedStage">删除</button>
        </div>
        <label>
          <span>教学点名称</span>
          <input v-model="stageForm.name" :disabled="configurationLocked" />
        </label>
        <div class="config-grid">
          <label>
            <span>教学点标识 stageKey</span>
            <input v-model="stageForm.stageKey" :disabled="configurationLocked" />
          </label>
          <label>
            <span>负责角色 groupKey</span>
            <input v-model="stageForm.groupKey" :disabled="configurationLocked" />
          </label>
          <label>
            <span>教学点分值</span>
            <input v-model.number="stageForm.score" type="number" min="0" :disabled="configurationLocked" />
          </label>
          <label>
            <span>完成方式 completionMethod</span>
            <select v-model="stageForm.completionMethod" :disabled="configurationLocked">
              <option v-for="method in completionMethods" :key="method.value" :value="method.value">
                {{ method.label }}
              </option>
            </select>
          </label>
        </div>
        <label>
          <span>教学说明</span>
          <textarea v-model="stageForm.description" rows="4" :disabled="configurationLocked" />
        </label>
        <section class="attachment-editor">
          <div class="attachment-editor__heading">
            <span>教学点附件</span>
            <label class="attachment-upload">
              <input
                type="file"
                multiple
                :disabled="configurationLocked"
                @change="uploadTeachingPointAttachments"
              />
              ＋ 上传附件
            </label>
          </div>
          <small>支持图片、PDF、文档等文件；讲解/学习时可弹窗预览并最小化到右侧栏；单个文件不超过 5 MB。</small>
          <div v-if="stageForm.attachments?.length" class="attachment-editor__list">
            <article v-for="attachment in stageForm.attachments" :key="attachment.id">
              <span>附件</span>
              <div>
                <strong>{{ attachment.name }}</strong>
                <small>
                  {{ attachment.mimeType }} · {{ formatAttachmentSize(attachment.size) }}
                </small>
              </div>
              <button
                type="button"
                :disabled="configurationLocked"
                @click="removeTeachingPointAttachment(attachment.id)"
              >
                移除
              </button>
            </article>
          </div>
          <p v-else>尚未上传教学点附件。</p>
        </section>
        <div class="visibility-editor">
          <span>模式可见性 visibility</span>
          <label v-for="mode in modes" :key="mode.key">
            <input v-model="stageForm.visibility[mode.key]" type="checkbox" :disabled="configurationLocked" />
            {{ mode.label }}
          </label>
        </div>
        <label class="checkbox-row">
          <input v-model="stageForm.required" type="checkbox" :disabled="configurationLocked" />
          必做教学点
        </label>
        <button class="panel-primary" type="button" :disabled="configurationLocked" @click="saveStage">
          保存教学点规则
        </button>
      </div>

      <div v-else-if="panelTab === 'step'" class="config-content">
        <div class="config-title">
          <div><small>RECORDED STEP</small><h2>节点配置</h2></div>
          <span v-if="selectedStep">{{ selectedStepIndex + 1 }} / {{ selectedStage?.recordedSteps.length }}</span>
        </div>
        <template v-if="selectedStep">
          <label>
            <span>步骤标题</span>
            <input
              :value="selectedStep.title"
              :disabled="configurationLocked"
              @change="updateRecordedStep(selectedStep.id, { title: inputValue($event) })"
            />
          </label>
          <div class="config-grid">
            <label>
              <span>页面 pageTitle</span>
              <input
                :value="selectedStep.pageTitle"
                :disabled="configurationLocked"
                @change="updateRecordedStep(selectedStep.id, { pageTitle: inputValue($event) })"
              />
            </label>
            <label>
              <span>动作 actionLabel</span>
              <input
                :value="selectedStep.actionLabel"
                :disabled="configurationLocked"
                @change="updateRecordedStep(selectedStep.id, { actionLabel: inputValue($event) })"
              />
            </label>
          </div>
          <label>
            <span>元素选择器 selector</span>
            <input
              class="mono"
              :value="selectedStep.selector"
              :disabled="configurationLocked"
              @change="updateRecordedStep(selectedStep.id, { selector: inputValue($event) })"
            />
          </label>
          <section class="element-binding-editor">
            <div>
              <strong>页面元素绑定</strong>
              <small>
                可像 Chrome 检查元素一样重新选择页面上的任意标题、文字、表格或控件。
              </small>
            </div>
            <button
              type="button"
              :disabled="configurationLocked"
              @click="startElementPick(selectedStep.id)"
            >
              ⌖ 重新选择元素
            </button>
          </section>
          <label>
            <span>逐步讲解</span>
            <textarea
              :value="selectedStep.teachingText ?? selectedStep.note"
              rows="4"
              :disabled="configurationLocked"
              @change="
                updateRecordedStep(selectedStep.id, {
                  teachingText: inputValue($event),
                  note: inputValue($event)
                })
              "
            />
          </label>
          <div class="config-grid">
            <label>
              <span>时长 durationSeconds</span>
              <input
                :value="selectedStep.durationSeconds"
                type="number"
                min="1"
                :disabled="configurationLocked"
                @change="updateRecordedStep(selectedStep.id, { durationSeconds: numberValue($event) })"
              />
            </label>
            <label>
              <span>失败策略</span>
              <select
                :value="selectedStep.failurePolicy ?? 'stop'"
                :disabled="configurationLocked"
                @change="
                  updateRecordedStep(selectedStep.id, {
                    failurePolicy: inputValue($event) as 'stop' | 'retry' | 'skip'
                  })
                "
              >
                <option value="stop">停止并等待处理</option>
                <option value="retry">允许重试</option>
                <option value="skip">允许跳过</option>
              </select>
            </label>
          </div>
          <label class="checkbox-row">
            <input
              type="checkbox"
              :checked="selectedStep.required ?? true"
              :disabled="configurationLocked"
              @change="
                updateRecordedStep(selectedStep.id, {
                  required: ($event.target as HTMLInputElement).checked
                })
              "
            />
            必做节点
          </label>
          <section class="attachment-editor">
            <div class="attachment-editor__heading">
              <span>节点附件</span>
              <label class="attachment-upload">
                <input
                  type="file"
                  multiple
                  :disabled="configurationLocked"
                  @change="uploadStepAttachments"
                />
                ＋ 上传附件
              </label>
            </div>
            <small>附件将在教师讲解与学生学习该节点时显示，可弹窗预览并最小化到右侧栏。</small>
            <div v-if="selectedStep.attachments?.length" class="attachment-editor__list">
              <article v-for="attachment in selectedStep.attachments" :key="attachment.id">
                <span>附件</span>
                <div>
                  <strong>{{ attachment.name }}</strong>
                  <small>
                    {{ attachment.mimeType }} · {{ formatAttachmentSize(attachment.size) }}
                  </small>
                </div>
                <button
                  type="button"
                  :disabled="configurationLocked"
                  @click="removeStepAttachment(attachment.id)"
                >
                  移除
                </button>
              </article>
            </div>
            <p v-else>尚未上传节点附件。</p>
          </section>
          <div class="node-actions">
            <button
              v-if="
                store.remote.enabled &&
                lesson.captureSessionId &&
                selectedStep.syncStatus !== 'SYNCED'
              "
              type="button"
              :disabled="selectedStep.syncStatus === 'SYNCING'"
              @click="queueStepSync(selectedStage?.id ?? '', selectedStep.id)"
            >
              {{ selectedStep.syncStatus === 'SYNCING' ? '正在同步…' : '重试后端同步' }}
            </button>
            <button type="button" @click="moveRecordedStep('up')">↑ 上移</button>
            <button type="button" @click="moveRecordedStep('down')">↓ 下移</button>
            <button class="danger-text" type="button" @click="removeRecordedStep(selectedStep.id)">删除节点</button>
          </div>
        </template>
        <div v-else class="config-empty">
          请选择一个已录制节点，或操作下层业务系统生成新节点。
        </div>
      </div>

      <div v-else-if="panelTab === 'lesson'" class="config-content">
        <div class="config-title">
          <div><small>LESSON SETTINGS</small><h2>教案基础配置</h2></div>
          <span>V{{ lesson.version }}</span>
        </div>
        <div class="config-grid">
          <label><span>教案编号</span><input v-model="basicForm.code" :disabled="configurationLocked" /></label>
          <label><span>教案名称</span><input v-model="basicForm.title" :disabled="configurationLocked" /></label>
        </div>
        <label>
          <span>录制业务平台</span>
          <select v-model="basicForm.businessPlatformId" :disabled="configurationLocked">
            <option
              v-for="platform in store.state.businessPlatforms.filter((item) => item.status === 'ENABLED' || item.id === basicForm.businessPlatformId)"
              :key="platform.id"
              :value="platform.id"
            >
              {{ platform.name }} · {{ platform.baseUrl }}
            </option>
          </select>
        </label>
        <label>
          <span>录制平台模块</span>
          <select
            v-model="basicForm.businessPlatformModuleId"
            :disabled="configurationLocked || !basicForm.businessPlatformId"
          >
            <option value="" disabled>
              {{
                basicForm.businessPlatformId
                  ? '请选择业务平台下的模块'
                  : '请先选择业务平台'
              }}
            </option>
            <option
              v-for="businessModule in availableBasicBusinessModules"
              :key="businessModule.id"
              :value="businessModule.id"
            >
              {{ businessModule.name }} · {{ businessModule.path }}
            </option>
          </select>
          <small v-if="basicForm.businessPlatformId && !availableBasicBusinessModules.length">
            当前平台没有可用模块，请先由管理员在业务平台管理中新增。
          </small>
        </label>
        <label><span>教学简介</span><textarea v-model="basicForm.description" rows="4" :disabled="configurationLocked" /></label>
        <div class="config-grid">
          <label><span>客观分上限</span><input v-model.number="basicForm.objectiveMaxScore" type="number" min="0" :disabled="configurationLocked" /></label>
          <label><span>主观分上限</span><input v-model.number="basicForm.subjectiveMaxScore" type="number" min="0" :disabled="configurationLocked" /></label>
        </div>
        <label><span>标签（逗号分隔）</span><input v-model="basicForm.tags" :disabled="configurationLocked" /></label>
        <div class="score-strip">
          <span><small>教案总分</small><strong>{{ totalScore }}</strong></span>
          <span><small>教学点分合计</small><strong>{{ objectiveStageScore }}</strong></span>
          <span><small>录制节点</small><strong>{{ recordedStepCount }}</strong></span>
        </div>
        <button class="panel-primary" type="button" :disabled="configurationLocked" @click="saveBasicInformation">
          保存基础信息
        </button>
      </div>

      <div v-else class="config-content">
        <div class="config-title">
          <div><small>PUBLISH CHECK</small><h2>发布校验</h2></div>
          <span>{{ validationMessages.length ? `${validationMessages.length} 项待处理` : '校验通过' }}</span>
        </div>
        <p class="publish-description">
          发布前检查基础信息、教学点分值、角色与录制步骤；通过后再进入考试设置。
        </p>
        <div v-if="validationMessages.length" class="validation-list">
          <span v-for="message in validationMessages" :key="message">! {{ message }}</span>
        </div>
        <div v-else class="validation-ok">✓ 教案完整，可执行发布</div>
        <button class="panel-primary" type="button" :disabled="configurationLocked" @click="publishLesson">
          执行发布校验
        </button>
        <RouterLink
          class="panel-next"
          :to="{ name: 'exam-setup', params: { lessonId: lesson.id } }"
        >
          下一步：考试设置 →
        </RouterLink>
      </div>
    </aside>

    <div
      v-if="!controlsCollapsed && !elementPicking && !showStagePanel && !showConfigPanel"
      class="quick-controls"
      aria-label="编排快捷工具坞"
    >
      <button v-if="recording" type="button" class="recording-action" @click="startElementPick()">
        ⌖ 继续选取
      </button>
      <button v-if="recording" type="button" @click="pauseRecording">Ⅱ 暂停录制</button>
      <button type="button" @click="toggleStagePanel">
        ☰ 教学点
      </button>
      <button type="button" @click="toggleConfigPanel('step')">
        ◆ 节点配置
      </button>
      <button type="button" @click="toggleConfigPanel('lesson')">⚙ 教案设置</button>
      <button type="button" @click="toggleConfigPanel('publish')">✓ 发布校验</button>
    </div>

    <footer
      v-if="!controlsCollapsed && !recording && !elementPicking && !showStagePanel && !showConfigPanel"
      class="authoring-status"
      aria-live="polite"
    >
      <span>{{ activityText }}</span>
      <strong>{{ statusSummary }}</strong>
    </footer>

    <button
      v-if="!elementPicking && !showStagePanel && !showConfigPanel"
      class="collapse-controls"
      type="button"
      @click="controlsCollapsed = !controlsCollapsed"
    >
      {{ controlsCollapsed ? '展开工具' : '隐藏工具' }}
    </button>

    <div v-if="feedback && !controlsCollapsed && !elementPicking" class="authoring-toast" :class="feedbackTone">
      {{ feedback }}
      <button type="button" aria-label="关闭提示" @click="feedback = ''">×</button>
    </div>

    <div v-if="configurationLocked" class="locked-shield">
      <strong>当前教案版本已冻结</strong>
      <span>该教案已有考试任务，只能查看下层业务界面和已录制节点；请复制为新版本后再修改。</span>
      <RouterLink class="button primary" :to="{ name: 'lesson-list' }">返回教案列表</RouterLink>
    </div>
  </section>

  <section v-else class="standalone-state">
    <span>404</span>
    <h1>教案不存在</h1>
    <p>该教案可能已被删除或链接已失效。</p>
    <RouterLink class="button primary" :to="{ name: 'lesson-list' }">返回教案列表</RouterLink>
  </section>
</template>

<style scoped>
.authoring-workspace {
  position: relative;
  height: 100vh;
  min-height: 620px;
  overflow: hidden;
  color: #172033;
  background: #eef2f7;
}

.attachment-editor {
  display: grid;
  gap: 8px;
  border: 1px solid #e2e5ee;
  border-radius: 10px;
  padding: 11px;
  background: #fafbfe;
}

.attachment-editor__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.attachment-editor__heading > span {
  font-size: 11px;
  font-weight: 850;
}

.attachment-upload {
  display: inline-flex !important;
  min-height: 28px;
  align-items: center;
  border: 1px solid #d9d3ff;
  border-radius: 7px;
  padding: 0 9px;
  color: #5d4bd8;
  background: #f0edff;
  cursor: pointer;
  font-size: 9px;
  font-weight: 800;
}

.attachment-upload input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}

.attachment-editor > small,
.attachment-editor > p {
  margin: 0;
  color: #8b94a5;
  font-size: 9px;
}

.attachment-editor__list {
  display: grid;
  gap: 6px;
}

.attachment-editor__list article {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  border-radius: 8px;
  padding: 7px;
  background: #fff;
}

.attachment-editor__list article > span {
  border-radius: 6px;
  padding: 7px 5px;
  color: #6553df;
  background: #efecff;
  font-size: 8px;
  font-weight: 850;
}

.attachment-editor__list article > div {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.attachment-editor__list strong,
.attachment-editor__list small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-editor__list strong {
  font-size: 10px;
}

.attachment-editor__list small {
  color: #8992a2;
  font-size: 8px;
}

.attachment-editor__list button {
  border: 0;
  color: #b33a55;
  background: transparent;
  font-size: 9px;
  font-weight: 800;
}

.business-layer {
  position: absolute;
  inset: 0;
  overflow: hidden;
  background: #f2f5f9;
}

.business-layer.element-picking,
.business-layer.element-picking * {
  cursor: crosshair !important;
}

.configured-business-frame {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
  background: #fff;
}

.authoring-launch-state {
  position: absolute;
  inset: 0;
  z-index: 2;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 12px;
  padding: 32px;
  text-align: center;
  background: #f2f5f9;
}

.authoring-launch-state > span {
  display: grid;
  width: 48px;
  height: 48px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  font-size: 24px;
  font-weight: 800;
  background: #087f70;
}

.authoring-launch-state h2,
.authoring-launch-state p {
  margin: 0;
}

.authoring-launch-state p {
  max-width: 560px;
  color: #667085;
}

.authoring-launch-state button {
  border: 0;
  border-radius: 8px;
  padding: 10px 18px;
  color: #fff;
  background: #087f70;
  cursor: pointer;
}

.business-header {
  display: grid;
  grid-template-columns: minmax(230px, 0.8fr) minmax(260px, 1.2fr) minmax(280px, 0.8fr);
  height: 66px;
  align-items: center;
  gap: 24px;
  border-bottom: 1px solid #dfe5ec;
  padding: 0 24px;
  background: #fff;
}

.business-brand,
.business-user,
.business-heading {
  display: flex;
  align-items: center;
}

.business-brand {
  gap: 10px;
}

.business-brand__mark {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 9px;
  color: #fff;
  background: #087f70;
  font-size: 15px;
  font-weight: 900;
}

.business-brand > span:last-child,
.business-user > span:last-child {
  display: grid;
  gap: 2px;
}

.business-brand strong {
  font-size: 14px;
}

.business-brand small {
  color: #99a3b1;
  font-size: 8px;
  letter-spacing: 0.12em;
}

.business-search {
  position: relative;
  display: block;
}

.business-search > span {
  position: absolute;
  z-index: 1;
  left: 13px;
  top: 50%;
  color: #8d98a8;
  transform: translateY(-50%);
}

.business-search input {
  border-color: #e2e7ed;
  border-radius: 7px;
  padding-left: 38px;
  background: #f7f9fb;
  font-size: 12px;
}

.business-user {
  justify-content: flex-end;
  gap: 9px;
}

.business-user__status {
  margin-right: 10px;
  color: #16806f;
  font-size: 10px;
}

.business-user__status::before {
  display: inline-block;
  width: 6px;
  height: 6px;
  margin-right: 6px;
  border-radius: 50%;
  background: #12a879;
  content: "";
}

.business-avatar {
  display: grid;
  width: 31px;
  height: 31px;
  place-items: center;
  border-radius: 8px;
  color: #087f70;
  background: #dff6f1;
  font-size: 11px;
  font-weight: 900;
}

.business-user strong {
  font-size: 11px;
}

.business-user small {
  color: #929cad;
  font-size: 9px;
}

.business-body {
  display: grid;
  grid-template-columns: 208px minmax(0, 1fr);
  height: calc(100% - 66px);
}

.business-sidebar {
  display: flex;
  flex-direction: column;
  border-right: 1px solid #dde4ec;
  padding: 22px 13px;
  background: #202c3b;
  color: #d8e0e9;
}

.business-nav-title {
  margin: 0 10px 11px;
  color: #8190a2;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.1em;
}

.business-sidebar nav {
  display: grid;
  gap: 4px;
}

.business-sidebar nav button {
  display: grid;
  grid-template-columns: 24px 1fr auto;
  min-height: 42px;
  align-items: center;
  border: 0;
  padding: 0 11px;
  color: #aeb9c7;
  background: transparent;
  text-align: left;
  font-size: 11px;
}

.business-sidebar nav button:hover,
.business-sidebar nav button.active {
  color: #fff;
  background: rgb(255 255 255 / 9%);
  box-shadow: none;
  transform: none;
}

.business-sidebar nav i {
  font-style: normal;
}

.business-sidebar nav em {
  display: grid;
  width: 18px;
  height: 18px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #c65e5e;
  font-style: normal;
  font-size: 8px;
}

.business-sidebar__help {
  display: grid;
  gap: 7px;
  margin-top: auto;
  border: 1px solid rgb(255 255 255 / 8%);
  border-radius: 9px;
  padding: 12px;
  color: #9da9b8;
  background: rgb(255 255 255 / 4%);
  font-size: 9px;
  line-height: 1.6;
}

.business-sidebar__help strong {
  color: #dce4ed;
  font-size: 10px;
}

.business-content {
  overflow: auto;
  padding: 19px 26px 110px;
}

.business-breadcrumb {
  margin-bottom: 13px;
  color: #8d98a8;
  font-size: 9px;
}

.business-page-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
}

.business-page-title h1 {
  margin: 0;
  font-size: 23px;
}

.business-page-title p {
  margin: 5px 0 0;
  color: #7c8798;
  font-size: 10px;
}

.business-primary,
.business-dark {
  color: #fff;
}

.business-primary {
  border-color: #087f70;
  background: #087f70;
}

.business-dark {
  border-color: #2c3a4d;
  background: #2c3a4d;
}

.business-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 11px;
  margin: 16px 0;
}

.business-stats > span {
  display: grid;
  grid-template-columns: 1fr auto;
  align-items: end;
  gap: 4px 10px;
  border: 1px solid #e0e6ed;
  border-radius: 9px;
  padding: 12px 14px;
  background: #fff;
}

.business-stats small {
  grid-column: 1 / -1;
  color: #7b8797;
  font-size: 9px;
}

.business-stats strong {
  font-size: 18px;
}

.business-stats em {
  color: #148472;
  font-style: normal;
  font-size: 8px;
}

.business-document-grid {
  display: grid;
  grid-template-columns: minmax(520px, 1fr) 250px;
  align-items: start;
  gap: 14px;
}

.business-card {
  border: 1px solid #dfe5ec;
  border-radius: 9px;
  background: #fff;
  box-shadow: 0 4px 12px rgb(29 43 62 / 3%);
}

.business-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #e7ebf0;
  padding: 16px 18px;
}

.business-card__header > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.business-card h2 {
  margin: 0;
  font-size: 15px;
}

.business-tag {
  display: inline-flex;
  width: max-content;
  border-radius: 999px;
  padding: 4px 8px;
  color: #087f70;
  background: #dff7f2;
  font-size: 9px;
  font-weight: 800;
}

.business-document-no {
  color: #909aa9;
  font-size: 9px;
}

.business-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 16px;
  padding: 18px;
}

.business-form label {
  color: #556274;
  font-size: 10px;
}

.business-form label > span {
  font-weight: 700;
}

.business-form label i {
  color: #d34e59;
  font-style: normal;
}

.business-form input,
.business-form select,
.business-form textarea {
  border-radius: 6px;
  font-size: 10px;
}

.business-form .wide {
  grid-column: 1 / -1;
}

.amount-input {
  position: relative;
}

.amount-input b {
  position: absolute;
  z-index: 1;
  top: 50%;
  left: 11px;
  color: #5c6879;
  transform: translateY(-50%);
}

.amount-input input {
  padding-left: 27px;
}

.business-form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #e7ebf0;
  padding: 13px 18px;
}

.business-form-actions button {
  min-height: 35px;
  border-radius: 6px;
  font-size: 10px;
}

.business-summary {
  padding: 17px;
}

.business-summary h2 {
  margin: 9px 0 17px;
  font-size: 16px;
}

.business-process {
  display: grid;
  gap: 0;
}

.business-process > span {
  position: relative;
  display: grid;
  grid-template-columns: 27px 1fr;
  gap: 2px 9px;
  min-height: 57px;
}

.business-process > span:not(:last-child)::after {
  position: absolute;
  top: 25px;
  bottom: 2px;
  left: 13px;
  width: 1px;
  background: #dfe5eb;
  content: "";
}

.business-process i {
  display: grid;
  grid-row: 1 / 3;
  width: 27px;
  height: 27px;
  place-items: center;
  border: 1px solid #d8dfe7;
  border-radius: 50%;
  color: #8995a5;
  font-style: normal;
  font-size: 9px;
}

.business-process .done i {
  border-color: #0e8a76;
  color: #fff;
  background: #0e8a76;
}

.business-process b {
  font-size: 10px;
}

.business-process small {
  color: #919baa;
  font-size: 8px;
}

.business-summary__notice {
  margin-top: 7px;
  border-radius: 7px;
  padding: 11px;
  color: #6e7887;
  background: #f5f8fa;
  font-size: 9px;
}

.business-summary__notice strong {
  color: #415064;
}

.business-summary__notice p {
  margin: 5px 0 0;
  line-height: 1.6;
}

.authoring-commandbar,
.glass-panel,
.quick-controls,
.authoring-status,
.collapse-controls,
.authoring-toast {
  position: absolute;
  z-index: 20;
}

.authoring-commandbar {
  top: 12px;
  left: 12px;
  right: 12px;
  display: flex;
  min-height: 48px;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border: 1px solid rgb(210 217 226 / 78%);
  border-radius: 10px;
  padding: 7px 9px 7px 13px;
  background: rgb(249 251 253 / 74%);
  box-shadow: 0 10px 30px rgb(27 38 55 / 10%);
  backdrop-filter: blur(12px) saturate(1.12);
}

.authoring-heading {
  display: flex;
  min-width: 260px;
  align-items: center;
  gap: 9px;
}

.authoring-heading > div {
  display: grid;
  gap: 2px;
}

.authoring-heading small {
  color: #7a8798;
  font-size: 8px;
}

.authoring-heading strong {
  overflow: hidden;
  max-width: 270px;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recording-dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #a9b2be;
}

.recording-dot.active {
  background: #e35360;
  box-shadow: 0 0 0 4px rgb(227 83 96 / 14%);
  animation: recording-pulse 1.5s infinite;
}

.version-badge {
  border: 1px solid #d8dee7;
  border-radius: 999px;
  padding: 3px 6px;
  color: #758194;
  background: rgb(255 255 255 / 72%);
  font-size: 8px;
}

.platform-context-badge {
  overflow: hidden;
  max-width: 150px;
  border: 1px solid #cde6df;
  border-radius: 999px;
  padding: 3px 7px;
  color: #087b69;
  background: rgb(232 249 244 / 82%);
  font-size: 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.command-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
}

.command-actions button,
.command-link {
  display: inline-flex;
  min-height: 31px;
  align-items: center;
  border: 1px solid rgb(54 67 84 / 20%);
  border-radius: 7px;
  padding: 0 10px;
  color: #fff;
  background: rgb(43 55 72 / 78%);
  box-shadow: 0 5px 14px rgb(26 37 52 / 13%);
  font-size: 9px;
  font-weight: 800;
  white-space: nowrap;
}

.command-actions button:hover:not(:disabled),
.command-link:hover {
  border-color: rgb(43 55 72 / 55%);
  background: rgb(31 42 57 / 94%);
  box-shadow: 0 7px 17px rgb(26 37 52 / 20%);
  transform: translateY(-1px);
}

.command-actions .command-primary {
  border-color: rgb(90 72 223 / 60%);
  background: rgb(91 73 224 / 88%);
}

.command-actions button.active {
  border-color: #7765ef;
  background: #604ddd;
}

.glass-panel {
  border: 1px solid rgb(207 216 226 / 86%);
  border-radius: 10px;
  background: rgb(247 250 252 / 82%);
  box-shadow: 0 16px 42px rgb(22 34 50 / 16%);
  backdrop-filter: blur(16px) saturate(1.1);
}

.stage-panel {
  z-index: 30;
  top: 12px;
  bottom: 12px;
  left: 12px;
  width: 310px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  min-height: 56px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid rgb(210 218 227 / 72%);
  padding: 10px 12px;
}

.panel-header small,
.config-title small {
  color: #6f5ce4;
  font-size: 7px;
  font-weight: 900;
  letter-spacing: 0.12em;
}

.panel-header h2,
.config-title h2 {
  margin: 2px 0 0;
  font-size: 12px;
}

.panel-header button {
  min-height: 28px;
  border-color: #d3d9e2;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 8px;
}

.panel-header-actions {
  display: flex;
  align-items: center;
  gap: 6px;
}

.panel-header-actions button:last-child {
  width: 28px;
  padding: 0;
  font-size: 16px;
}

.stage-list {
  height: calc(100% - 101px);
  overflow: auto;
  padding: 8px;
}

.stage-item {
  overflow: hidden;
  border: 1px solid transparent;
  border-radius: 8px;
}

.stage-item + .stage-item {
  margin-top: 5px;
}

.stage-item.active {
  border-color: rgb(106 88 229 / 22%);
  background: rgb(242 240 255 / 72%);
}

.stage-item > button {
  display: grid;
  grid-template-columns: 26px minmax(0, 1fr) auto;
  width: 100%;
  min-height: 51px;
  align-items: center;
  gap: 8px;
  border: 0;
  border-radius: 0;
  padding: 7px 8px;
  background: transparent;
  text-align: left;
}

.stage-item > button:hover {
  box-shadow: none;
  transform: none;
}

.stage-item > button > span:first-child {
  display: grid;
  width: 25px;
  height: 25px;
  place-items: center;
  border-radius: 7px;
  color: #6655d7;
  background: #e7e3ff;
  font-size: 9px;
  font-weight: 900;
}

.stage-item > button > span:nth-child(2) {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.stage-item > button strong {
  overflow: hidden;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.stage-item > button small {
  color: #818c9d;
  font-size: 8px;
}

.stage-item > button em {
  display: grid;
  min-width: 19px;
  height: 19px;
  place-items: center;
  border-radius: 999px;
  color: #687589;
  background: #e7ebf0;
  font-style: normal;
  font-size: 8px;
}

.step-list {
  display: grid;
  gap: 3px;
  border-top: 1px solid rgb(215 220 230 / 55%);
  padding: 6px 7px 8px 32px;
}

.step-list > button {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) auto;
  min-height: 40px;
  align-items: center;
  gap: 7px;
  border: 1px solid transparent;
  border-radius: 6px;
  padding: 5px 6px;
  background: rgb(255 255 255 / 48%);
  text-align: left;
}

.step-list > button:hover,
.step-list > button.active {
  border-color: #cfc7ff;
  background: rgb(255 255 255 / 88%);
  box-shadow: none;
  transform: none;
}

.step-list i {
  display: grid;
  width: 19px;
  height: 19px;
  place-items: center;
  border-radius: 5px;
  color: #596679;
  background: #e8ecf2;
  font-style: normal;
  font-size: 7px;
}

.step-list > button > span {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.step-list strong {
  overflow: hidden;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.step-list small {
  color: #8994a5;
  font-size: 7px;
}

.step-list b {
  color: #6754dd;
  font-size: 7px;
}

.step-empty,
.stage-empty {
  padding: 15px 7px;
  color: #7e8999;
  font-size: 8px;
  line-height: 1.6;
  text-align: center;
}

.stage-panel-actions {
  display: flex;
  height: 45px;
  align-items: center;
  gap: 5px;
  border-top: 1px solid rgb(210 218 227 / 72%);
  padding: 7px 8px;
}

.stage-panel-actions button {
  min-height: 27px;
  flex: 1;
  border-color: #d5dbe4;
  border-radius: 6px;
  padding: 0 6px;
  font-size: 8px;
}

.config-panel {
  z-index: 30;
  top: 12px;
  right: 12px;
  bottom: 12px;
  width: 390px;
  overflow: hidden;
}

.config-tabs {
  display: grid;
  grid-template-columns: repeat(4, 1fr) 36px;
  min-height: 43px;
  align-items: stretch;
  border-bottom: 1px solid rgb(210 218 227 / 72%);
  padding: 0 7px;
}

.config-tabs button {
  min-height: 42px;
  border: 0;
  border-bottom: 2px solid transparent;
  border-radius: 0;
  padding: 0 8px;
  color: #758194;
  background: transparent;
  font-size: 8px;
}

.config-tabs button:hover {
  box-shadow: none;
  transform: none;
}

.config-tabs button.active {
  border-bottom-color: #6856df;
  color: #5745d1;
}

.config-tabs .config-close {
  font-size: 18px;
}

.config-content {
  display: grid;
  max-height: calc(100% - 43px);
  gap: 11px;
  overflow: auto;
  padding: 14px;
}

.element-binding-editor {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #dcd6ff;
  border-radius: 9px;
  padding: 10px;
  background: #f4f2ff;
}

.element-binding-editor > div {
  display: grid;
  gap: 3px;
}

.element-binding-editor strong {
  color: #4938bd;
  font-size: 10px;
}

.element-binding-editor small {
  color: #746b9a;
  font-size: 8px;
  line-height: 1.5;
}

.element-binding-editor button {
  min-height: 30px;
  flex: none;
  border-color: #cfc6ff;
  color: #5542ce;
  background: #fff;
  font-size: 8px;
  font-weight: 850;
}

.config-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.config-title > span {
  color: #7c8798;
  font-size: 8px;
}

.config-content label {
  gap: 5px;
  font-size: 9px;
}

.config-content input,
.config-content select,
.config-content textarea {
  border-radius: 6px;
  padding: 8px 9px;
  font-size: 9px;
}

.config-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 9px;
}

.visibility-editor {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  border-radius: 7px;
  padding: 9px;
  background: rgb(237 241 246 / 85%);
}

.visibility-editor > span {
  width: 100%;
  color: #596679;
  font-size: 9px;
  font-weight: 800;
}

.visibility-editor label,
.checkbox-row {
  display: flex;
  align-items: center;
  gap: 5px;
}

.visibility-editor input,
.checkbox-row input {
  width: auto;
}

.panel-primary,
.panel-next {
  display: flex;
  min-height: 34px;
  align-items: center;
  justify-content: center;
  border: 1px solid #6552dd;
  border-radius: 7px;
  color: #fff;
  background: #6552dd;
  font-size: 9px;
  font-weight: 800;
}

.danger-text {
  min-height: 27px;
  border-color: #ffd5d9;
  padding: 0 8px;
  color: #c7434f;
  background: #fff4f5;
  font-size: 8px;
}

.node-actions {
  display: flex;
  gap: 6px;
}

.node-actions button {
  min-height: 29px;
  flex: 1;
  border-color: #d5dbe4;
  padding: 0 7px;
  font-size: 8px;
}

.mono {
  font-family: "Cascadia Code", Consolas, monospace;
}

.score-strip {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 6px;
}

.score-strip > span {
  display: grid;
  gap: 3px;
  border-radius: 7px;
  padding: 9px;
  background: #eef2f6;
}

.score-strip small {
  color: #7c8798;
  font-size: 7px;
}

.score-strip strong {
  font-size: 15px;
}

.publish-description {
  margin: 0;
  color: #6f7b8c;
  font-size: 9px;
  line-height: 1.65;
}

.validation-list {
  display: grid;
  gap: 6px;
}

.validation-list span,
.validation-ok {
  border-radius: 7px;
  padding: 9px;
  font-size: 8px;
}

.validation-list span {
  color: #b13f4a;
  background: #fff2f3;
}

.validation-ok {
  color: #08755f;
  background: #e8f8f3;
}

.config-empty {
  display: grid;
  min-height: 180px;
  place-items: center;
  color: #7b8798;
  font-size: 9px;
  line-height: 1.7;
  text-align: center;
}

.quick-controls {
  top: 50%;
  right: 12px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  border: 1px solid rgb(255 255 255 / 18%);
  border-radius: 11px;
  padding: 5px;
  background: rgb(30 41 57 / 44%);
  box-shadow: 0 12px 30px rgb(18 28 42 / 18%);
  backdrop-filter: blur(12px);
  transform: translateY(-50%);
}

.quick-controls button,
.collapse-controls {
  min-height: 31px;
  border-color: rgb(44 57 75 / 22%);
  border-radius: 7px;
  padding: 0 9px;
  color: #fff;
  background: rgb(45 58 77 / 72%);
  box-shadow: 0 6px 17px rgb(25 36 51 / 13%);
  backdrop-filter: blur(10px);
  font-size: 8px;
}

.quick-controls button {
  display: flex;
  width: 82px;
  align-items: center;
  justify-content: flex-start;
  text-align: left;
}

.quick-controls .recording-action {
  border-color: rgb(156 140 255 / 58%);
  background: rgb(91 73 224 / 90%);
}

.quick-controls button.active {
  background: rgb(91 73 224 / 82%);
}

.collapse-controls {
  right: 13px;
  bottom: 13px;
  min-width: 96px;
}

.authoring-status {
  bottom: 13px;
  left: 12px;
  display: flex;
  width: min(650px, calc(100% - 610px));
  min-height: 32px;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
  border: 1px solid rgb(208 217 227 / 80%);
  border-radius: 8px;
  padding: 6px 10px;
  color: #566276;
  background: rgb(248 250 252 / 74%);
  box-shadow: 0 7px 20px rgb(28 40 57 / 9%);
  backdrop-filter: blur(12px);
  font-size: 8px;
}

.authoring-status span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.authoring-status strong {
  color: #445064;
  white-space: nowrap;
}

.authoring-toast {
  top: 76px;
  left: 50%;
  display: flex;
  min-width: 280px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid #bce6da;
  border-radius: 8px;
  padding: 9px 10px 9px 13px;
  color: #08755f;
  background: rgb(235 250 246 / 94%);
  box-shadow: 0 12px 30px rgb(23 52 45 / 14%);
  font-size: 9px;
  transform: translateX(-50%);
}

.authoring-toast.danger {
  border-color: #f1c3c7;
  color: #b23d48;
  background: rgb(255 242 243 / 96%);
}

.authoring-toast button {
  min-height: 22px;
  border: 0;
  padding: 0 4px;
  color: inherit;
  background: transparent;
}

.element-picker-highlight {
  position: absolute;
  z-index: 42;
  border: 2px solid #6d5dfc;
  border-radius: 4px;
  background: rgb(109 93 252 / 12%);
  box-shadow:
    0 0 0 1px rgb(255 255 255 / 84%),
    0 0 0 5px rgb(109 93 252 / 14%);
  pointer-events: none;
}

.element-picker-highlight > span {
  position: absolute;
  bottom: calc(100% + 4px);
  left: -2px;
  overflow: hidden;
  max-width: min(520px, 76vw);
  border-radius: 5px 5px 5px 0;
  padding: 5px 8px;
  color: #fff;
  background: #5948dc;
  font: 700 10px/1.35 Consolas, monospace;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.element-picker-toolbar {
  position: absolute;
  z-index: 45;
  top: 14px;
  right: 14px;
  display: flex;
  max-width: min(560px, calc(100% - 28px));
  align-items: center;
  gap: 7px;
  border: 1px solid rgb(255 255 255 / 38%);
  border-radius: 10px;
  padding: 6px 7px 6px 10px;
  color: #fff;
  background: rgb(67 53 171 / 94%);
  box-shadow: 0 16px 36px rgb(40 28 127 / 30%);
  backdrop-filter: blur(12px);
}

.element-picker-toolbar.position-bottom-right {
  top: auto;
  right: 14px;
  bottom: 14px;
}

.element-picker-toolbar.position-bottom-left {
  top: auto;
  right: auto;
  bottom: 14px;
  left: 14px;
}

.element-picker-toolbar.position-top-left {
  right: auto;
  left: 14px;
}

.element-picker-toolbar span {
  font-size: 10px;
  font-weight: 900;
  white-space: nowrap;
}

.element-picker-toolbar strong {
  font-size: 9px;
  white-space: nowrap;
}

.element-picker-toolbar small {
  display: none;
}

.element-picker-toolbar button {
  min-height: 27px;
  border-color: rgb(255 255 255 / 26%);
  color: #fff;
  background: rgb(255 255 255 / 13%);
  font-size: 8px;
  font-weight: 800;
  white-space: nowrap;
}

.teaching-mask {
  position: absolute;
  z-index: 12;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.target-highlight {
  position: absolute;
  z-index: 1;
  border: 2px solid #6e5df3;
  border-radius: 8px;
  box-shadow:
    0 0 0 9999px rgb(22 31 44 / 31%),
    0 0 0 5px rgb(110 93 243 / 19%),
    0 9px 28px rgb(26 36 52 / 21%);
  transition: all 200ms ease;
}

.target-highlight.target-create {
  top: 103px;
  right: 27px;
  width: 119px;
  height: 40px;
}

.target-highlight.target-subject {
  top: 299px;
  left: 235px;
  width: calc((100% - 529px) / 2);
  height: 65px;
}

.target-highlight.target-category {
  top: 299px;
  left: calc(235px + (100% - 529px) / 2 + 16px);
  width: calc((100% - 529px) / 2);
  height: 65px;
}

.target-highlight.target-counterparty {
  top: 378px;
  left: 235px;
  width: calc((100% - 529px) / 2);
  height: 65px;
}

.target-highlight.target-amount {
  top: 378px;
  left: calc(235px + (100% - 529px) / 2 + 16px);
  width: calc((100% - 529px) / 2);
  height: 65px;
}

.target-highlight.target-date {
  top: 457px;
  left: 235px;
  width: calc((100% - 529px) / 2);
  height: 65px;
}

.target-highlight.target-reason {
  top: 536px;
  left: 235px;
  width: calc(100% - 529px);
  height: 112px;
}

.target-highlight.target-save {
  right: 251px;
  bottom: 117px;
  width: 83px;
  height: 37px;
}

.target-highlight.target-approve {
  right: 157px;
  bottom: 117px;
  width: 87px;
  height: 37px;
}

.target-highlight.target-submit {
  right: 58px;
  bottom: 117px;
  width: 92px;
  height: 37px;
}

.target-highlight.target-generic-0,
.target-highlight.target-generic-1,
.target-highlight.target-generic-2 {
  top: 299px;
  left: 235px;
  width: calc((100% - 529px) / 2);
  height: 65px;
}

.target-highlight.target-generic-1 {
  left: calc(235px + (100% - 529px) / 2 + 16px);
}

.target-highlight.target-generic-2 {
  top: 378px;
}

.teaching-bubble {
  position: absolute;
  z-index: 2;
  top: 159px;
  left: 50%;
  display: grid;
  width: min(330px, calc(100% - 40px));
  gap: 7px;
  border: 1px solid rgb(215 220 231 / 94%);
  border-radius: 10px;
  padding: 13px;
  color: #273246;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 17px 45px rgb(20 31 48 / 22%);
  backdrop-filter: blur(12px);
  transform: translateX(-18%);
  pointer-events: auto;
}

.teaching-bubble > span {
  color: #6855dc;
  font-size: 8px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.teaching-bubble > strong {
  font-size: 13px;
}

.teaching-bubble p {
  margin: 0;
  color: #647084;
  font-size: 9px;
  line-height: 1.6;
}

.teaching-bubble small {
  color: #929cab;
  font-size: 8px;
}

.teaching-bubble > div {
  display: flex;
  justify-content: flex-end;
  gap: 6px;
  margin-top: 2px;
}

.teaching-bubble button {
  min-height: 27px;
  border-color: #d7dce5;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 8px;
}

.locked-shield {
  position: absolute;
  z-index: 50;
  inset: 0;
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 9px;
  padding: 25px;
  color: #7b4d19;
  background: rgb(255 250 235 / 76%);
  backdrop-filter: blur(3px);
  text-align: center;
}

.locked-shield strong {
  font-size: 18px;
}

.locked-shield span {
  max-width: 520px;
  font-size: 11px;
  line-height: 1.7;
}

@keyframes recording-pulse {
  50% {
    box-shadow: 0 0 0 7px rgb(227 83 96 / 4%);
  }
}

@media (max-width: 1250px) {
  .authoring-heading {
    min-width: auto;
  }

  .authoring-heading strong {
    max-width: 150px;
  }

  .command-actions button,
  .command-link {
    padding: 0 7px;
    font-size: 8px;
  }

  .business-document-grid {
    grid-template-columns: 1fr;
  }

  .business-summary {
    display: none;
  }

  .target-highlight.target-subject,
  .target-highlight.target-counterparty,
  .target-highlight.target-date,
  .target-highlight.target-generic-0,
  .target-highlight.target-generic-2 {
    width: calc((100% - 279px) / 2);
  }

  .target-highlight.target-category,
  .target-highlight.target-amount,
  .target-highlight.target-generic-1 {
    left: calc(235px + (100% - 279px) / 2 + 16px);
    width: calc((100% - 279px) / 2);
  }

  .target-highlight.target-reason {
    width: calc(100% - 279px);
  }

  .authoring-status {
    display: none;
  }
}

@media (max-width: 980px) {
  .authoring-workspace {
    min-height: 720px;
  }

  .business-header {
    grid-template-columns: 1fr auto;
  }

  .business-search,
  .business-user__status,
  .business-user > span:last-child {
    display: none;
  }

  .business-body {
    grid-template-columns: 70px minmax(0, 1fr);
  }

  .business-sidebar {
    padding-inline: 8px;
  }

  .business-nav-title,
  .business-sidebar nav button:not(.active),
  .business-sidebar__help,
  .business-sidebar nav button.active {
    font-size: 0;
  }

  .business-sidebar nav button {
    grid-template-columns: 1fr;
    justify-items: center;
    padding: 0;
  }

  .business-sidebar nav i {
    font-size: 13px;
  }

  .business-content {
    padding-inline: 16px;
  }

  .business-stats {
    grid-template-columns: 1fr 1fr;
  }

  .authoring-commandbar {
    align-items: flex-start;
  }

  .command-actions {
    max-width: 520px;
    flex-wrap: wrap;
  }

  .command-actions .command-link:last-child,
  .command-actions button:nth-of-type(3) {
    display: none;
  }

  .stage-panel {
    width: 280px;
  }

  .config-panel {
    width: 350px;
  }

  .target-highlight {
    display: none;
  }
}

@media (max-width: 720px) {
  .authoring-workspace {
    height: 100vh;
    min-height: 650px;
  }

  .business-content {
    padding: 16px 10px 100px;
  }

  .business-stats {
    display: none;
  }

  .business-document-grid {
    margin-top: 14px;
  }

  .business-form {
    grid-template-columns: 1fr;
  }

  .business-form .wide {
    grid-column: auto;
  }

  .authoring-heading,
  .command-actions .command-link,
  .command-actions button:nth-of-type(2),
  .command-actions button:nth-of-type(3),
  .command-actions button:nth-of-type(4) {
    display: none;
  }

  .authoring-commandbar {
    justify-content: flex-end;
  }

  .stage-panel,
  .config-panel {
    top: 8px;
    right: 8px;
    bottom: 8px;
    left: 8px;
    width: auto;
  }

  .config-panel {
    z-index: 25;
  }

  .quick-controls {
    right: 8px;
  }

  .quick-controls button {
    width: 74px;
    padding-inline: 7px;
  }

  .quick-controls button:nth-child(n + 6) {
    display: none;
  }

  .element-picker-toolbar {
    gap: 5px;
  }

  .element-picker-toolbar strong {
    display: none;
  }

  .teaching-bubble {
    top: 125px;
    left: 50%;
    transform: translateX(-50%);
  }
}
</style>
