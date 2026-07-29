package com.sxpt.module.connector.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.BusinessModuleProcessActor;
import com.sxpt.module.connector.entity.BusinessModuleProcessStep;
import com.sxpt.module.connector.service.BusinessModuleProcessChainService;
import com.sxpt.module.connector.service.BusinessModuleProcessSnapshotService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 业务模块办理链快照生成服务实现。
 *
 * 业务功能：
 * 1. 将后台维护的标准步骤和步骤参与方转换为数据实例级办理链快照。
 * 2. 在原平台未返回完整链路时，为数据准备结果补齐当前步骤、当前参与方和原平台单位角色。
 * 3. 保证后续学生进入练习或考试时不再临时猜测身份，而是使用已固化的实例快照。
 *
 * 关键流程：
 * 1. 查询业务模块下启用的标准步骤。
 * 2. 查询每个步骤下启用的参与方，并组装为 processChain 数组。
 * 3. 按原平台返回优先、PRIMARY 优先、必需优先、序号优先的规则定位默认进入身份。
 */
@Service
@Profile("!test")
public class BusinessModuleProcessSnapshotServiceImpl implements BusinessModuleProcessSnapshotService {

    private static final String PRIMARY_RELATION = "PRIMARY";

    private final BusinessModuleProcessChainService processChainService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public BusinessModuleProcessSnapshotServiceImpl(BusinessModuleProcessChainService processChainService) {
        this.processChainService = processChainService;
    }

    /**
     * 根据业务模块标准链生成数据实例办理链快照。
     *
     * @param tenantId 租户 ID。
     * @param businessModuleId 业务模块 ID。
     * @param preferredStepCode 原平台返回的当前步骤编码，可为空。
     * @param preferredActorNo 原平台返回的当前参与方序号，可为空。
     * @return 办理链快照生成结果。
     */
    @Override
    public SnapshotResult generateStandardSnapshot(String tenantId,
                                                   String businessModuleId,
                                                   String preferredStepCode,
                                                   Integer preferredActorNo) {
        requireText(tenantId);
        requireText(businessModuleId);
        List<BusinessModuleProcessStep> steps = processChainService.listActiveProcessSteps(tenantId, businessModuleId);
        if (steps == null || steps.isEmpty()) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        List<StepSnapshotContext> contexts = buildSnapshotContexts(tenantId, steps);
        StepSnapshotContext currentStep = resolveCurrentStep(contexts, preferredStepCode);
        BusinessModuleProcessActor currentActor = resolveCurrentActor(currentStep.getActors(), preferredActorNo);
        SnapshotResult result = new SnapshotResult();
        result.setSnapshotJson(toJson(contexts));
        result.setCurrentStepCode(currentStep.getStep().getStepCode());
        result.setCurrentStepName(currentStep.getStep().getStepName());
        result.setCurrentActorNo(currentActor.getActorNo());
        result.setCurrentActorRelation(currentActor.getActorRelation());
        result.setCurrentOrgId(firstText(currentActor.getRequiredOrgCode(), currentActor.getRequiredOrgType()));
        result.setCurrentOrgName(currentActor.getRequiredOrgName());
        result.setCurrentRoleId(currentActor.getRequiredRoleCode());
        result.setCurrentRoleName(currentActor.getRequiredRoleName());
        return result;
    }

    /**
     * 构造包含步骤和参与方的快照上下文。
     *
     * @param tenantId 租户 ID。
     * @param steps 启用标准步骤列表。
     * @return 快照上下文列表。
     */
    private List<StepSnapshotContext> buildSnapshotContexts(String tenantId, List<BusinessModuleProcessStep> steps) {
        List<StepSnapshotContext> contexts = new ArrayList<>();
        for (BusinessModuleProcessStep step : steps) {
            List<BusinessModuleProcessActor> actors =
                    processChainService.listActiveProcessActors(tenantId, step.getId());
            if (actors == null || actors.isEmpty()) {
                throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
            }
            contexts.add(new StepSnapshotContext(step, actors));
        }
        return contexts;
    }

    /**
     * 定位当前步骤。
     * <p>
     * 原平台若返回当前步骤，以原平台为准；否则使用标准链第一步作为默认进入点，保证普通练习可以直接进入。
     *
     * @param contexts 快照上下文列表。
     * @param preferredStepCode 原平台返回的当前步骤编码。
     * @return 当前步骤上下文。
     */
    private StepSnapshotContext resolveCurrentStep(List<StepSnapshotContext> contexts, String preferredStepCode) {
        if (StringUtils.hasText(preferredStepCode)) {
            for (StepSnapshotContext context : contexts) {
                if (preferredStepCode.equals(context.getStep().getStepCode())) {
                    return context;
                }
            }
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        return contexts.get(0);
    }

    /**
     * 定位当前参与方。
     * <p>
     * 当前参与方优先级为：原平台指定 actorNo、PRIMARY、必需参与方、序号最小参与方。
     *
     * @param actors 当前步骤下启用参与方。
     * @param preferredActorNo 原平台返回的当前参与方序号。
     * @return 当前参与方。
     */
    private BusinessModuleProcessActor resolveCurrentActor(List<BusinessModuleProcessActor> actors,
                                                           Integer preferredActorNo) {
        if (preferredActorNo != null) {
            for (BusinessModuleProcessActor actor : actors) {
                if (preferredActorNo.equals(actor.getActorNo())) {
                    return actor;
                }
            }
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED);
        }
        BusinessModuleProcessActor primary = findPrimaryActor(actors);
        if (primary != null) {
            return primary;
        }
        BusinessModuleProcessActor required = findRequiredActor(actors);
        if (required != null) {
            return required;
        }
        return actors.get(0);
    }

