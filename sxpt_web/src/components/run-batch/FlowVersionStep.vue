<script setup lang="ts">
import { computed } from 'vue';
import type { PublishedWorkflowOption } from '../../types/runBatch';

const props = defineProps<{
  workflows: PublishedWorkflowOption[];
  selectedWorkflowId: string;
  batchName: string;
  plannedStartTime: string;
  plannedEndTime: string;
  disabled: boolean;
  saved: boolean;
}>();

const emit = defineEmits<{
  'update:selectedWorkflowId': [value: string];
  'update:batchName': [value: string];
  'update:plannedStartTime': [value: string];
  'update:plannedEndTime': [value: string];
  create: [];
}>();

const selected = computed(() =>
  props.workflows.find((item) => item.id === props.selectedWorkflowId)
);
</script>

<template>
  <section class="run-card">
    <div class="card-heading">
      <div>
        <span>STEP 01</span>
        <h2>选择已发布流程版本</h2>
      </div>
      <strong>{{ saved ? '批次已创建' : '等待创建' }}</strong>
    </div>

    <div class="form-grid">
      <label>
        已发布流程
        <select
          :value="selectedWorkflowId"
          :disabled="disabled || saved"
          @change="emit('update:selectedWorkflowId', ($event.target as HTMLSelectElement).value)"
        >
          <option value="">请选择流程版本</option>
          <option
            v-for="workflow in workflows"
            :key="workflow.id"
            :value="workflow.id"
          >
            {{ workflow.name }} · v{{ workflow.versionNo }}
          </option>
        </select>
      </label>
      <label>
        批次名称
        <input
          :value="batchName"
          :disabled="disabled || saved"
          placeholder="例如：采购流程期末考试"
          @input="emit('update:batchName', ($event.target as HTMLInputElement).value)"
        />
      </label>
      <label>
        计划开始时间
        <input
          type="datetime-local"
          :value="plannedStartTime"
          :disabled="disabled || saved"
          @input="emit('update:plannedStartTime', ($event.target as HTMLInputElement).value)"
        />
      </label>
      <label>
        计划结束时间
        <input
          type="datetime-local"
          :value="plannedEndTime"
          :disabled="disabled || saved"
          @input="emit('update:plannedEndTime', ($event.target as HTMLInputElement).value)"
        />
      </label>
    </div>

    <template v-if="selected">
      <dl class="summary-grid">
        <div><dt>任务</dt><dd>{{ selected.taskId }}</dd></div>
        <div><dt>版本摘要</dt><dd><code>{{ selected.snapshotDigest }}</code></dd></div>
        <div><dt>数据模板</dt><dd>{{ selected.teachingDataTemplateId }}</dd></div>
        <div><dt>场景版本</dt><dd>{{ selected.scenarioVersion }}</dd></div>
      </dl>
      <div class="workflow-map">
        <div>
          <h3>角色组（{{ selected.roleGroups.length }}）</h3>
          <span
            v-for="group in selected.roleGroups"
            :key="group.id"
            class="chip"
          >{{ group.sequenceNo }}. {{ group.name }}</span>
        </div>
        <div>
          <h3>串行阶段（{{ selected.stages.length }}）</h3>
          <ol>
            <li v-for="stage in selected.stages" :key="stage.id">
              {{ stage.name }}
            </li>
          </ol>
        </div>
      </div>
    </template>

    <div class="card-footer">
      <p>流程快照与评分上限将在创建时冻结，创建后不可切换版本。</p>
      <button
        type="button"
        :disabled="disabled || saved || !selectedWorkflowId || !batchName.trim()"
        @click="emit('create')"
      >
        {{ saved ? '批次已创建' : '创建并继续' }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.run-card { padding: 22px; border: 1px solid #e2e8f0; border-radius: 18px; background: #fff; box-shadow: 0 12px 30px rgb(15 23 42 / 6%); }
.card-heading, .card-footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.card-heading span { color: #7c3aed; font-size: 11px; font-weight: 850; letter-spacing: .14em; }
.card-heading h2 { margin: 5px 0 18px; }
.card-heading strong { color: #166534; font-size: 13px; }
.form-grid, .summary-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
label { display: grid; gap: 6px; color: #475569; font-size: 13px; font-weight: 750; }
.summary-grid div { min-width: 0; padding: 12px; border-radius: 12px; background: #f8fafc; }
dt { color: #64748b; font-size: 12px; } dd { margin: 5px 0 0; overflow-wrap: anywhere; }
.workflow-map { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-top: 14px; }
.workflow-map > div { padding: 14px; border: 1px solid #e2e8f0; border-radius: 12px; }
h3 { margin: 0 0 10px; font-size: 14px; }
.chip { display: inline-flex; margin: 3px; padding: 5px 9px; border-radius: 999px; color: #5b21b6; background: #ede9fe; font-size: 12px; font-weight: 700; }
ol { margin: 0; padding-left: 22px; color: #475569; line-height: 1.8; }
.card-footer { margin-top: 18px; padding-top: 16px; border-top: 1px solid #e2e8f0; }
.card-footer p { margin: 0; color: #64748b; font-size: 12px; }
@media (max-width: 720px) { .form-grid, .summary-grid, .workflow-map { grid-template-columns: 1fr; } .card-footer { align-items: stretch; flex-direction: column; } }
</style>
