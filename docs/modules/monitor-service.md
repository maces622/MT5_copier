# 仓位与资产监控服务

## 1. 职责

Monitor Service 负责把执行结果和账户状态变成可查询、可告警、可推送的监控视图。

核心职责：

1. 聚合账户净值、余额、保证金
2. 聚合持仓和历史订单
3. 计算浮盈、已实现盈亏、回撤
4. 输出前端盯盘视图
5. 触发风险告警

## 2. 输入

建议消费：

1. `signal.equity.v1`
2. `execution.result.v1`
3. `signal.order.v1`
4. `monitor.alert.v1`

## 3. 存储分层

### 3.1 Redis

存放实时快照：

1. `account:equity:{accountId}`
2. `position:snapshot:{accountId}`
3. `risk:drawdown:{accountId}`

### 3.2 MySQL

存放持久化事实：

1. `account_equity_history`
2. `position_history`
3. `order_history`
4. `deal_history`
5. `risk_alert_history`

## 4. 查询接口

建议第一阶段提供：

1. `GET /monitor/accounts/{accountId}/equity`
2. `GET /monitor/accounts/{accountId}/positions`
3. `GET /monitor/accounts/{accountId}/orders`
4. `GET /monitor/accounts/{accountId}/alerts`

## 5. 计算原则

1. 实时页面优先读 Redis 快照。
2. 历史统计优先读 MySQL 聚合。
3. 如果 Redis 快照丢失，可以通过最近事件重建。

## 6. 告警策略

第一阶段建议支持：

1. 回撤超阈值
2. 保证金不足
3. 跟单失败
4. MT5 连接异常

告警事件不直接在查询接口内计算，而是由消费链路异步生成。

## 7. 第一阶段实现重点

1. 做好账户实时快照模型
2. 打通执行结果到监控的入库链路
3. 为前端盯盘页面提供稳定查询接口
