package com.sxpt.module.connector.service;

import java.util.List;

/**
 * 原平台数据准备适配器。
 *
 * 业务功能：
 * 1. 定义教学平台调用原平台创建、查询、校验、锁定和归档教学用业务数据的内部契约。
 * 2. 隔离核心数据准备服务与具体 HTTP、RPC 或 mock 实现，避免业务编排直接依赖外部接口 DTO。
 * 3. 明确原平台数据仍由原业务系统落库，教学平台只保存外部业务标识、状态、目标地址和校验结果。
 *
 * 关键流程：
 * 1. DataPrepareJobService 后续编排时按 requestBatchId 调用 createTeachingData。
 * 2. 原平台按 requestItemId 逐条返回外部业务标识、业务名称、当前状态、当前步骤、当前参与方和 targetUrl。
 * 3. 教学平台调用 validateTeachingData 确认可见性、操作单位、角色、状态和动作是否满足需求。
 * 4. 考试或强约束场景调用 lockTeachingData，完成后按策略调用 archiveTeachingData。
 */
public interface OriginDataPrepareAdapter {

    /**
     * 批量创建教学用原平台业务数据。
     *
     * @param request 批量创建请求。
     * @return 批量创建响应。
     */
    BatchCreateResponse createTeachingData(BatchCreateRequest request);

    /**
     * 查询原平台业务数据摘要。
     *
     * @param request 查询请求。
     * @return 查询响应。
     */
    QueryResponse queryTeachingData(QueryRequest request);

    /**
     * 校验原平台业务数据是否满足教学约束。
     *
     * @param request 校验请求。
     * @return 校验响应。
     */
    ValidationResponse validateTeachingData(ValidationRequest request);

    /**
     * 锁定原平台业务数据。
     *
     * @param request 锁定请求。
     * @return 锁定响应。
     */
    LockResponse lockTeachingData(LockRequest request);

    /**
     * 归档原平台业务数据。
     *
     * @param request 归档请求。
     * @return 归档响应。
     */
    ArchiveResponse archiveTeachingData(ArchiveRequest request);

    /**
     * 批量创建请求。
     *
     * 业务功能：
     * 1. 表达一次数据准备任务向原平台提出的批量造数需求。
     * 2. 使用 requestBatchId 和 idempotencyKey 保证原平台侧可对账、可去重。
     */
    class BatchCreateRequest {

        private String tenantId;

        private String connectorSystemId;

        private String moduleCode;

        private String sceneType;

        private String requestBatchId;

        private String idempotencyKey;

        private String requestJson;

        private List<RequestItem> items;

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

        public List<RequestItem> getItems() {
            return items;
        }

        public void setItems(List<RequestItem> items) {
            this.items = items;
        }
    }

    /**
     * 逐条创建请求项。
     *
     * 业务功能：
     * 1. 表达一个学生、题目、单位、角色、协作片段对应的一条原平台数据需求。
     * 2. requestItemId 是逐条回填结果的最小幂等和对账键。
     */
    class RequestItem {

        private String requestItemId;

        private String studentId;

        private String questionId;

        private String requiredExternalOrgId;

        private String requiredExternalRoleId;

        private String actorType;

        private String dataScopeJson;

        private String requiredActionsJson;

        public String getRequestItemId() {
            return requestItemId;
        }

        public void setRequestItemId(String requestItemId) {
            this.requestItemId = requestItemId;
        }

        public String getStudentId() {
            return studentId;
        }

        public void setStudentId(String studentId) {
            this.studentId = studentId;
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

        public String getDataScopeJson() {
            return dataScopeJson;
        }

        public void setDataScopeJson(String dataScopeJson) {
            this.dataScopeJson = dataScopeJson;
        }

        public String getRequiredActionsJson() {
            return requiredActionsJson;
        }

        public void setRequiredActionsJson(String requiredActionsJson) {
            this.requiredActionsJson = requiredActionsJson;
        }
    }

    /**
     * 批量创建响应。
     *
     * 业务功能：
     * 1. 表达原平台对一次批量创建请求的整体处理结果。
     * 2. items 按 requestItemId 返回逐条创建结果，教学平台据此回填需求明细。
     */
    class BatchCreateResponse {

        private String externalRequestId;

        private String requestBatchId;

        private String adapterStatus;

        private String resultJson;

        private List<ResponseItem> items;

        public String getExternalRequestId() {
            return externalRequestId;
        }

