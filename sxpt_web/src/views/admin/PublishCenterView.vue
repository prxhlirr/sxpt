<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import PageHeader from '../../components/ui/PageHeader.vue';
import StatusPill from '../../components/ui/StatusPill.vue';
import WorkflowStepper from '../../components/exam/WorkflowStepper.vue';
import {
  buildPublishChecklist,
  summarizeExamData,
  validateExamDraft
} from '../../components/exam/workflow';
import type { PublishedTask } from '../../domain/models';
import { useTrainingStore } from '../../stores/trainingStore';

const route = useRoute();
const router = useRouter();
const store = useTrainingStore();
const lessonId = String(route.params.lessonId);
const lesson = computed(() => store.getLesson(lessonId));
const examSettings = computed(() => store.state.examSettings[lessonId]);
const groupPlan = computed(() => store.state.groupPlans[lessonId]);
const dataItems = computed(() => store.state.dataItems[lessonId] ?? []);
const generatedTask = ref<PublishedTask>();
const feedback = ref('');
const publishing = ref(false);
const trainingPublishing = ref(false);
const feedbackSuccess = ref(false);

const existingTask = computed(() =>
  [...store.state.publishedTasks]
    .find((task) => task.lessonId === lessonId && task.mode === 'EXAM')
);
const activeTask = computed(() => generatedTask.value ?? existingTask.value);
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
const trainingTasksReady = computed(
  () =>
    learningTask.value?.syncStatus === 'SYNCED' &&
    practiceTask.value?.syncStatus === 'SYNCED'
);
const lessonPublished = computed(() => lesson.value?.status === 'PUBLISHED');
const examConfigured = computed(
  () =>
    Boolean(examSettings.value) &&
    validateExamDraft(examSettings.value).length === 0
);
const groupValidation = computed(() => {
  const plan = groupPlan.value;
  if (!plan?.roles.length || !plan.members.length) return false;
  const lessonStageIds = lesson.value?.stages.map((stage) => stage.id) ?? [];
  const mappedStageIds = plan.roles.flatMap((role) => role.stageIds);
  const covered = new Set(mappedStageIds);
  if (
    lessonStageIds.some((id) => !covered.has(id)) ||
    covered.size !== mappedStageIds.length
  ) {
    return false;
  }
  const rolesHaveMembers = plan.roles.every(
    (role) =>
      plan.members.some((member) => member.groupKey === role.key) &&
      plan.members
        .filter((member) => member.groupKey === role.key)
        .every((member) => member.accountId.trim())
  );
  if (!rolesHaveMembers) return false;
  const unitIds = [...new Set(plan.members.map((member) => member.unitId))];
  return unitIds.every((unitId) =>
    plan.roles.every((role) =>
      plan.members.some(
        (member) => member.unitId === unitId && member.groupKey === role.key
      )
    )
  );
});
const unitCoverage = computed(() => {
  const plan = groupPlan.value;
  if (!plan) return [];
  const units = new Map(
    plan.members.map((member) => [
      member.unitId,
      { unitId: member.unitId, unitName: member.unitName }
    ])
  );
  return [...units.values()].map((unit) => {
    const required = Math.max(
      1,
      ...plan.roles.map(
        (role) =>
          plan.members.filter(
            (member) =>
              member.unitId === unit.unitId && member.groupKey === role.key
          ).length
      )
    );
    const ready = dataItems.value.filter(
      (item) =>
        item.unitId === unit.unitId &&
        item.kind === 'FORMAL' &&
        item.status === 'READY'
    ).length;
    return { ...unit, required, ready, passed: ready >= required };
  });
});
const dataReady = computed(
  () =>
    unitCoverage.value.length > 0 &&
    unitCoverage.value.every((unit) => unit.passed)
);
const checklist = computed(() =>
  buildPublishChecklist({
    lessonPublished: lessonPublished.value,
    examConfigured: examConfigured.value,
    groupsValid: groupValidation.value,
    dataReady: dataReady.value,
    alreadyPublished: Boolean(activeTask.value)
  })
);
const dataSummary = computed(() => summarizeExamData(dataItems.value));
const uniqueLearners = computed(
  () =>
    new Set(groupPlan.value?.members.map((member) => member.studentId) ?? []).size
);
const multiRoleLearners = computed(() => {
  const groupsByStudent = new Map<string, Set<string>>();
  (groupPlan.value?.members ?? []).forEach((member) => {
    const groups = groupsByStudent.get(member.studentId) ?? new Set<string>();
    groups.add(member.groupKey);
    groupsByStudent.set(member.studentId, groups);
  });
  return [...groupsByStudent.values()].filter((groups) => groups.size > 1).length;
});

function goTo(routeName: string) {
  void router.push({ name: routeName, params: { lessonId } });
}

function startLecture() {
  void router.push({
    name: 'lesson-recording',
    params: { lessonId },
    query: { from: 'publish' }
  });
}

