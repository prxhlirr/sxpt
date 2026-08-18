package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/** 教案保存前由服务端校验的经典案例锁版配置。 */
public class ClassicCaseConfigValidationRequest {

    @NotBlank(message = "业务模块编码不能为空")
    @Size(max = 128, message = "业务模块编码长度不能超过 128")
    private String businessModuleCode;

    @NotBlank(message = "来源原平台 ID 不能为空")
    @Size(max = 64, message = "来源原平台 ID 长度不能超过 64")
    private String connectorSystemId;

    @NotBlank(message = "经典案例 ID 不能为空")
    @Size(max = 64, message = "经典案例 ID 长度不能超过 64")
    private String classicCaseId;

    @NotBlank(message = "案例版本 ID 不能为空")
    @Size(max = 128, message = "案例版本 ID 长度不能超过 128")
    private String caseVersionId;

    @NotBlank(message = "生成模式不能为空")
    @Size(max = 32, message = "生成模式长度不能超过 32")
    private String generationMode;

    public String getBusinessModuleCode() {
        return businessModuleCode;
    }

    public void setBusinessModuleCode(String businessModuleCode) {
        this.businessModuleCode = businessModuleCode;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getClassicCaseId() {
        return classicCaseId;
    }

    public void setClassicCaseId(String classicCaseId) {
        this.classicCaseId = classicCaseId;
    }

    public String getCaseVersionId() {
        return caseVersionId;
    }

    public void setCaseVersionId(String caseVersionId) {
        this.caseVersionId = caseVersionId;
    }

    public String getGenerationMode() {
        return generationMode;
    }

    public void setGenerationMode(String generationMode) {
        this.generationMode = generationMode;
    }
}
