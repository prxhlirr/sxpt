package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.BusinessModuleProcessActor;
import com.sxpt.module.connector.entity.BusinessModuleProcessStep;
import com.sxpt.module.connector.mapper.BusinessModuleProcessActorMapper;
import com.sxpt.module.connector.mapper.BusinessModuleProcessStepMapper;
import com.sxpt.module.connector.service.BusinessModuleProcessChainService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务模块标准办理链服务实现。
 *
 * 业务功能：
 * 1. 为后台维护标准办理步骤和步骤参与方提供统一业务入口。
 * 2. 通过步骤表和参与方表表达“一步骤多单位多角色”的原平台办理结构。
 * 3. 为后续数据准备链路快照生成、学生数据分配和原平台 launchToken 身份定位提供标准链来源。
 *
 * 关键流程：
 * 1. 创建步骤时校验同一业务模块下 stepCode 和 stepNo 不重复。
 * 2. 创建参与方时校验所属步骤存在，且同一步骤下 actorNo 不重复。
 * 3. 查询时统一追加租户、状态和软删除条件，避免跨租户或误读停用配置。
 */
@Service
@Profile("!test")
public class BusinessModuleProcessChainServiceImpl implements BusinessModuleProcessChainService {

    private final BusinessModuleProcessStepMapper stepMapper;

    private final BusinessModuleProcessActorMapper actorMapper;

    public BusinessModuleProcessChainServiceImpl(BusinessModuleProcessStepMapper stepMapper,
                                                 BusinessModuleProcessActorMapper actorMapper) {
        this.stepMapper = stepMapper;
        this.actorMapper = actorMapper;
    }

