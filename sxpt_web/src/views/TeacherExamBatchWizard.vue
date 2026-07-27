<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { createApiLogStore } from '../api/apiLogStore';
import { createAuthenticatedApiClient } from '../api/v1/authenticatedApiClient';
import { createRunBatchApiFromConfig } from '../api/v1/runBatchApiFactory';
import BatchPreflightStep from '../components/run-batch/BatchPreflightStep.vue';
import DataGenerationStep from '../components/run-batch/DataGenerationStep.vue';
import FlowVersionStep from '../components/run-batch/FlowVersionStep.vue';
import GroupAssignmentStep from '../components/run-batch/GroupAssignmentStep.vue';
import SubjectiveRubricStep from '../components/run-batch/SubjectiveRubricStep.vue';
import { trainingConfig } from '../config/trainingConfig';
import {
  createRunBatchWizard,
  type RunBatchWizardStep
} from '../composables/useRunBatchWizard';
import type {
  PublishedWorkflowOption,
  RunBatchGroupMemberInput,
  UnitDataPlanInput,
  WorkflowRunType
} from '../types/runBatch';

const route = useRoute();
const router = useRouter();
const requestedRunType: WorkflowRunType =
  route.path.includes('/practices/') ? 'PRACTICE' : 'EXAM';
const apiLogStore = createApiLogStore();
const authenticatedClient = createAuthenticatedApiClient(
  trainingConfig,
  apiLogStore
);
const runBatchApi = createRunBatchApiFromConfig(
  trainingConfig,
  authenticatedClient
);
const wizard = createRunBatchWizard(runBatchApi, requestedRunType);

const selectedWorkflowId = ref('');
const batchName = ref('');
const plannedStartTime = ref('');
const plannedEndTime = ref('');
const notice = ref('');
const operation = ref('');

const steps: Array<{ key: RunBatchWizardStep; label: string }> = [
  { key: 'FLOW', label: '流程版本' },
  { key: 'MEMBERS', label: '成员分组' },
  { key: 'RUBRIC', label: '评分确认' },
  { key: 'DATA', label: '数据准备' },
  { key: 'PREFLIGHT', label: '预检发布' }
];

const selectedWorkflow = computed<PublishedWorkflowOption | undefined>(() =>
  wizard.publishedWorkflows.value.find(
    (workflow) => workflow.id === selectedWorkflowId.value
  )
);
const visibleRoleGroups = computed(() =>
  wizard.batch.value?.roleGroups ?? selectedWorkflow.value?.roleGroups ?? []
);
const visibleRubric = computed(() =>
  wizard.batch.value?.rubricItems ?? selectedWorkflow.value?.rubricItems ?? []
);
const objectiveMaxScore = computed(() =>
  wizard.batch.value?.objectiveMaxScore
    ?? selectedWorkflow.value?.objectiveMaxScore
    ?? 0
);
const subjectiveMaxScore = computed(() =>
  wizard.batch.value?.subjectiveMaxScore
    ?? selectedWorkflow.value?.subjectiveMaxScore
    ?? 0
);
const editingDisabled = computed(() => wizard.saving.value);
const commandDisabled = computed(
  () => wizard.loading.value || wizard.saving.value
);
const pageError = computed(() => readableError(wizard.error.value));
const effectiveRunType = computed(
  () => wizard.batch.value?.runType ?? requestedRunType
);
const pageTitle = computed(() =>
  effectiveRunType.value === 'EXAM' ? '创建考试批次' : '创建练习轮次'
);

onMounted(async () => {
  await execute('load', async () => {
    await wizard.loadPublishedWorkflows();
    const restoredBatchId = normalizeQuery(route.query.batchId);
    if (restoredBatchId) {
      await wizard.loadBatch(restoredBatchId);
      const restored = wizard.batch.value;
      if (!restored) throw new Error('无法恢复批次。');
      selectedWorkflowId.value = restored.workflowVersionId;
      batchName.value = restored.batchName;
      plannedStartTime.value = toLocalDateTime(restored.plannedStartTime);
      plannedEndTime.value = toLocalDateTime(restored.plannedEndTime);
      const generationStatus = wizard.generationJob.value?.status;
      wizard.setStep(
        generationStatus === 'COMPLETED'
        || generationStatus === 'COMPLETED_WITH_FAILURES'
          ? 'PREFLIGHT'
          : 'DATA'
      );
      notice.value = `已恢复批次 ${restored.id}，可继续调整、预检或发布。`;
      return;
    }
    const taskId = normalizeQuery(route.query.taskId);
    const preferred = wizard.publishedWorkflows.value.find(
      (workflow) => workflow.taskId === taskId
    );
    selectedWorkflowId.value =
      preferred?.id ?? wizard.publishedWorkflows.value[0]?.id ?? '';
    if (preferred) {
      batchName.value = `${preferred.name} - ${
        requestedRunType === 'EXAM' ? '考试批次' : '练习轮次'
      }`;
    }
    notice.value = wizard.publishedWorkflows.value.length
      ? '已加载已发布流程，请确认版本后创建批次。'
      : '当前没有可用的已发布流程版本。';
  });
});

