<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusPill from '../../components/ui/StatusPill.vue';
import type {
  BusinessPlatform,
  BusinessPlatformModule,
  LessonPlan,
  StudentTask
} from '../../domain/models';
import { useTrainingStore } from '../../stores/trainingStore';
import { authApi } from '../../services/trainingApi';

type TrainingMode = 'LEARNING' | 'PRACTICE';
type TrainingStatusFilter = 'ALL' | 'TODO' | 'DOING' | 'FINISHED';

interface TrainingRound {
  learning?: StudentTask;
  practice?: StudentTask;
  publishedAt: string;
}

interface LessonTrainingGroup {
  lesson: LessonPlan;
  rounds: TrainingRound[];
  currentRound: TrainingRound;
  historyRounds: TrainingRound[];
  latestPublishedAt: string;
}

const store = useTrainingStore();
const router = useRouter();
store.refreshPublishedTaskStatuses();

const statusFilter = ref<TrainingStatusFilter>('ALL');
const platformFilter = ref('ALL');
const searchKeyword = ref('');
const openHistoryLessonIds = ref(new Set<string>());
const session = authApi.getSession();
const currentStudentId =
  session?.user.userId ??
  (typeof window === 'undefined'
    ? store.state.studentTasks[0]?.studentId ?? ''
    : '');

const myTasks = computed(() =>
  store.state.studentTasks.filter(
    (task) => task.studentId === currentStudentId
  )
);
const currentLearner = computed(() => ({
  id: currentStudentId,
  name: session?.user.displayName || session?.user.username || '学员'
}));

function lessonFor(lessonId: string) {
  return store.state.lessons.find((lesson) => lesson.id === lessonId);
}

function publishedTaskFor(task: StudentTask) {
  return store.state.publishedTasks.find(
    (published) => published.id === task.publishedTaskId
  );
}

function platformFor(lesson: LessonPlan) {
  return store.state.businessPlatforms.find(
    (platform) => platform.id === lesson.businessPlatformId
  );
}

function moduleFor(
  lesson: LessonPlan,
  platform?: BusinessPlatform
): BusinessPlatformModule | undefined {
  return platform?.modules.find(
    (businessModule) => businessModule.id === lesson.businessPlatformModuleId
  );
}

function taskPublishedAt(task: StudentTask) {
  return (
    publishedTaskFor(task)?.startAt ||
    task.startedAt ||
    task.submittedAt ||
    lessonFor(task.lessonId)?.publishedAt ||
    ''
  );
}

function timestamp(value?: string) {
  const parsed = value ? Date.parse(value) : 0;
  return Number.isFinite(parsed) ? parsed : 0;
}

function sortedTrainingTasks(tasks: StudentTask[], mode: TrainingMode) {
  return tasks
    .filter((task) => task.mode === mode)
    .sort((left, right) => timestamp(taskPublishedAt(right)) - timestamp(taskPublishedAt(left)));
}

function createTrainingRounds(tasks: StudentTask[]) {
  const learningTasks = sortedTrainingTasks(tasks, 'LEARNING');
  const practiceTasks = sortedTrainingTasks(tasks, 'PRACTICE');
  const count = Math.max(learningTasks.length, practiceTasks.length);
  return Array.from({ length: count }, (_, index) => {
    const learning = learningTasks[index];
    const practice = practiceTasks[index];
    const publishedAt = [learning, practice]
      .filter((task): task is StudentTask => Boolean(task))
      .map(taskPublishedAt)
      .sort((left, right) => timestamp(right) - timestamp(left))[0] ?? '';
    return { learning, practice, publishedAt };
  });
}

const trainingLessonGroups = computed<LessonTrainingGroup[]>(() => {
  const tasksByLesson = new Map<string, StudentTask[]>();
  myTasks.value
    .filter((task) => task.mode === 'LEARNING' || task.mode === 'PRACTICE')
    .forEach((task) => {
      const tasks = tasksByLesson.get(task.lessonId) ?? [];
      tasks.push(task);
      tasksByLesson.set(task.lessonId, tasks);
    });

  return [...tasksByLesson.entries()]
    .flatMap(([lessonId, tasks]) => {
      const lesson = lessonFor(lessonId);
      if (!lesson) return [];
      const rounds = createTrainingRounds(tasks);
      const currentRound = rounds[0];
      if (!currentRound) return [];
      return [
        {
          lesson,
          rounds,
          currentRound,
          historyRounds: rounds.slice(1),
          latestPublishedAt: currentRound.publishedAt
        }
      ];
    })
    .sort(
      (left, right) =>
        timestamp(right.latestPublishedAt) - timestamp(left.latestPublishedAt)
    );
});

function isTaskFinished(task?: StudentTask) {
  return task?.status === 'SUBMITTED' || task?.status === 'GRADED';
}

