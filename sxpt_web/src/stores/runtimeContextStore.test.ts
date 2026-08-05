import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { RuntimeUserContext } from '../api/systemConfig';
import { createRuntimeContextStore } from './runtimeContextStore';

function createContext(
  patch: Partial<RuntimeUserContext> = {}
): RuntimeUserContext {
  return {
    user: {
      userId: 'user-1',
      tenantId: 'tenant-1',
      username: 'teacher',
      displayName: '教师',
      userType: 'TEACHER'
    },
    roles: [],
    orgs: [],
    permissions: [],
    menus: [],
    ...patch
  };
}

describe('运行时上下文 Store', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('会缓存当前用户上下文并复用已完成的结果', async () => {
    const listCurrentUserContext = vi.fn(async () =>
      createContext({
        permissions: ['system:user:create']
      })
    );
    const store = createRuntimeContextStore({ listCurrentUserContext });

    await store.loadRuntimeContext();
    await store.loadRuntimeContext();

    expect(listCurrentUserContext).toHaveBeenCalledTimes(1);
    expect(store.state.context?.permissions).toEqual(['system:user:create']);
  });

  it('加载失败时拒绝需要权限码的操作入口', async () => {
    const store = createRuntimeContextStore({
      listCurrentUserContext: vi.fn(async () => {
        throw new Error('网络异常');
      })
    });

    const context = await store.loadRuntimeContext();

    expect(context).toBeNull();
    expect(store.state.initialized).toBe(true);
    expect(store.state.lastError).toBe('网络异常');
    expect(store.hasPermission('system:menu:create')).toBe(false);
  });

  it('超级管理员默认拥有全部权限', async () => {
    const store = createRuntimeContextStore({
      listCurrentUserContext: vi.fn(async () =>
        createContext({
          roles: [{ id: 'role-1', roleCode: 'ADMIN', roleName: '管理员' }],
          permissions: []
        })
      )
    });

    await store.loadRuntimeContext();

    expect(store.isSuperAdmin()).toBe(true);
    expect(store.hasPermission('system:unknown:operate')).toBe(true);
  });

  it('兼容种子数据中的小写超级管理员角色编码', async () => {
    const store = createRuntimeContextStore({
      listCurrentUserContext: vi.fn(async () =>
        createContext({
          roles: [{ id: 'role-1', roleCode: 'admin', roleName: '管理员' }],
          permissions: []
        })
      )
    });

    await store.loadRuntimeContext();

    expect(store.isSuperAdmin()).toBe(true);
    expect(store.hasPermission('system:user:create')).toBe(true);
  });

  it('普通角色按权限码判断操作入口', async () => {
    const store = createRuntimeContextStore({
      listCurrentUserContext: vi.fn(async () =>
        createContext({
          roles: [{ id: 'role-2', roleCode: 'TEACHER', roleName: '教师' }],
          permissions: ['system:user-org:remove']
        })
      )
    });

    await store.loadRuntimeContext();

    expect(store.hasPermission('system:user-org:remove')).toBe(true);
    expect(store.hasPermission('system:user:create')).toBe(false);
  });

  it('只返回可见页面菜单并按排序号排列', async () => {
    const store = createRuntimeContextStore({
      listCurrentUserContext: vi.fn(async () =>
        createContext({
          menus: [
            {
              id: 'menu-2',
              tenantId: 'tenant-1',
              menuCode: 'basic-users',
              menuName: '用户管理',
              menuType: 'MENU',
              routePath: '/admin/basic/users',
              sortNo: 20,
              visible: true,
              status: 'ACTIVE',
              createTime: '2026-07-30T10:00:00',
              updateTime: '2026-07-30T10:00:00'
            },
            {
              id: 'menu-1',
              tenantId: 'tenant-1',
              menuCode: 'admin-overview',
              menuName: '运营总览',
              menuType: 'MENU',
              routePath: '/admin/overview',
              sortNo: 10,
              visible: true,
              status: 'ACTIVE',
              createTime: '2026-07-30T10:00:00',
              updateTime: '2026-07-30T10:00:00'
            },
            {
              id: 'button-1',
              tenantId: 'tenant-1',
              menuCode: 'basic-user-create',
              menuName: '新增用户',
              menuType: 'BUTTON',
              routePath: '/admin/basic/users',
              sortNo: 30,
              visible: true,
              status: 'ACTIVE',
              createTime: '2026-07-30T10:00:00',
              updateTime: '2026-07-30T10:00:00'
            }
          ]
        })
      )
    });

    await store.loadRuntimeContext();

    expect(store.getVisibleMenus().map((menu) => menu.menuCode)).toEqual([
      'admin-overview',
      'basic-users'
    ]);
  });

  it('没有菜单配置时拒绝页面级后台访问', async () => {
    const store = createRuntimeContextStore({
      listCurrentUserContext: vi.fn(async () =>
        createContext({
          roles: [{ id: 'role-2', roleCode: 'TEACHER', roleName: '教师' }],
          menus: []
        })
      )
    });

    await store.loadRuntimeContext();

    expect(store.canAccessRoute('/platforms')).toBe(true);
    expect(store.canAccessRoute('/admin/basic/users')).toBe(false);
  });

  it('有菜单配置时按菜单路由限制页面访问', async () => {
    const store = createRuntimeContextStore({
      listCurrentUserContext: vi.fn(async () =>
        createContext({
          menus: [
            {
              id: 'menu-1',
              tenantId: 'tenant-1',
              menuCode: 'student-tasks',
              menuName: '我的任务',
              menuType: 'MENU',
              routePath: '/student/tasks',
              sortNo: 10,
              visible: true,
              status: 'ACTIVE',
              createTime: '2026-07-30T10:00:00',
              updateTime: '2026-07-30T10:00:00'
            },
            {
              id: 'menu-2',
              tenantId: 'tenant-1',
              menuCode: 'lesson-editor',
              menuName: '教案编排',
              menuType: 'MENU',
              routePath: '/admin/lessons/:lessonId/editor',
              sortNo: 20,
              visible: true,
              status: 'ACTIVE',
              createTime: '2026-07-30T10:00:00',
              updateTime: '2026-07-30T10:00:00'
            }
          ]
        })
      )
    });

    await store.loadRuntimeContext();

    expect(store.canAccessRoute('/student/tasks/task-1')).toBe(true);
    expect(store.canAccessRoute('/admin/lessons/lesson-1/editor')).toBe(true);
    expect(store.canAccessRoute('/admin/basic/users')).toBe(false);
  });
});
