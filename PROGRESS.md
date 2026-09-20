# 实施进度

更新时间：2026-09-05

## 阶段 0

- 已完成原项目只读扫描与迁移分析。
- 分析结果见 `docs/ORIGINAL_PROJECT_ANALYSIS.md`。

## 阶段 1：Maven 父工程和微服务骨架（已完成）

### 已完成内容

- 建立 Maven 聚合父工程，统一 Java 21、UTF-8、插件和依赖版本。
- 建立公共模块、契约模块、Gateway 和八个业务服务骨架。
- 所有可运行模块均包含启动类、`application.yml` 和 `application-dev.yml`。
- 建立公共依赖单向边界；服务之间没有 Maven 直接依赖和循环依赖。
- Gateway 仅保留 WebFlux Gateway 骨架；AI、Search、Behavior、Schedule 均未提前接入后续组件。
- 未复制原项目业务代码，未创建 Controller、Service、Mapper、SQL、Docker 或前端资源。

### 最终模块结构

```text
leadnews-ai-platform
├─ leadnews-common
├─ leadnews-model
├─ leadnews-feign-api
├─ leadnews-gateway
├─ leadnews-user-service
├─ leadnews-article-service
├─ leadnews-wemedia-service
├─ leadnews-behavior-service
├─ leadnews-search-service
├─ leadnews-schedule-service
├─ leadnews-admin-service
└─ leadnews-ai-service
```

根包名统一为 `com.aaliyun.leadnews`。没有新增 infrastructure 模块：当前阶段尚无可复用的中间件适配实现，过早建立该模块只会形成空壳和不明确的依赖边界；待后续出现明确复用需求时再评估。

### 最终技术版本

| 技术 | 版本 |
| --- | --- |
| JDK | 21 |
| Spring Boot | 3.5.16 |
| Spring Cloud | 2025.0.3 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| MyBatis-Plus | 3.5.17 |
| Spring AI | 1.1.2（仅 BOM 预留） |
| Spring AI Alibaba | 1.1.2.2（仅 BOM 预留） |
| Lombok | 1.18.42 |
| Maven Compiler Plugin | 3.14.1 |

### 版本选择依据

- Spring Cloud 2025.0.x 官方映射 Spring Boot 3.5.x。
- Spring Cloud Alibaba 2025.0.x 官方映射 Spring Cloud 2025.0.x、Spring Boot 3.5.x，并支持 JDK 17 及以上。
- Spring AI 1.1.x 官方映射 Spring Boot 3.5.x；Spring AI Alibaba 1.1.2.2 对齐 Spring AI 1.1.2。
- MyBatis-Plus 3.5.17 提供 Spring Boot 3 专用 Starter。
- 详细依据和后续兼容注意事项见 `docs/TECH_STACK.md`。

### 构建验证

- 执行目录：`D:\Toutiao\leadnews-ai-platform`
- 命令：`mvn clean verify`
- 验证工具：Apache Maven 3.9.10；编译目标 `release 21`
- 结果：`BUILD SUCCESS`，父工程与全部 12 个子模块均为 `SUCCESS`。
- Java 源码编译日志明确使用 `release 21`。
- 新项目源码、POM 和配置未引用原项目绝对路径，未包含旧虚拟机固定地址。
- 原项目 Git 工作树保持不变。

### 当前遗留问题

- 已增加 Maven Wrapper 3.9.10，并通过项目级环境使用 Temurin JDK 21.0.12.1；不修改 Windows 全局 `JAVA_HOME`。
- 父 POM 已增加 Maven Enforcer，要求 Maven `[3.9,4.0)`、Java `[21,22)`，防止误用 JDK 17 或 23。
- 使用 JDK 21 执行 `mvn clean verify`，父工程和全部 12 个子模块均为 `SUCCESS`。
- Spring Cloud Alibaba 2025.0.0.0 与 Spring Cloud 2025.0.3 同属 2025.0 发布列车；启用 Nacos、Sentinel 时需通过真实启动和集成测试进一步确认组件级兼容性。
- 公共模块目前是预期的空 Jar，仅建立依赖边界；不应为了填充模块而提前加入业务代码。

## 阶段 2：Docker Desktop 基础设施环境（已完成）

### 已完成

- 完成阶段 1 收尾：Maven Wrapper 3.9.10、项目级 Temurin JDK 21.0.12.1、Maven Enforcer 和 JDK 21 全量构建验证。
- 修复 Maven Wrapper 3.3.4 Windows 启动脚本在普通目录的空 Junction Target 上索引空数组的问题；最终 Wrapper 全量 Reactor 再次 `BUILD SUCCESS`。
- 创建 `docker-compose.yml`、`.env.example` 和本地忽略的 `.env`。
- 创建 MySQL、Redis、Nacos、Kafka、Elasticsearch、MinIO、XXL-Job 的 Docker 配置及初始化目录；未加入不可靠的 Sentinel 第三方镜像。
- MySQL 初始化脚本创建 `leadnews_user`、`leadnews_article`、`leadnews_wemedia`、`leadnews_behavior`、`leadnews_admin`、`leadnews_schedule` 和 `xxl_job` schema，仅初始化 XXL-Job 官方基础表，不创建业务表。
- Kafka 使用 KRaft，配置容器内部 `kafka:29092` 与 Windows 主机 `localhost:9092` 两套 listener/advertised listener。
- 使用命名 Volume 持久化数据，并建立统一 `leadnews-network`。
- `docker compose config --quiet` 已通过，所有镜像均使用固定版本标签，无 `latest`。
- 因宿主机端口 3306、9200 已占用，采用 MySQL `3307:3306`、Elasticsearch `9201:9200`，未停止占用端口的用户程序。

### 已验证

- Maven Wrapper 在 JDK 21 下执行 `clean verify`：`BUILD SUCCESS`。
- 宿主机访问 Elasticsearch `http://localhost:9201/` 成功，返回版本 `8.17.10` 的集群信息。
- MySQL 实际连接成功，7 个 schema 均存在；`xxl_job` 有 8 张基础表和本地管理员记录。
- Redis 带密码执行 `PING`，返回 `PONG`。
- Nacos Console 返回 HTTP 200，v3 readiness API 返回 `code=0/data=ok`；完成本地管理员首次初始化并成功登录，容器最终为 healthy。
- Kafka Host 端口 9092 可达；通过临时 Topic 完成 EXTERNAL 生产/INTERNAL 消费和 INTERNAL 生产/EXTERNAL 消费，消息内容一致；Broker 元数据返回 `localhost:9092`，测试 Topic 已删除。
- MinIO API 和 Console 返回 HTTP 200；通过 `leadnews-network` 实际连接并列出私有 Bucket `leadnews`。
- XXL-Job Admin 登录页返回 HTTP 200，使用本地管理员调用登录接口返回 `code=200/success=true`；数据库连接和 8 张基础表已验证。
- `docker compose ps -a` 显示 MySQL、Redis、Nacos、Kafka、Elasticsearch、MinIO、XXL-Job Admin 均为 healthy；一次性 `minio-init` 容器为预期的 Exited (0)。
- 原项目 Git 工作树为空；新项目配置与源码扫描未发现原项目绝对路径或旧虚拟机固定地址；`.env` 已加入 `.gitignore`。

### Docker 服务版本与端口规划

| 服务 | 镜像 | Windows 端口 |
| --- | --- | --- |
| MySQL | `mysql:8.4.11` | `3307` |
| Redis | `redis:7.4.11-alpine` | `6379` |
| Nacos | `nacos/nacos-server:v3.0.3` | `8080`、`8848`、`9848` |
| Kafka | `apache/kafka:3.9.1` | `9092` |
| Elasticsearch | `docker.elastic.co/elasticsearch/elasticsearch:8.17.10` | `9201` |
| MinIO | `quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z` | `9000`、`9001` |
| MinIO Client | `minio/mc:RELEASE.2025-08-13T08-35-41Z` | 不暴露 |
| XXL-Job Admin | `xuxueli/xxl-job-admin:3.4.2` | `8088` |

### 遇到的问题与当前状态

- Docker Desktop 在创建和检查项目容器时出现宿主机级状态异常：`docker ps` 可列出容器，但 `docker exec` / `docker inspect` 间歇返回 `No such container`。
- 随后 Docker Desktop 的 Inference manager 发生宿主机异常；按用户要求暂停，未删除任何容器、镜像或 Volume。用户修复 Docker Desktop 后，全部服务恢复。
- Nacos 长时间停留在 `health: starting`。日志显示服务已启动，但旧检查错误访问 8848 上的 Console 路径；Nacos 3 Console 实际位于 8080，且 readiness 返回 `data: ok` 而不是 `UP`。仅重建 Nacos 容器应用正确检查后变为 healthy，未重建其他服务。
- Windows 挂载的 MySQL `conf.d` 文件产生 world-writable 警告；关键 UTF8MB4、排序规则和时区同时通过容器启动参数明确设置，因此实际服务器配置不受影响。

### 尚未完成

- 本阶段无阻塞遗留项。
- Sentinel Dashboard 按计划未容器化；后续如需使用，采用与 Spring Cloud Alibaba 管理版本匹配的官方 Jar。
- 尚未创建业务表、Elasticsearch 文章索引、Redis Key、Kafka 业务 Topic 或 Nacos 业务 DataId；这些内容留到对应业务阶段。

## 阶段 3：公共基础能力、Nacos、Gateway、OpenFeign（已完成）

### 已完成

- `leadnews-common` 增加统一响应、错误码、业务异常、Servlet 全局异常处理、TraceId/MDC、UserContext、JWT 工具及 Feign 上下文透传。
- `leadnews-model` 增加最小 Ping DTO；`leadnews-feign-api` 增加 User 内部调用契约。
- 全部九个可运行模块接入 Nacos Discovery、Config Data 和 Actuator；服务端口固定为 Gateway `51601`、业务服务 `51801` 至 `51808`。
- Nacos `public` Namespace、`LEADNEWS_GROUP` 下发布一个公共 DataId 和九个服务 DataId，凭据继续通过环境变量注入。
- Gateway 建立七条 `lb://` 路由、JWT 验证、白名单、TraceId 和可信用户头注入；Schedule 不建立外部路由。
- Article 通过 OpenFeign 和服务名调用 User 内部 Ping；没有服务实例 IP、端口硬编码。
- 新增 `docs/SERVICE_FOUNDATION.md`、`docs/NACOS_CONFIG.md`、`docs/GATEWAY_DESIGN.md`。

### 已验证

- Nacos 容器为 healthy；Nacos 服务列表显示 `leadnews-gateway`、`leadnews-user-service`、`leadnews-article-service` 三个健康注册实例。
- Gateway、User、Article 的 `/actuator/health` 均返回 `UP`。
- Gateway 缺失 Token、伪造 Token、过期 Token 均返回 HTTP 401 和脱敏统一错误。
- 客户端伪造 `X-User-Id: 999` 时，下游实际读取 JWT 用户 `42`，证明不信任外部用户头。
- Gateway→Article→Feign→User 调用返回 HTTP 200；两级服务用户 ID 均为 `42`，TraceId 全链路一致。
- Gateway 的 `/internal/**` 返回 404；直接调用 User 内部端点但不携带内部标记时返回拒绝结果。
- JDK 21 下 `mvn clean verify` 全 Reactor 成功；基础单元测试 7 个全部通过。

### 问题与解决方案

