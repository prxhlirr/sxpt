<script setup lang="ts">
import StatusPill from '../ui/StatusPill.vue';

interface ArchiveMetric {
  label: string;
  value: string | number;
  suffix?: string;
  tone?: 'primary' | 'blue' | 'green';
}

withDefaults(
  defineProps<{
    title: string;
    businessScope: string;
    coverBadge: string;
    avatarText: string;
    ownerName: string;
    ownerMeta: string;
    status: string;
    statusLabel: string;
    metrics: ArchiveMetric[];
    summary: string;
    theme?: 'purple' | 'green' | 'blue';
    selected?: boolean;
  }>(),
  {
    theme: 'purple',
    selected: false
  }
);

defineEmits<{
  select: [];
}>();
</script>

<template>
  <article
    class="result-archive-card"
    :class="[`theme-${theme}`, { selected }]"
    @click="$emit('select')"
  >
    <header class="archive-cover">
      <span>{{ coverBadge }}</span>
      <small>{{ businessScope }}</small>
      <strong>{{ title }}</strong>
    </header>

    <div class="archive-body">
      <div class="archive-person">
        <div>
          <i>{{ avatarText }}</i>
          <span>
            <strong>{{ ownerName }}</strong>
            <small>{{ ownerMeta }}</small>
          </span>
        </div>
        <StatusPill :status="status" :label="statusLabel" />
      </div>

      <div class="archive-metrics">
        <span
          v-for="metric in metrics"
          :key="metric.label"
          :class="metric.tone ? `tone-${metric.tone}` : undefined"
        >
          {{ metric.label }}
          <b>{{ metric.value }}</b>
          <small v-if="metric.suffix">{{ metric.suffix }}</small>
        </span>
      </div>

      <p class="archive-summary">{{ summary }}</p>

      <div class="archive-actions" @click.stop>
        <slot name="actions" />
      </div>
    </div>
  </article>
</template>

<style scoped>
.result-archive-card {
  min-width: 0;
  overflow: hidden;
  border: 1px solid #e2e6ef;
  border-radius: 18px;
  background: #fff;
  box-shadow: 0 10px 28px rgb(31 37 78 / 6%);
  cursor: pointer;
  transition:
    transform 160ms ease,
    border-color 160ms ease,
    box-shadow 160ms ease;
}

.result-archive-card:hover,
.result-archive-card.selected {
  transform: translateY(-2px);
  border-color: #cfc8ff;
  box-shadow: 0 16px 34px rgb(48 41 112 / 12%);
}

.archive-cover {
  position: relative;
  display: grid;
  min-height: 142px;
  align-content: end;
  gap: 8px;
  overflow: hidden;
  padding: 24px;
  color: #fff;
  background:
    radial-gradient(circle at 84% 12%, rgb(255 255 255 / 24%), transparent 34%),
    linear-gradient(135deg, #5142bc, #7565de);
}

.archive-cover::after {
  position: absolute;
  top: -34px;
  right: -26px;
  width: 118px;
  height: 118px;
  border: 20px solid rgb(255 255 255 / 8%);
  border-radius: 50%;
  content: '';
}

.theme-green .archive-cover {
  background:
    radial-gradient(circle at 84% 12%, rgb(255 255 255 / 23%), transparent 34%),
    linear-gradient(135deg, #147a69, #30a183);
}

.theme-blue .archive-cover {
  background:
    radial-gradient(circle at 84% 12%, rgb(255 255 255 / 23%), transparent 34%),
    linear-gradient(135deg, #256d9f, #4a96c8);
}

.archive-cover > span {
  position: absolute;
  z-index: 1;
  top: 18px;
  left: 20px;
  border: 1px solid rgb(255 255 255 / 28%);
  border-radius: 999px;
  padding: 6px 11px;
  background: rgb(255 255 255 / 15%);
  font-size: 12px;
  font-weight: 800;
  backdrop-filter: blur(8px);
}

.archive-cover small,
.archive-cover strong {
  position: relative;
  z-index: 1;
}

.archive-cover small {
  overflow: hidden;
  color: rgb(255 255 255 / 78%);
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.archive-cover strong {
  display: -webkit-box;
  overflow: hidden;
  font-size: 20px;
  line-height: 1.4;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.archive-body {
  display: grid;
  gap: 16px;
  padding: 20px;
}

.archive-person,
.archive-person > div {
  display: flex;
  align-items: center;
}

.archive-person {
  justify-content: space-between;
  gap: 12px;
}

.archive-person > div {
  min-width: 0;
  gap: 10px;
}

.archive-person i {
  display: grid;
  width: 40px;
  height: 40px;
  flex: 0 0 auto;
  place-items: center;
  border-radius: 12px;
  color: #5948c8;
  background: #efedff;
  font-size: 15px;
  font-style: normal;
  font-weight: 900;
}

.theme-green .archive-person i {
  color: #11745f;
  background: #e7f7f1;
}

.theme-blue .archive-person i {
  color: #246d9f;
  background: #e9f4fb;
}

.archive-person span {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.archive-person strong,
.archive-person small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.archive-person strong {
  color: #344157;
  font-size: 15px;
}

.archive-person small {
  color: #8a94a5;
  font-size: 12px;
}

.archive-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  overflow: hidden;
  border: 1px solid #e9ebf1;
  border-radius: 12px;
  background: #fafbfc;
}

.archive-metrics > span {
  display: grid;
  min-width: 0;
  justify-items: center;
  gap: 5px;
  border-right: 1px solid #e9ebf1;
  padding: 12px 6px;
  color: #8b95a5;
  font-size: 11px;
}

.archive-metrics > span:last-child {
  border-right: 0;
}

.archive-metrics b {
  color: #5948c8;
  font-size: 21px;
  line-height: 1;
}

.archive-metrics .tone-blue b {
  color: #2479b7;
}

.archive-metrics .tone-green b {
  color: #07815d;
}

.archive-metrics small {
  color: #a0a7b3;
  font-size: 10px;
}

.archive-summary {
  min-height: 44px;
  margin: 0;
  color: #707b8d;
  font-size: 13px;
  line-height: 1.7;
}

.archive-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px solid #edf0f4;
  padding-top: 15px;
}

.archive-actions :deep(button),
.archive-actions :deep(.button) {
  min-height: 40px;
  border-radius: 10px;
}

@media (max-width: 540px) {
  .archive-cover {
    min-height: 126px;
    padding: 20px;
  }

  .archive-cover strong {
    font-size: 18px;
  }

  .archive-body {
    padding: 16px;
  }
}
</style>
