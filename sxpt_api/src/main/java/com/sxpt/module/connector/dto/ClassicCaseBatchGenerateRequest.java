package com.sxpt.module.connector.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 经典案例学生 demo 批量生成请求。
 *
 * 业务功能：
 * 1. 面向一个任务/一批学生，基于经典案例的 caseDataFormat 在原平台学习环境批量生成隔离 demo 数据。
 * 2. 第一阶段仅支持 STUDENT_DEMO，避免老师复刻场景出现“一份经典案例复制多份完全相同数据”的歧义。
 *
 * 关键流程：
 * 1. 服务端只携带 caseDataFormat，不携带 generationRule。
 * 2. 原平台学习环境负责按每个 item 自主生成 demo 数据。
 */
public class ClassicCaseBatchGenerateRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "经典案例 ID 不能为空")
    @Size(max = 64, message = "经典案例 ID 长度不能超过 64")
    private String caseAssetId;

    @Size(max = 64, message = "经典案例版本 ID 长度不能超过 64")
    private String caseVersionId;

    @NotBlank(message = "使用方式不能为空")
    @Size(max = 32, message = "使用方式长度不能超过 32")
    private String usageScene;

    @NotBlank(message = "教学场景不能为空")
    @Size(max = 32, message = "教学场景长度不能超过 32")
    private String sceneType;

    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @Size(max = 128, message = "请求批次 ID 长度不能超过 128")
    private String requestBatchId;

    @Size(max = 128, message = "链路追踪 ID 长度不能超过 128")
    private String traceId;

    @Valid
    @NotEmpty(message = "批量生成明细不能为空")
    private List<ClassicCaseBatchGenerateItemRequest> items;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getCaseAssetId() {
        return caseAssetId;
    }

    public void setCaseAssetId(String caseAssetId) {
        this.caseAssetId = caseAssetId;
    }

    public String getCaseVersionId() {
        return caseVersionId;
    }

    public void setCaseVersionId(String caseVersionId) {
        this.caseVersionId = caseVersionId;
    }

    public String getUsageScene() {
        return usageScene;
    }

    public void setUsageScene(String usageScene) {
        this.usageScene = usageScene;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getRequestBatchId() {
        return requestBatchId;
    }

    public void setRequestBatchId(String requestBatchId) {
        this.requestBatchId = requestBatchId;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public List<ClassicCaseBatchGenerateItemRequest> getItems() {
        return items;
    }

    public void setItems(List<ClassicCaseBatchGenerateItemRequest> items) {
        this.items = items;
    }
}
