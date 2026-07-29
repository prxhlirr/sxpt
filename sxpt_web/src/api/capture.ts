import { apiRequest } from './http';
import type {
  CaptureActionDraft,
  CaptureEvent,
  CaptureResourceSnapshot,
  CaptureSegmentSwitch,
  CaptureSession,
  IsoDateTime
} from './contracts';

export interface CreateCaptureSessionRequest {
  tenantId: string;
  connectorSystemId: string;
  teacherId: string;
  sessionName: string;
  businessName?: string;
  captureMode?: string;
  startUrl: string;
}

export interface ConfirmCaptureSegmentSwitchRequest {
  tenantId: string;
  teacherId: string;
  connectorSystemId: string;
  captureSessionId?: string;
  dataInstanceId?: string;
  currentSegmentNo: number;
  nextSegmentNo: number;
  actorType?: string;
  requiredExternalOrgId: string;
  requiredExternalOrgName?: string;
  requiredExternalRoleId: string;
  requiredExternalRoleName?: string;
  externalBusinessId?: string;
  externalBusinessNo?: string;
  targetUrl: string;
  switchReason?: string;
  dataScopeJson?: string;
}

export interface ReportCaptureEventRequest {
  tenantId: string;
  captureSessionId: string;
  sdkSessionId?: string;
  clientEventId: string;
  eventType: string;
  eventTime: IsoDateTime;
  sequenceNo: number;
  retryCount?: number;
  pageUrl?: string;
  targetText?: string;
  targetLocator?: string;
  targetStableKey?: string;
  inputValueMasked?: string;
  eventPayloadJson?: string;
}

export interface ReportResourceSnapshotRequest {
  tenantId: string;
  captureSessionId: string;
  pageUrl: string;
  pageTitle?: string;
  resourceType: string;
  snapshotScope: string;
  resourceName?: string;
  resourceLocator?: string;
  elementSnapshotJson: string;
  metadataJson?: string;
  snapshotHash?: string;
  expireTime?: IsoDateTime;
}

export interface CreateActionDraftRequest {
  tenantId: string;
  captureSessionId: string;
  eventId?: string;
  actionName: string;
  actionType: string;
  sequenceNo: number;
  connectorResourceId?: string;
  suggestedOperationName?: string;
  confirmedOperationName?: string;
  confirmedStepName?: string;
  guideContent?: string;
  practiceHint?: string;
  confirmStatus?: string;
}

export interface ConfirmActionDraftRequest {
  confirmedOperationName: string;
  confirmedStepName: string;
  guideContent?: string;
  practiceHint?: string;
  connectorResourceId?: string;
  updateBy?: string;
}

export const captureApi = {
  createSession(request: CreateCaptureSessionRequest) {
    return apiRequest<CaptureSession>({
      method: 'POST',
      url: '/capture/sessions/create',
      data: request
    });
  },

  listSessions(tenantId: string, teacherId: string) {
    return apiRequest<CaptureSession[]>({
      method: 'GET',
      url: '/capture/sessions',
      params: { tenantId, teacherId }
    });
  },

  finishSession(id: string) {
    return apiRequest<CaptureSession>({
      method: 'POST',
      url: `/capture/sessions/${encodeURIComponent(id)}/finish`
    });
  },

  confirmSegmentSwitch(request: ConfirmCaptureSegmentSwitchRequest) {
    return apiRequest<CaptureSegmentSwitch>({
      method: 'POST',
      url: '/capture/segment-switches/confirm',
      data: request
    });
  },

  reportEvent(request: ReportCaptureEventRequest) {
    return apiRequest<CaptureEvent>({
      method: 'POST',
      url: '/capture/events/report',
      data: request
    });
  },

  listEvents(tenantId: string, captureSessionId: string) {
    return apiRequest<CaptureEvent[]>({
      method: 'GET',
      url: '/capture/events',
      params: { tenantId, captureSessionId }
    });
  },

  reportResourceSnapshot(request: ReportResourceSnapshotRequest) {
    return apiRequest<CaptureResourceSnapshot>({
      method: 'POST',
      url: '/capture/resource-snapshots/report',
      data: request
    });
  },

  listResourceSnapshots(tenantId: string, captureSessionId: string) {
    return apiRequest<CaptureResourceSnapshot[]>({
      method: 'GET',
      url: '/capture/resource-snapshots',
      params: { tenantId, captureSessionId }
    });
  },

  createActionDraft(request: CreateActionDraftRequest) {
    return apiRequest<CaptureActionDraft>({
      method: 'POST',
      url: '/capture/action-drafts/create',
      data: request
    });
  },

  listActionDrafts(tenantId: string, captureSessionId: string) {
    return apiRequest<CaptureActionDraft[]>({
      method: 'GET',
      url: '/capture/action-drafts',
      params: { tenantId, captureSessionId }
    });
  },

  confirmActionDraft(id: string, request: ConfirmActionDraftRequest) {
    return apiRequest<CaptureActionDraft>({
      method: 'POST',
      url: `/capture/action-drafts/${encodeURIComponent(id)}/confirm`,
      data: request
    });
  },

  discardActionDraft(id: string, updateBy?: string) {
    return apiRequest<CaptureActionDraft>({
      method: 'POST',
      url: `/capture/action-drafts/${encodeURIComponent(id)}/discard`,
      data: { updateBy }
    });
  }
};
