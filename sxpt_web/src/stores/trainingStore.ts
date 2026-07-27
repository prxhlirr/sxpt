import { reactive } from 'vue';
import type {
  ActivityEvent,
  DataAuditEvent,
  ExamDataItem,
  ExamSettings,
  GroupPlan,
  LessonPlan,
  LessonStage,
  PortalRole,
  PublishedTask,
  StudentTask,
  StorageLike,
  TrainingState,
  UnitDataPlan
} from '../domain/models';
import {
  createTrainingApi,
  type TrainingApi
} from '../services/trainingApi';

export interface TrainingStoreOptions {
  api?: TrainingApi;
  storage?: StorageLike;
  now?: () => string;
  idFactory?: (prefix: string) => string;
}

export interface DataGenerationSummary {
  requestedCount: number;
  readyCount: number;
  failedCount: number;
  status: 'COMPLETED' | 'COMPLETED_WITH_FAILURES';
}

export class TrainingValidationError extends Error {
  constructor(public readonly issues: string[]) {
    super(issues.join('；'));
    this.name = 'TrainingValidationError';
  }
}

export function createTrainingStore(options: TrainingStoreOptions = {}) {
  const api = options.api ?? createTrainingApi(options.storage);
  const now = options.now ?? (() => new Date().toISOString());
  const idFactory = options.idFactory ?? defaultIdFactory;
  const state = reactive(api.loadState()) as TrainingState;

  const persist = () => {
    api.saveState(toPlain(state));
  };

  const requireLesson = (lessonId: string) => {
    const lesson = state.lessons.find((candidate) => candidate.id === lessonId);
    if (!lesson) throw new Error(`未找到教案：${lessonId}`);
    return lesson;
  };

  const requireDataItem = (lessonId: string, itemId: string) => {
    requireLesson(lessonId);
    const item = (state.dataItems[lessonId] ?? []).find(
      (candidate) => candidate.id === itemId
    );
    if (!item) throw new Error(`未找到考试数据：${itemId}`);
    return item;
  };

  const requireStudentTask = (taskId: string) => {
    const task = state.studentTasks.find((candidate) => candidate.id === taskId);
    if (!task) throw new Error(`未找到学员任务：${taskId}`);
    return task;
  };

  const assertLessonConfigurationMutable = (lessonId: string) => {
    if (state.publishedTasks.some((task) => task.lessonId === lessonId)) {
      throw new Error(
        '该教案已有已发布考试，配置已冻结；请复制教案后创建新版本'
      );
    }
  };

  const resolvePublishedStatus = (
    task: Pick<PublishedTask, 'startAt' | 'endAt'>,
    at = Date.parse(now())
  ): PublishedTask['status'] =>
    at < Date.parse(task.startAt)
      ? 'SCHEDULED'
      : at >= Date.parse(task.endAt)
        ? 'FINISHED'
        : 'RUNNING';

  const requireRunningExam = (task: StudentTask) => {
    const published = state.publishedTasks.find(
      (candidate) => candidate.id === task.publishedTaskId
    );
    if (!published) throw new Error('未找到已发布考试批次');
    published.status = resolvePublishedStatus(published);
    if (published.status === 'SCHEDULED') throw new Error('考试尚未开始');
    if (published.status === 'FINISHED') throw new Error('考试已结束');
    return published;
  };

  const assertAttemptWithinDuration = (task: StudentTask) => {
    const settings = state.examSettings[task.lessonId];
    if (!settings || !task.startedAt) return;
    const deadline =
      Date.parse(task.startedAt) + settings.durationMinutes * 60_000;
    if (Date.parse(now()) > deadline) {
      throw new Error('本次作答已超时，请按考试规则重新作答');
    }
  };

  const addActivity = (
    type: string,
    title: string,
    detail: string
  ): ActivityEvent => {
    const event: ActivityEvent = {
      id: idFactory('activity'),
      type,
      title,
      detail,
      at: now()
    };
    state.activities.unshift(event);
    if (state.activities.length > 200) state.activities.length = 200;
    return event;
  };

  const addDataAudit = (
    item: ExamDataItem,
    type: string,
    reason: string
  ): DataAuditEvent => {
    const event: DataAuditEvent = {
      id: idFactory('audit'),
      type,
      reason,
      at: now()
    };
    item.audit.push(event);
    return event;
  };

  function setRole(role: PortalRole) {
    state.currentRole = role;
    addActivity('ROLE_SWITCHED', '切换门户角色', `当前角色：${role}`);
    persist();
  }

  function refreshPublishedTaskStatuses(): PublishedTask[] {
    let changed = false;
    state.publishedTasks.forEach((task) => {
      const status = resolvePublishedStatus(task);
      if (task.status !== status) {
        task.status = status;
        changed = true;
      }
    });
    if (changed) persist();
    return state.publishedTasks;
  }

  function getLesson(lessonId: string) {
    return state.lessons.find((lesson) => lesson.id === lessonId);
  }

  function currentLesson(lessonId: string) {
    return getLesson(lessonId);
  }

  function createLesson(input: Partial<LessonPlan> = {}): LessonPlan {
    const timestamp = now();
    const lessonId = idFactory('lesson');
    const created: LessonPlan = {
      id: lessonId,
      code: input.code?.trim() || `LESSON-${state.lessons.length + 1}`,
      title: input.title?.trim() || '未命名实训教案',
      moduleName: input.moduleName?.trim() || '未分类业务',
      description: input.description?.trim() || '',
      version: 1,
      status: 'DRAFT',
      teacherName: input.teacherName?.trim() || '当前管理员',
      tags: input.tags ? [...input.tags] : [],
      objectiveMaxScore: input.objectiveMaxScore ?? 80,
      subjectiveMaxScore: input.subjectiveMaxScore ?? 20,
      stages: input.stages ? toPlain(input.stages) : [],
      updatedAt: timestamp
    };
    state.lessons.unshift(created);
    state.dataItems[lessonId] = [];
    addActivity('LESSON_CREATED', `新建教案：${created.title}`, created.code);
    persist();
    return requireLesson(lessonId);
  }

  function duplicateLesson(lessonId: string): LessonPlan {
    const source = requireLesson(lessonId);
    const timestamp = now();
    const copiedId = idFactory('lesson');
    const copied: LessonPlan = {
      ...toPlain(source),
      id: copiedId,
      code: uniqueLessonCode(`${source.code}-COPY`),
      title: `${source.title}（副本）`,
      version: 1,
      status: 'DRAFT',
      stages: source.stages.map((stage) => ({
        ...toPlain(stage),
        id: idFactory('stage'),
        stageKey: `${stage.stageKey}-copy`,
        recordedSteps: stage.recordedSteps.map((step) => ({
          ...toPlain(step),
          id: idFactory('record')
        }))
      })),
      updatedAt: timestamp,
      publishedAt: undefined
    };
    state.lessons.unshift(copied);
    state.dataItems[copiedId] = [];
    addActivity(
      'LESSON_DUPLICATED',
      `复制教案：${source.title}`,
      `新教案：${copied.title}`
    );
    persist();
    return requireLesson(copiedId);
  }

  function updateLesson(
    lessonId: string,
    patch: Partial<Omit<LessonPlan, 'id' | 'stages'>>
  ): LessonPlan {
    assertLessonConfigurationMutable(lessonId);
    const lesson = requireLesson(lessonId);
    const stableId = lesson.id;
    Object.assign(lesson, toPlain(patch), { id: stableId, updatedAt: now() });
    addActivity('LESSON_UPDATED', `更新教案：${lesson.title}`, '教案元数据已保存');
    persist();
    return lesson;
  }

  function addStage(
    lessonId: string,
    input: Partial<LessonStage> = {}
  ): LessonStage {
    assertLessonConfigurationMutable(lessonId);
    const lesson = requireLesson(lessonId);
    const stageId = idFactory('stage');
    const order = lesson.stages.length + 1;
    const created: LessonStage = {
      id: stageId,
      stageKey: input.stageKey?.trim() || `stage-${order}`,
      name: input.name?.trim() || `业务阶段 ${order}`,
      groupKey: input.groupKey?.trim() || '',
      description: input.description?.trim() || '',
      required: input.required ?? true,
      score: input.score ?? 0,
      completionMethod: input.completionMethod ?? 'mixed',
      visibility: input.visibility
        ? toPlain(input.visibility)
        : { LEARNING: true, PRACTICE: true, EXAM: true },
      recordedSteps: input.recordedSteps ? toPlain(input.recordedSteps) : []
    };
    lesson.stages.push(created);
    lesson.updatedAt = now();
    addActivity('LESSON_STAGE_ADDED', `新增阶段：${created.name}`, lesson.title);
    persist();
    return lesson.stages.find((stage) => stage.id === stageId)!;
  }

  function updateStage(
    lessonId: string,
    stageId: string,
    patch: Partial<Omit<LessonStage, 'id'>>
  ): LessonStage {
    assertLessonConfigurationMutable(lessonId);
    const lesson = requireLesson(lessonId);
    const stage = requireStage(lesson, stageId);
    Object.assign(stage, toPlain(patch), { id: stageId });
    lesson.updatedAt = now();
    addActivity('LESSON_STAGE_UPDATED', `更新阶段：${stage.name}`, lesson.title);
    persist();
    return stage;
  }

  function removeStage(lessonId: string, stageId: string) {
    assertLessonConfigurationMutable(lessonId);
    const lesson = requireLesson(lessonId);
    const index = lesson.stages.findIndex((stage) => stage.id === stageId);
    if (index < 0) throw new Error(`未找到阶段：${stageId}`);
    const [removed] = lesson.stages.splice(index, 1);
    const plan = state.groupPlans[lessonId];
    plan?.roles.forEach((role) => {
      role.stageIds = role.stageIds.filter((id) => id !== stageId);
    });
    lesson.updatedAt = now();
    addActivity('LESSON_STAGE_REMOVED', `删除阶段：${removed.name}`, lesson.title);
    persist();
    return removed;
  }

  function moveStage(
    lessonId: string,
    stageId: string,
    target: 'up' | 'down' | number
  ): LessonStage[] {
    assertLessonConfigurationMutable(lessonId);
    const lesson = requireLesson(lessonId);
    const from = lesson.stages.findIndex((stage) => stage.id === stageId);
    if (from < 0) throw new Error(`未找到阶段：${stageId}`);
    const desired =
      target === 'up' ? from - 1 : target === 'down' ? from + 1 : target;
    const to = Math.max(0, Math.min(lesson.stages.length - 1, desired));
    if (from !== to) {
      const [stage] = lesson.stages.splice(from, 1);
      lesson.stages.splice(to, 0, stage);
      lesson.updatedAt = now();
      addActivity('LESSON_STAGE_MOVED', `调整阶段顺序：${stage.name}`, lesson.title);
      persist();
    }
    return lesson.stages;
  }

  function validateLesson(lessonId: string): string[] {
    const lesson = requireLesson(lessonId);
    const issues: string[] = [];
    if (!lesson.title.trim()) issues.push('教案名称不能为空');
    if (!lesson.moduleName.trim()) issues.push('业务模块不能为空');
    if (lesson.stages.length === 0) issues.push('教案至少需要一个业务阶段');
    if (lesson.stages.some((stage) => !stage.groupKey.trim())) {
      issues.push('每个阶段必须指定负责角色组');
    }
    if (lesson.stages.some((stage) => stage.recordedSteps.length === 0)) {
      issues.push('每个阶段至少需要一个录制步骤');
    }
    if (
      !Number.isFinite(lesson.objectiveMaxScore) ||
      !Number.isFinite(lesson.subjectiveMaxScore) ||
      lesson.objectiveMaxScore < 0 ||
      lesson.subjectiveMaxScore < 0 ||
      lesson.objectiveMaxScore + lesson.subjectiveMaxScore !== 100
    ) {
      issues.push('教案客观分与主观分合计必须为 100');
    }
    if (lesson.stages.some((stage) => !Number.isFinite(stage.score) || stage.score < 0)) {
      issues.push('阶段分值必须为非负数');
    }
    const stageScore = lesson.stages.reduce((total, stage) => total + stage.score, 0);
    if (stageScore !== lesson.objectiveMaxScore) {
      issues.push('阶段客观分合计必须等于教案客观分');
    }
    return [...new Set(issues)];
  }

  function publishLesson(lessonId: string): LessonPlan {
    const lesson = requireLesson(lessonId);
    const issues = validateLesson(lessonId);
    if (issues.length) throw new TrainingValidationError(issues);
    if (lesson.status === 'PUBLISHED') lesson.version += 1;
    lesson.status = 'PUBLISHED';
    lesson.publishedAt = now();
    lesson.updatedAt = lesson.publishedAt;
    addActivity(
      'LESSON_PUBLISHED',
      `发布教案：${lesson.title}`,
      `版本 V${lesson.version}`
    );
    persist();
    return lesson;
  }

  function saveExamSettings(
    lessonId: string,
    input: ExamSettings | Omit<ExamSettings, 'lessonId'>
  ): ExamSettings {
    assertLessonConfigurationMutable(lessonId);
    requireLesson(lessonId);
    const settings: ExamSettings = { ...toPlain(input), lessonId };
    const issues: string[] = [];
    if (!settings.batchName.trim()) issues.push('考试名称不能为空');
    if (
      settings.objectiveWeight < 0 ||
      settings.subjectiveWeight < 0 ||
      settings.objectiveWeight + settings.subjectiveWeight !== 100
    ) {
      issues.push('考试评分权重合计必须为 100');
    }
    if (!Number.isInteger(settings.durationMinutes) || settings.durationMinutes <= 0) {
      issues.push('考试时长必须为正整数');
    }
    if (!Number.isInteger(settings.maxAttempts) || settings.maxAttempts < 1) {
      issues.push('最大作答次数至少为 1');
    }
    if (
      !isDate(settings.startAt) ||
      !isDate(settings.endAt) ||
      Date.parse(settings.endAt) <= Date.parse(settings.startAt)
    ) {
      issues.push('考试结束时间必须晚于开始时间');
    }
    if (issues.length) throw new TrainingValidationError(issues);
    state.examSettings[lessonId] = settings;
    addActivity('EXAM_SETTINGS_SAVED', `保存考试设置：${settings.batchName}`, lessonId);
    persist();
    return state.examSettings[lessonId];
  }

  function saveGroupPlan(lessonId: string, input: GroupPlan): GroupPlan {
    assertLessonConfigurationMutable(lessonId);
    const lesson = requireLesson(lessonId);
    const plan: GroupPlan = { ...toPlain(input), lessonId };
    const issues: string[] = [];
    const roleKeys = plan.roles.map((role) => role.key.trim());
    if (plan.roles.length === 0) issues.push('至少需要一个角色组');
    if (roleKeys.some((key) => !key)) issues.push('角色组标识不能为空');
    if (new Set(roleKeys).size !== roleKeys.length) issues.push('角色组标识不能重复');
    const stageIds = new Set(lesson.stages.map((stage) => stage.id));
    const mappedStageIds = plan.roles.flatMap((role) => role.stageIds);
    if (mappedStageIds.some((stageId) => !stageIds.has(stageId))) {
      issues.push('角色组包含不存在的教案阶段');
    }
    if (
      new Set(mappedStageIds).size !== mappedStageIds.length ||
      lesson.stages.some((stage) => !mappedStageIds.includes(stage.id))
    ) {
      issues.push('每个教案阶段必须且只能属于一个角色组');
    }
    const membershipKeys = plan.members.map(
      (member) => `${member.studentId}|${member.groupKey}`
    );
    if (new Set(membershipKeys).size !== membershipKeys.length) {
      issues.push('同一学员不能重复加入同一个角色组');
    }
    if (plan.members.some((member) => !roleKeys.includes(member.groupKey))) {
      issues.push('学员分组必须引用已存在的角色组');
    }
    if (
      plan.members.some(
        (member) =>
          !member.studentId.trim() ||
          !member.studentName.trim() ||
          !member.unitId.trim() ||
          !member.accountId.trim()
      )
    ) {
      issues.push('学员、单位和业务账号信息必须完整');
    }
    if (issues.length) throw new TrainingValidationError(issues);

    plan.roles.forEach((role) => {
      role.stageIds.forEach((stageId) => {
        requireStage(lesson, stageId).groupKey = role.key;
      });
    });
    state.groupPlans[lessonId] = plan;
    lesson.updatedAt = now();
    addActivity(
      'GROUP_PLAN_SAVED',
      `保存分组：${lesson.title}`,
      `${plan.roles.length} 个角色组，${plan.members.length} 条成员关系`
    );
    persist();
    return state.groupPlans[lessonId];
  }

  function saveUnitDataPlans(
    lessonId: string,
    input: UnitDataPlan[]
  ): UnitDataPlan[] {
    assertLessonConfigurationMutable(lessonId);
    requireLesson(lessonId);
    const plans = toPlain(input);
    const issues: string[] = [];
    const unitIds = plans.map((plan) => plan.unitId.trim());
    if (plans.length === 0) issues.push('至少需要一个单位数据计划');
    if (unitIds.some((unitId) => !unitId)) issues.push('单位标识不能为空');
    if (new Set(unitIds).size !== unitIds.length) issues.push('单位数据计划不能重复');
    if (
      plans.some(
        (plan) =>
          !Number.isInteger(plan.formalCount) ||
          plan.formalCount < 0 ||
          !Number.isInteger(plan.spareCount) ||
          plan.spareCount < 0
      )
    ) {
      issues.push('正式数据和备用数据数量必须为非负整数');
    }
    if (issues.length) throw new TrainingValidationError(issues);
    state.unitDataPlans[lessonId] = plans;
    addActivity(
      'UNIT_DATA_PLANS_SAVED',
      `保存考试数据计划：${requireLesson(lessonId).title}`,
      `${plans.length} 个单位`
    );
    persist();
    return state.unitDataPlans[lessonId];
  }

  function generateData(lessonId: string): DataGenerationSummary {
    const lesson = requireLesson(lessonId);
    const plans = state.unitDataPlans[lessonId] ?? [];
    if (!plans.length) {
      throw new TrainingValidationError(['请先配置单位考试数据计划']);
    }
    const items = (state.dataItems[lessonId] ??= []);
    let requestedCount = 0;
    let readyCount = 0;
    let failedCount = 0;

    plans.forEach((plan) => {
      const total = plan.formalCount + plan.spareCount;
      requestedCount += total;
      for (let index = 0; index < total; index += 1) {
        const isSpare = index >= plan.formalCount;
        const shouldFail = plan.simulateFailure && index === total - 1;
        const itemId = idFactory('data');
        const item: ExamDataItem = {
          id: itemId,
          lessonId,
          unitId: plan.unitId,
          unitName: plan.unitName,
          kind: isSpare ? 'SPARE' : 'FORMAL',
          status: shouldFail ? 'GENERATION_FAILED' : 'READY',
          revision: 1,
          maskedReference: maskedReference(plan.unitId, itemId, index + 1),
          failureReason: shouldFail ? 'Mock：业务造数接口返回可重试错误' : undefined,
          assignedStudentTaskIds: [],
          audit: []
        };
        addDataAudit(
          item,
          shouldFail ? 'DATA_GENERATION_FAILED' : 'DATA_GENERATION_READY',
          shouldFail ? 'Mock：按单位模拟部分失败' : `Mock：${plan.scenario}`
        );
        items.push(item);
        if (shouldFail) failedCount += 1;
        else readyCount += 1;
      }
    });
    const result: DataGenerationSummary = {
      requestedCount,
      readyCount,
      failedCount,
      status: failedCount ? 'COMPLETED_WITH_FAILURES' : 'COMPLETED'
    };
    addActivity(
      'EXAM_DATA_GENERATED',
      `生成考试数据：${lesson.title}`,
      `请求 ${requestedCount}，成功 ${readyCount}，失败 ${failedCount}`
    );
    persist();
    return result;
  }

  function retryData(
    lessonId: string,
    itemId: string,
    reason: string
  ): ExamDataItem {
    const normalizedReason = requiredReason(reason);
    const original = requireDataItem(lessonId, itemId);
    if (original.status !== 'GENERATION_FAILED') {
      throw new Error('只有生成失败的数据可以重试');
    }
    addDataAudit(original, 'DATA_RETRY_REQUESTED', normalizedReason);
    original.status = 'DISABLED';
    const retried = createRevision(original, 'READY', normalizedReason);
    retried.failureReason = undefined;
    (state.dataItems[lessonId] ??= []).push(retried);
    addActivity('EXAM_DATA_RETRIED', '重试考试数据', normalizedReason);
    persist();
    return retried;
  }

  function disableData(
    lessonId: string,
    itemId: string,
    reason: string
  ): ExamDataItem {
    const normalizedReason = requiredReason(reason);
    const item = requireDataItem(lessonId, itemId);
    if (item.status === 'DISABLED') return item;
    if (item.status !== 'READY' && item.status !== 'GENERATION_FAILED') {
      throw new Error('只有未使用的就绪数据或生成失败数据可以停用');
    }
    item.status = 'DISABLED';
    addDataAudit(item, 'DATA_DISABLED', normalizedReason);
    addActivity('EXAM_DATA_DISABLED', '停用考试数据', normalizedReason);
    persist();
    return item;
  }

  function promoteData(
    lessonId: string,
    itemId: string,
    reason: string
  ): ExamDataItem {
    const normalizedReason = requiredReason(reason);
    const item = requireDataItem(lessonId, itemId);
    if (item.status !== 'READY' || item.kind !== 'SPARE') {
      throw new Error('只有未使用的就绪备用数据可以转正式');
    }
    item.kind = 'FORMAL';
    addDataAudit(item, 'DATA_PROMOTED_TO_FORMAL', normalizedReason);
    addActivity('EXAM_DATA_PROMOTED', '备用数据转正式', normalizedReason);
    persist();
    return item;
  }

  function replaceData(
    lessonId: string,
    itemId: string,
    reason: string
  ): ExamDataItem {
    const normalizedReason = requiredReason(reason);
    const original = requireDataItem(lessonId, itemId);
    if (original.status !== 'READY') {
      throw new Error('只有未使用的就绪数据可以替换');
    }
    original.status = 'DISABLED';
    addDataAudit(original, 'DATA_REPLACED', normalizedReason);
    const replacement = createRevision(original, 'READY', normalizedReason);
    (state.dataItems[lessonId] ??= []).push(replacement);
    addActivity('EXAM_DATA_REPLACED', '替换考试数据', normalizedReason);
    persist();
    return replacement;
  }

  function validateExamPublication(lessonId: string): string[] {
    const lesson = requireLesson(lessonId);
    const issues: string[] = [];
    if (lesson.status !== 'PUBLISHED') issues.push('必须先发布教案');
    if (!state.examSettings[lessonId]) issues.push('必须先保存考试设置');
    const plan = state.groupPlans[lessonId];
    if (!plan?.roles.length) issues.push('必须先配置角色组');
    if (!plan?.members.length) issues.push('必须先配置学员分组');
    const unitPlans = state.unitDataPlans[lessonId] ?? [];
    if (!unitPlans.length) issues.push('必须先配置单位考试数据');

    if (plan?.roles.length && plan.members.length) {
      const unitIds = [...new Set(plan.members.map((member) => member.unitId))];
      unitIds.forEach((unitId) => {
        const groupCounts = plan.roles.map(
          (role) =>
            plan.members.filter(
              (member) => member.unitId === unitId && member.groupKey === role.key
            ).length
        );
        if (groupCounts.some((count) => count === 0)) {
          issues.push('每个参与单位的每个角色组至少需要一名学员');
        }
        const requiredCount = Math.max(0, ...groupCounts);
        const readyFormalCount = (state.dataItems[lessonId] ?? []).filter(
          (item) =>
            item.unitId === unitId &&
            item.kind === 'FORMAL' &&
            item.status === 'READY'
        ).length;
        if (readyFormalCount < requiredCount) {
          issues.push('每个单位的可用正式数据不得少于该单位最大角色组人数');
        }
        if (!unitPlans.some((unitPlan) => unitPlan.unitId === unitId)) {
          issues.push('学员所属单位缺少考试数据计划');
        }
      });
    }
    return [...new Set(issues)];
  }

  function publishExam(lessonId: string): PublishedTask {
    const existing = state.publishedTasks.find(
      (task) => task.lessonId === lessonId
    );
    if (existing) return existing;
    const lesson = requireLesson(lessonId);
    const issues = validateExamPublication(lessonId);
    if (issues.length) throw new TrainingValidationError(issues);
    const settings = state.examSettings[lessonId]!;
    const groupPlan = state.groupPlans[lessonId]!;
    const publishedTaskId = idFactory('published-task');
    const assignments = buildStudentAssignments(
      lessonId,
      publishedTaskId,
      lesson,
      settings,
      groupPlan
    );
    const status = resolvePublishedStatus(settings);
    const usedDataIds = new Set(assignments.map((assignment) => assignment.dataItemId));
    const published: PublishedTask = {
      id: publishedTaskId,
      lessonId,
      title: settings.batchName,
      mode: settings.mode,
      status,
      startAt: settings.startAt,
      endAt: settings.endAt,
      assignedCount: assignments.length,
      groupCount: groupPlan.roles.length,
      dataCount: usedDataIds.size,
      completedCount: 0
    };
    state.publishedTasks.unshift(published);
    state.studentTasks.push(...assignments);
    usedDataIds.forEach((dataItemId) => {
      const item = requireDataItem(lessonId, dataItemId);
      item.status = 'IN_USE';
      item.assignedStudentTaskIds = assignments
        .filter((assignment) => assignment.dataItemId === dataItemId)
        .map((assignment) => assignment.id);
      addDataAudit(item, 'DATA_ASSIGNED_TO_EXAM', publishedTaskId);
    });
    addActivity(
      'EXAM_PUBLISHED',
      `发布考试：${settings.batchName}`,
      `${assignments.length} 个学员任务，${usedDataIds.size} 条正式数据`
    );
    persist();
    return published;
  }

  function startStudentTask(taskId: string): StudentTask {
    const task = requireStudentTask(taskId);
    requireRunningExam(task);
    if (task.status === 'DOING') return task;
    if (task.status !== 'TODO') throw new Error('只有待办任务可以开始');
    task.status = 'DOING';
    task.startedAt = now();
    addActivity('STUDENT_TASK_STARTED', `${task.studentName}开始办理`, task.title);
    persist();
    return task;
  }

  function completeStudentStage(taskId: string, stageId: string): StudentTask {
    const task = requireStudentTask(taskId);
    requireRunningExam(task);
    assertAttemptWithinDuration(task);
    if (task.status !== 'DOING') throw new Error('任务必须处于办理中');
    const lesson = requireLesson(task.lessonId);
    const stage = requireStage(lesson, stageId);
    if (!stage.visibility[task.mode]) {
      throw new Error('该阶段在当前考试模式中不可办理');
    }
    if (!task.groupKeys.includes(stage.groupKey)) {
      throw new Error('该阶段不属于学员负责的角色组');
    }
    if (task.completedStageIds.includes(stageId)) return task;
    const stageIndex = lesson.stages.findIndex((candidate) => candidate.id === stageId);
    const completedForData = new Set(
      state.studentTasks
        .filter(
          (candidate) =>
            candidate.publishedTaskId === task.publishedTaskId &&
            candidate.dataItemId === task.dataItemId
        )
        .flatMap((candidate) => candidate.completedStageIds)
    );
    const incompletePrevious = lesson.stages
      .slice(0, stageIndex)
      .some(
        (previous) =>
          previous.visibility[task.mode] &&
          previous.required &&
          !completedForData.has(previous.id)
      );
    if (incompletePrevious) throw new Error('必须先完成前序阶段');

    task.completedStageIds.push(stageId);
    task.currentStageIndex = stageIndex + 1;
    addActivity(
      'STUDENT_STAGE_COMPLETED',
      `${task.studentName}完成阶段：${stage.name}`,
      `依据平台会话与流程操作轨迹判定`
    );
    persist();
    return task;
  }

  function submitStudentTask(
    taskId: string,
    submissionValues: Record<string, string> = {}
  ): StudentTask {
    const task = requireStudentTask(taskId);
    requireRunningExam(task);
    assertAttemptWithinDuration(task);
    if (task.status !== 'DOING') throw new Error('只有办理中的任务可以提交');
    const lesson = requireLesson(task.lessonId);
    const assignedStages = lesson.stages.filter(
      (stage) =>
        stage.visibility[task.mode] &&
        task.groupKeys.includes(stage.groupKey)
    );
    if (
      assignedStages.some(
        (stage) => stage.required && !task.completedStageIds.includes(stage.id)
      )
    ) {
      throw new Error('必须先完成本人负责的全部必做阶段');
    }
    const submissionFields =
      state.examSettings[task.lessonId]?.submissionFields ?? [];
    const normalizedSubmissionValues = Object.fromEntries(
      submissionFields.map((field) => [
        field.key,
        String(submissionValues[field.key] ?? '').trim()
      ])
    );
    const missingFields = submissionFields.filter(
      (field) =>
        field.required && !normalizedSubmissionValues[field.key]
    );
    if (missingFields.length) {
      throw new Error(
        `请填写必填提交信息：${missingFields.map((field) => field.label).join('、')}`
      );
    }
    task.status = 'SUBMITTED';
    const assignedRawScore = assignedStages.reduce(
      (total, stage) => total + stage.score,
      0
    );
    const completedRawScore = assignedStages
      .filter((stage) => task.completedStageIds.includes(stage.id))
      .reduce((total, stage) => total + stage.score, 0);
    const examObjectiveMax =
      state.examSettings[task.lessonId]?.objectiveWeight ??
      lesson.objectiveMaxScore;
    task.objectiveScore = assignedRawScore
      ? Math.round((completedRawScore / assignedRawScore) * examObjectiveMax)
      : 0;
    task.submissionValues = normalizedSubmissionValues;
    task.submittedAt = now();
    const published = state.publishedTasks.find(
      (candidate) => candidate.id === task.publishedTaskId
    );
    if (published) {
      published.completedCount = state.studentTasks.filter(
        (candidate) =>
          candidate.publishedTaskId === published.id &&
          (candidate.status === 'SUBMITTED' || candidate.status === 'GRADED')
      ).length;
    }
    addActivity(
      'STUDENT_TASK_SUBMITTED',
      `${task.studentName}已提交`,
      `系统客观评分：${task.objectiveScore}`
    );
    persist();
    return task;
  }

  function restartStudentAttempt(taskId: string): StudentTask {
    const requestedTask = requireStudentTask(taskId);
    requireRunningExam(requestedTask);
    const settings = state.examSettings[requestedTask.lessonId];
    if (!settings?.allowRetry) throw new Error('当前考试不允许重新作答');

    const collaborationTasks = state.studentTasks.filter(
      (task) =>
        task.publishedTaskId === requestedTask.publishedTaskId &&
        task.dataItemId === requestedTask.dataItemId
    );
    if (
      collaborationTasks.length > 0 &&
      collaborationTasks.every(
        (task) => task.status === 'SUBMITTED' || task.status === 'GRADED'
      )
    ) {
      throw new Error('协作单元已全部提交，不能重新作答');
    }
    const currentAttempt = Math.max(
      1,
      ...collaborationTasks.map((task) => task.attemptNumber)
    );
    if (currentAttempt >= settings.maxAttempts) {
      throw new Error('已达到最大作答次数');
    }

    const original = requireDataItem(
      requestedTask.lessonId,
      requestedTask.dataItemId
    );
    if (original.status !== 'IN_USE') {
      throw new Error('当前协作单元没有可替换的在用数据');
    }
    const reason = `学员在最终提交前重新作答（第 ${currentAttempt + 1} 次）`;
    original.status = 'DISABLED';
    addDataAudit(original, 'STUDENT_ATTEMPT_DATA_DISABLED', reason);

    const replacement: ExamDataItem = {
      ...toPlain(original),
      id: idFactory('data'),
      kind: 'FORMAL',
      status: 'IN_USE',
      revision: original.revision + 1,
      maskedReference: maskedReference(
        original.unitId,
        idFactory('reference'),
        original.revision + 1
      ),
      replacementOf: original.id,
      failureReason: undefined,
      assignedStudentTaskIds: collaborationTasks.map((task) => task.id),
      audit: []
    };
    addDataAudit(replacement, 'STUDENT_ATTEMPT_DATA_READY', reason);
    (state.dataItems[requestedTask.lessonId] ??= []).push(replacement);

    collaborationTasks.forEach((task) => {
      task.dataItemId = replacement.id;
      task.attemptNumber = currentAttempt + 1;
      task.status = 'TODO';
      task.currentStageIndex = 0;
      task.completedStageIds = [];
      task.submissionValues = {};
      delete task.objectiveScore;
      delete task.subjectiveScore;
      delete task.comment;
      delete task.startedAt;
      delete task.submittedAt;
      delete task.gradedAt;
    });
    const published = state.publishedTasks.find(
      (task) => task.id === requestedTask.publishedTaskId
    );
    if (published) {
      published.completedCount = state.studentTasks.filter(
        (task) =>
          task.publishedTaskId === published.id &&
          (task.status === 'SUBMITTED' || task.status === 'GRADED')
      ).length;
    }
    addActivity(
      'STUDENT_ATTEMPT_RESTARTED',
      `${requestedTask.studentName}重新作答`,
      `协作单元已整体切换到新数据，第 ${currentAttempt + 1} 次作答`
    );
    persist();
    return requireStudentTask(taskId);
  }

  function gradeStudentTask(
    taskId: string,
    subjectiveScore: number,
    comment: string
  ): StudentTask {
    const task = requireStudentTask(taskId);
    if (task.status !== 'SUBMITTED' && task.status !== 'GRADED') {
      throw new Error('只有已提交任务可以评分');
    }
    const lesson = requireLesson(task.lessonId);
    const maxScore =
      state.examSettings[task.lessonId]?.subjectiveWeight ??
      lesson.subjectiveMaxScore;
    if (
      !Number.isFinite(subjectiveScore) ||
      subjectiveScore < 0 ||
      subjectiveScore > maxScore
    ) {
      throw new Error(`主观评分必须在 0-${maxScore} 分之间`);
    }
    const normalizedComment = requiredReason(comment, '教师批语不能为空');
    task.status = 'GRADED';
    task.subjectiveScore = subjectiveScore;
    task.comment = normalizedComment;
    task.gradedAt = now();
    addActivity(
      'STUDENT_TASK_GRADED',
      `完成评阅：${task.studentName}`,
      `主观评分 ${subjectiveScore}，${normalizedComment}`
    );
    persist();
    return task;
  }

  function resetDemo(): TrainingState {
    replaceReactiveState(state, api.resetDemo());
    return state;
  }

  function createRevision(
    original: ExamDataItem,
    status: ExamDataItem['status'],
    reason: string
  ): ExamDataItem {
    const created: ExamDataItem = {
      ...toPlain(original),
      id: idFactory('data'),
      status,
      revision: original.revision + 1,
      maskedReference: maskedReference(
        original.unitId,
        idFactory('reference'),
        original.revision + 1
      ),
      replacementOf: original.id,
      assignedStudentTaskIds: [],
      audit: []
    };
    addDataAudit(created, 'DATA_REVISION_READY', reason);
    return created;
  }

  function buildStudentAssignments(
    lessonId: string,
    publishedTaskId: string,
    lesson: LessonPlan,
    settings: ExamSettings,
    groupPlan: GroupPlan
  ): StudentTask[] {
    const assignments = new Map<
      string,
      {
        member: GroupPlan['members'][number];
        dataItemId: string;
        groupKeys: string[];
      }
    >();
    const unitIds = [...new Set(groupPlan.members.map((member) => member.unitId))];

    unitIds.forEach((unitId) => {
      const eligibleDataItems = (state.dataItems[lessonId] ?? []).filter(
        (item) =>
          item.unitId === unitId && item.kind === 'FORMAL' && item.status === 'READY'
      );
      const dataItems = settings.randomizeData
        ? [...eligibleDataItems].sort(
            (left, right) =>
              stableHash(`${publishedTaskId}:${left.id}`) -
              stableHash(`${publishedTaskId}:${right.id}`)
          )
        : eligibleDataItems;
      const preferredDataByStudent = new Map<string, string>();
      groupPlan.roles.forEach((role) => {
        const members = groupPlan.members.filter(
          (member) => member.unitId === unitId && member.groupKey === role.key
        );
        const usedByRole = new Set<string>();
        const assign = (
          member: GroupPlan['members'][number],
          dataItemId: string
        ) => {
          usedByRole.add(dataItemId);
          preferredDataByStudent.set(member.studentId, dataItemId);
          const key = `${member.studentId}|${dataItemId}`;
          const existing = assignments.get(key);
          if (existing) {
            if (!existing.groupKeys.includes(role.key)) {
              existing.groupKeys.push(role.key);
            }
          } else {
            assignments.set(key, {
              member,
              dataItemId,
              groupKeys: [role.key]
            });
          }
        };

        members.forEach((member) => {
          const preferredId = preferredDataByStudent.get(member.studentId);
          const preferred =
            preferredId && !usedByRole.has(preferredId)
              ? dataItems.find((item) => item.id === preferredId)
              : undefined;
          const dataItem =
            preferred ?? dataItems.find((item) => !usedByRole.has(item.id));
          if (!dataItem) {
            throw new TrainingValidationError([
              '每个单位的可用正式数据不得少于该单位最大角色组人数'
            ]);
          }
          assign(member, dataItem.id);
        });

        let roundRobinIndex = 0;
        dataItems
          .filter((dataItem) => !usedByRole.has(dataItem.id))
          .forEach((dataItem) => {
            const alreadyOnData = members.find((member) =>
              assignments.has(`${member.studentId}|${dataItem.id}`)
            );
            const member =
              alreadyOnData ?? members[roundRobinIndex % members.length];
            if (!member) {
              throw new TrainingValidationError([
                '每个参与单位的每个角色组至少需要一名学员'
              ]);
            }
            assign(member, dataItem.id);
            roundRobinIndex += 1;
        });
      });
    });

    return [...assignments.values()].map(({ member, dataItemId, groupKeys }) => ({
      id: idFactory('student-task'),
      publishedTaskId,
      lessonId,
      studentId: member.studentId,
      studentName: member.studentName,
      title: `${settings.batchName} · ${member.unitName}`,
      mode: settings.mode,
      groupKey: groupKeys[0],
      groupKeys,
      unitId: member.unitId,
      unitName: member.unitName,
      dataItemId,
      attemptNumber: 1,
      submissionValues: {},
      status: 'TODO',
      currentStageIndex: 0,
      completedStageIds: []
    }));
  }

  return {
    state,
    currentLesson,
    getLesson,
    setRole,
    refreshPublishedTaskStatuses,
    createLesson,
    duplicateLesson,
    updateLesson,
    addStage,
    updateStage,
    removeStage,
    moveStage,
    validateLesson,
    publishLesson,
    saveExamSettings,
    saveGroupPlan,
    saveUnitDataPlans,
    generateData,
    retryData,
    disableData,
    promoteData,
    replaceData,
    validateExamPublication,
    publishExam,
    startStudentTask,
    completeStudentStage,
    submitStudentTask,
    restartStudentAttempt,
    gradeStudentTask,
    resetDemo
  };
}

