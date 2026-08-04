import { apiRequest } from './http';
import type { IsoDateTime } from './contracts';

interface StatusRecord {
  id: string;
  tenantId: string;
  status: string;
  createTime: IsoDateTime;
  updateTime: IsoDateTime;
}

export interface TeachUser extends StatusRecord {
  username: string;
  realName: string;
  phone?: string;
  email?: string;
  userType: string;
  sourceType: string;
  studentNo?: string;
  employeeNo?: string;
}

export interface TeachRole extends StatusRecord {
  roleCode: string;
  roleName: string;
  description?: string;
}

export interface TeachOrg extends StatusRecord {
  parentId?: string;
  orgCode: string;
  orgName: string;
  orgType: string;
}

export interface TeachUserRole extends StatusRecord {
  userId: string;
  roleId: string;
  grantSource?: string;
}

export interface TeachUserOrg extends StatusRecord {
  userId: string;
  orgId: string;
  relationType: string;
}

export interface StudentDirectoryItem {
  studentId: string;
  studentName: string;
  username: string;
  studentNo?: string;
  unitId?: string;
  unitName?: string;
}

export const usersApi = {
  listStudents() {
    return apiRequest<StudentDirectoryItem[]>({
      method: 'GET',
      url: '/user/students'
    });
  },

  listUsers(tenantId: string) {
    return apiRequest<TeachUser[]>({
      method: 'GET',
      url: '/user',
      params: { tenantId }
    });
  },

  createUser(request: {
    tenantId: string;
    username: string;
    realName: string;
    phone?: string;
    email?: string;
    userType: string;
    sourceType: string;
    externalInfoJson?: string;
    initialPassword?: string;
    studentNo?: string;
    employeeNo?: string;
  }) {
    return apiRequest<TeachUser>({
      method: 'POST',
      url: '/user/create',
      data: request
    });
  },

  createRole(request: {
    tenantId: string;
    roleCode: string;
    roleName: string;
    description?: string;
  }) {
    return apiRequest<TeachRole>({
      method: 'POST',
      url: '/roles/create',
      data: request
    });
  },

  listRoles(tenantId: string) {
    return apiRequest<TeachRole[]>({
      method: 'GET',
      url: '/roles',
      params: { tenantId }
    });
  },

  grantRole(request: {
    tenantId: string;
    userId: string;
    roleId: string;
    grantSource?: string;
  }) {
    return apiRequest<TeachUserRole>({
      method: 'POST',
      url: '/user-roles/grant',
      data: request
    });
  },

  listUserRoles(tenantId: string) {
    return apiRequest<TeachUserRole[]>({
      method: 'GET',
      url: '/user-roles',
      params: { tenantId }
    });
  },

  createOrg(request: {
    tenantId: string;
    parentId?: string;
    orgCode: string;
    orgName: string;
    orgType: string;
  }) {
    return apiRequest<TeachOrg>({
      method: 'POST',
      url: '/orgs/create',
      data: request
    });
  },

  listOrgs(tenantId: string) {
    return apiRequest<TeachOrg[]>({
      method: 'GET',
      url: '/orgs',
      params: { tenantId }
    });
  },

  addUserToOrg(request: {
    tenantId: string;
    orgId: string;
    userId: string;
    relationType: string;
  }) {
    return apiRequest<TeachUserOrg>({
      method: 'POST',
      url: '/orgs/users/add',
      data: request
    });
  },

  removeUserFromOrg(request: {
    tenantId: string;
    orgId: string;
    userId: string;
  }) {
    return apiRequest<TeachUserOrg>({
      method: 'POST',
      url: '/orgs/users/remove',
      data: request
    });
  },

  listUserOrgs(tenantId: string) {
    return apiRequest<TeachUserOrg[]>({
      method: 'GET',
      url: '/orgs/users',
      params: { tenantId }
    });
  }
};
