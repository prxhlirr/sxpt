package com.sxpt.module.teachingdata.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 协作单元实体。
 *
 * 业务功能：
 * 1. 映射 collaboration_unit 表，表达多个学生围绕同一原平台业务数据分角色协同办理的聚合边界。
 * 2. 保存协作单元共享的数据实例、原平台业务数据 ID、协作状态和重置来源。
 * 3. 支撑协作练习整体重置，避免只替换某个角色数据导致业务流程断裂。
 *
 * 关键流程：
 * 1. 多角色练习或考试准备数据后，系统创建协作单元。
 * 2. 各角色片段通过 collaboration_segment_allocation 绑定到该协作单元。
 * 3. 任一成员发起重置时，系统废弃旧协作单元并创建新的协作单元和原平台业务数据。
 */
@TableName("collaboration_unit")
public class CollaborationUnit {

    @TableId
    private String id;

    private String tenantId;

    private String taskId;

    private String executionId;

    private String sceneType;

    private String connectorSystemId;

    private String moduleCode;

    private String dataInstanceId;

    private String externalBusinessId;

    private String unitStatus;

    private Long resetCount;

    private String resetFromUnitId;

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

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getDataInstanceId() {
        return dataInstanceId;
    }

    public void setDataInstanceId(String dataInstanceId) {
        this.dataInstanceId = dataInstanceId;
    }

    public String getExternalBusinessId() {
        return externalBusinessId;
    }

    public void setExternalBusinessId(String externalBusinessId) {
        this.externalBusinessId = externalBusinessId;
    }

    public String getUnitStatus() {
        return unitStatus;
    }

    public void setUnitStatus(String unitStatus) {
        this.unitStatus = unitStatus;
    }

    public Long getResetCount() {
        return resetCount;
    }

    public void setResetCount(Long resetCount) {
        this.resetCount = resetCount;
    }

    public String getResetFromUnitId() {
        return resetFromUnitId;
    }

    public void setResetFromUnitId(String resetFromUnitId) {
        this.resetFromUnitId = resetFromUnitId;
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
