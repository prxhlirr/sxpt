import type { NodeDef } from '../types/domain';

export const nodeDefs: NodeDef[] = [
  {
    id: 'def-create',
    code: 'PURCHASE_CREATE',
    name: '创建申请',
    type: 'create',
    defaultScore: 20,
    defaultCompletionMethod: 'click',
    needBusinessCheck: false,
    enabled: true,
    category: '采购申请'
  },
  {
    id: 'def-fill',
    code: 'PURCHASE_FILL',
    name: '填写基础信息',
    type: 'update',
    defaultScore: 25,
    defaultCompletionMethod: 'click',
    needBusinessCheck: false,
    enabled: true,
    category: '采购申请'
  },
  {
    id: 'def-submit',
    code: 'PURCHASE_SUBMIT',
    name: '提交申请',
    type: 'submit',
    defaultScore: 25,
    defaultCompletionMethod: 'click',
    needBusinessCheck: false,
    enabled: true,
    category: '采购申请'
  },
  {
    id: 'def-approve',
    code: 'PURCHASE_APPROVE',
    name: '审批通过',
    type: 'approve',
    defaultScore: 20,
    defaultCompletionMethod: 'business_check',
    needBusinessCheck: true,
    enabled: true,
    category: '采购申请'
  },
  {
    id: 'def-result',
    code: 'PURCHASE_RESULT',
    name: '查看结果',
    type: 'export',
    defaultScore: 10,
    defaultCompletionMethod: 'submission',
    needBusinessCheck: true,
    enabled: true,
    category: '采购申请'
  }
];
