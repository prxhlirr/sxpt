package com.sxpt.module.evaluation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.evaluation.entity.EvaluationItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评分项 Mapper。
 *
 * 业务功能：
 * 1. 绑定 evaluation_item 表的基础持久化能力。
 * 2. 为评分项创建、查询和后续自动评分断言提供数据访问入口。
 *
 * 关键流程：
 * 1. Service 层负责评分项必填字段、断言配置和租户边界。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD，避免持久化层混入业务判断。
 */
@Mapper
public interface EvaluationItemMapper extends BaseMapper<EvaluationItem> {
}
