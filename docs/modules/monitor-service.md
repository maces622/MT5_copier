# Monitor Service

## 1. 当前职责

`monitor` 负责把系统中的信号、运行状态、会话和执行结果聚合成可查询视图。

当前覆盖：

1. signal audit
2. runtime-state
3. master websocket session
4. follower websocket session
5. account overview
6. command / dispatch 查询
7. order / position trace

## 2. 主要输入与输出

### 输入

1. `signal-ingest` 写入的 signal audit
2. master runtime-state
3. follower runtime-state
4. master session registry
5. follower session registry
6. `execution_commands`
7. `follower_dispatch_outbox`

### 输出

1. `/api/monitor/dashboard`
2. `/api/monitor/accounts/{accountId}/detail`
3. `/api/monitor/accounts/{accountId}/commands`
4. `/api/monitor/accounts/{accountId}/dispatches`
5. `/api/monitor/traces/order`
6. `/api/monitor/traces/position`

## 3. 当前 runtime-state 设计

### 数据主权

1. Redis 持有最新 runtime-state
2. MySQL 保存节流同步后的快照
3. 断线事件会强制落盘

### 当前 key

1. `copy:runtime:state:{server}:{login}`
2. `copy:runtime:account:{accountId}`
3. `copy:runtime:index`
4. `copy:runtime:db-sync:{server}:{login}`

### 当前字段

runtime-state 当前至少包含：

1. connectionStatus
2. lastHelloAt
3. lastHeartbeatAt
4. lastSignalAt
5. lastSignalType
6. lastEventId
7. balance
8. equity

## 4. 账户详情页的数据来源

### `overview`

来自：

1. MT5 账户基础信息
2. runtime-state
3. dispatch 计数

### `runtimeState`

来自统一 runtime-state store。

注意：

1. follower 账户现在也会返回 `balance` 和 `equity`
2. follower 的 `lastSignalType` 可能为 `n/a`，这是当前设计内的正常结果

### `wsSessions`

只表示 master `/ws/trade` 会话。

### `followerExecSessions`

表示 follower `/ws/follower-exec` 会话。

当前更重要的字段是：

1. `lastHeartbeatAt`
2. `lastDispatchSentAt`
3. `lastDispatchId`

## 5. 当前监控控制台行为

1. 监控列表页和详情页当前会定时轮询刷新
2. 前端看到的是聚合 API 的读模型，不是额外的 websocket 推送
3. command、dispatch、runtime-state、session 会随着轮询刷新

## 6. 持仓台账

除了 runtime-state 之外，监控层还维护 open-position ledger：

1. Redis key：
   `copy:runtime:positions:{accountKey}`
2. Redis index：
   `copy:runtime:positions:index`
3. MySQL durable ledger：
   open-position 持久化表

held-position inventory 会来自 master 与 follower 的 `HELLO / HEARTBEAT`。

## 7. 与其他模块的交互

1. `signal-ingest`
   提供 signal audit 与 master runtime-state
2. `follower-exec`
   提供 follower runtime-state 与 follower session
3. `copy-engine`
   提供 command / dispatch / trace 查询数据
4. `web-console`
   消费监控聚合接口

## 8. 当前边界

1. runtime-state 的数据库同步仍然是节流式 inline persistence，不是独立 flush worker
2. 没有完整告警编排和通知中心
3. 还没有完整 broker 级对账系统

## 9. 相关文档

1. [总体架构](../architecture/overall-architecture.md)
2. [模块交互与端到端数据链路](../architecture/system-modules-and-dataflows.md)
3. [Redis 备份与恢复](../operations/redis-backup-recovery.md)
