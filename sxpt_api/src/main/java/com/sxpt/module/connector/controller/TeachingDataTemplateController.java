package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.CreateTeachingDataTemplateRequest;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.service.TeachingDataTemplateService;
import com.sxpt.module.connector.vo.TeachingDataTemplateVO;
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
 * 教学业务数据模板接口。
 *
 * 业务功能：
 * 1. 提供原平台教学数据模板的创建入口。
 * 2. 提供按原平台、教学点和场景查询模板的入口，支撑后续教学数据实例创建。
 *
 * 关键流程：
 * 1. 接收创建请求并触发 Bean Validation。
 * 2. 将 DTO 转换为 TeachingDataTemplate 实体，并生成应用层主键。
 * 3. 调用 Service 完成模板写入或查询。
 * 4. 将实体转换为 VO，避免前端依赖数据库 Entity。
 */
@RestController
@RequestMapping("/api/v1/connector/data-templates")
@ConditionalOnProperty(name = "sxpt.connector.data-template-controller.enabled", havingValue = "true", matchIfMissing = true)
public class TeachingDataTemplateController {

    private final TeachingDataTemplateService teachingDataTemplateService;

    public TeachingDataTemplateController(TeachingDataTemplateService teachingDataTemplateService) {
        this.teachingDataTemplateService = teachingDataTemplateService;
    }

    /**
     * 创建教学业务数据模板。
     *
     * @param request 创建教学业务数据模板请求。
     * @return 已创建的教学业务数据模板。
     */
    @PostMapping("/create")
    public ApiResult<TeachingDataTemplateVO> create(@Valid @RequestBody CreateTeachingDataTemplateRequest request) {
        TeachingDataTemplate saved = teachingDataTemplateService.createTeachingDataTemplate(toEntity(request));
        return ApiResult.success(toVO(saved));
    }

    /**
     * 查询指定原平台下的教学业务数据模板。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @return 教学业务数据模板列表。
     */
    @GetMapping
    public ApiResult<List<TeachingDataTemplateVO>> listByConnector(@RequestParam String tenantId,
                                                                   @RequestParam String connectorSystemId) {
        return ApiResult.success(toVOList(teachingDataTemplateService.listTemplatesByConnector(tenantId, connectorSystemId)));
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
    @GetMapping("/active")
    public ApiResult<List<TeachingDataTemplateVO>> listActive(@RequestParam String tenantId,
                                                             @RequestParam String connectorSystemId,
                                                             @RequestParam String teachingPointId,
                                                             @RequestParam String sceneType) {
        return ApiResult.success(toVOList(teachingDataTemplateService
                .listActiveTemplatesByTeachingPointAndScene(tenantId, connectorSystemId, teachingPointId, sceneType)));
    }

    /**
     * 将创建请求转换为数据库实体。
     *
     * @param request 创建教学业务数据模板请求。
     * @return 教学业务数据模板实体。
     */
    private TeachingDataTemplate toEntity(CreateTeachingDataTemplateRequest request) {
        TeachingDataTemplate template = new TeachingDataTemplate();
        template.setId(generateId());
        template.setTenantId(request.getTenantId());
        template.setConnectorSystemId(request.getConnectorSystemId());
        template.setTeachingPointId(request.getTeachingPointId());
        template.setTemplateCode(request.getTemplateCode());
        template.setTemplateName(request.getTemplateName());
        template.setSceneType(request.getSceneType());
        template.setInitState(request.getInitState());
        template.setSupportMode(request.getSupportMode());
        template.setConfigJson(request.getConfigJson());
        return template;
    }

    /**
     * 将实体列表转换为前端展示对象列表。
     *
     * @param templates 教学业务数据模板实体列表。
     * @return 教学业务数据模板展示对象列表。
     */
    private List<TeachingDataTemplateVO> toVOList(List<TeachingDataTemplate> templates) {
        List<TeachingDataTemplateVO> result = new ArrayList<>();
        for (TeachingDataTemplate template : templates) {
            result.add(toVO(template));
        }
        return result;
    }

    /**
     * 将实体转换为前端展示对象。
     *
     * @param template 教学业务数据模板实体。
     * @return 教学业务数据模板展示对象。
     */
    private TeachingDataTemplateVO toVO(TeachingDataTemplate template) {
        TeachingDataTemplateVO vo = new TeachingDataTemplateVO();
        vo.setId(template.getId());
        vo.setTenantId(template.getTenantId());
        vo.setConnectorSystemId(template.getConnectorSystemId());
        vo.setTeachingPointId(template.getTeachingPointId());
        vo.setTemplateCode(template.getTemplateCode());
        vo.setTemplateName(template.getTemplateName());
        vo.setSceneType(template.getSceneType());
        vo.setInitState(template.getInitState());
        vo.setSupportMode(template.getSupportMode());
        vo.setConfigJson(template.getConfigJson());
        vo.setStatus(template.getStatus());
        vo.setCreateTime(template.getCreateTime());
        vo.setUpdateTime(template.getUpdateTime());
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
