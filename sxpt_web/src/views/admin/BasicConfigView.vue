<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import TimedToast from '../../components/ui/TimedToast.vue';
import {
  usersApi,
  type TeachOrg,
  type TeachRole,
  type TeachUser,
  type TeachUserOrg,
  type TeachUserRole
} from '../../api/users';
import {
  systemConfigApi,
  type SysDictItem,
  type SysMenuConfig,
  type SysPermissionConfig,
  type SysRolePermission
} from '../../api/systemConfig';
import { authApi } from '../../services/trainingApi';
import { useRuntimeContextStore } from '../../stores/runtimeContextStore';

type SectionKey =
  | 'users'
  | 'roles'
  | 'orgs'
  | 'userRoles'
  | 'userOrgs'
  | 'menus'
  | 'permissions'
  | 'dictItems'
  | 'rolePermissions';

const props = defineProps<{
  section: SectionKey;
}>();

const PAGE_SIZE = 10;
const DEFAULT_INITIAL_PASSWORD = 'Sxpt@123456';

const runtimeContextStore = useRuntimeContextStore();
const session = authApi.getSession();
const tenantId = ref(session?.user.tenantId || 'demo-tenant');
const loading = ref(false);
const dialogOpen = ref(false);
const editingUserId = ref('');
const editingRoleId = ref('');
const editingOrgId = ref('');
const editingMenuId = ref('');
const editingPermissionId = ref('');
const editingDictItemId = ref('');
const keyword = ref('');
const currentPage = ref(1);
const users = ref<TeachUser[]>([]);
const roles = ref<TeachRole[]>([]);
const orgs = ref<TeachOrg[]>([]);
const userRoles = ref<TeachUserRole[]>([]);
const userOrgs = ref<TeachUserOrg[]>([]);
const menus = ref<SysMenuConfig[]>([]);
const permissions = ref<SysPermissionConfig[]>([]);
const dictItems = ref<SysDictItem[]>([]);
const rolePermissions = ref<SysRolePermission[]>([]);
const notice = reactive({
  show: false,
  type: 'info' as 'success' | 'error' | 'info',
  message: ''
});

const createPermissionBySection: Partial<Record<SectionKey, string>> = {
  users: 'system:user:create',
  roles: 'system:role:create',
  orgs: 'system:org:create',
  userRoles: 'system:user-role:grant',
  userOrgs: 'system:user-org:grant',
  menus: 'system:menu:create',
  permissions: 'system:permission:create',
  rolePermissions: 'system:role-permission:grant',
  dictItems: 'system:dict:create'
};

const removePermissionBySection: Partial<Record<SectionKey, string>> = {
  userOrgs: 'system:user-org:remove'
};

const userActionPermission = {
  edit: 'system:user:edit',
  resetPassword: 'system:user:edit',
  enable: 'system:user:enable',
  disable: 'system:user:disable'
};

const roleActionPermission = {
  edit: 'system:role:edit',
  enable: 'system:role:enable',
  disable: 'system:role:disable'
};

const orgActionPermission = {
  edit: 'system:org:edit',
  enable: 'system:org:enable',
  disable: 'system:org:disable'
};

const userRoleActionPermission = {
  enable: 'system:user-role:enable',
  disable: 'system:user-role:disable'
};

const userOrgActionPermission = {
  enable: 'system:user-org:enable',
  disable: 'system:user-org:disable'
};

const menuActionPermission = {
  view: 'system:menu:view',
  edit: 'system:menu:edit',
  enable: 'system:menu:enable',
  disable: 'system:menu:disable'
};

const permissionActionPermission = {
  view: 'system:permission:view',
  edit: 'system:permission:edit',
  enable: 'system:permission:enable',
  disable: 'system:permission:disable'
};

const dictActionPermission = {
  view: 'system:dict:view',
  edit: 'system:dict:edit',
  enable: 'system:dict:enable',
  disable: 'system:dict:disable'
};

const rolePermissionActionPermission = {
  view: 'system:role-permission:view',
  enable: 'system:role-permission:enable',
  disable: 'system:role-permission:disable'
};

const userForm = reactive({
  username: '',
  realName: '',
  userType: 'TEACHER',
  sourceType: 'LOCAL',
  initialPassword: DEFAULT_INITIAL_PASSWORD,
  studentNo: '',
  employeeNo: '',
  phone: '',
  email: ''
});

const roleForm = reactive({
  roleCode: '',
  roleName: '',
  description: ''
});

const orgForm = reactive({
  parentId: '',
  orgCode: '',
  orgName: '',
  orgType: 'SCHOOL'
});

const userRoleForm = reactive({
  userId: '',
  roleId: '',
  grantSource: 'ADMIN'
});

const userOrgForm = reactive({
  userId: '',
  orgId: '',
  relationType: 'BELONG'
});

const menuForm = reactive({
  parentId: '',
  menuCode: '',
  menuName: '',
  menuType: 'MENU',
  routePath: '',
  componentPath: '',
  permissionCode: '',
  icon: '',
  sortNo: 0,
  visible: true
});

const permissionForm = reactive({
  permissionCode: '',
  permissionName: '',
  resourceType: 'MENU',
  resourceCode: '',
  actionCode: 'VIEW',
  description: ''
});

const dictForm = reactive({
  dictCode: '',
  dictName: '',
  itemCode: '',
  itemName: '',
  itemValue: '',
  sortNo: 0,
  remark: ''
});

const rolePermissionForm = reactive({
  roleId: '',
  permissionId: '',
  grantSource: 'ADMIN'
});

const pageMeta = computed(() => {
  const meta: Record<SectionKey, {
    eyebrow: string;
    title: string;
    description: string;
    actionText: string;
    emptyText: string;
  }> = {
    users: {
      eyebrow: '基础配置 / 用户管理',
      title: '用户账号维护',
      description: '维护教学平台内的管理员、教师、学生账号，为后续授权、单位归属和任务发布提供身份基础。',
      actionText: '新增用户',
      emptyText: '暂无用户'
    },
    roles: {
      eyebrow: '基础配置 / 角色管理',
      title: '角色维护',
      description: '维护教学平台角色，用于表达用户在平台内可承担的管理、教学或学习职责。',
      actionText: '新增角色',
      emptyText: '暂无角色'
    },
    orgs: {
      eyebrow: '基础配置 / 单位管理',
      title: '单位维护',
      description: '维护学校、院系、班级等教学平台组织结构，为用户归属和任务范围选择提供基础数据。',
      actionText: '新增单位',
      emptyText: '暂无单位'
    },
    userRoles: {
      eyebrow: '基础配置 / 用户角色',
      title: '用户角色绑定',
      description: '将用户和角色建立绑定关系，明确账号在教学平台内可承担的业务身份。',
      actionText: '绑定角色',
      emptyText: '暂无用户角色绑定'
    },
    userOrgs: {
      eyebrow: '基础配置 / 用户单位',
      title: '用户单位绑定',
      description: '将用户绑定到教学单位或班级，支撑教师按班级发布任务、学生按归属进入练习。',
      actionText: '绑定单位',
      emptyText: '暂无用户单位绑定'
    },
    menus: {
      eyebrow: '基础配置 / 菜单管理',
      title: '菜单配置维护',
      description: '维护平台菜单入口、路由地址和关联权限编码，让后台功能入口有统一台账可追溯。',
      actionText: '新增菜单',
      emptyText: '暂无菜单配置'
    },
    permissions: {
      eyebrow: '基础配置 / 权限管理',
      title: '权限点维护',
      description: '维护菜单、按钮、接口和数据范围的权限点，为后续角色授权和访问控制提供稳定来源。',
      actionText: '新增权限',
      emptyText: '暂无权限配置'
    },
    dictItems: {
      eyebrow: '基础配置 / 字典配置',
      title: '字典项维护',
      description: '维护页面下拉选项、状态枚举和业务分类，减少系统配置散落在代码中的硬编码。',
      actionText: '新增字典项',
      emptyText: '暂无字典项'
    },
    rolePermissions: {
      eyebrow: '基础配置 / 角色权限',
      title: '角色权限绑定',
      description: '将系统权限点授权给教学平台角色，为后续菜单显示、按钮控制和接口鉴权提供配置依据。',
      actionText: '绑定权限',
      emptyText: '暂无角色权限绑定'
    }
  };
  return meta[props.section];
});

const rows = computed(() => {
  if (props.section === 'users') return users.value;
  if (props.section === 'roles') return roles.value;
  if (props.section === 'orgs') return orgs.value;
  if (props.section === 'userRoles') return userRoles.value;
  if (props.section === 'userOrgs') return userOrgs.value;
  if (props.section === 'menus') return menus.value;
  if (props.section === 'permissions') return permissions.value;
  if (props.section === 'rolePermissions') return rolePermissions.value;
  return dictItems.value;
});

const filteredRows = computed(() => {
  const key = keyword.value.trim().toLowerCase();
  if (!key) return rows.value;
  return rows.value.filter((row) => rowSearchText(row).includes(key));
});

const totalPages = computed(() =>
  Math.max(1, Math.ceil(filteredRows.value.length / PAGE_SIZE))
);

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * PAGE_SIZE;
  return filteredRows.value.slice(start, start + PAGE_SIZE);
});

