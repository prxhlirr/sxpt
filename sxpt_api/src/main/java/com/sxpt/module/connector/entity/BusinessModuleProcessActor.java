package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 业务模块步骤参与方实体。
 *
 * 业务功能：
 * 1. 映射 business_module_process_actor 表，维护一个标准办理步骤下需要参与的多个单位和角色。
 * 2. 支撑主办、协办、审核、复核、会签、抄送等参与关系，解决原平台一个办理环节多单位多角色协同的问题。
 * 3. 为学生进入练习或考试时生成“学生-数据实例-步骤参与方”分配关系提供标准参与方来源。
 *
 * 关键流程：
 * 1. 管理员先维护 BusinessModuleProcessStep，再为步骤维护一个或多个参与方。
 * 2. 数据准备阶段按步骤和参与方生成实例链路快照。
 * 3. 学生进入时按当前步骤、当前参与方定位原平台单位和角色，再生成 launchToken。
 */
@TableName("business_module_process_actor")
public class BusinessModuleProcessActor {

    @TableId
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

    private Long lockVersion;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    private String status;

    private Boolean deleted;

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

    public Long getLockVersion() {
        return lockVersion;
    }

    public void setLockVersion(Long lockVersion) {
        this.lockVersion = lockVersion;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
