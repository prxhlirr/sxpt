<script setup lang="ts">
import { computed } from 'vue';
import type { RunBatchPreflight } from '../../types/runBatch';

const props = defineProps<{
  result: RunBatchPreflight | null;
  disabled: boolean;
  canPublish: boolean;
}>();

const emit = defineEmits<{
  check: [];
  publish: [];
}>();

const issueGroups = computed(() => {
  const groups = new Map<string, NonNullable<typeof props.result>['issues']>();
  for (const issue of props.result?.issues ?? []) {
    const key = `${issue.unitId ?? '全局'} / ${issue.stageId ?? '全部阶段'}`;
    groups.set(key, [...(groups.get(key) ?? []), issue]);
  }
  return [...groups.entries()].map(([label, issues]) => ({ label, issues }));
});
</script>

<template>
  <section class="run-card">
    <div class="card-heading">
      <div>
        <span>STEP 05</span>
        <h2>发布预检</h2>
        <p>阻断项按单位、阶段和角色组定位；修改任何配置后需重新预检。</p>
      </div>
      <strong :class="{ valid: result?.valid, invalid: result && !result.valid }">
        {{ !result ? '尚未预检' : result.valid ? '预检通过' : '存在阻断项' }}
      </strong>
    </div>

    <div v-if="result" class="result">
      <p>检查时间：{{ result.checkedAt }}</p>
      <div v-if="result.issues.length" class="issue-groups">
        <section v-for="group in issueGroups" :key="group.label">
          <h3>{{ group.label }}</h3>
          <table>
            <thead><tr><th>阻断代码</th><th>角色组</th><th>说明</th></tr></thead>
            <tbody>
              <tr v-for="(issue, index) in group.issues" :key="`${issue.code}-${index}`">
                <td><code>{{ issue.code }}</code></td>
                <td>{{ issue.groupKey ?? '—' }}</td>
                <td>{{ issue.message }}</td>
              </tr>
            </tbody>
          </table>
        </section>
      </div>
      <div v-else class="success">成员、正式数据配额、业务身份映射与数据就绪状态均已通过检查。</div>

      <details>
        <summary>查看单位角色组配额（{{ result.memberStageQuotas.length }}）</summary>
        <table>
          <thead><tr><th>单位</th><th>角色组</th><th>成员数</th><th>所需正式数据</th></tr></thead>
          <tbody>
            <tr v-for="quota in result.memberStageQuotas" :key="`${quota.unitId}-${quota.groupKey}`">
              <td>{{ quota.unitId }}</td><td>{{ quota.groupKey }}</td><td>{{ quota.memberCount }}</td><td>{{ quota.requiredFormalCount }}</td>
            </tr>
          </tbody>
        </table>
      </details>
    </div>
    <p v-else class="empty">完成造数后运行预检，系统将从 API 读取最新服务端状态。</p>

    <div class="card-footer">
      <button type="button" class="secondary" :disabled="disabled" @click="emit('check')">运行发布预检</button>
      <button type="button" :disabled="disabled || !canPublish" @click="emit('publish')">发布批次</button>
    </div>
  </section>
</template>

<style scoped>
.run-card { padding: 22px; border: 1px solid #e2e8f0; border-radius: 18px; background: #fff; }
.card-heading, .card-footer { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
.card-heading span { color: #7c3aed; font-size: 11px; font-weight: 850; letter-spacing: .14em; }
h2 { margin: 5px 0; } p { margin: 0; color: #64748b; font-size: 12px; }
.card-heading strong { padding: 6px 10px; border-radius: 999px; color: #475569; background: #f1f5f9; font-size: 12px; }
.card-heading strong.valid { color: #166534; background: #dcfce7; }
.card-heading strong.invalid { color: #b91c1c; background: #fee2e2; }
.result { margin-top: 20px; }
.issue-groups { display: grid; gap: 12px; margin-top: 14px; }
.issue-groups section { overflow-x: auto; padding: 12px; border: 1px solid #fecaca; border-radius: 12px; background: #fffafa; }
.issue-groups h3 { margin: 0 0 8px; color: #991b1b; font-size: 13px; }
.success, .empty { margin: 16px 0; padding: 22px; border-radius: 12px; text-align: center; color: #166534; background: #f0fdf4; }
.empty { color: #64748b; background: #f8fafc; }
details { margin-top: 16px; padding: 12px; border: 1px solid #e2e8f0; border-radius: 12px; }
summary { cursor: pointer; font-weight: 750; }
.card-footer { justify-content: flex-end; margin-top: 18px; padding-top: 15px; border-top: 1px solid #e2e8f0; }
.secondary { border-color: #cbd5e1; color: #475569; background: #fff; }
@media (max-width: 620px) { .card-heading, .card-footer { align-items: stretch; flex-direction: column; } }
</style>
