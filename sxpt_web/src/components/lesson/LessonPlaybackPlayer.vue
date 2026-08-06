<script setup lang="ts">
import { computed, ref } from 'vue';
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

interface PlaybackStep {
  stage: LessonStage;
  step: RecordedStep;
  stageIndex: number;
  stepIndex: number;
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

const navigationOpen = ref(false);
const promptCollapsed = ref(false);
const promptPosition = ref<'left' | 'right' | 'bottom'>('left');
const previewAttachment = ref<TrainingAttachment | null>(null);
const attachmentPreviewMinimized = ref(false);

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

function formatDuration(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(remainder).padStart(2, '0')}`;
}

function toggleNavigation() {
  navigationOpen.value = !navigationOpen.value;
  if (navigationOpen.value && promptPosition.value === 'right') {
    promptPosition.value = 'left';
  }
}

function cyclePromptPosition() {
  promptPosition.value =
    promptPosition.value === 'left'
      ? 'right'
      : promptPosition.value === 'right'
        ? 'bottom'
        : 'left';
}

function openAttachmentPreview(attachment: TrainingAttachment) {
  previewAttachment.value = attachment;
  attachmentPreviewMinimized.value = false;
}

function closeAttachmentPreview() {
  previewAttachment.value = null;
  attachmentPreviewMinimized.value = false;
}

function minimizeAttachmentPreview() {
  if (previewAttachment.value) attachmentPreviewMinimized.value = true;
}

function restoreAttachmentPreview() {
  attachmentPreviewMinimized.value = false;
}
</script>

<template>
  <section class="lesson-playback-player">
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

    <nav class="playback-edge-toolbar" aria-label="讲解与学习控制">
      <RouterLink :to="returnTo" :title="returnLabel" aria-label="返回">
        ←
      </RouterLink>
      <button
        type="button"
        :class="{ active: navigationOpen }"
        :aria-expanded="navigationOpen"
        title="教学点与节点目录"
        @click="toggleNavigation"
      >
        目录
      </button>
      <button
        type="button"
        :aria-expanded="!promptCollapsed"
        title="收起或展开当前说明"
        @click="promptCollapsed = !promptCollapsed"
      >
        {{ promptCollapsed ? '说明' : '收起' }}
      </button>
    </nav>

    <aside
      v-if="navigationOpen"
      class="playback-navigation-drawer"
      aria-label="教学点与节点目录"
    >
      <header>
        <div>
          <small>流程讲解</small>
          <strong>{{ lesson.title }}</strong>
        </div>
        <button type="button" aria-label="关闭目录" @click="navigationOpen = false">×</button>
      </header>
      <div class="playback-metrics">
        <span><small>教学点</small><strong>{{ teachingPoints.length }}</strong></span>
        <span><small>节点</small><strong>{{ steps.length }}</strong></span>
        <span><small>时长</small><strong>{{ formatDuration(totalDuration) }}</strong></span>
        <span><small>进度</small><strong>{{ progress }}%</strong></span>
      </div>
      <div class="playback-drawer-progress"><i :style="{ width: `${progress}%` }"></i></div>
      <section>
        <h2>教学点</h2>
        <button
          v-for="(stage, index) in teachingPoints"
          :key="stage.id"
          type="button"
          :disabled="playerState !== 'PLAYING'"
          :class="{
            active: stage.id === current?.stage.id && showStageIntroduction
          }"
          @click="emit('selectStage', stage.id)"
        >
          <span>{{ index + 1 }}</span>
          <strong>{{ stage.name }}</strong>
          <small>{{ stage.recordedSteps.length }} 个节点</small>
        </button>
      </section>
      <section>
        <h2>操作节点</h2>
        <button
          v-for="(item, index) in steps"
          :key="item.step.id"
          type="button"
          :disabled="playerState !== 'PLAYING'"
          :class="{ active: index === currentIndex && !showStageIntroduction }"
          @click="emit('selectStep', index)"
        >
          <span>{{ index + 1 }}</span>
          <strong>{{ item.step.title }}</strong>
          <small>{{ item.stage.name }}</small>
        </button>
      </section>
    </aside>

    <aside
      class="playback-prompt"
      :class="[
        `prompt-${promptPosition}`,
        {
          collapsed: promptCollapsed,
          'stage-prompt': showStageIntroduction,
          'node-prompt': !showStageIntroduction
        }
      ]"
    >
      <header class="playback-prompt-toolbar">
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
            <span>流程讲解</span>
            <strong>{{ lesson.title }}</strong>
            <small>学习内容与教师讲解使用同一界面</small>
          </div>
          <div class="playback-instruction">
            <span>开始说明</span>
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
            <span>流程讲解</span>
            <strong>本次流程已经完成</strong>
            <small>{{ lesson.title }}</small>
          </div>
          <div class="playback-instruction">
            <span>完成说明</span>
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
              <span>
                本教学点说明 · 教学点 {{ current.stageIndex + 1 }} /
                {{ teachingPoints.length }}
              </span>
              <strong>{{ current.stage.name }}</strong>
              <small>{{ current.stage.groupKey || '未指定业务角色' }}</small>
            </div>
            <div class="playback-instruction">
              <span>教学点目标与注意事项</span>
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
            <dl>
              <div><dt>教学点节点</dt><dd>{{ current.stage.recordedSteps.length }} 个</dd></div>
              <div><dt>负责角色</dt><dd>{{ current.stage.groupKey || '未指定' }}</dd></div>
              <div><dt>教学点分值</dt><dd>{{ current.stage.score }} 分</dd></div>
              <div><dt>完成依据</dt><dd>{{ current.stage.completionMethod }}</dd></div>
            </dl>
          </template>
          <template v-else>
            <div class="playback-heading">
              <span>本节点说明 · 步骤 {{ currentIndex + 1 }} / {{ steps.length }}</span>
              <strong>{{ currentStep.title }}</strong>
              <small>{{ current.stage.name }} · {{ current.stage.groupKey }}</small>
            </div>
            <div class="playback-instruction">
              <span>逐步讲解</span>
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
            <dl>
              <div><dt>pageTitle</dt><dd>{{ currentStep.pageTitle }}</dd></div>
              <div><dt>actionLabel</dt><dd>{{ currentStep.actionLabel }}</dd></div>
              <div><dt>selector</dt><dd><code>{{ currentStep.selector }}</code></dd></div>
              <div><dt>durationSeconds</dt><dd>{{ currentStep.durationSeconds }} 秒</dd></div>
              <div><dt>完成依据</dt><dd>{{ current.stage.completionMethod }}</dd></div>
            </dl>
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

    <footer class="playback-progress-strip" aria-label="流程进度">
      <span>{{ formatDuration(elapsedDuration) }}</span>
      <div><i :style="{ width: `${progress}%` }"></i></div>
      <span>{{ formatDuration(totalDuration) }}</span>
    </footer>

    <AttachmentPreviewLayer
      :attachment="previewAttachment"
      :minimized="attachmentPreviewMinimized"
      @close="closeAttachmentPreview"
      @minimize="minimizeAttachmentPreview"
      @restore="restoreAttachmentPreview"
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
</style>
