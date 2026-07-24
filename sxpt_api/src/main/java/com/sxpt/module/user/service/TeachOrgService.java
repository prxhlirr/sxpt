package com.sxpt.module.user.service;

import com.sxpt.module.user.entity.TeachOrg;

import java.util.List;

/**
 * 教学组织服务。
 *
 * 业务功能：
 * 1. 维护教学平台自己的班级、课程班和分组。
 * 2. 为后续课程、任务发布和学生可见范围提供稳定组织来源。
 *
 * 关键流程：
 * 1. Service 层负责校验组织基础字段和补齐生命周期字段。
 * 2. Mapper 层只负责数据库写入和查询，不承载业务判断。
 */
public interface TeachOrgService {

    /**
     * 创建教学组织。
     *
     * @param teachOrg 教学组织实体，必须包含 ID、租户、组织编码、组织名称和组织类型。
     * @return 已保存的教学组织实体。
     */
    TeachOrg createTeachOrg(TeachOrg teachOrg);

    /**
     * 查询指定租户下的教学组织列表。
     *
     * @param tenantId 租户 ID。
     * @return 指定租户下未删除的教学组织列表。
     */
    List<TeachOrg> listTeachOrgsByTenantId(String tenantId);
}
