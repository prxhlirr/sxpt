package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.OriginOrg;

import java.util.List;

/**
 * 原平台组织字典维护服务。
 *
 * 业务功能：
 * 1. 维护原平台系统下可供教学平台配置选择的组织字典。
 * 2. 为模块流程参与方配置、模板生成规则和数据准备参数组装提供可信组织来源。
 *
 * 关键流程：
 * 1. 创建时校验原平台存在、组织编码唯一，并补齐生命周期字段。
 * 2. 更新时固定租户、原平台和组织编码边界，只允许修改展示名称、原平台标识、层级、分类和备注。
 * 3. 列表查询按租户和原平台过滤，作为前端下拉数据源。
 */
public interface OriginOrgService {

    /**
     * 创建原平台组织字典。
     *
     * @param originOrg 原平台组织字典实体。
     * @return 已保存的原平台组织字典实体。
     */
    OriginOrg createOriginOrg(OriginOrg originOrg);

    /**
     * 更新原平台组织字典。
     *
     * @param originOrg 原平台组织字典实体。
     * @return 已更新的原平台组织字典实体。
     */
    OriginOrg updateOriginOrg(OriginOrg originOrg);

    /**
     * 启用原平台组织字典。
     *
     * @param id 组织字典 ID。
     * @return 已启用的原平台组织字典实体。
     */
    OriginOrg enableOriginOrg(String id);

    /**
     * 停用原平台组织字典。
     *
     * @param id 组织字典 ID。
     * @return 已停用的原平台组织字典实体。
     */
    OriginOrg disableOriginOrg(String id);

    /**
     * 查询原平台组织字典详情。
     *
     * @param id 组织字典 ID。
     * @return 未删除的原平台组织字典实体。
     */
    OriginOrg getOriginOrgById(String id);

    /**
     * 查询某个原平台下的组织字典列表。
     *
     * @param tenantId 租户 ID。
     * @param connectorSystemId 原平台 ID。
     * @param activeOnly 是否只返回启用组织。
     * @return 原平台组织字典列表。
     */
    List<OriginOrg> listOriginOrgs(String tenantId, String connectorSystemId, boolean activeOnly);
}
