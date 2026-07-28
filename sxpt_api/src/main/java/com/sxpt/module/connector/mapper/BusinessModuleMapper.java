package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.BusinessModule;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原平台业务模块数据访问接口。
 *
 * 业务功能：
 * 1. 提供 business_module 表的基础数据访问能力。
 * 2. 支撑后台维护原平台业务模块、数据策略绑定和数据需求生成。
 *
 * 关键流程：
 * 1. Service 层负责校验同一租户、同一原平台下 moduleCode 唯一。
 * 2. Mapper 只负责持久化访问，不承载模块启停和策略完整性判断。
 */
@Mapper
public interface BusinessModuleMapper extends BaseMapper<BusinessModule> {
}
