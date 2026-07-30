package com.sxpt.module.user.controller;

import com.sxpt.common.api.ApiResult;
import com.sxpt.common.security.CurrentUserContext;
import com.sxpt.module.user.entity.SysDictItem;
import com.sxpt.module.user.entity.SysMenuConfig;
import com.sxpt.module.user.entity.SysPermissionConfig;
import com.sxpt.module.user.entity.SysRolePermission;
import com.sxpt.module.user.service.SystemConfigService;
import com.sxpt.module.user.vo.RuntimeUserContextVO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

/**
 * 系统基础配置接口。
 *
 * 业务功能：
 * 1. 为管理端提供菜单、权限、字典的可视化维护接口。
 * 2. 补齐用户、角色、单位之外的后台配置入口，支撑后续完整权限配置流程。
 *
 * 关键流程：
 * 1. Controller 接收轻量请求对象并做 Bean Validation。
 * 2. Controller 转换为实体后交给 SystemConfigService 统一处理租户边界和默认字段。
 * 3. 当前接口只维护配置台账，不直接改变运行时鉴权逻辑。
 */
@RestController
@RequestMapping("/api/v1/system-config")
@ConditionalOnProperty(name = "sxpt.system-config.controller.enabled", havingValue = "true", matchIfMissing = true)
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    public SystemConfigController(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * 业务功能：创建系统菜单配置。
     * 关键流程：应用层生成主键，Service 补齐生命周期字段，数据库唯一索引兜底菜单编码重复。
     */
    @PostMapping("/menus/create")
    public ApiResult<SysMenuConfig> createMenu(@Valid @RequestBody CreateMenuRequest request) {
        SysMenuConfig saved = systemConfigService.createMenu(toMenuEntity(request));
        return ApiResult.success(saved);
    }

    /**
     * 业务功能：查询租户下系统菜单配置。
     * 关键流程：按租户和软删除边界查询，返回给管理页面进行菜单台账维护。
     */
    @GetMapping("/menus")
    public ApiResult<List<SysMenuConfig>> listMenus(@RequestParam String tenantId) {
        return ApiResult.success(systemConfigService.listMenus(tenantId));
    }

    /**
     * 业务功能：查询单个菜单配置详情。
     * 关键流程：Service 校验租户和软删除边界，避免管理页面跨租户读取配置。
     */
    @GetMapping("/menus/detail")
    public ApiResult<SysMenuConfig> getMenu(@RequestParam String tenantId, @RequestParam String id) {
        return ApiResult.success(systemConfigService.getMenu(tenantId, id));
    }

    /**
     * 业务功能：编辑系统菜单配置。
     * 关键流程：只更新菜单可维护字段，生命周期字段和租户边界由 Service 统一控制。
     */
    @PostMapping("/menus/update")
    public ApiResult<SysMenuConfig> updateMenu(@Valid @RequestBody UpdateMenuRequest request) {
        SysMenuConfig saved = systemConfigService.updateMenu(
                request.getTenantId(),
                request.getId(),
                toMenuEntity(request));
        return ApiResult.success(saved);
    }

    /**
     * 业务功能：启用或停用系统菜单配置。
     * 关键流程：停用菜单不会删除台账，运行时菜单查询会因状态过滤自然隐藏。
     */
    @PostMapping("/menus/status")
    public ApiResult<SysMenuConfig> updateMenuStatus(@Valid @RequestBody UpdateMenuStatusRequest request) {
        SysMenuConfig saved = systemConfigService.updateMenuStatus(
                request.getTenantId(),
                request.getId(),
                request.getStatus());
        return ApiResult.success(saved);
    }

    /**
     * 业务功能：创建系统权限配置。
     * 关键流程：权限编码作为后续角色授权和菜单绑定的稳定引用，不在此处做运行时授权变更。
     */
    @PostMapping("/permissions/create")
    public ApiResult<SysPermissionConfig> createPermission(@Valid @RequestBody CreatePermissionRequest request) {
        SysPermissionConfig saved = systemConfigService.createPermission(toPermissionEntity(request));
        return ApiResult.success(saved);
    }

    /**
     * 业务功能：查询租户下系统权限配置。
     * 关键流程：按租户和软删除边界查询，为权限管理页面提供真实配置列表。
     */
    @GetMapping("/permissions")
    public ApiResult<List<SysPermissionConfig>> listPermissions(@RequestParam String tenantId) {
        return ApiResult.success(systemConfigService.listPermissions(tenantId));
    }

    /**
     * 业务功能：查询单个权限配置详情。
     * 关键流程：Service 校验租户和软删除边界，避免管理页面跨租户读取权限点。
     */
    @GetMapping("/permissions/detail")
    public ApiResult<SysPermissionConfig> getPermission(@RequestParam String tenantId, @RequestParam String id) {
        return ApiResult.success(systemConfigService.getPermission(tenantId, id));
    }

    /**
     * 业务功能：编辑系统权限配置。
     * 关键流程：只更新权限可维护字段，生命周期字段和租户边界由 Service 统一控制。
     */
    @PostMapping("/permissions/update")
    public ApiResult<SysPermissionConfig> updatePermission(@Valid @RequestBody UpdatePermissionRequest request) {
        SysPermissionConfig saved = systemConfigService.updatePermission(
                request.getTenantId(),
                request.getId(),
                toPermissionEntity(request));
        return ApiResult.success(saved);
    }

    /**
     * 业务功能：启用或停用系统权限配置。
     * 关键流程：停用权限不会删除台账，运行时权限查询会因状态过滤自然失效。
     */
    @PostMapping("/permissions/status")
    public ApiResult<SysPermissionConfig> updatePermissionStatus(
            @Valid @RequestBody UpdatePermissionStatusRequest request) {
        SysPermissionConfig saved = systemConfigService.updatePermissionStatus(
                request.getTenantId(),
                request.getId(),
                request.getStatus());
        return ApiResult.success(saved);
    }

    /**
     * 业务功能：创建系统字典项。
     * 关键流程：同一字典下按字典项编码唯一，保证页面下拉值和业务枚举有稳定来源。
     */
    @PostMapping("/dict-items/create")
    public ApiResult<SysDictItem> createDictItem(@Valid @RequestBody CreateDictItemRequest request) {
        SysDictItem saved = systemConfigService.createDictItem(toDictEntity(request));
        return ApiResult.success(saved);
    }

    /**
     * 业务功能：查询租户下系统字典项。
     * 关键流程：按字典编码和排序号展示，方便管理员维护同一组下拉选项。
     */
    @GetMapping("/dict-items")
    public ApiResult<List<SysDictItem>> listDictItems(@RequestParam String tenantId) {
        return ApiResult.success(systemConfigService.listDictItems(tenantId));
    }

    /**
     * 业务功能：查询单个字典项配置详情。
     * 关键流程：Service 校验租户和软删除边界，避免管理页面跨租户读取字典项。
     */
    @GetMapping("/dict-items/detail")
    public ApiResult<SysDictItem> getDictItem(@RequestParam String tenantId, @RequestParam String id) {
        return ApiResult.success(systemConfigService.getDictItem(tenantId, id));
    }

    /**
     * 业务功能：编辑系统字典项配置。
     * 关键流程：只更新字典项可维护字段，生命周期字段和租户边界由 Service 统一控制。
     */
    @PostMapping("/dict-items/update")
    public ApiResult<SysDictItem> updateDictItem(@Valid @RequestBody UpdateDictItemRequest request) {
        SysDictItem saved = systemConfigService.updateDictItem(
                request.getTenantId(),
                request.getId(),
                toDictEntity(request));
        return ApiResult.success(saved);
    }

    /**
     * 业务功能：启用或停用系统字典项配置。
     * 关键流程：停用字典项不会删除台账，后续业务读取字典时可按状态过滤。
     */
    @PostMapping("/dict-items/status")
    public ApiResult<SysDictItem> updateDictItemStatus(@Valid @RequestBody UpdateDictItemStatusRequest request) {
        SysDictItem saved = systemConfigService.updateDictItemStatus(
                request.getTenantId(),
                request.getId(),
                request.getStatus());
        return ApiResult.success(saved);
    }

    /**
     * 业务功能：创建角色权限绑定关系。
     * 关键流程：只维护授权台账，不直接刷新运行时权限缓存，避免基础配置维护影响当前在线用户。
     */
    @PostMapping("/role-permissions/grant")
    public ApiResult<SysRolePermission> grantRolePermission(@Valid @RequestBody GrantRolePermissionRequest request) {
        SysRolePermission saved = systemConfigService.grantRolePermission(toRolePermissionEntity(request));
        return ApiResult.success(saved);
    }

    /**
     * 业务功能：查询租户下角色权限绑定关系。
     * 关键流程：返回弱引用 ID，前端结合角色列表和权限列表展示可读名称。
     */
    @GetMapping("/role-permissions")
    public ApiResult<List<SysRolePermission>> listRolePermissions(@RequestParam String tenantId) {
        return ApiResult.success(systemConfigService.listRolePermissions(tenantId));
    }

    /**
     * 业务功能：查询单个角色权限绑定详情。
     * 关键流程：Service 校验租户和软删除边界，避免管理页面跨租户读取授权关系。
     */
    @GetMapping("/role-permissions/detail")
    public ApiResult<SysRolePermission> getRolePermission(@RequestParam String tenantId, @RequestParam String id) {
        return ApiResult.success(systemConfigService.getRolePermission(tenantId, id));
    }

    /**
     * 业务功能：启用或停用角色权限绑定。
     * 关键流程：停用绑定不会删除台账，运行时权限查询会因状态过滤自然失效。
     */
    @PostMapping("/role-permissions/status")
    public ApiResult<SysRolePermission> updateRolePermissionStatus(
            @Valid @RequestBody UpdateRolePermissionStatusRequest request) {
        SysRolePermission saved = systemConfigService.updateRolePermissionStatus(
                request.getTenantId(),
                request.getId(),
                request.getStatus());
        return ApiResult.success(saved);
    }

    /**
     * 业务功能：查询当前登录用户可见菜单。
     * 关键流程：用户身份来自 JWT 认证上下文，Service 再反查用户角色和角色权限，避免信任前端传入的用户 ID。
     */
    @GetMapping("/runtime/menus")
    public ApiResult<List<SysMenuConfig>> listCurrentUserMenus() {
        String userId = CurrentUserContext.getRequiredUser().getUserId();
        return ApiResult.success(systemConfigService.listVisibleMenusForUser(userId));
    }

    /**
     * 业务功能：查询当前登录用户拥有的权限编码。
     * 关键流程：只返回权限编码列表，便于前端调试菜单展示和后续按钮级控制，不泄露角色绑定内部 ID。
     */
    @GetMapping("/runtime/permissions")
    public ApiResult<List<String>> listCurrentUserPermissions() {
        String userId = CurrentUserContext.getRequiredUser().getUserId();
        return ApiResult.success(systemConfigService.listPermissionCodesForUser(userId));
    }

    /**
     * 业务功能：查询当前登录用户运行时上下文。
     * 关键流程：聚合用户、角色、单位、权限和可见菜单，前端据此渲染不同角色看到的页面入口。
     */
    @GetMapping("/runtime/context")
    public ApiResult<RuntimeUserContextVO> getCurrentUserRuntimeContext() {
        String userId = CurrentUserContext.getRequiredUser().getUserId();
        return ApiResult.success(systemConfigService.getRuntimeContextForUser(userId));
    }

    private SysMenuConfig toMenuEntity(CreateMenuRequest request) {
        SysMenuConfig menuConfig = new SysMenuConfig();
        menuConfig.setId(generateId());
        menuConfig.setTenantId(request.getTenantId());
        menuConfig.setParentId(request.getParentId());
        menuConfig.setMenuCode(request.getMenuCode());
        menuConfig.setMenuName(request.getMenuName());
        menuConfig.setMenuType(request.getMenuType());
        menuConfig.setRoutePath(request.getRoutePath());
        menuConfig.setComponentPath(request.getComponentPath());
        menuConfig.setPermissionCode(request.getPermissionCode());
        menuConfig.setIcon(request.getIcon());
        menuConfig.setSortNo(request.getSortNo());
        menuConfig.setVisible(request.getVisible());
        return menuConfig;
    }

    private SysPermissionConfig toPermissionEntity(CreatePermissionRequest request) {
        SysPermissionConfig permissionConfig = new SysPermissionConfig();
        permissionConfig.setId(generateId());
        permissionConfig.setTenantId(request.getTenantId());
        permissionConfig.setPermissionCode(request.getPermissionCode());
        permissionConfig.setPermissionName(request.getPermissionName());
        permissionConfig.setResourceType(request.getResourceType());
        permissionConfig.setResourceCode(request.getResourceCode());
        permissionConfig.setActionCode(request.getActionCode());
        permissionConfig.setDescription(request.getDescription());
        return permissionConfig;
    }

    private SysDictItem toDictEntity(CreateDictItemRequest request) {
        SysDictItem dictItem = new SysDictItem();
        dictItem.setId(generateId());
        dictItem.setTenantId(request.getTenantId());
        dictItem.setDictCode(request.getDictCode());
        dictItem.setDictName(request.getDictName());
        dictItem.setItemCode(request.getItemCode());
        dictItem.setItemName(request.getItemName());
        dictItem.setItemValue(request.getItemValue());
        dictItem.setSortNo(request.getSortNo());
        dictItem.setRemark(request.getRemark());
        return dictItem;
    }

    private SysRolePermission toRolePermissionEntity(GrantRolePermissionRequest request) {
        SysRolePermission rolePermission = new SysRolePermission();
        rolePermission.setId(generateId());
        rolePermission.setTenantId(request.getTenantId());
        rolePermission.setRoleId(request.getRoleId());
        rolePermission.setPermissionId(request.getPermissionId());
        rolePermission.setGrantSource(request.getGrantSource());
        return rolePermission;
    }

    private String generateId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static class CreateMenuRequest {
        @NotBlank(message = "租户 ID 不能为空")
        @Size(max = 64, message = "租户 ID 长度不能超过 64")
        private String tenantId;
        @Size(max = 64, message = "上级菜单 ID 长度不能超过 64")
        private String parentId;
        @NotBlank(message = "菜单编码不能为空")
        @Size(max = 128, message = "菜单编码长度不能超过 128")
        private String menuCode;
        @NotBlank(message = "菜单名称不能为空")
        @Size(max = 128, message = "菜单名称长度不能超过 128")
        private String menuName;
        @NotBlank(message = "菜单类型不能为空")
        @Size(max = 32, message = "菜单类型长度不能超过 32")
        private String menuType;
        @Size(max = 256, message = "路由地址长度不能超过 256")
        private String routePath;
        @Size(max = 256, message = "组件路径长度不能超过 256")
        private String componentPath;
        @Size(max = 128, message = "权限编码长度不能超过 128")
        private String permissionCode;
        @Size(max = 64, message = "图标长度不能超过 64")
        private String icon;
        private Integer sortNo;
        @NotNull(message = "是否可见不能为空")
        private Boolean visible;

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }
        public String getMenuCode() { return menuCode; }
        public void setMenuCode(String menuCode) { this.menuCode = menuCode; }
        public String getMenuName() { return menuName; }
        public void setMenuName(String menuName) { this.menuName = menuName; }
        public String getMenuType() { return menuType; }
        public void setMenuType(String menuType) { this.menuType = menuType; }
        public String getRoutePath() { return routePath; }
        public void setRoutePath(String routePath) { this.routePath = routePath; }
        public String getComponentPath() { return componentPath; }
        public void setComponentPath(String componentPath) { this.componentPath = componentPath; }
        public String getPermissionCode() { return permissionCode; }
        public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public Integer getSortNo() { return sortNo; }
        public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
        public Boolean getVisible() { return visible; }
        public void setVisible(Boolean visible) { this.visible = visible; }
    }

    public static class UpdateMenuRequest extends CreateMenuRequest {
        @NotBlank(message = "菜单 ID 不能为空")
        @Size(max = 64, message = "菜单 ID 长度不能超过 64")
        private String id;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    public static class UpdateMenuStatusRequest {
        @NotBlank(message = "租户 ID 不能为空")
        @Size(max = 64, message = "租户 ID 长度不能超过 64")
        private String tenantId;
        @NotBlank(message = "菜单 ID 不能为空")
        @Size(max = 64, message = "菜单 ID 长度不能超过 64")
        private String id;
        @NotBlank(message = "菜单状态不能为空")
        @Size(max = 32, message = "菜单状态长度不能超过 32")
        private String status;

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class CreatePermissionRequest {
        @NotBlank(message = "租户 ID 不能为空")
        @Size(max = 64, message = "租户 ID 长度不能超过 64")
        private String tenantId;
        @NotBlank(message = "权限编码不能为空")
        @Size(max = 128, message = "权限编码长度不能超过 128")
        private String permissionCode;
        @NotBlank(message = "权限名称不能为空")
        @Size(max = 128, message = "权限名称长度不能超过 128")
        private String permissionName;
        @NotBlank(message = "资源类型不能为空")
        @Size(max = 32, message = "资源类型长度不能超过 32")
        private String resourceType;
        @Size(max = 128, message = "资源编码长度不能超过 128")
        private String resourceCode;
        @NotBlank(message = "动作编码不能为空")
        @Size(max = 64, message = "动作编码长度不能超过 64")
        private String actionCode;
        @Size(max = 512, message = "权限说明长度不能超过 512")
        private String description;

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getPermissionCode() { return permissionCode; }
        public void setPermissionCode(String permissionCode) { this.permissionCode = permissionCode; }
        public String getPermissionName() { return permissionName; }
        public void setPermissionName(String permissionName) { this.permissionName = permissionName; }
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public String getResourceCode() { return resourceCode; }
        public void setResourceCode(String resourceCode) { this.resourceCode = resourceCode; }
        public String getActionCode() { return actionCode; }
        public void setActionCode(String actionCode) { this.actionCode = actionCode; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class UpdatePermissionRequest extends CreatePermissionRequest {
        @NotBlank(message = "权限 ID 不能为空")
        @Size(max = 64, message = "权限 ID 长度不能超过 64")
        private String id;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    public static class UpdatePermissionStatusRequest {
        @NotBlank(message = "租户 ID 不能为空")
        @Size(max = 64, message = "租户 ID 长度不能超过 64")
        private String tenantId;
        @NotBlank(message = "权限 ID 不能为空")
        @Size(max = 64, message = "权限 ID 长度不能超过 64")
        private String id;
        @NotBlank(message = "权限状态不能为空")
        @Size(max = 32, message = "权限状态长度不能超过 32")
        private String status;

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class CreateDictItemRequest {
        @NotBlank(message = "租户 ID 不能为空")
        @Size(max = 64, message = "租户 ID 长度不能超过 64")
        private String tenantId;
        @NotBlank(message = "字典编码不能为空")
        @Size(max = 128, message = "字典编码长度不能超过 128")
        private String dictCode;
        @NotBlank(message = "字典名称不能为空")
        @Size(max = 128, message = "字典名称长度不能超过 128")
        private String dictName;
        @NotBlank(message = "字典项编码不能为空")
        @Size(max = 128, message = "字典项编码长度不能超过 128")
        private String itemCode;
        @NotBlank(message = "字典项名称不能为空")
        @Size(max = 128, message = "字典项名称长度不能超过 128")
        private String itemName;
        @NotBlank(message = "字典项值不能为空")
        @Size(max = 256, message = "字典项值长度不能超过 256")
        private String itemValue;
        private Integer sortNo;
        @Size(max = 512, message = "备注长度不能超过 512")
        private String remark;

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getDictCode() { return dictCode; }
        public void setDictCode(String dictCode) { this.dictCode = dictCode; }
        public String getDictName() { return dictName; }
        public void setDictName(String dictName) { this.dictName = dictName; }
        public String getItemCode() { return itemCode; }
        public void setItemCode(String itemCode) { this.itemCode = itemCode; }
        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }
        public String getItemValue() { return itemValue; }
        public void setItemValue(String itemValue) { this.itemValue = itemValue; }
        public Integer getSortNo() { return sortNo; }
        public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class UpdateDictItemRequest extends CreateDictItemRequest {
        @NotBlank(message = "字典项 ID 不能为空")
        @Size(max = 64, message = "字典项 ID 长度不能超过 64")
        private String id;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
    }

    public static class UpdateDictItemStatusRequest {
        @NotBlank(message = "租户 ID 不能为空")
        @Size(max = 64, message = "租户 ID 长度不能超过 64")
        private String tenantId;
        @NotBlank(message = "字典项 ID 不能为空")
        @Size(max = 64, message = "字典项 ID 长度不能超过 64")
        private String id;
        @NotBlank(message = "字典项状态不能为空")
        @Size(max = 32, message = "字典项状态长度不能超过 32")
        private String status;

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class GrantRolePermissionRequest {
        @NotBlank(message = "租户 ID 不能为空")
        @Size(max = 64, message = "租户 ID 长度不能超过 64")
        private String tenantId;
        @NotBlank(message = "角色 ID 不能为空")
        @Size(max = 64, message = "角色 ID 长度不能超过 64")
        private String roleId;
        @NotBlank(message = "权限 ID 不能为空")
        @Size(max = 64, message = "权限 ID 长度不能超过 64")
        private String permissionId;
        @Size(max = 64, message = "授权来源长度不能超过 64")
        private String grantSource;

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getRoleId() { return roleId; }
        public void setRoleId(String roleId) { this.roleId = roleId; }
        public String getPermissionId() { return permissionId; }
        public void setPermissionId(String permissionId) { this.permissionId = permissionId; }
        public String getGrantSource() { return grantSource; }
        public void setGrantSource(String grantSource) { this.grantSource = grantSource; }
    }

    public static class UpdateRolePermissionStatusRequest {
        @NotBlank(message = "租户 ID 不能为空")
        @Size(max = 64, message = "租户 ID 长度不能超过 64")
        private String tenantId;
        @NotBlank(message = "角色权限绑定 ID 不能为空")
        @Size(max = 64, message = "角色权限绑定 ID 长度不能超过 64")
        private String id;
        @NotBlank(message = "角色权限绑定状态不能为空")
        @Size(max = 32, message = "角色权限绑定状态长度不能超过 32")
        private String status;

        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
