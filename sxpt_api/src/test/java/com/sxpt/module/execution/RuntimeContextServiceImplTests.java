package com.sxpt.module.execution;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.execution.dto.RuntimeContextQueryRequest;
import com.sxpt.module.execution.entity.TaskExecution;
import com.sxpt.module.execution.entity.TaskExecutionContext;
import com.sxpt.module.execution.service.RuntimeContextService;
import com.sxpt.module.execution.service.TaskExecutionService;
import com.sxpt.module.execution.service.TaskExecutionContextService;
import com.sxpt.module.execution.service.impl.RuntimeContextServiceImpl;
import com.sxpt.module.execution.vo.RuntimeContextVO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SDK 运行上下文服务测试。
 *
 * 业务功能：
 * 1. 验证 Service 可将已固化的执行上下文快照转换为 SDK 响应。
 * 2. 验证快照不存在或定位结果不唯一时拒绝返回不确定上下文。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 TaskExecutionContextService，聚焦运行上下文组装规则。
 * 2. 直接调用 RuntimeContextService，覆盖成功和业务异常分支。
 */
class RuntimeContextServiceImplTests {

    private final TaskExecutionContextService taskExecutionContextService = mock(TaskExecutionContextService.class);

    private final TaskExecutionService taskExecutionService = mock(TaskExecutionService.class);

    private final RuntimeContextService service = new RuntimeContextServiceImpl(
            taskExecutionContextService, taskExecutionService);

    /**
     * 校验成功读取并转换 SDK 运行上下文。
     */
    @Test
    void getRuntimeContextShouldReturnSnapshotContext() {
        RuntimeContextQueryRequest request = buildRequest();
        TaskExecutionContext context = buildContext();
        when(taskExecutionService.getExecution("tenant_001", "execution_001")).thenReturn(buildExecution("RUNNING"));
        when(taskExecutionContextService.listContexts("tenant_001", "execution_001", "task_001", "student_001"))
                .thenReturn(Collections.singletonList(context));

        RuntimeContextVO result = service.getRuntimeContext(request);

        assertEquals("context_001", result.getContextId());
        assertEquals("LEARNING", result.getSdkMode());
        assertEquals("{\"taskVersion\":1}", result.getContextJson());
        assertEquals("[{\"id\":\"tp_001\"}]", result.getTeachingPointSnapshotJson());
        verify(taskExecutionService).getExecution("tenant_001", "execution_001");
        verify(taskExecutionContextService).listContexts("tenant_001", "execution_001", "task_001", "student_001");
    }

    /**
     * 校验缺少执行 ID 时拒绝查询。
     */
    @Test
    void getRuntimeContextShouldRejectMissingExecutionId() {
        RuntimeContextQueryRequest request = buildRequest();
        request.setExecutionId(" ");

        assertThrows(BusinessException.class, () -> service.getRuntimeContext(request));
    }

    /**
     * 校验快照不存在时返回业务异常。
     */
    @Test
    void getRuntimeContextShouldRejectMissingContext() {
        RuntimeContextQueryRequest request = buildRequest();
        when(taskExecutionService.getExecution("tenant_001", "execution_001")).thenReturn(buildExecution("RUNNING"));
        when(taskExecutionContextService.listContexts("tenant_001", "execution_001", "task_001", "student_001"))
                .thenReturn(Collections.emptyList());

        assertThrows(BusinessException.class, () -> service.getRuntimeContext(request));
    }

    /**
     * 校验定位到多条快照时拒绝返回不确定上下文。
     */
    @Test
    void getRuntimeContextShouldRejectDuplicateContexts() {
        RuntimeContextQueryRequest request = buildRequest();
        when(taskExecutionService.getExecution("tenant_001", "execution_001")).thenReturn(buildExecution("RUNNING"));
        when(taskExecutionContextService.listContexts("tenant_001", "execution_001", "task_001", "student_001"))
                .thenReturn(Arrays.asList(buildContext(), buildContext()));

        assertThrows(BusinessException.class, () -> service.getRuntimeContext(request));
    }

    /**
     * 校验非运行态执行不能继续读取 SDK 上下文。
     */
    @Test
    void getRuntimeContextShouldRejectNonRunningExecution() {
        RuntimeContextQueryRequest request = buildRequest();
        when(taskExecutionService.getExecution("tenant_001", "execution_001")).thenReturn(buildExecution("SUBMITTED"));

        assertThrows(BusinessException.class, () -> service.getRuntimeContext(request));
    }

    /**
     * 校验请求中的任务 ID 与执行主记录不一致时拒绝读取，避免跨任务串线。
     */
    @Test
    void getRuntimeContextShouldRejectMismatchedTaskId() {
        RuntimeContextQueryRequest request = buildRequest();
        request.setTaskId("task_other");
        when(taskExecutionService.getExecution("tenant_001", "execution_001")).thenReturn(buildExecution("RUNNING"));

        assertThrows(BusinessException.class, () -> service.getRuntimeContext(request));
    }

    /**
     * 校验请求中的学生 ID 与执行主记录不一致时拒绝读取，避免跨学生串线。
     */
    @Test
    void getRuntimeContextShouldRejectMismatchedStudentId() {
        RuntimeContextQueryRequest request = buildRequest();
        request.setStudentId("student_other");
        when(taskExecutionService.getExecution("tenant_001", "execution_001")).thenReturn(buildExecution("RUNNING"));

        assertThrows(BusinessException.class, () -> service.getRuntimeContext(request));
    }

    /**
     * 构造 SDK 运行上下文查询请求。
     *
     * @return SDK 运行上下文查询请求。
     */
    private RuntimeContextQueryRequest buildRequest() {
        RuntimeContextQueryRequest request = new RuntimeContextQueryRequest();
        request.setTenantId("tenant_001");
        request.setExecutionId("execution_001");
        request.setTaskId("task_001");
        request.setStudentId("student_001");
        return request;
    }

    /**
     * 构造学生任务执行主记录。
     *
     * @param executionStatus 执行状态。
     * @return 学生任务执行主记录。
     */
    private TaskExecution buildExecution(String executionStatus) {
        TaskExecution execution = new TaskExecution();
        execution.setId("execution_001");
        execution.setTenantId("tenant_001");
        execution.setTaskId("task_001");
        execution.setStudentId("student_001");
        execution.setExecutionStatus(executionStatus);
        return execution;
    }

    /**
     * 构造任务执行上下文快照。
     *
     * @return 任务执行上下文快照实体。
     */
    private TaskExecutionContext buildContext() {
        TaskExecutionContext context = new TaskExecutionContext();
        context.setId("context_001");
        context.setTenantId("tenant_001");
        context.setExecutionId("execution_001");
        context.setTaskId("task_001");
        context.setStudentId("student_001");
        context.setSdkMode("LEARNING");
        context.setContextJson("{\"taskVersion\":1}");
        context.setOverlayPolicyJson("{\"mask\":true}");
        context.setTeachingPointSnapshotJson("[{\"id\":\"tp_001\"}]");
        context.setResourceSnapshotJson("[{\"id\":\"resource_001\"}]");
        context.setEvaluationSnapshotJson("[{\"id\":\"rule_001\"}]");
        context.setArchiveStatus("NONE");
        return context;
    }
}