function groupStatus(group: LessonTrainingGroup): Exclude<TrainingStatusFilter, 'ALL'> {
  const tasks = [group.currentRound.learning, group.currentRound.practice].filter(
    (task): task is StudentTask => Boolean(task)
  );
  if (tasks.some((task) => task.status === 'DOING')) return 'DOING';
  if (tasks.some((task) => task.status === 'TODO')) return 'TODO';
  return 'FINISHED';
}

function groupStatusLabel(group: LessonTrainingGroup) {
  const { learning, practice } = group.currentRound;
  if (practice?.status === 'DOING') return '练习中';
  if (learning?.status === 'DOING') return '学习中';
  if (learning?.status === 'TODO') return '待学习';
  if (practice?.status === 'TODO') return '待练习';
  if (isTaskFinished(learning) && isTaskFinished(practice)) return '已完成';
  if (isTaskFinished(learning) && !practice) return '学习已完成';
  if (isTaskFinished(practice) && !learning) return '练习已完成';
  return '待开始';
}

function groupMatchesSearch(group: LessonTrainingGroup) {
  const keyword = searchKeyword.value.trim().toLocaleLowerCase();
  if (!keyword) return true;
  const platform = platformFor(group.lesson);
  const businessModule = moduleFor(group.lesson, platform);
  return [
    group.lesson.title,
    group.lesson.code,
    group.lesson.moduleName,
    platform?.name,
    businessModule?.name
  ]
    .filter(Boolean)
    .some((value) => String(value).toLocaleLowerCase().includes(keyword));
}

const visibleTrainingLessonGroups = computed(() =>
  trainingLessonGroups.value.filter((group) => {
    const statusMatches =
      statusFilter.value === 'ALL' || groupStatus(group) === statusFilter.value;
    const platformMatches =
      platformFilter.value === 'ALL' ||
      group.lesson.businessPlatformId === platformFilter.value;
    return statusMatches && platformMatches && groupMatchesSearch(group);
  })
);

const platformOptions = computed(() => {
  const options = new Map<string, { id: string; name: string; count: number }>();
  trainingLessonGroups.value.forEach((group) => {
    const platform = platformFor(group.lesson);
    const id = platform?.id ?? (group.lesson.businessPlatformId || 'UNASSIGNED');
    const current = options.get(id) ?? {
      id,
      name: platform?.name || '未配置业务平台',
      count: 0
    };
    current.count += 1;
    options.set(id, current);
  });
  return [...options.values()];
});

const todoCount = computed(
  () => trainingLessonGroups.value.filter((group) => groupStatus(group) === 'TODO').length
);
const doingCount = computed(
  () => trainingLessonGroups.value.filter((group) => groupStatus(group) === 'DOING').length
);
const finishedCount = computed(
  () => trainingLessonGroups.value.filter((group) => groupStatus(group) === 'FINISHED').length
);
const examTasks = computed(() =>
  myTasks.value
    .filter((task) => task.mode === 'EXAM')
    .sort((left, right) => timestamp(taskPublishedAt(right)) - timestamp(taskPublishedAt(left)))
);

const statusLabel: Record<StudentTask['status'], string> = {
  TODO: '待开始',
  DOING: '进行中',
  SUBMITTED: '待教师评分',
  GRADED: '已出成绩'
};

function taskRoleNames(group: LessonTrainingGroup) {
  const keys = [group.currentRound.learning, group.currentRound.practice]
    .filter((task): task is StudentTask => Boolean(task))
    .flatMap((task) => task.groupKeys);
  const plan = store.state.groupPlans[group.lesson.id];
  return [...new Set(keys)].map(
    (key) => plan?.roles.find((role) => role.key === key)?.name ?? key
  );
}

function taskRoleNamesForTask(task: StudentTask) {
  const plan = store.state.groupPlans[task.lessonId];
  return task.groupKeys.map(
    (key) => plan?.roles.find((role) => role.key === key)?.name ?? key
  );
}

function visibleStages(lesson: LessonPlan, mode: TrainingMode) {
  return lesson.stages.filter((stage) => stage.visibility[mode]);
}

function visibleStepCount(lesson: LessonPlan, mode: TrainingMode) {
  return visibleStages(lesson, mode).reduce(
    (total, stage) => total + stage.recordedSteps.length,
    0
  );
}

function taskProgress(task: StudentTask | undefined, lesson: LessonPlan, mode: TrainingMode) {
  if (!task) return 0;
  if (isTaskFinished(task)) return 100;
  if (mode === 'LEARNING') {
    const total = visibleStages(lesson, mode).length;
    return total ? Math.round((task.completedStageIds.length / total) * 100) : 0;
  }
  const total = visibleStepCount(lesson, mode);
  return total
    ? Math.round(((task.completedPracticeStepIds?.length ?? 0) / total) * 100)
    : 0;
}

