import { reactive } from 'vue';
import type {
  ActivityEvent,
  BusinessPlatform,
  BusinessPlatformModule,
  DataAuditEvent,
  ExamDataItem,
  ExamSettings,
  GroupPlan,
  LessonPlan,
  LessonStage,
  PortalRole,
  PublishedTask,
  RecordedStep,
  RunMode,
  StudentTask,
  StorageLike,
  TrainingState,
  UnitDataPlan
} from '../domain/models';
import {
  authApi,
  createTrainingApi,
  trainingWorkspaceApi,
  type AuthSession,
  type TrainingApi
} from '../services/trainingApi';
import {
  backendTrainingApi,
  type BackendTrainingApi
} from '../services/backendTrainingApi';
import { getApiConfig } from '../config/api';
import { createDefaultBusinessPlatforms } from '../data/mockSeed';
import { usersApi } from '../api/users';

export interface TrainingStoreOptions {
  api?: TrainingApi;
  backend?: BackendTrainingApi;
  workspaceApi?: typeof trainingWorkspaceApi;
  storage?: StorageLike;
  now?: () => string;
  idFactory?: (prefix: string) => string;
}

export interface RemoteSyncState {
  enabled: boolean;
  loading: boolean;
  initialized: boolean;
  operation: string;
  lastError: string;
  lastSyncedAt: string;
}

export interface DataGenerationSummary {
  requestedCount: number;
  readyCount: number;
  failedCount: number;
  status: 'COMPLETED' | 'COMPLETED_WITH_FAILURES';
}

export class TrainingValidationError extends Error {
  constructor(public readonly issues: string[]) {
    super(issues.join('；'));
    this.name = 'TrainingValidationError';
  }
}

