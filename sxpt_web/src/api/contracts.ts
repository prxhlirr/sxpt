export type IsoDateTime = string;

export interface ApiResult<T> {
  success: boolean;
  code: number;
  message: string;
  result: T;
  timestamp: number;
}

export interface AuthUserSummary {
  userId: string;
  tenantId: string;
  username: string;
  displayName: string;
  userType: string;
  studentNo?: string;
  employeeNo?: string;
  roles: string[];
  orgIds: string[];
}

export interface AuthLoginResponse {
  token: string;
  tokenType: string;
  expiresIn: number;
  user: AuthUserSummary;
}

export interface ConnectorSystem {
  id: string;
  tenantId: string;
  systemCode: string;
  systemName: string;
  systemType: string;
  baseUrl: string;
  authType: string;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface IdentityBinding {
  id: string;
  tenantId: string;
  userId: string;
  connectorSystemId: string;
  externalUserId: string;
  externalUsername?: string;
  bindingType: string;
  lastLoginTime?: IsoDateTime;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface PlatformLaunchContext {
  id: string;
  tenantId: string;
  launchToken: string;
  userId: string;
  connectorSystemId: string;
  taskId?: string;
  teachingPointId?: string;
  executionId?: string;
  sceneType: string;
  sdkMode: string;
  targetUrl: string;
  launchStatus: string;
  expireTime: IsoDateTime;
  createTime: IsoDateTime;
}

export interface VerifiedPlatformLaunchContext
  extends Omit<PlatformLaunchContext, 'launchToken' | 'createTime'> {
  dataInstanceId?: string;
  segmentNo?: number;
  actorType?: string;
  requiredExternalOrgId?: string;
  requiredExternalOrgName?: string;
  requiredExternalRoleId?: string;
  requiredExternalRoleName?: string;
  externalBusinessId?: string;
  externalBusinessNo?: string;
  dataScopeJson?: string;
  verifiedTime: IsoDateTime;
}

export interface TeachingDataTemplate {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  teachingPointId?: string;
  templateCode: string;
  templateName: string;
  sceneType: string;
  initState?: string;
  supportMode?: string;
  configJson?: string;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface TeachingDataInstance {
  id: string;
  tenantId: string;
  templateId: string;
  connectorSystemId: string;
  ownerUserId?: string;
  classId?: string;
  taskId?: string;
  teachingPointId?: string;
  executionId?: string;
  attemptId?: string;
  sceneType: string;
  externalBusinessId: string;
  externalBusinessNo?: string;
  externalStatus?: string;
  instanceStatus: string;
  resetCount: number;
  lockTime?: IsoDateTime;
  expireTime?: IsoDateTime;
  metadataJson?: string;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface CaptureSession {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  teacherId: string;
  sessionName: string;
  businessName?: string;
  captureMode: string;
  startUrl: string;
  startTime: IsoDateTime;
  endTime?: IsoDateTime;
  sessionStatus: string;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface CaptureEvent {
  id: string;
  tenantId: string;
  captureSessionId: string;
  sdkSessionId?: string;
  clientEventId: string;
  eventType: string;
  eventTime: IsoDateTime;
  sequenceNo: number;
  retryCount: number;
  pageUrl?: string;
  targetText?: string;
  targetLocator?: string;
  targetStableKey?: string;
  inputValueMasked?: string;
  archiveStatus: string;
  status: string;
  createTime: IsoDateTime;
}

export interface CaptureResourceSnapshot {
  id: string;
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
  archiveStatus: string;
  expireTime?: IsoDateTime;
  status: string;
  createTime: IsoDateTime;
}

export interface CaptureActionDraft {
  id: string;
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
  confirmStatus: string;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface CaptureSegmentSwitch {
  launchContextId: string;
  launchToken: string;
  tenantId: string;
  teacherId: string;
  connectorSystemId: string;
  currentSegmentNo: number;
  nextSegmentNo: number;
  actorType?: string;
  requiredExternalOrgId: string;
  requiredExternalOrgName?: string;
  requiredExternalRoleId: string;
  requiredExternalRoleName?: string;
  targetUrl: string;
  switchConfirmRequired: boolean;
  switchDecisionSource: string;
  switchReason?: string;
  expireTime: IsoDateTime;
}

export interface ConnectorResource {
  id: string;
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
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface TeachingPoint {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  pointCode: string;
  pointName: string;
  pointType: string;
  versionNo: number;
  sourceCaptureSessionId?: string;
  businessOverviewJson?: string;
  recordPathJson?: string;
  requiredExternalRoleId?: string;
  requiredExternalRoleName?: string;
  executionStrategy: string;
  dataScopeJson?: string;
  overlayPolicyJson?: string;
  description?: string;
  pointStatus: string;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface TaskStep {
  id: string;
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
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface Course {
  id: string;
  tenantId: string;
  courseCode: string;
  courseName: string;
  versionNo: number;
  targetOrgId?: string;
  startTime?: IsoDateTime;
  endTime?: IsoDateTime;
  description?: string;
  courseStatus: string;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface TeachingTask {
  id: string;
  tenantId: string;
  courseId: string;
  publishOrgId: string;
  taskCode: string;
  taskName: string;
  versionNo: number;
  taskType: string;
  taskGoal: string;
  taskDescription?: string;
  startTime?: IsoDateTime;
  endTime?: IsoDateTime;
  timeLimitMinutes?: number;
  overlayPolicyJson?: string;
  taskStatus: string;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface TaskTeachingPoint {
  id: string;
  tenantId: string;
  taskId: string;
  teachingPointId: string;
  requiredFlag: boolean;
  sequenceNo: number;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface EvaluationRule {
  id: string;
  tenantId: string;
  ruleCode: string;
  ruleName: string;
  versionNo: number;
  taskId?: string;
  teachingPointId?: string;
  totalScore: number;
  description?: string;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface EvaluationItem {
  id: string;
  tenantId: string;
  evaluationRuleId: string;
  teachingPointId?: string;
  itemCode: string;
  itemName: string;
  itemType: string;
  relatedResourceId?: string;
  relatedApiResourceId?: string;
  relatedTaskStepId?: string;
  score: number;
  required: boolean;
  assertionType: string;
  assertionConfigJson: string;
  failPolicy: string;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface EvaluationResult {
  id: string;
  tenantId: string;
  executionId: string;
  evaluationRuleId: string;
  autoScore: number;
  manualScore?: number;
  finalScore: number;
  evaluationSummary?: string;
  evidenceJson?: string;
  reviewedBy?: string;
  reviewedTime?: IsoDateTime;
  reviewStatus?: string;
  reviewReason?: string;
  evaluationStatus: string;
  status: string;
  createTime: IsoDateTime;
}

export interface TaskExecution {
  id: string;
  tenantId: string;
  taskId: string;
  studentId: string;
  connectorSystemId: string;
  executionMode: string;
  sdkMode: string;
  executionIdentityStatus: string;
  startTime: IsoDateTime;
  endTime?: IsoDateTime;
  executionStatus: string;
  score?: number;
  resultSummary?: string;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface ExecutionTrace {
  id: string;
  tenantId: string;
  executionId: string;
  sdkSessionId?: string;
  clientTraceId: string;
  taskStepId?: string;
  teachingPointId?: string;
  resourceId?: string;
  traceType: string;
  traceTime: IsoDateTime;
  sequenceNo: number;
  retryCount: number;
  success: boolean;
  errorMessage?: string;
  archiveStatus: string;
  status: string;
  createTime: IsoDateTime;
}

export interface RuntimeContext {
  contextId: string;
  tenantId: string;
  executionId: string;
  taskId: string;
  studentId: string;
  sdkMode: string;
  contextJson: string;
  overlayPolicyJson?: string;
  teachingPointSnapshotJson: string;
  resourceSnapshotJson: string;
  evaluationSnapshotJson: string;
  archiveStatus: string;
  expireTime?: IsoDateTime;
}
