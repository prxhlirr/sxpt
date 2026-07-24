package com.sxpt.module.execution;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.execution.entity.ExecutionTrace;
import com.sxpt.module.execution.entity.TaskExecution;
import com.sxpt.module.execution.mapper.ExecutionTraceMapper;
import com.sxpt.module.execution.mapper.TaskExecutionMapper;
import com.sxpt.module.execution.service.ExecutionTraceService;
import com.sxpt.module.execution.service.impl.ExecutionTraceServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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
 * 学生执行轨迹服务测试。
 *
 * 业务功能：
 * 1. 验证 SDK 轨迹上报会补齐归档、重试次数和通用生命周期默认值。
 * 2. 验证 clientTraceId 幂等规则，避免 SDK 网络重试造成重复入库。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 Mapper，聚焦 Service 业务规则。
 * 2. 直接调用 Service，避免单元测试依赖真实数据库。
 */
class ExecutionTraceServiceImplTests {

    private final ExecutionTraceMapper mapper = mock(ExecutionTraceMapper.class);

    private final TaskExecutionMapper taskExecutionMapper = mock(TaskExecutionMapper.class);

    private final ExecutionTraceService service = new ExecutionTraceServiceImpl(mapper, taskExecutionMapper);

    /**
     * 校验轨迹首次上报时插入 Mapper 并补齐默认值。
     */
    @Test
    void reportExecutionTraceShouldInsertAndFillDefaults() {
        ExecutionTrace executionTrace = buildValidTrace();
        when(taskExecutionMapper.selectOne(any())).thenReturn(buildExecution("RUNNING"), null);

        ExecutionTrace saved = service.reportExecutionTrace(executionTrace);

        assertSame(executionTrace, saved);
        assertEquals(0L, saved.getRetryCount());
        assertEquals("NONE", saved.getArchiveStatus());
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Boolean.FALSE, saved.getDeleted());
        assertNotNull(saved.getCreateTime());
        assertNotNull(saved.getUpdateTime());
        verify(taskExecutionMapper).selectOne(any());
        verify(mapper).selectOne(any());
        verify(mapper).insert(saved);
    }

    /**
     * 校验同一个 clientTraceId 重试上报时返回既有轨迹，不重复插入。
     */
    @Test
    void reportExecutionTraceShouldReturnExistedClientTrace() {
        ExecutionTrace executionTrace = buildValidTrace();
        ExecutionTrace existed = buildValidTrace();
        existed.setId("trace_existed");
        when(taskExecutionMapper.selectOne(any())).thenReturn(buildExecution("RUNNING"));
        when(mapper.selectOne(any())).thenReturn(existed);

        ExecutionTrace result = service.reportExecutionTrace(executionTrace);

        assertSame(existed, result);
        verify(taskExecutionMapper).selectOne(any());
        verify(mapper).selectOne(any());
        verify(mapper, times(0)).insert(executionTrace);
    }

    /**
     * 校验非运行态执行不能继续写入轨迹，避免学生提交后仍污染评分事实。
     */
    @Test
    void reportExecutionTraceShouldRejectNonRunningExecution() {
        ExecutionTrace executionTrace = buildValidTrace();
        when(taskExecutionMapper.selectOne(any())).thenReturn(buildExecution("SUBMITTED"));

        assertThrows(BusinessException.class, () -> service.reportExecutionTrace(executionTrace));
        verify(taskExecutionMapper).selectOne(any());
        verify(mapper, times(0)).insert(executionTrace);
    }

    /**
     * 校验缺少轨迹类型时拒绝上报，避免产生无法归类的执行事实。
     */
    @Test
    void reportExecutionTraceShouldRejectMissingTraceType() {
        ExecutionTrace executionTrace = buildValidTrace();
        executionTrace.setTraceType(" ");

        assertThrows(BusinessException.class, () -> service.reportExecutionTrace(executionTrace));
        verify(taskExecutionMapper, times(0)).selectOne(any());
        verify(mapper, times(0)).insert(executionTrace);
    }

    /**
     * 校验顺序号必须大于 0，避免无效顺序污染评分事实排序。
     */
    @Test
    void reportExecutionTraceShouldRejectNonPositiveSequenceNo() {
        ExecutionTrace executionTrace = buildValidTrace();
        executionTrace.setSequenceNo(0L);

        assertThrows(BusinessException.class, () -> service.reportExecutionTrace(executionTrace));
        verify(taskExecutionMapper, times(0)).selectOne(any());
        verify(mapper, times(0)).insert(executionTrace);
    }

    /**
     * 校验轨迹摘要不能包含明显敏感字段名，避免明文隐私进入评分事实表。
     */
    @Test
    void reportExecutionTraceShouldRejectSensitivePayload() {
        ExecutionTrace executionTrace = buildValidTrace();
        executionTrace.setInputDataJson("{\"password\":\"plain-text\"}");

        assertThrows(BusinessException.class, () -> service.reportExecutionTrace(executionTrace));
        verify(taskExecutionMapper, times(0)).selectOne(any());
        verify(mapper, times(0)).insert(executionTrace);
    }

    /**
     * 校验按任务执行查询轨迹时返回 Mapper 结果。
     */
    @Test
    void listByExecutionShouldReturnMapperResult() {
        ExecutionTrace executionTrace = buildValidTrace();
        when(mapper.selectList(any())).thenReturn(Collections.singletonList(executionTrace));

        List<ExecutionTrace> result = service.listByExecution("tenant_001", "execution_001");

        assertEquals(1, result.size());
        assertSame(executionTrace, result.get(0));
        verify(mapper).selectList(any());
    }

    /**
     * 构造最小有效 SDK 执行轨迹。
     *
     * @return SDK 执行轨迹实体。
     */
    private ExecutionTrace buildValidTrace() {
        ExecutionTrace executionTrace = new ExecutionTrace();
        executionTrace.setId("trace_001");
        executionTrace.setTenantId("tenant_001");
        executionTrace.setExecutionId("execution_001");
        executionTrace.setSdkSessionId("sdk_001");
        executionTrace.setClientTraceId("client_trace_001");
        executionTrace.setTaskStepId("step_001");
        executionTrace.setTeachingPointId("tp_001");
        executionTrace.setResourceId("res_001");
        executionTrace.setTraceType("CLICK");
        executionTrace.setTraceTime(LocalDateTime.now());
        executionTrace.setSequenceNo(1L);
        executionTrace.setInputDataJson("{\"masked\":true}");
        executionTrace.setEvidenceJson("{\"resource\":\"res_001\"}");
        executionTrace.setSuccess(Boolean.TRUE);
        return executionTrace;
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
}
