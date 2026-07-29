import { describe, expect, it } from 'vitest';
import { createEmptyRecordedFlow, purchaseRecordedFlow } from '../mock/lessonFlows';
import { canEnterPublishedFlow, hasRecordedContent } from './publishedFlowAccess';

describe('publishedFlowAccess', () => {
  it('rejects missing, draft, and empty published flows', () => {
    const empty = createEmptyRecordedFlow();

    expect(canEnterPublishedFlow(undefined)).toBe(false);
    expect(canEnterPublishedFlow(empty)).toBe(false);
    expect(
      canEnterPublishedFlow({ ...empty, publishStatus: 'published' })
    ).toBe(false);
  });

  it('accepts only a published flow with recorded content', () => {
    expect(canEnterPublishedFlow(purchaseRecordedFlow)).toBe(true);
    expect(hasRecordedContent(purchaseRecordedFlow)).toBe(true);
    expect(hasRecordedContent(createEmptyRecordedFlow())).toBe(false);
  });
});
