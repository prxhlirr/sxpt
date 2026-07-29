<script setup lang="ts">
import type { RunDataItem } from '../../types/runBatch';

defineProps<{
  item: RunDataItem | null;
  loading: boolean;
}>();

const emit = defineEmits<{ close: [] }>();

function formatJson(value: Record<string, unknown>) {
  return JSON.stringify(value, null, 2);
}

function formatTime(value: string) {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString('zh-CN');
}
</script>

<template>
  <div v-if="item || loading" class="drawer-backdrop" @click.self="emit('close')">
    <aside class="drawer" aria-label="数据条目详情">
      <header>
        <div><span>DATA ITEM</span><h2>数据条目详情</h2></div>
        <button type="button" class="close" @click="emit('close')">关闭</button>
      </header>
      <p v-if="loading" class="loading">正在读取最新条目状态…</p>
      <template v-else-if="item">
        <dl>
          <div><dt>平台数据编号</dt><dd>{{ item.id }}</dd></div>
          <div><dt>脱敏业务引用</dt><dd><code>{{ item.businessReferenceMasked || '—' }}</code></dd></div>
          <div><dt>单位 / 类型</dt><dd>{{ item.unitId }} / {{ item.kind }}</dd></div>
          <div><dt>状态 / 修订</dt><dd>{{ item.status }} / r{{ item.generationRevision }}</dd></div>
          <div><dt>生成任务</dt><dd>{{ item.generationJobId }}</dd></div>
          <div><dt>替换来源</dt><dd>{{ item.replacementOfItemId ?? '—' }}</dd></div>
        </dl>

        <section>
          <h3>不可变生成参数快照</h3>
          <pre>{{ formatJson(item.generationParameters) }}</pre>
        </section>

        <section>
          <h3>审计历史 auditHistory</h3>
          <ol v-if="item.auditHistory.length">
            <li v-for="event in item.auditHistory" :key="`${event.sequenceNo}-${event.eventType}`">
              <div><strong>{{ event.eventType }}</strong><time>{{ formatTime(event.occurredAt) }}</time></div>
              <p>{{ event.reason || '系统状态变更' }}</p>
              <small>{{ event.aggregateType }} · {{ event.clientRequestId }}</small>
            </li>
          </ol>
          <p v-else class="empty">该条目暂无独立审计事件。</p>
        </section>
      </template>
    </aside>
  </div>
</template>

<style scoped>
.drawer-backdrop { position: fixed; z-index: 30; inset: 0; display: flex; justify-content: flex-end; background: rgb(15 23 42 / 45%); }
.drawer { width: min(580px, 94vw); height: 100%; overflow-y: auto; padding: 24px; background: #fff; box-shadow: -20px 0 50px rgb(15 23 42 / 18%); }
header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-bottom: 18px; border-bottom: 1px solid #e2e8f0; }
header span { color: #7c3aed; font-size: 11px; font-weight: 850; letter-spacing: .14em; }
h2 { margin: 5px 0 0; }
.close { border-color: #cbd5e1; color: #475569; background: #fff; }
dl { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
dl div { min-width: 0; padding: 12px; border-radius: 10px; background: #f8fafc; }
dt { color: #64748b; font-size: 11px; } dd { margin: 5px 0 0; overflow-wrap: anywhere; font-size: 13px; }
section { margin-top: 20px; } h3 { font-size: 14px; }
pre { max-height: 330px; overflow: auto; padding: 14px; border-radius: 10px; color: #e2e8f0; background: #172033; white-space: pre-wrap; overflow-wrap: anywhere; }
ol { display: grid; gap: 10px; padding: 0; list-style: none; }
li { padding: 12px; border-left: 3px solid #8b5cf6; background: #f8fafc; }
li div { display: flex; justify-content: space-between; gap: 10px; }
time, li small { color: #64748b; font-size: 11px; }
li p { margin: 7px 0; font-size: 13px; }
.empty, .loading { padding: 28px; text-align: center; color: #64748b; }
@media (max-width: 520px) { dl { grid-template-columns: 1fr; } }
</style>
