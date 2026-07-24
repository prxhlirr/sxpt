package com.sxpt.module.execution.service.impl;

import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.execution.dto.RuntimeContextQueryRequest;
import com.sxpt.module.execution.entity.TaskExecution;
import com.sxpt.module.execution.entity.TaskExecutionContext;
import com.sxpt.module.execution.service.RuntimeContextService;
import com.sxpt.module.execution.service.TaskExecutionService;
import com.sxpt.module.execution.service.TaskExecutionContextService;
import com.sxpt.module.execution.vo.RuntimeContextVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * SDK 运行上下文服务实现。
 *
 * 业务功能：
 * 1. 根据 SDK 携带的执行身份读取已固化的任务执行上下文。
 * 2. 将上下文快照转换为 SDK 专用响应，避免 SDK 直接耦合数据库实体。
 *
 * 关键流程：
 * 1. 校验租户 ID 和执行 ID，确保请求可以唯一定位一次任务执行。
 * 2. 复用 TaskExecutionContextService 查询快照，并要求结果唯一。
 * 3. 将上下文 JSON、遮罩、教学点、资源和评分快照返回给 SDK。
 */
@Service
@Profile("!test")
@ConditionalOnProperty(name = "sxpt.execution.runtime-context-service.enabled",
        havingValue = "true", matchIfMissing = true)
public class RuntimeContextServiceImpl implements RuntimeContextService {

    private static final String RUNNING_STATUS = "RUNNING";

    private final TaskExecutionContextService taskExecutionContextService;

    private final TaskExecutionService taskExecutionService;

    public RuntimeContextServiceImpl(TaskExecutionContextService taskExecutionContextService,
                                     TaskExecutionService taskExecutionService) {
        this.taskExecutionContextService = taskExecutionContextService;
        this.taskExecutionService = taskExecutionService;
    }

    /**
     * 获取 SDK 运行上下文。
     *
     * @param request SDK 运行上下文查询请求。
     * @return SDK 运行上下文。
     */
    @Override
    public RuntimeContextVO getRuntimeContext(RuntimeContextQueryRequest request) {
        validateRequest(request);
        TaskExecution execution = taskExecutionService.getExecution(request.getTenantId(), request.getExecutionId());
        validateExecutionReadable(request, execution);
        List<TaskExecutionContext> contexts = taskExecutionContextService.listContexts(
                request.getTenantId(), request.getExecutionId(), request.getTaskId(), request.getStudentId());
        if (contexts.isEmpty()) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        if (contexts.size() > 1) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        return toVO(contexts.get(0));
    }

    /**
     * 校验运行上下文读取权限。
     *
     * @param request SDK 运行上下文查询请求。
     * @param execution 学生任务执行主记录。
     */
    private void validateExecutionReadable(RuntimeContextQueryRequest request, TaskExecution execution) {
        if (execution == null || !RUNNING_STATUS.equals(execution.getExecutionStatus())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        if (StringUtils.hasText(request.getTaskId()) && !request.getTaskId().equals(execution.getTaskId())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        if (StringUtils.hasText(request.getStudentId()) && !request.getStudentId().equals(execution.getStudentId())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
    }

    /**
     * 校验 SDK 运行上下文查询请求。
     *
     * @param request SDK 运行上下文查询请求。
     */
    private void validateRequest(RuntimeContextQueryRequest request) {
        if (request == null || !StringUtils.hasText(request.getTenantId())
                || !StringUtils.hasText(request.getExecutionId())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 将任务执行上下文快照转换为 SDK 运行上下文。
     *
     * @param context 任务执行上下文快照。
     * @return SDK 运行上下文。
     */
    private RuntimeContextVO toVO(TaskExecutionContext context) {
        RuntimeContextVO vo = new RuntimeContextVO();
        vo.setContextId(context.getId());
        vo.setTenantId(context.getTenantId());
        vo.setExecutionId(context.getExecutionId());
        vo.setTaskId(context.getTaskId());
        vo.setStudentId(context.getStudentId());
        vo.setSdkMode(context.getSdkMode());
        vo.setContextJson(context.getContextJson());
        vo.setOverlayPolicyJson(context.getOverlayPolicyJson());
        vo.setTeachingPointSnapshotJson(context.getTeachingPointSnapshotJson());
        vo.setResourceSnapshotJson(context.getResourceSnapshotJson());
        vo.setEvaluationSnapshotJson(context.getEvaluationSnapshotJson());
        vo.setArchiveStatus(context.getArchiveStatus());
        vo.setExpireTime(context.getExpireTime());
        return vo;
    }
}

