CREATE TABLE IF NOT EXISTS business_module_process_step (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    business_module_id varchar(64) NOT NULL,
    module_code varchar(128) NOT NULL,
    step_no integer NOT NULL,
    step_code varchar(128) NOT NULL,
    step_name varchar(256) NOT NULL,
    step_type varchar(64),
    init_external_status varchar(64),
    target_external_status varchar(64),
    completion_rule_json jsonb,
    remark varchar(512),
    lock_version bigint NOT NULL DEFAULT 0,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE business_module_process_step IS '业务模块标准办理步骤表：定义原平台业务模块在教学场景中的标准流程步骤。';
COMMENT ON COLUMN business_module_process_step.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN business_module_process_step.tenant_id IS '租户 ID。';
COMMENT ON COLUMN business_module_process_step.connector_system_id IS '所属原平台系统 ID。';
COMMENT ON COLUMN business_module_process_step.business_module_id IS '所属业务模块 ID。';
COMMENT ON COLUMN business_module_process_step.module_code IS '业务模块编码，用于快照和跨表对账。';
COMMENT ON COLUMN business_module_process_step.step_no IS '步骤顺序，同一模块内不可重复。';
COMMENT ON COLUMN business_module_process_step.step_code IS '步骤编码，同一模块内不可重复。';
COMMENT ON COLUMN business_module_process_step.step_name IS '步骤名称。';
COMMENT ON COLUMN business_module_process_step.step_type IS '步骤类型，例如 FILL、APPROVE、REVIEW、COUNTERSIGN、ARCHIVE、SYSTEM。';
COMMENT ON COLUMN business_module_process_step.init_external_status IS '进入该步骤前的原平台状态。';
COMMENT ON COLUMN business_module_process_step.target_external_status IS '完成该步骤后的原平台状态。';
COMMENT ON COLUMN business_module_process_step.completion_rule_json IS '步骤完成规则，用于描述动作、状态和校验条件。';
COMMENT ON COLUMN business_module_process_step.remark IS '业务说明。';
COMMENT ON COLUMN business_module_process_step.lock_version IS '乐观锁版本号，用于防止并发覆盖配置。';
COMMENT ON COLUMN business_module_process_step.create_by IS '创建人 ID。';
COMMENT ON COLUMN business_module_process_step.create_time IS '创建时间。';
COMMENT ON COLUMN business_module_process_step.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN business_module_process_step.update_time IS '最后更新时间。';
COMMENT ON COLUMN business_module_process_step.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN business_module_process_step.deleted IS '软删除标记。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_business_module_process_step_code
    ON business_module_process_step (tenant_id, business_module_id, step_code)
    WHERE deleted = false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_business_module_process_step_no
    ON business_module_process_step (tenant_id, business_module_id, step_no)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_business_module_process_step_module
    ON business_module_process_step (tenant_id, connector_system_id, business_module_id)
    WHERE deleted = false;

CREATE TABLE IF NOT EXISTS business_module_process_actor (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    connector_system_id varchar(64) NOT NULL,
    business_module_id varchar(64) NOT NULL,
    process_step_id varchar(64) NOT NULL,
    module_code varchar(128) NOT NULL,
    step_code varchar(128) NOT NULL,
    actor_no integer NOT NULL,
    actor_relation varchar(64) NOT NULL,
    actor_type varchar(64) NOT NULL,
    required_org_type varchar(64),
    required_org_code varchar(128),
    required_org_name varchar(256),
    required_role_code varchar(128),
    required_role_name varchar(256),
    is_required boolean NOT NULL DEFAULT true,
    assignment_rule varchar(64) NOT NULL DEFAULT 'CURRENT_STUDENT',
    remark varchar(512),
    lock_version bigint NOT NULL DEFAULT 0,
    create_by varchar(64) NOT NULL,
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64) NOT NULL,
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE business_module_process_actor IS '业务模块步骤参与方表：定义一个标准办理步骤下需要哪些单位和角色共同参与。';
COMMENT ON COLUMN business_module_process_actor.id IS '主键 ID，由应用层统一生成。';
COMMENT ON COLUMN business_module_process_actor.tenant_id IS '租户 ID。';
COMMENT ON COLUMN business_module_process_actor.connector_system_id IS '所属原平台系统 ID。';
COMMENT ON COLUMN business_module_process_actor.business_module_id IS '所属业务模块 ID。';
COMMENT ON COLUMN business_module_process_actor.process_step_id IS '所属标准办理步骤 ID。';
COMMENT ON COLUMN business_module_process_actor.module_code IS '业务模块编码，用于快照和跨表对账。';
COMMENT ON COLUMN business_module_process_actor.step_code IS '步骤编码，用于和实例链路快照匹配。';
COMMENT ON COLUMN business_module_process_actor.actor_no IS '参与方顺序，同一步骤内不可重复。';
COMMENT ON COLUMN business_module_process_actor.actor_relation IS '参与关系，例如 PRIMARY、COOPERATE、APPROVER、REVIEWER、COUNTERSIGN、CC。';
COMMENT ON COLUMN business_module_process_actor.actor_type IS '参与人类型，例如 STUDENT、TEACHER、SYSTEM、ORIGIN_USER。';
COMMENT ON COLUMN business_module_process_actor.required_org_type IS '所需单位类型。';
COMMENT ON COLUMN business_module_process_actor.required_org_code IS '所需单位编码，可为空。';
COMMENT ON COLUMN business_module_process_actor.required_org_name IS '所需单位名称，可为空。';
COMMENT ON COLUMN business_module_process_actor.required_role_code IS '所需角色编码。';
COMMENT ON COLUMN business_module_process_actor.required_role_name IS '所需角色名称。';
COMMENT ON COLUMN business_module_process_actor.is_required IS '是否必须参与。';
COMMENT ON COLUMN business_module_process_actor.assignment_rule IS '分配规则，例如 CURRENT_STUDENT、GROUP_MEMBER、TEACHER、SYSTEM。';
COMMENT ON COLUMN business_module_process_actor.remark IS '业务说明。';
COMMENT ON COLUMN business_module_process_actor.lock_version IS '乐观锁版本号，用于防止并发覆盖配置。';
COMMENT ON COLUMN business_module_process_actor.create_by IS '创建人 ID。';
COMMENT ON COLUMN business_module_process_actor.create_time IS '创建时间。';
COMMENT ON COLUMN business_module_process_actor.update_by IS '最后更新人 ID。';
COMMENT ON COLUMN business_module_process_actor.update_time IS '最后更新时间。';
COMMENT ON COLUMN business_module_process_actor.status IS '通用状态：ACTIVE、DISABLED。';
COMMENT ON COLUMN business_module_process_actor.deleted IS '软删除标记。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_business_module_process_actor_no
    ON business_module_process_actor (tenant_id, process_step_id, actor_no)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_business_module_process_actor_step
    ON business_module_process_actor (tenant_id, process_step_id)
    WHERE deleted = false;

ALTER TABLE data_requirement_item ADD COLUMN IF NOT EXISTS external_business_name varchar(256);
ALTER TABLE data_requirement_item ADD COLUMN IF NOT EXISTS current_step_code varchar(128);
ALTER TABLE data_requirement_item ADD COLUMN IF NOT EXISTS current_actor_no integer;
ALTER TABLE data_requirement_item ADD COLUMN IF NOT EXISTS current_org_id varchar(128);
ALTER TABLE data_requirement_item ADD COLUMN IF NOT EXISTS current_org_name varchar(256);
ALTER TABLE data_requirement_item ADD COLUMN IF NOT EXISTS current_role_id varchar(128);
ALTER TABLE data_requirement_item ADD COLUMN IF NOT EXISTS current_role_name varchar(256);
ALTER TABLE data_requirement_item ADD COLUMN IF NOT EXISTS process_chain_snapshot_json jsonb;

COMMENT ON COLUMN data_requirement_item.external_business_name IS '原平台业务数据名称。';
COMMENT ON COLUMN data_requirement_item.current_step_code IS '原平台返回或教学平台推断出的当前办理步骤编码。';
COMMENT ON COLUMN data_requirement_item.current_actor_no IS '当前步骤下默认进入的参与方序号。';
COMMENT ON COLUMN data_requirement_item.current_org_id IS '当前进入原平台所需的单位 ID。';
COMMENT ON COLUMN data_requirement_item.current_org_name IS '当前进入原平台所需的单位名称。';
COMMENT ON COLUMN data_requirement_item.current_role_id IS '当前进入原平台所需的角色 ID。';
COMMENT ON COLUMN data_requirement_item.current_role_name IS '当前进入原平台所需的角色名称。';
COMMENT ON COLUMN data_requirement_item.process_chain_snapshot_json IS '数据实例办理链快照；原平台未返回完整链路时由模块标准链生成。';

ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS request_batch_id varchar(128);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS request_item_id varchar(128);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS student_id varchar(64);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS student_name varchar(128);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS class_id varchar(64);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS connector_system_id varchar(64);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS business_module_id varchar(64);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS external_business_id varchar(128);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS external_business_name varchar(256);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS target_url varchar(1024);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS process_step_code varchar(128);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS process_step_name varchar(256);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS process_actor_no integer;
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS actor_relation varchar(64);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS origin_org_id varchar(128);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS origin_org_name varchar(256);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS origin_role_id varchar(128);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS origin_role_name varchar(256);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS allocation_lock_status varchar(32);
ALTER TABLE data_instance_allocation ADD COLUMN IF NOT EXISTS actor_snapshot_json jsonb;

COMMENT ON COLUMN data_instance_allocation.request_batch_id IS '数据准备批次 ID，用于追溯本次分配来自哪次造数。';
COMMENT ON COLUMN data_instance_allocation.request_item_id IS '数据准备明细 ID，用于追溯本次分配来自哪条造数结果。';
COMMENT ON COLUMN data_instance_allocation.student_id IS '学生 ID，优先与 owner_user_id 保持一致，便于后台按学生维度查询。';
COMMENT ON COLUMN data_instance_allocation.student_name IS '学生姓名快照。';
COMMENT ON COLUMN data_instance_allocation.class_id IS '班级 ID。';
COMMENT ON COLUMN data_instance_allocation.connector_system_id IS '原平台系统 ID。';
COMMENT ON COLUMN data_instance_allocation.business_module_id IS '业务模块 ID。';
COMMENT ON COLUMN data_instance_allocation.external_business_id IS '原平台业务数据 ID。';
COMMENT ON COLUMN data_instance_allocation.external_business_name IS '原平台业务数据名称。';
COMMENT ON COLUMN data_instance_allocation.target_url IS '原平台访问地址。';
COMMENT ON COLUMN data_instance_allocation.process_step_code IS '分配到的办理步骤编码。';
COMMENT ON COLUMN data_instance_allocation.process_step_name IS '分配到的办理步骤名称。';
COMMENT ON COLUMN data_instance_allocation.process_actor_no IS '分配到的步骤参与方序号。';
COMMENT ON COLUMN data_instance_allocation.actor_relation IS '分配到的参与关系，例如 PRIMARY、COOPERATE、APPROVER。';
COMMENT ON COLUMN data_instance_allocation.origin_org_id IS '分配时确定的原平台单位 ID。';
COMMENT ON COLUMN data_instance_allocation.origin_org_name IS '分配时确定的原平台单位名称。';
COMMENT ON COLUMN data_instance_allocation.origin_role_id IS '分配时确定的原平台角色 ID。';
COMMENT ON COLUMN data_instance_allocation.origin_role_name IS '分配时确定的原平台角色名称。';
COMMENT ON COLUMN data_instance_allocation.allocation_lock_status IS '分配锁定状态，用于考试和强约束练习。';
COMMENT ON COLUMN data_instance_allocation.actor_snapshot_json IS '分配时的步骤参与方快照。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_data_instance_allocation_student_actor
    ON data_instance_allocation (tenant_id, request_batch_id, student_id, process_step_code, process_actor_no)
    WHERE deleted = false
      AND request_batch_id IS NOT NULL
      AND student_id IS NOT NULL
      AND process_step_code IS NOT NULL
      AND process_actor_no IS NOT NULL;

ALTER TABLE module_data_strategy ADD COLUMN IF NOT EXISTS allocation_mode varchar(64) NOT NULL DEFAULT 'ON_FIRST_ENTER';

COMMENT ON COLUMN module_data_strategy.allocation_mode IS '数据实例分配模式：PRE_ALLOCATE、ON_FIRST_ENTER、GROUP_PRE_ALLOCATE、ROLE_SPECIFIED。';
