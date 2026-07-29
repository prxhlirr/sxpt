<script setup lang="ts">
import type { RunnerMode } from '../types/domain';

defineProps<{
  modelValue: RunnerMode;
}>();

const emit = defineEmits<{
  'update:modelValue': [mode: RunnerMode];
}>();

const modes: Array<{ value: RunnerMode; label: string }> = [
  { value: 'learning', label: '学习' },
  { value: 'practice', label: '练习' },
  { value: 'exam', label: '考试' }
];
</script>

<template>
  <div class="segmented-control" aria-label="运行模式">
    <button
      v-for="mode in modes"
      :key="mode.value"
      type="button"
      :class="{ active: modelValue === mode.value }"
      @click="emit('update:modelValue', mode.value)"
    >
      {{ mode.label }}
    </button>
  </div>
</template>
