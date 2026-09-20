# Kafka 行为事件设计

## 阶段 11 ArticlePublishedEvent

Topic `leadnews.article.published`（DLT `leadnews.article.published.DLT`），3 partitions、开发副本 1，key 为 articleId，group 为 `leadnews-article-summary-v1`。Payload 不含正文；任务 outbox、唯一键和 summaryVersion CAS 负责可恢复与幂等，不复用 search/behavior Topic。Kafka 3.9 要求 delivery timeout 不小于 linger + request timeout，Article Producer 均为 30000 ms。

## Topic 与事件

- 主 Topic：`leadnews.behavior.events`，3 partitions，replication factor 1（本地单 Broker）。
- DLT：`leadnews.behavior.events.DLT`，分区与来源分区一致。
- `BehaviorEvent`：`eventId`、`articleId`、`userId`、`behaviorType`、`delta`、`occurredAt`、`traceId`。
- LIKE/UNLIKE key：`articleId:userId`；VIEW key：`articleId`。Kafka 只保证同一 partition 内有序。

Producer 使用 JSON、`acks=all`、idempotence、3 次有限 retry、5 秒 request timeout、15 秒 delivery timeout。Lua 在发送前已把事件写入 Redis pending；Kafka ACK 成功才删除，因此同步或异步发送失败都不会永久丢失。

Consumer 使用手动 record ack 语义。数据库事务先 `INSERT IGNORE behavior_event_log`；唯一 eventId 冲突立即结束，不重复累计。LIKE/UNLIKE 还要求 `article_like` 的状态确实改变才修改统计。VIEW 日志标记 `stat_applied=0`，由数据库事务批量聚合应用，提交失败时锁定、统计和标记一起回滚。异常不吞掉，初次处理加 2 次固定退避重试，之后由 `DeadLetterPublishingRecoverer` 写 DLT；DLT Consumer 写失败审计表。

阶段 6 真实验证：Host Java Producer → Docker Kafka → Host Java Consumer 完成 LIKE/UNLIKE/VIEW；相同 eventId 投递两次仅一条日志且只增加一次；COLLECT 测试事件主事务回滚并最终进入 DLT 审计；Kafka 停机期间 pending=1，恢复后 pending=0 且 MySQL 计数收敛。
# 阶段 7 文章搜索事件

Topic `leadnews.article.search`（3 partitions），DLT `leadnews.article.search.DLT`（3 partitions）；key 固定 articleId。`ArticleSearchEvent` 不携带正文。Search 失败有限重试后投递 DLT 并落 `search_sync_failure`；Article 的 `article_search_sync` outbox 负责生产可靠性和 Kafka 恢复后的补发。

# 阶段 8 热点刷新事件

不创建第二套行为 Topic。`leadnews-hot-score-stream-v1` 独立消费 `leadnews.behavior.events`，按 `occurredAt` 事件时间进行 5 秒窗口 + 10 秒 grace 聚合，V3 Window Store 保存频道和累计分值；窗口关闭后使用确定性 windowEventId，由 Redis Lua 原子检查 applied ZSet、更新 global/channel 榜并记录已应用窗口。旧 refresh Topic/Consumer 已停用但不自动删除历史 Topic。MySQL persistence group 与 Streams application-id 各有 offset，互不抢占。Kafka EOS 不涵盖 Redis，见 `HOT_ARTICLE_DESIGN.md`。
# 阶段 10 审核 Topic

- `leadnews.wemedia.audit`：3 partitions，key=newsId，payload 为 eventId/newsId/auditVersion/occurredAt/traceId。
- `leadnews.wemedia.audit.DLT`：有限消费重试后的死信；业务异常仍先安全落 `MANUAL_REVIEW`。
- Consumer group `leadnews-wemedia-audit-v1`。数据库任务/outbox 负责可靠投递；任务唯一键、eventId 与版本 CAS 负责幂等。
