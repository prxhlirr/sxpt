import { captureApi } from '../api/capture';
import { connectorApi } from '../api/connector';
import { courseApi } from '../api/course';
import type {
  ConnectorSystem,
  Course,
  EvaluationRule,
  TaskExecution,
  TeachingTask
} from '../api/contracts';
import { evaluationApi } from '../api/evaluation';
import { executionApi } from '../api/execution';
import { teachingApi } from '../api/teaching';
import { usersApi } from '../api/users';
import { getApiConfig } from '../config/api';
import type {
  BusinessPlatform,
  BusinessPlatformModule,
  LessonPlan,
  LessonStage,
  PublishedTask,
  RecordedStep,
  RunMode,
  StudentTask
} from '../domain/models';
import {
  dataPrepareApi,
  type BusinessModule,
  type ModuleDataStrategy,
  type TeachingDataTemplate,
  type TeachingDataPool
} from './trainingApi';
import {
  isPracticeMonitorableStep,
  practiceRecordedActionType
} from '../utils/practiceStep';

export interface ReportedStepBinding {
  eventId: string;
  resourceSnapshotId: string;
  draftId: string;
}

export interface PublishedLessonBinding {
  teachingPointId: string;
  finishedCaptureSession: boolean;
  resourceIdsByStepId: Record<string, string>;
}

export interface PublishTeachingTaskOptions {
  title: string;
  startAt: string;
  endAt: string;
  timeLimitMinutes?: number;
  publishOrgId?: string;
}

export interface PublishedTeachingTaskBinding {
  courseId: string;
  taskId: string;
  teachingPointId: string;
  evaluationRuleId: string;
  taskStepIdsByStepId: Record<string, string>;
}

export interface StartedTaskExecutionBinding {
  executionId: string;
  executionStatus: string;
  contextLoaded: boolean;
}

export interface PracticeActionEvidence {
  actionType: 'click' | 'input' | 'select' | 'submit';
  selector: string;
  selectorCandidates?: string[];
  url?: string;
  pageTitle?: string;
  completedAt: string;
}

export interface ReportedPracticeStepBinding {
  clientTraceId: string;
  traceId: string;
}

export const MAX_RECORDED_STEP_SNAPSHOT_JSON_LENGTH = 4 * 1024 * 1024;

export interface BackendTrainingApi {
  isEnabled(): boolean;
  listBusinessPlatforms(
    cachedPlatforms: BusinessPlatform[]
  ): Promise<BusinessPlatform[]>;
  createBusinessPlatform(
    input: Pick<BusinessPlatform, 'code' | 'name' | 'baseUrl'> &
      Partial<Pick<BusinessPlatform, 'description' | 'status'>>
  ): Promise<BusinessPlatform>;
  updateBusinessPlatform(platform: BusinessPlatform): Promise<BusinessPlatform>;
  setBusinessPlatformStatus(
    platform: BusinessPlatform,
    status: BusinessPlatform['status']
  ): Promise<BusinessPlatform>;
  startCaptureSession(
    lesson: LessonPlan,
    platform: BusinessPlatform,
    startUrl: string
  ): Promise<string>;
  reportRecordedStep(
    lesson: LessonPlan,
    stage: LessonStage,
    step: RecordedStep,
    sequenceNo: number
  ): Promise<ReportedStepBinding>;
  publishLesson(
    lesson: LessonPlan,
    platform: BusinessPlatform
  ): Promise<PublishedLessonBinding>;
  publishTeachingTask(
    lesson: LessonPlan,
    platform: BusinessPlatform,
    mode: RunMode,
    options: PublishTeachingTaskOptions
  ): Promise<PublishedTeachingTaskBinding>;
  prepareInitialDataForPublishedTask(
    lesson: LessonPlan,
    platform: BusinessPlatform,
    publishedTask: PublishedTask,
    studentTasks: StudentTask[]
  ): Promise<number>;
  startStudentTaskExecution(
    lesson: LessonPlan,
    platform: BusinessPlatform,
    publishedTask: PublishedTask,
    studentTask: StudentTask
  ): Promise<StartedTaskExecutionBinding>;
  reportStudentStageCompletion(
    lesson: LessonPlan,
    stage: LessonStage,
    publishedTask: PublishedTask,
    studentTask: StudentTask
  ): Promise<void>;
  reportStudentPracticeStep(
    lesson: LessonPlan,
    stage: LessonStage,
    step: RecordedStep,
    publishedTask: PublishedTask,
    studentTask: StudentTask,
    evidence: PracticeActionEvidence
  ): Promise<ReportedPracticeStepBinding>;
  submitStudentTaskExecution(
    studentTask: StudentTask
  ): Promise<TaskExecution>;
}

