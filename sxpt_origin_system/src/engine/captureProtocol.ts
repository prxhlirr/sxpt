import type {
  BusinessActionPayload,
  RecordedViewport,
  Rect
} from '../types/domain';
import type { ReportCaptureEventRequest } from '../types/trainingApi';

export interface CaptureEventContext {
  tenantId: string;
  captureSessionId: string;
  sequenceNo?: number;
}

export interface BusinessReadyMessage {
  type: 'BUSINESS_READY';
  url: string;
  title: string;
  sdkSessionId: string;
  timestamp: string;
}

export interface BusinessNavigatingMessage {
  type: 'BUSINESS_NAVIGATING';
  url: string;
  timestamp: string;
}

export type PlaybackErrorCode =
  | 'ELEMENT_NOT_FOUND'
  | 'ACTION_UNSUPPORTED'
  | 'ACTION_CANCELED';

export interface PlaybackResultMessage {
  type: 'PLAYBACK_RESULT';
  requestId: string;
  success: boolean;
  url: string;
  rect?: Rect;
  error?: PlaybackErrorCode;
}

export interface TargetResolutionResultMessage {
  type: 'TARGET_RESOLUTION_RESULT';
  requestId: string;
  success: boolean;
  url: string;
  selector?: string;
  rect?: Rect;
  viewport?: RecordedViewport;
  error?: PlaybackErrorCode;
}

export interface ElementPickedMessage {
  type: 'ELEMENT_PICKED';
  url: string;
  selector: string;
  selectorCandidates?: string[];
  text: string;
  rect: Rect;
  recordedViewport?: RecordedViewport;
}

export function toCaptureEventRequest(
  action: BusinessActionPayload,
  context: CaptureEventContext
): ReportCaptureEventRequest {
  return {
    tenantId: context.tenantId,
    captureSessionId: context.captureSessionId,
    sdkSessionId: action.sdkSessionId,
    clientEventId: action.clientEventId ?? createProtocolId('client-event'),
    eventType: action.actionType.toUpperCase(),
    eventTime: action.timestamp,
    sequenceNo: action.sequenceNo ?? context.sequenceNo ?? 1,
    retryCount: 0,
    pageUrl: action.url,
    targetText: action.text,
    targetLocator: action.selector,
    targetStableKey: action.stableKey,
    inputValueMasked:
      action.inputValueMasked ??
      (action.value === undefined ? undefined : maskInputValue(action.value)),
    eventPayloadJson: JSON.stringify({
      actionType: action.actionType,
      rect: action.rect,
      selectorCandidates: action.selectorCandidates,
      recordedViewport: action.recordedViewport
    })
  };
}

export function maskInputValue(value: string): string {
  if (!value) return '';
  if (value.length <= 2) return '*'.repeat(value.length);
  if (value.length <= 4) {
    return `${value[0]}${'*'.repeat(value.length - 2)}${value.at(-1)}`;
  }
  return `${value.slice(0, 2)}${'*'.repeat(value.length - 4)}${value.slice(-2)}`;
}

export function isBusinessReadyMessage(
  value: unknown
): value is BusinessReadyMessage {
  const message = asRecord(value);
  return (
    message?.type === 'BUSINESS_READY' &&
    typeof message.url === 'string' &&
    typeof message.title === 'string' &&
    typeof message.sdkSessionId === 'string'
  );
}

export function isBusinessNavigatingMessage(
  value: unknown
): value is BusinessNavigatingMessage {
  const message = asRecord(value);
  return (
    message?.type === 'BUSINESS_NAVIGATING' &&
    typeof message.url === 'string' &&
    typeof message.timestamp === 'string'
  );
}

export function isPlaybackResultMessage(
  value: unknown
): value is PlaybackResultMessage {
  const message = asRecord(value);
  return (
    message?.type === 'PLAYBACK_RESULT' &&
    typeof message.requestId === 'string' &&
    typeof message.success === 'boolean' &&
    typeof message.url === 'string' &&
    isPlaybackError(message.error)
  );
}

export function isTargetResolutionResultMessage(
  value: unknown
): value is TargetResolutionResultMessage {
  const message = asRecord(value);
  if (
    message?.type !== 'TARGET_RESOLUTION_RESULT' ||
    typeof message.requestId !== 'string' ||
    typeof message.success !== 'boolean' ||
    typeof message.url !== 'string'
  ) {
    return false;
  }

  if (!isPlaybackError(message.error)) return false;
  if (!message.success) return true;
  return (
    typeof message.selector === 'string' &&
    isRect(message.rect) &&
    isRecordedViewport(message.viewport)
  );
}

export function isElementPickedMessage(
  value: unknown
): value is ElementPickedMessage {
  const message = asRecord(value);
  return (
    message?.type === 'ELEMENT_PICKED' &&
    typeof message.url === 'string' &&
    typeof message.selector === 'string' &&
    typeof message.text === 'string' &&
    isRect(message.rect)
  );
}

function asRecord(value: unknown): Record<string, unknown> | undefined {
  return value && typeof value === 'object'
    ? (value as Record<string, unknown>)
    : undefined;
}

function isRect(value: unknown): value is Rect {
  const rect = asRecord(value);
  return Boolean(
    rect &&
      typeof rect.x === 'number' &&
      typeof rect.y === 'number' &&
      typeof rect.width === 'number' &&
      typeof rect.height === 'number'
  );
}

function isRecordedViewport(value: unknown): value is RecordedViewport {
  const viewport = asRecord(value);
  return Boolean(
    viewport &&
      typeof viewport.width === 'number' &&
      typeof viewport.height === 'number' &&
      typeof viewport.devicePixelRatio === 'number'
  );
}

function isPlaybackError(value: unknown): value is PlaybackErrorCode | undefined {
  return (
    value === undefined ||
    value === 'ELEMENT_NOT_FOUND' ||
    value === 'ACTION_UNSUPPORTED' ||
    value === 'ACTION_CANCELED'
  );
}

function createProtocolId(prefix: string): string {
  const suffix =
    typeof crypto !== 'undefined' && crypto.randomUUID
      ? crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
  return `${prefix}-${suffix}`;
}
