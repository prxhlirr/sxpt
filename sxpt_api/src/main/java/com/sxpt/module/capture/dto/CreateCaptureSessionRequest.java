package com.sxpt.module.capture.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 创建备案采集会话请求。
 *
 * 业务功能：
 * 1. 承载教师从教学平台点击开始备案时提交的会话信息。
 * 2. 使用 Bean Validation 在 Controller 层提前拦截缺失和超长字段。
 *
 * 关键流程：
 * 1. Controller 接收请求并触发基础参数校验。
 * 2. Controller 将请求转换为 CaptureSession 实体后交给 Service 写入。
 */
public class CreateCaptureSessionRequest {

    @NotBlank(message = "租户 ID 不能为空")
    @Size(max = 64, message = "租户 ID 长度不能超过 64")
    private String tenantId;

    @NotBlank(message = "原平台配置 ID 不能为空")
    @Size(max = 64, message = "原平台配置 ID 长度不能超过 64")
    private String connectorSystemId;

    @NotBlank(message = "教师用户 ID 不能为空")
    @Size(max = 64, message = "教师用户 ID 长度不能超过 64")
    private String teacherId;

    @NotBlank(message = "会话名称不能为空")
    @Size(max = 128, message = "会话名称长度不能超过 128")
    private String sessionName;

    @Size(max = 128, message = "业务名称长度不能超过 128")
    private String businessName;

    @Size(max = 32, message = "采集模式长度不能超过 32")
    private String captureMode;

    @NotBlank(message = "开始地址不能为空")
    @Size(max = 1024, message = "开始地址长度不能超过 1024")
    private String startUrl;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getConnectorSystemId() {
        return connectorSystemId;
    }

    public void setConnectorSystemId(String connectorSystemId) {
        this.connectorSystemId = connectorSystemId;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public String getSessionName() {
        return sessionName;
    }

    public void setSessionName(String sessionName) {
        this.sessionName = sessionName;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getCaptureMode() {
        return captureMode;
    }

    public void setCaptureMode(String captureMode) {
        this.captureMode = captureMode;
    }

    public String getStartUrl() {
        return startUrl;
    }

    public void setStartUrl(String startUrl) {
        this.startUrl = startUrl;
    }
}
