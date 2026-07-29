import type { TrainingRuntimeConfig } from '../config/trainingConfig';
import {
  TrainingApiError,
  type ActionDraftVO,
  type CaptureEventVO,
  type CaptureSegmentSwitchVO,
  type CaptureSessionVO,
  type ConfirmActionDraftRequest,
  type ConfirmSegmentSwitchRequest,
  type ConnectorResourceVO,
  type CreateActionDraftRequest,
  type CreateCaptureSessionRequest,
  type CreateConnectorResourceRequest,
  type CreateTeachingPointRequest,
  type CreateTaskStepRequest,
  type DiscardActionDraftRequest,
  type ReportCaptureEventRequest,
  type ReportResourceSnapshotRequest,
  type ResourceSnapshotVO,
  type TeachingPointVO,
  type TaskStepVO,
  type TrainingApi
} from '../types/trainingApi';
import type { ApiLogStore } from './apiLogStore';
import { createHttpClient, type ApiHttpClient } from './httpClient';

export class RemoteTrainingApi implements TrainingApi {
  private issuedBearerToken?: string;
  private issuedBearerTokenPromise?: Promise<string>;
  private readonly httpClient: ApiHttpClient;

  constructor(
    private readonly config: TrainingRuntimeConfig,
    private readonly logStore: ApiLogStore,
    httpClient?: ApiHttpClient
  ) {
    this.httpClient = httpClient ?? createHttpClient(config);
  }

  createCaptureSession(request: CreateCaptureSessionRequest) {
    return this.request<CaptureSessionVO>(
      'POST',
      '/capture/sessions/create',
      request
    );
  }

  listCaptureSessions(tenantId: string, teacherId: string) {
    return this.request<CaptureSessionVO[]>('GET', '/capture/sessions', undefined, {
      tenantId,
      teacherId
    });
  }

  finishCaptureSession(id: string) {
    return this.request<CaptureSessionVO>(
      'POST',
      `/capture/sessions/${encodeURIComponent(id)}/finish`
    );
  }

  confirmSegmentSwitch(request: ConfirmSegmentSwitchRequest) {
    return this.request<CaptureSegmentSwitchVO>(
      'POST',
      '/capture/segment-switches/confirm',
      request
    );
  }

  reportCaptureEvent(request: ReportCaptureEventRequest) {
    return this.request<CaptureEventVO>(
      'POST',
      '/capture/events/report',
      request
    );
  }

  listCaptureEvents(tenantId: string, captureSessionId: string) {
    return this.request<CaptureEventVO[]>('GET', '/capture/events', undefined, {
      tenantId,
      captureSessionId
    });
  }

  reportResourceSnapshot(request: ReportResourceSnapshotRequest) {
    return this.request<ResourceSnapshotVO>(
      'POST',
      '/capture/resource-snapshots/report',
      request
    );
  }

  listResourceSnapshots(tenantId: string, captureSessionId: string) {
    return this.request<ResourceSnapshotVO[]>(
      'GET',
      '/capture/resource-snapshots',
      undefined,
      { tenantId, captureSessionId }
    );
  }

  createActionDraft(request: CreateActionDraftRequest) {
    return this.request<ActionDraftVO>(
      'POST',
      '/capture/action-drafts/create',
      request
    );
  }

  listActionDrafts(tenantId: string, captureSessionId: string) {
    return this.request<ActionDraftVO[]>(
      'GET',
      '/capture/action-drafts',
      undefined,
      { tenantId, captureSessionId }
    );
  }

  confirmActionDraft(id: string, request: ConfirmActionDraftRequest) {
    return this.request<ActionDraftVO>(
      'POST',
      `/capture/action-drafts/${encodeURIComponent(id)}/confirm`,
      request
    );
  }

  discardActionDraft(id: string, request: DiscardActionDraftRequest) {
    return this.request<ActionDraftVO>(
      'POST',
      `/capture/action-drafts/${encodeURIComponent(id)}/discard`,
      request
    );
  }

  createConnectorResource(request: CreateConnectorResourceRequest) {
    return this.request<ConnectorResourceVO>(
      'POST',
      '/connector/resources/create',
      request
    );
  }

