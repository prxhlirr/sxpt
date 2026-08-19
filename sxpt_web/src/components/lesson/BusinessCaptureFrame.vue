<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type {
  BusinessPageSnapshot,
  CaptureRect,
  PracticeEvidenceScreenshot
} from '../../domain/models';
import {
  calculateContainedViewport,
  DEFAULT_RECORDING_VIEWPORT,
  mapRectToContainedViewport,
  mapRectToFilledViewport,
  normalizeViewport
} from '../../utils/viewportScaling';
import {
  createBusinessStepPreviewClearMessages,
  createBusinessStepPreviewMessages,
  type BusinessStepPreviewLocator
} from '../../utils/businessStepPreview';

interface BusinessReadyPayload {
  url: string;
  pageTitle: string;
}

interface BusinessActionPayload extends BusinessReadyPayload {
  actionType: 'click' | 'input' | 'select' | 'submit';
  selector: string;
  selectorCandidates?: string[];
  text: string;
  valueMasked?: string;
  rect: CaptureRect;
  recordedViewport?: {
    width: number;
    height: number;
  };
  pageSnapshot?: BusinessPageSnapshot;
  evidenceScreenshot?: PracticeEvidenceScreenshot;
}

interface TargetPayload {
  selector: string;
  rect?: CaptureRect;
  url: string;
  pageTitle: string;
  viewport?: {
    width: number;
    height: number;
  };
}

const props = withDefaults(
  defineProps<{
    src: string;
    title: string;
    fitMode?: 'fill' | 'contain';
    studentMode?: 'LEARNING' | 'PRACTICE';
    monitorActions?: boolean;
    showResolution?: boolean;
    allowedOrigins?: string[];
  }>(),
  {
    fitMode: 'fill',
    monitorActions: false,
    showResolution: true,
    allowedOrigins: () => []
  }
);

const emit = defineEmits<{
  'business-ready': [payload: BusinessReadyPayload];
  'business-action': [payload: BusinessActionPayload];
  'business-interaction': [];
  'target-resolved': [payload: TargetPayload];
  'element-picked': [payload: BusinessActionPayload];
  'element-pick-cancelled': [];
  'frame-load': [];
}>();

const containerRef = ref<HTMLElement | null>(null);
const frameRef = ref<HTMLIFrameElement | null>(null);
const frameSrc = ref(props.src);
const frameKey = ref(0);
const ready = ref(false);
const recordingEnabled = ref(false);
const elementPickRequested = ref(false);
const containerSize = ref({ width: 0, height: 0 });
const activeFrameOrigin = ref('');
const previewRect = ref<CaptureRect>();
const previewViewport = ref<{ width: number; height: number }>();
let resizeObserver: ResizeObserver | undefined;
let lastActionSignature = '';
let lastActionAt = 0;
let previewRequestSequence = 0;
let activePreviewRequest:
  | {
      requestId: string;
      locator: BusinessStepPreviewLocator;
    }
  | undefined;
const frameOrigin = computed(() => {
  try {
    return new URL(frameSrc.value, window.location.href).origin;
  } catch {
    return '';
  }
});
const trustedFrameOrigins = computed(() => {
  const origins = new Set<string>();
  if (frameOrigin.value) origins.add(frameOrigin.value);
  for (const candidate of props.allowedOrigins) {
    try {
      origins.add(new URL(candidate, window.location.href).origin);
    } catch {
      // Ignore malformed optional origins; the iframe source remains trusted.
    }
  }
  return origins;
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
  const viewport = DEFAULT_RECORDING_VIEWPORT;
  if (containerSize.value.width <= 0 || containerSize.value.height <= 0) {
    return {
      width: `${viewport.width}px`,
      height: `${viewport.height}px`
    };
  }
  const placement = calculateContainedViewport(
    viewport,
    containerSize.value
  );
  return {
    width: `${viewport.width}px`,
    height: `${viewport.height}px`,
    left: `${placement.left}px`,
    top: `${placement.top}px`,
    transform: `scale(${placement.scale})`,
    transformOrigin: 'top left'
  };
});
const previewRectStyle = computed(() => {
  if (
    !previewRect.value ||
    containerSize.value.width <= 0 ||
    containerSize.value.height <= 0
  ) {
    return undefined;
  }
  const viewport = normalizeViewport(
    previewViewport.value,
    props.fitMode === 'contain'
      ? DEFAULT_RECORDING_VIEWPORT
      : containerSize.value
  );
  if (!viewport) return undefined;
  const mapped =
    props.fitMode === 'contain'
      ? mapRectToContainedViewport(
          previewRect.value,
          viewport,
          containerSize.value
        )
      : mapRectToFilledViewport(
          previewRect.value,
          viewport,
          containerSize.value
        );
  if (mapped.width <= 0 || mapped.height <= 0) return undefined;
  return {
    left: `${mapped.left}px`,
    top: `${mapped.top}px`,
    width: `${mapped.width}px`,
    height: `${mapped.height}px`
  };
});

