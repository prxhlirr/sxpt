package com.sxpt.module.execution;

import com.sxpt.module.execution.dto.IdentitySwitchRunRequest;
import com.sxpt.module.execution.vo.IdentitySwitchRunVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 身份切换运行契约测试。
 *
 * 业务功能：
 * 1. 验证身份切换请求 DTO 的必填约束稳定。
 * 2. 验证身份切换返回 VO 不暴露内部令牌 hash 和软删除字段。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认 VO 字段边界。
 */
class IdentitySwitchRunContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效身份切换请求可以通过参数校验。
     */
    @Test
    void validRunRequestShouldPassValidation() {
        Set<ConstraintViolation<IdentitySwitchRunRequest>> violations = validator.validate(buildValidRequest());

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少下一步骤 ID 时参数校验失败。
     */
    @Test
    void runRequestShouldRejectMissingNextStepId() {
        IdentitySwitchRunRequest request = buildValidRequest();
        request.setNextStepId(" ");

        Set<ConstraintViolation<IdentitySwitchRunRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验 VO 不暴露内部敏感字段。
     */
    @Test
    void identitySwitchRunVoShouldHideSensitiveFields() {
        assertThrows(NoSuchFieldException.class, () -> IdentitySwitchRunVO.class.getDeclaredField("launchTokenHash"));
        assertThrows(NoSuchFieldException.class, () -> IdentitySwitchRunVO.class.getDeclaredField("deleted"));
    }

    /**
     * 构造最小有效身份切换请求。
     *
     * @return 身份切换运行请求。
     */
    private IdentitySwitchRunRequest buildValidRequest() {
        IdentitySwitchRunRequest request = new IdentitySwitchRunRequest();
        request.setTenantId("tenant_001");
        request.setStudentId("student_001");
        request.setConnectorSystemId("connector_001");
        request.setTaskId("task_001");
        request.setTeachingPointId("tp_001");
        request.setExecutionId("execution_001");
        request.setNextStepId("step_002");
        request.setSdkMode("PRACTICE");
        request.setTargetUrl("https://origin.example.com/practice/audit");
        return request;
    }
}
