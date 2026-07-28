<script setup lang="ts">
import { ref, watch } from 'vue';

const props = defineProps<{
  templateId?: string;
  scenarioVersion?: string;
  defaults?: Record<string, unknown>;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  change: [
    value: {
      templateId: string;
      scenarioVersion: string;
      defaults: Record<string, unknown>;
    }
  ];
}>();

const templateId = ref('');
const scenarioVersion = ref('');
const defaultsText = ref('{}');
const parseError = ref('');

watch(
  () => [props.templateId, props.scenarioVersion, props.defaults] as const,
  ([nextTemplate, nextScenario, nextDefaults]) => {
    templateId.value = nextTemplate ?? '';
    scenarioVersion.value = nextScenario ?? '';
    defaultsText.value = JSON.stringify(nextDefaults ?? {}, null, 2);
  },
  { immediate: true, deep: true }
);

function submit() {
  if (props.readonly) return;
  try {
    const parsed = JSON.parse(defaultsText.value);
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      throw new Error('必须是 JSON 对象');
    }
    parseError.value = '';
    emit('change', {
      templateId: templateId.value.trim(),
      scenarioVersion: scenarioVersion.value.trim(),
      defaults: parsed as Record<string, unknown>
    });
  } catch (error) {
    parseError.value = error instanceof Error ? error.message : 'JSON 格式错误';
  }
}
</script>

<template>
  <section class="workflow-card">
    <header>
      <div>
        <p class="section-kicker">考试数据</p>
        <h2>数据生成模板</h2>
      </div>
      <span class="policy">重新作答 = 生成替代数据</span>
    </header>
    <p class="section-help">
      平台不直接访问业务库，仅保存业务方接口所需的模板、场景版本与默认参数。
    </p>
    <fieldset class="template-grid" :disabled="readonly">
      <label>
        教学数据模板 ID
        <input v-model="templateId" placeholder="purchase-approval-template" />
      </label>
      <label>
        数据场景版本
        <input v-model="scenarioVersion" placeholder="v1" />
      </label>
      <label class="wide">
        默认生成参数（JSON）
        <textarea v-model="defaultsText" rows="6" spellcheck="false" />
      </label>
      <p v-if="parseError" class="error wide">{{ parseError }}</p>
      <button type="button" class="apply-button wide" @click="submit">
        应用数据模板配置
      </button>
    </fieldset>
  </section>
</template>

<style scoped>
.workflow-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 18px; padding: 20px; box-shadow: 0 12px 35px rgb(15 23 42 / 6%); }
header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
h2 { margin: 2px 0 0; font-size: 20px; }
.section-kicker { margin: 0; color: #059669; font-size: 12px; font-weight: 800; letter-spacing: .08em; }
.section-help { color: #64748b; font-size: 13px; line-height: 1.6; }
.policy { padding: 6px 10px; border-radius: 999px; color: #047857; background: #ecfdf5; font-size: 11px; font-weight: 750; }
.template-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; padding: 0; border: 0; }
label { display: grid; gap: 6px; color: #334155; font-size: 13px; font-weight: 650; }
input, textarea { border: 1px solid #cbd5e1; border-radius: 10px; padding: 10px; font: inherit; }
textarea { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; resize: vertical; }
.wide { grid-column: 1 / -1; }
.apply-button { border: 0; border-radius: 10px; padding: 10px; color: #fff; background: #059669; font-weight: 750; cursor: pointer; }
.error { margin: 0; color: #dc2626; font-size: 12px; }
@media (max-width: 720px) { .template-grid { grid-template-columns: 1fr; } .wide { grid-column: auto; } }
</style>
