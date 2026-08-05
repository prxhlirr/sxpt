<script setup lang="ts">
import type { TrainingAttachment } from '../../domain/models';
import { formatAttachmentSize } from '../../utils/trainingAttachments';

defineProps<{
  attachments?: TrainingAttachment[];
  title: string;
}>();
</script>

<template>
  <section v-if="attachments?.length" class="attachment-panel">
    <header>
      <strong>{{ title }}</strong>
      <span>{{ attachments.length }} 个</span>
    </header>
    <a
      v-for="attachment in attachments"
      :key="attachment.id"
      class="attachment-panel__item"
      :href="attachment.dataUrl"
      :download="attachment.name"
      target="_blank"
      rel="noreferrer"
    >
      <span>附件</span>
      <span>
        <strong>{{ attachment.name }}</strong>
        <small>{{ attachment.mimeType }} · {{ formatAttachmentSize(attachment.size) }}</small>
      </span>
    </a>
  </section>
</template>

<style scoped>
.attachment-panel {
  display: grid;
  gap: 8px;
  border: 1px solid #e5eaf2;
  border-radius: 8px;
  background: #fff;
  padding: 10px;
}

.attachment-panel header,
.attachment-panel__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.attachment-panel header strong {
  color: #334155;
  font-size: 12px;
}

.attachment-panel header span {
  color: #8b96a8;
  font-size: 10px;
  font-weight: 800;
}

.attachment-panel__item {
  justify-content: flex-start;
  border-radius: 7px;
  padding: 8px;
  background: #f8fafc;
}

.attachment-panel__item > span:first-child {
  display: grid;
  width: 34px;
  height: 26px;
  place-items: center;
  border-radius: 6px;
  color: #2563eb;
  background: #eaf2ff;
  font-size: 9px;
  font-weight: 900;
}

.attachment-panel__item > span:last-child {
  display: grid;
  min-width: 0;
  gap: 2px;
}

.attachment-panel__item strong,
.attachment-panel__item small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-panel__item strong {
  color: #334155;
  font-size: 11px;
}

.attachment-panel__item small {
  color: #7b8797;
  font-size: 9px;
}
</style>
