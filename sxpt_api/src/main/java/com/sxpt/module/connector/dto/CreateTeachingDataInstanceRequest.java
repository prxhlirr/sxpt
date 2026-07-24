package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * 创建教学业务数据实例请求。
 *
 * 业务功能：
 * 1. 承载原平台创建或复用业务数据后返回给教学平台的业务数据引用。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段，避免写入无法定位原平台的数据实例。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发基础参数校验。
 * 2. Controller 将请求转换为 TeachingDataInstance 实体后交给 Service 写入。
 */
public class CreateTeachingDataInstanceRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "教学数据模板 ID 不能为空")
    @Size(max = 64, message = "教学数据模板 ID 长度不能超过 64")
    private String templateId;

    @NotBlank(message = "原平台配置 ID 不能为空")
    @Size(max = 64, message = "原平台配置 ID 长度不能超过 64")
    private String connectorSystemId;

    @Size(max = 64, message = "使用人 ID 长度不能超过 64")
    private String ownerUserId;

    @Size(max = 64, message = "班级 ID 长度不能超过 64")
    private String classId;

    @Size(max = 64, message = "任务 ID 长度不能超过 64")
    private String taskId;

    @Size(max = 64, message = "教学点 ID 长度不能超过 64")
    private String teachingPointId;

    @Size(max = 64, message = "任务执行 ID 长度不能超过 64")
    private String executionId;

    @Size(max = 64, message = "练习次数 ID 长度不能超过 64")
    private String attemptId;

    @NotBlank(message = "场景类型不能为空")
    @Size(max = 32, message = "场景类型长度不能超过 32")
    private String sceneType;

    @NotBlank(message = "原平台业务数据 ID 不能为空")
    @Size(max = 128, message = "原平台业务数据 ID 长度不能超过 128")
    private String externalBusinessId;

    @Size(max = 128, message = "原平台业务单据号长度不能超过 128")
    private String externalBusinessNo;

    @Size(max = 64, message = "原平台业务状态长度不能超过 64")
    private String externalStatus;

    private LocalDateTime expireTime;

    @Size(max = 32768, message = "元数据摘要长度不能超过 32768")
    private String metadataJson;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
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

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getExternalBusinessId() {
        return externalBusinessId;
    }

    public void setExternalBusinessId(String externalBusinessId) {
        this.externalBusinessId = externalBusinessId;
    }

    public String getExternalBusinessNo() {
        return externalBusinessNo;
    }

    public void setExternalBusinessNo(String externalBusinessNo) {
        this.externalBusinessNo = externalBusinessNo;
    }

    public String getExternalStatus() {
        return externalStatus;
    }

    public void setExternalStatus(String externalStatus) {
        this.externalStatus = externalStatus;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }
}
