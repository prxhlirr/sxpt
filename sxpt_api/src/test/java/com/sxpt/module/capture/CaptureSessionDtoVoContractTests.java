package com.sxpt.module.capture;

import com.sxpt.module.capture.dto.CreateCaptureSessionRequest;
import com.sxpt.module.capture.vo.CaptureSessionVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 备案采集会话 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证创建会话请求 DTO 的必填和长度约束。
 * 2. 验证返回 VO 不暴露软删除字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认 VO 字段边界，避免内部持久化字段泄漏给前端。
 */
class CaptureSessionDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效创建会话请求可以通过参数校验。
     */
    @Test
    void validCreateCaptureSessionRequestShouldPassValidation() {
        CreateCaptureSessionRequest request = buildValidRequest();

        Set<ConstraintViolation<CreateCaptureSessionRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少开始地址时参数校验失败。
     */
    @Test
    void createCaptureSessionRequestShouldRejectMissingStartUrl() {
        CreateCaptureSessionRequest request = buildValidRequest();
        request.setStartUrl(" ");

        Set<ConstraintViolation<CreateCaptureSessionRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验开始地址超长时参数校验失败。
     */
    @Test
    void createCaptureSessionRequestShouldRejectTooLongStartUrl() {
        CreateCaptureSessionRequest request = buildValidRequest();
        request.setStartUrl(repeat("a", 1025));

        Set<ConstraintViolation<CreateCaptureSessionRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验采集会话 VO 不暴露软删除字段。
     */
    @Test
    void captureSessionVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> CaptureSessionVO.class.getDeclaredField("deleted"));
    }

    /**
     * 构造最小有效创建会话请求。
     *
     * @return 创建备案采集会话请求。
     */
    private CreateCaptureSessionRequest buildValidRequest() {
        CreateCaptureSessionRequest request = new CreateCaptureSessionRequest();
        request.setTenantId("tenant_001");
        request.setConnectorSystemId("connector_001");
        request.setTeacherId("teacher_001");
        request.setSessionName("标准备案申请采集");
        request.setStartUrl("https://origin.example.com/record/start");
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
