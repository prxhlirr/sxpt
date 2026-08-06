package com.sxpt.module.teachingdata.vo;

import java.time.LocalDateTime;

/** Result of provisioning and launching a teacher authoring data instance. */
public class AuthoringLaunchVO {

    private String tenantId;
    private String launchContextId;
    private String launchToken;
    private String dataInstanceId;
    private String redirectUrl;
    private String launchUrl;
    private LocalDateTime expireTime;

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

    public String getLaunchToken() {
        return launchToken;
    }

    public void setLaunchToken(String launchToken) {
        this.launchToken = launchToken;
    }

    public String getDataInstanceId() {
        return dataInstanceId;
    }

    public void setDataInstanceId(String dataInstanceId) {
        this.dataInstanceId = dataInstanceId;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getLaunchUrl() {
        return launchUrl;
    }

    public void setLaunchUrl(String launchUrl) {
        this.launchUrl = launchUrl;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
}
