<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import PageHeader from '../../components/ui/PageHeader.vue';
import type { PublishedTask, TaskDataPrepareBinding } from '../../domain/models';
import {
  authApi,
  dataPrepareApi,
  type ClassicCaseAsset
} from '../../services/trainingApi';
import { useTrainingStore } from '../../stores/trainingStore';
import { summarizeLessonRelease } from '../../utils/publishCenterPresentation';

const route = useRoute();
const router = useRouter();
const store = useTrainingStore();
const lessonId = String(route.params.lessonId);
const lesson = computed(() => store.getLesson(lessonId));
const feedback = ref('');
const feedbackSuccess = ref(false);
const publishing = ref(false);
const classicCases = ref<ClassicCaseAsset[]>([]);
const classicCaseLoading = ref(false);
const classicCaseError = ref('');
const session = authApi.getSession();

const learningTask = computed(() =>
  store.state.publishedTasks.find(
    (task) => task.lessonId === lessonId && task.mode === 'LEARNING'
  )
);
const practiceTask = computed(() =>
  store.state.publishedTasks.find(
    (task) => task.lessonId === lessonId && task.mode === 'PRACTICE'
  )
);
const lectureCompleted = computed(() => Boolean(lesson.value?.lectureCompletedAt));
const taskReady = (task?: PublishedTask) =>
  Boolean(task && task.syncStatus !== 'FAILED' && task.syncStatus !== 'SYNCING');
const trainingTasksReady = computed(
  () => taskReady(learningTask.value) && taskReady(practiceTask.value)
);
const releaseSummary = computed(() =>
  summarizeLessonRelease({
    stages: lesson.value?.stages ?? [],
    lectureCompleted: lectureCompleted.value,
    learningTask: learningTask.value,
    practiceTask: practiceTask.value
  })
);
const assignedCount = computed(() => releaseSummary.value.assignedCount);
const lockedClassicCaseConfig = computed(() => lesson.value?.classicCaseConfig);
const dataPrepareBinding = computed<TaskDataPrepareBinding>(() => {
  const config = lockedClassicCaseConfig.value;
  if (lesson.value?.generationSource !== 'CLASSIC_CASE' || !config) {
    return { dataPrepareMode: 'NORMAL' };
  }
  return {
    dataPrepareMode: 'CLASSIC_CASE',
    classicCaseAssetId: config.classicCaseId,
    classicCaseVersionId: config.caseVersionId,
    classicCaseCode: config.caseCode,
    classicCaseTitle: config.caseName,
    generationMode: config.generationMode
  };
});
const dataPrepareModeLabel = computed(() =>
  lockedClassicCaseConfig.value
    ? `经典案例：${lockedClassicCaseConfig.value.caseName} · ${lockedClassicCaseConfig.value.caseVersionId}`
    : '普通造数'
);

async function loadClassicCases() {
  classicCaseLoading.value = true;
  classicCaseError.value = '';
  try {
    classicCases.value = await dataPrepareApi.listClassicCases({
      tenantId: session?.user.tenantId || '',
      learningConnectorSystemId: lesson.value?.businessPlatformId || undefined,
      moduleCode: store.getBusinessPlatformModule(
        lesson.value?.businessPlatformId || '',
        lesson.value?.businessPlatformModuleId || ''
      )?.code
    });
    if (
      lockedClassicCaseConfig.value &&
      !classicCases.value.some(
        (item) =>
          item.id === lockedClassicCaseConfig.value?.classicCaseId &&
          ['AVAILABLE', 'ACTIVE'].includes(item.status ?? '')
      )
    ) {
      classicCaseError.value = '教案锁定的经典案例当前已停用或不可用；已发布任务仍保留锁定版本。';
    }
  } catch (error) {
    classicCaseError.value =
      error instanceof Error ? error.message : '经典案例加载失败';
  } finally {
    classicCaseLoading.value = false;
  }
}

function startLecture() {
  void router.push({
    name: 'lesson-recording',
    params: { lessonId },
    query: { from: 'publish' }
  });
}

