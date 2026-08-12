package com.sxpt.module.connector.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.ClassicCaseRuntimeConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一 DATA_CREATE 请求构建服务。
 *
 * 业务功能：
 * 1. 将普通造数、经典案例还原、经典案例 demo 等运行时上下文收敛为同一种 Adapter 请求。
 * 2. 固化经典案例只能通过 bizParams 扩展传给第三方学习环境，避免后续新增顶层字段破坏已接入系统。
 *
 * 关键流程：
 * 1. 业务服务只传入平台内部上下文和已脱敏案例版本内容。
 * 2. 本服务统一生成 requestJson、幂等键、参与方明细和 Adapter 可识别的 BatchCreateRequest。
 */
@Service
public class DataCreateRequestBuildService {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * 构建经典案例 DATA_CREATE 请求。
     *
     * 业务功能：
     * 1. 教师/专家备案教学时生成 REPLAY_CASE 请求，携带 desensitizedCasePayload。
     * 2. 学生练习/考试时生成 FORMAT_DEMO 请求，只携带 caseDataFormat。
     *
     * @param context 经典案例造数上下文。
     * @return Adapter 标准批量创建请求。
     */
    public OriginDataPrepareAdapter.BatchCreateRequest buildClassicCaseRequest(ClassicCaseCreateContext context) {
        validateClassicCaseContext(context);
        OriginDataPrepareAdapter.BatchCreateRequest request = new OriginDataPrepareAdapter.BatchCreateRequest();
        request.setTenantId(context.getTenantId());
        request.setConnectorSystemId(context.getConnectorSystemId());
        request.setModuleCode(context.getModuleCode());
        request.setTemplateId(context.getTemplateId());
        request.setInitState(context.getInitState());
        request.setSceneType(context.getSceneType());
        request.setRequestBatchId(context.getRequestBatchId());
        request.setIdempotencyKey(context.getIdempotencyKey());
        request.setRequestJson(buildClassicCaseRequestJson(context));
        request.setTraceId(context.getTraceId());
        request.setItems(buildRequestItems(context.getItems()));
        return request;
    }

    /**
     * 构建普通 DATA_CREATE 请求。
     *
     * 业务功能：
     * 1. 将批次准备链路提交给第三方学习环境的请求统一收口到同一个构建器。
     * 2. 保持历史 requestJson 和 item 字段原样透传，避免已接入的第三方系统协议变化。
     *
     * 关键流程：
     * 1. 校验批次、模板、场景和参与方明细的最小必填项。
     * 2. 写入 Adapter 识别的顶层字段。
     * 3. 复用统一 item 转换逻辑，保留单位、角色、动作和数据范围约束。
     *
     * @param context 普通造数请求上下文。
     * @return Adapter 标准批量创建请求。
     */
    public OriginDataPrepareAdapter.BatchCreateRequest buildNormalRequest(NormalCreateContext context) {
        validateNormalContext(context);
        OriginDataPrepareAdapter.BatchCreateRequest request = new OriginDataPrepareAdapter.BatchCreateRequest();
        request.setTenantId(context.getTenantId());
        request.setConnectorSystemId(context.getConnectorSystemId());
        request.setModuleCode(context.getModuleCode());
        request.setTemplateId(context.getTemplateId());
        request.setInitState(context.getInitState());
        request.setSceneType(context.getSceneType());
        request.setRequestBatchId(context.getRequestBatchId());
        request.setIdempotencyKey(context.getIdempotencyKey());
        request.setRequestJson(context.getRequestJson());
        request.setTraceId(context.getTraceId());
        request.setItems(buildRequestItems(context.getItems()));
        return request;
    }

