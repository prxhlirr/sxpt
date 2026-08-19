<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import ResultArchiveCard from '../../components/evaluation/ResultArchiveCard.vue';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusPill from '../../components/ui/StatusPill.vue';
import type { LessonStage, StudentTask } from '../../domain/models';
import { useTrainingStore } from '../../stores/trainingStore';
import { isRequiredPracticeStep } from '../../utils/practiceStep';

type ReviewFilter = 'DOING' | 'PENDING' | 'GRADED' | 'ALL';

const store = useTrainingStore();
const filter = ref<ReviewFilter>('PENDING');
const courseFilter = ref('ALL');
const reviewFilters: Array<{ value: ReviewFilter; label: string }> = [
  { value: 'PENDING', label: '待评阅' },
  { value: 'DOING', label: '练习中' },
  { value: 'GRADED', label: '已评分' },
  { value: 'ALL', label: '全部' }
];
const selectedTaskId = ref('');
const detailOpen = ref(false);
const evidencePreviewOpen = ref(false);
const evidencePreviewIndex = ref(0);
const subjectiveScore = ref(0);
const comment = ref('');
const message = ref('');
const loading = ref(false);

const reviewEligibleTasks = computed(() =>
  store.state.studentTasks.filter(
    (task) =>
      (task.mode === 'PRACTICE' && task.status === 'DOING') ||
      task.status === 'SUBMITTED' ||
      task.status === 'GRADED'
  )
);
const courseOptions = computed(() => {
  const options = new Map<string, { id: string; title: string; scope: string; count: number }>();
  reviewEligibleTasks.value.forEach((task) => {
    const current = options.get(task.lessonId);
    if (current) {
      current.count += 1;
      return;
    }
    options.set(task.lessonId, {
      id: task.lessonId,
      title: lessonFor(task)?.title ?? task.title,
      scope: businessScope(task),
      count: 1
    });
  });
  return [...options.values()];
});
const courseScopedTasks = computed(() =>
  reviewEligibleTasks.value.filter(
    (task) => courseFilter.value === 'ALL' || task.lessonId === courseFilter.value
  )
);
const selectedCourse = computed(() =>
  courseOptions.value.find((course) => course.id === courseFilter.value)
);
const reviewTasks = computed(() =>
  courseScopedTasks.value.filter((task) => {
    if (filter.value === 'DOING') {
      return task.mode === 'PRACTICE' && task.status === 'DOING';
    }
    if (filter.value === 'PENDING') return task.status === 'SUBMITTED';
    if (filter.value === 'GRADED') return task.status === 'GRADED';
    return true;
  })
);
const selectedTask = computed(() =>
  store.state.studentTasks.find((task) => task.id === selectedTaskId.value)
);
const lesson = computed(() =>
  store.state.lessons.find((item) => item.id === selectedTask.value?.lessonId)
);
const reviewStages = computed(() => {
  const stages = lesson.value?.stages ?? [];
  return selectedTask.value?.mode === 'PRACTICE'
    ? stages.filter((stage) => requiredPracticeStepCount(stage) > 0)
    : stages;
});
const completedReviewStageCount = computed(() =>
  reviewStages.value.filter((stage) =>
    selectedTask.value?.completedStageIds.includes(stage.id)
  ).length
);
const groupPlan = computed(() =>
  selectedTask.value
    ? store.state.groupPlans[selectedTask.value.lessonId]
    : undefined
);
const maxSubjectiveScore = computed(() =>
  selectedTask.value ? subjectiveMaxFor(selectedTask.value) : 0
);
const maxObjectiveScore = computed(() =>
  selectedTask.value ? objectiveMaxFor(selectedTask.value) : 0
);
const pendingCount = computed(
  () => courseScopedTasks.value.filter((task) => task.status === 'SUBMITTED').length
);
const doingCount = computed(
  () =>
    courseScopedTasks.value.filter(
      (task) => task.mode === 'PRACTICE' && task.status === 'DOING'
    ).length
);
const gradedCount = computed(
  () => courseScopedTasks.value.filter((task) => task.status === 'GRADED').length
);
const selectedPracticeSteps = computed(() =>
  [...(selectedTask.value?.practiceStepResults ?? [])].sort(
    (first, second) =>
      Date.parse(first.completedAt) - Date.parse(second.completedAt)
  )
);
const evidenceGallery = computed(() =>
  selectedPracticeSteps.value.flatMap((result) => {
    const screenshot = result.evidenceScreenshot;
    if (!screenshot?.dataUrl) return [];
    return [
      {
        result,
        screenshot,
        stepName: stepTitle(result.stepId),
        stageName: stageTitle(result.stageId)
      }
    ];
  })
);
const currentEvidence = computed(
  () => evidenceGallery.value[evidencePreviewIndex.value]
);

watch(
  courseOptions,
  (courses) => {
    if (
      courseFilter.value !== 'ALL' &&
      !courses.some((course) => course.id === courseFilter.value)
    ) {
      courseFilter.value = 'ALL';
    }
  },
  { immediate: true }
);

watch(
  reviewTasks,
  (tasks) => {
    if (!tasks.some((task) => task.id === selectedTaskId.value)) {
      selectedTaskId.value = tasks[0]?.id ?? '';
      if (!selectedTaskId.value) detailOpen.value = false;
    }
  },
  { immediate: true }
);

watch(
  selectedTask,
  (task) => {
    subjectiveScore.value = task?.subjectiveScore ?? 0;
    comment.value = task?.comment ?? '';
    message.value = '';
  },
  { immediate: true }
);

watch(evidenceGallery, (items) => {
  if (!items.length) {
    evidencePreviewOpen.value = false;
    evidencePreviewIndex.value = 0;
    return;
  }
  evidencePreviewIndex.value = Math.min(
    evidencePreviewIndex.value,
    items.length - 1
  );
});

