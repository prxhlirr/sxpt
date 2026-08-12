package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.ClassicCaseVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 经典案例版本 Mapper。
 *
 * 业务功能：
 * 1. 提供 classic_case_version 表的基础持久化能力。
 * 2. 版本号计算、payload 校验和哈希生成由 Service 层集中处理。
 */
@Mapper
public interface ClassicCaseVersionMapper extends BaseMapper<ClassicCaseVersion> {
}
