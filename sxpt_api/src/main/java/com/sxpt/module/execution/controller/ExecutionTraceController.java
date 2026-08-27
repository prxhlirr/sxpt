package com.sxpt.module.execution.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.execution.dto.ReportExecutionTraceRequest;
import com.sxpt.module.execution.entity.ExecutionTrace;
import com.sxpt.module.execution.service.ExecutionTraceService;
import com.sxpt.module.execution.service.TaskExecutionService;
import com.sxpt.module.execution.vo.ExecutionTraceVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 学生执行轨迹接口。
 *
 * 业务功能：
 * 1. 提供 SDK 单条执行轨迹上报入口，支持点击、输入、选择、上传、状态和提示请求。
 * 2. 提供按任务执行查询轨迹入口，为练习过程分和自动评分提供事实列表。
 *
 * 关键流程：
 * 1. 接收上报请求并触发 Bean Validation，优先拦截缺少 clientTraceId 的非幂等轨迹。
 * 2. 将 DTO 转换为 ExecutionTrace 实体并生成应用层主键。
 * 3. 调用 Service 完成幂等写入，再转换为 VO 返回给 SDK。
 */
@RestController
@RequestMapping("/api/v1/execution/traces")
@ConditionalOnProperty(name = "sxpt.execution.trace-controller.enabled",
        havingValue = "true", matchIfMissing = true)
public class ExecutionTraceController {

    private final ExecutionTraceService executionTraceService;

    private final TaskExecutionService taskExecutionService;

    public ExecutionTraceController(ExecutionTraceService executionTraceService,
                                    TaskExecutionService taskExecutionService) {
        this.executionTraceService = executionTraceService;
        this.taskExecutionService = taskExecutionService;
    }

    /**
     * 上报单条 SDK 执行轨迹。
     *
     * @param request SDK 执行轨迹上报请求。
     * @return 已保存或幂等命中的 SDK 执行轨迹。
     */
    @PostMapping("/report")
    public ApiResult<ExecutionTraceVO> report(@Valid @RequestBody ReportExecutionTraceRequest request) {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        com.sxpt.module.execution.entity.TaskExecution execution = taskExecutionService.getExecution(
                user.getTenantId(), request.getExecutionId());
        if (!user.hasAnyRole("ADMIN", "TEACHER") && !user.getUserId().equals(execution.getStudentId())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        ExecutionTrace saved = executionTraceService.reportExecutionTrace(toEntity(request, user));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询指定任务执行下的 SDK 执行轨迹。
     *
     * @param tenantId 租户 ID。
     * @param executionId 任务执行 ID。
     * @return SDK 执行轨迹列表。
     */
    @GetMapping
    public ApiResult<List<ExecutionTraceVO>> listByExecution(@RequestParam String tenantId,
                                                             @RequestParam String executionId) {
        CurrentUserContext.CurrentUser user = CurrentUserContext.getRequiredUser();
        com.sxpt.module.execution.entity.TaskExecution execution = taskExecutionService.getExecution(
                user.getTenantId(), executionId);
        if (!user.hasAnyRole("ADMIN", "TEACHER") && !user.getUserId().equals(execution.getStudentId())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        return ApiResult.success(toVOList(executionTraceService.listByExecution(user.getTenantId(), executionId)));
    }

    /**
     * 将上报请求转换为数据库实体。
     *
     * @param request SDK 执行轨迹上报请求。
     * @return SDK 执行轨迹实体。
     */
    private ExecutionTrace toEntity(ReportExecutionTraceRequest request, CurrentUserContext.CurrentUser user) {
        ExecutionTrace executionTrace = new ExecutionTrace();
        executionTrace.setId(generateId());
        executionTrace.setTenantId(user.getTenantId());
        executionTrace.setExecutionId(request.getExecutionId());
        executionTrace.setSdkSessionId(request.getSdkSessionId());
        executionTrace.setClientTraceId(request.getClientTraceId());
        executionTrace.setTaskStepId(request.getTaskStepId());
        executionTrace.setTeachingPointId(request.getTeachingPointId());
        executionTrace.setResourceId(request.getResourceId());
        executionTrace.setDataSessionId(request.getDataSessionId());
        executionTrace.setDataInstanceId(request.getDataInstanceId());
        executionTrace.setBusinessSceneCode(request.getBusinessSceneCode());
        executionTrace.setExternalBusinessId(request.getExternalBusinessId());
        executionTrace.setTraceType(request.getTraceType());
        executionTrace.setTraceTime(request.getTraceTime());
        executionTrace.setSequenceNo(request.getSequenceNo());
        executionTrace.setRetryCount(request.getRetryCount());
        executionTrace.setInputDataJson(request.getInputDataJson());
        executionTrace.setOutputDataJson(request.getOutputDataJson());
        executionTrace.setBeforeStateJson(request.getBeforeStateJson());
        executionTrace.setAfterStateJson(request.getAfterStateJson());
        executionTrace.setEvidenceJson(request.getEvidenceJson());
        executionTrace.setSuccess(request.getSuccess());
        executionTrace.setErrorMessage(request.getErrorMessage());
        return executionTrace;
    }

    /**
     * 将实体列表转换为前端展示对象列表。
     *
     * @param traces SDK 执行轨迹实体列表。
     * @return SDK 执行轨迹展示对象列表。
     */
    private List<ExecutionTraceVO> toVOList(List<ExecutionTrace> traces) {
        List<ExecutionTraceVO> result = new ArrayList<>();
        for (ExecutionTrace trace : traces) {
            result.add(toVO(trace));
        }
        return result;
    }

    /**
     * 将实体转换为前端展示对象。
     *
     * @param trace SDK 执行轨迹实体。
     * @return SDK 执行轨迹展示对象。
     */
    private ExecutionTraceVO toVO(ExecutionTrace trace) {
        ExecutionTraceVO vo = new ExecutionTraceVO();
        vo.setId(trace.getId());
        vo.setTenantId(trace.getTenantId());
        vo.setExecutionId(trace.getExecutionId());
        vo.setSdkSessionId(trace.getSdkSessionId());
        vo.setClientTraceId(trace.getClientTraceId());
        vo.setTaskStepId(trace.getTaskStepId());
        vo.setTeachingPointId(trace.getTeachingPointId());
        vo.setResourceId(trace.getResourceId());
        vo.setDataSessionId(trace.getDataSessionId());
        vo.setDataInstanceId(trace.getDataInstanceId());
        vo.setBusinessSceneCode(trace.getBusinessSceneCode());
        vo.setExternalBusinessId(trace.getExternalBusinessId());
        vo.setTraceType(trace.getTraceType());
        vo.setTraceTime(trace.getTraceTime());
        vo.setSequenceNo(trace.getSequenceNo());
        vo.setRetryCount(trace.getRetryCount());
        vo.setSuccess(trace.getSuccess());
        vo.setErrorMessage(trace.getErrorMessage());
        vo.setArchiveStatus(trace.getArchiveStatus());
        vo.setStatus(trace.getStatus());
        vo.setCreateTime(trace.getCreateTime());
        return vo;
    }

    /**
     * 生成应用层主键。
     *
     * @return 32 位无横线字符串 ID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
