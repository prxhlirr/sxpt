package com.sxpt.module.teachingdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.ModuleDataStrategy;
import com.sxpt.module.connector.service.ModuleDataStrategyService;
import com.sxpt.module.teachingdata.entity.DataRequirement;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RequirementItemStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.ValidationStatus;
import com.sxpt.module.teachingdata.mapper.DataRequirementItemMapper;
import com.sxpt.module.teachingdata.mapper.DataRequirementMapper;
import com.sxpt.module.teachingdata.service.DataRequirementGenerationService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 数据需求明细生成服务实现。
 *
 * 业务功能：
 * 1. 将一次数据需求批次展开为多条可提交原平台的数据需求明细。
 * 2. 固化每个学生对应的单位、角色、题目、动作和评分点快照，保证后续重置和考试绑定有稳定依据。
 *
 * 关键流程：
 * 1. 读取未删除的数据需求批次。
 * 2. 校验参与者必须携带学生、单位、角色和参与身份。
 * 3. 批量插入需求明细，并回写批次 expectedCount。
 */
@Service
@Profile("!test")
public class DataRequirementGenerationServiceImpl implements DataRequirementGenerationService {

    private final DataRequirementMapper dataRequirementMapper;

    private final DataRequirementItemMapper dataRequirementItemMapper;

    private final ModuleDataStrategyService moduleDataStrategyService;

    public DataRequirementGenerationServiceImpl(DataRequirementMapper dataRequirementMapper,
                                                DataRequirementItemMapper dataRequirementItemMapper,
                                                ModuleDataStrategyService moduleDataStrategyService) {
        this.dataRequirementMapper = dataRequirementMapper;
        this.dataRequirementItemMapper = dataRequirementItemMapper;
        this.moduleDataStrategyService = moduleDataStrategyService;
    }

