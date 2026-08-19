<script setup lang="ts">
import {
  computed,
  nextTick,
  onBeforeUnmount,
  onMounted,
  ref,
  watch
} from 'vue';
import type { RouteLocationRaw } from 'vue-router';
import type {
  LessonPlan,
  LessonStage,
  RecordedStep,
  TrainingAttachment
} from '../../domain/models';
import { useTrainingStore } from '../../stores/trainingStore';
import AttachmentPanel from './AttachmentPanel.vue';
import AttachmentPreviewLayer from './AttachmentPreviewLayer.vue';
import BusinessSnapshotFrame from './BusinessSnapshotFrame.vue';
import PlaybackNavigationTree from './PlaybackNavigationTree.vue';
import {
  beginOverlayDrag,
  clampOverlayPosition,
  updateOverlayDrag,
  type OverlayDragSession,
  type OverlayPosition
} from '../../utils/draggableOverlay';

interface PlaybackStep {
  stage: LessonStage;
  step: RecordedStep;
  stageIndex: number;
  stepIndex: number;
}

interface PlaybackLauncherPosition {
  left: number;
  top: number;
}

interface PlaybackLauncherDragState extends PlaybackLauncherPosition {
  pointerId: number;
  startX: number;
  startY: number;
  moved: boolean;
}

const props = withDefaults(
  defineProps<{
    lesson: LessonPlan;
    currentIndex: number;
    showStageIntroduction: boolean;
    returnTo: RouteLocationRaw;
    returnLabel?: string;
    playerState?: 'READY' | 'PLAYING' | 'COMPLETED';
    startLabel?: string;
    finishLabel?: string;
    restartLabel?: string;
    actionDisabled?: boolean;
    feedback?: string;
    feedbackTone?: 'success' | 'danger';
  }>(),
  {
    returnLabel: '返回',
    playerState: 'PLAYING',
    startLabel: '开始流程讲解',
    finishLabel: '完成本次讲解',
    restartLabel: '重新学习',
    actionDisabled: false,
    feedback: '',
    feedbackTone: 'success'
  }
);
const store = useTrainingStore();

const emit = defineEmits<{
  start: [];
  restart: [];
  previous: [];
  next: [];
  finish: [];
  selectStep: [index: number];
  selectStage: [stageId: string];
}>();

const navigationOpen = ref(true);
const promptCollapsed = ref(false);
const promptPosition = ref<'left' | 'right' | 'bottom'>('right');
const playerElement = ref<HTMLElement | null>(null);
const promptElement = ref<HTMLElement | null>(null);
const manualPromptPosition = ref<OverlayPosition | null>(null);
const promptDragging = ref(false);
const dragState = ref<OverlayDragSession | null>(null);
const previewAttachment = ref<TrainingAttachment | null>(null);
const attachmentPreviewMinimized = ref(false);
const attachmentPreviewMaximized = ref(false);
const launcherPosition = ref<PlaybackLauncherPosition | null>(null);
const launcherDragging = ref(false);
let launcherDragState: PlaybackLauncherDragState | null = null;
let suppressLauncherClick = false;
const PLAYBACK_LAUNCHER_SIZE = 58;
const PLAYBACK_LAUNCHER_GAP = 10;

const steps = computed<PlaybackStep[]>(() =>
  props.lesson.stages.flatMap((stage, stageIndex) =>
    stage.recordedSteps.map((step, stepIndex) => ({
      stage,
      step,
      stageIndex,
      stepIndex
    }))
  )
);
const teachingPoints = computed(() =>
  props.lesson.stages.filter((stage) => stage.recordedSteps.length)
);
const current = computed(() => steps.value[props.currentIndex] ?? steps.value[0]);
const currentStep = computed(() => current.value?.step);
const currentAttachments = computed(() => {
  const unique = new Map<string, TrainingAttachment>();
  (current.value?.stage.attachments ?? []).forEach((attachment) =>
    unique.set(attachment.id, attachment)
  );
  if (!props.showStageIntroduction) {
    (currentStep.value?.attachments ?? []).forEach((attachment) =>
      unique.set(attachment.id, attachment)
    );
  }
  return [...unique.values()];
});
const businessResourceBaseUrl = computed(() => {
  const platform = store.getBusinessPlatform(props.lesson.businessPlatformId);
  if (!platform || platform.baseUrl.startsWith('internal://')) return undefined;
  const businessModule = store.getBusinessPlatformModule(
    props.lesson.businessPlatformId,
    props.lesson.businessPlatformModuleId
  );
  try {
    return new URL(businessModule?.path || platform.baseUrl, platform.baseUrl).href;
  } catch {
    return platform.baseUrl;
  }
});
const currentBusinessUrl = computed(() => {
  const candidate = currentStep.value?.url || currentStep.value?.pageSnapshot?.pageUrl;
  if (!candidate) return businessResourceBaseUrl.value;
  try {
    return new URL(candidate, businessResourceBaseUrl.value).href;
  } catch {
    return candidate;
  }
});
const canMovePrevious = computed(
  () =>
    props.currentIndex > 0 ||
    (!props.showStageIntroduction && current.value?.stepIndex === 0)
);
const totalDuration = computed(() =>
  steps.value.reduce((total, item) => total + item.step.durationSeconds, 0)
);
const elapsedDuration = computed(() =>
  steps.value
    .slice(0, props.currentIndex + 1)
    .reduce((total, item) => total + item.step.durationSeconds, 0)
);
const progress = computed(() =>
  steps.value.length
    ? Math.round(((Math.min(props.currentIndex, steps.value.length - 1) + 1) / steps.value.length) * 100)
    : 0
);
const promptStyle = computed(() =>
  manualPromptPosition.value
    ? {
        left: `${manualPromptPosition.value.x}px`,
        top: `${manualPromptPosition.value.y}px`,
        right: 'auto',
        bottom: 'auto',
        transform: 'none'
      }
    : undefined
);
const launcherStyle = computed<Record<string, string>>(() => {
  if (!launcherPosition.value) return {} as Record<string, string>;
  return {
    left: `${launcherPosition.value.left}px`,
    top: `${launcherPosition.value.top}px`,
    transform: 'none'
  };
});