watch(
  () => props.src,
  (src) => {
    if (src === frameSrc.value) return;
    ready.value = false;
    activeFrameOrigin.value = '';
    frameSrc.value = src;
  }
);

watch(
  () => [props.studentMode, props.monitorActions] as const,
  () => {
    if (ready.value) syncFrameControls();
  }
);

function isMessageFromFrame(event: MessageEvent) {
  return (
    Boolean(frameRef.value?.contentWindow) &&
    event.source === frameRef.value?.contentWindow &&
    trustedFrameOrigins.value.has(event.origin)
  );
}

function emitBusinessAction(payload: BusinessActionPayload) {
  if (!payload || !payload.selector || !payload.actionType) return;
  const signature = [
    payload.actionType,
    payload.url,
    payload.selector,
    payload.valueMasked ?? ''
  ].join('|');
  const at = Date.now();
  if (signature === lastActionSignature && at - lastActionAt < 450) return;
  lastActionSignature = signature;
  lastActionAt = at;
  emit('business-action', payload);
}

function handleMessage(event: MessageEvent) {
  if (!isMessageFromFrame(event) || !event.data || typeof event.data !== 'object') {
    return;
  }

  const message = event.data as {
    type?: string;
    payload?: unknown;
    [key: string]: unknown;
  };
  if (message.type === 'SXPT_BUSINESS_READY' || message.type === 'BUSINESS_READY') {
    activeFrameOrigin.value = event.origin;
    ready.value = true;
    syncFrameControls();
    const payload =
      message.type === 'SXPT_BUSINESS_READY'
        ? (message.payload as BusinessReadyPayload)
        : {
            url: String(message.url ?? frameSrc.value),
            pageTitle: String(message.pageTitle ?? message.title ?? props.title)
          };
    emit('business-ready', payload);
    return;
  }
  if (message.type === 'BUSINESS_INTERACTION') {
    emit('business-interaction');
    return;
  }
  if (
    message.type === 'SXPT_BUSINESS_ACTION' ||
    message.type === 'SXPT_BUSINESS_INTERACTION' ||
    message.type === 'BUSINESS_ACTION'
  ) {
    emitBusinessAction(
      (message.type.startsWith('SXPT_')
        ? message.payload
        : message) as BusinessActionPayload
    );
    return;
  }
  if (message.type === 'SXPT_TARGET_RECT') {
    const payload = message.payload as TargetPayload;
    if (activePreviewRequest && payload?.rect) {
      previewRect.value = payload.rect;
      previewViewport.value = payload.viewport;
    }
    emit('target-resolved', payload);
    return;
  }
  if (message.type === 'TARGET_RESOLUTION_RESULT') {
    if (
      !activePreviewRequest ||
      message.requestId !== activePreviewRequest.requestId
    ) {
      return;
    }
    if (message.success && message.rect) {
      previewRect.value = message.rect as CaptureRect;
      previewViewport.value = message.viewport as
        | { width: number; height: number }
        | undefined;
      emit('target-resolved', {
        selector: String(
          message.selector ?? activePreviewRequest.locator.selector
        ),
        rect: message.rect as CaptureRect,
        url: String(message.url ?? activePreviewRequest.locator.url ?? frameSrc.value),
        pageTitle: props.title,
        viewport: previewViewport.value
      });
    }
    return;
  }
  if (message.type === 'SXPT_ELEMENT_PICKED') {
    elementPickRequested.value = false;
    emit('element-picked', message.payload as BusinessActionPayload);
    return;
  }
  if (message.type === 'ELEMENT_PICKED') {
    elementPickRequested.value = false;
    emit('element-picked', {
      ...(message as unknown as BusinessActionPayload),
      actionType: (message.actionType as BusinessActionPayload['actionType']) ?? 'click',
      url: String(message.url ?? frameSrc.value),
      pageTitle: String(message.pageTitle ?? props.title)
    });
    return;
  }
  if (
    message.type === 'SXPT_ELEMENT_PICK_CANCELLED' ||
    message.type === 'ELEMENT_PICK_CANCELLED'
  ) {
    elementPickRequested.value = false;
    emit('element-pick-cancelled');
  }
}

function post(message: unknown) {
  const targetOrigin = activeFrameOrigin.value || frameOrigin.value;
  if (!targetOrigin) return;
  frameRef.value?.contentWindow?.postMessage(message, targetOrigin);
}

function setRecording(enabled: boolean) {
  recordingEnabled.value = enabled;
  if (ready.value) postRecordingState();
}

function resolveTarget(selector: string, url?: string) {
  post({ type: 'SXPT_RESOLVE_TARGET', selector, url });
}

function startElementPick() {
  elementPickRequested.value = true;
  if (!ready.value) return;
  postElementPickState();
}

function postElementPickState() {
  post({ type: 'SXPT_START_ELEMENT_PICK' });
  post({ type: 'START_ELEMENT_PICK' });
}

