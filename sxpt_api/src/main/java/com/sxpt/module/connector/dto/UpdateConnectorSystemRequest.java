package com.sxpt.module.connector.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 更新原业务平台配置请求。
 *
 * 业务功能：
 * 1. 承载管理员编辑原平台接入配置时提交的可变参数。
 * 2. 不接收租户 ID 和原平台编码，避免更新时破坏后续 launchToken、SDK 和教学数据引用关系。
 *
 * 关键流程：
 * 1. Controller 接收请求体并触发基础参数校验。
 * 2. Controller 将请求转换为 ConnectorSystem 实体后交给 Service 执行存在性校验和更新。
 */
public class UpdateConnectorSystemRequest {

    @NotBlank(message = "原平台配置 ID 不能为空")
    @Size(max = 64, message = "原平台配置 ID 长度不能超过 64")
    private String id;

    @NotBlank(message = "原平台名称不能为空")
    @Size(max = 128, message = "原平台名称长度不能超过 128")
    private String systemName;

    @NotBlank(message = "原平台类型不能为空")
    @Size(max = 64, message = "原平台类型长度不能超过 64")
    private String systemType;

    @NotBlank(message = "原平台基础地址不能为空")
    @Size(max = 512, message = "原平台基础地址长度不能超过 512")
    private String baseUrl;

    @NotBlank(message = "认证方式不能为空")
    @Size(max = 32, message = "认证方式长度不能超过 32")
    private String authType;

    @Size(max = 4096, message = "原平台配置 JSON 长度不能超过 4096")
    private String configJson;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = systemName;
    }

    public String getSystemType() {
        return systemType;
    }

    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }
}
