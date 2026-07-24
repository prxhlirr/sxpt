package com.sxpt.module.user;

import com.sxpt.module.user.dto.CreateTeachOrgRequest;
import com.sxpt.module.user.vo.TeachOrgVO;
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
 * 教学组织 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证创建组织请求 DTO 的必填和长度约束。
 * 2. 验证返回 VO 只暴露组织展示字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 直接校验请求对象。
 * 2. 使用反射检查 VO 字段，避免误暴露数据库软删除字段。
 */
class TeachOrgDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效创建组织请求可以通过参数校验。
     */
    @Test
    void validCreateRequestShouldPassValidation() {
        CreateTeachOrgRequest request = buildValidRequest();

        Set<ConstraintViolation<CreateTeachOrgRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    /**
     * 校验缺少组织编码时触发参数校验错误。
     */
    @Test
    void createRequestShouldRejectMissingOrgCode() {
        CreateTeachOrgRequest request = buildValidRequest();
        request.setOrgCode(" ");

        Set<ConstraintViolation<CreateTeachOrgRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    /**
     * 校验组织名称超长时触发参数校验错误。
     */
    @Test
    void createRequestShouldRejectTooLongOrgName() {
        CreateTeachOrgRequest request = buildValidRequest();
        request.setOrgName(repeat("A", 129));

        Set<ConstraintViolation<CreateTeachOrgRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
    }

    /**
     * 校验教学组织 VO 不暴露软删除字段。
     *
     * @throws NoSuchFieldException 当字段契约被破坏时，测试应失败。
     */
    @Test
    void teachOrgVoShouldExposeOnlyResponseFields() throws NoSuchFieldException {
        Field orgCodeField = TeachOrgVO.class.getDeclaredField("orgCode");
        Field orgNameField = TeachOrgVO.class.getDeclaredField("orgName");
        Field orgTypeField = TeachOrgVO.class.getDeclaredField("orgType");

        assertEquals(String.class, orgCodeField.getType());
        assertEquals(String.class, orgNameField.getType());
        assertEquals(String.class, orgTypeField.getType());
        assertFalse(hasField("deleted"));
    }

    /**
     * 构造最小有效创建组织请求。
     *
     * @return 创建教学组织请求。
     */
    private CreateTeachOrgRequest buildValidRequest() {
        CreateTeachOrgRequest request = new CreateTeachOrgRequest();
        request.setTenantId("tenant_001");
        request.setOrgCode("class_2026_01");
        request.setOrgName("2026级实训1班");
        request.setOrgType("CLASS");
        return request;
    }

    /**
     * 判断 VO 是否包含指定字段。
     *
     * @param fieldName 字段名。
     * @return true 表示包含该字段。
     */
    private boolean hasField(String fieldName) {
        for (Field field : TeachOrgVO.class.getDeclaredFields()) {
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
