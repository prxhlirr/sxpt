package com.sxpt.module.connector;

import com.sxpt.module.connector.dto.MarkPlatformLaunchFailedRequest;
import com.sxpt.module.connector.dto.MarkPlatformLaunchUsedRequest;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 原平台启动回写 DTO 契约测试。
 *
 * 业务功能：
 * 1. 验证 USED 回写请求必须包含启动上下文 ID。
 * 2. 验证 FAILED 回写请求必须包含启动上下文 ID 和失败原因。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 提前固定 Controller 入参契约，避免回写接口接收无审计价值的空原因。
 */
class PlatformLaunchCallbackDtoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效 USED 回写请求可以通过参数校验。
     */
    @Test
    void validMarkUsedRequestShouldPassValidation() {
        MarkPlatformLaunchUsedRequest request = new MarkPlatformLaunchUsedRequest();
        request.setId("launch_001");

        Set<ConstraintViolation<MarkPlatformLaunchUsedRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验 USED 回写请求缺少 ID 时参数校验失败。
     */
    @Test
    void markUsedRequestShouldRejectMissingId() {
        MarkPlatformLaunchUsedRequest request = new MarkPlatformLaunchUsedRequest();
        request.setId(" ");

        Set<ConstraintViolation<MarkPlatformLaunchUsedRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验最小有效 FAILED 回写请求可以通过参数校验。
     */
    @Test
    void validMarkFailedRequestShouldPassValidation() {
        MarkPlatformLaunchFailedRequest request = new MarkPlatformLaunchFailedRequest();
        request.setId("launch_001");
        request.setErrorMessage("原平台 session 建立失败");

        Set<ConstraintViolation<MarkPlatformLaunchFailedRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验 FAILED 回写请求缺少失败原因时参数校验失败。
     */
    @Test
    void markFailedRequestShouldRejectMissingErrorMessage() {
        MarkPlatformLaunchFailedRequest request = new MarkPlatformLaunchFailedRequest();
        request.setId("launch_001");
        request.setErrorMessage(" ");

        Set<ConstraintViolation<MarkPlatformLaunchFailedRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }
}
