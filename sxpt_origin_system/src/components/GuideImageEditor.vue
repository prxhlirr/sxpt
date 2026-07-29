<script setup lang="ts">
import {
  GUIDE_IMAGE_ACCEPT,
  takeSelectedGuideImageFile
} from '../engine/guideImage';
import type { GuideImage } from '../types/domain';

defineProps<{
  image?: GuideImage;
  busy?: boolean;
}>();

const emit = defineEmits<{
  'file-selected': [file: File];
  remove: [];
}>();

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement;
  const selectedFile = takeSelectedGuideImageFile(input);
  if (selectedFile) emit('file-selected', selectedFile);
}
</script>

<template>
  <section class="guide-image-editor" aria-label="说明图片">
    <div class="guide-image-editor__header">
      <strong>说明图片</strong>
      <small>PNG、JPEG 或 WebP，原图不超过 5MB</small>
    </div>
    <img
      v-if="image"
      class="guide-image-editor__preview"
      :src="image.src"
      :alt="image.name"
    />
    <span v-if="image" class="guide-image-editor__name">{{ image.name }}</span>
    <div class="guide-image-editor__actions">
      <label class="secondary-action guide-image-editor__select">
        {{ image ? '替换图片' : '选择图片' }}
        <input
          type="file"
          :accept="GUIDE_IMAGE_ACCEPT"
          :disabled="busy"
          @change="handleFileChange"
        />
      </label>
      <button
        v-if="image"
        type="button"
        class="secondary-action"
        :disabled="busy"
        @click="emit('remove')"
      >
        删除图片
      </button>
    </div>
  </section>
</template>
