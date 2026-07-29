<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type { BusinessPageSnapshot, CaptureRect } from '../../domain/models';

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
}

interface TargetPayload {
  selector: string;
  rect?: CaptureRect;
  url: string;
  pageTitle: string;
}

const props = defineProps<{
  src: string;
  title: string;
}>();

const emit = defineEmits<{
  'business-ready': [payload: BusinessReadyPayload];
  'business-action': [payload: BusinessActionPayload];
  'target-resolved': [payload: TargetPayload];
  'element-picked': [payload: BusinessActionPayload];
  'frame-load': [];
}>();

const frameRef = ref<HTMLIFrameElement | null>(null);
const frameSrc = ref(props.src);
const frameKey = ref(0);
const ready = ref(false);

watch(
  () => props.src,
  (src) => {
    if (src === frameSrc.value) return;
    ready.value = false;
    frameSrc.value = src;
  }
);

function isMessageFromFrame(event: MessageEvent) {
  return Boolean(frameRef.value?.contentWindow) && event.source === frameRef.value?.contentWindow;
}

function handleMessage(event: MessageEvent) {
  if (!isMessageFromFrame(event) || !event.data || typeof event.data !== 'object') {
    return;
  }

  const message = event.data as { type?: string; payload?: unknown };
  if (message.type === 'SXPT_BUSINESS_READY') {
    ready.value = true;
    emit('business-ready', message.payload as BusinessReadyPayload);
    return;
  }
  if (message.type === 'SXPT_BUSINESS_ACTION') {
    emit('business-action', message.payload as BusinessActionPayload);
    return;
  }
  if (message.type === 'SXPT_TARGET_RECT') {
    emit('target-resolved', message.payload as TargetPayload);
    return;
  }
  if (message.type === 'SXPT_ELEMENT_PICKED') {
    emit('element-picked', message.payload as BusinessActionPayload);
  }
}

function post(message: unknown) {
  frameRef.value?.contentWindow?.postMessage(message, window.location.origin);
}

function setRecording(enabled: boolean) {
  post({ type: 'SXPT_SET_RECORDING', enabled });
}

function resolveTarget(selector: string, url?: string) {
  post({ type: 'SXPT_RESOLVE_TARGET', selector, url });
}

function startElementPick() {
  post({ type: 'SXPT_START_ELEMENT_PICK' });
}

function cancelElementPick() {
  post({ type: 'SXPT_CANCEL_ELEMENT_PICK' });
}

function previewStep(selector: string, url?: string) {
  post({ type: 'SXPT_PREVIEW_STEP', selector, url });
}

function reload() {
  ready.value = false;
  frameKey.value += 1;
}

function handleLoad() {
  ready.value = false;
  emit('frame-load');
  post({ type: 'SXPT_REQUEST_READY' });
}

onMounted(() => window.addEventListener('message', handleMessage));
onBeforeUnmount(() => window.removeEventListener('message', handleMessage));

defineExpose({
  ready,
  setRecording,
  resolveTarget,
  startElementPick,
  cancelElementPick,
  previewStep,
  reload
});
</script>

<template>
  <iframe
    :key="frameKey"
    ref="frameRef"
    class="business-capture-frame"
    :src="frameSrc"
    :title="title"
    @load="handleLoad"
  />
</template>

<style scoped>
.business-capture-frame {
  display: block;
  width: 100%;
  height: 100%;
  min-height: 620px;
  border: 0;
  background: #eef2f7;
}
</style>
