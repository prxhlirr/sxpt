package com.sxpt.module.teaching.service;

/**
 * 教学资产发布前完整性校验服务。
 *
 * 业务功能：
 * 1. 在教学资产发布给学生执行前，统一校验教学步骤和评分项是否已经形成可执行闭环。
 * 2. 将发布闸门集中在教学模块，避免 Controller、采集发布和学生执行链路各自散落校验逻辑。
 *
 * 关键流程：
 * 1. 调用方传入租户、任务、教学点和评价规则。
 * 2. 服务校验至少存在教学步骤，并且指定评价规则下存在关联这些步骤的评分项。
 * 3. 校验不通过时抛出业务异常，阻止不完整资产进入学生执行链路。
 */
public interface TeachingAssetPublishCheckService {

    /**
     * 校验指定教学点资产是否具备发布给学生执行的最小完整性。
     *
     * @param tenantId 租户 ID。
     * @param taskId 任务 ID。
     * @param teachingPointId 教学点 ID。
     * @param evaluationRuleId 评价规则 ID。
     */
    void validateReadyToPublish(String tenantId, String taskId, String teachingPointId, String evaluationRuleId);
}
