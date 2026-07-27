package com.sxpt.common.security;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 密码哈希服务测试。
 *
 * 业务功能：
 * 1. 验证正式登录密码不会使用明文或 salt + md5 存储。
 * 2. 验证同一密码在同一 salt 下可校验，在不同 salt 下产生不同哈希。
 *
 * 关键流程：
 * 1. 生成 salt 后派生 PBKDF2 哈希。
 * 2. 使用正确密码和错误密码分别执行匹配。
 */
class PasswordHashServiceTests {

    private final PasswordHashService passwordHashService = new PasswordHashService();

    /**
     * 校验相同密码和 salt 可以通过密码匹配。
     */
    @Test
    void shouldMatchPasswordWithSameSaltAndIterations() {
        String salt = passwordHashService.generateSalt();
        String hash = passwordHashService.hash("StrongPassword123", salt);

        assertThat(hash).isNotEqualTo("StrongPassword123");
        assertThat(passwordHashService.matches(
                "StrongPassword123",
                salt,
                hash,
                PasswordHashService.DEFAULT_ITERATIONS
        )).isTrue();
    }

    /**
     * 校验错误密码无法通过匹配。
     */
    @Test
    void shouldRejectWrongPassword() {
        String salt = passwordHashService.generateSalt();
        String hash = passwordHashService.hash("StrongPassword123", salt);

        assertThat(passwordHashService.matches(
                "WrongPassword123",
                salt,
                hash,
                PasswordHashService.DEFAULT_ITERATIONS
        )).isFalse();
    }

    /**
     * 校验每用户独立 salt 会让相同密码生成不同哈希。
     */
    @Test
    void shouldGenerateDifferentHashWithDifferentSalt() {
        String firstHash = passwordHashService.hash("StrongPassword123", passwordHashService.generateSalt());
        String secondHash = passwordHashService.hash("StrongPassword123", passwordHashService.generateSalt());

        assertThat(firstHash).isNotEqualTo(secondHash);
    }

    /**
     * 校验空密码不会进入哈希流程。
     */
    @Test
    void shouldRejectBlankPassword() {
        assertThatThrownBy(() -> passwordHashService.hash(" ", passwordHashService.generateSalt()))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo(ApiResultCode.PARAM_ERROR.getCode());
    }
}
