<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import AttachmentPanel from '../../components/lesson/AttachmentPanel.vue';
import BusinessSnapshotFrame from '../../components/lesson/BusinessSnapshotFrame.vue';
import type { LessonStage, RecordedStep } from '../../domain/models';
import { useTrainingStore } from '../../stores/trainingStore';

interface PreviewStep {
  stage: LessonStage;
  step: RecordedStep;
  stageIndex: number;
  stepIndex: number;
}

const route = useRoute();
const store = useTrainingStore();
const lessonId = computed(() => String(route.params.lessonId ?? ''));
const lesson = computed(() => store.getLesson(lessonId.value));
const openedFromPublishCenter = computed(() => route.query.from === 'publish');
const returnRoute = computed(() => ({
  name: openedFromPublishCenter.value ? 'publish-center' : 'lesson-editor',
  params: { lessonId: lessonId.value }
}));
const currentIndex = ref(0);
const lectureFeedback = ref('');
const lectureFeedbackSuccess = ref(false);
const showLectureOverlay = ref(true);
const showStageIntroduction = ref(true);

const previewSteps = computed<PreviewStep[]>(() =>
  (lesson.value?.stages ?? []).flatMap((stage, stageIndex) =>
    stage.recordedSteps.map((step, stepIndex) => ({
      stage,
      step,
      stageIndex,
      stepIndex
    }))
  )
);
const current = computed(() => previewSteps.value[currentIndex.value]);
const currentStep = computed(() => current.value?.step);
const lectureTeachingPoints = computed(() =>
  (lesson.value?.stages ?? []).filter((stage) => stage.recordedSteps.length)
);
const canMovePrevious = computed(
  () =>
    currentIndex.value > 0 ||
    (!showStageIntroduction.value && current.value?.stepIndex === 0)
);
const totalDuration = computed(() =>
  previewSteps.value.reduce((total, item) => total + item.step.durationSeconds, 0)
);
const elapsedDuration = computed(() =>
  previewSteps.value
    .slice(0, currentIndex.value + 1)
    .reduce((total, item) => total + item.step.durationSeconds, 0)
);
const progress = computed(() =>
  previewSteps.value.length
    ? Math.round(((currentIndex.value + 1) / previewSteps.value.length) * 100)
    : 0
);

watch(previewSteps, (steps) => {
  if (currentIndex.value >= steps.length) currentIndex.value = Math.max(0, steps.length - 1);
});

function formatDuration(seconds: number) {
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return `${String(minutes).padStart(2, '0')}:${String(remainder).padStart(2, '0')}`;
}

function selectStep(index: number) {
  currentIndex.value = Math.max(0, Math.min(index, previewSteps.value.length - 1));
  showStageIntroduction.value = false;
}

function selectTeachingPoint(stageId: string) {
  const index = previewSteps.value.findIndex((item) => item.stage.id === stageId);
  if (index < 0) return;
  currentIndex.value = index;
  showStageIntroduction.value = true;
}

function move(direction: -1 | 1) {
  if (direction === 1) {
    if (showStageIntroduction.value) {
      showStageIntroduction.value = false;
      return;
    }
    const following = previewSteps.value[currentIndex.value + 1];
    if (!following) return;
    const stageChanged = following.stage.id !== current.value?.stage.id;
    currentIndex.value += 1;
    showStageIntroduction.value = stageChanged;
    return;
  }

  if (!showStageIntroduction.value && current.value?.stepIndex === 0) {
    showStageIntroduction.value = true;
    return;
  }
  if (currentIndex.value <= 0) return;
  currentIndex.value -= 1;
  showStageIntroduction.value = false;
}

function finishLecture() {
  if (!lesson.value) return;
  try {
    store.markLessonLectureCompleted(lesson.value.id);
    lectureFeedbackSuccess.value = true;
    lectureFeedback.value = '教师讲解已完成，现在可以发布流程学习与练习任务。';
  } catch (error) {
    lectureFeedbackSuccess.value = false;
    lectureFeedback.value =
      error instanceof Error ? error.message : '暂时无法完成教师讲解。';
  }
}
</script>

