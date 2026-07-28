package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.mapper.TeachingDataTemplateMapper;
import com.sxpt.module.connector.service.TeachingDataTemplateService;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 教学业务数据模板服务实现。
 *
 * 业务功能：
 * 1. 创建 teaching_data_template 记录，为备案、学习、练习、考试准备原平台数据模板。
 * 2. 按原平台、教学点和场景查询可用模板，供后续教学数据实例创建使用。
 *
 * 关键流程：
 * 1. 校验模板创建所需最小字段。
 * 2. 补齐生命周期默认字段。
 * 3. 查询时统一追加租户、原平台和软删除条件，避免跨租户读取模板。
 */
@Service
@Profile("!test")
public class TeachingDataTemplateServiceImpl implements TeachingDataTemplateService {

    private final TeachingDataTemplateMapper teachingDataTemplateMapper;

    public TeachingDataTemplateServiceImpl(TeachingDataTemplateMapper teachingDataTemplateMapper) {
        this.teachingDataTemplateMapper = teachingDataTemplateMapper;
    }

    /**
     * 创建教学业务数据模板。
     *
     * @param template 教学业务数据模板实体。
     * @return 已保存的教学业务数据模板。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingDataTemplate createTeachingDataTemplate(TeachingDataTemplate template) {
        validateCreateFields(template);
        fillCreateDefaults(template);
        teachingDataTemplateMapper.insert(template);
        return template;
    }

    /**
     * 更新教学业务数据模板。
     *
     * @param template 教学业务数据模板实体，必须包含 ID 和可编辑模板字段。
     * @return 已更新的教学业务数据模板。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingDataTemplate updateTeachingDataTemplate(TeachingDataTemplate template) {
        validateUpdateFields(template);
        TeachingDataTemplate existing = getTeachingDataTemplateById(template.getId());
        existing.setTeachingPointId(template.getTeachingPointId());
        existing.setTemplateName(template.getTemplateName());
        existing.setSceneType(template.getSceneType());
        existing.setModuleCode(template.getModuleCode());
        existing.setStrategyId(template.getStrategyId());
        existing.setInitState(template.getInitState());
        existing.setSupportMode(template.getSupportMode());
        existing.setConfigJson(template.getConfigJson());
        existing.setDataSchemaJson(template.getDataSchemaJson());
        existing.setMockRuleJson(template.getMockRuleJson());
        existing.setReadonlyFlag(template.getReadonlyFlag());
        existing.setRequestSchemaJson(template.getRequestSchemaJson());
        existing.setRequiredOrgRoleJson(template.getRequiredOrgRoleJson());
        existing.setResultCheckSchemaJson(template.getResultCheckSchemaJson());
        existing.setSensitiveFieldPolicyJson(template.getSensitiveFieldPolicyJson());
        existing.setUpdateBy(template.getUpdateBy());
        existing.setUpdateTime(LocalDateTime.now());
        teachingDataTemplateMapper.updateById(existing);
        return existing;
    }

    /**
     * 启用教学业务数据模板。
     *
     * @param id 模板 ID。
     * @return 已启用的教学业务数据模板。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingDataTemplate enableTeachingDataTemplate(String id) {
        return changeStatus(id, RecordStatus.ACTIVE.getValue());
    }

    /**
     * 停用教学业务数据模板。
     *
     * @param id 模板 ID。
     * @return 已停用的教学业务数据模板。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingDataTemplate disableTeachingDataTemplate(String id) {
        return changeStatus(id, RecordStatus.DISABLED.getValue());
    }

    /**
     * 查询教学业务数据模板详情。
     *
     * @param id 模板 ID。
     * @return 未删除的教学业务数据模板。
     */
    @Override
    public TeachingDataTemplate getTeachingDataTemplateById(String id) {
        requireText(id);
        TeachingDataTemplate template = teachingDataTemplateMapper.selectOne(new QueryWrapper<TeachingDataTemplate>()
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (template == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return template;
    }

    /**
     * 查询指定租户和原平台下的教学业务数据模板。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @return 教学业务数据模板列表。
     */
    @Override
    public List<TeachingDataTemplate> listTemplatesByConnector(String tenantId, String connectorSystemId) {
        requireText(tenantId);
        requireText(connectorSystemId);
        return teachingDataTemplateMapper.selectList(new QueryWrapper<TeachingDataTemplate>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 查询指定平台、模块和场景下的模板，保证运行时模板不会跨模块复用。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param moduleCode 业务模块编码。
     * @param sceneType 教学场景。
     * @return 模板列表。
     */
    @Override
    public List<TeachingDataTemplate> listTemplatesByModuleAndScene(String tenantId,
                                                                    String connectorSystemId,
                                                                    String moduleCode,
                                                                    String sceneType) {
        requireText(tenantId);
        requireText(connectorSystemId);
        requireText(moduleCode);
        requireText(sceneType);
        return teachingDataTemplateMapper.selectList(new QueryWrapper<TeachingDataTemplate>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("module_code", moduleCode)
                .eq("scene_type", sceneType)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 查询指定教学点和场景下的可用教学业务数据模板。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @param teachingPointId 教学点 ID。
     * @param sceneType 场景类型。
     * @return 可用教学业务数据模板列表。
     */
    @Override
    public List<TeachingDataTemplate> listActiveTemplatesByTeachingPointAndScene(String tenantId,
                                                                                  String connectorSystemId,
                                                                                  String teachingPointId,
                                                                                  String sceneType) {
        requireText(tenantId);
        requireText(connectorSystemId);
        requireText(teachingPointId);
        requireText(sceneType);
        return teachingDataTemplateMapper.selectList(new QueryWrapper<TeachingDataTemplate>()
                .eq("tenant_id", tenantId)
                .eq("connector_system_id", connectorSystemId)
                .eq("teaching_point_id", teachingPointId)
                .eq("scene_type", sceneType)
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    /**
     * 校验创建模板所需的最小字段。
     *
     * @param template 教学业务数据模板实体。
     */
    private void validateCreateFields(TeachingDataTemplate template) {
        if (template == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(template.getId());
        requireText(template.getTenantId());
        requireText(template.getConnectorSystemId());
        requireText(template.getTemplateCode());
        requireText(template.getTemplateName());
        requireText(template.getSceneType());
        requireText(template.getModuleCode());
    }

    /**
     * 校验更新模板所需的最小字段。
     *
     * @param template 教学业务数据模板实体。
     */
    private void validateUpdateFields(TeachingDataTemplate template) {
        if (template == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(template.getId());
        requireText(template.getTemplateName());
        requireText(template.getSceneType());
        requireText(template.getModuleCode());
    }

    /**
     * 切换教学业务数据模板状态。
     *
     * @param id 模板 ID。
     * @param status 目标状态。
     * @return 已更新状态的模板。
     */
    private TeachingDataTemplate changeStatus(String id, String status) {
        TeachingDataTemplate existing = getTeachingDataTemplateById(id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        teachingDataTemplateMapper.updateById(existing);
        return existing;
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
     * @param template 教学业务数据模板实体。
     */
    private void fillCreateDefaults(TeachingDataTemplate template) {
        LocalDateTime now = LocalDateTime.now();
        if (template.getCreateTime() == null) {
            template.setCreateTime(now);
        }
        if (template.getUpdateTime() == null) {
            template.setUpdateTime(now);
        }
        if (!StringUtils.hasText(template.getStatus())) {
            template.setStatus(RecordStatus.ACTIVE.getValue());
        }
        if (template.getDeleted() == null) {
            template.setDeleted(Boolean.FALSE);
        }
    }
}

