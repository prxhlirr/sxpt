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
    '{"seed":"system-menu-permission","role":"admin","initialPassword":"local-demo-only"}'::jsonb,
    now(),
    'X9+UuJ57mFq+k1sEtguTvg8UP7BoocS+t+IsrKLyC3Q=',
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
        ('menu-teacher-data-prepare', 'demo-tenant', null, 'teacher-data-prepare', '批次准备', 'MENU', '/teacher/data-prepare', 'views/admin/DataPrepareView.vue', 'menu:teacher-data-prepare:view', '03', 1030, true, 'seed', now(), 'seed', now(), 'ACTIVE', false),
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

WITH admin_permissions AS (
    SELECT
        'srp-admin-' || permission.resource_code AS id,
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
        'srp-teacher-' || permission.resource_code AS id,
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
        'lesson-list'
     )
    WHERE role.tenant_id = 'demo-tenant'
      AND role.role_code = 'teacher'
      AND role.deleted = false
),
student_permissions AS (
    SELECT
        'srp-student-' || permission.resource_code AS id,
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

COMMIT;
