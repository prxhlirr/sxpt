package com.sxpt.module.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.course.entity.Task;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教学任务 Mapper。
 *
 * 业务功能：
 * 1. 绑定 task 表的基础持久化能力。
 * 2. 为任务发布、任务查询和后续执行上下文生成提供数据访问入口。
 *
 * 关键流程：
 * 1. Service 层负责任务发布口径、默认版本和租户边界。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD，避免持久化层混入业务判断。
 */
@Mapper
public interface TaskMapper extends BaseMapper<Task> {
}
