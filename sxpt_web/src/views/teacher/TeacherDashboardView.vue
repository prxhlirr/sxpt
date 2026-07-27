<script setup lang="ts">
import { computed, ref } from 'vue';
import { RouterLink } from 'vue-router';
import MetricCard from '../../components/ui/MetricCard.vue';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusPill from '../../components/ui/StatusPill.vue';
import { useTrainingStore } from '../../stores/trainingStore';

const store = useTrainingStore();
store.refreshPublishedTaskStatuses();
const selectedPublishedTaskId = ref(
  store.state.publishedTasks.find((task) => task.status !== 'FINISHED')?.id ??
    store.state.publishedTasks[0]?.id ??
    ''
);
const statusFilter = ref('ALL');

const selectedPublishedTask = computed(() =>
  store.state.publishedTasks.find(
    (task) => task.id === selectedPublishedTaskId.value
  )
);
const lesson = computed(() =>
  store.state.lessons.find(
    (item) => item.id === selectedPublishedTask.value?.lessonId
  )
);
const groupPlan = computed(() =>
  selectedPublishedTask.value
    ? store.state.groupPlans[selectedPublishedTask.value.lessonId]
    : undefined
);
const allStudentTasks = computed(() =>
  store.state.studentTasks.filter(
    (task) => task.publishedTaskId === selectedPublishedTaskId.value
  )
);
const visibleStudentTasks = computed(() =>
  allStudentTasks.value.filter(
    (task) =>
      statusFilter.value === 'ALL' || task.status === statusFilter.value
  )
);
const submittedCount = computed(
  () =>
    allStudentTasks.value.filter(
      (task) => task.status === 'SUBMITTED' || task.status === 'GRADED'
    ).length
);
const doingCount = computed(
  () =>
    allStudentTasks.value.filter((task) => task.status === 'DOING').length
);
const completionRate = computed(() => {
  if (!allStudentTasks.value.length) return 0;
  return Math.round(
    (submittedCount.value / allStudentTasks.value.length) * 100
  );
});

const statusLabel: Record<string, string> = {
  TODO: '未开始',
  DOING: '办理中',
  SUBMITTED: '待评阅',
  GRADED: '已评分'
};

function taskGroups(groupKeys: string[]) {
  return groupKeys.map((key) => {
    const role = groupPlan.value?.roles.find((item) => item.key === key);
    return role?.name ?? key;
  });
}

function assignedStages(task: (typeof allStudentTasks.value)[number]) {
  const stageIds = new Set(
    groupPlan.value?.roles
      .filter((role) => task.groupKeys.includes(role.key))
      .flatMap((role) => role.stageIds) ?? []
  );
  return (
    lesson.value?.stages.filter(
      (stage) =>
        stageIds.has(stage.id) || task.groupKeys.includes(stage.groupKey)
    ) ?? []
  );
}

function currentStageName(task: (typeof allStudentTasks.value)[number]) {
  return (
    assignedStages(task).find(
      (stage) => !task.completedStageIds.includes(stage.id)
    )?.name ??
    (task.status === 'SUBMITTED' || task.status === 'GRADED'
      ? '本人阶段已提交'
      : '等待提交')
  );
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}
</script>