const emptyColspan = computed(() => {
  if (props.section === 'users') return canOperateUser.value ? 7 : 6;
  if (props.section === 'roles') return canOperateRole.value ? 5 : 4;
  if (props.section === 'orgs') return canOperateOrg.value ? 6 : 5;
  if (props.section === 'userRoles') return canOperateUserRole.value ? 5 : 4;
  if (props.section === 'userOrgs') return canOperateUserOrg.value ? 5 : 4;
  if (props.section === 'menus') return canOperateMenu.value ? 8 : 7;
  if (props.section === 'permissions') return canOperatePermission.value ? 7 : 6;
  if (props.section === 'rolePermissions') return canOperateRolePermission.value ? 5 : 4;
  if (props.section === 'dictItems') return canOperateDictItem.value ? 8 : 7;
  return 4;
});

const canCreateCurrent = computed(() =>
  hasRuntimePermission(createPermissionBySection[props.section])
);

const canRemoveCurrent = computed(() =>
  hasRuntimePermission(removePermissionBySection[props.section])
);

const canOperateUser = computed(() =>
  props.section === 'users' &&
  (
    hasRuntimePermission(userActionPermission.edit) ||
    hasRuntimePermission(userActionPermission.enable) ||
    hasRuntimePermission(userActionPermission.disable)
  )
);

const canOperateRole = computed(() =>
  props.section === 'roles' &&
  (
    hasRuntimePermission(roleActionPermission.edit) ||
    hasRuntimePermission(roleActionPermission.enable) ||
    hasRuntimePermission(roleActionPermission.disable)
  )
);

const canOperateOrg = computed(() =>
  props.section === 'orgs' &&
  (
    hasRuntimePermission(orgActionPermission.edit) ||
    hasRuntimePermission(orgActionPermission.enable) ||
    hasRuntimePermission(orgActionPermission.disable)
  )
);

const canOperateUserRole = computed(() =>
  props.section === 'userRoles' &&
  (
    hasRuntimePermission(userRoleActionPermission.enable) ||
    hasRuntimePermission(userRoleActionPermission.disable)
  )
);

const canOperateUserOrg = computed(() =>
  props.section === 'userOrgs' &&
  (
    canRemoveCurrent.value ||
    hasRuntimePermission(userOrgActionPermission.enable) ||
    hasRuntimePermission(userOrgActionPermission.disable)
  )
);

const canOperateMenu = computed(() =>
  props.section === 'menus' &&
  (
    hasRuntimePermission(menuActionPermission.view) ||
    hasRuntimePermission(menuActionPermission.edit) ||
    hasRuntimePermission(menuActionPermission.enable) ||
    hasRuntimePermission(menuActionPermission.disable)
  )
);

const canOperatePermission = computed(() =>
  props.section === 'permissions' &&
  (
    hasRuntimePermission(permissionActionPermission.view) ||
    hasRuntimePermission(permissionActionPermission.edit) ||
    hasRuntimePermission(permissionActionPermission.enable) ||
    hasRuntimePermission(permissionActionPermission.disable)
  )
);

const canOperateDictItem = computed(() =>
  props.section === 'dictItems' &&
  (
    hasRuntimePermission(dictActionPermission.view) ||
    hasRuntimePermission(dictActionPermission.edit) ||
    hasRuntimePermission(dictActionPermission.enable) ||
    hasRuntimePermission(dictActionPermission.disable)
  )
);

const canOperateRolePermission = computed(() =>
  props.section === 'rolePermissions' &&
  (
    hasRuntimePermission(rolePermissionActionPermission.view) ||
    hasRuntimePermission(rolePermissionActionPermission.enable) ||
    hasRuntimePermission(rolePermissionActionPermission.disable)
  )
);

const dialogTitle = computed(() =>
  editingUserId.value
    ? '编辑用户'
    : editingRoleId.value
      ? '编辑角色'
      : editingOrgId.value
        ? '编辑单位'
        : editingMenuId.value
    ? '编辑菜单'
    : editingPermissionId.value
      ? '编辑权限'
      : editingDictItemId.value
        ? '编辑字典项'
        : pageMeta.value.actionText
);

onMounted(async () => {
  await Promise.all([loadRuntimeContext(), loadPageData()]);
});

watch(
  () => props.section,
  async () => {
    keyword.value = '';
    currentPage.value = 1;
    closeDialog();
    await loadPageData();
  }
);

watch(keyword, () => {
  currentPage.value = 1;
});

/**
 * 业务功能：按当前配置入口加载必要数据，保证绑定类页面具备用户、角色、单位的下拉来源。
 * 关键流程：基础主数据全部复用租户维度查询，避免页面内写死演示数据。
 */
async function loadPageData() {
  await run(async () => {
    if (props.section === 'users') {
      users.value = await usersApi.listUsers(tenantId.value);
    } else if (props.section === 'roles') {
      roles.value = await usersApi.listRoles(tenantId.value);
    } else if (props.section === 'orgs') {
      orgs.value = await usersApi.listOrgs(tenantId.value);
    } else if (props.section === 'userRoles') {
      await loadIdentityOptions();
      userRoles.value = await usersApi.listUserRoles(tenantId.value);
    } else if (props.section === 'userOrgs') {
      await loadIdentityOptions();
      userOrgs.value = await usersApi.listUserOrgs(tenantId.value);
    } else if (props.section === 'menus') {
      menus.value = await systemConfigApi.listMenus(tenantId.value);
    } else if (props.section === 'permissions') {
      permissions.value = await systemConfigApi.listPermissions(tenantId.value);
    } else if (props.section === 'rolePermissions') {
      await loadRolePermissionOptions();
      rolePermissions.value = await systemConfigApi.listRolePermissions(tenantId.value);
    } else {
      dictItems.value = await systemConfigApi.listDictItems(tenantId.value);
    }
    normalizePage();
  }, '列表已刷新');
}

/**
 * 业务功能：加载当前登录用户的角色、单位、权限和菜单上下文，用于前端入口级显隐。
 * 关键流程：接口权限暂不拦截，因此这里失败时保持页面可用，避免基础配置尚未初始化时把管理员锁在门外。
 */
async function loadRuntimeContext() {
  await runtimeContextStore.loadRuntimeContext();
}

/**
 * 业务功能：加载用户、角色、单位基础选项，供绑定页面选择前置对象。
 * 关键流程：只在绑定页面集中加载，降低普通列表页的接口噪声。
 */
async function loadIdentityOptions() {
  const [userList, roleList, orgList] = await Promise.all([
    usersApi.listUsers(tenantId.value),
    usersApi.listRoles(tenantId.value),
    usersApi.listOrgs(tenantId.value)
  ]);
  users.value = userList;
  roles.value = roleList;
  orgs.value = orgList;
}

/**
 * 业务功能：打开新增或绑定弹窗，并为下拉框准备默认选择。
 * 关键流程：绑定类表单优先选择第一条可用主数据，减少空提交概率。
 */
async function openCreateDialog() {
  if (!canCreateCurrent.value) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }

  editingUserId.value = '';
  editingRoleId.value = '';
  editingOrgId.value = '';
  editingMenuId.value = '';
  editingPermissionId.value = '';
  editingDictItemId.value = '';
  if (props.section === 'userRoles' || props.section === 'userOrgs') {
    await run(loadIdentityOptions, '可选项已加载');
    userRoleForm.userId = users.value[0]?.id || '';
    userRoleForm.roleId = roles.value[0]?.id || '';
    userOrgForm.userId = users.value[0]?.id || '';
    userOrgForm.orgId = orgs.value[0]?.id || '';
  } else if (props.section === 'rolePermissions') {
    await run(loadRolePermissionOptions, '可选项已加载');
    rolePermissionForm.roleId = roles.value[0]?.id || '';
    rolePermissionForm.permissionId = permissions.value[0]?.id || '';
  }
  dialogOpen.value = true;
}

/**
 * 业务功能：查看菜单详情。
 * 关键流程：从后端读取最新菜单配置，避免列表缓存和详情展示不一致。
 */