function lessonFor(task: StudentTask) {
  return store.state.lessons.find((item) => item.id === task.lessonId);
}

function objectiveMaxFor(task: StudentTask) {
  return (
    store.state.examSettings[task.lessonId]?.objectiveWeight ??
    lessonFor(task)?.objectiveMaxScore ??
    0
  );
}

function subjectiveMaxFor(task: StudentTask) {
  return (
    store.state.examSettings[task.lessonId]?.subjectiveWeight ??
    lessonFor(task)?.subjectiveMaxScore ??
    0
  );
}

function roleNames(groupKeys: string[]) {
  return groupKeys
    .map(
      (key) =>
        groupPlan.value?.roles.find((role) => role.key === key)?.name ?? key
    )
    .join('、');
}

function roleNamesForTask(task: StudentTask) {
  const plan = store.state.groupPlans[task.lessonId];
  return task.groupKeys
    .map((key) => plan?.roles.find((role) => role.key === key)?.name ?? key)
    .join('、');
}

function businessScope(task: StudentTask) {
  const taskLesson = lessonFor(task);
  const platform = store.state.businessPlatforms.find(
    (item) => item.id === taskLesson?.businessPlatformId
  );
  const businessModule = platform?.modules.find(
    (item) => item.id === taskLesson?.businessPlatformModuleId
  );
  return `${platform?.name ?? '业务平台'} · ${businessModule?.name ?? taskLesson?.moduleName ?? '业务模块'}`;
}

function taskStatusLabel(status: StudentTask['status']) {
  if (status === 'DOING') return '练习中';
  if (status === 'GRADED') return '已评分';
  return '待评阅';
}

function taskCoverBadge(task: StudentTask) {
  if (task.status !== 'GRADED') return taskStatusLabel(task.status);
  return `${(task.objectiveScore ?? 0) + (task.subjectiveScore ?? 0)} 分`;
}

function taskCardMetrics(task: StudentTask) {
  const objective = task.status === 'DOING' ? '—' : (task.objectiveScore ?? 0);
  const subjective = task.status === 'GRADED' ? (task.subjectiveScore ?? 0) : '—';
  const total =
    task.status === 'GRADED'
      ? (task.objectiveScore ?? 0) + (task.subjectiveScore ?? 0)
      : '—';
  return [
    { label: '系统客观分', value: objective, suffix: `/ ${objectiveMaxFor(task)}` },
    {
      label: '教师主观分',
      value: subjective,
      suffix: `/ ${subjectiveMaxFor(task)}`,
      tone: 'blue' as const
    },
    {
      label: '总分',
      value: total,
      suffix: `/ ${objectiveMaxFor(task) + subjectiveMaxFor(task)}`,
      tone: 'green' as const
    }
  ];
}

function taskSummary(task: StudentTask) {
  const taskLesson = lessonFor(task);
  const summarizedStages =
    task.mode === 'PRACTICE'
      ? taskLesson?.stages.filter((stage) => requiredPracticeStepCount(stage) > 0) ?? []
      : taskLesson?.stages ?? [];
  const stageCount = summarizedStages.length;
  const completedStageCount = summarizedStages.filter((stage) =>
    task.completedStageIds.includes(stage.id)
  ).length;
  const stepCount = summarizedStages.reduce(
    (sum, stage) =>
      sum +
      (task.mode === 'PRACTICE'
        ? requiredPracticeStepCount(stage)
        : stage.recordedSteps.length),
    0
  );
  const requiredStepIds = new Set(
    summarizedStages.flatMap((stage) =>
      stage.recordedSteps
        .filter(isRequiredPracticeStep)
        .map((step) => step.id)
    )
  );
  const hitCount =
    task.mode === 'PRACTICE'
      ? new Set(
          (task.practiceStepResults ?? [])
            .filter((result) => requiredStepIds.has(result.stepId))
            .map((result) => result.stepId)
        ).size
      : task.practiceStepResults?.length ?? 0;
  if (task.status === 'DOING') {
    return `正在练习，已完成 ${completedStageCount} / ${stageCount} 个教学点，命中 ${hitCount} 个必做操作点。`;
  }
  return `已完成 ${completedStageCount} / ${stageCount} 个教学点，命中 ${hitCount} / ${stepCount} 个必做操作点。`;
}

function requiredPracticeStepCount(stage: LessonStage) {
  return stage.recordedSteps.filter(isRequiredPracticeStep).length;
}

function taskOwnerMeta(task: StudentTask) {
  const role = roleNamesForTask(task) || '未分配业务角色';
  return `${role} · 第 ${task.attemptNumber} 次`;
}

function cardTheme(index: number) {
  return (['purple', 'green', 'blue'] as const)[index % 3];
}

function stepTitle(stepId: string) {
  return (
    lesson.value?.stages
      .flatMap((stage) => stage.recordedSteps)
      .find((step) => step.id === stepId)?.title ?? stepId
  );
}

function stageTitle(stageId: string) {
  return lesson.value?.stages.find((stage) => stage.id === stageId)?.name ?? stageId;
}

function formatTime(value: string) {
  const parsed = Date.parse(value);
  return Number.isFinite(parsed) ? new Date(parsed).toLocaleString('zh-CN') : value;
}

function openTask(taskId: string) {
  selectedTaskId.value = taskId;
  detailOpen.value = true;
}

function closeDetail() {
  closeEvidencePreview();
  detailOpen.value = false;
}

function openEvidencePreview(stepId: string, completedAt: string) {
  const index = evidenceGallery.value.findIndex(
    ({ result }) =>
      result.stepId === stepId && result.completedAt === completedAt
  );
  if (index < 0) return;
  evidencePreviewIndex.value = index;
  evidencePreviewOpen.value = true;
}

