import type {
  CreateRunBatchRequest, PagedRunDataItems, PrepareRunDataRequest, PublishedWorkflowOption,
  ReasonedCommand, ReplaceRunDataRequest, RunAuditEvent, RunBatchPreflight, RunDataFilter, RunPreflightIssue,
  RunDataGenerationJob, RunDataItem, SaveGroupMembersRequest, SaveUnitPlansRequest, UnitDataPlanInput,
  WorkflowRunBatch
} from '../../types/runBatch';
import type { RunBatchApi } from './runBatchApi';

export const RUN_BATCH_MOCK_STORAGE_KEY = 'sxpt.run-batch.mock.v1';

interface MockRepository {
  workflows: PublishedWorkflowOption[];
  batches: WorkflowRunBatch[];
  jobs: RunDataGenerationJob[];
  items: RunDataItem[];
  commandResults: Record<string, unknown>;
  sequence: number;
  nextId: number;
}

export function createMockRunBatchApi(
  storage: Storage,
  requestIdFactory: () => string = defaultRequestId
): RunBatchApi {
  const load = (): MockRepository => clone(readRepository(storage));
  const save = (repository: MockRepository) => storage.setItem(RUN_BATCH_MOCK_STORAGE_KEY, JSON.stringify(repository));
  const nextId = (repository: MockRepository, kind: string) => `${kind}-${++repository.nextId}`;
  const now = () => new Date().toISOString();
  const resultKey = (action: string, clientRequestId: string) => `${action}:${clientRequestId}`;
  const audit = (repository: MockRepository, batch: WorkflowRunBatch, eventType: string, aggregateType: RunAuditEvent['aggregateType'], aggregateId: string, clientRequestId: string, reason?: string) => {
    const event: RunAuditEvent = { sequenceNo: ++repository.sequence, eventType, aggregateType, aggregateId, clientRequestId, reason, occurredAt: now() };
    batch.auditHistory.push(event);
    if (aggregateType === 'DATA_ITEM') repository.items.find((item) => item.id === aggregateId)?.auditHistory.push(event);
  };
  const batch = (repository: MockRepository, batchId: string) => required(repository.batches.find((value) => value.id === batchId), `Batch ${batchId} was not found`);
  const item = (repository: MockRepository, batchId: string, itemId: string) => required(repository.items.find((value) => value.id === itemId && value.batchId === batchId), `Data item ${itemId} was not found`);
  const job = (repository: MockRepository, batchId: string, jobId: string) => required(repository.jobs.find((value) => value.id === jobId && value.batchId === batchId), `Generation job ${jobId} was not found`);
  const remember = <T>(repository: MockRepository, key: string, value: T): T => { repository.commandResults[key] = clone(value); return value; };
  const recalled = <T>(repository: MockRepository, key: string): T | undefined => repository.commandResults[key] as T | undefined;

  return {
    async listPublishedWorkflows() { return clone(load().workflows); },
    async createBatch(request) {
      const repository = load();
      const key = resultKey('create', request.clientRequestId);
      const existing = recalled<WorkflowRunBatch>(repository, key);
      if (existing) return clone(existing);
      const workflow = required(repository.workflows.find((value) => value.id === request.workflowVersionId), `Published workflow ${request.workflowVersionId} was not found`);
      const timestamp = now();
      const created: WorkflowRunBatch = {
        id: nextId(repository, 'batch'), workflowVersionId: workflow.id, workflowSnapshotDigest: workflow.snapshotDigest,
        batchName: request.batchName, runType: request.runType, status: 'DRAFT', plannedStartTime: request.plannedStartTime,
        plannedEndTime: request.plannedEndTime, objectiveMaxScore: workflow.objectiveMaxScore, subjectiveMaxScore: workflow.subjectiveMaxScore,
        roleGroups: clone(workflow.roleGroups), stages: clone(workflow.stages), rubricItems: clone(workflow.rubricItems), members: [], unitPlans: [], auditHistory: [], createdAt: timestamp, updatedAt: timestamp
      };
      audit(repository, created, 'BATCH_CREATED', 'BATCH', created.id, request.clientRequestId);
      repository.batches.push(created); remember(repository, key, created); save(repository); return clone(created);
    },
    async getBatch(batchId) { const repository = load(); return clone(batch(repository, batchId)); },
    async saveMembers(batchId, request) {
      const repository = load(); const target = batch(repository, batchId); const key = resultKey(`members:${batchId}`, request.clientRequestId); const previous = recalled<WorkflowRunBatch>(repository, key);
      if (previous) return clone(previous);
      target.members = request.members.map((member) => ({ ...clone(member), externalAccountMapping: sanitizeExternalAccountMapping(member.externalAccountMapping) })); target.updatedAt = now(); audit(repository, target, 'MEMBERS_SAVED', 'BATCH', batchId, request.clientRequestId); remember(repository, key, target); save(repository); return clone(target);
    },
    async saveUnitPlans(batchId, request) {
      const repository = load(); const target = batch(repository, batchId); const key = resultKey(`plans:${batchId}`, request.clientRequestId); const previous = recalled<WorkflowRunBatch>(repository, key);
      if (previous) return clone(previous);
      target.unitPlans = clone(request.unitPlans); target.updatedAt = now(); audit(repository, target, 'UNIT_PLANS_SAVED', 'BATCH', batchId, request.clientRequestId); remember(repository, key, target); save(repository); return clone(target);
    },
    async preflight(batchId) { const repository = load(); return clone(createPreflight(batch(repository, batchId), repository.items)); },
    async startDataGeneration(batchId, request) {
      const repository = load(); const target = batch(repository, batchId); const key = resultKey(`prepare:${batchId}`, request.clientRequestId); const previous = recalled<RunDataGenerationJob>(repository, key);
      if (previous) return clone(previous);
      const plans = request.unitPlans.length ? request.unitPlans : target.unitPlans;
      const timestamp = now(); const generationJob: RunDataGenerationJob = { id: nextId(repository, 'job'), batchId, clientRequestId: request.clientRequestId, status: 'PENDING', requestedCount: plans.reduce((sum, plan) => sum + plan.formalCount + plan.spareCount, 0), readyCount: 0, failedCount: 0, createdAt: timestamp, updatedAt: timestamp };
      for (const plan of plans) createItems(repository, generationJob, plan, nextId, timestamp);
      target.status = 'DATA_PREPARING'; target.latestGenerationJobId = generationJob.id; target.updatedAt = timestamp; repository.jobs.push(generationJob); audit(repository, target, 'DATA_GENERATION_STARTED', 'DATA_JOB', generationJob.id, request.clientRequestId); remember(repository, key, generationJob); save(repository); return clone(generationJob);
    },
    async getGenerationJob(batchId, jobId) { const repository = load(); return clone(job(repository, batchId, jobId)); },
    async advanceGenerationJob(batchId, jobId, clientRequestId) {
      const repository = load(); const target = batch(repository, batchId); const generationJob = job(repository, batchId, jobId); const key = resultKey(`advance:${jobId}`, clientRequestId); const previous = recalled<RunDataGenerationJob>(repository, key);
      if (previous) return clone(previous);
      generationJob.status = 'RUNNING';
      for (const dataItem of repository.items.filter((value) => value.generationJobId === jobId && value.status === 'GENERATING')) {
        const indexes = Array.isArray(dataItem.generationParameters.failIndexes) ? dataItem.generationParameters.failIndexes : [];
        const shouldFail = indexes.includes(dataItemIndex(dataItem));
        dataItem.status = shouldFail ? 'GENERATION_FAILED' : 'READY'; dataItem.updatedAt = now();
        audit(repository, target, shouldFail ? 'DATA_GENERATION_FAILED' : 'DATA_GENERATION_READY', 'DATA_ITEM', dataItem.id, clientRequestId, shouldFail ? 'Mock generation failure' : undefined);
      }
      const generated = repository.items.filter((value) => value.generationJobId === jobId);
      generationJob.readyCount = generated.filter((value) => value.status === 'READY').length; generationJob.failedCount = generated.filter((value) => value.status === 'GENERATION_FAILED').length;
      generationJob.status = generationJob.failedCount ? 'COMPLETED_WITH_FAILURES' : 'COMPLETED'; generationJob.updatedAt = now(); target.updatedAt = generationJob.updatedAt; remember(repository, key, generationJob); save(repository); return clone(generationJob);
    },
    async listDataItems(batchId, filter) {
      const repository = load(); const filtered = repository.items.filter((value) => value.batchId === batchId && (!filter?.unitId || value.unitId === filter.unitId) && (!filter?.status || value.status === filter.status) && (!filter?.kind || value.kind === filter.kind));
      const page = filter?.page ?? 1; const pageSize = filter?.pageSize ?? 20; return clone({ items: filtered.slice((page - 1) * pageSize, page * pageSize), page, pageSize, total: filtered.length } satisfies PagedRunDataItems);
    },
    async getDataItem(batchId, itemId) { const repository = load(); return clone(item(repository, batchId, itemId)); },
    async disableData(batchId, itemId, request) { return mutateItem('disable', 'DATA_DISABLED', 'DISABLED', batchId, itemId, request); },
    async promoteData(batchId, itemId, request) {
      const reason = requiredReason(request.reason); const repository = load(); const target = batch(repository, batchId); const dataItem = item(repository, batchId, itemId); const key = resultKey(`promote:${itemId}`, request.clientRequestId); const previous = recalled<RunDataItem>(repository, key); if (previous) return clone(previous);
      if (dataItem.kind !== 'SPARE' || dataItem.status !== 'READY') throw new Error('Only ready spare data can be promoted'); dataItem.kind = 'FORMAL'; dataItem.updatedAt = now(); audit(repository, target, 'DATA_PROMOTED', 'DATA_ITEM', itemId, request.clientRequestId, reason); remember(repository, key, dataItem); save(repository); return clone(dataItem);
    },
    async replaceData(batchId, itemId, request) {
      const reason = requiredReason(request.reason); const repository = load(); const target = batch(repository, batchId); const original = item(repository, batchId, itemId); const key = resultKey(`replace:${itemId}`, request.clientRequestId); const previous = recalled<RunDataItem>(repository, key); if (previous) return clone(previous);
      if (original.status !== 'READY') throw new Error('Only ready data can be replaced'); original.status = 'DISABLED'; original.updatedAt = now(); audit(repository, target, 'DATA_REPLACED', 'DATA_ITEM', itemId, request.clientRequestId, reason);
      const replacement = { ...clone(original), id: nextId(repository, 'item'), status: 'READY' as const, generationRevision: original.generationRevision + 1, generationParameters: clone(request.generationParameters), replacementOfItemId: itemId, auditHistory: [], createdAt: now(), updatedAt: now() }; repository.items.push(replacement); audit(repository, target, 'REPLACEMENT_GENERATION_READY', 'DATA_ITEM', replacement.id, request.clientRequestId, reason); remember(repository, key, replacement); save(repository); return clone(replacement);
    },
    async retryData(batchId, itemId, request) {
      const reason = requiredReason(request.reason); const repository = load(); const target = batch(repository, batchId); const failed = item(repository, batchId, itemId); const key = resultKey(`retry:${itemId}`, request.clientRequestId); const previous = recalled<RunDataItem>(repository, key); if (previous) return clone(previous);
      if (failed.status !== 'GENERATION_FAILED') throw new Error('Only failed data can be retried'); audit(repository, target, 'DATA_RETRY_REQUESTED', 'DATA_ITEM', itemId, request.clientRequestId, reason);
      const retry = { ...clone(failed), id: nextId(repository, 'item'), status: 'READY' as const, generationRevision: failed.generationRevision + 1, replacementOfItemId: itemId, auditHistory: [], createdAt: now(), updatedAt: now() }; repository.items.push(retry); audit(repository, target, 'RETRY_GENERATION_READY', 'DATA_ITEM', retry.id, request.clientRequestId, reason); remember(repository, key, retry); save(repository); return clone(retry);
    },
    async publishBatch(batchId, clientRequestId) { const repository = load(); const target = batch(repository, batchId); const key = resultKey(`publish:${batchId}`, clientRequestId); const previous = recalled<WorkflowRunBatch>(repository, key); if (previous) return clone(previous); const preflight = createPreflight(target, repository.items); if (!preflight.valid) throw new Error('Batch preflight failed'); target.status = target.plannedStartTime ? 'SCHEDULED' : 'READY'; target.updatedAt = now(); audit(repository, target, 'BATCH_PUBLISHED', 'BATCH', batchId, clientRequestId); remember(repository, key, target); save(repository); return clone(target); },
    async cancelBatch(batchId, request) { const reason = requiredReason(request.reason); const repository = load(); const target = batch(repository, batchId); const key = resultKey(`cancel:${batchId}`, request.clientRequestId); const previous = recalled<WorkflowRunBatch>(repository, key); if (previous) return clone(previous); target.status = 'CANCELLED'; target.updatedAt = now(); repository.items.filter((value) => value.batchId === batchId && value.status === 'READY').forEach((value) => { value.status = 'DISABLED'; value.updatedAt = target.updatedAt; }); audit(repository, target, 'BATCH_CANCELLED', 'BATCH', batchId, request.clientRequestId, reason); remember(repository, key, target); save(repository); return clone(target); },
    createClientRequestId: requestIdFactory
  };

  async function mutateItem(action: string, eventType: string, status: RunDataItem['status'], batchId: string, itemId: string, request: ReasonedCommand): Promise<RunDataItem> {
    const reason = requiredReason(request.reason); const repository = load(); const target = batch(repository, batchId); const dataItem = item(repository, batchId, itemId); const key = resultKey(`${action}:${itemId}`, request.clientRequestId); const previous = recalled<RunDataItem>(repository, key); if (previous) return clone(previous);
    if (dataItem.status !== 'READY') throw new Error('Only ready unused data can be disabled'); dataItem.status = status; dataItem.updatedAt = now(); audit(repository, target, eventType, 'DATA_ITEM', itemId, request.clientRequestId, reason); remember(repository, key, dataItem); save(repository); return clone(dataItem);
  }
}