function taskProgressLabel(
  task: StudentTask | undefined,
  lesson: LessonPlan,
  mode: TrainingMode
) {
  if (!task) return '教师暂未发布';
  if (mode === 'LEARNING') {
    const total = visibleStages(lesson, mode).length;
    const completed = isTaskFinished(task)
      ? total
      : Math.min(task.completedStageIds.length, total);
    return `${completed} / ${total} 个教学点`;
  }
  const total = visibleStepCount(lesson, mode);
  const completed = isTaskFinished(task)
    ? total
    : Math.min(task.completedPracticeStepIds?.length ?? 0, total);
  return `${completed} / ${total} 个操作点`;
}

function trainingTaskStatusLabel(task?: StudentTask) {
  if (!task) return '未发布';
  return isTaskFinished(task) ? '已完成' : statusLabel[task.status];
}

function taskActionLabel(task: StudentTask | undefined, mode: TrainingMode) {
  if (!task) return '暂未发布';
  if (isTaskFinished(task)) return mode === 'LEARNING' ? '重新学习' : '重新练习';
  if (task.status === 'DOING') return mode === 'LEARNING' ? '继续学习' : '继续练习';
  return mode === 'LEARNING' ? '开始学习' : '开始练习';
}

function roundStatusLabel(task?: StudentTask) {
  if (!task) return '未发布';
  if (task.status === 'GRADED') {
    const total = (task.objectiveScore ?? 0) + (task.subjectiveScore ?? 0);
    return `已评分 ${total} 分`;
  }
  return trainingTaskStatusLabel(task);
}

function formatPublishedAt(value?: string) {
  if (!value) return '发布时间未知';
  const date = new Date(value);
  if (!Number.isFinite(date.getTime())) return '发布时间未知';
  return new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date);
}

function formatDeadline(task: StudentTask) {
  const endAt = publishedTaskFor(task)?.endAt;
  if (!endAt) return '未设置截止时间';
  const date = new Date(endAt);
  if (!Number.isFinite(date.getTime())) return '未设置截止时间';
  return `${new Intl.DateTimeFormat('zh-CN', {
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date)}截止`;
}

function toggleHistory(lessonId: string) {
  const next = new Set(openHistoryLessonIds.value);
  if (next.has(lessonId)) next.delete(lessonId);
  else next.add(lessonId);
  openHistoryLessonIds.value = next;
}

async function openTrainingTask(task: StudentTask) {
  if (isTaskFinished(task)) {
    store.restartLearningOrPractice(task.id);
  }
  await router.push(`/student/tasks/${task.id}`);
}

function openHistoryRound(round: TrainingRound) {
  const task = round.practice ?? round.learning;
  if (task) void router.push(`/student/tasks/${task.id}`);
}
</script>

