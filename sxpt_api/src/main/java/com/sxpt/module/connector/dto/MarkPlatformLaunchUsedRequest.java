package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 标记原平台启动已使用请求。
 *
 * 业务功能：
 * 1. 承载原平台完成 session 建立后回写 USED 状态所需的启动上下文 ID。
 * 2. 使用 Bean Validation 在 Controller 层拦截空 ID 和超长 ID。
 *
 * 关键流程：
 * 1. 原平台 verify 成功并建立 session 后提交该请求。
 * 2. Controller 调用 Service 将 VERIFIED 状态推进为 USED。
 */
public class MarkPlatformLaunchUsedRequest {

    @NotBlank(message = "启动上下文 ID 不能为空")
    @Size(max = 64, message = "启动上下文 ID 长度不能超过 64")
    private String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
