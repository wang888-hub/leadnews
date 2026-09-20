# 原项目审查与迁移设计（阶段 0）

> 审查日期：2026-09-02  
> 原项目只读根目录：`D:\Toutiao\project\heima-leadnews`  
> 新项目目标目录：`D:\Toutiao\leadnews-ai-platform`  
> 结论：`D:\Toutiao\project\heima-leadnews` 是功能最完整的工程；`D:\Toutiao\heima-leadnews` 是只含 app gateway、user、article 等模块的裁剪副本，不作为迁移基线。原目录未被修改。

## 1. 审查范围与证据

本次检查了完整版父/子 `pom.xml`、所有服务和网关源码、`bootstrap.yml`、MyBatis XML、Freemarker 模板、课程 SQL、Nginx 配置，以及 `app-web`、`wemedia-web`、`admin-web` 的已构建静态资源。原工程自身没有可复用的 Nacos 配置导出文件；服务运行所需配置主要存在于外部 Nacos，部分值可从源码配置和历史日志还原。因此后续不能把“现有 Nacos 配置完整”当作迁移前提，必须在新项目中显式维护配置清单与初始化内容。

## 2. Maven 模块全量清单

父工程使用 `Spring Boot 2.3.9.RELEASE`、`Spring Cloud Hoxton.SR10`、`Spring Cloud Alibaba 2.2.5.RELEASE`。POM 声明编译级别为 Java 17，但依赖基线较旧，存在“编译级别被手工抬高、框架未同步升级”的不一致。

| 层级 | 模块 | 职责 |
|---|---|---|
| root | `heima-leadnews` | 聚合与依赖版本管理 |
| common | `heima-leadnews-common` | Redis、异常、Jackson、Swagger、Kafka、阿里云审核等通用能力 |
| utils | `heima-leadnews-utils` | JWT、DFA、ID、线程上下文和通用工具 |
| model | `heima-leadnews-model` | PO/DTO/VO、统一响应、枚举和消息模型 |
| api | `heima-leadnews-feign-api` | Article、Wemedia、Schedule Feign 契约及部分 fallback |
| basic | `heima-leadnews-basic` | 基础组件聚合 |
| basic | `heima-file-starter` | MinIO 文件存储 starter |
| gateway | `heima-leadnews-gateway` | 三个网关的聚合模块 |
| gateway | `heima-leadnews-app-gateway` | 用户端路由与 JWT |
| gateway | `heima-leadnews-wemedia-gateway` | 自媒体端路由与 JWT |
| gateway | `heima-leadnews-admin-gateway` | 管理端路由与 JWT |
| service | `heima-leadnews-service` | 七个业务服务聚合 |
| service | `heima-leadnews-user` | 用户登录、关注关系、实名认证审核 |
| service | `heima-leadnews-article` | Feed、详情行为状态、收藏、文章落库、静态化、热点、Kafka Streams |
| service | `heima-leadnews-wemedia` | 自媒体登录、素材、频道、文章、审核、上下架、人工审核 |
| service | `heima-leadnews-schedule` | 动态任务持久化与 Redis 延迟队列 |
| service | `heima-leadnews-search` | ES 搜索，MongoDB 搜索历史和联想词 |
| service | `heima-leadnews-behavior` | 点赞、不喜欢、阅读行为与热点事件生产 |
| service | `heima-leadnews-admin` | 管理员登录；审核/频道/敏感词实际借用 wemedia/user 接口 |
| test | `heima-leadnews-test` | 示例与初始化聚合 |
| test | `kafaka-demo` | Kafka 示例（模块名有拼写错误） |
| test | `es-init` | MySQL 全量导入 ES 的初始化程序 |
| test | `free-marker-demo` | Freemarker 示例；同时被父 POM 重复显式聚合 |

## 3. 服务、网关与主要 API

原项目按 app/wemedia/admin 划分三个 Gateway（51601/51602/51603），业务服务端口依次为 user 51801、article 51802、wemedia 51803、search 51804、behavior 51805、schedule 51701、admin 51809。网关路由来自 Nacos，仓库中未落盘。

