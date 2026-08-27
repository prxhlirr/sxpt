CREATE TABLE IF NOT EXISTS teacher_record_artifact (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    teacher_user_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    capture_session_id varchar(64),

    source_data_session_id varchar(64) NOT NULL,
    source_data_instance_id varchar(64),
    business_scene_code varchar(128) NOT NULL,
    business_scene_name varchar(255),
    source_external_business_id varchar(128) NOT NULL,
    source_external_business_no varchar(128),
    target_status varchar(64),

    step_snapshot_json jsonb,
    score_rule_snapshot_json jsonb,
    key_field_snapshot_json jsonb,
    origin_context_snapshot_json jsonb,
    artifact_status varchar(32) NOT NULL,

    create_by varchar(64),
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE INDEX IF NOT EXISTS idx_teacher_record_artifact_scene
ON teacher_record_artifact (tenant_id, connector_system_id, business_scene_code)
WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_teacher_record_artifact_capture
ON teacher_record_artifact (tenant_id, capture_session_id)
WHERE deleted = false;

CREATE TABLE IF NOT EXISTS practice_task_scene (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    task_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,

    record_artifact_id varchar(64) NOT NULL,
    business_scene_code varchar(128) NOT NULL,
    business_scene_name varchar(255),
    source_data_session_id varchar(64) NOT NULL,
    source_external_business_id varchar(128) NOT NULL,

    sort_no integer,
    required_flag boolean NOT NULL DEFAULT true,
    score_rule_snapshot_json jsonb,

    create_by varchar(64),
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_practice_task_scene_artifact
ON practice_task_scene (tenant_id, task_id, record_artifact_id)
WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_practice_task_scene_task
ON practice_task_scene (tenant_id, task_id, connector_system_id)
WHERE deleted = false;

COMMENT ON TABLE teacher_record_artifact IS '老师备案产物，保存可发布为练习任务的数据来源和评分依据。';
COMMENT ON COLUMN teacher_record_artifact.business_scene_code IS '原平台稳定业务场景编码，不是页面路径。';
COMMENT ON COLUMN teacher_record_artifact.source_data_session_id IS '老师备案时注册的 RECORD DataSession ID。';
COMMENT ON COLUMN teacher_record_artifact.source_external_business_id IS '老师备案样本在原平台的业务数据 ID。';

COMMENT ON TABLE practice_task_scene IS '练习任务业务场景绑定，一个任务可绑定多个老师备案产物。';
COMMENT ON COLUMN practice_task_scene.sort_no IS '展示顺序，学生可任意顺序完成，不作为强制流程。';
COMMENT ON COLUMN practice_task_scene.source_data_session_id IS '学生练习复制数据时的来源 RECORD DataSession ID。';
COMMENT ON COLUMN practice_task_scene.source_external_business_id IS '学生练习复制数据时的来源原平台业务数据 ID。';
