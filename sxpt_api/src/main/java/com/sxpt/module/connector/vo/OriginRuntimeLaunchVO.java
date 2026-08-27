package com.sxpt.module.connector.vo;

import java.time.LocalDateTime;

/**
 * 原平台运行时启动返回对象。
 *
 * 业务功能：
 * 1. 向教学平台前端返回进入原平台首页所需的 launchUrl 和上下文摘要。
 * 2. 隐藏 token hash，只暴露本次跳转必须携带的一次性明文 token。
 *
 * 关键流程：
 * 1. OriginRuntimeLaunchService 创建平台首页启动上下文。
 * 2. Controller 将 launchToken 拼入原平台教学入口 URL。
 * 3. 前端跳转 launchUrl，原平台后续通过 verify 获取教学上下文。
 */
public class OriginRuntimeLaunchVO {

    private String launchContextId;

    private String launchToken;

    private String launchUrl;

    private String originHomeUrl;

    private String connectorSystemId;

    private String sceneType;

    private String sdkMode;

    private String actorType;

    private LocalDateTime expireTime;

    public String getLaunchContextId() {
        return launchContextId;
    }

    public void setLaunchContextId(String launchContextId) {
        this.launchContextId = launchContextId;
    }

    public String getLaunchToken() {
        return launchToken;
    }

    public void setLaunchToken(String launchToken) {
        this.launchToken = launchToken;
    }

    public String getLaunchUrl() {
        return launchUrl;
    }

    public void setLaunchUrl(String launchUrl) {
        this.launchUrl = launchUrl;
    }

    public String getOriginHomeUrl() {
        return originHomeUrl;
    }

    public void setOriginHomeUrl(String originHomeUrl) {
        this.originHomeUrl = originHomeUrl;
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

    public String getActorType() {
        return actorType;
    }

    public void setActorType(String actorType) {
        this.actorType = actorType;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
}
