package com.sxpt.module.course.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 创建课程请求。
 *
 * 业务功能：
 * 1. 承载管理员或教师创建课程时提交的课程编码、名称、班级和时间范围。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发基础参数校验。
 * 2. Controller 将请求转换为 Course 实体后交给 Service 写入。
 */
public class CreateCourseRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "课程编码不能为空")
    @Size(max = 64, message = "课程编码长度不能超过 64")
    private String courseCode;

    @NotBlank(message = "课程名称不能为空")
    @Size(max = 128, message = "课程名称长度不能超过 128")
    private String courseName;

    @Size(max = 64, message = "默认授课班级 ID 长度不能超过 64")
    private String targetOrgId;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    @Size(max = 4096, message = "课程说明长度不能超过 4096")
    private String description;

    @Size(max = 64, message = "创建人 ID 长度不能超过 64")
    private String createBy;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getTargetOrgId() {
        return targetOrgId;
    }

    public void setTargetOrgId(String targetOrgId) {
        this.targetOrgId = targetOrgId;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
}
