package com.sxpt.module.execution.service.impl;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.execution.dto.ExamContextQueryRequest;
import com.sxpt.module.execution.dto.RuntimeContextQueryRequest;
import com.sxpt.module.execution.service.ExamContextService;
import com.sxpt.module.execution.service.RuntimeContextService;
import com.sxpt.module.execution.vo.ExamContextVO;
import com.sxpt.module.execution.vo.RuntimeContextVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 考试模式上下文服务实现。
 *
 * 业务功能：
 * 1. 基于已固化的 runtime-context 返回考试模式专用上下文。
 * 2. 阻止 LEARNING、PRACTICE 等非考试模式上下文进入考试流程。
 *
 * 关键流程：
 * 1. 校验租户 ID 和执行 ID，保证请求可以定位一次任务执行。
 * 2. 调用 RuntimeContextService 读取通用运行上下文。
 * 3. 校验 sdkMode=EXAM 后转换为 ExamContextVO。
 */
@Service
@Profile("!test")
public class ExamContextServiceImpl implements ExamContextService {

    private static final String EXAM_MODE = "EXAM";

    private final RuntimeContextService runtimeContextService;

    public ExamContextServiceImpl(RuntimeContextService runtimeContextService) {
        this.runtimeContextService = runtimeContextService;
    }

    /**
     * 获取考试模式上下文。
     *
     * @param request 考试模式上下文查询请求。
     * @return 考试模式上下文。
     */
    @Override
    public ExamContextVO getExamContext(ExamContextQueryRequest request) {
        validateRequest(request);
        RuntimeContextVO runtimeContext = runtimeContextService.getRuntimeContext(toRuntimeRequest(request));
        if (!EXAM_MODE.equals(runtimeContext.getSdkMode())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        return toExamContext(runtimeContext);
    }

    /**
     * 校验考试模式上下文查询请求。
     *
     * @param request 考试模式上下文查询请求。
     */
    private void validateRequest(ExamContextQueryRequest request) {
        if (request == null || !StringUtils.hasText(request.getTenantId())
                || !StringUtils.hasText(request.getExecutionId())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 将考试模式请求转换为通用 runtime-context 查询请求。
     *
     * @param request 考试模式上下文查询请求。
     * @return 通用 runtime-context 查询请求。
     */
    private RuntimeContextQueryRequest toRuntimeRequest(ExamContextQueryRequest request) {
        RuntimeContextQueryRequest runtimeRequest = new RuntimeContextQueryRequest();
        runtimeRequest.setTenantId(request.getTenantId());
        runtimeRequest.setExecutionId(request.getExecutionId());
        runtimeRequest.setTaskId(request.getTaskId());
        runtimeRequest.setStudentId(request.getStudentId());
        return runtimeRequest;
    }

    /**
     * 将通用 runtime-context 转换为考试模式上下文。
     *
     * @param runtimeContext 通用 runtime-context。
     * @return 考试模式上下文。
     */
    private ExamContextVO toExamContext(RuntimeContextVO runtimeContext) {
        ExamContextVO vo = new ExamContextVO();
        vo.setContextId(runtimeContext.getContextId());
        vo.setTenantId(runtimeContext.getTenantId());
        vo.setExecutionId(runtimeContext.getExecutionId());
        vo.setTaskId(runtimeContext.getTaskId());
        vo.setStudentId(runtimeContext.getStudentId());
        vo.setSdkMode(runtimeContext.getSdkMode());
        vo.setExamContextJson(runtimeContext.getContextJson());
        vo.setResourceSnapshotJson(runtimeContext.getResourceSnapshotJson());
        vo.setEvaluationSnapshotJson(runtimeContext.getEvaluationSnapshotJson());
        vo.setSilentCapturePolicyJson(runtimeContext.getOverlayPolicyJson());
        vo.setHintDisabled(Boolean.TRUE);
        return vo;
    }
}

