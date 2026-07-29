package com.sxpt.module.teachingdata.service;

import com.sxpt.module.teachingdata.entity.DataRequirementItem;

import java.util.List;

/**
 * 数据需求明细服务。
 *
 * 业务功能：
 * 1. 创建数据准备明细，明确每个学生、题目、单位、角色对应的原平台数据需求。
 * 2. 按需求批次或学生场景查询明细，为原平台数据准备、练习重置和考试进入提供稳定入口。
 *
 * 关键流程：
 * 1. 数据需求批次创建后，按学生、题目、协作片段展开 DataRequirementItem。
 * 2. Service 强制校验归属单位、所需单位、所需角色和参与人类型，避免生成无法在原平台操作的数据。
 * 3. 后续编排服务基于本明细逐条请求原平台、回填外部业务标识并校验可操作性。
 */
public interface DataRequirementItemService {

    /**
     * 创建数据需求明细。
     *
     * @param item 数据需求明细实体。
     * @return 已保存的数据需求明细。
     */
    DataRequirementItem createDataRequirementItem(DataRequirementItem item);

    /**
     * 查询指定需求批次下的数据需求明细。
     *
     * @param tenantId 租户 ID。
     * @param requirementId 数据需求批次 ID。
     * @return 数据需求明细列表。
     */
    List<DataRequirementItem> listByRequirement(String tenantId, String requirementId);

    /**
     * 查询指定学生在指定任务和场景下的数据需求明细。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param sceneType 教学场景。
     * @param studentId 学生 ID。
     * @return 数据需求明细列表。
     */
    List<DataRequirementItem> listByStudentAndScene(String tenantId, String taskId, String sceneType, String studentId);
}
