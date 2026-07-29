package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.BusinessModuleProcessActor;
import org.apache.ibatis.annotations.Mapper;

/**
 * 业务模块步骤参与方数据访问接口。
 *
 * 业务功能：
 * 1. 提供 business_module_process_actor 表的基础数据访问能力。
 * 2. 支撑一个办理步骤下配置多个单位、多个角色和多种参与关系。
 *
 * 关键流程：
 * 1. Service 层负责校验参与方所属步骤、参与方序号和单位角色完整性。
 * 2. Mapper 只负责持久化访问，避免承载学生分配和原平台身份推断规则。
 */
@Mapper
public interface BusinessModuleProcessActorMapper extends BaseMapper<BusinessModuleProcessActor> {
}
