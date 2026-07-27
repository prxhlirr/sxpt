import type {
  CompletionMethod,
  RecordedActionType
} from '../types/domain';

export const completionMethodOptions: Array<{
  value: CompletionMethod;
  label: string;
}> = [
  { value: 'click', label: '点击完成' },
  { value: 'business_check', label: '业务校验' },
  { value: 'submission', label: '成果提交' },
  { value: 'manual', label: '人工确认' },
  { value: 'mixed', label: '组合判定' }
];

const actionTypeLabels: Record<RecordedActionType, string> = {
  click: '点击',
  input: '填写',
  select: '选择',
  submit: '提交'
};

const completionMethodLabels: Record<CompletionMethod, string> = {
  click: '点击完成',
  business_check: '业务校验',
  submission: '成果提交',
  manual: '人工确认',
  mixed: '组合判定'
};

const nodeCodeLabels: Record<string, string> = {
  PURCHASE_CREATE: '采购创建',
  PURCHASE_FILL: '采购填写',
  PURCHASE_SUBMIT: '采购提交',
  PURCHASE_APPROVE: '采购审批',
  PURCHASE_RESULT: '采购结果'
};

const nodeTypeLabels: Record<string, string> = {
  create: '创建类',
  update: '填写类',
  submit: '提交类',
  approve: '审批类',
  export: '结果类'
};

export function actionTypeLabel(actionType: RecordedActionType): string {
  return actionTypeLabels[actionType];
}

export function completionMethodLabel(method: CompletionMethod): string {
  return completionMethodLabels[method];
}

export function nodeCodeLabel(code: string): string {
  return nodeCodeLabels[code] ?? code;
}

export function nodeTypeLabel(type: string): string {
  return nodeTypeLabels[type] ?? type;
}

export function versionLabel(version: string): string {
  return version.replace(/^v/i, '版本 ');
}
