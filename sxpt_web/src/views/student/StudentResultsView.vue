<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusPill from '../../components/ui/StatusPill.vue';
import { useTrainingStore } from '../../stores/trainingStore';

const store = useTrainingStore();

const learners = computed(() => {
  const map = new Map<string, string>();
  store.state.studentTasks.forEach((task) =>
    map.set(task.studentId, task.studentName)
  );
  return [...map.entries()].map(([id, name]) => ({ id, name }));
});
const selectedStudentId = ref(
  store.state.studentTasks.find(
    (task) => task.status === 'SUBMITTED' || task.status === 'GRADED'
  )?.studentId ??
    learners.value[0]?.id ??
    ''
);
watch(
  learners,
  (items) => {
    if (!items.some((item) => item.id === selectedStudentId.value)) {
      selectedStudentId.value = items[0]?.id ?? '';
    }
  },
  { immediate: true }
);

const resultTasks = computed(() =>
  store.state.studentTasks.filter(
    (task) =>
      task.studentId === selectedStudentId.value &&
      (task.status === 'SUBMITTED' || task.status === 'GRADED')
  )
);
const gradedTasks = computed(() =>
  resultTasks.value.filter((task) => task.status === 'GRADED')
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

function lessonFor(lessonId: string) {
  return store.state.lessons.find((lesson) => lesson.id === lessonId);
}

function roleNames(lessonId: string, keys: string[]) {
  const roles = store.state.groupPlans[lessonId]?.roles ?? [];
  return keys.map(
    (key) => roles.find((role) => role.key === key)?.name ?? key
  );
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
</script>

<template>
  <section class="page student-results">
    <PageHeader
      eyebrow="RESULTS & FEEDBACK"
      title="成绩与教师反馈"
      description="客观分由平台根据流程性操作自动计算；主观分和批语由教师评阅后推送。两部分共同构成最终考试结果。"
    >
      <label v-if="learners.length > 1" class="result-student-select">
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

    <div class="result-summary">
      <div>
        <span>已提交考试</span>
        <strong>{{ resultTasks.length }}</strong>
        <small>次</small>
      </div>
      <div>
        <span>已完成评分</span>
        <strong>{{ gradedTasks.length }}</strong>
        <small>次</small>
      </div>
      <div>
        <span>平均总分</span>
        <strong>{{ averageScore }}</strong>
        <small>分</small>
      </div>
      <div>
        <span>结果构成</span>
        <strong>客观 + 主观</strong>
        <small>双轨评分</small>
      </div>
    </div>

    <div v-if="resultTasks.length" class="result-list">
      <article v-for="task in resultTasks" :key="task.id" class="result-card">
        <header>
          <div>
            <span class="result-kicker">TRAINING RESULT</span>
            <h2>{{ task.title }}</h2>
            <p>
              {{ lessonFor(task.lessonId)?.moduleName }} ·
              <b
                v-for="role in roleNames(task.lessonId, task.groupKeys)"
                :key="role"
              >
                {{ role }}
              </b>
              · 第 {{ task.attemptNumber }} 次作答
            </p>
          </div>
          <StatusPill
            :status="task.status"
            :label="task.status === 'GRADED' ? '评分完成' : '等待教师评分'"
          />
        </header>

        <div class="result-score-grid">
          <div class="objective">
            <span>系统客观分</span>
            <strong>{{ task.objectiveScore ?? 0 }}</strong>
            <small>/ {{ objectiveMax(task.lessonId) }}</small>
            <p>依据页面访问、流程按钮和阶段时序自动评分</p>
          </div>
          <div class="subjective">
            <span>教师主观分</span>
            <strong>{{ task.subjectiveScore ?? '—' }}</strong>
            <small>/ {{ subjectiveMax(task.lessonId) }}</small>
            <p>
              {{
                task.status === 'GRADED'
                  ? '依据操作规范与说明完整度评分'
                  : '教师正在评阅，请稍后查看'
              }}
            </p>
          </div>
          <div class="total">
            <span>最终总分</span>
            <strong>
              {{
                task.status === 'GRADED'
                  ? (task.objectiveScore ?? 0) + (task.subjectiveScore ?? 0)
                  : '—'
              }}
            </strong>
            <small>
              /
              {{
                objectiveMax(task.lessonId) + subjectiveMax(task.lessonId)
              }}
            </small>
            <p>客观分与教师主观分合计</p>
          </div>
        </div>

        <div class="result-detail-grid">
          <section>
            <h3>流程完成记录</h3>
            <div class="completed-stage-list">
              <div
                v-for="(stage, index) in lessonFor(task.lessonId)?.stages ?? []"
                :key="stage.id"
                :class="{ complete: task.completedStageIds.includes(stage.id) }"
              >
                <span>
                  {{
                    task.completedStageIds.includes(stage.id) ? '✓' : index + 1
                  }}
                </span>
                <p>
                  <strong>{{ stage.name }}</strong>
                  <small>
                    {{
                      task.completedStageIds.includes(stage.id)
                        ? '已记录流程性操作'
                        : '由其他角色办理或未完成'
                    }}
                  </small>
                </p>
              </div>
            </div>
          </section>
          <section class="teacher-comment">
            <h3>教师批语</h3>
            <blockquote v-if="task.comment">
              “{{ task.comment }}”
            </blockquote>
            <div v-else class="pending-comment">
              <span>评</span>
              <p>
                <strong>等待教师完成主观评阅</strong>
                <small>评分完成后，批语会自动推送到这里。</small>
              </p>
            </div>
            <p class="score-explain">
              <strong>评分说明</strong>
              实训平台无法可靠绑定具体业务数据，本次客观成绩仅证明学员完成了预设流程操作；教师可通过主观评分补充评价业务质量。
            </p>
            <div
              v-if="Object.keys(task.submissionValues).length"
              class="submitted-values"
            >
              <strong>我的提交信息</strong>
              <p
                v-for="field in store.state.examSettings[task.lessonId]
                  ?.submissionFields ?? []"
                :key="field.key"
              >
                <span>{{ field.label }}</span>
                <b>{{ task.submissionValues[field.key] || '未填写' }}</b>
              </p>
            </div>
          </section>
        </div>
      </article>
    </div>

    <article v-else class="card empty-state">
      <div>
        <strong>暂无可查看的考试结果</strong>
        <p>完成并提交学员任务后，系统客观评分会立即出现在这里。</p>
        <RouterLink class="button primary" to="/student/tasks">
          返回我的任务
        </RouterLink>
      </div>
    </article>
  </section>
</template>

<style scoped>
.result-student-select {
  display: flex;
  align-items: center;
  gap: 9px;
}

.result-student-select span {
  white-space: nowrap;
}

.result-student-select select {
  min-width: 220px;
}

.result-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 16px;
  overflow: hidden;
  border: 1px solid #e4e7ee;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 7px 22px rgb(31 37 77 / 4%);
}

.result-summary > div {
  display: flex;
  align-items: baseline;
  gap: 7px;
  border-right: 1px solid #edf0f4;
  padding: 17px 19px;
}

.result-summary > div:last-child {
  border-right: 0;
}

.result-summary span,
.result-summary small {
  color: #8993a4;
  font-size: 9px;
}

.result-summary strong {
  color: #5544c8;
  font-size: 18px;
}

.result-list {
  display: grid;
  gap: 17px;
}

.result-card {
  overflow: hidden;
  border: 1px solid #e4e7ef;
  border-radius: 15px;
  background: #fff;
  box-shadow: 0 10px 28px rgb(31 37 78 / 5%);
}

.result-card > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border-bottom: 1px solid #eceff4;
  padding: 17px 20px;
}

