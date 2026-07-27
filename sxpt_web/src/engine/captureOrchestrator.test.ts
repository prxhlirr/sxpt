import { describe, expect, it } from 'vitest';
import type { TrainingRuntimeConfig } from '../config/trainingConfig';
import type {
  ActionDraftVO,
  CaptureEventVO,
  CaptureSegmentSwitchVO,
  CaptureSessionVO,
  ConnectorResourceVO,
  ResourceSnapshotVO,
  TaskStepVO,
  TeachingPointVO,
  TrainingApi
} from '../types/trainingApi';
import type { BusinessActionPayload, RecordedFlow } from '../types/domain';
import { appendActionStep, createCaptureWorkspace, startWorkspaceSession } from './captureWorkspace';
import {
  buildRecordedStageBinding,
  publishRecordedStage,
  publishTeachingPoint,
  saveSegment,
  startCapture,
  switchSegment
} from './captureOrchestrator';

const config: TrainingRuntimeConfig = {
  apiMode: 'mock',
  runBatchApiMode: 'mock',
  apiBaseUrl: 'http://localhost:8080/api/v1',
  bearerToken: '',
  requestTimeoutMs: 8000,
  tenantId: 'demo-tenant',
  connectorSystemId: 'demo-connector',
  teacherId: 'demo-teacher',
  teacherName: '演示教师'
};

const flow: RecordedFlow = {
  id: 'flow-1',
  title: '采购申请备案',
  businessUrl: '/business-sdk-demo.html',
  source: 'custom',
  publishStatus: 'draft',
  steps: []
};

const action: BusinessActionPayload = {
  actionType: 'input',
  url: '/business-sdk-demo.html',
  selector: '[data-action="supplier"]',
  text: '供应商',
  value: '上海示例供应商有限公司',
  rect: { x: 20, y: 80, width: 200, height: 40 },
  timestamp: '2026-07-16T01:00:00.000Z',
  clientEventId: 'client-event-1',
  sequenceNo: 1,
  stableKey: 'supplier'
};

class RecordingApi implements TrainingApi {
  calls: string[] = [];
  failAt?: string;
  teachingPointRequest?: Parameters<TrainingApi['createTeachingPoint']>[0];

  private mark(name: string) {
    this.calls.push(name);
    if (this.failAt === name) throw new Error(`${name} failed`);
  }

