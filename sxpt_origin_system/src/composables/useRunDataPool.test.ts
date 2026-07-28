import { describe, expect, it, vi } from 'vitest';
import type { RunBatchApi } from '../api/v1/runBatchApi';
import type { RunDataItem } from '../types/runBatch';
import { createRunDataPool } from './useRunDataPool';

const itemFixture = (overrides: Partial<RunDataItem> = {}): RunDataItem => ({
  id: 'item-1', batchId: 'batch-1', unitId: 'unit-1', kind: 'FORMAL', status: 'READY', generationRevision: 1,
  businessReferenceMasked: 'MASKED-1', generationParameters: {}, generationJobId: 'job-1', auditHistory: [],
  createdAt: '2026-07-24T00:00:00Z', updatedAt: '2026-07-24T00:00:00Z', ...overrides
});

function apiWith(overrides: Partial<RunBatchApi> = {}): RunBatchApi {
  return {
    listPublishedWorkflows: vi.fn(), createBatch: vi.fn(), getBatch: vi.fn(), saveMembers: vi.fn(), saveUnitPlans: vi.fn(), preflight: vi.fn(), startDataGeneration: vi.fn(), getGenerationJob: vi.fn(), advanceGenerationJob: vi.fn(),
    listDataItems: vi.fn().mockResolvedValue({ items: [itemFixture()], page: 1, pageSize: 20, total: 1 }),
    getDataItem: vi.fn().mockImplementation(async (_batchId, itemId) =>
      itemFixture({
        id: itemId,
        generationRevision: itemId === 'item-2' ? 2 : 1,
        replacementOfItemId: itemId === 'item-2' ? 'item-1' : undefined
      })
    ),
    disableData: vi.fn().mockResolvedValue(itemFixture({ status: 'DISABLED' })),
    promoteData: vi.fn().mockResolvedValue(itemFixture({ kind: 'FORMAL' })),
    replaceData: vi.fn().mockResolvedValue(itemFixture({ id: 'item-2', generationRevision: 2, replacementOfItemId: 'item-1' })),
    retryData: vi.fn().mockResolvedValue(itemFixture({ id: 'item-2', status: 'READY', generationRevision: 2, replacementOfItemId: 'item-1' })),
    publishBatch: vi.fn(), cancelBatch: vi.fn(), createClientRequestId: vi.fn().mockReturnValue('data-intent-1'), ...overrides
  };
}