<template>
  <section class="page teacher-dashboard">
    <PageHeader
      eyebrow="TEACHER CONTROL DESK"
      title="考试全过程监控"
      description="教师只监控实训平台记录到的页面访问、流程操作与提交状态；具体业务数据关联由平台会话轨迹推断，并以流程性操作作为客观完成依据。"
    >
      <label v-if="store.state.publishedTasks.length" class="batch-select">
        <span>当前考试批次</span>
        <select v-model="selectedPublishedTaskId">
          <option
            v-for="task in store.state.publishedTasks"
            :key="task.id"
            :value="task.id"
          >
            {{ task.title }}
          </option>
        </select>
      </label>
      <RouterLink class="button primary" to="/teacher/review">
        进入主观评阅
      </RouterLink>
    </PageHeader>

    <template v-if="selectedPublishedTask">
      <div class="monitor-banner">
        <div>
          <span class="live-dot"></span>
          <strong>{{ selectedPublishedTask.title }}</strong>
          <small>
            {{ formatTime(selectedPublishedTask.startAt) }} —
            {{ formatTime(selectedPublishedTask.endAt) }}
          </small>
        </div>
        <StatusPill
          :status="selectedPublishedTask.status"
          :label="
            selectedPublishedTask.status === 'RUNNING'
              ? '考试进行中'
              : selectedPublishedTask.status === 'SCHEDULED'
                ? '等待开始'
                : '考试已结束'
          "
        />
      </div>

      <div class="metric-grid">
        <MetricCard
          label="已分配学员"
          :value="allStudentTasks.length"
          :hint="`${selectedPublishedTask.groupCount} 个业务角色组`"
          tone="purple"
        >
          <template #icon>员</template>
        </MetricCard>
        <MetricCard
          label="办理中"
          :value="doingCount"
          hint="正在访问或执行业务流程"
          tone="blue"
        >
          <template #icon>办</template>
        </MetricCard>
        <MetricCard
          label="已提交"
          :value="submittedCount"
          :hint="`${completionRate}% 总体完成率`"
          tone="green"
        >
          <template #icon>交</template>
        </MetricCard>
        <MetricCard
          label="待主观评阅"
          :value="
            allStudentTasks.filter((task) => task.status === 'SUBMITTED').length
          "
          hint="系统客观分已自动生成"
          tone="amber"
        >
          <template #icon>评</template>
        </MetricCard>
      </div>

      <div class="monitor-grid">
        <article class="card student-monitor">
          <div class="card-header">
            <div>
              <h2>学员实时状态</h2>
              <p>按学员会话查看角色、阶段与平台轨迹判定结果</p>
            </div>
            <div class="status-tabs" role="tablist">
              <button
                v-for="option in [
                  ['ALL', '全部'],
                  ['TODO', '未开始'],
                  ['DOING', '办理中'],
                  ['SUBMITTED', '待评阅'],
                  ['GRADED', '已评分']
                ]"
                :key="option[0]"
                type="button"
                :class="{ active: statusFilter === option[0] }"
                @click="statusFilter = option[0]"
              >
                {{ option[1] }}
              </button>
            </div>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>学员</th>
                  <th>承担角色</th>
                  <th>当前业务阶段</th>
                  <th>流程完成度</th>
                  <th>状态</th>
                  <th>作答</th>
                  <th>客观分</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="task in visibleStudentTasks" :key="task.id">
                  <td>
                    <div class="student-cell">
                      <span>{{ task.studentName.slice(0, 1) }}</span>
                      <div>
                        <strong>{{ task.studentName }}</strong>
                        <small>{{ task.studentId }}</small>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div class="role-tags">
                      <span
                        v-for="name in taskGroups(task.groupKeys)"
                        :key="name"
                      >
                        {{ name }}
                      </span>
                    </div>
                  </td>
                  <td>{{ currentStageName(task) }}</td>
                  <td>
                    <div class="task-progress">
                      <span>
                        {{
                          assignedStages(task).filter((stage) =>
                            task.completedStageIds.includes(stage.id)
                          ).length
                        }}
                        / {{ assignedStages(task).length }}
                      </span>
                      <i>
                        <b
                          :style="{
                            width: `${
                              assignedStages(task).length
                                ? (assignedStages(task).filter((stage) =>
                                    task.completedStageIds.includes(stage.id)
                                  ).length /
                                    assignedStages(task).length) *
                                  100
                                : 0
                            }%`
                          }"
                        ></b>
                      </i>
                    </div>
                  </td>
                  <td>
                    <StatusPill
                      :status="task.status"
                      :label="statusLabel[task.status]"
                    />
                  </td>
                  <td>
                    第 {{ task.attemptNumber }} /
                    {{
                      store.state.examSettings[task.lessonId]?.maxAttempts ?? 1
                    }}
                    次
                  </td>
                  <td>
                    <strong class="score-value">
                      {{ task.objectiveScore ?? '—' }}
                    </strong>
                  </td>
                </tr>
                <tr v-if="!visibleStudentTasks.length">
                  <td colspan="7">
                    <div class="empty-table">当前筛选条件下暂无学员</div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </article>

        <aside class="monitor-side">
          <article class="card group-progress-card">
            <div class="card-header">
              <div>
                <h2>角色组进度</h2>
                <p>多组串行交接概览</p>
              </div>
            </div>
            <div class="group-progress-list">
              <div
                v-for="role in groupPlan?.roles ?? []"
                :key="role.key"
                class="group-progress-item"
              >
                <span
                  class="group-color"
                  :style="{ background: role.color }"
                ></span>
                <div>
                  <strong>{{ role.name }}</strong>
                  <small>
                    {{
                      allStudentTasks.filter((task) =>
                        task.groupKeys.includes(role.key)
                      ).length
                    }}
                    人 · 负责 {{ role.stageIds.length }} 个阶段
                  </small>
                </div>
                <b>
                  {{
                    allStudentTasks.filter(
                      (task) =>
                        task.groupKeys.includes(role.key) &&
                        (task.status === 'SUBMITTED' ||
                          task.status === 'GRADED')
                    ).length
                  }}
                </b>
              </div>
            </div>
          </article>

          <article class="card trace-card">
            <div class="card-header">
              <div>
                <h2>实时平台事件</h2>
                <p>仅展示实训平台可采集的行为</p>
              </div>
            </div>
            <div class="trace-list">
              <div
                v-for="event in store.state.activities.slice(0, 7)"
                :key="event.id"
              >
                <span></span>
                <p>
                  <strong>{{ event.title }}</strong>
                  {{ event.detail }}
                  <small>{{ formatTime(event.at) }}</small>
                </p>
              </div>
              <p v-if="!store.state.activities.length" class="empty-trace">
                暂无事件
              </p>
            </div>
          </article>
        </aside>
      </div>
    </template>

    <article v-else class="card empty-state">
      <div>
        <strong>尚未发布考试任务</strong>
        <p>请先由管理员完成教案、考试、分组和考试数据配置。</p>
        <RouterLink class="button primary" to="/admin/lessons">
          前往后台配置
        </RouterLink>
      </div>
    </article>
  </section>
