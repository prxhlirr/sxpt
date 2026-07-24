package com.sxpt.module.course.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 创建教学任务请求。
 *
 * 业务功能：
 * 1. 承载教师创建学习、练习或考试任务时提交的课程、班级、类型和目标。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发基础参数校验。
 * 2. Controller 将请求转换为 Task 实体后交给 Service 写入。
 */
public class CreateTaskRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "课程 ID 不能为空")
    @Size(max = 64, message = "课程 ID 长度不能超过 64")
    private String courseId;

    @NotBlank(message = "发布班级 ID 不能为空")
    @Size(max = 64, message = "发布班级 ID 长度不能超过 64")
    private String publishOrgId;

    @NotBlank(message = "任务编码不能为空")
    @Size(max = 64, message = "任务编码长度不能超过 64")
    private String taskCode;

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 128, message = "任务名称长度不能超过 128")
    private String taskName;

    @NotBlank(message = "任务类型不能为空")
    @Size(max = 32, message = "任务类型长度不能超过 32")
    private String taskType;

    @NotBlank(message = "任务目标不能为空")
    @Size(max = 4096, message = "任务目标长度不能超过 4096")
    private String taskGoal;

    @Size(max = 4096, message = "任务说明长度不能超过 4096")
    private String taskDescription;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long timeLimitMinutes;

    @Size(max = 32768, message = "遮罩策略 JSON 长度不能超过 32768")
    private String overlayPolicyJson;

    @Size(max = 64, message = "创建人 ID 长度不能超过 64")
    private String createBy;

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

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
}
