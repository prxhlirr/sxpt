package com.sxpt.common.security;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码哈希服务。
 *
 * 业务功能：
 * 1. 为教学平台正式账号密码登录提供密码哈希、salt 生成和密码校验能力。
 * 2. 使用 PBKDF2 替代 salt + md5，避免数据库泄露后密码被高速离线撞库。
 *
 * 关键流程：
 * 1. 创建或重置密码时生成每用户独立 salt。
 * 2. 使用 PBKDF2WithHmacSHA256 按固定迭代次数派生密码哈希。
 * 3. 登录时使用相同参数重新派生并做恒定时间比对。
 */
@Service
public class PasswordHashService {

    public static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    public static final int DEFAULT_ITERATIONS = 120000;

    private static final int SALT_BYTES = 16;

    private static final int KEY_BITS = 256;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * 生成随机 salt。
     *
     * @return Base64 编码的 salt。
     */
    public String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 使用默认安全参数生成密码哈希。
     *
     * @param rawPassword 用户提交的原始密码。
     * @param saltBase64 Base64 编码的 salt。
     * @return Base64 编码的密码哈希。
     */
    public String hash(String rawPassword, String saltBase64) {
        return hash(rawPassword, saltBase64, DEFAULT_ITERATIONS);
    }

    /**
     * 使用指定迭代次数生成密码哈希。
     *
     * @param rawPassword 用户提交的原始密码。
     * @param saltBase64 Base64 编码的 salt。
     * @param iterations PBKDF2 迭代次数。
     * @return Base64 编码的密码哈希。
     */
    public String hash(String rawPassword, String saltBase64, int iterations) {
        if (!StringUtils.hasText(rawPassword) || !StringUtils.hasText(saltBase64) || iterations <= 0) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        try {
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, KEY_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        } catch (Exception ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 校验原始密码是否匹配已有哈希。
     *
     * @param rawPassword 用户提交的原始密码。
     * @param saltBase64 Base64 编码的 salt。
     * @param expectedHashBase64 数据库存储的 Base64 密码哈希。
     * @param iterations PBKDF2 迭代次数。
     * @return 密码是否匹配。
     */
    public boolean matches(String rawPassword, String saltBase64, String expectedHashBase64, int iterations) {
        if (!StringUtils.hasText(expectedHashBase64)) {
            return false;
        }
        String actualHashBase64 = hash(rawPassword, saltBase64, iterations);
        byte[] actual = Base64.getDecoder().decode(actualHashBase64);
        byte[] expected = Base64.getDecoder().decode(expectedHashBase64);
        return MessageDigest.isEqual(actual, expected);
    }
}
