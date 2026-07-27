# 教学蒙版与多段备案 Demo

Vue 3 + Vite 前端 demo，用透明教学工具栏覆盖可嵌入的业务系统页面，实现多角色分段备案、跨页录制、说明节点、学习演示、练习和考试。

## 启动

```powershell
npm install
npm run dev
```

根路由 `/` 是教师备案工作台，`/learn` 是学习运行页。

教师工作台每次从一个空分段开始，不预置任何录制节点。`/learn` 只读取成功发布的教案；没有已发布节点时不会加载业务 iframe，也不能进入学习、练习或考试模式。

## 接口配置

编辑 `public/training-config.js`：

- `apiMode: 'mock'` 使用 localStorage 模拟接口 8–14。
- `apiMode: 'remote'` 使用真实 `fetch`。
- `apiBaseUrl` 填写包含 `/api/v1` 的接口基础地址。
- `bearerToken` 填写临时联调 token。
- `tenantId`、`connectorSystemId`、`teacherId` 填写联调上下文。

真实接口错误不会自动切换为 mock。接口日志浮层只显示路径、会话、状态和耗时，不显示 token 或请求体。

## 备案流程

1. 点击“开始备案”创建 `captureSessionId`。
2. 在业务页面操作，SDK 捕获并上报事件和关键元素摘要。
3. 点击“保存本段”创建本段动作草稿。
4. 点击“开始下一段”保存当前段并确认角色切换。
5. 点击“发布教案”确认草稿、沉淀正式资源、发布教学点并结束采集会话。

录制节点保存稳定选择器候选和录制视口信息。学习或练习时，SDK 会在当前业务页面重新查找元素、滚动到可见区域并返回实时坐标，因此提示高亮不依赖录制电脑的分辨率。录制坐标只作为快照，不用于自动点击。

当前后端尚无教学步骤发布接口，因此完整分段播放数据同时保存到版本化 localStorage；接口 14 的 `recordPathJson` 保存备案路径摘要。Mock 模式只能在同一浏览器中演示发布与学习，真实跨电脑使用需要切换远程 API 模式。

## 验证

```powershell
npm test
npm run build
```
