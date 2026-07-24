package com.sxpt.module.execution.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 创建任务执行上下文快照请求。
 *
 * 业务功能：
 * 1. 承载学生进入任务时需要固化的 SDK 运行上下文。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺少执行记录、任务、学生或上下文 JSON 的请求。
 *
 * 关键流程：
 * 1. Controller 接收请求并完成基础参数校验。
 * 2. Controller 将请求转换为 TaskExecutionContext 实体后交给 Service 写入。
 */
public class CreateTaskExecutionContextRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "任务执行 ID 不能为空")
    @Size(max = 64, message = "任务执行 ID 长度不能超过 64")
    private String executionId;

    @NotBlank(message = "任务 ID 不能为空")
    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @NotBlank(message = "学生 ID 不能为空")
    @Size(max = 64, message = "学生 ID 长度不能超过 64")
    private String studentId;

    @NotBlank(message = "SDK 模式不能为空")
    @Size(max = 32, message = "SDK 模式长度不能超过 32")
    private String sdkMode;

    @NotBlank(message = "上下文 JSON 不能为空")
    @Size(max = 32768, message = "上下文 JSON 长度不能超过 32768")
    private String contextJson;

    @Size(max = 32768, message = "遮罩策略 JSON 长度不能超过 32768")
    private String overlayPolicyJson;

    @Size(max = 32768, message = "教学点快照 JSON 长度不能超过 32768")
    private String teachingPointSnapshotJson;

    @Size(max = 32768, message = "资源快照 JSON 长度不能超过 32768")
    private String resourceSnapshotJson;

    @Size(max = 32768, message = "评分快照 JSON 长度不能超过 32768")
    private String evaluationSnapshotJson;

    private LocalDateTime expireTime;

    @Size(max = 64, message = "创建人 ID 长度不能超过 64")
    private String createBy;

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

    public String getSdkMode() {
        return sdkMode;
    }

    public void setSdkMode(String sdkMode) {
        this.sdkMode = sdkMode;
    }

    public String getContextJson() {
        return contextJson;
    }

    public void setContextJson(String contextJson) {
        this.contextJson = contextJson;
    }

    public String getOverlayPolicyJson() {
        return overlayPolicyJson;
    }

    public void setOverlayPolicyJson(String overlayPolicyJson) {
        this.overlayPolicyJson = overlayPolicyJson;
    }

    public String getTeachingPointSnapshotJson() {
        return teachingPointSnapshotJson;
    }

    public void setTeachingPointSnapshotJson(String teachingPointSnapshotJson) {
        this.teachingPointSnapshotJson = teachingPointSnapshotJson;
    }

    public String getResourceSnapshotJson() {
        return resourceSnapshotJson;
    }

    public void setResourceSnapshotJson(String resourceSnapshotJson) {
        this.resourceSnapshotJson = resourceSnapshotJson;
    }

    public String getEvaluationSnapshotJson() {
        return evaluationSnapshotJson;
    }

    public void setEvaluationSnapshotJson(String evaluationSnapshotJson) {
        this.evaluationSnapshotJson = evaluationSnapshotJson;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
}
