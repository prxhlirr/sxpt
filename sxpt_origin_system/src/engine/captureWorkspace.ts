import type { CaptureSessionVO } from '../types/trainingApi';
import type {
  BusinessActionPayload,
  RecordedFlow,
  RecordedSegment,
  RecordedStep,
  StepPersistence
} from '../types/domain';
import { createRecordedStep } from './recordingFlow';

export type WorkspaceSessionStatus =
  | 'idle'
  | 'creating'
  | 'running'
  | 'finishing'
  | 'finished'
  | 'error';

export interface CaptureWorkspace {
  flow: RecordedFlow;
  session?: Pick<CaptureSessionVO, 'id'> & Partial<CaptureSessionVO>;
  sessionStatus: WorkspaceSessionStatus;
  activeSegmentId?: string;
  selectedStepId?: string;
}

export interface CaptureCommandState {
  canStart: boolean;
  canPause: boolean;
  canResume: boolean;
  canUndo: boolean;
  canInsertGuide: boolean;
  canRerecord: boolean;
  canSaveSegment: boolean;
  canSwitchSegment: boolean;
  canPublish: boolean;
}

export function migrateRecordedFlow(flow: RecordedFlow): RecordedFlow {
  const existingSegments = flow.segments?.length
    ? flow.segments
    : [
        {
          id: 'segment-1',
          segmentNo: 1,
          title: '备案第 1 段',
          targetUrl: flow.businessUrl,
          status: 'recording' as const,
          steps: flow.steps ?? []
        }
      ];
  const segments = existingSegments.map((segment, segmentIndex) => ({
    ...segment,
    id: segment.id || `segment-${segmentIndex + 1}`,
    segmentNo: segment.segmentNo || segmentIndex + 1,
    title: segment.title || `备案第 ${segmentIndex + 1} 段`,
    targetUrl: segment.targetUrl || flow.businessUrl,
    status: segment.status || ('recording' as const),
    steps: segment.steps.map((step, stepIndex) => normalizeStep(step, stepIndex + 1))
  }));

  return syncFlow({ ...flow, segments });
}

export function createCaptureWorkspace(flow: RecordedFlow): CaptureWorkspace {
  const migrated = migrateRecordedFlow(flow);
  return {
    flow: migrated,
    sessionStatus: 'idle',
    activeSegmentId: migrated.segments?.[0]?.id,
    selectedStepId: migrated.segments?.[0]?.steps[0]?.id
  };
}

export function startWorkspaceSession(
  workspace: CaptureWorkspace,
  session: CaptureSessionVO
): CaptureWorkspace {
  return { ...workspace, session, sessionStatus: 'running' };
}

export function appendActionStep(
  workspace: CaptureWorkspace,
  payload: BusinessActionPayload
): CaptureWorkspace {
  const segment = getActiveSegment(workspace);
  if (!segment) return workspace;
  const step = {
    ...createRecordedStep(payload, segment.steps.length + 1),
    id: createId('step'),
    nodeId: createId('node'),
    kind: 'action' as const,
    required: true,
    advanceMode: 'manual' as const,
    durationMs: 2600,
    failurePolicy: 'stop' as const,
    persistence: { status: 'local' as const },
    clientEventId: payload.clientEventId,
    sdkSessionId: payload.sdkSessionId,
    sequenceNo: payload.sequenceNo,
    stableKey: payload.stableKey,
    pageTitle: payload.pageTitle,
    inputValueMasked: payload.inputValueMasked
  };

  return updateSegment(workspace, segment.id, (current) => ({
    ...current,
    status: 'recording',
    steps: [...current.steps, step]
  }), step.id);
}