function syncLabel(task?: PublishedTask) {
  if (!task) return '未发布';
  if (task.syncStatus === 'SYNCING') return '发布中';
  if (task.syncStatus === 'FAILED') return `发布失败${task.syncError ? ` · ${task.syncError}` : ''}`;
  return task.syncStatus === 'SYNCED' ? '已同步后端' : '已发布';
}

async function publishTrainingTasks() {
  feedback.value = '';
  feedbackSuccess.value = false;
  if (!lectureCompleted.value) {
    feedback.value = '请先完成教师讲解，再发布学生学习与练习。';
    return;
  }
  publishing.value = true;
  try {
    const [, practice] = await store.publishLearningAndPracticeRemote(
      lessonId,
      dataPrepareBinding.value
    );
    feedbackSuccess.value = true;
    feedback.value =
      practice.dataPrepareMode === 'CLASSIC_CASE'
        ? `学习与练习已发布，为 ${practice.assignedCount} 名学生生成任务；练习数据将在学生进入原平台时按经典案例即时生成。`
        : `学习与练习已发布，为 ${practice.assignedCount} 名学生生成任务，并准备 ${practice.dataCount} 条原平台练习数据。`;
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '学习、练习任务发布失败';
  } finally {
    publishing.value = false;
  }
}

onMounted(() => {
  void loadClassicCases();
});
</script>

