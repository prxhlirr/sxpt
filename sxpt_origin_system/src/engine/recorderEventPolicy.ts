export type RecorderElementKind = 'text-control' | 'select' | 'command' | 'form';
export type RecorderDomEventType = 'click' | 'change' | 'blur' | 'submit';

export function shouldEmitRecorderEvent(
  elementKind: RecorderElementKind,
  eventType: RecorderDomEventType
): boolean {
  if (eventType === 'submit') {
    return true;
  }

  if (elementKind === 'text-control' || elementKind === 'select') {
    return eventType === 'change';
  }

  if (elementKind === 'form') {
    return false;
  }

  return eventType === 'click';
}
