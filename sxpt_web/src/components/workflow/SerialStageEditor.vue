<script setup lang="ts">
import type { WorkflowStageInput } from '../../types/workflow';

defineProps<{
  stages: WorkflowStageInput[];
  selectedStageId?: string;
  readonly?: boolean;
}>();

const emit = defineEmits<{
  add: [];
  remove: [stageId: string];
  move: [stageId: string, targetIndex: number];
  select: [stageId: string];
  record: [stageId: string];
}>();
</script>

<template>
  <section class="workflow-card stage-card">
    <header>
      <div>
        <p class="section-kicker">串行业务链</p>
        <h2>业务阶段</h2>
      </div>
      <button type="button" class="add-button" :disabled="readonly" @click="emit('add')">
        + 新增阶段
      </button>
    </header>

    <p class="section-help">
      学员按 1 → 2 → 3 的顺序协作，每个阶段只绑定一个实训分组。
    </p>

    <div v-if="stages.length" class="stage-flow">
      <article
        v-for="(stage, index) in stages"
        :key="stage.id"
        :class="{ selected: selectedStageId === stage.id }"
        @click="emit('select', stage.id)"
      >
        <div class="stage-sequence">{{ stage.sequenceNo }}</div>
        <div class="stage-copy">
          <strong>{{ stage.name }}</strong>
          <span>{{ stage.stageKey }} · 分组 {{ stage.roleGroupId || '未绑定' }}</span>
          <small>{{ stage.steps.length }} 个录制步骤 · {{ stage.stageMaxScore }} 分</small>
        </div>
        <div class="stage-actions" @click.stop>
          <button
            type="button"
            :disabled="readonly || index === 0"
            @click="emit('move', stage.id, index - 1)"
          >
            ↑
          </button>
          <button
            type="button"
            :disabled="readonly || index === stages.length - 1"
            @click="emit('move', stage.id, index + 1)"
          >
            ↓
          </button>
          <button type="button" :disabled="readonly" @click="emit('record', stage.id)">录制</button>
          <button
            type="button"
            class="danger"
            :disabled="readonly"
            @click="emit('remove', stage.id)"
          >
            删除
          </button>
        </div>
        <span v-if="index < stages.length - 1" class="flow-arrow">→</span>
      </article>
    </div>
    <p v-else class="empty-copy">创建分组后，新增第一个业务阶段。</p>
  </section>
</template>

<style scoped>
.workflow-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 18px; padding: 20px; box-shadow: 0 12px 35px rgb(15 23 42 / 6%); }
header { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
h2 { margin: 2px 0 0; font-size: 20px; color: #0f172a; }
.section-kicker { margin: 0; color: #7c3aed; font-size: 12px; font-weight: 800; letter-spacing: .08em; }
.section-help, .empty-copy { color: #64748b; font-size: 13px; line-height: 1.6; }
.add-button { border: 0; border-radius: 10px; padding: 9px 13px; background: #f3e8ff; color: #6d28d9; font-weight: 700; cursor: pointer; }
.stage-flow { display: grid; grid-auto-flow: column; grid-auto-columns: minmax(230px, 1fr); gap: 30px; overflow-x: auto; padding: 8px 2px 12px; }
article { position: relative; display: grid; grid-template-columns: 38px 1fr; gap: 10px; border: 1px solid #e2e8f0; border-radius: 15px; padding: 14px; cursor: pointer; transition: .18s ease; }
article:hover, article.selected { border-color: #8b5cf6; box-shadow: 0 8px 22px rgb(124 58 237 / 12%); }
article.selected { background: #faf5ff; }
.stage-sequence { width: 36px; height: 36px; border-radius: 12px; display: grid; place-items: center; color: #fff; background: linear-gradient(135deg, #7c3aed, #2563eb); font-weight: 800; }
.stage-copy strong, .stage-copy span, .stage-copy small { display: block; }
.stage-copy span { margin-top: 4px; color: #475569; font-size: 12px; }
.stage-copy small { margin-top: 6px; color: #94a3b8; }
.stage-actions { grid-column: 1 / -1; display: flex; flex-wrap: wrap; gap: 6px; }
.stage-actions button { border: 1px solid #cbd5e1; border-radius: 8px; padding: 5px 9px; background: #fff; cursor: pointer; }
.stage-actions button:disabled { opacity: .35; cursor: default; }
.stage-actions .danger { color: #dc2626; border-color: #fecaca; }
.flow-arrow { position: absolute; right: -22px; top: 42%; color: #94a3b8; font-size: 22px; }
</style>