<template>
  <div
    v-if="lesson"
    class="page preview-page"
    :class="{ 'lecture-overlay-hidden': !showLectureOverlay }"
  >
    <button
      class="lecture-overlay-toggle"
      type="button"
      @click="showLectureOverlay = !showLectureOverlay"
    >
      {{ showLectureOverlay ? '隐藏其他讲解菜单' : '显示完整讲解菜单' }}
    </button>

    <header v-show="showLectureOverlay" class="preview-header">
      <div>
        <span class="eyebrow">RECORDING PREVIEW</span>
        <h1>{{ lesson.title }}</h1>
        <p>逐步回放录制时的完整业务页面快照；快照只读，不会再次提交真实业务数据。</p>
      </div>
      <div class="preview-header__actions">
        <RouterLink
          class="button secondary"
          :to="returnRoute"
        >
          ← {{ openedFromPublishCenter ? '返回发布中心' : '返回编辑' }}
        </RouterLink>
        <span class="demo-badge">● 安全演示</span>
      </div>
    </header>

    <section v-show="showLectureOverlay" class="preview-metrics">
      <span><small>教学点</small><strong>{{ lesson.stages.length }}</strong></span>
      <span><small>录制片段</small><strong>{{ previewSteps.length }}</strong></span>
      <span><small>总时长</small><strong>{{ formatDuration(totalDuration) }}</strong></span>
      <span><small>当前进度</small><strong>{{ progress }}%</strong></span>
      <div class="preview-progress"><i :style="{ width: `${progress}%` }"></i></div>
    </section>

    <div v-if="current && currentStep" class="preview-layout">
      <main class="business-stage">
        <div v-show="showLectureOverlay" class="browser-chrome">
          <div class="browser-dots"><i></i><i></i><i></i></div>
          <div class="browser-address">
            {{ currentStep.pageSnapshot?.pageUrl ?? currentStep.url ?? currentStep.pageTitle }}
          </div>
          <span>页面快照 · 只读</span>
        </div>

        <BusinessSnapshotFrame
          class="snapshot-business-view"
          :snapshot="currentStep.pageSnapshot"
          :fallback-url="currentStep.url"
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

        <div v-show="showLectureOverlay" class="playback-bar">
          <span>{{ formatDuration(elapsedDuration) }}</span>
          <div><i :style="{ width: `${progress}%` }"></i></div>
          <span>{{ formatDuration(totalDuration) }}</span>
        </div>
      </main>

      <aside
        class="explanation-panel lecture-step-prompt"
        :class="{
          'stage-prompt': showStageIntroduction,
          'node-prompt': !showStageIntroduction
        }"
      >
        <template v-if="showStageIntroduction">
          <div class="explanation-heading stage-introduction-heading">
            <span>
              本教学点说明 · 教学点 {{ current.stageIndex + 1 }} /
              {{ lesson.stages.length }}
            </span>
            <strong>{{ current.stage.name }}</strong>
            <small>{{ current.stage.groupKey || '未指定业务角色' }}</small>
          </div>
          <div class="instruction stage-introduction">
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
          />
          <dl>
            <div><dt>教学点节点</dt><dd>{{ current.stage.recordedSteps.length }} 个</dd></div>
            <div><dt>负责角色</dt><dd>{{ current.stage.groupKey || '未指定' }}</dd></div>
            <div><dt>教学点分值</dt><dd>{{ current.stage.score }} 分</dd></div>
            <div>
              <dt>完成依据</dt>
              <dd>{{ current.stage.completionMethod }}</dd>
            </div>
          </dl>
        </template>
        <template v-else>
          <div class="explanation-heading">
            <span>本节点说明 · 步骤 {{ currentIndex + 1 }} / {{ previewSteps.length }}</span>
            <strong>{{ currentStep.title }}</strong>
            <small>{{ current.stage.name }} · {{ current.stage.groupKey }}</small>
          </div>
          <div class="instruction">
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
          />
          <dl>
            <div><dt>pageTitle</dt><dd>{{ currentStep.pageTitle }}</dd></div>
            <div><dt>actionLabel</dt><dd>{{ currentStep.actionLabel }}</dd></div>
            <div><dt>selector</dt><dd><code>{{ currentStep.selector }}</code></dd></div>
            <div><dt>durationSeconds</dt><dd>{{ currentStep.durationSeconds }} 秒</dd></div>
            <div>
              <dt>完成依据</dt>
              <dd>{{ current.stage.completionMethod }}</dd>
            </div>
          </dl>
        </template>
        <div class="step-controls">
          <button type="button" :disabled="!canMovePrevious" @click="move(-1)">
            ← 上一步
          </button>
          <button
            v-if="showStageIntroduction"
            class="primary"
            type="button"
            @click="move(1)"
          >
            进入本教学点 →
          </button>
          <button
            v-else-if="currentIndex < previewSteps.length - 1"
            class="primary"
            type="button"
            @click="move(1)"
          >
            下一步 →
          </button>
          <button
            v-else
            class="primary"
            type="button"
            :disabled="Boolean(lesson.lectureCompletedAt)"
            @click="finishLecture"
          >
            {{ lesson.lectureCompletedAt ? '讲解已完成' : '完成教师讲解' }}
          </button>
        </div>
        <p
          v-if="lectureFeedback"
          class="lecture-feedback"
          :class="{ success: lectureFeedbackSuccess, danger: !lectureFeedbackSuccess }"
        >
          {{ lectureFeedback }}
        </p>
        <RouterLink
          v-if="lesson.lectureCompletedAt"
          class="publish-link"
          :to="{ name: 'publish-center', params: { lessonId: lesson.id } }"
        >
          前往发布学习与练习 →
        </RouterLink>
      </aside>
    </div>

    <section v-else class="card empty-recording">
      <div class="empty-state">
        <div>
          <strong>暂无可回看的录制步骤</strong><br />
          请返回教案编排，为任意教学点添加录制步骤。
          <div class="empty-action">
            <RouterLink
              class="button primary"
              :to="returnRoute"
            >
              {{ openedFromPublishCenter ? '返回发布中心' : '返回编辑' }}
            </RouterLink>
          </div>
        </div>
      </div>
    </section>

    <section
      v-if="previewSteps.length"
      v-show="showLectureOverlay"
      class="card segment-timeline"
    >
      <div class="card-header">
        <div>
          <h2>教学点与节点导航</h2>
          <p>可直接选择任意教学点查看说明，或选择任意节点开始讲解。</p>
        </div>
      </div>
      <div class="teaching-point-list">
        <button
          v-for="(stage, index) in lectureTeachingPoints"
          :key="stage.id"
          type="button"
          :class="{
            active: stage.id === current?.stage.id && showStageIntroduction
          }"
          @click="selectTeachingPoint(stage.id)"
        >
          <span>{{ index + 1 }}</span>
          <strong>{{ stage.name }}</strong>
          <small>{{ stage.recordedSteps.length }} 个节点</small>
        </button>
      </div>
      <div class="segment-list">
        <button
          v-for="(item, index) in previewSteps"
          :key="item.step.id"
          type="button"
          :class="{ active: index === currentIndex }"
          @click="selectStep(index)"
        >
          <span>{{ index + 1 }}</span>
          <strong>{{ item.step.title }}</strong>
          <small>{{ item.stage.name }} · {{ item.step.durationSeconds }} 秒</small>
        </button>
      </div>
    </section>
  </div>

  <section v-else class="standalone-state">
    <span>404</span>
    <h1>教案不存在</h1>
    <p>无法加载本次录制回看。</p>
    <RouterLink class="button primary" :to="{ name: 'lesson-list' }">返回教案列表</RouterLink>
  </section>
