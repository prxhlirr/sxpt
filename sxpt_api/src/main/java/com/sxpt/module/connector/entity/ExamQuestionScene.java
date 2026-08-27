package com.sxpt.module.connector.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 考试题目业务场景实体。
 *
 * 业务功能：
 * 1. 映射 exam_question_scene 表，保存一道考试题与原平台业务场景之间的绑定关系。
 * 2. 支撑“考试一道题对应一个原平台数据”的最小模型，不要求教学平台维护原平台页面路径。
 *
 * 关键流程：
 * 1. 老师发布考试或组卷时生成题目尝试 ID 和业务场景编码。
 * 2. 学生进入原平台考试时，verify launchToken 返回本表清单。
 * 3. 原平台按 businessSceneCode 和 dataSpecSnapshotJson 自行造数并注册 DataSession。
 */
@TableName("exam_question_scene")
public class ExamQuestionScene {

    @TableId
    private String id;

    private String tenantId;

    private String taskId;

    private String executionId;

    private String examAttemptId;

    private String questionAttemptId;

    private Long questionNo;

    private String connectorSystemId;

    private String businessSceneCode;

    private String businessSceneName;

    private String dataSpecSnapshotJson;

    private String scoreRuleSnapshotJson;

    private Integer sortNo;

    private Boolean requiredFlag;

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

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
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

    public Long getQuestionNo() {
        return questionNo;
    }

    public void setQuestionNo(Long questionNo) {
        this.questionNo = questionNo;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
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

    public String getDataSpecSnapshotJson() {
        return dataSpecSnapshotJson;
    }

    public void setDataSpecSnapshotJson(String dataSpecSnapshotJson) {
        this.dataSpecSnapshotJson = dataSpecSnapshotJson;
    }

    public String getScoreRuleSnapshotJson() {
        return scoreRuleSnapshotJson;
    }

    public void setScoreRuleSnapshotJson(String scoreRuleSnapshotJson) {
        this.scoreRuleSnapshotJson = scoreRuleSnapshotJson;
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
