# MT5 Bridge / Signal Ingest

## 1. 当前职责

当前仓库里所谓“MT5 Bridge”，真实形态并不是独立微服务，而是：

1. Master EA 上行接入
2. Spring Boot 内部的 `signal-ingest`
3. Follower EA 下行执行接入

因此这里描述的是“当前已经落地的 MT5 接入层”。

## 2. 当前两条接入链路

### 2.1 Master 上行

1. endpoint：`/ws/trade`
2. 发送：
   `HELLO`
   `HEARTBEAT`
   `DEAL`
   `ORDER`
3. 用于：
   信号标准化
   去重
   signal audit
   runtime-state 更新
   Copy Engine 触发

### 2.2 Follower 下行

1. endpoint：`/ws/follower-exec`
2. 发送：
   `HELLO`
   `HEARTBEAT`
   `ACK`
   `FAIL`
3. 用于：
   follower 绑定
   backlog replay
   realtime dispatch
   dispatch 状态回写
   follower runtime-state 更新

## 3. 当前 token 与参数管理

EA 连接参数当前仍由运维手工填写：

1. Master EA：
   `WsUrl`
   `BearerToken`
2. Follower EA：
   `WsUrl`
   `BearerToken`
   `FollowerAccountId`
   `ExecutionMode`

必须注意：

1. 前端账户页里的 `credential` 不是 websocket token。
2. `FollowerAccountId` 是平台账户 ID。
3. token 来自后端配置，不由前端自动同步给 EA。

## 4. 当前桥接层承载的数据

1. master 余额、净值、合约元信息
2. master held-position inventory
3. follower 余额、净值
4. follower held-position inventory
5. follower copied position tracking comment

## 5. 与其他模块的交互

1. `signal-ingest` 把 master 信号发布给 `copy-engine`
2. `signal-ingest` 和 `follower-exec` 都会更新 `monitor` 使用的 runtime-state
3. `follower-exec` 消费 `copy-engine` 产生的 dispatch

## 6. 当前边界

1. 还不是独立 Bridge 微服务
2. 还没有 MQ 骨干
3. 还没有独立的 dispatcher / reconcile / compensation worker

## 7. 相关文档

1. [总体架构](../architecture/overall-architecture.md)
2. [模块交互与端到端数据链路](../architecture/system-modules-and-dataflows.md)
3. [前端配置到 EA 参数填写](../operations/frontend-to-ea-setup.md)