export const backendTrainingApi: BackendTrainingApi = {
  isEnabled() {
    return getApiConfig().enabled;
  },

  async listBusinessPlatforms(cachedPlatforms) {
    const config = getApiConfig();
    const systems = await connectorApi.listSystems(config.tenantId);
    const missingEmbeddedPlatforms = cachedPlatforms.filter(
      (platform) =>
        platform.baseUrl.startsWith('internal://') &&
        !systems.some(
          (system) =>
            system.id === platform.id ||
            normalizeCode(system.systemCode) === normalizeCode(platform.code)
        )
    );

    for (const platform of missingEmbeddedPlatforms) {
      let created: ConnectorSystem | undefined;
      try {
        created = await connectorApi.createSystem({
          tenantId: config.tenantId,
          systemCode: normalizeCode(platform.code),
          systemName: platform.name,
          systemType: 'LOCAL_DEMO',
          baseUrl: platform.baseUrl,
          authType: 'NONE',
          configJson: serializePlatformConfig(platform)
        });
      } catch (error) {
        // A second browser tab may have provisioned the same unique platform
        // between the list and create calls. Re-read before surfacing the error.
        const refreshed = await connectorApi.listSystems(config.tenantId);
        created = refreshed.find(
          (system) =>
            normalizeCode(system.systemCode) === normalizeCode(platform.code)
        );
        if (!created) throw error;
      }
      systems.push(created);
      if (platform.status === 'DISABLED' && created.status === 'ACTIVE') {
        const disabled = await connectorApi.disableSystem(created.id);
        systems.splice(systems.indexOf(created), 1, disabled);
      }
    }

    return Promise.all(
      systems.map(async (system) => {
        const cached = cachedPlatforms.find(
          (platform) =>
            platform.id === system.id ||
            normalizeCode(platform.code) === normalizeCode(system.systemCode)
        );
        const persistedModules = await dataPrepareApi.listAllBusinessModules({
          tenantId: config.tenantId,
          connectorSystemId: system.id
        });
        return mapConnectorSystem(system, cached, persistedModules);
      })
    );
  },

  async createBusinessPlatform(input) {
    const config = getApiConfig();
    const system = await connectorApi.createSystem({
      tenantId: config.tenantId,
      systemCode: input.code.trim().toUpperCase(),
      systemName: input.name.trim(),
      systemType: 'BUSINESS',
      baseUrl: input.baseUrl.trim(),
      authType: 'NONE',
      configJson: JSON.stringify({
        description: input.description?.trim() ?? '',
        modules: []
      })
    });
    const platform = mapConnectorSystem(system);
    if (input.status === 'DISABLED' && platform.status === 'ENABLED') {
      return mapConnectorSystem(await connectorApi.disableSystem(platform.id));
    }
    return platform;
  },

  async updateBusinessPlatform(platform) {
    const updated = await connectorApi.updateSystem({
      id: platform.id,
      systemName: platform.name,
      systemType: 'BUSINESS',
      baseUrl: platform.baseUrl,
      authType: 'NONE',
      configJson: serializePlatformConfig(platform)
    });
    let mapped = mapConnectorSystem(updated, platform);
    if (mapped.status !== platform.status) {
      mapped = mapConnectorSystem(
        platform.status === 'ENABLED'
          ? await connectorApi.enableSystem(platform.id)
          : await connectorApi.disableSystem(platform.id),
        platform
      );
    }
    return mapped;
  },

  async setBusinessPlatformStatus(platform, status) {
    const updated =
      status === 'ENABLED'
        ? await connectorApi.enableSystem(platform.id)
        : await connectorApi.disableSystem(platform.id);
    return mapConnectorSystem(updated, platform);
  },

  async startCaptureSession(lesson, platform, startUrl) {
    const config = getApiConfig();
    const session = await captureApi.createSession({
      tenantId: config.tenantId,
      connectorSystemId: platform.id,
      teacherId: config.currentUserId,
      sessionName: `${lesson.title} - 备案采集`,
      businessName: lesson.moduleName,
      captureMode: 'STANDARD',
      startUrl
    });
    return session.id;
  },

  async reportRecordedStep(lesson, stage, step, sequenceNo) {
    if (!lesson.captureSessionId) {
      throw new Error('尚未创建后端采集会话，请先点击“开始录制”');
    }
    const config = getApiConfig();
    const eventType = mapStepActionType(step);
    const event = await captureApi.reportEvent({
      tenantId: config.tenantId,
      captureSessionId: lesson.captureSessionId,
      sdkSessionId: `web-${lesson.id}`,
      clientEventId: step.id,
      eventType,
      eventTime: toLocalDateTime(new Date()),
      sequenceNo,
      retryCount: 0,
      pageUrl: step.url,
      targetText: step.actionLabel,
      targetLocator: step.selector,
      targetStableKey: step.selectorCandidates?.[0] ?? step.selector,
      inputValueMasked: step.valueMasked,
      eventPayloadJson: JSON.stringify({
        lessonId: lesson.id,
        stageId: stage.id,
        title: step.title,
        note: step.note,
        kind: step.kind ?? 'action'
      })
    });

    const snapshot = await captureApi.reportResourceSnapshot({
      tenantId: config.tenantId,
      captureSessionId: lesson.captureSessionId,
      pageUrl: step.pageSnapshot?.pageUrl ?? step.url ?? '',
      pageTitle: step.pageSnapshot?.pageTitle ?? step.pageTitle,
      resourceType: step.pageSnapshot
        ? 'PAGE_STATE'
        : step.kind === 'guide'
          ? 'PAGE_GUIDE'
          : 'ELEMENT',
      snapshotScope: step.pageSnapshot ? 'FULL_PAGE' : 'KEY_ELEMENT',
      resourceName: step.title,
      resourceLocator: step.selector,
      elementSnapshotJson: serializeRecordedStepSnapshot(step),
      metadataJson: JSON.stringify({
        actionType: step.actionType,
        required: step.required,
        failurePolicy: step.failurePolicy
      })
    });

    const draft = await captureApi.createActionDraft({
      tenantId: config.tenantId,
      captureSessionId: lesson.captureSessionId,
      eventId: event.id,
      actionName: step.title,
      actionType: eventType,
      sequenceNo,
      suggestedOperationName: step.actionLabel,
      guideContent: step.teachingText || step.note,
      practiceHint: step.practiceHint,
      confirmStatus: 'PENDING'
    });

    return {
      eventId: event.id,
      resourceSnapshotId: snapshot.id,
      draftId: draft.id
    };
  },

  async publishLesson(lesson, platform) {
    const config = getApiConfig();
    const resourceIdsByStepId: Record<string, string> = {};
    for (const stage of lesson.stages) {
      for (const step of stage.recordedSteps) {
        if (!step.remoteDraftId || step.remoteResourceId) continue;
        const resource = await connectorApi.createResource({
          tenantId: config.tenantId,
          connectorSystemId: platform.id,
          resourceCode: safeCode(`${lesson.code}-${step.id}`, 64),
          resourceName: step.title,
          resourceType: step.kind === 'guide' ? 'PAGE_GUIDE' : 'ELEMENT',
          pageUrl: step.url,
          locator: step.selector,
          stableKey: step.selectorCandidates?.[0] ?? step.selector,
          metadataJson: JSON.stringify({
            lessonId: lesson.id,
            stageId: stage.id,
            actionType: step.actionType,
            rect: step.rect
          }),
          sourceCaptureId: lesson.captureSessionId,
          createBy: config.currentUserId
        });
        resourceIdsByStepId[step.id] = resource.id;
        await captureApi.confirmActionDraft(step.remoteDraftId, {
          confirmedOperationName: step.actionLabel,
          confirmedStepName: step.title,
          guideContent: step.teachingText || step.note,
          practiceHint: step.practiceHint,
          connectorResourceId: resource.id,
          updateBy: config.currentUserId
        });
      }
    }

    const point = await teachingApi.createPoint({
      tenantId: config.tenantId,
      connectorSystemId: platform.id,
      pointCode: safeCode(lesson.code, 64),
      pointName: lesson.title,
      pointType: 'SCENARIO',
      sourceCaptureSessionId: lesson.captureSessionId,
      businessOverviewJson: JSON.stringify({
        description: lesson.description,
        moduleName: lesson.moduleName,
        tags: lesson.tags,
        objectiveMaxScore: lesson.objectiveMaxScore,
        subjectiveMaxScore: lesson.subjectiveMaxScore
      }),
      recordPathJson: JSON.stringify(
        lesson.stages.map((stage, stageIndex) => ({
          segmentNo: stageIndex + 1,
          stageId: stage.id,
          stageKey: stage.stageKey,
          stageName: stage.name,
          actorType: stage.groupKey,
          steps: stage.recordedSteps.map((step, stepIndex) => ({
            stepId: step.id,
            sequenceNo: stepIndex + 1,
            title: step.title,
            url: step.url,
            resourceId: resourceIdsByStepId[step.id]
          }))
        }))
      ),
      executionStrategy: 'ROLE_SWITCH',
      overlayPolicyJson: JSON.stringify({
        learning: true,
        practice: true,
        exam: true
      }),
      description: lesson.description,
      createBy: config.currentUserId
    });

    let finishedCaptureSession = false;
    if (lesson.captureSessionId && !lesson.captureSessionFinished) {
      await captureApi.finishSession(lesson.captureSessionId);
      finishedCaptureSession = true;
    }
    return {
      teachingPointId: point.id,
      finishedCaptureSession,
      resourceIdsByStepId
    };
  },

  async publishTeachingTask(lesson, platform, mode, options) {
    if (!lesson.teachingPointId) {
      throw new Error('请先完成教案备案，生成后端教学点后再发布任务');
    }
    const config = getApiConfig();
    const publishOrgId = await ensureSimulatedPublishOrg(
      options.publishOrgId ?? config.simulatedOrgId
    );
    const course = await ensureCourse(lesson, publishOrgId);
    const task = await ensureTeachingTask(
      lesson,
      course,
      mode,
      options,
      publishOrgId
    );
    await ensureTaskTeachingPoint(task.id, lesson.teachingPointId);
    const taskStepIdsByStepId = await ensureTaskSteps(
      lesson,
      task,
      lesson.teachingPointId,
      mode
    );
    const evaluationRule = await ensureEvaluation(
      lesson,
      task,
      lesson.teachingPointId,
      mode,
      taskStepIdsByStepId
    );
    await courseApi.publishTaskTeachingPoint(
      config.tenantId,
      task.id,
      lesson.teachingPointId,
      evaluationRule.id
    );
    return {
      courseId: course.id,
      taskId: task.id,
      teachingPointId: lesson.teachingPointId,
      evaluationRuleId: evaluationRule.id,
      taskStepIdsByStepId
    };
  },

  async prepareInitialDataForPublishedTask(
    lesson,
    platform,
    publishedTask,
    studentTasks
  ) {
    if (publishedTask.mode === 'LEARNING') return 0;
    if (!publishedTask.remoteTaskId) {
      throw new Error('后端任务尚未发布成功，不能自动准备初始数据');
    }
    const config = getApiConfig();
    const module = await resolvePublishedBusinessModule(lesson, platform);
    const strategies = await dataPrepareApi.listActiveStrategies({
      tenantId: config.tenantId,
      connectorSystemId: platform.id,
      businessModuleId: module.id
    });
    const strategy = strategies.find(
      (candidate) => candidate.sceneType === publishedTask.mode
    );
    if (!strategy) {
      throw new Error('当前业务模块没有启用匹配发布场景的数据准备策略');
    }
    const templates = await dataPrepareApi.listActiveTemplatesByModuleScene({
      tenantId: config.tenantId,
      connectorSystemId: platform.id,
      moduleCode: module.moduleCode,
      sceneType: publishedTask.mode
    });
    // 管理端重新创建模板时会停用旧模板，已有启用策略可能仍保留旧模板 ID。
    // 发布时优先使用策略当前绑定；绑定已失效时回退到同模块、同场景的启用模板，
    // 避免数据配置已具备但任务仍无法发布。
    const template =
      templates.find((candidate) => candidate.id === strategy.templateId) ??
      templates[0];
    if (!template) {
      throw new Error('当前数据准备策略引用的初始数据模板不存在或已停用');
    }
    const publishAttemptId = Date.now();
    const requirement = await dataPrepareApi.createRequirement({
      tenantId: config.tenantId,
      requirementCode: safeCode(
        `REQ-${publishedTask.remoteTaskId}-${publishedTask.mode}-${publishAttemptId}`,
        64
      ),
      connectorSystemId: platform.id,
      businessModuleId: module.id,
      moduleCode: module.moduleCode,
      strategyId: strategy.id,
      templateId: template.id,
      taskId: publishedTask.remoteTaskId,
      classId: config.simulatedOrgId,
      sceneType: publishedTask.mode,
      createBy: config.currentUserId,
      updateBy: config.currentUserId
    });
    const requestBatchId = `publish-${publishedTask.remoteTaskId}-${publishAttemptId}`;
    await dataPrepareApi.prepareAndExecute({
      triggerType: 'PUBLISH',
      idempotencyKey: `publish:${requirement.id}:${requestBatchId}`,
      requestJson: JSON.stringify(
        buildPublishedDataPrepareSnapshot(
          config.tenantId,
          config.currentUserId,
          lesson,
          platform,
          module,
          template,
          strategy,
          publishedTask,
          studentTasks
        )
      ),
      generateRequest: {
        requirementId: requirement.id,
        requestBatchId,
        createBy: config.currentUserId,
        updateBy: config.currentUserId,
        participants: studentTasks.map((studentTask) =>
          buildPublishDataParticipant(lesson, module, publishedTask, studentTask, requestBatchId)
        )
      }
    });
    const pools = await dataPrepareApi.listPools({
      tenantId: config.tenantId,
      requirementId: requirement.id
    });
    let allocatedCount = 0;
    for (const studentTask of studentTasks) {
      const pool = resolvePoolForStudentTask(pools, studentTask);
      const questionId = publishedDataQuestionId(studentTask);
      await dataPrepareApi.acquireDataInstance({
        tenantId: config.tenantId,
        poolId: pool.id,
        ownerUserId: studentTask.studentId,
        taskId: publishedTask.remoteTaskId,
        allocationScene: publishedTask.mode,
        attemptId: safeCode(requestBatchId, 64),
        questionAttemptId: safeCode(`${questionId}-${requestBatchId}`, 64),
        createBy: config.currentUserId,
        updateBy: config.currentUserId
      });
      allocatedCount += 1;
    }
    return allocatedCount;
  },

  async startStudentTaskExecution(lesson, platform, publishedTask, studentTask) {
    if (!publishedTask.remoteTaskId) {
      throw new Error('该学生任务尚未同步到后端，不能开始执行');
    }
    const config = getApiConfig();
    const execution = await executionApi.start({
      tenantId: config.tenantId,
      taskId: publishedTask.remoteTaskId,
      connectorSystemId: platform.id,
      executionMode: publishedTask.mode,
      sdkMode: publishedTask.mode,
      executionIdentityJson: JSON.stringify({
        simulated: true,
        localStudentId: studentTask.studentId,
        localStudentName: studentTask.studentName,
        localGroupKeys: studentTask.groupKeys,
        authenticatedUserId: config.currentUserId
      })
    });
    await executionApi.context(
      publishedTask.mode,
      config.tenantId,
      execution.id,
      publishedTask.remoteTaskId
    );
    return {
      executionId: execution.id,
      executionStatus: execution.executionStatus,
      contextLoaded: true
    };
  },

  async reportStudentStageCompletion(
    lesson,
    stage,
    publishedTask,
    studentTask
  ) {
    if (!studentTask.remoteExecutionId) {
      throw new Error('后端执行记录尚未创建，请先开始任务');
    }
    const config = getApiConfig();
    const allSteps = lesson.stages.flatMap((candidate) =>
      candidate.recordedSteps.map((step) => ({ stage: candidate, step }))
    );
    for (const step of stage.recordedSteps) {
      const taskStepId =
        publishedTask.remoteTaskStepIdsByStepId?.[step.id];
      if (!taskStepId) {
        throw new Error(`步骤“${step.title}”尚未生成后端任务步骤`);
      }
      const sequenceNo =
        allSteps.findIndex((candidate) => candidate.step.id === step.id) + 1;
      await executionApi.reportTrace({
        tenantId: config.tenantId,
        executionId: studentTask.remoteExecutionId,
        sdkSessionId: `web-${studentTask.remoteExecutionId}`,
        clientTraceId: `${studentTask.remoteExecutionId}:${taskStepId}:completed`,
        taskStepId,
        teachingPointId: publishedTask.remoteTeachingPointId,
        resourceId: step.remoteResourceId,
        traceType: 'STEP_COMPLETED',
        traceTime: toLocalDateTime(new Date()),
        sequenceNo,
        retryCount: 0,
        outputDataJson: JSON.stringify({
          lessonId: lesson.id,
          stageId: stage.id,
          localStepId: step.id,
          mode: publishedTask.mode
        }),
        evidenceJson: JSON.stringify({
          source: 'sxpt_web',
          simulatedIdentity: true
        }),
        success: true
      });
    }
  },

  async reportStudentPracticeStep(
    lesson,
    stage,
    step,
    publishedTask,
    studentTask,
    evidence
  ) {
    if (!studentTask.remoteExecutionId) {
      throw new Error('后端执行记录尚未创建，请先开始练习');
    }
    const taskStepId = publishedTask.remoteTaskStepIdsByStepId?.[step.id];
    if (!taskStepId) {
      throw new Error(`操作点“${step.title}”尚未生成后端任务步骤`);
    }
    const allSteps = lesson.stages.flatMap((candidate) =>
      candidate.recordedSteps.map((candidateStep) => candidateStep)
    );
    const sequenceNo =
      Math.max(0, allSteps.findIndex((candidate) => candidate.id === step.id)) + 1;
    const config = getApiConfig();
    const clientTraceId = `${studentTask.remoteExecutionId}:${taskStepId}:practice-completed`;
    const trace = await executionApi.reportTrace({
      tenantId: config.tenantId,
      executionId: studentTask.remoteExecutionId,
      sdkSessionId: `web-${studentTask.remoteExecutionId}`,
      clientTraceId,
      taskStepId,
      teachingPointId: publishedTask.remoteTeachingPointId,
      resourceId: step.remoteResourceId,
      traceType: mapPracticeStepActionType(step),
      traceTime: toLocalDateTime(new Date(evidence.completedAt)),
      sequenceNo,
      retryCount: 0,
      outputDataJson: JSON.stringify({
        lessonId: lesson.id,
        stageId: stage.id,
        localStepId: step.id,
        mode: publishedTask.mode,
        recordedActionType: practiceRecordedActionType(step),
        observedActionType: evidence.actionType
      }),
      evidenceJson: JSON.stringify({
        source: 'sxpt_web_practice_monitor',
        matchStrategy: 'recorded_element_selector',
        observedSelector: evidence.selector,
        observedSelectorCandidates: evidence.selectorCandidates ?? [],
        observedUrl: evidence.url,
        observedPageTitle: evidence.pageTitle
      }),
      success: true
    });
    return {
      clientTraceId,
      traceId: trace.id
    };
  },

  async submitStudentTaskExecution(studentTask) {
    if (!studentTask.remoteExecutionId) {
      throw new Error('后端执行记录尚未创建，不能提交任务');
    }
    return executionApi.submit(
      getApiConfig().tenantId,
      studentTask.remoteExecutionId
    );
  }
};

