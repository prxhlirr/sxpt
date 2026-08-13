package com.sxpt.module.user.service;

import com.sxpt.module.user.entity.SysDictItem;
import com.sxpt.module.user.entity.SysMenuConfig;
import com.sxpt.module.user.entity.SysPermissionConfig;
import com.sxpt.module.user.entity.SysRolePermission;
import com.sxpt.module.user.vo.RuntimeUserContextVO;

import java.util.List;

/**
 * 系统基础配置服务。
 *
 * 业务功能：
 * 1. 提供菜单、权限、字典三类后台基础配置的创建和查询能力。
 * 2. 将租户隔离、软删除边界和默认生命周期字段统一收敛在服务层。
 *
 * 关键流程：
 * 1. Controller 只负责请求转换和响应转换。
 * 2. Service 负责最小业务校验、默认值补齐和 Mapper 调用。
 */
public interface SystemConfigService {

    SysMenuConfig createMenu(SysMenuConfig menuConfig);

    SysMenuConfig getMenu(String tenantId, String id);

    SysMenuConfig updateMenu(String tenantId, String id, SysMenuConfig menuConfig);

    SysMenuConfig updateMenuStatus(String tenantId, String id, String status);

    List<SysMenuConfig> listMenus(String tenantId);

    SysPermissionConfig createPermission(SysPermissionConfig permissionConfig);

    SysPermissionConfig getPermission(String tenantId, String id);

    SysPermissionConfig updatePermission(String tenantId, String id, SysPermissionConfig permissionConfig);

    SysPermissionConfig updatePermissionStatus(String tenantId, String id, String status);

    List<SysPermissionConfig> listPermissions(String tenantId);

    SysDictItem createDictItem(SysDictItem dictItem);

    SysDictItem getDictItem(String tenantId, String id);

    SysDictItem updateDictItem(String tenantId, String id, SysDictItem dictItem);

    SysDictItem updateDictItemStatus(String tenantId, String id, String status);

    List<SysDictItem> listDictItems(String tenantId);

    /**
     * 业务功能：按字典编码查询当前租户启用的字典项。
     * 关键流程：只返回 ACTIVE 且未删除的数据，供运行时元数据和前端下拉读取稳定配置来源。
     */
    List<SysDictItem> listActiveDictItemsByCode(String tenantId, String dictCode);

    SysRolePermission grantRolePermission(SysRolePermission rolePermission);

    SysRolePermission getRolePermission(String tenantId, String id);

    SysRolePermission updateRolePermissionStatus(String tenantId, String id, String status);

    List<SysRolePermission> listRolePermissions(String tenantId);

    List<SysMenuConfig> listVisibleMenusForUser(String userId);

    List<String> listPermissionCodesForUser(String userId);

    boolean hasPermissionCode(String userId, String permissionCode);

    RuntimeUserContextVO getRuntimeContextForUser(String userId);
}
