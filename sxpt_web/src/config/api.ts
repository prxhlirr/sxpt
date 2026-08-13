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
  autoDevToken: import.meta.env.VITE_API_AUTO_DEV_TOKEN === 'true',
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
  const authenticated = readAuthenticatedUser();
  return {
    ...defaults,
    ...runtime,
    ...(authenticated
      ? {
          tenantId: authenticated.tenantId,
          currentUserId: authenticated.userId,
          currentUsername: authenticated.username,
          simulatedOrgId:
            authenticated.orgIds[0] ??
            runtime?.simulatedOrgId ??
            defaults.simulatedOrgId,
          simulatedStudentId: authenticated.userId,
          simulatedStudentName:
            authenticated.displayName || authenticated.username
        }
      : {}),
    apiBaseUrl: normalizeBaseUrl(
      runtime?.apiBaseUrl ??
        import.meta.env.VITE_API_BASE_URL ??
        defaults.apiBaseUrl
    )
  };
}

function readAuthenticatedUser(): {
  userId: string;
  tenantId: string;
  username: string;
  displayName?: string;
  orgIds: string[];
} | null {
  try {
    const raw =
      typeof window === 'undefined'
        ? null
        : window.localStorage?.getItem('sxpt_web.auth.session.v1');
    if (!raw) return null;
    const session = JSON.parse(raw) as {
      user?: {
        userId?: string;
        tenantId?: string;
        username?: string;
        displayName?: string;
        orgIds?: string[];
      };
    };
    if (
      !session.user?.userId ||
      !session.user.tenantId ||
      !session.user.username
    ) {
      return null;
    }
    return {
      userId: session.user.userId,
      tenantId: session.user.tenantId,
      username: session.user.username,
      displayName: session.user.displayName,
      orgIds: Array.isArray(session.user.orgIds) ? session.user.orgIds : []
    };
  } catch {
    return null;
  }
}

export function normalizeBaseUrl(value: string): string {
  return value.trim().replace(/\/+$/, '');
}