/**
 * 将录制步骤转换为后端资源快照，并与后端 4 MB 安全上限保持一致。
 */
export function serializeRecordedStepSnapshot(step: RecordedStep) {
  const snapshotJson = JSON.stringify({
    selector: step.selector,
    selectorCandidates: step.selectorCandidates ?? [],
    rect: step.rect,
    viewport: step.recordedViewport,
    pageSnapshot: step.pageSnapshot
  });
  if (snapshotJson.length > MAX_RECORDED_STEP_SNAPSHOT_JSON_LENGTH) {
    throw new Error(
      `业务页面快照过大（${snapshotJson.length} 字符），请减少页面内容后重新录制`
    );
  }
  return snapshotJson;
}

async function ensureSimulatedPublishOrg(preferredIdOrCode: string) {
  const config = getApiConfig();
  const preferredCode = safeCode(preferredIdOrCode || 'DEMO-CLASS', 64);
  const organizations = await usersApi.listOrgs(config.tenantId);
  const existing = findOrganizationByIdOrCode(
    organizations,
    preferredIdOrCode,
    preferredCode
  );
  if (existing) return existing.id;
  try {
    const created = await usersApi.createOrg({
      tenantId: config.tenantId,
      orgCode: preferredCode,
      orgName: '模拟班级',
      orgType: 'CLASS'
    });
    return created.id;
  } catch (error) {
    // 多标签页或并发发布可能在“查询后、创建前”写入同一组织。
    // 重新查询并复用可保证学习/练习任务发布具有幂等性。
    const refreshed = await usersApi.listOrgs(config.tenantId);
    const concurrentlyCreated = findOrganizationByIdOrCode(
      refreshed,
      preferredIdOrCode,
      preferredCode
    );
    if (!concurrentlyCreated) throw error;
    return concurrentlyCreated.id;
  }
}

