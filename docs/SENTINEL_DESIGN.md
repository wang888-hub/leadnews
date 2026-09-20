# Sentinel 流量治理设计

## 版本与依赖依据

阶段 12 不升级 Spring Boot 3.5.16、Spring Cloud 2025.0.3 或 Spring Cloud Alibaba 2025.0.0.0。Alibaba BOM 管理两个 Sentinel Starter 为 2025.0.0.0，并实际解析 Sentinel Core、参数流控、Spring Cloud Gateway v6x Adapter、Nacos datasource 为 1.8.9。Gateway 使用 WebFlux/Reactor Adapter，未复制 Servlet Filter。

## 两层治理与本地阈值

Gateway 按真实 Route ID 和少量 API Group 控制入口速率；Article Service 的 `articleDetail(Long articleId)` 使用 `@SentinelResource("articleDetail")` 与 `paramIdx=0` 隔离单热点文章。前者保护整体入口，后者保护参数值，职责不同。

| 类型 | Resource | QPS | 用途 |
|---|---|---:|---|
| Route | user-service | 20 | 用户入口 |
| Route | article-service / article-static | 20 / 30 | 动态/静态文章 |
| Route | wemedia-service | 10 | 自媒体操作 |
| Route | behavior-service | 8 | 行为写入口 |
| Route | search-service | 5 | ES 查询 |
| Route | admin-service | 5 | 管理接口 |
| Route | ai-service | 2 | AI 对外路由 |
| API Group | public-read-api | 15 | Article、Search |
| API Group | write-api | 8 | Behavior |
| API Group | ai-api | 1 | SSE 续写、AI |
| Param Flow | articleDetail | 5/参数值 | articleId 索引 0 |

以上均为一秒窗口的本地功能验证值，不代表生产容量。AI Group QPS 管进入速度，阶段 11 Semaphore=4 管模型并发，二者均保留；SSE 仅在入站时判定。

## Nacos 动态规则

规则事实来源为 Nacos public namespace、`LEADNEWS_GROUP`。应用只声明 DataId；JSON 规则在 Nacos，仓库 `docker/nacos/config` 保存无密钥模板。Gateway 使用 `gw-flow`，API 定义使用 `gw-api-group`，Article 使用 `param-flow`。

实测 Gateway PID 45588 不变：Article Route QPS=5 时 30 并发为 200=5、429=25；动态改 20 后为 200=20、429=10。最终值为 20。Nacos 曾短暂发生 gRPC 连接失败，服务仍保持 UP，恢复后自动重订阅；客户端使用最后成功加载的进程内规则，不在每次请求访问 Nacos。

## 真实验收

- 热点隔离：articleId=13 的 30 次为 200=5、429=25；同窗 articleId=12 的 5 次为 200=5、429=0；窗口后 13 恢复 200。
- Behavior：VIEW 20 次为 200=8、429=12；Redis `article:counter:13.viewCount` 从 8 到 16，增量严格等于通过数。
- Search：20 次为 200=5、429=15，窗口后恢复 200。
- SSE：草稿 22 一次真实 qwen3.8-max 为 HTTP 200，meta=1、chunk=16、done=1、error=0。
- AI Block：动态设 ai-api=0 后 5/5 为 429，重启后的 `ai.continuation.requests` 指标未注册（0 次业务/Provider 调用），随后恢复 QPS=1。

## 响应、安全和指标

Gateway Block 为 HTTP 429 JSON（42900）；Article 热点 Block 为 HTTP 429（42901）；均含 timestamp、data=null、traceId。日志只记录 resource、rule type、traceId。指标 `sentinel.block.total` 仅按 resource 标记，不使用 articleId。

顺序为 Trace(-200) → JWT(-100) → Sentinel Gateway(-1) → Routing。实测无 Token 的 Behavior 请求仍为 401；身份头清洗测试继续通过；`/internal/**` 仍为 404。匿名 Article/Search 语义不变。

## Dashboard 与阶段 15

未部署 Dashboard：阶段 2 没有可靠固定镜像，本阶段不引入来源不明镜像；Nacos 才是规则事实来源。需要时可独立运行官方固定 1.8.9 Dashboard JAR。阶段 15 必须依据压测、下游容量、错误率和模型成本重新调参。
