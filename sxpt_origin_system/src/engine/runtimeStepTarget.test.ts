import { describe, expect, it } from 'vitest';
import type { RecordedStep, Rect } from '../types/domain';
import { withRuntimeRect } from './runtimeStepTarget';

const recordedRect: Rect = { x: 20, y: 80, width: 200, height: 40 };
const runtimeRect: Rect = { x: 12, y: 240, width: 340, height: 48 };

const action: RecordedStep = {
  id: 'step-1',
  nodeId: 'node-1',
  order: 1,
  title: '填写供应商',
  actionType: 'input',
  selector: '[data-action="supplier"]',
  rect: recordedRect,
  text: '供应商',
  url: '/business-sdk-demo.html',
  teachingText: '填写供应商。',
  practiceHint: '请填写供应商。',
  examGoal: '独立填写供应商。',
  completionMethod: 'click'
};

describe('withRuntimeRect', () => {
  it('projects a live rectangle onto an action without mutating the recording', () => {
    const projected = withRuntimeRect(action, runtimeRect);

    expect(projected.rect).toEqual(runtimeRect);
    expect(projected).not.toBe(action);
    expect(action.rect).toEqual(recordedRect);
  });

  it('projects a live rectangle onto an element-anchored guide', () => {
    const guide: RecordedStep = {
      ...action,
      id: 'guide-1',
      kind: 'guide',
      anchor: {
        mode: 'element',
        selector: '[data-action="supplier"]',
        rect: recordedRect
      }
    };

    const projected = withRuntimeRect(guide, runtimeRect);

    expect(projected.rect).toEqual(runtimeRect);
    expect(projected.anchor?.rect).toEqual(runtimeRect);
    expect(guide.anchor?.rect).toEqual(recordedRect);
  });
});
