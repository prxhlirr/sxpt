import { beforeEach, describe, expect, it, vi } from 'vitest';
import { systemConfigApi } from './systemConfig';
import { apiRequest } from './http';

vi.mock('./http', () => ({
  apiRequest: vi.fn()
}));

const mockedApiRequest = vi.mocked(apiRequest);

describe('系统基础配置接口封装', () => {
  beforeEach(() => {
    mockedApiRequest.mockReset();
  });

  it('会按租户查询菜单配置', async () => {
    mockedApiRequest.mockResolvedValueOnce([]);

    await systemConfigApi.listMenus('demo-tenant');

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/system-config/menus',
      params: { tenantId: 'demo-tenant' }
    });
  });

  it('会查询菜单详情', async () => {
    mockedApiRequest.mockResolvedValueOnce({});

    await systemConfigApi.getMenuDetail('demo-tenant', 'menu-001');

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/system-config/menus/detail',
      params: { tenantId: 'demo-tenant', id: 'menu-001' }
    });
  });

  it('会提交菜单编辑请求', async () => {
    const request = {
      tenantId: 'demo-tenant',
      id: 'menu-001',
      menuCode: 'basic-users',
      menuName: '用户管理',
      menuType: 'MENU',
      routePath: '/admin/basic/users',
      sortNo: 10,
      visible: true
    };
    mockedApiRequest.mockResolvedValueOnce({});

    await systemConfigApi.updateMenu(request);

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'POST',
      url: '/system-config/menus/update',
      data: request
    });
  });

  it('会提交菜单启停请求', async () => {
    const request = {
      tenantId: 'demo-tenant',
      id: 'menu-001',
      status: 'DISABLED' as const
    };
    mockedApiRequest.mockResolvedValueOnce({});

    await systemConfigApi.updateMenuStatus(request);

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'POST',
      url: '/system-config/menus/status',
      data: request
    });
  });

  it('会按租户查询权限配置', async () => {
    mockedApiRequest.mockResolvedValueOnce([]);

    await systemConfigApi.listPermissions('demo-tenant');

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/system-config/permissions',
      params: { tenantId: 'demo-tenant' }
    });
  });

  it('会查询权限详情', async () => {
    mockedApiRequest.mockResolvedValueOnce({});

    await systemConfigApi.getPermissionDetail('demo-tenant', 'permission-001');

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/system-config/permissions/detail',
      params: { tenantId: 'demo-tenant', id: 'permission-001' }
    });
  });

  it('会提交权限编辑请求', async () => {
    const request = {
      tenantId: 'demo-tenant',
      id: 'permission-001',
      permissionCode: 'system:menu:edit',
      permissionName: '编辑菜单',
      resourceType: 'BUTTON',
      resourceCode: 'basic-menu-edit',
      actionCode: 'EDIT',
      description: '允许编辑菜单'
    };
    mockedApiRequest.mockResolvedValueOnce({});

    await systemConfigApi.updatePermission(request);

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'POST',
      url: '/system-config/permissions/update',
      data: request
    });
  });

  it('会提交权限启停请求', async () => {
    const request = {
      tenantId: 'demo-tenant',
      id: 'permission-001',
      status: 'DISABLED' as const
    };
    mockedApiRequest.mockResolvedValueOnce({});

    await systemConfigApi.updatePermissionStatus(request);

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'POST',
      url: '/system-config/permissions/status',
      data: request
    });
  });

  it('会按租户查询字典项配置', async () => {
    mockedApiRequest.mockResolvedValueOnce([]);

    await systemConfigApi.listDictItems('demo-tenant');

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/system-config/dict-items',
      params: { tenantId: 'demo-tenant' }
    });
  });

  it('会查询字典项详情', async () => {
    mockedApiRequest.mockResolvedValueOnce({});

    await systemConfigApi.getDictItemDetail('demo-tenant', 'dict-001');

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/system-config/dict-items/detail',
      params: { tenantId: 'demo-tenant', id: 'dict-001' }
    });
  });

  it('会提交字典项编辑请求', async () => {
    const request = {
      tenantId: 'demo-tenant',
      id: 'dict-001',
      dictCode: 'user_type',
      dictName: '用户类型',
      itemCode: 'TEACHER',
      itemName: '教师',
      itemValue: 'TEACHER',
      sortNo: 10,
      remark: '教师账号'
    };
    mockedApiRequest.mockResolvedValueOnce({});

    await systemConfigApi.updateDictItem(request);

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'POST',
      url: '/system-config/dict-items/update',
      data: request
    });
  });

  it('会提交字典项启停请求', async () => {
    const request = {
      tenantId: 'demo-tenant',
      id: 'dict-001',
      status: 'DISABLED' as const
    };
    mockedApiRequest.mockResolvedValueOnce({});

    await systemConfigApi.updateDictItemStatus(request);

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'POST',
      url: '/system-config/dict-items/status',
      data: request
    });
  });

  it('会按租户查询角色权限绑定配置', async () => {
    mockedApiRequest.mockResolvedValueOnce([]);

    await systemConfigApi.listRolePermissions('demo-tenant');

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/system-config/role-permissions',
      params: { tenantId: 'demo-tenant' }
    });
  });

  it('会查询角色权限绑定详情', async () => {
    mockedApiRequest.mockResolvedValueOnce({});

    await systemConfigApi.getRolePermissionDetail('demo-tenant', 'role-permission-001');

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/system-config/role-permissions/detail',
      params: { tenantId: 'demo-tenant', id: 'role-permission-001' }
    });
  });

  it('会提交角色权限绑定启停请求', async () => {
    const request = {
      tenantId: 'demo-tenant',
      id: 'role-permission-001',
      status: 'DISABLED' as const
    };
    mockedApiRequest.mockResolvedValueOnce({});

    await systemConfigApi.updateRolePermissionStatus(request);

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'POST',
      url: '/system-config/role-permissions/status',
      data: request
    });
  });

  it('会查询当前用户可见菜单', async () => {
    mockedApiRequest.mockResolvedValueOnce([]);

    await systemConfigApi.listCurrentUserMenus();

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/system-config/runtime/menus'
    });
  });

  it('会查询当前用户权限编码', async () => {
    mockedApiRequest.mockResolvedValueOnce([]);

    await systemConfigApi.listCurrentUserPermissions();

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/system-config/runtime/permissions'
    });
  });

  it('会查询当前用户运行时上下文', async () => {
    mockedApiRequest.mockResolvedValueOnce({
      user: {
        userId: 'user_001',
        tenantId: 'tenant_001',
        username: 'admin',
        displayName: '管理员',
        userType: 'ADMIN'
      },
      roles: [],
      orgs: [],
      permissions: [],
      menus: []
    });

    await systemConfigApi.listCurrentUserContext();

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/system-config/runtime/context'
    });
  });
});