</template>

<style scoped>
.preview-page {
  display: grid;
  gap: 16px;
}

.preview-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
}

.preview-header h1,
.preview-header p {
  margin: 0;
}

.preview-header h1 {
  font-size: clamp(24px, 3vw, 31px);
}

.preview-header p {
  margin-top: 8px;
  color: #758195;
  font-size: 12px;
}

.preview-header__actions {
  display: flex;
  align-items: center;
  gap: 9px;
}

.demo-badge {
  border: 1px solid #c7ecdd;
  border-radius: 999px;
  padding: 9px 12px;
  color: #0a805d;
  background: #effbf7;
  font-size: 10px;
  font-weight: 900;
}

.preview-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(100px, 1fr));
  position: relative;
  overflow: hidden;
  border: 1px solid #e2e5ed;
  border-radius: 12px;
  background: #fff;
}

.preview-metrics > span {
  display: grid;
  gap: 3px;
  border-right: 1px solid #eceef3;
  padding: 13px 18px 17px;
}

.preview-metrics small {
  color: #8993a4;
  font-size: 9px;
}

.preview-metrics strong {
  font-size: 17px;
}

.preview-progress {
  position: absolute;
  right: 0;
  bottom: 0;
  left: 0;
  height: 3px;
  background: #eeecff;
}

