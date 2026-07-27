import type {
  BusinessActionPayload,
  RecordedFlow,
  RecordedStep
} from '../types/domain';

export function createRecordedStep(
  payload: BusinessActionPayload,
  order: number
): RecordedStep {
  const title = payload.text || actionTitle(payload.actionType);

  return {
    id: `step-${order}`,
    nodeId: `node-${order}`,
    order,
    title,
    actionType: payload.actionType,
    selector: payload.selector,
    selectorCandidates: payload.selectorCandidates,
    rect: payload.rect,
    text: payload.text,
    value: payload.value,
    url: payload.url,
    teachingText: `请完成：${title}`,
    practiceHint: `请找到并操作“${title}”。`,
    examGoal: `独立完成“${title}”相关业务操作。`,
    completionMethod: payload.actionType === 'submit' ? 'submission' : 'click',
    stableKey: payload.stableKey,
    recordedViewport: payload.recordedViewport
  };
}

export function moveRecordedStep(
  flow: RecordedFlow,
  nodeId: string,
  direction: 'up' | 'down'
): RecordedFlow {
  const index = flow.steps.findIndex((step) => step.nodeId === nodeId);
  const targetIndex = direction === 'up' ? index - 1 : index + 1;

  if (index < 0 || targetIndex < 0 || targetIndex >= flow.steps.length) {
    return flow;
  }

  const steps = [...flow.steps];
  const [removed] = steps.splice(index, 1);
  steps.splice(targetIndex, 0, removed);

  return {
    ...flow,
    steps: steps.map((step, stepIndex) => ({
      ...step,
      order: stepIndex + 1
    }))
  };
}

export function getPlaybackStep(
  flow: RecordedFlow,
  index: number
): RecordedStep | undefined {
  return getPlaybackEntries(flow)[index]?.step;
}

export interface PlaybackEntry {
  segmentId: string;
  segmentNo: number;
  segmentTitle: string;
  roleName?: string;
  orgName?: string;
  targetUrl: string;
  switchReason?: string;
  isSegmentStart: boolean;
  requiresNavigation: boolean;
  step: RecordedStep;
}

export function getPlaybackEntries(flow: RecordedFlow): PlaybackEntry[] {
  const segments = flow.segments?.length
    ? [...flow.segments].sort((a, b) => a.segmentNo - b.segmentNo)
    : [
        {
          id: 'segment-1',
          segmentNo: 1,
          title: '备案第 1 段',
          targetUrl: flow.businessUrl,
          status: 'saved' as const,
          steps: flow.steps
        }
      ];

  return segments.flatMap((segment) =>
    [...segment.steps]
      .sort((a, b) => a.order - b.order)
      .map((step, index) => ({
        segmentId: segment.id,
        segmentNo: segment.segmentNo,
        segmentTitle: segment.title,
        roleName: segment.externalRoleName,
        orgName: segment.externalOrgName,
        targetUrl: segment.targetUrl,
        switchReason: segment.switchReason,
        isSegmentStart: index === 0,
        requiresNavigation:
          index === 0 && normalizePath(segment.targetUrl) !== normalizePath(flow.businessUrl),
        step
      }))
  );
}

export function doesActionMatchStep(
  payload: BusinessActionPayload,
  step: RecordedStep
): boolean {
  if (step.kind === 'guide') return false;
  return (
    payload.actionType === step.actionType &&
    normalizePath(payload.url) === normalizePath(step.url) &&
    payload.selector === step.selector
  );
}

function actionTitle(actionType: BusinessActionPayload['actionType']) {
  const titles: Record<BusinessActionPayload['actionType'], string> = {
    click: '点击目标元素',
    input: '填写输入框',
    select: '选择下拉项',
    submit: '提交业务'
  };

  return titles[actionType];
}

function normalizePath(url: string): string {
  try {
    return new URL(url).pathname;
  } catch {
    return url.split('?')[0];
  }
}
