package com.sxpt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 应用启动测试。
 *
 * 业务功能：
 * 1. 验证 Spring Boot 应用上下文可以正常加载。
 * 2. 作为项目骨架的最小回归测试，避免基础配置破坏启动流程。
 */
@SpringBootTest
@ActiveProfiles("test")
class SxptApiApplicationTests {

    /**
     * 校验应用上下文加载。
     */
    @Test
    void contextLoads() {
    }
}
