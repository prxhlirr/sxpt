<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import BusinessFrame from '../components/BusinessFrame.vue';
import ModeSwitcher from '../components/ModeSwitcher.vue';
import SubmissionPanel from '../components/SubmissionPanel.vue';
import TeachingOverlay from '../components/TeachingOverlay.vue';
import type { BusinessReadyMessage } from '../engine/captureProtocol';
import { migrateRecordedFlow } from '../engine/captureWorkspace';
import { recordEvent } from '../engine/eventRecorder';
import { createInitialSession, submitExam } from '../engine/flowState';
import {
  doesActionMatchStep,
  getPlaybackEntries
} from '../engine/recordingFlow';
import { canEnterPublishedFlow } from '../engine/publishedFlowAccess';
import { loadPublishedRecordedFlow } from '../engine/recordingStorage';
import {
  getVisibleOverlayStep,
  isSameBusinessPage
} from '../engine/businessPageState';
import {
  getPlaybackPreparation,
  shouldHandlePlaybackFailure,
  shouldReprepareAfterPlayback
} from '../engine/playbackPreparation';
import { purchaseLessonFlow } from '../mock/lessonFlows';
import type {
  BusinessActionPayload,
  RecordedStep,
  Rect,
  RunnerMode,
  TaskSubmission
} from '../types/domain';

const storedFlow = loadPublishedRecordedFlow();
const recordedFlow = canEnterPublishedFlow(storedFlow)
  ? migrateRecordedFlow(storedFlow!)
  : undefined;
const mode = ref<RunnerMode>('learning');
const session = ref(
  recordedFlow ? createInitialSession(purchaseLessonFlow, mode.value) : undefined
);
const playbackIndex = ref(0);
const isAutoPlaying = ref(true);
const statusText = ref(
  recordedFlow
    ? '学习模式：系统将按多段备案路径自动演示。'
    : '暂无已发布教案。'
);
const businessFrameRef = ref<InstanceType<typeof BusinessFrame> | null>(null);
const latestBusinessUrl = ref('');
const frameReady = ref(false);
const completedPracticeStepIds = ref<string[]>([]);
const showFlowPanel = ref(false);
const controlsCollapsed = ref(false);
const runtimeRect = ref<Rect>();
const currentStepPlayed = ref(false);
let playbackTimer: number | undefined;
let resizeTimer: number | undefined;
let lastPlayedKey = '';
let playing = false;
let resolutionGeneration = 0;

const playbackEntries = computed(() =>
  recordedFlow ? getPlaybackEntries(recordedFlow) : []
);
const currentEntry = computed(() => playbackEntries.value[playbackIndex.value]);
const currentStep = computed(() => currentEntry.value?.step);
const overlayStep = computed<RecordedStep | undefined>(() => {
  const entry = currentEntry.value;
  return getVisibleOverlayStep({
    step: entry?.step,
    stepUrl: entry?.step.url || entry?.targetUrl,
    frameReady: frameReady.value,
    currentUrl: latestBusinessUrl.value,
    runtimeRect: runtimeRect.value
  });
});

watch(mode, (nextMode) => {
  if (!recordedFlow) return;
  clearPlaybackTimer();
  playbackIndex.value = 0;
  lastPlayedKey = '';
  currentStepPlayed.value = false;
  session.value = createInitialSession(purchaseLessonFlow, nextMode);
  completedPracticeStepIds.value = [];
  isAutoPlaying.value = nextMode === 'learning';
  statusText.value =
    nextMode === 'learning'
      ? '学习模式：系统将按多段备案路径自动演示。'
      : nextMode === 'practice'
        ? '练习模式：请按分段流程自行完成业务操作。'
        : '考试模式：流程提示已隐藏，系统仅记录跨页面操作轨迹。';
  invalidateBusinessFrame();
  businessFrameRef.value?.reload(recordedFlow.businessUrl);
});

watch(isAutoPlaying, (enabled) => {
  if (!enabled) {
    clearPlaybackTimer();
    return;
  }
  if (currentStepPlayed.value) {
    scheduleAutoPlay();
  } else {
    prepareCurrentEntry();
  }
});
watch(
  currentEntry,
  () => {
    clearPlaybackTimer();
    lastPlayedKey = '';
    currentStepPlayed.value = false;
    runtimeRect.value = undefined;
    resolutionGeneration += 1;
    prepareCurrentEntry();
  },
  { flush: 'post' }
);

