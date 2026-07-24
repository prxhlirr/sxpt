package com.sxpt.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.user.entity.TeachUserOrg;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教学用户组织关系 Mapper。
 *
 * 业务功能：
 * 1. 绑定 teach_user_org 表的基础持久化能力。
 * 2. 为班级成员维护、课程班成员查询和任务发布范围判断提供数据库访问入口。
 *
 * 关键流程：
 * 1. Service 负责租户隔离、关系类型和重复关系规则。
 * 2. Mapper 只承载 MyBatis Plus 的基础 CRUD 能力，不放业务判断。
 */
@Mapper
public interface TeachUserOrgMapper extends BaseMapper<TeachUserOrg> {
}
