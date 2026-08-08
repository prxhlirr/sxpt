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
});
