import { describe, expect, it } from 'vitest';
import type { TrainingAttachment } from '../domain/models';
import {
  attachmentStackOffset,
  buildAttachmentDockItems,
  listStageStepEntries,
  resolveAttachmentDockPosition
} from './playbackPresentation';

const attachment = (id: string): TrainingAttachment => ({
  id,
  name: `${id}.pdf`,
  mimeType: 'application/pdf',
  size: 1024,
  dataUrl: `data:application/pdf;base64,${id}`,
  uploadedAt: '2026-08-07T00:00:00.000Z'
});

describe('listStageStepEntries', () => {
  it('returns only the selected teaching point while preserving global indexes', () => {
    const steps = [
      { stage: { id: 'stage-a' }, step: { id: 'a-1' } },
      { stage: { id: 'stage-b' }, step: { id: 'b-1' } },
      { stage: { id: 'stage-b' }, step: { id: 'b-2' } }
    ];

    expect(listStageStepEntries(steps, 'stage-b')).toEqual([
      { item: steps[1], globalIndex: 1 },
      { item: steps[2], globalIndex: 2 }
    ]);
  });

  it('returns no nodes when no teaching point is active', () => {
    expect(listStageStepEntries([], undefined)).toEqual([]);
  });
});

describe('buildAttachmentDockItems', () => {
  it('shows every visible teaching-point attachment by default', () => {
    const stageAttachments = [attachment('stage-a'), attachment('stage-b')];

    expect(
      buildAttachmentDockItems({
        stageAttachments,
        activeAttachment: null,
        minimized: false,
        hiddenIds: new Set()
      })
    ).toEqual(stageAttachments);
  });

  it('adds a minimized node attachment once and filters hidden cards', () => {
    const stageAttachment = attachment('stage-a');
    const nodeAttachment = attachment('node-a');

    expect(
      buildAttachmentDockItems({
        stageAttachments: [stageAttachment],
        activeAttachment: nodeAttachment,
        minimized: true,
        hiddenIds: new Set(['stage-a'])
      })
    ).toEqual([nodeAttachment]);

    expect(
      buildAttachmentDockItems({
        stageAttachments: [stageAttachment],
        activeAttachment: stageAttachment,
        minimized: true,
        hiddenIds: new Set()
      })
    ).toEqual([stageAttachment]);
  });
});

describe('attachment dock geometry', () => {
  it('creates a six-pixel cascade with the first attachment on top', () => {
    expect(attachmentStackOffset(2, 4)).toEqual({
      x: 12,
      y: 12,
      zIndex: 2
    });
  });

  it('defaults to the right and moves left when the directory needs avoidance', () => {
    const input = {
      viewport: { width: 1200, height: 800 },
      dock: { width: 280, height: 120 }
    };

    expect(
      resolveAttachmentDockPosition({ ...input, avoidRight: false })
    ).toEqual({ x: 908, y: 340 });
    expect(
      resolveAttachmentDockPosition({ ...input, avoidRight: true })
    ).toEqual({ x: 12, y: 340 });
  });

  it('clamps a manual position after the viewport changes', () => {
    expect(
      resolveAttachmentDockPosition({
        viewport: { width: 600, height: 400 },
        dock: { width: 280, height: 120 },
        avoidRight: false,
        manual: { x: 900, y: -10 }
      })
    ).toEqual({ x: 308, y: 12 });
  });
});