function closeEvidencePreview() {
  evidencePreviewOpen.value = false;
}

function selectEvidencePreview(index: number) {
  if (index < 0 || index >= evidenceGallery.value.length) return;
  evidencePreviewIndex.value = index;
}

function moveEvidencePreview(direction: -1 | 1) {
  const total = evidenceGallery.value.length;
  if (total < 2) return;
  evidencePreviewIndex.value =
    (evidencePreviewIndex.value + direction + total) % total;
}

function handleReviewKeydown(event: KeyboardEvent) {
  if (evidencePreviewOpen.value) {
    if (event.key === 'ArrowLeft') {
      event.preventDefault();
      moveEvidencePreview(-1);
    } else if (event.key === 'ArrowRight') {
      event.preventDefault();
      moveEvidencePreview(1);
    } else if (event.key === 'Escape') {
      event.preventDefault();
      closeEvidencePreview();
    }
    return;
  }
  if (event.key === 'Escape' && detailOpen.value) {
    closeDetail();
  }
}

function handleCourseChange(event: Event) {
  courseFilter.value = (event.target as HTMLSelectElement).value;
  detailOpen.value = false;
}

async function refreshPracticeStatus() {
  message.value = '';
  try {
    loading.value = true;
    await store.refreshAuthenticatedWorkspace();
  } catch (error) {
    message.value = error instanceof Error ? error.message : '练习情况刷新失败';
  } finally {
    loading.value = false;
  }
}

async function scoreTask() {
  if (!selectedTask.value) return;
  const taskId = selectedTask.value.id;
  message.value = '';
  try {
    loading.value = true;
    await store.gradeStudentTaskRemote(
      taskId,
      Number(subjectiveScore.value),
      comment.value.trim()
    );
    filter.value = 'GRADED';
    selectedTaskId.value = taskId;
    detailOpen.value = true;
    message.value = '评分已保存，客观分、主观分和教师批语已推送至学生端。';
  } catch (error) {
    message.value =
      error instanceof Error ? error.message : '评分保存失败，请检查输入。';
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  window.addEventListener('keydown', handleReviewKeydown);
  void refreshPracticeStatus();
});

onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleReviewKeydown);
});
</script>

