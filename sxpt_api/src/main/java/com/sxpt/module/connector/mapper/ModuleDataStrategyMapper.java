package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.ModuleDataStrategy;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原平台业务模块数据准备策略数据访问接口。
 *
 * 业务功能：
 * 1. 提供 module_data_strategy 表的基础数据访问能力。
 * 2. 支撑后台按业务模块和场景维护数据准备策略，也支撑运行时按策略生成数据需求。
 *
 * 关键流程：
 * 1. Service 层负责校验同一租户、同一原平台、同一模块、同一场景只存在一条未删除策略。
 * 2. Mapper 只负责持久化访问，不承载策略完整性、发布校验和状态流转规则。
 */
@Mapper
public interface ModuleDataStrategyMapper extends BaseMapper<ModuleDataStrategy> {
}
