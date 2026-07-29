package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 业务模块标准办理链返回对象集合。
 *
 * 业务功能：
 * 1. 向后台页面返回标准办理步骤和步骤参与方的展示字段。
 * 2. 隔离数据库实体，避免前端直接依赖软删除、内部版本等持久化细节。
 *
 * 关键流程：
 * 1. Controller 从标准办理步骤和步骤参与方实体中提取可展示字段。
 * 2. 前端使用 id 作为编辑、启停和参与方绑定的稳定标识。
 */
public final class BusinessModuleProcessChainVO {

    private BusinessModuleProcessChainVO() {
    }

    /**
     * 标准办理步骤返回对象。
     *
     * 业务功能：
     * 1. 返回业务模块下的流程步骤展示字段。
     * 2. 支撑前端按步骤顺序展示标准办理链。
     */
    public static class StepVO {

        private String id;

        private String tenantId;

        private String connectorSystemId;

        private String businessModuleId;

        private String moduleCode;

        private Integer stepNo;

        private String stepCode;

        private String stepName;

        private String stepType;

        private String initExternalStatus;

        private String targetExternalStatus;

        private String completionRuleJson;

        private String remark;

        private String status;

        private LocalDateTime createTime;

        private LocalDateTime updateTime;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

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

        public Integer getStepNo() {
            return stepNo;
        }

        public void setStepNo(Integer stepNo) {
            this.stepNo = stepNo;
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

        public String getStepType() {
            return stepType;
        }

        public void setStepType(String stepType) {
            this.stepType = stepType;
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

        public String getCompletionRuleJson() {
            return completionRuleJson;
        }

        public void setCompletionRuleJson(String completionRuleJson) {
            this.completionRuleJson = completionRuleJson;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }
    }

    /**
     * 步骤参与方返回对象。
     *
     * 业务功能：
     * 1. 返回某个标准办理步骤下的单位、角色和参与关系。
     * 2. 支撑前端展示一个步骤下多个参与方的协作结构。
     */
    public static class ActorVO {

        private String id;

        private String tenantId;

        private String connectorSystemId;

        private String businessModuleId;

        private String processStepId;

        private String moduleCode;

        private String stepCode;

        private Integer actorNo;

        private String actorRelation;

        private String actorType;

        private String requiredOrgType;

        private String requiredOrgCode;

        private String requiredOrgName;

        private String requiredRoleCode;

        private String requiredRoleName;

        private Boolean isRequired;

        private String assignmentRule;

        private String remark;

        private String status;

        private LocalDateTime createTime;

        private LocalDateTime updateTime;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

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

        public String getBusinessModuleId() {
            return businessModuleId;
        }

        public void setBusinessModuleId(String businessModuleId) {
            this.businessModuleId = businessModuleId;
        }

        public String getProcessStepId() {
            return processStepId;
        }

        public void setProcessStepId(String processStepId) {
            this.processStepId = processStepId;
        }

        public String getModuleCode() {
            return moduleCode;
        }

        public void setModuleCode(String moduleCode) {
            this.moduleCode = moduleCode;
        }

        public String getStepCode() {
            return stepCode;
        }

        public void setStepCode(String stepCode) {
            this.stepCode = stepCode;
        }

        public Integer getActorNo() {
            return actorNo;
        }

        public void setActorNo(Integer actorNo) {
            this.actorNo = actorNo;
        }

        public String getActorRelation() {
            return actorRelation;
        }

        public void setActorRelation(String actorRelation) {
            this.actorRelation = actorRelation;
        }

        public String getActorType() {
            return actorType;
        }

        public void setActorType(String actorType) {
            this.actorType = actorType;
        }

        public String getRequiredOrgType() {
            return requiredOrgType;
        }

        public void setRequiredOrgType(String requiredOrgType) {
            this.requiredOrgType = requiredOrgType;
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

        public Boolean getIsRequired() {
            return isRequired;
        }

        public void setIsRequired(Boolean isRequired) {
            this.isRequired = isRequired;
        }

        public String getAssignmentRule() {
            return assignmentRule;
        }

        public void setAssignmentRule(String assignmentRule) {
            this.assignmentRule = assignmentRule;
        }

        public String getRemark() {
            return remark;
        }

        public void setRemark(String remark) {
            this.remark = remark;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(LocalDateTime createTime) {
            this.createTime = createTime;
        }

        public LocalDateTime getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(LocalDateTime updateTime) {
            this.updateTime = updateTime;
        }
    }
}
