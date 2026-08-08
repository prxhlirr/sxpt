import { describe, expect, it, vi } from 'vitest';
import type {
  AuthoringLaunchResult,
  CreateAuthoringLaunchRequest
} from '../services/trainingApi';
import { useAuthoringBusinessLaunch } from './useAuthoringBusinessLaunch';

const request: CreateAuthoringLaunchRequest = {
  lessonId: 'lesson-1',
  connectorSystemId: 'system-1',
  businessModuleId: 'module-1'
};

function launch(dataInstanceId: string, launchUrl: string): AuthoringLaunchResult {
  return {
    tenantId: 'tenant-1',
    launchContextId: `launch-${dataInstanceId}`,
    launchToken: `token-${dataInstanceId}`,
    dataInstanceId,
    redirectUrl: 'https://oa.example.com/purchase/apply',
    launchUrl
  };
}

describe('教师编排业务实例启动', () => {
  it('每次显式启动都会请求一个全新的数据实例', async () => {
    const launchApi = vi
      .fn()
      .mockResolvedValueOnce(launch('instance-1', 'url-1'))
      .mockResolvedValueOnce(launch('instance-2', 'url-2'));
    const controller = useAuthoringBusinessLaunch(launchApi);

    await controller.start(request);
    await controller.start(request);

    expect(launchApi).toHaveBeenCalledTimes(2);
    expect(controller.result.value?.dataInstanceId).toBe('instance-2');
    expect(controller.frameUrl.value).toBe('url-2');
  });

  it('启动失败时清除旧地址且不回退到业务模块直连地址', async () => {
    const launchApi = vi
      .fn()
      .mockResolvedValueOnce(launch('instance-1', 'url-1'))
      .mockRejectedValueOnce(new Error('造数失败'));
    const controller = useAuthoringBusinessLaunch(launchApi);

    await controller.start(request);
    await controller.start(request);

    expect(controller.frameUrl.value).toBe('');
    expect(controller.error.value).toBe('造数失败');
    expect(controller.loading.value).toBe(false);
  });
});
