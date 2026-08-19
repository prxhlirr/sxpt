package com.sxpt.module.connector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.connector.ClassicCaseRuntimeConstants;
import com.sxpt.module.connector.TeachingDataTemplateUsageConstants;
import com.sxpt.module.connector.dto.ClassicCaseBatchGenerateItemRequest;
import com.sxpt.module.connector.dto.ClassicCaseBatchGenerateRequest;
import com.sxpt.module.connector.dto.ClassicCaseGenerateLaunchRequest;
import com.sxpt.module.connector.dto.ClassicCaseGenerateRequest;
import com.sxpt.module.connector.dto.ClassicCaseImportRequest;
import com.sxpt.module.connector.dto.ClassicCaseUpsertRequest;
import com.sxpt.module.connector.dto.ClassicCaseConfigValidationRequest;
import com.sxpt.module.connector.entity.BusinessModule;
import com.sxpt.module.connector.entity.BusinessModuleProcessActor;
import com.sxpt.module.connector.entity.ClassicCaseAsset;
import com.sxpt.module.connector.entity.ClassicCaseUsage;
import com.sxpt.module.connector.entity.ClassicCaseVersion;
import com.sxpt.module.connector.entity.ConnectorSystem;
import com.sxpt.module.connector.entity.PlatformLaunchContext;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import com.sxpt.module.connector.mapper.BusinessModuleMapper;
import com.sxpt.module.connector.mapper.BusinessModuleProcessActorMapper;
import com.sxpt.module.connector.mapper.ClassicCaseAssetMapper;
import com.sxpt.module.connector.mapper.ClassicCaseUsageMapper;
import com.sxpt.module.connector.mapper.ClassicCaseVersionMapper;
import com.sxpt.module.connector.mapper.ConnectorSystemMapper;
import com.sxpt.module.connector.mapper.TeachingDataInstanceMapper;
import com.sxpt.module.connector.mapper.TeachingDataTemplateMapper;
import com.sxpt.module.connector.service.ClassicCaseService;
import com.sxpt.module.connector.service.DataCreateRequestBuildService;
import com.sxpt.module.connector.service.OriginDataPrepareAdapter;
import com.sxpt.module.connector.service.PlatformLaunchContextService;
import com.sxpt.module.connector.service.ExternalConnectorCredentialService.AuthenticatedExternalConnector;
import com.sxpt.module.connector.vo.LessonPlanClassicCaseOptionVO;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.DataInstanceStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.RecordStatus;
import com.sxpt.module.teachingdata.enums.DataPrepareStatusEnums.ValidationStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 经典案例服务实现。
 *
 * 业务功能：
 * 1. 接收原平台正式环境推送的完全脱敏案例内容并落库。
 * 2. 通过正式环境、学习环境和环境组编码约束案例只在学习环境中复刻。
 *
 * 关键流程：
 * 1. 校验请求字段和 JSON 结构，避免非脱敏文本或错误格式进入案例资产。
 * 2. 校验来源平台必须是 PROD，目标平台必须是同组 LEARNING。
 * 3. 首次导入创建资产，重复导入新增版本并切换 currentVersionId。
 */
@Service
@Profile("!test")
public class ClassicCaseServiceImpl implements ClassicCaseService {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    private static final String ENVIRONMENT_TYPE_PROD = "PROD";

    private static final String ENVIRONMENT_TYPE_LEARNING = "LEARNING";

    private static final String DEFAULT_PAYLOAD_SCHEMA_VERSION = "1.0";

    private final ClassicCaseAssetMapper classicCaseAssetMapper;

    private final ClassicCaseVersionMapper classicCaseVersionMapper;

    private final ClassicCaseUsageMapper classicCaseUsageMapper;

    private final TeachingDataInstanceMapper teachingDataInstanceMapper;

    private final ConnectorSystemMapper connectorSystemMapper;

    private final BusinessModuleMapper businessModuleMapper;

    private final BusinessModuleProcessActorMapper businessModuleProcessActorMapper;

    private final TeachingDataTemplateMapper teachingDataTemplateMapper;

    private final OriginDataPrepareAdapter originDataPrepareAdapter;

    private final DataCreateRequestBuildService dataCreateRequestBuildService;

    private final PlatformLaunchContextService platformLaunchContextService;

    public ClassicCaseServiceImpl(ClassicCaseAssetMapper classicCaseAssetMapper,
                                  ClassicCaseVersionMapper classicCaseVersionMapper,
                                  ClassicCaseUsageMapper classicCaseUsageMapper,
                                  TeachingDataInstanceMapper teachingDataInstanceMapper,
                                  ConnectorSystemMapper connectorSystemMapper,
                                  BusinessModuleMapper businessModuleMapper,
                                  BusinessModuleProcessActorMapper businessModuleProcessActorMapper,
                                  TeachingDataTemplateMapper teachingDataTemplateMapper,
                                  OriginDataPrepareAdapter originDataPrepareAdapter,
                                  DataCreateRequestBuildService dataCreateRequestBuildService,
                                  PlatformLaunchContextService platformLaunchContextService) {
        this.classicCaseAssetMapper = classicCaseAssetMapper;
        this.classicCaseVersionMapper = classicCaseVersionMapper;
        this.classicCaseUsageMapper = classicCaseUsageMapper;
        this.teachingDataInstanceMapper = teachingDataInstanceMapper;
        this.connectorSystemMapper = connectorSystemMapper;
        this.businessModuleMapper = businessModuleMapper;
        this.businessModuleProcessActorMapper = businessModuleProcessActorMapper;
        this.teachingDataTemplateMapper = teachingDataTemplateMapper;
        this.originDataPrepareAdapter = originDataPrepareAdapter;
        this.dataCreateRequestBuildService = dataCreateRequestBuildService;
        this.platformLaunchContextService = platformLaunchContextService;
    }

    /**
     * 导入经典案例。
     *
     * @param request 已完成脱敏的经典案例导入请求。
     * @return 已创建或更新的经典案例资产。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassicCaseAsset importClassicCase(ClassicCaseImportRequest request) {
        validateRequiredFields(request);
        ConnectorSystem sourceSystem = getConnectorSystem(request.getSourceConnectorSystemId());
        ConnectorSystem learningSystem = getConnectorSystem(request.getLearningConnectorSystemId());
        BusinessModule businessModule = getBusinessModule(request.getBusinessModuleId());
        validateEnvironmentBinding(request, sourceSystem, learningSystem, businessModule);

        String sceneTypesJson = normalizeJson(request.getSceneTypesJson(), true);
        boolean hasReplayPayload = StringUtils.hasText(request.getDesensitizedCasePayloadJson());
        boolean hasCaseDataFormat = StringUtils.hasText(request.getCaseDataFormatJson());
        String desensitizedPayloadJson = hasReplayPayload
                ? normalizeJson(request.getDesensitizedCasePayloadJson(), false)
                : "{}";
        String caseDataFormatJson = hasCaseDataFormat
                ? normalizeJson(request.getCaseDataFormatJson(), false)
                : "{}";
        String identityBindingJson = normalizeJson(request.getIdentityBindingJson(), false);
        String tagsJson = StringUtils.hasText(request.getTagsJson())
                ? normalizeJson(request.getTagsJson(), true)
                : "[]";
        String supportedGenerationModesJson = normalizeSupportedGenerationModes(
                request.getSupportedGenerationModesJson(), hasReplayPayload, hasCaseDataFormat);
        String desensitizePolicyJson = StringUtils.hasText(request.getDesensitizePolicyJson())
                ? normalizeJson(request.getDesensitizePolicyJson(), false)
                : null;
        validateIdentityBindingActors(request, identityBindingJson);

        ClassicCaseAsset asset = findAsset(request.getTenantId(),
                request.getSourceConnectorSystemId(), request.getCaseCode());
        boolean newAsset = asset == null;
        String contentHash = buildContentHash(request, request.getModuleCode(), tagsJson,
                supportedGenerationModesJson, identityBindingJson,
                desensitizedPayloadJson, caseDataFormatJson);
        if (newAsset) {
            asset = buildNewAsset(request, sourceSystem, learningSystem, sceneTypesJson);
            classicCaseAssetMapper.insert(asset);
        }

        ClassicCaseVersion existingVersion = StringUtils.hasText(request.getCaseVersionId())
                ? findVersionByExternalId(request.getTenantId(), asset.getId(), request.getCaseVersionId())
                : null;
        if (existingVersion != null) {
            boolean unchangedContentExceptModule = matchesStoredContentExceptModule(
                    request, asset, existingVersion, tagsJson,
                    supportedGenerationModesJson, identityBindingJson,
                    desensitizedPayloadJson, caseDataFormatJson);
            boolean unchangedDesensitizePolicy = jsonContentEquals(
                    desensitizePolicyJson, existingVersion.getDesensitizePolicyJson(), false);
            if (!unchangedDesensitizePolicy
                    || (!contentHash.equals(existingVersion.getContentHash())
                    && !unchangedContentExceptModule)) {
                throw new BusinessException(ApiResultCode.IDEMPOTENCY_CONFLICT.getCode(),
                        "相同 caseVersionId 对应的案例内容不一致");
            }
            if (!contentHash.equals(existingVersion.getContentHash())) {
                // 模块归属属于案例资产元数据；同步历史哈希，保证迁移完成后的重推仍然幂等。
                existingVersion.setContentHash(contentHash);
                classicCaseVersionMapper.updateById(existingVersion);
            }
            if (!newAsset) {
                patchAsset(asset, request, sourceSystem, learningSystem, sceneTypesJson);
            }
            // 同内容重推也视为一次显式启用，便于 OA 在停用后恢复同一不可变版本。
            asset.setCurrentVersionId(existingVersion.getId());
            asset.setTagsJson(tagsJson);
            asset.setSourceUpdatedAt(request.getSourceUpdatedAt());
            asset.setDisableReason(null);
            asset.setStatus(ClassicCaseRuntimeConstants.STATUS_AVAILABLE);
            asset.setUpdateBy(request.getCreateBy());
            asset.setUpdateTime(LocalDateTime.now());
            asset.setLockVersion((asset.getLockVersion() == null ? 0L : asset.getLockVersion()) + 1L);
            classicCaseAssetMapper.updateById(asset);
            return asset;
        }

        if (!newAsset) {
            patchAsset(asset, request, sourceSystem, learningSystem, sceneTypesJson);
        }

        ClassicCaseVersion version = buildVersion(
                request,
                asset,
                nextVersionNo(asset),
                desensitizedPayloadJson,
                caseDataFormatJson,
                identityBindingJson,
                desensitizePolicyJson,
                supportedGenerationModesJson,
                contentHash);
        classicCaseVersionMapper.insert(version);

        asset.setCurrentVersionId(version.getId());
        asset.setTagsJson(tagsJson);
        asset.setSourceUpdatedAt(request.getSourceUpdatedAt());
        asset.setDisableReason(null);
        asset.setStatus(ClassicCaseRuntimeConstants.STATUS_AVAILABLE);
        asset.setUpdateBy(request.getCreateBy());
        asset.setUpdateTime(LocalDateTime.now());
        asset.setLockVersion((asset.getLockVersion() == null ? 0L : asset.getLockVersion()) + 1L);
        classicCaseAssetMapper.updateById(asset);
        return asset;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassicCaseAsset upsertClassicCase(ClassicCaseUpsertRequest request,
                                              AuthenticatedExternalConnector connector) {
        if (request == null || connector == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        ConnectorSystem sourceSystem = connector.getSourceSystem();
        ConnectorSystem learningSystem = connector.getLearningSystem();
        BusinessModule module = businessModuleMapper.selectOne(new QueryWrapper<BusinessModule>()
                .eq("tenant_id", sourceSystem.getTenantId())
                .eq("connector_system_id", learningSystem.getId())
                .eq("module_code", request.getBusinessModuleCode().trim())
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE)
                .last("limit 1"));
        if (module == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND.getCode(), "业务模块不存在或未启用");
        }
        validateObjectNode(request.getIdentityBinding(), "identityBinding");
        validateOptionalObjectNode(request.getDesensitizedCasePayload(), "desensitizedCasePayload");
        validateOptionalObjectNode(request.getCaseDataFormat(), "caseDataFormat");

        ClassicCaseImportRequest importRequest = new ClassicCaseImportRequest();
        importRequest.setTenantId(sourceSystem.getTenantId());
        importRequest.setCaseCode(request.getCaseCode().trim());
        importRequest.setCaseVersionId(request.getCaseVersionId().trim());
        importRequest.setCaseTitle(request.getCaseName().trim());
        importRequest.setCaseSummary(trimToNull(request.getSummary()));
        importRequest.setSourceConnectorSystemId(sourceSystem.getId());
        importRequest.setLearningConnectorSystemId(learningSystem.getId());
        importRequest.setBusinessModuleId(module.getId());
        importRequest.setModuleCode(module.getModuleCode());
        importRequest.setSceneTypesJson("[\"TEACHING\",\"PRACTICE\"]");
        importRequest.setTagsJson(toJson(request.getTags() == null
                ? Collections.emptyList() : request.getTags()));
        importRequest.setSupportedGenerationModesJson(toJson(request.getSupportedGenerationModes()));
        importRequest.setPayloadSchemaVersion(request.getPayloadSchemaVersion().trim());
        importRequest.setIdentityBindingJson(request.getIdentityBinding().toString());
        importRequest.setDesensitizedCasePayloadJson(request.getDesensitizedCasePayload() == null
                || request.getDesensitizedCasePayload().isNull()
                ? null : request.getDesensitizedCasePayload().toString());
        importRequest.setCaseDataFormatJson(request.getCaseDataFormat() == null
                || request.getCaseDataFormat().isNull()
                ? null : request.getCaseDataFormat().toString());
        importRequest.setSourceUpdatedAt(parseSourceUpdatedAt(request.getSourceUpdatedAt()));
        importRequest.setCreateBy(sourceSystem.getId());
        return importClassicCase(importRequest);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassicCaseAsset disableClassicCase(String tenantId,
                                               String sourceConnectorSystemId,
                                               String caseCode,
                                               String reason,
                                               String operator) {
        requireText(tenantId);
        requireText(sourceConnectorSystemId);
        requireText(caseCode);
        requireText(reason);
        ClassicCaseAsset asset = findAsset(tenantId, sourceConnectorSystemId, caseCode);
        if (asset == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        asset.setStatus(ClassicCaseRuntimeConstants.STATUS_DISABLED);
        asset.setDisableReason(reason.trim());
        asset.setUpdateBy(firstText(operator, sourceConnectorSystemId));
        asset.setUpdateTime(LocalDateTime.now());
        asset.setLockVersion((asset.getLockVersion() == null ? 0L : asset.getLockVersion()) + 1L);
        classicCaseAssetMapper.updateById(asset);
        return asset;
    }

    /**
     * 基于经典案例在学习环境生成业务数据。
     *
     * @param request 经典案例生成请求。
     * @return 经典案例使用记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassicCaseUsage generateClassicCaseData(ClassicCaseGenerateRequest request) {
        return generateClassicCaseDataInternal(request).getUsage();
    }

    /**
     * 查询经典案例资产列表。
     *
     * @param tenantId 租户 ID。
     * @param learningConnectorSystemId 学习环境平台 ID，可选。
     * @param moduleCode 模块编码，可选。
     * @param teachingPointId 教学点 ID，可选。
     * @return 经典案例资产列表。
     */
    @Override
    public List<ClassicCaseAsset> listClassicCaseAssets(String tenantId,
                                                        String learningConnectorSystemId,
                                                        String moduleCode,
                                                        String teachingPointId) {
        requireText(tenantId);
        QueryWrapper<ClassicCaseAsset> query = new QueryWrapper<ClassicCaseAsset>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("update_time")
                .orderByDesc("create_time");
        if (StringUtils.hasText(learningConnectorSystemId)) {
            query.eq("learning_connector_system_id", learningConnectorSystemId.trim());
        }
        if (StringUtils.hasText(moduleCode)) {
            query.eq("module_code", moduleCode.trim());
        }
        if (StringUtils.hasText(teachingPointId)) {
            query.eq("teaching_point_id", teachingPointId.trim());
        }
        return classicCaseAssetMapper.selectList(query);
    }

