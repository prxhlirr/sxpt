package com.sxpt.module.evaluation.assertion;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.execution.entity.ExecutionTrace;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * FIELD_EQUALS 评分断言策略。
 *
 * 业务功能：
 * 1. 从执行轨迹的脱敏摘要 JSON 中读取指定字段并与期望值比较。
 * 2. 支撑“字段值等于指定业务状态/结果”的过程评分，不要求额外调用原平台。
 *
 * 关键流程：
 * 1. 先按 traceType、taskStepId、resourceId、success 等可选条件过滤轨迹。
 * 2. 再从 input/output/before/after/evidence 中读取 field 指定的字段路径。
 * 3. 命中时只返回 traceId，不把实际字段值写入证据，避免敏感值二次扩散。
 */
@Component
public class FieldEqualsAssertionStrategy implements EvaluationAssertionStrategy {

    private static final String FIELD_EQUALS = "FIELD_EQUALS";

    private final ObjectMapper objectMapper;

    public FieldEqualsAssertionStrategy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean supports(String assertionType) {
        return FIELD_EQUALS.equals(assertionType);
    }

    @Override
    public AssertionDecision evaluate(EvaluationItem item, Map<String, Object> config, List<ExecutionTrace> traces) {
        String field = firstTextConfig(config, "field", "fieldPath", "path");
        String expected = firstTextConfig(config, "expected", "expectedValue");
        if (!StringUtils.hasText(field) || !StringUtils.hasText(expected)) {
            return AssertionDecision.notMatched("MISSING_FIELD_CONFIG");
        }
        for (ExecutionTrace trace : traces) {
            if (!matchesTraceScope(item, config, trace)) {
                continue;
            }
            String actual = fieldValue(trace, config, field);
            if (expected.equals(actual)) {
                return AssertionDecision.matchedTrace(trace.getId());
            }
        }
        return AssertionDecision.notMatched("FIELD_NOT_MATCHED");
    }

    /**
     * 判断轨迹是否满足字段断言的可选范围条件。
     *
     * @param item 评分项。
     * @param config 断言配置。
     * @param trace 执行轨迹。
     * @return 满足范围时返回 true。
     */
    private boolean matchesTraceScope(EvaluationItem item, Map<String, Object> config, ExecutionTrace trace) {
        String traceType = firstTextConfig(config, "traceType", "eventType");
        if (StringUtils.hasText(traceType) && !traceType.equals(trace.getTraceType())) {
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
     * 从轨迹指定 JSON 摘要中读取字段值。
     *
     * @param trace 执行轨迹。
     * @param config 断言配置。
     * @param field 字段路径。
     * @return 字段文本值。
     */
    private String fieldValue(ExecutionTrace trace, Map<String, Object> config, String field) {
        String json = sourceJson(trace, firstTextConfig(config, "source", "jsonSource"));
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            Map<String, Object> root = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            Object value = pathValue(root, field);
            return value == null ? null : String.valueOf(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 根据配置选择轨迹 JSON 摘要来源。
     *
     * @param trace 执行轨迹。
     * @param source 来源配置。
     * @return JSON 字符串。
     */
    private String sourceJson(ExecutionTrace trace, String source) {
        if ("input".equals(source) || "inputDataJson".equals(source)) {
            return trace.getInputDataJson();
        }
        if ("before".equals(source) || "beforeStateJson".equals(source)) {
            return trace.getBeforeStateJson();
        }
        if ("after".equals(source) || "afterStateJson".equals(source)) {
            return trace.getAfterStateJson();
        }
        if ("evidence".equals(source) || "evidenceJson".equals(source)) {
            return trace.getEvidenceJson();
        }
        return trace.getOutputDataJson();
    }

    /**
     * 按点号路径读取 Map 内字段。
     *
     * @param root JSON Map。
     * @param path 点号路径。
     * @return 字段值。
     */
    private Object pathValue(Map<String, Object> root, String path) {
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(part);
        }
        return current;
    }

    private String firstTextConfig(Map<String, Object> config, String... keys) {
        for (String key : keys) {
            String value = textConfig(config, key);
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
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
