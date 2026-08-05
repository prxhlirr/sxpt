<script setup lang="ts">
import type { TrainingAttachment } from '../../domain/models';
import { formatAttachmentSize } from '../../utils/trainingAttachments';

withDefaults(
  defineProps<{
    attachments?: TrainingAttachment[];
    title?: string;
    open?: boolean;
  }>(),
  {
    attachments: () => [],
    title: '教学附件',
    open: false
  }
);

function isImage(attachment: TrainingAttachment) {
  return attachment.mimeType.startsWith('image/');
}
</script>

<template>
  <details v-if="attachments.length" class="attachment-panel" :open="open">
    <summary>
      <span>{{ title }}</span>
      <em>{{ attachments.length }} 个</em>
      <small>点击收起 / 展开</small>
    </summary>
    <div class="attachment-list">
      <article v-for="attachment in attachments" :key="attachment.id">
        <a
          v-if="isImage(attachment)"
          class="attachment-preview"
          :href="attachment.dataUrl"
          target="_blank"
          rel="noreferrer"
          :title="`查看${attachment.name}`"
        >
          <img :src="attachment.dataUrl" :alt="attachment.name" />
        </a>
        <span v-else class="attachment-file-icon">附件</span>
        <div>
          <strong>{{ attachment.name }}</strong>
          <small>
            {{ attachment.mimeType }} · {{ formatAttachmentSize(attachment.size) }}
          </small>
        </div>
        <a
          class="attachment-open"
          :href="attachment.dataUrl"
          :download="attachment.name"
          target="_blank"
          rel="noreferrer"
        >
          下载
        </a>
      </article>
    </div>
  </details>
</template>

<style scoped>
.attachment-panel {
  overflow: hidden;
  border: 1px solid rgb(113 91 238 / 20%);
  border-radius: 10px;
  background: rgb(249 248 255 / 94%);
}

.attachment-panel summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 3px 10px;
  padding: 10px 12px;
  cursor: pointer;
  list-style: none;
}

.attachment-panel summary::-webkit-details-marker {
  display: none;
}

.attachment-panel summary span {
  font-size: 12px;
  font-weight: 850;
}

.attachment-panel summary em {
  border-radius: 999px;
  padding: 2px 7px;
  color: #5c4ad5;
  background: #ece8ff;
  font-size: 9px;
  font-style: normal;
  font-weight: 800;
}

.attachment-panel summary small {
  grid-column: 1 / -1;
  color: #8790a2;
  font-size: 9px;
}

.attachment-list {
  display: grid;
  gap: 7px;
  border-top: 1px solid rgb(113 91 238 / 12%);
  padding: 9px;
}

.attachment-list article {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
  border-radius: 8px;
  padding: 7px;
  background: #fff;
}

.attachment-preview,
.attachment-file-icon {
  display: grid;
  width: 38px;
  height: 38px;
  overflow: hidden;
  place-items: center;
  border-radius: 7px;
  color: #6857df;
  background: #ece9ff;
  font-size: 9px;
  font-weight: 800;
}

.attachment-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.attachment-list article > div {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.attachment-list strong,
.attachment-list small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.attachment-list strong {
  font-size: 10px;
}

.attachment-list small {
  color: #8790a2;
  font-size: 8px;
}

.attachment-open {
  color: #5c4ad5;
  font-size: 9px;
  font-weight: 800;
  text-decoration: none;
}
</style>
