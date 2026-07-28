import { describe, expect, it, vi } from 'vitest';
import { createMockRunBatchApi } from './mockRunBatchApi';
import { createRunBatchApiFromConfig } from './runBatchApiFactory';
import type { AuthenticatedApiClient } from './authenticatedApiClient';
import type { CreateRunBatchRequest, SaveGroupMembersRequest } from '../../types/runBatch';

class MemoryStorage implements Storage {
  private readonly values = new Map<string, string>();
  get length() { return this.values.size; }
  clear() { this.values.clear(); }
  getItem(key: string) { return this.values.get(key) ?? null; }
  key(index: number) { return [...this.values.keys()][index] ?? null; }
  removeItem(key: string) { this.values.delete(key); }
  setItem(key: string, value: string) { this.values.set(key, value); }
}

const createRequest: CreateRunBatchRequest = {
  clientRequestId: 'create-1', workflowVersionId: 'workflow-published-1', runType: 'EXAM',
  batchName: 'Mock exam', plannedStartTime: '2026-07-30T09:00:00Z', plannedEndTime: '2026-07-30T11:00:00Z'
};

async function createdApi() {
  const api = createMockRunBatchApi(new MemoryStorage(), () => 'generated-id');
  return { api, batch: await api.createBatch(createRequest) };
}

function completeMembers() {
  return [
    { learnerId: 'learner-1', groupKey: 'CREATOR', unitId: 'unit-1', externalAccountMapping: { accountId: 'creator-01', roleId: 'creator' } },
    { learnerId: 'learner-2', groupKey: 'REVIEWER', unitId: 'unit-1', externalAccountMapping: { accountId: 'reviewer-01', roleId: 'reviewer' } },
    { learnerId: 'learner-3', groupKey: 'APPROVER', unitId: 'unit-1', externalAccountMapping: { accountId: 'approver-01', roleId: 'approver' } }
  ];
}

const unitPlan = {
  unitId: 'unit-1', formalCount: 1, spareCount: 0,
  teachingDataTemplateId: 'template-1', scenarioVersion: 'scenario-v1', generationParameters: {}
};

async function prepareReadyItem() {
  const { api, batch } = await createdApi();
  await api.saveMembers(batch.id, { clientRequestId: 'members-complete', members: completeMembers() });
  await api.saveUnitPlans(batch.id, { clientRequestId: 'plans-ready', unitPlans: [unitPlan] });
  const job = await api.startDataGeneration(batch.id, { clientRequestId: 'job-ready', unitPlans: [unitPlan] });
  await api.advanceGenerationJob(batch.id, job.id, 'advance-ready');
  return { api, batch, item: (await api.listDataItems(batch.id)).items[0] };
}

