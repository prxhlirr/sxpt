-- 业务数字孪生教学平台 MVP 初始化脚本
-- 目标数据库：PostgreSQL 14.8
-- 设计原则：教学平台只保存教学闭环必需的数据契约，原平台真实业务明细仍由原平台负责。

SET client_encoding = 'UTF8';

CREATE TABLE teach_user (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    username varchar(128) NOT NULL,
    real_name varchar(128) NOT NULL,
    phone varchar(32),
    email varchar(128),
    user_type varchar(32) NOT NULL,
    source_type varchar(32) NOT NULL,
    external_info_json jsonb,
    last_sync_time timestamptz,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE teach_role (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    role_code varchar(64) NOT NULL,
    role_name varchar(128) NOT NULL,
    description varchar(512),
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE teach_user_role (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    user_id varchar(64) NOT NULL,
    role_id varchar(64) NOT NULL,
    grant_source varchar(32) NOT NULL,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE connector_system (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    system_code varchar(64) NOT NULL,
    system_name varchar(128) NOT NULL,
    system_type varchar(64) NOT NULL,
    base_url varchar(512) NOT NULL,
    auth_type varchar(32) NOT NULL,
    config_json jsonb,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE identity_binding (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    user_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    external_user_id varchar(128) NOT NULL,
    external_username varchar(128),
    external_role_json jsonb,
    external_org_json jsonb,
    binding_type varchar(32) NOT NULL,
    last_login_time timestamptz,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE teach_org (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    parent_id varchar(64),
    org_code varchar(64) NOT NULL,
    org_name varchar(128) NOT NULL,
    org_type varchar(32) NOT NULL,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE teach_user_org (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    user_id varchar(64) NOT NULL,
    org_id varchar(64) NOT NULL,
    relation_type varchar(32) NOT NULL,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE connector_resource (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    resource_code varchar(128) NOT NULL,
    resource_name varchar(128) NOT NULL,
    resource_type varchar(32) NOT NULL,
    page_url varchar(1024),
    locator varchar(1024),
    stable_key varchar(256),
    metadata_json jsonb,
    source_capture_id varchar(64),
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE platform_launch_context (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    launch_token_hash varchar(128) NOT NULL,
    user_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    task_id varchar(64),
    teaching_point_id varchar(64),
    execution_id varchar(64),
    data_instance_id varchar(64),
    scene_type varchar(32) NOT NULL,
    sdk_mode varchar(32) NOT NULL,
    target_url varchar(1024) NOT NULL,
    segment_no bigint,
    actor_type varchar(64),
    required_external_org_id varchar(128),
    required_external_org_name varchar(256),
    required_external_role_id varchar(128),
    required_external_role_name varchar(256),
    external_business_id varchar(128),
    external_business_no varchar(128),
    data_scope_json jsonb,
    launch_status varchar(32) NOT NULL,
    verified_time timestamptz,
    used_time timestamptz,
    expire_time timestamptz NOT NULL,
    error_message text,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE teaching_data_template (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    teaching_point_id varchar(64),
    template_code varchar(128) NOT NULL,
    template_name varchar(256) NOT NULL,
    scene_type varchar(32) NOT NULL,
    init_state varchar(64),
    support_mode varchar(128),
    config_json jsonb,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE teaching_data_instance (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    template_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    owner_user_id varchar(64),
    class_id varchar(64),
    task_id varchar(64),
    teaching_point_id varchar(64),
    execution_id varchar(64),
    attempt_id varchar(64),
    scene_type varchar(32) NOT NULL,
    external_business_id varchar(128) NOT NULL,
    external_business_no varchar(128),
    external_status varchar(64),
    instance_status varchar(32) NOT NULL,
    reset_count bigint DEFAULT 0,
    lock_time timestamptz,
    expire_time timestamptz,
    metadata_json jsonb,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE capture_session (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    teacher_id varchar(64) NOT NULL,
    session_name varchar(128) NOT NULL,
    business_name varchar(128),
    capture_mode varchar(32) NOT NULL,
    start_url varchar(1024) NOT NULL,
    start_time timestamptz,
    end_time timestamptz,
    session_status varchar(32) NOT NULL,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE capture_event (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    capture_session_id varchar(64) NOT NULL,
    sdk_session_id varchar(128),
    client_event_id varchar(128),
    event_type varchar(32) NOT NULL,
    event_time timestamptz NOT NULL,
    sequence_no bigint NOT NULL,
    retry_count bigint DEFAULT 0,
    page_url varchar(1024),
    target_text varchar(512),
    target_locator varchar(1024),
    target_stable_key varchar(256),
    input_value_masked varchar(1024),
    event_payload_json jsonb,
    archive_status varchar(32) DEFAULT 'NONE',
    archive_time timestamptz,
    expire_time timestamptz,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE capture_resource_snapshot (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    capture_session_id varchar(64) NOT NULL,
    page_url varchar(1024) NOT NULL,
    page_title varchar(256),
    resource_type varchar(32) NOT NULL,
    snapshot_scope varchar(32) NOT NULL,
    resource_name varchar(128),
    resource_locator varchar(1024),
    element_snapshot_json jsonb NOT NULL,
    metadata_json jsonb,
    snapshot_hash varchar(128),
    promoted_resource_id varchar(64),
    archive_status varchar(32) DEFAULT 'NONE',
    archive_time timestamptz,
    expire_time timestamptz,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE capture_action_draft (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    capture_session_id varchar(64) NOT NULL,
    event_id varchar(64),
    action_name varchar(128) NOT NULL,
    action_type varchar(32) NOT NULL,
    sequence_no bigint NOT NULL,
    connector_resource_id varchar(64),
    suggested_operation_name varchar(128),
    confirmed_operation_name varchar(128),
    confirmed_step_name varchar(128),
    guide_content text,
    practice_hint text,
    confirm_status varchar(32) NOT NULL,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE teaching_point (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    point_code varchar(64) NOT NULL,
    point_name varchar(128) NOT NULL,
    point_type varchar(32) NOT NULL,
    version_no bigint NOT NULL DEFAULT 1,
    source_capture_session_id varchar(64),
    business_overview_json jsonb,
    flow_file_id varchar(64),
    flow_file_url varchar(1024),
    record_path_json jsonb,
    required_external_role_id varchar(128),
    required_external_role_name varchar(128),
    execution_strategy varchar(32) NOT NULL,
    default_grant_start_time timestamptz,
    default_grant_end_time timestamptz,
    data_scope_json jsonb,
    overlay_policy_json jsonb,
    description varchar(1024),
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    point_status varchar(32) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE course (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    course_code varchar(64) NOT NULL,
    course_name varchar(128) NOT NULL,
    version_no bigint NOT NULL DEFAULT 1,
    target_org_id varchar(64),
    start_time timestamptz,
    end_time timestamptz,
    description text,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    course_status varchar(32) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE task (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    course_id varchar(64) NOT NULL,
    publish_org_id varchar(64) NOT NULL,
    task_code varchar(64) NOT NULL,
    task_name varchar(128) NOT NULL,
    version_no bigint NOT NULL DEFAULT 1,
    task_type varchar(32) NOT NULL,
    task_goal text NOT NULL,
    task_description text,
    start_time timestamptz,
    end_time timestamptz,
    time_limit_minutes bigint,
    overlay_policy_json jsonb,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    task_status varchar(32) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE task_teaching_point (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    task_id varchar(64) NOT NULL,
    teaching_point_id varchar(64) NOT NULL,
    required_flag boolean NOT NULL DEFAULT true,
    sequence_no bigint NOT NULL,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE task_step (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    task_id varchar(64) NOT NULL,
    teaching_point_id varchar(64) NOT NULL,
    step_code varchar(64) NOT NULL,
    step_name varchar(128) NOT NULL,
    step_description text,
    sequence_no bigint NOT NULL,
    segment_no bigint,
    actor_type varchar(64),
    required_external_org_id varchar(128),
    required_external_org_name varchar(256),
    required_external_role_id varchar(128),
    required_external_role_name varchar(256),
    switch_strategy varchar(32),
    next_segment_no bigint,
    switch_confirm_required boolean,
    switch_decision_source varchar(32),
    switch_reason varchar(512),
    rollback_policy varchar(32),
    related_resource_ids jsonb,
    guide_content text,
    practice_hint text,
    required boolean NOT NULL DEFAULT true,
    allow_skip boolean NOT NULL DEFAULT false,
    source_action_draft_id varchar(64),
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE task_execution (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    task_id varchar(64) NOT NULL,
    student_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    execution_mode varchar(32) NOT NULL,
    sdk_mode varchar(32) NOT NULL,
    execution_identity_status varchar(32),
    execution_identity_json jsonb,
    start_time timestamptz,
    end_time timestamptz,
    execution_status varchar(32) NOT NULL,
    score numeric(8,2),
    result_summary text,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE task_execution_context (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    execution_id varchar(64) NOT NULL,
    task_id varchar(64) NOT NULL,
    student_id varchar(64) NOT NULL,
    sdk_mode varchar(32) NOT NULL,
    context_json jsonb NOT NULL,
    overlay_policy_json jsonb,
    teaching_point_snapshot_json jsonb,
    resource_snapshot_json jsonb,
    evaluation_snapshot_json jsonb,
    archive_status varchar(32) DEFAULT 'NONE',
    archive_time timestamptz,
    expire_time timestamptz,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE execution_trace (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    execution_id varchar(64) NOT NULL,
    sdk_session_id varchar(128),
    client_trace_id varchar(128),
    task_step_id varchar(64),
    teaching_point_id varchar(64),
    resource_id varchar(64),
    trace_type varchar(32) NOT NULL,
    trace_time timestamptz NOT NULL,
    sequence_no bigint NOT NULL,
    retry_count bigint DEFAULT 0,
    input_data_json jsonb,
    output_data_json jsonb,
    before_state_json jsonb,
    after_state_json jsonb,
    evidence_json jsonb,
    success boolean,
    error_message text,
    archive_status varchar(32) DEFAULT 'NONE',
    archive_time timestamptz,
    expire_time timestamptz,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE practice_attempt (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    execution_id varchar(64) NOT NULL,
    student_id varchar(64) NOT NULL,
    class_id varchar(64),
    task_id varchar(64) NOT NULL,
    teaching_point_id varchar(64),
    data_instance_id varchar(64),
    attempt_no bigint NOT NULL,
    start_time timestamptz,
    end_time timestamptz,
    duration_seconds bigint,
    attempt_status varchar(32) NOT NULL,
    score numeric(8,2),
    max_score numeric(8,2),
    pass_flag boolean,
    error_count bigint DEFAULT 0,
    hint_count bigint DEFAULT 0,
    rollback_count bigint DEFAULT 0,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE practice_step_result (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    attempt_id varchar(64) NOT NULL,
    student_id varchar(64) NOT NULL,
    task_id varchar(64) NOT NULL,
    teaching_point_id varchar(64),
    task_step_id varchar(64),
    sequence_no bigint NOT NULL,
    segment_no bigint,
    required_external_org_id varchar(128),
    required_external_role_id varchar(128),
    start_time timestamptz,
    end_time timestamptz,
    duration_seconds bigint,
    step_status varchar(32) NOT NULL,
    score numeric(8,2),
    max_score numeric(8,2),
    error_count bigint DEFAULT 0,
    hint_used boolean,
    rollback_used boolean,
    evaluation_message text,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE practice_score_summary (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    student_id varchar(64) NOT NULL,
    class_id varchar(64),
    course_id varchar(64),
    task_id varchar(64),
    teaching_point_id varchar(64),
    practice_count bigint NOT NULL DEFAULT 0,
    complete_count bigint NOT NULL DEFAULT 0,
    best_score numeric(8,2),
    last_score numeric(8,2),
    avg_score numeric(8,2),
    completion_rate numeric(8,4),
    final_practice_score numeric(8,2),
    score_policy varchar(32) NOT NULL,
    weak_step_json jsonb,
    summary_time timestamptz NOT NULL,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE evaluation_rule (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    rule_code varchar(64) NOT NULL,
    rule_name varchar(128) NOT NULL,
    version_no bigint NOT NULL DEFAULT 1,
    task_id varchar(64),
    teaching_point_id varchar(64),
    total_score numeric(8,2) NOT NULL,
    description varchar(1024),
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE evaluation_item (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    evaluation_rule_id varchar(64) NOT NULL,
    teaching_point_id varchar(64),
    item_code varchar(64) NOT NULL,
    item_name varchar(128) NOT NULL,
    item_type varchar(32) NOT NULL,
    related_resource_id varchar(64),
    related_api_resource_id varchar(64),
    related_task_step_id varchar(64),
    score numeric(8,2) NOT NULL,
    required boolean NOT NULL DEFAULT true,
    assertion_type varchar(32) NOT NULL,
    assertion_config_json jsonb NOT NULL,
    fail_policy varchar(32) NOT NULL,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

CREATE TABLE evaluation_result (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    execution_id varchar(64) NOT NULL,
    evaluation_rule_id varchar(64) NOT NULL,
    auto_score numeric(8,2),
    manual_score numeric(8,2),
    final_score numeric(8,2),
    evaluation_summary text,
    evidence_json jsonb,
    reviewed_by varchar(64),
    reviewed_time timestamptz,
    archive_status varchar(32) DEFAULT 'NONE',
    archive_time timestamptz,
    expire_time timestamptz,
    create_by varchar(64),
    create_time timestamptz NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamptz NOT NULL DEFAULT now(),
    evaluation_status varchar(32) NOT NULL,
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

ALTER TABLE teach_user_role ADD CONSTRAINT fk_teach_user_role_user FOREIGN KEY (user_id) REFERENCES teach_user(id);
ALTER TABLE teach_user_role ADD CONSTRAINT fk_teach_user_role_role FOREIGN KEY (role_id) REFERENCES teach_role(id);
ALTER TABLE identity_binding ADD CONSTRAINT fk_identity_binding_user FOREIGN KEY (user_id) REFERENCES teach_user(id);
ALTER TABLE identity_binding ADD CONSTRAINT fk_identity_binding_connector FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);
ALTER TABLE teach_user_org ADD CONSTRAINT fk_teach_user_org_user FOREIGN KEY (user_id) REFERENCES teach_user(id);
ALTER TABLE teach_user_org ADD CONSTRAINT fk_teach_user_org_org FOREIGN KEY (org_id) REFERENCES teach_org(id);
ALTER TABLE connector_resource ADD CONSTRAINT fk_connector_resource_system FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);
ALTER TABLE platform_launch_context ADD CONSTRAINT fk_launch_context_user FOREIGN KEY (user_id) REFERENCES teach_user(id);
ALTER TABLE platform_launch_context ADD CONSTRAINT fk_launch_context_connector FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);
ALTER TABLE teaching_data_template ADD CONSTRAINT fk_data_template_connector FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);
ALTER TABLE teaching_data_instance ADD CONSTRAINT fk_data_instance_template FOREIGN KEY (template_id) REFERENCES teaching_data_template(id);
ALTER TABLE teaching_data_instance ADD CONSTRAINT fk_data_instance_connector FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);
ALTER TABLE capture_session ADD CONSTRAINT fk_capture_session_connector FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);
ALTER TABLE capture_session ADD CONSTRAINT fk_capture_session_teacher FOREIGN KEY (teacher_id) REFERENCES teach_user(id);
ALTER TABLE capture_action_draft ADD CONSTRAINT fk_action_draft_session FOREIGN KEY (capture_session_id) REFERENCES capture_session(id);
ALTER TABLE teaching_point ADD CONSTRAINT fk_teaching_point_connector FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);
ALTER TABLE task ADD CONSTRAINT fk_task_course FOREIGN KEY (course_id) REFERENCES course(id);
ALTER TABLE task ADD CONSTRAINT fk_task_publish_org FOREIGN KEY (publish_org_id) REFERENCES teach_org(id);
ALTER TABLE task_teaching_point ADD CONSTRAINT fk_task_point_task FOREIGN KEY (task_id) REFERENCES task(id);
ALTER TABLE task_teaching_point ADD CONSTRAINT fk_task_point_point FOREIGN KEY (teaching_point_id) REFERENCES teaching_point(id);
ALTER TABLE task_step ADD CONSTRAINT fk_task_step_task FOREIGN KEY (task_id) REFERENCES task(id);
ALTER TABLE task_step ADD CONSTRAINT fk_task_step_point FOREIGN KEY (teaching_point_id) REFERENCES teaching_point(id);
ALTER TABLE task_execution ADD CONSTRAINT fk_task_execution_task FOREIGN KEY (task_id) REFERENCES task(id);
ALTER TABLE task_execution ADD CONSTRAINT fk_task_execution_student FOREIGN KEY (student_id) REFERENCES teach_user(id);
ALTER TABLE task_execution ADD CONSTRAINT fk_task_execution_connector FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);
ALTER TABLE task_execution_context ADD CONSTRAINT fk_execution_context_execution FOREIGN KEY (execution_id) REFERENCES task_execution(id);
ALTER TABLE execution_trace ADD CONSTRAINT fk_execution_trace_execution FOREIGN KEY (execution_id) REFERENCES task_execution(id);
ALTER TABLE practice_attempt ADD CONSTRAINT fk_practice_attempt_execution FOREIGN KEY (execution_id) REFERENCES task_execution(id);
ALTER TABLE practice_attempt ADD CONSTRAINT fk_practice_attempt_student FOREIGN KEY (student_id) REFERENCES teach_user(id);
ALTER TABLE practice_attempt ADD CONSTRAINT fk_practice_attempt_task FOREIGN KEY (task_id) REFERENCES task(id);
ALTER TABLE practice_step_result ADD CONSTRAINT fk_practice_step_attempt FOREIGN KEY (attempt_id) REFERENCES practice_attempt(id);
ALTER TABLE practice_score_summary ADD CONSTRAINT fk_practice_summary_student FOREIGN KEY (student_id) REFERENCES teach_user(id);
ALTER TABLE evaluation_item ADD CONSTRAINT fk_evaluation_item_rule FOREIGN KEY (evaluation_rule_id) REFERENCES evaluation_rule(id);
ALTER TABLE evaluation_result ADD CONSTRAINT fk_evaluation_result_execution FOREIGN KEY (execution_id) REFERENCES task_execution(id);
ALTER TABLE evaluation_result ADD CONSTRAINT fk_evaluation_result_rule FOREIGN KEY (evaluation_rule_id) REFERENCES evaluation_rule(id);

CREATE UNIQUE INDEX uk_teach_user_tenant_username ON teach_user (tenant_id, username) WHERE deleted = false;
CREATE UNIQUE INDEX uk_teach_role_tenant_code ON teach_role (tenant_id, role_code) WHERE deleted = false;
CREATE UNIQUE INDEX uk_teach_user_role_tenant_user_role ON teach_user_role (tenant_id, user_id, role_id) WHERE deleted = false;
CREATE UNIQUE INDEX uk_identity_binding_external_user ON identity_binding (tenant_id, connector_system_id, external_user_id) WHERE deleted = false;
CREATE UNIQUE INDEX uk_teach_org_tenant_code ON teach_org (tenant_id, org_code) WHERE deleted = false;
CREATE UNIQUE INDEX uk_teach_user_org_tenant_user_org ON teach_user_org (tenant_id, user_id, org_id) WHERE deleted = false;
CREATE UNIQUE INDEX uk_connector_system_tenant_code ON connector_system (tenant_id, system_code) WHERE deleted = false;
CREATE UNIQUE INDEX uk_connector_resource_tenant_code ON connector_resource (tenant_id, connector_system_id, resource_code) WHERE deleted = false;
CREATE UNIQUE INDEX uk_platform_launch_token_hash ON platform_launch_context (tenant_id, launch_token_hash) WHERE deleted = false;
CREATE UNIQUE INDEX uk_data_template_tenant_code ON teaching_data_template (tenant_id, connector_system_id, template_code) WHERE deleted = false;
CREATE UNIQUE INDEX uk_data_instance_external_business ON teaching_data_instance (tenant_id, connector_system_id, external_business_id) WHERE deleted = false;
CREATE UNIQUE INDEX uk_capture_event_client_event ON capture_event (tenant_id, capture_session_id, client_event_id) WHERE deleted = false AND client_event_id IS NOT NULL;
CREATE UNIQUE INDEX uk_task_point_tenant_task_point ON task_teaching_point (tenant_id, task_id, teaching_point_id) WHERE deleted = false;
CREATE UNIQUE INDEX uk_task_execution_context_execution ON task_execution_context (tenant_id, execution_id) WHERE deleted = false;
CREATE UNIQUE INDEX uk_execution_trace_client_trace ON execution_trace (tenant_id, execution_id, client_trace_id) WHERE deleted = false AND client_trace_id IS NOT NULL;
CREATE UNIQUE INDEX uk_practice_step_result_step ON practice_step_result (tenant_id, attempt_id, task_step_id) WHERE deleted = false AND task_step_id IS NOT NULL;
CREATE UNIQUE INDEX uk_practice_score_summary_scope ON practice_score_summary (tenant_id, student_id, task_id, teaching_point_id) WHERE deleted = false;
CREATE UNIQUE INDEX uk_evaluation_rule_tenant_code ON evaluation_rule (tenant_id, rule_code, version_no) WHERE deleted = false;
CREATE UNIQUE INDEX uk_evaluation_item_tenant_code ON evaluation_item (tenant_id, evaluation_rule_id, item_code) WHERE deleted = false;
CREATE UNIQUE INDEX uk_evaluation_result_execution_rule ON evaluation_result (tenant_id, execution_id, evaluation_rule_id) WHERE deleted = false;

CREATE INDEX idx_teach_user_type ON teach_user (tenant_id, user_type, status) WHERE deleted = false;
CREATE INDEX idx_identity_binding_user ON identity_binding (tenant_id, user_id) WHERE deleted = false;
CREATE INDEX idx_teach_org_parent ON teach_org (tenant_id, parent_id) WHERE deleted = false;
CREATE INDEX idx_connector_resource_page ON connector_resource (tenant_id, connector_system_id, page_url) WHERE deleted = false;
CREATE INDEX idx_launch_context_user_scene ON platform_launch_context (tenant_id, user_id, scene_type, create_time DESC) WHERE deleted = false;
CREATE INDEX idx_launch_context_task_execution ON platform_launch_context (tenant_id, task_id, execution_id) WHERE deleted = false;
CREATE INDEX idx_data_instance_owner_scene ON teaching_data_instance (tenant_id, owner_user_id, scene_type, instance_status) WHERE deleted = false;
CREATE INDEX idx_capture_session_teacher ON capture_session (tenant_id, teacher_id, session_status) WHERE deleted = false;
CREATE INDEX idx_capture_event_session_time ON capture_event (tenant_id, capture_session_id, event_time);
CREATE INDEX idx_capture_event_archive ON capture_event (archive_status, expire_time);
CREATE INDEX idx_capture_snapshot_session ON capture_resource_snapshot (tenant_id, capture_session_id, create_time DESC);
CREATE INDEX idx_capture_snapshot_hash ON capture_resource_snapshot (tenant_id, snapshot_hash) WHERE snapshot_hash IS NOT NULL;
CREATE INDEX idx_action_draft_session_sequence ON capture_action_draft (tenant_id, capture_session_id, sequence_no) WHERE deleted = false;
CREATE UNIQUE INDEX uk_action_draft_session_event ON capture_action_draft (tenant_id, capture_session_id, event_id) WHERE deleted = false AND event_id IS NOT NULL;
CREATE INDEX idx_teaching_point_connector ON teaching_point (tenant_id, connector_system_id, point_status) WHERE deleted = false;
CREATE INDEX idx_course_target_org ON course (tenant_id, target_org_id, course_status) WHERE deleted = false;
CREATE INDEX idx_task_course_org ON task (tenant_id, course_id, publish_org_id, task_status) WHERE deleted = false;
CREATE INDEX idx_task_step_task_sequence ON task_step (tenant_id, task_id, sequence_no) WHERE deleted = false;
CREATE UNIQUE INDEX uk_task_step_source_action_draft ON task_step (tenant_id, source_action_draft_id) WHERE deleted = false AND source_action_draft_id IS NOT NULL;
CREATE INDEX idx_task_execution_student_task ON task_execution (tenant_id, student_id, task_id, execution_status) WHERE deleted = false;
CREATE INDEX idx_execution_trace_execution_time ON execution_trace (tenant_id, execution_id, trace_time);
CREATE INDEX idx_execution_trace_archive ON execution_trace (archive_status, expire_time);
CREATE INDEX idx_practice_attempt_student_task ON practice_attempt (tenant_id, student_id, task_id, attempt_no DESC) WHERE deleted = false;
CREATE INDEX idx_practice_step_attempt_sequence ON practice_step_result (tenant_id, attempt_id, sequence_no) WHERE deleted = false;
CREATE INDEX idx_evaluation_rule_task_point ON evaluation_rule (tenant_id, task_id, teaching_point_id) WHERE deleted = false;
CREATE INDEX idx_evaluation_result_status ON evaluation_result (tenant_id, evaluation_status, reviewed_by) WHERE deleted = false;
CREATE INDEX idx_evaluation_result_archive ON evaluation_result (archive_status, expire_time);

COMMENT ON TABLE teach_user IS '教学平台用户表：保存教学平台识别教师、学生、管理员所需的最小用户镜像。';
COMMENT ON TABLE teach_role IS '教学平台角色表：保存教学平台内部角色，独立于原平台角色体系。';
COMMENT ON TABLE teach_user_role IS '用户角色关联表：维护教学用户与教学角色之间的授权关系。';
COMMENT ON TABLE connector_system IS '原业务平台表：保存接入的原业务平台信息及 SDK 配置。';
COMMENT ON TABLE identity_binding IS '原平台身份绑定表：记录教学用户与原平台账号、角色和组织快照的绑定关系。';
COMMENT ON TABLE teach_org IS '教学组织表：保存教学平台自己的班级、课程班或分组。';
COMMENT ON TABLE teach_user_org IS '用户教学组织关联表：维护学生、教师与班级或课程班的关系。';
COMMENT ON TABLE connector_resource IS '原平台页面资源表：保存被教学步骤、遮罩或评分引用的原平台页面资源。';
COMMENT ON TABLE platform_launch_context IS '原平台启动上下文表：记录每次跳转原平台的短令牌、执行身份、场景和业务数据范围。';
COMMENT ON TABLE teaching_data_template IS '教学业务数据模板表：描述备案、学习、练习、考试所需的原平台初始数据类型。';
COMMENT ON TABLE teaching_data_instance IS '教学业务数据实例表：保存某次教学运行实际使用的原平台业务数据引用。';
COMMENT ON TABLE capture_session IS '采集会话表：记录教师在原平台进行一次教学点采集标注的过程。';
COMMENT ON TABLE capture_event IS '采集事件表：保存 SDK 采集到的原平台页面操作事实。';
COMMENT ON TABLE capture_resource_snapshot IS '资源快照表：保存采集过程中的关键资源摘要快照，默认不保存全量 DOM。';
COMMENT ON TABLE capture_action_draft IS '页面动作草稿表：保存由采集事件整理出的教师可确认页面动作。';
COMMENT ON TABLE teaching_point IS '教学点表：保存教师发布后的可教学业务单元，是 MVP 的核心资产。';
COMMENT ON TABLE course IS '课程表：定义一门课程或一学期实训课程。';
COMMENT ON TABLE task IS '教学任务表：定义学习、练习或考试任务。';
COMMENT ON TABLE task_teaching_point IS '任务教学点关联表：维护任务与教学点之间的多对多关系。';
COMMENT ON TABLE task_step IS '教学任务步骤表：定义学生在学习和练习中看到的步骤、提示和角色切换建议。';
COMMENT ON TABLE task_execution IS '任务执行表：记录学生一次学习、练习或考试任务执行。';
COMMENT ON TABLE task_execution_context IS '任务执行上下文快照表：保存 SDK 任务开始时获得的运行上下文快照。';
COMMENT ON TABLE execution_trace IS '执行轨迹表：记录学生在原平台中的操作轨迹和评分证据引用。';
COMMENT ON TABLE practice_attempt IS '练习次数表：记录学生每一次练习过程。';
COMMENT ON TABLE practice_step_result IS '练习步骤结果表：记录某次练习中每个关键步骤的完成、错误、提示和得分。';
COMMENT ON TABLE practice_score_summary IS '练习过程分汇总表：按学生、任务、教学点汇总练习过程分。';
COMMENT ON TABLE evaluation_rule IS '评价规则表：定义任务或教学点的评分规则集合。';
COMMENT ON TABLE evaluation_item IS '评分项表：保存教师确认的关键评分点和断言配置。';
COMMENT ON TABLE evaluation_result IS '评价结果表：保存自动评分、教师复核和评分证据摘要。';

COMMENT ON COLUMN teach_user.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN teach_user.tenant_id IS '租户 ID，用于隔离不同教学租户的数据。';
COMMENT ON COLUMN teach_user.username IS '用户账号，可来自原平台账号。';
COMMENT ON COLUMN teach_user.real_name IS '用户真实姓名。';
COMMENT ON COLUMN teach_user.phone IS '手机号，返回前端和日志中需要脱敏。';
COMMENT ON COLUMN teach_user.email IS '邮箱地址。';
COMMENT ON COLUMN teach_user.user_type IS '用户类型：TEACHER、STUDENT、ADMIN。';
COMMENT ON COLUMN teach_user.source_type IS '用户来源：EXTERNAL、LOCAL。';
COMMENT ON COLUMN teach_user.external_info_json IS '原平台返回的用户扩展信息快照。';
COMMENT ON COLUMN teach_user.last_sync_time IS '最近一次从原平台同步用户信息的时间。';
COMMENT ON COLUMN teach_user.create_by IS '创建人 ID。';
COMMENT ON COLUMN teach_user.create_time IS '创建时间。';
COMMENT ON COLUMN teach_user.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN teach_user.update_time IS '最后更新时间。';
COMMENT ON COLUMN teach_user.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN teach_user.deleted IS '软删除标记。';

COMMENT ON COLUMN teach_role.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN teach_role.tenant_id IS '租户 ID。';
COMMENT ON COLUMN teach_role.role_code IS '角色编码，如 TEACHER、STUDENT、ADMIN。';
COMMENT ON COLUMN teach_role.role_name IS '角色名称，如教师、学生、管理员。';
COMMENT ON COLUMN teach_role.description IS '角色说明。';
COMMENT ON COLUMN teach_role.create_by IS '创建人 ID。';
COMMENT ON COLUMN teach_role.create_time IS '创建时间。';
COMMENT ON COLUMN teach_role.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN teach_role.update_time IS '最后更新时间。';
COMMENT ON COLUMN teach_role.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN teach_role.deleted IS '软删除标记。';

COMMENT ON COLUMN teach_user_role.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN teach_user_role.tenant_id IS '租户 ID。';
COMMENT ON COLUMN teach_user_role.user_id IS '教学平台用户 ID。';
COMMENT ON COLUMN teach_user_role.role_id IS '教学平台角色 ID。';
COMMENT ON COLUMN teach_user_role.grant_source IS '授权来源：MANUAL、SYNC、SYSTEM。';
COMMENT ON COLUMN teach_user_role.create_by IS '创建人 ID。';
COMMENT ON COLUMN teach_user_role.create_time IS '创建时间。';
COMMENT ON COLUMN teach_user_role.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN teach_user_role.update_time IS '最后更新时间。';
COMMENT ON COLUMN teach_user_role.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN teach_user_role.deleted IS '软删除标记。';

COMMENT ON COLUMN connector_system.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN connector_system.tenant_id IS '租户 ID。';
COMMENT ON COLUMN connector_system.system_code IS '原平台编码。';
COMMENT ON COLUMN connector_system.system_name IS '原平台名称。';
COMMENT ON COLUMN connector_system.system_type IS '系统类型：OA、ERP、CUSTOM。';
COMMENT ON COLUMN connector_system.base_url IS '原平台基础地址。';
COMMENT ON COLUMN connector_system.auth_type IS '认证方式：SSO、TOKEN、COOKIE。';
COMMENT ON COLUMN connector_system.config_json IS '原平台连接配置、SDK 配置、授权策略配置。';
COMMENT ON COLUMN connector_system.create_by IS '创建人 ID。';
COMMENT ON COLUMN connector_system.create_time IS '创建时间。';
COMMENT ON COLUMN connector_system.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN connector_system.update_time IS '最后更新时间。';
COMMENT ON COLUMN connector_system.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN connector_system.deleted IS '软删除标记。';

COMMENT ON COLUMN identity_binding.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN identity_binding.tenant_id IS '租户 ID。';
COMMENT ON COLUMN identity_binding.user_id IS '教学平台用户 ID。';
COMMENT ON COLUMN identity_binding.connector_system_id IS '原业务平台 ID。';
COMMENT ON COLUMN identity_binding.external_user_id IS '原平台用户 ID。';
COMMENT ON COLUMN identity_binding.external_username IS '原平台账号。';
COMMENT ON COLUMN identity_binding.external_role_json IS '原平台角色信息快照，MVP 不单独拆角色表。';
COMMENT ON COLUMN identity_binding.external_org_json IS '原平台组织信息快照，MVP 不单独拆组织映射。';
COMMENT ON COLUMN identity_binding.binding_type IS '绑定类型：REAL、TRAINING、TEMP。';
COMMENT ON COLUMN identity_binding.last_login_time IS '最近登录时间。';
COMMENT ON COLUMN identity_binding.create_by IS '创建人 ID。';
COMMENT ON COLUMN identity_binding.create_time IS '创建时间。';
COMMENT ON COLUMN identity_binding.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN identity_binding.update_time IS '最后更新时间。';
COMMENT ON COLUMN identity_binding.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN identity_binding.deleted IS '软删除标记。';

COMMENT ON COLUMN teach_org.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN teach_org.tenant_id IS '租户 ID。';
COMMENT ON COLUMN teach_org.parent_id IS '父级组织 ID，组织层级属于弱引用。';
COMMENT ON COLUMN teach_org.org_code IS '教学组织编码。';
COMMENT ON COLUMN teach_org.org_name IS '教学组织名称。';
COMMENT ON COLUMN teach_org.org_type IS '组织类型：CLASS、COURSE_CLASS、GROUP。';
COMMENT ON COLUMN teach_org.create_by IS '创建人 ID。';
COMMENT ON COLUMN teach_org.create_time IS '创建时间。';
COMMENT ON COLUMN teach_org.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN teach_org.update_time IS '最后更新时间。';
COMMENT ON COLUMN teach_org.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN teach_org.deleted IS '软删除标记。';

COMMENT ON COLUMN teach_user_org.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN teach_user_org.tenant_id IS '租户 ID。';
COMMENT ON COLUMN teach_user_org.user_id IS '用户 ID。';
COMMENT ON COLUMN teach_user_org.org_id IS '教学组织 ID。';
COMMENT ON COLUMN teach_user_org.relation_type IS '关系类型：STUDENT、TEACHER、MANAGER。';
COMMENT ON COLUMN teach_user_org.create_by IS '创建人 ID。';
COMMENT ON COLUMN teach_user_org.create_time IS '创建时间。';
COMMENT ON COLUMN teach_user_org.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN teach_user_org.update_time IS '最后更新时间。';
COMMENT ON COLUMN teach_user_org.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN teach_user_org.deleted IS '软删除标记。';

COMMENT ON COLUMN connector_resource.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN connector_resource.tenant_id IS '租户 ID。';
COMMENT ON COLUMN connector_resource.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN connector_resource.resource_code IS '资源编码。';
COMMENT ON COLUMN connector_resource.resource_name IS '资源名称。';
COMMENT ON COLUMN connector_resource.resource_type IS '资源类型：PAGE、FIELD、BUTTON、FORM、UPLOAD、STATE、API。';
COMMENT ON COLUMN connector_resource.page_url IS '所属页面地址。';
COMMENT ON COLUMN connector_resource.locator IS '元素定位表达式。';
COMMENT ON COLUMN connector_resource.stable_key IS '稳定定位键，如字段 name、组件 key。';
COMMENT ON COLUMN connector_resource.metadata_json IS '元数据，如字段类型、按钮文本、必填标记。';
COMMENT ON COLUMN connector_resource.source_capture_id IS '来源采集会话 ID，保留弱引用。';
COMMENT ON COLUMN connector_resource.create_by IS '创建人 ID。';
COMMENT ON COLUMN connector_resource.create_time IS '创建时间。';
COMMENT ON COLUMN connector_resource.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN connector_resource.update_time IS '最后更新时间。';
COMMENT ON COLUMN connector_resource.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN connector_resource.deleted IS '软删除标记。';

COMMENT ON COLUMN platform_launch_context.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN platform_launch_context.tenant_id IS '租户 ID。';
COMMENT ON COLUMN platform_launch_context.launch_token_hash IS '启动令牌 hash，数据库不保存明文 token。';
COMMENT ON COLUMN platform_launch_context.user_id IS '教学平台用户 ID。';
COMMENT ON COLUMN platform_launch_context.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN platform_launch_context.task_id IS '任务 ID，教师备案阶段可为空，保留弱引用。';
COMMENT ON COLUMN platform_launch_context.teaching_point_id IS '教学点 ID，备案草稿阶段可为空，保留弱引用。';
COMMENT ON COLUMN platform_launch_context.execution_id IS '任务执行 ID，教师备案阶段可为空，保留弱引用。';
COMMENT ON COLUMN platform_launch_context.data_instance_id IS '教学业务数据实例 ID，保留弱引用。';
COMMENT ON COLUMN platform_launch_context.scene_type IS '场景类型：RECORD、LEARN、PRACTICE、EXAM。';
COMMENT ON COLUMN platform_launch_context.sdk_mode IS 'SDK 模式：CAPTURE、LEARNING、PRACTICE、EXAM。';
COMMENT ON COLUMN platform_launch_context.target_url IS '原平台目标页面地址。';
COMMENT ON COLUMN platform_launch_context.segment_no IS '多角色分段编号。';
COMMENT ON COLUMN platform_launch_context.actor_type IS '业务参与方类型，如 APPLICANT、ACCEPTOR、AUDITOR、APPROVER。';
COMMENT ON COLUMN platform_launch_context.required_external_org_id IS '本次进入原平台需要切换的原平台单位 ID。';
COMMENT ON COLUMN platform_launch_context.required_external_org_name IS '本次进入原平台需要切换的原平台单位名称。';
COMMENT ON COLUMN platform_launch_context.required_external_role_id IS '本次进入原平台需要切换的原平台角色 ID。';
COMMENT ON COLUMN platform_launch_context.required_external_role_name IS '本次进入原平台需要切换的原平台角色名称。';
COMMENT ON COLUMN platform_launch_context.external_business_id IS '原平台业务数据 ID。';
COMMENT ON COLUMN platform_launch_context.external_business_no IS '原平台业务单据号。';
COMMENT ON COLUMN platform_launch_context.data_scope_json IS '本次允许操作的数据范围。';
COMMENT ON COLUMN platform_launch_context.launch_status IS '启动状态：CREATED、VERIFIED、USED、EXPIRED、FAILED。';
COMMENT ON COLUMN platform_launch_context.verified_time IS '原平台完成 token 校验时间。';
COMMENT ON COLUMN platform_launch_context.used_time IS '原平台完成 session 建立时间。';
COMMENT ON COLUMN platform_launch_context.expire_time IS 'token 过期时间。';
COMMENT ON COLUMN platform_launch_context.error_message IS '原平台启动失败原因。';
COMMENT ON COLUMN platform_launch_context.create_by IS '创建人 ID。';
COMMENT ON COLUMN platform_launch_context.create_time IS '创建时间。';
COMMENT ON COLUMN platform_launch_context.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN platform_launch_context.update_time IS '最后更新时间。';
COMMENT ON COLUMN platform_launch_context.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN platform_launch_context.deleted IS '软删除标记。';

COMMENT ON COLUMN teaching_data_template.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN teaching_data_template.tenant_id IS '租户 ID。';
COMMENT ON COLUMN teaching_data_template.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN teaching_data_template.teaching_point_id IS '教学点 ID，备案前可为空，保留弱引用。';
COMMENT ON COLUMN teaching_data_template.template_code IS '原平台可识别的数据模板编码。';
COMMENT ON COLUMN teaching_data_template.template_name IS '数据模板名称。';
COMMENT ON COLUMN teaching_data_template.scene_type IS '适用场景：RECORD、LEARN、PRACTICE、EXAM、COMMON。';
COMMENT ON COLUMN teaching_data_template.init_state IS '原平台业务数据初始状态，如 DRAFT、WAIT_SUBMIT。';
COMMENT ON COLUMN teaching_data_template.support_mode IS '支持模式集合，如 RECORD,PRACTICE,EXAM。';
COMMENT ON COLUMN teaching_data_template.config_json IS '造数规则、默认字段、数据池策略等配置。';
COMMENT ON COLUMN teaching_data_template.create_by IS '创建人 ID。';
COMMENT ON COLUMN teaching_data_template.create_time IS '创建时间。';
COMMENT ON COLUMN teaching_data_template.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN teaching_data_template.update_time IS '最后更新时间。';
COMMENT ON COLUMN teaching_data_template.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN teaching_data_template.deleted IS '软删除标记。';

COMMENT ON COLUMN teaching_data_instance.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN teaching_data_instance.tenant_id IS '租户 ID。';
COMMENT ON COLUMN teaching_data_instance.template_id IS '教学业务数据模板 ID。';
COMMENT ON COLUMN teaching_data_instance.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN teaching_data_instance.owner_user_id IS '使用人 ID，可能是教师或学生，保留弱引用。';
COMMENT ON COLUMN teaching_data_instance.class_id IS '班级 ID，保留弱引用。';
COMMENT ON COLUMN teaching_data_instance.task_id IS '任务 ID，保留弱引用。';
COMMENT ON COLUMN teaching_data_instance.teaching_point_id IS '教学点 ID，保留弱引用。';
COMMENT ON COLUMN teaching_data_instance.execution_id IS '任务执行 ID，保留弱引用。';
COMMENT ON COLUMN teaching_data_instance.attempt_id IS '练习次数 ID，保留弱引用。';
COMMENT ON COLUMN teaching_data_instance.scene_type IS '场景类型：RECORD、LEARN、PRACTICE、EXAM。';
COMMENT ON COLUMN teaching_data_instance.external_business_id IS '原平台业务数据 ID。';
COMMENT ON COLUMN teaching_data_instance.external_business_no IS '原平台业务单据号。';
COMMENT ON COLUMN teaching_data_instance.external_status IS '原平台业务数据当前状态摘要。';
COMMENT ON COLUMN teaching_data_instance.instance_status IS '实例状态：CREATED、IN_USE、COMPLETED、LOCKED、DISCARDED、RESET。';
COMMENT ON COLUMN teaching_data_instance.reset_count IS '重置次数。';
COMMENT ON COLUMN teaching_data_instance.lock_time IS '锁定时间，考试数据结束后应锁定。';
COMMENT ON COLUMN teaching_data_instance.expire_time IS '数据实例建议过期时间。';
COMMENT ON COLUMN teaching_data_instance.metadata_json IS '原平台返回的数据摘要，禁止保存敏感原文。';
COMMENT ON COLUMN teaching_data_instance.create_by IS '创建人 ID。';
COMMENT ON COLUMN teaching_data_instance.create_time IS '创建时间。';
COMMENT ON COLUMN teaching_data_instance.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN teaching_data_instance.update_time IS '最后更新时间。';
COMMENT ON COLUMN teaching_data_instance.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN teaching_data_instance.deleted IS '软删除标记。';

COMMENT ON COLUMN capture_session.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN capture_session.tenant_id IS '租户 ID。';
COMMENT ON COLUMN capture_session.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN capture_session.teacher_id IS '教师用户 ID。';
COMMENT ON COLUMN capture_session.session_name IS '采集会话名称。';
COMMENT ON COLUMN capture_session.business_name IS '业务名称，如标准备案申请。';
COMMENT ON COLUMN capture_session.capture_mode IS '采集模式：LIGHT、STANDARD、DEBUG，默认 STANDARD。';
COMMENT ON COLUMN capture_session.start_url IS '采集开始页面。';
COMMENT ON COLUMN capture_session.start_time IS '开始时间。';
COMMENT ON COLUMN capture_session.end_time IS '结束时间。';
COMMENT ON COLUMN capture_session.session_status IS '会话状态：RUNNING、FINISHED、CONFIRMED、PUBLISHED。';
COMMENT ON COLUMN capture_session.create_by IS '创建人 ID。';
COMMENT ON COLUMN capture_session.create_time IS '创建时间。';
COMMENT ON COLUMN capture_session.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN capture_session.update_time IS '最后更新时间。';
COMMENT ON COLUMN capture_session.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN capture_session.deleted IS '软删除标记。';

COMMENT ON COLUMN capture_event.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN capture_event.tenant_id IS '租户 ID。';
COMMENT ON COLUMN capture_event.capture_session_id IS '采集会话 ID，作为历史事实保留弱引用。';
COMMENT ON COLUMN capture_event.sdk_session_id IS 'SDK 本地会话 ID，用于排查浏览器端采集链路。';
COMMENT ON COLUMN capture_event.client_event_id IS 'SDK 生成的客户端事件 ID，用于批量上报幂等去重。';
COMMENT ON COLUMN capture_event.event_type IS '事件类型：PAGE_VIEW、CLICK、INPUT、SELECT、UPLOAD、SUBMIT、ROUTE_CHANGE。';
COMMENT ON COLUMN capture_event.event_time IS '事件时间。';
COMMENT ON COLUMN capture_event.sequence_no IS '事件顺序号。';
COMMENT ON COLUMN capture_event.retry_count IS 'SDK 重试上报次数。';
COMMENT ON COLUMN capture_event.page_url IS '页面地址。';
COMMENT ON COLUMN capture_event.target_text IS '目标元素文本。';
COMMENT ON COLUMN capture_event.target_locator IS '目标元素定位。';
COMMENT ON COLUMN capture_event.target_stable_key IS '目标元素稳定键。';
COMMENT ON COLUMN capture_event.input_value_masked IS '脱敏后的输入值。';
COMMENT ON COLUMN capture_event.event_payload_json IS '原始事件摘要数据，禁止保存完整 DOM 和敏感原文。';
COMMENT ON COLUMN capture_event.archive_status IS '归档状态：NONE、READY、ARCHIVED。';
COMMENT ON COLUMN capture_event.archive_time IS '归档时间。';
COMMENT ON COLUMN capture_event.expire_time IS '建议清理或转冷存储时间。';
COMMENT ON COLUMN capture_event.create_by IS '创建人 ID。';
COMMENT ON COLUMN capture_event.create_time IS '创建时间。';
COMMENT ON COLUMN capture_event.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN capture_event.update_time IS '最后更新时间。';
COMMENT ON COLUMN capture_event.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN capture_event.deleted IS '软删除标记。';

COMMENT ON COLUMN capture_resource_snapshot.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN capture_resource_snapshot.tenant_id IS '租户 ID。';
COMMENT ON COLUMN capture_resource_snapshot.capture_session_id IS '采集会话 ID，作为历史事实保留弱引用。';
COMMENT ON COLUMN capture_resource_snapshot.page_url IS '页面地址。';
COMMENT ON COLUMN capture_resource_snapshot.page_title IS '页面标题。';
COMMENT ON COLUMN capture_resource_snapshot.resource_type IS '快照资源类型：PAGE_SUMMARY、FIELD、BUTTON、FORM、UPLOAD、STATE。';
COMMENT ON COLUMN capture_resource_snapshot.snapshot_scope IS '快照范围：TARGET_ELEMENT、PAGE_SUMMARY、DEBUG_FULL_DOM。';
COMMENT ON COLUMN capture_resource_snapshot.resource_name IS '资源名称。';
COMMENT ON COLUMN capture_resource_snapshot.resource_locator IS '资源定位。';
COMMENT ON COLUMN capture_resource_snapshot.element_snapshot_json IS '关键元素摘要快照，默认不保存完整页面 DOM。';
COMMENT ON COLUMN capture_resource_snapshot.metadata_json IS '元数据，如采集原因、是否被操作、是否被标注、是否用于评分。';
COMMENT ON COLUMN capture_resource_snapshot.snapshot_hash IS '快照内容哈希，用于识别重复快照。';
COMMENT ON COLUMN capture_resource_snapshot.promoted_resource_id IS '转为正式资源后的 ID，保留弱引用。';
COMMENT ON COLUMN capture_resource_snapshot.archive_status IS '归档状态：NONE、READY、ARCHIVED。';
COMMENT ON COLUMN capture_resource_snapshot.archive_time IS '归档时间。';
COMMENT ON COLUMN capture_resource_snapshot.expire_time IS '建议清理或转冷存储时间。';
COMMENT ON COLUMN capture_resource_snapshot.create_by IS '创建人 ID。';
COMMENT ON COLUMN capture_resource_snapshot.create_time IS '创建时间。';
COMMENT ON COLUMN capture_resource_snapshot.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN capture_resource_snapshot.update_time IS '最后更新时间。';
COMMENT ON COLUMN capture_resource_snapshot.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN capture_resource_snapshot.deleted IS '软删除标记。';

COMMENT ON COLUMN capture_action_draft.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN capture_action_draft.tenant_id IS '租户 ID。';
COMMENT ON COLUMN capture_action_draft.capture_session_id IS '采集会话 ID。';
COMMENT ON COLUMN capture_action_draft.event_id IS '来源事件 ID，部分草稿可能由多个事件合并生成。';
COMMENT ON COLUMN capture_action_draft.action_name IS '页面动作名称。';
COMMENT ON COLUMN capture_action_draft.action_type IS '动作类型：OPEN_PAGE、CLICK、INPUT、SELECT、UPLOAD、ASSERT_STATE。';
COMMENT ON COLUMN capture_action_draft.sequence_no IS '顺序号。';
COMMENT ON COLUMN capture_action_draft.connector_resource_id IS '关联正式资源 ID，草稿阶段允许为空。';
COMMENT ON COLUMN capture_action_draft.suggested_operation_name IS '系统建议业务操作名称。';
COMMENT ON COLUMN capture_action_draft.confirmed_operation_name IS '教师确认业务操作名称。';
COMMENT ON COLUMN capture_action_draft.confirmed_step_name IS '教师确认教学步骤名称。';
COMMENT ON COLUMN capture_action_draft.guide_content IS '学习模式提示内容。';
COMMENT ON COLUMN capture_action_draft.practice_hint IS '练习模式提示内容。';
COMMENT ON COLUMN capture_action_draft.confirm_status IS '确认状态：PENDING、CONFIRMED、DISCARDED。';
COMMENT ON COLUMN capture_action_draft.create_by IS '创建人 ID。';
COMMENT ON COLUMN capture_action_draft.create_time IS '创建时间。';
COMMENT ON COLUMN capture_action_draft.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN capture_action_draft.update_time IS '最后更新时间。';
COMMENT ON COLUMN capture_action_draft.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN capture_action_draft.deleted IS '软删除标记。';

COMMENT ON COLUMN teaching_point.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN teaching_point.tenant_id IS '租户 ID。';
COMMENT ON COLUMN teaching_point.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN teaching_point.point_code IS '教学点编码。';
COMMENT ON COLUMN teaching_point.point_name IS '教学点名称，如标准备案申请。';
COMMENT ON COLUMN teaching_point.point_type IS '教学点类型：MODULE、SCENARIO、OPERATION_GROUP。';
COMMENT ON COLUMN teaching_point.version_no IS '教学点版本号，发布后变更需要递增。';
COMMENT ON COLUMN teaching_point.source_capture_session_id IS '来源采集会话 ID，教学点发布后不依赖采集会话可用性。';
COMMENT ON COLUMN teaching_point.business_overview_json IS '业务总说明，包括目标、参与角色、前置条件、最终结果、教学重点和评分重点。';
COMMENT ON COLUMN teaching_point.flow_file_id IS '上传流程图文件 ID，MVP 仅作为附件引用，不参与自动流转。';
COMMENT ON COLUMN teaching_point.flow_file_url IS '上传流程图访问地址。';
COMMENT ON COLUMN teaching_point.record_path_json IS '备案路径清单，记录发布时确认的角色顺序、单位、片段和切换说明。';
COMMENT ON COLUMN teaching_point.required_external_role_id IS '原平台所需执行角色 ID，仅引用原平台角色，不定义权限。';
COMMENT ON COLUMN teaching_point.required_external_role_name IS '原平台所需执行角色名称。';
COMMENT ON COLUMN teaching_point.execution_strategy IS '执行策略：ROLE_SWITCH、TRAINING_ACCOUNT、TEMP_GRANT、VALIDATE_ONLY。';
COMMENT ON COLUMN teaching_point.default_grant_start_time IS '教学点默认授权开始时间，仅作为创建任务时的建议值。';
COMMENT ON COLUMN teaching_point.default_grant_end_time IS '教学点默认授权结束时间，仅作为创建任务时的建议值。';
COMMENT ON COLUMN teaching_point.data_scope_json IS '数据范围，如训练数据、班级数据、个人任务数据。';
COMMENT ON COLUMN teaching_point.overlay_policy_json IS '默认遮罩策略。';
COMMENT ON COLUMN teaching_point.description IS '教学点说明。';
COMMENT ON COLUMN teaching_point.create_by IS '创建人 ID。';
COMMENT ON COLUMN teaching_point.create_time IS '创建时间。';
COMMENT ON COLUMN teaching_point.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN teaching_point.update_time IS '最后更新时间。';
COMMENT ON COLUMN teaching_point.point_status IS '教学点状态：DRAFT、PUBLISHED、DISABLED。';
COMMENT ON COLUMN teaching_point.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN teaching_point.deleted IS '软删除标记。';

COMMENT ON COLUMN course.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN course.tenant_id IS '租户 ID。';
COMMENT ON COLUMN course.course_code IS '课程编码。';
COMMENT ON COLUMN course.course_name IS '课程名称。';
COMMENT ON COLUMN course.version_no IS '课程版本号，发布后变更需要递增。';
COMMENT ON COLUMN course.target_org_id IS '默认授课班级 ID，课程历史应保留原班级引用。';
COMMENT ON COLUMN course.start_time IS '课程开始时间。';
COMMENT ON COLUMN course.end_time IS '课程结束时间。';
COMMENT ON COLUMN course.description IS '课程说明。';
COMMENT ON COLUMN course.create_by IS '创建人 ID。';
COMMENT ON COLUMN course.create_time IS '创建时间。';
COMMENT ON COLUMN course.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN course.update_time IS '最后更新时间。';
COMMENT ON COLUMN course.course_status IS '课程状态：DRAFT、PUBLISHED、DISABLED。';
COMMENT ON COLUMN course.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN course.deleted IS '软删除标记。';

COMMENT ON COLUMN task.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN task.tenant_id IS '租户 ID。';
COMMENT ON COLUMN task.course_id IS '课程 ID。';
COMMENT ON COLUMN task.publish_org_id IS '发布班级 ID。';
COMMENT ON COLUMN task.task_code IS '任务编码。';
COMMENT ON COLUMN task.task_name IS '任务名称。';
COMMENT ON COLUMN task.version_no IS '任务版本号，发布后变更需要递增。';
COMMENT ON COLUMN task.task_type IS '任务类型：LEARNING、PRACTICE、EXAM。';
COMMENT ON COLUMN task.task_goal IS '任务目标。';
COMMENT ON COLUMN task.task_description IS '任务说明。';
COMMENT ON COLUMN task.start_time IS '任务开始时间。';
COMMENT ON COLUMN task.end_time IS '任务结束时间。';
COMMENT ON COLUMN task.time_limit_minutes IS '限制时长，单位分钟。';
COMMENT ON COLUMN task.overlay_policy_json IS '遮罩策略配置。';
COMMENT ON COLUMN task.create_by IS '创建人 ID。';
COMMENT ON COLUMN task.create_time IS '创建时间。';
COMMENT ON COLUMN task.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN task.update_time IS '最后更新时间。';
COMMENT ON COLUMN task.task_status IS '任务状态：DRAFT、PUBLISHED、CLOSED。';
COMMENT ON COLUMN task.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN task.deleted IS '软删除标记。';

COMMENT ON COLUMN task_teaching_point.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN task_teaching_point.tenant_id IS '租户 ID。';
COMMENT ON COLUMN task_teaching_point.task_id IS '任务 ID。';
COMMENT ON COLUMN task_teaching_point.teaching_point_id IS '教学点 ID。';
COMMENT ON COLUMN task_teaching_point.required_flag IS '是否必考或必学。';
COMMENT ON COLUMN task_teaching_point.sequence_no IS '排序号。';
COMMENT ON COLUMN task_teaching_point.create_by IS '创建人 ID。';
COMMENT ON COLUMN task_teaching_point.create_time IS '创建时间。';
COMMENT ON COLUMN task_teaching_point.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN task_teaching_point.update_time IS '最后更新时间。';
COMMENT ON COLUMN task_teaching_point.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN task_teaching_point.deleted IS '软删除标记。';

COMMENT ON COLUMN task_step.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN task_step.tenant_id IS '租户 ID。';
COMMENT ON COLUMN task_step.task_id IS '任务 ID。';
COMMENT ON COLUMN task_step.teaching_point_id IS '教学点 ID。';
COMMENT ON COLUMN task_step.step_code IS '步骤编码。';
COMMENT ON COLUMN task_step.step_name IS '步骤名称。';
COMMENT ON COLUMN task_step.step_description IS '步骤说明。';
COMMENT ON COLUMN task_step.sequence_no IS '排序号。';
COMMENT ON COLUMN task_step.segment_no IS '多角色业务片段编号，用于表达完整业务中的不同参与方阶段。';
COMMENT ON COLUMN task_step.actor_type IS '业务参与方类型，如 APPLICANT、ACCEPTOR、AUDITOR、APPROVER。';
COMMENT ON COLUMN task_step.required_external_org_id IS '执行该步骤需要的原平台单位 ID。';
COMMENT ON COLUMN task_step.required_external_org_name IS '执行该步骤需要的原平台单位名称。';
COMMENT ON COLUMN task_step.required_external_role_id IS '执行该步骤需要的原平台角色 ID。';
COMMENT ON COLUMN task_step.required_external_role_name IS '执行该步骤需要的原平台角色名称。';
COMMENT ON COLUMN task_step.switch_strategy IS '身份切换策略：AUTO、MANUAL_CONFIRM。';
COMMENT ON COLUMN task_step.next_segment_no IS '当前步骤完成后建议切换到的下一个业务片段编号。';
COMMENT ON COLUMN task_step.switch_confirm_required IS '是否要求老师或学生确认后再切换身份，MVP 多角色切换建议为 true。';
COMMENT ON COLUMN task_step.switch_decision_source IS '下一片段建议来源：TEACHER_PATH、ORIGIN_STATE、MANUAL。';
COMMENT ON COLUMN task_step.switch_reason IS '老师调整下一角色或下一片段时填写的原因。';
COMMENT ON COLUMN task_step.rollback_policy IS '练习上一步策略：UI_ONLY、CHECKPOINT、RESTART_ONLY、FORBIDDEN。';
COMMENT ON COLUMN task_step.related_resource_ids IS '关联原平台资源 ID 列表。';
COMMENT ON COLUMN task_step.guide_content IS '学习模式提示。';
COMMENT ON COLUMN task_step.practice_hint IS '练习模式提示。';
COMMENT ON COLUMN task_step.required IS '是否必做。';
COMMENT ON COLUMN task_step.allow_skip IS '是否允许跳过。';
COMMENT ON COLUMN task_step.source_action_draft_id IS '来源动作草稿 ID，发布后不依赖草稿可用性。';
COMMENT ON COLUMN task_step.create_by IS '创建人 ID。';
COMMENT ON COLUMN task_step.create_time IS '创建时间。';
COMMENT ON COLUMN task_step.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN task_step.update_time IS '最后更新时间。';
COMMENT ON COLUMN task_step.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN task_step.deleted IS '软删除标记。';

COMMENT ON COLUMN task_execution.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN task_execution.tenant_id IS '租户 ID。';
COMMENT ON COLUMN task_execution.task_id IS '任务 ID。';
COMMENT ON COLUMN task_execution.student_id IS '学生用户 ID。';
COMMENT ON COLUMN task_execution.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN task_execution.execution_mode IS '执行模式：LEARNING、PRACTICE、EXAM。';
COMMENT ON COLUMN task_execution.sdk_mode IS 'SDK 模式：LEARNING、PRACTICE、EXAM。';
COMMENT ON COLUMN task_execution.execution_identity_status IS '原平台执行身份状态：NONE、READY、SWITCHED、GRANTED、FAILED。';
COMMENT ON COLUMN task_execution.execution_identity_json IS '本次任务原平台执行身份，包含 identityMode、activeExternalRoleId、contextToken、expireTime、dataScope。';
COMMENT ON COLUMN task_execution.start_time IS '开始时间。';
COMMENT ON COLUMN task_execution.end_time IS '结束时间。';
COMMENT ON COLUMN task_execution.execution_status IS '执行状态：RUNNING、SUBMITTED、COMPLETED、FAILED。';
COMMENT ON COLUMN task_execution.score IS '得分。';
COMMENT ON COLUMN task_execution.result_summary IS '结果摘要。';
COMMENT ON COLUMN task_execution.create_by IS '创建人 ID。';
COMMENT ON COLUMN task_execution.create_time IS '创建时间。';
COMMENT ON COLUMN task_execution.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN task_execution.update_time IS '最后更新时间。';
COMMENT ON COLUMN task_execution.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN task_execution.deleted IS '软删除标记。';

COMMENT ON COLUMN task_execution_context.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN task_execution_context.tenant_id IS '租户 ID。';
COMMENT ON COLUMN task_execution_context.execution_id IS '任务执行 ID。';
COMMENT ON COLUMN task_execution_context.task_id IS '任务 ID。';
COMMENT ON COLUMN task_execution_context.student_id IS '学生 ID。';
COMMENT ON COLUMN task_execution_context.sdk_mode IS 'SDK 模式。';
COMMENT ON COLUMN task_execution_context.context_json IS '下发给 SDK 的完整上下文。';
COMMENT ON COLUMN task_execution_context.overlay_policy_json IS '遮罩策略快照。';
COMMENT ON COLUMN task_execution_context.teaching_point_snapshot_json IS '教学点快照。';
COMMENT ON COLUMN task_execution_context.resource_snapshot_json IS '资源定位快照。';
COMMENT ON COLUMN task_execution_context.evaluation_snapshot_json IS '评分点快照。';
COMMENT ON COLUMN task_execution_context.archive_status IS '归档状态：NONE、READY、ARCHIVED。';
COMMENT ON COLUMN task_execution_context.archive_time IS '归档时间。';
COMMENT ON COLUMN task_execution_context.expire_time IS '建议清理或转冷存储时间。';
COMMENT ON COLUMN task_execution_context.create_by IS '创建人 ID。';
COMMENT ON COLUMN task_execution_context.create_time IS '创建时间。';
COMMENT ON COLUMN task_execution_context.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN task_execution_context.update_time IS '最后更新时间。';
COMMENT ON COLUMN task_execution_context.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN task_execution_context.deleted IS '软删除标记。';

COMMENT ON COLUMN execution_trace.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN execution_trace.tenant_id IS '租户 ID。';
COMMENT ON COLUMN execution_trace.execution_id IS '任务执行 ID。';
COMMENT ON COLUMN execution_trace.sdk_session_id IS 'SDK 本地会话 ID。';
COMMENT ON COLUMN execution_trace.client_trace_id IS 'SDK 生成的客户端轨迹 ID，用于批量上报幂等去重。';
COMMENT ON COLUMN execution_trace.task_step_id IS '教学步骤 ID，考试轨迹可能无法完全匹配步骤。';
COMMENT ON COLUMN execution_trace.teaching_point_id IS '教学点 ID，保留执行时教学点引用。';
COMMENT ON COLUMN execution_trace.resource_id IS '原平台资源 ID，页面资源可能后续变更。';
COMMENT ON COLUMN execution_trace.trace_type IS '轨迹类型：CLICK、INPUT、SELECT、UPLOAD、STATE、HINT_REQUEST。';
COMMENT ON COLUMN execution_trace.trace_time IS '轨迹时间。';
COMMENT ON COLUMN execution_trace.sequence_no IS '顺序号。';
COMMENT ON COLUMN execution_trace.retry_count IS 'SDK 重试上报次数。';
COMMENT ON COLUMN execution_trace.input_data_json IS '输入数据摘要，必须脱敏。';
COMMENT ON COLUMN execution_trace.output_data_json IS '输出数据摘要，禁止保存完整原平台响应。';
COMMENT ON COLUMN execution_trace.before_state_json IS '操作前状态。';
COMMENT ON COLUMN execution_trace.after_state_json IS '操作后状态。';
COMMENT ON COLUMN execution_trace.evidence_json IS '证据数据，只保存评分和复核需要的摘要。';
COMMENT ON COLUMN execution_trace.success IS '是否成功。';
COMMENT ON COLUMN execution_trace.error_message IS '错误信息。';
COMMENT ON COLUMN execution_trace.archive_status IS '归档状态：NONE、READY、ARCHIVED。';
COMMENT ON COLUMN execution_trace.archive_time IS '归档时间。';
COMMENT ON COLUMN execution_trace.expire_time IS '建议清理或转冷存储时间。';
COMMENT ON COLUMN execution_trace.create_by IS '创建人 ID。';
COMMENT ON COLUMN execution_trace.create_time IS '创建时间。';
COMMENT ON COLUMN execution_trace.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN execution_trace.update_time IS '最后更新时间。';
COMMENT ON COLUMN execution_trace.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN execution_trace.deleted IS '软删除标记。';

COMMENT ON COLUMN practice_attempt.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN practice_attempt.tenant_id IS '租户 ID。';
COMMENT ON COLUMN practice_attempt.execution_id IS '任务执行 ID。';
COMMENT ON COLUMN practice_attempt.student_id IS '学生 ID。';
COMMENT ON COLUMN practice_attempt.class_id IS '班级 ID，保留弱引用。';
COMMENT ON COLUMN practice_attempt.task_id IS '任务 ID。';
COMMENT ON COLUMN practice_attempt.teaching_point_id IS '教学点 ID，保留执行时教学点引用。';
COMMENT ON COLUMN practice_attempt.data_instance_id IS '本次练习使用的原平台业务数据实例 ID。';
COMMENT ON COLUMN practice_attempt.attempt_no IS '第几次练习。';
COMMENT ON COLUMN practice_attempt.start_time IS '开始时间。';
COMMENT ON COLUMN practice_attempt.end_time IS '结束时间。';
COMMENT ON COLUMN practice_attempt.duration_seconds IS '练习耗时秒数。';
COMMENT ON COLUMN practice_attempt.attempt_status IS '练习状态：RUNNING、COMPLETED、ABANDONED、ERROR。';
COMMENT ON COLUMN practice_attempt.score IS '本次练习得分。';
COMMENT ON COLUMN practice_attempt.max_score IS '本次练习满分。';
COMMENT ON COLUMN practice_attempt.pass_flag IS '是否达标。';
COMMENT ON COLUMN practice_attempt.error_count IS '错误次数。';
COMMENT ON COLUMN practice_attempt.hint_count IS '使用提示次数。';
COMMENT ON COLUMN practice_attempt.rollback_count IS '上一步或重开次数。';
COMMENT ON COLUMN practice_attempt.create_by IS '创建人 ID。';
COMMENT ON COLUMN practice_attempt.create_time IS '创建时间。';
COMMENT ON COLUMN practice_attempt.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN practice_attempt.update_time IS '最后更新时间。';
COMMENT ON COLUMN practice_attempt.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN practice_attempt.deleted IS '软删除标记。';

COMMENT ON COLUMN practice_step_result.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN practice_step_result.tenant_id IS '租户 ID。';
COMMENT ON COLUMN practice_step_result.attempt_id IS '练习次数 ID。';
COMMENT ON COLUMN practice_step_result.student_id IS '学生 ID。';
COMMENT ON COLUMN practice_step_result.task_id IS '任务 ID。';
COMMENT ON COLUMN practice_step_result.teaching_point_id IS '教学点 ID。';
COMMENT ON COLUMN practice_step_result.task_step_id IS '教学步骤 ID，历史步骤结果不应被后续步骤变更破坏。';
COMMENT ON COLUMN practice_step_result.sequence_no IS '步骤顺序号。';
COMMENT ON COLUMN practice_step_result.segment_no IS '多角色业务片段编号。';
COMMENT ON COLUMN practice_step_result.required_external_org_id IS '该步骤实际要求的原平台单位 ID。';
COMMENT ON COLUMN practice_step_result.required_external_role_id IS '该步骤实际要求的原平台角色 ID。';
COMMENT ON COLUMN practice_step_result.start_time IS '步骤开始时间。';
COMMENT ON COLUMN practice_step_result.end_time IS '步骤结束时间。';
COMMENT ON COLUMN practice_step_result.duration_seconds IS '步骤耗时秒数。';
COMMENT ON COLUMN practice_step_result.step_status IS '步骤状态：NOT_STARTED、RUNNING、CORRECT、WRONG、SKIPPED、TIMEOUT。';
COMMENT ON COLUMN practice_step_result.score IS '步骤得分。';
COMMENT ON COLUMN practice_step_result.max_score IS '步骤满分。';
COMMENT ON COLUMN practice_step_result.error_count IS '步骤错误次数。';
COMMENT ON COLUMN practice_step_result.hint_used IS '是否使用提示。';
COMMENT ON COLUMN practice_step_result.rollback_used IS '是否使用上一步或重开。';
COMMENT ON COLUMN practice_step_result.evaluation_message IS '步骤评价说明。';
COMMENT ON COLUMN practice_step_result.create_by IS '创建人 ID。';
COMMENT ON COLUMN practice_step_result.create_time IS '创建时间。';
COMMENT ON COLUMN practice_step_result.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN practice_step_result.update_time IS '最后更新时间。';
COMMENT ON COLUMN practice_step_result.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN practice_step_result.deleted IS '软删除标记。';

COMMENT ON COLUMN practice_score_summary.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN practice_score_summary.tenant_id IS '租户 ID。';
COMMENT ON COLUMN practice_score_summary.student_id IS '学生 ID。';
COMMENT ON COLUMN practice_score_summary.class_id IS '班级 ID。';
COMMENT ON COLUMN practice_score_summary.course_id IS '课程 ID。';
COMMENT ON COLUMN practice_score_summary.task_id IS '任务 ID。';
COMMENT ON COLUMN practice_score_summary.teaching_point_id IS '教学点 ID。';
COMMENT ON COLUMN practice_score_summary.practice_count IS '练习总次数。';
COMMENT ON COLUMN practice_score_summary.complete_count IS '完成次数。';
COMMENT ON COLUMN practice_score_summary.best_score IS '最高分。';
COMMENT ON COLUMN practice_score_summary.last_score IS '最近一次得分。';
COMMENT ON COLUMN practice_score_summary.avg_score IS '平均分。';
COMMENT ON COLUMN practice_score_summary.completion_rate IS '完成率。';
COMMENT ON COLUMN practice_score_summary.final_practice_score IS '最终练习过程分。';
COMMENT ON COLUMN practice_score_summary.score_policy IS '汇总策略：BEST、LAST、WEIGHTED。';
COMMENT ON COLUMN practice_score_summary.weak_step_json IS '薄弱步骤摘要。';
COMMENT ON COLUMN practice_score_summary.summary_time IS '汇总时间。';
COMMENT ON COLUMN practice_score_summary.create_by IS '创建人 ID。';
COMMENT ON COLUMN practice_score_summary.create_time IS '创建时间。';
COMMENT ON COLUMN practice_score_summary.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN practice_score_summary.update_time IS '最后更新时间。';
COMMENT ON COLUMN practice_score_summary.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN practice_score_summary.deleted IS '软删除标记。';

COMMENT ON COLUMN evaluation_rule.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN evaluation_rule.tenant_id IS '租户 ID。';
COMMENT ON COLUMN evaluation_rule.rule_code IS '规则编码。';
COMMENT ON COLUMN evaluation_rule.rule_name IS '规则名称。';
COMMENT ON COLUMN evaluation_rule.version_no IS '评分规则版本号，发布后变更需要递增。';
COMMENT ON COLUMN evaluation_rule.task_id IS '适用任务 ID，评分规则可按任务或教学点配置。';
COMMENT ON COLUMN evaluation_rule.teaching_point_id IS '适用教学点 ID，评分规则可按任务或教学点配置。';
COMMENT ON COLUMN evaluation_rule.total_score IS '总分。';
COMMENT ON COLUMN evaluation_rule.description IS '说明。';
COMMENT ON COLUMN evaluation_rule.create_by IS '创建人 ID。';
COMMENT ON COLUMN evaluation_rule.create_time IS '创建时间。';
COMMENT ON COLUMN evaluation_rule.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN evaluation_rule.update_time IS '最后更新时间。';
COMMENT ON COLUMN evaluation_rule.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN evaluation_rule.deleted IS '软删除标记。';

COMMENT ON COLUMN evaluation_item.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN evaluation_item.tenant_id IS '租户 ID。';
COMMENT ON COLUMN evaluation_item.evaluation_rule_id IS '评价规则 ID。';
COMMENT ON COLUMN evaluation_item.teaching_point_id IS '教学点 ID，支持任务级评分项不绑定单个教学点。';
COMMENT ON COLUMN evaluation_item.item_code IS '评分项编码。';
COMMENT ON COLUMN evaluation_item.item_name IS '评分项名称。';
COMMENT ON COLUMN evaluation_item.item_type IS '评分项类型：RESULT、KEY_ACTION、BUSINESS_RULE、RISK。';
COMMENT ON COLUMN evaluation_item.related_resource_id IS '关联原平台资源 ID。';
COMMENT ON COLUMN evaluation_item.related_api_resource_id IS '关联原平台结果查询 API 资源 ID，用于最终业务结果校验。';
COMMENT ON COLUMN evaluation_item.related_task_step_id IS '关联教学步骤 ID。';
COMMENT ON COLUMN evaluation_item.score IS '分值。';
COMMENT ON COLUMN evaluation_item.required IS '是否必得项。';
COMMENT ON COLUMN evaluation_item.assertion_type IS '断言类型：TRACE_EXISTS、FIELD_EQUALS、STATE_EQUALS、RESULT_EXISTS、EXTERNAL_API_RESULT、CUSTOM。';
COMMENT ON COLUMN evaluation_item.assertion_config_json IS '断言配置。';
COMMENT ON COLUMN evaluation_item.fail_policy IS '失败策略：NO_SCORE、DEDUCT、REVIEW。';
COMMENT ON COLUMN evaluation_item.create_by IS '创建人 ID。';
COMMENT ON COLUMN evaluation_item.create_time IS '创建时间。';
COMMENT ON COLUMN evaluation_item.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN evaluation_item.update_time IS '最后更新时间。';
COMMENT ON COLUMN evaluation_item.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN evaluation_item.deleted IS '软删除标记。';

COMMENT ON COLUMN evaluation_result.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN evaluation_result.tenant_id IS '租户 ID。';
COMMENT ON COLUMN evaluation_result.execution_id IS '任务执行 ID。';
COMMENT ON COLUMN evaluation_result.evaluation_rule_id IS '评价规则 ID。';
COMMENT ON COLUMN evaluation_result.auto_score IS '自动评分。';
COMMENT ON COLUMN evaluation_result.manual_score IS '人工评分。';
COMMENT ON COLUMN evaluation_result.final_score IS '最终评分。';
COMMENT ON COLUMN evaluation_result.evaluation_summary IS '评价摘要。';
COMMENT ON COLUMN evaluation_result.evidence_json IS '评分明细和证据，MVP 不单独拆 evaluation_detail。';
COMMENT ON COLUMN evaluation_result.reviewed_by IS '复核人 ID。';
COMMENT ON COLUMN evaluation_result.reviewed_time IS '复核时间。';
COMMENT ON COLUMN evaluation_result.archive_status IS '归档状态：NONE、READY、ARCHIVED。';
COMMENT ON COLUMN evaluation_result.archive_time IS '归档时间。';
COMMENT ON COLUMN evaluation_result.expire_time IS '建议清理或转冷存储时间。';
COMMENT ON COLUMN evaluation_result.create_by IS '创建人 ID。';
COMMENT ON COLUMN evaluation_result.create_time IS '创建时间。';
COMMENT ON COLUMN evaluation_result.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN evaluation_result.update_time IS '最后更新时间。';
COMMENT ON COLUMN evaluation_result.evaluation_status IS '评价状态：PENDING、AUTO_EVALUATED、REVIEWED。';
COMMENT ON COLUMN evaluation_result.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN evaluation_result.deleted IS '软删除标记。';
