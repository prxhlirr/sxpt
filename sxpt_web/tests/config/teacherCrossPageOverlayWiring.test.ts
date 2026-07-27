import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

const teacherDesigner = readFileSync(
  new URL('../../src/views/TeacherRecordDesigner.vue', import.meta.url),
  'utf8'
);

describe('teacher cross-page overlay wiring', () => {
  it('invalidates stale overlay state for both iframe navigation lifecycle events', () => {
    expect(teacherDesigner).toContain(
      '@frame-navigating="invalidateBusinessFrame"'
    );
    expect(teacherDesigner).toContain('@frame-load="invalidateBusinessFrame"');

    const invalidation = teacherDesigner.match(
      /function invalidateBusinessFrame\(\) \{([\s\S]*?)\n\}/
    )?.[1];
    expect(invalidation).toContain("latestBusinessUrl.value = '';");
    expect(invalidation).toContain('runtimeSelectedRect.value = undefined;');
    expect(invalidation).toContain('resolutionGeneration += 1;');
  });

  it('gates selected overlays by iframe readiness and current page', () => {
    expect(teacherDesigner).toContain('getVisibleOverlayStep({');
    expect(teacherDesigner).toContain(
      'frameReady: Boolean(latestBusinessUrl.value)'
    );
    expect(teacherDesigner).toContain('currentUrl: latestBusinessUrl.value');
  });

  it('keeps the freshly picked element rect visible without another resolve', () => {
    const handler = teacherDesigner.match(
      /function handleElementPicked\(payload: ElementPickedMessage\) \{([\s\S]*?)\n\}/
    )?.[1];
    expect(handler).toContain('runtimeSelectedRect.value = payload.rect;');
  });

  it('creates guides on the currently loaded page and shares page matching logic', () => {
    expect(teacherDesigner).toContain(
      'workspace.value.selectedStepId,\n    latestBusinessUrl.value'
    );
    expect(teacherDesigner).toContain('isSameBusinessPage(');
    expect(teacherDesigner).not.toContain('function samePage(');
  });

  it('disables and rejects guide insertion while the business page is navigating', () => {
    expect(teacherDesigner).toContain(
      'canInsertGuide:\n    Boolean(latestBusinessUrl.value)'
    );
    expect(teacherDesigner).toContain('if (!latestBusinessUrl.value) {');
  });
});
