import { describe, expect, it } from 'vitest';
import { createApiLogStore } from './apiLogStore';

describe('createApiLogStore', () => {
  it('keeps only the configured number of recent entries', () => {
    const store = createApiLogStore(2);

    for (const path of ['/one', '/two', '/three']) {
      store.begin({ id: path, method: 'GET', path });
    }

    expect(store.entries.value.map((entry) => entry.path)).toEqual([
      '/three',
      '/two'
    ]);
  });

  it('records request metadata without accepting request bodies', () => {
    const store = createApiLogStore();
    store.begin({
      id: 'request-1',
      method: 'POST',
      path: '/capture/events/report',
      captureSessionId: 'capture-1'
    });

    expect(JSON.stringify(store.entries.value)).not.toContain('Authorization');
    expect(JSON.stringify(store.entries.value)).not.toContain('eventPayloadJson');
  });
});
