package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.entity.BusinessModuleProcessActor;
import com.sxpt.module.connector.entity.BusinessModuleProcessStep;
import com.sxpt.module.connector.entity.OriginOrg;
import com.sxpt.module.connector.entity.OriginRole;
import com.sxpt.module.connector.mapper.BusinessModuleMapper;
import com.sxpt.module.connector.mapper.BusinessModuleProcessActorMapper;
import com.sxpt.module.connector.mapper.BusinessModuleProcessStepMapper;
import com.sxpt.module.connector.mapper.OriginOrgMapper;
import com.sxpt.module.connector.mapper.OriginRoleMapper;
import com.sxpt.module.connector.service.BusinessModuleService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 原平台业务模块管理服务实现。
 *
 * 业务功能：
 * 1. 为后台维护业务名称、租户、业务 ID、入口、支持场景和默认状态提供统一业务入口。
 * 2. 为数据准备策略选择业务模块时提供租户和原平台边界，保证后续生成的数据能落到正确原业务系统。
 *
 * 关键流程：
 * 1. 创建前校验业务模块最小可用字段，补齐状态、软删除、版本和时间字段。
 * 2. 更新时先读取现有记录，再覆盖允许维护字段并递增版本，避免稳定身份字段被误改。
 * 3. 查询时统一追加未删除条件，启用列表额外限制 ACTIVE 状态。
 */
@Service
@Profile("!test")
public class BusinessModuleServiceImpl implements BusinessModuleService {

    private final BusinessModuleMapper businessModuleMapper;

    private final BusinessModuleProcessStepMapper processStepMapper;

    private final BusinessModuleProcessActorMapper processActorMapper;

    private final OriginRoleMapper originRoleMapper;

    private final OriginOrgMapper originOrgMapper;

    public BusinessModuleServiceImpl(BusinessModuleMapper businessModuleMapper,
                                     BusinessModuleProcessStepMapper processStepMapper,
                                     BusinessModuleProcessActorMapper processActorMapper,
                                     OriginRoleMapper originRoleMapper,
                                     OriginOrgMapper originOrgMapper) {
        this.businessModuleMapper = businessModuleMapper;
        this.processStepMapper = processStepMapper;
        this.processActorMapper = processActorMapper;
        this.originRoleMapper = originRoleMapper;
        this.originOrgMapper = originOrgMapper;
    }