主要接口包括：用户登录 `/api/v1/login/login_auth`、关注 `/api/v1/user/user_follow`、实名认证列表/通过/拒绝；Feed `/api/v1/article/load|loadmore|loadnew`、文章行为状态、收藏；行为点赞/不喜欢/阅读；搜索、联想、历史；自媒体素材、频道、文章列表/提交/上下架；人工审核列表、详情、通过、拒绝；Schedule add/cancel/poll。原 API 风格大量使用 POST、命名不统一，且内部 Feign 接口与外部接口边界不够清晰。

网关当前读取自定义 `token` Header，登录路径按字符串 contains 放行，解析 JWT 后用 `headers.add("userId", ...)` 注入身份。下游拦截器读取该 Header 并写 ThreadLocal。问题是未先删除客户端自带 `userId`，可能形成多个同名值；wemedia Gateway 捕获 JWT 异常后甚至继续放行；三个网关复制了 JWT 工具和过滤器；401 为空响应而非统一 JSON。

## 4. 数据库与主要表

课程 SQL 位于课程资料目录，不在 Maven 工程内。原业务按库拆分：

| 数据库 | 主要表 | 用途 |
|---|---|---|
| `leadnews_user` | `ap_user`, `ap_user_fan`, `ap_user_follow`, `ap_user_realname` | 用户、粉丝关注、实名认证 |
| `leadnews_article` | `ap_article`, `ap_article_config`, `ap_article_content`, `ap_author`, `ap_collection` | 文章元数据、内容、配置、作者、收藏 |
| `leadnews_wemedia` | `wm_channel`, `wm_fans_statistics`, `wm_material`, `wm_news`, `wm_news_material`, `wm_news_statistics`, `wm_sensitive`, `wm_user` | 自媒体内容、素材、频道、敏感词与统计 |
| `leadnews_schedule` | `taskinfo`, `taskinfo_logs` | 动态任务当前态与日志态 |
| `leadnews_admin` | `ad_user` 及角色、权限、菜单、策略、访问/文章统计等 15 张表 | 平台管理 |
| `xxl_job` | XXL-Job 官方 8 张表 | 周期任务管理 |

评论资料另有 MongoDB 导入脚本，但当前完整版服务中没有评论服务实现；`ap_article.comment` 只是热点分值来源字段之一。因此“评论闭环”不能视为已实现。

## 5. Redis 使用

- 点赞：Hash `LIKE-BEHAVIOR-{articleId}`，field 为 userId；采用 `HGET` 后 `HSET/HDEL`，不是原子操作。
- 不喜欢：Hash `UNLIKE-BEHAVIOR-{articleId}`。
- 收藏：Hash `COLLECTION-BEHAVIOR-{userId}`，field 为 articleId；收藏目前位于 article service，边界不统一。
- 阅读：Hash `READ-BEHAVIOR-{articleId}`，field 为 userId，保存累计 DTO。
- 关注/粉丝：`APUSER-FOLLOW-{userId}`、`APUSER-FANS-{authorId}`。
- 热点首页：String `hot_article_first_page_{channelId|__all__}`，值为排序后的 Top30 JSON 数组；增量处理会读取、反序列化、修改、全量排序再整体写回。
- 动态调度：ZSet `future_{taskType}_{priority}` 存未来五分钟任务，List `topic_{taskType}_{priority}` 存可消费任务；锁 `FUTURE_TASK_SYNC`。

风险：行为更新没有 Lua；事件发送与 Redis 状态变更无一致性；热点 JSON 写存在并发覆盖；大量 key 无 TTL/版本/租户规范；调度 `SCAN + 清空 + 重载` 在多实例下风险较高。

## 6. Kafka 与 RabbitMQ

源码确认的 Kafka Topic：

