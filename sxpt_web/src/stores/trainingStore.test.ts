import { describe, expect, it } from 'vitest';
import { createMockTrainingState } from '../data/mockSeed';
import type {
  ExamSettings,
  GroupPlan,
  LessonPlan,
  LessonStage,
  StorageLike,
  TrainingState,
  UnitDataPlan
} from '../domain/models';
import type { BackendTrainingApi } from '../services/backendTrainingApi';
import {
  TRAINING_STORAGE_KEY,
  createMemoryStorage,
  createTrainingApi,
  type AuthSession
} from '../services/trainingApi';
import { TrainingValidationError, createTrainingStore } from './trainingStore';

function deterministicStore(storage: StorageLike = createMemoryStorage()) {
  let sequence = 0;
  return createTrainingStore({
    storage,
    now: () => '2026-07-25T08:00:00.000Z',
    idFactory: (prefix) => `${prefix}-${++sequence}`
  });
}

function editableLesson(store: ReturnType<typeof deterministicStore>): LessonPlan {
  return store.createLesson({
    code: 'LESSON-TDD',
    title: '测试教案',
    moduleName: '测试模块',
    description: '通过测试创建',
    objectiveMaxScore: 80,
    subjectiveMaxScore: 20
  });
}

function recordedStage(
  groupKey: string,
  score: number,
  name = '办理阶段'
): Partial<LessonStage> {
  return {
    name,
    groupKey,
    score,
    recordedSteps: [
      {
        id: `record-${groupKey}`,
        title: `${name}操作`,
        pageTitle: '业务系统',
        actionLabel: '提交',
        selector: '[data-action="submit"]',
        durationSeconds: 12,
        note: '完成流程操作'
      }
    ]
  };
}

function validExamSettings(lessonId: string): ExamSettings {
  return {
    lessonId,
    batchName: '2026 年采购业务考试',
    mode: 'EXAM',
    startAt: '2026-07-25T07:00:00.000Z',
    endAt: '2026-07-25T09:00:00.000Z',
    durationMinutes: 90,
    allowRetry: true,
    maxAttempts: 2,
    objectiveWeight: 80,
    subjectiveWeight: 20,
    showProgress: true,
    randomizeData: false,
    submissionFields: [{ key: 'businessNo', label: '业务单号', required: true }]
  };
}

function configurePublishableExam(
  store: ReturnType<typeof deterministicStore>,
  options: { sameStudentMultiGroup?: boolean; formalCount?: number } = {}
) {
  const lesson = editableLesson(store);
  const first = store.addStage(lesson.id, recordedStage('handler', 40, '经办'));
  const second = store.addStage(lesson.id, recordedStage('reviewer', 40, '审核'));
  store.publishLesson(lesson.id);
  store.saveExamSettings(lesson.id, validExamSettings(lesson.id));

  const members = [
    {
      id: 'member-handler-1',
      studentId: 'student-1',
      studentName: '张敏',
      unitId: 'unit-1',
      unitName: '第一事业部',
      groupKey: 'handler',
      accountId: 'zhangmin'
    },
    {
      id: 'member-reviewer-1',
      studentId: options.sameStudentMultiGroup ? 'student-1' : 'student-2',
      studentName: options.sameStudentMultiGroup ? '张敏' : '李晨',
      unitId: 'unit-1',
      unitName: '第一事业部',
      groupKey: 'reviewer',
      accountId: options.sameStudentMultiGroup ? 'zhangmin' : 'lichen'
    }
  ];

  const plan: GroupPlan = {
    lessonId: lesson.id,
    roles: [
      { key: 'handler', name: '经办组', color: '#2563eb', stageIds: [first.id] },
      { key: 'reviewer', name: '审核组', color: '#16a34a', stageIds: [second.id] }
    ],
    members
  };
  store.saveGroupPlan(lesson.id, plan);
  store.saveUnitDataPlans(lesson.id, [
    {
      unitId: 'unit-1',
      unitName: '第一事业部',
      formalCount: options.formalCount ?? 1,
      spareCount: 1,
      scenario: '采购业务标准场景',
      simulateFailure: false
    }
  ]);

  return { lesson, first, second };
}

