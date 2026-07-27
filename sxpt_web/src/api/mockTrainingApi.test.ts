import { describe, expect, it } from 'vitest';
import { createApiLogStore } from './apiLogStore';
import { MockTrainingApi } from './mockTrainingApi';

class MemoryStorage implements Storage {
  private values = new Map<string, string>();
  get length() { return this.values.size; }
  clear() { this.values.clear(); }
  getItem(key: string) { return this.values.get(key) ?? null; }
  key(index: number) { return Array.from(this.values.keys())[index] ?? null; }
  removeItem(key: string) { this.values.delete(key); }
  setItem(key: string, value: string) { this.values.set(key, value); }
}

function createApi() {
  return new MockTrainingApi(createApiLogStore(), new MemoryStorage());
}

const sessionRequest = {
  tenantId: 'demo-tenant',
  connectorSystemId: 'demo-connector',
  teacherId: 'demo-teacher',
  sessionName: '采购申请备案',
  startUrl: '/business-sdk-demo.html'
};

describe('MockTrainingApi', () => {
  it('creates and finishes one capture session', async () => {
    const api = createApi();
    const created = await api.createCaptureSession(sessionRequest);

    expect(created.sessionStatus).toBe('RUNNING');
    await expect(api.finishCaptureSession(created.id)).resolves.toMatchObject({
      id: created.id,
      sessionStatus: 'FINISHED'
    });
    await expect(api.finishCaptureSession(created.id)).rejects.toMatchObject({
      message: 'STATE_NOT_ALLOWED'
    });
  });

  it('deduplicates events by session and client event id', async () => {
    const api = createApi();
    const session = await api.createCaptureSession(sessionRequest);
    const request = {
      tenantId: 'demo-tenant',
      captureSessionId: session.id,
      clientEventId: 'client-event-1',
      eventType: 'INPUT',
      eventTime: '2026-07-16T01:00:00.000Z',
      sequenceNo: 2,
      pageUrl: '/business-sdk-demo.html'
    };

    const first = await api.reportCaptureEvent(request);
    const second = await api.reportCaptureEvent(request);

    expect(second.id).toBe(first.id);
    expect(await api.listCaptureEvents('demo-tenant', session.id)).toHaveLength(1);
  });

  it('sorts events by sequence number', async () => {
    const api = createApi();
    const session = await api.createCaptureSession(sessionRequest);

    for (const sequenceNo of [3, 1, 2]) {
      await api.reportCaptureEvent({
        tenantId: 'demo-tenant',
        captureSessionId: session.id,
        clientEventId: `event-${sequenceNo}`,
        eventType: 'CLICK',
        eventTime: `2026-07-16T01:00:0${sequenceNo}.000Z`,
        sequenceNo
      });
    }

    expect(
      (await api.listCaptureEvents('demo-tenant', session.id)).map(
        (event) => event.sequenceNo
      )
    ).toEqual([1, 2, 3]);
  });

  it('only confirms pending action drafts', async () => {
    const api = createApi();
    const session = await api.createCaptureSession(sessionRequest);
    const draft = await api.createActionDraft({
      tenantId: 'demo-tenant',
      captureSessionId: session.id,
      actionName: '填写供应商',
      actionType: 'INPUT',
      sequenceNo: 1
    });

    await api.confirmActionDraft(draft.id, {
      confirmedOperationName: '填写供应商',
      confirmedStepName: '填写供应商',
      guideContent: '录入供应商名称'
    });

    await expect(api.discardActionDraft(draft.id, {})).rejects.toMatchObject({
      message: 'STATE_NOT_ALLOWED'
    });
  });

  it('returns launch context information for a segment switch', async () => {
    const api = createApi();
    const result = await api.confirmSegmentSwitch({
      tenantId: 'demo-tenant',
      teacherId: 'demo-teacher',
      connectorSystemId: 'demo-connector',
      currentSegmentNo: 1,
      nextSegmentNo: 2,
      requiredExternalOrgId: 'org-finance',
      requiredExternalRoleId: 'role-approver',
      targetUrl: '/business-approval-demo.html'
    });

    expect(result).toMatchObject({
      switchConfirmRequired: true,
      switchDecisionSource: 'TEACHER_PATH'
    });
    expect(result.launchToken).toBeTruthy();
  });
});
