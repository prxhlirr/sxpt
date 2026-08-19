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

/**
 * 练习完成度只消费必做操作点。历史教案未保存 required 时按必做兼容，
 * 显式标记为非必做的讲解/辅助节点可以被记录，但不会阻塞教学点完成。
 */
export function isRequiredPracticeStep(step: RecordedStep): boolean {
  return isPracticeMonitorableStep(step) && step.required !== false;
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
