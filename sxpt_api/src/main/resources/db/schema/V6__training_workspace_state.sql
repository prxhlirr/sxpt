-- 教案编排完整状态持久化。
-- 每位教师在一个租户内拥有独立工作区；学生任务由服务端按当前 JWT 用户过滤后汇总。

SET client_encoding = 'UTF8';

CREATE TABLE IF NOT EXISTS training_workspace_state (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    owner_user_id VARCHAR(64) NOT NULL,
    workspace_json TEXT NOT NULL,
    version_no BIGINT NOT NULL DEFAULT 1,
    create_by VARCHAR(64) NOT NULL,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_by VARCHAR(64) NOT NULL,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_training_workspace_tenant_owner
    ON training_workspace_state (tenant_id, owner_user_id)
    WHERE deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_training_workspace_tenant
    ON training_workspace_state (tenant_id, status, deleted);
