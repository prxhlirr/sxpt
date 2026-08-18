package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 经典案例导入请求。
 *
 * 业务功能：
 * 1. 承载原平台正式环境推送到教学平台的完全脱敏案例内容。
 * 2. 明确区分老师/专家复刻使用的 desensitizedCasePayloadJson 和学生练习使用的 caseDataFormatJson。
 *
 * 关键流程：
 * 1. 原平台正式环境完成脱敏后组装本请求。
 * 2. 教学平台校验正式环境、学习环境、业务模块和身份占位符绑定后生成案例资产版本。
 */
public class ClassicCaseImportRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "案例编码不能为空")
    @Size(max = 128, message = "案例编码长度不能超过 128")
    private String caseCode;

    @NotBlank(message = "案例标题不能为空")
    @Size(max = 256, message = "案例标题长度不能超过 256")
    private String caseTitle;

    @Size(max = 1024, message = "案例说明长度不能超过 1024")
    private String caseSummary;

    @Size(max = 128, message = "案例版本 ID 长度不能超过 128")
    private String caseVersionId;

    @NotBlank(message = "来源正式环境平台 ID 不能为空")
    @Size(max = 64, message = "来源正式环境平台 ID 长度不能超过 64")
    private String sourceConnectorSystemId;

    @NotBlank(message = "目标学习环境平台 ID 不能为空")
    @Size(max = 64, message = "目标学习环境平台 ID 长度不能超过 64")
    private String learningConnectorSystemId;

    @NotBlank(message = "业务模块 ID 不能为空")
    @Size(max = 64, message = "业务模块 ID 长度不能超过 64")
    private String businessModuleId;

    @NotBlank(message = "业务模块编码不能为空")
    @Size(max = 128, message = "业务模块编码长度不能超过 128")
    private String moduleCode;

    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @NotBlank(message = "适用场景不能为空")
    private String sceneTypesJson;

    private String tagsJson;

    private String supportedGenerationModesJson;

    @Size(max = 32, message = "payload 格式版本长度不能超过 32")
    private String payloadSchemaVersion;

    @NotBlank(message = "脱敏案例内容不能为空")
    private String desensitizedCasePayloadJson;

    @NotBlank(message = "学生练习数据格式不能为空")
    private String caseDataFormatJson;

    @NotBlank(message = "身份绑定不能为空")
    private String identityBindingJson;

    private String desensitizePolicyJson;

    private LocalDateTime sourceUpdatedAt;

    @NotBlank(message = "创建人不能为空")
    @Size(max = 64, message = "创建人长度不能超过 64")
    private String createBy;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCaseCode() {
        return caseCode;
    }

    public void setCaseCode(String caseCode) {
        this.caseCode = caseCode;
    }

    public String getCaseTitle() {
        return caseTitle;
    }

    public void setCaseTitle(String caseTitle) {
        this.caseTitle = caseTitle;
    }

    public String getCaseSummary() {
        return caseSummary;
    }

    public void setCaseSummary(String caseSummary) {
        this.caseSummary = caseSummary;
    }

    public String getCaseVersionId() {
        return caseVersionId;
    }

    public void setCaseVersionId(String caseVersionId) {
        this.caseVersionId = caseVersionId;
    }

    public String getSourceConnectorSystemId() {
        return sourceConnectorSystemId;
    }

    public void setSourceConnectorSystemId(String sourceConnectorSystemId) {
        this.sourceConnectorSystemId = sourceConnectorSystemId;
    }

    public String getLearningConnectorSystemId() {
        return learningConnectorSystemId;
    }

    public void setLearningConnectorSystemId(String learningConnectorSystemId) {
        this.learningConnectorSystemId = learningConnectorSystemId;
    }

    public String getBusinessModuleId() {
        return businessModuleId;
    }

    public void setBusinessModuleId(String businessModuleId) {
        this.businessModuleId = businessModuleId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getTeachingPointId() {
        return teachingPointId;
    }

    public void setTeachingPointId(String teachingPointId) {
        this.teachingPointId = teachingPointId;
    }

    public String getSceneTypesJson() {
        return sceneTypesJson;
    }

    public void setSceneTypesJson(String sceneTypesJson) {
        this.sceneTypesJson = sceneTypesJson;
    }

    public String getTagsJson() {
        return tagsJson;
    }

    public void setTagsJson(String tagsJson) {
        this.tagsJson = tagsJson;
    }

    public String getSupportedGenerationModesJson() {
        return supportedGenerationModesJson;
    }

    public void setSupportedGenerationModesJson(String supportedGenerationModesJson) {
        this.supportedGenerationModesJson = supportedGenerationModesJson;
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

    public String getDesensitizePolicyJson() {
        return desensitizePolicyJson;
    }

    public void setDesensitizePolicyJson(String desensitizePolicyJson) {
        this.desensitizePolicyJson = desensitizePolicyJson;
    }

    public LocalDateTime getSourceUpdatedAt() {
        return sourceUpdatedAt;
    }

    public void setSourceUpdatedAt(LocalDateTime sourceUpdatedAt) {
        this.sourceUpdatedAt = sourceUpdatedAt;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
}
