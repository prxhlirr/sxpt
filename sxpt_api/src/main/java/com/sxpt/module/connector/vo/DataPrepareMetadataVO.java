package com.sxpt.module.connector.vo;

import java.util.List;

/**
 * 数据准备后台配置元数据。
 *
 * 业务功能：
 * 1. 统一向前端下发平台接入、模块、模板、策略和批次准备阶段可选择的稳定编码。
 * 2. 将 P0 阶段仅开放 DATA_CREATE 和 API_KEY 的约束集中到一个响应模型，避免页面各自硬编码。
 *
 * 关键流程：
 * 1. Controller 从集中元数据构造本对象。
 * 2. 前端管理页读取本对象后渲染选项和默认值。
 * 3. 后续如果元数据入库，本对象保持不变，只替换元数据来源。
 */
public class DataPrepareMetadataVO {

    private List<OptionVO> authTypes;

    private List<OptionVO> capabilities;

    private List<OptionVO> sceneTypes;

    private List<OptionVO> templateUsages;

    private List<OptionVO> platformTypes;

    private List<OptionVO> environmentTypes;

    private List<OptionVO> dataSourceStrategies;

    private List<OptionVO> prepareTimings;

    private List<OptionVO> sharePolicies;

    private List<OptionVO> regeneratePolicies;

    private List<OptionVO> lockPolicies;

    private List<OptionVO> recordStatuses;

    private PlatformDefaultsVO platformDefaults;

    private ModuleDefaultsVO moduleDefaults;

    private TemplateDefaultsVO templateDefaults;

    private StrategyDefaultsVO strategyDefaults;

    public List<OptionVO> getAuthTypes() {
        return authTypes;
    }

    public void setAuthTypes(List<OptionVO> authTypes) {
        this.authTypes = authTypes;
    }