function clearPlaybackTimer() {
  if (playbackTimer) {
    window.clearTimeout(playbackTimer);
    playbackTimer = undefined;
  }
}

function scheduleAutoPlay() {
  clearPlaybackTimer();
  if (!currentStepPlayed.value) return;
  const step = currentStep.value;
  if (
    mode.value !== 'learning' ||
    !isAutoPlaying.value ||
    !step ||
    (step.kind === 'guide' && (step.advanceMode ?? 'manual') === 'manual')
  ) {
    return;
  }
  playbackTimer = window.setTimeout(
    goNext,
    Math.max(800, step.durationMs ?? 2600)
  );
}

function invalidateBusinessFrame() {
  frameReady.value = false;
  latestBusinessUrl.value = '';
  runtimeRect.value = undefined;
  resolutionGeneration += 1;
}

function handleBusinessReady(payload: BusinessReadyMessage) {
  frameReady.value = true;
  latestBusinessUrl.value = payload.url;
  businessFrameRef.value?.setRecording(mode.value !== 'learning');
  prepareCurrentEntry();
}

function prepareCurrentEntry() {
  const entry = currentEntry.value;
  if (!entry || mode.value === 'exam') return;
  const targetUrl = entry.step.url || entry.targetUrl;

  const decision = getPlaybackPreparation({
    frameReady: frameReady.value,
    currentUrl: latestBusinessUrl.value,
    targetUrl,
    learning: mode.value === 'learning',
    playbackInFlight: playing,
    currentStepPlayed: currentStepPlayed.value
  });

  if (decision === 'wait' || decision === 'await-advance') return;

  if (decision === 'navigate') {
    invalidateBusinessFrame();
    statusText.value = `正在进入第 ${entry.segmentNo} 段：${entry.roleName || entry.segmentTitle}`;
    businessFrameRef.value?.navigate(targetUrl);
    return;
  }

  if (entry.isSegmentStart) {
    statusText.value = `第 ${entry.segmentNo} 段 · ${entry.roleName || '当前角色'}${entry.orgName ? ` · ${entry.orgName}` : ''}${entry.switchReason ? `：${entry.switchReason}` : ''}`;
  }

  if (mode.value === 'learning') {
    void playCurrentStep();
  } else if (mode.value === 'practice') {
    void resolveCurrentTarget();
  }
}

async function resolveCurrentTarget(allowRetry = true): Promise<boolean> {
  const step = currentStep.value;
  if (!step || !frameReady.value) return false;
  if (step.kind === 'guide' && step.anchor?.mode !== 'element') return true;

  const generation = ++resolutionGeneration;
  runtimeRect.value = undefined;
  try {
    const result = await businessFrameRef.value?.resolveStepTarget(step);
    if (
      !result?.rect ||
      generation !== resolutionGeneration ||
      currentStep.value?.id !== step.id
    ) {
      return false;
    }
    runtimeRect.value = result.rect;
    return true;
  } catch (error) {
    if (generation !== resolutionGeneration) return false;
    if (step.failurePolicy === 'retry' && allowRetry) {
      return resolveCurrentTarget(false);
    }
    statusText.value = `当前页面未找到目标元素：${step.title}`;
    if (step.failurePolicy === 'skip') {
      goNext();
    } else {
      isAutoPlaying.value = false;
    }
    return false;
  }
}