function createItems(repository: MockRepository, job: RunDataGenerationJob, plan: UnitDataPlanInput, nextId: (repository: MockRepository, kind: string) => string, timestamp: string) {
  for (const [kind, count] of [['FORMAL', plan.formalCount], ['SPARE', plan.spareCount]] as const) for (let index = 1; index <= count; index++) repository.items.push({ id: nextId(repository, 'item'), batchId: job.batchId, unitId: plan.unitId, kind, status: 'GENERATING', generationRevision: 1, businessReferenceMasked: `MASKED-${plan.unitId}-${index}`, generationParameters: { ...clone(plan.generationParameters), __mockIndex: index }, generationJobId: job.id, auditHistory: [], createdAt: timestamp, updatedAt: timestamp });
}
function dataItemIndex(item: RunDataItem) { return Number(item.generationParameters.__mockIndex) || 0; }
function createPreflight(batch: WorkflowRunBatch, items: RunDataItem[]): RunBatchPreflight {
  const unitIds = batch.unitPlans.map((plan) => plan.unitId);
  const quotas = unitIds.flatMap((unitId) => batch.roleGroups.map((group) => ({ unitId, groupKey: group.groupKey, memberCount: batch.members.filter((member) => member.unitId === unitId && member.groupKey === group.groupKey).length, requiredFormalCount: batch.members.filter((member) => member.unitId === unitId && member.groupKey === group.groupKey).length })));
  const issues: RunPreflightIssue[] = quotas.flatMap((quota) => {
    const groupMembers = batch.members.filter((member) => member.unitId === quota.unitId && member.groupKey === quota.groupKey);
    const formalCount = batch.unitPlans.find((plan) => plan.unitId === quota.unitId)?.formalCount ?? 0;
    return [
      ...(formalCount < quota.requiredFormalCount ? [{ code: 'FORMAL_COUNT_BELOW_GROUP_SIZE', unitId: quota.unitId, groupKey: quota.groupKey, message: `Unit ${quota.unitId} needs at least ${quota.requiredFormalCount} formal data items` }] : []),
      ...(groupMembers.length === 0 ? [{ code: 'MISSING_GROUP_MEMBER', unitId: quota.unitId, groupKey: quota.groupKey, message: `Unit ${quota.unitId} is missing ${quota.groupKey} members` }] : []),
      ...(groupMembers.some((member) => Object.keys(member.externalAccountMapping).length === 0) ? [{ code: 'INVALID_EXTERNAL_ACCOUNT_MAPPING', unitId: quota.unitId, groupKey: quota.groupKey, message: `Unit ${quota.unitId} has an empty business identity mapping` }] : [])
    ];
  });
  for (const plan of batch.unitPlans) {
    const readyFormal = items.filter((item) => item.batchId === batch.id && item.unitId === plan.unitId && item.kind === 'FORMAL' && item.status === 'READY').length;
    if (readyFormal < plan.formalCount) issues.push({ code: 'DATA_NOT_READY', unitId: plan.unitId, message: `Unit ${plan.unitId} needs ${plan.formalCount} ready formal data items` });
  }
  return { valid: issues.length === 0, issues, memberStageQuotas: quotas, checkedAt: new Date().toISOString() };
}
function groupedCounts(members: WorkflowRunBatch['members']) { return members.reduce<Record<string, number>>((counts, member) => { const key = `${member.unitId}|${member.groupKey}`; counts[key] = (counts[key] ?? 0) + 1; return counts; }, {}); }
const SAFE_EXTERNAL_ACCOUNT_MAPPING_KEYS = new Set([
  'account', 'accountId', 'externalAccountId', 'userId', 'externalUserId',
  'username', 'loginName', 'roleId', 'orgId'
]);

