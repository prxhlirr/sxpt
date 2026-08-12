CREATE UNIQUE INDEX IF NOT EXISTS uk_connector_external_credential_system
    ON connector_external_credential (connector_system_id)
    WHERE deleted = false;

COMMENT ON INDEX uk_connector_external_credential_system IS '同一个原平台系统只允许绑定一条第三方对接Key';
