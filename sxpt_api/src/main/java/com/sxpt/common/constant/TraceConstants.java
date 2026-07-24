package com.sxpt.common.constant;

/**
 * 链路追踪常量。
 *
 * 业务功能：
 * 1. 统一请求链路标识的 Header 和 MDC 字段名。
 * 2. 避免拦截器、日志、响应头中散落魔法字符串。
 *
 * 关键流程：
 * 1. 请求进入系统时读取或生成 traceId。
 * 2. traceId 写入 MDC，供日志自动携带。
 * 3. 响应返回时写入 Header，方便调用方定位问题。
 */
public final class TraceConstants {

    public static final String TRACE_ID = "traceId";

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private TraceConstants() {
    }
}
