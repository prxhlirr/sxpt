<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import ResultArchiveCard from '../../components/evaluation/ResultArchiveCard.vue';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusPill from '../../components/ui/StatusPill.vue';
import type { StudentTask } from '../../domain/models';
import { authApi } from '../../services/trainingApi';
import { useTrainingStore } from '../../stores/trainingStore';

type ResultFilter = 'ALL' | 'GRADED' | 'PENDING';

const store = useTrainingStore();
const route = useRoute();
const router = useRouter();
const refreshMessage = ref('');
const filter = ref<ResultFilter>('ALL');
const courseFilter = ref(
  typeof route.query.lessonId === 'string' ? route.query.lessonId : 'ALL'
);
const selectedTaskId = ref('');
const detailOpen = ref(false);
const resultFilters: Array<{ value: ResultFilter; label: string }> = [
  { value: 'ALL', label: '全部成绩' },
  { value: 'GRADED', label: '已评分' },
  { value: 'PENDING', label: '待评分' }
];
const currentStudentId =
  authApi.getSession()?.user.userId ??
  (typeof window === 'undefined'
    ? store.state.studentTasks.find(
        (task) => task.status === 'SUBMITTED' || task.status === 'GRADED'
      )?.studentId ??
      store.state.studentTasks[0]?.studentId ??
      ''
    : '');

const resultTasks = computed(() =>
  store.state.studentTasks
    .filter(
      (task) =>
        task.studentId === currentStudentId &&
        (task.status === 'SUBMITTED' || task.status === 'GRADED')
    )
    .sort(
      (left, right) =>
        resultTimestamp(right) - resultTimestamp(left)
    )
);
const courseOptions = computed(() => {
  const options = new Map<string, { id: string; title: string; scope: string; count: number }>();
  resultTasks.value.forEach((task) => {
    const current = options.get(task.lessonId);
    if (current) {
      current.count += 1;
      return;
    }
    options.set(task.lessonId, {
      id: task.lessonId,
      title: lessonFor(task.lessonId)?.title ?? task.title,
      scope: businessScope(task),
      count: 1
    });
  });
  return [...options.values()];
});
const courseScopedResultTasks = computed(() =>
  resultTasks.value.filter(
    (task) => courseFilter.value === 'ALL' || task.lessonId === courseFilter.value
  )
);
const selectedCourse = computed(() =>
  courseOptions.value.find((course) => course.id === courseFilter.value)
);
const visibleResultTasks = computed(() =>
  courseScopedResultTasks.value.filter((task) => {
    if (filter.value === 'GRADED') return task.status === 'GRADED';
    if (filter.value === 'PENDING') return task.status === 'SUBMITTED';
    return true;
  })
);
const selectedTask = computed(() =>
  resultTasks.value.find((task) => task.id === selectedTaskId.value)
);
const gradedTasks = computed(() =>
  courseScopedResultTasks.value.filter((task) => task.status === 'GRADED')
);
const pendingTasks = computed(() =>
  courseScopedResultTasks.value.filter((task) => task.status === 'SUBMITTED')
);
const averageScore = computed(() => {
  if (!gradedTasks.value.length) return 0;
  const total = gradedTasks.value.reduce(
    (sum, task) =>
      sum + (task.objectiveScore ?? 0) + (task.subjectiveScore ?? 0),
    0
  );
  return Math.round(total / gradedTasks.value.length);
});

watch(
  courseOptions,
  (courses) => {
    if (
      resultTasks.value.length &&
      courseFilter.value !== 'ALL' &&
      !courses.some((course) => course.id === courseFilter.value)
    ) {
      courseFilter.value = 'ALL';
      void router.replace({ query: { ...route.query, lessonId: undefined } });
    }
  },
  { immediate: true }
);

watch(
  visibleResultTasks,
  (tasks) => {
    if (!tasks.some((task) => task.id === selectedTaskId.value)) {
      selectedTaskId.value = tasks[0]?.id ?? '';
      if (!selectedTaskId.value) detailOpen.value = false;
    }
  },
  { immediate: true }
);

