package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.BusinessModuleProcessChainRequest.ActorRequest;
import com.sxpt.module.connector.dto.BusinessModuleProcessChainRequest.StepRequest;
import com.sxpt.module.connector.entity.BusinessModuleProcessActor;
import com.sxpt.module.connector.entity.BusinessModuleProcessStep;
import com.sxpt.module.connector.service.BusinessModuleProcessChainService;
import com.sxpt.module.connector.vo.BusinessModuleProcessChainVO.ActorVO;
import com.sxpt.module.connector.vo.BusinessModuleProcessChainVO.StepVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
 * 业务模块标准办理链管理接口。
 *
 * 业务功能：
 * 1. 为后台管理提供标准办理步骤和步骤参与方的创建、编辑、启停、详情和列表查询入口。
 * 2. 支撑业务模块维护“一步骤多单位多角色”的原平台协作结构。
 * 3. 为后续数据准备链路快照生成和学生进入原平台身份分配提供稳定维护入口。
 *
 * 关键流程：
 * 1. 管理员先在业务模块下维护标准办理步骤。
 * 2. 管理员再为步骤维护主办、协办、审核、复核、会签等参与方。
 * 3. Service 统一校验步骤唯一性、参与方唯一性和所属步骤边界。
 */
@RestController
@RequestMapping("/api/v1/connector")
@ConditionalOnProperty(name = "sxpt.connector.business-module-process-chain-controller.enabled", havingValue = "true", matchIfMissing = true)
public class BusinessModuleProcessChainController {

    private final BusinessModuleProcessChainService processChainService;

    public BusinessModuleProcessChainController(BusinessModuleProcessChainService processChainService) {
        this.processChainService = processChainService;
    }

    /**
     * 创建业务模块标准办理步骤。
     *
     * @param businessModuleId 业务模块 ID。
     * @param request 标准办理步骤创建请求。
     * @return 已创建的标准办理步骤。
     */
    @PostMapping("/business-modules/{businessModuleId}/process-steps/create")
    public ApiResult<StepVO> createStep(@PathVariable String businessModuleId,
                                        @Valid @RequestBody StepRequest request) {
        BusinessModuleProcessStep saved = processChainService.createProcessStep(toCreateStep(businessModuleId, request));
        return ApiResult.success(toStepVO(saved));
    }

    /**
     * 更新业务模块标准办理步骤。
     *
     * @param stepId 标准办理步骤 ID。
     * @param request 标准办理步骤更新请求。
     * @return 已更新的标准办理步骤。
     */
    @PostMapping("/business-module-process-steps/{stepId}/update")
    public ApiResult<StepVO> updateStep(@PathVariable String stepId,
                                        @Valid @RequestBody StepRequest request) {
        BusinessModuleProcessStep saved = processChainService.updateProcessStep(toUpdateStep(stepId, request));
        return ApiResult.success(toStepVO(saved));
    }

    /**
     * 启用业务模块标准办理步骤。
     *
     * @param stepId 标准办理步骤 ID。
     * @return 已启用的标准办理步骤。
     */
    @PostMapping("/business-module-process-steps/{stepId}/enable")
    public ApiResult<StepVO> enableStep(@PathVariable String stepId) {
        return ApiResult.success(toStepVO(processChainService.enableProcessStep(stepId)));
    }

    /**
     * 停用业务模块标准办理步骤。
     *
     * @param stepId 标准办理步骤 ID。
     * @return 已停用的标准办理步骤。
     */
    @PostMapping("/business-module-process-steps/{stepId}/disable")
    public ApiResult<StepVO> disableStep(@PathVariable String stepId) {
        return ApiResult.success(toStepVO(processChainService.disableProcessStep(stepId)));
    }

    /**
     * 查询业务模块标准办理步骤详情。
     *
     * @param stepId 标准办理步骤 ID。
     * @return 标准办理步骤详情。
     */
    @GetMapping("/business-module-process-steps/{stepId}")
    public ApiResult<StepVO> stepDetail(@PathVariable String stepId) {
        return ApiResult.success(toStepVO(processChainService.getProcessStepById(stepId)));
    }

