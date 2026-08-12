package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.dto.ClassicCaseBatchGenerateRequest;
import com.sxpt.module.connector.dto.ClassicCaseGenerateLaunchRequest;
import com.sxpt.module.connector.dto.ClassicCaseGenerateRequest;
import com.sxpt.module.connector.dto.ClassicCaseImportRequest;
import com.sxpt.module.connector.entity.ClassicCaseAsset;
import com.sxpt.module.connector.entity.ClassicCaseUsage;
import com.sxpt.module.connector.entity.ClassicCaseVersion;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.service.ClassicCaseService;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.connector.vo.ClassicCaseAssetVO;
import com.sxpt.module.connector.vo.ClassicCaseBatchGenerateVO;
import com.sxpt.module.connector.vo.ClassicCaseLaunchVO;
import com.sxpt.module.connector.vo.ClassicCaseUsageVO;
import com.sxpt.module.connector.vo.ClassicCaseVersionVO;
import com.sxpt.module.connector.vo.PlatformLaunchContextVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

/**
 * 经典案例接口。
 *
 * 业务功能：
 * 1. 接收原平台正式环境发起的经典案例导入请求。
 * 2. 返回案例资产索引信息，供原平台确认推送结果和后续追踪。
 *
 * 关键流程：
 * 1. Controller 只负责参数校验、调用服务和转换 VO。
 * 2. 正式/学习环境绑定、JSON 格式和版本生成由 ClassicCaseService 统一处理。
 */
@RestController
@RequestMapping("/api/v1/classic-cases")
@ConditionalOnProperty(name = "sxpt.connector.classic-case-controller.enabled", havingValue = "true", matchIfMissing = true)
public class ClassicCaseController {

    private final ClassicCaseService classicCaseService;

    public ClassicCaseController(ClassicCaseService classicCaseService) {
        this.classicCaseService = classicCaseService;
    }

    /**
     * 查询经典案例列表。
     *
     * @param tenantId 租户 ID。
     * @param learningConnectorSystemId 学习环境平台 ID，可选。
     * @param moduleCode 模块编码，可选。
     * @param teachingPointId 教学点 ID，可选。
     * @return 经典案例列表。
     */
    @GetMapping("/list")
    public ApiResult<List<ClassicCaseAssetVO>> listClassicCases(@RequestParam String tenantId,
                                                                @RequestParam(required = false) String learningConnectorSystemId,
                                                                @RequestParam(required = false) String moduleCode,
                                                                @RequestParam(required = false) String teachingPointId) {
        List<ClassicCaseAsset> assets = classicCaseService.listClassicCaseAssets(
                tenantId,
                learningConnectorSystemId,
                moduleCode,
                teachingPointId);
        List<ClassicCaseAssetVO> result = new ArrayList<>();
        for (ClassicCaseAsset asset : assets) {
            result.add(toVO(asset));
        }
        return ApiResult.success(result);
    }

    /**
     * 查询经典案例详情。
     *
     * @param id 经典案例资产 ID。
     * @param tenantId 租户 ID。
     * @return 经典案例详情。
     */
    @GetMapping("/{id}")
    public ApiResult<ClassicCaseAssetVO> getClassicCase(@PathVariable String id,
                                                        @RequestParam String tenantId) {
        return ApiResult.success(toVO(classicCaseService.getClassicCaseAssetDetail(tenantId, id)));
    }

    /**
     * 查询经典案例版本列表。
     *
     * @param id 经典案例资产 ID。
     * @param tenantId 租户 ID。
     * @return 版本列表。
     */
    @GetMapping("/{id}/versions")
    public ApiResult<List<ClassicCaseVersionVO>> listClassicCaseVersions(@PathVariable String id,
                                                                         @RequestParam String tenantId) {
        List<ClassicCaseVersion> versions = classicCaseService.listClassicCaseVersions(tenantId, id);
        List<ClassicCaseVersionVO> result = new ArrayList<>();
        for (ClassicCaseVersion version : versions) {
            result.add(toVO(version));
        }
        return ApiResult.success(result);
    }

    /**
     * 导入经典案例。
     *
     * @param request 原平台正式环境推送的完全脱敏案例内容。
     * @return 已创建或更新的经典案例资产。
     */
    @PostMapping("/import")
    public ApiResult<ClassicCaseAssetVO> importClassicCase(@Valid @RequestBody ClassicCaseImportRequest request) {
        ClassicCaseAsset asset = classicCaseService.importClassicCase(request);
        return ApiResult.success(toVO(asset));
    }

    /**
     * 基于经典案例在学习环境生成业务数据。
     *
     * @param request 经典案例生成请求。
     * @return 经典案例使用记录。
     */
    @PostMapping("/generate")
    public ApiResult<ClassicCaseUsageVO> generateClassicCaseData(@Valid @RequestBody ClassicCaseGenerateRequest request) {
        ClassicCaseUsage usage = classicCaseService.generateClassicCaseData(request);
        return ApiResult.success(toVO(usage));
    }