function resultTimestamp(task: StudentTask) {
  const value = task.gradedAt || task.submittedAt || task.startedAt || '';
  const parsed = Date.parse(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function lessonFor(lessonId: string) {
  return store.state.lessons.find((lesson) => lesson.id === lessonId);
}

function roleNames(lessonId: string, keys: string[]) {
  const roles = store.state.groupPlans[lessonId]?.roles ?? [];
  return keys
    .map((key) => roles.find((role) => role.key === key)?.name ?? key)
    .join('、');
}

function objectiveMax(lessonId: string) {
  return (
    store.state.examSettings[lessonId]?.objectiveWeight ??
    lessonFor(lessonId)?.objectiveMaxScore ??
    0
  );
}

function subjectiveMax(lessonId: string) {
  return (
    store.state.examSettings[lessonId]?.subjectiveWeight ??
    lessonFor(lessonId)?.subjectiveMaxScore ??
    0
  );
}

function businessScope(task: StudentTask) {
  const lesson = lessonFor(task.lessonId);
  const platform = store.state.businessPlatforms.find(
    (item) => item.id === lesson?.businessPlatformId
  );
  const businessModule = platform?.modules.find(
    (item) => item.id === lesson?.businessPlatformModuleId
  );
  return `${platform?.name ?? '业务平台'} · ${businessModule?.name ?? lesson?.moduleName ?? '业务模块'}`;
}

function modeLabel(mode: StudentTask['mode']) {
  if (mode === 'LEARNING') return '学习';
  if (mode === 'PRACTICE') return '练习';
  return '考试';
}

function taskStatusLabel(task: StudentTask) {
  return task.status === 'GRADED' ? '已评分' : '待评分';
}

function taskCoverBadge(task: StudentTask) {
  return task.status === 'GRADED'
    ? `${(task.objectiveScore ?? 0) + (task.subjectiveScore ?? 0)} 分`
    : '待评分';
}

function taskCardMetrics(task: StudentTask) {
  return [
    {
      label: '系统客观分',
      value: task.objectiveScore ?? 0,
      suffix: `/ ${objectiveMax(task.lessonId)}`
    },
    {
      label: '教师主观分',
      value: task.status === 'GRADED' ? (task.subjectiveScore ?? 0) : '—',
      suffix: `/ ${subjectiveMax(task.lessonId)}`,
      tone: 'blue' as const
    },
    {
      label: '总分',
      value:
        task.status === 'GRADED'
          ? (task.objectiveScore ?? 0) + (task.subjectiveScore ?? 0)
          : '—',
      suffix: `/ ${objectiveMax(task.lessonId) + subjectiveMax(task.lessonId)}`,
      tone: 'green' as const
    }
  ];
}

function taskSummary(task: StudentTask) {
  if (task.comment) return task.comment;
  const lesson = lessonFor(task.lessonId);
  return `已完成 ${task.completedStageIds.length} / ${lesson?.stages.length ?? 0} 个教学点，教师完成主观评分后会在这里显示总分和批语。`;
}

function taskOwnerMeta(task: StudentTask) {
  const time = task.gradedAt || task.submittedAt;
  return `${formatDate(time)} · ${roleNames(task.lessonId, task.groupKeys) || '未分配业务角色'}`;
}

function formatDate(value?: string) {
  if (!value) return '时间未记录';
  const parsed = Date.parse(value);
  if (!Number.isFinite(parsed)) return value;
  return new Date(parsed).toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });
}

function cardTheme(index: number) {
  return (['purple', 'green', 'blue'] as const)[index % 3];
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
  void router.replace({
    query: {
      ...route.query,
      lessonId: courseFilter.value === 'ALL' ? undefined : courseFilter.value
    }
  });
}

onMounted(async () => {
  try {
    await store.refreshAuthenticatedWorkspace();
  } catch (error) {
    refreshMessage.value =
      error instanceof Error ? error.message : '成绩反馈刷新失败';
  }
});
</script>

