package com.sxpt.module.capture.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 发布页面动作草稿请求。
 *
 * 业务功能：
 * 1. 承载教师将已确认动作草稿发布为教学任务步骤时所需的任务、教学点和评分规则归属信息。
 * 2. 使用路径参数表达草稿 ID，请求体只表达发布目标，避免同一业务主键出现两份来源导致歧义。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发 Bean Validation，提前拦截缺少任务、教学点或评分规则的无效发布。
 * 2. Service 根据草稿生成 task_step，并在指定评分规则下生成 MVP 基础评分项。
 */
public class PublishCaptureActionDraftRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "任务 ID 不能为空")
    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @NotBlank(message = "教学点 ID 不能为空")
    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @NotBlank(message = "评价规则 ID 不能为空")
    @Size(max = 64, message = "评价规则 ID 长度不能超过 64")
    private String evaluationRuleId;

    @NotBlank(message = "操作人 ID 不能为空")
    @Size(max = 64, message = "操作人 ID 长度不能超过 64")
    private String operatorId;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public String getEvaluationRuleId() {
        return evaluationRuleId;
    }

    public void setEvaluationRuleId(String evaluationRuleId) {
        this.evaluationRuleId = evaluationRuleId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }
}
