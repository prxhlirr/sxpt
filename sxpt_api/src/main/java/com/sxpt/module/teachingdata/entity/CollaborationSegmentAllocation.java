package com.sxpt.module.teachingdata.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 协作片段分配实体。
 *
 * 业务功能：
 * 1. 映射 collaboration_segment_allocation 表，记录协作单元中每个学生承担的单位、角色和业务片段。
 * 2. 将同一原平台业务数据拆分为多个可执行片段，支撑多角色协同办理。
 * 3. 保存片段初始状态、目标状态和评分点快照，支撑协作过程评分和审计。
 *
 * 关键流程：
 * 1. 创建协作单元后，系统按流程片段为学生分配原平台单位和角色。
 * 2. 学生进入对应片段时，launchToken 使用本表的单位、角色和状态约束。
 * 3. 片段完成后更新 segmentStatus 和 finishTime，供协作单元判断整体进度。
 */
@TableName("collaboration_segment_allocation")
public class CollaborationSegmentAllocation {

    @TableId
    private String id;

    private String tenantId;

    private String collaborationUnitId;

    private String taskId;

    private String studentId;

    private Long segmentNo;

    private String actorType;

    private String requiredExternalOrgId;

    private String requiredExternalOrgName;

    private String requiredExternalRoleId;

    private String requiredExternalRoleName;

    private String initExternalStatus;

    private String targetExternalStatus;

    private String segmentStatus;

    private LocalDateTime startTime;

    private LocalDateTime finishTime;

    private String scorePointSnapshotJson;

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

    public String getCollaborationUnitId() {
        return collaborationUnitId;
    }

    public void setCollaborationUnitId(String collaborationUnitId) {
        this.collaborationUnitId = collaborationUnitId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public Long getSegmentNo() {
        return segmentNo;
    }

    public void setSegmentNo(Long segmentNo) {
        this.segmentNo = segmentNo;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
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

    public String getSegmentStatus() {
        return segmentStatus;
    }

    public void setSegmentStatus(String segmentStatus) {
        this.segmentStatus = segmentStatus;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(LocalDateTime finishTime) {
        this.finishTime = finishTime;
    }

    public String getScorePointSnapshotJson() {
        return scorePointSnapshotJson;
    }

    public void setScorePointSnapshotJson(String scorePointSnapshotJson) {
        this.scorePointSnapshotJson = scorePointSnapshotJson;
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
