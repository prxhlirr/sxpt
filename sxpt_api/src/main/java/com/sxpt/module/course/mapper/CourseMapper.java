package com.sxpt.module.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.course.entity.Course;
import org.apache.ibatis.annotations.Mapper;

/**
 * 课程 Mapper。
 *
 * 业务功能：
 * 1. 绑定 course 表的基础持久化能力。
 * 2. 为课程创建、查询和后续任务发布提供数据访问入口。
 *
 * 关键流程：
 * 1. Service 层负责课程发布口径、默认版本和租户边界。
 * 2. Mapper 只承载 MyBatis Plus 基础 CRUD，避免持久化层混入业务判断。
 */
@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
