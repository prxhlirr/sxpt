import { describe, expect, it, vi } from 'vitest';
import type { TrainingRuntimeConfig } from '../config/trainingConfig';
import { TrainingApiError } from '../types/trainingApi';
import type { ApiHttpClient, ApiHttpResponse } from './httpClient';
import { createApiLogStore } from './apiLogStore';
import { RemoteTrainingApi } from './remoteTrainingApi';

const config: TrainingRuntimeConfig = {
  apiMode: 'remote',
  runBatchApiMode: 'mock',
  apiBaseUrl: 'http://localhost:8080/api/v1',
  bearerToken: 'demo-token',
  requestTimeoutMs: 8000,
  tenantId: 'demo-tenant',
  connectorSystemId: 'demo-connector',
  teacherId: 'demo-teacher',
  teacherName: '演示教师'
};

function apiSuccess<T>(result: T): ApiHttpResponse<T> {
  return { result, code: 200, message: 'success' };
}

function clientWith(
  get: ReturnType<typeof vi.fn> = vi.fn(),
  post: ReturnType<typeof vi.fn> = vi.fn()
): ApiHttpClient {
  return { get, post } as unknown as ApiHttpClient;
}

const captureSession = {
  id: 'capture-1',
  tenantId: 'demo-tenant',
  connectorSystemId: 'demo-connector',
  teacherId: 'demo-teacher',
  sessionName: '采购申请备案',
  startUrl: '/business-sdk-demo.html',
  captureMode: 'STANDARD' as const,
  sessionStatus: 'RUNNING' as const,
  status: 'ACTIVE' as const,
  createTime: '2026-07-16T00:00:00',
  updateTime: '2026-07-16T00:00:00'
};