- Gateway 首次启动时发现公共自动配置的方法签名直接引用 Spring MVC，导致 WebFlux 类路径反射失败。已将 Servlet 自动配置拆为独立、条件化自动配置，保持 Gateway 纯 WebFlux。
- Spring Cloud Gateway 2025.x 将路由属性迁移到 `spring.cloud.gateway.server.webflux.routes`，已使用新属性消除迁移告警。
- Spring MVC 不继承 Feign 接口的类型级路径映射。User 实现端显式声明 `/internal/users/ping`，Feign 契约仍集中在 `leadnews-feign-api`。

### 尚未完成

- 未实现真实登录/注册、Token 刷新与权限模型；留待用户业务阶段。
- 未实现数据库、Redis、Kafka、Elasticsearch、MinIO、XXL-Job 或 AI 业务接入。
- 内部 Header 当前是开发期调用边界，生产阶段仍需网络隔离或服务身份认证。

## 下一步

- 阶段 3 已完成并停止，不自动进入阶段 4。

## 阶段 4 暂停点（可恢复，未完成）

> 本暂停点已于 2026-09-05 按恢复顺序完成，最终结论见文末“阶段 4 完成记录”。以下内容保留为恢复审计记录。

### 当前完成

- 已只读参考原项目核心表/Entity，保留用户、自媒体、正式文章/内容分表、素材关联和后台审核的领域边界；未修改原项目。
- User、Article、Wemedia、Admin 四个模块接入 MyBatis-Plus、MySQL、Flyway、BCrypt 和审计字段；common/model/feign-api/Gateway 完成阶段 4 所需的上下文、DTO、契约和登录白名单调整。
- 已创建 migration：User V1/V2、Article V1、Wemedia V1/V2/V3、Admin V1/V2。
- 已创建表：`ap_user`；`ap_channel`、`ap_article`、`ap_article_content`；`wm_user`、`wm_news`、`wm_material`、`wm_news_material`；`ad_user`、`ad_audit_record`。
- 已实现 User 登录/资料；Wemedia 登录、草稿 CRUD/分页/提交/素材查询；Article 频道读取、文章分页/详情、内部正式文章创建；Admin 登录、待审/详情/通过/拒绝、频道管理。
- 已实现对应 Entity、Mapper、Application Service，以及 `WemediaAuditClient`、`ArticleInternalClient`；Controller 不直接调用 Mapper。
- 已实现 DRAFT/REJECTED 可编辑、提交至 MANUAL_REVIEW、人工 APPROVED/REJECTED 的显式状态流转；CAS 状态更新、`wm_news_id` 唯一键和重复审核返回用于并发与幂等。
- 已增加 BCrypt/JWT、状态机、Gateway 用户类型透传测试源码。本暂停点未扩展测试范围。

### 构建、启动与验证状态

- 2026-09-04 使用 Temurin JDK `21.0.12.1` 执行相关八模块 `-DskipTests compile`，结果 `BUILD SUCCESS`。
- 暂停前 Gateway 51601、User 51801、Article 51802、Wemedia 51803、Admin 51807 均曾健康为 `UP`，并验证 App/Wemedia/Admin 登录、错误密码 401 和创建草稿成功。
- 当前运行进程 PID：Gateway 6600、User 48248、Article 62448、Wemedia 55960、Admin 54796。它们是修正乐观锁前的 package 产物；Docker 基础设施未停止。

### 已知错误与未完成

- 草稿 update 曾因 MyBatis-Plus 缺少 `OptimisticLockerInnerInterceptor` 抛出 `MP_OPTLOCK_VERSION_ORIGINAL`。已删除 Wemedia Mapper 中间继承并在四个数据库服务配置乐观锁插件，源码编译成功，**尚待重新 package/start 后回归**。
- 尚未完成草稿全流程、所有权/提交后编辑拒绝、审核拒绝、审核通过、正式文章查询、重复审核幂等、频道 CRUD/过滤、并发 CAS 和数据库结果的真实验证。
- 尚未完成阶段 4 的数据库设计、状态机、API 文档与 Nacos datasource 文档更新；尚未运行最终全量 `clean verify`。
- 数据库已有暂停前 E2E 创建的草稿；不要假设空库。详细账号、API、事务边界、文件清单和恢复步骤见 `docs/STAGE4_CHECKPOINT.md`。

### 下次继续建议

1. 先按 checkpoint 第一条命令复核 JDK 21 编译。
2. 只停止 checkpoint 明确列出的五个任务 Java PID，重新 package/restart。
3. 第一项运行回归必须是“新建草稿 → update”，确认乐观锁修复。
4. 再完成闭环/并发/数据库/Nacos 验证，补文档，最后执行 JDK 21 全量 `clean verify`。
5. 不重复创建表、migration、账号或 Mapper 基类，不进入阶段 5。

## 阶段 4 完成记录（2026-09-05）

### 已完成

- User、Wemedia、Article、Admin 四个核心域已完成 MyBatis-Plus/MySQL/Flyway 持久化接入，Controller、Application Service、Mapper 分层明确。
- 已执行且成功的 migration 为：User V1/V2、Article V1、Wemedia V1/V2/V3、Admin V1/V2；现有已执行 migration 未被改写。
- 已创建 `ap_user`；`ap_channel`、`ap_article`、`ap_article_content`；`wm_user`、`wm_news`、`wm_material`、`wm_news_material`；`ad_user`、`ad_audit_record`。
- 已完成 App 用户登录/资料、自媒体登录及草稿 CRUD/分页/提交/素材查询、频道与正式文章读取、管理员登录/待审/详情/通过/驳回及频道管理 API。
- 已完成 `WemediaAuditClient`、`ArticleInternalClient`；内部频道状态契约采用 POST 以兼容当前默认 Feign HTTP client，外部 Admin API 保持 PATCH。
- 已完成 DRAFT/REJECTED 可编辑、提交至 MANUAL_REVIEW、人工 APPROVED/REJECTED 状态机；审核 CAS、正式文章 `wm_news_id` 唯一键和重复通过返回同一 articleId 共同保证收敛与幂等。
- 已新增 `docs/DATABASE_DESIGN.md`、`docs/ARTICLE_STATE_MACHINE.md`、`docs/API.md`，并更新 `docs/NACOS_CONFIG.md`。

### 已验证

- 以 Temurin JDK `21.0.12.1` 和 Maven Wrapper `3.9.10` 执行 `clean verify`，13 个 Reactor 模块全部 `BUILD SUCCESS`；已有自动化测试全部通过。
- Gateway `51601`、User `51801`、Article `51802`、Wemedia `51803`、Admin `51807` 最终启动后 `/actuator/health` 均为 `UP`。
- 真实端到端验证覆盖：登录与错误凭据、用户资料读写、草稿新增/查询/更新/分页/删除、所有权隔离、提交后禁止编辑、待审列表/详情、必填驳回原因及落库、审核通过、正式文章及内容读取、重复通过幂等、频道 CRUD/禁用过滤、DTO 校验和并发通过/驳回。
- 最终并发复测：稿件 `wm_news.id=9` 的 approve 返回 HTTP 200，竞争 reject 返回 HTTP 409，最终为 APPROVED，并创建唯一正式文章 `articleId=3`。
- 数据库核验确认已审核稿件仅对应一条正式文章，内容分表和审核记录正确；四个数据库服务的 Flyway history 均成功。
- Nacos 已发布并核验 User/Article/Wemedia/Admin DataId 的 datasource 约定，密码使用 `${DB_PASSWORD}` 环境变量占位；五个服务均在 Nacos 注册。
- Docker 基础设施最终均 healthy；未停止、重建或清理容器及 Volume。
- 排除日志、构建产物和原项目分析文档后，新项目未发现 `192.168.x.x` 或原项目绝对路径硬编码；原项目 Git 工作树保持 clean。

### 问题与解决方案

- 草稿更新的 `MP_OPTLOCK_VERSION_ORIGINAL` 错误通过为四个持久化服务注册 `OptimisticLockerInnerInterceptor`、让 Mapper 直接继承 `BaseMapper<T>` 修复，运行回归通过。
- 默认 Feign/JDK HTTP client 不支持 PATCH，内部频道状态调用改为 POST；对外语义化 PATCH 不变。
- Feign 并发状态冲突原先被统一映射为 502；全局异常处理现保留下游 409/403/404 语义，最终并发测试确认冲突返回 409。

### 当前遗留与边界

- 阶段 4 无阻塞遗留项；测试产生的数据保留在本地开发数据库中，未通过删除数据伪造空库。
- Admin → Wemedia → Article 仍是同步 Feign 和各服务本地事务，不是分布式事务；后续可设计 outbox/补偿，但不属于本阶段。
- 静态化/MinIO、消息事件/Kafka、缓存/Redis、搜索/Elasticsearch、AI 审核、前端均未提前实现。
- 阶段 4 至此停止，不自动进入阶段 5。

## 阶段 5：文章发布链路、Freemarker、MinIO 与动态定时发布（已完成，2026-09-05）

### 已完成

- 发布状态机已扩展为 `APPROVED/WAITING → PUBLISHING → PUBLISHED`，失败进入 `PUBLISH_FAILED`；通过数据库 CAS 获取发布权，保存重试次数、开始时间、发布时间及脱敏错误信息。
- Article 使用 Freemarker 生成 UTF-8 HTML，文本与图片属性均转义；MinIO 使用固定 bucket `leadnews` 和确定性对象键 `article/{articleId}/index.html`，重复发布覆盖同一对象，不产生重复文件。
- Admin 审核通过后：到期稿件同步触发发布；未来稿件写入 `leadnews_schedule.schedule_task` 并加入 Redis ZSet `schedule:publish:future`。
- Schedule 以 MySQL 为事实源，Redis 仅作时间索引；启动时会从 WAITING/READY/FAILED 恢复索引，任务用数据库 CAS 抢占并有限重试。
- Article 增加 `articlePublishCompensation` XXL-Job handler，仅用于周期补偿失败和超时发布，不为每篇文章创建 XXL-Job。
- Gateway 增加公开静态页代理 `/static/article/**`；响应支持 `Cache-Control: public, max-age=300`、ETag 和 `If-None-Match` 304。
- Wemedia 增加素材上传/删除：按文件魔数识别 JPEG/PNG/GIF/WebP、限制大小、生成安全对象键；已被稿件引用的素材删除返回 409。
- 新增 Article Flyway `V2__add_publish_fields.sql`；新增 Schedule Flyway `V1__create_schedule_task.sql`。未改写阶段 4 已执行 migration。
- 新增并更新 `ARTICLE_PUBLISH_FLOW.md`、`MINIO_DESIGN.md`、`SCHEDULE_DESIGN.md`、`PUBLISH_CONSISTENCY.md`、`DATABASE_DESIGN.md`、`API.md`、`NACOS_CONFIG.md`。

### 真实验证

