import { describe, expect, it, vi } from 'vitest';
import { createMockTrainingState } from '../data/mockSeed';
import type {
  BusinessPlatform,
  LessonPlan,
  RunMode
} from '../domain/models';
import type { BackendTrainingApi } from '../services/backendTrainingApi';
import {
  TRAINING_STORAGE_KEY,
  createMemoryStorage
} from '../services/trainingApi';
import { createTrainingStore } from './trainingStore';

describe('训练 Store 后端同步', () => {
  it('录制前恢复已被远程列表覆盖的内置平台关联', async () => {
    const seed = createMockTrainingState();
    seed.businessPlatforms = [
      {
        id: 'demo-connector',
        code: 'DEMO',
        name: 'Local Demo Business System',
        baseUrl: 'http://localhost:5173/biz-local',
        description: '',
        status: 'ENABLED',
        modules: [],
        updatedAt: '2026-07-28T12:00:00'
      }
    ];
    const storage = createMemoryStorage({
      [TRAINING_STORAGE_KEY]: JSON.stringify(seed)
    });
    const listBusinessPlatforms = vi.fn(
      async (cachedPlatforms: BusinessPlatform[]) => {
        const purchase = cachedPlatforms.find(
          (platform) => platform.code === 'PURCHASE'
        )!;
        return [
          seed.businessPlatforms[0],
          {
            ...purchase,
            id: 'purchase-server',
            updatedAt: '2026-07-28T12:00:00'
          }
        ];
      }
    );
    const startCaptureSession = vi.fn(async () => 'capture-server');
    const backend: BackendTrainingApi = {
      isEnabled: () => true,
      listBusinessPlatforms,
      createBusinessPlatform: vi.fn(),
      updateBusinessPlatform: vi.fn(),
      setBusinessPlatformStatus: vi.fn(),
      startCaptureSession,
      reportRecordedStep: vi.fn(),
      publishLesson: vi.fn(),
      publishTeachingTask: vi.fn(),
      startStudentTaskExecution: vi.fn(),
      reportStudentStageCompletion: vi.fn(),
      submitStudentTaskExecution: vi.fn()
    };
    const store = createTrainingStore({ backend, storage });
    const lesson = store.getLesson('lesson-purchase-v3')!;

    expect(store.getBusinessPlatform(lesson.businessPlatformId)).toBeUndefined();

    await store.startCaptureSessionRemote(lesson.id, 'internal://purchase/approval');

    expect(listBusinessPlatforms).toHaveBeenCalledWith(
      expect.arrayContaining([
        expect.objectContaining({ code: 'PURCHASE' })
      ])
    );
    expect(lesson.businessPlatformId).toBe('purchase-server');
    expect(startCaptureSession).toHaveBeenCalledWith(
      expect.objectContaining({ id: lesson.id }),
      expect.objectContaining({ id: 'purchase-server', code: 'PURCHASE' }),
      'internal://purchase/approval'
    );
  });

  it('同步平台 ID 后保持教案关联，并完成采集会话与节点上报', async () => {
    const seed = createMockTrainingState();
    const sourceLesson = seed.lessons[0];
    const sourcePlatform = seed.businessPlatforms.find(
      (platform) => platform.id === sourceLesson.businessPlatformId
    )!;
    const reportRecordedStep = vi.fn(async () => ({
      eventId: 'event-server-1',
      resourceSnapshotId: 'snapshot-server-1',
      draftId: 'draft-server-1'
    }));
    const backend: BackendTrainingApi = {
      isEnabled: () => true,
      listBusinessPlatforms: vi.fn(async () => [
        {
          ...sourcePlatform,
          id: 'platform-server-1',
          updatedAt: '2026-07-28T12:00:00'
        }
      ]),
      createBusinessPlatform: vi.fn(),
      updateBusinessPlatform: vi.fn(),
      setBusinessPlatformStatus: vi.fn(),
      startCaptureSession: vi.fn(async () => 'capture-server-1'),
      reportRecordedStep,
      publishLesson: vi.fn(),
      publishTeachingTask: vi.fn(),
      startStudentTaskExecution: vi.fn(),
      reportStudentStageCompletion: vi.fn(),
      submitStudentTaskExecution: vi.fn()
    };
    const store = createTrainingStore({
      backend,
      storage: createMemoryStorage(),
      now: () => '2026-07-28T12:00:00.000Z'
    });

    await store.syncBusinessPlatforms();

    const lesson = store.getLesson(sourceLesson.id)!;
    expect(lesson.businessPlatformId).toBe('platform-server-1');

    await store.startCaptureSessionRemote(
      lesson.id,
      'https://business.example.com/approval'
    );
    expect(lesson.captureSessionId).toBe('capture-server-1');

    const stage = lesson.stages.find(
      (candidate) => candidate.recordedSteps.length > 0
    )!;
    const step = stage.recordedSteps[0];
    await store.syncRecordedStep(lesson.id, stage.id, step.id);

    expect(reportRecordedStep).toHaveBeenCalledOnce();
    expect(step).toMatchObject({
      syncStatus: 'SYNCED',
      remoteEventId: 'event-server-1',
      remoteResourceSnapshotId: 'snapshot-server-1',
      remoteDraftId: 'draft-server-1'
    });
  });

  it('完成教师讲解、发布学习练习并提交学生学习任务的后端闭环', async () => {
    const publishTeachingTask = vi.fn(
      async (
        lesson: LessonPlan,
        _platform: BusinessPlatform,
        mode: RunMode
      ) => ({
        courseId: `course-${lesson.id}`,
        taskId: `task-${mode.toLowerCase()}`,
        teachingPointId: lesson.teachingPointId!,
        evaluationRuleId: `rule-${mode.toLowerCase()}`,
        taskStepIdsByStepId: Object.fromEntries(
          lesson.stages.flatMap((stage) =>
            stage.recordedSteps.map((step) => [
              step.id,
              `task-step-${mode.toLowerCase()}-${step.id}`
            ])
          )
        )
      })
    );
    const startStudentTaskExecution = vi.fn(async () => ({
      executionId: 'execution-learning-1',
      executionStatus: 'RUNNING',
      contextLoaded: true
    }));
    const reportStudentStageCompletion = vi.fn(async () => undefined);
    const submitStudentTaskExecution = vi.fn(async (studentTask) => ({
      id: studentTask.remoteExecutionId!,
      tenantId: 'default',
      taskId: 'task-learning',
      studentId: studentTask.studentId,
      connectorSystemId: 'business-platform-purchase',
      executionMode: 'LEARNING',
      sdkMode: 'LEARNING',
      executionIdentityStatus: 'READY',
      startTime: '2026-07-28T12:00:00',
      endTime: '2026-07-28T12:05:00',
      executionStatus: 'COMPLETED',
      score: 80,
      status: 'ACTIVE',
      createTime: '2026-07-28T12:00:00',
      updateTime: '2026-07-28T12:05:00'
    }));
    const backend: BackendTrainingApi = {
      isEnabled: () => true,
      listBusinessPlatforms: vi.fn(),
      createBusinessPlatform: vi.fn(),
      updateBusinessPlatform: vi.fn(),
      setBusinessPlatformStatus: vi.fn(),
      startCaptureSession: vi.fn(),
      reportRecordedStep: vi.fn(),
      publishLesson: vi.fn(),
      publishTeachingTask,
      startStudentTaskExecution,
      reportStudentStageCompletion,
      submitStudentTaskExecution
    };
    const store = createTrainingStore({
      backend,
      storage: createMemoryStorage(),
      now: () => '2026-07-28T12:00:00.000Z',
      idFactory: (prefix) => `${prefix}-test`
    });
    const lesson = store.state.lessons.find(
      (candidate) => candidate.id === 'lesson-purchase-v3'
    )!;
    lesson.teachingPointId = 'teaching-point-1';

    store.markLessonLectureCompleted(lesson.id);
    const published = await store.publishLearningAndPracticeRemote(lesson.id);

    expect(published.map((task) => task.mode)).toEqual([
      'LEARNING',
      'PRACTICE'
    ]);
    expect(published.every((task) => task.syncStatus === 'SYNCED')).toBe(true);
    expect(publishTeachingTask).toHaveBeenCalledTimes(2);

    const learningPublished = published.find(
      (candidate) => candidate.mode === 'LEARNING'
    )!;
    const studentTask = store.state.studentTasks.find(
      (candidate) => candidate.publishedTaskId === learningPublished.id
    )!;

    await store.startStudentTaskRemote(studentTask.id);
    for (const stage of lesson.stages) {
      await store.completeStudentStageRemote(studentTask.id, stage.id);
    }
    await store.submitStudentTaskRemote(studentTask.id);

    expect(startStudentTaskExecution).toHaveBeenCalledOnce();
    expect(reportStudentStageCompletion).toHaveBeenCalledTimes(
      lesson.stages.length
    );
    expect(submitStudentTaskExecution).toHaveBeenCalledOnce();
    expect(studentTask).toMatchObject({
      status: 'SUBMITTED',
      remoteExecutionId: 'execution-learning-1',
      remoteExecutionStatus: 'COMPLETED',
      remoteContextLoaded: true,
      remoteScore: 80,
      objectiveScore: 80,
      syncStatus: 'SYNCED'
    });
  });
});
