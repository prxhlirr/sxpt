package com.sxpt.config;

import com.sxpt.common.audit.ApiAccessLogInterceptor;
import com.sxpt.common.trace.TraceIdInterceptor;
import com.sxpt.common.idempotent.IdempotentInterceptor;
import com.sxpt.common.security.AuthenticatedUserContextService;
import com.sxpt.common.security.JwtAuthInterceptor;
import org.apache.shiro.mgt.SecurityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

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

    private final AuthenticatedUserContextService authenticatedUserContextService;

    private final String[] allowedOrigins;

    private final boolean accessLogEnabled;

    public WebConfig(SecurityManager securityManager,
                     StringRedisTemplate stringRedisTemplate,
                     AuthenticatedUserContextService authenticatedUserContextService,
                     @Value("${sxpt.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
                     String allowedOrigins,
                     @Value("${sxpt.access-log.enabled:false}") boolean accessLogEnabled) {
        this.securityManager = securityManager;
        this.stringRedisTemplate = stringRedisTemplate;
        this.authenticatedUserContextService = authenticatedUserContextService;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
        this.accessLogEnabled = accessLogEnabled;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("X-Trace-Id", "Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置前后端分离开发环境跨域。
     *
     * 业务功能：
     * 1. 允许 sxpt_web 在 Vite 开发端口访问 sxpt_api。
     * 2. 支持登录、后续携带 Authorization 的业务请求和浏览器预检请求。
     *
     * 关键流程：
     * 1. 浏览器从 `127.0.0.1:5173` 发起跨域请求。
     * 2. Spring MVC 在进入 Controller 前返回 CORS 响应头。
     * 3. 预检通过后浏览器才会发送真实登录或业务请求。
     */
    private void addLegacyLocalDevCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://127.0.0.1:5173",
                        "http://localhost:5173",
                        "http://127.0.0.1:5174",
                        "http://localhost:5174",
                        "http://127.0.0.1:5175",
                        "http://localhost:5175"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new TraceIdInterceptor()).addPathPatterns("/**");
        registry.addInterceptor(new IdempotentInterceptor(stringRedisTemplate)).addPathPatterns("/**");
        registry.addInterceptor(new JwtAuthInterceptor(securityManager, authenticatedUserContextService))
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/system/**",
                        "/api/v1/auth/login",
                        "/api/v1/auth/token",
                        // 原平台凭一次性 launchToken 校验并回写启动状态，不使用教学平台登录 JWT。
                        "/api/v1/connector/launch-contexts/verify",
                        "/api/v1/connector/launch-contexts/used",
                        "/api/v1/connector/launch-contexts/failed",
                        "/api/v1/teaching-attachments/*/content",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/webjars/**",
                        "/v2/api-docs"
                );
        if (accessLogEnabled) {
            registry.addInterceptor(new ApiAccessLogInterceptor()).addPathPatterns("/api/v1/**");
        }
    }
}
