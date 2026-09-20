# Nacos 注册与配置约定

## 阶段 12 Sentinel DataId

| DataId | Group / Namespace | Rule Type | 用途 |
|---|---|---|---|
| `leadnews-gateway-sentinel-gw-flow.json` | LEADNEWS_GROUP / public | `gw-flow` | Route 与 API Group 流控 |
| `leadnews-gateway-sentinel-api-group.json` | LEADNEWS_GROUP / public | `gw-api-group` | public-read/write/ai Path 定义 |
| `leadnews-article-sentinel-param-flow.json` | LEADNEWS_GROUP / public | `param-flow` | articleDetail，paramIdx=0，QPS=5 |

规则只含 resource、阈值和规则参数，不含密码/API Key。开发阈值仅用于验收，阶段 15 压测后重新调优。

阶段 11 已发布 Article（Kafka consumer、摘要长度/重试/恢复/backfill）、Wemedia（AI URL、续写限制）、AI（摘要/续写限制）三个 DataId。Key 仍只有 `${API-KEY:}` 占位，所有密码继续使用环境变量。

阶段 5 在 Article/Wemedia DataId 增加 `leadnews.minio` 环境变量占位；Article 增加 publish 重试/超时/cache；Schedule 增加 datasource、Redis 和 preload/scan/retry 配置。敏感值使用 `MINIO_ACCESS_KEY`、`MINIO_SECRET_KEY`、`DB_PASSWORD`、`REDIS_PASSWORD`，不得在 Nacos 写真实值。

更新时间：2026-09-04

## 连接约定

本地 IDEA/Java 进程连接 `localhost:8848`，Nacos gRPC 使用阶段 2 已暴露的 `localhost:9848`。容器间连接使用 `nacos:8848`，不得在容器中使用 `localhost` 指向 Nacos。

客户端地址和凭据使用环境变量：

- `NACOS_SERVER_ADDR`，本地默认 `localhost:8848`
- `NACOS_USERNAME`，本地默认用户名可用 `nacos`
- `NACOS_PASSWORD`，无源码默认值，必须由本地 `.env`、IDEA 或进程环境注入
- `APP_ENV`，默认 `dev`

## Namespace、Group 与 DataId

- 开发环境 Namespace：`public`
- 统一 Group：`LEADNEWS_GROUP`
- 公共配置：`leadnews-common-dev.yml`
- 服务配置：`${spring.application.name}-dev.yml`

已建立的服务 DataId：Gateway、User、Article、Wemedia、Behavior、Search、Schedule、Admin、AI 各一个。公共配置保存 Actuator 暴露范围、Feign 超时、日志 TraceId 格式等非敏感基础项。阶段 4 已为 User、Article、Wemedia、Admin 的 DataId 增加 datasource URL、用户名占位符和 Flyway 开关；密码只保存 `${DB_PASSWORD}` 占位符，不在 Nacos 或仓库中保存真实值。Redis、Kafka、MinIO 等业务参数仍不属于本阶段。

四个数据库配置分别指向 `leadnews_user`、`leadnews_article`、`leadnews_wemedia`、`leadnews_admin`，机器相关部分统一使用 `${DB_HOST:localhost}`、`${DB_PORT:3307}`、`${DB_USERNAME:root}`、`${DB_PASSWORD}`。IDEA 启动前必须注入 `DB_PASSWORD`；容器化 Java 服务未来应覆盖 DB_HOST 为 `mysql`，不能在容器间使用 localhost。

## 加载方式与优先级

项目使用 Spring Boot Config Data：

```yaml
spring:
  config:
    import:
      - optional:nacos:leadnews-common-dev.yml?group=LEADNEWS_GROUP&refreshEnabled=true
      - optional:nacos:${spring.application.name}-dev.yml?group=LEADNEWS_GROUP&refreshEnabled=true
```

本地 `application-dev.yml` 提供可启动的端口和路由回退值；Nacos 保存团队共享的非敏感配置。环境变量用于密码、JWT 密钥及机器相关地址，并具有最高运维优先级。使用 `optional:` 是为了允许离线编译和基础测试；需要严格依赖 Nacos 的部署环境应在部署清单中改为非 optional 并配置启动失败策略。

Spring Cloud Alibaba 2025.0.0.0 管理 Nacos Client 3.0.3，与阶段 2 的 Nacos Server 3.0.3 对齐。2025.x 不再依赖旧式 `bootstrap.yml` 隐式加载；新增配置必须继续使用 `spring.config.import`。

## 安全与变更规则

- Nacos 控制台认证已启用；账号密码不写入仓库。
- 禁止在 DataId 中存储生产密码、API Key、JWT 密钥或私钥。
- DataId 变更应小步发布并保留审计；动态刷新只用于明确可刷新属性，端口、注册身份等启动参数变更后应重启服务验证。
- 当前 SCA 2025.0.0.0 对占位符和动态刷新存在版本相关注意事项，因此本阶段的 Group 和 DataId 显式声明，不依赖远端配置反向决定其自身定位。
# 阶段 6 Behavior 配置

`leadnews-behavior-service-dev.yml` 已发布到 `public / LEADNEWS_GROUP`。数据库、Redis、Kafka 均使用环境变量：`${DB_PASSWORD}`、`${REDIS_HOST:localhost}`、`${REDIS_PORT:6379}`、`${REDIS_PASSWORD:}`、`${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`。业务 Topic、DLT、pending 重试周期同样可通过 `BEHAVIOR_*` 环境变量覆盖。Java 代码没有写死 Broker 或 Redis 地址。
# 阶段 7 Search DataId

`leadnews-search-service-dev.yml` 已发布 Elasticsearch URL、Kafka topic/DLT、分页限制和 `leadnews_search` datasource。敏感值继续使用 `${DB_PASSWORD}`，地址使用 `${ELASTICSEARCH_URL:http://localhost:9201}` 与 `${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}`，未写入真实密码或容器固定 IP。

## AI Service（阶段 9）

`leadnews-ai-service-dev.yml` 已发布到 `public / LEADNEWS_GROUP`。模型、温度、token、timeout、stream timeout、有限 retry、并发上限、图片大小/数量和 URL allowlist 均可由环境变量覆盖。DashScope Key 只引用 `${API-KEY:}`，Nacos 中没有真实 Key；模型默认 `${AI_MODEL:qwen3.8-max}`，并设置 `multi-model=true`。

# 阶段 8 Hot 配置

Behavior DataId 的 `leadnews.hot` 当前管理 TopN、5 秒窗口、10 秒 grace、增量权重、每分钟衰减和 checkpoint；旧 refresh/DLT Topic 配置已停用。Streams application-id 固定为 `leadnews-hot-score-stream-v1`，state-dir 用 `HOT_STREAM_STATE_DIR` 覆盖。IDEA 使用 Kafka/Redis `localhost:9092`/`localhost:6379`。`hotArticleRebuildJob` 只从 MySQL checkpoint 恢复丢失榜单，不会因权重调整而从累计行为数重算。
# 阶段 10 配置

已重新发布 `leadnews-wemedia-service-dev.yml` 和 `leadnews-ai-service-dev.yml`。新增项包括审核 Topic/group、任务派发/恢复间隔、AI 文本/图片上限、决策阈值以及 MinIO 可信 bucket/prefix。所有数据库、MinIO、Kafka 与 `API-KEY` 均为环境变量占位，Nacos 无真实密钥。
