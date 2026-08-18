import { apiRequest } from './http';
import type { IsoDateTime, TaskStep, TeachingPoint } from './contracts';

export interface CreateTeachingPointRequest {
  tenantId: string;
  connectorSystemId: string;
  pointCode: string;
  pointName: string;
  pointType: string;
  sourceCaptureSessionId?: string;
  businessOverviewJson?: string;
  flowFileId?: string;
  flowFileUrl?: string;
  recordPathJson?: string;
  requiredExternalRoleId?: string;
  requiredExternalRoleName?: string;
  executionStrategy: string;
  defaultGrantStartTime?: IsoDateTime;
  defaultGrantEndTime?: IsoDateTime;
  dataScopeJson?: string;
  overlayPolicyJson?: string;
  description?: string;
  createBy?: string;
}

export interface CreateTaskStepRequest {
  tenantId: string;
  taskId: string;
  teachingPointId: string;
  stepCode: string;
  stepName: string;
  stepDescription?: string;
  sequenceNo: number;
  segmentNo?: number;
  actorType?: string;
  requiredExternalOrgId?: string;
  requiredExternalOrgName?: string;
  requiredExternalRoleId?: string;
  requiredExternalRoleName?: string;
  switchStrategy?: string;
  nextSegmentNo?: number;
  switchConfirmRequired?: boolean;
  switchDecisionSource?: string;
  switchReason?: string;
  rollbackPolicy?: string;
  relatedResourceIds?: string;
  guideContent?: string;
  practiceHint?: string;
  required?: boolean;
  allowSkip?: boolean;
  sourceActionDraftId?: string;
  createBy?: string;
}

export const teachingApi = {
  createPoint(request: CreateTeachingPointRequest) {
    return apiRequest<TeachingPoint>({
      method: 'POST',
      url: '/teaching/points/create',
      data: request
    });
  },

  listPoints(tenantId: string, connectorSystemId: string) {
    return apiRequest<TeachingPoint[]>({
      method: 'GET',
      url: '/teaching/points',
      params: { tenantId, connectorSystemId }
    });
  },

  withdrawPoint(teachingPointId: string) {
    return apiRequest<TeachingPoint>({
      method: 'POST',
      url: `/teaching/points/${teachingPointId}/withdraw`
    });
  },

  createTaskStep(request: CreateTaskStepRequest) {
    return apiRequest<TaskStep>({
      method: 'POST',
      url: '/teaching/task-steps/create',
      data: request
    });
  },

  listTaskSteps(tenantId: string, taskId: string, teachingPointId: string) {
    return apiRequest<TaskStep[]>({
      method: 'GET',
      url: '/teaching/task-steps',
      params: { tenantId, taskId, teachingPointId }
    });
  }
};
