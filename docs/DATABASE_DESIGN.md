# 阶段 4 数据库设计

阶段 11 Article V4 为 `ap_article` 增加 `summary_status/summary_version/summary_model/summary_generated_time`，创建 `article_ai_summary_task` 保存 event、版本、状态、retry、时间、脱敏错误和 traceId；唯一键为 `(article_id,summary_version)`、`event_id`。续写明确不落库，因此没有续写表。

阶段 5 新增 Article V2：`static_object_key`、`publish_status`、`publish_retry_count`、`published_time`、`publish_started_time`、`last_error` 及恢复索引。新增 Schedule V1：`schedule_task`，以 `(task_type,business_id)` 唯一键和 `(status,execute_time)` 索引支持幂等与到期扫描。既有 migration 未修改。

更新时间：2026-09-05

## 设计约定

四个服务各自拥有 schema 和 Flyway 历史，不跨库建物理外键。主键统一采用 MySQL `BIGINT AUTO_INCREMENT`；当前规模不需要雪花 ID。业务关联由 ID、服务 API 和索引维护，跨服务删除/一致性后续通过事件或补偿演进。字符集为 `utf8mb4`，引擎为 InnoDB；实体表普遍包含 `created_time`、`updated_time`、`version`、`deleted`，MyBatis-Plus 负责自动填充、乐观锁和逻辑删除。

## Schema 与表

| Schema / 表 | 职责 | 关键字段 | 索引与约束 |
| --- | --- | --- | --- |
| `leadnews_user.ap_user` | App 用户与登录 | name、phone、password_hash、image、sex、认证/状态标志 | phone、name 唯一 |
| `leadnews_article.ap_channel` | 频道 | name、description、status、ord | name 唯一；status+ord 索引 |
| `leadnews_article.ap_article` | 审核通过后的正式文章元数据 | wm_news_id、author、channel、title、layout、cover_images、labels、publish_time、summary、static_url | wm_news_id 唯一；channel_id+publish_time 索引 |
| `leadnews_article.ap_article_content` | 正式文章正文 JSON | article_id、content | article_id 唯一 |
| `leadnews_article.ap_article_comment` | 已发布文章的用户评论 | article_id、user_id、author_name、content、status | article_id+created_time、user_id+created_time 索引；逻辑删除 |
| `leadnews_wemedia.wm_user` | 独立自媒体登录身份，可绑定 App 用户 | ap_user_id、name、password_hash、nickname、status、account_type | name、ap_user_id、phone 唯一 |
| `leadnews_wemedia.wm_news` | 自媒体草稿与审核状态 | user_id、title、content、channel_id、cover_images、status、reason、submitted_time | owner+status、status+submitted_time 索引 |
| `leadnews_wemedia.wm_material` | 素材元数据（阶段 4 只查询） | user_id、url、object_key、material_type | owner+created_time 索引 |
| `leadnews_wemedia.wm_news_material` | 稿件与素材关联 | news_id、material_id、reference_type、ord | 三字段唯一；material_id 索引 |
| `leadnews_admin.ad_user` | 管理员与登录 | name、password_hash、nickname、status | name、phone 唯一 |
| `leadnews_admin.ad_audit_record` | 人工审核事实 | wm_news_id、reviewer_id、decision、reason、reviewed_time | wm_news_id 唯一；reviewer+time 索引 |

JSON 字段中的正文使用受校验的 `[{"type":"text|image","value":"..."}]`；不是 Controller 接收的任意 Map。密码列仅保存 BCrypt hash。

## Flyway

- User：V1 建表/演示账号；V2 修正演示 BCrypt hash。
- Article：V1 建三表并初始化“推荐/科技”频道；V2 发布静态化状态；V3 搜索 Outbox；V4 AI 摘要状态/任务；V5 新增文章评论表。
- Wemedia：V1 建四表/主账号；V2 修正 hash；V3 增加所有权测试账号。
- Admin：V1 建两表/管理员；V2 修正 hash。

已执行的 migration 不允许改写；后续结构变化必须新增版本脚本。

## 原项目映射与差异

| 原表 | 新表 | 处理 |
| --- | --- | --- |
| `ap_user` | `ap_user` | 保留身份/资料思想；旧 MD5+salt 改 BCrypt hash，状态改语义字段，补审计/version/deleted |
| `ap_channel` | `ap_channel` | 保留频道字段，名称由唯一索引约束，状态使用 ENABLED/DISABLED |
| `ap_article` | `ap_article` | 保留作者、频道、标题、布局、封面、发布时间；增加 wm_news_id 幂等键、summary/static_url 预留 |
| `ap_article_content` | `ap_article_content` | 保留正文分表；正文改为受 DTO 约束的 JSON |
| `ap_article_config` | 暂不单建 | 阶段 4 尚无上下架/评论开关业务，不复制空模型；正式文章 status 承担最小发布状态 |
| `wm_user` | `wm_user` | 保留与 App 用户可选绑定及独立登录体系；密码升级 BCrypt |
| `wm_news` | `wm_news` | 保留草稿、频道、标签、封面、审核原因；数字状态改枚举字符串 |
| `wm_material` / `wm_news_material` | 同名新表 | 保留素材及关联思想；上传延后阶段 5，只存 URL/objectKey 元数据 |
| 原 Admin 用户/审核能力 | `ad_user` / `ad_audit_record` | 独立管理员身份；审核决定必须形成不可缺失的记录，而非只改稿件状态 |

本阶段刻意不创建 Redis Key、Kafka Topic、ES Index、MinIO 对象表或 AI 审核表。
# 阶段 6 行为库

`leadnews_behavior` 由 Flyway V1 初始化，V2 为事件日志增加 `stat_applied` 和待聚合索引：

- `article_behavior_stat`：article_id 主键，保存非负的点赞/浏览/评论/收藏持久化计数。
- `article_like`：唯一键 `(article_id,user_id)`，status 表示当前关系。
- `behavior_event_log`：`event_id` 唯一，是 Kafka 至少一次消费的最终幂等屏障；VIEW 用 `stat_applied` 支持可恢复批量汇总。
- `behavior_failed_event`：保存耗尽重试后的 DLT 审计信息。
# 阶段 7 Search 数据

`leadnews_article.article_search_sync` 保存发布事务内产生的搜索同步 outbox，状态为 PENDING/SUCCESS/FAILED，带 retry_count/next_retry_time/last_error。`leadnews_search.search_history` 使用 `(user_id,keyword)` 唯一键实现去重并更新时间，每用户只保留最近 20 条；`search_sync_failure` 审计进入 DLT 的事件。新增 migration：Article V3、Search V1；不修改历史 migration。
# 阶段 10 Wemedia V4/V5

- `wm_news` 新增 `audit_version`、`audit_source`。
- `wm_news_audit_task` 保存任务状态、独立投递状态、eventId、attemptNo、投递重试、下次投递、开始/完成时间和错误；`(news_id,audit_version)` 与 eventId 唯一。V5 将原先混入 status 的 SENT 拆到 `dispatch_status`，并增加 attempt fencing。
- `wm_news_audit_record` 是 DFA、AI、自动审核耗尽和人工审核的统一轨迹，含 attemptNo、决策、风险、原因、置信度、tags/matches JSON、模型/requestId/latency/error/reviewer 和时间；`(news_id,audit_version,audit_stage)` 唯一。
