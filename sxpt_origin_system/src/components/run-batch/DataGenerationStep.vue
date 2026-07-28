<script setup lang="ts">
import { ref, watch } from 'vue';
import type {
  RunDataGenerationJob,
  UnitDataPlanInput
} from '../../types/runBatch';

const props = defineProps<{
  plans: UnitDataPlanInput[];
  teachingDataTemplateId: string;
  scenarioVersion: string;
  summary: {
    requestedCount: number;
    readyCount: number;
    failedCount: number;
    jobStatus: RunDataGenerationJob['status'];
  } | null;
  disabled: boolean;
  commandDisabled: boolean;
}>();

const emit = defineEmits<{
  change: [plans: UnitDataPlanInput[]];
  save: [];
  start: [];
  advance: [];
  refresh: [];
}>();

const jsonDrafts = ref<string[]>([]);
const jsonErrors = ref<string[]>([]);
const parameterSignatures = ref<string[]>([]);

watch(
  () => props.plans,
  (plans) => {
    const nextDrafts = jsonDrafts.value.slice(0, plans.length);
    const nextErrors = jsonErrors.value.slice(0, plans.length);
    const nextSignatures = plans.map((plan) =>
      JSON.stringify(plan.generationParameters)
    );
    plans.forEach((plan, index) => {
      if (parameterSignatures.value[index] !== nextSignatures[index]) {
        nextDrafts[index] = JSON.stringify(
          plan.generationParameters,
          null,
          2
        );
        nextErrors[index] = '';
      }
    });
    jsonDrafts.value = nextDrafts;
    jsonErrors.value = nextErrors;
    parameterSignatures.value = nextSignatures;
  },
  { immediate: true }
);

function copyPlans() {
  return props.plans.map((plan) => ({
    ...plan,
    generationParameters: { ...plan.generationParameters }
  }));
}

function updatePlan(index: number, patch: Partial<UnitDataPlanInput>) {
  emit('change', copyPlans().map((plan, planIndex) =>
    planIndex === index
      ? {
          ...plan,
          ...patch,
          generationParameters: {
            ...plan.generationParameters,
            ...(patch.generationParameters ?? {})
          }
        }
      : plan
  ));
}

function updateJson(index: number, value: string) {
  jsonDrafts.value[index] = value;
  try {
    const parsed = JSON.parse(value) as unknown;
    if (!parsed || Array.isArray(parsed) || typeof parsed !== 'object') {
      throw new Error('参数必须是 JSON 对象');
    }
    jsonErrors.value[index] = '';
    const next = copyPlans();
    const current = next[index];
    if (current) {
      current.generationParameters = { ...(parsed as Record<string, unknown>) };
      emit('change', next);
    }
  } catch (error) {
    jsonErrors.value[index] =
      error instanceof Error ? error.message : 'JSON 格式无效';
  }
}

function addPlan() {
  emit('change', [
    ...copyPlans(),
    {
      unitId: '',
      formalCount: 1,
      spareCount: 0,
      teachingDataTemplateId: props.teachingDataTemplateId,
      scenarioVersion: props.scenarioVersion,
      generationParameters: {}
    }
  ]);
}

function removePlan(index: number) {
  emit('change', copyPlans().filter((_, planIndex) => planIndex !== index));
}
</script>