- 立即发布：稿件 10 创建文章 4，最终 `PUBLISHED`；生成 `article/4/index.html`，文本脚本标签被转义、图片正常渲染。重复审核返回同一 articleId 且 `created=false`。
- 定时发布：稿件 11/12 分别创建文章 5/6、任务 1/2；到期前静态页 404，到期后均发布成功。任务 2 在停止 Schedule、仅移除对应 Redis member 后重启，仍由 MySQL 恢复并成功发布。
- 故障与恢复：测试中仅短暂停止项目 MinIO，文章 7 首次进入 `PUBLISH_FAILED` 且 retry_count=1；恢复 MinIO 后重复审核成功发布。将该测试记录模拟为超时 `PUBLISHING` 后也能被重新 claim 并收敛至 `PUBLISHED`。MinIO 已恢复 healthy。
- HTTP 缓存：静态页返回 200、`Cache-Control: public, max-age=300` 和 ETag；携带匹配 ETag 返回 304。
- 素材安全：伪装 MIME/危险文件名的真实 PNG 被按魔数保存为生成的 `.png` 键；HTML 字节伪装 PNG 返回 400；未引用素材可删除，已引用素材返回 409。
- XXL-Job：修复 Windows 默认日志目录无权限问题后，以匹配管理端的 token 启动执行器，`leadnews-article-publish` 注册返回 `code=200, msg=Success`；测试 Article 进程随后已停止。
- Nacos 已发布并核验 Article、Wemedia、Schedule 的 dev 配置，敏感信息均使用环境变量占位。
- Article/Schedule Flyway history 均为 success；最终文章 4–7 均为 PUBLISHED，任务 1/2 均为 SUCCESS。
- Temurin JDK `21.0.12.1`、Maven Wrapper `3.9.10` 执行 `clean verify`，13 个 Reactor 模块全部 `BUILD SUCCESS`，已有测试全部通过。
- 原项目 Git 工作树保持 clean；排除日志、构建产物及分析文档后，新项目无旧虚拟机地址和原项目绝对路径硬编码。

### 问题、处理与阶段边界

- XXL-Job 在 Windows 首次启用时尝试写入 `D:\\data` 并失败；已增加 `XXL_JOB_EXECUTOR_LOG_PATH`，默认使用项目相对目录。
- XXL-Job Admin 与执行器 token 不一致会拒绝注册；`.env.example` 现提供非敏感占位，实际部署必须在两端设置相同值。
- 当前发布一致性采用本地事务、唯一键、CAS、幂等对象键、有限重试和补偿实现最终一致；不是跨服务分布式事务。
- 阶段 5 无阻塞遗留项。Docker 基础设施保持运行，阶段验证启动的 Java 服务已停止；不进入阶段 6。

## 阶段 6：Redis Lua + Kafka 高频行为异步化（已完成，2026-09-05）

### 已完成

- 只读分析原 Behavior/点赞/阅读/收藏/Kafka Streams 方案，结论与改造理由见 `docs/BEHAVIOR_DESIGN.md`；原项目未修改。
- 新增统一 `BehaviorType` 与 `BehaviorEvent(eventId,articleId,userId,behaviorType,delta,occurredAt,traceId)`；预留收藏/评论枚举但未提前实现。
- Redis Key 集中在 common：点赞 Set `article:like:user:{articleId}`、计数 Hash `article:counter:{articleId}`、单用户初始化标记及 pending Hash `behavior:event:pending`。
- LIKE/UNLIKE/VIEW 使用 Lua；点赞状态、非负计数和 pending 事件原子更新。API 只读取 Gateway 注入的 APP_USER userId。
- Producer 使用 Topic `leadnews.behavior.events`、acks=all、idempotent producer、有限 retry；LIKE/UNLIKE key 为 `articleId:userId`，VIEW key 为 articleId。
- Redis pending outbox 在 Kafka ACK 后删除，定时任务重发遗留事件；Consumer 使用 `behavior_event_log.event_id` 唯一键实现数据库最终幂等。
- 新增 `article_behavior_stat`、`article_like`、`behavior_event_log`、`behavior_failed_event`，Flyway V1 执行成功；V2 增加 VIEW 可恢复批量聚合状态与索引。
- Consumer 异常不吞掉：固定 1 秒、最多 2 次重试，耗尽后进入 `leadnews.behavior.events.DLT` 并写失败审计表。
- VIEW 不逐消息更新统计表：事件先持久化，1 秒事务批量锁定最多 1000 条，按文章聚合更新后精确标记已应用。
- Article 详情融合实时计数和 liked；Redis 不可用时 Behavior 从 MySQL 降级，Article 仍可用并标记 `behaviorRealtime=false`。
- 增加缺失 Key 的并发安全预热和最小 reconciliation；不会用 MySQL 旧值覆盖已存在的 Redis 实时增量。
- 新增 `docs/BEHAVIOR_DESIGN.md`、`docs/KAFKA_DESIGN.md`、`docs/REDIS_DESIGN.md`，更新数据库、API、Nacos 文档。

### 真实验证

- 基础链路：article 4 的 LIKE、重复 LIKE、VIEW、UNLIKE 返回计数 `1/1/1/0`，重复 LIKE `changed=false`；MySQL 最终 like=0、view=1，事件日志只有 LIKE/VIEW/UNLIKE 三条。
- 同用户并发：100 个并发 LIKE 只有 1 个 `changed=true`、最终 +1；100 个并发 UNLIKE 只有 1 个 `changed=true`、最终 -1。
- 多用户并发：50 个不同用户并发点赞 article 6，Redis 最终 50；MySQL `like_count=50`、active article_like=50、事件=50。
- 重复消费：相同 `duplicate-stage6-event` 真实发送两次，`behavior_event_log` 仅 1 条，article 4 viewCount 只从 1 增至 2。
- Retry/DLT：发送本阶段不支持的 COLLECT 事件后主事务回滚，有限重试耗尽进入 DLT，`behavior_failed_event` 有 1 条，主 event log 为 0。
- Producer 故障补偿：短暂停止项目 Kafka 后 VIEW 已由 Lua 接受，pending=1；Kafka 恢复 healthy 后 pending=0，MySQL viewCount 从 2 收敛至 3。
- Redis 降级：短暂停止项目 Redis 后 LIKE 返回 HTTP 503/业务码 50300；Article 详情继续返回 MySQL like=0、view=3、`behaviorRealtime=false`。Redis 恢复 healthy 后删除单个测试 counter Key，下一次详情自动预热并返回 `behaviorRealtime=true`。
- VIEW 批量持久化复测：并发发送 10 条 article 5 浏览事件，Flyway V2 成功；批处理后 MySQL view_count=10、已应用日志=10、未应用日志=0。
- 未登录行为请求经 Gateway 返回 HTTP 401；Host Java Producer/Consumer 与 Docker Kafka 的 LIKE/UNLIKE/VIEW 流全部成功。
- Nacos `leadnews-behavior-service-dev.yml` 已发布，数据库/Redis/Kafka 地址与凭据均为环境变量占位。
- Temurin JDK `21.0.12.1`、Maven Wrapper `3.9.10` 最终执行 `clean verify`，13 个 Reactor 模块全部 `BUILD SUCCESS`（2026-09-05 20:26:58，2 分 10 秒）；新增 Behavior 3 个测试及既有测试全部通过。

### 问题与边界

- Redis 故障首次测试暴露写行为被映射为 500；已将完整 Redis 操作纳入故障边界，并将 503xx 业务码映射到 HTTP 503，复测通过。
- Redis 重建可能丢失点赞 Set；已增加单用户 MySQL 状态 Lua 预热，并让 MySQL 统计只在点赞关系真实迁移时变化，避免重复加减。
- 本阶段没有实现 Kafka Streams 热点、热榜 ZSet、Elasticsearch、AI、DFA、Sentinel、XXL 热点任务或 Vue。
- Docker Kafka/Redis 均已恢复 healthy，未删除 Volume；验证产生的数据保留用于审计。
- 原项目 Git 工作树保持 clean；排除构建产物、日志和分析文档后，新项目无旧虚拟机地址或原项目绝对路径硬编码。阶段 6 至此停止，不进入阶段 7。

## 阶段 7：Elasticsearch 全文检索、联想与历史（已完成，2026-09-07）

### 已完成

- 使用 Elasticsearch Java API Client `8.17.10` 对齐服务器 `8.17.10`，未使用已淘汰的 RestHighLevelClient。
- 创建版本化索引 `leadnews_article_v1`、`leadnews_suggestion_v1` 及稳定 alias；initializer 仅在不存在时创建，不在启动时删除。
- 文章检索支持 title^3/summary^2/content^1 权重、channelId 过滤、相关性/时间排序、分页边界及 title/content 安全高亮。
- 联想使用 Elasticsearch edge-ngram，来源为已发布文章 title/labels，最多 10 条；未引入 MongoDB 或 wildcard 扫描。
- 新增 Article V3 `article_search_sync` outbox；发布完成状态与 UPSERT outbox 在同一 MySQL 事务提交。事件只包含 eventId/articleId/eventType/occurredAt/traceId，Kafka key 为 articleId。
- Search Consumer 通过 OpenFeign 回查 Article 搜索文档，以 articleId 为 `_id` 幂等 UPSERT/DELETE；有限重试后进入 `leadnews.article.search.DLT` 并写 `search_sync_failure`。
- 新增 Search V1 migration：`search_history` 与 `search_sync_failure`；历史按用户/关键词唯一、更新最近时间、最多 20 条，并进行所有权删除。
- 新增分页 Bulk 全量重建入口；Gateway 仅精确匿名放行文章检索和联想，匿名历史仍为 401，公开端点携带 Token 时仍解析用户身份。
- Nacos 已发布 Search DataId（ES/Kafka/datasource/分页环境变量占位）；新增 `SEARCH_DESIGN.md`、`ELASTICSEARCH_DESIGN.md` 并更新 API/Kafka/Database/Nacos 文档。

### 已验证

- 初次重建从 MySQL 读取 7 篇 PUBLISHED 文章并写入两个索引；alias、mapping、文档数正常。
- 真实 Kafka 测试覆盖重复 UPSERT、DELETE（ES `_id=1` 返回 404）和恢复 UPSERT（返回 200），未产生重复文档。
- 搜索验证 title 命中得分高于仅 content 命中；TIME 排序、channelId、分页上限、`<em>` 高亮均通过。HTML encoder 将正文中的 script 标签转义为 `&lt;script&gt;`。
- 联想前缀返回已发布文章标题。用户 101 重复搜索只保留一条历史，用户 202 不可见；匿名搜索 200、匿名历史 401。
- 短暂停止仅 Elasticsearch 时搜索返回 HTTP 503、Article health 保持 200；ES 恢复 healthy 后搜索自动恢复 200。
- Temurin JDK `21.0.12.1` 与 Maven Wrapper `3.9.10` 执行 `clean verify`，13 个 Reactor 模块全部 `BUILD SUCCESS`，现有测试全部通过。

### 问题与边界

- Elasticsearch 容器未安装 IK，本阶段采用 standard analyzer；中文可按单字召回但语义相关性有限。后续若引入与 8.17.10 精确匹配的 IK，必须新建 v2 索引并通过 alias 切换。
- outbox 自动重试上限为 10，长期失败由 DLT 审计和全量 rebuild 恢复；Search 故障不会回滚已发布文章。
- 未进入阶段 8，未实现 AI、前端、DFA、热榜或新的业务域。

## 阶段 8：Kafka Streams + Redis ZSet + XXL-Job 热点计算（已完成，2026-09-07）

### 已完成

- 只读分析原项目热点实现，保留窗口聚合、频道榜和离线纠偏，移除完整 JSON List 缓存与重复公式；详见 `docs/HOT_ARTICLE_DESIGN.md`。
- common 新增唯一 `HotScoreCalculator`：VIEW/LIKE/COMMENT/COLLECT 默认权重 1/5/8/10，加入 `(hours+2)^0.35` 时间衰减；实时与离线共用。
- Behavior 引入 Kafka Streams 3.9.2（由 Boot 依赖管理），application-id `leadnews-hot-score-stream-v1`；5 秒窗口 suppress 后输出稳定 `HotScoreRefreshEvent` 到 `leadnews.hot.refresh`。
- refresh Consumer 使用独立 group、有限重试和 DLT；读取阶段 6 Redis counter 后执行绝对 ZADD，不重复累计行为；V3 新增 `hot_score_refresh_log`。
- Redis 使用 global/channel ZSet 和轻量 meta Hash；发布后写元数据，下架清理钩子 ZREM，查询顺带清理 stale ID。
- `hotArticleRebuildJob` 以 MySQL `article_behavior_stat` 为事实源，通过临时 Key + RENAME 重建并补齐历史 meta。
- Article 新增匿名热点 API、最大 50 的内部批量接口和离线元数据分页；保持 Redis 排名顺序，无 N+1。Redis 不可用或空榜时返回最新 PUBLISHED 且 `degraded=true`。
- 已更新 HOT_ARTICLE_DESIGN、KAFKA_DESIGN、REDIS_DESIGN、API、NACOS_CONFIG 文档和 Gateway 白名单。

