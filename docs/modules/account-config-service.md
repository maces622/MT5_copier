# 账户与配置服务

## 1. 当前职责

`account-config` 负责维护跟单系统的静态配置真源：

1. MT5 账户
2. follower 风控
3. 主从关系
4. symbol mapping
5. master share 配置
6. route/risk/account-binding 的 Redis 投影

当前规则是：

1. MariaDB 是配置真源
2. Redis 只存投影快照
3. Copy Engine 读 Redis-first，miss 再回源 MariaDB

## 2. 主要输入与输出

### 输入

1. Web Console 的 `/api/me/*` 写请求
2. bootstrap 初始化数据
3. 兼容保留的基础配置接口

### 输出

1. MariaDB 中的配置记录
2. Redis 中的 route/risk/account-binding 快照
3. 给 Copy Engine 和 Monitor 使用的只读配置投影

## 3. 当前已落地能力

1. 绑定或更新 MT5 账户
2. 支持 WebSocket-only 账户不填 `credential`
3. 保存 follower 风控
4. 保存主从关系
5. 更新关系状态与优先级
6. 保存 symbol mapping
7. 保存 master share 配置
8. `share_id + share_code` 建立 follow 关系
9. 显式解绑关系
10. 仅删除 follower 账户

## 4. 关键接口

### 当前用户视角接口

1. `GET /api/me/accounts`
2. `POST /api/me/accounts`
3. `DELETE /api/me/accounts/{accountId}`
4. `POST /api/me/accounts/{accountId}/risk-rule`
5. `GET /api/me/copy-relations`
6. `POST /api/me/copy-relations`
7. `PUT /api/me/copy-relations/{relationId}`
8. `DELETE /api/me/copy-relations/{relationId}`
9. `POST /api/me/accounts/{accountId}/symbol-mappings`
10. `GET /api/me/share-profile`

### 读模型接口

1. `GET /api/accounts/{accountId}`
2. `GET /api/accounts/{accountId}/detail`
3. `GET /api/accounts/{accountId}/risk-rule`
4. `GET /api/accounts/{accountId}/relations`
5. `GET /api/accounts/{accountId}/symbol-mappings`
6. `POST /api/accounts/{accountId}/share-config`
7. `PUT /api/accounts/{accountId}/share-config`
8. `POST /api/copy-relations/join-by-share`

## 5. 关键业务语义

### 5.1 `PAUSED` 不等于解绑

1. 把关系改成 `PAUSED` 再保存，只是暂停复制。
2. 真正解绑关系，必须删除对应的 `copy relation`。

### 5.2 删除账户当前只开放给 follower

`DELETE /api/me/accounts/{accountId}` 只允许删除 `FOLLOWER` 账户。

删除 follower 时会同时清理：

1. 该 follower 参与的关系
2. 该 follower 的风控
3. 该 follower 的 symbol mapping

当前不会开放给 `MASTER` 或 `BOTH`，避免误删主链路配置。

### 5.3 `credential` 与 EA token 不是一回事

1. 前端里的 `credential` 是平台侧保存的 MT5 凭证字段。
2. EA 连接 websocket 用的是后端配置里的 `BearerToken`。
3. 前端配置不会自动下发 EA 的 `WsUrl`、`BearerToken`、`FollowerAccountId`。

## 6. Redis 投影

当前最关键的 Redis key：

1. `copy:account:binding:{server}:{login}`
2. `copy:route:master:{masterAccountId}`
3. `copy:route:version:{masterAccountId}`
4. `copy:account:risk:{followerAccountId}`

当配置变更时，会同步刷新这些快照，供 Copy Engine 热路径直接读取。

## 7. 与其他模块的交互

1. `user-auth`
   提供当前登录用户上下文，决定账户与关系的可见范围和写权限。
2. `copy-engine`
   读取 account-binding、route、risk、symbol mapping。
3. `monitor`
   读取账户基础信息并与 runtime-state、dispatch 计数拼成监控概览。
4. `web-console`
   当前最主要的写入口。

## 8. 当前边界

1. 不负责真实下单
2. 不负责交易审计
3. 不把 Redis 当配置真源
4. 当前没有把配置变更再对外发布到独立 MQ

## 9. 相关文档

1. [总体架构](../architecture/overall-architecture.md)
2. [模块交互与端到端数据链路](../architecture/system-modules-and-dataflows.md)
3. [前端配置到 EA 参数填写](../operations/frontend-to-ea-setup.md)
