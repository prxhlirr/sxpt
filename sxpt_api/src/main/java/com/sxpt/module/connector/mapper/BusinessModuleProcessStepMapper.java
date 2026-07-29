package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.BusinessModuleProcessStep;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务模块标准办理步骤数据访问接口。
 *
 * 业务功能：
 * 1. 提供 business_module_process_step 表的基础数据访问能力。
 * 2. 支撑后台维护业务模块标准办理链，也支撑数据准备阶段按标准链生成实例链路快照。
 *
 * 关键流程：
 * 1. Service 层负责校验同一模块下 stepCode 和 stepNo 的唯一性。
 * 2. Mapper 只负责持久化访问，避免承载步骤启停、参与方完整性和链路快照生成规则。
 */
@Mapper
public interface BusinessModuleProcessStepMapper extends BaseMapper<BusinessModuleProcessStep> {
}
