package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.BusinessModuleProcessActor;
import com.sxpt.module.connector.entity.BusinessModuleProcessStep;

import java.util.List;

/**
 * 业务模块标准办理链服务。
 *
 * 业务功能：
 * 1. 维护业务模块下的标准办理步骤和步骤参与方，表达“一步骤多单位多角色”的原平台真实协作结构。
 * 2. 为数据准备阶段生成实例链路快照提供标准链来源。
 * 3. 为学生进入练习或考试时定位步骤参与方、原平台单位和原平台角色提供维护入口。
 *
 * 关键流程：
 * 1. 管理员先在业务模块下创建标准办理步骤。
 * 2. 再为每个步骤维护主办、协办、审核、复核、会签等参与方。
 * 3. 批次准备结果未包含完整真实办理链时，后续快照服务读取本服务的启用步骤和参与方自动补齐。
 */
public interface BusinessModuleProcessChainService {

    /**
     * 创建业务模块标准办理步骤。
     *
     * @param step 标准办理步骤实体，必须包含租户、原平台、业务模块、步骤顺序、步骤编码和步骤名称。
     * @return 已保存并补齐默认字段的标准办理步骤。
     */
    BusinessModuleProcessStep createProcessStep(BusinessModuleProcessStep step);

    /**
     * 更新业务模块标准办理步骤。
     *
     * @param step 标准办理步骤实体，必须包含步骤 ID 以及允许后台编辑的字段。
     * @return 已更新的标准办理步骤。
     */
    BusinessModuleProcessStep updateProcessStep(BusinessModuleProcessStep step);

    /**
     * 启用业务模块标准办理步骤。
     *
     * @param stepId 标准办理步骤 ID。
     * @return 已启用的标准办理步骤。
     */
    BusinessModuleProcessStep enableProcessStep(String stepId);

    /**
     * 停用业务模块标准办理步骤。
     *
     * @param stepId 标准办理步骤 ID。
     * @return 已停用的标准办理步骤。
     */
    BusinessModuleProcessStep disableProcessStep(String stepId);

    /**
     * 查询业务模块标准办理步骤详情。
     *
     * @param stepId 标准办理步骤 ID。
     * @return 未删除的标准办理步骤。
     */
    BusinessModuleProcessStep getProcessStepById(String stepId);

    /**
     * 查询业务模块下全部未删除标准办理步骤。
     *
     * @param tenantId 租户 ID。
     * @param businessModuleId 业务模块 ID。
     * @return 按 stepNo 升序排列的标准办理步骤列表。
     */
    List<BusinessModuleProcessStep> listProcessSteps(String tenantId, String businessModuleId);

    /**
     * 查询业务模块下启用的标准办理步骤。
     *
     * @param tenantId 租户 ID。
     * @param businessModuleId 业务模块 ID。
     * @return 按 stepNo 升序排列的启用步骤列表。
     */
    List<BusinessModuleProcessStep> listActiveProcessSteps(String tenantId, String businessModuleId);

    /**
     * 创建标准办理步骤参与方。
     *
     * @param actor 步骤参与方实体，必须包含租户、业务模块、步骤、参与方序号、参与关系和参与类型。
     * @return 已保存并补齐默认字段的步骤参与方。
     */
    BusinessModuleProcessActor createProcessActor(BusinessModuleProcessActor actor);

    /**
     * 更新标准办理步骤参与方。
     *
     * @param actor 步骤参与方实体，必须包含参与方 ID 以及允许后台编辑的字段。
     * @return 已更新的步骤参与方。
     */
    BusinessModuleProcessActor updateProcessActor(BusinessModuleProcessActor actor);

    /**
     * 启用标准办理步骤参与方。
     *
     * @param actorId 步骤参与方 ID。
     * @return 已启用的步骤参与方。
     */
    BusinessModuleProcessActor enableProcessActor(String actorId);

    /**
     * 停用标准办理步骤参与方。
     *
     * @param actorId 步骤参与方 ID。
     * @return 已停用的步骤参与方。
     */
    BusinessModuleProcessActor disableProcessActor(String actorId);

    /**
     * 查询标准办理步骤参与方详情。
     *
     * @param actorId 步骤参与方 ID。
     * @return 未删除的步骤参与方。
     */
    BusinessModuleProcessActor getProcessActorById(String actorId);

    /**
     * 查询某步骤下全部未删除参与方。
     *
     * @param tenantId 租户 ID。
     * @param processStepId 标准办理步骤 ID。
     * @return 按 actorNo 升序排列的参与方列表。
     */
    List<BusinessModuleProcessActor> listProcessActors(String tenantId, String processStepId);

    /**
     * 查询某步骤下启用的参与方。
     *
     * @param tenantId 租户 ID。
     * @param processStepId 标准办理步骤 ID。
     * @return 按 actorNo 升序排列的启用参与方列表。
     */
    List<BusinessModuleProcessActor> listActiveProcessActors(String tenantId, String processStepId);
}
