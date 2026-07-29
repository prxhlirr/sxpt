import { apiRequest } from './http';
import type { EvaluationItem, EvaluationRule } from './contracts';

export const evaluationApi = {
  createRule(request: {
    tenantId: string;
    ruleCode: string;
    ruleName: string;
    taskId?: string;
    teachingPointId?: string;
    totalScore: number;
    description?: string;
    createBy?: string;
  }) {
    return apiRequest<EvaluationRule>({
      method: 'POST',
      url: '/evaluation/config/rules/create',
      data: request
    });
  },

  listRules(tenantId: string, taskId?: string, teachingPointId?: string) {
    return apiRequest<EvaluationRule[]>({
      method: 'GET',
      url: '/evaluation/config/rules',
      params: { tenantId, taskId, teachingPointId }
    });
  },

  createItem(request: {
    tenantId: string;
    evaluationRuleId: string;
    teachingPointId?: string;
    itemCode: string;
    itemName: string;
    itemType: string;
    relatedResourceId?: string;
    relatedApiResourceId?: string;
    relatedTaskStepId?: string;
    score: number;
    required: boolean;
    assertionType: string;
    assertionConfigJson: string;
    failPolicy: string;
    createBy?: string;
  }) {
    return apiRequest<EvaluationItem>({
      method: 'POST',
      url: '/evaluation/config/items/create',
      data: request
    });
  },

  listItems(tenantId: string, evaluationRuleId: string) {
    return apiRequest<EvaluationItem[]>({
      method: 'GET',
      url: '/evaluation/config/items',
      params: { tenantId, evaluationRuleId }
    });
  }
};
