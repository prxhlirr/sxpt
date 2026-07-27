<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import MetricCard from '../../components/ui/MetricCard.vue';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusPill from '../../components/ui/StatusPill.vue';
import { useTrainingStore } from '../../stores/trainingStore';

const store = useTrainingStore();
store.refreshPublishedTaskStatuses();

const activeLesson = computed(
  () =>
    store.state.lessons.find((lesson) => lesson.status !== 'ARCHIVED') ??
    store.state.lessons[0]
);
const lessonId = computed(() => activeLesson.value?.id ?? '');
const publishedLessons = computed(
  () =>
    store.state.lessons.filter((lesson) => lesson.status === 'PUBLISHED').length
);
const readyData = computed(() =>
  Object.values(store.state.dataItems)
    .flat()
    .filter((item) => item.status === 'READY' || item.status === 'IN_USE')
);
const assignedStudents = computed(
  () => new Set(store.state.studentTasks.map((task) => task.studentId)).size
);
const submittedTasks = computed(
  () =>
    store.state.studentTasks.filter(
      (task) => task.status === 'SUBMITTED' || task.status === 'GRADED'
    ).length
);

const chain = computed(() => {
  if (!activeLesson.value) return [];
  const id = activeLesson.value.id;
  const exam = store.state.examSettings[id];
  const groups = store.state.groupPlans[id];
  const data = store.state.dataItems[id] ?? [];
  const published = store.state.publishedTasks.find(
    (task) => task.lessonId === id
  );
  return [
    {
      index: '01',
      title: '教案编排',
      description: `${activeLesson.value.stages.length} 个业务阶段 · ${activeLesson.value.version}.0 版`,
      done: activeLesson.value.status === 'PUBLISHED',
      label:
        activeLesson.value.status === 'PUBLISHED' ? '已发布' : '待完善',
      to: `/admin/lessons/${id}/editor`
    },
    {
      index: '02',
      title: '考试设置',
      description: exam
        ? `${exam.durationMinutes} 分钟 · 最多 ${exam.maxAttempts} 次`
        : '配置批次、时限与评分权重',
      done: Boolean(exam),
      label: exam ? '已配置' : '待配置',
      to: `/admin/lessons/${id}/exam`
    },
    {
      index: '03',
      title: '动态分组',
      description: groups
        ? `${groups.roles.length} 个角色组 · ${groups.members.length} 条成员分配`
        : '按业务阶段设置多角色协作',
      done: Boolean(groups?.roles.length && groups?.members.length),
      label:
        groups?.roles.length && groups?.members.length ? '已分组' : '待分组',
      to: `/admin/lessons/${id}/groups`
    },
    {
      index: '04',
      title: '考试数据',
      description: data.length
        ? `${data.filter((item) => item.status === 'READY').length} 条可用 / ${data.length} 条已生成`
        : '通过业务方接口生成正式与备用数据',
      done: data.some((item) => item.status === 'READY'),
      label: data.some((item) => item.status === 'READY')
        ? '数据就绪'
        : '待生成',
      to: `/admin/lessons/${id}/data`
    },
    {
      index: '05',
      title: '发布任务',
      description: published
        ? `${published.assignedCount} 人已接收 · ${published.completedCount} 人完成`
        : '校验所有配置并生成学员任务',
      done: Boolean(published),
      label: published ? '已发布' : '待发布',
      to: `/admin/lessons/${id}/publish`
    }
  ];
});

const lessonStatusLabel: Record<string, string> = {
  DRAFT: '草稿',
  RECORDED: '已录制',
  PUBLISHED: '已发布',
  ARCHIVED: '已归档'
};

function formatTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value));
}

function resetDemo() {
  if (!window.confirm('确认恢复为初始 Mock 演示数据吗？当前页面中的演示操作将被清除。')) {
    return;
  }
  store.resetDemo();
}
</script>