<template>
  <section class="page student-results">
    <PageHeader
      eyebrow="学生成绩"
      title="成绩与教师反馈"
      description="每次实训形成一份成绩档案，客观 + 主观双轨评分共同构成最终结果。"
    >
      <RouterLink class="button secondary" to="/student/tasks">
        返回我的任务
      </RouterLink>
    </PageHeader>

    <p v-if="refreshMessage" class="notice">{{ refreshMessage }}</p>

    <section class="course-switcher">
      <div class="course-switcher-heading">
        <span>课程切换</span>
        <strong>{{ selectedCourse?.title ?? '全部课程' }}</strong>
        <small>
          {{ selectedCourse?.scope ?? `汇总查看 ${courseOptions.length} 门课程的成绩档案` }}
        </small>
      </div>
      <label>
        <span>选择课程</span>
        <select :value="courseFilter" @change="handleCourseChange">
          <option value="ALL">全部课程（{{ resultTasks.length }} 份档案）</option>
          <option v-for="course in courseOptions" :key="course.id" :value="course.id">
            {{ course.title }}（{{ course.count }} 次记录）
          </option>
        </select>
      </label>
    </section>

    <div class="archive-overview">
      <div><span>成绩档案</span><strong>{{ courseScopedResultTasks.length }}</strong><small>份</small></div>
      <div><span>已评分</span><strong>{{ gradedTasks.length }}</strong><small>份</small></div>
      <div><span>待评分</span><strong>{{ pendingTasks.length }}</strong><small>份</small></div>
      <div class="average"><span>平均总分</span><strong>{{ averageScore }}</strong><small>分</small></div>
    </div>

    <div class="archive-toolbar">
      <div class="archive-filters" aria-label="成绩状态筛选">
        <button
          v-for="item in resultFilters"
          :key="item.value"
          type="button"
          :class="{ active: filter === item.value }"
          @click="filter = item.value"
        >
          {{ item.label }}
        </button>
      </div>
      <span>
        {{ selectedCourse?.title ?? '全部课程' }} · 当前显示 {{ visibleResultTasks.length }} 份成绩档案
      </span>
    </div>

    <div v-if="visibleResultTasks.length" class="archive-grid">
      <ResultArchiveCard
        v-for="(task, index) in visibleResultTasks"
        :key="task.id"
        :title="lessonFor(task.lessonId)?.title ?? task.title"
        :business-scope="businessScope(task)"
        :cover-badge="taskCoverBadge(task)"
        :avatar-text="(lessonFor(task.lessonId)?.title ?? task.title).slice(0, 1)"
        :owner-name="`第 ${task.attemptNumber} 次${modeLabel(task.mode)}`"
        :owner-meta="taskOwnerMeta(task)"
        :status="task.status"
        :status-label="taskStatusLabel(task)"
        :metrics="taskCardMetrics(task)"
        :summary="taskSummary(task)"
        :theme="cardTheme(index)"
        :selected="detailOpen && selectedTaskId === task.id"
        @select="openTask(task.id)"
      >
        <template #actions>
          <button class="secondary" type="button" @click="openTask(task.id)">
            {{ task.status === 'GRADED' ? '查看完整成绩' : '查看客观结果' }}
          </button>
          <RouterLink
            v-if="task.status === 'GRADED'"
            class="button primary"
            to="/student/tasks"
          >
            返回任务中心
          </RouterLink>
          <button v-else class="primary" type="button" disabled>等待评阅</button>
        </template>
      </ResultArchiveCard>
    </div>

    <p v-if="resultTasks.length" class="archive-disclaimer">
      评分说明：实训平台无法可靠绑定具体业务数据，客观成绩用于反映预设流程操作的完成情况，教师主观评分用于补充评价业务质量。
    </p>

    <article v-else class="card empty-state archive-empty">
      <div>
        <strong>{{ resultTasks.length ? '当前筛选条件下暂无成绩' : '暂无可查看的成绩档案' }}</strong>
        <p>完成并提交实训任务后，系统客观评分会立即出现在这里。</p>
        <RouterLink class="button primary" to="/student/tasks">
          返回我的任务
        </RouterLink>
      </div>
    </article>

    <div
      v-if="detailOpen && selectedTask"
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
            <h2>{{ lessonFor(selectedTask.lessonId)?.title ?? selectedTask.title }}</h2>
            <p>
              第 {{ selectedTask.attemptNumber }} 次{{ modeLabel(selectedTask.mode) }} ·
              {{ formatDate(selectedTask.gradedAt || selectedTask.submittedAt) }}
            </p>
          </div>
          <div>
            <StatusPill
              :status="selectedTask.status"
              :label="taskStatusLabel(selectedTask)"
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
              <small>/ {{ objectiveMax(selectedTask.lessonId) }}</small>
              <p>依据流程操作点和教学点完成情况自动评分</p>
            </div>
            <div class="subjective">
              <span>教师主观分</span>
              <strong>
                {{ selectedTask.status === 'GRADED' ? (selectedTask.subjectiveScore ?? 0) : '—' }}
              </strong>
              <small>/ {{ subjectiveMax(selectedTask.lessonId) }}</small>
              <p>{{ selectedTask.status === 'GRADED' ? '教师已完成主观评阅' : '教师正在评阅' }}</p>
            </div>
            <div class="total">
              <span>最终总分</span>
              <strong>
                {{
                  selectedTask.status === 'GRADED'
                    ? (selectedTask.objectiveScore ?? 0) +
                      (selectedTask.subjectiveScore ?? 0)
                    : '—'
                }}
              </strong>
              <small>
                /
                {{ objectiveMax(selectedTask.lessonId) + subjectiveMax(selectedTask.lessonId) }}
              </small>
              <p>客观分与教师主观分合计</p>
            </div>
          </div>

          <div class="result-detail-grid">
            <section class="detail-section">
              <div class="section-heading">
                <div>
                  <h3>教学点完成记录</h3>
                  <p>展示本次实训中的流程完成情况。</p>
                </div>
                <span>
                  {{ selectedTask.completedStageIds.length }} /
                  {{ lessonFor(selectedTask.lessonId)?.stages.length ?? 0 }}
                </span>
              </div>
              <div class="completed-stage-list">
                <div
                  v-for="(stage, index) in lessonFor(selectedTask.lessonId)?.stages ?? []"
                  :key="stage.id"
                  :class="{ complete: selectedTask.completedStageIds.includes(stage.id) }"
                >
                  <span>
                    {{ selectedTask.completedStageIds.includes(stage.id) ? '✓' : index + 1 }}
                  </span>
                  <p>
                    <strong>{{ stage.name }}</strong>
                    <small>
                      {{
                        selectedTask.completedStageIds.includes(stage.id)
                          ? '已完成预设流程操作'
                          : '未完成或由其他角色办理'
                      }}
                    </small>
                  </p>
                </div>
              </div>
            </section>

            <section class="detail-section teacher-comment">
              <div class="section-heading">
                <div><h3>教师反馈</h3><p>主观评分与改进建议。</p></div>
              </div>
              <div class="comment-body">
                <blockquote v-if="selectedTask.comment">“{{ selectedTask.comment }}”</blockquote>
                <div v-else class="pending-comment">
                  <span>评</span>
                  <p>
                    <strong>等待教师完成主观评阅</strong>
                    <small>评分完成后，主观分和批语会自动显示在这里。</small>
                  </p>
                </div>

                <p class="score-explain">
                  <strong>评分说明</strong>
                  实训平台无法可靠绑定具体业务数据，客观成绩用于反映预设流程操作的完成情况；教师通过主观评分补充评价业务质量。
                </p>

                <div
                  v-if="Object.keys(selectedTask.submissionValues).length"
                  class="submitted-values"
                >
                  <strong>我的提交信息</strong>
                  <p
                    v-for="field in store.state.examSettings[selectedTask.lessonId]
                      ?.submissionFields ?? []"
                    :key="field.key"
                  >
                    <span>{{ field.label }}</span>
                    <b>{{ selectedTask.submissionValues[field.key] || '未填写' }}</b>
                  </p>
                </div>
              </div>
            </section>
          </div>

          <div class="detail-actions">
            <button class="secondary" type="button" @click="closeDetail">关闭档案</button>
            <RouterLink class="button primary" to="/student/tasks">返回任务中心</RouterLink>
          </div>
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
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
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