<template>
  <section class="page student-tasks">
    <PageHeader
      eyebrow="学生任务中心"
      :title="`${currentLearner.name}，开始今天的业务实训`"
      description="按业务平台和业务模块查找教案。同一教案的学习与练习合并展示，可直接进入对应任务。"
    >
      <div class="page-task-summary">
        <span><b>{{ trainingLessonGroups.length }}</b> 个教案</span>
        <span><b>{{ doingCount }}</b> 个进行中</span>
        <span><b>{{ todoCount }}</b> 个待开始</span>
        <span><b>{{ finishedCount }}</b> 个已完成</span>
      </div>
    </PageHeader>

    <div class="task-toolbar">
      <div class="platform-filters" aria-label="业务平台筛选">
        <button
          type="button"
          :class="{ active: platformFilter === 'ALL' }"
          @click="platformFilter = 'ALL'"
        >
          全部平台
          <small>{{ trainingLessonGroups.length }}</small>
        </button>
        <button
          v-for="platform in platformOptions"
          :key="platform.id"
          type="button"
          :class="{ active: platformFilter === platform.id }"
          @click="platformFilter = platform.id"
        >
          {{ platform.name }}
          <small>{{ platform.count }}</small>
        </button>
      </div>
      <label class="task-search">
        <span>搜索</span>
        <input v-model="searchKeyword" type="search" placeholder="输入教案或业务模块名称" />
      </label>
    </div>

    <div class="status-toolbar">
      <div class="status-tabs">
        <button
          v-for="item in [
            ['ALL', '全部'],
            ['TODO', '待开始'],
            ['DOING', '进行中'],
            ['FINISHED', '已完成']
          ]"
          :key="item[0]"
          type="button"
          :class="{ active: statusFilter === item[0] }"
          @click="statusFilter = item[0] as TrainingStatusFilter"
        >
          {{ item[1] }}
        </button>
      </div>
      <span>共 {{ visibleTrainingLessonGroups.length }} 个教案</span>
    </div>

    <div v-if="visibleTrainingLessonGroups.length" class="course-gallery">
      <article
        v-for="(group, lessonIndex) in visibleTrainingLessonGroups"
        :key="group.lesson.id"
        class="course-card"
        :class="`course-tone-${lessonIndex % 3}`"
      >
        <header class="course-cover">
          <span class="course-status" :class="groupStatus(group).toLowerCase()">
            {{ groupStatusLabel(group) }}
          </span>
          <small>
            {{ platformFor(group.lesson)?.name || '未配置业务平台' }} ·
            {{
              moduleFor(group.lesson, platformFor(group.lesson))?.name ||
              group.lesson.moduleName ||
              '未配置业务模块'
            }}
          </small>
          <h2>{{ group.lesson.title }}</h2>
          <p>发布于 {{ formatPublishedAt(group.latestPublishedAt) }}</p>
        </header>

        <div class="course-body">
          <div class="course-meta">
            <span>{{ group.lesson.stages.length }} 个教学点</span>
            <span>
              {{
                group.lesson.stages.reduce(
                  (total, stage) => total + stage.recordedSteps.length,
                  0
                )
              }}
              个操作节点
            </span>
            <span>第 {{ group.lesson.version }} 版</span>
          </div>

          <div v-if="taskRoleNames(group).length" class="course-role">
            <span>我的业务角色</span>
            <b v-for="name in taskRoleNames(group)" :key="name">{{ name }}</b>
          </div>

          <div class="course-mode-list">
            <section class="course-mode-row learning">
              <i>学</i>
              <div>
                <strong>学习任务</strong>
                <span>
                  {{
                    taskProgressLabel(
                      group.currentRound.learning,
                      group.lesson,
                      'LEARNING'
                    )
                  }}
                </span>
                <em>
                  <b
                    :style="{
                      width: `${taskProgress(
                        group.currentRound.learning,
                        group.lesson,
                        'LEARNING'
                      )}%`
                    }"
                  ></b>
                </em>
              </div>
              <small :class="{ finished: isTaskFinished(group.currentRound.learning) }">
                {{ trainingTaskStatusLabel(group.currentRound.learning) }}
              </small>
            </section>

            <section class="course-mode-row practice">
              <i>练</i>
              <div>
                <strong>练习任务</strong>
                <span>
                  {{
                    taskProgressLabel(
                      group.currentRound.practice,
                      group.lesson,
                      'PRACTICE'
                    )
                  }}
                </span>
                <em>
                  <b
                    :style="{
                      width: `${taskProgress(
                        group.currentRound.practice,
                        group.lesson,
                        'PRACTICE'
                      )}%`
                    }"
                  ></b>
                </em>
              </div>
              <RouterLink
                v-if="group.currentRound.practice?.status === 'GRADED'"
                class="course-feedback-link"
                :to="{
                  path: '/student/results',
                  query: { lessonId: group.lesson.id }
                }"
              >
                查看反馈
              </RouterLink>
              <small
                v-else
                :class="{ finished: isTaskFinished(group.currentRound.practice) }"
              >
                {{ trainingTaskStatusLabel(group.currentRound.practice) }}
              </small>
            </section>
          </div>

          <div class="course-actions">
            <button
              type="button"
              :disabled="!group.currentRound.learning"
              @click="
                group.currentRound.learning &&
                openTrainingTask(group.currentRound.learning)
              "
            >
              {{ taskActionLabel(group.currentRound.learning, 'LEARNING') }}
            </button>
            <button
              class="practice"
              type="button"
              :disabled="!group.currentRound.practice"
              @click="
                group.currentRound.practice &&
                openTrainingTask(group.currentRound.practice)
              "
            >
              {{ taskActionLabel(group.currentRound.practice, 'PRACTICE') }}
            </button>
          </div>

          <template v-if="group.historyRounds.length">
            <button
              class="course-history-toggle"
              type="button"
              :aria-expanded="openHistoryLessonIds.has(group.lesson.id)"
              @click="toggleHistory(group.lesson.id)"
            >
              <span>
                另有 <b>{{ group.historyRounds.length }} 轮</b>历史任务
              </span>
              <span>
                {{ openHistoryLessonIds.has(group.lesson.id) ? '收起' : '查看记录' }}
              </span>
            </button>
            <div
              v-if="openHistoryLessonIds.has(group.lesson.id)"
              class="course-history-list"
            >
              <div
                v-for="(round, historyIndex) in group.historyRounds"
                :key="`${group.lesson.id}-${historyIndex}`"
                class="course-history-row"
              >
                <strong>第 {{ group.rounds.length - historyIndex - 1 }} 轮</strong>
                <span>{{ formatPublishedAt(round.publishedAt) }}</span>
                <span>学习：{{ roundStatusLabel(round.learning) }}</span>
                <span>练习：{{ roundStatusLabel(round.practice) }}</span>
                <button type="button" @click="openHistoryRound(round)">打开</button>
              </div>
            </div>
          </template>
        </div>
      </article>
    </div>

    <article v-else class="card empty-state training-empty">
      <div>
        <strong>当前没有符合条件的学习或练习任务</strong>
        <p>教师发布任务后，学习与练习会自动合并到对应的教案卡片中。</p>
      </div>
    </article>

    <section v-if="examTasks.length" class="exam-section">
      <header>
        <div>
          <h2>我的考试</h2>
          <p>考试具有截止时间、作答次数和成绩状态，因此保持独立展示。</p>
        </div>
        <span>{{ examTasks.length }} 项</span>
      </header>
      <div class="exam-task-grid">
        <article v-for="task in examTasks" :key="task.id" class="exam-task-card">
          <span class="exam-mark">考</span>
          <div>
            <h3>{{ task.title }}</h3>
            <p>{{ lessonFor(task.lessonId)?.moduleName }} · {{ formatDeadline(task) }}</p>
          </div>
          <StatusPill :status="task.status" :label="statusLabel[task.status]" />
          <div v-if="taskRoleNamesForTask(task).length" class="exam-role-block">
            <span>我的业务角色</span>
            <b v-for="name in taskRoleNamesForTask(task)" :key="name">{{ name }}</b>
          </div>
          <RouterLink
            class="button primary"
            :to="
              task.status === 'GRADED'
                ? { path: '/student/results', query: { lessonId: task.lessonId } }
                : `/student/tasks/${task.id}`
            "
          >
            {{
              task.status === 'TODO'
                ? '开始考试'
                : task.status === 'DOING'
                  ? '继续考试'
                  : task.status === 'GRADED'
                    ? '查看成绩'
                    : '查看提交结果'
            }}
          </RouterLink>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.student-metrics {
  margin-bottom: 20px;
}