.preview-progress i,
.playback-bar div i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: linear-gradient(90deg, #6958ee, #9b90ff);
  transition: width 200ms ease;
}

.preview-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.8fr) minmax(280px, 0.65fr);
  min-height: 590px;
  gap: 16px;
}

.business-stage,
.explanation-panel {
  overflow: hidden;
  border: 1px solid #dfe3ec;
  border-radius: 14px;
  background: #fff;
  box-shadow: 0 12px 30px rgb(25 30 70 / 7%);
}

.business-stage {
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
}

.lecture-overlay-toggle {
  position: absolute;
  z-index: 60;
  top: 14px;
  right: 14px;
  min-height: 34px;
  border: 1px solid rgb(255 255 255 / 78%);
  border-radius: 999px;
  padding: 0 14px;
  color: #fff;
  background: rgb(32 39 58 / 78%);
  box-shadow: 0 10px 28px rgb(15 20 40 / 22%);
  backdrop-filter: blur(12px);
  font-size: 9px;
  font-weight: 800;
}

.lecture-overlay-hidden .business-stage {
  grid-template-rows: minmax(0, 1fr);
}

.snapshot-business-view {
  min-height: 0;
}

.browser-chrome {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  min-height: 38px;
  background: #242739;
  padding: 0 13px;
  color: #9da5ba;
  font-size: 9px;
}

.browser-dots {
  display: flex;
  gap: 5px;
}

.browser-dots i {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #e46870;
}

.browser-dots i:nth-child(2) {
  background: #dca448;
}

.browser-dots i:nth-child(3) {
  background: #55b98f;
}

.browser-address {
  overflow: hidden;
  border-radius: 5px;
  padding: 5px 10px;
  background: #303447;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.business-toolbar {
  display: flex;
  min-height: 57px;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8ebf1;
  padding: 0 18px;
}

.business-toolbar > div:first-child {
  display: grid;
  gap: 3px;
}

.business-toolbar span {
  color: #8b95a6;
  font-size: 9px;
}

.business-toolbar strong {
  font-size: 14px;
}

.mock-user {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #687488;
  font-size: 10px;
}

.mock-user span {
  display: grid;
  width: 27px;
  height: 27px;
  place-items: center;
  border-radius: 50%;
  color: #fff;
  background: #6d5dfc;
  font-weight: 900;
}

.mock-business-screen {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  min-height: 440px;
  background: #f5f7fb;
}

.mock-business-screen > aside {
  display: grid;
  align-content: start;
  gap: 5px;
  border-right: 1px solid #e1e5ed;
  background: #fff;
  padding: 19px 12px;
}

