package com.sxpt.common.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 基于日志文件的审计服务实现。
 *
 * 业务功能：
 * 1. 在业务表结构未确定前，先通过日志记录审计行为。
 * 2. 保持 AuditLogService 接口稳定，后续落库时不影响业务调用方。
 *
 * 关键流程：
 * 1. 补齐审计时间。
 * 2. 输出结构化审计日志。
 */
@Service
public class LogbackAuditLogService implements AuditLogService {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("AUDIT_LOG");

    @Override
    public void record(AuditLog auditLog) {
        if (auditLog.getCreatedAt() == null) {
            auditLog.setCreatedAt(LocalDateTime.now());
        }
        AUDIT_LOGGER.info(
                "traceId={}, operatorId={}, operatorName={}, action={}, targetType={}, targetId={}, beforeStatus={}, afterStatus={}, ip={}, userAgent={}, createdAt={}",
                auditLog.getTraceId(),
                auditLog.getOperatorId(),
                auditLog.getOperatorName(),
                auditLog.getAction(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getBeforeStatus(),
                auditLog.getAfterStatus(),
                auditLog.getIp(),
                auditLog.getUserAgent(),
                auditLog.getCreatedAt()
        );
    }
}