/** Keys are exact, case-sensitive lower-camel identifiers; every other key is discarded. */
function sanitizeExternalAccountMapping(mapping: Record<string, string>): Record<string, string> {
  return Object.fromEntries(
    Object.entries(mapping)
      .filter(([key, value]) => SAFE_EXTERNAL_ACCOUNT_MAPPING_KEYS.has(key) && typeof value === 'string' && value.trim().length > 0)
      .map(([key, value]) => [key, value.trim()])
  );
}
function requiredReason(reason: string): string { const trimmed = reason.trim(); if (!trimmed) throw new Error('reason is required'); return trimmed; }
function required<T>(value: T | undefined, message: string): T { if (!value) throw new Error(message); return value; }
function clone<T>(value: T): T { return JSON.parse(JSON.stringify(value)) as T; }
function defaultRequestId() { return typeof crypto !== 'undefined' && crypto.randomUUID ? crypto.randomUUID() : `request-${Date.now()}-${Math.random().toString(16).slice(2)}`; }
function readRepository(storage: Storage): MockRepository {
  const raw = storage.getItem(RUN_BATCH_MOCK_STORAGE_KEY);
  if (!raw) return { workflows: [seedWorkflow()], batches: [], jobs: [], items: [], commandResults: {}, sequence: 0, nextId: 0 };
  const repository = JSON.parse(raw) as MockRepository;
  sanitizeRepository(repository);
  storage.setItem(RUN_BATCH_MOCK_STORAGE_KEY, JSON.stringify(repository));
  return repository;
}