.mock-business-screen > aside strong {
  margin: 0 8px 13px;
  font-size: 11px;
}

.mock-business-screen > aside span {
  border-radius: 7px;
  padding: 9px;
  color: #7c8799;
  font-size: 10px;
}

.mock-business-screen > aside .active {
  color: #5c4dd4;
  background: #f1efff;
  font-weight: 800;
}

.mock-business-screen > section {
  min-width: 0;
  padding: 19px;
}

.mock-breadcrumb {
  color: #919bac;
  font-size: 9px;
}

.mock-title {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin: 17px 0;
}

.mock-title small {
  color: #8a94a5;
  font-size: 9px;
}

.mock-title h2 {
  margin: 4px 0 0;
  font-size: 20px;
}

.mock-title > span {
  border-radius: 999px;
  padding: 5px 9px;
  color: #9b651a;
  background: #fff1dd;
  font-size: 9px;
  font-weight: 800;
}

.mock-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  overflow: hidden;
  border: 1px solid #e0e4ec;
  border-radius: 10px;
  background: #fff;
}

.mock-form label {
  gap: 7px;
  border-right: 1px solid #e9ecf2;
  border-bottom: 1px solid #e9ecf2;
  padding: 14px;
}

.mock-form label:nth-child(2n) {
  border-right: 0;
}

.mock-form span {
  color: #8a94a5;
  font-size: 9px;
}

.mock-form strong {
  font-size: 11px;
}

.mock-form .wide {
  grid-column: 1 / -1;
  border-right: 0;
}

.mock-action-zone {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 25px;
  border: 2px dashed #968aff;
  border-radius: 11px;
  padding: 14px;
  background: #f5f3ff;
  box-shadow: 0 0 0 5px rgb(109 93 252 / 5%);
}

.mock-action-zone > span {
  color: #7264e6;
  font-size: 9px;
  font-weight: 900;
}

.mock-action-zone button {
  min-height: 32px;
  color: #fff;
  background: #2e75f0;
  font-size: 10px;
}

