# Follower Exec 服务

## 1. 当前职责

`follower-exec` 负责跟单系统的下行执行链路：

1. 接收 follower EA 的 websocket 连接
2. 绑定 follower 平台账户
3. 回放 backlog dispatch
4. 实时推送新的 dispatch
5. 接收 `ACK / FAIL`
6. 更新 dispatch 状态
7. 更新 follower runtime-state 与 held-position inventory

## 2. 主要输入与输出

### 输入

1. `/ws/follower-exec`
2. `HELLO`
3. `HEARTBEAT`
4. `ACK`
5. `FAIL`
6. Copy Engine 生成的 dispatch

### 输出

1. `HELLO_ACK`
2. `DISPATCH`
3. `STATUS_ACK`
4. dispatch 状态更新
5. follower runtime-state
6. follower session 读模型

## 3. 当前主链路

1. Follower EA 连接 `/ws/follower-exec`
2. 通过 token 校验
3. 发送 `HELLO`
4. 服务端按 `followerAccountId` 或 `server + login` 完成绑定
5. 更新 runtime-state
6. 回放 backlog `PENDING` dispatch
7. 推送新的实时 dispatch
8. EA 回 `ACK / FAIL`
9. 服务端回写状态并返回 `STATUS_ACK`

## 4. 当前支持的 EA 参数

Follower EA 至少需要手工填写：

1. `WsUrl`
2. `BearerToken`
3. `FollowerAccountId`
4. `ExecutionMode`

必须注意：

1. `FollowerAccountId` 是平台账户 ID，不是 MT5 登录号。
2. `BearerToken` 来自后端配置，不来自前端。
3. 首次联调建议先用 `EXECUTION_DRY_RUN`。

## 5. 当前 runtime-state 语义

Follower 的 `HELLO / HEARTBEAT` 会更新：

1. connection status
2. lastHello
3. lastHeartbeat
4. balance
5. equity
6. held-position inventory

因此 follower 详情页里：

1. `balance / equity` 来自 follower 自己的 `HELLO / HEARTBEAT`
2. `lastSignalType` 可能是空，因为 follower 不走 master signal ingest 那套信号链路
3. 更重要的会话字段是 `lastHeartbeat`、`lastDispatchSentAt`、`lastDispatchId`

## 6. 当前实时推送边界

### 单实例

1. 直接使用当前节点持有的 websocket `liveSessions`

### 多实例

1. Redis pub/sub 只负责通知“某 follower 有新 dispatch”
2. 真正持有 websocket 的节点负责推送
3. MySQL 仍是 dispatch 历史真源

## 7. 当前恢复能力

1. backlog replay 可以覆盖 follower 短时离线或服务重启
2. follower 持仓会通过 `HELLO / HEARTBEAT` 上报
3. copied position 的 MT5 comment 会写：
   `cp1|mp=<masterPositionId>|mo=<masterOrderId>`
4. Java 依据该 comment 与持仓快照维护 open-position ledger

## 8. 当前边界

1. `BUY_STOP_LIMIT / SELL_STOP_LIMIT` 还未完成
2. 没有独立 durable ticket mapping store
3. 没有 broker 级 margin 预检查
4. 没有 claim/lease 式多实例补偿 worker

## 9. 相关文档

1. [总体架构](../architecture/overall-architecture.md)
2. [模块交互与端到端数据链路](../architecture/system-modules-and-dataflows.md)
3. [前端配置到 EA 参数填写](../operations/frontend-to-ea-setup.md)
