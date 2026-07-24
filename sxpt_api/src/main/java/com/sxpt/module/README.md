# 业务模块边界说明

本文档用于约束 `com.sxpt.module` 下的 MVP 业务模块边界。

后续实现应按模块逐批补充 `controller`、`service`、`service.impl`、`mapper`、`entity`、`dto`、`vo`、`enums`，不要一次性生成大量空类。

## 模块清单

| 模块 | 主要职责 | 对应核心表 |
|---|---|---|
| `user` | 教学平台用户、角色、教学组织和班级关系 | `teach_user`、`teach_role`、`teach_user_role`、`teach_org`、`teach_user_org` |
| `connector` | 原业务平台接入配置、原平台身份绑定、正式页面资源 | `connector_system`、`identity_binding`、`connector_resource` |
| `launchcontext` | 教学平台跳转原平台的启动上下文与 launchToken 校验 | `platform_launch_context` |
| `teachingdata` | 教学业务数据模板和原平台业务数据实例引用 | `teaching_data_template`、`teaching_data_instance` |
| `capture` | 教师备案采集、采集事件、资源快照、动作草稿 | `capture_session`、`capture_event`、`capture_resource_snapshot`、`capture_action_draft` |
| `teaching` | 教学点发布、教学点版本和备案路径 | `teaching_point` |
| `course` | 课程、任务、任务教学点和任务步骤 | `course`、`task`、`task_teaching_point`、`task_step` |
| `execution` | 学生任务执行、运行上下文和执行轨迹 | `task_execution`、`task_execution_context`、`execution_trace` |
| `practice` | 练习次数、练习步骤结果和练习过程分汇总 | `practice_attempt`、`practice_step_result`、`practice_score_summary` |
| `evaluation` | 评分规则、评分项、自动评分和教师复核结果 | `evaluation_rule`、`evaluation_item`、`evaluation_result` |

## 分层约束

每个模块内部统一采用以下分层：

```text
controller     HTTP 入参、参数校验触发、统一响应封装
service        业务服务接口，声明业务动作
service.impl   业务编排、事务边界、状态流转
mapper         MyBatis Plus 数据访问接口
entity         数据库表映射对象
dto            请求入参对象
vo             返回前端的展示对象
enums          模块内枚举
```

## 执行原则

1. Controller 不直接调用 Mapper。
2. Service 不向前端返回 Entity。
3. DTO 和 VO 不复用 Entity。
4. 原平台真实业务明细不进入教学平台主表，只保存引用、摘要和必要快照。
5. 高增长事实表的查询优先从主线 ID 进入，例如 `execution_id`、`capture_session_id`、`attempt_id`。
6. 后续接口、方法和服务必须补充业务功能、关键方法和流程说明注释。
