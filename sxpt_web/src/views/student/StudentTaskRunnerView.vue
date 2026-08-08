<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import AttachmentPanel from '../../components/lesson/AttachmentPanel.vue';
import BusinessCaptureFrame from '../../components/lesson/BusinessCaptureFrame.vue';
import BusinessSnapshotFrame from '../../components/lesson/BusinessSnapshotFrame.vue';
import LessonPlaybackPlayer from '../../components/lesson/LessonPlaybackPlayer.vue';
import PlaybackNavigationTree from '../../components/lesson/PlaybackNavigationTree.vue';
import type { RecordedStep } from '../../domain/models';
import StatusPill from '../../components/ui/StatusPill.vue';
import {
  dataPrepareApi,
  type DataInstanceAllocation,
  type StudentDataLaunchResult
} from '../../services/trainingApi';
import { useTrainingStore } from '../../stores/trainingStore';
import { authApi } from '../../services/trainingApi';
import { resolveLaunchedBusinessFrameUrl } from '../../utils/businessLaunch';
import { isPracticeMonitorableStep } from '../../utils/practiceStep';

interface BusinessActionPayload {
  actionType: 'click' | 'input' | 'select' | 'submit';
  selector: string;
  selectorCandidates?: string[];
  text: string;
  url?: string;
  pageTitle?: string;
  valueMasked?: string;
}

const store = useTrainingStore();
const legacyLearningPlaybackEnabled = false;
store.refreshPublishedTaskStatuses();
const route = useRoute();
const currentStudentId =
  authApi.getSession()?.user.userId ??
  (typeof window === 'undefined'
    ? store.state.studentTasks.find(
        (item) => item.id === String(route.params.taskId)
      )?.studentId ?? ''
    : '');
const message = ref('');
const errorMessage = ref('');
const attemptMessage = ref('');
const launchMessage = ref('');
const launchErrorMessage = ref('');
const launchLoading = ref(false);
const allocationLoading = ref(false);
const launchResult = ref<StudentDataLaunchResult | null>(null);
const launchAllocation = ref<DataInstanceAllocation | null>(null);
const showHelp = ref(true);
const showRunnerMenu = ref(true);
const syncing = ref(false);
const practiceRuntimeLoading = ref(false);
const practiceRuntimeReady = ref(false);
const practiceSubmitting = ref(false);
const practiceFrameRef = ref<InstanceType<typeof BusinessCaptureFrame> | null>(
  null
);
const showPracticeGuide = ref(false);
const practiceHintStageId = ref('');
const practiceHintStepIndex = ref(-1);
const learningStepIndex = ref(0);
const showStageIntroduction = ref(false);
const checks = reactive({
  entered: false,
  located: false,
  reviewed: false
});
const submissionValues = reactive<Record<string, string>>({});

const task = computed(() =>
  store.state.studentTasks.find(
    (item) =>
      item.id === String(route.params.taskId) &&
      item.studentId === currentStudentId
  )
);
const publishedTask = computed(() =>
  store.state.publishedTasks.find(
    (item) => item.id === task.value?.publishedTaskId
  )
);
const lesson = computed(() =>
  store.state.lessons.find((item) => item.id === task.value?.lessonId)
);
const groupPlan = computed(() =>
  task.value ? store.state.groupPlans[task.value.lessonId] : undefined
);
const examSettings = computed(() =>
  task.value ? store.state.examSettings[task.value.lessonId] : undefined
);
const dataItem = computed(() =>
  task.value
    ? (store.state.dataItems[task.value.lessonId] ?? []).find(
        (item) => item.id === task.value?.dataItemId
      )
    : undefined
);
const isLearning = computed(() => task.value?.mode === 'LEARNING');
const isPractice = computed(() => task.value?.mode === 'PRACTICE');
const isExam = computed(() => task.value?.mode === 'EXAM');
const practiceBusinessPlatform = computed(() =>
  lesson.value
    ? store.getBusinessPlatform(lesson.value.businessPlatformId)
    : undefined
);
const practiceBusinessBaseUrl = computed(() => {
  if (!lesson.value || !practiceBusinessPlatform.value) return '';
  const platform = practiceBusinessPlatform.value;
  if (platform.baseUrl.startsWith('internal://')) return '';
  const businessModule = store.getBusinessPlatformModule(
    lesson.value.businessPlatformId,
    lesson.value.businessPlatformModuleId
  );
  try {
    return new URL(
      businessModule?.path || platform.baseUrl,
      platform.baseUrl
    ).href;
  } catch {
    return platform.baseUrl;
  }
});
const practiceBusinessUrl = computed(
  () => resolveLaunchedBusinessFrameUrl(launchResult.value)
);
const practiceAllowedOrigins = computed(() =>
  [
    practiceBusinessPlatform.value?.baseUrl,
    practiceBusinessBaseUrl.value,
    launchResult.value?.targetUrl,
    launchResult.value?.launchUrl
  ].filter((url): url is string => Boolean(url))
);

const visibleStages = computed(() => {
  if (!lesson.value || !task.value) return [];
  if (task.value.mode === 'LEARNING') {
    return lesson.value.stages;
  }
  return lesson.value.stages.filter((stage) => stage.visibility[task.value!.mode]);
});
const assignedStageIds = computed(() => {
  if (!task.value) return new Set<string>();
  const visibleIds = new Set(visibleStages.value.map((stage) => stage.id));
  const ids = groupPlan.value?.roles
    .filter((role) => task.value?.groupKeys.includes(role.key))
    .flatMap((role) => role.stageIds)
    .filter((id) => visibleIds.has(id));
  if (ids?.length) return new Set(ids);
  return new Set(
    visibleStages.value
      .filter((stage) => task.value?.groupKeys.includes(stage.groupKey))
      .map((stage) => stage.id)
  );
});
const playbackStages = computed(() =>
  visibleStages.value.filter((stage) =>
    stage.recordedSteps.some(
      (step) => !isPractice.value || isPracticeMonitorableStep(step)
    )
  )
);
const playbackStageIds = computed(() => {
  if (
    task.value?.mode === 'LEARNING' ||
    task.value?.mode === 'PRACTICE'
  ) {
    return new Set(playbackStages.value.map((stage) => stage.id));
  }
  return new Set(
    playbackStages.value
      .filter((stage) => assignedStageIds.value.has(stage.id))
      .map((stage) => stage.id)
  );
});
const navigationTeachingPoints = computed(() =>
  playbackStages.value.filter((stage) => playbackStageIds.value.has(stage.id))
);
const learningSteps = computed(() =>
  navigationTeachingPoints.value
    .flatMap((stage) =>
      stage.recordedSteps.map((step, stepIndex) => ({
        stage,
        step,
        stepIndex
      }))
    )
    .filter(({ step }) => !isPractice.value || isPracticeMonitorableStep(step))
);
const currentLearningStep = computed(
  () => learningSteps.value[learningStepIndex.value]
);
const displayedLearningStep = computed(
  () =>
    currentLearningStep.value ??
    learningSteps.value[Math.max(0, learningSteps.value.length - 1)]
);
const selectedPracticeHint = computed(() => {
  const selected = learningSteps.value[practiceHintStepIndex.value];
  return selected?.stage.id === practiceHintStageId.value
    ? selected
    : undefined;
});
const practiceHintInstruction = computed(() => {
  const step = selectedPracticeHint.value?.step;
  return step
    ? step.practiceHint || step.teachingText || step.note || '请在高亮位置完成对应业务操作。'
    : '';
});
const currentStepIsGuide = computed(
  () =>
    currentLearningStep.value?.step.kind === 'guide' ||
    currentLearningStep.value?.step.actionType === 'guide'
);
const learningProgress = computed(() =>
  learningSteps.value.length
    ? Math.min(learningStepIndex.value + 1, learningSteps.value.length)
    : 0
);
const learningTotalDuration = computed(() =>
  learningSteps.value.reduce(
    (total, item) => total + item.step.durationSeconds,
    0
  )
);
const learningElapsedDuration = computed(() =>
  learningSteps.value
    .slice(0, learningStepIndex.value + 1)
    .reduce((total, item) => total + item.step.durationSeconds, 0)
);
const learningPlaybackProgress = computed(() =>
  learningSteps.value.length
    ? Math.round((learningProgress.value / learningSteps.value.length) * 100)
    : 0
);
const currentLearningStageIndex = computed(() =>
  navigationTeachingPoints.value.findIndex(
    (stage) => stage.id === currentLearningStep.value?.stage.id
  )
);
const canMoveLearningPrevious = computed(
  () =>
    learningStepIndex.value > 0 ||
    (!showStageIntroduction.value &&
      currentLearningStep.value?.stage.recordedSteps[0]?.id ===
        currentLearningStep.value?.step.id)
);
const nextStage = computed(() =>
  lesson.value?.stages.find(
    (stage) =>
      assignedStageIds.value.has(stage.id) &&
      !task.value?.completedStageIds.includes(stage.id)
  )
);
const nextStageIndex = computed(() =>
  lesson.value?.stages.findIndex((stage) => stage.id === nextStage.value?.id)
);
const allPlaybackStagesComplete = computed(
  () =>
    [...playbackStageIds.value].length > 0 &&
    [...playbackStageIds.value].every((id) =>
      task.value?.completedStageIds.includes(id)
    )
);
const allAssignedStagesComplete = computed(
  () =>
    [...assignedStageIds.value].length > 0 &&
    [...assignedStageIds.value].every((id) =>
      task.value?.completedStageIds.includes(id)
    )
);
const currentLearningInstruction = computed(() => {
  if (!currentLearningStep.value || !task.value) return '';
  if (task.value.mode === 'LEARNING') {
    return (
      currentLearningStep.value.step.teachingText ||
      currentLearningStep.value.step.note ||
      '请观察页面变化，并理解该动作在业务流程中的作用。'
    );
  }
  return (
    currentLearningStep.value.step.practiceHint ||
    currentLearningStep.value.step.note ||
    '请在高亮位置完成当前业务操作。'
  );
});
const canRecordCompletion = computed(
  () =>
    task.value?.status === 'DOING' &&
    checks.entered &&
    checks.located &&
    checks.reviewed &&
    Boolean(nextStage.value)
);
const roleNames = computed(() =>
  task.value?.groupKeys.map(
    (key) =>
      groupPlan.value?.roles.find((role) => role.key === key)?.name ?? key
  )
);
const collaborationTasks = computed(() =>
  task.value
    ? store.state.studentTasks.filter(
        (item) =>
          item.publishedTaskId === task.value?.publishedTaskId &&
          item.dataItemId === task.value?.dataItemId
      )
    : []
);
const collaborationSubmitted = computed(
  () =>
    collaborationTasks.value.length > 0 &&
    collaborationTasks.value.every(
      (item) => item.status === 'SUBMITTED' || item.status === 'GRADED'
    )
);
const canRestartAttempt = computed(
  () =>
    task.value?.mode === 'EXAM' &&
    Boolean(examSettings.value?.allowRetry) &&
    !collaborationSubmitted.value &&
    (task.value?.attemptNumber ?? 1) <
      (examSettings.value?.maxAttempts ?? 1)
);
const requiredSubmissionComplete = computed(() =>
  (task.value?.mode === 'EXAM'
    ? examSettings.value?.submissionFields ?? []
    : []
  ).every(
    (field) => !field.required || submissionValues[field.key]?.trim()
  )
);
const modeLabel = computed(() =>
  task.value?.mode === 'LEARNING'
    ? '流程学习'
    : task.value?.mode === 'PRACTICE'
      ? '流程练习'
      : '正式考试'
);
const activeAllocationId = computed(() => launchAllocation.value?.id || '');
const activeOperationTarget = computed<
  'start' | 'search' | 'open' | 'submit' | undefined
>(() => {
  if (
    task.value?.mode === 'EXAM' ||
    task.value?.mode === 'LEARNING' ||
    task.value?.status === 'SUBMITTED' ||
    task.value?.status === 'GRADED'
  ) {
    return undefined;
  }
  if (task.value?.status === 'TODO') return 'start';
  if (!checks.located) return 'search';
  if (!checks.reviewed) return 'open';
  if (nextStage.value) return 'submit';
  return undefined;
});

/**
 * 业务功能：解析数据准备侧使用的教学任务 ID。
 * 关键流程：优先使用后端发布任务 ID，未同步到后端时回退到本地发布任务 ID，保证本地联调仍可查询分配记录。
 */
function resolveDataPrepareTaskId() {
  return (
    publishedTask.value?.remoteTaskId ||
    task.value?.publishedTaskId ||
    task.value?.id ||
    ''
  );
}

/**
 * 业务功能：查询当前学生在本任务和场景下已分配的原平台数据。
 * 关键流程：学生页只展示已由数据准备批次分配好的记录，不在前端自行决定数据池或领取策略。
 */
async function loadStudentAllocation() {
  if (!task.value) return;
  const taskId = resolveDataPrepareTaskId();
  if (!taskId) {
    launchAllocation.value = null;
    launchErrorMessage.value = '无法识别当前教学任务，请刷新后再试。';
    return;
  }
  allocationLoading.value = true;
  launchMessage.value = '';
  launchErrorMessage.value = '';
  try {
    const allocations = await dataPrepareApi.listCurrentStudentTaskAllocations({
      taskId,
      sceneType: task.value.mode,
      executionId: task.value.remoteExecutionId
    });
    launchAllocation.value = allocations[0] || null;
    if (!launchAllocation.value) {
      launchErrorMessage.value = '暂未查询到可进入的原平台数据，请确认老师已完成数据准备并下发。';
    }
  } catch (error) {
    launchAllocation.value = null;
    launchErrorMessage.value =
      error instanceof Error ? error.message : '原平台数据分配查询失败。';
  } finally {
    allocationLoading.value = false;
  }
}

