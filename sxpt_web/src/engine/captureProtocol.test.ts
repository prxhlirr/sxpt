import { describe, expect, it } from 'vitest';
import type { BusinessActionPayload, RecordedStep } from '../types/domain';
import {
  isBusinessNavigatingMessage,
  isBusinessReadyMessage,
  isElementPickedMessage,
  isPlaybackResultMessage,
  isTargetResolutionResultMessage,
  maskInputValue,
  toCaptureEventRequest
} from './captureProtocol';
import { createStepPlaybackMessage } from './stepPlayback';

const action: BusinessActionPayload = {
  actionType: 'input',
  url: '/business-sdk-demo.html',
  selector: '[data-action="supplier"]',
  selectorCandidates: [
    '[data-action="supplier"]',
    'input[name="supplier"]'
  ],
  text: '供应商',
  value: '13800138000',
  rect: { x: 20, y: 80, width: 200, height: 40 },
  timestamp: '2026-07-16T01:00:00.000Z',
  clientEventId: 'client-event-1',
  sdkSessionId: 'sdk-session-1',
  sequenceNo: 4,
  stableKey: 'supplier',
  recordedViewport: { width: 1920, height: 1080, devicePixelRatio: 1 }
};

const step: RecordedStep = {
  id: 'step-1',
  nodeId: 'node-1',
  order: 1,
  title: '填写供应商',
  actionType: 'input',
  selector: '[data-action="supplier"]',
  rect: { x: 20, y: 80, width: 200, height: 40 },
  text: '供应商',
  value: '上海示例供应商有限公司',
  url: '/business-sdk-demo.html',
  teachingText: '填写供应商名称。',
  practiceHint: '填写供应商。',
  examGoal: '正确填写供应商。',
  completionMethod: 'click'
};

describe('captureProtocol', () => {
  it('converts an SDK action to the event report contract', () => {
    const request = toCaptureEventRequest(action, {
        tenantId: 'demo-tenant',
        captureSessionId: 'capture-1'
      });

    expect(request).toMatchObject({
      tenantId: 'demo-tenant',
      captureSessionId: 'capture-1',
      sdkSessionId: 'sdk-session-1',
      clientEventId: 'client-event-1',
      eventType: 'INPUT',
      sequenceNo: 4,
      pageUrl: '/business-sdk-demo.html',
      targetStableKey: 'supplier',
      inputValueMasked: '13*******00'
    });
    expect(JSON.parse(request.eventPayloadJson ?? '{}')).toMatchObject({
      selectorCandidates: [
        '[data-action="supplier"]',
        'input[name="supplier"]'
      ],
      recordedViewport: { width: 1920, height: 1080, devicePixelRatio: 1 }
    });
  });

  it('masks captured form values', () => {
    expect(maskInputValue('13800138000')).toBe('13*******00');
    expect(maskInputValue('AB')).toBe('**');
  });

  it('recognizes ready and element picked messages', () => {
    expect(
      isBusinessReadyMessage({
        type: 'BUSINESS_READY',
        url: '/purchase',
        title: '采购申请',
        sdkSessionId: 'sdk-1',
        timestamp: '2026-07-16T01:00:00.000Z'
      })
    ).toBe(true);
    expect(
      isElementPickedMessage({
        type: 'ELEMENT_PICKED',
        url: '/purchase',
        selector: '#amount',
        text: '申请金额',
        rect: { x: 1, y: 2, width: 3, height: 4 }
      })
    ).toBe(true);
  });

  it('recognizes only complete business navigating messages', () => {
    expect(
      isBusinessNavigatingMessage({
        type: 'BUSINESS_NAVIGATING',
        url: '/purchase',
        timestamp: '2026-07-21T03:00:00.000Z'
      })
    ).toBe(true);
    expect(
      isBusinessNavigatingMessage({
        type: 'BUSINESS_NAVIGATING',
        url: '/purchase'
      })
    ).toBe(false);
  });

  it('correlates playback messages with request ids', () => {
    expect(createStepPlaybackMessage(step, 'play-1')).toMatchObject({
      type: 'PLAY_RECORDED_STEP',
      requestId: 'play-1'
    });
  });

  it('recognizes only complete target resolution results', () => {
    expect(
      isTargetResolutionResultMessage({
        type: 'TARGET_RESOLUTION_RESULT',
        requestId: 'resolve-1',
        success: true,
        url: '/purchase',
        selector: '#amount',
        rect: { x: 1, y: 2, width: 3, height: 4 },
        viewport: { width: 1366, height: 768, devicePixelRatio: 1 }
      })
    ).toBe(true);
    expect(
      isTargetResolutionResultMessage({
        type: 'TARGET_RESOLUTION_RESULT',
        success: true,
        url: '/purchase'
      })
    ).toBe(false);
  });

  it('accepts cancellation errors and rejects unknown playback errors', () => {
    expect(
      isPlaybackResultMessage({
        type: 'PLAYBACK_RESULT',
        requestId: 'play-canceled',
        success: false,
        url: '/purchase',
        error: 'ACTION_CANCELED'
      })
    ).toBe(true);
    expect(
      isTargetResolutionResultMessage({
        type: 'TARGET_RESOLUTION_RESULT',
        requestId: 'resolve-canceled',
        success: false,
        url: '/purchase',
        error: 'ACTION_CANCELED'
      })
    ).toBe(true);
    expect(
      isPlaybackResultMessage({
        type: 'PLAYBACK_RESULT',
        requestId: 'play-unknown',
        success: false,
        url: '/purchase',
        error: 'UNKNOWN_ERROR'
      })
    ).toBe(false);
  });
});
