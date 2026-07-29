import { describe, expect, it } from 'vitest';
import type { BusinessActionPayload } from '../types/domain';
import {
  createBusinessActionSignature,
  shouldSkipDuplicateBusinessAction
} from './businessActionDeduper';

const payload: BusinessActionPayload = {
  actionType: 'input',
  url: '/business-sdk-demo.html',
  selector: '[data-action="supplier"]',
  text: '供应商',
  value: '上海示例供应商有限公司',
  rect: { x: 100, y: 120, width: 200, height: 40 },
  timestamp: '2026-07-13T00:00:00.000Z'
};

describe('businessActionDeduper', () => {
  it('builds a stable signature without volatile timestamp and rect fields', () => {
    expect(createBusinessActionSignature(payload)).toBe(
      'input|/business-sdk-demo.html|[data-action="supplier"]|上海示例供应商有限公司'
    );
  });

  it('skips the same action repeated inside the duplicate window', () => {
    const last = {
      signature: createBusinessActionSignature(payload),
      at: 1000
    };

    expect(shouldSkipDuplicateBusinessAction(payload, last, 1200)).toBe(true);
  });

  it('keeps the same action when it happens outside the duplicate window', () => {
    const last = {
      signature: createBusinessActionSignature(payload),
      at: 1000
    };

    expect(shouldSkipDuplicateBusinessAction(payload, last, 1500)).toBe(false);
  });
});
