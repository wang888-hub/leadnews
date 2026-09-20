# 公共服务基础能力

更新时间：2026-09-04

## 模块边界

- `leadnews-common` 提供统一响应、错误码、业务异常、Servlet 全局异常处理、TraceId、UserContext、JWT 工具和 Feign 请求上下文透传；不依赖任何业务服务。
- `leadnews-model` 只保存跨模块 DTO，不包含业务实现。
- `leadnews-feign-api` 保存服务间契约，依赖 `leadnews-model`，不依赖服务实现。
- 服务实现只依赖公共层或契约层，服务之间没有 Maven 直接依赖，因此不会形成循环依赖。
- Servlet 自动配置与通用/Feign 自动配置分离，Gateway 不会被传递引入 Spring MVC。

## 统一响应与异常

`ResponseResult<T>` 的字段为 `code`、`message`、`data`、`timestamp`、`traceId`。成功码为 `0`；当前基础错误码覆盖参数校验、未认证、无权限、内部 Feign 失败和系统异常。

`GlobalExceptionHandler` 只在 Servlet 应用中启用。参数校验和业务异常返回明确错误；Feign 和未知异常仅在服务日志保留堆栈，对外返回脱敏消息，不暴露内部主机、类名或调用栈。

## TraceId 与用户上下文

- Gateway 接受合法的 `X-Trace-Id`，否则生成 32 位 UUID；该值写回响应头并传给下游。
- Servlet 拦截器将 `X-Trace-Id`、`X-User-Id` 放入 MDC/`UserContext`，请求完成后清理 ThreadLocal。
- Feign 拦截器传递 TraceId、可信用户 ID，并增加 `X-Internal-Request: true`。
- Gateway 会先删除客户端伪造的 `X-User-Id` 和 `X-Internal-Request`，再从已验证 JWT 注入用户 ID。

## JWT 基线

JWT 使用 HMAC-SHA 密钥，密钥必须通过 `JWT_SECRET` 环境变量注入且至少 32 UTF-8 字节。当前阶段只实现签发/验签工具与 Gateway 校验骨架，不实现真实登录、刷新令牌、注销、权限模型或 Redis 黑名单。

生产环境必须使用独立强随机密钥，并通过密钥管理系统注入；不得把密钥放入 Git 或 Nacos 明文配置。当前内部调用标记只用于建立调用边界，不等价于零信任服务认证；后续生产化需叠加网络隔离或 mTLS/服务身份。