    /**
     * 构造经典案例请求快照。
     *
     * @param context 经典案例造数上下文。
     * @return 用于 Adapter 透传 bizParams 和平台侧审计追踪的 JSON 快照。
     */
    private String buildClassicCaseRequestJson(ClassicCaseCreateContext context) {
        ObjectNode root = JSON_MAPPER.createObjectNode();
        root.put("requestMode", ClassicCaseRuntimeConstants.REQUEST_MODE_CLASSIC_CASE);
        root.put("templateCode", context.getTemplateCode());
        root.put("initState", context.getInitState());
        root.put("usageScene", context.getUsageScene());
        root.put("sceneType", context.getSceneType());
        root.put("classicCaseAssetId", context.getCaseAssetId());
        root.put("classicCaseVersionId", context.getCaseVersionId());
        root.put("payloadSchemaVersion", context.getPayloadSchemaVersion());
        root.put("traceId", context.getTraceId());
        ObjectNode template = root.putObject("template");
        template.put("id", context.getTemplateId());
        template.put("code", context.getTemplateCode());
        template.put("initState", context.getInitState());
        root.set("identityBinding", parseJsonNode(context.getIdentityBindingJson()));
        if (StringUtils.hasText(context.getParticipantContextJson())) {
            root.set("participantContext", parseJsonNode(context.getParticipantContextJson()));
        }
        root.set("resolvedActors", buildResolvedActors(context.getItems()));

        ObjectNode bizParams = root.putObject("bizParams");
        bizParams.put("generationSource", ClassicCaseRuntimeConstants.GENERATION_SOURCE_CLASSIC_CASE);
        bizParams.put("generationMode", resolveGenerationMode(context.getUsageScene()));
        ObjectNode classicCase = bizParams.putObject("classicCase");
        classicCase.put("caseCode", context.getCaseCode());
        classicCase.put("caseVersionId", context.getCaseVersionId());
        classicCase.put("payloadSchemaVersion", context.getPayloadSchemaVersion());
        classicCase.set("identityBinding", parseJsonNode(context.getIdentityBindingJson()));

        if (isReplayCase(context.getUsageScene())) {
            JsonNode desensitizedCasePayload = parseJsonNode(context.getDesensitizedCasePayloadJson());
            root.set("desensitizedCasePayload", desensitizedCasePayload);
            classicCase.set("desensitizedCasePayload", desensitizedCasePayload);
        } else {
            JsonNode caseDataFormat = parseJsonNode(context.getCaseDataFormatJson());
            root.set("caseDataFormat", caseDataFormat);
            classicCase.set("caseDataFormat", caseDataFormat);
        }
        return root.toString();
    }

    /**
     * 构造第三方学习环境参与方明细。
     *
     * @param items 平台内部运行时参与方。
     * @return Adapter 标准参与方明细。
     */
    /**
     * 构建经典案例参与方解析快照。
     *
     * 业务功能：
     * 1. 将本次生成最终使用的单位、角色、参与方类型写入平台内部 requestJson。
     * 2. 不写入 bizParams，避免第三方学习平台收到额外字段后产生兼容风险。
     *
     * @param items 本次生成的参与方明细。
     * @return 平台内部审计用参与方快照。
     */
    private ArrayNode buildResolvedActors(List<DataCreateRequestItem> items) {
        ArrayNode resolvedActors = JSON_MAPPER.createArrayNode();
        for (DataCreateRequestItem item : items) {
            ObjectNode actor = resolvedActors.addObject();
            actor.put("requestItemId", item.getRequestItemId());
            putTextIfPresent(actor, "ownerUserId", item.getOwnerUserId());
            putTextIfPresent(actor, "questionId", item.getQuestionId());
            putTextIfPresent(actor, "requiredExternalOrgId", item.getRequiredExternalOrgId());
            putTextIfPresent(actor, "requiredExternalRoleId", item.getRequiredExternalRoleId());
            putTextIfPresent(actor, "actorType", item.getActorType());
        }
        return resolvedActors;
    }

    /**
     * 只写入有值文本，避免审计快照充满空字段。
     *
     * @param node 目标 JSON 对象。
     * @param fieldName 字段名。
     * @param value 字段值。
     */
    private void putTextIfPresent(ObjectNode node, String fieldName, String value) {
        if (StringUtils.hasText(value)) {
            node.put(fieldName, value.trim());
        }
    }

    private List<OriginDataPrepareAdapter.RequestItem> buildRequestItems(List<DataCreateRequestItem> items) {
        List<OriginDataPrepareAdapter.RequestItem> requestItems = new ArrayList<>();
        for (DataCreateRequestItem source : items) {
            OriginDataPrepareAdapter.RequestItem item = new OriginDataPrepareAdapter.RequestItem();
            item.setRequestItemId(source.getRequestItemId());
            item.setStudentId(source.getOwnerUserId());
            item.setQuestionId(trimToNull(source.getQuestionId()));
            item.setRequiredExternalOrgId(trimToNull(source.getRequiredExternalOrgId()));
            item.setRequiredExternalRoleId(trimToNull(source.getRequiredExternalRoleId()));
            item.setActorType(trimToNull(source.getActorType()));
            item.setDataScopeJson(trimToNull(source.getParticipantContextJson()));
            item.setRequiredActionsJson(trimToNull(source.getRequiredActionsJson()));
            requestItems.add(item);
        }
        return requestItems;
    }