export function findOrganizationByIdOrCode<
  T extends { id: string; orgCode: string }
>(
  organizations: T[],
  preferredIdOrCode: string,
  normalizedPreferredCode = normalizeCode(preferredIdOrCode)
): T | undefined {
  return organizations.find(
    (organization) =>
      organization.id === preferredIdOrCode ||
      normalizeCode(organization.orgCode) ===
        normalizeCode(normalizedPreferredCode)
  );
}

export function mapConnectorSystem(
  system: ConnectorSystem,
  cached?: BusinessPlatform,
  persistedModules: BusinessModule[] = []
): BusinessPlatform {
  const remoteModuleKeys = new Set(
    persistedModules.flatMap((businessModule) => [
      businessModule.id,
      normalizeCode(businessModule.moduleCode)
    ])
  );
  const modules = [
    ...persistedModules.map(mapBusinessModule),
    ...(cached?.modules ?? []).filter(
      (businessModule) =>
        !remoteModuleKeys.has(businessModule.id) &&
        !remoteModuleKeys.has(normalizeCode(businessModule.code))
    )
  ];
  return {
    id: system.id,
    code: system.systemCode,
    name: system.systemName,
    baseUrl: system.baseUrl,
    description: cached?.description ?? '',
    status: system.status === 'ACTIVE' ? 'ENABLED' : 'DISABLED',
    modules: clone(modules),
    updatedAt: system.updateTime ?? system.createTime ?? new Date().toISOString()
  };
}