  async createCaptureSession(request: Parameters<TrainingApi['createCaptureSession']>[0]) {
    this.mark('createSession');
    const now = new Date().toISOString();
    return { ...request, id: 'capture-1', captureMode: 'STANDARD' as const, sessionStatus: 'RUNNING' as const, status: 'ACTIVE' as const, createTime: now, updateTime: now } satisfies CaptureSessionVO;
  }
  async listCaptureSessions() { return []; }
  async finishCaptureSession(id: string) {
    this.mark('finishSession');
    const now = new Date().toISOString();
    return { tenantId: 'demo-tenant', connectorSystemId: 'demo-connector', teacherId: 'demo-teacher', sessionName: '备案', startUrl: '/', captureMode: 'STANDARD' as const, id, sessionStatus: 'FINISHED' as const, status: 'ACTIVE' as const, createTime: now, updateTime: now } satisfies CaptureSessionVO;
  }
  async confirmSegmentSwitch(request: Parameters<TrainingApi['confirmSegmentSwitch']>[0]) {
    this.mark('switchSegment');
    return { ...request, id: 'switch-1', launchContextId: 'launch-1', launchToken: 'token-1', expireTime: new Date().toISOString(), switchConfirmRequired: true as const, switchDecisionSource: 'TEACHER_PATH' as const } satisfies CaptureSegmentSwitchVO;
  }
  async reportCaptureEvent(request: Parameters<TrainingApi['reportCaptureEvent']>[0]) {
    this.mark('reportEvent');
    const { eventPayloadJson: _omitted, ...safe } = request;
    return { ...safe, id: 'event-1', createTime: new Date().toISOString() } satisfies CaptureEventVO;
  }
  async listCaptureEvents() { return []; }
  async reportResourceSnapshot(request: Parameters<TrainingApi['reportResourceSnapshot']>[0]) {
    this.mark('reportSnapshot');
    return { ...request, id: 'snapshot-1', createTime: new Date().toISOString() } satisfies ResourceSnapshotVO;
  }
  async listResourceSnapshots() { return []; }
  async createActionDraft(request: Parameters<TrainingApi['createActionDraft']>[0]) {
    this.mark('createDraft');
    const { confirmStatus: _status, ...safe } = request;
    const now = new Date().toISOString();
    return { ...safe, id: 'draft-1', confirmStatus: 'PENDING' as const, createTime: now, updateTime: now } satisfies ActionDraftVO;
  }
  async listActionDrafts() { return []; }
  async confirmActionDraft(id: string, request: Parameters<TrainingApi['confirmActionDraft']>[1]) {
    this.mark('confirmDraft');
    const now = new Date().toISOString();
    return { tenantId: 'demo-tenant', captureSessionId: 'capture-1', actionName: request.confirmedOperationName, actionType: 'INPUT', sequenceNo: 1, id, ...request, confirmStatus: 'CONFIRMED' as const, createTime: now, updateTime: now } satisfies ActionDraftVO;
  }
  async discardActionDraft(id: string) {
    const now = new Date().toISOString();
    return {
      tenantId: 'demo-tenant',
      captureSessionId: 'capture-1',
      actionName: 'unused',
      actionType: 'CLICK',
      sequenceNo: 1,
      id,
      confirmStatus: 'DISCARDED' as const,
      createTime: now,
      updateTime: now
    } satisfies ActionDraftVO;
  }
  async createConnectorResource(request: Parameters<TrainingApi['createConnectorResource']>[0]) {
    this.mark('createResource');
    return { ...request, id: 'resource-1', status: 'ACTIVE' as const, createTime: new Date().toISOString() } satisfies ConnectorResourceVO;
  }
  async listConnectorResources() { return []; }
  async createTeachingPoint(request: Parameters<TrainingApi['createTeachingPoint']>[0]) {
    this.mark('createTeachingPoint');
    this.teachingPointRequest = request;
    return { ...request, id: 'point-1', versionNo: 1, pointStatus: 'PUBLISHED' as const, status: 'ACTIVE' as const, createTime: new Date().toISOString() } satisfies TeachingPointVO;
  }
  async listTeachingPoints() { return []; }
  async createTaskStep(request: Parameters<TrainingApi['createTaskStep']>[0]) {
    this.mark('createTaskStep');
    const now = new Date().toISOString();
    return {
      ...request,
      id: `task-step-${request.sequenceNo}`,
      status: 'ACTIVE' as const,
      createTime: now,
      updateTime: now
    } satisfies TaskStepVO;
  }
  async listTaskSteps() { return []; }
}

function runningWorkspace() {
  const base = startWorkspaceSession(
    createCaptureWorkspace(flow),
    {
      ...sessionForTest(),
      id: 'capture-1'
    }
  );
  return appendActionStep(base, action);
}

function sessionForTest(): CaptureSessionVO {
  const now = new Date().toISOString();
  return {
    tenantId: 'demo-tenant',
    connectorSystemId: 'demo-connector',
    teacherId: 'demo-teacher',
    sessionName: '采购申请备案',
    startUrl: '/business-sdk-demo.html',
    captureMode: 'STANDARD',
    id: 'capture-1',
    sessionStatus: 'RUNNING',
    status: 'ACTIVE',
    createTime: now,
    updateTime: now
  };
}

