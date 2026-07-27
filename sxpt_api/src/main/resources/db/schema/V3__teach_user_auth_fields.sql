ALTER TABLE teach_user ADD COLUMN password_hash varchar(255);
ALTER TABLE teach_user ADD COLUMN password_salt varchar(128);
ALTER TABLE teach_user ADD COLUMN password_algorithm varchar(64);
ALTER TABLE teach_user ADD COLUMN password_iterations integer NOT NULL DEFAULT 120000;
ALTER TABLE teach_user ADD COLUMN password_status varchar(32) NOT NULL DEFAULT 'RESET_REQUIRED';
ALTER TABLE teach_user ADD COLUMN password_updated_time timestamptz;
ALTER TABLE teach_user ADD COLUMN last_login_time timestamptz;
ALTER TABLE teach_user ADD COLUMN failed_login_count integer NOT NULL DEFAULT 0;
ALTER TABLE teach_user ADD COLUMN locked_until timestamptz;
ALTER TABLE teach_user ADD COLUMN student_no varchar(64);
ALTER TABLE teach_user ADD COLUMN employee_no varchar(64);

COMMENT ON COLUMN teach_user.password_hash IS '密码哈希，使用 password_algorithm 和 password_salt 生成，不保存明文密码。';
COMMENT ON COLUMN teach_user.password_salt IS '每用户独立 salt，避免相同密码产生相同哈希。';
COMMENT ON COLUMN teach_user.password_algorithm IS '密码哈希算法，例如 PBKDF2WithHmacSHA256。';
COMMENT ON COLUMN teach_user.password_iterations IS '密码哈希迭代次数，用于平衡安全性和性能。';
COMMENT ON COLUMN teach_user.password_status IS '密码状态：NORMAL、RESET_REQUIRED、DISABLED。';
COMMENT ON COLUMN teach_user.password_updated_time IS '密码最后更新时间。';
COMMENT ON COLUMN teach_user.last_login_time IS '最近一次登录成功时间。';
COMMENT ON COLUMN teach_user.failed_login_count IS '连续登录失败次数，用于账号锁定策略。';
COMMENT ON COLUMN teach_user.locked_until IS '账号临时锁定截止时间。';
COMMENT ON COLUMN teach_user.student_no IS '学生学号，非学生用户可为空。';
COMMENT ON COLUMN teach_user.employee_no IS '教师或管理员工号，学生用户可为空。';

CREATE INDEX idx_teach_user_student_no ON teach_user (tenant_id, student_no) WHERE deleted = false AND student_no IS NOT NULL;
CREATE INDEX idx_teach_user_employee_no ON teach_user (tenant_id, employee_no) WHERE deleted = false AND employee_no IS NOT NULL;
