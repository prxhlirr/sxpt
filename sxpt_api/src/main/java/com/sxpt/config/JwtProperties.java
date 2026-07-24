package com.sxpt.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性。
 *
 * 业务功能：
 * 1. 从 application.yml 或 application-dev.yml 读取 JWT 密钥和有效期。
 * 2. 避免认证代码中硬编码密钥、过期时间等策略参数。
 *
 * 关键流程：
 * 1. Spring 启动时绑定 jwt 配置。
 * 2. JwtService 使用该配置生成和校验 Token。
 */
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;

    private Long expireSeconds;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Long getExpireSeconds() {
        return expireSeconds;
    }

    public void setExpireSeconds(Long expireSeconds) {
        this.expireSeconds = expireSeconds;
    }
}
