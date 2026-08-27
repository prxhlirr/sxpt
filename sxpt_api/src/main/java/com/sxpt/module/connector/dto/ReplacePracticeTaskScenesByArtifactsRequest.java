package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 按备案产物替换练习任务业务场景请求。
 *
 * 业务功能：
 * 1. 承载老师发布练习任务时选择的备案产物 ID 列表。
 * 2. 避免前端或教学平台调用方手工拼业务场景、来源 DataSession 和来源原平台业务 ID。
 *
 * 关键流程：
 * 1. 老师先完成原平台备案并沉淀备案产物。
 * 2. 发布练习任务时提交本请求。
 * 3. 服务从备案产物生成 practice_task_scene，学生练习时按该清单校验。
 */
public class ReplacePracticeTaskScenesByArtifactsRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "任务 ID 不能为空")
    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @NotBlank(message = "操作人 ID 不能为空")
    @Size(max = 64, message = "操作人 ID 长度不能超过 64")
    private String operatorId;

    @NotEmpty(message = "备案产物 ID 列表不能为空")
    private List<@NotBlank(message = "备案产物 ID 不能为空") @Size(max = 64, message = "备案产物 ID 长度不能超过 64") String> recordArtifactIds;

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

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public List<String> getRecordArtifactIds() {
        return recordArtifactIds;
    }

    public void setRecordArtifactIds(List<String> recordArtifactIds) {
        this.recordArtifactIds = recordArtifactIds;
    }
}
