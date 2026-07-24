package com.sxpt.config;

import com.sxpt.common.trace.TraceIdInterceptor;
import com.sxpt.common.idempotent.IdempotentInterceptor;
import com.sxpt.common.security.JwtAuthInterceptor;
import org.apache.shiro.mgt.SecurityManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 层通用配置。
 *
 * 业务功能：
 * 1. 注册请求级公共能力，避免每个 Controller 重复处理。
 * 2. 当前用于接入 traceId，后续可扩展统一日志、审计前置能力。
 *
 * 关键流程：
 * 1. Spring MVC 初始化时注册拦截器。
 * 2. 每个请求进入 Controller 前先建立 traceId。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final SecurityManager securityManager;

    private final StringRedisTemplate stringRedisTemplate;

    public WebConfig(SecurityManager securityManager, StringRedisTemplate stringRedisTemplate) {
        this.securityManager = securityManager;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TraceIdInterceptor()).addPathPatterns("/**");
        registry.addInterceptor(new IdempotentInterceptor(stringRedisTemplate)).addPathPatterns("/**");
        registry.addInterceptor(new JwtAuthInterceptor(securityManager))
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/system/**",
                        "/api/v1/auth/**",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v2/api-docs"
                );
    }
}
