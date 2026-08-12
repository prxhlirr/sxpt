CREATE TABLE IF NOT EXISTS classic_case_usage (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    case_asset_id varchar(64) NOT NULL,
    case_version_id varchar(64) NOT NULL,
    usage_scene varchar(32) NOT NULL,
    scene_type varchar(32) NOT NULL,
    task_id varchar(64),
    request_batch_id varchar(128) NOT NULL,
    request_item_id varchar(128) NOT NULL,
    owner_user_id varchar(64) NOT NULL,
    question_id varchar(64),
    generated_instance_id varchar(128),
    external_business_no varchar(128),
    external_business_name varchar(256),
    external_status varchar(64),
    target_url varchar(1024),
    request_json jsonb NOT NULL,
    result_json jsonb,
    use_by varchar(64) NOT NULL,
    use_time timestamp NOT NULL DEFAULT now(),
    trace_id varchar(128),
    status varchar(32) NOT NULL DEFAULT 'SUCCESS',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE classic_case_usage IS '经典案例使用记录表：记录老师复刻或学生练习生成学习环境业务数据的请求和结果';
COMMENT ON COLUMN classic_case_usage.usage_scene IS '使用方式：TEACHING_REPLICA 老师/专家复刻，STUDENT_DEMO 学生练习';
COMMENT ON COLUMN classic_case_usage.generated_instance_id IS '学习环境生成的原平台业务数据 ID';
COMMENT ON COLUMN classic_case_usage.request_json IS '调用学习环境 DATA_CREATE 时的请求快照';
COMMENT ON COLUMN classic_case_usage.result_json IS '学习环境生成结果快照';

CREATE INDEX IF NOT EXISTS idx_classic_case_usage_case
    ON classic_case_usage (tenant_id, case_asset_id, case_version_id)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_classic_case_usage_owner
    ON classic_case_usage (tenant_id, owner_user_id, usage_scene)
    WHERE deleted = false;
