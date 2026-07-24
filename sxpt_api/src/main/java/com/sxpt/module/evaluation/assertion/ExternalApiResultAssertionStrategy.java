package com.sxpt.module.evaluation.assertion;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.module.connector.entity.ConnectorResource;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.mapper.ConnectorResourceMapper;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import com.sxpt.module.execution.entity.ExecutionTrace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * EXTERNAL_API_RESULT 评分断言策略。
 *
 * 业务功能：
 * 1. 基于原平台结果配置判断业务结果是否符合预期。
 * 2. MVP 阶段先复用 assertionConfigJson 中的 actual/expected 字段，不在策略内直接调用外部系统。
 *
 * 关键流程：
 * 1. 校验评分项已关联 relatedApiResourceId。
 * 2. 从配置读取 expectedStatus/expectedResult/expected 与 actualStatus/actualResult/actual。
 * 3. 两者相等时返回 matchedExternal，否则返回稳定未命中原因。
 */
@Component
public class ExternalApiResultAssertionStrategy implements EvaluationAssertionStrategy {

    private static final String EXTERNAL_API_RESULT = "EXTERNAL_API_RESULT";

    private static final String GET_METHOD = "GET";

    private final ConnectorResourceMapper connectorResourceMapper;

    private final ConnectorSystemMapper connectorSystemMapper;

    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate;

    public ExternalApiResultAssertionStrategy() {
        this(null, null, new ObjectMapper(), new RestTemplate());
    }

    /**
     * 构建外部 API 结果断言策略。
     *
     * 业务功能：在自动评分时，根据评分项关联的正式 API 资源读取原平台配置并查询真实业务状态。
     * 关键流程：优先兼容断言配置中的 actual 值；缺少 actual 时，读取 connector_resource.metadataJson
     * 与 connector_system.baseUrl 拼装 GET 地址，再抽取响应字段与 expected 比较。
     *
     * @param connectorResourceMapperProvider 正式资源 Mapper 提供器，test profile 下允许不存在。
     * @param connectorSystemMapperProvider 原平台配置 Mapper 提供器，test profile 下允许不存在。
     * @param objectMapper JSON 解析器。
     */
    @Autowired
    public ExternalApiResultAssertionStrategy(ObjectProvider<ConnectorResourceMapper> connectorResourceMapperProvider,
                                              ObjectProvider<ConnectorSystemMapper> connectorSystemMapperProvider,
                                              ObjectMapper objectMapper) {
        this(connectorResourceMapperProvider.getIfAvailable(),
                connectorSystemMapperProvider.getIfAvailable(),
                objectMapper,
                new RestTemplate());
    }

    public ExternalApiResultAssertionStrategy(ConnectorResourceMapper connectorResourceMapper,
                                              ConnectorSystemMapper connectorSystemMapper,
                                              ObjectMapper objectMapper,
                                              RestTemplate restTemplate) {
        this.connectorResourceMapper = connectorResourceMapper;
        this.connectorSystemMapper = connectorSystemMapper;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }

    @Override
    public boolean supports(String assertionType) {
        return EXTERNAL_API_RESULT.equals(assertionType);
    }

    @Override
    public AssertionDecision evaluate(EvaluationItem item, Map<String, Object> config, List<ExecutionTrace> traces) {
        if (!StringUtils.hasText(item.getRelatedApiResourceId())) {
            return AssertionDecision.notMatched("MISSING_API_RESOURCE");
        }
        String expected = firstTextConfig(config, "expectedStatus", "expectedResult", "expected");
        String actual = firstTextConfig(config, "actualStatus", "actualResult", "actual");
        if (!StringUtils.hasText(expected)) {
            return AssertionDecision.notMatched("MISSING_RESULT_CONFIG", null, item.getRelatedApiResourceId());
        }
        if (!StringUtils.hasText(actual)) {
            ExternalResult externalResult = requestExternalResult(item, config);
            if (!externalResult.isSuccess()) {
                return AssertionDecision.notMatched(externalResult.getReason(), null, item.getRelatedApiResourceId());
            }
            actual = externalResult.getActual();
        }
        if (expected.equals(actual)) {
            return AssertionDecision.matchedExternal(item.getRelatedApiResourceId());
        }
        return AssertionDecision.notMatched("RESULT_MISMATCH", null, item.getRelatedApiResourceId());
    }

