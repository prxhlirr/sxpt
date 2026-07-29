-- 教学平台演示账号初始化脚本
-- 数据库：PostgreSQL
-- 默认租户：demo-tenant
-- 默认密码：Sxpt@123456
-- 密码算法：PBKDF2WithHmacSHA256，迭代次数 120000
-- 说明：脚本按现有唯一约束设计为幂等，可重复执行。

BEGIN;

INSERT INTO teach_role (
    id, tenant_id, role_code, role_name, description,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('role-demo-teacher', 'demo-tenant', 'teacher', '教师', '负责备案、任务发布、过程查看和评阅。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('role-demo-student', 'demo-tenant', 'student', '学生', '负责学习、练习、考试和成果提交。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('role-demo-expert', 'demo-tenant', 'expert', '专家', '负责教案质量复核、评分规则评审和教学诊断。', 'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, role_code) WHERE deleted = false
DO UPDATE SET
    role_name = EXCLUDED.role_name,
    description = EXCLUDED.description,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO teach_org (
    id, tenant_id, parent_id, org_code, org_name, org_type,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('org-demo-school', 'demo-tenant', null, 'school-demo', '演示学院', 'SCHOOL', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('org-demo-class-a', 'demo-tenant', 'org-demo-school', 'class-2026-a', '2026级实训一班', 'CLASS', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('org-demo-class-b', 'demo-tenant', 'org-demo-school', 'class-2026-b', '2026级实训二班', 'CLASS', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('org-demo-expert-group', 'demo-tenant', 'org-demo-school', 'expert-group', '实训专家组', 'EXPERT_GROUP', 'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, org_code) WHERE deleted = false
DO UPDATE SET
    parent_id = EXCLUDED.parent_id,
    org_name = EXCLUDED.org_name,
    org_type = EXCLUDED.org_type,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO teach_user (
    id, tenant_id, username, real_name, phone, email, user_type, source_type,
    external_info_json, last_sync_time,
    password_hash, password_salt, password_algorithm, password_iterations,
    password_status, password_updated_time, last_login_time, failed_login_count,
    locked_until, student_no, employee_no,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    (
        'user-demo-teacher-01', 'demo-tenant', 'teacher01', '王老师', '13800010001', 'teacher01@example.com', 'TEACHER', 'LOCAL',
        '{"seed":"demo","role":"teacher"}'::jsonb, now(),
        'hg3GiyOGiqb/84H5TmZooWK5YK8dr/hIckXg8fkyl70=', '7Pm6g6Gi3BS67usxjm25+A==', 'PBKDF2WithHmacSHA256', 120000,
        'NORMAL', now(), null, 0,
        null, null, 'T2026001',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'user-demo-teacher-02', 'demo-tenant', 'teacher02', '李老师', '13800010002', 'teacher02@example.com', 'TEACHER', 'LOCAL',
        '{"seed":"demo","role":"teacher"}'::jsonb, now(),
        '4DIaLLHDGqffAs5dxEC7NXJ3ZTEzwj40afBT5wFmY5E=', 'TNY7rTINyfzjnNnehjm9lw==', 'PBKDF2WithHmacSHA256', 120000,
        'NORMAL', now(), null, 0,
        null, null, 'T2026002',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'user-demo-teacher-03', 'demo-tenant', 'teacher03', '赵老师', '13800010003', 'teacher03@example.com', 'TEACHER', 'LOCAL',
        '{"seed":"demo","role":"teacher"}'::jsonb, now(),
        'V4OVGH6rql2+7Czkwd518k6Lkgke/gclpFuFJ1VT9aw=', 'ePXAb5KP6BP/NzBMiwG1hg==', 'PBKDF2WithHmacSHA256', 120000,
        'NORMAL', now(), null, 0,
        null, null, 'T2026003',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'user-demo-student-01', 'demo-tenant', 'student01', '陈同学', '13900020001', 'student01@example.com', 'STUDENT', 'LOCAL',
        '{"seed":"demo","role":"student"}'::jsonb, now(),
        'sTxxT6hQomFH/aJorQqXq2Pxcx2QyjIvxMGbwtftllw=', '1CnX1JdgnuALbN/Cotq8aw==', 'PBKDF2WithHmacSHA256', 120000,
        'NORMAL', now(), null, 0,
        null, 'S2026001', null,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'user-demo-student-02', 'demo-tenant', 'student02', '周同学', '13900020002', 'student02@example.com', 'STUDENT', 'LOCAL',
        '{"seed":"demo","role":"student"}'::jsonb, now(),
        '6+jIThfNv+p0C/D8qKxY3gsfTftmpgHhmhlfEHBKeg8=', 'u49sesvoM7kj+rYGVXGxCg==', 'PBKDF2WithHmacSHA256', 120000,
        'NORMAL', now(), null, 0,
        null, 'S2026002', null,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'user-demo-student-03', 'demo-tenant', 'student03', '刘同学', '13900020003', 'student03@example.com', 'STUDENT', 'LOCAL',
        '{"seed":"demo","role":"student"}'::jsonb, now(),
        '3r0JjwF9LOrEKcd88/tI7iyx6jnwlUvda3GCn4UW5dk=', 'NgsMv7b6VcHAiCSkYa5ceQ==', 'PBKDF2WithHmacSHA256', 120000,
        'NORMAL', now(), null, 0,
        null, 'S2026003', null,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'user-demo-expert-01', 'demo-tenant', 'expert01', '孙专家', '13700030001', 'expert01@example.com', 'EXPERT', 'LOCAL',
        '{"seed":"demo","role":"expert"}'::jsonb, now(),
        'gWSd1U68BSiSUXAJR4tPR3ac1pyND5CGu5yS5Yy5WSE=', '0/ftve9wyFvAleqQ1BluFA==', 'PBKDF2WithHmacSHA256', 120000,
        'NORMAL', now(), null, 0,
        null, null, 'E2026001',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'user-demo-expert-02', 'demo-tenant', 'expert02', '吴专家', '13700030002', 'expert02@example.com', 'EXPERT', 'LOCAL',
        '{"seed":"demo","role":"expert"}'::jsonb, now(),
        'e2+ZD0cvnguQbEF6YUpcr/+Znd3l17/wgAhj55lPFDA=', 'iZYBZ5O7jkTf+OaXlmP98A==', 'PBKDF2WithHmacSHA256', 120000,
        'NORMAL', now(), null, 0,
        null, null, 'E2026002',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'user-demo-expert-03', 'demo-tenant', 'expert03', '郑专家', '13700030003', 'expert03@example.com', 'EXPERT', 'LOCAL',
        '{"seed":"demo","role":"expert"}'::jsonb, now(),
        'DL+3JjBgISx5r4o15+m+bGPLCUKL9TZdzja9JOTVCbQ=', 'j2wBdgB/yYKqVkR6/mOGQg==', 'PBKDF2WithHmacSHA256', 120000,
        'NORMAL', now(), null, 0,
        null, null, 'E2026003',
        'seed', now(), 'seed', now(), 'ACTIVE', false
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
) VALUES
    ('ur-demo-teacher-01', 'demo-tenant', 'user-demo-teacher-01', 'role-demo-teacher', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ur-demo-teacher-02', 'demo-tenant', 'user-demo-teacher-02', 'role-demo-teacher', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ur-demo-teacher-03', 'demo-tenant', 'user-demo-teacher-03', 'role-demo-teacher', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ur-demo-student-01', 'demo-tenant', 'user-demo-student-01', 'role-demo-student', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ur-demo-student-02', 'demo-tenant', 'user-demo-student-02', 'role-demo-student', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ur-demo-student-03', 'demo-tenant', 'user-demo-student-03', 'role-demo-student', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ur-demo-expert-01', 'demo-tenant', 'user-demo-expert-01', 'role-demo-expert', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ur-demo-expert-02', 'demo-tenant', 'user-demo-expert-02', 'role-demo-expert', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ur-demo-expert-03', 'demo-tenant', 'user-demo-expert-03', 'role-demo-expert', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, user_id, role_id) WHERE deleted = false
DO UPDATE SET
    grant_source = EXCLUDED.grant_source,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO teach_user_org (
    id, tenant_id, user_id, org_id, relation_type,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('uo-demo-teacher-01', 'demo-tenant', 'user-demo-teacher-01', 'org-demo-class-a', 'TEACHER', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('uo-demo-teacher-02', 'demo-tenant', 'user-demo-teacher-02', 'org-demo-class-b', 'TEACHER', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('uo-demo-teacher-03', 'demo-tenant', 'user-demo-teacher-03', 'org-demo-school', 'TEACHER', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('uo-demo-student-01', 'demo-tenant', 'user-demo-student-01', 'org-demo-class-a', 'STUDENT', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('uo-demo-student-02', 'demo-tenant', 'user-demo-student-02', 'org-demo-class-a', 'STUDENT', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('uo-demo-student-03', 'demo-tenant', 'user-demo-student-03', 'org-demo-class-b', 'STUDENT', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('uo-demo-expert-01', 'demo-tenant', 'user-demo-expert-01', 'org-demo-expert-group', 'EXPERT', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('uo-demo-expert-02', 'demo-tenant', 'user-demo-expert-02', 'org-demo-expert-group', 'EXPERT', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('uo-demo-expert-03', 'demo-tenant', 'user-demo-expert-03', 'org-demo-expert-group', 'EXPERT', 'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, user_id, org_id) WHERE deleted = false
DO UPDATE SET
    relation_type = EXCLUDED.relation_type,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

COMMIT;