    /**
     * 创建业务模块标准办理步骤。
     *
     * @param step 标准办理步骤实体。
     * @return 已保存的标准办理步骤。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessModuleProcessStep createProcessStep(BusinessModuleProcessStep step) {
        validateCreateStep(step);
        ensureStepUnique(step);
        fillCreateDefaults(step);
        stepMapper.insert(step);
        return step;
    }

    /**
     * 更新业务模块标准办理步骤。
     *
     * @param step 标准办理步骤实体。
     * @return 已更新的标准办理步骤。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessModuleProcessStep updateProcessStep(BusinessModuleProcessStep step) {
        validateUpdateStep(step);
        BusinessModuleProcessStep existing = getProcessStepById(step.getId());
        existing.setStepName(step.getStepName());
        existing.setStepType(step.getStepType());
        existing.setInitExternalStatus(step.getInitExternalStatus());
        existing.setTargetExternalStatus(step.getTargetExternalStatus());
        existing.setCompletionRuleJson(step.getCompletionRuleJson());
        existing.setRemark(step.getRemark());
        existing.setUpdateBy(step.getUpdateBy());
        existing.setUpdateTime(LocalDateTime.now());
        existing.setLockVersion(nextLockVersion(existing.getLockVersion()));
        stepMapper.updateById(existing);
        return existing;
    }

    /**
     * 启用业务模块标准办理步骤。
     *
     * @param stepId 标准办理步骤 ID。
     * @return 已启用的标准办理步骤。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessModuleProcessStep enableProcessStep(String stepId) {
        return changeStepStatus(stepId, RecordStatus.ACTIVE.getValue());
    }

    /**
     * 停用业务模块标准办理步骤。
     *
     * @param stepId 标准办理步骤 ID。
     * @return 已停用的标准办理步骤。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessModuleProcessStep disableProcessStep(String stepId) {
        return changeStepStatus(stepId, RecordStatus.DISABLED.getValue());
    }

    /**
     * 查询业务模块标准办理步骤详情。
     *
     * @param stepId 标准办理步骤 ID。
     * @return 未删除的标准办理步骤。
     */
    @Override
    public BusinessModuleProcessStep getProcessStepById(String stepId) {
        requireText(stepId);
        BusinessModuleProcessStep step = stepMapper.selectOne(new QueryWrapper<BusinessModuleProcessStep>()
                .eq("id", stepId)
                .eq("deleted", Boolean.FALSE));
        if (step == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return step;
    }

    /**
     * 查询业务模块下全部未删除标准办理步骤。
     *
     * @param tenantId 租户 ID。
     * @param businessModuleId 业务模块 ID。
     * @return 标准办理步骤列表。
     */
    @Override
    public List<BusinessModuleProcessStep> listProcessSteps(String tenantId, String businessModuleId) {
        requireText(tenantId);
        requireText(businessModuleId);
        return stepMapper.selectList(new QueryWrapper<BusinessModuleProcessStep>()
                .eq("tenant_id", tenantId)
                .eq("business_module_id", businessModuleId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("step_no"));
    }

    /**
     * 查询业务模块下启用的标准办理步骤。
     *
     * @param tenantId 租户 ID。
     * @param businessModuleId 业务模块 ID。
     * @return 启用标准办理步骤列表。
     */
    @Override
    public List<BusinessModuleProcessStep> listActiveProcessSteps(String tenantId, String businessModuleId) {
        requireText(tenantId);
        requireText(businessModuleId);
        return stepMapper.selectList(new QueryWrapper<BusinessModuleProcessStep>()
                .eq("tenant_id", tenantId)
                .eq("business_module_id", businessModuleId)
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("step_no"));
    }

    /**
     * 创建标准办理步骤参与方。
     *
     * @param actor 步骤参与方实体。
     * @return 已保存的步骤参与方。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessModuleProcessActor createProcessActor(BusinessModuleProcessActor actor) {
        validateCreateActor(actor);
        BusinessModuleProcessStep step = getProcessStepById(actor.getProcessStepId());
        alignActorWithStep(actor, step);
        ensureActorUnique(actor);
        fillCreateDefaults(actor);
        actorMapper.insert(actor);
        return actor;
    }

    /**
     * 更新标准办理步骤参与方。
     *
     * @param actor 步骤参与方实体。
     * @return 已更新的步骤参与方。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessModuleProcessActor updateProcessActor(BusinessModuleProcessActor actor) {
        validateUpdateActor(actor);
        BusinessModuleProcessActor existing = getProcessActorById(actor.getId());
        existing.setActorRelation(actor.getActorRelation());
        existing.setActorType(actor.getActorType());
        existing.setRequiredOrgType(actor.getRequiredOrgType());
        existing.setRequiredOrgCode(actor.getRequiredOrgCode());
        existing.setRequiredOrgName(actor.getRequiredOrgName());
        existing.setRequiredRoleCode(actor.getRequiredRoleCode());
        existing.setRequiredRoleName(actor.getRequiredRoleName());
        existing.setIsRequired(actor.getIsRequired());
        existing.setAssignmentRule(actor.getAssignmentRule());
        existing.setRemark(actor.getRemark());
        existing.setUpdateBy(actor.getUpdateBy());
        existing.setUpdateTime(LocalDateTime.now());
        existing.setLockVersion(nextLockVersion(existing.getLockVersion()));
        actorMapper.updateById(existing);
        return existing;
    }

    /**
     * 启用标准办理步骤参与方。
     *
     * @param actorId 步骤参与方 ID。
     * @return 已启用的步骤参与方。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessModuleProcessActor enableProcessActor(String actorId) {
        return changeActorStatus(actorId, RecordStatus.ACTIVE.getValue());
    }

    /**
     * 停用标准办理步骤参与方。
     *
     * @param actorId 步骤参与方 ID。
     * @return 已停用的步骤参与方。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessModuleProcessActor disableProcessActor(String actorId) {
        return changeActorStatus(actorId, RecordStatus.DISABLED.getValue());
    }

    /**
     * 查询标准办理步骤参与方详情。
     *
     * @param actorId 步骤参与方 ID。
     * @return 未删除的步骤参与方。
     */
    @Override
    public BusinessModuleProcessActor getProcessActorById(String actorId) {
        requireText(actorId);
        BusinessModuleProcessActor actor = actorMapper.selectOne(new QueryWrapper<BusinessModuleProcessActor>()
                .eq("id", actorId)
                .eq("deleted", Boolean.FALSE));
        if (actor == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return actor;
    }

    /**
     * 查询某步骤下全部未删除参与方。
     *
     * @param tenantId 租户 ID。
     * @param processStepId 标准办理步骤 ID。
     * @return 步骤参与方列表。
     */
    @Override
    public List<BusinessModuleProcessActor> listProcessActors(String tenantId, String processStepId) {
        requireText(tenantId);
        requireText(processStepId);
        return actorMapper.selectList(new QueryWrapper<BusinessModuleProcessActor>()
                .eq("tenant_id", tenantId)
                .eq("process_step_id", processStepId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("actor_no"));
    }

    /**
     * 查询某步骤下启用的参与方。
     *
     * @param tenantId 租户 ID。
     * @param processStepId 标准办理步骤 ID。
     * @return 启用步骤参与方列表。
     */
    @Override
    public List<BusinessModuleProcessActor> listActiveProcessActors(String tenantId, String processStepId) {
        requireText(tenantId);
        requireText(processStepId);
        return actorMapper.selectList(new QueryWrapper<BusinessModuleProcessActor>()
                .eq("tenant_id", tenantId)
                .eq("process_step_id", processStepId)
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("actor_no"));
    }

    /**
     * 校验创建步骤的最小字段，避免产生无法挂接模块和无法排序的步骤。
     *
     * @param step 标准办理步骤实体。
     */
    private void validateCreateStep(BusinessModuleProcessStep step) {
        if (step == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(step.getId());
        requireText(step.getTenantId());
        requireText(step.getConnectorSystemId());
        requireText(step.getBusinessModuleId());
        requireText(step.getModuleCode());
        requireText(step.getStepCode());
        requireText(step.getStepName());
        requireText(step.getCreateBy());
        requireText(step.getUpdateBy());
        if (step.getStepNo() == null || step.getStepNo() <= 0) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验更新步骤的最小字段，步骤编码和顺序属于稳定身份，暂不允许更新。
     *
     * @param step 标准办理步骤实体。
     */
    private void validateUpdateStep(BusinessModuleProcessStep step) {
        if (step == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(step.getId());
        requireText(step.getStepName());
        requireText(step.getUpdateBy());
    }

    /**
     * 校验创建参与方的最小字段，保证参与方能定位到具体步骤和具体角色关系。
     *
     * @param actor 步骤参与方实体。
     */
    private void validateCreateActor(BusinessModuleProcessActor actor) {
        if (actor == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(actor.getId());
        requireText(actor.getTenantId());
        requireText(actor.getProcessStepId());
        requireText(actor.getActorRelation());
        requireText(actor.getActorType());
        requireText(actor.getAssignmentRule());
        requireText(actor.getCreateBy());
        requireText(actor.getUpdateBy());
        if (actor.getActorNo() == null || actor.getActorNo() <= 0) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验更新参与方的最小字段，参与方序号属于稳定身份，暂不允许更新。
     *
     * @param actor 步骤参与方实体。
     */
    private void validateUpdateActor(BusinessModuleProcessActor actor) {
        if (actor == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(actor.getId());
        requireText(actor.getActorRelation());
        requireText(actor.getActorType());
        requireText(actor.getAssignmentRule());
        requireText(actor.getUpdateBy());
    }

    /**
     * 校验同一业务模块下步骤编码和步骤顺序不重复。
     *
     * @param step 标准办理步骤实体。
     */
    private void ensureStepUnique(BusinessModuleProcessStep step) {
        Integer codeCount = stepMapper.selectCount(new QueryWrapper<BusinessModuleProcessStep>()
                .eq("tenant_id", step.getTenantId())
                .eq("business_module_id", step.getBusinessModuleId())
                .eq("step_code", step.getStepCode())
                .eq("deleted", Boolean.FALSE));
        Integer noCount = stepMapper.selectCount(new QueryWrapper<BusinessModuleProcessStep>()
                .eq("tenant_id", step.getTenantId())
                .eq("business_module_id", step.getBusinessModuleId())
                .eq("step_no", step.getStepNo())
                .eq("deleted", Boolean.FALSE));
        if ((codeCount != null && codeCount > 0) || (noCount != null && noCount > 0)) {
            throw new BusinessException(ApiResultCode.IDEMPOTENCY_CONFLICT);
        }
    }

    /**
     * 校验同一步骤下参与方序号不重复。
     *
     * @param actor 步骤参与方实体。
     */
    private void ensureActorUnique(BusinessModuleProcessActor actor) {
        Integer count = actorMapper.selectCount(new QueryWrapper<BusinessModuleProcessActor>()
                .eq("tenant_id", actor.getTenantId())
                .eq("process_step_id", actor.getProcessStepId())
                .eq("actor_no", actor.getActorNo())
                .eq("deleted", Boolean.FALSE));
        if (count != null && count > 0) {
            throw new BusinessException(ApiResultCode.IDEMPOTENCY_CONFLICT);
        }
    }

    /**
     * 将参与方的模块和步骤冗余字段与所属步骤对齐，避免前端传错造成链路快照不一致。
     *
     * @param actor 步骤参与方实体。
     * @param step 所属标准办理步骤。
     */
    private void alignActorWithStep(BusinessModuleProcessActor actor, BusinessModuleProcessStep step) {
        actor.setConnectorSystemId(step.getConnectorSystemId());
        actor.setBusinessModuleId(step.getBusinessModuleId());
        actor.setModuleCode(step.getModuleCode());
        actor.setStepCode(step.getStepCode());
    }

    /**
     * 切换标准办理步骤状态。
     *
     * @param stepId 标准办理步骤 ID。
     * @param status 目标状态。
     * @return 已切换状态的标准办理步骤。
     */
    private BusinessModuleProcessStep changeStepStatus(String stepId, String status) {
        BusinessModuleProcessStep existing = getProcessStepById(stepId);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        existing.setLockVersion(nextLockVersion(existing.getLockVersion()));
        stepMapper.updateById(existing);
        return existing;
    }

    /**
     * 切换标准办理步骤参与方状态。
     *
     * @param actorId 步骤参与方 ID。
     * @param status 目标状态。
     * @return 已切换状态的步骤参与方。
     */
    private BusinessModuleProcessActor changeActorStatus(String actorId, String status) {
        BusinessModuleProcessActor existing = getProcessActorById(actorId);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        existing.setLockVersion(nextLockVersion(existing.getLockVersion()));
        actorMapper.updateById(existing);
        return existing;
    }

    /**
     * 补齐创建默认值，保证应用层插入不依赖数据库默认值回填。
     *
     * @param step 标准办理步骤实体。
     */
    private void fillCreateDefaults(BusinessModuleProcessStep step) {
        LocalDateTime now = LocalDateTime.now();
        if (step.getLockVersion() == null) {
            step.setLockVersion(0L);
        }
        if (step.getCreateTime() == null) {
            step.setCreateTime(now);
        }
        if (step.getUpdateTime() == null) {
            step.setUpdateTime(now);
        }
        if (!StringUtils.hasText(step.getStatus())) {
            step.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (step.getDeleted() == null) {
            step.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 补齐创建默认值，保证参与方默认是必需且启用的配置。
     *
     * @param actor 步骤参与方实体。
     */
    private void fillCreateDefaults(BusinessModuleProcessActor actor) {
        LocalDateTime now = LocalDateTime.now();
        if (actor.getIsRequired() == null) {
            actor.setIsRequired(Boolean.TRUE);
        }
        if (actor.getLockVersion() == null) {
            actor.setLockVersion(0L);
        }
        if (actor.getCreateTime() == null) {
            actor.setCreateTime(now);
        }
        if (actor.getUpdateTime() == null) {
            actor.setUpdateTime(now);
        }
        if (!StringUtils.hasText(actor.getStatus())) {
            actor.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (actor.getDeleted() == null) {
            actor.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 计算下一个乐观锁版本。
     *
     * @param lockVersion 当前版本。
     * @return 下一个版本。
     */
    private Long nextLockVersion(Long lockVersion) {
        return lockVersion == null ? 1L : lockVersion + 1L;
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
}
