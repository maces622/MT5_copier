# 总体架构

## 1. 设计目标

平台围绕三类核心约束设计：

1. 低延迟：主账号信号传递到跟单执行链路，目标控制在 `100-300ms` 区间内，尽量压低滑点。
2. 高并发：支持单个主账号扇出到大量跟单账户时的瞬时流量洪峰。
3. 高安全：MT5 账户凭证、交易授权和风控规则必须按高敏资产标准管理。

## 2. 当前状态与目标状态

### 当前状态

1. Java 服务端尚未实现具体业务服务。
2. MT5 主账号信号发送端已经存在，使用 WebSocket 发送交易事件。
3. 仓库还没有 MQ、Redis、MySQL、鉴权、监控和执行链路代码。

### 目标状态

系统最终演进为围绕 MQ 解耦、Redis 加速、MySQL 持久化的微服务平台。

## 3. 逻辑分层

```text
[Client]
Web / H5 / Future Agent Console
    |
    | HTTP / REST / WebSocket
    v
[API Gateway]
路由分发 / 鉴权 / 限流 / 防重放
    |
    +-------------------------------+
    |                               |
    v                               v
[User/Auth]                   [WebSocket Notification]
[Account/Config]              [Agent Service]
[Monitor Service]             [Admin/Operator APIs]
    |
    v
[Copy Engine]
    |
    v
[MQ Cluster] <----> [MT5 Bridge Cluster]
    |
    +------> [Redis Cluster]
    |
    +------> [MySQL Cluster]
```

## 4. 核心链路

### 4.1 主账号信号链路

1. MT5 EA 监听 `OnTradeTransaction`。
2. EA 将交易事件组装为标准 JSON，通过 WebSocket 发往服务端。
3. MT5 Bridge 将 WebSocket 消息标准化后写入 `signal` 类 Topic。
4. Copy Engine 消费信号，读取 Redis 路由表，生成跟单执行指令。
5. 执行指令进入 `execution` 类 Topic，再由 MT5 Bridge 或执行网关调用 MT5 下单。
6. 执行结果进入监控与通知链路。

### 4.2 监控链路

1. MT5 Bridge 和 Copy Engine 产生账户净值、持仓、执行结果、风险告警事件。
2. Monitor Service 消费后更新 Redis 快照并异步落库 MySQL。
3. WebSocket Notification 将最新状态推送给前端。

### 4.3 Agent 链路

1. Agent Service 接受自然语言请求。
2. 大模型只负责意图解析，不直接拥有交易执行权限。
3. Agent Service 将结果翻译为受控内部命令，再调用 Account/Config 或发布执行/控制事件。
4. 全链路保留审计记录与审批钩子。

## 5. 服务边界

### API Gateway

统一入口，负责 JWT 校验、签名校验、限流、防重放、灰度路由和 WebSocket 握手前置认证。

### User/Auth Service

负责平台用户、操作员、角色、会话、MFA、Token 生命周期和审计身份。

### Account/Config Service

负责 MT5 账户绑定、主从关系、风控规则、路由缓存写入、配置版本管理和防环校验。

### MT5 Bridge Service

负责 MT5 与平台之间的双向通信：

1. 上行：接收 MT5 WebSocket 信号，转为内部消息。
2. 下行：消费执行指令并调用 MT5 API 实际下单。

### Copy Engine Service

平台核心决策层，负责信号路由、仓位换算、风控拦截、削峰并发执行和幂等控制。

### Monitor Service

负责净值、仓位、订单状态、历史结算、回撤和告警指标的聚合与查询。

### WebSocket Notification Service

负责向前端实时推送跟单结果、持仓变化、告警和系统事件。

### Agent Service

负责未来 AI Agent 接入，提供自然语言到内部受控命令的转换和执行编排。

## 6. 中间件职责

## 6.1 MQ

推荐优先使用 RocketMQ，原因是延迟、顺序消息和消费组语义较适合交易信号场景。Kafka 也可行，但要额外处理顺序和重试语义。

建议 Topic 规划：

1. `signal.deal.v1`
2. `signal.order.v1`
3. `signal.equity.v1`
4. `execution.command.v1`
5. `execution.result.v1`
6. `monitor.alert.v1`
7. `audit.action.v1`

分区建议：按 `masterAccountId` 或 `brokerAccountKey` 做分区键，保证同一主账号事件有序。

## 6.2 Redis

Redis 只服务于高频、低延迟链路：

1. 跟单路由表
2. 风控参数快照
3. 账户净值/保证金快照
4. 持仓快照
5. 幂等去重键
6. WebSocket 订阅会话元数据

建议键示例：

1. `copy:route:master:{masterAccountId}`
2. `account:equity:{accountId}`
3. `account:margin:{accountId}`
4. `position:snapshot:{accountId}`
5. `signal:dedup:{eventId}`

## 6.3 MySQL

MySQL 负责配置和最终账务事实：

1. 用户与角色
2. MT5 账户绑定
3. 跟单关系与风控规则
4. 订单/成交历史
5. 结算流水
6. 审计日志

交易流水表必须预留分库分表策略，至少支持按时间和账户维度归档。

## 7. 关键非功能要求

### 7.1 延迟预算

第一版建议目标：

1. EA 到服务端接入：`< 30ms`
2. 信号标准化入 MQ：`< 10ms`
3. Copy Engine 路由与风控计算：`< 20ms`
4. 执行指令入 MQ：`< 10ms`
5. 执行网关调 MT5：按券商和网络环境评估，目标 `50-200ms`

### 7.2 幂等与顺序

1. 任何外部事件必须带 `event_id`。
2. 同一主账号使用单分区有序消费。
3. 服务端统一做去重缓存和重复执行保护。

### 7.3 可观测性

每条信号需要可追踪：

`event_id -> route_calc_id -> execution_command_id -> execution_result_id`

## 8. 推荐的第一阶段拆分

第一阶段不建议一次性启动全部微服务，建议先用模块化单体或少量服务启动：

1. `signal-ingest`
2. `account-config`
3. `copy-engine`
4. `monitor`

当链路稳定后，再拆独立 Gateway、Notification、Agent 等服务。

## 9. 当前仓库与目标架构的对应关系

目前仓库已落地的唯一业务模块，是 MT5 主账号信号发送端：

`mt5/Websock_Sender_Master_v0.mq5`

该文件已经体现出第一阶段服务端必须优先支持的协议能力：

1. WebSocket 握手
2. HELLO/HEARTBEAT 维护
3. DEAL/ORDER 消息解析
4. 重连与重复消息处理

因此，下一轮实现建议从“服务端信号接入 + MQ 标准化”开始。
