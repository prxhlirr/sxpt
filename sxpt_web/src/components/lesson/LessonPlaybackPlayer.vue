<script setup lang="ts">
import { computed, ref } from 'vue';
import type { RouteLocationRaw } from 'vue-router';
import type { LessonPlan, LessonStage, RecordedStep } from '../../domain/models';
import AttachmentPanel from './AttachmentPanel.vue';
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
    returnLabel: string;
    finishLabel: string;
    actionDisabled?: boolean;
    feedback?: string;
    feedbackTone?: 'success' | 'danger' | string;
    playerState?: string;
  }>(),
  {
    actionDisabled: false,
    feedback: '',
    feedbackTone: 'success',
    playerState: ''
  }
);

const emit = defineEmits<{
  previous: [];
  next: [];
  finish: [];
  'select-step': [index: number];
  'select-stage': [stageId: string];
}>();
const showNavigationDrawer = ref(true);
const promptPosition = ref<'top-right' | 'bottom-right' | 'bottom-left' | 'top-left'>(
  'top-right'
);

const playbackSteps = computed<PlaybackStep[]>(() =>
  props.lesson.stages.flatMap((stage, stageIndex) =>
    stage.recordedSteps.map((step, stepIndex) => ({
      stage,
      step,
      stageIndex,
      stepIndex
    }))
  )
);
const current = computed(() => playbackSteps.value[props.currentIndex]);
const currentStage = computed(() => current.value?.stage ?? props.lesson.stages[0]);
const currentStep = computed(() => current.value?.step);
const hasPrevious = computed(
  () => props.currentIndex > 0 || (!props.showStageIntroduction && current.value?.stepIndex === 0)
);
const hasNext = computed(() => {
  if (props.showStageIntroduction) return Boolean(currentStep.value);
  return props.currentIndex < playbackSteps.value.length - 1;
});
const progressText = computed(() =>
  playbackSteps.value.length
    ? `${Math.min(props.currentIndex + 1, playbackSteps.value.length)} / ${playbackSteps.value.length}`
    : '0 / 0'
);
const promptPositionClass = computed(() => `prompt-${promptPosition.value}`);

/**
 * 业务功能：向父页面提交“下一步或完成”意图。
 * 关键流程：仍由父页面维护播放索引和完成业务，组件只负责统一交互入口。
 */
function handlePrimaryAction() {
  if (hasNext.value) {
    emit('next');
    return;
  }
  emit('finish');
}

/**
 * 业务功能：切换教师讲解提示卡片位置，避免遮挡被录制的业务操作区域。
 * 关键流程：按四角顺序轮换，状态只影响本组件展示，不改变录制数据。
 */
function cyclePromptPosition() {
  const positions: Array<typeof promptPosition.value> = [
    'top-right',
    'bottom-right',
    'bottom-left',
    'top-left'
  ];
  const currentIndex = positions.indexOf(promptPosition.value);
  promptPosition.value = positions[(currentIndex + 1) % positions.length];
}
</script>

<template>
  <section class="lesson-playback-player">
    <aside class="playback-sidebar">
      <RouterLink class="playback-return" :to="returnTo">{{ returnLabel }}</RouterLink>
      <header>
        <span>课程回放</span>
        <strong>{{ lesson.title }}</strong>
        <small>{{ progressText }}</small>
      </header>

      <nav class="playback-stage-list">
        <button
          v-for="stage in lesson.stages"
          :key="stage.id"
          type="button"
          :class="{ active: stage.id === currentStage?.id }"
          @click="emit('select-stage', stage.id)"
        >
          <strong>{{ stage.name }}</strong>
          <small>{{ stage.recordedSteps.length }} 个节点</small>
        </button>
      </nav>
    </aside>

    <main class="playback-main">
      <div class="playback-edge-toolbar">
        <button type="button" @click="showNavigationDrawer = !showNavigationDrawer">
          {{ showNavigationDrawer ? '隐藏菜单' : '显示菜单' }}
        </button>
        <button type="button" @click="cyclePromptPosition">换个角落</button>
      </div>

      <aside
        v-if="showNavigationDrawer"
        class="playback-navigation-drawer"
      >
        <strong>教学点目标与注意事项</strong>
        <p>{{ currentStage?.description || '本教学点说明' }}</p>
      </aside>

      <header class="playback-topbar">
        <div>
          <span>{{ currentStage?.name || '暂无教学点' }}</span>
          <strong>
            {{ showStageIntroduction ? '教学点说明' : currentStep?.title || '暂无录制节点' }}
          </strong>
        </div>
        <div class="playback-actions">
          <button type="button" :disabled="!hasPrevious" @click="emit('previous')">
            上一步
          </button>
          <button
            class="primary"
            type="button"
            :disabled="actionDisabled || (!hasNext && playbackSteps.length === 0)"
            @click="handlePrimaryAction"
          >
            {{ hasNext ? '下一步' : finishLabel }}
          </button>
        </div>
      </header>

      <p
        v-if="feedback"
        class="playback-feedback"
        :class="feedbackTone === 'danger' ? 'danger' : 'success'"
      >
        {{ feedback }}
      </p>

      <section v-if="showStageIntroduction && currentStage" class="stage-introduction">
        <span>{{ currentStage.groupKey || '通用角色' }}</span>
        <h1>{{ currentStage.name }}</h1>
        <p>{{ currentStage.description || '本教学点说明' }}</p>
        <button class="primary" type="button" @click="emit('next')">
          进入本教学点
        </button>
        <AttachmentPanel :attachments="currentStage.attachments" title="教学点附件" />
      </section>

      <section v-else-if="currentStep" class="step-playback">
        <div class="step-frame">
          <BusinessSnapshotFrame
            :snapshot="currentStep.pageSnapshot"
            :fallback-url="currentStep.url"
            :selector="showStageIntroduction ? undefined : currentStep.selector"
            :selector-candidates="currentStep.selectorCandidates"
            :rect="currentStep.rect"
            :recorded-viewport="currentStep.recordedViewport"
            :title="currentStep.pageTitle"
            fit-mode="fill"
          />
        </div>
        <aside
          class="step-notes"
          :class="[
            promptPositionClass,
            {
              'stage-prompt': showStageIntroduction,
              'node-prompt': !showStageIntroduction
            }
          ]"
        >
          <span>{{ currentStep.actionLabel }}</span>
          <h2>{{ currentStep.title }}</h2>
          <p>{{ currentStep.teachingText || currentStep.note || '本节点说明' }}</p>
          <AttachmentPanel :attachments="currentStep.attachments" title="节点附件" />
          <dl>
            <div>
              <dt>pageTitle</dt>
              <dd>{{ currentStep.pageTitle }}</dd>
            </div>
            <div>
              <dt>actionLabel</dt>
              <dd>{{ currentStep.actionLabel }}</dd>
            </div>
            <div>
              <dt>selector</dt>
              <dd>{{ currentStep.selector }}</dd>
            </div>
            <div>
              <dt>durationSeconds</dt>
              <dd>{{ currentStep.durationSeconds }}</dd>
            </div>
          </dl>
          <div class="step-list">
            <button
              v-for="(item, index) in playbackSteps"
              :key="item.step.id"
              type="button"
              :class="{ active: index === currentIndex }"
              @click="emit('select-step', index)"
            >
              <span>{{ index + 1 }}</span>
              <strong>{{ item.step.title }}</strong>
            </button>
          </div>
          <button class="primary" type="button" @click="emit('next')">
            下一步 →
          </button>
        </aside>
      </section>

      <section v-else class="empty-playback">
        <strong>暂无录制节点</strong>
        <p>请先在教案编辑页录制教学点流程。</p>
      </section>
    </main>
  </section>
