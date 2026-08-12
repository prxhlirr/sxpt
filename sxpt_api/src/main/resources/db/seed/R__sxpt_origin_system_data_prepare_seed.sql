-- sxpt_origin_system 数据准备主数据脚本
-- 目标数据库：PostgreSQL
-- 使用方式：
--   psql -h 127.0.0.1 -p 5432 -U postgres -d sxpt_dev -f sxpt_api/src/main/resources/db/seed/R__sxpt_origin_system_data_prepare_seed.sql
--
-- 数据来源：
--   1. sxpt_origin_system/local-business/business-demo.html
--      原平台页面为“采购申请业务系统”，包含采购申请、审批中心、供应商管理、统计报表导航。
--   2. sxpt_origin_system/src/mock/flowTemplates.ts
--      已发布的采购申请标准流程：新建申请 -> 填写基础信息 -> 提交申请 -> 审批通过 -> 查看结果。
--   3. sxpt_origin_system/src/mock/nodeDefs.ts
--      节点编码：PURCHASE_CREATE、PURCHASE_FILL、PURCHASE_SUBMIT、PURCHASE_APPROVE、PURCHASE_RESULT。
--   4. sxpt_origin_system/src/types/domain.ts
--      支持 learning、practice、exam 三类运行模式，以及 click、business_check、submission 等完成方式。
--
-- 设计边界：
--   本脚本只维护“平台接入、业务模块、模板管理、策略管理、批次准备”所需的最小闭环数据。
--   不写入完整教学任务、学生账号、数据实例分配等运行态数据，避免把原平台业务明细复制到教学平台。

SET client_encoding = 'UTF8';

BEGIN;

