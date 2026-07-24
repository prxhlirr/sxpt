package com.sxpt.module.capture;

import com.sxpt.module.capture.dto.ConfirmCaptureSegmentSwitchRequest;
import com.sxpt.module.capture.vo.CaptureSegmentSwitchVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 备案片段切换 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证教师确认切换请求的必填和长度约束。
 * 2. 验证返回 VO 不暴露 launchTokenHash 等内部安全字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认 VO 字段边界。
 */
class CaptureSegmentSwitchDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效片段切换请求可以通过参数校验。
     */
    @Test
    void validConfirmCaptureSegmentSwitchRequestShouldPassValidation() {
        ConfirmCaptureSegmentSwitchRequest request = buildValidRequest();

        Set<ConstraintViolation<ConfirmCaptureSegmentSwitchRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少下一片段编号时参数校验失败。
     */
    @Test
    void confirmCaptureSegmentSwitchRequestShouldRejectMissingNextSegmentNo() {
        ConfirmCaptureSegmentSwitchRequest request = buildValidRequest();
        request.setNextSegmentNo(null);

        Set<ConstraintViolation<ConfirmCaptureSegmentSwitchRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验缺少下一片段原平台角色时参数校验失败。
     */
    @Test
    void confirmCaptureSegmentSwitchRequestShouldRejectMissingRoleId() {
        ConfirmCaptureSegmentSwitchRequest request = buildValidRequest();
        request.setRequiredExternalRoleId(" ");

        Set<ConstraintViolation<ConfirmCaptureSegmentSwitchRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验切换返回 VO 不暴露 token hash。
     */
    @Test
    void captureSegmentSwitchVoShouldHideLaunchTokenHash() {
        assertThrows(NoSuchFieldException.class, () -> CaptureSegmentSwitchVO.class.getDeclaredField("launchTokenHash"));
    }

    /**
     * 构造最小有效片段切换请求。
     *
     * @return 确认备案片段切换请求。
     */
    private ConfirmCaptureSegmentSwitchRequest buildValidRequest() {
        ConfirmCaptureSegmentSwitchRequest request = new ConfirmCaptureSegmentSwitchRequest();
        request.setTenantId("tenant_001");
        request.setTeacherId("teacher_001");
        request.setConnectorSystemId("connector_001");
        request.setCurrentSegmentNo(1L);
        request.setNextSegmentNo(2L);
        request.setRequiredExternalOrgId("org_ext_002");
        request.setRequiredExternalRoleId("role_ext_002");
        request.setTargetUrl("https://origin.example.com/record/audit");
        return request;
    }
}
