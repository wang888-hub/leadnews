# 阶段 7 搜索方案

## 原项目只读分析

原项目搜索服务使用已停止维护的 `RestHighLevelClient`；文章发布后把完整搜索文档直接写入 Kafka；全文检索仅查询 title/content 并按时间排序；历史与联想均使用 MongoDB，其中联想采用正则扫描。新项目不复制这些实现：使用 Elasticsearch Java API Client 8.17.10、轻量事件 + Article 内部查询、MySQL 历史和 Elasticsearch edge-ngram 联想。

## 边界与流程

Article 是文章真相源。发布完成事务同时把文章置为 `PUBLISHED`、递增 `search_version` 并写 `article_search_sync` outbox；下架、删除、重新上架和已发布文章 AI 摘要成功也在同一个 MySQL 本地事务内递增版本并写出 UPSERT/DELETE 意图。事件含 eventId/articleId/articleVersion/eventType/occurredAt/traceId，不携带正文。

Outbox 状态机为 `PENDING → PROCESSING → SENT`。Publisher 用带状态条件的数据库 UPDATE 做 CAS Claim；`PROCESSING` 是默认 5 分钟的可配置 Lease，进程在 Kafka 成功而标记 SENT 前宕机时允许超时重发，因此此段明确是 At-Least-Once。发送失败递增 retryCount、记录 lastError 并按指数退避回 PENDING，达到 10 次后显式进入 DEAD。

Search Consumer 收到 UPSERT 后通过 OpenFeign 调 Article `/internal/articles/{id}/search-document`，只抽取结构化 content 中 `type=text` 的纯文本；DELETE 不物理删除，而是向文章索引和联想索引写入 `deleted=true` Tombstone。两类写入都以 articleId 为 `_id`、articleVersion 为 Elasticsearch `version`，并使用 `version_type=external` 原子门禁。409 表示旧版本或重复版本，属于正常幂等结果，直接 ACK，不重试也不进 DLT。其他异常做最多 3 次指数退避，随后进入 `leadnews.article.search.DLT` 并记入 `search_sync_failure`；明确的反序列化/本地映射异常不做无意义重试。

## 查询与权限

- `GET /api/search/articles`：keyword 必填，channelId 可选，page 从 1 开始，size 最大 50，`page*size<=10000`，sort 为 RELEVANCE/TIME。
- RELEVANCE 使用 `multi_match(title^3, summary^2, content^1)`，随后以发布时间降序稳定排序；TIME 仅按发布时间倒序。
- 文章搜索和 suggestion 查询都强制过滤 `deleted=false`，Tombstone 永远不可见。
- title/content 高亮只输出 ES HTML encoder 转义后的文本和固定 `<em>` 标签。
- `GET /api/search/suggestions`：edge-ngram 前缀匹配，最多 10 条，数据源为已发布文章标题与 labels，不使用 wildcard 或 MongoDB。
- 文章检索与联想允许匿名；携带有效 Token 时 Gateway 仍解析身份，因此第一页搜索会更新历史。历史查询和删除必须登录，删除按 `id + user_id` 校验所有权。

## 一致性与恢复

重复与乱序由 `search_version + external version` 保证无害。例如 v15 Tombstone 后到达的 v14 UPSERT 会得到 409，不能复活文章；v16 UPSERT 能正常重新上架。管理/内部重建入口 `POST /internal/search/rebuild` 仅供灾难恢复，先清除现有 alias 中的幽灵文档，再分页读取全部 PUBLISHED 文章并用带 external version 的 Bulk API 重建，不配置定时任务，也不一次性加载全表。当前保留了版本化 index 与 alias 扩展点，但此次最小改动没有加入完整蓝绿校验/双 alias 切换编排；生产零停机重建可在该扩展点补充。

## 数据库迁移 V6

- `ap_article.search_version BIGINT NOT NULL`：既有行回填为 1，新文章首次发布从 0 原子递增到 1。
- `article_search_sync.article_version`：既有事件按当前 Article 版本回填。
- `article_search_sync.claimed_at`：记录 PROCESSING Lease 起点。
- 历史 SUCCESS 迁为 SENT、FAILED 迁为可重试 PENDING，并新增 dispatch/lease 索引。

部署时必须先让 Article Service 应用 Flyway V6，再启动新版 Publisher/Search Consumer。因旧 ES 文档没有 `deleted` 字段，新版上线后应人工执行一次 `POST /internal/search/rebuild`；该动作有短暂清空再重建窗口，需安排在维护时间。此次没有新增 Inbox、Retry Topic、Redis Queue 或分布式事务框架。

## 降级

Elasticsearch 不可用时检索/联想返回 HTTP 503，Article 发布、详情和静态页不受影响。恢复 ES 后客户端会自动重新连接；outbox、DLT 审计和全量重建共同提供恢复路径。