describe('captureOrchestrator', () => {
  it('creates a session before returning a running workspace', async () => {
    const api = new RecordingApi();
    const next = await startCapture(api, config, createCaptureWorkspace(flow));

    expect(api.calls).toEqual(['createSession']);
    expect(next.session?.id).toBe('capture-1');
    expect(next.sessionStatus).toBe('running');
  });

  it('saves events, snapshots, and drafts before marking a segment saved', async () => {
    const api = new RecordingApi();
    const workspace = runningWorkspace();
    const next = await saveSegment(api, config, workspace, workspace.activeSegmentId!);

    expect(api.calls).toEqual(['reportEvent', 'reportSnapshot', 'createDraft']);
    expect(next.flow.segments?.[0].status).toBe('saved');
  });

  it('does not switch segment when save fails', async () => {
    const api = new RecordingApi();
    api.failAt = 'createDraft';
    const workspace = runningWorkspace();

    await expect(
      switchSegment(api, config, workspace, {
        nextSegmentNo: 2,
        requiredExternalOrgId: 'org-finance',
        requiredExternalRoleId: 'role-approver',
        targetUrl: '/business-approval-demo.html'
      })
    ).rejects.toThrow('createDraft failed');

    expect(api.calls).not.toContain('switchSegment');
  });

  it('publishes a point before finishing the session', async () => {
    const api = new RecordingApi();
    const workspace = runningWorkspace();

    await publishTeachingPoint(api, config, workspace, {
      pointCode: 'PURCHASE_APPLY',
      pointName: '采购申请备案'
    });

    expect(api.calls.slice(-2)).toEqual(['createTeachingPoint', 'finishSession']);
  });

  it('does not put an inline guide image into the backend record path', async () => {
    const api = new RecordingApi();
    const workspace = runningWorkspace();
    workspace.flow.segments![0].steps[0].guideImage = {
      src: 'data:image/webp;base64,AAAA',
      name: 'guide.webp',
      mimeType: 'image/webp',
      width: 640,
      height: 360,
      storageType: 'inline'
    };

    await publishTeachingPoint(api, config, workspace, {
      pointCode: 'PURCHASE_APPLY',
      pointName: '采购申请备案'
    });

    expect(api.teachingPointRequest?.recordPathJson).not.toContain('data:image');
    expect(api.teachingPointRequest?.recordPathJson).not.toContain('guideImage');
  });

  it('builds a stage binding with full recorded step semantics', () => {
    const workspace = runningWorkspace();
    workspace.flow.segments![0].steps[0].required = true;
    workspace.flow.segments![0].steps[0].completionMethod = 'click';
    workspace.flow.segments![0].steps[0].failurePolicy = 'retry';
    workspace.flow.segments![0].steps[0].persistence = {
      status: 'confirmed',
      actionDraftId: 'draft-1',
      taskStepId: 'task-step-1'
    };

    const binding = buildRecordedStageBinding({
      workflowDraftId: 'workflow-1',
      stageId: 'stage-submit',
      workspace,
      stageMaxScore: 30
    });

    expect(binding).toMatchObject({
      workflowDraftId: 'workflow-1',
      stageId: 'stage-submit',
      recordedSegmentId: 'draft-1',
      recordedAssetVersion: 1
    });
    expect(binding.steps[0]).toMatchObject({
      taskStepId: 'task-step-1',
      recordedSegmentId: 'draft-1',
      sequenceNo: 1,
      required: true,
      completionMethod: 'click',
      failurePolicy: 'retry',
      score: 30,
      recordedAssetVersion: 1,
      matcher: {
        selector: '[data-action="supplier"]',
        actionType: 'input'
      }
    });
  });

  it('publishes one recorded stage, creates TaskSteps, and returns its binding', async () => {
    const api = new RecordingApi();

    const result = await publishRecordedStage(
      api,
      config,
      runningWorkspace(),
      {
        workflowDraftId: 'workflow-1',
        stageId: 'stage-submit',
        taskId: 'task-1',
        stageSequenceNo: 1,
        stageMaxScore: 30,
        externalOrgId: 'org-1',
        externalRoleId: 'handler',
        pointCode: 'STAGE_SUBMIT',
        pointName: '提交阶段'
      }
    );

    expect(api.calls.slice(-3)).toEqual([
      'createTeachingPoint',
      'finishSession',
      'createTaskStep'
    ]);
    expect(result.binding.steps).toEqual([
      expect.objectContaining({
        taskStepId: 'task-step-1',
        recordedSegmentId: 'draft-1',
        recordedAssetVersion: 1,
        score: 30
      })
    ]);
  });
});
