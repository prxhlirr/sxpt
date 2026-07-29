import type { TrainingRuntimeConfig } from '../config/trainingConfig';
import type {
  ConfirmSegmentSwitchRequest,
  CreateTeachingPointRequest,
  TeachingPointVO,
  TrainingApi
} from '../types/trainingApi';
import type {
  BusinessActionPayload,
  RecordedSegment,
  RecordedStep,
  StepPersistence
} from '../types/domain';
import { toCaptureEventRequest } from './captureProtocol';
import {
  appendActionStep,
  appendSegment,
  flattenRecordedSteps,
  getActiveSegment,
  markActiveSegmentSaved,
  markStepPersistence,
  startWorkspaceSession,
  type CaptureWorkspace
} from './captureWorkspace';

export interface SwitchSegmentInput {
  nextSegmentNo: number;
  actorType?: string;
  requiredExternalOrgId: string;
  requiredExternalOrgName?: string;
  requiredExternalRoleId: string;
  requiredExternalRoleName?: string;
  targetUrl: string;
  switchReason?: string;
  externalBusinessId?: string;
  externalBusinessNo?: string;
  dataScopeJson?: string;
}

export interface PublishTeachingPointInput {
  pointCode: string;
  pointName: string;
  pointType?: string;
  executionStrategy?: string;
  description?: string;
}

export interface PublishTeachingPointResult {
  workspace: CaptureWorkspace;
  teachingPoint: TeachingPointVO;
}

export interface RecordedStageBindingStep {
  taskStepId: string;
  recordedSegmentId: string;
  recordedAssetVersion: number;
  sequenceNo: number;
  required: boolean;
  completionMethod: string;
  failurePolicy: string;
  score: number;
  matcher: Record<string, unknown>;
}

export interface RecordedStageBinding {
  workflowDraftId: string;
  stageId: string;
  recordedSegmentId: string;
  recordedAssetVersion: number;
  steps: RecordedStageBindingStep[];
}

export interface BuildRecordedStageBindingInput {
  workflowDraftId: string;
  stageId: string;
  workspace: CaptureWorkspace;
  stageMaxScore: number;
  recordedAssetVersion?: number;
}

export interface PublishRecordedStageInput
  extends PublishTeachingPointInput {
  workflowDraftId: string;
  stageId: string;
  taskId: string;
  stageSequenceNo: number;
  stageMaxScore: number;
  actorType?: string;
  externalOrgId?: string;
  externalRoleId?: string;
}

export interface PublishRecordedStageResult
  extends PublishTeachingPointResult {
  binding: RecordedStageBinding;
}

export async function startCapture(
  api: TrainingApi,
  config: TrainingRuntimeConfig,
  workspace: CaptureWorkspace
): Promise<CaptureWorkspace> {
  const session = await api.createCaptureSession({
    tenantId: config.tenantId,
    connectorSystemId: config.connectorSystemId,
    teacherId: config.teacherId,
    sessionName: workspace.flow.title,
    startUrl: workspace.flow.businessUrl,
    captureMode: 'STANDARD'
  });
  const sessionId = session.id || session.captureSessionId;
  if (!sessionId) throw new Error('创建采集会话成功，但响应缺少 captureSessionId');
  return startWorkspaceSession(workspace, { ...session, id: sessionId });
}

export async function reportAction(
  api: TrainingApi,
  config: TrainingRuntimeConfig,
  workspace: CaptureWorkspace,
  action: BusinessActionPayload
): Promise<CaptureWorkspace> {
  let next = appendActionStep(workspace, action);
  const stepId = next.selectedStepId;
  if (!stepId) return next;

  try {
    next = await reportStepEventAndSnapshot(api, config, next, stepId);
    return next;
  } catch (error) {
    return markStepPersistence(next, stepId, {
      ...(findStep(next, stepId)?.persistence ?? { status: 'local' }),
      status: 'error',
      errorMessage: errorMessage(error)
    });
  }
}

