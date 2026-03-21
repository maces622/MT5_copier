# Implementation Status

## Scope

This file tracks the server-side implementation status for the current phase:

1. Receive MT5 websocket signals
2. Monitor all bound account roles
3. Convert master trade signals into follower-side dispatch instructions
4. Delay MQ and real MT5 downlink until later iterations

## Implemented Now

### 1. MT5 Signal Ingest

Implemented:

1. WebSocket endpoint: `/ws/trade`
2. Bearer token handshake validation
3. Session registration and disconnect tracking
4. Normalization for `HELLO`, `HEARTBEAT`, `DEAL`, and `ORDER`
5. Short-term dedup for repeated trade events

Key classes:

1. `Mt5TradeWebSocketHandler`
2. `Mt5SignalIngestService`
3. `Mt5SignalNormalizer`
4. `Mt5SessionRegistry`

### 2. Account and Copy Configuration

Implemented:

1. MT5 account binding
2. Credential encryption at rest
3. Risk rule persistence
4. Copy relation management
5. DAG anti-cycle validation
6. Route cache refresh abstraction

### 3. Copy Engine

Implemented:

1. Consume accepted MT5 `DEAL` events from the in-process event bus
2. Resolve the platform master account by `server + login`
3. Load active follower routes and risk snapshots
4. Generate `execution_commands`
5. Generate `follower_dispatch_outbox`
6. Support `FIXED_LOT`, `BALANCE_RATIO`, `EQUITY_RATIO`, `FOLLOW_MASTER`
7. Apply symbol allow/block checks and lot-size limits
8. Apply `reverseFollow` when converting the follower action
9. Consume `ORDER` signals for follower-side TP/SL sync and pending-order replication

Current output model:

1. `execution_commands` keeps the decision result per follower
2. `follower_dispatch_outbox` keeps the follower-side dispatch payload

Dispatch lifecycle now supports:

1. `PENDING`
2. `ACKED`
3. `FAILED`

Current `ORDER` support:

1. Market-order `ORDER_UPDATE` supports position `SL/TP` sync when `followTpSl=true`
2. Pending-order `ORDER_ADD` generates `CREATE_PENDING_ORDER`
3. Pending-order `ORDER_UPDATE` generates `UPDATE_PENDING_ORDER`
4. Pending-order `ORDER_DELETE` generates `CANCEL_PENDING_ORDER`
5. Dispatch payload includes master order metadata, order type/state, requested volume, price, `SL`, and `TP`
6. Market-order lifecycle noise from `ORDER_ADD` and `ORDER_DELETE` is recorded but rejected, so it does not trigger duplicate follower dispatches when a `DEAL` already exists
7. Follower-level manual symbol mapping is applied before risk validation and dispatch generation
8. Dispatch payload now carries slippage policy metadata and source instrument metadata; slippage check is disabled by default and only applies to market opens when enabled

API added for dispatch lifecycle:

1. `GET /api/execution-commands/dispatches/followers/{followerAccountId}?status=...`
2. `PATCH /api/execution-commands/dispatches/{dispatchId}`
3. `GET /api/execution-commands/order-trace?masterAccountId=...&masterOrderId=...`
4. `GET /api/execution-commands/position-trace?masterAccountId=...&masterPositionId=...`

### 4. Follower Exec Skeleton

Implemented:

1. Dedicated websocket endpoint: `/ws/follower-exec`
2. Dedicated bearer/query-token handshake validation
3. Follower session binding by `followerAccountId` or `server + login`
4. Backlog replay from `follower_dispatch_outbox` when follower `HELLO` succeeds
5. Live push of newly created `PENDING` dispatches to online follower sessions
6. Follower-side `ACK` and `FAIL` callbacks with dispatch status write-back
7. Session list API for current follower-exec connections
8. MT5 follower EA real-execution path for `OPEN_POSITION`, `CLOSE_POSITION`, `SYNC_TP_SL`, and pending-order create/update/cancel
9. MT5 follower EA local mapping recovery from MT5 comments on startup

Follower exec APIs:

1. `GET /api/follower-exec/sessions`

Follower exec websocket messages:

1. Inbound: `HELLO`, `HEARTBEAT`, `ACK`, `FAIL`
2. Outbound: `HELLO_ACK`, `DISPATCH`, `STATUS_ACK`

Follower MT5 EA modes:

1. `EXECUTION_DRY_RUN`
2. `EXECUTION_REAL`
3. Market open can reject locally when slippage check is enabled; market close does not apply slippage blocking

### 5. Monitor

Implemented:

1. Persist accepted MT5 signals for audit and query
2. Persist runtime state per `server + login`
3. Mark runtime sessions disconnected when websocket closes
4. Expose merged account monitoring overview

Monitor APIs:

1. `GET /api/monitor/runtime-states`
2. `GET /api/monitor/accounts/overview`
3. `GET /api/monitor/ws-sessions`
4. `GET /api/monitor/accounts/{accountId}/signals`
5. `GET /api/monitor/signals?accountKey=...`

Account overview includes:

1. Account role and account status
2. Effective websocket connection status
3. Last signal metadata
4. Active master/follower relation counts
5. Pending and failed follower dispatch counts

WS session view includes:

1. Current in-memory active websocket sessions
2. Session trace ID and connected time
3. Bound MT5 `server + login` identity when `HELLO` has completed
4. Latest hello/heartbeat/signal snapshot merged from runtime state
5. Optional `userId` and `accountRole` filtering for bound accounts

Connection status derivation:

1. `CONNECTED` when recent activity exists
2. `STALE` when no activity is seen within `copier.monitor.heartbeat-stale-after`
3. `DISCONNECTED` when the websocket session has closed
4. `UNKNOWN` when the account has no runtime signal yet

## Not Implemented Yet

1. MQ integration
2. Pending stop-limit order execution on follower MT5 (`BUY_STOP_LIMIT` / `SELL_STOP_LIMIT`)
3. Durable follower local-ticket mapping outside the EA process
4. Advanced margin checks and broker-side post-fill slippage reconciliation
5. Equity snapshot ingestion and full position monitoring

## Local Connection Configuration

If you want to switch from embedded H2 to real MySQL/Redis, create:

`src/main/resources/application-local.yml`

Use `src/main/resources/application-local.example.yml` as the template.

The keys you need to fill are:

1. `spring.datasource.url`
2. `spring.datasource.username`
3. `spring.datasource.password`
4. `spring.datasource.driver-class-name`
5. `spring.data.redis.host`
6. `spring.data.redis.port`
7. `spring.data.redis.password`
8. `spring.data.redis.database`
9. `copier.security.credentials.secret`
10. `copier.account-config.route-cache.backend`
11. `copier.mt5.signal-ingest.bearer-token`
12. `copier.mt5.follower-exec.bearer-token`
13. `copier.mt5.follower-exec.heartbeat-stale-after`
14. `copier.monitor.heartbeat-stale-after`
