import type { BusinessPageSnapshot } from '../domain/models';

const MAX_SNAPSHOT_HTML_LENGTH = 700_000;
const MAX_SNAPSHOT_CSS_LENGTH = 400_000;
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

function collectDocumentCss() {
  const cssParts: string[] = [];
  Array.from(document.styleSheets).forEach((styleSheet) => {
    try {
      const rules = Array.from(styleSheet.cssRules);
      cssParts.push(rules.map((rule) => rule.cssText).join('\n'));
    } catch {
      // Cross-origin stylesheets cannot be inspected; the DOM snapshot remains usable.
    }
  });
  return clampText(cssParts.join('\n'), MAX_SNAPSHOT_CSS_LENGTH);
}

export function captureBusinessPageSnapshot(
  root: HTMLElement,
  context: SnapshotContext
): BusinessPageSnapshot {
  const clone = root.cloneNode(true) as HTMLElement;
  copyFormState(root, clone);
  sanitizeElementTree(clone);
  clone.classList.add('sxpt-recorded-business-root');

  return {
    version: 1,
    format: 'DOM',
    pageUrl: context.pageUrl,
    pageTitle: context.pageTitle,
    capturedAt: new Date().toISOString(),
    html: clampText(clone.outerHTML, MAX_SNAPSHOT_HTML_LENGTH),
    cssText: collectDocumentCss(),
    viewport:
      context.viewport ??
      {
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
    candidate.viewport &&
    Number.isFinite(candidate.viewport.width) &&
    Number.isFinite(candidate.viewport.height)
      ? {
          width: Number(candidate.viewport.width),
          height: Number(candidate.viewport.height)
        }
      : fallback.viewport;

  return {
    version: 1,
    format: 'DOM',
    pageUrl:
      typeof candidate.pageUrl === 'string' && candidate.pageUrl
        ? candidate.pageUrl
        : fallback.pageUrl,
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

function sanitizeSnapshotHtml(
  html: string,
  selectors: string[] = [],
  interactive = false,
  clearFormValues = false
) {
  const parsed = new DOMParser().parseFromString(
    `<body>${html}</body>`,
    'text/html'
  );
  const body = parsed.body;
  body.querySelectorAll(BLOCKED_ELEMENTS).forEach((element) => element.remove());
  body.querySelectorAll('*').forEach((element) => {
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
  return body.innerHTML;
}

export function createBusinessSnapshotDocument(
  snapshot: BusinessPageSnapshot,
  selectors: string[] = [],
  interactive = false,
  clearFormValues = false
) {
  const safeHtml = sanitizeSnapshotHtml(
    snapshot.html,
    selectors,
    interactive,
    clearFormValues
  );
  const safeCss = (snapshot.cssText ?? '').replace(/<\/style/gi, '<\\/style');
  const baseUrl = /^https?:\/\//i.test(snapshot.pageUrl)
    ? snapshot.pageUrl
    : window.location.origin;

  return `<!doctype html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <base href="${escapeHtml(baseUrl)}" />
    <style>${safeCss}</style>
    <style>
      html, body { width: 100%; height: 100%; margin: 0; overflow: auto; background: #eef2f7; }
      body {
        pointer-events: ${interactive ? 'auto' : 'none'};
        user-select: ${interactive ? 'auto' : 'none'};
      }
      .sxpt-snapshot-viewport { width: 100%; min-height: 100%; }
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
  <body>
    <div class="sxpt-snapshot-viewport">${safeHtml}</div>
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
