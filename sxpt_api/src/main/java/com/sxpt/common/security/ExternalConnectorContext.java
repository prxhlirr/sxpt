package com.sxpt.common.security;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.service.ExternalConnectorCredentialService.AuthenticatedExternalConnector;

import java.util.Optional;

/**
 * 第三方原平台调用上下文。
 *
 * 业务功能：
 * 1. 在一次外部接口请求内保存已通过 API Key 认证的原平台正式环境身份。
 * 2. 避免 Controller 重复解析请求头和凭证，同时保证业务层能拿到可信边界。
 *
 * 关键流程：
 * 1. 外部认证拦截器认证成功后写入 ThreadLocal。
 * 2. 请求结束后必须清理，防止线程复用导致身份串扰。
 */
public final class ExternalConnectorContext {

    private static final ThreadLocal<AuthenticatedExternalConnector> HOLDER = new ThreadLocal<>();

    private ExternalConnectorContext() {
    }

    public static void set(AuthenticatedExternalConnector connector) {
        HOLDER.set(connector);
    }

    public static Optional<AuthenticatedExternalConnector> get() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static AuthenticatedExternalConnector require() {
        return get().orElseThrow(() -> new BusinessException(ApiResultCode.UNAUTHORIZED));
    }

    public static void clear() {
        HOLDER.remove();
    }
}
