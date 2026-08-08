export interface OverlayPosition {
  x: number;
  y: number;
}

export interface OverlaySize {
  width: number;
  height: number;
}

export interface OverlayDragSession {
  pointerId: number;
  start: OverlayPosition;
  origin: OverlayPosition;
  moved: boolean;
}

export interface OverlayDragUpdate {
  moved: boolean;
  position: OverlayPosition | null;
  session: OverlayDragSession;
}

export function clampOverlayPosition(
  position: OverlayPosition,
  overlaySize: OverlaySize,
  viewportSize: OverlaySize,
  margin = 8
): OverlayPosition {
  const maxX = Math.max(
    margin,
    viewportSize.width - overlaySize.width - margin
  );
  const maxY = Math.max(
    margin,
    viewportSize.height - overlaySize.height - margin
  );
  return {
    x: Math.min(Math.max(position.x, margin), maxX),
    y: Math.min(Math.max(position.y, margin), maxY)
  };
}

export function beginOverlayDrag(
  pointerId: number,
  pointer: OverlayPosition,
  origin: OverlayPosition
): OverlayDragSession {
  return { pointerId, start: pointer, origin, moved: false };
}

export function updateOverlayDrag(
  session: OverlayDragSession,
  pointerId: number,
  pointer: OverlayPosition,
  overlaySize: OverlaySize,
  viewportSize: OverlaySize,
  threshold = 3
): OverlayDragUpdate {
  if (pointerId !== session.pointerId) {
    return { moved: session.moved, position: null, session };
  }

  const deltaX = pointer.x - session.start.x;
  const deltaY = pointer.y - session.start.y;
  const moved = session.moved || Math.hypot(deltaX, deltaY) >= threshold;
  const nextSession =
    moved && !session.moved ? { ...session, moved: true } : session;

  return {
    moved,
    position: moved
      ? clampOverlayPosition(
          {
            x: session.origin.x + deltaX,
            y: session.origin.y + deltaY
          },
          overlaySize,
          viewportSize
        )
      : null,
    session: nextSession
  };
}
