package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 练习任务业务场景实体。
 *
 * 业务功能：
 * 1. 映射 practice_task_scene 表，保存练习任务与老师备案产物之间的绑定关系。
 * 2. 支撑一个练习任务包含多个 businessSceneCode，学生可任意顺序完成。
 *
 * 关键流程：
 * 1. 老师发布练习任务时选择多个备案产物。
 * 2. 教学平台把备案产物快照固化到本表。
 * 3. 学生进入原平台 verify token 时，根据 taskId 查询本表返回任务清单。
 */
@TableName("practice_task_scene")
public class PracticeTaskScene {

    @TableId
    private String id;

    private String tenantId;

    private String taskId;

    private String connectorSystemId;

    private String recordArtifactId;

    private String businessSceneCode;

    private String businessSceneName;

    private String sourceDataSessionId;

    private String sourceExternalBusinessId;

    private Integer sortNo;

    private Boolean requiredFlag;

    private String scoreRuleSnapshotJson;

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

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getRecordArtifactId() {
        return recordArtifactId;
    }

    public void setRecordArtifactId(String recordArtifactId) {
        this.recordArtifactId = recordArtifactId;
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

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Boolean getRequiredFlag() {
        return requiredFlag;
    }

    public void setRequiredFlag(Boolean requiredFlag) {
        this.requiredFlag = requiredFlag;
    }

    public String getScoreRuleSnapshotJson() {
        return scoreRuleSnapshotJson;
    }

    public void setScoreRuleSnapshotJson(String scoreRuleSnapshotJson) {
        this.scoreRuleSnapshotJson = scoreRuleSnapshotJson;
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
