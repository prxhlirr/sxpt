package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.TeacherRecordArtifact;

/**
 * 老师备案产物服务。
 *
 * 业务功能：
 * 1. 将老师在原平台完成备案后注册的 RECORD DataSession 沉淀为可复用教学资产。
 * 2. 为后续练习任务选择备案样本、生成 practice_task_scene 提供稳定来源。
 *
 * 关键流程：
 * 1. 原平台完成备案并注册 DataSession。
 * 2. 教学平台按 dataSessionId 读取原平台业务数据引用和评分快照。
 * 3. 服务生成或复用老师备案产物，后续任务发布只引用产物 ID。
 */
public interface TeacherRecordArtifactService {

    /**
     * 根据老师备案 DataSession 创建备案产物。
     *
     * @param tenantId 租户 ID。
     * @param dataSessionId 老师备案 DataSession ID。
     * @param operatorId 操作人 ID。
     * @return 已创建或已存在的老师备案产物。
     */
    TeacherRecordArtifact createFromDataSession(String tenantId, String dataSessionId, String operatorId);
}
