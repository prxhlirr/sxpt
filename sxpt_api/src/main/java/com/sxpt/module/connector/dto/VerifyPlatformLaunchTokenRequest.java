package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 校验原平台启动令牌请求。
 *
 * 业务功能：
 * 1. 承载原平台 verify 接口提交的租户 ID 和明文 launchToken。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长 token。
 *
 * 关键流程：
 * 1. 原平台从跳转入口拿到明文 launchToken 后提交该请求。
 * 2. Controller 将参数交给 Service，Service 计算 hash 后查询启动上下文。
 */
public class VerifyPlatformLaunchTokenRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "启动令牌不能为空")
    @Size(max = 256, message = "启动令牌长度不能超过 256")
    private String launchToken;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getLaunchToken() {
        return launchToken;
    }

    public void setLaunchToken(String launchToken) {
        this.launchToken = launchToken;
    }
}
