# Implementation Status

## 判定规则

1. `已完成`：代码、配置、测试已经落地，可以直接联调。
2. `部分完成`：主链路可用，但仍有边界能力、持久化或多实例能力未补齐。
3. `未完成`：只有设计，没有当前仓库实现。

## 已完成

### 1. MT5 Signal Ingest

1. WebSocket endpoint: `/ws/trade`
2. Bearer token / query token handshake validation
3. Session registration and disconnect tracking
4. Normalization for `HELLO`, `HEARTBEAT`, `DEAL`, and `ORDER`
5. In-process dedup for repeated trade events
6. Master EA now reports account balance/equity and instrument metadata

### 2. Account and Copy Configuration

1. MT5 account binding
2. Optional credential field for websocket-only accounts
3. Credential encryption at rest when credential is provided
4. Risk rule persistence
5. Copy relation management
6. DAG anti-cycle validation
7. Symbol mapping persistence
8. Command-line bootstrap for local initialization
9. Redis route/follower-risk cache refresh after config writes

### 3. Copy Engine

1. Consume accepted MT5 `DEAL` and `ORDER` events from the in-process event bus
2. Resolve master account by `server + login`
3. Load active follower routes and risk snapshots
4. Generate `execution_commands`
5. Generate `follower_dispatch_outbox`
6. Support `FIXED_LOT`, `BALANCE_RATIO`, `EQUITY_RATIO`, `FOLLOW_MASTER`
7. Default relation mode is now `BALANCE_RATIO`
8. Open sizing supports `risk ratio * account funds scale`
9. Partial close uses master close ratio; final close can force `closeAll=true`
10. Apply symbol allow/block checks and lot-size limits
11. Apply `reverseFollow` when converting follower action
12. Support TP/SL sync and pending-order replication from `ORDER` events
13. Dispatch payload includes source instrument metadata
14. Slippage/spread blocking is disabled by default and only applies to market opens when enabled

### 4. Redis Route Cache

1. MariaDB is the source of truth
2. Redis stores master route snapshots and follower risk snapshots
3. Copy engine now reads Redis first, falls back to DB on cache miss, then backfills Redis
4. Route cache keys are prewarmed on startup when Redis backend is enabled
5. Redis write/read failures degrade gracefully and do not block DB writes

Current keys:

1. `copy:route:master:{masterAccountId}`
2. `copy:route:version:{masterAccountId}`
3. `copy:account:risk:{followerAccountId}`

### 5. Follower Exec

1. Dedicated websocket endpoint: `/ws/follower-exec`
2. Dedicated bearer/query-token handshake validation
3. Follower session binding by `followerAccountId` or `server + login`
4. Backlog replay from `follower_dispatch_outbox`
5. Live push of newly created `PENDING` dispatches
6. Follower-side `ACK` and `FAIL` callbacks with dispatch status write-back
7. Session list API for current follower-exec connections
8. MT5 follower EA execution path for:
   - `OPEN_POSITION`
   - `CLOSE_POSITION`
   - `SYNC_TP_SL`
   - `CREATE_PENDING_ORDER`
   - `UPDATE_PENDING_ORDER`
   - `CANCEL_PENDING_ORDER`
9. Follower EA local mapping recovery from MT5 comments on startup
10. Market close does not apply slippage blocking

### 6. Monitor

1. Persist accepted MT5 signals for audit and query
2. Persist runtime state per `server + login`
3. Runtime state now includes `balance` and `equity`
4. Mark runtime sessions disconnected when websocket closes
5. Expose merged account monitoring overview

APIs already available:

1. `GET /api/monitor/runtime-states`
2. `GET /api/monitor/accounts/overview`
3. `GET /api/monitor/ws-sessions`
4. `GET /api/monitor/accounts/{accountId}/signals`
5. `GET /api/monitor/signals?accountKey=...`
6. `GET /api/follower-exec/sessions`
7. `GET /api/execution-commands?...`
8. `GET /api/execution-commands/dispatches?...`
9. `PATCH /api/execution-commands/dispatches/{dispatchId}`

### 7. Engineering Maintenance

1. JPA entities now carry route/runtime/dispatch related indexes and optimistic-lock `row_version`
2. Redis configuration has been aligned to Spring Boot 2.7 `spring.redis.*`
3. DTO/entity/config/cache boilerplate has been reduced with Lombok
4. Current test suite passes: `28/28`

## 部分完成

### 1. MT5 Bridge / Single-Repo Runtime

1. Current codebase already contains MT5 signal ingest and follower downlink execution
2. But the code is still a single Spring Boot app, not the final split multi-service deployment

### 2. Redis Usage

1. Route cache and follower risk cache are in Redis
2. Signal dedup is still JVM-local, not Redis-backed
3. WebSocket session registry is still in-memory, not Redis-backed
4. There is no standalone cache rebuild endpoint yet; current rebuild path is startup warmup + config rewrite

### 3. Monitoring Depth

1. Runtime state already tracks connection, signal heartbeat, balance, equity
2. Full follower-side position inventory and broker reconciliation are not finished

### 4. Follower Pending Orders

1. Standard pending order create/update/delete is supported
2. `BUY_STOP_LIMIT` / `SELL_STOP_LIMIT` follower execution is not finished

## 未完成

### 1. MQ and Event Backbone

1. No MQ integration yet
2. No external topic publishing for config changes, execution commands, or audit events

### 2. Independent Platform Services

1. API Gateway
2. User Auth Service
3. WebSocket Notification Service
4. Agent Scheduler Service

### 3. Advanced Trading Controls

1. Advanced margin checks before follower execution
2. Broker-side post-fill slippage reconciliation
3. Durable follower local-ticket mapping outside the EA process
4. Multi-broker execution worker routing

## Local Configuration

For local MariaDB + Redis:

1. Use `src/main/resources/application-local.yml`
2. Redis settings must use `spring.redis.*`
3. Route cache backend is controlled by `copier.account-config.route-cache.backend`
4. Route cache warmup is controlled by `copier.account-config.route-cache.warmup-on-startup`

Key local toggles:

1. `SPRING_PROFILES_ACTIVE=local`
2. `ACCOUNT_ROUTE_CACHE_BACKEND=redis`
3. `COPY_ENGINE_SLIPPAGE_ENABLED=false` by default
