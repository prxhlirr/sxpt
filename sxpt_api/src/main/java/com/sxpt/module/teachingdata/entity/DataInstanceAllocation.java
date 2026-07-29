package com.sxpt.module.teachingdata.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 数据实例分配实体。
 *
 * 业务功能：
 * 1. 映射 data_instance_allocation 表，记录教学数据实例被谁、何时、为何领取。
 * 2. 防止同一数据实例在练习或考试中被并发重复领取。
 * 3. 保存领取时的单位、角色、协作片段和数据需求快照，支撑练习重置、考试追溯和审计。
 *
 * 关键流程：
 * 1. 学生开始练习或考试题目时，系统从数据池领取 READY 数据实例。
 * 2. 分配记录绑定 ownerUserId、taskId、attemptId 或 questionAttemptId。
 * 3. 数据完成、释放、废弃或重置时更新 allocationStatus 和对应时间、原因。
 */
@TableName("data_instance_allocation")
public class DataInstanceAllocation {

    @TableId
    private String id;

    private String tenantId;

    private String poolId;

    private String dataInstanceId;

    private String taskId;

    private String executionId;

    private String attemptId;

    private String questionAttemptId;

    private String ownerUserId;

    private String allocationScene;

    private String allocationStatus;

    private LocalDateTime allocateTime;

    private LocalDateTime releaseTime;

    private String requiredExternalOrgId;

    private String requiredExternalOrgName;

    private String requiredExternalRoleId;

    private String requiredExternalRoleName;

    private String actorType;

    private Long segmentNo;

    private String collaborationUnitId;

    private String requirementSnapshotJson;

    private LocalDateTime consumeTime;

    private String releaseReason;

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

    public String getPoolId() {
        return poolId;
    }

    public void setPoolId(String poolId) {
        this.poolId = poolId;
    }

    public String getDataInstanceId() {
        return dataInstanceId;
    }

    public void setDataInstanceId(String dataInstanceId) {
        this.dataInstanceId = dataInstanceId;
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

    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public String getQuestionAttemptId() {
        return questionAttemptId;
    }

    public void setQuestionAttemptId(String questionAttemptId) {
        this.questionAttemptId = questionAttemptId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getAllocationScene() {
        return allocationScene;
    }

    public void setAllocationScene(String allocationScene) {
        this.allocationScene = allocationScene;
    }

    public String getAllocationStatus() {
        return allocationStatus;
    }

    public void setAllocationStatus(String allocationStatus) {
        this.allocationStatus = allocationStatus;
    }

    public LocalDateTime getAllocateTime() {
        return allocateTime;
    }

    public void setAllocateTime(LocalDateTime allocateTime) {
        this.allocateTime = allocateTime;
    }

    public LocalDateTime getReleaseTime() {
        return releaseTime;
    }

    public void setReleaseTime(LocalDateTime releaseTime) {
        this.releaseTime = releaseTime;
    }

    public String getRequiredExternalOrgId() {
        return requiredExternalOrgId;
    }

    public void setRequiredExternalOrgId(String requiredExternalOrgId) {
        this.requiredExternalOrgId = requiredExternalOrgId;
    }

    public String getRequiredExternalOrgName() {
        return requiredExternalOrgName;
    }

    public void setRequiredExternalOrgName(String requiredExternalOrgName) {
        this.requiredExternalOrgName = requiredExternalOrgName;
    }

    public String getRequiredExternalRoleId() {
        return requiredExternalRoleId;
    }

    public void setRequiredExternalRoleId(String requiredExternalRoleId) {
        this.requiredExternalRoleId = requiredExternalRoleId;
    }

    public String getRequiredExternalRoleName() {
        return requiredExternalRoleName;
    }

    public void setRequiredExternalRoleName(String requiredExternalRoleName) {
        this.requiredExternalRoleName = requiredExternalRoleName;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public Long getSegmentNo() {
        return segmentNo;
    }

    public void setSegmentNo(Long segmentNo) {
        this.segmentNo = segmentNo;
    }

    public String getCollaborationUnitId() {
        return collaborationUnitId;
    }

    public void setCollaborationUnitId(String collaborationUnitId) {
        this.collaborationUnitId = collaborationUnitId;
    }

    public String getRequirementSnapshotJson() {
        return requirementSnapshotJson;
    }

    public void setRequirementSnapshotJson(String requirementSnapshotJson) {
        this.requirementSnapshotJson = requirementSnapshotJson;
    }

    public LocalDateTime getConsumeTime() {
        return consumeTime;
    }

    public void setConsumeTime(LocalDateTime consumeTime) {
        this.consumeTime = consumeTime;
    }

    public String getReleaseReason() {
        return releaseReason;
    }

    public void setReleaseReason(String releaseReason) {
        this.releaseReason = releaseReason;
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
