CREATE TABLE IF NOT EXISTS sys_menu_config (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    parent_id varchar(64),
    menu_code varchar(128) NOT NULL,
    menu_name varchar(128) NOT NULL,
    menu_type varchar(32) NOT NULL,
    route_path varchar(256),
    component_path varchar(256),
    permission_code varchar(128),
    icon varchar(64),
    sort_no integer NOT NULL DEFAULT 0,
    visible boolean NOT NULL DEFAULT true,
    create_by varchar(64),
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE sys_menu_config IS '系统菜单配置表：维护教学平台后台、教师端、学生端可展示的菜单入口。';
COMMENT ON COLUMN sys_menu_config.parent_id IS '上级菜单 ID，空表示一级菜单。';
COMMENT ON COLUMN sys_menu_config.menu_code IS '菜单编码，租户内唯一，用于权限绑定和前端定位。';
COMMENT ON COLUMN sys_menu_config.menu_type IS '菜单类型：DIR、MENU、BUTTON。';
COMMENT ON COLUMN sys_menu_config.route_path IS '前端路由地址。';
COMMENT ON COLUMN sys_menu_config.component_path IS '前端组件路径或约定标识。';
COMMENT ON COLUMN sys_menu_config.permission_code IS '菜单或按钮需要的权限编码。';
COMMENT ON COLUMN sys_menu_config.visible IS '是否在导航中可见。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_menu_config_code
    ON sys_menu_config (tenant_id, menu_code)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_sys_menu_config_parent
    ON sys_menu_config (tenant_id, parent_id, sort_no)
    WHERE deleted = false;

CREATE TABLE IF NOT EXISTS sys_permission_config (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    permission_code varchar(128) NOT NULL,
    permission_name varchar(128) NOT NULL,
    resource_type varchar(32) NOT NULL,
    resource_code varchar(128),
    action_code varchar(64) NOT NULL,
    description varchar(512),
    create_by varchar(64),
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE sys_permission_config IS '系统权限配置表：维护菜单、按钮、接口等可授权资源动作。';
COMMENT ON COLUMN sys_permission_config.permission_code IS '权限编码，租户内唯一，例如 data:prepare:create。';
COMMENT ON COLUMN sys_permission_config.resource_type IS '资源类型：MENU、BUTTON、API、DATA。';
COMMENT ON COLUMN sys_permission_config.resource_code IS '资源编码，可关联菜单编码、接口编码或数据范围编码。';
COMMENT ON COLUMN sys_permission_config.action_code IS '动作编码：VIEW、CREATE、EDIT、DISABLE、EXPORT 等。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_permission_config_code
    ON sys_permission_config (tenant_id, permission_code)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_sys_permission_config_resource
    ON sys_permission_config (tenant_id, resource_type, resource_code)
    WHERE deleted = false;

CREATE TABLE IF NOT EXISTS sys_dict_item (
    id varchar(64) PRIMARY KEY,
    tenant_id varchar(64) NOT NULL,
    dict_code varchar(128) NOT NULL,
    dict_name varchar(128) NOT NULL,
    item_code varchar(128) NOT NULL,
    item_name varchar(128) NOT NULL,
    item_value varchar(256) NOT NULL,
    sort_no integer NOT NULL DEFAULT 0,
    remark varchar(512),
    create_by varchar(64),
    create_time timestamp NOT NULL DEFAULT now(),
    update_by varchar(64),
    update_time timestamp NOT NULL DEFAULT now(),
    status varchar(32) NOT NULL DEFAULT 'ACTIVE',
    deleted boolean NOT NULL DEFAULT false
);

COMMENT ON TABLE sys_dict_item IS '系统字典项表：维护页面下拉框、状态枚举和业务分类等可配置值。';
COMMENT ON COLUMN sys_dict_item.dict_code IS '字典编码，例如 user_type、org_type、data_prepare_status。';
COMMENT ON COLUMN sys_dict_item.item_code IS '字典项编码，同一字典内唯一。';
COMMENT ON COLUMN sys_dict_item.item_value IS '字典项实际值，供前端表单或后端规则使用。';

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_dict_item_code
    ON sys_dict_item (tenant_id, dict_code, item_code)
    WHERE deleted = false;

CREATE INDEX IF NOT EXISTS idx_sys_dict_item_dict
    ON sys_dict_item (tenant_id, dict_code, sort_no)
    WHERE deleted = false;
