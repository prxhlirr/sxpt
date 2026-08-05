package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.entity.PlatformCapability;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.connector.mapper.PlatformCapabilityMapper;
import com.sxpt.module.connector.service.OriginDataPrepareAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Locale;

/**
 * HTTP 原平台数据准备适配器。
 *
 * 业务功能：
 * 1. 根据 connector_system 和 platform_capability 的运行配置调用真实原平台接口。
 * 2. 将教学平台内部标准请求提交给原平台，并把原平台标准响应映射回 OriginDataPrepareAdapter 契约。
 * 3. 保持数据准备编排服务只依赖统一 Adapter，不感知 HTTP 地址、认证头和响应包装细节。
 *
 * 关键流程：
 * 1. 按租户、原平台和能力编码读取启用能力，例如 DATA_CREATE。
 * 2. 解析 endpointUrl、HTTP method、认证配置并发起 JSON 请求。
 * 3. 兼容直接返回标准响应或返回 {success, code, result} 包装响应。
 */
@Service
@ConditionalOnProperty(name = "sxpt.origin.adapter-mode", havingValue = "http")
public class HttpOriginDataPrepareAdapter implements OriginDataPrepareAdapter {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private static final String CAPABILITY_DATA_CREATE = "DATA_CREATE";

    private static final String CAPABILITY_DATA_QUERY = "DATA_QUERY";

    private static final String CAPABILITY_RESULT_CHECK = "RESULT_CHECK";

    private static final String CAPABILITY_DATA_LOCK = "DATA_LOCK";

    private static final String CAPABILITY_DATA_ARCHIVE = "DATA_ARCHIVE";

    private static final String AUTH_TYPE_BEARER = "BEARER";

    private static final String AUTH_TYPE_API_KEY = "API_KEY";

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final ConnectorSystemMapper connectorSystemMapper;

    private final PlatformCapabilityMapper platformCapabilityMapper;

    private final RestTemplate restTemplate;

    @Autowired
    public HttpOriginDataPrepareAdapter(ConnectorSystemMapper connectorSystemMapper,
                                        PlatformCapabilityMapper platformCapabilityMapper) {
        this(connectorSystemMapper, platformCapabilityMapper, new RestTemplate());
    }

    public HttpOriginDataPrepareAdapter(ConnectorSystemMapper connectorSystemMapper,
                                        PlatformCapabilityMapper platformCapabilityMapper,
                                        RestTemplate restTemplate) {
        this.connectorSystemMapper = connectorSystemMapper;
        this.platformCapabilityMapper = platformCapabilityMapper;
        this.restTemplate = restTemplate;
    }

    /**
     * 调用真实原平台批量创建教学用业务数据。
     *
     * @param request 批量创建请求。
     * @return 原平台标准批量创建响应。
     */
    @Override
    public BatchCreateResponse createTeachingData(BatchCreateRequest request) {
        System.out.println("66666666666666666666666666666666666666666");
        System.out.println("request: " + request.getRequestBatchId() + request.getTenantId() + request.getConnectorSystemId() + ", items: " + request.getItems().size());
        OriginCallContext context = buildCallContext(
                request == null ? null : request.getTenantId(),
                request == null ? null : request.getConnectorSystemId(),
                CAPABILITY_DATA_CREATE);
        return exchange(context, request, BatchCreateResponse.class);
    }

    /**
     * 调用真实原平台查询业务数据摘要。
     *
     * @param request 查询请求。
     * @return 原平台查询响应。
     */
    @Override
    public QueryResponse queryTeachingData(QueryRequest request) {
        OriginCallContext context = buildCallContext(
                request == null ? null : request.getTenantId(),
                request == null ? null : request.getConnectorSystemId(),
                CAPABILITY_DATA_QUERY);
        return exchange(context, request, QueryResponse.class);
    }

    /**
     * 调用真实原平台校验业务数据是否满足教学约束。
     *
     * @param request 校验请求。
     * @return 原平台校验响应。
     */
    @Override
    public ValidationResponse validateTeachingData(ValidationRequest request) {
        OriginCallContext context = buildCallContext(
                request == null ? null : request.getTenantId(),
                request == null ? null : request.getConnectorSystemId(),
                CAPABILITY_RESULT_CHECK);
        return exchange(context, request, ValidationResponse.class);
    }