<template>
  <section class="run-card">
    <div class="card-heading">
      <div>
        <span>STEP 04</span>
        <h2>按单位准备正式与备用数据</h2>
        <p>正式数量需覆盖单位内人数最多的角色组，备用数据不计入正式配额。</p>
      </div>
      <button type="button" :disabled="disabled" @click="addPlan">添加单位计划</button>
    </div>

    <div class="plan-list">
      <article v-for="(plan, index) in plans" :key="index">
        <div class="plan-fields">
          <label>单位 ID<input :value="plan.unitId" :disabled="disabled" @input="updatePlan(index, { unitId: ($event.target as HTMLInputElement).value })" /></label>
          <label>正式数量<input type="number" min="0" :value="plan.formalCount" :disabled="disabled" @input="updatePlan(index, { formalCount: Math.max(0, Number(($event.target as HTMLInputElement).value)) })" /></label>
          <label>备用数量<input type="number" min="0" :value="plan.spareCount" :disabled="disabled" @input="updatePlan(index, { spareCount: Math.max(0, Number(($event.target as HTMLInputElement).value)) })" /></label>
          <label>数据模板<input :value="plan.teachingDataTemplateId" disabled /></label>
          <label>场景版本<input :value="plan.scenarioVersion" disabled /></label>
          <button type="button" class="danger" :disabled="disabled" @click="removePlan(index)">移除</button>
        </div>
        <label class="json-field">
          造数参数（JSON 对象）
          <textarea
            rows="6"
            :value="jsonDrafts[index]"
            :disabled="disabled"
            @input="updateJson(index, ($event.target as HTMLTextAreaElement).value)"
          />
          <small v-if="jsonErrors[index]" class="error">{{ jsonErrors[index] }}</small>
        </label>
      </article>
      <p v-if="plans.length === 0" class="empty">暂无单位计划。</p>
    </div>

    <section v-if="summary" class="progress-card">
      <div><span>请求总数 requestedCount</span><strong>{{ summary.requestedCount }}</strong></div>
      <div><span>就绪 readyCount</span><strong>{{ summary.readyCount }}</strong></div>
      <div><span>失败 failedCount</span><strong>{{ summary.failedCount }}</strong></div>
      <div><span>任务 jobStatus</span><strong class="status">{{ summary.jobStatus }}</strong></div>
    </section>

    <div class="card-footer">
      <button type="button" class="secondary" :disabled="disabled || plans.length === 0 || jsonErrors.some(Boolean)" @click="emit('save')">保存单位计划</button>
      <button v-if="!summary" type="button" :disabled="commandDisabled || plans.length === 0 || jsonErrors.some(Boolean)" @click="emit('start')">保存并开始造数</button>
      <template v-else>
        <button type="button" class="secondary" :disabled="commandDisabled" @click="emit('refresh')">刷新进度</button>
        <button v-if="summary.jobStatus === 'PENDING' || summary.jobStatus === 'RUNNING'" type="button" :disabled="commandDisabled" @click="emit('advance')">推进模拟造数</button>
      </template>
    </div>
  </section>
</template>

<style scoped>
.run-card { padding: 22px; border: 1px solid #e2e8f0; border-radius: 18px; background: #fff; }
.card-heading, .card-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.card-heading span { color: #7c3aed; font-size: 11px; font-weight: 850; letter-spacing: .14em; }
h2 { margin: 5px 0; } p { margin: 0; color: #64748b; font-size: 12px; }
.plan-list { display: grid; gap: 14px; margin: 18px 0; }
article { padding: 15px; border: 1px solid #e2e8f0; border-radius: 14px; background: #f8fafc; }
.plan-fields { display: grid; grid-template-columns: 1fr .65fr .65fr 1fr 1fr auto; gap: 10px; align-items: end; }
label { display: grid; gap: 5px; color: #475569; font-size: 12px; font-weight: 700; }
.json-field { margin-top: 12px; }
.danger, .secondary { border-color: #cbd5e1; color: #475569; background: #fff; }
.danger { color: #b91c1c; }
.error { color: #b91c1c; }
.progress-card { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin: 18px 0; }
.progress-card div { display: grid; gap: 6px; padding: 14px; border-radius: 12px; background: #f5f3ff; }
.progress-card span { color: #6d28d9; font-size: 11px; font-weight: 750; }
.progress-card strong { font-size: 24px; }
.progress-card .status { font-size: 14px; overflow-wrap: anywhere; }
.card-footer { justify-content: flex-end; padding-top: 15px; border-top: 1px solid #e2e8f0; }
.empty { padding: 24px; text-align: center; }
@media (max-width: 1050px) { .plan-fields { grid-template-columns: repeat(2, 1fr); } .progress-card { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 620px) { .card-heading, .card-footer { align-items: stretch; flex-direction: column; } .plan-fields, .progress-card { grid-template-columns: 1fr; } }
</style>
