package com.sxpt.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.sxpt.module.user.entity.SysDictItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统字典项 Mapper。
 *
 * 业务功能：绑定 sys_dict_item 表的基础持久化能力。
 * 关键流程：按字典编码和排序字段查询由 Service 统一组织，避免 Controller 直接拼查询条件。
 */
@Mapper
public interface SysDictItemMapper extends BaseMapper<SysDictItem> {
}