.archive-overview > div:last-child {
  border-right: 0;
}

.archive-overview span,
.archive-overview small {
  color: #8791a2;
  font-size: 12px;
}

.archive-overview strong {
  color: #5847c8;
  font-size: 22px;
}

.archive-overview .average strong {
  color: #07815d;
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

.archive-disclaimer {
  margin: 16px 0 0;
  border: 1px solid #e5e2f8;
  border-radius: 11px;
  padding: 11px 14px;
  color: #77718d;
  background: #faf9ff;
  font-size: 11px;
  line-height: 1.7;
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
  width: min(1050px, 100%);
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
  display: grid;
  grid-template-columns: auto auto 1fr;
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

.score-strip p {
  grid-column: 1 / -1;
  margin: 2px 0 0;
  color: #969eac;
  font-size: 10px;
}

.result-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(320px, 0.85fr);
  gap: 16px;
}

.detail-section {
  overflow: hidden;
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

.completed-stage-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 9px;
  padding: 16px;
}

.completed-stage-list > div {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr);
  align-items: center;
  gap: 9px;
  border: 1px solid #e7e9ef;
  border-radius: 10px;
  padding: 10px;
  background: #fafbfc;
}

.completed-stage-list > div > span {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 8px;
  color: #8993a3;
  background: #eef0f4;
  font-size: 10px;
  font-weight: 900;
}

