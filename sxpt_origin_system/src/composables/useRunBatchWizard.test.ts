import { describe, expect, it, vi } from 'vitest';
import type { RunBatchApi } from '../api/v1/runBatchApi';
import type {
  RunBatchPreflight,
  RunDataGenerationJob,
  WorkflowRunBatch
} from '../types/runBatch';
import { createRunBatchWizard } from './useRunBatchWizard';

const batchFixture = (): WorkflowRunBatch => ({
  id: 'batch-1', workflowVersionId: 'workflow-1', workflowSnapshotDigest: 'digest',
  batchName: 'July exam', runType: 'EXAM', status: 'DRAFT', objectiveMaxScore: 80,
  subjectiveMaxScore: 20, roleGroups: [], stages: [], rubricItems: [], members: [],
  unitPlans: [], auditHistory: [], createdAt: '2026-07-24T00:00:00Z', updatedAt: '2026-07-24T00:00:00Z'
});

const jobFixture = (overrides: Partial<RunDataGenerationJob> = {}): RunDataGenerationJob => ({
  id: 'job-1', batchId: 'batch-1', clientRequestId: 'generation-1', status: 'PENDING',
  requestedCount: 3, readyCount: 0, failedCount: 0,
  createdAt: '2026-07-24T00:00:00Z', updatedAt: '2026-07-24T00:00:00Z', ...overrides
});

function apiWith(overrides: Partial<RunBatchApi> = {}): RunBatchApi {
  const batch = batchFixture();
  return {
    listPublishedWorkflows: vi.fn().mockResolvedValue([]),
    createBatch: vi.fn().mockResolvedValue(batch),
    getBatch: vi.fn().mockResolvedValue(batch),
    saveMembers: vi.fn().mockResolvedValue(batch),
    saveUnitPlans: vi.fn().mockResolvedValue(batch),
    preflight: vi.fn().mockResolvedValue({ valid: true, issues: [], memberStageQuotas: [], checkedAt: '2026-07-24T00:00:00Z' } satisfies RunBatchPreflight),
    startDataGeneration: vi.fn().mockResolvedValue(jobFixture()),
    getGenerationJob: vi.fn().mockResolvedValue(jobFixture()),
    advanceGenerationJob: vi.fn().mockResolvedValue(jobFixture({ status: 'COMPLETED', readyCount: 3 })),
    listDataItems: vi.fn().mockResolvedValue({ items: [], page: 1, pageSize: 20, total: 0 }),
    getDataItem: vi.fn(), disableData: vi.fn(), promoteData: vi.fn(), replaceData: vi.fn(), retryData: vi.fn(),
    publishBatch: vi.fn().mockResolvedValue(batch), cancelBatch: vi.fn().mockResolvedValue(batch),
    createClientRequestId: vi.fn().mockReturnValue('request-1'), ...overrides
  };
}

