ALTER TABLE classic_case_asset
    ADD COLUMN IF NOT EXISTS tags_json jsonb NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE classic_case_asset
    ADD COLUMN IF NOT EXISTS source_updated_at timestamp;

ALTER TABLE classic_case_asset
    ADD COLUMN IF NOT EXISTS disable_reason varchar(1024);

ALTER TABLE classic_case_version
    ADD COLUMN IF NOT EXISTS case_version_id varchar(128);

ALTER TABLE classic_case_version
    ADD COLUMN IF NOT EXISTS supported_generation_modes_json jsonb NOT NULL
        DEFAULT '["REPLAY_CASE","FORMAT_DEMO"]'::jsonb;

ALTER TABLE classic_case_version
    ADD COLUMN IF NOT EXISTS content_hash varchar(128);

UPDATE classic_case_version
SET case_version_id = id
WHERE case_version_id IS NULL;

UPDATE classic_case_version
SET content_hash = payload_hash
WHERE content_hash IS NULL;

ALTER TABLE classic_case_version
    ALTER COLUMN case_version_id SET NOT NULL;

ALTER TABLE classic_case_version
    ALTER COLUMN content_hash SET NOT NULL;

DROP INDEX IF EXISTS uk_classic_case_asset_code;

CREATE UNIQUE INDEX IF NOT EXISTS uk_classic_case_asset_source_code
    ON classic_case_asset (tenant_id, source_connector_system_id, case_code)
    WHERE deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_classic_case_version_external_id
    ON classic_case_version (tenant_id, case_asset_id, case_version_id)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_classic_case_asset_options
    ON classic_case_asset (tenant_id, module_code, source_connector_system_id, learning_connector_system_id, status)
    WHERE deleted = false;

COMMENT ON COLUMN classic_case_asset.tags_json IS 'OA 推送的脱敏案例标签 JSON 数组';
COMMENT ON COLUMN classic_case_asset.source_updated_at IS 'OA 原平台记录的案例更新时间';
COMMENT ON COLUMN classic_case_asset.disable_reason IS '原平台停用案例时提供的原因';
COMMENT ON COLUMN classic_case_version.case_version_id IS 'OA 提供的不可变案例版本 ID';
COMMENT ON COLUMN classic_case_version.supported_generation_modes_json IS '该版本支持的 REPLAY_CASE/FORMAT_DEMO 模式';
COMMENT ON COLUMN classic_case_version.content_hash IS '案例版本完整规范化内容摘要，用于幂等与冲突检测';
