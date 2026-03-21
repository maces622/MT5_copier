# API Gateway

## 1. 职责

API Gateway 是所有外部入口的统一边界，负责：

1. HTTP / WebSocket 请求接入
2. JWT 校验与租户识别
3. 频率限制与防刷
4. 防重放和签名校验
5. 灰度路由与版本路由
6. 统一 trace id 注入

Gateway 不承载任何交易业务逻辑。

## 2. 入口类型

### 2.1 用户 API

1. 账户绑定
2. 跟单规则配置
3. 持仓查询
4. 历史订单查询
5. 告警配置

### 2.2 WebSocket 推送入口

前端连接 `/ws/notify`，由 Gateway 完成握手鉴权并转发到 Notification Service。

### 2.3 管理入口

用于运营、审计、风控后台，权限与普通用户接口隔离。

## 3. 鉴权与安全

1. Access Token 过期短，Refresh Token 长。
2. 高危接口要求幂等键和请求签名。
3. 对“强平、停用跟单、修改凭证”等接口启用二次认证。
4. WebSocket 握手时校验 Token，并将用户身份注入下游。

## 4. 限流策略

建议三层限流：

1. IP 级限流
2. 用户级限流
3. 接口级限流

需要重点保护的接口：

1. 登录
2. 绑定 MT5 账户
3. 修改跟单配置
4. 历史分页查询
5. WebSocket 重连

## 5. 输出规范

Gateway 应统一：

1. 错误码
2. trace id
3. request id
4. 时间戳

示例：

```json
{
  "code": "OK",
  "message": "",
  "traceId": "9e09c1c5f21d",
  "data": {}
}
```

## 6. 第一阶段落地建议

第一阶段可以先将 Gateway 与 User/Auth 合并在同一 Spring Boot 应用中，但代码层面仍按模块隔离，避免后续拆分成本过高。
