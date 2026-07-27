import { describe, expect, it } from 'vitest';
import {
  actionTypeLabel,
  completionMethodLabel,
  completionMethodOptions,
  nodeCodeLabel,
  nodeTypeLabel,
  versionLabel
} from './displayLabels';

describe('displayLabels', () => {
  it('labels recorded action types in Chinese', () => {
    expect(actionTypeLabel('click')).toBe('点击');
    expect(actionTypeLabel('input')).toBe('填写');
    expect(actionTypeLabel('select')).toBe('选择');
    expect(actionTypeLabel('submit')).toBe('提交');
  });

  it('labels completion methods in Chinese', () => {
    expect(completionMethodLabel('click')).toBe('点击完成');
    expect(completionMethodLabel('business_check')).toBe('业务校验');
    expect(completionMethodLabel('submission')).toBe('成果提交');
    expect(completionMethodLabel('manual')).toBe('人工确认');
    expect(completionMethodLabel('mixed')).toBe('组合判定');
  });

  it('keeps internal completion values while exposing Chinese option labels', () => {
    expect(completionMethodOptions[0]).toEqual({
      value: 'click',
      label: '点击完成'
    });
  });

  it('labels node codes and node types in Chinese', () => {
    expect(nodeCodeLabel('PURCHASE_CREATE')).toBe('采购创建');
    expect(nodeTypeLabel('submit')).toBe('提交类');
  });

  it('labels template versions in Chinese', () => {
    expect(versionLabel('v1.0')).toBe('版本 1.0');
  });
});
