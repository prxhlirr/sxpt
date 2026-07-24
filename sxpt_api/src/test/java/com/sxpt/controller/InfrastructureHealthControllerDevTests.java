package com.sxpt.controller;

import com.sxpt.SxptApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * dev 环境基础设施健康检查测试。
 *
 * 业务功能：
 * 1. 使用 application-dev.yml 中的真实 PostgreSQL 配置验证数据库连通性。
 * 2. 使用 application-dev.yml 中的真实 Redis 配置验证缓存连通性。
 *
 * 关键流程：
 * 1. 激活 dev profile。
 * 2. 通过 MockMvc 调用基础设施健康检查接口。
 * 3. 断言接口返回统一响应和 UP 状态。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class InfrastructureHealthControllerDevTests {

    @Autowired
    private MockMvc mockMvc;

    /**
     * 校验 PostgreSQL 连通性。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void dbHealthShouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/v1/system/db-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.database", is("PostgreSQL")))
                .andExpect(jsonPath("$.result.status", is("UP")));
    }

    /**
     * 校验 Redis 连通性。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void redisHealthShouldReturnUp() throws Exception {
        mockMvc.perform(get("/api/v1/system/redis-health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.result.redis", is("Redis")))
                .andExpect(jsonPath("$.result.status", is("UP")));
    }
}
