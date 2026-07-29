<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { RouterLink, useRouter } from 'vue-router';
import MetricCard from '../../components/ui/MetricCard.vue';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusPill from '../../components/ui/StatusPill.vue';
import { useTrainingStore } from '../../stores/trainingStore';

const store = useTrainingStore();
const router = useRouter();
store.refreshPublishedTaskStatuses();
const statusFilter = ref('ALL');

const learners = computed(() => {
  const map = new Map<string, string>();
  store.state.studentTasks.forEach((task) =>
    map.set(task.studentId, task.studentName)
  );
  return [...map.entries()].map(([id, name]) => ({ id, name }));
});
const selectedStudentId = ref(learners.value[0]?.id ?? '');

watch(
  learners,
  (items) => {
    if (!items.some((item) => item.id === selectedStudentId.value)) {
      selectedStudentId.value = items[0]?.id ?? '';
    }
  },
  { immediate: true }
);

const myTasks = computed(() =>
  store.state.studentTasks.filter(
    (task) => task.studentId === selectedStudentId.value
  )
);
const visibleTasks = computed(() =>
  myTasks.value.filter(
    (task) =>
      statusFilter.value === 'ALL' || task.status === statusFilter.value
  )
);
const currentLearner = computed(() =>
  learners.value.find((item) => item.id === selectedStudentId.value)
);
const todoCount = computed(
  () => myTasks.value.filter((task) => task.status === 'TODO').length
);
const doingCount = computed(
  () => myTasks.value.filter((task) => task.status === 'DOING').length
);
const finishedCount = computed(
  () =>
    myTasks.value.filter(
      (task) => task.status === 'SUBMITTED' || task.status === 'GRADED'
    ).length
);

const statusLabel: Record<string, string> = {
  TODO: '待开始',
  DOING: '办理中',
  SUBMITTED: '待教师评分',
  GRADED: '已出成绩'
};

function modeLabel(mode: 'LEARNING' | 'PRACTICE' | 'EXAM') {
  return mode === 'LEARNING' ? '学习' : mode === 'PRACTICE' ? '练习' : '考试';
}

function taskStatusLabel(task: {
  mode: 'LEARNING' | 'PRACTICE' | 'EXAM';
  status: string;
}) {
  if (
    task.mode !== 'EXAM' &&
    (task.status === 'SUBMITTED' || task.status === 'GRADED')
  ) {
    return '已完成';
  }
  return statusLabel[task.status];
}

function lessonFor(lessonId: string) {
  return store.state.lessons.find((lesson) => lesson.id === lessonId);
}

function taskRoleNames(lessonId: string, groupKeys: string[]) {
  const plan = store.state.groupPlans[lessonId];
  return groupKeys.map(
    (key) => plan?.roles.find((role) => role.key === key)?.name ?? key
  );
}

function taskDataLabel(taskId: string) {
  const task = store.state.studentTasks.find((item) => item.id === taskId);
  if (!task?.dataItemId) return '平台将在进入任务时分配';
  const data = (store.state.dataItems[task.lessonId] ?? []).find(
    (item) => item.id === task.dataItemId
  );
  return data?.maskedReference ?? '已分配业务数据';
}

async function restartTrainingTask(taskId: string) {
  store.restartLearningOrPractice(taskId);
  await router.push(`/student/tasks/${taskId}`);
}
</script>

