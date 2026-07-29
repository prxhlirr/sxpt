package com.sxpt.common.security;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * JWT 认证拦截器。
 *
 * 业务功能：
 * 1. 对受保护 API 执行 JWT 认证。
 * 2. 将认证委托给 Shiro Realm，避免拦截器承载业务权限逻辑。
 *
 * 关键流程：
 * 1. 从 Authorization 请求头读取 Bearer Token。
 * 2. 使用 Shiro Subject 执行登录认证。
 * 3. 请求完成后解除当前线程的 Shiro 上下文绑定。
 */
public class JwtAuthInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String BEARER_PREFIX = "Bearer ";

    private static final String OPTIONS_METHOD = "OPTIONS";

    private final SecurityManager securityManager;

    public JwtAuthInterceptor(SecurityManager securityManager) {
        this.securityManager = securityManager;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (OPTIONS_METHOD.equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
        try {
            ThreadContext.bind(securityManager);
            Subject subject = new Subject.Builder(securityManager).buildSubject();
            subject.login(new JwtAuthenticationToken(token));
            ThreadContext.bind(subject);
            JwtPrincipal principal = (JwtPrincipal) SecurityUtils.getSubject().getPrincipal();
            CurrentUserContext.set(new CurrentUserContext.CurrentUser(principal.getUserId(), principal.getUsername()));
            return true;
        } catch (RuntimeException ex) {
            CurrentUserContext.clear();
            ThreadContext.unbindSubject();
            ThreadContext.unbindSecurityManager();
            throw ex;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
        ThreadContext.unbindSubject();
        ThreadContext.unbindSecurityManager();
    }

    private String resolveToken(HttpServletRequest request) {
        String authorization = request.getHeader(AUTHORIZATION_HEADER);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            return null;
        }
        return authorization.substring(BEARER_PREFIX.length());
    }
}
