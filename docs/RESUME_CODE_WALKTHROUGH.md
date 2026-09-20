# 简历亮点源码与链路讲解

本文是 `RESUME_PROJECT.md` 的代码版讲稿。路径均相对项目根目录；链接同时带当前源码行号。由于部分 Java 类采用紧凑单行写法，定位时以类名和方法名为主。

## 1. Gateway JWT、可信 Header 与 Sentinel 限流

### 代码位置

- 请求认证入口：[JwtAuthenticationGlobalFilter.java](../leadnews-gateway/src/main/java/com/aaliyun/leadnews/gateway/filter/JwtAuthenticationGlobalFilter.java#L1)
- Gateway Sentinel 初始化：[GatewaySentinelConfiguration.java](../leadnews-gateway/src/main/java/com/aaliyun/leadnews/gateway/config/GatewaySentinelConfiguration.java#L24)
- Feign 上下文透传配置：[FoundationAutoConfiguration.java](../leadnews-common/src/main/java/com/aaliyun/leadnews/common/config/FoundationAutoConfiguration.java#L13)
- 统一身份上下文：[UserContext.java](../leadnews-common/src/main/java/com/aaliyun/leadnews/common/context/UserContext.java#L1)

### 完整链路

```text
Vue Bearer Token
 → Gateway GlobalFilter 删除外部 X-User-* 头
 → 白名单判断 / JWT 验签与过期校验
 → 从 claims 重建 X-User-Id、X-User-Type
 → lb:// 服务路由
 → Servlet Filter 建立 UserContext/MDC
 → Feign Interceptor 继续透传可信身份和 TraceId
```

Sentinel 在业务路由之前按 Route、API Group 和热点参数限流。被拒请求直接返回 429，因此不会进入 Redis Lua、Kafka Producer 或 Qwen Provider。

### 主要代码

```java
// 核心语义：先移除不可信头，再只从已验证 JWT 构造身份。
ServerHttpRequest.Builder request = exchange.getRequest().mutate();
request.headers(headers -> {
    headers.remove("X-User-Id");
    headers.remove("X-User-Type");
});
request.header("X-User-Id", claims.userId().toString());
request.header("X-User-Type", claims.userType());
```

### 面试讲法

不要说“用了 JWT”。应说明客户端 Header 本身不可信，Gateway 是身份信任边界；下游只读 Gateway 重建的信息。当前 internal header 是开发边界，生产仍需 mTLS、服务身份和网络策略。

## 2. Redis Lua + Kafka 高频行为最终一致性

### 代码位置

- HTTP 入口：[BehaviorController.java](../leadnews-behavior-service/src/main/java/com/aaliyun/leadnews/behavior/web/BehaviorController.java#L3)
- 行为编排：[BehaviorApplicationService.java](../leadnews-behavior-service/src/main/java/com/aaliyun/leadnews/behavior/service/BehaviorApplicationService.java#L7)
- 点赞脚本：[like.lua](../leadnews-behavior-service/src/main/resources/lua/like.lua#L1)
- 取消脚本：[unlike.lua](../leadnews-behavior-service/src/main/resources/lua/unlike.lua#L1)
- 浏览脚本：[view.lua](../leadnews-behavior-service/src/main/resources/lua/view.lua#L1)
- 发送与 pending 重投：[BehaviorEventPublisher.java](../leadnews-behavior-service/src/main/java/com/aaliyun/leadnews/behavior/service/BehaviorEventPublisher.java#L4)
- Kafka 幂等消费：[BehaviorEventConsumer.java](../leadnews-behavior-service/src/main/java/com/aaliyun/leadnews/behavior/service/BehaviorEventConsumer.java#L4)
- 数据库唯一写入：[BehaviorMapper.java](../leadnews-behavior-service/src/main/java/com/aaliyun/leadnews/behavior/mapper/BehaviorMapper.java#L4)

### 完整链路

```text
POST/DELETE /api/behavior/articles/{articleId}/like 或 POST /view
 → Gateway 身份与 Sentinel
 → BehaviorApplicationService 生成固定 eventId/payload
 → Lua 原子修改 relation + counter + pending Hash
 → 只有 changed=true 才发 Kafka
 → 发送成功删除 pending；失败保留并由定时任务重投
 → Consumer 先 INSERT IGNORE eventId
 → 首次事件才修改 relation/stat；VIEW 留给批处理聚合
 → 事务成功后提交 offset
```

### 主要代码

```java
List<?> result = redis.execute(
    liked ? likeScript : unlikeScript,
    List.of(BehaviorRedisKeys.articleLikes(articleId),
            BehaviorRedisKeys.articleCounter(articleId),
            BehaviorRedisKeys.PENDING_EVENTS),
    String.valueOf(user), event.eventId(), payload);
boolean changed = ((Number) result.get(0)).longValue() == 1;
if (changed) publisher.send(event);
```

```lua
-- like.lua 的核心语义（源码中完成 relation、counter、pending 原子更新）
if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
  return {0, tonumber(redis.call('HGET', KEYS[2], 'likeCount') or '0')}
end
redis.call('SADD', KEYS[1], ARGV[1])
local count = redis.call('HINCRBY', KEYS[2], 'likeCount', 1)
redis.call('HSET', KEYS[3], ARGV[2], ARGV[3])
return {1, count}
```

```java
@Transactional
public void consume(BehaviorEvent event) {
    if (db.insertEvent(event) == 0) return; // eventId 唯一键：重复即 no-op
    switch (event.behaviorType()) {
        case LIKE -> { if (db.saveLike(event.articleId(), event.userId(), 1) > 0)
                           db.addStat(event.articleId(), 1, 0); }
        case UNLIKE -> { /* 同样按真实状态迁移更新 */ }
        case VIEW -> { /* durable log，由批处理聚合 */ }
    }
}
```

### 面试讲法

Lua 解决 Redis 内部竞态，pending 解决“Redis 成功、Kafka 失败”，eventId 解决“DB 成功、offset 未提交导致重放”。这三个机制处理的是不同失败窗口，不能只说一个“幂等”。

## 3. Kafka Streams + Redis ZSet 实时/离线热点

### 代码位置

- Streams 拓扑：[HotArticleStreamTopology.java](../leadnews-behavior-service/src/main/java/com/aaliyun/leadnews/behavior/service/HotArticleStreamTopology.java#L1)
- Redis 增量写入：[HotScoreRedisWriter.java](../leadnews-behavior-service/src/main/java/com/aaliyun/leadnews/behavior/service/HotScoreRedisWriter.java#L1) 与 [hot-increment.lua](../leadnews-behavior-service/src/main/resources/lua/hot-increment.lua#L1)
- 每分钟衰减：[HotScoreDecayService.java](../leadnews-behavior-service/src/main/java/com/aaliyun/leadnews/behavior/service/HotScoreDecayService.java#L1) 与 [hot-decay.lua](../leadnews-behavior-service/src/main/resources/lua/hot-decay.lua#L1)
- MySQL 完整快照与近似恢复：[HotArticleRebuildService.java](../leadnews-behavior-service/src/main/java/com/aaliyun/leadnews/behavior/service/HotArticleRebuildService.java#L1)、[HotScoreRecoveryCalculator.java](../leadnews-behavior-service/src/main/java/com/aaliyun/leadnews/behavior/service/HotScoreRecoveryCalculator.java#L1) 与 [HotArticleRebuildJob.java](../leadnews-behavior-service/src/main/java/com/aaliyun/leadnews/behavior/service/HotArticleRebuildJob.java#L1)
- 查询与降级：[HotArticleQueryService.java](../leadnews-article-service/src/main/java/com/aaliyun/leadnews/article/service/HotArticleQueryService.java#L1)

### 完整链路

```text
Behavior Kafka Event
 → Kafka Streams 按 articleId 分组
 → occurredAt 事件时间、5 秒窗口 + 10 秒 grace 聚合 channelId 与 deltaScore
 → suppress 仅输出关闭窗口的最终分数增量
 → 确定性 windowEventId → Redis Lua 原子去重并 ZINCRBY 全局榜与频道榜
 → 每分钟 Lua 将当前分数乘以 0.99
 → Article 查询 ZREVRANGE 后批量回源文章

每 30 分钟保存 HotScore + 四类业务 count 的 MySQL 完整快照
 → Redis 榜缺失时用快照、当前 count 差值与时间衰减近似恢复
 → 恢复期间实时窗口增量写 delta 榜，最终 Lua 合并并原子切换
```

### 主要代码

```java
builder.stream(behavior.getTopic(), Consumed.with(Serdes.String(), Serdes.String())
        .withTimestampExtractor(new BehaviorEventTimestampExtractor(json)))
    .map((key, value) -> parseArticleIdAndDelta(value))
    .filter((articleId, delta) -> articleId != null && delta != null && delta != 0D)
    .groupByKey(Grouped.with(Serdes.String(), Serdes.Double()))
    .windowedBy(TimeWindows.ofSizeAndGrace(Duration.ofSeconds(5), Duration.ofSeconds(10)))
    .aggregate(() -> new HotScoreAggregate(null, 0D),
        (articleId, next, current) -> new HotScoreAggregate(next.channelId(),
            current.deltaScore() + next.deltaScore()), Materialized.as("hot-score-window-v3"))
    .suppress(Suppressed.untilWindowCloses(Suppressed.BufferConfig.maxRecords(10_000).shutDownWhenFull()))
    .toStream().foreach((windowed, deltaScore) -> writer.apply(Long.parseLong(windowed.key()), deltaScore));
```

### 面试讲法

这个榜单是行为事件的时间窗口增量，不依赖 MySQL 累计计数重算。`exactly_once_v2` 保护 Kafka offset 与 State Store，但不覆盖 Redis 外部副作用；确定性的 windowEventId 与 Redis Lua 在同一脚本中完成去重及加分。applied 标记过期或 Redis 数据回滚后仍可能漂移，详情见 [HOT_ARTICLE_DESIGN.md](HOT_ARTICLE_DESIGN.md)。

## 4. 审核、定时发布、Freemarker 与 MinIO

### 代码位置

- 审核编排：[ArticleAuditOrchestrator.java](../leadnews-wemedia-service/src/main/java/com/aaliyun/leadnews/wemedia/audit/ArticleAuditOrchestrator.java#L1)
- 决策阈值：[AuditDecisionPolicy.java](../leadnews-wemedia-service/src/main/java/com/aaliyun/leadnews/wemedia/audit/AuditDecisionPolicy.java#L1)
- 发布入口：[ArticlePublishService.java](../leadnews-article-service/src/main/java/com/aaliyun/leadnews/article/service/ArticlePublishService.java#L1)
- HTML 渲染：[ArticleHtmlRenderer.java](../leadnews-article-service/src/main/java/com/aaliyun/leadnews/article/service/ArticleHtmlRenderer.java#L1)
- 发布终态事务：[ArticlePublicationFinalizer.java](../leadnews-article-service/src/main/java/com/aaliyun/leadnews/article/service/ArticlePublicationFinalizer.java#L3)
- 定时任务：[PublishTaskService.java](../leadnews-schedule-service/src/main/java/com/aaliyun/leadnews/schedule/service/PublishTaskService.java#L6)

### 完整链路

```text
Wemedia submit
 → 事务递增 auditVersion、写审核任务/outbox
 → Kafka Consumer claim 当前版本
 → DFA：命中直接 REJECTED
 → 未命中调用 Qwen 多模态
 → 高置信 PASS/REJECT；低置信或异常 MANUAL_REVIEW
 → 立即发布，或 Redis ZSet 保存 executeTime
 → Schedule 到期取候选，MySQL CAS 决定唯一执行者
 → Article APPROVED/WAITING → PUBLISHING
 → Freemarker 渲染 → MinIO article/{id}/index.html
 → 本地事务 PUBLISHED + search outbox
 → 失败 PUBLISH_FAILED，XXL-Job 精确补偿
```

### 主要代码

```java
@Transactional
public void complete(Long id, String key, String url, LocalDateTime done) {
    int changed = articles.update(null, new LambdaUpdateWrapper<Article>()
        .eq(Article::getId, id)
        .eq(Article::getPublishStatus, "PUBLISHING")
        .set(Article::getStaticObjectKey, key)
        .set(Article::getStaticUrl, url)
        .set(Article::getPublishStatus, "PUBLISHED")
        .set(Article::getStatus, "PUBLISHED"));
    if (changed != 1) throw new IllegalStateException("Publish completion CAS failed");
    outbox.enqueue(id, ArticleSearchEvent.EventType.UPSERT);
}
```

```java
String objectKey = "article/" + articleId + "/index.html";
// 重试覆盖同一 Key，而不是生成新的随机对象。
```

### 面试讲法

这里没有分布式事务。关键是显式 `PUBLISHING/PUBLISH_FAILED/PUBLISHED`、确定性 objectKey、CAS 和补偿。MinIO 故障实测先保留失败状态，恢复后约 606 ms 精确发布成功。

## 5. MySQL Outbox + Kafka + Elasticsearch

### 代码位置

- 发布事务写 outbox：[ArticlePublicationFinalizer.java](../leadnews-article-service/src/main/java/com/aaliyun/leadnews/article/service/ArticlePublicationFinalizer.java#L3)
- outbox 发送/退避：[ArticleSearchOutboxService.java](../leadnews-article-service/src/main/java/com/aaliyun/leadnews/article/service/ArticleSearchOutboxService.java#L3)
- 搜索事件模型：[ArticleSearchEvent.java](../leadnews-model/src/main/java/com/aaliyun/leadnews/model/search/ArticleSearchEvent.java#L5)
- ES Consumer 与 DLT：[ArticleSearchListener.java](../leadnews-search-service/src/main/java/com/aaliyun/leadnews/search/messaging/ArticleSearchListener.java#L1)
- 搜索实现：[ArticleSearchService.java](../leadnews-search-service/src/main/java/com/aaliyun/leadnews/search/service/ArticleSearchService.java#L3)

### 完整链路

```text
Article 发布完成事务
 → INSERT article_search_sync(PENDING,eventId,articleId)
 → 定时投递器发送 leadnews.article.search，key=articleId
 → 成功标 SUCCESS；失败记录 retryCount/nextRetryTime/lastError
 → Search Consumer 按 articleId 回查 Article 权威文档
 → ES 幂等 index/delete
 → 有限重试耗尽进入 DLT，并记录 search_sync_failure
```

### 主要代码

```java
kafka.send("leadnews.article.search", row.getArticleId().toString(), event)
     .get(10, TimeUnit.SECONDS);
mapper.update(null, update.eq(ArticleSearchSync::getId, row.getId())
    .set(ArticleSearchSync::getStatus, "SUCCESS"));
// 异常时保留 FAILED、retryCount、nextRetryTime、lastError。
```

```java
@KafkaListener(topics = "${leadnews.search.dlt-topic:leadnews.article.search.DLT}")
public void dlt(ArticleSearchEvent event) {
    jdbc.update("INSERT INTO search_sync_failure(event_id,article_id,...) " +
                "VALUES(?,?,...) ON DUPLICATE KEY UPDATE ...", ...);
}
```

### 面试讲法

ES 是可重建查询投影，不是文章数据库。ES 停止时发布继续成功、Search 返回明确 503；恢复后实测约 22.6 秒 outbox 转 SUCCESS 并可检索。

## 6. AI 三级审核与版本隔离

### 代码位置

- DFA：[SensitiveWordMatcher.java](../leadnews-wemedia-service/src/main/java/com/aaliyun/leadnews/wemedia/audit/SensitiveWordMatcher.java#L1)
- 审核编排：[ArticleAuditOrchestrator.java](../leadnews-wemedia-service/src/main/java/com/aaliyun/leadnews/wemedia/audit/ArticleAuditOrchestrator.java#L1)
- 策略：[AuditDecisionPolicy.java](../leadnews-wemedia-service/src/main/java/com/aaliyun/leadnews/wemedia/audit/AuditDecisionPolicy.java#L1)
- AI 内部契约：[AiAuditInternalClient.java](../leadnews-feign-api/src/main/java/com/aaliyun/leadnews/feign/ai/AiAuditInternalClient.java#L4)
- AI 实现入口：[AiInternalController.java](../leadnews-ai-service/src/main/java/com/aaliyun/leadnews/ai/controller/AiInternalController.java#L59)
- 任务版本字段：[AuditTask.java](../leadnews-wemedia-service/src/main/java/com/aaliyun/leadnews/wemedia/domain/AuditTask.java#L2)

### 完整链路

```text
提交稿件
 → auditVersion +1，创建唯一 (newsId,auditVersion) 任务
 → DFA 扫描 title/labels/text
 → 命中：记录 DFA 轨迹并 REJECTED，不调用 Provider
 → 未命中：解析可信 MinIO objectKey，调用 Qwen 多模态
 → Bean/schema 校验 decision、riskLevel、reason、confidence、tags
 → PASS≥0.85 / REJECT≥0.90 才自动决定
 → 其他结果、超时、限流、解析/图片错误 → MANUAL_REVIEW
 → CAS where newsId + auditVersion，旧消息不得覆盖
```

### 主要代码

```java
return switch (result.decision()) {
    case PASS -> result.confidence() >= passThreshold
        ? Outcome.APPROVE : Outcome.MANUAL;
    case REJECT -> result.confidence() >= rejectThreshold
        ? Outcome.REJECT : Outcome.MANUAL;
};
```

```java
// 任务/稿件更新都附带当前 auditVersion 条件。
.eq(WmNews::getId, newsId)
.eq(WmNews::getAuditVersion, auditVersion)
```

### 面试讲法

AI 失败绝不能默认 PASS，因为这会把 Provider 故障变成内容安全漏洞。DFA 负责确定性低成本规则，Qwen 负责语义和图片，人工负责不确定性。

## 7. AI Summary 异步生成与精确恢复

### 代码位置

- 摘要任务全链路：[ArticleAiSummaryService.java](../leadnews-article-service/src/main/java/com/aaliyun/leadnews/article/service/ArticleAiSummaryService.java#L1)
- 精确恢复入口：[InternalArticleController.java](../leadnews-article-service/src/main/java/com/aaliyun/leadnews/article/web/InternalArticleController.java#L8)
- 事件模型：[ArticlePublishedEvent.java](../leadnews-model/src/main/java/com/aaliyun/leadnews/model/ai/ArticlePublishedEvent.java#L3)
- Qwen 摘要实现：[AiWritingService.java](../leadnews-ai-service/src/main/java/com/aaliyun/leadnews/ai/service/AiWritingService.java#L1)
- 幂等测试：[ArticleAiSummaryServiceTest.java](../leadnews-article-service/src/test/java/com/aaliyun/leadnews/article/service/ArticleAiSummaryServiceTest.java#L11)

### 完整链路

```text
PUBLISHED
 → 创建 article_ai_summary_task(PENDING, summaryVersion, eventId)
 → 投递 leadnews.article.published
 → Consumer 以 eventId/version 找任务并 CAS claim RUNNING
 → 回查文章正文 → Feign 调 AI Service → Qwen
 → 校验长度/非空
 → UPDATE article WHERE PUBLISHED AND summaryVersion=消息版本
 → SUCCESS；旧版本 STALE；异常有限重试后 FAILED
 → 运维 internal/{id}/summary/retry 双 CAS 只重开目标 FAILED
```

### 主要代码

```java
if (article == null || !"PUBLISHED".equals(article.getPublishStatus())
        || !Objects.equals(article.getSummaryVersion(), event.summaryVersion())) {
    finishTask(task, "STALE", null);
    return;
}
int changed = articles.update(null, new LambdaUpdateWrapper<Article>()
    .eq(Article::getId, article.getId())
    .eq(Article::getSummaryVersion, event.summaryVersion())
    .eq(Article::getPublishStatus, "PUBLISHED")
    .set(Article::getSummary, summary)
    .set(Article::getSummaryStatus, "SUCCESS"));
```

```java
@PostMapping("/{id}/summary/retry")
public SummaryRetryResult retrySummary(@PathVariable Long id) {
    return summaries.retryFailed(id); // Gateway 不暴露 internal 路径
}
```

### 面试讲法

摘要是增强字段，所以失败不回滚文章。`summaryVersion` 解决旧结果迟到，单 articleId 双 CAS 恢复解决全量 backfill 会误触其他文章和浪费 Qwen 调用的问题。

## 8. SSE 流式续写、取消与错误语义

### 代码位置

- 外部业务入口：[WemediaController.java](../leadnews-wemedia-service/src/main/java/com/aaliyun/leadnews/wemedia/web/WemediaController.java#L1)
- 所有权/状态与 SSE 编排：[ArticleContinuationService.java](../leadnews-wemedia-service/src/main/java/com/aaliyun/leadnews/wemedia/service/ArticleContinuationService.java#L1)
- SSE 专用异常处理：[ContinuationExceptionHandler.java](../leadnews-wemedia-service/src/main/java/com/aaliyun/leadnews/wemedia/web/ContinuationExceptionHandler.java#L1)
- AI Prompt 与业务校验：[AiWritingService.java](../leadnews-ai-service/src/main/java/com/aaliyun/leadnews/ai/service/AiWritingService.java#L55)
- Provider 流与并发许可：[SpringAiQwenModelClient.java](../leadnews-ai-service/src/main/java/com/aaliyun/leadnews/ai/client/SpringAiQwenModelClient.java#L115)
- AI Controller：[AiInternalController.java](../leadnews-ai-service/src/main/java/com/aaliyun/leadnews/ai/controller/AiInternalController.java#L69)
- 前端解析/取消：[sse.ts](../frontend/packages/shared/src/sse.ts#L1)

### 完整链路

```text
Wemedia Editor POST continuation + Accept:text/event-stream
 → Gateway JWT/Sentinel
 → 校验稿件所有权与 DRAFT/REJECTED
 → 构造 title/current text/instruction/targetLength
 → AI Service 尝试获取 Semaphore
 → Spring AI/Qwen Flux
 → meta → 多个 chunk → done
 → Provider 异常：流内 error；同步校验异常：真实 HTTP 4xx + error frame
 → 浏览器 AbortController → Reactor cancel → doFinally 释放 permit
 → 前端只展示/插入建议；服务端不执行稿件 update
```

### 主要代码

```java
return Flux.defer(() -> {
    if (!calls.tryAcquire()) {
        return Flux.error(new AiExceptions.CapacityExceeded());
    }
    return model.stream(prompt(system, new UserMessage(user)))
        .map(x -> x.getResult().getOutput().getText())
        .filter(x -> x != null && !x.isEmpty())
        .timeout(properties.streamTimeout())
        .doFinally(signal -> calls.release()); // COMPLETE/ERROR/CANCEL 都释放
});
```

```ts
const controller = new AbortController()
const response = await fetch(url, {
  method: 'POST',
  headers: { Accept: 'text/event-stream', Authorization: `Bearer ${token}` },
  body: JSON.stringify(request),
  signal: controller.signal
})
// 用户点击取消：controller.abort()
```

### 面试讲法

选择 SSE 是因为它是单请求、服务端单向输出，不需要 WebSocket 双向会话。真实 smoke 为 29 chunks + done，调用前后稿件正文哈希一致；非法参数返回 400 且 Provider 调用为 0。

## 面试展示顺序

1. 先打开 [RESUME_PROJECT.md](RESUME_PROJECT.md)，用 100～150 字简介讲背景。
2. 选“行为链路”展示跨 Redis/Kafka/MySQL 的三个失败窗口。
3. 选“发布链路”解释为何不用分布式事务。
4. 选“AI 审核或摘要”体现版本 CAS、安全边界和成本意识。
5. 最后用阶段 15 的故障结果证明实现，而不是只罗列组件。

代码摘录为便于口述而保留的核心逻辑；完整边界、异常分支和配置应以链接中的实际源码为准。
