<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type {
  BusinessPageSnapshot,
  CaptureRect
} from '../../domain/models';
import { createBusinessSnapshotDocument } from '../../utils/businessSnapshot';
import {
  calculateContainedViewport,
  mapRectToFilledViewport,
  mapRectToContainedViewport,
  normalizeViewport
} from '../../utils/viewportScaling';

const props = withDefaults(
  defineProps<{
    snapshot?: BusinessPageSnapshot;
    fallbackUrl?: string;
    selector?: string;
    selectorCandidates?: string[];
    rect?: CaptureRect;
    recordedViewport?: {
      width: number;
      height: number;
    };
    interactive?: boolean;
    clearFormValues?: boolean;
    fitMode?: 'fill' | 'contain';
    title: string;
  }>(),
  {
    fitMode: 'fill'
  }
);

interface BusinessActionPayload {
  actionType: 'click' | 'input' | 'select' | 'submit';
  selector: string;
  selectorCandidates?: string[];
  text: string;
}

const emit = defineEmits<{
  'business-action': [payload: BusinessActionPayload];
}>();

const containerRef = ref<HTMLElement | null>(null);
const frameRef = ref<HTMLIFrameElement | null>(null);
const frameReady = ref(false);
const lastImmediateAction = ref<{ selector: string; at: number }>();
const containerSize = ref({ width: 0, height: 0 });
let frameReadyTimer: number | undefined;
let resizeObserver: ResizeObserver | undefined;
const selectors = computed(() =>
  [props.selector, ...(props.selectorCandidates ?? [])].filter(
    (selector): selector is string => Boolean(selector)
  )
);
const snapshotHasTarget = computed(() => {
  if (!props.snapshot || !props.interactive || !selectors.value.length) {
    return Boolean(props.snapshot);
  }
  const body = new DOMParser().parseFromString(
    `<body>${props.snapshot.html}</body>`,
    'text/html'
  ).body;
  return selectors.value.some((selector) => {
    try {
      return Boolean(body.querySelector(selector));
    } catch {
      return false;
    }
  });
});
const snapshotDocument = computed(() =>
  props.snapshot && snapshotHasTarget.value
    ? createBusinessSnapshotDocument(
        props.snapshot,
        selectors.value,
        props.interactive,
        props.clearFormValues
      )
    : undefined
);
const resolutionViewport = computed(() =>
  normalizeViewport(props.recordedViewport ?? props.snapshot?.viewport)
);
const containedViewport = computed(() => {
  if (
    props.fitMode !== 'contain' ||
    !resolutionViewport.value ||
    containerSize.value.width <= 0 ||
    containerSize.value.height <= 0
  ) {
    return undefined;
  }
  return calculateContainedViewport(
    resolutionViewport.value,
    containerSize.value
  );
});
const frameViewportStyle = computed(() => {
  if (props.fitMode === 'fill') {
    return {
      inset: '0',
      width: '100%',
      height: '100%',
      transform: 'none'
    };
  }
  const viewport = resolutionViewport.value;
  const placement = containedViewport.value;
  if (!viewport || !placement) return undefined;
  return {
    width: `${viewport.width}px`,
    height: `${viewport.height}px`,
    left: `${placement.left}px`,
    top: `${placement.top}px`,
    transform: `scale(${placement.scale})`,
    transformOrigin: 'top left'
  };
});
const recordedRectStyle = computed(() => {
  if (!snapshotDocument.value) return undefined;
  const viewport = resolutionViewport.value;
  if (
    !props.rect ||
    !viewport ||
    containerSize.value.width <= 0 ||
    containerSize.value.height <= 0
  ) {
    return undefined;
  }
  const mapped =
    props.fitMode === 'fill'
      ? mapRectToFilledViewport(props.rect, viewport, containerSize.value)
      : mapRectToContainedViewport(props.rect, viewport, containerSize.value);
  return {
    left: `${mapped.left}px`,
    top: `${mapped.top}px`,
    width: `${mapped.width}px`,
    height: `${mapped.height}px`
  };
});
const replayUrl = computed(() => {
  if (snapshotDocument.value) return undefined;
  if (!props.fallbackUrl || props.fallbackUrl.startsWith('internal://')) {
    return '/lesson-business-capture.html';
  }
  return props.fallbackUrl;
});
const selectorSignature = computed(() => selectors.value.join('|'));
const snapshotFrameKey = computed(
  () =>
    [
      props.snapshot?.capturedAt ?? 'snapshot',
      props.interactive ? 'interactive' : 'readonly',
      props.clearFormValues ? 'empty-form' : 'recorded-form',
      selectorSignature.value
    ].join(':')
);
const fallbackFrameKey = computed(
  () =>
    [
      replayUrl.value ?? 'fallback',
      props.interactive ? 'interactive' : 'readonly',
      props.clearFormValues ? 'empty-form' : 'recorded-form',
      selectorSignature.value
    ].join(':')
);

