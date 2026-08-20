-- 仅由 application-dev.yml 加载：保留旧 OA 演示环境，并把 test-0805 配成 OA 经典案例学习环境。
-- 生产环境仍必须通过接入管理页面创建环境配对和独立 Connector Key。

UPDATE connector_system
SET environment_type = 'LEARNING',
    environment_group_code = 'oa-demo-local',
    update_by = 'dev-bootstrap',
    update_time = now()
WHERE id = 'origin-oa-demo-system'
  AND tenant_id = 'demo-tenant'
  AND deleted = false;

UPDATE connector_system
SET environment_type = 'LEARNING',
    environment_group_code = 'oa-test-0805-local',
    auth_type = 'API_KEY',
    config_json = (COALESCE(config_json, '{}'::jsonb) - 'token') || jsonb_build_object(
        'apiKey', 'oa-data-center-incoming-token',
        'apiKeyEnv', 'OA_DATA_CREATE_API_KEY',
        'headerName', 'X-API-Key',
        'dataPrepareMode', 'CALL_ORIGIN_API'
    ),
    status = 'ACTIVE',
    deleted = false,
    update_by = 'dev-bootstrap',
    update_time = now()
WHERE id = '5d5bba96bdc24fc99bf70261db4ebac4'
  AND tenant_id = 'demo-tenant';

-- 本地 OA 必须以 OA_DATA_CREATE_API_KEY=oa-data-center-incoming-token 启动。
-- 该 Key 只用于实训平台 -> OA DATA_CREATE，不能复用 OA -> 实训平台的 Connector Key。

INSERT INTO connector_system (
    id, tenant_id, system_code, system_name, system_type,
    environment_type, environment_group_code,
    base_url, auth_type, config_json,
    create_by, create_time, update_by, update_time, status, deleted
)
SELECT
    'origin-oa-prod-system', source.tenant_id, 'OA_DEMO_PROD', 'OA 协同办公系统（案例来源）', source.system_type,
    'PROD', 'oa-test-0805-local',
    source.base_url, source.auth_type, source.config_json,
    'dev-bootstrap', now(), 'dev-bootstrap', now(), 'ACTIVE', false
FROM connector_system source
WHERE source.id = 'origin-oa-demo-system'
  AND source.tenant_id = 'demo-tenant'
  AND source.deleted = false
  AND NOT EXISTS (
      SELECT 1
      FROM connector_system target
      WHERE target.id = 'origin-oa-prod-system'
  );

UPDATE connector_system
SET environment_type = 'PROD',
    environment_group_code = 'oa-test-0805-local',
    status = 'ACTIVE',
    deleted = false,
    update_by = 'dev-bootstrap',
    update_time = now()
WHERE id = 'origin-oa-prod-system';

-- 明文只用于本地联调：sxpt_dev_oa_connector_20260818。
-- 数据库只保存 SHA-256；OA 通过 OA_TRAINING_CONNECTOR_KEY 注入同一开发 Key。
INSERT INTO connector_external_credential (
    id, tenant_id, connector_system_id, credential_name,
    api_key_prefix, api_key_hash, hash_algorithm,
    create_by, create_time, update_by, update_time, status, deleted
)
SELECT
    'credential-oa-demo-local', 'demo-tenant', 'origin-oa-prod-system', 'OA 本地经典案例接入密钥',
    'sxpt_dev_oa_connector_',
    '1de9f75d584d624823a8851ed2dace03726c497ea1bf189a9609c6c14e51deb2',
    'SHA-256',
    'dev-bootstrap', now(), 'dev-bootstrap', now(), 'ACTIVE', false
WHERE EXISTS (
    SELECT 1 FROM connector_system
    WHERE id = 'origin-oa-prod-system' AND deleted = false
)
AND NOT EXISTS (
    SELECT 1 FROM connector_external_credential
    WHERE connector_system_id = 'origin-oa-prod-system' AND deleted = false
);

