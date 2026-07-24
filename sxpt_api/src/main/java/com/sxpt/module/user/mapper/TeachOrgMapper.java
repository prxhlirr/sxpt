package com.sxpt.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.user.entity.TeachOrg;
import org.apache.ibatis.annotations.Mapper;

/**
 * 教学组织 Mapper。
 *
 * 业务功能：
 * 1. 绑定 teach_org 表的基础持久化能力。
 * 2. 为班级、课程班和分组维护提供数据库访问入口。
 *
 * 关键流程：
 * 1. Service 负责组织编码、租户隔离和状态规则。
 * 2. Mapper 只承载 MyBatis Plus 的基础 CRUD 能力，不放业务判断。
 */
@Mapper
public interface TeachOrgMapper extends BaseMapper<TeachOrg> {
}