  listConnectorResources(
    tenantId: string,
    connectorSystemId: string,
    pageUrl?: string
  ) {
    return this.request<ConnectorResourceVO[]>(
      'GET',
      '/connector/resources',
      undefined,
      { tenantId, connectorSystemId, pageUrl }
    );
  }

  createTeachingPoint(request: CreateTeachingPointRequest) {
    return this.request<TeachingPointVO>(
      'POST',
      '/teaching/points/create',
      request
    );
  }

  listTeachingPoints(tenantId: string, connectorSystemId: string) {
    return this.request<TeachingPointVO[]>(
      'GET',
      '/teaching/points',
      undefined,
      { tenantId, connectorSystemId }
    );
  }

  createTaskStep(request: CreateTaskStepRequest) {
    return this.request<TaskStepVO>(
      'POST',
      '/teaching/task-steps/create',
      request
    );
  }

  listTaskSteps(
    tenantId: string,
    taskId: string,
    teachingPointId: string
  ) {
    return this.request<TaskStepVO[]>(
      'GET',
      '/teaching/task-steps',
      undefined,
      { tenantId, taskId, teachingPointId }
    );
  }

  private getBearerToken(): Promise<string> {
    const configuredToken = this.config.bearerToken.trim();
    if (configuredToken) return Promise.resolve(configuredToken);
    if (this.issuedBearerToken) return Promise.resolve(this.issuedBearerToken);

    if (!this.issuedBearerTokenPromise) {
      this.issuedBearerTokenPromise = this.fetchTestBearerToken()
        .then((token) => {
          this.issuedBearerToken = token;
          return token;
        })
        .finally(() => {
          this.issuedBearerTokenPromise = undefined;
        });
    }

    return this.issuedBearerTokenPromise;
  }

  private async fetchTestBearerToken(): Promise<string> {
    const path = '/auth/token';

    try {
      const response = await this.httpClient.post<{
        token: string;
        tokenType: string;
      }>(path, undefined, {
        query: {
          userId: this.config.teacherId,
          username: this.config.teacherName
        }
      });
      const normalizedToken = response.result.token?.trim() ?? '';

      if (!normalizedToken) {
        throw new TrainingApiError(
          response.message || '获取认证 Token 失败',
          response.code,
          path
        );
      }

      return normalizedToken;
    } catch (error) {
      throw normalizeApiError(error, path);
    }
  }

  private async request<T>(
    method: 'GET' | 'POST',
    path: string,
    body?: unknown,
    query?: Record<string, string | undefined>
  ): Promise<T> {
    const queryString = new URLSearchParams(
      Object.entries(query ?? {}).filter(
        (entry): entry is [string, string] => Boolean(entry[1])
      )
    ).toString();
    const requestPath = `${path}${queryString ? `?${queryString}` : ''}`;
    const requestId = createRequestId();
    const captureSessionId = getCaptureSessionId(body, query, path);
    this.logStore.begin({
      id: requestId,
      method,
      path: requestPath,
      captureSessionId
    });

    try {
      const bearerToken = await this.getBearerToken();
      const response = method === 'GET'
        ? await this.httpClient.get<T>(path, { query, bearerToken })
        : await this.httpClient.post<T>(path, body, { query, bearerToken });

      this.logStore.succeed(requestId, response.code, response.message);
      return response.result;
    } catch (error) {
      const normalized = normalizeApiError(error, path);
      this.logStore.fail(requestId, normalized.code, normalized.message);
      throw normalized;
    }
  }
}

function createRequestId(): string {
  return typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `request-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function getCaptureSessionId(
  body: unknown,
  query: Record<string, string | undefined> | undefined,
  path: string
): string | undefined {
  if (body && typeof body === 'object' && 'captureSessionId' in body) {
    const value = (body as { captureSessionId?: unknown }).captureSessionId;
    if (typeof value === 'string') return value;
  }
  if (query?.captureSessionId) return query.captureSessionId;
  const match = path.match(/\/capture\/sessions\/([^/]+)\/finish/);
  return match ? decodeURIComponent(match[1]) : undefined;
}

function normalizeApiError(error: unknown, path: string): TrainingApiError {
  if (error instanceof TrainingApiError) return error;
  return new TrainingApiError(
    error instanceof Error ? error.message : '接口请求失败',
    'REQUEST_FAILED',
    path
  );
}
