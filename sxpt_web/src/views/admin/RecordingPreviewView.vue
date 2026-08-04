<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import LessonPlaybackPlayer from '../../components/lesson/LessonPlaybackPlayer.vue';
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

watch(previewSteps, (steps) => {
  if (currentIndex.value >= steps.length) {
    currentIndex.value = Math.max(0, steps.length - 1);
  }
});

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
  <LessonPlaybackPlayer
    v-if="lesson"
    :lesson="lesson"
    :current-index="currentIndex"
    :show-stage-introduction="showStageIntroduction"
    :return-to="returnRoute"
    :return-label="openedFromPublishCenter ? '返回发布中心' : '返回编辑'"
    finish-label="完成教师讲解"
    :action-disabled="Boolean(lesson.lectureCompletedAt)"
    :feedback="lectureFeedback"
    :feedback-tone="lectureFeedbackSuccess ? 'success' : 'danger'"
    @previous="move(-1)"
    @next="move(1)"
    @finish="finishLecture"
    @select-step="selectStep"
    @select-stage="selectTeachingPoint"
  />

  <section v-else class="standalone-state">
    <span>404</span>
    <h1>教案不存在</h1>
    <p>无法加载本次录制回看。</p>
    <RouterLink class="button primary" :to="{ name: 'lesson-list' }">返回教案列表</RouterLink>
  </section>
</template>

<style scoped>
.standalone-state {
  display: grid;
  min-height: 70vh;
  place-items: center;
  align-content: center;
  gap: 10px;
  text-align: center;
}

.standalone-state > span {
  color: #695bc7;
  font-size: 48px;
  font-weight: 900;
}

.standalone-state h1,
.standalone-state p {
  margin: 0;
}
</style>
