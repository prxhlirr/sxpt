-- 系统菜单与角色权限最小闭环配置。
-- 业务边界：
-- 1. 菜单来源对齐前端 AppShell 与 router 中已经存在的页面入口。
-- 2. 动态教案路由统一落到 /admin/lessons 稳定入口，避免侧边栏生成不可用的 :lessonId 链接。
-- 3. 管理员授予全部菜单权限，教师授予教案管理和教学运行菜单，学生授予考试任务与成绩反馈菜单。

SET client_encoding = 'UTF8';

BEGIN;

INSERT INTO teach_role (
    id, tenant_id, role_code, role_name, description,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('role-demo-admin', 'demo-tenant', 'admin', '管理员', '查看全部菜单并维护平台基础配置、教案、数据准备和发布运行。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('role-demo-teacher', 'demo-tenant', 'teacher', '教师', '查看教案管理、教学运行和评阅相关菜单。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('role-demo-student', 'demo-tenant', 'student', '学生', '查看考试任务、任务办理和成绩反馈相关菜单。', 'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, role_code) WHERE deleted = false
DO UPDATE SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO teach_role (
    id, tenant_id, role_code, role_name, description,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES (
    'role-demo-expert',
    'demo-tenant',
    'expert',
    '专家',
    '查看教案管理、数据准备和经典案例菜单。',
    'seed',
    now(),
    'seed',
    now(),
    'ACTIVE',
    false
)
ON CONFLICT (tenant_id, role_code) WHERE deleted = false
DO UPDATE SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO teach_user (
    id, tenant_id, username, real_name, phone, email, user_type, source_type,
    external_info_json, last_sync_time,
    password_hash, password_salt, password_algorithm, password_iterations,
    password_status, password_updated_time, failed_login_count, locked_until,
    student_no, employee_no,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES (
    'user-demo-super-admin',
    'demo-tenant',
    'superadmin',
    '超级管理员',
    '13800000000',
    'superadmin@example.com',
    'ADMIN',
    'LOCAL',
    '{"seed":"system-menu-permission","role":"admin","initialPassword":"Sxpt@123456"}'::jsonb,
    now(),
    'KwqqBr4ujZtPLEl805NTj3UgNJfZMNWjF2bl/hOE/58=',
    'c3hwdC1zdXBlci1hZG0=',
    'PBKDF2WithHmacSHA256',
    120000,
    'NORMAL',
    now(),
    0,
    null,
    null,
    'A2026000',
    'seed',
    now(),
    'seed',
    now(),
    'ACTIVE',
    false
)
ON CONFLICT (tenant_id, username) WHERE deleted = false
DO UPDATE SET
    real_name = EXCLUDED.real_name,
    phone = EXCLUDED.phone,
    email = EXCLUDED.email,
    user_type = EXCLUDED.user_type,
    source_type = EXCLUDED.source_type,
    external_info_json = EXCLUDED.external_info_json,
    last_sync_time = EXCLUDED.last_sync_time,
    password_hash = EXCLUDED.password_hash,
    password_salt = EXCLUDED.password_salt,
    password_algorithm = EXCLUDED.password_algorithm,
    password_iterations = EXCLUDED.password_iterations,
    password_status = EXCLUDED.password_status,
    password_updated_time = EXCLUDED.password_updated_time,
    failed_login_count = 0,
    locked_until = null,
    student_no = EXCLUDED.student_no,
    employee_no = EXCLUDED.employee_no,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO teach_user_role (
    id, tenant_id, user_id, role_id, grant_source,
    create_by, create_time, update_by, update_time, status, deleted
)
SELECT
    'ur-demo-super-admin',
    role.tenant_id,
    'user-demo-super-admin',
    role.id,
    'SEED',
    'seed',
    now(),
    'seed',
    now(),
    'ACTIVE',
    false
FROM teach_role role
WHERE role.tenant_id = 'demo-tenant'
  AND role.role_code = 'admin'
  AND role.deleted = false
ON CONFLICT (tenant_id, user_id, role_id) WHERE deleted = false
DO UPDATE SET
    grant_source = EXCLUDED.grant_source,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO teach_user_role (
    id, tenant_id, user_id, role_id, grant_source,
    create_by, create_time, update_by, update_time, status, deleted
)
SELECT
    'ur-demo-teacher-02-admin',
    role.tenant_id,
    user_account.id,
    role.id,
    'SEED',
    'seed',
    now(),
    'seed',
    now(),
    'ACTIVE',
    false
FROM teach_role role
JOIN teach_user user_account
  ON user_account.tenant_id = role.tenant_id
 AND user_account.username = 'teacher02'
 AND user_account.deleted = false
WHERE role.tenant_id = 'demo-tenant'
  AND role.role_code = 'admin'
  AND role.deleted = false
ON CONFLICT (tenant_id, user_id, role_id) WHERE deleted = false
DO UPDATE SET
    grant_source = EXCLUDED.grant_source,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

-- 教案内部流程由“教案管理”的操作入口进入，不再作为一级侧栏菜单重复展示。
UPDATE sys_role_permission role_permission
SET status = 'DISABLED',
    deleted = true,
    update_by = 'seed',
    update_time = now()
FROM sys_permission_config permission
WHERE role_permission.tenant_id = 'demo-tenant'
  AND role_permission.deleted = false
  AND permission.id = role_permission.permission_id
  AND permission.tenant_id = role_permission.tenant_id
  AND permission.resource_code IN (
      'lesson-editor',
      'exam-setup',
      'group-setup',
      'exam-data',
      'publish-center'
  );

UPDATE sys_permission_config
SET status = 'DISABLED',
    deleted = true,
    update_by = 'seed',
    update_time = now()
WHERE tenant_id = 'demo-tenant'
  AND deleted = false
  AND resource_code IN (
      'lesson-editor',
      'exam-setup',
      'group-setup',
      'exam-data',
      'publish-center'
  );

UPDATE sys_menu_config
SET visible = false,
    status = 'DISABLED',
    deleted = true,
    update_by = 'seed',
    update_time = now()
WHERE tenant_id = 'demo-tenant'
  AND deleted = false
  AND menu_code IN (
      'lesson-editor',
      'exam-setup',
      'group-setup',
      'exam-data',
      'publish-center'
  );

INSERT INTO sys_menu_config (
    id, tenant_id, parent_id, menu_code, menu_name, menu_type,
    route_path, component_path, permission_code, icon, sort_no, visible,
    create_by, create_time, update_by, update_time, status, deleted
)
SELECT *
FROM (
    VALUES
        ('menu-admin-overview', 'demo-tenant', null, 'admin-overview', '运营总览', 'MENU', '/admin/overview', 'views/admin/AdminOverviewView.vue', 'menu:admin-overview:view', '01', 10, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-basic-users', 'demo-tenant', null, 'basic-users', '用户管理', 'MENU', '/admin/basic/users', 'views/admin/BasicConfigView.vue#users', 'menu:basic-users:view', '02', 20, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-basic-roles', 'demo-tenant', null, 'basic-roles', '角色管理', 'MENU', '/admin/basic/roles', 'views/admin/BasicConfigView.vue#roles', 'menu:basic-roles:view', '03', 30, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-basic-orgs', 'demo-tenant', null, 'basic-orgs', '单位管理', 'MENU', '/admin/basic/orgs', 'views/admin/BasicConfigView.vue#orgs', 'menu:basic-orgs:view', '04', 40, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-basic-user-roles', 'demo-tenant', null, 'basic-user-roles', '用户角色', 'MENU', '/admin/basic/user-roles', 'views/admin/BasicConfigView.vue#userRoles', 'menu:basic-user-roles:view', '05', 50, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-basic-user-orgs', 'demo-tenant', null, 'basic-user-orgs', '用户单位', 'MENU', '/admin/basic/user-orgs', 'views/admin/BasicConfigView.vue#userOrgs', 'menu:basic-user-orgs:view', '06', 60, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-basic-menus', 'demo-tenant', null, 'basic-menus', '菜单管理', 'MENU', '/admin/basic/menus', 'views/admin/BasicConfigView.vue#menus', 'menu:basic-menus:view', '07', 70, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-basic-permissions', 'demo-tenant', null, 'basic-permissions', '权限管理', 'MENU', '/admin/basic/permissions', 'views/admin/BasicConfigView.vue#permissions', 'menu:basic-permissions:view', '08', 80, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-basic-role-permissions', 'demo-tenant', null, 'basic-role-permissions', '角色权限', 'MENU', '/admin/basic/role-permissions', 'views/admin/BasicConfigView.vue#rolePermissions', 'menu:basic-role-permissions:view', '09', 90, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-basic-dict-items', 'demo-tenant', null, 'basic-dict-items', '字典配置', 'MENU', '/admin/basic/dict-items', 'views/admin/BasicConfigView.vue#dictItems', 'menu:basic-dict-items:view', '10', 100, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-lesson-list', 'demo-tenant', null, 'lesson-list', '教案管理', 'MENU', '/admin/lessons', 'views/admin/LessonListView.vue', 'menu:lesson-list:view', '11', 110, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-data-prepare-systems', 'demo-tenant', null, 'data-prepare-systems', '平台接入', 'MENU', '/admin/data-prepare/systems', 'views/admin/DataPrepareSystemsView.vue', 'menu:data-prepare-systems:view', '16', 160, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-data-prepare-modules', 'demo-tenant', null, 'data-prepare-modules', '业务模块', 'MENU', '/admin/data-prepare/modules', 'views/admin/DataPrepareModulesView.vue', 'menu:data-prepare-modules:view', '17', 170, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-data-prepare-templates', 'demo-tenant', null, 'data-prepare-templates', '模板管理', 'MENU', '/admin/data-prepare/templates', 'views/admin/DataPrepareTemplatesView.vue', 'menu:data-prepare-templates:view', '18', 180, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-data-prepare-strategies', 'demo-tenant', null, 'data-prepare-strategies', '策略管理', 'MENU', '/admin/data-prepare/strategies', 'views/admin/DataPrepareStrategiesView.vue', 'menu:data-prepare-strategies:view', '19', 190, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-data-prepare', 'demo-tenant', null, 'data-prepare', '批次准备', 'MENU', '/admin/data-prepare', 'views/admin/DataPrepareView.vue', 'menu:data-prepare:view', '20', 200, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-teacher-dashboard', 'demo-tenant', null, 'teacher-dashboard', '教学工作台', 'MENU', '/teacher/dashboard', 'views/teacher/TeacherDashboardView.vue', 'menu:teacher-dashboard:view', '01', 1010, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-teacher-review', 'demo-tenant', null, 'teacher-review', '评阅反馈', 'MENU', '/teacher/review', 'views/teacher/TeacherReviewView.vue', 'menu:teacher-review:view', '02', 1020, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-student-tasks', 'demo-tenant', null, 'student-tasks', '我的任务', 'MENU', '/student/tasks', 'views/student/StudentTasksView.vue', 'menu:student-tasks:view', '01', 2010, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('menu-student-results', 'demo-tenant', null, 'student-results', '成绩反馈', 'MENU', '/student/results', 'views/student/StudentResultsView.vue', 'menu:student-results:view', '02', 2020, true, 'seed', now(), 'seed', now(), 'ACTIVE', false)
) AS value(
    id, tenant_id, parent_id, menu_code, menu_name, menu_type,
    route_path, component_path, permission_code, icon, sort_no, visible,
    create_by, create_time, update_by, update_time, status, deleted
)
ON CONFLICT (tenant_id, menu_code) WHERE deleted = false
DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    menu_name = EXCLUDED.menu_name,
    menu_type = EXCLUDED.menu_type,
    route_path = EXCLUDED.route_path,
    component_path = EXCLUDED.component_path,
    permission_code = EXCLUDED.permission_code,
    icon = EXCLUDED.icon,
    sort_no = EXCLUDED.sort_no,
    visible = EXCLUDED.visible,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

UPDATE sys_role_permission role_permission
SET status = 'DISABLED',
    deleted = true,
    update_by = 'seed',
    update_time = now()
FROM sys_permission_config permission
WHERE role_permission.tenant_id = 'demo-tenant'
  AND role_permission.deleted = false
  AND permission.id = role_permission.permission_id
  AND permission.tenant_id = role_permission.tenant_id
  AND permission.resource_code = 'teacher-data-prepare';

UPDATE sys_permission_config
SET status = 'DISABLED',
    deleted = true,
    update_by = 'seed',
    update_time = now()
WHERE tenant_id = 'demo-tenant'
  AND deleted = false
  AND resource_code = 'teacher-data-prepare';

UPDATE sys_menu_config
SET visible = false,
    status = 'DISABLED',
    deleted = true,
    update_by = 'seed',
    update_time = now()
WHERE tenant_id = 'demo-tenant'
  AND deleted = false
  AND menu_code = 'teacher-data-prepare';

INSERT INTO sys_menu_config (
    id, tenant_id, parent_id, menu_code, menu_name, menu_type,
    route_path, component_path, permission_code, icon, sort_no, visible,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES (
    'menu-data-prepare-classic-cases',
    'demo-tenant',
    null,
    'data-prepare-classic-cases',
    U&'\7ECF\5178\6848\4F8B',
    'MENU',
    '/admin/data-prepare/classic-cases',
    'views/admin/DataPrepareClassicCasesView.vue',
    'menu:data-prepare-classic-cases:view',
    '20',
    195,
    true,
    'seed',
    now(),
    'seed',
    now(),
    'ACTIVE',
    false
)
ON CONFLICT (tenant_id, menu_code) WHERE deleted = false
DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    menu_name = EXCLUDED.menu_name,
    menu_type = EXCLUDED.menu_type,
    route_path = EXCLUDED.route_path,
    component_path = EXCLUDED.component_path,
    permission_code = EXCLUDED.permission_code,
    icon = EXCLUDED.icon,
    sort_no = EXCLUDED.sort_no,
    visible = EXCLUDED.visible,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO sys_permission_config (
    id, tenant_id, permission_code, permission_name, resource_type,
    resource_code, action_code, description,
    create_by, create_time, update_by, update_time, status, deleted
)
SELECT
    'perm-' || menu_code,
    tenant_id,
    permission_code,
    menu_name || '查看',
    'MENU',
    menu_code,
    'VIEW',
    '允许查看“' || menu_name || '”菜单及其对应页面入口。',
    'seed',
    now(),
    'seed',
    now(),
    'ACTIVE',
    false
FROM sys_menu_config
WHERE tenant_id = 'demo-tenant'
  AND deleted = false
  AND status = 'ACTIVE'
  AND permission_code IS NOT NULL
ON CONFLICT (tenant_id, permission_code) WHERE deleted = false
DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    resource_type = EXCLUDED.resource_type,
    resource_code = EXCLUDED.resource_code,
    action_code = EXCLUDED.action_code,
    description = EXCLUDED.description,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO sys_permission_config (
    id, tenant_id, permission_code, permission_name, resource_type,
    resource_code, action_code, description,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('perm-system-user-create', 'demo-tenant', 'system:user:create', '用户新增', 'BUTTON', 'system-user', 'CREATE', '允许新增教学平台用户。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-user-view', 'demo-tenant', 'system:user:view', '用户查看', 'BUTTON', 'system-user', 'VIEW', '允许查看用户详情。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-user-edit', 'demo-tenant', 'system:user:edit', '用户编辑', 'BUTTON', 'system-user', 'EDIT', '允许编辑教学平台用户。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-user-enable', 'demo-tenant', 'system:user:enable', '用户启用', 'BUTTON', 'system-user', 'ENABLE', '允许启用教学平台用户。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-user-disable', 'demo-tenant', 'system:user:disable', '用户停用', 'BUTTON', 'system-user', 'DISABLE', '允许停用教学平台用户。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-role-create', 'demo-tenant', 'system:role:create', '角色新增', 'BUTTON', 'system-role', 'CREATE', '允许新增教学平台角色。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-role-view', 'demo-tenant', 'system:role:view', '角色查看', 'BUTTON', 'system-role', 'VIEW', '允许查看角色详情。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-role-edit', 'demo-tenant', 'system:role:edit', '角色编辑', 'BUTTON', 'system-role', 'EDIT', '允许编辑教学平台角色。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-role-enable', 'demo-tenant', 'system:role:enable', '角色启用', 'BUTTON', 'system-role', 'ENABLE', '允许启用教学平台角色。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-role-disable', 'demo-tenant', 'system:role:disable', '角色停用', 'BUTTON', 'system-role', 'DISABLE', '允许停用教学平台角色。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-org-create', 'demo-tenant', 'system:org:create', '单位新增', 'BUTTON', 'system-org', 'CREATE', '允许新增教学平台单位。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-org-view', 'demo-tenant', 'system:org:view', '单位查看', 'BUTTON', 'system-org', 'VIEW', '允许查看单位详情。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-org-edit', 'demo-tenant', 'system:org:edit', '单位编辑', 'BUTTON', 'system-org', 'EDIT', '允许编辑教学平台单位。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-org-enable', 'demo-tenant', 'system:org:enable', '单位启用', 'BUTTON', 'system-org', 'ENABLE', '允许启用教学平台单位。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-org-disable', 'demo-tenant', 'system:org:disable', '单位停用', 'BUTTON', 'system-org', 'DISABLE', '允许停用教学平台单位。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-user-role-grant', 'demo-tenant', 'system:user-role:grant', '用户角色绑定', 'BUTTON', 'system-user-role', 'GRANT', '允许绑定用户角色。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-user-role-view', 'demo-tenant', 'system:user-role:view', '用户角色查看', 'BUTTON', 'system-user-role', 'VIEW', '允许查看用户角色绑定。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-user-role-enable', 'demo-tenant', 'system:user-role:enable', '用户角色启用', 'BUTTON', 'system-user-role', 'ENABLE', '允许启用用户角色绑定。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-user-role-disable', 'demo-tenant', 'system:user-role:disable', '用户角色停用', 'BUTTON', 'system-user-role', 'DISABLE', '允许停用用户角色绑定。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-user-role-remove', 'demo-tenant', 'system:user-role:remove', '用户角色移除', 'BUTTON', 'system-user-role', 'REMOVE', '允许移除用户角色绑定。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-user-org-grant', 'demo-tenant', 'system:user-org:grant', '用户单位绑定', 'BUTTON', 'system-user-org', 'GRANT', '允许绑定用户单位。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-user-org-view', 'demo-tenant', 'system:user-org:view', '用户单位查看', 'BUTTON', 'system-user-org', 'VIEW', '允许查看用户单位绑定。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-user-org-remove', 'demo-tenant', 'system:user-org:remove', '用户单位移除', 'BUTTON', 'system-user-org', 'REMOVE', '允许移除用户单位绑定。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-menu-create', 'demo-tenant', 'system:menu:create', '菜单新增', 'BUTTON', 'system-menu', 'CREATE', '允许新增菜单配置。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-menu-view', 'demo-tenant', 'system:menu:view', '菜单查看', 'BUTTON', 'system-menu', 'VIEW', '允许查看菜单详情。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-menu-edit', 'demo-tenant', 'system:menu:edit', '菜单编辑', 'BUTTON', 'system-menu', 'EDIT', '允许编辑菜单配置。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-menu-enable', 'demo-tenant', 'system:menu:enable', '菜单启用', 'BUTTON', 'system-menu', 'ENABLE', '允许启用菜单配置。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-menu-disable', 'demo-tenant', 'system:menu:disable', '菜单停用', 'BUTTON', 'system-menu', 'DISABLE', '允许停用菜单配置。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-permission-create', 'demo-tenant', 'system:permission:create', '权限新增', 'BUTTON', 'system-permission', 'CREATE', '允许新增权限点。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-permission-view', 'demo-tenant', 'system:permission:view', '权限查看', 'BUTTON', 'system-permission', 'VIEW', '允许查看权限点详情。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-permission-edit', 'demo-tenant', 'system:permission:edit', '权限编辑', 'BUTTON', 'system-permission', 'EDIT', '允许编辑权限点。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-permission-enable', 'demo-tenant', 'system:permission:enable', '权限启用', 'BUTTON', 'system-permission', 'ENABLE', '允许启用权限点。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-permission-disable', 'demo-tenant', 'system:permission:disable', '权限停用', 'BUTTON', 'system-permission', 'DISABLE', '允许停用权限点。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-role-permission-grant', 'demo-tenant', 'system:role-permission:grant', '角色权限绑定', 'BUTTON', 'system-role-permission', 'GRANT', '允许绑定角色权限。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-role-permission-view', 'demo-tenant', 'system:role-permission:view', '角色权限查看', 'BUTTON', 'system-role-permission', 'VIEW', '允许查看角色权限绑定。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-role-permission-enable', 'demo-tenant', 'system:role-permission:enable', '角色权限启用', 'BUTTON', 'system-role-permission', 'ENABLE', '允许启用角色权限绑定。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-role-permission-disable', 'demo-tenant', 'system:role-permission:disable', '角色权限停用', 'BUTTON', 'system-role-permission', 'DISABLE', '允许停用角色权限绑定。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-dict-create', 'demo-tenant', 'system:dict:create', '字典新增', 'BUTTON', 'system-dict', 'CREATE', '允许新增字典项。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-dict-view', 'demo-tenant', 'system:dict:view', '字典查看', 'BUTTON', 'system-dict', 'VIEW', '允许查看字典项详情。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-dict-edit', 'demo-tenant', 'system:dict:edit', '字典编辑', 'BUTTON', 'system-dict', 'EDIT', '允许编辑字典项。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-dict-enable', 'demo-tenant', 'system:dict:enable', '字典启用', 'BUTTON', 'system-dict', 'ENABLE', '允许启用字典项。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('perm-system-dict-disable', 'demo-tenant', 'system:dict:disable', '字典停用', 'BUTTON', 'system-dict', 'DISABLE', '允许停用字典项。', 'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, permission_code) WHERE deleted = false
DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    resource_type = EXCLUDED.resource_type,
    resource_code = EXCLUDED.resource_code,
    action_code = EXCLUDED.action_code,
    description = EXCLUDED.description,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

WITH admin_permissions AS (
    SELECT
        'srp-admin-' || md5(permission.id) AS id,
        role.tenant_id,
        role.id AS role_id,
        permission.id AS permission_id
    FROM teach_role role
    JOIN sys_permission_config permission
      ON permission.tenant_id = role.tenant_id
     AND permission.deleted = false
     AND permission.status = 'ACTIVE'
    WHERE role.tenant_id = 'demo-tenant'
      AND role.role_code = 'admin'
      AND role.deleted = false
),
teacher_permissions AS (
    SELECT
        'srp-teacher-' || md5(permission.id) AS id,
        role.tenant_id,
        role.id AS role_id,
        permission.id AS permission_id
    FROM teach_role role
    JOIN sys_permission_config permission
      ON permission.tenant_id = role.tenant_id
     AND permission.deleted = false
     AND permission.status = 'ACTIVE'
     AND permission.resource_code IN (
        'teacher-dashboard',
        'teacher-review',
        'lesson-list',
        'data-prepare-systems',
        'data-prepare-modules',
        'data-prepare-templates',
        'data-prepare-strategies',
        'data-prepare-classic-cases',
        'data-prepare'
     )
    WHERE role.tenant_id = 'demo-tenant'
      AND role.role_code = 'teacher'
      AND role.deleted = false
),
expert_permissions AS (
    SELECT
        'srp-expert-' || md5(permission.id) AS id,
        role.tenant_id,
        role.id AS role_id,
        permission.id AS permission_id
    FROM teach_role role
    JOIN sys_permission_config permission
      ON permission.tenant_id = role.tenant_id
     AND permission.deleted = false
     AND permission.status = 'ACTIVE'
     AND permission.resource_code IN (
        'lesson-list',
        'data-prepare-systems',
        'data-prepare-modules',
        'data-prepare-templates',
        'data-prepare-strategies',
        'data-prepare-classic-cases',
        'data-prepare'
     )
    WHERE role.tenant_id = 'demo-tenant'
      AND role.role_code = 'expert'
      AND role.deleted = false
),
student_permissions AS (
    SELECT
        'srp-student-' || md5(permission.id) AS id,
        role.tenant_id,
        role.id AS role_id,
        permission.id AS permission_id
    FROM teach_role role
    JOIN sys_permission_config permission
      ON permission.tenant_id = role.tenant_id
     AND permission.deleted = false
     AND permission.status = 'ACTIVE'
     AND permission.resource_code IN (
        'student-tasks',
        'student-results'
     )
    WHERE role.tenant_id = 'demo-tenant'
      AND role.role_code = 'student'
      AND role.deleted = false
),
all_role_permissions AS (
    SELECT * FROM admin_permissions
    UNION ALL
    SELECT * FROM teacher_permissions
    UNION ALL
    SELECT * FROM expert_permissions
    UNION ALL
    SELECT * FROM student_permissions
)
INSERT INTO sys_role_permission (
    id, tenant_id, role_id, permission_id, grant_source,
    create_by, create_time, update_by, update_time, status, deleted
)
SELECT
    id,
    tenant_id,
    role_id,
    permission_id,
    'SEED',
    'seed',
    now(),
    'seed',
    now(),
    'ACTIVE',
    false
FROM all_role_permissions
ON CONFLICT (tenant_id, role_id, permission_id) WHERE deleted = false
DO UPDATE SET
    grant_source = EXCLUDED.grant_source,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO sys_dict_item (
    id, tenant_id, dict_code, dict_name, item_code, item_name, item_value,
    sort_no, remark, create_by, create_time, update_by, update_time, status, deleted
)
VALUES
    ('dict-dp-auth-api-key', 'demo-tenant', 'data_prepare_auth_type', '数据准备认证方式', 'API_KEY', 'API Key', 'API_KEY', 10, '第三方原平台通过 X-API-Key 请求头调用教学平台。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-cap-data-create', 'demo-tenant', 'data_prepare_capability', '数据准备能力', 'DATA_CREATE', '数据创建', 'DATA_CREATE', 10, 'P0 阶段唯一开放能力。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-scene-record', 'demo-tenant', 'data_prepare_scene_type', '数据准备场景', 'RECORD', '备案', 'RECORD', 10, '老师或专家备案讲解场景。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-scene-learn', 'demo-tenant', 'data_prepare_scene_type', '数据准备场景', 'LEARN', '学习', 'LEARN', 20, '学生学习场景。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-scene-practice', 'demo-tenant', 'data_prepare_scene_type', '数据准备场景', 'PRACTICE', '练习', 'PRACTICE', 30, '学生练习场景。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-scene-exam', 'demo-tenant', 'data_prepare_scene_type', '数据准备场景', 'EXAM', '考试', 'EXAM', 40, '考试场景。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-template-usage-normal', 'demo-tenant', 'data_prepare_template_usage', '模板用途', 'NORMAL', '普通造数模板', 'NORMAL', 10, '正常教学造数使用。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-template-usage-replay', 'demo-tenant', 'data_prepare_template_usage', '模板用途', 'CLASSIC_CASE_REPLAY', '经典案例还原模板', 'CLASSIC_CASE_REPLAY', 20, '专家或老师备案教学时还原经典案例数据。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-template-usage-demo', 'demo-tenant', 'data_prepare_template_usage', '模板用途', 'CLASSIC_CASE_DEMO', '经典案例练习模板', 'CLASSIC_CASE_DEMO', 30, '学生练习时按经典案例格式生成 demo 数据。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-platform-local-dev', 'demo-tenant', 'data_prepare_platform_type', '原平台类型', 'LOCAL_DEV', '本地联调', 'LOCAL_DEV', 10, '开发联调使用。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-platform-custom', 'demo-tenant', 'data_prepare_platform_type', '原平台类型', 'CUSTOM', '自定义系统', 'CUSTOM', 20, '通用第三方原平台。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-platform-oa', 'demo-tenant', 'data_prepare_platform_type', '原平台类型', 'OA', 'OA 系统', 'OA', 30, '办公类原平台。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-platform-erp', 'demo-tenant', 'data_prepare_platform_type', '原平台类型', 'ERP', 'ERP 系统', 'ERP', 40, '业务资源类原平台。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-env-prod', 'demo-tenant', 'data_prepare_environment_type', '原平台环境类型', 'PROD', '正式环境', 'PROD', 10, '专家创建经典案例的数据来源环境。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-env-learning', 'demo-tenant', 'data_prepare_environment_type', '原平台环境类型', 'LEARNING', '学习环境', 'LEARNING', 20, '备案、学习、练习、考试造数目标环境。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-source-mock', 'demo-tenant', 'data_prepare_source_strategy', '数据来源策略', 'MOCK_GENERATE', '系统生成', 'MOCK_GENERATE', 10, '由原平台学习环境按模板生成数据。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-source-pull', 'demo-tenant', 'data_prepare_source_strategy', '数据来源策略', 'PULL_ORIGIN', '原平台拉取', 'PULL_ORIGIN', 20, '从原平台读取或复刻数据。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-timing-demand', 'demo-tenant', 'data_prepare_prepare_timing', '数据准备时机', 'ON_DEMAND', '按需准备', 'ON_DEMAND', 10, '用户发起时即时造数。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-timing-publish', 'demo-tenant', 'data_prepare_prepare_timing', '数据准备时机', 'ON_PUBLISH', '发布时准备', 'ON_PUBLISH', 20, '教学任务发布时预生成。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-timing-before-start', 'demo-tenant', 'data_prepare_prepare_timing', '数据准备时机', 'BEFORE_START', '开始前准备', 'BEFORE_START', 30, '学习或考试开始前生成。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-share-readonly', 'demo-tenant', 'data_prepare_share_policy', '数据共享策略', 'SHARED_READONLY', '只读共享', 'SHARED_READONLY', 10, '多人只读复用同一份数据。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-share-student', 'demo-tenant', 'data_prepare_share_policy', '数据共享策略', 'STUDENT_EXCLUSIVE', '学生独占', 'STUDENT_EXCLUSIVE', 20, '每个学生独占一份数据。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-share-attempt', 'demo-tenant', 'data_prepare_share_policy', '数据共享策略', 'ATTEMPT_EXCLUSIVE', '练习独占', 'ATTEMPT_EXCLUSIVE', 30, '每次练习独占一份数据。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-share-question', 'demo-tenant', 'data_prepare_share_policy', '数据共享策略', 'QUESTION_EXCLUSIVE', '题目独占', 'QUESTION_EXCLUSIVE', 40, '按题目分配独立数据。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-regenerate-never', 'demo-tenant', 'data_prepare_regenerate_policy', '数据重建策略', 'NEVER', '不重建', 'NEVER', 10, '已生成数据持续复用。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-regenerate-attempt', 'demo-tenant', 'data_prepare_regenerate_policy', '数据重建策略', 'ON_ATTEMPT', '每次练习重建', 'ON_ATTEMPT', 20, '每次练习重新生成数据。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-regenerate-retake', 'demo-tenant', 'data_prepare_regenerate_policy', '数据重建策略', 'ON_RETAKE', '补考重建', 'ON_RETAKE', 30, '补考时重新生成数据。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-regenerate-failure', 'demo-tenant', 'data_prepare_regenerate_policy', '数据重建策略', 'ON_FAILURE', '失败重建', 'ON_FAILURE', 40, '生成失败后重新生成。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-lock-none', 'demo-tenant', 'data_prepare_lock_policy', '数据锁定策略', 'NONE', '不锁定', 'NONE', 10, '生成后不锁定。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-lock-allocate', 'demo-tenant', 'data_prepare_lock_policy', '数据锁定策略', 'ON_ALLOCATE', '领取时锁定', 'ON_ALLOCATE', 20, '分配给用户后锁定。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-dp-lock-exam-start', 'demo-tenant', 'data_prepare_lock_policy', '数据锁定策略', 'ON_EXAM_START', '考试开始锁定', 'ON_EXAM_START', 30, '考试开始后锁定。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-origin-status-draft', 'demo-tenant', 'origin_record_status', '原平台业务状态', 'DRAFT', '草稿', 'DRAFT', 10, '原平台草稿状态。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-origin-status-submitted', 'demo-tenant', 'origin_record_status', '原平台业务状态', 'SUBMITTED', '已提交', 'SUBMITTED', 20, '原平台提交状态。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dict-origin-status-approved', 'demo-tenant', 'origin_record_status', '原平台业务状态', 'APPROVED', '已通过', 'APPROVED', 30, '原平台审批通过状态。', 'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, dict_code, item_code) WHERE deleted = false
DO UPDATE SET
    dict_name = EXCLUDED.dict_name,
    item_name = EXCLUDED.item_name,
    item_value = EXCLUDED.item_value,
    sort_no = EXCLUDED.sort_no,
    remark = EXCLUDED.remark,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

COMMIT;
