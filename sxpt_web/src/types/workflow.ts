export type WorkflowStepKind = 'REQUIRED' | 'OPTIONAL' | 'ERROR';
export type WorkflowVersionStatus = 'DRAFT' | 'PUBLISHED' | 'SUPERSEDED';
export type WorkflowReleasePolicy = 'BEFORE_FIRST_EVENT';
export type WorkflowReattemptPolicy = 'REPLACEMENT_DATA';

export interface WorkflowRoleGroupInput {
  id: string;
  groupKey: string;
  name: string;
  color?: string;
  sequenceNo: number;
  description?: string;
}

export interface WorkflowStageStepInput {
  id: string;
  taskStepId: string;
  recordedSegmentId: string;
  recordedAssetVersion: number;
  sequenceNo: number;
  stepKind: WorkflowStepKind;
  completionMethod: string;
  failurePolicy: string;
  score: number;
  matcher: Record<string, unknown>;
}

export interface WorkflowStageInput {
  id: string;
  stageKey: string;
  name: string;
  sequenceNo: number;
  roleGroupId: string;
  externalRoleId: string;
  externalOrgId: string;
  launchConfig: Record<string, unknown>;
  stageMaxScore: number;
  timeLimitSeconds?: number;
  reconnectGraceSeconds: number;
  allowSameLearnerNextDefault: boolean;
  allowBatchOverride: boolean;
  releasePolicy: WorkflowReleasePolicy;
  reattemptPolicy: WorkflowReattemptPolicy;
  steps: WorkflowStageStepInput[];
}

export interface SubjectiveRubricItemInput {
  id: string;
  itemKey: string;
  name: string;
  description?: string;
  sequenceNo: number;
  maxScore: number;
  required: boolean;
}

export interface TaskWorkflowDraft {
  id: string;
  tenantId: string;
  taskId: string;
  versionNo: number;
  versionStatus: WorkflowVersionStatus;
  name: string;
  serialPolicy: 'SERIAL_ONLY';
  teachingDataTemplateId: string;
  dataScenarioVersion: string;
  dataGenerationDefaults: Record<string, unknown>;
  objectiveMaxScore: number;
  subjectiveMaxScore: number;
  lockVersion: number;
  roleGroups: WorkflowRoleGroupInput[];
  stages: WorkflowStageInput[];
  rubricItems: SubjectiveRubricItemInput[];
}

export interface TaskWorkflowVersion extends TaskWorkflowDraft {
  snapshotDigest: string;
  publishedBy: string;
  publishedTime: string;
}

export interface SaveWorkflowDraftRequest {
  clientRequestId: string;
  expectedLockVersion: number;
  name: string;
  teachingDataTemplateId: string;
  dataScenarioVersion: string;
  dataGenerationDefaults: Record<string, unknown>;
  objectiveMaxScore: number;
  subjectiveMaxScore: number;
  roleGroups: WorkflowRoleGroupInput[];
  stages: WorkflowStageInput[];
  rubricItems: SubjectiveRubricItemInput[];
}

export interface WorkflowValidationIssue {
  code: string;
  path: string;
  message: string;
}

export interface WorkflowValidationResult {
  valid: boolean;
  issues: WorkflowValidationIssue[];
}
