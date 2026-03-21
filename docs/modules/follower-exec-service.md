# Follower Exec Service

## Goal

This module is the follower-side downlink execution path.

Reference MT5 EA sample:

`mt5/Websock_Receiver_Follower_Exec_v0.mq5`

Current scope:

1. Accept follower MT5 websocket connections
2. Bind a websocket session to a configured follower account
3. Replay pending dispatches from `follower_dispatch_outbox`
4. Push newly created dispatches to online follower sessions
5. Accept `ACK` and `FAIL` callbacks and write dispatch status back
6. Provide an MT5 follower EA that can run in dry-run mode or submit real `OrderSend` requests

## Terminology

1. `node` means one Java service instance.
2. A local single-process setup is a single-node deployment.
3. In a multi-node deployment, different follower websocket connections may land on different nodes.

## WebSocket Endpoint

Path:

`/ws/follower-exec`

Auth:

1. `Authorization: Bearer <token>`
2. Or query token: `?access_token=<token>`

Config keys:

1. `copier.mt5.follower-exec.path`
2. `copier.mt5.follower-exec.bearer-token`
3. `copier.mt5.follower-exec.allow-query-token`
4. `copier.mt5.follower-exec.heartbeat-stale-after`
5. `copier.mt5.follower-exec.realtime-dispatch.backend`
6. `copier.mt5.follower-exec.realtime-dispatch.channel`

## Inbound Protocol

### 1. HELLO

Bind the session to a follower account.

Supported identity forms:

1. `followerAccountId`
2. `server + login`

Example:

```json
{
  "type": "HELLO",
  "followerAccountId": 12
}
```

Or:

```json
{
  "type": "HELLO",
  "login": 123456,
  "server": "Broker-Live"
}
```

### 2. HEARTBEAT

Example:

```json
{
  "type": "HEARTBEAT"
}
```

### 3. ACK

Example:

```json
{
  "type": "ACK",
  "dispatchId": 1001,
  "statusMessage": "submitted"
}
```

### 4. FAIL

Example:

```json
{
  "type": "FAIL",
  "dispatchId": 1001,
  "statusMessage": "symbol not found"
}
```

## Outbound Protocol

### 1. HELLO_ACK

Sent after a follower session binds successfully.

### 2. DISPATCH

Sent for each pending follower dispatch.

Envelope example:

```json
{
  "type": "DISPATCH",
  "dispatchId": 1001,
  "executionCommandId": 2001,
  "masterEventId": "900001-DEAL-1",
  "payload": {
    "commandType": "OPEN_POSITION",
    "symbol": "XAUUSD"
  }
}
```

### 3. STATUS_ACK

Sent after dispatch status is committed to the database.

## Realtime Dispatch Coordination

### Single-node

1. New dispatches are pushed directly through the current JVM's in-memory `liveSessions`.
2. Redis pub/sub is not required for normal local integration testing.

### Multi-node

1. Session metadata can be shared through the Redis-backed session registry.
2. The actual `WebSocketSession` object still lives only in the JVM that accepted the connection.
3. Redis pub/sub is used only as a notification bus so the node that owns the websocket can push the dispatch.
4. `follower_dispatch_outbox` in the database remains the source of truth.

## MT5 Follower EA

Reference:

`mt5/Websock_Receiver_Follower_Exec_v0.mq5`

Key inputs:

1. `ExecutionMode`
2. `ReplyMode`
3. `FollowerMagicNumber`
4. `CommentPrefix`
5. `DefaultDeviationPoints`

Current EA behavior:

1. Connect to `/ws/follower-exec`
2. Send `HELLO` by `followerAccountId` or `server + login`
3. Send periodic `HEARTBEAT`
4. Receive `HELLO_ACK`, `DISPATCH`, `STATUS_ACK`
5. In `EXECUTION_DRY_RUN`, auto `ACK`, auto `FAIL`, or log-only based on `ReplyMode`
6. In `EXECUTION_REAL`, execute `OPEN_POSITION`, `CLOSE_POSITION`, `SYNC_TP_SL`, `CREATE_PENDING_ORDER`, `UPDATE_PENDING_ORDER`, and `CANCEL_PENDING_ORDER`
7. Track local follower position/order tickets in memory and rebuild mappings from MT5 comments on EA startup

## Current Limits

1. Real execution is implemented in the MT5 EA, but it is still best-effort and not production-hardened.
2. Pending `BUY_STOP_LIMIT` and `SELL_STOP_LIMIT` are not supported yet.
3. Local position/order mappings are only rebuilt from live MT5 comments; no separate durable mapping store exists yet.
4. No margin pre-check, no broker-side slippage model, and no retry/backoff model are implemented yet.
5. Symbol remapping is generated in Java dispatch payloads, but final execution still depends on the follower EA accepting the mapped target symbol.
6. Cross-node realtime push currently uses Redis pub/sub notification only; there is no durable claim/lease or compensation worker yet.

Recommended safety default:

1. Keep `ExecutionMode=EXECUTION_DRY_RUN` during protocol testing.
2. For local single-node testing, prefer `copier.mt5.follower-exec.realtime-dispatch.backend=local`.
3. Switch to `EXECUTION_REAL` only after the account binding, route setup, and symbol checks are validated.
4. Enable `copier.mt5.follower-exec.realtime-dispatch.backend=redis` only when validating multi-node realtime push behavior.
