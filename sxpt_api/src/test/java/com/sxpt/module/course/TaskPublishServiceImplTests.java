package com.sxpt.module.course;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.course.entity.Task;
import com.sxpt.module.course.entity.TaskTeachingPoint;
import com.sxpt.module.course.mapper.TaskMapper;
import com.sxpt.module.course.mapper.TaskTeachingPointMapper;
import com.sxpt.module.course.service.TaskPublishService;
import com.sxpt.module.course.service.impl.TaskPublishServiceImpl;
import com.sxpt.module.teaching.service.TeachingAssetPublishCheckService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 任务发布服务测试。
 *
 * 业务功能：
 * 1. 验证任务和任务教学点关联创建会补齐默认生命周期字段。
 * 2. 验证任务查询和教学点关联查询通过 Service 附加租户和软删除边界。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class TaskPublishServiceImplTests {

    private final TaskMapper taskMapper = mock(TaskMapper.class);

    private final TaskTeachingPointMapper taskTeachingPointMapper = mock(TaskTeachingPointMapper.class);

    private final TeachingAssetPublishCheckService teachingAssetPublishCheckService =
            mock(TeachingAssetPublishCheckService.class);

    private final TaskPublishService service = new TaskPublishServiceImpl(
            taskMapper, taskTeachingPointMapper, teachingAssetPublishCheckService);

    /**
     * 校验创建任务时插入 Mapper 并补齐默认值。
     */
    @Test
    void createTaskShouldInsertAndFillDefaults() {
        Task task = buildValidTask();

        Task saved = service.createTask(task);

        assertSame(task, saved);
        assertEquals(1L, saved.getVersionNo());
        assertEquals("DRAFT", saved.getTaskStatus());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(taskMapper).insert(saved);
    }

    /**
     * 校验缺少任务目标时拒绝创建。
     */
    @Test
    void createTaskShouldRejectMissingTaskGoal() {
        Task task = buildValidTask();
        task.setTaskGoal(" ");

        assertThrows(BusinessException.class, () -> service.createTask(task));
        verify(taskMapper, times(0)).insert(task);
    }

    /**
     * 校验创建任务教学点关联时插入 Mapper 并补齐默认值。
     */
    @Test
    void createTaskTeachingPointShouldInsertAndFillDefaults() {
        TaskTeachingPoint taskTeachingPoint = buildValidTaskTeachingPoint();

        TaskTeachingPoint saved = service.createTaskTeachingPoint(taskTeachingPoint);

        assertSame(taskTeachingPoint, saved);
        assertEquals(Boolean.TRUE, saved.getRequiredFlag());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(taskTeachingPointMapper).insert(saved);
    }

    /**
     * 校验缺少排序号时拒绝创建任务教学点关联。
     */
    @Test
    void createTaskTeachingPointShouldRejectMissingSequenceNo() {
        TaskTeachingPoint taskTeachingPoint = buildValidTaskTeachingPoint();
        taskTeachingPoint.setSequenceNo(null);

        assertThrows(BusinessException.class, () -> service.createTaskTeachingPoint(taskTeachingPoint));
        verify(taskTeachingPointMapper, times(0)).insert(taskTeachingPoint);
    }

    /**
     * 校验查询任务时返回 Mapper 结果。
     */
    @Test
    void listTasksShouldReturnMapperResult() {
        Task task = buildValidTask();
        when(taskMapper.selectList(any())).thenReturn(Collections.singletonList(task));

        List<Task> result = service.listTasks("tenant_001", "course_001", "org_001");

        assertEquals(1, result.size());
        assertSame(task, result.get(0));
        verify(taskMapper).selectList(any());
    }

    /**
     * 校验查询任务教学点关联时返回 Mapper 结果。
     */
    @Test
    void listTeachingPointsByTaskShouldReturnMapperResult() {
        TaskTeachingPoint taskTeachingPoint = buildValidTaskTeachingPoint();
        when(taskTeachingPointMapper.selectList(any())).thenReturn(Collections.singletonList(taskTeachingPoint));

        List<TaskTeachingPoint> result = service.listTeachingPointsByTask("tenant_001", "task_001");

        assertEquals(1, result.size());
        assertSame(taskTeachingPoint, result.get(0));
        verify(taskTeachingPointMapper).selectList(any());
    }

    /**
     * 校验发布任务教学点资产时先执行完整性校验，通过后才更新任务发布状态。
     */
    @Test
    void publishTaskTeachingPointAssetsShouldValidateBeforePublishingTask() {
        Task task = buildValidTask();
        task.setTaskStatus("DRAFT");
        when(taskMapper.selectOne(any())).thenReturn(task);

        Task published = service.publishTaskTeachingPointAssets(
                "tenant_001", "task_001", "tp_001", "rule_001", "teacher_001");

        assertSame(task, published);
        assertEquals("PUBLISHED", published.getTaskStatus());
        assertEquals("teacher_001", published.getUpdateBy());
        assertNotNull(published.getUpdateTime());
        verify(teachingAssetPublishCheckService).validateReadyToPublish(
                "tenant_001", "task_001", "tp_001", "rule_001");
        verify(taskMapper).updateById(task);
    }

    /**
     * 校验发布前完整性校验失败时不更新任务，避免不完整备案资产被真实发布动作绕过。
     */
    @Test
    void publishTaskTeachingPointAssetsShouldRejectWhenPublishCheckFails() {
        doThrow(new BusinessException(com.sxpt.common.api.ApiResultCode.STATE_NOT_ALLOWED))
                .when(teachingAssetPublishCheckService)
                .validateReadyToPublish("tenant_001", "task_001", "tp_001", "rule_001");

        assertThrows(BusinessException.class, () -> service.publishTaskTeachingPointAssets(
                "tenant_001", "task_001", "tp_001", "rule_001", "teacher_001"));
        verify(taskMapper, times(0)).selectOne(any());
        verify(taskMapper, times(0)).updateById(any());
    }

    /**
     * 构造最小有效任务。
     *
     * @return 教学任务实体。
     */
    private Task buildValidTask() {
        Task task = new Task();
        task.setId("task_001");
        task.setTenantId("tenant_001");
        task.setCourseId("course_001");
        task.setPublishOrgId("org_001");
        task.setTaskCode("TASK_LEARN_RECORD");
        task.setTaskName("备案学习任务");
        task.setTaskType("LEARNING");
        task.setTaskGoal("完成备案流程学习");
        return task;
    }

    /**
     * 构造最小有效任务教学点关联。
     *
     * @return 任务教学点关联实体。
     */
    private TaskTeachingPoint buildValidTaskTeachingPoint() {
        TaskTeachingPoint taskTeachingPoint = new TaskTeachingPoint();
        taskTeachingPoint.setId("task_point_001");
        taskTeachingPoint.setTenantId("tenant_001");
        taskTeachingPoint.setTaskId("task_001");
        taskTeachingPoint.setTeachingPointId("tp_001");
        taskTeachingPoint.setSequenceNo(1L);
        return taskTeachingPoint;
    }
}
