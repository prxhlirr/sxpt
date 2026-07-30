CREATE TABLE IF NOT EXISTS sys_role_permission (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    role_id varchar(64) NOT NULL,
    permission_id varchar(64) NOT NULL,
    grant_source varchar(64),
    create_by varchar(64),
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE sys_role_permission IS '角色权限绑定表：维护教学平台角色与系统权限点之间的授权关系。';
COMMENT ON COLUMN sys_role_permission.role_id IS '教学平台角色 ID，弱引用 teach_role。';
COMMENT ON COLUMN sys_role_permission.permission_id IS '系统权限点 ID，弱引用 sys_permission_config。';
COMMENT ON COLUMN sys_role_permission.grant_source IS '授权来源：ADMIN、IMPORT、SYNC 等。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_role_permission
    ON sys_role_permission (tenant_id, role_id, permission_id)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_sys_role_permission_role
    ON sys_role_permission (tenant_id, role_id)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_sys_role_permission_permission
    ON sys_role_permission (tenant_id, permission_id)
    WHERE deleted = false;
