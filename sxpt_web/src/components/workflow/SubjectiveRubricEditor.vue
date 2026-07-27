<script setup lang="ts">
import type { SubjectiveRubricItemInput } from '../../types/workflow';

const props = defineProps<{
  items: SubjectiveRubricItemInput[];
  readonly?: boolean;
}>();

const emit = defineEmits<{
  change: [items: SubjectiveRubricItemInput[]];
}>();

function update(
  id: string,
  patch: Partial<SubjectiveRubricItemInput>
) {
  if (props.readonly) return;
  emit(
    'change',
    props.items.map((item) => item.id === id ? { ...item, ...patch } : item)
  );
}

function add() {
  if (props.readonly) return;
  emit('change', [
    ...props.items,
    {
      id: createId('rubric'),
      itemKey: `RUBRIC_${props.items.length + 1}`,
      name: '新的主观评分项',
      description: '',
      sequenceNo: props.items.length + 1,
      maxScore: 0,
      required: true
    }
  ]);
}

function remove(id: string) {
  if (props.readonly) return;
  emit('change', props.items.filter((item) => item.id !== id));
}

function createId(prefix: string) {
  return `${prefix}-${typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`}`;
}
</script>

<template>
  <section class="workflow-card">
    <header>
      <div>
        <p class="section-kicker">教师评价</p>
        <h2>主观评分量表</h2>
      </div>
      <button type="button" class="add-button" :disabled="readonly" @click="add">+ 评分项</button>
    </header>
    <p class="section-help">
      系统只计算客观分；教师在考试结束后依据量表评分并填写批语。
    </p>
    <fieldset v-if="items.length" class="rubric-list" :disabled="readonly">
      <article v-for="item in items" :key="item.id">
        <span class="sequence">{{ item.sequenceNo }}</span>
        <input
          :value="item.name"
          placeholder="评分项名称"
          @input="update(item.id, { name: ($event.target as HTMLInputElement).value })"
        />
        <input
          :value="item.itemKey"
          placeholder="编码"
          @input="update(item.id, { itemKey: ($event.target as HTMLInputElement).value })"
        />
        <input
          :value="item.maxScore"
          type="number"
          min="0"
          step="0.5"
          @input="update(item.id, { maxScore: Number(($event.target as HTMLInputElement).value) })"
        />
        <label>
          <input
            type="checkbox"
            :checked="item.required"
            @change="update(item.id, { required: ($event.target as HTMLInputElement).checked })"
          />
          必评
        </label>
        <button type="button" class="danger" @click="remove(item.id)">删除</button>
        <textarea
          :value="item.description"
          rows="2"
          placeholder="评分说明"
          @input="update(item.id, { description: ($event.target as HTMLTextAreaElement).value })"
        />
      </article>
    </fieldset>
    <p v-else class="empty-copy">未设置主观评分项，主观满分应为 0。</p>
  </section>
</template>

<style scoped>
.workflow-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 18px; padding: 20px; box-shadow: 0 12px 35px rgb(15 23 42 / 6%); }
header { display: flex; align-items: center; justify-content: space-between; }
h2 { margin: 2px 0 0; font-size: 20px; }
.section-kicker { margin: 0; color: #d97706; font-size: 12px; font-weight: 800; letter-spacing: .08em; }
.section-help, .empty-copy { color: #64748b; font-size: 13px; }
.add-button { border: 0; border-radius: 10px; padding: 9px 12px; background: #fef3c7; color: #b45309; font-weight: 750; cursor: pointer; }
.rubric-list { display: grid; gap: 9px; padding: 0; border: 0; }
article { display: grid; grid-template-columns: 30px 1.4fr 1fr 90px 70px auto; gap: 8px; align-items: center; padding: 10px; border: 1px solid #e2e8f0; border-radius: 12px; }
.sequence { width: 26px; height: 26px; display: grid; place-items: center; border-radius: 8px; background: #f59e0b; color: #fff; font-weight: 800; }
input, textarea { min-width: 0; border: 1px solid #cbd5e1; border-radius: 8px; padding: 8px; }
label { display: flex; gap: 5px; align-items: center; font-size: 12px; }
textarea { grid-column: 2 / -1; resize: vertical; }
.danger { border: 0; background: transparent; color: #dc2626; cursor: pointer; }
@media (max-width: 900px) { article { grid-template-columns: 30px 1fr; } textarea { grid-column: 2; } }
</style>