        public void setExternalRequestId(String externalRequestId) {
            this.externalRequestId = externalRequestId;
        }

        public String getRequestBatchId() {
            return requestBatchId;
        }

        public void setRequestBatchId(String requestBatchId) {
            this.requestBatchId = requestBatchId;
        }

        public String getAdapterStatus() {
            return adapterStatus;
        }

        public void setAdapterStatus(String adapterStatus) {
            this.adapterStatus = adapterStatus;
        }

        public String getResultJson() {
            return resultJson;
        }

        public void setResultJson(String resultJson) {
            this.resultJson = resultJson;
        }

        public List<ResponseItem> getItems() {
            return items;
        }

        public void setItems(List<ResponseItem> items) {
            this.items = items;
        }
    }

    /**
     * 逐条创建响应项。
     *
     * 业务功能：
     * 1. 返回原平台已经落库的数据引用。
     * 2. 返回当前步骤和当前参与方身份，供教学平台优先保存真实办理上下文。
     * 3. 返回 targetUrl 供 launchToken 后续进入原平台具体页面。
     */
    class ResponseItem {

        private String requestItemId;

        private String externalBusinessId;

        private String externalBusinessNo;

        private String externalBusinessName;

        private String externalStatus;

        private String targetUrl;

        private String currentStepCode;

        private Integer currentActorNo;

        private String currentOrgId;

        private String currentOrgName;

        private String currentRoleId;

        private String currentRoleName;

        private String processChainJson;

        private String itemStatus;

        private String errorMessage;

        public String getRequestItemId() {
            return requestItemId;
        }

        public void setRequestItemId(String requestItemId) {
            this.requestItemId = requestItemId;
        }

        public String getExternalBusinessId() {
            return externalBusinessId;
        }

        public void setExternalBusinessId(String externalBusinessId) {
            this.externalBusinessId = externalBusinessId;
        }

        public String getExternalBusinessNo() {
            return externalBusinessNo;
        }

        public void setExternalBusinessNo(String externalBusinessNo) {
            this.externalBusinessNo = externalBusinessNo;
        }

        public String getExternalBusinessName() {
            return externalBusinessName;
        }

        public void setExternalBusinessName(String externalBusinessName) {
            this.externalBusinessName = externalBusinessName;
        }

        public String getExternalStatus() {
            return externalStatus;
        }

        public void setExternalStatus(String externalStatus) {
            this.externalStatus = externalStatus;
        }

        public String getTargetUrl() {
            return targetUrl;
        }

        public void setTargetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
        }

        public String getCurrentStepCode() {
            return currentStepCode;
        }

        public void setCurrentStepCode(String currentStepCode) {
            this.currentStepCode = currentStepCode;
        }

        public Integer getCurrentActorNo() {
            return currentActorNo;
        }

        public void setCurrentActorNo(Integer currentActorNo) {
            this.currentActorNo = currentActorNo;
        }

        public String getCurrentOrgId() {
            return currentOrgId;
        }

        public void setCurrentOrgId(String currentOrgId) {
            this.currentOrgId = currentOrgId;
        }

        public String getCurrentOrgName() {
            return currentOrgName;
        }

        public void setCurrentOrgName(String currentOrgName) {
            this.currentOrgName = currentOrgName;
        }

        public String getCurrentRoleId() {
            return currentRoleId;
        }

        public void setCurrentRoleId(String currentRoleId) {
            this.currentRoleId = currentRoleId;
        }

        public String getCurrentRoleName() {
            return currentRoleName;
        }

        public void setCurrentRoleName(String currentRoleName) {
            this.currentRoleName = currentRoleName;
        }

        public String getProcessChainJson() {
            return processChainJson;
        }

        public void setProcessChainJson(String processChainJson) {
            this.processChainJson = processChainJson;
        }

        public String getItemStatus() {
            return itemStatus;
        }