    /**
     * 查询业务模块下全部标准办理步骤。
     *
     * @param tenantId 租户 ID。
     * @param businessModuleId 业务模块 ID。
     * @return 标准办理步骤列表。
     */
    @GetMapping("/business-modules/{businessModuleId}/process-steps")
    public ApiResult<List<StepVO>> listSteps(@RequestParam String tenantId,
                                             @PathVariable String businessModuleId) {
        return ApiResult.success(toStepVOList(processChainService.listProcessSteps(tenantId, businessModuleId)));
    }

    /**
     * 查询业务模块下启用的标准办理步骤。
     *
     * @param tenantId 租户 ID。
     * @param businessModuleId 业务模块 ID。
     * @return 启用标准办理步骤列表。
     */
    @GetMapping("/business-modules/{businessModuleId}/process-steps/active")
    public ApiResult<List<StepVO>> listActiveSteps(@RequestParam String tenantId,
                                                   @PathVariable String businessModuleId) {
        return ApiResult.success(toStepVOList(processChainService.listActiveProcessSteps(tenantId, businessModuleId)));
    }

    /**
     * 创建标准办理步骤参与方。
     *
     * @param stepId 标准办理步骤 ID。
     * @param request 步骤参与方创建请求。
     * @return 已创建的步骤参与方。
     */
    @PostMapping("/business-module-process-steps/{stepId}/actors/create")
    public ApiResult<ActorVO> createActor(@PathVariable String stepId,
                                          @Valid @RequestBody ActorRequest request) {
        BusinessModuleProcessActor saved = processChainService.createProcessActor(toCreateActor(stepId, request));
        return ApiResult.success(toActorVO(saved));
    }

    /**
     * 更新标准办理步骤参与方。
     *
     * @param actorId 步骤参与方 ID。
     * @param request 步骤参与方更新请求。
     * @return 已更新的步骤参与方。
     */
    @PostMapping("/business-module-process-actors/{actorId}/update")
    public ApiResult<ActorVO> updateActor(@PathVariable String actorId,
                                          @Valid @RequestBody ActorRequest request) {
        BusinessModuleProcessActor saved = processChainService.updateProcessActor(toUpdateActor(actorId, request));
        return ApiResult.success(toActorVO(saved));
    }

    /**
     * 启用标准办理步骤参与方。
     *
     * @param actorId 步骤参与方 ID。
     * @return 已启用的步骤参与方。
     */
    @PostMapping("/business-module-process-actors/{actorId}/enable")
    public ApiResult<ActorVO> enableActor(@PathVariable String actorId) {
        return ApiResult.success(toActorVO(processChainService.enableProcessActor(actorId)));
    }

    /**
     * 停用标准办理步骤参与方。
     *
     * @param actorId 步骤参与方 ID。
     * @return 已停用的步骤参与方。
     */
    @PostMapping("/business-module-process-actors/{actorId}/disable")
    public ApiResult<ActorVO> disableActor(@PathVariable String actorId) {
        return ApiResult.success(toActorVO(processChainService.disableProcessActor(actorId)));
    }

    /**
     * 查询标准办理步骤参与方详情。
     *
     * @param actorId 步骤参与方 ID。
     * @return 步骤参与方详情。
     */
    @GetMapping("/business-module-process-actors/{actorId}")
    public ApiResult<ActorVO> actorDetail(@PathVariable String actorId) {
        return ApiResult.success(toActorVO(processChainService.getProcessActorById(actorId)));
    }

    /**
     * 查询某步骤下全部参与方。
     *
     * @param tenantId 租户 ID。
     * @param stepId 标准办理步骤 ID。
     * @return 步骤参与方列表。
     */
    @GetMapping("/business-module-process-steps/{stepId}/actors")
    public ApiResult<List<ActorVO>> listActors(@RequestParam String tenantId,
                                               @PathVariable String stepId) {
        return ApiResult.success(toActorVOList(processChainService.listProcessActors(tenantId, stepId)));
    }

