package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 标记原平台启动失败请求。
 *
 * 业务功能：
 * 1. 承载原平台启动失败时回写 FAILED 状态和失败原因所需参数。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长失败原因。
 *
 * 关键流程：
 * 1. 原平台在 token 校验、身份切换或 session 建立失败时提交该请求。
 * 2. Controller 调用 Service 写入 FAILED 状态和 errorMessage，便于后续审计。
 */
public class MarkPlatformLaunchFailedRequest {

    @NotBlank(message = "启动上下文 ID 不能为空")
    @Size(max = 64, message = "启动上下文 ID 长度不能超过 64")
    private String id;

    @NotBlank(message = "启动失败原因不能为空")
    @Size(max = 2048, message = "启动失败原因长度不能超过 2048")
    private String errorMessage;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
