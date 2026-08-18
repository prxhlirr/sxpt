<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import ResultArchiveCard from '../../components/evaluation/ResultArchiveCard.vue';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusPill from '../../components/ui/StatusPill.vue';
import type { StudentTask } from '../../domain/models';
import { useTrainingStore } from '../../stores/trainingStore';

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
  const stageCount = taskLesson?.stages.length ?? 0;
  const stepCount = taskLesson?.stages.reduce(
    (sum, stage) => sum + stage.recordedSteps.length,
    0
  ) ?? 0;
  const hitCount = task.practiceStepResults?.length ?? 0;
  if (task.status === 'DOING') {
    return `正在练习，已完成 ${task.completedStageIds.length} / ${stageCount} 个教学点，命中 ${hitCount} 个操作点。`;
  }
  return `已完成 ${task.completedStageIds.length} / ${stageCount} 个教学点，命中 ${hitCount} / ${stepCount} 个录制操作点。`;
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
  detailOpen.value = false;
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

onMounted(refreshPracticeStatus);
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
              <span>{{ selectedTask.completedStageIds.length }} / {{ lesson.stages.length }}</span>
            </div>
            <div class="evidence-timeline">
              <div
                v-for="(stage, index) in lesson.stages"
                :key="stage.id"
                :class="{ complete: selectedTask.completedStageIds.includes(stage.id) }"
              >
                <span>
                  {{ selectedTask.completedStageIds.includes(stage.id) ? '✓' : index + 1 }}
                </span>
                <section>
                  <strong>{{ stage.name }}</strong>
                  <p>{{ stage.description || '按照录制路径完成流程性操作' }}</p>
                  <small>
                    {{ stage.recordedSteps.length }} 个录制步骤 · 客观分 {{ stage.score }}
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
                <span>{{ step.recordedActionType?.toUpperCase() || '操作' }}</span>
                <div>
                  <strong>{{ stepTitle(step.stepId) }}</strong>
                  <p>{{ stageTitle(step.stageId) }} · {{ formatTime(step.completedAt) }}</p>
                  <small>
                    实际操作 {{ step.observedActionType }} · 页面 {{ step.observedUrl || '未记录' }}
                  </small>
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
  padding: 24px;
  background: rgb(20 25 42 / 56%);
  backdrop-filter: blur(4px);
}

.detail-panel {
  display: grid;
  width: min(1080px, 100%);
  max-height: calc(100vh - 48px);
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
  overflow-y: auto;
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
  grid-template-columns: 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #eef0f5;
  padding: 14px 0;
}

.evidence-timeline > div:last-child {
  border-bottom: 0;
}

.evidence-timeline > div > span {
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

.evidence-timeline > div.complete > span {
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
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #eceff4;
  padding: 13px 0;
}

.practice-step-list article:last-child {
  border-bottom: 0;
}

.practice-step-list article > span {
  border-radius: 7px;
  padding: 5px 8px;
  color: #5a4ac9;
  background: #efedff;
  font-size: 10px;
  font-weight: 900;
}

.practice-step-list article > div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.practice-step-list p,
.practice-step-list small {
  overflow: hidden;
  margin: 0;
  color: #8791a3;
  font-size: 11px;
  text-overflow: ellipsis;
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
    max-height: 100vh;
    border-radius: 0;
  }

  .detail-heading {
    padding: 14px 16px;
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
}
</style>
