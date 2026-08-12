ALTER TABLE classic_case_usage
    ADD COLUMN IF NOT EXISTS teaching_data_instance_id varchar(64);

COMMENT ON COLUMN classic_case_usage.teaching_data_instance_id IS '教学平台数据实例ID，用于复用现有launchToken启动链路';

CREATE INDEX IF NOT EXISTS idx_classic_case_usage_teaching_instance
    ON classic_case_usage (tenant_id, teaching_data_instance_id)
    WHERE deleted = false;
