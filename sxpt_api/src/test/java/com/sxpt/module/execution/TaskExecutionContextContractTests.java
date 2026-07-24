package com.sxpt.module.execution;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.execution.dto.CreateTaskExecutionContextRequest;
import com.sxpt.module.execution.entity.TaskExecutionContext;
import com.sxpt.module.execution.mapper.TaskExecutionContextMapper;
import com.sxpt.module.execution.vo.TaskExecutionContextVO;
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
 * 任务执行上下文快照契约测试。
 *
 * 业务功能：
 * 1. 验证创建上下文快照 DTO 的必填和长度约束。
 * 2. 验证实体、Mapper 和 VO 的对外契约稳定。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认实体表名、Mapper 泛型和 VO 字段边界。
 */
class TaskExecutionContextContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效创建上下文快照请求可以通过参数校验。
     */
    @Test
    void validCreateContextRequestShouldPassValidation() {
        Set<ConstraintViolation<CreateTaskExecutionContextRequest>> violations =
                validator.validate(buildValidRequest());

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少上下文 JSON 时参数校验失败。
     */
    @Test
    void createContextRequestShouldRejectMissingContextJson() {
        CreateTaskExecutionContextRequest request = buildValidRequest();
        request.setContextJson(" ");

        Set<ConstraintViolation<CreateTaskExecutionContextRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验 VO 不暴露内部软删除字段。
     */
    @Test
    void contextVoShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> TaskExecutionContextVO.class.getDeclaredField("deleted"));
    }

    /**
     * 校验实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void contextEntityShouldMapTable() throws NoSuchFieldException {
        TableName tableName = TaskExecutionContext.class.getAnnotation(TableName.class);
        Field idField = TaskExecutionContext.class.getDeclaredField("id");

        assertEquals("task_execution_context", tableName.value());
        assertTrue(idField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定正确实体。
     */
    @Test
    void contextMapperShouldBindEntity() {
        ParameterizedType baseMapperType =
                (ParameterizedType) TaskExecutionContextMapper.class.getGenericInterfaces()[0];

        assertEquals(BaseMapper.class, baseMapperType.getRawType());
        assertEquals(TaskExecutionContext.class, baseMapperType.getActualTypeArguments()[0]);
    }

    /**
     * 构造最小有效创建上下文快照请求。
     *
     * @return 创建任务执行上下文快照请求。
     */
    private CreateTaskExecutionContextRequest buildValidRequest() {
        CreateTaskExecutionContextRequest request = new CreateTaskExecutionContextRequest();
        request.setTenantId("tenant_001");
        request.setExecutionId("execution_001");
        request.setTaskId("task_001");
        request.setStudentId("student_001");
        request.setSdkMode("LEARNING");
        request.setContextJson("{\"taskVersion\":1}");
        request.setOverlayPolicyJson("{\"mask\":true}");
        request.setTeachingPointSnapshotJson("[{\"id\":\"tp_001\"}]");
        request.setResourceSnapshotJson("[{\"id\":\"resource_001\"}]");
        request.setEvaluationSnapshotJson("[{\"id\":\"rule_001\"}]");
        request.setCreateBy("student_001");
        return request;
    }
}
