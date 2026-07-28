<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import {
  isBusinessNavigatingMessage,
  isBusinessReadyMessage,
  isElementPickedMessage,
  isPlaybackResultMessage,
  isTargetResolutionResultMessage,
  type BusinessNavigatingMessage,
  type BusinessReadyMessage,
  type ElementPickedMessage,
  type PlaybackResultMessage,
  type TargetResolutionResultMessage
} from '../engine/captureProtocol';
import { shouldForceBoundFrameReload } from '../engine/businessPageState';
import {
  isMessageFromBusinessFrame,
  resolveCurrentFrameTargetOrigin,
  resolveFrameTargetOrigin
} from '../engine/frameMessaging';
import {
  createStepPlaybackMessage,
  createTargetResolutionMessage
} from '../engine/stepPlayback';
import type { BusinessActionPayload, RecordedStep } from '../types/domain';

const props = defineProps<{
  src: string;
  title: string;
}>();

const emit = defineEmits<{
  'business-action': [payload: BusinessActionPayload];
  'frame-load': [];
  'frame-navigating': [payload: BusinessNavigatingMessage];
  'business-ready': [payload: BusinessReadyMessage];
  'playback-result': [payload: PlaybackResultMessage];
  'element-picked': [payload: ElementPickedMessage];
}>();

const frameRef = ref<HTMLIFrameElement | null>(null);
const frameSrc = ref(props.src);
const frameKey = ref(0);
const currentUrl = ref(props.src);
const ready = ref(false);
const pendingPlayback = new Map<
  string,
  {
    resolve: (result: PlaybackResultMessage) => void;
    reject: (error: Error) => void;
    timer: number;
  }
>();
const pendingTargetResolution = new Map<
  string,
  {
    resolve: (result: TargetResolutionResultMessage) => void;
    reject: (error: Error) => void;
    timer: number;
  }
>();

watch(
  () => props.src,
  (nextSrc) => {
    if (nextSrc !== frameSrc.value) navigate(nextSrc);
  }
);

function handleMessage(event: MessageEvent) {
  if (
    !isMessageFromBusinessFrame(
      event.source,
      frameRef.value?.contentWindow,
      event.origin,
      resolveFrameTargetOrigin(frameSrc.value, window.location.origin)
    )
  ) {
    return;
  }

  if (isBusinessNavigatingMessage(event.data)) {
    ready.value = false;
    emit('frame-navigating', event.data);
    rejectPendingRequests('业务页面正在跳转');
    return;
  }

  if (isBusinessReadyMessage(event.data)) {
    ready.value = true;
    currentUrl.value = event.data.url;
    emit('business-ready', event.data);
    return;
  }

  if (isPlaybackResultMessage(event.data)) {
    const pending = pendingPlayback.get(event.data.requestId);
    if (pending) {
      window.clearTimeout(pending.timer);
      pendingPlayback.delete(event.data.requestId);
      if (event.data.success) pending.resolve(event.data);
      else pending.reject(new Error(event.data.error ?? '回放失败'));
    }
    emit('playback-result', event.data);
    return;
  }

  if (isTargetResolutionResultMessage(event.data)) {
    const pending = pendingTargetResolution.get(event.data.requestId);
    if (pending) {
      window.clearTimeout(pending.timer);
      pendingTargetResolution.delete(event.data.requestId);
      if (event.data.success) pending.resolve(event.data);
      else pending.reject(new Error(event.data.error ?? '目标元素解析失败'));
    }
    return;
  }

  if (isElementPickedMessage(event.data)) {
    emit('element-picked', event.data);
    return;
  }

  if (!event.data || event.data.type !== 'BUSINESS_ACTION') return;

  emit('business-action', {
    actionType: event.data.actionType,
    url: event.data.url,
    selector: event.data.selector,
    selectorCandidates: event.data.selectorCandidates,
    text: event.data.text,
    value: event.data.value,
    rect: event.data.rect,
    timestamp: event.data.timestamp,
    clientEventId: event.data.clientEventId,
    sdkSessionId: event.data.sdkSessionId,
    sequenceNo: event.data.sequenceNo,
    stableKey: event.data.stableKey,
    pageTitle: event.data.pageTitle,
    inputValueMasked: event.data.inputValueMasked,
    recordedViewport: event.data.recordedViewport
  });
}