function openEditUserDialog(row: TeachUser) {
  if (!hasRuntimePermission(userActionPermission.edit)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  editingUserId.value = row.id;
  fillUserForm(row);
  dialogOpen.value = true;
}

function openEditRoleDialog(row: TeachRole) {
  if (!hasRuntimePermission(roleActionPermission.edit)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  editingRoleId.value = row.id;
  fillRoleForm(row);
  dialogOpen.value = true;
}

function openEditOrgDialog(row: TeachOrg) {
  if (!hasRuntimePermission(orgActionPermission.edit)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  editingOrgId.value = row.id;
  fillOrgForm(row);
  dialogOpen.value = true;
}

async function toggleUserStatus(row: TeachUser) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const permissionCode =
    nextStatus === 'ACTIVE' ? userActionPermission.enable : userActionPermission.disable;
  if (!hasRuntimePermission(permissionCode)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  await run(async () => {
    await usersApi.updateUserStatus({
      tenantId: tenantId.value,
      id: row.id,
      status: nextStatus
    });
    await loadPageData();
    await refreshRuntimeContextAfterConfigChange();
  }, nextStatus === 'ACTIVE' ? '用户已启用' : '用户已停用');
}

async function resetUserPassword(row: TeachUser) {
  if (!hasRuntimePermission(userActionPermission.resetPassword)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  const confirmed = window.confirm(`确认将 ${row.realName || row.username} 的密码重置为 ${DEFAULT_INITIAL_PASSWORD} 吗？`);
  if (!confirmed) return;
  await run(async () => {
    await usersApi.resetUserPassword({
      tenantId: tenantId.value,
      id: row.id
    });
    await loadPageData();
  }, `密码已重置为 ${DEFAULT_INITIAL_PASSWORD}`);
}

async function toggleRoleStatus(row: TeachRole) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const permissionCode =
    nextStatus === 'ACTIVE' ? roleActionPermission.enable : roleActionPermission.disable;
  if (!hasRuntimePermission(permissionCode)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  await run(async () => {
    await usersApi.updateRoleStatus({
      tenantId: tenantId.value,
      id: row.id,
      status: nextStatus
    });
    await loadPageData();
    await refreshRuntimeContextAfterConfigChange();
  }, nextStatus === 'ACTIVE' ? '角色已启用' : '角色已停用');
}

async function toggleOrgStatus(row: TeachOrg) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const permissionCode =
    nextStatus === 'ACTIVE' ? orgActionPermission.enable : orgActionPermission.disable;
  if (!hasRuntimePermission(permissionCode)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  await run(async () => {
    await usersApi.updateOrgStatus({
      tenantId: tenantId.value,
      id: row.id,
      status: nextStatus
    });
    await loadPageData();
    await refreshRuntimeContextAfterConfigChange();
  }, nextStatus === 'ACTIVE' ? '单位已启用' : '单位已停用');
}

async function toggleUserRoleStatus(row: TeachUserRole) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const permissionCode =
    nextStatus === 'ACTIVE' ? userRoleActionPermission.enable : userRoleActionPermission.disable;
  if (!hasRuntimePermission(permissionCode)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  await run(async () => {
    await usersApi.updateUserRoleStatus({
      tenantId: tenantId.value,
      id: row.id,
      status: nextStatus
    });
    await loadPageData();
    await refreshRuntimeContextAfterConfigChange();
  }, nextStatus === 'ACTIVE' ? '用户角色已启用' : '用户角色已停用');
}

async function toggleUserOrgStatus(row: TeachUserOrg) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const permissionCode =
    nextStatus === 'ACTIVE' ? userOrgActionPermission.enable : userOrgActionPermission.disable;
  if (!hasRuntimePermission(permissionCode)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  await run(async () => {
    await usersApi.updateUserOrgStatus({
      tenantId: tenantId.value,
      id: row.id,
      status: nextStatus
    });
    await loadPageData();
    await refreshRuntimeContextAfterConfigChange();
  }, nextStatus === 'ACTIVE' ? '用户单位已启用' : '用户单位已停用');
}

async function viewMenuDetail(row: SysMenuConfig) {
  if (!hasRuntimePermission(menuActionPermission.view)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  loading.value = true;
  closeNotice();
  try {
    const detail = await systemConfigApi.getMenuDetail(tenantId.value, row.id);
    notify(
      'info',
      `${detail.menuName}：${detail.routePath || '无路由'}，状态 ${statusText(detail.status)}`
    );
  } catch (error) {
    notify('error', error instanceof Error ? error.message : '详情加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 业务功能：打开菜单编辑弹窗。
 * 关键流程：编辑前读取最新详情并回填表单，避免基于过期列表数据覆盖后台配置。
 */
async function openEditMenuDialog(row: SysMenuConfig) {
  if (!hasRuntimePermission(menuActionPermission.edit)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  await run(async () => {
    const detail = await systemConfigApi.getMenuDetail(tenantId.value, row.id);
    editingMenuId.value = detail.id;
    fillMenuForm(detail);
    dialogOpen.value = true;
  }, '菜单详情已加载');
}

/**
 * 业务功能：启用或停用菜单。
 * 关键流程：只切换 ACTIVE/DISABLED，成功后刷新列表和运行时上下文，让侧边栏立刻跟随配置变化。
 */
async function toggleMenuStatus(row: SysMenuConfig) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const permissionCode =
    nextStatus === 'ACTIVE' ? menuActionPermission.enable : menuActionPermission.disable;
  if (!hasRuntimePermission(permissionCode)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  await run(async () => {
    await systemConfigApi.updateMenuStatus({
      tenantId: tenantId.value,
      id: row.id,
      status: nextStatus
    });
    await loadPageData();
    await refreshRuntimeContextAfterConfigChange();
  }, nextStatus === 'ACTIVE' ? '菜单已启用' : '菜单已停用');
}

/**
 * 业务功能：查看权限详情。
 * 关键流程：从后端读取最新权限点，避免列表缓存和详情展示不一致。
 */
async function viewPermissionDetail(row: SysPermissionConfig) {
  if (!hasRuntimePermission(permissionActionPermission.view)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  loading.value = true;
  closeNotice();
  try {
    const detail = await systemConfigApi.getPermissionDetail(tenantId.value, row.id);
    notify(
      'info',
      `${detail.permissionName}：${detail.permissionCode}，${detail.resourceType}/${detail.actionCode}`
    );
  } catch (error) {
    notify('error', error instanceof Error ? error.message : '详情加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 业务功能：打开权限编辑弹窗。
 * 关键流程：编辑前读取最新详情并回填表单，避免基于过期列表数据覆盖后台配置。
 */
async function openEditPermissionDialog(row: SysPermissionConfig) {
  if (!hasRuntimePermission(permissionActionPermission.edit)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  await run(async () => {
    const detail = await systemConfigApi.getPermissionDetail(tenantId.value, row.id);
    editingPermissionId.value = detail.id;
    fillPermissionForm(detail);
    dialogOpen.value = true;
  }, '权限详情已加载');
}

/**
 * 业务功能：启用或停用权限点。
 * 关键流程：只切换 ACTIVE/DISABLED，成功后刷新列表和运行时上下文，让按钮显隐立刻跟随配置变化。
 */
async function togglePermissionStatus(row: SysPermissionConfig) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const permissionCode =
    nextStatus === 'ACTIVE'
      ? permissionActionPermission.enable
      : permissionActionPermission.disable;
  if (!hasRuntimePermission(permissionCode)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  await run(async () => {
    await systemConfigApi.updatePermissionStatus({
      tenantId: tenantId.value,
      id: row.id,
      status: nextStatus
    });
    await loadPageData();
    await refreshRuntimeContextAfterConfigChange();
  }, nextStatus === 'ACTIVE' ? '权限已启用' : '权限已停用');
}

/**
 * 业务功能：查看字典项详情。
 * 关键流程：从后端读取最新字典项，避免列表缓存和详情展示不一致。
 */
async function viewDictItemDetail(row: SysDictItem) {
  if (!hasRuntimePermission(dictActionPermission.view)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  loading.value = true;
  closeNotice();
  try {
    const detail = await systemConfigApi.getDictItemDetail(tenantId.value, row.id);
    notify(
      'info',
      `${detail.dictName}：${detail.itemName}=${detail.itemValue}，状态 ${statusText(detail.status)}`
    );
  } catch (error) {
    notify('error', error instanceof Error ? error.message : '详情加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 业务功能：打开字典项编辑弹窗。
 * 关键流程：编辑前读取最新详情并回填表单，避免基于过期列表数据覆盖后台配置。
 */
async function openEditDictItemDialog(row: SysDictItem) {
  if (!hasRuntimePermission(dictActionPermission.edit)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  await run(async () => {
    const detail = await systemConfigApi.getDictItemDetail(tenantId.value, row.id);
    editingDictItemId.value = detail.id;
    fillDictForm(detail);
    dialogOpen.value = true;
  }, '字典项详情已加载');
}

/**
 * 业务功能：启用或停用字典项。
 * 关键流程：只切换 ACTIVE/DISABLED，成功后刷新列表和运行时上下文，为后续字典读取统一状态语义。
 */
async function toggleDictItemStatus(row: SysDictItem) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const permissionCode =
    nextStatus === 'ACTIVE' ? dictActionPermission.enable : dictActionPermission.disable;
  if (!hasRuntimePermission(permissionCode)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  await run(async () => {
    await systemConfigApi.updateDictItemStatus({
      tenantId: tenantId.value,
      id: row.id,
      status: nextStatus
    });
    await loadPageData();
    await refreshRuntimeContextAfterConfigChange();
  }, nextStatus === 'ACTIVE' ? '字典项已启用' : '字典项已停用');
}

/**
 * 业务功能：查看角色权限绑定详情。
 * 关键流程：从后端读取最新授权关系，前端再结合本页已加载的角色和权限列表展示可读名称。
 */
async function viewRolePermissionDetail(row: SysRolePermission) {
  if (!hasRuntimePermission(rolePermissionActionPermission.view)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  loading.value = true;
  closeNotice();
  try {
    const detail = await systemConfigApi.getRolePermissionDetail(tenantId.value, row.id);
    notify(
      'info',
      `${roleName(detail.roleId)} -> ${permissionName(detail.permissionId)}，状态 ${statusText(detail.status)}`
    );
  } catch (error) {
    notify('error', error instanceof Error ? error.message : '详情加载失败');
  } finally {
    loading.value = false;
  }
}

/**
 * 业务功能：启用或停用角色权限绑定。
 * 关键流程：只切换 ACTIVE/DISABLED，停用后运行时权限解析会自然移除该授权。
 */
async function toggleRolePermissionStatus(row: SysRolePermission) {
  const nextStatus = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
  const permissionCode =
    nextStatus === 'ACTIVE'
      ? rolePermissionActionPermission.enable
      : rolePermissionActionPermission.disable;
  if (!hasRuntimePermission(permissionCode)) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }
  await run(async () => {
    await systemConfigApi.updateRolePermissionStatus({
      tenantId: tenantId.value,
      id: row.id,
      status: nextStatus
    });
    await loadPageData();
    await refreshRuntimeContextAfterConfigChange();
  }, nextStatus === 'ACTIVE' ? '授权已启用' : '授权已停用');
}

/**
 * 业务功能：提交当前入口的新增或绑定表单。
 * 关键流程：先进行前端必填校验，再调用后端真实接口，成功后重新加载列表保证数据一致。
 */
async function submitForm() {
  const validation = validateForm();
  if (validation) {
    notify('error', validation);
    return;
  }

  await run(async () => {
    if (props.section === 'users') {
      if (editingUserId.value) {
        await usersApi.updateUser({
          tenantId: tenantId.value,
          id: editingUserId.value,
          realName: userForm.realName.trim(),
          userType: userForm.userType,
          sourceType: userForm.sourceType,
          studentNo: userForm.studentNo.trim() || undefined,
          employeeNo: userForm.employeeNo.trim() || undefined,
          phone: userForm.phone.trim() || undefined,
          email: userForm.email.trim() || undefined
        });
      } else {
        await usersApi.createUser({
          tenantId: tenantId.value,
          username: userForm.username.trim(),
          realName: userForm.realName.trim(),
          userType: userForm.userType,
          sourceType: userForm.sourceType,
          initialPassword: userForm.initialPassword.trim() || undefined,
          studentNo: userForm.studentNo.trim() || undefined,
          employeeNo: userForm.employeeNo.trim() || undefined,
          phone: userForm.phone.trim() || undefined,
          email: userForm.email.trim() || undefined
        });
      }
      resetUserForm();
      editingUserId.value = '';
    } else if (props.section === 'roles') {
      if (editingRoleId.value) {
        await usersApi.updateRole({
          tenantId: tenantId.value,
          id: editingRoleId.value,
          roleName: roleForm.roleName.trim(),
          description: roleForm.description.trim() || undefined
        });
      } else {
        await usersApi.createRole({
          tenantId: tenantId.value,
          roleCode: roleForm.roleCode.trim(),
          roleName: roleForm.roleName.trim(),
          description: roleForm.description.trim() || undefined
        });
      }
      resetRoleForm();
      editingRoleId.value = '';
    } else if (props.section === 'orgs') {
      if (editingOrgId.value) {
        await usersApi.updateOrg({
          tenantId: tenantId.value,
          id: editingOrgId.value,
          parentId: orgForm.parentId || undefined,
          orgName: orgForm.orgName.trim(),
          orgType: orgForm.orgType
        });
      } else {
        await usersApi.createOrg({
          tenantId: tenantId.value,
          parentId: orgForm.parentId || undefined,
          orgCode: orgForm.orgCode.trim(),
          orgName: orgForm.orgName.trim(),
          orgType: orgForm.orgType
        });
      }
      resetOrgForm();
      editingOrgId.value = '';
    } else if (props.section === 'userRoles') {
      await usersApi.grantRole({
        tenantId: tenantId.value,
        userId: userRoleForm.userId,
        roleId: userRoleForm.roleId,
        grantSource: userRoleForm.grantSource.trim() || undefined
      });
    } else if (props.section === 'userOrgs') {
      await usersApi.addUserToOrg({
        tenantId: tenantId.value,
        userId: userOrgForm.userId,
        orgId: userOrgForm.orgId,
        relationType: userOrgForm.relationType
      });
    } else if (props.section === 'menus') {
      const request = {
        tenantId: tenantId.value,
        parentId: menuForm.parentId || undefined,
        menuCode: menuForm.menuCode.trim(),
        menuName: menuForm.menuName.trim(),
        menuType: menuForm.menuType,
        routePath: menuForm.routePath.trim() || undefined,
        componentPath: menuForm.componentPath.trim() || undefined,
        permissionCode: menuForm.permissionCode.trim() || undefined,
        icon: menuForm.icon.trim() || undefined,
        sortNo: Number(menuForm.sortNo) || 0,
        visible: menuForm.visible
      };
      if (editingMenuId.value) {
        await systemConfigApi.updateMenu({
          ...request,
          id: editingMenuId.value
        });
      } else {
        await systemConfigApi.createMenu(request);
      }
      resetMenuForm();
      editingMenuId.value = '';
    } else if (props.section === 'permissions') {
      const request = {
        tenantId: tenantId.value,
        permissionCode: permissionForm.permissionCode.trim(),
        permissionName: permissionForm.permissionName.trim(),
        resourceType: permissionForm.resourceType,
        resourceCode: permissionForm.resourceCode.trim() || undefined,
        actionCode: permissionForm.actionCode,
        description: permissionForm.description.trim() || undefined
      };
      if (editingPermissionId.value) {
        await systemConfigApi.updatePermission({
          ...request,
          id: editingPermissionId.value
        });
      } else {
        await systemConfigApi.createPermission(request);
      }
      resetPermissionForm();
      editingPermissionId.value = '';
    } else if (props.section === 'dictItems') {
      const request = {
        tenantId: tenantId.value,
        dictCode: dictForm.dictCode.trim(),
        dictName: dictForm.dictName.trim(),
        itemCode: dictForm.itemCode.trim(),
        itemName: dictForm.itemName.trim(),
        itemValue: dictForm.itemValue.trim(),
        sortNo: Number(dictForm.sortNo) || 0,
        remark: dictForm.remark.trim() || undefined
      };
      if (editingDictItemId.value) {
        await systemConfigApi.updateDictItem({
          ...request,
          id: editingDictItemId.value
        });
      } else {
        await systemConfigApi.createDictItem(request);
      }
      resetDictForm();
      editingDictItemId.value = '';
    } else {
      await systemConfigApi.grantRolePermission({
        tenantId: tenantId.value,
        roleId: rolePermissionForm.roleId,
        permissionId: rolePermissionForm.permissionId,
        grantSource: rolePermissionForm.grantSource.trim() || undefined
      });
    }
    dialogOpen.value = false;
    await loadPageData();
    await refreshRuntimeContextAfterConfigChange();
  }, '保存成功');
}

/**
 * 业务功能：移除用户单位绑定关系。
 * 关键流程：当前后端只提供用户单位移除接口，因此只在用户单位页面开放真实删除动作。
 */
async function removeUserOrg(row: TeachUserOrg) {
  if (!canRemoveCurrent.value) {
    notify('error', '当前角色暂无该操作入口');
    return;
  }

  await run(async () => {
    await usersApi.removeUserFromOrg({
      tenantId: tenantId.value,
      userId: row.userId,
      orgId: row.orgId
    });
    await loadPageData();
    await refreshRuntimeContextAfterConfigChange();
  }, '绑定已移除');
}

/**
 * 业务功能：判断当前页面操作是否应该展示给登录用户。
 * 关键流程：超级管理员保留全量入口；未完成权限种子初始化时走兼容模式，防止部署初期无权限码导致页面不可维护。
 */
function hasRuntimePermission(permissionCode?: string) {
  return runtimeContextStore.hasPermission(permissionCode);
}

/**
 * 业务功能：基础配置变化后刷新当前用户运行时上下文。
 * 关键流程：菜单、权限、角色、单位绑定都会影响导航和按钮显隐，保存后强制刷新可避免继续使用旧缓存。
 */
async function refreshRuntimeContextAfterConfigChange() {
  await runtimeContextStore.loadRuntimeContext({ force: true });
}

/**
 * 业务功能：统一执行异步请求并转换为短时提示。
 * 关键流程：请求前关闭旧提示，请求后用 1 秒自动关闭的提示反馈结果。
 */
async function run(action: () => Promise<void>, successMessage: string) {
  loading.value = true;
  closeNotice();
  try {
    await action();
    notify('success', successMessage);
  } catch (error) {
    notify('error', error instanceof Error ? error.message : '操作失败');
  } finally {
    loading.value = false;
  }
}

function validateForm() {
  if (props.section === 'users') {
    if (!userForm.username.trim()) return '请填写登录账号';
    if (!userForm.realName.trim()) return '请填写用户姓名';
  }
  if (props.section === 'roles') {
    if (!roleForm.roleCode.trim()) return '请填写角色编码';
    if (!roleForm.roleName.trim()) return '请填写角色名称';
  }
  if (props.section === 'orgs') {
    if (!orgForm.orgCode.trim()) return '请填写单位编码';
    if (!orgForm.orgName.trim()) return '请填写单位名称';
  }
  if (props.section === 'userRoles') {
    if (!userRoleForm.userId || !userRoleForm.roleId) return '请选择用户和角色';
  }
  if (props.section === 'userOrgs') {
    if (!userOrgForm.userId || !userOrgForm.orgId) return '请选择用户和单位';
  }
  if (props.section === 'menus') {
    if (!menuForm.menuCode.trim()) return '请填写菜单编码';
    if (!menuForm.menuName.trim()) return '请填写菜单名称';
  }
  if (props.section === 'permissions') {
    if (!permissionForm.permissionCode.trim()) return '请填写权限编码';
    if (!permissionForm.permissionName.trim()) return '请填写权限名称';
  }
  if (props.section === 'dictItems') {
    if (!dictForm.dictCode.trim()) return '请填写字典编码';
    if (!dictForm.dictName.trim()) return '请填写字典名称';
    if (!dictForm.itemCode.trim()) return '请填写字典项编码';
    if (!dictForm.itemName.trim()) return '请填写字典项名称';
    if (!dictForm.itemValue.trim()) return '请填写字典项值';
  }
  if (props.section === 'rolePermissions') {
    if (!rolePermissionForm.roleId || !rolePermissionForm.permissionId) return '请选择角色和权限';
  }
  return '';
}

function rowSearchText(
  row:
    | TeachUser
    | TeachRole
    | TeachOrg
    | TeachUserRole
    | TeachUserOrg
    | SysMenuConfig
    | SysPermissionConfig
    | SysDictItem
    | SysRolePermission
) {
  if ('username' in row) return `${row.username} ${row.realName}`.toLowerCase();
  if ('roleCode' in row) return `${row.roleCode} ${row.roleName}`.toLowerCase();
  if ('orgCode' in row) return `${row.orgCode} ${row.orgName}`.toLowerCase();
  if ('menuCode' in row) return `${row.menuCode} ${row.menuName} ${row.routePath || ''}`.toLowerCase();
  if ('permissionCode' in row) return `${row.permissionCode} ${row.permissionName} ${row.resourceCode || ''}`.toLowerCase();
  if ('dictCode' in row) return `${row.dictCode} ${row.dictName} ${row.itemCode} ${row.itemName}`.toLowerCase();
  if (isRolePermission(row)) {
    return `${roleName(row.roleId)} ${permissionName(row.permissionId)}`.toLowerCase();
  }
  if ('roleId' in row) return `${userName(row.userId)} ${roleName(row.roleId)}`.toLowerCase();
  return `${userName(row.userId)} ${orgName(row.orgId)}`.toLowerCase();
}

function isRolePermission(
  row:
    | TeachUser
    | TeachRole
    | TeachOrg
    | TeachUserRole
    | TeachUserOrg
    | SysMenuConfig
    | SysPermissionConfig
    | SysDictItem
    | SysRolePermission
): row is SysRolePermission {
  return 'permissionId' in row && 'roleId' in row && !('userId' in row);
}

function userName(userId: string) {
  const user = users.value.find((item) => item.id === userId);
  return user ? `${user.realName}（${user.username}）` : userId;
}

function roleName(roleId: string) {
  const role = roles.value.find((item) => item.id === roleId);
  return role ? `${role.roleName}（${role.roleCode}）` : roleId;
}

function permissionName(permissionId: string) {
  const permission = permissions.value.find((item) => item.id === permissionId);
  return permission
    ? `${permission.permissionName}（${permission.permissionCode}）`
    : permissionId;
}

/**
 * 业务功能：加载角色权限绑定所需下拉选项。
 * 关键流程：角色来自用户模块，权限来自系统配置模块，页面只做弱引用展示和提交。
 */
async function loadRolePermissionOptions() {
  const [roleList, permissionList] = await Promise.all([
    usersApi.listRoles(tenantId.value),
    systemConfigApi.listPermissions(tenantId.value)
  ]);
  roles.value = roleList;
  permissions.value = permissionList;
}

function orgName(orgId: string) {
  const org = orgs.value.find((item) => item.id === orgId);
  return org ? `${org.orgName}（${org.orgCode}）` : orgId;
}

function statusText(status: string) {
  return status === 'ACTIVE' ? '启用' : status;
}

function normalizePage() {
  if (currentPage.value > totalPages.value) currentPage.value = totalPages.value;
}

function previousPage() {
  currentPage.value = Math.max(1, currentPage.value - 1);
}

function nextPage() {
  currentPage.value = Math.min(totalPages.value, currentPage.value + 1);
}

function closeDialog() {
  dialogOpen.value = false;
  editingUserId.value = '';
  editingRoleId.value = '';
  editingOrgId.value = '';
  editingMenuId.value = '';
  editingPermissionId.value = '';
  editingDictItemId.value = '';
}

function notify(type: 'success' | 'error' | 'info', message: string) {
  notice.type = type;
  notice.message = message;
  notice.show = true;
}

function closeNotice() {
  notice.show = false;
}

function resetUserForm() {
  userForm.username = '';
  userForm.realName = '';
  userForm.userType = 'TEACHER';
  userForm.sourceType = 'LOCAL';
  userForm.initialPassword = DEFAULT_INITIAL_PASSWORD;
  userForm.studentNo = '';
  userForm.employeeNo = '';
  userForm.phone = '';
  userForm.email = '';
}

function fillUserForm(user: TeachUser) {
  userForm.username = user.username;
  userForm.realName = user.realName;
  userForm.userType = user.userType;
  userForm.sourceType = user.sourceType;
  userForm.initialPassword = '';
  userForm.studentNo = user.studentNo || '';
  userForm.employeeNo = user.employeeNo || '';
  userForm.phone = user.phone || '';
  userForm.email = user.email || '';
}

function resetRoleForm() {
  roleForm.roleCode = '';
  roleForm.roleName = '';
  roleForm.description = '';
}

function fillRoleForm(role: TeachRole) {
  roleForm.roleCode = role.roleCode;
  roleForm.roleName = role.roleName;
  roleForm.description = role.description || '';
}

function resetOrgForm() {
  orgForm.parentId = '';
  orgForm.orgCode = '';
  orgForm.orgName = '';
  orgForm.orgType = 'SCHOOL';
}

function fillOrgForm(org: TeachOrg) {
  orgForm.parentId = org.parentId || '';
  orgForm.orgCode = org.orgCode;
  orgForm.orgName = org.orgName;
  orgForm.orgType = org.orgType;
}

function resetMenuForm() {
  menuForm.parentId = '';
  menuForm.menuCode = '';
  menuForm.menuName = '';
  menuForm.menuType = 'MENU';
  menuForm.routePath = '';
  menuForm.componentPath = '';
  menuForm.permissionCode = '';
  menuForm.icon = '';
  menuForm.sortNo = 0;
  menuForm.visible = true;
}

function fillMenuForm(menu: SysMenuConfig) {
  menuForm.parentId = menu.parentId || '';
  menuForm.menuCode = menu.menuCode;
  menuForm.menuName = menu.menuName;
  menuForm.menuType = menu.menuType;
  menuForm.routePath = menu.routePath || '';
  menuForm.componentPath = menu.componentPath || '';
  menuForm.permissionCode = menu.permissionCode || '';
  menuForm.icon = menu.icon || '';
  menuForm.sortNo = menu.sortNo || 0;
  menuForm.visible = menu.visible;
}

function resetPermissionForm() {
  permissionForm.permissionCode = '';
  permissionForm.permissionName = '';
  permissionForm.resourceType = 'MENU';
  permissionForm.resourceCode = '';
  permissionForm.actionCode = 'VIEW';
  permissionForm.description = '';
}

function fillPermissionForm(permission: SysPermissionConfig) {
  permissionForm.permissionCode = permission.permissionCode;
  permissionForm.permissionName = permission.permissionName;
  permissionForm.resourceType = permission.resourceType;
  permissionForm.resourceCode = permission.resourceCode || '';
  permissionForm.actionCode = permission.actionCode;
  permissionForm.description = permission.description || '';
}

function resetDictForm() {
  dictForm.dictCode = '';
  dictForm.dictName = '';
  dictForm.itemCode = '';
  dictForm.itemName = '';
  dictForm.itemValue = '';
  dictForm.sortNo = 0;
  dictForm.remark = '';
}

function fillDictForm(item: SysDictItem) {
  dictForm.dictCode = item.dictCode;
  dictForm.dictName = item.dictName;
  dictForm.itemCode = item.itemCode;
  dictForm.itemName = item.itemName;
  dictForm.itemValue = item.itemValue;
  dictForm.sortNo = item.sortNo || 0;
  dictForm.remark = item.remark || '';
}
</script>

<template>
  <section class="basic-config-page">
    <TimedToast
      :show="notice.show"
      :type="notice.type"
      :message="notice.message"
      @close="closeNotice"
    />

    <header class="page-hero">
      <div>
        <span>{{ pageMeta.eyebrow }}</span>
        <h1>{{ pageMeta.title }}</h1>
        <p>{{ pageMeta.description }}</p>
      </div>
      <button
        v-if="canCreateCurrent"
        type="button"
        class="primary-action"
        :disabled="loading"
        @click="openCreateDialog"
      >
        {{ pageMeta.actionText }}
      </button>
    </header>

    <section class="list-panel">
      <header class="list-toolbar">
        <div>
          <h2>{{ pageMeta.title }}列表</h2>
          <span>共 {{ filteredRows.length }} 条，每页 {{ PAGE_SIZE }} 条</span>
        </div>
        <label>
          <span>检索</span>
          <input v-model="keyword" type="search" placeholder="按名称、编码或账号模糊查询" />
        </label>
      </header>

      <div class="table-wrap">
        <table>
          <thead>
            <tr v-if="section === 'users'">
              <th>账号</th>
              <th>姓名</th>
              <th>类型</th>
              <th>来源</th>
              <th>联系方式</th>
              <th>状态</th>
              <th v-if="canOperateUser">操作</th>
            </tr>
            <tr v-else-if="section === 'roles'">
              <th>角色编码</th>
              <th>角色名称</th>
              <th>说明</th>
              <th>状态</th>
              <th v-if="canOperateRole">操作</th>
            </tr>
            <tr v-else-if="section === 'orgs'">
              <th>单位编码</th>
              <th>单位名称</th>
              <th>单位类型</th>
              <th>上级单位</th>
              <th>状态</th>
              <th v-if="canOperateOrg">操作</th>
            </tr>
            <tr v-else-if="section === 'userRoles'">
              <th>用户</th>
              <th>角色</th>
              <th>授权来源</th>
              <th>状态</th>
              <th v-if="canOperateUserRole">操作</th>
            </tr>
            <tr v-else-if="section === 'userOrgs'">
              <th>用户</th>
              <th>单位</th>
              <th>关系类型</th>
              <th>状态</th>
              <th v-if="canOperateUserOrg">操作</th>
            </tr>
            <tr v-else-if="section === 'menus'">
              <th>菜单编码</th>
              <th>菜单名称</th>
              <th>类型</th>
              <th>路由</th>
              <th>权限编码</th>
              <th>排序</th>
              <th>状态</th>
              <th v-if="canOperateMenu">操作</th>
            </tr>
            <tr v-else-if="section === 'permissions'">
              <th>权限编码</th>
              <th>权限名称</th>
              <th>资源类型</th>
              <th>资源编码</th>
              <th>动作</th>
              <th>状态</th>
              <th v-if="canOperatePermission">操作</th>
            </tr>
            <tr v-else-if="section === 'rolePermissions'">
              <th>角色</th>
              <th>权限</th>
              <th>授权来源</th>
              <th>状态</th>
              <th v-if="canOperateRolePermission">操作</th>
            </tr>
            <tr v-else>
              <th>字典编码</th>
              <th>字典名称</th>
              <th>字典项编码</th>
              <th>字典项名称</th>
              <th>字典项值</th>
              <th>排序</th>
              <th>状态</th>
              <th v-if="canOperateDictItem">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="pagedRows.length === 0">
              <td :colspan="emptyColspan" class="empty-cell">
                {{ pageMeta.emptyText }}
              </td>
            </tr>
            <tr v-for="row in pagedRows" :key="row.id">
              <template v-if="section === 'users'">
                <td>{{ (row as TeachUser).username }}</td>
                <td>{{ (row as TeachUser).realName }}</td>
                <td>{{ (row as TeachUser).userType }}</td>
                <td>{{ (row as TeachUser).sourceType }}</td>
                <td>{{ (row as TeachUser).phone || (row as TeachUser).email || '-' }}</td>
                <td><span class="status-pill">{{ statusText(row.status) }}</span></td>
                <td v-if="canOperateUser" class="table-actions">
                  <button
                    v-if="hasRuntimePermission(userActionPermission.edit)"
                    type="button"
                    class="text-action"
                    :disabled="loading"
                    @click="openEditUserDialog(row as TeachUser)"
                  >
                    编辑
                  </button>
                  <button
                    v-if="hasRuntimePermission(userActionPermission.resetPassword)"
                    type="button"
                    class="text-action"
                    :disabled="loading"
                    @click="resetUserPassword(row as TeachUser)"
                  >
                    重置密码
                  </button>
                  <button
                    v-if="
                      hasRuntimePermission(
                        (row as TeachUser).status === 'ACTIVE'
                          ? userActionPermission.disable
                          : userActionPermission.enable
                      )
                    "
                    type="button"
                    class="text-action"
                    :class="{ danger: (row as TeachUser).status === 'ACTIVE' }"
                    :disabled="loading"
                    @click="toggleUserStatus(row as TeachUser)"
                  >
                    {{ (row as TeachUser).status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                </td>
              </template>
              <template v-else-if="section === 'roles'">
                <td>{{ (row as TeachRole).roleCode }}</td>
                <td>{{ (row as TeachRole).roleName }}</td>
                <td>{{ (row as TeachRole).description || '-' }}</td>
                <td><span class="status-pill">{{ statusText(row.status) }}</span></td>
                <td v-if="canOperateRole" class="table-actions">
                  <button
                    v-if="hasRuntimePermission(roleActionPermission.edit)"
                    type="button"
                    class="text-action"
                    :disabled="loading"
                    @click="openEditRoleDialog(row as TeachRole)"
                  >
                    编辑
                  </button>
                  <button
                    v-if="
                      hasRuntimePermission(
                        (row as TeachRole).status === 'ACTIVE'
                          ? roleActionPermission.disable
                          : roleActionPermission.enable
                      )
                    "
                    type="button"
                    class="text-action"
                    :class="{ danger: (row as TeachRole).status === 'ACTIVE' }"
                    :disabled="loading"
                    @click="toggleRoleStatus(row as TeachRole)"
                  >
                    {{ (row as TeachRole).status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                </td>
              </template>
              <template v-else-if="section === 'orgs'">
                <td>{{ (row as TeachOrg).orgCode }}</td>
                <td>{{ (row as TeachOrg).orgName }}</td>
                <td>{{ (row as TeachOrg).orgType }}</td>
                <td>{{ (row as TeachOrg).parentId ? orgName((row as TeachOrg).parentId!) : '-' }}</td>
                <td><span class="status-pill">{{ statusText(row.status) }}</span></td>
                <td v-if="canOperateOrg" class="table-actions">
                  <button
                    v-if="hasRuntimePermission(orgActionPermission.edit)"
                    type="button"
                    class="text-action"
                    :disabled="loading"
                    @click="openEditOrgDialog(row as TeachOrg)"
                  >
                    编辑
                  </button>
                  <button
                    v-if="
                      hasRuntimePermission(
                        (row as TeachOrg).status === 'ACTIVE'
                          ? orgActionPermission.disable
                          : orgActionPermission.enable
                      )
                    "
                    type="button"
                    class="text-action"
                    :class="{ danger: (row as TeachOrg).status === 'ACTIVE' }"
                    :disabled="loading"
                    @click="toggleOrgStatus(row as TeachOrg)"
                  >
                    {{ (row as TeachOrg).status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                </td>
              </template>
              <template v-else-if="section === 'userRoles'">
                <td>{{ userName((row as TeachUserRole).userId) }}</td>
                <td>{{ roleName((row as TeachUserRole).roleId) }}</td>
                <td>{{ (row as TeachUserRole).grantSource || '-' }}</td>
                <td><span class="status-pill">{{ statusText(row.status) }}</span></td>
                <td v-if="canOperateUserRole" class="table-actions">
                  <button
                    v-if="
                      hasRuntimePermission(
                        (row as TeachUserRole).status === 'ACTIVE'
                          ? userRoleActionPermission.disable
                          : userRoleActionPermission.enable
                      )
                    "
                    type="button"
                    class="text-action"
                    :class="{ danger: (row as TeachUserRole).status === 'ACTIVE' }"
                    :disabled="loading"
                    @click="toggleUserRoleStatus(row as TeachUserRole)"
                  >
                    {{ (row as TeachUserRole).status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                </td>
              </template>
              <template v-else-if="section === 'userOrgs'">
                <td>{{ userName((row as TeachUserOrg).userId) }}</td>
                <td>{{ orgName((row as TeachUserOrg).orgId) }}</td>
                <td>{{ (row as TeachUserOrg).relationType }}</td>
                <td><span class="status-pill">{{ statusText(row.status) }}</span></td>
                <td v-if="canOperateUserOrg" class="table-actions">
                  <button
                    v-if="
                      hasRuntimePermission(
                        (row as TeachUserOrg).status === 'ACTIVE'
                          ? userOrgActionPermission.disable
                          : userOrgActionPermission.enable
                      )
                    "
                    type="button"
                    class="text-action"
                    :class="{ danger: (row as TeachUserOrg).status === 'ACTIVE' }"
                    :disabled="loading"
                    @click="toggleUserOrgStatus(row as TeachUserOrg)"
                  >
                    {{ (row as TeachUserOrg).status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                  <button
                    v-if="canRemoveCurrent"
                    type="button"
                    class="text-action danger"
                    :disabled="loading"
                    @click="removeUserOrg(row as TeachUserOrg)"
                  >
                    移除
                  </button>
                </td>
              </template>
              <template v-else-if="section === 'menus'">
                <td>{{ (row as SysMenuConfig).menuCode }}</td>
                <td>{{ (row as SysMenuConfig).menuName }}</td>
                <td>{{ (row as SysMenuConfig).menuType }}</td>
                <td>{{ (row as SysMenuConfig).routePath || '-' }}</td>
                <td>{{ (row as SysMenuConfig).permissionCode || '-' }}</td>
                <td>{{ (row as SysMenuConfig).sortNo }}</td>
                <td><span class="status-pill">{{ statusText(row.status) }}</span></td>
                <td v-if="canOperateMenu" class="table-actions">
                  <button
                    v-if="hasRuntimePermission(menuActionPermission.view)"
                    type="button"
                    class="text-action"
                    :disabled="loading"
                    @click="viewMenuDetail(row as SysMenuConfig)"
                  >
                    详情
                  </button>
                  <button
                    v-if="hasRuntimePermission(menuActionPermission.edit)"
                    type="button"
                    class="text-action"
                    :disabled="loading"
                    @click="openEditMenuDialog(row as SysMenuConfig)"
                  >
                    编辑
                  </button>
                  <button
                    v-if="
                      hasRuntimePermission(
                        (row as SysMenuConfig).status === 'ACTIVE'
                          ? menuActionPermission.disable
                          : menuActionPermission.enable
                      )
                    "
                    type="button"
                    class="text-action"
                    :class="{ danger: (row as SysMenuConfig).status === 'ACTIVE' }"
                    :disabled="loading"
                    @click="toggleMenuStatus(row as SysMenuConfig)"
                  >
                    {{ (row as SysMenuConfig).status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                </td>
              </template>
              <template v-else-if="section === 'permissions'">
                <td>{{ (row as SysPermissionConfig).permissionCode }}</td>
                <td>{{ (row as SysPermissionConfig).permissionName }}</td>
                <td>{{ (row as SysPermissionConfig).resourceType }}</td>
                <td>{{ (row as SysPermissionConfig).resourceCode || '-' }}</td>
                <td>{{ (row as SysPermissionConfig).actionCode }}</td>
                <td><span class="status-pill">{{ statusText(row.status) }}</span></td>
                <td v-if="canOperatePermission" class="table-actions">
                  <button
                    v-if="hasRuntimePermission(permissionActionPermission.view)"
                    type="button"
                    class="text-action"
                    :disabled="loading"
                    @click="viewPermissionDetail(row as SysPermissionConfig)"
                  >
                    详情
                  </button>
                  <button
                    v-if="hasRuntimePermission(permissionActionPermission.edit)"
                    type="button"
                    class="text-action"
                    :disabled="loading"
                    @click="openEditPermissionDialog(row as SysPermissionConfig)"
                  >
                    编辑
                  </button>
                  <button
                    v-if="
                      hasRuntimePermission(
                        (row as SysPermissionConfig).status === 'ACTIVE'
                          ? permissionActionPermission.disable
                          : permissionActionPermission.enable
                      )
                    "
                    type="button"
                    class="text-action"
                    :class="{ danger: (row as SysPermissionConfig).status === 'ACTIVE' }"
                    :disabled="loading"
                    @click="togglePermissionStatus(row as SysPermissionConfig)"
                  >
                    {{ (row as SysPermissionConfig).status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                </td>
              </template>
              <template v-else-if="section === 'rolePermissions'">
                <td>{{ roleName((row as SysRolePermission).roleId) }}</td>
                <td>{{ permissionName((row as SysRolePermission).permissionId) }}</td>
                <td>{{ (row as SysRolePermission).grantSource || '-' }}</td>
                <td><span class="status-pill">{{ statusText(row.status) }}</span></td>
                <td v-if="canOperateRolePermission" class="table-actions">
                  <button
                    v-if="hasRuntimePermission(rolePermissionActionPermission.view)"
                    type="button"
                    class="text-action"
                    :disabled="loading"
                    @click="viewRolePermissionDetail(row as SysRolePermission)"
                  >
                    详情
                  </button>
                  <button
                    v-if="
                      hasRuntimePermission(
                        (row as SysRolePermission).status === 'ACTIVE'
                          ? rolePermissionActionPermission.disable
                          : rolePermissionActionPermission.enable
                      )
                    "
                    type="button"
                    class="text-action"
                    :class="{ danger: (row as SysRolePermission).status === 'ACTIVE' }"
                    :disabled="loading"
                    @click="toggleRolePermissionStatus(row as SysRolePermission)"
                  >
                    {{ (row as SysRolePermission).status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                </td>
              </template>
              <template v-else>
                <td>{{ (row as SysDictItem).dictCode }}</td>
                <td>{{ (row as SysDictItem).dictName }}</td>
                <td>{{ (row as SysDictItem).itemCode }}</td>
                <td>{{ (row as SysDictItem).itemName }}</td>
                <td>{{ (row as SysDictItem).itemValue }}</td>
                <td>{{ (row as SysDictItem).sortNo }}</td>
                <td><span class="status-pill">{{ statusText(row.status) }}</span></td>
                <td v-if="canOperateDictItem" class="table-actions">
                  <button
                    v-if="hasRuntimePermission(dictActionPermission.view)"
                    type="button"
                    class="text-action"
                    :disabled="loading"
                    @click="viewDictItemDetail(row as SysDictItem)"
                  >
                    详情
                  </button>
                  <button
                    v-if="hasRuntimePermission(dictActionPermission.edit)"
                    type="button"
                    class="text-action"
                    :disabled="loading"
                    @click="openEditDictItemDialog(row as SysDictItem)"
                  >
                    编辑
                  </button>
                  <button
                    v-if="
                      hasRuntimePermission(
                        (row as SysDictItem).status === 'ACTIVE'
                          ? dictActionPermission.disable
                          : dictActionPermission.enable
                      )
                    "
                    type="button"
                    class="text-action"
                    :class="{ danger: (row as SysDictItem).status === 'ACTIVE' }"
                    :disabled="loading"
                    @click="toggleDictItemStatus(row as SysDictItem)"
                  >
                    {{ (row as SysDictItem).status === 'ACTIVE' ? '停用' : '启用' }}
                  </button>
                </td>
              </template>
            </tr>
          </tbody>
        </table>
      </div>

      <footer class="pager">
        <button type="button" :disabled="currentPage <= 1" @click="previousPage">上一页</button>
        <span>第 {{ currentPage }} / {{ totalPages }} 页</span>
        <button type="button" :disabled="currentPage >= totalPages" @click="nextPage">下一页</button>
      </footer>
    </section>

    <div v-if="dialogOpen" class="dialog-backdrop" @click.self="closeDialog">
      <section class="dialog-panel" role="dialog" aria-modal="true">
        <header class="dialog-header">
          <div>
            <span>{{ pageMeta.eyebrow }}</span>
            <h2>{{ dialogTitle }}</h2>
          </div>
          <button type="button" class="close-button" aria-label="关闭弹窗" @click="closeDialog">×</button>
        </header>

        <form class="config-form" @submit.prevent="submitForm">
          <template v-if="section === 'users'">
            <label>
              <span>登录账号</span>
              <input v-model="userForm.username" type="text" :disabled="Boolean(editingUserId)" />
            </label>
            <label>
              <span>用户姓名</span>
              <input v-model="userForm.realName" type="text" />
            </label>
            <label>
              <span>用户类型</span>
              <select v-model="userForm.userType">
                <option value="ADMIN">管理员</option>
                <option value="TEACHER">教师</option>
                <option value="STUDENT">学生</option>
              </select>
            </label>
            <label>
              <span>来源类型</span>
              <select v-model="userForm.sourceType">
                <option value="LOCAL">本地维护</option>
                <option value="IMPORT">批量导入</option>
                <option value="SYNC">外部同步</option>
              </select>
            </label>
            <label v-if="!editingUserId">
              <span>初始密码</span>
              <input v-model="userForm.initialPassword" type="password" />
            </label>
            <label>
              <span>学号</span>
              <input v-model="userForm.studentNo" type="text" />
            </label>
            <label>
              <span>工号</span>
              <input v-model="userForm.employeeNo" type="text" />
            </label>
            <label>
              <span>手机号</span>
              <input v-model="userForm.phone" type="text" />
            </label>
            <label>
              <span>邮箱</span>
              <input v-model="userForm.email" type="email" />
            </label>
          </template>

          <template v-else-if="section === 'roles'">
            <label>
              <span>角色编码</span>
              <input v-model="roleForm.roleCode" type="text" :disabled="Boolean(editingRoleId)" />
            </label>
            <label>
              <span>角色名称</span>
              <input v-model="roleForm.roleName" type="text" />
            </label>
            <label class="field-wide">
              <span>角色说明</span>
              <textarea v-model="roleForm.description" rows="4"></textarea>
            </label>
          </template>

          <template v-else-if="section === 'orgs'">
            <label>
              <span>单位编码</span>
              <input v-model="orgForm.orgCode" type="text" :disabled="Boolean(editingOrgId)" />
            </label>
            <label>
              <span>单位名称</span>
              <input v-model="orgForm.orgName" type="text" />
            </label>
            <label>
              <span>单位类型</span>
              <select v-model="orgForm.orgType">
                <option value="SCHOOL">学校</option>
                <option value="DEPARTMENT">院系</option>
                <option value="CLASS">班级</option>
                <option value="TEAM">小组</option>
              </select>
            </label>
            <label>
              <span>上级单位</span>
              <select v-model="orgForm.parentId">
                <option value="">无</option>
                <option v-for="org in orgs" :key="org.id" :value="org.id">{{ org.orgName }}</option>
              </select>
            </label>
          </template>

          <template v-else-if="section === 'userRoles'">
            <label>
              <span>用户</span>
              <select v-model="userRoleForm.userId">
                <option value="">请选择用户</option>
                <option v-for="user in users" :key="user.id" :value="user.id">{{ user.realName }}（{{ user.username }}）</option>
              </select>
            </label>
            <label>
              <span>角色</span>
              <select v-model="userRoleForm.roleId">
                <option value="">请选择角色</option>
                <option v-for="role in roles" :key="role.id" :value="role.id">{{ role.roleName }}（{{ role.roleCode }}）</option>
              </select>
            </label>
            <label>
              <span>授权来源</span>
              <input v-model="userRoleForm.grantSource" type="text" />
            </label>
          </template>

          <template v-else-if="section === 'userOrgs'">
            <label>
              <span>用户</span>
              <select v-model="userOrgForm.userId">
                <option value="">请选择用户</option>
                <option v-for="user in users" :key="user.id" :value="user.id">{{ user.realName }}（{{ user.username }}）</option>
              </select>
            </label>
            <label>
              <span>单位</span>
              <select v-model="userOrgForm.orgId">
                <option value="">请选择单位</option>
                <option v-for="org in orgs" :key="org.id" :value="org.id">{{ org.orgName }}（{{ org.orgCode }}）</option>
              </select>
            </label>
            <label>
              <span>关系类型</span>
              <select v-model="userOrgForm.relationType">
                <option value="BELONG">归属</option>
                <option value="MANAGE">管理</option>
                <option value="TEACH">任教</option>
              </select>
            </label>
          </template>

          <template v-else-if="section === 'menus'">
            <label>
              <span>菜单编码</span>
              <input v-model="menuForm.menuCode" type="text" />
            </label>
            <label>
              <span>菜单名称</span>
              <input v-model="menuForm.menuName" type="text" />
            </label>
            <label>
              <span>菜单类型</span>
              <select v-model="menuForm.menuType">
                <option value="DIR">目录</option>
                <option value="MENU">菜单</option>
                <option value="BUTTON">按钮</option>
              </select>
            </label>
            <label>
              <span>上级菜单</span>
              <select v-model="menuForm.parentId">
                <option value="">无</option>
                <option v-for="menu in menus" :key="menu.id" :value="menu.id">{{ menu.menuName }}</option>
              </select>
            </label>
            <label>
              <span>路由地址</span>
              <input v-model="menuForm.routePath" type="text" />
            </label>
            <label>
              <span>组件路径</span>
              <input v-model="menuForm.componentPath" type="text" />
            </label>
            <label>
              <span>权限编码</span>
              <input v-model="menuForm.permissionCode" type="text" />
            </label>
            <label>
              <span>图标</span>
              <input v-model="menuForm.icon" type="text" />
            </label>
            <label>
              <span>排序号</span>
              <input v-model.number="menuForm.sortNo" type="number" min="0" />
            </label>
            <label class="inline-field">
              <input v-model="menuForm.visible" type="checkbox" />
              <span>导航可见</span>
            </label>
          </template>

          <template v-else-if="section === 'permissions'">
            <label>
              <span>权限编码</span>
              <input v-model="permissionForm.permissionCode" type="text" />
            </label>
            <label>
              <span>权限名称</span>
              <input v-model="permissionForm.permissionName" type="text" />
            </label>
            <label>
              <span>资源类型</span>
              <select v-model="permissionForm.resourceType">
                <option value="MENU">菜单</option>
                <option value="BUTTON">按钮</option>
                <option value="API">接口</option>
                <option value="DATA">数据范围</option>
              </select>
            </label>
            <label>
              <span>资源编码</span>
              <input v-model="permissionForm.resourceCode" type="text" />
            </label>
            <label>
              <span>动作</span>
              <select v-model="permissionForm.actionCode">
                <option value="VIEW">查看</option>
                <option value="CREATE">新增</option>
                <option value="EDIT">编辑</option>
                <option value="DISABLE">启停用</option>
                <option value="EXPORT">导出</option>
              </select>
            </label>
            <label class="field-wide">
              <span>权限说明</span>
              <textarea v-model="permissionForm.description" rows="4"></textarea>
            </label>
          </template>

          <template v-else-if="section === 'rolePermissions'">
            <label>
              <span>角色</span>
              <select v-model="rolePermissionForm.roleId">
                <option value="">请选择角色</option>
                <option v-for="role in roles" :key="role.id" :value="role.id">{{ role.roleName }}（{{ role.roleCode }}）</option>
              </select>
            </label>
            <label>
              <span>权限</span>
              <select v-model="rolePermissionForm.permissionId">
                <option value="">请选择权限</option>
                <option v-for="permission in permissions" :key="permission.id" :value="permission.id">
                  {{ permission.permissionName }}（{{ permission.permissionCode }}）
                </option>
              </select>
            </label>
            <label>
              <span>授权来源</span>
              <input v-model="rolePermissionForm.grantSource" type="text" />
            </label>
          </template>

          <template v-else>
            <label>
              <span>字典编码</span>
              <input v-model="dictForm.dictCode" type="text" />
            </label>
            <label>
              <span>字典名称</span>
              <input v-model="dictForm.dictName" type="text" />
            </label>
            <label>
              <span>字典项编码</span>
              <input v-model="dictForm.itemCode" type="text" />
            </label>
            <label>
              <span>字典项名称</span>
              <input v-model="dictForm.itemName" type="text" />
            </label>
            <label>
              <span>字典项值</span>
              <input v-model="dictForm.itemValue" type="text" />
            </label>
            <label>
              <span>排序号</span>
              <input v-model.number="dictForm.sortNo" type="number" min="0" />
            </label>
            <label class="field-wide">
              <span>备注</span>
              <textarea v-model="dictForm.remark" rows="4"></textarea>
            </label>
          </template>

          <footer class="dialog-actions">
            <button type="button" class="secondary-action" @click="closeDialog">取消</button>
            <button type="submit" class="primary-action" :disabled="loading">提交保存</button>
          </footer>
        </form>
      </section>
    </div>
  </section>
</template>

<style scoped>
.basic-config-page {
  display: grid;
  gap: 18px;
}

.page-hero,
.list-panel {
  border: 1px solid #e4eaf3;
  border-radius: 8px;
  background: #ffffff;
}

.page-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 24px 28px;
}

.page-hero span,
.dialog-header span,
.list-toolbar span {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.page-hero h1,
.dialog-header h2,
.list-toolbar h2 {
  margin: 6px 0;
  color: #172033;
  letter-spacing: 0;
}

.page-hero h1 {
  font-size: 26px;
}

.page-hero p {
  max-width: 780px;
  margin: 0;
  color: #708199;
  line-height: 1.7;
}

.primary-action,
.secondary-action,
.pager button,
.text-action {
  min-height: 38px;
  border-radius: 7px;
  border: 1px solid #d8e2f0;
  padding: 0 16px;
  font-weight: 800;
  cursor: pointer;
}

.primary-action {
  border-color: #2f6df6;
  background: #2f6df6;
  color: #ffffff;
}

.secondary-action,
.pager button,
.text-action {
  background: #ffffff;
  color: #2f6df6;
}

.primary-action:disabled,
.secondary-action:disabled,
.pager button:disabled,
.text-action:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.list-panel {
  overflow: hidden;
}

.list-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 18px 20px;
  border-bottom: 1px solid #e8edf5;
}

.list-toolbar label {
  display: grid;
  gap: 6px;
  min-width: min(360px, 42vw);
}

.list-toolbar input,
.config-form input,
.config-form select,
.config-form textarea {
  width: 100%;
  border: 1px solid #d8e2f0;
  border-radius: 7px;
  background: #ffffff;
  color: #172033;
  font: inherit;
  outline: none;
}

.list-toolbar input,
.config-form input,
.config-form select {
  height: 40px;
  padding: 0 12px;
}

.config-form textarea {
  resize: vertical;
  padding: 10px 12px;
}

.list-toolbar input:focus,
.config-form input:focus,
.config-form select:focus,
.config-form textarea:focus {
  border-color: #2f6df6;
  box-shadow: 0 0 0 3px rgba(47, 109, 246, 0.12);
}

.table-wrap {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  min-width: 820px;
}

th,
td {
  border-bottom: 1px solid #e8edf5;
  padding: 14px 18px;
  color: #334155;
  text-align: left;
  white-space: nowrap;
}

th {
  background: #f8fafc;
  color: #52637a;
  font-size: 13px;
}

.empty-cell {
  height: 160px;
  color: #94a3b8;
  font-weight: 800;
  text-align: center;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  border-radius: 999px;
  background: #eafaf1;
  color: #07875c;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 850;
}

.text-action.danger {
  border-color: #fecaca;
  color: #c2410c;
}

.table-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.pager {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 14px 18px;
}

.pager span {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.dialog-backdrop {
  position: fixed;
  inset: 0;
  z-index: 4000;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.32);
}

.dialog-panel {
  width: min(720px, calc(100vw - 36px));
  max-height: calc(100vh - 48px);
  overflow: auto;
  border-radius: 8px;
  background: #ffffff;
  box-shadow: 0 24px 70px rgba(15, 23, 42, 0.22);
}

.dialog-header,
.dialog-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 22px;
}

.dialog-header {
  border-bottom: 1px solid #e8edf5;
}

.dialog-actions {
  grid-column: 1 / -1;
  justify-content: flex-end;
  border-top: 1px solid #e8edf5;
  padding-bottom: 0;
}

.close-button {
  display: inline-grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid #d8e2f0;
  border-radius: 7px;
  background: #ffffff;
  color: #64748b;
  cursor: pointer;
  font-size: 20px;
  line-height: 1;
}

.close-button:hover,
.close-button:focus-visible {
  border-color: #2f6df6;
  color: #2f6df6;
  outline: none;
}

.config-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  padding: 20px 22px 22px;
}

.config-form label {
  display: grid;
  gap: 7px;
  color: #52637a;
  font-size: 13px;
  font-weight: 800;
}

.field-wide {
  grid-column: 1 / -1;
}

.inline-field {
  align-content: end;
  grid-template-columns: auto 1fr;
  align-items: center;
}

.config-form .inline-field input[type='checkbox'] {
  width: 18px;
  height: 18px;
  padding: 0;
  accent-color: #2f6df6;
}

@media (max-width: 760px) {
  .page-hero,
  .list-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .config-form {
    grid-template-columns: 1fr;
  }

  .list-toolbar label {
    min-width: 0;
  }
}
</style>
