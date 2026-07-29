# 前后端接口对接说明

## 项目位置

- 前端：`sxpt_web`
- 后端：`sxpt_api`
- 运行时接口配置：`sxpt_web/public/api-config.js`
- Axios 统一客户端：`sxpt_web/src/api/http.ts`
- 分模块接口：`sxpt_web/src/api`
- 页面业务适配：`sxpt_web/src/services/backendTrainingApi.ts`

旧目录 `D:\sxptObject` 仅作为历史版本参考，本次修改均位于当前 Git 仓库。

## 运行时配置

`api-config.js` 会在 Vue 入口之前加载。修改 `apiBaseUrl` 即可切换后端：

```js
window.__SXPT_API_CONFIG__ = {
  enabled: true,
  apiBaseUrl: 'http://127.0.0.1:8080/api/v1',
  timeoutMs: 15000,
  bearerToken: '',
  autoDevToken: true,
  tenantId: 'demo-tenant',
  currentUserId: 'demo-teacher',
  currentUsername: 'demo-teacher',
  simulatedOrgId: 'demo-class',
  simulatedStudentId: 'demo-student',
  simulatedStudentName: '模拟学生'
};
```

正式环境应使用登录接口生成的 JWT，并关闭 `autoDevToken`。本地联调时，后端需启用 `dev` profile 才会提供 `/auth/token`。

## Axios 公共行为

- 自动拼接后端基础地址。
- 自动携带 `Authorization: Bearer <token>`。
- 开发模式 Token 为空时可自动申请测试 Token。
- 自动解包 `ApiResult<T>.result`。
- HTTP 错误、业务 `success=false` 和超时统一转换为 `ApiRequestError`。
- Token 缓存在浏览器键 `sxpt_web.api.token`。

## 已接入接口

| 模块 | 接口范围 |
|---|---|
| 认证 | `/auth/login`、`/auth/me` |
| 用户组织 | `/user`、`/roles`、`/user-roles`、`/orgs`、`/orgs/users` |
| 原平台 | `/connector/system`、`/identity-bindings`、`/launch-contexts` |
| 教学数据 | `/connector/data-templates`、`/data-instances` |
| 备案采集 | `/capture/sessions`、`/segment-switches`、`/events`、`/resource-snapshots`、`/action-drafts` |
| 正式资源 | `/connector/resources` |
| 教学发布 | `/teaching/points`、`/teaching/task-steps` |
| 课程任务 | `/courses`、`/tasks`、`/tasks/teaching-points` |
| 评价规则 | `/evaluation/config/rules`、`/evaluation/config/items` |
| 学生执行 | `/student/task-executions`、`/sdk/{mode}/context`、`/execution/traces` |
| 健康检查 | `/system/health`、`/db-health`、`/redis-health` |

## 页面主链

1. 进入“业务平台管理”时从后端读取原平台配置。
2. 新增、编辑、启停业务平台时直接调用后端。
3. 教师点击“开始录制”时创建采集会话。
4. 每个采集节点依次上报事件、关键资源摘要并创建动作草稿。
5. 发布教案前自动补传失败或未同步的节点。
6. 发布时为节点沉淀正式资源、确认草稿、创建教学点并结束采集会话。
7. 教师在全屏业务界面完成一遍讲解后，发布学习任务和练习任务。
8. 发布任务时自动解析模拟班级、创建课程、教学任务、任务步骤和评价规则，并完成教学资产发布检查。
9. 学生开始任务时创建后端执行记录并读取 LEARNING、PRACTICE 或 EXAM 运行态上下文。
10. 学生完成阶段时逐步骤上报 `STEP_COMPLETED` 轨迹，最终提交触发自动评价并回写得分。
11. 学习、练习均已发布后，教师可发布考试任务；考试沿用同一执行与评分链路。

## 业务页面快照协议

录制节点同时保存动作信息与“动作执行后的完整业务页面状态”。教师讲解和学生
`LEARNING` 模式回放只读页面快照；`PRACTICE`、`EXAM` 模式仍加载可操作的业务系统。