<template>
  <main class="page publish-center-page">
    <PageHeader
      eyebrow="LESSON RELEASE"
      title="发布中心"
      description="完成一次教师讲解后，发布学生学习与练习；练习发布会自动调用批次准备造数链路。"
    >
      <button class="secondary" type="button" @click="router.push({ name: 'lesson-list' })">
        返回教案管理
      </button>
    </PageHeader>

    <section v-if="!lesson" class="card publish-empty">
      <h2>教案不存在或已删除</h2>
      <button class="primary" type="button" @click="router.push({ name: 'lesson-list' })">
        返回教案管理
      </button>
    </section>

    <template v-else>
      <section class="release-overview">
        <div class="release-overview__copy">
          <span class="eyebrow">LESSON RELEASE</span>
          <h1>{{ lesson.title }}</h1>
          <p>
            {{ lesson.moduleName || '未指定业务模块' }} ·
            {{ releaseSummary.stageCount }} 个教学点
          </p>
        </div>
        <strong class="release-overview__status">
          {{ lesson.status === 'PUBLISHED' ? '教案已发布' : lesson.status }}
        </strong>
      </section>

      <section class="release-metrics" aria-label="教案发布概览">
        <article>
          <span>教学点</span>
          <strong>{{ releaseSummary.stageCount }}</strong>
        </article>
        <article>
          <span>录制节点</span>
          <strong>{{ releaseSummary.recordedStepCount }}</strong>
        </article>
        <article>
          <span>教师讲解状态</span>
          <strong>{{ releaseSummary.lectureCompleted ? '已完成' : '待完成' }}</strong>
        </article>
        <article>
          <span>任务覆盖学生</span>
          <strong>{{ releaseSummary.assignedCount }}</strong>
        </article>
        <article>
          <span>练习数据</span>
          <strong>{{ releaseSummary.preparedDataCount }}</strong>
        </article>
      </section>

      <section class="release-data-source">
        <div>
          <span class="eyebrow">DATA SOURCE</span>
          <h2>数据来源</h2>
          <p>
            当前：{{ dataPrepareModeLabel }}。普通造数会在发布练习时批量准备数据；
            经典案例会按教案锁定版本在学生进入原平台时生成数据。
          </p>
        </div>
        <label>
          <span>教案锁定的数据配置</span>
          <input :value="dataPrepareModeLabel" disabled />
          <small v-if="lockedClassicCaseConfig">
            {{ lockedClassicCaseConfig.generationMode === 'REPLAY_CASE' ? '复刻脱敏案例' : '按格式生成 Demo' }}
          </small>
        </label>
        <button class="secondary" type="button" :disabled="classicCaseLoading" @click="loadClassicCases">
          {{ classicCaseLoading ? '正在刷新' : '刷新案例' }}
        </button>
        <small v-if="classicCaseError" class="source-error">{{ classicCaseError }}</small>
      </section>

      <div v-if="feedback" class="notice" :class="feedbackSuccess ? 'success' : 'danger'">
        {{ feedback }}
      </div>

      <section class="release-rail">
        <article class="release-step" :class="{ completed: lectureCompleted }">
          <span class="release-step__number">01</span>
          <div class="release-step__body">
            <span class="eyebrow">TEACHER LECTURE</span>
            <h2>教师讲解</h2>
            <p>教师完整讲解教案流程。学习与练习发布只校验本步骤是否完成。</p>
            <strong>{{ lectureCompleted ? '教师讲解已完成' : '尚未完成教师讲解' }}</strong>
          </div>
          <button class="secondary" type="button" @click="startLecture">
            {{ lectureCompleted ? '重新讲解' : '开始讲解' }}
          </button>
        </article>

        <span class="release-rail__connector" aria-hidden="true">→</span>

        <article class="release-step" :class="{ completed: trainingTasksReady }">
          <span class="release-step__number">02</span>
          <div class="release-step__body">
            <span class="eyebrow">STUDENT LEARNING &amp; PRACTICE</span>
            <h2>发布学生学习与练习</h2>
            <p>同步两类学生任务，并在练习任务发布时调用批次准备的数据生成操作。</p>
            <div class="release-modes">
              <div>
                <span>LEARNING</span>
                <strong>学生学习</strong>
                <small>{{ syncLabel(learningTask) }}</small>
              </div>
              <div>
                <span>PRACTICE</span>
                <strong>学生练习</strong>
                <small>{{ syncLabel(practiceTask) }}</small>
              </div>
            </div>
          </div>
          <button
            class="primary"
            type="button"
            :disabled="!lectureCompleted || publishing || trainingTasksReady"
            @click="publishTrainingTasks"
          >
            {{
              publishing
                ? '正在发布并生成数据…'
                : trainingTasksReady
                  ? '学习与练习已发布'
                  : '发布学习与练习'
            }}
          </button>
        </article>
      </section>

      <section v-if="!lectureCompleted" class="lecture-gate">
        <strong>当前唯一发布条件：完成教师讲解</strong>
        <p>不再校验考试设置、分组覆盖、考试数据或五步发布清单。</p>
      </section>

      <section v-if="trainingTasksReady" class="card release-result">
        <div>
          <span class="eyebrow">PUBLISHED</span>
          <h2>学生学习与练习已就绪</h2>
          <p>
            已覆盖 {{ assignedCount }} 名学生；练习任务已准备
            {{ practiceTask?.dataCount ?? 0 }} 条原平台数据。
          </p>
        </div>
        <button class="primary" type="button" @click="router.push({ name: 'student-tasks' })">
          查看学生任务
        </button>
      </section>
    </template>
  </main>
</template>

<style scoped>
.publish-center-page {
  display: grid;
  gap: 18px;
}

