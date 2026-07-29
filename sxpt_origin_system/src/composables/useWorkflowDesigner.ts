import { computed, shallowRef, ref, type Ref } from 'vue';
import type { WorkflowDefinitionApi } from '../api/v1/workflowDefinitionApi';
import type {
  SaveWorkflowDraftRequest,
  SubjectiveRubricItemInput,
  TaskWorkflowDraft,
  TaskWorkflowVersion,
  WorkflowRoleGroupInput,
  WorkflowStageInput,
  WorkflowStageStepInput,
  WorkflowValidationResult
} from '../types/workflow';

export class WorkflowDesignerError extends Error {
  constructor(
    public readonly code: string,
    message: string
  ) {
    super(message);
    this.name = 'WorkflowDesignerError';
  }
}

export interface WorkflowDesigner {
  draft: Ref<TaskWorkflowDraft | null>;
  loading: Ref<boolean>;
  saving: Ref<boolean>;
  dirty: Ref<boolean>;
  conflict: Ref<boolean>;
  isReadOnly: Ref<boolean>;
  validation: Ref<WorkflowValidationResult | null>;
  load(taskId: string): Promise<void>;
  continueEditing(): Promise<void>;
  addRoleGroup(input: WorkflowRoleGroupInput): void;
  removeRoleGroup(groupId: string): void;
  addStage(input: WorkflowStageInput): void;
  removeStage(stageId: string): void;
  moveStage(stageId: string, targetIndex: number): void;
  updateStage(
    stageId: string,
    patch: Partial<WorkflowStageInput>
  ): void;
  replaceStageSteps(
    stageId: string,
    steps: WorkflowStageStepInput[]
  ): void;
  replaceRubricItems(items: SubjectiveRubricItemInput[]): void;
  updateMetadata(
    patch: Partial<
      Pick<
        TaskWorkflowDraft,
        'name' | 'objectiveMaxScore' | 'subjectiveMaxScore'
      >
    >
  ): void;
  updateDataTemplate(
    templateId: string,
    scenarioVersion: string,
    defaults: Record<string, unknown>
  ): void;
  save(): Promise<void>;
  validate(): Promise<WorkflowValidationResult>;
  publish(): Promise<TaskWorkflowVersion>;
}

