package com.sxpt.module.connector;

import com.sxpt.module.connector.dto.CreateIdentityBindingRequest;
import com.sxpt.module.connector.vo.IdentityBindingVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 原平台身份绑定 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证创建身份绑定请求 DTO 的必填和长度约束。
 * 2. 验证返回 VO 不暴露原平台角色和组织快照字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 直接校验请求对象。
 * 2. 使用反射检查 VO 字段，避免误暴露数据库快照字段。
 */
class IdentityBindingDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效创建身份绑定请求可以通过参数校验。
     */
    @Test
    void validCreateRequestShouldPassValidation() {
        CreateIdentityBindingRequest request = buildValidRequest();

        Set<ConstraintViolation<CreateIdentityBindingRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    /**
     * 校验缺少外部用户 ID 时触发参数校验错误。
     */
    @Test
    void createRequestShouldRejectMissingExternalUserId() {
        CreateIdentityBindingRequest request = buildValidRequest();
        request.setExternalUserId(" ");

        Set<ConstraintViolation<CreateIdentityBindingRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    /**
     * 校验原平台角色快照超长时触发参数校验错误。
     */
    @Test
    void createRequestShouldRejectTooLongExternalRoleJson() {
        CreateIdentityBindingRequest request = buildValidRequest();
        request.setExternalRoleJson(repeat("A", 4097));

        Set<ConstraintViolation<CreateIdentityBindingRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    /**
     * 校验身份绑定 VO 不暴露原平台快照字段。
     *
     * @throws NoSuchFieldException 当字段契约被破坏时，测试应失败。
     */
    @Test
    void identityBindingVoShouldHideSnapshotFields() throws NoSuchFieldException {
        Field externalUserIdField = IdentityBindingVO.class.getDeclaredField("externalUserId");
        Field bindingTypeField = IdentityBindingVO.class.getDeclaredField("bindingType");

        assertEquals(String.class, externalUserIdField.getType());
        assertEquals(String.class, bindingTypeField.getType());
        assertFalse(hasField("externalRoleJson"));
        assertFalse(hasField("externalOrgJson"));
        assertFalse(hasField("deleted"));
    }

    /**
     * 构造最小有效创建身份绑定请求。
     *
     * @return 创建原平台身份绑定请求。
     */
    private CreateIdentityBindingRequest buildValidRequest() {
        CreateIdentityBindingRequest request = new CreateIdentityBindingRequest();
        request.setTenantId("tenant_001");
        request.setUserId("user_001");
        request.setConnectorSystemId("connector_001");
        request.setExternalUserId("origin_user_001");
        request.setExternalUsername("origin_teacher");
        request.setExternalRoleJson("{}");
        request.setExternalOrgJson("{}");
        request.setBindingType("TRAINING");
        return request;
    }

    /**
     * 判断 VO 是否包含指定字段。
     *
     * @param fieldName 字段名。
     * @return true 表示包含该字段。
     */
    private boolean hasField(String fieldName) {
        for (Field field : IdentityBindingVO.class.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构造指定重复次数的字符串。
     *
     * @param value 重复片段。
     * @param count 重复次数。
     * @return 拼接后的字符串。
     */
    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }
}