.task-toolbar,
.status-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.task-toolbar {
  border: 1px solid #e3e7ee;
  border-radius: 14px;
  padding: 8px;
  background: #fff;
}

.platform-filters,
.status-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.platform-filters button,
.status-tabs button {
  min-height: 40px;
  border: 0;
  border-radius: 10px;
  padding: 0 14px;
  color: #748094;
  background: transparent;
  font-size: 14px;
  font-weight: 800;
}

.platform-filters button {
  display: flex;
  align-items: center;
  gap: 7px;
}

.platform-filters button small {
  display: grid;
  min-width: 22px;
  height: 22px;
  place-items: center;
  border-radius: 999px;
  color: #737e91;
  background: #eef1f5;
  font-size: 12px;
}

.platform-filters button.active,
.status-tabs button.active {
  color: #fff;
  background: #6657d6;
}

.platform-filters button.active small {
  color: #5d50be;
  background: #fff;
}

.task-search {
  display: flex;
  min-width: 310px;
  align-items: center;
  gap: 9px;
  border: 1px solid #e0e4eb;
  border-radius: 10px;
  padding: 0 12px;
}

.task-search span {
  color: #7d8797;
  font-size: 13px;
  font-weight: 800;
}

.task-search input {
  width: 100%;
  height: 40px;
  border: 0;
  outline: 0;
  color: #344056;
  background: transparent;
  font: inherit;
  font-size: 14px;
}

.status-toolbar {
  margin: 14px 0 20px;
}

.status-tabs {
  border: 1px solid #e3e7ee;
  border-radius: 11px;
  padding: 4px;
  background: #fff;
}

.status-tabs button {
  min-height: 34px;
  padding: 0 13px;
}

.status-toolbar > span {
  color: #808b9d;
  font-size: 13px;
}

.platform-groups,
.platform-group {
  display: grid;
  gap: 18px;
}

.platform-group + .platform-group {
  margin-top: 12px;
}

.platform-heading {
  display: flex;
  align-items: center;
  gap: 12px;
}

