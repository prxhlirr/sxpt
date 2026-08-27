package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.ExamQuestionScene;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考试题目业务场景 Mapper。
 *
 * 业务功能：
 * 1. 绑定 exam_question_scene 表的基础持久化能力。
 * 2. 支撑原平台 verify launchToken 时返回考试题目业务场景清单。
 *
 * 关键流程：
 * 1. 考试发布或组卷阶段写入题目业务场景。
 * 2. 学生进入原平台后按 examAttemptId 查询待完成题目。
 */
@Mapper
public interface ExamQuestionSceneMapper extends BaseMapper<ExamQuestionScene> {
}
