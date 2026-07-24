package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.TeachingDataInstance;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教学业务数据实例 Mapper。
 *
 * 业务功能：
 * 1. 绑定 teaching_data_instance 表的基础持久化能力。
 * 2. 为教学任务、launchToken 和练习重置按原平台业务数据定位实例提供数据库访问入口。
 *
 * 关键流程：
 * 1. Service 负责租户隔离、实例状态和唯一性规则。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD 能力，避免在持久化层混入业务判断。
 */
@Mapper
public interface TeachingDataInstanceMapper extends BaseMapper<TeachingDataInstance> {
}
