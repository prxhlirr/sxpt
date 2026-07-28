import type { BusinessActionPayload } from '../types/domain';

export interface LastBusinessActionSignature {
  signature: string;
  at: number;
}

export function createBusinessActionSignature(payload: BusinessActionPayload): string {
  return [
    payload.actionType,
    payload.url,
    payload.selector,
    payload.value ?? ''
  ].join('|');
}

export function shouldSkipDuplicateBusinessAction(
  payload: BusinessActionPayload,
  lastAction: LastBusinessActionSignature | undefined,
  now: number,
  duplicateWindowMs = 350
): boolean {
  if (!lastAction) {
    return false;
  }

  return (
    createBusinessActionSignature(payload) === lastAction.signature &&
    now - lastAction.at < duplicateWindowMs
  );
}