describe('createRunBatchWizard', () => {
  it('preserves one learner in multiple role groups', () => {
    const wizard = createRunBatchWizard(apiWith(), 'EXAM');
    wizard.setMembers([
      { learnerId: 'learner-1', groupKey: 'CREATOR', unitId: 'unit-1', externalAccountMapping: { accountId: 'creator-1' } },
      { learnerId: 'learner-1', groupKey: 'REVIEWER', unitId: 'unit-1', externalAccountMapping: { accountId: 'reviewer-1' } }
    ]);

    expect(wizard.members.value).toHaveLength(2);
    expect(wizard.members.value.map((member) => member.groupKey)).toEqual(['CREATOR', 'REVIEWER']);
  });

  it('reloads the saved batch after each wizard mutation', async () => {
    const saved = { ...batchFixture(), updatedAt: '2026-07-24T01:00:00Z' };
    const getBatch = vi.fn().mockResolvedValue(saved);
    const api = apiWith({ saveMembers: vi.fn().mockResolvedValue(batchFixture()), getBatch });
    const wizard = createRunBatchWizard(api, 'EXAM');
    await wizard.loadBatch('batch-1');
    getBatch.mockClear();
    wizard.setMembers([{ learnerId: 'learner-1', groupKey: 'CREATOR', unitId: 'unit-1', externalAccountMapping: {} }]);

    await wizard.saveMembers();

    expect(getBatch).toHaveBeenCalledWith('batch-1');
    expect(wizard.batch.value).toEqual(saved);
  });

  it('keeps one clientRequestId when a save is retried', async () => {
    const saveMembers = vi.fn().mockRejectedValueOnce(new Error('network')).mockResolvedValueOnce(batchFixture());
    const api = apiWith({ saveMembers, createClientRequestId: vi.fn().mockReturnValue('members-intent-1') });
    const wizard = createRunBatchWizard(api, 'EXAM');
    await wizard.loadBatch('batch-1');
    wizard.setMembers([{ learnerId: 'learner-1', groupKey: 'CREATOR', unitId: 'unit-1', externalAccountMapping: {} }]);

    await expect(wizard.saveMembers()).rejects.toThrow('network');
    await wizard.saveMembers();

    expect(saveMembers).toHaveBeenNthCalledWith(1, 'batch-1', expect.objectContaining({ clientRequestId: 'members-intent-1' }));
    expect(saveMembers).toHaveBeenNthCalledWith(2, 'batch-1', expect.objectContaining({ clientRequestId: 'members-intent-1' }));
    expect(api.createClientRequestId).toHaveBeenCalledTimes(1);
  });

  it('does not enable publish while preflight is invalid', async () => {
    const invalid = { valid: false, issues: [{ code: 'BLOCKED', message: 'not ready' }], memberStageQuotas: [], checkedAt: '2026-07-24T00:00:00Z' };
    const wizard = createRunBatchWizard(apiWith({ preflight: vi.fn().mockResolvedValue(invalid) }), 'EXAM');
    await wizard.loadBatch('batch-1');
    wizard.generationJob.value = jobFixture({ status: 'COMPLETED', readyCount: 3 });
    await wizard.runPreflight();

    expect(wizard.canPublish.value).toBe(false);
  });

  it('shows generation requested ready and failed counts', async () => {
    const wizard = createRunBatchWizard(apiWith({ startDataGeneration: vi.fn().mockResolvedValue(jobFixture({ status: 'COMPLETED_WITH_FAILURES', readyCount: 2, failedCount: 1 })) }), 'EXAM');
    await wizard.loadBatch('batch-1');
    await wizard.startGeneration();

    expect(wizard.generationSummary.value).toEqual({ requestedCount: 3, readyCount: 2, failedCount: 1, jobStatus: 'COMPLETED_WITH_FAILURES' });
  });

  it('does not let a stale preflight response enable publish after configuration changes', async () => {
    const deferred = deferredPromise<RunBatchPreflight>();
    const wizard = createRunBatchWizard(apiWith({ preflight: vi.fn().mockReturnValue(deferred.promise) }), 'EXAM');
    await wizard.loadBatch('batch-1');
    wizard.generationJob.value = jobFixture({ status: 'COMPLETED', readyCount: 3 });
    const pending = wizard.runPreflight();

    wizard.setUnitPlans([{ unitId: 'unit-1', formalCount: 1, spareCount: 0, teachingDataTemplateId: 'template-1', scenarioVersion: 'v1', generationParameters: {} }]);
    deferred.resolve({ valid: true, issues: [], memberStageQuotas: [], checkedAt: '2026-07-24T01:00:00Z' });
    await pending;

    expect(wizard.preflight.value).toBeNull();
    expect(wizard.canPublish.value).toBe(false);
  });

  it('restores the persisted latest generation job when loading a batch', async () => {
    const persisted = { ...batchFixture(), latestGenerationJobId: 'job-1' };
    const api = apiWith({
      getBatch: vi.fn().mockResolvedValue(persisted),
      getGenerationJob: vi.fn().mockResolvedValue(jobFixture({ status: 'COMPLETED_WITH_FAILURES', readyCount: 2, failedCount: 1 }))
    });
    const wizard = createRunBatchWizard(api, 'EXAM');

    await wizard.loadBatch('batch-1');

    expect(api.getGenerationJob).toHaveBeenCalledWith('batch-1', 'job-1');
    expect(wizard.generationSummary.value).toEqual({ requestedCount: 3, readyCount: 2, failedCount: 1, jobStatus: 'COMPLETED_WITH_FAILURES' });
    await wizard.runPreflight();
    expect(wizard.canPublish.value).toBe(true);
  });

  it('only permits publish from draft or data-preparing batches and turns it off after publish or cancel', async () => {
    const valid = { valid: true, issues: [], memberStageQuotas: [], checkedAt: '2026-07-24T00:00:00Z' };
    const draft = batchFixture();
    const published = { ...draft, status: 'READY' as const };
    const cancelled = { ...draft, status: 'CANCELLED' as const };
    const getBatch = vi.fn().mockResolvedValueOnce(draft).mockResolvedValueOnce(published).mockResolvedValueOnce(draft).mockResolvedValueOnce(cancelled);
    const wizard = createRunBatchWizard(apiWith({ getBatch, preflight: vi.fn().mockResolvedValue(valid), publishBatch: vi.fn().mockResolvedValue(published), cancelBatch: vi.fn().mockResolvedValue(cancelled) }), 'EXAM');
    await wizard.loadBatch('batch-1');
    wizard.generationJob.value = jobFixture({ status: 'COMPLETED' });
    await wizard.runPreflight();
    expect(wizard.canPublish.value).toBe(true);
    await wizard.publish();
    expect(wizard.canPublish.value).toBe(false);

    await wizard.loadBatch('batch-1');
    wizard.generationJob.value = jobFixture({ status: 'COMPLETED' });
    await wizard.runPreflight();
    await wizard.cancel('teacher cancelled');
    expect(wizard.canPublish.value).toBe(false);
  });

  it('keeps loading true until every overlapping load finishes and only exposes the newest error', async () => {
    const workflows = deferredPromise<[]>();
    const batch = deferredPromise<WorkflowRunBatch>();
    const oldError = new Error('old request failed');
    const api = apiWith({ listPublishedWorkflows: vi.fn().mockReturnValue(workflows.promise), getBatch: vi.fn().mockReturnValue(batch.promise) });
    const wizard = createRunBatchWizard(api, 'EXAM');
    const first = wizard.loadPublishedWorkflows();
    const second = wizard.loadBatch('batch-1');

    workflows.reject(oldError);
    await expect(first).rejects.toBe(oldError);
    expect(wizard.loading.value).toBe(true);
    expect(wizard.error.value).toBeNull();
    batch.resolve(batchFixture());
    await second;
    expect(wizard.loading.value).toBe(false);
    expect(wizard.error.value).toBeNull();
  });

  it('does not let an old batch mutation overwrite a newer loaded batch', async () => {
    const pendingSave = deferredPromise<WorkflowRunBatch>();
    const first = batchFixture();
    const second = { ...batchFixture(), id: 'batch-2', batchName: 'new batch' };
    const api = apiWith({ getBatch: vi.fn().mockImplementation(async (id) => id === 'batch-2' ? second : first), saveMembers: vi.fn().mockReturnValue(pendingSave.promise) });
    const wizard = createRunBatchWizard(api, 'EXAM');
    await wizard.loadBatch('batch-1');
    wizard.setMembers([{ learnerId: 'learner-1', groupKey: 'CREATOR', unitId: 'unit-1', externalAccountMapping: {} }]);
    const oldSave = wizard.saveMembers();
    await wizard.loadBatch('batch-2');
    pendingSave.resolve(first);
    await oldSave;

    expect(wizard.batch.value).toEqual(second);
  });

  it('isolates same-action request IDs by batch and starts a new member intent for changed members', async () => {
    const firstFailure = new Error('lost response');
    const first = batchFixture();
    const second = { ...batchFixture(), id: 'batch-2' };
    const saveMembers = vi.fn().mockRejectedValueOnce(firstFailure).mockResolvedValue(first).mockResolvedValue(second);
    const api = apiWith({ getBatch: vi.fn().mockImplementation(async (id) => id === 'batch-2' ? second : first), saveMembers, createClientRequestId: vi.fn().mockReturnValueOnce('batch-1-first').mockReturnValueOnce('batch-1-edited').mockReturnValueOnce('batch-2-members') });
    const wizard = createRunBatchWizard(api, 'EXAM');
    await wizard.loadBatch('batch-1');
    wizard.setMembers([{ learnerId: 'learner-1', groupKey: 'CREATOR', unitId: 'unit-1', externalAccountMapping: {} }]);
    await expect(wizard.saveMembers()).rejects.toBe(firstFailure);
    wizard.setMembers([{ learnerId: 'learner-1', groupKey: 'REVIEWER', unitId: 'unit-1', externalAccountMapping: {} }]);
    await wizard.saveMembers();
    await wizard.loadBatch('batch-2');
    await wizard.saveMembers();

    expect(saveMembers).toHaveBeenNthCalledWith(1, 'batch-1', expect.objectContaining({ clientRequestId: 'batch-1-first' }));
    expect(saveMembers).toHaveBeenNthCalledWith(2, 'batch-1', expect.objectContaining({ clientRequestId: 'batch-1-edited' }));
    expect(saveMembers).toHaveBeenNthCalledWith(3, 'batch-2', expect.objectContaining({ clientRequestId: 'batch-2-members' }));
  });

  it('uses stable IDs for identical create payloads and new IDs when create semantics change', async () => {
    const lost = new Error('lost');
    const createBatch = vi.fn().mockRejectedValueOnce(lost).mockResolvedValueOnce(batchFixture()).mockResolvedValueOnce(batchFixture());
    const api = apiWith({ createBatch, createClientRequestId: vi.fn().mockReturnValueOnce('create-1').mockReturnValueOnce('create-2') });
    const wizard = createRunBatchWizard(api, 'EXAM');
    const payload = { workflowVersionId: 'workflow-1', batchName: 'Exam', plannedStartTime: '2026-07-25T00:00:00Z' };

    await expect(wizard.createBatch(payload)).rejects.toBe(lost);
    await wizard.createBatch({ ...payload });
    await wizard.createBatch({ ...payload, batchName: 'Renamed exam' });

    expect(createBatch).toHaveBeenNthCalledWith(1, expect.objectContaining({ clientRequestId: 'create-1' }));
    expect(createBatch).toHaveBeenNthCalledWith(2, expect.objectContaining({ clientRequestId: 'create-1' }));
    expect(createBatch).toHaveBeenNthCalledWith(3, expect.objectContaining({ clientRequestId: 'create-2' }));
  });

  it('does not overwrite edited member or unit-plan buffers when a same-context save reloads', async () => {
    const pendingMembers = deferredPromise<WorkflowRunBatch>();
    const pendingPlans = deferredPromise<WorkflowRunBatch>();
    const server = batchFixture();
    const api = apiWith({ getBatch: vi.fn().mockResolvedValue(server), saveMembers: vi.fn().mockReturnValue(pendingMembers.promise), saveUnitPlans: vi.fn().mockReturnValue(pendingPlans.promise) });
    const wizard = createRunBatchWizard(api, 'EXAM');
    await wizard.loadBatch('batch-1');
    wizard.setMembers([{ learnerId: 'old', groupKey: 'CREATOR', unitId: 'unit-1', externalAccountMapping: {} }]);
    const memberSave = wizard.saveMembers();
    wizard.setMembers([{ learnerId: 'edited', groupKey: 'REVIEWER', unitId: 'unit-1', externalAccountMapping: {} }]);
    pendingMembers.resolve(server);
    await memberSave;
    expect(wizard.members.value.map((member) => member.learnerId)).toEqual(['edited']);

    wizard.setUnitPlans([{ unitId: 'unit-1', formalCount: 1, spareCount: 0, teachingDataTemplateId: 'template-1', scenarioVersion: 'v1', generationParameters: {} }]);
    const planSave = wizard.saveUnitPlans();
    wizard.setUnitPlans([{ unitId: 'unit-1', formalCount: 2, spareCount: 1, teachingDataTemplateId: 'template-1', scenarioVersion: 'v2', generationParameters: {} }]);
    pendingPlans.resolve(server);
    await planSave;
    expect(wizard.unitPlans.value[0]).toMatchObject({ formalCount: 2, scenarioVersion: 'v2' });
  });

  it('does not let an older restored generation job overwrite a newer generation job', async () => {
    const restore = deferredPromise<RunDataGenerationJob>();
    const restoredBatch = { ...batchFixture(), latestGenerationJobId: 'old-job' };
    const newJob = jobFixture({ id: 'new-job', status: 'PENDING' });
    const api = apiWith({ getBatch: vi.fn().mockResolvedValue(restoredBatch), getGenerationJob: vi.fn().mockReturnValue(restore.promise), startDataGeneration: vi.fn().mockResolvedValue(newJob) });
    const wizard = createRunBatchWizard(api, 'EXAM');
    const loading = wizard.loadBatch('batch-1');
    await Promise.resolve();
    await wizard.startGeneration();
    restore.resolve(jobFixture({ id: 'old-job', status: 'COMPLETED' }));
    await loading;

    expect(wizard.generationJob.value?.id).toBe('new-job');
  });
});

function deferredPromise<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((onResolve, onReject) => { resolve = onResolve; reject = onReject; });
  return { promise, resolve, reject };
}
