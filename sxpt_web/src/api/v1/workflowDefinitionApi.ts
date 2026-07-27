import { TrainingApiError } from '../../types/trainingApi';
import type {
  SaveWorkflowDraftRequest,
  SubjectiveRubricItemInput,
  TaskWorkflowDraft,
  TaskWorkflowVersion,
  WorkflowRoleGroupInput,
  WorkflowStageInput,
  WorkflowStageStepInput,
  WorkflowValidationResult,
  WorkflowVersionStatus
} from '../../types/workflow';
import type { AuthenticatedApiClient } from './authenticatedApiClient';

export interface WorkflowDefinitionApi {
  getDraft(taskId: string): Promise<TaskWorkflowDraft>;
  saveDraft(
    taskId: string,
    request: SaveWorkflowDraftRequest
  ): Promise<TaskWorkflowDraft>;
  validateDraft(taskId: string): Promise<WorkflowValidationResult>;
  publish(
    taskId: string,
    request: { clientRequestId: string }
  ): Promise<TaskWorkflowVersion>;
  getVersion(versionId: string): Promise<TaskWorkflowVersion>;
  createClientRequestId(): string;
}

type DecimalValue = number | string;

interface RawWorkflowStageStep
  extends Omit<WorkflowStageStepInput, 'score' | 'matcher'> {
  score: DecimalValue;
  matcherJson: unknown;
}

interface RawWorkflowStage
  extends Omit<
    WorkflowStageInput,
    'launchConfig' | 'stageMaxScore' | 'steps'
  > {
  launchConfigJson: unknown;
  stageMaxScore: DecimalValue;
  steps: RawWorkflowStageStep[];
}

interface RawRubricItem
  extends Omit<SubjectiveRubricItemInput, 'maxScore'> {
  maxScore: DecimalValue;
}

interface RawTaskWorkflowDraft
  extends Omit<
    TaskWorkflowDraft,
    | 'dataGenerationDefaults'
    | 'objectiveMaxScore'
    | 'subjectiveMaxScore'
    | 'stages'
    | 'rubricItems'
  > {
  dataGenerationDefaultsJson: unknown;
  objectiveMaxScore: DecimalValue;
  subjectiveMaxScore: DecimalValue;
  stages: RawWorkflowStage[];
  rubricItems: RawRubricItem[];
}

interface RawTaskWorkflowVersion extends RawTaskWorkflowDraft {
  snapshotDigest: string;
  publishedBy: string;
  publishedTime: string;
}

export function createWorkflowDefinitionApi(
  client: AuthenticatedApiClient
): WorkflowDefinitionApi {
  return {
    async getDraft(taskId) {
      const raw = await client.get<RawTaskWorkflowDraft>(
        `/tasks/${encodeURIComponent(taskId)}/workflow-draft`
      );
      return normalizeDraft(raw);
    },

    async saveDraft(taskId, request) {
      const raw = await client.put<RawTaskWorkflowDraft>(
        `/tasks/${encodeURIComponent(taskId)}/workflow-draft`,
        serializeSaveRequest(request)
      );
      return normalizeDraft(raw);
    },

    validateDraft(taskId) {
      return client.get<WorkflowValidationResult>(
        `/tasks/${encodeURIComponent(taskId)}/workflow-validation`
      );
    },

    async publish(taskId, request) {
      const raw = await client.post<RawTaskWorkflowVersion>(
        `/tasks/${encodeURIComponent(taskId)}/workflow-publish`,
        request
      );
      return normalizeVersion(raw);
    },

    async getVersion(versionId) {
      const raw = await client.get<RawTaskWorkflowVersion>(
        `/workflow-versions/${encodeURIComponent(versionId)}`
      );
      return normalizeVersion(raw);
    },

    createClientRequestId: () => client.createClientRequestId()
  };
}

function serializeSaveRequest(request: SaveWorkflowDraftRequest) {
  return {
    ...request,
    objectiveMaxScore: decimalString(request.objectiveMaxScore),
    subjectiveMaxScore: decimalString(request.subjectiveMaxScore),
    stages: request.stages.map((stage) => ({
      ...stage,
      stageMaxScore: decimalString(stage.stageMaxScore),
      steps: stage.steps.map((step) => ({
        ...step,
        score: decimalString(step.score)
      }))
    })),
    rubricItems: request.rubricItems.map((item) => ({
      ...item,
      maxScore: decimalString(item.maxScore)
    }))
  };
}

