<script setup lang="ts">
import type { RunBatchRubricItem } from '../../types/runBatch';

defineProps<{
  objectiveMaxScore: number;
  subjectiveMaxScore: number;
  rubricItems: RunBatchRubricItem[];
}>();
</script>

<template>
  <section class="run-card">
    <div class="card-heading">
      <span>STEP 03</span>
      <h2>冻结评分确认</h2>
      <p>评分构成来自已发布流程快照，本批次只读使用。</p>
    </div>
    <div class="score-grid">
      <div><span>客观满分</span><strong>{{ objectiveMaxScore }}</strong></div>
      <div><span>主观满分</span><strong>{{ subjectiveMaxScore }}</strong></div>
      <div><span>总分</span><strong>{{ objectiveMaxScore + subjectiveMaxScore }}</strong></div>
    </div>
    <table>
      <thead><tr><th>序号</th><th>评分项</th><th>标识</th><th>分值</th><th>必评</th></tr></thead>
      <tbody>
        <tr v-for="item in rubricItems" :key="item.id">
          <td>{{ item.sequenceNo }}</td><td>{{ item.name }}</td><td><code>{{ item.itemKey }}</code></td><td>{{ item.maxScore }}</td><td>{{ item.required ? '是' : '否' }}</td>
        </tr>
        <tr v-if="rubricItems.length === 0"><td colspan="5" class="empty">该版本没有主观评分项。</td></tr>
      </tbody>
    </table>
  </section>
</template>

<style scoped>
.run-card { padding: 22px; border: 1px solid #e2e8f0; border-radius: 18px; background: #fff; }
.card-heading span { color: #7c3aed; font-size: 11px; font-weight: 850; letter-spacing: .14em; }
h2 { margin: 5px 0; } p { margin: 0; color: #64748b; }
.score-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin: 20px 0; }
.score-grid div { display: grid; gap: 5px; padding: 16px; border-radius: 14px; background: #f5f3ff; }
.score-grid span { color: #6d28d9; font-size: 12px; font-weight: 750; }
.score-grid strong { font-size: 26px; }
.empty { text-align: center; color: #64748b; }
@media (max-width: 620px) { .score-grid { grid-template-columns: 1fr; } }
</style>