<template>
  <section class="page student-tasks">
    <PageHeader
      eyebrow="MY TRAINING DESK"
      :title="`${currentLearner?.name ?? '学员'}，开始今天的业务实训`"
      description="根据你承担的业务角色进入任务。平台只记录页面访问和流程操作，完成当前角色阶段后将自动移交给下一角色。"
    >
      <label v-if="learners.length > 1" class="student-session">
        <span>演示学员会话</span>
        <select v-model="selectedStudentId">
          <option
            v-for="learner in learners"
            :key="learner.id"
            :value="learner.id"
          >
            {{ learner.name }}（{{ learner.id }}）
          </option>
        </select>
      </label>
    </PageHeader>

    <div class="metric-grid student-metrics">
      <MetricCard
        label="我的任务"
        :value="myTasks.length"
        hint="当前登录会话已分配"
        tone="purple"
      >
        <template #icon>任</template>
      </MetricCard>
      <MetricCard
        label="待开始"
        :value="todoCount"
        hint="尚未进入业务页面"
        tone="amber"
      >
        <template #icon>待</template>
      </MetricCard>
      <MetricCard
        label="办理中"
        :value="doingCount"
        hint="已留下流程访问轨迹"
        tone="blue"
      >
        <template #icon>办</template>
      </MetricCard>
      <MetricCard
        label="已提交"
        :value="finishedCount"
        hint="含等待评分与已出成绩"
        tone="green"
      >
        <template #icon>完</template>
      </MetricCard>
    </div>

    <div class="task-toolbar">
      <div class="task-tabs">
        <button
          v-for="item in [
            ['ALL', '全部任务'],
            ['TODO', '待开始'],
            ['DOING', '办理中'],
            ['SUBMITTED', '待评分'],
            ['GRADED', '已出成绩']
          ]"
          :key="item[0]"
          type="button"
          :class="{ active: statusFilter === item[0] }"
          @click="statusFilter = item[0]"
        >
          {{ item[1] }}
        </button>
      </div>
      <span>共 {{ visibleTasks.length }} 项</span>
    </div>

    <div v-if="visibleTasks.length" class="student-task-grid">
      <article v-for="task in visibleTasks" :key="task.id" class="task-card">
        <header>
          <span class="mode-badge">{{ modeLabel(task.mode) }}</span>
          <StatusPill :status="task.status" :label="taskStatusLabel(task)" />
        </header>
        <div class="task-title">
          <span>{{ lessonFor(task.lessonId)?.moduleName.slice(0, 1) }}</span>
          <div>
            <h2>{{ task.title }}</h2>
            <p>
              {{ lessonFor(task.lessonId)?.moduleName }} ·
              {{ lessonFor(task.lessonId)?.code }}
            </p>
          </div>
        </div>
        <div class="role-block">
          <span>我的业务角色</span>
          <div>
            <b
              v-for="name in taskRoleNames(task.lessonId, task.groupKeys)"
              :key="name"
            >
              {{ name }}
            </b>
          </div>
          <small v-if="task.groupKeys.length > 1">
            本次由同一学员承担多个角色，请按阶段顺序完成完整业务。
          </small>
        </div>
        <dl>
          <div>
            <dt>流程完成</dt>
            <dd>
              {{ task.completedStageIds.length }} /
              {{ lessonFor(task.lessonId)?.stages.length ?? 0 }} 阶段
            </dd>
          </div>
          <div>
            <dt>业务数据</dt>
            <dd>{{ taskDataLabel(task.id) }}</dd>
          </div>
          <div>
            <dt>{{ task.mode === 'EXAM' ? '作答次数' : '完成次数' }}</dt>
            <dd>
              <template v-if="task.mode === 'EXAM'">
                第 {{ task.attemptNumber }} /
                {{ store.state.examSettings[task.lessonId]?.maxAttempts ?? 1 }} 次
              </template>
              <template v-else>第 {{ task.attemptNumber }} 次</template>
            </dd>
          </div>
          <div>
            <dt>客观评分</dt>
            <dd>
              {{
                task.objectiveScore === undefined
                  ? '提交后生成'
                  : `${task.objectiveScore} 分`
              }}
            </dd>
          </div>
        </dl>
        <div class="task-card__footer">
          <RouterLink
            v-if="task.status === 'TODO' || task.status === 'DOING'"
            class="button primary"
            :to="`/student/tasks/${task.id}`"
          >
            {{ task.status === 'TODO' ? '开始办理' : '继续办理' }}
          </RouterLink>
          <template
            v-else-if="task.mode === 'LEARNING' || task.mode === 'PRACTICE'"
          >
            <RouterLink
              class="button secondary"
              :to="`/student/tasks/${task.id}`"
            >
              查看完成记录
            </RouterLink>
            <button
              class="primary"
              type="button"
              @click="restartTrainingTask(task.id)"
            >
              {{ task.mode === 'LEARNING' ? '重新学习' : '重新练习' }}
            </button>
          </template>
          <RouterLink
            v-else
            class="button secondary"
            :to="
              task.status === 'GRADED'
                ? '/student/results'
                : `/student/tasks/${task.id}`
            "
          >
            {{ task.status === 'GRADED' ? '查看成绩' : '查看提交结果' }}
          </RouterLink>
        </div>
      </article>
    </div>

    <article v-else class="card empty-state">
      <div>
        <strong>当前没有符合条件的任务</strong>
        <p>考试发布后，平台会根据单位与角色分组自动生成学员任务。</p>
        <RouterLink class="button secondary" to="/admin/lessons">
          查看后台演示链路
        </RouterLink>
      </div>
    </article>
  </section>