export function mapBusinessModule(
  businessModule: BusinessModule
): BusinessPlatformModule {
  return {
    id: businessModule.id,
    code: businessModule.moduleCode,
    name: businessModule.moduleName,
    path: businessModule.entryUrl,
    description: businessModule.remark ?? '',
    status: businessModule.status === 'ACTIVE' ? 'ENABLED' : 'DISABLED',
    updatedAt:
      businessModule.updateTime ??
      businessModule.createTime ??
      new Date().toISOString()
  };
}

export function mapStepActionType(step: RecordedStep): string {
  switch (step.actionType) {
    case 'input':
      return 'INPUT';
    case 'select':
      return 'SELECT';
    case 'guide':
      return 'VERIFY_STATE';
    case 'submit':
    case 'click':
    default:
      return 'CLICK';
  }
}

export function mapPracticeStepActionType(step: RecordedStep): string {
  const actionType = practiceRecordedActionType(step);
  if (actionType === 'input') return 'INPUT';
  if (actionType === 'select') return 'SELECT';
  return 'CLICK';
}

export function toLocalDateTime(date: Date): string {
  return date.toISOString().slice(0, 19);
}

function serializePlatformConfig(platform: BusinessPlatform): string {
  return JSON.stringify({
    description: platform.description,
    modules: platform.modules
  });
}

