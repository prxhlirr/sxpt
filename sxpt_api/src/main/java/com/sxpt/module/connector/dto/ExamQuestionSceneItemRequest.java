package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 考试题目业务场景明细请求。
 *
 * 业务功能：
 * 1. 承载单道考试题对应的原平台业务场景编码和造数规格。
 * 2. 让考试发布方只描述题目业务语义，不维护原平台页面路径。
 *
 * 关键流程：
 * 1. 组卷阶段为每道题生成 questionAttemptId。
 * 2. 本对象携带 questionAttemptId、businessSceneCode 和 dataSpecSnapshotJson。
 * 3. 原平台后续按这些字段造数并注册 DataSession。
 */
public class ExamQuestionSceneItemRequest {

    @NotBlank(message = "题目尝试 ID 不能为空")
    @Size(max = 64, message = "题目尝试 ID 长度不能超过 64")
    private String questionAttemptId;

    private Long questionNo;

    @NotBlank(message = "原平台 ID 不能为空")
    @Size(max = 64, message = "原平台 ID 长度不能超过 64")
    private String connectorSystemId;

    @NotBlank(message = "业务场景编码不能为空")
    @Size(max = 128, message = "业务场景编码长度不能超过 128")
    private String businessSceneCode;

    @Size(max = 255, message = "业务场景名称长度不能超过 255")
    private String businessSceneName;

    @Size(max = 32768, message = "数据规格快照长度不能超过 32768")
    private String dataSpecSnapshotJson;

    @Size(max = 32768, message = "评分规则快照长度不能超过 32768")
    private String scoreRuleSnapshotJson;

    private Integer sortNo;

    private Boolean requiredFlag;

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
}