### 已验证

- 单元测试固定时间验证权重、衰减、负计数防御；TopologyTestDriver 验证 100 条同窗口 VIEW 只输出 1 条 refresh。
- 真实 Docker Kafka 输入 100 条 article 4 VIEW + 窗口推进事件，对应窗口只输出 1 条 refresh；refresh log 为 SUCCESS。
- Redis `article:counter:4.viewCount` 测试前后均为 10，证明阶段 8 未二次增加阶段 6 计数；全局 ZSet 得到绝对 score。
- XXL 执行器 `leadnews-hot-rebuild` 注册成功，handler `hotArticleRebuildJob` 调用返回 `code=200, msg=Success`。删除 global 榜后从 MySQL 重建 7 篇 PUBLISHED 文章及频道榜，未残留 rebuild Key。
- Redis 短暂停机时热点 API HTTP 200、`degraded=true`；恢复后容器 healthy、PING=PONG、Behavior health=UP。
- 频道榜 Top3 顺序与全局榜一致；注入的 stale articleId 被查询流程清理。
- Temurin JDK `21.0.12.1`、Maven Wrapper `3.9.10` 执行全量 `clean verify`，13 个 Reactor 模块全部 `BUILD SUCCESS`（2026-09-07 20:51:30，2 分 46 秒）；新增公式与 Streams 测试及既有测试全部通过。
- Nacos `leadnews-behavior-service-dev.yml` 已发布阶段 8 hot/Streams 配置，凭据仍为环境变量占位。

### 问题与解决方案

- 自定义 String KafkaTemplate 会使 Boot 不再自动创建 BehaviorEvent Template；已显式定义两个强类型 Template，保持阶段 6 JSON Producer 与阶段 8 String Streams 输出互不干扰。
- Behavior 初次启用 Article Feign 时缺少 load-balancer；已补最小 OpenFeign/LoadBalancer 依赖。
- XXL 自动探测的 Windows 地址不可供 Docker 回调；本地验证用 `XXL_JOB_EXECUTOR_ADDRESS=http://host.docker.internal:9998`，并与 Admin 现有 token 对齐。源码未写入 token。
- Windows 对 Kafka state.dir 的 POSIX 权限变更会记录无害告警；目录位于可写 JVM temp、Streams 实际已进入 RUNNING。需要固定目录时设置 `HOT_STREAM_STATE_DIR`，不得放入源码树。

### 边界

- COMMENT/COLLECT 权重已预留，但未为展示热点而提前实现评论/收藏业务。
- Redis 与 MySQL 不做分布式事务；绝对刷新、稳定 refreshId、离线重建实现最终收敛。
- 阶段 8 验证产生的行为日志和统计保留作审计；未删除 Docker Volume。验证用 Java 服务在最终记录后停止，Docker 基础设施保持运行。
- 阶段 8 至此停止，不进入阶段 9。

## 阶段 9：Spring AI + Qwen AI 基础服务（已完成，2026-09-08）

### 已完成

- 固定 Spring AI `1.1.2`、Spring AI Alibaba `1.1.2.2`、模型 `qwen3.8-max`，未升级阶段 0~8 技术基线。
- 建立 `Controller -> AiApplicationService -> AiModelClient -> SpringAiQwenModelClient -> ChatModel`；Feign 只暴露通用 chat/structured/vision，SSE 由 AI Service 独立提供。
- Prompt 移入 `resources/prompts`；`AiProperties` 集中管理模型、温度、token、timeout/retry、stream、图片和并发上限。
- Structured Output 使用 `BeanOutputConverter` schema + Jakarta Validation；解析失败统一 502，不存在默认 PASS。
- Vision 支持 jpeg/png/webp 的 bytes 或 allowlist URL，默认单图 5 MiB；限制协议/host，业务层不依赖 Provider Media 类型。
- 同步 call timeout 默认 30 秒、stream 60 秒；瞬时连接/429/5xx 额外重试 1 次，400/认证不重试；Semaphore 默认 4 并发。
- Key 只读取 `API-KEY`；缺失时服务可启动、capability `NOT_CONFIGURED`、调用 503。日志只记录元数据、长度和 nullable usage。
- Nacos 已发布 `leadnews-ai-service-dev.yml`，只含 `${API-KEY:}` 占位；`.env.example` 仅含示例占位。

### 已验证

- 8 组单元测试通过：AiProperties、AiModelClient、StructuredOutput、OutputParseFailure、TimeoutMapping、RetryPolicy、SecretRedaction、Bulkhead；均未调用真实模型。
- AI Service 在 51808 启动并注册 Nacos，health HTTP 200/UP。
- Text：真实 `qwen3.8-max` 成功，latency 2922 ms，requestId 可取，totalTokens 93。
- Structured：真实 DTO 反序列化成功，五个字段均通过验证。
- Vision：Spring AI Alibaba `DashScopeChatModel` + `multi-model=true` + PNG bytes 真实成功，latency 932 ms，totalTokens 149；未发生兼容问题、未启用 fallback。
- Streaming：真实收到 44 个 SSE data 事件（41 个非空 chunk），first token 1009 ms，total 3453 ms。
- 缺失 `API-KEY` 的独立进程真实验证：服务 health HTTP 200，模型调用 HTTP 503；没有假响应。
- Temurin JDK `21.0.12.1`、Maven Wrapper `3.9.10` 执行全量 `clean verify`，13 个 Reactor 模块全部 `BUILD SUCCESS`（2026-09-08 11:22:35，3 分 20 秒）；AI 模块 9 个测试全部通过。
- 最终安全扫描：源码、配置、文档及日志中真实 Key 命中 0，`sk-` 形态命中 0；运行时代码/配置中旧虚拟机 IP 与原项目绝对路径命中 0。历史分析文档仍保留原项目 IP 事实作为只读迁移依据。
- 原项目 Git 工作树 clean；最终已停止本阶段启动的 51808/51809 Java 进程，Docker 基础设施未停止。

### 问题与边界

- 首次真实请求受宿主执行沙箱阻止外网 socket；在获准的外网执行上下文重启同一 AI Java 服务后成功，证明不是 endpoint、区域、Key 或 Adapter 错误。
- 未创建 `ai_call_log` 表，阶段 9 采用不含正文/Key 的结构化日志，避免过度设计。
- 未修改 WmNews 状态，未创建正式审核记录，未生成摘要，未提供正式续写业务接口；阶段 9 到此停止，不进入阶段 10。

## 阶段 10：DFA + Qwen 多模态审核 + 人工兜底（已完成，2026-09-08）

### 已完成

- 引入可恢复数据库审核任务与 Kafka 异步消费；提交事务递增 `audit_version` 并写任务，Consumer 以 eventId、唯一键和版本 CAS 防重复、防旧结果覆盖。
- 新增 DFA trie 前置审核；命中直接拒绝且不调用 AI。未命中时提取正文文本，并把稿件关联素材 URL 解析为可信 MinIO objectKey。
- AI Service 新增正式结构化多模态文章审核，模型固定 `qwen3.8-max`；PASS/REJECT 阈值分别为 0.85/0.90，其余及任何失败均转人工。
- `wm_news_audit_record` 统一记录 DFA、AI 和人工轨迹；详情返回版本、来源和轨迹。人工通过继续复用阶段 5 即时/定时发布。
- 增加 Micrometer 审核结果/失败计数和 AI latency；日志不记录正文、Prompt、图片、Key 或完整 Provider 响应。
- Nacos 已发布 Wemedia/AI 阶段 10 配置，Kafka 已创建审核 Topic 和 DLT；新增 `docs/ARTICLE_AUDIT_FLOW.md` 并更新相关设计文档。

### 已验证

- 模块测试覆盖 DFA 命中短路、AI PASS/失败、决策阈值、重复事件和旧版本 CAS；AI 模块 11 个测试通过。
- 真实 DFA 稿件 15 被拒绝，AI 调用次数未增加；真实文本稿件 18 自动通过并发布。
- 真实图片稿件 19 从 MinIO 可信 objectKey 读取字节，Qwen 返回 PASS/LOW、confidence=1.0，最终 PUBLISHED。
- 稿件 20 版本 2 的 MinIO objectKey 不存在，安全转 `MANUAL_REVIEW`；人工驳回、重提及人工通过发布均成功，轨迹保留两个版本。
- Kafka 重放完成事件和旧版本事件后，状态及记录数不变；并发提交稿件 21 一次 200、一次 409，仅生成一个任务。
- Windows 精确 `API-KEY` 子进程继承已验证；增加启动期精确读取适配后，正式接口真实调用 Qwen 成功且未泄露密钥。
- Temurin JDK `21.0.12.1`、Maven Wrapper `3.9.10` 执行全量 `clean verify`，13 个 Reactor 模块全部 `BUILD SUCCESS`（2026-09-08 13:04:11，3 分 27 秒）。

### 问题与边界

- 运行验证发现直接把 `.env` 原变量传给 Java 不等同于服务配置所需的 `DB_PASSWORD/MINIO_ACCESS_KEY/MINIO_SECRET_KEY`；验收进程使用显式内存映射，源码及 Nacos 仍只保留占位符。
- 原 Admin `ad_audit_record` 作为兼容数据保留；阶段 10 的权威审核轨迹为 `wm_news_audit_record`。
- 测试数据与审核记录保留用于审计；Docker 基础设施不停止。阶段 10 完成后停止，不进入阶段 11。

## 阶段 11：文章 AI 摘要 + 自媒体 SSE 流式续写（已完成，2026-09-08）

### 已完成

- Article 发布后创建独立 `article_ai_summary_task`/outbox，发送 `ArticlePublishedEvent` 到 `leadnews.article.published`，真实调用 `qwen3.8-max` 后按 `summaryVersion` CAS 回写。
- 实现摘要状态、有限重试、过期 RUNNING 恢复、XXL retry/backfill handler 和分页内部 backfill；任何摘要失败均不回滚 PUBLISHED。
- Wemedia 新增自有 DRAFT/REJECTED 稿件 SSE 续写，事件为 meta/chunk/done/error；真实 Spring AI streaming，不保存、不提交、不审核、不发布。
- 完成 Article V4 migration、四个 Prompt、DTO/Feign、指标、安全日志、Nacos 配置和两份专项设计文档。
- 验收发现 Kafka 3.9 拒绝 `delivery.timeout.ms=15000 < request.timeout.ms=30000`；已修正为 30000 并同步 Nacos。

### 真实验证

