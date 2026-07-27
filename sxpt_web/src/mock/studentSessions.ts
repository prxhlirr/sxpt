import { createInitialSession } from '../engine/flowState';
import type { RunnerMode, StudentSession } from '../types/domain';
import { purchaseLessonFlow } from './lessonFlows';

export function createDemoSession(mode: RunnerMode): StudentSession {
  return createInitialSession(purchaseLessonFlow, mode);
}
