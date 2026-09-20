# 阶段 7 搜索方案

## 原项目只读分析

原项目搜索服务使用已停止维护的 `RestHighLevelClient`；文章发布后把完整搜索文档直接写入 Kafka；全文检索仅查询 title/content 并按时间排序；历史与联想均使用 MongoDB，其中联想采用正则扫描。新项目不复制这些实现：使用 Elasticsearch Java API Client 8.17.10、轻量事件 + Article 内部查询、MySQL 历史和 Elasticsearch edge-ngram 联想。

## 边界与流程

Article 是文章真相源。发布完成事务同时把文章置为 `PUBLISHED` 并写 `article_search_sync` outbox；后台 dispatcher 发送 `ArticleSearchEvent` 到 `leadnews.article.search`，Kafka key 为 articleId。事件只含 eventId/articleId/eventType/occurredAt/traceId，不携带正文。

Search Consumer 收到 UPSERT 后通过 OpenFeign 调 Article `/internal/articles/{id}/search-document`，只抽取结构化 content 中 `type=text` 的纯文本，再以 articleId 作为 ES `_id` 覆盖写入；DELETE 对文章索引和联想索引执行幂等删除。消费失败做 3 次指数退避，然后进入 `leadnews.article.search.DLT` 并记入 `search_sync_failure`。发布不依赖 Kafka 实时可用：失败事件保留在 outbox，最多自动尝试 10 次并可通过重建兜底。

## 查询与权限

- `GET /api/search/articles`：keyword 必填，channelId 可选，page 从 1 开始，size 最大 50，`page*size<=10000`，sort 为 RELEVANCE/TIME。
- RELEVANCE 使用 `multi_match(title^3, summary^2, content^1)`，随后以发布时间降序稳定排序；TIME 仅按发布时间倒序。
- title/content 高亮只输出 ES HTML encoder 转义后的文本和固定 `<em>` 标签。
- `GET /api/search/suggestions`：edge-ngram 前缀匹配，最多 10 条，数据源为已发布文章标题与 labels，不使用 wildcard 或 MongoDB。
- 文章检索与联想允许匿名；携带有效 Token 时 Gateway 仍解析身份，因此第一页搜索会更新历史。历史查询和删除必须登录，删除按 `id + user_id` 校验所有权。

## 一致性与恢复

重放同一文章 UPSERT 只覆盖同一 `_id`；DELETE 重放无副作用。管理/内部重建入口 `POST /internal/search/rebuild` 分页读取所有 PUBLISHED 文章并 Bulk 写入。重建不会修改 MySQL。发现漂移时可先删除版本化测试索引并重启 Search 让 initializer 重建，再执行 rebuild；生产蓝绿切换应新建版本索引、校验后原子切 alias。

## 降级

Elasticsearch 不可用时检索/联想返回 HTTP 503，Article 发布、详情和静态页不受影响。恢复 ES 后客户端会自动重新连接；outbox、DLT 审计和全量重建共同提供恢复路径。