    /**
     * 调用真实原平台锁定业务数据。
     *
     * @param request 锁定请求。
     * @return 原平台锁定响应。
     */
    @Override
    public LockResponse lockTeachingData(LockRequest request) {
        OriginCallContext context = buildCallContext(
                request == null ? null : request.getTenantId(),
                request == null ? null : request.getConnectorSystemId(),
                CAPABILITY_DATA_LOCK);
        return exchange(context, request, LockResponse.class);
    }

    /**
     * 调用真实原平台归档业务数据。
     *
     * @param request 归档请求。
     * @return 原平台归档响应。
     */
    @Override
    public ArchiveResponse archiveTeachingData(ArchiveRequest request) {
        OriginCallContext context = buildCallContext(
                request == null ? null : request.getTenantId(),
                request == null ? null : request.getConnectorSystemId(),
                CAPABILITY_DATA_ARCHIVE);
        return exchange(context, request, ArchiveResponse.class);
    }

    /**
     * 构建一次原平台能力调用上下文。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台系统 ID。
     * @param capabilityCode 能力编码。
     * @return 调用上下文。
     */
    private OriginCallContext buildCallContext(String tenantId, String connectorSystemId, String capabilityCode) {
        requireText(tenantId);
        requireText(connectorSystemId);
        ConnectorSystem connectorSystem = connectorSystemMapper.selectOne(new QueryWrapper<ConnectorSystem>()
                .eq("tenant_id", tenantId)
                .eq("id", connectorSystemId)
                .eq("status", STATUS_ACTIVE)
                .eq("deleted", Boolean.FALSE)
                .last("limit 1"));
        if (connectorSystem == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        PlatformCapability capability = platformCapabilityMapper.selectOne(new QueryWrapper<PlatformCapability>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("capability_code", capabilityCode)
                .eq("support_flag", Boolean.TRUE)
                .eq("status", STATUS_ACTIVE)
                .eq("deleted", Boolean.FALSE)
                .last("limit 1"));
        if (capability == null) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                    "原平台未配置可用的 " + capabilityCode + " 能力");
        }
        requireText(capability.getEndpointUrl());
        requireText(capability.getMethod());
        OriginCallContext context = new OriginCallContext();
        context.setConnectorSystem(connectorSystem);
        context.setCapability(capability);
        context.setUrl(resolveUrl(connectorSystem, capability));
        context.setHttpMethod(resolveHttpMethod(capability.getMethod()));
        return context;
    }

    /**
     * 发起 HTTP 请求并解析标准响应。
     *
     * @param context 原平台调用上下文。
     * @param request 请求对象。
     * @param responseType 响应类型。
     * @param <T> 响应泛型。
     * @return 解析后的标准响应对象。
     */
    private <T> T exchange(OriginCallContext context, Object request, Class<T> responseType) {
        HttpEntity<Object> entity = new HttpEntity<>(request, buildHeaders(context.getConnectorSystem()));
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    context.getUrl(), context.getHttpMethod(), entity, String.class);
            return parseResponse(response.getBody(), responseType);
        } catch (RestClientException ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR.getCode(),
                    "调用原平台接口失败：" + ex.getMessage());
        }
    }

    /**
     * 构建原平台 HTTP 请求头。
     *
     * @param connectorSystem 原平台系统配置。
     * @return HTTP 请求头。
     */
    private HttpHeaders buildHeaders(ConnectorSystem connectorSystem) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String authType = normalize(connectorSystem.getAuthType());
        JsonNode config = parseConfigJson(connectorSystem.getConfigJson());
        if (AUTH_TYPE_BEARER.equals(authType)) {
            String token = textValue(config, "token");
            if (!StringUtils.hasText(token)) {
                throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                        "原平台 BEARER 认证缺少 token");
            }
            headers.setBearerAuth(token);
        } else if (AUTH_TYPE_API_KEY.equals(authType)) {
            String headerName = firstText(textValue(config, "headerName"), "X-API-Key");
            String apiKey = textValue(config, "apiKey");
            if (!StringUtils.hasText(apiKey)) {
                throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                        "原平台 API_KEY 认证缺少 apiKey");
            }
            headers.set(headerName, apiKey);
        }
        System.out.println("888888");
        System.out.println("原平台请求头: " + headers.toSingleValueMap());
        return headers;
    }

    /**
     * 解析原平台响应，兼容标准对象和 ApiResult 包装对象。
     *
     * @param responseBody HTTP 响应体。
     * @param responseType 目标响应类型。
     * @param <T> 响应泛型。
     * @return 目标响应对象。
     */
    private <T> T parseResponse(String responseBody, Class<T> responseType) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR.getCode(), "原平台接口返回为空");
        }
        try {
            JsonNode root = JSON_MAPPER.readTree(responseBody);
            System.out.println("************************");
            System.out.println("原平台响应 JSON: " + root.toString());
            JsonNode payload = root.has("result") ? root.get("result") : root;
            return JSON_MAPPER.treeToValue(payload, responseType);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR.getCode(),
                    "原平台接口响应格式不符合标准契约");
        }
    }

    /**
     * 解析原平台 endpoint，支持绝对地址和 baseUrl + 相对路径。
     *
     * @param connectorSystem 原平台系统配置。
     * @param capability 能力配置。
     * @return 可调用 URL。
     */
    private String resolveUrl(ConnectorSystem connectorSystem, PlatformCapability capability) {
        String endpointUrl = capability.getEndpointUrl();
        if (endpointUrl.startsWith("http://") || endpointUrl.startsWith("https://")) {
            return endpointUrl;
        }
        requireText(connectorSystem.getBaseUrl());
        String baseUrl = connectorSystem.getBaseUrl();
        if (baseUrl.endsWith("/") && endpointUrl.startsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + endpointUrl;
        }
        if (!baseUrl.endsWith("/") && !endpointUrl.startsWith("/")) {
            return baseUrl + "/" + endpointUrl;
        }
        return baseUrl + endpointUrl;
    }

    /**
     * 解析 HTTP 方法。
     *
     * @param method 配置的方法名。
     * @return Spring HTTP 方法。
     */
    private HttpMethod resolveHttpMethod(String method) {
        try {
            return HttpMethod.valueOf(method.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 解析原平台系统认证配置。
     *
     * @param configJson 配置 JSON。
     * @return JSON 节点。
     */
    private JsonNode parseConfigJson(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            return JSON_MAPPER.createObjectNode();
        }
        try {
            return JSON_MAPPER.readTree(configJson);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                    "原平台认证配置不是合法 JSON");
        }
    }

    /**
     * 读取 JSON 字符串字段。
     *
     * @param node JSON 节点。
     * @param fieldName 字段名。
     * @return 字段文本。
     */
    private String textValue(JsonNode node, String fieldName) {
        if (node == null || !node.has(fieldName) || node.get(fieldName).isNull()) {
            return null;
        }
        return node.get(fieldName).asText();
    }

    /**
     * 校验文本非空。
     *
     * @param value 待校验文本。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 归一化配置枚举值。
     *
     * @param value 原始配置值。
     * @return 大写后的配置值。
     */
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT) : "";
    }

    /**
     * 返回第一个非空文本。
     *
     * @param first 第一候选。
     * @param second 第二候选。
     * @return 非空文本或 null。
     */
    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    /**
     * 原平台 HTTP 调用上下文。
     *
     * 业务功能：把一次能力调用需要的系统配置、能力配置、URL 和方法聚合起来，避免各方法重复解析配置。
     */
    private static class OriginCallContext {

        private ConnectorSystem connectorSystem;

        private PlatformCapability capability;

        private String url;

        private HttpMethod httpMethod;

        public ConnectorSystem getConnectorSystem() {
            return connectorSystem;
        }

        public void setConnectorSystem(ConnectorSystem connectorSystem) {
            this.connectorSystem = connectorSystem;
        }

        public PlatformCapability getCapability() {
            return capability;
        }

        public void setCapability(PlatformCapability capability) {
            this.capability = capability;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public HttpMethod getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(HttpMethod httpMethod) {
            this.httpMethod = httpMethod;
        }
    }
}
