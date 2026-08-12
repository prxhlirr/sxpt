package com.sxpt.module.connector.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.connector.entity.ClassicCaseAsset;
import org.apache.ibatis.annotations.Mapper;

/**
 * 经典案例资产 Mapper。
 *
 * 业务功能：
 * 1. 提供 classic_case_asset 表的基础持久化能力。
 * 2. 业务校验、版本切换和租户边界全部由 Service 层负责。
 */
@Mapper
public interface ClassicCaseAssetMapper extends BaseMapper<ClassicCaseAsset> {
}
