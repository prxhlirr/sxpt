package com.sxpt.module.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.evaluation.entity.EvaluationResult;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价结果 Mapper。
 *
 * 业务功能：
 * 1. 绑定 evaluation_result 表的基础持久化能力。
 * 2. 支撑自动评分结果的插入、更新和查询。
 *
 * 关键流程：
 * 1. Service 按执行 ID 与规则 ID 查询已有评分结果。
 * 2. 命中时更新自动分和证据，未命中时插入新评分结果。
 */
@Mapper
public interface EvaluationResultMapper extends BaseMapper<EvaluationResult> {
}