.platform-mark {
  display: grid;
  width: 46px;
  height: 46px;
  flex: 0 0 46px;
  place-items: center;
  border-radius: 13px;
  color: #fff;
  background: linear-gradient(145deg, #7567e7, #5544c3);
  font-size: 18px;
  font-weight: 900;
}

.platform-heading h2,
.platform-heading p,
.lesson-card-heading h3,
.lesson-card-heading p,
.exam-section h2,
.exam-section p,
.exam-task-card h3,
.exam-task-card p {
  margin: 0;
}

.platform-heading h2 {
  color: #2c384d;
  font-size: 20px;
}

.platform-heading p {
  margin-top: 4px;
  color: #818c9d;
  font-size: 13px;
}

.module-group {
  overflow: hidden;
  border: 1px solid #e1e5ec;
  border-radius: 17px;
  background: #eef1f5;
}

.module-heading {
  display: flex;
  min-height: 54px;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 0 17px;
}

.module-heading strong {
  color: #374358;
  font-size: 16px;
}

.module-heading span {
  color: #7567d4;
  font-size: 13px;
  font-weight: 800;
}

.lesson-task-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  padding: 0 14px 14px;
}

.lesson-task-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e3e7ee;
  border-radius: 15px;
  background: #fff;
  box-shadow: 0 8px 22px rgb(31 38 70 / 5%);
}

.lesson-card-heading {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: start;
  gap: 12px;
  padding: 18px 18px 14px;
}

.lesson-order {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 12px;
  color: #5d4ec7;
  background: #efedff;
  font-size: 15px;
  font-weight: 900;
}

.lesson-card-heading h3 {
  overflow: hidden;
  color: #303b4f;
  font-size: 17px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.lesson-card-heading p {
  margin-top: 5px;
  color: #8892a2;
  font-size: 12px;
}

.lesson-status {
  border-radius: 999px;
  padding: 6px 9px;
  color: #25775f;
  background: #eaf8f2;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.lesson-status.todo {
  color: #a66c1a;
  background: #fff4e3;
}

.lesson-status.doing {
  color: #286fba;
  background: #edf5ff;
}

.lesson-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 0 18px 12px;
}

.lesson-meta span {
  border-radius: 7px;
  padding: 5px 8px;
  color: #6f7a8d;
  background: #f2f4f7;
  font-size: 12px;
}

.role-block {
  display: flex;
  align-items: center;
  gap: 9px;
  margin: 0 18px 13px;
  border-radius: 10px;
  padding: 9px 11px;
  background: #f8f7ff;
}

.role-block > span {
  color: #796db1;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.role-block > div {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.role-block b {
  border: 1px solid #d9d3ff;
  border-radius: 6px;
  padding: 4px 7px;
  color: #5b4ac9;
  background: #fff;
  font-size: 12px;
}

.training-task-pair {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 0 18px 17px;
}

.training-mode-card {
  display: grid;
  min-width: 0;
  gap: 10px;
  border: 1px solid #dcd7ff;
  border-radius: 13px;
  padding: 13px;
  background: #faf9ff;
}

.training-mode-card.practice {
  border-color: #d7ece4;
  background: #f8fcfa;
}

.training-mode-card > header,
.mode-detail {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.training-mode-card header strong {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #3a4559;
  font-size: 14px;
}

.training-mode-card header strong i {
  display: grid;
  width: 27px;
  height: 27px;
  place-items: center;
  border-radius: 8px;
  color: #fff;
  background: #6657d6;
  font-size: 12px;
  font-style: normal;
}

.training-mode-card.practice header strong i {
  background: #14936d;
}

.training-mode-card header > span {
  color: #7d8798;
  font-size: 12px;
  font-weight: 800;
  white-space: nowrap;
}

.training-mode-card header > span.finished {
  color: #168260;
}

.mode-progress {
  overflow: hidden;
  height: 5px;
  border-radius: 999px;
  background: #e7e9ef;
}

.mode-progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #6657d6;
  transition: width 180ms ease;
}

.practice .mode-progress i {
  background: #14936d;
}

.mode-detail {
  min-height: 28px;
  color: #7b8698;
  font-size: 12px;
}

.mode-detail b {
  color: #4d586c;
}

.mode-detail a {
  color: #6657d6;
  font-weight: 800;
  text-decoration: none;
}

.training-mode-card > button {
  min-height: 42px;
  border: 0;
  border-radius: 10px;
  color: #fff;
  background: #6657d6;
  font-size: 14px;
  font-weight: 900;
}

.training-mode-card.practice > button {
  background: #14936d;
}

.training-mode-card > button:disabled {
  color: #9ba4b3;
  background: #e9ecf1;
  cursor: not-allowed;
}

.history-toggle {
  display: flex;
  width: 100%;
  min-height: 45px;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 0;
  border-top: 1px solid #e6e9ef;
  padding: 0 18px;
  color: #6f7a8d;
  background: #fafbfd;
  font-size: 12px;
}

.history-toggle b {
  color: #5f50c8;
}

.history-list {
  border-top: 1px solid #e6e9ef;
  background: #f8f9fb;
}

.history-row {
  display: grid;
  grid-template-columns: minmax(145px, 1.25fr) repeat(2, minmax(95px, 1fr)) auto;
  align-items: center;
  gap: 10px;
  padding: 11px 18px;
  color: #6d788b;
  font-size: 12px;
}

