-- 取消物理外键约束
-- 目标数据库：PostgreSQL
-- 说明：
-- 1. 当前项目以原平台业务引用、教学快照、异步数据准备和软删除为核心，物理外键会放大初始化和演示数据装配成本。
-- 2. 本脚本只删除 FOREIGN KEY 约束，不删除主键、唯一索引、普通索引、字段或表。
-- 3. 引用合法性应由 Service 层在写入前校验，历史快照和原平台业务 ID 不依赖数据库级联维护。

SET client_encoding = 'UTF8';

ALTER TABLE IF EXISTS teach_user_role DROP CONSTRAINT IF EXISTS fk_teach_user_role_user;
ALTER TABLE IF EXISTS teach_user_role DROP CONSTRAINT IF EXISTS fk_teach_user_role_role;
ALTER TABLE IF EXISTS identity_binding DROP CONSTRAINT IF EXISTS fk_identity_binding_user;
ALTER TABLE IF EXISTS identity_binding DROP CONSTRAINT IF EXISTS fk_identity_binding_connector;
ALTER TABLE IF EXISTS teach_user_org DROP CONSTRAINT IF EXISTS fk_teach_user_org_user;
ALTER TABLE IF EXISTS teach_user_org DROP CONSTRAINT IF EXISTS fk_teach_user_org_org;
ALTER TABLE IF EXISTS connector_resource DROP CONSTRAINT IF EXISTS fk_connector_resource_system;
ALTER TABLE IF EXISTS platform_launch_context DROP CONSTRAINT IF EXISTS fk_launch_context_user;
ALTER TABLE IF EXISTS platform_launch_context DROP CONSTRAINT IF EXISTS fk_launch_context_connector;
ALTER TABLE IF EXISTS teaching_data_template DROP CONSTRAINT IF EXISTS fk_data_template_connector;
ALTER TABLE IF EXISTS teaching_data_instance DROP CONSTRAINT IF EXISTS fk_data_instance_template;
ALTER TABLE IF EXISTS teaching_data_instance DROP CONSTRAINT IF EXISTS fk_data_instance_connector;
ALTER TABLE IF EXISTS capture_session DROP CONSTRAINT IF EXISTS fk_capture_session_connector;
ALTER TABLE IF EXISTS capture_session DROP CONSTRAINT IF EXISTS fk_capture_session_teacher;
ALTER TABLE IF EXISTS capture_action_draft DROP CONSTRAINT IF EXISTS fk_action_draft_session;
ALTER TABLE IF EXISTS teaching_point DROP CONSTRAINT IF EXISTS fk_teaching_point_connector;
ALTER TABLE IF EXISTS task DROP CONSTRAINT IF EXISTS fk_task_course;
ALTER TABLE IF EXISTS task DROP CONSTRAINT IF EXISTS fk_task_publish_org;
ALTER TABLE IF EXISTS task_teaching_point DROP CONSTRAINT IF EXISTS fk_task_point_task;
ALTER TABLE IF EXISTS task_teaching_point DROP CONSTRAINT IF EXISTS fk_task_point_point;
ALTER TABLE IF EXISTS task_step DROP CONSTRAINT IF EXISTS fk_task_step_task;
ALTER TABLE IF EXISTS task_step DROP CONSTRAINT IF EXISTS fk_task_step_point;
ALTER TABLE IF EXISTS task_execution DROP CONSTRAINT IF EXISTS fk_task_execution_task;
ALTER TABLE IF EXISTS task_execution DROP CONSTRAINT IF EXISTS fk_task_execution_student;
ALTER TABLE IF EXISTS task_execution DROP CONSTRAINT IF EXISTS fk_task_execution_connector;
ALTER TABLE IF EXISTS task_execution_context DROP CONSTRAINT IF EXISTS fk_execution_context_execution;
ALTER TABLE IF EXISTS execution_trace DROP CONSTRAINT IF EXISTS fk_execution_trace_execution;
ALTER TABLE IF EXISTS practice_attempt DROP CONSTRAINT IF EXISTS fk_practice_attempt_execution;
ALTER TABLE IF EXISTS practice_attempt DROP CONSTRAINT IF EXISTS fk_practice_attempt_student;
ALTER TABLE IF EXISTS practice_attempt DROP CONSTRAINT IF EXISTS fk_practice_attempt_task;
ALTER TABLE IF EXISTS practice_step_result DROP CONSTRAINT IF EXISTS fk_practice_step_attempt;
ALTER TABLE IF EXISTS practice_score_summary DROP CONSTRAINT IF EXISTS fk_practice_summary_student;
ALTER TABLE IF EXISTS evaluation_item DROP CONSTRAINT IF EXISTS fk_evaluation_item_rule;
ALTER TABLE IF EXISTS evaluation_result DROP CONSTRAINT IF EXISTS fk_evaluation_result_execution;
ALTER TABLE IF EXISTS evaluation_result DROP CONSTRAINT IF EXISTS fk_evaluation_result_rule;

