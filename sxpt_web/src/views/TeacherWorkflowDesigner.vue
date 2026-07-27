<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { createApiLogStore } from '../api/apiLogStore';
import { createAuthenticatedApiClient } from '../api/v1/authenticatedApiClient';
import { createWorkflowDefinitionApi } from '../api/v1/workflowDefinitionApi';
import RoleGroupPanel from '../components/workflow/RoleGroupPanel.vue';
import SerialStageEditor from '../components/workflow/SerialStageEditor.vue';
import StagePropertyPanel from '../components/workflow/StagePropertyPanel.vue';
import StageStepBindingPanel from '../components/workflow/StageStepBindingPanel.vue';
import SubjectiveRubricEditor from '../components/workflow/SubjectiveRubricEditor.vue';
import WorkflowDataTemplatePanel from '../components/workflow/WorkflowDataTemplatePanel.vue';
import WorkflowValidationPanel from '../components/workflow/WorkflowValidationPanel.vue';
import { trainingConfig } from '../config/trainingConfig';
import { createWorkflowDesigner } from '../composables/useWorkflowDesigner';
import type {
  SubjectiveRubricItemInput,
  WorkflowRoleGroupInput,
  WorkflowStageInput,
  WorkflowStageStepInput
} from '../types/workflow';

const route = useRoute();
const router = useRouter();
const taskId = computed(() => String(route.params.taskId ?? ''));
const apiLogStore = createApiLogStore();
const workflowApi = createWorkflowDefinitionApi(
  createAuthenticatedApiClient(trainingConfig, apiLogStore)
);
const designer = createWorkflowDesigner(workflowApi);
const selectedStageId = ref('');
const operation = ref('');
const notice = ref('');
const publishedDigest = ref('');
const isReadOnly = computed(() => designer.isReadOnly.value);

const selectedStage = computed(() =>
  designer.draft.value?.stages.find(
    (stage) => stage.id === selectedStageId.value
  )
);
const rubricItems = computed(
  () => designer.draft.value?.rubricItems ?? []
);
const publishDisabled = computed(
  () =>
    designer.loading.value
    || designer.saving.value
    || designer.dirty.value
    || designer.validation.value?.valid !== true
    || designer.draft.value?.versionStatus !== 'DRAFT'
);

watch(
  () => designer.draft.value?.stages.map((stage) => stage.id).join(','),
  () => {
    const stages = designer.draft.value?.stages ?? [];
    if (!stages.some((stage) => stage.id === selectedStageId.value)) {
      selectedStageId.value = stages[0]?.id ?? '';
    }
  },
  { immediate: true }
);

onMounted(async () => {
  if (!taskId.value) {
    notice.value = '路由中缺少 taskId，无法加载工作流。';
    return;
  }
  await run('load', async () => {
    await designer.load(taskId.value);
    const recordedStageId = String(route.query.stageRecorded ?? '');
    selectedStageId.value = recordedStageId
      || designer.draft.value?.stages[0]?.id
      || '';
    notice.value = recordedStageId
      ? '阶段录制资产已写入草稿，请执行校验。'
      : '工作流草稿已加载。';
  });
});

async function run(name: string, action: () => Promise<void>) {
  operation.value = name;
  try {
    await action();
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '操作失败';
  } finally {
    operation.value = '';
  }
}

function addRoleGroup(group: WorkflowRoleGroupInput) {
  designer.addRoleGroup(group);
  notice.value = `已新增实训分组“${group.name}”。`;
}

function removeRoleGroup(groupId: string) {
  try {
    designer.removeRoleGroup(groupId);
  } catch (error) {
    notice.value = error instanceof Error ? error.message : '无法删除分组';
  }
}

function addStage() {
  const group = designer.draft.value?.roleGroups[0];
  if (!group) {
    notice.value = '请先创建实训分组，再新增业务阶段。';
    return;
  }
  const sequenceNo = (designer.draft.value?.stages.length ?? 0) + 1;
  const stage: WorkflowStageInput = {
    id: createId('stage'),
    stageKey: `STAGE_${sequenceNo}`,
    name: `业务阶段 ${sequenceNo}`,
    sequenceNo,
    roleGroupId: group.id,
    externalRoleId: '',
    externalOrgId: '',
    launchConfig: { path: '' },
    stageMaxScore: 0,
    timeLimitSeconds: 900,
    reconnectGraceSeconds: 60,
    allowSameLearnerNextDefault: false,
    allowBatchOverride: true,
    releasePolicy: 'BEFORE_FIRST_EVENT',
    reattemptPolicy: 'REPLACEMENT_DATA',
    steps: []
  };
  designer.addStage(stage);
  selectedStageId.value = stage.id;
}

