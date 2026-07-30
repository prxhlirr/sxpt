import { reactive } from 'vue';
import {
  systemConfigApi,
  type RuntimeUserContext,
  type SysMenuConfig
} from '../api/systemConfig';

interface RuntimeContextApi {
  listCurrentUserContext: () => Promise<RuntimeUserContext>;
}

export interface RuntimeContextState {
  context: RuntimeUserContext | null;
  loading: boolean;
  initialized: boolean;
  lastError: string;
}

export function createRuntimeContextStore(
  api: RuntimeContextApi = systemConfigApi
) {
  const state = reactive<RuntimeContextState>({
    context: null,
    loading: false,
    initialized: false,
    lastError: ''
  });
  let loadingPromise: Promise<RuntimeUserContext | null> | undefined;

  /**
   * 业务功能：加载当前用户运行时上下文，作为菜单和按钮显隐的统一数据源。
   * 关键流程：同一时刻复用请求 Promise；失败时记录错误但返回空上下文，避免初始化期锁死管理入口。
   */
  async function loadRuntimeContext(
    options: { force?: boolean } = {}
  ): Promise<RuntimeUserContext | null> {
    if (!options.force && state.initialized) return state.context;
    if (loadingPromise) return loadingPromise;

    state.loading = true;
    state.lastError = '';
    loadingPromise = api
      .listCurrentUserContext()
      .then((context) => {
        state.context = context;
        state.initialized = true;
        return state.context;
      })
      .catch((error) => {
        state.context = null;
        state.initialized = true;
        state.lastError = error instanceof Error ? error.message : '运行时上下文加载失败';
        return null;
      })
      .finally(() => {
        state.loading = false;
        loadingPromise = undefined;
      });
    return loadingPromise;
  }

  /**
   * 业务功能：判断当前用户是否具备超级管理员角色。
   * 关键流程：兼容 ADMIN 和 SUPER_ADMIN 两种历史编码，避免种子数据迁移期间管理员权限失效。
   */
  function isSuperAdmin(): boolean {
    const roleCodes = state.context?.roles.map((role) => role.roleCode) ?? [];
    return roleCodes.includes('ADMIN') || roleCodes.includes('SUPER_ADMIN');
  }

  /**
   * 业务功能：判断指定权限码是否对当前用户开放。
   * 关键流程：无权限码或上下文未初始化时走兼容放行；上下文存在后优先使用角色和权限码精确判断。
   */
  function hasPermission(permissionCode?: string): boolean {
    if (!permissionCode || !state.context) return true;
    if (isSuperAdmin()) return true;
    return state.context.permissions.includes(permissionCode);
  }

  /**
   * 业务功能：返回当前用户可见的菜单配置。
   * 关键流程：只输出导航菜单，过滤按钮和隐藏项，并按排序号稳定排序。
   */
  function getVisibleMenus(): SysMenuConfig[] {
    return [...(state.context?.menus ?? [])]
      .filter((menu) => menu.visible && menu.menuType !== 'BUTTON' && Boolean(menu.routePath))
      .sort((left, right) => left.sortNo - right.sortNo);
  }

  /**
   * 业务功能：判断目标路由是否在当前用户可见菜单范围内。
   * 关键流程：菜单未初始化或加载失败时放行；存在菜单配置后，按精确路由、动态路由和父级菜单前缀判断页面访问。
   */
  function canAccessRoute(targetPath: string): boolean {
    if (!state.context || isSuperAdmin()) return true;
    const menuPaths = getVisibleMenus()
      .map((menu) => menu.routePath)
      .filter((routePath): routePath is string => Boolean(routePath));
    if (menuPaths.length === 0) return true;

    const normalizedTarget = normalizeRoutePath(targetPath);
    if (normalizedTarget === '/platforms') return true;
    return menuPaths.some((menuPath) => routePathMatches(menuPath, normalizedTarget));
  }

  /**
   * 业务功能：清空当前运行时上下文。
   * 关键流程：登录身份切换或测试隔离时重置缓存，下一次访问会重新加载后端上下文。
   */
  function resetRuntimeContext() {
    state.context = null;
    state.loading = false;
    state.initialized = false;
    state.lastError = '';
    loadingPromise = undefined;
  }

  return {
    state,
    loadRuntimeContext,
    isSuperAdmin,
    hasPermission,
    getVisibleMenus,
    canAccessRoute,
    resetRuntimeContext
  };
}

export type RuntimeContextStore = ReturnType<typeof createRuntimeContextStore>;

let singleton: RuntimeContextStore | undefined;

export function useRuntimeContextStore(): RuntimeContextStore {
  singleton ??= createRuntimeContextStore();
  return singleton;
}

export function resetRuntimeContextStoreForTest(): void {
  singleton?.resetRuntimeContext();
  singleton = undefined;
}

function routePathMatches(menuPath: string, targetPath: string): boolean {
  const normalizedMenuPath = normalizeRoutePath(menuPath);
  if (normalizedMenuPath === targetPath) return true;
  if (targetPath.startsWith(`${normalizedMenuPath}/`)) return true;
  if (!normalizedMenuPath.includes(':')) return false;
  const pattern = normalizedMenuPath
    .split('/')
    .map((segment) => (segment.startsWith(':') ? '[^/]+' : escapeRegex(segment)))
    .join('/');
  return new RegExp(`^${pattern}$`).test(targetPath);
}

function normalizeRoutePath(routePath: string): string {
  const [path] = routePath.split(/[?#]/);
  const normalized = path.startsWith('/') ? path : `/${path}`;
  return normalized.length > 1 ? normalized.replace(/\/+$/, '') : normalized;
}

function escapeRegex(value: string): string {
  return value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}
