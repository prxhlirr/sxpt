package com.sxpt.module.execution;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.execution.entity.TaskExecutionContext;
import com.sxpt.module.execution.mapper.TaskExecutionContextMapper;
import com.sxpt.module.execution.service.TaskExecutionContextService;
import com.sxpt.module.execution.service.impl.TaskExecutionContextServiceImpl;
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
 * 任务执行上下文快照服务测试。
 *
 * 业务功能：
 * 1. 验证创建快照时会补齐归档、通用状态和软删除字段。
 * 2. 验证查询快照时通过 Service 附加租户和软删除边界。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class TaskExecutionContextServiceImplTests {

    private final TaskExecutionContextMapper taskExecutionContextMapper = mock(TaskExecutionContextMapper.class);

    private final TaskExecutionContextService service =
            new TaskExecutionContextServiceImpl(taskExecutionContextMapper);

    /**
     * 校验创建上下文快照时插入 Mapper 并补齐默认值。
     */
    @Test
    void createContextShouldInsertAndFillDefaults() {
        TaskExecutionContext context = buildValidContext();

        TaskExecutionContext saved = service.createContext(context);

        assertSame(context, saved);
        assertEquals("NONE", saved.getArchiveStatus());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(taskExecutionContextMapper).insert(saved);
    }

    /**
     * 校验缺少完整上下文 JSON 时拒绝创建。
     */
    @Test
    void createContextShouldRejectMissingContextJson() {
        TaskExecutionContext context = buildValidContext();
        context.setContextJson(" ");

        assertThrows(BusinessException.class, () -> service.createContext(context));
        verify(taskExecutionContextMapper, times(0)).insert(context);
    }

    /**
     * 校验查询上下文快照时返回 Mapper 结果。
     */
    @Test
    void listContextsShouldReturnMapperResult() {
        TaskExecutionContext context = buildValidContext();
        when(taskExecutionContextMapper.selectList(any())).thenReturn(Collections.singletonList(context));

        List<TaskExecutionContext> result =
                service.listContexts("tenant_001", "execution_001", "task_001", "student_001");

        assertEquals(1, result.size());
        assertSame(context, result.get(0));
        verify(taskExecutionContextMapper).selectList(any());
    }

    /**
     * 构造最小有效任务执行上下文快照。
     *
     * @return 任务执行上下文快照实体。
     */
    private TaskExecutionContext buildValidContext() {
        TaskExecutionContext context = new TaskExecutionContext();
        context.setId("context_001");
        context.setTenantId("tenant_001");
        context.setExecutionId("execution_001");
        context.setTaskId("task_001");
        context.setStudentId("student_001");
        context.setSdkMode("LEARNING");
        context.setContextJson("{\"taskVersion\":1}");
        return context;
    }
}