export function insertGuideStep(
  workspace: CaptureWorkspace,
  afterStepId?: string,
  currentUrl?: string
): CaptureWorkspace {
  const segment = getActiveSegment(workspace);
  if (!segment) return workspace;
  const targetIndex = afterStepId
    ? segment.steps.findIndex((step) => step.id === afterStepId)
    : segment.steps.length - 1;
  const insertIndex = targetIndex < 0 ? segment.steps.length : targetIndex + 1;
  const guideId = createId('guide');
  const guide: RecordedStep = {
    id: guideId,
    nodeId: createId('node'),
    kind: 'guide',
    order: insertIndex + 1,
    title: '操作说明',
    actionType: 'click',
    selector: '',
    rect: { x: 0, y: 0, width: 0, height: 0 },
    text: '',
    url: currentUrl || segment.targetUrl,
    teachingText: '请填写本节点需要向学员说明的内容。',
    practiceHint: '阅读说明后继续操作。',
    examGoal: '',
    completionMethod: 'manual',
    required: false,
    advanceMode: 'manual',
    durationMs: 2600,
    failurePolicy: 'skip',
    persistence: { status: 'local' },
    anchor: { mode: 'center' }
  };
  const steps = [...segment.steps];
  steps.splice(insertIndex, 0, guide);

  return updateSegment(
    workspace,
    segment.id,
    (current) => ({ ...current, steps: renumber(steps), status: 'recording' }),
    guideId
  );
}

export function patchStep(
  workspace: CaptureWorkspace,
  stepId: string,
  patch: Partial<RecordedStep>
): CaptureWorkspace {
  const segment = findSegmentByStep(workspace, stepId);
  if (!segment) return workspace;
  return updateSegment(workspace, segment.id, (current) => ({
    ...current,
    steps: current.steps.map((step) =>
      step.id === stepId ? ({ ...step, ...patch } as RecordedStep) : step
    )
  }));
}

export function moveStepWithinSegment(
  workspace: CaptureWorkspace,
  stepId: string,
  direction: 'up' | 'down'
): CaptureWorkspace {
  const segment = findSegmentByStep(workspace, stepId);
  if (!segment) return workspace;
  const index = segment.steps.findIndex((step) => step.id === stepId);
  const targetIndex = direction === 'up' ? index - 1 : index + 1;
  if (index < 0 || targetIndex < 0 || targetIndex >= segment.steps.length) {
    return workspace;
  }
  const steps = [...segment.steps];
  const [moved] = steps.splice(index, 1);
  steps.splice(targetIndex, 0, moved);
  return updateSegment(workspace, segment.id, (current) => ({
    ...current,
    steps: renumber(steps),
    status: 'recording'
  }), stepId);
}

export function markStepPersistence(
  workspace: CaptureWorkspace,
  stepId: string,
  persistence: StepPersistence
): CaptureWorkspace {
  return patchStep(workspace, stepId, { persistence });
}

export function undoLastLocalStep(workspace: CaptureWorkspace): CaptureWorkspace {
  const segment = getActiveSegment(workspace);
  const last = segment?.steps.at(-1);
  if (!segment || !last || (last.persistence?.status ?? 'local') !== 'local') {
    return workspace;
  }
  return updateSegment(workspace, segment.id, (current) => ({
    ...current,
    steps: renumber(current.steps.slice(0, -1)),
    status: 'recording'
  }), segment.steps.at(-2)?.id);
}

export function truncateSegmentFromStep(
  workspace: CaptureWorkspace,
  stepId: string
): CaptureWorkspace {
  const segment = findSegmentByStep(workspace, stepId);
  if (!segment) return workspace;
  const index = segment.steps.findIndex((step) => step.id === stepId);
  const removed = segment.steps.slice(index);
  if (
    removed.some((step) =>
      ['confirmed', 'discarded'].includes(step.persistence?.status ?? '')
    )
  ) {
    return workspace;
  }
  return updateSegment(workspace, segment.id, (current) => ({
    ...current,
    steps: renumber(current.steps.slice(0, index)),
    status: 'recording'
  }), segment.steps[index - 1]?.id);
}

export function markActiveSegmentSaved(
  workspace: CaptureWorkspace
): CaptureWorkspace {
  const segment = getActiveSegment(workspace);
  return segment
    ? updateSegment(workspace, segment.id, (current) => ({
        ...current,
        status: 'saved'
      }))
    : workspace;
}

