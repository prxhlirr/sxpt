package com.sxpt.module.connector.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResult;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.ExternalConnectorContext;
import com.sxpt.module.connector.dto.ClassicCaseImportRequest;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.entity.BusinessModuleProcessActor;
import com.sxpt.module.connector.entity.BusinessModuleProcessStep;
import com.sxpt.module.connector.entity.ClassicCaseAsset;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.mapper.BusinessModuleMapper;
import com.sxpt.module.connector.mapper.BusinessModuleProcessActorMapper;
import com.sxpt.module.connector.mapper.BusinessModuleProcessStepMapper;
import com.sxpt.module.connector.service.ClassicCaseService;
import com.sxpt.module.connector.service.ExternalConnectorCredentialService.AuthenticatedExternalConnector;
import com.sxpt.module.connector.vo.ClassicCaseAssetVO;
import com.sxpt.module.connector.vo.ExternalClassicCaseMetadataVO;
import com.sxpt.module.connector.vo.ExternalClassicCaseMetadataVO.ActorVO;
import com.sxpt.module.connector.vo.ExternalClassicCaseMetadataVO.ModuleVO;
import com.sxpt.module.connector.vo.ExternalClassicCaseMetadataVO.ProcessStepVO;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 第三方原平台经典案例外部接口。
 *
 * 业务功能：
 * 1. 为原平台正式环境提供只读元数据接口，避免原平台硬编码教学平台模块和流程节点。
 * 2. 为原平台正式环境提供 API Key 保护的经典案例导入入口。
 *
 * 关键流程：
 * 1. 外部 API Key 拦截器先认证第三方系统并写入 ExternalConnectorContext。
 * 2. Controller 只信任认证上下文中的正式环境和学习环境，再校验请求体边界。
 * 3. 导入逻辑复用 ClassicCaseService，避免第三方入口与后台入口出现两套版本规则。
 */
@RestController
@RequestMapping("/api/v1/external/classic-cases")
@ConditionalOnProperty(name = "sxpt.connector.external-classic-case-controller.enabled", havingValue = "true", matchIfMissing = true)
public class ExternalClassicCaseController {

    private final ClassicCaseService classicCaseService;

    private final BusinessModuleMapper businessModuleMapper;

    private final BusinessModuleProcessStepMapper processStepMapper;

    private final BusinessModuleProcessActorMapper processActorMapper;

    public ExternalClassicCaseController(ClassicCaseService classicCaseService,
                                         BusinessModuleMapper businessModuleMapper,
                                         BusinessModuleProcessStepMapper processStepMapper,
                                         BusinessModuleProcessActorMapper processActorMapper) {
        this.classicCaseService = classicCaseService;
        this.businessModuleMapper = businessModuleMapper;
        this.processStepMapper = processStepMapper;
        this.processActorMapper = processActorMapper;
    }

    /**
     * 查询第三方原平台创建经典案例所需的教学平台元数据。
     *
     * @param moduleCode 业务模块编码，可选。
     * @return 可绑定的学习环境、业务模块、流程节点和参与方。
     */
    @GetMapping("/metadata")
    public ApiResult<ExternalClassicCaseMetadataVO> metadata(@RequestParam(required = false) String moduleCode) {
        AuthenticatedExternalConnector connector = ExternalConnectorContext.require();
        ConnectorSystem sourceSystem = connector.getSourceSystem();
        ConnectorSystem learningSystem = connector.getLearningSystem();

        ExternalClassicCaseMetadataVO metadata = new ExternalClassicCaseMetadataVO();
        metadata.setTenantId(sourceSystem.getTenantId());
        metadata.setSourceConnectorSystemId(sourceSystem.getId());
        metadata.setSourceSystemName(sourceSystem.getSystemName());
        metadata.setLearningConnectorSystemId(learningSystem.getId());
        metadata.setLearningSystemName(learningSystem.getSystemName());
        metadata.setEnvironmentGroupCode(sourceSystem.getEnvironmentGroupCode());
        metadata.setModules(toModuleVOList(listActiveModules(learningSystem, moduleCode)));
        return ApiResult.success(metadata);
    }

    /**
     * 接收原平台正式环境推送的经典案例脱敏内容。
     *
     * @param request 经典案例导入请求。
     * @return 已创建或更新的经典案例资产摘要。
     */
    @PostMapping("/import")
    public ApiResult<ClassicCaseAssetVO> importClassicCase(@Valid @RequestBody ClassicCaseImportRequest request) {
        validateRequestBoundary(ExternalConnectorContext.require(), request);
        ClassicCaseAsset asset = classicCaseService.importClassicCase(request);
        return ApiResult.success(toVO(asset));
    }

    private List<BusinessModule> listActiveModules(ConnectorSystem learningSystem, String moduleCode) {
        QueryWrapper<BusinessModule> query = new QueryWrapper<BusinessModule>()
                .eq("tenant_id", learningSystem.getTenantId())
                .eq("connector_system_id", learningSystem.getId())
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("module_code");
        if (StringUtils.hasText(moduleCode)) {
            query.eq("module_code", moduleCode.trim());
        }
        return businessModuleMapper.selectList(query);
    }

