CREATE TABLE IF NOT EXISTS classic_case_asset (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    case_code varchar(128) NOT NULL,
    case_title varchar(256) NOT NULL,
    case_summary varchar(1024),
    source_connector_system_id varchar(64) NOT NULL,
    learning_connector_system_id varchar(64) NOT NULL,
    environment_group_code varchar(128) NOT NULL,
    business_module_id varchar(64) NOT NULL,
    module_code varchar(128) NOT NULL,
    teaching_point_id varchar(64),
    scene_types_json jsonb NOT NULL,
    current_version_id varchar(64),
    lock_version bigint NOT NULL DEFAULT 0,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'DRAFT',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE classic_case_asset IS '经典案例资产表：保存脱敏案例的业务归属、正式环境来源和学习环境复刻目标';
COMMENT ON COLUMN classic_case_asset.source_connector_system_id IS '来源原平台正式环境 ID';
COMMENT ON COLUMN classic_case_asset.learning_connector_system_id IS '对应原平台学习环境 ID';
COMMENT ON COLUMN classic_case_asset.environment_group_code IS '正式环境和学习环境的绑定组编码';
COMMENT ON COLUMN classic_case_asset.scene_types_json IS '适用教学场景 JSON 数组';
COMMENT ON COLUMN classic_case_asset.current_version_id IS '当前可用案例版本 ID';

CREATE UNIQUE INDEX IF NOT EXISTS uk_classic_case_asset_code
    ON classic_case_asset (tenant_id, case_code)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_classic_case_asset_module
    ON classic_case_asset (tenant_id, learning_connector_system_id, business_module_id)
    WHERE deleted = false;

CREATE TABLE IF NOT EXISTS classic_case_version (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    case_asset_id varchar(64) NOT NULL,
    version_no integer NOT NULL,
    payload_schema_version varchar(32) NOT NULL,
    desensitized_case_payload_json jsonb NOT NULL,
    case_data_format_json jsonb NOT NULL,
    identity_binding_json jsonb NOT NULL,
    desensitize_policy_json jsonb,
    payload_hash varchar(128) NOT NULL,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE classic_case_version IS '经典案例版本表：保存完全脱敏后的案例内容、学生练习格式和身份占位符绑定';
COMMENT ON COLUMN classic_case_version.desensitized_case_payload_json IS '专家/老师备案或教学时用于学习环境一比一复刻的脱敏案例内容';
COMMENT ON COLUMN classic_case_version.case_data_format_json IS '学生练习时传给学习环境的数据格式，不包含复杂造数规则';
COMMENT ON COLUMN classic_case_version.identity_binding_json IS '案例身份占位符和教学平台模块参与方配置的绑定关系';
COMMENT ON COLUMN classic_case_version.payload_hash IS '脱敏内容哈希，用于审计和幂等比对';

CREATE UNIQUE INDEX IF NOT EXISTS uk_classic_case_version_no
    ON classic_case_version (tenant_id, case_asset_id, version_no)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_classic_case_version_asset
    ON classic_case_version (tenant_id, case_asset_id, status)
    WHERE deleted = false;
