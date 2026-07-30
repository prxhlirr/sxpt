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
});
