import { describe, expect, it, vi } from 'vitest';
import type { AuthenticatedApiClient } from './authenticatedApiClient';
import { createRunBatchApi } from './runBatchApi';
import type { CreateRunBatchRequest } from '../../types/runBatch';

function client(): AuthenticatedApiClient {
  return {
    get: vi.fn().mockResolvedValue({}),
    post: vi.fn().mockResolvedValue({}),
    put: vi.fn().mockResolvedValue({}),
    createClientRequestId: vi.fn(() => 'request-1')
  };
}

const createRequest: CreateRunBatchRequest = {
  clientRequestId: 'create-1',
  workflowVersionId: 'workflow-1',
  runType: 'EXAM',
  batchName: 'July exam',
  plannedStartTime: '2026-07-30T09:00:00Z',
  plannedEndTime: '2026-07-30T11:00:00Z'
};

describe('createRunBatchApi', () => {
  it('maps the HTTP adapter to workflow-runs resources', async () => {
    const authenticated = client();
    const api = createRunBatchApi(authenticated);

    await api.listPublishedWorkflows();
    await api.createBatch(createRequest);
    await api.saveMembers('batch / 1', { clientRequestId: 'members-1', members: [] });
    await api.saveUnitPlans('batch / 1', { clientRequestId: 'plans-1', unitPlans: [] });
    await api.preflight('batch / 1');
    await api.startDataGeneration('batch / 1', { clientRequestId: 'prepare-1', unitPlans: [] });
    await api.getGenerationJob('batch / 1', 'job / 1');
    await api.advanceGenerationJob('batch / 1', 'job / 1', 'advance-1');
    await api.listDataItems('batch / 1', { page: 2, pageSize: 10, unitId: 'unit / 1' });
    await api.getDataItem('batch / 1', 'item / 1');
    await api.disableData('batch / 1', 'item / 1', { clientRequestId: 'disable-1', reason: 'duplicate' });
    await api.promoteData('batch / 1', 'item / 1', { clientRequestId: 'promote-1', reason: 'needed' });
    await api.replaceData('batch / 1', 'item / 1', { clientRequestId: 'replace-1', reason: 'bad seed', generationParameters: {} });
    await api.retryData('batch / 1', 'item / 1', { clientRequestId: 'retry-1', reason: 'transient failure' });
    await api.publishBatch('batch / 1', 'publish-1');
    await api.cancelBatch('batch / 1', { clientRequestId: 'cancel-1', reason: 'cancelled' });

    expect(authenticated.get).toHaveBeenCalledWith('/workflow-versions/published');
    expect(authenticated.post).toHaveBeenCalledWith('/workflow-runs', createRequest);
    expect(authenticated.put).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/members', { clientRequestId: 'members-1', members: [] });
    expect(authenticated.put).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/unit-plans', { clientRequestId: 'plans-1', unitPlans: [] });
    expect(authenticated.get).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/preflight');
    expect(authenticated.post).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/data-jobs', { clientRequestId: 'prepare-1', unitPlans: [] });
    expect(authenticated.get).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/data-jobs/job%20%2F%201');
    expect(authenticated.post).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/data-jobs/job%20%2F%201/advance', { clientRequestId: 'advance-1' });
    expect(authenticated.get).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/data-items', { page: '2', pageSize: '10', unitId: 'unit / 1' });
    expect(authenticated.get).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/data-items/item%20%2F%201');
    expect(authenticated.post).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/data-items/item%20%2F%201/disable', { clientRequestId: 'disable-1', reason: 'duplicate' });
    expect(authenticated.post).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/data-items/item%20%2F%201/promote', { clientRequestId: 'promote-1', reason: 'needed' });
    expect(authenticated.post).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/data-items/item%20%2F%201/replace', { clientRequestId: 'replace-1', reason: 'bad seed', generationParameters: {} });
    expect(authenticated.post).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/data-items/item%20%2F%201/retry', { clientRequestId: 'retry-1', reason: 'transient failure' });
    expect(authenticated.post).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/publish', { clientRequestId: 'publish-1' });
    expect(authenticated.post).toHaveBeenCalledWith('/workflow-runs/batch%20%2F%201/cancel', { clientRequestId: 'cancel-1', reason: 'cancelled' });
  });
});
