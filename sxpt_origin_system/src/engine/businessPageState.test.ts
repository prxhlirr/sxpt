import { describe, expect, it } from 'vitest';
import type { RecordedStep, Rect } from '../types/domain';
import {
  getVisibleOverlayStep,
  isSameBusinessPage,
  shouldForceBoundFrameReload
} from './businessPageState';

const oldRect: Rect = { x: 40, y: 120, width: 180, height: 44 };
const nextRect: Rect = { x: 80, y: 220, width: 240, height: 52 };

function actionStep(url: string): RecordedStep {
  return {
    id: `step-${url}`,
    nodeId: `node-${url}`,
    kind: 'action',
    order: 1,
    title: '绑定动作',
    actionType: 'click',
    selector: '[data-action="target"]',
    rect: oldRect,
    text: '目标',
    url,
    teachingText: '点击目标。',
    practiceHint: '请点击目标。',
    examGoal: '完成目标操作。',
    completionMethod: 'click'
  };
}

describe('businessPageState', () => {
  it('hides an old page overlay after the iframe reaches another page', () => {
    expect(
      getVisibleOverlayStep({
        step: actionStep('/page-a'),
        frameReady: true,
        currentUrl: '/page-b',
        runtimeRect: oldRect
      })
    ).toBeUndefined();
  });

  it('hides every overlay while the iframe is not ready', () => {
    expect(
      getVisibleOverlayStep({
        step: actionStep('/page-a'),
        frameReady: false,
        currentUrl: '/page-a',
        runtimeRect: oldRect
      })
    ).toBeUndefined();
  });

  it('projects the next page live rectangle only after its page is ready', () => {
    expect(
      getVisibleOverlayStep({
        step: actionStep('/page-b'),
        frameReady: true,
        currentUrl: '/page-b',
        runtimeRect: nextRect
      })
    )?.toMatchObject({ rect: nextRect });
  });

  it('gates a centered guide by its recorded page', () => {
    const guide: RecordedStep = {
      ...actionStep('/page-a'),
      kind: 'guide',
      anchor: { mode: 'center' }
    };

    expect(
      getVisibleOverlayStep({
        step: guide,
        frameReady: true,
        currentUrl: '/page-b'
      })
    ).toBeUndefined();
    expect(
      getVisibleOverlayStep({
        step: guide,
        frameReady: true,
        currentUrl: '/page-a'
      })
    ).toBe(guide);
  });

  it('forces a reload when the bound src is stale after internal navigation', () => {
    expect(
      shouldForceBoundFrameReload('/page-a', '/page-b', '/page-a')
    ).toBe(true);
    expect(
      shouldForceBoundFrameReload('/page-a', '/page-a', '/page-a')
    ).toBe(false);
  });

  it('forces a bound-src reload when navigation is still in progress', () => {
    expect(
      shouldForceBoundFrameReload('/page-a', '/page-a', '/page-a', false)
    ).toBe(true);
  });

  it('compares pathname and query while ignoring URL representation', () => {
    expect(
      isSameBusinessPage('/page-b?task=1', 'http://demo.local/page-b?task=1')
    ).toBe(true);
    expect(isSameBusinessPage('/page-b?task=1', '/page-b?task=2')).toBe(false);
  });

  it('treats hash routes as different business pages', () => {
    expect(
      isSameBusinessPage('/app?task=1#/apply', 'http://demo.local/app?task=1#/apply')
    ).toBe(true);
    expect(isSameBusinessPage('/app#/apply', '/app#/approve')).toBe(false);
  });
});
