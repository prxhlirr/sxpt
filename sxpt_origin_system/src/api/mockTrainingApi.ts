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

const MOCK_STORAGE_KEY = 'sxpt.mockTrainingApi.v1';

interface MockState {
  sessions: CaptureSessionVO[];
  switches: CaptureSegmentSwitchVO[];
  events: CaptureEventVO[];
  snapshots: ResourceSnapshotVO[];
  drafts: ActionDraftVO[];
  resources: ConnectorResourceVO[];
  teachingPoints: TeachingPointVO[];
  taskSteps: TaskStepVO[];
}

const emptyState = (): MockState => ({
  sessions: [],
  switches: [],
  events: [],
  snapshots: [],
  drafts: [],
  resources: [],
  teachingPoints: [],
  taskSteps: []
});

export class MockTrainingApi implements TrainingApi {
  constructor(
    private readonly logStore: ApiLogStore,
    private readonly storage: Storage = window.localStorage
  ) {}

  createCaptureSession(request: CreateCaptureSessionRequest) {
    return this.run('POST', '/capture/sessions/create', undefined, () => {
      const state = this.read();
      const now = new Date().toISOString();
      const session: CaptureSessionVO = {
        ...request,
        id: createId('capture'),
        captureMode: request.captureMode ?? 'STANDARD',
        sessionStatus: 'RUNNING',
        status: 'ACTIVE',
        createTime: now,
        updateTime: now
      };
      state.sessions.push(session);
      this.write(state);
      return session;
    });
  }

  listCaptureSessions(tenantId: string, teacherId: string) {
    return this.run('GET', '/capture/sessions', undefined, () =>
      this.read().sessions
        .filter(
          (session) =>
            session.tenantId === tenantId && session.teacherId === teacherId
        )
        .sort((a, b) => b.createTime.localeCompare(a.createTime))
    );
  }

  finishCaptureSession(id: string) {
    return this.run('POST', `/capture/sessions/${id}/finish`, id, () => {
      const state = this.read();
      const session = requiredById(state.sessions, id);
      if (session.sessionStatus === 'FINISHED') {
        throw new TrainingApiError('STATE_NOT_ALLOWED', 'STATE_NOT_ALLOWED');
      }
      session.sessionStatus = 'FINISHED';
      session.updateTime = new Date().toISOString();
      this.write(state);
      return session;
    });
  }

  confirmSegmentSwitch(request: ConfirmSegmentSwitchRequest) {
    return this.run(
      'POST',
      '/capture/segment-switches/confirm',
      request.captureSessionId,
      () => {
        if (request.currentSegmentNo === request.nextSegmentNo) {
          throw new TrainingApiError('STATE_NOT_ALLOWED', 'STATE_NOT_ALLOWED');
        }
        const state = this.read();
        const result: CaptureSegmentSwitchVO = {
          ...request,
          id: createId('switch'),
          launchContextId: createId('launch-context'),
          launchToken: createId('launch-token'),
          expireTime: new Date(Date.now() + 15 * 60 * 1000).toISOString(),
          switchConfirmRequired: true,
          switchDecisionSource: 'TEACHER_PATH'
        };
        state.switches.push(result);
        this.write(state);
        return result;
      }
    );
  }

  reportCaptureEvent(request: ReportCaptureEventRequest) {
    return this.run(
      'POST',
      '/capture/events/report',
      request.captureSessionId,
      () => {
        const state = this.read();
        const existing = state.events.find(
          (event) =>
            event.tenantId === request.tenantId &&
            event.captureSessionId === request.captureSessionId &&
            event.clientEventId === request.clientEventId
        );
        if (existing) return existing;

        const { eventPayloadJson: _omitted, ...safeRequest } = request;
        const event: CaptureEventVO = {
          ...safeRequest,
          id: createId('event'),
          retryCount: request.retryCount ?? 0,
          createTime: new Date().toISOString()
        };
        state.events.push(event);
        this.write(state);
        return event;
      }
    );
  }