- 文章 13 始终 PUBLISHED，真实摘要成功（139 字、`qwen3.8-max`）。AI 故障时为 `PUBLISHED|FAILED`，服务恢复并重置测试时序耗尽的重试额度后收敛为 `PUBLISHED|SUCCESS`。
- 同一 event 重放两次后任务仍 1 条、摘要不变；旧 version 1 事件在当前 version 2 时标记 STALE，未覆盖新版本。backfill 随后生成新版本并成功；一次实际 enqueue 8 篇历史文章。
- SSE 实际 38 chunks + done，总时延 4.03 秒，首 Token 494~805 ms；调用前后稿件内容一致。
- 匿名 401、他人稿件 403、PUBLISHED 409、越界输入 400。客户端中断退出码 28；AI Flux 收到 cancel（首 Token 468 ms、1074 ms），后续请求 34 chunks + done 成功。
- 阶段 11 模块测试：Article 5、Wemedia 14、AI 14，连同公共测试均通过。最终全量 clean verify 结果记录于阶段最终验收。

### 边界

- 摘要任务与发布事务故意隔离，极端 enqueue 缺失由 backfill 补偿；没有引入分布式事务。
- 续写输出仍是不可信普通文本，未来富文本编辑器必须继续转义。本阶段未进入前端或阶段 12。

### 阶段最终验收

- 使用 Temurin JDK `21.0.12.1`、Maven Wrapper `3.9.10` 执行 `clean verify`，13 个 Reactor 模块全部 `BUILD SUCCESS`（2026-09-08 20:01:23，总计 3 分 44 秒），全部现有测试通过。
- 前两次 clean 因仍存活的本项目 Gateway/Article 验收进程锁定 Windows JAR 而中止；核对命令行后只停止对应项目进程，第三次全量成功。最终 51601/51802/51803/51807/51808 均无本阶段 Java 监听。
- 精确 API Key、`sk-` 密钥形态、旧 `192.168.x.x` 和原项目绝对路径运行代码扫描均为 0；原项目 Git 工作树 clean。
- MySQL、Redis、Nacos、Kafka、Elasticsearch、MinIO、XXL-Job Admin 最终均 healthy；未停止或清理 Docker 容器、镜像、Volume。阶段 11 完成并停止，不进入阶段 12。
## 阶段 12：Sentinel 流量治理（已完成）

- 基于 Alibaba 2025.0.0.0 BOM 接入 Sentinel 1.8.9、Gateway v6x Adapter 与 Nacos datasource；未升级现有技术栈。
- Gateway 完成 Route/API Group 分级限流、统一 429 JSON、TraceId、Block 日志及低基数指标；Article 完成 `articleDetail` paramIdx=0 热点流控。
- 三份规则发布至 Nacos public / LEADNEWS_GROUP，并保留无凭据模板。动态阈值实测 Gateway PID 不变：30 并发从 5/25（200/429）变为 20/10。
- 热点隔离：articleId=13 为 5/25，articleId=12 同窗为 5/0；Behavior 20 次为 8/12且 Redis 增量=8；Search 20 次为 5/15并恢复 200。
- SSE：真实 qwen3.8-max 为 meta=1、chunk=16、done=1；ai-api=0 时 5/5 为 429、AI 调用计数=0，随后恢复 QPS=1。
- JWT 401、Header 清洗、internal 404 均保持；Nacos 短暂连接异常期间服务保持 UP并在恢复后重订阅。未部署非官方 Dashboard 镜像。
- 新增 Gateway Block/顺序/secret 测试与 Article 注解、Block、参数隔离测试。最终全量构建和安全扫描见收尾记录。
- 2026-09-09 使用 Temurin JDK 21.0.12.1、Maven Wrapper 3.9.10 执行 `clean verify`：13/13 Reactor 模块 `BUILD SUCCESS`（4 分 04 秒），全部测试通过。本任务 Java 进程已停止，Docker 核心容器保持 healthy；运行代码/配置的 `sk-` 密钥形态、旧 `192.168.x.x`、原项目绝对路径扫描均为 0，原项目 Git clean。
## 阶段 13：Vue 3 前端重写（暂停于依赖安装验证，2026-09-09）

### 已完成

- 建立 pnpm workspace 与 `leadnews-app`、`leadnews-wemedia`、`leadnews-admin`、`packages/shared`，固定 Vue 3.5.42、Router 5.3.1、Pinia 4.0.3、Axios 1.20.0、Element Plus 2.14.5、Vite 7.3.1、TypeScript 5.9.3、Vitest 3.2.4。
- 13A 公共源码：Gateway base URL、Bearer JWT、Pinia 持久化/类型守卫、HTTP 状态差异化、TraceId、共享 DTO、状态映射、安全高亮、POST SSE parser。
- 13B 用户端源码：登录、频道/文章/热点、详情/AI 摘要、一次性浏览、点赞、搜索/排序/联想竞态取消/历史。
- 13C 创作端源码：登录、稿件列表/块编辑、素材、审核状态/轨迹、定时发布输入、真实 Fetch SSE 续写/取消/建议插入。
- 13D 管理端源码：登录、人工审核列表/详情/轨迹、通过确认、拒绝原因、频道 CRUD/启停。
- 已新增 FRONTEND_DESIGN、FRONTEND_RUN 和 STAGE13_CHECKPOINT；未改后端、Docker 或原项目。

### 已验证

- Node 24.20.0、npm 11.19.0、pnpm 11.19.0 可用；官方版本元数据查询成功。
- `pnpm install --lockfile-only --offline --no-optional` 成功，最终 lockfile 与 manifests 一致。
- 静态扫描未发现 `192.168.x.x`、API Key/数据库/MinIO 密钥或最终 Mock；原项目 Git 工作树 clean。

### 暂停原因与未完成

- 宿主网络下载 `@esbuild/win32-x64@0.27.7` 持续 code 23/timeout，依赖实体未完整安装；因此 typecheck、Vitest、三个 production build、dev server、浏览器和真实 Gateway 业务验收均尚未执行，不能标记阶段完成。
- 恢复首命令、已完成文件、禁止重复事项及后续验收顺序详见 `docs/STAGE13_CHECKPOINT.md`。阶段 13 当前暂停，不进入阶段 14。
# 阶段 13 第二次暂停点（2026-09-09，已恢复完成）

阶段 13 已解除依赖安装阻塞并完成三前端 typecheck、shared 测试（5/5）和 production build。真实 Gateway API 基础联调通过，401 已验证。浏览器首次联调发现 Gateway CORS 预检 403，已把三个开发环境改为同源 `/api` + Vite 代理到 Gateway；该修改尚待重新编译与浏览器复验。5173 被用户现有程序占用，未关闭，本次用户端临时使用 5176。三个 Vite 与本任务启动的八个 Java 进程已经停止，Docker 基础设施保持运行。详细恢复顺序见 `docs/STAGE13_CHECKPOINT.md` 顶部的“第二次安全暂停点”。阶段 13 尚未完成，禁止进入阶段 14。

## 阶段 13 完成记录（2026-09-10）

- pnpm 固定 `11.19.0`，仅批准 esbuild `0.27.7` 安装脚本；TypeScript 恢复为 registry 固定版本 `5.9.3`，移除临时 vendor tarball。
- 三个 Vue SPA 与 shared workspace 已完成；所有业务请求只经 `VITE_API_BASE_URL` 和 Gateway。开发环境采用同源 `/api` + Vite proxy，修复 Gateway CORS 预检 403；SSE 同时修复根基址双斜杠问题。
- 最终执行 `corepack pnpm -r typecheck`、shared Vitest 5/5、`corepack pnpm -r build`，全部成功；仅有 Element Plus 整包导致的 chunk size 非阻塞警告。
- 浏览器真实验证用户端登录、频道、文章列表/详情、浏览数、AI 摘要、搜索、高亮、联想/历史、热点；自媒体登录、概览、稿件状态/详情、素材、真实 Qwen SSE、AbortController 取消；管理端登录、待审列表、审核详情/轨迹、通过/拒绝入口与频道管理。
- SSE 接口真实返回 24 个 chunk 和 1 个 done；取消状态为“已取消”，完成状态为“完成”。调用前后稿件正文一致，续写不会自动保存。
- 状态验证：匿名 401、跨角色 403、已发布稿件修改 409、Behavior 突发 20 次得到 8 个 200/12 个 429、受限上游时 SSE 50320 且编辑内容不受影响；前端对五类状态使用不同文案，Like/View 仅在成功响应后更新本地状态。
- 最终源码与 dist 扫描未发现 API Key、数据库/MinIO 密钥、旧 VM IP、业务服务 518xx 直连或最终 Mock。原项目 Git 工作树 clean，始终只读。
- 宿主 5173/5174 已被用户 EchoMind 占用，未停止；验收临时使用 5176/5177/5179。项目默认端口仍为 5173/5174/5175。
- 阶段 13 完成并停止，不进入阶段 14。
- 最终收尾已停止本任务启动的八个 Java 服务和全部 Vite/Node 进程；Docker 基础设施保留运行。临时 JWT 文件已删除。

## 阶段 14：Windows JVM 本地运行与前端 Nginx 验收（已完成，2026-09-10）

### 架构与工程化

- 最终拓扑固定为：七项基础设施运行 Docker Desktop；Gateway + 八个业务服务运行 Windows JDK 21/IDEA；三个 Vue SPA 使用 Vite 开发，production dist 使用独立 Nginx 容器验收。没有创建 Java Dockerfile，也没有把 Java 服务加入 Compose。
- 新增 `scripts/common.ps1`、infra/backend/frontend start/stop/check 脚本与 SSE smoke；PID/启动时间/JAR/端口记录在忽略提交的 `.stage14-runtime`，日志写入 `.stage14-logs`。停止脚本只终止校验匹配的本项目进程。
- 新增 `docker-compose.frontend.yml`、固定 `nginx:1.28.0-alpine`、三个 SPA 的 history fallback、入口与 hash 资产差异缓存、统一 Gateway 反代、SSE 禁止 buffering/cache/gzip。
- `.env.example` 不再提供 `API-KEY` 文件化入口；AI Service 的 MinIO secret 默认值已移除，只允许环境变量注入。真实 `.env` 保持忽略。
- 新增 `LOCAL_DEVELOPMENT.md`、`DEPLOYMENT_TOPOLOGY.md`、`NGINX_DESIGN.md`，更新 README、FRONTEND_RUN、DOCKER_SETUP、FAILURE_SCENARIOS。

### 构建与运行验收

- Temurin `21.0.12.1`、Maven Wrapper `3.9.10` 执行 `clean verify`：13/13 Reactor 模块 `BUILD SUCCESS`（2026-09-10 19:28:34，总计 4 分 55 秒）。
- pnpm `11.19.0`、esbuild `0.27.7`、TypeScript `5.9.3`；三个前端 typecheck、shared Vitest 5/5、三个 production build 全部成功，仅有非阻塞 chunk-size 警告。
- 九个 Windows Java 服务 Actuator 全部 UP，Nacos `public / LEADNEWS_GROUP` 实际注册 9/9，实例地址为 `127.0.0.1`。
- 基础设施检查全部 READY；两份 Compose `config --quiet` 成功。Nginx health 为 healthy，18080/18081/18082 根路径和 history fallback 均 200；index no-cache，hash assets 一年 immutable。
- 5173/5174 被用户 EchoMind 占用且未停止，阶段验收使用 Vite 5176/5177/5179；项目默认端口不变。

### 真实链路