    /**
     * 批量生成学生经典案例 demo 数据。
     *
     * @param request 批量生成请求。
     * @return 批量使用记录。
     */
    @PostMapping("/batch-generate")
    public ApiResult<ClassicCaseBatchGenerateVO> batchGenerateClassicCaseData(
            @Valid @RequestBody ClassicCaseBatchGenerateRequest request) {
        List<ClassicCaseUsage> usages = classicCaseService.batchGenerateClassicCaseData(request);
        ClassicCaseBatchGenerateVO vo = new ClassicCaseBatchGenerateVO();
        List<ClassicCaseUsageVO> usageVOList = new ArrayList<>();
        for (ClassicCaseUsage usage : usages) {
            usageVOList.add(toVO(usage));
        }
        vo.setRequestBatchId(usages.isEmpty() ? request.getRequestBatchId() : usages.get(0).getRequestBatchId());
        vo.setTotalCount(request.getItems() == null ? 0 : request.getItems().size());
        vo.setSuccessCount(usages.size());
        vo.setFailedCount(Math.max(0, vo.getTotalCount() - vo.getSuccessCount()));
        vo.setUsages(usageVOList);
        return ApiResult.success(vo);
    }

    /**
     * 生成经典案例数据并创建进入原平台学习环境的启动上下文。
     *
     * @param request 经典案例生成并启动请求。
     * @return 使用记录和带明文 launchToken 的启动上下文。
     */
    @PostMapping("/generate-launch")
    public ApiResult<ClassicCaseLaunchVO> generateAndLaunch(@Valid @RequestBody ClassicCaseGenerateLaunchRequest request) {
        ClassicCaseService.ClassicCaseLaunchResult result =
                classicCaseService.generateAndCreateLaunchContext(request);
        return ApiResult.success(toLaunchVO(result));
    }

    /**
     * 将实体转换为接口返回对象，避免直接暴露持久化对象。
     *
     * @param asset 经典案例资产。
     * @return 返回对象。
     */
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

    /**
     * 将使用记录实体转换为接口返回对象，只返回后续跳转和追踪需要的摘要字段。
     *
     * @param usage 经典案例使用记录。
     * @return 使用记录返回对象。
     */
    private ClassicCaseUsageVO toVO(ClassicCaseUsage usage) {
        ClassicCaseUsageVO vo = new ClassicCaseUsageVO();
        vo.setId(usage.getId());
        vo.setCaseAssetId(usage.getCaseAssetId());
        vo.setCaseVersionId(usage.getCaseVersionId());
        vo.setUsageScene(usage.getUsageScene());
        vo.setSceneType(usage.getSceneType());
        vo.setRequestBatchId(usage.getRequestBatchId());
        vo.setRequestItemId(usage.getRequestItemId());
        vo.setOwnerUserId(usage.getOwnerUserId());
        vo.setGeneratedInstanceId(usage.getGeneratedInstanceId());
        vo.setTeachingDataInstanceId(usage.getTeachingDataInstanceId());
        vo.setExternalBusinessNo(usage.getExternalBusinessNo());
        vo.setExternalBusinessName(usage.getExternalBusinessName());
        vo.setExternalStatus(usage.getExternalStatus());
        vo.setTargetUrl(usage.getTargetUrl());
        vo.setStatus(usage.getStatus());
        vo.setUseTime(usage.getUseTime());
        return vo;
    }

    /**
     * 将生成并启动的领域结果转换为接口返回对象。
     *
     * @param result 经典案例生成并启动结果。
     * @return 返回对象。
     */
    private ClassicCaseLaunchVO toLaunchVO(ClassicCaseService.ClassicCaseLaunchResult result) {
        ClassicCaseLaunchVO vo = new ClassicCaseLaunchVO();
        vo.setUsage(toVO(result.getUsage()));
        vo.setLaunchContext(toLaunchContextVO(result.getCreatedLaunchContext()));
        return vo;
    }

    /**
     * 将启动上下文服务结果转换为经典案例接口可复用的返回对象。
     *
     * @param created 已创建的启动上下文和明文 token。
     * @return 启动上下文返回对象。
     */
    private PlatformLaunchContextVO toLaunchContextVO(PlatformLaunchContextService.CreatedLaunchContext created) {
        PlatformLaunchContext launchContext = created.getLaunchContext();
        PlatformLaunchContextVO vo = new PlatformLaunchContextVO();
        vo.setId(launchContext.getId());
        vo.setTenantId(launchContext.getTenantId());
        vo.setLaunchToken(created.getLaunchToken());
        vo.setUserId(launchContext.getUserId());
        vo.setConnectorSystemId(launchContext.getConnectorSystemId());
        vo.setTaskId(launchContext.getTaskId());
        vo.setTeachingPointId(launchContext.getTeachingPointId());
        vo.setExecutionId(launchContext.getExecutionId());
        vo.setSceneType(launchContext.getSceneType());
        vo.setSdkMode(launchContext.getSdkMode());
        vo.setTargetUrl(launchContext.getTargetUrl());
        vo.setLaunchStatus(launchContext.getLaunchStatus());
        vo.setExpireTime(launchContext.getExpireTime());
        vo.setCreateTime(launchContext.getCreateTime());
        return vo;
    }

    /**
     * 将经典案例版本转换为接口返回对象。
     *
     * @param version 经典案例版本。
     * @return 版本返回对象。
     */
    private ClassicCaseVersionVO toVO(ClassicCaseVersion version) {
        ClassicCaseVersionVO vo = new ClassicCaseVersionVO();
        vo.setId(version.getId());
        vo.setCaseAssetId(version.getCaseAssetId());
        vo.setVersionNo(version.getVersionNo());
        vo.setPayloadSchemaVersion(version.getPayloadSchemaVersion());
        vo.setPayloadHash(version.getPayloadHash());
        vo.setStatus(version.getStatus());
        vo.setCreateBy(version.getCreateBy());
        vo.setCreateTime(version.getCreateTime());
        return vo;
    }
}