.mock-action-zone code {
  overflow: hidden;
  margin-left: auto;
  color: #7a70c9;
  font-size: 9px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.playback-bar {
  display: grid;
  grid-template-columns: auto minmax(80px, 1fr) auto;
  align-items: center;
  gap: 10px;
  min-height: 52px;
  border-top: 1px solid #e4e7ee;
  padding: 0 13px;
  background: #fff;
  color: #7a8597;
  font-size: 9px;
}

.playback-bar > div {
  height: 4px;
  overflow: hidden;
  border-radius: 20px;
  background: #e9e7fa;
}

.explanation-panel {
  display: grid;
  grid-template-rows: auto auto 1fr auto;
  align-content: start;
}

.explanation-heading {
  display: grid;
  gap: 5px;
  border-bottom: 1px solid #e9ecf2;
  padding: 20px;
}

.explanation-heading > span {
  color: #6c5ce7;
  font-size: 9px;
  font-weight: 900;
}

.explanation-heading strong {
  font-size: 17px;
}

.explanation-heading small {
  color: #8a94a5;
  font-size: 10px;
}

.instruction {
  margin: 16px;
  border: 1px solid #dfdbff;
  border-radius: 11px;
  padding: 14px;
  background: #f7f5ff;
}

.explanation-panel :deep(.attachment-panel) {
  margin: 0 16px 14px;
}

.stage-introduction-heading {
  background: linear-gradient(135deg, #f7f5ff, #fff);
}

.stage-introduction {
  border-color: #cfc8ff;
  background: linear-gradient(145deg, #f3f0ff, #fbfaff);
}

.instruction span {
  color: #6555df;
  font-size: 9px;
  font-weight: 900;
}

.instruction p {
  margin: 7px 0 0;
  color: #566277;
  font-size: 12px;
  line-height: 1.75;
}

.explanation-panel dl {
  display: grid;
  align-content: start;
  gap: 0;
  margin: 0;
  padding: 0 18px 18px;
}

.explanation-panel dl > div {
  display: grid;
  grid-template-columns: 105px minmax(0, 1fr);
  gap: 8px;
  border-bottom: 1px solid #edf0f5;
  padding: 11px 0;
}

.explanation-panel dt {
  color: #8b95a7;
  font-size: 9px;
}

.explanation-panel dd {
  min-width: 0;
  margin: 0;
  color: #4c596e;
  font-size: 10px;
  overflow-wrap: anywhere;
}

.step-controls {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  border-top: 1px solid #e8ebf1;
  padding: 14px;
}

.segment-timeline {
  overflow: hidden;
}

.teaching-point-list {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  border-bottom: 1px solid #eceef4;
  padding: 10px 13px 8px;
}

.teaching-point-list button {
  display: grid;
  grid-template-columns: auto minmax(90px, 1fr);
  flex: 0 0 180px;
  align-items: center;
  gap: 2px 7px;
  border-color: #e2e5ec;
  padding: 7px 9px;
  text-align: left;
}

.teaching-point-list button.active {
  border-color: #9185f8;
  background: #f4f2ff;
}

.teaching-point-list span {
  display: grid;
  grid-row: 1 / 3;
  width: 23px;
  height: 23px;
  place-items: center;
  border-radius: 7px;
  color: #6656dd;
  background: #ece9ff;
  font-size: 9px;
}

.teaching-point-list strong {
  overflow: hidden;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.teaching-point-list small {
  color: #8a94a6;
  font-size: 8px;
}

.segment-list {
  display: flex;
  gap: 9px;
  overflow-x: auto;
  padding: 13px;
}

.segment-list button {
  display: grid;
  grid-template-columns: 25px minmax(130px, 1fr);
  flex: 0 0 220px;
  align-items: center;
  gap: 2px 8px;
  border-color: #e2e5ec;
  padding: 9px;
  text-align: left;
}

.segment-list button.active {
  border-color: #9185f8;
  background: #f4f2ff;
}

.segment-list button > span {
  display: grid;
  grid-row: 1 / 3;
  width: 25px;
  height: 25px;
  place-items: center;
  border-radius: 8px;
  color: #6656dd;
  background: #ece9ff;
  font-size: 9px;
}

.segment-list strong {
  overflow: hidden;
  font-size: 10px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.segment-list small {
  color: #8a94a6;
  font-size: 8px;
}

.empty-recording {
  min-height: 440px;
}

.empty-action {
  margin-top: 18px;
}

@media (max-width: 1050px) {
  .preview-layout {
    grid-template-columns: 1fr;
  }

  .explanation-panel {
    grid-template-columns: 1fr 1fr;
    grid-template-rows: auto auto;
  }

  .step-controls {
    align-self: end;
  }
}

@media (max-width: 720px) {
  .preview-header {
    align-items: stretch;
    flex-direction: column;
  }

  .preview-header__actions {
    justify-content: space-between;
  }

  .preview-metrics {
    grid-template-columns: 1fr 1fr;
  }

  .mock-business-screen {
    grid-template-columns: 1fr;
  }

  .mock-business-screen > aside {
    display: none;
  }

  .explanation-panel {
    display: block;
  }
}

@media (max-width: 520px) {
  .preview-metrics,
  .mock-form {
    grid-template-columns: 1fr;
  }

  .mock-form label,
  .mock-form label:nth-child(2n) {
    border-right: 0;
  }

  .mock-action-zone {
    align-items: flex-start;
    flex-direction: column;
  }

  .mock-action-zone code {
    width: 100%;
    margin-left: 0;
  }
}

/* 沉浸式讲解：业务界面铺满整个视口，讲解控件悬浮在业务界面上层。 */
.preview-page {
  position: relative;
  width: 100%;
  max-width: none;
  height: 100vh;
  overflow: hidden;
  background: #eef1f6;
}

.preview-layout {
  position: absolute;
  z-index: 1;
  inset: 0;
  display: block;
  min-height: 0;
}

.business-stage {
  position: absolute;
  inset: 0;
  height: 100vh;
  border: 0;
  border-radius: 0;
  box-shadow: none;
}

.mock-business-screen {
  min-height: 0;
}

.preview-header,
.preview-metrics,
.segment-timeline,
.explanation-panel {
  position: absolute;
  z-index: 10;
  border: 1px solid rgb(218 223 234 / 88%);
  background: rgb(255 255 255 / 94%);
  box-shadow: 0 18px 46px rgb(23 29 55 / 18%);
  backdrop-filter: blur(14px);
}

.preview-header {
  top: 14px;
  left: 14px;
  width: min(620px, calc(100vw - 28px));
  align-items: center;
  border-radius: 14px;
  padding: 12px 14px;
}

.preview-header h1 {
  font-size: 17px;
}

.preview-header p {
  max-width: 460px;
  margin-top: 4px;
  font-size: 9px;
}

.preview-metrics {
  top: 92px;
  left: 14px;
  width: min(430px, calc(100vw - 28px));
}

.preview-metrics > span {
  padding: 9px 12px 12px;
}

.preview-metrics strong {
  font-size: 13px;
}

.segment-timeline {
  right: 14px;
  bottom: 66px;
  left: 14px;
  border-radius: 13px;
}

.segment-timeline .card-header {
  display: none;
}

.segment-list {
  padding: 9px;
}

.segment-list button {
  flex-basis: 190px;
}

.explanation-panel {
  z-index: 30;
  width: min(430px, calc(100vw - 36px));
  max-height: calc(100vh - 36px);
  overflow-y: auto;
  border-radius: 16px;
}

.lecture-step-prompt.stage-prompt {
  top: 50%;
  right: auto;
  bottom: auto;
  left: 50%;
  width: min(500px, calc(100vw - 36px));
  transform: translate(-50%, -50%);
}

.lecture-step-prompt.node-prompt {
  top: 50%;
  right: auto;
  bottom: auto;
  left: 18px;
  transform: translateY(-50%);
}

.lecture-feedback {
  margin: 0 14px 10px;
  border-radius: 9px;
  padding: 10px;
  font-size: 9px;
  line-height: 1.5;
}

.lecture-feedback.success {
  color: #087b59;
  background: #eaf8f2;
}

.lecture-feedback.danger {
  color: #a43d48;
  background: #fff0f1;
}

.publish-link {
  display: block;
  margin: 0 14px 14px;
  color: #6253d5;
  font-size: 10px;
  font-weight: 800;
  text-align: center;
}

@media (max-height: 720px) {
  .preview-metrics {
    display: none;
  }

  .segment-timeline {
    bottom: 58px;
  }

  .teaching-point-list {
    display: none;
  }

  .explanation-heading {
    padding: 14px;
  }

  .instruction {
    margin: 10px 14px;
    padding: 10px;
  }

  .explanation-panel dl > div {
    padding: 7px 0;
  }
}

@media (max-height: 560px) {
  .preview-header p,
  .segment-timeline,
  .explanation-panel dl,
  .explanation-panel :deep(.attachment-panel) {
    display: none;
  }

  .explanation-panel {
    max-height: calc(100vh - 24px);
  }
}

@media (max-width: 880px) {
  .lecture-overlay-toggle {
    top: 10px;
    right: 10px;
  }

  .preview-header {
    right: 12px;
    left: 12px;
    width: auto;
  }

  .preview-header p,
  .preview-metrics,
  .segment-timeline {
    display: none;
  }

  .explanation-panel {
    top: auto;
    right: 12px;
    bottom: 12px;
    left: 12px;
    width: auto;
    max-height: 46vh;
    transform: none;
  }

  .explanation-panel dl,
  .instruction {
    display: none;
  }
}
</style>