- Vite 与 Nginx 均经 Gateway 完成用户登录、频道、文章列表/详情、摘要、搜索和热点 smoke；本轮另完成 View、Like/Unlike、联想、历史，均返回成功。
- Wemedia/Admin 登录、稿件/待审/频道列表成功；本轮实际得到匿名 401、跨角色 403、PUBLISHED 编辑 409。阶段 13 已真实验证 Behavior 20 次 8/12（200/429）及 AI 503，本阶段不重复压力或故障注入。
- Vite SSE：34 chunks、done=1、首块 2262 ms、总计 3804 ms；Nginx SSE：23 chunks、done=1、首块 757 ms、总计 2456 ms。两者取消均成功，调用前后稿件正文一致。
- 异步收敛只读抽样：article 13 Redis/MySQL 均为 like=0、view=27，行为日志 31；搜索 SENT outbox 待处理 0；摘要 SUCCESS 8；ES 文档 13 存在；全局热点 ZSet 8。Kafka 通过 `kafka:29092` 可列出 behavior/search/published/audit/hot 及 DLT/Streams Topics。

### 安全与边界

- 前端源码/dist 不含旧 VM IP、业务 518xx 直连或最终 Mock；TypeScript 依赖为 registry 固定版本。历史 checkpoint 中的旧路径/vendor 描述只作审计记录。
- 精确 `.env` 值扫描只命中明确标注为本地开发默认值的 `.env.example`；AI 源码中的 MinIO 默认 secret 已移除。运行时凭据没有写入脚本、Compose、Java、前端或文档。
- 原项目始终只读。最终收尾停止本阶段 Java、Vite 与 Nginx；Docker 基础设施保留。阶段 14 完成后停止，不进入阶段 15。

## 阶段 15：性能压测、故障注入与可靠性验证（已完成，2026-09-10）

- 新增无第三方依赖的 Node fetch 压测、登录、Behavior toggle、SSE concurrency/cancel、JVM sampler 与安全服务重启脚本；JSON/CSV 原始结果位于 `performance/results`。
- 记录真实机器、Docker、JDK/JVM、端口和 Sentinel 基线。Article/Search 均执行 30 秒预热 + 60 秒正式正常区间与 Sentinel 饱和观察；遇到高比例 429 后停止增加并发。
- stage15 专用 article 14 上完成同用户/不同身份 LIKE、VIEW、LIKE/UNLIKE 并发，Redis/MySQL/event log 完全一致；100 并发已有 92% 429，未继续 200。
- Kafka lag 最终为 0；单次行为到热点分值可见约 1.23 秒且 counter 只 +1。AI SSE 只做 1/2/4 温和并发，取消与 permit 释放成功，429 未进入 Provider。
- 真实执行 Redis、ES、Kafka、AI、MinIO 故障及 Behavior/Article/Search/Schedule 重启；结果与恢复数据见 `PERFORMANCE_TEST.md` 和 `FAILURE_SCENARIOS.md`。
- 发现两个限制：SSE 非法参数在内容协商下返回 500；AI 摘要耗尽最大重试后恢复需全量 backfill，缺少 articleId 精确入口。为避免多余付费模型调用，本阶段未直接改数据库或触发 9 篇全量摘要。
- JVM 72 样本全部 Full GC=0；MySQL 关键 Article detail SQL 索引命中。没有证据支持业务调参或加索引，因此没有性能优化 before/after。
- 新增 `docs/PERFORMANCE_TEST.md`、`docs/RESUME_METRICS.md` 并更新故障文档。本阶段最终 Maven/JDK21 构建、安全扫描与进程收尾结果记录于最终验收。
- 最终使用 Temurin JDK 21.0.12.1、Maven Wrapper 3.9.10 执行 `clean verify`，13/13 Reactor 模块全部 `BUILD SUCCESS`（2026-09-10 21:07:26，总计 3 分 49 秒）。本阶段 Java 进程已停止，Docker 基础设施保留运行；原项目 Git clean。阶段 15 完成并停止，不进入阶段 16。

## 阶段 15.5：可靠性缺口收尾（已完成，2026-09-11）

- 已定位并修复 SSE 错误放大根因：`text/event-stream` 请求中的业务/校验异常不再交给 JSON converter，而由仅匹配该媒体类型的专用 advice 返回真实 HTTP 状态和带统一业务码、traceId 的 SSE `error` frame。
- 已增加受可信内部标记保护、且 Gateway 不暴露的 `POST /internal/articles/{articleId}/summary/retry`。它沿用当前 summaryVersion 和任务唯一键，通过文章与任务双 CAS 精确重开，重复/并发请求幂等，不扫描其他文章。
- 新增续写错误契约和摘要精确恢复测试；针对 Article/Wemedia 及依赖模块测试分别为 13/13、19/19 通过。JDK 21 针对性 compile/package 成功。
- 真实 Gateway 非法 targetLength=80 已返回 HTTP 400、业务码 40000 和 traceId；AI Provider 请求指标未创建，调用数为 0。
- 经授权完成稿件 22 合法 Qwen 续写：HTTP 200、29 chunks、done=1、首块 2424 ms、总计 3568 ms，Provider 指标为 1，数据库正文哈希不变。
- 文章 17 首次精确恢复真实观察到 `PENDING -> GENERATING -> FAILED`，既有状态机自动尝试 3 次，12/14 始终未变化。根因是该测试文章正文过短，固定 80 字摘要下限与不虚构事实冲突，AI Service 返回 50210。已增加统一的短文自适应下限及测试，针对 Article/AI 测试 13/13、15/15 通过。
- 经追加授权执行最后一轮文章 17 精确恢复：`PENDING/PENDING/0 -> GENERATING/RUNNING/0 -> SUCCESS/SUCCESS/0`，耗时 6213 ms，Provider 操作数 1，摘要 27 字；文章 12/14 的 FAILED、summaryVersion、retryCount 均未变化，未触发 backfill 或其他文章。
- 最终使用 Temurin JDK 21.0.12.1、Maven Wrapper 3.9.10 执行 `clean verify`，13/13 Reactor 模块全部 `BUILD SUCCESS`（2026-09-11 17:44:03，总计 3 分 48 秒）。本次启动的九个 Java 服务已用项目 PID 安全停止，Docker 七项基础设施均保持 healthy。
- 未增加数据库 migration；未修改阶段 15 原始性能数据；未进入阶段 16。
- 最终扫描：运行源码/配置中旧 VM IP、原项目绝对路径、真实 API Key 均为 0；Gateway 继续显式拒绝 `/internal/**`；无主代码临时 Mock；原项目 Git clean。

## 阶段 16：项目最终收口、README、简历与面试材料（已完成，2026-09-11）

### 文档收口

- 重写根 README，面向 Java 后端面试官和 GitHub 浏览者，准确记录 Java 21 / Spring Cloud 2025 技术栈、12 个子模块、Windows JVM + Docker 基础设施拓扑、启动顺序、安全边界与真实验收结果。
- README 已加入总体架构、文章发布、高频行为、AI 审核四幅 Mermaid 图，并额外展示摘要/SSE 链路；没有把 Java 服务画成 Docker 容器。
- 新增 `PROJECT_HIGHLIGHTS.md`，按“问题、设计、理由、验证”整理 8 个真实工程亮点。
- 新增 `RESUME_PROJECT.md`，提供 100～150 字简介、6 条标准版、8 条详细版和可安全引用的指标边界。
- 新增 30 秒/1 分钟/3 分钟/5 分钟 `INTERVIEW_PROJECT_PITCH.md`，以及覆盖 Redis、Kafka、Streams、MySQL、ES、MinIO、Sentinel、AI、SSE、微服务、故障和性能的双层回答 `INTERVIEW_QA.md`。
- 新增 `INTERVIEW_DEEP_DIVE.md`（60 题，每题含核心答案和常见误区）、`INTERVIEW_STAR_CASES.md`（8 个真实 STAR + Reflection）、`ORIGINAL_VS_REBUILD.md` 和 8 条 ADR。
- 原项目已有能力与本人重构贡献已明确拆分；没有虚构评论、IK、MongoDB、RabbitMQ、Seata、Kubernetes、Sentinel Dashboard、生产集群或生产 SLA。

### 最终构建与检查

- `mvnw.cmd -version` 确认 Maven 3.9.10 实际运行在 Eclipse Temurin `21.0.12.1`。
- 2026-09-11 18:14:34 执行 `mvnw.cmd clean verify`：13/13 Reactor 模块 `BUILD SUCCESS`，总计 3 分 58 秒，全部现有测试通过。
- 前端执行 `corepack pnpm -r typecheck`：shared 与三个 Vue 应用全部成功；`corepack pnpm --filter @leadnews/shared test`：5/5 成功；`corepack pnpm -r build`：三个 production dist 全部成功。仅保留既有 Element Plus 整包 chunk-size 非阻塞警告。
- 文档/源码/资源/前端/dist/脚本/performance/Compose 扫描未发现真实 `sk-` Key、真实密码/Token、旧 VM IP、运行代码中的原项目绝对路径、前端 518xx 直连或最终 Mock；`.env` 仍被 `.gitignore` 忽略。扫描到的 `$env:*` 取值和 `.env.example` 本地开发值/占位符不是泄漏。
- 所有 Compose 镜像均固定版本，无 `latest`；前端不存在 `file:` 依赖。文档中 RabbitMQ/MongoDB/Seata 等字样仅用于原项目对照、备选方案或明确“未使用”的边界说明。
- 原项目最终 `git status --porcelain` 无输出，工作树 clean，整个阶段保持只读。

### 最终边界

- 阶段 16 未修改 Java/Vue 业务源码、数据库、Topic、Redis Key、Sentinel 规则或 AI 模型，未运行新压测和新 E2E。
- 性能数字只引用阶段 15 的 Windows 单机、localhost、短时且受 Sentinel 约束的真实报告，不外推生产能力。
- 阶段 16 完成。整个项目开发阶段结束，不自动新增阶段 17。

### 阶段 16 简历材料增强（2026-09-12）

- `RESUME_PROJECT.md` 已按“技术栈、项目简介、项目核心功能、标准/详细亮点”重新组织。
- 新增 `RESUME_CODE_WALKTHROUGH.md`：为 Gateway/限流、Redis Lua 行为、Kafka Streams 热点、审核发布、ES Outbox、AI 审核、AI 摘要、SSE 续写 8 条亮点标注真实源码位置，补充端到端调用链、状态与失败窗口、面试讲法及关键代码摘录。
- 已自动检查该文档 48 个本地链接，未发现缺失目标；本次只修改 Markdown 文档，不修改或重新构建业务代码。

### 用户端搜索联想缺陷修复（2026-09-13）

- 定位到 `SearchView.vue` 把 `el-autocomplete` 的 callback 在异步请求完成前以空数组调用，后续只更新独立 ref，导致接口已有结果但下拉框不刷新。
- 改为由 `fetchSuggestions` 自身完成 300 ms debounce、AbortController 取消、请求序号防竞态，并在真实 API 返回后调用当前 Element Plus callback。
- Gateway 实测 prefix=`阶段` 返回 3 条、prefix=`Stage` 返回 10 条；prefix=`新` 因当前 suggestion 索引无匹配数据返回空，属于数据结果而非 UI 故障。
- `corepack pnpm --filter leadnews-app typecheck` 与 `corepack pnpm --filter leadnews-app build` 均成功；仅有既有 chunk-size 非阻塞警告。

### 文章详情阅读布局与文章 18 摘要恢复（2026-09-13）