export function createTrainingStore(options: TrainingStoreOptions = {}) {
  const api = options.api ?? createTrainingApi(options.storage);
  const backend = options.backend ?? backendTrainingApi;
  const workspaceApi = options.workspaceApi ?? trainingWorkspaceApi;
  const now = options.now ?? (() => new Date().toISOString());
  const idFactory = options.idFactory ?? defaultIdFactory;
  const state = reactive(api.loadState()) as TrainingState;
  const remote = reactive<RemoteSyncState>({
    enabled: backend.isEnabled(),
    loading: false,
    initialized: false,
    operation: '',
    lastError: '',
    lastSyncedAt: ''
  });
  let workspaceUserKey = '';
  let workspaceSession: AuthSession | null = null;
  let workspaceLoadPromise: Promise<void> | undefined;
  let workspaceSaveTimer: ReturnType<typeof setTimeout> | undefined;
  let workspaceSaveQueue: Promise<void> = Promise.resolve();

  const persist = () => {
    api.saveState(toPlain(state));
    scheduleWorkspaceSave();
  };

  function replaceState(nextState: TrainingState) {
    const next = toPlain(nextState);
    state.currentRole = next.currentRole;
    state.businessPlatforms = next.businessPlatforms;
    state.lessons = next.lessons;
    state.examSettings = next.examSettings;
    state.groupPlans = next.groupPlans;
    state.unitDataPlans = next.unitDataPlans;
    state.dataItems = next.dataItems;
    state.publishedTasks = next.publishedTasks;
    state.studentTasks = next.studentTasks;
    state.activities = next.activities;
  }

  function scheduleWorkspaceSave() {
    if (!workspaceUserKey || !backend.isEnabled()) return;
    if (workspaceSaveTimer) clearTimeout(workspaceSaveTimer);
    workspaceSaveTimer = setTimeout(() => {
      workspaceSaveTimer = undefined;
      void saveAuthenticatedWorkspace().catch((error) => {
        remote.lastError =
          error instanceof Error ? error.message : '工作区自动保存失败';
      });
    }, 350);
  }

  async function saveAuthenticatedWorkspace(): Promise<void> {
    const session = workspaceSession ?? authApi.getSession();
    if (
      !session ||
      `${session.user.tenantId}:${session.user.userId}` !== workspaceUserKey ||
      !backend.isEnabled()
    ) {
      return;
    }
    const userKey = workspaceUserKey;
    const snapshot = toPlain(state);
    const pendingSave = workspaceSaveQueue
      .catch(() => undefined)
      .then(async () => {
        await workspaceApi.save(session, snapshot);
        if (workspaceUserKey === userKey) {
          remote.lastError = '';
          remote.lastSyncedAt = now();
        }
      });
    workspaceSaveQueue = pendingSave;
    await pendingSave;
  }

  async function flushAuthenticatedWorkspace(): Promise<void> {
    if (workspaceSaveTimer) {
      clearTimeout(workspaceSaveTimer);
      workspaceSaveTimer = undefined;
    }
    await saveAuthenticatedWorkspace();
  }

  function clearAuthenticatedWorkspace(): void {
    workspaceUserKey = '';
    workspaceSession = null;
    workspaceLoadPromise = undefined;
    if (workspaceSaveTimer) {
      clearTimeout(workspaceSaveTimer);
      workspaceSaveTimer = undefined;
    }
    replaceState(api.resetDemo());
    remote.initialized = false;
    remote.loading = false;
    remote.operation = '';
    remote.lastError = '';
    remote.lastSyncedAt = '';
  }

  async function initializeAuthenticatedWorkspace(
    session: AuthSession | null = authApi.getSession()
  ): Promise<void> {
    if (!session) {
      workspaceUserKey = '';
      workspaceSession = null;
      return;
    }
    const userKey = `${session.user.tenantId}:${session.user.userId}`;
    if (workspaceUserKey === userKey) {
      workspaceSession = session;
      return;
    }
    if (workspaceLoadPromise) return workspaceLoadPromise;
    const previousWorkspaceUserKey = workspaceUserKey;
    workspaceLoadPromise = (async () => {
      const role = authApi.getPortalRole(session);
      state.currentRole = role;
      if (!backend.isEnabled()) {
        if (role === 'student') {
          state.studentTasks = state.studentTasks.filter(
            (task) => task.studentId === session.user.userId
          );
        }
        api.saveState(toPlain(state));
        workspaceUserKey = userKey;
        workspaceSession = session;
        return;
      }
      const savedState = await workspaceApi.load(session);
      if (savedState) {
        replaceState(savedState);
        state.currentRole = role;
      } else if (role !== 'student') {
        if (
          previousWorkspaceUserKey &&
          previousWorkspaceUserKey !== userKey
        ) {
          replaceState(api.resetDemo());
          state.currentRole = role;
        }
        await workspaceApi.save(session, toPlain(state));
      }
      api.saveState(toPlain(state));
      workspaceUserKey = userKey;
      workspaceSession = session;
      remote.initialized = true;
      remote.lastError = '';
      remote.lastSyncedAt = now();
    })();
    try {
      await workspaceLoadPromise;
    } finally {
      workspaceLoadPromise = undefined;
    }
  }

  const requireLesson = (lessonId: string) => {
    const lesson = state.lessons.find((candidate) => candidate.id === lessonId);
    if (!lesson) throw new Error(`未找到教案：${lessonId}`);
    return lesson;
  };

  const requireBusinessPlatform = (platformId: string) => {
    const platform = state.businessPlatforms.find(
      (candidate) => candidate.id === platformId
    );
    if (!platform) throw new Error(`未找到业务平台：${platformId}`);
    return platform;
  };

  const requireBusinessPlatformModule = (
    platformId: string,
    moduleId: string
  ) => {
    const platform = requireBusinessPlatform(platformId);
    const businessModule = platform.modules.find(
      (candidate) => candidate.id === moduleId
    );
    if (!businessModule) {
      throw new Error(`未在“${platform.name}”中找到业务模块：${moduleId}`);
    }
    return businessModule;
  };

  const requireDataItem = (lessonId: string, itemId: string) => {
    requireLesson(lessonId);
    const item = (state.dataItems[lessonId] ?? []).find(
      (candidate) => candidate.id === itemId
    );
    if (!item) throw new Error(`未找到考试数据：${itemId}`);
    return item;
  };

  const requireStudentTask = (taskId: string) => {
    const task = state.studentTasks.find((candidate) => candidate.id === taskId);
    if (!task) throw new Error(`未找到学员任务：${taskId}`);
    return task;
  };

  const assertLessonConfigurationMutable = (lessonId: string) => {
    if (
      state.publishedTasks.some(
        (task) => task.lessonId === lessonId && task.mode === 'EXAM'
      )
    ) {
      throw new Error(
        '该教案已有已发布考试，配置已冻结；请复制教案后创建新版本'
      );
    }
  };

  const resolvePublishedStatus = (
    task: Pick<PublishedTask, 'startAt' | 'endAt'>,
    at = Date.parse(now())
  ): PublishedTask['status'] =>
    at < Date.parse(task.startAt)
      ? 'SCHEDULED'
      : at >= Date.parse(task.endAt)
        ? 'FINISHED'
        : 'RUNNING';

  const requireRunningExam = (task: StudentTask) => {
    const published = state.publishedTasks.find(
      (candidate) => candidate.id === task.publishedTaskId
    );
    if (!published) throw new Error('未找到已发布考试批次');
    published.status = resolvePublishedStatus(published);
    if (published.status === 'SCHEDULED') throw new Error('考试尚未开始');
    if (published.status === 'FINISHED') throw new Error('考试已结束');
    return published;
  };

  const assertAttemptWithinDuration = (task: StudentTask) => {
    if (task.mode !== 'EXAM') return;
    const settings = state.examSettings[task.lessonId];
    if (!settings || !task.startedAt) return;
    const deadline =
      Date.parse(task.startedAt) + settings.durationMinutes * 60_000;
    if (Date.parse(now()) > deadline) {
      throw new Error('本次作答已超时，请按考试规则重新作答');
    }
  };

  const addActivity = (
    type: string,
    title: string,
    detail: string
  ): ActivityEvent => {
    const event: ActivityEvent = {
      id: idFactory('activity'),
      type,
      title,
      detail,
      at: now()
    };
    state.activities.unshift(event);
    if (state.activities.length > 200) state.activities.length = 200;
    return event;
  };

  const addDataAudit = (
    item: ExamDataItem,
    type: string,
    reason: string
  ): DataAuditEvent => {
    const event: DataAuditEvent = {
      id: idFactory('audit'),
      type,
      reason,
      at: now()
    };
    item.audit.push(event);
    return event;
  };

  function setRole(role: PortalRole) {
    state.currentRole = role;
    addActivity('ROLE_SWITCHED', '切换门户角色', `当前角色：${role}`);
    persist();
  }

  function getBusinessPlatform(platformId: string) {
    return state.businessPlatforms.find((platform) => platform.id === platformId);
  }

  function getBusinessPlatformModule(platformId: string, moduleId: string) {
    return getBusinessPlatform(platformId)?.modules.find(
      (businessModule) => businessModule.id === moduleId
    );
  }

  async function runRemote<T>(
    operation: string,
    action: () => Promise<T>
  ): Promise<T> {
    remote.enabled = backend.isEnabled();
    remote.loading = true;
    remote.operation = operation;
    remote.lastError = '';
    try {
      const result = await action();
      remote.initialized = true;
      remote.lastSyncedAt = now();
      return result;
    } catch (error) {
      remote.lastError =
        error instanceof Error ? error.message : '后端接口调用失败';
      throw error;
    } finally {
      remote.loading = false;
      remote.operation = '';
    }
  }

  async function syncBusinessPlatforms(): Promise<BusinessPlatform[]> {
    if (!backend.isEnabled()) {
      remote.enabled = false;
      remote.initialized = true;
      return state.businessPlatforms;
    }
    return runRemote('同步业务平台', async () => {
      const previousPlatforms = toPlain(state.businessPlatforms);
      const syncCandidates = [...previousPlatforms];
      for (const defaultPlatform of createDefaultBusinessPlatforms()) {
        if (
          !syncCandidates.some(
            (platform) =>
              platform.id === defaultPlatform.id ||
              platform.code.trim().toUpperCase() ===
                defaultPlatform.code.trim().toUpperCase()
          )
        ) {
          syncCandidates.push(defaultPlatform);
        }
      }
      const platforms = await backend.listBusinessPlatforms(
        syncCandidates
      );
      state.lessons.forEach((lesson) => {
        const previous = syncCandidates.find(
          (platform) => platform.id === lesson.businessPlatformId
        );
        const replacement = platforms.find(
          (platform) =>
            platform.id === lesson.businessPlatformId ||
            (previous && platform.code === previous.code)
        );
        if (replacement) {
          lesson.businessPlatformId = replacement.id;
          const previousModule = previous?.modules.find(
            (businessModule) =>
              businessModule.id === lesson.businessPlatformModuleId
          );
          const replacementModule = replacement.modules.find(
            (businessModule) =>
              businessModule.id === lesson.businessPlatformModuleId ||
              (previousModule &&
                businessModule.code.trim().toUpperCase() ===
                  previousModule.code.trim().toUpperCase())
          );
          if (replacementModule) {
            lesson.businessPlatformModuleId = replacementModule.id;
          }
        }
      });
      state.businessPlatforms.splice(
        0,
        state.businessPlatforms.length,
        ...toPlain(platforms)
      );
      persist();
      return state.businessPlatforms;
    });
  }

  async function createBusinessPlatformRemote(
    input: Pick<BusinessPlatform, 'code' | 'name' | 'baseUrl'> &
      Partial<Pick<BusinessPlatform, 'description' | 'status'>>
  ): Promise<BusinessPlatform> {
    if (!backend.isEnabled()) return createBusinessPlatform(input);
    const code = input.code.trim().toUpperCase();
    const name = input.name.trim();
    const baseUrl = input.baseUrl.trim();
    if (!code || !name || !baseUrl) {
      throw new Error('平台编码、平台名称和访问地址不能为空');
    }
    if (state.businessPlatforms.some((platform) => platform.code === code)) {
      throw new Error(`业务平台编码已存在：${code}`);
    }
    return runRemote('新增业务平台', async () => {
      const created = await backend.createBusinessPlatform({
        ...input,
        code,
        name,
        baseUrl
      });
      state.businessPlatforms.unshift(toPlain(created));
      addActivity(
        'BUSINESS_PLATFORM_CREATED',
        `新增业务平台：${created.name}`,
        created.baseUrl
      );
      persist();
      return requireBusinessPlatform(created.id);
    });
  }

  async function updateBusinessPlatformRemote(
    platformId: string,
    patch: Partial<Omit<BusinessPlatform, 'id' | 'updatedAt' | 'modules'>>
  ): Promise<BusinessPlatform> {
    if (!backend.isEnabled()) return updateBusinessPlatform(platformId, patch);
    const platform = requireBusinessPlatform(platformId);
    const next: BusinessPlatform = {
      ...toPlain(platform),
      ...toPlain(patch),
      id: platform.id,
      code: patch.code?.trim().toUpperCase() ?? platform.code,
      name: patch.name?.trim() ?? platform.name,
      baseUrl: patch.baseUrl?.trim() ?? platform.baseUrl,
      description:
        patch.description !== undefined
          ? patch.description.trim()
          : platform.description,
      updatedAt: now()
    };
    if (!next.code || !next.name || !next.baseUrl) {
      throw new Error('平台编码、平台名称和访问地址不能为空');
    }
    if (
      state.businessPlatforms.some(
        (candidate) =>
          candidate.id !== platformId && candidate.code === next.code
      )
    ) {
      throw new Error(`业务平台编码已存在：${next.code}`);
    }
    return runRemote('更新业务平台', async () => {
      const saved = await backend.updateBusinessPlatform(next);
      Object.assign(platform, next, {
        name: saved.name,
        baseUrl: saved.baseUrl,
        status: saved.status,
        updatedAt: saved.updatedAt
      });
      addActivity(
        'BUSINESS_PLATFORM_UPDATED',
        `更新业务平台：${platform.name}`,
        platform.baseUrl
      );
      persist();
      return platform;
    });
  }

  async function setBusinessPlatformStatusRemote(
    platformId: string,
    status: BusinessPlatform['status']
  ): Promise<BusinessPlatform> {
    if (!backend.isEnabled()) {
      return updateBusinessPlatform(platformId, { status });
    }
    const platform = requireBusinessPlatform(platformId);
    return runRemote(
      status === 'ENABLED' ? '启用业务平台' : '停用业务平台',
      async () => {
        const saved = await backend.setBusinessPlatformStatus(platform, status);
        platform.status = saved.status;
        platform.updatedAt = saved.updatedAt;
        addActivity(
          'BUSINESS_PLATFORM_STATUS_UPDATED',
          `${status === 'ENABLED' ? '启用' : '停用'}业务平台：${platform.name}`,
          platform.code
        );
        persist();
        return platform;
      }
    );
  }

  async function createBusinessPlatformModuleRemote(
    platformId: string,
    input: Pick<BusinessPlatformModule, 'code' | 'name' | 'path'> &
      Partial<Pick<BusinessPlatformModule, 'description' | 'status'>>
  ): Promise<BusinessPlatformModule> {
    if (!backend.isEnabled()) {
      return createBusinessPlatformModule(platformId, input);
    }
    const platform = requireBusinessPlatform(platformId);
    const before = toPlain(platform);
    const beforeActivities = toPlain(state.activities);
    const created = createBusinessPlatformModule(platformId, input);
    try {
      await runRemote('保存平台模块', () =>
        backend.updateBusinessPlatform(toPlain(platform))
      );
      return created;
    } catch (error) {
      Object.assign(platform, before);
      state.activities.splice(
        0,
        state.activities.length,
        ...beforeActivities
      );
      persist();
      throw error;
    }
  }

  async function updateBusinessPlatformModuleRemote(
    platformId: string,
    moduleId: string,
    patch: Partial<Omit<BusinessPlatformModule, 'id' | 'updatedAt'>>
  ): Promise<BusinessPlatformModule> {
    if (!backend.isEnabled()) {
      return updateBusinessPlatformModule(platformId, moduleId, patch);
    }
    const platform = requireBusinessPlatform(platformId);
    const before = toPlain(platform);
    const beforeActivities = toPlain(state.activities);
    const updated = updateBusinessPlatformModule(platformId, moduleId, patch);
    try {
      await runRemote('更新平台模块', () =>
        backend.updateBusinessPlatform(toPlain(platform))
      );
      return updated;
    } catch (error) {
      Object.assign(platform, before);
      state.activities.splice(
        0,
        state.activities.length,
        ...beforeActivities
      );
      persist();
      throw error;
    }
  }

  async function removeBusinessPlatformModuleRemote(
    platformId: string,
    moduleId: string
  ): Promise<BusinessPlatformModule> {
    if (!backend.isEnabled()) {
      return removeBusinessPlatformModule(platformId, moduleId);
    }
    const platform = requireBusinessPlatform(platformId);
    const before = toPlain(platform);
    const beforeActivities = toPlain(state.activities);
    const removed = removeBusinessPlatformModule(platformId, moduleId);
    try {
      await runRemote('删除平台模块', () =>
        backend.updateBusinessPlatform(toPlain(platform))
      );
      return removed;
    } catch (error) {
      Object.assign(platform, before);
      state.activities.splice(
        0,
        state.activities.length,
        ...beforeActivities
      );
      persist();
      throw error;
    }
  }

  function createBusinessPlatform(
    input: Pick<BusinessPlatform, 'code' | 'name' | 'baseUrl'> &
      Partial<Pick<BusinessPlatform, 'description' | 'status'>>
  ): BusinessPlatform {
    const code = input.code.trim().toUpperCase();
    const name = input.name.trim();
    const baseUrl = input.baseUrl.trim();
    if (!code || !name || !baseUrl) {
      throw new Error('平台编码、平台名称和访问地址不能为空');
    }
    if (state.businessPlatforms.some((platform) => platform.code === code)) {
      throw new Error(`业务平台编码已存在：${code}`);
    }
    const created: BusinessPlatform = {
      id: idFactory('business-platform'),
      code,
      name,
      baseUrl,
      description: input.description?.trim() ?? '',
      status: input.status ?? 'ENABLED',
      modules: [],
      updatedAt: now()
    };
    state.businessPlatforms.unshift(created);
    addActivity('BUSINESS_PLATFORM_CREATED', `新增业务平台：${name}`, baseUrl);
    persist();
    return requireBusinessPlatform(created.id);
  }

  function updateBusinessPlatform(
    platformId: string,
    patch: Partial<Omit<BusinessPlatform, 'id' | 'updatedAt' | 'modules'>>
  ): BusinessPlatform {
    const platform = requireBusinessPlatform(platformId);
    const nextCode = patch.code?.trim().toUpperCase() ?? platform.code;
    const nextName = patch.name?.trim() ?? platform.name;
    const nextBaseUrl = patch.baseUrl?.trim() ?? platform.baseUrl;
    if (!nextCode || !nextName || !nextBaseUrl) {
      throw new Error('平台编码、平台名称和访问地址不能为空');
    }
    if (
      state.businessPlatforms.some(
        (candidate) => candidate.id !== platformId && candidate.code === nextCode
      )
    ) {
      throw new Error(`业务平台编码已存在：${nextCode}`);
    }
    const normalizedPatch = {
      ...toPlain(patch),
      ...(patch.code !== undefined ? { code: nextCode } : {}),
      ...(patch.name !== undefined ? { name: nextName } : {}),
      ...(patch.baseUrl !== undefined ? { baseUrl: nextBaseUrl } : {}),
      ...(patch.description !== undefined
        ? { description: patch.description.trim() }
        : {})
    };
    Object.assign(platform, normalizedPatch, {
      id: platformId,
      updatedAt: now()
    });
    addActivity(
      'BUSINESS_PLATFORM_UPDATED',
      `更新业务平台：${platform.name}`,
      platform.baseUrl
    );
    persist();
    return platform;
  }

  function removeBusinessPlatform(platformId: string) {
    const platform = requireBusinessPlatform(platformId);
    if (
      state.lessons.some((lesson) => lesson.businessPlatformId === platformId)
    ) {
      throw new Error('该业务平台已被教案使用，请停用平台而不要删除');
    }
    state.businessPlatforms.splice(
      state.businessPlatforms.findIndex((candidate) => candidate.id === platformId),
      1
    );
    addActivity('BUSINESS_PLATFORM_REMOVED', `删除业务平台：${platform.name}`, platform.code);
    persist();
    return platform;
  }

  function createBusinessPlatformModule(
    platformId: string,
    input: Pick<BusinessPlatformModule, 'code' | 'name' | 'path'> &
      Partial<Pick<BusinessPlatformModule, 'description' | 'status'>>
  ): BusinessPlatformModule {
    const platform = requireBusinessPlatform(platformId);
    const code = input.code.trim().toUpperCase();
    const name = input.name.trim();
    const path = normalizeBusinessModulePath(input.path);
    if (!code || !name || !path) {
      throw new Error('模块编码、模块名称和模块路径不能为空');
    }
    if (platform.modules.some((businessModule) => businessModule.code === code)) {
      throw new Error(`“${platform.name}”中已存在模块编码：${code}`);
    }
    const created: BusinessPlatformModule = {
      id: idFactory('business-module'),
      code,
      name,
      path,
      description: input.description?.trim() ?? '',
      status: input.status ?? 'ENABLED',
      updatedAt: now()
    };
    platform.modules.push(created);
    platform.updatedAt = created.updatedAt;
    addActivity(
      'BUSINESS_PLATFORM_MODULE_CREATED',
      `新增平台模块：${name}`,
      `${platform.name} · ${path}`
    );
    persist();
    return requireBusinessPlatformModule(platformId, created.id);
  }

  function updateBusinessPlatformModule(
    platformId: string,
    moduleId: string,
    patch: Partial<Omit<BusinessPlatformModule, 'id' | 'updatedAt'>>
  ): BusinessPlatformModule {
    const platform = requireBusinessPlatform(platformId);
    const businessModule = requireBusinessPlatformModule(platformId, moduleId);
    const nextCode = patch.code?.trim().toUpperCase() ?? businessModule.code;
    const nextName = patch.name?.trim() ?? businessModule.name;
    const nextPath =
      patch.path !== undefined
        ? normalizeBusinessModulePath(patch.path)
        : businessModule.path;
    if (!nextCode || !nextName || !nextPath) {
      throw new Error('模块编码、模块名称和模块路径不能为空');
    }
    if (
      platform.modules.some(
        (candidate) =>
          candidate.id !== moduleId && candidate.code === nextCode
      )
    ) {
      throw new Error(`“${platform.name}”中已存在模块编码：${nextCode}`);
    }
    Object.assign(
      businessModule,
      toPlain(patch),
      {
        code: nextCode,
        name: nextName,
        path: nextPath,
        ...(patch.description !== undefined
          ? { description: patch.description.trim() }
          : {}),
        id: moduleId,
        updatedAt: now()
      }
    );
    platform.updatedAt = businessModule.updatedAt;
    addActivity(
      'BUSINESS_PLATFORM_MODULE_UPDATED',
      `更新平台模块：${businessModule.name}`,
      platform.name
    );
    persist();
    return businessModule;
  }

  function removeBusinessPlatformModule(platformId: string, moduleId: string) {
    const platform = requireBusinessPlatform(platformId);
    const businessModule = requireBusinessPlatformModule(platformId, moduleId);
    if (
      state.lessons.some(
        (lesson) => lesson.businessPlatformModuleId === moduleId
      )
    ) {
      throw new Error('该平台模块已被教案使用，请停用模块而不要删除');
    }
    platform.modules.splice(
      platform.modules.findIndex((candidate) => candidate.id === moduleId),
      1
    );
    platform.updatedAt = now();
    addActivity(
      'BUSINESS_PLATFORM_MODULE_REMOVED',
      `删除平台模块：${businessModule.name}`,
      platform.name
    );
    persist();
    return businessModule;
  }

  function refreshPublishedTaskStatuses(): PublishedTask[] {
    let changed = false;
    state.publishedTasks.forEach((task) => {
      const status = resolvePublishedStatus(task);
      if (task.status !== status) {
        task.status = status;
        changed = true;
      }
    });
    if (changed) persist();
    return state.publishedTasks;
  }

  function getLesson(lessonId: string) {
    return state.lessons.find((lesson) => lesson.id === lessonId);
  }

  function currentLesson(lessonId: string) {
    return getLesson(lessonId);
  }

  function createLesson(input: Partial<LessonPlan> = {}): LessonPlan {
    const timestamp = now();
    const lessonId = idFactory('lesson');
    const businessPlatformId =
      input.businessPlatformId ??
      state.businessPlatforms.find((platform) => platform.status === 'ENABLED')?.id ??
      '';
    const businessPlatform = requireBusinessPlatform(businessPlatformId);
    if (businessPlatform.status !== 'ENABLED') {
      throw new Error('请选择已启用的业务平台');
    }
    const businessPlatformModuleId =
      input.businessPlatformModuleId ??
      businessPlatform.modules.find(
        (businessModule) => businessModule.status === 'ENABLED'
      )?.id ??
      '';
    const businessPlatformModule = requireBusinessPlatformModule(
      businessPlatformId,
      businessPlatformModuleId
    );
    if (businessPlatformModule.status !== 'ENABLED') {
      throw new Error('请选择已启用的平台模块');
    }
    const created: LessonPlan = {
      id: lessonId,
      code: input.code?.trim() || `LESSON-${state.lessons.length + 1}`,
      title: input.title?.trim() || '未命名实训教案',
      moduleName: input.moduleName?.trim() || businessPlatformModule.name,
      businessPlatformId,
      businessPlatformModuleId,
      description: input.description?.trim() || '',
      version: 1,
      status: 'DRAFT',
      teacherName:
        input.teacherName?.trim() ||
        workspaceSession?.user.displayName.trim() ||
        authApi.getSession()?.user.displayName.trim() ||
        workspaceSession?.user.username ||
        authApi.getSession()?.user.username ||
        '当前教师',
      tags: input.tags ? [...input.tags] : [],
      objectiveMaxScore: input.objectiveMaxScore ?? 80,
      subjectiveMaxScore: input.subjectiveMaxScore ?? 20,
      stages: input.stages ? toPlain(input.stages) : [],
      updatedAt: timestamp
    };
    state.lessons.unshift(created);
    state.dataItems[lessonId] = [];
    addActivity('LESSON_CREATED', `新建教案：${created.title}`, created.code);
    persist();
    return requireLesson(lessonId);
  }

  async function createLessonRemote(
    input: Partial<LessonPlan> = {}
  ): Promise<LessonPlan> {
    return persistLessonMutation(() => createLesson(input));
  }

  function duplicateLesson(lessonId: string): LessonPlan {
    const source = requireLesson(lessonId);
    const timestamp = now();
    const copiedId = idFactory('lesson');
    const copied: LessonPlan = {
      ...toPlain(source),
      id: copiedId,
      code: uniqueLessonCode(`${source.code}-COPY`),
      title: `${source.title}（副本）`,
      version: 1,
      status: 'DRAFT',
      stages: source.stages.map((stage) => ({
        ...toPlain(stage),
        id: idFactory('stage'),
        stageKey: `${stage.stageKey}-copy`,
        recordedSteps: stage.recordedSteps.map((step) => ({
          ...toPlain(step),
          id: idFactory('record'),
          syncStatus: 'LOCAL',
          syncError: undefined,
          remoteEventId: undefined,
          remoteResourceSnapshotId: undefined,
          remoteDraftId: undefined,
          remoteResourceId: undefined
        }))
      })),
      updatedAt: timestamp,
      publishedAt: undefined,
      captureSessionId: undefined,
      captureSessionFinished: undefined,
      teachingPointId: undefined,
      remoteCourseId: undefined,
      lectureCompletedAt: undefined
    };
    state.lessons.unshift(copied);
    state.dataItems[copiedId] = [];
    addActivity(
      'LESSON_DUPLICATED',
      `复制教案：${source.title}`,
      `新教案：${copied.title}`
    );
    persist();
    return requireLesson(copiedId);
  }

  async function duplicateLessonRemote(lessonId: string): Promise<LessonPlan> {
    return persistLessonMutation(() => duplicateLesson(lessonId));
  }

  async function persistLessonMutation(
    mutate: () => LessonPlan
  ): Promise<LessonPlan> {
    if (!backend.isEnabled()) return mutate();
    if (!workspaceUserKey || !workspaceSession) {
      throw new Error('登录用户工作区尚未加载，请刷新页面后重试');
    }
    const previousState = toPlain(state);
    try {
      const lesson = mutate();
      await flushAuthenticatedWorkspace();
      return requireLesson(lesson.id);
    } catch (error) {
      if (workspaceSaveTimer) {
        clearTimeout(workspaceSaveTimer);
        workspaceSaveTimer = undefined;
      }
      replaceState(previousState);
      api.saveState(previousState);
      throw error;
    }
  }

  function updateLesson(
    lessonId: string,
    patch: Partial<Omit<LessonPlan, 'id' | 'stages'>>
  ): LessonPlan {
    assertLessonConfigurationMutable(lessonId);
    const lesson = requireLesson(lessonId);
    if (
      patch.businessPlatformId !== undefined ||
      patch.businessPlatformModuleId !== undefined
    ) {
      const nextPlatformId =
        patch.businessPlatformId ?? lesson.businessPlatformId;
      const nextModuleId =
        patch.businessPlatformModuleId ?? lesson.businessPlatformModuleId;
      const platform = requireBusinessPlatform(nextPlatformId);
      const businessModule = requireBusinessPlatformModule(
        nextPlatformId,
        nextModuleId
      );
      if (
        platform.status !== 'ENABLED' ||
        businessModule.status !== 'ENABLED'
      ) {
        throw new Error('教案只能绑定已启用的业务平台和平台模块');
      }
    }
    const stableId = lesson.id;
    Object.assign(lesson, toPlain(patch), { id: stableId, updatedAt: now() });
    addActivity('LESSON_UPDATED', `更新教案：${lesson.title}`, '教案元数据已保存');
    persist();
    return lesson;
  }

  function addStage(
    lessonId: string,
    input: Partial<LessonStage> = {}
  ): LessonStage {
    assertLessonConfigurationMutable(lessonId);
    const lesson = requireLesson(lessonId);
    const stageId = idFactory('stage');
    const order = lesson.stages.length + 1;
    const created: LessonStage = {
      id: stageId,
      stageKey: input.stageKey?.trim() || `stage-${order}`,
      name: input.name?.trim() || `教学点 ${order}`,
      groupKey: input.groupKey?.trim() || '',
      description: input.description?.trim() || '',
      required: input.required ?? true,
      score: input.score ?? 0,
      completionMethod: input.completionMethod ?? 'mixed',
      visibility: input.visibility
        ? toPlain(input.visibility)
        : { LEARNING: true, PRACTICE: true, EXAM: true },
      recordedSteps: input.recordedSteps ? toPlain(input.recordedSteps) : []
    };
    lesson.stages.push(created);
    lesson.updatedAt = now();
    addActivity('LESSON_STAGE_ADDED', `新增教学点：${created.name}`, lesson.title);
    persist();
    return lesson.stages.find((stage) => stage.id === stageId)!;
  }

  function updateStage(
    lessonId: string,
    stageId: string,
    patch: Partial<Omit<LessonStage, 'id'>>
  ): LessonStage {
    assertLessonConfigurationMutable(lessonId);
    const lesson = requireLesson(lessonId);
    const stage = requireStage(lesson, stageId);
    Object.assign(stage, toPlain(patch), { id: stageId });
    lesson.updatedAt = now();
    addActivity('LESSON_STAGE_UPDATED', `更新教学点：${stage.name}`, lesson.title);
    persist();
    return stage;
  }

  function removeStage(lessonId: string, stageId: string) {
    assertLessonConfigurationMutable(lessonId);
    const lesson = requireLesson(lessonId);
    const index = lesson.stages.findIndex((stage) => stage.id === stageId);
    if (index < 0) throw new Error(`未找到教学点：${stageId}`);
    const [removed] = lesson.stages.splice(index, 1);
    const plan = state.groupPlans[lessonId];
    plan?.roles.forEach((role) => {
      role.stageIds = role.stageIds.filter((id) => id !== stageId);
    });
    lesson.updatedAt = now();
    addActivity('LESSON_STAGE_REMOVED', `删除教学点：${removed.name}`, lesson.title);
    persist();
    return removed;
  }

  function moveStage(
    lessonId: string,
    stageId: string,
    target: 'up' | 'down' | number
  ): LessonStage[] {
    assertLessonConfigurationMutable(lessonId);
    const lesson = requireLesson(lessonId);
    const from = lesson.stages.findIndex((stage) => stage.id === stageId);
    if (from < 0) throw new Error(`未找到教学点：${stageId}`);
    const desired =
      target === 'up' ? from - 1 : target === 'down' ? from + 1 : target;
    const to = Math.max(0, Math.min(lesson.stages.length - 1, desired));
    if (from !== to) {
      const [stage] = lesson.stages.splice(from, 1);
      lesson.stages.splice(to, 0, stage);
      lesson.updatedAt = now();
    addActivity('LESSON_STAGE_MOVED', `调整教学点顺序：${stage.name}`, lesson.title);
      persist();
    }
    return lesson.stages;
  }

  function validateLesson(lessonId: string): string[] {
    const lesson = requireLesson(lessonId);
    const issues: string[] = [];
    if (!lesson.title.trim()) issues.push('教案名称不能为空');
    if (!lesson.moduleName.trim()) issues.push('业务模块不能为空');
    const platform = getBusinessPlatform(lesson.businessPlatformId);
    if (!platform) {
      issues.push('教案必须绑定有效的业务平台');
    } else if (platform.status !== 'ENABLED') {
      issues.push('教案绑定的业务平台已停用');
    } else if (!platform.baseUrl.trim()) {
      issues.push('教案绑定的业务平台尚未配置访问地址');
    } else {
      const businessModule = getBusinessPlatformModule(
        platform.id,
        lesson.businessPlatformModuleId
      );
      if (!businessModule) {
        issues.push('教案必须绑定当前业务平台下的有效模块');
      } else if (businessModule.status !== 'ENABLED') {
        issues.push('教案绑定的平台模块已停用');
      } else if (!businessModule.path.trim()) {
        issues.push('教案绑定的平台模块尚未配置访问路径');
      }
    }
    if (lesson.stages.length === 0) issues.push('教案至少需要一个教学点');
    if (lesson.stages.some((stage) => !stage.groupKey.trim())) {
      issues.push('每个教学点必须指定负责角色组');
    }
    if (lesson.stages.some((stage) => stage.recordedSteps.length === 0)) {
      issues.push('每个教学点至少需要一个录制步骤');
    }
    if (
      !Number.isFinite(lesson.objectiveMaxScore) ||
      !Number.isFinite(lesson.subjectiveMaxScore) ||
      lesson.objectiveMaxScore < 0 ||
      lesson.subjectiveMaxScore < 0 ||
      lesson.objectiveMaxScore + lesson.subjectiveMaxScore !== 100
    ) {
      issues.push('教案客观分与主观分合计必须为 100');
    }
    if (lesson.stages.some((stage) => !Number.isFinite(stage.score) || stage.score < 0)) {
      issues.push('教学点分值必须为非负数');
    }
    const stageScore = lesson.stages.reduce((total, stage) => total + stage.score, 0);
    if (stageScore !== lesson.objectiveMaxScore) {
      issues.push('教学点客观分合计必须等于教案客观分');
    }
    return [...new Set(issues)];
  }

  function publishLesson(lessonId: string): LessonPlan {
    const lesson = requireLesson(lessonId);
    const issues = validateLesson(lessonId);
    if (issues.length) throw new TrainingValidationError(issues);
    if (lesson.status === 'PUBLISHED') lesson.version += 1;
    lesson.status = 'PUBLISHED';
    lesson.publishedAt = now();
    lesson.updatedAt = lesson.publishedAt;
    addActivity(
      'LESSON_PUBLISHED',
      `发布教案：${lesson.title}`,
      `版本 V${lesson.version}`
    );
    persist();
    return lesson;
  }

  function markLessonLectureCompleted(lessonId: string): LessonPlan {
    const lesson = requireLesson(lessonId);
    if (lesson.status !== 'PUBLISHED') {
      throw new Error('请先完成教案备案并发布，再进行教师讲解');
    }
    lesson.lectureCompletedAt = now();
    lesson.updatedAt = lesson.lectureCompletedAt;
    addActivity(
      'LESSON_LECTURE_COMPLETED',
      `完成教师讲解：${lesson.title}`,
      '已按备案步骤完整讲解，可发布学习与练习任务'
    );
    persist();
    return lesson;
  }

  async function startCaptureSessionRemote(
    lessonId: string,
    startUrl: string
  ): Promise<string> {
    let lesson = requireLesson(lessonId);
    if (!backend.isEnabled()) return lesson.captureSessionId ?? '';
    if (lesson.captureSessionId && !lesson.captureSessionFinished) {
      return lesson.captureSessionId;
    }
    await syncBusinessPlatforms();
    lesson = requireLesson(lessonId);
    const platform = requireBusinessPlatform(lesson.businessPlatformId);
    return runRemote('创建备案采集会话', async () => {
      const captureSessionId = await backend.startCaptureSession(
        toPlain(lesson),
        toPlain(platform),
        startUrl
      );
      lesson.captureSessionId = captureSessionId;
      lesson.captureSessionFinished = false;
      lesson.updatedAt = now();
      persist();
      return captureSessionId;
    });
  }

  async function syncRecordedStep(
    lessonId: string,
    stageId: string,
    stepId: string
  ): Promise<RecordedStep> {
    const lesson = requireLesson(lessonId);
    const stage = requireStage(lesson, stageId);
    const step = stage.recordedSteps.find(
      (candidate) => candidate.id === stepId
    );
    if (!step) throw new Error(`未找到录制步骤：${stepId}`);
    if (!backend.isEnabled()) {
      step.syncStatus = 'LOCAL';
      persist();
      return step;
    }
    if (step.remoteDraftId && step.syncStatus === 'SYNCED') return step;

    step.syncStatus = 'SYNCING';
    delete step.syncError;
    persist();
    const sequenceNo =
      lesson.stages
        .slice(0, lesson.stages.indexOf(stage))
        .reduce((total, candidate) => total + candidate.recordedSteps.length, 0) +
      stage.recordedSteps.indexOf(step) +
      1;
    try {
      const binding = await runRemote('上报录制步骤', () =>
        backend.reportRecordedStep(
          toPlain(lesson),
          toPlain(stage),
          toPlain(step),
          sequenceNo
        )
      );
      step.remoteEventId = binding.eventId;
      step.remoteResourceSnapshotId = binding.resourceSnapshotId;
      step.remoteDraftId = binding.draftId;
      step.syncStatus = 'SYNCED';
      delete step.syncError;
      persist();
      return step;
    } catch (error) {
      step.syncStatus = 'FAILED';
      step.syncError =
        error instanceof Error ? error.message : '录制步骤上报失败';
      persist();
      throw error;
    }
  }

  async function publishLessonRemote(lessonId: string): Promise<LessonPlan> {
    const lesson = requireLesson(lessonId);
    const issues = validateLesson(lessonId);
    if (issues.length) throw new TrainingValidationError(issues);
    if (!backend.isEnabled()) return publishLesson(lessonId);
    if (lesson.status === 'PUBLISHED' && lesson.teachingPointId) {
      return lesson;
    }
    const platform = requireBusinessPlatform(lesson.businessPlatformId);

    for (const stage of lesson.stages) {
      for (const step of stage.recordedSteps) {
        if (!step.remoteDraftId || step.syncStatus !== 'SYNCED') {
          await syncRecordedStep(lessonId, stage.id, step.id);
        }
      }
    }

    const binding = await runRemote('发布教学点', () =>
      backend.publishLesson(toPlain(lesson), toPlain(platform))
    );
    lesson.teachingPointId = binding.teachingPointId;
    lesson.captureSessionFinished =
      lesson.captureSessionFinished || binding.finishedCaptureSession;
    lesson.stages.forEach((stage) => {
      stage.recordedSteps.forEach((step) => {
        const resourceId = binding.resourceIdsByStepId[step.id];
        if (resourceId) step.remoteResourceId = resourceId;
      });
    });
    persist();
    const publishedLesson =
      lesson.status === 'PUBLISHED' ? lesson : publishLesson(lessonId);
    if (lesson.status === 'PUBLISHED') {
      lesson.publishedAt ??= now();
      lesson.updatedAt = now();
      persist();
    }
    await saveAuthenticatedWorkspace();
    return publishedLesson;
  }

  function ensureSimulatedModeTask(
    lessonId: string,
    mode: 'LEARNING' | 'PRACTICE'
  ): PublishedTask {
    const existing = state.publishedTasks.find(
      (task) => task.lessonId === lessonId && task.mode === mode
    );
    const lesson = requireLesson(lessonId);
    const config = getApiConfig();
    const startAt = new Date(Date.parse(now()) - 60_000).toISOString();
    const endAt = new Date(
      Date.parse(startAt) + 30 * 24 * 60 * 60 * 1000
    ).toISOString();
    const id = existing?.id ?? idFactory('published-task');
    const title =
      mode === 'LEARNING'
        ? `${lesson.title}｜流程学习`
        : `${lesson.title}｜流程练习`;
    const published: PublishedTask = existing ?? {
      id,
      lessonId,
      title,
      mode,
      status: 'RUNNING',
      startAt,
      endAt,
      assignedCount: 0,
      groupCount: new Set(
        lesson.stages.map((stage) => stage.groupKey).filter(Boolean)
      ).size,
      dataCount: 0,
      completedCount: 0,
      syncStatus: backend.isEnabled() ? 'SYNCING' : 'LOCAL'
    };
    const membersByStudent = new Map<
      string,
      {
        studentId: string;
        studentName: string;
        unitId: string;
        unitName: string;
      }
    >();
    (state.groupPlans[lessonId]?.members ?? []).forEach((member) =>
      membersByStudent.set(member.studentId, member)
    );
    if (!membersByStudent.size && !backend.isEnabled()) {
      membersByStudent.set(config.simulatedStudentId, {
        studentId: config.simulatedStudentId,
        studentName: config.simulatedStudentName,
        unitId: config.simulatedOrgId,
        unitName: '模拟班级'
      });
    }
    const allGroupKeys = [
      ...new Set(lesson.stages.map((stage) => stage.groupKey).filter(Boolean))
    ];
    const assignedStudentIds = new Set(membersByStudent.keys());
    state.studentTasks = state.studentTasks.filter(
      (task) =>
        task.publishedTaskId !== id ||
        task.mode !== mode ||
        assignedStudentIds.has(task.studentId)
    );
    membersByStudent.forEach((member) => {
      const existingAssignment = state.studentTasks.find(
        (task) =>
          task.publishedTaskId === id &&
          task.mode === mode &&
          task.studentId === member.studentId
      );
      if (existingAssignment) {
        existingAssignment.studentName = member.studentName;
        existingAssignment.groupKey = allGroupKeys[0] ?? 'all';
        existingAssignment.groupKeys = allGroupKeys;
        existingAssignment.unitId = member.unitId;
        existingAssignment.unitName = member.unitName;
        return;
      }
      state.studentTasks.push({
        id: idFactory('student-task'),
        publishedTaskId: id,
        lessonId,
        studentId: member.studentId,
        studentName: member.studentName,
        title,
        mode,
        groupKey: allGroupKeys[0] ?? 'all',
        groupKeys: allGroupKeys,
        unitId: member.unitId,
        unitName: member.unitName,
        dataItemId: `training-${mode.toLowerCase()}-${lessonId}-${member.studentId}`,
        attemptNumber: 1,
        submissionValues: {},
        status: 'TODO',
        currentStageIndex: 0,
        completedStageIds: [],
        syncStatus: backend.isEnabled() ? 'SYNCING' : 'LOCAL'
      });
    });
    published.assignedCount = membersByStudent.size;
    if (!existing) state.publishedTasks.unshift(published);
    addActivity(
      'TRAINING_TASK_CREATED',
      `生成${mode === 'LEARNING' ? '学习' : '练习'}任务：${title}`,
      `已分配 ${membersByStudent.size} 名真实学员`
    );
    persist();
    return published;
  }

  async function publishModeTaskRemote(
    lessonId: string,
    mode: RunMode,
    published: PublishedTask
  ): Promise<PublishedTask> {
    const lesson = requireLesson(lessonId);
    if (!backend.isEnabled()) {
      published.syncStatus = 'LOCAL';
      persist();
      return published;
    }
    if (
      published.remoteTaskId &&
      published.syncStatus === 'SYNCED'
    ) {
      return published;
    }
    const platform = requireBusinessPlatform(lesson.businessPlatformId);
    published.syncStatus = 'SYNCING';
    delete published.syncError;
    persist();
    try {
      const binding = await runRemote(`发布${mode}任务`, () =>
        backend.publishTeachingTask(
          toPlain(lesson),
          toPlain(platform),
          mode,
          {
            title: published.title,
            startAt: published.startAt,
            endAt: published.endAt,
            timeLimitMinutes:
              mode === 'EXAM'
                ? state.examSettings[lessonId]?.durationMinutes
                : mode === 'LEARNING'
                  ? 60
                  : 120
          }
        )
      );
      lesson.remoteCourseId = binding.courseId;
      published.remoteCourseId = binding.courseId;
      published.remoteTaskId = binding.taskId;
      published.remoteTeachingPointId = binding.teachingPointId;
      published.remoteEvaluationRuleId = binding.evaluationRuleId;
      published.remoteTaskStepIdsByStepId =
        binding.taskStepIdsByStepId;
      if (mode !== 'LEARNING') {
        const assignedStudentTasks = state.studentTasks.filter(
          (task) =>
            task.publishedTaskId === published.id && task.mode === mode
        );
        published.dataCount = await runRemote('自动准备原平台初始数据', () =>
          backend.prepareInitialDataForPublishedTask(
            toPlain(lesson),
            toPlain(platform),
            toPlain(published),
            toPlain(assignedStudentTasks)
          )
        );
      }
      published.syncStatus = 'SYNCED';
      delete published.syncError;
      state.studentTasks
        .filter(
          (task) =>
            task.publishedTaskId === published.id && task.mode === mode
        )
        .forEach((task) => {
          task.syncStatus = 'SYNCED';
          delete task.syncError;
        });
      persist();
      return published;
    } catch (error) {
      published.syncStatus = 'FAILED';
      published.syncError =
        error instanceof Error ? error.message : `${mode}任务发布失败`;
      state.studentTasks
        .filter(
          (task) =>
            task.publishedTaskId === published.id && task.mode === mode
        )
        .forEach((task) => {
          task.syncStatus = 'FAILED';
          task.syncError = published.syncError;
        });
      persist();
      throw error;
    }
  }

  async function publishLearningAndPracticeRemote(
    lessonId: string
  ): Promise<PublishedTask[]> {
    let lesson = requireLesson(lessonId);
    if (lesson.status !== 'PUBLISHED') {
      throw new Error('请先完成教案备案并发布');
    }
    if (!lesson.teachingPointId && backend.isEnabled()) {
      await publishLessonRemote(lessonId);
      lesson = requireLesson(lessonId);
    }
    if (!lesson.teachingPointId) {
      throw new Error('教案教学点尚未发布，请重新执行备案发布');
    }
    if (!lesson.lectureCompletedAt) {
      throw new Error('请先在教师讲解页完整讲解一遍备案流程');
    }
    if (
      backend.isEnabled() &&
      !(state.groupPlans[lessonId]?.members.length)
    ) {
      throw new Error('请先在分组设置中选择真实学生账号，再发布学习与练习任务');
    }
    if (backend.isEnabled() && authApi.getSession()) {
      const realStudents = await usersApi.listStudents();
      const realStudentIds = new Set(
        realStudents.map((student) => student.studentId)
      );
      const invalidMembers = (
        state.groupPlans[lessonId]?.members ?? []
      ).filter((member) => !realStudentIds.has(member.studentId));
      if (invalidMembers.length) {
        throw new Error(
          `分组中有 ${invalidMembers.length} 个演示或已失效账号，请进入分组设置重新选择真实学生`
        );
      }
    }
    const learning = ensureSimulatedModeTask(lessonId, 'LEARNING');
    const practice = ensureSimulatedModeTask(lessonId, 'PRACTICE');
    await publishModeTaskRemote(lessonId, 'LEARNING', learning);
    await publishModeTaskRemote(lessonId, 'PRACTICE', practice);
    await saveAuthenticatedWorkspace();
    return [learning, practice];
  }

  function saveExamSettings(
    lessonId: string,
    input: ExamSettings | Omit<ExamSettings, 'lessonId'>
  ): ExamSettings {
    assertLessonConfigurationMutable(lessonId);
    requireLesson(lessonId);
    const settings: ExamSettings = { ...toPlain(input), lessonId };
    const issues: string[] = [];
    if (!settings.batchName.trim()) issues.push('考试名称不能为空');
    if (
      settings.objectiveWeight < 0 ||
      settings.subjectiveWeight < 0 ||
      settings.objectiveWeight + settings.subjectiveWeight !== 100
    ) {
      issues.push('考试评分权重合计必须为 100');
    }
    if (!Number.isInteger(settings.durationMinutes) || settings.durationMinutes <= 0) {
      issues.push('考试时长必须为正整数');
    }
    if (!Number.isInteger(settings.maxAttempts) || settings.maxAttempts < 1) {
      issues.push('最大作答次数至少为 1');
    }
    if (
      !isDate(settings.startAt) ||
      !isDate(settings.endAt) ||
      Date.parse(settings.endAt) <= Date.parse(settings.startAt)
    ) {
      issues.push('考试结束时间必须晚于开始时间');
    }
    if (issues.length) throw new TrainingValidationError(issues);
    state.examSettings[lessonId] = settings;
    addActivity('EXAM_SETTINGS_SAVED', `保存考试设置：${settings.batchName}`, lessonId);
    persist();
    return state.examSettings[lessonId];
  }

  function saveGroupPlan(lessonId: string, input: GroupPlan): GroupPlan {
    assertLessonConfigurationMutable(lessonId);
    const lesson = requireLesson(lessonId);
    const plan: GroupPlan = { ...toPlain(input), lessonId };
    const issues: string[] = [];
    const roleKeys = plan.roles.map((role) => role.key.trim());
    if (plan.roles.length === 0) issues.push('至少需要一个角色组');
    if (roleKeys.some((key) => !key)) issues.push('角色组标识不能为空');
    if (new Set(roleKeys).size !== roleKeys.length) issues.push('角色组标识不能重复');
    const stageIds = new Set(lesson.stages.map((stage) => stage.id));
    const mappedStageIds = plan.roles.flatMap((role) => role.stageIds);
    if (mappedStageIds.some((stageId) => !stageIds.has(stageId))) {
      issues.push('角色组包含不存在的教学点');
    }
    if (
      new Set(mappedStageIds).size !== mappedStageIds.length ||
      lesson.stages.some((stage) => !mappedStageIds.includes(stage.id))
    ) {
      issues.push('每个教学点必须且只能属于一个角色组');
    }
    const membershipKeys = plan.members.map(
      (member) => `${member.studentId}|${member.groupKey}`
    );
    if (new Set(membershipKeys).size !== membershipKeys.length) {
      issues.push('同一学员不能重复加入同一个角色组');
    }
    if (plan.members.some((member) => !roleKeys.includes(member.groupKey))) {
      issues.push('学员分组必须引用已存在的角色组');
    }
    if (
      plan.members.some(
        (member) =>
          !member.studentId.trim() ||
          !member.studentName.trim() ||
          !member.unitId.trim() ||
          !member.accountId.trim()
      )
    ) {
      issues.push('学员、单位和业务账号信息必须完整');
    }
    if (issues.length) throw new TrainingValidationError(issues);

    plan.roles.forEach((role) => {
      role.stageIds.forEach((stageId) => {
        requireStage(lesson, stageId).groupKey = role.key;
      });
    });
    state.groupPlans[lessonId] = plan;
    lesson.updatedAt = now();
    addActivity(
      'GROUP_PLAN_SAVED',
      `保存分组：${lesson.title}`,
      `${plan.roles.length} 个角色组，${plan.members.length} 条成员关系`
    );
    persist();
    return state.groupPlans[lessonId];
  }

  function saveUnitDataPlans(
    lessonId: string,
    input: UnitDataPlan[]
  ): UnitDataPlan[] {
    assertLessonConfigurationMutable(lessonId);
    requireLesson(lessonId);
    const plans = toPlain(input);
    const issues: string[] = [];
    const unitIds = plans.map((plan) => plan.unitId.trim());
    if (plans.length === 0) issues.push('至少需要一个单位数据计划');
    if (unitIds.some((unitId) => !unitId)) issues.push('单位标识不能为空');
    if (new Set(unitIds).size !== unitIds.length) issues.push('单位数据计划不能重复');
    if (
      plans.some(
        (plan) =>
          !Number.isInteger(plan.formalCount) ||
          plan.formalCount < 0 ||
          !Number.isInteger(plan.spareCount) ||
          plan.spareCount < 0
      )
    ) {
      issues.push('正式数据和备用数据数量必须为非负整数');
    }
    if (issues.length) throw new TrainingValidationError(issues);
    state.unitDataPlans[lessonId] = plans;
    addActivity(
      'UNIT_DATA_PLANS_SAVED',
      `保存考试数据计划：${requireLesson(lessonId).title}`,
      `${plans.length} 个单位`
    );
    persist();
    return state.unitDataPlans[lessonId];
  }

  function generateData(lessonId: string): DataGenerationSummary {
    const lesson = requireLesson(lessonId);
    const plans = state.unitDataPlans[lessonId] ?? [];
    if (!plans.length) {
      throw new TrainingValidationError(['请先配置单位考试数据计划']);
    }
    const items = (state.dataItems[lessonId] ??= []);
    let requestedCount = 0;
    let readyCount = 0;
    let failedCount = 0;

    plans.forEach((plan) => {
      const total = plan.formalCount + plan.spareCount;
      requestedCount += total;
      for (let index = 0; index < total; index += 1) {
        const isSpare = index >= plan.formalCount;
        const shouldFail = plan.simulateFailure && index === total - 1;
        const itemId = idFactory('data');
        const item: ExamDataItem = {
          id: itemId,
          lessonId,
          unitId: plan.unitId,
          unitName: plan.unitName,
          kind: isSpare ? 'SPARE' : 'FORMAL',
          status: shouldFail ? 'GENERATION_FAILED' : 'READY',
          revision: 1,
          maskedReference: maskedReference(plan.unitId, itemId, index + 1),
          failureReason: shouldFail ? 'Mock：业务造数接口返回可重试错误' : undefined,
          assignedStudentTaskIds: [],
          audit: []
        };
        addDataAudit(
          item,
          shouldFail ? 'DATA_GENERATION_FAILED' : 'DATA_GENERATION_READY',
          shouldFail ? 'Mock：按单位模拟部分失败' : `Mock：${plan.scenario}`
        );
        items.push(item);
        if (shouldFail) failedCount += 1;
        else readyCount += 1;
      }
    });
    const result: DataGenerationSummary = {
      requestedCount,
      readyCount,
      failedCount,
      status: failedCount ? 'COMPLETED_WITH_FAILURES' : 'COMPLETED'
    };
    addActivity(
      'EXAM_DATA_GENERATED',
      `生成考试数据：${lesson.title}`,
      `请求 ${requestedCount}，成功 ${readyCount}，失败 ${failedCount}`
    );
    persist();
    return result;
  }

  function retryData(
    lessonId: string,
    itemId: string,
    reason: string
  ): ExamDataItem {
    const normalizedReason = requiredReason(reason);
    const original = requireDataItem(lessonId, itemId);
    if (original.status !== 'GENERATION_FAILED') {
      throw new Error('只有生成失败的数据可以重试');
    }
    addDataAudit(original, 'DATA_RETRY_REQUESTED', normalizedReason);
    original.status = 'DISABLED';
    const retried = createRevision(original, 'READY', normalizedReason);
    retried.failureReason = undefined;
    (state.dataItems[lessonId] ??= []).push(retried);
    addActivity('EXAM_DATA_RETRIED', '重试考试数据', normalizedReason);
    persist();
    return retried;
  }

  function disableData(
    lessonId: string,
    itemId: string,
    reason: string
  ): ExamDataItem {
    const normalizedReason = requiredReason(reason);
    const item = requireDataItem(lessonId, itemId);
    if (item.status === 'DISABLED') return item;
    if (item.status !== 'READY' && item.status !== 'GENERATION_FAILED') {
      throw new Error('只有未使用的就绪数据或生成失败数据可以停用');
    }
    item.status = 'DISABLED';
    addDataAudit(item, 'DATA_DISABLED', normalizedReason);
    addActivity('EXAM_DATA_DISABLED', '停用考试数据', normalizedReason);
    persist();
    return item;
  }

  function promoteData(
    lessonId: string,
    itemId: string,
    reason: string
  ): ExamDataItem {
    const normalizedReason = requiredReason(reason);
    const item = requireDataItem(lessonId, itemId);
    if (item.status !== 'READY' || item.kind !== 'SPARE') {
      throw new Error('只有未使用的就绪备用数据可以转正式');
    }
    item.kind = 'FORMAL';
    addDataAudit(item, 'DATA_PROMOTED_TO_FORMAL', normalizedReason);
    addActivity('EXAM_DATA_PROMOTED', '备用数据转正式', normalizedReason);
    persist();
    return item;
  }

  function replaceData(
    lessonId: string,
    itemId: string,
    reason: string
  ): ExamDataItem {
    const normalizedReason = requiredReason(reason);
    const original = requireDataItem(lessonId, itemId);
    if (original.status !== 'READY') {
      throw new Error('只有未使用的就绪数据可以替换');
    }
    original.status = 'DISABLED';
    addDataAudit(original, 'DATA_REPLACED', normalizedReason);
    const replacement = createRevision(original, 'READY', normalizedReason);
    (state.dataItems[lessonId] ??= []).push(replacement);
    addActivity('EXAM_DATA_REPLACED', '替换考试数据', normalizedReason);
    persist();
    return replacement;
  }

  function validateExamPublication(lessonId: string): string[] {
    const lesson = requireLesson(lessonId);
    const issues: string[] = [];
    if (lesson.status !== 'PUBLISHED') issues.push('必须先发布教案');
    if (!state.examSettings[lessonId]) issues.push('必须先保存考试设置');
    const plan = state.groupPlans[lessonId];
    if (!plan?.roles.length) issues.push('必须先配置角色组');
    if (!plan?.members.length) issues.push('必须先配置学员分组');
    const unitPlans = state.unitDataPlans[lessonId] ?? [];
    if (!unitPlans.length) issues.push('必须先配置单位考试数据');

    if (plan?.roles.length && plan.members.length) {
      const unitIds = [...new Set(plan.members.map((member) => member.unitId))];
      unitIds.forEach((unitId) => {
        const groupCounts = plan.roles.map(
          (role) =>
            plan.members.filter(
              (member) => member.unitId === unitId && member.groupKey === role.key
            ).length
        );
        if (groupCounts.some((count) => count === 0)) {
          issues.push('每个参与单位的每个角色组至少需要一名学员');
        }
        const requiredCount = Math.max(0, ...groupCounts);
        const readyFormalCount = (state.dataItems[lessonId] ?? []).filter(
          (item) =>
            item.unitId === unitId &&
            item.kind === 'FORMAL' &&
            item.status === 'READY'
        ).length;
        if (readyFormalCount < requiredCount) {
          issues.push('每个单位的可用正式数据不得少于该单位最大角色组人数');
        }
        if (!unitPlans.some((unitPlan) => unitPlan.unitId === unitId)) {
          issues.push('学员所属单位缺少考试数据计划');
        }
      });
    }
    return [...new Set(issues)];
  }

  function publishExam(lessonId: string): PublishedTask {
    const existing = state.publishedTasks.find(
      (task) => task.lessonId === lessonId && task.mode === 'EXAM'
    );
    if (existing) return existing;
    const lesson = requireLesson(lessonId);
    const issues = validateExamPublication(lessonId);
    if (issues.length) throw new TrainingValidationError(issues);
    const settings = state.examSettings[lessonId]!;
    const groupPlan = state.groupPlans[lessonId]!;
    const publishedTaskId = idFactory('published-task');
    const assignments = buildStudentAssignments(
      lessonId,
      publishedTaskId,
      lesson,
      settings,
      groupPlan
    );
    const status = resolvePublishedStatus(settings);
    const usedDataIds = new Set(assignments.map((assignment) => assignment.dataItemId));
    const published: PublishedTask = {
      id: publishedTaskId,
      lessonId,
      title: settings.batchName,
      mode: settings.mode,
      status,
      startAt: settings.startAt,
      endAt: settings.endAt,
      assignedCount: assignments.length,
      groupCount: groupPlan.roles.length,
      dataCount: usedDataIds.size,
      completedCount: 0
    };
    state.publishedTasks.unshift(published);
    state.studentTasks.push(...assignments);
    usedDataIds.forEach((dataItemId) => {
      const item = requireDataItem(lessonId, dataItemId);
      item.status = 'IN_USE';
      item.assignedStudentTaskIds = assignments
        .filter((assignment) => assignment.dataItemId === dataItemId)
        .map((assignment) => assignment.id);
      addDataAudit(item, 'DATA_ASSIGNED_TO_EXAM', publishedTaskId);
    });
    addActivity(
      'EXAM_PUBLISHED',
      `发布考试：${settings.batchName}`,
      `${assignments.length} 个学员任务，${usedDataIds.size} 条正式数据`
    );
    persist();
    return published;
  }

  async function publishExamRemote(lessonId: string): Promise<PublishedTask> {
    if (backend.isEnabled()) {
      const lesson = requireLesson(lessonId);
      if (!lesson.lectureCompletedAt) {
        throw new Error('请先完成教师讲解并发布学习、练习任务');
      }
      const prerequisiteModes: RunMode[] = ['LEARNING', 'PRACTICE'];
      const missing = prerequisiteModes.filter(
        (mode) =>
          !state.publishedTasks.some(
            (task) =>
              task.lessonId === lessonId &&
              task.mode === mode &&
              task.syncStatus === 'SYNCED'
          )
      );
      if (missing.length) {
        throw new Error('请先将学习任务和练习任务同步发布到后端');
      }
    }
    const published = publishExam(lessonId);
    const remotePublished = await publishModeTaskRemote(
      lessonId,
      'EXAM',
      published
    );
    await saveAuthenticatedWorkspace();
    return remotePublished;
  }

  function startStudentTask(taskId: string): StudentTask {
    const task = requireStudentTask(taskId);
    requireRunningExam(task);
    if (task.status === 'DOING') return task;
    if (task.status !== 'TODO') throw new Error('只有待办任务可以开始');
    task.status = 'DOING';
    task.startedAt = now();
    addActivity('STUDENT_TASK_STARTED', `${task.studentName}开始办理`, task.title);
    persist();
    return task;
  }

  async function startStudentTaskRemote(taskId: string): Promise<StudentTask> {
    const task = requireStudentTask(taskId);
    if (!backend.isEnabled()) return startStudentTask(taskId);
    const published = state.publishedTasks.find(
      (candidate) => candidate.id === task.publishedTaskId
    );
    if (!published || published.syncStatus !== 'SYNCED') {
      throw new Error('任务尚未同步发布到后端，暂时不能开始');
    }
    if (task.remoteExecutionId && task.remoteExecutionStatus === 'RUNNING') {
      return task;
    }
    const previous = toPlain(task);
    startStudentTask(taskId);
    task.syncStatus = 'SYNCING';
    delete task.syncError;
    persist();
    const lesson = requireLesson(task.lessonId);
    const platform = requireBusinessPlatform(lesson.businessPlatformId);
    try {
      const binding = await runRemote('开始学生任务', () =>
        backend.startStudentTaskExecution(
          toPlain(lesson),
          toPlain(platform),
          toPlain(published),
          toPlain(task)
        )
      );
      task.remoteExecutionId = binding.executionId;
      task.remoteExecutionStatus = binding.executionStatus;
      task.remoteContextLoaded = binding.contextLoaded;
      task.syncStatus = 'SYNCED';
      delete task.syncError;
      persist();
      return task;
    } catch (error) {
      restoreObject(task, previous);
      task.syncStatus = 'FAILED';
      task.syncError =
        error instanceof Error ? error.message : '学生任务启动失败';
      persist();
      throw error;
    }
  }

  function completeStudentStage(taskId: string, stageId: string): StudentTask {
    const task = requireStudentTask(taskId);
    requireRunningExam(task);
    assertAttemptWithinDuration(task);
    if (task.status !== 'DOING') throw new Error('任务必须处于办理中');
    const lesson = requireLesson(task.lessonId);
    const stage = requireStage(lesson, stageId);
    if (!stage.visibility[task.mode]) {
      throw new Error('该教学点在当前考试模式中不可办理');
    }
    if (!task.groupKeys.includes(stage.groupKey)) {
      throw new Error('该教学点不属于学员负责的角色组');
    }
    if (task.completedStageIds.includes(stageId)) return task;
    const stageIndex = lesson.stages.findIndex((candidate) => candidate.id === stageId);
    const completedForData = new Set(
      state.studentTasks
        .filter(
          (candidate) =>
            candidate.publishedTaskId === task.publishedTaskId &&
            candidate.dataItemId === task.dataItemId
        )
        .flatMap((candidate) => candidate.completedStageIds)
    );
    const incompletePrevious = lesson.stages
      .slice(0, stageIndex)
      .some(
        (previous) =>
          previous.visibility[task.mode] &&
          previous.required &&
          !completedForData.has(previous.id)
      );
    if (incompletePrevious) throw new Error('必须先完成前序教学点');

    task.completedStageIds.push(stageId);
    task.currentStageIndex = stageIndex + 1;
    addActivity(
      'STUDENT_STAGE_COMPLETED',
      `${task.studentName}完成教学点：${stage.name}`,
      `依据平台会话与流程操作轨迹判定`
    );
    persist();
    return task;
  }

  async function completeStudentStageRemote(
    taskId: string,
    stageId: string
  ): Promise<StudentTask> {
    const task = requireStudentTask(taskId);
    if (!backend.isEnabled()) return completeStudentStage(taskId, stageId);
    const published = state.publishedTasks.find(
      (candidate) => candidate.id === task.publishedTaskId
    );
    if (!published || !task.remoteExecutionId) {
      throw new Error('后端任务执行尚未开始');
    }
    const previous = toPlain(task);
    const completed = completeStudentStage(taskId, stageId);
    completed.syncStatus = 'SYNCING';
    delete completed.syncError;
    persist();
    const lesson = requireLesson(task.lessonId);
    const stage = requireStage(lesson, stageId);
    try {
    await runRemote('上报教学点完成轨迹', () =>
        backend.reportStudentStageCompletion(
          toPlain(lesson),
          toPlain(stage),
          toPlain(published),
          toPlain(completed)
        )
      );
      completed.syncStatus = 'SYNCED';
      delete completed.syncError;
      persist();
      return completed;
    } catch (error) {
      restoreObject(completed, previous);
      completed.syncStatus = 'FAILED';
      completed.syncError =
        error instanceof Error ? error.message : '教学点轨迹上报失败';
      persist();
      throw error;
    }
  }

  function submitStudentTask(
    taskId: string,
    submissionValues: Record<string, string> = {}
  ): StudentTask {
    const task = requireStudentTask(taskId);
    requireRunningExam(task);
    assertAttemptWithinDuration(task);
    if (task.status !== 'DOING') throw new Error('只有办理中的任务可以提交');
    const lesson = requireLesson(task.lessonId);
    const assignedStages = lesson.stages.filter(
      (stage) =>
        stage.visibility[task.mode] &&
        task.groupKeys.includes(stage.groupKey)
    );
    if (
      assignedStages.some(
        (stage) => stage.required && !task.completedStageIds.includes(stage.id)
      )
    ) {
      throw new Error('必须先完成本人负责的全部必做教学点');
    }
    const submissionFields =
      task.mode === 'EXAM'
        ? state.examSettings[task.lessonId]?.submissionFields ?? []
        : [];
    const normalizedSubmissionValues = Object.fromEntries(
      submissionFields.map((field) => [
        field.key,
        String(submissionValues[field.key] ?? '').trim()
      ])
    );
    const missingFields = submissionFields.filter(
      (field) =>
        field.required && !normalizedSubmissionValues[field.key]
    );
    if (missingFields.length) {
      throw new Error(
        `请填写必填提交信息：${missingFields.map((field) => field.label).join('、')}`
      );
    }
    task.status = 'SUBMITTED';
    const assignedRawScore = assignedStages.reduce(
      (total, stage) => total + stage.score,
      0
    );
    const completedRawScore = assignedStages
      .filter((stage) => task.completedStageIds.includes(stage.id))
      .reduce((total, stage) => total + stage.score, 0);
    const examObjectiveMax =
      state.examSettings[task.lessonId]?.objectiveWeight ??
      lesson.objectiveMaxScore;
    task.objectiveScore = assignedRawScore
      ? Math.round((completedRawScore / assignedRawScore) * examObjectiveMax)
      : 0;
    task.submissionValues = normalizedSubmissionValues;
    task.submittedAt = now();
    const published = state.publishedTasks.find(
      (candidate) => candidate.id === task.publishedTaskId
    );
    if (published) {
      published.completedCount = state.studentTasks.filter(
        (candidate) =>
          candidate.publishedTaskId === published.id &&
          (candidate.status === 'SUBMITTED' || candidate.status === 'GRADED')
      ).length;
    }
    addActivity(
      'STUDENT_TASK_SUBMITTED',
      `${task.studentName}已提交`,
      `系统客观评分：${task.objectiveScore}`
    );
    persist();
    return task;
  }

  async function submitStudentTaskRemote(
    taskId: string,
    submissionValues: Record<string, string> = {}
  ): Promise<StudentTask> {
    const task = requireStudentTask(taskId);
    if (!backend.isEnabled()) {
      return submitStudentTask(taskId, submissionValues);
    }
    if (!task.remoteExecutionId) {
      throw new Error('后端任务执行尚未开始，不能提交');
    }
    const previous = toPlain(task);
    const published = state.publishedTasks.find(
      (candidate) => candidate.id === task.publishedTaskId
    );
    const previousCompletedCount = published?.completedCount;
    const submitted = submitStudentTask(taskId, submissionValues);
    submitted.syncStatus = 'SYNCING';
    delete submitted.syncError;
    persist();
    try {
      const execution = await runRemote('提交学生任务并评分', () =>
        backend.submitStudentTaskExecution(toPlain(submitted))
      );
      submitted.remoteExecutionStatus = execution.executionStatus;
      submitted.remoteScore = execution.score;
      if (typeof execution.score === 'number') {
        submitted.objectiveScore = execution.score;
      }
      submitted.syncStatus = 'SYNCED';
      delete submitted.syncError;
      persist();
      return submitted;
    } catch (error) {
      restoreObject(submitted, previous);
      if (published && previousCompletedCount !== undefined) {
        published.completedCount = previousCompletedCount;
      }
      submitted.syncStatus = 'FAILED';
      submitted.syncError =
        error instanceof Error ? error.message : '学生任务提交失败';
      persist();
      throw error;
    }
  }

  function restartLearningOrPractice(taskId: string): StudentTask {
    const task = requireStudentTask(taskId);
    if (task.mode === 'EXAM') {
      throw new Error('考试任务请使用考试重新作答流程');
    }
    if (task.status !== 'SUBMITTED' && task.status !== 'GRADED') {
      throw new Error('只有已完成的学习或练习任务可以重新开始');
    }

    task.attemptNumber += 1;
    task.status = 'TODO';
    task.currentStageIndex = 0;
    task.completedStageIds = [];
    task.submissionValues = {};
    delete task.objectiveScore;
    delete task.subjectiveScore;
    delete task.comment;
    delete task.startedAt;
    delete task.submittedAt;
    delete task.gradedAt;
    delete task.remoteExecutionId;
    delete task.remoteExecutionStatus;
    delete task.remoteScore;
    delete task.remoteContextLoaded;
    delete task.syncError;
    task.syncStatus = backend.isEnabled() ? 'SYNCED' : 'LOCAL';

    const published = state.publishedTasks.find(
      (candidate) => candidate.id === task.publishedTaskId
    );
    if (published) {
      published.completedCount = state.studentTasks.filter(
        (candidate) =>
          candidate.publishedTaskId === published.id &&
          (candidate.status === 'SUBMITTED' || candidate.status === 'GRADED')
      ).length;
    }
    addActivity(
      'TRAINING_TASK_RESTARTED',
      `${task.studentName}${task.mode === 'LEARNING' ? '重新学习' : '重新练习'}`,
      `从教案录制流程第一步开始，第 ${task.attemptNumber} 次`
    );
    persist();
    return task;
  }

  function restartStudentAttempt(taskId: string): StudentTask {
    const requestedTask = requireStudentTask(taskId);
    requireRunningExam(requestedTask);
    const settings = state.examSettings[requestedTask.lessonId];
    if (!settings?.allowRetry) throw new Error('当前考试不允许重新作答');

    const collaborationTasks = state.studentTasks.filter(
      (task) =>
        task.publishedTaskId === requestedTask.publishedTaskId &&
        task.dataItemId === requestedTask.dataItemId
    );
    if (
      collaborationTasks.length > 0 &&
      collaborationTasks.every(
        (task) => task.status === 'SUBMITTED' || task.status === 'GRADED'
      )
    ) {
      throw new Error('协作单元已全部提交，不能重新作答');
    }
    const currentAttempt = Math.max(
      1,
      ...collaborationTasks.map((task) => task.attemptNumber)
    );
    if (currentAttempt >= settings.maxAttempts) {
      throw new Error('已达到最大作答次数');
    }

    const original = requireDataItem(
      requestedTask.lessonId,
      requestedTask.dataItemId
    );
    if (original.status !== 'IN_USE') {
      throw new Error('当前协作单元没有可替换的在用数据');
    }
    const reason = `学员在最终提交前重新作答（第 ${currentAttempt + 1} 次）`;
    original.status = 'DISABLED';
    addDataAudit(original, 'STUDENT_ATTEMPT_DATA_DISABLED', reason);

    const replacement: ExamDataItem = {
      ...toPlain(original),
      id: idFactory('data'),
      kind: 'FORMAL',
      status: 'IN_USE',
      revision: original.revision + 1,
      maskedReference: maskedReference(
        original.unitId,
        idFactory('reference'),
        original.revision + 1
      ),
      replacementOf: original.id,
      failureReason: undefined,
      assignedStudentTaskIds: collaborationTasks.map((task) => task.id),
      audit: []
    };
    addDataAudit(replacement, 'STUDENT_ATTEMPT_DATA_READY', reason);
    (state.dataItems[requestedTask.lessonId] ??= []).push(replacement);

    collaborationTasks.forEach((task) => {
      task.dataItemId = replacement.id;
      task.attemptNumber = currentAttempt + 1;
      task.status = 'TODO';
      task.currentStageIndex = 0;
      task.completedStageIds = [];
      task.submissionValues = {};
      delete task.objectiveScore;
      delete task.subjectiveScore;
      delete task.comment;
      delete task.startedAt;
      delete task.submittedAt;
      delete task.gradedAt;
    });
    const published = state.publishedTasks.find(
      (task) => task.id === requestedTask.publishedTaskId
    );
    if (published) {
      published.completedCount = state.studentTasks.filter(
        (task) =>
          task.publishedTaskId === published.id &&
          (task.status === 'SUBMITTED' || task.status === 'GRADED')
      ).length;
    }
    addActivity(
      'STUDENT_ATTEMPT_RESTARTED',
      `${requestedTask.studentName}重新作答`,
      `协作单元已整体切换到新数据，第 ${currentAttempt + 1} 次作答`
    );
    persist();
    return requireStudentTask(taskId);
  }

  function gradeStudentTask(
    taskId: string,
    subjectiveScore: number,
    comment: string
  ): StudentTask {
    const task = requireStudentTask(taskId);
    if (task.status !== 'SUBMITTED' && task.status !== 'GRADED') {
      throw new Error('只有已提交任务可以评分');
    }
    const lesson = requireLesson(task.lessonId);
    const maxScore =
      state.examSettings[task.lessonId]?.subjectiveWeight ??
      lesson.subjectiveMaxScore;
    if (
      !Number.isFinite(subjectiveScore) ||
      subjectiveScore < 0 ||
      subjectiveScore > maxScore
    ) {
      throw new Error(`主观评分必须在 0-${maxScore} 分之间`);
    }
    const normalizedComment = requiredReason(comment, '教师批语不能为空');
    task.status = 'GRADED';
    task.subjectiveScore = subjectiveScore;
    task.comment = normalizedComment;
    task.gradedAt = now();
    addActivity(
      'STUDENT_TASK_GRADED',
      `完成评阅：${task.studentName}`,
      `主观评分 ${subjectiveScore}，${normalizedComment}`
    );
    persist();
    return task;
  }

  function resetDemo(): TrainingState {
    replaceReactiveState(state, api.resetDemo());
    return state;
  }

  function createRevision(
    original: ExamDataItem,
    status: ExamDataItem['status'],
    reason: string
  ): ExamDataItem {
    const created: ExamDataItem = {
      ...toPlain(original),
      id: idFactory('data'),
      status,
      revision: original.revision + 1,
      maskedReference: maskedReference(
        original.unitId,
        idFactory('reference'),
        original.revision + 1
      ),
      replacementOf: original.id,
      assignedStudentTaskIds: [],
      audit: []
    };
    addDataAudit(created, 'DATA_REVISION_READY', reason);
    return created;
  }

  function buildStudentAssignments(
    lessonId: string,
    publishedTaskId: string,
    lesson: LessonPlan,
    settings: ExamSettings,
    groupPlan: GroupPlan
  ): StudentTask[] {
    const assignments = new Map<
      string,
      {
        member: GroupPlan['members'][number];
        dataItemId: string;
        groupKeys: string[];
      }
    >();
    const unitIds = [...new Set(groupPlan.members.map((member) => member.unitId))];

    unitIds.forEach((unitId) => {
      const eligibleDataItems = (state.dataItems[lessonId] ?? []).filter(
        (item) =>
          item.unitId === unitId && item.kind === 'FORMAL' && item.status === 'READY'
      );
      const dataItems = settings.randomizeData
        ? [...eligibleDataItems].sort(
            (left, right) =>
              stableHash(`${publishedTaskId}:${left.id}`) -
              stableHash(`${publishedTaskId}:${right.id}`)
          )
        : eligibleDataItems;
      const preferredDataByStudent = new Map<string, string>();
      groupPlan.roles.forEach((role) => {
        const members = groupPlan.members.filter(
          (member) => member.unitId === unitId && member.groupKey === role.key
        );
        const usedByRole = new Set<string>();
        const assign = (
          member: GroupPlan['members'][number],
          dataItemId: string
        ) => {
          usedByRole.add(dataItemId);
          preferredDataByStudent.set(member.studentId, dataItemId);
          const key = `${member.studentId}|${dataItemId}`;
          const existing = assignments.get(key);
          if (existing) {
            if (!existing.groupKeys.includes(role.key)) {
              existing.groupKeys.push(role.key);
            }
          } else {
            assignments.set(key, {
              member,
              dataItemId,
              groupKeys: [role.key]
            });
          }
        };

        members.forEach((member) => {
          const preferredId = preferredDataByStudent.get(member.studentId);
          const preferred =
            preferredId && !usedByRole.has(preferredId)
              ? dataItems.find((item) => item.id === preferredId)
              : undefined;
          const dataItem =
            preferred ?? dataItems.find((item) => !usedByRole.has(item.id));
          if (!dataItem) {
            throw new TrainingValidationError([
              '每个单位的可用正式数据不得少于该单位最大角色组人数'
            ]);
          }
          assign(member, dataItem.id);
        });

        let roundRobinIndex = 0;
        dataItems
          .filter((dataItem) => !usedByRole.has(dataItem.id))
          .forEach((dataItem) => {
            const alreadyOnData = members.find((member) =>
              assignments.has(`${member.studentId}|${dataItem.id}`)
            );
            const member =
              alreadyOnData ?? members[roundRobinIndex % members.length];
            if (!member) {
              throw new TrainingValidationError([
                '每个参与单位的每个角色组至少需要一名学员'
              ]);
            }
            assign(member, dataItem.id);
            roundRobinIndex += 1;
        });
      });
    });

    return [...assignments.values()].map(({ member, dataItemId, groupKeys }) => ({
      id: idFactory('student-task'),
      publishedTaskId,
      lessonId,
      studentId: member.studentId,
      studentName: member.studentName,
      title: `${settings.batchName} · ${member.unitName}`,
      mode: settings.mode,
      groupKey: groupKeys[0],
      groupKeys,
      unitId: member.unitId,
      unitName: member.unitName,
      dataItemId,
      attemptNumber: 1,
      submissionValues: {},
      status: 'TODO',
      currentStageIndex: 0,
      completedStageIds: []
    }));
  }

  return {
    state,
    remote,
    currentLesson,
    getLesson,
    getBusinessPlatform,
    getBusinessPlatformModule,
    setRole,
    createBusinessPlatform,
    createBusinessPlatformRemote,
    updateBusinessPlatform,
    updateBusinessPlatformRemote,
    setBusinessPlatformStatusRemote,
    removeBusinessPlatform,
    createBusinessPlatformModule,
    createBusinessPlatformModuleRemote,
    updateBusinessPlatformModule,
    updateBusinessPlatformModuleRemote,
    removeBusinessPlatformModule,
    removeBusinessPlatformModuleRemote,
    syncBusinessPlatforms,
    refreshPublishedTaskStatuses,
    createLesson,
    createLessonRemote,
    duplicateLesson,
    duplicateLessonRemote,
    updateLesson,
    addStage,
    updateStage,
    removeStage,
    moveStage,
    validateLesson,
    publishLesson,
    publishLessonRemote,
    markLessonLectureCompleted,
    publishLearningAndPracticeRemote,
    startCaptureSessionRemote,
    syncRecordedStep,
    saveExamSettings,
    saveGroupPlan,
    saveUnitDataPlans,
    generateData,
    retryData,
    disableData,
    promoteData,
    replaceData,
    validateExamPublication,
    publishExam,
    publishExamRemote,
    startStudentTask,
    startStudentTaskRemote,
    completeStudentStage,
    completeStudentStageRemote,
    submitStudentTask,
    submitStudentTaskRemote,
    restartLearningOrPractice,
    restartStudentAttempt,
    gradeStudentTask,
    resetDemo,
    clearAuthenticatedWorkspace,
    initializeAuthenticatedWorkspace,
    saveAuthenticatedWorkspace,
    flushAuthenticatedWorkspace
  };
}