</template>

<style scoped>
.batch-select {
  display: flex;
  align-items: center;
  gap: 9px;
}

.batch-select span {
  white-space: nowrap;
}

.batch-select select {
  min-width: 240px;
}

.monitor-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 14px;
  border: 1px solid #dcd8ff;
  border-radius: 13px;
  padding: 13px 16px;
  background: linear-gradient(90deg, #f6f4ff, #fff);
}

.monitor-banner > div {
  display: flex;
  align-items: center;
  gap: 9px;
}

.monitor-banner strong {
  font-size: 12px;
}

.monitor-banner small {
  color: #8791a2;
  font-size: 10px;
}

.live-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #17a779;
  box-shadow: 0 0 0 5px rgb(23 167 121 / 12%);
}

.monitor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(300px, 0.7fr);
  gap: 16px;
  margin-top: 16px;
}

.status-tabs {
  display: flex;
  gap: 3px;
  padding: 3px;
  border-radius: 9px;
  background: #f3f5f8;
}

.status-tabs button {
  min-height: 30px;
  border: 0;
  padding: 0 9px;
  background: transparent;
  color: #7a8495;
  font-size: 9px;
}

.status-tabs button.active {
  background: #fff;
  color: #5e4cdd;
  box-shadow: 0 2px 8px rgb(32 37 70 / 8%);
}

.student-cell {
  display: flex;
  align-items: center;
  gap: 9px;
}

.student-cell > span {
  display: grid;
  width: 30px;
  height: 30px;
  place-items: center;
  border-radius: 9px;
  color: #5c4cd0;
  background: #efedff;
  font-size: 10px;
  font-weight: 900;
}

.student-cell div {
  display: grid;
  gap: 2px;
}

.student-cell strong {
  font-size: 11px;
}

.student-cell small {
  color: #929baa;
  font-size: 9px;
}

.role-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.role-tags span {
  border-radius: 6px;
  padding: 4px 6px;
  color: #6253cc;
  background: #f0edff;
  font-size: 9px;
  font-weight: 800;
}

.task-progress {
  display: grid;
  gap: 5px;
  min-width: 86px;
}

.task-progress span {
  color: #7e8899;
  font-size: 9px;
}

.task-progress i {
  display: block;
  height: 4px;
  overflow: hidden;
  border-radius: 6px;
  background: #eceff4;
}

.task-progress b {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #7161eb, #9a8fff);
}

.score-value {
  color: #4b3dc1;
  font-size: 14px;
}

.empty-table {
  padding: 25px;
  color: #8e98a8;
  text-align: center;
}

.monitor-side {
  display: grid;
  align-content: start;
  gap: 16px;
}

.group-progress-list,
.trace-list {
  display: grid;
  padding: 14px 17px;
}

.group-progress-item {
  display: grid;
  grid-template-columns: 9px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  border-bottom: 1px solid #eef1f5;
  padding: 12px 0;
}

.group-progress-item:last-child {
  border-bottom: 0;
}

.group-color {
  width: 7px;
  height: 30px;
  border-radius: 5px;
}

.group-progress-item div {
  display: grid;
  gap: 4px;
}

.group-progress-item strong {
  font-size: 11px;
}

.group-progress-item small {
  color: #8a94a5;
  font-size: 9px;
}

.group-progress-item b {
  display: grid;
  width: 27px;
  height: 27px;
  place-items: center;
  border-radius: 8px;
  color: #0d815e;
  background: #eaf8f3;
  font-size: 10px;
}

.trace-list > div {
  display: grid;
  grid-template-columns: 7px minmax(0, 1fr);
  gap: 9px;
  padding: 8px 0;
}

.trace-list > div > span {
  width: 6px;
  height: 6px;
  margin-top: 5px;
  border-radius: 50%;
  background: #8d80f8;
}

.trace-list p {
  display: grid;
  gap: 3px;
  margin: 0;
  color: #687487;
  font-size: 9px;
  line-height: 1.45;
}

.trace-list strong {
  color: #374359;
  font-size: 10px;
}

.trace-list small {
  color: #9aa3b1;
}

.empty-trace {
  padding: 20px 0;
  text-align: center;
}

@media (max-width: 1180px) {
  .monitor-grid {
    grid-template-columns: 1fr;
  }

  .monitor-side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .monitor-side {
    grid-template-columns: 1fr;
  }

  .monitor-banner,
  .monitor-banner > div {
    align-items: flex-start;
    flex-direction: column;
  }

  .batch-select {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
