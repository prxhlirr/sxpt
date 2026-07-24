package com.sxpt.common.trace;

/**
 * traceId 线程上下文。
 *
 * 业务功能：
 * 1. 在当前请求线程中保存 traceId。
 * 2. 为业务代码和审计日志提供统一的链路标识读取方式。
 *
 * 关键流程：
 * 1. 拦截器在请求进入时写入 traceId。
 * 2. 请求结束后清理 ThreadLocal，避免线程复用造成串号。
 */
public final class TraceIdContext {

    private static final ThreadLocal<String> TRACE_ID_HOLDER = new ThreadLocal<String>();

    private TraceIdContext() {
    }

    public static void setTraceId(String traceId) {
        TRACE_ID_HOLDER.set(traceId);
    }

    public static String getTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    public static void clear() {
        TRACE_ID_HOLDER.remove();
    }
}
