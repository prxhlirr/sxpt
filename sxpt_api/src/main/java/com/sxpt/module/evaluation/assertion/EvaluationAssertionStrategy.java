package com.sxpt.module.evaluation.assertion;

import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.execution.entity.ExecutionTrace;

import java.util.List;
import java.util.Map;

/**
 * 评分断言策略接口。
 *
 * 业务功能：
 * 1. 为不同 assertionType 提供统一扩展点。
 * 2. 让自动评分主流程只关心“选择策略、累计分数、写证据”，不关心断言细节。
 *
 * 关键流程：
 * 1. supports 判断当前策略是否处理指定 assertionType。
 * 2. evaluate 基于评分项、解析后的断言配置和执行轨迹返回统一判定结果。
 */
public interface EvaluationAssertionStrategy {

    /**
     * 判断策略是否支持指定断言类型。
     *
     * @param assertionType 评分项断言类型。
     * @return 支持时返回 true。
     */
    boolean supports(String assertionType);

    /**
     * 执行断言判定。
     *
     * @param item 评分项。
     * @param config 解析后的断言配置。
     * @param traces 执行轨迹列表。
     * @return 判定结果。
     */
    AssertionDecision evaluate(EvaluationItem item, Map<String, Object> config, List<ExecutionTrace> traces);
}