- 按实际阅读需求将详情页调整为“标题/作者 → 正文 → AI 摘要 → 点赞/评论数/浏览量”，移除需要用户额外跳转的静态阅读页入口。
- 当时评论域尚无发布 API，页面只展示后端真实 `commentCount`；随后已按用户测试反馈补齐真实评论最小闭环，见下方记录。
- 确认宿主环境存在 API Key但不输出其值。首次文章 18 精确恢复因 AI JVM 位于受限网络上下文，DashScope socket 被拒，自动尝试 3 次后 FAILED；未将其误判为 Key 或模型错误。
- 仅安全重启已记录的 `leadnews-ai-service` 到可联网运行上下文，其他八个 Java 服务未重启；随后只对文章 18 再执行一次精确恢复。
- 文章 18 最终 `PENDING → SUCCESS`，摘要为“阶段十五MinIO故障恢复专用文章，用于验证失败状态与确定性对象键。”，未执行全量 backfill。
- 用户端 `typecheck` 和 production build 再次成功，仅有既有 chunk-size 非阻塞警告。

### 用户端热门、摘要状态与静态页修复（2026-09-13）

- 核对真实数据：全部文章共 17 篇，热榜 12 篇均属于这 17 篇；首页原先只加载前 12 篇且无分页，造成较旧热点看似不在“全部”。用户端当前加载上限调整为 50，以完整展示当前数据集。
- 热榜现在展示后端真实 `score`（两位小数）并标明点赞、浏览与时间衰减口径；降级榜单不伪造热度数值。
- 文章详情补齐 AI 摘要 `PENDING/GENERATING/FAILED/未生成` 状态提示。文章 18 当前真实状态为 `FAILED`，本次未擅自调用 Qwen；已有 SUCCESS 摘要的文章继续展示摘要正文。
- 定位静态页 403 根因：数据库 `staticUrl` 指向私有 MinIO 直链。保留 Bucket 私有策略，前端改走既有 Gateway `/static/article/{id}` 受控代理；Vite 与 Nginx 均补充该路径代理，不开放 Bucket 匿名访问。
- 已验证 Gateway 与 Vite 代理访问文章 18 均返回 HTTP 200、`text/html;charset=UTF-8`，正文 410 bytes；用户端 typecheck/build 成功，仅有既有 chunk-size 警告。

### 长文章与真实评论闭环（2026-09-13）

- Article 新增 Flyway `V5__create_article_comment.sql` 与 `ap_article_comment` 表，评论保存文章、APP_USER、作者展示名、1～500 字正文、可见状态及创建/更新时间；未扩展回复、评论点赞或 AI 评论审核。
- 新增公开分页查询 `GET /api/article/{articleId}/comments` 和 APP_USER 发布 `POST /api/article/{articleId}/comments`；服务端校验文章存在且已发布，前端使用文本插值安全展示。
- 用户端详情页新增评论输入、提交、分页结果展示和真实总数；未登录时提示登录，提交成功后重新读取服务端数据，不制作本地假评论。
- Article 及依赖模块 JDK 21 compile/package 成功；`leadnews-app` typecheck 与 production build 成功。Article 服务按 PID/命令行核验后单独安全重启，Flyway V5 已应用。
- 真实创建稿件 29《从一次故障恢复看 Java 微服务的最终一致性设计》，包含六段较长技术正文；真实 Qwen 审核为 LOW/PASS 后自动发布为文章 19，AI 摘要状态 SUCCESS。
- 文章 19 静态正文经 Gateway `/static/article/19` 返回 HTTP 200、`text/html`；APP_USER 真实写入评论 ID 2，重新分页读取 total=1，确认评论已持久化而非 Mock。
- 热门区原先直接显示衰减公式产生的两位浮点分数，并被阶段 15 压测文章 6 的 50 次点赞主导。保留真实历史数据、不擅自清库；用户端改为明确排名、整数化“热度指数”和前 10 名展示，文案明确当前口径为点赞、浏览与发布时间衰减。

### 点赞行为一致性重构（2026-09-17）

- 完整检查 Behavior Service 的 HTTP、Redis Lua、pending Producer、补偿、Kafka Consumer、MySQL 表和原有测试；设计与完整链路见 `docs/BEHAVIOR_CONSISTENCY.md`。
- Article LIKE/UNLIKE Lua 原子更新关系、计数、关系版本并写入完整 pending JSON；Consumer 使用新的 `(consumer_group,event_id)` 幂等表、`article_like.relation_version` 与文章统计行锁处理乱序。未误用用户关系版本作为文章总计数版本。
- Flyway V4 已真实应用。`consumed_event` 默认保留 30 天，每天最多 10×500 行分批清理。
- JDK 21 全量 `mvnw.cmd verify` 成功。新增 Consumer、Producer pending 和清理单元测试；原 Kafka Streams 测试通过。
- 真实文章 19、演示用户执行点赞→取消→点赞→取消；Redis HTTP 结果依次 `true/false/true/false`，计数 `1/0/1/0`；MySQL `relation_version=4,status=0,like_count=0`，`consumed_event` 4 条，pending 长度 0，Flyway V4 success=1。测试后已恢复最初未点赞状态。
- 尚未进行真实 Kafka 乱序注入或真实数据库失败注入；相应单元测试验证了版本判断与异常传播，但不能替代跨进程故障演练。V4 之前无版本消息需要迁移前检查 lag/pending，见设计文档。

### Kafka Streams 实时热点重构（2026-09-17）

- 按新增要求将旧“窗口刷新信号 → refresh Consumer 读绝对计数 → ZADD”替换为“BehaviorEvent.occurredAt → 5 秒窗口 + 10 秒 grace → 显式 State Store `hot-score-delta-window-v2` → suppress 最终 deltaScore → Redis Lua ZINCRBY”。Streams application-id 保持 `leadnews-hot-score-stream-v1`。
- 删除运行时旧 `HotScoreRefreshEvent`、Consumer、refresh Topic 配置与对应 Bean；历史 Kafka Topic、Flyway V3 表不自动删除。common 中的旧 `HotScoreCalculator` 不再被运行时注入。
- 新增原子热点增量/衰减 Lua、活跃频道 Set、每分钟 ×0.99 衰减及分钟级多实例 NX 锁。新增 Flyway V5 `hot_score_checkpoint`，每 30 分钟保存分数；XXL handler 现仅从 checkpoint 恢复空榜，不再按累计行为总数覆盖增量热度。
- TopologyTestDriver 已验证多事件聚合为一次最终分数、文章隔离、正负权重、事件时间 grace 内迟到、超过 grace 丢弃和中间 Window Store 状态；Extractor 单测验证 Kafka 延迟仍按 occurredAt 分窗。Behavior 模块及依赖模块测试通过。
- JDK 21 全量 `mvnw.cmd clean verify` 已通过。七个 Docker 基础设施健康，九个 Java 服务重新启动；Behavior Streams `leadnews-hot-score-stream-v1` 转为 `RUNNING`。Flyway V5 已成功应用。
- 在隔离 Redis 测试 Key 上真实执行增量和衰减 Lua：100 分衰减为 99 分，低于 1 分的成员从全局/频道榜移除；测试 Key 已删除。真实文章 19 的点赞事件经 Kafka 窗口输出后，全局/频道分数为 2.97（3 分增量经过一次 0.99 衰减），证明 Producer→Streams→Lua→Redis 链路；取消点赞后演示用户最终为未点赞。窗口关闭需后续事件推进 stream time，不能以墙上时钟 15 秒保证自动输出。
- Redis 外部副作用不受 Kafka exactly-once_v2 保护，极端重放可能轻微漂移。当前 checkpoint 将同一文章的全局与频道分数保存为一份；新算法正常情况下二者同步，但旧榜迁移期若二者原本不同，恢复后的数值可能近似，见 `docs/HOT_ARTICLE_DESIGN.md`。
- 文档收口：README 与 `docs/RESUME_CODE_WALKTHROUGH.md` 已改为新链路；JDK 21 再次执行 `mvnw.cmd -q -pl leadnews-behavior-service -am test`，退出码 0。原项目 `git status --porcelain` 无输出。尚未做真实跨进程崩溃/Redis 故障注入，不能据此宣称外部副作用 exactly-once；MySQL checkpoint 恢复也未做破坏性真实演练。

### 热点窗口 Redis 幂等与频道聚合收口（2026-09-18）

- `BehaviorEvent` 增加 `channelId`，点赞 Lua 的 pending 事件与 Java 投递事件、浏览事件均携带同一频道 ID；先读现有热点 metadata，缺失时通过 Article 内部批量接口补查已发布文章，窗口关闭时不再查库。现有 Topic、Streams application-id、状态目录、独立 MySQL 消费者未改。
- 热度权重从 Topology 抽到 `HotScoreDeltaCalculator`。窗口 Store 值改为 JSON Serde 的 `HotScoreAggregate(channelId,deltaScore)`；旧 V2 Store 值是 Double，故新 Store 显式命名 `hot-score-window-v3`，避免不兼容 changelog 反序列化。最终 `HotScoreWindowResult` 的 ID 为 `articleId:windowStart:windowEnd`。
- `hot-increment.lua` 原子检查 `hot:article:applied` ZSet、更新 global/channel 榜、记录窗口 ID，重复返回 0；`HotScoreAppliedCleaner` 默认 7 天保留并定时按 windowEnd 清理。现有每分钟冷却与半小时 MySQL checkpoint 保持独立。
- TopologyTestDriver 覆盖窗口聚合、频道、负反馈、事件时间与 grace、suppress、窗口 ID；Mockito 覆盖 Redis 首次/重复/失败和清理调用。真实 Redis 单机使用隔离 Key 验证 EVALSHA：A 首次=1、重复=0 且分数保持 10；B→A 乱序得到 30，负增量后为 29；按 score 清理 3 条中旧 2 条、保留新 1 条。隔离 Key 已删除。
- JDK 21 全量 `mvnw.cmd -q clean verify` 退出码 0。构建前项目脚本仅停止匹配 PID/可执行文件/启动时间的九个本项目 Java 进程，构建后九服务均恢复 `UP`；Behavior Streams 已创建 V3 changelog 并转为 `RUNNING`。修改单测 mock 后，Behavior 及依赖模块测试复跑退出码 0。Docker 未重建/删除，原项目只读且 Git 状态无输出。
- 上线边界：启动前 Redis pending 长度实查为 0；重启后 `leadnews-hot-score-stream-v1` 在 Behavior Topic 三分区 lag 均为 0，故此次迁移没有待消费旧事件。历史 Kafka 数据仍可能不含 channelId，若未来人为重置 offset，新 Topology 会计入 malformed 并跳过。7 天后才重放旧窗口、Redis applied 与榜单不一致、以及 Redis 衰减分钟边界，仍可能使派生热度轻微漂移。当前尚未做非空 State Store 的真实宕机恢复演练或 Redis Cluster 验证；项目 Compose 使用单机 Redis，跨 slot 迁移须先改同槽 Key。
- 最后补充 `HotScoreAggregate` JSON Serde 往返测试并复跑 `mvnw.cmd -q -pl leadnews-behavior-service -am test`，退出码 0；这验证序列化兼容性，但不等同于非空 Kafka changelog 跨进程恢复演练。

### 点赞/收藏冷却防刷与 heatDelta（2026-09-18）

