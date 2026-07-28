<script setup lang="ts">
import type {
  WorkflowRoleGroupInput,
  WorkflowStageInput
} from '../../types/workflow';

const props = defineProps<{
  stage?: WorkflowStageInput;
  groups: WorkflowRoleGroupInput[];
  readonly?: boolean;
}>();

const emit = defineEmits<{
  change: [stageId: string, patch: Partial<WorkflowStageInput>];
}>();

function patch(patchValue: Partial<WorkflowStageInput>) {
  if (props.stage && !props.readonly) emit('change', props.stage.id, patchValue);
}

function text(event: Event) {
  return (event.target as HTMLInputElement).value;
}

function numberValue(event: Event) {
  return Number((event.target as HTMLInputElement).value);
}
</script>

<template>
  <section class="workflow-card">
    <header>
      <div>
        <p class="section-kicker">阶段属性</p>
        <h2>{{ stage?.name || '请选择业务阶段' }}</h2>
      </div>
      <span v-if="stage" class="sequence">第 {{ stage.sequenceNo }} 阶段</span>
    </header>

    <fieldset v-if="stage" class="form-grid" :disabled="readonly">
      <label>
        阶段名称
        <input :value="stage.name" @input="patch({ name: text($event) })" />
      </label>
      <label>
        阶段编码
        <input
          :value="stage.stageKey"
          @input="patch({ stageKey: text($event) })"
        />
      </label>
      <label class="wide identity-field training-identity">
        实训分组（平台编排）
        <select
          :value="stage.roleGroupId"
          @change="patch({ roleGroupId: text($event) })"
        >
          <option value="">请选择实训分组</option>
          <option v-for="group in groups" :key="group.id" :value="group.id">
            {{ group.name }}（{{ group.groupKey }}）
          </option>
        </select>
        <small>决定哪些学员负责本阶段，不会写入业务系统身份。</small>
      </label>

      <div class="wide identity-title">
        <strong>业务身份</strong>
        <span>由业务方提供，用于打开对应角色与单位的数据范围。</span>
      </div>
      <label class="identity-field">
        业务角色 ID
        <input
          :value="stage.externalRoleId"
          placeholder="如：PURCHASE_HANDLER"
          @input="patch({ externalRoleId: text($event) })"
        />
      </label>
      <label class="identity-field">
        业务单位 ID
        <input
          :value="stage.externalOrgId"
          placeholder="如：ORG_1001"
          @input="patch({ externalOrgId: text($event) })"
        />
      </label>
      <label class="wide">
        业务入口地址
        <input
          :value="String(stage.launchConfig.path ?? '')"
          placeholder="/business/module"
          @input="patch({ launchConfig: { ...stage.launchConfig, path: text($event) } })"
        />
      </label>
      <label>
        阶段客观分
        <input
          :value="stage.stageMaxScore"
          type="number"
          min="0"
          step="0.5"
          @input="patch({ stageMaxScore: numberValue($event) })"
        />
      </label>
      <label>
        限时（秒）
        <input
          :value="stage.timeLimitSeconds"
          type="number"
          min="1"
          @input="patch({ timeLimitSeconds: numberValue($event) || undefined })"
        />
      </label>
      <label>
        断线宽限（秒）
        <input
          :value="stage.reconnectGraceSeconds"
          type="number"
          min="1"
          @input="patch({ reconnectGraceSeconds: numberValue($event) })"
        />
      </label>
      <label class="check-line">
        <input
          type="checkbox"
          :checked="stage.allowSameLearnerNextDefault"
          @change="patch({ allowSameLearnerNextDefault: ($event.target as HTMLInputElement).checked })"
        />
        默认允许同一学员继续下一阶段
      </label>
      <label class="check-line">
        <input
          type="checkbox"
          :checked="stage.allowBatchOverride"
          @change="patch({ allowBatchOverride: ($event.target as HTMLInputElement).checked })"
        />
        允许考试批次覆盖分配方式
      </label>
    </fieldset>
    <p v-else class="empty-copy">
      从上方串行业务链中选择一个阶段，配置分组与业务身份。
    </p>
  </section>
</template>

<style scoped>
.workflow-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 18px; padding: 20px; box-shadow: 0 12px 35px rgb(15 23 42 / 6%); }
header { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
h2 { margin: 2px 0 0; font-size: 20px; color: #0f172a; }
.section-kicker { margin: 0; color: #0891b2; font-size: 12px; font-weight: 800; letter-spacing: .08em; }
.sequence { color: #0e7490; background: #ecfeff; border-radius: 999px; padding: 6px 10px; font-size: 12px; font-weight: 700; }
.form-grid { margin-top: 16px; display: grid; grid-template-columns: 1fr 1fr; gap: 12px; padding: 0; border: 0; }
label { display: grid; gap: 6px; color: #334155; font-size: 13px; font-weight: 650; }
input, select { min-width: 0; border: 1px solid #cbd5e1; border-radius: 10px; padding: 10px; background: #fff; color: #0f172a; }
.wide { grid-column: 1 / -1; }
.identity-title { display: flex; align-items: baseline; justify-content: space-between; padding-top: 6px; border-top: 1px solid #e2e8f0; }
.identity-title span, label small { color: #64748b; font-size: 12px; font-weight: 400; }
.identity-field { border-radius: 12px; padding: 10px; background: #f8fafc; }
.training-identity { background: #eff6ff; border: 1px solid #bfdbfe; }
.check-line { display: flex; grid-column: 1 / -1; align-items: center; gap: 8px; }
.check-line input { width: 16px; }
.empty-copy { color: #64748b; }
@media (max-width: 720px) { .form-grid { grid-template-columns: 1fr; } .wide { grid-column: auto; } }
</style>
