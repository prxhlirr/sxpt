package com.sxpt.common.security;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 当前用户上下文。
 *
 * 业务功能：
 * 1. 保存本次请求经过 JWT 认证后得到的可信用户身份。
 * 2. 为 Service 层、审计日志、对象级授权提供统一的当前操作者来源。
 *
 * 关键流程：
 * 1. JwtAuthInterceptor 认证通过后调用 set 写入当前线程。
 * 2. 业务代码通过 getRequiredUser 读取当前用户，避免继续信任前端传入的 operatorId。
 * 3. 请求结束或认证失败时必须 clear，避免 Web 容器线程复用造成身份串用。
 */
public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<CurrentUser>();

    private CurrentUserContext() {
    }

    /**
     * 写入当前请求用户。
     *
     * @param currentUser 当前认证用户，必须来自服务端认证链路。
     */
    public static void set(CurrentUser currentUser) {
        if (currentUser == null || !StringUtils.hasText(currentUser.getUserId())) {
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
        HOLDER.set(currentUser);
    }

    /**
     * 获取当前请求用户。
     *
     * @return 当前用户可选值，未认证请求为空。
     */
    public static Optional<CurrentUser> get() {
        return Optional.ofNullable(HOLDER.get());
    }

    /**
     * 获取必须存在的当前请求用户。
     *
     * @return 当前认证用户。
     */
    public static CurrentUser getRequiredUser() {
        return get().orElseThrow(() -> new BusinessException(ApiResultCode.UNAUTHORIZED));
    }

    /**
     * 清理当前线程用户。
     */
    public static void clear() {
        HOLDER.remove();
    }

    /**
     * 当前认证用户。
     *
     * 业务功能：
     * 1. 表达服务端已认证的最小用户身份。
     * 2. 后续可扩展 tenantId、roles、orgIds、identityBindings 等上下文字段。
     *
     * 关键流程：
     * 1. 当前阶段先由 JwtPrincipal 转换生成。
     * 2. auth/me 和核心业务服务优先读取该对象，而不是读取前端传参。
     */
    public static final class CurrentUser {

        private final String userId;

        private final String username;

        public CurrentUser(String userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public String getUserId() {
            return userId;
        }

        public String getUsername() {
            return username;
        }
    }
}
