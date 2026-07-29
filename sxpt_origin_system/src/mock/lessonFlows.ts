import type { LessonFlow, RecordedFlow } from '../types/domain';

export const purchaseLessonFlow: LessonFlow = {
  id: 'lesson-purchase',
  templateId: 'template-purchase',
  title: '采购申请实训教案',
  moduleName: '采购申请',
  publishStatus: 'published',
  modePolicies: {
    learning: {
      showFullFlow: true,
      showTeachingText: true,
      showOverlayHint: true,
      allowFreeBrowse: true,
      enforceOrder: false,
      showSubmission: false,
      silentRecord: false
    },
    practice: {
      showFullFlow: true,
      showTeachingText: false,
      showOverlayHint: true,
      allowFreeBrowse: false,
      enforceOrder: true,
      showSubmission: false,
      silentRecord: false
    },
    exam: {
      showFullFlow: false,
      showTeachingText: false,
      showOverlayHint: false,
      allowFreeBrowse: false,
      enforceOrder: false,
      showSubmission: true,
      silentRecord: true
    }
  },
  nodes: [
    {
      id: 'node-create',
      nodeDefId: 'def-create',
      name: '创建申请',
      required: true,
      score: 20,
      completionMethod: 'click',
      teachingText: '在业务系统中新建一张采购申请单。',
      binding: {
        url: '/business-sdk-demo.html',
        selector: '[data-action="create"]',
        rect: { x: 84, y: 112, width: 132, height: 44 },
        actionText: '新建申请'
      }
    },
    {
      id: 'node-fill',
      nodeDefId: 'def-fill',
      name: '填写基础信息',
      required: true,
      score: 25,
      completionMethod: 'click',
      teachingText: '填写供应商、申请金额、采购品类和申请原因。',
      binding: {
        url: '/business-sdk-demo.html',
        selector: '[data-action="fill"]',
        rect: { x: 72, y: 202, width: 560, height: 184 },
        actionText: '申请信息表单'
      }
    },
    {
      id: 'node-submit',
      nodeDefId: 'def-submit',
      name: '提交申请',
      required: true,
      score: 25,
      completionMethod: 'click',
      teachingText: '确认表单内容无误后提交采购申请。',
      binding: {
        url: '/business-sdk-demo.html',
        selector: '[data-action="submit"]',
        rect: { x: 500, y: 420, width: 132, height: 44 },
        actionText: '提交申请'
      }
    },
    {
      id: 'node-approve',
      nodeDefId: 'def-approve',
      name: '审批通过',
      required: true,
      score: 20,
      completionMethod: 'business_check',
      teachingText: '切换审批视角，完成采购申请审批。',
      binding: {
        url: '/business-sdk-demo.html',
        selector: '[data-action="approve"]',
        rect: { x: 356, y: 420, width: 132, height: 44 },
        actionText: '审批通过'
      }
    },
    {
      id: 'node-result',
      nodeDefId: 'def-result',
      name: '查看结果',
      required: false,
      score: 10,
      completionMethod: 'submission',
      teachingText: '查看业务单据状态，并在考试模式提交业务单号。',
      binding: {
        url: '/business-sdk-demo.html',
        selector: '[data-action="result"]',
        rect: { x: 696, y: 176, width: 220, height: 128 },
        actionText: '结果面板'
      }
    }
  ],
  edges: [
    { id: 'edge-1', from: 'node-create', to: 'node-fill' },
    { id: 'edge-2', from: 'node-fill', to: 'node-submit' },
    { id: 'edge-3', from: 'node-submit', to: 'node-approve' },
    { id: 'edge-4', from: 'node-approve', to: 'node-result' }
  ]
};

export function createEmptyRecordedFlow(): RecordedFlow {
  const suffix = createFlowId();
  return {
    id: `recorded-flow-${suffix}`,
    title: '采购申请录制流程',
    businessUrl: '/business-sdk-demo.html',
    source: 'custom',
    publishStatus: 'draft',
    steps: [],
    segments: [
      {
        id: `segment-${suffix}`,
        segmentNo: 1,
        title: '备案第 1 段',
        targetUrl: '/business-sdk-demo.html',
        status: 'recording',
        steps: []
      }
    ]
  };
}

function createFlowId(): string {
  return typeof crypto !== 'undefined' && crypto.randomUUID
    ? crypto.randomUUID()
    : `${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

export const purchaseRecordedFlow: RecordedFlow = {
  id: 'recorded-purchase-flow',
  title: '采购申请录制流程',
  businessUrl: '/business-sdk-demo.html',
  source: 'template',
  publishStatus: 'published',
  steps: [
    {
      id: 'step-1',
      nodeId: 'record-node-create',
      order: 1,
      title: '新建申请',
      actionType: 'click',
      selector: '[data-action="create"]',
      rect: { x: 84, y: 112, width: 132, height: 44 },
      text: '新建申请',
      url: '/business-sdk-demo.html',
      teachingText: '先点击新建申请，进入采购申请的创建状态。',
      practiceHint: '从页面顶部工具栏找到新建申请按钮。',
      examGoal: '独立创建一张采购申请。',
      completionMethod: 'click'
    },
    {
      id: 'step-2',
      nodeId: 'record-node-supplier',
      order: 2,
      title: '填写供应商',
      actionType: 'input',
      selector: '[data-action="supplier"]',
      rect: { x: 250, y: 224, width: 260, height: 40 },
      text: '供应商',
      value: '上海示例供应商有限公司',
      url: '/business-sdk-demo.html',
      teachingText: '在供应商字段中录入本次采购对应的供应商名称。',
      practiceHint: '填写供应商名称。',
      examGoal: '正确填写供应商信息。',
      completionMethod: 'click'
    },
    {
      id: 'step-3',
      nodeId: 'record-node-amount',
      order: 3,
      title: '填写申请金额',
      actionType: 'input',
      selector: '[data-action="amount"]',
      rect: { x: 250, y: 316, width: 260, height: 40 },
      text: '申请金额',
      value: '12800',
      url: '/business-sdk-demo.html',
      teachingText: '填写申请金额，金额用于后续审批判断。',
      practiceHint: '填写本次采购金额。',
      examGoal: '正确填写采购申请金额。',
      completionMethod: 'click'
    },
    {
      id: 'step-4',
      nodeId: 'record-node-submit',
      order: 4,
      title: '提交申请',
      actionType: 'submit',
      selector: '[data-action="submit"]',
      rect: { x: 500, y: 420, width: 132, height: 44 },
      text: '提交申请',
      url: '/business-sdk-demo.html',
      teachingText: '确认信息无误后提交采购申请，生成业务单号。',
      practiceHint: '检查表单后提交申请。',
      examGoal: '独立提交采购申请并保留业务单号。',
      completionMethod: 'submission'
    }
  ]
};
