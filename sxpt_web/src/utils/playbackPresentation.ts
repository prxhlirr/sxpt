import type { TrainingAttachment } from '../domain/models';
import {
  clampOverlayPosition,
  type OverlayPosition,
  type OverlaySize
} from './draggableOverlay';

export interface StageStepEntry<T> {
  item: T;
  globalIndex: number;
}

export interface AttachmentDockItemsInput {
  stageAttachments: TrainingAttachment[];
  activeAttachment?: TrainingAttachment | null;
  minimized: boolean;
  hiddenIds: ReadonlySet<string>;
}

export function listStageStepEntries<
  T extends { stage: { id: string } }
>(steps: T[], stageId?: string): StageStepEntry<T>[] {
  if (!stageId) return [];
  return steps.flatMap((item, globalIndex) =>
    item.stage.id === stageId ? [{ item, globalIndex }] : []
  );
}

export function buildAttachmentDockItems({
  stageAttachments,
  activeAttachment,
  minimized,
  hiddenIds
}: AttachmentDockItemsInput): TrainingAttachment[] {
  const candidates = [...stageAttachments];
  if (
    minimized &&
    activeAttachment &&
    !candidates.some((attachment) => attachment.id === activeAttachment.id)
  ) {
    candidates.push(activeAttachment);
  }
  return candidates.filter((attachment) => !hiddenIds.has(attachment.id));
}

export function attachmentStackOffset(
  index: number,
  total: number,
  step = 6
) {
  return {
    x: index * step,
    y: index * step,
    zIndex: Math.max(1, total - index)
  };
}

export function resolveAttachmentDockPosition(input: {
  viewport: OverlaySize;
  dock: OverlaySize;
  avoidRight: boolean;
  manual?: OverlayPosition | null;
  margin?: number;
}): OverlayPosition {
  const margin = input.margin ?? 12;
  const desired = input.manual ?? {
    x: input.avoidRight
      ? margin
      : input.viewport.width - input.dock.width - margin,
    y: (input.viewport.height - input.dock.height) / 2
  };
  return clampOverlayPosition(desired, input.dock, input.viewport, margin);
}
