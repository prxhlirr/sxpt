package com.sxpt.common.exception;

import org.apache.shiro.authc.AuthenticationException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全局异常响应契约测试。
 */
class GlobalExceptionHandlerTests {

    @Test
    void expiredOrInvalidTokenShouldReturnUnauthorizedInsteadOfSystemError() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AuthenticationFailureController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/test/authentication-failure"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("用户未登录"));
    }

    @RestController
    static class AuthenticationFailureController {

        @GetMapping("/test/authentication-failure")
        public void fail() {
            throw new AuthenticationException("expired token");
        }
    }
}
