<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusPill from '../../components/ui/StatusPill.vue';
import { useTrainingStore } from '../../stores/trainingStore';

const store = useTrainingStore();
const filter = ref<'PENDING' | 'GRADED' | 'ALL'>('PENDING');
const reviewFilters: Array<{
  value: 'PENDING' | 'GRADED' | 'ALL';
  label: string;
}> = [
  { value: 'PENDING', label: '待评阅' },
  { value: 'GRADED', label: '已评分' },
  { value: 'ALL', label: '全部' }
];
const selectedTaskId = ref('');
const subjectiveScore = ref(0);
const comment = ref('');
const message = ref('');

const reviewTasks = computed(() =>
  store.state.studentTasks.filter((task) => {
    if (filter.value === 'PENDING') return task.status === 'SUBMITTED';
    if (filter.value === 'GRADED') return task.status === 'GRADED';
    return task.status === 'SUBMITTED' || task.status === 'GRADED';
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
const maxSubjectiveScore = computed(
  () =>
    (selectedTask.value
      ? store.state.examSettings[selectedTask.value.lessonId]?.subjectiveWeight
      : undefined) ??
    lesson.value?.subjectiveMaxScore ??
    0
);
const maxObjectiveScore = computed(
  () =>
    (selectedTask.value
      ? store.state.examSettings[selectedTask.value.lessonId]?.objectiveWeight
      : undefined) ??
    lesson.value?.objectiveMaxScore ??
    0
);
const pendingCount = computed(
  () =>
    store.state.studentTasks.filter((task) => task.status === 'SUBMITTED').length
);

watch(
  reviewTasks,
  (tasks) => {
    if (!tasks.some((task) => task.id === selectedTaskId.value)) {
      selectedTaskId.value = tasks[0]?.id ?? '';
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

function roleNames(groupKeys: string[]) {
  return groupKeys
    .map(
      (key) =>
        groupPlan.value?.roles.find((role) => role.key === key)?.name ?? key
    )
    .join('、');
}

function scoreTask() {
  if (!selectedTask.value) return;
  message.value = '';
  try {
    store.gradeStudentTask(
      selectedTask.value.id,
      Number(subjectiveScore.value),
      comment.value.trim()
    );
    message.value = '评分已保存，学员端现在可以查看客观分、主观分和教师批语。';
  } catch (error) {
    message.value =
      error instanceof Error ? error.message : '评分保存失败，请检查输入。';
  }
}
</script>

<template>
  <section class="page teacher-review">
    <PageHeader
      eyebrow="SUBJECTIVE REVIEW"
      title="主观评分与反馈"
      description="系统保留客观评分，教师补充主观分和批语。完成评分后，结果会立即出现在学员端成绩反馈中。"
    >
      <RouterLink class="button secondary" to="/teacher/dashboard">
        返回考试监控
      </RouterLink>
    </PageHeader>

    <div class="review-summary">
      <div>
        <span>待评阅</span>
        <strong>{{ pendingCount }}</strong>
        <small>份学员答卷</small>
      </div>
      <div>
        <span>已评阅</span>
        <strong>
          {{
            store.state.studentTasks.filter((task) => task.status === 'GRADED')
              .length
          }}
        </strong>
        <small>份结果已推送</small>
      </div>
      <div>
        <span>评分方式</span>
        <strong>双轨</strong>
        <small>系统客观 + 教师主观</small>
      </div>
    </div>

    <div class="review-layout">
      <aside class="card answer-list">
        <div class="card-header">
          <div>
            <h2>学员答卷</h2>
            <p>选择一份答卷进行评分</p>
          </div>
        </div>
        <div class="review-tabs">
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
        <div class="answer-items">
          <button
            v-for="task in reviewTasks"
            :key="task.id"
            type="button"
            :class="{ active: selectedTaskId === task.id }"
            @click="selectedTaskId = task.id"
          >
            <span>{{ task.studentName.slice(0, 1) }}</span>
            <div>
              <strong>{{ task.studentName }}</strong>
              <small>{{ task.title }}</small>
              <em>{{ roleNames(task.groupKeys) }}</em>
            </div>
            <StatusPill
              :status="task.status"
              :label="task.status === 'GRADED' ? '已评分' : '待评阅'"
            />
          </button>
          <div v-if="!reviewTasks.length" class="empty-list">
            当前分类下没有答卷
          </div>
        </div>
      </aside>

      <main v-if="selectedTask && lesson" class="review-main">
        <article class="card evidence-card">
          <div class="card-header">
            <div>
              <span class="review-kicker">PROCESS EVIDENCE</span>
              <h2>{{ selectedTask.studentName }} · {{ lesson.title }}</h2>
              <p>
                {{ roleNames(selectedTask.groupKeys) }} ·
              已完成 {{ selectedTask.completedStageIds.length }} 个教学点
              </p>
            </div>
            <StatusPill
              :status="selectedTask.status"
              :label="selectedTask.status === 'GRADED' ? '已评分' : '待评阅'"
            />
          </div>

          <div class="score-strip">
            <div>
              <span>系统客观分</span>
              <strong>{{ selectedTask.objectiveScore ?? 0 }}</strong>
              <small>/ {{ maxObjectiveScore }}</small>
            </div>
            <div>
              <span>教师主观分</span>
              <strong>{{ selectedTask.subjectiveScore ?? '—' }}</strong>
              <small>/ {{ maxSubjectiveScore }}</small>
            </div>
            <div>
              <span>当前总分</span>
              <strong>
                {{
                  (selectedTask.objectiveScore ?? 0) +
                  (selectedTask.subjectiveScore ?? 0)
                }}
              </strong>
              <small>
                /
                {{ maxObjectiveScore + maxSubjectiveScore }}
              </small>
            </div>
          </div>

          <div class="evidence-timeline">
            <div
              v-for="(stage, index) in lesson.stages"
              :key="stage.id"
              :class="{
                complete: selectedTask.completedStageIds.includes(stage.id)
              }"
            >
              <span>
                {{
                  selectedTask.completedStageIds.includes(stage.id)
                    ? '✓'
                    : index + 1
                }}
              </span>
              <section>
                <strong>{{ stage.name }}</strong>
                <p>{{ stage.description || '按照录制路径完成流程性操作' }}</p>
                <small>
                  {{ stage.recordedSteps.length }} 个录制步骤 ·
                  客观分 {{ stage.score }} ·
                  判定方式 {{ stage.completionMethod }}
                </small>
              </section>
              <StatusPill
                :status="
                  selectedTask.completedStageIds.includes(stage.id)
                    ? 'COMPLETED'
                    : 'TODO'
                "
                :label="
                  selectedTask.completedStageIds.includes(stage.id)
                    ? '轨迹完整'
                    : '未完成'
                "
              />
            </div>
          </div>
          <div class="evidence-note">
            <strong>证据边界说明</strong>
            <p>
              当前 Demo 仅基于学员登录会话、页面访问和流程按钮操作判定完成，不承诺准确识别业务系统中的具体数据编号；教师可结合过程轨迹进行主观评价。
            </p>
          </div>
          <div
            v-if="Object.keys(selectedTask.submissionValues).length"
            class="submission-evidence"
          >
            <strong>学员提交信息</strong>
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
          </div>
        </article>

        <article class="card grading-card">
          <div class="card-header">
            <div>
              <h2>教师评分</h2>
              <p>主观分与批语将推送至学员成绩页</p>
            </div>
          </div>
          <div class="card-body grading-form">
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
            <p
              v-if="message"
              class="notice"
              :class="{ success: selectedTask.status === 'GRADED' }"
            >
              {{ message }}
            </p>
            <div class="button-row">
              <button
                class="primary"
                type="button"
                :disabled="
                  subjectiveScore < 0 ||
                  subjectiveScore > maxSubjectiveScore ||
                  !comment.trim()
                "
                @click="scoreTask"
              >
                保存评分并推送结果
              </button>
            </div>
          </div>
        </article>
      </main>

      <article v-else class="card empty-state review-empty">
        <div>
          <strong>请选择一份学员答卷</strong>
          <p>学员完成流程并提交后，会进入教师主观评阅队列。</p>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.review-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  margin-bottom: 16px;
  overflow: hidden;
  border: 1px solid #e4e7ef;
  border-radius: 14px;
  background: #fff;
}

.review-summary > div {
  display: grid;
  grid-template-columns: auto auto 1fr;
  align-items: baseline;
  gap: 8px;
  border-right: 1px solid #edf0f4;
  padding: 16px 19px;
}

.review-summary > div:last-child {
  border-right: 0;
}

.review-summary span,
.review-summary small {
  color: #8892a3;
  font-size: 10px;
}

.review-summary strong {
  color: #4e3fc7;
  font-size: 20px;
}

.review-layout {
  display: grid;
  grid-template-columns: 310px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.answer-list {
  position: sticky;
  top: 88px;
}

.review-tabs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 4px;
  padding: 10px 12px;
  border-bottom: 1px solid #eef0f5;
}

.review-tabs button {
  min-height: 31px;
  border: 0;
  padding: 0 8px;
  background: #f4f5f8;
  color: #7c8798;
  font-size: 9px;
}

.review-tabs button.active {
  background: #ece9ff;
  color: #5948ce;
}

.answer-items {
  display: grid;
  max-height: calc(100vh - 280px);
  overflow-y: auto;
  padding: 8px;
}

.answer-items > button {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
  min-height: 75px;
  border: 1px solid transparent;
  border-radius: 11px;
  padding: 10px;
  text-align: left;
}

.answer-items > button:hover,
.answer-items > button.active {
  transform: none;
  border-color: #d9d4ff;
  background: #f8f7ff;
  box-shadow: none;
}

.answer-items > button > span {
  display: grid;
  width: 31px;
  height: 31px;
  place-items: center;
  border-radius: 9px;
  color: #5c4bd0;
  background: #ece9ff;
  font-size: 10px;
  font-weight: 900;
}

.answer-items > button > div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.answer-items strong,
.answer-items small,
.answer-items em {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.answer-items strong {
  color: #39455a;
  font-size: 11px;
}

.answer-items small,
.answer-items em {
  color: #8d96a6;
  font-size: 9px;
}

.answer-items em {
  color: #6555cd;
  font-style: normal;
}

.empty-list {
  padding: 32px 12px;
  color: #929baa;
  font-size: 10px;
  text-align: center;
}

.review-main {
  display: grid;
  gap: 16px;
}

.review-kicker {
  display: block;
  margin-bottom: 5px;
  color: var(--purple);
  font-size: 8px;
  font-weight: 900;
  letter-spacing: 0.14em;
}

.score-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  border-bottom: 1px solid #eceff4;
  background: #fafafe;
}

.score-strip > div {
  display: flex;
  align-items: baseline;
  gap: 7px;
  border-right: 1px solid #eceff4;
  padding: 17px 20px;
}

.score-strip > div:last-child {
  border-right: 0;
}

.score-strip span,
.score-strip small {
  color: #8993a4;
  font-size: 9px;
}

.score-strip strong {
  color: #5544ce;
  font-size: 21px;
}

.evidence-timeline {
  display: grid;
  padding: 8px 20px;
}

.evidence-timeline > div {
  display: grid;
  grid-template-columns: 35px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #eef0f5;
  padding: 15px 0;
}

.evidence-timeline > div:last-child {
  border-bottom: 0;
}

.evidence-timeline > div > span {
  display: grid;
  width: 31px;
  height: 31px;
  place-items: center;
  border: 1px solid #dfe3eb;
  border-radius: 9px;
  color: #919aaa;
  background: #f8f9fb;
  font-size: 10px;
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

.evidence-timeline strong {
  font-size: 11px;
}

.evidence-timeline p,
.evidence-timeline small {
  margin: 0;
  color: #8791a3;
  font-size: 9px;
}

.evidence-note {
  margin: 0 20px 20px;
  border: 1px solid #e0dcff;
  border-radius: 10px;
  padding: 11px 13px;
  background: #f8f7ff;
}

.evidence-note strong {
  color: #5a4ac9;
  font-size: 10px;
}

.evidence-note p {
  margin: 5px 0 0;
  color: #756c9d;
  font-size: 9px;
  line-height: 1.6;
}

.submission-evidence {
  margin: 0 20px 20px;
  border: 1px solid #e6e9f0;
  border-radius: 10px;
  padding: 12px 14px;
}

.submission-evidence > strong {
  color: #465267;
  font-size: 10px;
}

.submission-evidence dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 10px 0 0;
}

.submission-evidence dl > div {
  display: grid;
  gap: 4px;
  border-radius: 8px;
  padding: 9px;
  background: #f8f9fb;
}

.submission-evidence dt,
.submission-evidence dd {
  margin: 0;
  font-size: 9px;
}

.submission-evidence dt {
  color: #8b95a5;
}

.submission-evidence dd {
  color: #3f4b60;
  font-weight: 800;
}

.grading-form {
  display: grid;
  grid-template-columns: minmax(160px, 0.3fr) minmax(0, 1fr);
  gap: 15px;
}

.grading-form .notice,
.grading-form .button-row {
  grid-column: 1 / -1;
}

.review-empty {
  min-height: 430px;
}

@media (max-width: 1050px) {
  .review-layout {
    grid-template-columns: 1fr;
  }

  .answer-list {
    position: static;
  }

  .answer-items {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    max-height: none;
  }
}

@media (max-width: 700px) {
  .review-summary,
  .score-strip,
  .grading-form,
  .answer-items,
  .submission-evidence dl {
    grid-template-columns: 1fr;
  }

  .review-summary > div,
  .score-strip > div {
    border-right: 0;
    border-bottom: 1px solid #edf0f4;
  }
}
</style>
