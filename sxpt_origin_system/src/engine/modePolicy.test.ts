import { describe, expect, it } from 'vitest';
import { getModePolicy } from './modePolicy';

describe('modePolicy', () => {
  it('returns a learning policy that shows the full teaching guidance', () => {
    const policy = getModePolicy('learning');

    expect(policy).toMatchObject({
      showFullFlow: true,
      showTeachingText: true,
      showOverlayHint: true,
      allowFreeBrowse: true,
      enforceOrder: false,
      showSubmission: false,
      silentRecord: false
    });
  });

  it('returns a practice policy that guides the current executable step only', () => {
    const policy = getModePolicy('practice');

    expect(policy).toMatchObject({
      showFullFlow: true,
      showTeachingText: false,
      showOverlayHint: true,
      allowFreeBrowse: false,
      enforceOrder: true,
      showSubmission: false,
      silentRecord: false
    });
  });

  it('returns an exam policy that hides the flow and records silently', () => {
    const policy = getModePolicy('exam');

    expect(policy).toMatchObject({
      showFullFlow: false,
      showTeachingText: false,
      showOverlayHint: false,
      allowFreeBrowse: false,
      enforceOrder: false,
      showSubmission: true,
      silentRecord: true
    });
  });
});
