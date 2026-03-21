# MT5 Bridge 服务

## 1. 职责

MT5 Bridge 是平台和 MT5 之间的唯一交易接触层，职责分成上行和下行两部分：

1. 上行：接入主账号信号、账户状态、成交回执
2. 下行：消费执行指令并调用 MT5 API 下单、平仓、改单

## 2. 当前已存在能力

当前仓库已有两部分上行链路基础能力：

1. MT5 主账号发送端：

[Websock_Sender_Master_v0.mq5](D:\repo\Copy_trader_MT5\copier_v0\mt5\Websock_Sender_Master_v0.mq5)

2. Java 服务端信号接入层：

   - `/ws/trade`
   - Bearer Token 握手鉴权
   - `HELLO / HEARTBEAT / DEAL / ORDER` 标准化接收
   - 会话跟踪
   - 基础去重

当前 EA 的特点：

1. 由 MT5 EA 直接通过 WebSocket 推送
2. 当前覆盖 `HELLO`、`HEARTBEAT`、`DEAL`、`ORDER`
3. 已实现本地 outbox、重连、历史查询重试

这决定了 Bridge 下一步重点不再是“能不能接住消息”，而是“如何稳定写入 MQ，并为执行链路提供标准事件”。

## 3. 目标内部拆分

建议 Bridge 内部再拆为三个子模块：

### 3.1 Signal Ingest

1. 接收 MT5 WebSocket 连接
2. 校验身份
3. 解析 JSON
4. 转成内部标准消息
5. 发布到 `signal.*` Topic

### 3.2 Execution Dispatcher

1. 消费 `execution.command.v1`
2. 根据目标券商、服务器、账户选择执行 worker
3. 调用 MT5 API 执行
4. 发布 `execution.result.v1`

### 3.3 State Sync

1. 定时对齐账户状态
2. 处理掉线恢复后的订单补偿
3. 定时上报净值和保证金快照

## 4. 上行信号标准化

建议统一消息外壳：

```json
{
  "eventId": "123456-DEAL-987654321",
  "sourceType": "mt5-master-ws",
  "masterAccountKey": "Broker-Demo:123456",
  "payloadType": "DEAL",
  "eventTime": "2026-03-20T06:35:14Z",
  "payload": {}
}
```

标准化时补齐：

1. `masterAccountKey`
2. `receivedAt`
3. `ingestNodeId`
4. `traceId`

## 5. 下行执行模型

执行命令建议至少包含：

1. `commandId`
2. `followerAccountId`
3. `masterEventId`
4. `action`
5. `symbol`
6. `volume`
7. `pricePolicy`
8. `sl`
9. `tp`
10. `riskContext`

## 6. 可靠性要求

1. 同一 MT5 账户的信号在 Bridge 层保持顺序。
2. 相同 `eventId` 重复到达不重复发布。
3. 执行命令失败要能区分可重试和不可重试。
4. 连接断开后需要支持自动恢复和状态对账。

## 7. 部署建议

1. Ingest 和 Dispatcher 可分开部署。
2. 账户或券商维度做分片，避免热点实例。
3. 每个 Worker 只维护有限数量的活跃 MT5 会话。

## 8. 第一阶段实现重点

1. 保持 `/ws/trade` 接口稳定
2. 将现有四类消息从内存发布升级为 MQ 发布
3. 把重复消息、非法消息、无权限连接挡在最外层
4. 为后续执行 dispatcher 预留统一命令模型
