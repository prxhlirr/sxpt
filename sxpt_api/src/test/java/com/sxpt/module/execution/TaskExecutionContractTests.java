package com.sxpt.module.execution;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.execution.dto.StartTaskExecutionRequest;
import com.sxpt.module.execution.dto.SubmitTaskExecutionRequest;
import com.sxpt.module.execution.entity.TaskExecution;
import com.sxpt.module.execution.mapper.TaskExecutionMapper;
import com.sxpt.module.execution.vo.TaskExecutionVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 学生任务执行主聚合契约测试。
 *
 * 业务功能：
 * 1. 验证开始和提交 DTO 的必填字段边界。
 * 2. 验证 TaskExecution 实体、Mapper 和 VO 的对外契约稳定。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 验证请求对象。
 * 2. 使用反射确认实体表名、主键注解、Mapper 泛型和 VO 隐藏字段。
 */
class TaskExecutionContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 验证最小有效开始执行请求可以通过参数校验。
     */
    @Test
    void validStartRequestShouldPassValidation() {
        Set<ConstraintViolation<StartTaskExecutionRequest>> violations =
                validator.validate(buildValidStartRequest());

        assertEquals(0, violations.size());
    }

    /**
     * 验证缺少学生 ID 时开始执行请求会被拒绝。
     */
    @Test
    void startRequestShouldRejectMissingStudentId() {
        StartTaskExecutionRequest request = buildValidStartRequest();
        request.setStudentId(" ");

        Set<ConstraintViolation<StartTaskExecutionRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 验证最小有效提交执行请求可以通过参数校验。
     */
    @Test
    void validSubmitRequestShouldPassValidation() {
        Set<ConstraintViolation<SubmitTaskExecutionRequest>> violations =
                validator.validate(buildValidSubmitRequest());

        assertEquals(0, violations.size());
    }

    /**
     * 验证缺少执行记录 ID 时提交请求会被拒绝。
     */
    @Test
    void submitRequestShouldRejectMissingExecutionId() {
        SubmitTaskExecutionRequest request = buildValidSubmitRequest();
        request.setExecutionId(" ");

        Set<ConstraintViolation<SubmitTaskExecutionRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 验证实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void taskExecutionEntityShouldMapTable() throws NoSuchFieldException {
        TableName tableName = TaskExecution.class.getAnnotation(TableName.class);
        Field idField = TaskExecution.class.getDeclaredField("id");

        assertEquals("task_execution", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 验证 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定正确实体。
     */
    @Test
    void taskExecutionMapperShouldBindEntity() {
        ParameterizedType baseMapperType = (ParameterizedType) TaskExecutionMapper.class.getGenericInterfaces()[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(TaskExecution.class, baseMapperType.getActualTypeArguments()[0]);
    }

    /**
     * 验证 VO 不暴露内部软删除字段。
     */
    @Test
    void taskExecutionVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> TaskExecutionVO.class.getDeclaredField("deleted"));
    }

    /**
     * 构造最小有效开始执行请求。
     *
     * @return 学生开始任务执行请求。
     */
    private StartTaskExecutionRequest buildValidStartRequest() {
        StartTaskExecutionRequest request = new StartTaskExecutionRequest();
        request.setTenantId("tenant_001");
        request.setTaskId("task_001");
        request.setStudentId("student_001");
        request.setConnectorSystemId("connector_001");
        request.setExecutionMode("PRACTICE");
        request.setSdkMode("PRACTICE");
        request.setExecutionIdentityJson("{\"identityMode\":\"STUDENT\"}");
        request.setCreateBy("student_001");
        return request;
    }

    /**
     * 构造最小有效提交执行请求。
     *
     * @return 学生提交任务执行请求。
     */
    private SubmitTaskExecutionRequest buildValidSubmitRequest() {
        SubmitTaskExecutionRequest request = new SubmitTaskExecutionRequest();
        request.setTenantId("tenant_001");
        request.setExecutionId("execution_001");
        request.setOperatorId("student_001");
        return request;
    }
}
