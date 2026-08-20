import type { RecordedStep } from '../domain/models';

export interface PracticeBusinessAction {
  actionType: 'click' | 'input' | 'select' | 'submit';
  selector: string;
  selectorCandidates?: string[];
  text?: string;
  url?: string;
}

function selectorToken(selector: string) {
  const normalized = selector.replace(/\s+/g, '');
  const action = normalized.match(/data-action=["']([^"']+)["']/)?.[1];
  if (action) return `action:${action}`;
  const businessField = normalized.match(
    /data-business-field=["']([^"']+)["']/
  )?.[1];
  if (businessField) return `field:${businessField}`;
  const trainingId = normalized.match(
    /data-training-id=["']([^"']+)["']/
  )?.[1];
  const trainingAliases: Record<string, string> = {
    supplier: 'counterparty',
    category: 'category',
    amount: 'amount',
    'arrival-date': 'date',
    reason: 'reason',
    subject: 'subject'
  };
  if (trainingId && trainingAliases[trainingId]) {
    return `field:${trainingAliases[trainingId]}`;
  }
  return normalized;
}

function selectorMatchesRecordedTarget(expected: string, actual: string) {
  const expectedToken = selectorToken(expected);
  const actualToken = selectorToken(actual);
  if (expectedToken === actualToken) return true;

  // Element picking may intentionally bind a business card/container while
  // the runtime recorder reports the button clicked inside that container.
  // A unique recorded CSS path is therefore also a valid ancestor of the
  // observed actionable element path.
  return (
    actualToken.startsWith(`${expectedToken}>`) ||
    actualToken.includes(`>${expectedToken}>`)
  );
}

function normalizedActionText(value: string | undefined) {
  return (value ?? '').replace(/\s+/g, '').trim().toLocaleLowerCase();
}

function actionTextMatchesRecordedStep(
  payload: PracticeBusinessAction,
  step: RecordedStep
) {
  if (payload.actionType !== 'click' && payload.actionType !== 'submit') {
    return false;
  }
  const observed = normalizedActionText(payload.text);
  if (observed.length < 2) return false;
  return [step.actionLabel, step.title].some((candidate) => {
    const expected = normalizedActionText(candidate);
    return expected.includes(observed) || observed.includes(expected);
  });
}

function isDynamicBusinessId(segment: string) {
  let decoded = segment;
  try {
    decoded = decodeURIComponent(segment);
  } catch {
    // Keep the original segment when it is not valid URI text.
  }
  return (
    /^\d{6,}$/.test(decoded) ||
    /^[0-9a-f]{16,}$/i.test(decoded) ||
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
      decoded
    )
  );
}

/**
 * 教师录制和学生练习会分别创建原平台业务数据，因此详情页末尾的数据 ID
 * 通常不同。比较业务路由时保留模块/页面结构，只把纯数字、UUID、长十六进制
 * 数据 ID 视为同一动态占位符。
 */
export function businessPathsMatch(
  expectedUrl: string,
  observedUrl: string,
  baseUrl: string
) {
  const expectedPath = new URL(expectedUrl, baseUrl).pathname.replace(/\/$/, '');
  const observedPath = new URL(observedUrl, baseUrl).pathname.replace(/\/$/, '');
  if (expectedPath === observedPath) return true;

  const expectedSegments = expectedPath.split('/').filter(Boolean);
  const observedSegments = observedPath.split('/').filter(Boolean);
  if (expectedSegments.length !== observedSegments.length) return false;
  return expectedSegments.every(
    (segment, index) =>
      segment === observedSegments[index] ||
      (isDynamicBusinessId(segment) &&
        isDynamicBusinessId(observedSegments[index] ?? ''))
  );
}

export function actionMatchesRecordedStep(
  payload: PracticeBusinessAction,
  step: RecordedStep,
  baseUrl: string
) {
  const expectedActionType =
    step.actionType && step.actionType !== 'guide' ? step.actionType : 'click';
  const compatibleAction =
    payload.actionType === expectedActionType ||
    ((expectedActionType === 'click' || expectedActionType === 'submit') &&
      (payload.actionType === 'click' || payload.actionType === 'submit'));
  if (!compatibleAction) return false;

  const expected = [step.selector, ...(step.selectorCandidates ?? [])]
    .filter(Boolean);
  const actual = [payload.selector, ...(payload.selectorCandidates ?? [])]
    .filter(Boolean);
  const selectorMatched = actual.some((actualSelector) =>
      expected.some((expectedSelector) =>
        selectorMatchesRecordedTarget(expectedSelector, actualSelector)
      )
    );
  if (!selectorMatched && !actionTextMatchesRecordedStep(payload, step)) {
    return false;
  }

  const expectedUrl = step.url || step.pageSnapshot?.pageUrl;
  if (!expectedUrl || !payload.url) return true;
  try {
    return businessPathsMatch(expectedUrl, payload.url, baseUrl);
  } catch {
    // URL is only an auxiliary condition. Stable element selectors remain the
    // source of truth when a legacy URL cannot be normalized.
    return true;
  }
}

/**
 * 同一次点击可能同时命中按钮、卡片和 main 等多个录制节点。优先选择学生
 * 实际操作的精确/较具体目标，避免宽泛的说明节点先消费事件，导致必做按钮
 * 看起来一直未完成。
 */
export function practiceActionMatchPriority(
  payload: PracticeBusinessAction,
  step: RecordedStep,
  baseUrl: string
) {
  if (!actionMatchesRecordedStep(payload, step, baseUrl)) return -1;

  const expectedSelectors = [
    step.selector,
    ...(step.selectorCandidates ?? [])
  ].filter(Boolean);
  const primarySelector = payload.selector;
  const observedCandidates = (payload.selectorCandidates ?? []).filter(Boolean);
  let selectorPriority = 0;

  for (const expectedSelector of expectedSelectors) {
    const specificity = Math.min(
      expectedSelector.replace(/\s+/g, '').length,
      9_999
    );
    if (selectorMatchesRecordedTarget(expectedSelector, primarySelector)) {
      const exact =
        selectorToken(expectedSelector) === selectorToken(primarySelector);
      selectorPriority = Math.max(
        selectorPriority,
        (exact ? 300_000 : 200_000) + specificity
      );
    }
    if (
      observedCandidates.some((candidate) =>
        selectorMatchesRecordedTarget(expectedSelector, candidate)
      )
    ) {
      selectorPriority = Math.max(selectorPriority, 100_000 + specificity);
    }
  }

  // 相同精度的重复录制节点中，必做节点应先消费学生操作。
  return selectorPriority + (step.required === false ? 0 : 10_000);
}
