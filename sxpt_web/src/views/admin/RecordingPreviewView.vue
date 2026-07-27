<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
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
const currentIndex = ref(0);

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
}

function move(direction: -1 | 1) {
  selectStep(currentIndex.value + direction);
}
</script>

<template>
  <div v-if="lesson" class="page preview-page">
    <header class="preview-header">
      <div>
        <span class="eyebrow">RECORDING PREVIEW</span>
        <h1>{{ lesson.title }}</h1>
        <p>演示模式仅回放已录制页面与讲解，不连接业务系统，也不会消费真实待办数据。</p>
      </div>
      <div class="preview-header__actions">
        <RouterLink
          class="button secondary"
          :to="{ name: 'lesson-editor', params: { lessonId: lesson.id } }"
        >
          ← 返回编辑
        </RouterLink>
        <span class="demo-badge">● 安全演示</span>
      </div>
    </header>

    <section class="preview-metrics">
      <span><small>业务阶段</small><strong>{{ lesson.stages.length }}</strong></span>
      <span><small>录制片段</small><strong>{{ previewSteps.length }}</strong></span>
      <span><small>总时长</small><strong>{{ formatDuration(totalDuration) }}</strong></span>
      <span><small>当前进度</small><strong>{{ progress }}%</strong></span>
      <div class="preview-progress"><i :style="{ width: `${progress}%` }"></i></div>
    </section>

    <div v-if="current && currentStep" class="preview-layout">
      <main class="business-stage">
        <div class="browser-chrome">
          <div class="browser-dots"><i></i><i></i><i></i></div>
          <div class="browser-address">training-preview.local / {{ currentStep.selector }}</div>
          <span>只读</span>
        </div>

        <div class="business-toolbar">
          <div>
            <span>模拟业务系统</span>
            <strong>{{ currentStep.pageTitle }}</strong>
          </div>
          <div class="mock-user">
            <span>{{ current.stage.groupKey.slice(0, 1).toUpperCase() || '角' }}</span>
            {{ current.stage.groupKey || '业务角色' }}
          </div>
        </div>

        <div class="mock-business-screen">
          <aside>
            <strong>业务工作台</strong>
            <span>我的待办</span>
            <span class="active">流程办理</span>
            <span>已办事项</span>
            <span>业务查询</span>
          </aside>
          <section>
            <div class="mock-breadcrumb">业务办理 / {{ current.stage.name }} / 当前录制步骤</div>
            <div class="mock-title">
              <div>
                <small>模拟单号 MOCK-2026-0718</small>
                <h2>{{ currentStep.title }}</h2>
              </div>
              <span>演示数据</span>
            </div>
            <div class="mock-form">
              <label><span>申请单位</span><strong>第一事业部</strong></label>
              <label><span>业务类型</span><strong>{{ lesson.moduleName }}</strong></label>
              <label><span>当前环节</span><strong>{{ current.stage.name }}</strong></label>
              <label><span>办理角色</span><strong>{{ current.stage.groupKey }}</strong></label>
              <label class="wide">
                <span>事项说明</span>
                <strong>{{ current.stage.description || '按录制步骤完成本环节业务办理。' }}</strong>
              </label>
            </div>
            <div class="mock-action-zone">
              <span>录制焦点</span>
              <button type="button">{{ currentStep.actionLabel }}</button>
              <code>{{ currentStep.selector }}</code>
            </div>
          </section>
        </div>

        <div class="playback-bar">
          <button
            class="icon-button"
            type="button"
            :disabled="currentIndex === 0"
            aria-label="上一步"
            @click="move(-1)"
          >
            ‹
          </button>
          <span>{{ formatDuration(elapsedDuration) }}</span>
          <div><i :style="{ width: `${progress}%` }"></i></div>
          <span>{{ formatDuration(totalDuration) }}</span>
          <button
            class="icon-button"
            type="button"
            :disabled="currentIndex === previewSteps.length - 1"
            aria-label="下一步"
            @click="move(1)"
          >
            ›
          </button>
        </div>
      </main>

      <aside class="explanation-panel">
        <div class="explanation-heading">
          <span>步骤 {{ currentIndex + 1 }} / {{ previewSteps.length }}</span>
          <strong>{{ currentStep.title }}</strong>
          <small>{{ current.stage.name }} · {{ current.stage.groupKey }}</small>
        </div>
        <div class="instruction">
          <span>逐步讲解</span>
          <p>{{ currentStep.note || '请观察页面变化，并理解该动作在业务流程中的作用。' }}</p>
        </div>
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
        <div class="step-controls">
          <button type="button" :disabled="currentIndex === 0" @click="move(-1)">← 上一步</button>
          <button
            class="primary"
            type="button"
            :disabled="currentIndex === previewSteps.length - 1"
            @click="move(1)"
          >
            下一步 →
          </button>
        </div>
      </aside>
    </div>

    <section v-else class="card empty-recording">
      <div class="empty-state">
        <div>
          <strong>暂无可回看的录制步骤</strong><br />
          请返回教案编排，为任意业务阶段添加录制步骤。
          <div class="empty-action">
            <RouterLink
              class="button primary"
              :to="{ name: 'lesson-editor', params: { lessonId: lesson.id } }"
            >
              返回编辑
            </RouterLink>
          </div>
        </div>
      </div>
    </section>

    <section v-if="previewSteps.length" class="card segment-timeline">
      <div class="card-header">
        <div>
          <h2>录制片段时间线</h2>
          <p>阶段与角色由教案动态生成；点击任意片段可直接定位。</p>
        </div>
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
  grid-template-rows: auto auto 1fr auto;
  min-width: 0;
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
  grid-template-columns: auto auto minmax(80px, 1fr) auto auto;
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
</style>
