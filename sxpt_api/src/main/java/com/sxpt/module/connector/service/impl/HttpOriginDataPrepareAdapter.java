package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.entity.IdentityBinding;
import com.sxpt.module.connector.entity.PlatformCapability;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.connector.mapper.IdentityBindingMapper;
import com.sxpt.module.connector.mapper.PlatformCapabilityMapper;
import com.sxpt.module.connector.mapper.TeachingDataTemplateMapper;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    private static final String STANDARD_BATCH_CREATE_CONTRACT = "TEACHING_DATA_BATCH_CREATE_V1";

    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    private static final String HEADER_IDEMPOTENCY_KEY = "X-Idempotency-Key";

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private final ConnectorSystemMapper connectorSystemMapper;

    private final PlatformCapabilityMapper platformCapabilityMapper;

    private final TeachingDataTemplateMapper teachingDataTemplateMapper;

    private final IdentityBindingMapper identityBindingMapper;

    private final RestTemplate restTemplate;

    @Autowired
    public HttpOriginDataPrepareAdapter(ConnectorSystemMapper connectorSystemMapper,
                                        PlatformCapabilityMapper platformCapabilityMapper,
                                        TeachingDataTemplateMapper teachingDataTemplateMapper,
                                        IdentityBindingMapper identityBindingMapper) {
        this(connectorSystemMapper, platformCapabilityMapper, teachingDataTemplateMapper,
                identityBindingMapper, new RestTemplate());
    }

    public HttpOriginDataPrepareAdapter(ConnectorSystemMapper connectorSystemMapper,
                                        PlatformCapabilityMapper platformCapabilityMapper,
                                        TeachingDataTemplateMapper teachingDataTemplateMapper,
                                        IdentityBindingMapper identityBindingMapper,
                                        RestTemplate restTemplate) {
        this.connectorSystemMapper = connectorSystemMapper;
        this.platformCapabilityMapper = platformCapabilityMapper;
        this.teachingDataTemplateMapper = teachingDataTemplateMapper;
        this.identityBindingMapper = identityBindingMapper;
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
        OriginCallContext context = buildCallContext(
                request == null ? null : request.getTenantId(),
                request == null ? null : request.getConnectorSystemId(),
                CAPABILITY_DATA_CREATE);
        if (usesStandardBatchCreateContract(context.getCapability())) {
            return createTeachingDataWithStandardContract(context, request);
        }
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
        String responseBody = exchangeForBody(context, request);
        return parseResponse(responseBody, responseType);
    }

    /**
     * 发起一次原平台 HTTP 调用并保留业务错误响应体。
     *
     * 标准造数接口使用 HTTP 422 表达部分成功；该响应仍包含成功数据，不能被 RestTemplate
     * 默认错误处理直接丢弃。
     */
    private String exchangeForBody(OriginCallContext context, Object request) {
        HttpEntity<Object> entity = new HttpEntity<>(request, buildHeaders(context, request));
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    context.getUrl(), context.getHttpMethod(), entity, String.class);
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            if (ex.getRawStatusCode() == 422 && StringUtils.hasText(ex.getResponseBodyAsString())) {
                return ex.getResponseBodyAsString();
            }
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR.getCode(),
                    "调用原平台接口失败（HTTP " + ex.getRawStatusCode() + "）：" + ex.getStatusText());
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
    private HttpHeaders buildHeaders(OriginCallContext context, Object request) {
        ConnectorSystem connectorSystem = context.getConnectorSystem();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        String authType = normalize(connectorSystem.getAuthType());
        JsonNode config = parseConfigJson(connectorSystem.getConfigJson());
        if (AUTH_TYPE_BEARER.equals(authType)) {
            String token = resolveConfiguredSecret(config, "tokenEnv", "token");
            if (!StringUtils.hasText(token)) {
                throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                        "原平台 BEARER 认证缺少 token");
            }
            headers.setBearerAuth(token);
        } else if (AUTH_TYPE_API_KEY.equals(authType)) {
            String headerName = firstText(textValue(config, "headerName"), "X-API-Key");
            String apiKey = resolveConfiguredSecret(config, "apiKeyEnv", "apiKey");
            if (!StringUtils.hasText(apiKey)) {
                throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                        "原平台 API_KEY 认证缺少 apiKey");
            }
            headers.set(headerName, apiKey);
        }
        BatchCreateRequest batchRequest = request instanceof BatchCreateRequest
                ? (BatchCreateRequest) request
                : request instanceof StandardBatchCreateHttpRequest
                    ? ((StandardBatchCreateHttpRequest) request).getSource()
                    : null;
        if (batchRequest != null && usesStandardBatchCreateContract(context.getCapability())) {
            headers.set(HEADER_TRACE_ID, firstText(batchRequest.getTraceId(), batchRequest.getRequestBatchId()));
            headers.set(HEADER_IDEMPOTENCY_KEY,
                    firstText(batchRequest.getIdempotencyKey(), batchRequest.getRequestBatchId()));
        }
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
     * 优先从配置指定的环境变量读取密钥，未设置时使用开发配置中的回退值。
     */
    private String resolveConfiguredSecret(JsonNode config, String envField, String fallbackField) {
        String envName = textValue(config, envField);
        if (StringUtils.hasText(envName)) {
            String environmentValue = System.getenv(envName);
            if (StringUtils.hasText(environmentValue)) {
                return environmentValue;
            }
        }
        return textValue(config, fallbackField);
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
     * 按能力配置判断 DATA_CREATE 是否使用教学平台标准造数契约。
     */
    private boolean usesStandardBatchCreateContract(PlatformCapability capability) {
        JsonNode schema = parseConfigJson(capability.getRequestSchemaJson());
        return STANDARD_BATCH_CREATE_CONTRACT.equalsIgnoreCase(textValue(schema, "contract"))
                || "/openapi/teaching-data/batch-create".equals(capability.getEndpointUrl());
    }

    /**
     * 将教学平台内部批次请求转换为第三方标准请求，并映射标准响应。
     */
    private BatchCreateResponse createTeachingDataWithStandardContract(OriginCallContext context,
                                                                        BatchCreateRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        TeachingDataTemplate template = getActiveTemplate(request);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("businessModuleCode", request.getModuleCode());
        payload.put("templateCode", template.getTemplateCode());
        payload.put("initState", firstText(request.getInitState(), template.getInitState()));
        payload.put("sceneType", request.getSceneType());
        payload.put("participants", buildStandardParticipants(context, request));
        payload.put("bizParams", resolveBizParams(request.getRequestJson(), template.getConfigJson()));
        String responseBody = exchangeForBody(context, payloadWithHeaders(payload, request));
        return parseStandardBatchCreateResponse(responseBody, request);
    }

    /**
     * 使用一个只承载请求头上下文的 Map 包装标准 body。实际交换前会解包，避免污染外部 JSON。
     */
    private StandardBatchCreateHttpRequest payloadWithHeaders(Map<String, Object> payload,
                                                               BatchCreateRequest source) {
        return new StandardBatchCreateHttpRequest(payload, source);
    }

    private TeachingDataTemplate getActiveTemplate(BatchCreateRequest request) {
        requireText(request.getTemplateId());
        TeachingDataTemplate template = teachingDataTemplateMapper.selectOne(
                new QueryWrapper<TeachingDataTemplate>()
                        .eq("tenant_id", request.getTenantId())
                        .eq("connector_system_id", request.getConnectorSystemId())
                        .eq("id", request.getTemplateId())
                        .eq("status", STATUS_ACTIVE)
                        .eq("deleted", Boolean.FALSE)
                        .last("limit 1"));
        if (template == null) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                    "当前数据准备模板不存在或未启用");
        }
        return template;
    }

    private List<Map<String, Object>> buildStandardParticipants(OriginCallContext context,
                                                                 BatchCreateRequest request) {
        JsonNode schema = parseConfigJson(context.getCapability().getRequestSchemaJson());
        String defaultPoolKey = textValue(schema, "defaultPoolKey");
        boolean preferPoolKey = schema.path("preferPoolKey").asBoolean(false);
        List<Map<String, Object>> participants = new ArrayList<>();
        for (RequestItem item : request.getItems()) {
            Map<String, Object> participant = new LinkedHashMap<>();
            participant.put("participantId", item.getRequestItemId());
            if (StringUtils.hasText(item.getStudentId())) {
                participant.put("ownerUserId", item.getStudentId());
            }
            IdentityBinding binding = findIdentityBinding(request, item.getStudentId());
            String externalUserId = binding == null ? null : binding.getExternalUserId();
            String externalOrgId = firstText(resolveExternalOrgId(binding), item.getRequiredExternalOrgId());
            if (preferPoolKey && StringUtils.hasText(defaultPoolKey)) {
                participant.put("poolKey", defaultPoolKey);
            } else {
                if (StringUtils.hasText(externalUserId)) {
                    participant.put("externalUserId", externalUserId);
                }
                if (StringUtils.hasText(externalOrgId)) {
                    participant.put("externalOrgId", externalOrgId);
                }
                if (!StringUtils.hasText(externalUserId)
                        && !StringUtils.hasText(externalOrgId)
                        && StringUtils.hasText(defaultPoolKey)) {
                    participant.put("poolKey", defaultPoolKey);
                }
            }
            participants.add(participant);
        }
        return participants;
    }

    private IdentityBinding findIdentityBinding(BatchCreateRequest request, String userId) {
        if (!StringUtils.hasText(userId)) {
            return null;
        }
        return identityBindingMapper.selectOne(new QueryWrapper<IdentityBinding>()
                .eq("tenant_id", request.getTenantId())
                .eq("connector_system_id", request.getConnectorSystemId())
                .eq("user_id", userId)
                .eq("status", STATUS_ACTIVE)
                .eq("deleted", Boolean.FALSE)
                .last("limit 1"));
    }

    private String resolveExternalOrgId(IdentityBinding binding) {
        if (binding == null || !StringUtils.hasText(binding.getExternalOrgJson())) {
            return null;
        }
        JsonNode org = parseConfigJson(binding.getExternalOrgJson());
        if (org.isArray() && org.size() > 0) {
            org = org.get(0);
        }
        return firstText(textValue(org, "externalOrgId"),
                firstText(textValue(org, "orgId"), textValue(org, "id")));
    }

    private Object resolveBizParams(String requestJson, String templateConfigJson) {
        JsonNode requestNode = parseConfigJson(requestJson);
        JsonNode bizParams = findBizParams(requestNode);
        if (bizParams == null) {
            bizParams = findBizParams(parseConfigJson(templateConfigJson));
        }
        if (bizParams == null || !bizParams.isObject()) {
            return new LinkedHashMap<String, Object>();
        }
        return JSON_MAPPER.convertValue(bizParams, Object.class);
    }

    private JsonNode findBizParams(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.has("bizParams") && node.get("bizParams").isObject()) {
            return node.get("bizParams");
        }
        if (node.has("sourceRequestJson") && node.get("sourceRequestJson").isTextual()) {
            return findBizParams(parseConfigJson(node.get("sourceRequestJson").asText()));
        }
        return null;
    }

    private BatchCreateResponse parseStandardBatchCreateResponse(String responseBody,
                                                                  BatchCreateRequest request) {
        if (!StringUtils.hasText(responseBody)) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR.getCode(), "原平台接口返回为空");
        }
        try {
            JsonNode root = JSON_MAPPER.readTree(responseBody);
            JsonNode result = root.has("result") ? root.get("result") : root;
            BatchCreateResponse response = new BatchCreateResponse();
            response.setExternalRequestId(textValue(result, "originBatchId"));
            response.setRequestBatchId(request.getRequestBatchId());
            response.setAdapterStatus(firstText(textValue(result, "status"),
                    root.path("success").asBoolean(false) ? "SUCCESS" : "FAILED"));
            response.setResultJson(root.toString());
            List<ResponseItem> items = new ArrayList<>();
            appendStandardSuccessItems(items, result.path("items"));
            appendStandardFailedItems(items, result.path("failedItems"));
            response.setItems(items);
            return response;
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR.getCode(),
                    "原平台接口响应格式不符合标准造数契约");
        }
    }

    private void appendStandardSuccessItems(List<ResponseItem> target, JsonNode source) {
        if (!source.isArray()) {
            return;
        }
        for (JsonNode item : source) {
            ResponseItem mapped = new ResponseItem();
            mapped.setRequestItemId(textValue(item, "participantId"));
            mapped.setExternalBusinessId(textValue(item, "externalDataId"));
            mapped.setExternalBusinessNo(textValue(item, "externalBizNo"));
            mapped.setExternalStatus(textValue(item, "externalStatus"));
            mapped.setTargetUrl(textValue(item, "entryUrl"));
            mapped.setCurrentOrgId(textValue(item, "externalOrgId"));
            JsonNode rawData = item.path("rawData");
            mapped.setExternalBusinessName(firstText(textValue(rawData, "title"), mapped.getExternalBusinessNo()));
            mapped.setItemStatus("SUCCESS");
            target.add(mapped);
        }
    }

    private void appendStandardFailedItems(List<ResponseItem> target, JsonNode source) {
        if (!source.isArray()) {
            return;
        }
        for (JsonNode item : source) {
            ResponseItem mapped = new ResponseItem();
            mapped.setRequestItemId(textValue(item, "participantId"));
            mapped.setItemStatus("FAILED");
            String errorCode = textValue(item, "errorCode");
            String errorMessage = textValue(item, "errorMessage");
            mapped.setErrorMessage(StringUtils.hasText(errorCode)
                    ? errorCode + "：" + firstText(errorMessage, "原平台造数失败")
                    : firstText(errorMessage, "原平台造数失败"));
            target.add(mapped);
        }
    }

    /**
     * 标准请求 body 与内部请求头上下文的组合；RestTemplate 实际发送时只发送 payload。
     */
    private static class StandardBatchCreateHttpRequest extends LinkedHashMap<String, Object> {

        private final BatchCreateRequest source;

        StandardBatchCreateHttpRequest(Map<String, Object> payload, BatchCreateRequest source) {
            super(payload);
            this.source = source;
        }

        BatchCreateRequest getSource() {
            return source;
        }
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