function sanitizeRepository(repository: MockRepository) {
  repository.batches.forEach(sanitizeBatchMappings);
  Object.values(repository.commandResults).forEach((value) => {
    if (isWorkflowRunBatch(value)) sanitizeBatchMappings(value);
  });
}

function sanitizeBatchMappings(batch: WorkflowRunBatch) {
  batch.members = batch.members.map((member) => ({
    ...member,
    externalAccountMapping: sanitizeExternalAccountMapping(member.externalAccountMapping)
  }));
}

function isWorkflowRunBatch(value: unknown): value is WorkflowRunBatch {
  return typeof value === 'object' && value !== null && Array.isArray((value as WorkflowRunBatch).members);
}
function seedWorkflow(): PublishedWorkflowOption { return { id: 'workflow-published-1', taskId: 'task-purchase-1', name: 'Three-stage purchasing workflow', versionNo: 1, snapshotDigest: 'sha256:mock-workflow-v1', teachingDataTemplateId: 'template-1', scenarioVersion: 'scenario-v1', objectiveMaxScore: 80, subjectiveMaxScore: 20, roleGroups: [{ id: 'group-creator', groupKey: 'CREATOR', name: 'Creator', sequenceNo: 1 }, { id: 'group-reviewer', groupKey: 'REVIEWER', name: 'Reviewer', sequenceNo: 2 }, { id: 'group-approver', groupKey: 'APPROVER', name: 'Approver', sequenceNo: 3 }], stages: [{ id: 'stage-create', stageKey: 'CREATE', name: 'Create request', sequenceNo: 1, roleGroupId: 'group-creator', externalRoleId: 'creator', externalOrgId: 'org-1' }, { id: 'stage-review', stageKey: 'REVIEW', name: 'Review request', sequenceNo: 2, roleGroupId: 'group-reviewer', externalRoleId: 'reviewer', externalOrgId: 'org-1' }, { id: 'stage-approve', stageKey: 'APPROVE', name: 'Approve request', sequenceNo: 3, roleGroupId: 'group-approver', externalRoleId: 'approver', externalOrgId: 'org-1' }], rubricItems: [{ id: 'rubric-quality', itemKey: 'QUALITY', name: 'Quality', sequenceNo: 1, maxScore: 20, required: true }] }; }