function updateStage(
  stageId: string,
  patch: Partial<WorkflowStageInput>
) {
  designer.updateStage(stageId, patch);
}

function replaceStageSteps(
  stageId: string,
  steps: WorkflowStageStepInput[]
) {
  designer.replaceStageSteps(stageId, steps);
}

function replaceRubricItems(items: SubjectiveRubricItemInput[]) {
  designer.replaceRubricItems(items);
}

async function recordStage(stageId: string) {
  await run('record', async () => {
    if (isReadOnly.value) {
      throw new Error('已发布工作流不能重新录制，请先创建新草稿');
    }
    if (designer.dirty.value) await designer.save();
    const draftId = designer.draft.value?.id;
    if (!draftId) throw new Error('工作流草稿尚未加载。');
    await router.push(
      `/teacher/workflows/${encodeURIComponent(draftId)}/stages/${encodeURIComponent(stageId)}/record`
    );
  });
}

async function saveDraft() {
  await run('save', async () => {
    await designer.save();
    notice.value = `草稿已保存，锁版本 ${designer.draft.value?.lockVersion}。`;
  });
}

async function validateDraft() {
  await run('validate', async () => {
    if (designer.dirty.value) await designer.save();
    const result = await designer.validate();
    notice.value = result.valid
      ? '发布校验通过。'
      : `校验发现 ${result.issues.length} 个阻断项。`;
  });
}

async function publishWorkflow() {
  await run('publish', async () => {
    const result = await designer.publish();
    publishedDigest.value = result.snapshotDigest;
    notice.value = `工作流版本 ${result.versionNo} 已发布。`;
  });
}

async function continueEditing() {
  await run('continue', async () => {
    await designer.continueEditing();
    publishedDigest.value = '';
    notice.value = `已创建版本 ${designer.draft.value?.versionNo} 草稿，可继续编辑。`;
  });
}

async function openRunBatch(kind: 'exam' | 'practice') {
  await router.push({
    path: kind === 'exam'
      ? '/teacher/exams/new'
      : '/teacher/practices/new',
    query: { taskId: taskId.value }
  });
}

function applyDataTemplate(value: {
  templateId: string;
  scenarioVersion: string;
  defaults: Record<string, unknown>;
}) {
  designer.updateDataTemplate(
    value.templateId,
    value.scenarioVersion,
    value.defaults
  );
}