| Topic | 生产方 | 消费方 | 用途 |
|---|---|---|---|
| `article.es.sync.topic` | article static service | search | 发布后同步 ES |
| `wm.news.up.or.down.topic` | wemedia | article、search（相应 listener） | 上下架联动 |
| `hot.article.score.topic` | behavior/相关行为 | article Kafka Streams | 行为增量输入 |
| `hot.article.incr.handle.topic` | Kafka Streams | article listener | 2 秒窗口聚合后更新文章与热点缓存 |

此外，人工审核通过后的文章保存使用 RabbitMQ `ARTICLE_EXCHANGE`/routing key，而自动审核路径使用 Feign 保存文章。消息体系存在重复与割裂。Consumer 主要依赖默认自动 ACK，未看到统一重试、DLT、eventId 幂等或 Outbox；Producer 可靠性配置也未工程化固化。

## 7. Elasticsearch 与 MongoDB

ES 7.2.0 使用已淘汰的 `RestHighLevelClient`，索引固定为 `app_info_article`。搜索对 title/content 做 query_string，按 publishTime 倒序，标题高亮；分页实际上固定 `from(0)`，仅通过 `minBehotTime` 做时间游标。文章静态化成功后才发 ES 同步消息，因此 MinIO 失败会阻断索引消息。另有 `es-init` 从 MySQL 全量导入。

MongoDB 保存用户最近 10 条搜索历史，并以 regex 查询 `ap_associate_words` 联想词。联想 regex 难以利用普通索引，规模上升后性能不可控。新项目建议：搜索历史迁移到 MySQL（便于唯一约束、分页和治理），联想词迁移到 ES `completion`（使用独立 suggestion 文档并在发布/热词任务中维护）；这样核心运行无需 MongoDB。评论若后续实现，优先 MySQL 分库表/合理索引，当前阶段不为未实现功能强行保留 MongoDB。

## 8. MinIO 与静态 HTML

`heima-file-starter` 封装上传；素材直接上传 MinIO。文章保存后 `@Async` 使用 Freemarker `article.ftl` 生成 `{articleId}.html`，上传 MinIO，回写 `ap_article.static_url`，随后发 ES 同步消息。优点是保留了静态化思想；问题是异步异常只打印、无状态机/幂等 objectKey/重试/补偿，模板异常仍可能上传空内容，MySQL、对象存储和索引不存在可恢复的一致性协议，也没有 Cache-Control/ETag 设计。

## 9. XXL-Job 与 Schedule Service

XXL-Job 只在 article service 中用于 `computeHotArticle` 周期离线热点计算，方向合理，但执行器 IP 被写死为 `192.168.150.1`，版本属性为 `2.2.0-SNAPSHOT`，课程 SQL却是 2.3.0，存在版本漂移。

Schedule Service 面向大量动态发布时间任务：先写 `taskinfo` 和 `taskinfo_logs`；近五分钟任务写 Redis ZSet/即时 List；每分钟将到期 ZSet 原子管道迁入 List；每五分钟并在启动时从 DB 重载；自媒体服务轮询任务并触发审核/发布。该模式应保留并重构。明显问题包括捕获异常后返回、任务删除与日志更新缺少可靠重试、缓存重载会全量清空、多实例调度与消费幂等不足、`@PostConstruct` 与 scheduled 逻辑耦合。

## 10. 文章、自媒体与审核完整流程

### 自媒体提交

1. `submitNews` 保存/更新 `wm_news`，抽取正文图片并维护 `wm_news_material`。
2. 草稿直接返回；非草稿根据正文图片自动选择封面。
3. 直接 `autoScanWmNews` 的调用已被注释，改为 `wmNewsTaskService.addNewsToTask(newsId, publishTime)`。
4. Schedule 按发布时间投递，wemedia 轮询任务后调用自动审核。

### 自动审核

1. 读取 `wm_news`，仅处理 SUBMIT。
2. 提取标题/正文和图片；图片还会通过 Tess4J OCR 并拼入文本。
3. 本地 `SensitiveWordUtil` DFA 棐查 `wm_sensitive`。
4. 调用阿里云 Green 文本与图片审核。
5. 结果通过则构造 `ArticleDto`，Feign 调 article 保存；命中/不确定时更新 wm_news 状态与原因，部分情况进入人工审核。

