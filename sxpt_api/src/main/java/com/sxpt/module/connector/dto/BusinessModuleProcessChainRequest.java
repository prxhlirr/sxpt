package com.sxpt.module.connector.dto;

import javax.validation.constraints.Size;

/**
 * 业务模块标准办理链维护请求集合。
 *
 * 业务功能：
 * 1. 承载管理员维护标准办理步骤和步骤参与方时提交的表单数据。
 * 2. 将步骤和参与方请求集中在一个聚合类中，避免为同一业务能力拆出过多轻量 DTO 文件。
 *
 * 关键流程：
 * 1. Controller 按具体接口选择 StepRequest 或 ActorRequest。
 * 2. Controller 将请求转换为标准办理步骤或步骤参与方实体。
 * 3. Service 负责补齐默认值、校验唯一性和维护状态。
 */
public final class BusinessModuleProcessChainRequest {

    private BusinessModuleProcessChainRequest() {
    }

    /**
     * 标准办理步骤维护请求。
     *
     * 业务功能：
     * 1. 表达一个业务模块中的流程步骤本身。
     * 2. 不包含单位和角色，因为单位和角色属于步骤参与方。
     *
     * 关键流程：
     * 1. 创建时提交租户、原平台、业务模块、步骤顺序、步骤编码和步骤名称。
     * 2. 更新时只允许调整名称、类型、状态映射、完成规则和说明。
     */
    public static class StepRequest {

        @Size(max = 64, message = "租户 ID 长度不能超过 64")
        private String tenantId;

        @Size(max = 64, message = "原平台配置 ID 长度不能超过 64")
        private String connectorSystemId;

        @Size(max = 64, message = "业务模块 ID 长度不能超过 64")
        private String businessModuleId;

        @Size(max = 128, message = "业务模块编码长度不能超过 128")
        private String moduleCode;

        private Integer stepNo;

        @Size(max = 128, message = "步骤编码长度不能超过 128")
        private String stepCode;

        @Size(max = 256, message = "步骤名称长度不能超过 256")
        private String stepName;

        @Size(max = 64, message = "步骤类型长度不能超过 64")
        private String stepType;

        @Size(max = 64, message = "初始状态长度不能超过 64")
        private String initExternalStatus;

        @Size(max = 64, message = "目标状态长度不能超过 64")
        private String targetExternalStatus;

        @Size(max = 4096, message = "完成规则 JSON 长度不能超过 4096")
        private String completionRuleJson;

        @Size(max = 512, message = "备注长度不能超过 512")
        private String remark;

        @Size(max = 64, message = "创建人长度不能超过 64")
        private String createBy;

        @Size(max = 64, message = "更新人长度不能超过 64")
        private String updateBy;

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

        public String getCreateBy() {
            return createBy;
        }

        public void setCreateBy(String createBy) {
            this.createBy = createBy;
        }

        public String getUpdateBy() {
            return updateBy;
        }

        public void setUpdateBy(String updateBy) {
            this.updateBy = updateBy;
        }
    }

    /**
     * 步骤参与方维护请求。
     *
     * 业务功能：
     * 1. 表达一个标准办理步骤下的具体参与身份。
     * 2. 支撑一个步骤下存在主办、协办、审核、复核、会签等多个单位角色。
     *
     * 关键流程：
     * 1. 创建时提交所属步骤、参与方序号、参与关系、参与类型和分配规则。
     * 2. Service 会从所属步骤自动继承原平台、业务模块和步骤编码。
     */
    public static class ActorRequest {

        @Size(max = 64, message = "租户 ID 长度不能超过 64")
        private String tenantId;

        @Size(max = 64, message = "标准办理步骤 ID 长度不能超过 64")
        private String processStepId;

        private Integer actorNo;

        @Size(max = 64, message = "参与关系长度不能超过 64")
        private String actorRelation;

        @Size(max = 64, message = "参与人类型长度不能超过 64")
        private String actorType;

        @Size(max = 64, message = "单位类型长度不能超过 64")
        private String requiredOrgType;

        @Size(max = 128, message = "单位编码长度不能超过 128")
        private String requiredOrgCode;

        @Size(max = 256, message = "单位名称长度不能超过 256")
        private String requiredOrgName;

        @Size(max = 128, message = "角色编码长度不能超过 128")
        private String requiredRoleCode;

        @Size(max = 256, message = "角色名称长度不能超过 256")
        private String requiredRoleName;

        private Boolean isRequired;

        @Size(max = 64, message = "分配规则长度不能超过 64")
        private String assignmentRule;

        @Size(max = 512, message = "备注长度不能超过 512")
        private String remark;

        @Size(max = 64, message = "创建人长度不能超过 64")
        private String createBy;

        @Size(max = 64, message = "更新人长度不能超过 64")
        private String updateBy;

        public String getTenantId() {
            return tenantId;
        }

        public void setTenantId(String tenantId) {
            this.tenantId = tenantId;
        }

        public String getProcessStepId() {
            return processStepId;
        }

        public void setProcessStepId(String processStepId) {
            this.processStepId = processStepId;
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

        public String getCreateBy() {
            return createBy;
        }

        public void setCreateBy(String createBy) {
            this.createBy = createBy;
        }

        public String getUpdateBy() {
            return updateBy;
        }

        public void setUpdateBy(String updateBy) {
            this.updateBy = updateBy;
        }
    }
}
