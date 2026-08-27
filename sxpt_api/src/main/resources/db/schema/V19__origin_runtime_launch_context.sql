ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS launch_entry_type varchar(32);
ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS origin_home_url varchar(1024);
ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS capture_session_id varchar(64);
ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS practice_attempt_id varchar(64);
ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS exam_attempt_id varchar(64);
ALTER TABLE platform_launch_context ADD COLUMN IF NOT EXISTS question_attempt_id varchar(64);

COMMENT ON COLUMN platform_launch_context.launch_entry_type IS '原平台启动入口类型：MODULE_ENTRY 表示旧模块入口，PLATFORM_HOME 表示进入原平台首页。';
COMMENT ON COLUMN platform_launch_context.origin_home_url IS '原平台首页地址，用于新模式下只进入原平台首页。';
COMMENT ON COLUMN platform_launch_context.capture_session_id IS '老师备案采集会话 ID。';
COMMENT ON COLUMN platform_launch_context.practice_attempt_id IS '学生练习尝试 ID。';
COMMENT ON COLUMN platform_launch_context.exam_attempt_id IS '学生考试尝试 ID。';
COMMENT ON COLUMN platform_launch_context.question_attempt_id IS '学生考试题目作答实例 ID。';
