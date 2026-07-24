package com.sxpt.module.practice.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 练习过程分汇总生成请求。
 *
 * 业务功能：
 * 1. 承载一次过程分重算所需的学生、任务和教学点范围。
 * 2. 支持教师手动重算或后续定时任务复用同一服务入口。
 *
 * 关键流程：
 * 1. Controller 校验租户、学生和任务必填。
 * 2. Service 在该范围内读取练习次数和步骤结果并生成汇总。
 */
public class GeneratePracticeScoreSummaryRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "学生 ID 不能为空")
    @Size(max = 64, message = "学生 ID 长度不能超过 64")
    private String studentId;

    @Size(max = 64, message = "班级 ID 长度不能超过 64")
    private String classId;

    @Size(max = 64, message = "课程 ID 长度不能超过 64")
    private String courseId;

    @NotBlank(message = "任务 ID 不能为空")
    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @Size(max = 64, message = "创建人长度不能超过 64")
    private String createBy;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTeachingPointId() {
        return teachingPointId;
    }

    public void setTeachingPointId(String teachingPointId) {
        this.teachingPointId = teachingPointId;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
}