    private List<ModuleVO> toModuleVOList(List<BusinessModule> modules) {
        List<ModuleVO> result = new ArrayList<>();
        for (BusinessModule module : modules) {
            ModuleVO vo = new ModuleVO();
            vo.setBusinessModuleId(module.getId());
            vo.setModuleCode(module.getModuleCode());
            vo.setModuleName(module.getModuleName());
            vo.setEntryUrl(module.getEntryUrl());
            vo.setSupportScenes(module.getSupportScenes());
            vo.setProcessSteps(toProcessStepVOList(module, listActiveSteps(module)));
            result.add(vo);
        }
        return result;
    }

    private List<BusinessModuleProcessStep> listActiveSteps(BusinessModule module) {
        return processStepMapper.selectList(new QueryWrapper<BusinessModuleProcessStep>()
                .eq("tenant_id", module.getTenantId())
                .eq("business_module_id", module.getId())
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("step_no")
                .orderByAsc("step_code"));
    }

    private List<ProcessStepVO> toProcessStepVOList(BusinessModule module, List<BusinessModuleProcessStep> steps) {
        List<ProcessStepVO> result = new ArrayList<>();
        for (BusinessModuleProcessStep step : steps) {
            ProcessStepVO vo = new ProcessStepVO();
            vo.setTeachingPointId(step.getId());
            vo.setStepCode(step.getStepCode());
            vo.setStepName(step.getStepName());
            vo.setStepNo(step.getStepNo());
            vo.setInitExternalStatus(step.getInitExternalStatus());
            vo.setTargetExternalStatus(step.getTargetExternalStatus());
            vo.setRequiredActors(toActorVOList(listActiveActors(module, step)));
            result.add(vo);
        }
        return result;
    }

    private List<BusinessModuleProcessActor> listActiveActors(BusinessModule module, BusinessModuleProcessStep step) {
        return processActorMapper.selectList(new QueryWrapper<BusinessModuleProcessActor>()
                .eq("tenant_id", module.getTenantId())
                .eq("business_module_id", module.getId())
                .eq("process_step_id", step.getId())
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("actor_no"));
    }

    private List<ActorVO> toActorVOList(List<BusinessModuleProcessActor> actors) {
        List<ActorVO> result = new ArrayList<>();
        for (BusinessModuleProcessActor actor : actors) {
            ActorVO vo = new ActorVO();
            vo.setPlaceholder(firstText(actor.getActorRelation(), actor.getRequiredRoleCode(), actor.getRequiredOrgCode()));
            vo.setActorType(actor.getActorType());
            vo.setActorRelation(actor.getActorRelation());
            vo.setRequiredOrgCode(actor.getRequiredOrgCode());
            vo.setRequiredOrgName(actor.getRequiredOrgName());
            vo.setRequiredRoleCode(actor.getRequiredRoleCode());
            vo.setRequiredRoleName(actor.getRequiredRoleName());
            vo.setRequired(actor.getIsRequired());
            result.add(vo);
        }
        return result;
    }

    private void validateRequestBoundary(AuthenticatedExternalConnector connector, ClassicCaseImportRequest request) {
        ConnectorSystem sourceSystem = connector.getSourceSystem();
        ConnectorSystem learningSystem = connector.getLearningSystem();
        if (!sourceSystem.getTenantId().equals(request.getTenantId())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        if (!sourceSystem.getId().equals(request.getSourceConnectorSystemId())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        if (!learningSystem.getId().equals(request.getLearningConnectorSystemId())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        BusinessModule module = businessModuleMapper.selectOne(new QueryWrapper<BusinessModule>()
                .eq("id", request.getBusinessModuleId())
                .eq("tenant_id", sourceSystem.getTenantId())
                .eq("connector_system_id", learningSystem.getId())
                .eq("module_code", request.getModuleCode())
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE));
        if (module == null) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        if (StringUtils.hasText(request.getTeachingPointId())) {
            Integer count = processStepMapper.selectCount(new QueryWrapper<BusinessModuleProcessStep>()
                    .eq("id", request.getTeachingPointId())
                    .eq("tenant_id", sourceSystem.getTenantId())
                    .eq("business_module_id", module.getId())
                    .eq("status", RecordStatus.ACTIVE.getValue())
                    .eq("deleted", Boolean.FALSE));
            if (count == null || count == 0) {
                throw new BusinessException(ApiResultCode.FORBIDDEN);
            }
        }
    }

    private ClassicCaseAssetVO toVO(ClassicCaseAsset asset) {
        ClassicCaseAssetVO vo = new ClassicCaseAssetVO();
        vo.setId(asset.getId());
        vo.setTenantId(asset.getTenantId());
        vo.setCaseCode(asset.getCaseCode());
        vo.setCaseTitle(asset.getCaseTitle());
        vo.setCaseSummary(asset.getCaseSummary());
        vo.setSourceConnectorSystemId(asset.getSourceConnectorSystemId());
        vo.setLearningConnectorSystemId(asset.getLearningConnectorSystemId());
        vo.setEnvironmentGroupCode(asset.getEnvironmentGroupCode());
        vo.setBusinessModuleId(asset.getBusinessModuleId());
        vo.setModuleCode(asset.getModuleCode());
        vo.setTeachingPointId(asset.getTeachingPointId());
        vo.setSceneTypesJson(asset.getSceneTypesJson());
        vo.setCurrentVersionId(asset.getCurrentVersionId());
        vo.setStatus(asset.getStatus());
        vo.setCreateTime(asset.getCreateTime());
        vo.setUpdateTime(asset.getUpdateTime());
        return vo;
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
