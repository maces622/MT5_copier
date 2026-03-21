# 文档索引

## 说明

这套文档现在按三类状态管理：

1. `已完成实现对齐`：文档内容已经和当前代码对齐，可直接用于联调、排障和本地初始化。
2. `部分完成，含目标设计`：文档同时包含当前实现和后续目标，阅读时要结合状态总表判断哪些已经落地。
3. `仅目标设计 / 未实现`：当前仓库没有对应运行时代码，文档保留给后续拆分或扩展时复用。

当前代码状态以 [implementation-status.md](./implementation-status.md) 为准。

## 已完成实现对齐

1. [实现状态总表](./implementation-status.md)
2. [账户与配置服务](./modules/account-config-service.md)
3. [跟单引擎](./modules/copy-engine-service.md)
4. [Follower Exec 服务](./modules/follower-exec-service.md)
5. [监控服务](./modules/monitor-service.md)

这些内容已经覆盖当前仓库真实可用的主链路：

1. MT5 主端 EA 通过 `/ws/trade` 上报 `HELLO / HEARTBEAT / DEAL / ORDER`
2. Java 侧完成主从账户绑定、风控、品种映射、主从关系持久化
3. Copy Engine 生成 `execution_commands` 和 `follower_dispatch_outbox`
4. Follower EA 通过 `/ws/follower-exec` 接收并执行开仓、平仓、TP/SL、挂单指令
5. Redis 当前已经用于 route/risk/account-binding 缓存、信号去重、WebSocket session registry 和 follower realtime dispatch 协调
6. 监控接口可以查看账户状态、运行态、信号审计和 WebSocket 会话

## 部分完成，含目标设计

1. [总体架构](./architecture/overall-architecture.md)
2. [安全与可靠性](./architecture/security-and-reliability.md)
3. [MT5 WebSocket 协议](./contracts/mt5-websocket-signal.md)
4. [MT5 Bridge 服务](./modules/mt5-bridge-service.md)
5. [迭代工作流](./process/iteration-cycle.md)

这些文档里既有已经落地的内容，也有后续目标：

1. MT5 上行接入、主从配置、Copy Engine、Follower 下行、监控聚合已经在当前仓库落地
2. MQ、独立 Agent、网关拆分和完整多服务部署仍然属于目标设计
3. Redis 已进入热路径，跨节点 follower 实时推送也已经通过 pub/sub 接通，但更重的 worker/claim 机制还没做

## 仅目标设计 / 未实现

1. [API Gateway](./modules/api-gateway.md)
2. [用户认证服务](./modules/user-auth-service.md)
3. [实时通知服务](./modules/websocket-notification-service.md)
4. [Agent 调度服务](./modules/agent-service.md)

这些模块当前没有对应运行时代码，文档保留用于后续演进。

## 当前代码重点

当前仓库已经可用于本地 MariaDB + Redis 联调，建议先看：

1. [实现状态总表](./implementation-status.md)
2. `src/main/resources/application-local.yml`
3. `bootstrap/local.example.json`

本地开发注意：

1. 项目已引入 Lombok，IDEA 需要开启 annotation processing
2. `local` profile 下使用 MariaDB 作为真源，Redis 作为缓存和运行态共享层
3. 是否已完成某项能力，先看 [implementation-status.md](./implementation-status.md)