- `BehaviorEvent` 增加 `heatDelta`，收藏事件还携带 `collected/collectCount`。点赞/收藏共用现有 Behavior Topic、Redis pending/outbox 和现有 Kafka Streams V3 窗口架构；Streams 对这两类事件直接聚合事件自带 `heatDelta`，不查询 Redis 判断首赞。
- 旧点赞 Set 迁为 `like:state:{articleId}` Hash（1 有效、2 已取消冷却中、nil 无状态）+ `like:cooldown:{articleId}` ZSet（member=userId，score=到期毫秒）。收藏采用对应的 `collect:state`/`collect:cooldown`；低频有界旧 Set 扫描迁移任务只负责排空旧数据，新写入不再进入旧 Set。请求 Lua 单用户 O(1)，过期判断与状态/计数/版本/pending 在脚本内原子完成。
- 冷却默认均 1 小时，可通过 `leadnews.behavior.like-cooldown/collect-cooldown` 配置。点赞首次 +2、取消 -1、冷却内再次 +1；收藏首次 +5、取消 -2、冷却内再次 +2。独立原子清理 Lua 通过活跃文章 ZSet 有界遍历，只有状态仍为 2 才删 Hash field；未使用 HEXPIRE 或整篇文章 EXPIRE。
- 原项目不存在收藏行为接口/关系表，本次在同一 Behavior 模块增加 POST/DELETE `/api/behavior/articles/{articleId}/collect`、Flyway V6 `article_collect` 及版本化 MySQL Consumer 分支。MySQL 真实实现是 relationVersion + 状态差额修正计数，**不是**请求所假设的 countVersion 全量覆盖；未借热度改写计数语义，`heatDelta` 不进入 MySQL 计数。
- 隔离 Redis 真实 Lua 测试：点赞 `+2,-1,+1,-1,+1,-1` 净 +1，模拟 1 小时后再赞 +2；收藏 `+5,-2,+2`；清理过期 ZSet 项不删除已重新收藏的 state=1。测试 Key 已删除。事件 pending JSON/Kafka 重发、Streams 使用事件热度、收藏 MySQL 版本分支均有单元测试。
- JDK 21 全量 `mvnw.cmd -q clean verify` 退出码 0。九个项目 Java 服务已由精确 PID 脚本重启并全部 UP；Flyway 日志确认 behavior schema v6，Streams 回到 RUNNING。Docker 未重建或删除，原项目 Git 状态无输出。尚未执行真实用户 API→Kafka→MySQL→Streams 跨进程联调；当前运行服务可用于下一步验收。
- 发现原 `minScore=1` 会把 1 分历史吸引值在下一分钟衰减为 0.99 时立刻删除，因此将默认阈值改为 0.01，以满足“1→0.99→0.9801”自然衰减；此项改动需随后重新构建并重启 Behavior 服务。
- minScore 修正后再次执行 JDK 21 全量 `clean verify`，退出码 0。首次恢复时旧 AI 进程端口 51808 尚未完全释放，新 AI 进程因端口占用退出；待端口释放后仅重启缺失的 AI 服务，最终 51801–51808、51601 九个健康端点全部 UP。Behavior Flyway 保持 v6、Streams 正常运行；Docker 未操作。
- 最终修复旧点赞 `initialized` 标记已存在时的 Set→Hash 惰性迁移顺序；Behavior 及依赖模块 `test package` 退出码 0，仅精确重启 Behavior 服务。最终九个健康端点全部 UP，Streams `REBALANCING→RUNNING`，运行代码未发现 HEXPIRE/HGETALL/SMEMBERS/KEYS *，原项目 Git 状态无输出。

### HotScore 快照恢复与业务计数全量同步（2026-09-19）

- 沿用 Behavior Service、现有 Behavior Topic、Kafka Streams V3 Store、热点 ZSet 与衰减任务，没有引入平行架构或 Kafka 历史重放系统。
- Redis Lua 在点赞/收藏状态变更时原子递增文章级 `countVersion`；`BehaviorEvent` 携带最终关系状态、最终业务 count、独立 `relationVersion/countVersion` 与 `heatDelta`。MySQL Consumer 继续以 `(consumer_group,event_id)` 本地事务去重，并分别使用两个版本做 CAS；like/collect count 改为绝对值覆盖，`heatDelta` 不参与数据库计数。
- Flyway V7 新增完整 HotScore 快照批次表与明细表，并为 `article_behavior_stat` 增加 like/collect count version。快照默认每 30 分钟保存当前有效榜单分数及四类业务计数；只有 COMPLETE 批次可恢复。
- 恢复使用“最近完整快照 + 当前 MySQL count 差值 + 0.99 时间衰减”。正向 like/collect 按 +2/+5，负向按 -1/-2；旧分数衰减完整时间，估算增量按区间中点衰减。该结果明确为派生榜单的近似值，不改变核心业务数据一致性。
- 恢复期间以 Redis flag 将 Streams 新增量旁路到 delta ZSet，快照结果写临时榜，最终由单次 Lua 合并 delta、裁剪并切换正式 key，避免旧 ZADD 覆盖并发实时热度；当前实现面向项目既有单机 Redis，迁移 Cluster 前需解决跨 slot。
- 新增恢复正向/负向权重、中点衰减、完整快照字段、MySQL Consumer 绝对计数版本覆盖等测试；Behavior 本轮 25 个测试全部通过。2026-09-19 12:48 使用 Temurin JDK 21 与 Maven Wrapper 3.9.10 执行 `clean verify`，13/13 Reactor 模块 `BUILD SUCCESS`，总计 3 分 39 秒。
- 九个 Windows Java 服务重新启动并全部 UP；Behavior 启动后 Flyway V7 `success=1`，两张快照表与 like/collect countVersion 列已在实库确认。首次定时快照生成 COMPLETE 批次，保存 14 篇热点文章完整分数与四类 count。
- Kafka Streams 消费组 `leadnews-hot-score-stream-v1` 为 Stable、1 member，Behavior Topic 三个分区 lag 均为 0。隔离 Redis 已真实验证恢复期间增量先进入 delta 榜、切换后 global/channel 均从 100 合并为 103、临时 key 清理；未清空或破坏正式热榜做恢复演练。
- 最终运行源码/配置扫描未发现旧 VM IP、原项目绝对路径或密钥形态；原项目 `git status --porcelain` 无输出，始终只读。Docker 基础设施未停止、重建或删除。

### 自动审核任务可靠性收口（2026-09-19）

- 复用现有 `wm_news_audit_task` 作为任务事实与 Kafka 投递前可靠存储，没有增加 audit outbox，也没有改写 DFA、Qwen 客户端或审核决策阈值。
- 文章提交事务改为直接 `AUDITING + auditVersion+1` 并创建 `PENDING/attemptNo=0/dispatchStatus=PENDING` AuditTask；任务插入异常继续向外抛出，由同一 MySQL 本地事务回滚文章更新。
- Wemedia Flyway V5 将投递状态从任务生命周期拆开，新增 attemptNo、dispatchStatus、dispatchRetryCount、nextDispatchTime、lastDispatchedAt、startedAt、finishedAt，并为审核记录补充 attemptNo；既有 eventId 与 `(newsId,auditVersion)` 唯一约束保持。
- Consumer 不再查询后用 Java 判断任务状态，也不 INSERT 任务；Mapper 单条 SQL 按 eventId/newsId/auditVersion/PENDING/maxAttempts CAS 到 RUNNING 并原子 `attemptNo+1`。返回的 attemptNo 贯穿 DFA/AI、最终落库、主动失败重试和僵尸恢复。
- 已知基础设施异常在同一任务上 CAS 回 PENDING 并重置 dispatchStatus；RUNNING 默认 90 秒超时、每 30 秒恢复扫描，最大自动审核 attempt 为 3。耗尽后任务 FAILED、文章 MANUAL_REVIEW、审核记录保存失败原因。
- 最终 DFA/AI/人工兜底结果在本地事务中按 `Task RUNNING + attemptNo` 和 `Article auditVersion + AUDITING` 双重保护，原子更新 Task、Article 并写 audit_record；旧 attempt 更新 0 行，旧 auditVersion 任务进入 STALE。
- 新增提交事务、迁移约束、双线程 claim、重复消息、attempt 递增、僵尸恢复、旧 attempt、旧 auditVersion、最大次数转人工和结果事务失败传播测试。Wemedia 本轮 30 个测试通过。
- 2026-09-19 17:17 使用 Temurin JDK 21 与 Maven Wrapper 3.9.10 执行 `clean verify`，13/13 Reactor 模块 `BUILD SUCCESS`，总计 3 分 21 秒。九个 Windows Java 服务重启后全部 UP。
- 实库确认 Wemedia Flyway V5 `success=1`；两个唯一索引和七个新增/重命名任务字段均存在。历史 18 个任务全部迁移为 `SUCCESS/SENT`，没有遗留 SENT 执行状态或 PENDING/RUNNING 僵尸任务。原项目 Git 状态无输出，始终只读。
- 审核 Consumer Group `leadnews-wemedia-audit-v1` 已恢复运行，Topic 三个分区 lag 均为 0；本轮未额外调用 Qwen，也未创建新的审核业务数据。

### 自动审核多实例、图片与 AI 韧性补强（2026-09-19）

- 在上一轮 AuditTask/CAS/fencing 基础上增量实现：MySQL 敏感词全局版本、Redis Pub/Sub 版本广播、30 秒数据库版本兜底、AtomicReference 不可变 DFA 快照切换，并在审核记录中保存 sensitiveWordVersion。
- AI Service 的 MinIO 图片读取改为逐张有界读取、关闭流、最长边 1280 缩放、JPEG 0.8 起步有限压缩；增加单图/总图/Base64 请求估算上限，最多 5 张压缩图仍为一次多模态请求。
- 增加 `AiAuditResultValidator` 业务组合校验、CSV 阈值离线校准入口，以及 Wemedia 审核侧 Semaphore(4) 与 CLOSED/OPEN/HALF_OPEN 轻量熔断器。capacity busy 与 circuit open 在真正调用 AI 前把任务延期并回退该次 attempt 占用；人工积压默认上限 500。
- 新增 Flyway V6：敏感词表、全局版本表、audit_record 敏感词版本字段。详细 45 项实现映射与边界见 `docs/AUDIT_RELIABILITY_REPORT.md`。
- 使用 Temurin JDK 21.0.12.1 与 Maven Wrapper 3.9.10 执行全量 `clean verify`，13/13 Reactor 模块 `BUILD SUCCESS`（2026-09-19 19:01，总计 3 分 31 秒）；Wemedia 38 项、AI 15 项测试全部通过。
- 构建前仅按项目 PID 记录精确停止九个本项目 Java 进程，构建后全部恢复 UP，Nacos 中九服务均注册。Wemedia 启动日志确认 Flyway V6 成功、DFA 初始化为 version=1；Docker 未停止、重建或删除。
- 最终扫描新增审核源码/配置/文档未发现旧 `192.168.x.x`、原项目绝对路径或 `sk-` 密钥形态；原项目 `git status --porcelain` 无输出，保持只读 clean。本轮没有发起真实 Qwen 调用。
- 补充真实 ImageIO 缩放压缩测试后，JDK 21 复跑 AI 模块及依赖测试退出码 0；AI 测试数增至 16，验证 2000x1000 PNG 生成 1280x640、≤512KB 的审核 JPEG 且原始字节未被修改。
- 对同一审核可靠性需求做第二轮差异复核：修复 `AiCallExecutor` timeout 分支未遵守 maxRetries 的缺口，现在 timeout/连接/适合重试的 5xx 均为首次加最多一次；新增测试确认 timeout 最多执行 2 次。上传端增加默认 2000 万像素头部校验，写 MinIO 前拒绝尺寸异常或无法验证的图片；审核拉取层新增超过 5 张时不触碰 MinIO 的防御测试。
- 最终使用 Temurin JDK 21.0.12.1 执行 `mvnw.cmd -q clean verify`，退出码 0；Wemedia 39 项、AI 18 项测试均为 0 failure/0 error。九个项目 Java 服务按 PID 脚本恢复后全部 UP，Nacos 九服务注册正常；Docker 未操作，原项目 Git clean，安全扫描无旧 VM IP、原项目路径或 API Key 形态。
