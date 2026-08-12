ALTER TABLE connector_system
    ADD COLUMN IF NOT EXISTS environment_type varchar(32);

ALTER TABLE connector_system
    ADD COLUMN IF NOT EXISTS environment_group_code varchar(128);

COMMENT ON COLUMN connector_system.environment_type IS '原平台环境类型：PROD 正式环境，LEARNING 学习环境';
COMMENT ON COLUMN connector_system.environment_group_code IS '同一原业务系统正式环境和学习环境的绑定组编码';

CREATE INDEX IF NOT EXISTS idx_connector_system_env_group
    ON connector_system (tenant_id, environment_group_code, environment_type);