describe('trainingApi repository', () => {
  it('falls back to a fresh demo when LocalStorage JSON is corrupted', () => {
    const storage = createMemoryStorage();
    storage.setItem(TRAINING_STORAGE_KEY, '{broken-json');

    const api = createTrainingApi(storage, createMockTrainingState);
    const recovered = api.loadState();

    expect(recovered.lessons.length).toBeGreaterThan(1);
    expect(() => JSON.parse(storage.getItem(TRAINING_STORAGE_KEY) ?? '')).not.toThrow();
  });

  it('recovers when persisted JSON is valid but its domain structure is broken', () => {
    const storage = createMemoryStorage();
    const broken = createMockTrainingState() as unknown as Record<string, unknown>;
    broken.lessons = [null];
    storage.setItem(TRAINING_STORAGE_KEY, JSON.stringify(broken));

    const recovered = createTrainingApi(storage, createMockTrainingState).loadState();

    expect(recovered.lessons.length).toBeGreaterThan(1);
    expect(recovered.lessons.every((lesson) => lesson?.id && lesson?.title)).toBe(
      true
    );
  });

  it('persists a detached state snapshot and can reset to the seed', () => {
    const storage = createMemoryStorage();
    const api = createTrainingApi(storage, createMockTrainingState);
    const changed = api.loadState();
    changed.lessons[0].title = '已修改但未保存';
    expect(api.loadState().lessons[0].title).not.toBe('已修改但未保存');

    api.saveState(changed);
    expect(api.loadState().lessons[0].title).toBe('已修改但未保存');

    const reset = api.resetDemo();
    expect(reset.lessons[0].title).toBe(createMockTrainingState().lessons[0].title);
  });
});

describe('lesson authoring', () => {
  it('restores persisted lesson data after the same user logs in again', async () => {
    const session: AuthSession = {
      token: 'teacher-token',
      tokenType: 'Bearer',
      expiresIn: 7200,
      issuedAt: Date.now(),
      user: {
        userId: 'teacher-1',
        tenantId: 'tenant-1',
        username: 'teacher01',
        displayName: '张老师',
        userType: 'TEACHER',
        roles: ['TEACHER'],
        orgIds: ['org-1']
      }
    };
    let persistedState: TrainingState | null = null;
    const cloneState = (state: TrainingState): TrainingState =>
      JSON.parse(JSON.stringify(state)) as TrainingState;
    const workspaceApi = {
      async load(): Promise<TrainingState | null> {
        return persistedState ? cloneState(persistedState) : null;
      },
      async save(
        _session: AuthSession,
        state: TrainingState
      ): Promise<TrainingState> {
        persistedState = cloneState(state);
        return cloneState(state);
      }
    };
    const store = createTrainingStore({
      storage: createMemoryStorage(),
      backend: { isEnabled: () => true } as BackendTrainingApi,
      workspaceApi,
      now: () => '2026-07-25T08:00:00.000Z',
      idFactory: (prefix) => `${prefix}-persisted`
    });

    await store.initializeAuthenticatedWorkspace(session);
    const lesson = await store.createLessonRemote({
      code: 'LESSON-REAL-001',
      title: '真实数据教案'
    });
    store.updateLesson(lesson.id, { description: '重新登录后仍需保留' });
    await store.flushAuthenticatedWorkspace();

    store.clearAuthenticatedWorkspace();
    await store.initializeAuthenticatedWorkspace(session);

    expect(store.getLesson(lesson.id)).toMatchObject({
      code: 'LESSON-REAL-001',
      title: '真实数据教案',
      description: '重新登录后仍需保留',
      teacherName: '张老师'
    });
  });

  it('creates, edits and duplicates a lesson without mutating the source', () => {
    const store = deterministicStore();
    const lesson = editableLesson(store);
    store.updateLesson(lesson.id, { title: '采购办理教案', tags: ['采购', '考试'] });
    const copied = store.duplicateLesson(lesson.id);

    expect(store.getLesson(lesson.id)?.title).toBe('采购办理教案');
    expect(copied).toMatchObject({
      title: '采购办理教案（副本）',
      status: 'DRAFT',
      version: 1
    });
    expect(copied.id).not.toBe(lesson.id);
  });

  it('adds, updates, reorders and removes serial stages', () => {
    const store = deterministicStore();
    const lesson = editableLesson(store);
    const first = store.addStage(lesson.id, recordedStage('handler', 40, '经办'));
    const second = store.addStage(lesson.id, recordedStage('reviewer', 40, '审核'));

    store.updateStage(lesson.id, second.id, { description: '审核提交内容' });
    store.moveStage(lesson.id, second.id, 'up');
    expect(store.getLesson(lesson.id)?.stages.map((stage) => stage.id)).toEqual([
      second.id,
      first.id
    ]);
    expect(store.getLesson(lesson.id)?.stages[0].description).toBe('审核提交内容');

    store.removeStage(lesson.id, first.id);
    expect(store.getLesson(lesson.id)?.stages).toHaveLength(1);
  });

  it('rejects publishing when a stage has no recorded operation', () => {
    const store = deterministicStore();
    const lesson = editableLesson(store);
    store.addStage(lesson.id, {
      name: '空白阶段',
      groupKey: 'handler',
      score: 80,
      recordedSteps: []
    });

    expect(() => store.publishLesson(lesson.id)).toThrowError(TrainingValidationError);
    expect(store.validateLesson(lesson.id)).toContain('每个教学点至少需要一个录制步骤');
  });

  it('rejects unreasonable total scores and publishes a complete lesson', () => {
    const store = deterministicStore();
    const lesson = editableLesson(store);
    const stage = store.addStage(lesson.id, recordedStage('handler', 70));

    expect(() => store.publishLesson(lesson.id)).toThrow('教学点客观分合计必须等于教案客观分');

    store.updateStage(lesson.id, stage.id, { score: 80 });
    const published = store.publishLesson(lesson.id);
    expect(published.status).toBe('PUBLISHED');
    expect(published.publishedAt).toBe('2026-07-25T08:00:00.000Z');
  });
});

