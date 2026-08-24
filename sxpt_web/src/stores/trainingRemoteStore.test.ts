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
      prepareInitialDataForPublishedTask: vi.fn(async () => 0),
      startStudentTaskExecution: vi.fn(),
      reportStudentStageCompletion: vi.fn(),
      reportStudentPracticeStep: vi.fn(),
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

  it('同步平台 ID 后保持教案关联，同步节点时自动补建采集会话', async () => {
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
      prepareInitialDataForPublishedTask: vi.fn(async () => 0),
      startStudentTaskExecution: vi.fn(),
      reportStudentStageCompletion: vi.fn(),
      reportStudentPracticeStep: vi.fn(),
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

    const stage = lesson.stages.find(
      (candidate) => candidate.recordedSteps.length > 0
    )!;
    const step = stage.recordedSteps[0];
    await store.syncRecordedStep(lesson.id, stage.id, step.id);

    expect(backend.startCaptureSession).toHaveBeenCalledOnce();
    expect(lesson.captureSessionId).toBe('capture-server-1');
    expect(reportRecordedStep).toHaveBeenCalledOnce();
    expect(step).toMatchObject({
      syncStatus: 'SYNCED',
      remoteEventId: 'event-server-1',
      remoteResourceSnapshotId: 'snapshot-server-1',
      remoteDraftId: 'draft-server-1'
    });
  });

  it('完成教师讲解、发布学习练习并提交学生学习任务的后端闭环', async () => {
    const publishLesson = vi.fn(async (lesson: LessonPlan) => ({
      teachingPointId: 'teaching-point-1',
      finishedCaptureSession: true,
      resourceIdsByStepId: Object.fromEntries(
        lesson.stages.flatMap((stage) =>
          stage.recordedSteps.map((step) => [
            step.id,
            `resource-${step.id}`
          ])
        )
      )
    }));
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
    const reportStudentPracticeStep = vi.fn(async () => ({
      clientTraceId: 'practice-trace-client-1',
      traceId: 'practice-trace-server-1'
    }));
    const prepareInitialDataForPublishedTask = vi.fn(async () => 1);
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
      publishLesson,
      publishTeachingTask,
      prepareInitialDataForPublishedTask,
      startStudentTaskExecution,
      reportStudentStageCompletion,
      reportStudentPracticeStep,
      submitStudentTaskExecution
    };
    let generatedId = 0;
    const store = createTrainingStore({
      backend,
      storage: createMemoryStorage(),
      now: () => '2026-07-28T12:00:00.000Z',
      idFactory: (prefix) => `${prefix}-test-${++generatedId}`
    });
    const lesson = store.state.lessons.find(
      (candidate) => candidate.id === 'lesson-purchase-v3'
    )!;
    lesson.stages.forEach((stage) =>
      stage.recordedSteps.forEach((step) => {
        step.remoteDraftId = `draft-${step.id}`;
        step.syncStatus = 'SYNCED';
      })
    );

    store.markLessonLectureCompleted(lesson.id);
    const published = await store.publishLearningAndPracticeRemote(lesson.id);

    expect(publishLesson).toHaveBeenCalledOnce();
    expect(lesson.teachingPointId).toBe('teaching-point-1');
    expect(lesson.captureSessionFinished).toBe(true);
    expect(published.map((task) => task.mode)).toEqual([
      'LEARNING',
      'PRACTICE'
    ]);
    expect(published.every((task) => task.syncStatus === 'SYNCED')).toBe(true);
    expect(publishTeachingTask).toHaveBeenCalledTimes(2);
    expect(prepareInitialDataForPublishedTask).toHaveBeenCalledTimes(1);
    expect(prepareInitialDataForPublishedTask).toHaveBeenCalledWith(
      expect.objectContaining({ id: lesson.id }),
      expect.any(Object),
      expect.objectContaining({
        mode: 'PRACTICE',
        remoteTaskId: 'task-practice'
      }),
      expect.arrayContaining([
        expect.objectContaining({ mode: 'PRACTICE' })
      ])
    );
    expect(
      published.find((candidate) => candidate.mode === 'PRACTICE')?.dataCount
    ).toBe(1);

    const practicePublished = published.find(
      (candidate) => candidate.mode === 'PRACTICE'
    )!;
    const practiceTask = store.state.studentTasks.find(
      (candidate) => candidate.publishedTaskId === practicePublished.id
    )!;
    const practiceStage = lesson.stages[0];
    const practiceStep = practiceStage.recordedSteps[0];
    await store.startStudentTaskRemote(practiceTask.id);
    await store.recordStudentPracticeStepRemote(
      practiceTask.id,
      practiceStage.id,
      practiceStep.id,
      {
        actionType: practiceStep.actionType === 'input' ? 'input' : 'click',
        selector: practiceStep.selector,
        selectorCandidates: practiceStep.selectorCandidates,
        url: practiceStep.url,
        pageTitle: practiceStep.pageTitle,
        completedAt: '2026-07-28T12:00:00.000Z'
      }
    );
    expect(reportStudentPracticeStep).toHaveBeenCalledOnce();
    expect(practiceTask.completedPracticeStepIds).toEqual([practiceStep.id]);
    expect(practiceTask.practiceStepResults).toEqual([
      expect.objectContaining({
        stepId: practiceStep.id,
        remoteTraceId: 'practice-trace-server-1'
      })
    ]);

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

    expect(startStudentTaskExecution).toHaveBeenCalledTimes(2);
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

  it('发布本地已录节点时自动补建采集会话，不再要求返回点击开始录制', async () => {
    const seed = createMockTrainingState();
    const lesson = seed.lessons[0];
    lesson.status = 'RECORDED';
    delete lesson.captureSessionId;
    delete lesson.teachingPointId;
    lesson.stages.forEach((stage) =>
      stage.recordedSteps.forEach((step) => {
        delete step.remoteDraftId;
        delete step.remoteResourceId;
        step.syncStatus = 'LOCAL';
      })
    );
    const startCaptureSession = vi.fn(async () => 'capture-auto-created');
    const reportRecordedStep = vi.fn(async () => ({
      eventId: 'event-auto-created',
      resourceSnapshotId: 'snapshot-auto-created',
      draftId: 'draft-auto-created'
    }));
    const publishLesson = vi.fn(async () => ({
      teachingPointId: 'point-auto-created',
      finishedCaptureSession: true,
      resourceIdsByStepId: {}
    }));
    const backend: BackendTrainingApi = {
      isEnabled: () => true,
      listBusinessPlatforms: vi.fn(async (platforms) => platforms),
      createBusinessPlatform: vi.fn(),
      updateBusinessPlatform: vi.fn(),
      setBusinessPlatformStatus: vi.fn(),
      startCaptureSession,
      reportRecordedStep,
      publishLesson,
      publishTeachingTask: vi.fn(),
      prepareInitialDataForPublishedTask: vi.fn(async () => 0),
      startStudentTaskExecution: vi.fn(),
      reportStudentStageCompletion: vi.fn(),
      reportStudentPracticeStep: vi.fn(),
      submitStudentTaskExecution: vi.fn()
    };
    const store = createTrainingStore({
      backend,
      storage: createMemoryStorage({
        [TRAINING_STORAGE_KEY]: JSON.stringify(seed)
      })
    });

    const published = await store.publishLessonRemote(lesson.id);

    expect(startCaptureSession).toHaveBeenCalledOnce();
    expect(reportRecordedStep).toHaveBeenCalledTimes(
      lesson.stages.reduce(
        (total, stage) => total + stage.recordedSteps.length,
        0
      )
    );
    expect(publishLesson).toHaveBeenCalledOnce();
    expect(published).toMatchObject({
      status: 'PUBLISHED',
      captureSessionId: 'capture-auto-created',
      teachingPointId: 'point-auto-created'
    });
  });

  it('撤回发布时同步撤回后端教学点并恢复可编辑状态', async () => {
    const seed = createMockTrainingState();
    const lesson = seed.lessons[0];
    lesson.status = 'PUBLISHED';
    lesson.teachingPointId = 'point-published';
    seed.publishedTasks = seed.publishedTasks.filter(
      (task) => task.lessonId !== lesson.id
    );
    seed.studentTasks = seed.studentTasks.filter(
      (task) => task.lessonId !== lesson.id
    );
    const withdrawLesson = vi.fn(async () => undefined);
    const backend: BackendTrainingApi = {
      isEnabled: () => true,
      listBusinessPlatforms: vi.fn(),
      createBusinessPlatform: vi.fn(),
      updateBusinessPlatform: vi.fn(),
      setBusinessPlatformStatus: vi.fn(),
      startCaptureSession: vi.fn(),
      reportRecordedStep: vi.fn(),
      publishLesson: vi.fn(),
      withdrawLesson,
      publishTeachingTask: vi.fn(),
      prepareInitialDataForPublishedTask: vi.fn(async () => 0),
      startStudentTaskExecution: vi.fn(),
      reportStudentStageCompletion: vi.fn(),
      reportStudentPracticeStep: vi.fn(),
      submitStudentTaskExecution: vi.fn()
    };
    const store = createTrainingStore({
      backend,
      storage: createMemoryStorage({
        [TRAINING_STORAGE_KEY]: JSON.stringify(seed)
      })
    });

    const withdrawn = await store.withdrawLessonRemote(lesson.id);

    expect(withdrawLesson).toHaveBeenCalledWith(
      expect.objectContaining({ teachingPointId: 'point-published' })
    );
    expect(withdrawn.status).toBe('RECORDED');
    expect(withdrawn.teachingPointId).toBeUndefined();
    expect(withdrawn.publishedAt).toBeUndefined();
  });

  it('未配置分组时按真实学生目录生成学习和练习任务', async () => {
    const seed = createMockTrainingState();
    const lesson = seed.lessons.find(
      (candidate) => candidate.id === 'lesson-purchase-v3'
    )!;
    seed.groupPlans = {};
    lesson.lectureCompletedAt = '2026-08-07T08:00:00.000Z';
    lesson.teachingPointId = 'teaching-point-existing';
    lesson.status = 'PUBLISHED';
    const backend: BackendTrainingApi = {
      isEnabled: () => true,
      listBusinessPlatforms: vi.fn(),
      createBusinessPlatform: vi.fn(),
      updateBusinessPlatform: vi.fn(),
      setBusinessPlatformStatus: vi.fn(),
      startCaptureSession: vi.fn(),
      reportRecordedStep: vi.fn(),
      publishLesson: vi.fn(),
      publishTeachingTask: vi.fn(async (_lesson, _platform, mode) => ({
        courseId: 'course-existing',
        taskId: `task-${String(mode).toLowerCase()}`,
        teachingPointId: 'teaching-point-existing',
        evaluationRuleId: `rule-${String(mode).toLowerCase()}`,
        taskStepIdsByStepId: {}
      })),
      prepareInitialDataForPublishedTask: vi.fn(async () => 2),
      startStudentTaskExecution: vi.fn(),
      reportStudentStageCompletion: vi.fn(),
      reportStudentPracticeStep: vi.fn(),
      submitStudentTaskExecution: vi.fn()
    };
    const listStudents = vi.fn(async () => [
      {
        studentId: 'student-real-1',
        studentName: '学生一',
        username: 'student01',
        unitId: 'class-a',
        unitName: '一班'
      },
      {
        studentId: 'student-real-2',
        studentName: '学生二',
        username: 'student02',
        unitId: 'class-a',
        unitName: '一班'
      }
    ]);
    const store = createTrainingStore({
      backend,
      listStudents,
      storage: createMemoryStorage({
        [TRAINING_STORAGE_KEY]: JSON.stringify(seed)
      }),
      now: () => '2026-08-07T08:00:00.000Z'
    });

    const published = await store.publishLearningAndPracticeRemote(lesson.id);

    expect(listStudents).toHaveBeenCalledOnce();
    expect(published.map((task) => task.assignedCount)).toEqual([2, 2]);
    const publishedIds = new Set(published.map((task) => task.id));
    const generatedTasks = store.state.studentTasks.filter((task) =>
      publishedIds.has(task.publishedTaskId)
    );
    expect(generatedTasks).toHaveLength(4);
    expect(new Set(generatedTasks.map((task) => task.studentId))).toEqual(
      new Set(['student-real-1', 'student-real-2'])
    );
  });

  it('教师主观评分写入后端并生成学生端可读取的成绩反馈', async () => {
    const seed = createMockTrainingState();
    const task = seed.studentTasks.find((candidate) => candidate.status === 'SUBMITTED')!;
    const published = seed.publishedTasks.find(
      (candidate) => candidate.id === task.publishedTaskId
    )!;
    task.remoteExecutionId = 'execution-reviewed-1';
    published.remoteEvaluationRuleId = 'rule-reviewed-1';
    const reviewStudentTask = vi.fn(async () => ({
      id: 'result-reviewed-1',
      tenantId: 'default',
      executionId: task.remoteExecutionId!,
      evaluationRuleId: published.remoteEvaluationRuleId!,
      autoScore: task.objectiveScore ?? 0,
      manualScore: 12,
      finalScore: (task.objectiveScore ?? 0) + 12,
      evaluationStatus: 'REVIEWED',
      reviewStatus: 'ADJUSTED',
      reviewReason: '流程完整，关键操作准确。',
      status: 'ACTIVE',
      createTime: '2026-08-13T09:30:00.000Z'
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
      publishTeachingTask: vi.fn(),
      prepareInitialDataForPublishedTask: vi.fn(async () => 0),
      startStudentTaskExecution: vi.fn(),
      reportStudentStageCompletion: vi.fn(),
      reportStudentPracticeStep: vi.fn(),
      submitStudentTaskExecution: vi.fn(),
      reviewStudentTask
    };
    const store = createTrainingStore({
      backend,
      storage: createMemoryStorage({
        [TRAINING_STORAGE_KEY]: JSON.stringify(seed)
      }),
      now: () => '2026-08-13T09:30:00.000Z'
    });

    const graded = await store.gradeStudentTaskRemote(
      task.id,
      12,
      '流程完整，关键操作准确。'
    );

    expect(reviewStudentTask).toHaveBeenCalledWith(
      expect.objectContaining({ remoteEvaluationRuleId: 'rule-reviewed-1' }),
      expect.objectContaining({ remoteExecutionId: 'execution-reviewed-1' }),
      12,
      '流程完整，关键操作准确。'
    );
    expect(graded).toMatchObject({
      status: 'GRADED',
      subjectiveScore: 12,
      comment: '流程完整，关键操作准确。',
      gradedAt: '2026-08-13T09:30:00.000Z'
    });
  });
});