<template>
  <section class="page teacher-review">
    <PageHeader
      eyebrow="教师评阅"
      title="主观评分与反馈"
      description="每份练习以成绩档案卡集中展示。打开档案即可核对教学点和操作证据，补充主观评分与批语。"
    >
      <button
        class="button secondary"
        type="button"
        :disabled="loading"
        @click="refreshPracticeStatus"
      >
        {{ loading ? '正在刷新…' : '刷新练习情况' }}
      </button>
      <RouterLink class="button secondary" to="/teacher/dashboard">
        返回教学工作台
      </RouterLink>
    </PageHeader>

    <section class="course-switcher">
      <div class="course-switcher-heading">
        <span>课程切换</span>
        <strong>{{ selectedCourse?.title ?? '全部课程' }}</strong>
        <small>
          {{ selectedCourse?.scope ?? `汇总查看 ${courseOptions.length} 门课程的评阅档案` }}
        </small>
      </div>
      <label>
        <span>选择课程</span>
        <select :value="courseFilter" @change="handleCourseChange">
          <option value="ALL">全部课程（{{ reviewEligibleTasks.length }} 份档案）</option>
          <option v-for="course in courseOptions" :key="course.id" :value="course.id">
            {{ course.title }}（{{ course.count }} 份）
          </option>
        </select>
      </label>
    </section>

    <div class="archive-overview">
      <div><span>练习中</span><strong>{{ doingCount }}</strong><small>份</small></div>
      <div><span>待评阅</span><strong>{{ pendingCount }}</strong><small>份</small></div>
      <div><span>已评分</span><strong>{{ gradedCount }}</strong><small>份</small></div>
      <p>系统客观评分与教师主观评阅共同组成最终成绩。</p>
    </div>

    <div class="archive-toolbar">
      <div class="archive-filters" aria-label="评阅状态筛选">
        <button
          v-for="item in reviewFilters"
          :key="item.value"
          type="button"
          :class="{ active: filter === item.value }"
          @click="filter = item.value"
        >
          {{ item.label }}
        </button>
      </div>
      <span>
        {{ selectedCourse?.title ?? '全部课程' }} · 当前共 {{ reviewTasks.length }} 份成绩档案
      </span>
    </div>

    <div v-if="reviewTasks.length" class="archive-grid">
      <ResultArchiveCard
        v-for="(task, index) in reviewTasks"
        :key="task.id"
        :title="lessonFor(task)?.title ?? task.title"
        :business-scope="businessScope(task)"
        :cover-badge="taskCoverBadge(task)"
        :avatar-text="task.studentName.slice(0, 1)"
        :owner-name="task.studentName"
        :owner-meta="taskOwnerMeta(task)"
        :status="task.status"
        :status-label="taskStatusLabel(task.status)"
        :metrics="taskCardMetrics(task)"
        :summary="taskSummary(task)"
        :theme="cardTheme(index)"
        :selected="detailOpen && selectedTaskId === task.id"
        @select="openTask(task.id)"
      >
        <template #actions>
          <button class="secondary" type="button" @click="openTask(task.id)">
            {{ task.status === 'DOING' ? '查看实时轨迹' : '查看操作证据' }}
          </button>
          <button
            class="primary"
            type="button"
            :disabled="task.status === 'DOING'"
            @click="openTask(task.id)"
          >
            {{
              task.status === 'DOING'
                ? '等待提交'
                : task.status === 'GRADED'
                  ? '修改评阅'
                  : '开始评阅'
            }}
          </button>
        </template>
      </ResultArchiveCard>
    </div>

    <article v-else class="card empty-state archive-empty">
      <div>
        <strong>当前分类下没有成绩档案</strong>
        <p>学生开始练习或提交任务后，对应档案会自动出现在这里。</p>
      </div>
    </article>

    <div
      v-if="detailOpen && selectedTask && lesson"
      class="detail-backdrop"
      role="dialog"
      aria-modal="true"
      aria-label="成绩档案详情"
      @click.self="closeDetail"
    >
      <section class="detail-panel">
        <header class="detail-heading">
          <div>
            <span>成绩档案</span>
            <h2>{{ selectedTask.studentName }} · {{ lesson.title }}</h2>
            <p>{{ businessScope(selectedTask) }} · {{ roleNames(selectedTask.groupKeys) }}</p>
          </div>
          <div>
            <StatusPill
              :status="selectedTask.status"
              :label="taskStatusLabel(selectedTask.status)"
            />
            <button class="detail-close" type="button" aria-label="关闭" @click="closeDetail">
              ×
            </button>
          </div>
        </header>

        <div class="detail-scroll">
          <div class="score-strip">
            <div>
              <span>系统客观分</span>
              <strong>{{ selectedTask.objectiveScore ?? 0 }}</strong>
              <small>/ {{ maxObjectiveScore }}</small>
            </div>
            <div class="subjective">
              <span>教师主观分</span>
              <strong>{{ selectedTask.subjectiveScore ?? '—' }}</strong>
              <small>/ {{ maxSubjectiveScore }}</small>
            </div>
            <div class="total">
              <span>当前总分</span>
              <strong>
                {{
                  selectedTask.status === 'GRADED'
                    ? (selectedTask.objectiveScore ?? 0) +
                      (selectedTask.subjectiveScore ?? 0)
                    : '—'
                }}
              </strong>
              <small>/ {{ maxObjectiveScore + maxSubjectiveScore }}</small>
            </div>
          </div>

          <section class="detail-section">
            <div class="section-heading">
              <div>
                <h3>教学点完成情况</h3>
                <p>系统根据学生实际操作记录自动判定。</p>
              </div>
              <span>{{ completedReviewStageCount }} / {{ reviewStages.length }}</span>
            </div>
            <div class="evidence-timeline">
              <div
                v-for="(stage, index) in reviewStages"
                :key="stage.id"
                :class="{ complete: selectedTask.completedStageIds.includes(stage.id) }"
              >
                <span class="evidence-index">
                  {{ selectedTask.completedStageIds.includes(stage.id) ? '✓' : index + 1 }}
                </span>
                <section>
                  <strong>{{ stage.name }}</strong>
                  <p>{{ stage.description || '按照录制路径完成流程性操作' }}</p>
                  <small>
                    {{ requiredPracticeStepCount(stage) }} 个必做操作点 · 客观分 {{ stage.score }}
                  </small>
                </section>
                <StatusPill
                  :status="selectedTask.completedStageIds.includes(stage.id) ? 'COMPLETED' : 'TODO'"
                  :label="selectedTask.completedStageIds.includes(stage.id) ? '轨迹完整' : '未完成'"
                />
              </div>
            </div>
          </section>

          <section v-if="selectedTask.mode === 'PRACTICE'" class="detail-section practice-detail">
            <div class="section-heading">
              <div>
                <h3>学生练习操作记录</h3>
                <p>按原业务系统实际操作时间排列，仅教师端可见。</p>
              </div>
              <span>{{ selectedPracticeSteps.length }} 个已命中操作点</span>
            </div>
            <div v-if="selectedPracticeSteps.length" class="practice-step-list">
              <article v-for="step in selectedPracticeSteps" :key="step.clientTraceId">
                <button
                  v-if="step.evidenceScreenshot?.dataUrl"
                  class="practice-evidence-open"
                  type="button"
                  title="打开连续操作证据预览"
                  :aria-label="`预览${stepTitle(step.stepId)}操作截屏`"
                  @click="openEvidencePreview(step.stepId, step.completedAt)"
                >
                  <img
                    :src="step.evidenceScreenshot.dataUrl"
                    :alt="`${stepTitle(step.stepId)}操作截屏`"
                    loading="lazy"
                  />
                  <span>预览</span>
                </button>
                <div v-else-if="step.pageSnapshot" class="practice-evidence-placeholder">
                  <span>页面快照</span>
                </div>
                <div v-else class="practice-evidence-placeholder empty">
                  <span>无截屏</span>
                </div>
                <div class="practice-step-meta">
                  <strong>{{ stepTitle(step.stepId) }}</strong>
                  <p>{{ stageTitle(step.stageId) }} · {{ formatTime(step.completedAt) }}</p>
                </div>
                <StatusPill status="COMPLETED" label="已完成" />
              </article>
            </div>
            <div v-else class="empty-list">尚未采集到学生练习操作点</div>
          </section>

          <section
            v-if="Object.keys(selectedTask.submissionValues).length"
            class="detail-section submission-evidence"
          >
            <div class="section-heading"><div><h3>学生提交信息</h3></div></div>
            <dl>
              <div
                v-for="field in store.state.examSettings[selectedTask.lessonId]
                  ?.submissionFields ?? []"
                :key="field.key"
              >
                <dt>{{ field.label }}</dt>
                <dd>{{ selectedTask.submissionValues[field.key] || '未填写' }}</dd>
              </div>
            </dl>
          </section>

          <div class="evidence-note">
            <strong>评分证据说明</strong>
            <p>
              客观成绩依据登录会话、页面访问及录制操作点的实际命中情况生成；教师可结合过程轨迹评价业务质量并给出主观分。
            </p>
          </div>

          <section v-if="selectedTask.status !== 'DOING'" class="grading-card">
            <div class="section-heading">
              <div>
                <h3>教师评分</h3>
                <p>保存后，主观分和批语会同步展示在学生成绩档案中。</p>
              </div>
            </div>
            <div class="grading-form">
              <label>
                主观评分（0 — {{ maxSubjectiveScore }}）
                <input
                  v-model.number="subjectiveScore"
                  type="number"
                  min="0"
                  :max="maxSubjectiveScore"
                />
              </label>
              <label>
                教师批语
                <textarea
                  v-model="comment"
                  rows="5"
                  placeholder="例如：业务流程完整，提交说明准确；建议进一步关注审批意见的规范性。"
                ></textarea>
              </label>
              <p v-if="message" class="notice" :class="{ success: selectedTask.status === 'GRADED' }">
                {{ message }}
              </p>
              <div class="button-row">
                <button class="secondary" type="button" @click="closeDetail">取消</button>
                <button
                  class="primary"
                  type="button"
                  :disabled="
                    loading ||
                    subjectiveScore < 0 ||
                    subjectiveScore > maxSubjectiveScore ||
                    !comment.trim()
                  "
                  @click="scoreTask"
                >
                  {{ loading ? '正在保存…' : '保存评分并推送结果' }}
                </button>
              </div>
            </div>
          </section>

          <section v-else class="grading-card waiting-card">
            <strong>学生正在练习</strong>
            <p>可以查看最新命中操作点；学生提交并完成客观判定后，才可填写主观评分。</p>
          </section>
        </div>
      </section>
    </div>

    <div
      v-if="evidencePreviewOpen && currentEvidence"
      class="evidence-preview-backdrop"
      role="dialog"
      aria-modal="true"
      aria-label="学生操作截屏连续预览"
      @click.self="closeEvidencePreview"
    >
      <section class="evidence-preview-panel">
        <header class="evidence-preview-heading">
          <div>
            <span>学生操作证据 · 连续预览</span>
            <h2>{{ currentEvidence.stepName }}</h2>
            <p>
              {{ currentEvidence.stageName }} ·
              {{ formatTime(currentEvidence.result.completedAt) }}
            </p>
          </div>
          <div>
            <strong>
              {{ evidencePreviewIndex + 1 }} / {{ evidenceGallery.length }}
            </strong>
            <button
              class="evidence-preview-close"
              type="button"
              aria-label="关闭截图预览"
              @click="closeEvidencePreview"
            >
              ×
            </button>
          </div>
        </header>

        <div class="evidence-preview-stage">
          <button
            class="evidence-preview-arrow previous"
            type="button"
            aria-label="查看上一张操作截屏"
            :disabled="evidenceGallery.length < 2"
            @click="moveEvidencePreview(-1)"
          >
            ‹
          </button>
          <figure>
            <img
              :src="currentEvidence.screenshot.dataUrl"
              :alt="`${currentEvidence.stepName}操作截屏`"
            />
            <figcaption>
              <span>
                {{ currentEvidence.screenshot.width }} ×
                {{ currentEvidence.screenshot.height }}
              </span>
              <span>学生操作证据 · 第 {{ evidencePreviewIndex + 1 }} 张</span>
            </figcaption>
          </figure>
          <button
            class="evidence-preview-arrow next"
            type="button"
            aria-label="查看下一张操作截屏"
            :disabled="evidenceGallery.length < 2"
            @click="moveEvidencePreview(1)"
          >
            ›
          </button>
        </div>

        <nav class="evidence-preview-strip" aria-label="全部操作截屏">
          <button
            v-for="(item, index) in evidenceGallery"
            :key="`${item.result.clientTraceId}-${item.result.completedAt}`"
            type="button"
            :class="{ active: evidencePreviewIndex === index }"
            :aria-current="evidencePreviewIndex === index ? 'true' : undefined"
            @click="selectEvidencePreview(index)"
          >
            <img :src="item.screenshot.dataUrl" :alt="`${item.stepName}缩略图`" />
            <span>{{ index + 1 }}. {{ item.stepName }}</span>
            <small>{{ item.stageName }}</small>
          </button>
        </nav>

        <footer class="evidence-preview-footer">
          <span>可使用键盘 ← → 连续切换，Esc 退出预览</span>
          <button class="secondary" type="button" @click="closeEvidencePreview">
            返回评阅
          </button>
        </footer>
      </section>
    </div>
  </section>
