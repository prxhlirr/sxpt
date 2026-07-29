CREATE TABLE IF NOT EXISTS business_module (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    module_code varchar(128) NOT NULL,
    module_name varchar(256) NOT NULL,
    external_module_id varchar(128),
    entry_url varchar(1024),
    module_type varchar(64),
    support_scenes varchar(256),
    need_pre_data boolean NOT NULL DEFAULT true,
    default_initial_status varchar(64),
    default_target_status varchar(64),
    capability_codes_json jsonb,
    default_template_id varchar(64),
    remark varchar(512),
    lock_version bigint NOT NULL DEFAULT 0,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE business_module IS '业务模块表：维护原平台中可被教学化的业务入口主数据。';
COMMENT ON COLUMN business_module.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN business_module.tenant_id IS '租户 ID。';
COMMENT ON COLUMN business_module.connector_system_id IS '所属原平台 ID。';
COMMENT ON COLUMN business_module.module_code IS '原平台业务模块编码，必须稳定。';
COMMENT ON COLUMN business_module.module_name IS '业务模块名称。';
COMMENT ON COLUMN business_module.external_module_id IS '原平台模块 ID。';
COMMENT ON COLUMN business_module.entry_url IS '原平台默认入口地址。';
COMMENT ON COLUMN business_module.module_type IS '业务类型，如申请、审批、查询、归档。';
COMMENT ON COLUMN business_module.support_scenes IS '支持场景：RECORD、LEARN、PRACTICE、EXAM。';
COMMENT ON COLUMN business_module.need_pre_data IS '该业务模块是否需要前置业务数据。';
COMMENT ON COLUMN business_module.default_initial_status IS '默认初始原平台状态。';
COMMENT ON COLUMN business_module.default_target_status IS '默认目标原平台状态。';
COMMENT ON COLUMN business_module.capability_codes_json IS '依赖的原平台能力编码列表。';
COMMENT ON COLUMN business_module.default_template_id IS '默认数据模板 ID。';
COMMENT ON COLUMN business_module.remark IS '业务说明。';
COMMENT ON COLUMN business_module.lock_version IS '乐观锁版本号，用于防止并发覆盖配置。';
COMMENT ON COLUMN business_module.create_by IS '创建人 ID。';
COMMENT ON COLUMN business_module.create_time IS '创建时间。';
COMMENT ON COLUMN business_module.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN business_module.update_time IS '最后更新时间。';
COMMENT ON COLUMN business_module.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN business_module.deleted IS '软删除标记。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_business_module_code
    ON business_module (tenant_id, connector_system_id, module_code)
    WHERE deleted = false;

ALTER TABLE module_data_strategy ADD COLUMN IF NOT EXISTS business_module_id varchar(64);
ALTER TABLE module_data_strategy ADD COLUMN IF NOT EXISTS strategy_code varchar(128);
ALTER TABLE module_data_strategy ADD COLUMN IF NOT EXISTS template_id varchar(64);
ALTER TABLE module_data_strategy ADD COLUMN IF NOT EXISTS prepare_timing varchar(32);
ALTER TABLE module_data_strategy ADD COLUMN IF NOT EXISTS pool_size_policy_json jsonb;
ALTER TABLE module_data_strategy ADD COLUMN IF NOT EXISTS validation_policy_json jsonb;
ALTER TABLE module_data_strategy ADD COLUMN IF NOT EXISTS archive_policy_json jsonb;
ALTER TABLE module_data_strategy ADD COLUMN IF NOT EXISTS strategy_version bigint NOT NULL DEFAULT 1;
ALTER TABLE module_data_strategy ADD COLUMN IF NOT EXISTS lock_version bigint NOT NULL DEFAULT 0;

COMMENT ON COLUMN module_data_strategy.business_module_id IS '业务模块 ID，用于解除模块主数据和场景策略的耦合。';
COMMENT ON COLUMN module_data_strategy.strategy_code IS '数据策略编码。';
COMMENT ON COLUMN module_data_strategy.template_id IS '默认数据模板 ID。';
COMMENT ON COLUMN module_data_strategy.prepare_timing IS '数据准备时机：ON_PUBLISH、BEFORE_START、ON_DEMAND。';
COMMENT ON COLUMN module_data_strategy.pool_size_policy_json IS '数据池容量策略。';
COMMENT ON COLUMN module_data_strategy.validation_policy_json IS '数据准备完成后的可见性、可操作性和状态校验策略。';
COMMENT ON COLUMN module_data_strategy.archive_policy_json IS '归档策略。';
COMMENT ON COLUMN module_data_strategy.strategy_version IS '策略版本号，用于发布快照和历史追溯。';
COMMENT ON COLUMN module_data_strategy.lock_version IS '乐观锁版本号。';

ALTER TABLE teaching_data_template ADD COLUMN IF NOT EXISTS request_schema_json jsonb;
ALTER TABLE teaching_data_template ADD COLUMN IF NOT EXISTS required_org_role_json jsonb;
ALTER TABLE teaching_data_template ADD COLUMN IF NOT EXISTS result_check_schema_json jsonb;
ALTER TABLE teaching_data_template ADD COLUMN IF NOT EXISTS sensitive_field_policy_json jsonb;

COMMENT ON COLUMN teaching_data_template.request_schema_json IS '调用原平台数据准备接口的请求结构。';
COMMENT ON COLUMN teaching_data_template.required_org_role_json IS '默认单位和角色要求。';
COMMENT ON COLUMN teaching_data_template.result_check_schema_json IS '原平台结果校验字段契约。';
COMMENT ON COLUMN teaching_data_template.sensitive_field_policy_json IS '脱敏、禁采和禁传策略。';

CREATE TABLE IF NOT EXISTS data_requirement (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    requirement_code varchar(128) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    business_module_id varchar(64),
    module_code varchar(128) NOT NULL,
    strategy_id varchar(64),
    template_id varchar(64),
    task_id varchar(64),
    class_id varchar(64),
    scene_type varchar(32) NOT NULL,
    requirement_status varchar(32) NOT NULL DEFAULT 'CREATED',
    expected_count bigint NOT NULL DEFAULT 0,
    success_count bigint NOT NULL DEFAULT 0,
    failed_count bigint NOT NULL DEFAULT 0,
    requirement_policy_json jsonb,
    remark varchar(512),
    lock_version bigint NOT NULL DEFAULT 0,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE data_requirement IS '数据需求批次表：保存一次批量数据准备的业务约束入口。';
COMMENT ON COLUMN data_requirement.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN data_requirement.tenant_id IS '租户 ID。';
COMMENT ON COLUMN data_requirement.requirement_code IS '数据需求批次编码。';
COMMENT ON COLUMN data_requirement.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN data_requirement.business_module_id IS '业务模块 ID。';
COMMENT ON COLUMN data_requirement.module_code IS '原平台功能模块编码。';
COMMENT ON COLUMN data_requirement.strategy_id IS '采用的数据策略 ID。';
COMMENT ON COLUMN data_requirement.template_id IS '采用的数据模板 ID。';
COMMENT ON COLUMN data_requirement.task_id IS '教学任务 ID。';
COMMENT ON COLUMN data_requirement.class_id IS '班级 ID。';
COMMENT ON COLUMN data_requirement.scene_type IS '教学场景：RECORD、LEARN、PRACTICE、EXAM。';
COMMENT ON COLUMN data_requirement.requirement_status IS '需求状态：CREATED、PREPARING、READY、PARTIAL_FAILED、FAILED、CANCELLED。';
COMMENT ON COLUMN data_requirement.expected_count IS '期望准备数量。';
COMMENT ON COLUMN data_requirement.success_count IS '成功准备数量。';
COMMENT ON COLUMN data_requirement.failed_count IS '失败准备数量。';
COMMENT ON COLUMN data_requirement.requirement_policy_json IS '本批次数据需求策略快照。';
COMMENT ON COLUMN data_requirement.remark IS '业务说明。';
COMMENT ON COLUMN data_requirement.lock_version IS '乐观锁版本号。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_data_requirement_code
    ON data_requirement (tenant_id, requirement_code)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_data_requirement_task
    ON data_requirement (tenant_id, task_id, scene_type, requirement_status)
    WHERE deleted = false;

CREATE TABLE IF NOT EXISTS data_requirement_item (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    requirement_id varchar(64) NOT NULL,
    request_batch_id varchar(128) NOT NULL,
    request_item_id varchar(128) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    business_module_id varchar(64),
    module_code varchar(128) NOT NULL,
    template_id varchar(64),
    scene_type varchar(32) NOT NULL,
    task_id varchar(64),
    execution_id varchar(64),
    student_id varchar(64),
    question_id varchar(64),
    exam_attempt_id varchar(64),
    question_attempt_id varchar(64),
    collaboration_unit_id varchar(64),
    segment_no bigint,
    actor_type varchar(64),
    owner_external_org_id varchar(128),
    owner_external_org_name varchar(256),
    required_external_org_id varchar(128),
    required_external_org_name varchar(256),
    required_external_role_id varchar(128),
    required_external_role_name varchar(256),
    init_external_status varchar(64),
    target_external_status varchar(64),
    target_url varchar(1024),
    data_scope_json jsonb,
    required_actions_json jsonb,
    validation_policy_json jsonb,
    score_point_snapshot_json jsonb,
    item_status varchar(32) NOT NULL DEFAULT 'CREATED',
    external_business_id varchar(128),
    external_business_no varchar(128),
    external_status varchar(64),
    validation_status varchar(32),
    validation_time timestamp,
    validation_result_json jsonb,
    failure_reason text,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE data_requirement_item IS '数据需求明细表：保存每条原平台教学用业务数据的生成约束和返回结果。';
COMMENT ON COLUMN data_requirement_item.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN data_requirement_item.tenant_id IS '租户 ID。';
COMMENT ON COLUMN data_requirement_item.requirement_id IS '数据需求批次 ID。';
COMMENT ON COLUMN data_requirement_item.request_batch_id IS '本次请求批次 ID，用于和原平台批量接口对账。';
COMMENT ON COLUMN data_requirement_item.request_item_id IS '请求项 ID，用于批量创建时逐条幂等和逐条回填结果。';
COMMENT ON COLUMN data_requirement_item.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN data_requirement_item.business_module_id IS '业务模块 ID。';
COMMENT ON COLUMN data_requirement_item.module_code IS '原平台功能模块编码。';
COMMENT ON COLUMN data_requirement_item.template_id IS '数据模板 ID。';
COMMENT ON COLUMN data_requirement_item.scene_type IS '教学场景：RECORD、LEARN、PRACTICE、EXAM。';
COMMENT ON COLUMN data_requirement_item.task_id IS '教学任务 ID。';
COMMENT ON COLUMN data_requirement_item.execution_id IS '任务执行 ID。';
COMMENT ON COLUMN data_requirement_item.student_id IS '学生 ID。';
COMMENT ON COLUMN data_requirement_item.question_id IS '实操题 ID。';
COMMENT ON COLUMN data_requirement_item.exam_attempt_id IS '考试 attempt ID。';
COMMENT ON COLUMN data_requirement_item.question_attempt_id IS '考试题目作答实例 ID。';
COMMENT ON COLUMN data_requirement_item.collaboration_unit_id IS '多角色协作单元 ID。';
COMMENT ON COLUMN data_requirement_item.segment_no IS '业务片段编号。';
COMMENT ON COLUMN data_requirement_item.actor_type IS '业务参与身份，如申请人、受理人、审核人。';
COMMENT ON COLUMN data_requirement_item.owner_external_org_id IS '原平台业务数据归属单位 ID。';
COMMENT ON COLUMN data_requirement_item.owner_external_org_name IS '原平台业务数据归属单位名称。';
COMMENT ON COLUMN data_requirement_item.required_external_org_id IS '本条数据必须满足的原平台操作单位 ID。';
COMMENT ON COLUMN data_requirement_item.required_external_org_name IS '本条数据必须满足的原平台操作单位名称。';
COMMENT ON COLUMN data_requirement_item.required_external_role_id IS '本条数据必须满足的原平台操作角色 ID。';
COMMENT ON COLUMN data_requirement_item.required_external_role_name IS '本条数据必须满足的原平台操作角色名称。';
COMMENT ON COLUMN data_requirement_item.init_external_status IS '生成后要求的原平台初始状态。';
COMMENT ON COLUMN data_requirement_item.target_external_status IS '完成后要求的原平台目标状态。';
COMMENT ON COLUMN data_requirement_item.target_url IS '原平台入口页面。';
COMMENT ON COLUMN data_requirement_item.data_scope_json IS '数据范围和读写策略。';
COMMENT ON COLUMN data_requirement_item.required_actions_json IS '本条数据必须支持的业务动作或节点。';
COMMENT ON COLUMN data_requirement_item.validation_policy_json IS '原平台可见、可操作、状态匹配校验策略。';
COMMENT ON COLUMN data_requirement_item.score_point_snapshot_json IS '本条数据关联的评分点快照。';
COMMENT ON COLUMN data_requirement_item.item_status IS '需求项状态：CREATED、REQUESTED、READY、VALIDATION_FAILED、FAILED、DISCARDED。';
COMMENT ON COLUMN data_requirement_item.external_business_id IS '原平台业务数据 ID。';
COMMENT ON COLUMN data_requirement_item.external_business_no IS '原平台业务单号。';
COMMENT ON COLUMN data_requirement_item.external_status IS '原平台业务数据状态摘要。';
COMMENT ON COLUMN data_requirement_item.validation_status IS '校验状态：NOT_CHECKED、PASSED、FAILED、PARTIAL、UNKNOWN。';
COMMENT ON COLUMN data_requirement_item.validation_time IS '校验时间。';
COMMENT ON COLUMN data_requirement_item.validation_result_json IS '原平台返回的可见性、可操作性和状态校验结果。';
COMMENT ON COLUMN data_requirement_item.failure_reason IS '失败原因。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_data_requirement_item_request
    ON data_requirement_item (tenant_id, request_batch_id, request_item_id)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_data_requirement_item_owner
    ON data_requirement_item (tenant_id, task_id, student_id, scene_type, item_status)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_data_requirement_item_requirement
    ON data_requirement_item (tenant_id, requirement_id, item_status)
    WHERE deleted = false;

ALTER TABLE teaching_data_pool ADD COLUMN IF NOT EXISTS strategy_id varchar(64);
ALTER TABLE teaching_data_pool ADD COLUMN IF NOT EXISTS requirement_id varchar(64);
ALTER TABLE teaching_data_pool ADD COLUMN IF NOT EXISTS idempotency_key varchar(128);
ALTER TABLE teaching_data_pool ADD COLUMN IF NOT EXISTS lock_version bigint NOT NULL DEFAULT 0;

COMMENT ON COLUMN teaching_data_pool.strategy_id IS '本数据池采用的数据策略 ID。';
COMMENT ON COLUMN teaching_data_pool.requirement_id IS '本数据池对应的数据需求批次 ID。';
COMMENT ON COLUMN teaching_data_pool.idempotency_key IS '创建数据池的业务幂等键。';
COMMENT ON COLUMN teaching_data_pool.lock_version IS '乐观锁版本号，用于并发领取和状态迁移。';

ALTER TABLE data_prepare_job ADD COLUMN IF NOT EXISTS idempotency_key varchar(128);
ALTER TABLE data_prepare_job ADD COLUMN IF NOT EXISTS external_request_id varchar(128);
ALTER TABLE data_prepare_job ADD COLUMN IF NOT EXISTS request_batch_id varchar(128);
ALTER TABLE data_prepare_job ADD COLUMN IF NOT EXISTS retry_count bigint NOT NULL DEFAULT 0;
ALTER TABLE data_prepare_job ADD COLUMN IF NOT EXISTS next_retry_time timestamp;
ALTER TABLE data_prepare_job ADD COLUMN IF NOT EXISTS trigger_type varchar(32);
ALTER TABLE data_prepare_job ADD COLUMN IF NOT EXISTS trace_id varchar(128);

COMMENT ON COLUMN data_prepare_job.idempotency_key IS '外部写操作幂等键。';
COMMENT ON COLUMN data_prepare_job.external_request_id IS '调用原平台时使用的外部请求 ID。';
COMMENT ON COLUMN data_prepare_job.request_batch_id IS '批量数据准备请求批次 ID。';
COMMENT ON COLUMN data_prepare_job.retry_count IS '重试次数。';
COMMENT ON COLUMN data_prepare_job.next_retry_time IS '下一次重试时间。';
COMMENT ON COLUMN data_prepare_job.trigger_type IS '触发方式：MANUAL、PUBLISH、ON_DEMAND、RETRY、SYSTEM。';
COMMENT ON COLUMN data_prepare_job.trace_id IS '链路追踪 ID。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_data_prepare_job_idempotency
    ON data_prepare_job (tenant_id, connector_system_id, idempotency_key)
    WHERE deleted = false AND idempotency_key IS NOT NULL;

ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS requirement_id varchar(64);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS requirement_item_id varchar(64);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS prepare_job_id varchar(64);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS request_batch_id varchar(128);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS request_item_id varchar(128);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS owner_external_org_id varchar(128);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS owner_external_org_name varchar(256);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS required_external_org_id varchar(128);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS required_external_org_name varchar(256);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS required_external_role_id varchar(128);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS required_external_role_name varchar(256);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS actor_type varchar(64);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS target_url varchar(1024);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS requirement_snapshot_json jsonb;
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS validation_status varchar(32);
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS validation_time timestamp;
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS validation_result_json jsonb;
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS failure_reason text;
ALTER TABLE teaching_data_instance ADD COLUMN IF NOT EXISTS lock_version bigint NOT NULL DEFAULT 0;

COMMENT ON COLUMN teaching_data_instance.requirement_id IS '生成该数据实例的数据需求批次 ID。';
COMMENT ON COLUMN teaching_data_instance.requirement_item_id IS '生成该数据实例的数据需求明细 ID。';
COMMENT ON COLUMN teaching_data_instance.prepare_job_id IS '生成该数据实例的数据准备任务 ID。';
COMMENT ON COLUMN teaching_data_instance.request_batch_id IS '原平台数据准备请求批次 ID。';
COMMENT ON COLUMN teaching_data_instance.request_item_id IS '原平台数据准备请求项 ID。';
COMMENT ON COLUMN teaching_data_instance.owner_external_org_id IS '原平台业务数据归属单位 ID。';
COMMENT ON COLUMN teaching_data_instance.owner_external_org_name IS '原平台业务数据归属单位名称。';
COMMENT ON COLUMN teaching_data_instance.required_external_org_id IS '本次教学要求的原平台操作单位 ID。';
COMMENT ON COLUMN teaching_data_instance.required_external_org_name IS '本次教学要求的原平台操作单位名称。';
COMMENT ON COLUMN teaching_data_instance.required_external_role_id IS '本次教学要求的原平台操作角色 ID。';
COMMENT ON COLUMN teaching_data_instance.required_external_role_name IS '本次教学要求的原平台操作角色名称。';
COMMENT ON COLUMN teaching_data_instance.actor_type IS '业务参与身份，如申请人、受理人、审核人。';
COMMENT ON COLUMN teaching_data_instance.target_url IS '原平台入口页面。';
COMMENT ON COLUMN teaching_data_instance.requirement_snapshot_json IS '创建该数据时的数据需求快照，重置时必须继承。';
COMMENT ON COLUMN teaching_data_instance.validation_status IS '原平台数据校验状态。';
COMMENT ON COLUMN teaching_data_instance.validation_time IS '原平台数据校验时间。';
COMMENT ON COLUMN teaching_data_instance.validation_result_json IS '原平台可见性、可操作性、状态匹配校验结果。';
COMMENT ON COLUMN teaching_data_instance.failure_reason IS '数据准备或校验失败原因。';
COMMENT ON COLUMN teaching_data_instance.lock_version IS '乐观锁版本号。';

CREATE INDEX IF NOT EXISTS idx_data_instance_requirement
    ON teaching_data_instance (tenant_id, requirement_id, requirement_item_id)
    WHERE deleted = false;

ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS required_external_org_id varchar(128);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS required_external_org_name varchar(256);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS required_external_role_id varchar(128);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS required_external_role_name varchar(256);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS actor_type varchar(64);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS segment_no bigint;
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS collaboration_unit_id varchar(64);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS requirement_snapshot_json jsonb;
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS consume_time timestamp;
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS release_reason varchar(256);

COMMENT ON COLUMN data_instance_allocation.required_external_org_id IS '领取该数据时要求的原平台单位 ID。';
COMMENT ON COLUMN data_instance_allocation.required_external_org_name IS '领取该数据时要求的原平台单位名称。';
COMMENT ON COLUMN data_instance_allocation.required_external_role_id IS '领取该数据时要求的原平台角色 ID。';
COMMENT ON COLUMN data_instance_allocation.required_external_role_name IS '领取该数据时要求的原平台角色名称。';
COMMENT ON COLUMN data_instance_allocation.actor_type IS '领取该数据时的业务参与身份。';
COMMENT ON COLUMN data_instance_allocation.segment_no IS '多角色业务片段编号。';
COMMENT ON COLUMN data_instance_allocation.collaboration_unit_id IS '多角色协作单元 ID。';
COMMENT ON COLUMN data_instance_allocation.requirement_snapshot_json IS '分配时的数据需求快照。';
COMMENT ON COLUMN data_instance_allocation.consume_time IS '数据实例被消费或完成的时间。';
COMMENT ON COLUMN data_instance_allocation.release_reason IS '释放、废弃或重置该分配的原因。';

CREATE INDEX IF NOT EXISTS idx_data_instance_allocation_collaboration
    ON data_instance_allocation (tenant_id, collaboration_unit_id, segment_no)
    WHERE deleted = false AND collaboration_unit_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS collaboration_unit (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    task_id varchar(64) NOT NULL,
    execution_id varchar(64),
    scene_type varchar(32) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    module_code varchar(128) NOT NULL,
    data_instance_id varchar(64) NOT NULL,
    external_business_id varchar(128) NOT NULL,
    unit_status varchar(32) NOT NULL DEFAULT 'CREATED',
    reset_count bigint NOT NULL DEFAULT 0,
    reset_from_unit_id varchar(64),
    lock_version bigint NOT NULL DEFAULT 0,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE collaboration_unit IS '协作单元表：多个学生围绕同一原平台业务数据分角色协同办理的聚合边界。';
COMMENT ON COLUMN collaboration_unit.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN collaboration_unit.tenant_id IS '租户 ID。';
COMMENT ON COLUMN collaboration_unit.task_id IS '教学任务 ID。';
COMMENT ON COLUMN collaboration_unit.execution_id IS '任务执行 ID。';
COMMENT ON COLUMN collaboration_unit.scene_type IS '教学场景：PRACTICE、EXAM。';
COMMENT ON COLUMN collaboration_unit.connector_system_id IS '原平台 ID。';
COMMENT ON COLUMN collaboration_unit.module_code IS '原平台功能模块编码。';
COMMENT ON COLUMN collaboration_unit.data_instance_id IS '协作单元共享的数据实例 ID。';
COMMENT ON COLUMN collaboration_unit.external_business_id IS '协作单元共享的原平台业务数据 ID。';
COMMENT ON COLUMN collaboration_unit.unit_status IS '协作状态：CREATED、RUNNING、COMPLETED、DISCARDED、ARCHIVED。';
COMMENT ON COLUMN collaboration_unit.reset_count IS '协作单元重置次数。';
COMMENT ON COLUMN collaboration_unit.reset_from_unit_id IS '重置时来源协作单元 ID。';
COMMENT ON COLUMN collaboration_unit.lock_version IS '乐观锁版本号。';

CREATE INDEX IF NOT EXISTS idx_collaboration_unit_task
    ON collaboration_unit (tenant_id, task_id, scene_type, unit_status)
    WHERE deleted = false;

CREATE TABLE IF NOT EXISTS collaboration_segment_allocation (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    collaboration_unit_id varchar(64) NOT NULL,
    task_id varchar(64) NOT NULL,
    student_id varchar(64) NOT NULL,
    segment_no bigint NOT NULL,
    actor_type varchar(64) NOT NULL,
    required_external_org_id varchar(128) NOT NULL,
    required_external_org_name varchar(256),
    required_external_role_id varchar(128) NOT NULL,
    required_external_role_name varchar(256),
    init_external_status varchar(64),
    target_external_status varchar(64),
    segment_status varchar(32) NOT NULL DEFAULT 'PENDING',
    start_time timestamp,
    finish_time timestamp,
    score_point_snapshot_json jsonb,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE collaboration_segment_allocation IS '协作片段分配表：记录协作单元中每个学生承担的单位、角色和业务片段。';
COMMENT ON COLUMN collaboration_segment_allocation.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN collaboration_segment_allocation.tenant_id IS '租户 ID。';
COMMENT ON COLUMN collaboration_segment_allocation.collaboration_unit_id IS '协作单元 ID。';
COMMENT ON COLUMN collaboration_segment_allocation.task_id IS '教学任务 ID。';
COMMENT ON COLUMN collaboration_segment_allocation.student_id IS '学生 ID。';
COMMENT ON COLUMN collaboration_segment_allocation.segment_no IS '协作片段编号。';
COMMENT ON COLUMN collaboration_segment_allocation.actor_type IS '业务参与身份。';
COMMENT ON COLUMN collaboration_segment_allocation.required_external_org_id IS '片段要求的原平台单位 ID。';
COMMENT ON COLUMN collaboration_segment_allocation.required_external_org_name IS '片段要求的原平台单位名称。';
COMMENT ON COLUMN collaboration_segment_allocation.required_external_role_id IS '片段要求的原平台角色 ID。';
COMMENT ON COLUMN collaboration_segment_allocation.required_external_role_name IS '片段要求的原平台角色名称。';
COMMENT ON COLUMN collaboration_segment_allocation.init_external_status IS '片段开始时要求的原平台状态。';
COMMENT ON COLUMN collaboration_segment_allocation.target_external_status IS '片段完成后要求的原平台状态。';
COMMENT ON COLUMN collaboration_segment_allocation.segment_status IS '片段状态：PENDING、RUNNING、COMPLETED、SKIPPED、FAILED。';
COMMENT ON COLUMN collaboration_segment_allocation.start_time IS '片段开始时间。';
COMMENT ON COLUMN collaboration_segment_allocation.finish_time IS '片段完成时间。';
COMMENT ON COLUMN collaboration_segment_allocation.score_point_snapshot_json IS '片段关联评分点快照。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_collaboration_segment_student
    ON collaboration_segment_allocation (tenant_id, collaboration_unit_id, segment_no, student_id)
    WHERE deleted = false;

ALTER TABLE exam_question_attempt ADD COLUMN IF NOT EXISTS data_requirement_item_id varchar(64);
ALTER TABLE exam_question_attempt ADD COLUMN IF NOT EXISTS launch_context_id varchar(64);
ALTER TABLE exam_question_attempt ADD COLUMN IF NOT EXISTS external_business_id varchar(128);
ALTER TABLE exam_question_attempt ADD COLUMN IF NOT EXISTS external_business_no varchar(128);
ALTER TABLE exam_question_attempt ADD COLUMN IF NOT EXISTS external_status_snapshot varchar(64);
ALTER TABLE exam_question_attempt ADD COLUMN IF NOT EXISTS data_requirement_snapshot_json jsonb;
ALTER TABLE exam_question_attempt ADD COLUMN IF NOT EXISTS score_point_snapshot_json jsonb;
ALTER TABLE exam_question_attempt ADD COLUMN IF NOT EXISTS evidence_snapshot_json jsonb;
ALTER TABLE exam_question_attempt ADD COLUMN IF NOT EXISTS abnormal_reason varchar(512);
ALTER TABLE exam_question_attempt ADD COLUMN IF NOT EXISTS lock_version bigint NOT NULL DEFAULT 0;

COMMENT ON COLUMN exam_question_attempt.data_requirement_item_id IS '本题绑定的数据需求明细 ID。';
COMMENT ON COLUMN exam_question_attempt.launch_context_id IS '最近一次进入原平台的启动上下文 ID。';
COMMENT ON COLUMN exam_question_attempt.external_business_id IS '考试题目绑定的原平台业务数据 ID。';
COMMENT ON COLUMN exam_question_attempt.external_business_no IS '考试题目绑定的原平台业务单号。';
COMMENT ON COLUMN exam_question_attempt.external_status_snapshot IS '考试题目开始或提交时的原平台状态快照。';
COMMENT ON COLUMN exam_question_attempt.data_requirement_snapshot_json IS '考试开始时的数据需求快照。';
COMMENT ON COLUMN exam_question_attempt.score_point_snapshot_json IS '考试开始时的评分点快照。';
COMMENT ON COLUMN exam_question_attempt.evidence_snapshot_json IS '提交或评分时的证据摘要快照。';
COMMENT ON COLUMN exam_question_attempt.abnormal_reason IS '异常原因。';
COMMENT ON COLUMN exam_question_attempt.lock_version IS '乐观锁版本号。';

ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS verify_request_id varchar(128);
ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS verify_time timestamp;
ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS sdk_config_snapshot_json jsonb;
ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS data_instance_validation_snapshot_json jsonb;

COMMENT ON COLUMN platform_launch_context.verify_request_id IS '原平台校验 launchToken 时的请求 ID。';
COMMENT ON COLUMN platform_launch_context.verify_time IS '原平台最近一次校验 launchToken 的时间。';
COMMENT ON COLUMN platform_launch_context.sdk_config_snapshot_json IS '下发给遮罩 SDK 的配置快照。';
COMMENT ON COLUMN platform_launch_context.data_instance_validation_snapshot_json IS '进入原平台时绑定的数据实例校验快照。';

ALTER TABLE evaluation_result ADD COLUMN IF NOT EXISTS score_point_snapshot_json jsonb;
ALTER TABLE evaluation_result ADD COLUMN IF NOT EXISTS evaluation_rule_snapshot_json jsonb;
ALTER TABLE evaluation_result ADD COLUMN IF NOT EXISTS evidence_snapshot_json jsonb;
ALTER TABLE evaluation_result ADD COLUMN IF NOT EXISTS review_status varchar(32);
ALTER TABLE evaluation_result ADD COLUMN IF NOT EXISTS review_reason varchar(512);

COMMENT ON COLUMN evaluation_result.score_point_snapshot_json IS '评分点发布快照。';
COMMENT ON COLUMN evaluation_result.evaluation_rule_snapshot_json IS '评分规则发布快照。';
COMMENT ON COLUMN evaluation_result.evidence_snapshot_json IS '用于评分的过程证据快照。';
COMMENT ON COLUMN evaluation_result.review_status IS '复核状态：NONE、PENDING、APPROVED、REJECTED、ADJUSTED。';
COMMENT ON COLUMN evaluation_result.review_reason IS '教师复核或调整原因。';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_business_module_connector'
    ) THEN
        ALTER TABLE business_module
            ADD CONSTRAINT fk_business_module_connector
            FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_module_data_strategy_business_module'
    ) THEN
        ALTER TABLE module_data_strategy
            ADD CONSTRAINT fk_module_data_strategy_business_module
            FOREIGN KEY (business_module_id) REFERENCES business_module(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_data_requirement_connector'
    ) THEN
        ALTER TABLE data_requirement
            ADD CONSTRAINT fk_data_requirement_connector
            FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_data_requirement_business_module'
    ) THEN
        ALTER TABLE data_requirement
            ADD CONSTRAINT fk_data_requirement_business_module
            FOREIGN KEY (business_module_id) REFERENCES business_module(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_data_requirement_item_requirement'
    ) THEN
        ALTER TABLE data_requirement_item
            ADD CONSTRAINT fk_data_requirement_item_requirement
            FOREIGN KEY (requirement_id) REFERENCES data_requirement(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_data_requirement_item_connector'
    ) THEN
        ALTER TABLE data_requirement_item
            ADD CONSTRAINT fk_data_requirement_item_connector
            FOREIGN KEY (connector_system_id) REFERENCES connector_system(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_collaboration_unit_instance'
    ) THEN
        ALTER TABLE collaboration_unit
            ADD CONSTRAINT fk_collaboration_unit_instance
            FOREIGN KEY (data_instance_id) REFERENCES teaching_data_instance(id);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_collaboration_segment_unit'
    ) THEN
        ALTER TABLE collaboration_segment_allocation
            ADD CONSTRAINT fk_collaboration_segment_unit
            FOREIGN KEY (collaboration_unit_id) REFERENCES collaboration_unit(id);
    END IF;
END $$;