async function playCurrentStep() {
  const entry = currentEntry.value;
  const step = entry?.step;
  if (!entry || !step || mode.value !== 'learning' || !frameReady.value) return;
  const playKey = `${mode.value}:${playbackIndex.value}:${step.id}:${latestBusinessUrl.value}`;
  if (lastPlayedKey === playKey || playing) return;

  playing = true;
  try {
    if (step.kind === 'guide') {
      const resolved = await resolveCurrentTarget();
      if (!resolved || currentStep.value?.id !== step.id) return;
      lastPlayedKey = playKey;
      currentStepPlayed.value = true;
      statusText.value = `说明：${step.title}`;
      scheduleAutoPlay();
      return;
    }

    await nextTick();
    const resolved = await resolveCurrentTarget();
    if (!resolved) return;
    const result = await businessFrameRef.value?.playStep(step);
    if (currentStep.value?.id !== step.id) return;
    if (
      result?.rect &&
      frameReady.value &&
      isSameBusinessPage(latestBusinessUrl.value, step.url || entry.targetUrl)
    ) {
      runtimeRect.value = result.rect;
    }
    lastPlayedKey = playKey;
    currentStepPlayed.value = true;
    statusText.value = `演示第 ${playbackIndex.value + 1} 步：${step.title}`;
    scheduleAutoPlay();
  } catch (error) {
    lastPlayedKey = '';
    if (!shouldHandlePlaybackFailure(step.id, currentStep.value?.id)) return;
    statusText.value = `演示失败：${error instanceof Error ? error.message : step.title}`;
    if (step.failurePolicy === 'skip') {
      goNext();
    } else if (step.failurePolicy !== 'retry') {
      isAutoPlaying.value = false;
    }
  } finally {
    playing = false;
    if (
      shouldReprepareAfterPlayback({
        learning: mode.value === 'learning',
        autoPlaying: isAutoPlaying.value,
        frameReady: frameReady.value,
        currentStepPlayed: currentStepPlayed.value,
        currentStepExists: Boolean(currentStep.value),
        currentStepChanged: currentStep.value?.id !== step.id
      })
    ) {
      lastPlayedKey = '';
      prepareCurrentEntry();
    }
  }
}

function goNext() {
  if (playbackIndex.value < playbackEntries.value.length - 1) {
    playbackIndex.value += 1;
    return;
  }
  isAutoPlaying.value = false;
  clearPlaybackTimer();
  statusText.value = '学习演示已播放完毕，可以切换练习模式自行操作。';
}

function goPrevious() {
  playbackIndex.value = Math.max(0, playbackIndex.value - 1);
  isAutoPlaying.value = false;
  statusText.value = `回到第 ${playbackIndex.value + 1} 步：${currentStep.value?.title ?? ''}`;
}

function selectStep(index: number) {
  if (mode.value === 'exam') return;
  playbackIndex.value = index;
  isAutoPlaying.value = false;
  statusText.value = `已定位到第 ${index + 1} 步：${playbackEntries.value[index]?.step.title ?? ''}`;
}

function handleBusinessAction(payload: BusinessActionPayload) {
  if (!session.value) return;
  session.value = recordEvent(session.value, {
    type: 'business_action',
    actionType: payload.actionType,
    url: payload.url,
    selector: payload.selector,
    text: payload.text,
    value: payload.value,
    nodeId: currentStep.value?.nodeId,
    timestamp: payload.timestamp
  });

  if (mode.value === 'learning') return;
  if (mode.value === 'exam') {
    statusText.value = `已记录 ${session.value.events.length} 条考试操作轨迹。`;
    return;
  }

  const step = currentStep.value;
  if (!step) return;
  if (step.kind === 'guide') {
    statusText.value = '请阅读当前说明，然后点击下一步继续。';
    return;
  }

  if (doesActionMatchStep(payload, step)) {
    if (!completedPracticeStepIds.value.includes(step.id)) {
      completedPracticeStepIds.value = [...completedPracticeStepIds.value, step.id];
    }
    statusText.value = `练习通过：${step.title}`;
    goNext();
  } else {
    statusText.value = `当前应完成“${step.title}”，请回到高亮区域操作。`;
  }
}

function submit(submission: TaskSubmission) {
  if (!session.value) return;
  session.value = submitExam(purchaseLessonFlow, session.value, submission);
  statusText.value = `考试已提交：${submission.businessId}`;
}

function isPracticeCompleted(stepId: string) {
  return completedPracticeStepIds.value.includes(stepId);
}

function handleViewportResize() {
  if (resizeTimer) window.clearTimeout(resizeTimer);
  resizeTimer = window.setTimeout(() => {
    if (recordedFlow && frameReady.value && mode.value !== 'exam') {
      void resolveCurrentTarget();
    }
  }, 180);
}

onMounted(() => window.addEventListener('resize', handleViewportResize));

onBeforeUnmount(() => {
  clearPlaybackTimer();
  if (resizeTimer) window.clearTimeout(resizeTimer);
  window.removeEventListener('resize', handleViewportResize);
});
</script>