    /**
     * 创建原平台业务模块。
     *
     * @param businessModule 原平台业务模块实体。
     * @return 已保存的业务模块实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessModule createBusinessModule(BusinessModule businessModule) {
        validateCreateFields(businessModule);
        fillCreateDefaults(businessModule);
        if (RecordStatus.ACTIVE.getValue().equals(businessModule.getStatus())) {
            validateActiveProcessChain(businessModule);
        }
        businessModuleMapper.insert(businessModule);
        return businessModule;
    }

    /**
     * 更新原平台业务模块。
     *
     * @param businessModule 原平台业务模块实体。
     * @return 已更新的业务模块实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessModule updateBusinessModule(BusinessModule businessModule) {
        validateUpdateFields(businessModule);
        BusinessModule existing = getBusinessModuleById(businessModule.getId());
        existing.setModuleName(businessModule.getModuleName());
        existing.setExternalModuleId(businessModule.getExternalModuleId());
        existing.setEntryUrl(businessModule.getEntryUrl());
        existing.setModuleType(businessModule.getModuleType());
        existing.setSupportScenes(businessModule.getSupportScenes());
        existing.setNeedPreData(businessModule.getNeedPreData());
        existing.setDefaultInitialStatus(businessModule.getDefaultInitialStatus());
        existing.setDefaultTargetStatus(businessModule.getDefaultTargetStatus());
        existing.setCapabilityCodesJson(businessModule.getCapabilityCodesJson());
        existing.setDefaultTemplateId(businessModule.getDefaultTemplateId());
        existing.setRemark(businessModule.getRemark());
        existing.setUpdateBy(businessModule.getUpdateBy());
        existing.setUpdateTime(LocalDateTime.now());
        existing.setLockVersion(nextLockVersion(existing.getLockVersion()));
        businessModuleMapper.updateById(existing);
        return existing;
    }

    /**
     * 启用原平台业务模块。
     *
     * @param id 业务模块 ID。
     * @return 已启用的业务模块实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessModule enableBusinessModule(String id) {
        return changeStatus(id, RecordStatus.ACTIVE.getValue());
    }

    /**
     * 禁用原平台业务模块。
     *
     * @param id 业务模块 ID。
     * @return 已禁用的业务模块实体。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessModule disableBusinessModule(String id) {
        return changeStatus(id, RecordStatus.DISABLED.getValue());
    }

    /**
     * 查询业务模块详情。
     *
     * @param id 业务模块 ID。
     * @return 未删除的业务模块实体。
     */
    @Override
    public BusinessModule getBusinessModuleById(String id) {
        requireText(id);
        BusinessModule businessModule = businessModuleMapper.selectOne(new QueryWrapper<BusinessModule>()
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (businessModule == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return businessModule;
    }

    /**
     * 查询某租户在某原平台下的全部未删除业务模块。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @return 业务模块列表。
     */
    @Override
    public List<BusinessModule> listBusinessModules(String tenantId, String connectorSystemId) {
        requireText(tenantId);
        requireText(connectorSystemId);
        return businessModuleMapper.selectList(new QueryWrapper<BusinessModule>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 查询某租户在某原平台下可用于策略选择的启用业务模块。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @return 启用业务模块列表。
     */
    @Override
    public List<BusinessModule> listActiveBusinessModules(String tenantId, String connectorSystemId) {
        requireText(tenantId);
        requireText(connectorSystemId);
        return businessModuleMapper.selectList(new QueryWrapper<BusinessModule>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 切换业务模块通用状态。
     *
     * @param id 业务模块 ID。
     * @param status 目标状态。
     * @return 已切换状态的业务模块实体。
     */
    private BusinessModule changeStatus(String id, String status) {
        requireText(id);
        BusinessModule existing = getBusinessModuleById(id);
        if (RecordStatus.ACTIVE.getValue().equals(status)) {
            validateActiveProcessChain(existing);
        }
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        existing.setLockVersion(nextLockVersion(existing.getLockVersion()));
        businessModuleMapper.updateById(existing);
        return existing;
    }

    /**
     * 校验创建业务模块所需的最小字段。
     *
     * @param businessModule 业务模块实体。
     */
    private void validateCreateFields(BusinessModule businessModule) {
        if (businessModule == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(businessModule.getId());
        requireText(businessModule.getTenantId());
        requireText(businessModule.getConnectorSystemId());
        requireText(businessModule.getModuleCode());
        requireText(businessModule.getModuleName());
        requireText(businessModule.getEntryUrl());
        requireText(businessModule.getSupportScenes());
    }

    /**
     * 校验更新业务模块所需的最小字段。
     *
     * @param businessModule 业务模块实体。
     */
    private void validateUpdateFields(BusinessModule businessModule) {
        if (businessModule == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(businessModule.getId());
        requireText(businessModule.getModuleName());
        requireText(businessModule.getEntryUrl());
        requireText(businessModule.getSupportScenes());
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
     * 校验业务模块启用前的标准办理链，避免教师发布任务时才发现无法确定原平台单位和角色。
     *
     * @param businessModule 业务模块实体。
     */
    private void validateActiveProcessChain(BusinessModule businessModule) {
        if (Boolean.FALSE.equals(businessModule.getNeedPreData())) {
            return;
        }
        List<BusinessModuleProcessStep> activeSteps = processStepMapper.selectList(new QueryWrapper<BusinessModuleProcessStep>()
                .eq("tenant_id", businessModule.getTenantId())
                .eq("business_module_id", businessModule.getId())
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("step_no"));
        if (activeSteps == null || activeSteps.isEmpty()) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
        for (BusinessModuleProcessStep step : activeSteps) {
            List<BusinessModuleProcessActor> activeActors = processActorMapper.selectList(new QueryWrapper<BusinessModuleProcessActor>()
                    .eq("tenant_id", businessModule.getTenantId())
                    .eq("process_step_id", step.getId())
                    .eq("status", RecordStatus.ACTIVE.getValue())
                    .eq("deleted", Boolean.FALSE)
                    .orderByAsc("actor_no"));
            if (activeActors == null || activeActors.isEmpty()) {
                throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
            }
            for (BusinessModuleProcessActor actor : activeActors) {
                validateActiveActorDictionaryReference(businessModule, actor);
            }
        }
    }

    /**
     * 校验启用参与方引用的原平台组织和角色仍然有效。
     *
     * @param businessModule 业务模块实体。
     * @param actor 步骤参与方实体。
     */
    private void validateActiveActorDictionaryReference(BusinessModule businessModule,
                                                        BusinessModuleProcessActor actor) {
        if (!StringUtils.hasText(actor.getRequiredOrgCode())
                || !StringUtils.hasText(actor.getRequiredRoleCode())) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
        Integer activeRoleCount = originRoleMapper.selectCount(new QueryWrapper<OriginRole>()
                .eq("tenant_id", businessModule.getTenantId())
                .eq("connector_system_id", businessModule.getConnectorSystemId())
                .eq("role_code", actor.getRequiredRoleCode())
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE));
        if (activeRoleCount == null || activeRoleCount <= 0) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
        Integer activeOrgCount = originOrgMapper.selectCount(new QueryWrapper<OriginOrg>()
                .eq("tenant_id", businessModule.getTenantId())
                .eq("connector_system_id", businessModule.getConnectorSystemId())
                .eq("org_code", actor.getRequiredOrgCode())
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE));
        if (activeOrgCount == null || activeOrgCount <= 0) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE);
        }
    }

    /**
     * 补齐创建时默认字段。
     *
     * @param businessModule 业务模块实体。
     */
    private void fillCreateDefaults(BusinessModule businessModule) {
        LocalDateTime now = LocalDateTime.now();
        if (businessModule.getCreateTime() == null) {
            businessModule.setCreateTime(now);
        }
        if (businessModule.getUpdateTime() == null) {
            businessModule.setUpdateTime(now);
        }
        if (businessModule.getLockVersion() == null) {
            businessModule.setLockVersion(0L);
        }
        if (businessModule.getNeedPreData() == null) {
            businessModule.setNeedPreData(Boolean.TRUE);
        }
        if (!StringUtils.hasText(businessModule.getStatus())) {
            businessModule.setStatus(RecordStatus.DISABLED.getValue());
        }
        if (!StringUtils.hasText(businessModule.getCreateBy())) {
            businessModule.setCreateBy(resolveOperator(businessModule));
        }
        if (!StringUtils.hasText(businessModule.getUpdateBy())) {
            businessModule.setUpdateBy(businessModule.getCreateBy());
        }
        if (businessModule.getDeleted() == null) {
            businessModule.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 递增乐观锁版本。
     *
     * @param currentVersion 当前版本，历史数据为空时按 0 处理。
     * @return 下一版本号。
     */
    private Long nextLockVersion(Long currentVersion) {
        if (currentVersion == null) {
            return 1L;
        }
        return currentVersion + 1;
    }

    /**
     * 解析创建业务模块时使用的审计操作人。
     *
     * @param businessModule 业务模块实体。
     * @return 优先使用前端传入的更新人，缺失时使用系统操作人，避免数据库审计字段为空。
     */
    private String resolveOperator(BusinessModule businessModule) {
        if (StringUtils.hasText(businessModule.getUpdateBy())) {
            return businessModule.getUpdateBy();
        }
        return "system";
    }
}
