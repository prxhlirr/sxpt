import axios, {
  type AxiosAdapter,
  type AxiosInstance
} from 'axios';
import type { TrainingRuntimeConfig } from '../config/trainingConfig';
import { TrainingApiError, type ApiResult } from '../types/trainingApi';

export type ApiQuery = Record<string, string | undefined>;

export interface ApiRequestOptions {
  query?: ApiQuery;
  bearerToken?: string;
}

export interface ApiHttpResponse<T> {
  result: T;
  code: number;
  message: string;
}

export interface ApiHttpClient {
  get<T>(path: string, options?: ApiRequestOptions): Promise<ApiHttpResponse<T>>;
  post<T>(
    path: string,
    body?: unknown,
    options?: ApiRequestOptions
  ): Promise<ApiHttpResponse<T>>;
  put<T>(
    path: string,
    body?: unknown,
    options?: ApiRequestOptions
  ): Promise<ApiHttpResponse<T>>;
}

export interface CreateHttpClientOptions {
  adapter?: AxiosAdapter;
}

export function createHttpClient(
  config: Pick<TrainingRuntimeConfig, 'apiBaseUrl' | 'requestTimeoutMs'>,
  options: CreateHttpClientOptions = {}
): ApiHttpClient {
  const instance = axios.create({
    baseURL: config.apiBaseUrl,
    timeout: config.requestTimeoutMs,
    adapter: options.adapter,
    headers: { Accept: 'application/json' },
    paramsSerializer: {
      serialize: serializeQuery
    }
  });

  return {
    get: <T>(path: string, requestOptions?: ApiRequestOptions) =>
      send<T>(instance, 'GET', path, undefined, requestOptions),
    post: <T>(
      path: string,
      body?: unknown,
      requestOptions?: ApiRequestOptions
    ) => send<T>(instance, 'POST', path, body, requestOptions),
    put: <T>(
      path: string,
      body?: unknown,
      requestOptions?: ApiRequestOptions
    ) => send<T>(instance, 'PUT', path, body, requestOptions)
  };
}

async function send<T>(
  instance: AxiosInstance,
  method: 'GET' | 'POST' | 'PUT',
  path: string,
  body?: unknown,
  options?: ApiRequestOptions
): Promise<ApiHttpResponse<T>> {
  try {
    const response = await instance.request<unknown>({
      url: path,
      method,
      params: options?.query,
      data: method === 'GET' ? undefined : body,
      headers: options?.bearerToken
        ? { Authorization: `Bearer ${options.bearerToken}` }
        : undefined
    });
    const payload = response.data;

    if (!isApiResult<T>(payload)) {
      throw new TrainingApiError(
        '接口响应格式错误',
        'INVALID_RESPONSE',
        path
      );
    }

    if (!payload.success) {
      throw new TrainingApiError(
        payload.message || `HTTP ${response.status}`,
        payload.code,
        path
      );
    }

    return {
      result: payload.result,
      code: payload.code,
      message: payload.message
    };
  } catch (error) {
    throw normalizeApiError(error, path);
  }
}

function normalizeApiError(error: unknown, path: string): TrainingApiError {
  if (error instanceof TrainingApiError) return error;

  if (axios.isAxiosError(error)) {
    if (error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT') {
      return new TrainingApiError('请求超时', 'REQUEST_TIMEOUT', path);
    }

    const payload = error.response?.data;
    if (isApiResult<unknown>(payload) && !payload.success) {
      return new TrainingApiError(
        payload.message || `HTTP ${error.response?.status ?? payload.code}`,
        payload.code,
        path
      );
    }

    const status = error.response?.status;
    return new TrainingApiError(
      status === undefined
        ? error.message || '接口请求失败'
        : `HTTP ${status}`,
      status ?? 'REQUEST_FAILED',
      path
    );
  }

  return new TrainingApiError(
    error instanceof Error ? error.message : '接口请求失败',
    'REQUEST_FAILED',
    path
  );
}

function isApiResult<T>(value: unknown): value is ApiResult<T> {
  if (typeof value !== 'object' || value === null) return false;

  const payload = value as Record<string, unknown>;
  return typeof payload.success === 'boolean'
    && typeof payload.code === 'number'
    && typeof payload.message === 'string'
    && Object.prototype.hasOwnProperty.call(payload, 'result')
    && typeof payload.timestamp === 'number';
}

function serializeQuery(params: Record<string, unknown>): string {
  const query = new URLSearchParams();
  for (const [key, value] of Object.entries(params ?? {})) {
    if (typeof value === 'string' && value) query.append(key, value);
  }
  return query.toString();
}
