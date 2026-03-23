# Redis 备份与恢复

## 设计原则

这个项目里 Redis 的角色是：

1. 降低高频 JPA 读写压力
2. 存放热缓存和运行态
3. 协调去重、会话注册和跨实例通知

这个项目里 Redis 不是：

1. 配置真源
2. 核心交易状态真源
3. 审计流水真源

业务真源始终是 MariaDB。恢复设计也必须围绕这个原则展开。

## 数据分类

### 1. 必须以 MariaDB 为准

这些数据恢复时只认数据库：

1. MT5 账户绑定
2. 风控规则
3. 主从关系
4. 品种映射
5. `execution_commands`
6. `follower_dispatch_outbox`
7. 信号审计流水

说明：

1. `REDIS_QUEUE` 只改变热路径写入顺序，不改变这些数据最终以 MariaDB 为准的原则。

### 2. 可重建缓存

这些 Redis 数据可以丢，可以重建：

1. `copy:route:*`
2. `copy:account:risk:*`
3. `copy:account:binding:*`
4. `copy:hot:*`

恢复时最稳的做法不是“信任旧缓存”，而是：

1. 先恢复 MariaDB
2. 再启动应用
3. 由 warmup、持仓台账回补和配置重写自动重建 Redis

### 3. 运行态 / 易失态

这些键需要特殊处理：

1. `copy:runtime:*`
2. `copy:ws:*`
3. `copy:signal:dedup:*`
4. `copy:follower:dispatch` pub/sub channel

其中：

1. `copy:ws:*` 和 `copy:signal:dedup:*` 不应作为恢复真相使用，恢复后应清理
2. `copy:runtime:*` 可以恢复，但不能无条件信任

原因很直接：`BALANCE_RATIO / EQUITY_RATIO` 会用 runtime-state 做手数计算。恢复出来的旧余额快照如果还没收到新的 heartbeat，就不应该继续参与真实下单。

## 资金快照新鲜度门禁

比例跟单现在有显式门禁：

1. `copier.monitor.runtime-state.funds-stale-after`
2. `copier.monitor.runtime-state.require-fresh-funds-for-ratio`

默认行为：

1. 如果 follower runtime-state 缺失，拒绝比例跟单
2. 如果 follower runtime-state 过期，拒绝比例跟单
3. 不再静默回退成 `1.0` 倍缩放

这意味着：

1. Redis 恢复后，即使 `copy:runtime:*` 还在，也要先过新鲜度校验
2. 超过过期窗口的 runtime-state 不会直接参与真实下单
3. 最稳妥的策略是等待新的 `HELLO / HEARTBEAT`

## 推荐的 Redis 持久化方式

建议同时开启：

1. `appendonly yes`
2. `appendfsync everysec`
3. 保留 `RDB` 快照

这样做的目的不是把 Redis 变成真源，而是：

1. 缩短热状态恢复时间
2. 降低重启后的冷启动成本
3. 保留足够小的热状态恢复窗口

## 建议的备份流程

### 1. 先确认 Redis 持久化配置

关注：

1. `appendonly`
2. `appendfilename`
3. `appenddirname`
4. `dir`
5. `dbfilename`

本地可直接用脚本：

1. `bootstrap/redis-backup-local.ps1`

### 2. 触发快照

本地建议：

1. 先执行 `BGSAVE`
2. 再记录 `LASTSAVE`
3. 再拷贝 Redis 持久化目录里的 RDB / AOF 文件

### 3. 同时备份 MariaDB

不要只备份 Redis。MariaDB 才是业务真源。

## 建议的恢复流程

### 1. 先恢复 MariaDB

先把配置、执行历史、outbox、审计恢复好。

### 2. 再恢复 Redis 文件

恢复 RDB / AOF 只是为了缩短热状态恢复时间，不是为了覆盖数据库真相。

### 3. 清理易失键

恢复后必须先清理：

1. `copy:ws:*`
2. `copy:signal:dedup:*`

本地可直接用脚本：

1. `bootstrap/redis-recovery-cleanup.ps1`

默认它只清理易失键。

### 4. 视情况清理可重建缓存

如果你希望恢复后完全按数据库重建缓存，可以额外清理：

1. `copy:route:*`
2. `copy:account:*`

如果你怀疑 runtime-state 已明显过期，也可以一并清理：

1. `copy:runtime:*`

脚本支持：

1. `-ClearWarmCaches`
2. `-ClearRuntimeState`

### 5. 启动应用

应用启动后会：

1. 预热 route/risk/account-binding/runtime-state
2. 从 `mt5_open_positions` 回补持仓台账热状态
3. 把 `copy:hot:seq:*` 对齐到数据库当前最大 signal / command / dispatch ID
4. 重新建立 session registry
5. 等待 EA 重连和 heartbeat

### 6. 等待新鲜 heartbeat 后再恢复比例跟单

如果你恢复的是老 runtime-state：

1. `BALANCE_RATIO / EQUITY_RATIO` 可能暂时被拒绝
2. 这是正确行为，不是故障
3. 收到新鲜 `HELLO / HEARTBEAT` 后会自动恢复

## 推荐的清理策略

### 必清理

1. `copy:ws:*`
2. `copy:signal:dedup:*`

### 可按需清理

1. `copy:route:*`
2. `copy:account:*`
3. `copy:runtime:*`

## 本地脚本

### 1. 触发备份元信息

```powershell
powershell -ExecutionPolicy Bypass -File .\bootstrap\redis-backup-local.ps1 -Password foobared
```

### 2. 恢复后清理易失键

```powershell
powershell -ExecutionPolicy Bypass -File .\bootstrap\redis-recovery-cleanup.ps1 -Password foobared
```

### 3. 恢复后强制重建缓存

```powershell
powershell -ExecutionPolicy Bypass -File .\bootstrap\redis-recovery-cleanup.ps1 -Password foobared -ClearWarmCaches -ClearRuntimeState
```

## 结论

这套方案的核心不是“把 Redis 备份得像数据库一样可靠”，而是：

1. MariaDB 保持业务真源
2. Redis 保持热状态加速层
3. 恢复后宁可降级、宁可等待新鲜 heartbeat，也不要把旧 Redis 状态直接当真

## Additional Hot Keys In The Current Design

The latest Redis-first copy path adds these hot keys:

1. `copy:hot:command:*`
2. `copy:hot:dispatch:*`
3. `copy:hot:commands:*`
4. `copy:hot:dispatches:*`
5. `copy:hot:persistence:queue`
6. `copy:hot:persistence:dead-letter`
7. `copy:hot:seq:*`
8. `copy:runtime:positions:*`

Backup / recovery interpretation:

1. `copy:hot:*` accelerates command/dispatch lookup and async persistence, but MySQL is still the durable truth.
2. `copy:runtime:positions:*` is recoverable hot state; the durable source is `mt5_open_positions`.
3. After Redis restore, fresh MT5 held-position snapshots can overwrite stale ledger state and trigger async persistence again if the snapshot changed.
4. If restored `copy:hot:seq:*` is missing or lower than DB max IDs, startup warmup will only move it forward and never roll IDs back.
