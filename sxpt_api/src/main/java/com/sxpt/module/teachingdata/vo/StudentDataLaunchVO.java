package com.sxpt.module.teachingdata.vo;

import com.sxpt.module.teachingdata.entity.DataInstanceAllocation;

import java.time.LocalDateTime;

/**
 * 学生进入原平台办理的启动结果。
 *
 * 业务功能：
 * 1. 返回一次性 launchToken 和带 token 的跳转地址。
 * 2. 同步返回分配记录快照，便于学生端展示本次使用的数据名称、办理步骤、单位和角色。
 *
 * 关键流程：
 * 1. Service 创建 platform_launch_context 后组装本对象。
 * 2. 前端使用 launchUrl 打开原平台，原平台再通过 verify 接口换取可信上下文。
 */
public class StudentDataLaunchVO {

    private String launchContextId;

    private String launchToken;

    private String launchUrl;

    private String targetUrl;

    private LocalDateTime expireTime;

    private DataInstanceAllocation allocation;

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

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }

    public DataInstanceAllocation getAllocation() {
        return allocation;
    }

    public void setAllocation(DataInstanceAllocation allocation) {
        this.allocation = allocation;
    }
}
