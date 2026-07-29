<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue';

const props = defineProps<{
  show: boolean;
  type: 'success' | 'error' | 'info';
  message: string;
  duration?: number;
}>();

const emit = defineEmits<{
  close: [];
}>();

let timer: number | undefined;

watch(
  () => [props.show, props.message, props.type] as const,
  () => {
    window.clearTimeout(timer);
    if (!props.show || !props.message) return;
    timer = window.setTimeout(() => emit('close'), props.duration ?? 1000);
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  window.clearTimeout(timer);
});
</script>

<template>
  <div v-if="show && message" class="timed-toast" :class="type" role="status">
    <span>{{ message }}</span>
    <button type="button" aria-label="关闭提示" @click="emit('close')">×</button>
  </div>
</template>

<style scoped>
.timed-toast {
  position: fixed;
  top: 22px;
  right: 22px;
  z-index: 5000;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  max-width: min(420px, calc(100vw - 32px));
  border: 1px solid #dbe3ef;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 16px 42px rgba(15, 23, 42, 0.14);
  color: #172033;
  padding: 12px 12px 12px 14px;
  font-size: 13px;
  font-weight: 750;
  line-height: 1.5;
}

.timed-toast.success {
  border-color: #bbf7d0;
  color: #0f7a55;
}

.timed-toast.error {
  border-color: #fecaca;
  color: #b42318;
}

.timed-toast.info {
  border-color: #bfdbfe;
  color: #2563eb;
}

.timed-toast button {
  display: inline-grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  color: currentColor;
  cursor: pointer;
  font-size: 18px;
  line-height: 1;
  opacity: 0.72;
}

.timed-toast button:hover,
.timed-toast button:focus-visible {
  border-color: currentColor;
  outline: none;
  opacity: 1;
}

@media (max-width: 720px) {
  .timed-toast {
    top: 12px;
    right: 12px;
    left: 12px;
    max-width: none;
  }
}
</style>