ALTER TABLE IF EXISTS platform_capability DROP CONSTRAINT IF EXISTS fk_platform_capability_connector;
ALTER TABLE IF EXISTS module_data_strategy DROP CONSTRAINT IF EXISTS fk_module_data_strategy_connector;
ALTER TABLE IF EXISTS lesson_plan DROP CONSTRAINT IF EXISTS fk_lesson_plan_connector;
ALTER TABLE IF EXISTS lesson_plan DROP CONSTRAINT IF EXISTS fk_lesson_plan_teaching_point;
ALTER TABLE IF EXISTS operation_flow_path DROP CONSTRAINT IF EXISTS fk_operation_flow_path_lesson_plan;
ALTER TABLE IF EXISTS operation_question DROP CONSTRAINT IF EXISTS fk_operation_question_lesson_plan;
ALTER TABLE IF EXISTS operation_question DROP CONSTRAINT IF EXISTS fk_operation_question_flow_path;
ALTER TABLE IF EXISTS exam_paper DROP CONSTRAINT IF EXISTS fk_exam_paper_course;
ALTER TABLE IF EXISTS exam_paper_question DROP CONSTRAINT IF EXISTS fk_exam_paper_question_paper;
ALTER TABLE IF EXISTS exam_paper_question DROP CONSTRAINT IF EXISTS fk_exam_paper_question_question;
ALTER TABLE IF EXISTS teaching_data_pool DROP CONSTRAINT IF EXISTS fk_teaching_data_pool_connector;
ALTER TABLE IF EXISTS data_prepare_job DROP CONSTRAINT IF EXISTS fk_data_prepare_job_connector;
ALTER TABLE IF EXISTS data_instance_allocation DROP CONSTRAINT IF EXISTS fk_data_instance_allocation_pool;
ALTER TABLE IF EXISTS data_instance_allocation DROP CONSTRAINT IF EXISTS fk_data_instance_allocation_instance;
ALTER TABLE IF EXISTS exam_attempt DROP CONSTRAINT IF EXISTS fk_exam_attempt_execution;
ALTER TABLE IF EXISTS exam_attempt DROP CONSTRAINT IF EXISTS fk_exam_attempt_paper;
ALTER TABLE IF EXISTS exam_question_attempt DROP CONSTRAINT IF EXISTS fk_exam_question_attempt_exam;
ALTER TABLE IF EXISTS exam_question_attempt DROP CONSTRAINT IF EXISTS fk_exam_question_attempt_data_instance;

ALTER TABLE IF EXISTS business_module DROP CONSTRAINT IF EXISTS fk_business_module_connector;
ALTER TABLE IF EXISTS module_data_strategy DROP CONSTRAINT IF EXISTS fk_module_data_strategy_business_module;
ALTER TABLE IF EXISTS data_requirement DROP CONSTRAINT IF EXISTS fk_data_requirement_connector;
ALTER TABLE IF EXISTS data_requirement DROP CONSTRAINT IF EXISTS fk_data_requirement_business_module;
ALTER TABLE IF EXISTS data_requirement_item DROP CONSTRAINT IF EXISTS fk_data_requirement_item_requirement;
ALTER TABLE IF EXISTS data_requirement_item DROP CONSTRAINT IF EXISTS fk_data_requirement_item_connector;
ALTER TABLE IF EXISTS collaboration_unit DROP CONSTRAINT IF EXISTS fk_collaboration_unit_instance;
ALTER TABLE IF EXISTS collaboration_segment_allocation DROP CONSTRAINT IF EXISTS fk_collaboration_segment_unit;
