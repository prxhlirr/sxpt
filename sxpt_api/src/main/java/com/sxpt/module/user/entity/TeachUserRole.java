package com.sxpt.module.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 教学平台用户角色关联实体。
 *
 * 业务功能：
 * 1. 映射 teach_user_role 表，维护教学用户与教学角色之间的授权关系。
 * 2. 为教师、学生、管理员等角色判断提供稳定的授权来源。
 *
 * 关键流程：
 * 1. 授权时记录用户 ID、角色 ID、租户 ID 和授权来源。
 * 2. 后续接口通过用户角色关系判断用户能否执行教学管理、学习或复核动作。
 * 3. 通过通用状态和软删除字段控制授权关系是否继续生效。
 */
@TableName("teach_user_role")
public class TeachUserRole {

    @TableId
    private String id;

    private String tenantId;

    private String userId;

    private String roleId;

    private String grantSource;

    private String createBy;

    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    private String status;

    private Boolean deleted;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRoleId() {
        return roleId;
    }

    public void setRoleId(String roleId) {
        this.roleId = roleId;
    }

    public String getGrantSource() {
        return grantSource;
    }

    public void setGrantSource(String grantSource) {
        this.grantSource = grantSource;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getDeleted() {
        return deleted;
    }

    public void setDeleted(Boolean deleted) {
        this.deleted = deleted;
    }
}
