package com.sxpt.controller;

import com.sxpt.common.security.JwtService;
import com.sxpt.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 测试 Token 控制器环境隔离测试。
 *
 * 业务功能：
 * 1. 验证测试 Token 签发入口只在联调环境注册。
 * 2. 避免生产环境因误暴露接口导致任意用户身份可被伪造。
 *
 * 关键流程：
 * 1. 使用轻量级 Spring 上下文分别启动 test 和 prod profile。
 * 2. 校验 AuthController 在 test 中存在、在 prod 中不存在。
 */
class AuthControllerProfileTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(AuthControllerProfileTestConfig.class)
            .withPropertyValues(
                    "jwt.secret=sxpt_test_jwt_secret",
                    "jwt.expire-seconds=7200"
            );

    /**
     * 校验测试环境保留 Token 签发能力，避免影响本地联调和既有安全测试。
     */
    @Test
    void testProfileShouldRegisterAuthController() {
        contextRunner
                .withPropertyValues("spring.profiles.active=test")
                .run(context -> assertThat(context).hasSingleBean(AuthController.class));
    }

    /**
     * 校验生产环境不注册测试 Token 控制器，因为生产身份必须来自正式登录或 SSO。
     */
    @Test
    void prodProfileShouldNotRegisterAuthController() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).doesNotHaveBean(AuthController.class));
    }

    @Configuration
    @Import({AuthController.class, JwtService.class, JwtProperties.class})
    static class AuthControllerProfileTestConfig {
    }
}
