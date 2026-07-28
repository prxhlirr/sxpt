import { describe, expect, it } from 'vitest';
import type { NodeBinding, RecordedEvent } from '../types/domain';
import { matchesBinding } from './bindingMatcher';

const binding: NodeBinding = {
  url: '/business-demo.html',
  selector: '[data-action="create"]',
  rect: { x: 84, y: 112, width: 132, height: 44 },
  actionText: '新建申请'
};

describe('bindingMatcher', () => {
  it('matches events with the same page and selector', () => {
    const event: RecordedEvent = {
      id: 'event-1',
      type: 'business_action',
      url: 'http://localhost:5173/business-demo.html',
      selector: '[data-action="create"]',
      text: '新建申请',
      timestamp: '2026-07-08T09:00:00.000Z'
    };

    expect(matchesBinding(event, binding)).toBe(true);
  });

  it('rejects events for the wrong selector', () => {
    const event: RecordedEvent = {
      id: 'event-1',
      type: 'business_action',
      url: '/business-demo.html',
      selector: '[data-action="submit"]',
      text: '提交申请',
      timestamp: '2026-07-08T09:00:00.000Z'
    };

    expect(matchesBinding(event, binding)).toBe(false);
  });
});
