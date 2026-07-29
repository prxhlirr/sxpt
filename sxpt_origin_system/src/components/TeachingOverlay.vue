<script setup lang="ts">
import { computed } from 'vue';
import {
  getCenteredGuideBubblePlacement,
  getGuideBubblePlacement,
  getGuideBubbleSize
} from '../engine/overlayPlacement';
import { useViewportSize } from '../engine/viewportSize';
import type { NodeBinding, RecordedStep } from '../types/domain';

const props = defineProps<{
  binding?: NodeBinding;
  step?: RecordedStep;
  visible: boolean;
  showHint: boolean;
  compact?: boolean;
}>();

const emit = defineEmits<{
  next: [];
  previous: [];
}>();

const hasGuideImage = computed(
  () => props.step?.kind === 'guide' && Boolean(props.step.guideImage)
);
const bubbleSize = computed(() =>
  getGuideBubbleSize(Boolean(props.compact), hasGuideImage.value)
);
const viewportSize = useViewportSize();

const highlightRect = computed(() => {
  if (props.step?.kind === 'guide') {
    return props.step.anchor?.mode === 'element'
      ? (props.step.anchor.rect ?? props.step.rect)
      : undefined;
  }
  return props.step?.rect ?? props.binding?.rect;
});

const guideBubbleStyle = computed(() => {
  const baseStyle = { width: `${bubbleSize.value.width}px` };

  if (!props.step || viewportSize.value.width === 0) {
    return baseStyle;
  }

  if (
    props.step.kind === 'guide' &&
    props.step.anchor?.mode !== 'element'
  ) {
    const centered = getCenteredGuideBubblePlacement(
      viewportSize.value,
      bubbleSize.value
    );
    return {
      ...baseStyle,
      left: `${centered.left}px`,
      top: `${centered.top}px`
    };
  }

  const placement = getGuideBubblePlacement(
    highlightRect.value ?? props.step.rect,
    viewportSize.value,
    bubbleSize.value
  );

  return {
    ...baseStyle,
    left: `${placement.left}px`,
    top: `${placement.top}px`
  };
});
</script>

<template>
  <div v-if="visible && (binding || step)" class="teaching-overlay">
    <div
      v-if="highlightRect"
      class="overlay-highlight"
      :style="{
        left: `${highlightRect.x}px`,
        top: `${highlightRect.y}px`,
        width: `${highlightRect.width}px`,
        height: `${highlightRect.height}px`
      }"
    >
      <span v-if="showHint && !step">{{ binding?.actionText }}</span>
    </div>
    <div
      v-if="showHint && step"
      class="guide-bubble"
      :class="{ compact }"
      :style="guideBubbleStyle"
    >
      <strong>{{ step.order }}. {{ step.title }}</strong>
      <p>{{ step.teachingText }}</p>
      <img
        v-if="hasGuideImage && step.guideImage"
        class="guide-bubble__image"
        :src="step.guideImage.src"
        :alt="`${step.title}说明图片`"
      />
      <small v-if="step.value">录制值：{{ step.value }}</small>
      <div v-if="!compact" class="guide-actions">
        <button type="button" @click="emit('previous')">上一步</button>
        <button type="button" @click="emit('next')">下一步</button>
      </div>
    </div>
  </div>
</template>
