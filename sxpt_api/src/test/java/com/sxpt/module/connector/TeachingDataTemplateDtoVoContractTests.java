package com.sxpt.module.connector;

import com.sxpt.module.connector.dto.CreateTeachingDataTemplateRequest;
import com.sxpt.module.connector.vo.TeachingDataTemplateVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 教学业务数据模板 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证创建模板请求 DTO 的必填和长度约束。
 * 2. 验证返回 VO 不暴露软删除字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认 VO 字段边界，避免内部持久化字段泄漏给前端。
 */
class TeachingDataTemplateDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效创建模板请求可以通过参数校验。
     */
    @Test
    void validCreateTeachingDataTemplateRequestShouldPassValidation() {
        CreateTeachingDataTemplateRequest request = buildValidRequest();

        Set<ConstraintViolation<CreateTeachingDataTemplateRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少模板编码时参数校验失败。
     */
    @Test
    void createTeachingDataTemplateRequestShouldRejectMissingTemplateCode() {
        CreateTeachingDataTemplateRequest request = buildValidRequest();
        request.setTemplateCode(" ");

        Set<ConstraintViolation<CreateTeachingDataTemplateRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验模板配置超长时参数校验失败。
     */
    @Test
    void createTeachingDataTemplateRequestShouldRejectTooLongConfigJson() {
        CreateTeachingDataTemplateRequest request = buildValidRequest();
        request.setConfigJson(repeat("a", 32769));

        Set<ConstraintViolation<CreateTeachingDataTemplateRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验模板 VO 不暴露软删除字段。
     */
    @Test
    void teachingDataTemplateVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> TeachingDataTemplateVO.class.getDeclaredField("deleted"));
    }

    /**
     * 构造最小有效创建模板请求。
     *
     * @return 创建教学业务数据模板请求。
     */
    private CreateTeachingDataTemplateRequest buildValidRequest() {
        CreateTeachingDataTemplateRequest request = new CreateTeachingDataTemplateRequest();
        request.setTenantId("tenant_001");
        request.setConnectorSystemId("connector_001");
        request.setTemplateCode("record_apply_default");
        request.setTemplateName("标准备案申请默认数据");
        request.setSceneType("RECORD");
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
