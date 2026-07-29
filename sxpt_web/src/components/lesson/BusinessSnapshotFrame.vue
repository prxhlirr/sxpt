<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import type {
  BusinessPageSnapshot,
  CaptureRect
} from '../../domain/models';
import { createBusinessSnapshotDocument } from '../../utils/businessSnapshot';

const props = defineProps<{
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
  title: string;
}>();

interface BusinessActionPayload {
  actionType: 'click' | 'input' | 'select' | 'submit';
  selector: string;
  selectorCandidates?: string[];
  text: string;
}

const emit = defineEmits<{
  'business-action': [payload: BusinessActionPayload];
}>();

const frameRef = ref<HTMLIFrameElement | null>(null);
const selectors = computed(() =>
  [props.selector, ...(props.selectorCandidates ?? [])].filter(
    (selector): selector is string => Boolean(selector)
  )
);
const snapshotDocument = computed(() =>
  props.snapshot
    ? createBusinessSnapshotDocument(
        props.snapshot,
        selectors.value,
        props.interactive
      )
    : undefined
);
const recordedRectStyle = computed(() => {
  const viewport = props.recordedViewport ?? props.snapshot?.viewport;
  if (!props.rect || !viewport?.width || !viewport.height) return undefined;
  return {
    left: `${Math.max(0, (props.rect.x / viewport.width) * 100)}%`,
    top: `${Math.max(0, (props.rect.y / viewport.height) * 100)}%`,
    width: `${Math.min(100, (props.rect.width / viewport.width) * 100)}%`,
    height: `${Math.min(100, (props.rect.height / viewport.height) * 100)}%`
  };
});
const replayUrl = computed(() => {
  if (props.snapshot) return undefined;
  if (!props.fallbackUrl || props.fallbackUrl.startsWith('internal://')) {
    return '/lesson-business-capture.html';
  }
  return props.fallbackUrl;
});

function previewFallbackPage() {
  if (props.snapshot || !frameRef.value?.contentWindow) return;
  let targetOrigin = window.location.origin;
  try {
    targetOrigin = new URL(replayUrl.value ?? '/', window.location.origin).origin;
  } catch {
    targetOrigin = window.location.origin;
  }
  frameRef.value.contentWindow.postMessage(
    {
      type: 'SXPT_PREVIEW_STEP',
      selector: props.selector ?? '',
      url: props.fallbackUrl
    },
    targetOrigin
  );
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
  if (
    message.type === 'SXPT_SNAPSHOT_ACTION' ||
    message.type === 'SXPT_BUSINESS_ACTION'
  ) {
    emit('business-action', message.payload as BusinessActionPayload);
  }
}

watch(
  () => [props.fallbackUrl, props.selector, props.interactive],
  () => previewFallbackPage()
);

onMounted(() => window.addEventListener('message', handleMessage));
onBeforeUnmount(() => window.removeEventListener('message', handleMessage));
</script>

<template>
  <div
    class="business-snapshot-frame"
    :class="{ 'is-interactive': interactive }"
  >
    <iframe
      v-if="snapshotDocument"
      :key="snapshot?.capturedAt"
      ref="frameRef"
      :srcdoc="snapshotDocument"
      :title="title"
      :sandbox="interactive ? 'allow-scripts' : ''"
      referrerpolicy="no-referrer"
    />
    <iframe
      v-else
      :key="`${replayUrl}:${selector}`"
      ref="frameRef"
      :src="replayUrl"
      :title="title"
      sandbox="allow-scripts allow-same-origin"
      referrerpolicy="no-referrer"
      @load="previewFallbackPage"
    />
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
          ? `录制页面快照 · ${new Date(snapshot.capturedAt).toLocaleString()}`
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

.business-snapshot-frame.is-interactive iframe {
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