describe('createMockRunBatchApi', () => {
  it('keeps one learner in multiple groups', async () => {
    const { api, batch } = await createdApi();
    const request: SaveGroupMembersRequest = {
      clientRequestId: 'members-1',
      members: [
        { learnerId: 'learner-1', groupKey: 'CREATOR', unitId: 'unit-1', externalAccountMapping: { account: 'creator-01' } },
        { learnerId: 'learner-1', groupKey: 'REVIEWER', unitId: 'unit-1', externalAccountMapping: { account: 'reviewer-01' } }
      ]
    };

    const saved = await api.saveMembers(batch.id, request);
    saved.members[0].externalAccountMapping.account = 'changed by UI';

    expect(saved.members).toHaveLength(2);
    expect((await api.getBatch(batch.id)).members.map((member) => member.groupKey)).toEqual(['CREATOR', 'REVIEWER']);
    expect((await api.getBatch(batch.id)).members[0].externalAccountMapping.account).toBe('creator-01');
  });

  it('persists only trimmed allowlisted external account mapping fields', async () => {
    const storage = new MemoryStorage();
    const api = createMockRunBatchApi(storage, () => 'generated-id');
    const batch = await api.createBatch(createRequest);

    const saved = await api.saveMembers(batch.id, {
      clientRequestId: 'members-sensitive',
      members: [{ learnerId: 'learner-1', groupKey: 'CREATOR', unitId: 'unit-1', externalAccountMapping: { account: ' account-name ', accountId: ' account-1 ', roleId: ' creator ', password: 'do-not-store', accessToken: 'do-not-store', authorization: 'do-not-store', apiKey: 'api-key', privateKey: 'private-key', bearer: 'bearer-value', unknownIdentityField: 'unknown-value' } }]
    });

    expect(saved.members[0].externalAccountMapping).toEqual({ account: 'account-name', accountId: 'account-1', roleId: 'creator' });
    expect(await api.getBatch(batch.id)).toMatchObject({ members: [expect.objectContaining({ externalAccountMapping: { account: 'account-name', accountId: 'account-1', roleId: 'creator' } })] });
    const persisted = storage.getItem('sxpt.run-batch.mock.v1') ?? '';
    for (const forbiddenValue of ['do-not-store', 'api-key', 'private-key', 'bearer-value', 'unknown-value']) expect(persisted).not.toContain(forbiddenValue);
  });

  it('scrubs unallowlisted mappings from an existing mock repository before returning them', async () => {
    const storage = new MemoryStorage();
    const api = createMockRunBatchApi(storage, () => 'generated-id');
    const batch = await api.createBatch(createRequest);
    await api.saveMembers(batch.id, { clientRequestId: 'members-legacy', members: [{ learnerId: 'learner-1', groupKey: 'CREATOR', unitId: 'unit-1', externalAccountMapping: { accountId: 'account-1', roleId: 'creator' } }] });
    storage.setItem('sxpt.run-batch.mock.v1', (storage.getItem('sxpt.run-batch.mock.v1') ?? '').replace('"roleId":"creator"', '"roleId":"creator","apiKey":"legacy-api-key"'));

    const reloaded = await createMockRunBatchApi(storage, () => 'generated-id').getBatch(batch.id);

    expect(reloaded.members[0].externalAccountMapping).toEqual({ accountId: 'account-1', roleId: 'creator' });
    expect(storage.getItem('sxpt.run-batch.mock.v1')).not.toContain('legacy-api-key');
  });

  it('returns the same batch for the same create clientRequestId', async () => {
    const api = createMockRunBatchApi(new MemoryStorage(), () => 'generated-id');
    const first = await api.createBatch(createRequest);
    const repeated = await api.createBatch({ ...createRequest, batchName: 'ignored duplicate' });

    expect(repeated).toEqual(first);
    expect(repeated.id).toBe(first.id);
  });

  it('blocks a unit below its largest group size', async () => {
    const { api, batch } = await createdApi();
    await api.saveMembers(batch.id, {
      clientRequestId: 'members-1',
      members: [
        { learnerId: 'learner-1', groupKey: 'CREATOR', unitId: 'unit-1', externalAccountMapping: {} },
        { learnerId: 'learner-2', groupKey: 'CREATOR', unitId: 'unit-1', externalAccountMapping: {} },
        { learnerId: 'learner-3', groupKey: 'REVIEWER', unitId: 'unit-1', externalAccountMapping: {} }
      ]
    });
    await api.saveUnitPlans(batch.id, { clientRequestId: 'plans-1', unitPlans: [{ unitId: 'unit-1', formalCount: 1, spareCount: 0, teachingDataTemplateId: 'template-1', scenarioVersion: 'scenario-v1', generationParameters: {} }] });

    await expect(api.preflight(batch.id)).resolves.toMatchObject({ valid: false, issues: expect.arrayContaining([expect.objectContaining({ unitId: 'unit-1', code: 'FORMAL_COUNT_BELOW_GROUP_SIZE' })]) });
  });

  it('blocks configured units that omit a workflow group or a usable external account mapping', async () => {
    const { api, batch } = await createdApi();
    await api.saveMembers(batch.id, {
      clientRequestId: 'members-invalid',
      members: [{ learnerId: 'learner-1', groupKey: 'CREATOR', unitId: 'unit-1', externalAccountMapping: { password: 'removed' } }]
    });
    await api.saveUnitPlans(batch.id, { clientRequestId: 'plans-invalid', unitPlans: [unitPlan] });

    await expect(api.preflight(batch.id)).resolves.toMatchObject({
      valid: false,
      issues: expect.arrayContaining([
        expect.objectContaining({ code: 'MISSING_GROUP_MEMBER', unitId: 'unit-1' }),
        expect.objectContaining({ code: 'INVALID_EXTERNAL_ACCOUNT_MAPPING', unitId: 'unit-1', groupKey: 'CREATOR' })
      ])
    });
  });

  it('blocks publish until each configured unit has actual ready formal data', async () => {
    const { api, batch } = await createdApi();
    await api.saveMembers(batch.id, { clientRequestId: 'members-complete', members: completeMembers() });
    await api.saveUnitPlans(batch.id, { clientRequestId: 'plans-ready', unitPlans: [unitPlan] });

    await expect(api.preflight(batch.id)).resolves.toMatchObject({ valid: false, issues: expect.arrayContaining([expect.objectContaining({ code: 'DATA_NOT_READY', unitId: 'unit-1' })]) });
    await expect(api.publishBatch(batch.id, 'publish-before-data')).rejects.toThrow('Batch preflight failed');

    const job = await api.startDataGeneration(batch.id, { clientRequestId: 'job-ready', unitPlans: [unitPlan] });
    await api.advanceGenerationJob(batch.id, job.id, 'advance-ready');

    await expect(api.preflight(batch.id)).resolves.toMatchObject({ valid: true, issues: [] });
    await expect(api.publishBatch(batch.id, 'publish-after-data')).resolves.toMatchObject({ status: 'SCHEDULED' });
  });

  it('keeps successful items when one mock generation item fails', async () => {
    const { api, batch } = await createdApi();
    await api.saveUnitPlans(batch.id, { clientRequestId: 'plans-1', unitPlans: [{ unitId: 'unit-1', formalCount: 3, spareCount: 0, teachingDataTemplateId: 'template-1', scenarioVersion: 'scenario-v1', generationParameters: { failIndexes: [2] } }] });
    const job = await api.startDataGeneration(batch.id, { clientRequestId: 'job-1', unitPlans: (await api.getBatch(batch.id)).unitPlans });
    const advanced = await api.advanceGenerationJob(batch.id, job.id, 'advance-1');
    const items = await api.listDataItems(batch.id);

    expect(advanced.readyCount).toBe(2);
    expect(advanced.failedCount).toBe(1);
    expect(items.items.map((item) => item.status)).toEqual(['READY', 'GENERATION_FAILED', 'READY']);
  });

  it('persists the latest generation job identifier on its batch', async () => {
    const { api, batch } = await createdApi();
    const job = await api.startDataGeneration(batch.id, { clientRequestId: 'job-identifier', unitPlans: [unitPlan] });

    await expect(api.getBatch(batch.id)).resolves.toMatchObject({ latestGenerationJobId: job.id });
  });

  it('retries a failed item as a new generation revision', async () => {
    const { api, batch } = await createdApi();
    await api.saveUnitPlans(batch.id, { clientRequestId: 'plans-1', unitPlans: [{ unitId: 'unit-1', formalCount: 1, spareCount: 0, teachingDataTemplateId: 'template-1', scenarioVersion: 'scenario-v1', generationParameters: { failIndexes: [1] } }] });
    const job = await api.startDataGeneration(batch.id, { clientRequestId: 'job-1', unitPlans: (await api.getBatch(batch.id)).unitPlans });
    await api.advanceGenerationJob(batch.id, job.id, 'advance-1');
    const failed = (await api.listDataItems(batch.id)).items[0];
    const retried = await api.retryData(batch.id, failed.id, { clientRequestId: 'retry-1', reason: 'retry transient failure' });

    expect(retried.id).not.toBe(failed.id);
    expect(retried.generationRevision).toBe(2);
    expect(retried.replacementOfItemId).toBe(failed.id);
    expect(retried.status).toBe('READY');
    expect((await api.getDataItem(batch.id, failed.id)).auditHistory).toEqual(expect.arrayContaining([expect.objectContaining({ eventType: 'DATA_RETRY_REQUESTED' })]));
  });

  it('completes a replacement revision immediately while retaining the disabled original', async () => {
    const { api, batch, item } = await prepareReadyItem();
    const replacement = await api.replaceData(batch.id, item.id, { clientRequestId: 'replace-ready', reason: 'invalid source', generationParameters: { source: 'replacement' } });

    expect(replacement).toMatchObject({ status: 'READY', generationRevision: 2, replacementOfItemId: item.id });
    await expect(api.getDataItem(batch.id, item.id)).resolves.toMatchObject({ status: 'DISABLED' });
  });

  it('rejects blank reasons for data and batch commands', async () => {
    const { api, batch, item } = await prepareReadyItem();

    await expect(api.disableData(batch.id, item.id, { clientRequestId: 'disable-blank', reason: '   ' })).rejects.toThrow('reason');
    await expect(api.cancelBatch(batch.id, { clientRequestId: 'cancel-blank', reason: '' })).rejects.toThrow('reason');
  });

  it('uses mock mode by default without calling HTTP', async () => {
    const authenticated: AuthenticatedApiClient = { get: vi.fn(), post: vi.fn(), put: vi.fn(), createClientRequestId: vi.fn(() => 'http-id') };
    const api = createRunBatchApiFromConfig({ apiMode: 'remote', runBatchApiMode: 'mock', apiBaseUrl: '/api/v1', bearerToken: '', requestTimeoutMs: 8000, tenantId: 'tenant-1', connectorSystemId: 'connector-1', teacherId: 'teacher-1', teacherName: 'teacher' }, authenticated, new MemoryStorage());

    await expect(api.listPublishedWorkflows()).resolves.toHaveLength(1);
    expect(authenticated.get).not.toHaveBeenCalled();
  });
});
