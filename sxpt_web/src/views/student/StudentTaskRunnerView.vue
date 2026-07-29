<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';
import BusinessSnapshotFrame from '../../components/lesson/BusinessSnapshotFrame.vue';
import type { RecordedStep } from '../../domain/models';
import StatusPill from '../../components/ui/StatusPill.vue';
import {
  authApi,
  dataPrepareApi,
  type DataInstanceAllocation,
  type StudentDataLaunchResult
} from '../../services/trainingApi';
import { useTrainingStore } from '../../stores/trainingStore';

interface BusinessActionPayload {
  actionType: 'click' | 'input' | 'select' | 'submit';
  selector: string;
  selectorCandidates?: string[];
  text: string;
}

const store = useTrainingStore();
store.refreshPublishedTaskStatuses();
const route = useRoute();
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
    (item) => item.id === String(route.params.taskId)
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
const visibleStages = computed(
  () =>
    lesson.value?.stages.filter(
      (stage) => task.value && stage.visibility[task.value.mode]
    ) ?? []
);
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
const learningSteps = computed(() =>
  visibleStages.value
    .filter((stage) => assignedStageIds.value.has(stage.id))
    .flatMap((stage) =>
      stage.recordedSteps.map((step) => ({ stage, step }))
    )
);
const currentLearningStep = computed(
  () => learningSteps.value[learningStepIndex.value]
);
const displayedLearningStep = computed(
  () =>
    currentLearningStep.value ??
    learningSteps.value[Math.max(0, learningSteps.value.length - 1)]
);
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
const currentLearningStageIndex = computed(() =>
  visibleStages.value.findIndex(
    (stage) => stage.id === currentLearningStep.value?.stage.id
  )
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
const allAssignedStagesComplete = computed(
  () =>
    [...assignedStageIds.value].length > 0 &&
    [...assignedStageIds.value].every((id) =>
      task.value?.completedStageIds.includes(id)
    )
);
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
const isExam = computed(() => task.value?.mode === 'EXAM');
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

watch(
  () => [task.value?.id, task.value?.status] as const,
  () => {
    syncLearningProgress(task.value?.status === 'DOING');
    syncSubmissionValues();
  },
  { immediate: true }
);

function syncLearningProgress(introduceStage = false) {
  const firstIncompleteIndex = learningSteps.value.findIndex(
    ({ stage }) => !task.value?.completedStageIds.includes(stage.id)
  );
  learningStepIndex.value =
    firstIncompleteIndex >= 0
      ? firstIncompleteIndex
      : learningSteps.value.length;
  showStageIntroduction.value =
    introduceStage && Boolean(learningSteps.value[learningStepIndex.value]);
}

async function startTask() {
  if (!task.value) return;
  try {
    syncing.value = true;
    await store.startStudentTaskRemote(task.value.id);
    syncLearningProgress(task.value.mode !== 'EXAM');
    message.value =
      task.value.mode === 'EXAM'
        ? '任务已开始。请按考试说明独立完成业务操作。'
        : '任务已开始。请在录制业务系统的高亮位置完成当前操作。';
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
      '平台已记录本阶段的页面访问与流程操作。若有下一阶段，请继续办理。';
  } catch (error) {
    errorMessage.value =
      error instanceof Error
        ? error.message
        : '本阶段暂不能完成，请确认前序角色是否已提交。';
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
      error instanceof Error ? error.message : '提交失败，请检查阶段完成情况。';
  } finally {
    syncing.value = false;
  }
}

function resetLocalAttempt() {
  checks.entered = false;
  checks.located = false;
  checks.reviewed = false;
  message.value = '已重新开始当前未提交阶段，平台不会回退已经完成的业务数据。';
  errorMessage.value = '';
}

function restartAttempt() {
  if (!task.value) return;
  const participantCount = collaborationTasks.value.length;
  const confirmed = window.confirm(
    `重新作答将调用 Mock 业务接口生成一条同单位的新业务数据，并重置共享该数据的 ${participantCount} 个学员任务；旧业务数据不会回滚。确认继续吗？`
  );
  if (!confirmed) return;
  message.value = '';
  errorMessage.value = '';
  attemptMessage.value = '';
  try {
    store.restartStudentAttempt(task.value.id);
    checks.entered = false;
    checks.located = false;
    checks.reviewed = false;
    syncSubmissionValues();
    attemptMessage.value =
      '已生成新的同条件业务数据，协作单元已整体进入下一次作答；旧数据仅停用，不执行回滚。';
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '重新作答失败，请稍后重试。';
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
      stageFinished && Boolean(learningSteps.value[learningStepIndex.value]);
    message.value =
      learningStepIndex.value >= learningSteps.value.length
        ? '教案录制的完整业务流程已操作完成，可以提交本次任务。'
        : `已完成“${current.step.title}”，请继续下一项录制操作。`;
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '当前录制步骤同步失败。';
  } finally {
    syncing.value = false;
  }
}

async function handleRecordedBusinessAction(payload: BusinessActionPayload) {
  if (
    !task.value ||
    task.value.mode === 'EXAM' ||
    task.value.status !== 'DOING' ||
    !currentLearningStep.value ||
    showStageIntroduction.value ||
    syncing.value
  ) {
    return;
  }
  if (!actionMatchesStep(payload, currentLearningStep.value.step)) {
    errorMessage.value = `当前应完成“${currentLearningStep.value.step.actionLabel}”，请操作高亮位置。`;
    return;
  }
  await advanceLearningStep();
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
  message.value = `已进入“${currentLearningStep.value.stage.name}”，请完成第一个录制节点。`;
  errorMessage.value = '';
}

function restartTrainingTask() {
  if (
    !task.value ||
    task.value.mode === 'EXAM'
  ) {
    return;
  }
  try {
    store.restartLearningOrPractice(task.value.id);
    syncLearningProgress(false);
    message.value =
      task.value.mode === 'LEARNING'
        ? '已重置学习记录，可以重新学习完整业务流程。'
        : '已重置练习记录，可以重新练习完整业务流程。';
    errorMessage.value = '';
    showRunnerMenu.value = true;
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '任务重置失败。';
  }
}
</script>

<template>
  <section
    v-if="task && lesson"
    class="runner-page"
    :class="{
      'runner-page--exam': isExam,
      'runner-menu-hidden': !showRunnerMenu
    }"
  >
    <button
      v-if="!isExam"
      class="runner-menu-toggle"
      type="button"
      @click="showRunnerMenu = !showRunnerMenu"
    >
      {{ showRunnerMenu ? '隐藏上层菜单' : '显示上层菜单' }}
    </button>

    <header v-if="!isExam" v-show="showRunnerMenu" class="runner-header">
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

    <div
      class="runner-layout"
      :class="{
        'help-hidden': !showHelp,
        'exam-mode': isExam
      }"
    >
      <aside v-if="!isExam && showRunnerMenu" class="stage-sidebar">
        <span class="runner-kicker">BUSINESS WORKFLOW</span>
        <h1>业务阶段</h1>
        <p>
          各角色按顺序完成同一批业务流程；你仅需办理分配给自己的阶段。
        </p>
        <div v-if="examSettings?.showProgress !== false" class="stage-list">
          <div
            v-for="(stage, index) in visibleStages"
            :key="stage.id"
            :class="{
              assigned: assignedStageIds.has(stage.id),
              complete: task.completedStageIds.includes(stage.id),
              current:
                stage.id ===
                (currentLearningStep?.stage.id ?? nextStage?.id)
            }"
          >
            <span>
              {{ task.completedStageIds.includes(stage.id) ? '✓' : index + 1 }}
            </span>
            <section>
              <strong>{{ stage.name }}</strong>
              <small>
                {{
                  assignedStageIds.has(stage.id)
                    ? '由我办理'
                    : '等待其他角色办理'
                }}
              </small>
            </section>
          </div>
        </div>
        <div v-else class="hidden-progress">
          <strong>考试进度已隐藏</strong>
          <p>按考试设置，仅显示当前操作引导，不展示完整业务阶段。</p>
        </div>
        <div class="evidence-boundary">
          <strong>当前判定口径</strong>
          <p>
            访问目标页面 + 定位业务待办 + 执行流程按钮，即判定本阶段完成。
          </p>
        </div>
      </aside>

      <main class="business-canvas">
        <div
          v-if="!isExam"
          class="learning-playback training-business-flow"
        >
          <BusinessSnapshotFrame
            v-if="displayedLearningStep"
            :snapshot="displayedLearningStep.step.pageSnapshot"
            :fallback-url="displayedLearningStep.step.url"
            :selector="
              showStageIntroduction ? undefined : currentLearningStep?.step.selector
            "
            :selector-candidates="
              showStageIntroduction
                ? undefined
                : currentLearningStep?.step.selectorCandidates
            "
            :rect="
              showStageIntroduction ? undefined : currentLearningStep?.step.rect
            "
            :recorded-viewport="
              showStageIntroduction
                ? undefined
                : currentLearningStep?.step.recordedViewport
            "
            :interactive="
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
            v-else-if="task.status === 'TODO'"
            class="training-state-overlay"
          >
            <span>01</span>
            <h3>准备进入录制时的业务系统</h3>
            <p>
              开始后请依次完成教师录制的 {{ learningSteps.length }}
              个业务操作，系统将按真实教案顺序推进。
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
            v-else-if="showStageIntroduction && currentLearningStep"
            class="training-state-overlay stage-introduction-overlay"
          >
            <span>{{ currentLearningStageIndex + 1 }}</span>
            <small>
              本阶段说明 · 阶段 {{ currentLearningStageIndex + 1 }} /
              {{ visibleStages.length }}
            </small>
            <h3>{{ currentLearningStep.stage.name }}</h3>
            <p>
              {{
                currentLearningStep.stage.description ||
                '本阶段暂无补充说明，请按照录制节点顺序完成业务操作。'
              }}
            </p>
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
              {{ syncing ? '正在初始化任务…' : '进入本阶段' }}
            </button>
          </div>

          <div
            v-else-if="allAssignedStagesComplete && !currentLearningStep"
            class="training-state-overlay success"
          >
            <span>✓</span>
            <h3>教案录制的整个业务流程已完成</h3>
            <p>确认提交后，平台将保存本次{{ modeLabel }}的完整流程轨迹。</p>
            <button
              class="primary"
              type="button"
              :disabled="syncing"
              @click="submitTask"
            >
              {{ syncing ? '正在提交…' : `完成本次${modeLabel}` }}
            </button>
          </div>

          <div
            v-if="
              showRunnerMenu &&
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
            <p>
              {{
                task.mode === 'LEARNING'
                  ? currentLearningStep.step.teachingText ||
                    currentLearningStep.step.note ||
                    '请在高亮位置重做教师录制的业务操作。'
                  : currentLearningStep.step.practiceHint ||
                    currentLearningStep.step.note ||
                    '请在高亮位置完成当前业务操作。'
              }}
            </p>
            <div>
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
                  <h2>{{ nextStage?.name ?? '本角色阶段已完成' }}</h2>
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
                    :disabled="launchLoading || allocationLoading || !activeAllocationId"
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
                    系统已保存学习轨迹与阶段完成记录，可以返回任务中心继续下一项任务。
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
                <h3>你负责的业务阶段已全部完成</h3>
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
                      重新进入本阶段
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
        v-if="showRunnerMenu && !isExam && !showStageIntroduction"
        class="guide-panel"
      >
        <span class="runner-kicker">RECORDED GUIDE</span>
        <h2>{{ currentLearningStep?.step.title ?? '流程完成' }}</h2>
        <p>
          {{
            currentLearningStep
              ? task.mode === 'LEARNING'
                ? currentLearningStep.step.teachingText ||
                  currentLearningStep.step.note ||
                  '请观察高亮位置，并完成教师录制的对应操作。'
                : currentLearningStep.step.practiceHint ||
                  currentLearningStep.step.note ||
                  '请在高亮位置独立完成当前操作。'
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
          <strong>本阶段录制步骤</strong>
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
          <strong>第 {{ task.attemptNumber }} 次{{ task.mode === 'LEARNING' ? '学习' : '练习' }}</strong>
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

/* 沉浸式任务：业务界面使用完整视口，阶段与操作引导作为上层浮窗。 */
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