</template>

<style scoped>
.course-switcher {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 16px;
  border: 1px solid #dedaf7;
  border-radius: 16px;
  padding: 17px 20px;
  background:
    linear-gradient(90deg, rgb(244 242 255 / 94%), rgb(255 255 255 / 96%)),
    #fff;
}

.course-switcher-heading {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.course-switcher-heading > span {
  color: #6654cf;
  font-size: 11px;
  font-weight: 800;
}

.course-switcher-heading strong,
.course-switcher-heading small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-switcher-heading strong {
  color: #354157;
  font-size: 17px;
}

.course-switcher-heading small {
  color: #818b9c;
  font-size: 12px;
}

.course-switcher label {
  display: grid;
  width: min(390px, 45%);
  flex: 0 0 auto;
  gap: 6px;
}

.course-switcher label > span {
  color: #6f798b;
  font-size: 11px;
  font-weight: 700;
}

.course-switcher select {
  min-height: 42px;
  border-color: #d6d1f1;
  border-radius: 11px;
  background-color: #fff;
  font-weight: 700;
}

.archive-overview {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 18px;
  overflow: hidden;
  border: 1px solid #e5e8ef;
  border-radius: 14px;
  background: #fff;
}

.archive-overview > div {
  display: flex;
  align-items: baseline;
  gap: 7px;
  border-right: 1px solid #edf0f4;
  padding: 16px 20px;
}

.archive-overview span,
.archive-overview small,
.archive-overview p {
  color: #8791a2;
  font-size: 12px;
}

.archive-overview strong {
  color: #5847c8;
  font-size: 22px;
}

.archive-overview p {
  margin: 0 18px 0 auto;
}

.archive-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.archive-toolbar > span {
  color: #7f899a;
  font-size: 13px;
}

.archive-filters {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
}

.archive-filters button {
  min-height: 38px;
  border-color: #dfe3eb;
  border-radius: 999px;
  padding: 0 17px;
  color: #697589;
  background: #fff;
}

.archive-filters button.active {
  border-color: #6654d1;
  color: #fff;
  background: #6654d1;
}

.archive-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.archive-empty {
  min-height: 360px;
}

.detail-backdrop {
  position: fixed;
  z-index: 1200;
  inset: 0;
  display: grid;
  place-items: center;
  overflow: hidden;
  padding: 24px;
  background: rgb(20 25 42 / 56%);
  backdrop-filter: blur(4px);
}

.detail-panel {
  display: grid;
  min-height: 0;
  width: min(1080px, 100%);
  height: min(900px, calc(100vh - 48px));
  height: min(900px, calc(100dvh - 48px));
  max-height: calc(100vh - 48px);
  max-height: calc(100dvh - 48px);
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
  border-radius: 18px;
  background: #f5f6f9;
  box-shadow: 0 28px 80px rgb(12 16 34 / 28%);
}

.detail-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border-bottom: 1px solid #e1e5ed;
  padding: 18px 22px;
  background: #fff;
}