实现问题：固定 sleep、`@Async` 与 DB 状态缺少可靠队列、异常降级和状态语义不统一、外部审核 SDK 分散、凭据治理不完整。旧的两态/模糊状态无法满足 PASS/REJECT/REVIEW 与审计记录要求。

### 人工审核与发布

管理端通过 wemedia 接口查询 `wm_news`、查看详情、拒绝或通过。拒绝更新 FAIL/reason；通过设置 ADMIN_SUCCESS，并通过 RabbitMQ 把 ArticleDto 交给 article 保存。article 落库 `ap_article`、`ap_article_config`、`ap_article_content` 后回写 wm_news.articleId；异步 Freemarker + MinIO + staticUrl；最后 Kafka 同步 ES。该链路跨 MySQL 库、RabbitMQ、MinIO、Kafka，无统一事件 ID、Outbox 或明确补偿状态。

## 11. 行为与热点流程

点赞先 HGET 判断再 HPUT/HDEL，随后发 `UpdateArticleMess`；浏览更新 Redis Hash 后发 views +1；收藏仅更新 Redis，未与热点事件/持久表形成完整一致闭环。Kafka Streams 以 articleId 分组，2 秒窗口聚合 collection/comment/likes/views，输出到增量处理 Topic；listener 更新 MySQL计数并修改 Redis 热点 JSON。离线 XXL-Job 查询近期文章，按 `views + likes*3 + comment*5 + collection*8` 排序并覆盖每频道/推荐 Top30。

实时与离线共享的是常量权重概念，但没有统一计算器，实时链路侧重增量计数、离线侧重总分；两套缓存写法都有覆盖风险。新项目应统一 `HotScoreCalculator`，实时写 ZSet 增量、离线原子替换/校准 ZSet。

## 12. 登录与 JWT

app 用户以手机号/密码登录，密码采用 salt + MD5；管理员和自媒体分别登录并签发同类 JWT。JWT 工具在多个模块复制，密钥/令牌规范陈旧。Gateway 将 claim `id` 注入 `userId`，下游拦截器构造最小用户对象放 ThreadLocal，请求结束后清理。需改为 `Authorization: Bearer`、统一密钥环境变量、显式白名单、先移除外部身份 Header 再写内部 Header、统一 401/403 JSON，并阻断绕过网关的服务端口或引入内部签名。

## 13. 原前端能力与现状

`app-web`、`wemedia-web`、`admin-web` 均主要是已构建静态资源，缺少可维护源码与现代构建工程。Nginx 将三套站点代理到 localhost:51601/51602/51603；构建产物中可见旧接口与环境地址，难以系统性修改。

- 用户端：登录、频道/Feed、文章详情、搜索/联想/历史、点赞/不喜欢、收藏、阅读等基础页面。
- 自媒体端：登录、素材、文章列表、编辑/发布、上下架、频道。
- 管理端：登录、实名认证审核、文章人工审核、频道与敏感词管理等。

结论：不复制构建产物，使用 Vue 3/Vite/TypeScript/Element Plus 重写三端；API 仅从 `VITE_API_BASE_URL` 读取。

## 14. 已发现的硬编码地址与敏感配置

- 所有服务与网关 Nacos：`192.168.150.102:8848`。
- Schedule MinIO：`http://192.168.150.102:9000`。
- ES 初始化：`192.168.150.102:9200`。
- Kafka 示例及外部 Nacos 配置痕迹：`192.168.150.102:9092`。
- XXL-Job executor IP：`192.168.150.1`。
- 历史 Nacos 日志还原出 Redis/MinIO 等使用 `192.168.150.102`。
- 源码中存在本机绝对 OCR 路径和明文开发数据库/MinIO密码；新项目必须全部改为环境变量或 `.env`（仅提交 `.env.example`）。

