ALTER TABLE teaching_data_template
    ADD COLUMN IF NOT EXISTS template_usage varchar(32) NOT NULL DEFAULT 'NORMAL';

COMMENT ON COLUMN teaching_data_template.template_usage IS '模板用途：NORMAL 普通造数，CLASSIC_CASE_REPLAY 经典案例还原，CLASSIC_CASE_DEMO 经典案例 demo。';

CREATE INDEX IF NOT EXISTS idx_data_template_usage_lookup
    ON teaching_data_template (tenant_id, connector_system_id, module_code, scene_type, template_usage)
    WHERE deleted = false;
