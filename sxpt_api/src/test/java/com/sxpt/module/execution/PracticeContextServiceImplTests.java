package com.sxpt.module.execution;

import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.execution.dto.PracticeContextQueryRequest;
import com.sxpt.module.execution.dto.RuntimeContextQueryRequest;
import com.sxpt.module.execution.service.PracticeContextService;
import com.sxpt.module.execution.service.RuntimeContextService;
import com.sxpt.module.execution.service.impl.PracticeContextServiceImpl;
import com.sxpt.module.execution.vo.PracticeContextVO;
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
 * 练习模式上下文服务测试。
 *
 * 业务功能：
 * 1. 验证练习模式只接受 sdkMode=PRACTICE 的 runtime-context。
 * 2. 验证练习模式返回弱提示、资源定位和重开策略快照。
 *
 * 关键流程：
 * 1. 使用 Mockito 替代 RuntimeContextService，聚焦练习模式规则。
 * 2. 直接调用 PracticeContextService，覆盖成功和非练习模式拒绝分支。
 */
class PracticeContextServiceImplTests {

    private final RuntimeContextService runtimeContextService = mock(RuntimeContextService.class);

    private final PracticeContextService service = new PracticeContextServiceImpl(runtimeContextService);

    /**
     * 校验练习模式上下文成功返回。
     */
    @Test
    void getPracticeContextShouldReturnPracticeSnapshot() {
        PracticeContextQueryRequest request = buildRequest();
        when(runtimeContextService.getRuntimeContext(any(RuntimeContextQueryRequest.class)))
                .thenReturn(buildRuntimeContext("PRACTICE"));

        PracticeContextVO result = service.getPracticeContext(request);

        assertEquals("context_001", result.getContextId());
        assertEquals("PRACTICE", result.getSdkMode());
        assertEquals("{\"steps\":[{\"name\":\"练习步骤\"}]}", result.getPracticeContextJson());
        assertEquals("[{\"id\":\"resource_001\"}]", result.getResourceSnapshotJson());

        ArgumentCaptor<RuntimeContextQueryRequest> captor = ArgumentCaptor.forClass(RuntimeContextQueryRequest.class);
        verify(runtimeContextService).getRuntimeContext(captor.capture());
        assertEquals("tenant_001", captor.getValue().getTenantId());
        assertEquals("execution_001", captor.getValue().getExecutionId());
    }

    /**
     * 校验非练习模式上下文被拒绝。
     */
    @Test
    void getPracticeContextShouldRejectLearningMode() {
        PracticeContextQueryRequest request = buildRequest();
        when(runtimeContextService.getRuntimeContext(any(RuntimeContextQueryRequest.class)))
                .thenReturn(buildRuntimeContext("LEARNING"));

        assertThrows(BusinessException.class, () -> service.getPracticeContext(request));
    }

    /**
     * 校验缺少执行 ID 时拒绝查询。
     */
    @Test
    void getPracticeContextShouldRejectMissingExecutionId() {
        PracticeContextQueryRequest request = buildRequest();
        request.setExecutionId(" ");

        assertThrows(BusinessException.class, () -> service.getPracticeContext(request));
    }

    /**
     * 构造练习模式查询请求。
     *
     * @return 练习模式查询请求。
     */
    private PracticeContextQueryRequest buildRequest() {
        PracticeContextQueryRequest request = new PracticeContextQueryRequest();
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
        vo.setContextJson("{\"steps\":[{\"name\":\"练习步骤\"}]}");
        vo.setOverlayPolicyJson("{\"retry\":true}");
        vo.setTeachingPointSnapshotJson("[{\"id\":\"tp_001\"}]");
        vo.setResourceSnapshotJson("[{\"id\":\"resource_001\"}]");
        return vo;
    }
}
