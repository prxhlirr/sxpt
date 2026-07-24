package com.sxpt.module.capture;

import com.sxpt.module.capture.dto.ReportCaptureEventRequest;
import com.sxpt.module.capture.vo.CaptureEventVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SDK 采集事件 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证事件上报请求 DTO 的必填和长度约束。
 * 2. 验证返回 VO 不暴露软删除和完整事件摘要 JSON 字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认 VO 字段边界，避免内部持久化字段泄漏给 SDK。
 */
class CaptureEventDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效事件上报请求可以通过参数校验。
     */
    @Test
    void validReportCaptureEventRequestShouldPassValidation() {
        ReportCaptureEventRequest request = buildValidRequest();

        Set<ConstraintViolation<ReportCaptureEventRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少客户端事件 ID 时参数校验失败，避免幂等能力失效。
     */
    @Test
    void reportCaptureEventRequestShouldRejectMissingClientEventId() {
        ReportCaptureEventRequest request = buildValidRequest();
        request.setClientEventId(" ");

        Set<ConstraintViolation<ReportCaptureEventRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验缺少事件时间时参数校验失败，避免事件无法排序和追溯。
     */
    @Test
    void reportCaptureEventRequestShouldRejectMissingEventTime() {
        ReportCaptureEventRequest request = buildValidRequest();
        request.setEventTime(null);

        Set<ConstraintViolation<ReportCaptureEventRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验事件摘要 JSON 超长时参数校验失败，避免保存全量 DOM。
     */
    @Test
    void reportCaptureEventRequestShouldRejectTooLongPayloadJson() {
        ReportCaptureEventRequest request = buildValidRequest();
        request.setEventPayloadJson(repeat("a", 32769));

        Set<ConstraintViolation<ReportCaptureEventRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验采集事件 VO 不暴露内部持久化字段。
     */
    @Test
    void captureEventVoShouldHideInternalFields() {
        assertThrows(NoSuchFieldException.class, () -> CaptureEventVO.class.getDeclaredField("deleted"));
        assertThrows(NoSuchFieldException.class, () -> CaptureEventVO.class.getDeclaredField("eventPayloadJson"));
    }

    /**
     * 构造最小有效事件上报请求。
     *
     * @return SDK 采集事件上报请求。
     */
    private ReportCaptureEventRequest buildValidRequest() {
        ReportCaptureEventRequest request = new ReportCaptureEventRequest();
        request.setTenantId("tenant_001");
        request.setCaptureSessionId("cap_001");
        request.setSdkSessionId("sdk_001");
        request.setClientEventId("client_evt_001");
        request.setEventType("CLICK");
        request.setEventTime(LocalDateTime.now());
        request.setSequenceNo(1L);
        request.setPageUrl("https://origin.example.com/record/start");
        request.setTargetText("提交");
        request.setTargetLocator("#submit");
        request.setTargetStableKey("submit_button");
        request.setEventPayloadJson("{\"source\":\"sdk\"}");
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