/**
 * 业务功能：基于已分配数据生成进入原平台的一次性凭证。
 * 关键流程：必须先查询到分配记录，再由后端创建 launchContext，避免学生端拼接不可信原平台入口。
 */
async function prepareOriginPlatformLaunch(openInNewWindow: boolean) {
  if (!task.value) return;
  const taskId = resolveDataPrepareTaskId();
  if (!taskId) {
    launchErrorMessage.value = '无法识别当前教学任务，请刷新后再试。';
    return;
  }
  launchLoading.value = true;
  launchMessage.value = '';
  launchErrorMessage.value = '';
  launchResult.value = null;
  try {
    const result = await dataPrepareApi.createCurrentStudentTaskLaunch({
      taskId,
      sceneType: task.value.mode,
      executionId: task.value.remoteExecutionId
    });
    launchResult.value = result;
    launchAllocation.value = result.allocation;
    if (openInNewWindow) {
      window.open(result.launchUrl, '_blank', 'noopener,noreferrer');
      launchMessage.value = '原平台进入凭证已生成，请在新窗口继续办理。';
    }
  } catch (error) {
    launchErrorMessage.value =
      error instanceof Error ? error.message : '原平台进入凭证生成失败。';
  } finally {
    launchLoading.value = false;
  }
}

async function launchOriginPlatform() {
  await prepareOriginPlatformLaunch(true);
}

async function ensurePracticeRuntime() {
  if (
    !task.value ||
    task.value.mode !== 'PRACTICE' ||
    task.value.status === 'SUBMITTED' ||
    task.value.status === 'GRADED' ||
    practiceRuntimeLoading.value
  ) {
    return;
  }
  practiceRuntimeLoading.value = true;
  practiceRuntimeReady.value = false;
  try {
    if (task.value.status === 'TODO') {
      await startTask();
    }
    if (
      task.value.status === 'DOING' &&
      publishedTask.value?.remoteTaskId &&
      !launchResult.value
    ) {
      await prepareOriginPlatformLaunch(false);
    }
  } finally {
    practiceRuntimeReady.value = true;
    practiceRuntimeLoading.value = false;
  }
}

