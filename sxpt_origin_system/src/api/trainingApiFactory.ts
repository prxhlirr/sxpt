import type { TrainingRuntimeConfig } from '../config/trainingConfig';
import type { TrainingApi } from '../types/trainingApi';
import type { ApiLogStore } from './apiLogStore';
import type { ApiHttpClient } from './httpClient';
import { MockTrainingApi } from './mockTrainingApi';
import { RemoteTrainingApi } from './remoteTrainingApi';

export function createTrainingApi(
  config: TrainingRuntimeConfig,
  logStore: ApiLogStore,
  storage: Storage = window.localStorage,
  httpClient?: ApiHttpClient
): TrainingApi {
  return config.apiMode === 'remote'
    ? new RemoteTrainingApi(config, logStore, httpClient)
    : new MockTrainingApi(logStore, storage);
}
