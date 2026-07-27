import type { ModePolicy, RunnerMode } from '../types/domain';

export const modePolicies: Record<RunnerMode, ModePolicy> = {
  learning: {
    showFullFlow: true,
    showTeachingText: true,
    showOverlayHint: true,
    allowFreeBrowse: true,
    enforceOrder: false,
    showSubmission: false,
    silentRecord: false
  },
  practice: {
    showFullFlow: true,
    showTeachingText: false,
    showOverlayHint: true,
    allowFreeBrowse: false,
    enforceOrder: true,
    showSubmission: false,
    silentRecord: false
  },
  exam: {
    showFullFlow: false,
    showTeachingText: false,
    showOverlayHint: false,
    allowFreeBrowse: false,
    enforceOrder: false,
    showSubmission: true,
    silentRecord: true
  }
};

export function getModePolicy(mode: RunnerMode): ModePolicy {
  return modePolicies[mode];
}