<template>
  <section class="immersive-workspace student-playback segmented-student-playback">
    <template v-if="recordedFlow">
    <main class="immersive-business-layer">
      <BusinessFrame
        ref="businessFrameRef"
        :src="recordedFlow.businessUrl"
        title="采购申请业务系统"
        @business-action="handleBusinessAction"
        @business-ready="handleBusinessReady"
        @frame-navigating="invalidateBusinessFrame"
        @frame-load="invalidateBusinessFrame"
      />
      <TeachingOverlay
        :step="mode === 'exam' ? undefined : overlayStep"
        :visible="mode !== 'exam' && Boolean(overlayStep)"
        :show-hint="mode === 'learning' || currentStep?.kind === 'guide'"
        @next="goNext"
        @previous="goPrevious"
      />
    </main>

    <button type="button" class="floating-collapse-toggle" @click="controlsCollapsed = !controlsCollapsed">
      {{ controlsCollapsed ? '展开操作' : '隐藏全部' }}
    </button>

    <header v-if="!controlsCollapsed" class="floating-topbar command-strip">
      <div>
        <p class="eyebrow">分段学习运行</p>
        <h2>学习 / 练习 / 考试</h2>
      </div>
      <div class="topbar-actions">
        <ModeSwitcher v-model="mode" />
        <RouterLink class="glass-link" to="/">返回编排</RouterLink>
      </div>
    </header>

    <div v-if="!controlsCollapsed" class="floating-glass floating-status">
      <span>{{ statusText }}</span>
      <span>
        第 {{ currentEntry?.segmentNo ?? 1 }} 段 ·
        {{ playbackIndex + 1 }} / {{ playbackEntries.length }} ·
        轨迹 {{ session?.events.length ?? 0 }} 条
      </span>
    </div>

    <div v-if="!controlsCollapsed && mode !== 'exam'" class="floating-glass floating-quickbar">
      <button type="button" class="secondary-action" @click="showFlowPanel = !showFlowPanel">
        {{ showFlowPanel ? '隐藏节点' : '显示节点' }}
      </button>
    </div>

    <aside v-if="!controlsCollapsed && mode !== 'exam' && showFlowPanel" class="floating-glass floating-step-panel student-segment-panel">
      <div class="panel-header">
        <div>
          <p class="eyebrow">分段录制流程</p>
          <h3>{{ recordedFlow?.title }}</h3>
        </div>
        <strong>{{ recordedFlow.segments?.length ?? 1 }} 段</strong>
      </div>
      <ol class="recorded-step-list">
        <li
          v-for="(entry, index) in playbackEntries"
          :key="entry.step.id"
          :class="{
            active: playbackIndex === index,
            success: mode === 'practice' && isPracticeCompleted(entry.step.id)
          }"
          @click="selectStep(index)"
        >
          <span class="step-index">{{ index + 1 }}</span>
          <div>
            <small class="segment-kicker">第 {{ entry.segmentNo }} 段 · {{ entry.roleName || entry.segmentTitle }}</small>
            <strong>{{ entry.step.title }}</strong>
            <p>{{ mode === 'learning' ? entry.step.teachingText : entry.step.practiceHint }}</p>
            <small v-if="mode === 'learning' && entry.step.value">录制值：{{ entry.step.value }}</small>
          </div>
        </li>
      </ol>
      <div v-if="mode === 'learning'" class="playback-controls">
        <button type="button" class="secondary-action" @click="goPrevious">上一步</button>
        <button type="button" @click="isAutoPlaying = !isAutoPlaying">
          {{ isAutoPlaying ? '暂停演示' : '继续演示' }}
        </button>
        <button type="button" class="secondary-action" @click="goNext">下一步</button>
      </div>
    </aside>

    <div v-if="!controlsCollapsed && mode === 'exam'" class="floating-glass floating-exam-panel">
      <SubmissionPanel :submitted="session?.submission" :event-count="session?.events.length ?? 0" @submit="submit" />
    </div>
    </template>

    <main v-else class="published-flow-empty">
      <div>
        <p class="eyebrow">学习入口</p>
        <h1>暂无已发布教案</h1>
        <p>请先返回流程编排，完成业务操作录制并发布教案。</p>
        <RouterLink class="primary-link" to="/">返回流程编排</RouterLink>
      </div>
    </main>
  </section>
</template>
