package com.sxpt.module.execution.service.impl;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.execution.dto.LearningContextQueryRequest;
import com.sxpt.module.execution.dto.RuntimeContextQueryRequest;
import com.sxpt.module.execution.service.LearningContextService;
import com.sxpt.module.execution.service.RuntimeContextService;
import com.sxpt.module.execution.vo.LearningContextVO;
import com.sxpt.module.execution.vo.RuntimeContextVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 学习模式上下文服务实现。
 *
 * 业务功能：
 * 1. 基于已固化的 runtime-context 返回学习模式专用上下文。
 * 2. 阻止 PRACTICE、EXAM 等非学习模式上下文进入学习引导流程。
 *
 * 关键流程：
 * 1. 校验租户 ID 和执行 ID，保证请求可以定位一次任务执行。
 * 2. 调用 RuntimeContextService 读取通用运行上下文。
 * 3. 校验 sdkMode=LEARNING 后转换为 LearningContextVO。
 */
@Service
@Profile("!test")
public class LearningContextServiceImpl implements LearningContextService {

    private static final String LEARNING_MODE = "LEARNING";

    private final RuntimeContextService runtimeContextService;

    public LearningContextServiceImpl(RuntimeContextService runtimeContextService) {
        this.runtimeContextService = runtimeContextService;
    }

    /**
     * 获取学习模式上下文。
     *
     * @param request 学习模式上下文查询请求。
     * @return 学习模式上下文。
     */
    @Override
    public LearningContextVO getLearningContext(LearningContextQueryRequest request) {
        validateRequest(request);
        RuntimeContextVO runtimeContext = runtimeContextService.getRuntimeContext(toRuntimeRequest(request));
        if (!LEARNING_MODE.equals(runtimeContext.getSdkMode())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        return toLearningContext(runtimeContext);
    }

    /**
     * 校验学习模式上下文查询请求。
     *
     * @param request 学习模式上下文查询请求。
     */
    private void validateRequest(LearningContextQueryRequest request) {
        if (request == null || !StringUtils.hasText(request.getTenantId())
                || !StringUtils.hasText(request.getExecutionId())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 将学习模式请求转换为通用 runtime-context 查询请求。
     *
     * @param request 学习模式上下文查询请求。
     * @return 通用 runtime-context 查询请求。
     */
    private RuntimeContextQueryRequest toRuntimeRequest(LearningContextQueryRequest request) {
        RuntimeContextQueryRequest runtimeRequest = new RuntimeContextQueryRequest();
        runtimeRequest.setTenantId(request.getTenantId());
        runtimeRequest.setExecutionId(request.getExecutionId());
        runtimeRequest.setTaskId(request.getTaskId());
        runtimeRequest.setStudentId(request.getStudentId());
        return runtimeRequest;
    }

    /**
     * 将通用 runtime-context 转换为学习模式上下文。
     *
     * @param runtimeContext 通用 runtime-context。
     * @return 学习模式上下文。
     */
    private LearningContextVO toLearningContext(RuntimeContextVO runtimeContext) {
        LearningContextVO vo = new LearningContextVO();
        vo.setContextId(runtimeContext.getContextId());
        vo.setTenantId(runtimeContext.getTenantId());
        vo.setExecutionId(runtimeContext.getExecutionId());
        vo.setTaskId(runtimeContext.getTaskId());
        vo.setStudentId(runtimeContext.getStudentId());
        vo.setSdkMode(runtimeContext.getSdkMode());
        vo.setLearningContextJson(runtimeContext.getContextJson());
        vo.setOverlayPolicyJson(runtimeContext.getOverlayPolicyJson());
        vo.setTeachingPointSnapshotJson(runtimeContext.getTeachingPointSnapshotJson());
        vo.setResourceSnapshotJson(runtimeContext.getResourceSnapshotJson());
        vo.setGuideSnapshotJson(runtimeContext.getTeachingPointSnapshotJson());
        return vo;
    }
}