describe('RemoteTrainingApi', () => {
  it('posts capture session creation with configured auth', async () => {
    const post = vi.fn().mockResolvedValue(apiSuccess(captureSession));
    const logStore = createApiLogStore();
    const api = new RemoteTrainingApi(
      config,
      logStore,
      clientWith(vi.fn(), post)
    );
    const request = {
      tenantId: 'demo-tenant',
      connectorSystemId: 'demo-connector',
      teacherId: 'demo-teacher',
      sessionName: '采购申请备案',
      startUrl: '/business-sdk-demo.html'
    };

    await expect(api.createCaptureSession(request)).resolves.toEqual(
      captureSession
    );
    expect(post).toHaveBeenCalledWith(
      '/capture/sessions/create',
      request,
      { query: undefined, bearerToken: 'demo-token' }
    );
    expect(logStore.entries.value[0]).toEqual(
      expect.objectContaining({
        path: '/capture/sessions/create',
        status: 'success',
        code: 200
      })
    );
  });

  it('preserves a normalized backend error from the common client', async () => {
    const get = vi.fn().mockRejectedValue(
      new TrainingApiError(
        'STATE_NOT_ALLOWED',
        409,
        '/capture/sessions'
      )
    );
    const api = new RemoteTrainingApi(
      config,
      createApiLogStore(),
      clientWith(get)
    );

    await expect(
      api.listCaptureSessions('demo-tenant', 'demo-teacher')
    ).rejects.toEqual(
      expect.objectContaining({
        code: 409,
        message: 'STATE_NOT_ALLOWED',
        path: '/capture/sessions'
      })
    );
  });

  it('passes list query parameters to the common client', async () => {
    const get = vi.fn().mockResolvedValue(apiSuccess([]));
    const api = new RemoteTrainingApi(
      config,
      createApiLogStore(),
      clientWith(get)
    );

    await api.listConnectorResources(
      'tenant one',
      'connector/1',
      '/purchase/apply?draft=true'
    );

    expect(get).toHaveBeenCalledWith('/connector/resources', {
      query: {
        tenantId: 'tenant one',
        connectorSystemId: 'connector/1',
        pageUrl: '/purchase/apply?draft=true'
      },
      bearerToken: 'demo-token'
    });
  });

  it('obtains a token before sending a protected request', async () => {
    const post = vi.fn().mockResolvedValue(
      apiSuccess({ token: 'issued-token', tokenType: 'Bearer' })
    );
    const get = vi.fn().mockResolvedValue(apiSuccess([]));
    const api = new RemoteTrainingApi(
      { ...config, apiBaseUrl: '/api/v1', bearerToken: '' },
      createApiLogStore(),
      clientWith(get, post)
    );

    await api.listCaptureSessions('demo-tenant', 'demo-teacher');

    expect(post).toHaveBeenCalledWith('/auth/token', undefined, {
      query: { userId: 'demo-teacher', username: '演示教师' }
    });
    expect(get).toHaveBeenCalledWith('/capture/sessions', {
      query: { tenantId: 'demo-tenant', teacherId: 'demo-teacher' },
      bearerToken: 'issued-token'
    });
  });

  it('shares one issued token request across concurrent business requests', async () => {
    let releaseToken!: () => void;
    const tokenResponse = new Promise<
      ApiHttpResponse<{ token: string; tokenType: string }>
    >((resolve) => {
      releaseToken = () =>
        resolve(apiSuccess({ token: 'shared-token', tokenType: 'Bearer' }));
    });
    const post = vi.fn().mockImplementation(() => tokenResponse);
    const get = vi.fn().mockResolvedValue(apiSuccess([]));
    const api = new RemoteTrainingApi(
      { ...config, bearerToken: '' },
      createApiLogStore(),
      clientWith(get, post)
    );

    const first = api.listCaptureSessions('demo-tenant', 'demo-teacher');
    const second = api.listTeachingPoints('demo-tenant', 'demo-connector');
    releaseToken();
    await Promise.all([first, second]);

    expect(post).toHaveBeenCalledTimes(1);
    expect(post).toHaveBeenCalledWith('/auth/token', undefined, {
      query: { userId: 'demo-teacher', username: '演示教师' }
    });
    expect(get).toHaveBeenCalledTimes(2);
    expect(get.mock.calls[0][1].bearerToken).toBe('shared-token');
    expect(get.mock.calls[1][1].bearerToken).toBe('shared-token');
  });

  it('reuses an issued token for later sequential requests', async () => {
    const post = vi.fn().mockResolvedValue(
      apiSuccess({ token: 'cached-token', tokenType: 'Bearer' })
    );
    const get = vi.fn().mockResolvedValue(apiSuccess([]));
    const api = new RemoteTrainingApi(
      { ...config, bearerToken: '' },
      createApiLogStore(),
      clientWith(get, post)
    );

    await api.listCaptureSessions('demo-tenant', 'demo-teacher');
    await api.listTeachingPoints('demo-tenant', 'demo-connector');

    expect(post).toHaveBeenCalledTimes(1);
    expect(get).toHaveBeenCalledTimes(2);
    expect(get.mock.calls[0][1].bearerToken).toBe('cached-token');
    expect(get.mock.calls[1][1].bearerToken).toBe('cached-token');
  });

  it('retries token acquisition after a failed token request', async () => {
    const post = vi.fn()
      .mockRejectedValueOnce(
        new TrainingApiError('AUTH_FAILED', 401, '/auth/token')
      )
      .mockResolvedValueOnce(
        apiSuccess({ token: 'retry-token', tokenType: 'Bearer' })
      );
    const get = vi.fn().mockResolvedValue(apiSuccess([]));
    const api = new RemoteTrainingApi(
      { ...config, bearerToken: '' },
      createApiLogStore(),
      clientWith(get, post)
    );

    await expect(
      api.listCaptureSessions('demo-tenant', 'demo-teacher')
    ).rejects.toEqual(expect.objectContaining({ code: 401 }));
    await expect(
      api.listCaptureSessions('demo-tenant', 'demo-teacher')
    ).resolves.toEqual([]);

    expect(post).toHaveBeenCalledTimes(2);
    expect(get).toHaveBeenCalledTimes(1);
    expect(get.mock.calls[0][1].bearerToken).toBe('retry-token');
  });

  it('does not send the business request when authentication fails', async () => {
    const get = vi.fn();
    const post = vi.fn().mockRejectedValue(
      new TrainingApiError('AUTH_FAILED', 401, '/auth/token')
    );
    const logStore = createApiLogStore();
    const api = new RemoteTrainingApi(
      { ...config, bearerToken: '' },
      logStore,
      clientWith(get, post)
    );

    await expect(
      api.listCaptureSessions('demo-tenant', 'demo-teacher')
    ).rejects.toEqual(expect.objectContaining({ code: 401 }));
    expect(get).not.toHaveBeenCalled();
    expect(logStore.entries.value[0]).toEqual(
      expect.objectContaining({ status: 'error', code: 401 })
    );
  });

  it('rejects a whitespace-only issued token before the business request', async () => {
    const get = vi.fn();
    const post = vi.fn().mockResolvedValue(
      apiSuccess({ token: '   ', tokenType: 'Bearer' })
    );
    const api = new RemoteTrainingApi(
      { ...config, bearerToken: '' },
      createApiLogStore(),
      clientWith(get, post)
    );

    await expect(
      api.listCaptureSessions('demo-tenant', 'demo-teacher')
    ).rejects.toEqual(
      expect.objectContaining({
        code: 200,
        message: 'success',
        path: '/auth/token'
      })
    );
    expect(get).not.toHaveBeenCalled();
  });

  it('starts the business request only after authentication completes', async () => {
    let releaseToken!: () => void;
    const tokenResponse = new Promise<
      ApiHttpResponse<{ token: string; tokenType: string }>
    >((resolve) => {
      releaseToken = () =>
        resolve(apiSuccess({ token: 'issued-token', tokenType: 'Bearer' }));
    });
    const post = vi.fn().mockImplementation(() => tokenResponse);
    const get = vi.fn().mockResolvedValue(apiSuccess([]));
    const api = new RemoteTrainingApi(
      { ...config, bearerToken: '' },
      createApiLogStore(),
      clientWith(get, post)
    );

    const request = api.listCaptureSessions('demo-tenant', 'demo-teacher');
    await Promise.resolve();
    expect(get).not.toHaveBeenCalled();

    releaseToken();
    await request;
    expect(get).toHaveBeenCalledTimes(1);
  });
});