.result-kicker {
  color: var(--purple);
  font-size: 8px;
  font-weight: 900;
  letter-spacing: 0.15em;
}

.result-card h2,
.result-card header p {
  margin: 0;
}

.result-card h2 {
  margin-top: 5px;
  color: #303c51;
  font-size: 16px;
}

.result-card header p {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 6px;
  color: #8993a4;
  font-size: 9px;
}

.result-card header b {
  border-radius: 5px;
  padding: 3px 5px;
  color: #5e4dc9;
  background: #efedff;
  font-size: 8px;
}

.result-score-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-bottom: 1px solid #eceff4;
  background: #fafafe;
}

.result-score-grid > div {
  display: grid;
  grid-template-columns: auto auto 1fr;
  align-items: baseline;
  gap: 7px;
  border-right: 1px solid #eceff4;
  padding: 20px;
}

.result-score-grid > div:last-child {
  border-right: 0;
}

.result-score-grid span,
.result-score-grid small {
  color: #8993a4;
  font-size: 9px;
}

.result-score-grid strong {
  color: #5b4bd0;
  font-size: 26px;
}

.result-score-grid .subjective strong {
  color: #2580c4;
}

.result-score-grid .total strong {
  color: #07845e;
}

.result-score-grid p {
  grid-column: 1 / -1;
  margin: 3px 0 0;
  color: #969eac;
  font-size: 8px;
}

