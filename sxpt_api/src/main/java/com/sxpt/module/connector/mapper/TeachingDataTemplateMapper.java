package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.TeachingDataTemplate;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教学业务数据模板 Mapper。
 *
 * 业务功能：
 * 1. 绑定 teaching_data_template 表的基础持久化能力。
 * 2. 为按原平台、教学点和场景选择造数模板提供数据库访问入口。
 *
 * 关键流程：
 * 1. Service 负责租户隔离、模板状态和查询规则。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD 能力，不放业务判断。
 */
@Mapper
public interface TeachingDataTemplateMapper extends BaseMapper<TeachingDataTemplate> {
}
