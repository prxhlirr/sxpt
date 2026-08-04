package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.OriginRole;

import java.util.List;

/**
 * 原平台角色字典维护服务。
 *
 * 业务功能：
 * 1. 维护原平台系统下可供教学平台配置选择的角色字典。
 * 2. 为模块流程参与方配置、模板生成规则和数据准备参数组装提供可信角色来源。
 *
 * 关键流程：
 * 1. 创建时校验原平台存在、角色编码唯一，并补齐生命周期字段。
 * 2. 更新时固定租户、原平台和角色编码边界，只允许修改展示名称、原平台标识、分类和备注。
 * 3. 列表查询按租户和原平台过滤，作为前端下拉数据源。
 */
public interface OriginRoleService {

    /**
     * 创建原平台角色字典。
     *
     * @param originRole 原平台角色字典实体。
     * @return 已保存的原平台角色字典实体。
     */
    OriginRole createOriginRole(OriginRole originRole);

    /**
     * 更新原平台角色字典。
     *
     * @param originRole 原平台角色字典实体。
     * @return 已更新的原平台角色字典实体。
     */
    OriginRole updateOriginRole(OriginRole originRole);

    /**
     * 启用原平台角色字典。
     *
     * @param id 角色字典 ID。
     * @return 已启用的原平台角色字典实体。
     */
    OriginRole enableOriginRole(String id);

    /**
     * 停用原平台角色字典。
     *
     * @param id 角色字典 ID。
     * @return 已停用的原平台角色字典实体。
     */
    OriginRole disableOriginRole(String id);

    /**
     * 查询原平台角色字典详情。
     *
     * @param id 角色字典 ID。
     * @return 未删除的原平台角色字典实体。
     */
    OriginRole getOriginRoleById(String id);

    /**
     * 查询某个原平台下的角色字典列表。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @param activeOnly 是否只返回启用角色。
     * @return 原平台角色字典列表。
     */
    List<OriginRole> listOriginRoles(String tenantId, String connectorSystemId, boolean activeOnly);
}