    /**
     * 查询主办参与方。
     *
     * @param actors 参与方列表。
     * @return 主办参与方；不存在时返回 null。
     */
    private BusinessModuleProcessActor findPrimaryActor(List<BusinessModuleProcessActor> actors) {
        for (BusinessModuleProcessActor actor : actors) {
            if (PRIMARY_RELATION.equals(actor.getActorRelation())) {
                return actor;
            }
        }
        return null;
    }

    /**
     * 查询必需参与方。
     *
     * @param actors 参与方列表。
     * @return 必需参与方；不存在时返回 null。
     */
    private BusinessModuleProcessActor findRequiredActor(List<BusinessModuleProcessActor> actors) {
        for (BusinessModuleProcessActor actor : actors) {
            if (Boolean.TRUE.equals(actor.getIsRequired())) {
                return actor;
            }
        }
        return null;
    }

    /**
     * 将快照上下文转换为 JSON 字符串。
     *
     * @param contexts 快照上下文列表。
     * @return 可持久化的 JSON 字符串。
     */
    private String toJson(List<StepSnapshotContext> contexts) {
        List<Map<String, Object>> steps = new ArrayList<>();
        for (StepSnapshotContext context : contexts) {
            steps.add(toStepMap(context));
        }
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 将步骤上下文转换为快照结构。
     *
     * @param context 步骤上下文。
     * @return 步骤快照结构。
     */
    private Map<String, Object> toStepMap(StepSnapshotContext context) {
        BusinessModuleProcessStep step = context.getStep();
        Map<String, Object> stepMap = new LinkedHashMap<>();
        stepMap.put("stepNo", step.getStepNo());
        stepMap.put("stepCode", step.getStepCode());
        stepMap.put("stepName", step.getStepName());
        stepMap.put("stepType", step.getStepType());
        stepMap.put("initExternalStatus", step.getInitExternalStatus());
        stepMap.put("targetExternalStatus", step.getTargetExternalStatus());
        stepMap.put("completionRuleJson", step.getCompletionRuleJson());
        List<Map<String, Object>> actorMaps = new ArrayList<>();
        for (BusinessModuleProcessActor actor : context.getActors()) {
            actorMaps.add(toActorMap(actor));
        }
        stepMap.put("actors", actorMaps);
        return stepMap;
    }

    /**
     * 将参与方转换为快照结构。
     *
     * @param actor 步骤参与方。
     * @return 参与方快照结构。
     */
    private Map<String, Object> toActorMap(BusinessModuleProcessActor actor) {
        Map<String, Object> actorMap = new LinkedHashMap<>();
        actorMap.put("actorNo", actor.getActorNo());
        actorMap.put("actorRelation", actor.getActorRelation());
        actorMap.put("actorType", actor.getActorType());
        actorMap.put("orgType", actor.getRequiredOrgType());
        actorMap.put("orgId", firstText(actor.getRequiredOrgCode(), actor.getRequiredOrgType()));
        actorMap.put("orgName", actor.getRequiredOrgName());
        actorMap.put("roleId", actor.getRequiredRoleCode());
        actorMap.put("roleName", actor.getRequiredRoleName());
        actorMap.put("isRequired", actor.getIsRequired());
        actorMap.put("assignmentRule", actor.getAssignmentRule());
        return actorMap;
    }

    /**
     * 取第一个有内容的文本。
     *
     * @param first 首选文本。
     * @param second 兜底文本。
     * @return 第一个非空文本。
     */
    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    /**
     * 校验文本字段非空。
     *
     * @param value 待校验字段值。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 步骤快照上下文。
     *
     * 业务功能：
     * 1. 临时聚合一个标准步骤及其启用参与方。
     * 2. 避免在生成 JSON 和定位当前身份时重复查询数据库。
     */
    private static class StepSnapshotContext {

        private final BusinessModuleProcessStep step;

        private final List<BusinessModuleProcessActor> actors;

        StepSnapshotContext(BusinessModuleProcessStep step, List<BusinessModuleProcessActor> actors) {
            this.step = step;
            this.actors = actors;
        }

        BusinessModuleProcessStep getStep() {
            return step;
        }

        List<BusinessModuleProcessActor> getActors() {
            return actors;
        }
    }
}
