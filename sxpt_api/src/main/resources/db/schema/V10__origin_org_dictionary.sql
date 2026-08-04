CREATE TABLE IF NOT EXISTS origin_org (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    org_code varchar(128) NOT NULL,
    org_name varchar(128) NOT NULL,
    external_org_id varchar(128),
    parent_external_org_id varchar(128),
    org_type varchar(64),
    remark varchar(512),
    create_by varchar(64),
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE origin_org IS '原平台组织字典表：维护教学平台可选择的原业务系统组织或单位，供模块流程参与方配置和数据准备参数生成使用。';
COMMENT ON COLUMN origin_org.connector_system_id IS '原平台系统 ID，弱引用 connector_system。';
COMMENT ON COLUMN origin_org.org_code IS '教学平台侧稳定组织编码，同一原平台内唯一。';
COMMENT ON COLUMN origin_org.org_name IS '组织展示名称，用于后台下拉选择和配置核对。';
COMMENT ON COLUMN origin_org.external_org_id IS '原平台组织 ID 或组织编码，用于调用原平台接口时定位真实单位。';
COMMENT ON COLUMN origin_org.parent_external_org_id IS '原平台父级组织 ID，用于展示或校验组织层级关系。';
COMMENT ON COLUMN origin_org.org_type IS '组织分类，例如 DEPT、COMPANY、SCHOOL、CLASS。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_origin_org_code
    ON origin_org (tenant_id, connector_system_id, org_code)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_origin_org_connector
    ON origin_org (tenant_id, connector_system_id, status)
    WHERE deleted = false;
