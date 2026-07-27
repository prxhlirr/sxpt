<script setup lang="ts">
import type { RunDataItem } from '../../types/runBatch';

defineProps<{
  items: RunDataItem[];
  loading: boolean;
  mutating: boolean;
}>();

const emit = defineEmits<{
  select: [itemId: string];
  action: [payload: {
    action: 'disable' | 'promote' | 'replace' | 'retry';
    item: RunDataItem;
  }];
}>();

function generationRequestStatus(item: RunDataItem) {
  if (item.status === 'GENERATION_FAILED') return '请求执行失败';
  if (item.status === 'GENERATING') return '请求执行中';
  return item.generationJobId ? '请求已完成' : '无可靠请求关联';
}

function canDisable(item: RunDataItem) {
  return item.status === 'READY';
}

function canReplace(item: RunDataItem) {
  return item.status === 'READY';
}

function canPromote(item: RunDataItem) {
  return item.kind === 'SPARE' && item.status === 'READY';
}

function canRetry(item: RunDataItem) {
  return item.status === 'GENERATION_FAILED';
}

function caseStageSummary(item: RunDataItem) {
  return item.status === 'IN_USE'
    ? { primary: '办理中', secondary: '阶段平台暂不可可靠判定' }
    : { primary: '尚未开始', secondary: '平台暂不可可靠判定' };
}

function formatTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN');
}
</script>

<template>
  <div class="table-scroll">
    <table>
      <thead>
        <tr>
          <th>平台数据编号</th>
          <th>脱敏业务引用</th>
          <th>单位</th>
          <th>正式/备用</th>
          <th>生成修订</th>
          <th>生成请求状态</th>
          <th>数据条目状态</th>
          <th>当前业务单/阶段摘要</th>
          <th>创建/更新时间</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="item in items" :key="item.id">
          <td><button type="button" class="link" :disabled="loading" @click="emit('select', item.id)">{{ item.id }}</button></td>
          <td><code>{{ item.businessReferenceMasked || '—' }}</code></td>
          <td>{{ item.unitId }}</td>
          <td><span class="kind" :class="item.kind.toLowerCase()">{{ item.kind === 'FORMAL' ? '正式' : '备用' }}</span></td>
          <td>r{{ item.generationRevision }}</td>
          <td>
            <span>{{ generationRequestStatus(item) }}</span>
            <small v-if="item.generationJobId">{{ item.generationJobId }}</small>
          </td>
          <td><span class="status">{{ item.status }}</span></td>
          <td>
            <strong>{{ caseStageSummary(item).primary }}</strong>
            <small>{{ caseStageSummary(item).secondary }}</small>
          </td>
          <td><span>{{ formatTime(item.createdAt) }}</span><small>{{ formatTime(item.updatedAt) }}</small></td>
          <td>
            <div class="row-actions">
              <button type="button" class="minor" :disabled="mutating || !canDisable(item)" @click="emit('action', { action: 'disable', item })">停用</button>
              <button type="button" class="minor" :disabled="mutating || !canReplace(item)" @click="emit('action', { action: 'replace', item })">替换</button>
              <button type="button" class="minor" :disabled="mutating || !canPromote(item)" @click="emit('action', { action: 'promote', item })">转正式</button>
              <button type="button" class="minor" :disabled="mutating || !canRetry(item)" @click="emit('action', { action: 'retry', item })">重试</button>
            </div>
          </td>
        </tr>
        <tr v-if="!loading && items.length === 0"><td colspan="10" class="empty">当前筛选条件下没有数据。</td></tr>
        <tr v-if="loading"><td colspan="10" class="empty">正在加载数据池…</td></tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.table-scroll { overflow-x: auto; }
table { min-width: 1480px; }
td { color: #334155; font-size: 13px; }
td span, td small { display: block; }
td small { margin-top: 4px; color: #94a3b8; font-size: 10px; overflow-wrap: anywhere; }
.link { min-height: 0; padding: 0; border: 0; color: #6d28d9; background: transparent; text-align: left; }
.kind, .status { display: inline-flex; width: fit-content; padding: 4px 7px; border-radius: 999px; color: #334155; background: #e2e8f0; font-size: 11px; font-weight: 750; }
.kind.formal { color: #166534; background: #dcfce7; }
.kind.spare { color: #92400e; background: #fef3c7; }
.row-actions { display: flex; flex-wrap: wrap; gap: 5px; min-width: 150px; }
.minor { min-height: 28px; padding: 3px 7px; border-color: #cbd5e1; color: #475569; background: #fff; font-size: 11px; }
.empty { padding: 36px; text-align: center; color: #64748b; }
</style>