.history-row + .history-row {
  border-top: 1px dashed #dce1e8;
}

.history-row strong {
  color: #435067;
}

.history-row button {
  border: 0;
  color: #5f50c8;
  background: transparent;
  font-weight: 800;
}

.training-empty {
  margin-top: 18px;
}

.exam-section {
  margin-top: 28px;
  border-top: 1px solid #e2e6ed;
  padding-top: 24px;
}

.exam-section > header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 13px;
}

.exam-section h2 {
  color: #323e52;
  font-size: 20px;
}

.exam-section p {
  margin-top: 4px;
  color: #818c9d;
  font-size: 13px;
}

.exam-section > header > span {
  border-radius: 999px;
  padding: 6px 10px;
  color: #a66b19;
  background: #fff2dd;
  font-size: 12px;
  font-weight: 800;
}

.exam-task-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 13px;
}

.exam-task-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  border: 1px solid #ead9c0;
  border-radius: 14px;
  padding: 14px;
  background: #fffaf2;
}

.exam-mark {
  display: grid;
  width: 42px;
  height: 42px;
  grid-row: span 2;
  place-items: center;
  border-radius: 12px;
  color: #fff;
  background: #c97b1b;
  font-weight: 900;
}

.exam-task-card h3 {
  color: #3d4656;
  font-size: 15px;
}

.exam-task-card p {
  margin-top: 4px;
  color: #88775f;
  font-size: 12px;
}

.exam-task-card .button {
  grid-column: 2 / -1;
  justify-self: end;
}

.exam-role-block {
  display: flex;
  grid-column: 2 / -1;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}

.exam-role-block span {
  color: #8a7251;
  font-size: 12px;
}

.exam-role-block b {
  border: 1px solid #edd5b2;
  border-radius: 6px;
  padding: 4px 7px;
  color: #8c5b18;
  background: #fff;
  font-size: 12px;
}