export function safeCode(value: string, maxLength: number): string {
  const normalized = value
    .trim()
    .toUpperCase()
    .replace(/[^A-Z0-9_-]+/g, '_')
    .replace(/^_+|_+$/g, '');
  const fallback = normalized || `RESOURCE_${Date.now()}`;
  if (fallback.length <= maxLength) return fallback;

  // 教案节点 ID 的差异通常位于时间戳尾部。直接截断会让多个资源、
  // task_step 和 evaluation_item 共用同一业务编码，进而丢失轨迹与分数。
  const hash = stableCodeHash(fallback);
  if (maxLength <= hash.length) return hash.slice(0, maxLength);
  const prefixLength = maxLength - hash.length - 1;
  const prefix =
    fallback.slice(0, prefixLength).replace(/[-_]+$/g, '') || 'CODE';
  return `${prefix}-${hash}`.slice(0, maxLength);
}

function stableCodeHash(value: string): string {
  let hash = 0x811c9dc5;
  for (let index = 0; index < value.length; index += 1) {
    hash ^= value.charCodeAt(index);
    hash = Math.imul(hash, 0x01000193);
  }
  return (hash >>> 0).toString(16).toUpperCase().padStart(8, '0');
}

function normalizeCode(value: string): string {
  return value.trim().toUpperCase();
}

async function ensureCourse(
  lesson: LessonPlan,
  publishOrgId: string
): Promise<Course> {
  const config = getApiConfig();
  const courseCode = safeCode(`COURSE-${lesson.code}`, 64);
  const existing = (
    await courseApi.listCourses(config.tenantId, publishOrgId)
  ).find(
    (course) =>
      course.id === lesson.remoteCourseId ||
      course.courseCode === courseCode
  );
  if (existing) return existing;
  return courseApi.createCourse({
    tenantId: config.tenantId,
    courseCode,
    courseName: lesson.title,
    targetOrgId: publishOrgId,
    description: lesson.description,
    createBy: config.currentUserId
  });
}

async function ensureTeachingTask(
  lesson: LessonPlan,
  course: Course,
  mode: RunMode,
  options: PublishTeachingTaskOptions,
  publishOrgId: string
): Promise<TeachingTask> {
  const config = getApiConfig();
  const taskCode = safeCode(`${lesson.code}-${mode}`, 64);
  const existing = (
    await courseApi.listTasks(config.tenantId, course.id, publishOrgId)
  ).find((task) => task.taskCode === taskCode);
  if (existing) return existing;
  return courseApi.createTask({
    tenantId: config.tenantId,
    courseId: course.id,
    publishOrgId,
    taskCode,
    taskName: options.title,
    taskType: mode,
    taskGoal:
      mode === 'LEARNING'
        ? '按备案流程完整观看并理解教师讲解'
        : mode === 'PRACTICE'
          ? '按备案步骤完成一次业务流程练习'
          : '在考试规则下独立完成业务流程',
    taskDescription: lesson.description,
    startTime: toLocalDateTime(new Date(options.startAt)),
    endTime: toLocalDateTime(new Date(options.endAt)),
    timeLimitMinutes: options.timeLimitMinutes,
    overlayPolicyJson: JSON.stringify({
      showGuide: mode === 'LEARNING',
      showPracticeHint: mode === 'PRACTICE',
      silentCapture: mode === 'EXAM'
    }),
    createBy: config.currentUserId
  });
}

async function ensureTaskTeachingPoint(
  taskId: string,
  teachingPointId: string
): Promise<void> {
  const config = getApiConfig();
  const bindings = await courseApi.listTaskTeachingPoints(
    config.tenantId,
    taskId
  );
  if (bindings.some((binding) => binding.teachingPointId === teachingPointId)) {
    return;
  }
  await courseApi.createTaskTeachingPoint({
    tenantId: config.tenantId,
    taskId,
    teachingPointId,
    requiredFlag: true,
    sequenceNo: 1,
    createBy: config.currentUserId
  });
}

async function ensureTaskSteps(
  lesson: LessonPlan,
  task: TeachingTask,
  teachingPointId: string,
  mode: RunMode
): Promise<Record<string, string>> {
  const config = getApiConfig();
  const existing = await teachingApi.listTaskSteps(
    config.tenantId,
    task.id,
    teachingPointId
  );
  const result: Record<string, string> = {};
  let sequenceNo = 0;
  for (let stageIndex = 0; stageIndex < lesson.stages.length; stageIndex += 1) {
    const stage = lesson.stages[stageIndex];
    if (!stage.visibility[mode]) continue;
    for (const step of stage.recordedSteps) {
      if (
        mode === 'PRACTICE' &&
        !isPracticeMonitorableStep(step)
      ) {
        continue;
      }
      sequenceNo += 1;
      const stepCode = safeCode(`${mode}-${step.id}`, 64);
      let remoteStep = existing.find(
        (candidate) => candidate.stepCode === stepCode
      );
      if (!remoteStep) {
        remoteStep = await teachingApi.createTaskStep({
          tenantId: config.tenantId,
          taskId: task.id,
          teachingPointId,
          stepCode,
          stepName: step.title,
          stepDescription: step.note,
          sequenceNo,
          segmentNo: stageIndex + 1,
          relatedResourceIds: step.remoteResourceId
            ? JSON.stringify([step.remoteResourceId])
            : undefined,
          guideContent: step.teachingText || step.note,
          practiceHint: step.practiceHint,
          required: step.required ?? stage.required,
          allowSkip: !(step.required ?? stage.required),
          sourceActionDraftId: step.remoteDraftId,
          createBy: config.currentUserId
        });
        existing.push(remoteStep);
      }
      result[step.id] = remoteStep.id;
    }
  }
  if (!Object.keys(result).length) {
    throw new Error(`教案没有可用于 ${mode} 模式的备案步骤`);
  }
  return result;
}

