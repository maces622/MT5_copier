# 账户与配置服务

## 当前实现状态

当前仓库已落地第一版实现，覆盖：

1. MT5 账户绑定
2. 凭证加密存储
3. 风控规则保存
4. 主从关系创建与更新
5. 防环校验
6. Redis 路由缓存抽象，支持 `log` 和 `redis` 两种 backend

当前仓库尚未覆盖：

1. 凭证解密读取的受控流程
2. 审计事件发布
3. 配置变更 MQ 广播
4. 更细粒度的关系级风控参数

## 1. 职责

这是平台配置中心，负责：

1. 绑定 MT5 账户
2. 保存主从跟单关系
3. 管理风控参数
4. 生成并刷新 Redis 路由表
5. 防止循环跟单
6. 凭证加密与版本管理

## 2. 核心对象

### 2.1 MT5 账户

建议字段：

1. `account_id`
2. `user_id`
3. `broker_name`
4. `server_name`
5. `mt5_login`
6. `credential_ciphertext`
7. `credential_version`
8. `account_role`，例如 `MASTER` / `FOLLOWER` / `BOTH`
9. `status`

### 2.2 跟单关系

建议字段：

1. `relation_id`
2. `master_account_id`
3. `follower_account_id`
4. `copy_mode`
5. `status`
6. `priority`
7. `config_version`

### 2.3 风控规则

建议字段：

1. `max_lot`
2. `fixed_lot`
3. `balance_ratio`
4. `max_slippage_points`
5. `max_daily_loss`
6. `max_drawdown_pct`
7. `allowed_symbols`
8. `blocked_symbols`
9. `follow_tp_sl`
10. `reverse_follow`

## 3. Redis 路由缓存

Copy Engine 核心链路不能频繁查 MySQL，因此本服务负责把主从关系投影到 Redis。

建议键：

1. `copy:route:master:{masterAccountId}`
2. `copy:route:version:{masterAccountId}`
3. `copy:account:risk:{followerAccountId}`

`copy:route:master:{masterAccountId}` 中至少包含：

1. 跟单账号列表
2. 跟单模式
3. 风控快照
4. 配置版本

## 4. 防环设计

保存主从关系前必须做有向图校验。

规则：

1. 不允许自跟单
2. 不允许形成环
3. 不允许跨租户非法引用

建议在写入事务内完成：

1. 先读取当前关系图
2. 临时加入新边
3. 做 DAG 检查
4. 成功后落库并刷新 Redis

## 5. 凭证安全

1. 密码只允许在受控服务内解密。
2. 凭证字段加密后落 MySQL。
3. 凭证更新要产生新版本号。
4. 凭证读取需要审计。

## 6. 对外接口

建议第一阶段：

1. `POST /accounts`
2. `GET /accounts`
3. `POST /copy-relations`
4. `PUT /copy-relations/{id}`
5. `POST /risk-rules`
6. `GET /copy-relations/master/{masterAccountId}`

## 7. 发布事件

建议发布：

1. `account.bound.v1`
2. `copy.relation.changed.v1`
3. `risk.rule.changed.v1`

这样 Copy Engine 和 Monitor 能及时刷新本地缓存。

## 8. 第一阶段实现重点

优先保证下面四点，不要一开始就做过多 UI：

1. 账户绑定
2. 跟单关系建模
3. 风控快照写 Redis
4. 防环校验
