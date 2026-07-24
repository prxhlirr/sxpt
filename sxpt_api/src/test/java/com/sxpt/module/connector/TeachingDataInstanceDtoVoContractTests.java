package com.sxpt.module.connector;

import com.sxpt.module.connector.dto.CreateTeachingDataInstanceRequest;
import com.sxpt.module.connector.dto.ResetTeachingDataInstanceRequest;
import com.sxpt.module.connector.vo.TeachingDataInstanceVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 教学业务数据实例 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证创建实例请求 DTO 的必填和长度约束。
 * 2. 验证返回 VO 不暴露软删除字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认 VO 字段边界，避免内部持久化字段泄漏给前端。
 */
class TeachingDataInstanceDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效创建实例请求可以通过参数校验。
     */
    @Test
    void validCreateTeachingDataInstanceRequestShouldPassValidation() {
        CreateTeachingDataInstanceRequest request = buildValidRequest();

        Set<ConstraintViolation<CreateTeachingDataInstanceRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少原平台业务数据 ID 时参数校验失败。
     */
    @Test
    void createTeachingDataInstanceRequestShouldRejectMissingExternalBusinessId() {
        CreateTeachingDataInstanceRequest request = buildValidRequest();
        request.setExternalBusinessId(" ");

        Set<ConstraintViolation<CreateTeachingDataInstanceRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验元数据摘要超长时参数校验失败，避免接口层接收过大的原平台摘要。
     */
    @Test
    void createTeachingDataInstanceRequestShouldRejectTooLongMetadataJson() {
        CreateTeachingDataInstanceRequest request = buildValidRequest();
        request.setMetadataJson(repeat("a", 32769));

        Set<ConstraintViolation<CreateTeachingDataInstanceRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验最小有效重置实例请求可以通过参数校验。
     */
    @Test
    void validResetTeachingDataInstanceRequestShouldPassValidation() {
        ResetTeachingDataInstanceRequest request = buildValidResetRequest();

        Set<ConstraintViolation<ResetTeachingDataInstanceRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验重置实例缺少新的原平台业务数据 ID 时参数校验失败。
     */
    @Test
    void resetTeachingDataInstanceRequestShouldRejectMissingExternalBusinessId() {
        ResetTeachingDataInstanceRequest request = buildValidResetRequest();
        request.setExternalBusinessId(" ");

        Set<ConstraintViolation<ResetTeachingDataInstanceRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验重置实例元数据摘要超长时参数校验失败。
     */
    @Test
    void resetTeachingDataInstanceRequestShouldRejectTooLongMetadataJson() {
        ResetTeachingDataInstanceRequest request = buildValidResetRequest();
        request.setMetadataJson(repeat("a", 32769));

        Set<ConstraintViolation<ResetTeachingDataInstanceRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验实例 VO 不暴露软删除字段。
     */
    @Test
    void teachingDataInstanceVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> TeachingDataInstanceVO.class.getDeclaredField("deleted"));
    }

    /**
     * 构造最小有效创建实例请求。
     *
     * @return 创建教学业务数据实例请求。
     */
    private CreateTeachingDataInstanceRequest buildValidRequest() {
        CreateTeachingDataInstanceRequest request = new CreateTeachingDataInstanceRequest();
        request.setTenantId("tenant_001");
        request.setTemplateId("tpl_001");
        request.setConnectorSystemId("connector_001");
        request.setSceneType("PRACTICE");
        request.setExternalBusinessId("biz_001");
        return request;
    }

    /**
     * 构造最小有效重置实例请求。
     *
     * @return 重置教学业务数据实例请求。
     */
    private ResetTeachingDataInstanceRequest buildValidResetRequest() {
        ResetTeachingDataInstanceRequest request = new ResetTeachingDataInstanceRequest();
        request.setExternalBusinessId("biz_002");
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
