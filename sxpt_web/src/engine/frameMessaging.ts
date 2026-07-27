export function resolveFrameTargetOrigin(
  frameSrc: string,
  appOrigin: string
): string {
  try {
    return new URL(frameSrc, appOrigin).origin;
  } catch {
    return '*';
  }
}

export function resolveCurrentFrameTargetOrigin(
  boundSrc: string,
  currentUrl: string,
  frameReady: boolean,
  appOrigin: string
): string {
  const boundOrigin = resolveFrameTargetOrigin(boundSrc, appOrigin);
  return resolveFrameTargetOrigin(
    frameReady ? currentUrl : boundSrc,
    boundOrigin
  );
}

export function isMessageFromBusinessFrame(
  eventSource: MessageEventSource | null,
  frameWindow: Window | null | undefined,
  eventOrigin?: string,
  expectedOrigin?: string
): boolean {
  if (!frameWindow || eventSource !== frameWindow) return false;
  if (!eventOrigin || !expectedOrigin || expectedOrigin === '*') return true;
  return eventOrigin === expectedOrigin;
}
