import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

const studentRunner = readFileSync(
  new URL('../../src/views/StudentRunner.vue', import.meta.url),
  'utf8'
);

describe('cross-page overlay wiring', () => {
  it('invalidates and re-prepares student overlays through page lifecycle state', () => {
    expect(studentRunner).toContain(
      '@frame-navigating="invalidateBusinessFrame"'
    );
    expect(studentRunner).toContain('@frame-load="invalidateBusinessFrame"');
    expect(studentRunner).toContain('getVisibleOverlayStep({');
    expect(studentRunner).toContain('getPlaybackPreparation({');
    expect(studentRunner).toContain('shouldReprepareAfterPlayback({');
    expect(studentRunner).toContain(
      'if (!shouldHandlePlaybackFailure(step.id, currentStep.value?.id)) return;'
    );
    expect(studentRunner).toContain('currentStepPlayed.value = true;');
    expect(studentRunner).toContain('if (!currentStepPlayed.value) return;');
  });
});
