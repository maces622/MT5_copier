# Implementation Status

## 状态判定

1. `已完成`：代码、配置、测试都已经落地，可以直接联调。
2. `部分完成`：主链路可用，但边界能力、多实例能力或补充运维能力还没补齐。
3. `未完成`：当前仓库没有对应实现，仍然停留在设计目标。

## 已完成

### 1. MT5 Signal Ingest

1. WebSocket endpoint：`/ws/trade`
2. Bearer token / query token 握手校验
3. 连接注册、断开跟踪
4. `HELLO`、`HEARTBEAT`、`DEAL`、`ORDER` 标准化
5. Redis TTL 信号去重，Redis 故障时自动降级到本地内存
6. 主端 EA 已上报余额、净值、合约元信息
7. 主端 WebSocket session registry 已支持 Redis TTL 存储和本地回退

### 2. 账户与跟单配置

1. MT5 账户绑定
2. WebSocket-only 账户可不填 credential
3. 提供 credential 时按落库加密
4. 风控规则持久化
5. 主从关系管理
6. DAG 防环校验
7. 品种映射持久化
8. 本地 bootstrap 命令行初始化
9. 配置变更后自动刷新 Redis 路由、风控和账户绑定缓存

### 3. Copy Engine

1. 消费进程内事件总线中的 `DEAL` / `ORDER`
2. 通过 Redis-first 的 `server + login -> masterAccountId` 绑定缓存定位主账户
3. 通过 Redis-first 的 route/risk 快照加载 follower 路由和风控
4. Redis miss 时回源数据库并自动回填缓存
5. 生成 `execution_commands`
6. 生成 `follower_dispatch_outbox`
7. 支持 `FIXED_LOT`、`BALANCE_RATIO`、`EQUITY_RATIO`、`FOLLOW_MASTER`
8. 默认 relation mode 已改为 `BALANCE_RATIO`
9. 开仓手数支持 `风险比例 * 账户资金缩放比例`
10. 部分平仓按主端平仓比例执行，最后一笔支持 `closeAll=true`
11. 支持 symbol allow/block 和 lot 限制
12. 支持 `reverseFollow`
13. 支持 TP/SL 同步和挂单复制
14. dispatch payload 已包含源端合约元信息
15. 点差限制默认关闭；开启时只限制市价开仓，不限制平仓

### 4. Redis 缓存与运行态

1. MariaDB 仍然是业务真源
2. Redis 缓存主账户 route snapshot
3. Redis 缓存 follower risk snapshot
4. Redis 缓存 `server + login -> account binding`
5. 启动时会预热 route/risk/account-binding 缓存
6. Route/risk/account-binding 读写失败都按降级策略处理，不阻断 DB 真写
7. 主端 WebSocket session registry 已支持 Redis TTL 存储
8. Follower WebSocket session registry 已支持 Redis TTL 存储
9. Follower `followerAccountId -> sessionId` 绑定也已进 Redis
10. Follower realtime dispatch 已通过 Redis pub/sub 跨节点广播

当前默认 key：

1. `copy:route:master:{masterAccountId}`
2. `copy:route:version:{masterAccountId}`
3. `copy:account:risk:{followerAccountId}`
4. `copy:account:binding:{server}:{login}`
5. `copy:signal:dedup:{eventId}`
6. `copy:ws:mt5:session:{sessionId}`
7. `copy:ws:mt5:index`
8. `copy:ws:follower:session:{sessionId}`
9. `copy:ws:follower:index`
10. `copy:ws:follower:account:{followerAccountId}`

当前默认 channel：

1. `copy:follower:dispatch`

### 5. Follower Exec

1. 独立 websocket endpoint：`/ws/follower-exec`
2. 独立 bearer/query-token 握手校验
3. 支持按 `followerAccountId` 或 `server + login` 绑定 follower
4. 支持从 `follower_dispatch_outbox` 回放 backlog
5. 支持新建 `PENDING` dispatch 的实时下发
6. 支持 follower `ACK` / `FAIL` 回写状态
7. Session list API 已可通过 Redis-backed registry 聚合会话视图
8. Follower EA 已支持：
   `OPEN_POSITION`
   `CLOSE_POSITION`
   `SYNC_TP_SL`
   `CREATE_PENDING_ORDER`
   `UPDATE_PENDING_ORDER`
   `CANCEL_PENDING_ORDER`
