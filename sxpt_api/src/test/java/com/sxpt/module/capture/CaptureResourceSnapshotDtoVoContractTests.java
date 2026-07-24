package com.sxpt.module.capture;

import com.sxpt.module.capture.dto.ReportCaptureResourceSnapshotRequest;
import com.sxpt.module.capture.vo.CaptureResourceSnapshotVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 采集资源快照 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证资源快照上报请求 DTO 的必填和长度约束。
 * 2. 验证 DEBUG_FULL_DOM 快照必须携带过期时间。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认 VO 字段边界，避免内部持久化字段泄漏给前端。
 */
class CaptureResourceSnapshotDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效资源快照上报请求可以通过参数校验。
     */
    @Test
    void validReportCaptureResourceSnapshotRequestShouldPassValidation() {
        ReportCaptureResourceSnapshotRequest request = buildValidRequest();

        Set<ConstraintViolation<ReportCaptureResourceSnapshotRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少元素摘要 JSON 时参数校验失败。
     */
    @Test
    void reportCaptureResourceSnapshotRequestShouldRejectMissingElementSnapshotJson() {
        ReportCaptureResourceSnapshotRequest request = buildValidRequest();
        request.setElementSnapshotJson(" ");

        Set<ConstraintViolation<ReportCaptureResourceSnapshotRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验元素摘要 JSON 超长时参数校验失败，避免保存全量 DOM。
     */
    @Test
    void reportCaptureResourceSnapshotRequestShouldRejectTooLongElementSnapshotJson() {
        ReportCaptureResourceSnapshotRequest request = buildValidRequest();
        request.setElementSnapshotJson(repeat("a", 32769));

        Set<ConstraintViolation<ReportCaptureResourceSnapshotRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验 DEBUG_FULL_DOM 缺少过期时间时参数校验失败。
     */
    @Test
    void reportCaptureResourceSnapshotRequestShouldRejectDebugFullDomWithoutExpireTime() {
        ReportCaptureResourceSnapshotRequest request = buildValidRequest();
        request.setSnapshotScope("DEBUG_FULL_DOM");
        request.setExpireTime(null);

        Set<ConstraintViolation<ReportCaptureResourceSnapshotRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验资源快照 VO 不暴露内部持久化字段。
     */
    @Test
    void captureResourceSnapshotVoShouldHideInternalFields() {
        assertThrows(NoSuchFieldException.class,
                () -> CaptureResourceSnapshotVO.class.getDeclaredField("deleted"));
        assertThrows(NoSuchFieldException.class,
                () -> CaptureResourceSnapshotVO.class.getDeclaredField("archiveTime"));
    }

    /**
     * 构造最小有效资源快照上报请求。
     *
     * @return 采集资源快照上报请求。
     */
    private ReportCaptureResourceSnapshotRequest buildValidRequest() {
        ReportCaptureResourceSnapshotRequest request = new ReportCaptureResourceSnapshotRequest();
        request.setTenantId("tenant_001");
        request.setCaptureSessionId("cap_001");
        request.setPageUrl("https://origin.example.com/record/start");
        request.setPageTitle("备案申请");
        request.setResourceType("BUTTON");
        request.setSnapshotScope("TARGET_ELEMENT");
        request.setResourceName("提交按钮");
        request.setResourceLocator("#submit");
        request.setElementSnapshotJson("{\"text\":\"提交\",\"role\":\"button\"}");
        request.setSnapshotHash("hash_001");
        request.setExpireTime(LocalDateTime.now().plusDays(7));
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