  listCaptureEvents(tenantId: string, captureSessionId: string) {
    return this.run('GET', '/capture/events', captureSessionId, () =>
      this.read().events
        .filter(
          (event) =>
            event.tenantId === tenantId &&
            event.captureSessionId === captureSessionId
        )
        .sort(
          (a, b) =>
            a.sequenceNo - b.sequenceNo || a.eventTime.localeCompare(b.eventTime)
        )
    );
  }

  reportResourceSnapshot(request: ReportResourceSnapshotRequest) {
    return this.run(
      'POST',
      '/capture/resource-snapshots/report',
      request.captureSessionId,
      () => {
        const state = this.read();
        const snapshot: ResourceSnapshotVO = {
          ...request,
          id: createId('snapshot'),
          createTime: new Date().toISOString()
        };
        state.snapshots.push(snapshot);
        this.write(state);
        return snapshot;
      }
    );
  }

  listResourceSnapshots(tenantId: string, captureSessionId: string) {
    return this.run(
      'GET',
      '/capture/resource-snapshots',
      captureSessionId,
      () =>
        this.read().snapshots
          .filter(
            (snapshot) =>
              snapshot.tenantId === tenantId &&
              snapshot.captureSessionId === captureSessionId
          )
          .sort((a, b) => b.createTime.localeCompare(a.createTime))
    );
  }

  createActionDraft(request: CreateActionDraftRequest) {
    return this.run(
      'POST',
      '/capture/action-drafts/create',
      request.captureSessionId,
      () => {
        const state = this.read();
        const now = new Date().toISOString();
        const { confirmStatus: _requestedStatus, ...fields } = request;
        const draft: ActionDraftVO = {
          ...fields,
          id: createId('draft'),
          confirmStatus: 'PENDING',
          createTime: now,
          updateTime: now
        };
        state.drafts.push(draft);
        this.write(state);
        return draft;
      }
    );
  }

  listActionDrafts(tenantId: string, captureSessionId: string) {
    return this.run('GET', '/capture/action-drafts', captureSessionId, () =>
      this.read().drafts
        .filter(
          (draft) =>
            draft.tenantId === tenantId &&
            draft.captureSessionId === captureSessionId
        )
        .sort(
          (a, b) =>
            a.sequenceNo - b.sequenceNo || a.createTime.localeCompare(b.createTime)
        )
    );
  }

  confirmActionDraft(id: string, request: ConfirmActionDraftRequest) {
    return this.transitionDraft(id, 'confirm', request);
  }

  discardActionDraft(id: string, request: DiscardActionDraftRequest) {
    return this.transitionDraft(id, 'discard', request);
  }

  createConnectorResource(request: CreateConnectorResourceRequest) {
    return this.run('POST', '/connector/resources/create', request.sourceCaptureId, () => {
      const state = this.read();
      const resource: ConnectorResourceVO = {
        ...request,
        id: createId('resource'),
        status: 'ACTIVE',
        createTime: new Date().toISOString()
      };
      state.resources.push(resource);
      this.write(state);
      return resource;
    });
  }

  listConnectorResources(
    tenantId: string,
    connectorSystemId: string,
    pageUrl?: string
  ) {
    return this.run('GET', '/connector/resources', undefined, () =>
      this.read().resources
        .filter(
          (resource) =>
            resource.tenantId === tenantId &&
            resource.connectorSystemId === connectorSystemId &&
            (!pageUrl || resource.pageUrl === pageUrl)
        )
        .sort(
          (a, b) =>
            a.resourceType.localeCompare(b.resourceType) ||
            a.resourceCode.localeCompare(b.resourceCode)
        )
    );
  }