    /**
     * 校验经典案例请求构建所需的最小上下文。
     *
     * @param context 经典案例造数上下文。
     */
    private void validateClassicCaseContext(ClassicCaseCreateContext context) {
        if (context == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(context.getTenantId());
        requireText(context.getConnectorSystemId());
        requireText(context.getModuleCode());
        requireText(context.getTemplateId());
        requireText(context.getTemplateCode());
        requireText(context.getInitState());
        requireText(context.getSceneType());
        requireText(context.getRequestBatchId());
        requireText(context.getIdempotencyKey());
        requireText(context.getUsageScene());
        requireText(context.getCaseAssetId());
        requireText(context.getCaseCode());
        requireText(context.getCaseVersionId());
        requireText(context.getPayloadSchemaVersion());
        requireText(context.getTraceId());
        requireText(context.getIdentityBindingJson());
        if (isReplayCase(context.getUsageScene())) {
            requireText(context.getDesensitizedCasePayloadJson());
        } else {
            requireText(context.getCaseDataFormatJson());
        }
        if (context.getItems() == null || context.getItems().isEmpty()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        for (DataCreateRequestItem item : context.getItems()) {
            if (item == null) {
                throw new BusinessException(ApiResultCode.PARAM_ERROR);
            }
            requireText(item.getRequestItemId());
        }
    }

    /**
     * 校验普通造数请求构建所需的最小上下文。
     *
     * @param context 普通造数请求上下文。
     */
    private void validateNormalContext(NormalCreateContext context) {
        if (context == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(context.getTenantId());
        requireText(context.getConnectorSystemId());
        requireText(context.getModuleCode());
        requireText(context.getSceneType());
        requireText(context.getRequestBatchId());
        requireText(context.getIdempotencyKey());
        if (context.getItems() == null || context.getItems().isEmpty()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        for (DataCreateRequestItem item : context.getItems()) {
            if (item == null) {
                throw new BusinessException(ApiResultCode.PARAM_ERROR);
            }
            requireText(item.getRequestItemId());
            requireText(item.getOwnerUserId());
        }
    }

    /**
     * 将教学平台内部使用场景翻译为第三方造数协议中的经典案例生成模式。
     *
     * @param usageScene 教学平台内部经典案例使用场景。
     * @return 第三方学习环境可以直接识别的生成模式。
     */
    private String resolveGenerationMode(String usageScene) {
        if (isReplayCase(usageScene)) {
            return ClassicCaseRuntimeConstants.GENERATION_MODE_REPLAY_CASE;
        }
        return ClassicCaseRuntimeConstants.GENERATION_MODE_FORMAT_DEMO;
    }

    /**
     * 判断当前经典案例是否需要按脱敏内容还原。
     *
     * @param usageScene 教学平台内部使用场景。
     * @return true 表示教师/专家备案教学还原；false 表示学生 demo。
     */
    private boolean isReplayCase(String usageScene) {
        return ClassicCaseRuntimeConstants.USAGE_SCENE_TEACHING_REPLICA.equals(usageScene.trim());
    }

    /**
     * 解析对象 JSON，保证经典案例内容以对象结构进入 requestJson，而不是二次转义字符串。
     *
     * @param json JSON 文本。
     * @return JSON 对象节点。
     */
    private JsonNode parseJsonNode(String json) {
        try {
            JsonNode node = JSON_MAPPER.readTree(json);
            if (node == null || !node.isObject()) {
                throw new BusinessException(ApiResultCode.PARAM_ERROR);
            }
            return node;
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 经典案例 DATA_CREATE 构建上下文。
     */
    public static class ClassicCaseCreateContext {

        private String tenantId;

        private String connectorSystemId;

        private String moduleCode;

        private String templateId;

        private String templateCode;

        private String initState;

        private String sceneType;

        private String requestBatchId;

        private String idempotencyKey;

        private String traceId;

        private String usageScene;

        private String caseAssetId;

        private String caseCode;

        private String caseVersionId;

        private String payloadSchemaVersion;

        private String desensitizedCasePayloadJson;

        private String caseDataFormatJson;

        private String identityBindingJson;

        private String participantContextJson;

        private List<DataCreateRequestItem> items;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getConnectorSystemId() {
            return connectorSystemId;
        }

        public void setConnectorSystemId(String connectorSystemId) {
            this.connectorSystemId = connectorSystemId;
        }

        public String getModuleCode() {
            return moduleCode;
        }

        public void setModuleCode(String moduleCode) {
            this.moduleCode = moduleCode;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
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

        public String getRequestBatchId() {
            return requestBatchId;
        }

        public void setRequestBatchId(String requestBatchId) {
            this.requestBatchId = requestBatchId;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public String getUsageScene() {
            return usageScene;
        }

        public void setUsageScene(String usageScene) {
            this.usageScene = usageScene;
        }

        public String getCaseAssetId() {
            return caseAssetId;
        }

        public void setCaseAssetId(String caseAssetId) {
            this.caseAssetId = caseAssetId;
        }

        public String getCaseCode() {
            return caseCode;
        }

        public void setCaseCode(String caseCode) {
            this.caseCode = caseCode;
        }

        public String getCaseVersionId() {
            return caseVersionId;
        }

        public void setCaseVersionId(String caseVersionId) {
            this.caseVersionId = caseVersionId;
        }

        public String getPayloadSchemaVersion() {
            return payloadSchemaVersion;
        }

        public void setPayloadSchemaVersion(String payloadSchemaVersion) {
            this.payloadSchemaVersion = payloadSchemaVersion;
        }

        public String getDesensitizedCasePayloadJson() {
            return desensitizedCasePayloadJson;
        }

        public void setDesensitizedCasePayloadJson(String desensitizedCasePayloadJson) {
            this.desensitizedCasePayloadJson = desensitizedCasePayloadJson;
        }

        public String getCaseDataFormatJson() {
            return caseDataFormatJson;
        }

        public void setCaseDataFormatJson(String caseDataFormatJson) {
            this.caseDataFormatJson = caseDataFormatJson;
        }

        public String getIdentityBindingJson() {
            return identityBindingJson;
        }

        public void setIdentityBindingJson(String identityBindingJson) {
            this.identityBindingJson = identityBindingJson;
        }

        public String getParticipantContextJson() {
            return participantContextJson;
        }

        public void setParticipantContextJson(String participantContextJson) {
            this.participantContextJson = participantContextJson;
        }

        public List<DataCreateRequestItem> getItems() {
            return items;
        }

        public void setItems(List<DataCreateRequestItem> items) {
            this.items = items;
        }
    }

    /**
     * 普通 DATA_CREATE 构建上下文。
     */
    public static class NormalCreateContext {

        private String tenantId;

        private String connectorSystemId;

        private String moduleCode;

        private String templateId;

        private String initState;

        private String sceneType;

        private String requestBatchId;

        private String idempotencyKey;

        private String requestJson;

        private String traceId;

        private List<DataCreateRequestItem> items;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getConnectorSystemId() {
            return connectorSystemId;
        }

        public void setConnectorSystemId(String connectorSystemId) {
            this.connectorSystemId = connectorSystemId;
        }

        public String getModuleCode() {
            return moduleCode;
        }

        public void setModuleCode(String moduleCode) {
            this.moduleCode = moduleCode;
        }

        public String getTemplateId() {
            return templateId;
        }

        public void setTemplateId(String templateId) {
            this.templateId = templateId;
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

        public String getRequestBatchId() {
            return requestBatchId;
        }

        public void setRequestBatchId(String requestBatchId) {
            this.requestBatchId = requestBatchId;
        }

        public String getIdempotencyKey() {
            return idempotencyKey;
        }

        public void setIdempotencyKey(String idempotencyKey) {
            this.idempotencyKey = idempotencyKey;
        }

        public String getRequestJson() {
            return requestJson;
        }

        public void setRequestJson(String requestJson) {
            this.requestJson = requestJson;
        }

        public String getTraceId() {
            return traceId;
        }

        public void setTraceId(String traceId) {
            this.traceId = traceId;
        }

        public List<DataCreateRequestItem> getItems() {
            return items;
        }

        public void setItems(List<DataCreateRequestItem> items) {
            this.items = items;
        }
    }

    /**
     * DATA_CREATE 参与方构建上下文。
     */
    public static class DataCreateRequestItem {

        private String requestItemId;

        private String ownerUserId;

        private String questionId;

        private String requiredExternalOrgId;

        private String requiredExternalRoleId;

        private String actorType;

        private String participantContextJson;

        private String requiredActionsJson;

        public String getRequestItemId() {
            return requestItemId;
        }

        public void setRequestItemId(String requestItemId) {
            this.requestItemId = requestItemId;
        }

        public String getOwnerUserId() {
            return ownerUserId;
        }

        public void setOwnerUserId(String ownerUserId) {
            this.ownerUserId = ownerUserId;
        }

        public String getQuestionId() {
            return questionId;
        }

        public void setQuestionId(String questionId) {
            this.questionId = questionId;
        }

        public String getRequiredExternalOrgId() {
            return requiredExternalOrgId;
        }

        public void setRequiredExternalOrgId(String requiredExternalOrgId) {
            this.requiredExternalOrgId = requiredExternalOrgId;
        }

        public String getRequiredExternalRoleId() {
            return requiredExternalRoleId;
        }

        public void setRequiredExternalRoleId(String requiredExternalRoleId) {
            this.requiredExternalRoleId = requiredExternalRoleId;
        }

        public String getActorType() {
            return actorType;
        }

        public void setActorType(String actorType) {
            this.actorType = actorType;
        }

        public String getParticipantContextJson() {
            return participantContextJson;
        }

        public void setParticipantContextJson(String participantContextJson) {
            this.participantContextJson = participantContextJson;
        }

        public String getRequiredActionsJson() {
            return requiredActionsJson;
        }

        public void setRequiredActionsJson(String requiredActionsJson) {
            this.requiredActionsJson = requiredActionsJson;
        }
    }
}