.result-detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(300px, 0.8fr);
  gap: 26px;
  padding: 20px;
}

.result-detail-grid h3 {
  margin: 0 0 13px;
  color: #3c485d;
  font-size: 11px;
}

.completed-stage-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
}

.completed-stage-list > div {
  display: grid;
  grid-template-columns: 27px minmax(0, 1fr);
  align-items: center;
  gap: 8px;
  border: 1px solid #e7e9ef;
  border-radius: 9px;
  padding: 9px;
  background: #fafbfc;
}

.completed-stage-list > div > span {
  display: grid;
  width: 25px;
  height: 25px;
  place-items: center;
  border-radius: 7px;
  color: #8993a3;
  background: #eef0f4;
  font-size: 8px;
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
  font-size: 9px;
}

.completed-stage-list small,
.pending-comment small {
  color: #8f98a7;
  font-size: 8px;
}

.teacher-comment blockquote {
  margin: 0;
  border-left: 3px solid #8b7df3;
  border-radius: 0 9px 9px 0;
  padding: 14px 16px;
  color: #4e4a66;
  background: #f7f5ff;
  font-size: 11px;
  line-height: 1.8;
}

.pending-comment {
  display: grid;
  grid-template-columns: 33px minmax(0, 1fr);
  align-items: center;
  gap: 9px;
  border: 1px dashed #dfe3ea;
  border-radius: 10px;
  padding: 13px;
  background: #fafbfc;
}

.pending-comment > span {
  display: grid;
  width: 31px;
  height: 31px;
  place-items: center;
  border-radius: 9px;
  color: #9b7027;
  background: #fff2dc;
  font-size: 9px;
  font-weight: 900;
}

.score-explain {
  margin: 12px 0 0;
  border-radius: 9px;
  padding: 10px 12px;
  color: #7d8494;
  background: #f5f6f8;
  font-size: 8px;
  line-height: 1.65;
}

.score-explain strong {
  display: block;
  margin-bottom: 3px;
  color: #555f71;
}

.submitted-values {
  display: grid;
  gap: 7px;
  margin-top: 12px;
  border: 1px solid #e7e9ef;
  border-radius: 9px;
  padding: 11px;
  background: #fafbfc;
}

.submitted-values > strong {
  color: #4b576b;
  font-size: 9px;
}

.submitted-values p {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin: 0;
  font-size: 8px;
}

.submitted-values span {
  color: #8a94a4;
}

.submitted-values b {
  color: #465267;
}

@media (max-width: 950px) {
  .result-summary,
  .result-score-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .result-detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 650px) {
  .result-summary,
  .result-score-grid,
  .completed-stage-list {
    grid-template-columns: 1fr;
  }

  .result-student-select {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