## 15. 主要可靠性、并发与工程问题

1. Spring Boot/Cloud/Alibaba/ES/JWT/Fastjson 等版本老旧且与 Java 17 声明不协调。
2. 外部 Nacos 配置未版本化，仓库无法独立复现运行环境。
3. Kafka/RabbitMQ 双消息体系割裂，无 Outbox、eventId、幂等、retry/DLT 规范。
4. 多处 `catch` 后只打印或仍返回成功；AI/审核式异常无法审计。
5. 点赞为 GET-check-SET 竞态；浏览/收藏缺少一致的异步持久化闭环。
6. 热点 JSON 全量覆盖导致并发丢更新，实时/离线公式未抽象复用。
7. Gateway 身份 Header 可污染，wemedia 鉴权异常可放行。
8. 静态化、DB、MinIO、ES 无状态机和补偿，失败状态不可观测。
9. `@Async` 用于关键业务但无持久任务保证；线程池/超时/重试边界不清。
10. Mongo regex 联想不可扩展；ES 客户端已过时。
11. Schedule 全量清缓存重载及异常吞噬会导致任务丢失/重复。
12. 测试以示例/JUnit4 为主，缺少关键并发、消息幂等和状态机测试。
13. API、命名、包职责和状态码不一致，复制式 Gateway/JWT 代码较多。
14. 原项目没有 AI 摘要、AI 续写、三态 AI 审核、Sentinel 治理、统一 TraceId。

## 16. 功能迁移表

| 原项目功能 | 保留 | 重构 | 新实现 | 原因 |
|---|---:|---:|---|---|
| 用户/自媒体/管理员登录 | 是 | 是 | Gateway + Bearer JWT + 内部身份 Header | 修复安全与一致性 |
| 用户、关注、实名认证 | 是 | 适度 | MySQL + 清晰 DTO/状态枚举 | 保留业务模型 |
| Feed/频道/文章详情 | 是 | 是 | article service + 缓存 + 静态页优先 | 核心闭环 |
| 自媒体素材与文章管理 | 是 | 是 | wemedia + MinIO + 状态机 | 可恢复发布 |
| 定时发布 | 是 | 是 | Schedule DB + Redis ZSet/List + 幂等消费 | 适合海量动态任务 |
| DFA 审核 | 是 | 是 | 本地 Trie，命中立即 REJECT | 低成本前置拦截 |
| 阿里云 Green 审核 | 否 | 替换 | AI service + Qwen 多模态三态输出 | 统一 AI 能力与审计 |
| 人工审核 | 是 | 是 | REVIEW 队列 + 管理端理由/记录 | AI 故障不放行 |
| Freemarker + MinIO | 是 | 是 | 状态机 + 幂等 objectKey + retry/补偿 | 最终一致性 |
| ES 搜索/高亮 | 是 | 是 | ES 8 Java API Client | 客户端与版本升级 |
| Mongo 搜索历史 | 是 | 替换 | MySQL 唯一约束/最近记录 | 降低非必要中间件 |
| Mongo regex 联想 | 是 | 替换 | ES completion suggester | 查询可扩展 |
| 点赞/浏览/收藏 | 是 | 是 | Redis Lua + BehaviorEvent + Kafka + 批写 MySQL | 原子、高吞吐、可追溯 |
| 热点实时/离线 | 是 | 是 | Kafka Streams + Redis ZSet；XXL-Job 校准；共享 Calculator | 避免覆盖和公式漂移 |
| Kafka | 是 | 是 | Topic 常量、acks、幂等、retry/DLT/补偿 | 至少一次语义可控 |
| RabbitMQ 发布 | 否 | 替换 | Kafka + transactional outbox | 减少重复消息栈 |
| XXL-Job | 是 | 是 | 仅周期后台任务 | 不滥用于单篇定时发布 |
| 评论 | 是（目标） | 新增闭环 | behavior/article 边界内先做基础评论 API 与计数事件 | 原源码未完整实现，需诚实标注新增 |
| 三套静态前端 | 功能保留 | 全量重写 | Vue 3 monorepo 三应用 | 原产物不可维护 |
| AI 摘要/续写 | 否 | 新增 | Kafka 异步摘要 + SSE 续写 | 明确业务价值 |
| Sentinel | 否 | 新增 | Gateway 与文章热点参数限流 | 爆款流量保护 |

