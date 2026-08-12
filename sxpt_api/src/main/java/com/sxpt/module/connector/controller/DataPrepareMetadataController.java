package com.sxpt.module.connector.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.module.connector.vo.DataPrepareMetadataVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;

/**
 * 数据准备后台配置元数据接口。
 *
 * 业务功能：
 * 1. 为平台接入、模块、模板、策略和批次准备管理页提供统一编码来源。
 * 2. 在 P0 阶段只开放 API_KEY 认证和 DATA_CREATE 能力，隐藏暂不需要的完整生命周期能力。
 *
 * 关键流程：
 * 1. 管理页启动时调用 metadata 接口。
 * 2. 前端根据 visible 字段渲染可用选项。
 * 3. 后续需要开放新能力时，只调整本接口背后的元数据来源，不要求原平台造数接口改造。
 */
@RestController
@RequestMapping("/api/v1/connector/data-prepare-metadata")
@ConditionalOnProperty(name = "sxpt.connector.data-prepare-metadata-controller.enabled", havingValue = "true", matchIfMissing = true)
public class DataPrepareMetadataController {

    private static final String AUTH_TYPE_API_KEY = "API_KEY";

    private static final String CAPABILITY_DATA_CREATE = "DATA_CREATE";

    private static final String STATUS_DRAFT = "DRAFT";

    private static final String STATUS_SUBMITTED = "SUBMITTED";

    private static final String SCENE_PRACTICE = "PRACTICE";

    /**
     * 查询数据准备后台配置元数据。
     *
     * @return 平台接入、模块、模板、策略和批次准备阶段共享的编码与默认值。
     */
    @GetMapping
    public ApiResult<DataPrepareMetadataVO> detail() {
        return ApiResult.success(buildMetadata());
    }

    /**
     * 构建 P0 阶段集中元数据。
     *
     * @return 只暴露 DATA_CREATE 和 API_KEY 的元数据响应。
     */
    private DataPrepareMetadataVO buildMetadata() {
        DataPrepareMetadataVO metadata = new DataPrepareMetadataVO();
        metadata.setAuthTypes(Collections.singletonList(option(AUTH_TYPE_API_KEY, "API Key", true)));
        metadata.setCapabilities(Arrays.asList(
                option(CAPABILITY_DATA_CREATE, "数据创建", true),
                option("DATA_QUERY", "数据查询", false),
                option("RESULT_CHECK", "结果校验", false),
                option("DATA_LOCK", "数据锁定", false),
                option("DATA_ARCHIVE", "数据归档", false)
        ));
        metadata.setSceneTypes(Arrays.asList(
                option("RECORD", "备案", true),
                option("LEARN", "学习", true),
                option(SCENE_PRACTICE, "练习", true),
                option("EXAM", "考试", true)
        ));
        metadata.setDataSourceStrategies(Arrays.asList(
                option("MOCK_GENERATE", "本系统生成", true),
                option("PULL_ORIGIN", "原平台拉取", true)
        ));
        metadata.setPrepareTimings(Arrays.asList(
                option("ON_DEMAND", "按需准备", true),
                option("ON_PUBLISH", "发布时准备", true),
                option("BEFORE_START", "开始前准备", true)
        ));
        metadata.setSharePolicies(Arrays.asList(
                option("SHARED_READONLY", "只读共享", true),
                option("STUDENT_EXCLUSIVE", "学生独占", true),
                option("ATTEMPT_EXCLUSIVE", "练习独占", true),
                option("QUESTION_EXCLUSIVE", "题目独占", true)
        ));
        metadata.setRegeneratePolicies(Arrays.asList(
                option("NEVER", "不重建", true),
                option("ON_ATTEMPT", "每次练习重建", true),
                option("ON_RETAKE", "补考重建", true),
                option("ON_FAILURE", "失败重建", true)
        ));
        metadata.setLockPolicies(Arrays.asList(
                option("NONE", "不锁定", true),
                option("ON_ALLOCATE", "领取时锁定", true),
                option("ON_EXAM_START", "考试开始锁定", true)
        ));
        metadata.setRecordStatuses(Arrays.asList(
                option(STATUS_DRAFT, "草稿", true),
                option(STATUS_SUBMITTED, "已提交", true),
                option("APPROVED", "已通过", true)
        ));
        metadata.setPlatformDefaults(buildPlatformDefaults());
        metadata.setModuleDefaults(buildModuleDefaults());
        metadata.setTemplateDefaults(buildTemplateDefaults());
        metadata.setStrategyDefaults(buildStrategyDefaults());
        return metadata;
    }

    private DataPrepareMetadataVO.OptionVO option(String code, String label, boolean visible) {
        return new DataPrepareMetadataVO.OptionVO(code, label, visible);
    }

    private DataPrepareMetadataVO.PlatformDefaultsVO buildPlatformDefaults() {
        DataPrepareMetadataVO.PlatformDefaultsVO defaults = new DataPrepareMetadataVO.PlatformDefaultsVO();
        defaults.setSystemType("LOCAL_DEV");
        defaults.setAuthType(AUTH_TYPE_API_KEY);
        defaults.setAuthConfigJson("{\"apiKey\":\"dev-api-key\",\"headerName\":\"X-API-Key\"}");
        defaults.setCapabilityCode(CAPABILITY_DATA_CREATE);
        defaults.setCapabilityName("数据创建");
        defaults.setCapabilityType("HTTP");
        defaults.setCapabilityMethod("POST");
        defaults.setCapabilityTimeoutMs(10000);
        return defaults;
    }

    private DataPrepareMetadataVO.ModuleDefaultsVO buildModuleDefaults() {
        DataPrepareMetadataVO.ModuleDefaultsVO defaults = new DataPrepareMetadataVO.ModuleDefaultsVO();
        defaults.setInitialStatus(STATUS_DRAFT);
        defaults.setTargetStatus(STATUS_SUBMITTED);
        defaults.setCapabilityCodesJson("[\"" + CAPABILITY_DATA_CREATE + "\"]");
        defaults.setProcessStepCode("DATA_CREATE");
        defaults.setProcessStepType("BUSINESS");
        return defaults;
    }

    private DataPrepareMetadataVO.TemplateDefaultsVO buildTemplateDefaults() {
        DataPrepareMetadataVO.TemplateDefaultsVO defaults = new DataPrepareMetadataVO.TemplateDefaultsVO();
        defaults.setSceneType(SCENE_PRACTICE);
        defaults.setInitState(STATUS_DRAFT);
        defaults.setSupportMode("AUTO_CREATE");
        defaults.setRequiredStatus(STATUS_DRAFT);
        return defaults;
    }

    private DataPrepareMetadataVO.StrategyDefaultsVO buildStrategyDefaults() {
        DataPrepareMetadataVO.StrategyDefaultsVO defaults = new DataPrepareMetadataVO.StrategyDefaultsVO();
        defaults.setSceneType(SCENE_PRACTICE);
        defaults.setDataSourceStrategy("MOCK_GENERATE");
        defaults.setInitExternalStatus(STATUS_DRAFT);
        defaults.setTargetExternalStatus(STATUS_SUBMITTED);
        defaults.setPrepareTiming("ON_DEMAND");
        defaults.setSharePolicy("ATTEMPT_EXCLUSIVE");
        defaults.setRegeneratePolicy("ON_ATTEMPT");
        defaults.setLockPolicy("NONE");
        defaults.setValidationPolicyJson("{\"requiredStatus\":\"" + STATUS_DRAFT + "\"}");
        return defaults;
    }
}
