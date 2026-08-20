<script setup lang="ts">
import {
  audioPlugin,
  createViewer,
  fallbackPlugin,
  imagePlugin,
  officePlugin,
  pdfPlugin,
  textPlugin,
  videoPlugin,
  type FileViewer,
  type PreviewPlugin
} from '@open-file-viewer/core';
import '@open-file-viewer/core/style.css';
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import type { TrainingAttachment } from '../../domain/models';
import { formatAttachmentSize } from '../../utils/trainingAttachments';
import {
  beginOverlayDrag,
  clampOverlayPosition,
  updateOverlayDrag,
  type OverlayDragSession,
  type OverlayPosition
} from '../../utils/draggableOverlay';

const props = withDefaults(
  defineProps<{
    attachment?: TrainingAttachment | null;
    attachments?: TrainingAttachment[];
    contextKey?: string;
    minimized?: boolean;
    maximized?: boolean;
    avoidRight?: boolean;
  }>(),
  {
    attachment: null,
    attachments: () => [],
    contextKey: '',
    minimized: false,
    maximized: false,
    avoidRight: false
  }
);

const emit = defineEmits<{
  close: [];
  minimize: [];
  maximize: [];
  restoreSize: [];
  restore: [];
  preview: [attachment: TrainingAttachment];
}>();

const viewerHost = ref<HTMLElement | null>(null);
const minimizedButton = ref<HTMLButtonElement | null>(null);
const viewerError = ref('');
const minimizedPosition = ref<OverlayPosition | null>(null);
const minimizedDragging = ref(false);
const downloadSource = computed(
  () => props.attachment?.downloadUrl || props.attachment?.dataUrl || ''
);
const previewAttachments = computed(() => {
  const unique = new Map<string, TrainingAttachment>();
  props.attachments.forEach((attachment) => unique.set(attachment.id, attachment));
  if (props.attachment) unique.set(props.attachment.id, props.attachment);
  return [...unique.values()];
});
const minimizedStyle = computed(() =>
  minimizedPosition.value
    ? {
        left: `${minimizedPosition.value.x}px`,
        top: `${minimizedPosition.value.y}px`,
        right: 'auto',
        transform: 'none'
      }
    : undefined
);

let viewer: FileViewer | undefined;
let renderGeneration = 0;
let resizeObserver: ResizeObserver | undefined;
let viewerResizeTimer: number | undefined;
let minimizedDragState: OverlayDragSession | null = null;
let suppressMinimizedClick = false;
const VIEWER_RESIZE_SETTLE_MS = 260;

function buildPlugins(): PreviewPlugin[] {
  const pdfOptions = { useFetchData: true };
  return [
    imagePlugin(),
    videoPlugin(),
    audioPlugin(),
    officePlugin({ pdf: pdfOptions }),
    pdfPlugin(pdfOptions),
    textPlugin(),
    fallbackPlugin()
  ];
}

function resolvePreviewFit(attachment: TrainingAttachment): 'contain' | 'width' {
  const mimeType = attachment.mimeType.toLowerCase();
  const fileName = attachment.name.toLowerCase();
  const isPdfOrWord =
    mimeType === 'application/pdf' ||
    mimeType === 'application/msword' ||
    mimeType.includes('wordprocessingml') ||
    /\.(?:pdf|doc|docx|docm|dot|dotx|dotm)$/.test(fileName);
  return isPdfOrWord ? 'width' : 'contain';
}

function destroyViewer() {
  if (viewerResizeTimer !== undefined && typeof window !== 'undefined') {
    window.clearTimeout(viewerResizeTimer);
    viewerResizeTimer = undefined;
  }
  resizeObserver?.disconnect();
  resizeObserver = undefined;
  viewer?.destroy();
  viewer = undefined;
  viewerHost.value?.replaceChildren();
}

function scheduleViewerResize() {
  if (viewerResizeTimer !== undefined && typeof window !== 'undefined') {
    window.clearTimeout(viewerResizeTimer);
    viewerResizeTimer = undefined;
  }

  void nextTick(() => {
    viewer?.resize();
    if (typeof window === 'undefined') return;
    viewerResizeTimer = window.setTimeout(() => {
      viewerResizeTimer = undefined;
      viewer?.resize();
    }, VIEWER_RESIZE_SETTLE_MS);
  });
}

function handlePreviewTransitionEnd(event: TransitionEvent) {
  if (event.target !== event.currentTarget || event.propertyName !== 'width') return;
  scheduleViewerResize();
}

