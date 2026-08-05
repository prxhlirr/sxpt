export interface StableElementSelector {
  selector: string;
  selectorCandidates: string[];
}

/**
 * 业务功能：为被录制的业务元素生成稳定 CSS 选择器。
 * 关键流程：优先使用业务语义属性，其次使用 id/name，最后生成相对根节点的结构路径。
 */
export function buildStableElementSelector(
  target: Element,
  root: ParentNode = document
): StableElementSelector {
  const selectorCandidates = buildSelectorCandidates(target, root);
  return {
    selector: selectorCandidates[0] ?? target.tagName.toLowerCase(),
    selectorCandidates
  };
}

/**
 * 业务功能：生成人可读的录制元素描述，作为教学节点默认动作文案。
 * 关键流程：按业务标签、可访问性标签、文本和表单名称依次兜底。
 */
export function describePickedElement(target: Element): string {
  return (
    target.getAttribute('data-label') ||
    target.getAttribute('aria-label') ||
    target.getAttribute('placeholder') ||
    target.getAttribute('name') ||
    target.textContent?.trim() ||
    target.tagName.toLowerCase()
  ).trim();
}

function buildSelectorCandidates(target: Element, root: ParentNode): string[] {
  const candidates = [
    attrSelector(target, 'data-action'),
    attrSelector(target, 'data-business-field'),
    attrSelector(target, 'data-training-id'),
    idSelector(target),
    attrSelector(target, 'name'),
    structuralSelector(target, root),
    target.tagName.toLowerCase()
  ].filter((selector): selector is string => Boolean(selector));
  return [...new Set(candidates)].filter((selector) =>
    selectorMatchesTarget(selector, target, root)
  );
}

function attrSelector(target: Element, attributeName: string): string | undefined {
  const value = target.getAttribute(attributeName);
  return value ? `[${attributeName}="${cssEscape(value)}"]` : undefined;
}

function idSelector(target: Element): string | undefined {
  return target.id ? `#${cssEscape(target.id)}` : undefined;
}

function structuralSelector(target: Element, root: ParentNode): string {
  const segments: string[] = [];
  let current: Element | null = target;
  while (current && current !== root) {
    const parent: HTMLElement | null = current.parentElement;
    const tagName = current.tagName.toLowerCase();
    if (!parent) {
      segments.unshift(tagName);
      break;
    }
    const currentTagName = current.tagName;
    const sameTagSiblings = Array.from(parent.children).filter(
      (child): child is Element => child.tagName === currentTagName
    );
    if (sameTagSiblings.length <= 1) {
      segments.unshift(tagName);
    } else {
      segments.unshift(`${tagName}:nth-of-type(${sameTagSiblings.indexOf(current) + 1})`);
    }
    current = parent;
  }
  return segments.join(' > ');
}

function selectorMatchesTarget(
  selector: string,
  target: Element,
  root: ParentNode
): boolean {
  try {
    return root.querySelector(selector) === target;
  } catch {
    return false;
  }
}

function cssEscape(value: string): string {
  if (typeof CSS !== 'undefined' && CSS.escape) return CSS.escape(value);
  return value.replace(/["\\]/g, '\\$&');
}