function formatPlaybackDuration(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(remainder).padStart(2, '0')}`;
}

function previewSelectedPracticeHint() {
  if (!showPracticeGuide.value || !selectedPracticeHint.value) return;
  const step = selectedPracticeHint.value.step;
  const selector = step.selector || step.selectorCandidates?.[0];
  if (!selector) return;
  practiceFrameRef.value?.previewStep(
    selector,
    step.url || step.pageSnapshot?.pageUrl,
    step.selectorCandidates,
    step.rect,
    step.recordedViewport
  );
}

function clearPracticeHintPreview() {
  practiceFrameRef.value?.clearStepPreview();
}

function closePracticeGuide() {
  showPracticeGuide.value = false;
  clearPracticeHintPreview();
}

function togglePracticeGuide() {
  if (showPracticeGuide.value) {
    closePracticeGuide();
    return;
  }
  showPracticeGuide.value = true;
  if (!practiceHintStageId.value) {
    practiceHintStageId.value = navigationTeachingPoints.value[0]?.id ?? '';
  }
  void nextTick(previewSelectedPracticeHint);
}

function selectPracticeHintTeachingPoint(stageId: string) {
  if (task.value?.mode !== 'PRACTICE' || task.value.status !== 'DOING') return;
  practiceHintStageId.value = stageId;
  practiceHintStepIndex.value = -1;
  clearPracticeHintPreview();
}

function selectPracticeHintStep(index: number) {
  if (
    task.value?.mode !== 'PRACTICE' ||
    task.value.status !== 'DOING' ||
    index < 0 ||
    index >= learningSteps.value.length
  ) {
    return;
  }
  practiceHintStepIndex.value = index;
  practiceHintStageId.value = learningSteps.value[index].stage.id;
  void nextTick(previewSelectedPracticeHint);
}

function syncSubmissionValues() {
  Object.keys(submissionValues).forEach((key) => {
    delete submissionValues[key];
  });
  Object.assign(submissionValues, task.value?.submissionValues ?? {});
}

watch(
  () => nextStage.value?.id,
  () => {
    if (task.value?.mode !== 'EXAM') return;
    checks.entered = false;
    checks.located = false;
    checks.reviewed = false;
    message.value = '';
    errorMessage.value = '';
  }
);

let practiceRuntimeKey = '';

watch(
  () =>
    [task.value?.id, task.value?.status, task.value?.attemptNumber] as const,
  () => {
    syncLearningProgress(task.value?.status === 'DOING');
    syncSubmissionValues();
    if (task.value?.mode === 'PRACTICE') {
      const nextRuntimeKey = `${task.value.id}:${task.value.attemptNumber}`;
      if (practiceRuntimeKey !== nextRuntimeKey) {
        practiceRuntimeKey = nextRuntimeKey;
        launchResult.value = null;
        practiceRuntimeReady.value = false;
        closePracticeGuide();
        practiceHintStageId.value = navigationTeachingPoints.value[0]?.id ?? '';
        practiceHintStepIndex.value = -1;
      }
      showRunnerMenu.value = false;
      void ensurePracticeRuntime();
      if (task.value.status !== 'DOING') closePracticeGuide();
    }
  },
  { immediate: true }
);

function syncLearningProgress(introduceStage = false) {
  if (
    task.value?.mode === 'LEARNING' &&
    task.value.status !== 'SUBMITTED' &&
    task.value.status !== 'GRADED'
  ) {
    learningStepIndex.value = 0;
    showStageIntroduction.value =
      introduceStage && isLearning.value && Boolean(learningSteps.value[0]);
    return;
  }
  const firstIncompleteIndex = learningSteps.value.findIndex(
    ({ stage }) => !task.value?.completedStageIds.includes(stage.id)
  );
  learningStepIndex.value =
    firstIncompleteIndex >= 0
      ? firstIncompleteIndex
      : learningSteps.value.length;
  showStageIntroduction.value =
    introduceStage &&
    isLearning.value &&
    Boolean(learningSteps.value[learningStepIndex.value]);
}

async function startTask() {
  if (!task.value) return;
  message.value = '';
  errorMessage.value = '';
  try {
    syncing.value = true;
    await store.startStudentTaskRemote(task.value.id);
    syncLearningProgress(task.value.mode === 'LEARNING');
    if (task.value.mode === 'PRACTICE') {
      showRunnerMenu.value = false;
    }
    message.value =
      task.value.mode === 'EXAM'
        ? '任务已开始。请按考试说明独立完成业务操作。'
        : task.value.mode === 'LEARNING'
          ? '学习已开始，可自由选择任意教学点或节点，学习内容与教师讲解一致。'
          : '';
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '任务启动失败。';
  } finally {
    syncing.value = false;
  }
}

async function completeStage() {
  if (!task.value || !nextStage.value) return;
  message.value = '';
  errorMessage.value = '';
  try {
    syncing.value = true;
    await store.completeStudentStageRemote(task.value.id, nextStage.value.id);
    message.value =
      '平台已记录本教学点的页面访问与流程操作。若有下一教学点，请继续办理。';
  } catch (error) {
    errorMessage.value =
      error instanceof Error
        ? error.message
        : '本教学点暂不能完成，请确认前序角色是否已提交。';
  } finally {
    syncing.value = false;
  }
}

async function submitTask() {
  if (!task.value) return;
  message.value = '';
  errorMessage.value = '';
  try {
    syncing.value = true;
    await store.submitStudentTaskRemote(task.value.id, {
      ...submissionValues
    });
    message.value =
      '答卷已提交，系统客观分已经生成；教师完成主观评分后会推送完整结果。';
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '提交失败，请检查教学点完成情况。';
  } finally {
    syncing.value = false;
  }
}

function resetLocalAttempt() {
  checks.entered = false;
  checks.located = false;
  checks.reviewed = false;
  message.value = '已重新开始当前未提交教学点，平台不会回退已经完成的业务数据。';
  errorMessage.value = '';
}

async function restartAttempt() {
  if (!task.value) return;
  const participantCount = collaborationTasks.value.length;
  const confirmed = window.confirm(
    `重新作答将重新创建一条原平台初始业务数据，并重置共享该数据的 ${participantCount} 个学员任务；旧业务数据不会回滚。确认继续吗？`
  );
  if (!confirmed) return;
  message.value = '';
  errorMessage.value = '';
  attemptMessage.value = '';
  try {
    syncing.value = true;
    const taskId = resolveDataPrepareTaskId();
    if (!taskId) {
      throw new Error('无法识别当前教学任务，请刷新后再试。');
    }
    const allocation = await dataPrepareApi.restartCurrentStudentTaskData({
      taskId,
      sceneType: task.value.mode,
      executionId: task.value.remoteExecutionId,
      sourceAllocationId: launchAllocation.value?.id
    });
    launchAllocation.value = allocation;
    launchResult.value = null;
    store.restartStudentAttempt(task.value.id);
    checks.entered = false;
    checks.located = false;
    checks.reviewed = false;
    syncSubmissionValues();
    attemptMessage.value =
      '已生成新的原平台初始业务数据，协作单元已整体进入下一次作答；旧数据保留审计，不执行回滚。';
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '重新作答失败，请稍后重试。';
  } finally {
    syncing.value = false;
  }
}

function selectorToken(selector: string) {
  const normalized = selector.replace(/\s+/g, '');
  const action = normalized.match(/data-action=["']([^"']+)["']/)?.[1];
  if (action) return `action:${action}`;
  const businessField = normalized.match(
    /data-business-field=["']([^"']+)["']/
  )?.[1];
  if (businessField) return `field:${businessField}`;
  const trainingId = normalized.match(
    /data-training-id=["']([^"']+)["']/
  )?.[1];
  const trainingAliases: Record<string, string> = {
    supplier: 'counterparty',
    category: 'category',
    amount: 'amount',
    'arrival-date': 'date',
    reason: 'reason',
    subject: 'subject'
  };
  if (trainingId && trainingAliases[trainingId]) {
    return `field:${trainingAliases[trainingId]}`;
  }
  return normalized;
}

function actionMatchesStep(
  payload: BusinessActionPayload,
  step: RecordedStep
) {
  const expectedActionType =
    step.actionType && step.actionType !== 'guide' ? step.actionType : 'click';
  const compatibleAction =
    payload.actionType === expectedActionType ||
    ((expectedActionType === 'click' || expectedActionType === 'submit') &&
      (payload.actionType === 'click' || payload.actionType === 'submit'));
  if (!compatibleAction) return false;

  const expectedUrl = step.url || step.pageSnapshot?.pageUrl;
  if (expectedUrl && payload.url) {
    try {
      const baseUrl = practiceBusinessBaseUrl.value || window.location.href;
      const expectedPath = new URL(expectedUrl, baseUrl).pathname.replace(/\/$/, '');
      const observedPath = new URL(payload.url, baseUrl).pathname.replace(/\/$/, '');
      if (expectedPath && observedPath && expectedPath !== observedPath) {
        return false;
      }
    } catch {
      // URL 只作为辅助条件；无法规范化时继续使用稳定元素选择器判定。
    }
  }
  const expected = [step.selector, ...(step.selectorCandidates ?? [])]
    .filter(Boolean)
    .map(selectorToken);
  const actual = [payload.selector, ...(payload.selectorCandidates ?? [])]
    .filter(Boolean)
    .map(selectorToken);
  return actual.some((selector) => expected.includes(selector));
}

async function advanceLearningStep() {
  if (!task.value || !currentLearningStep.value) return;
  if (task.value.mode === 'LEARNING') {
    moveLearningPlayback(1);
    return;
  }
  const current = currentLearningStep.value;
  const following = learningSteps.value[learningStepIndex.value + 1];
  const stageFinished = !following || following.stage.id !== current.stage.id;

  message.value = '';
  errorMessage.value = '';
  try {
    syncing.value = true;
    if (
      stageFinished &&
      !task.value.completedStageIds.includes(current.stage.id)
    ) {
      await store.completeStudentStageRemote(task.value.id, current.stage.id);
    }
    learningStepIndex.value = Math.min(
      learningStepIndex.value + 1,
      learningSteps.value.length
    );
    showStageIntroduction.value =
      isLearning.value &&
      stageFinished &&
      Boolean(learningSteps.value[learningStepIndex.value]);
    message.value = isPractice.value
      ? ''
      : learningStepIndex.value >= learningSteps.value.length
        ? '教案录制的完整业务流程已操作完成，可以提交本次任务。'
        : `已完成“${current.step.title}”，请继续下一项录制操作。`;
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '当前录制步骤同步失败。';
  } finally {
    syncing.value = false;
  }
}

function selectLearningStep(index: number) {
  if (
    task.value?.mode !== 'LEARNING' ||
    task.value.status !== 'DOING' ||
    index < 0 ||
    index >= learningSteps.value.length
  ) {
    return;
  }
  learningStepIndex.value = index;
  showStageIntroduction.value = false;
  message.value = `已切换到节点“${learningSteps.value[index].step.title}”。`;
  errorMessage.value = '';
}

function learningStepPosition(stepId: string) {
  return learningSteps.value.findIndex(({ step }) => step.id === stepId);
}

function selectLearningTeachingPoint(stageId: string) {
  if (
    task.value?.mode !== 'LEARNING' ||
    task.value.status !== 'DOING'
  ) {
    return;
  }
  const index = learningSteps.value.findIndex(
    ({ stage }) => stage.id === stageId
  );
  if (index < 0) return;
  learningStepIndex.value = index;
  showStageIntroduction.value = true;
  message.value = '';
  errorMessage.value = '';
}

function moveLearningPlayback(direction: -1 | 1) {
  if (
    task.value?.mode !== 'LEARNING' ||
    task.value.status !== 'DOING' ||
    !currentLearningStep.value
  ) {
    return;
  }
  if (direction === 1) {
    if (showStageIntroduction.value) {
      showStageIntroduction.value = false;
      return;
    }
    const following = learningSteps.value[learningStepIndex.value + 1];
    if (!following) return;
    const teachingPointChanged =
      following.stage.id !== currentLearningStep.value.stage.id;
    learningStepIndex.value += 1;
    showStageIntroduction.value = teachingPointChanged;
    return;
  }

  const firstStepId = currentLearningStep.value.stage.recordedSteps[0]?.id;
  if (
    !showStageIntroduction.value &&
    currentLearningStep.value.step.id === firstStepId
  ) {
    showStageIntroduction.value = true;
    return;
  }
  if (learningStepIndex.value <= 0) return;
  learningStepIndex.value -= 1;
  showStageIntroduction.value = false;
}

async function finishLearningTask() {
  if (!task.value || task.value.mode !== 'LEARNING') return;
  message.value = '';
  errorMessage.value = '';
  try {
    syncing.value = true;
    for (const teachingPoint of navigationTeachingPoints.value) {
      if (!task.value.completedStageIds.includes(teachingPoint.id)) {
        await store.completeStudentStageRemote(
          task.value.id,
          teachingPoint.id
        );
      }
    }
    await store.submitStudentTaskRemote(task.value.id, {});
    message.value = '本次学习已完成，平台已保存学习记录。';
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '学习记录提交失败。';
  } finally {
    syncing.value = false;
  }
}

const pendingPracticeStepIds = new Set<string>();
let practiceTraceQueue: Promise<void> = Promise.resolve();

async function finishPracticeWhenEvidenceComplete() {
  if (
    !task.value ||
    task.value.mode !== 'PRACTICE' ||
    task.value.status !== 'DOING' ||
    practiceSubmitting.value ||
    !learningSteps.value.length ||
    !learningSteps.value.every(({ step }) =>
      task.value?.completedPracticeStepIds?.includes(step.id)
    )
  ) {
    return;
  }
  practiceSubmitting.value = true;
  try {
    await store.submitStudentTaskRemote(task.value.id, {});
  } finally {
    practiceSubmitting.value = false;
  }
}

function handleRecordedBusinessAction(payload: BusinessActionPayload) {
  if (
    !task.value ||
    task.value.mode !== 'PRACTICE' ||
    task.value.status !== 'DOING' ||
    !payload.selector
  ) {
    return;
  }
  const matched = learningSteps.value.find(
    ({ step }) =>
      !task.value?.completedPracticeStepIds?.includes(step.id) &&
      !pendingPracticeStepIds.has(step.id) &&
      actionMatchesStep(payload, step)
  );
  if (!matched) {
    void finishPracticeWhenEvidenceComplete().catch((error) => {
      console.warn('练习任务自动提交失败，将在后续操作时重试。', error);
    });
    return;
  }

  pendingPracticeStepIds.add(matched.step.id);
  const work = practiceTraceQueue.then(async () => {
    if (
      !task.value ||
      task.value.status !== 'DOING' ||
      task.value.completedPracticeStepIds?.includes(matched.step.id)
    ) {
      return;
    }
    await store.recordStudentPracticeStepRemote(
      task.value.id,
      matched.stage.id,
      matched.step.id,
      {
        actionType: payload.actionType,
        selector: payload.selector,
        selectorCandidates: payload.selectorCandidates,
        url: payload.url,
        pageTitle: payload.pageTitle,
        completedAt: new Date().toISOString()
      }
    );
    await finishPracticeWhenEvidenceComplete();
  });
  practiceTraceQueue = work.catch(() => undefined);
  void work
    .catch((error) => {
      console.warn('练习操作点轨迹记录失败，学生可再次执行该操作重试。', error);
    })
    .finally(() => {
      pendingPracticeStepIds.delete(matched.step.id);
    });
}

function continueGuideStep() {
  if (
    showStageIntroduction.value ||
    !currentStepIsGuide.value ||
    syncing.value
  ) {
    return;
  }
  void advanceLearningStep();
}

function enterCurrentStage() {
  if (
    !currentLearningStep.value ||
    task.value?.status !== 'DOING' ||
    syncing.value
  ) {
    return;
  }
  showStageIntroduction.value = false;
  message.value =
    task.value?.mode === 'LEARNING'
      ? `已进入“${currentLearningStep.value.stage.name}”，可继续查看或切换任意节点。`
      : `已进入“${currentLearningStep.value.stage.name}”，请完成第一个录制节点。`;
  errorMessage.value = '';
}

async function restartTrainingTask() {
  if (
    !task.value ||
    task.value.mode === 'EXAM'
  ) {
    return;
  }
  try {
    syncing.value = true;
    if (task.value.mode === 'PRACTICE') {
      const taskId = resolveDataPrepareTaskId();
      if (!taskId) {
        throw new Error('无法识别当前教学任务，请刷新后再试。');
      }
      const allocation = await dataPrepareApi.restartCurrentStudentTaskData({
        taskId,
        sceneType: task.value.mode,
        executionId: task.value.remoteExecutionId,
        sourceAllocationId: launchAllocation.value?.id
      });
      launchAllocation.value = allocation;
      launchResult.value = null;
    }
    store.restartLearningOrPractice(task.value.id);
    syncLearningProgress(false);
    message.value =
      task.value.mode === 'LEARNING'
        ? '已重置学习记录，可以重新学习完整业务流程。'
        : '已生成新的原平台初始业务数据，可以重新练习完整业务流程。';
    errorMessage.value = '';
    showRunnerMenu.value = true;
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '任务重置失败。';
  } finally {
    syncing.value = false;
  }
}
</script>

<template>
  <section
    v-if="task && lesson"
    class="runner-page"
    :class="{
      'runner-page--exam': isExam,
      'runner-page--practice': isPractice,
      'learning-lecture-page': isLearning,
      'runner-menu-hidden': !showRunnerMenu
    }"
  >
    <button
      v-if="!isExam && !isLearning && !isPractice"
      class="runner-menu-toggle"
      type="button"
      @click="showRunnerMenu = !showRunnerMenu"
    >
      {{
        isLearning
          ? showRunnerMenu
            ? '隐藏其他学习菜单'
            : '显示完整学习菜单'
          : showRunnerMenu
            ? '隐藏上层菜单'
            : '显示上层菜单'
      }}
    </button>

    <header
      v-if="!isExam && !isLearning && !isPractice"
      v-show="showRunnerMenu"
      class="runner-header"
    >
      <div>
        <RouterLink to="/student/tasks">← 返回任务中心</RouterLink>
        <span class="runner-divider"></span>
        <strong>{{ task.title }}</strong>
        <span class="mode-badge">{{ modeLabel }}</span>
        <StatusPill
          :status="task.status"
          :label="
            task.status === 'TODO'
              ? '待开始'
              : task.status === 'DOING'
                ? '办理中'
                : task.status === 'SUBMITTED'
                  ? '已提交'
                  : '已评分'
          "
        />
      </div>
      <div>
        <span>学员 {{ task.studentName }}</span>
        <span>角色 {{ roleNames?.join(' / ') }}</span>
        <span v-if="task.mode === 'EXAM'">
          第 {{ task.attemptNumber }} /
          {{ examSettings?.maxAttempts ?? 1 }} 次作答
        </span>
        <button
          type="button"
          class="secondary"
          @click="showRunnerMenu = false"
        >
          隐藏全部菜单
        </button>
      </div>
    </header>

    <LessonPlaybackPlayer
      v-if="isLearning"
      :lesson="lesson"
      :current-index="learningStepIndex"
      :show-stage-introduction="showStageIntroduction"
      return-to="/student/tasks"
      return-label="返回任务中心"
      :player-state="
        task.status === 'TODO'
          ? 'READY'
          : task.status === 'SUBMITTED' || task.status === 'GRADED'
            ? 'COMPLETED'
            : 'PLAYING'
      "
      :start-label="syncing ? '正在创建会话…' : '开始流程学习'"
      :finish-label="syncing ? '正在保存…' : '完成本次学习'"
      restart-label="重新学习"
      :action-disabled="syncing"
      :feedback="errorMessage || message"
      :feedback-tone="errorMessage ? 'danger' : 'success'"
      @start="startTask"
      @restart="restartTrainingTask"
      @previous="moveLearningPlayback(-1)"
      @next="moveLearningPlayback(1)"
      @finish="finishLearningTask"
      @select-step="selectLearningStep"
      @select-stage="selectLearningTeachingPoint"
    />

    <nav
      v-if="isPractice"
      class="practice-edge-toolbar"
      aria-label="练习页面操作"
    >
      <RouterLink
        to="/student/tasks"
        title="返回任务中心"
        aria-label="返回任务中心"
      >
        ← 返回
      </RouterLink>
      <button
        type="button"
        :class="{ active: showPracticeGuide }"
        :disabled="task.status !== 'DOING' || !learningSteps.length"
        :aria-expanded="showPracticeGuide"
        @click="togglePracticeGuide"
      >
        {{ showPracticeGuide ? '收起提示' : '教案提示' }}
      </button>
    </nav>

    <aside
      v-if="isPractice && showPracticeGuide"
      class="practice-hint-drawer"
      aria-label="练习教案提示目录"
    >
      <header>
        <div>
          <small>按需查看，不影响练习进度</small>
          <strong>{{ lesson.title }}</strong>
        </div>
        <button type="button" aria-label="关闭教案提示" @click="closePracticeGuide">
          ×
        </button>
      </header>
      <div class="practice-hint-metrics">
        <span><small>教学点</small><strong>{{ navigationTeachingPoints.length }}</strong></span>
        <span><small>可提示节点</small><strong>{{ learningSteps.length }}</strong></span>
      </div>
      <PlaybackNavigationTree
        :teaching-points="navigationTeachingPoints"
        :steps="learningSteps"
        :current-stage-id="practiceHintStageId"
        :current-index="practiceHintStepIndex"
        :show-stage-introduction="false"
        :disabled="task.status !== 'DOING'"
        @select-stage="selectPracticeHintTeachingPoint"
        @select-step="selectPracticeHintStep"
      />
      <section v-if="selectedPracticeHint" class="practice-hint-detail">
        <small>{{ selectedPracticeHint.stage.name }} · 当前提示节点</small>
        <strong>{{ selectedPracticeHint.step.title }}</strong>
        <p>{{ practiceHintInstruction }}</p>
        <dl>
          <div>
            <dt>操作位置</dt>
            <dd>{{ selectedPracticeHint.step.actionLabel || selectedPracticeHint.step.pageTitle }}</dd>
          </div>
          <div>
            <dt>操作类型</dt>
            <dd>{{ selectedPracticeHint.step.actionType === 'guide' ? '点击' : selectedPracticeHint.step.actionType || '点击' }}</dd>
          </div>
        </dl>
        <AttachmentPanel
          :attachments="selectedPracticeHint.step.attachments"
          title="节点附件"
        />
        <span>业务界面中的绑定元素已高亮；可收起提示后继续练习。</span>
      </section>
      <section v-else class="practice-hint-empty">
        <strong>请选择需要查看的节点</strong>
        <p>先展开教学点，再点击具体节点，业务界面会高亮录制时绑定的元素。</p>
      </section>
      <button
        class="practice-hint-continue primary"
        type="button"
        @click="closePracticeGuide"
      >
        收起提示，继续练习
      </button>
    </aside>

    <div
      v-if="task && lesson && isLearning && legacyLearningPlaybackEnabled"
      class="learning-lecture-view"
    >
      <section
        v-show="showRunnerMenu"
        class="learning-preview-metrics"
      >
        <span>
          <small>教学点</small>
          <strong>{{ navigationTeachingPoints.length }}</strong>
        </span>
        <span>
          <small>录制片段</small>
          <strong>{{ learningSteps.length }}</strong>
        </span>
        <span>
          <small>总时长</small>
          <strong>{{ formatPlaybackDuration(learningTotalDuration) }}</strong>
        </span>
        <span>
          <small>当前进度</small>
          <strong>{{ learningPlaybackProgress }}%</strong>
        </span>
        <div class="learning-preview-progress">
          <i :style="{ width: `${learningPlaybackProgress}%` }"></i>
        </div>
      </section>

      <div class="learning-preview-layout">
        <main class="learning-business-stage">
          <div v-show="showRunnerMenu" class="learning-browser-chrome">
            <div class="learning-browser-dots"><i></i><i></i><i></i></div>
            <div class="learning-browser-address">
              {{
                displayedLearningStep?.step.pageSnapshot?.pageUrl ??
                displayedLearningStep?.step.url ??
                displayedLearningStep?.step.pageTitle
              }}
            </div>
            <span>页面快照 · 只读</span>
          </div>

          <BusinessSnapshotFrame
            v-if="displayedLearningStep"
            class="learning-snapshot-business-view"
            :snapshot="displayedLearningStep.step.pageSnapshot"
            :fallback-url="displayedLearningStep.step.url"
            :selector="
              task.status === 'DOING' && !showStageIntroduction
                ? currentLearningStep?.step.selector
                : undefined
            "
            :selector-candidates="
              task.status === 'DOING' && !showStageIntroduction
                ? currentLearningStep?.step.selectorCandidates
                : undefined
            "
            :rect="
              task.status === 'DOING' && !showStageIntroduction
                ? currentLearningStep?.step.rect
                : undefined
            "
            :recorded-viewport="
              task.status === 'DOING' && !showStageIntroduction
                ? currentLearningStep?.step.recordedViewport
                : undefined
            "
            :title="`${displayedLearningStep.step.pageTitle}录制页面快照`"
          />
          <div v-else class="learning-empty-business">
            当前教案还没有可学习的录制节点
          </div>

          <div
            v-show="showRunnerMenu"
            class="learning-playback-bar"
          >
            <span>{{ formatPlaybackDuration(learningElapsedDuration) }}</span>
            <div><i :style="{ width: `${learningPlaybackProgress}%` }"></i></div>
            <span>{{ formatPlaybackDuration(learningTotalDuration) }}</span>
          </div>
        </main>

        <aside
          v-if="task.status === 'DOING' && currentLearningStep"
          class="learning-explanation-panel learning-step-prompt"
          :class="{
            'stage-prompt': showStageIntroduction,
            'node-prompt': !showStageIntroduction
          }"
        >
          <template v-if="showStageIntroduction">
            <div class="learning-explanation-heading stage-introduction-heading">
              <span>
                本教学点说明 · 教学点 {{ currentLearningStageIndex + 1 }} /
                {{ navigationTeachingPoints.length }}
              </span>
              <strong>{{ currentLearningStep.stage.name }}</strong>
              <small>{{ currentLearningStep.stage.groupKey || '未指定业务角色' }}</small>
            </div>
            <div class="learning-instruction stage-introduction">
              <span>教学点目标与注意事项</span>
              <p>
                {{
                  currentLearningStep.stage.description ||
                  '本教学点暂无补充说明，可按需选择任意节点进行学习。'
                }}
              </p>
            </div>
            <AttachmentPanel
              :attachments="currentLearningStep.stage.attachments"
              title="教学点附件"
            />
            <dl>
              <div>
                <dt>教学点节点</dt>
                <dd>{{ currentLearningStep.stage.recordedSteps.length }} 个</dd>
              </div>
              <div>
                <dt>负责角色</dt>
                <dd>{{ currentLearningStep.stage.groupKey || '未指定' }}</dd>
              </div>
              <div>
                <dt>教学点分值</dt>
                <dd>{{ currentLearningStep.stage.score }} 分</dd>
              </div>
              <div>
                <dt>完成依据</dt>
                <dd>{{ currentLearningStep.stage.completionMethod }}</dd>
              </div>
            </dl>
          </template>
          <template v-else>
            <div class="learning-explanation-heading">
              <span>
                本节点说明 · 步骤 {{ learningProgress }} /
                {{ learningSteps.length }}
              </span>
              <strong>{{ currentLearningStep.step.title }}</strong>
              <small>
                {{ currentLearningStep.stage.name }} ·
                {{ currentLearningStep.stage.groupKey }}
              </small>
            </div>
            <div class="learning-instruction">
              <span>逐步讲解</span>
              <p>{{ currentLearningInstruction }}</p>
            </div>
            <AttachmentPanel
              :attachments="currentLearningStep.step.attachments"
              title="节点附件"
            />
            <dl>
              <div>
                <dt>pageTitle</dt>
                <dd>{{ currentLearningStep.step.pageTitle }}</dd>
              </div>
              <div>
                <dt>actionLabel</dt>
                <dd>{{ currentLearningStep.step.actionLabel }}</dd>
              </div>
              <div>
                <dt>selector</dt>
                <dd><code>{{ currentLearningStep.step.selector }}</code></dd>
              </div>
              <div>
                <dt>durationSeconds</dt>
                <dd>{{ currentLearningStep.step.durationSeconds }} 秒</dd>
              </div>
              <div>
                <dt>完成依据</dt>
                <dd>{{ currentLearningStep.stage.completionMethod }}</dd>
              </div>
            </dl>
          </template>

          <div class="learning-step-controls">
            <button
              type="button"
              :disabled="!canMoveLearningPrevious || syncing"
              @click="moveLearningPlayback(-1)"
            >
              ← 上一步
            </button>
            <button
              v-if="showStageIntroduction"
              class="primary"
              type="button"
              :disabled="syncing"
              @click="moveLearningPlayback(1)"
            >
              进入本教学点 →
            </button>
            <button
              v-else-if="learningStepIndex < learningSteps.length - 1"
              class="primary"
              type="button"
              :disabled="syncing"
              @click="moveLearningPlayback(1)"
            >
              下一步 →
            </button>
            <button
              v-else
              class="primary"
              type="button"
              :disabled="syncing"
              @click="finishLearningTask"
            >
              {{ syncing ? '正在保存…' : '完成本次学习' }}
            </button>
          </div>
          <p v-if="message" class="learning-feedback success">{{ message }}</p>
          <p v-if="errorMessage" class="learning-feedback danger">
            {{ errorMessage }}
          </p>
        </aside>

        <aside
          v-else
          class="learning-explanation-panel learning-step-prompt stage-prompt learning-state-prompt"
        >
          <div class="learning-explanation-heading stage-introduction-heading">
            <span>
              {{
                task.status === 'TODO'
                  ? '准备开始流程学习'
                  : task.status === 'SUBMITTED' || task.status === 'GRADED'
                    ? '学习记录已保存'
                    : '暂无录制节点'
              }}
            </span>
            <strong>
              {{
                task.status === 'TODO'
                  ? lesson.title
                  : task.status === 'SUBMITTED' || task.status === 'GRADED'
                    ? '本次流程学习已经完成'
                    : '当前教案暂无学习内容'
              }}
            </strong>
            <small>{{ task.studentName }} · {{ roleNames?.join(' / ') }}</small>
          </div>
          <div class="learning-instruction stage-introduction">
            <span>学习说明</span>
            <p v-if="task.status === 'TODO'">
              开始后将按照教师讲解的同一界面、教学点说明和节点提示进行只读学习。
            </p>
            <p v-else-if="task.status === 'SUBMITTED' || task.status === 'GRADED'">
              系统已保存完整学习轨迹。你可以返回任务中心，也可以从第一步重新学习。
            </p>
            <p v-else>
              请联系教师返回教案编排，至少录制一个完整业务操作。
            </p>
          </div>
          <div class="learning-state-actions">
            <button
              v-if="task.status === 'TODO'"
              class="primary"
              type="button"
              :disabled="syncing || !learningSteps.length"
              @click="startTask"
            >
              {{ syncing ? '正在创建会话…' : '开始流程学习' }}
            </button>
            <template v-else-if="task.status === 'SUBMITTED' || task.status === 'GRADED'">
              <RouterLink class="button secondary" to="/student/tasks">
                返回任务中心
              </RouterLink>
              <button class="primary" type="button" @click="restartTrainingTask">
                重新学习
              </button>
            </template>
            <RouterLink v-else class="button secondary" to="/student/tasks">
              返回任务中心
            </RouterLink>
          </div>
          <p v-if="message" class="learning-feedback success">{{ message }}</p>
          <p v-if="errorMessage" class="learning-feedback danger">
            {{ errorMessage }}
          </p>
        </aside>
      </div>

      <section
        v-if="learningSteps.length"
        v-show="showRunnerMenu"
        class="learning-segment-timeline"
      >
        <div class="learning-teaching-point-list">
          <button
            v-for="(stage, index) in navigationTeachingPoints"
            :key="stage.id"
            type="button"
            :disabled="task.status !== 'DOING'"
            :class="{
              active:
                stage.id === currentLearningStep?.stage.id &&
                showStageIntroduction
            }"
            @click="selectLearningTeachingPoint(stage.id)"
          >
            <span>{{ index + 1 }}</span>
            <strong>{{ stage.name }}</strong>
            <small>{{ stage.recordedSteps.length }} 个节点</small>
          </button>
        </div>
        <div class="learning-segment-list">
          <button
            v-for="(item, index) in learningSteps"
            :key="item.step.id"
            type="button"
            :disabled="task.status !== 'DOING'"
            :class="{
              active: index === learningStepIndex && !showStageIntroduction
            }"
            @click="selectLearningStep(index)"
          >
            <span>{{ index + 1 }}</span>
            <strong>{{ item.step.title }}</strong>
            <small>{{ item.stage.name }} · {{ item.step.durationSeconds }} 秒</small>
          </button>
        </div>
      </section>
    </div>

    <div
      v-if="!isLearning"
      class="runner-layout"
      :class="{
        'help-hidden': !showHelp,
        'learning-mode': task.mode === 'LEARNING',
        'practice-independent': isPractice,
        'exam-mode': isExam
      }"
    >
      <aside
        v-if="!isExam && !isPractice && showRunnerMenu"
        class="stage-sidebar"
      >
        <span class="runner-kicker">BUSINESS WORKFLOW</span>
        <h1>教学点导航</h1>
        <p>
          {{
            task.mode === 'LEARNING'
              ? '学习内容与教师讲解一致，可自由选择任意教学点或节点。'
              : '各角色按顺序完成同一批业务流程；你仅需办理分配给自己的教学点。'
          }}
        </p>
        <div v-if="examSettings?.showProgress !== false" class="stage-list">
          <div
            v-for="(stage, index) in visibleStages"
            :key="stage.id"
            :class="{
              selectable: task.mode === 'LEARNING' && playbackStageIds.has(stage.id),
              assigned: playbackStageIds.has(stage.id),
              complete: task.completedStageIds.includes(stage.id),
              current:
                stage.id ===
                (currentLearningStep?.stage.id ?? nextStage?.id)
            }"
            @click="
              task.mode === 'LEARNING' &&
              playbackStageIds.has(stage.id) &&
              selectLearningTeachingPoint(stage.id)
            "
          >
            <span>
              {{ task.completedStageIds.includes(stage.id) ? '✓' : index + 1 }}
            </span>
            <section>
              <strong>{{ stage.name }}</strong>
              <small>
                {{
                  task.mode === 'LEARNING'
                    ? playbackStageIds.has(stage.id)
                      ? '点击查看教学点说明'
                      : '当前教学点无录制节点'
                    : playbackStageIds.has(stage.id)
                      ? '由我办理'
                      : '等待其他角色办理'
                }}
              </small>
              <div
                v-if="
                  task.mode === 'LEARNING' &&
                  stage.id === currentLearningStep?.stage.id &&
                  stage.recordedSteps.length
                "
                class="teaching-point-nodes"
              >
                <button
                  v-for="(step, stepIndex) in stage.recordedSteps"
                  :key="step.id"
                  type="button"
                  :class="{ active: step.id === currentLearningStep?.step.id && !showStageIntroduction }"
                  @click.stop="selectLearningStep(learningStepPosition(step.id))"
                >
                  {{ stepIndex + 1 }}. {{ step.title }}
                </button>
              </div>
            </section>
          </div>
        </div>
        <div v-else class="hidden-progress">
          <strong>考试进度已隐藏</strong>
          <p>按考试设置，仅显示当前操作引导，不展示完整教学点。</p>
        </div>
        <div class="evidence-boundary">
          <strong>当前判定口径</strong>
          <p>
            {{
              task.mode === 'LEARNING'
                ? '学习模式为讲解式只读回放，可按需要切换教学点与节点。'
                : '访问目标页面 + 定位业务待办 + 执行流程按钮，即判定本教学点完成。'
            }}
          </p>
        </div>
      </aside>

      <main class="business-canvas">
        <div
          v-if="!isExam"
          class="learning-playback training-business-flow"
        >
          <BusinessCaptureFrame
            v-if="
              isPractice &&
              task.status === 'DOING' &&
              practiceRuntimeReady &&
              practiceBusinessUrl &&
              learningSteps.length
            "
            ref="practiceFrameRef"
            class="practice-live-business-view"
            :src="practiceBusinessUrl"
            :title="`${lesson.title}原业务系统练习界面`"
            fit-mode="fill"
            student-mode="PRACTICE"
            monitor-actions
            :show-resolution="false"
            :allowed-origins="practiceAllowedOrigins"
            @business-ready="previewSelectedPracticeHint"
            @business-action="handleRecordedBusinessAction"
          />
          <BusinessSnapshotFrame
            v-else-if="!isPractice && displayedLearningStep"
            :snapshot="displayedLearningStep.step.pageSnapshot"
            :fallback-url="displayedLearningStep.step.url"
            :selector="
              isPractice || showStageIntroduction
                ? undefined
                : currentLearningStep?.step.selector
            "
            :selector-candidates="
              isPractice || showStageIntroduction
                ? undefined
                : currentLearningStep?.step.selectorCandidates
            "
            :rect="
              isPractice || showStageIntroduction
                ? undefined
                : currentLearningStep?.step.rect
            "
            :recorded-viewport="
              isPractice || showStageIntroduction
                ? undefined
                : currentLearningStep?.step.recordedViewport
            "
            :interactive="
              task.mode === 'PRACTICE' &&
              task.status === 'DOING' &&
              Boolean(currentLearningStep) &&
              !showStageIntroduction &&
              !syncing &&
              !currentStepIsGuide
            "
            :clear-form-values="task.mode === 'PRACTICE'"
            :title="`${displayedLearningStep.step.pageTitle}${modeLabel}业务界面`"
            @business-action="handleRecordedBusinessAction"
          />

          <div
            v-if="!learningSteps.length"
            class="training-state-overlay"
          >
            <span>!</span>
            <h3>当前教案还没有可执行的录制节点</h3>
            <p>请联系教师返回教案编排，至少录制一个完整业务操作。</p>
            <RouterLink class="button secondary" to="/student/tasks">
              返回任务中心
            </RouterLink>
          </div>

          <div
            v-else-if="!isPractice && task.status === 'TODO'"
            class="training-state-overlay"
          >
            <span>01</span>
            <h3>{{ isPractice ? '开始本次练习' : '准备进入录制时的业务系统' }}</h3>
            <p v-if="!isPractice">
              开始后将加载教师录制的 {{ learningSteps.length }}
              个业务节点。学习模式可自由切换，练习模式按实际操作推进。
            </p>
            <button
              class="primary"
              type="button"
              :disabled="syncing"
              @click="startTask"
            >
              {{ syncing ? '正在创建会话…' : `开始${modeLabel}` }}
            </button>
          </div>

          <div
            v-else-if="
              isPractice &&
              task.status === 'DOING' &&
              (!practiceRuntimeReady || practiceRuntimeLoading)
            "
            class="training-state-overlay practice-runtime-state"
          >
            <span>…</span>
            <h3>正在进入原业务系统</h3>
            <p>平台正在创建独立练习会话。</p>
          </div>

          <div
            v-else-if="
              isPractice &&
              task.status === 'DOING' &&
              practiceRuntimeReady &&
              !practiceBusinessUrl
            "
            class="training-state-overlay"
          >
            <span>!</span>
            <h3>原业务系统暂时无法打开</h3>
            <p>{{ launchErrorMessage || '请检查业务平台及业务模块入口配置。' }}</p>
            <RouterLink class="button secondary" to="/student/tasks">
              返回任务中心
            </RouterLink>
          </div>

          <div
            v-else-if="task.status === 'SUBMITTED' || task.status === 'GRADED'"
            class="training-state-overlay success"
          >
            <span>✓</span>
            <h3>本次{{ modeLabel }}已经完成</h3>
            <p>
              系统已保存完整业务操作轨迹。你可以返回任务中心，也可以从第一步重新开始。
            </p>
            <div>
              <RouterLink class="button secondary" to="/student/tasks">
                返回任务中心
              </RouterLink>
              <button class="primary" type="button" @click="restartTrainingTask">
                {{ task.mode === 'LEARNING' ? '重新学习' : '重新练习' }}
              </button>
            </div>
          </div>

          <div
            v-else-if="
              !isPractice && showStageIntroduction && currentLearningStep
            "
            class="training-state-overlay stage-introduction-overlay"
          >
            <span>{{ currentLearningStageIndex + 1 }}</span>
            <small>
              本教学点说明 · 教学点 {{ currentLearningStageIndex + 1 }} /
              {{ playbackStageIds.size }}
            </small>
            <h3>{{ currentLearningStep.stage.name }}</h3>
            <p>
              {{
                currentLearningStep.stage.description ||
                '本教学点暂无补充说明，可按需选择任意节点查看。'
              }}
            </p>
            <AttachmentPanel
              :attachments="currentLearningStep.stage.attachments"
              title="教学点附件"
            />
            <dl>
              <div>
                <dt>负责角色</dt>
                <dd>{{ currentLearningStep.stage.groupKey || '未指定' }}</dd>
              </div>
              <div>
                <dt>操作节点</dt>
                <dd>{{ currentLearningStep.stage.recordedSteps.length }} 个</dd>
              </div>
            </dl>
            <button
              class="primary"
              type="button"
              :disabled="syncing"
              @click="enterCurrentStage"
            >
              {{ syncing ? '正在初始化任务…' : '进入本教学点' }}
            </button>
          </div>

          <div
            v-if="
              !isPractice &&
              (showRunnerMenu || task.mode === 'LEARNING') &&
              task.status === 'DOING' &&
              currentLearningStep &&
              !showStageIntroduction
            "
            class="learning-controls"
          >
            <span>
              {{ modeLabel }} {{ learningProgress }} / {{ learningSteps.length }}
            </span>
            <strong>{{ currentLearningStep.step.title }}</strong>
            <p>{{ currentLearningInstruction }}</p>
            <AttachmentPanel
              :attachments="currentLearningStep.step.attachments"
              title="节点附件"
            />
            <div v-if="task.mode === 'LEARNING'" class="learning-playback-actions">
              <button
                type="button"
                :disabled="!canMoveLearningPrevious || syncing"
                @click="moveLearningPlayback(-1)"
              >
                ← 上一步
              </button>
              <button
                v-if="learningStepIndex < learningSteps.length - 1"
                class="primary"
                type="button"
                :disabled="syncing"
                @click="moveLearningPlayback(1)"
              >
                下一步 →
              </button>
              <button
                v-else
                class="primary"
                type="button"
                :disabled="syncing"
                @click="finishLearningTask"
              >
                {{ syncing ? '正在保存…' : '完成本次学习' }}
              </button>
            </div>
            <div v-else>
              <button
                v-if="currentStepIsGuide"
                class="primary"
                type="button"
                :disabled="syncing"
                @click="continueGuideStep"
              >
                已阅读，继续
              </button>
              <small v-else>在高亮位置操作后将自动进入下一步</small>
            </div>
            <p v-if="message" class="notice success">{{ message }}</p>
            <p v-if="errorMessage" class="notice danger">{{ errorMessage }}</p>
          </div>
        </div>
        <div v-else class="mock-browser">
          <div class="browser-bar">
            <div><i></i><i></i><i></i></div>
            <span>业务系统演示窗口 · 非真实业务提交</span>
            <b>安全代理会话</b>
          </div>
          <div class="business-app">
            <nav>
              <strong>协同业务平台</strong>
              <a class="active">我的待办</a>
              <a>业务申请</a>
              <a>审批中心</a>
              <a>归档查询</a>
              <small>当前单位：第一事业部</small>
            </nav>
            <section class="business-content">
              <header>
                <div>
                  <span>业务办理 / 我的待办</span>
                  <h2>{{ nextStage?.name ?? '本角色教学点已完成' }}</h2>
                </div>
                <em>演示模式 · 操作不会写入真实业务系统</em>
              </header>

              <div class="origin-launch-card">
                <div>
                  <strong>
                    {{
                      allocationLoading
                        ? '正在识别原平台数据'
                        : activeAllocationId
                          ? '原平台数据已分配'
                          : '暂无可进入的原平台数据'
                    }}
                  </strong>
                  <small>
                    {{
                      activeAllocationId
                        ? `分配记录 ${activeAllocationId}，系统将按该记录中的单位和角色进入原平台。`
                        : '系统会按当前学生、任务和场景查询数据分配记录；未查到时请确认老师是否已完成批次准备并下发。'
                    }}
                  </small>
                </div>
                <div class="origin-launch-card__actions">
                  <button
                    class="secondary"
                    type="button"
                    :disabled="allocationLoading"
                    @click="loadStudentAllocation"
                  >
                    重新查询
                  </button>
                  <button
                    class="primary"
                    type="button"
                    :disabled="launchLoading || allocationLoading"
                    @click="launchOriginPlatform"
                  >
                    {{ launchLoading ? '正在生成凭证' : '进入原平台办理' }}
                  </button>
                </div>
              </div>

              <div v-if="task.status === 'TODO'" class="business-start">
                <span>01</span>
                <h3>准备进入业务办理</h3>
                <p>
                  点击开始后，平台将创建本次学员会话轨迹并展示录制好的操作指引。
                </p>
                <button
                  class="primary"
                  :class="{ 'operation-target': activeOperationTarget === 'start' }"
                  type="button"
                  :disabled="syncing"
                  @click="startTask"
                >
                  {{ syncing ? '正在创建会话…' : '开始本次任务' }}
                </button>
              </div>

              <div
                v-else-if="task.status === 'SUBMITTED' || task.status === 'GRADED'"
                class="business-start success"
              >
                <span>✓</span>
                <h3>本次{{ modeLabel }}任务已经完成</h3>
                <p>
                  <template v-if="task.mode === 'EXAM'">
                    客观分 {{ task.objectiveScore ?? 0 }} 分。教师评分后，可在成绩反馈中查看主观分和批语。
                  </template>
                  <template v-else>
                    系统已保存学习轨迹与教学点完成记录，可以返回任务中心继续下一项任务。
                  </template>
                </p>
                <RouterLink v-if="task.mode === 'EXAM'" class="button primary" to="/student/results">
                  查看成绩反馈
                </RouterLink>
                <RouterLink v-else class="button primary" to="/student/tasks">
                  返回任务中心
                </RouterLink>
                <button
                  v-if="canRestartAttempt"
                  class="secondary"
                  type="button"
                  @click="restartAttempt"
                >
                  协作单元尚未全部提交，重新作答
                </button>
              </div>

              <div
                v-else-if="allAssignedStagesComplete"
                class="business-start success"
              >
                <span>✓</span>
                <h3>你负责的教学点已全部完成</h3>
                <p>
                  {{
                    task.mode === 'EXAM'
                      ? '提交答卷后系统将固化客观操作成绩；提交前仍可检查本次流程记录。'
                      : '完成任务后系统将固化本次学习轨迹和客观操作结果。'
                  }}
                </p>
                <div
                  v-if="task.mode === 'EXAM' && examSettings?.submissionFields.length"
                  class="submission-fields"
                >
                  <label
                    v-for="field in examSettings.submissionFields"
                    :key="field.key"
                  >
                    <span>
                      {{ field.label }}
                      <b v-if="field.required">*</b>
                    </span>
                    <input
                      v-model="submissionValues[field.key]"
                      :placeholder="`请输入${field.label}`"
                    />
                  </label>
                </div>
                <button
                  class="primary"
                  type="button"
                  :disabled="!requiredSubmissionComplete || syncing"
                  @click="submitTask"
                >
                  {{
                    syncing
                      ? '正在提交…'
                      : task.mode === 'EXAM'
                        ? '提交本次答卷'
                        : '完成本次任务'
                  }}
                </button>
              </div>

              <template v-else>
                <div class="business-search">
                  <label>
                    待办标题
                    <input
                      value="采购业务办理"
                      readonly
                      @focus="checks.entered = true"
                    />
                  </label>
                  <label>
                    业务状态
                    <select @change="checks.entered = true">
                      <option>待办理</option>
                      <option>全部</option>
                    </select>
                  </label>
                  <button
                    class="business-search-button"
                    :class="{ 'operation-target': activeOperationTarget === 'search' }"
                    type="button"
                    @click="
                      checks.entered = true;
                      checks.located = true;
                    "
                  >
                    查询待办
                  </button>
                </div>

                <div class="business-table">
                  <div class="business-table__head">
                    <span>业务编号</span>
                    <span>业务类型</span>
                    <span>发起单位</span>
                    <span>当前环节</span>
                    <span>操作</span>
                  </div>
                  <div
                    class="business-table__row"
                    :class="{ highlighted: checks.located }"
                  >
                    <strong>
                      {{
                        checks.located
                          ? dataItem?.maskedReference ?? 'BUS-****-2801'
                          : '请先查询'
                      }}
                    </strong>
                    <span>采购申请</span>
                    <span>第一事业部</span>
                    <span>{{ nextStage?.name }}</span>
                    <button
                      :class="{ 'operation-target': activeOperationTarget === 'open' }"
                      type="button"
                      :disabled="!checks.located"
                      @click="checks.reviewed = true"
                    >
                      进入办理
                    </button>
                  </div>
                </div>

                <div v-if="checks.reviewed" class="business-form">
                  <div>
                    <span>业务摘要</span>
                    <strong>办公设备集中采购申请</strong>
                    <small>
                      仅展示平台演示字段，不呈现真实业务数据内容。
                    </small>
                  </div>
                  <label>
                    办理意见
                    <textarea
                      rows="3"
                      :placeholder="
                        nextStage?.completionMethod === 'submission'
                          ? '请填写提交说明'
                          : '按录制教案完成流程操作'
                      "
                    ></textarea>
                  </label>
                  <div class="business-form__actions">
                    <button
                      type="button"
                      class="secondary"
                      @click="resetLocalAttempt"
                    >
                      重新进入本教学点
                    </button>
                    <button
                      type="button"
                      class="primary"
                      :class="{ 'operation-target': activeOperationTarget === 'submit' }"
                      :disabled="!canRecordCompletion || syncing"
                      @click="completeStage"
                    >
                      {{ nextStage?.recordedSteps.at(-1)?.actionLabel || '提交办理' }}
                    </button>
                  </div>
                </div>
              </template>
            </section>
          </div>
        </div>
      </main>

      <aside
        v-if="
          !isPractice &&
          showRunnerMenu &&
          task.mode === 'PRACTICE' &&
          !showStageIntroduction
        "
        class="guide-panel"
      >
        <span class="runner-kicker">RECORDED GUIDE</span>
        <h2>{{ currentLearningStep?.step.title ?? '流程完成' }}</h2>
        <p>
          {{
            currentLearningStep
              ? currentLearningInstruction
              : '教案录制的业务节点已经全部完成。'
          }}
        </p>

        <div v-if="currentLearningStep" class="guide-checks">
          <div class="done">
            <span>1</span>
            <p>
              <strong>进入录制业务页面</strong>
              <small>{{ currentLearningStep.step.pageTitle }}</small>
            </p>
          </div>
          <div class="done">
            <span>2</span>
            <p>
              <strong>定位当前高亮位置</strong>
              <small>{{ currentLearningStep.step.actionLabel }}</small>
            </p>
          </div>
          <div>
            <span>3</span>
            <p>
              <strong>完成录制对应操作</strong>
              <small>
                操作匹配后自动进入下一录制节点
              </small>
            </p>
          </div>
        </div>

        <div v-if="currentLearningStep?.stage.recordedSteps.length" class="recorded-steps">
          <strong>本教学点录制步骤</strong>
          <div
            v-for="(step, index) in currentLearningStep.stage.recordedSteps"
            :key="step.id"
          >
            <span>{{ index + 1 }}</span>
            <p>
              <b>{{ step.title }}</b>
              <small>{{ step.note }}</small>
            </p>
            <em>
              {{
                step.id === currentLearningStep.step.id
                  ? '当前'
                  : '录制'
              }}
            </em>
          </div>
        </div>

        <p v-if="message" class="notice success">{{ message }}</p>
        <p v-if="launchMessage" class="notice success">
          {{ launchMessage }}
        </p>
        <p v-if="attemptMessage" class="notice success">
          {{ attemptMessage }}
        </p>
        <p v-if="launchErrorMessage" class="notice danger">
          {{ launchErrorMessage }}
        </p>
        <p v-if="errorMessage" class="notice danger">{{ errorMessage }}</p>

        <div class="session-info">
          <span>会话证据</span>
          <strong>页面访问</strong>
          <strong>元素操作</strong>
          <strong>录制顺序</strong>
          <strong>第 {{ task.attemptNumber }} 次练习</strong>
          <small>步骤必须与教案录制选择器匹配后才会推进</small>
        </div>
      </aside>

      <aside v-if="isExam && showHelp" class="exam-task-panel">
        <div>
          <span>考试任务说明</span>
          <strong>{{ task.title }}</strong>
          <small>
            第 {{ task.attemptNumber }} / {{ examSettings?.maxAttempts ?? 1 }} 次作答
          </small>
        </div>
        <p>
          {{
            nextStage?.description ||
            '请在业务系统中独立完成分配给你的考试流程，系统将记录操作轨迹。'
          }}
        </p>
        <span v-if="message" class="exam-message success">{{ message }}</span>
        <span v-if="errorMessage" class="exam-message danger">{{ errorMessage }}</span>
        <button
          v-if="canRestartAttempt"
          class="secondary"
          type="button"
          @click="restartAttempt"
        >
          重新作答
        </button>
        <button type="button" @click="showHelp = false">隐藏说明</button>
      </aside>

      <button
        v-if="isExam && !showHelp"
        class="exam-task-toggle"
        type="button"
        @click="showHelp = true"
      >
        显示考试说明
      </button>
    </div>
  </section>

  <section v-else class="standalone-state">
    <span>404</span>
    <h1>未找到该学员任务</h1>
    <p>任务可能尚未发布，或演示数据已被重置。</p>
    <RouterLink class="button primary" to="/student/tasks">
      返回任务中心
    </RouterLink>
  </section>
</template>

<style scoped>
.runner-page {
  width: 100%;
  min-height: calc(100vh - 128px);
}

.runner-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 14px;
  border: 1px solid #e4e7ee;
  border-radius: 13px;
  background: #fff;
  padding: 11px 14px;
}

.runner-header > div {
  display: flex;
  align-items: center;
  gap: 9px;
}

.runner-header a,
.runner-header span {
  color: #7f899a;
  font-size: 10px;
}

.runner-header strong {
  color: #344056;
  font-size: 12px;
}

.runner-divider {
  width: 1px;
  height: 18px;
  background: #e1e4ea;
}

.runner-header button {
  min-height: 31px;
  font-size: 9px;
}

.runner-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 275px;
  min-height: calc(100vh - 198px);
  overflow: hidden;
  border: 1px solid #e1e5ec;
  border-radius: 15px;
  background: #fff;
  box-shadow: 0 12px 30px rgb(30 36 79 / 5%);
}

.runner-layout.help-hidden {
  grid-template-columns: 220px minmax(0, 1fr);
}

.stage-sidebar,
.guide-panel {
  padding: 19px;
}

.stage-sidebar {
  border-right: 1px solid #e8ebf1;
  background: #fafbfc;
}

.runner-kicker {
  color: var(--purple);
  font-size: 8px;
  font-weight: 900;
  letter-spacing: 0.15em;
}

.stage-sidebar h1,
.guide-panel h2 {
  margin: 7px 0 5px;
  color: #2f3b50;
  font-size: 17px;
}

.stage-sidebar > p,
.guide-panel > p {
  margin: 0;
  color: #8a94a5;
  font-size: 9px;
  line-height: 1.65;
}

.stage-list {
  display: grid;
  margin-top: 18px;
}

.stage-list > div {
  position: relative;
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 9px;
  min-height: 63px;
  opacity: 0.5;
}

.stage-list > div.selectable {
  cursor: pointer;
}

.stage-list > div.selectable:hover {
  border-radius: 9px;
  background: #f7f5ff;
}

.stage-list > div::before {
  position: absolute;
  top: 30px;
  bottom: 0;
  left: 13px;
  width: 1px;
  background: #dce0e8;
  content: "";
}

.stage-list > div:last-child::before {
  display: none;
}

.stage-list > div.assigned,
.stage-list > div.complete {
  opacity: 1;
}

.stage-list > div > span {
  position: relative;
  z-index: 1;
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border: 1px solid #d9dee7;
  border-radius: 9px;
  color: #8490a2;
  background: #fff;
  font-size: 9px;
  font-weight: 900;
}

.stage-list > div.current > span {
  border-color: #7161e8;
  color: #fff;
  background: #7161e8;
  box-shadow: 0 0 0 4px rgb(113 97 232 / 10%);
}

.stage-list > div.complete > span {
  border-color: #bee6d7;
  color: #087c59;
  background: #e9f8f2;
}

.stage-list section {
  display: grid;
  align-content: start;
  gap: 4px;
  padding-top: 3px;
}

.stage-list strong {
  font-size: 10px;
}

.stage-list small {
  color: #8993a4;
  font-size: 8px;
}

.teaching-point-nodes {
  display: grid;
  gap: 4px;
  margin-top: 7px;
}

.teaching-point-nodes button {
  overflow: hidden;
  border: 1px solid #e4e1f7;
  border-radius: 6px;
  padding: 6px 7px;
  color: #6f7889;
  background: #fff;
  font-size: 8px;
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.teaching-point-nodes button.active {
  border-color: #8b7cf1;
  color: #5747c9;
  background: #efecff;
  font-weight: 800;
}

.evidence-boundary {
  margin-top: 20px;
  border: 1px solid #e0dcff;
  border-radius: 10px;
  padding: 10px;
  background: #f5f3ff;
}

.hidden-progress {
  margin-top: 18px;
  border: 1px dashed #d8dce5;
  border-radius: 10px;
  padding: 13px;
  background: #fff;
}

.hidden-progress strong {
  color: #566176;
  font-size: 10px;
}

.hidden-progress p {
  margin: 5px 0 0;
  color: #8a94a5;
  font-size: 8px;
  line-height: 1.55;
}

.evidence-boundary strong {
  color: #5747c3;
  font-size: 9px;
}

.evidence-boundary p {
  margin: 4px 0 0;
  color: #776da0;
  font-size: 8px;
  line-height: 1.55;
}

.business-canvas {
  min-width: 0;
  padding: 18px;
  background:
    radial-gradient(circle at 50% 20%, rgb(109 93 252 / 7%), transparent 25%),
    #f1f3f7;
}

.mock-browser {
  height: 100%;
  min-height: 600px;
  overflow: hidden;
  border: 1px solid #d8dde6;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 15px 35px rgb(32 39 74 / 10%);
}

.learning-playback {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 600px;
  overflow: hidden;
  background: #eef2f7;
}

.training-state-overlay {
  position: absolute;
  z-index: 12;
  top: 50%;
  left: 50%;
  display: grid;
  width: min(440px, calc(100vw - 32px));
  place-items: center;
  gap: 11px;
  transform: translate(-50%, -50%);
  border: 1px solid rgb(219 224 234 / 94%);
  border-radius: 18px;
  padding: 26px;
  color: #354158;
  background: rgb(255 255 255 / 96%);
  box-shadow: 0 24px 64px rgb(20 27 53 / 22%);
  text-align: center;
  backdrop-filter: blur(16px);
}

.training-state-overlay :deep(.attachment-panel),
.learning-controls :deep(.attachment-panel) {
  width: 100%;
  text-align: left;
}

.training-state-overlay > span {
  display: grid;
  width: 54px;
  height: 54px;
  place-items: center;
  border-radius: 16px;
  color: #6755dc;
  background: #efedff;
  font-size: 15px;
  font-weight: 900;
}

.stage-introduction-overlay {
  z-index: 30;
  width: min(500px, calc(100vw - 32px));
  border-color: rgb(201 194 255 / 96%);
  background:
    linear-gradient(145deg, rgb(247 245 255 / 98%), rgb(255 255 255 / 98%));
}

.stage-introduction-overlay > small {
  color: #6755dc;
  font-size: 9px;
  font-weight: 900;
}

.stage-introduction-overlay dl {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 2px 0;
  overflow: hidden;
  border: 1px solid #e2defc;
  border-radius: 10px;
  background: rgb(255 255 255 / 76%);
}

.stage-introduction-overlay dl > div {
  display: grid;
  gap: 3px;
  padding: 10px;
}

.stage-introduction-overlay dl > div + div {
  border-left: 1px solid #e8e4fa;
}

.stage-introduction-overlay dt {
  color: #8a94a6;
  font-size: 8px;
}

.stage-introduction-overlay dd {
  margin: 0;
  color: #465269;
  font-size: 10px;
  font-weight: 800;
}

.training-state-overlay.success > span {
  color: #087b59;
  background: #e8f8f2;
}

.training-state-overlay h3,
.training-state-overlay p {
  margin: 0;
}

.training-state-overlay h3 {
  font-size: 16px;
}

.training-state-overlay p {
  color: #7f8b9e;
  font-size: 10px;
  line-height: 1.65;
}

.training-state-overlay > div {
  display: flex;
  gap: 9px;
}

.learning-controls {
  position: absolute;
  z-index: 5;
  right: 330px;
  bottom: 16px;
  display: grid;
  width: min(520px, calc(100vw - 590px));
  gap: 6px;
  border: 1px solid rgb(221 225 235 / 92%);
  border-radius: 14px;
  padding: 13px 15px;
  color: #2f3a50;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 16px 42px rgb(21 28 55 / 18%);
  backdrop-filter: blur(14px);
}

.learning-controls > span {
  color: #6758dd;
  font-size: 9px;
  font-weight: 900;
}

.learning-controls > strong {
  font-size: 15px;
}

.learning-controls > p {
  margin: 0;
  color: #68758a;
  font-size: 10px;
  line-height: 1.6;
}

.learning-controls > div {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.learning-controls > div small {
  align-self: center;
  color: #7d8798;
  font-size: 9px;
}

.learning-playback-actions button {
  min-height: 32px;
}

.browser-bar {
  display: grid;
  grid-template-columns: 70px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 37px;
  border-bottom: 1px solid #dde1e8;
  background: #f3f4f7;
  padding: 0 12px;
}

.browser-bar > div {
  display: flex;
  gap: 5px;
}

.browser-bar i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #cbd0d9;
}

.browser-bar span {
  overflow: hidden;
  border: 1px solid #e0e3e9;
  border-radius: 6px;
  padding: 5px 8px;
  color: #8992a1;
  background: #fff;
  font-size: 8px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.browser-bar b {
  color: #19815f;
  font-size: 8px;
}

.business-app {
  display: grid;
  grid-template-columns: 125px minmax(0, 1fr);
  min-height: 563px;
}

.business-app > nav {
  display: grid;
  grid-auto-rows: 36px;
  align-content: start;
  border-right: 1px solid #e7e9ee;
  background: #222d42;
  padding: 16px 9px;
}

.business-app nav strong {
  margin-bottom: 15px;
  color: #fff;
  font-size: 11px;
}

.business-app nav a {
  display: flex;
  align-items: center;
  border-radius: 6px;
  padding: 0 8px;
  color: #aeb7c7;
  font-size: 9px;
}

.business-app nav a.active {
  color: #fff;
  background: #5364c9;
}

.business-app nav small {
  margin-top: 20px;
  color: #798499;
  font-size: 7px;
  line-height: 1.5;
}

.business-content {
  min-width: 0;
  padding: 17px;
}

.business-content > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  border-bottom: 1px solid #eceef2;
  padding-bottom: 12px;
}

.business-content header span,
.business-content header em {
  color: #9099a8;
  font-size: 8px;
  font-style: normal;
}

.business-content header h2 {
  margin: 5px 0 0;
  font-size: 15px;
}

.business-content header em {
  border-radius: 999px;
  padding: 5px 8px;
  color: #896d32;
  background: #fff4de;
}

.business-start {
  display: grid;
  min-height: 420px;
  place-items: center;
  align-content: center;
  gap: 10px;
  text-align: center;
}

.business-start > span {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border-radius: 17px;
  color: #5e4bd4;
  background: #eeebff;
  font-size: 17px;
  font-weight: 900;
}

.business-start.success > span {
  color: #08805c;
  background: #e8f8f2;
}

.business-start h3,
.business-start p {
  margin: 0;
}

.business-start h3 {
  color: #344056;
  font-size: 14px;
}

.business-start p {
  max-width: 390px;
  color: #8993a4;
  font-size: 9px;
  line-height: 1.6;
}

.origin-launch-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  border: 1px solid #dfe5f3;
  border-radius: 8px;
  padding: 12px;
  background: #fbfcff;
  text-align: left;
}

.origin-launch-card > div:first-child {
  display: grid;
  min-width: 0;
  gap: 5px;
}

.origin-launch-card strong {
  color: #344056;
  font-size: 10px;
}

.origin-launch-card small {
  color: #7f8998;
  font-size: 8px;
  line-height: 1.5;
}

.origin-launch-card__actions {
  display: flex;
  flex-shrink: 0;
  gap: 8px;
}

.origin-launch-card__actions button {
  min-height: 30px;
  font-size: 8px;
}

.submission-fields {
  display: grid;
  width: min(440px, 100%);
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
  margin: 6px 0;
  text-align: left;
}

.submission-fields label {
  font-size: 9px;
}

.submission-fields label span {
  color: #657186;
}

.submission-fields label b {
  color: #d64c59;
}

.submission-fields input {
  padding: 8px 9px;
  font-size: 9px;
}

.business-search {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(130px, 0.7fr) auto;
  align-items: end;
  gap: 9px;
  margin-top: 14px;
  border: 1px solid #e7e9ee;
  border-radius: 8px;
  padding: 11px;
  background: #fafbfc;
}

.business-search label,
.business-form label {
  font-size: 8px;
}

.business-search input,
.business-search select,
.business-form textarea {
  border-radius: 5px;
  padding: 7px 8px;
  font-size: 9px;
}

.business-search-button {
  min-height: 31px;
  border-color: #6373ce;
  border-radius: 5px;
  color: #fff;
  background: #6373ce;
  font-size: 8px;
}

.business-table {
  margin-top: 12px;
  overflow: hidden;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
}

.business-table__head,
.business-table__row {
  display: grid;
  grid-template-columns: 1.2fr 0.9fr 0.9fr 0.9fr 60px;
  align-items: center;
  gap: 8px;
  min-height: 39px;
  padding: 0 10px;
  font-size: 8px;
}

.business-table__head {
  color: #7f8998;
  background: #f4f5f8;
  font-weight: 800;
}

.business-table__row {
  color: #697587;
}

.business-table__row.highlighted {
  background: #fcfbff;
}

.business-table__row strong {
  color: #4f5ac0;
}

.business-table__row button {
  min-height: 25px;
  border: 0;
  padding: 0 7px;
  color: #5261bd;
  background: #edf0ff;
  font-size: 8px;
}

.business-form {
  display: grid;
  gap: 12px;
  margin-top: 12px;
  border: 1px solid #dedafc;
  border-radius: 8px;
  padding: 13px;
  background: #fbfaff;
}

.business-form > div:first-child {
  display: grid;
  gap: 4px;
}

.business-form span,
.business-form small {
  color: #8d86a8;
  font-size: 8px;
}

.business-form strong {
  color: #3f4960;
  font-size: 10px;
}

.business-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.business-form__actions button {
  min-height: 30px;
  font-size: 8px;
}

.guide-panel {
  border-left: 1px solid #e8ebf1;
  background: #fff;
}

.guide-checks {
  display: grid;
  margin-top: 17px;
}

.guide-checks > div {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 9px;
  min-height: 66px;
  opacity: 0.52;
}

.guide-checks > div.done {
  opacity: 1;
}

.guide-checks > div > span {
  display: grid;
  width: 27px;
  height: 27px;
  place-items: center;
  border-radius: 8px;
  color: #7566dc;
  background: #efedff;
  font-size: 9px;
  font-weight: 900;
}

.guide-checks > div.done > span {
  color: #087b59;
  background: #e8f8f2;
}

.guide-checks p,
.recorded-steps p {
  display: grid;
  gap: 3px;
  margin: 0;
}

.guide-checks strong,
.recorded-steps strong,
.recorded-steps b {
  color: #3b475c;
  font-size: 9px;
}

.guide-checks small,
.recorded-steps small {
  color: #8b95a6;
  font-size: 8px;
  line-height: 1.45;
}

.recorded-steps {
  display: grid;
  gap: 8px;
  margin-top: 5px;
  border-top: 1px solid #eceef3;
  padding-top: 15px;
}

.recorded-steps > div {
  display: grid;
  grid-template-columns: 20px minmax(0, 1fr) auto;
  gap: 7px;
  border-radius: 8px;
  padding: 8px;
  background: #f8f9fb;
}

.recorded-steps > div > span {
  display: grid;
  width: 18px;
  height: 18px;
  place-items: center;
  border-radius: 5px;
  color: #5e4bd0;
  background: #ece9ff;
  font-size: 7px;
}

.recorded-steps em {
  color: #9aa3b1;
  font-size: 7px;
  font-style: normal;
}

.guide-panel .notice {
  font-size: 8px;
  line-height: 1.5;
}

.session-info {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 14px;
  border-top: 1px solid #eceef3;
  padding-top: 14px;
}

.session-info span,
.session-info small {
  width: 100%;
  color: #8a94a5;
  font-size: 8px;
}

.session-info strong {
  border: 1px solid #dcd8ff;
  border-radius: 5px;
  padding: 4px 6px;
  color: #5d4dca;
  background: #f6f4ff;
  font-size: 7px;
}

@media (max-width: 1250px) {
  .runner-layout,
  .runner-layout.help-hidden {
    grid-template-columns: 190px minmax(0, 1fr);
  }

  .guide-panel {
    grid-column: 1 / -1;
    border-top: 1px solid #e8ebf1;
    border-left: 0;
  }

  .guide-checks {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 800px) {
  .runner-header,
  .runner-header > div {
    align-items: flex-start;
    flex-direction: column;
  }

  .runner-layout,
  .runner-layout.help-hidden {
    grid-template-columns: 1fr;
  }

  .stage-sidebar {
    border-right: 0;
    border-bottom: 1px solid #e8ebf1;
  }

  .business-search,
  .guide-checks,
  .submission-fields {
    grid-template-columns: 1fr;
  }

  .origin-launch-card,
  .origin-launch-card__actions {
    align-items: stretch;
    flex-direction: column;
  }

  .browser-bar {
    grid-template-columns: 55px minmax(0, 1fr);
  }

  .browser-bar b {
    display: none;
  }

  .business-app {
    grid-template-columns: 1fr;
  }

  .business-app > nav {
    display: none;
  }
}

/* 学习模式与教师讲解使用一致的沉浸式快照、说明浮窗和底部节点导航。 */
.learning-lecture-view,
.learning-preview-layout {
  position: absolute;
  inset: 0;
  min-height: 0;
}

.learning-lecture-view {
  z-index: 1;
}

.learning-preview-layout {
  z-index: 1;
}

.learning-business-stage {
  position: absolute;
  inset: 0;
  display: grid;
  height: 100vh;
  min-width: 0;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
  background: #fff;
}

.learning-snapshot-business-view {
  min-height: 0;
}

.learning-browser-chrome {
  display: grid;
  min-height: 38px;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 0 13px;
  color: #9da5ba;
  background: #242739;
  font-size: 9px;
}

.learning-browser-dots {
  display: flex;
  gap: 5px;
}

.learning-browser-dots i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #e46870;
}

.learning-browser-dots i:nth-child(2) {
  background: #dca448;
}

.learning-browser-dots i:nth-child(3) {
  background: #55b98f;
}

.learning-browser-address {
  overflow: hidden;
  border-radius: 5px;
  padding: 5px 10px;
  background: #303447;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.learning-playback-bar {
  display: grid;
  min-height: 52px;
  grid-template-columns: auto minmax(80px, 1fr) auto;
  align-items: center;
  gap: 10px;
  border-top: 1px solid #e4e7ee;
  padding: 0 13px;
  color: #7a8597;
  background: #fff;
  font-size: 9px;
}

.learning-playback-bar > div,
.learning-preview-progress {
  overflow: hidden;
  border-radius: 20px;
  background: #e9e7fa;
}

.learning-playback-bar > div {
  height: 4px;
}

.learning-playback-bar i,
.learning-preview-progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #6958ee, #9b90ff);
  transition: width 200ms ease;
}

.learning-preview-metrics,
.learning-segment-timeline,
.learning-explanation-panel {
  position: absolute;
  z-index: 20;
  border: 1px solid rgb(218 223 234 / 88%);
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 18px 46px rgb(23 29 55 / 18%);
  backdrop-filter: blur(14px);
}

.learning-preview-metrics {
  top: 82px;
  left: 14px;
  display: grid;
  width: min(430px, calc(100vw - 28px));
  grid-template-columns: repeat(4, minmax(80px, 1fr));
  overflow: hidden;
  border-radius: 12px;
}

.learning-preview-metrics > span {
  display: grid;
  gap: 3px;
  border-right: 1px solid #eceef3;
  padding: 9px 12px 12px;
}

.learning-preview-metrics small {
  color: #8993a4;
  font-size: 9px;
}

.learning-preview-metrics strong {
  font-size: 13px;
}

.learning-preview-progress {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 3px;
}

.learning-explanation-panel {
  z-index: 35;
  display: grid;
  width: min(430px, calc(100vw - 36px));
  max-height: calc(100vh - 36px);
  grid-template-rows: auto auto 1fr auto;
  align-content: start;
  overflow-y: auto;
  border-radius: 16px;
}

.learning-step-prompt.stage-prompt {
  top: 50%;
  left: 50%;
  width: min(500px, calc(100vw - 36px));
  transform: translate(-50%, -50%);
}

.learning-step-prompt.node-prompt {
  top: 50%;
  left: 18px;
  transform: translateY(-50%);
}

.learning-explanation-heading {
  display: grid;
  gap: 5px;
  border-bottom: 1px solid #e9ecf2;
  padding: 20px;
}

.learning-explanation-heading > span {
  color: #6c5ce7;
  font-size: 9px;
  font-weight: 900;
}

.learning-explanation-heading strong {
  font-size: 17px;
}

.learning-explanation-heading small {
  color: #8a94a5;
  font-size: 10px;
}

.learning-explanation-heading.stage-introduction-heading {
  background: linear-gradient(135deg, #f7f5ff, #fff);
}

.learning-instruction {
  margin: 16px;
  border: 1px solid #dfdbff;
  border-radius: 11px;
  padding: 14px;
  background: #f7f5ff;
}

.learning-instruction.stage-introduction {
  border-color: #cfc8ff;
  background: linear-gradient(145deg, #f3f0ff, #fbfaff);
}

.learning-instruction span {
  color: #6555df;
  font-size: 9px;
  font-weight: 900;
}

.learning-instruction p {
  margin: 7px 0 0;
  color: #566277;
  font-size: 12px;
  line-height: 1.75;
}

.learning-explanation-panel :deep(.attachment-panel) {
  margin: 0 16px 14px;
}

.learning-explanation-panel dl {
  display: grid;
  align-content: start;
  gap: 0;
  margin: 0;
  padding: 0 18px 18px;
}

.learning-explanation-panel dl > div {
  display: grid;
  grid-template-columns: 105px minmax(0, 1fr);
  gap: 8px;
  border-bottom: 1px solid #edf0f5;
  padding: 11px 0;
}

.learning-explanation-panel dt {
  color: #8b95a7;
  font-size: 9px;
}

.learning-explanation-panel dd {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  color: #4c596e;
  font-size: 10px;
}

.learning-step-controls,
.learning-state-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  border-top: 1px solid #e8ebf1;
  padding: 14px;
}

.learning-state-actions > :only-child {
  grid-column: 1 / -1;
}

.learning-feedback {
  margin: 0 14px 10px;
  border-radius: 9px;
  padding: 10px;
  font-size: 9px;
  line-height: 1.5;
}

.learning-feedback.success {
  color: #087b59;
  background: #eaf8f2;
}

.learning-feedback.danger {
  color: #a43d48;
  background: #fff0f1;
}

.learning-empty-business {
  display: grid;
  place-items: center;
  color: #7b8699;
  background: #f4f6fa;
  font-size: 13px;
}

.learning-segment-timeline {
  right: 14px;
  bottom: 66px;
  left: 14px;
  overflow: hidden;
  border-radius: 13px;
}

.learning-teaching-point-list,
.learning-segment-list {
  display: flex;
  overflow-x: auto;
}

.learning-teaching-point-list {
  gap: 8px;
  border-bottom: 1px solid #eceef4;
  padding: 10px 13px 8px;
}

.learning-teaching-point-list button {
  display: grid;
  flex: 0 0 180px;
  grid-template-columns: auto minmax(90px, 1fr);
  align-items: center;
  gap: 2px 7px;
  border-color: #e2e5ec;
  padding: 7px 9px;
  text-align: left;
}

.learning-teaching-point-list button.active,
.learning-segment-list button.active {
  border-color: #9185f8;
  background: #f4f2ff;
}

.learning-teaching-point-list span,
.learning-segment-list button > span {
  display: grid;
  grid-row: 1 / 3;
  place-items: center;
  color: #6656dd;
  background: #ece9ff;
}

.learning-teaching-point-list span {
  width: 23px;
  height: 23px;
  border-radius: 7px;
  font-size: 9px;
}

.learning-teaching-point-list strong,
.learning-segment-list strong {
  overflow: hidden;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.learning-teaching-point-list small,
.learning-segment-list small {
  color: #8a94a6;
  font-size: 8px;
}

.learning-segment-list {
  gap: 9px;
  padding: 9px;
}

.learning-segment-list button {
  display: grid;
  flex: 0 0 190px;
  grid-template-columns: 25px minmax(110px, 1fr);
  align-items: center;
  gap: 2px 8px;
  border-color: #e2e5ec;
  padding: 9px;
  text-align: left;
}

.learning-segment-list button > span {
  width: 25px;
  height: 25px;
  border-radius: 8px;
  font-size: 9px;
}

.learning-lecture-page .runner-menu-toggle {
  top: 14px;
}

.runner-menu-hidden .learning-business-stage {
  grid-template-rows: minmax(0, 1fr);
}

@media (max-height: 720px) {
  .learning-preview-metrics {
    display: none;
  }

  .learning-segment-timeline {
    bottom: 58px;
  }

  .learning-teaching-point-list {
    display: none;
  }

  .learning-explanation-heading {
    padding: 14px;
  }

  .learning-instruction {
    margin: 10px 14px;
    padding: 10px;
  }

  .learning-explanation-panel dl > div {
    padding: 7px 0;
  }
}

@media (max-height: 560px) {
  .learning-segment-timeline,
  .learning-explanation-panel dl,
  .learning-explanation-panel :deep(.attachment-panel) {
    display: none;
  }

  .learning-explanation-panel {
    max-height: calc(100vh - 24px);
  }
}

@media (max-width: 880px) {
  .learning-preview-metrics,
  .learning-segment-timeline {
    display: none;
  }

  .learning-explanation-panel,
  .learning-step-prompt.stage-prompt,
  .learning-step-prompt.node-prompt {
    top: auto;
    right: 12px;
    bottom: 12px;
    left: 12px;
    width: auto;
    max-height: 46vh;
    transform: none;
  }

  .learning-explanation-panel dl,
  .learning-instruction {
    display: none;
  }
}

/* 沉浸式任务：业务界面使用完整视口，教学点与操作引导作为上层浮窗。 */
.runner-page {
  position: relative;
  width: 100%;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  background: #eef1f6;
}

.runner-header {
  position: absolute;
  z-index: 30;
  top: 12px;
  right: 12px;
  left: 12px;
  margin: 0;
  border-color: rgb(220 224 233 / 88%);
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 16px 42px rgb(21 27 52 / 17%);
  backdrop-filter: blur(14px);
}

.mode-badge {
  border-radius: 999px;
  padding: 5px 8px;
  color: #5f50cf !important;
  background: #efedff;
  font-size: 8px !important;
  font-weight: 900;
}

.runner-layout,
.runner-layout.help-hidden {
  position: absolute;
  z-index: 1;
  inset: 0;
  display: block;
  min-height: 0;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.business-canvas {
  position: absolute;
  z-index: 1;
  inset: 0;
  padding: 0;
}

.mock-browser {
  height: 100vh;
  min-height: 0;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.learning-playback {
  height: 100vh;
  min-height: 0;
}

.practice-live-business-view {
  position: absolute;
  inset: 0;
  width: 100vw;
  height: 100vh;
  min-height: 0;
  background: #fff;
}

.business-app {
  min-height: calc(100vh - 37px);
}

.stage-sidebar,
.guide-panel {
  position: absolute;
  z-index: 20;
  top: 76px;
  bottom: 14px;
  overflow-y: auto;
  border: 1px solid rgb(220 224 233 / 90%);
  border-radius: 14px;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 18px 46px rgb(21 27 52 / 18%);
  backdrop-filter: blur(14px);
}

.stage-sidebar {
  left: 14px;
  width: 230px;
}

.guide-panel {
  right: 14px;
  width: 300px;
}

.runner-layout.learning-mode .learning-controls {
  right: 18px;
  bottom: 18px;
  width: min(460px, calc(100vw - 280px));
  max-height: calc(100vh - 110px);
  overflow-y: auto;
}

.runner-layout.help-hidden .business-canvas {
  inset: 0;
}

.runner-menu-toggle,
.exam-task-toggle {
  position: absolute;
  z-index: 45;
  top: 78px;
  right: 14px;
  min-height: 33px;
  border: 1px solid rgb(255 255 255 / 78%);
  border-radius: 999px;
  padding: 0 13px;
  color: #fff;
  background: rgb(31 39 58 / 80%);
  box-shadow: 0 10px 28px rgb(15 20 40 / 22%);
  backdrop-filter: blur(12px);
  font-size: 9px;
  font-weight: 800;
}

.runner-menu-hidden .runner-menu-toggle {
  top: 14px;
}

.runner-menu-hidden .training-business-flow :deep(.snapshot-status) {
  display: none;
}

.practice-edge-toolbar {
  position: absolute;
  z-index: 65;
  top: 14px;
  right: 14px;
  display: flex;
  gap: 6px;
  border: 1px solid rgb(255 255 255 / 72%);
  border-radius: 999px;
  padding: 5px;
  background: rgb(25 32 49 / 82%);
  box-shadow: 0 10px 28px rgb(15 20 40 / 24%);
  backdrop-filter: blur(12px);
}

.practice-edge-toolbar a,
.practice-edge-toolbar button {
  display: grid;
  min-width: 64px;
  min-height: 30px;
  place-items: center;
  border: 0;
  border-radius: 999px;
  padding: 0 11px;
  color: #fff;
  background: transparent;
  font-size: 11px;
  font-weight: 800;
  text-decoration: none;
  cursor: pointer;
}

.practice-edge-toolbar a:hover,
.practice-edge-toolbar button:hover,
.practice-edge-toolbar button.active {
  background: rgb(255 255 255 / 16%);
}

.practice-edge-toolbar button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.practice-hint-drawer {
  position: absolute;
  z-index: 60;
  top: 58px;
  right: 14px;
  bottom: 14px;
  display: flex;
  width: min(360px, calc(100vw - 28px));
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgb(218 223 234 / 90%);
  border-radius: 16px;
  color: #39455a;
  background: rgb(255 255 255 / 96%);
  box-shadow: 0 20px 54px rgb(23 29 55 / 24%);
  backdrop-filter: blur(16px);
}

.practice-hint-drawer > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e9edf4;
  padding: 14px 16px;
}

.practice-hint-drawer > header div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.practice-hint-drawer > header small {
  color: #6b5dd3;
  font-size: 9px;
  font-weight: 900;
}

.practice-hint-drawer > header strong {
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.practice-hint-drawer > header button {
  flex: 0 0 auto;
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 50%;
  color: #526077;
  background: #eef1f6;
  cursor: pointer;
}

.practice-hint-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  border-bottom: 1px solid #edf0f5;
}

.practice-hint-metrics > span {
  display: grid;
  gap: 3px;
  padding: 10px 14px;
}

.practice-hint-metrics > span + span {
  border-left: 1px solid #edf0f5;
}

.practice-hint-metrics small {
  color: #929bab;
  font-size: 8px;
}

.practice-hint-metrics strong {
  color: #4b5870;
  font-size: 14px;
}

.practice-hint-drawer :deep(.playback-navigation-tree) {
  min-height: 120px;
  flex: 1 1 auto;
}

.practice-hint-detail,
.practice-hint-empty {
  display: grid;
  flex: 0 0 auto;
  gap: 7px;
  max-height: 42%;
  overflow-y: auto;
  border-top: 1px solid #e9edf4;
  padding: 13px 15px;
  background: #fafbff;
}

.practice-hint-detail > small {
  color: #6b5dd3;
  font-size: 9px;
  font-weight: 900;
}

.practice-hint-detail > strong,
.practice-hint-empty > strong {
  font-size: 13px;
}

.practice-hint-detail > p,
.practice-hint-empty > p {
  margin: 0;
  color: #68758a;
  font-size: 10px;
  line-height: 1.6;
}

.practice-hint-detail dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
  overflow: hidden;
  border: 1px solid #e4e7f0;
  border-radius: 9px;
  background: #fff;
}

.practice-hint-detail dl > div {
  display: grid;
  gap: 3px;
  min-width: 0;
  padding: 8px 9px;
}

.practice-hint-detail dl > div + div {
  border-left: 1px solid #e9ebf2;
}

.practice-hint-detail dt {
  color: #929bab;
  font-size: 8px;
}

.practice-hint-detail dd {
  overflow: hidden;
  margin: 0;
  font-size: 9px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.practice-hint-detail > span {
  border-radius: 8px;
  padding: 7px 9px;
  color: #5b4dc1;
  background: #efedff;
  font-size: 9px;
  line-height: 1.5;
}

.practice-hint-continue {
  flex: 0 0 auto;
  margin: 0 14px 14px;
}

.runner-layout.exam-mode {
  display: block;
}

.exam-task-panel {
  position: absolute;
  z-index: 35;
  top: 14px;
  left: 50%;
  display: flex;
  width: min(920px, calc(100vw - 28px));
  min-height: 58px;
  align-items: center;
  gap: 14px;
  transform: translateX(-50%);
  border: 1px solid rgb(222 226 235 / 92%);
  border-radius: 14px;
  padding: 10px 12px 10px 16px;
  color: #39455a;
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 16px 42px rgb(21 27 52 / 18%);
  backdrop-filter: blur(14px);
}

.exam-task-panel > div {
  display: grid;
  flex: 0 0 auto;
  gap: 2px;
}

.exam-task-panel > div span {
  color: #6b5cdf;
  font-size: 8px;
  font-weight: 900;
}

.exam-task-panel > div strong {
  font-size: 12px;
}

.exam-task-panel > div small {
  color: #8893a5;
  font-size: 8px;
}

.exam-task-panel > p {
  min-width: 0;
  flex: 1;
  margin: 0;
  color: #68758a;
  font-size: 9px;
  line-height: 1.5;
}

.exam-task-panel > button {
  flex: 0 0 auto;
  min-height: 31px;
  font-size: 8px;
}

.exam-message {
  max-width: 180px;
  border-radius: 7px;
  padding: 7px 9px;
  font-size: 8px;
}

.exam-message.success {
  color: #087b59;
  background: #eaf8f2;
}

.exam-message.danger {
  color: #a43d48;
  background: #fff0f1;
}

.exam-task-toggle {
  top: 14px;
}

.operation-target {
  position: relative;
  z-index: 6;
  outline: 4px solid #ff9f1c !important;
  outline-offset: 4px;
  box-shadow: 0 0 0 8px rgb(255 159 28 / 22%), 0 0 28px rgb(255 93 46 / 64%) !important;
  animation: operation-target-pulse 1.1s ease-in-out infinite alternate;
}

.operation-target::after {
  position: absolute;
  z-index: 7;
  top: -32px;
  left: 50%;
  transform: translateX(-50%);
  border-radius: 999px;
  padding: 5px 9px;
  color: #fff;
  background: #e66b00;
  box-shadow: 0 8px 20px rgb(116 50 0 / 26%);
  content: "当前操作";
  font-size: 8px;
  font-weight: 900;
  white-space: nowrap;
}

@keyframes operation-target-pulse {
  from {
    outline-color: #ff9f1c;
    box-shadow: 0 0 0 5px rgb(255 159 28 / 18%), 0 0 18px rgb(255 159 28 / 48%);
  }
  to {
    outline-color: #ff5d2e;
    box-shadow: 0 0 0 11px rgb(255 93 46 / 20%), 0 0 34px rgb(255 93 46 / 76%);
  }
}

@media (max-width: 980px) {
  .runner-header > div:last-child > span {
    display: none;
  }

  .stage-sidebar {
    display: none;
  }

  .guide-panel {
    top: auto;
    right: 12px;
    bottom: 12px;
    left: 12px;
    width: auto;
    max-height: 42vh;
  }

  .learning-controls {
    right: 12px;
    bottom: 12px;
    left: 12px;
    width: auto;
  }

  .guide-checks {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .exam-task-panel {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .exam-task-panel > p {
    flex-basis: calc(100% - 190px);
  }
}

@media (max-width: 620px) {
  .practice-edge-toolbar {
    top: 8px;
    right: 8px;
  }

  .practice-hint-drawer {
    top: 52px;
    right: 8px;
    bottom: 8px;
    width: calc(100vw - 16px);
  }

  .practice-hint-detail {
    max-height: 36%;
  }

  .runner-header {
    align-items: stretch;
    padding: 9px;
  }

  .runner-header > div:last-child {
    display: none;
  }

  .guide-checks {
    grid-template-columns: 1fr;
  }

  .recorded-steps,
  .session-info {
    display: none;
  }
}
</style>
