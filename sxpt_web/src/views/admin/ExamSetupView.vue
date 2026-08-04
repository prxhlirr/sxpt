<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import PageHeader from '../../components/ui/PageHeader.vue';
import WorkflowStepper from '../../components/exam/WorkflowStepper.vue';
import { validateExamDraft } from '../../components/exam/workflow';
import type { ExamSettings } from '../../domain/models';
import { useTrainingStore } from '../../stores/trainingStore';

const route = useRoute();
const router = useRouter();
const store = useTrainingStore();
const lessonId = String(route.params.lessonId);
const lesson = computed(() => store.getLesson(lessonId));
const saved = store.state.examSettings[lessonId];
const configurationLocked = computed(() =>
  store.state.publishedTasks.some((task) => task.lessonId === lessonId)
);

function toLocalDateTime(value: string | undefined, fallback: string) {
  if (!value) return fallback;
  return value.slice(0, 16);
}

function relativeLocalDateTime(offsetMinutes: number) {
  const date = new Date(Date.now() + offsetMinutes * 60_000);
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(
    date.getDate()
  )}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

const defaults: ExamSettings = {
  lessonId,
  batchName: `${lesson.value?.title ?? '业务实训'} · 第一期考试`,
  mode: 'EXAM',
  startAt: relativeLocalDateTime(-5),
  endAt: relativeLocalDateTime(120),
  durationMinutes: 90,
  allowRetry: true,
  maxAttempts: 2,
  objectiveWeight: 80,
  subjectiveWeight: 20,
  showProgress: true,
  randomizeData: false,
  submissionFields: [
    { key: 'businessNo', label: '业务单号', required: true },
    { key: 'completionNote', label: '办理说明', required: false }
  ]
};

const draft = reactive<ExamSettings>({
  ...(saved ?? defaults),
  startAt: toLocalDateTime(saved?.startAt, defaults.startAt),
  endAt: toLocalDateTime(saved?.endAt, defaults.endAt),
  submissionFields: (saved?.submissionFields ?? defaults.submissionFields).map(
    (field) => ({ ...field })
  )
});
const feedback = ref('');
const saving = ref(false);
const errors = computed(() => validateExamDraft(draft));
const weightTotal = computed(
  () => Number(draft.objectiveWeight || 0) + Number(draft.subjectiveWeight || 0)
);
const requiredFields = computed(
  () => draft.submissionFields.filter((field) => field.required).length
);

function addSubmissionField() {
  draft.submissionFields.push({
    key: `field${draft.submissionFields.length + 1}`,
    label: '新提交字段',
    required: false
  });
}

function removeSubmissionField(index: number) {
  draft.submissionFields.splice(index, 1);
}

function normalizeNumbers() {
  draft.durationMinutes = Number(draft.durationMinutes);
  draft.maxAttempts = Number(draft.maxAttempts);
  draft.objectiveWeight = Number(draft.objectiveWeight);
  draft.subjectiveWeight = Number(draft.subjectiveWeight);
}

function save(next = false) {
  normalizeNumbers();
  feedback.value = '';
  if (errors.value.length) {
    feedback.value = '请先处理右侧列出的配置问题。';
    return;
  }
  saving.value = true;
  try {
    store.saveExamSettings(lessonId, {
      ...draft,
      submissionFields: draft.submissionFields.map((field) => ({ ...field }))
    });
    feedback.value = '考试设置已保存，后续分组将沿用本批次规则。';
    if (next) {
      void router.push({ name: 'group-setup', params: { lessonId } });
    }
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '考试设置保存失败';
  } finally {
    saving.value = false;
  }
}
</script>

