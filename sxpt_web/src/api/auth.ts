import { apiRequest, setApiToken } from './http';
import type { AuthLoginResponse } from './contracts';

export interface LoginRequest {
  loginType: string;
  tenantId: string;
  username: string;
  password: string;
}

export interface CurrentUser {
  userId: string;
  username: string;
  tenantId?: string;
  roles: string[];
  orgIds: string[];
  identityBindings: unknown[];
}

export const authApi = {
  async login(request: LoginRequest): Promise<AuthLoginResponse> {
    const result = await apiRequest<AuthLoginResponse>({
      method: 'POST',
      url: '/auth/login',
      data: request
    });
    setApiToken(result.token);
    return result;
  },

  me() {
    return apiRequest<CurrentUser>({
      method: 'GET',
      url: '/auth/me'
    });
  }
};
