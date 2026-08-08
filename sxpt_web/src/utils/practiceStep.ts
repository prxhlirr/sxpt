import type { RecordedStep } from '../domain/models';

export type PracticeRecordedActionType = 'click' | 'input' | 'select' | 'submit';

function isGuideStep(step: RecordedStep): boolean {
  return step.kind === 'guide' || step.actionType === 'guide';
}

export function isPracticeMonitorableStep(step: RecordedStep): boolean {
  if (!isGuideStep(step)) return true;
  return Boolean(
    step.selector.trim() ||
      step.selectorCandidates?.some((selector) => selector.trim())
  );
}

export function practiceRecordedActionType(
  step: RecordedStep
): PracticeRecordedActionType {
  if (
    step.actionType === 'input' ||
    step.actionType === 'select' ||
    step.actionType === 'submit'
  ) {
    return step.actionType;
  }
  return 'click';
}
