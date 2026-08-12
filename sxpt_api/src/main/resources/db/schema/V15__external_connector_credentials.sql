CREATE TABLE IF NOT EXISTS connector_external_credential (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    credential_name varchar(128) NOT NULL,
    api_key_prefix varchar(64) NOT NULL,
    api_key_hash varchar(128) NOT NULL,
    hash_algorithm varchar(32) NOT NULL,
    last_used_time timestamp,
    expire_time timestamp,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL,
    update_by varchar(64),
    update_time timestamp,
    status varchar(32) NOT NULL,
    deleted boolean NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_connector_external_credential_hash
    ON connector_external_credential (api_key_hash)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_connector_external_credential_system
    ON connector_external_credential (tenant_id, connector_system_id)
    WHERE deleted = false;

COMMENT ON TABLE connector_external_credential IS '第三方原平台调用教学平台的系统级凭证';
COMMENT ON COLUMN connector_external_credential.connector_system_id IS '教学平台生成的原平台正式环境ID';
COMMENT ON COLUMN connector_external_credential.api_key_prefix IS 'API Key前缀，仅用于后台展示和定位，不可用于认证';
COMMENT ON COLUMN connector_external_credential.api_key_hash IS 'API Key哈希，数据库不保存明文';
