package com.sxpt.common.security;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.service.ExternalConnectorCredentialService;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 为 /api/v1/connector 下的 OA 系统调用识别 X-Connector-Key。
 * Header 缺失时，只有同时支持教学平台 JWT 的业务模块列表接口继续进入后续鉴权；
 * 其余连接器接口直接返回 401。
 */
public class ConnectorKeyAuthInterceptor implements HandlerInterceptor {

    private static final String CONNECTOR_KEY_HEADER = "X-Connector-Key";
    private static final String OPTIONS_METHOD = "OPTIONS";
    private static final String SHARED_BUSINESS_MODULE_PATH =
            "/api/v1/connector/business-modules";

    private final ExternalConnectorCredentialService credentialService;

    public ConnectorKeyAuthInterceptor(ExternalConnectorCredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (OPTIONS_METHOD.equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String connectorKey = request.getHeader(CONNECTOR_KEY_HEADER);
        if (!StringUtils.hasText(connectorKey)) {
            if (isSharedBusinessModuleRequest(request)) {
                return true;
            }
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
        ExternalConnectorContext.set(credentialService.authenticate(connectorKey));
        return true;
    }

    /**
     * 业务模块根列表同时服务教学平台登录用户和 OA 连接器。
     * 未携带连接器密钥时放行到 JwtAuthInterceptor，由 JWT 决定是否允许访问。
     */
    private boolean isSharedBusinessModuleRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        return (contextPath + SHARED_BUSINESS_MODULE_PATH).equals(requestUri);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        ExternalConnectorContext.clear();
    }
}