</template>

<style scoped>
.student-session {
  display: flex;
  align-items: center;
  gap: 9px;
}

.student-session span {
  white-space: nowrap;
}

.student-session select {
  min-width: 220px;
}

.student-metrics {
  margin-bottom: 18px;
}

.task-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 15px;
  margin-bottom: 13px;
}

.task-toolbar > span {
  color: #8993a4;
  font-size: 10px;
}

.task-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 4px;
  border: 1px solid #e5e8ef;
  border-radius: 10px;
  background: #fff;
}

.task-tabs button {
  min-height: 31px;
  border: 0;
  padding: 0 11px;
  background: transparent;
  color: #7c8798;
  font-size: 9px;
}

.task-tabs button.active {
  background: #eeeaff;
  color: #5848c8;
}

.student-task-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 15px;
}

.task-card {
  display: grid;
  gap: 16px;
  align-content: start;
  border: 1px solid #e5e8f0;
  border-radius: 15px;
  background: #fff;
  padding: 17px;
  box-shadow: 0 9px 24px rgb(31 37 79 / 4%);
}

.task-card > header,
.task-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.mode-badge {
  border-radius: 6px;
  padding: 4px 7px;
  color: #5e4cd5;
  background: #eeebff;
  font-size: 9px;
  font-weight: 900;
}

.task-title {
  display: flex;
  align-items: center;
  gap: 11px;
}

.task-title > span {
  display: grid;
  width: 43px;
  height: 43px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 12px;
  color: #fff;
  background: linear-gradient(145deg, #7869ec, #5141bf);
  font-size: 14px;
  font-weight: 900;
}

.task-title h2,
.task-title p {
  margin: 0;
}

.task-title h2 {
  color: #2e3a4f;
  font-size: 14px;
}

.task-title p {
  margin-top: 4px;
  color: #8b95a5;
  font-size: 9px;
}

.role-block {
  display: grid;
  gap: 7px;
  border-radius: 11px;
  padding: 11px;
  background: #f8f7ff;
}

.role-block > span,
.role-block small {
  color: #8178a8;
  font-size: 9px;
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
  font-size: 9px;
}

.role-block small {
  line-height: 1.5;
}

.task-card dl {
  display: grid;
  gap: 9px;
  margin: 0;
}

.task-card dl > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px dashed #e8ebf1;
  padding-bottom: 9px;
}

.task-card dt,
.task-card dd {
  margin: 0;
  font-size: 9px;
}

.task-card dt {
  color: #8a94a5;
}

.task-card dd {
  overflow: hidden;
  color: #4b586d;
  font-weight: 800;
  text-align: right;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.task-card__footer {
  margin-top: auto;
  border-top: 1px solid #eef0f5;
  padding-top: 13px;
}

.task-card__footer .button {
  display: inline-flex;
  width: 100%;
  align-items: center;
  justify-content: center;
}

@media (max-width: 1180px) {
  .student-task-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 720px) {
  .student-task-grid {
    grid-template-columns: 1fr;
  }

  .task-toolbar,
  .student-session {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
