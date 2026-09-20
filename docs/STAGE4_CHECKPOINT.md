# 阶段 4 暂停点（2026-09-04）

> 状态：**已关闭**。该 checkpoint 已于 2026-09-05 严格按第 9 节顺序恢复并完成阶段 4。最终全量 JDK 21 `clean verify` 成功，端到端与并发验证通过；完成详情见 `PROGRESS.md` 的“阶段 4 完成记录”。本文以下内容保留为当时状态和恢复审计依据，不再作为待办清单。

## 1. 阶段原始目标与完成度

阶段 4 的目标是恢复用户、自媒体、文章、后台管理四个核心业务域，以 MyBatis-Plus、MySQL 和 Flyway 建立持久化，并形成“登录 → 草稿 → 提交 → 人工审核 → 正式文章 → 用户读取”的最小闭环。静态化/MinIO、AI 审核、ES、Kafka、Redis 和前端均不属于本阶段。

当前估计完成约 **75%**：模型、迁移、Mapper、主要 Service/API、Feign 契约及 JWT 登录已落地且源码可编译；真实闭环验证进行到“创建草稿后更新”，暴露并修正了乐观锁插件缺失，但修正后的 Jar 尚未重新打包和运行验证。拒绝流程、频道 CRUD、并发流转、数据库结果及全量测试仍未完成。

## 2. 已修改/创建文件

既有文件已修改：根 `pom.xml`；四个数据库服务及 common/model/feign-api 的 `pom.xml`；四个服务的 `application-dev.yml` 和 Application 类；Gateway 的 JWT Filter、配置及测试；common 的错误码、自动配置、UserContext、TraceConstants、请求上下文与异常处理；Article 原有 Application/Ping 基础文件。

本阶段创建的主要文件（按模块）：

- `leadnews-common`: `persistence/BaseEntity.java`、`persistence/AuditMetaObjectHandler.java`、`security/AuthenticationFoundationTest.java`。
- `leadnews-model`: `ArticleContentItemDTO.java`、`ArticleCreatedResponse.java`、`ChannelDTO.java`、`CreateArticleCommand.java`、`PageResponse.java`、`WmNewsSnapshot.java`。
- `leadnews-feign-api`: `article/ArticleInternalClient.java`、`wemedia/WemediaAuditClient.java`。
- `leadnews-user-service`: `domain/ApUser.java`、`mapper/ApUserMapper.java`、`service/UserAccountService.java`、`web/UserAccountController.java`、`config/UserSecurityConfiguration.java`，以及 2 个 migration。
- `leadnews-article-service`: `domain/{Channel,Article,ArticleContent}.java`、对应 3 个 Mapper、`ArticleApplicationService.java`、`ChannelService.java`、`ArticleController.java`、`InternalArticleController.java`、`ArticlePersistenceConfiguration.java`，以及 1 个 migration。
- `leadnews-wemedia-service`: `domain/{WmUser,WmNews,WmMaterial,WmNewsMaterial,WmNewsStatus}.java`、对应 4 个 Mapper、`WemediaService.java`、两个 Controller、`WemediaConfiguration.java`、`WmNewsStatusTest.java`，以及 3 个 migration。曾创建的 `WemediaMappers.java` 已因 Mapper 中间继承丢失参数注解而删除，四个公开 Mapper 现直接继承 `BaseMapper<T>`。
- `leadnews-admin-service`: `domain/{AdminUser,AuditRecord}.java`、对应 Mapper、`AdminApplicationService.java`、`AdminController.java`、`AdminConfiguration.java`，以及 2 个 migration。

## 3. 数据库当前状态

Flyway 已在 Docker MySQL `localhost:3307` 上执行。数据库及表：

- `leadnews_user`: `ap_user`。
- `leadnews_article`: `ap_channel`、`ap_article`、`ap_article_content`。
- `leadnews_wemedia`: `wm_user`、`wm_news`、`wm_material`、`wm_news_material`。
- `leadnews_admin`: `ad_user`、`ad_audit_record`。

所有表使用 InnoDB/utf8mb4；核心实体包含时间、version、逻辑删除字段，业务唯一性由数据库索引保障。服务间不建立物理外键。Migration 清单：User V1/V2；Article V1；Wemedia V1/V2/V3；Admin V1/V2。V2 将演示账号更新为 BCrypt 的 `password` 哈希；Wemedia V3 增加第二个所有权测试账号。数据库中已有至少两个由未完成 E2E 创建的草稿记录，未清理，继续验证时应创建新记录而非假设空库。

## 4. Nacos 与运行状态

Nacos/Docker 基础设施保持运行，未被本次暂停操作停止。四个新数据库服务能使用 `NACOS_SERVER_ADDR=localhost:8848` 注册；阶段 4 尚未把 datasource 内容正式发布/核验到各 Nacos DataId，当前由本地 `application-dev.yml` 和环境变量工作。

暂停记录时本任务产生的 Java 服务仍在运行：Gateway `51601`（PID 6600）、User `51801`（PID 48248）、Article `51802`（PID 62448）、Wemedia `51803`（PID 55960）、Admin `51807`（PID 54796），健康接口此前均为 `UP`。这些进程运行的是最后一次 package 产物，**不包含暂停前刚编译的全部乐观锁配置修正**；继续时应先停止这五个 PID，再重新 package/restart，不能据此认定修复已运行验证。其他 Java 进程不是本任务所有，禁止操作。

## 5. 本地测试账号

仅限本地开发：

