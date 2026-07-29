<script setup lang="ts">
import type {
  WorkflowStageInput,
  WorkflowStageStepInput
} from '../../types/workflow';

const props = defineProps<{
  stage?: WorkflowStageInput;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  change: [stageId: string, steps: WorkflowStageStepInput[]];
  record: [stageId: string];
}>();

function updateStep(
  stepId: string,
  patch: Partial<WorkflowStageStepInput>
) {
  if (!props.stage || props.readonly) return;
  emit(
    'change',
    props.stage.id,
    props.stage.steps.map((step) =>
      step.id === stepId ? { ...step, ...patch } : step
    )
  );
}

function removeStep(stepId: string) {
  if (!props.stage || props.readonly) return;
  emit(
    'change',
    props.stage.id,
    props.stage.steps.filter((step) => step.id !== stepId)
  );
}

function moveStep(stepId: string, targetIndex: number) {
  if (!props.stage || props.readonly) return;
  const sourceIndex = props.stage.steps.findIndex((item) => item.id === stepId);
  if (sourceIndex < 0 || targetIndex < 0 || targetIndex >= props.stage.steps.length) {
    return;
  }
  const steps = [...props.stage.steps];
  const [moved] = steps.splice(sourceIndex, 1);
  steps.splice(targetIndex, 0, moved);
  emit('change', props.stage.id, steps);
}
</script>

<template>
  <section class="workflow-card">
    <header>
      <div>
        <p class="section-kicker">录制资产</p>
        <h2>阶段步骤绑定</h2>
      </div>
      <button
        v-if="stage"
        type="button"
        class="record-button"
        :disabled="readonly"
        @click="emit('record', stage.id)"
      >
        ● 录制本阶段
      </button>
    </header>
    <p class="section-help">
      每个步骤同时保存任务步骤、录制资产版本、判定方式和分值，发布后形成不可变快照。
    </p>

    <fieldset v-if="stage?.steps.length" class="step-table" :disabled="readonly">
      <article v-for="(step, index) in stage.steps" :key="step.id">
        <div class="step-head">
          <span>{{ step.sequenceNo }}</span>
          <strong>{{ step.taskStepId || '未绑定任务步骤' }}</strong>
          <div>
            <button type="button" @click="moveStep(step.id, index - 1)">↑</button>
            <button type="button" @click="moveStep(step.id, index + 1)">↓</button>
            <button type="button" class="danger" @click="removeStep(step.id)">删除</button>
          </div>
        </div>
        <div class="step-fields">
          <label>
            TaskStep ID
            <input
              :value="step.taskStepId"
              @input="updateStep(step.id, { taskStepId: ($event.target as HTMLInputElement).value })"
            />
          </label>
          <label>
            录制资产 ID
            <input
              :value="step.recordedSegmentId"
              @input="updateStep(step.id, { recordedSegmentId: ($event.target as HTMLInputElement).value })"
            />
          </label>
          <label>
            资产版本
            <input
              :value="step.recordedAssetVersion"
              type="number"
              min="1"
              @input="updateStep(step.id, { recordedAssetVersion: Number(($event.target as HTMLInputElement).value) })"
            />
          </label>
          <label>
            完成判定
            <input
              :value="step.completionMethod"
              @input="updateStep(step.id, { completionMethod: ($event.target as HTMLInputElement).value })"
            />
          </label>
          <label>
            失败策略
            <select
              :value="step.failurePolicy"
              @change="updateStep(step.id, { failurePolicy: ($event.target as HTMLSelectElement).value })"
            >
              <option value="stop">停止</option>
              <option value="retry">重试</option>
              <option value="skip">允许跳过</option>
            </select>
          </label>
          <label>
            分值
            <input
              :value="step.score"
              type="number"
              min="0"
              step="0.5"
              @input="updateStep(step.id, { score: Number(($event.target as HTMLInputElement).value) })"
            />
          </label>
        </div>
      </article>
    </fieldset>
    <p v-else class="empty-copy">
      {{ stage ? '尚未绑定步骤，点击“录制本阶段”开始采集。' : '请先选择业务阶段。' }}
    </p>
  </section>
</template>

<style scoped>
.workflow-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 18px; padding: 20px; box-shadow: 0 12px 35px rgb(15 23 42 / 6%); }
header { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
h2 { margin: 2px 0 0; font-size: 20px; }
.section-kicker { margin: 0; color: #dc2626; font-size: 12px; font-weight: 800; letter-spacing: .08em; }
.section-help, .empty-copy { color: #64748b; font-size: 13px; line-height: 1.6; }
.record-button { border: 0; border-radius: 10px; padding: 10px 14px; background: #fee2e2; color: #b91c1c; font-weight: 800; cursor: pointer; }
.step-table { display: grid; gap: 10px; margin-top: 14px; padding: 0; border: 0; }
article { border: 1px solid #e2e8f0; border-radius: 13px; overflow: hidden; }
.step-head { display: grid; grid-template-columns: 30px 1fr auto; align-items: center; gap: 8px; padding: 10px 12px; background: #f8fafc; }
.step-head > span { width: 26px; height: 26px; display: grid; place-items: center; border-radius: 8px; color: #fff; background: #475569; font-size: 12px; }
.step-head button { border: 0; background: transparent; cursor: pointer; }
.step-head .danger { color: #dc2626; }
.step-fields { display: grid; grid-template-columns: repeat(3, 1fr); gap: 9px; padding: 12px; }
label { display: grid; gap: 5px; color: #475569; font-size: 12px; font-weight: 650; }
input, select { min-width: 0; border: 1px solid #cbd5e1; border-radius: 8px; padding: 8px; background: #fff; }
@media (max-width: 800px) { .step-fields { grid-template-columns: 1fr; } }
</style>