    /**
     * 查询经典案例资产详情。
     *
     * @param tenantId 租户 ID。
     * @param caseAssetId 经典案例资产 ID。
     * @return 经典案例资产。
     */
    @Override
    public ClassicCaseAsset getClassicCaseAssetDetail(String tenantId, String caseAssetId) {
        return getClassicCaseAsset(tenantId, caseAssetId);
    }

    /**
     * 查询经典案例版本列表。
     *
     * @param tenantId 租户 ID。
     * @param caseAssetId 经典案例资产 ID。
     * @return 版本列表。
     */
    @Override
    public List<ClassicCaseVersion> listClassicCaseVersions(String tenantId, String caseAssetId) {
        getClassicCaseAsset(tenantId, caseAssetId);
        return classicCaseVersionMapper.selectList(new QueryWrapper<ClassicCaseVersion>()
                .eq("tenant_id", tenantId)
                .eq("case_asset_id", caseAssetId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("version_no"));
    }

    @Override
    public ClassicCaseVersion getClassicCaseVersionDetail(String tenantId,
                                                          String caseAssetId,
                                                          String caseVersionId) {
        getClassicCaseAsset(tenantId, caseAssetId);
        return getClassicCaseVersion(tenantId, caseAssetId, caseVersionId);
    }

    @Override
    public List<LessonPlanClassicCaseOptionVO> listClassicCaseOptions(String tenantId,
                                                                      String businessModuleCode,
                                                                      String connectorSystemId,
                                                                      String keyword) {
        requireText(tenantId);
        requireText(businessModuleCode);
        QueryWrapper<ClassicCaseAsset> query = new QueryWrapper<ClassicCaseAsset>()
                .eq("tenant_id", tenantId)
                .eq("module_code", businessModuleCode.trim())
                .in("status", ClassicCaseRuntimeConstants.STATUS_AVAILABLE,
                        RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("update_time");
        if (StringUtils.hasText(connectorSystemId)) {
            query.and(wrapper -> wrapper
                    .eq("source_connector_system_id", connectorSystemId.trim())
                    .or()
                    .eq("learning_connector_system_id", connectorSystemId.trim()));
        }
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            query.and(wrapper -> wrapper
                    .like("case_title", normalizedKeyword)
                    .or()
                    .like("case_code", normalizedKeyword));
        }
        List<LessonPlanClassicCaseOptionVO> options = new ArrayList<>();
        for (ClassicCaseAsset asset : classicCaseAssetMapper.selectList(query)) {
            List<ClassicCaseVersion> versions = classicCaseVersionMapper.selectList(
                    new QueryWrapper<ClassicCaseVersion>()
                            .eq("tenant_id", tenantId)
                            .eq("case_asset_id", asset.getId())
                            .in("status", ClassicCaseRuntimeConstants.STATUS_AVAILABLE,
                                    RecordStatus.ACTIVE.getValue())
                            .eq("deleted", Boolean.FALSE)
                            .orderByDesc("version_no"));
            for (ClassicCaseVersion version : versions) {
                options.add(toClassicCaseOption(asset, version));
            }
        }
        return options;
    }

    @Override
    public LessonPlanClassicCaseOptionVO validateClassicCaseConfig(String tenantId,
                                                                   ClassicCaseConfigValidationRequest request) {
        requireText(tenantId);
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        ClassicCaseAsset asset = classicCaseAssetMapper.selectOne(new QueryWrapper<ClassicCaseAsset>()
                .eq("tenant_id", tenantId)
                .eq("id", request.getClassicCaseId())
                .eq("source_connector_system_id", request.getConnectorSystemId())
                .eq("module_code", request.getBusinessModuleCode())
                .in("status", ClassicCaseRuntimeConstants.STATUS_AVAILABLE,
                        RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE));
        if (asset == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND.getCode(),
                    "经典案例不存在、已停用或与教案模块不匹配");
        }
        ClassicCaseVersion version = getClassicCaseVersion(
                tenantId, asset.getId(), request.getCaseVersionId());
        if (!supportedGenerationModes(version).contains(request.getGenerationMode().trim())) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                    "案例版本不支持所选生成模式");
        }
        return toClassicCaseOption(asset, version);
    }

    /**
     * 批量生成学生经典案例 demo 数据。
     *
     * @param request 批量生成请求。
     * @return 批量使用记录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<ClassicCaseUsage> batchGenerateClassicCaseData(ClassicCaseBatchGenerateRequest request) {
        validateBatchGenerateFields(request);
        ClassicCaseAsset asset = getClassicCaseAsset(request.getTenantId(), request.getCaseAssetId());
        ClassicCaseVersion version = getClassicCaseVersion(
                request.getTenantId(),
                asset.getId(),
                StringUtils.hasText(request.getCaseVersionId())
                        ? request.getCaseVersionId()
                        : asset.getCurrentVersionId());
        if (!supportedGenerationModes(version).contains(
                ClassicCaseRuntimeConstants.GENERATION_MODE_FORMAT_DEMO)) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                    "案例版本不支持 FORMAT_DEMO 模式");
        }
        String requestBatchId = firstText(request.getRequestBatchId(), "classic-case-batch-" + UUID.randomUUID());
        String traceId = firstText(request.getTraceId(), requestBatchId);
        List<BatchRuntimeItem> runtimeItems = buildBatchRuntimeItems(request);
        TeachingDataTemplate template = resolveClassicCaseTemplate(
                asset,
                request.getSceneType(),
                request.getUsageScene());
        OriginDataPrepareAdapter.BatchCreateRequest createRequest =
                dataCreateRequestBuildService.buildClassicCaseRequest(buildBatchCreateContext(
                        request,
                        asset,
                        version,
                        template,
                        requestBatchId,
                        traceId,
                        runtimeItems));
        String requestJson = createRequest.getRequestJson();

        OriginDataPrepareAdapter.BatchCreateResponse response =
                originDataPrepareAdapter.createTeachingData(createRequest);
        Map<String, OriginDataPrepareAdapter.ResponseItem> responseItems = responseItemsByRequestItemId(response);
        List<ClassicCaseUsage> usages = new ArrayList<>();
        for (BatchRuntimeItem runtimeItem : runtimeItems) {
            OriginDataPrepareAdapter.ResponseItem responseItem =
                    requireResponseItem(responseItems, runtimeItem.getRequestItemId());
            ClassicCaseGenerateRequest itemRequest =
                    buildGenerateRequestFromBatchItem(request, runtimeItem.getItem(), runtimeItem.getRequestItemId());
            String usageId = UUID.randomUUID().toString();
            TeachingDataInstance instance = buildTeachingDataInstance(itemRequest, asset, version, usageId,
                    requestBatchId, runtimeItem.getRequestItemId(), requestJson, responseItem);
            teachingDataInstanceMapper.insert(instance);
            ClassicCaseUsage usage = buildUsage(itemRequest, asset, version, usageId, instance.getId(),
                    requestBatchId, runtimeItem.getRequestItemId(), traceId, requestJson, response, responseItem);
            classicCaseUsageMapper.insert(usage);
            usages.add(usage);
        }
        return usages;
    }

    /**
     * 生成经典案例数据并创建进入原平台学习环境的 launchToken。
     *
     * @param request 经典案例生成并启动请求。
     * @return 使用记录和已创建的启动上下文。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClassicCaseLaunchResult generateAndCreateLaunchContext(ClassicCaseGenerateLaunchRequest request) {
        requireText(request.getSdkMode());
        GeneratedClassicCaseData generated = generateClassicCaseDataInternal(request);
        PlatformLaunchContext launchContext = buildLaunchContext(request, generated.getUsage(), generated.getInstance());
        PlatformLaunchContextService.CreatedLaunchContext created =
                platformLaunchContextService.createLaunchContext(launchContext);
        return new ClassicCaseLaunchResult(generated.getUsage(), created);
    }

    /**
     * 生成经典案例数据并返回本次生成的使用记录和教学数据实例。
     *
     * @param request 经典案例生成请求。
     * @return 内部生成结果。
     */
    private GeneratedClassicCaseData generateClassicCaseDataInternal(ClassicCaseGenerateRequest request) {
        validateGenerateFields(request);
        ClassicCaseAsset asset = getClassicCaseAsset(request.getTenantId(), request.getCaseAssetId());
        ClassicCaseVersion version = getClassicCaseVersion(
                request.getTenantId(),
                asset.getId(),
                StringUtils.hasText(request.getCaseVersionId())
                        ? request.getCaseVersionId()
                        : asset.getCurrentVersionId());
        if (ClassicCaseRuntimeConstants.STATUS_DISABLED.equals(asset.getStatus())
                && !StringUtils.hasText(request.getCaseVersionId())) {
            throw new BusinessException(ApiResultCode.STATE_NOT_ALLOWED.getCode(), "经典案例已停用");
        }
        String generationMode = resolveGenerationMode(request.getUsageScene());
        if (!supportedGenerationModes(version).contains(generationMode)) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                    "案例版本不支持当前生成模式");
        }
        String requestBatchId = firstText(request.getRequestBatchId(), "classic-case-batch-" + UUID.randomUUID());
        String requestItemId = firstText(request.getRequestItemId(), "classic-case-item-" + UUID.randomUUID());
        String traceId = firstText(request.getTraceId(), requestBatchId);
        TeachingDataTemplate template = resolveClassicCaseTemplate(
                asset,
                request.getSceneType(),
                request.getUsageScene());
        OriginDataPrepareAdapter.BatchCreateRequest createRequest =
                dataCreateRequestBuildService.buildClassicCaseRequest(buildSingleCreateContext(
                        request,
                        asset,
                        version,
                        template,
                        requestBatchId,
                        requestItemId,
                        traceId));
        String requestJson = createRequest.getRequestJson();

        OriginDataPrepareAdapter.BatchCreateResponse response =
                originDataPrepareAdapter.createTeachingData(createRequest);
        OriginDataPrepareAdapter.ResponseItem responseItem = firstResponseItem(response);
        String usageId = UUID.randomUUID().toString();
        TeachingDataInstance instance = buildTeachingDataInstance(request, asset, version, usageId,
                requestBatchId, requestItemId, requestJson, responseItem);
        teachingDataInstanceMapper.insert(instance);
        ClassicCaseUsage usage = buildUsage(request, asset, version, usageId, instance.getId(),
                requestBatchId, requestItemId, traceId, requestJson, response, responseItem);
        classicCaseUsageMapper.insert(usage);
        return new GeneratedClassicCaseData(usage, instance);
    }

    /**
     * 校验导入请求最小字段，保证后续版本能被准确定位到租户、平台、模块和教学场景。
     *
     * @param request 导入请求。
     */
    private void validateRequiredFields(ClassicCaseImportRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getTenantId());
        requireText(request.getCaseCode());
        requireText(request.getCaseTitle());
        requireText(request.getSourceConnectorSystemId());
        requireText(request.getLearningConnectorSystemId());
        requireText(request.getBusinessModuleId());
        requireText(request.getModuleCode());
        requireText(request.getSceneTypesJson());
        if (!StringUtils.hasText(request.getDesensitizedCasePayloadJson())
                && !StringUtils.hasText(request.getCaseDataFormatJson())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR.getCode(),
                    "脱敏案例内容和数据格式至少提供一项");
        }
        requireText(request.getIdentityBindingJson());
        requireText(request.getCreateBy());
    }

    /**
     * 校验经典案例生成请求的最小字段和使用方式。
     *
     * @param request 生成请求。
     */
    private void validateGenerateFields(ClassicCaseGenerateRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getTenantId());
        requireText(request.getCaseAssetId());
        requireText(request.getUsageScene());
        requireText(request.getSceneType());
        requireText(request.getOwnerUserId());
        String usageScene = request.getUsageScene().trim();
        if (!ClassicCaseRuntimeConstants.USAGE_SCENE_TEACHING_REPLICA.equals(usageScene)
                && !ClassicCaseRuntimeConstants.USAGE_SCENE_STUDENT_DEMO.equals(usageScene)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (StringUtils.hasText(request.getParticipantContextJson())) {
            normalizeJson(request.getParticipantContextJson(), false);
        }
    }

    /**
     * 校验经典案例批量生成请求。
     *
     * @param request 批量生成请求。
     */
    private void validateBatchGenerateFields(ClassicCaseBatchGenerateRequest request) {
        if (request == null) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        requireText(request.getTenantId());
        requireText(request.getCaseAssetId());
        requireText(request.getUsageScene());
        requireText(request.getSceneType());
        if (!ClassicCaseRuntimeConstants.USAGE_SCENE_STUDENT_DEMO.equals(request.getUsageScene().trim())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        for (ClassicCaseBatchGenerateItemRequest item : request.getItems()) {
            if (item == null) {
                throw new BusinessException(ApiResultCode.PARAM_ERROR);
            }
            requireText(item.getOwnerUserId());
            if (StringUtils.hasText(item.getParticipantContextJson())) {
                normalizeJson(item.getParticipantContextJson(), false);
            }
        }
    }

    /**
     * 读取未删除且启用的经典案例资产。
     *
     * @param tenantId 租户 ID。
     * @param caseAssetId 案例资产 ID。
     * @return 案例资产。
     */
    private ClassicCaseAsset getClassicCaseAsset(String tenantId, String caseAssetId) {
        ClassicCaseAsset asset = classicCaseAssetMapper.selectOne(new QueryWrapper<ClassicCaseAsset>()
                .eq("tenant_id", tenantId)
                .eq("id", caseAssetId)
                .in("status", RecordStatus.ACTIVE.getValue(),
                        ClassicCaseRuntimeConstants.STATUS_AVAILABLE,
                        ClassicCaseRuntimeConstants.STATUS_DISABLED)
                .eq("deleted", Boolean.FALSE));
        if (asset == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        requireText(asset.getCurrentVersionId());
        return asset;
    }

    /**
     * 读取指定案例版本，运行时只允许使用当前资产下未删除的启用版本。
     *
     * @param tenantId 租户 ID。
     * @param caseAssetId 案例资产 ID。
     * @param caseVersionId 案例版本 ID。
     * @return 案例版本。
     */
    private ClassicCaseVersion getClassicCaseVersion(String tenantId, String caseAssetId, String caseVersionId) {
        requireText(caseVersionId);
        ClassicCaseVersion version = classicCaseVersionMapper.selectOne(new QueryWrapper<ClassicCaseVersion>()
                .eq("tenant_id", tenantId)
                .eq("case_asset_id", caseAssetId)
                .and(wrapper -> wrapper.eq("id", caseVersionId)
                        .or().eq("case_version_id", caseVersionId))
                .in("status", RecordStatus.ACTIVE.getValue(),
                        ClassicCaseRuntimeConstants.STATUS_AVAILABLE)
                .eq("deleted", Boolean.FALSE));
        if (version == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return version;
    }

    /**
     * 构造调用学习环境 DATA_CREATE 的请求快照；老师复刻和学生 demo 在这里明确分流 payload。
     *
     * @param request 生成请求。
     * @param asset 案例资产。
     * @param version 案例版本。
     * @param traceId 链路追踪 ID。
     * @return 请求快照 JSON。
     */
    /**
     * 构造单条经典案例 DATA_CREATE 上下文。
     *
     * 业务功能：
     * 1. 把经典案例服务内的资产、版本、参与方信息转换为统一请求构建器输入。
     * 2. 后续单位/角色自动映射会集中接入该转换点，避免散落到造数调用处。
     *
     * @param request 经典案例生成请求。
     * @param asset 经典案例资产。
     * @param version 经典案例版本。
     * @param requestBatchId 请求批次 ID。
     * @param requestItemId 请求明细 ID。
     * @param traceId 链路追踪 ID。
     * @return 经典案例 DATA_CREATE 构建上下文。
     */
    /**
     * 解析经典案例运行时应使用的数据模板。
     *
     * 业务功能：
     * 1. 根据使用场景区分教师/专家还原模板与学生 demo 模板。
     * 2. 优先使用案例绑定教学点下的模板，没有教学点模板时退回模块默认模板。
     *
     * @param asset 经典案例资产。
     * @param sceneType 教学场景。
     * @param usageScene 经典案例使用场景。
     * @return 已启用的数据模板。
     */
    private TeachingDataTemplate resolveClassicCaseTemplate(ClassicCaseAsset asset,
                                                            String sceneType,
                                                            String usageScene) {
        String templateUsage = resolveTemplateUsage(usageScene);
        List<TeachingDataTemplate> templates = teachingDataTemplateMapper.selectList(
                buildClassicCaseTemplateQuery(asset, sceneType, templateUsage, true));
        if ((templates == null || templates.isEmpty()) && StringUtils.hasText(asset.getTeachingPointId())) {
            templates = teachingDataTemplateMapper.selectList(
                    buildClassicCaseTemplateQuery(asset, sceneType, templateUsage, false));
        }
        if (templates == null || templates.isEmpty()) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                    "经典案例缺少可用数据模板，请配置 " + templateUsage + " 模板");
        }
        TeachingDataTemplate template = templates.get(0);
        requireText(template.getId());
        requireText(template.getTemplateCode());
        requireText(template.getInitState());
        return template;
    }

    /**
     * 构造经典案例模板查询条件。
     *
     * @param asset 经典案例资产。
     * @param sceneType 教学场景。
     * @param templateUsage 模板用途。
     * @param preferTeachingPoint 是否优先限定教学点。
     * @return 模板查询条件。
     */
    private QueryWrapper<TeachingDataTemplate> buildClassicCaseTemplateQuery(ClassicCaseAsset asset,
                                                                             String sceneType,
                                                                             String templateUsage,
                                                                             boolean preferTeachingPoint) {
        QueryWrapper<TeachingDataTemplate> query = new QueryWrapper<TeachingDataTemplate>()
                .eq("tenant_id", asset.getTenantId())
                .eq("connector_system_id", asset.getLearningConnectorSystemId())
                .eq("module_code", asset.getModuleCode())
                .eq("scene_type", sceneType.trim())
                .eq("template_usage", templateUsage)
                .eq("status", RecordStatus.ACTIVE.getValue())
                .eq("deleted", Boolean.FALSE);
        if (preferTeachingPoint && StringUtils.hasText(asset.getTeachingPointId())) {
            query.eq("teaching_point_id", asset.getTeachingPointId());
        } else {
            query.isNull("teaching_point_id");
        }
        return query.orderByDesc("create_time");
    }

    /**
     * 将经典案例使用场景转换为模板用途。
     *
     * @param usageScene 经典案例使用场景。
     * @return 模板用途。
     */
    private String resolveTemplateUsage(String usageScene) {
        if (ClassicCaseRuntimeConstants.USAGE_SCENE_TEACHING_REPLICA.equals(usageScene.trim())) {
            return TeachingDataTemplateUsageConstants.CLASSIC_CASE_REPLAY;
        }
        return TeachingDataTemplateUsageConstants.CLASSIC_CASE_DEMO;
    }

    /**
     * 解析经典案例运行时默认参与方。
     *
     * 业务功能：
     * 1. 复用业务模块已维护的单位/角色参与方配置，不为经典案例另建角色体系。
     * 2. 当老师或学生请求未显式传入学习环境单位、角色时，自动补齐默认主体约束。
     *
     * 关键流程：
     * 1. 按租户、学习环境、业务模块和模块编码查询启用的流程参与方。
     * 2. 取 actorNo 最小的参与方作为第一阶段默认主体。
     * 3. 缺少配置时保持兼容，不阻断已有手动传参或无主体约束的第三方造数。
     *
     * @param asset 经典案例资产。
     * @return 默认参与方；未配置时返回 null。
     */
    private ResolvedClassicCaseActor resolveClassicCaseActor(ClassicCaseAsset asset,
                                                             String identityBindingJson,
                                                             String requestedActorType) {
        List<BusinessModuleProcessActor> actors = businessModuleProcessActorMapper.selectList(
                new QueryWrapper<BusinessModuleProcessActor>()
                        .eq("tenant_id", asset.getTenantId())
                        .eq("connector_system_id", asset.getLearningConnectorSystemId())
                        .eq("business_module_id", asset.getBusinessModuleId())
                        .eq("module_code", asset.getModuleCode())
                        .eq("status", RecordStatus.ACTIVE.getValue())
                        .eq("deleted", Boolean.FALSE)
                        .orderByAsc("actor_no")
                        .orderByAsc("create_time"));
        if (actors == null || actors.isEmpty()) {
            return null;
        }
        String moduleActorCode = resolveRequestedOrBoundActorCode(actors, requestedActorType, identityBindingJson);
        BusinessModuleProcessActor actor = resolveActorByCode(actors, moduleActorCode);
        return new ResolvedClassicCaseActor(
                trimToNull(actor.getRequiredOrgCode()),
                trimToNull(actor.getRequiredRoleCode()),
                firstText(actor.getActorType(), actor.getActorRelation()));
    }

    /**
     * 解析经典案例运行时应采用的模块参与方编码。
     *
     * 业务功能：
     * 1. 请求入参 actorType 只在能命中模块参与方时作为显式选择。
     * 2. teacher/student 等门户角色不能命中模块参与方时，回退到经典案例入库时确认过的 identityBinding。
     *
     * @param actors 模块参与方配置。
     * @param requestedActorType 请求显式传入的参与方编码。
     * @param identityBindingJson 经典案例身份绑定 JSON。
     * @return 可用于匹配模块参与方的编码。
     */
    private String resolveRequestedOrBoundActorCode(List<BusinessModuleProcessActor> actors,
                                                    String requestedActorType,
                                                    String identityBindingJson) {
        if (StringUtils.hasText(requestedActorType) && containsActorCode(actors, requestedActorType.trim())) {
            return requestedActorType.trim();
        }
        return firstModuleActorCode(identityBindingJson);
    }

    /**
     * 校验经典案例身份绑定中的模块参与方是否存在。
     *
     * 业务功能：
     * 1. 原平台正式环境推送经典案例时，提前校验 moduleActorCode 和教学平台模块参与方配置的一致性。
     * 2. 避免案例已经入库并启用后，老师备案、学生练习生成数据时才发现单位/角色无法映射。
     *
     * 关键流程：
     * 1. 从 identity_binding_json.bindings 中提取所有 moduleActorCode。
     * 2. 仅当存在绑定编码时查询学习环境的业务模块参与方，保持空绑定的历史兼容。
     * 3. 任一编码无法匹配 actorType、actorRelation、requiredOrgCode、requiredRoleCode 时拒绝导入。
     *
     * @param request 经典案例导入请求。
     * @param identityBindingJson 已规范化身份绑定 JSON。
     */
    private void validateIdentityBindingActors(ClassicCaseImportRequest request, String identityBindingJson) {
        Set<String> moduleActorCodes = collectModuleActorCodes(identityBindingJson);
        if (moduleActorCodes.isEmpty()) {
            return;
        }

        List<BusinessModuleProcessActor> actors = businessModuleProcessActorMapper.selectList(
                new QueryWrapper<BusinessModuleProcessActor>()
                        .eq("tenant_id", request.getTenantId())
                        .eq("connector_system_id", request.getLearningConnectorSystemId())
                        .eq("business_module_id", request.getBusinessModuleId())
                        .eq("module_code", request.getModuleCode())
                        .eq("status", RecordStatus.ACTIVE.getValue())
                        .eq("deleted", Boolean.FALSE)
                        .orderByAsc("actor_no")
                        .orderByAsc("create_time"));
        List<String> missingCodes = new ArrayList<>();
        for (String moduleActorCode : moduleActorCodes) {
            if (!containsActorCode(actors, moduleActorCode)) {
                missingCodes.add(moduleActorCode);
            }
        }
        if (!missingCodes.isEmpty()) {
            throw new BusinessException(
                    ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                    "经典案例身份绑定缺少模块参与方配置: " + String.join(",", missingCodes));
        }
    }

    /**
     * 收集身份绑定 JSON 中声明的模块参与方编码。
     *
     * @param identityBindingJson 已规范化身份绑定 JSON。
     * @return 去重且保序的模块参与方编码集合。
     */
    private Set<String> collectModuleActorCodes(String identityBindingJson) {
        Set<String> moduleActorCodes = new LinkedHashSet<>();
        if (!StringUtils.hasText(identityBindingJson)) {
            return moduleActorCodes;
        }
        JsonNode root = parseJsonNode(identityBindingJson, false);
        JsonNode bindings = root.get("bindings");
        if (bindings == null || !bindings.isArray()) {
            return moduleActorCodes;
        }
        for (JsonNode binding : bindings) {
            JsonNode moduleActorCode = binding.get("moduleActorCode");
            if (moduleActorCode != null && moduleActorCode.isTextual()
                    && StringUtils.hasText(moduleActorCode.asText())) {
                moduleActorCodes.add(moduleActorCode.asText().trim());
            }
        }
        return moduleActorCodes;
    }

    /**
     * 判断参与方列表中是否存在指定编码。
     *
     * @param actors 模块参与方列表。
     * @param moduleActorCode 经典案例绑定的模块参与方编码。
     * @return true 表示存在匹配参与方。
     */
    private boolean containsActorCode(List<BusinessModuleProcessActor> actors, String moduleActorCode) {
        if (actors == null || actors.isEmpty()) {
            return false;
        }
        for (BusinessModuleProcessActor actor : actors) {
            if (matchesActorCode(actor, moduleActorCode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据 identity_binding_json 读取第一个模块参与方编码。
     *
     * @param identityBindingJson 经典案例身份绑定 JSON。
     * @return 模块参与方编码，未配置时返回 null。
     */
    private String firstModuleActorCode(String identityBindingJson) {
        if (!StringUtils.hasText(identityBindingJson)) {
            return null;
        }
        JsonNode root = parseJsonNode(identityBindingJson, false);
        JsonNode bindings = root.get("bindings");
        if (bindings == null || !bindings.isArray()) {
            return null;
        }
        for (JsonNode binding : bindings) {
            JsonNode moduleActorCode = binding.get("moduleActorCode");
            if (moduleActorCode != null && moduleActorCode.isTextual()
                    && StringUtils.hasText(moduleActorCode.asText())) {
                return moduleActorCode.asText().trim();
            }
        }
        return null;
    }

    /**
     * 按参与方编码解析模块参与方，无法命中时回退第一条默认参与方。
     *
     * @param actors 模块参与方列表。
     * @param moduleActorCode 参与方编码。
     * @return 命中的模块参与方或默认第一条。
     */
    private BusinessModuleProcessActor resolveActorByCode(List<BusinessModuleProcessActor> actors,
                                                          String moduleActorCode) {
        if (StringUtils.hasText(moduleActorCode)) {
            for (BusinessModuleProcessActor actor : actors) {
                if (matchesActorCode(actor, moduleActorCode.trim())) {
                    return actor;
                }
            }
        }
        return actors.get(0);
    }

    /**
     * 判断模块参与方是否匹配经典案例中的参与方编码。
     *
     * @param actor 模块参与方。
     * @param moduleActorCode 经典案例绑定的模块参与方编码。
     * @return true 表示匹配。
     */
    private boolean matchesActorCode(BusinessModuleProcessActor actor, String moduleActorCode) {
        return moduleActorCode.equals(actor.getActorType())
                || moduleActorCode.equals(actor.getActorRelation())
                || moduleActorCode.equals(actor.getRequiredOrgCode())
                || moduleActorCode.equals(actor.getRequiredRoleCode());
    }

    /**
     * 解析学习环境单位 ID。
     *
     * @param requestedOrgId 调用方显式传入的单位 ID。
     * @param defaultActor 模块默认参与方。
     * @return 最终传给第三方学习环境的单位 ID。
     */
    private String resolveRequiredExternalOrgId(String requestedOrgId, ResolvedClassicCaseActor defaultActor) {
        if (StringUtils.hasText(requestedOrgId)) {
            return requestedOrgId;
        }
        return defaultActor == null ? null : defaultActor.getRequiredExternalOrgId();
    }

    /**
     * 解析学习环境角色 ID。
     *
     * @param requestedRoleId 调用方显式传入的角色 ID。
     * @param defaultActor 模块默认参与方。
     * @return 最终传给第三方学习环境的角色 ID。
     */
    private String resolveRequiredExternalRoleId(String requestedRoleId, ResolvedClassicCaseActor defaultActor) {
        if (StringUtils.hasText(requestedRoleId)) {
            return requestedRoleId;
        }
        return defaultActor == null ? null : defaultActor.getRequiredExternalRoleId();
    }

    /**
     * 解析参与方类型。
     *
     * @param requestedActorType 调用方显式传入的参与方类型。
     * @param defaultActor 模块默认参与方。
     * @return 最终传给第三方学习环境的参与方类型。
     */
    private String resolveActorType(String requestedActorType, ResolvedClassicCaseActor defaultActor) {
        // actorType 必须来自教学平台模块参与方配置，避免前端把 teacher/student 等门户角色误传给原平台。
        return defaultActor == null ? null : defaultActor.getActorType();
    }

    private DataCreateRequestBuildService.ClassicCaseCreateContext buildSingleCreateContext(
            ClassicCaseGenerateRequest request,
            ClassicCaseAsset asset,
            ClassicCaseVersion version,
            TeachingDataTemplate template,
            String requestBatchId,
            String requestItemId,
            String traceId) {
        DataCreateRequestBuildService.ClassicCaseCreateContext context = buildBaseCreateContext(
                request.getTenantId(),
                asset,
                version,
                template,
                request.getUsageScene(),
                request.getSceneType(),
                requestBatchId,
                requestBatchId + ":" + requestItemId,
                traceId);
        context.setParticipantContextJson(trimToNull(request.getParticipantContextJson()));
        context.setItems(Collections.singletonList(buildCreateRequestItem(
                request,
                requestItemId,
                resolveClassicCaseActor(asset, version.getIdentityBindingJson(), request.getActorType()))));
        return context;
    }

    /**
     * 构造批量经典案例 DATA_CREATE 上下文。
     *
     * 业务功能：
     * 1. 学生批量 demo 只生成一次 Adapter 批量请求，避免每个学生单独调用第三方。
     * 2. 批量上下文只携带 caseDataFormat，不携带 generationRule 或正式环境完整 payload。
     *
     * @param request 经典案例批量生成请求。
     * @param asset 经典案例资产。
     * @param version 经典案例版本。
     * @param requestBatchId 请求批次 ID。
     * @param traceId 链路追踪 ID。
     * @param runtimeItems 运行时请求明细。
     * @return 经典案例 DATA_CREATE 构建上下文。
     */
    private DataCreateRequestBuildService.ClassicCaseCreateContext buildBatchCreateContext(
            ClassicCaseBatchGenerateRequest request,
            ClassicCaseAsset asset,
            ClassicCaseVersion version,
            TeachingDataTemplate template,
            String requestBatchId,
            String traceId,
            List<BatchRuntimeItem> runtimeItems) {
        DataCreateRequestBuildService.ClassicCaseCreateContext context = buildBaseCreateContext(
                request.getTenantId(),
                asset,
                version,
                template,
                request.getUsageScene(),
                request.getSceneType(),
                requestBatchId,
                requestBatchId,
                traceId);
        List<DataCreateRequestBuildService.DataCreateRequestItem> items = new ArrayList<>();
        for (BatchRuntimeItem runtimeItem : runtimeItems) {
            items.add(buildCreateRequestItem(
                    runtimeItem.getItem(),
                    runtimeItem.getRequestItemId(),
                    resolveClassicCaseActor(asset, version.getIdentityBindingJson(), runtimeItem.getItem().getActorType())));
        }
        context.setItems(items);
        return context;
    }

    /**
     * 构造经典案例 DATA_CREATE 基础上下文。
     *
     * @param tenantId 租户 ID。
     * @param asset 经典案例资产。
     * @param version 经典案例版本。
     * @param usageScene 使用场景。
     * @param sceneType 教学场景。
     * @param requestBatchId 请求批次 ID。
     * @param idempotencyKey 幂等键。
     * @param traceId 链路追踪 ID。
     * @return 基础上下文。
     */
    private DataCreateRequestBuildService.ClassicCaseCreateContext buildBaseCreateContext(
            String tenantId,
            ClassicCaseAsset asset,
            ClassicCaseVersion version,
            TeachingDataTemplate template,
            String usageScene,
            String sceneType,
            String requestBatchId,
            String idempotencyKey,
            String traceId) {
        DataCreateRequestBuildService.ClassicCaseCreateContext context =
                new DataCreateRequestBuildService.ClassicCaseCreateContext();
        context.setTenantId(tenantId);
        context.setConnectorSystemId(asset.getLearningConnectorSystemId());
        context.setModuleCode(asset.getModuleCode());
        context.setTemplateId(template.getId());
        context.setTemplateCode(template.getTemplateCode());
        context.setInitState(template.getInitState());
        context.setSceneType(sceneType.trim());
        context.setRequestBatchId(requestBatchId);
        context.setIdempotencyKey(idempotencyKey);
        context.setTraceId(traceId);
        context.setUsageScene(usageScene.trim());
        context.setCaseAssetId(asset.getId());
        context.setCaseCode(asset.getCaseCode());
        context.setCaseVersionId(firstText(version.getCaseVersionId(), version.getId()));
        context.setPayloadSchemaVersion(version.getPayloadSchemaVersion());
        context.setDesensitizedCasePayloadJson(version.getDesensitizedCasePayloadJson());
        context.setCaseDataFormatJson(version.getCaseDataFormatJson());
        context.setIdentityBindingJson(version.getIdentityBindingJson());
        return context;
    }

    /**
     * 构造单条经典案例 DATA_CREATE 参与方上下文。
     *
     * @param request 经典案例生成请求。
     * @param requestItemId 请求明细 ID。
     * @return 统一构建器参与方上下文。
     */
    private DataCreateRequestBuildService.DataCreateRequestItem buildCreateRequestItem(
            ClassicCaseGenerateRequest request,
            String requestItemId,
            ResolvedClassicCaseActor defaultActor) {
        DataCreateRequestBuildService.DataCreateRequestItem item =
                new DataCreateRequestBuildService.DataCreateRequestItem();
        item.setRequestItemId(requestItemId);
        item.setOwnerUserId(request.getOwnerUserId());
        item.setQuestionId(request.getQuestionId());
        item.setRequiredExternalOrgId(resolveRequiredExternalOrgId(request.getRequiredExternalOrgId(), defaultActor));
        item.setRequiredExternalRoleId(resolveRequiredExternalRoleId(request.getRequiredExternalRoleId(), defaultActor));
        item.setActorType(resolveActorType(request.getActorType(), defaultActor));
        item.setParticipantContextJson(request.getParticipantContextJson());
        return item;
    }

    /**
     * 构造批量经典案例 DATA_CREATE 参与方上下文。
     *
     * @param source 批量生成明细。
     * @param requestItemId 请求明细 ID。
     * @return 统一构建器参与方上下文。
     */
    private DataCreateRequestBuildService.DataCreateRequestItem buildCreateRequestItem(
            ClassicCaseBatchGenerateItemRequest source,
            String requestItemId,
            ResolvedClassicCaseActor defaultActor) {
        DataCreateRequestBuildService.DataCreateRequestItem item =
                new DataCreateRequestBuildService.DataCreateRequestItem();
        item.setRequestItemId(requestItemId);
        item.setOwnerUserId(source.getOwnerUserId());
        item.setQuestionId(source.getQuestionId());
        item.setRequiredExternalOrgId(resolveRequiredExternalOrgId(source.getRequiredExternalOrgId(), defaultActor));
        item.setRequiredExternalRoleId(resolveRequiredExternalRoleId(source.getRequiredExternalRoleId(), defaultActor));
        item.setActorType(resolveActorType(source.getActorType(), defaultActor));
        item.setParticipantContextJson(source.getParticipantContextJson());
        return item;
    }

    /**
     * 将教学平台内部使用场景翻译为第三方造数协议中的经典案例生成模式。
     *
     * @param usageScene 教学平台内部经典案例使用场景。
     * @return 第三方学习环境可以直接识别的生成模式。
     */
    private String resolveGenerationMode(String usageScene) {
        if (ClassicCaseRuntimeConstants.USAGE_SCENE_TEACHING_REPLICA.equals(usageScene.trim())) {
            return ClassicCaseRuntimeConstants.GENERATION_MODE_REPLAY_CASE;
        }
        return ClassicCaseRuntimeConstants.GENERATION_MODE_FORMAT_DEMO;
    }

    /**
     * 构造批量生成运行时明细，补齐未传入的 requestItemId。
     *
     * @param request 批量生成请求。
     * @return 运行时明细。
     */
    private List<BatchRuntimeItem> buildBatchRuntimeItems(ClassicCaseBatchGenerateRequest request) {
        List<BatchRuntimeItem> runtimeItems = new ArrayList<>();
        for (ClassicCaseBatchGenerateItemRequest item : request.getItems()) {
            String requestItemId = firstText(item.getRequestItemId(), "classic-case-item-" + UUID.randomUUID());
            runtimeItems.add(new BatchRuntimeItem(requestItemId, item));
        }
        return runtimeItems;
    }

    /**
     * 构造批量调用原平台学习环境的请求项。
     *
     * @param runtimeItems 运行时明细。
     * @return 原平台请求项。
     */
    private List<OriginDataPrepareAdapter.RequestItem> buildBatchRequestItems(List<BatchRuntimeItem> runtimeItems) {
        List<OriginDataPrepareAdapter.RequestItem> requestItems = new ArrayList<>();
        for (BatchRuntimeItem runtimeItem : runtimeItems) {
            ClassicCaseBatchGenerateItemRequest source = runtimeItem.getItem();
            OriginDataPrepareAdapter.RequestItem item = new OriginDataPrepareAdapter.RequestItem();
            item.setRequestItemId(runtimeItem.getRequestItemId());
            item.setStudentId(source.getOwnerUserId());
            item.setQuestionId(trimToNull(source.getQuestionId()));
            item.setRequiredExternalOrgId(trimToNull(source.getRequiredExternalOrgId()));
            item.setRequiredExternalRoleId(trimToNull(source.getRequiredExternalRoleId()));
            item.setActorType(trimToNull(source.getActorType()));
            item.setDataScopeJson(trimToNull(source.getParticipantContextJson()));
            requestItems.add(item);
        }
        return requestItems;
    }

    /**
     * 将批量请求项转换为单条生成请求，复用 usage 和 teaching_data_instance 构造逻辑。
     *
     * @param request 批量请求。
     * @param item 批量请求项。
     * @param requestItemId 请求项 ID。
     * @return 单条生成请求。
     */
    private ClassicCaseGenerateRequest buildGenerateRequestFromBatchItem(ClassicCaseBatchGenerateRequest request,
                                                                         ClassicCaseBatchGenerateItemRequest item,
                                                                         String requestItemId) {
        ClassicCaseGenerateRequest single = new ClassicCaseGenerateRequest();
        single.setTenantId(request.getTenantId());
        single.setCaseAssetId(request.getCaseAssetId());
        single.setCaseVersionId(request.getCaseVersionId());
        single.setUsageScene(request.getUsageScene());
        single.setSceneType(request.getSceneType());
        single.setTaskId(request.getTaskId());
        single.setOwnerUserId(item.getOwnerUserId());
        single.setQuestionId(item.getQuestionId());
        single.setRequestBatchId(request.getRequestBatchId());
        single.setRequestItemId(requestItemId);
        single.setTraceId(request.getTraceId());
        single.setParticipantContextJson(item.getParticipantContextJson());
        single.setRequiredExternalOrgId(item.getRequiredExternalOrgId());
        single.setRequiredExternalRoleId(item.getRequiredExternalRoleId());
        single.setActorType(item.getActorType());
        return single;
    }

    /**
     * 获取第一条原平台生成结果，当前经典案例生成接口先保持单条语义。
     *
     * @param response 原平台批量创建响应。
     * @return 第一条生成结果。
     */
    private OriginDataPrepareAdapter.ResponseItem firstResponseItem(OriginDataPrepareAdapter.BatchCreateResponse response) {
        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
        OriginDataPrepareAdapter.ResponseItem item = response.getItems().get(0);
        if (item == null || !StringUtils.hasText(item.getExternalBusinessId())) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
        return item;
    }

    /**
     * 按 requestItemId 整理原平台批量响应。
     *
     * @param response 原平台批量响应。
     * @return 响应项索引。
     */
    private Map<String, OriginDataPrepareAdapter.ResponseItem> responseItemsByRequestItemId(
            OriginDataPrepareAdapter.BatchCreateResponse response) {
        if (response == null || response.getItems() == null || response.getItems().isEmpty()) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
        Map<String, OriginDataPrepareAdapter.ResponseItem> itemMap = new HashMap<>();
        for (OriginDataPrepareAdapter.ResponseItem item : response.getItems()) {
            if (item == null || !StringUtils.hasText(item.getRequestItemId())) {
                throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
            }
            itemMap.put(item.getRequestItemId(), item);
        }
        return itemMap;
    }

    /**
     * 获取指定 requestItemId 的原平台响应项。
     *
     * @param responseItems 响应项索引。
     * @param requestItemId 请求项 ID。
     * @return 响应项。
     */
    private OriginDataPrepareAdapter.ResponseItem requireResponseItem(
            Map<String, OriginDataPrepareAdapter.ResponseItem> responseItems,
            String requestItemId) {
        OriginDataPrepareAdapter.ResponseItem item = responseItems.get(requestItemId);
        if (item == null || !StringUtils.hasText(item.getExternalBusinessId())) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
        return item;
    }

    /**
     * 构造经典案例使用记录，把学习环境返回的业务数据引用固定下来。
     *
     * @param request 生成请求。
     * @param asset 案例资产。
     * @param version 案例版本。
     * @param requestBatchId 请求批次 ID。
     * @param requestItemId 请求项 ID。
     * @param traceId 链路追踪 ID。
     * @param requestJson 请求快照。
     * @param response 原平台批量创建响应。
     * @param responseItem 原平台单条生成结果。
     * @return 使用记录。
     */
    private ClassicCaseUsage buildUsage(ClassicCaseGenerateRequest request,
                                        ClassicCaseAsset asset,
                                        ClassicCaseVersion version,
                                        String usageId,
                                        String teachingDataInstanceId,
                                        String requestBatchId,
                                        String requestItemId,
                                        String traceId,
                                        String requestJson,
                                        OriginDataPrepareAdapter.BatchCreateResponse response,
                                        OriginDataPrepareAdapter.ResponseItem responseItem) {
        ClassicCaseUsage usage = new ClassicCaseUsage();
        usage.setId(usageId);
        usage.setTenantId(request.getTenantId());
        usage.setCaseAssetId(asset.getId());
        usage.setCaseVersionId(version.getId());
        usage.setUsageScene(request.getUsageScene().trim());
        usage.setSceneType(request.getSceneType().trim());
        usage.setTaskId(trimToNull(request.getTaskId()));
        usage.setRequestBatchId(requestBatchId);
        usage.setRequestItemId(requestItemId);
        usage.setOwnerUserId(request.getOwnerUserId());
        usage.setQuestionId(trimToNull(request.getQuestionId()));
        usage.setGeneratedInstanceId(responseItem.getExternalBusinessId());
        usage.setTeachingDataInstanceId(teachingDataInstanceId);
        usage.setExternalBusinessNo(responseItem.getExternalBusinessNo());
        usage.setExternalBusinessName(responseItem.getExternalBusinessName());
        usage.setExternalStatus(responseItem.getExternalStatus());
        usage.setTargetUrl(responseItem.getTargetUrl());
        usage.setRequestJson(requestJson);
        usage.setResultJson(toJson(response));
        usage.setUseBy(request.getOwnerUserId());
        usage.setUseTime(LocalDateTime.now());
        usage.setTraceId(traceId);
        usage.setStatus(ClassicCaseRuntimeConstants.STATUS_SUCCESS);
        usage.setDeleted(Boolean.FALSE);
        return usage;
    }

    /**
     * 构造原平台启动上下文。
     *
     * 业务功能：
     * 1. 将经典案例生成出的 teaching_data_instance 转换为现有 launchToken 服务可校验的启动上下文。
     * 2. 不在经典案例链路中重新实现 token、过期时间和实例状态校验，统一交给 PlatformLaunchContextService。
     *
     * 关键流程：
     * 1. 使用实例中的学习环境平台、业务数据 ID、目标地址和校验快照。
     * 2. 使用请求中的 sdkMode 和 segmentNo 描述本次进入原平台的 SDK 形态和流程段。
     *
     * @param request 生成并启动请求。
     * @param usage 经典案例使用记录。
     * @param instance 教学数据实例。
     * @return 原平台启动上下文。
     */
    private PlatformLaunchContext buildLaunchContext(ClassicCaseGenerateLaunchRequest request,
                                                     ClassicCaseUsage usage,
                                                     TeachingDataInstance instance) {
        PlatformLaunchContext launchContext = new PlatformLaunchContext();
        launchContext.setId(UUID.randomUUID().toString().replace("-", ""));
        launchContext.setTenantId(request.getTenantId());
        launchContext.setUserId(request.getOwnerUserId());
        launchContext.setConnectorSystemId(instance.getConnectorSystemId());
        launchContext.setTaskId(trimToNull(request.getTaskId()));
        launchContext.setTeachingPointId(instance.getTeachingPointId());
        launchContext.setDataInstanceId(instance.getId());
        launchContext.setSceneType(request.getSceneType().trim());
        launchContext.setSdkMode(request.getSdkMode().trim());
        launchContext.setTargetUrl(instance.getTargetUrl());
        launchContext.setSegmentNo(request.getSegmentNo());
        launchContext.setActorType(instance.getActorType());
        launchContext.setRequiredExternalOrgId(instance.getRequiredExternalOrgId());
        launchContext.setRequiredExternalRoleId(instance.getRequiredExternalRoleId());
        launchContext.setExternalBusinessId(instance.getExternalBusinessId());
        launchContext.setExternalBusinessNo(instance.getExternalBusinessNo());
        launchContext.setDataScopeJson(instance.getRequirementSnapshotJson());
        launchContext.setCreateBy(request.getOwnerUserId());
        launchContext.setUpdateBy(request.getOwnerUserId());
        return launchContext;
    }

    /**
     * 构造可被现有 launchToken 链路识别的教学数据实例。
     *
     * 业务功能：
     * 1. 将经典案例学习环境 DATA_CREATE 的原平台业务 ID 转换为 teaching_data_instance。
     * 2. 保持 generatedInstanceId 与教学平台 dataInstanceId 的语义隔离，避免后续启动链路误把原平台业务 ID 当成本地实例 ID。
     *
     * 关键流程：
     * 1. 使用案例资产绑定的学习环境、模块、教学点补齐实例归属。
     * 2. 将生成请求快照写入 requirementSnapshotJson，供启动上下文作为 dataScopeJson 复用。
     * 3. 将实例置为 READY 且校验状态置为 PASSED，使 PlatformLaunchContextService 的既有校验可以直接通过。
     *
     * @param request 经典案例生成请求。
     * @param asset 经典案例资产。
     * @param version 经典案例版本。
     * @param usageId 经典案例使用记录 ID。
     * @param requestBatchId 请求批次 ID。
     * @param requestItemId 请求项 ID。
     * @param requestJson 请求快照 JSON。
     * @param responseItem 原平台单条生成结果。
     * @return 可启动的教学数据实例。
     */
    private TeachingDataInstance buildTeachingDataInstance(ClassicCaseGenerateRequest request,
                                                           ClassicCaseAsset asset,
                                                           ClassicCaseVersion version,
                                                           String usageId,
                                                           String requestBatchId,
                                                           String requestItemId,
                                                           String requestJson,
                                                           OriginDataPrepareAdapter.ResponseItem responseItem) {
        LocalDateTime now = LocalDateTime.now();
        TeachingDataInstance instance = new TeachingDataInstance();
        instance.setId(UUID.randomUUID().toString());
        instance.setTenantId(request.getTenantId());
        instance.setTemplateId(ClassicCaseRuntimeConstants.TEMPLATE_ID_PREFIX + version.getId());
        instance.setConnectorSystemId(asset.getLearningConnectorSystemId());
        instance.setOwnerUserId(request.getOwnerUserId());
        instance.setTaskId(trimToNull(request.getTaskId()));
        instance.setTeachingPointId(trimToNull(asset.getTeachingPointId()));
        instance.setSceneType(request.getSceneType().trim());
        instance.setModuleCode(asset.getModuleCode());
        instance.setRequestBatchId(requestBatchId);
        instance.setRequestItemId(requestItemId);
        instance.setExternalBusinessId(responseItem.getExternalBusinessId());
        instance.setExternalBusinessNo(responseItem.getExternalBusinessNo());
        instance.setExternalStatus(responseItem.getExternalStatus());
        instance.setRequiredExternalOrgId(trimToNull(request.getRequiredExternalOrgId()));
        instance.setRequiredExternalRoleId(trimToNull(request.getRequiredExternalRoleId()));
        instance.setActorType(trimToNull(request.getActorType()));
        instance.setTargetUrl(responseItem.getTargetUrl());
        instance.setRequirementSnapshotJson(requestJson);
        instance.setValidationStatus(ValidationStatus.PASSED.getValue());
        instance.setValidationTime(now);
        instance.setValidationResultJson(buildClassicCaseValidationSnapshot(asset, version, usageId));
        instance.setInstanceStatus(DataInstanceStatus.READY.getValue());
        instance.setResetCount(0L);
        instance.setMetadataJson(buildClassicCaseInstanceMetadata(request, asset, version, usageId));
        instance.setLockVersion(0L);
        instance.setCreateBy(request.getOwnerUserId());
        instance.setCreateTime(now);
        instance.setUpdateBy(request.getOwnerUserId());
        instance.setUpdateTime(now);
        instance.setStatus(RecordStatus.ACTIVE.getValue());
        instance.setDeleted(Boolean.FALSE);
        return instance;
    }

    /**
     * 构造经典案例实例的校验快照。
     *
     * @param asset 经典案例资产。
     * @param version 经典案例版本。
     * @param usageId 使用记录 ID。
     * @return 校验快照 JSON。
     */
    private String buildClassicCaseValidationSnapshot(ClassicCaseAsset asset,
                                                      ClassicCaseVersion version,
                                                      String usageId) {
        ObjectNode root = JSON_MAPPER.createObjectNode();
        root.put("source", ClassicCaseRuntimeConstants.REQUEST_MODE_CLASSIC_CASE);
        root.put(ClassicCaseRuntimeConstants.FIELD_CLASSIC_CASE_ASSET_ID, asset.getId());
        root.put(ClassicCaseRuntimeConstants.FIELD_CLASSIC_CASE_VERSION_ID, version.getId());
        root.put("classicCaseUsageId", usageId);
        root.put("validationStatus", ValidationStatus.PASSED.getValue());
        return root.toString();
    }

    /**
     * 构造经典案例实例元数据。
     *
     * @param request 经典案例生成请求。
     * @param asset 经典案例资产。
     * @param version 经典案例版本。
     * @param usageId 使用记录 ID。
     * @return 元数据 JSON。
     */
    private String buildClassicCaseInstanceMetadata(ClassicCaseGenerateRequest request,
                                                    ClassicCaseAsset asset,
                                                    ClassicCaseVersion version,
                                                    String usageId) {
        ObjectNode root = JSON_MAPPER.createObjectNode();
        root.put("requestMode", ClassicCaseRuntimeConstants.REQUEST_MODE_CLASSIC_CASE);
        root.put("usageScene", request.getUsageScene().trim());
        root.put(ClassicCaseRuntimeConstants.FIELD_CLASSIC_CASE_ASSET_ID, asset.getId());
        root.put(ClassicCaseRuntimeConstants.FIELD_CLASSIC_CASE_VERSION_ID, version.getId());
        root.put("classicCaseUsageId", usageId);
        return root.toString();
    }

    /**
     * 经典案例内部生成结果。
     *
     * 业务功能：
     * 1. 在同一个事务内把 usage 和 teaching_data_instance 一起返回给启动编排逻辑。
     * 2. 避免为了创建 launchToken 再从数据库反查刚刚写入的实例。
     */
    private static class GeneratedClassicCaseData {

        private final ClassicCaseUsage usage;

        private final TeachingDataInstance instance;

        GeneratedClassicCaseData(ClassicCaseUsage usage, TeachingDataInstance instance) {
            this.usage = usage;
            this.instance = instance;
        }

        ClassicCaseUsage getUsage() {
            return usage;
        }

        TeachingDataInstance getInstance() {
            return instance;
        }
    }

    /**
     * 批量生成运行时请求项。
     *
     * 业务功能：
     * 1. 固定服务端补齐后的 requestItemId。
     * 2. 避免修改入参 DTO，同时保证请求、响应、usage 和教学数据实例使用同一个明细 ID。
     */
    private static class BatchRuntimeItem {

        private final String requestItemId;

        private final ClassicCaseBatchGenerateItemRequest item;

        BatchRuntimeItem(String requestItemId, ClassicCaseBatchGenerateItemRequest item) {
            this.requestItemId = requestItemId;
            this.item = item;
        }

        String getRequestItemId() {
            return requestItemId;
        }

        ClassicCaseBatchGenerateItemRequest getItem() {
            return item;
        }
    }

    /**
     * 校验正式环境、学习环境和业务模块归属关系，防止生产案例误落到生产环境运行。
     *
     * @param request 导入请求。
     * @param sourceSystem 来源正式环境。
     * @param learningSystem 目标学习环境。
     * @param businessModule 业务模块。
     */
    private void validateEnvironmentBinding(ClassicCaseImportRequest request,
                                            ConnectorSystem sourceSystem,
                                            ConnectorSystem learningSystem,
                                            BusinessModule businessModule) {
        if (!request.getTenantId().equals(sourceSystem.getTenantId())
                || !request.getTenantId().equals(learningSystem.getTenantId())
                || !request.getTenantId().equals(businessModule.getTenantId())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (!ENVIRONMENT_TYPE_PROD.equals(sourceSystem.getEnvironmentType())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (!ENVIRONMENT_TYPE_LEARNING.equals(learningSystem.getEnvironmentType())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (!StringUtils.hasText(sourceSystem.getEnvironmentGroupCode())
                || !sourceSystem.getEnvironmentGroupCode().equals(learningSystem.getEnvironmentGroupCode())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (!learningSystem.getId().equals(businessModule.getConnectorSystemId())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
        if (!request.getModuleCode().equals(businessModule.getModuleCode())) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 查询未删除的平台配置，避免经典案例绑定到已经废弃的平台。
     *
     * @param id 平台 ID。
     * @return 原平台配置。
     */
    private ConnectorSystem getConnectorSystem(String id) {
        ConnectorSystem system = connectorSystemMapper.selectOne(new QueryWrapper<ConnectorSystem>()
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (system == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return system;
    }

    /**
     * 查询未删除业务模块，经典案例必须复用教学平台已有模块配置。
     *
     * @param id 业务模块 ID。
     * @return 业务模块。
     */
    private BusinessModule getBusinessModule(String id) {
        BusinessModule businessModule = businessModuleMapper.selectOne(new QueryWrapper<BusinessModule>()
                .eq("id", id)
                .eq("deleted", Boolean.FALSE));
        if (businessModule == null) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return businessModule;
    }

    /**
     * 按租户和案例编码查询现有案例资产，用于重复导入时新增版本。
     *
     * @param tenantId 租户 ID。
     * @param caseCode 案例编码。
     * @return 已存在的案例资产，未命中时返回 null。
     */
    private ClassicCaseAsset findAsset(String tenantId, String sourceConnectorSystemId, String caseCode) {
        return classicCaseAssetMapper.selectOne(new QueryWrapper<ClassicCaseAsset>()
                .eq("tenant_id", tenantId)
                .eq("source_connector_system_id", sourceConnectorSystemId)
                .eq("case_code", caseCode)
                .eq("deleted", Boolean.FALSE));
    }

    private ClassicCaseVersion findVersionByExternalId(String tenantId,
                                                       String caseAssetId,
                                                       String caseVersionId) {
        return classicCaseVersionMapper.selectOne(new QueryWrapper<ClassicCaseVersion>()
                .eq("tenant_id", tenantId)
                .eq("case_asset_id", caseAssetId)
                .eq("case_version_id", caseVersionId.trim())
                .eq("deleted", Boolean.FALSE)
                .last("limit 1"));
    }

    /**
     * 创建新的经典案例资产，资产只保存索引和绑定关系，具体脱敏内容进入版本表。
     *
     * @param request 导入请求。
     * @param sourceSystem 来源正式环境。
     * @param learningSystem 目标学习环境。
     * @param sceneTypesJson 已规范化场景 JSON。
     * @return 新资产实体。
     */
    private ClassicCaseAsset buildNewAsset(ClassicCaseImportRequest request,
                                           ConnectorSystem sourceSystem,
                                           ConnectorSystem learningSystem,
                                           String sceneTypesJson) {
        ClassicCaseAsset asset = new ClassicCaseAsset();
        asset.setId(UUID.randomUUID().toString());
        asset.setTenantId(request.getTenantId());
        asset.setCaseCode(request.getCaseCode().trim());
        asset.setCreateBy(request.getCreateBy());
        asset.setCreateTime(LocalDateTime.now());
        asset.setLockVersion(0L);
        asset.setDeleted(Boolean.FALSE);
        asset.setStatus(RecordStatus.ACTIVE.getValue());
        patchAsset(asset, request, sourceSystem, learningSystem, sceneTypesJson);
        return asset;
    }

    /**
     * 更新案例资产的可变索引字段，重复导入时允许标题、说明、教学点和目标模块随新版本调整。
     *
     * @param asset 案例资产。
     * @param request 导入请求。
     * @param sourceSystem 来源正式环境。
     * @param learningSystem 目标学习环境。
     * @param sceneTypesJson 已规范化场景 JSON。
     */
    private void patchAsset(ClassicCaseAsset asset,
                            ClassicCaseImportRequest request,
                            ConnectorSystem sourceSystem,
                            ConnectorSystem learningSystem,
                            String sceneTypesJson) {
        asset.setCaseTitle(request.getCaseTitle().trim());
        asset.setCaseSummary(trimToNull(request.getCaseSummary()));
        asset.setSourceConnectorSystemId(sourceSystem.getId());
        asset.setLearningConnectorSystemId(learningSystem.getId());
        asset.setEnvironmentGroupCode(sourceSystem.getEnvironmentGroupCode());
        asset.setBusinessModuleId(request.getBusinessModuleId());
        asset.setModuleCode(request.getModuleCode());
        asset.setTeachingPointId(trimToNull(request.getTeachingPointId()));
        asset.setSceneTypesJson(sceneTypesJson);
        asset.setTagsJson(StringUtils.hasText(request.getTagsJson()) ? request.getTagsJson() : "[]");
        asset.setSourceUpdatedAt(request.getSourceUpdatedAt());
        asset.setDisableReason(null);
        asset.setUpdateBy(request.getCreateBy());
        asset.setUpdateTime(LocalDateTime.now());
    }

    /**
     * 计算下一个版本号，同一案例资产内版本号单调递增。
     *
     * @param asset 案例资产。
     * @return 下一个版本号。
     */
    private int nextVersionNo(ClassicCaseAsset asset) {
        ClassicCaseVersion latest = classicCaseVersionMapper.selectOne(new QueryWrapper<ClassicCaseVersion>()
                .eq("tenant_id", asset.getTenantId())
                .eq("case_asset_id", asset.getId())
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("version_no")
                .last("limit 1"));
        return latest == null || latest.getVersionNo() == null ? 1 : latest.getVersionNo() + 1;
    }

    /**
     * 构造经典案例版本，版本表持有后续运行所需的完整脱敏数据和学生格式。
     *
     * @param request 导入请求。
     * @param asset 案例资产。
     * @param versionNo 版本号。
     * @param desensitizedPayloadJson 已规范化脱敏 payload。
     * @param caseDataFormatJson 已规范化学生数据格式。
     * @param identityBindingJson 已规范化身份绑定。
     * @param desensitizePolicyJson 已规范化脱敏策略，可为空。
     * @return 案例版本。
     */
    private ClassicCaseVersion buildVersion(ClassicCaseImportRequest request,
                                            ClassicCaseAsset asset,
                                            int versionNo,
                                            String desensitizedPayloadJson,
                                            String caseDataFormatJson,
                                            String identityBindingJson,
                                            String desensitizePolicyJson,
                                            String supportedGenerationModesJson,
                                            String contentHash) {
        ClassicCaseVersion version = new ClassicCaseVersion();
        version.setId(UUID.randomUUID().toString());
        version.setTenantId(request.getTenantId());
        version.setCaseAssetId(asset.getId());
        version.setVersionNo(versionNo);
        version.setCaseVersionId(StringUtils.hasText(request.getCaseVersionId())
                ? request.getCaseVersionId().trim()
                : version.getId());
        version.setPayloadSchemaVersion(StringUtils.hasText(request.getPayloadSchemaVersion())
                ? request.getPayloadSchemaVersion().trim()
                : DEFAULT_PAYLOAD_SCHEMA_VERSION);
        version.setDesensitizedCasePayloadJson(desensitizedPayloadJson);
        version.setCaseDataFormatJson(caseDataFormatJson);
        version.setIdentityBindingJson(identityBindingJson);
        version.setSupportedGenerationModesJson(supportedGenerationModesJson);
        version.setDesensitizePolicyJson(desensitizePolicyJson);
        version.setPayloadHash(sha256(desensitizedPayloadJson));
        version.setContentHash(contentHash);
        version.setCreateBy(request.getCreateBy());
        version.setCreateTime(LocalDateTime.now());
        version.setStatus(ClassicCaseRuntimeConstants.STATUS_AVAILABLE);
        version.setDeleted(Boolean.FALSE);
        return version;
    }

    private String normalizeSupportedGenerationModes(String modesJson,
                                                       boolean hasReplayPayload,
                                                       boolean hasCaseDataFormat) {
        List<String> modes = new ArrayList<>();
        if (StringUtils.hasText(modesJson)) {
            JsonNode node = parseJsonNode(modesJson, true);
            for (JsonNode item : node) {
                if (!item.isTextual()) {
                    throw new BusinessException(ApiResultCode.PARAM_ERROR.getCode(), "生成模式必须为字符串");
                }
                String mode = item.asText().trim();
                if (!ClassicCaseRuntimeConstants.GENERATION_MODE_REPLAY_CASE.equals(mode)
                        && !ClassicCaseRuntimeConstants.GENERATION_MODE_FORMAT_DEMO.equals(mode)) {
                    throw new BusinessException(ApiResultCode.PARAM_ERROR.getCode(), "不支持的经典案例生成模式");
                }
                if (!modes.contains(mode)) {
                    modes.add(mode);
                }
            }
        } else {
            if (hasReplayPayload) {
                modes.add(ClassicCaseRuntimeConstants.GENERATION_MODE_REPLAY_CASE);
            }
            if (hasCaseDataFormat) {
                modes.add(ClassicCaseRuntimeConstants.GENERATION_MODE_FORMAT_DEMO);
            }
        }
        if (modes.isEmpty()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR.getCode(), "至少需要一种经典案例生成模式");
        }
        if (modes.contains(ClassicCaseRuntimeConstants.GENERATION_MODE_REPLAY_CASE) && !hasReplayPayload) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                    "REPLAY_CASE 模式缺少 desensitizedCasePayload");
        }
        if (modes.contains(ClassicCaseRuntimeConstants.GENERATION_MODE_FORMAT_DEMO) && !hasCaseDataFormat) {
            throw new BusinessException(ApiResultCode.DATA_PREPARE_CONFIG_INCOMPLETE.getCode(),
                    "FORMAT_DEMO 模式缺少 caseDataFormat");
        }
        return toJson(modes);
    }

    private String buildContentHash(ClassicCaseImportRequest request,
                                    String moduleCode,
                                    String tagsJson,
                                    String supportedGenerationModesJson,
                                    String identityBindingJson,
                                    String desensitizedPayloadJson,
                                    String caseDataFormatJson) {
        String canonical = firstText(request.getCaseTitle(), "") + "\n"
                + firstText(moduleCode, "") + "\n"
                + firstText(request.getPayloadSchemaVersion(), DEFAULT_PAYLOAD_SCHEMA_VERSION) + "\n"
                + firstText(request.getCaseSummary(), "") + "\n"
                + supportedGenerationModesJson + "\n"
                + tagsJson + "\n"
                + identityBindingJson + "\n"
                + desensitizedPayloadJson + "\n"
                + caseDataFormatJson + "\n"
                + (request.getSourceUpdatedAt() == null ? "" : request.getSourceUpdatedAt().toString());
        return sha256(canonical);
    }

    /**
     * 直接核对版本表和资产表中的不可变内容，用于修复已经完成模块迁移但仍保留旧模块哈希的数据。
     * 模块、学习环境和业务模块属于资产归属元数据，不参与此处的内容一致性判断。
     */
    private boolean matchesStoredContentExceptModule(ClassicCaseImportRequest request,
                                                     ClassicCaseAsset asset,
                                                     ClassicCaseVersion version,
                                                     String tagsJson,
                                                     String supportedGenerationModesJson,
                                                     String identityBindingJson,
                                                     String desensitizedPayloadJson,
                                                     String caseDataFormatJson) {
        String payloadSchemaVersion = StringUtils.hasText(request.getPayloadSchemaVersion())
                ? request.getPayloadSchemaVersion().trim()
                : DEFAULT_PAYLOAD_SCHEMA_VERSION;
        return Objects.equals(request.getCaseTitle().trim(), asset.getCaseTitle())
                && Objects.equals(trimToNull(request.getCaseSummary()), asset.getCaseSummary())
                && jsonContentEquals(tagsJson, asset.getTagsJson(), true)
                && Objects.equals(request.getSourceUpdatedAt(), asset.getSourceUpdatedAt())
                && Objects.equals(payloadSchemaVersion, version.getPayloadSchemaVersion())
                && jsonContentEquals(
                        supportedGenerationModesJson, version.getSupportedGenerationModesJson(), true)
                && jsonContentEquals(identityBindingJson, version.getIdentityBindingJson(), false)
                && jsonContentEquals(
                        desensitizedPayloadJson, version.getDesensitizedCasePayloadJson(), false)
                && jsonContentEquals(caseDataFormatJson, version.getCaseDataFormatJson(), false);
    }

    private boolean jsonContentEquals(String incomingJson,
                                      String storedJson,
                                      boolean arrayRequired) {
        if (!StringUtils.hasText(incomingJson) || !StringUtils.hasText(storedJson)) {
            return !StringUtils.hasText(incomingJson) && !StringUtils.hasText(storedJson);
        }
        return Objects.equals(
                parseJsonNode(incomingJson, arrayRequired),
                parseJsonNode(storedJson, arrayRequired));
    }

    private Set<String> supportedGenerationModes(ClassicCaseVersion version) {
        Set<String> result = new LinkedHashSet<>();
        String json = version == null ? null : version.getSupportedGenerationModesJson();
        if (!StringUtils.hasText(json)) {
            result.add(ClassicCaseRuntimeConstants.GENERATION_MODE_REPLAY_CASE);
            result.add(ClassicCaseRuntimeConstants.GENERATION_MODE_FORMAT_DEMO);
            return result;
        }
        JsonNode node = parseJsonNode(json, true);
        for (JsonNode item : node) {
            if (item.isTextual() && StringUtils.hasText(item.asText())) {
                result.add(item.asText().trim());
            }
        }
        return result;
    }

    private LessonPlanClassicCaseOptionVO toClassicCaseOption(ClassicCaseAsset asset,
                                                               ClassicCaseVersion version) {
        LessonPlanClassicCaseOptionVO option = new LessonPlanClassicCaseOptionVO();
        option.setClassicCaseId(asset.getId());
        option.setConnectorSystemId(asset.getSourceConnectorSystemId());
        option.setLearningConnectorSystemId(asset.getLearningConnectorSystemId());
        option.setCaseCode(asset.getCaseCode());
        option.setCaseName(asset.getCaseTitle());
        option.setBusinessModuleCode(asset.getModuleCode());
        option.setSummary(asset.getCaseSummary());
        option.setTags(parseStringArray(asset.getTagsJson()));
        option.setCaseVersionId(firstText(version.getCaseVersionId(), version.getId()));
        option.setVersionNo(version.getVersionNo());
        option.setPayloadSchemaVersion(version.getPayloadSchemaVersion());
        List<String> modes = new ArrayList<>(supportedGenerationModes(version));
        option.setSupportedGenerationModes(modes);
        option.setDefaultGenerationMode(modes.contains(ClassicCaseRuntimeConstants.GENERATION_MODE_REPLAY_CASE)
                ? ClassicCaseRuntimeConstants.GENERATION_MODE_REPLAY_CASE
                : ClassicCaseRuntimeConstants.GENERATION_MODE_FORMAT_DEMO);
        return option;
    }

    private List<String> parseStringArray(String json) {
        if (!StringUtils.hasText(json)) {
            return Collections.emptyList();
        }
        JsonNode node = parseJsonNode(json, true);
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item.isTextual() && StringUtils.hasText(item.asText())) {
                values.add(item.asText().trim());
            }
        }
        return values;
    }

    private void validateObjectNode(JsonNode node, String fieldName) {
        if (node == null || node.isNull() || !node.isObject()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR.getCode(), fieldName + " 必须是 JSON 对象");
        }
    }

    private void validateOptionalObjectNode(JsonNode node, String fieldName) {
        if (node != null && !node.isNull() && !node.isObject()) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR.getCode(), fieldName + " 必须是 JSON 对象");
        }
    }

    private LocalDateTime parseSourceUpdatedAt(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException ex) {
                throw new BusinessException(ApiResultCode.PARAM_ERROR.getCode(), "sourceUpdatedAt 格式错误");
            }
        }
    }

    /**
     * 规范化 JSON 文本，避免同一 payload 因空格和换行差异导致哈希或审计比较不稳定。
     *
     * @param json JSON 文本。
     * @param arrayRequired 是否必须是 JSON 数组。
     * @return 规范化后的 JSON 字符串。
     */
    private String normalizeJson(String json, boolean arrayRequired) {
        try {
            JsonNode node = JSON_MAPPER.readTree(json);
            if (node == null || (arrayRequired && !node.isArray()) || (!arrayRequired && !node.isObject())) {
                throw new BusinessException(ApiResultCode.PARAM_ERROR);
            }
            return node.toString();
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 解析 JSON 节点，运行时组装请求需要保留对象结构而不是二次转义字符串。
     *
     * @param json JSON 文本。
     * @param arrayRequired 是否必须是数组。
     * @return JSON 节点。
     */
    private JsonNode parseJsonNode(String json, boolean arrayRequired) {
        try {
            JsonNode node = JSON_MAPPER.readTree(json);
            if (node == null || (arrayRequired && !node.isArray()) || (!arrayRequired && !node.isObject())) {
                throw new BusinessException(ApiResultCode.PARAM_ERROR);
            }
            return node;
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 序列化对象为 JSON，用于保存原平台响应快照。
     *
     * @param value 待序列化对象。
     * @return JSON 字符串。
     */
    private String toJson(Object value) {
        try {
            return JSON_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 返回第一个非空文本，减少批次号和追踪号默认值判断散落。
     *
     * @param values 候选文本。
     * @return 第一个有效文本。
     */
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

    /**
     * 生成脱敏 payload 哈希，用于审计和识别重复脱敏内容。
     *
     * @param text 规范化后的脱敏 payload。
     * @return SHA-256 十六进制字符串。
     */
    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte item : hash) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new BusinessException(ApiResultCode.SYSTEM_ERROR);
        }
    }

    /**
     * 校验文本字段不能为空。
     *
     * @param value 待校验文本。
     */
    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 将可选文本规范为 null 或去空格后的值，避免数据库保存无意义空白。
     *
     * @param value 原始文本。
     * @return 规范化文本。
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 经典案例运行时解析出的默认参与方。
     */
    private static class ResolvedClassicCaseActor {

        private final String requiredExternalOrgId;

        private final String requiredExternalRoleId;

        private final String actorType;

        private ResolvedClassicCaseActor(String requiredExternalOrgId,
                                         String requiredExternalRoleId,
                                         String actorType) {
            this.requiredExternalOrgId = requiredExternalOrgId;
            this.requiredExternalRoleId = requiredExternalRoleId;
            this.actorType = actorType;
        }

        private String getRequiredExternalOrgId() {
            return requiredExternalOrgId;
        }

        private String getRequiredExternalRoleId() {
            return requiredExternalRoleId;
        }

        private String getActorType() {
            return actorType;
        }
    }
}
