import { ref, type Ref } from 'vue';

export interface ApiLogEntry {
  id: string;
  method: 'GET' | 'POST' | 'PUT';
  path: string;
  captureSessionId?: string;
  startedAt: string;
  durationMs?: number;
  status: 'pending' | 'success' | 'error';
  code?: number | string;
  message?: string;
}

export interface ApiLogStart {
  id: string;
  method: 'GET' | 'POST' | 'PUT';
  path: string;
  captureSessionId?: string;
}

export interface ApiLogStore {
  entries: Ref<ApiLogEntry[]>;
  begin(entry: ApiLogStart): void;
  succeed(id: string, code?: number | string, message?: string): void;
  fail(id: string, code?: number | string, message?: string): void;
  clear(): void;
}

export function createApiLogStore(limit = 100): ApiLogStore {
  const entries = ref<ApiLogEntry[]>([]);

  function begin(entry: ApiLogStart) {
    const nextEntry: ApiLogEntry = {
      ...entry,
      startedAt: new Date().toISOString(),
      status: 'pending'
    };
    entries.value = [
      nextEntry,
      ...entries.value
    ].slice(0, limit);
  }

  function complete(
    id: string,
    status: ApiLogEntry['status'],
    code?: number | string,
    message?: string
  ) {
    entries.value = entries.value.map((entry) => {
      if (entry.id !== id) return entry;
      return {
        ...entry,
        status,
        code,
        message,
        durationMs: Math.max(
          0,
          Date.now() - new Date(entry.startedAt).getTime()
        )
      };
    });
  }

  return {
    entries,
    begin,
    succeed: (id, code, message) => complete(id, 'success', code, message),
    fail: (id, code, message) => complete(id, 'error', code, message),
    clear: () => {
      entries.value = [];
    }
  };
}