export type TrainingStore = ReturnType<typeof createTrainingStore>;

let singleton: TrainingStore | undefined;

export function useTrainingStore(): TrainingStore {
  singleton ??= createTrainingStore();
  return singleton;
}

function requireStage(lesson: LessonPlan, stageId: string): LessonStage {
  const stage = lesson.stages.find((candidate) => candidate.id === stageId);
    if (!stage) throw new Error(`未找到教学点：${stageId}`);
  return stage;
}

function uniqueLessonCode(candidate: string): string {
  return `${candidate}-${Date.now().toString(36).toUpperCase()}`;
}

function isDate(value: string): boolean {
  return Number.isFinite(Date.parse(value));
}

function requiredReason(reason: string, message = '操作原因不能为空'): string {
  const normalized = reason.trim();
  if (!normalized) throw new Error(message);
  return normalized;
}

function normalizeBusinessModulePath(value: string): string {
  const path = value.trim();
  if (!path || path.startsWith('/') || /^[a-z][a-z\d+.-]*:\/\//i.test(path)) {
    return path;
  }
  return `/${path}`;
}

function maskedReference(unitId: string, id: string, index: number): string {
  const suffix = id.replace(/[^a-zA-Z0-9]/g, '').slice(-4).toUpperCase();
  return `${unitId.toUpperCase()}-***-${String(index).padStart(2, '0')}-${suffix}`;
}

function stableHash(value: string): number {
  let hash = 0;
  for (let index = 0; index < value.length; index += 1) {
    hash = (hash * 31 + value.charCodeAt(index)) | 0;
  }
  return hash;
}

function defaultIdFactory(prefix: string): string {
  const suffix =
    typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `${prefix}-${suffix}`;
}

function replaceReactiveState(target: TrainingState, source: TrainingState) {
  Object.keys(target).forEach((key) => {
    delete (target as unknown as Record<string, unknown>)[key];
  });
  Object.assign(target, toPlain(source));
}

function toPlain<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

function restoreObject<T extends object>(target: T, source: T): void {
  Object.keys(target).forEach((key) => {
    delete (target as Record<string, unknown>)[key];
  });
  Object.assign(target, toPlain(source));
}
