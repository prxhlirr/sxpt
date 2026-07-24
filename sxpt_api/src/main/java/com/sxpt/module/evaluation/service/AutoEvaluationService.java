package com.sxpt.module.evaluation.service;

import com.sxpt.module.evaluation.entity.EvaluationResult;

/**
 * 自动评分服务。
 *
 * 业务功能：
 * 1. 根据评价规则下的评分项和学生执行轨迹生成自动评分。
 * 2. 支持教师端查询一次执行在某规则下的评分结果。
 *
 * 关键流程：
 * 1. 自动评分读取 evaluation_item 与 execution_trace。
 * 2. 按评分项断言命中情况累加自动分，并保存 evidenceJson 供教师复核。
 */
public interface AutoEvaluationService {

    /**
     * 生成或重算自动评分结果。
     *
     * @param result 评分结果范围。
     * @return 已保存的评分结果。
     */
    EvaluationResult generateAutoEvaluation(EvaluationResult result);

    /**
     * 查询自动评分结果。
     *
     * @param tenantId 租户 ID。
     * @param executionId 执行 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @return 评分结果。
     */
    EvaluationResult getEvaluationResult(String tenantId, String executionId, String evaluationRuleId);

    /**
     * 提交教师复核结果。
     *
     * @param result 复核范围和人工分。
     * @return 已更新的评分结果。
     */
    EvaluationResult reviewEvaluationResult(EvaluationResult result);
}
