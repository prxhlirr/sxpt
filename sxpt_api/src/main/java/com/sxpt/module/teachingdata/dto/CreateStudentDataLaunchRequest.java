package com.sxpt.module.teachingdata.dto;

/**
 * 学生进入原平台办理的数据启动请求。
 *
 * 业务功能：
 * 1. 承载学生端从“已分配数据”生成原平台启动上下文所需的最小可信参数。
 * 2. 前端只提交分配记录 ID 和学生身份，不提交原平台单位、角色，避免页面伪造办理身份。
 *
 * 关键流程：
 * 1. Service 按 allocationId 读取分配记录。
 * 2. Service 校验租户、学生归属和分配状态。
 * 3. Service 基于分配记录组装 PlatformLaunchContext 并生成一次性 launchToken。
 */
public class CreateStudentDataLaunchRequest {

    private String tenantId;

    private String allocationId;

    private String studentId;

    private String executionId;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getAllocationId() {
        return allocationId;
    }

    public void setAllocationId(String allocationId) {
        this.allocationId = allocationId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
}
