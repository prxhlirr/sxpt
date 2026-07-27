import { createMockTrainingState } from '../data/mockSeed';
import type { StorageLike, TrainingState } from '../domain/models';

export const TRAINING_STORAGE_KEY = 'sxpt_web.training.demo.v1';

export interface TrainingApi {
  loadState(): TrainingState;
  saveState(state: TrainingState): TrainingState;
  resetDemo(): TrainingState;
}

export function createTrainingApi(
  storage: StorageLike = resolveBrowserStorage(),
  seedFactory: () => TrainingState = createMockTrainingState
): TrainingApi {
  const persist = (state: TrainingState) => {
    const snapshot = clone(state);
    try {
      storage.setItem(TRAINING_STORAGE_KEY, JSON.stringify(snapshot));
    } catch {
      // Storage can be unavailable in privacy mode or embedded browsers.
    }
    return clone(snapshot);
  };

  const recover = () => persist(seedFactory());

  return {
    loadState() {
      let raw: string | null = null;
      try {
        raw = storage.getItem(TRAINING_STORAGE_KEY);
      } catch {
        return clone(seedFactory());
      }
      if (!raw) {
        return recover();
      }

      try {
        const parsed: unknown = JSON.parse(raw);
        if (!isTrainingState(parsed)) {
          return recover();
        }
        return normalizeState(parsed);
      } catch {
        return recover();
      }
    },

    saveState(state) {
      return persist(state);
    },

    resetDemo() {
      try {
        storage.removeItem(TRAINING_STORAGE_KEY);
      } catch {
        // The in-memory reactive state can still be reset.
      }
      return recover();
    }
  };
}

export function createMemoryStorage(
  initialEntries: Record<string, string> = {}
): StorageLike {
  const values = new Map(Object.entries(initialEntries));
  return {
    getItem(key) {
      return values.get(key) ?? null;
    },
    setItem(key, value) {
      values.set(key, value);
    },
    removeItem(key) {
      values.delete(key);
    }
  };
}

function resolveBrowserStorage(): StorageLike {
  try {
    if (typeof window !== 'undefined' && window.localStorage) {
      return window.localStorage;
    }
  } catch {
    return fallbackMemoryStorage;
  }
  return fallbackMemoryStorage;
}

const fallbackMemoryStorage = createMemoryStorage();

function isTrainingState(value: unknown): value is TrainingState {
  if (!isRecord(value)) return false;
  return (
    (value.currentRole === 'admin' ||
      value.currentRole === 'teacher' ||
      value.currentRole === 'student') &&
    Array.isArray(value.lessons) &&
    value.lessons.every(isLessonRecord) &&
    isRecord(value.examSettings) &&
    isRecord(value.groupPlans) &&
    isRecord(value.unitDataPlans) &&
    isRecord(value.dataItems) &&
    Array.isArray(value.publishedTasks) &&
    value.publishedTasks.every(isPublishedTaskRecord) &&
    Array.isArray(value.studentTasks) &&
    value.studentTasks.every(isStudentTaskRecord) &&
    Array.isArray(value.activities) &&
    value.activities.every(
      (event) =>
        isRecord(event) &&
        typeof event.id === 'string' &&
        typeof event.title === 'string'
    ) &&
    Object.values(value.dataItems).every(
      (items) =>
        Array.isArray(items) &&
        items.every(
          (item) =>
            isRecord(item) &&
            typeof item.id === 'string' &&
            typeof item.lessonId === 'string' &&
            Array.isArray(item.audit)
        )
    )
  );
}

function isLessonRecord(value: unknown) {
  return (
    isRecord(value) &&
    typeof value.id === 'string' &&
    typeof value.title === 'string' &&
    typeof value.status === 'string' &&
    Array.isArray(value.stages) &&
    value.stages.every(
      (stage) =>
        isRecord(stage) &&
        typeof stage.id === 'string' &&
        typeof stage.name === 'string' &&
        Array.isArray(stage.recordedSteps)
    )
  );
}

function isPublishedTaskRecord(value: unknown) {
  return (
    isRecord(value) &&
    typeof value.id === 'string' &&
    typeof value.lessonId === 'string' &&
    typeof value.startAt === 'string' &&
    typeof value.endAt === 'string'
  );
}

function isStudentTaskRecord(value: unknown) {
  return (
    isRecord(value) &&
    typeof value.id === 'string' &&
    typeof value.lessonId === 'string' &&
    typeof value.studentId === 'string' &&
    Array.isArray(value.completedStageIds)
  );
}

/**
 * Keeps browser data created by an earlier demo revision usable after adding
 * multi-role student assignments.
 */
function normalizeState(state: TrainingState): TrainingState {
  const normalized = clone(state);
  normalized.studentTasks = normalized.studentTasks.map((task) => ({
    ...task,
    groupKeys:
      Array.isArray(task.groupKeys) && task.groupKeys.length > 0
        ? [...new Set(task.groupKeys)]
        : [task.groupKey],
    unitId: task.unitId ?? '',
    unitName: task.unitName ?? '',
    dataItemId: task.dataItemId ?? '',
    attemptNumber:
      Number.isInteger(task.attemptNumber) && task.attemptNumber > 0
        ? task.attemptNumber
        : 1,
    submissionValues:
      task.submissionValues && typeof task.submissionValues === 'object'
        ? task.submissionValues
        : {}
  }));
  Object.values(normalized.dataItems).forEach((items) => {
    items.forEach((item) => {
      item.assignedStudentTaskIds ??= [];
    });
  });
  return normalized;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}
