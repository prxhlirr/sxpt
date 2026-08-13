import { apiRequest } from './http';
import type { IsoDateTime } from './contracts';

interface StatusRecord {
  id: string;
  tenantId: string;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface SysMenuConfig extends StatusRecord {
  parentId?: string;
  menuCode: string;
  menuName: string;
  menuType: string;
  routePath?: string;
  componentPath?: string;
  permissionCode?: string;
  icon?: string;
  sortNo: number;
  visible: boolean;
}

export interface SysPermissionConfig extends StatusRecord {
  permissionCode: string;
  permissionName: string;
  resourceType: string;
  resourceCode?: string;
  actionCode: string;
  description?: string;
}

export interface SysDictItem extends StatusRecord {
  dictCode: string;
  dictName: string;
  itemCode: string;
  itemName: string;
  itemValue: string;
  sortNo: number;
  remark?: string;
}

export interface SysRolePermission extends StatusRecord {
  roleId: string;
  permissionId: string;
  grantSource?: string;
}

export interface RuntimeUserContext {
  user: {
    userId: string;
    tenantId: string;
    username: string;
    displayName: string;
    userType: string;
    studentNo?: string;
    employeeNo?: string;
  };
  roles: Array<{
    id: string;
    roleCode: string;
    roleName: string;
  }>;
  orgs: Array<{
    id: string;
    orgCode: string;
    orgName: string;
    orgType: string;
    relationType?: string;
  }>;
  permissions: string[];
  menus: SysMenuConfig[];
}

export const systemConfigApi = {
  listMenus(tenantId: string) {
    return apiRequest<SysMenuConfig[]>({
      method: 'GET',
      url: '/system-config/menus',
      params: { tenantId }
    });
  },

  getMenuDetail(tenantId: string, id: string) {
    return apiRequest<SysMenuConfig>({
      method: 'GET',
      url: '/system-config/menus/detail',
      params: { tenantId, id }
    });
  },

  createMenu(request: {
    tenantId: string;
    parentId?: string;
    menuCode: string;
    menuName: string;
    menuType: string;
    routePath?: string;
    componentPath?: string;
    permissionCode?: string;
    icon?: string;
    sortNo?: number;
    visible: boolean;
  }) {
    return apiRequest<SysMenuConfig>({
      method: 'POST',
      url: '/system-config/menus/create',
      data: request
    });
  },

  updateMenu(request: {
    tenantId: string;
    id: string;
    parentId?: string;
    menuCode: string;
    menuName: string;
    menuType: string;
    routePath?: string;
    componentPath?: string;
    permissionCode?: string;
    icon?: string;
    sortNo?: number;
    visible: boolean;
  }) {
    return apiRequest<SysMenuConfig>({
      method: 'POST',
      url: '/system-config/menus/update',
      data: request
    });
  },

  updateMenuStatus(request: {
    tenantId: string;
    id: string;
    status: 'ACTIVE' | 'DISABLED';
  }) {
    return apiRequest<SysMenuConfig>({
      method: 'POST',
      url: '/system-config/menus/status',
      data: request
    });
  },

  listPermissions(tenantId: string) {
    return apiRequest<SysPermissionConfig[]>({
      method: 'GET',
      url: '/system-config/permissions',
      params: { tenantId }
    });
  },

  getPermissionDetail(tenantId: string, id: string) {
    return apiRequest<SysPermissionConfig>({
      method: 'GET',
      url: '/system-config/permissions/detail',
      params: { tenantId, id }
    });
  },

  createPermission(request: {
    tenantId: string;
    permissionCode: string;
    permissionName: string;
    resourceType: string;
    resourceCode?: string;
    actionCode: string;
    description?: string;
  }) {
    return apiRequest<SysPermissionConfig>({
      method: 'POST',
      url: '/system-config/permissions/create',
      data: request
    });
  },

  updatePermission(request: {
    tenantId: string;
    id: string;
    permissionCode: string;
    permissionName: string;
    resourceType: string;
    resourceCode?: string;
    actionCode: string;
    description?: string;
  }) {
    return apiRequest<SysPermissionConfig>({
      method: 'POST',
      url: '/system-config/permissions/update',
      data: request
    });
  },

  updatePermissionStatus(request: {
    tenantId: string;
    id: string;
    status: 'ACTIVE' | 'DISABLED';
  }) {
    return apiRequest<SysPermissionConfig>({
      method: 'POST',
      url: '/system-config/permissions/status',
      data: request
    });
  },

  listDictItems(tenantId: string) {
    return apiRequest<SysDictItem[]>({
      method: 'GET',
      url: '/system-config/dict-items',
      params: { tenantId }
    });
  },

  listActiveDictItemsByCode(tenantId: string, dictCode: string) {
    return apiRequest<SysDictItem[]>({
      method: 'GET',
      url: '/system-config/dict-items/active',
      params: { tenantId, dictCode }
    });
  },

  getDictItemDetail(tenantId: string, id: string) {
    return apiRequest<SysDictItem>({
      method: 'GET',
      url: '/system-config/dict-items/detail',
      params: { tenantId, id }
    });
  },

  createDictItem(request: {
    tenantId: string;
    dictCode: string;
    dictName: string;
    itemCode: string;
    itemName: string;
    itemValue: string;
    sortNo?: number;
    remark?: string;
  }) {
    return apiRequest<SysDictItem>({
      method: 'POST',
      url: '/system-config/dict-items/create',
      data: request
    });
  },

  updateDictItem(request: {
    tenantId: string;
    id: string;
    dictCode: string;
    dictName: string;
    itemCode: string;
    itemName: string;
    itemValue: string;
    sortNo?: number;
    remark?: string;
  }) {
    return apiRequest<SysDictItem>({
      method: 'POST',
      url: '/system-config/dict-items/update',
      data: request
    });
  },

  updateDictItemStatus(request: {
    tenantId: string;
    id: string;
    status: 'ACTIVE' | 'DISABLED';
  }) {
    return apiRequest<SysDictItem>({
      method: 'POST',
      url: '/system-config/dict-items/status',
      data: request
    });
  },

  listRolePermissions(tenantId: string) {
    return apiRequest<SysRolePermission[]>({
      method: 'GET',
      url: '/system-config/role-permissions',
      params: { tenantId }
    });
  },

  getRolePermissionDetail(tenantId: string, id: string) {
    return apiRequest<SysRolePermission>({
      method: 'GET',
      url: '/system-config/role-permissions/detail',
      params: { tenantId, id }
    });
  },

  grantRolePermission(request: {
    tenantId: string;
    roleId: string;
    permissionId: string;
    grantSource?: string;
  }) {
    return apiRequest<SysRolePermission>({
      method: 'POST',
      url: '/system-config/role-permissions/grant',
      data: request
    });
  },

  updateRolePermissionStatus(request: {
    tenantId: string;
    id: string;
    status: 'ACTIVE' | 'DISABLED';
  }) {
    return apiRequest<SysRolePermission>({
      method: 'POST',
      url: '/system-config/role-permissions/status',
      data: request
    });
  },

  listCurrentUserMenus() {
    return apiRequest<SysMenuConfig[]>({
      method: 'GET',
      url: '/system-config/runtime/menus'
    });
  },

  listCurrentUserPermissions() {
    return apiRequest<string[]>({
      method: 'GET',
      url: '/system-config/runtime/permissions'
    });
  },

  listCurrentUserContext() {
    return apiRequest<RuntimeUserContext>({
      method: 'GET',
      url: '/system-config/runtime/context'
    });
  }
};
