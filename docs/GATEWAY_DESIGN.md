# Gateway 路由与安全边界

## 阶段 12 Sentinel

使用 Sentinel 1.8.9 Spring Cloud Gateway v6x Adapter，按真实 Route ID 及 public-read/write/ai 三个 API Group 治理。顺序为 Trace(-200)、JWT(-100)、Sentinel(-1)、Routing，保证 429 含 traceId、无认证优先 401、伪造身份头仍被清洗。`/internal/**` 未新增路由。

更新时间：2026-09-04

## 路由

Gateway 监听 `51601`，使用 `lb://` 和 Nacos 服务发现，不写死实例地址。

| 外部路径 | 服务名 | 端口 |
| --- | --- | --- |
| `/api/user/**` | `leadnews-user-service` | 51801 |
| `/api/article/**` | `leadnews-article-service` | 51802 |
| `/api/wemedia/**` | `leadnews-wemedia-service` | 51803 |
| `/api/behavior/**` | `leadnews-behavior-service` | 51804 |
| `/api/search/**` | `leadnews-search-service` | 51805 |
| `/api/admin/**` | `leadnews-admin-service` | 51807 |
| `/api/ai/**` | `leadnews-ai-service` | 51808 |

Schedule 当前没有公共 HTTP 路由，避免把内部调度能力暴露到外网。

## 过滤器顺序

1. Trace 过滤器生成或规范化 `X-Trace-Id`，并在响应中回传。
2. JWT 过滤器清除客户端传入的用户/内部调用头。
3. 白名单请求直接继续；其他请求必须提供 `Authorization: Bearer <token>`。
4. Token 验证成功后，仅由 Gateway 注入可信 `X-User-Id`；无效或过期 Token 返回脱敏 JSON 401。
5. `/internal/**` 在 Gateway 边界直接返回 404，内部契约不建立外部路由。

开发白名单仅包含 `/api/user/login`、`/api/user/register` 和 `/actuator/health`。当前没有实现登录业务，因此这两个业务路径只是为下一阶段保留的安全边界。

## 内部调用

`leadnews-article-service` 通过 `UserFeignClient` 和服务名 `leadnews-user-service` 调用 `/internal/users/ping`。Feign 自动透传 TraceId、用户 ID和内部调用标记；User 的内部端点拒绝缺少内部标记的请求。实际测试证明调用由 Nacos/LoadBalancer 解析，没有写死 IP 或端口。

## 后续注意事项

- 阶段 4 实现登录时再确定 Token 生命周期、刷新策略、角色/权限声明和吊销机制。
- 生产环境不能仅依靠可伪造的内部 Header；应限制服务端口暴露并增加服务身份认证。
- Gateway 保持纯 WebFlux，禁止引入 `spring-boot-starter-web`。
- 新增路由时必须同时评审鉴权、限流、超时、重试幂等性和内部接口暴露风险。
