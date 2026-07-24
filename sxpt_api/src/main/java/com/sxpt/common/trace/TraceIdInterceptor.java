package com.sxpt.common.trace;

import com.sxpt.common.constant.TraceConstants;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * traceId 请求拦截器。
 *
 * 业务功能：
 * 1. 为每个请求建立唯一链路标识。
 * 2. 将 traceId 写入日志 MDC 和响应头，方便接口问题定位。
 *
 * 关键流程：
 * 1. 优先复用调用方传入的 X-Trace-Id。
 * 2. 未传入时由服务端生成 UUID。
 * 3. 请求结束后清理 MDC 和 ThreadLocal。
 */
public class TraceIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = request.getHeader(TraceConstants.TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        TraceIdContext.setTraceId(traceId);
        MDC.put(TraceConstants.TRACE_ID, traceId);
        response.setHeader(TraceConstants.TRACE_ID_HEADER, traceId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        MDC.remove(TraceConstants.TRACE_ID);
        TraceIdContext.clear();
    }
}