.completed-stage-list > div.complete > span {
  color: #087d5a;
  background: #e6f7f0;
}

.completed-stage-list p,
.pending-comment p {
  display: grid;
  gap: 3px;
  margin: 0;
}

.completed-stage-list strong,
.pending-comment strong {
  color: #414d61;
  font-size: 12px;
}

.completed-stage-list small,
.pending-comment small {
  color: #8f98a7;
  font-size: 10px;
}

.comment-body {
  display: grid;
  gap: 13px;
  padding: 16px;
}

.teacher-comment blockquote {
  margin: 0;
  border-left: 3px solid #8b7df3;
  border-radius: 0 10px 10px 0;
  padding: 15px 16px;
  color: #4e4a66;
  background: #f7f5ff;
  font-size: 13px;
  line-height: 1.8;
}

.pending-comment {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  border: 1px dashed #dfe3ea;
  border-radius: 10px;
  padding: 13px;
  background: #fafbfc;
}

.pending-comment > span {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 10px;
  color: #9b7027;
  background: #fff2dc;
  font-size: 11px;
  font-weight: 900;
}

.score-explain {
  margin: 0;
  border-radius: 10px;
  padding: 12px 13px;
  color: #7d8494;
  background: #f5f6f8;
  font-size: 11px;
  line-height: 1.7;
}

.score-explain strong {
  display: block;
  margin-bottom: 3px;
  color: #555f71;
}

.submitted-values {
  display: grid;
  gap: 8px;
  border: 1px solid #e7e9ef;
  border-radius: 10px;
  padding: 12px;
  background: #fafbfc;
}

.submitted-values > strong {
  color: #4b576b;
  font-size: 12px;
}

.submitted-values p {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin: 0;
  font-size: 11px;
}

.submitted-values span {
  color: #8a94a4;
}

.submitted-values b {
  color: #465267;
}

.detail-actions {
  display: flex;
  justify-content: flex-end;
  gap: 9px;
  margin-top: 16px;
}

@media (max-width: 1250px) {
  .archive-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 850px) {
  .course-switcher,
  .archive-overview,
  .score-strip {
    align-items: stretch;
  }

  .course-switcher {
    flex-direction: column;
  }

  .course-switcher label {
    width: 100%;
  }

  .archive-overview,
  .score-strip {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .archive-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .result-detail-grid {
    grid-template-columns: 1fr;
  }

  .detail-backdrop {
    padding: 0;
  }

  .detail-panel {
    width: 100%;
    max-height: 100vh;
    border-radius: 0;
  }
}

@media (max-width: 560px) {
  .archive-grid,
  .archive-overview,
  .score-strip,
  .completed-stage-list {
    grid-template-columns: minmax(0, 1fr);
  }

  .archive-overview > div,
  .score-strip > div {
    border-right: 0;
    border-bottom: 1px solid #edf0f4;
  }

  .detail-heading > div:last-child > :first-child {
    display: none;
  }
}
</style>