function normalizeDraft(raw: RawTaskWorkflowDraft): TaskWorkflowDraft {
  return {
    id: raw.id,
    tenantId: raw.tenantId,
    taskId: raw.taskId,
    versionNo: raw.versionNo,
    versionStatus: raw.versionStatus as WorkflowVersionStatus,
    name: raw.name,
    serialPolicy: 'SERIAL_ONLY',
    teachingDataTemplateId: raw.teachingDataTemplateId,
    dataScenarioVersion: raw.dataScenarioVersion,
    dataGenerationDefaults: parseJsonRecord(
      raw.dataGenerationDefaultsJson,
      'dataGenerationDefaultsJson'
    ),
    objectiveMaxScore: decimalNumber(
      raw.objectiveMaxScore,
      'objectiveMaxScore'
    ),
    subjectiveMaxScore: decimalNumber(
      raw.subjectiveMaxScore,
      'subjectiveMaxScore'
    ),
    lockVersion: raw.lockVersion,
    roleGroups: raw.roleGroups.map(normalizeRoleGroup),
    stages: raw.stages.map(normalizeStage),
    rubricItems: raw.rubricItems.map((item) => ({
      ...item,
      maxScore: decimalNumber(item.maxScore, 'rubricItems.maxScore')
    }))
  };
}

function normalizeVersion(raw: RawTaskWorkflowVersion): TaskWorkflowVersion {
  return {
    ...normalizeDraft(raw),
    snapshotDigest: raw.snapshotDigest,
    publishedBy: raw.publishedBy,
    publishedTime: raw.publishedTime
  };
}

function normalizeRoleGroup(
  group: WorkflowRoleGroupInput
): WorkflowRoleGroupInput {
  return { ...group };
}

function normalizeStage(stage: RawWorkflowStage): WorkflowStageInput {
  return {
    id: stage.id,
    stageKey: stage.stageKey,
    name: stage.name,
    sequenceNo: stage.sequenceNo,
    roleGroupId: stage.roleGroupId,
    externalRoleId: stage.externalRoleId,
    externalOrgId: stage.externalOrgId,
    launchConfig: parseJsonRecord(
      stage.launchConfigJson,
      `stages.${stage.id}.launchConfigJson`
    ),
    stageMaxScore: decimalNumber(
      stage.stageMaxScore,
      `stages.${stage.id}.stageMaxScore`
    ),
    timeLimitSeconds: stage.timeLimitSeconds,
    reconnectGraceSeconds: stage.reconnectGraceSeconds,
    allowSameLearnerNextDefault: stage.allowSameLearnerNextDefault,
    allowBatchOverride: stage.allowBatchOverride,
    releasePolicy: stage.releasePolicy,
    reattemptPolicy: stage.reattemptPolicy,
    steps: stage.steps.map((step) => ({
      id: step.id,
      taskStepId: step.taskStepId,
      recordedSegmentId: step.recordedSegmentId,
      recordedAssetVersion: step.recordedAssetVersion,
      sequenceNo: step.sequenceNo,
      stepKind: step.stepKind,
      completionMethod: step.completionMethod,
      failurePolicy: step.failurePolicy,
      score: decimalNumber(
        step.score,
        `stages.${stage.id}.steps.${step.id}.score`
      ),
      matcher: parseJsonRecord(
        step.matcherJson,
        `stages.${stage.id}.steps.${step.id}.matcherJson`
      )
    }))
  };
}

function parseJsonRecord(
  value: unknown,
  field: string
): Record<string, unknown> {
  try {
    const parsed = typeof value === 'string' ? JSON.parse(value) : value;
    if (typeof parsed !== 'object' || parsed === null || Array.isArray(parsed)) {
      throw new Error('not an object');
    }
    return parsed as Record<string, unknown>;
  } catch {
    throw new TrainingApiError(
      `接口字段 ${field} 不是有效的 JSON 对象`,
      'INVALID_RESPONSE'
    );
  }
}

function decimalNumber(value: DecimalValue, field: string): number {
  const result = Number(value);
  if (!Number.isFinite(result)) {
    throw new TrainingApiError(
      `接口字段 ${field} 不是有效分值`,
      'INVALID_RESPONSE'
    );
  }
  return result;
}

function decimalString(value: number): string {
  if (!Number.isFinite(value)) {
    throw new TrainingApiError('分值必须是有限数字', 'INVALID_REQUEST');
  }
  return String(value);
}