function previewFallbackPage() {
  if (snapshotDocument.value || !frameRef.value?.contentWindow) return;
  let targetOrigin = window.location.origin;
  try {
    targetOrigin = new URL(replayUrl.value ?? '/', window.location.origin).origin;
  } catch {
    targetOrigin = window.location.origin;
  }
  frameRef.value.contentWindow.postMessage(
    {
      type: 'SXPT_SET_STUDENT_MODE',
      mode: props.clearFormValues ? 'PRACTICE' : 'LEARNING'
    },
    targetOrigin
  );
  frameRef.value.contentWindow.postMessage(
    {
      type: 'SXPT_PREVIEW_STEP',
      selector: props.selector ?? '',
      url: props.fallbackUrl
    },
    targetOrigin
  );
}

function markFrameReady() {
  if (frameReadyTimer !== undefined) {
    window.clearTimeout(frameReadyTimer);
    frameReadyTimer = undefined;
  }
  frameReady.value = true;
}

function handleFrameLoad() {
  if (snapshotDocument.value) {
    markFrameReady();
    return;
  }
  previewFallbackPage();
  frameReadyTimer = window.setTimeout(markFrameReady, 300);
}

function handleMessage(event: MessageEvent) {
  if (
    event.source !== frameRef.value?.contentWindow ||
    !event.data ||
    typeof event.data !== 'object'
  ) {
    return;
  }
  const message = event.data as { type?: string; payload?: unknown };
  if (message.type === 'SXPT_TARGET_RECT') {
    markFrameReady();
    return;
  }
  if (message.type === 'SXPT_BUSINESS_INTERACTION') {
    const payload = message.payload as BusinessActionPayload;
    lastImmediateAction.value = {
      selector: payload.selector,
      at: Date.now()
    };
    emit('business-action', payload);
    return;
  }
  if (
    message.type === 'SXPT_SNAPSHOT_ACTION' ||
    message.type === 'SXPT_BUSINESS_ACTION'
  ) {
    const payload = message.payload as BusinessActionPayload;
    if (
      message.type === 'SXPT_BUSINESS_ACTION' &&
      lastImmediateAction.value?.selector === payload.selector &&
      Date.now() - lastImmediateAction.value.at < 2_000
    ) {
      lastImmediateAction.value = undefined;
      return;
    }
    emit('business-action', payload);
  }
}

function updateContainerSize() {
  const rect = containerRef.value?.getBoundingClientRect();
  if (!rect) return;
  containerSize.value = {
    width: rect.width,
    height: rect.height
  };
}

watch(
  () => [snapshotFrameKey.value, fallbackFrameKey.value],
  () => {
    if (frameReadyTimer !== undefined) {
      window.clearTimeout(frameReadyTimer);
      frameReadyTimer = undefined;
    }
    frameReady.value = false;
    lastImmediateAction.value = undefined;
  }
);

onMounted(() => {
  window.addEventListener('message', handleMessage);
  window.addEventListener('resize', updateContainerSize);
  updateContainerSize();
  if (typeof ResizeObserver !== 'undefined' && containerRef.value) {
    resizeObserver = new ResizeObserver(updateContainerSize);
    resizeObserver.observe(containerRef.value);
  }
});
onBeforeUnmount(() => {
  if (frameReadyTimer !== undefined) window.clearTimeout(frameReadyTimer);
  resizeObserver?.disconnect();
  window.removeEventListener('resize', updateContainerSize);
  window.removeEventListener('message', handleMessage);
});
</script>

