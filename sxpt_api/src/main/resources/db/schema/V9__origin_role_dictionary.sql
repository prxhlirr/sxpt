CREATE TABLE IF NOT EXISTS origin_role (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    role_code varchar(128) NOT NULL,
    role_name varchar(128) NOT NULL,
    external_role_id varchar(128),
    role_type varchar(64),
    remark varchar(512),
    create_by varchar(64),
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE origin_role IS '原平台角色字典表：维护教学平台可选择的原业务系统角色，供模块流程参与方配置和数据准备参数生成使用。';
COMMENT ON COLUMN origin_role.connector_system_id IS '原平台系统 ID，弱引用 connector_system。';
COMMENT ON COLUMN origin_role.role_code IS '教学平台侧稳定角色编码，同一原平台内唯一。';
COMMENT ON COLUMN origin_role.role_name IS '角色展示名称，用于后台下拉选择和配置核对。';
COMMENT ON COLUMN origin_role.external_role_id IS '原平台角色 ID 或角色编码，用于调用原平台接口时定位真实角色。';
COMMENT ON COLUMN origin_role.role_type IS '角色分类，例如 ORG_ROLE、SYSTEM_ROLE、PROCESS_ROLE。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_origin_role_code
    ON origin_role (tenant_id, connector_system_id, role_code)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_origin_role_connector
    ON origin_role (tenant_id, connector_system_id, status)
    WHERE deleted = false;
