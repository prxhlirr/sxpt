export interface ApiResult<T> {
  success: boolean;
  code: number;
  message: string;
  result: T;
  timestamp: number;
}

export class TrainingApiError extends Error {
  constructor(
    message: string,
    public readonly code: number | string = 'REQUEST_FAILED',
    public readonly path?: string
  ) {
    super(message);
    this.name = 'TrainingApiError';
  }
}

export interface CreateCaptureSessionRequest {
  tenantId: string;
  connectorSystemId: string;
  teacherId: string;
  sessionName: string;
  startUrl: string;
  captureMode?: 'STANDARD';
}

export interface CaptureSessionVO extends CreateCaptureSessionRequest {
  id: string;
  captureSessionId?: string;
  sessionStatus: 'RUNNING' | 'FINISHED';
  status: 'ACTIVE' | 'DISABLED';
  createTime: string;
  updateTime: string;
}

export interface ConfirmSegmentSwitchRequest {
  tenantId: string;
  teacherId: string;
  connectorSystemId: string;
  captureSessionId?: string;
  dataInstanceId?: string;
  currentSegmentNo: number;
  nextSegmentNo: number;
  actorType?: string;
  requiredExternalOrgId: string;
  requiredExternalOrgName?: string;
  requiredExternalRoleId: string;
  requiredExternalRoleName?: string;
  externalBusinessId?: string;
  externalBusinessNo?: string;
  targetUrl: string;
  switchReason?: string;
  dataScopeJson?: string;
}

export interface CaptureSegmentSwitchVO extends ConfirmSegmentSwitchRequest {
  id: string;
  launchContextId: string;
  launchToken: string;
  expireTime: string;
  switchConfirmRequired: true;
  switchDecisionSource: 'TEACHER_PATH';
}

export interface ReportCaptureEventRequest {
  tenantId: string;
  captureSessionId: string;
  sdkSessionId?: string;
  clientEventId: string;
  eventType: string;
  eventTime: string;
  sequenceNo: number;
  retryCount?: number;
  pageUrl?: string;
  targetText?: string;
  targetLocator?: string;
  targetStableKey?: string;
  inputValueMasked?: string;
  eventPayloadJson?: string;
}

export interface CaptureEventVO
  extends Omit<ReportCaptureEventRequest, 'eventPayloadJson'> {
  id: string;
  createTime: string;
}

export interface ReportResourceSnapshotRequest {
  tenantId: string;
  captureSessionId: string;
  pageUrl: string;
  pageTitle?: string;
  resourceType: string;
  snapshotScope: string;
  resourceName?: string;
  resourceLocator?: string;
  elementSnapshotJson: string;
  metadataJson?: string;
  snapshotHash?: string;
  expireTime?: string;
}

export interface ResourceSnapshotVO extends ReportResourceSnapshotRequest {
  id: string;
  createTime: string;
}

export interface CreateActionDraftRequest {
  tenantId: string;
  captureSessionId: string;
  eventId?: string;
  actionName: string;
  actionType: string;
  sequenceNo: number;
  connectorResourceId?: string;
  suggestedOperationName?: string;
  confirmedOperationName?: string;
  confirmedStepName?: string;
  guideContent?: string;
  practiceHint?: string;
  confirmStatus?: 'PENDING';
}

export interface ConfirmActionDraftRequest {
  confirmedOperationName: string;
  confirmedStepName: string;
  guideContent?: string;
  practiceHint?: string;
  updateBy?: string;
}

export interface DiscardActionDraftRequest {
  updateBy?: string;
}

export interface ActionDraftVO
  extends Omit<CreateActionDraftRequest, 'confirmStatus'> {
  id: string;
  confirmStatus: 'PENDING' | 'CONFIRMED' | 'DISCARDED';
  createTime: string;
  updateTime: string;
}

export interface CreateConnectorResourceRequest {
  tenantId: string;
  connectorSystemId: string;
  resourceCode: string;
  resourceName: string;
  resourceType: string;
  pageUrl?: string;
  locator?: string;
  stableKey?: string;
  metadataJson?: string;
  sourceCaptureId?: string;
  createBy?: string;
}

export interface ConnectorResourceVO extends CreateConnectorResourceRequest {
  id: string;
  status: 'ACTIVE' | 'DISABLED';
  createTime: string;
}

