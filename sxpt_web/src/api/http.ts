import axios, {
  AxiosHeaders,
  type AxiosError,
  type AxiosRequestConfig
} from 'axios';
import { getApiConfig } from '../config/api';
import type { ApiResult } from './contracts';

export const API_TOKEN_STORAGE_KEY = 'sxpt_web.api.token';

export class ApiRequestError extends Error {
  constructor(
    message: string,
    public readonly code?: number | string,
    public readonly httpStatus?: number,
    options?: ErrorOptions
  ) {
    super(message, options);
    this.name = 'ApiRequestError';
  }
}

const apiHttp = axios.create();
let devTokenPromise: Promise<string> | undefined;
let devTokenRefreshPromise: Promise<string> | undefined;

apiHttp.interceptors.request.use(async (request) => {
  const config = getApiConfig();
  request.baseURL = config.apiBaseUrl;
  request.timeout = config.timeoutMs;

  if (!isPublicPath(request.url)) {
    const token = await resolveToken();
    if (token) {
      request.headers = AxiosHeaders.from(request.headers);
      request.headers.set('Authorization', `Bearer ${token}`);
    }
  }
  return request;
});

export async function apiRequest<T>(
  request: AxiosRequestConfig
): Promise<T> {
  return apiRequestWithAuthRetry<T>(request, false);
}

async function apiRequestWithAuthRetry<T>(
  request: AxiosRequestConfig,
  authRetried: boolean
): Promise<T> {
  try {
    const response = await apiHttp.request<ApiResult<T>>(request);
    return unwrapApiResult(response.data, response.status);
  } catch (error) {
    const requestError =
      error instanceof ApiRequestError
        ? error
        : normalizeAxiosError(error as AxiosError<ApiResult<unknown>>);
    if (
      !authRetried &&
      shouldRefreshDevToken(requestError, request.url)
    ) {
      await refreshDevToken();
      return apiRequestWithAuthRetry<T>(request, true);
    }
    throw requestError;
  }
}

export function unwrapApiResult<T>(
  response: ApiResult<T>,
  httpStatus = 200
): T {
  if (!response || response.success !== true || response.code !== 200) {
    throw new ApiRequestError(
      response?.message || '后端接口返回失败',
      response?.code,
      httpStatus
    );
  }
  return response.result;
}

export function setApiToken(token: string): void {
  const normalized = token.trim();
  try {
    if (typeof window !== 'undefined' && window.localStorage) {
      if (normalized) {
        window.localStorage.setItem(API_TOKEN_STORAGE_KEY, normalized);
      } else {
        window.localStorage.removeItem(API_TOKEN_STORAGE_KEY);
      }
    }
  } catch {
    // Token can still be supplied from api-config.js.
  }
  devTokenPromise = normalized ? Promise.resolve(normalized) : undefined;
}

export function clearApiToken(): void {
  setApiToken('');
}

function shouldRefreshDevToken(
  error: ApiRequestError,
  requestUrl?: string
): boolean {
  const config = getApiConfig();
  return Boolean(
    config.autoDevToken &&
      !config.bearerToken.trim() &&
      !isPublicPath(requestUrl) &&
      (Number(error.code) === 401 || error.httpStatus === 401)
  );
}

async function refreshDevToken(): Promise<string> {
  devTokenRefreshPromise ??= (async () => {
    clearApiToken();
    return requestDevToken();
  })();
  try {
    return await devTokenRefreshPromise;
  } finally {
    devTokenRefreshPromise = undefined;
  }
}

function isPublicPath(url?: string): boolean {
  return Boolean(
    url &&
      (/\/auth\/(?:login|token)$/.test(url) ||
        /\/system\/(?:health|db-health|redis-health)$/.test(url))
  );
}

async function resolveToken(): Promise<string> {
  const config = getApiConfig();
  if (config.bearerToken.trim()) return config.bearerToken.trim();

  try {
    const stored =
      typeof window === 'undefined'
        ? ''
        : window.localStorage?.getItem(API_TOKEN_STORAGE_KEY) ?? '';
    if (stored) return stored;
  } catch {
    // Fall through to the dev-token flow.
  }

  if (!config.autoDevToken) return '';
  devTokenPromise ??= requestDevToken();
  try {
    return await devTokenPromise;
  } catch (error) {
    devTokenPromise = undefined;
    throw error;
  }
}

async function requestDevToken(): Promise<string> {
  const config = getApiConfig();
  try {
    const response = await axios.post<ApiResult<{ token: string }>>(
      `${config.apiBaseUrl}/auth/token`,
      undefined,
      {
        params: {
          userId: config.currentUserId,
          username: config.currentUsername
        },
        timeout: config.timeoutMs
      }
    );
    const token = unwrapApiResult(response.data, response.status).token;
    setApiToken(token);
    return token;
  } catch (error) {
    throw normalizeAxiosError(error as AxiosError<ApiResult<unknown>>);
  }
}

function normalizeAxiosError(
  error: AxiosError<ApiResult<unknown>>
): ApiRequestError {
  const response = error.response?.data;
  const message =
    response?.message ||
    (error.code === 'ECONNABORTED'
      ? '后端接口请求超时'
      : error.message || '后端接口请求失败');
  return new ApiRequestError(
    message,
    response?.code ?? error.code,
    error.response?.status,
    { cause: error }
  );
}
