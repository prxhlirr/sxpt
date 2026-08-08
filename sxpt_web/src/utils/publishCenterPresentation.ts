interface ReleaseStage {
  recordedSteps: unknown[];
}

interface ReleaseTaskSummary {
  assignedCount?: number;
  dataCount?: number;
}

export interface LessonReleaseSummary {
  stageCount: number;
  recordedStepCount: number;
  lectureCompleted: boolean;
  assignedCount: number;
  preparedDataCount: number;
}

export function summarizeLessonRelease(input: {
  stages: ReleaseStage[];
  lectureCompleted: boolean;
  learningTask?: ReleaseTaskSummary;
  practiceTask?: ReleaseTaskSummary;
}): LessonReleaseSummary {
  return {
    stageCount: input.stages.length,
    recordedStepCount: input.stages.reduce(
      (total, stage) => total + stage.recordedSteps.length,
      0
    ),
    lectureCompleted: input.lectureCompleted,
    assignedCount: Math.max(
      input.learningTask?.assignedCount ?? 0,
      input.practiceTask?.assignedCount ?? 0
    ),
    preparedDataCount: input.practiceTask?.dataCount ?? 0
  };
}
