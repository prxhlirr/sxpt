package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.mapper.TeachingDataTemplateMapper;
import com.sxpt.module.connector.service.TeachingDataTemplateService;
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

    private static final String DEFAULT_STATUS = "ACTIVE";

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
                .eq("status", DEFAULT_STATUS)
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
            template.setStatus(DEFAULT_STATUS);
        }
        if (template.getDeleted() == null) {
            template.setDeleted(Boolean.FALSE);
        }
    }
}