export function createWorkflowDesigner(
  api: WorkflowDefinitionApi,
  initialDraft: TaskWorkflowDraft | null = null
): WorkflowDesigner {
  const draft = shallowRef<TaskWorkflowDraft | null>(
    initialDraft ? copyDraft(initialDraft) : null
  );
  const loading = ref(false);
  const saving = ref(false);
  const dirty = ref(false);
  const conflict = ref(false);
  const isReadOnly = computed(
    () => Boolean(draft.value && draft.value.versionStatus !== 'DRAFT')
  );
  const validation = shallowRef<WorkflowValidationResult | null>(null);
  let pendingSaveRequestId: string | undefined;
  let pendingPublishRequestId: string | undefined;

  function requireDraft(): TaskWorkflowDraft {
    if (!draft.value) {
      throw new WorkflowDesignerError(
        'WORKFLOW_NOT_LOADED',
        '工作流草稿尚未加载'
      );
    }
    return draft.value;
  }

  function requireEditable(): TaskWorkflowDraft {
    const current = requireDraft();
    if (current.versionStatus !== 'DRAFT') {
      throw new WorkflowDesignerError(
        'WORKFLOW_READ_ONLY',
        '已发布工作流为只读快照，请先创建新草稿再继续编辑'
      );
    }
    return current;
  }

  function mutate(
    operation: (current: TaskWorkflowDraft) => TaskWorkflowDraft
  ): void {
    draft.value = normalizeDraft(operation(copyDraft(requireEditable())));
    dirty.value = true;
    conflict.value = false;
    validation.value = null;
    pendingSaveRequestId = undefined;
    pendingPublishRequestId = undefined;
  }

  async function load(taskId: string): Promise<void> {
    loading.value = true;
    try {
      draft.value = normalizeDraft(await api.getDraft(taskId));
      dirty.value = false;
      conflict.value = false;
      validation.value = null;
      pendingSaveRequestId = undefined;
      pendingPublishRequestId = undefined;
    } finally {
      loading.value = false;
    }
  }

  async function continueEditing(): Promise<void> {
    const current = requireDraft();
    if (current.versionStatus === 'DRAFT') return;
    loading.value = true;
    try {
      const next = normalizeDraft(await api.getDraft(current.taskId));
      if (next.versionStatus !== 'DRAFT') {
        throw new WorkflowDesignerError(
          'DRAFT_NOT_CREATED',
          '未能获得新的工作流草稿'
        );
      }
      draft.value = next;
      dirty.value = false;
      conflict.value = false;
      validation.value = null;
      pendingSaveRequestId = undefined;
      pendingPublishRequestId = undefined;
    } finally {
      loading.value = false;
    }
  }

  function addRoleGroup(input: WorkflowRoleGroupInput): void {
    mutate((current) => ({
      ...current,
      roleGroups: [...current.roleGroups, { ...input }]
    }));
  }

  function removeRoleGroup(groupId: string): void {
    const current = requireEditable();
    if (current.stages.some((stage) => stage.roleGroupId === groupId)) {
      throw new WorkflowDesignerError(
        'GROUP_IN_USE',
        '该实训分组仍被业务阶段引用，请先调整阶段归属'
      );
    }
    mutate((value) => ({
      ...value,
      roleGroups: value.roleGroups.filter((group) => group.id !== groupId)
    }));
  }

  function addStage(input: WorkflowStageInput): void {
    mutate((current) => ({
      ...current,
      stages: [...current.stages, copyStage(input)]
    }));
  }

  function removeStage(stageId: string): void {
    mutate((current) => ({
      ...current,
      stages: current.stages.filter((stage) => stage.id !== stageId)
    }));
  }

  function moveStage(stageId: string, targetIndex: number): void {
    const current = requireEditable();
    const sourceIndex = current.stages.findIndex(
      (stage) => stage.id === stageId
    );
    if (sourceIndex < 0) {
      throw new WorkflowDesignerError(
        'STAGE_NOT_FOUND',
        '未找到要移动的业务阶段'
      );
    }
    const boundedIndex = Math.max(
      0,
      Math.min(targetIndex, current.stages.length - 1)
    );
    mutate((value) => {
      const stages = [...value.stages];
      const [moved] = stages.splice(sourceIndex, 1);
      stages.splice(boundedIndex, 0, moved);
      return { ...value, stages };
    });
  }

  function updateStage(
    stageId: string,
    patch: Partial<WorkflowStageInput>
  ): void {
    mutate((current) => ({
      ...current,
      stages: current.stages.map((stage) =>
        stage.id === stageId
          ? {
              ...stage,
              ...patch,
              id: stage.id,
              steps: patch.steps
                ? patch.steps.map(copyStep)
                : stage.steps.map(copyStep)
            }
          : stage
      )
    }));
  }

  function replaceStageSteps(
    stageId: string,
    steps: WorkflowStageStepInput[]
  ): void {
    const current = requireEditable();
    if (!current.stages.some((stage) => stage.id === stageId)) {
      throw new WorkflowDesignerError(
        'STAGE_NOT_FOUND',
        '未找到要绑定录制步骤的业务阶段'
      );
    }
    updateStage(stageId, { steps: steps.map(copyStep) });
  }

  function replaceRubricItems(
    items: SubjectiveRubricItemInput[]
  ): void {
    mutate((current) => ({
      ...current,
      rubricItems: items.map((item) => ({ ...item }))
    }));
  }

  function updateMetadata(
    patch: Partial<
      Pick<
        TaskWorkflowDraft,
        'name' | 'objectiveMaxScore' | 'subjectiveMaxScore'
      >
    >
  ): void {
    mutate((current) => ({ ...current, ...patch }));
  }

  function updateDataTemplate(
    templateId: string,
    scenarioVersion: string,
    defaults: Record<string, unknown>
  ): void {
    mutate((current) => ({
      ...current,
      teachingDataTemplateId: templateId,
      dataScenarioVersion: scenarioVersion,
      dataGenerationDefaults: { ...defaults }
    }));
  }

  async function save(): Promise<void> {
    const localDraft = requireEditable();
    if (!dirty.value) return;

    saving.value = true;
    pendingSaveRequestId ??= api.createClientRequestId();
    try {
      const saved = await api.saveDraft(
        localDraft.taskId,
        toSaveRequest(localDraft, pendingSaveRequestId)
      );
      draft.value = normalizeDraft(saved);
      dirty.value = false;
      conflict.value = false;
      pendingSaveRequestId = undefined;
    } catch (error) {
      if (isConflict(error)) conflict.value = true;
      throw error;
    } finally {
      saving.value = false;
    }
  }

  async function validate(): Promise<WorkflowValidationResult> {
    const current = requireDraft();
    const result = await api.validateDraft(current.taskId);
    validation.value = result;
    return result;
  }

  async function publish(): Promise<TaskWorkflowVersion> {
    requireEditable();
    if (dirty.value) await save();
    const current = requireDraft();
    if (!pendingPublishRequestId) {
      const result = await validate();
      if (!result.valid) {
        throw new WorkflowDesignerError(
          'WORKFLOW_INVALID',
          '工作流仍有发布阻断项'
        );
      }
      pendingPublishRequestId = api.createClientRequestId();
    }

    try {
      const published = await api.publish(current.taskId, {
        clientRequestId: pendingPublishRequestId
      });
      draft.value = normalizeDraft(published);
      dirty.value = false;
      conflict.value = false;
      pendingPublishRequestId = undefined;
      return published;
    } catch (error) {
      throw error;
    }
  }

  return {
    draft,
    loading,
    saving,
    dirty,
    conflict,
    isReadOnly,
    validation,
    load,
    continueEditing,
    addRoleGroup,
    removeRoleGroup,
    addStage,
    removeStage,
    moveStage,
    updateStage,
    replaceStageSteps,
    replaceRubricItems,
    updateMetadata,
    updateDataTemplate,
    save,
    validate,
    publish
  };
}