async function publishTrainingTasks() {
  feedback.value = '';
  feedbackSuccess.value = false;
  trainingPublishing.value = true;
  try {
    await store.publishLearningAndPracticeRemote(lessonId);
    feedbackSuccess.value = true;
    feedback.value =
      '学习任务和练习任务已发布到后端，分组内学生现在可以先看一遍流程，再完成练习。';
  } catch (error) {
    feedback.value =
      error instanceof Error ? error.message : '学习、练习任务发布失败';
  } finally {
    trainingPublishing.value = false;
  }
}

async function publishExam() {
  feedback.value = '';
  feedbackSuccess.value = false;
  if (!checklist.value.ready) {
    feedback.value = '发布条件尚未全部通过，请根据阻断原因返回对应步骤处理。';
    return;
  }
  publishing.value = true;
  try {
    generatedTask.value = await store.publishExamRemote(lessonId);
    feedbackSuccess.value = true;
    feedback.value = `考试任务「${generatedTask.value.title}」已发布，共生成 ${generatedTask.value.assignedCount} 个学员任务。`;
  } catch (error) {
    feedback.value = error instanceof Error ? error.message : '考试任务发布失败';
  } finally {
    publishing.value = false;
  }
}
</script>

<template>
  <main class="page exam-page">
    <PageHeader
      eyebrow="LESSON BUSINESS CHAIN · 05"
      title="发布中心"
      description="汇总教案、考试、分组和数据配置，通过五步就绪检查后一次生成学生任务。"
    >
      <button class="secondary" type="button" @click="goTo('exam-data')">上一步</button>
      <button
        v-if="!activeTask"
        class="primary publish-button"
        type="button"
        :disabled="!checklist.ready || publishing"
        @click="publishExam"
      >
        {{ publishing ? '正在发布…' : '确认发布考试' }}
      </button>
      <button v-else class="primary" type="button" @click="router.push({ name: 'teacher-dashboard' })">
        进入教师监控
      </button>
    </PageHeader>

    <section class="card training-release">
      <div class="card-header">
        <div>
          <h2>先发布学习与练习任务</h2>
          <p>可直接从发布中心进入教师讲解；讲解完成后，为真实分组学生生成流程学习和操作练习任务。</p>
        </div>
        <button
          class="primary"
          type="button"
          :disabled="trainingPublishing || trainingTasksReady"
          @click="publishTrainingTasks"
        >
          {{
            trainingPublishing
              ? '正在同步任务…'
              : trainingTasksReady
                ? '学习与练习已发布'
                : '发布学习与练习'
          }}
        </button>
      </div>
      <div class="training-release__modes">
        <div class="training-release__mode">
          <span>LEARNING</span>
          <strong>流程学习</strong>
          <small>{{ learningTask?.syncStatus ?? '未发布' }}</small>
        </div>
        <div class="training-release__mode">
          <span>PRACTICE</span>
          <strong>流程练习</strong>
          <small>{{ practiceTask?.syncStatus ?? '未发布' }}</small>
        </div>
        <button
          class="training-release__mode training-release__lecture"
          type="button"
          @click="startLecture"
        >
          <span>LECTURE</span>
          <strong>教师讲解</strong>
          <small>
            {{
              lesson?.lectureCompletedAt
                ? '已完成 · 点击重新讲解 →'
                : '待完成 · 点击开始讲解 →'
            }}
          </small>
        </button>
      </div>
    </section>

    <WorkflowStepper
      :lesson-id="lessonId"
      current="publish"
      aria-label="教案编排、考试设置、分组设置、数据生成、发布任务"
    />

    <div v-if="feedback" class="notice" :class="{ success: feedbackSuccess, danger: !feedbackSuccess }">
      {{ feedback }}
    </div>

    <section v-if="activeTask" class="publish-success">
      <span class="success-mark">✓</span>
      <div>
        <span class="eyebrow">PUBLISHED SUCCESSFULLY</span>
        <h2>考试任务已生成并推送</h2>
        <p>学员将按角色、单位和业务数据进入串行办理流程，教师可全程监控并在提交后补充主观评分。</p>
      </div>
      <div class="success-actions">
        <button class="primary" type="button" @click="router.push({ name: 'teacher-dashboard' })">
          教师监控
        </button>
        <button class="secondary" type="button" @click="router.push({ name: 'student-tasks' })">
          查看学生任务
        </button>
      </div>
    </section>

    <div class="publish-layout">
      <section class="publish-main">
        <article class="card checklist-card">
          <div class="card-header">
            <div>
              <h2>五步发布就绪检查</h2>
              <p>任何未通过项都会阻断发布，点击对应步骤可返回调整。</p>
            </div>
            <span class="readiness-score" :class="{ ready: checklist.reasons.length === 0 }">
              {{ checklist.items.filter((item) => item.passed).length }} / 5
            </span>
          </div>
          <div class="publish-checklist">
            <button
              v-for="(item, index) in checklist.items"
              :key="item.key"
              type="button"
              :class="{ passed: item.passed, current: item.key === 'task' }"
              @click="goTo(item.routeName)"
            >
              <span class="check-icon">{{ item.passed ? '✓' : index + 1 }}</span>
              <span><strong>{{ item.label }}</strong><small>{{ item.detail }}</small></span>
              <b>{{ item.passed ? '已通过' : '去处理 →' }}</b>
            </button>
          </div>
        </article>

        <article v-if="checklist.reasons.length" class="card blocking-card">
          <div class="card-header">
            <div><h2>阻断原因</h2><p>请完成以下事项后重新进入发布检查。</p></div>
            <span class="status-pill danger">{{ checklist.reasons.length }} 项</span>
          </div>
          <ul>
            <li v-for="reason in checklist.reasons" :key="reason">{{ reason }}</li>
          </ul>
        </article>

        <article class="card">
          <div class="card-header">
            <div><h2>业务链摘要</h2><p>本次发布将依据下列关系创建学员任务。</p></div>
          </div>
          <div class="business-chain">
            <div>
              <span class="chain-icon purple">教</span>
          <span><strong>{{ lesson?.title ?? '未配置教案' }}</strong><small>{{ lesson?.stages.length ?? 0 }} 个教学点 · V{{ lesson?.version ?? '-' }}</small></span>
            </div>
            <i>→</i>
            <div>
              <span class="chain-icon blue">组</span>
              <span><strong>{{ groupPlan?.roles.length ?? 0 }} 个业务角色</strong><small>{{ uniqueLearners }} 名学员 · {{ multiRoleLearners }} 人承担多角色</small></span>
            </div>
            <i>→</i>
            <div>
              <span class="chain-icon green">数</span>
              <span><strong>{{ dataSummary.readyFormal }} 条正式数据</strong><small>{{ dataSummary.readySpare }} 条备用 · 按单位分配</small></span>
            </div>
            <i>→</i>
            <div>
              <span class="chain-icon amber">考</span>
              <span><strong>{{ examSettings?.batchName ?? '考试未配置' }}</strong><small>{{ examSettings?.durationMinutes ?? 0 }} 分钟 · 最多 {{ examSettings?.allowRetry ? examSettings.maxAttempts : 1 }} 次</small></span>
            </div>
          </div>
        </article>

        <article v-if="activeTask" class="card task-result-card">
          <div class="card-header">
            <div><h2>已发布任务</h2><p>任务与学生子任务已同步到后端工作区。</p></div>
            <StatusPill :status="activeTask.status" :label="activeTask.status === 'RUNNING' ? '进行中' : activeTask.status === 'SCHEDULED' ? '待开始' : '已结束'" />
          </div>
          <div class="task-result">
            <div class="task-title"><span>任务编号</span><strong>{{ activeTask.id }}</strong></div>
            <div><span>任务名称</span><strong>{{ activeTask.title }}</strong></div>
            <div><span>学员任务</span><strong>{{ activeTask.assignedCount }} 个</strong></div>
            <div><span>角色数量</span><strong>{{ activeTask.groupCount }} 个</strong></div>
            <div><span>占用数据</span><strong>{{ activeTask.dataCount }} 条</strong></div>
            <div><span>完成进度</span><strong>{{ activeTask.completedCount }} / {{ activeTask.assignedCount }}</strong></div>
          </div>
        </article>
      </section>

      <aside class="publish-aside">
        <article class="card sticky-card">
          <div class="card-header">
            <div><h2>最终确认</h2><p>发布后即可进入监控</p></div>
            <span class="summary-dot" :class="{ ready: checklist.ready || Boolean(activeTask) }" />
          </div>
          <div class="confirm-summary">
            <div><span>考试批次</span><strong>{{ examSettings?.batchName ?? '未配置' }}</strong></div>
            <div><span>评分结构</span><strong>{{ examSettings?.objectiveWeight ?? 0 }}% 客观 / {{ examSettings?.subjectiveWeight ?? 0 }}% 主观</strong></div>
            <div><span>学生人数</span><strong>{{ uniqueLearners }} 人</strong></div>
            <div><span>角色关系</span><strong>{{ groupPlan?.members.length ?? 0 }} 条</strong></div>
            <div><span>正式数据</span><strong>{{ dataSummary.readyFormal }} 条可用</strong></div>
          </div>
          <div v-if="!activeTask" class="publish-callout" :class="{ blocked: !checklist.ready }">
            <strong>{{ checklist.ready ? '已满足发布条件' : '暂不可发布' }}</strong>
            <p>{{ checklist.ready ? '点击下方按钮将创建考试及全部学生任务。' : `仍有 ${checklist.reasons.length} 项阻断，请返回处理。` }}</p>
            <button class="primary" type="button" :disabled="!checklist.ready || publishing" @click="publishExam">
              {{ publishing ? '正在生成任务…' : '发布并生成学生任务' }}
            </button>
          </div>
          <div v-else class="published-links">
            <strong>下一步</strong>
            <p>教师进行过程监控与主观评分，学员可进入任务列表开始办理。</p>
            <button class="primary" type="button" @click="router.push({ name: 'teacher-dashboard' })">教师端 · 全程监控</button>
            <button class="secondary" type="button" @click="router.push({ name: 'student-tasks' })">学生端 · 我的任务</button>
          </div>
        </article>
      </aside>
    </div>
  </main>
</template>
