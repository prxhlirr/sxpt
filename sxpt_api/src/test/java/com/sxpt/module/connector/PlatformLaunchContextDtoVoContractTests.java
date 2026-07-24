package com.sxpt.module.connector;

import com.sxpt.module.connector.dto.CreatePlatformLaunchContextRequest;
import com.sxpt.module.connector.vo.PlatformLaunchContextVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 原平台启动上下文 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证创建启动上下文请求 DTO 的必填和长度约束。
 * 2. 验证返回 VO 不暴露数据库中的 token hash 和软删除字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认 VO 字段边界，避免敏感字段被误加入返回对象。
 */
class PlatformLaunchContextDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效创建启动上下文请求可以通过参数校验。
     */
    @Test
    void validCreatePlatformLaunchContextRequestShouldPassValidation() {
        CreatePlatformLaunchContextRequest request = buildValidRequest();

        Set<ConstraintViolation<CreatePlatformLaunchContextRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少目标地址时参数校验失败。
     */
    @Test
    void createPlatformLaunchContextRequestShouldRejectMissingTargetUrl() {
        CreatePlatformLaunchContextRequest request = buildValidRequest();
        request.setTargetUrl(" ");

        Set<ConstraintViolation<CreatePlatformLaunchContextRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验数据范围摘要超长时参数校验失败。
     */
    @Test
    void createPlatformLaunchContextRequestShouldRejectTooLongDataScopeJson() {
        CreatePlatformLaunchContextRequest request = buildValidRequest();
        request.setDataScopeJson(repeat("a", 32769));

        Set<ConstraintViolation<CreatePlatformLaunchContextRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验启动上下文 VO 不暴露 token hash 和软删除字段。
     *
     * @throws NoSuchFieldException 期望字段不存在时由断言捕获。
     */
    @Test
    void platformLaunchContextVoShouldHideSensitiveFields() throws NoSuchFieldException {
        assertThrows(NoSuchFieldException.class, () -> PlatformLaunchContextVO.class.getDeclaredField("launchTokenHash"));
        assertThrows(NoSuchFieldException.class, () -> PlatformLaunchContextVO.class.getDeclaredField("deleted"));
        assertThrows(NoSuchFieldException.class, () -> PlatformLaunchContextVO.class.getDeclaredField("errorMessage"));
        PlatformLaunchContextVO.class.getDeclaredField("launchToken");
    }

    /**
     * 构造最小有效创建启动上下文请求。
     *
     * @return 创建原平台启动上下文请求。
     */
    private CreatePlatformLaunchContextRequest buildValidRequest() {
        CreatePlatformLaunchContextRequest request = new CreatePlatformLaunchContextRequest();
        request.setTenantId("tenant_001");
        request.setUserId("user_001");
        request.setConnectorSystemId("connector_001");
        request.setSceneType("RECORD");
        request.setSdkMode("CAPTURE");
        request.setTargetUrl("/record/apply");
        return request;
    }

    /**
     * 构造指定长度字符串。
     *
     * @param value 重复字符。
     * @param count 重复次数。
     * @return 指定长度字符串。
     */
    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
