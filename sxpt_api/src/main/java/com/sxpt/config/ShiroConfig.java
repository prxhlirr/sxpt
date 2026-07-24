package com.sxpt.config;

import com.sxpt.common.security.JwtRealm;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shiro 基础配置。
 *
 * 业务功能：
 * 1. 注册 Shiro SecurityManager 和 JWT Realm。
 * 2. 保持认证逻辑集中在 Realm 中，避免分散到业务接口。
 *
 * 关键流程：
 * 1. Spring 启动时创建 SecurityManager。
 * 2. SecurityManager 持有 JwtRealm，用于后续认证和授权扩展。
 */
@Configuration
public class ShiroConfig {

    /**
     * 创建 Shiro 安全管理器。
     *
     * @param jwtRealm JWT Realm。
     * @return Shiro SecurityManager。
     */
    @Bean
    public SecurityManager securityManager(JwtRealm jwtRealm) {
        DefaultWebSecurityManager securityManager = new DefaultWebSecurityManager();
        securityManager.setRealm(jwtRealm);
        return securityManager;
    }
}
