import axios, {
  AxiosError,
  AxiosHeaders,
  type AxiosAdapter,
  type InternalAxiosRequestConfig
} from 'axios';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { normalizeBaseUrl } from '../config/api';
import type { ApiResult } from './contracts';
import {
  ApiRequestError,
  apiRequest,
  clearApiToken,
  setApiToken,
  unwrapApiResult
} from './http';

describe('API 运行时配置与统一响应', () => {
  afterEach(() => {
    clearApiToken();
    vi.restoreAllMocks();
  });

  it('会移除接口基础地址末尾的斜杠', () => {
    expect(normalizeBaseUrl(' http://127.0.0.1:8080/api/v1/// ')).toBe(
      'http://127.0.0.1:8080/api/v1'
    );
  });

  it('会解包后端 ApiResult 的 result', () => {
    const response: ApiResult<{ id: string }> = {
      success: true,
      code: 200,
      message: 'success',
      result: { id: 'platform-1' },
      timestamp: Date.now()
    };

    expect(unwrapApiResult(response)).toEqual({ id: 'platform-1' });
  });

  it('业务失败时抛出包含后端 code 的统一错误', () => {
    const response: ApiResult<never> = {
      success: false,
      code: 400,
      message: 'tenantId 不能为空',
      result: undefined as never,
      timestamp: Date.now()
    };

    expect(() => unwrapApiResult(response)).toThrowError(ApiRequestError);
    try {
      unwrapApiResult(response);
    } catch (error) {
      expect(error).toMatchObject({
        message: 'tenantId 不能为空',
        code: 400,
        httpStatus: 200
      });
    }
  });

  it('开发 Token 过期后会自动刷新并重试一次原请求', async () => {
    setApiToken('expired-token');
    const tokenResponse: ApiResult<{ token: string }> = {
      success: true,
      code: 200,
      message: 'success',
      result: { token: 'fresh-token' },
      timestamp: Date.now()
    };
    const tokenSpy = vi.spyOn(axios, 'post').mockResolvedValue({
      data: tokenResponse,
      status: 200
    });
    let requestCount = 0;
    const adapter: AxiosAdapter = async (config) => {
      requestCount += 1;
      if (requestCount === 1) {
        const response = {
          data: {
            success: false,
            code: 401,
            message: '用户未登录',
            result: null,
            timestamp: Date.now()
          },
          status: 401,
          statusText: 'Unauthorized',
          headers: new AxiosHeaders(),
          config: config as InternalAxiosRequestConfig
        };
        throw new AxiosError(
          'Request failed with status code 401',
          AxiosError.ERR_BAD_REQUEST,
          config as InternalAxiosRequestConfig,
          undefined,
          response
        );
      }
      return {
        data: {
          success: true,
          code: 200,
          message: 'success',
          result: { id: 'retried-request' },
          timestamp: Date.now()
        },
        status: 200,
        statusText: 'OK',
        headers: new AxiosHeaders(),
        config: config as InternalAxiosRequestConfig
      };
    };

    const result = await apiRequest<{ id: string }>({
      method: 'GET',
      url: '/protected-resource',
      adapter
    });

    expect(result).toEqual({ id: 'retried-request' });
    expect(requestCount).toBe(2);
    expect(tokenSpy).toHaveBeenCalledTimes(1);
  });
});