function startMinimizedDrag(event: PointerEvent) {
  if (
    !event.isPrimary ||
    (event.pointerType === 'mouse' && event.button !== 0) ||
    !minimizedButton.value
  ) {
    return;
  }
  const rect = minimizedButton.value.getBoundingClientRect();
  minimizedDragState = beginOverlayDrag(
    event.pointerId,
    { x: event.clientX, y: event.clientY },
    { x: rect.left, y: rect.top }
  );
  try {
    minimizedButton.value.setPointerCapture(event.pointerId);
  } catch {
    // Pointer capture is optional.
  }
}

function moveMinimizedDrag(event: PointerEvent) {
  if (!minimizedDragState || !minimizedButton.value) return;
  const rect = minimizedButton.value.getBoundingClientRect();
  const update = updateOverlayDrag(
    minimizedDragState,
    event.pointerId,
    { x: event.clientX, y: event.clientY },
    { width: rect.width, height: rect.height },
    { width: window.innerWidth, height: window.innerHeight },
    4
  );
  minimizedDragState = update.session;
  if (!update.position) return;
  minimizedPosition.value = update.position;
  minimizedDragging.value = true;
  event.preventDefault();
}

function finishMinimizedDrag(event: PointerEvent) {
  if (!minimizedDragState || minimizedDragState.pointerId !== event.pointerId) return;
  const moved = minimizedDragState.moved;
  try {
    if (minimizedButton.value?.hasPointerCapture(event.pointerId)) {
      minimizedButton.value.releasePointerCapture(event.pointerId);
    }
  } catch {
    // Ignore browsers without pointer capture state.
  }
  minimizedDragState = null;
  minimizedDragging.value = false;
  if (moved && event.type === 'pointerup') suppressMinimizedClick = true;
}

function restoreFromMinimized(event: MouseEvent) {
  if (suppressMinimizedClick) {
    suppressMinimizedClick = false;
    event.preventDefault();
    return;
  }
  emit('restore');
}

function constrainMinimizedPosition() {
  if (!minimizedPosition.value || !minimizedButton.value) return;
  const rect = minimizedButton.value.getBoundingClientRect();
  minimizedPosition.value = clampOverlayPosition(
    minimizedPosition.value,
    { width: rect.width, height: rect.height },
    { width: window.innerWidth, height: window.innerHeight }
  );
}

async function renderViewer() {
  const generation = ++renderGeneration;
  destroyViewer();
  viewerError.value = '';
  const attachment = props.attachment;
  if (!attachment || props.minimized) return;

  await nextTick();
  const container = viewerHost.value;
  if (!container || generation !== renderGeneration) return;

  try {
    viewer = createViewer({
      container,
      file: attachment.dataUrl,
      fileName: attachment.name,
      mimeType: attachment.mimeType,
      width: '100%',
      height: '100%',
      fit: resolvePreviewFit(attachment),
      locale: 'zh-CN',
      theme: 'light',
      fallback: 'inline',
      toolbar: {
        zoom: true,
        rotate: true,
        download: false,
        fullscreen: false,
        print: true,
        search: true
      },
      plugins: buildPlugins(),
      onLoad: () => {
        if (generation === renderGeneration) {
          viewerError.value = '';
          scheduleViewerResize();
        }
      },
      onError: (error) => {
        if (generation === renderGeneration) {
          viewerError.value = error.message || '附件预览加载失败';
        }
      },
      onUnsupported: () => {
        if (generation === renderGeneration) {
          viewerError.value = '当前文件格式暂不支持在线预览，可下载原文件查看。';
        }
      }
    });

    if (typeof ResizeObserver !== 'undefined') {
      resizeObserver = new ResizeObserver(() => viewer?.resize());
      resizeObserver.observe(container);
    }
  } catch (error) {
    viewerError.value =
      error instanceof Error ? error.message : '附件预览加载失败';
  }
}

watch(
  [() => props.attachment, () => props.minimized],
  () => void renderViewer(),
  { immediate: true, flush: 'post' }
);

watch(
  () => props.contextKey,
  () => {
    viewerError.value = '';
  },
  { flush: 'post' }
);

watch(
  () => props.maximized,
  scheduleViewerResize,
  { flush: 'post' }
);

function handleKeydown(event: KeyboardEvent) {
  if (event.key !== 'Escape' || !props.attachment) return;
  if (props.minimized) emit('close');
  else if (props.maximized) emit('restoreSize');
  else emit('minimize');
}

function handleResize() {
  viewer?.resize();
  constrainMinimizedPosition();
}

onMounted(() => {
  window.addEventListener('keydown', handleKeydown);
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  renderGeneration += 1;
  destroyViewer();
  window.removeEventListener('keydown', handleKeydown);
  window.removeEventListener('resize', handleResize);
});
</script>

