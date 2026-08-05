import type { BusinessPageSnapshot } from '../domain/models';
import { normalizeViewport } from './viewportScaling';

const MAX_SNAPSHOT_HTML_LENGTH = 700_000;
const MAX_SNAPSHOT_CSS_LENGTH = 1_500_000;
const MAX_SNAPSHOT_STYLE_URLS = 64;
const BLOCKED_ELEMENTS = 'script,noscript,iframe,object,embed,base';

interface SnapshotContext {
  pageUrl: string;
  pageTitle: string;
  viewport?: {
    width: number;
    height: number;
  };
}

function clampText(value: string, maxLength: number) {
  return value.length > maxLength ? value.slice(0, maxLength) : value;
}

function copyFormState(source: ParentNode, clone: ParentNode) {
  const sourceControls = Array.from(
    source.querySelectorAll<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>(
      'input, textarea, select'
    )
  );
  const clonedControls = Array.from(
    clone.querySelectorAll<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>(
      'input, textarea, select'
    )
  );

  sourceControls.forEach((control, index) => {
    const clonedControl = clonedControls[index];
    if (!clonedControl) return;

    if (control instanceof HTMLInputElement && clonedControl instanceof HTMLInputElement) {
      if (control.type === 'password' || control.dataset.snapshotMask === 'true') {
        clonedControl.value = control.value ? '••••••••' : '';
      } else {
        clonedControl.value = control.value;
      }
      clonedControl.setAttribute('value', clonedControl.value);
      clonedControl.toggleAttribute('checked', control.checked);
      return;
    }

    if (
      control instanceof HTMLTextAreaElement &&
      clonedControl instanceof HTMLTextAreaElement
    ) {
      clonedControl.value =
        control.dataset.snapshotMask === 'true' && control.value
          ? '••••••••'
          : control.value;
      clonedControl.textContent = clonedControl.value;
      return;
    }

    if (control instanceof HTMLSelectElement && clonedControl instanceof HTMLSelectElement) {
      Array.from(clonedControl.options).forEach((option, optionIndex) => {
        option.toggleAttribute('selected', optionIndex === control.selectedIndex);
      });
    }
  });
}

function sanitizeElementTree(root: Element) {
  root.querySelectorAll(BLOCKED_ELEMENTS).forEach((element) => element.remove());
  root.querySelectorAll('*').forEach((element) => {
    Array.from(element.attributes).forEach((attribute) => {
      if (
        attribute.name.toLowerCase().startsWith('on') ||
        attribute.name.toLowerCase() === 'srcdoc'
      ) {
        element.removeAttribute(attribute.name);
      }
    });
    if (
      element instanceof HTMLInputElement ||
      element instanceof HTMLTextAreaElement ||
      element instanceof HTMLSelectElement ||
      element instanceof HTMLButtonElement
    ) {
      element.setAttribute(
        'data-sxpt-original-disabled',
        String(element.disabled)
      );
      element.disabled = true;
      element.setAttribute('disabled', '');
    }
  });
  return root;
}

export function resolveSnapshotResourceUrl(
  value: string,
  baseUrl: string
): string | undefined {
  const normalized = value.trim();
  if (!normalized || normalized.startsWith('#')) return undefined;
  try {
    const resolved = new URL(normalized, baseUrl);
    return ['http:', 'https:'].includes(resolved.protocol)
      ? resolved.href
      : undefined;
  } catch {
    return undefined;
  }
}

function copyMediaState(source: ParentNode, clone: ParentNode) {
  const sourceImages = Array.from(source.querySelectorAll<HTMLImageElement>('img'));
  const clonedImages = Array.from(clone.querySelectorAll<HTMLImageElement>('img'));
  sourceImages.forEach((image, index) => {
    const clonedImage = clonedImages[index];
    if (!clonedImage) return;
    const renderedSource =
      image.currentSrc ||
      image.src ||
      image.dataset.src ||
      image.dataset.original;
    if (renderedSource) {
      clonedImage.setAttribute('src', renderedSource);
      clonedImage.removeAttribute('srcset');
    }
    clonedImage.removeAttribute('crossorigin');
    clonedImage.setAttribute('loading', 'eager');
  });
}

