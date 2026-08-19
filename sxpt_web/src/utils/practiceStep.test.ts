import { describe, expect, it } from 'vitest';
import type { RecordedStep } from '../domain/models';
import {
  isPracticeMonitorableStep,
  isRequiredPracticeStep,
  practiceRecordedActionType
} from './practiceStep';

const step = (patch: Partial<RecordedStep>): RecordedStep => ({
  id: 'step-1',
  title: '操作点',
  pageTitle: '业务页面',
  actionLabel: '操作',
  selector: '',
  durationSeconds: 1,
  note: '',
  ...patch
});

describe('practice step semantics', () => {
  it('treats an element-selection guide with a selector as a click monitor point', () => {
    const recorded = step({
      kind: 'guide',
      actionType: 'guide',
      selector: '[data-action="query"]'
    });

    expect(isPracticeMonitorableStep(recorded)).toBe(true);
    expect(practiceRecordedActionType(recorded)).toBe('click');
  });

  it('keeps pure text guides out of practice scoring', () => {
    expect(
      isPracticeMonitorableStep(
        step({ kind: 'guide', actionType: 'guide', selector: '' })
      )
    ).toBe(false);
  });

  it('keeps recorded action nodes and their action types unchanged', () => {
    const recorded = step({ actionType: 'input', selector: '#subject' });

    expect(isPracticeMonitorableStep(recorded)).toBe(true);
    expect(isRequiredPracticeStep(recorded)).toBe(true);
    expect(practiceRecordedActionType(recorded)).toBe('input');
  });

  it('does not let an explicitly optional operation block practice completion', () => {
    const optional = step({
      actionType: 'click',
      selector: '[data-action="help"]',
      required: false
    });

    expect(isPracticeMonitorableStep(optional)).toBe(true);
    expect(isRequiredPracticeStep(optional)).toBe(false);
  });
});
