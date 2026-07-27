<script setup lang="ts">
import type { RecordedSegment, StepPersistenceStatus } from '../types/domain';

defineProps<{
  segments: RecordedSegment[];
  activeSegmentId?: string;
  selectedStepId?: string;
}>();

const emit = defineEmits<{
  'select-segment': [segmentId: string];
  'select-step': [segmentId: string, stepId: string];
  retry: [stepId: string];
}>();

const persistenceLabels: Record<StepPersistenceStatus, string> = {
  local: '本地',
  reporting: '上报中',
  reported: '已上报',
  draft_pending: '草稿待确认',
  confirmed: '已确认',
  discarded: '已丢弃',
  error: '上报失败'
};

function persistenceLabel(status?: StepPersistenceStatus) {
  return persistenceLabels[status ?? 'local'];
}

function segmentStatusLabel(status: RecordedSegment['status']) {
  return {
    recording: '录制中',
    saving: '保存中',
    saved: '已保存',
    error: '保存失败'
  }[status];
}
</script>

<template>
  <aside class="floating-glass floating-step-panel segmented-flow-panel">
    <div class="panel-header">
      <div>
        <p class="eyebrow">备案路径</p>
        <h3>多角色分段</h3>
      </div>
      <strong>{{ segments.length }} 段</strong>
    </div>

    <div class="segment-list">
      <section
        v-for="segment in segments"
        :key="segment.id"
        class="segment-block"
        :class="{ active: segment.id === activeSegmentId }"
      >
        <button class="segment-header" type="button" @click="emit('select-segment', segment.id)">
          <span class="segment-number">{{ segment.segmentNo }}</span>
          <span class="segment-title">
            <strong>{{ segment.title }}</strong>
            <small>
              {{ segment.externalRoleName || '当前教师角色' }} ·
              {{ segment.externalOrgName || '默认单位' }}
            </small>
          </span>
          <span class="segment-status" :class="segment.status">{{ segmentStatusLabel(segment.status) }}</span>
        </button>

        <ol v-if="segment.id === activeSegmentId" class="recorded-step-list segment-step-list">
          <li
            v-for="step in segment.steps"
            :key="step.id"
            :class="{ active: step.id === selectedStepId, discarded: step.persistence?.status === 'discarded' }"
            @click="emit('select-step', segment.id, step.id)"
          >
            <span class="step-index">{{ step.order }}</span>
            <div class="step-copy">
              <strong>{{ step.title }}</strong>
              <p>{{ step.kind === 'guide' ? '说明弹框' : step.text || step.actionType }}</p>
              <span class="persistence-badge" :class="step.persistence?.status ?? 'local'">
                {{ persistenceLabel(step.persistence?.status) }}
              </span>
              <button
                v-if="step.persistence?.status === 'error'"
                type="button"
                class="step-retry"
                @click.stop="emit('retry', step.id)"
              >
                重试
              </button>
            </div>
          </li>
          <li v-if="!segment.steps.length" class="empty-segment">本段尚未录制节点</li>
        </ol>
      </section>
    </div>
  </aside>
</template>
