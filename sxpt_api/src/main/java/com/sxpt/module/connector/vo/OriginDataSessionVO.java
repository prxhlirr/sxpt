package com.sxpt.module.connector.vo;

/**
 * 原平台数据会话返回对象。
 *
 * 业务功能：
 * 1. 向原平台返回 DataSession 注册后的教学平台绑定信息。
 * 2. 为遮罩层初始化提供 dataSessionId、dataInstanceId 和业务场景上下文。
 *
 * 关键流程：
 * 1. 原平台注册业务数据后接收该对象。
 * 2. 原平台把其中的 dataSessionId 注入页面或遮罩层。
 * 3. 遮罩层后续上报事件时携带这些字段完成过程证据绑定。
 */
public class OriginDataSessionVO {

    private String dataSessionId;

    private String dataInstanceId;

    private String launchContextId;

    private String connectorSystemId;

    private String sceneType;

    private String sdkMode;

    private String businessSceneCode;

    private String questionAttemptId;

    private String externalBusinessId;

    private String sessionStatus;

    public String getDataSessionId() {
        return dataSessionId;
    }

    public void setDataSessionId(String dataSessionId) {
        this.dataSessionId = dataSessionId;
    }

    public String getDataInstanceId() {
        return dataInstanceId;
    }

    public void setDataInstanceId(String dataInstanceId) {
        this.dataInstanceId = dataInstanceId;
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

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getSdkMode() {
        return sdkMode;
    }

    public void setSdkMode(String sdkMode) {
        this.sdkMode = sdkMode;
    }

    public String getBusinessSceneCode() {
        return businessSceneCode;
    }

    public void setBusinessSceneCode(String businessSceneCode) {
        this.businessSceneCode = businessSceneCode;
    }

    public String getQuestionAttemptId() {
        return questionAttemptId;
    }

    public void setQuestionAttemptId(String questionAttemptId) {
        this.questionAttemptId = questionAttemptId;
    }

    public String getExternalBusinessId() {
        return externalBusinessId;
    }

    public void setExternalBusinessId(String externalBusinessId) {
        this.externalBusinessId = externalBusinessId;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }
}
