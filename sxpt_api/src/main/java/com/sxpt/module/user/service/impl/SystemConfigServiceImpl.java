package com.sxpt.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.sxpt.common.api.ApiResultCode;
import com.sxpt.common.exception.BusinessException;
import com.sxpt.module.user.entity.SysDictItem;
import com.sxpt.module.user.entity.SysMenuConfig;
import com.sxpt.module.user.entity.SysPermissionConfig;
import com.sxpt.module.user.entity.SysRolePermission;
import com.sxpt.module.user.entity.TeachOrg;
import com.sxpt.module.user.entity.TeachRole;
import com.sxpt.module.user.entity.TeachUser;
import com.sxpt.module.user.entity.TeachUserOrg;
import com.sxpt.module.user.entity.TeachUserRole;
import com.sxpt.module.user.mapper.SysDictItemMapper;
import com.sxpt.module.user.mapper.SysMenuConfigMapper;
import com.sxpt.module.user.mapper.SysPermissionConfigMapper;
import com.sxpt.module.user.mapper.SysRolePermissionMapper;
import com.sxpt.module.user.mapper.TeachOrgMapper;
import com.sxpt.module.user.mapper.TeachRoleMapper;
import com.sxpt.module.user.mapper.TeachUserMapper;
import com.sxpt.module.user.mapper.TeachUserOrgMapper;
import com.sxpt.module.user.mapper.TeachUserRoleMapper;
import com.sxpt.module.user.service.SystemConfigService;
import com.sxpt.module.user.vo.RuntimeUserContextVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 系统基础配置服务实现。
 *
 * 业务功能：
 * 1. 持久化菜单、权限、字典三类后台基础配置。
 * 2. 为管理员页面提供真实数据库台账，补齐用户、角色、单位之外的后台配置入口。
 *
 * 关键流程：
 * 1. 创建前校验租户和业务编码，避免写入不可追溯配置。
 * 2. 写入前统一补齐创建时间、更新时间、状态和软删除字段。
 * 3. 查询时只返回当前租户未软删除的数据，避免跨租户泄露。
 */
@Service
@Profile("!test")
public class SystemConfigServiceImpl implements SystemConfigService {

    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final String DISABLED_STATUS = "DISABLED";

    private final SysMenuConfigMapper menuConfigMapper;
    private final SysPermissionConfigMapper permissionConfigMapper;
    private final SysDictItemMapper dictItemMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final TeachRoleMapper teachRoleMapper;
    private final TeachOrgMapper teachOrgMapper;
    private final TeachUserMapper teachUserMapper;
    private final TeachUserOrgMapper teachUserOrgMapper;
    private final TeachUserRoleMapper teachUserRoleMapper;

