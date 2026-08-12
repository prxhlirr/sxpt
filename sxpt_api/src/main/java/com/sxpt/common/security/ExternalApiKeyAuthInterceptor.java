package com.sxpt.common.security;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.service.ExternalConnectorCredentialService;
import com.sxpt.module.connector.service.ExternalConnectorCredentialService.AuthenticatedExternalConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 第三方原平台 API Key 认证拦截器。
 *
 * 业务功能：
 * 1. 保护 /api/v1/external/** 外部接口，认证对象是原平台正式环境而不是教学平台用户。
 * 2. 通过 X-API-Key 反查教学平台生成的正式环境身份，形成可校验的系统级调用上下文。
 *
 * 关键流程：
 * 1. 读取 X-API-Key，请求缺失时返回未认证。
 * 2. 委托凭证服务校验哈希、状态、过期时间和正式环境边界。
 * 3. 认证成功后写入 ExternalConnectorContext，请求完成后清理。
 */
public class ExternalApiKeyAuthInterceptor implements HandlerInterceptor {

    private static final String API_KEY_HEADER = "X-API-Key";

    private static final String OPTIONS_METHOD = "OPTIONS";

    private final ExternalConnectorCredentialService credentialService;

    public ExternalApiKeyAuthInterceptor(ExternalConnectorCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (OPTIONS_METHOD.equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (!StringUtils.hasText(apiKey)) {
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
        AuthenticatedExternalConnector connector = credentialService.authenticate(apiKey);
        ExternalConnectorContext.set(connector);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ExternalConnectorContext.clear();
    }
}