export interface CreateTeachingPointRequest {
  tenantId: string;
  connectorSystemId: string;
  pointCode: string;
  pointName: string;
  pointType: 'MODULE' | 'SCENARIO' | 'OPERATION_GROUP' | string;
  sourceCaptureSessionId?: string;
  businessOverviewJson?: string;
  flowFileId?: string;
  flowFileUrl?: string;
  recordPathJson?: string;
  requiredExternalRoleId?: string;
  requiredExternalRoleName?: string;
  executionStrategy:
    | 'ROLE_SWITCH'
    | 'TRAINING_ACCOUNT'
    | 'TEMP_GRANT'
    | 'VALIDATE_ONLY'
    | string;
  defaultGrantStartTime?: string;
  defaultGrantEndTime?: string;
  dataScopeJson?: string;
  overlayPolicyJson?: string;
  description?: string;
  createBy?: string;
}

export interface TeachingPointVO extends CreateTeachingPointRequest {
  id: string;
  versionNo: number;
  pointStatus: 'PUBLISHED';
  status: 'ACTIVE' | 'DISABLED';
  createTime: string;
}

export interface CreateTaskStepRequest {
  tenantId: string;
  taskId: string;
  teachingPointId: string;
  stepCode: string;
  stepName: string;
  stepDescription?: string;
  sequenceNo: number;
  segmentNo?: number;
  actorType?: string;
  requiredExternalOrgId?: string;
  requiredExternalOrgName?: string;
  requiredExternalRoleId?: string;
  requiredExternalRoleName?: string;
  switchStrategy?: string;
  nextSegmentNo?: number;
  switchConfirmRequired?: boolean;
  switchDecisionSource?: string;
  switchReason?: string;
  rollbackPolicy?: string;
  relatedResourceIds?: string;
  guideContent?: string;
  practiceHint?: string;
  required?: boolean;
  allowSkip?: boolean;
  sourceActionDraftId?: string;
  createBy?: string;
}

export interface TaskStepVO extends CreateTaskStepRequest {
  id: string;
  status: 'ACTIVE' | 'DISABLED';
  createTime: string;
  updateTime: string;
}

export interface TrainingApi {
  createCaptureSession(request: CreateCaptureSessionRequest): Promise<CaptureSessionVO>;
  listCaptureSessions(tenantId: string, teacherId: string): Promise<CaptureSessionVO[]>;
  finishCaptureSession(id: string): Promise<CaptureSessionVO>;
  confirmSegmentSwitch(request: ConfirmSegmentSwitchRequest): Promise<CaptureSegmentSwitchVO>;
  reportCaptureEvent(request: ReportCaptureEventRequest): Promise<CaptureEventVO>;
  listCaptureEvents(tenantId: string, captureSessionId: string): Promise<CaptureEventVO[]>;
  reportResourceSnapshot(request: ReportResourceSnapshotRequest): Promise<ResourceSnapshotVO>;
  listResourceSnapshots(tenantId: string, captureSessionId: string): Promise<ResourceSnapshotVO[]>;
  createActionDraft(request: CreateActionDraftRequest): Promise<ActionDraftVO>;
  listActionDrafts(tenantId: string, captureSessionId: string): Promise<ActionDraftVO[]>;
  confirmActionDraft(id: string, request: ConfirmActionDraftRequest): Promise<ActionDraftVO>;
  discardActionDraft(id: string, request: DiscardActionDraftRequest): Promise<ActionDraftVO>;
  createConnectorResource(request: CreateConnectorResourceRequest): Promise<ConnectorResourceVO>;
  listConnectorResources(tenantId: string, connectorSystemId: string, pageUrl?: string): Promise<ConnectorResourceVO[]>;
  createTeachingPoint(request: CreateTeachingPointRequest): Promise<TeachingPointVO>;
  listTeachingPoints(tenantId: string, connectorSystemId: string): Promise<TeachingPointVO[]>;
  createTaskStep(request: CreateTaskStepRequest): Promise<TaskStepVO>;
  listTaskSteps(
    tenantId: string,
    taskId: string,
    teachingPointId: string
  ): Promise<TaskStepVO[]>;
}
