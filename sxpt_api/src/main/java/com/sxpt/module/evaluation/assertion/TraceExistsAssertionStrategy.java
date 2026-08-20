package com.sxpt.module.evaluation.assertion;

import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.execution.entity.ExecutionTrace;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * TRACE_EXISTS 评分断言策略。
 *
 * 业务功能：
 * 1. 判断学生执行轨迹中是否存在满足评分项要求的操作事实。
 * 2. 支撑点击、输入、选择、上传、状态确认等基于 traceType 的基础评分。
 *
 * 关键流程：
 * 1. 从断言配置读取 traceType/eventType、taskStepId、resourceId 和 success。
 * 2. 遍历执行轨迹，命中第一条符合条件的轨迹即返回 matchedTrace。
 */
@Component
public class TraceExistsAssertionStrategy implements EvaluationAssertionStrategy {

    private static final String TRACE_EXISTS = "TRACE_EXISTS";

    private static final String NO_SCORE_POLICY = "NO_SCORE";

    private static final String STEP_COMPLETED = "STEP_COMPLETED";

    private static final String LEGACY_PRACTICE_COMPLETION_SUFFIX = ":practice-completed";

    @Override
    public boolean supports(String assertionType) {
        return TRACE_EXISTS.equals(assertionType);
    }

    @Override
    public AssertionDecision evaluate(EvaluationItem item, Map<String, Object> config, List<ExecutionTrace> traces) {
        for (ExecutionTrace trace : traces) {
            if (matchesTrace(item, config, trace)) {
                return AssertionDecision.matchedTrace(trace.getId());
            }
        }
        return AssertionDecision.notMatched(NO_SCORE_POLICY.equals(item.getFailPolicy()) ? "NO_TRACE" : "NOT_MATCHED");
    }

    /**
     * 判断执行轨迹是否命中 TRACE_EXISTS 评分项。
     *
     * @param item 评分项。
     * @param config 断言配置。
     * @param trace 执行轨迹。
     * @return 命中时返回 true。
     */
    private boolean matchesTrace(EvaluationItem item, Map<String, Object> config, ExecutionTrace trace) {
        String traceType = textConfig(config, "traceType");
        if (!StringUtils.hasText(traceType)) {
            traceType = textConfig(config, "eventType");
        }
        if (StringUtils.hasText(traceType) && !matchesTraceType(traceType, trace)) {
            return false;
        }
        String taskStepId = StringUtils.hasText(item.getRelatedTaskStepId())
                ? item.getRelatedTaskStepId() : textConfig(config, "taskStepId");
        if (StringUtils.hasText(taskStepId) && !taskStepId.equals(trace.getTaskStepId())) {
            return false;
        }
        String resourceId = StringUtils.hasText(item.getRelatedResourceId())
                ? item.getRelatedResourceId() : textConfig(config, "resourceId");
        if (StringUtils.hasText(resourceId) && !resourceId.equals(trace.getResourceId())) {
            return false;
        }
        Boolean success = booleanConfig(config, "success");
        return success == null || success.equals(trace.getSuccess());
    }

    /**
     * 兼容本次修复前已经入库的练习完成轨迹。
     *
     * 旧前端使用 CLICK/INPUT/SELECT 作为轨迹类型，但 clientTraceId 已明确标记
     * practice-completed。兼容范围只覆盖该稳定幂等键，普通点击轨迹不能冒充步骤完成。
     */
    private boolean matchesTraceType(String expectedTraceType, ExecutionTrace trace) {
        if (expectedTraceType.equals(trace.getTraceType())) {
            return true;
        }
        if (!STEP_COMPLETED.equals(expectedTraceType)
                || !StringUtils.hasText(trace.getClientTraceId())
                || !trace.getClientTraceId().endsWith(LEGACY_PRACTICE_COMPLETION_SUFFIX)) {
            return false;
        }
        return "CLICK".equals(trace.getTraceType())
                || "INPUT".equals(trace.getTraceType())
                || "SELECT".equals(trace.getTraceType());
    }

    private String textConfig(Map<String, Object> config, String key) {
        Object value = config.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Boolean booleanConfig(Map<String, Object> config, String key) {
        Object value = config.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.valueOf((String) value);
        }
        return null;
    }
}