describe('createRunDataPool', () => {
  it('refreshes a data row after disable promote replace or retry', async () => {
    const api = apiWith();
    const pool = createRunDataPool(api);
    await pool.load('batch-1');

    await pool.disable('item-1', 'bad source');
    await pool.promote('item-1', 'quota needed');
    await pool.replace('item-1', 'bad source', { source: 'new' });
    await pool.retry('item-1', 'retry transient failure');

    expect(api.getDataItem).toHaveBeenCalledTimes(4);
    expect(api.listDataItems).toHaveBeenCalledTimes(5);
    expect(pool.selectedItem.value).toEqual(expect.objectContaining({ id: 'item-2' }));
  });

  it.each([
    ['disable', 'disableData', (pool: ReturnType<typeof createRunDataPool>) => pool.disable('item-1', '   ')],
    ['promote', 'promoteData', (pool: ReturnType<typeof createRunDataPool>) => pool.promote('item-1', '   ')],
    ['replace', 'replaceData', (pool: ReturnType<typeof createRunDataPool>) => pool.replace('item-1', '   ', {})],
    ['retry', 'retryData', (pool: ReturnType<typeof createRunDataPool>) => pool.retry('item-1', '   ')]
  ])('rejects a blank reason before sending %s', async (name, _apiMethod, operation) => {
    const api = apiWith();
    const pool = createRunDataPool(api);
    await pool.load('batch-1');

    await expect(operation(pool)).rejects.toThrow('reason');
    expect(({ disable: api.disableData, promote: api.promoteData, replace: api.replaceData, retry: api.retryData }[name as 'disable' | 'promote' | 'replace' | 'retry'] as ReturnType<typeof vi.fn>)).not.toHaveBeenCalled();
  });

  it('keeps only the newest filter page and selection responses', async () => {
    const firstPage = deferredPromise<{ items: RunDataItem[]; page: number; pageSize: number; total: number }>();
    const secondPage = deferredPromise<{ items: RunDataItem[]; page: number; pageSize: number; total: number }>();
    const firstItem = deferredPromise<RunDataItem>();
    const secondItem = deferredPromise<RunDataItem>();
    const api = apiWith({
      listDataItems: vi.fn().mockReturnValueOnce(firstPage.promise).mockReturnValueOnce(secondPage.promise),
      getDataItem: vi.fn().mockReturnValueOnce(firstItem.promise).mockReturnValueOnce(secondItem.promise)
    });
    const pool = createRunDataPool(api);
    const initialLoad = pool.load('batch-1', { page: 1, pageSize: 20 });
    const newestFilter = pool.setFilter({ page: 2, pageSize: 20 });

    secondPage.resolve({ items: [itemFixture({ id: 'new-page' })], page: 2, pageSize: 20, total: 1 });
    await newestFilter;
    firstPage.resolve({ items: [itemFixture({ id: 'old-page' })], page: 1, pageSize: 20, total: 1 });
    await initialLoad;
    expect(pool.pageResult.value?.items[0].id).toBe('new-page');

    const oldSelection = pool.select('old-item');
    const newSelection = pool.select('new-item');
    secondItem.resolve(itemFixture({ id: 'new-item' }));
    await newSelection;
    firstItem.resolve(itemFixture({ id: 'old-item' }));
    await oldSelection;
    expect(pool.selectedItem.value?.id).toBe('new-item');
  });

  it('retains a mutation clientRequestId if post-command refresh fails', async () => {
    const refreshFailure = new Error('refresh failed');
    const getDataItem = vi.fn().mockRejectedValueOnce(refreshFailure).mockResolvedValue(itemFixture({ status: 'DISABLED' }));
    const disableData = vi.fn().mockResolvedValue(itemFixture({ status: 'DISABLED' }));
    const api = apiWith({ getDataItem, disableData, createClientRequestId: vi.fn().mockReturnValue('disable-intent-1') });
    const pool = createRunDataPool(api);
    await pool.load('batch-1');

    await expect(pool.disable('item-1', 'bad source')).rejects.toBe(refreshFailure);
    await pool.disable('item-1', 'bad source');

    expect(disableData).toHaveBeenNthCalledWith(1, 'batch-1', 'item-1', { clientRequestId: 'disable-intent-1', reason: 'bad source' });
    expect(disableData).toHaveBeenNthCalledWith(2, 'batch-1', 'item-1', { clientRequestId: 'disable-intent-1', reason: 'bad source' });
    expect(pool.mutating.value).toBe(false);
  });

  it('does not refresh an old batch after a mutation completes following a batch switch', async () => {
    const pendingDisable = deferredPromise<RunDataItem>();
    const listDataItems = vi.fn().mockImplementation(async (batchId) => ({ items: [itemFixture({ id: `${batchId}-page`, batchId })], page: 1, pageSize: 20, total: 1 }));
    const api = apiWith({ listDataItems, disableData: vi.fn().mockReturnValue(pendingDisable.promise) });
    const pool = createRunDataPool(api);
    await pool.load('batch-1');
    const oldMutation = pool.disable('item-1', 'bad source');
    await pool.load('batch-2');
    pendingDisable.resolve(itemFixture({ status: 'DISABLED' }));
    await oldMutation;

    expect(pool.pageResult.value?.items[0].id).toBe('batch-2-page');
    expect(api.getDataItem).not.toHaveBeenCalled();
    expect(listDataItems).toHaveBeenCalledTimes(2);
  });

  it('keeps a newer selection and returns the operated item when selection changes during mutation refresh', async () => {
    const refreshedMutationItem = deferredPromise<RunDataItem>();
    const selectedOther = deferredPromise<RunDataItem>();
    const getDataItem = vi.fn().mockReturnValueOnce(refreshedMutationItem.promise).mockReturnValueOnce(selectedOther.promise);
    const api = apiWith({ getDataItem, disableData: vi.fn().mockResolvedValue(itemFixture({ status: 'DISABLED' })) });
    const pool = createRunDataPool(api);
    await pool.load('batch-1');
    const mutation = pool.disable('item-1', 'bad source');
    await Promise.resolve();
    const newerSelection = pool.select('item-2');
    selectedOther.resolve(itemFixture({ id: 'item-2' }));
    await newerSelection;
    refreshedMutationItem.resolve(itemFixture({ id: 'item-1', status: 'DISABLED' }));

    await expect(mutation).resolves.toEqual(expect.objectContaining({ id: 'item-1', status: 'DISABLED' }));
    expect(pool.selectedItem.value?.id).toBe('item-2');
  });

  it('isolates same data-command request IDs across batches', async () => {
    const firstDisable = deferredPromise<RunDataItem>();
    const api = apiWith({ disableData: vi.fn().mockReturnValueOnce(firstDisable.promise).mockResolvedValueOnce(itemFixture({ status: 'DISABLED' })), createClientRequestId: vi.fn().mockReturnValueOnce('batch-1-disable').mockReturnValueOnce('batch-2-disable') });
    const pool = createRunDataPool(api);
    await pool.load('batch-1');
    const oldDisable = pool.disable('item-1', 'bad source');
    await pool.load('batch-2');
    await pool.disable('item-1', 'bad source');
    firstDisable.resolve(itemFixture({ status: 'DISABLED' }));
    await oldDisable;

    expect(api.disableData).toHaveBeenNthCalledWith(1, 'batch-1', 'item-1', expect.objectContaining({ clientRequestId: 'batch-1-disable' }));
    expect(api.disableData).toHaveBeenNthCalledWith(2, 'batch-2', 'item-1', expect.objectContaining({ clientRequestId: 'batch-2-disable' }));
  });

  it('refreshes the current filter after a mutation when the filter changes while it is running', async () => {
    const pendingDisable = deferredPromise<RunDataItem>();
    const api = apiWith({ disableData: vi.fn().mockReturnValue(pendingDisable.promise) });
    const pool = createRunDataPool(api);
    await pool.load('batch-1', { page: 1, pageSize: 20 });
    const mutation = pool.disable('item-1', 'bad source');
    await pool.setFilter({ page: 2, pageSize: 10 });
    pendingDisable.resolve(itemFixture({ status: 'DISABLED' }));
    await mutation;

    expect(api.listDataItems).toHaveBeenLastCalledWith('batch-1', { page: 2, pageSize: 10 });
  });

  it('clears old rows and selection immediately when loading another batch even if the new request fails', async () => {
    const secondLoad = deferredPromise<{ items: RunDataItem[]; page: number; pageSize: number; total: number }>();
    const api = apiWith({ listDataItems: vi.fn().mockResolvedValueOnce({ items: [itemFixture()], page: 1, pageSize: 20, total: 1 }).mockReturnValueOnce(secondLoad.promise) });
    const pool = createRunDataPool(api);
    await pool.load('batch-1');
    await pool.select('item-1');
    const loading = pool.load('batch-2');
    expect(pool.pageResult.value).toBeNull();
    expect(pool.selectedItem.value).toBeNull();
    secondLoad.reject(new Error('new batch unavailable'));
    await expect(loading).rejects.toThrow('new batch unavailable');
    expect(pool.pageResult.value).toBeNull();
  });

  it('reuses only semantically identical data command intents', async () => {
    const lost = new Error('lost');
    const disableData = vi.fn().mockRejectedValueOnce(lost).mockResolvedValueOnce(itemFixture({ status: 'DISABLED' })).mockResolvedValueOnce(itemFixture({ status: 'DISABLED' }));
    const api = apiWith({ disableData, createClientRequestId: vi.fn().mockReturnValueOnce('disable-1').mockReturnValueOnce('disable-2') });
    const pool = createRunDataPool(api);
    await pool.load('batch-1');
    await expect(pool.disable('item-1', ' bad source ')).rejects.toBe(lost);
    await pool.disable('item-1', 'bad source');
    await pool.disable('item-1', 'different source');

    expect(disableData).toHaveBeenNthCalledWith(1, 'batch-1', 'item-1', expect.objectContaining({ clientRequestId: 'disable-1', reason: 'bad source' }));
    expect(disableData).toHaveBeenNthCalledWith(2, 'batch-1', 'item-1', expect.objectContaining({ clientRequestId: 'disable-1', reason: 'bad source' }));
    expect(disableData).toHaveBeenNthCalledWith(3, 'batch-1', 'item-1', expect.objectContaining({ clientRequestId: 'disable-2', reason: 'different source' }));
  });
});

function deferredPromise<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((onResolve, onReject) => { resolve = onResolve; reject = onReject; });
  return { promise, resolve, reject };
}
