package com.sxpt.module.evaluation.service;

import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.evaluation.entity.EvaluationRule;

import java.util.List;

/**
 * 评分配置服务。
 *
 * 业务功能：
 * 1. 创建评价规则和评分项，支撑 MVP 自动评分的配置准备。
 * 2. 支持按任务或教学点查询规则、按规则查询评分项。
 *
 * 关键流程：
 * 1. 创建规则时校验规则编码、名称和总分。
 * 2. 创建评分项时校验断言配置、失败策略和分值。
 */
public interface EvaluationConfigService {

    /**
     * 创建评价规则。
     *
     * @param evaluationRule 评价规则实体。
     * @return 已保存的评价规则。
     */
    EvaluationRule createEvaluationRule(EvaluationRule evaluationRule);

    /**
     * 查询评价规则。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @return 评价规则列表。
     */
    List<EvaluationRule> listRules(String tenantId, String taskId, String teachingPointId);

    /**
     * 创建评分项。
     *
     * @param evaluationItem 评分项实体。
     * @return 已保存的评分项。
     */
    EvaluationItem createEvaluationItem(EvaluationItem evaluationItem);

    /**
     * 按评价规则查询评分项。
     *
     * @param tenantId 租户 ID。
     * @param evaluationRuleId 评价规则 ID。
     * @return 评分项列表。
     */
    List<EvaluationItem> listItemsByRule(String tenantId, String evaluationRuleId);
}