describe('exam configuration and data pool', () => {
  it('saves valid exam settings and rejects invalid score weights', () => {
    const store = deterministicStore();
    const lesson = editableLesson(store);
    const settings = validExamSettings(lesson.id);

    expect(store.saveExamSettings(lesson.id, settings)).toEqual(settings);
    expect(() =>
      store.saveExamSettings(lesson.id, {
        ...settings,
        objectiveWeight: 70,
        subjectiveWeight: 20
      })
    ).toThrow('考试评分权重合计必须为 100');
  });

  it('supports arbitrary role groups and the same learner in multiple groups', () => {
    const store = deterministicStore();
    const lesson = editableLesson(store);
    const a = store.addStage(lesson.id, recordedStage('maker', 30));
    const b = store.addStage(lesson.id, recordedStage('reviewer', 30));
    const c = store.addStage(lesson.id, recordedStage('archiver', 20));
    const plan: GroupPlan = {
      lessonId: lesson.id,
      roles: [
        { key: 'maker', name: '经办', color: '#1d4ed8', stageIds: [a.id] },
        { key: 'reviewer', name: '审核', color: '#059669', stageIds: [b.id] },
        { key: 'archiver', name: '归档', color: '#7c3aed', stageIds: [c.id] }
      ],
      members: [
        {
          id: 'membership-1',
          studentId: 'student-1',
          studentName: '张敏',
          unitId: 'unit-1',
          unitName: '第一事业部',
          groupKey: 'maker',
          accountId: 'zhangmin'
        },
        {
          id: 'membership-2',
          studentId: 'student-1',
          studentName: '张敏',
          unitId: 'unit-1',
          unitName: '第一事业部',
          groupKey: 'reviewer',
          accountId: 'zhangmin'
        },
        {
          id: 'membership-3',
          studentId: 'student-2',
          studentName: '李晨',
          unitId: 'unit-1',
          unitName: '第一事业部',
          groupKey: 'archiver',
          accountId: 'lichen'
        }
      ]
    };

    const saved = store.saveGroupPlan(lesson.id, plan);
    expect(saved.roles).toHaveLength(3);
    expect(saved.members.filter((member) => member.studentId === 'student-1')).toHaveLength(2);
  });

  it('generates formal and spare data per unit with an intentional partial failure', () => {
    const store = deterministicStore();
    const lesson = editableLesson(store);
    const plans: UnitDataPlan[] = [
      {
        unitId: 'unit-1',
        unitName: '第一事业部',
        formalCount: 2,
        spareCount: 1,
        scenario: '标准场景',
        simulateFailure: true
      },
      {
        unitId: 'unit-2',
        unitName: '第二事业部',
        formalCount: 1,
        spareCount: 1,
        scenario: '加急场景',
        simulateFailure: false
      }
    ];
    store.saveUnitDataPlans(lesson.id, plans);

    const result = store.generateData(lesson.id);
    expect(result).toMatchObject({ requestedCount: 5, readyCount: 4, failedCount: 1 });
    expect(store.state.dataItems[lesson.id].filter((item) => item.kind === 'FORMAL')).toHaveLength(3);
    expect(
      store.state.dataItems[lesson.id].filter((item) => item.status === 'GENERATION_FAILED')
    ).toHaveLength(1);
  });

  it('retries failed data as a new ready revision while retaining audit history', () => {
    const store = deterministicStore();
    const lesson = editableLesson(store);
    store.saveUnitDataPlans(lesson.id, [
      {
        unitId: 'unit-1',
        unitName: '第一事业部',
        formalCount: 1,
        spareCount: 1,
        scenario: '失败演示',
        simulateFailure: true
      }
    ]);
    store.generateData(lesson.id);
    const failed = store.state.dataItems[lesson.id].find(
      (item) => item.status === 'GENERATION_FAILED'
    )!;

    const retried = store.retryData(lesson.id, failed.id, '业务接口临时失败');
    expect(retried).toMatchObject({
      status: 'READY',
      revision: 2,
      replacementOf: failed.id
    });
    expect(
      store.state.dataItems[lesson.id].find((item) => item.id === failed.id)
        ?.status
    ).toBe('DISABLED');
    expect(() =>
      store.retryData(lesson.id, failed.id, '重复重试')
    ).toThrow('只有生成失败的数据可以重试');
    expect(store.state.dataItems[lesson.id].find((item) => item.id === failed.id)?.audit).toEqual(
      expect.arrayContaining([expect.objectContaining({ type: 'DATA_RETRY_REQUESTED' })])
    );
    expect(
      store.disableData(lesson.id, failed.id, '重试已成功，关闭原失败记录').status
    ).toBe('DISABLED');
  });

  it('disables, promotes and replaces unused data with reasoned audits', () => {
    const store = deterministicStore();
    const lesson = editableLesson(store);
    store.saveUnitDataPlans(lesson.id, [
      {
        unitId: 'unit-1',
        unitName: '第一事业部',
        formalCount: 2,
        spareCount: 2,
        scenario: '标准场景',
        simulateFailure: false
      }
    ]);
    store.generateData(lesson.id);
    const [formal, replaceTarget] = store.state.dataItems[lesson.id].filter(
      (item) => item.kind === 'FORMAL'
    );
    const spare = store.state.dataItems[lesson.id].find((item) => item.kind === 'SPARE')!;

    expect(store.disableData(lesson.id, formal.id, '数据内容不符合考试要求').status).toBe(
      'DISABLED'
    );
    expect(store.promoteData(lesson.id, spare.id, '补足正式数据').kind).toBe('FORMAL');
    const replacement = store.replaceData(lesson.id, replaceTarget.id, '重新生成同条件数据');
    expect(replacement).toMatchObject({
      status: 'READY',
      replacementOf: replaceTarget.id,
      revision: 2
    });
    expect(store.state.dataItems[lesson.id].find((item) => item.id === replaceTarget.id)?.status).toBe(
      'DISABLED'
    );
  });
});

