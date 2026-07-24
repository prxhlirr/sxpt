package com.sxpt.module.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.evaluation.entity.EvaluationRule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评价规则 Mapper。
 *
 * 业务功能：
 * 1. 绑定 evaluation_rule 表的基础持久化能力。
 * 2. 为评分规则创建、查询和后续自动评分提供数据访问入口。
 *
 * 关键流程：
 * 1. Service 层负责规则发布口径、默认版本和租户边界。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD，避免持久化层混入业务判断。
 */
@Mapper
public interface EvaluationRuleMapper extends BaseMapper<EvaluationRule> {
}
