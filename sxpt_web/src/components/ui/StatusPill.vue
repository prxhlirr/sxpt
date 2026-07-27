<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ status: string; label?: string }>();

const tone = computed(() => {
  if (['PUBLISHED', 'READY', 'RUNNING', 'GRADED', 'COMPLETED'].includes(props.status)) return 'success';
  if (['DRAFT', 'RECORDED', 'SCHEDULED', 'TODO'].includes(props.status)) return 'warning';
  if (['GENERATION_FAILED', 'DISABLED', 'ARCHIVED'].includes(props.status)) return 'danger';
  return 'neutral';
});
</script>

<template>
  <span class="status-pill" :class="tone">{{ label ?? status }}</span>
</template>

