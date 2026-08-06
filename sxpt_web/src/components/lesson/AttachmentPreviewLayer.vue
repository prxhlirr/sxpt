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
import pdfWorkerSrc from 'pdfjs-dist/build/pdf.worker.mjs?url';
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue';
import type { TrainingAttachment } from '../../domain/models';
import { formatAttachmentSize } from '../../utils/trainingAttachments';

const props = withDefaults(
  defineProps<{
    attachment?: TrainingAttachment | null;
    minimized?: boolean;
  }>(),
  {
    attachment: null,
    minimized: false
  }
);

const emit = defineEmits<{
  close: [];
  minimize: [];
  restore: [];
}>();

const viewerHost = ref<HTMLElement | null>(null);
const viewerError = ref('');
const downloadSource = computed(
  () => props.attachment?.downloadUrl || props.attachment?.dataUrl || ''
);

let viewer: FileViewer | undefined;
let renderGeneration = 0;
let resizeObserver: ResizeObserver | undefined;

function buildPlugins(): PreviewPlugin[] {
  const pdfOptions = {
    workerSrc: pdfWorkerSrc,
    useFetchData: true
  };
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

function destroyViewer() {
  resizeObserver?.disconnect();
  resizeObserver = undefined;
  viewer?.destroy();
  viewer = undefined;
  viewerHost.value?.replaceChildren();
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
      fit: 'contain',
      locale: 'zh-CN',
      theme: 'light',
      fallback: 'inline',
      toolbar: {
        zoom: true,
        rotate: true,
        download: false,
        fullscreen: true,
        print: true,
        search: true
      },
      plugins: buildPlugins(),
      onLoad: () => {
        if (generation === renderGeneration) viewerError.value = '';
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

function handleKeydown(event: KeyboardEvent) {
  if (event.key !== 'Escape' || !props.attachment) return;
  if (props.minimized) {
    emit('close');
  } else {
    emit('minimize');
  }
}

function handleResize() {
  viewer?.resize();
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
      class="attachment-preview-modal"
      role="dialog"
      aria-modal="true"
      :aria-label="`预览附件：${attachment.name}`"
      @click.self="emit('minimize')"
    >
      <section class="attachment-preview-window">
        <header>
          <div class="attachment-preview-title">
            <span>附件预览</span>
            <strong :title="attachment.name">{{ attachment.name }}</strong>
          </div>
          <div class="attachment-preview-actions">
            <a
              :href="downloadSource"
              :download="attachment.name"
              title="下载附件"
            >
              下载
            </a>
            <button type="button" title="最小化到右侧栏" @click="emit('minimize')">
              最小化
            </button>
            <button type="button" title="关闭附件预览" @click="emit('close')">
              关闭
            </button>
          </div>
        </header>

        <div class="attachment-preview-content">
          <div ref="viewerHost" class="open-file-viewer-host"></div>
          <div v-if="viewerError" class="attachment-viewer-error" role="alert">
            <strong>附件预览加载失败</strong>
            <p>{{ viewerError }}</p>
            <a :href="downloadSource" :download="attachment.name">
              下载原文件
            </a>
          </div>
        </div>

        <footer>
          <span>{{ attachment.mimeType || '未知类型' }}</span>
          <span>{{ formatAttachmentSize(attachment.size) }}</span>
          <small>Open File Viewer · 浏览器本地解析；按 Esc 可最小化</small>
        </footer>
      </section>
    </div>

    <aside
      v-else-if="attachment"
      class="attachment-preview-dock"
      aria-label="已最小化的附件预览"
    >
      <button
        class="attachment-dock-main"
        type="button"
        :title="`继续查看 ${attachment.name}`"
        @click="emit('restore')"
      >
        <span>附件</span>
        <strong>{{ attachment.name }}</strong>
        <small>继续查看</small>
      </button>
      <button
        class="attachment-dock-close"
        type="button"
        title="关闭附件"
        aria-label="关闭附件"
        @click="emit('close')"
      >
        ×
      </button>
    </aside>
  </Teleport>
</template>

<style scoped>
.attachment-preview-modal {
  position: fixed;
  z-index: 10000;
  inset: 0;
  display: grid;
  padding: 16px;
  place-items: center;
  background: rgb(15 23 42 / 68%);
  backdrop-filter: blur(3px);
}

.attachment-preview-window {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  width: min(1120px, calc(100vw - 32px));
  height: min(820px, calc(100vh - 32px));
  overflow: hidden;
  border: 1px solid rgb(255 255 255 / 40%);
  border-radius: 16px;
  background: #f8fafc;
  box-shadow: 0 24px 70px rgb(15 23 42 / 42%);
}

.attachment-preview-window > header {
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
  max-width: min(600px, 48vw);
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
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

.attachment-preview-content {
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

.attachment-preview-dock {
  position: fixed;
  z-index: 10001;
  top: 50%;
  right: 12px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  width: min(284px, calc(100vw - 24px));
  overflow: hidden;
  transform: translateY(-50%);
  border: 1px solid rgb(255 255 255 / 42%);
  border-radius: 13px;
  background: #172033;
  box-shadow: 0 14px 38px rgb(15 23 42 / 34%);
}

.attachment-dock-main {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-items: center;
  gap: 2px 9px;
  min-width: 0;
  border: 0;
  padding: 10px 12px;
  color: #fff;
  background: transparent;
  cursor: pointer;
  text-align: left;
}

.attachment-dock-main > span {
  grid-row: 1 / 3;
  border-radius: 7px;
  padding: 7px 6px;
  color: #5b4ad5;
  background: #ede9fe;
  font-size: 9px;
  font-weight: 900;
}

.attachment-dock-main strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 11px;
}

.attachment-dock-main small {
  color: #aeb9cc;
  font-size: 9px;
}

.attachment-dock-close {
  align-self: stretch;
  border: 0;
  border-left: 1px solid rgb(255 255 255 / 12%);
  padding: 0 12px;
  color: #cbd5e1;
  background: transparent;
  cursor: pointer;
  font-size: 18px;
}

.attachment-dock-main:hover,
.attachment-dock-close:hover {
  background: rgb(255 255 255 / 8%);
}

@media (max-width: 640px) {
  .attachment-preview-modal {
    padding: 0;
  }

  .attachment-preview-window {
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
    max-width: 44vw;
  }

  .attachment-preview-window > footer small {
    display: none;
  }
}
</style>
