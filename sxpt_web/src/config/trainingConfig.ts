export type ApiMode = 'mock' | 'remote';

export interface TrainingRuntimeConfig {
  apiMode: ApiMode;
  runBatchApiMode: ApiMode;
  apiBaseUrl: string;
  bearerToken: string;
  requestTimeoutMs: number;
  tenantId: string;
  connectorSystemId: string;
  teacherId: string;
  teacherName: string;
}

declare global {
  interface Window {
    __TRAINING_CONFIG__?: Partial<TrainingRuntimeConfig>;
  }
}

const defaults: TrainingRuntimeConfig = {
  apiMode: 'remote',
  runBatchApiMode: 'mock',
  apiBaseUrl: '/api/v1',
  bearerToken: '',
  requestTimeoutMs: 8000,
  tenantId: 'demo-tenant',
  connectorSystemId: 'demo-connector',
  teacherId: 'demo-teacher',
  teacherName: '演示教师'
};

export function loadTrainingConfig(
  source: Partial<TrainingRuntimeConfig> =
    typeof window === 'undefined' ? {} : (window.__TRAINING_CONFIG__ ?? {})
): Readonly<TrainingRuntimeConfig> {
  const config: TrainingRuntimeConfig = {
    ...defaults,
    ...source,
    apiBaseUrl: (source.apiBaseUrl ?? defaults.apiBaseUrl).replace(/\/+$/, '')
  };

  if (config.apiMode === 'remote') {
    for (const field of [
      'apiBaseUrl',
      'tenantId',
      'connectorSystemId',
      'teacherId'
    ] as const) {
      if (!config[field]) {
        throw new Error(`远程接口配置缺少 ${field}`);
      }
    }
  }

  return Object.freeze(config);
}

export const trainingConfig = loadTrainingConfig();
