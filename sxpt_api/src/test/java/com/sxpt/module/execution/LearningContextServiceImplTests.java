package com.sxpt.module.execution;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.execution.dto.LearningContextQueryRequest;
import com.sxpt.module.execution.dto.RuntimeContextQueryRequest;
import com.sxpt.module.execution.service.LearningContextService;
import com.sxpt.module.execution.service.RuntimeContextService;
import com.sxpt.module.execution.service.impl.LearningContextServiceImpl;
import com.sxpt.module.execution.vo.LearningContextVO;
import com.sxpt.module.execution.vo.RuntimeContextVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 学习模式上下文服务测试。
 *
 * 业务功能：
 * 1. 验证学习模式只接受 sdkMode=LEARNING 的 runtime-context。
 * 2. 验证学习模式返回步骤、遮罩、教学点和资源快照。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 RuntimeContextService，聚焦学习模式规则。
 * 2. 直接调用 LearningContextService，覆盖成功和非学习模式拒绝分支。
 */
class LearningContextServiceImplTests {

    private final RuntimeContextService runtimeContextService = mock(RuntimeContextService.class);

    private final LearningContextService service = new LearningContextServiceImpl(runtimeContextService);

    /**
     * 校验学习模式上下文成功返回。
     */
    @Test
    void getLearningContextShouldReturnLearningSnapshot() {
        LearningContextQueryRequest request = buildRequest();
        when(runtimeContextService.getRuntimeContext(any(RuntimeContextQueryRequest.class)))
                .thenReturn(buildRuntimeContext("LEARNING"));

        LearningContextVO result = service.getLearningContext(request);

        assertEquals("context_001", result.getContextId());
        assertEquals("LEARNING", result.getSdkMode());
        assertEquals("{\"steps\":[{\"name\":\"学习步骤\"}]}", result.getLearningContextJson());
        assertEquals("[{\"id\":\"resource_001\"}]", result.getResourceSnapshotJson());

        ArgumentCaptor<RuntimeContextQueryRequest> captor = ArgumentCaptor.forClass(RuntimeContextQueryRequest.class);
        verify(runtimeContextService).getRuntimeContext(captor.capture());
        assertEquals("tenant_001", captor.getValue().getTenantId());
        assertEquals("execution_001", captor.getValue().getExecutionId());
    }

    /**
     * 校验非学习模式上下文被拒绝。
     */
    @Test
    void getLearningContextShouldRejectPracticeMode() {
        LearningContextQueryRequest request = buildRequest();
        when(runtimeContextService.getRuntimeContext(any(RuntimeContextQueryRequest.class)))
                .thenReturn(buildRuntimeContext("PRACTICE"));

        assertThrows(BusinessException.class, () -> service.getLearningContext(request));
    }

    /**
     * 校验缺少执行 ID 时拒绝查询。
     */
    @Test
    void getLearningContextShouldRejectMissingExecutionId() {
        LearningContextQueryRequest request = buildRequest();
        request.setExecutionId(" ");

        assertThrows(BusinessException.class, () -> service.getLearningContext(request));
    }

    /**
     * 构造学习模式查询请求。
     *
     * @return 学习模式查询请求。
     */
    private LearningContextQueryRequest buildRequest() {
        LearningContextQueryRequest request = new LearningContextQueryRequest();
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
        vo.setContextJson("{\"steps\":[{\"name\":\"学习步骤\"}]}");
        vo.setOverlayPolicyJson("{\"mask\":true}");
        vo.setTeachingPointSnapshotJson("[{\"id\":\"tp_001\"}]");
        vo.setResourceSnapshotJson("[{\"id\":\"resource_001\"}]");
        return vo;
    }
}