<template>
  <section class="page admin-overview">
    <PageHeader
      eyebrow="ADMIN COMMAND CENTER"
      title="实训平台运营总览"
      description="从录制教案到考试发布，以同一条业务链检查编排、分组、业务数据和学员任务的就绪状态。"
    >
      <button class="secondary" type="button" @click="resetDemo">
        恢复演示数据
      </button>
      <RouterLink class="button primary" to="/admin/lessons">
        进入教案管理
      </RouterLink>
    </PageHeader>

    <div class="metric-grid">
      <MetricCard
        label="教案总数"
        :value="store.state.lessons.length"
        :hint="`${publishedLessons} 个已发布`"
        tone="purple"
      >
        <template #icon>案</template>
      </MetricCard>
      <MetricCard
        label="已发布考试"
        :value="store.state.publishedTasks.length"
        hint="跨角色协同任务"
        tone="blue"
      >
        <template #icon>考</template>
      </MetricCard>
      <MetricCard
        label="覆盖学员"
        :value="assignedStudents"
        :hint="`${submittedTasks} 份已提交或评分`"
        tone="green"
      >
        <template #icon>员</template>
      </MetricCard>
      <MetricCard
        label="可用业务数据"
        :value="readyData.length"
        hint="正式数据与备用数据"
        tone="amber"
      >
        <template #icon>数</template>
      </MetricCard>
    </div>

    <div v-if="activeLesson" class="overview-layout">
      <article class="card chain-card">
        <div class="card-header">
          <div>
            <span class="card-kicker">CURRENT DELIVERY PIPELINE</span>
            <h2>{{ activeLesson.title }}</h2>
            <p>
              {{ activeLesson.moduleName }} · {{ activeLesson.code }} ·
              业务链可逐步进入、修改并重新校验
            </p>
          </div>
          <StatusPill
            :status="activeLesson.status"
            :label="lessonStatusLabel[activeLesson.status]"
          />
        </div>
        <div class="chain-list">
          <RouterLink
            v-for="item in chain"
            :key="item.index"
            class="chain-step"
            :class="{ complete: item.done }"
            :to="item.to"
          >
            <span class="chain-step__index">{{ item.done ? '✓' : item.index }}</span>
            <span class="chain-step__body">
              <strong>{{ item.title }}</strong>
              <small>{{ item.description }}</small>
            </span>
            <span class="chain-step__state">{{ item.label }}</span>
            <span class="chain-step__arrow">→</span>
          </RouterLink>
        </div>
      </article>

      <aside class="overview-side">
        <article class="card quick-card">
          <div class="card-header">
            <div>
              <h2>快捷工作台</h2>
              <p>继续当前教案的关键配置</p>
            </div>
          </div>
          <div class="quick-grid">
            <RouterLink
              :to="`/admin/lessons/${lessonId}/recording`"
              class="quick-action"
            >
              <span>▶</span>
              <strong>查看录制教案</strong>
              <small>回看页面与操作步骤</small>
            </RouterLink>
            <RouterLink
              :to="`/admin/lessons/${lessonId}/groups`"
              class="quick-action"
            >
              <span>组</span>
              <strong>配置多角色</strong>
              <small>支持任意组和同人多角色</small>
            </RouterLink>
            <RouterLink
              :to="`/admin/lessons/${lessonId}/data`"
              class="quick-action"
            >
              <span>数</span>
              <strong>生成考试数据</strong>
              <small>按单位生成正式与备用数据</small>
            </RouterLink>
            <RouterLink
              :to="`/admin/lessons/${lessonId}/publish`"
              class="quick-action"
            >
              <span>发</span>
              <strong>发布考试任务</strong>
              <small>就绪检查与任务下发</small>
            </RouterLink>
          </div>
        </article>

        <article class="card activity-card">
          <div class="card-header">
            <div>
              <h2>最近动态</h2>
              <p>Mock 业务链操作留痕</p>
            </div>
          </div>
          <div v-if="store.state.activities.length" class="activity-list">
            <div
              v-for="event in store.state.activities.slice(0, 5)"
              :key="event.id"
              class="activity-item"
            >
              <span></span>
              <div>
                <strong>{{ event.title }}</strong>
                <p>{{ event.detail }}</p>
                <small>{{ formatTime(event.at) }}</small>
              </div>
            </div>
          </div>
          <div v-else class="empty-mini">暂时没有操作记录</div>
        </article>
      </aside>
    </div>

    <article class="card lesson-table-card">
      <div class="card-header">
        <div>
          <h2>最近教案</h2>
          <p>快速查看当前编排和发布状态</p>
        </div>
        <RouterLink class="button secondary" to="/admin/lessons">
          查看全部
        </RouterLink>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>教案</th>
              <th>业务模块</th>
              <th>阶段</th>
              <th>客观 / 主观分</th>
              <th>状态</th>
              <th>最近更新</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="lesson in store.state.lessons.slice(0, 5)" :key="lesson.id">
              <td>
                <div class="lesson-name">
                  <strong>{{ lesson.title }}</strong>
                  <small>{{ lesson.code }} · V{{ lesson.version }}.0</small>
                </div>
              </td>
              <td>{{ lesson.moduleName }}</td>
              <td>{{ lesson.stages.length }} 个</td>
              <td>
                {{ lesson.objectiveMaxScore }} /
                {{ lesson.subjectiveMaxScore }}
              </td>
              <td>
                <StatusPill
                  :status="lesson.status"
                  :label="lessonStatusLabel[lesson.status]"
                />
              </td>
              <td>{{ formatTime(lesson.updatedAt) }}</td>
              <td>
                <RouterLink
                  class="table-link"
                  :to="`/admin/lessons/${lesson.id}/editor`"
                >
                  继续编排 →
                </RouterLink>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </article>
  </section>