    public SystemConfigServiceImpl(
            SysMenuConfigMapper menuConfigMapper,
            SysPermissionConfigMapper permissionConfigMapper,
            SysDictItemMapper dictItemMapper,
            SysRolePermissionMapper rolePermissionMapper,
            TeachRoleMapper teachRoleMapper,
            TeachOrgMapper teachOrgMapper,
            TeachUserMapper teachUserMapper,
            TeachUserOrgMapper teachUserOrgMapper,
            TeachUserRoleMapper teachUserRoleMapper) {
        this.menuConfigMapper = menuConfigMapper;
        this.permissionConfigMapper = permissionConfigMapper;
        this.dictItemMapper = dictItemMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.teachRoleMapper = teachRoleMapper;
        this.teachOrgMapper = teachOrgMapper;
        this.teachUserMapper = teachUserMapper;
        this.teachUserOrgMapper = teachUserOrgMapper;
        this.teachUserRoleMapper = teachUserRoleMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMenuConfig createMenu(SysMenuConfig menuConfig) {
        requireText(menuConfig.getTenantId());
        requireText(menuConfig.getMenuCode());
        requireText(menuConfig.getMenuName());
        requireText(menuConfig.getMenuType());
        fillMenuDefaults(menuConfig);
        menuConfigMapper.insert(menuConfig);
        return menuConfig;
    }

    @Override
    public SysMenuConfig getMenu(String tenantId, String id) {
        return findMenuInTenant(tenantId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMenuConfig updateMenu(String tenantId, String id, SysMenuConfig menuConfig) {
        requireText(menuConfig.getMenuCode());
        requireText(menuConfig.getMenuName());
        requireText(menuConfig.getMenuType());
        SysMenuConfig existing = findMenuInTenant(tenantId, id);
        existing.setParentId(menuConfig.getParentId());
        existing.setMenuCode(menuConfig.getMenuCode());
        existing.setMenuName(menuConfig.getMenuName());
        existing.setMenuType(menuConfig.getMenuType());
        existing.setRoutePath(menuConfig.getRoutePath());
        existing.setComponentPath(menuConfig.getComponentPath());
        existing.setPermissionCode(menuConfig.getPermissionCode());
        existing.setIcon(menuConfig.getIcon());
        existing.setSortNo(menuConfig.getSortNo() == null ? 0 : menuConfig.getSortNo());
        existing.setVisible(menuConfig.getVisible() == null ? Boolean.TRUE : menuConfig.getVisible());
        existing.setUpdateTime(LocalDateTime.now());
        menuConfigMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysMenuConfig updateMenuStatus(String tenantId, String id, String status) {
        requireSupportedConfigStatus(status);
        SysMenuConfig existing = findMenuInTenant(tenantId, id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        menuConfigMapper.updateById(existing);
        return existing;
    }

    @Override
    public List<SysMenuConfig> listMenus(String tenantId) {
        requireText(tenantId);
        return menuConfigMapper.selectList(new QueryWrapper<SysMenuConfig>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("sort_no")
                .orderByDesc("create_time"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionConfig createPermission(SysPermissionConfig permissionConfig) {
        requireText(permissionConfig.getTenantId());
        requireText(permissionConfig.getPermissionCode());
        requireText(permissionConfig.getPermissionName());
        requireText(permissionConfig.getResourceType());
        requireText(permissionConfig.getActionCode());
        fillPermissionDefaults(permissionConfig);
        permissionConfigMapper.insert(permissionConfig);
        return permissionConfig;
    }

    @Override
    public SysPermissionConfig getPermission(String tenantId, String id) {
        return findPermissionInTenant(tenantId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionConfig updatePermission(
            String tenantId,
            String id,
            SysPermissionConfig permissionConfig) {
        requireText(permissionConfig.getPermissionCode());
        requireText(permissionConfig.getPermissionName());
        requireText(permissionConfig.getResourceType());
        requireText(permissionConfig.getActionCode());
        SysPermissionConfig existing = findPermissionInTenant(tenantId, id);
        existing.setPermissionCode(permissionConfig.getPermissionCode());
        existing.setPermissionName(permissionConfig.getPermissionName());
        existing.setResourceType(permissionConfig.getResourceType());
        existing.setResourceCode(permissionConfig.getResourceCode());
        existing.setActionCode(permissionConfig.getActionCode());
        existing.setDescription(permissionConfig.getDescription());
        existing.setUpdateTime(LocalDateTime.now());
        permissionConfigMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysPermissionConfig updatePermissionStatus(String tenantId, String id, String status) {
        requireSupportedConfigStatus(status);
        SysPermissionConfig existing = findPermissionInTenant(tenantId, id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        permissionConfigMapper.updateById(existing);
        return existing;
    }

    @Override
    public List<SysPermissionConfig> listPermissions(String tenantId) {
        requireText(tenantId);
        return permissionConfigMapper.selectList(new QueryWrapper<SysPermissionConfig>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDictItem createDictItem(SysDictItem dictItem) {
        requireText(dictItem.getTenantId());
        requireText(dictItem.getDictCode());
        requireText(dictItem.getDictName());
        requireText(dictItem.getItemCode());
        requireText(dictItem.getItemName());
        requireText(dictItem.getItemValue());
        fillDictDefaults(dictItem);
        dictItemMapper.insert(dictItem);
        return dictItem;
    }

    @Override
    public SysDictItem getDictItem(String tenantId, String id) {
        return findDictItemInTenant(tenantId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDictItem updateDictItem(String tenantId, String id, SysDictItem dictItem) {
        requireText(dictItem.getDictCode());
        requireText(dictItem.getDictName());
        requireText(dictItem.getItemCode());
        requireText(dictItem.getItemName());
        requireText(dictItem.getItemValue());
        SysDictItem existing = findDictItemInTenant(tenantId, id);
        existing.setDictCode(dictItem.getDictCode());
        existing.setDictName(dictItem.getDictName());
        existing.setItemCode(dictItem.getItemCode());
        existing.setItemName(dictItem.getItemName());
        existing.setItemValue(dictItem.getItemValue());
        existing.setSortNo(dictItem.getSortNo() == null ? 0 : dictItem.getSortNo());
        existing.setRemark(dictItem.getRemark());
        existing.setUpdateTime(LocalDateTime.now());
        dictItemMapper.updateById(existing);
        return existing;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysDictItem updateDictItemStatus(String tenantId, String id, String status) {
        requireSupportedConfigStatus(status);
        SysDictItem existing = findDictItemInTenant(tenantId, id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        dictItemMapper.updateById(existing);
        return existing;
    }

    @Override
    public List<SysDictItem> listDictItems(String tenantId) {
        requireText(tenantId);
        return dictItemMapper.selectList(new QueryWrapper<SysDictItem>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("dict_code")
                .orderByAsc("sort_no")
                .orderByDesc("create_time"));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRolePermission grantRolePermission(SysRolePermission rolePermission) {
        requireText(rolePermission.getTenantId());
        requireText(rolePermission.getRoleId());
        requireText(rolePermission.getPermissionId());
        fillRolePermissionDefaults(rolePermission);
        rolePermissionMapper.insert(rolePermission);
        return rolePermission;
    }

    @Override
    public SysRolePermission getRolePermission(String tenantId, String id) {
        return findRolePermissionInTenant(tenantId, id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRolePermission updateRolePermissionStatus(String tenantId, String id, String status) {
        requireSupportedConfigStatus(status);
        SysRolePermission existing = findRolePermissionInTenant(tenantId, id);
        existing.setStatus(status);
        existing.setUpdateTime(LocalDateTime.now());
        rolePermissionMapper.updateById(existing);
        return existing;
    }

    @Override
    public List<SysRolePermission> listRolePermissions(String tenantId) {
        requireText(tenantId);
        return rolePermissionMapper.selectList(new QueryWrapper<SysRolePermission>()
                .eq("tenant_id", tenantId)
                .eq("deleted", Boolean.FALSE)
                .orderByDesc("create_time"));
    }

    @Override
    public List<SysMenuConfig> listVisibleMenusForUser(String userId) {
        requireText(userId);
        TeachUser user = findActiveUser(userId);
        Set<String> permissionCodes = resolvePermissionCodes(user);
        List<SysMenuConfig> menus = menuConfigMapper.selectList(new QueryWrapper<SysMenuConfig>()
                .eq("tenant_id", user.getTenantId())
                .eq("visible", Boolean.TRUE)
                .eq("status", DEFAULT_STATUS)
                .eq("deleted", Boolean.FALSE)
                .orderByAsc("sort_no")
                .orderByAsc("menu_code"));
        List<SysMenuConfig> visibleMenus = new ArrayList<>();
        for (SysMenuConfig menu : menus) {
            if (!StringUtils.hasText(menu.getPermissionCode())
                    || permissionCodes.contains(menu.getPermissionCode())) {
                visibleMenus.add(menu);
            }
        }
        return visibleMenus;
    }

    @Override
    public List<String> listPermissionCodesForUser(String userId) {
        requireText(userId);
        TeachUser user = findActiveUser(userId);
        List<String> permissionCodes = new ArrayList<>(resolvePermissionCodes(user));
        Collections.sort(permissionCodes);
        return permissionCodes;
    }

    @Override
    public boolean hasPermissionCode(String userId, String permissionCode) {
        requireText(userId);
        requireText(permissionCode);
        TeachUser user = findActiveUser(userId);
        return resolvePermissionCodes(user).contains(permissionCode);
    }

    @Override
    public RuntimeUserContextVO getRuntimeContextForUser(String userId) {
        requireText(userId);
        TeachUser user = findActiveUser(userId);
        RuntimeUserContextVO context = new RuntimeUserContextVO();
        context.setUser(toUserSummary(user));
        context.setRoles(resolveRoleSummaries(user));
        context.setOrgs(resolveOrgSummaries(user));
        context.setPermissions(listPermissionCodesForUser(userId));
        context.setMenus(listVisibleMenusForUser(userId));
        return context;
    }

    /**
     * 业务功能：补齐菜单配置默认字段。
     * 关键流程：菜单排序和可见性在前端可填，但为空时必须有稳定默认值。
     */
    private void fillMenuDefaults(SysMenuConfig menuConfig) {
        LocalDateTime now = LocalDateTime.now();
        if (menuConfig.getSortNo() == null) {
            menuConfig.setSortNo(0);
        }
        if (menuConfig.getVisible() == null) {
            menuConfig.setVisible(Boolean.TRUE);
        }
        fillCommonDefaults(menuConfig, now);
    }

    /**
     * 业务功能：补齐权限配置默认字段。
     * 关键流程：权限不需要排序，只需要统一生命周期字段。
     */
    private void fillPermissionDefaults(SysPermissionConfig permissionConfig) {
        LocalDateTime now = LocalDateTime.now();
        if (permissionConfig.getCreateTime() == null) {
            permissionConfig.setCreateTime(now);
        }
        if (permissionConfig.getUpdateTime() == null) {
            permissionConfig.setUpdateTime(now);
        }
        if (!StringUtils.hasText(permissionConfig.getStatus())) {
            permissionConfig.setStatus(DEFAULT_STATUS);
        }
        if (permissionConfig.getDeleted() == null) {
            permissionConfig.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 业务功能：补齐字典项默认字段。
     * 关键流程：字典项排序为空时落到 0，避免列表顺序出现空值差异。
     */
    private void fillDictDefaults(SysDictItem dictItem) {
        LocalDateTime now = LocalDateTime.now();
        if (dictItem.getSortNo() == null) {
            dictItem.setSortNo(0);
        }
        if (dictItem.getCreateTime() == null) {
            dictItem.setCreateTime(now);
        }
        if (dictItem.getUpdateTime() == null) {
            dictItem.setUpdateTime(now);
        }
        if (!StringUtils.hasText(dictItem.getStatus())) {
            dictItem.setStatus(DEFAULT_STATUS);
        }
        if (dictItem.getDeleted() == null) {
            dictItem.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 业务功能：补齐角色权限绑定默认字段。
     * 关键流程：授权来源为空不影响绑定生效，但生命周期字段必须稳定，便于后续审计。
     */
    private void fillRolePermissionDefaults(SysRolePermission rolePermission) {
        LocalDateTime now = LocalDateTime.now();
        if (rolePermission.getCreateTime() == null) {
            rolePermission.setCreateTime(now);
        }
        if (rolePermission.getUpdateTime() == null) {
            rolePermission.setUpdateTime(now);
        }
        if (!StringUtils.hasText(rolePermission.getStatus())) {
            rolePermission.setStatus(DEFAULT_STATUS);
        }
        if (rolePermission.getDeleted() == null) {
            rolePermission.setDeleted(Boolean.FALSE);
        }
    }

    /**
     * 业务功能：解析当前用户通过角色获得的权限编码集合。
     * 关键流程：用户角色、角色权限、权限点三段都按租户和启用状态过滤，避免历史或停用授权继续影响菜单展示。
     */
    private Set<String> resolvePermissionCodes(TeachUser user) {
        List<TeachUserRole> userRoles = teachUserRoleMapper.selectList(new QueryWrapper<TeachUserRole>()
                .eq("tenant_id", user.getTenantId())
                .eq("user_id", user.getId())
                .eq("status", DEFAULT_STATUS)
                .eq("deleted", Boolean.FALSE));
        if (userRoles.isEmpty()) {
            return new HashSet<>();
        }
        List<String> roleIds = new ArrayList<>();
        for (TeachUserRole userRole : userRoles) {
            roleIds.add(userRole.getRoleId());
        }
        List<SysRolePermission> rolePermissions = rolePermissionMapper.selectList(new QueryWrapper<SysRolePermission>()
                .eq("tenant_id", user.getTenantId())
                .in("role_id", roleIds)
                .eq("status", DEFAULT_STATUS)
                .eq("deleted", Boolean.FALSE));
        if (rolePermissions.isEmpty()) {
            return new HashSet<>();
        }
        List<String> permissionIds = new ArrayList<>();
        for (SysRolePermission rolePermission : rolePermissions) {
            permissionIds.add(rolePermission.getPermissionId());
        }
        List<SysPermissionConfig> permissions = permissionConfigMapper.selectList(new QueryWrapper<SysPermissionConfig>()
                .eq("tenant_id", user.getTenantId())
                .in("id", permissionIds)
                .eq("status", DEFAULT_STATUS)
                .eq("deleted", Boolean.FALSE));
        Set<String> permissionCodes = new HashSet<>();
        for (SysPermissionConfig permission : permissions) {
            if (StringUtils.hasText(permission.getPermissionCode())) {
                permissionCodes.add(permission.getPermissionCode());
            }
        }
        return permissionCodes;
    }

    /**
     * 业务功能：转换当前用户展示摘要。
     * 关键流程：只返回前端渲染需要的字段，不暴露密码哈希、盐和登录失败次数等安全字段。
     */
    private RuntimeUserContextVO.UserSummary toUserSummary(TeachUser user) {
        RuntimeUserContextVO.UserSummary summary = new RuntimeUserContextVO.UserSummary();
        summary.setUserId(user.getId());
        summary.setTenantId(user.getTenantId());
        summary.setUsername(user.getUsername());
        summary.setDisplayName(user.getRealName());
        summary.setUserType(user.getUserType());
        summary.setStudentNo(user.getStudentNo());
        summary.setEmployeeNo(user.getEmployeeNo());
        return summary;
    }

    /**
     * 业务功能：解析当前用户拥有的角色摘要。
     * 关键流程：先查有效用户角色关系，再查有效角色定义，返回前端展示和页面判断所需的最小角色字段。
     */
    private List<RuntimeUserContextVO.RoleSummary> resolveRoleSummaries(TeachUser user) {
        List<TeachUserRole> userRoles = teachUserRoleMapper.selectList(new QueryWrapper<TeachUserRole>()
                .eq("tenant_id", user.getTenantId())
                .eq("user_id", user.getId())
                .eq("status", DEFAULT_STATUS)
                .eq("deleted", Boolean.FALSE));
        if (userRoles.isEmpty()) {
            return new ArrayList<RuntimeUserContextVO.RoleSummary>();
        }
        List<String> roleIds = new ArrayList<String>();
        for (TeachUserRole userRole : userRoles) {
            roleIds.add(userRole.getRoleId());
        }
        List<TeachRole> roles = teachRoleMapper.selectList(new QueryWrapper<TeachRole>()
                .eq("tenant_id", user.getTenantId())
                .in("id", roleIds)
                .eq("status", DEFAULT_STATUS)
                .eq("deleted", Boolean.FALSE));
        List<RuntimeUserContextVO.RoleSummary> summaries = new ArrayList<RuntimeUserContextVO.RoleSummary>();
        for (TeachRole role : roles) {
            RuntimeUserContextVO.RoleSummary summary = new RuntimeUserContextVO.RoleSummary();
            summary.setId(role.getId());
            summary.setRoleCode(role.getRoleCode());
            summary.setRoleName(role.getRoleName());
            summaries.add(summary);
        }
        return summaries;
    }

    /**
     * 业务功能：解析当前用户所属教学单位摘要。
     * 关键流程：用户单位关系保留 relationType，前端可以区分归属、管理、任教等业务含义。
     */
    private List<RuntimeUserContextVO.OrgSummary> resolveOrgSummaries(TeachUser user) {
        List<TeachUserOrg> userOrgs = teachUserOrgMapper.selectList(new QueryWrapper<TeachUserOrg>()
                .eq("tenant_id", user.getTenantId())
                .eq("user_id", user.getId())
                .eq("status", DEFAULT_STATUS)
                .eq("deleted", Boolean.FALSE));
        if (userOrgs.isEmpty()) {
            return new ArrayList<RuntimeUserContextVO.OrgSummary>();
        }
        List<String> orgIds = new ArrayList<String>();
        for (TeachUserOrg userOrg : userOrgs) {
            orgIds.add(userOrg.getOrgId());
        }
        List<TeachOrg> orgs = teachOrgMapper.selectList(new QueryWrapper<TeachOrg>()
                .eq("tenant_id", user.getTenantId())
                .in("id", orgIds)
                .eq("status", DEFAULT_STATUS)
                .eq("deleted", Boolean.FALSE));
        List<RuntimeUserContextVO.OrgSummary> summaries = new ArrayList<RuntimeUserContextVO.OrgSummary>();
        for (TeachOrg org : orgs) {
            RuntimeUserContextVO.OrgSummary summary = new RuntimeUserContextVO.OrgSummary();
            summary.setId(org.getId());
            summary.setOrgCode(org.getOrgCode());
            summary.setOrgName(org.getOrgName());
            summary.setOrgType(org.getOrgType());
            summary.setRelationType(findOrgRelationType(userOrgs, org.getId()));
            summaries.add(summary);
        }
        return summaries;
    }

    private String findOrgRelationType(List<TeachUserOrg> userOrgs, String orgId) {
        for (TeachUserOrg userOrg : userOrgs) {
            if (orgId.equals(userOrg.getOrgId())) {
                return userOrg.getRelationType();
            }
        }
        return null;
    }

    /**
     * 业务功能：读取可用于权限解析的当前用户。
     * 关键流程：运行时权限必须从服务端认证用户反查，且软删除用户不能继续获得菜单或权限。
     */
    private TeachUser findActiveUser(String userId) {
        TeachUser user = teachUserMapper.selectById(userId);
        if (user == null || Boolean.TRUE.equals(user.getDeleted())) {
            throw new BusinessException(ApiResultCode.UNAUTHORIZED);
        }
        if (!DEFAULT_STATUS.equals(user.getStatus()) || !StringUtils.hasText(user.getTenantId())) {
            throw new BusinessException(ApiResultCode.FORBIDDEN);
        }
        return user;
    }

    /**
     * 业务功能：读取租户内未删除菜单配置。
     * 关键流程：详情、编辑和启停共用同一入口，避免跨租户或已删除菜单被不同接口误返回。
     */
    private SysMenuConfig findMenuInTenant(String tenantId, String id) {
        requireText(tenantId);
        requireText(id);
        SysMenuConfig menuConfig = menuConfigMapper.selectById(id);
        if (menuConfig == null
                || Boolean.TRUE.equals(menuConfig.getDeleted())
                || !tenantId.equals(menuConfig.getTenantId())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return menuConfig;
    }

    /**
     * 业务功能：读取租户内未删除权限配置。
     * 关键流程：详情、编辑和启停共用同一入口，避免跨租户或已删除权限被不同接口误返回。
     */
    private SysPermissionConfig findPermissionInTenant(String tenantId, String id) {
        requireText(tenantId);
        requireText(id);
        SysPermissionConfig permissionConfig = permissionConfigMapper.selectById(id);
        if (permissionConfig == null
                || Boolean.TRUE.equals(permissionConfig.getDeleted())
                || !tenantId.equals(permissionConfig.getTenantId())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return permissionConfig;
    }

    /**
     * 业务功能：读取租户内未删除字典配置。
     * 关键流程：详情、编辑和启停共用同一入口，避免跨租户或已删除字典项被不同接口误返回。
     */
    private SysDictItem findDictItemInTenant(String tenantId, String id) {
        requireText(tenantId);
        requireText(id);
        SysDictItem dictItem = dictItemMapper.selectById(id);
        if (dictItem == null
                || Boolean.TRUE.equals(dictItem.getDeleted())
                || !tenantId.equals(dictItem.getTenantId())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return dictItem;
    }

    /**
     * 业务功能：读取租户内未删除角色权限绑定。
     * 关键流程：详情和启停共用同一入口，避免跨租户或已删除绑定被不同接口误返回。
     */
    private SysRolePermission findRolePermissionInTenant(String tenantId, String id) {
        requireText(tenantId);
        requireText(id);
        SysRolePermission rolePermission = rolePermissionMapper.selectById(id);
        if (rolePermission == null
                || Boolean.TRUE.equals(rolePermission.getDeleted())
                || !tenantId.equals(rolePermission.getTenantId())) {
            throw new BusinessException(ApiResultCode.DATA_NOT_FOUND);
        }
        return rolePermission;
    }

    /**
     * 业务功能：限定配置启停状态。
     * 关键流程：只允许 ACTIVE 和 DISABLED，避免列表与运行时过滤逻辑出现未知状态。
     */
    private void requireSupportedConfigStatus(String status) {
        requireText(status);
        if (!DEFAULT_STATUS.equals(status) && !DISABLED_STATUS.equals(status)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }

    /**
     * 业务功能：补齐菜单配置公共生命周期字段。
     * 关键流程：菜单需要额外处理 visible 和 sortNo，因此公共逻辑只服务菜单分支。
     */
    private void fillCommonDefaults(SysMenuConfig menuConfig, LocalDateTime now) {
        if (menuConfig.getCreateTime() == null) {
            menuConfig.setCreateTime(now);
        }
        if (menuConfig.getUpdateTime() == null) {
            menuConfig.setUpdateTime(now);
        }
        if (!StringUtils.hasText(menuConfig.getStatus())) {
            menuConfig.setStatus(DEFAULT_STATUS);
        }
        if (menuConfig.getDeleted() == null) {
            menuConfig.setDeleted(Boolean.FALSE);
        }
    }

    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException(ApiResultCode.PARAM_ERROR);
        }
    }
}
