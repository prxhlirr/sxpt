import { describe, expect, it } from 'vitest';
import {
  createBusinessStepPreviewClearMessages,
  createBusinessStepPreviewMessages
} from './businessStepPreview';

describe('business step preview protocol', () => {
  it('sends the selected node locator to both supported business iframe protocols', () => {
    expect(
      createBusinessStepPreviewMessages('preview-12', {
        selector: '[data-training-id="approve"]',
        selectorCandidates: [
          '[data-training-id="approve"]',
          '#approve-button',
          '[data-training-id="approve"]',
          ''
        ],
        url: 'https://oa.example.test/approval'
      })
    ).toEqual([
      {
        type: 'SXPT_PREVIEW_STEP',
        selector: '[data-training-id="approve"]',
        selectorCandidates: [
          '[data-training-id="approve"]',
          '#approve-button'
        ],
        url: 'https://oa.example.test/approval'
      },
      {
        type: 'RESOLVE_RECORDED_TARGET',
        requestId: 'preview-12',
        locator: {
          selector: '[data-training-id="approve"]',
          selectorCandidates: [
            '[data-training-id="approve"]',
            '#approve-button'
          ],
          url: 'https://oa.example.test/approval'
        }
      }
    ]);
  });

  it('clears both supported highlight protocols when the hint is closed', () => {
    expect(createBusinessStepPreviewClearMessages()).toEqual([
      { type: 'SXPT_CLEAR_PREVIEW_STEP' },
      { type: 'CLEAR_RECORDED_TARGET_PREVIEW' }
    ]);
  });
});
