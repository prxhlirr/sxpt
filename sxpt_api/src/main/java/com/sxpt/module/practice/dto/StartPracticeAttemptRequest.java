package com.sxpt.module.practice.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 开始练习次数请求。
 *
 * 业务功能：
 * 1. 承载学生进入练习模式时创建 practice_attempt 所需的最小上下文。
 * 2. 由服务端计算 attemptNo，避免客户端伪造练习次数。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发 Bean Validation。
 * 2. Service 按租户、学生和任务查询历史次数后生成下一次练习记录。
 */
public class StartPracticeAttemptRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "执行 ID 不能为空")
    @Size(max = 64, message = "执行 ID 长度不能超过 64")
    private String executionId;

    @NotBlank(message = "学生 ID 不能为空")
    @Size(max = 64, message = "学生 ID 长度不能超过 64")
    private String studentId;

    @Size(max = 64, message = "班级 ID 长度不能超过 64")
    private String classId;

    @NotBlank(message = "任务 ID 不能为空")
    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @Size(max = 64, message = "教学业务数据实例 ID 长度不能超过 64")
    private String dataInstanceId;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
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

    public String getDataInstanceId() {
        return dataInstanceId;
    }

    public void setDataInstanceId(String dataInstanceId) {
        this.dataInstanceId = dataInstanceId;
    }
}
