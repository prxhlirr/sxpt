import type { RecordedStep } from '../types/domain';

export interface StepTargetLocator {
  selector: string;
  selectorCandidates: string[];
  stableKey?: string;
}

export interface StepPlaybackMessage {
  type: 'PLAY_RECORDED_STEP';
  requestId?: string;
  step: {
    actionType: RecordedStep['actionType'];
    selector: string;
    selectorCandidates: string[];
    stableKey?: string;
    value?: string;
  };
}

export interface TargetResolutionMessage {
  type: 'RESOLVE_RECORDED_TARGET';
  requestId: string;
  locator: StepTargetLocator;
}

export function createStepPlaybackMessage(
  step: RecordedStep,
  requestId?: string
): StepPlaybackMessage {
  const locator = getStepTargetLocator(step);
  return {
    type: 'PLAY_RECORDED_STEP',
    ...(requestId ? { requestId } : {}),
    step: {
      actionType: step.actionType,
      ...locator,
      value: step.value
    }
  };
}

export function createTargetResolutionMessage(
  step: RecordedStep,
  requestId: string
): TargetResolutionMessage {
  return {
    type: 'RESOLVE_RECORDED_TARGET',
    requestId,
    locator: getStepTargetLocator(step)
  };
}

export function getStepTargetLocator(step: RecordedStep): StepTargetLocator {
  const anchor = step.kind === 'guide' && step.anchor?.mode === 'element'
    ? step.anchor
    : undefined;
  const selector = anchor?.selector || step.selector;
  const candidates = anchor?.selectorCandidates ?? step.selectorCandidates ?? [];
  const selectorCandidates = Array.from(
    new Set([selector, ...candidates].filter(Boolean))
  );

  return {
    selector,
    selectorCandidates,
    ...(step.stableKey ? { stableKey: step.stableKey } : {})
  };
}
