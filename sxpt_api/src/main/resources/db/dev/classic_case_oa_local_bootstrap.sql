-- 仅由 application-dev.yml 加载：把已有 OA 本地系统作为学习环境，并补齐对应正式来源环境。
-- 生产环境仍必须通过接入管理页面创建环境配对和独立 Connector Key。

UPDATE connector_system
SET environment_type = 'LEARNING',
    environment_group_code = 'oa-demo-local',
    update_by = 'dev-bootstrap',
    update_time = now()
WHERE id = 'origin-oa-demo-system'
  AND tenant_id = 'demo-tenant'
  AND deleted = false;

INSERT INTO connector_system (
    id, tenant_id, system_code, system_name, system_type,
    environment_type, environment_group_code,
    base_url, auth_type, config_json,
    create_by, create_time, update_by, update_time, status, deleted
)
SELECT
    'origin-oa-prod-system', source.tenant_id, 'OA_DEMO_PROD', 'OA 协同办公系统（案例来源）', source.system_type,
    'PROD', 'oa-demo-local',
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
    environment_group_code = 'oa-demo-local',
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