<template>
  <Teleport to="body">
    <div
      v-if="attachment && !minimized"
      class="attachment-preview-layer"
      :class="{ maximized }"
      role="dialog"
      :aria-modal="maximized ? 'true' : 'false'"
      :aria-label="`预览附件：${attachment.name}`"
      @click.self="maximized ? emit('restoreSize') : undefined"
    >
      <section
        class="attachment-preview-window"
        @transitionend="handlePreviewTransitionEnd"
      >
        <header>
          <div class="attachment-preview-title">
            <span>附件预览</span>
            <strong :title="attachment.name">{{ attachment.name }}</strong>
          </div>
          <div class="attachment-preview-actions">
            <a :href="downloadSource" :download="attachment.name" title="下载附件">下载</a>
            <button type="button" title="最小化到附件图标" @click="emit('minimize')">
              最小化
            </button>
            <button
              v-if="!maximized"
              type="button"
              title="最大化附件预览"
              @click="emit('maximize')"
            >
              最大化
            </button>
            <button
              v-else
              type="button"
              title="还原附件预览"
              @click="emit('restoreSize')"
            >
              还原
            </button>
            <button type="button" title="关闭附件预览" @click="emit('close')">
              关闭
            </button>
          </div>
        </header>

        <nav
          v-if="previewAttachments.length > 1"
          class="attachment-preview-list"
          aria-label="当前教学附件"
        >
          <button
            v-for="item in previewAttachments"
            :key="item.id"
            type="button"
            :class="{ active: item.id === attachment.id }"
            :title="`预览 ${item.name}`"
            @click="emit('preview', item)"
          >
            <span aria-hidden="true">📎</span>
            <strong>{{ item.name }}</strong>
          </button>
        </nav>

        <div class="attachment-preview-content">
          <div ref="viewerHost" class="open-file-viewer-host"></div>
          <div v-if="viewerError" class="attachment-viewer-error" role="alert">
            <strong>附件预览加载失败</strong>
            <p>{{ viewerError }}</p>
            <a :href="downloadSource" :download="attachment.name">下载原文件</a>
          </div>
        </div>

        <footer>
          <span>{{ attachment.mimeType || '未知类型' }}</span>
          <span>{{ formatAttachmentSize(attachment.size) }}</span>
          <small>
            Open File Viewer · 浏览器本地解析；按 Esc
            {{ maximized ? '还原' : '最小化' }}
          </small>
        </footer>
      </section>
    </div>

    <button
      v-else-if="attachment && minimized"
      ref="minimizedButton"
      class="attachment-preview-minimized"
      :class="{
        'avoid-right': avoidRight && !minimizedPosition,
        dragging: minimizedDragging
      }"
      :style="minimizedStyle"
      type="button"
      :title="`拖动调整位置，点击恢复附件预览：${attachment.name}`"
      :aria-label="`恢复附件预览：${attachment.name}`"
      @pointerdown="startMinimizedDrag"
      @pointermove="moveMinimizedDrag"
      @pointerup="finishMinimizedDrag"
      @pointercancel="finishMinimizedDrag"
      @click="restoreFromMinimized"
    >
      <svg viewBox="0 0 24 24" aria-hidden="true">
        <path d="M8.5 12.7 14.9 6.3a3.2 3.2 0 0 1 4.5 4.5l-8.6 8.6a5 5 0 0 1-7.1-7.1l8.5-8.5" />
      </svg>
      <span v-if="previewAttachments.length > 1">{{ previewAttachments.length }}</span>
    </button>
  </Teleport>
</template>

<style scoped>
.attachment-preview-layer {
  position: fixed;
  z-index: 10000;
  inset: 0;
  pointer-events: none;
}

.attachment-preview-layer.maximized {
  pointer-events: auto;
  background: rgb(15 23 42 / 68%);
  backdrop-filter: blur(3px);
}

.attachment-preview-window {
  position: absolute;
  top: 16px;
  right: 16px;
  bottom: 16px;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr) auto;
  width: clamp(420px, 42vw, 620px);
  overflow: hidden;
  pointer-events: auto;
  border: 1px solid rgb(255 255 255 / 40%);
  border-radius: 16px;
  background: #f8fafc;
  box-shadow: 0 24px 70px rgb(15 23 42 / 42%);
  transition: inset 180ms ease, width 180ms ease, border-radius 180ms ease;
}

.attachment-preview-layer.maximized .attachment-preview-window {
  width: calc(100vw - 32px);
}

.attachment-preview-window > header {
  grid-row: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  min-height: 64px;
  padding: 10px 14px 10px 18px;
  color: #fff;
  background: #172033;
}

.attachment-preview-title {
  display: grid;
  min-width: 0;
  gap: 3px;
}

.attachment-preview-title span {
  color: #aeb9cc;
  font-size: 11px;
}