@media (max-width: 1180px) {
  .lesson-task-grid,
  .exam-task-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .task-toolbar,
  .status-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .task-search {
    min-width: 0;
  }

  .status-toolbar > span {
    align-self: flex-end;
  }

  .training-task-pair {
    grid-template-columns: 1fr;
  }

  .lesson-card-heading {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .lesson-status {
    grid-column: 2;
    justify-self: start;
  }

  .role-block {
    align-items: flex-start;
    flex-direction: column;
  }

  .history-row {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .history-row span {
    display: none;
  }
}

/* 课程卡片型：平台与模块放入封面，卡片主体只保留任务状态和操作。 */
.page-task-summary {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.page-task-summary span {
  border: 1px solid #e1e5ec;
  border-radius: 999px;
  padding: 7px 11px;
  color: #788396;
  background: #fff;
  font-size: 12px;
  white-space: nowrap;
}

.page-task-summary b {
  color: #4f43b5;
  font-size: 15px;
}

.task-toolbar {
  border: 0;
  padding: 0;
  background: transparent;
}

.platform-filters button {
  min-height: 38px;
  border: 1px solid #e0e4eb;
  border-radius: 999px;
  background: #fff;
}

.platform-filters button.active {
  border-color: #cec8ff;
  color: #5a4bc3;
  background: #f1efff;
}

.platform-filters button.active small {
  color: #fff;
  background: #6657d6;
}

.task-search {
  background: #fff;
}

.status-toolbar {
  margin: 12px 0 16px;
}

.status-tabs {
  border: 0;
  padding: 0;
  background: transparent;
}

.status-tabs button {
  min-height: 32px;
  border: 1px solid transparent;
  border-radius: 999px;
}

.status-tabs button.active {
  border-color: #d7d2fb;
  color: #5a4bc3;
  background: #f1efff;
}

.course-gallery {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.course-card {
  display: flex;
  min-width: 0;
  overflow: hidden;
  flex-direction: column;
  border: 1px solid #e1e5ec;
  border-radius: 17px;
  background: #fff;
  box-shadow: 0 10px 28px rgb(30 38 68 / 6%);
}

.course-cover {
  position: relative;
  display: flex;
  min-height: 130px;
  flex-direction: column;
  justify-content: flex-end;
  padding: 18px;
  color: #fff;
  background:
    radial-gradient(circle at 88% 18%, rgb(255 255 255 / 23%), transparent 24%),
    linear-gradient(135deg, #6e63db, #4c3baa);
}

.course-tone-1 .course-cover {
  background:
    radial-gradient(circle at 88% 18%, rgb(255 255 255 / 21%), transparent 24%),
    linear-gradient(135deg, #279777, #146e55);
}

.course-tone-2 .course-cover {
  background:
    radial-gradient(circle at 88% 18%, rgb(255 255 255 / 21%), transparent 24%),
    linear-gradient(135deg, #4d88d0, #3159a4);
}

.course-status {
  position: absolute;
  top: 14px;
  right: 14px;
  border: 1px solid rgb(255 255 255 / 18%);
  border-radius: 999px;
  padding: 5px 9px;
  color: #fff;
  background: rgb(255 255 255 / 17%);
  font-size: 12px;
  font-weight: 800;
  backdrop-filter: blur(8px);
}

.course-cover small {
  overflow: hidden;
  margin-right: 78px;
  opacity: 0.8;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-cover h2 {
  display: -webkit-box;
  overflow: hidden;
  margin: 6px 0 0;
  font-size: 19px;
  line-height: 1.35;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.course-cover p {
  margin: 6px 0 0;
  opacity: 0.7;
  font-size: 12px;
}

.course-body {
  display: flex;
  min-height: 0;
  flex: 1;
  flex-direction: column;
  padding: 16px;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.course-meta span {
  border-radius: 7px;
  padding: 5px 8px;
  color: #6f7a8c;
  background: #f2f4f7;
  font-size: 12px;
}

.course-role {
  display: flex;
  min-height: 34px;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-bottom: 12px;
  border-radius: 9px;
  padding: 6px 9px;
  background: #f7f5ff;
}

.course-role span {
  color: #7a6fad;
  font-size: 12px;
  font-weight: 800;
}

.course-role b {
  border: 1px solid #d9d3ff;
  border-radius: 6px;
  padding: 3px 6px;
  color: #5a4bc4;
  background: #fff;
  font-size: 11px;
}

.course-mode-list {
  display: grid;
  gap: 9px;
}

.course-mode-row {
  display: grid;
  min-width: 0;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
  border: 1px solid #e4e7ed;
  border-radius: 11px;
  padding: 10px;
  background: #fcfcfd;
}

.course-mode-row > i {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 9px;
  color: #fff;
  background: #6657d6;
  font-size: 12px;
  font-style: normal;
  font-weight: 900;
}

.course-mode-row.practice > i {
  background: #168866;
}

.course-mode-row > div {
  display: grid;
  min-width: 0;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 2px 8px;
}

.course-mode-row strong {
  color: #3e495d;
  font-size: 13px;
}

.course-mode-row > div > span {
  overflow: hidden;
  color: #838d9d;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-mode-row em {
  grid-column: 1 / -1;
  overflow: hidden;
  height: 4px;
  margin-top: 4px;
  border-radius: 999px;
  background: #e9ebf0;
}

.course-mode-row em b {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: #6657d6;
}

.course-mode-row.practice em b {
  background: #168866;
}

.course-mode-row > small,
.course-feedback-link {
  color: #758094;
  font-size: 11px;
  font-weight: 800;
  white-space: nowrap;
}

.course-mode-row > small.finished,
.course-feedback-link {
  color: #16805f;
}

.course-feedback-link {
  text-decoration: none;
}

.course-actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: auto;
  padding-top: 14px;
}

.course-actions button {
  min-height: 43px;
  border: 0;
  border-radius: 10px;
  color: #fff;
  background: #6657d6;
  font-size: 14px;
  font-weight: 900;
}

.course-actions button.practice {
  background: #168866;
}

.course-actions button:disabled {
  color: #98a2b1;
  background: #e8ebf0;
  cursor: not-allowed;
}

.course-history-toggle {
  display: flex;
  width: 100%;
  min-height: 38px;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  margin-top: 10px;
  border: 0;
  padding: 0;
  color: #7d8797;
  background: transparent;
  font-size: 11px;
}

.course-history-toggle b,
.course-history-toggle span:last-child {
  color: #5d50c5;
  font-weight: 800;
}

.course-history-list {
  overflow: hidden;
  border: 1px solid #e2e6ec;
  border-radius: 9px;
  background: #fafbfd;
}

.course-history-row {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 4px 8px;
  padding: 9px 10px;
  color: #768194;
  font-size: 11px;
}

.course-history-row + .course-history-row {
  border-top: 1px dashed #dce1e8;
}

.course-history-row strong {
  color: #435066;
}

.course-history-row span:nth-of-type(1) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.course-history-row span:nth-of-type(2),
.course-history-row span:nth-of-type(3) {
  grid-column: span 1;
}

.course-history-row button {
  grid-row: 1 / span 2;
  grid-column: 3;
  border: 0;
  color: #5d50c5;
  background: transparent;
  font-size: 11px;
  font-weight: 800;
}

@media (max-width: 1320px) {
  .course-gallery {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .course-gallery {
    grid-template-columns: 1fr;
  }

  .course-cover {
    min-height: 122px;
  }
}
</style>
