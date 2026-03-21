# 文档索引

## 说明

这套文档现在按三类状态管理：

1. `已完成实现对齐`：文档内容已经和当前代码基本一致，可直接用于联调和排障。
2. `部分完成，含目标设计`：文档既描述了当前实现，也保留了后续设计目标，阅读时需要结合状态总表。
3. `仅目标设计 / 未实现`：文档描述的是目标架构或未来模块，当前仓库还没有对应实现。

当前代码状态以 [implementation-status.md](./implementation-status.md) 为准。

## 已完成实现对齐

1. [实现状态总表](./implementation-status.md)
2. [账户与配置服务](./modules/account-config-service.md)
3. [跟单引擎](./modules/copy-engine-service.md)
4. [Follower Exec 服务](./modules/follower-exec-service.md)
5. [监控服务](./modules/monitor-service.md)

## 部分完成，含目标设计

1. [总体架构](./architecture/overall-architecture.md)
2. [安全与可靠性](./architecture/security-and-reliability.md)
3. [MT5 WebSocket 协议](./contracts/mt5-websocket-signal.md)
4. [MT5 Bridge 服务](./modules/mt5-bridge-service.md)
5. [迭代工作流](./process/iteration-cycle.md)

这些文档里包含一部分已经落地的内容，也包含后续迭代目标：

1. MT5 上行信号接入、主从配置、复制指令生成、Follower 下行执行、监控聚合已经有代码。
2. MQ、独立 Agent、网关拆分、完整多服务部署仍然是目标设计，不是当前仓库现状。

## 仅目标设计 / 未实现

1. [API Gateway](./modules/api-gateway.md)
2. [用户认证服务](./modules/user-auth-service.md)
3. [实时通知服务](./modules/websocket-notification-service.md)
4. [Agent 调度服务](./modules/agent-service.md)

这些模块当前仓库没有对应运行时代码，保留文档是为了后续拆分服务时复用设计。

## 当前代码重点

当前仓库已经可以完成的主链路：

1. MT5 主端 EA 通过 `/ws/trade` 上报 `HELLO / HEARTBEAT / DEAL / ORDER`
2. Java 侧完成主从账户绑定、风控、品种映射、关系配置的持久化
3. Copy Engine 生成 `execution_commands` 和 `follower_dispatch_outbox`
4. Follower EA 通过 `/ws/follower-exec` 接收下行指令并执行开仓、平仓、TP/SL 同步、挂单操作
5. 监控接口可查看账户状态、运行态、信号审计、WebSocket 会话
6. Redis 已用于主从路由和 follower 风控的读缓存、回填、启动预热

## 开发说明

项目已经引入 Lombok 来收缩 DTO、实体、配置属性和缓存快照样板代码。

本地开发建议：

1. 使用 Maven 或 IDEA 导入项目后启用 annotation processing
2. 以 `local` profile 启动时使用 MariaDB 作为真源、Redis 作为缓存层
3. 任何功能是否已经落地，先看 [implementation-status.md](./implementation-status.md)
