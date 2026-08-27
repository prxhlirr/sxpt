package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.TeacherRecordArtifact;
import org.apache.ibatis.annotations.Mapper;

/**
 * 老师备案产物 Mapper。
 *
 * 业务功能：
 * 1. 绑定 teacher_record_artifact 表的基础持久化能力。
 * 2. 支撑练习任务从备案产物中引用业务场景和来源样本数据。
 *
 * 关键流程：
 * 1. Service 负责备案产物的创建、发布和引用校验。
 * 2. Mapper 只承载基础 CRUD 能力。
 */
@Mapper
public interface TeacherRecordArtifactMapper extends BaseMapper<TeacherRecordArtifact> {
}
