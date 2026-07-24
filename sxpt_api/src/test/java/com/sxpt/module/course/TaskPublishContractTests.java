package com.sxpt.module.course;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.course.dto.CreateTaskRequest;
import com.sxpt.module.course.dto.CreateTaskTeachingPointRequest;
import com.sxpt.module.course.entity.Task;
import com.sxpt.module.course.entity.TaskTeachingPoint;
import com.sxpt.module.course.mapper.TaskMapper;
import com.sxpt.module.course.mapper.TaskTeachingPointMapper;
import com.sxpt.module.course.vo.TaskTeachingPointVO;
import com.sxpt.module.course.vo.TaskVO;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 任务发布模块契约测试。
 *
 * 业务功能：
 * 1. 验证任务和任务教学点 DTO 的必填和长度约束。
 * 2. 验证实体、Mapper 和 VO 的对外契约稳定。
 *
 * 关键流程：
 * 1. 使用 Bean Validation 校验请求对象。
 * 2. 使用反射确认实体表名、Mapper 泛型和 VO 字段边界。
 */
class TaskPublishContractTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    /**
     * 校验最小有效创建任务请求可以通过参数校验。
     */
    @Test
    void validCreateTaskRequestShouldPassValidation() {
        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(buildValidTaskRequest());

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少任务目标时参数校验失败。
     */
    @Test
    void createTaskRequestShouldRejectMissingTaskGoal() {
        CreateTaskRequest request = buildValidTaskRequest();
        request.setTaskGoal(" ");

        Set<ConstraintViolation<CreateTaskRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验最小有效任务教学点关联请求可以通过参数校验。
     */
    @Test
    void validCreateTaskTeachingPointRequestShouldPassValidation() {
        Set<ConstraintViolation<CreateTaskTeachingPointRequest>> violations =
                validator.validate(buildValidTaskTeachingPointRequest());

        assertEquals(0, violations.size());
    }

    /**
     * 校验缺少排序号时参数校验失败。
     */
    @Test
    void createTaskTeachingPointRequestShouldRejectMissingSequenceNo() {
        CreateTaskTeachingPointRequest request = buildValidTaskTeachingPointRequest();
        request.setSequenceNo(null);

        Set<ConstraintViolation<CreateTaskTeachingPointRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
    }

    /**
     * 校验 VO 不暴露内部软删除字段。
     */
    @Test
    void taskVosShouldHideDeletedField() {
        assertThrows(NoSuchFieldException.class, () -> TaskVO.class.getDeclaredField("deleted"));
        assertThrows(NoSuchFieldException.class, () -> TaskTeachingPointVO.class.getDeclaredField("deleted"));
    }

    /**
     * 校验实体表名和主键注解。
     *
     * @throws NoSuchFieldException 当实体主键字段被误删或重命名时，测试应失败。
     */
    @Test
    void taskEntitiesShouldMapTables() throws NoSuchFieldException {
        TableName taskTableName = Task.class.getAnnotation(TableName.class);
        Field taskIdField = Task.class.getDeclaredField("id");
        TableName taskPointTableName = TaskTeachingPoint.class.getAnnotation(TableName.class);
        Field taskPointIdField = TaskTeachingPoint.class.getDeclaredField("id");

        assertEquals("task", taskTableName.value());
        assertTrue(taskIdField.isAnnotationPresent(TableId.class));
        assertEquals("task_teaching_point", taskPointTableName.value());
        assertTrue(taskPointIdField.isAnnotationPresent(TableId.class));
    }

    /**
     * 校验 Mapper 继承 MyBatis Plus 标准 Mapper 并绑定正确实体。
     */
    @Test
    void taskMappersShouldBindEntities() {
        ParameterizedType taskBaseMapperType = (ParameterizedType) TaskMapper.class.getGenericInterfaces()[0];
        ParameterizedType taskPointBaseMapperType =
                (ParameterizedType) TaskTeachingPointMapper.class.getGenericInterfaces()[0];

        assertEquals(BaseMapper.class, taskBaseMapperType.getRawType());
        assertEquals(Task.class, taskBaseMapperType.getActualTypeArguments()[0]);
        assertEquals(BaseMapper.class, taskPointBaseMapperType.getRawType());
        assertEquals(TaskTeachingPoint.class, taskPointBaseMapperType.getActualTypeArguments()[0]);
    }

    /**
     * 构造最小有效创建任务请求。
     *
     * @return 创建任务请求。
     */
    private CreateTaskRequest buildValidTaskRequest() {
        CreateTaskRequest request = new CreateTaskRequest();
        request.setTenantId("tenant_001");
        request.setCourseId("course_001");
        request.setPublishOrgId("org_001");
        request.setTaskCode("TASK_LEARN_RECORD");
        request.setTaskName("备案学习任务");
        request.setTaskType("LEARNING");
        request.setTaskGoal("完成备案流程学习");
        request.setTaskDescription("任务说明");
        request.setOverlayPolicyJson("{\"mask\":true}");
        request.setCreateBy("teacher_001");
        return request;
    }

    /**
     * 构造最小有效任务教学点关联请求。
     *
     * @return 创建任务教学点关联请求。
     */
    private CreateTaskTeachingPointRequest buildValidTaskTeachingPointRequest() {
        CreateTaskTeachingPointRequest request = new CreateTaskTeachingPointRequest();
        request.setTenantId("tenant_001");
        request.setTaskId("task_001");
        request.setTeachingPointId("tp_001");
        request.setRequiredFlag(Boolean.TRUE);
        request.setSequenceNo(1L);
        request.setCreateBy("teacher_001");
        return request;
    }
}