export async function saveSegment(
  api: TrainingApi,
  config: TrainingRuntimeConfig,
  workspace: CaptureWorkspace,
  segmentId: string
): Promise<CaptureWorkspace> {
  let next = patchSegmentStatus(workspace, segmentId, 'saving');
  const stepIds = findSegment(next, segmentId)?.steps.map((step) => step.id) ?? [];

  try {
    for (const stepId of stepIds) {
      const step = findStep(next, stepId);
      if (!step || step.persistence?.status === 'discarded') continue;

      if (step.kind !== 'guide') {
        next = await reportStepEventAndSnapshot(api, config, next, stepId);
      }

      const current = findStep(next, stepId);
      if (!current?.persistence?.actionDraftId) {
        const draft = await api.createActionDraft({
          tenantId: config.tenantId,
          captureSessionId: requiredSessionId(next),
          eventId: current?.persistence?.eventId,
          actionName: current?.title ?? '未命名动作',
          actionType:
            current?.kind === 'guide'
              ? 'GUIDE'
              : (current?.actionType.toUpperCase() ?? 'CLICK'),
          sequenceNo: current?.sequenceNo ?? globalStepIndex(next, stepId) + 1,
          suggestedOperationName: current?.title,
          confirmedOperationName: current?.title,
          confirmedStepName: current?.title,
          guideContent: current?.teachingText,
          practiceHint: current?.practiceHint
        });
        next = markStepPersistence(next, stepId, {
          ...(current?.persistence ?? { status: 'local' }),
          actionDraftId: draft.id,
          status: 'draft_pending',
          errorMessage: undefined
        });
      }
    }

    return markActiveSegmentSaved(patchSegmentStatus(next, segmentId, 'saved'));
  } catch (error) {
    const failedStepId = stepIds.find(
      (stepId) => findStep(next, stepId)?.persistence?.status === 'error'
    );
    if (failedStepId) {
      next = markStepPersistence(next, failedStepId, {
        ...(findStep(next, failedStepId)?.persistence ?? { status: 'local' }),
        status: 'error',
        errorMessage: errorMessage(error)
      });
    }
    patchSegmentStatus(next, segmentId, 'error');
    throw error;
  }
}

export async function switchSegment(
  api: TrainingApi,
  config: TrainingRuntimeConfig,
  workspace: CaptureWorkspace,
  input: SwitchSegmentInput
): Promise<CaptureWorkspace> {
  const current = getActiveSegment(workspace);
  if (!current) throw new Error('当前没有可保存的备案分段');
  let next = await saveSegment(api, config, workspace, current.id);
  const request: ConfirmSegmentSwitchRequest = {
    tenantId: config.tenantId,
    teacherId: config.teacherId,
    connectorSystemId: config.connectorSystemId,
    captureSessionId: requiredSessionId(next),
    currentSegmentNo: current.segmentNo,
    ...input
  };
  const result = await api.confirmSegmentSwitch(request);
  next = appendSegment(next, {
    segmentNo: input.nextSegmentNo,
    actorType: input.actorType,
    externalOrgId: input.requiredExternalOrgId,
    externalOrgName: input.requiredExternalOrgName,
    externalRoleId: input.requiredExternalRoleId,
    externalRoleName: input.requiredExternalRoleName,
    targetUrl: input.targetUrl,
    switchReason: input.switchReason,
    launchToken: result.launchToken,
    expireTime: result.expireTime
  });
  return next;
}

export async function publishTeachingPoint(
  api: TrainingApi,
  config: TrainingRuntimeConfig,
  workspace: CaptureWorkspace,
  input: PublishTeachingPointInput
): Promise<PublishTeachingPointResult> {
  const active = getActiveSegment(workspace);
  let next =
    active && active.status !== 'saved'
      ? await saveSegment(api, config, workspace, active.id)
      : workspace;

  for (const step of flattenRecordedSteps(next.flow)) {
    if (
      step.persistence?.status === 'draft_pending' &&
      step.persistence.actionDraftId
    ) {
      await api.confirmActionDraft(step.persistence.actionDraftId, {
        confirmedOperationName: step.title,
        confirmedStepName: step.title,
        guideContent: step.teachingText,
        practiceHint: step.practiceHint,
        updateBy: config.teacherId
      });
      next = markStepPersistence(next, step.id, {
        ...step.persistence,
        status: 'confirmed'
      });
    }
  }

  for (const step of flattenRecordedSteps(next.flow)) {
    const current = findStep(next, step.id);
    if (
      current?.kind !== 'guide' &&
      current?.persistence?.status === 'confirmed' &&
      !current.persistence.connectorResourceId &&
      current.selector
    ) {
      const resource = await api.createConnectorResource({
        tenantId: config.tenantId,
        connectorSystemId: config.connectorSystemId,
        resourceCode: current.stableKey || `STEP_${globalStepIndex(next, current.id) + 1}`,
        resourceName: current.title,
        resourceType: 'DOM_ELEMENT',
        pageUrl: current.url,
        locator: current.selector,
        stableKey: current.stableKey,
        sourceCaptureId: requiredSessionId(next),
        createBy: config.teacherId
      });
      next = markStepPersistence(next, current.id, {
        ...current.persistence,
        connectorResourceId: resource.id
      });
    }
  }

  const pointRequest: CreateTeachingPointRequest = {
    tenantId: config.tenantId,
    connectorSystemId: config.connectorSystemId,
    pointCode: input.pointCode,
    pointName: input.pointName,
    pointType: input.pointType ?? 'SCENARIO',
    sourceCaptureSessionId: requiredSessionId(next),
    businessOverviewJson: JSON.stringify({
      title: next.flow.title,
      segmentCount: next.flow.segments?.length ?? 0,
      teacherName: config.teacherName
    }),
    recordPathJson: JSON.stringify(buildRecordPath(next)),
    executionStrategy: input.executionStrategy ?? 'ROLE_SWITCH',
    overlayPolicyJson: JSON.stringify({
      learning: { showGuide: true, autoPlayback: true },
      practice: { showGuide: true, autoPlayback: false },
      exam: { showGuide: false, autoPlayback: false }
    }),
    description: input.description,
    createBy: config.teacherId
  };
  const teachingPoint = await api.createTeachingPoint(pointRequest);
  await api.finishCaptureSession(requiredSessionId(next));

  next = {
    ...next,
    sessionStatus: 'finished',
    flow: { ...next.flow, publishStatus: 'published' }
  };
  return { workspace: next, teachingPoint };
}

