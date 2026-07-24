package com.sxpt.common.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sxpt.config.JwtProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * JWT 服务。
 *
 * 业务功能：
 * 1. 负责生成、解析、校验 JWT。
 * 2. 将 Token 细节封装在公共服务中，避免 Controller 或拦截器直接操作算法。
 *
 * 关键流程：
 * 1. 生成 Token 时写入用户 ID、用户名、签发时间和过期时间。
 * 2. 校验 Token 时验证签名和过期时间。
 * 3. 解析成功后返回认证主体。
 */
@Service
public class JwtService {

    private static final String USER_ID_CLAIM = "userId";

    private static final String USERNAME_CLAIM = "username";

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * 生成访问 Token。
     *
     * @param userId 用户 ID。
     * @param username 用户名。
     * @return JWT 字符串。
     */
    public String generateToken(String userId, String username) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + jwtProperties.getExpireSeconds() * 1000);
        return JWT.create()
                .withClaim(USER_ID_CLAIM, userId)
                .withClaim(USERNAME_CLAIM, username)
                .withIssuedAt(now)
                .withExpiresAt(expiresAt)
                .sign(algorithm());
    }

    /**
     * 解析并校验 Token。
     *
     * @param token JWT 字符串。
     * @return JWT 认证主体。
     */
    public JwtPrincipal parseToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Token 不能为空");
        }
        JWTVerifier verifier = JWT.require(algorithm()).build();
        DecodedJWT decodedJWT = verifier.verify(token);
        return new JwtPrincipal(
                decodedJWT.getClaim(USER_ID_CLAIM).asString(),
                decodedJWT.getClaim(USERNAME_CLAIM).asString()
        );
    }

    private Algorithm algorithm() {
        return Algorithm.HMAC256(jwtProperties.getSecret());
    }
}
