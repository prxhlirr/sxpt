package com.sxpt.module.user;

import com.sxpt.module.user.dto.CreateTeachUserRequest;
import com.sxpt.module.user.vo.TeachUserVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 教学平台用户 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证创建教学用户的请求参数具备基础校验。
 * 2. 验证返回对象不默认暴露外部同步快照。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 直接校验 DTO，不依赖 Spring 容器。
 * 2. 使用反射确认 VO 字段边界，避免后续误把内部快照返回给前端。
 */
class TeachUserDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验完整请求不产生参数错误。
     */
    @Test
    void createRequestShouldPassWhenRequiredFieldsExist() {
        CreateTeachUserRequest request = buildValidRequest();

        Set<ConstraintViolation<CreateTeachUserRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少用户账号时触发参数错误。
     */
    @Test
    void createRequestShouldRejectBlankUsername() {
        CreateTeachUserRequest request = buildValidRequest();
        request.setUsername(" ");

        Set<ConstraintViolation<CreateTeachUserRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("username", violations.iterator().next().getPropertyPath().toString());
    }

    /**
     * 校验邮箱格式不正确时触发参数错误。
     */
    @Test
    void createRequestShouldRejectInvalidEmail() {
        CreateTeachUserRequest request = buildValidRequest();
        request.setEmail("invalid-email");

        Set<ConstraintViolation<CreateTeachUserRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("email", violations.iterator().next().getPropertyPath().toString());
    }

    /**
     * 校验返回对象不包含外部同步快照 JSON。
     *
     * @throws NoSuchFieldException 当 VO 暴露 externalInfoJson 字段时，测试应失败。
     */
    @Test
    void teachUserVoShouldNotExposeExternalInfoJsonByDefault() {
        assertThrows(NoSuchFieldException.class, () -> findField("externalInfoJson"));
    }

    /**
     * 通过反射查找 VO 字段。
     *
     * @param fieldName 字段名。
     * @return 字段定义。
     * @throws NoSuchFieldException 字段不存在时抛出。
     */
    private Field findField(String fieldName) throws NoSuchFieldException {
        return TeachUserVO.class.getDeclaredField(fieldName);
    }

    /**
     * 构造最小有效创建请求。
     *
     * @return 创建教学平台用户请求。
     */
    private CreateTeachUserRequest buildValidRequest() {
        CreateTeachUserRequest request = new CreateTeachUserRequest();
        request.setTenantId("tenant_001");
        request.setUsername("teacher001");
        request.setRealName("教师一");
        request.setPhone("13800000000");
        request.setEmail("teacher001@example.com");
        request.setUserType("TEACHER");
        request.setSourceType("LOCAL");
        return request;
    }
}
