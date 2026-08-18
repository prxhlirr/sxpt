package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final TypeReference<Map<String, Object>> MAP_TYPE =
            new TypeReference<Map<String, Object>>() {
            };

    private static final String HEADER_TRACE_ID = "X-Trace-Id";

    private static final String HEADER_IDEMPOTENCY_KEY = "X-Idempotency-Key";

    private static final String STATUS_SUCCESS = "SUCCESS";

    private static final String STATUS_FAILED = "FAILED";

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
        OriginCallContext context = buildCallContext(
                request == null ? null : request.getTenantId(),
                request == null ? null : request.getConnectorSystemId(),
                CAPABILITY_DATA_CREATE);
        Map<String, String> participantIdCorrelation = buildParticipantIdCorrelation(request);
        OriginBatchCreateRequest originRequest = buildOriginBatchCreateRequest(
                request, context.getCapability());
        OriginBatchCreateResponse originResponse = exchange(
                context,
                originRequest,
                buildHeaders(context.getConnectorSystem(), request),
                OriginBatchCreateResponse.class);
        return toBatchCreateResponse(originResponse, request, participantIdCorrelation);
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
        return exchange(context, request, buildHeaders(context.getConnectorSystem(), null), QueryResponse.class);
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
        return exchange(context, request, buildHeaders(context.getConnectorSystem(), null), ValidationResponse.class);
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
        return exchange(context, request, buildHeaders(context.getConnectorSystem(), null), LockResponse.class);
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
        return exchange(context, request, buildHeaders(context.getConnectorSystem(), null), ArchiveResponse.class);
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
    private <T> T exchange(OriginCallContext context,
                           Object request,
                           HttpHeaders headers,
                           Class<T> responseType) {
        HttpEntity<Object> entity = new HttpEntity<>(request, headers);
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
    private HttpHeaders buildHeaders(ConnectorSystem connectorSystem, BatchCreateRequest request) {
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
        } else {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                    "原平台认证方式仅支持 API_KEY");
        }
        if (request != null) {
            requireText(request.getIdempotencyKey());
            headers.set(HEADER_IDEMPOTENCY_KEY, request.getIdempotencyKey());
            headers.set(HEADER_TRACE_ID, resolveTraceId(request));
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
     * 构造符合第三方造数接口文档的请求体。
     *
     * 业务功能：把教学平台内部批量创建请求转换为原平台公开 HTTP 契约，避免把内部批次、系统 ID 和明细字段泄漏给第三方。
     * 关键流程：从数据准备快照中解析模板编码、初始状态和扩展参数；将每条明细转换为 participants。
     *
     * @param request 内部批量创建请求。
     * @return 第三方造数请求。
     */
    private OriginBatchCreateRequest buildOriginBatchCreateRequest(BatchCreateRequest request,
                                                                    PlatformCapability capability) {
        validateBatchCreateRequest(request);
        JsonNode requestSnapshot = parseOptionalJson(request.getRequestJson());
        JsonNode capabilityContract = parseOptionalJson(
                capability == null ? null : capability.getRequestSchemaJson());
        boolean classicCaseRequest = isClassicCaseRequest(requestSnapshot);
        OriginBatchCreateRequest originRequest = new OriginBatchCreateRequest();
        originRequest.setBusinessModuleCode(firstText(
                textValue(capabilityContract, "businessModuleCode"), request.getModuleCode()));
        originRequest.setTemplateCode(classicCaseRequest
                ? resolveTemplateCode(requestSnapshot)
                : firstText(textValue(capabilityContract, "templateCode"), resolveTemplateCode(requestSnapshot)));
        originRequest.setInitState(classicCaseRequest
                ? resolveInitState(requestSnapshot)
                : firstText(textValue(capabilityContract, "initState"), resolveInitState(requestSnapshot)));
        originRequest.setSceneType(request.getSceneType());
        originRequest.setParticipants(buildOriginParticipants(request, capabilityContract));
        originRequest.setBizParams(resolveBizParams(requestSnapshot, capabilityContract));
        return originRequest;
    }

    /**
     * 校验内部批量创建请求满足转换外部契约所需的最小字段。
     *
     * @param request 内部批量创建请求。
     */
    private void validateBatchCreateRequest(BatchCreateRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getTenantId());
        requireText(request.getConnectorSystemId());
        requireText(request.getModuleCode());
        requireText(request.getSceneType());
        requireText(request.getRequestBatchId());
        requireText(request.getIdempotencyKey());
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 构造第三方造数参与方列表。
     *
     * @param request 内部批量创建请求。
     * @return 第三方造数参与方列表。
     */
    private List<OriginParticipant> buildOriginParticipants(BatchCreateRequest request,
                                                            JsonNode capabilityContract) {
        boolean preferPoolKey = booleanValue(capabilityContract, "preferPoolKey", false);
        String defaultPoolKey = textValue(capabilityContract, "defaultPoolKey");
        List<OriginParticipant> participants = new ArrayList<>();
        for (RequestItem item : request.getItems()) {
            if (item == null) {
                continue;
            }
            requireText(item.getRequestItemId());
            OriginParticipant participant = new OriginParticipant();
            participant.setParticipantId(toExternalParticipantId(item.getRequestItemId()));
            participant.setOwnerUserId(item.getStudentId());
            if (preferPoolKey && StringUtils.hasText(item.getQuestionId())) {
                participant.setPoolKey(firstText(defaultPoolKey, item.getQuestionId()));
            } else {
                participant.setExternalOrgId(item.getRequiredExternalOrgId());
                participant.setPoolKey(item.getQuestionId());
            }
            participants.add(participant);
        }
        if (participants.isEmpty()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        return participants;
    }

    private Map<String, String> buildParticipantIdCorrelation(BatchCreateRequest request) {
        validateBatchCreateRequest(request);
        Map<String, String> correlation = new LinkedHashMap<>();
        for (RequestItem item : request.getItems()) {
            if (item == null) {
                continue;
            }
            requireText(item.getRequestItemId());
            String externalId = toExternalParticipantId(item.getRequestItemId());
            String existingInternalId = correlation.putIfAbsent(externalId, item.getRequestItemId());
            if (existingInternalId != null && !existingInternalId.equals(item.getRequestItemId())) {
                throw new BusinessException(
                        ApiResultCode.SYSTEM_ERROR.getCode(), "外部参与方标识发生冲突");
            }
        }
        return correlation;
    }

    private String toExternalParticipantId(String internalId) {
        if (internalId.length() <= 64) {
            return internalId;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(internalId.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                int unsignedValue = value & 0xff;
                hex.append(Character.forDigit(unsignedValue >>> 4, 16));
                hex.append(Character.forDigit(unsignedValue & 0x0f, 16));
            }
            return "p_" + hex.substring(0, 62);
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(
                    ApiResultCode.SYSTEM_ERROR.getCode(), "运行环境不支持 SHA-256");
        }
    }

    /**
     * 将第三方造数响应转换回内部适配器响应。
     *
     * @param originResponse 第三方造数响应。
     * @param request 内部批量创建请求。
     * @return 内部批量创建响应。
     */
    private BatchCreateResponse toBatchCreateResponse(OriginBatchCreateResponse originResponse,
                                                      BatchCreateRequest request,
                                                      Map<String, String> participantIdCorrelation) {
        if (originResponse == null) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR.getCode(), "原平台接口返回为空");
        }
        BatchCreateResponse response = new BatchCreateResponse();
        response.setExternalRequestId(originResponse.getOriginBatchId());
        response.setRequestBatchId(request.getRequestBatchId());
        response.setAdapterStatus(firstText(originResponse.getStatus(), STATUS_FAILED));
        response.setResultJson(toJson(originResponse));
        List<ResponseItem> items = new ArrayList<>();
        if (originResponse.getItems() != null) {
            for (OriginResponseItem originItem : originResponse.getItems()) {
                items.add(toSuccessResponseItem(originItem, participantIdCorrelation));
            }
        }
        if (originResponse.getFailedItems() != null) {
            for (OriginFailedItem failedItem : originResponse.getFailedItems()) {
                items.add(toFailedResponseItem(failedItem, participantIdCorrelation));
            }
        }
        response.setItems(items);
        return response;
    }

    /**
     * 转换第三方成功明细。
     *
     * @param originItem 第三方成功明细。
     * @return 内部响应明细。
     */
    private ResponseItem toSuccessResponseItem(OriginResponseItem originItem,
                                               Map<String, String> participantIdCorrelation) {
        ResponseItem item = new ResponseItem();
        if (originItem == null) {
            item.setItemStatus(STATUS_FAILED);
            item.setErrorMessage("原平台返回空明细");
            return item;
        }
        item.setRequestItemId(resolveInternalParticipantId(
                originItem.getParticipantId(), participantIdCorrelation));
        item.setExternalBusinessId(originItem.getExternalDataId());
        item.setExternalBusinessNo(originItem.getExternalBizNo());
        item.setExternalStatus(originItem.getExternalStatus());
        item.setTargetUrl(originItem.getEntryUrl());
        item.setCurrentOrgId(originItem.getExternalOrgId());
        item.setItemStatus(StringUtils.hasText(originItem.getExternalDataId()) ? STATUS_SUCCESS : STATUS_FAILED);
        item.setErrorMessage(StringUtils.hasText(originItem.getExternalDataId()) ? null : "原平台未返回业务数据 ID");
        return item;
    }

    /**
     * 转换第三方失败明细。
     *
     * @param failedItem 第三方失败明细。
     * @return 内部响应明细。
     */
    private ResponseItem toFailedResponseItem(OriginFailedItem failedItem,
                                              Map<String, String> participantIdCorrelation) {
        ResponseItem item = new ResponseItem();
        if (failedItem == null) {
            item.setItemStatus(STATUS_FAILED);
            item.setErrorMessage("原平台返回空失败明细");
            return item;
        }
        item.setRequestItemId(resolveInternalParticipantId(
                failedItem.getParticipantId(), participantIdCorrelation));
        item.setItemStatus(STATUS_FAILED);
        item.setErrorMessage(firstText(failedItem.getErrorMessage(), failedItem.getErrorCode()));
        return item;
    }

    private String resolveInternalParticipantId(String externalId,
                                                Map<String, String> participantIdCorrelation) {
        return firstText(participantIdCorrelation.get(externalId), externalId);
    }

    /**
     * 从数据准备快照中解析模板编码。
     *
     * @param requestSnapshot 数据准备请求快照。
     * @return 模板编码。
     */
    private String resolveTemplateCode(JsonNode requestSnapshot) {
        String templateCode = firstText(
                textValue(requestSnapshot, "templateCode"),
                nestedTextValue(requestSnapshot, "template", "code"));
        templateCode = firstText(templateCode, nestedTextValue(requestSnapshot, "template", "templateCode"));
        if (!StringUtils.hasText(templateCode)) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                    "原平台造数请求缺少 templateCode，请在数据准备快照中固化模板编码");
        }
        return templateCode;
    }

    /**
     * 从数据准备快照中解析初始状态。
     *
     * @param requestSnapshot 数据准备请求快照。
     * @return 初始状态。
     */
    private String resolveInitState(JsonNode requestSnapshot) {
        String initState = firstText(
                textValue(requestSnapshot, "initState"),
                nestedTextValue(requestSnapshot, "template", "initState"));
        if (!StringUtils.hasText(initState)) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                    "原平台造数请求缺少 initState，请在数据准备快照中固化初始业务状态");
        }
        return initState;
    }

    /**
     * 从快照中解析业务扩展参数。
     *
     * @param requestSnapshot 数据准备请求快照。
     * @return 业务扩展参数。
     */
    private Map<String, Object> resolveBizParams(JsonNode requestSnapshot,
                                                 JsonNode capabilityContract) {
        if (!isClassicCaseRequest(requestSnapshot)
                && !booleanValue(capabilityContract, "forwardBizParams", true)) {
            return new LinkedHashMap<>();
        }
        JsonNode bizParams = requestSnapshot == null ? null : requestSnapshot.get("bizParams");
        if (bizParams == null || bizParams.isNull() || !bizParams.isObject()) {
            return new LinkedHashMap<>();
        }
        return JSON_MAPPER.convertValue(bizParams, MAP_TYPE);
    }

    private boolean isClassicCaseRequest(JsonNode requestSnapshot) {
        return "CLASSIC_CASE".equals(nestedTextValue(
                requestSnapshot, "bizParams", "generationSource"));
    }

    /**
     * 读取能力契约中的布尔配置，缺失或类型不正确时保持兼容默认值。
     *
     * @param node 能力请求契约。
     * @param fieldName 配置字段名。
     * @param defaultValue 兼容默认值。
     * @return 解析后的布尔值。
     */
    private boolean booleanValue(JsonNode node, String fieldName, boolean defaultValue) {
        if (node == null || !node.has(fieldName) || !node.get(fieldName).isBoolean()) {
            return defaultValue;
        }
        return node.get(fieldName).asBoolean();
    }

    /**
     * 解析链路追踪 ID。
     *
     * @param request 内部批量创建请求。
     * @return 追踪 ID。
     */
    private String resolveTraceId(BatchCreateRequest request) {
        JsonNode requestSnapshot = parseOptionalJson(request.getRequestJson());
        return firstText(textValue(requestSnapshot, "traceId"), request.getRequestBatchId());
    }

    /**
     * 解析可选 JSON 文本。
     *
     * @param json JSON 文本。
     * @return JSON 节点；空文本时返回 null。
     */
    private JsonNode parseOptionalJson(String json) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        try {
            return JSON_MAPPER.readTree(json);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR.getCode(), "数据准备请求快照不是合法 JSON");
        }
    }

    /**
     * 读取嵌套对象中的字符串字段。
     *
     * @param node JSON 节点。
     * @param parentField 父字段名。
     * @param childField 子字段名。
     * @return 字符串字段。
     */
    private String nestedTextValue(JsonNode node, String parentField, String childField) {
        if (node == null || !node.has(parentField) || node.get(parentField).isNull()) {
            return null;
        }
        return textValue(node.get(parentField), childField);
    }

    /**
     * 序列化原平台响应摘要。
     *
     * @param value 原平台响应对象。
     * @return JSON 字符串。
     */
    private String toJson(Object value) {
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
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

    /**
     * 第三方造数请求 DTO。
     *
     * 业务功能：严格匹配《第三方造数接口对接文档》的最小请求字段。
     */
    private static class OriginBatchCreateRequest {

        private String businessModuleCode;

        private String templateCode;

        private String initState;

        private String sceneType;

        private List<OriginParticipant> participants;

        private Map<String, Object> bizParams;

        public String getBusinessModuleCode() {
            return businessModuleCode;
        }

        public void setBusinessModuleCode(String businessModuleCode) {
            this.businessModuleCode = businessModuleCode;
        }

        public String getTemplateCode() {
            return templateCode;
        }

        public void setTemplateCode(String templateCode) {
            this.templateCode = templateCode;
        }

        public String getInitState() {
            return initState;
        }

        public void setInitState(String initState) {
            this.initState = initState;
        }

        public String getSceneType() {
            return sceneType;
        }

        public void setSceneType(String sceneType) {
            this.sceneType = sceneType;
        }

        public List<OriginParticipant> getParticipants() {
            return participants;
        }

        public void setParticipants(List<OriginParticipant> participants) {
            this.participants = participants;
        }

        public Map<String, Object> getBizParams() {
            return bizParams;
        }

        public void setBizParams(Map<String, Object> bizParams) {
            this.bizParams = bizParams;
        }
    }

    /**
     * 第三方造数参与方 DTO。
     *
     * 业务功能：只传稳定主体标识，不传姓名、角色名等展示字段。
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class OriginParticipant {

        private String participantId;

        private String ownerUserId;

        private String externalOrgId;

        private String poolKey;

        public String getParticipantId() {
            return participantId;
        }

        public void setParticipantId(String participantId) {
            this.participantId = participantId;
        }

        public String getOwnerUserId() {
            return ownerUserId;
        }

        public void setOwnerUserId(String ownerUserId) {
            this.ownerUserId = ownerUserId;
        }

        public String getExternalOrgId() {
            return externalOrgId;
        }

        public void setExternalOrgId(String externalOrgId) {
            this.externalOrgId = externalOrgId;
        }

        public String getPoolKey() {
            return poolKey;
        }

        public void setPoolKey(String poolKey) {
            this.poolKey = poolKey;
        }
    }

    /**
     * 第三方造数响应 DTO。
     *
     * 业务功能：承接文档中的 result 对象，并由 Adapter 转换为内部响应。
     */
    private static class OriginBatchCreateResponse {

        private String originBatchId;

        private String status;

        private Long totalCount;

        private Long successCount;

        private Long failedCount;

        private List<OriginResponseItem> items;

        private List<OriginFailedItem> failedItems;

        public String getOriginBatchId() {
            return originBatchId;
        }

        public void setOriginBatchId(String originBatchId) {
            this.originBatchId = originBatchId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public Long getTotalCount() {
            return totalCount;
        }

        public void setTotalCount(Long totalCount) {
            this.totalCount = totalCount;
        }

        public Long getSuccessCount() {
            return successCount;
        }

        public void setSuccessCount(Long successCount) {
            this.successCount = successCount;
        }

        public Long getFailedCount() {
            return failedCount;
        }

        public void setFailedCount(Long failedCount) {
            this.failedCount = failedCount;
        }

        public List<OriginResponseItem> getItems() {
            return items;
        }

        public void setItems(List<OriginResponseItem> items) {
            this.items = items;
        }

        public List<OriginFailedItem> getFailedItems() {
            return failedItems;
        }

        public void setFailedItems(List<OriginFailedItem> failedItems) {
            this.failedItems = failedItems;
        }
    }

    /**
     * 第三方造数成功明细 DTO。
     */
    private static class OriginResponseItem {

        private String participantId;

        private String ownerUserId;

        private String externalDataId;

        private String externalBizNo;

        private String externalStatus;

        private String entryUrl;

        private String externalUserId;

        private String externalOrgId;

        private JsonNode rawData;

        public String getParticipantId() {
            return participantId;
        }

        public void setParticipantId(String participantId) {
            this.participantId = participantId;
        }

        public String getOwnerUserId() {
            return ownerUserId;
        }

        public void setOwnerUserId(String ownerUserId) {
            this.ownerUserId = ownerUserId;
        }

        public String getExternalDataId() {
            return externalDataId;
        }

        public void setExternalDataId(String externalDataId) {
            this.externalDataId = externalDataId;
        }

        public String getExternalBizNo() {
            return externalBizNo;
        }

        public void setExternalBizNo(String externalBizNo) {
            this.externalBizNo = externalBizNo;
        }

        public String getExternalStatus() {
            return externalStatus;
        }

        public void setExternalStatus(String externalStatus) {
            this.externalStatus = externalStatus;
        }

        public String getEntryUrl() {
            return entryUrl;
        }

        public void setEntryUrl(String entryUrl) {
            this.entryUrl = entryUrl;
        }

        public String getExternalUserId() {
            return externalUserId;
        }

        public void setExternalUserId(String externalUserId) {
            this.externalUserId = externalUserId;
        }

        public String getExternalOrgId() {
            return externalOrgId;
        }

        public void setExternalOrgId(String externalOrgId) {
            this.externalOrgId = externalOrgId;
        }

        public JsonNode getRawData() {
            return rawData;
        }

        public void setRawData(JsonNode rawData) {
            this.rawData = rawData;
        }
    }

    /**
     * 第三方造数失败明细 DTO。
     */
    private static class OriginFailedItem {

        private String participantId;

        private String ownerUserId;

        private String errorCode;

        private String errorMessage;

        public String getParticipantId() {
            return participantId;
        }

        public void setParticipantId(String participantId) {
            this.participantId = participantId;
        }

        public String getOwnerUserId() {
            return ownerUserId;
        }

        public void setOwnerUserId(String ownerUserId) {
            this.ownerUserId = ownerUserId;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
