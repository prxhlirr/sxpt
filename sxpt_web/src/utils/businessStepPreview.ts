export interface BusinessStepPreviewLocator {
  selector: string;
  selectorCandidates?: string[];
  url?: string;
}

export function normalizeBusinessStepSelectors(
  selector: string,
  selectorCandidates: string[] = []
) {
  return [selector, ...selectorCandidates]
    .map((candidate) => candidate.trim())
    .filter((candidate, index, candidates) =>
      Boolean(candidate) && candidates.indexOf(candidate) === index
    );
}

export function createBusinessStepPreviewMessages(
  requestId: string,
  locator: BusinessStepPreviewLocator
) {
  const selectorCandidates = normalizeBusinessStepSelectors(
    locator.selector,
    locator.selectorCandidates
  );
  const normalizedLocator = {
    selector: selectorCandidates[0] ?? locator.selector.trim(),
    selectorCandidates,
    url: locator.url
  };

  return [
    {
      type: 'SXPT_PREVIEW_STEP' as const,
      ...normalizedLocator
    },
    {
      type: 'RESOLVE_RECORDED_TARGET' as const,
      requestId,
      locator: normalizedLocator
    }
  ];
}

export function createBusinessStepPreviewClearMessages() {
  return [
    { type: 'SXPT_CLEAR_PREVIEW_STEP' as const },
    { type: 'CLEAR_RECORDED_TARGET_PREVIEW' as const }
  ];
}
