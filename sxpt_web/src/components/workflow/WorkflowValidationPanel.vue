<script setup lang="ts">
import { computed } from 'vue';
import type { WorkflowValidationResult } from '../../types/workflow';

const props = defineProps<{
  result: WorkflowValidationResult | null;
}>();

const issueMessages: Record<string, string> = {
  MISSING_ROLE_GROUP: '至少需要一个实训分组。',
  MISSING_STAGE: '至少需要一个业务阶段。',
  STAGE_SEQUENCE_NOT_CONTINUOUS: '阶段顺序必须从 1 开始连续编号。',
  UNBOUND_ROLE_GROUP: '阶段尚未绑定有效的实训分组。',
  MISSING_REQUIRED_STEP: '阶段缺少必做步骤。',
  DUPLICATE_STEP_SEQUENCE: '阶段内存在重复的步骤顺序。',
  INVALID_TASK_STEP_REFERENCE: '任务步骤不存在或不属于当前任务。',
  INVALID_RECORDED_ASSET_VERSION: '录制资产不存在、未确认或版本无效。',
  OBJECTIVE_MAX_MISMATCH: '各阶段客观分之和与客观满分不一致。',
  SUBJECTIVE_MAX_MISMATCH: '量表分值之和与主观满分不一致。',
  MISSING_EXTERNAL_IDENTITY: '阶段缺少业务角色或业务单位身份。',
  MISSING_LAUNCH_CONFIG: '阶段缺少业务入口配置。',
  INVALID_DATA_TEMPLATE: '数据模板或场景版本尚未正确配置。',
  NON_SERIAL_TRANSITION: '当前版本仅支持严格串行阶段流转。'
};

const title = computed(() => {
  if (!props.result) return '尚未执行发布校验';
  return props.result.valid ? '校验通过，可以发布' : `发现 ${props.result.issues.length} 个阻断项`;
});

function chineseMessage(code: string, fallback: string) {
  return issueMessages[code] ?? fallback ?? '请检查该配置项。';
}
</script>

<template>
  <aside
    class="validation-panel"
    :class="{ valid: result?.valid, invalid: result && !result.valid }"
  >
    <div class="validation-mark">{{ result?.valid ? '✓' : result ? '!' : 'i' }}</div>
    <div>
      <strong>{{ title }}</strong>
      <p v-if="!result">保存草稿后执行校验，系统会列出所有发布阻断项。</p>
      <ol v-else-if="!result.valid">
        <li v-for="issue in result.issues" :key="`${issue.code}-${issue.path}`">
          <code>{{ issue.code }}</code>
          <span>{{ chineseMessage(issue.code, issue.message) }}</span>
          <small>{{ issue.path }}</small>
        </li>
      </ol>
      <p v-else>分组、阶段、录制资产、数据模板与评分上限均满足发布要求。</p>
    </div>
  </aside>
</template>

<style scoped>
.validation-panel { display: grid; grid-template-columns: 38px 1fr; gap: 12px; padding: 16px; border: 1px solid #cbd5e1; border-radius: 14px; background: #f8fafc; }
.validation-panel.valid { border-color: #86efac; background: #f0fdf4; }
.validation-panel.invalid { border-color: #fca5a5; background: #fef2f2; }
.validation-mark { width: 34px; height: 34px; border-radius: 50%; display: grid; place-items: center; background: #64748b; color: #fff; font-weight: 900; }
.valid .validation-mark { background: #16a34a; }
.invalid .validation-mark { background: #dc2626; }
strong { color: #0f172a; }
p { margin: 5px 0 0; color: #64748b; font-size: 13px; }
ol { margin: 10px 0 0; padding-left: 20px; display: grid; gap: 8px; }
li span, li small { display: block; }
code { color: #b91c1c; font-weight: 800; }
li span { margin-top: 2px; color: #334155; }
li small { color: #94a3b8; }
</style>
