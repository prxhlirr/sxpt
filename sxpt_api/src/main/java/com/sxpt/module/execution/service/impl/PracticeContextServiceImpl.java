package com.sxpt.module.execution.service.impl;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.execution.dto.PracticeContextQueryRequest;
import com.sxpt.module.execution.dto.RuntimeContextQueryRequest;
import com.sxpt.module.execution.service.PracticeContextService;
import com.sxpt.module.execution.service.RuntimeContextService;
import com.sxpt.module.execution.vo.PracticeContextVO;
import com.sxpt.module.execution.vo.RuntimeContextVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 练习模式上下文服务实现。
 *
 * 业务功能：
 * 1. 基于已固化的 runtime-context 返回练习模式专用上下文。
 * 2. 阻止 LEARNING、EXAM 等非练习模式上下文进入练习流程。
 *
 * 关键流程：
 * 1. 校验租户 ID 和执行 ID，保证请求可以定位一次任务执行。
 * 2. 调用 RuntimeContextService 读取通用运行上下文。
 * 3. 校验 sdkMode=PRACTICE 后转换为 PracticeContextVO。
 */
@Service
@Profile("!test")
public class PracticeContextServiceImpl implements PracticeContextService {

    private static final String PRACTICE_MODE = "PRACTICE";

    private final RuntimeContextService runtimeContextService;

    public PracticeContextServiceImpl(RuntimeContextService runtimeContextService) {
        this.runtimeContextService = runtimeContextService;
    }

    /**
     * 获取练习模式上下文。
     *
     * @param request 练习模式上下文查询请求。
     * @return 练习模式上下文。
     */
    @Override
    public PracticeContextVO getPracticeContext(PracticeContextQueryRequest request) {
        validateRequest(request);
        RuntimeContextVO runtimeContext = runtimeContextService.getRuntimeContext(toRuntimeRequest(request));
        if (!PRACTICE_MODE.equals(runtimeContext.getSdkMode())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        return toPracticeContext(runtimeContext);
    }

    /**
     * 校验练习模式上下文查询请求。
     *
     * @param request 练习模式上下文查询请求。
     */
    private void validateRequest(PracticeContextQueryRequest request) {
        if (request == null || !StringUtils.hasText(request.getTenantId())
                || !StringUtils.hasText(request.getExecutionId())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 将练习模式请求转换为通用 runtime-context 查询请求。
     *
     * @param request 练习模式上下文查询请求。
     * @return 通用 runtime-context 查询请求。
     */
    private RuntimeContextQueryRequest toRuntimeRequest(PracticeContextQueryRequest request) {
        RuntimeContextQueryRequest runtimeRequest = new RuntimeContextQueryRequest();
        runtimeRequest.setTenantId(request.getTenantId());
        runtimeRequest.setExecutionId(request.getExecutionId());
        runtimeRequest.setTaskId(request.getTaskId());
        runtimeRequest.setStudentId(request.getStudentId());
        return runtimeRequest;
    }

    /**
     * 将通用 runtime-context 转换为练习模式上下文。
     *
     * @param runtimeContext 通用 runtime-context。
     * @return 练习模式上下文。
     */
    private PracticeContextVO toPracticeContext(RuntimeContextVO runtimeContext) {
        PracticeContextVO vo = new PracticeContextVO();
        vo.setContextId(runtimeContext.getContextId());
        vo.setTenantId(runtimeContext.getTenantId());
        vo.setExecutionId(runtimeContext.getExecutionId());
        vo.setTaskId(runtimeContext.getTaskId());
        vo.setStudentId(runtimeContext.getStudentId());
        vo.setSdkMode(runtimeContext.getSdkMode());
        vo.setPracticeContextJson(runtimeContext.getContextJson());
        vo.setOverlayPolicyJson(runtimeContext.getOverlayPolicyJson());
        vo.setTeachingPointSnapshotJson(runtimeContext.getTeachingPointSnapshotJson());
        vo.setResourceSnapshotJson(runtimeContext.getResourceSnapshotJson());
        vo.setHintPolicyJson(runtimeContext.getTeachingPointSnapshotJson());
        vo.setRetryPolicyJson(runtimeContext.getOverlayPolicyJson());
        return vo;
    }
}

