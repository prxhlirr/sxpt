import { describe, expect, it } from 'vitest';
import type { BusinessActionPayload, RecordedFlow } from '../types/domain';
import {
  appendActionStep,
  createCaptureWorkspace,
  getCaptureCommandState,
  insertGuideStep,
  migrateRecordedFlow,
  moveStepWithinSegment,
  truncateSegmentFromStep,
  undoLastLocalStep
} from './captureWorkspace';

const legacyFlow: RecordedFlow = {
  id: 'legacy-flow',
  title: '采购申请录制流程',
  businessUrl: '/business-sdk-demo.html',
  source: 'custom',
  publishStatus: 'draft',
  steps: [
    {
      id: 'step-1',
      nodeId: 'node-1',
      order: 1,
      title: '新建申请',
      actionType: 'click',
      selector: '[data-action="create"]',
      rect: { x: 10, y: 20, width: 100, height: 40 },
      text: '新建申请',
      url: '/business-sdk-demo.html',
      teachingText: '点击新建申请。',
      practiceHint: '找到新建申请按钮。',
      examGoal: '独立创建申请。',
      completionMethod: 'click'
    }
  ]
};

const action: BusinessActionPayload = {
  actionType: 'input',
  url: '/business-sdk-demo.html',
  selector: '[data-action="supplier"]',
  selectorCandidates: [
    '[data-action="supplier"]',
    'input[name="supplier"]'
  ],
  text: '供应商',
  value: '上海示例供应商有限公司',
  rect: { x: 20, y: 80, width: 200, height: 40 },
  timestamp: '2026-07-16T01:00:00.000Z',
  recordedViewport: { width: 1920, height: 1080, devicePixelRatio: 1 }
};

describe('captureWorkspace', () => {
  it('preserves responsive locator metadata on captured steps', () => {
    const workspace = appendActionStep(
      createCaptureWorkspace({ ...legacyFlow, steps: [], segments: undefined }),
      action
    );

    expect(workspace.flow.segments?.[0].steps[0]).toMatchObject({
      selectorCandidates: [
        '[data-action="supplier"]',
        'input[name="supplier"]'
      ],
      recordedViewport: { width: 1920, height: 1080, devicePixelRatio: 1 }
    });
  });

  it('migrates a legacy flat flow into segment one', () => {
    const migrated = migrateRecordedFlow(legacyFlow);

    expect(migrated.segments).toHaveLength(1);
    expect(migrated.segments?.[0]).toMatchObject({ segmentNo: 1 });
    expect(migrated.segments?.[0].steps).toHaveLength(1);
  });

  it('undoes only the final local step in the active segment', () => {
    let workspace = createCaptureWorkspace(legacyFlow);
    workspace = appendActionStep(workspace, action);

    const next = undoLastLocalStep(workspace);

    expect(next.flow.segments?.[0].steps.map((step) => step.id)).toEqual([
      'step-1'
    ]);
  });

  it('does not undo a confirmed step', () => {
    let workspace = createCaptureWorkspace(legacyFlow);
    workspace.flow.segments![0].steps[0].persistence = {
      status: 'confirmed',
      actionDraftId: 'draft-1'
    };

    expect(undoLastLocalStep(workspace)).toEqual(workspace);
  });

  it('truncates the selected node and later nodes for rerecord', () => {
    let workspace = createCaptureWorkspace(legacyFlow);
    workspace = appendActionStep(workspace, action);

    const selectedId = workspace.flow.segments![0].steps[1].id;
    const next = truncateSegmentFromStep(workspace, selectedId);

    expect(next.flow.segments?.[0].steps).toHaveLength(1);
  });

  it('inserts a guide step after the selected action', () => {
    const workspace = createCaptureWorkspace(legacyFlow);
    const next = insertGuideStep(workspace, 'step-1');

    expect(next.flow.segments?.[0].steps[1]).toMatchObject({
      kind: 'guide',
      title: '操作说明'
    });
  });

  it('uses the current business page for a newly inserted guide step', () => {
    const workspace = createCaptureWorkspace(legacyFlow);
    const next = insertGuideStep(
      workspace,
      'step-1',
      '/business-approval-demo.html?tab=detail'
    );

    expect(next.flow.segments?.[0].steps[1]?.url).toBe(
      '/business-approval-demo.html?tab=detail'
    );
  });

  it('moves a step only within its segment', () => {
    let workspace = createCaptureWorkspace(legacyFlow);
    workspace = appendActionStep(workspace, action);
    const secondId = workspace.flow.segments![0].steps[1].id;

    const next = moveStepWithinSegment(workspace, secondId, 'up');

    expect(next.flow.segments?.[0].steps.map((step) => step.id)).toEqual([
      secondId,
      'step-1'
    ]);
    expect(next.flow.segments?.[0].steps.map((step) => step.order)).toEqual([
      1,
      2
    ]);
  });

  it('enables capture commands only after a session starts', () => {
    const workspace = createCaptureWorkspace(legacyFlow);
    expect(getCaptureCommandState(workspace)).toMatchObject({
      canStart: true,
      canSaveSegment: false
    });

    workspace.sessionStatus = 'running';
    workspace.session = { id: 'capture-1' };
    expect(getCaptureCommandState(workspace)).toMatchObject({
      canStart: false,
      canSaveSegment: true,
      canInsertGuide: true
    });
  });
});