export async function publishRecordedStage(
  api: TrainingApi,
  config: TrainingRuntimeConfig,
  workspace: CaptureWorkspace,
  input: PublishRecordedStageInput
): Promise<PublishRecordedStageResult> {
  const published = await publishTeachingPoint(
    api,
    config,
    workspace,
    input
  );
  let next = published.workspace;
  const recordedSteps = flattenRecordedSteps(next.flow).filter(
    (step) =>
      step.persistence?.status === 'confirmed'
      && Boolean(step.persistence.actionDraftId)
  );

  for (const [index, step] of recordedSteps.entries()) {
    const actionDraftId = step.persistence!.actionDraftId!;
    const taskStep = await api.createTaskStep({
      tenantId: config.tenantId,
      taskId: input.taskId,
      teachingPointId: published.teachingPoint.id,
      stepCode: `STEP_${index + 1}_${actionDraftId.slice(0, 20)}`,
      stepName: step.title,
      stepDescription: step.examGoal || step.teachingText,
      sequenceNo: index + 1,
      segmentNo: input.stageSequenceNo,
      actorType: input.actorType ?? 'WORKFLOW_STAGE',
      requiredExternalOrgId: input.externalOrgId,
      requiredExternalRoleId: input.externalRoleId,
      switchStrategy: 'SERIAL_ONLY',
      switchConfirmRequired: true,
      switchDecisionSource: 'WORKFLOW_AUTHORING',
      rollbackPolicy: 'REPLACEMENT_DATA',
      relatedResourceIds: JSON.stringify(
        step.persistence?.connectorResourceId
          ? [step.persistence.connectorResourceId]
          : []
      ),
      guideContent: step.teachingText,
      practiceHint: step.practiceHint,
      required: step.required ?? true,
      allowSkip: !(step.required ?? true),
      sourceActionDraftId: actionDraftId,
      createBy: config.teacherId
    });
    next = markStepPersistence(next, step.id, {
      ...step.persistence!,
      taskStepId: taskStep.id
    });
  }

  return {
    workspace: next,
    teachingPoint: published.teachingPoint,
    binding: buildRecordedStageBinding({
      workflowDraftId: input.workflowDraftId,
      stageId: input.stageId,
      workspace: next,
      stageMaxScore: input.stageMaxScore
    })
  };
}

export function buildRecordedStageBinding(
  input: BuildRecordedStageBindingInput
): RecordedStageBinding {
  const version = input.recordedAssetVersion ?? 1;
  const steps = flattenRecordedSteps(input.workspace.flow).filter(
    (step) =>
      step.persistence?.status !== 'discarded'
      && Boolean(step.persistence?.actionDraftId)
      && Boolean(step.persistence?.taskStepId)
  );
  if (!steps.length) {
    throw new Error('当前阶段没有可绑定的已确认录制步骤');
  }

  const scores = distributeScore(input.stageMaxScore, steps.length);
  const bindings = steps.map<RecordedStageBindingStep>((step, index) => ({
    taskStepId: step.persistence!.taskStepId!,
    recordedSegmentId: step.persistence!.actionDraftId!,
    recordedAssetVersion: version,
    sequenceNo: index + 1,
    required: step.required ?? true,
    completionMethod: step.completionMethod,
    failurePolicy: step.failurePolicy ?? 'stop',
    score: scores[index],
    matcher: {
      url: step.url,
      selector: step.selector,
      actionType: step.actionType,
      stableKey: step.stableKey,
      expectedText: step.text || undefined
    }
  }));

  return {
    workflowDraftId: input.workflowDraftId,
    stageId: input.stageId,
    recordedSegmentId: bindings[0].recordedSegmentId,
    recordedAssetVersion: version,
    steps: bindings
  };
}

