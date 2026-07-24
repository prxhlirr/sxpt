package com.sxpt.module.user;

import com.sxpt.module.user.dto.AddTeachUserOrgRequest;
import com.sxpt.module.user.dto.RemoveTeachUserOrgRequest;
import com.sxpt.module.user.vo.TeachUserOrgVO;
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
 * 教学用户组织关系 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证添加成员请求 DTO 的必填和长度约束。
 * 2. 验证返回 VO 只暴露成员关系展示字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 直接校验请求对象。
 * 2. 使用反射检查 VO 字段，避免误暴露数据库软删除字段。
 */
class TeachUserOrgDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效添加成员请求可以通过参数校验。
     */
    @Test
    void validAddRequestShouldPassValidation() {
        AddTeachUserOrgRequest request = buildValidRequest();

        Set<ConstraintViolation<AddTeachUserOrgRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    /**
     * 校验缺少组织 ID 时触发参数校验错误。
     */
    @Test
    void addRequestShouldRejectMissingOrgId() {
        AddTeachUserOrgRequest request = buildValidRequest();
        request.setOrgId(" ");

        Set<ConstraintViolation<AddTeachUserOrgRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    /**
     * 校验关系类型超长时触发参数校验错误。
     */
    @Test
    void addRequestShouldRejectTooLongRelationType() {
        AddTeachUserOrgRequest request = buildValidRequest();
        request.setRelationType(repeat("A", 33));

        Set<ConstraintViolation<AddTeachUserOrgRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    /**
     * 校验最小有效移除成员请求可以通过参数校验。
     */
    @Test
    void validRemoveRequestShouldPassValidation() {
        RemoveTeachUserOrgRequest request = buildValidRemoveRequest();

        Set<ConstraintViolation<RemoveTeachUserOrgRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    /**
     * 校验移除成员请求缺少用户 ID 时触发参数校验错误。
     */
    @Test
    void removeRequestShouldRejectMissingUserId() {
        RemoveTeachUserOrgRequest request = buildValidRemoveRequest();
        request.setUserId(" ");

        Set<ConstraintViolation<RemoveTeachUserOrgRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    /**
     * 校验用户组织关系 VO 不暴露软删除字段。
     *
     * @throws NoSuchFieldException 当字段契约被破坏时，测试应失败。
     */
    @Test
    void teachUserOrgVoShouldExposeOnlyResponseFields() throws NoSuchFieldException {
        Field userIdField = TeachUserOrgVO.class.getDeclaredField("userId");
        Field orgIdField = TeachUserOrgVO.class.getDeclaredField("orgId");
        Field relationTypeField = TeachUserOrgVO.class.getDeclaredField("relationType");

        assertEquals(String.class, userIdField.getType());
        assertEquals(String.class, orgIdField.getType());
        assertEquals(String.class, relationTypeField.getType());
        assertFalse(hasField("deleted"));
    }

    /**
     * 构造最小有效添加成员请求。
     *
     * @return 添加用户到教学组织请求。
     */
    private AddTeachUserOrgRequest buildValidRequest() {
        AddTeachUserOrgRequest request = new AddTeachUserOrgRequest();
        request.setTenantId("tenant_001");
        request.setOrgId("org_001");
        request.setUserId("user_001");
        request.setRelationType("STUDENT");
        return request;
    }

    /**
     * 构造最小有效移除成员请求。
     *
     * @return 从教学组织移除用户请求。
     */
    private RemoveTeachUserOrgRequest buildValidRemoveRequest() {
        RemoveTeachUserOrgRequest request = new RemoveTeachUserOrgRequest();
        request.setTenantId("tenant_001");
        request.setOrgId("org_001");
        request.setUserId("user_001");
        return request;
    }

    /**
     * 判断 VO 是否包含指定字段。
     *
     * @param fieldName 字段名。
     * @return true 表示包含该字段。
     */
    private boolean hasField(String fieldName) {
        for (Field field : TeachUserOrgVO.class.getDeclaredFields()) {
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
