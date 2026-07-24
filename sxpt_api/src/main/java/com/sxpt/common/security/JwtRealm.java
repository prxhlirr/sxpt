package com.sxpt.common.security;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.stereotype.Component;

/**
 * JWT Realm 骨架。
 *
 * 业务功能：
 * 1. 使用 JWT 完成 Shiro 认证。
 * 2. 为后续接入用户表、角色、权限点预留授权扩展位置。
 *
 * 关键流程：
 * 1. 判断是否支持 JwtAuthenticationToken。
 * 2. 调用 JwtService 校验 Token 并解析用户身份。
 * 3. 授权阶段暂不加载权限，后续接用户模块时补齐。
 */
@Component
public class JwtRealm extends AuthorizingRealm {

    private final JwtService jwtService;

    public JwtRealm(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        return token instanceof JwtAuthenticationToken;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return null;
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        JwtPrincipal principal = jwtService.parseToken((String) token.getCredentials());
        return new SimpleAuthenticationInfo(principal, token.getCredentials(), getName());
    }
}
