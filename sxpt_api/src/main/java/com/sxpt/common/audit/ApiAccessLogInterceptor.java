package com.sxpt.common.audit;

import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.common.trace.TraceIdContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * API 访问日志拦截器。
 *
 * 业务功能：
 * 1. 在开发环境记录每一次后端 API 调用，帮助确认前端操作是否真正进入后端。
 * 2. 输出 traceId、请求方法、接口路径、响应状态、耗时和认证用户边界，便于把前端操作、后端接口和 SQL 日志串起来。
 *
 * 关键流程：
 * 1. 请求进入 Controller 前记录开始时间。
 * 2. 请求完成后读取响应状态、traceId 和当前认证用户。
 * 3. 日志不打印请求体、Token、密码或查询参数，避免开发期排查时泄漏敏感信息。
 */
public class ApiAccessLogInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiAccessLogInterceptor.class);

    private static final String START_TIME_ATTRIBUTE = ApiAccessLogInterceptor.class.getName() + ".startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME_ATTRIBUTE, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        long durationMs = resolveDurationMs(request);
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.get().orElse(null);
        LOGGER.info("api_access traceId={} method={} uri={} status={} durationMs={} userId={} tenantId={}",
                defaultText(TraceIdContext.getTraceId()),
                defaultText(request.getMethod()),
                defaultText(request.getRequestURI()),
                response.getStatus(),
                durationMs,
                currentUser == null ? "-" : defaultText(currentUser.getUserId()),
                currentUser == null ? "-" : defaultText(currentUser.getTenantId()));
    }

    private long resolveDurationMs(HttpServletRequest request) {
        Object startTime = request.getAttribute(START_TIME_ATTRIBUTE);
        if (startTime instanceof Long) {
            return Math.max(0L, System.currentTimeMillis() - (Long) startTime);
        }
        return 0L;
    }

    private String defaultText(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}
