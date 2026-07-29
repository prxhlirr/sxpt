package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 业务模块标准办理步骤实体。
 *
 * 业务功能：
 * 1. 映射 business_module_process_step 表，维护原平台业务模块在教学平台中的标准办理步骤。
 * 2. 只描述步骤本身，不直接绑定具体单位和角色，避免把“一步骤多参与方”的真实业务压扁成一对一关系。
 * 3. 为批量造数结果补齐实例办理链快照、学生进入原平台时定位当前步骤提供标准来源。
 *
 * 关键流程：
 * 1. 管理员在业务模块下维护步骤顺序、步骤编码、步骤名称、进入状态和完成状态。
 * 2. 每个步骤再通过 BusinessModuleProcessActor 维护一个或多个参与方。
 * 3. 原平台未返回完整真实办理链时，数据准备服务按本表步骤和参与方表自动生成实例链路快照。
 */
@TableName("business_module_process_step")
public class BusinessModuleProcessStep {

    @TableId
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
