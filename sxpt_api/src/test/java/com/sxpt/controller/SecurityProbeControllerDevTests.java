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

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 安全探测接口 dev 测试。
 *
 * 业务功能：
 * 1. 验证 JWT 拦截器能拒绝未认证请求。
 * 2. 验证合法 Token 能访问受保护接口。
 * 3. 验证 Redis 幂等组件能拒绝重复请求。
 */
@SpringBootTest(classes = SxptApiApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class SecurityProbeControllerDevTests {

    private static final String AUTHORIZATION_HEADER = "Authorization";

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 校验受保护接口在无 Token 时返回未登录错误。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void protectedApiShouldRejectRequestWithoutToken() throws Exception {
        mockMvc.perform(post("/api/v1/security-probe/protected"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(ApiResultCode.UNAUTHORIZED.getCode())));
    }

    /**
     * 校验受保护接口在 Token 正确时允许访问。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void protectedApiShouldAcceptRequestWithToken() throws Exception {
        String token = issueToken();

        mockMvc.perform(post("/api/v1/security-probe/protected")
                        .header(AUTHORIZATION_HEADER, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(ApiResultCode.SUCCESS.getCode())))
                .andExpect(jsonPath("$.result.authenticated", is(true)));
    }

    /**
     * 校验幂等写接口首次请求成功、重复请求失败。
     *
     * @throws Exception MockMvc 请求异常由测试框架处理。
     */
    @Test
    void idempotentApiShouldRejectDuplicateRequest() throws Exception {
        String token = issueToken();
        String idempotencyKey = UUID.randomUUID().toString();

        mockMvc.perform(post("/api/v1/security-probe/idempotent")
                        .header(AUTHORIZATION_HEADER, "Bearer " + token)
                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.code", is(ApiResultCode.SUCCESS.getCode())));

        mockMvc.perform(post("/api/v1/security-probe/idempotent")
                        .header(AUTHORIZATION_HEADER, "Bearer " + token)
                        .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.code", is(ApiResultCode.IDEMPOTENCY_CONFLICT.getCode())));
    }

    private String issueToken() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/token")
                        .param("userId", "10001")
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode rootNode = objectMapper.readTree(response);
        return rootNode.path("result").path("token").asText();
    }
}
