# 用户与认证服务

## 1. 职责

负责平台层身份管理，不直接管理 MT5 交易逻辑。核心职责：

1. 用户注册与登录
2. 角色权限模型
3. Token 签发与刷新
4. MFA 和高危操作二次确认
5. 审计身份归因

## 2. 核心数据

建议最小表：

1. `platform_user`
2. `user_credential`
3. `user_role`
4. `role_permission`
5. `user_session`
6. `user_mfa_device`
7. `audit_operator_identity`

## 3. 权限模型

至少区分：

1. 普通用户
2. 子账户操作员
3. 风控管理员
4. 运维管理员
5. 审计只读角色

## 4. 对外接口

建议第一阶段接口：

1. `POST /auth/login`
2. `POST /auth/refresh`
3. `POST /auth/logout`
4. `GET /users/me`
5. `POST /auth/mfa/verify`

## 5. 与其他模块的关系

1. Account/Config Service 通过 `user_id` 归属 MT5 账户和跟单配置。
2. Agent Service 执行任何动作前，都需要先拿到用户上下文和权限上下文。
3. 审计日志中的操作者身份由本服务提供。

## 6. 第一阶段边界

第一阶段不需要做复杂组织架构，但下面三件事应保留：

1. JWT 认证
2. 基础 RBAC
3. 高危操作二次校验预留
