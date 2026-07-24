package com.sxpt.module.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.practice.entity.PracticeScoreSummary;
import org.apache.ibatis.annotations.Mapper;

/**
 * 练习过程分汇总 Mapper。
 *
 * 业务功能：
 * 1. 绑定 practice_score_summary 表的基础持久化能力。
 * 2. 承载过程分汇总的插入、更新和查询操作。
 *
 * 关键流程：
 * 1. Service 按租户、学生、任务和教学点查询现有汇总。
 * 2. 命中时更新汇总，未命中时插入新汇总。
 */
@Mapper
public interface PracticeScoreSummaryMapper extends BaseMapper<PracticeScoreSummary> {
}
