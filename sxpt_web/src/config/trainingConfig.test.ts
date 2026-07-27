import { describe, expect, it } from 'vitest';
import { loadTrainingConfig } from './trainingConfig';

describe('loadTrainingConfig', () => {
  it('loads the documented remote defaults', () => {
    expect(loadTrainingConfig({})).toMatchObject({
      apiMode: 'remote',
      runBatchApiMode: 'mock',
      apiBaseUrl: '/api/v1',
      requestTimeoutMs: 8000,
      tenantId: 'demo-tenant',
      connectorSystemId: 'demo-connector',
      teacherId: 'demo-teacher',
      teacherName: '演示教师'
    });
  });

  it('normalizes a trailing slash in the API base URL', () => {
    expect(
      loadTrainingConfig({ apiBaseUrl: 'http://localhost:8080/api/v1/' })
        .apiBaseUrl
    ).toBe('http://localhost:8080/api/v1');
  });

  it('rejects a remote configuration without an API URL', () => {
    expect(() =>
      loadTrainingConfig({ apiMode: 'remote', apiBaseUrl: '' })
    ).toThrow('apiBaseUrl');
  });
});