## 17. 新项目最终模块结构

```text
leadnews-ai-platform/
├─ pom.xml
├─ leadnews-common/
│  ├─ common-core/          # response/error/trace/enums/constants
│  ├─ common-web/           # validation/exception/internal identity
│  ├─ common-redis/         # key registry/Lua/Redisson
│  ├─ common-kafka/         # event envelope/retry/DLT conventions
│  └─ common-storage/       # MinIO abstraction
├─ leadnews-model/          # shared contracts; no service implementation
├─ leadnews-feign-api/      # internal service contracts
├─ leadnews-gateway/        # one deployable gateway, route namespaces app/wemedia/admin
├─ leadnews-user-service/
├─ leadnews-article-service/
├─ leadnews-wemedia-service/
├─ leadnews-behavior-service/
├─ leadnews-search-service/
├─ leadnews-schedule-service/
├─ leadnews-admin-service/
├─ leadnews-ai-service/
├─ frontend/
│  ├─ packages/api-client/
│  ├─ leadnews-app/
│  ├─ leadnews-wemedia/
│  └─ leadnews-admin/
├─ docker/{mysql,nacos,kafka,elasticsearch,minio,xxl-job,redis,sentinel}/
└─ docs/
```

保留八个业务边界，但将三个复制式 Gateway 合并为一个可部署 Gateway，以路由前缀和独立鉴权策略隔离三端；这不合并业务服务，且能消除三份 JWT/filter 漂移。若后续验证三网关需要独立容量，可由同一 gateway module 通过 profile 部署三实例。

## 18. 数据库迁移方案

继续按 user/article/wemedia/schedule/admin 逻辑 schema 管理，开发态可共用一个 MySQL 容器、多个 database。保留原核心表和业务字段语义，统一 bigint ID、审计时间、乐观锁/状态字段与必要联合索引。

新增/调整：

- article：`summary`, `summary_status`, `publish_status`, `static_object_key`, `version`；增加 `article_outbox`、`article_publish_attempt`。
- wemedia/ai：`ai_audit_record`（articleId、decision、riskLevel、reason、confidence、riskTags、model、requestId、createdTime）和人工审核记录；`wm_news` 增加 `ai_generated/source` 与明确状态机。
- behavior：`behavior_event_processed(event_id unique, ...)`、行为聚合/批次表；用户-文章点赞/收藏唯一约束。
- search history：`user_search_history`，唯一 `(user_id, normalized_keyword)`，按更新时间索引并限制最近记录。
- schedule：保留 `taskinfo/taskinfo_logs`，增加业务幂等键、版本和索引 `(status, execute_time, task_type, priority)`。
- 所有外部事件使用全局唯一 eventId；Outbox 以 `(aggregate_type, aggregate_id, event_type, event_version)` 或 eventId 唯一。

迁移顺序：原 SQL 基线清洗 → 新 Flyway 风格版本脚本 → 初始频道/敏感词/演示账户 → 新增 AI/Outbox/幂等表 → 索引验证。密码只保存 BCrypt/Argon2 哈希；演示密码写在本地文档但不作为生产凭据。

## 19. Docker Compose 规划

统一网络 `leadnews-net`，开发机 Java 服务通过 localhost 暴露端口，容器间用 service name：

