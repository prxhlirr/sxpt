package com.sxpt.module.teachingdata.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 教学数据池实体。
 *
 * 业务功能：
 * 1. 映射 teaching_data_pool 表，管理练习和考试场景下批量准备的数据实例。
 * 2. 保存任务、班级、题目、模板、策略和数据需求批次之间的池化关系。
 * 3. 支撑 READY 数据领取、考试数据锁定、失败统计和后台数据池监控。
 *
 * 关键流程：
 * 1. 发布练习或考试任务后，系统按策略创建数据池。
 * 2. 数据准备任务将原平台返回的数据实例写入池中，并回写 ready、failed 等计数。
 * 3. 学生开始练习或考试题目时，从数据池领取符合约束的 READY 数据实例。
 */
@TableName("teaching_data_pool")
public class TeachingDataPool {

    @TableId
    private String id;

    private String tenantId;

    private String connectorSystemId;

    private String moduleCode;

    private String taskId;

    private String classId;

    private String sceneType;

    private String questionId;

    private String templateId;

    private String strategyId;

    private String requirementId;

    private String poolStatus;

    private Long totalCount;

    private Long readyCount;

    private Long allocatedCount;

    private Long failedCount;

    private String poolPolicyJson;

    private String idempotencyKey;

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

    public String getModuleCode() {
        return moduleCode;
    }

    public void setModuleCode(String moduleCode) {
        this.moduleCode = moduleCode;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getStrategyId() {
        return strategyId;
    }

    public void setStrategyId(String strategyId) {
        this.strategyId = strategyId;
    }

    public String getRequirementId() {
        return requirementId;
    }

    public void setRequirementId(String requirementId) {
        this.requirementId = requirementId;
    }

    public String getPoolStatus() {
        return poolStatus;
    }

    public void setPoolStatus(String poolStatus) {
        this.poolStatus = poolStatus;
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getReadyCount() {
        return readyCount;
    }

    public void setReadyCount(Long readyCount) {
        this.readyCount = readyCount;
    }

    public Long getAllocatedCount() {
        return allocatedCount;
    }

    public void setAllocatedCount(Long allocatedCount) {
        this.allocatedCount = allocatedCount;
    }

    public Long getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Long failedCount) {
        this.failedCount = failedCount;
    }

    public String getPoolPolicyJson() {
        return poolPolicyJson;
    }

    public void setPoolPolicyJson(String poolPolicyJson) {
        this.poolPolicyJson = poolPolicyJson;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
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