.detail-heading > div:first-child {
  min-width: 0;
}

.detail-heading > div:last-child {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-heading span {
  color: #6553cf;
  font-size: 12px;
  font-weight: 800;
}

.detail-heading h2,
.detail-heading p {
  overflow: hidden;
  margin: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-heading h2 {
  margin-top: 5px;
  color: #303c51;
  font-size: 20px;
}

.detail-heading p {
  margin-top: 5px;
  color: #8993a4;
  font-size: 12px;
}

.detail-close {
  width: 38px;
  min-height: 38px;
  border-color: #e1e5ec;
  border-radius: 50%;
  padding: 0;
  color: #687386;
  font-size: 24px;
  font-weight: 400;
}

.detail-scroll {
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  padding: 18px;
}

.score-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  margin-bottom: 16px;
  border: 1px solid #e3e6ed;
  border-radius: 14px;
  background: #fff;
}

.score-strip > div {
  display: flex;
  align-items: baseline;
  gap: 8px;
  border-right: 1px solid #e8ebf0;
  padding: 20px;
}

.score-strip > div:last-child {
  border-right: 0;
}

.score-strip span,
.score-strip small {
  color: #8993a4;
  font-size: 12px;
}

.score-strip strong {
  color: #5948c8;
  font-size: 28px;
}

.score-strip .subjective strong {
  color: #2479b7;
}

.score-strip .total strong {
  color: #07815d;
}

.detail-section,
.grading-card,
.evidence-note {
  margin-bottom: 16px;
  border: 1px solid #e3e6ed;
  border-radius: 14px;
  background: #fff;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  border-bottom: 1px solid #eceff4;
  padding: 15px 18px;
}

.section-heading h3,
.section-heading p {
  margin: 0;
}

.section-heading h3 {
  color: #3d495e;
  font-size: 15px;
}

.section-heading p {
  margin-top: 4px;
  color: #8993a4;
  font-size: 11px;
}

.section-heading > span {
  border-radius: 999px;
  padding: 6px 10px;
  color: #5a49c8;
  background: #efedff;
  font-size: 11px;
  font-weight: 800;
}

.evidence-timeline {
  display: grid;
  padding: 4px 18px;
}

.evidence-timeline > div {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) minmax(72px, auto);
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #eef0f5;
  padding: 14px 0;
}

.evidence-timeline > div:last-child {
  border-bottom: 0;
}

.evidence-timeline > div > .evidence-index {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border: 1px solid #dfe3eb;
  border-radius: 10px;
  color: #919aaa;
  background: #f8f9fb;
  font-size: 12px;
  font-weight: 900;
}

.evidence-timeline > div.complete > .evidence-index {
  border-color: #c2e9da;
  color: #087b59;
  background: #e9f8f2;
}

.evidence-timeline section {
  display: grid;
  gap: 4px;
}

.evidence-timeline p,
.evidence-timeline small {
  margin: 0;
  color: #8791a3;
  font-size: 11px;
}

.practice-step-list {
  display: grid;
  padding: 4px 18px;
}

.practice-step-list article {
  display: grid;
  min-height: 90px;
  grid-template-columns: 104px minmax(0, 1fr) minmax(58px, auto);
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #eceff4;
  padding: 13px 0;
}

.practice-evidence-open {
  position: relative;
  display: block;
  width: 104px;
  height: 68px;
  min-height: 68px;
  overflow: hidden;
  border: 1px solid #dfe4ec;
  border-radius: 10px;
  padding: 0;
  background: #edf1f6;
  cursor: zoom-in;
}

.practice-evidence-open::after {
  position: absolute;
  inset: 0;
  border-radius: inherit;
  box-shadow: inset 0 0 0 0 rgb(99 82 208 / 0%);
  content: '';
  pointer-events: none;
  transition: box-shadow 160ms ease;
}

.practice-evidence-open:hover::after,
.practice-evidence-open:focus-visible::after {
  box-shadow: inset 0 0 0 3px rgb(99 82 208 / 68%);
}

