CREATE TABLE IF NOT EXISTS origin_data_session (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    launch_context_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,

    user_id varchar(64) NOT NULL,
    actor_type varchar(32),
    scene_type varchar(32) NOT NULL,

    task_id varchar(64),
    execution_id varchar(64),
    capture_session_id varchar(64),
    practice_attempt_id varchar(64),
    exam_attempt_id varchar(64),
    question_attempt_id varchar(64),

    business_scene_code varchar(128) NOT NULL,
    business_scene_name varchar(255),

    source_data_session_id varchar(64),
    source_external_business_id varchar(128),

    data_instance_id varchar(64) NOT NULL,
    external_business_id varchar(128) NOT NULL,
    external_business_no varchar(128),
    external_status varchar(64),
    entry_url varchar(1024),

    data_spec_snapshot_json jsonb,
    origin_payload_snapshot_json jsonb,

    idempotency_key varchar(255) NOT NULL,
    session_status varchar(32) NOT NULL,

    create_by varchar(64),
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_origin_data_session_idem
ON origin_data_session (tenant_id, idempotency_key)
WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_origin_data_session_launch
ON origin_data_session (tenant_id, launch_context_id);

CREATE INDEX IF NOT EXISTS idx_origin_data_session_execution
ON origin_data_session (tenant_id, execution_id);

CREATE INDEX IF NOT EXISTS idx_origin_data_session_external
ON origin_data_session (tenant_id, connector_system_id, external_business_id);

ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS generation_source varchar(64);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS business_scene_code varchar(128);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS business_scene_name varchar(255);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS source_data_session_id varchar(64);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS source_external_business_id varchar(128);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS data_spec_snapshot_json jsonb;
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS entry_url varchar(1024);

COMMENT ON TABLE origin_data_session IS '原平台数据会话，绑定教学上下文和原平台真实业务数据。';
COMMENT ON COLUMN origin_data_session.idempotency_key IS '原平台和教学平台共同使用的幂等键，防止重复点击重复注册。';
COMMENT ON COLUMN origin_data_session.business_scene_code IS '原平台稳定业务场景编码，不是页面路径。';
COMMENT ON COLUMN origin_data_session.source_data_session_id IS '练习复制老师备案样本时的来源 DataSession ID。';
COMMENT ON COLUMN origin_data_session.source_external_business_id IS '练习复制老师备案样本时的来源原平台业务数据 ID。';

COMMENT ON COLUMN teaching_data_instance.generation_source IS '教学数据实例来源：ORIGIN_SELF_CREATED、ORIGIN_COPIED_FROM_RECORD 等。';
COMMENT ON COLUMN teaching_data_instance.business_scene_code IS '原平台稳定业务场景编码。';
COMMENT ON COLUMN teaching_data_instance.source_data_session_id IS '基于老师备案样本复制时的来源 DataSession ID。';
COMMENT ON COLUMN teaching_data_instance.source_external_business_id IS '基于老师备案样本复制时的来源原平台业务数据 ID。';
