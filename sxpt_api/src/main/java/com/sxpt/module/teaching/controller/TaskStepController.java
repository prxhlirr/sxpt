package com.sxpt.module.teaching.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.teaching.dto.CreateTaskStepRequest;
import com.sxpt.module.teaching.entity.TaskStep;
import com.sxpt.module.teaching.service.TaskStepService;
import com.sxpt.module.teaching.vo.TaskStepVO;
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
 * 教学任务步骤接口。
 *
 * 业务功能：
 * 1. 提供教学步骤发布入口，保存步骤顺序、角色路径、资源引用和提示内容。
 * 2. 提供按任务和教学点查询步骤入口，为 SDK runtime 和步骤配置页面提供步骤清单。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation，提前拦截缺少步骤编码、名称或排序号的无效步骤。
 * 2. 将 DTO 转换为 TaskStep 实体并生成应用层主键。
 * 3. 调用 Service 写入教学步骤，再转换为 VO 返回前端。
 */
@RestController
@RequestMapping("/api/v1/teaching/task-steps")
@ConditionalOnProperty(name = "sxpt.teaching.task-step-controller.enabled", havingValue = "true", matchIfMissing = true)
public class TaskStepController {

    private final TaskStepService taskStepService;

    public TaskStepController(TaskStepService taskStepService) {
        this.taskStepService = taskStepService;
    }

    /**
     * 创建教学任务步骤。
     *
     * @param request 创建教学任务步骤请求。
     * @return 已保存的教学任务步骤。
     */
    @PostMapping("/create")
    public ApiResult<TaskStepVO> create(@Valid @RequestBody CreateTaskStepRequest request) {
        TaskStep saved = taskStepService.createTaskStep(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 按任务和教学点查询教学步骤。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @return 教学任务步骤列表。
     */
    @GetMapping
    public ApiResult<List<TaskStepVO>> listByTaskAndTeachingPoint(@RequestParam String tenantId,
                                                                  @RequestParam String taskId,
                                                                  @RequestParam String teachingPointId) {
        return ApiResult.success(toVOList(taskStepService.listByTaskAndTeachingPoint(
                tenantId, taskId, teachingPointId)));
    }

    /**
     * 将创建请求转换为数据库实体。
     *
     * @param request 创建教学任务步骤请求。
     * @return 教学任务步骤实体。
     */
    private TaskStep toEntity(CreateTaskStepRequest request) {
        TaskStep taskStep = new TaskStep();
        taskStep.setId(generateId());
        taskStep.setTenantId(request.getTenantId());
        taskStep.setTaskId(request.getTaskId());
        taskStep.setTeachingPointId(request.getTeachingPointId());
        taskStep.setStepCode(request.getStepCode());
        taskStep.setStepName(request.getStepName());
        taskStep.setStepDescription(request.getStepDescription());
        taskStep.setSequenceNo(request.getSequenceNo());
        taskStep.setSegmentNo(request.getSegmentNo());
        taskStep.setActorType(request.getActorType());
        taskStep.setRequiredExternalOrgId(request.getRequiredExternalOrgId());
        taskStep.setRequiredExternalOrgName(request.getRequiredExternalOrgName());
        taskStep.setRequiredExternalRoleId(request.getRequiredExternalRoleId());
        taskStep.setRequiredExternalRoleName(request.getRequiredExternalRoleName());
        taskStep.setSwitchStrategy(request.getSwitchStrategy());
        taskStep.setNextSegmentNo(request.getNextSegmentNo());
        taskStep.setSwitchConfirmRequired(request.getSwitchConfirmRequired());
        taskStep.setSwitchDecisionSource(request.getSwitchDecisionSource());
        taskStep.setSwitchReason(request.getSwitchReason());
        taskStep.setRollbackPolicy(request.getRollbackPolicy());
        taskStep.setRelatedResourceIds(request.getRelatedResourceIds());
        taskStep.setGuideContent(request.getGuideContent());
        taskStep.setPracticeHint(request.getPracticeHint());
        taskStep.setRequired(request.getRequired());
        taskStep.setAllowSkip(request.getAllowSkip());
        taskStep.setSourceActionDraftId(request.getSourceActionDraftId());
        taskStep.setCreateBy(request.getCreateBy());
        taskStep.setUpdateBy(request.getCreateBy());
        return taskStep;
    }

    /**
     * 将实体列表转换为前端展示对象列表。
     *
     * @param taskSteps 教学任务步骤实体列表。
     * @return 教学任务步骤展示对象列表。
     */
    private List<TaskStepVO> toVOList(List<TaskStep> taskSteps) {
        List<TaskStepVO> result = new ArrayList<>();
        for (TaskStep taskStep : taskSteps) {
            result.add(toVO(taskStep));
        }
        return result;
    }

    /**
     * 将实体转换为前端展示对象。
     *
     * @param taskStep 教学任务步骤实体。
     * @return 教学任务步骤展示对象。
     */
    private TaskStepVO toVO(TaskStep taskStep) {
        TaskStepVO vo = new TaskStepVO();
        vo.setId(taskStep.getId());
        vo.setTenantId(taskStep.getTenantId());
        vo.setTaskId(taskStep.getTaskId());
        vo.setTeachingPointId(taskStep.getTeachingPointId());
        vo.setStepCode(taskStep.getStepCode());
        vo.setStepName(taskStep.getStepName());
        vo.setStepDescription(taskStep.getStepDescription());
        vo.setSequenceNo(taskStep.getSequenceNo());
        vo.setSegmentNo(taskStep.getSegmentNo());
        vo.setActorType(taskStep.getActorType());
        vo.setRequiredExternalOrgId(taskStep.getRequiredExternalOrgId());
        vo.setRequiredExternalOrgName(taskStep.getRequiredExternalOrgName());
        vo.setRequiredExternalRoleId(taskStep.getRequiredExternalRoleId());
        vo.setRequiredExternalRoleName(taskStep.getRequiredExternalRoleName());
        vo.setSwitchStrategy(taskStep.getSwitchStrategy());
        vo.setNextSegmentNo(taskStep.getNextSegmentNo());
        vo.setSwitchConfirmRequired(taskStep.getSwitchConfirmRequired());
        vo.setSwitchDecisionSource(taskStep.getSwitchDecisionSource());
        vo.setSwitchReason(taskStep.getSwitchReason());
        vo.setRollbackPolicy(taskStep.getRollbackPolicy());
        vo.setRelatedResourceIds(taskStep.getRelatedResourceIds());
        vo.setGuideContent(taskStep.getGuideContent());
        vo.setPracticeHint(taskStep.getPracticeHint());
        vo.setRequired(taskStep.getRequired());
        vo.setAllowSkip(taskStep.getAllowSkip());
        vo.setSourceActionDraftId(taskStep.getSourceActionDraftId());
        vo.setStatus(taskStep.getStatus());
        vo.setCreateTime(taskStep.getCreateTime());
        vo.setUpdateTime(taskStep.getUpdateTime());
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
