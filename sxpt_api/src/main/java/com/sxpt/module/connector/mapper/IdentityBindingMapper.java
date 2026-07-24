package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.IdentityBinding;
import org.apache.ibatis.annotations.Mapper;

/**
 * 原平台身份绑定 Mapper。
 *
 * 业务功能：
 * 1. 绑定 identity_binding 表的基础持久化能力。
 * 2. 为教学用户跳转原平台前查找外部账号绑定提供数据库访问入口。
 *
 * 关键流程：
 * 1. Service 负责租户隔离、外部账号唯一性和绑定状态规则。
 * 2. Mapper 只承载 MyBatis Plus 的基础 CRUD 能力，不放业务判断。
 */
@Mapper
public interface IdentityBindingMapper extends BaseMapper<IdentityBinding> {
}
