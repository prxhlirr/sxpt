import { beforeEach, describe, expect, it, vi } from 'vitest';
import { usersApi } from './users';
import { apiRequest } from './http';

vi.mock('./http', () => ({
  apiRequest: vi.fn()
}));

const mockedApiRequest = vi.mocked(apiRequest);

describe('用户基础配置接口封装', () => {
  beforeEach(() => {
    mockedApiRequest.mockReset();
  });

  it('会按租户查询用户列表', async () => {
    mockedApiRequest.mockResolvedValueOnce([]);

    await usersApi.listUsers('demo-tenant');

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/user',
      params: { tenantId: 'demo-tenant' }
    });
  });

  it('会按租户查询用户角色绑定列表', async () => {
    mockedApiRequest.mockResolvedValueOnce([]);

    await usersApi.listUserRoles('demo-tenant');

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/user-roles',
      params: { tenantId: 'demo-tenant' }
    });
  });

  it('会按租户查询用户单位绑定列表', async () => {
    mockedApiRequest.mockResolvedValueOnce([]);

    await usersApi.listUserOrgs('demo-tenant');

    expect(mockedApiRequest).toHaveBeenCalledWith({
      method: 'GET',
      url: '/orgs/users',
      params: { tenantId: 'demo-tenant' }
    });
  });

  it('会提交用户编辑和状态切换请求', async () => {
    mockedApiRequest.mockResolvedValue(undefined);

    await usersApi.updateUser({
      tenantId: 'demo-tenant',
      id: 'user-001',
      realName: '赵老师',
      userType: 'TEACHER',
      sourceType: 'LOCAL'
    });
    await usersApi.updateUserStatus({
      tenantId: 'demo-tenant',
      id: 'user-001',
      status: 'DISABLED'
    });

    expect(mockedApiRequest).toHaveBeenNthCalledWith(1, {
      method: 'POST',
      url: '/user/update',
      data: {
        tenantId: 'demo-tenant',
        id: 'user-001',
        realName: '赵老师',
        userType: 'TEACHER',
        sourceType: 'LOCAL'
      }
    });
    expect(mockedApiRequest).toHaveBeenNthCalledWith(2, {
      method: 'POST',
      url: '/user/status',
      data: {
        tenantId: 'demo-tenant',
        id: 'user-001',
        status: 'DISABLED'
      }
    });
  });

  it('会提交角色、单位和绑定关系状态切换请求', async () => {
    mockedApiRequest.mockResolvedValue(undefined);

    await usersApi.updateRole({ tenantId: 'demo-tenant', id: 'role-001', roleName: '教师' });
    await usersApi.updateRoleStatus({ tenantId: 'demo-tenant', id: 'role-001', status: 'ACTIVE' });
    await usersApi.updateOrg({
      tenantId: 'demo-tenant',
      id: 'org-001',
      orgName: '演示学院',
      orgType: 'DEPARTMENT'
    });
    await usersApi.updateOrgStatus({ tenantId: 'demo-tenant', id: 'org-001', status: 'DISABLED' });
    await usersApi.updateUserRoleStatus({ tenantId: 'demo-tenant', id: 'ur-001', status: 'DISABLED' });
    await usersApi.updateUserOrgStatus({ tenantId: 'demo-tenant', id: 'uo-001', status: 'ACTIVE' });

    expect(mockedApiRequest).toHaveBeenNthCalledWith(1, {
      method: 'POST',
      url: '/roles/update',
      data: { tenantId: 'demo-tenant', id: 'role-001', roleName: '教师' }
    });
    expect(mockedApiRequest).toHaveBeenNthCalledWith(2, {
      method: 'POST',
      url: '/roles/status',
      data: { tenantId: 'demo-tenant', id: 'role-001', status: 'ACTIVE' }
    });
    expect(mockedApiRequest).toHaveBeenNthCalledWith(3, {
      method: 'POST',
      url: '/orgs/update',
      data: {
        tenantId: 'demo-tenant',
        id: 'org-001',
        orgName: '演示学院',
        orgType: 'DEPARTMENT'
      }
    });
    expect(mockedApiRequest).toHaveBeenNthCalledWith(4, {
      method: 'POST',
      url: '/orgs/status',
      data: { tenantId: 'demo-tenant', id: 'org-001', status: 'DISABLED' }
    });
    expect(mockedApiRequest).toHaveBeenNthCalledWith(5, {
      method: 'POST',
      url: '/user-roles/status',
      data: { tenantId: 'demo-tenant', id: 'ur-001', status: 'DISABLED' }
    });
    expect(mockedApiRequest).toHaveBeenNthCalledWith(6, {
      method: 'POST',
      url: '/orgs/users/status',
      data: { tenantId: 'demo-tenant', id: 'uo-001', status: 'ACTIVE' }
    });
  });
});
