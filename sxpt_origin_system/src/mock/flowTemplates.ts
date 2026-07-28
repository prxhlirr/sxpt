import type { FlowTemplate } from '../types/domain';

export const purchaseFlowTemplate: FlowTemplate = {
  id: 'template-purchase',
  moduleName: '采购申请',
  title: '采购申请标准流程',
  version: 'v1.0',
  status: 'published',
  nodes: [
    {
      id: 'tpl-node-create',
      nodeDefId: 'def-create',
      name: '创建申请',
      required: true,
      score: 20,
      completionMethod: 'click'
    },
    {
      id: 'tpl-node-fill',
      nodeDefId: 'def-fill',
      name: '填写基础信息',
      required: true,
      score: 25,
      completionMethod: 'click'
    },
    {
      id: 'tpl-node-submit',
      nodeDefId: 'def-submit',
      name: '提交申请',
      required: true,
      score: 25,
      completionMethod: 'click'
    },
    {
      id: 'tpl-node-approve',
      nodeDefId: 'def-approve',
      name: '审批通过',
      required: true,
      score: 20,
      completionMethod: 'business_check'
    },
    {
      id: 'tpl-node-result',
      nodeDefId: 'def-result',
      name: '查看结果',
      required: false,
      score: 10,
      completionMethod: 'submission'
    }
  ],
  edges: [
    { id: 'tpl-edge-1', from: 'tpl-node-create', to: 'tpl-node-fill' },
    { id: 'tpl-edge-2', from: 'tpl-node-fill', to: 'tpl-node-submit' },
    { id: 'tpl-edge-3', from: 'tpl-node-submit', to: 'tpl-node-approve' },
    { id: 'tpl-edge-4', from: 'tpl-node-approve', to: 'tpl-node-result' }
  ]
};
