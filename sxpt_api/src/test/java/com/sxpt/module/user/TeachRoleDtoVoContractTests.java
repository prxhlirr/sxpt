package com.sxpt.module.user;

import com.sxpt.module.user.dto.CreateTeachRoleRequest;
import com.sxpt.module.user.vo.TeachRoleVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 教学平台角色 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证创建教学角色的请求参数具备基础校验。
 * 2. 验证返回对象具备前端展示所需的角色字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 直接校验 DTO，不依赖 Spring 容器。
 * 2. 使用反射确认 VO 字段边界，避免后续误删前端依赖字段。
 */
class TeachRoleDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验完整请求不产生参数错误。
     */
    @Test
    void createRequestShouldPassWhenRequiredFieldsExist() {
        CreateTeachRoleRequest request = buildValidRequest();

        Set<ConstraintViolation<CreateTeachRoleRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少角色编码时触发参数错误。
     */
    @Test
    void createRequestShouldRejectBlankRoleCode() {
        CreateTeachRoleRequest request = buildValidRequest();
        request.setRoleCode(" ");

        Set<ConstraintViolation<CreateTeachRoleRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("roleCode", violations.iterator().next().getPropertyPath().toString());
    }

    /**
     * 校验角色说明超长时触发参数错误。
     */
    @Test
    void createRequestShouldRejectTooLongDescription() {
        CreateTeachRoleRequest request = buildValidRequest();
        request.setDescription(repeat("a", 513));

        Set<ConstraintViolation<CreateTeachRoleRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("description", violations.iterator().next().getPropertyPath().toString());
    }

    /**
     * 校验返回对象包含角色编码字段。
     *
     * @throws NoSuchFieldException 当 VO 缺少 roleCode 字段时，测试应失败。
     */
    @Test
    void teachRoleVoShouldExposeRoleCode() throws NoSuchFieldException {
        Field roleCodeField = TeachRoleVO.class.getDeclaredField("roleCode");

        assertNotNull(roleCodeField);
    }

    /**
     * 构造固定长度字符串。
     *
     * @param value 单字符文本。
     * @param count 重复次数。
     * @return 指定长度字符串。
     */
    private String repeat(String value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    /**
     * 构造最小有效创建请求。
     *
     * @return 创建教学平台角色请求。
     */
    private CreateTeachRoleRequest buildValidRequest() {
        CreateTeachRoleRequest request = new CreateTeachRoleRequest();
        request.setTenantId("tenant_001");
        request.setRoleCode("TEACHER");
        request.setRoleName("教师");
        request.setDescription("负责备案、发布任务和复核评分");
        return request;
    }
}
