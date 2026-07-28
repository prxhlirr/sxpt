import { describe, expect, it } from 'vitest';
import type { BusinessActionPayload, RecordedFlow, RecordedStep } from '../types/domain';
import {
  createRecordedStep,
  doesActionMatchStep,
  getPlaybackStep,
  getPlaybackEntries,
  moveRecordedStep
} from './recordingFlow';

const payload: BusinessActionPayload = {
  actionType: 'input',
  url: '/business-demo.html',
  selector: '[data-action="supplier"]',
  text: '供应商',
  value: '上海示例供应商有限公司',
  rect: { x: 270, y: 202, width: 260, height: 62 },
  timestamp: '2026-07-08T10:00:00.000Z'
};

function step(id: string, order: number): RecordedStep {
  return {
    id,
    nodeId: `node-${id}`,
    order,
    title: `步骤 ${order}`,
    actionType: 'click',
    selector: `[data-action="${id}"]`,
    rect: { x: order * 10, y: 20, width: 80, height: 36 },
    text: `动作 ${id}`,
    url: '/business-demo.html',
    teachingText: `说明 ${id}`,
    practiceHint: `提示 ${id}`,
    examGoal: `目标 ${id}`,
    completionMethod: 'click'
  };
}

describe('recordingFlow', () => {
  it('creates a teaching step from a recorded business action', () => {
    const recordedStep = createRecordedStep(payload, 2);

    expect(recordedStep).toMatchObject({
      id: 'step-2',
      nodeId: 'node-2',
      order: 2,
      title: '供应商',
      actionType: 'input',
      selector: '[data-action="supplier"]',
      value: '上海示例供应商有限公司',
      teachingText: '请完成：供应商'
    });
  });

  it('moves a step up and reorders the flow', () => {
    const flow: RecordedFlow = {
      id: 'recorded-flow',
      title: '采购申请录制流程',
      businessUrl: '/business-demo.html',
      source: 'custom',
      publishStatus: 'draft',
      steps: [step('create', 1), step('supplier', 2), step('submit', 3)]
    };

    const nextFlow = moveRecordedStep(flow, 'node-supplier', 'up');

    expect(nextFlow.steps.map((item) => item.nodeId)).toEqual([
      'node-supplier',
      'node-create',
      'node-submit'
    ]);
    expect(nextFlow.steps.map((item) => item.order)).toEqual([1, 2, 3]);
  });

  it('returns the playback step by zero-based index', () => {
    const flow: RecordedFlow = {
      id: 'recorded-flow',
      title: '采购申请录制流程',
      businessUrl: '/business-demo.html',
      source: 'template',
      publishStatus: 'published',
      steps: [step('create', 1), step('submit', 2)]
    };

    expect(getPlaybackStep(flow, 1)?.nodeId).toBe('node-submit');
    expect(getPlaybackStep(flow, 2)).toBeUndefined();
  });

  it('matches a student action to the current recorded step', () => {
    const recordedStep = createRecordedStep(payload, 1);

    expect(doesActionMatchStep(payload, recordedStep)).toBe(true);
    expect(
      doesActionMatchStep(
        {
          ...payload,
          actionType: 'click',
          selector: '[data-action="submit"]'
        },
        recordedStep
      )
    ).toBe(false);
  });

  it('orders playback entries by segment and step order', () => {
    const first = step('create', 1);
    const second = step('submit', 2);
    const approval = { ...step('approve', 1), url: '/business-approval-demo.html' };
    const flow: RecordedFlow = {
      id: 'segmented-flow',
      title: '分段采购流程',
      businessUrl: '/business-sdk-demo.html',
      source: 'custom',
      publishStatus: 'published',
      steps: [approval, first, second],
      segments: [
        {
          id: 'segment-2',
          segmentNo: 2,
          title: '审批段',
          externalRoleName: '审批人',
          targetUrl: '/business-approval-demo.html',
          status: 'saved',
          steps: [approval]
        },
        {
          id: 'segment-1',
          segmentNo: 1,
          title: '申请段',
          externalRoleName: '申请人',
          targetUrl: '/business-demo.html',
          status: 'saved',
          steps: [second, first]
        }
      ]
    };

    const entries = getPlaybackEntries(flow);

    expect(entries.map((entry) => entry.step.id)).toEqual([
      'create',
      'submit',
      'approve'
    ]);
    expect(entries[2]).toMatchObject({
      segmentNo: 2,
      roleName: '审批人',
      requiresNavigation: true,
      isSegmentStart: true
    });
  });

  it('never matches a guide step to a business action', () => {
    const guide: RecordedStep = {
      ...step('guide', 1),
      kind: 'guide',
      anchor: { mode: 'center' },
      selector: payload.selector,
      actionType: payload.actionType
    };

    expect(doesActionMatchStep(payload, guide)).toBe(false);
  });
});