async function reportStepEventAndSnapshot(
  api: TrainingApi,
  config: TrainingRuntimeConfig,
  workspace: CaptureWorkspace,
  stepId: string
): Promise<CaptureWorkspace> {
  let next = workspace;
  let step = findStep(next, stepId);
  if (!step || step.kind === 'guide') return next;
  const persistence: StepPersistence = step.persistence ?? { status: 'local' };

  if (!persistence.eventId) {
    next = markStepPersistence(next, stepId, {
      ...persistence,
      status: 'reporting',
      errorMessage: undefined
    });
    const action = stepToPayload(step);
    const event = await api.reportCaptureEvent(
      toCaptureEventRequest(action, {
        tenantId: config.tenantId,
        captureSessionId: requiredSessionId(next),
        sequenceNo: globalStepIndex(next, stepId) + 1
      })
    );
    step = findStep(next, stepId)!;
    next = markStepPersistence(next, stepId, {
      ...(step.persistence ?? persistence),
      eventId: event.id,
      status: 'reported'
    });
  }

  step = findStep(next, stepId)!;
  if (!step.persistence?.resourceSnapshotId) {
    const snapshot = await api.reportResourceSnapshot({
      tenantId: config.tenantId,
      captureSessionId: requiredSessionId(next),
      pageUrl: step.url,
      pageTitle: step.pageTitle,
      resourceType: 'DOM_ELEMENT',
      snapshotScope: 'KEY_ELEMENT',
      resourceName: step.title,
      resourceLocator: step.selector,
      elementSnapshotJson: JSON.stringify({
        rect: step.rect,
        text: step.text,
        stableKey: step.stableKey
      })
    });
    next = markStepPersistence(next, stepId, {
      ...(step.persistence ?? { status: 'reported' }),
      resourceSnapshotId: snapshot.id,
      status: 'reported'
    });
  }
  return next;
}

function stepToPayload(step: RecordedStep): BusinessActionPayload {
  return {
    actionType: step.actionType,
    url: step.url,
    selector: step.selector,
    text: step.text,
    value: step.value,
    rect: step.rect,
    timestamp: new Date().toISOString(),
    clientEventId: step.clientEventId ?? `client-${step.id}`,
    sdkSessionId: step.sdkSessionId,
    sequenceNo: step.sequenceNo,
    stableKey: step.stableKey,
    pageTitle: step.pageTitle,
    inputValueMasked: step.inputValueMasked
  };
}

function requiredSessionId(workspace: CaptureWorkspace): string {
  if (!workspace.session?.id) throw new Error('采集会话尚未创建');
  return workspace.session.id;
}

function findSegment(
  workspace: CaptureWorkspace,
  segmentId: string
): RecordedSegment | undefined {
  return workspace.flow.segments?.find((segment) => segment.id === segmentId);
}

function findStep(
  workspace: CaptureWorkspace,
  stepId: string
): RecordedStep | undefined {
  return flattenRecordedSteps(workspace.flow).find((step) => step.id === stepId);
}

function patchSegmentStatus(
  workspace: CaptureWorkspace,
  segmentId: string,
  status: RecordedSegment['status']
): CaptureWorkspace {
  const segments = (workspace.flow.segments ?? []).map((segment) =>
    segment.id === segmentId ? { ...segment, status } : segment
  );
  return {
    ...workspace,
    flow: {
      ...workspace.flow,
      segments,
      steps: segments.flatMap((segment) => segment.steps)
    }
  };
}

function globalStepIndex(workspace: CaptureWorkspace, stepId: string): number {
  return Math.max(
    0,
    flattenRecordedSteps(workspace.flow).findIndex((step) => step.id === stepId)
  );
}

function buildRecordPath(workspace: CaptureWorkspace) {
  return (workspace.flow.segments ?? []).map((segment) => ({
    segmentNo: segment.segmentNo,
    actorType: segment.actorType,
    externalOrgId: segment.externalOrgId,
    externalOrgName: segment.externalOrgName,
    externalRoleId: segment.externalRoleId,
    externalRoleName: segment.externalRoleName,
    targetUrl: segment.targetUrl,
    switchReason: segment.switchReason,
    steps: segment.steps
      .filter((step) => step.persistence?.status !== 'discarded')
      .map((step) => ({
        id: step.id,
        kind: step.kind ?? 'action',
        order: step.order,
        title: step.title,
        actionType: step.actionType,
        selector: step.selector,
        teachingText: step.teachingText,
        practiceHint: step.practiceHint,
        connectorResourceId: step.persistence?.connectorResourceId
      }))
  }));
}

function errorMessage(error: unknown): string {
  return error instanceof Error ? error.message : '接口调用失败';
}

function distributeScore(total: number, count: number): number[] {
  const totalCents = Math.round(total * 100);
  const base = Math.floor(totalCents / count);
  let remainder = totalCents - base * count;
  return Array.from({ length: count }, () => {
    const cents = base + (remainder-- > 0 ? 1 : 0);
    return cents / 100;
  });
}
