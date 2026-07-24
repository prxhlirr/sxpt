package com.sxpt.common.audit;

/**
 * 审计日志服务。
 *
 * 业务功能：
 * 1. 为关键业务动作提供统一审计入口。
 * 2. 隔离审计记录方式，后续可从日志输出平滑切换为数据库落库。
 *
 * 关键流程：
 * 1. 业务服务构造 AuditLog。
 * 2. 调用 record 方法记录审计信息。
 */
public interface AuditLogService {

    /**
     * 记录审计日志。
     *
     * @param auditLog 审计日志。
     */
    void record(AuditLog auditLog);
}
