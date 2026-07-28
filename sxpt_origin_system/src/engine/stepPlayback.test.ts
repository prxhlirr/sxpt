import { describe, expect, it } from 'vitest';
import type { RecordedStep } from '../types/domain';
import {
  createStepPlaybackMessage,
  createTargetResolutionMessage
} from './stepPlayback';

const step: RecordedStep = {
  id: 'step-2',
  nodeId: 'record-node-supplier',
  order: 2,
  title: '填写供应商',
  actionType: 'input',
  selector: '[data-action="supplier"]',
  selectorCandidates: [
    '[data-action="supplier"]',
    'input[name="supplier"]'
  ],
  stableKey: 'supplier',
  rect: { x: 250, y: 224, width: 260, height: 40 },
  text: '供应商',
  value: '上海示例供应商有限公司',
  url: '/business-demo.html',
  teachingText: '在供应商字段中录入本次采购对应的供应商名称。',
  practiceHint: '填写供应商名称。',
  examGoal: '正确填写供应商信息。',
  completionMethod: 'click'
};

describe('stepPlayback', () => {
  it('creates a postMessage payload that can replay a recorded form step', () => {
    expect(createStepPlaybackMessage(step)).toEqual({
      type: 'PLAY_RECORDED_STEP',
      step: {
        actionType: 'input',
        selector: '[data-action="supplier"]',
        selectorCandidates: [
          '[data-action="supplier"]',
          'input[name="supplier"]'
        ],
        stableKey: 'supplier',
        value: '上海示例供应商有限公司'
      }
    });
    expect(createStepPlaybackMessage(step).step).not.toHaveProperty('rect');
  });

  it('creates a locator-only request for the current responsive layout', () => {
    expect(createTargetResolutionMessage(step, 'resolve-1')).toEqual({
      type: 'RESOLVE_RECORDED_TARGET',
      requestId: 'resolve-1',
      locator: {
        selector: '[data-action="supplier"]',
        selectorCandidates: [
          '[data-action="supplier"]',
          'input[name="supplier"]'
        ],
        stableKey: 'supplier'
      }
    });
  });
});