<template>
  <div
    ref="containerRef"
    class="business-snapshot-frame"
    :class="{
      'is-interactive': interactive && frameReady,
      'is-loading': interactive && !frameReady,
      'has-recorded-viewport': Boolean(resolutionViewport),
      'fills-viewport': fitMode === 'fill'
    }"
  >
    <iframe
      v-if="snapshotDocument"
      :key="snapshotFrameKey"
      ref="frameRef"
      :srcdoc="snapshotDocument"
      :title="title"
      :sandbox="interactive ? 'allow-scripts' : ''"
      :style="frameViewportStyle"
      referrerpolicy="no-referrer"
      @load="handleFrameLoad"
    />
    <iframe
      v-else
      :key="fallbackFrameKey"
      ref="frameRef"
      :src="replayUrl"
      :title="title"
      sandbox="allow-scripts allow-same-origin"
      :style="frameViewportStyle"
      referrerpolicy="no-referrer"
      @load="handleFrameLoad"
    />
    <span v-if="interactive && !frameReady" class="frame-loading-state">
      正在初始化当前操作…
    </span>
    <span
      v-if="recordedRectStyle"
      class="recorded-rect-highlight"
      :style="recordedRectStyle"
      aria-hidden="true"
    >
      <b>操作位置</b>
    </span>
    <span class="snapshot-status">
      {{
        snapshot
          ? snapshotDocument
            ? `录制页面快照 · ${new Date(snapshot.capturedAt).toLocaleString()}`
            : '录制快照缺少目标元素 · 已切换到同一业务系统操作页'
          : '旧节点无页面快照 · 正在按录制地址重建页面'
      }}
    </span>
  </div>
</template>

<style scoped>
.business-snapshot-frame {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #eef2f7;
}

.business-snapshot-frame iframe {
  display: block;
  width: 100%;
  height: 100%;
  border: 0;
  pointer-events: none;
  background: #eef2f7;
}

.business-snapshot-frame.has-recorded-viewport iframe {
  position: absolute;
  max-width: none;
  max-height: none;
}

.business-snapshot-frame.fills-viewport iframe {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  max-width: none;
  max-height: none;
  transform: none;
}

.business-snapshot-frame.is-interactive iframe {
  pointer-events: auto;
}

.frame-loading-state {
  position: absolute;
  z-index: 8;
  inset: 0;
  display: grid;
  place-items: center;
  color: #6253d5;
  background: rgb(244 246 251 / 72%);
  backdrop-filter: blur(2px);
  font-size: 11px;
  font-weight: 800;
  pointer-events: auto;
}

.snapshot-status {
  position: absolute;
  right: 12px;
  bottom: 12px;
  border: 1px solid rgb(255 255 255 / 70%);
  border-radius: 999px;
  padding: 6px 10px;
  color: #eef6ff;
  background: rgb(20 28 45 / 72%);
  box-shadow: 0 8px 22px rgb(10 18 34 / 16%);
  backdrop-filter: blur(8px);
  font-size: 9px;
  pointer-events: none;
}

.recorded-rect-highlight {
  position: absolute;
  z-index: 4;
  min-width: 18px;
  min-height: 18px;
  border: 4px solid #ff8a1f;
  border-radius: 8px;
  box-shadow: 0 0 0 8px rgb(255 138 31 / 22%), 0 0 28px rgb(255 92 38 / 62%);
  pointer-events: none;
  animation: recorded-rect-pulse 1.2s ease-in-out infinite alternate;
}

.recorded-rect-highlight b {
  position: absolute;
  top: -34px;
  left: -4px;
  border-radius: 999px;
  padding: 6px 10px;
  color: #fff;
  background: #e66b00;
  box-shadow: 0 8px 20px rgb(116 50 0 / 26%);
  font-size: 10px;
  white-space: nowrap;
}

@keyframes recorded-rect-pulse {
  from {
    border-color: #ff9f1c;
    box-shadow: 0 0 0 5px rgb(255 159 28 / 18%), 0 0 18px rgb(255 159 28 / 48%);
  }
  to {
    border-color: #ff5d2e;
    box-shadow: 0 0 0 12px rgb(255 93 46 / 20%), 0 0 34px rgb(255 93 46 / 76%);
  }
}
</style>
