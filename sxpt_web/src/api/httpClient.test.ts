import axios, {
  AxiosError,
  AxiosHeaders,
  type AxiosAdapter,
  type InternalAxiosRequestConfig
} from 'axios';
import { describe, expect, it } from 'vitest';
import type { TrainingRuntimeConfig } from '../config/trainingConfig';
import { TrainingApiError } from '../types/trainingApi';
import { createHttpClient } from './httpClient';

const config: TrainingRuntimeConfig = {
  apiMode: 'remote',
  runBatchApiMode: 'mock',
  apiBaseUrl: 'http://localhost:8080/api/v1',
  bearerToken: '',
  requestTimeoutMs: 8000,
  tenantId: 'demo-tenant',
  connectorSystemId: 'demo-connector',
  teacherId: 'demo-teacher',
  teacherName: '演示教师'
};

function success<T>(result: T) {
  return {
    success: true,
    code: 200,
    message: 'success',
    result,
    timestamp: Date.now()
  };
}

function okAdapter(
  requests: InternalAxiosRequestConfig[],
  result: unknown
): AxiosAdapter {
  return async (request) => {
    requests.push(request);
    return {
      data: success(result),
      status: 200,
      statusText: 'OK',
      headers: new AxiosHeaders({ 'content-type': 'application/json' }),
      config: request
    };
  };
}

describe('createHttpClient', () => {
  it('sends an authenticated GET with base URL, timeout, and encoded query', async () => {
    const requests: InternalAxiosRequestConfig[] = [];
    const client = createHttpClient(config, {
      adapter: okAdapter(requests, [])
    });

    const response = await client.get<string[]>('/capture/sessions', {
      query: { tenantId: 'tenant one', teacherId: 'teacher/1', empty: '' },
      bearerToken: 'issued-token'
    });

    expect(response).toEqual({ result: [], code: 200, message: 'success' });
    expect(axios.getUri(requests[0])).toBe(
      'http://localhost:8080/api/v1/capture/sessions?tenantId=tenant+one&teacherId=teacher%2F1'
    );
    expect(requests[0].timeout).toBe(8000);
    expect(requests[0].headers.get('Accept')).toBe('application/json');
    expect(requests[0].headers.get('Authorization')).toBe(
      'Bearer issued-token'
    );
  });

  it('sends POST query and JSON body without Authorization when no token is supplied', async () => {
    const requests: InternalAxiosRequestConfig[] = [];
    const client = createHttpClient(config, {
      adapter: okAdapter(requests, { token: 'token-1' })
    });

    await client.post<{ token: string }>(
      '/auth/token',
      { source: 'test' },
      { query: { userId: 'demo-teacher', username: '演示教师' } }
    );

    expect(axios.getUri(requests[0])).toBe(
      'http://localhost:8080/api/v1/auth/token?userId=demo-teacher&username=%E6%BC%94%E7%A4%BA%E6%95%99%E5%B8%88'
    );
    expect(JSON.parse(String(requests[0].data))).toEqual({ source: 'test' });
    expect(requests[0].headers.get('Content-Type')).toContain(
      'application/json'
    );
    expect(requests[0].headers.has('Authorization')).toBe(false);
  });

  it('maps a success=false payload to TrainingApiError', async () => {
    const adapter: AxiosAdapter = async (request) => ({
      data: {
        success: false,
        code: 409,
        message: 'STATE_NOT_ALLOWED',
        result: null,
        timestamp: Date.now()
      },
      status: 200,
      statusText: 'OK',
      headers: new AxiosHeaders(),
      config: request
    });
    const client = createHttpClient(config, { adapter });

    await expect(client.get('/capture/sessions')).rejects.toEqual(
      expect.objectContaining<Partial<TrainingApiError>>({
        code: 409,
        message: 'STATE_NOT_ALLOWED',
        path: '/capture/sessions'
      })
    );
  });

  it('rejects a malformed success envelope instead of returning undefined', async () => {
    const adapter: AxiosAdapter = async (request) => ({
      data: { success: true },
      status: 200,
      statusText: 'OK',
      headers: new AxiosHeaders(),
      config: request
    });
    const client = createHttpClient(config, { adapter });
    const request = client.get('/capture/sessions');

    await expect(request).rejects.toBeInstanceOf(TrainingApiError);
    await expect(request).rejects.toEqual(
      expect.objectContaining({
        code: 'INVALID_RESPONSE',
        message: '接口响应格式错误',
        path: '/capture/sessions'
      })
    );
  });

  it('uses the backend payload from a rejected HTTP response', async () => {
    const adapter: AxiosAdapter = async (request) => {
      const response = {
        data: {
          success: false,
          code: 401,
          message: 'AUTH_FAILED',
          result: null,
          timestamp: Date.now()
        },
        status: 401,
        statusText: 'Unauthorized',
        headers: new AxiosHeaders(),
        config: request
      };
      throw new AxiosError(
        'Request failed with status code 401',
        AxiosError.ERR_BAD_REQUEST,
        request,
        undefined,
        response
      );
    };
    const client = createHttpClient(config, { adapter });

    await expect(client.post('/auth/token')).rejects.toEqual(
      expect.objectContaining({
        code: 401,
        message: 'AUTH_FAILED',
        path: '/auth/token'
      })
    );
  });

  it('uses HTTP status when a rejected response has a malformed envelope', async () => {
    const adapter: AxiosAdapter = async (request) => {
      const response = {
        data: { code: 999, message: 'malformed' },
        status: 502,
        statusText: 'Bad Gateway',
        headers: new AxiosHeaders(),
        config: request
      };
      throw new AxiosError(
        'Request failed with status code 502',
        AxiosError.ERR_BAD_RESPONSE,
        request,
        undefined,
        response
      );
    };
    const client = createHttpClient(config, { adapter });
    const request = client.get('/capture/events');

    await expect(request).rejects.toBeInstanceOf(TrainingApiError);
    await expect(request).rejects.toEqual(
      expect.objectContaining({
        code: 502,
        message: 'HTTP 502',
        path: '/capture/events'
      })
    );
  });

  it.each([
    ['ECONNABORTED', 'REQUEST_TIMEOUT'],
    ['ETIMEDOUT', 'REQUEST_TIMEOUT'],
    [AxiosError.ERR_NETWORK, 'REQUEST_FAILED']
  ])('maps Axios code %s to %s', async (axiosCode, expectedCode) => {
    const adapter: AxiosAdapter = async (request) => {
      throw new AxiosError('request failed', axiosCode, request);
    };
    const client = createHttpClient(config, { adapter });

    await expect(client.get('/capture/events')).rejects.toEqual(
      expect.objectContaining({
        code: expectedCode,
        path: '/capture/events'
      })
    );
  });
});