    /**
     * 按参与者约束生成数据需求明细。
     *
     * @param request 生成请求。
     * @return 生成结果。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public GenerateResult generateRequirementItems(GenerateRequest request) {
        validateRequest(request);
        DataRequirement requirement = getExistingRequirement(request.getRequirementId());
        List<DataRequirementItem> existingItems = listExistingItems(
                request.getRequirementId(), request.getRequestBatchId());
        if (!existingItems.isEmpty()) {
            return new GenerateResult(requirement, existingItems);
        }
        ModuleDataStrategy strategy = getRequirementStrategy(requirement);
        List<DataRequirementItem> items = new ArrayList<>();
        for (ParticipantRequirement participant : request.getParticipants()) {
            validateParticipant(participant);
            DataRequirementItem item = buildItem(requirement, strategy, request, participant);
            dataRequirementItemMapper.insert(item);
            items.add(item);
        }
        requirement.setExpectedCount((long) items.size());
        requirement.setUpdateTime(LocalDateTime.now());
        dataRequirementMapper.updateById(requirement);
        return new GenerateResult(requirement, items);
    }

    /**
     * 查询同一批次已经生成的需求明细，避免同一次 attempt 的重试重复插入明细。
     *
     * @param requirementId 数据需求批次 ID。
     * @param requestBatchId 请求批次 ID，调用方应绑定具体 attempt 或 resetNo。
     * @return 已存在的需求明细列表。
     */
    private List<DataRequirementItem> listExistingItems(String requirementId, String requestBatchId) {
        return dataRequirementItemMapper.selectList(new QueryWrapper<DataRequirementItem>()
                .eq("requirement_id", requirementId)
                .eq("request_batch_id", requestBatchId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("create_time"));
    }

    /**
     * 读取未软删除的数据需求批次。
     *
     * @param requirementId 数据需求批次 ID。
     * @return 数据需求批次。
     */
    private DataRequirement getExistingRequirement(String requirementId) {
        DataRequirement requirement = dataRequirementMapper.selectById(requirementId);
        if (requirement == null || Boolean.TRUE.equals(requirement.getDeleted())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return requirement;
    }

    /**
     * 读取批次绑定的策略，确保明细生成使用创建批次时确定的业务规则。
     *
     * @param requirement 数据需求批次。
     * @return 数据准备策略。
     */
    private ModuleDataStrategy getRequirementStrategy(DataRequirement requirement) {
        requireText(requirement.getStrategyId());
        ModuleDataStrategy strategy = moduleDataStrategyService.getModuleDataStrategyById(requirement.getStrategyId());
        if (!requirement.getTenantId().equals(strategy.getTenantId())
                || !requirement.getConnectorSystemId().equals(strategy.getConnectorSystemId())
                || !requirement.getBusinessModuleId().equals(strategy.getBusinessModuleId())
                || !requirement.getModuleCode().equals(strategy.getModuleCode())
                || !requirement.getSceneType().equals(strategy.getSceneType())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        return strategy;
    }

    /**
     * 校验生成请求的最小字段。
     *
     * @param request 生成请求。
     */
    private void validateRequest(GenerateRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getRequirementId());
        requireText(request.getRequestBatchId());
        requireText(request.getCreateBy());
        requireText(request.getUpdateBy());
        if (request.getParticipants() == null || request.getParticipants().isEmpty()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 校验单个参与者约束，确保原平台能按单位和角色创建可操作数据。
     *
     * @param participant 参与者约束。
     */
    private void validateParticipant(ParticipantRequirement participant) {
        if (participant == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(participant.getStudentId());
        requireText(participant.getActorType());
        requireText(participant.getOwnerExternalOrgId());
        requireText(participant.getRequiredExternalOrgId());
        requireText(participant.getRequiredExternalRoleId());
    }

    /**
     * 构造单条数据需求明细。
     *
     * @param requirement 数据需求批次。
     * @param request 生成请求。
     * @param participant 参与者约束。
     * @return 数据需求明细。
     */
    private DataRequirementItem buildItem(DataRequirement requirement,
                                          ModuleDataStrategy strategy,
                                          GenerateRequest request,
                                          ParticipantRequirement participant) {
        LocalDateTime now = LocalDateTime.now();
        DataRequirementItem item = new DataRequirementItem();
        item.setId(generateId());
        item.setTenantId(requirement.getTenantId());
        item.setRequirementId(requirement.getId());
        item.setRequestBatchId(request.getRequestBatchId());
        item.setRequestItemId(buildRequestItemId(request.getRequestBatchId()));
        item.setConnectorSystemId(requirement.getConnectorSystemId());
        item.setBusinessModuleId(requirement.getBusinessModuleId());
        item.setModuleCode(requirement.getModuleCode());
        item.setTemplateId(requirement.getTemplateId());
        item.setSceneType(requirement.getSceneType());
        item.setTaskId(requirement.getTaskId());
        item.setStudentId(participant.getStudentId());
        item.setQuestionId(participant.getQuestionId());
        item.setExamAttemptId(participant.getExamAttemptId());
        item.setQuestionAttemptId(participant.getQuestionAttemptId());
        item.setCollaborationUnitId(participant.getCollaborationUnitId());
        item.setSegmentNo(participant.getSegmentNo());
        item.setActorType(participant.getActorType());
        item.setOwnerExternalOrgId(participant.getOwnerExternalOrgId());
        item.setOwnerExternalOrgName(participant.getOwnerExternalOrgName());
        item.setRequiredExternalOrgId(participant.getRequiredExternalOrgId());
        item.setRequiredExternalOrgName(participant.getRequiredExternalOrgName());
        item.setRequiredExternalRoleId(participant.getRequiredExternalRoleId());
        item.setRequiredExternalRoleName(participant.getRequiredExternalRoleName());
        item.setInitExternalStatus(strategy.getInitExternalStatus());
        item.setTargetExternalStatus(strategy.getTargetExternalStatus());
        item.setDataScopeJson(participant.getDataScopeJson());
        item.setRequiredActionsJson(participant.getRequiredActionsJson());
        item.setValidationPolicyJson(strategy.getValidationPolicyJson());
        item.setScorePointSnapshotJson(participant.getScorePointSnapshotJson());
        item.setItemStatus(RequirementItemStatus.CREATED.getValue());
        item.setValidationStatus(ValidationStatus.NOT_CHECKED.getValue());
        item.setCreateBy(request.getCreateBy());
        item.setUpdateBy(request.getUpdateBy());
        item.setCreateTime(now);
        item.setUpdateTime(now);
        item.setStatus(RecordStatus.ACTIVE.getValue());
        item.setDeleted(Boolean.FALSE);
        return item;
    }

    /**
     * 生成明细请求 ID，保证后续原平台逐条回填可对账。
     *
     * @param requestBatchId 批量请求 ID。
     * @return 明细请求 ID。
     */
    private String buildRequestItemId(String requestBatchId) {
        return requestBatchId + "_" + generateId();
    }

    /**
     * 生成应用层主键。
     *
     * @return 无横线 UUID。
     */
    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
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