.attachment-preview-title strong {
  overflow: hidden;
  max-width: min(360px, 28vw);
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.attachment-preview-layer.maximized .attachment-preview-title strong {
  max-width: min(600px, 48vw);
}

.attachment-preview-actions {
  display: flex;
  flex: none;
  gap: 7px;
}

.attachment-preview-actions a,
.attachment-preview-actions button {
  border: 1px solid rgb(255 255 255 / 20%);
  border-radius: 8px;
  padding: 7px 11px;
  color: #fff;
  background: rgb(255 255 255 / 9%);
  cursor: pointer;
  font: inherit;
  font-size: 12px;
  text-decoration: none;
}

.attachment-preview-actions a:hover,
.attachment-preview-actions button:hover {
  background: rgb(255 255 255 / 18%);
}

.attachment-preview-list {
  grid-row: 2;
  display: flex;
  min-width: 0;
  gap: 7px;
  overflow-x: auto;
  border-bottom: 1px solid #e2e8f0;
  padding: 8px 10px;
  background: #fff;
}

.attachment-preview-list button {
  display: flex;
  min-width: 0;
  max-width: 230px;
  flex: 0 0 auto;
  align-items: center;
  gap: 6px;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 7px 9px;
  color: #475569;
  background: #f8fafc;
  cursor: pointer;
  font: inherit;
}

.attachment-preview-list button.active {
  border-color: #7767e9;
  color: #5745cc;
  background: #f0edff;
}

.attachment-preview-list strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
}

.attachment-preview-content {
  grid-row: 3;
  position: relative;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
  background: #eef2f7;
}

.open-file-viewer-host {
  width: 100%;
  height: 100%;
  min-height: 0;
  background: #fff;
}

.attachment-viewer-error {
  position: absolute;
  z-index: 2;
  top: 50%;
  left: 50%;
  display: grid;
  width: min(480px, calc(100% - 40px));
  gap: 10px;
  padding: 28px;
  transform: translate(-50%, -50%);
  justify-items: center;
  border: 1px solid #fecaca;
  border-radius: 14px;
  color: #7f1d1d;
  background: rgb(255 255 255 / 97%);
  box-shadow: 0 16px 40px rgb(15 23 42 / 15%);
  text-align: center;
}

.attachment-viewer-error p {
  margin: 0;
  color: #64748b;
  font-size: 13px;
}

.attachment-viewer-error a {
  border-radius: 8px;
  padding: 8px 14px;
  color: #fff;
  background: #5b4ad5;
  font-size: 13px;
  font-weight: 800;
  text-decoration: none;
}

.attachment-preview-window > footer {
  grid-row: 4;
  display: flex;
  align-items: center;
  gap: 12px;
  min-height: 40px;
  padding: 8px 16px;
  border-top: 1px solid #e2e8f0;
  color: #64748b;
  background: #fff;
  font-size: 11px;
}

.attachment-preview-window > footer small {
  margin-left: auto;
  color: #94a3b8;
}

.attachment-preview-minimized {
  position: fixed;
  z-index: 10001;
  top: 50%;
  right: 18px;
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border: 1px solid rgb(255 255 255 / 50%);
  border-radius: 50%;
  padding: 0;
  color: #fff;
  background: linear-gradient(145deg, #7c6cf2, #5b4bd2);
  box-shadow: 0 15px 36px rgb(53 41 151 / 38%);
  transform: translateY(-50%);
  cursor: grab;
  touch-action: none;
  user-select: none;
  transition: transform 160ms ease, box-shadow 160ms ease;
}

.attachment-preview-minimized.dragging {
  cursor: grabbing;
  transition: none;
}

.attachment-preview-minimized:hover {
  transform: translateY(-50%) scale(1.06);
  box-shadow: 0 18px 42px rgb(53 41 151 / 48%);
}

.attachment-preview-minimized.avoid-right {
  right: 390px;
}

.attachment-preview-minimized svg {
  width: 27px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.attachment-preview-minimized > span {
  position: absolute;
  top: -4px;
  right: -3px;
  display: grid;
  min-width: 20px;
  height: 20px;
  place-items: center;
  border: 2px solid #fff;
  border-radius: 999px;
  padding: 0 4px;
  color: #fff;
  background: #ef4444;
  box-sizing: border-box;
  font-size: 10px;
  font-weight: 900;
}

@media (max-width: 640px) {
  .attachment-preview-window,
  .attachment-preview-layer.maximized .attachment-preview-window {
    inset: 0;
    width: 100vw;
    height: 100vh;
    border: 0;
    border-radius: 0;
  }

  .attachment-preview-window > header {
    align-items: flex-start;
  }

  .attachment-preview-actions a {
    display: none;
  }

  .attachment-preview-title strong {
    max-width: 34vw;
  }

  .attachment-preview-window > footer small {
    display: none;
  }

  .attachment-preview-minimized,
  .attachment-preview-minimized.avoid-right {
    right: 12px;
  }
}
</style>
