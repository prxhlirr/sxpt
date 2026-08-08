package com.sxpt.module.teachingdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.entity.ModuleDataStrategy;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.mapper.BusinessModuleMapper;
import com.sxpt.module.connector.service.ModuleDataStrategyService;
import com.sxpt.module.connector.service.TeachingDataTemplateService;
import com.sxpt.module.teachingdata.entity.DataRequirement;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RequirementStatus;
import com.sxpt.module.teachingdata.mapper.DataRequirementMapper;
import com.sxpt.module.teachingdata.service.DataRequirementService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据需求批次服务实现。
 *
 * 业务功能：
 * 1. 创建 data_requirement 记录，作为一次数据准备批次的业务入口。
 * 2. 查询指定任务和场景的数据需求批次，支撑后台和后续准备任务编排。
 *
 * 关键流程：
 * 1. 校验创建数据需求批次所需的最小字段。
 * 2. 补齐状态、计数、审计时间、软删除和乐观锁默认值。
 * 3. 查询时统一追加租户和软删除条件，避免跨租户读取数据需求。
 */
@Service
@Profile("!test")
public class DataRequirementServiceImpl implements DataRequirementService {

    private final DataRequirementMapper dataRequirementMapper;

    private final BusinessModuleMapper businessModuleMapper;

    private final ModuleDataStrategyService moduleDataStrategyService;

    private final TeachingDataTemplateService teachingDataTemplateService;

    public DataRequirementServiceImpl(DataRequirementMapper dataRequirementMapper,
                                      BusinessModuleMapper businessModuleMapper,
                                      ModuleDataStrategyService moduleDataStrategyService,
                                      TeachingDataTemplateService teachingDataTemplateService) {
        this.dataRequirementMapper = dataRequirementMapper;
        this.businessModuleMapper = businessModuleMapper;
        this.moduleDataStrategyService = moduleDataStrategyService;
        this.teachingDataTemplateService = teachingDataTemplateService;
    }

    /**
     * 创建数据需求批次。
     *
     * @param requirement 数据需求批次实体。
     * @return 已保存的数据需求批次。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataRequirement createDataRequirement(DataRequirement requirement) {
        validateCreateFields(requirement);
        bindBusinessModuleAndStrategy(requirement);
        fillCreateDefaults(requirement);
        dataRequirementMapper.insert(requirement);
        return requirement;
    }

    /**
     * 查询指定任务和场景下的数据需求批次。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param sceneType 教学场景。
     * @return 数据需求批次列表。
     */
    @Override
    public List<DataRequirement> listByTaskAndScene(String tenantId, String taskId, String sceneType) {
        requireText(tenantId);
        requireText(taskId);
        requireText(sceneType);
        return dataRequirementMapper.selectList(new QueryWrapper<DataRequirement>()
                .eq("tenant_id", tenantId)
                .eq("task_id", taskId)
                .eq("scene_type", sceneType)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 冻结数据准备批次策略快照。
     *
     * @param tenantId 租户 ID。
     * @param requirementId 数据准备批次 ID。
     * @param policySnapshotJson 批次策略快照 JSON。
     * @return 已冻结或原样返回的数据准备批次。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataRequirement freezeRequirementPolicySnapshot(String tenantId,
                                                           String requirementId,
                                                           String policySnapshotJson) {
        requireText(tenantId);
        requireText(requirementId);
        DataRequirement requirement = dataRequirementMapper.selectById(requirementId);
        if (requirement == null
                || Boolean.TRUE.equals(requirement.getDeleted())
                || !tenantId.equals(requirement.getTenantId())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        if (!StringUtils.hasText(policySnapshotJson)
                || StringUtils.hasText(requirement.getRequirementPolicyJson())) {
            return requirement;
        }
        requirement.setRequirementPolicyJson(policySnapshotJson);
        requirement.setUpdateTime(LocalDateTime.now());
        dataRequirementMapper.updateById(requirement);
        return requirement;
    }

    /**
     * 校验创建数据需求批次所需的最小字段。
     *
     * @param requirement 数据需求批次实体。
     */
    private void validateCreateFields(DataRequirement requirement) {
        if (requirement == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(requirement.getId());
        requireText(requirement.getTenantId());
        requireText(requirement.getRequirementCode());
        requireText(requirement.getConnectorSystemId());
        requireText(requirement.getBusinessModuleId());
        requireText(requirement.getSceneType());
    }

    /**
     * 按业务模块和场景解析启用策略，避免前端手填模块编码导致批次落到错误业务边界。
     *
     * @param requirement 数据需求批次。
     */
    private void bindBusinessModuleAndStrategy(DataRequirement requirement) {
        BusinessModule businessModule = businessModuleMapper.selectOne(new QueryWrapper<BusinessModule>()
                .eq("id", requirement.getBusinessModuleId())
                .eq("tenant_id", requirement.getTenantId())
                .eq("connector_system_id", requirement.getConnectorSystemId())
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE));
        if (businessModule == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        ModuleDataStrategy strategy = moduleDataStrategyService.getActiveStrategyByModuleCodeAndScene(
                requirement.getTenantId(),
                requirement.getConnectorSystemId(),
                businessModule.getModuleCode(),
                requirement.getSceneType());
        if (!businessModule.getId().equals(strategy.getBusinessModuleId())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        List<TeachingDataTemplate> activeTemplates = teachingDataTemplateService
                .listActiveTemplatesByModuleAndScene(
                        requirement.getTenantId(),
                        requirement.getConnectorSystemId(),
                        businessModule.getModuleCode(),
                        requirement.getSceneType());
        TeachingDataTemplate effectiveTemplate = activeTemplates.stream()
                .filter(template -> template.getId().equals(strategy.getTemplateId()))
                .findFirst()
                .orElseGet(() -> activeTemplates.stream().findFirst()
                        .orElseThrow(() -> new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE)));
        requirement.setModuleCode(businessModule.getModuleCode());
        requirement.setStrategyId(strategy.getId());
        requirement.setTemplateId(effectiveTemplate.getId());
        requirement.setRequirementPolicyJson(strategy.getValidationPolicyJson());
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
     * 补齐创建时默认字段。
     *
     * @param requirement 数据需求批次实体。
     */
    private void fillCreateDefaults(DataRequirement requirement) {
        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(requirement.getRequirementStatus())) {
            requirement.setRequirementStatus(RequirementStatus.CREATED.getValue());
        }
        if (requirement.getExpectedCount() == null) {
            requirement.setExpectedCount(0L);
        }
        if (requirement.getSuccessCount() == null) {
            requirement.setSuccessCount(0L);
        }
        if (requirement.getFailedCount() == null) {
            requirement.setFailedCount(0L);
        }
        if (requirement.getLockVersion() == null) {
            requirement.setLockVersion(0L);
        }
        if (requirement.getCreateTime() == null) {
            requirement.setCreateTime(now);
        }
        if (requirement.getUpdateTime() == null) {
            requirement.setUpdateTime(now);
        }
        if (!StringUtils.hasText(requirement.getStatus())) {
            requirement.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (requirement.getDeleted() == null) {
            requirement.setDeleted(Boolean.FALSE);
        }
    }
}
