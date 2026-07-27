<script setup lang="ts">
defineProps<{
  label: string;
  value: string | number;
  hint: string;
  tone?: 'purple' | 'blue' | 'green' | 'amber';
}>();
</script>

<template>
  <article class="metric-card" :class="tone ?? 'purple'">
    <div class="metric-card__icon"><slot name="icon">◆</slot></div>
    <div><span>{{ label }}</span><strong>{{ value }}</strong><small>{{ hint }}</small></div>
  </article>
</template>

