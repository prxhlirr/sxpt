import type { TrainingRuntimeConfig } from '../../config/trainingConfig';
import type { AuthenticatedApiClient } from './authenticatedApiClient';
import { createMockRunBatchApi } from './mockRunBatchApi';
import { createRunBatchApi, type RunBatchApi } from './runBatchApi';

export function createRunBatchApiFromConfig(config: TrainingRuntimeConfig, client: AuthenticatedApiClient, storage: Storage = browserStorage()): RunBatchApi {
  return config.runBatchApiMode === 'mock' ? createMockRunBatchApi(storage, () => client.createClientRequestId()) : createRunBatchApi(client);
}

function browserStorage(): Storage {
  if (typeof localStorage !== 'undefined') return localStorage;
  const values = new Map<string, string>();
  return { get length() { return values.size; }, clear: () => values.clear(), getItem: (key) => values.get(key) ?? null, key: (index) => [...values.keys()][index] ?? null, removeItem: (key) => values.delete(key), setItem: (key, value) => values.set(key, value) };
}