function cancelElementPick() {
  elementPickRequested.value = false;
  post({ type: 'SXPT_CANCEL_ELEMENT_PICK' });
  post({ type: 'CANCEL_ELEMENT_PICK' });
}

function postRecordingState() {
  if (props.monitorActions) {
    post({ type: 'SET_RECORDING_STATE', enabled: true, monitorOnly: true });
    return;
  }
  post({ type: 'SXPT_SET_RECORDING', enabled: recordingEnabled.value });
  post({ type: 'SET_RECORDING_STATE', enabled: recordingEnabled.value });
}

function syncFrameControls() {
  postRecordingState();
  if (props.studentMode) {
    post({ type: 'SXPT_SET_STUDENT_MODE', mode: props.studentMode });
  }
  if (elementPickRequested.value) postElementPickState();
  postActivePreview();
}

function postActivePreview() {
  if (!activePreviewRequest) return;
  for (const message of createBusinessStepPreviewMessages(
    activePreviewRequest.requestId,
    activePreviewRequest.locator
  )) {
    post(message);
  }
}

function previewStep(
  selector: string,
  url?: string,
  selectorCandidates?: string[],
  fallbackRect?: CaptureRect,
  fallbackViewport?: { width: number; height: number }
) {
  previewRequestSequence += 1;
  activePreviewRequest = {
    requestId: `step-preview-${Date.now()}-${previewRequestSequence}`,
    locator: { selector, selectorCandidates, url }
  };
  previewRect.value = fallbackRect;
  previewViewport.value = fallbackViewport;
  postActivePreview();
}

function clearStepPreview() {
  activePreviewRequest = undefined;
  previewRect.value = undefined;
  previewViewport.value = undefined;
  for (const message of createBusinessStepPreviewClearMessages()) {
    post(message);
  }
}

function reload() {
  ready.value = false;
  elementPickRequested.value = false;
  frameKey.value += 1;
}

function handleLoad() {
  ready.value = false;
  emit('frame-load');
  post({ type: 'SXPT_REQUEST_READY' });
  post({ type: 'REQUEST_BUSINESS_READY' });
}

function updateContainerSize() {
  const rect = containerRef.value?.getBoundingClientRect();
  if (!rect) return;
  containerSize.value = {
    width: rect.width,
    height: rect.height
  };
}

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
  resizeObserver?.disconnect();
  window.removeEventListener('resize', updateContainerSize);
  window.removeEventListener('message', handleMessage);
});

defineExpose({
  ready,
  setRecording,
  resolveTarget,
  startElementPick,
  cancelElementPick,
  previewStep,
  clearStepPreview,
  reload
});
</script>

<template>
  <div
    ref="containerRef"
    class="business-capture-frame"
    :class="`fit-${fitMode}`"
  >
    <iframe
      :key="frameKey"
      ref="frameRef"
      class="business-capture-frame__viewport"
      :src="frameSrc"
      :style="frameViewportStyle"
      :title="title"
      @load="handleLoad"
    />
    <span
      v-if="previewRectStyle"
      class="business-capture-frame__preview-highlight"
      :style="previewRectStyle"
      aria-hidden="true"
    >
      <b>已绑定元素</b>
    </span>
    <span v-if="showResolution" class="business-capture-frame__resolution">
      {{ fitMode === 'fill' ? '自适应全屏' : '等比例视口' }}
      {{ Math.round(containerSize.width) }} × {{ Math.round(containerSize.height) }}
    </span>
  </div>
</template>

<style scoped>
.business-capture-frame {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: #18202d;
}

.business-capture-frame__viewport {
  position: absolute;
  display: block;
  max-width: none;
  max-height: none;
  border: 0;
  background: #eef2f7;
}

.business-capture-frame__resolution {
  position: absolute;
  right: 12px;
  bottom: 12px;
  border: 1px solid rgb(255 255 255 / 45%);
  border-radius: 999px;
  padding: 5px 9px;
  color: #eef4ff;
  background: rgb(20 28 45 / 68%);
  backdrop-filter: blur(8px);
  font-size: 8px;
  font-weight: 800;
  pointer-events: none;
}

.business-capture-frame__preview-highlight {
  position: absolute;
  z-index: 3;
  box-sizing: border-box;
  border: 3px solid #6d5dfc;
  border-radius: 6px;
  background: rgb(109 93 252 / 9%);
  box-shadow:
    0 0 0 5px rgb(109 93 252 / 18%),
    0 8px 28px rgb(35 24 118 / 24%);
  pointer-events: none;
}

.business-capture-frame__preview-highlight > b {
  position: absolute;
  bottom: calc(100% + 5px);
  left: -3px;
  max-width: 180px;
  overflow: hidden;
  border-radius: 5px;
  padding: 4px 8px;
  color: #fff;
  background: #5948dc;
  font-size: 11px;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
