import type { StudentDataLaunchResult } from '../services/trainingApi';

export function sanitizeRecordedBusinessUrl(
  recordedUrl: string,
  launchUrl: string,
  redirectUrl: string
): string {
  const recorded = recordedUrl.trim();
  const redirect = redirectUrl.trim();
  if (!recorded) {
    return redirect;
  }
  if (recorded === launchUrl.trim()) {
    return redirect;
  }

  try {
    const recordedLocation = new URL(recorded, redirect);
    const launchLocation = new URL(launchUrl);
    if (
      recordedLocation.origin === launchLocation.origin &&
      recordedLocation.pathname === launchLocation.pathname &&
      recordedLocation.searchParams.has('launchToken')
    ) {
      return redirect;
    }
    return recordedLocation.toString();
  } catch {
    return recorded;
  }
}

export function resolveLaunchedBusinessFrameUrl(
  launch: StudentDataLaunchResult | null | undefined,
  platformLaunchEntryUrl?: string
): string {
  const launchUrl = launch?.launchUrl?.trim() || '';
  if (!launchUrl) return '';
  try {
    return new URL(launchUrl).toString();
  } catch {
    // 原平台 DATA_CREATE 通常只返回业务详情相对地址。此时必须先走原平台
    // teaching-launch，以一次性 launchToken 建立 OA 会话，再重定向到详情页。
  }
  const entryUrl = platformLaunchEntryUrl?.trim() || '';
  if (!entryUrl || !launch?.launchToken || !launch.tenantId) {
    return launchUrl;
  }
  try {
    const entry = new URL(entryUrl);
    // 业务平台配置保存的是平台根地址时，也必须进入一次性令牌交换页。
    // 直接把 tenantId/launchToken 挂到根路由，可能会先被原平台的首页
    // redirect 吞掉查询串，导致全新学生会话提示“启动链接缺少参数”。
    if (entry.pathname === '/' || entry.pathname === '') {
      entry.pathname = '/teaching-launch';
    }
    entry.searchParams.set('tenantId', launch.tenantId);
    entry.searchParams.set('launchToken', launch.launchToken);
    entry.searchParams.set('redirect', launch.targetUrl?.trim() || launchUrl);
    return entry.toString();
  } catch {
    return launchUrl;
  }
}
