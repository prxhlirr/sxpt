package com.sxpt.module.connector.dto;

import com.fasterxml.jackson.databind.JsonNode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

/** OA 原平台按目标契约推送的经典案例版本。 */
public class ClassicCaseUpsertRequest {

    @NotBlank(message = "案例编码不能为空")
    @Size(max = 128, message = "案例编码长度不能超过 128")
    private String caseCode;

    @NotBlank(message = "案例版本 ID 不能为空")
    @Size(max = 128, message = "案例版本 ID 长度不能超过 128")
    private String caseVersionId;

    @NotBlank(message = "案例名称不能为空")
    @Size(max = 256, message = "案例名称长度不能超过 256")
    private String caseName;

    @NotBlank(message = "业务模块编码不能为空")
    @Size(max = 128, message = "业务模块编码长度不能超过 128")
    private String businessModuleCode;

    @NotBlank(message = "payload 结构版本不能为空")
    @Size(max = 32, message = "payload 结构版本长度不能超过 32")
    private String payloadSchemaVersion;

    @NotEmpty(message = "支持的生成模式不能为空")
    private List<String> supportedGenerationModes = new ArrayList<>();

    @Size(max = 1024, message = "案例摘要长度不能超过 1024")
    private String summary;

    private List<String> tags = new ArrayList<>();

    private JsonNode identityBinding;

    private JsonNode desensitizedCasePayload;

    private JsonNode caseDataFormat;

    @Size(max = 32, message = "来源更新时间长度不能超过 32")
    private String sourceUpdatedAt;

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

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public String getBusinessModuleCode() {
        return businessModuleCode;
    }

    public void setBusinessModuleCode(String businessModuleCode) {
        this.businessModuleCode = businessModuleCode;
    }

    public String getPayloadSchemaVersion() {
        return payloadSchemaVersion;
    }

    public void setPayloadSchemaVersion(String payloadSchemaVersion) {
        this.payloadSchemaVersion = payloadSchemaVersion;
    }

    public List<String> getSupportedGenerationModes() {
        return supportedGenerationModes;
    }

    public void setSupportedGenerationModes(List<String> supportedGenerationModes) {
        this.supportedGenerationModes = supportedGenerationModes;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public JsonNode getIdentityBinding() {
        return identityBinding;
    }

    public void setIdentityBinding(JsonNode identityBinding) {
        this.identityBinding = identityBinding;
    }

    public JsonNode getDesensitizedCasePayload() {
        return desensitizedCasePayload;
    }

    public void setDesensitizedCasePayload(JsonNode desensitizedCasePayload) {
        this.desensitizedCasePayload = desensitizedCasePayload;
    }

    public JsonNode getCaseDataFormat() {
        return caseDataFormat;
    }

    public void setCaseDataFormat(JsonNode caseDataFormat) {
        this.caseDataFormat = caseDataFormat;
    }

    public String getSourceUpdatedAt() {
        return sourceUpdatedAt;
    }

    public void setSourceUpdatedAt(String sourceUpdatedAt) {
        this.sourceUpdatedAt = sourceUpdatedAt;
    }
}