function toSaveRequest(
  draft: TaskWorkflowDraft,
  clientRequestId: string
): SaveWorkflowDraftRequest {
  return {
    clientRequestId,
    expectedLockVersion: draft.lockVersion,
    name: draft.name,
    teachingDataTemplateId: draft.teachingDataTemplateId,
    dataScenarioVersion: draft.dataScenarioVersion,
    dataGenerationDefaults: { ...draft.dataGenerationDefaults },
    objectiveMaxScore: draft.objectiveMaxScore,
    subjectiveMaxScore: draft.subjectiveMaxScore,
    roleGroups: draft.roleGroups.map((group) => ({ ...group })),
    stages: draft.stages.map(copyStage),
    rubricItems: draft.rubricItems.map((item) => ({ ...item }))
  };
}

function normalizeDraft(draft: TaskWorkflowDraft): TaskWorkflowDraft {
  return {
    ...draft,
    dataGenerationDefaults: { ...draft.dataGenerationDefaults },
    roleGroups: draft.roleGroups.map((group, index) => ({
      ...group,
      sequenceNo: index + 1
    })),
    stages: draft.stages.map((stage, index) => ({
      ...copyStage(stage),
      sequenceNo: index + 1,
      steps: stage.steps.map((step, stepIndex) => ({
        ...copyStep(step),
        sequenceNo: stepIndex + 1
      }))
    })),
    rubricItems: draft.rubricItems.map((item, index) => ({
      ...item,
      sequenceNo: index + 1
    }))
  };
}

function copyDraft(draft: TaskWorkflowDraft): TaskWorkflowDraft {
  return {
    ...draft,
    dataGenerationDefaults: { ...draft.dataGenerationDefaults },
    roleGroups: draft.roleGroups.map((group) => ({ ...group })),
    stages: draft.stages.map(copyStage),
    rubricItems: draft.rubricItems.map((item) => ({ ...item }))
  };
}

function copyStage(stage: WorkflowStageInput): WorkflowStageInput {
  return {
    ...stage,
    launchConfig: { ...stage.launchConfig },
    steps: stage.steps.map(copyStep)
  };
}

function copyStep(step: WorkflowStageStepInput): WorkflowStageStepInput {
  return {
    ...step,
    matcher: { ...step.matcher }
  };
}

function isConflict(error: unknown): boolean {
  if (typeof error !== 'object' || error === null) return false;
  const candidate = error as { code?: unknown; message?: unknown };
  return candidate.code === 409
    || candidate.code === 'CONCURRENT_MODIFICATION'
    || candidate.message === 'WORKFLOW_DRAFT_CONFLICT';
}
