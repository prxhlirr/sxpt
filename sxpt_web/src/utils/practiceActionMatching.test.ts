import { describe, expect, it } from 'vitest';
import type { RecordedStep } from '../domain/models';
import {
  actionMatchesRecordedStep,
  businessPathsMatch,
  practiceActionMatchPriority
} from './practiceActionMatching';

function step(overrides: Partial<RecordedStep> = {}): RecordedStep {
  return {
    id: 'step-1',
    title: '提交收文登记',
    pageTitle: '收文登记',
    actionLabel: '提交',
    selector: 'button.el-button:nth-of-type(2)',
    selectorCandidates: ['button.el-button:nth-of-type(2)'],
    durationSeconds: 8,
    note: '',
    kind: 'action',
    actionType: 'click',
    url: '/workspace/incoming/detail/1785916779495',
    ...overrides
  };
}

describe('practice action matching', () => {
  it('treats teacher and student OA data IDs as the same business route', () => {
    expect(
      businessPathsMatch(
        '/workspace/incoming/detail/1785916779495?mode=record',
        '/workspace/incoming/detail/1785922999001?mode=practice',
        'http://127.0.0.1:3000/'
      )
    ).toBe(true);
    expect(
      businessPathsMatch(
        '/workspace/incoming/detail/3428b543-751e-40cf-b198-021593d532a8',
        '/workspace/incoming/detail/6d5fbdb8-2ea4-4e68-84c5-b4703fd06e5d',
        'http://127.0.0.1:3000/'
      )
    ).toBe(true);
    expect(
      actionMatchesRecordedStep(
        {
          actionType: 'click',
          selector: 'button.el-button:nth-of-type(2)',
          url: '/workspace/incoming/detail/1785922999001'
        },
        step(),
        'http://127.0.0.1:3000/'
      )
    ).toBe(true);
  });

  it('accepts an actionable button inside an element-picked business card', () => {
    expect(
      actionMatchesRecordedStep(
        {
          actionType: 'click',
          selector:
            'main > div > div:nth-of-type(1) > div:nth-of-type(3) > div:nth-of-type(1) > div:nth-of-type(2) > button',
          url: '/workspace/incoming?status=pending_reg'
        },
        step({
          selector:
            'main > div > div:nth-of-type(1) > div:nth-of-type(3) > div:nth-of-type(1)',
          selectorCandidates: [],
          url: '/workspace/incoming?status=pending_reg'
        }),
        'http://127.0.0.1:3000/'
      )
    ).toBe(true);
  });

  it('uses the recorded card text when the runtime button selector is shorter', () => {
    expect(
      actionMatchesRecordedStep(
        {
          actionType: 'click',
          selector:
            'div:nth-of-type(3) > div:nth-of-type(1) > div:nth-of-type(2) > button',
          text: '登记',
          url: '/workspace/incoming?status=pending_reg'
        },
        step({
          actionLabel:
            '关于开展2026年度重点工作督查的通知 普通 来文单位：市数据局 登记',
          selector:
            'main > div > div:nth-of-type(1) > div:nth-of-type(3) > div:nth-of-type(1)',
          selectorCandidates: [],
          url: '/workspace/incoming?status=pending_reg'
        }),
        'http://127.0.0.1:3000/'
      )
    ).toBe(true);
  });

  it('still rejects a click from a different OA module page', () => {
    expect(
      actionMatchesRecordedStep(
        {
          actionType: 'click',
          selector: 'button.el-button:nth-of-type(2)',
          url: '/workspace/outgoing/detail/1785922999001'
        },
        step(),
        'http://127.0.0.1:3000/'
      )
    ).toBe(false);
  });

  it('matches any recorded selector candidate and requires the action type', () => {
    const recorded = step({
      selector: '#legacy-submit',
      selectorCandidates: ['[data-action="submit"]'],
      actionType: 'submit'
    });
    expect(
      actionMatchesRecordedStep(
        {
          actionType: 'click',
          selector: '[data-action="submit"]',
          url: '/workspace/incoming/detail/1785922999001'
        },
        recorded,
        'http://127.0.0.1:3000/'
      )
    ).toBe(true);
    expect(
      actionMatchesRecordedStep(
        {
          actionType: 'input',
          selector: '[data-action="submit"]',
          url: '/workspace/incoming/detail/1785922999001'
        },
        recorded,
        'http://127.0.0.1:3000/'
      )
    ).toBe(false);
  });

  it('prefers the exact mandatory button over a broad optional page node', () => {
    const payload = {
      actionType: 'click' as const,
      selector:
        'div:nth-of-type(3) > div:nth-of-type(1) > div:nth-of-type(2) > button',
      selectorCandidates: ['main'],
      text: '登记',
      url: '/workspace/incoming?status=pending_reg'
    };
    const optionalPage = step({
      selector: 'main',
      selectorCandidates: ['main'],
      title: '列表展示区域',
      actionLabel: '查看列表说明',
      required: false,
      kind: 'guide',
      actionType: 'guide',
      url: '/workspace/incoming?status=pending_reg'
    });
    const mandatoryButton = step({
      selector: payload.selector,
      selectorCandidates: [payload.selector],
      title: '登记',
      actionLabel: '登记',
      required: true,
      kind: 'guide',
      actionType: 'guide',
      url: '/workspace/incoming?status=pending_reg'
    });

    expect(
      practiceActionMatchPriority(
        payload,
        mandatoryButton,
        'http://127.0.0.1:3000/'
      )
    ).toBeGreaterThan(
      practiceActionMatchPriority(
        payload,
        optionalPage,
        'http://127.0.0.1:3000/'
      )
    );
  });
});
