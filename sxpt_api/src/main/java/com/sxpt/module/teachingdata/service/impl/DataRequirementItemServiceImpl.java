package com.sxpt.module.teachingdata.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.teachingdata.entity.DataRequirementItem;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RequirementItemStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.ValidationStatus;
import com.sxpt.module.teachingdata.mapper.DataRequirementItemMapper;
import com.sxpt.module.teachingdata.service.DataRequirementItemService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据需求明细服务实现。
 *
 * 业务功能：
 * 1. 创建 data_requirement_item 记录，保存原平台数据准备的逐条约束。
 * 2. 提供按批次、按学生场景查询能力，支撑发布预生成、学生练习重置和考试按需生成。
 *
 * 关键流程：
 * 1. 创建时校验租户、批次、请求幂等键、原平台、业务模块、场景、学生和单位角色。
 * 2. 补齐明细状态、校验状态、审计时间和软删除默认值。
 * 3. 查询时统一追加租户和软删除条件，避免跨租户读取明细。
 */
@Service
@Profile("!test")
public class DataRequirementItemServiceImpl implements DataRequirementItemService {

    private final DataRequirementItemMapper dataRequirementItemMapper;

    public DataRequirementItemServiceImpl(DataRequirementItemMapper dataRequirementItemMapper) {
        this.dataRequirementItemMapper = dataRequirementItemMapper;
    }

    /**
     * 创建数据需求明细。
     *
     * @param item 数据需求明细实体。
     * @return 已保存的数据需求明细。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataRequirementItem createDataRequirementItem(DataRequirementItem item) {
        validateCreateFields(item);
        fillCreateDefaults(item);
        dataRequirementItemMapper.insert(item);
        return item;
    }

    /**
     * 查询指定需求批次下的数据需求明细。
     *
     * @param tenantId 租户 ID。
     * @param requirementId 数据需求批次 ID。
     * @return 数据需求明细列表。
     */
    @Override
    public List<DataRequirementItem> listByRequirement(String tenantId, String requirementId) {
        requireText(tenantId);
        requireText(requirementId);
        return dataRequirementItemMapper.selectList(new QueryWrapper<DataRequirementItem>()
                .eq("tenant_id", tenantId)
                .eq("requirement_id", requirementId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("student_id")
                .orderByAsc("question_id")
                .orderByAsc("segment_no")
                .orderByAsc("create_time"));
    }

    /**
     * 查询指定学生在指定任务和场景下的数据需求明细。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param sceneType 教学场景。
     * @param studentId 学生 ID。
     * @return 数据需求明细列表。
     */
    @Override
    public List<DataRequirementItem> listByStudentAndScene(String tenantId,
                                                           String taskId,
                                                           String sceneType,
                                                           String studentId) {
        requireText(tenantId);
        requireText(taskId);
        requireText(sceneType);
        requireText(studentId);
        return dataRequirementItemMapper.selectList(new QueryWrapper<DataRequirementItem>()
                .eq("tenant_id", tenantId)
                .eq("task_id", taskId)
                .eq("scene_type", sceneType)
                .eq("student_id", studentId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("question_id")
                .orderByAsc("segment_no")
                .orderByAsc("create_time"));
    }

    /**
     * 校验创建数据需求明细所需的最小字段。
     *
     * @param item 数据需求明细实体。
     */
    private void validateCreateFields(DataRequirementItem item) {
        if (item == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(item.getId());
        requireText(item.getTenantId());
        requireText(item.getRequirementId());
        requireText(item.getRequestBatchId());
        requireText(item.getRequestItemId());
        requireText(item.getConnectorSystemId());
        requireText(item.getModuleCode());
        requireText(item.getSceneType());
        requireText(item.getTaskId());
        requireText(item.getStudentId());
        requireText(item.getActorType());
        requireText(item.getOwnerExternalOrgId());
        requireText(item.getRequiredExternalOrgId());
        requireText(item.getRequiredExternalRoleId());
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
     * @param item 数据需求明细实体。
     */
    private void fillCreateDefaults(DataRequirementItem item) {
        LocalDateTime now = LocalDateTime.now();
        if (!StringUtils.hasText(item.getItemStatus())) {
            item.setItemStatus(RequirementItemStatus.CREATED.getValue());
        }
        if (!StringUtils.hasText(item.getValidationStatus())) {
            item.setValidationStatus(ValidationStatus.NOT_CHECKED.getValue());
        }
        if (item.getCreateTime() == null) {
            item.setCreateTime(now);
        }
        if (item.getUpdateTime() == null) {
            item.setUpdateTime(now);
        }
        if (!StringUtils.hasText(item.getStatus())) {
            item.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (item.getDeleted() == null) {
            item.setDeleted(Boolean.FALSE);
        }
    }
}
