import type { RecordedStep, Rect } from '../types/domain';

export function withRuntimeRect(
  step: RecordedStep,
  rect: Rect
): RecordedStep {
  return {
    ...step,
    rect: { ...rect },
    ...(step.kind === 'guide' && step.anchor?.mode === 'element'
      ? { anchor: { ...step.anchor, rect: { ...rect } } }
      : {})
  };
}
