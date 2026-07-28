export type WorkflowRunType = 'PRACTICE' | 'EXAM';
export type WorkflowRunStatus =
  | 'DRAFT' | 'DATA_PREPARING' | 'READY' | 'SCHEDULED' | 'RUNNING' | 'CANCELLED';
export type RunDataItemStatus =
  | 'GENERATING' | 'READY' | 'IN_USE' | 'RETIRED'
  | 'GENERATION_FAILED' | 'QUARANTINED' | 'DISABLED';

export interface RunBatchRoleGroup { id: string; groupKey: string; name: string; color?: string; sequenceNo: number; }
export interface RunBatchStage { id: string; stageKey: string; name: string; sequenceNo: number; roleGroupId: string; externalRoleId: string; externalOrgId: string; }
export interface RunBatchRubricItem { id: string; itemKey: string; name: string; sequenceNo: number; maxScore: number; required: boolean; }
export interface PublishedWorkflowOption {
  id: string;
  taskId: string;
  name: string;
  versionNo: number;
  snapshotDigest: string;
  teachingDataTemplateId: string;
  scenarioVersion: string;
  objectiveMaxScore: number;
  subjectiveMaxScore: number;
  roleGroups: RunBatchRoleGroup[];
  stages: RunBatchStage[];
  rubricItems: RunBatchRubricItem[];
}

export interface RunBatchGroupMemberInput { learnerId: string; groupKey: string; unitId: string; externalAccountMapping: Record<string, string>; }
export interface UnitDataPlanInput { unitId: string; formalCount: number; spareCount: number; teachingDataTemplateId: string; scenarioVersion: string; generationParameters: Record<string, unknown>; }
export interface CreateRunBatchRequest { clientRequestId: string; workflowVersionId: string; runType: WorkflowRunType; batchName: string; plannedStartTime?: string; plannedEndTime?: string; }
export interface SaveGroupMembersRequest { clientRequestId: string; members: RunBatchGroupMemberInput[]; }
export interface SaveUnitPlansRequest { clientRequestId: string; unitPlans: UnitDataPlanInput[]; }
export interface PrepareRunDataRequest { clientRequestId: string; unitPlans: UnitDataPlanInput[]; }
export interface ReasonedCommand { clientRequestId: string; reason: string; }
export interface ReplaceRunDataRequest extends ReasonedCommand { generationParameters: Record<string, unknown>; }

export interface RunMemberStageQuota { unitId: string; groupKey: string; memberCount: number; requiredFormalCount: number; }
export interface RunAuditEvent { sequenceNo: number; eventType: string; aggregateType: 'BATCH' | 'DATA_JOB' | 'DATA_ITEM'; aggregateId: string; clientRequestId: string; reason?: string; occurredAt: string; }
export interface WorkflowRunBatch {
  id: string; workflowVersionId: string; workflowSnapshotDigest: string; batchName: string; runType: WorkflowRunType; status: WorkflowRunStatus;
  plannedStartTime?: string; plannedEndTime?: string; objectiveMaxScore: number; subjectiveMaxScore: number;
  roleGroups: RunBatchRoleGroup[]; stages: RunBatchStage[]; rubricItems: RunBatchRubricItem[];
  members: RunBatchGroupMemberInput[]; unitPlans: UnitDataPlanInput[]; latestGenerationJobId?: string; auditHistory: RunAuditEvent[]; createdAt: string; updatedAt: string;
}
export interface RunPreflightIssue { code: string; message: string; unitId?: string; stageId?: string; groupKey?: string; }
export interface RunBatchPreflight { valid: boolean; issues: RunPreflightIssue[]; memberStageQuotas: RunMemberStageQuota[]; checkedAt: string; }
export interface RunDataGenerationJob { id: string; batchId: string; clientRequestId: string; status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'COMPLETED_WITH_FAILURES'; requestedCount: number; readyCount: number; failedCount: number; createdAt: string; updatedAt: string; }
export interface RunDataItem {
  id: string; batchId: string; unitId: string; kind: 'FORMAL' | 'SPARE'; status: RunDataItemStatus; generationRevision: number;
  businessReferenceMasked: string; generationParameters: Record<string, unknown>; replacementOfItemId?: string; generationJobId: string;
  auditHistory: RunAuditEvent[]; createdAt: string; updatedAt: string;
}
export interface RunDataFilter { page?: number; pageSize?: number; unitId?: string; status?: RunDataItemStatus; kind?: 'FORMAL' | 'SPARE'; }
export interface PagedRunDataItems { items: RunDataItem[]; page: number; pageSize: number; total: number; }