async function ensureEvaluation(
  lesson: LessonPlan,
  task: TeachingTask,
  teachingPointId: string,
  mode: RunMode,
  taskStepIdsByStepId: Record<string, string>
): Promise<EvaluationRule> {
  const config = getApiConfig();
  const ruleCode = safeCode(`${task.taskCode}-AUTO`, 64);
  const rules = await evaluationApi.listRules(
    config.tenantId,
    task.id,
    teachingPointId
  );
  let rule = rules.find((candidate) => candidate.ruleCode === ruleCode);
  const totalScore = lesson.objectiveMaxScore > 0
    ? lesson.objectiveMaxScore
    : 100;
  if (!rule) {
    rule = await evaluationApi.createRule({
      tenantId: config.tenantId,
      ruleCode,
      ruleName: `${task.taskName}自动评分`,
      taskId: task.id,
      teachingPointId,
      totalScore,
      description: '由教案备案步骤自动生成的流程轨迹评分规则',
      createBy: config.currentUserId
    });
  }
  const existingItems = await evaluationApi.listItems(
    config.tenantId,
    rule.id
  );
  const steps = lesson.stages.flatMap((stage) =>
    stage.visibility[mode]
      ? stage.recordedSteps
          .filter(
            (step) =>
              mode !== 'PRACTICE' ||
              isPracticeMonitorableStep(step)
          )
          .map((step) => ({ stage, step }))
      : []
  );
  for (let index = 0; index < steps.length; index += 1) {
    const { step } = steps[index];
    const taskStepId = taskStepIdsByStepId[step.id];
    const itemCode = safeCode(`${mode}-${step.id}-TRACE`, 64);
    if (existingItems.some((item) => item.itemCode === itemCode)) continue;
    const item = await evaluationApi.createItem({
      tenantId: config.tenantId,
      evaluationRuleId: rule.id,
      teachingPointId,
      itemCode,
      itemName: `${step.title}完成`,
      itemType: 'PROCESS',
      relatedResourceId: step.remoteResourceId,
      relatedTaskStepId: taskStepId,
      score: distributedScore(totalScore, steps.length, index),
      required: step.required ?? true,
      assertionType: 'TRACE_EXISTS',
      assertionConfigJson: JSON.stringify({
        traceType: 'STEP_COMPLETED',
        taskStepId,
        success: true
      }),
      failPolicy: 'NO_SCORE',
      createBy: config.currentUserId
    });
    existingItems.push(item);
  }
  return rule;
}

/**
 * 业务功能：构造发布时的数据准备快照。
 * 关键流程：把原平台、业务模块、初始模板、生成策略、教学任务和学生范围固化到 requestJson，后续即使管理员修改配置，
 * 也能解释“本批数据为什么按当时这套规则生成”。
 */
export function buildPublishedDataPrepareSnapshot(
  tenantId: string,
  operatorId: string,
  lesson: LessonPlan,
  platform: BusinessPlatform,
  module: BusinessModule,
  template: TeachingDataTemplate,
  strategy: ModuleDataStrategy,
  publishedTask: PublishedTask,
  studentTasks: StudentTask[]
) {
  const uniqueStudents = new Map<string, StudentTask>();
  for (const task of studentTasks) {
    if (!uniqueStudents.has(task.studentId)) {
      uniqueStudents.set(task.studentId, task);
    }
  }
  return {
    source: 'publishTeachingTask',
    snapshotVersion: 1,
    snapshotAt: new Date().toISOString(),
    tenantId,
    operatorId,
    platform: {
      id: platform.id,
      code: platform.code,
      name: platform.name,
      baseUrl: platform.baseUrl,
      status: platform.status
    },
    module: {
      id: module.id,
      code: module.moduleCode,
      name: module.moduleName,
      entryUrl: module.entryUrl,
      status: module.status
    },
    template: {
      id: template.id,
      code: template.templateCode,
      name: template.templateName,
      sceneType: template.sceneType,
      moduleCode: template.moduleCode,
      initState: template.initState,
      supportMode: template.supportMode,
      configJson: template.configJson,
      requestSchemaJson: template.requestSchemaJson,
      requiredOrgRoleJson: template.requiredOrgRoleJson,
      resultCheckSchemaJson: template.resultCheckSchemaJson,
      sensitiveFieldPolicyJson: template.sensitiveFieldPolicyJson,
      updateTime: template.updateTime
    },
    strategy: {
      id: strategy.id,
      code: strategy.strategyCode,
      version: strategy.strategyVersion,
      sceneType: strategy.sceneType,
      templateId: strategy.templateId,
      dataSourceStrategy: strategy.dataSourceStrategy,
      prepareTiming: strategy.prepareTiming,
      sharePolicy: strategy.sharePolicy,
      regeneratePolicy: strategy.regeneratePolicy,
      lockPolicy: strategy.lockPolicy,
      validationPolicyJson: strategy.validationPolicyJson,
      poolSizePolicyJson: strategy.poolSizePolicyJson,
      defaultOrgRolePolicyJson: strategy.defaultOrgRolePolicyJson,
      updateTime: strategy.updateTime
    },
    lesson: {
      id: lesson.id,
      code: lesson.code,
      title: lesson.title,
      version: lesson.version,
      teachingPointId: lesson.teachingPointId,
      objectiveMaxScore: lesson.objectiveMaxScore,
      subjectiveMaxScore: lesson.subjectiveMaxScore,
      stageCount: lesson.stages.length,
      stageSnapshot: lesson.stages.map((stage) => ({
        id: stage.id,
        name: stage.name,
        groupKey: stage.groupKey,
        visible: stage.visibility[publishedTask.mode],
        stepCount: stage.recordedSteps.length
      }))
    },
    publishedTask: {
      localPublishedTaskId: publishedTask.id,
      remoteTaskId: publishedTask.remoteTaskId,
      remoteCourseId: publishedTask.remoteCourseId,
      remoteTeachingPointId: publishedTask.remoteTeachingPointId,
      remoteEvaluationRuleId: publishedTask.remoteEvaluationRuleId,
      mode: publishedTask.mode,
      title: publishedTask.title,
      startAt: publishedTask.startAt,
      endAt: publishedTask.endAt,
      assignedCount: publishedTask.assignedCount,
      groupCount: publishedTask.groupCount
    },
    studentScope: {
      totalTaskCount: studentTasks.length,
      uniqueStudentCount: uniqueStudents.size,
      unitCount: new Set(studentTasks.map((task) => task.unitId)).size,
      groupKeys: Array.from(
        new Set(studentTasks.flatMap((task) => task.groupKeys))
      ),
      students: Array.from(uniqueStudents.values()).map((task) => ({
        studentId: task.studentId,
        studentName: task.studentName,
        unitId: task.unitId,
        unitName: task.unitName,
        groupKeys: task.groupKeys
      }))
    }
  };
}

