package com.sxpt.module.connector.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * 第三方原平台经典案例接入元数据返回对象。
 *
 * 业务功能：
 * 1. 向原平台正式环境返回可绑定的教学平台学习环境、业务模块和标准办理节点。
 * 2. 让原平台创建经典案例时不硬编码教学平台模块、教学点和角色单位占位符。
 *
 * 关键流程：
 * 1. 第三方通过 API Key 认证后，服务端按正式环境反查同环境组学习环境。
 * 2. 只返回该学习环境下启用的业务模块、流程节点和参与方。
 */
public class ExternalClassicCaseMetadataVO {

    private String tenantId;

    private String sourceConnectorSystemId;

    private String sourceSystemName;

    private String learningConnectorSystemId;

    private String learningSystemName;

    private String environmentGroupCode;

    private List<ModuleVO> modules = new ArrayList<>();

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getSourceConnectorSystemId() {
        return sourceConnectorSystemId;
    }

    public void setSourceConnectorSystemId(String sourceConnectorSystemId) {
        this.sourceConnectorSystemId = sourceConnectorSystemId;
    }

    public String getSourceSystemName() {
        return sourceSystemName;
    }

    public void setSourceSystemName(String sourceSystemName) {
        this.sourceSystemName = sourceSystemName;
    }

    public String getLearningConnectorSystemId() {
        return learningConnectorSystemId;
    }

    public void setLearningConnectorSystemId(String learningConnectorSystemId) {
        this.learningConnectorSystemId = learningConnectorSystemId;
    }

    public String getLearningSystemName() {
        return learningSystemName;
    }

    public void setLearningSystemName(String learningSystemName) {
        this.learningSystemName = learningSystemName;
    }

    public String getEnvironmentGroupCode() {
        return environmentGroupCode;
    }

    public void setEnvironmentGroupCode(String environmentGroupCode) {
        this.environmentGroupCode = environmentGroupCode;
    }

    public List<ModuleVO> getModules() {
        return modules;
    }

    public void setModules(List<ModuleVO> modules) {
        this.modules = modules;
    }

    public static class ModuleVO {

        private String businessModuleId;

        private String moduleCode;

        private String moduleName;

        private String entryUrl;

        private String supportScenes;

        private List<ProcessStepVO> processSteps = new ArrayList<>();

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

        public String getModuleName() {
            return moduleName;
        }

        public void setModuleName(String moduleName) {
            this.moduleName = moduleName;
        }

        public String getEntryUrl() {
            return entryUrl;
        }

        public void setEntryUrl(String entryUrl) {
            this.entryUrl = entryUrl;
        }

        public String getSupportScenes() {
            return supportScenes;
        }

        public void setSupportScenes(String supportScenes) {
            this.supportScenes = supportScenes;
        }

        public List<ProcessStepVO> getProcessSteps() {
            return processSteps;
        }

        public void setProcessSteps(List<ProcessStepVO> processSteps) {
            this.processSteps = processSteps;
        }
    }

    public static class ProcessStepVO {

        private String teachingPointId;

        private String stepCode;

        private String stepName;

        private Integer stepNo;

        private String initExternalStatus;

        private String targetExternalStatus;

        private List<ActorVO> requiredActors = new ArrayList<>();

        public String getTeachingPointId() {
            return teachingPointId;
        }

        public void setTeachingPointId(String teachingPointId) {
            this.teachingPointId = teachingPointId;
        }

        public String getStepCode() {
            return stepCode;
        }

        public void setStepCode(String stepCode) {
            this.stepCode = stepCode;
        }

        public String getStepName() {
            return stepName;
        }

        public void setStepName(String stepName) {
            this.stepName = stepName;
        }

        public Integer getStepNo() {
            return stepNo;
        }

        public void setStepNo(Integer stepNo) {
            this.stepNo = stepNo;
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

        public List<ActorVO> getRequiredActors() {
            return requiredActors;
        }

        public void setRequiredActors(List<ActorVO> requiredActors) {
            this.requiredActors = requiredActors;
        }
    }

    public static class ActorVO {

        private String placeholder;

        private String actorType;

        private String actorRelation;

        private String requiredOrgCode;

        private String requiredOrgName;

        private String requiredRoleCode;

        private String requiredRoleName;

        private Boolean required;

        public String getPlaceholder() {
            return placeholder;
        }

        public void setPlaceholder(String placeholder) {
            this.placeholder = placeholder;
        }

        public String getActorType() {
            return actorType;
        }

        public void setActorType(String actorType) {
            this.actorType = actorType;
        }

        public String getActorRelation() {
            return actorRelation;
        }

        public void setActorRelation(String actorRelation) {
            this.actorRelation = actorRelation;
        }

        public String getRequiredOrgCode() {
            return requiredOrgCode;
        }

        public void setRequiredOrgCode(String requiredOrgCode) {
            this.requiredOrgCode = requiredOrgCode;
        }

        public String getRequiredOrgName() {
            return requiredOrgName;
        }

        public void setRequiredOrgName(String requiredOrgName) {
            this.requiredOrgName = requiredOrgName;
        }

        public String getRequiredRoleCode() {
            return requiredRoleCode;
        }

        public void setRequiredRoleCode(String requiredRoleCode) {
            this.requiredRoleCode = requiredRoleCode;
        }

        public String getRequiredRoleName() {
            return requiredRoleName;
        }

        public void setRequiredRoleName(String requiredRoleName) {
            this.requiredRoleName = requiredRoleName;
        }

        public Boolean getRequired() {
            return required;
        }

        public void setRequired(Boolean required) {
            this.required = required;
        }
    }
}