describe('published exam business chain', () => {
  it('enforces the configured exam start and end window', () => {
    const scheduledStore = deterministicStore();
    const { lesson: scheduledLesson } = configurePublishableExam(scheduledStore);
    scheduledStore.saveExamSettings(scheduledLesson.id, {
      ...validExamSettings(scheduledLesson.id),
      startAt: '2026-07-26T07:00:00.000Z',
      endAt: '2026-07-26T09:00:00.000Z'
    });
    scheduledStore.generateData(scheduledLesson.id);
    const scheduled = scheduledStore.publishExam(scheduledLesson.id);
    const scheduledTask = scheduledStore.state.studentTasks.find(
      (task) => task.publishedTaskId === scheduled.id
    )!;
    expect(scheduled.status).toBe('SCHEDULED');
    expect(() => scheduledStore.startStudentTask(scheduledTask.id)).toThrow(
      '考试尚未开始'
    );

    const finishedStore = deterministicStore();
    const { lesson: finishedLesson } = configurePublishableExam(finishedStore);
    finishedStore.saveExamSettings(finishedLesson.id, {
      ...validExamSettings(finishedLesson.id),
      startAt: '2026-07-24T07:00:00.000Z',
      endAt: '2026-07-24T09:00:00.000Z'
    });
    finishedStore.generateData(finishedLesson.id);
    const finished = finishedStore.publishExam(finishedLesson.id);
    const finishedTask = finishedStore.state.studentTasks.find(
      (task) => task.publishedTaskId === finished.id
    )!;
    expect(finished.status).toBe('FINISHED');
    expect(() => finishedStore.startStudentTask(finishedTask.id)).toThrow(
      '考试已结束'
    );
  });

  it('enforces the per-attempt duration while the exam window is still open', () => {
    let clock = '2026-07-25T08:00:00.000Z';
    let sequence = 0;
    const store = createTrainingStore({
      storage: createMemoryStorage(),
      now: () => clock,
      idFactory: (prefix) => `${prefix}-duration-${++sequence}`
    });
    const { lesson, first } = configurePublishableExam(store);
    store.saveExamSettings(lesson.id, {
      ...validExamSettings(lesson.id),
      endAt: '2026-07-25T12:00:00.000Z',
      durationMinutes: 30
    });
    store.generateData(lesson.id);
    const published = store.publishExam(lesson.id);
    const task = store.state.studentTasks.find(
      (candidate) =>
        candidate.publishedTaskId === published.id &&
        candidate.groupKeys.includes('handler')
    )!;
    store.startStudentTask(task.id);
    clock = '2026-07-25T08:31:00.000Z';

    expect(() => store.completeStudentStage(task.id, first.id)).toThrow(
      '本次作答已超时'
    );
  });

  it('blocks exam publication when formal data does not cover the largest unit role group', () => {
    const store = deterministicStore();
    const { lesson } = configurePublishableExam(store, { formalCount: 0 });
    store.generateData(lesson.id);

    expect(() => store.publishExam(lesson.id)).toThrow(
      '每个单位的可用正式数据不得少于该单位最大角色组人数'
    );
  });

  it('publishes a task, assigns data by unit/group index and merges same-person roles', () => {
    const store = deterministicStore();
    const { lesson } = configurePublishableExam(store, {
      sameStudentMultiGroup: true,
      formalCount: 1
    });
    store.generateData(lesson.id);

    const published = store.publishExam(lesson.id);
    const studentTasks = store.state.studentTasks.filter(
      (task) => task.publishedTaskId === published.id
    );
    expect(published).toMatchObject({ assignedCount: 1, groupCount: 2, dataCount: 1 });
    expect(studentTasks).toHaveLength(1);
    expect(studentTasks[0].groupKeys).toEqual(['handler', 'reviewer']);
    expect(studentTasks[0].dataItemId).toBeTruthy();
    const repeated = store.publishExam(lesson.id);
    expect(repeated.id).toBe(published.id);
    expect(
      store.state.studentTasks.filter(
        (task) => task.publishedTaskId === published.id
      )
    ).toHaveLength(1);
  });

  it('freezes lesson, exam and group configuration after task publication', () => {
    const store = deterministicStore();
    const { lesson } = configurePublishableExam(store);
    store.generateData(lesson.id);
    store.publishExam(lesson.id);

    expect(() => store.updateLesson(lesson.id, { title: '追溯修改' })).toThrow(
      '配置已冻结'
    );
    expect(() =>
      store.saveExamSettings(lesson.id, {
        ...validExamSettings(lesson.id),
        batchName: '追溯修改批次'
      })
    ).toThrow('配置已冻结');
    expect(() =>
      store.saveGroupPlan(lesson.id, store.state.groupPlans[lesson.id])
    ).toThrow('配置已冻结');
  });

  it('keeps a multi-role learner on one business datum even when role member order differs', () => {
    const store = deterministicStore();
    const lesson = editableLesson(store);
    const handler = store.addStage(lesson.id, recordedStage('handler', 40, '经办'));
    const reviewer = store.addStage(lesson.id, recordedStage('reviewer', 40, '审核'));
    store.publishLesson(lesson.id);
    store.saveExamSettings(lesson.id, validExamSettings(lesson.id));
    const member = (
      id: string,
      studentId: string,
      studentName: string,
      groupKey: string
    ) => ({
      id,
      studentId,
      studentName,
      unitId: 'unit-1',
      unitName: '第一事业部',
      groupKey,
      accountId: studentId
    });
    store.saveGroupPlan(lesson.id, {
      lessonId: lesson.id,
      roles: [
        { key: 'handler', name: '经办组', color: '#2563eb', stageIds: [handler.id] },
        { key: 'reviewer', name: '审核组', color: '#16a34a', stageIds: [reviewer.id] }
      ],
      members: [
        member('h-1', 'student-1', '张敏', 'handler'),
        member('h-2', 'student-2', '李晨', 'handler'),
        member('r-2', 'student-2', '李晨', 'reviewer'),
        member('r-1', 'student-1', '张敏', 'reviewer')
      ]
    });
    store.saveUnitDataPlans(lesson.id, [
      {
        unitId: 'unit-1',
        unitName: '第一事业部',
        formalCount: 2,
        spareCount: 0,
        scenario: '同人多角色',
        simulateFailure: false
      }
    ]);
    store.generateData(lesson.id);

    const published = store.publishExam(lesson.id);
    const tasks = store.state.studentTasks.filter(
      (task) => task.publishedTaskId === published.id
    );
    expect(tasks).toHaveLength(2);
    expect(tasks.every((task) => task.groupKeys.length === 2)).toBe(true);
  });

  it('routes every upstream datum through every role when group sizes differ', () => {
    const store = deterministicStore();
    const lesson = editableLesson(store);
    const handler = store.addStage(lesson.id, recordedStage('handler', 40, '经办'));
    const reviewer = store.addStage(lesson.id, recordedStage('reviewer', 40, '审核'));
    store.publishLesson(lesson.id);
    store.saveExamSettings(lesson.id, validExamSettings(lesson.id));
    const member = (index: number, groupKey: string) => ({
      id: `${groupKey}-${index}`,
      studentId: `${groupKey}-student-${index}`,
      studentName: `${groupKey}学员${index}`,
      unitId: 'unit-1',
      unitName: '第一事业部',
      groupKey,
      accountId: `${groupKey}-account-${index}`
    });
    store.saveGroupPlan(lesson.id, {
      lessonId: lesson.id,
      roles: [
        { key: 'handler', name: '经办组', color: '#2563eb', stageIds: [handler.id] },
        { key: 'reviewer', name: '审核组', color: '#16a34a', stageIds: [reviewer.id] }
      ],
      members: [
        ...Array.from({ length: 5 }, (_, index) => member(index + 1, 'handler')),
        ...Array.from({ length: 2 }, (_, index) => member(index + 1, 'reviewer'))
      ]
    });
    store.saveUnitDataPlans(lesson.id, [
      {
        unitId: 'unit-1',
        unitName: '第一事业部',
        formalCount: 5,
        spareCount: 0,
        scenario: '不等人数串行流转',
        simulateFailure: false
      }
    ]);
    store.generateData(lesson.id);

    const published = store.publishExam(lesson.id);
    const tasks = store.state.studentTasks.filter(
      (task) => task.publishedTaskId === published.id
    );
    const dataIds = new Set(tasks.map((task) => task.dataItemId));
    expect(dataIds.size).toBe(5);
    dataIds.forEach((dataItemId) => {
      const groupsOnDatum = new Set(
        tasks
          .filter((task) => task.dataItemId === dataItemId)
          .flatMap((task) => task.groupKeys)
      );
      expect(groupsOnDatum).toEqual(new Set(['handler', 'reviewer']));
    });
  });

  it('enforces serial handoff, records stage completion and submits objective results', () => {
    const store = deterministicStore();
    const { lesson, first, second } = configurePublishableExam(store);
    store.generateData(lesson.id);
    const published = store.publishExam(lesson.id);
    const tasks = store.state.studentTasks.filter(
      (task) => task.publishedTaskId === published.id
    );
    const handlerTask = tasks.find((task) => task.groupKeys.includes('handler'))!;
    const reviewerTask = tasks.find((task) => task.groupKeys.includes('reviewer'))!;

    store.startStudentTask(reviewerTask.id);
    expect(() => store.completeStudentStage(reviewerTask.id, second.id)).toThrow(
      '必须先完成前序教学点'
    );

    store.startStudentTask(handlerTask.id);
    store.completeStudentStage(handlerTask.id, first.id);
    expect(() => store.submitStudentTask(handlerTask.id)).toThrow(
      '请填写必填提交信息：业务单号'
    );
    const handlerSubmission = store.submitStudentTask(handlerTask.id, {
      businessNo: 'TEST-HANDLER'
    });
    expect(handlerSubmission).toMatchObject({ status: 'SUBMITTED', objectiveScore: 80 });

    store.completeStudentStage(reviewerTask.id, second.id);
    const reviewerSubmission = store.submitStudentTask(reviewerTask.id, {
      businessNo: 'TEST-REVIEWER'
    });
    expect(reviewerSubmission).toMatchObject({ status: 'SUBMITTED', objectiveScore: 80 });
    expect(
      store.state.publishedTasks.find((task) => task.id === published.id)?.completedCount
    ).toBe(2);
  });

  it('records teacher subjective score and comment without changing objective score', () => {
    const store = deterministicStore();
    const { lesson } = configurePublishableExam(store, {
      sameStudentMultiGroup: true
    });
    store.generateData(lesson.id);
    const published = store.publishExam(lesson.id);
    const task = store.state.studentTasks.find(
      (candidate) => candidate.publishedTaskId === published.id
    )!;
    store.startStudentTask(task.id);
    lesson.stages.forEach((stage) => store.completeStudentStage(task.id, stage.id));
    store.submitStudentTask(task.id, { businessNo: 'TEST-GRADE' });
    const objectiveScore = task.objectiveScore;

    const graded = store.gradeStudentTask(task.id, 18, '流程规范，说明材料可更完整');
    expect(graded).toMatchObject({
      status: 'GRADED',
      objectiveScore,
      subjectiveScore: 18,
      comment: '流程规范，说明材料可更完整'
    });
  });

  it('normalizes personal scores to the configured exam weights', () => {
    const store = deterministicStore();
    const { lesson } = configurePublishableExam(store, {
      sameStudentMultiGroup: true
    });
    store.saveExamSettings(lesson.id, {
      ...validExamSettings(lesson.id),
      objectiveWeight: 70,
      subjectiveWeight: 30
    });
    store.generateData(lesson.id);
    const published = store.publishExam(lesson.id);
    const task = store.state.studentTasks.find(
      (candidate) => candidate.publishedTaskId === published.id
    )!;

    store.startStudentTask(task.id);
    lesson.stages.forEach((stage) => store.completeStudentStage(task.id, stage.id));
    expect(
      store.submitStudentTask(task.id, { businessNo: 'TEST-WEIGHT' }).objectiveScore
    ).toBe(70);
    expect(store.gradeStudentTask(task.id, 30, '权重评分验证').subjectiveScore).toBe(30);
    expect(() => store.gradeStudentTask(task.id, 31, '超过主观分上限')).toThrow(
      '主观评分必须在 0-30 分之间'
    );
  });

  it('restarts every task on the same collaborative datum with newly generated data', () => {
    const store = deterministicStore();
    const { lesson, first } = configurePublishableExam(store);
    store.generateData(lesson.id);
    const published = store.publishExam(lesson.id);
    const unitTasks = store.state.studentTasks.filter(
      (task) => task.publishedTaskId === published.id
    );
    const handler = unitTasks.find((task) => task.groupKeys.includes('handler'))!;
    const oldDataId = handler.dataItemId;
    store.startStudentTask(handler.id);
    store.completeStudentStage(handler.id, first.id);
    store.submitStudentTask(handler.id, { businessNo: 'TEST-RESTART' });
    expect(published.completedCount).toBe(1);

    const restarted = store.restartStudentAttempt(handler.id);
    const refreshedTasks = store.state.studentTasks.filter(
      (task) => unitTasks.some((original) => original.id === task.id)
    );
    expect(restarted).toMatchObject({ status: 'TODO', attemptNumber: 2 });
    expect(new Set(refreshedTasks.map((task) => task.dataItemId)).size).toBe(1);
    expect(refreshedTasks[0].dataItemId).not.toBe(oldDataId);
    expect(
      refreshedTasks.every(
        (task) =>
          task.status === 'TODO' &&
          task.attemptNumber === 2 &&
          task.completedStageIds.length === 0 &&
          task.objectiveScore === undefined
      )
    ).toBe(true);
    expect(
      store.state.dataItems[lesson.id].find((item) => item.id === oldDataId)
    ).toMatchObject({ status: 'DISABLED' });
    expect(
      store.state.dataItems[lesson.id].find(
        (item) => item.id === refreshedTasks[0].dataItemId
      )
    ).toMatchObject({
      status: 'IN_USE',
      kind: 'FORMAL',
      replacementOf: oldDataId,
      revision: 2
    });
    expect(published.completedCount).toBe(0);
  });

  it('restarts a completed learning task from the first recorded stage', () => {
    const store = deterministicStore();
    const lesson = store.state.lessons[0];
    const publishedTaskId = 'published-learning-restart';
    const studentTaskId = 'student-learning-restart';
    store.state.publishedTasks.push({
      id: publishedTaskId,
      lessonId: lesson.id,
      title: '采购流程学习',
      mode: 'LEARNING',
      status: 'RUNNING',
      startAt: '2026-07-25T07:00:00.000Z',
      endAt: '2026-07-25T09:00:00.000Z',
      assignedCount: 1,
      groupCount: 3,
      dataCount: 0,
      completedCount: 1
    });
    store.state.studentTasks.push({
      id: studentTaskId,
      publishedTaskId,
      lessonId: lesson.id,
      studentId: 'student-learning',
      studentName: '学习学员',
      title: '采购流程学习',
      mode: 'LEARNING',
      groupKey: lesson.stages[0].groupKey,
      groupKeys: [...new Set(lesson.stages.map((stage) => stage.groupKey))],
      unitId: 'unit-learning',
      unitName: '学习班级',
      dataItemId: 'simulated-learning',
      attemptNumber: 1,
      submissionValues: {},
      status: 'SUBMITTED',
      currentStageIndex: lesson.stages.length,
      completedStageIds: lesson.stages.map((stage) => stage.id),
      objectiveScore: 80,
      startedAt: '2026-07-25T07:10:00.000Z',
      submittedAt: '2026-07-25T07:30:00.000Z',
      remoteExecutionId: 'execution-learning-old',
      remoteExecutionStatus: 'COMPLETED',
      remoteContextLoaded: true,
      syncStatus: 'SYNCED'
    });

    const restarted = store.restartLearningOrPractice(studentTaskId);

    expect(restarted).toMatchObject({
      status: 'TODO',
      attemptNumber: 2,
      currentStageIndex: 0,
      completedStageIds: [],
      submissionValues: {}
    });
    expect(restarted.objectiveScore).toBeUndefined();
    expect(restarted.startedAt).toBeUndefined();
    expect(restarted.submittedAt).toBeUndefined();
    expect(restarted.remoteExecutionId).toBeUndefined();
    expect(restarted.remoteExecutionStatus).toBeUndefined();
    expect(
      store.state.publishedTasks.find((task) => task.id === publishedTaskId)
        ?.completedCount
    ).toBe(0);
    expect(store.state.activities[0]).toMatchObject({
      type: 'TRAINING_TASK_RESTARTED'
    });
  });

  it('blocks restarting when the configured maximum attempt count is reached', () => {
    const store = deterministicStore();
    const { lesson } = configurePublishableExam(store, {
      sameStudentMultiGroup: true
    });
    store.generateData(lesson.id);
    const published = store.publishExam(lesson.id);
    const task = store.state.studentTasks.find(
      (candidate) => candidate.publishedTaskId === published.id
    )!;

    store.restartStudentAttempt(task.id);
    expect(task.attemptNumber).toBe(2);
    expect(() => store.restartStudentAttempt(task.id)).toThrow(
      '已达到最大作答次数'
    );
  });

  it('blocks restarting after every task in the collaborative unit is submitted', () => {
    const store = deterministicStore();
    const { lesson } = configurePublishableExam(store, {
      sameStudentMultiGroup: true
    });
    store.generateData(lesson.id);
    const published = store.publishExam(lesson.id);
    const task = store.state.studentTasks.find(
      (candidate) => candidate.publishedTaskId === published.id
    )!;
    store.startStudentTask(task.id);
    lesson.stages.forEach((stage) => store.completeStudentStage(task.id, stage.id));
    store.submitStudentTask(task.id, { businessNo: 'TEST-FINAL' });

    expect(() => store.restartStudentAttempt(task.id)).toThrow(
      '协作单元已全部提交'
    );
  });

  it('persists actions, records activity and resets the entire demo reactively', () => {
    const storage = createMemoryStorage();
    const store = deterministicStore(storage);
    const lesson = editableLesson(store);
    store.setRole('teacher');

    const reloaded = deterministicStore(storage);
    expect(reloaded.getLesson(lesson.id)).toBeTruthy();
    expect(reloaded.state.activities[0]).toMatchObject({ type: 'ROLE_SWITCHED' });

    const sameStateReference = store.state;
    store.resetDemo();
    expect(store.state).toBe(sameStateReference);
    expect(store.getLesson(lesson.id)).toBeUndefined();
    expect(store.state.lessons.length).toBe(createMockTrainingState().lessons.length);
  });
});
