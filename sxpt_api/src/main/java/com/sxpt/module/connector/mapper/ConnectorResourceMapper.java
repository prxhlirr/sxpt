package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.ConnectorResource;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原平台正式资源 Mapper。
 *
 * 业务功能：
 * 1. 绑定 connector_resource 表的基础持久化能力。
 * 2. 为资源沉淀、资源查询、教学步骤引用和评分项引用提供数据访问入口。
 *
 * 关键流程：
 * 1. Service 层负责资源是否值得沉淀、租户边界和默认生命周期字段。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD，避免持久化层混入业务判断。
 */
@Mapper
public interface ConnectorResourceMapper extends BaseMapper<ConnectorResource> {
}
