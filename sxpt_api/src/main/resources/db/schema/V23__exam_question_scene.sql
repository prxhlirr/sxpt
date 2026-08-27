CREATE TABLE IF NOT EXISTS exam_question_scene (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    task_id varchar(64) NOT NULL,
    execution_id varchar(64),
    exam_attempt_id varchar(64) NOT NULL,
    question_attempt_id varchar(64) NOT NULL,
    question_no bigint,
    connector_system_id varchar(64) NOT NULL,

    business_scene_code varchar(128) NOT NULL,
    business_scene_name varchar(255),
    data_spec_snapshot_json jsonb,
    score_rule_snapshot_json jsonb,

    sort_no integer,
    required_flag boolean NOT NULL DEFAULT true,

    create_by varchar(64),
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_exam_question_scene_attempt
ON exam_question_scene (tenant_id, question_attempt_id)
WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_exam_question_scene_exam
ON exam_question_scene (tenant_id, task_id, exam_attempt_id, connector_system_id)
WHERE deleted = false;

COMMENT ON TABLE exam_question_scene IS '考试题目业务场景绑定，一道题对应一个原平台业务数据。';
COMMENT ON COLUMN exam_question_scene.business_scene_code IS '原平台稳定业务场景编码，不是页面路径。';
COMMENT ON COLUMN exam_question_scene.data_spec_snapshot_json IS '原平台为该题造数所需的规格快照。';
COMMENT ON COLUMN exam_question_scene.question_attempt_id IS '学生本次考试中的题目尝试 ID，用于绑定 DataSession 和评分。';
