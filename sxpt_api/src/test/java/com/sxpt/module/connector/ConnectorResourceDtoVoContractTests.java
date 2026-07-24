package com.sxpt.module.connector;

import com.sxpt.module.connector.dto.CreateConnectorResourceRequest;
import com.sxpt.module.connector.vo.ConnectorResourceVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 原平台正式资源 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证创建正式资源请求 DTO 的必填和长度约束。
 * 2. 验证返回 VO 不暴露软删除字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认 VO 字段边界，避免内部持久化字段泄漏给前端。
 */
class ConnectorResourceDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效创建正式资源请求可以通过参数校验。
     */
    @Test
    void validCreateConnectorResourceRequestShouldPassValidation() {
        CreateConnectorResourceRequest request = buildValidRequest();

        Set<ConstraintViolation<CreateConnectorResourceRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少资源编码时参数校验失败。
     */
    @Test
    void createConnectorResourceRequestShouldRejectMissingResourceCode() {
        CreateConnectorResourceRequest request = buildValidRequest();
        request.setResourceCode(" ");

        Set<ConstraintViolation<CreateConnectorResourceRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验元数据 JSON 超长时参数校验失败。
     */
    @Test
    void createConnectorResourceRequestShouldRejectTooLongMetadataJson() {
        CreateConnectorResourceRequest request = buildValidRequest();
        request.setMetadataJson(repeat("a", 32769));

        Set<ConstraintViolation<CreateConnectorResourceRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验正式资源 VO 不暴露内部软删除字段。
     */
    @Test
    void connectorResourceVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> ConnectorResourceVO.class.getDeclaredField("deleted"));
    }

    /**
     * 构造最小有效创建正式资源请求。
     *
     * @return 创建正式资源请求。
     */
    private CreateConnectorResourceRequest buildValidRequest() {
        CreateConnectorResourceRequest request = new CreateConnectorResourceRequest();
        request.setTenantId("tenant_001");
        request.setConnectorSystemId("connector_001");
        request.setResourceCode("BTN_SUBMIT");
        request.setResourceName("提交按钮");
        request.setResourceType("BUTTON");
        request.setPageUrl("https://origin.example.com/record/start");
        request.setLocator("#submit");
        request.setStableKey("submit_button");
        request.setMetadataJson("{\"text\":\"提交\"}");
        request.setSourceCaptureId("cap_001");
        request.setCreateBy("teacher_001");
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
