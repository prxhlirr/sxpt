package com.sxpt.module.connector.vo;

import java.util.ArrayList;
import java.util.List;

/** 原平台查询可绑定模块时使用的精简契约。 */
public class ConnectorBusinessModuleVO {

    private String businessModuleCode;
    private String businessModuleName;
    private Boolean classicCaseEnabled;
    private List<String> supportedGenerationModes = new ArrayList<>();

    public String getBusinessModuleCode() {
        return businessModuleCode;
    }

    public void setBusinessModuleCode(String businessModuleCode) {
        this.businessModuleCode = businessModuleCode;
    }

    public String getBusinessModuleName() {
        return businessModuleName;
    }

    public void setBusinessModuleName(String businessModuleName) {
        this.businessModuleName = businessModuleName;
    }

    public Boolean getClassicCaseEnabled() {
        return classicCaseEnabled;
    }

    public void setClassicCaseEnabled(Boolean classicCaseEnabled) {
        this.classicCaseEnabled = classicCaseEnabled;
    }

    public List<String> getSupportedGenerationModes() {
        return supportedGenerationModes;
    }

    public void setSupportedGenerationModes(List<String> supportedGenerationModes) {
        this.supportedGenerationModes = supportedGenerationModes;
    }
}