</template>

<style scoped>
.overview-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.75fr);
  gap: 16px;
  margin-top: 16px;
}

.card-kicker {
  display: block;
  margin-bottom: 6px;
  color: var(--purple);
  font-size: 9px;
  font-weight: 900;
  letter-spacing: 0.14em;
}

.chain-list {
  display: grid;
  padding: 8px 18px 16px;
}

.chain-step {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto 20px;
  align-items: center;
  gap: 13px;
  min-height: 76px;
  border-bottom: 1px solid #eef0f5;
}

.chain-step:last-child {
  border-bottom: 0;
}

.chain-step__index {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border: 1px solid #dfe3ec;
  border-radius: 11px;
  color: #8993a5;
  background: #f8f9fc;
  font-size: 11px;
  font-weight: 900;
}

.chain-step.complete .chain-step__index {
  border-color: #c7ebdd;
  color: #07855e;
  background: #eaf8f3;
}

.chain-step__body {
  display: grid;
  gap: 5px;
}

.chain-step__body strong {
  color: #29354a;
  font-size: 13px;
}

.chain-step__body small {
  color: #8791a3;
  font-size: 10px;
}

.chain-step__state {
  border-radius: 999px;
  padding: 5px 9px;
  color: #8a6a29;
  background: #fff4df;
  font-size: 9px;
  font-weight: 800;
  white-space: nowrap;
}

.chain-step.complete .chain-step__state {
  color: #087d5b;
  background: #e9f8f2;
}

.chain-step__arrow {
  color: #a0a8b7;
  transition: transform 150ms ease;
}

.chain-step:hover .chain-step__arrow {
  transform: translateX(3px);
  color: var(--purple);
}

.overview-side {
  display: grid;
  gap: 16px;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding: 14px;
}

.quick-action {
  display: grid;
  gap: 5px;
  min-height: 112px;
  align-content: center;
  border: 1px solid #e8eaf1;
  border-radius: 12px;
  padding: 13px;
  background: #fbfbfd;
  transition:
    border-color 150ms ease,
    transform 150ms ease,
    box-shadow 150ms ease;
}

.quick-action:hover {
  transform: translateY(-2px);
  border-color: #cfc9ff;
  box-shadow: 0 10px 22px rgb(42 37 98 / 8%);
}

.quick-action > span {
  display: grid;
  width: 29px;
  height: 29px;
  place-items: center;
  border-radius: 9px;
  color: #6554da;
  background: #efedff;
  font-size: 11px;
  font-weight: 900;
}

.quick-action strong {
  color: #344056;
  font-size: 11px;
}

.quick-action small {
  color: #8a94a6;
  font-size: 9px;
  line-height: 1.5;
}

.activity-list {
  display: grid;
  padding: 16px 18px;
}

.activity-item {
  display: grid;
  grid-template-columns: 9px minmax(0, 1fr);
  gap: 10px;
  padding-bottom: 15px;
}

.activity-item:last-child {
  padding-bottom: 0;
}

.activity-item > span {
  width: 7px;
  height: 7px;
  margin-top: 5px;
  border: 2px solid #c9c3ff;
  border-radius: 50%;
  background: #fff;
}

.activity-item > div {
  display: grid;
  gap: 3px;
}

.activity-item strong {
  color: #39465b;
  font-size: 10px;
}

.activity-item p,
.activity-item small {
  margin: 0;
  color: #8b95a6;
  font-size: 9px;
  line-height: 1.45;
}

.empty-mini {
  padding: 28px 18px;
  color: #939cad;
  font-size: 11px;
  text-align: center;
}

.lesson-table-card {
  margin-top: 16px;
}

.lesson-name {
  display: grid;
  gap: 4px;
}

.lesson-name strong {
  color: #354158;
  font-size: 12px;
}

.lesson-name small {
  color: #8e97a7;
  font-size: 9px;
}

.table-link {
  color: #5f4fe0;
  font-size: 10px;
  font-weight: 800;
  white-space: nowrap;
}

@media (max-width: 1180px) {
  .overview-layout {
    grid-template-columns: 1fr;
  }

  .overview-side {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 760px) {
  .overview-side,
  .quick-grid {
    grid-template-columns: 1fr;
  }

  .chain-step {
    grid-template-columns: 38px minmax(0, 1fr) 18px;
  }

  .chain-step__state {
    display: none;
  }
}
</style>
