# MT5 跟单平台文档索引

## 1. 文档目标

本目录用于沉淀 `copier_v0` 的系统设计，作为后续开发的统一基线。当前仓库仍处于早期阶段，文档先解决三件事：

1. 明确当前代码已经具备的能力，避免设计脱离实现。
2. 明确目标微服务架构、模块边界、消息流和安全约束。
3. 约定后续按“设计 -> 实现 -> 验证 -> 调整设计”的循环推进。

## 2. 当前仓库现状

当前代码状态不是完整微服务平台，而是“已完成第一段接入骨架 + 若干待建设模块”：

1. Java 侧已落地第一版 MT5 WebSocket 信号接入模块，提供 `/ws/trade`、Bearer Token 握手校验、消息标准化、会话跟踪和基础去重。
2. Java 侧已落地 `Account/Config` 第一版，包含 MT5 账户绑定、凭证加密、风控规则保存、主从关系管理、防环校验和 Redis 路由缓存抽象。
3. Java 侧已落地 `Copy Engine` 最小闭环，能够消费已接收的 `DEAL` 信号，读取主从关系并生成执行命令或拒绝记录。
4. Java 侧当前仍未接入 MQ，也尚未落地真实 MT5 下行执行、监控聚合和真实交易回执处理。
5. `mt5/Websock_Sender_Master_v0.mq5` 已经能够在 MT5 主账号侧捕获成交/订单事件，并通过 WebSocket 向服务端推送 `HELLO`、`HEARTBEAT`、`DEAL`、`ORDER` 四类消息。
6. MT5 EA 侧已实现重连、心跳、断线缓存、延迟重试历史查询等基础可靠性逻辑。
7. `wsclient.mqh` 头文件当前未纳入仓库，需要在后续实现阶段补齐依赖来源和版本管理方式。

这意味着：当前最真实的系统边界，已经演进为“MT5 发送端 + 服务端信号接入层 + 账户配置层 + 执行命令生成层已具备，真实下单链路仍待建设”。

## 3. 阅读顺序

1. [总体架构](./architecture/overall-architecture.md)
2. [安全与可靠性](./architecture/security-and-reliability.md)
3. [MT5 WebSocket 信号协议](./contracts/mt5-websocket-signal.md)
4. 模块设计
   - [API Gateway](./modules/api-gateway.md)
   - [用户与认证服务](./modules/user-auth-service.md)
   - [账户与配置服务](./modules/account-config-service.md)
   - [MT5 Bridge 服务](./modules/mt5-bridge-service.md)
   - [实时跟单引擎](./modules/copy-engine-service.md)
   - [仓位与资产监控服务](./modules/monitor-service.md)
   - [实时推送服务](./modules/websocket-notification-service.md)
   - [Agent 调度服务](./modules/agent-service.md)
5. [迭代工作流](./process/iteration-cycle.md)

## 4. 推荐的第一阶段实施顺序

按照当前仓库状态，建议优先级如下：

1. 先把当前 `/ws/trade` 接入层升级为标准化入 MQ。
2. 再做账户与配置服务，先把主从关系和风控规则落地。
3. 随后实现最小可用的跟单引擎，只支持主账号成交事件驱动的基础跟单。
4. 最后补齐监控、通知、Agent 和更复杂的风险控制。

## 5. 后续协作方式

后续每一轮工作都遵循统一循环：

1. 先更新设计文档，明确范围、接口、风险和验收标准。
2. 再落实现，代码只覆盖当前设计范围。
3. 然后做验证，至少包含联调路径、边界条件和失败场景。
4. 最后把验证暴露出来的偏差回写到文档，形成下一轮输入。

详细规则见 [迭代工作流](./process/iteration-cycle.md)。
