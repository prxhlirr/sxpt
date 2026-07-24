package com.sxpt.module.teaching;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.mapper.TaskStepMapper;
import com.sxpt.module.teaching.service.TaskStepService;
import com.sxpt.module.teaching.service.impl.TaskStepServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 教学任务步骤服务测试。
 *
 * 业务功能：
 * 1. 验证教学步骤创建会补齐通用生命周期字段。
 * 2. 验证教学步骤查询始终通过 Service 附加租户、任务、教学点和软删除边界。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class TaskStepServiceImplTests {

    private final TaskStepMapper mapper = mock(TaskStepMapper.class);

    private final TaskStepService service = new TaskStepServiceImpl(mapper);

    /**
     * 校验创建教学步骤时插入 Mapper 并补齐默认值。
     */
    @Test
    void createTaskStepShouldInsertAndFillDefaults() {
        TaskStep taskStep = buildValidTaskStep();

        TaskStep saved = service.createTaskStep(taskStep);

        assertSame(taskStep, saved);
        assertEquals(Boolean.TRUE, saved.getRequired());
        assertEquals(Boolean.FALSE, saved.getAllowSkip());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(mapper).insert(saved);
    }

    /**
     * 校验缺少排序号时拒绝创建，避免 SDK runtime 无法稳定排序。
     */
    @Test
    void createTaskStepShouldRejectMissingSequenceNo() {
        TaskStep taskStep = buildValidTaskStep();
        taskStep.setSequenceNo(null);

        assertThrows(BusinessException.class, () -> service.createTaskStep(taskStep));
        verify(mapper, times(0)).insert(taskStep);
    }

    /**
     * 校验按任务和教学点查询教学步骤时返回 Mapper 结果。
     */
    @Test
    void listByTaskAndTeachingPointShouldReturnMapperResult() {
        TaskStep taskStep = buildValidTaskStep();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(taskStep));

        List<TaskStep> result = service.listByTaskAndTeachingPoint("tenant_001", "task_001", "tp_001");

        assertEquals(1, result.size());
        assertSame(taskStep, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造最小有效教学步骤。
     *
     * @return 教学任务步骤实体。
     */
    private TaskStep buildValidTaskStep() {
        TaskStep taskStep = new TaskStep();
        taskStep.setId("step_001");
        taskStep.setTenantId("tenant_001");
        taskStep.setTaskId("task_001");
        taskStep.setTeachingPointId("tp_001");
        taskStep.setStepCode("STEP_001");
        taskStep.setStepName("填写申请信息");
        taskStep.setSequenceNo(1L);
        return taskStep;
    }
}
