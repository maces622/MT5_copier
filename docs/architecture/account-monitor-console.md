# 账户与监控控制台方案

## 1. 当前定位

当前 `web-console/` 的定位不是“运营大屏”，而是“配置入口 + 联调入口 + 排障入口”。

它覆盖两块能力：

1. 账户台
   管理 MT5 账户、风控、关系、symbol mapping、share 配置
2. 监控台
   查看 runtime-state、session、command、dispatch、trace

## 2. 当前页面

1. `/login`
2. `/register`
3. `/app/overview`
4. `/app/accounts`
5. `/app/accounts/:accountId`
6. `/app/share`
7. `/app/follow/bind`
8. `/app/relations`
9. `/app/monitor/accounts`
10. `/app/monitor/accounts/:accountId`
11. `/app/traces/order`
12. `/app/traces/position`
13. `/app/settings/profile`

## 3. 当前控制台写能力

1. 绑定 MT5 账户
2. 保存 follower 风控
3. 创建主从关系
4. 更新关系状态与优先级
5. 显式解绑关系
6. 保存 symbol mapping
7. 保存 share 配置
8. 通过 `share_id + share_code` 建立 follow 关系
9. 删除 follower 账户
10. 修改显示名与密码

## 4. 当前控制台读能力

1. 账户列表
2. 账户详情聚合
3. 当前用户关系列表
4. share 档案
5. monitor dashboard
6. monitor account detail
7. command 列表
8. dispatch 列表
9. order / position trace

## 5. 当前重要语义

### 5.1 `PAUSED` 不是解绑

1. 改成 `PAUSED` 并保存，只是暂停跟单。
2. 真正解绑要删除关系。

### 5.2 删除账户只开放给 follower

1. 当前控制台只允许删除 `FOLLOWER` 账户。
2. 删除时会同时清理：
   copy relation
   risk rule
   symbol mapping

### 5.3 控制台不会自动配置 EA

控制台保存的是平台内配置，不会自动下发 EA 的本地输入参数。

EA 仍然要手工填写：

1. `WsUrl`
2. `BearerToken`
3. `FollowerAccountId`
4. `ExecutionMode`

## 6. 当前监控页数据来源

### 概览页

来源于：

1. MT5 账户基础数据
2. runtime-state
3. dispatch 计数

### 详情页

来源于：

1. `overview`
2. `runtimeState`
3. `wsSessions`
4. `followerExecSessions`
5. `commands`
6. `dispatches`
7. `traces`

### 当前已确认语义

1. follower 的 `balance / equity` 来自 follower `HELLO / HEARTBEAT`
2. follower 的 `lastSignalType` 可能为空，这是当前设计内正常现象
3. 监控页当前通过定时轮询刷新，不是前端直接订阅后端推送

## 7. 当前边界

1. 还没有完整告警中心
2. 还没有工单联动
3. 还没有运营型报表
4. 还没有细粒度 RBAC

## 8. 相关文档

1. [总体架构](./overall-architecture.md)
2. [模块交互与端到端数据链路](./system-modules-and-dataflows.md)
3. [前端配置到 EA 参数填写](../operations/frontend-to-ea-setup.md)
