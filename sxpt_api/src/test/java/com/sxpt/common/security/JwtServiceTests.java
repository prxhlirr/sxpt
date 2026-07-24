package com.sxpt.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * JWT 服务测试。
 *
 * 业务功能：
 * 1. 验证 Token 可以被正确生成。
 * 2. 验证 Token 可以解析出用户身份。
 */
@SpringBootTest
@ActiveProfiles("test")
class JwtServiceTests {

    @Autowired
    private JwtService jwtService;

    /**
     * 校验生成和解析 Token 的闭环。
     */
    @Test
    void tokenShouldBeGeneratedAndParsed() {
        String token = jwtService.generateToken("10001", "admin");
        JwtPrincipal principal = jwtService.parseToken(token);

        assertNotNull(token);
        assertEquals("10001", principal.getUserId());
        assertEquals("admin", principal.getUsername());
    }
}