export type TrainingStore = ReturnType<typeof createTrainingStore>;

let singleton: TrainingStore | undefined;

export function useTrainingStore(): TrainingStore {
  singleton ??= createTrainingStore();
  return singleton;
}

function requireStage(lesson: LessonPlan, stageId: string): LessonStage {
  const stage = lesson.stages.find((candidate) => candidate.id === stageId);
  if (!stage) throw new Error(`未找到阶段：${stageId}`);
  return stage;
}

function uniqueLessonCode(candidate: string): string {
  return `${candidate}-${Date.now().toString(36).toUpperCase()}`;
}

function isDate(value: string): boolean {
  return Number.isFinite(Date.parse(value));
}

function requiredReason(reason: string, message = '操作原因不能为空'): string {
  const normalized = reason.trim();
  if (!normalized) throw new Error(message);
  return normalized;
}

function maskedReference(unitId: string, id: string, index: number): string {
  const suffix = id.replace(/[^a-zA-Z0-9]/g, '').slice(-4).toUpperCase();
  return `${unitId.toUpperCase()}-***-${String(index).padStart(2, '0')}-${suffix}`;
}

function stableHash(value: string): number {
  let hash = 0;
  for (let index = 0; index < value.length; index += 1) {
    hash = (hash * 31 + value.charCodeAt(index)) | 0;
  }
  return hash;
}

function defaultIdFactory(prefix: string): string {
  const suffix =
    typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `${prefix}-${suffix}`;
}

function replaceReactiveState(target: TrainingState, source: TrainingState) {
  Object.keys(target).forEach((key) => {
    delete (target as unknown as Record<string, unknown>)[key];
  });
  Object.assign(target, toPlain(source));
}

function toPlain<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}
