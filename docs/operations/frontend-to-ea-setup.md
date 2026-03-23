# 前端配置到 EA 参数填写

## 1. 适用范围

本文档描述当前仓库已经落地的联调方式：

1. 前端负责平台内账户、关系、风控、mapping、share 配置
2. EA 负责连接 websocket 并发送或执行信号
3. 前端不会自动把 EA 参数下发到 MT5

## 2. 先分清 4 个容易混淆的概念

### 2.1 `PAUSED` 不等于解绑

1. 把关系改成 `PAUSED` 并保存，只是暂停复制。
2. 真正解绑关系，要删除对应的 `copy relation`。

### 2.2 删除账户只开放给 follower

1. 当前控制台允许删除 `FOLLOWER` 账户。
2. 当前不允许删除 `MASTER` 或 `BOTH`。

### 2.3 `credential` 不等于 websocket token

1. 前端里的 `credential` 是平台侧保存的账户凭证字段。
2. EA 连接 websocket 使用的是后端配置里的 `BearerToken`。

### 2.4 `FollowerAccountId` 不是 MT5 登录号

1. `FollowerAccountId` 是平台账户 ID。
2. 它可以在“我的 MT5 账户”列表和账户详情页看到。

## 3. 前端必须先配置什么

### 3.1 绑定 MT5 账户

至少创建：

1. 一个 master 账户
2. 一个 follower 账户

关键字段：

1. `brokerName`
2. `serverName`
3. `mt5Login`
4. `accountRole`
5. `status`

要求：

1. `serverName + mt5Login` 必须与 EA 实际登录的 MT5 账户完全一致。
2. 联调时建议账户状态为 `ACTIVE`。

### 3.2 配置主从关系

至少设置：

1. `masterAccountId`
2. `followerAccountId`
3. `copyMode`
4. `status`
5. `priority`

常用起步值：

1. `copyMode = BALANCE_RATIO`
2. `status = ACTIVE`
3. `priority = 100`

### 3.3 配置 follower 风控

至少确认：

1. `balanceRatio`
2. `fixedLot`
3. `maxLot`
4. `followTpSl`
5. `reverseFollow`

### 3.4 必要时配置 symbol mapping

如果 master 和 follower 品种名不同，就必须配置 mapping。

例如：

1. master：`XAUUSD`
2. follower：`XAUUSDm`

## 4. EA 里必须填什么

### 4.1 Master EA

文件：

1. `mt5/Websock_Sender_Master_v0.mq5`

至少填写：

1. `WsUrl`
2. `BearerToken`

本地示例：

```text
WsUrl=ws://127.0.0.1:8080/ws/trade
BearerToken=dev-mt5-token
```

### 4.2 Follower EA

文件：

1. `mt5/Websock_Receiver_Follower_Exec_v0.mq5`

至少填写：

1. `WsUrl`
2. `BearerToken`
3. `FollowerAccountId`
4. `ExecutionMode`

本地示例：

```text
WsUrl=ws://127.0.0.1:8080/ws/follower-exec
BearerToken=dev-follower-token
FollowerAccountId=<前端中的 follower 账户 ID>
ExecutionMode=EXECUTION_REAL
```

首次联调更稳的做法：

```text
ExecutionMode=EXECUTION_DRY_RUN
```

## 5. Token 从哪里来

EA token 当前来自后端配置，而不是前端。

通常对应：

1. Master sender token -> `/ws/trade`
2. Follower exec token -> `/ws/follower-exec`

本地默认值通常是：

```text
Master sender token = dev-mt5-token
Follower exec token = dev-follower-token
```

## 6. 最小联调步骤

1. 启动 MariaDB 和 Redis
2. 启动后端
3. 打开前端控制台
4. 绑定一个 master 账户和一个 follower 账户
5. 记录 follower 的平台账户 ID
6. 创建一条 `ACTIVE` 的主从关系
7. 给 follower 配风控
8. 必要时补 symbol mapping
9. 在 Master EA 填 `WsUrl + BearerToken`
10. 在 Follower EA 填 `WsUrl + BearerToken + FollowerAccountId + ExecutionMode`
11. 让两个 EA 都连上
12. 用一笔全新的开仓 -> 平仓做完整闭环测试

## 7. 排障时先看什么

### 7.1 follower 没有动作

优先检查：

1. master EA 是否连上 `/ws/trade`
2. follower EA 是否连上 `/ws/follower-exec`
3. `FollowerAccountId` 是否填成平台账户 ID
4. 主从关系是否是 `ACTIVE`
5. follower 风控是否拒绝了该指令
6. 是否缺少 symbol mapping

### 7.2 监控台看到在线但数据不完整

优先检查：

1. follower 是否已经发过 `HELLO / HEARTBEAT`
2. monitor 详情里的 `followerExecSessions` 是否在线
3. 当前 follower 的 `lastSignalType` 为空是否其实是设计内正常现象

### 7.3 平仓提示本地仓位不存在

优先检查：

1. 对应开仓是否真的执行成功
2. 是否混入了历史遗留测试仓位
3. 是否在链路修复前留下了漏开的旧单

## 8. 相关文档

1. [总体架构](../architecture/overall-architecture.md)
2. [模块交互与端到端数据链路](../architecture/system-modules-and-dataflows.md)
3. [账户与配置服务](../modules/account-config-service.md)
4. [Follower Exec 服务](../modules/follower-exec-service.md)
