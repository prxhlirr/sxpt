package com.sxpt.module.connector.vo;

/**
 * 原平台业务场景返回对象。
 *
 * 业务功能：
 * 1. 向原平台或遮罩层返回本次任务需要完成的业务场景清单。
 * 2. 不暴露原平台页面路径，只返回稳定业务场景编码和教学侧来源样本引用。
 *
 * 关键流程：
 * 1. 原平台 verify launchToken 后获得该对象列表。
 * 2. 原平台或遮罩层展示任务清单。
 * 3. 用户点击页面时，原平台用页面教学标签中的 businessSceneCode 与该清单匹配。
 */
public class OriginBusinessSceneVO {

    private String businessSceneCode;

    private String businessSceneName;

    private String sourceDataSessionId;

    private String sourceExternalBusinessId;

    private String dataSpecSnapshotJson;

    private String questionAttemptId;

    private Long questionNo;

    private String status;

    private Integer sortNo;

    private Boolean requiredFlag;

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

    public String getDataSpecSnapshotJson() {
        return dataSpecSnapshotJson;
    }

    public void setDataSpecSnapshotJson(String dataSpecSnapshotJson) {
        this.dataSpecSnapshotJson = dataSpecSnapshotJson;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
