package com.sxpt.module.execution.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.execution.entity.ExecutionTrace;
import org.apache.ibatis.annotations.Mapper;

/**
 * 学生执行轨迹 Mapper。
 *
 * 业务功能：
 * 1. 绑定 execution_trace 表的基础持久化能力。
 * 2. 为 SDK 轨迹上报、轨迹查询、练习过程分和自动评分提供数据访问入口。
 *
 * 关键流程：
 * 1. Service 层负责租户边界、幂等和默认值处理。
 * 2. Mapper 保持简单，只承载 MyBatis Plus 基础 CRUD。
 */
@Mapper
public interface ExecutionTraceMapper extends BaseMapper<ExecutionTrace> {
}
