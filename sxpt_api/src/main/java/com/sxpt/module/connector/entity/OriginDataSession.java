package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 原平台数据会话实体。
 *
 * 业务功能：
 * 1. 映射 origin_data_session 表，保存教学上下文与原平台真实业务数据之间的绑定关系。
 * 2. 支撑老师备案、学生练习和考试按 dataSessionId 追溯过程证据、业务数据和评分依据。
 *
 * 关键流程：
 * 1. 原平台在页面命中业务场景并完成造数或复制后，调用 DataSession 注册接口。
 * 2. 教学平台根据 launchToken 解析教学上下文，幂等创建本实体。
 * 3. 遮罩层后续上报 capture_event 或 execution_trace 时携带 dataSessionId。
 */
@TableName("origin_data_session")
public class OriginDataSession {

    @TableId
    private String id;

    private String tenantId;

    private String launchContextId;

    private String connectorSystemId;

    private String userId;

    private String actorType;

    private String sceneType;

    private String taskId;

    private String executionId;

    private String captureSessionId;

    private String practiceAttemptId;

    private String examAttemptId;

    private String questionAttemptId;

    private String businessSceneCode;

    private String businessSceneName;

    private String sourceDataSessionId;

    private String sourceExternalBusinessId;

    private String dataInstanceId;

    private String externalBusinessId;

    private String externalBusinessNo;

    private String externalStatus;

    private String entryUrl;

    private String dataSpecSnapshotJson;

    private String originPayloadSnapshotJson;

    private String idempotencyKey;

    private String sessionStatus;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    private String status;

    private Boolean deleted;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getLaunchContextId() {
        return launchContextId;
    }

    public void setLaunchContextId(String launchContextId) {
        this.launchContextId = launchContextId;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
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

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getCaptureSessionId() {
        return captureSessionId;
    }

    public void setCaptureSessionId(String captureSessionId) {
        this.captureSessionId = captureSessionId;
    }

    public String getPracticeAttemptId() {
        return practiceAttemptId;
    }

    public void setPracticeAttemptId(String practiceAttemptId) {
        this.practiceAttemptId = practiceAttemptId;
    }

    public String getExamAttemptId() {
        return examAttemptId;
    }

    public void setExamAttemptId(String examAttemptId) {
        this.examAttemptId = examAttemptId;
    }

    public String getQuestionAttemptId() {
        return questionAttemptId;
    }

    public void setQuestionAttemptId(String questionAttemptId) {
        this.questionAttemptId = questionAttemptId;
    }

    public String getBusinessSceneCode() {
        return businessSceneCode;
    }

    public void setBusinessSceneCode(String businessSceneCode) {
        this.businessSceneCode = businessSceneCode;
    }

    public String getBusinessSceneName() {
        return businessSceneName;
    }

    public void setBusinessSceneName(String businessSceneName) {
        this.businessSceneName = businessSceneName;
    }

    public String getSourceDataSessionId() {
        return sourceDataSessionId;
    }

    public void setSourceDataSessionId(String sourceDataSessionId) {
        this.sourceDataSessionId = sourceDataSessionId;
    }

    public String getSourceExternalBusinessId() {
        return sourceExternalBusinessId;
    }

    public void setSourceExternalBusinessId(String sourceExternalBusinessId) {
        this.sourceExternalBusinessId = sourceExternalBusinessId;
    }

    public String getDataInstanceId() {
        return dataInstanceId;
    }

    public void setDataInstanceId(String dataInstanceId) {
        this.dataInstanceId = dataInstanceId;
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

    public String getEntryUrl() {
        return entryUrl;
    }

    public void setEntryUrl(String entryUrl) {
        this.entryUrl = entryUrl;
    }

    public String getDataSpecSnapshotJson() {
        return dataSpecSnapshotJson;
    }

    public void setDataSpecSnapshotJson(String dataSpecSnapshotJson) {
        this.dataSpecSnapshotJson = dataSpecSnapshotJson;
    }

    public String getOriginPayloadSnapshotJson() {
        return originPayloadSnapshotJson;
    }

    public void setOriginPayloadSnapshotJson(String originPayloadSnapshotJson) {
        this.originPayloadSnapshotJson = originPayloadSnapshotJson;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
