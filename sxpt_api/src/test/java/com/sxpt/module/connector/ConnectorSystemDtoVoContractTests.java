package com.sxpt.module.connector;

import com.sxpt.module.connector.dto.CreateConnectorSystemRequest;
import com.sxpt.module.connector.dto.UpdateConnectorSystemRequest;
import com.sxpt.module.connector.vo.ConnectorSystemVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 原业务平台配置 DTO 和 VO 契约测试。
 *
 * 业务功能：
 * 1. 验证创建原平台配置的请求参数具备基础校验。
 * 2. 验证返回对象不默认暴露内部配置 JSON。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 直接校验 DTO，不依赖 Spring 容器。
 * 2. 使用反射确认 VO 字段边界，避免后续误把内部配置返回给前端。
 */
class ConnectorSystemDtoVoContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验完整请求不产生参数错误。
     */
    @Test
    void createRequestShouldPassWhenRequiredFieldsExist() {
        CreateConnectorSystemRequest request = buildValidRequest();

        Set<ConstraintViolation<CreateConnectorSystemRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少原平台编码时触发参数错误。
     */
    @Test
    void createRequestShouldRejectBlankSystemCode() {
        CreateConnectorSystemRequest request = buildValidRequest();
        request.setSystemCode(" ");

        Set<ConstraintViolation<CreateConnectorSystemRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("systemCode", violations.iterator().next().getPropertyPath().toString());
    }

    /**
     * 校验完整更新请求不产生参数错误。
     */
    @Test
    void updateRequestShouldPassWhenRequiredFieldsExist() {
        UpdateConnectorSystemRequest request = buildValidUpdateRequest();

        Set<ConstraintViolation<UpdateConnectorSystemRequest>> violations = validator.validate(request);

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少原平台配置 ID 时触发参数错误。
     */
    @Test
    void updateRequestShouldRejectBlankId() {
        UpdateConnectorSystemRequest request = buildValidUpdateRequest();
        request.setId(" ");

        Set<ConstraintViolation<UpdateConnectorSystemRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        assertEquals("id", violations.iterator().next().getPropertyPath().toString());
    }

    /**
     * 校验更新请求不允许修改租户 ID 和原平台编码。
     *
     * @throws NoSuchFieldException 当 DTO 暴露不可变字段时，测试应失败。
     */
    @Test
    void updateRequestShouldNotExposeStableIdentityFields() throws NoSuchFieldException {
        assertThrows(NoSuchFieldException.class, () -> UpdateConnectorSystemRequest.class.getDeclaredField("tenantId"));
        assertThrows(NoSuchFieldException.class, () -> UpdateConnectorSystemRequest.class.getDeclaredField("systemCode"));
    }

    /**
     * 校验返回对象不包含内部配置 JSON。
     *
     * @throws NoSuchFieldException 当 VO 暴露 configJson 字段时，测试应失败。
     */
    @Test
    void connectorSystemVoShouldNotExposeConfigJsonByDefault() {
        assertThrows(NoSuchFieldException.class, () -> findField("configJson"));
    }

    /**
     * 通过反射查找 VO 字段。
     *
     * @param fieldName 字段名。
     * @return 字段定义。
     * @throws NoSuchFieldException 字段不存在时抛出。
     */
    private Field findField(String fieldName) throws NoSuchFieldException {
        return ConnectorSystemVO.class.getDeclaredField(fieldName);
    }

    /**
     * 构造最小有效创建请求。
     *
     * @return 创建原平台配置请求。
     */
    private CreateConnectorSystemRequest buildValidRequest() {
        CreateConnectorSystemRequest request = new CreateConnectorSystemRequest();
        request.setTenantId("tenant_001");
        request.setSystemCode("origin_platform");
        request.setSystemName("原业务平台");
        request.setSystemType("CUSTOM");
        request.setBaseUrl("https://origin.example.com");
        request.setAuthType("TOKEN");
        return request;
    }

    /**
     * 构造最小有效更新请求。
     *
     * @return 更新原平台配置请求。
     */
    private UpdateConnectorSystemRequest buildValidUpdateRequest() {
        UpdateConnectorSystemRequest request = new UpdateConnectorSystemRequest();
        request.setId("connector_001");
        request.setSystemName("原业务平台");
        request.setSystemType("CUSTOM");
        request.setBaseUrl("https://origin.example.com");
        request.setAuthType("TOKEN");
        return request;
    }
}
