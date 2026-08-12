-- 原平台采购申请数据准备演示脚本
-- 目标数据库：PostgreSQL
-- 设计原则：只保存教学平台需要的原平台业务引用、状态、上下文、校验摘要和评分快照，不保存原平台完整业务明细。

SET client_encoding = 'UTF8';

BEGIN;

INSERT INTO teach_role (
    id, tenant_id, role_code, role_name, description,
    create_by, create_time, update_by, update_time, status, deleted
)
SELECT *
FROM (
    VALUES
        ('role-demo-admin', 'demo-tenant', 'admin', '管理员', '负责原平台接入、数据准备配置和运行监控。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('role-demo-teacher', 'demo-tenant', 'teacher', '教师', '负责备课、任务发布、过程查看和评分复核。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
        ('role-demo-student', 'demo-tenant', 'student', '学生', '负责学习、练习、考试和业务办理提交。', 'seed', now(), 'seed', now(), 'ACTIVE', false)
) AS value(
    id, tenant_id, role_code, role_name, description,
    create_by, create_time, update_by, update_time, status, deleted
)
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
    ('org-origin-purchase-dept', 'demo-tenant', 'org-demo-school', 'origin-purchase-dept', '原平台采购申请单位', 'ORIGIN_ORG', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('org-origin-review-dept', 'demo-tenant', 'org-demo-school', 'origin-review-dept', '原平台采购复核单位', 'ORIGIN_ORG', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('org-origin-approve-dept', 'demo-tenant', 'org-demo-school', 'origin-approve-dept', '原平台采购审批单位', 'ORIGIN_ORG', 'seed', now(), 'seed', now(), 'ACTIVE', false)
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
    password_status, password_updated_time, failed_login_count, locked_until,
    student_no, employee_no,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('user-demo-admin-01', 'demo-tenant', 'admin01', '系统管理员', '13800000001', 'admin01@example.com', 'ADMIN', 'LOCAL',
     '{"seed":"origin-purchase-data-prepare","role":"admin"}'::jsonb, now(),
     'WmT/VqfxqH/kETaWuDqVAm5EOWkLz/6kS9e/YyYtXjk=', 'c3hwdC1kZW1vLWFkbS0wMQ==', 'PBKDF2WithHmacSHA256', 120000,
     'NORMAL', now(), 0, null, null, 'A2026001',
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('user-demo-teacher-01', 'demo-tenant', 'teacher01', '王老师', '13800010001', 'teacher01@example.com', 'TEACHER', 'LOCAL',
     '{"seed":"origin-purchase-data-prepare","role":"teacher"}'::jsonb, now(),
     'AsuvH6cfVVkrdnQJs5TfmLOjbet7DRDOl/5SglUZyXM=', 'c3hwdC1kZW1vLXRjaC0wMQ==', 'PBKDF2WithHmacSHA256', 120000,
     'NORMAL', now(), 0, null, null, 'T2026001',
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('user-demo-student-01', 'demo-tenant', 'student01', '陈同学', '13900020001', 'student01@example.com', 'STUDENT', 'LOCAL',
     '{"seed":"origin-purchase-data-prepare","role":"student"}'::jsonb, now(),
     'llJ2UbNkZ7eybx6JNYbQKIiBCiIk52pbd016Sk+uruE=', 'c3hwdC1kZW1vLXN0dS0wMQ==', 'PBKDF2WithHmacSHA256', 120000,
     'NORMAL', now(), 0, null, 'S2026001', null,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('user-demo-student-02', 'demo-tenant', 'student02', '周同学', '13900020002', 'student02@example.com', 'STUDENT', 'LOCAL',
     '{"seed":"origin-purchase-data-prepare","role":"student"}'::jsonb, now(),
     '3r/EpPJwJp6dF+aImPdB8jcX2MNnvBmJMLrqDGFZ82I=', 'c3hwdC1kZW1vLXN0dS0wMg==', 'PBKDF2WithHmacSHA256', 120000,
     'NORMAL', now(), 0, null, 'S2026002', null,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('user-demo-student-03', 'demo-tenant', 'student03', '刘同学', '13900020003', 'student03@example.com', 'STUDENT', 'LOCAL',
     '{"seed":"origin-purchase-data-prepare","role":"student"}'::jsonb, now(),
     'nPhM8GYgRRbZSapcQl+wDZ6D/1s8pbHxdDUjBvB3Iww=', 'c3hwdC1kZW1vLXN0dS0wMw==', 'PBKDF2WithHmacSHA256', 120000,
     'NORMAL', now(), 0, null, 'S2026003', null,
     'seed', now(), 'seed', now(), 'ACTIVE', false)
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
    ('ur-demo-admin-01', 'demo-tenant', 'user-demo-admin-01', 'role-demo-admin', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ur-demo-teacher-01', 'demo-tenant', 'user-demo-teacher-01', 'role-demo-teacher', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ur-demo-student-01', 'demo-tenant', 'user-demo-student-01', 'role-demo-student', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ur-demo-student-02', 'demo-tenant', 'user-demo-student-02', 'role-demo-student', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ur-demo-student-03', 'demo-tenant', 'user-demo-student-03', 'role-demo-student', 'SEED', 'seed', now(), 'seed', now(), 'ACTIVE', false)
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
    ('uo-demo-admin-01', 'demo-tenant', 'user-demo-admin-01', 'org-demo-school', 'ADMIN', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('uo-demo-teacher-01', 'demo-tenant', 'user-demo-teacher-01', 'org-demo-class-a', 'TEACHER', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('uo-demo-student-01', 'demo-tenant', 'user-demo-student-01', 'org-demo-class-a', 'STUDENT', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('uo-demo-student-02', 'demo-tenant', 'user-demo-student-02', 'org-demo-class-a', 'STUDENT', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('uo-demo-student-03', 'demo-tenant', 'user-demo-student-03', 'org-demo-class-a', 'STUDENT', 'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (id)
DO UPDATE SET
    tenant_id = EXCLUDED.tenant_id,
    user_id = EXCLUDED.user_id,
    org_id = EXCLUDED.org_id,
    relation_type = EXCLUDED.relation_type,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE',
    deleted = false;

INSERT INTO connector_system (
    id, tenant_id, system_code, system_name, system_type, base_url, auth_type, config_json,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES (
    'origin-system-purchase-demo',
    'demo-tenant',
    'origin-purchase-demo',
    '原平台采购业务系统',
    'ORIGIN_BUSINESS',
    'http://localhost:5173/business-sdk-demo.html',
    'API_KEY',
    '{
        "apiKey":"purchase-demo-api-key",
        "headerName":"X-API-Key",
        "sdkMode":"embedded",
        "demoSource":"sxpt_origin_system",
        "entryPath":"/business-sdk-demo.html",
        "description":"基于原型 sxpt_origin_system 整理的采购申请实训原平台。"
    }'::jsonb,
    'seed', now(), 'seed', now(), 'ACTIVE', false
)
ON CONFLICT (tenant_id, system_code) WHERE deleted = false
DO UPDATE SET
    system_name = EXCLUDED.system_name,
    system_type = EXCLUDED.system_type,
    base_url = EXCLUDED.base_url,
    auth_type = EXCLUDED.auth_type,
    config_json = EXCLUDED.config_json,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO identity_binding (
    id, tenant_id, user_id, connector_system_id, external_user_id, external_username,
    external_role_json, external_org_json, binding_type, last_login_time,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('ib-origin-admin-01', 'demo-tenant', 'user-demo-admin-01', 'origin-system-purchase-demo', 'origin-admin-01', 'admin01',
     '[{"externalRoleId":"origin-admin","externalRoleName":"系统管理员"}]'::jsonb,
     '[{"externalOrgId":"origin-platform","externalOrgName":"原平台管理域"}]'::jsonb,
     'SEED', null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ib-origin-teacher-01', 'demo-tenant', 'user-demo-teacher-01', 'origin-system-purchase-demo', 'origin-teacher-01', 'teacher01',
     '[{"externalRoleId":"origin-teacher","externalRoleName":"备课教师"}]'::jsonb,
     '[{"externalOrgId":"origin-purchase-dept","externalOrgName":"原平台采购申请单位"}]'::jsonb,
     'SEED', null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ib-origin-student-01', 'demo-tenant', 'user-demo-student-01', 'origin-system-purchase-demo', 'origin-student-01', 'student01',
     '[{"externalRoleId":"creator","externalRoleName":"申请人"}]'::jsonb,
     '[{"externalOrgId":"origin-purchase-dept","externalOrgName":"原平台采购申请单位"}]'::jsonb,
     'SEED', null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ib-origin-student-02', 'demo-tenant', 'user-demo-student-02', 'origin-system-purchase-demo', 'origin-student-02', 'student02',
     '[{"externalRoleId":"reviewer","externalRoleName":"复核人"}]'::jsonb,
     '[{"externalOrgId":"origin-review-dept","externalOrgName":"原平台采购复核单位"}]'::jsonb,
     'SEED', null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ib-origin-student-03', 'demo-tenant', 'user-demo-student-03', 'origin-system-purchase-demo', 'origin-student-03', 'student03',
     '[{"externalRoleId":"approver","externalRoleName":"审批人"}]'::jsonb,
     '[{"externalOrgId":"origin-approve-dept","externalOrgName":"原平台采购审批单位"}]'::jsonb,
     'SEED', null, 'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, connector_system_id, external_user_id) WHERE deleted = false
DO UPDATE SET
    user_id = EXCLUDED.user_id,
    external_username = EXCLUDED.external_username,
    external_role_json = EXCLUDED.external_role_json,
    external_org_json = EXCLUDED.external_org_json,
    binding_type = EXCLUDED.binding_type,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO platform_capability (
    id, tenant_id, connector_system_id, capability_code, capability_name, capability_type,
    support_flag, endpoint_url, method, request_schema_json, response_schema_json,
    timeout_ms, retry_policy_json,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('cap-origin-purchase-data-create', 'demo-tenant', 'origin-system-purchase-demo', 'DATA_CREATE', '批量创建教学业务数据', 'DATA_CREATE',
     true, '/origin/openapi/teaching/data/batch-create', 'POST',
     '{"required":["requestBatchId","items"],"itemKeys":["clientRequestItemId","moduleCode","requiredExternalOrgId","requiredExternalRoleId","initExternalStatus"]}'::jsonb,
     '{"required":["batchId","items"],"itemKeys":["clientRequestItemId","externalBusinessId","externalBusinessNo","externalStatus","targetUrl","validationResult"]}'::jsonb,
     10000, '{"maxAttempts":3,"backoffMs":1000}'::jsonb,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('cap-origin-purchase-data-validate', 'demo-tenant', 'origin-system-purchase-demo', 'DATA_VALIDATE', '校验业务数据可用性', 'DATA_VALIDATE',
     false, '/origin/openapi/teaching/data/validate', 'POST',
     '{"required":["externalBusinessId","requiredExternalOrgId","requiredExternalRoleId"]}'::jsonb,
     '{"required":["visible","operable","statusMatched"]}'::jsonb,
     5000, '{"maxAttempts":2,"backoffMs":500}'::jsonb,
     'seed', now(), 'seed', now(), 'DISABLED', false),
    ('cap-origin-purchase-data-lock', 'demo-tenant', 'origin-system-purchase-demo', 'DATA_LOCK', '锁定考试业务数据', 'DATA_LOCK',
     false, '/origin/openapi/teaching/data/lock', 'POST',
     '{"required":["externalBusinessId","lockReason"]}'::jsonb,
     '{"required":["locked","externalStatus"]}'::jsonb,
     5000, '{"maxAttempts":2,"backoffMs":500}'::jsonb,
     'seed', now(), 'seed', now(), 'DISABLED', false),
    ('cap-origin-purchase-result-check', 'demo-tenant', 'origin-system-purchase-demo', 'RESULT_CHECK', '校验采购申请办理结果', 'RESULT_CHECK',
     false, '/origin/openapi/teaching/result/check', 'POST',
     '{"required":["externalBusinessId","expectedStatus"]}'::jsonb,
     '{"required":["passed","externalStatus","matchedFields"]}'::jsonb,
     5000, '{"maxAttempts":2,"backoffMs":500}'::jsonb,
     'seed', now(), 'seed', now(), 'DISABLED', false)
ON CONFLICT (tenant_id, connector_system_id, capability_code) WHERE deleted = false
DO UPDATE SET
    capability_name = EXCLUDED.capability_name,
    capability_type = EXCLUDED.capability_type,
    support_flag = EXCLUDED.support_flag,
    endpoint_url = EXCLUDED.endpoint_url,
    method = EXCLUDED.method,
    request_schema_json = EXCLUDED.request_schema_json,
    response_schema_json = EXCLUDED.response_schema_json,
    timeout_ms = EXCLUDED.timeout_ms,
    retry_policy_json = EXCLUDED.retry_policy_json,
    update_by = 'seed',
    update_time = now(),
    status = EXCLUDED.status;

INSERT INTO business_module (
    id, tenant_id, connector_system_id, module_code, module_name, external_module_id,
    entry_url, module_type, support_scenes, need_pre_data, default_initial_status,
    default_target_status, capability_codes_json, default_template_id, remark, lock_version,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES (
    'bm-origin-purchase-apply',
    'demo-tenant',
    'origin-system-purchase-demo',
    'PURCHASE_APPLY',
    '采购申请',
    'origin-module-purchase-apply',
    '/business-sdk-demo.html',
    'APPLICATION',
    'RECORD,LEARN,PRACTICE,EXAM',
    true,
    'DRAFT',
    'APPROVED',
    '["DATA_CREATE"]'::jsonb,
    'tdt-purchase-practice-v1',
    '来自 sxpt_origin_system 原型的采购申请标准流程。', 0,
    'seed', now(), 'seed', now(), 'ACTIVE', false
)
ON CONFLICT (tenant_id, connector_system_id, module_code) WHERE deleted = false
DO UPDATE SET
    module_name = EXCLUDED.module_name,
    external_module_id = EXCLUDED.external_module_id,
    entry_url = EXCLUDED.entry_url,
    module_type = EXCLUDED.module_type,
    support_scenes = EXCLUDED.support_scenes,
    need_pre_data = EXCLUDED.need_pre_data,
    default_initial_status = EXCLUDED.default_initial_status,
    default_target_status = EXCLUDED.default_target_status,
    capability_codes_json = EXCLUDED.capability_codes_json,
    default_template_id = EXCLUDED.default_template_id,
    remark = EXCLUDED.remark,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO module_data_strategy (
    id, tenant_id, connector_system_id, module_code, module_name, scene_type,
    need_pre_data, data_source_strategy, init_external_status, target_external_status,
    default_org_role_policy_json, share_policy, regenerate_policy, lock_policy,
    expire_policy_json, result_check_policy_json, business_module_id, strategy_code,
    template_id, prepare_timing, pool_size_policy_json, validation_policy_json,
    archive_policy_json, strategy_version, lock_version,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('mds-purchase-record-v1', 'demo-tenant', 'origin-system-purchase-demo', 'PURCHASE_APPLY', '采购申请', 'RECORD',
     true, 'MOCK_GENERATE', 'DRAFT', 'APPROVED',
     '{"defaultOrgId":"origin-purchase-dept","defaultRoleId":"origin-teacher","roleGroups":[{"groupKey":"CREATOR","roleId":"creator"}]}'::jsonb,
     'TEACHER_SAMPLE', 'NEVER', 'NONE',
     '{"ttlDays":30}'::jsonb, '{"expectedFinalStatus":"APPROVED"}'::jsonb,
     'bm-origin-purchase-apply', 'PURCHASE_RECORD_SEED_V1', null, 'ON_DEMAND',
     '{"formalCount":1,"spareCount":0}'::jsonb,
     '{"visible":true,"operable":true,"statusMatched":true}'::jsonb,
     '{"archiveAfterDays":180}'::jsonb, 1, 0,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('mds-purchase-practice-v1', 'demo-tenant', 'origin-system-purchase-demo', 'PURCHASE_APPLY', '采购申请', 'PRACTICE',
     true, 'MOCK_GENERATE', 'DRAFT', 'APPROVED',
     '{"roleGroups":[{"groupKey":"CREATOR","roleId":"creator","orgId":"origin-purchase-dept"}]}'::jsonb,
     'ATTEMPT_EXCLUSIVE', 'ON_ATTEMPT', 'ON_ALLOCATE',
     '{"ttlDays":7}'::jsonb, '{"expectedFinalStatus":"SUBMITTED"}'::jsonb,
     'bm-origin-purchase-apply', 'PURCHASE_PRACTICE_SEED_V1', null, 'ON_DEMAND',
     '{"formalCountPerStudent":1,"spareRate":0}'::jsonb,
     '{"visible":true,"operable":true,"statusMatched":true}'::jsonb,
     '{"archiveAfterDays":90}'::jsonb, 1, 0,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('mds-purchase-exam-v1', 'demo-tenant', 'origin-system-purchase-demo', 'PURCHASE_APPLY', '采购申请', 'EXAM',
     true, 'MOCK_GENERATE', 'DRAFT', 'APPROVED',
     '{"roleGroups":[{"groupKey":"CREATOR","roleId":"creator","orgId":"origin-purchase-dept"},{"groupKey":"REVIEWER","roleId":"reviewer","orgId":"origin-review-dept"},{"groupKey":"APPROVER","roleId":"approver","orgId":"origin-approve-dept"}]}'::jsonb,
     'QUESTION_EXCLUSIVE', 'ON_RETAKE', 'ON_EXAM_START',
     '{"ttlDays":3}'::jsonb, '{"expectedFinalStatus":"APPROVED"}'::jsonb,
     'bm-origin-purchase-apply', 'PURCHASE_EXAM_SEED_V1', null, 'BEFORE_START',
     '{"formalCountPerQuestion":1,"spareCount":1}'::jsonb,
     '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb,
     '{"archiveAfterDays":365}'::jsonb, 1, 0,
     'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, connector_system_id, module_code, scene_type) WHERE deleted = false
DO UPDATE SET
    module_name = EXCLUDED.module_name,
    need_pre_data = EXCLUDED.need_pre_data,
    data_source_strategy = EXCLUDED.data_source_strategy,
    init_external_status = EXCLUDED.init_external_status,
    target_external_status = EXCLUDED.target_external_status,
    default_org_role_policy_json = EXCLUDED.default_org_role_policy_json,
    share_policy = EXCLUDED.share_policy,
    regenerate_policy = EXCLUDED.regenerate_policy,
    lock_policy = EXCLUDED.lock_policy,
    expire_policy_json = EXCLUDED.expire_policy_json,
    result_check_policy_json = EXCLUDED.result_check_policy_json,
    business_module_id = EXCLUDED.business_module_id,
    strategy_code = EXCLUDED.strategy_code,
    prepare_timing = EXCLUDED.prepare_timing,
    pool_size_policy_json = EXCLUDED.pool_size_policy_json,
    validation_policy_json = EXCLUDED.validation_policy_json,
    archive_policy_json = EXCLUDED.archive_policy_json,
    strategy_version = EXCLUDED.strategy_version,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO teaching_data_template (
    id, tenant_id, connector_system_id, teaching_point_id, template_code, template_name,
    scene_type, init_state, support_mode, config_json, module_code, strategy_id,
    data_schema_json, mock_rule_json, readonly_flag, request_schema_json,
    required_org_role_json, result_check_schema_json, sensitive_field_policy_json,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('tdt-purchase-record-v1', 'demo-tenant', 'origin-system-purchase-demo', null, 'PURCHASE_RECORD_V1', '采购申请备课样例数据模板',
     'RECORD', 'DRAFT', 'TEACHER_RECORD', '{"businessUrl":"/business-sdk-demo.html","source":"sxpt_origin_system"}'::jsonb,
     'PURCHASE_APPLY', 'mds-purchase-record-v1',
     '{"fields":[{"name":"supplierName","type":"string","required":true},{"name":"amount","type":"number","required":true},{"name":"category","type":"string","required":true},{"name":"reason","type":"string","required":true}]}'::jsonb,
     '{"supplierPrefix":"演示供应商","amountRange":[8000,30000],"categoryOptions":["办公设备","实训耗材","软件服务"]}'::jsonb,
     false,
     '{"required":["moduleCode","sceneType","requiredExternalOrgId","requiredExternalRoleId"]}'::jsonb,
     '{"defaultOrgId":"origin-purchase-dept","defaultRoleId":"origin-teacher"}'::jsonb,
     '{"fields":["externalBusinessId","externalBusinessNo","externalStatus","targetUrl"]}'::jsonb,
     '{"mask":["supplierContact","bankAccount"],"forbidPersist":["fullFormPayload"]}'::jsonb,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('tdt-purchase-practice-v1', 'demo-tenant', 'origin-system-purchase-demo', null, 'PURCHASE_PRACTICE_V1', '采购申请练习数据模板',
     'PRACTICE', 'DRAFT', 'STUDENT_PRACTICE', '{"businessUrl":"/business-sdk-demo.html","source":"sxpt_origin_system"}'::jsonb,
     'PURCHASE_APPLY', 'mds-purchase-practice-v1',
     '{"fields":[{"name":"supplierName","type":"string","required":true},{"name":"amount","type":"number","required":true},{"name":"category","type":"string","required":true},{"name":"reason","type":"string","required":true}]}'::jsonb,
     '{"supplierPrefix":"练习供应商","amountRange":[1000,20000],"categoryOptions":["办公设备","实训耗材","软件服务"]}'::jsonb,
     false,
     '{"required":["studentId","taskId","requiredExternalOrgId","requiredExternalRoleId"]}'::jsonb,
     '{"defaultOrgId":"origin-purchase-dept","defaultRoleId":"creator"}'::jsonb,
     '{"fields":["externalStatus","resultCheck.passed"]}'::jsonb,
     '{"mask":["supplierContact","bankAccount"],"forbidPersist":["fullFormPayload"]}'::jsonb,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('tdt-purchase-exam-v1', 'demo-tenant', 'origin-system-purchase-demo', null, 'PURCHASE_EXAM_V1', '采购申请考试数据模板',
     'EXAM', 'DRAFT', 'STUDENT_EXAM', '{"businessUrl":"/business-sdk-demo.html","source":"sxpt_origin_system"}'::jsonb,
     'PURCHASE_APPLY', 'mds-purchase-exam-v1',
     '{"fields":[{"name":"supplierName","type":"string","required":true},{"name":"amount","type":"number","required":true},{"name":"category","type":"string","required":true},{"name":"reason","type":"string","required":true}]}'::jsonb,
     '{"supplierPrefix":"考试供应商","amountRange":[5000,50000],"categoryOptions":["办公设备","实训耗材","软件服务"],"uniquePerStudent":true}'::jsonb,
     false,
     '{"required":["studentId","questionId","requiredExternalOrgId","requiredExternalRoleId"]}'::jsonb,
     '{"roleGroups":[{"groupKey":"CREATOR","roleId":"creator"},{"groupKey":"REVIEWER","roleId":"reviewer"},{"groupKey":"APPROVER","roleId":"approver"}]}'::jsonb,
     '{"fields":["externalStatus","resultCheck.passed","approvedTime"]}'::jsonb,
     '{"mask":["supplierContact","bankAccount"],"forbidPersist":["fullFormPayload"]}'::jsonb,
     'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, connector_system_id, template_code) WHERE deleted = false
DO UPDATE SET
    template_name = EXCLUDED.template_name,
    scene_type = EXCLUDED.scene_type,
    init_state = EXCLUDED.init_state,
    support_mode = EXCLUDED.support_mode,
    config_json = EXCLUDED.config_json,
    module_code = EXCLUDED.module_code,
    strategy_id = EXCLUDED.strategy_id,
    data_schema_json = EXCLUDED.data_schema_json,
    mock_rule_json = EXCLUDED.mock_rule_json,
    readonly_flag = EXCLUDED.readonly_flag,
    request_schema_json = EXCLUDED.request_schema_json,
    required_org_role_json = EXCLUDED.required_org_role_json,
    result_check_schema_json = EXCLUDED.result_check_schema_json,
    sensitive_field_policy_json = EXCLUDED.sensitive_field_policy_json,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

UPDATE module_data_strategy
SET template_id = CASE scene_type
        WHEN 'RECORD' THEN 'tdt-purchase-record-v1'
        WHEN 'PRACTICE' THEN 'tdt-purchase-practice-v1'
        WHEN 'EXAM' THEN 'tdt-purchase-exam-v1'
        ELSE template_id
    END,
    update_by = 'seed',
    update_time = now()
WHERE tenant_id = 'demo-tenant'
  AND connector_system_id = 'origin-system-purchase-demo'
  AND module_code = 'PURCHASE_APPLY'
  AND deleted = false;

UPDATE business_module
SET default_template_id = 'tdt-purchase-practice-v1',
    update_by = 'seed',
    update_time = now()
WHERE id = 'bm-origin-purchase-apply';

INSERT INTO connector_resource (
    id, tenant_id, connector_system_id, resource_code, resource_name, resource_type,
    page_url, locator, stable_key, metadata_json, source_capture_id,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('res-purchase-create-button', 'demo-tenant', 'origin-system-purchase-demo', 'PURCHASE_CREATE_BUTTON', '新建申请按钮', 'BUTTON',
     '/business-sdk-demo.html', '[data-action="create"]', 'purchase.create', '{"actionType":"click","rect":{"x":84,"y":112,"width":132,"height":44}}'::jsonb, null,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('res-purchase-form', 'demo-tenant', 'origin-system-purchase-demo', 'PURCHASE_BASE_FORM', '申请信息表单', 'FORM',
     '/business-sdk-demo.html', '[data-action="fill"]', 'purchase.form', '{"actionType":"input","rect":{"x":72,"y":202,"width":560,"height":184}}'::jsonb, null,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('res-purchase-submit-button', 'demo-tenant', 'origin-system-purchase-demo', 'PURCHASE_SUBMIT_BUTTON', '提交申请按钮', 'BUTTON',
     '/business-sdk-demo.html', '[data-action="submit"]', 'purchase.submit', '{"actionType":"submit","rect":{"x":500,"y":420,"width":132,"height":44}}'::jsonb, null,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('res-purchase-approve-button', 'demo-tenant', 'origin-system-purchase-demo', 'PURCHASE_APPROVE_BUTTON', '审批通过按钮', 'BUTTON',
     '/business-sdk-demo.html', '[data-action="approve"]', 'purchase.approve', '{"actionType":"click","rect":{"x":356,"y":420,"width":132,"height":44}}'::jsonb, null,
     'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, connector_system_id, resource_code) WHERE deleted = false
DO UPDATE SET
    resource_name = EXCLUDED.resource_name,
    resource_type = EXCLUDED.resource_type,
    page_url = EXCLUDED.page_url,
    locator = EXCLUDED.locator,
    stable_key = EXCLUDED.stable_key,
    metadata_json = EXCLUDED.metadata_json,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO teaching_point (
    id, tenant_id, connector_system_id, point_code, point_name, point_type, version_no,
    source_capture_session_id, business_overview_json, flow_file_id, flow_file_url,
    record_path_json, required_external_role_id, required_external_role_name,
    execution_strategy, default_grant_start_time, default_grant_end_time,
    data_scope_json, overlay_policy_json, description,
    create_by, create_time, update_by, update_time, point_status, status, deleted
) VALUES (
    'tp-purchase-apply-v1',
    'demo-tenant',
    'origin-system-purchase-demo',
    'TP_PURCHASE_APPLY',
    '采购申请办理',
    'OPERATION',
    1,
    null,
    '{"moduleCode":"PURCHASE_APPLY","moduleName":"采购申请","goal":"完成采购申请创建、填写、提交和审批确认。"}'::jsonb,
    null,
    null,
    '[
        {"stepCode":"PURCHASE_CREATE","stepName":"创建采购申请","selector":"[data-action=\"create\"]","score":20},
        {"stepCode":"PURCHASE_FILL","stepName":"填写基础信息","selector":"[data-action=\"fill\"]","score":25},
        {"stepCode":"PURCHASE_SUBMIT","stepName":"提交采购申请","selector":"[data-action=\"submit\"]","score":25},
        {"stepCode":"PURCHASE_APPROVE","stepName":"审批通过","selector":"[data-action=\"approve\"]","score":20},
        {"stepCode":"PURCHASE_RESULT","stepName":"查看办理结果","selector":"[data-action=\"result\"]","score":10}
    ]'::jsonb,
    'creator',
    '申请人',
    'ORDERED_STEPS',
    null,
    null,
    '{"readonly":false,"lockOnExam":true,"targetUrl":"/business-sdk-demo.html"}'::jsonb,
    '{"learning":{"showTeachingText":true,"showOverlayHint":true},"practice":{"enforceOrder":true},"exam":{"silentRecord":true}}'::jsonb,
    '基于原平台采购申请页面的标准实操教学点。',
    'seed', now(), 'seed', now(), 'PUBLISHED', 'ACTIVE', false
)
ON CONFLICT (id)
DO UPDATE SET
    point_name = EXCLUDED.point_name,
    business_overview_json = EXCLUDED.business_overview_json,
    record_path_json = EXCLUDED.record_path_json,
    required_external_role_id = EXCLUDED.required_external_role_id,
    required_external_role_name = EXCLUDED.required_external_role_name,
    execution_strategy = EXCLUDED.execution_strategy,
    data_scope_json = EXCLUDED.data_scope_json,
    overlay_policy_json = EXCLUDED.overlay_policy_json,
    description = EXCLUDED.description,
    point_status = EXCLUDED.point_status,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE',
    deleted = false;

INSERT INTO course (
    id, tenant_id, course_code, course_name, version_no, target_org_id,
    start_time, end_time, description,
    create_by, create_time, update_by, update_time, course_status, status, deleted
) VALUES (
    'course-purchase-demo',
    'demo-tenant',
    'COURSE_PURCHASE_DEMO',
    '采购业务数字实训',
    1,
    'org-demo-class-a',
    now(),
    now() + interval '180 days',
    '用于演示原平台采购申请业务的数据准备、练习和考试闭环。',
    'seed', now(), 'seed', now(), 'PUBLISHED', 'ACTIVE', false
)
ON CONFLICT (id)
DO UPDATE SET
    course_name = EXCLUDED.course_name,
    target_org_id = EXCLUDED.target_org_id,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    description = EXCLUDED.description,
    course_status = EXCLUDED.course_status,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE',
    deleted = false;

INSERT INTO task (
    id, tenant_id, course_id, publish_org_id, task_code, task_name, version_no,
    task_type, task_goal, task_description, start_time, end_time,
    time_limit_minutes, overlay_policy_json,
    create_by, create_time, update_by, update_time, task_status, status, deleted
) VALUES
    ('task-purchase-practice-demo', 'demo-tenant', 'course-purchase-demo', 'org-demo-class-a', 'TASK_PURCHASE_PRACTICE', '采购申请流程练习', 1,
     'PRACTICE', '学生独立完成采购申请创建、填写和提交。', '练习场景按 attempt 独占生成一份原平台业务数据。', now(), now() + interval '90 days',
     45, '{"modePolicies":{"practice":{"enforceOrder":true,"showOverlayHint":true}}}'::jsonb,
     'seed', now(), 'seed', now(), 'PUBLISHED', 'ACTIVE', false),
    ('task-purchase-exam-demo', 'demo-tenant', 'course-purchase-demo', 'org-demo-class-a', 'TASK_PURCHASE_EXAM', '采购申请流程考试', 1,
     'EXAM', '学生在静默采集模式下完成采购申请关键办理动作。', '考试场景每生每题独占并锁定一份原平台业务数据。', now(), now() + interval '90 days',
     60, '{"modePolicies":{"exam":{"silentRecord":true,"showSubmission":true}}}'::jsonb,
     'seed', now(), 'seed', now(), 'PUBLISHED', 'ACTIVE', false)
ON CONFLICT (id)
DO UPDATE SET
    task_name = EXCLUDED.task_name,
    task_type = EXCLUDED.task_type,
    task_goal = EXCLUDED.task_goal,
    task_description = EXCLUDED.task_description,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    time_limit_minutes = EXCLUDED.time_limit_minutes,
    overlay_policy_json = EXCLUDED.overlay_policy_json,
    task_status = EXCLUDED.task_status,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE',
    deleted = false;

INSERT INTO task_teaching_point (
    id, tenant_id, task_id, teaching_point_id, required_flag, sequence_no,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('ttp-purchase-practice-demo', 'demo-tenant', 'task-purchase-practice-demo', 'tp-purchase-apply-v1', true, 1, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('ttp-purchase-exam-demo', 'demo-tenant', 'task-purchase-exam-demo', 'tp-purchase-apply-v1', true, 1, 'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, task_id, teaching_point_id) WHERE deleted = false
DO UPDATE SET
    required_flag = EXCLUDED.required_flag,
    sequence_no = EXCLUDED.sequence_no,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO task_step (
    id, tenant_id, task_id, teaching_point_id, step_code, step_name, step_description,
    sequence_no, segment_no, actor_type, required_external_org_id, required_external_org_name,
    required_external_role_id, required_external_role_name, switch_strategy, next_segment_no,
    switch_confirm_required, switch_decision_source, switch_reason, rollback_policy,
    related_resource_ids, guide_content, practice_hint, required, allow_skip,
    source_action_draft_id, create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('step-purchase-practice-create', 'demo-tenant', 'task-purchase-practice-demo', 'tp-purchase-apply-v1', 'PURCHASE_CREATE', '创建采购申请', '在原平台中新建一张采购申请单。',
     1, 1, 'CREATOR', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人', null, null, false, null, null, 'RESTART_ONLY',
     '["res-purchase-create-button"]'::jsonb, '点击新建申请，进入采购申请创建状态。', '从页面顶部工具栏找到新建申请按钮。', true, false,
     null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('step-purchase-practice-fill', 'demo-tenant', 'task-purchase-practice-demo', 'tp-purchase-apply-v1', 'PURCHASE_FILL', '填写基础信息', '填写供应商、金额、品类和申请原因。',
     2, 1, 'CREATOR', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人', null, null, false, null, null, 'RESTART_ONLY',
     '["res-purchase-form"]'::jsonb, '录入采购申请的基础业务信息。', '确认供应商、金额、品类和原因均已填写。', true, false,
     null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('step-purchase-practice-submit', 'demo-tenant', 'task-purchase-practice-demo', 'tp-purchase-apply-v1', 'PURCHASE_SUBMIT', '提交采购申请', '确认信息无误后提交采购申请。',
     3, 1, 'CREATOR', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人', null, null, false, null, null, 'RESTART_ONLY',
     '["res-purchase-submit-button"]'::jsonb, '提交后原平台应生成稳定业务单号。', '检查表单后提交采购申请。', true, false,
     null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('step-purchase-practice-result', 'demo-tenant', 'task-purchase-practice-demo', 'tp-purchase-apply-v1', 'PURCHASE_RESULT', '查看办理结果', '查看采购申请状态并提交业务单号。',
     4, 1, 'CREATOR', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人', null, null, false, null, null, 'RESTART_ONLY',
     '["res-purchase-submit-button"]'::jsonb, '确认业务单据已提交。', '记录采购申请业务单号。', false, true,
     null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('step-purchase-exam-create', 'demo-tenant', 'task-purchase-exam-demo', 'tp-purchase-apply-v1', 'PURCHASE_CREATE', '创建采购申请', '在考试数据上创建采购申请。',
     1, 1, 'CREATOR', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人', null, null, false, null, null, 'FORBIDDEN',
     '["res-purchase-create-button"]'::jsonb, null, null, true, false,
     null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('step-purchase-exam-fill', 'demo-tenant', 'task-purchase-exam-demo', 'tp-purchase-apply-v1', 'PURCHASE_FILL', '填写基础信息', '填写考试采购申请信息。',
     2, 1, 'CREATOR', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人', null, null, false, null, null, 'FORBIDDEN',
     '["res-purchase-form"]'::jsonb, null, null, true, false,
     null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('step-purchase-exam-submit', 'demo-tenant', 'task-purchase-exam-demo', 'tp-purchase-apply-v1', 'PURCHASE_SUBMIT', '提交采购申请', '提交考试采购申请。',
     3, 1, 'CREATOR', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人', null, null, false, null, null, 'FORBIDDEN',
     '["res-purchase-submit-button"]'::jsonb, null, null, true, false,
     null, 'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (id)
DO UPDATE SET
    step_name = EXCLUDED.step_name,
    step_description = EXCLUDED.step_description,
    sequence_no = EXCLUDED.sequence_no,
    segment_no = EXCLUDED.segment_no,
    actor_type = EXCLUDED.actor_type,
    required_external_org_id = EXCLUDED.required_external_org_id,
    required_external_org_name = EXCLUDED.required_external_org_name,
    required_external_role_id = EXCLUDED.required_external_role_id,
    required_external_role_name = EXCLUDED.required_external_role_name,
    rollback_policy = EXCLUDED.rollback_policy,
    related_resource_ids = EXCLUDED.related_resource_ids,
    guide_content = EXCLUDED.guide_content,
    practice_hint = EXCLUDED.practice_hint,
    required = EXCLUDED.required,
    allow_skip = EXCLUDED.allow_skip,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE',
    deleted = false;

INSERT INTO evaluation_rule (
    id, tenant_id, rule_code, rule_name, version_no, task_id, teaching_point_id,
    total_score, description,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('eval-rule-purchase-practice-v1', 'demo-tenant', 'EVAL_PURCHASE_PRACTICE', '采购申请练习评分规则', 1, 'task-purchase-practice-demo', 'tp-purchase-apply-v1',
     90.00, '按创建、填写、提交和结果确认动作计算练习过程分。', 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('eval-rule-purchase-exam-v1', 'demo-tenant', 'EVAL_PURCHASE_EXAM', '采购申请考试评分规则', 1, 'task-purchase-exam-demo', 'tp-purchase-apply-v1',
     100.00, '按考试静默采集证据和原平台结果校验计算得分。', 'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, rule_code, version_no) WHERE deleted = false
DO UPDATE SET
    rule_name = EXCLUDED.rule_name,
    task_id = EXCLUDED.task_id,
    teaching_point_id = EXCLUDED.teaching_point_id,
    total_score = EXCLUDED.total_score,
    description = EXCLUDED.description,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO evaluation_item (
    id, tenant_id, evaluation_rule_id, teaching_point_id, item_code, item_name,
    item_type, related_resource_id, related_api_resource_id, related_task_step_id,
    score, required, assertion_type, assertion_config_json, fail_policy,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('eval-item-practice-create', 'demo-tenant', 'eval-rule-purchase-practice-v1', 'tp-purchase-apply-v1', 'PURCHASE_CREATE', '创建采购申请', 'KEY_ACTION',
     'res-purchase-create-button', null, 'step-purchase-practice-create', 20.00, true, 'TRACE_EXISTS',
     '{"traceType":"CLICK","selector":"[data-action=\"create\"]","expectedText":"新建申请"}'::jsonb, 'NO_SCORE',
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('eval-item-practice-fill', 'demo-tenant', 'eval-rule-purchase-practice-v1', 'tp-purchase-apply-v1', 'PURCHASE_FILL', '填写基础信息', 'KEY_ACTION',
     'res-purchase-form', null, 'step-purchase-practice-fill', 25.00, true, 'FIELD_EQUALS',
     '{"requiredFields":["supplierName","amount","category","reason"],"masked":true}'::jsonb, 'NO_SCORE',
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('eval-item-practice-submit', 'demo-tenant', 'eval-rule-purchase-practice-v1', 'tp-purchase-apply-v1', 'PURCHASE_SUBMIT', '提交采购申请', 'KEY_ACTION',
     'res-purchase-submit-button', null, 'step-purchase-practice-submit', 25.00, true, 'TRACE_EXISTS',
     '{"traceType":"SUBMIT","selector":"[data-action=\"submit\"]"}'::jsonb, 'NO_SCORE',
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('eval-item-practice-result', 'demo-tenant', 'eval-rule-purchase-practice-v1', 'tp-purchase-apply-v1', 'PURCHASE_RESULT', '结果状态正确', 'RESULT',
     null, null, 'step-purchase-practice-result', 20.00, true, 'EXTERNAL_API_RESULT',
     '{"capabilityCode":"RESULT_CHECK","expectedStatus":"SUBMITTED"}'::jsonb, 'REVIEW',
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('eval-item-exam-create', 'demo-tenant', 'eval-rule-purchase-exam-v1', 'tp-purchase-apply-v1', 'PURCHASE_CREATE', '创建采购申请', 'KEY_ACTION',
     'res-purchase-create-button', null, 'step-purchase-exam-create', 20.00, true, 'TRACE_EXISTS',
     '{"traceType":"CLICK","selector":"[data-action=\"create\"]"}'::jsonb, 'NO_SCORE',
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('eval-item-exam-fill', 'demo-tenant', 'eval-rule-purchase-exam-v1', 'tp-purchase-apply-v1', 'PURCHASE_FILL', '填写基础信息', 'KEY_ACTION',
     'res-purchase-form', null, 'step-purchase-exam-fill', 30.00, true, 'FIELD_EQUALS',
     '{"requiredFields":["supplierName","amount","category","reason"],"masked":true}'::jsonb, 'NO_SCORE',
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('eval-item-exam-submit', 'demo-tenant', 'eval-rule-purchase-exam-v1', 'tp-purchase-apply-v1', 'PURCHASE_SUBMIT', '提交采购申请', 'KEY_ACTION',
     'res-purchase-submit-button', null, 'step-purchase-exam-submit', 30.00, true, 'TRACE_EXISTS',
     '{"traceType":"SUBMIT","selector":"[data-action=\"submit\"]"}'::jsonb, 'NO_SCORE',
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('eval-item-exam-result', 'demo-tenant', 'eval-rule-purchase-exam-v1', 'tp-purchase-apply-v1', 'PURCHASE_RESULT', '原平台结果校验通过', 'RESULT',
     null, null, null, 20.00, true, 'EXTERNAL_API_RESULT',
     '{"capabilityCode":"RESULT_CHECK","expectedStatus":"SUBMITTED"}'::jsonb, 'REVIEW',
     'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, evaluation_rule_id, item_code) WHERE deleted = false
DO UPDATE SET
    item_name = EXCLUDED.item_name,
    item_type = EXCLUDED.item_type,
    related_resource_id = EXCLUDED.related_resource_id,
    related_api_resource_id = EXCLUDED.related_api_resource_id,
    related_task_step_id = EXCLUDED.related_task_step_id,
    score = EXCLUDED.score,
    required = EXCLUDED.required,
    assertion_type = EXCLUDED.assertion_type,
    assertion_config_json = EXCLUDED.assertion_config_json,
    fail_policy = EXCLUDED.fail_policy,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO lesson_plan (
    id, tenant_id, connector_system_id, teaching_point_id, module_code, plan_code,
    plan_name, plan_goal, business_summary, version_no, publish_status,
    source_capture_session_id, metadata_json,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES (
    'lesson-plan-purchase-v1',
    'demo-tenant',
    'origin-system-purchase-demo',
    'tp-purchase-apply-v1',
    'PURCHASE_APPLY',
    'LESSON_PURCHASE_APPLY_V1',
    '采购申请标准实训教案',
    '掌握采购申请从创建到提交的业务办理流程，并理解审批节点的结果校验要求。',
    '原型流程包含创建申请、填写基础信息、提交申请、审批通过、查看结果五个节点。',
    1,
    'PUBLISHED',
    null,
    '{"source":"sxpt_origin_system","templateId":"template-purchase","lessonFlowId":"lesson-purchase"}'::jsonb,
    'seed', now(), 'seed', now(), 'ACTIVE', false
)
ON CONFLICT (tenant_id, connector_system_id, plan_code, version_no) WHERE deleted = false
DO UPDATE SET
    plan_name = EXCLUDED.plan_name,
    plan_goal = EXCLUDED.plan_goal,
    business_summary = EXCLUDED.business_summary,
    publish_status = EXCLUDED.publish_status,
    metadata_json = EXCLUDED.metadata_json,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO operation_flow_path (
    id, tenant_id, lesson_plan_id, path_code, path_name, path_type, module_code,
    step_snapshot_json, initial_data_state, target_data_state, difficulty,
    publish_status, create_by, create_time, update_by, update_time, status, deleted
) VALUES (
    'flow-path-purchase-normal-v1',
    'demo-tenant',
    'lesson-plan-purchase-v1',
    'PURCHASE_NORMAL',
    '采购申请标准流程',
    'NORMAL',
    'PURCHASE_APPLY',
    '[
        {"sequenceNo":1,"stepCode":"PURCHASE_CREATE","stepName":"创建采购申请","actorType":"CREATOR","roleId":"creator","score":20},
        {"sequenceNo":2,"stepCode":"PURCHASE_FILL","stepName":"填写基础信息","actorType":"CREATOR","roleId":"creator","score":30},
        {"sequenceNo":3,"stepCode":"PURCHASE_SUBMIT","stepName":"提交采购申请","actorType":"CREATOR","roleId":"creator","score":30},
        {"sequenceNo":4,"stepCode":"PURCHASE_RESULT","stepName":"查看办理结果","actorType":"CREATOR","roleId":"creator","score":20}
    ]'::jsonb,
    'DRAFT',
    'SUBMITTED',
    'NORMAL',
    'PUBLISHED',
    'seed', now(), 'seed', now(), 'ACTIVE', false
)
ON CONFLICT (tenant_id, lesson_plan_id, path_code) WHERE deleted = false
DO UPDATE SET
    path_name = EXCLUDED.path_name,
    path_type = EXCLUDED.path_type,
    step_snapshot_json = EXCLUDED.step_snapshot_json,
    initial_data_state = EXCLUDED.initial_data_state,
    target_data_state = EXCLUDED.target_data_state,
    difficulty = EXCLUDED.difficulty,
    publish_status = EXCLUDED.publish_status,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO operation_question (
    id, tenant_id, lesson_plan_id, flow_path_id, connector_system_id, module_code,
    question_code, question_name, question_type, difficulty, data_template_id,
    evaluation_rule_id, reversible_policy, question_config_json, publish_status,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('question-purchase-practice-v1', 'demo-tenant', 'lesson-plan-purchase-v1', 'flow-path-purchase-normal-v1', 'origin-system-purchase-demo', 'PURCHASE_APPLY',
     'QUESTION_PURCHASE_PRACTICE_V1', '采购申请流程练习题', 'OPERATION', 'NORMAL', 'tdt-purchase-practice-v1',
     'eval-rule-purchase-practice-v1', 'PARTIAL',
     '{"sceneType":"PRACTICE","targetUrl":"/business-sdk-demo.html","objectiveMaxScore":90,"subjectiveMaxScore":10}'::jsonb, 'PUBLISHED',
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('question-purchase-exam-v1', 'demo-tenant', 'lesson-plan-purchase-v1', 'flow-path-purchase-normal-v1', 'origin-system-purchase-demo', 'PURCHASE_APPLY',
     'QUESTION_PURCHASE_EXAM_V1', '采购申请流程考试题', 'OPERATION', 'NORMAL', 'tdt-purchase-exam-v1',
     'eval-rule-purchase-exam-v1', 'IRREVERSIBLE',
     '{"sceneType":"EXAM","targetUrl":"/business-sdk-demo.html","objectiveMaxScore":100,"subjectiveMaxScore":0}'::jsonb, 'PUBLISHED',
     'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, connector_system_id, question_code) WHERE deleted = false
DO UPDATE SET
    question_name = EXCLUDED.question_name,
    question_type = EXCLUDED.question_type,
    difficulty = EXCLUDED.difficulty,
    data_template_id = EXCLUDED.data_template_id,
    evaluation_rule_id = EXCLUDED.evaluation_rule_id,
    reversible_policy = EXCLUDED.reversible_policy,
    question_config_json = EXCLUDED.question_config_json,
    publish_status = EXCLUDED.publish_status,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO exam_paper (
    id, tenant_id, course_id, task_id, paper_code, paper_name, total_score,
    time_limit_minutes, paper_strategy_json, publish_status,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES (
    'paper-purchase-exam-v1',
    'demo-tenant',
    'course-purchase-demo',
    'task-purchase-exam-demo',
    'PAPER_PURCHASE_EXAM_V1',
    '采购申请流程考试试卷',
    100.00,
    60,
    '{"questionOrder":"FIXED","dataPrepare":"BEFORE_START","lockPolicy":"ON_EXAM_START"}'::jsonb,
    'PUBLISHED',
    'seed', now(), 'seed', now(), 'ACTIVE', false
)
ON CONFLICT (tenant_id, paper_code) WHERE deleted = false
DO UPDATE SET
    paper_name = EXCLUDED.paper_name,
    total_score = EXCLUDED.total_score,
    time_limit_minutes = EXCLUDED.time_limit_minutes,
    paper_strategy_json = EXCLUDED.paper_strategy_json,
    publish_status = EXCLUDED.publish_status,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO exam_paper_question (
    id, tenant_id, paper_id, question_id, sequence_no, score, required,
    question_snapshot_json, create_by, create_time, update_by, update_time, status, deleted
) VALUES (
    'paper-question-purchase-exam-v1',
    'demo-tenant',
    'paper-purchase-exam-v1',
    'question-purchase-exam-v1',
    1,
    100.00,
    true,
    '{"questionCode":"QUESTION_PURCHASE_EXAM_V1","questionName":"采购申请流程考试题","moduleCode":"PURCHASE_APPLY","dataTemplateId":"tdt-purchase-exam-v1","evaluationRuleId":"eval-rule-purchase-exam-v1"}'::jsonb,
    'seed', now(), 'seed', now(), 'ACTIVE', false
)
ON CONFLICT (tenant_id, paper_id, sequence_no) WHERE deleted = false
DO UPDATE SET
    question_id = EXCLUDED.question_id,
    score = EXCLUDED.score,
    required = EXCLUDED.required,
    question_snapshot_json = EXCLUDED.question_snapshot_json,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO data_requirement (
    id, tenant_id, requirement_code, connector_system_id, business_module_id,
    module_code, strategy_id, template_id, task_id, class_id, scene_type,
    requirement_status, expected_count, success_count, failed_count,
    requirement_policy_json, remark, lock_version,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('dr-purchase-practice-20260728', 'demo-tenant', 'DR_PURCHASE_PRACTICE_20260728', 'origin-system-purchase-demo', 'bm-origin-purchase-apply',
     'PURCHASE_APPLY', 'mds-purchase-practice-v1', 'tdt-purchase-practice-v1', 'task-purchase-practice-demo', 'org-demo-class-a', 'PRACTICE',
     'READY', 3, 3, 0,
     '{"sharePolicy":"ATTEMPT_EXCLUSIVE","regeneratePolicy":"ON_ATTEMPT","source":"sxpt_origin_system"}'::jsonb,
     '为采购申请练习任务预置 3 份可领取数据。', 0,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dr-purchase-exam-20260728', 'demo-tenant', 'DR_PURCHASE_EXAM_20260728', 'origin-system-purchase-demo', 'bm-origin-purchase-apply',
     'PURCHASE_APPLY', 'mds-purchase-exam-v1', 'tdt-purchase-exam-v1', 'task-purchase-exam-demo', 'org-demo-class-a', 'EXAM',
     'READY', 3, 3, 0,
     '{"sharePolicy":"QUESTION_EXCLUSIVE","lockPolicy":"ON_EXAM_START","source":"sxpt_origin_system"}'::jsonb,
     '为采购申请考试任务预置每生每题独占数据。', 0,
     'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, requirement_code) WHERE deleted = false
DO UPDATE SET
    connector_system_id = EXCLUDED.connector_system_id,
    business_module_id = EXCLUDED.business_module_id,
    module_code = EXCLUDED.module_code,
    strategy_id = EXCLUDED.strategy_id,
    template_id = EXCLUDED.template_id,
    task_id = EXCLUDED.task_id,
    class_id = EXCLUDED.class_id,
    scene_type = EXCLUDED.scene_type,
    requirement_status = EXCLUDED.requirement_status,
    expected_count = EXCLUDED.expected_count,
    success_count = EXCLUDED.success_count,
    failed_count = EXCLUDED.failed_count,
    requirement_policy_json = EXCLUDED.requirement_policy_json,
    remark = EXCLUDED.remark,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO teaching_data_pool (
    id, tenant_id, connector_system_id, module_code, task_id, class_id,
    scene_type, question_id, template_id, pool_status, total_count,
    ready_count, allocated_count, failed_count, pool_policy_json,
    strategy_id, requirement_id, idempotency_key, lock_version,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('pool-purchase-practice-20260728', 'demo-tenant', 'origin-system-purchase-demo', 'PURCHASE_APPLY', 'task-purchase-practice-demo', 'org-demo-class-a',
     'PRACTICE', 'question-purchase-practice-v1', 'tdt-purchase-practice-v1', 'READY', 3, 3, 0, 0,
     '{"formalCount":3,"spareCount":0,"allocation":"ATTEMPT_EXCLUSIVE"}'::jsonb,
     'mds-purchase-practice-v1', 'dr-purchase-practice-20260728', 'seed:purchase:practice:20260728', 0,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('pool-purchase-exam-20260728', 'demo-tenant', 'origin-system-purchase-demo', 'PURCHASE_APPLY', 'task-purchase-exam-demo', 'org-demo-class-a',
     'EXAM', 'question-purchase-exam-v1', 'tdt-purchase-exam-v1', 'READY', 3, 3, 0, 0,
     '{"formalCount":3,"spareCount":0,"allocation":"QUESTION_EXCLUSIVE","lockPolicy":"ON_EXAM_START"}'::jsonb,
     'mds-purchase-exam-v1', 'dr-purchase-exam-20260728', 'seed:purchase:exam:20260728', 0,
     'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (id)
DO UPDATE SET
    pool_status = EXCLUDED.pool_status,
    total_count = EXCLUDED.total_count,
    ready_count = EXCLUDED.ready_count,
    allocated_count = EXCLUDED.allocated_count,
    failed_count = EXCLUDED.failed_count,
    pool_policy_json = EXCLUDED.pool_policy_json,
    strategy_id = EXCLUDED.strategy_id,
    requirement_id = EXCLUDED.requirement_id,
    idempotency_key = EXCLUDED.idempotency_key,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE',
    deleted = false;

INSERT INTO data_prepare_job (
    id, tenant_id, connector_system_id, pool_id, task_id, module_code,
    scene_type, job_type, expected_count, success_count, failed_count,
    job_status, request_json, result_json, error_message, start_time, end_time,
    idempotency_key, external_request_id, request_batch_id, retry_count,
    next_retry_time, trigger_type, trace_id,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('job-purchase-practice-20260728', 'demo-tenant', 'origin-system-purchase-demo', 'pool-purchase-practice-20260728', 'task-purchase-practice-demo', 'PURCHASE_APPLY',
     'PRACTICE', 'CREATE', 3, 3, 0, 'SUCCESS',
     '{"requestBatchId":"batch-purchase-practice-20260728","templateId":"tdt-purchase-practice-v1"}'::jsonb,
     '{"batchId":"batch-purchase-practice-20260728","total":3,"successCount":3,"failedCount":0}'::jsonb,
     null, now(), now(),
     'seed:job:purchase:practice:20260728', 'origin-request-practice-20260728', 'batch-purchase-practice-20260728', 0,
     null, 'MANUAL', 'trace-seed-purchase-practice-20260728',
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('job-purchase-exam-20260728', 'demo-tenant', 'origin-system-purchase-demo', 'pool-purchase-exam-20260728', 'task-purchase-exam-demo', 'PURCHASE_APPLY',
     'EXAM', 'CREATE', 3, 3, 0, 'SUCCESS',
     '{"requestBatchId":"batch-purchase-exam-20260728","templateId":"tdt-purchase-exam-v1"}'::jsonb,
     '{"batchId":"batch-purchase-exam-20260728","total":3,"successCount":3,"failedCount":0}'::jsonb,
     null, now(), now(),
     'seed:job:purchase:exam:20260728', 'origin-request-exam-20260728', 'batch-purchase-exam-20260728', 0,
     null, 'MANUAL', 'trace-seed-purchase-exam-20260728',
     'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, connector_system_id, idempotency_key) WHERE deleted = false AND idempotency_key IS NOT NULL
DO UPDATE SET
    pool_id = EXCLUDED.pool_id,
    task_id = EXCLUDED.task_id,
    module_code = EXCLUDED.module_code,
    scene_type = EXCLUDED.scene_type,
    job_type = EXCLUDED.job_type,
    expected_count = EXCLUDED.expected_count,
    success_count = EXCLUDED.success_count,
    failed_count = EXCLUDED.failed_count,
    job_status = EXCLUDED.job_status,
    request_json = EXCLUDED.request_json,
    result_json = EXCLUDED.result_json,
    error_message = EXCLUDED.error_message,
    start_time = EXCLUDED.start_time,
    end_time = EXCLUDED.end_time,
    external_request_id = EXCLUDED.external_request_id,
    request_batch_id = EXCLUDED.request_batch_id,
    retry_count = EXCLUDED.retry_count,
    trigger_type = EXCLUDED.trigger_type,
    trace_id = EXCLUDED.trace_id,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO data_requirement_item (
    id, tenant_id, requirement_id, request_batch_id, request_item_id,
    connector_system_id, business_module_id, module_code, template_id, scene_type,
    task_id, execution_id, student_id, question_id, exam_attempt_id,
    question_attempt_id, collaboration_unit_id, segment_no, actor_type,
    owner_external_org_id, owner_external_org_name, required_external_org_id,
    required_external_org_name, required_external_role_id, required_external_role_name,
    init_external_status, target_external_status, target_url, data_scope_json,
    required_actions_json, validation_policy_json, score_point_snapshot_json,
    item_status, external_business_id, external_business_no, external_status,
    validation_status, validation_time, validation_result_json, failure_reason,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('dri-purchase-practice-stu-01', 'demo-tenant', 'dr-purchase-practice-20260728', 'batch-purchase-practice-20260728', 'practice-student01-question-purchase',
     'origin-system-purchase-demo', 'bm-origin-purchase-apply', 'PURCHASE_APPLY', 'tdt-purchase-practice-v1', 'PRACTICE',
     'task-purchase-practice-demo', null, 'user-demo-student-01', 'question-purchase-practice-v1', null, null, null, 1, 'CREATOR',
     'origin-purchase-dept', '原平台采购申请单位', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人',
     'DRAFT', 'SUBMITTED', '/business-sdk-demo.html?bizId=origin-biz-practice-001',
     '{"readonly":false,"lock":false,"exclusive":true}'::jsonb,
     '["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT"]'::jsonb,
     '{"visible":true,"operable":true,"statusMatched":true}'::jsonb,
     '{"ruleId":"eval-rule-purchase-practice-v1","totalScore":90}'::jsonb,
     'READY', 'origin-biz-practice-001', 'CGSQ-P-20260728-001', 'DRAFT',
     'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true}'::jsonb, null,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dri-purchase-practice-stu-02', 'demo-tenant', 'dr-purchase-practice-20260728', 'batch-purchase-practice-20260728', 'practice-student02-question-purchase',
     'origin-system-purchase-demo', 'bm-origin-purchase-apply', 'PURCHASE_APPLY', 'tdt-purchase-practice-v1', 'PRACTICE',
     'task-purchase-practice-demo', null, 'user-demo-student-02', 'question-purchase-practice-v1', null, null, null, 1, 'CREATOR',
     'origin-purchase-dept', '原平台采购申请单位', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人',
     'DRAFT', 'SUBMITTED', '/business-sdk-demo.html?bizId=origin-biz-practice-002',
     '{"readonly":false,"lock":false,"exclusive":true}'::jsonb,
     '["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT"]'::jsonb,
     '{"visible":true,"operable":true,"statusMatched":true}'::jsonb,
     '{"ruleId":"eval-rule-purchase-practice-v1","totalScore":90}'::jsonb,
     'READY', 'origin-biz-practice-002', 'CGSQ-P-20260728-002', 'DRAFT',
     'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true}'::jsonb, null,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dri-purchase-practice-stu-03', 'demo-tenant', 'dr-purchase-practice-20260728', 'batch-purchase-practice-20260728', 'practice-student03-question-purchase',
     'origin-system-purchase-demo', 'bm-origin-purchase-apply', 'PURCHASE_APPLY', 'tdt-purchase-practice-v1', 'PRACTICE',
     'task-purchase-practice-demo', null, 'user-demo-student-03', 'question-purchase-practice-v1', null, null, null, 1, 'CREATOR',
     'origin-purchase-dept', '原平台采购申请单位', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人',
     'DRAFT', 'SUBMITTED', '/business-sdk-demo.html?bizId=origin-biz-practice-003',
     '{"readonly":false,"lock":false,"exclusive":true}'::jsonb,
     '["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT"]'::jsonb,
     '{"visible":true,"operable":true,"statusMatched":true}'::jsonb,
     '{"ruleId":"eval-rule-purchase-practice-v1","totalScore":90}'::jsonb,
     'READY', 'origin-biz-practice-003', 'CGSQ-P-20260728-003', 'DRAFT',
     'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true}'::jsonb, null,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dri-purchase-exam-stu-01', 'demo-tenant', 'dr-purchase-exam-20260728', 'batch-purchase-exam-20260728', 'exam-student01-question-purchase',
     'origin-system-purchase-demo', 'bm-origin-purchase-apply', 'PURCHASE_APPLY', 'tdt-purchase-exam-v1', 'EXAM',
     'task-purchase-exam-demo', null, 'user-demo-student-01', 'question-purchase-exam-v1', null, null, null, 1, 'CREATOR',
     'origin-purchase-dept', '原平台采购申请单位', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人',
     'DRAFT', 'SUBMITTED', '/business-sdk-demo.html?bizId=origin-biz-exam-001',
     '{"readonly":false,"lock":true,"exclusive":true}'::jsonb,
     '["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT"]'::jsonb,
     '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb,
     '{"ruleId":"eval-rule-purchase-exam-v1","totalScore":100}'::jsonb,
     'READY', 'origin-biz-exam-001', 'CGSQ-E-20260728-001', 'DRAFT',
     'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb, null,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dri-purchase-exam-stu-02', 'demo-tenant', 'dr-purchase-exam-20260728', 'batch-purchase-exam-20260728', 'exam-student02-question-purchase',
     'origin-system-purchase-demo', 'bm-origin-purchase-apply', 'PURCHASE_APPLY', 'tdt-purchase-exam-v1', 'EXAM',
     'task-purchase-exam-demo', null, 'user-demo-student-02', 'question-purchase-exam-v1', null, null, null, 1, 'CREATOR',
     'origin-purchase-dept', '原平台采购申请单位', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人',
     'DRAFT', 'SUBMITTED', '/business-sdk-demo.html?bizId=origin-biz-exam-002',
     '{"readonly":false,"lock":true,"exclusive":true}'::jsonb,
     '["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT"]'::jsonb,
     '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb,
     '{"ruleId":"eval-rule-purchase-exam-v1","totalScore":100}'::jsonb,
     'READY', 'origin-biz-exam-002', 'CGSQ-E-20260728-002', 'DRAFT',
     'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb, null,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('dri-purchase-exam-stu-03', 'demo-tenant', 'dr-purchase-exam-20260728', 'batch-purchase-exam-20260728', 'exam-student03-question-purchase',
     'origin-system-purchase-demo', 'bm-origin-purchase-apply', 'PURCHASE_APPLY', 'tdt-purchase-exam-v1', 'EXAM',
     'task-purchase-exam-demo', null, 'user-demo-student-03', 'question-purchase-exam-v1', null, null, null, 1, 'CREATOR',
     'origin-purchase-dept', '原平台采购申请单位', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人',
     'DRAFT', 'SUBMITTED', '/business-sdk-demo.html?bizId=origin-biz-exam-003',
     '{"readonly":false,"lock":true,"exclusive":true}'::jsonb,
     '["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT"]'::jsonb,
     '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb,
     '{"ruleId":"eval-rule-purchase-exam-v1","totalScore":100}'::jsonb,
     'READY', 'origin-biz-exam-003', 'CGSQ-E-20260728-003', 'DRAFT',
     'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb, null,
     'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, request_batch_id, request_item_id) WHERE deleted = false
DO UPDATE SET
    requirement_id = EXCLUDED.requirement_id,
    connector_system_id = EXCLUDED.connector_system_id,
    business_module_id = EXCLUDED.business_module_id,
    module_code = EXCLUDED.module_code,
    template_id = EXCLUDED.template_id,
    scene_type = EXCLUDED.scene_type,
    task_id = EXCLUDED.task_id,
    student_id = EXCLUDED.student_id,
    question_id = EXCLUDED.question_id,
    segment_no = EXCLUDED.segment_no,
    actor_type = EXCLUDED.actor_type,
    owner_external_org_id = EXCLUDED.owner_external_org_id,
    owner_external_org_name = EXCLUDED.owner_external_org_name,
    required_external_org_id = EXCLUDED.required_external_org_id,
    required_external_org_name = EXCLUDED.required_external_org_name,
    required_external_role_id = EXCLUDED.required_external_role_id,
    required_external_role_name = EXCLUDED.required_external_role_name,
    init_external_status = EXCLUDED.init_external_status,
    target_external_status = EXCLUDED.target_external_status,
    target_url = EXCLUDED.target_url,
    data_scope_json = EXCLUDED.data_scope_json,
    required_actions_json = EXCLUDED.required_actions_json,
    validation_policy_json = EXCLUDED.validation_policy_json,
    score_point_snapshot_json = EXCLUDED.score_point_snapshot_json,
    item_status = EXCLUDED.item_status,
    external_business_id = EXCLUDED.external_business_id,
    external_business_no = EXCLUDED.external_business_no,
    external_status = EXCLUDED.external_status,
    validation_status = EXCLUDED.validation_status,
    validation_time = EXCLUDED.validation_time,
    validation_result_json = EXCLUDED.validation_result_json,
    failure_reason = EXCLUDED.failure_reason,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO teaching_data_instance (
    id, tenant_id, template_id, connector_system_id, owner_user_id, class_id,
    task_id, teaching_point_id, execution_id, attempt_id, scene_type,
    external_business_id, external_business_no, external_status, instance_status,
    reset_count, lock_time, expire_time, metadata_json, pool_id, module_code,
    question_id, question_attempt_id, lock_reason, readonly_flag,
    allocation_status, archive_status, archive_time, requirement_id,
    requirement_item_id, prepare_job_id, request_batch_id, request_item_id,
    owner_external_org_id, owner_external_org_name, required_external_org_id,
    required_external_org_name, required_external_role_id, required_external_role_name,
    actor_type, target_url, requirement_snapshot_json, validation_status,
    validation_time, validation_result_json, failure_reason, lock_version,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('tdi-purchase-practice-stu-01', 'demo-tenant', 'tdt-purchase-practice-v1', 'origin-system-purchase-demo', 'user-demo-student-01', 'org-demo-class-a',
     'task-purchase-practice-demo', 'tp-purchase-apply-v1', null, null, 'PRACTICE',
     'origin-biz-practice-001', 'CGSQ-P-20260728-001', 'DRAFT', 'READY',
     0, null, now() + interval '7 days', '{"summary":"练习数据：陈同学采购申请"}'::jsonb, 'pool-purchase-practice-20260728', 'PURCHASE_APPLY',
     'question-purchase-practice-v1', null, null, false, 'READY', 'NONE', null,
     'dr-purchase-practice-20260728', 'dri-purchase-practice-stu-01', 'job-purchase-practice-20260728',
     'batch-purchase-practice-20260728', 'practice-student01-question-purchase',
     'origin-purchase-dept', '原平台采购申请单位', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人',
     'CREATOR', '/business-sdk-demo.html?bizId=origin-biz-practice-001',
     '{"requirementItemId":"dri-purchase-practice-stu-01","sceneType":"PRACTICE","initStatus":"DRAFT","targetStatus":"SUBMITTED"}'::jsonb,
     'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true}'::jsonb, null, 0,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('tdi-purchase-practice-stu-02', 'demo-tenant', 'tdt-purchase-practice-v1', 'origin-system-purchase-demo', 'user-demo-student-02', 'org-demo-class-a',
     'task-purchase-practice-demo', 'tp-purchase-apply-v1', null, null, 'PRACTICE',
     'origin-biz-practice-002', 'CGSQ-P-20260728-002', 'DRAFT', 'READY',
     0, null, now() + interval '7 days', '{"summary":"练习数据：周同学采购申请"}'::jsonb, 'pool-purchase-practice-20260728', 'PURCHASE_APPLY',
     'question-purchase-practice-v1', null, null, false, 'READY', 'NONE', null,
     'dr-purchase-practice-20260728', 'dri-purchase-practice-stu-02', 'job-purchase-practice-20260728',
     'batch-purchase-practice-20260728', 'practice-student02-question-purchase',
     'origin-purchase-dept', '原平台采购申请单位', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人',
     'CREATOR', '/business-sdk-demo.html?bizId=origin-biz-practice-002',
     '{"requirementItemId":"dri-purchase-practice-stu-02","sceneType":"PRACTICE","initStatus":"DRAFT","targetStatus":"SUBMITTED"}'::jsonb,
     'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true}'::jsonb, null, 0,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('tdi-purchase-practice-stu-03', 'demo-tenant', 'tdt-purchase-practice-v1', 'origin-system-purchase-demo', 'user-demo-student-03', 'org-demo-class-a',
     'task-purchase-practice-demo', 'tp-purchase-apply-v1', null, null, 'PRACTICE',
     'origin-biz-practice-003', 'CGSQ-P-20260728-003', 'DRAFT', 'READY',
     0, null, now() + interval '7 days', '{"summary":"练习数据：刘同学采购申请"}'::jsonb, 'pool-purchase-practice-20260728', 'PURCHASE_APPLY',
     'question-purchase-practice-v1', null, null, false, 'READY', 'NONE', null,
     'dr-purchase-practice-20260728', 'dri-purchase-practice-stu-03', 'job-purchase-practice-20260728',
     'batch-purchase-practice-20260728', 'practice-student03-question-purchase',
     'origin-purchase-dept', '原平台采购申请单位', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人',
     'CREATOR', '/business-sdk-demo.html?bizId=origin-biz-practice-003',
     '{"requirementItemId":"dri-purchase-practice-stu-03","sceneType":"PRACTICE","initStatus":"DRAFT","targetStatus":"SUBMITTED"}'::jsonb,
     'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true}'::jsonb, null, 0,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('tdi-purchase-exam-stu-01', 'demo-tenant', 'tdt-purchase-exam-v1', 'origin-system-purchase-demo', 'user-demo-student-01', 'org-demo-class-a',
     'task-purchase-exam-demo', 'tp-purchase-apply-v1', null, null, 'EXAM',
     'origin-biz-exam-001', 'CGSQ-E-20260728-001', 'DRAFT', 'LOCKED',
     0, now(), now() + interval '3 days', '{"summary":"考试数据：陈同学采购申请"}'::jsonb, 'pool-purchase-exam-20260728', 'PURCHASE_APPLY',
     'question-purchase-exam-v1', null, '考试开始前锁定', false, 'READY', 'NONE', null,
     'dr-purchase-exam-20260728', 'dri-purchase-exam-stu-01', 'job-purchase-exam-20260728',
     'batch-purchase-exam-20260728', 'exam-student01-question-purchase',
     'origin-purchase-dept', '原平台采购申请单位', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人',
     'CREATOR', '/business-sdk-demo.html?bizId=origin-biz-exam-001',
     '{"requirementItemId":"dri-purchase-exam-stu-01","sceneType":"EXAM","initStatus":"DRAFT","targetStatus":"SUBMITTED","lockPolicy":"ON_EXAM_START"}'::jsonb,
     'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb, null, 0,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('tdi-purchase-exam-stu-02', 'demo-tenant', 'tdt-purchase-exam-v1', 'origin-system-purchase-demo', 'user-demo-student-02', 'org-demo-class-a',
     'task-purchase-exam-demo', 'tp-purchase-apply-v1', null, null, 'EXAM',
     'origin-biz-exam-002', 'CGSQ-E-20260728-002', 'DRAFT', 'LOCKED',
     0, now(), now() + interval '3 days', '{"summary":"考试数据：周同学采购申请"}'::jsonb, 'pool-purchase-exam-20260728', 'PURCHASE_APPLY',
     'question-purchase-exam-v1', null, '考试开始前锁定', false, 'READY', 'NONE', null,
     'dr-purchase-exam-20260728', 'dri-purchase-exam-stu-02', 'job-purchase-exam-20260728',
     'batch-purchase-exam-20260728', 'exam-student02-question-purchase',
     'origin-purchase-dept', '原平台采购申请单位', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人',
     'CREATOR', '/business-sdk-demo.html?bizId=origin-biz-exam-002',
     '{"requirementItemId":"dri-purchase-exam-stu-02","sceneType":"EXAM","initStatus":"DRAFT","targetStatus":"SUBMITTED","lockPolicy":"ON_EXAM_START"}'::jsonb,
     'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb, null, 0,
     'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('tdi-purchase-exam-stu-03', 'demo-tenant', 'tdt-purchase-exam-v1', 'origin-system-purchase-demo', 'user-demo-student-03', 'org-demo-class-a',
     'task-purchase-exam-demo', 'tp-purchase-apply-v1', null, null, 'EXAM',
     'origin-biz-exam-003', 'CGSQ-E-20260728-003', 'DRAFT', 'LOCKED',
     0, now(), now() + interval '3 days', '{"summary":"考试数据：刘同学采购申请"}'::jsonb, 'pool-purchase-exam-20260728', 'PURCHASE_APPLY',
     'question-purchase-exam-v1', null, '考试开始前锁定', false, 'READY', 'NONE', null,
     'dr-purchase-exam-20260728', 'dri-purchase-exam-stu-03', 'job-purchase-exam-20260728',
     'batch-purchase-exam-20260728', 'exam-student03-question-purchase',
     'origin-purchase-dept', '原平台采购申请单位', 'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人',
     'CREATOR', '/business-sdk-demo.html?bizId=origin-biz-exam-003',
     '{"requirementItemId":"dri-purchase-exam-stu-03","sceneType":"EXAM","initStatus":"DRAFT","targetStatus":"SUBMITTED","lockPolicy":"ON_EXAM_START"}'::jsonb,
     'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb, null, 0,
     'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, connector_system_id, external_business_id) WHERE deleted = false
DO UPDATE SET
    template_id = EXCLUDED.template_id,
    owner_user_id = EXCLUDED.owner_user_id,
    class_id = EXCLUDED.class_id,
    task_id = EXCLUDED.task_id,
    teaching_point_id = EXCLUDED.teaching_point_id,
    scene_type = EXCLUDED.scene_type,
    external_business_no = EXCLUDED.external_business_no,
    external_status = EXCLUDED.external_status,
    instance_status = EXCLUDED.instance_status,
    lock_time = EXCLUDED.lock_time,
    expire_time = EXCLUDED.expire_time,
    metadata_json = EXCLUDED.metadata_json,
    pool_id = EXCLUDED.pool_id,
    module_code = EXCLUDED.module_code,
    question_id = EXCLUDED.question_id,
    lock_reason = EXCLUDED.lock_reason,
    readonly_flag = EXCLUDED.readonly_flag,
    allocation_status = EXCLUDED.allocation_status,
    archive_status = EXCLUDED.archive_status,
    requirement_id = EXCLUDED.requirement_id,
    requirement_item_id = EXCLUDED.requirement_item_id,
    prepare_job_id = EXCLUDED.prepare_job_id,
    request_batch_id = EXCLUDED.request_batch_id,
    request_item_id = EXCLUDED.request_item_id,
    owner_external_org_id = EXCLUDED.owner_external_org_id,
    owner_external_org_name = EXCLUDED.owner_external_org_name,
    required_external_org_id = EXCLUDED.required_external_org_id,
    required_external_org_name = EXCLUDED.required_external_org_name,
    required_external_role_id = EXCLUDED.required_external_role_id,
    required_external_role_name = EXCLUDED.required_external_role_name,
    actor_type = EXCLUDED.actor_type,
    target_url = EXCLUDED.target_url,
    requirement_snapshot_json = EXCLUDED.requirement_snapshot_json,
    validation_status = EXCLUDED.validation_status,
    validation_time = EXCLUDED.validation_time,
    validation_result_json = EXCLUDED.validation_result_json,
    failure_reason = EXCLUDED.failure_reason,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO data_instance_allocation (
    id, tenant_id, pool_id, data_instance_id, task_id, execution_id,
    attempt_id, question_attempt_id, owner_user_id, allocation_scene,
    allocation_status, allocate_time, release_time,
    required_external_org_id, required_external_org_name,
    required_external_role_id, required_external_role_name, actor_type,
    segment_no, collaboration_unit_id, requirement_snapshot_json,
    consume_time, release_reason,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    ('alloc-purchase-practice-stu-01', 'demo-tenant', 'pool-purchase-practice-20260728', 'tdi-purchase-practice-stu-01', 'task-purchase-practice-demo', null,
     null, null, 'user-demo-student-01', 'PRACTICE', 'ALLOCATED', now(), null,
     'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人', 'CREATOR',
     1, null, '{"requirementItemId":"dri-purchase-practice-stu-01","exclusive":true}'::jsonb,
     null, null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('alloc-purchase-practice-stu-02', 'demo-tenant', 'pool-purchase-practice-20260728', 'tdi-purchase-practice-stu-02', 'task-purchase-practice-demo', null,
     null, null, 'user-demo-student-02', 'PRACTICE', 'ALLOCATED', now(), null,
     'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人', 'CREATOR',
     1, null, '{"requirementItemId":"dri-purchase-practice-stu-02","exclusive":true}'::jsonb,
     null, null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('alloc-purchase-practice-stu-03', 'demo-tenant', 'pool-purchase-practice-20260728', 'tdi-purchase-practice-stu-03', 'task-purchase-practice-demo', null,
     null, null, 'user-demo-student-03', 'PRACTICE', 'ALLOCATED', now(), null,
     'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人', 'CREATOR',
     1, null, '{"requirementItemId":"dri-purchase-practice-stu-03","exclusive":true}'::jsonb,
     null, null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('alloc-purchase-exam-stu-01', 'demo-tenant', 'pool-purchase-exam-20260728', 'tdi-purchase-exam-stu-01', 'task-purchase-exam-demo', null,
     null, null, 'user-demo-student-01', 'EXAM', 'ALLOCATED', now(), null,
     'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人', 'CREATOR',
     1, null, '{"requirementItemId":"dri-purchase-exam-stu-01","exclusive":true,"locked":true}'::jsonb,
     null, null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('alloc-purchase-exam-stu-02', 'demo-tenant', 'pool-purchase-exam-20260728', 'tdi-purchase-exam-stu-02', 'task-purchase-exam-demo', null,
     null, null, 'user-demo-student-02', 'EXAM', 'ALLOCATED', now(), null,
     'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人', 'CREATOR',
     1, null, '{"requirementItemId":"dri-purchase-exam-stu-02","exclusive":true,"locked":true}'::jsonb,
     null, null, 'seed', now(), 'seed', now(), 'ACTIVE', false),
    ('alloc-purchase-exam-stu-03', 'demo-tenant', 'pool-purchase-exam-20260728', 'tdi-purchase-exam-stu-03', 'task-purchase-exam-demo', null,
     null, null, 'user-demo-student-03', 'EXAM', 'ALLOCATED', now(), null,
     'origin-purchase-dept', '原平台采购申请单位', 'creator', '申请人', 'CREATOR',
     1, null, '{"requirementItemId":"dri-purchase-exam-stu-03","exclusive":true,"locked":true}'::jsonb,
     null, null, 'seed', now(), 'seed', now(), 'ACTIVE', false)
ON CONFLICT (tenant_id, data_instance_id) WHERE deleted = false AND allocation_status = 'ALLOCATED'
DO UPDATE SET
    pool_id = EXCLUDED.pool_id,
    task_id = EXCLUDED.task_id,
    owner_user_id = EXCLUDED.owner_user_id,
    allocation_scene = EXCLUDED.allocation_scene,
    allocate_time = EXCLUDED.allocate_time,
    required_external_org_id = EXCLUDED.required_external_org_id,
    required_external_org_name = EXCLUDED.required_external_org_name,
    required_external_role_id = EXCLUDED.required_external_role_id,
    required_external_role_name = EXCLUDED.required_external_role_name,
    actor_type = EXCLUDED.actor_type,
    segment_no = EXCLUDED.segment_no,
    requirement_snapshot_json = EXCLUDED.requirement_snapshot_json,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

COMMIT;