  createTeachingPoint(request: CreateTeachingPointRequest) {
    return this.run(
      'POST',
      '/teaching/points/create',
      request.sourceCaptureSessionId,
      () => {
        const state = this.read();
        const point: TeachingPointVO = {
          ...request,
          id: createId('teaching-point'),
          versionNo: 1,
          pointStatus: 'PUBLISHED',
          status: 'ACTIVE',
          createTime: new Date().toISOString()
        };
        state.teachingPoints.push(point);
        this.write(state);
        return point;
      }
    );
  }

  listTeachingPoints(tenantId: string, connectorSystemId: string) {
    return this.run('GET', '/teaching/points', undefined, () =>
      this.read().teachingPoints
        .filter(
          (point) =>
            point.tenantId === tenantId &&
            point.connectorSystemId === connectorSystemId
        )
        .sort(
          (a, b) =>
            a.pointCode.localeCompare(b.pointCode) || a.versionNo - b.versionNo
        )
    );
  }

  createTaskStep(request: CreateTaskStepRequest) {
    return this.run('POST', '/teaching/task-steps/create', undefined, () => {
      const state = this.read();
      const now = new Date().toISOString();
      const step: TaskStepVO = {
        ...request,
        id: createId('task-step'),
        status: 'ACTIVE',
        createTime: now,
        updateTime: now
      };
      state.taskSteps.push(step);
      this.write(state);
      return step;
    });
  }

  listTaskSteps(
    tenantId: string,
    taskId: string,
    teachingPointId: string
  ) {
    return this.run('GET', '/teaching/task-steps', undefined, () =>
      this.read().taskSteps
        .filter(
          (step) =>
            step.tenantId === tenantId
            && step.taskId === taskId
            && step.teachingPointId === teachingPointId
        )
        .sort((a, b) => a.sequenceNo - b.sequenceNo)
    );
  }

  private transitionDraft(
    id: string,
    action: 'confirm' | 'discard',
    request: ConfirmActionDraftRequest | DiscardActionDraftRequest
  ) {
    const path = `/capture/action-drafts/${id}/${action}`;
    return this.run('POST', path, undefined, () => {
      const state = this.read();
      const draft = requiredById(state.drafts, id);
      if (draft.confirmStatus !== 'PENDING') {
        throw new TrainingApiError('STATE_NOT_ALLOWED', 'STATE_NOT_ALLOWED');
      }
      if (action === 'confirm') {
        Object.assign(draft, request as ConfirmActionDraftRequest);
        draft.confirmStatus = 'CONFIRMED';
      } else {
        draft.confirmStatus = 'DISCARDED';
      }
      draft.updateTime = new Date().toISOString();
      this.write(state);
      return draft;
    });
  }

  private async run<T>(
    method: 'GET' | 'POST',
    path: string,
    captureSessionId: string | undefined,
    operation: () => T
  ): Promise<T> {
    const requestId = createId('mock-request');
    this.logStore.begin({ id: requestId, method, path, captureSessionId });
    try {
      const result = clone(operation());
      this.logStore.succeed(requestId, 200, 'success');
      return result;
    } catch (error) {
      const normalized =
        error instanceof TrainingApiError
          ? error
          : new TrainingApiError(
              error instanceof Error ? error.message : 'MOCK_REQUEST_FAILED'
            );
      this.logStore.fail(requestId, normalized.code, normalized.message);
      throw normalized;
    }
  }

  private read(): MockState {
    const raw = this.storage.getItem(MOCK_STORAGE_KEY);
    if (!raw) return emptyState();
    try {
      return { ...emptyState(), ...(JSON.parse(raw) as MockState) };
    } catch {
      return emptyState();
    }
  }

  private write(state: MockState) {
    this.storage.setItem(MOCK_STORAGE_KEY, JSON.stringify(state));
  }
}

function requiredById<T extends { id: string }>(values: T[], id: string): T {
  const value = values.find((item) => item.id === id);
  if (!value) throw new TrainingApiError('DATA_NOT_FOUND', 'DATA_NOT_FOUND');
  return value;
}

function createId(prefix: string): string {
  const suffix =
    typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `${prefix}-${suffix}`;
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}
