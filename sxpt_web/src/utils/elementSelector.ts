const PREFERRED_ATTRIBUTES = [
  'data-training-id',
  'data-business-field',
  'data-action',
  'data-testid',
  'data-test',
  'aria-label',
  'name'
] as const;

function cssEscape(value: string) {
  if (typeof CSS !== 'undefined' && typeof CSS.escape === 'function') {
    return CSS.escape(value);
  }
  return value.replace(/(^-?\d)|[^a-zA-Z0-9_-]/g, (character, digit) =>
    digit ? `\\3${digit} ` : `\\${character}`
  );
}

function attributeSelector(name: string, value: string) {
  return `[${name}=${JSON.stringify(value)}]`;
}

function queryCount(root: ParentNode, selector: string) {
  try {
    return root.querySelectorAll(selector).length;
  } catch {
    return 0;
  }
}

function uniqueSelector(root: ParentNode, selector: string) {
  return queryCount(root, selector) === 1;
}

function selectorSegment(element: Element) {
  const tagName = element.tagName.toLowerCase();
  const stableClasses = Array.from(element.classList)
    .filter(
      (className) =>
        /^[a-zA-Z_][\w-]*$/.test(className) &&
        !/^(active|current|open|selected|hover|focus|runtime-target|sxpt-)/i.test(
          className
        )
    )
    .slice(0, 2);
  let segment = `${tagName}${stableClasses.map((name) => `.${cssEscape(name)}`).join('')}`;
  const parent = element.parentElement;
  if (parent) {
    const sameTagSiblings = Array.from(parent.children).filter(
      (sibling) => sibling.tagName === element.tagName
    );
    if (sameTagSiblings.length > 1) {
      segment += `:nth-of-type(${sameTagSiblings.indexOf(element) + 1})`;
    }
  }
  return segment;
}

export interface StableElementSelector {
  selector: string;
  selectorCandidates: string[];
}

export function buildStableElementSelector(
  element: Element,
  root: ParentNode = document
): StableElementSelector {
  const candidates: string[] = [];
  const append = (selector: string) => {
    if (selector && !candidates.includes(selector)) candidates.push(selector);
  };

  for (const attribute of PREFERRED_ATTRIBUTES) {
    const value = element.getAttribute(attribute)?.trim();
    if (!value) continue;
    const selector = attributeSelector(attribute, value);
    append(selector);
    if (uniqueSelector(root, selector)) {
      return { selector, selectorCandidates: candidates };
    }
  }

  if (element.id) {
    const selector = `#${cssEscape(element.id)}`;
    append(selector);
    if (uniqueSelector(root, selector)) {
      return { selector, selectorCandidates: candidates };
    }
  }

  const tagName = element.tagName.toLowerCase();
  const stableClasses = Array.from(element.classList)
    .filter(
      (className) =>
        /^[a-zA-Z_][\w-]*$/.test(className) &&
        !/^(active|current|open|selected|hover|focus|runtime-target|sxpt-)/i.test(
          className
        )
    )
    .slice(0, 3);
  for (let count = 1; count <= stableClasses.length; count += 1) {
    const selector = `${tagName}${stableClasses
      .slice(0, count)
      .map((name) => `.${cssEscape(name)}`)
      .join('')}`;
    append(selector);
    if (uniqueSelector(root, selector)) {
      return { selector, selectorCandidates: candidates };
    }
  }

  const path: string[] = [];
  let current: Element | null = element;
  while (
    current &&
    current !== document.documentElement &&
    current !== document.body &&
    path.length < 7
  ) {
    path.unshift(selectorSegment(current));
    const selector = path.join(' > ');
    append(selector);
    if (uniqueSelector(root, selector)) {
      return { selector, selectorCandidates: candidates };
    }
    if (current.parentElement === root) break;
    current = current.parentElement;
  }

  const selector = path.join(' > ') || tagName;
  append(selector);
  return { selector, selectorCandidates: candidates };
}

export function describePickedElement(element: Element) {
  const formLabel =
    element instanceof HTMLInputElement ||
    element instanceof HTMLSelectElement ||
    element instanceof HTMLTextAreaElement
      ? element.labels?.[0]?.textContent
      : '';
  const closestLabel = element.closest('label')?.textContent;
  const rawText =
    element.getAttribute('data-label') ||
    element.getAttribute('aria-label') ||
    element.getAttribute('title') ||
    formLabel ||
    closestLabel ||
    element.textContent ||
    element.getAttribute('name') ||
    element.id ||
    element.tagName.toLowerCase();
  return rawText.replace(/\s+/g, ' ').trim().slice(0, 80) || '页面元素';
}
