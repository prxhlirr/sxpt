package com.sxpt.module.connector.vo;

import java.util.ArrayList;
import java.util.List;

/** 教案创建页使用的可选经典案例锁版摘要，不包含完整脱敏 payload。 */
public class LessonPlanClassicCaseOptionVO {

    private String classicCaseId;
    private String connectorSystemId;
    private String learningConnectorSystemId;
    private String caseCode;
    private String caseName;
    private String businessModuleCode;
    private String summary;
    private List<String> tags = new ArrayList<>();
    private String caseVersionId;
    private Integer versionNo;
    private String payloadSchemaVersion;
    private List<String> supportedGenerationModes = new ArrayList<>();
    private String defaultGenerationMode;

    public String getClassicCaseId() {
        return classicCaseId;
    }

    public void setClassicCaseId(String classicCaseId) {
        this.classicCaseId = classicCaseId;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getLearningConnectorSystemId() {
        return learningConnectorSystemId;
    }

    public void setLearningConnectorSystemId(String learningConnectorSystemId) {
        this.learningConnectorSystemId = learningConnectorSystemId;
    }

    public String getCaseCode() {
        return caseCode;
    }

    public void setCaseCode(String caseCode) {
        this.caseCode = caseCode;
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

    public String getCaseVersionId() {
        return caseVersionId;
    }

    public void setCaseVersionId(String caseVersionId) {
        this.caseVersionId = caseVersionId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
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

    public String getDefaultGenerationMode() {
        return defaultGenerationMode;
    }

    public void setDefaultGenerationMode(String defaultGenerationMode) {
        this.defaultGenerationMode = defaultGenerationMode;
    }
}