.release-overview {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  overflow: hidden;
  border: 1px solid rgb(255 255 255 / 28%);
  border-radius: 22px;
  padding: 26px 30px;
  color: #fff;
  background:
    radial-gradient(circle at 84% 18%, rgb(119 226 201 / 34%), transparent 27%),
    linear-gradient(132deg, #34305f 0%, #5d4fc1 54%, #398b88 118%);
  box-shadow: 0 20px 48px rgb(45 40 105 / 18%);
}

.release-overview__copy {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.release-overview .eyebrow {
  color: #d9d3ff;
}

.release-overview h1,
.release-overview p {
  margin: 0;
}

.release-overview h1 {
  overflow: hidden;
  font-size: clamp(24px, 3vw, 36px);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.release-overview p {
  color: rgb(255 255 255 / 74%);
}

.release-overview__status {
  flex: 0 0 auto;
  border: 1px solid rgb(255 255 255 / 26%);
  border-radius: 999px;
  padding: 9px 14px;
  color: #fff;
  background: rgb(255 255 255 / 13%);
  backdrop-filter: blur(10px);
}

.release-metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.release-metrics article {
  display: grid;
  gap: 8px;
  min-width: 0;
  border: 1px solid #e5e7ef;
  border-radius: 14px;
  padding: 15px 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgb(41 47 75 / 6%);
}

.release-metrics span {
  color: var(--muted);
  font-size: 12px;
}

.release-metrics strong {
  overflow: hidden;
  color: #343957;
  font-size: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.release-data-source {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(240px, 360px) auto;
  align-items: end;
  gap: 14px;
  border: 1px solid #dfe6f2;
  border-radius: 14px;
  padding: 16px;
  background: #fff;
  box-shadow: 0 8px 24px rgb(41 47 75 / 6%);
}

.release-data-source h2,
.release-data-source p {
  margin: 0;
}

.release-data-source p,
.release-data-source label span,
.source-error {
  color: var(--muted);
}

.release-data-source label {
  display: grid;
  gap: 6px;
  min-width: 0;
  font-size: 12px;
}

.release-data-source select {
  width: 100%;
  min-height: 40px;
  border: 1px solid #d9deea;
  border-radius: 10px;
  padding: 0 12px;
  background: #fff;
  color: #31364f;
}

.source-error {
  grid-column: 1 / -1;
}

.release-rail {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 34px minmax(0, 1.25fr);
  align-items: stretch;
  gap: 0;
}

.release-step {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  align-content: start;
  gap: 16px;
  border: 1px solid #e1e5ee;
  border-radius: 18px;
  padding: 20px;
  background: #fff;
  box-shadow: 0 12px 32px rgb(39 45 76 / 7%);
}

.release-step.completed {
  border-color: #b9dfcc;
  background: linear-gradient(135deg, #fff, #f5fcf8);
}

.release-step > button {
  grid-column: 1 / -1;
  align-self: end;
  justify-self: end;
  margin-top: auto;
}

.release-rail__connector {
  display: grid;
  align-self: center;
  width: 30px;
  height: 30px;
  margin: 2px;
  place-items: center;
  border-radius: 50%;
  color: #6255c8;
  background: #ebe8ff;
  font-weight: 900;
}

.release-step__number {
  display: grid;
  width: 46px;
  height: 46px;
  place-items: center;
  border-radius: 14px;
  color: #5b4fd0;
  background: #efedff;
  font-weight: 900;
}

.release-step__body {
  display: grid;
  gap: 7px;
}

.release-step__body h2,
.release-step__body p {
  margin: 0;
}

.release-step__body p,
.release-modes small,
.release-result p,
.lecture-gate p {
  color: var(--muted);
}

.release-modes {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 5px;
}

.release-modes > div {
  display: grid;
  gap: 3px;
  border: 1px solid #e4e7ef;
  border-radius: 10px;
  padding: 10px 12px;
  background: #fafbfe;
}

.release-modes span {
  color: #7468d8;
  font-size: 9px;
  font-weight: 900;
  letter-spacing: 0.08em;
}

.lecture-gate,
.release-result,
.publish-empty {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border: 1px solid #f0d5a7;
  border-radius: 12px;
  padding: 15px 18px;
  background: #fffbf2;
}

.lecture-gate p,
.release-result h2,
.release-result p {
  margin: 4px 0 0;
}

@media (max-width: 1000px) {
  .release-metrics {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .release-rail {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .release-data-source {
    grid-template-columns: 1fr;
  }

  .release-rail__connector {
    justify-self: center;
    transform: rotate(90deg);
  }
}

@media (max-width: 640px) {
  .release-overview {
    align-items: flex-start;
    padding: 18px;
  }

  .release-overview__status {
    font-size: 12px;
  }

  .release-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .release-step {
    grid-template-columns: 1fr;
  }

  .release-step__number {
    width: 38px;
    height: 38px;
    border-radius: 11px;
  }

  .release-modes {
    grid-template-columns: 1fr;
  }

  .lecture-gate,
  .release-result,
  .publish-empty {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
