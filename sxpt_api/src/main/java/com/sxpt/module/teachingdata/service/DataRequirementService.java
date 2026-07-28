package com.sxpt.module.teachingdata.service;

import com.sxpt.module.teachingdata.entity.DataRequirement;

import java.util.List;

/**
 * 数据需求批次服务。
 *
 * 业务功能：
 * 1. 创建数据准备需求批次，作为后续需求明细、准备任务、数据池和数据实例的聚合入口。
 * 2. 按任务和场景查询数据需求批次，支撑后台查看数据准备进度。
 *
 * 关键流程：
 * 1. 发布任务或学生按需开始时创建 DataRequirement。
 * 2. Service 校验租户、原平台、模块、场景和批次编码等最小字段。
 * 3. 后续 DataRequirementItem 生成服务基于批次继续展开学生、题目、单位和角色约束。
 */
public interface DataRequirementService {

    /**
     * 创建数据需求批次。
     *
     * @param requirement 数据需求批次实体。
     * @return 已保存的数据需求批次。
     */
    DataRequirement createDataRequirement(DataRequirement requirement);

    /**
     * 查询指定任务和场景下的数据需求批次。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param sceneType 教学场景。
     * @return 数据需求批次列表。
     */
    List<DataRequirement> listByTaskAndScene(String tenantId, String taskId, String sceneType);
}
