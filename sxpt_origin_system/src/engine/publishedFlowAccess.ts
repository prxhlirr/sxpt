import type { RecordedFlow } from '../types/domain';
import { flattenRecordedSteps } from './captureWorkspace';

export function hasRecordedContent(flow?: RecordedFlow): boolean {
  return Boolean(flow && flattenRecordedSteps(flow).length > 0);
}

export function canEnterPublishedFlow(flow?: RecordedFlow): boolean {
  return Boolean(
    flow?.publishStatus === 'published' && hasRecordedContent(flow)
  );
}
