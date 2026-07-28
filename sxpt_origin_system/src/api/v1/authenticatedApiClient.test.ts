import { describe, expect, it, vi } from 'vitest';
import { createApiLogStore } from '../apiLogStore';
import type { ApiHttpClient, ApiHttpResponse } from '../httpClient';
import type { TrainingRuntimeConfig } from '../../config/trainingConfig';
import { createAuthenticatedApiClient } from './authenticatedApiClient';

const config: TrainingRuntimeConfig = {
  apiMode: 'remote',
  runBatchApiMode: 'mock',
  apiBaseUrl: '/api/v1',
  bearerToken: 'configured-token',
  requestTimeoutMs: 8000,
  tenantId: 'demo-tenant',
  connectorSystemId: 'demo-connector',
  teacherId: 'demo-teacher',
  teacherName: '演示教师'
};

function success<T>(result: T): ApiHttpResponse<T> {
  return { result, code: 200, message: 'success' };
}

function httpClientWith(overrides: Partial<ApiHttpClient>): ApiHttpClient {
  return {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    ...overrides
  };
}

describe('createAuthenticatedApiClient', () => {
  it('uses the configured bearer token for PUT and records the request', async () => {
    const put = vi.fn().mockResolvedValue(success({ lockVersion: 2 }));
    const logStore = createApiLogStore();
    const client = createAuthenticatedApiClient(config, logStore, {
      httpClient: httpClientWith({ put })
    });

    await expect(
      client.put('/tasks/task-1/workflow-draft', { expectedLockVersion: 1 })
    ).resolves.toEqual({ lockVersion: 2 });

    expect(put).toHaveBeenCalledWith(
      '/tasks/task-1/workflow-draft',
      { expectedLockVersion: 1 },
      { query: undefined, bearerToken: 'configured-token' }
    );
    expect(logStore.entries.value[0]).toEqual(
      expect.objectContaining({
        method: 'PUT',
        path: '/tasks/task-1/workflow-draft',
        status: 'success'
      })
    );
  });

  it('shares one issued token request across concurrent calls', async () => {
    const post = vi
      .fn()
      .mockResolvedValueOnce(
        success({ token: 'issued-token', tokenType: 'Bearer' })
      )
      .mockResolvedValue(success({ id: 'published-1' }));
    const get = vi.fn().mockResolvedValue(success({ valid: true, issues: [] }));
    const client = createAuthenticatedApiClient(
      { ...config, bearerToken: '' },
      createApiLogStore(),
      { httpClient: httpClientWith({ get, post }) }
    );

    await Promise.all([
      client.get('/tasks/task-1/workflow-validation'),
      client.post('/tasks/task-1/workflow-publish', {
        clientRequestId: 'publish-1'
      })
    ]);

    expect(post).toHaveBeenCalledTimes(2);
    expect(post).toHaveBeenNthCalledWith(1, '/auth/token', undefined, {
      query: {
        userId: 'demo-teacher',
        username: '演示教师'
      }
    });
    expect(get).toHaveBeenCalledWith(
      '/tasks/task-1/workflow-validation',
      { query: undefined, bearerToken: 'issued-token' }
    );
    expect(post).toHaveBeenNthCalledWith(
      2,
      '/tasks/task-1/workflow-publish',
      { clientRequestId: 'publish-1' },
      { query: undefined, bearerToken: 'issued-token' }
    );
  });

  it('creates a fresh client request id for each user intent', () => {
    const requestIdFactory = vi
      .fn()
      .mockReturnValueOnce('request-1')
      .mockReturnValueOnce('request-2');
    const client = createAuthenticatedApiClient(
      config,
      createApiLogStore(),
      {
        httpClient: httpClientWith({}),
        requestIdFactory
      }
    );

    expect(client.createClientRequestId()).toBe('request-1');
    expect(client.createClientRequestId()).toBe('request-2');
  });
});