        public void setItemStatus(String itemStatus) {
            this.itemStatus = itemStatus;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * 查询请求。
     *
     * 业务功能：
     * 1. 查询原平台指定业务数据的当前摘要。
     * 2. 支撑重试、审计和进入原平台前的状态刷新。
     */
    class QueryRequest extends BusinessDataReference {
    }

    /**
     * 查询响应。
     *
     * 业务功能：
     * 1. 返回原平台业务数据当前状态和摘要。
     * 2. 教学平台只保存引用和摘要，不接管原平台真实业务数据。
     */
    class QueryResponse extends BusinessDataReference {

        private String externalStatus;

        private String targetUrl;

        private String metadataJson;

        public String getExternalStatus() {
            return externalStatus;
        }

        public void setExternalStatus(String externalStatus) {
            this.externalStatus = externalStatus;
        }

        public String getTargetUrl() {
            return targetUrl;
        }

        public void setTargetUrl(String targetUrl) {
            this.targetUrl = targetUrl;
        }

        public String getMetadataJson() {
            return metadataJson;
        }

        public void setMetadataJson(String metadataJson) {
            this.metadataJson = metadataJson;
        }
    }

    /**
     * 校验请求。
     *
     * 业务功能：
     * 1. 校验指定原平台业务数据是否满足单位、角色、状态和动作约束。
     * 2. 校验失败的数据不得生成可用 launchToken。
     */
    class ValidationRequest extends BusinessDataReference {

        private String requiredExternalOrgId;

        private String requiredExternalRoleId;

        private String requiredActionsJson;

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

        public String getRequiredActionsJson() {
            return requiredActionsJson;
        }

        public void setRequiredActionsJson(String requiredActionsJson) {
            this.requiredActionsJson = requiredActionsJson;
        }
    }

    /**
     * 校验响应。
     *
     * 业务功能：
     * 1. 明确原平台数据是否可见、可操作、状态匹配。
     * 2. 保存校验详情，支撑后台解释为什么某条数据不能进入 READY。
     */
    class ValidationResponse {

        private boolean passed;

        private String validationStatus;

        private String validationResultJson;

        private String errorMessage;

        public boolean isPassed() {
            return passed;
        }

        public void setPassed(boolean passed) {
            this.passed = passed;
        }

        public String getValidationStatus() {
            return validationStatus;
        }

        public void setValidationStatus(String validationStatus) {
            this.validationStatus = validationStatus;
        }

        public String getValidationResultJson() {
            return validationResultJson;
        }

        public void setValidationResultJson(String validationResultJson) {
            this.validationResultJson = validationResultJson;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * 锁定请求。
     *
     * 业务功能：
     * 1. 考试或强约束流程开始前锁定原平台业务数据。
     * 2. 防止数据被重置、复用或被其他教学流程修改。
     */
    class LockRequest extends BusinessDataReference {
    }

    /**
     * 锁定响应。
     *
     * 业务功能：
     * 1. 返回锁定是否成功及锁定后的原平台状态。
     * 2. 教学平台据此更新实例锁定状态。
     */
    class LockResponse {

        private boolean locked;

        private String externalStatus;

        private String resultJson;

        private String errorMessage;

        public boolean isLocked() {
            return locked;
        }

        public void setLocked(boolean locked) {
            this.locked = locked;
        }

        public String getExternalStatus() {
            return externalStatus;
        }

        public void setExternalStatus(String externalStatus) {
            this.externalStatus = externalStatus;
        }

        public String getResultJson() {
            return resultJson;
        }

        public void setResultJson(String resultJson) {
            this.resultJson = resultJson;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * 归档请求。
     *
     * 业务功能：
     * 1. 教学流程完成后通知原平台归档教学用业务数据。
     * 2. 原平台仍负责真实业务数据归档，教学平台只记录归档结果。
     */
    class ArchiveRequest extends BusinessDataReference {
    }

    /**
     * 归档响应。
     *
     * 业务功能：
     * 1. 返回归档是否成功及归档结果摘要。
     * 2. 支撑归档失败重试和后台审计。
     */
    class ArchiveResponse {

        private boolean archived;

        private String resultJson;

        private String errorMessage;

        public boolean isArchived() {
            return archived;
        }

        public void setArchived(boolean archived) {
            this.archived = archived;
        }

        public String getResultJson() {
            return resultJson;
        }

        public void setResultJson(String resultJson) {
            this.resultJson = resultJson;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * 原平台业务数据引用。
     *
     * 业务功能：
     * 1. 统一表达后续查询、校验、锁定和归档都必须携带的定位字段。
     * 2. 避免各请求类型重复定义外部业务数据定位字段。
     */
    class BusinessDataReference {

        private String tenantId;

        private String connectorSystemId;

        private String moduleCode;

        private String externalBusinessId;

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

        public String getExternalBusinessId() {
            return externalBusinessId;
        }

        public void setExternalBusinessId(String externalBusinessId) {
            this.externalBusinessId = externalBusinessId;
        }
    }
}
