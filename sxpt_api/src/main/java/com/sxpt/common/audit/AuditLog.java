package com.sxpt.common.audit;

import java.time.LocalDateTime;

/**
 * 审计日志模型。
 *
 * 业务功能：
 * 1. 表达审批、归档、发号、权限变更等关键动作的审计信息。
 * 2. 为后续落库或推送日志平台提供统一数据结构。
 *
 * 关键流程：
 * 1. 业务动作完成后构建 AuditLog。
 * 2. 调用 AuditLogService 记录审计信息。
 */
public class AuditLog {

    private String traceId;

    private String operatorId;

    private String operatorName;

    private String action;

    private String targetType;

    private String targetId;

    private String beforeStatus;

    private String afterStatus;

    private String ip;

    private String userAgent;

    private LocalDateTime createdAt;

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(String operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getBeforeStatus() {
        return beforeStatus;
    }

    public void setBeforeStatus(String beforeStatus) {
        this.beforeStatus = beforeStatus;
    }

    public String getAfterStatus() {
        return afterStatus;
    }

    public void setAfterStatus(String afterStatus) {
        this.afterStatus = afterStatus;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
