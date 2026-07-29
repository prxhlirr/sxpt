<script setup lang="ts">
import type { ApiLogEntry } from '../api/apiLogStore';

defineProps<{ entries: ApiLogEntry[] }>();
const emit = defineEmits<{ clear: []; close: [] }>();
</script>

<template>
  <aside class="floating-glass api-log-panel">
    <div class="panel-header">
      <div>
        <p class="eyebrow">接口调用</p>
        <h3>8–14 对接日志</h3>
      </div>
      <div class="mini-actions">
        <button type="button" class="secondary-action" @click="emit('clear')">清空</button>
        <button type="button" class="secondary-action" @click="emit('close')">关闭</button>
      </div>
    </div>
    <ol class="api-log-list">
      <li v-for="entry in entries" :key="entry.id">
        <span class="api-method">{{ entry.method }}</span>
        <div>
          <strong>{{ entry.path }}</strong>
          <small v-if="entry.captureSessionId">{{ entry.captureSessionId }}</small>
        </div>
        <span class="api-log-status" :class="entry.status">
          {{ entry.status === 'pending' ? '调用中' : entry.status === 'success' ? '成功' : '失败' }}
          <small v-if="entry.durationMs !== undefined">{{ entry.durationMs }}ms</small>
        </span>
      </li>
      <li v-if="!entries.length" class="empty-log">开始备案后，这里会显示接口调用顺序。</li>
    </ol>
  </aside>
</template>