.practice-evidence-open img {
  display: block;
  width: 100%;
  height: 100%;
  object-fit: cover;
  background: #edf1f6;
}

.practice-evidence-open > span {
  position: absolute;
  right: 5px;
  bottom: 5px;
  border-radius: 999px;
  padding: 3px 7px;
  color: #fff;
  background: rgb(27 34 51 / 78%);
  box-shadow: 0 8px 24px rgb(16 22 38 / 22%);
  backdrop-filter: blur(8px);
  font-size: 9px;
  font-weight: 800;
  pointer-events: none;
}

.practice-evidence-placeholder {
  display: grid;
  width: 104px;
  height: 68px;
  place-items: center;
  border: 1px solid #dfe4ec;
  border-radius: 10px;
  color: #8992a2;
  background: #f5f7fa;
  font-size: 10px;
  font-weight: 800;
}

.practice-evidence-placeholder.empty {
  border-style: dashed;
  color: #a0a8b6;
  background: #fafbfc;
}

.evidence-timeline > div :deep(.status-pill) {
  min-width: 68px;
  justify-content: center;
  justify-self: end;
  padding: 6px 10px;
  line-height: 1.2;
  text-align: center;
  white-space: nowrap;
}

.practice-step-list article:last-child {
  border-bottom: 0;
}

.practice-step-meta {
  display: grid;
  min-width: 0;
  gap: 6px;
}

.practice-step-meta strong {
  overflow: hidden;
  color: #30384a;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.practice-step-meta p {
  overflow: hidden;
  margin: 0;
  color: #8791a3;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.practice-step-list article :deep(.status-pill) {
  min-width: 56px;
  justify-content: center;
  justify-self: end;
  white-space: nowrap;
}

.submission-evidence dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
  padding: 16px 18px;
}

.submission-evidence dl > div {
  display: grid;
  gap: 4px;
  border-radius: 9px;
  padding: 11px;
  background: #f8f9fb;
}

.submission-evidence dt,
.submission-evidence dd {
  margin: 0;
  font-size: 12px;
}

.submission-evidence dt {
  color: #8b95a5;
}

.submission-evidence dd {
  color: #3f4b60;
  font-weight: 800;
}

.evidence-note {
  padding: 14px 16px;
  border-color: #ddd8ff;
  color: #706795;
  background: #f8f7ff;
}

.evidence-note strong {
  color: #5847c8;
  font-size: 13px;
}

.evidence-note p {
  margin: 5px 0 0;
  font-size: 12px;
  line-height: 1.7;
}

.grading-form {
  display: grid;
  grid-template-columns: minmax(190px, 0.3fr) minmax(0, 1fr);
  gap: 16px;
  padding: 18px;
}

.grading-form label {
  display: grid;
  gap: 7px;
  color: #576276;
  font-size: 13px;
  font-weight: 700;
}

.grading-form .notice,
.grading-form .button-row {
  grid-column: 1 / -1;
}

.grading-form .button-row {
  justify-content: flex-end;
}

.waiting-card {
  padding: 18px;
}

.waiting-card p {
  margin: 6px 0 0;
  color: #818b9c;
  font-size: 12px;
}

.empty-list {
  padding: 30px 18px;
  color: #8c96a6;
  font-size: 12px;
  text-align: center;
}

.evidence-preview-backdrop {
  position: fixed;
  z-index: 1500;
  inset: 0;
  display: grid;
  min-height: 0;
  place-items: center;
  overflow: hidden;
  padding: 18px;
  background: rgb(11 15 27 / 88%);
  backdrop-filter: blur(10px);
}

.evidence-preview-panel {
  display: grid;
  min-width: 0;
  min-height: 0;
  width: min(1440px, 100%);
  height: min(940px, calc(100vh - 36px));
  height: min(940px, calc(100dvh - 36px));
  grid-template-rows: auto minmax(0, 1fr) auto auto;
  overflow: hidden;
  border: 1px solid rgb(255 255 255 / 16%);
  border-radius: 18px;
  color: #eef2fb;
  background: #151b28;
  box-shadow: 0 34px 100px rgb(0 0 0 / 48%);
}

.evidence-preview-heading,
.evidence-preview-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 15px 18px;
  background: #1b2231;
}

.evidence-preview-heading {
  border-bottom: 1px solid rgb(255 255 255 / 9%);
}

