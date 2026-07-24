package com.sxpt.module.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.course.entity.TaskTeachingPoint;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务教学点关联 Mapper。
 *
 * 业务功能：
 * 1. 绑定 task_teaching_point 表的基础持久化能力。
 * 2. 为任务关联教学点、查询任务教学点清单提供数据访问入口。
 *
 * 关键流程：
 * 1. Service 层负责关联关系必填字段、排序号和租户边界。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD，避免持久化层混入业务判断。
 */
@Mapper
public interface TaskTeachingPointMapper extends BaseMapper<TaskTeachingPoint> {
}