    /**
     * 查询原平台 API 并抽取实际评分值。
     *
     * 业务功能：把评分项中的 API 资源引用转化为一次真实原平台查询。
     * 关键流程：读取正式资源、合并资源 metadata 与断言配置、校验只允许 GET、拼装 URL、请求响应并抽取字段。
     * 这样设计是为了先打通评分闭环，同时避免在评分模块内引入写操作和复杂鉴权副作用。
     *
     * @param item 评分项。
     * @param config 断言配置。
     * @return 外部查询结果。
     */
    private ExternalResult requestExternalResult(EvaluationItem item, Map<String, Object> config) {
        if (connectorResourceMapper == null || connectorSystemMapper == null) {
            return ExternalResult.failed("MISSING_RESULT_CONFIG");
        }
        ConnectorResource resource = connectorResourceMapper.selectById(item.getRelatedApiResourceId());
        if (resource == null || Boolean.TRUE.equals(resource.getDeleted())) {
            return ExternalResult.failed("API_RESOURCE_NOT_FOUND");
        }
        Map<String, Object> metadata = parseJsonMap(resource.getMetadataJson());
        String method = firstTextConfig(config, "method", "httpMethod");
        if (!StringUtils.hasText(method)) {
            method = firstTextConfig(metadata, "method", "httpMethod");
        }
        if (!StringUtils.hasText(method)) {
            method = GET_METHOD;
        }
        if (!GET_METHOD.equalsIgnoreCase(method)) {
            return ExternalResult.failed("UNSUPPORTED_API_METHOD");
        }
        String url = buildUrl(resource, metadata, config);
        if (!StringUtils.hasText(url)) {
            return ExternalResult.failed("MISSING_API_ENDPOINT");
        }
        try {
            String response = restTemplate.getForObject(url, String.class);
            String field = firstTextConfig(config, "responseField", "actualField", "resultField");
            if (!StringUtils.hasText(field)) {
                field = firstTextConfig(metadata, "responseField", "actualField", "resultField");
            }
            return ExternalResult.success(extractActualValue(response, field));
        } catch (RuntimeException ex) {
            return ExternalResult.failed("EXTERNAL_API_CALL_FAILED");
        }
    }

    /**
     * 拼装原平台查询 URL。
     *
     * 业务功能：优先支持资源元数据中的相对 endpoint，并通过原平台 baseUrl 保持资源与接入系统的绑定。
     * 关键流程：endpoint 为绝对地址时直接使用；为相对地址时读取 connector_system.baseUrl 后进行路径拼接。
     *
     * @param resource API 正式资源。
     * @param metadata 资源元数据。
     * @param config 断言配置。
     * @return 可请求的 URL，缺少必要配置时返回空。
     */
    private String buildUrl(ConnectorResource resource, Map<String, Object> metadata, Map<String, Object> config) {
        String endpoint = firstTextConfig(config, "url", "apiUrl", "endpoint", "apiPath");
        if (!StringUtils.hasText(endpoint)) {
            endpoint = firstTextConfig(metadata, "url", "apiUrl", "endpoint", "apiPath");
        }
        if (!StringUtils.hasText(endpoint)) {
            endpoint = resource.getPageUrl();
        }
        if (!StringUtils.hasText(endpoint)) {
            return null;
        }
        if (endpoint.startsWith("http://") || endpoint.startsWith("https://")) {
            return endpoint;
        }
        ConnectorSystem connectorSystem = connectorSystemMapper.selectById(resource.getConnectorSystemId());
        if (connectorSystem == null || !StringUtils.hasText(connectorSystem.getBaseUrl())) {
            return null;
        }
        return joinUrl(connectorSystem.getBaseUrl(), endpoint);
    }

    private String joinUrl(String baseUrl, String endpoint) {
        String normalizedBase = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        return normalizedBase + normalizedEndpoint;
    }

    private String extractActualValue(String response, String field) {
        if (!StringUtils.hasText(response)) {
            return null;
        }
        if (!StringUtils.hasText(field)) {
            return response;
        }
        try {
            JsonNode node = objectMapper.readTree(response);
            for (String segment : field.split("\\.")) {
                if (node == null || node.isMissingNode() || node.isNull()) {
                    return null;
                }
                node = node.path(segment);
            }
            if (node == null || node.isMissingNode() || node.isNull()) {
                return null;
            }
            return node.isTextual() ? node.asText() : node.toString();
        } catch (Exception ex) {
            return null;
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Collections.emptyMap();
        }
    }

    private String firstTextConfig(Map<String, Object> config, String... keys) {
        for (String key : keys) {
            Object value = config.get(key);
            String text = value == null ? null : String.valueOf(value);
            if (StringUtils.hasText(text)) {
                return text;
            }
        }
        return null;
    }

    private static class ExternalResult {

        private final boolean success;

        private final String actual;

        private final String reason;

        private ExternalResult(boolean success, String actual, String reason) {
            this.success = success;
            this.actual = actual;
            this.reason = reason;
        }

        private static ExternalResult success(String actual) {
            if (!StringUtils.hasText(actual)) {
                return failed("MISSING_RESULT_CONFIG");
            }
            return new ExternalResult(true, actual, null);
        }

        private static ExternalResult failed(String reason) {
            return new ExternalResult(false, null, reason);
        }

        private boolean isSuccess() {
            return success;
        }

        private String getActual() {
            return actual;
        }

        private String getReason() {
            return reason;
        }
    }
}