export function rebaseSnapshotCssUrls(cssText: string, baseUrl: string) {
  const withRebasedUrls = cssText.replace(
    /url\(\s*(?:(["'])(.*?)\1|([^\s"')][^)]*))\s*\)/gi,
    (source, quote: string | undefined, quotedValue: string, bareValue: string) => {
      const value = String(quotedValue ?? bareValue ?? '').trim();
      if (/^(?:data:|blob:|https?:|#)/i.test(value)) return source;
      const resolved = resolveSnapshotResourceUrl(value, baseUrl);
      if (!resolved) return source;
      const wrapper = quote ?? '';
      return `url(${wrapper}${resolved}${wrapper})`;
    }
  );
  return withRebasedUrls.replace(
    /@import\s+(["'])(.*?)\1/gi,
    (source, quote: string, value: string) => {
      const resolved = resolveSnapshotResourceUrl(value, baseUrl);
      return resolved ? `@import ${quote}${resolved}${quote}` : source;
    }
  );
}

function rebaseSnapshotSrcset(value: string, baseUrl: string) {
  if (/^\s*(?:data:|blob:)/i.test(value)) return value;
  return value
    .split(',')
    .map((candidate) => {
      const match = candidate.trim().match(/^(\S+)(\s+.*)?$/);
      if (!match) return candidate;
      const resolved = resolveSnapshotResourceUrl(match[1], baseUrl);
      return resolved ? `${resolved}${match[2] ?? ''}` : candidate.trim();
    })
    .join(', ');
}

function rebaseSnapshotMarkupResources(root: Element, baseUrl: string) {
  const resourceAttributes = [
    'src',
    'poster',
    'href',
    'xlink:href',
    'action',
    'background'
  ];
  [root, ...Array.from(root.querySelectorAll('*'))].forEach((element) => {
    resourceAttributes.forEach((attributeName) => {
      const value = element.getAttribute(attributeName);
      if (!value || /^(?:data:|blob:|#)/i.test(value.trim())) return;
      const resolved = resolveSnapshotResourceUrl(value, baseUrl);
      if (resolved) {
        element.setAttribute(attributeName, resolved);
      } else if (/^(?:javascript:|vbscript:)/i.test(value.trim())) {
        element.removeAttribute(attributeName);
      }
    });
    const srcset = element.getAttribute('srcset');
    if (srcset) element.setAttribute('srcset', rebaseSnapshotSrcset(srcset, baseUrl));
    const inlineStyle = element.getAttribute('style');
    if (inlineStyle) {
      element.setAttribute('style', rebaseSnapshotCssUrls(inlineStyle, baseUrl));
    }
  });
}

export function normalizeSnapshotStyleUrls(
  value: unknown,
  baseUrl: string
): string[] {
  if (!Array.isArray(value)) return [];
  return [
    ...new Set(
      value
        .filter((item): item is string => typeof item === 'string')
        .map((item) => resolveSnapshotResourceUrl(item, baseUrl))
        .filter((item): item is string => Boolean(item))
    )
  ].slice(0, MAX_SNAPSHOT_STYLE_URLS);
}

function collectDocumentStyles(ownerDocument: Document) {
  const cssParts: string[] = [];
  const styleUrls: string[] = [];
  Array.from(ownerDocument.styleSheets).forEach((styleSheet) => {
    const stylesheetBase = styleSheet.href || ownerDocument.baseURI;
    if (styleSheet.href) {
      const styleUrl = resolveSnapshotResourceUrl(
        styleSheet.href,
        ownerDocument.baseURI
      );
      if (styleUrl) styleUrls.push(styleUrl);
    }
    try {
      const rules = Array.from(styleSheet.cssRules);
      cssParts.push(
        rebaseSnapshotCssUrls(
          rules.map((rule) => rule.cssText).join('\n'),
          stylesheetBase
        )
      );
    } catch {
      // Cross-origin rules are restored through their absolute stylesheet URL.
    }
  });
  return {
    cssText: clampText(cssParts.join('\n'), MAX_SNAPSHOT_CSS_LENGTH),
    styleUrls: [...new Set(styleUrls)].slice(0, MAX_SNAPSHOT_STYLE_URLS)
  };
}

export function captureBusinessPageSnapshot(
  root: HTMLElement,
  context: SnapshotContext
): BusinessPageSnapshot {
  const clone = root.cloneNode(true) as HTMLElement;
  copyFormState(root, clone);
  copyMediaState(root, clone);
  sanitizeElementTree(clone);
  rebaseSnapshotMarkupResources(clone, root.ownerDocument.baseURI);
  clone.classList.add('sxpt-recorded-business-root');
  const styles = collectDocumentStyles(root.ownerDocument);

  return {
    version: 1,
    format: 'DOM',
    pageUrl: context.pageUrl,
    pageTitle: context.pageTitle,
    capturedAt: new Date().toISOString(),
    html: clampText(clone.outerHTML, MAX_SNAPSHOT_HTML_LENGTH),
    cssText: styles.cssText,
    styleUrls: styles.styleUrls.length ? styles.styleUrls : undefined,
    viewport:
      normalizeViewport(context.viewport) ?? {
        width: window.innerWidth,
        height: window.innerHeight
      }
  };
}

export function normalizeBusinessPageSnapshot(
  value: unknown,
  fallback: SnapshotContext
): BusinessPageSnapshot | undefined {
  if (!value || typeof value !== 'object') return undefined;
  const candidate = value as Partial<BusinessPageSnapshot>;
  if (
    candidate.format !== 'DOM' ||
    typeof candidate.html !== 'string' ||
    !candidate.html.trim()
  ) {
    return undefined;
  }

  const viewport =
    normalizeViewport(candidate.viewport) ?? normalizeViewport(fallback.viewport);
  const fallbackBaseUrl = resolveSnapshotBaseUrl(fallback.pageUrl);
  const pageUrl = resolveSnapshotBaseUrl(
    typeof candidate.pageUrl === 'string' && candidate.pageUrl
      ? candidate.pageUrl
      : fallback.pageUrl,
    fallbackBaseUrl
  );
  const baseUrl = resolveSnapshotBaseUrl(pageUrl, fallbackBaseUrl);

  return {
    version: 1,
    format: 'DOM',
    pageUrl,
    pageTitle:
      typeof candidate.pageTitle === 'string' && candidate.pageTitle
        ? candidate.pageTitle
        : fallback.pageTitle,
    capturedAt:
      typeof candidate.capturedAt === 'string' && candidate.capturedAt
        ? candidate.capturedAt
        : new Date().toISOString(),
    html: clampText(candidate.html, MAX_SNAPSHOT_HTML_LENGTH),
    cssText:
      typeof candidate.cssText === 'string'
        ? clampText(candidate.cssText, MAX_SNAPSHOT_CSS_LENGTH)
        : undefined,
    styleUrls: normalizeSnapshotStyleUrls(candidate.styleUrls, baseUrl),
    viewport
  };
}

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

interface SanitizedSnapshotMarkup {
  html: string;
  bodyAttributes: string;
}

function sanitizeSnapshotHtml(
  html: string,
  selectors: string[] = [],
  interactive = false,
  clearFormValues = false,
  baseUrl = resolveSnapshotBaseUrl('/')
): SanitizedSnapshotMarkup {
  const isBodySnapshot = /^\s*(?:<!doctype[^>]*>\s*)?<body(?:\s|>)/i.test(html);
  const parsed = new DOMParser().parseFromString(
    isBodySnapshot
      ? `<!doctype html><html><head></head>${html}</html>`
      : `<!doctype html><html><head></head><body>${html}</body></html>`,
    'text/html'
  );
  const body = parsed.body;
  body.querySelectorAll(BLOCKED_ELEMENTS).forEach((element) => element.remove());
  [body, ...Array.from(body.querySelectorAll('*'))].forEach((element) => {
    Array.from(element.attributes).forEach((attribute) => {
      if (
        attribute.name.toLowerCase().startsWith('on') ||
        attribute.name.toLowerCase() === 'srcdoc'
      ) {
        element.removeAttribute(attribute.name);
      }
    });
    if (
      interactive &&
      (element instanceof HTMLInputElement ||
        element instanceof HTMLTextAreaElement ||
        element instanceof HTMLSelectElement ||
        element instanceof HTMLButtonElement)
    ) {
      if (element.dataset.sxptOriginalDisabled !== 'true') {
        element.disabled = false;
        element.removeAttribute('disabled');
        element.removeAttribute('aria-disabled');
      }
    }
    if (
      clearFormValues &&
      (element instanceof HTMLInputElement ||
        element instanceof HTMLTextAreaElement ||
        element instanceof HTMLSelectElement)
    ) {
      const isRecordedBusinessField =
        element.hasAttribute('data-business-field') ||
        element.hasAttribute('data-training-id') ||
        element.hasAttribute('name') ||
        element.hasAttribute('id') ||
        selectors.some((selector) => {
          try {
            return element.matches(selector);
          } catch {
            return false;
          }
        });
      if (
        !isRecordedBusinessField ||
        element.dataset.sxptOriginalDisabled === 'true' ||
        element.hasAttribute('readonly') ||
        (element instanceof HTMLInputElement &&
          ['hidden', 'button', 'submit', 'reset'].includes(element.type))
      ) {
        return;
      }
      if (element instanceof HTMLSelectElement) {
        Array.from(element.options).forEach((option) => {
          option.selected = false;
          option.removeAttribute('selected');
        });
        const placeholder = element.ownerDocument.createElement('option');
        placeholder.value = '';
        placeholder.textContent = '请选择';
        placeholder.selected = true;
        placeholder.setAttribute('selected', '');
        element.prepend(placeholder);
        element.selectedIndex = 0;
      } else if (element instanceof HTMLInputElement) {
        if (element.type === 'checkbox' || element.type === 'radio') {
          element.checked = false;
          element.removeAttribute('checked');
        } else {
          element.value = '';
          element.removeAttribute('value');
        }
      } else {
        element.value = '';
        element.textContent = '';
      }
    }
  });
  rebaseSnapshotMarkupResources(body, baseUrl);
  for (const selector of selectors) {
    if (!selector) continue;
    try {
      const target = body.querySelector(selector);
      if (!target) continue;
      target.classList.add('sxpt-recorded-operation-target');
      target.setAttribute('data-sxpt-operation-label', '操作位置');
      break;
    } catch {
      // Invalid or legacy selectors fall through to the recorded-rect overlay.
    }
  }
  if (isBodySnapshot) body.classList.add('sxpt-recorded-business-root');
  return {
    html: body.innerHTML,
    bodyAttributes: isBodySnapshot ? serializeElementAttributes(body) : ''
  };
}

function serializeElementAttributes(element: Element) {
  return Array.from(element.attributes)
    .map(
      (attribute) =>
        `${attribute.name}="${escapeHtml(attribute.value)}"`
    )
    .join(' ');
}

function resolveSnapshotBaseUrl(pageUrl: string, fallbackUrl?: string) {
  const fallbackOrigin =
    typeof window !== 'undefined' ? window.location.origin : 'http://localhost';
  try {
    const fallbackBase = fallbackUrl
      ? new URL(fallbackUrl, `${fallbackOrigin}/`).href
      : `${fallbackOrigin}/`;
    return new URL(pageUrl || fallbackBase, fallbackBase).href;
  } catch {
    return fallbackUrl || `${fallbackOrigin}/`;
  }
}

export function createBusinessSnapshotDocument(
  snapshot: BusinessPageSnapshot,
  selectors: string[] = [],
  interactive = false,
  clearFormValues = false,
  resourceBaseUrl?: string
) {
  const baseUrl = resolveSnapshotBaseUrl(snapshot.pageUrl, resourceBaseUrl);
  const safeMarkup = sanitizeSnapshotHtml(
    snapshot.html,
    selectors,
    interactive,
    clearFormValues,
    baseUrl
  );
  const safeCss = rebaseSnapshotCssUrls(snapshot.cssText ?? '', baseUrl).replace(
    /<\/style/gi,
    '<\\/style'
  );
  const styleLinks = normalizeSnapshotStyleUrls(
    snapshot.styleUrls,
    baseUrl
  )
    .map(
      (url) =>
        `<link rel="stylesheet" href="${escapeHtml(url)}" referrerpolicy="no-referrer" />`
    )
    .join('\n    ');

  return `<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <base href="${escapeHtml(baseUrl)}" />
    ${styleLinks}
    <style>${safeCss}</style>
    <style>
      html, body { width: 100%; height: 100%; margin: 0; overflow: auto; background: #eef2f7; }
      body {
        pointer-events: ${interactive ? 'auto' : 'none'};
        user-select: ${interactive ? 'auto' : 'none'};
      }
      .sxpt-recorded-business-root { position: relative !important; inset: auto !important; width: 100% !important; min-height: 100vh !important; }
      .sxpt-recorded-operation-target {
        position: relative !important;
        z-index: 2147483000 !important;
        outline: 4px solid #ff9f1c !important;
        outline-offset: 4px !important;
        border-radius: 6px !important;
        box-shadow: 0 0 0 9px rgb(255 159 28 / 24%), 0 0 28px rgb(255 159 28 / 72%) !important;
        animation: sxpt-operation-pulse 1.2s ease-in-out infinite alternate !important;
      }
      .sxpt-recorded-operation-target::after {
        position: absolute !important;
        z-index: 2147483001 !important;
        top: -34px !important;
        left: 0 !important;
        display: block !important;
        border-radius: 999px !important;
        padding: 6px 10px !important;
        color: #fff !important;
        background: #e66b00 !important;
        box-shadow: 0 8px 20px rgb(116 50 0 / 26%) !important;
        content: attr(data-sxpt-operation-label) !important;
        font: 800 12px/1 sans-serif !important;
        white-space: nowrap !important;
      }
      @keyframes sxpt-operation-pulse {
        from { outline-color: #ff9f1c; box-shadow: 0 0 0 5px rgb(255 159 28 / 18%), 0 0 18px rgb(255 159 28 / 48%); }
        to { outline-color: #ff5d2e; box-shadow: 0 0 0 12px rgb(255 93 46 / 20%), 0 0 34px rgb(255 93 46 / 76%); }
      }
    </style>
  </head>
  <body${safeMarkup.bodyAttributes ? ` ${safeMarkup.bodyAttributes}` : ''}>
    ${safeMarkup.html}
    ${
      interactive
        ? `<script>
      (() => {
        let composing = false;
        const selectorFor = (element) => {
          if (element.dataset.action) {
            return '[data-action="' + element.dataset.action + '"]';
          }
          if (element.dataset.businessField) {
            return '[data-business-field="' + element.dataset.businessField + '"]';
          }
          if (element.dataset.trainingId) {
            return '[data-training-id="' + element.dataset.trainingId + '"]';
          }
          if (element.id) return '#' + CSS.escape(element.id);
          if (element.getAttribute('name')) {
            return '[name="' + CSS.escape(element.getAttribute('name')) + '"]';
          }
          return element.tagName.toLowerCase();
        };
        const actionElement = (target) =>
          target instanceof Element
            ? target.closest(
                '[data-action], [data-business-field], [data-training-id], button, input, select, textarea, a'
              )
            : null;
        const report = (element, actionType) => {
          const selector = selectorFor(element);
          window.parent.postMessage(
            {
              type: 'SXPT_SNAPSHOT_ACTION',
              payload: {
                actionType,
                selector,
                selectorCandidates: [
                  selector,
                  element.id ? '#' + CSS.escape(element.id) : '',
                  element.tagName.toLowerCase()
                ].filter(Boolean),
                text: (
                  element.dataset.label ||
                  element.textContent ||
                  element.getAttribute('aria-label') ||
                  element.getAttribute('name') ||
                  '业务操作'
                ).trim()
              }
            },
            '*'
          );
        };
        document.addEventListener('click', (event) => {
          const element = actionElement(event.target);
          if (!element) return;
          if (element.matches('a') || element.matches('[type="submit"]')) {
            event.preventDefault();
          }
          if (element.matches('input, textarea, select')) return;
          report(element, 'click');
        });
        document.addEventListener('change', (event) => {
          const element = actionElement(event.target);
          if (!element) return;
          if (
            element.matches('input, textarea') &&
            !String(element.value || '').trim()
          ) {
            return;
          }
          report(
            element,
            element.matches('select') ? 'select' : 'input'
          );
        });
        document.addEventListener('compositionstart', () => {
          composing = true;
        });
        document.addEventListener('compositionend', (event) => {
          composing = false;
          const element = actionElement(event.target);
          if (
            element &&
            element.matches('input, textarea') &&
            String(element.value || '').trim()
          ) {
            report(element, 'input');
          }
        });
        document.addEventListener('input', (event) => {
          const element = actionElement(event.target);
          if (
            composing ||
            event.isComposing ||
            !element ||
            !element.matches('input, textarea') ||
            !String(element.value || '').trim()
          ) {
            return;
          }
          report(element, 'input');
        });
        document.addEventListener('submit', (event) => event.preventDefault());
      })();
    <\/script>`
        : ''
    }
  </body>
</html>`;
}