    public List<OptionVO> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<OptionVO> capabilities) {
        this.capabilities = capabilities;
    }

    public List<OptionVO> getSceneTypes() {
        return sceneTypes;
    }

    public void setSceneTypes(List<OptionVO> sceneTypes) {
        this.sceneTypes = sceneTypes;
    }

    public List<OptionVO> getTemplateUsages() {
        return templateUsages;
    }

    public void setTemplateUsages(List<OptionVO> templateUsages) {
        this.templateUsages = templateUsages;
    }

    public List<OptionVO> getPlatformTypes() {
        return platformTypes;
    }

    public void setPlatformTypes(List<OptionVO> platformTypes) {
        this.platformTypes = platformTypes;
    }

    public List<OptionVO> getEnvironmentTypes() {
        return environmentTypes;
    }

    public void setEnvironmentTypes(List<OptionVO> environmentTypes) {
        this.environmentTypes = environmentTypes;
    }

    public List<OptionVO> getDataSourceStrategies() {
        return dataSourceStrategies;
    }

    public void setDataSourceStrategies(List<OptionVO> dataSourceStrategies) {
        this.dataSourceStrategies = dataSourceStrategies;
    }

    public List<OptionVO> getPrepareTimings() {
        return prepareTimings;
    }

    public void setPrepareTimings(List<OptionVO> prepareTimings) {
        this.prepareTimings = prepareTimings;
    }

    public List<OptionVO> getSharePolicies() {
        return sharePolicies;
    }

    public void setSharePolicies(List<OptionVO> sharePolicies) {
        this.sharePolicies = sharePolicies;
    }

    public List<OptionVO> getRegeneratePolicies() {
        return regeneratePolicies;
    }

    public void setRegeneratePolicies(List<OptionVO> regeneratePolicies) {
        this.regeneratePolicies = regeneratePolicies;
    }

    public List<OptionVO> getLockPolicies() {
        return lockPolicies;
    }

    public void setLockPolicies(List<OptionVO> lockPolicies) {
        this.lockPolicies = lockPolicies;
    }

    public List<OptionVO> getRecordStatuses() {
        return recordStatuses;
    }

    public void setRecordStatuses(List<OptionVO> recordStatuses) {
        this.recordStatuses = recordStatuses;
    }

    public PlatformDefaultsVO getPlatformDefaults() {
        return platformDefaults;
    }

    public void setPlatformDefaults(PlatformDefaultsVO platformDefaults) {
        this.platformDefaults = platformDefaults;
    }

    public ModuleDefaultsVO getModuleDefaults() {
        return moduleDefaults;
    }

    public void setModuleDefaults(ModuleDefaultsVO moduleDefaults) {
        this.moduleDefaults = moduleDefaults;
    }

    public TemplateDefaultsVO getTemplateDefaults() {
        return templateDefaults;
    }

    public void setTemplateDefaults(TemplateDefaultsVO templateDefaults) {
        this.templateDefaults = templateDefaults;
    }

    public StrategyDefaultsVO getStrategyDefaults() {
        return strategyDefaults;
    }

    public void setStrategyDefaults(StrategyDefaultsVO strategyDefaults) {
        this.strategyDefaults = strategyDefaults;
    }

    /**
     * 通用编码选项。
     *
     * 业务功能：
     * 1. 承载前端 select/radio 使用的编码、名称和是否可见。
     * 2. 通过 visible 支持 P0 阶段隐藏暂不开放的能力和生命周期节点。
     *
     * 关键流程：
     * 1. 后端构造稳定选项。
     * 2. 前端只展示 visible=true 的选项。
     */
    public static class OptionVO {

        private String code;

        private String label;

        private Boolean visible;

        public OptionVO() {
        }

        public OptionVO(String code, String label, Boolean visible) {
            this.code = code;
            this.label = label;
            this.visible = visible;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Boolean getVisible() {
            return visible;
        }

        public void setVisible(Boolean visible) {
            this.visible = visible;
        }
    }

    public static class PlatformDefaultsVO {

        private String systemType;

        private String authType;

        private String authConfigJson;

        private String capabilityCode;

        private String capabilityName;

        private String capabilityType;

        private String capabilityMethod;

        private Integer capabilityTimeoutMs;

        public String getSystemType() {
            return systemType;
        }

        public void setSystemType(String systemType) {
            this.systemType = systemType;
        }

        public String getAuthType() {
            return authType;
        }

        public void setAuthType(String authType) {
            this.authType = authType;
        }

        public String getAuthConfigJson() {
            return authConfigJson;
        }

        public void setAuthConfigJson(String authConfigJson) {
            this.authConfigJson = authConfigJson;
        }

        public String getCapabilityCode() {
            return capabilityCode;
        }

        public void setCapabilityCode(String capabilityCode) {
            this.capabilityCode = capabilityCode;
        }

        public String getCapabilityName() {
            return capabilityName;
        }

        public void setCapabilityName(String capabilityName) {
            this.capabilityName = capabilityName;
        }

        public String getCapabilityType() {
            return capabilityType;
        }

        public void setCapabilityType(String capabilityType) {
            this.capabilityType = capabilityType;
        }

        public String getCapabilityMethod() {
            return capabilityMethod;
        }

        public void setCapabilityMethod(String capabilityMethod) {
            this.capabilityMethod = capabilityMethod;
        }

        public Integer getCapabilityTimeoutMs() {
            return capabilityTimeoutMs;
        }

        public void setCapabilityTimeoutMs(Integer capabilityTimeoutMs) {
            this.capabilityTimeoutMs = capabilityTimeoutMs;
        }
    }

    public static class ModuleDefaultsVO {

        private String initialStatus;

        private String targetStatus;

        private String capabilityCodesJson;

        private String processStepCode;

        private String processStepType;

        public String getInitialStatus() {
            return initialStatus;
        }

        public void setInitialStatus(String initialStatus) {
            this.initialStatus = initialStatus;
        }

        public String getTargetStatus() {
            return targetStatus;
        }

        public void setTargetStatus(String targetStatus) {
            this.targetStatus = targetStatus;
        }

        public String getCapabilityCodesJson() {
            return capabilityCodesJson;
        }

        public void setCapabilityCodesJson(String capabilityCodesJson) {
            this.capabilityCodesJson = capabilityCodesJson;
        }

        public String getProcessStepCode() {
            return processStepCode;
        }

        public void setProcessStepCode(String processStepCode) {
            this.processStepCode = processStepCode;
        }

        public String getProcessStepType() {
            return processStepType;
        }

        public void setProcessStepType(String processStepType) {
            this.processStepType = processStepType;
        }
    }

    public static class TemplateDefaultsVO {

        private String sceneType;

        private String initState;

        private String supportMode;

        private String requiredStatus;

        public String getSceneType() {
            return sceneType;
        }

        public void setSceneType(String sceneType) {
            this.sceneType = sceneType;
        }

        public String getInitState() {
            return initState;
        }

        public void setInitState(String initState) {
            this.initState = initState;
        }

        public String getSupportMode() {
            return supportMode;
        }

        public void setSupportMode(String supportMode) {
            this.supportMode = supportMode;
        }

        public String getRequiredStatus() {
            return requiredStatus;
        }

        public void setRequiredStatus(String requiredStatus) {
            this.requiredStatus = requiredStatus;
        }
    }

    public static class StrategyDefaultsVO {

        private String sceneType;

        private String dataSourceStrategy;

        private String initExternalStatus;

        private String targetExternalStatus;

        private String prepareTiming;

        private String sharePolicy;

        private String regeneratePolicy;

        private String lockPolicy;

        private String validationPolicyJson;

        public String getSceneType() {
            return sceneType;
        }

        public void setSceneType(String sceneType) {
            this.sceneType = sceneType;
        }

        public String getDataSourceStrategy() {
            return dataSourceStrategy;
        }

        public void setDataSourceStrategy(String dataSourceStrategy) {
            this.dataSourceStrategy = dataSourceStrategy;
        }

        public String getInitExternalStatus() {
            return initExternalStatus;
        }

        public void setInitExternalStatus(String initExternalStatus) {
            this.initExternalStatus = initExternalStatus;
        }

        public String getTargetExternalStatus() {
            return targetExternalStatus;
        }

        public void setTargetExternalStatus(String targetExternalStatus) {
            this.targetExternalStatus = targetExternalStatus;
        }

        public String getPrepareTiming() {
            return prepareTiming;
        }

        public void setPrepareTiming(String prepareTiming) {
            this.prepareTiming = prepareTiming;
        }

        public String getSharePolicy() {
            return sharePolicy;
        }

        public void setSharePolicy(String sharePolicy) {
            this.sharePolicy = sharePolicy;
        }

        public String getRegeneratePolicy() {
            return regeneratePolicy;
        }

        public void setRegeneratePolicy(String regeneratePolicy) {
            this.regeneratePolicy = regeneratePolicy;
        }

        public String getLockPolicy() {
            return lockPolicy;
        }

        public void setLockPolicy(String lockPolicy) {
            this.lockPolicy = lockPolicy;
        }

        public String getValidationPolicyJson() {
            return validationPolicyJson;
        }

        public void setValidationPolicyJson(String validationPolicyJson) {
            this.validationPolicyJson = validationPolicyJson;
        }
    }
}
