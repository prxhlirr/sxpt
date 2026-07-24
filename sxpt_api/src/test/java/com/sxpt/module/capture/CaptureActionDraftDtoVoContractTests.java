package com.sxpt.module.capture;

import com.sxpt.module.capture.dto.CreateCaptureActionDraftRequest;
import com.sxpt.module.capture.dto.ConfirmCaptureActionDraftRequest;
import com.sxpt.module.capture.dto.DiscardCaptureActionDraftRequest;
import com.sxpt.module.capture.vo.CaptureActionDraftVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 页面动作草稿 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证创建动作草稿请求 DTO 的必填和长度约束。
 * 2. 验证返回 VO 不暴露软删除字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认 VO 字段边界，避免内部持久化字段泄漏给前端。
 */
class CaptureActionDraftDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效创建动作草稿请求可以通过参数校验。
     */
    @Test
    void validCreateCaptureActionDraftRequestShouldPassValidation() {
        CreateCaptureActionDraftRequest request = buildValidRequest();

        Set<ConstraintViolation<CreateCaptureActionDraftRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少动作名称时参数校验失败。
     */
    @Test
    void createCaptureActionDraftRequestShouldRejectMissingActionName() {
        CreateCaptureActionDraftRequest request = buildValidRequest();
        request.setActionName(" ");

        Set<ConstraintViolation<CreateCaptureActionDraftRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验缺少顺序号时参数校验失败。
     */
    @Test
    void createCaptureActionDraftRequestShouldRejectMissingSequenceNo() {
        CreateCaptureActionDraftRequest request = buildValidRequest();
        request.setSequenceNo(null);

        Set<ConstraintViolation<CreateCaptureActionDraftRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验学习提示超长时参数校验失败。
     */
    @Test
    void createCaptureActionDraftRequestShouldRejectTooLongGuideContent() {
        CreateCaptureActionDraftRequest request = buildValidRequest();
        request.setGuideContent(repeat("a", 4097));

        Set<ConstraintViolation<CreateCaptureActionDraftRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验动作草稿 VO 不暴露内部软删除字段。
     */
    @Test
    void captureActionDraftVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> CaptureActionDraftVO.class.getDeclaredField("deleted"));
    }

    /**
     * 校验最小有效确认动作草稿请求可以通过参数校验。
     */
    @Test
    void validConfirmCaptureActionDraftRequestShouldPassValidation() {
        ConfirmCaptureActionDraftRequest request = buildValidConfirmRequest();

        Set<ConstraintViolation<ConfirmCaptureActionDraftRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验确认动作草稿时缺少业务操作名称会失败。
     */
    @Test
    void confirmCaptureActionDraftRequestShouldRejectMissingOperationName() {
        ConfirmCaptureActionDraftRequest request = buildValidConfirmRequest();
        request.setConfirmedOperationName(" ");

        Set<ConstraintViolation<ConfirmCaptureActionDraftRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验丢弃动作草稿请求可以只携带更新人信息。
     */
    @Test
    void validDiscardCaptureActionDraftRequestShouldPassValidation() {
        DiscardCaptureActionDraftRequest request = new DiscardCaptureActionDraftRequest();
        request.setUpdateBy("teacher_001");

        Set<ConstraintViolation<DiscardCaptureActionDraftRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 构造最小有效创建动作草稿请求。
     *
     * @return 创建页面动作草稿请求。
     */
    private CreateCaptureActionDraftRequest buildValidRequest() {
        CreateCaptureActionDraftRequest request = new CreateCaptureActionDraftRequest();
        request.setTenantId("tenant_001");
        request.setCaptureSessionId("cap_001");
        request.setEventId("evt_001");
        request.setActionName("点击提交按钮");
        request.setActionType("CLICK");
        request.setSequenceNo(1L);
        request.setSuggestedOperationName("提交备案申请");
        request.setGuideContent("点击页面底部的提交按钮。");
        request.setPracticeHint("确认表单填写完整后再提交。");
        return request;
    }

    /**
     * 构造最小有效确认动作草稿请求。
     *
     * @return 确认页面动作草稿请求。
     */
    private ConfirmCaptureActionDraftRequest buildValidConfirmRequest() {
        ConfirmCaptureActionDraftRequest request = new ConfirmCaptureActionDraftRequest();
        request.setConfirmedOperationName("提交备案申请");
        request.setConfirmedStepName("提交申请表");
        request.setGuideContent("点击页面底部的提交按钮。");
        request.setPracticeHint("提交前检查必填项。");
        request.setUpdateBy("teacher_001");
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