/**
 * 业务功能：解析发布任务绑定的数据准备业务模块。
 * 关键流程：优先使用教案中保存的业务模块 ID，其次按模块编码匹配，避免前端缓存 ID 与后端模块 ID 不一致时直接中断发布后自动准备。
 */
async function resolvePublishedBusinessModule(
  lesson: LessonPlan,
  platform: BusinessPlatform
): Promise<BusinessModule> {
  const config = getApiConfig();
  const modules = await dataPrepareApi.listActiveBusinessModules({
    tenantId: config.tenantId,
    connectorSystemId: platform.id
  });
  const localModule = platform.modules.find(
    (candidate) => candidate.id === lesson.businessPlatformModuleId
  );
  const matched = modules.find(
    (candidate) =>
      candidate.id === lesson.businessPlatformModuleId ||
      candidate.moduleCode === localModule?.code
  );
  if (!matched) {
    throw new Error('当前教案未绑定可用于数据准备的后端业务模块');
  }
  return matched;
}

/**
 * 业务功能：构造发布后自动准备数据的单个学生参与方。
 * 关键流程：当前阶段以前端分组信息作为单位和角色的最小映射，后续可替换为管理员维护的原平台单位/角色映射表。
 */
function buildPublishDataParticipant(
  lesson: LessonPlan,
  module: BusinessModule,
  publishedTask: PublishedTask,
  studentTask: StudentTask,
  requestBatchId: string
) {
  const actorType = studentTask.groupKeys[0] || studentTask.groupKey || 'student';
  const questionId = publishedDataQuestionId(studentTask);
  const visibleStages = lesson.stages.filter(
    (stage) =>
      stage.visibility[publishedTask.mode] &&
      (publishedTask.mode === 'LEARNING' ||
        publishedTask.mode === 'PRACTICE' ||
        studentTask.groupKeys.includes(stage.groupKey))
  );
  return {
    studentId: studentTask.studentId,
    questionId,
    examAttemptId: safeCode(requestBatchId, 64),
    questionAttemptId: safeCode(`${questionId}-${requestBatchId}`, 64),
    actorType,
    ownerExternalOrgId: studentTask.unitId,
    ownerExternalOrgName: studentTask.unitName,
    requiredExternalOrgId: studentTask.unitId,
    requiredExternalOrgName: studentTask.unitName,
    requiredExternalRoleId: actorType,
    requiredExternalRoleName: actorType,
    dataScopeJson: JSON.stringify({
      lessonId: lesson.id,
      publishedTaskId: publishedTask.id,
      remoteTaskId: publishedTask.remoteTaskId,
      moduleCode: module.moduleCode,
      mode: publishedTask.mode
    }),
    requiredActionsJson: JSON.stringify(
      visibleStages.flatMap((stage) =>
        stage.recordedSteps
          .filter(
            (step) =>
              publishedTask.mode !== 'PRACTICE' ||
              isPracticeMonitorableStep(step)
          )
          .map((step) => ({
            stageId: stage.id,
            stageName: stage.name,
            stepId: step.id,
            actionType: practiceRecordedActionType(step),
            title: step.title
          }))
      )
    ),
    scorePointSnapshotJson: JSON.stringify({
      objectiveMaxScore: lesson.objectiveMaxScore,
      subjectiveMaxScore: lesson.subjectiveMaxScore,
      taskStepIdsByStepId: publishedTask.remoteTaskStepIdsByStepId ?? {}
    })
  };
}

/**
 * 业务功能：为学生任务选择本次发布生成的数据池。
 * 关键流程：优先按 dataItemId 对应的 questionId 选择，兜底使用公共池，保证练习和单题考试都能完成领取。
 */
function resolvePoolForStudentTask(
  pools: TeachingDataPool[],
  studentTask: StudentTask
): TeachingDataPool {
  const pool =
    pools.find(
      (candidate) =>
        candidate.questionId === publishedDataQuestionId(studentTask) &&
        candidate.poolStatus === 'READY' &&
        Number(candidate.readyCount || 0) > 0
    ) ||
    pools.find(
      (candidate) =>
        !candidate.questionId &&
        candidate.poolStatus === 'READY' &&
        Number(candidate.readyCount || 0) > 0
    ) ||
    pools.find(
      (candidate) =>
        candidate.poolStatus === 'READY' &&
        Number(candidate.readyCount || 0) > 0
    );
  if (!pool) {
    throw new Error('原平台初始数据已生成但没有可领取的数据池');
  }
  return pool;
}

function publishedDataQuestionId(studentTask: StudentTask): string {
  return safeCode(studentTask.dataItemId || studentTask.id, 64);
}

export function distributedScore(
  totalScore: number,
  count: number,
  index: number
): number {
  if (count <= 0) return 0;
  const totalCents = Math.round(totalScore * 100);
  const baseCents = Math.floor(totalCents / count);
  const remainder = totalCents - baseCents * count;
  return (baseCents + (index < remainder ? 1 : 0)) / 100;
}

function clone<T>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}