.evidence-preview-heading > div:first-child {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.evidence-preview-heading > div:last-child {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 13px;
}

.evidence-preview-heading span {
  color: #aaa0ff;
  font-size: 11px;
  font-weight: 900;
}

.evidence-preview-heading h2,
.evidence-preview-heading p {
  overflow: hidden;
  margin: 0;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.evidence-preview-heading h2 {
  font-size: 18px;
}

.evidence-preview-heading p,
.evidence-preview-footer span {
  color: #aab3c5;
  font-size: 11px;
}

.evidence-preview-heading strong {
  min-width: 64px;
  color: #fff;
  font-size: 14px;
  text-align: center;
}

.evidence-preview-close {
  width: 38px;
  min-height: 38px;
  border-color: rgb(255 255 255 / 16%);
  border-radius: 50%;
  padding: 0;
  color: #fff;
  background: rgb(255 255 255 / 8%);
  font-size: 25px;
  font-weight: 400;
}

.evidence-preview-stage {
  position: relative;
  display: grid;
  min-height: 0;
  grid-template-columns: 58px minmax(0, 1fr) 58px;
  align-items: stretch;
  gap: 10px;
  padding: 14px;
  background:
    radial-gradient(circle at 50% 42%, rgb(103 86 220 / 13%), transparent 42%),
    #0e131e;
}

.evidence-preview-stage figure {
  display: grid;
  min-width: 0;
  min-height: 0;
  grid-template-rows: minmax(0, 1fr) auto;
  margin: 0;
  overflow: hidden;
  border: 1px solid rgb(255 255 255 / 10%);
  border-radius: 12px;
  background: #0a0e16;
}

.evidence-preview-stage figure > img {
  display: block;
  min-width: 0;
  min-height: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.evidence-preview-stage figcaption {
  display: flex;
  min-width: 0;
  justify-content: space-between;
  gap: 18px;
  border-top: 1px solid rgb(255 255 255 / 8%);
  padding: 9px 12px;
  color: #aeb7c8;
  background: #151b27;
  font-size: 10px;
}

.evidence-preview-stage figcaption span:last-child {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.evidence-preview-arrow {
  align-self: center;
  width: 48px;
  min-height: 70px;
  border-color: rgb(255 255 255 / 12%);
  border-radius: 14px;
  padding: 0;
  color: #fff;
  background: rgb(255 255 255 / 7%);
  font-size: 40px;
  font-weight: 300;
}

.evidence-preview-arrow:hover:not(:disabled) {
  border-color: #8f81fa;
  background: rgb(111 92 227 / 28%);
}

.evidence-preview-arrow:disabled {
  cursor: default;
  opacity: 0.28;
}

.evidence-preview-strip {
  display: flex;
  gap: 9px;
  min-width: 0;
  overflow-x: auto;
  overscroll-behavior-inline: contain;
  border-top: 1px solid rgb(255 255 255 / 9%);
  padding: 10px 14px;
  background: #151b28;
  scrollbar-color: #535d70 transparent;
}

.evidence-preview-strip button {
  display: grid;
  flex: 0 0 168px;
  min-width: 0;
  grid-template-columns: 56px minmax(0, 1fr);
  grid-template-rows: 1fr 1fr;
  gap: 2px 8px;
  align-items: center;
  border-color: rgb(255 255 255 / 10%);
  border-radius: 10px;
  padding: 6px;
  color: #dce2ef;
  background: #202838;
  text-align: left;
}

.evidence-preview-strip button.active {
  border-color: #9a8dff;
  background: #302b51;
  box-shadow: 0 0 0 2px rgb(143 129 250 / 20%);
}

.evidence-preview-strip img {
  width: 56px;
  height: 42px;
  grid-row: 1 / 3;
  object-fit: cover;
  border-radius: 6px;
  background: #0d111a;
}

.evidence-preview-strip span,
.evidence-preview-strip small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.evidence-preview-strip span {
  align-self: end;
  font-size: 10px;
  font-weight: 800;
}

.evidence-preview-strip small {
  align-self: start;
  color: #9da7b9;
  font-size: 9px;
}

.evidence-preview-footer {
  border-top: 1px solid rgb(255 255 255 / 9%);
  padding-block: 10px;
}

.evidence-preview-footer button {
  border-color: rgb(255 255 255 / 15%);
  color: #fff;
  background: rgb(255 255 255 / 8%);
}

@media (max-width: 1250px) {
  .archive-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 800px) {
  .course-switcher,
  .archive-overview,
  .archive-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .course-switcher label {
    width: 100%;
  }

  .archive-overview > div {
    border-right: 0;
    border-bottom: 1px solid #edf0f4;
  }

  .archive-overview p {
    margin: 0;
    padding: 14px 18px;
  }

  .archive-grid,
  .score-strip,
  .grading-form,
  .submission-evidence dl {
    grid-template-columns: 1fr;
  }

  .score-strip > div {
    border-right: 0;
    border-bottom: 1px solid #e8ebf0;
  }

  .detail-backdrop {
    padding: 0;
  }

  .detail-panel {
    width: 100%;
    height: 100vh;
    height: 100dvh;
    max-height: 100vh;
    max-height: 100dvh;
    border-radius: 0;
  }

  .detail-heading {
    padding: 14px 16px;
  }

  .evidence-preview-backdrop {
    padding: 0;
  }

  .evidence-preview-panel {
    width: 100%;
    height: 100vh;
    height: 100dvh;
    border-radius: 0;
  }

  .evidence-preview-stage {
    grid-template-columns: 44px minmax(0, 1fr) 44px;
    gap: 5px;
    padding: 8px;
  }

  .evidence-preview-arrow {
    width: 38px;
  }
}

@media (max-width: 540px) {
  .archive-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .detail-heading > div:last-child > :first-child {
    display: none;
  }

  .evidence-timeline > div {
    grid-template-columns: 34px minmax(0, 1fr);
  }

  .evidence-timeline > div > :last-child {
    grid-column: 2;
    justify-self: start;
  }

  .practice-step-list {
    padding-inline: 12px;
  }

  .practice-step-list article {
    grid-template-columns: 84px minmax(0, 1fr);
  }

  .practice-evidence-open,
  .practice-evidence-placeholder {
    width: 84px;
    height: 58px;
    min-height: 58px;
  }

  .practice-step-list article :deep(.status-pill) {
    grid-column: 2;
    justify-self: start;
  }

  .detail-scroll {
    padding: 10px;
  }

  .evidence-preview-heading,
  .evidence-preview-footer {
    padding-inline: 12px;
  }

  .evidence-preview-footer > span,
  .evidence-preview-stage figcaption span:last-child {
    display: none;
  }

  .evidence-preview-stage {
    grid-template-columns: 36px minmax(0, 1fr) 36px;
  }

  .evidence-preview-arrow {
    width: 32px;
    min-height: 56px;
    font-size: 30px;
  }

  .practice-evidence-open > span {
    right: 5px;
    bottom: 5px;
    font-size: 9px;
  }
}
</style>
