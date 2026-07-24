package com.sxpt.module.teaching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teaching.entity.TeachingPoint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教学点 Mapper。
 *
 * 业务功能：
 * 1. 绑定 teaching_point 表的基础持久化能力。
 * 2. 为教学点发布、查询、后续步骤发布和任务引用提供数据访问入口。
 *
 * 关键流程：
 * 1. Service 层负责发布规则、默认状态和租户边界。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD，避免持久化层混入业务判断。
 */
@Mapper
public interface TeachingPointMapper extends BaseMapper<TeachingPoint> {
}
