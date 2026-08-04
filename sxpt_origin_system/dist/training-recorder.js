(function () {
  var script = document.currentScript;
  var dataset = script && script.dataset ? script.dataset : {};
  var config = {
    parentOrigin: dataset.teachingParentOrigin || '*',
    allowedParentOrigins: parseOrigins(
      dataset.teachingAllowedParentOrigins || dataset.teachingParentOrigin || '*'
    ),
    record: dataset.teachingRecord !== 'false',
    playback: dataset.teachingPlayback !== 'false',
    preventSubmit: dataset.teachingPreventSubmit !== 'false'
  };
  var sdkSessionId = createId('sdk');
  var captureSessionId = '';
  var sequenceNo = 0;
  var recordingEnabled = config.record;
  var isPlayingBack = false;
  var elementPickActive = false;
  var pickedHoverElement = null;
  var pickerOverlay = null;
  var pickerLabel = null;
  var lastSentAction = { signature: '', at: 0 };
  var routeGeneration = 0;

  function parseOrigins(value) {
    if (Array.isArray(value)) return value.filter(Boolean);
    return String(value || '*')
      .split(',')
      .map(function (origin) { return origin.trim(); })
      .filter(Boolean);
  }

  function updateConfig(nextConfig) {
    if (!nextConfig) return;
    if (nextConfig.parentOrigin) config.parentOrigin = nextConfig.parentOrigin;
    if (nextConfig.allowedParentOrigins) {
      config.allowedParentOrigins = parseOrigins(nextConfig.allowedParentOrigins);
    }
    if (typeof nextConfig.record === 'boolean') {
      config.record = nextConfig.record;
      recordingEnabled = nextConfig.record;
    }
    if (typeof nextConfig.playback === 'boolean') config.playback = nextConfig.playback;
    if (typeof nextConfig.preventSubmit === 'boolean') {
      config.preventSubmit = nextConfig.preventSubmit;
    }
  }

  window.TeachingRecorderSDK = {
    init: updateConfig,
    getConfig: function () {
      return {
        parentOrigin: config.parentOrigin,
        allowedParentOrigins: config.allowedParentOrigins.slice(),
        record: recordingEnabled,
        playback: config.playback,
        preventSubmit: config.preventSubmit,
        sdkSessionId: sdkSessionId,
        captureSessionId: captureSessionId,
        sequenceNo: sequenceNo
      };
    }
  };

  function isAllowedParentOrigin(origin) {
    return (
      config.allowedParentOrigins.indexOf('*') >= 0 ||
      config.allowedParentOrigins.indexOf(origin) >= 0
    );
  }

  function getActionType(actionElement, eventType) {
    if (actionElement.matches('input, textarea')) return 'input';
    if (actionElement.matches('select')) return 'select';
    if (actionElement.dataset.action === 'submit') return 'submit';
    if (eventType === 'submit') return 'submit';
    return 'click';
  }

  function getElementKind(actionElement) {
    if (actionElement.matches('input, textarea')) return 'text-control';
    if (actionElement.matches('select')) return 'select';
    if (actionElement.matches('form')) return 'form';
    return 'command';
  }

  function shouldEmitEvent(actionElement, eventType) {
    var kind = getElementKind(actionElement);
    if (eventType === 'submit') return true;
    if (kind === 'text-control' || kind === 'select') return eventType === 'change';
    if (kind === 'form') return false;
    return eventType === 'click';
  }

  function getActionKey(actionElement) {
    return (
      actionElement.dataset.action ||
      actionElement.id ||
      actionElement.name ||
      actionElement.getAttribute('aria-label') ||
      ''
    );
  }

  function getActionText(actionElement, action) {
    return (
      actionElement.dataset.label ||
      actionElement.innerText ||
      actionElement.getAttribute('aria-label') ||
      actionElement.name ||
      action
    );
  }

  function getActionValue(actionElement) {
    return 'value' in actionElement ? actionElement.value : undefined;
  }

  function escapeSelector(value) {
    if (window.CSS && CSS.escape) return CSS.escape(value);
    return String(value).replace(/"/g, '\\"');
  }

  function getSelectorCandidates(actionElement) {
    var candidates = [];
    var tagName = actionElement.tagName.toLowerCase();
    var preferredAttributes = [
      'data-training-id',
      'data-business-field',
      'data-action',
      'data-testid',
      'data-test'
    ];
    preferredAttributes.forEach(function (attribute) {
      var value = actionElement.getAttribute(attribute);
      if (!value) return;
      pushUniqueSelector(
        candidates,
        '[' + attribute + '="' + escapeSelector(value) + '"]',
        actionElement
      );
    });
    if (actionElement.id) {
      pushUniqueSelector(candidates, '#' + escapeSelector(actionElement.id), actionElement);
    }
    if (actionElement.name) {
      pushUniqueSelector(
        candidates,
        tagName + '[name="' + escapeSelector(actionElement.name) + '"]',
        actionElement
      );
    }
    var ariaLabel = actionElement.getAttribute('aria-label');
    if (ariaLabel) {
      pushUniqueSelector(
        candidates,
        tagName + '[aria-label="' + escapeSelector(ariaLabel) + '"]',
        actionElement
      );
    }
    pushUniqueSelector(candidates, getUniqueCssPath(actionElement), actionElement);
    return candidates;
  }

  function pushUniqueSelector(candidates, selector, expectedElement) {
    if (!selector || candidates.indexOf(selector) >= 0) return;
    try {
      var matches = document.querySelectorAll(selector);
      if (matches.length === 1 && matches[0] === expectedElement) candidates.push(selector);
    } catch (error) {
      return;
    }
  }

  function getUniqueCssPath(element) {
    var parts = [];
    var current = element;
    while (current && current.nodeType === 1) {
      var part = current.tagName.toLowerCase();
      var parent = current.parentElement;
      if (parent) {
        var sameTag = Array.prototype.filter.call(parent.children, function (child) {
          return child.tagName === current.tagName;
        });
        if (sameTag.length > 1) {
          part += ':nth-of-type(' + (sameTag.indexOf(current) + 1) + ')';
        }
      }
      parts.unshift(part);
      var selector = parts.join(' > ');
      try {
        if (document.querySelectorAll(selector).length === 1) return selector;
      } catch (error) {
        return '';
      }
      current = parent;
    }
    return parts.join(' > ');
  }

  function getRect(element) {
    var rect = element.getBoundingClientRect();
    return {
      x: Math.round(rect.x),
      y: Math.round(rect.y),
      width: Math.round(rect.width),
      height: Math.round(rect.height)
    };
  }

  function getViewport() {
    return {
      width: window.innerWidth,
      height: window.innerHeight,
      devicePixelRatio: window.devicePixelRatio || 1
    };
  }

  function ensurePickerUi() {
    if (pickerOverlay && document.body.contains(pickerOverlay)) return;
    var style = document.createElement('style');
    style.setAttribute('data-sxpt-element-picker', 'true');
    style.textContent =
      'html.sxpt-element-picking,html.sxpt-element-picking *{cursor:crosshair!important}' +
      '.sxpt-element-picker-overlay{position:fixed;z-index:2147483646;display:none;' +
      'border:2px solid #6d5dfc;border-radius:4px;background:rgba(109,93,252,.12);' +
      'box-shadow:0 0 0 1px rgba(255,255,255,.84),0 0 0 5px rgba(109,93,252,.14);' +
      'pointer-events:none}' +
      '.sxpt-element-picker-overlay.sxpt-visible{display:block}' +
      '.sxpt-element-picker-label{position:absolute;top:-28px;left:-2px;max-width:80vw;' +
      'overflow:hidden;border-radius:5px 5px 5px 0;padding:5px 8px;color:#fff;' +
      'background:#5948dc;font:700 11px/1.35 Consolas,monospace;text-overflow:ellipsis;' +
      'white-space:nowrap}';
    document.head.appendChild(style);
    pickerOverlay = document.createElement('div');
    pickerOverlay.className = 'sxpt-element-picker-overlay';
    pickerOverlay.setAttribute('data-sxpt-element-picker', 'true');
    pickerOverlay.setAttribute('aria-hidden', 'true');
    pickerLabel = document.createElement('span');
    pickerLabel.className = 'sxpt-element-picker-label';
    pickerOverlay.appendChild(pickerLabel);
    document.body.appendChild(pickerOverlay);
  }

  function getPickedElement(target) {
    if (!(target instanceof Element)) return null;
    if (target.closest('[data-sxpt-element-picker]')) return null;
    if (target === document.body || target === document.documentElement) return null;
    var rect = target.getBoundingClientRect();
    if (rect.width < 2 || rect.height < 2) return null;
    return target;
  }

  function updatePickerOverlay(target) {
    ensurePickerUi();
    pickedHoverElement = target;
    if (!target) {
      pickerOverlay.classList.remove('sxpt-visible');
      return;
    }
    var rect = target.getBoundingClientRect();
    var selectors = getSelectorCandidates(target);
    var selector = selectors[0] || getUniqueCssPath(target) || target.tagName.toLowerCase();
    pickerOverlay.style.left = Math.round(rect.left) + 'px';
    pickerOverlay.style.top = Math.round(rect.top) + 'px';
    pickerOverlay.style.width = Math.round(rect.width) + 'px';
    pickerOverlay.style.height = Math.round(rect.height) + 'px';
    pickerLabel.textContent =
      target.tagName.toLowerCase() + '  ' + selector + '  ' +
      Math.round(rect.width) + ' × ' + Math.round(rect.height);
    pickerLabel.style.top = rect.top < 34 ? Math.round(rect.height + 5) + 'px' : '-28px';
    pickerOverlay.classList.add('sxpt-visible');
  }

  function setElementPickActive(enabled, notifyCancelled) {
    elementPickActive = enabled;
    document.documentElement.classList.toggle('sxpt-element-picking', enabled);
    if (enabled) {
      ensurePickerUi();
    } else {
      pickedHoverElement = null;
      updatePickerOverlay(null);
    }
    if (notifyCancelled) {
      window.parent.postMessage(
        { type: 'ELEMENT_PICK_CANCELLED', url: getCurrentBusinessUrl() },
        config.parentOrigin
      );
    }
  }

  function copyPickerSnapshotFormState(source, clone) {
    var sourceControls = source.querySelectorAll('input, textarea, select');
    var cloneControls = clone.querySelectorAll('input, textarea, select');
    Array.prototype.forEach.call(sourceControls, function (control, index) {
      var cloneControl = cloneControls[index];
      if (!cloneControl) return;
      if (control.matches('input')) {
        cloneControl.value =
          control.type === 'password' && control.value ? '••••••••' : control.value;
        cloneControl.setAttribute('value', cloneControl.value);
        cloneControl.toggleAttribute('checked', control.checked);
      } else if (control.matches('textarea')) {
        cloneControl.value = control.value;
        cloneControl.textContent = control.value;
      } else {
        Array.prototype.forEach.call(cloneControl.options, function (option, optionIndex) {
          option.toggleAttribute('selected', optionIndex === control.selectedIndex);
        });
      }
    });
  }

  function createPickerPageSnapshot() {
    var clone = document.body.cloneNode(true);
    copyPickerSnapshotFormState(document.body, clone);
    clone.querySelectorAll(
      'script,noscript,iframe,object,embed,[data-sxpt-element-picker]'
    ).forEach(function (element) {
      element.remove();
    });
    clone.querySelectorAll('*').forEach(function (element) {
      Array.prototype.slice.call(element.attributes).forEach(function (attribute) {
        if (
          attribute.name.toLowerCase().indexOf('on') === 0 ||
          attribute.name.toLowerCase() === 'srcdoc'
        ) {
          element.removeAttribute(attribute.name);
        }
      });
      if (element.matches('input,textarea,select,button')) {
        element.setAttribute('data-sxpt-original-disabled', String(element.disabled));
        element.setAttribute('disabled', '');
      }
    });
    var cssText = Array.prototype.flatMap.call(document.styleSheets, function (sheet) {
      try {
        return Array.prototype.map.call(sheet.cssRules, function (rule) {
          return rule.cssText;
        });
      } catch (error) {
        return [];
      }
    }).join('\n').slice(0, 400000);
    return {
      version: 1,
      format: 'DOM',
      pageUrl: getCurrentBusinessUrl(),
      pageTitle: document.title,
      capturedAt: new Date().toISOString(),
      html: clone.outerHTML.slice(0, 700000),
      cssText: cssText,
      viewport: getViewport()
    };
  }

  function getCurrentBusinessUrl() {
    return (
      window.location.pathname +
      window.location.search +
      window.location.hash
    );
  }

  function maskValue(value) {
    value = String(value || '');
    if (!value) return '';
    if (value.length <= 2) return new Array(value.length + 1).join('*');
    if (value.length <= 4) {
      return value[0] + new Array(value.length - 1).join('*') + value[value.length - 1];
    }
    return value.slice(0, 2) + new Array(value.length - 3).join('*') + value.slice(-2);
  }

  function sendAction(target, eventType) {
    if (!recordingEnabled || isPlayingBack || !target || !target.closest) return;
    var actionElement = target.closest('[data-action], input, textarea, select, button, a');
    if (!actionElement || !shouldEmitEvent(actionElement, eventType)) return;

    var action = getActionKey(actionElement);
    var value = getActionValue(actionElement);
    var selectorCandidates = getSelectorCandidates(actionElement);
    var payload = {
      type: 'BUSINESS_ACTION',
      actionType: getActionType(actionElement, eventType),
      url: getCurrentBusinessUrl(),
      selector: selectorCandidates[0] || actionElement.tagName.toLowerCase(),
      selectorCandidates: selectorCandidates,
      text: String(getActionText(actionElement, action) || '').trim(),
      value: value,
      rect: getRect(actionElement),
      timestamp: new Date().toISOString(),
      clientEventId: createId('client-event'),
      sdkSessionId: sdkSessionId,
      captureSessionId: captureSessionId,
      sequenceNo: ++sequenceNo,
      stableKey: action,
      pageTitle: document.title,
      inputValueMasked: value === undefined ? undefined : maskValue(value),
      recordedViewport: getViewport()
    };
    var signature = [
      payload.actionType,
      payload.url,
      payload.selector,
      payload.value || ''
    ].join('|');
    var now = Date.now();
    if (lastSentAction.signature === signature && now - lastSentAction.at < 300) return;
    lastSentAction = { signature: signature, at: now };
    window.parent.postMessage(payload, config.parentOrigin);
  }

  function sendReady() {
    window.parent.postMessage(
      {
        type: 'BUSINESS_READY',
        url: getCurrentBusinessUrl(),
        title: document.title,
        sdkSessionId: sdkSessionId,
        timestamp: new Date().toISOString()
      },
      config.parentOrigin
    );
  }

  function sendNavigating() {
    routeGeneration += 1;
    window.parent.postMessage(
      {
        type: 'BUSINESS_NAVIGATING',
        url: getCurrentBusinessUrl(),
        timestamp: new Date().toISOString()
      },
      config.parentOrigin
    );
  }

  function notifyRouteChange() {
    sendNavigating();
    window.setTimeout(sendReady, 0);
  }

  function wrapHistoryMethod(methodName) {
    var original = window.history && window.history[methodName];
    if (typeof original !== 'function') return;
    window.history[methodName] = function () {
      var result = original.apply(window.history, arguments);
      notifyRouteChange();
      return result;
    };
  }

  function sendPlaybackResult(requestId, success, target, error, selector) {
    window.parent.postMessage(
      {
        type: 'PLAYBACK_RESULT',
        requestId: requestId || '',
        success: success,
        url: getCurrentBusinessUrl(),
        rect: target ? getRect(target) : undefined,
        selector: selector,
        viewport: getViewport(),
        error: error
      },
      config.parentOrigin
    );
  }

  function dispatchNativeEvent(element, eventName) {
    element.dispatchEvent(new Event(eventName, { bubbles: true }));
  }

  function getLocatorCandidates(locator) {
    var candidates = [];
    if (locator && locator.selector) candidates.push(locator.selector);
    if (locator && Array.isArray(locator.selectorCandidates)) {
      locator.selectorCandidates.forEach(function (selector) {
        if (selector && candidates.indexOf(selector) < 0) candidates.push(selector);
      });
    }
    return candidates;
  }

  function resolveTarget(locator) {
    var candidates = getLocatorCandidates(locator);
    for (var index = 0; index < candidates.length; index += 1) {
      try {
        var matches = document.querySelectorAll(candidates[index]);
        if (matches.length === 1) {
          return { target: matches[0], selector: candidates[index] };
        }
      } catch (error) {
        continue;
      }
    }
    return null;
  }

  function isCurrentRoute(expectedGeneration, expectedUrl) {
    return (
      routeGeneration === expectedGeneration &&
      getCurrentBusinessUrl() === expectedUrl
    );
  }

  function resolveTargetEventually(
    locator,
    callback,
    deadlineAt,
    expectedGeneration,
    expectedUrl
  ) {
    if (!isCurrentRoute(expectedGeneration, expectedUrl)) {
      callback(null, 'ACTION_CANCELED');
      return;
    }
    if (Date.now() >= deadlineAt) {
      callback(null, 'ELEMENT_NOT_FOUND');
      return;
    }
    var resolved = resolveTarget(locator);
    if (resolved) {
      callback(resolved);
      return;
    }
    window.setTimeout(function () {
      resolveTargetEventually(
        locator,
        callback,
        deadlineAt,
        expectedGeneration,
        expectedUrl
      );
    }, 100);
  }

  function afterTargetIsVisible(target, callback) {
    if (typeof target.scrollIntoView === 'function') {
      target.scrollIntoView({ behavior: 'auto', block: 'center', inline: 'nearest' });
    }
    window.requestAnimationFrame(function () {
      window.requestAnimationFrame(callback);
    });
  }

  function sendTargetResolutionResult(requestId, resolved, error) {
    window.parent.postMessage(
      {
        type: 'TARGET_RESOLUTION_RESULT',
        requestId: requestId || '',
        success: Boolean(resolved),
        url: getCurrentBusinessUrl(),
        selector: resolved ? resolved.selector : undefined,
        rect: resolved ? getRect(resolved.target) : undefined,
        viewport: getViewport(),
        error: error
      },
      config.parentOrigin
    );
  }

  function resolveRecordedTarget(requestId, locator) {
    if (!config.playback || !locator) {
      sendTargetResolutionResult(requestId, null, 'ACTION_UNSUPPORTED');
      return;
    }
    var expectedGeneration = routeGeneration;
    var expectedUrl = getCurrentBusinessUrl();
    var deadlineAt = Date.now() + 4500;
    resolveTargetEventually(locator, function (resolved, error) {
      if (!resolved || error) {
        sendTargetResolutionResult(requestId, null, error || 'ELEMENT_NOT_FOUND');
        return;
      }
      afterTargetIsVisible(resolved.target, function () {
        if (
          !isCurrentRoute(expectedGeneration, expectedUrl) ||
          Date.now() >= deadlineAt
        ) {
          sendTargetResolutionResult(requestId, null, 'ACTION_CANCELED');
          return;
        }
        sendTargetResolutionResult(requestId, resolved);
      });
    }, deadlineAt, expectedGeneration, expectedUrl);
  }

  function playResolvedStep(
    requestId,
    step,
    resolved,
    error,
    expectedGeneration,
    expectedUrl,
    deadlineAt
  ) {
    if (!resolved || error) {
      sendPlaybackResult(requestId, false, null, error || 'ELEMENT_NOT_FOUND');
      return;
    }

    afterTargetIsVisible(resolved.target, function () {
      if (
        !isCurrentRoute(expectedGeneration, expectedUrl) ||
        Date.now() >= deadlineAt
      ) {
        sendPlaybackResult(requestId, false, null, 'ACTION_CANCELED');
        return;
      }
      isPlayingBack = true;
      var target = resolved.target;
      try {
        if (step.actionType === 'input' || step.actionType === 'select') {
          if ('value' in target && step.value !== undefined) target.value = step.value;
          dispatchNativeEvent(target, 'input');
          dispatchNativeEvent(target, 'change');
          sendPlaybackResult(requestId, true, target, undefined, resolved.selector);
          return;
        }
        if (step.actionType === 'click' || step.actionType === 'submit') {
          sendPlaybackResult(requestId, true, target, undefined, resolved.selector);
          target.click();
          return;
        }
        sendPlaybackResult(
          requestId,
          false,
          target,
          'ACTION_UNSUPPORTED',
          resolved.selector
        );
      } finally {
        isPlayingBack = false;
      }
    });
  }

  function playRecordedStep(requestId, step) {
    if (!config.playback || !step || !step.selector) {
      sendPlaybackResult(requestId, false, null, 'ACTION_UNSUPPORTED');
      return;
    }
    var expectedGeneration = routeGeneration;
    var expectedUrl = getCurrentBusinessUrl();
    var deadlineAt = Date.now() + 4500;
    resolveTargetEventually(step, function (resolved, error) {
      playResolvedStep(
        requestId,
        step,
        resolved,
        error,
        expectedGeneration,
        expectedUrl,
        deadlineAt
      );
    }, deadlineAt, expectedGeneration, expectedUrl);
  }

  function handleElementPick(event) {
    if (!elementPickActive) return false;
    var target = getPickedElement(event.target) || pickedHoverElement;
    event.preventDefault();
    event.stopImmediatePropagation();
    if (!target) return true;
    var selectorCandidates = getSelectorCandidates(target);
    var selector =
      selectorCandidates[0] || getUniqueCssPath(target) || target.tagName.toLowerCase();
    setElementPickActive(false, false);
    window.parent.postMessage(
      {
        type: 'ELEMENT_PICKED',
        actionType: 'click',
        url: getCurrentBusinessUrl(),
        pageTitle: document.title,
        selector: selector,
        selectorCandidates: selectorCandidates,
        text: String(getActionText(target, getActionKey(target)) || '')
          .replace(/\s+/g, ' ')
          .trim()
          .slice(0, 80),
        rect: getRect(target),
        recordedViewport: getViewport(),
        pageSnapshot: createPickerPageSnapshot()
      },
      config.parentOrigin
    );
    return true;
  }

  document.addEventListener('pointermove', function (event) {
    if (!elementPickActive) return;
    updatePickerOverlay(getPickedElement(event.target));
  }, true);
  document.addEventListener('keydown', function (event) {
    if (!elementPickActive || event.key !== 'Escape') return;
    event.preventDefault();
    setElementPickActive(false, true);
  }, true);
  document.addEventListener('click', function (event) {
    if (handleElementPick(event)) return;
    sendAction(event.target, 'click');
  }, true);
  document.addEventListener('change', function (event) {
    sendAction(event.target, 'change');
  });
  document.addEventListener('blur', function (event) {
    sendAction(event.target, 'blur');
  }, true);
  document.addEventListener('submit', function (event) {
    if (config.preventSubmit) event.preventDefault();
    sendAction(event.submitter || event.target, 'submit');
  }, true);

  window.addEventListener('message', function (event) {
    if (
      !event.data ||
      event.source !== window.parent ||
      !isAllowedParentOrigin(event.origin)
    ) return;
    if (event.data.type === 'PLAY_RECORDED_STEP') {
      playRecordedStep(event.data.requestId, event.data.step);
      return;
    }
    if (event.data.type === 'RESOLVE_RECORDED_TARGET') {
      resolveRecordedTarget(event.data.requestId, event.data.locator);
      return;
    }
    if (event.data.type === 'SET_RECORDING_STATE') {
      recordingEnabled = Boolean(event.data.enabled);
      captureSessionId = event.data.captureSessionId || captureSessionId;
      if (Number.isFinite(event.data.sequenceStart)) {
        sequenceNo = Math.max(sequenceNo, Number(event.data.sequenceStart));
      }
      return;
    }
    if (event.data.type === 'REQUEST_BUSINESS_READY') {
      sendReady();
      return;
    }
    if (
      event.data.type === 'START_ELEMENT_PICK' ||
      event.data.type === 'SXPT_START_ELEMENT_PICK'
    ) {
      setElementPickActive(true, false);
      return;
    }
    if (
      event.data.type === 'CANCEL_ELEMENT_PICK' ||
      event.data.type === 'SXPT_CANCEL_ELEMENT_PICK'
    ) {
      setElementPickActive(false, false);
    }
  });

  function createId(prefix) {
    var suffix = window.crypto && window.crypto.randomUUID
      ? window.crypto.randomUUID()
      : Date.now() + '-' + Math.random().toString(16).slice(2);
    return prefix + '-' + suffix;
  }

  wrapHistoryMethod('pushState');
  wrapHistoryMethod('replaceState');
  window.addEventListener('pagehide', sendNavigating);
  window.addEventListener('popstate', notifyRouteChange);
  window.addEventListener('hashchange', notifyRouteChange);

  sendReady();
  window.setTimeout(sendReady, 0);
  window.addEventListener('load', sendReady, { once: true });
})();