<template>
  <main class="page exam-page">
    <PageHeader
      eyebrow="LESSON BUSINESS CHAIN · 02"
      title="考试设置"
      :description="`为「${lesson?.title ?? '当前教案'}」配置考试窗口、作答规则、评分结构与提交凭证。`"
    >
      <button class="secondary" type="button" @click="router.push({ name: 'lesson-editor', params: { lessonId } })">
        返回教案
      </button>
      <button
        class="primary"
        type="button"
        :disabled="saving || configurationLocked"
        @click="save(true)"
      >
        保存并进入分组
      </button>
    </PageHeader>

    <WorkflowStepper
      :lesson-id="lessonId"
      current="exam"
      aria-label="教案编排、考试设置、分组设置、数据生成、发布任务"
    />
    <div v-if="configurationLocked" class="notice">
      此教案已有考试任务，考试规则已冻结。请复制教案后创建新版本。
    </div>

    <div v-if="!lesson" class="card empty-state">
      <div>
        <strong>未找到教案</strong>
        <p>该教案可能已被删除，请返回教案列表重新选择。</p>
      </div>
    </div>

    <div v-else class="exam-layout">
      <section class="exam-main">
        <article class="card">
          <div class="card-header">
            <div>
              <h2>基础信息与考试窗口</h2>
              <p>考试开始后，学员会依据此窗口进入对应的业务操作任务。</p>
            </div>
            <span class="section-index">01</span>
          </div>
          <div class="card-body form-grid">
            <label class="field wide">
              考试批次名称
              <input v-model.trim="draft.batchName" placeholder="例如：2026 年采购业务考试（第一期）" />
            </label>
            <label class="field">
              运行模式
              <select v-model="draft.mode">
                <option value="EXAM">正式考试</option>
                <option value="PRACTICE">练习考核</option>
                <option value="LEARNING">学习演示</option>
              </select>
            </label>
            <label class="field">
              单次作答时长（分钟）
              <input v-model.number="draft.durationMinutes" type="number" min="1" step="5" />
            </label>
            <label class="field">
              开始时间
              <input v-model="draft.startAt" type="datetime-local" />
            </label>
            <label class="field">
              结束时间
              <input v-model="draft.endAt" type="datetime-local" />
            </label>
          </div>
        </article>

        <article class="card">
          <div class="card-header">
            <div>
              <h2>作答与数据策略</h2>
              <p>提交最终结果前可重新作答，每次作答将通过业务接口申请新的同条件数据。</p>
            </div>
            <span class="section-index">02</span>
          </div>
          <div class="card-body option-grid">
            <label class="toggle-card">
              <input v-model="draft.allowRetry" type="checkbox" />
              <span>
                <strong>允许重新作答</strong>
                <small>最终提交前可放弃当前作答并获取新数据</small>
              </span>
            </label>
            <label class="toggle-card">
              <input v-model="draft.randomizeData" type="checkbox" />
              <span>
                <strong>随机分配业务数据</strong>
                <small>在学员所属单位可用数据池内随机选择</small>
              </span>
            </label>
            <label class="toggle-card">
              <input v-model="draft.showProgress" type="checkbox" />
              <span>
                <strong>向学员显示流程进度</strong>
              <small>显示当前教学点、已完成教学点和下一步角色</small>
              </span>
            </label>
            <label class="field compact-field">
              最大作答次数
              <input
                v-model.number="draft.maxAttempts"
                type="number"
                min="1"
                :disabled="!draft.allowRetry"
              />
            </label>
          </div>
        </article>

        <article class="card">
          <div class="card-header">
            <div>
              <h2>评分结构</h2>
              <p>系统只计算客观分，教师在考试结束后补充主观评分和批语。</p>
            </div>
            <span class="weight-total" :class="{ invalid: weightTotal !== 100 }">
              合计 {{ weightTotal }}%
            </span>
          </div>
          <div class="card-body score-grid">
            <label class="score-field">
              <span><strong>客观评分</strong><small>流程操作、教学点完成、提交凭证</small></span>
              <div><input v-model.number="draft.objectiveWeight" type="number" min="0" max="100" /><b>%</b></div>
            </label>
            <label class="score-field subjective">
              <span><strong>主观评分</strong><small>教师评价业务规范性与完成质量</small></span>
              <div><input v-model.number="draft.subjectiveWeight" type="number" min="0" max="100" /><b>%</b></div>
            </label>
          </div>
        </article>

        <article class="card">
          <div class="card-header">
            <div>
              <h2>动态提交字段</h2>
              <p>用于学员提交本次办理结果，后续可替换为真实接口字段定义。</p>
            </div>
            <button class="secondary" type="button" @click="addSubmissionField">＋ 添加字段</button>
          </div>
          <div class="card-body submission-list">
            <div
              v-for="(field, index) in draft.submissionFields"
              :key="`${field.key}-${index}`"
              class="submission-row"
            >
              <span class="drag-handle">⋮⋮</span>
              <label>
                字段标识
                <input v-model.trim="field.key" placeholder="businessNo" />
              </label>
              <label>
                显示名称
                <input v-model.trim="field.label" placeholder="业务单号" />
              </label>
              <label class="required-check">
                <input v-model="field.required" type="checkbox" />
                必填
              </label>
              <button class="danger icon-action" type="button" aria-label="删除字段" @click="removeSubmissionField(index)">
                ×
              </button>
            </div>
            <div v-if="!draft.submissionFields.length" class="inline-empty">
              尚未配置提交字段。可仅依据流程操作进行客观评分，也可添加业务单号等凭证。
            </div>
          </div>
        </article>

        <div v-if="feedback" class="notice" :class="{ danger: errors.length }">{{ feedback }}</div>
        <div class="bottom-actions">
          <button
            class="secondary"
            type="button"
            :disabled="configurationLocked"
            @click="save(false)"
          >
            仅保存
          </button>
          <button
            class="primary"
            type="button"
            :disabled="errors.length > 0 || saving || configurationLocked"
            @click="save(true)"
          >
            下一步：分组设置 →
          </button>
        </div>
      </section>

      <aside class="exam-aside">
        <article class="card sticky-card">
          <div class="card-header">
            <div>
              <h2>配置摘要</h2>
              <p>保存前实时检查</p>
            </div>
            <span class="summary-dot" :class="{ ready: !errors.length }" />
          </div>
          <div class="card-body summary-list">
            <div><span>教案版本</span><strong>V{{ lesson.version }}</strong></div>
            <div><span>考试模式</span><strong>{{ draft.mode }}</strong></div>
            <div><span>作答时长</span><strong>{{ draft.durationMinutes || 0 }} 分钟</strong></div>
            <div><span>最大作答</span><strong>{{ draft.allowRetry ? `${draft.maxAttempts} 次` : '仅 1 次' }}</strong></div>
            <div><span>评分权重</span><strong>{{ draft.objectiveWeight }} / {{ draft.subjectiveWeight }}</strong></div>
            <div><span>提交字段</span><strong>{{ draft.submissionFields.length }} 项 / {{ requiredFields }} 项必填</strong></div>
          </div>
          <div class="validation-panel" :class="{ valid: !errors.length }">
            <strong>{{ errors.length ? `发现 ${errors.length} 个配置问题` : '配置校验通过' }}</strong>
            <ul v-if="errors.length">
              <li v-for="error in errors" :key="error">{{ error }}</li>
            </ul>
            <p v-else>考试规则可保存，下一步可配置任意数量的业务角色组。</p>
          </div>
        </article>
      </aside>
    </div>
  </main>
</template>
