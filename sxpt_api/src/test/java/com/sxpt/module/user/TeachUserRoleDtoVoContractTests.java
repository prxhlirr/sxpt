package com.sxpt.module.user;

import com.sxpt.module.user.dto.GrantTeachUserRoleRequest;
import com.sxpt.module.user.vo.TeachUserRoleVO;
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
 * 教学平台用户角色关系 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证授权请求 DTO 的必填和长度约束。
 * 2. 验证返回 VO 只暴露授权关系展示字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 直接校验请求对象。
 * 2. 使用反射检查 VO 字段，避免误暴露数据库软删除字段。
 */
class TeachUserRoleDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效授权请求可以通过参数校验。
     */
    @Test
    void validGrantRequestShouldPassValidation() {
        GrantTeachUserRoleRequest request = buildValidRequest();

        Set<ConstraintViolation<GrantTeachUserRoleRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    /**
     * 校验缺少用户 ID 时触发参数校验错误。
     */
    @Test
    void grantRequestShouldRejectMissingUserId() {
        GrantTeachUserRoleRequest request = buildValidRequest();
        request.setUserId(" ");

        Set<ConstraintViolation<GrantTeachUserRoleRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    /**
     * 校验授权来源超长时触发参数校验错误。
     */
    @Test
    void grantRequestShouldRejectTooLongGrantSource() {
        GrantTeachUserRoleRequest request = buildValidRequest();
        request.setGrantSource(repeat("A", 33));

        Set<ConstraintViolation<GrantTeachUserRoleRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    /**
     * 校验用户角色关系 VO 不暴露软删除字段。
     *
     * @throws NoSuchFieldException 当字段契约被破坏时，测试应失败。
     */
    @Test
    void teachUserRoleVoShouldExposeOnlyResponseFields() throws NoSuchFieldException {
        Field userIdField = TeachUserRoleVO.class.getDeclaredField("userId");
        Field roleIdField = TeachUserRoleVO.class.getDeclaredField("roleId");
        Field grantSourceField = TeachUserRoleVO.class.getDeclaredField("grantSource");

        assertEquals(String.class, userIdField.getType());
        assertEquals(String.class, roleIdField.getType());
        assertEquals(String.class, grantSourceField.getType());
        assertFalse(hasField("deleted"));
    }

    /**
     * 构造最小有效授权请求。
     *
     * @return 授予教学用户角色请求。
     */
    private GrantTeachUserRoleRequest buildValidRequest() {
        GrantTeachUserRoleRequest request = new GrantTeachUserRoleRequest();
        request.setTenantId("tenant_001");
        request.setUserId("user_001");
        request.setRoleId("role_001");
        request.setGrantSource("MANUAL");
        return request;
    }

    /**
     * 判断 VO 是否包含指定字段。
     *
     * @param fieldName 字段名。
     * @return true 表示包含该字段。
     */
    private boolean hasField(String fieldName) {
        for (Field field : TeachUserRoleVO.class.getDeclaredFields()) {
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
