package com.sxpt.module.execution;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.execution.dto.ExamContextQueryRequest;
import com.sxpt.module.execution.dto.RuntimeContextQueryRequest;
import com.sxpt.module.execution.service.ExamContextService;
import com.sxpt.module.execution.service.RuntimeContextService;
import com.sxpt.module.execution.service.impl.ExamContextServiceImpl;
import com.sxpt.module.execution.vo.ExamContextVO;
import com.sxpt.module.execution.vo.RuntimeContextVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 考试模式上下文服务测试。
 *
 * 业务功能：
 * 1. 验证考试模式只接受 sdkMode=EXAM 的 runtime-context。
 * 2. 验证考试模式返回考试上下文、评分快照和静默采集策略。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 RuntimeContextService，聚焦考试模式规则。
 * 2. 直接调用 ExamContextService，覆盖成功和非考试模式拒绝分支。
 */
class ExamContextServiceImplTests {

    private final RuntimeContextService runtimeContextService = mock(RuntimeContextService.class);

    private final ExamContextService service = new ExamContextServiceImpl(runtimeContextService);

    /**
     * 校验考试模式上下文成功返回。
     */
    @Test
    void getExamContextShouldReturnExamSnapshot() {
        ExamContextQueryRequest request = buildRequest();
        when(runtimeContextService.getRuntimeContext(any(RuntimeContextQueryRequest.class)))
                .thenReturn(buildRuntimeContext("EXAM"));

        ExamContextVO result = service.getExamContext(request);

        assertEquals("context_001", result.getContextId());
        assertEquals("EXAM", result.getSdkMode());
        assertEquals("{\"steps\":[{\"name\":\"考试步骤\"}]}", result.getExamContextJson());
        assertEquals("[{\"id\":\"rule_001\"}]", result.getEvaluationSnapshotJson());
        assertTrue(result.getHintDisabled());

        ArgumentCaptor<RuntimeContextQueryRequest> captor = ArgumentCaptor.forClass(RuntimeContextQueryRequest.class);
        verify(runtimeContextService).getRuntimeContext(captor.capture());
        assertEquals("tenant_001", captor.getValue().getTenantId());
        assertEquals("execution_001", captor.getValue().getExecutionId());
    }

    /**
     * 校验非考试模式上下文被拒绝。
     */
    @Test
    void getExamContextShouldRejectPracticeMode() {
        ExamContextQueryRequest request = buildRequest();
        when(runtimeContextService.getRuntimeContext(any(RuntimeContextQueryRequest.class)))
                .thenReturn(buildRuntimeContext("PRACTICE"));

        assertThrows(BusinessException.class, () -> service.getExamContext(request));
    }

    /**
     * 校验缺少执行 ID 时拒绝查询。
     */
    @Test
    void getExamContextShouldRejectMissingExecutionId() {
        ExamContextQueryRequest request = buildRequest();
        request.setExecutionId(" ");

        assertThrows(BusinessException.class, () -> service.getExamContext(request));
    }

    /**
     * 构造考试模式查询请求。
     *
     * @return 考试模式查询请求。
     */
    private ExamContextQueryRequest buildRequest() {
        ExamContextQueryRequest request = new ExamContextQueryRequest();
        request.setTenantId("tenant_001");
        request.setExecutionId("execution_001");
        request.setTaskId("task_001");
        request.setStudentId("student_001");
        return request;
    }

    /**
     * 构造通用 runtime-context。
     *
     * @param sdkMode SDK 模式。
     * @return 通用 runtime-context。
     */
    private RuntimeContextVO buildRuntimeContext(String sdkMode) {
        RuntimeContextVO vo = new RuntimeContextVO();
        vo.setContextId("context_001");
        vo.setTenantId("tenant_001");
        vo.setExecutionId("execution_001");
        vo.setTaskId("task_001");
        vo.setStudentId("student_001");
        vo.setSdkMode(sdkMode);
        vo.setContextJson("{\"steps\":[{\"name\":\"考试步骤\"}]}");
        vo.setOverlayPolicyJson("{\"silentCapture\":true}");
        vo.setResourceSnapshotJson("[{\"id\":\"resource_001\"}]");
        vo.setEvaluationSnapshotJson("[{\"id\":\"rule_001\"}]");
        return vo;
    }
}