| 服务 | 建议版本/端口 | 持久化与健康检查要点 |
|---|---|---|
| MySQL | 8.4 LTS / 3306 | init SQL + data volume；`mysqladmin ping` |
| Redis | 7.4 / 6379 | AOF + password env + volume；`redis-cli ping` |
| Nacos | 2.5.x / 8848,9848 | standalone + MySQL；HTTP health |
| Kafka | 3.9.x KRaft / 9092 | 内外双 listener：容器 `kafka:29092`、IDEA `localhost:9092`；broker API health |
| Elasticsearch | 8.17.x / 9200 | single-node、security dev 配置、data volume；cluster health |
| MinIO | 固定 release / 9000,9001 | data volume；`/minio/health/live`；mc init bucket |
| XXL-Job Admin | 与 Java client 固定同版 / 8088 | MySQL schema；HTTP health |
| Sentinel Dashboard | 与 Spring Cloud Alibaba 兼容版 / 8858 | 开发治理；HTTP health |

具体镜像 tag 在阶段 2 前根据选定 Spring Boot/Cloud Alibaba/Spring AI 兼容矩阵再锁定，禁止 `latest`。Compose 使用 `.env`，仓库只提交 `.env.example`。Java 服务不强制容器化。

## 20. 技术基线与架构冲突检查

目标 JDK 21 可选 Spring Boot 3.4.x + Spring Cloud 2024.0.x，并选择官方兼容的 Spring Cloud Alibaba 2023.x/2024.x 版本线；Spring AI 与 Spring AI Alibaba 必须在阶段 1 前以官方 BOM 兼容矩阵最终锁版。这里不在未核验官方矩阵时伪造精确版本。

未发现阻止重构的根本冲突：业务边界、动态调度、静态化、实时/离线热点都可保留。需要明确处理的设计取舍是：

1. RabbitMQ 从核心发布链路移除，统一 Kafka + Outbox；原项目目录中的 RabbitMQ 不再作为新项目依赖。
2. MongoDB 从搜索历史/联想移除，以 MySQL + ES 替代；评论当前没有完整实现，不能以其为保留 MongoDB 的理由。
3. 三 Gateway 合为一个部署模块但保持三路由域；不破坏微服务边界。
4. AI 故障统一 REVIEW，摘要故障不影响发布，二者采用不同失败语义。
5. 定时文章继续 Schedule，不为每篇文章注册 XXL-Job。

## 21. 分阶段实施计划与验收门

| 阶段 | 交付 | 强制验证 |
|---:|---|---|
| 0 | 本分析、迁移表、模块/DB/Docker/阶段规划 | 原项目零修改；证据完整 |
| 1 | JDK21 Maven 父工程与模块骨架 | `mvn clean verify` |
| 2 | Compose 基础设施与初始化 | `docker compose config`、全部 healthcheck |
| 3 | common/model/feign/Nacos/Gateway | JWT、401/403、路由集成测试 |
| 4 | user/article/wemedia 核心 CRUD/状态 | 登录与草稿/提交测试 |
| 5 | Freemarker/MinIO/Schedule | 静态化与定时发布、失败恢复测试 |
| 6 | Kafka/Behavior/Lua | 并发点赞、eventId 幂等、retry/DLT |
| 7 | ES 搜索/高亮/联想/历史 | Testcontainers 搜索测试 |
| 8 | Streams + ZSet + XXL 离线校准 | 共享热度公式、实时/离线校准测试 |
| 9 | AI service/Qwen | API-KEY 环境变量、超时/结构化解析 |
| 10 | DFA → AI → 人工 | 三态与所有 fallback 必为 REVIEW |
| 11 | 摘要 + SSE 续写 | 发布不被摘要失败阻塞；流式中断测试 |
| 12 | Sentinel | 路由及 articleId 热点参数限流 |
| 13 | 三个 Vue 前端 | `pnpm build`、无硬编码 API |
| 14 | 全链路联调 | 用户给出的完整闭环逐项留证 |
| 15 | 测试加固 | 单元/集成测试报告 |
| 16 | 全量文档/简历文档 | 功能声明与实际代码逐项对应 |

每阶段只进入下一阶段的条件：编译成功、相关测试通过、问题被记录/修复、`PROGRESS.md` 更新。阶段 0 完成后，下一步才允许创建 Maven 工程骨架；本次没有开始业务编码。
