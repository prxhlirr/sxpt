package com.sxpt.module.connector.service;

import com.sxpt.module.connector.entity.IdentityBinding;

/**
 * 原平台身份绑定服务。
 *
 * 业务功能：
 * 1. 维护教学用户与原平台账号之间的绑定关系。
 * 2. 为 launchToken 生成前定位原平台执行身份提供稳定数据来源。
 *
 * 关键流程：
 * 1. Service 层负责校验绑定关系的最小字段和补齐生命周期字段。
 * 2. Mapper 层只负责数据库写入和查询，唯一性由数据库索引保证。
 */
public interface IdentityBindingService {

    /**
     * 创建原平台身份绑定。
     *
     * @param identityBinding 原平台身份绑定实体，必须包含 ID、租户、教学用户、原平台和外部用户 ID。
     * @return 已保存的原平台身份绑定实体。
     */
    IdentityBinding createIdentityBinding(IdentityBinding identityBinding);

    /**
     * 查询教学用户在指定原平台下的有效身份绑定。
     *
     * @param tenantId 租户 ID。
     * @param userId 教学用户 ID。
     * @param connectorSystemId 原平台配置 ID。
     * @return 未删除的原平台身份绑定。
     */
    IdentityBinding getBindingByUserAndConnector(String tenantId, String userId, String connectorSystemId);
}
