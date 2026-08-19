import { describe, expect, it } from 'vitest';
import {
  resolveLaunchedBusinessFrameUrl,
  sanitizeRecordedBusinessUrl
} from './businessLaunch';

describe('业务平台启动地址', () => {
  it('不会把带一次性令牌的 SSO 地址保存到录制节点', () => {
    const launchUrl =
      'https://oa.example.com/sso?tenantId=tenant-1&launchToken=ctx-secret&redirect=module';

    expect(
      sanitizeRecordedBusinessUrl(
        launchUrl,
        launchUrl,
        'https://oa.example.com/purchase/apply'
      )
    ).toBe('https://oa.example.com/purchase/apply');
  });

  it('解析相对业务地址时以模块入口为基准且不复制 SSO 查询串', () => {
    expect(
      sanitizeRecordedBusinessUrl(
        '/purchase/detail/1',
        'https://oa.example.com/sso?launchToken=ctx-secret',
        'https://oa.example.com/purchase/apply'
      )
    ).toBe('https://oa.example.com/purchase/detail/1');
  });

  it('外部练习没有后端启动结果时不回退到模块直连地址', () => {
    expect(resolveLaunchedBusinessFrameUrl(null)).toBe('');
    expect(
      resolveLaunchedBusinessFrameUrl({
        tenantId: 'tenant-1',
        launchContextId: 'launch-1',
        launchToken: 'ctx-token',
        dataInstanceId: 'instance-1',
        launchUrl: 'https://oa.example.com/sso?launchToken=ctx-token',
        targetUrl: 'https://oa.example.com/purchase/apply',
        allocation: {} as never
      })
    ).toBe('https://oa.example.com/sso?launchToken=ctx-token');
  });

  it('经典案例返回相对详情地址时先进入原平台单点登录入口', () => {
    const resolved = resolveLaunchedBusinessFrameUrl(
      {
        tenantId: 'demo-tenant',
        launchContextId: 'launch-1',
        launchToken: 'ctx-token',
        dataInstanceId: 'instance-1',
        launchUrl: '/workspace/incoming/detail/biz-1',
        targetUrl: '/workspace/incoming/detail/biz-1'
      },
      'http://localhost:3000/teaching-launch'
    );

    expect(resolved).toBe(
      'http://localhost:3000/teaching-launch?tenantId=demo-tenant&launchToken=ctx-token&redirect=%2Fworkspace%2Fincoming%2Fdetail%2Fbiz-1'
    );
  });

  it('平台只配置根地址时仍进入 teaching-launch 并保留启动参数', () => {
    const resolved = resolveLaunchedBusinessFrameUrl(
      {
        tenantId: 'demo-tenant',
        launchContextId: 'launch-1',
        launchToken: 'ctx-token',
        dataInstanceId: 'instance-1',
        launchUrl: '/workspace/incoming/detail/biz-1',
        targetUrl: '/workspace/incoming/detail/biz-1'
      },
      'http://localhost:3000/'
    );

    expect(resolved).toBe(
      'http://localhost:3000/teaching-launch?tenantId=demo-tenant&launchToken=ctx-token&redirect=%2Fworkspace%2Fincoming%2Fdetail%2Fbiz-1'
    );
  });
});
