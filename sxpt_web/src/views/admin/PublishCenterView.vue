<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import PageHeader from '../../components/ui/PageHeader.vue';
import type { PublishedTask } from '../../domain/models';
import { useTrainingStore } from '../../stores/trainingStore';

const route = useRoute();
const router = useRouter();
const store = useTrainingStore();
const lessonId = String(route.params.lessonId);
const lesson = computed(() => store.getLesson(lessonId));
const feedback = ref('');
const feedbackSuccess = ref(false);
const publishing = ref(false);

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
const assignedCount = computed(
  () => Math.max(learningTask.value?.assignedCount ?? 0, practiceTask.value?.assignedCount ?? 0)
);

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
    const [, practice] = await store.publishLearningAndPracticeRemote(lessonId);
    feedbackSuccess.value = true;
    feedback.value = `学习与练习已发布，为 ${practice.assignedCount} 名学生生成任务，并准备 ${practice.dataCount} 条原平台练习数据。`;
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '学习、练习任务发布失败';
  } finally {
    publishing.value = false;
  }
}
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
      <div v-if="feedback" class="notice" :class="feedbackSuccess ? 'success' : 'danger'">
        {{ feedback }}
      </div>

      <section class="release-flow">
        <article class="card release-step" :class="{ completed: lectureCompleted }">
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

        <article class="card release-step" :class="{ completed: trainingTasksReady }">
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

.release-flow {
  display: grid;
  gap: 14px;
}

.release-step {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 18px;
  border-color: #e1e5ee;
}

.release-step.completed {
  border-color: #b9dfcc;
  background: linear-gradient(135deg, #fff, #f5fcf8);
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

@media (max-width: 760px) {
  .release-step {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .release-step > button {
    grid-column: 1 / -1;
  }

  .release-modes {
    grid-template-columns: 1fr;
  }
}
</style>
