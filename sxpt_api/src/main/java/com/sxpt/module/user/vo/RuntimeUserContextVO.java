package com.sxpt.module.user.vo;

import com.sxpt.module.user.entity.SysMenuConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 当前用户运行时上下文返回对象。
 *
 * 业务功能：
 * 1. 聚合前端渲染工作台所需的用户、角色、单位、权限和菜单信息。
 * 2. 避免前端分别调用多个接口再自行拼接权限上下文，降低页面入口判断复杂度。
 *
 * 关键流程：
 * 1. 后端从 JWT 当前用户反查真实用户主数据。
 * 2. 后端按用户角色解析权限编码，再按菜单配置计算可见菜单。
 * 3. 前端只消费本对象渲染菜单和按钮，不把它作为后端接口安全边界。
 */
public class RuntimeUserContextVO {

    private UserSummary user;

    private List<RoleSummary> roles = new ArrayList<RoleSummary>();

    private List<OrgSummary> orgs = new ArrayList<OrgSummary>();

    private List<String> permissions = new ArrayList<String>();

    private List<SysMenuConfig> menus = new ArrayList<SysMenuConfig>();

    public UserSummary getUser() {
        return user;
    }

    public void setUser(UserSummary user) {
        this.user = user;
    }

    public List<RoleSummary> getRoles() {
        return roles;
    }

    public void setRoles(List<RoleSummary> roles) {
        this.roles = roles;
    }

    public List<OrgSummary> getOrgs() {
        return orgs;
    }

    public void setOrgs(List<OrgSummary> orgs) {
        this.orgs = orgs;
    }

    public List<String> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<String> permissions) {
        this.permissions = permissions;
    }

    public List<SysMenuConfig> getMenus() {
        return menus;
    }

    public void setMenus(List<SysMenuConfig> menus) {
        this.menus = menus;
    }

    public static class UserSummary {
        private String userId;
        private String tenantId;
        private String username;
        private String displayName;
        private String userType;
        private String studentNo;
        private String employeeNo;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getUserType() { return userType; }
        public void setUserType(String userType) { this.userType = userType; }
        public String getStudentNo() { return studentNo; }
        public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
        public String getEmployeeNo() { return employeeNo; }
        public void setEmployeeNo(String employeeNo) { this.employeeNo = employeeNo; }
    }

    public static class RoleSummary {
        private String id;
        private String roleCode;
        private String roleName;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getRoleCode() { return roleCode; }
        public void setRoleCode(String roleCode) { this.roleCode = roleCode; }
        public String getRoleName() { return roleName; }
        public void setRoleName(String roleName) { this.roleName = roleName; }
    }

    public static class OrgSummary {
        private String id;
        private String orgCode;
        private String orgName;
        private String orgType;
        private String relationType;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getOrgCode() { return orgCode; }
        public void setOrgCode(String orgCode) { this.orgCode = orgCode; }
        public String getOrgName() { return orgName; }
        public void setOrgName(String orgName) { this.orgName = orgName; }
        public String getOrgType() { return orgType; }
        public void setOrgType(String orgType) { this.orgType = orgType; }
        public String getRelationType() { return relationType; }
        public void setRelationType(String relationType) { this.relationType = relationType; }
    }
}