INSERT INTO connector_system (
    id, tenant_id, system_code, system_name, system_type, base_url, auth_type, config_json,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES (
    'origin-sxpt-purchase-system',
    'demo-tenant',
    'SXPT_ORIGIN_PURCHASE',
    '采购申请业务系统',
    'LOCAL_DEV',
    'http://127.0.0.1:5174/business-sdk-demo.html',
    'API_KEY',
    $${
        "apiKey": "sxpt-origin-purchase-api-key",
        "headerName": "X-API-Key",
        "sourceSystem": "sxpt_origin_system",
        "entryPath": "/business-sdk-demo.html",
        "embeddedMode": "iframe",
        "moduleEvidence": [
            "local-business/business-demo.html",
            "src/mock/flowTemplates.ts",
            "src/mock/nodeDefs.ts"
        ],
        "maintainScope": "仅维护原平台接入边界，业务模块、模板和策略在下级菜单维护"
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

INSERT INTO platform_capability (
    id, tenant_id, connector_system_id, capability_code, capability_name, capability_type,
    support_flag, endpoint_url, method, request_schema_json, response_schema_json,
    timeout_ms, retry_policy_json,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    (
        'cap-sxpt-purchase-data-create',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'DATA_CREATE',
        '批量创建采购申请教学数据',
        'DATA_CREATE',
        true,
        '/origin/openapi/purchase/applications/batch-create',
        'POST',
        $${
            "required": ["requestBatchId", "items"],
            "itemFields": ["requestItemId", "supplierName", "category", "amount", "requiredExternalOrgId", "requiredExternalRoleId", "initExternalStatus"]
        }$$::jsonb,
        $${
            "required": ["requestBatchId", "items"],
            "itemFields": ["requestItemId", "externalBusinessId", "externalBusinessNo", "externalStatus", "targetUrl"]
        }$$::jsonb,
        10000,
        '{"maxAttempts":3,"backoffMs":1000}'::jsonb,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'cap-sxpt-purchase-data-validate',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'DATA_VALIDATE',
        '校验采购申请数据可用性',
        'DATA_QUERY',
        false,
        '/origin/openapi/purchase/applications/validate',
        'POST',
        $${
            "required": ["externalBusinessId", "requiredExternalOrgId", "requiredExternalRoleId", "expectedStatus"]
        }$$::jsonb,
        $${
            "required": ["visible", "operable", "statusMatched", "roleMatched"]
        }$$::jsonb,
        5000,
        '{"maxAttempts":2,"backoffMs":500}'::jsonb,
        'seed', now(), 'seed', now(), 'DISABLED', false
    ),
    (
        'cap-sxpt-purchase-data-lock',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'DATA_LOCK',
        '锁定考试采购申请数据',
        'DATA_LOCK',
        false,
        '/origin/openapi/purchase/applications/lock',
        'POST',
        '{"required":["externalBusinessId","lockReason"]}'::jsonb,
        '{"required":["locked","externalStatus"]}'::jsonb,
        5000,
        '{"maxAttempts":2,"backoffMs":500}'::jsonb,
        'seed', now(), 'seed', now(), 'DISABLED', false
    ),
    (
        'cap-sxpt-purchase-result-check',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'RESULT_CHECK',
        '校验采购申请办理结果',
        'RESULT_CHECK',
        false,
        '/origin/openapi/purchase/applications/result-check',
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
    'bm-sxpt-purchase-apply',
    'demo-tenant',
    'origin-sxpt-purchase-system',
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
    'tdt-sxpt-purchase-practice-v1',
    '来自 sxpt_origin_system 的采购申请模块，页面包含供应商、采购品类、申请金额、期望到货日期和申请原因。',
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

INSERT INTO business_module_process_step (
    id, tenant_id, connector_system_id, business_module_id, module_code,
    step_no, step_code, step_name, step_type, init_external_status, target_external_status,
    completion_rule_json, remark, lock_version,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    (
        'bmps-sxpt-purchase-create',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'PURCHASE_APPLY',
        1,
        'PURCHASE_CREATE',
        '新建采购申请',
        'FILL',
        null,
        'DRAFT',
        '{"action":"create","expectedStatus":"DRAFT"}'::jsonb,
        '学生以采购申请人身份进入原平台，新建一条采购申请草稿。',
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'bmps-sxpt-purchase-fill',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'PURCHASE_APPLY',
        2,
        'PURCHASE_FILL',
        '填写基础信息',
        'FILL',
        'DRAFT',
        'DRAFT',
        '{"action":"save","requiredFields":["supplierName","category","amount","arrivalDate","reason"]}'::jsonb,
        '学生补充供应商、采购品类、金额、到货日期和申请原因。',
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'bmps-sxpt-purchase-submit',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'PURCHASE_APPLY',
        3,
        'PURCHASE_SUBMIT',
        '提交采购申请',
        'FILL',
        'DRAFT',
        'SUBMITTED',
        '{"action":"submit","expectedStatus":"SUBMITTED"}'::jsonb,
        '学生完成填报后提交采购申请，进入审批流。',
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'bmps-sxpt-purchase-approve',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'PURCHASE_APPLY',
        4,
        'PURCHASE_APPROVE',
        '部门审批与财务复核',
        'APPROVE',
        'SUBMITTED',
        'APPROVED',
        '{"action":"approve","expectedStatus":"APPROVED","needMultiActor":true}'::jsonb,
        '审批步骤包含部门负责人主审和财务复核，覆盖一个步骤多个参与方的实际场景。',
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'bmps-sxpt-purchase-result',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'PURCHASE_APPLY',
        5,
        'PURCHASE_RESULT',
        '查看办理结果',
        'ARCHIVE',
        'APPROVED',
        'APPROVED',
        '{"action":"viewResult","expectedStatus":"APPROVED"}'::jsonb,
        '学生或老师查看采购申请最终办理结果。',
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    )
ON CONFLICT (tenant_id, business_module_id, step_code) WHERE deleted = false
DO UPDATE SET
    step_no = EXCLUDED.step_no,
    step_name = EXCLUDED.step_name,
    step_type = EXCLUDED.step_type,
    init_external_status = EXCLUDED.init_external_status,
    target_external_status = EXCLUDED.target_external_status,
    completion_rule_json = EXCLUDED.completion_rule_json,
    remark = EXCLUDED.remark,
    lock_version = business_module_process_step.lock_version + 1,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO business_module_process_actor (
    id, tenant_id, connector_system_id, business_module_id, process_step_id, module_code, step_code,
    actor_no, actor_relation, actor_type, required_org_type, required_org_code, required_org_name,
    required_role_code, required_role_name, is_required, assignment_rule, remark, lock_version,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    (
        'bmpa-sxpt-purchase-create-primary',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'bmps-sxpt-purchase-create',
        'PURCHASE_APPLY',
        'PURCHASE_CREATE',
        1,
        'PRIMARY',
        'STUDENT',
        'APPLY_ORG',
        'origin-purchase-dept',
        '采购申请单位',
        'creator',
        '申请人',
        true,
        'CURRENT_STUDENT',
        '默认由当前学生扮演采购申请人。',
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'bmpa-sxpt-purchase-fill-primary',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'bmps-sxpt-purchase-fill',
        'PURCHASE_APPLY',
        'PURCHASE_FILL',
        1,
        'PRIMARY',
        'STUDENT',
        'APPLY_ORG',
        'origin-purchase-dept',
        '采购申请单位',
        'creator',
        '申请人',
        true,
        'CURRENT_STUDENT',
        '同一学生继续完成基础信息填写。',
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'bmpa-sxpt-purchase-submit-primary',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'bmps-sxpt-purchase-submit',
        'PURCHASE_APPLY',
        'PURCHASE_SUBMIT',
        1,
        'PRIMARY',
        'STUDENT',
        'APPLY_ORG',
        'origin-purchase-dept',
        '采购申请单位',
        'creator',
        '申请人',
        true,
        'CURRENT_STUDENT',
        '提交动作仍由采购申请人完成。',
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'bmpa-sxpt-purchase-approve-primary',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'bmps-sxpt-purchase-approve',
        'PURCHASE_APPLY',
        'PURCHASE_APPROVE',
        1,
        'APPROVER',
        'STUDENT',
        'APPROVE_ORG',
        'origin-approve-dept',
        '采购审批单位',
        'approver',
        '审批人',
        true,
        'GROUP_MEMBER',
        '多角色练习中可由小组成员扮演部门审批人。',
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'bmpa-sxpt-purchase-approve-reviewer',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'bmps-sxpt-purchase-approve',
        'PURCHASE_APPLY',
        'PURCHASE_APPROVE',
        2,
        'REVIEWER',
        'STUDENT',
        'FINANCE_ORG',
        'origin-finance-dept',
        '财务复核单位',
        'finance_reviewer',
        '财务复核人',
        true,
        'GROUP_MEMBER',
        '同一审批步骤下的第二参与方，用于验证多单位、多角色协同办理。',
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'bmpa-sxpt-purchase-result-primary',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'bmps-sxpt-purchase-result',
        'PURCHASE_APPLY',
        'PURCHASE_RESULT',
        1,
        'PRIMARY',
        'STUDENT',
        'APPLY_ORG',
        'origin-purchase-dept',
        '采购申请单位',
        'creator',
        '申请人',
        false,
        'CURRENT_STUDENT',
        '学生查看自己发起的采购申请最终结果。',
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    )
ON CONFLICT (tenant_id, process_step_id, actor_no) WHERE deleted = false
DO UPDATE SET
    actor_relation = EXCLUDED.actor_relation,
    actor_type = EXCLUDED.actor_type,
    required_org_type = EXCLUDED.required_org_type,
    required_org_code = EXCLUDED.required_org_code,
    required_org_name = EXCLUDED.required_org_name,
    required_role_code = EXCLUDED.required_role_code,
    required_role_name = EXCLUDED.required_role_name,
    is_required = EXCLUDED.is_required,
    assignment_rule = EXCLUDED.assignment_rule,
    remark = EXCLUDED.remark,
    lock_version = business_module_process_actor.lock_version + 1,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO teaching_data_template (
    id, tenant_id, connector_system_id, teaching_point_id, template_code, template_name,
    scene_type, module_code, strategy_id, init_state, support_mode, config_json,
    data_schema_json, mock_rule_json, readonly_flag, request_schema_json,
    required_org_role_json, result_check_schema_json, sensitive_field_policy_json,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    (
        'tdt-sxpt-purchase-record-v1',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        null,
        'PURCHASE_RECORD_TEMPLATE_V1',
        '采购申请备案模板',
        'RECORD',
        'PURCHASE_APPLY',
        null,
        'DRAFT',
        'RECORD',
        $${
            "purpose": "教师录制采购申请标准流程时使用的样例数据",
            "formFields": ["supplierName", "category", "amount", "arrivalDate", "reason"],
            "flowNodes": ["PURCHASE_CREATE", "PURCHASE_FILL", "PURCHASE_SUBMIT", "PURCHASE_APPROVE", "PURCHASE_RESULT"]
        }$$::jsonb,
        $${
            "supplierName": "string",
            "category": "enum:OFFICE_EQUIPMENT,LOW_VALUE_CONSUMABLE,SOFTWARE_SERVICE",
            "amount": "decimal",
            "arrivalDate": "date",
            "reason": "text"
        }$$::jsonb,
        $${
            "supplierName": "上海示例供应商有限公司",
            "category": "OFFICE_EQUIPMENT",
            "amountRange": [1000, 50000],
            "arrivalDateOffsetDays": 7,
            "reason": "教学备案样例采购申请"
        }$$::jsonb,
        false,
        '{"required":["supplierName","category","amount","arrivalDate","reason"]}'::jsonb,
        $${
            "roles": [
                {"actorType":"CREATOR","orgId":"origin-purchase-dept","orgName":"采购申请单位","roleId":"creator","roleName":"申请人"}
            ]
        }$$::jsonb,
        '{"expectedFinalStatus":"APPROVED","requiredActions":["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT","PURCHASE_APPROVE"]}'::jsonb,
        '{"maskFields":["supplierContactPhone"],"forbidFields":["bankAccount"],"storeRawBusinessPayload":false}'::jsonb,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'tdt-sxpt-purchase-learn-v1',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        null,
        'PURCHASE_LEARN_TEMPLATE_V1',
        '采购申请学习模板',
        'LEARN',
        'PURCHASE_APPLY',
        null,
        'DRAFT',
        'LEARN',
        $${
            "purpose": "学习模式可复用演示数据，允许学生完整查看流程和提示",
            "showFullFlow": true,
            "showTeachingText": true,
            "showOverlayHint": true,
            "allowFreeBrowse": true
        }$$::jsonb,
        $${
            "supplierName": "string",
            "category": "enum:OFFICE_EQUIPMENT,LOW_VALUE_CONSUMABLE,SOFTWARE_SERVICE",
            "amount": "decimal",
            "arrivalDate": "date",
            "reason": "text"
        }$$::jsonb,
        $${
            "supplierNamePrefix": "学习样例供应商",
            "category": "OFFICE_EQUIPMENT",
            "amountRange": [1000, 20000],
            "initStatus": "DRAFT"
        }$$::jsonb,
        false,
        '{"required":["supplierName","category","amount","arrivalDate","reason"]}'::jsonb,
        $${
            "roles": [
                {"actorType":"CREATOR","orgId":"origin-purchase-dept","orgName":"采购申请单位","roleId":"creator","roleName":"申请人"}
            ]
        }$$::jsonb,
        '{"expectedFinalStatus":"SUBMITTED","requiredActions":["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT"]}'::jsonb,
        '{"maskFields":["supplierContactPhone"],"forbidFields":["bankAccount"],"storeRawBusinessPayload":false}'::jsonb,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'tdt-sxpt-purchase-practice-v1',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        null,
        'PURCHASE_PRACTICE_TEMPLATE_V1',
        '采购申请练习模板',
        'PRACTICE',
        'PURCHASE_APPLY',
        null,
        'DRAFT',
        'PRACTICE',
        $${
            "purpose": "练习模式每名学生领取一条独占采购申请数据",
            "showFullFlow": true,
            "showTeachingText": false,
            "showOverlayHint": true,
            "enforceOrder": true
        }$$::jsonb,
        $${
            "supplierName": "string",
            "category": "enum:OFFICE_EQUIPMENT,LOW_VALUE_CONSUMABLE,SOFTWARE_SERVICE",
            "amount": "decimal",
            "arrivalDate": "date",
            "reason": "text"
        }$$::jsonb,
        $${
            "supplierNamePrefix": "练习供应商",
            "category": "OFFICE_EQUIPMENT",
            "amountRange": [3000, 30000],
            "initStatus": "DRAFT"
        }$$::jsonb,
        false,
        '{"required":["supplierName","category","amount","arrivalDate","reason"]}'::jsonb,
        $${
            "roles": [
                {"actorType":"CREATOR","orgId":"origin-purchase-dept","orgName":"采购申请单位","roleId":"creator","roleName":"申请人"}
            ]
        }$$::jsonb,
        '{"expectedFinalStatus":"SUBMITTED","requiredActions":["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT"]}'::jsonb,
        '{"maskFields":["supplierContactPhone"],"forbidFields":["bankAccount"],"storeRawBusinessPayload":false}'::jsonb,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'tdt-sxpt-purchase-exam-v1',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        null,
        'PURCHASE_EXAM_TEMPLATE_V1',
        '采购申请考试模板',
        'EXAM',
        'PURCHASE_APPLY',
        null,
        'DRAFT',
        'EXAM',
        $${
            "purpose": "考试模式每题独占并锁定采购申请数据，隐藏流程提示",
            "showFullFlow": false,
            "showTeachingText": false,
            "showOverlayHint": false,
            "showSubmission": true,
            "silentRecord": true
        }$$::jsonb,
        $${
            "supplierName": "string",
            "category": "enum:OFFICE_EQUIPMENT,LOW_VALUE_CONSUMABLE,SOFTWARE_SERVICE",
            "amount": "decimal",
            "arrivalDate": "date",
            "reason": "text"
        }$$::jsonb,
        $${
            "supplierNamePrefix": "考试供应商",
            "category": "SOFTWARE_SERVICE",
            "amountRange": [8000, 80000],
            "initStatus": "DRAFT"
        }$$::jsonb,
        false,
        '{"required":["supplierName","category","amount","arrivalDate","reason"]}'::jsonb,
        $${
            "roles": [
                {"actorType":"CREATOR","orgId":"origin-purchase-dept","orgName":"采购申请单位","roleId":"creator","roleName":"申请人"},
                {"actorType":"REVIEWER","orgId":"origin-review-dept","orgName":"采购复核单位","roleId":"reviewer","roleName":"复核人"},
                {"actorType":"APPROVER","orgId":"origin-approve-dept","orgName":"采购审批单位","roleId":"approver","roleName":"审批人"}
            ]
        }$$::jsonb,
        '{"expectedFinalStatus":"APPROVED","requiredActions":["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT","PURCHASE_APPROVE"]}'::jsonb,
        '{"maskFields":["supplierContactPhone"],"forbidFields":["bankAccount"],"storeRawBusinessPayload":false}'::jsonb,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    )
ON CONFLICT (tenant_id, connector_system_id, template_code) WHERE deleted = false
DO UPDATE SET
    teaching_point_id = EXCLUDED.teaching_point_id,
    template_name = EXCLUDED.template_name,
    scene_type = EXCLUDED.scene_type,
    module_code = EXCLUDED.module_code,
    init_state = EXCLUDED.init_state,
    support_mode = EXCLUDED.support_mode,
    config_json = EXCLUDED.config_json,
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

INSERT INTO module_data_strategy (
    id, tenant_id, connector_system_id, business_module_id, module_code, module_name,
    scene_type, need_pre_data, data_source_strategy, init_external_status, target_external_status,
    default_org_role_policy_json, share_policy, regenerate_policy, lock_policy,
    expire_policy_json, result_check_policy_json, strategy_code, template_id, prepare_timing,
    pool_size_policy_json, validation_policy_json, archive_policy_json, strategy_version, lock_version,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    (
        'mds-sxpt-purchase-record-v1',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'PURCHASE_APPLY',
        '采购申请',
        'RECORD',
        true,
        'MOCK_GENERATE',
        'DRAFT',
        'APPROVED',
        $${
            "roleGroups": [
                {"groupKey":"CREATOR","actorType":"CREATOR","orgId":"origin-purchase-dept","roleId":"creator"}
            ]
        }$$::jsonb,
        'TEACHER_SAMPLE',
        'NEVER',
        'NONE',
        '{"ttlDays":30}'::jsonb,
        '{"expectedFinalStatus":"APPROVED"}'::jsonb,
        'PURCHASE_RECORD_STRATEGY_V1',
        'tdt-sxpt-purchase-record-v1',
        'ON_DEMAND',
        '{"formalCount":1,"spareCount":0}'::jsonb,
        '{"visible":true,"operable":true,"statusMatched":true}'::jsonb,
        '{"archiveAfterDays":180}'::jsonb,
        1,
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'mds-sxpt-purchase-learn-v1',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'PURCHASE_APPLY',
        '采购申请',
        'LEARN',
        true,
        'MOCK_GENERATE',
        'DRAFT',
        'SUBMITTED',
        $${
            "roleGroups": [
                {"groupKey":"CREATOR","actorType":"CREATOR","orgId":"origin-purchase-dept","roleId":"creator"}
            ]
        }$$::jsonb,
        'SHARED_READONLY',
        'NEVER',
        'NONE',
        '{"ttlDays":14}'::jsonb,
        '{"expectedFinalStatus":"SUBMITTED"}'::jsonb,
        'PURCHASE_LEARN_STRATEGY_V1',
        'tdt-sxpt-purchase-learn-v1',
        'ON_DEMAND',
        '{"formalCount":1,"spareCount":0}'::jsonb,
        '{"visible":true,"operable":true,"statusMatched":true}'::jsonb,
        '{"archiveAfterDays":90}'::jsonb,
        1,
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'mds-sxpt-purchase-practice-v1',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'PURCHASE_APPLY',
        '采购申请',
        'PRACTICE',
        true,
        'MOCK_GENERATE',
        'DRAFT',
        'SUBMITTED',
        $${
            "roleGroups": [
                {"groupKey":"CREATOR","actorType":"CREATOR","orgId":"origin-purchase-dept","roleId":"creator"}
            ]
        }$$::jsonb,
        'STUDENT_EXCLUSIVE',
        'ON_ATTEMPT',
        'ON_ALLOCATE',
        '{"ttlDays":7}'::jsonb,
        '{"expectedFinalStatus":"SUBMITTED"}'::jsonb,
        'PURCHASE_PRACTICE_STRATEGY_V1',
        'tdt-sxpt-purchase-practice-v1',
        'ON_DEMAND',
        '{"formalCountPerStudent":1,"spareRate":0}'::jsonb,
        '{"visible":true,"operable":true,"statusMatched":true}'::jsonb,
        '{"archiveAfterDays":90}'::jsonb,
        1,
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'mds-sxpt-purchase-exam-v1',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'PURCHASE_APPLY',
        '采购申请',
        'EXAM',
        true,
        'MOCK_GENERATE',
        'DRAFT',
        'APPROVED',
        $${
            "roleGroups": [
                {"groupKey":"CREATOR","actorType":"CREATOR","orgId":"origin-purchase-dept","roleId":"creator"},
                {"groupKey":"REVIEWER","actorType":"REVIEWER","orgId":"origin-review-dept","roleId":"reviewer"},
                {"groupKey":"APPROVER","actorType":"APPROVER","orgId":"origin-approve-dept","roleId":"approver"}
            ]
        }$$::jsonb,
        'QUESTION_EXCLUSIVE',
        'ON_RETAKE',
        'ON_EXAM_START',
        '{"ttlDays":3}'::jsonb,
        '{"expectedFinalStatus":"APPROVED"}'::jsonb,
        'PURCHASE_EXAM_STRATEGY_V1',
        'tdt-sxpt-purchase-exam-v1',
        'BEFORE_START',
        '{"formalCountPerQuestion":1,"spareCount":1}'::jsonb,
        '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb,
        '{"archiveAfterDays":365}'::jsonb,
        1,
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    )
ON CONFLICT (tenant_id, connector_system_id, module_code, scene_type) WHERE deleted = false
DO UPDATE SET
    business_module_id = EXCLUDED.business_module_id,
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
    strategy_code = EXCLUDED.strategy_code,
    template_id = EXCLUDED.template_id,
    prepare_timing = EXCLUDED.prepare_timing,
    pool_size_policy_json = EXCLUDED.pool_size_policy_json,
    validation_policy_json = EXCLUDED.validation_policy_json,
    archive_policy_json = EXCLUDED.archive_policy_json,
    strategy_version = EXCLUDED.strategy_version,
    lock_version = module_data_strategy.lock_version + 1,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

UPDATE teaching_data_template
SET strategy_id = CASE template_code
    WHEN 'PURCHASE_RECORD_TEMPLATE_V1' THEN 'mds-sxpt-purchase-record-v1'
    WHEN 'PURCHASE_LEARN_TEMPLATE_V1' THEN 'mds-sxpt-purchase-learn-v1'
    WHEN 'PURCHASE_PRACTICE_TEMPLATE_V1' THEN 'mds-sxpt-purchase-practice-v1'
    WHEN 'PURCHASE_EXAM_TEMPLATE_V1' THEN 'mds-sxpt-purchase-exam-v1'
    ELSE strategy_id
END,
update_by = 'seed',
update_time = now()
WHERE tenant_id = 'demo-tenant'
  AND connector_system_id = 'origin-sxpt-purchase-system'
  AND template_code IN (
      'PURCHASE_RECORD_TEMPLATE_V1',
      'PURCHASE_LEARN_TEMPLATE_V1',
      'PURCHASE_PRACTICE_TEMPLATE_V1',
      'PURCHASE_EXAM_TEMPLATE_V1'
  )
  AND deleted = false;

INSERT INTO data_requirement (
    id, tenant_id, requirement_code, connector_system_id, business_module_id,
    module_code, strategy_id, template_id, task_id, class_id, scene_type,
    requirement_status, expected_count, success_count, failed_count,
    requirement_policy_json, remark, lock_version,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    (
        'dr-sxpt-purchase-practice-seed',
        'demo-tenant',
        'REQ-SXPT-PURCHASE-PRACTICE-SEED',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'PURCHASE_APPLY',
        'mds-sxpt-purchase-practice-v1',
        'tdt-sxpt-purchase-practice-v1',
        'task-sxpt-purchase-practice-demo',
        'class-sxpt-demo-01',
        'PRACTICE',
        'READY',
        3,
        3,
        0,
        $${
            "source": "sxpt_origin_system",
            "strategyCode": "PURCHASE_PRACTICE_STRATEGY_V1",
            "templateCode": "PURCHASE_PRACTICE_TEMPLATE_V1",
            "expectedRole": {"actorType":"CREATOR","orgId":"origin-purchase-dept","roleId":"creator"},
            "flowNodes": ["PURCHASE_CREATE", "PURCHASE_FILL", "PURCHASE_SUBMIT"]
        }$$::jsonb,
        '采购申请练习批次示例：每名学生一条独占数据。',
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'dr-sxpt-purchase-exam-seed',
        'demo-tenant',
        'REQ-SXPT-PURCHASE-EXAM-SEED',
        'origin-sxpt-purchase-system',
        'bm-sxpt-purchase-apply',
        'PURCHASE_APPLY',
        'mds-sxpt-purchase-exam-v1',
        'tdt-sxpt-purchase-exam-v1',
        'task-sxpt-purchase-exam-demo',
        'class-sxpt-demo-01',
        'EXAM',
        'READY',
        3,
        3,
        0,
        $${
            "source": "sxpt_origin_system",
            "strategyCode": "PURCHASE_EXAM_STRATEGY_V1",
            "templateCode": "PURCHASE_EXAM_TEMPLATE_V1",
            "lockPolicy": "ON_EXAM_START",
            "flowNodes": ["PURCHASE_CREATE", "PURCHASE_FILL", "PURCHASE_SUBMIT", "PURCHASE_APPROVE"]
        }$$::jsonb,
        '采购申请考试批次示例：每名学生一条锁定数据。',
        0,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    )
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
    lock_version = data_requirement.lock_version + 1,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

INSERT INTO data_requirement_item (
    id, tenant_id, requirement_id, request_batch_id, request_item_id,
    connector_system_id, business_module_id, module_code, template_id, scene_type,
    task_id, execution_id, student_id, question_id, exam_attempt_id, question_attempt_id,
    collaboration_unit_id, segment_no, actor_type,
    owner_external_org_id, owner_external_org_name, required_external_org_id,
    required_external_org_name, required_external_role_id, required_external_role_name,
    init_external_status, target_external_status, target_url,
    data_scope_json, required_actions_json, validation_policy_json, score_point_snapshot_json,
    item_status, external_business_id, external_business_no, external_status,
    validation_status, validation_time, validation_result_json, failure_reason,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    (
        'dri-sxpt-purchase-practice-001', 'demo-tenant', 'dr-sxpt-purchase-practice-seed',
        'batch-sxpt-purchase-practice-seed', 'practice-student-001',
        'origin-sxpt-purchase-system', 'bm-sxpt-purchase-apply', 'PURCHASE_APPLY',
        'tdt-sxpt-purchase-practice-v1', 'PRACTICE',
        'task-sxpt-purchase-practice-demo', null, 'user-demo-student-01', 'question-sxpt-purchase-practice', null, null,
        null, 1, 'CREATOR',
        'origin-purchase-dept', '采购申请单位', 'origin-purchase-dept', '采购申请单位', 'creator', '申请人',
        'DRAFT', 'SUBMITTED', '/business-sdk-demo.html?bizId=sxpt-practice-001',
        '{"exclusive":true,"readonly":false}'::jsonb,
        '["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT"]'::jsonb,
        '{"visible":true,"operable":true,"statusMatched":true}'::jsonb,
        '{"totalScore":100,"objectiveScore":70,"subjectiveScore":30}'::jsonb,
        'READY', 'sxpt-practice-001', 'CGSQ-P-SEED-001', 'DRAFT',
        'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true}'::jsonb, null,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'dri-sxpt-purchase-practice-002', 'demo-tenant', 'dr-sxpt-purchase-practice-seed',
        'batch-sxpt-purchase-practice-seed', 'practice-student-002',
        'origin-sxpt-purchase-system', 'bm-sxpt-purchase-apply', 'PURCHASE_APPLY',
        'tdt-sxpt-purchase-practice-v1', 'PRACTICE',
        'task-sxpt-purchase-practice-demo', null, 'user-demo-student-02', 'question-sxpt-purchase-practice', null, null,
        null, 1, 'CREATOR',
        'origin-purchase-dept', '采购申请单位', 'origin-purchase-dept', '采购申请单位', 'creator', '申请人',
        'DRAFT', 'SUBMITTED', '/business-sdk-demo.html?bizId=sxpt-practice-002',
        '{"exclusive":true,"readonly":false}'::jsonb,
        '["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT"]'::jsonb,
        '{"visible":true,"operable":true,"statusMatched":true}'::jsonb,
        '{"totalScore":100,"objectiveScore":70,"subjectiveScore":30}'::jsonb,
        'READY', 'sxpt-practice-002', 'CGSQ-P-SEED-002', 'DRAFT',
        'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true}'::jsonb, null,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'dri-sxpt-purchase-practice-003', 'demo-tenant', 'dr-sxpt-purchase-practice-seed',
        'batch-sxpt-purchase-practice-seed', 'practice-student-003',
        'origin-sxpt-purchase-system', 'bm-sxpt-purchase-apply', 'PURCHASE_APPLY',
        'tdt-sxpt-purchase-practice-v1', 'PRACTICE',
        'task-sxpt-purchase-practice-demo', null, 'user-demo-student-03', 'question-sxpt-purchase-practice', null, null,
        null, 1, 'CREATOR',
        'origin-purchase-dept', '采购申请单位', 'origin-purchase-dept', '采购申请单位', 'creator', '申请人',
        'DRAFT', 'SUBMITTED', '/business-sdk-demo.html?bizId=sxpt-practice-003',
        '{"exclusive":true,"readonly":false}'::jsonb,
        '["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT"]'::jsonb,
        '{"visible":true,"operable":true,"statusMatched":true}'::jsonb,
        '{"totalScore":100,"objectiveScore":70,"subjectiveScore":30}'::jsonb,
        'READY', 'sxpt-practice-003', 'CGSQ-P-SEED-003', 'DRAFT',
        'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true}'::jsonb, null,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'dri-sxpt-purchase-exam-001', 'demo-tenant', 'dr-sxpt-purchase-exam-seed',
        'batch-sxpt-purchase-exam-seed', 'exam-student-001',
        'origin-sxpt-purchase-system', 'bm-sxpt-purchase-apply', 'PURCHASE_APPLY',
        'tdt-sxpt-purchase-exam-v1', 'EXAM',
        'task-sxpt-purchase-exam-demo', null, 'user-demo-student-01', 'question-sxpt-purchase-exam', null, null,
        null, 1, 'CREATOR',
        'origin-purchase-dept', '采购申请单位', 'origin-purchase-dept', '采购申请单位', 'creator', '申请人',
        'DRAFT', 'APPROVED', '/business-sdk-demo.html?bizId=sxpt-exam-001',
        '{"exclusive":true,"readonly":false,"locked":true}'::jsonb,
        '["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT","PURCHASE_APPROVE"]'::jsonb,
        '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb,
        '{"totalScore":100,"objectiveScore":80,"subjectiveScore":20}'::jsonb,
        'READY', 'sxpt-exam-001', 'CGSQ-E-SEED-001', 'DRAFT',
        'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb, null,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'dri-sxpt-purchase-exam-002', 'demo-tenant', 'dr-sxpt-purchase-exam-seed',
        'batch-sxpt-purchase-exam-seed', 'exam-student-002',
        'origin-sxpt-purchase-system', 'bm-sxpt-purchase-apply', 'PURCHASE_APPLY',
        'tdt-sxpt-purchase-exam-v1', 'EXAM',
        'task-sxpt-purchase-exam-demo', null, 'user-demo-student-02', 'question-sxpt-purchase-exam', null, null,
        null, 1, 'CREATOR',
        'origin-purchase-dept', '采购申请单位', 'origin-purchase-dept', '采购申请单位', 'creator', '申请人',
        'DRAFT', 'APPROVED', '/business-sdk-demo.html?bizId=sxpt-exam-002',
        '{"exclusive":true,"readonly":false,"locked":true}'::jsonb,
        '["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT","PURCHASE_APPROVE"]'::jsonb,
        '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb,
        '{"totalScore":100,"objectiveScore":80,"subjectiveScore":20}'::jsonb,
        'READY', 'sxpt-exam-002', 'CGSQ-E-SEED-002', 'DRAFT',
        'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb, null,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'dri-sxpt-purchase-exam-003', 'demo-tenant', 'dr-sxpt-purchase-exam-seed',
        'batch-sxpt-purchase-exam-seed', 'exam-student-003',
        'origin-sxpt-purchase-system', 'bm-sxpt-purchase-apply', 'PURCHASE_APPLY',
        'tdt-sxpt-purchase-exam-v1', 'EXAM',
        'task-sxpt-purchase-exam-demo', null, 'user-demo-student-03', 'question-sxpt-purchase-exam', null, null,
        null, 1, 'CREATOR',
        'origin-purchase-dept', '采购申请单位', 'origin-purchase-dept', '采购申请单位', 'creator', '申请人',
        'DRAFT', 'APPROVED', '/business-sdk-demo.html?bizId=sxpt-exam-003',
        '{"exclusive":true,"readonly":false,"locked":true}'::jsonb,
        '["PURCHASE_CREATE","PURCHASE_FILL","PURCHASE_SUBMIT","PURCHASE_APPROVE"]'::jsonb,
        '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb,
        '{"totalScore":100,"objectiveScore":80,"subjectiveScore":20}'::jsonb,
        'READY', 'sxpt-exam-003', 'CGSQ-E-SEED-003', 'DRAFT',
        'PASSED', now(), '{"visible":true,"operable":true,"statusMatched":true,"roleMatched":true}'::jsonb, null,
        'seed', now(), 'seed', now(), 'ACTIVE', false
    )
ON CONFLICT (tenant_id, request_batch_id, request_item_id) WHERE deleted = false
DO UPDATE SET
    requirement_id = EXCLUDED.requirement_id,
    connector_system_id = EXCLUDED.connector_system_id,
    business_module_id = EXCLUDED.business_module_id,
    module_code = EXCLUDED.module_code,
    template_id = EXCLUDED.template_id,
    scene_type = EXCLUDED.scene_type,
    task_id = EXCLUDED.task_id,
    execution_id = EXCLUDED.execution_id,
    student_id = EXCLUDED.student_id,
    question_id = EXCLUDED.question_id,
    exam_attempt_id = EXCLUDED.exam_attempt_id,
    question_attempt_id = EXCLUDED.question_attempt_id,
    collaboration_unit_id = EXCLUDED.collaboration_unit_id,
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

INSERT INTO data_prepare_job (
    id, tenant_id, connector_system_id, pool_id, task_id, module_code, scene_type,
    job_type, expected_count, success_count, failed_count, job_status,
    request_json, result_json, error_message, start_time, end_time,
    idempotency_key, external_request_id, request_batch_id, retry_count,
    next_retry_time, trigger_type, trace_id,
    create_by, create_time, update_by, update_time, status, deleted
) VALUES
    (
        'job-sxpt-purchase-practice-seed',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        null,
        'task-sxpt-purchase-practice-demo',
        'PURCHASE_APPLY',
        'PRACTICE',
        'CREATE',
        3,
        3,
        0,
        'SUCCESS',
        $${
            "requirementId": "dr-sxpt-purchase-practice-seed",
            "requestBatchId": "batch-sxpt-purchase-practice-seed",
            "templateId": "tdt-sxpt-purchase-practice-v1",
            "strategyId": "mds-sxpt-purchase-practice-v1"
        }$$::jsonb,
        '{"readyCount":3,"failedCount":0,"validationPassed":true}'::jsonb,
        null,
        now(),
        now(),
        'idem-sxpt-purchase-practice-seed',
        'origin-request-purchase-practice-seed',
        'batch-sxpt-purchase-practice-seed',
        0,
        null,
        'MANUAL',
        'trace-sxpt-purchase-practice-seed',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    ),
    (
        'job-sxpt-purchase-exam-seed',
        'demo-tenant',
        'origin-sxpt-purchase-system',
        null,
        'task-sxpt-purchase-exam-demo',
        'PURCHASE_APPLY',
        'EXAM',
        'CREATE',
        3,
        3,
        0,
        'SUCCESS',
        $${
            "requirementId": "dr-sxpt-purchase-exam-seed",
            "requestBatchId": "batch-sxpt-purchase-exam-seed",
            "templateId": "tdt-sxpt-purchase-exam-v1",
            "strategyId": "mds-sxpt-purchase-exam-v1",
            "lockPolicy": "ON_EXAM_START"
        }$$::jsonb,
        '{"readyCount":3,"failedCount":0,"validationPassed":true,"locked":true}'::jsonb,
        null,
        now(),
        now(),
        'idem-sxpt-purchase-exam-seed',
        'origin-request-purchase-exam-seed',
        'batch-sxpt-purchase-exam-seed',
        0,
        null,
        'MANUAL',
        'trace-sxpt-purchase-exam-seed',
        'seed', now(), 'seed', now(), 'ACTIVE', false
    )
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
    next_retry_time = EXCLUDED.next_retry_time,
    trigger_type = EXCLUDED.trigger_type,
    trace_id = EXCLUDED.trace_id,
    update_by = 'seed',
    update_time = now(),
    status = 'ACTIVE';

COMMIT;
