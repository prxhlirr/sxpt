package com.sxpt.module.teachingdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.teachingdata.entity.DataRequirement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据需求批次数据访问接口。
 *
 * 业务功能：
 * 1. 提供 data_requirement 表的基础数据访问能力。
 * 2. 支撑数据准备批次创建、进度回写、失败追踪和后台查询。
 *
 * 关键流程：
 * 1. Service 层负责生成 requirementCode、展开需求明细和维护批次状态机。
 * 2. Mapper 只负责持久化访问，避免承载数据准备业务规则。
 */
@Mapper
public interface DataRequirementMapper extends BaseMapper<DataRequirement> {
}
