package com.sxpt.module.connector.service;

import com.sxpt.module.connector.dto.ClassicCaseBatchGenerateRequest;
import com.sxpt.module.connector.dto.ClassicCaseGenerateLaunchRequest;
import com.sxpt.module.connector.dto.ClassicCaseImportRequest;
import com.sxpt.module.connector.dto.ClassicCaseGenerateRequest;
import com.sxpt.module.connector.entity.ClassicCaseAsset;
import com.sxpt.module.connector.entity.ClassicCaseUsage;
import com.sxpt.module.connector.entity.ClassicCaseVersion;

import java.util.List;

/**
 * 经典案例服务。
 *
 * 业务功能：
 * 1. 接收原平台正式环境推送的完全脱敏经典案例。
 * 2. 将经典案例保存为资产和版本，供后续备案、教学和学生练习复用。
 *
 * 关键流程：
 * 1. 导入时校验来源必须是正式环境，生成目标必须是同组学习环境。
 * 2. 同一 caseCode 重复导入时新增版本并切换 currentVersionId。
 */
public interface ClassicCaseService {

    /**
     * 导入经典案例。
     *
     * @param request 已完成脱敏的经典案例导入请求。
     * @return 已创建或更新的经典案例资产。
     */
    ClassicCaseAsset importClassicCase(ClassicCaseImportRequest request);

    /**
     * 基于经典案例在学习环境生成业务数据。
     *
     * @param request 经典案例生成请求。
     * @return 经典案例使用记录。
     */
    ClassicCaseUsage generateClassicCaseData(ClassicCaseGenerateRequest request);

    /**
     * 查询经典案例资产列表。
     *
     * @param tenantId 租户 ID。
     * @param learningConnectorSystemId 学习环境平台 ID，可选。
     * @param moduleCode 模块编码，可选。
     * @param teachingPointId 教学点 ID，可选。
     * @return 经典案例资产列表。
     */
    List<ClassicCaseAsset> listClassicCaseAssets(String tenantId,
                                                 String learningConnectorSystemId,
                                                 String moduleCode,
                                                 String teachingPointId);

    /**
     * 查询经典案例资产详情。
     *
     * @param tenantId 租户 ID。
     * @param caseAssetId 经典案例资产 ID。
     * @return 经典案例资产。
     */
    ClassicCaseAsset getClassicCaseAssetDetail(String tenantId, String caseAssetId);

    /**
     * 查询经典案例版本列表。
     *
     * @param tenantId 租户 ID。
     * @param caseAssetId 经典案例资产 ID。
     * @return 版本列表。
     */
    List<ClassicCaseVersion> listClassicCaseVersions(String tenantId, String caseAssetId);

    /**
     * 批量生成学生经典案例 demo 数据。
     *
     * 业务功能：
     * 1. 一次请求携带多个学生/题目 item，基于经典案例 caseDataFormat 在原平台学习环境批量生成 demo 数据。
     * 2. 避免循环调用单条接口造成伪批量、N 次原平台请求和不清晰的部分失败语义。
     *
     * 关键流程：
     * 1. 服务端只允许 STUDENT_DEMO。
     * 2. 服务端一次调用 OriginDataPrepareAdapter#createTeachingData，并逐条落 usage 与 teaching_data_instance。
     *
     * @param request 批量生成请求。
     * @return 批量使用记录。
     */
    List<ClassicCaseUsage> batchGenerateClassicCaseData(ClassicCaseBatchGenerateRequest request);

    /**
     * 生成经典案例数据并创建进入原平台学习环境的启动上下文。
     *
     * 业务功能：
     * 1. 将“经典案例生成数据”和“创建 launchToken”合并为一个服务动作，支撑教学平台一键进入原平台。
     * 2. 继续复用现有 PlatformLaunchContextService 的校验和 token 生成逻辑，避免经典案例另起一套启动机制。
     *
     * 关键流程：
     * 1. 先按使用场景生成学习环境业务数据和 teaching_data_instance。
     * 2. 再基于 teaching_data_instance 创建 platform_launch_context。
     *
     * @param request 经典案例生成并启动请求。
     * @return 经典案例使用记录和已创建的启动上下文。
     */
    ClassicCaseLaunchResult generateAndCreateLaunchContext(ClassicCaseGenerateLaunchRequest request);

    /**
     * 经典案例生成并启动的领域结果。
     *
     * 业务功能：
     * 1. 同时承载使用记录和一次性明文 launchToken。
     * 2. 让 Controller 只负责 VO 转换，不拼装启动业务规则。
     */
    class ClassicCaseLaunchResult {

        private final ClassicCaseUsage usage;

        private final PlatformLaunchContextService.CreatedLaunchContext createdLaunchContext;

        public ClassicCaseLaunchResult(ClassicCaseUsage usage,
                                       PlatformLaunchContextService.CreatedLaunchContext createdLaunchContext) {
            this.usage = usage;
            this.createdLaunchContext = createdLaunchContext;
        }

        public ClassicCaseUsage getUsage() {
            return usage;
        }

        public PlatformLaunchContextService.CreatedLaunchContext getCreatedLaunchContext() {
            return createdLaunchContext;
        }
    }
}