9. Follower EA 支持通过注释恢复本地映射
10. 市价平仓不做点差限制

### 6. Monitor

1. 已接受的 MT5 信号会持久化审计
2. 运行态按 `server + login` 持久化
3. 运行态已包含 `balance`、`equity`
4. WebSocket 断开时会标记运行态掉线
5. 提供账户监控聚合视图

当前可用 API：

1. `GET /api/monitor/runtime-states`
2. `GET /api/monitor/accounts/overview`
3. `GET /api/monitor/ws-sessions`
4. `GET /api/monitor/accounts/{accountId}/signals`
5. `GET /api/monitor/signals?accountKey=...`
6. `GET /api/follower-exec/sessions`
7. `GET /api/execution-commands?...`
8. `GET /api/execution-commands/dispatches?...`
9. `PATCH /api/execution-commands/dispatches/{dispatchId}`

### 7. 工程维护

1. 核心实体已补 route/runtime/dispatch 相关索引和 `row_version` 乐观锁
2. Redis 配置已统一到 Spring Boot 2.7 的 `spring.redis.*`
3. DTO / entity / config / cache 样板代码已用 Lombok 收缩
4. Route snapshot 的 DB fallback 已改成批量装配，消掉按 follower 的 N+1
5. 当前测试通过：`45/45`

## 部分完成

### 1. 单仓库运行形态

1. 当前仓库已经具备 MT5 上行、主从配置、Copy Engine、Follower 下行和监控
2. 但仍然是单个 Spring Boot 应用，不是最终拆分后的多服务部署

### 2. Redis 使用边界

1. Route/risk/account-binding/dedup/session registry 都已经接入 Redis
2. 会话可见性已经可以跨实例共享
3. Follower dispatch 的跨节点实时推送已经通过 Redis pub/sub 协调
4. 但还没有 durable 的分布式 claim/lease 机制，也没有独立补偿 worker
5. 还没有单独的“重建 Redis 缓存”管理接口；当前重建路径是启动预热加配置重写

### 3. 监控深度

1. 运行态已覆盖连接状态、信号心跳、余额、净值
2. Follower 持仓全量盘点和 broker 级对账还没补齐

### 4. Follower 挂单支持

1. 标准挂单新增、修改、删除已支持
2. `BUY_STOP_LIMIT` / `SELL_STOP_LIMIT` 还没完成

## 未完成

### 1. MQ 与事件骨干

1. 还没有 MQ 集成
2. 还没有对配置变更、执行指令、审计事件做外部 topic 发布

### 2. 独立平台服务

1. API Gateway
2. User Auth Service
3. WebSocket Notification Service
4. Agent Scheduler Service

### 3. 高级交易控制

1. Follower 执行前的高级保证金检查
2. Broker 成交后的滑点二次核对
3. EA 进程外的 durable 本地 ticket 映射
4. 多 broker 执行 worker 路由

## Local Configuration

本地 MariaDB + Redis 联调：

1. 使用 `src/main/resources/application-local.yml`
2. Redis 连接配置使用 `spring.redis.*`
3. Route cache backend 由 `copier.account-config.route-cache.backend` 控制
4. Route cache warmup 由 `copier.account-config.route-cache.warmup-on-startup` 控制
5. Signal dedup backend 由 `copier.mt5.signal-ingest.dedup-backend` 控制
6. Session registry backend 由 `copier.monitor.session-registry.backend` 控制
7. Follower realtime dispatch backend 由 `copier.mt5.follower-exec.realtime-dispatch.backend` 控制
8. 点差限制由 `copier.copy-engine.slippage.enabled` 控制

常用环境变量：

1. `SPRING_PROFILES_ACTIVE=local`
2. `COPIER_ACCOUNT_CONFIG_ROUTE_CACHE_BACKEND=redis`
3. `COPIER_MT5_SIGNAL_INGEST_DEDUP_BACKEND=redis`
4. `COPIER_MONITOR_SESSION_REGISTRY_BACKEND=redis`
5. `COPIER_MT5_FOLLOWER_EXEC_REALTIME_DISPATCH_BACKEND=redis`
6. `COPIER_COPY_ENGINE_SLIPPAGE_ENABLED=false`
