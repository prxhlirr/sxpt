import { describe, expect, it } from 'vitest';
import { createEmptyRecordedFlow } from './lessonFlows';

describe('createEmptyRecordedFlow', () => {
  it('starts with one empty segment and no compatibility steps', () => {
    const flow = createEmptyRecordedFlow();

    expect(flow.publishStatus).toBe('draft');
    expect(flow.source).toBe('custom');
    expect(flow.steps).toEqual([]);
    expect(flow.segments).toHaveLength(1);
    expect(flow.segments?.[0]).toMatchObject({
      segmentNo: 1,
      targetUrl: '/business-sdk-demo.html',
      status: 'recording',
      steps: []
    });
  });

  it('creates independent flow and segment identifiers', () => {
    const first = createEmptyRecordedFlow();
    const second = createEmptyRecordedFlow();

    expect(first.id).not.toBe(second.id);
    expect(first.segments?.[0].id).not.toBe(second.segments?.[0].id);
  });
});