function formatDuration(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(remainder).padStart(2, '0')}`;
}

function toggleNavigation() {
  navigationOpen.value = !navigationOpen.value;
  if (navigationOpen.value && promptPosition.value === 'left') {
    promptPosition.value = 'right';
  }
}

function playbackLauncherStorageKey() {
  return `sxpt:lesson-playback-launcher:${props.lesson.id}`;
}

function clampLauncherPosition(left: number, top: number) {
  const maxLeft = Math.max(
    PLAYBACK_LAUNCHER_GAP,
    window.innerWidth - PLAYBACK_LAUNCHER_SIZE - PLAYBACK_LAUNCHER_GAP
  );
  const maxTop = Math.max(
    PLAYBACK_LAUNCHER_GAP,
    window.innerHeight - PLAYBACK_LAUNCHER_SIZE - PLAYBACK_LAUNCHER_GAP
  );
  return {
    left: Math.min(Math.max(left, PLAYBACK_LAUNCHER_GAP), maxLeft),
    top: Math.min(Math.max(top, PLAYBACK_LAUNCHER_GAP), maxTop)
  };
}

function persistLauncherPosition() {
  if (!launcherPosition.value) return;
  try {
    window.localStorage.setItem(
      playbackLauncherStorageKey(),
      JSON.stringify(launcherPosition.value)
    );
  } catch {
    // 浏览器禁用本地存储时仍允许使用默认位置。
  }
}

function restoreLauncherPosition() {
  try {
    const raw = window.localStorage.getItem(playbackLauncherStorageKey());
    if (!raw) return;
    const parsed = JSON.parse(raw) as Partial<PlaybackLauncherPosition>;
    if (
      typeof parsed.left !== 'number' ||
      typeof parsed.top !== 'number' ||
      !Number.isFinite(parsed.left) ||
      !Number.isFinite(parsed.top)
    ) {
      return;
    }
    launcherPosition.value = clampLauncherPosition(parsed.left, parsed.top);
  } catch {
    launcherPosition.value = null;
  }
}

function constrainLauncherPosition() {
  if (!launcherPosition.value) return;
  launcherPosition.value = clampLauncherPosition(
    launcherPosition.value.left,
    launcherPosition.value.top
  );
  persistLauncherPosition();
}

function startLauncherDrag(event: PointerEvent) {
  if (event.button !== 0) return;
  const button = event.currentTarget as HTMLButtonElement;
  const rect = button.getBoundingClientRect();
  launcherDragState = {
    pointerId: event.pointerId,
    startX: event.clientX,
    startY: event.clientY,
    left: rect.left,
    top: rect.top,
    moved: false
  };
  try {
    button.setPointerCapture(event.pointerId);
  } catch {
    // Pointer capture is optional.
  }
}

function moveLauncherDrag(event: PointerEvent) {
  const drag = launcherDragState;
  if (!drag || drag.pointerId !== event.pointerId) return;
  const deltaX = event.clientX - drag.startX;
  const deltaY = event.clientY - drag.startY;
  if (!drag.moved && Math.hypot(deltaX, deltaY) < 4) return;
  drag.moved = true;
  launcherDragging.value = true;
  launcherPosition.value = clampLauncherPosition(
    drag.left + deltaX,
    drag.top + deltaY
  );
  event.preventDefault();
}

function finishLauncherDrag(event: PointerEvent) {
  const drag = launcherDragState;
  if (!drag || drag.pointerId !== event.pointerId) return;
  const button = event.currentTarget as HTMLButtonElement;
  try {
    if (button.hasPointerCapture(event.pointerId)) {
      button.releasePointerCapture(event.pointerId);
    }
  } catch {
    // Ignore browsers without pointer capture state.
  }
  if (drag.moved) {
    suppressLauncherClick = true;
    persistLauncherPosition();
  }
  launcherDragState = null;
  launcherDragging.value = false;
}

function openMenuFromLauncher(event: MouseEvent) {
  if (suppressLauncherClick) {
    suppressLauncherClick = false;
    event.preventDefault();
    return;
  }
  toggleNavigation();
}

function cyclePromptPosition() {
  manualPromptPosition.value = null;
  promptPosition.value =
    promptPosition.value === 'left'
      ? 'right'
      : promptPosition.value === 'right'
        ? 'bottom'
        : 'left';
}

function constrainManualPromptPosition() {
  if (
    !manualPromptPosition.value ||
    !playerElement.value ||
    !promptElement.value
  ) {
    return;
  }
  const playerRect = playerElement.value.getBoundingClientRect();
  const promptRect = promptElement.value.getBoundingClientRect();
  manualPromptPosition.value = clampOverlayPosition(
    manualPromptPosition.value,
    { width: promptRect.width, height: promptRect.height },
    { width: playerRect.width, height: playerRect.height }
  );
}

function schedulePromptConstraint() {
  void nextTick(constrainManualPromptPosition);
}

function startPromptDrag(event: PointerEvent) {
  if (
    !event.isPrimary ||
    (event.pointerType === 'mouse' && event.button !== 0) ||
    (event.target as HTMLElement | null)?.closest(
      'button, a, input, select, textarea'
    ) ||
    !playerElement.value ||
    !promptElement.value
  ) {
    return;
  }

  const playerRect = playerElement.value.getBoundingClientRect();
  const promptRect = promptElement.value.getBoundingClientRect();
  dragState.value = beginOverlayDrag(
    event.pointerId,
    { x: event.clientX, y: event.clientY },
    {
      x: promptRect.left - playerRect.left,
      y: promptRect.top - playerRect.top
    }
  );
  promptDragging.value = true;
  (event.currentTarget as HTMLElement).setPointerCapture(event.pointerId);
  event.preventDefault();
}

function movePromptDrag(event: PointerEvent) {
  if (
    !dragState.value ||
    !playerElement.value ||
    !promptElement.value
  ) {
    return;
  }
  const playerRect = playerElement.value.getBoundingClientRect();
  const promptRect = promptElement.value.getBoundingClientRect();
  const update = updateOverlayDrag(
    dragState.value,
    event.pointerId,
    { x: event.clientX, y: event.clientY },
    { width: promptRect.width, height: promptRect.height },
    { width: playerRect.width, height: playerRect.height }
  );
  dragState.value = update.session;
  if (!update.position) return;
  manualPromptPosition.value = update.position;
  event.preventDefault();
}

function finishPromptDrag(event: PointerEvent) {
  if (!dragState.value || dragState.value.pointerId !== event.pointerId) return;
  const handle = event.currentTarget as HTMLElement;
  if (handle.hasPointerCapture(event.pointerId)) {
    handle.releasePointerCapture(event.pointerId);
  }
  dragState.value = null;
  promptDragging.value = false;
}

watch(
  [
    promptCollapsed,
    () => props.showStageIntroduction,
    () => props.currentIndex,
    () => props.playerState
  ],
  schedulePromptConstraint
);

watch(
  () => current.value?.stage.id,
  (stageId, previousStageId) => {
    if (previousStageId && stageId !== previousStageId) {
      closeAttachmentPreview();
    }
  }
);

watch(
  [() => props.currentIndex, () => props.showStageIntroduction],
  () => {
    if (
      previewAttachment.value &&
      !currentAttachments.value.some(
        (attachment) => attachment.id === previewAttachment.value?.id
      )
    ) {
      closeAttachmentPreview();
    }
  }
);

function handleViewportResize() {
  schedulePromptConstraint();
  constrainLauncherPosition();
}

onMounted(() => {
  restoreLauncherPosition();
  window.addEventListener('resize', handleViewportResize);
});
onBeforeUnmount(() =>
  window.removeEventListener('resize', handleViewportResize)
);

function openAttachmentPreview(attachment: TrainingAttachment) {
  previewAttachment.value = attachment;
  attachmentPreviewMinimized.value = false;
}

function openAttachmentsFromMenu() {
  const firstAttachment = currentAttachments.value[0];
  if (!firstAttachment) return;
  if (
    !previewAttachment.value ||
    !currentAttachments.value.some(
      (attachment) => attachment.id === previewAttachment.value?.id
    )
  ) {
    previewAttachment.value = firstAttachment;
  }
  attachmentPreviewMinimized.value = false;
}

function closeAttachmentPreview() {
  previewAttachment.value = null;
  attachmentPreviewMinimized.value = false;
  attachmentPreviewMaximized.value = false;
}

function minimizeAttachmentPreview() {
  if (!previewAttachment.value) return;
  attachmentPreviewMinimized.value = true;
  attachmentPreviewMaximized.value = false;
}

function restoreAttachmentPreview() {
  attachmentPreviewMinimized.value = false;
}

function maximizeAttachmentPreview() {
  if (previewAttachment.value) attachmentPreviewMaximized.value = true;
}

function restoreAttachmentPreviewSize() {
  attachmentPreviewMaximized.value = false;
}
</script>

<template>
  <section ref="playerElement" class="lesson-playback-player">
    <BusinessSnapshotFrame
      v-if="currentStep"
      class="playback-business-view"
      fit-mode="fill"
      :snapshot="currentStep.pageSnapshot"
      :fallback-url="currentBusinessUrl"
      :selector="showStageIntroduction ? undefined : currentStep.selector"
      :selector-candidates="
        showStageIntroduction ? undefined : currentStep.selectorCandidates
      "
      :rect="showStageIntroduction ? undefined : currentStep.rect"
      :recorded-viewport="
        showStageIntroduction ? undefined : currentStep.recordedViewport
      "
      :title="`${currentStep.pageTitle}录制页面快照`"
    />
    <div v-else class="playback-empty">当前教案还没有可回放的录制节点</div>

    <section
      v-if="navigationOpen"
      class="playback-menu-shell"
      aria-label="讲解与学习菜单"
    >
      <nav class="playback-menu-rail" aria-label="讲解与学习操作">
        <div class="playback-menu-mark" aria-hidden="true">导</div>
        <RouterLink class="playback-rail-action" :to="returnTo" :title="returnLabel">
          <span>←</span>
          <small>返回</small>
        </RouterLink>
        <button
          class="playback-rail-action active"
          type="button"
          title="收起教学目录"
          @click="toggleNavigation"
        >
          <span>☰</span>
          <small>目录</small>
        </button>
        <button
          class="playback-rail-action"
          :class="{ active: !promptCollapsed }"
          type="button"
          :aria-expanded="!promptCollapsed"
          title="收起或展开当前说明"
          @click="promptCollapsed = !promptCollapsed"
        >
          <span>i</span>
          <small>说明</small>
        </button>
        <button
          class="playback-rail-action playback-attachment-action"
          :class="{
            active: Boolean(previewAttachment) && !attachmentPreviewMinimized
          }"
          type="button"
          :disabled="!currentAttachments.length"
          :aria-expanded="Boolean(previewAttachment) && !attachmentPreviewMinimized"
          :title="
            currentAttachments.length
              ? `查看当前附件（${currentAttachments.length} 个）`
              : '当前教学点和节点暂无附件'
          "
          @click="openAttachmentsFromMenu"
        >
          <span class="playback-attachment-icon" aria-hidden="true">
            <svg viewBox="0 0 24 24">
              <path d="M8.5 12.7 14.9 6.3a3.2 3.2 0 0 1 4.5 4.5l-8.6 8.6a5 5 0 0 1-7.1-7.1l8.5-8.5" />
            </svg>
            <b v-if="currentAttachments.length">{{ currentAttachments.length }}</b>
          </span>
          <small>附件</small>
        </button>
        <button
          class="playback-rail-action playback-rail-collapse"
          type="button"
          title="收起全部菜单"
          @click="navigationOpen = false"
        >
          <span>‹</span>
          <small>收起</small>
        </button>
      </nav>

      <aside class="playback-directory-panel" aria-label="教学点与节点目录">
        <header>
          <div>
            <small>LESSON GUIDE</small>
            <strong>{{ lesson.title }}</strong>
            <span>{{ teachingPoints.length }} 个教学点 · {{ steps.length }} 个节点</span>
          </div>
          <em>{{ progress }}%</em>
        </header>
        <div class="playback-directory-progress">
          <i :style="{ width: `${progress}%` }"></i>
        </div>
        <PlaybackNavigationTree
          :teaching-points="teachingPoints"
          :steps="steps"
          :current-stage-id="current?.stage.id"
          :current-index="currentIndex"
          :show-stage-introduction="showStageIntroduction"
          :disabled="playerState !== 'PLAYING'"
          @select-stage="emit('selectStage', $event)"
          @select-step="emit('selectStep', $event)"
        />
        <footer v-if="current">
          <span>{{ showStageIntroduction ? '当前教学点' : '当前节点' }}</span>
          <strong
            :title="showStageIntroduction ? current.stage.name : current.step.title"
          >
            {{ showStageIntroduction ? current.stage.name : current.step.title }}
          </strong>
          <small>{{ formatDuration(elapsedDuration) }} / {{ formatDuration(totalDuration) }}</small>
        </footer>
      </aside>
    </section>

    <button
      v-else
      class="playback-floating-launcher"
      :class="{ dragging: launcherDragging }"
      :style="launcherStyle"
      type="button"
      title="拖动调整位置，点击展开讲解菜单"
      aria-label="展开讲解菜单"
      @pointerdown="startLauncherDrag"
      @pointermove="moveLauncherDrag"
      @pointerup="finishLauncherDrag"
      @pointercancel="finishLauncherDrag"
      @click="openMenuFromLauncher"
    >
      导
    </button>

    <aside
      ref="promptElement"
      class="playback-prompt"
      :style="promptStyle"
      :class="[
        `prompt-${promptPosition}`,
        {
          collapsed: promptCollapsed,
          dragging: promptDragging,
          'stage-prompt': showStageIntroduction,
          'node-prompt': !showStageIntroduction
        }
      ]"
    >
      <header
        class="playback-prompt-toolbar"
        @pointerdown="startPromptDrag"
        @pointermove="movePromptDrag"
        @pointerup="finishPromptDrag"
        @pointercancel="finishPromptDrag"
      >
        <strong>
          {{
            playerState === 'READY'
              ? '准备开始'
              : playerState === 'COMPLETED'
                ? '流程已完成'
                : showStageIntroduction
                  ? '教学点说明'
                  : '本节点说明'
          }}
        </strong>
        <div>
          <button type="button" title="移动说明浮窗" @click="cyclePromptPosition">移动</button>
          <button type="button" @click="promptCollapsed = !promptCollapsed">
            {{ promptCollapsed ? '展开' : '收起' }}
          </button>
        </div>
      </header>

      <div v-if="!promptCollapsed" class="playback-prompt-content">
        <template v-if="playerState === 'READY'">
          <div class="playback-heading">
            <span>教学流程</span>
            <strong>{{ lesson.title }}</strong>
            <small>{{ teachingPoints.length }} 个教学点 · {{ steps.length }} 个操作节点</small>
          </div>
          <div class="playback-instruction">
            <span>学习说明</span>
            <p>开始后可按教学点和操作节点逐步查看完整业务系统流程。</p>
          </div>
          <button
            class="primary playback-state-action"
            type="button"
            :disabled="actionDisabled || !steps.length"
            @click="emit('start')"
          >
            {{ startLabel }}
          </button>
        </template>

        <template v-else-if="playerState === 'COMPLETED'">
          <div class="playback-heading">
            <span>教学流程</span>
            <strong>本次流程已经完成</strong>
            <small>{{ lesson.title }}</small>
          </div>
          <div class="playback-instruction">
            <span>完成情况</span>
            <p>完整学习轨迹已保存，可返回任务中心或从第一步重新开始。</p>
          </div>
          <button
            class="primary playback-state-action"
            type="button"
            :disabled="actionDisabled"
            @click="emit('restart')"
          >
            {{ restartLabel }}
          </button>
        </template>

        <template v-else-if="current && currentStep">
          <template v-if="showStageIntroduction">
            <div class="playback-heading">
              <span>教学点 {{ current.stageIndex + 1 }} / {{ teachingPoints.length }}</span>
              <strong>{{ current.stage.name }}</strong>
            </div>
            <div class="playback-instruction">
              <span>教学说明</span>
              <p>
                {{
                  current.stage.description ||
                  '本教学点暂无补充说明，可按需选择任意节点进行讲解。'
                }}
              </p>
            </div>
            <AttachmentPanel
              :attachments="current.stage.attachments"
              title="教学点附件"
              @preview="openAttachmentPreview"
            />
            <div class="playback-essential-meta">
              <span><b>{{ current.stage.recordedSteps.length }}</b> 个操作节点</span>
              <span>
                参考时长
                <b>
                  {{
                    formatDuration(
                      current.stage.recordedSteps.reduce(
                        (total, step) => total + step.durationSeconds,
                        0
                      )
                    )
                  }}
                </b>
              </span>
            </div>
          </template>
          <template v-else>
            <div class="playback-heading">
              <span>操作节点 {{ currentIndex + 1 }} / {{ steps.length }}</span>
              <strong>{{ currentStep.title }}</strong>
              <small>{{ current.stage.name }}</small>
            </div>
            <div class="playback-instruction">
              <span>操作说明</span>
              <p>
                {{
                  currentStep.teachingText ||
                  currentStep.note ||
                  '请观察页面变化，并理解该动作在业务流程中的作用。'
                }}
              </p>
            </div>
            <AttachmentPanel
              :attachments="currentStep.attachments"
              title="节点附件"
              @preview="openAttachmentPreview"
            />
            <div class="playback-essential-meta">
              <span>操作 <b>{{ currentStep.actionLabel || currentStep.title }}</b></span>
              <span>参考时长 <b>{{ currentStep.durationSeconds }} 秒</b></span>
            </div>
          </template>
          <div class="playback-step-controls">
            <button
              type="button"
              :disabled="!canMovePrevious || actionDisabled"
              @click="emit('previous')"
            >
              ← 上一步
            </button>
            <button
              v-if="showStageIntroduction"
              class="primary"
              type="button"
              :disabled="actionDisabled"
              @click="emit('next')"
            >
              进入本教学点 →
            </button>
            <button
              v-else-if="currentIndex < steps.length - 1"
              class="primary"
              type="button"
              :disabled="actionDisabled"
              @click="emit('next')"
            >
              下一步 →
            </button>
            <button
              v-else
              class="primary"
              type="button"
              :disabled="actionDisabled"
              @click="emit('finish')"
            >
              {{ finishLabel }}
            </button>
          </div>
        </template>

        <p v-if="feedback" class="playback-feedback" :class="feedbackTone">
          {{ feedback }}
        </p>
      </div>
    </aside>

    <AttachmentPreviewLayer
      :attachment="previewAttachment"
      :attachments="currentAttachments"
      :context-key="current?.stage.id ?? ''"
      :minimized="attachmentPreviewMinimized"
      :maximized="attachmentPreviewMaximized"
      :avoid-right="false"
      @close="closeAttachmentPreview"
      @maximize="maximizeAttachmentPreview"
      @minimize="minimizeAttachmentPreview"
      @preview="openAttachmentPreview"
      @restore="restoreAttachmentPreview"
      @restore-size="restoreAttachmentPreviewSize"
    />
  </section>
</template>

<style scoped>
.lesson-playback-player {
  position: relative;
  width: 100%;
  height: 100vh;
  min-height: 0;
  overflow: hidden;
  background: #eef1f6;
}

.playback-business-view {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.playback-empty {
  display: grid;
  height: 100%;
  place-items: center;
  color: #68748a;
  background: #fff;
}

.playback-edge-toolbar {
  position: absolute;
  z-index: 60;
  top: 14px;
  right: 14px;
  display: flex;
  gap: 6px;
  border: 1px solid rgb(255 255 255 / 72%);
  border-radius: 999px;
  padding: 5px;
  background: rgb(25 32 49 / 82%);
  box-shadow: 0 10px 28px rgb(15 20 40 / 24%);
  backdrop-filter: blur(12px);
}

.playback-edge-toolbar a,
.playback-edge-toolbar button,
.playback-prompt-toolbar button {
  min-width: 42px;
  min-height: 30px;
  border: 0;
  border-radius: 999px;
  padding: 0 10px;
  color: #fff;
  font: inherit;
  font-size: 11px;
  font-weight: 800;
  text-decoration: none;
  background: transparent;
  cursor: pointer;
}

.playback-edge-toolbar a {
  display: grid;
  min-width: 30px;
  padding: 0;
  place-items: center;
}

.playback-edge-toolbar button:hover,
.playback-edge-toolbar button.active,
.playback-edge-toolbar a:hover {
  background: rgb(255 255 255 / 16%);
}

.playback-navigation-drawer {
  position: absolute;
  z-index: 50;
  top: 58px;
  right: 14px;
  bottom: 48px;
  display: flex;
  width: min(340px, calc(100vw - 28px));
  flex-direction: column;
  overflow: hidden;
  border: 1px solid rgb(218 223 234 / 88%);
  border-radius: 16px;
  background: rgb(255 255 255 / 96%);
  box-shadow: 0 20px 54px rgb(23 29 55 / 22%);
  backdrop-filter: blur(16px);
}

.playback-navigation-drawer > header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-bottom: 1px solid #e9edf4;
  padding: 14px 16px;
}

.playback-navigation-drawer > header div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.playback-navigation-drawer > header small,
.playback-navigation-drawer h2 {
  color: #8c95a8;
  font-size: 10px;
  letter-spacing: 0.08em;
}

.playback-navigation-drawer > header strong {
  overflow: hidden;
  font-size: 14px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playback-navigation-drawer > header button {
  width: 30px;
  height: 30px;
  border: 0;
  border-radius: 50%;
  color: #526077;
  background: #eef1f6;
  cursor: pointer;
}

.playback-metrics {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  border-bottom: 1px solid #edf0f5;
}

.playback-metrics span {
  display: grid;
  gap: 3px;
  padding: 10px 8px;
  text-align: center;
}

.playback-metrics small {
  color: #98a1b2;
  font-size: 9px;
}

.playback-metrics strong {
  color: #334058;
  font-size: 12px;
}

.playback-drawer-progress,
.playback-progress-strip div {
  overflow: hidden;
  height: 4px;
  background: #e9ecf2;
}

.playback-drawer-progress i,
.playback-progress-strip i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #6b5dd3, #19a77b);
}

.playback-navigation-drawer > section {
  display: grid;
  gap: 6px;
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.playback-navigation-drawer > section + section {
  border-top: 1px solid #edf0f5;
}

.playback-navigation-drawer section > button {
  display: grid;
  grid-template-columns: 26px minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  border: 1px solid #e5e9f0;
  border-radius: 9px;
  padding: 8px;
  color: #556177;
  text-align: left;
  background: #fff;
  cursor: pointer;
}

.playback-navigation-drawer section > button.active {
  border-color: #786bd4;
  color: #5647bc;
  background: #f2efff;
}

.playback-navigation-drawer section > button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.playback-navigation-drawer section > button span {
  display: grid;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  place-items: center;
  color: #fff;
  font-size: 9px;
  background: #8a93a4;
}

.playback-navigation-drawer section > button strong {
  min-width: 0;
  overflow: hidden;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playback-navigation-drawer section > button small {
  color: #9199a8;
  font-size: 9px;
}

.playback-prompt {
  position: absolute;
  z-index: 40;
  display: grid;
  width: min(390px, calc(100vw - 36px));
  max-height: calc(100vh - 80px);
  grid-template-rows: auto minmax(0, 1fr);
  overflow: hidden;
  border: 1px solid rgb(218 223 234 / 88%);
  border-radius: 15px;
  background: rgb(255 255 255 / 95%);
  box-shadow: 0 18px 46px rgb(23 29 55 / 20%);
  backdrop-filter: blur(14px);
}

.playback-prompt.stage-prompt {
  top: 50%;
  left: 50%;
  width: min(480px, calc(100vw - 36px));
  transform: translate(-50%, -50%);
}

.playback-prompt.node-prompt.prompt-left {
  top: 50%;
  left: 18px;
  transform: translateY(-50%);
}

.playback-prompt.node-prompt.prompt-right {
  top: 50%;
  right: 18px;
  transform: translateY(-50%);
}

.playback-prompt.node-prompt.prompt-bottom {
  right: 50%;
  bottom: 36px;
  transform: translateX(50%);
}

.playback-prompt.collapsed {
  width: auto;
  max-width: min(320px, calc(100vw - 100px));
}

.playback-prompt.stage-prompt.collapsed {
  top: 14px;
  right: auto;
  left: 14px;
  transform: none;
}

.playback-prompt-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  min-height: 42px;
  padding: 6px 7px 6px 14px;
  color: #fff;
  background: linear-gradient(135deg, rgb(48 56 77 / 96%), rgb(73 62 145 / 94%));
  cursor: grab;
  touch-action: none;
  user-select: none;
}

.playback-prompt.dragging .playback-prompt-toolbar {
  cursor: grabbing;
}

.playback-prompt-toolbar strong {
  overflow: hidden;
  font-size: 11px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playback-prompt-toolbar div {
  display: flex;
  gap: 2px;
}

.playback-prompt-toolbar button {
  min-width: auto;
  min-height: 28px;
  padding: 0 7px;
  font-size: 9px;
  cursor: pointer;
  touch-action: manipulation;
}

.playback-prompt-content {
  overflow-y: auto;
}

.playback-heading {
  display: grid;
  gap: 5px;
  padding: 16px 18px 12px;
}

.playback-heading span,
.playback-instruction span {
  color: #7464d1;
  font-size: 9px;
  font-weight: 800;
  letter-spacing: 0.05em;
}

.playback-heading strong {
  color: #2f3c53;
  font-size: 16px;
}

.playback-heading small {
  color: #8a94a6;
  font-size: 10px;
}

.playback-instruction {
  margin: 0 16px 12px;
  border-radius: 10px;
  padding: 11px 12px;
  background: #f4f2ff;
}

.playback-instruction p {
  margin: 6px 0 0;
  color: #4b5870;
  font-size: 11px;
  line-height: 1.6;
}

.playback-prompt :deep(.attachment-panel) {
  margin: 0 16px 12px;
}

.playback-prompt dl {
  display: grid;
  margin: 0;
  padding: 0 18px 12px;
}

.playback-prompt dl > div {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr);
  gap: 8px;
  border-bottom: 1px solid #edf0f5;
  padding: 8px 0;
}

.playback-prompt dt,
.playback-prompt dd {
  font-size: 9px;
}

.playback-prompt dt {
  color: #8b95a7;
}

.playback-prompt dd {
  min-width: 0;
  margin: 0;
  overflow-wrap: anywhere;
  color: #4c596e;
}

.playback-step-controls {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 12px 16px 16px;
}

.playback-step-controls button,
.playback-state-action {
  min-height: 36px;
  border: 1px solid #dce1e9;
  border-radius: 8px;
  color: #566278;
  font: inherit;
  font-size: 10px;
  font-weight: 800;
  background: #fff;
  cursor: pointer;
}

.playback-step-controls button.primary,
.playback-state-action.primary {
  border-color: #695bc7;
  color: #fff;
  background: #695bc7;
}

.playback-step-controls button:disabled,
.playback-state-action:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}

.playback-state-action {
  width: calc(100% - 32px);
  margin: 0 16px 16px;
}

.playback-feedback {
  margin: 0 16px 14px;
  border-radius: 8px;
  padding: 9px 10px;
  font-size: 10px;
}

.playback-feedback.success {
  color: #087b59;
  background: #eaf8f2;
}

.playback-feedback.danger {
  color: #a43d48;
  background: #fff0f1;
}

.playback-progress-strip {
  position: absolute;
  z-index: 30;
  right: 14px;
  bottom: 10px;
  left: 14px;
  display: grid;
  grid-template-columns: auto minmax(80px, 1fr) auto;
  align-items: center;
  gap: 8px;
  border-radius: 999px;
  padding: 5px 9px;
  color: #fff;
  font-size: 9px;
  background: rgb(25 32 49 / 68%);
  backdrop-filter: blur(10px);
}

.playback-progress-strip div {
  border-radius: 999px;
  background: rgb(255 255 255 / 25%);
}

@media (max-height: 680px) {
  .playback-prompt dl,
  .playback-prompt :deep(.attachment-panel) {
    display: none;
  }

  .playback-navigation-drawer {
    bottom: 42px;
  }
}

@media (max-width: 720px) {
  .playback-edge-toolbar {
    top: 8px;
    right: 8px;
  }

  .playback-prompt,
  .playback-prompt.stage-prompt,
  .playback-prompt.node-prompt.prompt-left,
  .playback-prompt.node-prompt.prompt-right,
  .playback-prompt.node-prompt.prompt-bottom {
    top: auto;
    right: 8px;
    bottom: 36px;
    left: 8px;
    width: auto;
    max-height: 48vh;
    transform: none;
  }

  .playback-prompt dl,
  .playback-instruction,
  .playback-prompt :deep(.attachment-panel) {
    display: none;
  }

  .playback-navigation-drawer {
    top: 52px;
    right: 8px;
    bottom: 38px;
  }

  .playback-progress-strip {
    right: 8px;
    bottom: 6px;
    left: 8px;
  }
}

/* 与教案编排一致：业务系统全屏铺底，菜单作为可收起的左侧浮层。 */
.playback-menu-shell {
  position: absolute;
  z-index: 50;
  top: 14px;
  bottom: 14px;
  left: 14px;
  display: flex;
  max-height: calc(100vh - 28px);
  overflow: hidden;
  border: 1px solid rgb(222 226 236 / 92%);
  border-radius: 20px;
  background: transparent;
  box-shadow: 0 22px 60px rgb(27 36 54 / 24%);
}

.playback-menu-rail {
  display: flex;
  width: 88px;
  flex: 0 0 88px;
  flex-direction: column;
  align-items: stretch;
  gap: 8px;
  padding: 14px 8px 12px;
  color: #dce3f1;
  background:
    radial-gradient(circle at 50% 0%, rgb(104 88 229 / 30%), transparent 31%),
    linear-gradient(180deg, #252b3a 0%, #1b202d 100%);
  box-sizing: border-box;
}

.playback-menu-mark,
.playback-floating-launcher {
  display: grid;
  place-items: center;
  border: 0;
  color: #fff;
  background: linear-gradient(145deg, #7c6cf2, #5b4bd2);
  box-shadow: 0 9px 24px rgb(91 75 210 / 38%);
  font-weight: 900;
}

.playback-menu-mark {
  width: 50px;
  height: 50px;
  flex: 0 0 50px;
  align-self: center;
  margin-bottom: 4px;
  border-radius: 16px;
  font-size: 21px;
}

.playback-rail-action {
  display: flex;
  min-height: 58px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 3px;
  border: 0;
  border-radius: 13px;
  padding: 6px 3px;
  color: #bac4d5;
  background: transparent;
  box-shadow: none;
  font: inherit;
  text-align: center;
  text-decoration: none;
  cursor: pointer;
}

.playback-rail-action:hover,
.playback-rail-action.active {
  color: #fff;
  background: rgb(255 255 255 / 11%);
  box-shadow: none;
  transform: none;
}

.playback-rail-action > span {
  display: grid;
  width: 28px;
  height: 25px;
  place-items: center;
  font-size: 22px;
  font-weight: 900;
  line-height: 1;
}

.playback-rail-action > small {
  font-size: 14px;
  font-weight: 800;
}

.playback-rail-action:disabled {
  opacity: 0.42;
  cursor: not-allowed;
}

.playback-attachment-icon {
  position: relative;
}

.playback-attachment-icon svg {
  width: 23px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 2;
}

.playback-attachment-icon b {
  position: absolute;
  top: -7px;
  right: -8px;
  display: grid;
  min-width: 18px;
  height: 18px;
  place-items: center;
  border: 2px solid #252b3a;
  border-radius: 999px;
  padding: 0 3px;
  color: #fff;
  background: #ef4444;
  box-sizing: border-box;
  font-size: 9px;
  line-height: 1;
}

.playback-rail-collapse {
  margin-top: auto;
  border-top: 1px solid rgb(255 255 255 / 10%);
  border-radius: 0 0 13px 13px;
}

.playback-directory-panel {
  display: flex;
  width: 348px;
  min-width: 0;
  flex-direction: column;
  color: #202735;
  background: rgb(250 251 253 / 97%);
  backdrop-filter: blur(18px);
}

.playback-directory-panel > header {
  display: grid;
  min-height: 104px;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid #e5e8ef;
  padding: 17px 18px 14px;
  box-sizing: border-box;
}

.playback-directory-panel > header > div {
  display: grid;
  min-width: 0;
  gap: 4px;
}

.playback-directory-panel > header small {
  color: #6e5ce2;
  font-size: 13px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.playback-directory-panel > header strong {
  overflow: hidden;
  color: #273041;
  font-size: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playback-directory-panel > header span {
  color: #7d8797;
  font-size: 14px;
}

.playback-directory-panel > header em {
  display: grid;
  width: 50px;
  height: 50px;
  place-items: center;
  border-radius: 15px;
  color: #5e4fd0;
  background: #ebe8ff;
  font-size: 16px;
  font-style: normal;
  font-weight: 900;
}

.playback-directory-progress {
  overflow: hidden;
  height: 5px;
  background: #e8ebf1;
}

.playback-directory-progress i {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, #6b5dd3, #19a77b);
  transition: width 180ms ease;
}

.playback-directory-panel > :deep(.playback-navigation-tree) {
  min-height: 0;
  flex: 1 1 auto;
}

.playback-directory-panel > footer {
  display: grid;
  gap: 3px;
  border-top: 1px solid #e5e8ef;
  padding: 12px 16px;
  background: #fff;
}

.playback-directory-panel > footer span,
.playback-directory-panel > footer small {
  color: #858e9d;
  font-size: 13px;
}

.playback-directory-panel > footer strong {
  overflow: hidden;
  color: #313a4a;
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playback-floating-launcher {
  position: fixed;
  z-index: 60;
  top: 50%;
  left: 14px;
  width: 58px;
  height: 58px;
  min-height: 58px;
  border-radius: 18px;
  padding: 0;
  font-size: 22px;
  cursor: grab;
  touch-action: none;
  transform: translateY(-50%);
  user-select: none;
}

.playback-floating-launcher:hover {
  box-shadow: 0 12px 28px rgb(91 75 210 / 48%);
  transform: translateY(-50%) scale(1.03);
}

.playback-floating-launcher.dragging {
  cursor: grabbing;
  box-shadow: 0 16px 34px rgb(91 75 210 / 52%);
}

/* 讲解卡片只保留标题、说明、附件、操作与时长。 */
.playback-prompt {
  z-index: 45;
  width: min(390px, calc(100vw - 36px));
  max-height: calc(100vh - 32px);
  border-radius: 18px;
  box-shadow: 0 22px 60px rgb(23 29 55 / 24%);
}

.playback-prompt.stage-prompt {
  width: min(450px, calc(100vw - 36px));
}

.playback-prompt-toolbar {
  min-height: 50px;
  padding: 7px 8px 7px 16px;
}

.playback-prompt-toolbar strong {
  font-size: 16px;
}

.playback-prompt-toolbar button {
  min-height: 34px;
  padding: 0 10px;
  font-size: 14px;
}

.playback-heading {
  gap: 6px;
  padding: 18px 20px 12px;
}

.playback-heading span,
.playback-instruction span {
  font-size: 13px;
}

.playback-heading strong {
  font-size: 20px;
}

.playback-heading small {
  font-size: 14px;
}

.playback-instruction {
  margin: 0 18px 14px;
  border-radius: 12px;
  padding: 13px 14px;
}

.playback-instruction p {
  margin-top: 7px;
  font-size: 16px;
  line-height: 1.65;
}

.playback-prompt :deep(.attachment-panel) {
  margin: 0 18px 14px;
}

.playback-essential-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 0 18px 4px;
}

.playback-essential-meta span {
  border: 1px solid #e2e5ec;
  border-radius: 999px;
  padding: 7px 11px;
  color: #747e8f;
  background: #f8f9fb;
  font-size: 14px;
}

.playback-essential-meta b {
  color: #4f43b5;
}

.playback-step-controls {
  gap: 10px;
  padding: 14px 18px 18px;
}

.playback-step-controls button,
.playback-state-action {
  min-height: 44px;
  border-radius: 10px;
  font-size: 16px;
}

.playback-state-action {
  width: calc(100% - 36px);
  margin: 0 18px 18px;
}

.playback-feedback {
  margin: 0 18px 16px;
  border-radius: 10px;
  padding: 11px 12px;
  font-size: 15px;
}

@media (max-height: 680px) {
  .playback-prompt :deep(.attachment-panel) {
    display: block;
  }

  .playback-directory-panel > header {
    min-height: 86px;
    padding-block: 12px;
  }
}

@media (max-width: 720px) {
  .playback-menu-shell {
    top: 8px;
    bottom: 8px;
    left: 8px;
    max-height: calc(100vh - 16px);
  }

  .playback-menu-rail {
    width: 82px;
    flex-basis: 82px;
    padding-inline: 6px;
  }

  .playback-directory-panel {
    width: min(320px, calc(100vw - 98px));
  }

  .playback-directory-panel > header em,
  .playback-directory-panel > footer {
    display: none;
  }

  .playback-prompt,
  .playback-prompt.stage-prompt,
  .playback-prompt.node-prompt.prompt-left,
  .playback-prompt.node-prompt.prompt-right,
  .playback-prompt.node-prompt.prompt-bottom {
    max-height: 58vh;
  }

  .playback-instruction,
  .playback-prompt :deep(.attachment-panel) {
    display: block;
  }
}
</style>
