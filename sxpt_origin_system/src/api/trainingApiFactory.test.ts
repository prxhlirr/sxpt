import { describe, expect, it } from 'vitest';
import { loadTrainingConfig } from '../config/trainingConfig';
import { createApiLogStore } from './apiLogStore';
import { MockTrainingApi } from './mockTrainingApi';
import { RemoteTrainingApi } from './remoteTrainingApi';
import { createTrainingApi } from './trainingApiFactory';

class MemoryStorage implements Storage {
  private values = new Map<string, string>();
  get length() { return this.values.size; }
  clear() { this.values.clear(); }
  getItem(key: string) { return this.values.get(key) ?? null; }
  key(index: number) { return Array.from(this.values.keys())[index] ?? null; }
  removeItem(key: string) { this.values.delete(key); }
  setItem(key: string, value: string) { this.values.set(key, value); }
}

describe('createTrainingApi', () => {
  it('creates the explicitly configured adapter', () => {
    expect(
      createTrainingApi(
        loadTrainingConfig({ apiMode: 'mock' }),
        createApiLogStore(),
        new MemoryStorage()
      )
    ).toBeInstanceOf(MockTrainingApi);

    expect(
      createTrainingApi(
        loadTrainingConfig({ apiMode: 'remote' }),
        createApiLogStore(),
        new MemoryStorage()
      )
    ).toBeInstanceOf(RemoteTrainingApi);
  });
});