- App：手机号 `13800000000`，密码 `password`。
- Wemedia 主账号：`wemedia_demo`，密码 `password`。
- Wemedia 第二账号：`wemedia_other`，密码 `password`。
- Admin：`admin_demo`，密码 `password`。

账号密码仅为 migration 的非敏感本地演示值。真实凭据没有写入源码；数据库连接密码仍通过 `DB_PASSWORD` 注入。

## 6. 当前 API

- User：`POST /api/user/login`、`GET /api/user/me`、`PUT /api/user/me`。
- Wemedia：`POST /api/wemedia/login`；`POST/GET /api/wemedia/news`；`GET/PUT/DELETE /api/wemedia/news/{id}`；`POST /api/wemedia/news/{id}/submit`；`GET /api/wemedia/materials`。
- Article：`GET /api/article/channels`、`GET /api/article`、`GET /api/article/{id}`；原有 `/api/article/ping` 保留。
- Admin：`POST /api/admin/login`；`GET /api/admin/audit/news`、`GET /api/admin/audit/news/{id}`、`POST .../{id}/approve`、`POST .../{id}/reject`；频道 GET/POST/PUT/PATCH status/DELETE。
- 内部 Feign：`/internal/wemedia/news` 的列表/详情/通过/拒绝；`/internal/articles` 的正式文章创建与频道 CRUD。Gateway 仍不开放 `/internal/**`。

## 7. 状态机、幂等与事务

`WmNewsStatus` 已预留 DRAFT、SUBMITTED、AUTO_REVIEW、MANUAL_REVIEW、APPROVED、REJECTED、PUBLISHED、FAILED。当前可编辑状态仅 DRAFT/REJECTED；提交将可编辑态经 SUBMITTED 推进到 MANUAL_REVIEW；人工通过/拒绝使用带原状态条件的数据库 CAS 更新，拒绝必须有原因。

审核通过的幂等策略：Wemedia 已为 APPROVED 时返回同一快照；Article 以 `wm_news_id` 唯一索引保证一个自媒体稿件仅生成一个正式 Article，并处理重复键竞争；Admin 重试应返回相同 articleId。该闭环尚未在修复后的运行产物上验证。

事务边界均为单服务本地事务：草稿与素材关系替换在 Wemedia 事务；正式 Article 与 Content 在 Article 事务；后台审核记录在 Admin 本地事务。Admin → Wemedia → Article 是同步 Feign 跨服务流程，不是分布式事务；靠状态 CAS、唯一键和重试幂等收敛。补偿/outbox 留待后续阶段设计。

## 8. 已完成测试与已知问题

已落地测试：`AuthenticationFoundationTest` 覆盖 BCrypt 正误密码和 JWT 身份/类型；`WmNewsStatusTest` 覆盖可编辑态与转换；Gateway 测试增加用户类型从 Token 注入、外部伪造头不可覆盖。暂停前未执行新的全量测试。

真实验证已确认：五个服务健康；App/Wemedia/Admin 正确登录成功（Admin 用户名为 `admin_demo`），错误密码返回 401；主/第二 Wemedia 登录成功；创建草稿成功。更新草稿时曾出现 `MP_OPTLOCK_VERSION_ORIGINAL` 参数缺失。原因是缺少 `OptimisticLockerInnerInterceptor` 且 Wemedia Mapper 使用了不必要的中间继承。已将 Mapper 改为直接继承并为 User/Article/Wemedia/Admin 配置乐观锁插件，随后用 JDK 21 对相关八模块执行 `-DskipTests compile`，结果 `BUILD SUCCESS`；但尚未重新 package 和运行回归，所以该错误标记为“源码修复、运行待验证”。

`GlobalExceptionHandler` 已按 401/403/404/409 业务码映射 HTTP 状态，源码编译通过。源码运行日志会记录系统自动探测到的宿主机网卡地址，这不是配置硬编码；最终扫描应排除 `logs/` 和 `target/`。

## 9. 未完成任务与恢复顺序

未完成：重新打包/启动并回归乐观锁；草稿更新、所有权隔离、提交后禁止编辑；待审列表/详情；审核拒绝与原因落库；审核通过及重复通过幂等；正式文章详情；频道 CRUD/禁用过滤；并发 approve/reject CAS；数据库最终核验；Nacos datasource 配置约定核验；`DATABASE_DESIGN.md`、`ARTICLE_STATE_MACHINE.md`、`API.md` 和相关文档更新；全量 JDK 21 `clean verify`；原项目 clean 与禁用地址扫描。

下一次继续时第一条命令（先确认 Java 21 与源码仍可编译）：

```powershell
$env:JAVA_HOME='D:\jdkk'; $env:Path='D:\jdkk\bin;'+$env:Path; .\mvnw.cmd -DskipTests -pl leadnews-user-service,leadnews-article-service,leadnews-wemedia-service,leadnews-admin-service -am compile
```

建议顺序：只停止上述五个任务 PID → JDK 21 package 四个服务及依赖 → 以既有环境变量重启 → 首先复测 Wemedia 新草稿 update → 再跑完整闭环与并发验证 → 核验数据库/Nacos → 补文档和必要既有测试 → 停服务后执行全量 `clean verify` → 最终检查原项目只读/clean。

禁止重复：不要重建现有表或改写已执行的 V1/V2/V3 migration；不要复制原项目业务代码；不要再创建另一套账号/Mapper 基类；不要删除未完成 E2E 数据来伪造空库；不要重建 Docker 基础设施；不要进入静态化、MinIO、Kafka、Redis、ES、AI、前端或阶段 5。
