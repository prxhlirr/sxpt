import { describe, expect, it } from 'vitest';
import { summarizeLessonRelease } from './publishCenterPresentation';

describe('summarizeLessonRelease', () => {
  it('summarizes the complete lesson and uses the widest task assignment count', () => {
    expect(
      summarizeLessonRelease({
        stages: [
          { recordedSteps: [{ id: 'one' }, { id: 'two' }] },
          { recordedSteps: [{ id: 'three' }] }
        ],
        lectureCompleted: true,
        learningTask: { assignedCount: 11 },
        practiceTask: { assignedCount: 12, dataCount: 8 }
      })
    ).toEqual({
      stageCount: 2,
      recordedStepCount: 3,
      lectureCompleted: true,
      assignedCount: 12,
      preparedDataCount: 8
    });
  });

  it('returns zero metrics before student tasks exist', () => {
    expect(
      summarizeLessonRelease({
        stages: [],
        lectureCompleted: false
      })
    ).toEqual({
      stageCount: 0,
      recordedStepCount: 0,
      lectureCompleted: false,
      assignedCount: 0,
      preparedDataCount: 0
    });
  });
});
