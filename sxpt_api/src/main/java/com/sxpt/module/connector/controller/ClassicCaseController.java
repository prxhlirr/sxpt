package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.connector.ClassicCaseRuntimeConstants;
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
import org.springframework.util.StringUtils;

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
    public ApiResult<List<ClassicCaseAssetVO>> listClassicCases(@RequestParam(required = false) String tenantId,
                                                                @RequestParam(required = false) String learningConnectorSystemId,
                                                                @RequestParam(required = false) String moduleCode,
                                                                @RequestParam(required = false) String teachingPointId) {
        CurrentUserContext.CurrentUser currentUser = requireClassicCaseOperator();
        List<ClassicCaseAsset> assets = classicCaseService.listClassicCaseAssets(
                currentUser.getTenantId(),
                learningConnectorSystemId,
                moduleCode,
                teachingPointId);
        List<ClassicCaseAssetVO> result = new ArrayList<>();
        for (ClassicCaseAsset asset : assets) {
            result.add(toVO(asset));
        }
        return ApiResult.success(result);
    }

    /** 目标契约中的案例列表路径，支持模块、来源、状态、关键字、标签和分页筛选。 */
    @GetMapping
    public ApiResult<List<ClassicCaseAssetVO>> searchClassicCases(
            @RequestParam(required = false) String businessModuleCode,
            @RequestParam(required = false) String connectorSystemId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        CurrentUserContext.CurrentUser currentUser = requireClassicCaseOperator();
        if (page == null || page < 1 || pageSize == null || pageSize < 1 || pageSize > 200) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        List<ClassicCaseAssetVO> matched = new ArrayList<>();
        String normalizedKeyword = normalize(keyword);
        String normalizedTag = normalize(tag);
        for (ClassicCaseAsset asset : classicCaseService.listClassicCaseAssets(
                currentUser.getTenantId(), null, businessModuleCode, null)) {
            if (StringUtils.hasText(connectorSystemId)
                    && !connectorSystemId.equals(asset.getSourceConnectorSystemId())
                    && !connectorSystemId.equals(asset.getLearningConnectorSystemId())) {
                continue;
            }
            if (StringUtils.hasText(status) && !status.equalsIgnoreCase(asset.getStatus())) {
                continue;
            }
            String searchable = (asset.getCaseCode() + " " + asset.getCaseTitle() + " "
                    + (asset.getCaseSummary() == null ? "" : asset.getCaseSummary())).toLowerCase();
            if (normalizedKeyword != null && !searchable.contains(normalizedKeyword)) {
                continue;
            }
            if (normalizedTag != null && (asset.getTagsJson() == null
                    || !asset.getTagsJson().toLowerCase().contains(normalizedTag))) {
                continue;
            }
            matched.add(toVO(asset));
        }
        int from = Math.min((page - 1) * pageSize, matched.size());
        int to = Math.min(from + pageSize, matched.size());
        return ApiResult.success(new ArrayList<>(matched.subList(from, to)));
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
                                                        @RequestParam(required = false) String tenantId) {
        CurrentUserContext.CurrentUser currentUser = requireClassicCaseOperator();
        return ApiResult.success(toVO(classicCaseService.getClassicCaseAssetDetail(currentUser.getTenantId(), id)));
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
                                                                         @RequestParam(required = false) String tenantId) {
        CurrentUserContext.CurrentUser currentUser = requireClassicCaseOperator();
        List<ClassicCaseVersion> versions = classicCaseService.listClassicCaseVersions(currentUser.getTenantId(), id);
        List<ClassicCaseVersionVO> result = new ArrayList<>();
        for (ClassicCaseVersion version : versions) {
            result.add(toSummaryVO(version));
        }
        return ApiResult.success(result);
    }

    /** 查询确定的 OA 案例版本详情。 */
    @GetMapping("/{id}/versions/{caseVersionId}")
    public ApiResult<ClassicCaseVersionVO> getClassicCaseVersion(
            @PathVariable String id,
            @PathVariable String caseVersionId) {
        CurrentUserContext.CurrentUser currentUser = requireClassicCaseOperator();
        return ApiResult.success(toVO(classicCaseService.getClassicCaseVersionDetail(
                currentUser.getTenantId(), id, caseVersionId)));
    }

    /**
     * 导入经典案例。
     *
     * @param request 原平台正式环境推送的完全脱敏案例内容。
     * @return 已创建或更新的经典案例资产。
     */
    @PostMapping("/import")
    public ApiResult<ClassicCaseAssetVO> importClassicCase(@Valid @RequestBody ClassicCaseImportRequest request) {
        CurrentUserContext.CurrentUser currentUser = requireClassicCaseOperator();
        bindImportRequestToCurrentUser(request, currentUser);
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
        CurrentUserContext.CurrentUser currentUser = requireClassicCaseGenerateOperator(request);
        bindGenerateRequestToCurrentUser(request, currentUser);
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
        CurrentUserContext.CurrentUser currentUser = requireClassicCaseOperator();
        bindBatchGenerateRequestToCurrentUser(request, currentUser);
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
        CurrentUserContext.CurrentUser currentUser = requireClassicCaseGenerateOperator(request);
        bindGenerateRequestToCurrentUser(request, currentUser);
        ClassicCaseService.ClassicCaseLaunchResult result =
                classicCaseService.generateAndCreateLaunchContext(request);
        return ApiResult.success(toLaunchVO(result));
    }

    /**
     * 读取当前登录用户并校验经典案例后台入口权限。
     *
     * 业务功能：
     * 1. 经典案例管理属于教学平台内部能力，只允许管理员、老师和专家访问。
     * 2. 所有租户边界都从认证上下文取得，避免继续信任前端传入的 tenantId。
     *
     * @return 当前可信登录用户。
     */
    private CurrentUserContext.CurrentUser requireClassicCaseOperator() {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        if (!isClassicCaseManager(currentUser)) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        return requireTenant(currentUser);
    }

    /**
     * 校验经典案例生成入口权限。
     *
     * 业务功能：
     * 1. 管理员、老师、专家可以用于备案/教学复刻和批量准备。
     * 2. 学生只能为自己生成教案已锁定版本的复刻数据或格式 demo 数据。
     *
     * 关键流程：
     * 1. 先读取可信登录上下文，不信任前端传入的用户身份。
     * 2. 对学生角色额外限制 usageScene 必须是 STUDENT_DEMO 或 TEACHING_REPLICA，并强制绑定当前用户。
     *
     * @param request 经典案例生成请求。
     * @return 当前可信登录用户。
     */
    private CurrentUserContext.CurrentUser requireClassicCaseGenerateOperator(ClassicCaseGenerateRequest request) {
        CurrentUserContext.CurrentUser currentUser = CurrentUserContext.getRequiredUser();
        if (isClassicCaseManager(currentUser)) {
            return requireTenant(currentUser);
        }
        if (currentUser.hasAnyRole("STUDENT")
                && request != null
                && (ClassicCaseRuntimeConstants.USAGE_SCENE_STUDENT_DEMO.equals(request.getUsageScene())
                || ClassicCaseRuntimeConstants.USAGE_SCENE_TEACHING_REPLICA.equals(request.getUsageScene()))) {
            return requireTenant(currentUser);
        }
        throw new BusinessException(ApiResultCode.FORBIDDEN);
    }

    private boolean isClassicCaseManager(CurrentUserContext.CurrentUser currentUser) {
        return currentUser.hasAnyRole("ADMIN", "TEACHER", "EXPERT");
    }

    private CurrentUserContext.CurrentUser requireTenant(CurrentUserContext.CurrentUser currentUser) {
        if (currentUser.getTenantId() == null || currentUser.getTenantId().trim().isEmpty()) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        return currentUser;
    }

    /**
     * 将内部导入请求绑定到当前登录用户。
     *
     * 业务功能：
     * 1. 内部管理入口允许管理员/老师/专家手工导入或补录经典案例。
     * 2. 租户和创建人必须以后端认证上下文为准，防止请求体伪造跨租户案例。
     *
     * @param request 导入请求。
     * @param currentUser 当前可信登录用户。
     */
    private void bindImportRequestToCurrentUser(ClassicCaseImportRequest request,
                                                CurrentUserContext.CurrentUser currentUser) {
        request.setTenantId(currentUser.getTenantId());
        request.setCreateBy(currentUser.getUserId());
    }

    /**
     * 将单条经典案例生成请求绑定到当前登录用户。
     *
     * 业务功能：
     * 1. tenantId 必须从 JWT 上下文取得，避免前端伪造其他租户数据。
     * 2. ownerUserId 默认使用当前用户；当老师/专家后续需要为学生生成数据时，仍保留显式 ownerUserId 的业务扩展点。
     *
     * @param request 生成请求。
     * @param currentUser 当前可信登录用户。
     */
    private void bindGenerateRequestToCurrentUser(ClassicCaseGenerateRequest request,
                                                  CurrentUserContext.CurrentUser currentUser) {
        request.setTenantId(currentUser.getTenantId());
        if (currentUser.hasAnyRole("STUDENT")) {
            request.setOwnerUserId(currentUser.getUserId());
            request.setRequiredExternalOrgId(null);
            request.setRequiredExternalRoleId(null);
            request.setActorType(null);
            return;
        }
        if (!StringUtils.hasText(request.getOwnerUserId())) {
            request.setOwnerUserId(currentUser.getUserId());
        }
    }

    /**
     * 将批量经典案例生成请求绑定到当前登录用户租户。
     *
     * 业务功能：
     * 1. 批量 demo 生成可能面向一组学生，明细 ownerUserId 不能粗暴改成当前老师。
     * 2. 租户边界仍必须由服务端上下文统一覆盖，避免跨租户批量造数。
     *
     * @param request 批量生成请求。
     * @param currentUser 当前可信登录用户。
     */
    private void bindBatchGenerateRequestToCurrentUser(ClassicCaseBatchGenerateRequest request,
                                                       CurrentUserContext.CurrentUser currentUser) {
        request.setTenantId(currentUser.getTenantId());
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
        vo.setTagsJson(asset.getTagsJson());
        vo.setCurrentVersionId(asset.getCurrentVersionId());
        vo.setSourceUpdatedAt(asset.getSourceUpdatedAt());
        vo.setDisableReason(asset.getDisableReason());
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
        ClassicCaseVersionVO vo = toSummaryVO(version);
        vo.setIdentityBindingJson(version.getIdentityBindingJson());
        vo.setDesensitizedCasePayloadJson(version.getDesensitizedCasePayloadJson());
        vo.setCaseDataFormatJson(version.getCaseDataFormatJson());
        return vo;
    }

    /** 列表只返回版本索引；完整脱敏内容仅由确定版本详情接口返回。 */
    private ClassicCaseVersionVO toSummaryVO(ClassicCaseVersion version) {
        ClassicCaseVersionVO vo = new ClassicCaseVersionVO();
        vo.setId(version.getId());
        vo.setCaseAssetId(version.getCaseAssetId());
        vo.setVersionNo(version.getVersionNo());
        vo.setCaseVersionId(version.getCaseVersionId());
        vo.setPayloadSchemaVersion(version.getPayloadSchemaVersion());
        vo.setSupportedGenerationModesJson(version.getSupportedGenerationModesJson());
        vo.setPayloadHash(version.getPayloadHash());
        vo.setContentHash(version.getContentHash());
        vo.setStatus(version.getStatus());
        vo.setCreateBy(version.getCreateBy());
        vo.setCreateTime(version.getCreateTime());
        return vo;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim().toLowerCase() : null;
    }
}