async function execute(name: string, action: () => Promise<void>) {
  operation.value = name;
  notice.value = '';
  try {
    await action();
  } catch (error) {
    notice.value = readableError(error);
  } finally {
    operation.value = '';
  }
}

async function createBatch() {
  await execute('create', async () => {
    const workflow = selectedWorkflow.value;
    if (!workflow) throw new Error('请选择已发布流程版本。');
    const created = await wizard.createBatch({
      workflowVersionId: workflow.id,
      batchName: batchName.value.trim(),
      ...(plannedStartTime.value
        ? { plannedStartTime: new Date(plannedStartTime.value).toISOString() }
        : {}),
      ...(plannedEndTime.value
        ? { plannedEndTime: new Date(plannedEndTime.value).toISOString() }
        : {})
    });
    if (created.members.length === 0) {
      wizard.setMembers(seedMembers(workflow));
    }
    if (created.unitPlans.length === 0) {
      wizard.setUnitPlans(seedUnitPlans(workflow));
    }
    await router.replace({
      path: route.path,
      query: {
        ...(normalizeQuery(route.query.taskId)
          ? { taskId: normalizeQuery(route.query.taskId) }
          : {}),
        batchId: created.id
      }
    });
    wizard.setStep('MEMBERS');
    notice.value = `批次 ${created.id} 已创建，请完善成员分组。`;
  });
}

function updateMembers(members: RunBatchGroupMemberInput[]) {
  wizard.setMembers(members);
}

async function saveMembers() {
  await execute('save-members', async () => {
    await wizard.saveMembers();
    wizard.setStep('RUBRIC');
    notice.value = '成员分组已保存。';
  });
}

function updateUnitPlans(unitPlans: UnitDataPlanInput[]) {
  wizard.setUnitPlans(unitPlans);
}

async function saveUnitPlans() {
  await execute('save-plans', async () => {
    await wizard.saveUnitPlans();
    notice.value = '单位数据计划已保存。';
  });
}

async function startGeneration() {
  await execute('start-generation', async () => {
    await wizard.saveUnitPlans();
    await wizard.startGeneration();
    notice.value = '造数任务已创建，可刷新或推进任务进度。';
  });
}

async function advanceGeneration() {
  await execute('advance-generation', async () => {
    await wizard.advanceGeneration();
    notice.value = '造数任务已推进。';
  });
}

async function refreshGeneration() {
  await execute('refresh-generation', async () => {
    await wizard.refreshGenerationJob();
    notice.value = '已刷新造数进度。';
  });
}

async function runPreflight() {
  await execute('preflight', async () => {
    const result = await wizard.runPreflight();
    notice.value = result.valid
      ? '预检通过，可以发布批次。'
      : `预检发现 ${result.issues.length} 个阻断项。`;
  });
}

async function publishBatch() {
  await execute('publish', async () => {
    const saved = await wizard.publish();
    const destination = saved.runType === 'EXAM'
      ? `/teacher/exams/${encodeURIComponent(saved.id)}/data`
      : `/teacher/practices/${encodeURIComponent(saved.id)}/data`;
    await router.push(destination);
  });
}

async function openDataPool() {
  const current = wizard.batch.value;
  if (!current) return;
  await router.push(
    current.runType === 'EXAM'
      ? `/teacher/exams/${encodeURIComponent(current.id)}/data`
      : `/teacher/practices/${encodeURIComponent(current.id)}/data`
  );
}

