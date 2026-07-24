package com.sxpt.module.connector;

import com.sxpt.module.connector.dto.VerifyPlatformLaunchTokenRequest;
import com.sxpt.module.connector.vo.VerifiedPlatformLaunchContextVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 原平台启动令牌校验 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证校验请求的租户 ID 和明文 token 必填。
 * 2. 验证校验响应不暴露 token 明文、token hash 和软删除字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认 VO 字段边界，防止敏感凭据被误加入响应对象。
 */
class PlatformLaunchTokenVerifyDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效 token 校验请求可以通过参数校验。
     */
    @Test
    void validVerifyPlatformLaunchTokenRequestShouldPassValidation() {
        VerifyPlatformLaunchTokenRequest request = buildValidRequest();

        Set<ConstraintViolation<VerifyPlatformLaunchTokenRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少明文 token 时参数校验失败。
     */
    @Test
    void verifyPlatformLaunchTokenRequestShouldRejectMissingToken() {
        VerifyPlatformLaunchTokenRequest request = buildValidRequest();
        request.setLaunchToken(" ");

        Set<ConstraintViolation<VerifyPlatformLaunchTokenRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验校验响应 VO 不暴露 token 凭据字段。
     */
    @Test
    void verifiedPlatformLaunchContextVoShouldHideTokenFields() {
        assertThrows(NoSuchFieldException.class, () -> VerifiedPlatformLaunchContextVO.class.getDeclaredField("launchToken"));
        assertThrows(NoSuchFieldException.class, () -> VerifiedPlatformLaunchContextVO.class.getDeclaredField("launchTokenHash"));
        assertThrows(NoSuchFieldException.class, () -> VerifiedPlatformLaunchContextVO.class.getDeclaredField("deleted"));
    }

    /**
     * 构造最小有效 token 校验请求。
     *
     * @return 原平台启动令牌校验请求。
     */
    private VerifyPlatformLaunchTokenRequest buildValidRequest() {
        VerifyPlatformLaunchTokenRequest request = new VerifyPlatformLaunchTokenRequest();
        request.setTenantId("tenant_001");
        request.setLaunchToken("ctx_token_001");
        return request;
    }
}