onMounted(() => {
  window.addEventListener('message', handleMessage);
});

onBeforeUnmount(() => {
  window.removeEventListener('message', handleMessage);
  rejectPendingRequests('业务页面已关闭');
});

function post(message: unknown) {
  frameRef.value?.contentWindow?.postMessage(
    message,
    resolveCurrentFrameTargetOrigin(
      frameSrc.value,
      currentUrl.value,
      ready.value,
      window.location.origin
    )
  );
}

function navigate(url: string) {
  rejectPendingRequests('业务页面正在跳转');
  const forceReload = shouldForceBoundFrameReload(
    frameSrc.value,
    currentUrl.value,
    url,
    ready.value
  );
  ready.value = false;
  currentUrl.value = url;
  if (forceReload) frameKey.value += 1;
  else frameSrc.value = url;
}

function reload(startUrl = props.src) {
  rejectPendingRequests('业务页面正在重置');
  ready.value = false;
  currentUrl.value = startUrl;
  frameSrc.value = startUrl;
  frameKey.value += 1;
}

function setRecording(
  enabled: boolean,
  captureSessionId?: string,
  sequenceStart?: number
) {
  post({
    type: 'SET_RECORDING_STATE',
    enabled,
    captureSessionId,
    sequenceStart
  });
}

function playStep(step: RecordedStep): Promise<PlaybackResultMessage> {
  const requestId = createRequestId();
  return new Promise((resolve, reject) => {
    const timer = window.setTimeout(() => {
      pendingPlayback.delete(requestId);
      reject(new Error('业务页面回放超时'));
    }, 5000);
    pendingPlayback.set(requestId, { resolve, reject, timer });
    post(createStepPlaybackMessage(step, requestId));
  });
}

function resolveStepTarget(
  step: RecordedStep
): Promise<TargetResolutionResultMessage> {
  const requestId = createRequestId('resolve');
  return new Promise((resolve, reject) => {
    const timer = window.setTimeout(() => {
      pendingTargetResolution.delete(requestId);
      reject(new Error('目标元素解析超时'));
    }, 5000);
    pendingTargetResolution.set(requestId, { resolve, reject, timer });
    post(createTargetResolutionMessage(step, requestId));
  });
}

function startElementPick() {
  post({ type: 'START_ELEMENT_PICK' });
}

function cancelElementPick() {
  post({ type: 'CANCEL_ELEMENT_PICK' });
}

function rejectPendingRequests(message: string) {
  for (const pending of pendingPlayback.values()) {
    window.clearTimeout(pending.timer);
    pending.reject(new Error(message));
  }
  pendingPlayback.clear();
  for (const pending of pendingTargetResolution.values()) {
    window.clearTimeout(pending.timer);
    pending.reject(new Error(message));
  }
  pendingTargetResolution.clear();
}

function handleFrameLoad() {
  ready.value = false;
  emit('frame-load');
  rejectPendingRequests('业务页面已重新加载');
  post({ type: 'REQUEST_BUSINESS_READY' });
}

function createRequestId(prefix = 'play') {
  return typeof crypto !== 'undefined' && crypto.randomUUID
    ? `${prefix}-${crypto.randomUUID()}`
    : `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

defineExpose({
  currentUrl,
  ready,
  navigate,
  reload,
  setRecording,
  playStep,
  resolveStepTarget,
  startElementPick,
  cancelElementPick
});
</script>

<template>
  <iframe
    :key="frameKey"
    ref="frameRef"
    class="business-frame"
    :src="frameSrc"
    :title="title"
    @load="handleFrameLoad"
  />
</template>
