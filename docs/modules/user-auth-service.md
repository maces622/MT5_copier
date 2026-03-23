# 用户认证服务

## 1. 当前职责

`user-auth` 负责平台层身份体系，不直接参与 MT5 下单。

当前职责：

1. 平台用户注册
2. 平台用户登录与登出
3. 当前用户信息查询
4. 显示名与密码修改
5. 会话持久化
6. 为账户台、分享绑定、监控台提供当前用户上下文

## 2. 当前数据模型

### `platform_users`

核心字段：

1. `platform_id`
2. `username`
3. `password_hash`
4. `share_id`
5. `display_name`
6. `status`
7. `role`

### `platform_user_sessions`

核心字段：

1. `user_id`
2. `session_token_hash`
3. `expires_at`
4. `last_seen_at`
5. `ip`
6. `user_agent`

## 3. 当前接口

1. `POST /api/auth/register`
2. `POST /api/auth/login`
3. `POST /api/auth/logout`
4. `GET /api/auth/me`
5. `PUT /api/auth/me`

## 4. 当前实现特征

1. 注册后自动生成 `platform_id`
2. 注册后自动生成 `share_id`
3. 当前使用 `session + HttpOnly cookie`
4. 当前基础角色只有 `USER` 与 `ADMIN`

## 5. 与其他模块的交互

1. `account-config`
   用当前用户确定账户和关系归属
2. `monitor`
   用当前用户限制可见账户范围
3. `web-console`
   登录、设置页直接依赖本模块

## 6. 当前边界

1. 不是 JWT 体系
2. 没有 MFA
3. 没有找回密码
4. 还没有更细粒度 RBAC

## 7. 相关文档

1. [总体架构](../architecture/overall-architecture.md)
2. [账户与监控控制台方案](../architecture/account-monitor-console.md)
