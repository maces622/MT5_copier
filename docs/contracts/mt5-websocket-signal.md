# MT5 WebSocket 信号协议

## 1. 文档范围

本文档描述当前仓库中 MT5 主账号发送端已经实现的 WebSocket 协议行为，代码来源：

[Websock_Sender_Master_v0.mq5](../../mt5/Websock_Sender_Master_v0.mq5)

这是服务端接入模块的直接协议依据，不是理想化草案。

## 2. 当前客户端行为

### 2.1 连接参数

EA 当前支持以下输入参数：

1. `WsUrl`
2. `UseCompression`
3. `SubProtocol`
4. `BearerToken`
5. `TimeoutMs`
6. `TimerPeriodMs`
7. `ReconnectIntervalMs`
8. `HeartbeatIntervalMs`
9. `MaxOutbox`
10. `MaxDealRetries`
11. `MaxOrderRetries`
12. `SendBatchPerTick`

### 2.2 生命周期

1. `OnInit` 时尝试建立 WebSocket 连接。
2. 建连成功后立即发送 `HELLO`。
3. `OnTimer` 周期内执行消息泵、心跳、待发送队列处理和重连。
4. `OnTradeTransaction` 只入队，不直接做网络 IO。

### 2.3 当前可靠性语义

1. 网络断开时消息先进入本地 outbox。
2. DEAL 和 ORDER 事件允许短时间重试，等待 MT5 历史记录可查询。
3. 重连成功后会继续发送 outbox 中的消息。
4. 因此服务端必须按“至少一次”消费，不能按“恰好一次”假设实现。

## 3. 消息类型

## 3.1 HELLO

用途：连接建立后上报账号身份。

示例：

```json
{
  "type": "HELLO",
  "login": 123456,
  "server": "Broker-Demo",
  "ts": "2026.03.20 14:35:10"
}
```

字段：

1. `type`: 固定为 `HELLO`
2. `login`: MT5 账号
3. `server`: MT5 服务器名
4. `ts`: 客户端时间字符串

## 3.2 HEARTBEAT

用途：维持连接存活。

示例：

```json
{
  "type": "HEARTBEAT",
  "ts": "2026.03.20 14:35:13"
}
```

## 3.3 DEAL

用途：成交事件，是跟单主链路最关键的输入。

示例：

```json
{
  "type": "DEAL",
  "event_id": "123456-DEAL-987654321",
  "login": 123456,
  "server": "Broker-Demo",
  "deal": 987654321,
  "order": 456789,
  "position": 456789,
  "symbol": "XAUUSD",
  "action": "BUY OPEN",
  "volume": 1.0000,
  "price": 3025.1200000000,
  "deal_type": 0,
  "entry": 0,
  "magic": 0,
  "comment": "",
  "time": "2026.03.20 14:35:14"
}
```

关键字段解释：

1. `event_id`: 当前可作为服务端幂等键。
2. `action`: 由 `entry + deal_type` 推导，当前值可能为 `BUY OPEN`、`SELL OPEN`、`BUY CLOSE`、`SELL CLOSE`、`CLOSE & REVERSE`、`CLOSE BY OPPOSITE`。
3. `deal` / `order` / `position`: MT5 原生标识，可用于联动定位历史记录。

## 3.4 ORDER

用途：订单新增、修改、删除事件，主要用于补充挂单、止损止盈和状态同步。

示例：

```json
{
  "type": "ORDER",
  "event": "ORDER_UPDATE",
  "scope": "ACTIVE",
  "event_id": "123456-ORDER_UPDATE-456789",
  "login": 123456,
  "server": "Broker-Demo",
  "order": 456789,
  "symbol": "XAUUSD",
  "order_type": 0,
  "order_state": 1,
  "vol_init": 1.0000,
  "vol_cur": 1.0000,
  "price_open": 3025.1200000000,
  "sl": 3015.0000000000,
  "tp": 3040.0000000000,
  "magic": 0,
  "comment": "",
  "time_setup": "2026.03.20 14:35:14",
  "time_done": "1970.01.01 00:00:00"
}
```

字段说明：

1. `event`: `ORDER_ADD`、`ORDER_UPDATE`、`ORDER_DELETE`
2. `scope`: `ACTIVE` 或 `HISTORY`
3. `event_id`: 当前可作为订单事件幂等键

## 4. 服务端接入要求

### 4.1 连接层

服务端至少要提供：

1. WebSocket endpoint，例如 `/ws/trade`
2. Token 或签名校验
3. 连接级 trace id
4. 连接和账号映射表

### 4.2 消息层

服务端收到消息后建议统一转换为内部模型：

```json
{
  "ingest_id": "uuid",
  "source": "mt5-master-ws",
  "received_at": "2026-03-20T06:35:14Z",
  "payload": {}
}
```

然后根据 `type` 分发到不同 Topic。

### 4.3 幂等层

至少做两层幂等：

1. 连接接入层基于 `event_id` 做短期去重
2. 跟单引擎层再做一次执行前去重

## 5. 顺序与乱序假设

当前 EA 在单终端内按 timer 发送队列顺序出站，但服务端不能过度依赖天然顺序。原因：

1. 重连后会有积压重发
2. DEAL 和 ORDER 的重试次数不同
3. 网络层可能造成接收时间与交易发生时间偏移

因此建议：

1. 同一主账号事件进入同一 MQ 分区
2. 服务端以 `event time + account key + event_id` 做排序和去重辅助

## 6. 未来扩展

当前协议只覆盖“主账号上行信号发送”。后续可以扩展：

1. `EQUITY` 账户净值快照
2. `POSITION_SNAPSHOT` 持仓快照
3. `ACK` 服务端确认消息
4. 服务端到 MT5 的执行命令下行协议

第一阶段实现时，建议不要先发散扩展，先把现有四类消息完整接住并稳定入 MQ。