    /**
     * 查询某步骤下启用的参与方。
     *
     * @param tenantId 租户 ID。
     * @param stepId 标准办理步骤 ID。
     * @return 启用步骤参与方列表。
     */
    @GetMapping("/business-module-process-steps/{stepId}/actors/active")
    public ApiResult<List<ActorVO>> listActiveActors(@RequestParam String tenantId,
                                                     @PathVariable String stepId) {
        return ApiResult.success(toActorVOList(processChainService.listActiveProcessActors(tenantId, stepId)));
    }

    /**
     * 将创建步骤请求转换为数据库实体。
     *
     * @param businessModuleId 业务模块 ID。
     * @param request 创建步骤请求。
     * @return 标准办理步骤实体。
     */
    private BusinessModuleProcessStep toCreateStep(String businessModuleId, StepRequest request) {
        BusinessModuleProcessStep step = toEditableStep(request);
        step.setId(generateId());
        step.setTenantId(request.getTenantId());
        step.setConnectorSystemId(request.getConnectorSystemId());
        step.setBusinessModuleId(businessModuleId);
        step.setModuleCode(request.getModuleCode());
        step.setStepNo(request.getStepNo());
        step.setStepCode(request.getStepCode());
        step.setCreateBy(firstText(request.getCreateBy(), request.getUpdateBy()));
        return step;
    }

    /**
     * 将更新步骤请求转换为数据库实体。
     *
     * @param stepId 标准办理步骤 ID。
     * @param request 更新步骤请求。
     * @return 标准办理步骤实体。
     */
    private BusinessModuleProcessStep toUpdateStep(String stepId, StepRequest request) {
        BusinessModuleProcessStep step = toEditableStep(request);
        step.setId(stepId);
        return step;
    }

    /**
     * 提取步骤创建和更新都允许维护的字段。
     *
     * @param request 步骤维护请求。
     * @return 标准办理步骤实体。
     */
    private BusinessModuleProcessStep toEditableStep(StepRequest request) {
        BusinessModuleProcessStep step = new BusinessModuleProcessStep();
        step.setStepName(request.getStepName());
        step.setStepType(request.getStepType());
        step.setInitExternalStatus(request.getInitExternalStatus());
        step.setTargetExternalStatus(request.getTargetExternalStatus());
        step.setCompletionRuleJson(request.getCompletionRuleJson());
        step.setRemark(request.getRemark());
        step.setUpdateBy(request.getUpdateBy());
        return step;
    }

    /**
     * 将创建参与方请求转换为数据库实体。
     *
     * @param stepId 标准办理步骤 ID。
     * @param request 创建参与方请求。
     * @return 步骤参与方实体。
     */
    private BusinessModuleProcessActor toCreateActor(String stepId, ActorRequest request) {
        BusinessModuleProcessActor actor = toEditableActor(request);
        actor.setId(generateId());
        actor.setTenantId(request.getTenantId());
        actor.setProcessStepId(stepId);
        actor.setActorNo(request.getActorNo());
        actor.setCreateBy(firstText(request.getCreateBy(), request.getUpdateBy()));
        return actor;
    }

    /**
     * 将更新参与方请求转换为数据库实体。
     *
     * @param actorId 步骤参与方 ID。
     * @param request 更新参与方请求。
     * @return 步骤参与方实体。
     */
    private BusinessModuleProcessActor toUpdateActor(String actorId, ActorRequest request) {
        BusinessModuleProcessActor actor = toEditableActor(request);
        actor.setId(actorId);
        return actor;
    }

    /**
     * 提取参与方创建和更新都允许维护的字段。
     *
     * @param request 参与方维护请求。
     * @return 步骤参与方实体。
     */
    private BusinessModuleProcessActor toEditableActor(ActorRequest request) {
        BusinessModuleProcessActor actor = new BusinessModuleProcessActor();
        actor.setActorRelation(request.getActorRelation());
        actor.setActorType(request.getActorType());
        actor.setRequiredOrgType(request.getRequiredOrgType());
        actor.setRequiredOrgCode(request.getRequiredOrgCode());
        actor.setRequiredOrgName(request.getRequiredOrgName());
        actor.setRequiredRoleCode(request.getRequiredRoleCode());
        actor.setRequiredRoleName(request.getRequiredRoleName());
        actor.setIsRequired(request.getIsRequired());
        actor.setAssignmentRule(request.getAssignmentRule());
        actor.setRemark(request.getRemark());
        actor.setUpdateBy(request.getUpdateBy());
        return actor;
    }

