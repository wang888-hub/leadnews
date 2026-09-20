# AI 文章摘要设计

文章事务完成 `PUBLISHED` 后，Article Service 以 best-effort 创建 `article_ai_summary_task`。任务失败不会回滚发布；遗漏由 backfill 扫描补齐。

任务表也是可恢复 outbox：向 `leadnews.article.published` 发送 `ArticlePublishedEvent`，Kafka key 为 `articleId`。事件仅含 `eventId/articleId/summaryVersion/publishedAt/occurredAt/traceId`，消费者回查权威正文。

Article 状态为 `NONE -> PENDING -> GENERATING -> SUCCESS/FAILED`；Task 状态为 `PENDING/FAILED -> SENT -> RUNNING -> SUCCESS/FAILED/STALE`。`(article_id,summary_version)` 和 `event_id` 唯一，重复事件不再调用模型。每次重新生成递增 `summary_version`，回写用文章、版本、PUBLISHED 三条件 CAS，迟到结果只标记 STALE。

失败按 2/4/8…秒有限退避，默认最多 3 次；超过 2 分钟的 RUNNING 可恢复。XXL handlers 为 `aiSummaryRetryJob`、`aiSummaryBackfillJob`。内部 `POST /internal/articles/summary/backfill?page=&size=` 分页补齐 PUBLISHED 且 NONE/FAILED 的旧文章，默认 20、最大 100。

模型固定 `qwen3.8-max`，摘要默认 80~160 字。指标覆盖成功、失败、延迟；日志不记录正文、Prompt、摘要全文或 Key。

对正文很短的测试文章，固定 80 字下限会与“摘要不得添加正文外事实”冲突。阶段 15.5 将有效下限定义为 `min(配置下限, max(10, 正文 Unicode 字符数 / 2))`，上限仍为配置的 160 字；AI Service Prompt 与 Article 回写校验使用同一规则。正常长度文章仍严格采用 80~160 字。

## 阶段 15.5：单文章精确恢复

内部运维入口为 `POST /internal/articles/{articleId}/summary/retry`，不加入 Gateway 路由，并在应用层再次要求现有可信内部标记。它只接受 `PUBLISHED` 文章；不存在返回 404，非已发布或不适合恢复的状态返回 409。`SUCCESS` 以及已经进入 `PENDING/GENERATING` 的重复请求按幂等 no-op 返回，绝不重新生成。

恢复沿用当前 `summaryVersion` 和唯一的 `(article_id, summary_version)` 任务，不创建新版本：这是对同一正文、同一摘要版本的运维重试，不是正文变更或重新生成。事务内先以 `articleId + summaryVersion + PUBLISHED + FAILED` CAS 将文章改为 `PENDING`，再以任务主键、articleId、summaryVersion 和 `FAILED` CAS 将同一任务重置为 `PENDING/retryCount=0`。任一 CAS 失败则整体回滚；并发重复请求最多一个返回 `reopened=true`。

后续仍由既有 dispatcher/Kafka consumer 执行。Consumer 回写继续要求文章、当前 summaryVersion、PUBLISHED 三条件 CAS，因此旧版本结果不能覆盖新版本；精确入口不调用 backfill，也不会扫描或修改相邻文章。