-- test-0805 原先只有 NORMAL 练习模板。经典案例运行时会按用途严格选择模板，
-- 因此为还原和格式化 demo 各复制一份专用模板，保留原普通造数配置不受影响。
-- 模板编码使用 OA 已注册的开放接口编码，确保 generationSource=CLASSIC_CASE 校验通过。
UPDATE teaching_data_template
SET template_code = CASE id
        WHEN 'tdt-test0805-classic-replay-v1' THEN 'incoming_classic_case_v1'
        WHEN 'tdt-test0805-classic-demo-v1' THEN 'incoming_classic_demo_v1'
        ELSE template_code
    END,
    update_by = 'dev-bootstrap',
    update_time = now()
WHERE id IN ('tdt-test0805-classic-replay-v1', 'tdt-test0805-classic-demo-v1')
  AND deleted = false;

INSERT INTO teaching_data_template (
    id, tenant_id, connector_system_id, teaching_point_id,
    template_code, template_name, scene_type, module_code, strategy_id,
    init_state, support_mode, template_usage, config_json,
    data_schema_json, mock_rule_json, readonly_flag, request_schema_json,
    required_org_role_json, result_check_schema_json, sensitive_field_policy_json,
    create_by, create_time, update_by, update_time, status, deleted
)
SELECT
    usage.template_id,
    source.tenant_id,
    source.connector_system_id,
    null,
    usage.template_code,
    usage.template_name,
    source.scene_type,
    source.module_code,
    source.strategy_id,
    source.init_state,
    source.support_mode,
    usage.template_usage,
    source.config_json,
    source.data_schema_json,
    source.mock_rule_json,
    source.readonly_flag,
    source.request_schema_json,
    source.required_org_role_json,
    source.result_check_schema_json,
    source.sensitive_field_policy_json,
    'dev-bootstrap', now(), 'dev-bootstrap', now(), 'ACTIVE', false
FROM (
    SELECT *
    FROM teaching_data_template
    WHERE tenant_id = 'demo-tenant'
      AND connector_system_id = '5d5bba96bdc24fc99bf70261db4ebac4'
      AND module_code = 'record_apply_1785916779495'
      AND scene_type = 'PRACTICE'
      AND status = 'ACTIVE'
      AND deleted = false
    ORDER BY CASE WHEN template_usage = 'NORMAL' THEN 0 ELSE 1 END, create_time DESC
    LIMIT 1
) source
CROSS JOIN (VALUES
    ('tdt-test0805-classic-replay-v1', 'incoming_classic_case_v1',
     'test-0805 经典案例还原模板', 'CLASSIC_CASE_REPLAY'),
    ('tdt-test0805-classic-demo-v1', 'incoming_classic_demo_v1',
     'test-0805 经典案例练习模板', 'CLASSIC_CASE_DEMO')
) AS usage(template_id, template_code, template_name, template_usage)
ON CONFLICT (tenant_id, connector_system_id, template_code) WHERE deleted = false
DO UPDATE SET
    teaching_point_id = EXCLUDED.teaching_point_id,
    template_name = EXCLUDED.template_name,
    scene_type = EXCLUDED.scene_type,
    module_code = EXCLUDED.module_code,
    strategy_id = EXCLUDED.strategy_id,
    init_state = EXCLUDED.init_state,
    support_mode = EXCLUDED.support_mode,
    template_usage = EXCLUDED.template_usage,
    config_json = EXCLUDED.config_json,
    data_schema_json = EXCLUDED.data_schema_json,
    mock_rule_json = EXCLUDED.mock_rule_json,
    readonly_flag = EXCLUDED.readonly_flag,
    request_schema_json = EXCLUDED.request_schema_json,
    required_org_role_json = EXCLUDED.required_org_role_json,
    result_check_schema_json = EXCLUDED.result_check_schema_json,
    sensitive_field_policy_json = EXCLUDED.sensitive_field_policy_json,
    update_by = 'dev-bootstrap',
    update_time = now(),
    status = 'ACTIVE';