    /**
     * 将步骤实体列表转换为前端返回对象列表。
     *
     * @param steps 步骤实体列表。
     * @return 步骤返回对象列表。
     */
    private List<StepVO> toStepVOList(List<BusinessModuleProcessStep> steps) {
        List<StepVO> result = new ArrayList<>();
        for (BusinessModuleProcessStep step : steps) {
            result.add(toStepVO(step));
        }
        return result;
    }

    /**
     * 将参与方实体列表转换为前端返回对象列表。
     *
     * @param actors 参与方实体列表。
     * @return 参与方返回对象列表。
     */
    private List<ActorVO> toActorVOList(List<BusinessModuleProcessActor> actors) {
        List<ActorVO> result = new ArrayList<>();
        for (BusinessModuleProcessActor actor : actors) {
            result.add(toActorVO(actor));
        }
        return result;
    }

    /**
     * 将步骤实体转换为前端返回对象。
     *
     * @param step 步骤实体。
     * @return 步骤返回对象。
     */
    private StepVO toStepVO(BusinessModuleProcessStep step) {
        StepVO vo = new StepVO();
        vo.setId(step.getId());
        vo.setTenantId(step.getTenantId());
        vo.setConnectorSystemId(step.getConnectorSystemId());
        vo.setBusinessModuleId(step.getBusinessModuleId());
        vo.setModuleCode(step.getModuleCode());
        vo.setStepNo(step.getStepNo());
        vo.setStepCode(step.getStepCode());
        vo.setStepName(step.getStepName());
        vo.setStepType(step.getStepType());
        vo.setInitExternalStatus(step.getInitExternalStatus());
        vo.setTargetExternalStatus(step.getTargetExternalStatus());
        vo.setCompletionRuleJson(step.getCompletionRuleJson());
        vo.setRemark(step.getRemark());
        vo.setStatus(step.getStatus());
        vo.setCreateTime(step.getCreateTime());
        vo.setUpdateTime(step.getUpdateTime());
        return vo;
    }

    /**
     * 将参与方实体转换为前端返回对象。
     *
     * @param actor 参与方实体。
     * @return 参与方返回对象。
     */
    private ActorVO toActorVO(BusinessModuleProcessActor actor) {
        ActorVO vo = new ActorVO();
        vo.setId(actor.getId());
        vo.setTenantId(actor.getTenantId());
        vo.setConnectorSystemId(actor.getConnectorSystemId());
        vo.setBusinessModuleId(actor.getBusinessModuleId());
        vo.setProcessStepId(actor.getProcessStepId());
        vo.setModuleCode(actor.getModuleCode());
        vo.setStepCode(actor.getStepCode());
        vo.setActorNo(actor.getActorNo());
        vo.setActorRelation(actor.getActorRelation());
        vo.setActorType(actor.getActorType());
        vo.setRequiredOrgType(actor.getRequiredOrgType());
        vo.setRequiredOrgCode(actor.getRequiredOrgCode());
        vo.setRequiredOrgName(actor.getRequiredOrgName());
        vo.setRequiredRoleCode(actor.getRequiredRoleCode());
        vo.setRequiredRoleName(actor.getRequiredRoleName());
        vo.setIsRequired(actor.getIsRequired());
        vo.setAssignmentRule(actor.getAssignmentRule());
        vo.setRemark(actor.getRemark());
        vo.setStatus(actor.getStatus());
        vo.setCreateTime(actor.getCreateTime());
        vo.setUpdateTime(actor.getUpdateTime());
        return vo;
    }

    /**
     * 取第一个有内容的文本，创建接口允许前端只传 updateBy 来兼容现有后台表单。
     *
     * @param first 首选文本。
     * @param second 兜底文本。
     * @return 第一个非空文本。
     */
    private String firstText(String first, String second) {
        if (first != null && first.trim().length() > 0) {
            return first;
        }
        return second;
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
