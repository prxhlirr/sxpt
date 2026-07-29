import type { TrainingRuntimeConfig } from '../../config/trainingConfig';
import { TrainingApiError } from '../../types/trainingApi';
import type { ApiLogStore } from '../apiLogStore';
import {
  createHttpClient,
  type ApiHttpClient,
  type ApiQuery
} from '../httpClient';

export interface AuthenticatedApiClient {
  get<T>(path: string, query?: ApiQuery): Promise<T>;
  post<T>(path: string, body?: unknown, query?: ApiQuery): Promise<T>;
  put<T>(path: string, body?: unknown, query?: ApiQuery): Promise<T>;
  createClientRequestId(): string;
}

export interface AuthenticatedApiClientOptions {
  httpClient?: ApiHttpClient;
  requestIdFactory?: () => string;
}

export function createAuthenticatedApiClient(
  config: TrainingRuntimeConfig,
  logStore: ApiLogStore,
  options: AuthenticatedApiClientOptions = {}
): AuthenticatedApiClient {
  const httpClient = options.httpClient ?? createHttpClient(config);
  const requestIdFactory = options.requestIdFactory ?? defaultRequestId;
  let issuedBearerToken: string | undefined;
  let issuedBearerTokenPromise: Promise<string> | undefined;

  async function getBearerToken(): Promise<string> {
    const configuredToken = config.bearerToken.trim();
    if (configuredToken) return configuredToken;
    if (issuedBearerToken) return issuedBearerToken;

    if (!issuedBearerTokenPromise) {
      issuedBearerTokenPromise = httpClient
        .post<{ token: string; tokenType: string }>(
          '/auth/token',
          undefined,
          {
            query: {
              userId: config.teacherId,
              username: config.teacherName
            }
          }
        )
        .then((response) => {
          const token = response.result.token?.trim() ?? '';
          if (!token) {
            throw new TrainingApiError(
              response.message || '获取认证 Token 失败',
              response.code,
              '/auth/token'
            );
          }
          issuedBearerToken = token;
          return token;
        })
        .finally(() => {
          issuedBearerTokenPromise = undefined;
        });
    }

    return issuedBearerTokenPromise;
  }

  async function request<T>(
    method: 'GET' | 'POST' | 'PUT',
    path: string,
    body?: unknown,
    query?: ApiQuery
  ): Promise<T> {
    const logId = requestIdFactory();
    const queryString = new URLSearchParams(
      Object.entries(query ?? {}).filter(
        (entry): entry is [string, string] => Boolean(entry[1])
      )
    ).toString();
    logStore.begin({
      id: logId,
      method,
      path: `${path}${queryString ? `?${queryString}` : ''}`
    });

    try {
      const bearerToken = await getBearerToken();
      const options = { query, bearerToken };
      const response = method === 'GET'
        ? await httpClient.get<T>(path, options)
        : method === 'POST'
          ? await httpClient.post<T>(path, body, options)
          : await httpClient.put<T>(path, body, options);
      logStore.succeed(logId, response.code, response.message);
      return response.result;
    } catch (error) {
      const normalized = normalizeError(error, path);
      logStore.fail(logId, normalized.code, normalized.message);
      throw normalized;
    }
  }

  return {
    get: <T>(path: string, query?: ApiQuery) =>
      request<T>('GET', path, undefined, query),
    post: <T>(path: string, body?: unknown, query?: ApiQuery) =>
      request<T>('POST', path, body, query),
    put: <T>(path: string, body?: unknown, query?: ApiQuery) =>
      request<T>('PUT', path, body, query),
    createClientRequestId: requestIdFactory
  };
}

function defaultRequestId(): string {
  return typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `request-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function normalizeError(error: unknown, path: string): TrainingApiError {
  if (error instanceof TrainingApiError) return error;
  return new TrainingApiError(
    error instanceof Error ? error.message : '接口请求失败',
    'REQUEST_FAILED',
    path
  );
}