同源内置页面由实训平台直接序列化 DOM、表单当前值和可访问 CSS。跨域业务系统受浏览器
同源策略限制，父页面不能直接读取 iframe DOM，必须由业务系统接入脚本在动作完成且页面
渲染稳定后发送：

```javascript
window.parent.postMessage(
  {
    type: 'BUSINESS_ACTION',
    actionType: 'click',
    selector: '[data-action="submit"]',
    text: '提交审批',
    url: '/contract/filing',
    pageTitle: '合同备案',
    pageSnapshot: {
      version: 1,
      format: 'DOM',
      pageUrl: '/contract/filing/detail/123',
      pageTitle: '合同备案详情',
      capturedAt: new Date().toISOString(),
      html: sanitizedBusinessRootHtml,
      cssText: accessibleCssRules,
      viewport: { width: window.innerWidth, height: window.innerHeight }
    }
  },
  trainingPlatformOrigin
);
```

快照必须移除 `script`、`iframe`、内联事件和密码明文；需要脱敏的普通字段可增加
`data-snapshot-mask="true"`。回放组件会再次清理 HTML，并在无脚本沙箱中禁用交互。
后端以 `PAGE_STATE`、`FULL_PAGE` 保存到 `/capture/resource-snapshots/report` 的
`elementSnapshotJson.pageSnapshot`。内置参考实现位于
`sxpt_web/public/lesson-business-capture.html`。

## 本地 PostgreSQL 兼容配置

后端实体以字符串承载 JSONB 字段，本地 PostgreSQL JDBC 地址需增加：

```properties
spring.datasource.url=jdbc:postgresql://127.0.0.1:55433/sxpt_dev?stringtype=unspecified
```

仓库已经提供 `sxpt_api/src/main/resources/application.yml`，默认连接历史开发环境的
`127.0.0.1:55433/sxpt_dev`，直接执行 `mvn spring-boot:run` 即会激活 `dev` profile。
如本机数据库参数不同，可通过环境变量覆盖，无需修改代码：

```powershell
$env:SXPT_DB_URL = 'jdbc:postgresql://127.0.0.1:5432/sxpt?stringtype=unspecified'
$env:SXPT_DB_USERNAME = 'sxpt'
$env:SXPT_DB_PASSWORD = 'your-password'
mvn spring-boot:run
```

可覆盖的常用变量包括 `SXPT_SERVER_PORT`、`SXPT_DB_URL`、`SXPT_DB_USERNAME`、
`SXPT_DB_PASSWORD`、`SXPT_REDIS_HOST`、`SXPT_REDIS_PORT`、`SXPT_REDIS_PASSWORD`
和 `SXPT_JWT_SECRET`。部署环境必须替换开发用 JWT 密钥。

如果继续使用旧项目保留的本地 PostgreSQL 数据目录，但启动时提示
`Connection to 127.0.0.1:55433 refused`，可先启动数据库：

```powershell
& 'C:\Program Files\PostgreSQL\14\bin\pg_ctl.exe' start `
  -D 'D:\sxptObject\.runtime\postgres-14-data' `
  -l 'D:\sxptObject\.runtime\postgres-14-55433-server.log' `
  -o '-p 55433'
```

确认 55433 端口可用后，再进入 `sxpt_api` 目录执行 `mvn spring-boot:run`。

已有数据库还需应用：

```text
sxpt_api/src/main/resources/db/schema/V4__align_practice_step_result.sql
```

该迁移补齐练习步骤汇总所需字段，并把旧版 `timestamptz` 列转换为与 Java `LocalDateTime` 一致的无时区时间戳。

## CORS

后端默认允许以下开发地址：

```text
http://localhost:5173
http://127.0.0.1:5173
```

其他部署地址通过后端配置覆盖：

```properties
sxpt.cors.allowed-origins=https://training.example.com,https://admin.example.com
```