function seedMembers(
  workflow: PublishedWorkflowOption
): RunBatchGroupMemberInput[] {
  const units = ['unit-finance', 'unit-operations'];
  return units.flatMap((unitId, unitIndex) =>
    workflow.roleGroups.map((group, groupIndex) => ({
      learnerId: unitIndex === 0
        ? 'learner-finance-shared'
        : `learner-operations-${groupIndex + 1}`,
      groupKey: group.groupKey,
      unitId,
      externalAccountMapping: {
        accountId: `${unitId}-account-${groupIndex + 1}`,
        roleId: group.groupKey.toLowerCase(),
        orgId: unitId
      }
    }))
  );
}

function seedUnitPlans(
  workflow: PublishedWorkflowOption
): UnitDataPlanInput[] {
  return ['unit-finance', 'unit-operations'].map((unitId) => ({
    unitId,
    formalCount: 1,
    spareCount: 1,
    teachingDataTemplateId: workflow.teachingDataTemplateId,
    scenarioVersion: workflow.scenarioVersion,
    generationParameters: { source: 'teacher-wizard' }
  }));
}

function normalizeQuery(value: unknown): string {
  const raw = Array.isArray(value) ? value[0] : value;
  return typeof raw === 'string' ? raw.trim() : '';
}

function toLocalDateTime(value?: string): string {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

function readableError(error: unknown): string {
  if (!error) return '';
  if (error instanceof Error) return error.message;
  return typeof error === 'string' ? error : '操作失败，请稍后重试。';
}
</script>

<template>
  <main class="batch-page">
    <header class="page-hero">
      <div>
        <p class="hero-kicker">RUN BATCH WIZARD</p>
        <h1>{{ pageTitle }}</h1>
        <p>复用已发布工作流快照，配置任意数量角色组成员，并按单位准备正式与备用数据。</p>
      </div>
      <div class="hero-meta">
        <span>{{ effectiveRunType }}</span>
        <strong>{{ wizard.batch.value?.status ?? 'NEW' }}</strong>
        <small>{{ wizard.batch.value?.id ?? '尚未创建批次' }}</small>
      </div>
    </header>

    <nav class="step-nav" aria-label="创建步骤">
      <button
        v-for="(item, index) in steps"
        :key="item.key"
        type="button"
        :class="{ active: wizard.step.value === item.key }"
        :disabled="commandDisabled || (!wizard.batch.value && item.key !== 'FLOW')"
        @click="wizard.setStep(item.key)"
      >
        <span>{{ index + 1 }}</span>{{ item.label }}
      </button>
    </nav>

    <p v-if="notice" class="notice">{{ notice }}</p>
    <p v-if="pageError && pageError !== notice" class="error-banner">{{ pageError }}</p>

    <section v-if="wizard.loading.value && !wizard.publishedWorkflows.value.length" class="loading-card">
      正在加载已发布流程…
    </section>

    <div v-else class="step-content">
      <FlowVersionStep
        v-if="wizard.step.value === 'FLOW'"
        v-model:selected-workflow-id="selectedWorkflowId"
        v-model:batch-name="batchName"
        v-model:planned-start-time="plannedStartTime"
        v-model:planned-end-time="plannedEndTime"
        :workflows="wizard.publishedWorkflows.value"
        :disabled="editingDisabled"
        :saved="Boolean(wizard.batch.value)"
        @create="createBatch"
      />

      <GroupAssignmentStep
        v-else-if="wizard.step.value === 'MEMBERS'"
        :members="wizard.members.value"
        :role-groups="visibleRoleGroups"
        :disabled="editingDisabled"
        @change="updateMembers"
        @save="saveMembers"
      />

      <SubjectiveRubricStep
        v-else-if="wizard.step.value === 'RUBRIC'"
        :objective-max-score="objectiveMaxScore"
        :subjective-max-score="subjectiveMaxScore"
        :rubric-items="visibleRubric"
      />

      <DataGenerationStep
        v-else-if="wizard.step.value === 'DATA'"
        :plans="wizard.unitPlans.value"
        :teaching-data-template-id="selectedWorkflow?.teachingDataTemplateId ?? ''"
        :scenario-version="selectedWorkflow?.scenarioVersion ?? ''"
        :summary="wizard.generationSummary.value"
        :disabled="editingDisabled"
        :command-disabled="commandDisabled"
        @change="updateUnitPlans"
        @save="saveUnitPlans"
        @start="startGeneration"
        @advance="advanceGeneration"
        @refresh="refreshGeneration"
      />

      <BatchPreflightStep
        v-else
        :result="wizard.preflight.value"
        :disabled="commandDisabled"
        :can-publish="wizard.canPublish.value"
        @check="runPreflight"
        @publish="publishBatch"
      />

      <div v-if="wizard.batch.value" class="step-footer">
        <div>
          <button
            type="button"
            class="secondary"
            :disabled="commandDisabled || steps.findIndex((item) => item.key === wizard.step.value) === 0"
            @click="wizard.setStep(steps[Math.max(0, steps.findIndex((item) => item.key === wizard.step.value) - 1)]!.key)"
          >上一步</button>
          <button
            type="button"
            :disabled="commandDisabled || steps.findIndex((item) => item.key === wizard.step.value) === steps.length - 1"
            @click="wizard.setStep(steps[Math.min(steps.length - 1, steps.findIndex((item) => item.key === wizard.step.value) + 1)]!.key)"
          >下一步</button>
        </div>
        <button
          v-if="wizard.step.value === 'DATA' || wizard.step.value === 'PREFLIGHT'"
          type="button"
          class="pool-button"
          :disabled="commandDisabled"
          @click="openDataPool"
        >查看/调整数据池</button>
      </div>
    </div>
  </main>
</template>

<style scoped>
.batch-page { min-height: 100vh; padding: 30px clamp(14px, 4vw, 56px) 70px; color: #0f172a; background: radial-gradient(circle at top right, #ede9fe 0, transparent 34%), #f8fafc; }
.page-hero { max-width: 1500px; margin: 0 auto 18px; display: flex; justify-content: space-between; gap: 28px; padding: 30px; border-radius: 24px; color: #fff; background: linear-gradient(120deg, #172554, #312e81 52%, #6d28d9); box-shadow: 0 24px 55px rgb(49 46 129 / 22%); }
.hero-kicker { margin: 0; color: #c4b5fd; font-size: 12px; font-weight: 850; letter-spacing: .16em; }
h1 { margin: 8px 0; font-size: clamp(28px, 4vw, 44px); }
.page-hero p:last-child { max-width: 760px; margin-bottom: 0; color: #e0e7ff; line-height: 1.7; }
.hero-meta { min-width: 210px; align-self: center; display: grid; gap: 8px; padding: 18px; border: 1px solid rgb(255 255 255 / 20%); border-radius: 18px; background: rgb(255 255 255 / 10%); }
.hero-meta strong { color: #ddd6fe; font-size: 19px; }
.step-nav { position: sticky; z-index: 8; top: 12px; max-width: 1500px; margin: 0 auto 16px; display: grid; grid-template-columns: repeat(5, 1fr); gap: 7px; padding: 10px; border: 1px solid rgb(203 213 225 / 80%); border-radius: 16px; background: rgb(255 255 255 / 92%); backdrop-filter: blur(15px); box-shadow: 0 10px 30px rgb(15 23 42 / 9%); }
.step-nav button { display: flex; align-items: center; justify-content: center; gap: 7px; border-color: transparent; color: #475569; background: transparent; }
.step-nav button span { display: inline-grid; width: 24px; height: 24px; place-items: center; border-radius: 50%; background: #e2e8f0; font-size: 11px; }
.step-nav button.active { color: #fff; background: #6d28d9; }
.step-nav button.active span { color: #6d28d9; background: #fff; }
.step-content, .notice, .error-banner, .loading-card { max-width: 1500px; margin-left: auto; margin-right: auto; }
.notice, .error-banner { padding: 11px 14px; border-radius: 12px; }
.notice { color: #1e40af; border: 1px solid #bfdbfe; background: #eff6ff; }
.error-banner { color: #b91c1c; border: 1px solid #fecaca; background: #fef2f2; }
.loading-card { padding: 42px; border-radius: 18px; text-align: center; background: #fff; }
.step-footer, .step-footer > div { display: flex; justify-content: space-between; gap: 9px; margin-top: 14px; }
.step-footer > div { margin-top: 0; }
.secondary { border-color: #cbd5e1; color: #475569; background: #fff; }
.pool-button { color: #5b21b6; border: 1px solid #c4b5fd; background: #f5f3ff; }
@media (max-width: 860px) { .page-hero { align-items: stretch; flex-direction: column; } .hero-meta { min-width: 0; } .step-nav { grid-template-columns: 1fr; position: static; } .step-nav button { justify-content: flex-start; } .step-footer { align-items: stretch; flex-direction: column; } }
</style>
