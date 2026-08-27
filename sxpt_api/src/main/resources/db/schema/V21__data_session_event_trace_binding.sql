ALTER TABLE capture_event ADD COLUMN IF NOT EXISTS data_session_id varchar(64);
ALTER TABLE capture_event ADD COLUMN IF NOT EXISTS data_instance_id varchar(64);
ALTER TABLE capture_event ADD COLUMN IF NOT EXISTS business_scene_code varchar(128);
ALTER TABLE capture_event ADD COLUMN IF NOT EXISTS external_business_id varchar(128);

ALTER TABLE execution_trace ADD COLUMN IF NOT EXISTS data_session_id varchar(64);
ALTER TABLE execution_trace ADD COLUMN IF NOT EXISTS data_instance_id varchar(64);
ALTER TABLE execution_trace ADD COLUMN IF NOT EXISTS business_scene_code varchar(128);
ALTER TABLE execution_trace ADD COLUMN IF NOT EXISTS external_business_id varchar(128);

CREATE INDEX IF NOT EXISTS idx_capture_event_data_session
ON capture_event (tenant_id, data_session_id)
WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_execution_trace_data_session
ON execution_trace (tenant_id, data_session_id)
WHERE deleted = false;

COMMENT ON COLUMN capture_event.data_session_id IS '原平台 DataSession ID，用于绑定老师备案事件和原平台业务数据。';
COMMENT ON COLUMN capture_event.business_scene_code IS '原平台稳定业务场景编码。';
COMMENT ON COLUMN capture_event.external_business_id IS '原平台真实业务数据 ID。';
COMMENT ON COLUMN execution_trace.data_session_id IS '原平台 DataSession ID，用于绑定学生执行轨迹和原平台业务数据。';
COMMENT ON COLUMN execution_trace.business_scene_code IS '原平台稳定业务场景编码。';
COMMENT ON COLUMN execution_trace.external_business_id IS '原平台真实业务数据 ID。';