</template>

<style scoped>
.lesson-playback-player {
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  min-height: 100vh;
  background: #f5f7fb;
}

.playback-sidebar {
  display: grid;
  align-content: start;
  gap: 16px;
  border-right: 1px solid #e1e7f0;
  background: #fff;
  padding: 18px;
}

.playback-return {
  width: fit-content;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
}

.playback-sidebar header,
.playback-topbar > div:first-child,
.stage-introduction,
.step-notes {
  display: grid;
  gap: 6px;
}

.playback-sidebar span,
.playback-topbar span,
.stage-introduction span,
.step-notes span {
  color: #7b8797;
  font-size: 11px;
  font-weight: 800;
}

.playback-sidebar strong {
  color: #172033;
  font-size: 16px;
}

.playback-sidebar small {
  color: #8c97a8;
  font-size: 11px;
}

.playback-stage-list,
.step-list {
  display: grid;
  gap: 8px;
}

.playback-stage-list button,
.step-list button {
  display: grid;
  justify-content: initial;
  gap: 3px;
  min-height: 48px;
  border-color: #e7ebf2;
  border-radius: 8px;
  padding: 9px 10px;
  text-align: left;
}

.playback-stage-list button.active,
.step-list button.active {
  border-color: #b8d4ff;
  background: #eef6ff;
  color: #1d4ed8;
}

.playback-main {
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  min-width: 0;
}

.playback-topbar {
  display: flex;
  min-height: 74px;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border-bottom: 1px solid #e6ebf2;
  background: #fff;
  padding: 0 24px;
}

.playback-topbar strong {
  font-size: 18px;
}

.playback-actions {
  display: flex;
  gap: 8px;
}

.playback-feedback {
  margin: 14px 24px 0;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 12px;
  font-weight: 700;
}

.playback-feedback.success {
  color: #087b59;
  background: #eaf8f2;
}

.playback-feedback.danger {
  color: #b43a46;
  background: #fff1f2;
}

.stage-introduction {
  align-content: center;
  width: min(760px, calc(100% - 48px));
  margin: 24px auto;
}

.stage-introduction h1 {
  margin: 0;
  font-size: 30px;
}

.stage-introduction p,
.step-notes p,
.empty-playback p {
  margin: 0;
  color: #657388;
  line-height: 1.7;
}

.step-playback {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  min-height: 0;
  padding: 18px;
  gap: 18px;
}

.step-frame {
  min-height: 0;
  overflow: hidden;
  border: 1px solid #dfe6ef;
  border-radius: 8px;
  background: #eef2f7;
}

.step-notes {
  align-content: start;
  overflow-y: auto;
  border: 1px solid #e4e9f1;
  border-radius: 8px;
  background: #fff;
  padding: 16px;
}

.step-notes h2 {
  margin: 0;
  font-size: 18px;
}

.step-list {
  margin-top: 8px;
}

.step-list button {
  grid-template-columns: 24px minmax(0, 1fr);
  align-items: center;
}

.step-list span {
  display: grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border-radius: 6px;
  background: #eef2f7;
}

.empty-playback {
  display: grid;
  place-items: center;
  align-content: center;
  gap: 8px;
  color: #64748b;
  text-align: center;
}

@media (max-width: 980px) {
  .lesson-playback-player,
  .step-playback {
    grid-template-columns: 1fr;
  }

  .playback-sidebar {
    display: none;
  }
}
</style>
