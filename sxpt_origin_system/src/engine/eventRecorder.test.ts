import { describe, expect, it } from 'vitest';
import type { StudentSession } from '../types/domain';
import { recordEvent } from './eventRecorder';

const session: StudentSession = {
  id: 'session-1',
  lessonFlowId: 'lesson-purchase',
  mode: 'learning',
  nodeResults: {
    'node-create': 'available'
  },
  events: []
};

describe('eventRecorder', () => {
  it('appends a normalized event without mutating the original session', () => {
    const nextSession = recordEvent(session, {
      type: 'business_action',
      url: '/business-demo.html',
      selector: '[data-action="create"]',
      text: '新建申请',
      nodeId: 'node-create',
      timestamp: '2026-07-08T09:00:00.000Z'
    });

    expect(session.events).toEqual([]);
    expect(nextSession.events).toEqual([
      {
        id: 'event-1',
        type: 'business_action',
        url: '/business-demo.html',
        selector: '[data-action="create"]',
        text: '新建申请',
        nodeId: 'node-create',
        timestamp: '2026-07-08T09:00:00.000Z'
      }
    ]);
  });
});