function createId(prefix: string) {
  return `${prefix}-${typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`}`;
}
</script>

<template>
  <main class="workflow-page">
    <header class="page-hero">
      <div>
        <p class="hero-kicker">MULTI-ROLE WORKFLOW</p>
        <h1>多角色业务实训编排</h1>
        <p>
          通过任意数量的实训分组与串行业务阶段，编排“经办—复核—审批”等协作流程。
          平台记录流程性操作，业务数据由业务方接口生成。
        </p>
      </div>
      <div class="hero-meta">
        <span>任务 {{ taskId }}</span>
        <strong>{{ designer.draft.value?.versionStatus ?? '加载中' }}</strong>
        <small>v{{ designer.draft.value?.versionNo ?? '-' }} · lock {{ designer.draft.value?.lockVersion ?? '-' }}</small>
      </div>
    </header>

    <div class="sticky-actions">
      <div>
        <strong>{{ designer.draft.value?.name || '工作流草稿' }}</strong>
        <span :class="{ dirty: designer.dirty.value }">
          {{ designer.dirty.value ? '有未保存修改' : '已保存' }}
        </span>
        <span v-if="designer.conflict.value" class="conflict">
          检测到并发修改，请刷新后合并
        </span>
      </div>
      <div class="action-buttons">
        <button
          type="button"
          class="run-button"
          :disabled="!taskId || Boolean(operation)"
          @click="openRunBatch('exam')"
        >
          创建考试批次
        </button>
        <button
          type="button"
          class="run-button"
          :disabled="!taskId || Boolean(operation)"
          @click="openRunBatch('practice')"
        >
          创建练习轮次
        </button>
        <button
          v-if="isReadOnly"
          type="button"
          class="continue-button"
          :disabled="Boolean(operation)"
          @click="continueEditing"
        >
          {{ operation === 'continue' ? '创建中…' : '创建新草稿继续编辑' }}
        </button>
        <button
          type="button"
          :disabled="isReadOnly || !designer.dirty.value || Boolean(operation)"
          @click="saveDraft"
        >
          {{ operation === 'save' ? '保存中…' : '保存草稿' }}
        </button>
        <button
          type="button"
          :disabled="Boolean(operation)"
          @click="validateDraft"
        >
          {{ operation === 'validate' ? '校验中…' : '发布校验' }}
        </button>
        <button
          type="button"
          class="publish-button"
          :disabled="publishDisabled || Boolean(operation)"
          @click="publishWorkflow"
        >
          {{ operation === 'publish' ? '发布中…' : '发布工作流' }}
        </button>
      </div>
    </div>

    <p v-if="notice" class="notice">{{ notice }}</p>
    <p v-if="publishedDigest" class="digest">
      不可变快照 SHA-256：<code>{{ publishedDigest }}</code>
    </p>
    <p v-if="isReadOnly" class="read-only-banner">
      当前为已发布的不可变版本，页面仅供查看。继续编辑会先复制生成新的 DRAFT。
    </p>

    <section v-if="designer.loading.value" class="loading-card">
      正在加载工作流草稿…
    </section>

    <template v-else-if="designer.draft.value">
      <section class="score-strip">
        <label>
          工作流名称
          <input
            :value="designer.draft.value.name"
            :disabled="isReadOnly"
            @input="designer.updateMetadata({ name: ($event.target as HTMLInputElement).value })"
          />
        </label>
        <label>
          客观满分
          <input
            :value="designer.draft.value.objectiveMaxScore"
            :disabled="isReadOnly"
            type="number"
            min="0"
            @input="designer.updateMetadata({ objectiveMaxScore: Number(($event.target as HTMLInputElement).value) })"
          />
        </label>
        <label>
          主观满分
          <input
            :value="designer.draft.value.subjectiveMaxScore"
            :disabled="isReadOnly"
            type="number"
            min="0"
            @input="designer.updateMetadata({ subjectiveMaxScore: Number(($event.target as HTMLInputElement).value) })"
          />
        </label>
        <div>
          <strong>评分构成</strong>
          <span>系统客观评分 + 教师主观评分与批语</span>
        </div>
      </section>

      <div class="top-grid">
        <RoleGroupPanel
          :groups="designer.draft.value.roleGroups"
          :readonly="isReadOnly"
          @add="addRoleGroup"
          @remove="removeRoleGroup"
        />
        <WorkflowDataTemplatePanel
          :template-id="designer.draft.value.teachingDataTemplateId"
          :scenario-version="designer.draft.value.dataScenarioVersion"
          :defaults="designer.draft.value.dataGenerationDefaults"
          :readonly="isReadOnly"
          @change="applyDataTemplate"
        />
      </div>

      <SerialStageEditor
        :stages="designer.draft.value.stages"
        :selected-stage-id="selectedStageId"
        :readonly="isReadOnly"
        @add="addStage"
        @remove="designer.removeStage"
        @move="designer.moveStage"
        @select="selectedStageId = $event"
        @record="recordStage"
      />

      <div class="detail-grid">
        <StagePropertyPanel
          :stage="selectedStage"
          :groups="designer.draft.value.roleGroups"
          :readonly="isReadOnly"
          @change="updateStage"
        />
        <StageStepBindingPanel
          :stage="selectedStage"
          :readonly="isReadOnly"
          @change="replaceStageSteps"
          @record="recordStage"
        />
      </div>

      <SubjectiveRubricEditor
        :items="rubricItems"
        :readonly="isReadOnly"
        @change="replaceRubricItems"
      />
      <WorkflowValidationPanel :result="designer.validation.value" />
    </template>
  </main>
</template>

<style scoped>
.workflow-page { min-height: 100vh; padding: 30px clamp(16px, 4vw, 56px) 70px; background: radial-gradient(circle at top right, #ede9fe 0, transparent 34%), #f8fafc; color: #0f172a; }
.page-hero { max-width: 1500px; margin: 0 auto 20px; display: flex; justify-content: space-between; gap: 28px; padding: 30px; border-radius: 24px; color: #fff; background: linear-gradient(120deg, #172554, #312e81 52%, #6d28d9); box-shadow: 0 24px 55px rgb(49 46 129 / 22%); }
.hero-kicker { margin: 0; color: #c4b5fd; font-size: 12px; font-weight: 850; letter-spacing: .16em; }
h1 { margin: 8px 0; font-size: clamp(28px, 4vw, 44px); }
.page-hero p:last-child { max-width: 820px; margin-bottom: 0; color: #e0e7ff; line-height: 1.75; }
.hero-meta { min-width: 190px; align-self: center; display: grid; gap: 8px; padding: 18px; border: 1px solid rgb(255 255 255 / 20%); border-radius: 18px; background: rgb(255 255 255 / 10%); }
.hero-meta strong { font-size: 19px; color: #ddd6fe; }
.sticky-actions { position: sticky; z-index: 8; top: 12px; max-width: 1500px; margin: 0 auto 16px; display: flex; align-items: center; justify-content: space-between; gap: 18px; padding: 13px 16px; border: 1px solid rgb(203 213 225 / 80%); border-radius: 16px; background: rgb(255 255 255 / 92%); backdrop-filter: blur(15px); box-shadow: 0 10px 30px rgb(15 23 42 / 9%); }
.sticky-actions > div:first-child { display: flex; align-items: center; flex-wrap: wrap; gap: 10px; }
.sticky-actions span { padding: 4px 8px; border-radius: 999px; color: #047857; background: #ecfdf5; font-size: 11px; font-weight: 750; }
.sticky-actions span.dirty { color: #b45309; background: #fef3c7; }
.sticky-actions span.conflict { color: #b91c1c; background: #fee2e2; }
.action-buttons { display: flex; gap: 8px; }
.action-buttons button { border: 1px solid #cbd5e1; border-radius: 10px; padding: 9px 14px; background: #fff; color: #334155; font-weight: 750; cursor: pointer; }
.action-buttons button:disabled { opacity: .45; cursor: default; }
.action-buttons .publish-button { border: 0; color: #fff; background: #7c3aed; }
.action-buttons .continue-button { border-color: #86efac; color: #166534; background: #f0fdf4; }
.action-buttons .run-button { border-color: #c4b5fd; color: #5b21b6; background: #f5f3ff; }
.notice, .digest, .loading-card, .score-strip, .top-grid, .detail-grid, :deep(.workflow-card), :deep(.validation-panel) { max-width: 1500px; margin-left: auto; margin-right: auto; }
.notice, .digest { padding: 11px 14px; border-radius: 12px; }
.read-only-banner { max-width: 1500px; margin: 0 auto 16px; padding: 12px 14px; border: 1px solid #fde68a; border-radius: 12px; color: #92400e; background: #fffbeb; }
.notice { color: #1e40af; background: #eff6ff; border: 1px solid #bfdbfe; }
.digest { color: #475569; background: #fff; border: 1px solid #e2e8f0; overflow-wrap: anywhere; }
.loading-card { padding: 40px; text-align: center; background: #fff; border-radius: 18px; }
.score-strip { display: grid; grid-template-columns: 2fr .7fr .7fr 1.4fr; gap: 10px; margin-bottom: 16px; padding: 15px; border: 1px solid #e2e8f0; border-radius: 16px; background: #fff; }
.score-strip label { display: grid; gap: 5px; color: #475569; font-size: 12px; font-weight: 700; }
.score-strip input { min-width: 0; border: 1px solid #cbd5e1; border-radius: 9px; padding: 9px; }
.score-strip > div { display: grid; align-content: center; gap: 4px; padding-left: 12px; border-left: 1px solid #e2e8f0; }
.score-strip span { color: #64748b; font-size: 12px; }
.top-grid, .detail-grid { display: grid; grid-template-columns: 1fr 1.25fr; gap: 16px; margin-bottom: 16px; }
:deep(.workflow-card), :deep(.validation-panel) { margin-bottom: 16px; }
.top-grid :deep(.workflow-card), .detail-grid :deep(.workflow-card) { margin-bottom: 0; }
@media (max-width: 980px) { .page-hero, .sticky-actions { align-items: stretch; flex-direction: column; } .hero-meta { min-width: 0; } .top-grid, .detail-grid, .score-strip { grid-template-columns: 1fr; } .score-strip > div { border-left: 0; padding-left: 0; } }
@media (max-width: 620px) { .action-buttons { display: grid; grid-template-columns: 1fr 1fr; } .publish-button { grid-column: 1 / -1; } }
</style>
