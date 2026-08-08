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
  launch: StudentDataLaunchResult | null | undefined
): string {
  return launch?.launchUrl?.trim() || '';
}