export function appendSegment(
  workspace: CaptureWorkspace,
  input: Omit<RecordedSegment, 'id' | 'steps' | 'status' | 'title'> & {
    title?: string;
  }
): CaptureWorkspace {
  const segment: RecordedSegment = {
    ...input,
    id: createId('segment'),
    title: input.title ?? `备案第 ${input.segmentNo} 段`,
    status: 'recording',
    steps: []
  };
  const flow = syncFlow({
    ...workspace.flow,
    segments: [...(workspace.flow.segments ?? []), segment]
  });
  return {
    ...workspace,
    flow,
    activeSegmentId: segment.id,
    selectedStepId: undefined
  };
}

export function flattenRecordedSteps(flow: RecordedFlow): RecordedStep[] {
  if (!flow.segments?.length) return [...flow.steps].sort((a, b) => a.order - b.order);
  return [...flow.segments]
    .sort((a, b) => a.segmentNo - b.segmentNo)
    .flatMap((segment) => [...segment.steps].sort((a, b) => a.order - b.order));
}

export function getCaptureCommandState(
  workspace: CaptureWorkspace
): CaptureCommandState {
  const running = workspace.sessionStatus === 'running';
  const segment = getActiveSegment(workspace);
  const selected = segment?.steps.find((step) => step.id === workspace.selectedStepId);
  const last = segment?.steps.at(-1);
  return {
    canStart: workspace.sessionStatus === 'idle' || workspace.sessionStatus === 'error',
    canPause: running,
    canResume: Boolean(workspace.session && workspace.sessionStatus === 'idle'),
    canUndo: running && (last?.persistence?.status ?? 'local') === 'local',
    canInsertGuide: running,
    canRerecord: running && Boolean(selected),
    canSaveSegment: running && Boolean(segment?.steps.length),
    canSwitchSegment: running && Boolean(segment?.steps.length),
    canPublish: running && Boolean(workspace.flow.segments?.some((item) => item.steps.length))
  };
}

export function getActiveSegment(
  workspace: CaptureWorkspace
): RecordedSegment | undefined {
  return workspace.flow.segments?.find(
    (segment) => segment.id === workspace.activeSegmentId
  );
}

function findSegmentByStep(
  workspace: CaptureWorkspace,
  stepId: string
): RecordedSegment | undefined {
  return workspace.flow.segments?.find((segment) =>
    segment.steps.some((step) => step.id === stepId)
  );
}

function updateSegment(
  workspace: CaptureWorkspace,
  segmentId: string,
  update: (segment: RecordedSegment) => RecordedSegment,
  selectedStepId = workspace.selectedStepId
): CaptureWorkspace {
  const segments = (workspace.flow.segments ?? []).map((segment) =>
    segment.id === segmentId ? update(segment) : segment
  );
  return {
    ...workspace,
    flow: syncFlow({ ...workspace.flow, segments }),
    selectedStepId
  };
}

function syncFlow(flow: RecordedFlow): RecordedFlow {
  return {
    ...flow,
    segments: flow.segments,
    steps: flow.segments
      ? flow.segments.flatMap((segment) => segment.steps)
      : flow.steps
  };
}

function normalizeStep(step: RecordedStep, order: number): RecordedStep {
  return {
    ...step,
    kind: step.kind ?? 'action',
    order,
    required: step.required ?? true,
    advanceMode: step.advanceMode ?? 'manual',
    durationMs: step.durationMs ?? 2600,
    failurePolicy: step.failurePolicy ?? 'stop',
    persistence: step.persistence ?? { status: 'local' }
  } as RecordedStep;
}

function renumber(steps: RecordedStep[]): RecordedStep[] {
  return steps.map((step, index) => ({ ...step, order: index + 1 }));
}

function createId(prefix: string): string {
  const suffix =
    typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `${prefix}-${suffix}`;
}
