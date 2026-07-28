import type {
  CreateRunBatchRequest, PagedRunDataItems, PrepareRunDataRequest, PublishedWorkflowOption,
  ReasonedCommand, ReplaceRunDataRequest, RunBatchPreflight, RunDataFilter, RunDataGenerationJob,
  RunDataItem, SaveGroupMembersRequest, SaveUnitPlansRequest, WorkflowRunBatch
} from '../../types/runBatch';
import type { AuthenticatedApiClient } from './authenticatedApiClient';

export interface RunBatchApi {
  listPublishedWorkflows(): Promise<PublishedWorkflowOption[]>;
  createBatch(request: CreateRunBatchRequest): Promise<WorkflowRunBatch>;
  getBatch(batchId: string): Promise<WorkflowRunBatch>;
  saveMembers(batchId: string, request: SaveGroupMembersRequest): Promise<WorkflowRunBatch>;
  saveUnitPlans(batchId: string, request: SaveUnitPlansRequest): Promise<WorkflowRunBatch>;
  preflight(batchId: string): Promise<RunBatchPreflight>;
  startDataGeneration(batchId: string, request: PrepareRunDataRequest): Promise<RunDataGenerationJob>;
  getGenerationJob(batchId: string, jobId: string): Promise<RunDataGenerationJob>;
  advanceGenerationJob(batchId: string, jobId: string, clientRequestId: string): Promise<RunDataGenerationJob>;
  listDataItems(batchId: string, filter?: RunDataFilter): Promise<PagedRunDataItems>;
  getDataItem(batchId: string, itemId: string): Promise<RunDataItem>;
  disableData(batchId: string, itemId: string, request: ReasonedCommand): Promise<RunDataItem>;
  promoteData(batchId: string, itemId: string, request: ReasonedCommand): Promise<RunDataItem>;
  replaceData(batchId: string, itemId: string, request: ReplaceRunDataRequest): Promise<RunDataItem>;
  retryData(batchId: string, itemId: string, request: ReasonedCommand): Promise<RunDataItem>;
  publishBatch(batchId: string, clientRequestId: string): Promise<WorkflowRunBatch>;
  cancelBatch(batchId: string, request: ReasonedCommand): Promise<WorkflowRunBatch>;
  createClientRequestId(): string;
}

const segment = encodeURIComponent;
export function createRunBatchApi(client: AuthenticatedApiClient): RunBatchApi {
  const batchPath = (batchId: string) => `/workflow-runs/${segment(batchId)}`;
  const itemPath = (batchId: string, itemId: string) => `${batchPath(batchId)}/data-items/${segment(itemId)}`;
  return {
    listPublishedWorkflows: () => client.get('/workflow-versions/published'),
    createBatch: (request) => client.post('/workflow-runs', request),
    getBatch: (batchId) => client.get(batchPath(batchId)),
    saveMembers: (batchId, request) => client.put(`${batchPath(batchId)}/members`, request),
    saveUnitPlans: (batchId, request) => client.put(`${batchPath(batchId)}/unit-plans`, request),
    preflight: (batchId) => client.get(`${batchPath(batchId)}/preflight`),
    startDataGeneration: (batchId, request) => client.post(`${batchPath(batchId)}/data-jobs`, request),
    getGenerationJob: (batchId, jobId) => client.get(`${batchPath(batchId)}/data-jobs/${segment(jobId)}`),
    advanceGenerationJob: (batchId, jobId, clientRequestId) => client.post(`${batchPath(batchId)}/data-jobs/${segment(jobId)}/advance`, { clientRequestId }),
    listDataItems: (batchId, filter) => client.get(`${batchPath(batchId)}/data-items`, stringifyQuery(filter)),
    getDataItem: (batchId, itemId) => client.get(itemPath(batchId, itemId)),
    disableData: (batchId, itemId, request) => client.post(`${itemPath(batchId, itemId)}/disable`, request),
    promoteData: (batchId, itemId, request) => client.post(`${itemPath(batchId, itemId)}/promote`, request),
    replaceData: (batchId, itemId, request) => client.post(`${itemPath(batchId, itemId)}/replace`, request),
    retryData: (batchId, itemId, request) => client.post(`${itemPath(batchId, itemId)}/retry`, request),
    publishBatch: (batchId, clientRequestId) => client.post(`${batchPath(batchId)}/publish`, { clientRequestId }),
    cancelBatch: (batchId, request) => client.post(`${batchPath(batchId)}/cancel`, request),
    createClientRequestId: () => client.createClientRequestId()
  };
}

function stringifyQuery(filter?: RunDataFilter): Record<string, string> | undefined {
  if (!filter) return undefined;
  return Object.fromEntries(Object.entries(filter).filter(([, value]) => value !== undefined).map(([key, value]) => [key, String(value)]));
}
