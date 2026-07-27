import type { RecordedFlow } from '../types/domain';
import type { SaveWorkflowDraftRequest } from '../types/workflow';
import type { RecordedStageBinding } from './captureOrchestrator';
import { migrateRecordedFlow } from './captureWorkspace';

export const PUBLISHED_RECORDED_FLOW_KEY = 'sxpt.publishedRecordedFlow.v2';
export const PENDING_RECORDED_STAGE_WRITEBACK_KEY =
  'sxpt.pendingRecordedStageWriteback.v1';

export interface PendingRecordedStageWriteback {
  workflowDraftId: string;
  stageId: string;
  taskId: string;
  clientRequestId: string;
  binding: RecordedStageBinding;
  request: SaveWorkflowDraftRequest;
}

export function savePendingRecordedStageWriteback(
  pending: PendingRecordedStageWriteback,
  storage: Storage = window.localStorage
): PendingRecordedStageWriteback {
  storage.setItem(
    pendingWritebackKey(pending.workflowDraftId, pending.stageId),
    JSON.stringify(pending)
  );
  return pending;
}

export function loadPendingRecordedStageWriteback(
  workflowDraftId: string,
  stageId: string,
  storage: Storage = window.localStorage
): PendingRecordedStageWriteback | undefined {
  const raw = storage.getItem(pendingWritebackKey(workflowDraftId, stageId));
  if (!raw) return undefined;
  try {
    const parsed = JSON.parse(raw) as PendingRecordedStageWriteback;
    if (
      parsed.workflowDraftId !== workflowDraftId
      || parsed.stageId !== stageId
      || !parsed.taskId
      || !parsed.clientRequestId
      || parsed.request?.clientRequestId !== parsed.clientRequestId
      || parsed.binding?.workflowDraftId !== workflowDraftId
      || parsed.binding?.stageId !== stageId
    ) {
      return undefined;
    }
    return parsed;
  } catch {
    return undefined;
  }
}

export function clearPendingRecordedStageWriteback(
  workflowDraftId: string,
  stageId: string,
  storage: Storage = window.localStorage
): void {
  storage.removeItem(pendingWritebackKey(workflowDraftId, stageId));
}

function pendingWritebackKey(workflowDraftId: string, stageId: string): string {
  return `${PENDING_RECORDED_STAGE_WRITEBACK_KEY}:${workflowDraftId}:${stageId}`;
}

export function savePublishedRecordedFlow(
  flow: RecordedFlow,
  storage: Storage = window.localStorage
): RecordedFlow {
  const migrated = migrateRecordedFlow(flow);
  const publishedFlow: RecordedFlow = {
    ...migrated,
    publishStatus: 'published',
    steps: migrated.steps.map((step) => ({
      ...step,
      rect: { ...step.rect }
    })),
    segments: migrated.segments?.map((segment) => ({
      ...segment,
      steps: segment.steps.map((step) => ({
        ...step,
        rect: { ...step.rect },
        ...(step.kind === 'guide' && step.anchor
          ? { anchor: { ...step.anchor, rect: step.anchor.rect ? { ...step.anchor.rect } : undefined } }
          : {})
      }))
    }))
  };

  try {
    storage.setItem(PUBLISHED_RECORDED_FLOW_KEY, JSON.stringify(publishedFlow));
  } catch (error) {
    if (isStorageQuotaExceeded(error)) {
      throw new Error(
        '浏览器存储空间不足，无法保存带图片的演示教案，请删除部分图片后重试。'
      );
    }
    throw error;
  }
  return publishedFlow;
}

export function loadPublishedRecordedFlow(
  storage: Storage = window.localStorage
): RecordedFlow | undefined {
  const rawValue = storage.getItem(PUBLISHED_RECORDED_FLOW_KEY);
  if (!rawValue) {
    return undefined;
  }

  try {
    return migrateRecordedFlow(JSON.parse(rawValue) as RecordedFlow);
  } catch {
    return undefined;
  }
}

function isStorageQuotaExceeded(error: unknown): boolean {
  if (!error || typeof error !== 'object' || !('name' in error)) return false;
  return ['QuotaExceededError', 'NS_ERROR_DOM_QUOTA_REACHED'].includes(
    String(error.name)
  );
}
