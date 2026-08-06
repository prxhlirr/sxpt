# OA 正式造数契约映射设计

## 目标

修复 `POST /api/v1/teaching-data/authoring-launches/create` 在创建备案业务数据实例时返回“状态不允许操作”的问题，并保证相同映射能够用于后续学生练习、重新练习的数据重新生成。

实训平台继续保留内部模块编码、模板编码和场景模板的唯一性；调用 OA 时，由 HTTP 原平台适配器将内部请求转换为 OA 固定的开放接口契约。

## 已确认的根因

- `DATA_CREATE` 能力地址错误指向 `/api/ext/data-center/incoming/mock`，该接口只接受简化的 `participants` 请求，不是正式教学造数接口。
- OA 正式接口为 `/openapi/teaching-data/batch-create`，使用 Bearer Token、`X-Trace-Id` 和 `X-Idempotency-Key`。
- OA 当前只接受固定外部值：
  - `businessModuleCode = doc_incoming`
  - `templateCode = incoming_pending_reg_v1`
  - `initState = PENDING_REG`
- 练习请求可能同时带有 `externalOrgId` 与 `poolKey`，而 OA 要求两者互斥。
- 重练快照中的审计字段不是 OA 收文模板的业务参数，不能原样放入 `bizParams` 转发。

## 方案选择

采用能力级外部契约映射：把 OA 固定字段和参与方选择策略配置在 `platform_capability.request_schema_json`，由 `HttpOriginDataPrepareAdapter` 读取并转换。

不采用以下方案：

- 不直接把内部模块和不同场景模板都改成 OA 编码，因为实训库要求模板编码唯一，备案与练习无法同时使用同一 OA 模板编码。
- 不修改 OA 去接受实训平台随机生成的内部编码，避免两个系统的内部主数据相互耦合。

## 配置设计

`DATA_CREATE.request_schema_json` 增加以下配置：

```json
{
  "contract": "TEACHING_DATA_BATCH_CREATE_V1",
  "businessModuleCode": "doc_incoming",
  "templateCode": "incoming_pending_reg_v1",
  "initState": "PENDING_REG",
  "defaultPoolKey": "incoming-default",
  "preferPoolKey": true,
  "forwardBizParams": false
}
```

同时更新：

- `DATA_CREATE.endpoint_url` 为 `http://localhost:9527/openapi/teaching-data/batch-create`。
- OA 连接器认证方式为 `BEARER`，认证配置保存为 `{"token":"oa-data-center-incoming-token","adapter":"http"}`。
- 未实现正式协议的 `RESULT_CHECK` 与 `DATA_ARCHIVE` 不参与本次调用；创建结果由当前实训平台逻辑直接标记为校验通过。

## 请求转换

适配器按以下优先级生成 OA 请求：

1. 模块编码、模板编码、初始状态优先取 `DATA_CREATE.request_schema_json` 中的外部固定值；缺省时保持现有内部请求值，兼容其他第三方平台。
2. 备案请求没有题目数据池键时，继续发送流程配置给出的 `externalOrgId`，确保数据绑定指定组织。
3. 练习或重练请求存在题目键且 `preferPoolKey=true` 时，只发送 `poolKey`，其值使用配置的 `defaultPoolKey`；不再同时发送 `externalOrgId`。
4. `forwardBizParams=false` 时不向 OA 转发实训平台内部的重练审计字段；缺省时保持现有转发行为，兼容需要业务参数的其他平台。
5. `participantId`、`ownerUserId`、幂等键和追踪 ID 保持现有逻辑，用于逐项对账和重复请求保护。

## 响应与错误处理

- OA 成功响应继续映射为教学数据实例，保存 `externalDataId`、业务编号、状态、入口地址和组织信息。
- OA 返回 `failedItems` 时，按 `participantId` 回填具体失败原因。
- HTTP 非 2xx 或响应格式不符合契约时，数据准备任务保持失败，并保留可诊断的原平台错误信息。
- `AuthoringLaunchServiceImpl` 不再只对外返回笼统的“状态不允许操作”；若数据准备失败，应优先透传任务中的具体错误原因，便于继续排查配置或 OA 数据问题。

## 测试与验收

后端契约测试覆盖：

- 能力配置覆盖内部模块、模板和初始状态。
- 备案参与方使用 `externalOrgId`，不产生 `poolKey`。
- 练习参与方使用默认 `poolKey`，不同时发送 `externalOrgId`。
- 禁止转发内部重练审计 `bizParams`。
- 保留 Bearer、追踪 ID、幂等键与成功响应映射。
- 数据准备失败时，authoring-launch 返回具体错误而不是笼统状态错误。

集成验收顺序：

1. 更新数据库能力配置。
2. 重启实训平台后端使修改后的适配器生效。
3. 调用 authoring-launch，确认 OA 创建一条 `PENDING_REG` 收文数据并返回可用的 SSO 启动地址。
4. 使用同一模块执行学生练习和重新练习造数，确认每次生成新的 OA 业务数据实例，且不出现主体映射冲突。

