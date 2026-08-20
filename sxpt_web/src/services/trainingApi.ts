import {
  createDefaultBusinessPlatforms,
  createMockTrainingState
} from '../data/mockSeed';
import { clearApiToken, setApiToken } from '../api/http';
import type {
  DataPrepareMode,
  PortalRole,
  StorageLike,
  TrainingState
} from '../domain/models';

export const TRAINING_STORAGE_KEY = 'sxpt_web.training.demo.v1';
export const AUTH_SESSION_STORAGE_KEY = 'sxpt_web.auth.session.v1';
export const API_BASE_URL = normalizeApiBaseUrl(
  import.meta.env.VITE_API_BASE_URL ?? 'http://127.0.0.1:8080/'
);

export interface TrainingApi {
  loadState(): TrainingState;
  saveState(state: TrainingState): TrainingState;
  resetDemo(): TrainingState;
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

export interface AuthSession {
  token: string;
  tokenType: string;
  expiresIn: number;
  issuedAt: number;
  user: AuthUserSummary;
}

export interface LoginRequest {
  tenantId: string;
  username: string;
  password: string;
  loginType: 'PASSWORD';
}

const DEVELOPMENT_LOGIN_PROFILES: Record<
  PortalRole,
  { tenantId: string; username: string; password: string }
> = {
  admin: {
    tenantId: 'demo-tenant',
    username: 'teacher02',
    password: 'Sxpt@123456'
  },
  teacher: {
    tenantId: 'demo-tenant',
    username: 'teacher01',
    password: 'Sxpt@123456'
  },
  student: {
    tenantId: 'demo-tenant',
    username: 'student01',
    password: 'Sxpt@123456'
  }
};

export interface DataRequirement {
  id: string;
  tenantId: string;
  requirementCode: string;
  connectorSystemId: string;
  businessModuleId?: string;
  moduleCode: string;
  strategyId?: string;
  templateId?: string;
  taskId: string;
  classId?: string;
  sceneType: string;
  requirementStatus?: string;
  expectedCount?: number;
  successCount?: number;
  failedCount?: number;
  createBy?: string;
  updateBy?: string;
  createTime?: string;
  updateTime?: string;
}

export interface ConnectorSystem {
  id: string;
  tenantId: string;
  systemCode: string;
  systemName: string;
  systemType: string;
  environmentType?: string;
  environmentGroupCode?: string;
  baseUrl: string;
  authType: string;
  configJson?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface CreateConnectorSystemRequest {
  tenantId: string;
  systemCode: string;
  systemName: string;
  systemType: string;
  environmentType?: string;
  environmentGroupCode?: string;
  baseUrl: string;
  authType: string;
  configJson?: string;
}

export interface UpdateConnectorSystemRequest {
  id: string;
  systemName: string;
  systemType: string;
  environmentType?: string;
  environmentGroupCode?: string;
  baseUrl: string;
  authType: string;
  configJson?: string;
}

export interface PlatformCapability {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  capabilityCode: string;
  capabilityName: string;
  capabilityType: string;
  supportFlag: boolean;
  endpointUrl: string;
  method: string;
  requestSchemaJson?: string;
  responseSchemaJson?: string;
  timeoutMs?: number;
  retryPolicyJson?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface PlatformCapabilityRequest {
  tenantId: string;
  connectorSystemId: string;
  capabilityCode: string;
  capabilityName: string;
  capabilityType: string;
  supportFlag: boolean;
  endpointUrl: string;
  method: string;
  requestSchemaJson?: string;
  responseSchemaJson?: string;
  timeoutMs?: number;
  retryPolicyJson?: string;
  createBy?: string;
  updateBy?: string;
}

export interface DataPrepareMetadataOption {
  code: string;
  label: string;
  visible: boolean;
}

export interface DataPrepareMetadata {
  authTypes: DataPrepareMetadataOption[];
  capabilities: DataPrepareMetadataOption[];
  sceneTypes: DataPrepareMetadataOption[];
  templateUsages?: DataPrepareMetadataOption[];
  platformTypes?: DataPrepareMetadataOption[];
  environmentTypes?: DataPrepareMetadataOption[];
  dataSourceStrategies: DataPrepareMetadataOption[];
  prepareTimings: DataPrepareMetadataOption[];
  sharePolicies: DataPrepareMetadataOption[];
  regeneratePolicies: DataPrepareMetadataOption[];
  lockPolicies: DataPrepareMetadataOption[];
  recordStatuses: DataPrepareMetadataOption[];
  platformDefaults: {
    systemType: string;
    authType: string;
    authConfigJson: string;
    capabilityCode: string;
    capabilityName: string;
    capabilityType: string;
    capabilityMethod: string;
    capabilityTimeoutMs: number;
  };
  moduleDefaults: {
    initialStatus: string;
    targetStatus: string;
    capabilityCodesJson: string;
    processStepCode: string;
    processStepType: string;
  };
  templateDefaults: {
    sceneType: string;
    initState: string;
    supportMode: string;
    requiredStatus: string;
  };
  strategyDefaults: {
    sceneType: string;
    dataSourceStrategy: string;
    initExternalStatus: string;
    targetExternalStatus: string;
    prepareTiming: string;
    sharePolicy: string;
    regeneratePolicy: string;
    lockPolicy: string;
    validationPolicyJson: string;
  };
}

export interface OriginRole {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  roleCode: string;
  roleName: string;
  externalRoleId?: string;
  roleType?: string;
  remark?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface OriginRoleRequest {
  tenantId: string;
  connectorSystemId: string;
  roleCode: string;
  roleName: string;
  externalRoleId?: string;
  roleType?: string;
  remark?: string;
  createBy?: string;
  updateBy?: string;
}

export interface OriginOrg {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  orgCode: string;
  orgName: string;
  externalOrgId?: string;
  parentExternalOrgId?: string;
  orgType?: string;
  remark?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface OriginOrgRequest {
  tenantId: string;
  connectorSystemId: string;
  orgCode: string;
  orgName: string;
  externalOrgId?: string;
  parentExternalOrgId?: string;
  orgType?: string;
  remark?: string;
  createBy?: string;
  updateBy?: string;
}

export interface TeachingDataTemplate {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  teachingPointId?: string;
  templateCode: string;
  templateName: string;
  sceneType: string;
  moduleCode?: string;
  strategyId?: string;
  initState?: string;
  supportMode?: string;
  templateUsage?: string;
  configJson?: string;
  dataSchemaJson?: string;
  mockRuleJson?: string;
  readonlyFlag?: boolean;
  requestSchemaJson?: string;
  requiredOrgRoleJson?: string;
  resultCheckSchemaJson?: string;
  sensitiveFieldPolicyJson?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface TeachingDataTemplateRequest {
  templateName: string;
  sceneType: string;
  moduleCode: string;
  teachingPointId?: string;
  strategyId?: string;
  initState?: string;
  supportMode?: string;
  templateUsage?: string;
  configJson?: string;
  dataSchemaJson?: string;
  mockRuleJson?: string;
  readonlyFlag?: boolean;
  requestSchemaJson?: string;
  requiredOrgRoleJson?: string;
  resultCheckSchemaJson?: string;
  sensitiveFieldPolicyJson?: string;
  updateBy?: string;
}

export interface CreateTeachingDataTemplateRequest {
  tenantId: string;
  connectorSystemId: string;
  templateCode: string;
  templateName: string;
  sceneType: string;
  moduleCode: string;
  teachingPointId?: string;
  strategyId?: string;
  initState?: string;
  supportMode?: string;
  templateUsage?: string;
  configJson?: string;
  dataSchemaJson?: string;
  mockRuleJson?: string;
  readonlyFlag?: boolean;
  requestSchemaJson?: string;
  requiredOrgRoleJson?: string;
  resultCheckSchemaJson?: string;
  sensitiveFieldPolicyJson?: string;
  updateBy?: string;
}

export interface BusinessModule {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  moduleCode: string;
  moduleName: string;
  externalModuleId?: string;
  entryUrl: string;
  moduleType?: string;
  supportScenes?: string;
  needPreData?: boolean;
  defaultInitialStatus?: string;
  defaultTargetStatus?: string;
  capabilityCodesJson?: string;
  defaultTemplateId?: string;
  remark?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface BusinessModuleRequest {
  tenantId?: string;
  connectorSystemId?: string;
  moduleCode?: string;
  moduleName?: string;
  externalModuleId?: string;
  entryUrl?: string;
  moduleType?: string;
  supportScenes?: string;
  needPreData?: boolean;
  defaultInitialStatus?: string;
  defaultTargetStatus?: string;
  capabilityCodesJson?: string;
  defaultTemplateId?: string;
  remark?: string;
  updateBy?: string;
}

export interface BusinessModuleProcessStep {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  businessModuleId: string;
  moduleCode: string;
  stepNo: number;
  stepCode: string;
  stepName: string;
  stepType?: string;
  initExternalStatus?: string;
  targetExternalStatus?: string;
  completionRuleJson?: string;
  remark?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface BusinessModuleProcessStepRequest {
  tenantId?: string;
  connectorSystemId?: string;
  businessModuleId?: string;
  moduleCode?: string;
  stepNo?: number;
  stepCode?: string;
  stepName?: string;
  stepType?: string;
  initExternalStatus?: string;
  targetExternalStatus?: string;
  completionRuleJson?: string;
  remark?: string;
  createBy?: string;
  updateBy?: string;
}

export interface BusinessModuleProcessActor {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  businessModuleId: string;
  processStepId: string;
  moduleCode: string;
  stepCode: string;
  actorNo: number;
  actorRelation: string;
  actorType: string;
  requiredOrgType?: string;
  requiredOrgCode?: string;
  requiredOrgName?: string;
  requiredRoleCode?: string;
  requiredRoleName?: string;
  isRequired?: boolean;
  assignmentRule?: string;
  remark?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface BusinessModuleProcessActorRequest {
  tenantId?: string;
  processStepId?: string;
  actorNo?: number;
  actorRelation?: string;
  actorType?: string;
  requiredOrgType?: string;
  requiredOrgCode?: string;
  requiredOrgName?: string;
  requiredRoleCode?: string;
  requiredRoleName?: string;
  isRequired?: boolean;
  assignmentRule?: string;
  remark?: string;
  createBy?: string;
  updateBy?: string;
}

export interface ModuleDataStrategy {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  businessModuleId: string;
  moduleCode: string;
  moduleName: string;
  sceneType: string;
  templateId: string;
  dataSourceStrategy: string;
  initExternalStatus?: string;
  targetExternalStatus?: string;
  sharePolicy: string;
  regeneratePolicy: string;
  lockPolicy: string;
  strategyCode?: string;
  prepareTiming?: string;
  validationPolicyJson?: string;
  archivePolicyJson?: string;
  expirePolicyJson?: string;
  resultCheckPolicyJson?: string;
  poolSizePolicyJson?: string;
  defaultOrgRolePolicyJson?: string;
  needPreData?: boolean;
  strategyVersion?: number;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface ModuleDataStrategyRequest {
  tenantId?: string;
  connectorSystemId?: string;
  businessModuleId?: string;
  moduleCode?: string;
  moduleName?: string;
  sceneType?: string;
  needPreData?: boolean;
  dataSourceStrategy?: string;
  initExternalStatus?: string;
  targetExternalStatus?: string;
  defaultOrgRolePolicyJson?: string;
  sharePolicy?: string;
  regeneratePolicy?: string;
  lockPolicy?: string;
  expirePolicyJson?: string;
  resultCheckPolicyJson?: string;
  strategyCode?: string;
  templateId?: string;
  prepareTiming?: string;
  poolSizePolicyJson?: string;
  validationPolicyJson?: string;
  archivePolicyJson?: string;
  createBy?: string;
  updateBy?: string;
}

export interface DataRequirementItem {
  id: string;
  tenantId: string;
  requirementId: string;
  requestBatchId: string;
  requestItemId: string;
  connectorSystemId: string;
  moduleCode: string;
  sceneType: string;
  taskId: string;
  studentId: string;
  questionId?: string;
  questionAttemptId?: string;
  actorType: string;
  ownerExternalOrgId: string;
  requiredExternalOrgId: string;
  requiredExternalRoleId: string;
  itemStatus?: string;
  externalBusinessId?: string;
  externalBusinessNo?: string;
  externalBusinessName?: string;
  externalStatus?: string;
  currentStepCode?: string;
  currentActorNo?: number;
  currentOrgId?: string;
  currentOrgName?: string;
  currentRoleId?: string;
  currentRoleName?: string;
  processChainSnapshotJson?: string;
  validationStatus?: string;
  failureReason?: string;
}

export interface DataPrepareJob {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  taskId?: string;
  moduleCode: string;
  sceneType: string;
  jobType: string;
  expectedCount?: number;
  successCount?: number;
  failedCount?: number;
  jobStatus?: string;
  idempotencyKey: string;
  requestBatchId: string;
  externalRequestId?: string;
  errorMessage?: string;
  requestJson?: string;
  resultJson?: string;
  retryCount?: number;
  nextRetryTime?: string;
  triggerType?: string;
  traceId?: string;
  startTime?: string;
  endTime?: string;
  createTime?: string;
  updateTime?: string;
}

export interface RetryFailedJobRequest {
  tenantId: string;
  updateBy?: string;
}

export interface TeachingDataPool {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  moduleCode: string;
  taskId: string;
  sceneType: string;
  questionId?: string;
  templateId?: string;
  strategyId?: string;
  requirementId: string;
  poolStatus?: string;
  totalCount?: number;
  readyCount?: number;
  allocatedCount?: number;
  failedCount?: number;
  idempotencyKey?: string;
}

export interface DataInstanceAllocation {
  id: string;
  tenantId: string;
  poolId: string;
  dataInstanceId: string;
  taskId: string;
  executionId?: string;
  attemptId?: string;
  questionAttemptId?: string;
  ownerUserId: string;
  allocationScene: string;
  allocationStatus?: string;
  allocateTime?: string;
  requestBatchId?: string;
  requestItemId?: string;
  studentId?: string;
  studentName?: string;
  classId?: string;
  connectorSystemId?: string;
  businessModuleId?: string;
  externalBusinessId?: string;
  externalBusinessName?: string;
  targetUrl?: string;
  processStepCode?: string;
  processStepName?: string;
  processActorNo?: number;
  actorRelation?: string;
  originOrgId?: string;
  originOrgName?: string;
  originRoleId?: string;
  originRoleName?: string;
  allocationLockStatus?: string;
  actorSnapshotJson?: string;
  requiredExternalOrgId?: string;
  requiredExternalOrgName?: string;
  requiredExternalRoleId?: string;
  requiredExternalRoleName?: string;
  actorType?: string;
}

export interface CreateStudentDataLaunchRequest {
  tenantId: string;
  allocationId: string;
  studentId: string;
  executionId?: string;
}

export interface CreateStudentTaskLaunchRequest {
  taskId: string;
  sceneType: string;
  executionId?: string;
  sourceAllocationId?: string;
}

export interface StudentDataLaunchResult {
  tenantId: string;
  launchContextId: string;
  launchToken: string;
  dataInstanceId: string;
  launchUrl: string;
  targetUrl: string;
  expireTime?: string;
  allocation?: DataInstanceAllocation;
}

export interface CreateAuthoringLaunchRequest {
  lessonId: string;
  connectorSystemId: string;
  businessModuleId: string;
  generationSource?: DataPrepareMode;
}

export interface AuthoringLaunchResult {
  tenantId: string;
  launchContextId: string;
  launchToken: string;
  dataInstanceId: string;
  redirectUrl: string;
  launchUrl: string;
  expireTime?: string;
}

export interface ClassicCaseAsset {
  id: string;
  tenantId: string;
  caseCode: string;
  caseTitle: string;
  caseSummary?: string;
  sourceConnectorSystemId: string;
  learningConnectorSystemId: string;
  environmentGroupCode?: string;
  businessModuleId: string;
  moduleCode: string;
  teachingPointId?: string;
  sceneTypesJson?: string;
  tagsJson?: string;
  currentVersionId?: string;
  sourceUpdatedAt?: string;
  disableReason?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface ExternalCredential {
  id: string;
  tenantId: string;
  connectorSystemId: string;
  credentialName: string;
  apiKeyPrefix: string;
  apiKey?: string;
  lastUsedTime?: string;
  expireTime?: string;
  status?: string;
  createTime?: string;
  updateTime?: string;
}

export interface ClassicCaseVersion {
  id: string;
  caseAssetId: string;
  versionNo: number;
  caseVersionId?: string;
  payloadSchemaVersion?: string;
  supportedGenerationModesJson?: string;
  payloadHash?: string;
  contentHash?: string;
  identityBindingJson?: string;
  desensitizedCasePayloadJson?: string;
  caseDataFormatJson?: string;
  status?: string;
  createBy?: string;
  createTime?: string;
}

export interface LessonPlanClassicCaseOption {
  classicCaseId: string;
  connectorSystemId: string;
  learningConnectorSystemId: string;
  caseCode: string;
  caseName: string;
  businessModuleCode: string;
  summary?: string;
  tags: string[];
  caseVersionId: string;
  versionNo: number;
  payloadSchemaVersion: string;
  supportedGenerationModes: Array<'REPLAY_CASE' | 'FORMAT_DEMO'>;
  defaultGenerationMode: 'REPLAY_CASE' | 'FORMAT_DEMO';
}

export interface ClassicCaseConfigValidationRequest {
  businessModuleCode: string;
  connectorSystemId: string;
  classicCaseId: string;
  caseVersionId: string;
  generationMode: 'REPLAY_CASE' | 'FORMAT_DEMO';
}

export interface ClassicCaseUsage {
  id: string;
  caseAssetId: string;
  caseVersionId: string;
  usageScene: string;
  sceneType?: string;
  requestBatchId?: string;
  requestItemId?: string;
  ownerUserId?: string;
  generatedInstanceId?: string;
  teachingDataInstanceId?: string;
  externalBusinessNo?: string;
  externalBusinessName?: string;
  externalStatus?: string;
  targetUrl?: string;
  status?: string;
  useTime?: string;
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
  sceneType?: string;
  sdkMode?: string;
  targetUrl?: string;
  launchStatus?: string;
  expireTime?: string;
  createTime?: string;
}

export interface ClassicCaseGenerateLaunchRequest {
  tenantId: string;
  caseAssetId: string;
  caseVersionId?: string;
  usageScene: 'TEACHING_REPLICA' | 'STUDENT_DEMO' | string;
  sceneType?: string;
  taskId?: string;
  ownerUserId: string;
  questionId?: string;
  requestBatchId?: string;
  requestItemId?: string;
  traceId?: string;
  participantContextJson?: string;
  requiredExternalOrgId?: string;
  requiredExternalRoleId?: string;
  actorType?: string;
  sdkMode?: string;
  segmentNo?: number;
}

export interface ClassicCaseLaunchResult {
  usage: ClassicCaseUsage;
  launchContext: PlatformLaunchContext;
}

export interface DataPrepareParticipant {
  studentId: string;
  questionId: string;
  questionAttemptId: string;
  actorType: string;
  ownerExternalOrgId: string;
  requiredExternalOrgId: string;
  requiredExternalRoleId: string;
  dataScopeJson: string;
  requiredActionsJson: string;
  scorePointSnapshotJson: string;
}

export interface PrepareAndExecuteRequest {
  jobType?: string;
  triggerType: string;
  idempotencyKey?: string;
  requestJson?: string;
  generateRequest: {
    requirementId: string;
    requestBatchId: string;
    createBy: string;
    updateBy: string;
    participants: DataPrepareParticipant[];
  };
}

export interface TriggerTaskPrepareRequest {
  requirementCode?: string;
  connectorSystemId: string;
  businessModuleId: string;
  moduleCode: string;
  strategyId?: string;
  templateId?: string;
  classId?: string;
  sceneType: string;
  requestBatchId?: string;
  triggerType?: string;
  idempotencyKey?: string;
  traceId?: string;
  requestJson?: string;
  remark?: string;
  participants: DataPrepareParticipant[];
}

export interface TaskPrepareTriggerResult {
  requirement: DataRequirement;
  job: DataPrepareJob;
  requestBatchId: string;
}

export interface DataPreparePreflightCheck {
  nodeCode: string;
  status: 'PASS' | 'WARN' | 'FAIL' | string;
  message: string;
  evidence?: Record<string, string>;
}

export interface DataPreparePreflightResult {
  taskId: string;
  checks: DataPreparePreflightCheck[];
  ready: boolean;
  passCount: number;
  warnCount: number;
  failCount: number;
  summary: string;
}

interface ApiResult<T> {
  success: boolean;
  code: number;
  message: string;
  result: T;
  timestamp: number;
}

export const DATA_PREPARE_CONFIG_INCOMPLETE_CODE = 422;

export class TrainingApiRequestError extends Error {
  constructor(
    message: string,
    public readonly code?: number,
    public readonly httpStatus?: number
  ) {
    super(message);
    this.name = 'TrainingApiRequestError';
  }
}

export function isDataPrepareConfigIncompleteError(error: unknown): boolean {
  return error instanceof TrainingApiRequestError &&
    error.code === DATA_PREPARE_CONFIG_INCOMPLETE_CODE;
}

export function createTrainingApi(
  storage: StorageLike = resolveBrowserStorage(),
  seedFactory: () => TrainingState = createMockTrainingState
): TrainingApi {
  const persist = (state: TrainingState) => {
    const snapshot = clone(state);
    try {
      storage.setItem(TRAINING_STORAGE_KEY, JSON.stringify(snapshot));
    } catch {
      // Storage can be unavailable in privacy mode or embedded browsers.
    }
    return clone(snapshot);
  };

  const recover = () => persist(seedFactory());

  return {
    loadState() {
      let raw: string | null = null;
      try {
        raw = storage.getItem(TRAINING_STORAGE_KEY);
      } catch {
        return clone(seedFactory());
      }
      if (!raw) {
        return recover();
      }

      try {
        const parsed: unknown = JSON.parse(raw);
        if (!isTrainingState(parsed)) {
          return recover();
        }
        return normalizeState(parsed);
      } catch {
        return recover();
      }
    },

    saveState(state) {
      return persist(state);
    },

    resetDemo() {
      try {
        storage.removeItem(TRAINING_STORAGE_KEY);
      } catch {
        // The in-memory reactive state can still be reset.
      }
      return recover();
    }
  };
}

export function createMemoryStorage(
  initialEntries: Record<string, string> = {}
): StorageLike {
  const values = new Map(Object.entries(initialEntries));
  return {
    getItem(key) {
      return values.get(key) ?? null;
    },
    setItem(key, value) {
      values.set(key, value);
    },
    removeItem(key) {
      values.delete(key);
    }
  };
}

/**
 * 登录认证 API。
 *
 * 业务功能：
 * 1. 统一封装前端登录、会话恢复和退出逻辑。
 * 2. 保证后续教师、学生、后台入口只信任服务端签发的 token。
 *
 * 关键流程：
 * 1. 登录时调用 `/api/v1/auth/login`，成功后保存 token 和用户摘要。
 * 2. 路由守卫从本地会话读取身份，不从页面参数推断角色。
 * 3. 退出时清理会话，让所有受保护路由重新回到登录页。
 */
export const authApi = {
  async login(request: LoginRequest): Promise<AuthSession> {
    const response = await fetch(`${API_BASE_URL}api/v1/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request)
    });
    const payload = (await response.json()) as ApiResult<AuthSession>;
    if (!response.ok || !payload.success) {
      throw new Error(payload.message || '登录失败，请检查账号和密码');
    }
    const session = normalizeSession(payload.result, Date.now());
    saveAuthSession(session);
    return session;
  },

  getSession(): AuthSession | null {
    return loadAuthSession();
  },

  logout() {
    clearAuthSession();
  },

  getHomePath(session: AuthSession | null = loadAuthSession()): string {
    const role = resolvePortalRole(session);
    if (role === 'student') return '/student/tasks';
    if (role === 'teacher') return '/teacher/dashboard';
    return '/admin/overview';
  },

  getPortalRole(session: AuthSession | null = loadAuthSession()): PortalRole {
    return resolvePortalRole(session);
  },

  canAccess(
    requiredRole?: PortalRole | PortalRole[],
    session: AuthSession | null = loadAuthSession()
  ): boolean {
    if (!requiredRole) return true;
    const roles = Array.isArray(requiredRole) ? requiredRole : [requiredRole];
    return roles.some((role) => hasPortalRole(session, role));
  },

  /**
   * 开发环境快捷入口需要同步路由守卫读取的会话身份。
   *
   * 业务功能：
   * 1. 仅服务本地联调页面的管理端、教师端、学生端快速切换。
   * 2. 避免只修改前端 store 后，被基于 session 的路由守卫再次拦截。
   *
   * 关键流程：
   * 1. 根据目标门户角色构造本地开发会话。
   * 2. 写入与真实登录相同的 session 存储键，让后续守卫和 API 请求读取同一身份源。
   */
  async useDevelopmentSession(role: PortalRole): Promise<AuthSession> {
    if (!import.meta.env.DEV) {
      throw new Error('生产环境不允许使用开发快捷登录');
    }
    const profile = DEVELOPMENT_LOGIN_PROFILES[role];
    return authApi.login({
      loginType: 'PASSWORD',
      tenantId: profile.tenantId,
      username: profile.username,
      password: profile.password
    });
  },

  cleanupInvalidSession() {
    loadAuthSession();
  }
};

/**
 * 登录用户工作区 API。
 *
 * 教师/专家保存完整教学编排，学生只加载和回写后端按当前登录人裁剪后的
 * 学习、练习与考试进度。接口地址由登录身份决定，页面无需自行拼接。
 */
export const trainingWorkspaceApi = {
  async load(session: AuthSession): Promise<TrainingState | null> {
    const result = await requestApi<unknown | null>(
      resolveTrainingWorkspacePath(session)
    );
    if (result === null) return null;
    return parseTrainingWorkspaceState(result, session);
  },

  async save(
    session: AuthSession,
    state: TrainingState
  ): Promise<TrainingState> {
    const result = await requestApi<unknown>(resolveTrainingWorkspacePath(session), {
      method: 'PUT',
      body: JSON.stringify(state)
    });
    return parseTrainingWorkspaceState(result, session);
  }
};

/**
 * 数据准备后台 API。
 *
 * 业务功能：
 * 1. 封装数据准备批次、明细、任务和触发接口，服务后台最小交互闭环。
 * 2. 所有请求自动携带登录 token，让页面只关心业务参数。
 *
 * 关键流程：
 * 1. 创建或查询批次。
 * 2. 触发数据准备。
 * 3. 刷新任务和明细状态，验证幂等和原平台返回字段。
 */
export const dataPrepareApi = {
  async getDataPrepareMetadata(): Promise<DataPrepareMetadata> {
    return requestApi<DataPrepareMetadata>('api/v1/connector/data-prepare-metadata');
  },

  async listConnectorSystems(tenantId: string): Promise<ConnectorSystem[]> {
    return requestApi<ConnectorSystem[]>(
      `api/v1/connector/system/list?${stringifyQuery({ tenantId })}`
    );
  },

  async createConnectorSystem(
    request: CreateConnectorSystemRequest
  ): Promise<ConnectorSystem> {
    return requestApi<ConnectorSystem>('api/v1/connector/system/create', {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  async getConnectorSystem(id: string): Promise<ConnectorSystem> {
    return requestApi<ConnectorSystem>(
      `api/v1/connector/system/${encodeURIComponent(id)}`
    );
  },

  async updateConnectorSystem(
    request: UpdateConnectorSystemRequest
  ): Promise<ConnectorSystem> {
    return requestApi<ConnectorSystem>('api/v1/connector/system/update', {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  async createLocalConnectorSystem(tenantId: string): Promise<ConnectorSystem> {
    const metadata = await this.getDataPrepareMetadata();
    return this.createConnectorSystem({
      tenantId,
      systemCode: `LOCAL_ORIGIN_${Date.now()}`,
      systemName: '本地联调原平台',
      systemType: metadata.platformDefaults.systemType,
      environmentType: 'LEARNING',
      environmentGroupCode: 'LOCAL_DEV',
      baseUrl: 'http://127.0.0.1:8080/local-origin',
      authType: metadata.platformDefaults.authType,
      configJson: metadata.platformDefaults.authConfigJson
    });
  },

  async enableConnectorSystem(id: string): Promise<ConnectorSystem> {
    return requestApi<ConnectorSystem>(
      `api/v1/connector/system/${encodeURIComponent(id)}/enable`,
      { method: 'POST' }
    );
  },

  async disableConnectorSystem(id: string): Promise<ConnectorSystem> {
    return requestApi<ConnectorSystem>(
      `api/v1/connector/system/${encodeURIComponent(id)}/disable`,
      { method: 'POST' }
    );
  },

  async getExternalApiKey(connectorSystemId: string): Promise<ExternalCredential | null> {
    return requestApi<ExternalCredential | null>(
      `api/v1/connector/system/${encodeURIComponent(connectorSystemId)}/external-api-key`
    );
  },

  async generateExternalApiKey(
    connectorSystemId: string,
    operator?: string
  ): Promise<ExternalCredential> {
    const query = stringifyQuery({ operator });
    return requestApi<ExternalCredential>(
      `api/v1/connector/system/${encodeURIComponent(connectorSystemId)}/external-api-key/generate${query ? `?${query}` : ''}`,
      { method: 'POST' }
    );
  },

  async listPlatformCapabilities(params: {
    tenantId: string;
    connectorSystemId: string;
  }): Promise<PlatformCapability[]> {
    return requestApi<PlatformCapability[]>(
      `api/v1/connector/platform-capabilities?${stringifyQuery(params)}`
    );
  },

  async createPlatformCapability(
    request: PlatformCapabilityRequest
  ): Promise<PlatformCapability> {
    return requestApi<PlatformCapability>('api/v1/connector/platform-capabilities/create', {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  async getPlatformCapability(id: string): Promise<PlatformCapability> {
    return requestApi<PlatformCapability>(
      `api/v1/connector/platform-capabilities/${encodeURIComponent(id)}`
    );
  },

  async updatePlatformCapability(
    id: string,
    request: PlatformCapabilityRequest
  ): Promise<PlatformCapability> {
    return requestApi<PlatformCapability>(
      `api/v1/connector/platform-capabilities/${encodeURIComponent(id)}/update`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async enablePlatformCapability(id: string): Promise<PlatformCapability> {
    return requestApi<PlatformCapability>(
      `api/v1/connector/platform-capabilities/${encodeURIComponent(id)}/enable`,
      { method: 'POST' }
    );
  },

  async disablePlatformCapability(id: string): Promise<PlatformCapability> {
    return requestApi<PlatformCapability>(
      `api/v1/connector/platform-capabilities/${encodeURIComponent(id)}/disable`,
      { method: 'POST' }
    );
  },

  async listOriginRoles(params: {
    tenantId: string;
    connectorSystemId: string;
    activeOnly?: boolean;
  }): Promise<OriginRole[]> {
    return requestApi<OriginRole[]>(
      `api/v1/connector/origin-roles?${stringifyQuery({
        tenantId: params.tenantId,
        connectorSystemId: params.connectorSystemId,
        activeOnly: String(Boolean(params.activeOnly))
      })}`
    );
  },

  async createOriginRole(request: OriginRoleRequest): Promise<OriginRole> {
    return requestApi<OriginRole>('api/v1/connector/origin-roles/create', {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  async updateOriginRole(id: string, request: OriginRoleRequest): Promise<OriginRole> {
    return requestApi<OriginRole>(
      `api/v1/connector/origin-roles/${encodeURIComponent(id)}/update`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async enableOriginRole(id: string): Promise<OriginRole> {
    return requestApi<OriginRole>(
      `api/v1/connector/origin-roles/${encodeURIComponent(id)}/enable`,
      { method: 'POST' }
    );
  },

  async disableOriginRole(id: string): Promise<OriginRole> {
    return requestApi<OriginRole>(
      `api/v1/connector/origin-roles/${encodeURIComponent(id)}/disable`,
      { method: 'POST' }
    );
  },

  async listOriginOrgs(params: {
    tenantId: string;
    connectorSystemId: string;
    activeOnly?: boolean;
  }): Promise<OriginOrg[]> {
    return requestApi<OriginOrg[]>(
      `api/v1/connector/origin-orgs?${stringifyQuery({
        tenantId: params.tenantId,
        connectorSystemId: params.connectorSystemId,
        activeOnly: String(Boolean(params.activeOnly))
      })}`
    );
  },

  async createOriginOrg(request: OriginOrgRequest): Promise<OriginOrg> {
    return requestApi<OriginOrg>('api/v1/connector/origin-orgs/create', {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  async updateOriginOrg(id: string, request: OriginOrgRequest): Promise<OriginOrg> {
    return requestApi<OriginOrg>(
      `api/v1/connector/origin-orgs/${encodeURIComponent(id)}/update`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async enableOriginOrg(id: string): Promise<OriginOrg> {
    return requestApi<OriginOrg>(
      `api/v1/connector/origin-orgs/${encodeURIComponent(id)}/enable`,
      { method: 'POST' }
    );
  },

  async disableOriginOrg(id: string): Promise<OriginOrg> {
    return requestApi<OriginOrg>(
      `api/v1/connector/origin-orgs/${encodeURIComponent(id)}/disable`,
      { method: 'POST' }
    );
  },

  async listActiveBusinessModules(params: {
    tenantId: string;
    connectorSystemId: string;
  }): Promise<BusinessModule[]> {
    return requestApi<BusinessModule[]>(
      `api/v1/connector/business-modules/active?${stringifyQuery(params)}`
    );
  },

  async listBusinessModules(params: {
    tenantId: string;
    connectorSystemId: string;
  }): Promise<BusinessModule[]> {
    return this.listActiveBusinessModules(params);
  },

  async listAllBusinessModules(params: {
    tenantId: string;
    connectorSystemId: string;
  }): Promise<BusinessModule[]> {
    return requestApi<BusinessModule[]>(
      `api/v1/connector/business-modules?${stringifyQuery(params)}`
    );
  },

  async createLocalBusinessModule(params: {
    tenantId: string;
    connectorSystemId: string;
  }): Promise<BusinessModule> {
    const stamp = Date.now();
    return requestApi<BusinessModule>('api/v1/connector/business-modules/create', {
      method: 'POST',
      body: JSON.stringify({
        tenantId: params.tenantId,
        connectorSystemId: params.connectorSystemId,
        moduleCode: `record_apply_${stamp}`,
        moduleName: '本地联调-采购申请',
        externalModuleId: `local-record-apply-${stamp}`,
        entryUrl: '/local-origin/record_apply',
        moduleType: 'BUSINESS',
        supportScenes: 'PRACTICE,EXAM',
        needPreData: true,
        defaultInitialStatus: 'DRAFT',
        defaultTargetStatus: 'SUBMITTED',
        capabilityCodesJson: '["DATA_CREATE","DATA_VALIDATE"]',
        updateBy: authApi.getSession()?.user.userId || 'admin'
      })
    });
  },

  async createBusinessModule(request: BusinessModuleRequest): Promise<BusinessModule> {
    return requestApi<BusinessModule>('api/v1/connector/business-modules/create', {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  async getBusinessModule(id: string): Promise<BusinessModule> {
    return requestApi<BusinessModule>(
      `api/v1/connector/business-modules/${encodeURIComponent(id)}`
    );
  },

  async updateBusinessModule(
    id: string,
    request: BusinessModuleRequest
  ): Promise<BusinessModule> {
    return requestApi<BusinessModule>(
      `api/v1/connector/business-modules/${encodeURIComponent(id)}/update`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async enableBusinessModule(id: string): Promise<BusinessModule> {
    return requestApi<BusinessModule>(
      `api/v1/connector/business-modules/${encodeURIComponent(id)}/enable`,
      { method: 'POST' }
    );
  },

  async disableBusinessModule(id: string): Promise<BusinessModule> {
    return requestApi<BusinessModule>(
      `api/v1/connector/business-modules/${encodeURIComponent(id)}/disable`,
      { method: 'POST' }
    );
  },

  async listBusinessModuleProcessSteps(params: {
    tenantId: string;
    businessModuleId: string;
  }): Promise<BusinessModuleProcessStep[]> {
    const { businessModuleId, tenantId } = params;
    return requestApi<BusinessModuleProcessStep[]>(
      `api/v1/connector/business-modules/${encodeURIComponent(businessModuleId)}/process-steps?${stringifyQuery({ tenantId })}`
    );
  },

  async createBusinessModuleProcessStep(
    businessModuleId: string,
    request: BusinessModuleProcessStepRequest
  ): Promise<BusinessModuleProcessStep> {
    return requestApi<BusinessModuleProcessStep>(
      `api/v1/connector/business-modules/${encodeURIComponent(businessModuleId)}/process-steps/create`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async updateBusinessModuleProcessStep(
    stepId: string,
    request: BusinessModuleProcessStepRequest
  ): Promise<BusinessModuleProcessStep> {
    return requestApi<BusinessModuleProcessStep>(
      `api/v1/connector/business-module-process-steps/${encodeURIComponent(stepId)}/update`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async enableBusinessModuleProcessStep(stepId: string): Promise<BusinessModuleProcessStep> {
    return requestApi<BusinessModuleProcessStep>(
      `api/v1/connector/business-module-process-steps/${encodeURIComponent(stepId)}/enable`,
      { method: 'POST' }
    );
  },

  async disableBusinessModuleProcessStep(stepId: string): Promise<BusinessModuleProcessStep> {
    return requestApi<BusinessModuleProcessStep>(
      `api/v1/connector/business-module-process-steps/${encodeURIComponent(stepId)}/disable`,
      { method: 'POST' }
    );
  },

  async listBusinessModuleProcessActors(params: {
    tenantId: string;
    processStepId: string;
  }): Promise<BusinessModuleProcessActor[]> {
    const { processStepId, tenantId } = params;
    return requestApi<BusinessModuleProcessActor[]>(
      `api/v1/connector/business-module-process-steps/${encodeURIComponent(processStepId)}/actors?${stringifyQuery({ tenantId })}`
    );
  },

  async createBusinessModuleProcessActor(
    processStepId: string,
    request: BusinessModuleProcessActorRequest
  ): Promise<BusinessModuleProcessActor> {
    return requestApi<BusinessModuleProcessActor>(
      `api/v1/connector/business-module-process-steps/${encodeURIComponent(processStepId)}/actors/create`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async updateBusinessModuleProcessActor(
    actorId: string,
    request: BusinessModuleProcessActorRequest
  ): Promise<BusinessModuleProcessActor> {
    return requestApi<BusinessModuleProcessActor>(
      `api/v1/connector/business-module-process-actors/${encodeURIComponent(actorId)}/update`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async enableBusinessModuleProcessActor(actorId: string): Promise<BusinessModuleProcessActor> {
    return requestApi<BusinessModuleProcessActor>(
      `api/v1/connector/business-module-process-actors/${encodeURIComponent(actorId)}/enable`,
      { method: 'POST' }
    );
  },

  async disableBusinessModuleProcessActor(actorId: string): Promise<BusinessModuleProcessActor> {
    return requestApi<BusinessModuleProcessActor>(
      `api/v1/connector/business-module-process-actors/${encodeURIComponent(actorId)}/disable`,
      { method: 'POST' }
    );
  },

  async listTemplates(params: {
    tenantId: string;
    connectorSystemId: string;
    moduleCode?: string;
    sceneType?: string;
  }): Promise<TeachingDataTemplate[]> {
    return requestApi<TeachingDataTemplate[]>(
      `api/v1/connector/data-templates?${stringifyQuery(params)}`
    );
  },

  async listActiveTemplatesByModuleScene(params: {
    tenantId: string;
    connectorSystemId: string;
    moduleCode: string;
    sceneType: string;
  }): Promise<TeachingDataTemplate[]> {
    return requestApi<TeachingDataTemplate[]>(
      `api/v1/connector/data-templates/active/by-module-scene?${stringifyQuery(params)}`
    );
  },

  async createLocalTemplate(params: {
    tenantId: string;
    connectorSystemId: string;
    moduleCode: string;
    sceneType: string;
  }): Promise<TeachingDataTemplate> {
    return requestApi<TeachingDataTemplate>('api/v1/connector/data-templates/create', {
      method: 'POST',
      body: JSON.stringify({
        tenantId: params.tenantId,
        connectorSystemId: params.connectorSystemId,
        templateCode: `LOCAL_TEMPLATE_${Date.now()}`,
        templateName: '本地联调数据模板',
        sceneType: params.sceneType,
        moduleCode: params.moduleCode,
        initState: 'DRAFT',
        supportMode: 'PRACTICE',
        configJson: '{"adapter":"local","mode":"demo"}'
      })
    });
  },

  async createTemplate(
    request: CreateTeachingDataTemplateRequest
  ): Promise<TeachingDataTemplate> {
    return requestApi<TeachingDataTemplate>('api/v1/connector/data-templates/create', {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  async getTemplate(id: string): Promise<TeachingDataTemplate> {
    return requestApi<TeachingDataTemplate>(
      `api/v1/connector/data-templates/${encodeURIComponent(id)}`
    );
  },

  async updateTemplate(
    id: string,
    request: TeachingDataTemplateRequest
  ): Promise<TeachingDataTemplate> {
    return requestApi<TeachingDataTemplate>(
      `api/v1/connector/data-templates/${encodeURIComponent(id)}/update`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async enableTemplate(id: string): Promise<TeachingDataTemplate> {
    return requestApi<TeachingDataTemplate>(
      `api/v1/connector/data-templates/${encodeURIComponent(id)}/enable`,
      { method: 'POST' }
    );
  },

  async disableTemplate(id: string): Promise<TeachingDataTemplate> {
    return requestApi<TeachingDataTemplate>(
      `api/v1/connector/data-templates/${encodeURIComponent(id)}/disable`,
      { method: 'POST' }
    );
  },

  async listActiveStrategies(params: {
    tenantId: string;
    connectorSystemId: string;
    businessModuleId: string;
  }): Promise<ModuleDataStrategy[]> {
    return requestApi<ModuleDataStrategy[]>(
      `api/v1/connector/module-data-strategies/active?${stringifyQuery(params)}`
    );
  },

  async listModuleDataStrategies(params: {
    tenantId: string;
    connectorSystemId: string;
    businessModuleId: string;
  }): Promise<ModuleDataStrategy[]> {
    return requestApi<ModuleDataStrategy[]>(
      `api/v1/connector/module-data-strategies?${stringifyQuery(params)}`
    );
  },

  async createLocalStrategy(params: {
    tenantId: string;
    connectorSystemId: string;
    businessModuleId: string;
    moduleCode: string;
    moduleName: string;
    sceneType: string;
    templateId: string;
  }): Promise<ModuleDataStrategy> {
    const created = await requestApi<ModuleDataStrategy>(
      'api/v1/connector/module-data-strategies/create',
      {
        method: 'POST',
        body: JSON.stringify({
          tenantId: params.tenantId,
          connectorSystemId: params.connectorSystemId,
          businessModuleId: params.businessModuleId,
          moduleCode: params.moduleCode,
          moduleName: params.moduleName,
          sceneType: params.sceneType,
          dataSourceStrategy: 'CREATE',
          initExternalStatus: 'DRAFT',
          targetExternalStatus: 'SUBMITTED',
          defaultOrgRolePolicyJson: '{"org":"required","role":"required"}',
          sharePolicy: 'ATTEMPT_EXCLUSIVE',
          regeneratePolicy: 'ON_ATTEMPT',
          lockPolicy: 'NONE',
          strategyCode: `LOCAL_STRATEGY_${Date.now()}`,
          templateId: params.templateId,
          prepareTiming: 'ON_DEMAND',
          validationPolicyJson: '{"requiredStatus":"DRAFT"}',
          createBy: authApi.getSession()?.user.userId || 'admin',
          updateBy: authApi.getSession()?.user.userId || 'admin'
        })
      }
    );
    return requestApi<ModuleDataStrategy>(
      `api/v1/connector/module-data-strategies/${encodeURIComponent(created.id)}/enable`,
      { method: 'POST' }
    );
  },

  async createModuleDataStrategy(
    request: ModuleDataStrategyRequest
  ): Promise<ModuleDataStrategy> {
    return requestApi<ModuleDataStrategy>('api/v1/connector/module-data-strategies/create', {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  async enableModuleDataStrategy(id: string): Promise<ModuleDataStrategy> {
    return requestApi<ModuleDataStrategy>(
      `api/v1/connector/module-data-strategies/${encodeURIComponent(id)}/enable`,
      { method: 'POST' }
    );
  },

  async disableModuleDataStrategy(id: string): Promise<ModuleDataStrategy> {
    return requestApi<ModuleDataStrategy>(
      `api/v1/connector/module-data-strategies/${encodeURIComponent(id)}/disable`,
      { method: 'POST' }
    );
  },

  async getModuleDataStrategy(id: string): Promise<ModuleDataStrategy> {
    return requestApi<ModuleDataStrategy>(
      `api/v1/connector/module-data-strategies/${encodeURIComponent(id)}`
    );
  },

  async updateModuleDataStrategy(
    id: string,
    request: ModuleDataStrategyRequest
  ): Promise<ModuleDataStrategy> {
    return requestApi<ModuleDataStrategy>(
      `api/v1/connector/module-data-strategies/${encodeURIComponent(id)}/update`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async createRequirement(
    requirement: Partial<DataRequirement>
  ): Promise<DataRequirement> {
    return requestApi<DataRequirement>('api/v1/teaching-data/requirements/create', {
      method: 'POST',
      body: JSON.stringify(requirement)
    });
  },

  async listRequirements(params: {
    tenantId: string;
    taskId: string;
    sceneType: string;
  }): Promise<DataRequirement[]> {
    return requestApi<DataRequirement[]>(
      `api/v1/teaching-data/requirements?${stringifyQuery(params)}`
    );
  },

  async listRequirementItems(params: {
    tenantId: string;
    requirementId: string;
  }): Promise<DataRequirementItem[]> {
    return requestApi<DataRequirementItem[]>(
      `api/v1/teaching-data/requirements/${encodeURIComponent(
        params.requirementId
      )}/items?${stringifyQuery({ tenantId: params.tenantId })}`
    );
  },

  async listJobs(params: {
    tenantId: string;
    taskId: string;
    sceneType: string;
  }): Promise<DataPrepareJob[]> {
    return requestApi<DataPrepareJob[]>(
      `api/v1/teaching-data/jobs?${stringifyQuery(params)}`
    );
  },

  async prepareAndExecute(
    request: PrepareAndExecuteRequest
  ): Promise<DataPrepareJob> {
    return requestApi<DataPrepareJob>('api/v1/teaching-data/prepare/execute', {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  async triggerTaskPrepare(
    taskId: string,
    request: TriggerTaskPrepareRequest
  ): Promise<TaskPrepareTriggerResult> {
    return requestApi<TaskPrepareTriggerResult>(
      `api/v1/teaching-data/tasks/${encodeURIComponent(taskId)}/prepare`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async preflightTaskPrepare(params: {
    tenantId: string;
    taskId: string;
    connectorSystemId: string;
    businessModuleId: string;
    moduleCode: string;
    sceneType: string;
  }): Promise<DataPreparePreflightResult> {
    const { taskId, ...query } = params;
    return requestApi<DataPreparePreflightResult>(
      `api/v1/teaching-data/tasks/${encodeURIComponent(taskId)}/prepare/preflight?${stringifyQuery(query)}`
    );
  },

  async retryFailedJob(
    jobId: string,
    request: RetryFailedJobRequest
  ): Promise<DataPrepareJob> {
    return requestApi<DataPrepareJob>(
      `api/v1/teaching-data/prepare/jobs/${encodeURIComponent(jobId)}/retry`,
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async listClassicCases(params: {
    tenantId: string;
    learningConnectorSystemId?: string;
    moduleCode?: string;
    teachingPointId?: string;
  }): Promise<ClassicCaseAsset[]> {
    return requestApi<ClassicCaseAsset[]>(
      `api/v1/classic-cases/list?${stringifyQuery(params)}`
    );
  },

  async getClassicCase(id: string, tenantId: string): Promise<ClassicCaseAsset> {
    return requestApi<ClassicCaseAsset>(
      `api/v1/classic-cases/${encodeURIComponent(id)}?${stringifyQuery({ tenantId })}`
    );
  },

  async listClassicCaseVersions(id: string, tenantId: string): Promise<ClassicCaseVersion[]> {
    return requestApi<ClassicCaseVersion[]>(
      `api/v1/classic-cases/${encodeURIComponent(id)}/versions?${stringifyQuery({ tenantId })}`
    );
  },

  async getClassicCaseVersion(
    id: string,
    caseVersionId: string
  ): Promise<ClassicCaseVersion> {
    return requestApi<ClassicCaseVersion>(
      `api/v1/classic-cases/${encodeURIComponent(id)}/versions/${encodeURIComponent(caseVersionId)}`
    );
  },

  async listLessonPlanClassicCaseOptions(params: {
    businessModuleCode: string;
    connectorSystemId?: string;
    keyword?: string;
  }): Promise<LessonPlanClassicCaseOption[]> {
    return requestApi<LessonPlanClassicCaseOption[]>(
      `api/v1/lesson-plans/classic-case-options?${stringifyQuery(params)}`
    );
  },

  async validateLessonPlanClassicCaseConfig(
    request: ClassicCaseConfigValidationRequest
  ): Promise<LessonPlanClassicCaseOption> {
    return requestApi<LessonPlanClassicCaseOption>(
      'api/v1/lesson-plans/classic-case-config/validate',
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async generateClassicCaseLaunch(
    request: ClassicCaseGenerateLaunchRequest
  ): Promise<ClassicCaseLaunchResult> {
    return requestApi<ClassicCaseLaunchResult>('api/v1/classic-cases/generate-launch', {
      method: 'POST',
      body: JSON.stringify(request)
    });
  },

  async listPools(params: {
    tenantId: string;
    requirementId: string;
  }): Promise<TeachingDataPool[]> {
    return requestApi<TeachingDataPool[]>(
      `api/v1/teaching-data/pools?${stringifyQuery(params)}`
    );
  },

  async listAllocations(params: {
    tenantId: string;
    requirementId: string;
  }): Promise<DataInstanceAllocation[]> {
    return requestApi<DataInstanceAllocation[]>(
      `api/v1/teaching-data/allocations?${stringifyQuery(params)}`
    );
  },

  async listMyAllocations(params: {
    tenantId: string;
    taskId: string;
    sceneType: string;
    ownerUserId: string;
  }): Promise<DataInstanceAllocation[]> {
    return requestApi<DataInstanceAllocation[]>(
      `api/v1/teaching-data/allocations/mine?${stringifyQuery(params)}`
    );
  },

  async acquireDataInstance(params: {
    tenantId: string;
    poolId: string;
    ownerUserId: string;
    taskId: string;
    allocationScene: string;
    attemptId?: string;
    questionAttemptId?: string;
    createBy: string;
    updateBy: string;
  }): Promise<DataInstanceAllocation> {
    return requestApi<DataInstanceAllocation>('api/v1/teaching-data/allocations/acquire', {
      method: 'POST',
      body: JSON.stringify(params)
    });
  },

  async createStudentDataLaunch(
    request: CreateStudentDataLaunchRequest
  ): Promise<StudentDataLaunchResult> {
    return requestApi<StudentDataLaunchResult>(
      'api/v1/teaching-data/student-launches/create',
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async createAuthoringLaunch(
    request: CreateAuthoringLaunchRequest
  ): Promise<AuthoringLaunchResult> {
    return requestApi<AuthoringLaunchResult>(
      'api/v1/teaching-data/authoring-launches/create',
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async createCurrentStudentTaskLaunch(
    request: CreateStudentTaskLaunchRequest
  ): Promise<StudentDataLaunchResult> {
    return requestApi<StudentDataLaunchResult>(
      'api/v1/teaching-data/student-launches/task-launch',
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async listCurrentStudentTaskAllocations(
    request: CreateStudentTaskLaunchRequest
  ): Promise<DataInstanceAllocation[]> {
    return requestApi<DataInstanceAllocation[]>(
      'api/v1/teaching-data/student-launches/task-allocations',
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  },

  async restartCurrentStudentTaskData(
    request: CreateStudentTaskLaunchRequest
  ): Promise<DataInstanceAllocation> {
    return requestApi<DataInstanceAllocation>(
      'api/v1/teaching-data/student-launches/task-restart',
      {
        method: 'POST',
        body: JSON.stringify(request)
      }
    );
  }
};

function resolveTrainingWorkspacePath(session: AuthSession): string {
  return authApi.getPortalRole(session) === 'student'
    ? 'api/v1/student/training/workspace'
    : 'api/v1/training/workspace';
}

function parseTrainingWorkspaceState(
  value: unknown,
  session: AuthSession
): TrainingState {
  if (!isTrainingState(value)) {
    throw new TrainingApiRequestError('后端返回的工作区数据格式错误');
  }
  const state = clone(value);
  if (!Array.isArray(state.businessPlatforms)) {
    state.businessPlatforms =
      authApi.getPortalRole(session) === 'student'
        ? []
        : createDefaultBusinessPlatforms();
  }
  return state;
}

async function requestApi<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const headers = new Headers(options.headers);
  headers.set('Content-Type', 'application/json');
  const session = authApi.getSession();
  if (session?.token) {
    headers.set('Authorization', `${session.tokenType || 'Bearer'} ${session.token}`);
  }
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers
  });
  const payload = (await response.json()) as ApiResult<T>;
  if (response.status === 401 || payload.code === 401) {
    handleUnauthorizedSession();
    throw new TrainingApiRequestError('登录已失效，请重新登录', 401, response.status);
  }
  if (!response.ok || !payload.success) {
    throw new TrainingApiRequestError(
      payload.message || '接口请求失败',
      payload.code,
      response.status
    );
  }
  return payload.result;
}

function stringifyQuery(params: Record<string, string | undefined>): string {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== '') {
      query.set(key, value);
    }
  });
  return query.toString();
}

function normalizeApiBaseUrl(baseUrl: string): string {
  return baseUrl.replace(/\/?$/, '/');
}

function resolveBrowserStorage(): StorageLike {
  try {
    if (typeof window !== 'undefined' && window.localStorage) {
      return window.localStorage;
    }
  } catch {
    return fallbackMemoryStorage;
  }
  return fallbackMemoryStorage;
}

function saveAuthSession(session: AuthSession) {
  setApiToken(session.token);
  try {
    resolveBrowserStorage().setItem(
      AUTH_SESSION_STORAGE_KEY,
      JSON.stringify(session)
    );
  } catch {
    // 存储失败时不阻断登录结果，路由守卫会在下一次导航重新校验。
  }
}

function clearAuthSession() {
  clearApiToken();
  try {
    resolveBrowserStorage().removeItem(AUTH_SESSION_STORAGE_KEY);
  } catch {
    // 浏览器存储不可用时，当前请求已经失败，后续路由守卫会重新校验登录态。
  }
}

function handleUnauthorizedSession() {
  clearAuthSession();
  if (typeof window === 'undefined') return;
  const currentPath = `${window.location.pathname}${window.location.search}${window.location.hash}`;
  if (window.location.pathname === '/login') return;
  window.location.assign(`/login?redirect=${encodeURIComponent(currentPath)}`);
}

function loadAuthSession(): AuthSession | null {
  try {
    const raw = resolveBrowserStorage().getItem(AUTH_SESSION_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<AuthSession>;
    if (typeof parsed.issuedAt !== 'number') {
      resolveBrowserStorage().removeItem(AUTH_SESSION_STORAGE_KEY);
      clearApiToken();
      return null;
    }
    const session = normalizeSession(parsed as AuthSession, parsed.issuedAt);
    if (isSessionExpired(session)) {
      resolveBrowserStorage().removeItem(AUTH_SESSION_STORAGE_KEY);
      clearApiToken();
      return null;
    }
    setApiToken(session.token);
    return session;
  } catch {
    return null;
  }
}

function normalizeSession(session: AuthSession, issuedAt: number): AuthSession {
  return {
    token: session.token,
    tokenType: session.tokenType || 'Bearer',
    expiresIn: Number(session.expiresIn) || 0,
    issuedAt,
    user: {
      userId: session.user.userId,
      tenantId: session.user.tenantId,
      username: session.user.username,
      displayName: session.user.displayName || session.user.username,
      userType: session.user.userType || '',
      studentNo: session.user.studentNo,
      employeeNo: session.user.employeeNo,
      roles: Array.isArray(session.user.roles) ? session.user.roles : [],
      orgIds: Array.isArray(session.user.orgIds) ? session.user.orgIds : []
    }
  };
}

function resolvePortalRole(session: AuthSession | null): PortalRole {
  if (hasPortalRole(session, 'admin')) return 'admin';
  if (hasPortalRole(session, 'teacher')) return 'teacher';
  if (hasPortalRole(session, 'student')) return 'student';
  return 'admin';
}

function hasPortalRole(
  session: AuthSession | null,
  requiredRole: PortalRole
): boolean {
  const roleText = [
    ...(session?.user.roles ?? []),
    session?.user.userType ?? ''
  ]
    .join('|')
    .toLowerCase();
  if (requiredRole === 'admin') {
    return (
      roleText.includes('admin') ||
      roleText.includes('administrator') ||
      roleText.includes('expert') ||
      roleText.includes('manager') ||
      roleText.includes('管理') ||
      roleText.includes('专家')
    );
  }
  if (requiredRole === 'teacher') {
    return roleText.includes('teacher') || roleText.includes('教师');
  }
  return roleText.includes('student') || roleText.includes('学生');
}

function isSessionExpired(session: AuthSession): boolean {
  if (!session.token) return true;
  const ttlSeconds = Number(session.expiresIn);
  if (!Number.isFinite(ttlSeconds) || ttlSeconds <= 0) return false;
  return Date.now() - session.issuedAt >= ttlSeconds * 1000;
}

const fallbackMemoryStorage = createMemoryStorage();

function isTrainingState(value: unknown): value is TrainingState {
  if (!isRecord(value)) return false;
  return (
    (value.currentRole === 'admin' ||
      value.currentRole === 'teacher' ||
      value.currentRole === 'student') &&
    Array.isArray(value.lessons) &&
    value.lessons.every(isLessonRecord) &&
    (!('businessPlatforms' in value) ||
      (Array.isArray(value.businessPlatforms) &&
        value.businessPlatforms.every(isBusinessPlatformRecord))) &&
    isRecord(value.examSettings) &&
    isRecord(value.groupPlans) &&
    isRecord(value.unitDataPlans) &&
    isRecord(value.dataItems) &&
    Array.isArray(value.publishedTasks) &&
    value.publishedTasks.every(isPublishedTaskRecord) &&
    Array.isArray(value.studentTasks) &&
    value.studentTasks.every(isStudentTaskRecord) &&
    Array.isArray(value.activities) &&
    value.activities.every(
      (event) =>
        isRecord(event) &&
        typeof event.id === 'string' &&
        typeof event.title === 'string'
    ) &&
    Object.values(value.dataItems).every(
      (items) =>
        Array.isArray(items) &&
        items.every(
          (item) =>
            isRecord(item) &&
            typeof item.id === 'string' &&
            typeof item.lessonId === 'string' &&
            Array.isArray(item.audit)
        )
    )
  );
}

function isBusinessPlatformRecord(value: unknown) {
  return (
    isRecord(value) &&
    typeof value.id === 'string' &&
    typeof value.name === 'string' &&
    typeof value.baseUrl === 'string' &&
    (!('modules' in value) ||
      (Array.isArray(value.modules) &&
        value.modules.every(isBusinessPlatformModuleRecord)))
  );
}

function isBusinessPlatformModuleRecord(value: unknown) {
  return (
    isRecord(value) &&
    typeof value.id === 'string' &&
    typeof value.name === 'string' &&
    typeof value.path === 'string'
  );
}

function isLessonRecord(value: unknown) {
  return (
    isRecord(value) &&
    typeof value.id === 'string' &&
    typeof value.title === 'string' &&
    typeof value.status === 'string' &&
    Array.isArray(value.stages) &&
    value.stages.every(
      (stage) =>
        isRecord(stage) &&
        typeof stage.id === 'string' &&
        typeof stage.name === 'string' &&
        Array.isArray(stage.recordedSteps)
    )
  );
}

function isPublishedTaskRecord(value: unknown) {
  return (
    isRecord(value) &&
    typeof value.id === 'string' &&
    typeof value.lessonId === 'string' &&
    typeof value.startAt === 'string' &&
    typeof value.endAt === 'string'
  );
}

function isStudentTaskRecord(value: unknown) {
  return (
    isRecord(value) &&
    typeof value.id === 'string' &&
    typeof value.lessonId === 'string' &&
    typeof value.studentId === 'string' &&
    Array.isArray(value.completedStageIds)
  );
}

/**
 * Keeps browser data created by an earlier demo revision usable after adding
 * multi-role student assignments.
 */
function normalizeState(state: TrainingState): TrainingState {
  const normalized = clone(state);
  const defaultPlatforms = createDefaultBusinessPlatforms();
  const storedPlatforms =
    Array.isArray(normalized.businessPlatforms) &&
    normalized.businessPlatforms.length > 0
      ? normalized.businessPlatforms
      : defaultPlatforms;
  normalized.businessPlatforms = storedPlatforms.map((platform) => ({
    ...platform,
    modules: Array.isArray(platform.modules)
      ? platform.modules
      : clone(
          defaultPlatforms.find((candidate) => candidate.code === platform.code)
            ?.modules ?? []
        )
  }));
  normalized.lessons = normalized.lessons.map((lesson) => ({
    ...lesson,
    businessPlatformId:
      lesson.businessPlatformId ||
      resolveLegacyBusinessPlatformId(
        lesson.moduleName,
        normalized.businessPlatforms
      ),
    businessPlatformModuleId:
      lesson.businessPlatformModuleId ||
      resolveLegacyBusinessPlatformModuleId(
        lesson.businessPlatformId ||
          resolveLegacyBusinessPlatformId(
            lesson.moduleName,
            normalized.businessPlatforms
          ),
        lesson.moduleName,
        normalized.businessPlatforms
      )
  }));
  normalized.publishedTasks = normalized.publishedTasks.map((task) => ({
    ...task,
    dataPrepareMode: task.dataPrepareMode ?? 'NORMAL'
  }));
  normalized.studentTasks = normalized.studentTasks.map((task) => ({
    ...task,
    dataPrepareMode: task.dataPrepareMode ?? 'NORMAL',
    groupKeys:
      Array.isArray(task.groupKeys) && task.groupKeys.length > 0
        ? [...new Set(task.groupKeys)]
        : [task.groupKey],
    unitId: task.unitId ?? '',
    unitName: task.unitName ?? '',
    dataItemId: task.dataItemId ?? '',
    attemptNumber:
      Number.isInteger(task.attemptNumber) && task.attemptNumber > 0
        ? task.attemptNumber
        : 1,
    submissionValues:
      task.submissionValues && typeof task.submissionValues === 'object'
        ? task.submissionValues
        : {},
    completedPracticeStepIds: Array.isArray(task.completedPracticeStepIds)
      ? [...new Set(task.completedPracticeStepIds)]
      : [],
    practiceStepResults: Array.isArray(task.practiceStepResults)
      ? task.practiceStepResults
      : []
  }));
  Object.values(normalized.dataItems).forEach((items) => {
    items.forEach((item) => {
      item.assignedStudentTaskIds ??= [];
    });
  });
  return normalized;
}

function resolveLegacyBusinessPlatformId(
  moduleName: string,
  platforms: TrainingState['businessPlatforms']
) {
  const code = moduleName.includes('报销') || moduleName.includes('财务')
    ? 'EXPENSE'
    : moduleName.includes('合同')
      ? 'CONTRACT'
      : 'PURCHASE';
  return (
    platforms.find((platform) => platform.code === code)?.id ??
    platforms.find((platform) => platform.status === 'ENABLED')?.id ??
    platforms[0]?.id ??
    ''
  );
}

function resolveLegacyBusinessPlatformModuleId(
  platformId: string,
  moduleName: string,
  platforms: TrainingState['businessPlatforms']
) {
  const modules =
    platforms.find((platform) => platform.id === platformId)?.modules ?? [];
  const keywords = moduleName
    .split(/[\s、/与及管理业务]+/)
    .map((keyword) => keyword.trim())
    .filter((keyword) => keyword.length >= 2);
  return (
    modules.find((module) =>
      keywords.some(
        (keyword) =>
          module.name.includes(keyword) || module.description.includes(keyword)
      )
    )?.id ??
    modules.find((module) => module.status === 'ENABLED')?.id ??
    modules[0]?.id ??
    ''
  );
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}
