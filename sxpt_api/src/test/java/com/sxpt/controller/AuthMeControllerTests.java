package com.sxpt.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.SxptApiApplication;
import com.sxpt.common.api.ApiResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 当前登录人接口测试。
 *
 * 业务功能：
 * 1. 验证 auth/me 必须经过 JWT 认证。
 * 2. 验证 auth/me 返回的是服务端认证上下文中的用户，而不是前端传入身份。
 *
 * 关键流程：
 * 1. 无 Token 请求应返回统一未登录响应。
 * 2. 合法 Token 请求应返回 JwtAuthInterceptor 写入的 CurrentUserContext。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthMeControllerTests {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 校验 auth/me 不再被 /api/v1/auth/** 整体放行影响。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void meShouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(ApiResultCode.UNAUTHORIZED.getCode())));
    }

    /**
     * 校验 auth/me 返回 JWT 认证主体中的用户信息。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void meShouldReturnCurrentUserFromJwtContext() throws Exception {
        String token = issueToken();

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(AUTHORIZATION_HEADER, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.result.userId", is("10001")))
                .andExpect(jsonPath("$.result.username", is("teacher001")))
                .andExpect(jsonPath("$.result.roles").isArray())
                .andExpect(jsonPath("$.result.orgIds").isArray())
                .andExpect(jsonPath("$.result.identityBindings").isArray());
    }

    private String issueToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/token")
                        .param("userId", "10001")
                        .param("username", "teacher001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode rootNode = objectMapper.readTree(response);
        return rootNode.path("result").path("token").asText();
    }
}
