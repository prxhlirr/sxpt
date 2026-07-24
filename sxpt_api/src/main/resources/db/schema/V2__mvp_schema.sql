CREATE TABLE platform_capability (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    capability_code varchar(64) NOT NULL,
    capability_name varchar(128) NOT NULL,
    capability_type varchar(32) NOT NULL,
    support_flag boolean NOT NULL DEFAULT true,
    endpoint_url varchar(512),
    method varchar(16),
    request_schema_json jsonb,
    response_schema_json jsonb,
    timeout_ms bigint,
    retry_policy_json jsonb,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE platform_capability IS '原平台能力声明表：声明原平台支持哪些数据准备、锁定、校验和归档能力。';
COMMENT ON COLUMN platform_capability.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN platform_capability.tenant_id IS '租户 ID。';
COMMENT ON COLUMN platform_capability.connector_system_id IS '原平台 ID，关联 connector_system.id。';
COMMENT ON COLUMN platform_capability.capability_code IS '能力编码，如 DATA_CREATE、DATA_QUERY、DATA_LOCK、RESULT_CHECK、DATA_ARCHIVE。';
COMMENT ON COLUMN platform_capability.capability_name IS '能力名称。';
COMMENT ON COLUMN platform_capability.capability_type IS '能力类型：DATA_CREATE、DATA_QUERY、DATA_LOCK、RESULT_CHECK、DATA_ARCHIVE。';
COMMENT ON COLUMN platform_capability.support_flag IS '是否支持该能力。';
COMMENT ON COLUMN platform_capability.endpoint_url IS '原平台能力接口地址。';
COMMENT ON COLUMN platform_capability.method IS 'HTTP 方法。';
COMMENT ON COLUMN platform_capability.request_schema_json IS '请求结构描述。';
COMMENT ON COLUMN platform_capability.response_schema_json IS '响应结构描述。';
COMMENT ON COLUMN platform_capability.timeout_ms IS '调用超时时间，单位毫秒。';
COMMENT ON COLUMN platform_capability.retry_policy_json IS '重试策略配置。';
COMMENT ON COLUMN platform_capability.create_by IS '创建人 ID。';
COMMENT ON COLUMN platform_capability.create_time IS '创建时间。';
COMMENT ON COLUMN platform_capability.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN platform_capability.update_time IS '最后更新时间。';
COMMENT ON COLUMN platform_capability.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN platform_capability.deleted IS '软删除标记。';

CREATE UNIQUE INDEX uk_platform_capability_code
    ON platform_capability (tenant_id, connector_system_id, capability_code)
    WHERE deleted = false;

CREATE TABLE module_data_strategy (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    module_code varchar(128) NOT NULL,
    module_name varchar(256) NOT NULL,
    scene_type varchar(32) NOT NULL,
    need_pre_data boolean NOT NULL DEFAULT true,
    data_source_strategy varchar(32) NOT NULL,
    init_external_status varchar(64),
    target_external_status varchar(64),
    default_org_role_policy_json jsonb,
    share_policy varchar(32) NOT NULL,
    regenerate_policy varchar(32) NOT NULL,
    lock_policy varchar(32) NOT NULL,
    expire_policy_json jsonb,
    result_check_policy_json jsonb,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE module_data_strategy IS '功能模块数据策略表：定义某原平台某功能模块在备案、学习、练习、考试场景下如何准备数据。';
COMMENT ON COLUMN module_data_strategy.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN module_data_strategy.tenant_id IS '租户 ID。';
COMMENT ON COLUMN module_data_strategy.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN module_data_strategy.module_code IS '原平台功能模块编码。';
COMMENT ON COLUMN module_data_strategy.module_name IS '原平台功能模块名称。';
COMMENT ON COLUMN module_data_strategy.scene_type IS '教学场景：RECORD、LEARN、PRACTICE、EXAM。';
COMMENT ON COLUMN module_data_strategy.need_pre_data IS '是否需要前置业务数据。';
COMMENT ON COLUMN module_data_strategy.data_source_strategy IS '数据来源策略：PUSH_DESENSITIZED、PULL_ORIGIN、MOCK_GENERATE、MANUAL_POOL。';
COMMENT ON COLUMN module_data_strategy.init_external_status IS '原平台业务数据初始状态。';
COMMENT ON COLUMN module_data_strategy.target_external_status IS '原平台业务数据目标状态。';
COMMENT ON COLUMN module_data_strategy.default_org_role_policy_json IS '默认单位角色策略。';
COMMENT ON COLUMN module_data_strategy.share_policy IS '共享策略：SHARED_READONLY、ATTEMPT_EXCLUSIVE、STUDENT_EXCLUSIVE、QUESTION_EXCLUSIVE。';
COMMENT ON COLUMN module_data_strategy.regenerate_policy IS '重生成策略：NEVER、ON_ATTEMPT、ON_RETAKE、ON_FAILURE。';
COMMENT ON COLUMN module_data_strategy.lock_policy IS '锁定策略：NONE、ON_ALLOCATE、ON_EXAM_START。';
COMMENT ON COLUMN module_data_strategy.expire_policy_json IS '过期和归档策略。';
COMMENT ON COLUMN module_data_strategy.result_check_policy_json IS '原平台结果校验策略。';
COMMENT ON COLUMN module_data_strategy.create_by IS '创建人 ID。';
COMMENT ON COLUMN module_data_strategy.create_time IS '创建时间。';
COMMENT ON COLUMN module_data_strategy.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN module_data_strategy.update_time IS '最后更新时间。';
COMMENT ON COLUMN module_data_strategy.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN module_data_strategy.deleted IS '软删除标记。';

CREATE UNIQUE INDEX uk_module_data_strategy_scene
    ON module_data_strategy (tenant_id, connector_system_id, module_code, scene_type)
    WHERE deleted = false;

CREATE TABLE lesson_plan (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    teaching_point_id varchar(64) NOT NULL,
    module_code varchar(128) NOT NULL,
    plan_code varchar(128) NOT NULL,
    plan_name varchar(256) NOT NULL,
    plan_goal text NOT NULL,
    business_summary text,
    version_no bigint NOT NULL DEFAULT 1,
    publish_status varchar(32) NOT NULL DEFAULT 'DRAFT',
    source_capture_session_id varchar(64),
    metadata_json jsonb,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE lesson_plan IS '教案表：承载老师备案后形成的教学组织单元，是流程分支和实操题库的来源。';
COMMENT ON COLUMN lesson_plan.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN lesson_plan.tenant_id IS '租户 ID。';
COMMENT ON COLUMN lesson_plan.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN lesson_plan.teaching_point_id IS '来源教学点 ID。';
COMMENT ON COLUMN lesson_plan.module_code IS '原平台功能模块编码。';
COMMENT ON COLUMN lesson_plan.plan_code IS '教案编码。';
COMMENT ON COLUMN lesson_plan.plan_name IS '教案名称。';
COMMENT ON COLUMN lesson_plan.plan_goal IS '教学目标。';
COMMENT ON COLUMN lesson_plan.business_summary IS '业务说明。';
COMMENT ON COLUMN lesson_plan.version_no IS '版本号，发布后变更需要递增。';
COMMENT ON COLUMN lesson_plan.publish_status IS '发布状态：DRAFT、PUBLISHED、DISABLED。';
COMMENT ON COLUMN lesson_plan.source_capture_session_id IS '来源采集会话 ID。';
COMMENT ON COLUMN lesson_plan.metadata_json IS '教案扩展摘要。';
COMMENT ON COLUMN lesson_plan.create_by IS '创建人 ID。';
COMMENT ON COLUMN lesson_plan.create_time IS '创建时间。';
COMMENT ON COLUMN lesson_plan.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN lesson_plan.update_time IS '最后更新时间。';
COMMENT ON COLUMN lesson_plan.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN lesson_plan.deleted IS '软删除标记。';

CREATE UNIQUE INDEX uk_lesson_plan_code
    ON lesson_plan (tenant_id, connector_system_id, plan_code, version_no)
    WHERE deleted = false;

CREATE TABLE operation_flow_path (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    lesson_plan_id varchar(64) NOT NULL,
    path_code varchar(128) NOT NULL,
    path_name varchar(256) NOT NULL,
    path_type varchar(32) NOT NULL,
    module_code varchar(128) NOT NULL,
    step_snapshot_json jsonb NOT NULL,
    initial_data_state varchar(64),
    target_data_state varchar(64),
    difficulty varchar(32),
    publish_status varchar(32) NOT NULL DEFAULT 'DRAFT',
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE operation_flow_path IS '业务流程分支表：表达教案下的正常、补正、驳回等流程分支。';
COMMENT ON COLUMN operation_flow_path.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN operation_flow_path.tenant_id IS '租户 ID。';
COMMENT ON COLUMN operation_flow_path.lesson_plan_id IS '教案 ID。';
COMMENT ON COLUMN operation_flow_path.path_code IS '流程分支编码。';
COMMENT ON COLUMN operation_flow_path.path_name IS '流程分支名称。';
COMMENT ON COLUMN operation_flow_path.path_type IS '分支类型：NORMAL、BRANCH、REWORK、REJECT、CUSTOM。';
COMMENT ON COLUMN operation_flow_path.module_code IS '原平台功能模块编码。';
COMMENT ON COLUMN operation_flow_path.step_snapshot_json IS '步骤、片段、单位、角色快照。';
COMMENT ON COLUMN operation_flow_path.initial_data_state IS '所需原平台初始数据状态。';
COMMENT ON COLUMN operation_flow_path.target_data_state IS '目标原平台数据状态。';
COMMENT ON COLUMN operation_flow_path.difficulty IS '难度：EASY、NORMAL、HARD。';
COMMENT ON COLUMN operation_flow_path.publish_status IS '发布状态：DRAFT、PUBLISHED、DISABLED。';
COMMENT ON COLUMN operation_flow_path.create_by IS '创建人 ID。';
COMMENT ON COLUMN operation_flow_path.create_time IS '创建时间。';
COMMENT ON COLUMN operation_flow_path.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN operation_flow_path.update_time IS '最后更新时间。';
COMMENT ON COLUMN operation_flow_path.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN operation_flow_path.deleted IS '软删除标记。';

CREATE UNIQUE INDEX uk_operation_flow_path_code
    ON operation_flow_path (tenant_id, lesson_plan_id, path_code)
    WHERE deleted = false;

CREATE TABLE operation_question (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    lesson_plan_id varchar(64) NOT NULL,
    flow_path_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    module_code varchar(128) NOT NULL,
    question_code varchar(128) NOT NULL,
    question_name varchar(256) NOT NULL,
    question_type varchar(32) NOT NULL DEFAULT 'OPERATION',
    difficulty varchar(32) NOT NULL,
    data_template_id varchar(64) NOT NULL,
    evaluation_rule_id varchar(64) NOT NULL,
    reversible_policy varchar(32) NOT NULL,
    question_config_json jsonb,
    publish_status varchar(32) NOT NULL DEFAULT 'DRAFT',
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE operation_question IS '实操题库表：从教案和流程分支沉淀可练习、可考试的原平台实操题。';
COMMENT ON COLUMN operation_question.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN operation_question.tenant_id IS '租户 ID。';
COMMENT ON COLUMN operation_question.lesson_plan_id IS '教案 ID。';
COMMENT ON COLUMN operation_question.flow_path_id IS '流程分支 ID。';
COMMENT ON COLUMN operation_question.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN operation_question.module_code IS '原平台功能模块编码。';
COMMENT ON COLUMN operation_question.question_code IS '题目编码。';
COMMENT ON COLUMN operation_question.question_name IS '题目名称。';
COMMENT ON COLUMN operation_question.question_type IS '题目类型，第二阶段固定为 OPERATION。';
COMMENT ON COLUMN operation_question.difficulty IS '难度：EASY、NORMAL、HARD。';
COMMENT ON COLUMN operation_question.data_template_id IS '数据模板 ID。';
COMMENT ON COLUMN operation_question.evaluation_rule_id IS '评分规则 ID。';
COMMENT ON COLUMN operation_question.reversible_policy IS '可逆策略：REVERSIBLE、PARTIAL、IRREVERSIBLE。';
COMMENT ON COLUMN operation_question.question_config_json IS '题目配置。';
COMMENT ON COLUMN operation_question.publish_status IS '发布状态：DRAFT、PUBLISHED、DISABLED。';
COMMENT ON COLUMN operation_question.create_by IS '创建人 ID。';
COMMENT ON COLUMN operation_question.create_time IS '创建时间。';
COMMENT ON COLUMN operation_question.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN operation_question.update_time IS '最后更新时间。';
COMMENT ON COLUMN operation_question.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN operation_question.deleted IS '软删除标记。';

CREATE UNIQUE INDEX uk_operation_question_code
    ON operation_question (tenant_id, connector_system_id, question_code)
    WHERE deleted = false;

CREATE TABLE exam_paper (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    course_id varchar(64) NOT NULL,
    task_id varchar(64),
    paper_code varchar(128) NOT NULL,
    paper_name varchar(256) NOT NULL,
    total_score numeric(8,2) NOT NULL,
    time_limit_minutes bigint,
    paper_strategy_json jsonb,
    publish_status varchar(32) NOT NULL DEFAULT 'DRAFT',
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE exam_paper IS '试卷表：定义一次考试使用的题目集合。';
COMMENT ON COLUMN exam_paper.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN exam_paper.tenant_id IS '租户 ID。';
COMMENT ON COLUMN exam_paper.course_id IS '课程 ID。';
COMMENT ON COLUMN exam_paper.task_id IS '绑定的考试任务 ID。';
COMMENT ON COLUMN exam_paper.paper_code IS '试卷编码。';
COMMENT ON COLUMN exam_paper.paper_name IS '试卷名称。';
COMMENT ON COLUMN exam_paper.total_score IS '试卷总分。';
COMMENT ON COLUMN exam_paper.time_limit_minutes IS '考试限制时长，单位分钟。';
COMMENT ON COLUMN exam_paper.paper_strategy_json IS '试卷策略，如抽题、乱序、重考策略。';
COMMENT ON COLUMN exam_paper.publish_status IS '发布状态：DRAFT、PUBLISHED、LOCKED。';
COMMENT ON COLUMN exam_paper.create_by IS '创建人 ID。';
COMMENT ON COLUMN exam_paper.create_time IS '创建时间。';
COMMENT ON COLUMN exam_paper.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN exam_paper.update_time IS '最后更新时间。';
COMMENT ON COLUMN exam_paper.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN exam_paper.deleted IS '软删除标记。';

CREATE UNIQUE INDEX uk_exam_paper_code
    ON exam_paper (tenant_id, paper_code)
    WHERE deleted = false;

CREATE TABLE exam_paper_question (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    paper_id varchar(64) NOT NULL,
    question_id varchar(64) NOT NULL,
    sequence_no bigint NOT NULL,
    score numeric(8,2) NOT NULL,
    required boolean NOT NULL DEFAULT true,
    question_snapshot_json jsonb NOT NULL,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE exam_paper_question IS '试卷题目表：维护试卷与实操题之间的关系，并固化题目发布快照。';
COMMENT ON COLUMN exam_paper_question.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN exam_paper_question.tenant_id IS '租户 ID。';
COMMENT ON COLUMN exam_paper_question.paper_id IS '试卷 ID。';
COMMENT ON COLUMN exam_paper_question.question_id IS '实操题 ID。';
COMMENT ON COLUMN exam_paper_question.sequence_no IS '题目顺序号。';
COMMENT ON COLUMN exam_paper_question.score IS '题目分值。';
COMMENT ON COLUMN exam_paper_question.required IS '是否必答。';
COMMENT ON COLUMN exam_paper_question.question_snapshot_json IS '题目发布时的快照，避免题库后续变更影响历史考试。';
COMMENT ON COLUMN exam_paper_question.create_by IS '创建人 ID。';
COMMENT ON COLUMN exam_paper_question.create_time IS '创建时间。';
COMMENT ON COLUMN exam_paper_question.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN exam_paper_question.update_time IS '最后更新时间。';
COMMENT ON COLUMN exam_paper_question.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN exam_paper_question.deleted IS '软删除标记。';

CREATE UNIQUE INDEX uk_exam_paper_question_sequence
    ON exam_paper_question (tenant_id, paper_id, sequence_no)
    WHERE deleted = false;

CREATE TABLE teaching_data_pool (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    module_code varchar(128) NOT NULL,
    task_id varchar(64) NOT NULL,
    class_id varchar(64),
    scene_type varchar(32) NOT NULL,
    question_id varchar(64),
    template_id varchar(64) NOT NULL,
    pool_status varchar(32) NOT NULL DEFAULT 'CREATED',
    total_count bigint NOT NULL DEFAULT 0,
    ready_count bigint NOT NULL DEFAULT 0,
    allocated_count bigint NOT NULL DEFAULT 0,
    failed_count bigint NOT NULL DEFAULT 0,
    pool_policy_json jsonb,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE teaching_data_pool IS '教学数据池表：管理练习和考试场景下批量准备的数据实例。';
COMMENT ON COLUMN teaching_data_pool.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN teaching_data_pool.tenant_id IS '租户 ID。';
COMMENT ON COLUMN teaching_data_pool.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN teaching_data_pool.module_code IS '原平台功能模块编码。';
COMMENT ON COLUMN teaching_data_pool.task_id IS '任务 ID。';
COMMENT ON COLUMN teaching_data_pool.class_id IS '班级 ID。';
COMMENT ON COLUMN teaching_data_pool.scene_type IS '场景类型：PRACTICE、EXAM。';
COMMENT ON COLUMN teaching_data_pool.question_id IS '实操题 ID，考试数据池按题目隔离时使用。';
COMMENT ON COLUMN teaching_data_pool.template_id IS '数据模板 ID。';
COMMENT ON COLUMN teaching_data_pool.pool_status IS '数据池状态：CREATED、PREPARING、READY、LOCKED、EXHAUSTED、FAILED、ARCHIVED。';
COMMENT ON COLUMN teaching_data_pool.total_count IS '计划数据总数。';
COMMENT ON COLUMN teaching_data_pool.ready_count IS '可领取数据数量。';
COMMENT ON COLUMN teaching_data_pool.allocated_count IS '已领取数据数量。';
COMMENT ON COLUMN teaching_data_pool.failed_count IS '准备失败数据数量。';
COMMENT ON COLUMN teaching_data_pool.pool_policy_json IS '数据池策略。';
COMMENT ON COLUMN teaching_data_pool.create_by IS '创建人 ID。';
COMMENT ON COLUMN teaching_data_pool.create_time IS '创建时间。';
COMMENT ON COLUMN teaching_data_pool.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN teaching_data_pool.update_time IS '最后更新时间。';
COMMENT ON COLUMN teaching_data_pool.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN teaching_data_pool.deleted IS '软删除标记。';

CREATE INDEX idx_teaching_data_pool_task_scene
    ON teaching_data_pool (tenant_id, task_id, scene_type, pool_status)
    WHERE deleted = false;

CREATE TABLE data_prepare_job (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    pool_id varchar(64),
    task_id varchar(64),
    module_code varchar(128) NOT NULL,
    scene_type varchar(32) NOT NULL,
    job_type varchar(32) NOT NULL,
    expected_count bigint NOT NULL DEFAULT 0,
    success_count bigint NOT NULL DEFAULT 0,
    failed_count bigint NOT NULL DEFAULT 0,
    job_status varchar(32) NOT NULL DEFAULT 'CREATED',
    request_json jsonb,
    result_json jsonb,
    error_message text,
    start_time timestamp,
    end_time timestamp,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE data_prepare_job IS '数据准备任务表：记录批量造数、拉取、锁定、校验和归档任务。';
COMMENT ON COLUMN data_prepare_job.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN data_prepare_job.tenant_id IS '租户 ID。';
COMMENT ON COLUMN data_prepare_job.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN data_prepare_job.pool_id IS '数据池 ID。';
COMMENT ON COLUMN data_prepare_job.task_id IS '任务 ID。';
COMMENT ON COLUMN data_prepare_job.module_code IS '原平台功能模块编码。';
COMMENT ON COLUMN data_prepare_job.scene_type IS '场景类型：RECORD、LEARN、PRACTICE、EXAM。';
COMMENT ON COLUMN data_prepare_job.job_type IS '任务类型：CREATE、PULL、LOCK、CHECK、ARCHIVE。';
COMMENT ON COLUMN data_prepare_job.expected_count IS '期望处理数量。';
COMMENT ON COLUMN data_prepare_job.success_count IS '成功数量。';
COMMENT ON COLUMN data_prepare_job.failed_count IS '失败数量。';
COMMENT ON COLUMN data_prepare_job.job_status IS '任务状态：CREATED、RUNNING、SUCCESS、PARTIAL_FAILED、FAILED、CANCELLED。';
COMMENT ON COLUMN data_prepare_job.request_json IS '请求摘要。';
COMMENT ON COLUMN data_prepare_job.result_json IS '结果摘要。';
COMMENT ON COLUMN data_prepare_job.error_message IS '错误信息。';
COMMENT ON COLUMN data_prepare_job.start_time IS '开始时间。';
COMMENT ON COLUMN data_prepare_job.end_time IS '结束时间。';
COMMENT ON COLUMN data_prepare_job.create_by IS '创建人 ID。';
COMMENT ON COLUMN data_prepare_job.create_time IS '创建时间。';
COMMENT ON COLUMN data_prepare_job.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN data_prepare_job.update_time IS '最后更新时间。';
COMMENT ON COLUMN data_prepare_job.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN data_prepare_job.deleted IS '软删除标记。';

CREATE INDEX idx_data_prepare_job_pool
    ON data_prepare_job (tenant_id, pool_id, job_status, create_time DESC)
    WHERE deleted = false;

CREATE TABLE data_instance_allocation (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    pool_id varchar(64) NOT NULL,
    data_instance_id varchar(64) NOT NULL,
    task_id varchar(64) NOT NULL,
    execution_id varchar(64),
    attempt_id varchar(64),
    question_attempt_id varchar(64),
    owner_user_id varchar(64) NOT NULL,
    allocation_scene varchar(32) NOT NULL,
    allocation_status varchar(32) NOT NULL DEFAULT 'ALLOCATED',
    allocate_time timestamp NOT NULL DEFAULT now(),
    release_time timestamp,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE data_instance_allocation IS '数据实例分配表：记录数据实例被谁、何时、为何领取，防止并发重复领取。';
COMMENT ON COLUMN data_instance_allocation.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN data_instance_allocation.tenant_id IS '租户 ID。';
COMMENT ON COLUMN data_instance_allocation.pool_id IS '数据池 ID。';
COMMENT ON COLUMN data_instance_allocation.data_instance_id IS '教学数据实例 ID。';
COMMENT ON COLUMN data_instance_allocation.task_id IS '任务 ID。';
COMMENT ON COLUMN data_instance_allocation.execution_id IS '任务执行 ID。';
COMMENT ON COLUMN data_instance_allocation.attempt_id IS '练习或考试 attempt ID。';
COMMENT ON COLUMN data_instance_allocation.question_attempt_id IS '考试题目作答实例 ID。';
COMMENT ON COLUMN data_instance_allocation.owner_user_id IS '领取数据的学生 ID。';
COMMENT ON COLUMN data_instance_allocation.allocation_scene IS '分配场景：PRACTICE、EXAM。';
COMMENT ON COLUMN data_instance_allocation.allocation_status IS '分配状态：ALLOCATED、RELEASED、CONSUMED、DISCARDED。';
COMMENT ON COLUMN data_instance_allocation.allocate_time IS '领取时间。';
COMMENT ON COLUMN data_instance_allocation.release_time IS '释放时间。';
COMMENT ON COLUMN data_instance_allocation.create_by IS '创建人 ID。';
COMMENT ON COLUMN data_instance_allocation.create_time IS '创建时间。';
COMMENT ON COLUMN data_instance_allocation.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN data_instance_allocation.update_time IS '最后更新时间。';
COMMENT ON COLUMN data_instance_allocation.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN data_instance_allocation.deleted IS '软删除标记。';

CREATE UNIQUE INDEX uk_data_instance_active_allocation
    ON data_instance_allocation (tenant_id, data_instance_id)
    WHERE deleted = false AND allocation_status = 'ALLOCATED';

CREATE INDEX idx_data_instance_allocation_owner
    ON data_instance_allocation (tenant_id, owner_user_id, task_id, allocation_scene)
    WHERE deleted = false;

CREATE TABLE exam_attempt (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    execution_id varchar(64) NOT NULL,
    student_id varchar(64) NOT NULL,
    class_id varchar(64),
    task_id varchar(64) NOT NULL,
    paper_id varchar(64) NOT NULL,
    attempt_no bigint NOT NULL,
    retake_type varchar(32),
    start_time timestamp,
    submit_time timestamp,
    end_time timestamp,
    attempt_status varchar(32) NOT NULL DEFAULT 'RUNNING',
    score numeric(8,2),
    max_score numeric(8,2),
    metadata_json jsonb,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE exam_attempt IS '考试次数表：记录学生一次考试主记录。';
COMMENT ON COLUMN exam_attempt.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN exam_attempt.tenant_id IS '租户 ID。';
COMMENT ON COLUMN exam_attempt.execution_id IS '任务执行 ID。';
COMMENT ON COLUMN exam_attempt.student_id IS '学生 ID。';
COMMENT ON COLUMN exam_attempt.class_id IS '班级 ID。';
COMMENT ON COLUMN exam_attempt.task_id IS '考试任务 ID。';
COMMENT ON COLUMN exam_attempt.paper_id IS '试卷 ID。';
COMMENT ON COLUMN exam_attempt.attempt_no IS '第几次考试。';
COMMENT ON COLUMN exam_attempt.retake_type IS '重考类型：NORMAL、SYSTEM_EXCEPTION、TEACHER_APPROVED。';
COMMENT ON COLUMN exam_attempt.start_time IS '考试开始时间。';
COMMENT ON COLUMN exam_attempt.submit_time IS '交卷时间。';
COMMENT ON COLUMN exam_attempt.end_time IS '考试结束时间。';
COMMENT ON COLUMN exam_attempt.attempt_status IS '考试状态：RUNNING、SUBMITTED、COMPLETED、ABNORMAL、CANCELLED。';
COMMENT ON COLUMN exam_attempt.score IS '考试得分。';
COMMENT ON COLUMN exam_attempt.max_score IS '考试满分。';
COMMENT ON COLUMN exam_attempt.metadata_json IS '考试扩展摘要。';
COMMENT ON COLUMN exam_attempt.create_by IS '创建人 ID。';
COMMENT ON COLUMN exam_attempt.create_time IS '创建时间。';
COMMENT ON COLUMN exam_attempt.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN exam_attempt.update_time IS '最后更新时间。';
COMMENT ON COLUMN exam_attempt.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN exam_attempt.deleted IS '软删除标记。';

CREATE INDEX idx_exam_attempt_student_task
    ON exam_attempt (tenant_id, student_id, task_id, attempt_no DESC)
    WHERE deleted = false;

CREATE TABLE exam_question_attempt (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    exam_attempt_id varchar(64) NOT NULL,
    execution_id varchar(64) NOT NULL,
    student_id varchar(64) NOT NULL,
    task_id varchar(64) NOT NULL,
    paper_id varchar(64) NOT NULL,
    paper_question_id varchar(64) NOT NULL,
    question_id varchar(64) NOT NULL,
    data_instance_id varchar(64) NOT NULL,
    sequence_no bigint NOT NULL,
    question_status varchar(32) NOT NULL DEFAULT 'NOT_STARTED',
    reversible_policy varchar(32) NOT NULL,
    score numeric(8,2),
    max_score numeric(8,2) NOT NULL,
    start_time timestamp,
    submit_time timestamp,
    evaluation_result_id varchar(64),
    answer_snapshot_json jsonb,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE exam_question_attempt IS '考试题目作答实例表：记录学生某次考试中每道实操题的作答、数据实例和评分结果。';
COMMENT ON COLUMN exam_question_attempt.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN exam_question_attempt.tenant_id IS '租户 ID。';
COMMENT ON COLUMN exam_question_attempt.exam_attempt_id IS '考试 attempt ID。';
COMMENT ON COLUMN exam_question_attempt.execution_id IS '任务执行 ID。';
COMMENT ON COLUMN exam_question_attempt.student_id IS '学生 ID。';
COMMENT ON COLUMN exam_question_attempt.task_id IS '任务 ID。';
COMMENT ON COLUMN exam_question_attempt.paper_id IS '试卷 ID。';
COMMENT ON COLUMN exam_question_attempt.paper_question_id IS '试卷题目 ID。';
COMMENT ON COLUMN exam_question_attempt.question_id IS '实操题 ID。';
COMMENT ON COLUMN exam_question_attempt.data_instance_id IS '本题绑定的教学数据实例 ID。';
COMMENT ON COLUMN exam_question_attempt.sequence_no IS '题目顺序号。';
COMMENT ON COLUMN exam_question_attempt.question_status IS '题目状态：NOT_STARTED、RUNNING、ANSWERED、LOCKED、SUBMITTED、EVALUATED。';
COMMENT ON COLUMN exam_question_attempt.reversible_policy IS '可逆策略：REVERSIBLE、PARTIAL、IRREVERSIBLE。';
COMMENT ON COLUMN exam_question_attempt.score IS '本题得分。';
COMMENT ON COLUMN exam_question_attempt.max_score IS '本题满分。';
COMMENT ON COLUMN exam_question_attempt.start_time IS '题目开始时间。';
COMMENT ON COLUMN exam_question_attempt.submit_time IS '题目提交时间。';
COMMENT ON COLUMN exam_question_attempt.evaluation_result_id IS '评分结果 ID。';
COMMENT ON COLUMN exam_question_attempt.answer_snapshot_json IS '作答摘要快照，不保存原平台完整业务明细。';
COMMENT ON COLUMN exam_question_attempt.create_by IS '创建人 ID。';
COMMENT ON COLUMN exam_question_attempt.create_time IS '创建时间。';
COMMENT ON COLUMN exam_question_attempt.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN exam_question_attempt.update_time IS '最后更新时间。';
COMMENT ON COLUMN exam_question_attempt.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN exam_question_attempt.deleted IS '软删除标记。';

CREATE UNIQUE INDEX uk_exam_question_attempt_paper_question
    ON exam_question_attempt (tenant_id, exam_attempt_id, paper_question_id)
    WHERE deleted = false;

CREATE UNIQUE INDEX uk_exam_question_attempt_data_instance
    ON exam_question_attempt (tenant_id, data_instance_id)
    WHERE deleted = false;

ALTER TABLE teaching_data_template ADD COLUMN IF NOT EXISTS module_code varchar(128);
ALTER TABLE teaching_data_template ADD COLUMN IF NOT EXISTS strategy_id varchar(64);
ALTER TABLE teaching_data_template ADD COLUMN IF NOT EXISTS data_schema_json jsonb;
ALTER TABLE teaching_data_template ADD COLUMN IF NOT EXISTS mock_rule_json jsonb;
ALTER TABLE teaching_data_template ADD COLUMN IF NOT EXISTS readonly_flag boolean NOT NULL DEFAULT false;

COMMENT ON COLUMN teaching_data_template.module_code IS '原平台功能模块编码。';
COMMENT ON COLUMN teaching_data_template.strategy_id IS '功能模块数据策略 ID。';
COMMENT ON COLUMN teaching_data_template.data_schema_json IS '数据模板结构。';
COMMENT ON COLUMN teaching_data_template.mock_rule_json IS 'mock 数据生成规则。';
COMMENT ON COLUMN teaching_data_template.readonly_flag IS '是否只读模板。';

ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS pool_id varchar(64);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS module_code varchar(128);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS question_id varchar(64);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS question_attempt_id varchar(64);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS lock_reason varchar(256);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS readonly_flag boolean NOT NULL DEFAULT false;
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS allocation_status varchar(32);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS archive_status varchar(32) NOT NULL DEFAULT 'NONE';
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS archive_time timestamp;

COMMENT ON COLUMN teaching_data_instance.pool_id IS '所属教学数据池 ID。';
COMMENT ON COLUMN teaching_data_instance.module_code IS '原平台功能模块编码。';
COMMENT ON COLUMN teaching_data_instance.question_id IS '实操题 ID。';
COMMENT ON COLUMN teaching_data_instance.question_attempt_id IS '考试题目作答实例 ID。';
COMMENT ON COLUMN teaching_data_instance.lock_reason IS '数据锁定原因。';
COMMENT ON COLUMN teaching_data_instance.readonly_flag IS '是否只读数据。';
COMMENT ON COLUMN teaching_data_instance.allocation_status IS '分配状态：READY、ALLOCATED、RELEASED、CONSUMED。';
COMMENT ON COLUMN teaching_data_instance.archive_status IS '归档状态：NONE、READY、ARCHIVED。';
COMMENT ON COLUMN teaching_data_instance.archive_time IS '归档时间。';

ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS attempt_id varchar(64);
ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS question_attempt_id varchar(64);
ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS readonly_flag boolean NOT NULL DEFAULT false;
ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS lock_flag boolean NOT NULL DEFAULT false;
ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS launch_policy_json jsonb;

COMMENT ON COLUMN platform_launch_context.attempt_id IS '练习或考试 attempt ID。';
COMMENT ON COLUMN platform_launch_context.question_attempt_id IS '考试题目作答实例 ID。';
COMMENT ON COLUMN platform_launch_context.readonly_flag IS '本次进入原平台是否只读。';
COMMENT ON COLUMN platform_launch_context.lock_flag IS '本次进入原平台是否使用锁定数据。';
COMMENT ON COLUMN platform_launch_context.launch_policy_json IS '启动策略摘要。';

ALTER TABLE evaluation_result ADD COLUMN IF NOT EXISTS exam_attempt_id varchar(64);
ALTER TABLE evaluation_result ADD COLUMN IF NOT EXISTS question_attempt_id varchar(64);
ALTER TABLE evaluation_result ADD COLUMN IF NOT EXISTS data_instance_id varchar(64);
ALTER TABLE evaluation_result ADD COLUMN IF NOT EXISTS result_check_json jsonb;

COMMENT ON COLUMN evaluation_result.exam_attempt_id IS '考试 attempt ID。';
COMMENT ON COLUMN evaluation_result.question_attempt_id IS '考试题目作答实例 ID。';
COMMENT ON COLUMN evaluation_result.data_instance_id IS '评分对应的数据实例 ID。';
COMMENT ON COLUMN evaluation_result.result_check_json IS '原平台结果校验证据。';

ALTER TABLE platform_capability
    ADD CONSTRAINT fk_platform_capability_connector
    FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);

ALTER TABLE module_data_strategy
    ADD CONSTRAINT fk_module_data_strategy_connector
    FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);

ALTER TABLE lesson_plan
    ADD CONSTRAINT fk_lesson_plan_connector
    FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);

ALTER TABLE lesson_plan
    ADD CONSTRAINT fk_lesson_plan_teaching_point
    FOREIGN KEY (teaching_point_id) REFERENCES teaching_point(id);

ALTER TABLE operation_flow_path
    ADD CONSTRAINT fk_operation_flow_path_lesson_plan
    FOREIGN KEY (lesson_plan_id) REFERENCES lesson_plan(id);

ALTER TABLE operation_question
    ADD CONSTRAINT fk_operation_question_lesson_plan
    FOREIGN KEY (lesson_plan_id) REFERENCES lesson_plan(id);

ALTER TABLE operation_question
    ADD CONSTRAINT fk_operation_question_flow_path
    FOREIGN KEY (flow_path_id) REFERENCES operation_flow_path(id);

ALTER TABLE exam_paper
    ADD CONSTRAINT fk_exam_paper_course
    FOREIGN KEY (course_id) REFERENCES course(id);

ALTER TABLE exam_paper_question
    ADD CONSTRAINT fk_exam_paper_question_paper
    FOREIGN KEY (paper_id) REFERENCES exam_paper(id);

ALTER TABLE exam_paper_question
    ADD CONSTRAINT fk_exam_paper_question_question
    FOREIGN KEY (question_id) REFERENCES operation_question(id);

ALTER TABLE teaching_data_pool
    ADD CONSTRAINT fk_teaching_data_pool_connector
    FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);

ALTER TABLE data_prepare_job
    ADD CONSTRAINT fk_data_prepare_job_connector
    FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);

ALTER TABLE data_instance_allocation
    ADD CONSTRAINT fk_data_instance_allocation_pool
    FOREIGN KEY (pool_id) REFERENCES teaching_data_pool(id);

ALTER TABLE data_instance_allocation
    ADD CONSTRAINT fk_data_instance_allocation_instance
    FOREIGN KEY (data_instance_id) REFERENCES teaching_data_instance(id);

ALTER TABLE exam_attempt
    ADD CONSTRAINT fk_exam_attempt_execution
    FOREIGN KEY (execution_id) REFERENCES task_execution(id);

ALTER TABLE exam_attempt
    ADD CONSTRAINT fk_exam_attempt_paper
    FOREIGN KEY (paper_id) REFERENCES exam_paper(id);

ALTER TABLE exam_question_attempt
    ADD CONSTRAINT fk_exam_question_attempt_exam
    FOREIGN KEY (exam_attempt_id) REFERENCES exam_attempt(id);

ALTER TABLE exam_question_attempt
    ADD CONSTRAINT fk_exam_question_attempt_data_instance
    FOREIGN KEY (data_instance_id) REFERENCES teaching_data_instance(id);

