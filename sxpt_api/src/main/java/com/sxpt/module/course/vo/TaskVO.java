package com.sxpt.module.course.vo;

import java.time.LocalDateTime;

/**
 * 教学任务返回对象。
 *
 * 业务功能：
 * 1. 向教师端和学生任务列表返回任务基础信息。
 * 2. 隔离数据库实体，避免软删除等内部持久化字段泄露给前端。
 *
 * 关键流程：
 * 1. Controller 从 Task 实体提取可展示字段。
 * 2. 学生后续通过任务 ID 进入学习、练习或考试执行。
 */
public class TaskVO {

    private String id;

    private String tenantId;

    private String courseId;

    private String publishOrgId;

    private String taskCode;

    private String taskName;

    private Long versionNo;

    private String taskType;

    private String taskGoal;

    private String taskDescription;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long timeLimitMinutes;

    private String overlayPolicyJson;

    private String taskStatus;

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

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getPublishOrgId() {
        return publishOrgId;
    }

    public void setPublishOrgId(String publishOrgId) {
        this.publishOrgId = publishOrgId;
    }

    public String getTaskCode() {
        return taskCode;
    }

    public void setTaskCode(String taskCode) {
        this.taskCode = taskCode;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public Long getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Long versionNo) {
        this.versionNo = versionNo;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getTaskGoal() {
        return taskGoal;
    }

    public void setTaskGoal(String taskGoal) {
        this.taskGoal = taskGoal;
    }

    public String getTaskDescription() {
        return taskDescription;
    }

    public void setTaskDescription(String taskDescription) {
        this.taskDescription = taskDescription;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getTimeLimitMinutes() {
        return timeLimitMinutes;
    }

    public void setTimeLimitMinutes(Long timeLimitMinutes) {
        this.timeLimitMinutes = timeLimitMinutes;
    }

    public String getOverlayPolicyJson() {
        return overlayPolicyJson;
    }

    public void setOverlayPolicyJson(String overlayPolicyJson) {
        this.overlayPolicyJson = overlayPolicyJson;
    }

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
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
