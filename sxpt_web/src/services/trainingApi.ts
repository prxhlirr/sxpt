import { createMockTrainingState } from '../data/mockSeed';
import type { PortalRole, StorageLike, TrainingState } from '../domain/models';

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
  baseUrl: string;
  authType: string;
  configJson?: string;
}

export interface UpdateConnectorSystemRequest {
  id: string;
  systemName: string;
  systemType: string;
  baseUrl: string;
  authType: string;
  configJson?: string;
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
  configJson?: string;
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
  externalStatus?: string;
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
  triggerType?: string;
  createTime?: string;
  updateTime?: string;
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
  attemptId?: string;
  questionAttemptId?: string;
  ownerUserId: string;
  allocationScene: string;
  allocationStatus?: string;
  allocateTime?: string;
  requiredExternalOrgId?: string;
  requiredExternalRoleId?: string;
  actorType?: string;
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

interface ApiResult<T> {
  success: boolean;
  code: number;
  message: string;
  result: T;
  timestamp: number;
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
    try {
      resolveBrowserStorage().removeItem(AUTH_SESSION_STORAGE_KEY);
    } catch {
      // 浏览器存储不可用时，会话会在当前页面生命周期内自然失效。
    }
  },

  getHomePath(session: AuthSession | null = loadAuthSession()): string {
    const role = resolvePortalRole(session);
    if (role === 'student') return '/student/tasks';
    if (role === 'teacher') return '/teacher/dashboard';
    return '/admin/overview';
  },

  canAccess(requiredRole?: PortalRole | PortalRole[]): boolean {
    if (!requiredRole) return true;
    const session = loadAuthSession();
    const roles = Array.isArray(requiredRole) ? requiredRole : [requiredRole];
    return roles.some((role) => hasPortalRole(session, role));
  },

  cleanupInvalidSession() {
    loadAuthSession();
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
    return this.createConnectorSystem({
      tenantId,
      systemCode: `LOCAL_ORIGIN_${Date.now()}`,
      systemName: '本地联调原平台',
      systemType: 'LOCAL_DEV',
      baseUrl: 'http://127.0.0.1:8080/local-origin',
      authType: 'NONE',
      configJson: '{"adapter":"local"}'
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

  async listBusinessModules(params: {
    tenantId: string;
    connectorSystemId: string;
  }): Promise<BusinessModule[]> {
    return requestApi<BusinessModule[]>(
      `api/v1/connector/business-modules/active?${stringifyQuery(params)}`
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

  async listPools(params: {
    tenantId: string;
    requirementId: string;
  }): Promise<TeachingDataPool[]> {
    return requestApi<TeachingDataPool[]>(
      `api/v1/teaching-data/pools?${stringifyQuery(params)}`
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
  }
};

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
  if (!response.ok || !payload.success) {
    throw new Error(payload.message || '接口请求失败');
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
  try {
    resolveBrowserStorage().setItem(
      AUTH_SESSION_STORAGE_KEY,
      JSON.stringify(session)
    );
  } catch {
    // 存储失败时不阻断登录结果，路由守卫会在下一次导航重新校验。
  }
}

function loadAuthSession(): AuthSession | null {
  try {
    const raw = resolveBrowserStorage().getItem(AUTH_SESSION_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<AuthSession>;
    if (typeof parsed.issuedAt !== 'number') {
      resolveBrowserStorage().removeItem(AUTH_SESSION_STORAGE_KEY);
      return null;
    }
    const session = normalizeSession(parsed as AuthSession, parsed.issuedAt);
    if (isSessionExpired(session)) {
      resolveBrowserStorage().removeItem(AUTH_SESSION_STORAGE_KEY);
      return null;
    }
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
      roleText.includes('专家') ||
      (!roleText.includes('teacher') &&
        !roleText.includes('教师') &&
        !roleText.includes('student') &&
        !roleText.includes('学生'))
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
  normalized.studentTasks = normalized.studentTasks.map((task) => ({
    ...task,
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
        : {}
  }));
  Object.values(normalized.dataItems).forEach((items) => {
    items.forEach((item) => {
      item.assignedStudentTaskIds ??= [];
    });
  });
  return normalized;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}
