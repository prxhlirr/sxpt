import { apiRequest } from './http';

export const systemApi = {
  health() {
    return apiRequest<Record<string, unknown>>({
      method: 'GET',
      url: '/system/health'
    });
  },

  databaseHealth() {
    return apiRequest<Record<string, unknown>>({
      method: 'GET',
      url: '/system/db-health'
    });
  },

  redisHealth() {
    return apiRequest<Record<string, unknown>>({
      method: 'GET',
      url: '/system/redis-health'
    });
  }
};
