<script setup lang="ts">
import type { ApiMode } from '../config/trainingConfig';
import type { CaptureCommandState } from '../engine/captureWorkspace';

defineProps<{
  state: CaptureCommandState;
  recording: boolean;
  busyAction?: string;
  apiMode: ApiMode;
  sessionId?: string;
}>();

const emit = defineEmits<{
  start: [];
  pause: [];
  resume: [];
  undo: [];
  'insert-guide': [];
  rerecord: [];
  'save-segment': [];
  'switch-segment': [];
  publish: [];
}>();
</script>

<template>
  <header class="floating-topbar command-strip capture-command-strip">
    <div class="capture-heading">
      <p class="eyebrow">多段备案</p>
      <h2>教案采集工作台</h2>
      <div class="capture-context">
        <span class="api-mode-badge" :class="apiMode">{{ apiMode.toUpperCase() }}</span>
        <span v-if="sessionId" class="session-id" :title="sessionId">{{ sessionId }}</span>
      </div>
    </div>
    <div class="topbar-actions capture-actions">
      <button :disabled="!state.canStart || Boolean(busyAction)" @click="emit('start')">
        {{ busyAction === 'start' ? '创建中...' : '开始备案' }}
      </button>
      <button
        v-if="recording"
        class="secondary-action"
        :disabled="!state.canPause || Boolean(busyAction)"
        @click="emit('pause')"
      >
        暂停
      </button>
      <button
        v-else
        class="secondary-action"
        :disabled="!state.canResume || Boolean(busyAction)"
        @click="emit('resume')"
      >
        继续
      </button>
      <button class="secondary-action" :disabled="!state.canUndo || Boolean(busyAction)" @click="emit('undo')">
        撤销节点
      </button>
      <button class="secondary-action" :disabled="!state.canInsertGuide || Boolean(busyAction)" @click="emit('insert-guide')">
        插入说明
      </button>
      <button class="secondary-action" :disabled="!state.canRerecord || Boolean(busyAction)" @click="emit('rerecord')">
        从此处重录
      </button>
      <button :disabled="!state.canSaveSegment || Boolean(busyAction)" @click="emit('save-segment')">
        {{ busyAction === 'save' ? '保存中...' : '保存本段' }}
      </button>
      <button :disabled="!state.canSwitchSegment || Boolean(busyAction)" @click="emit('switch-segment')">
        开始下一段
      </button>
      <button :disabled="!state.canPublish || Boolean(busyAction)" @click="emit('publish')">
        {{ busyAction === 'publish' ? '发布中...' : '发布教案' }}
      </button>
      <RouterLink class="glass-link" to="/learn">学习演示</RouterLink>
    </div>
  </header>
</template>
