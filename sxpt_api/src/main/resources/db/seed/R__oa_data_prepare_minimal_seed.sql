-- OA 收文待登记数据准备配置脚本。
-- 业务边界：
-- 1. 只写入平台、能力、业务模块、数据模板、模块策略这些静态配置。
-- 2. 不写入教学任务、学生账号、数据实例或原平台业务明细，避免污染真实联调数据。
-- 3. 真实 OA 对接时可复用本脚本的字段结构，将 system_code、base_url、endpoint_url 替换为后台已创建的 OA 系统配置。

SET client_encoding = 'UTF8';

BEGIN;

INSERT INTO connector_system (
    id, tenant_id, system_code, system_name, system_type, base_url, auth_type, config_json,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES (
    'origin-oa-demo-system',
    'demo-tenant',
    'OA_DEMO',
    'OA 协同办公系统',
    'OA',
    'http://127.0.0.1:9527',
    'API_KEY',
    $${
        "sourceSystem": "oa",
        "embeddedMode": "iframe",
        "entryPath": "/workspace/incoming",
        "dataPrepareMode": "CALL_ORIGIN_API",
        "apiKey": "oa-data-center-incoming-token",
        "headerName": "X-API-Key",
        "apiKeyEnv": "DATA_CENTER_INCOMING_API_KEY",
        "maintainScope": "OA 收文待登记教学造数"
    }$$::jsonb,
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

INSERT INTO origin_org (
    id, tenant_id, connector_system_id, org_code, org_name, external_org_id,
    parent_external_org_id, org_type, remark,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    (
        'origin-org-oa-620000000000',
        'demo-tenant',
        'origin-oa-demo-system',
        '620000000000',
        '甘肃省本级',
        '620000000000',
        null,
        'PROVINCE',
        'OA 原平台省级单位',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'origin-org-oa-620100000000',
        'demo-tenant',
        'origin-oa-demo-system',
        '620100000000',
        '兰州市本级',
        '620100000000',
        '620000000000',
        'CITY',
        'OA 原平台市级单位',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'origin-org-oa-620102000000',
        'demo-tenant',
        'origin-oa-demo-system',
        '620102000000',
        '兰州市城关区',
        '620102000000',
        '620100000000',
        'DISTRICT',
        'OA 原平台区县单位',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'origin-org-oa-620102900000',
        'demo-tenant',
        'origin-oa-demo-system',
        '620102900000',
        '城关区业务单位',
        '620102900000',
        '620102000000',
        'DEPT',
        'OA 原平台末级业务单位',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    )
ON CONFLICT (tenant_id, connector_system_id, org_code) WHERE deleted = false
DO UPDATE SET
    org_name = EXCLUDED.org_name,
    external_org_id = EXCLUDED.external_org_id,
    parent_external_org_id = EXCLUDED.parent_external_org_id,
    org_type = EXCLUDED.org_type,
    remark = EXCLUDED.remark,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO origin_role (
    id, tenant_id, connector_system_id, role_code, role_name, external_role_id,
    role_type, remark,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    (
        'origin-role-oa-applicant',
        'demo-tenant',
        'origin-oa-demo-system',
        'OA_APPLICANT',
        'OA 申请人',
        'oa_applicant',
        'PROCESS_ROLE',
        'OA 流程发起人角色',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'origin-role-oa-approver',
        'demo-tenant',
        'origin-oa-demo-system',
        'OA_APPROVER',
        'OA 审批人',
        'oa_approver',
        'PROCESS_ROLE',
        'OA 流程审批人角色',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'origin-role-oa-add',
        'demo-tenant',
        'origin-oa-demo-system',
        'ADD',
        '新增',
        'add',
        'ACTION_ROLE',
        'OA 原平台新增动作角色',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'origin-role-oa-view',
        'demo-tenant',
        'origin-oa-demo-system',
        'VIEW',
        '查看',
        'view',
        'ACTION_ROLE',
        'OA 原平台查看动作角色',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'origin-role-oa-edit',
        'demo-tenant',
        'origin-oa-demo-system',
        'EDIT',
        '编辑',
        'edit',
        'ACTION_ROLE',
        'OA 原平台编辑动作角色',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'origin-role-oa-submit',
        'demo-tenant',
        'origin-oa-demo-system',
        'SUBMIT',
        '提交',
        'submit',
        'ACTION_ROLE',
        'OA 原平台提交动作角色',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'origin-role-oa-approve',
        'demo-tenant',
        'origin-oa-demo-system',
        'APPROVE',
        '审批',
        'approve',
        'ACTION_ROLE',
        'OA 原平台审批动作角色',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'origin-role-oa-archive',
        'demo-tenant',
        'origin-oa-demo-system',
        'ARCHIVE',
        '归档',
        'archive',
        'ACTION_ROLE',
        'OA 原平台归档动作角色',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    )
ON CONFLICT (tenant_id, connector_system_id, role_code) WHERE deleted = false
DO UPDATE SET
    role_name = EXCLUDED.role_name,
    external_role_id = EXCLUDED.external_role_id,
    role_type = EXCLUDED.role_type,
    remark = EXCLUDED.remark,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO platform_capability (
    id, tenant_id, connector_system_id, capability_code, capability_name, capability_type,
    support_flag, endpoint_url, method, request_schema_json, response_schema_json,
    timeout_ms, retry_policy_json,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    (
        'cap-oa-demo-data-create',
        'demo-tenant',
        'origin-oa-demo-system',
        'DATA_CREATE',
        '批量创建 OA 收文待登记数据',
        'DATA_CREATE',
        true,
        '/openapi/teaching-data/batch-create',
        'POST',
        $${
            "contract": "TEACHING_DATA_BATCH_CREATE_V1",
            "requiredHeaders": ["Authorization", "X-Trace-Id", "X-Idempotency-Key"],
            "required": ["businessModuleCode", "templateCode", "initState", "participants"],
            "participantFields": ["participantId", "ownerUserId", "externalUserId", "externalOrgId", "poolKey"],
            "businessModuleCode": "doc_incoming",
            "templateCode": "incoming_pending_reg_v1",
            "initState": "PENDING_REG",
            "defaultPoolKey": "incoming-default",
            "preferPoolKey": true,
            "forwardBizParams": false
        }$$::jsonb,
        $${
            "wrapper": "ApiResult",
            "required": ["originBatchId", "status", "totalCount", "successCount", "failedCount", "items"],
            "itemFields": ["participantId", "ownerUserId", "externalDataId", "externalBizNo", "externalStatus", "entryUrl", "externalUserId", "externalOrgId", "rawData"]
        }$$::jsonb,
        10000,
        '{"maxAttempts":3,"backoffMs":1000}'::jsonb,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'cap-oa-demo-data-validate',
        'demo-tenant',
        'origin-oa-demo-system',
        'DATA_VALIDATE',
        '旧 OA 审批数据校验（已停用）',
        'DATA_QUERY',
        false,
        '/openapi/teaching-data/oa/approvals/validate',
        'POST',
        $${
            "required": ["externalBusinessId", "expectedStatus", "requiredExternalOrgId", "requiredExternalRoleId"]
        }$$::jsonb,
        $${
            "required": ["visible", "operable", "statusMatched", "roleMatched"]
        }$$::jsonb,
        5000,
        '{"maxAttempts":2,"backoffMs":500}'::jsonb,
        'seed', now(), 'seed', now(), 'DISABLED', false
    ),
    (
        'cap-oa-demo-result-check',
        'demo-tenant',
        'origin-oa-demo-system',
        'RESULT_CHECK',
        '旧 OA 审批结果校验（已停用）',
        'RESULT_CHECK',
        false,
        '/openapi/teaching-data/oa/approvals/result-check',
        'POST',
        '{"required":["externalBusinessId","expectedStatus","expectedActions"]}'::jsonb,
        '{"required":["passed","externalStatus","matchedActions","matchedFields"]}'::jsonb,
        5000,
        '{"maxAttempts":2,"backoffMs":500}'::jsonb,
        'seed', now(), 'seed', now(), 'DISABLED', false
    )
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
    'bm-oa-incoming',
    'demo-tenant',
    'origin-oa-demo-system',
    'doc_incoming',
    '收文管理',
    'doc_incoming',
    '/workspace/incoming',
    'DOCUMENT_INCOMING',
    'PRACTICE,EXAM',
    true,
    'PENDING_REG',
    'REGISTERED',
    '["DATA_CREATE"]'::jsonb,
    'tdt-oa-incoming-pending-reg-v1',
    '通过 OA 标准 DATA_CREATE 接口生成收文待登记数据，供学生练习和考试使用。',
    0,
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
    lock_version = business_module.lock_version + 1,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO teaching_data_template (
    id, tenant_id, connector_system_id, teaching_point_id, template_code, template_name,
    scene_type, module_code, strategy_id, init_state, support_mode, config_json,
    request_schema_json, required_org_role_json, result_check_schema_json, sensitive_field_policy_json,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES (
    'tdt-oa-incoming-pending-reg-v1',
    'demo-tenant',
    'origin-oa-demo-system',
    null,
    'incoming_pending_reg_v1',
    'OA 收文待登记模板 V1',
    'PRACTICE',
    'doc_incoming',
    'mds-oa-incoming-practice-v1',
    'PENDING_REG',
    'PRACTICE,EXAM',
    $${
        "moduleCode": "doc_incoming",
        "templateVersion": 1,
        "bizParams": {},
        "fields": [
            {"name":"title","type":"string","required":true},
            {"name":"sourceOrgCode","type":"string","required":false},
            {"name":"sourceOrgName","type":"string","required":false},
            {"name":"docNumber","type":"string","required":false},
            {"name":"receiveDate","type":"date","required":false},
            {"name":"copies","type":"number","required":false},
            {"name":"urgency","type":"enum","required":false},
            {"name":"handleType","type":"enum","required":false},
            {"name":"remark","type":"string","required":false},
            {"name":"pdfPath","type":"string","required":false},
            {"name":"stationeryId","type":"string","required":false}
        ]
    }$$::jsonb,
    $${
        "contract": "TEACHING_DATA_BATCH_CREATE_V1",
        "required": ["businessModuleCode", "templateCode", "initState", "participants"],
        "businessModuleCode": "doc_incoming",
        "templateCode": "incoming_pending_reg_v1",
        "initState": "PENDING_REG"
    }$$::jsonb,
    $${
        "mappingModes": ["EXTERNAL_USER", "EXTERNAL_ORG", "POOL"],
        "defaultPoolKey": "incoming-default"
    }$$::jsonb,
    '{"createResponseIsValidationEvidence":true}'::jsonb,
    '{"forbidFields":["roleName","ownerUsername","studentName","phone","email"],"storeRawBusinessPayload":false}'::jsonb,
    'seed', now(), 'seed', now(), 'ACTIVE', false
)
ON CONFLICT (tenant_id, connector_system_id, template_code) WHERE deleted = false
DO UPDATE SET
    template_name = EXCLUDED.template_name,
    scene_type = EXCLUDED.scene_type,
    module_code = EXCLUDED.module_code,
    strategy_id = EXCLUDED.strategy_id,
    init_state = EXCLUDED.init_state,
    support_mode = EXCLUDED.support_mode,
    config_json = EXCLUDED.config_json,
    request_schema_json = EXCLUDED.request_schema_json,
    required_org_role_json = EXCLUDED.required_org_role_json,
    result_check_schema_json = EXCLUDED.result_check_schema_json,
    sensitive_field_policy_json = EXCLUDED.sensitive_field_policy_json,
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
) VALUES (
    'mds-oa-incoming-practice-v1',
    'demo-tenant',
    'origin-oa-demo-system',
    'doc_incoming',
    '收文管理',
    'PRACTICE',
    true,
    'PULL_ORIGIN',
    'PENDING_REG',
    'REGISTERED',
    $${
        "participantMapping": "POOL",
        "poolKey": "incoming-default"
    }$$::jsonb,
    'STUDENT_EXCLUSIVE',
    'ON_ATTEMPT',
    'NONE',
    '{"expireAfterHours":24}'::jsonb,
    '{"expectedStatus":"REGISTERED","requiredCapabilities":[]}'::jsonb,
    'bm-oa-incoming',
    'OA_INCOMING_PENDING_REG_PRACTICE_V1',
    'tdt-oa-incoming-pending-reg-v1',
    'ON_PUBLISH',
    '{"minReadyCount":1,"targetReadyCount":30}'::jsonb,
    '{"requiredCapabilities":["DATA_CREATE"],"validateAfterCreate":false,"failOnValidationError":false}'::jsonb,
    '{"archiveAfterDays":7,"archiveMode":"SOFT"}'::jsonb,
    1,
    0,
    'seed', now(), 'seed', now(), 'ACTIVE', false
)
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
    template_id = EXCLUDED.template_id,
    prepare_timing = EXCLUDED.prepare_timing,
    pool_size_policy_json = EXCLUDED.pool_size_policy_json,
    validation_policy_json = EXCLUDED.validation_policy_json,
    archive_policy_json = EXCLUDED.archive_policy_json,
    strategy_version = module_data_strategy.strategy_version + 1,
    lock_version = module_data_strategy.lock_version + 1,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

-- 旧版 OA_APPROVAL 样例与其策略不再用于批次准备，防止后台同时出现两套互不兼容的 OA 模块。
UPDATE business_module
SET status = 'DISABLED', update_by = 'seed', update_time = now()
WHERE tenant_id = 'demo-tenant'
  AND connector_system_id = 'origin-oa-demo-system'
  AND module_code = 'OA_APPROVAL'
  AND deleted = false;

UPDATE module_data_strategy
SET status = 'DISABLED', update_by = 'seed', update_time = now()
WHERE tenant_id = 'demo-tenant'
  AND connector_system_id = 'origin-oa-demo-system'
  AND module_code = 'OA_APPROVAL'
  AND deleted = false;

COMMIT;
