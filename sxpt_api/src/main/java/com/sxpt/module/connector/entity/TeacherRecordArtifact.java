package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 老师备案产物实体。
 *
 * 业务功能：
 * 1. 映射 teacher_record_artifact 表，保存老师在原平台备案后沉淀的可复用教学资产。
 * 2. 为练习任务提供来源业务场景、来源 DataSession、来源原平台业务数据和评分规则快照。
 *
 * 关键流程：
 * 1. 老师在原平台备案并注册 RECORD DataSession。
 * 2. 教学平台根据备案过程沉淀本实体。
 * 3. 老师发布练习任务时选择一个或多个备案产物，形成 practice_task_scene。
 */
@TableName("teacher_record_artifact")
public class TeacherRecordArtifact {

    @TableId
    private String id;

    private String tenantId;

    private String teacherUserId;

    private String connectorSystemId;

    private String captureSessionId;

    private String sourceDataSessionId;

    private String sourceDataInstanceId;

    private String businessSceneCode;

    private String businessSceneName;

    private String sourceExternalBusinessId;

    private String sourceExternalBusinessNo;

    private String targetStatus;

    private String stepSnapshotJson;

    private String scoreRuleSnapshotJson;

    private String keyFieldSnapshotJson;

    private String originContextSnapshotJson;

    private String artifactStatus;

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

    public String getTeacherUserId() {
        return teacherUserId;
    }

    public void setTeacherUserId(String teacherUserId) {
        this.teacherUserId = teacherUserId;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getCaptureSessionId() {
        return captureSessionId;
    }

    public void setCaptureSessionId(String captureSessionId) {
        this.captureSessionId = captureSessionId;
    }

    public String getSourceDataSessionId() {
        return sourceDataSessionId;
    }

    public void setSourceDataSessionId(String sourceDataSessionId) {
        this.sourceDataSessionId = sourceDataSessionId;
    }

    public String getSourceDataInstanceId() {
        return sourceDataInstanceId;
    }

    public void setSourceDataInstanceId(String sourceDataInstanceId) {
        this.sourceDataInstanceId = sourceDataInstanceId;
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

    public String getSourceExternalBusinessId() {
        return sourceExternalBusinessId;
    }

    public void setSourceExternalBusinessId(String sourceExternalBusinessId) {
        this.sourceExternalBusinessId = sourceExternalBusinessId;
    }

    public String getSourceExternalBusinessNo() {
        return sourceExternalBusinessNo;
    }

    public void setSourceExternalBusinessNo(String sourceExternalBusinessNo) {
        this.sourceExternalBusinessNo = sourceExternalBusinessNo;
    }

    public String getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(String targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getStepSnapshotJson() {
        return stepSnapshotJson;
    }

    public void setStepSnapshotJson(String stepSnapshotJson) {
        this.stepSnapshotJson = stepSnapshotJson;
    }

    public String getScoreRuleSnapshotJson() {
        return scoreRuleSnapshotJson;
    }

    public void setScoreRuleSnapshotJson(String scoreRuleSnapshotJson) {
        this.scoreRuleSnapshotJson = scoreRuleSnapshotJson;
    }

    public String getKeyFieldSnapshotJson() {
        return keyFieldSnapshotJson;
    }

    public void setKeyFieldSnapshotJson(String keyFieldSnapshotJson) {
        this.keyFieldSnapshotJson = keyFieldSnapshotJson;
    }

    public String getOriginContextSnapshotJson() {
        return originContextSnapshotJson;
    }

    public void setOriginContextSnapshotJson(String originContextSnapshotJson) {
        this.originContextSnapshotJson = originContextSnapshotJson;
    }

    public String getArtifactStatus() {
        return artifactStatus;
    }

    public void setArtifactStatus(String artifactStatus) {
        this.artifactStatus = artifactStatus;
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
