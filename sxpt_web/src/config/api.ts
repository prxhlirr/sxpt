export interface SxptApiRuntimeConfig {
  enabled: boolean;
  apiBaseUrl: string;
  timeoutMs: number;
  bearerToken: string;
  autoDevToken: boolean;
  tenantId: string;
  currentUserId: string;
  currentUsername: string;
  simulatedOrgId: string;
  simulatedStudentId: string;
  simulatedStudentName: string;
}

declare global {
  interface Window {
    __SXPT_API_CONFIG__?: Partial<SxptApiRuntimeConfig>;
  }
}

const defaults: SxptApiRuntimeConfig = {
  enabled: true,
  apiBaseUrl: 'http://127.0.0.1:8080/api/v1',
  timeoutMs: 15000,
  bearerToken: '',
  autoDevToken: true,
  tenantId: 'demo-tenant',
  currentUserId: 'demo-teacher',
  currentUsername: 'demo-teacher',
  simulatedOrgId: 'demo-class',
  simulatedStudentId: 'demo-student',
  simulatedStudentName: '模拟学生'
};

export function getApiConfig(): SxptApiRuntimeConfig {
  const runtime =
    typeof window === 'undefined' ? undefined : window.__SXPT_API_CONFIG__;
  return {
    ...defaults,
    ...runtime,
    apiBaseUrl: normalizeBaseUrl(
      runtime?.apiBaseUrl ??
        import.meta.env.VITE_API_BASE_URL ??
        defaults.apiBaseUrl
    )
  };
}

export function normalizeBaseUrl(value: string): string {
  return value.trim().replace(/\/+$/, '');
}
