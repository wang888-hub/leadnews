# Kafka Streams 热点链路（2026-09-17）

## 前后差异

旧链路：BehaviorEvent → 5 秒无 grace 的刷新信号 → `leadnews.hot.refresh` → Consumer 读 Redis 绝对行为计数 → 发布时间衰减公式 → ZADD。离线任务按 MySQL 累计行为数覆盖榜单。

新链路：带 `channelId` 的 BehaviorEvent → `occurredAt` TimestampExtractor → 按 articleId 分组 → 5 秒 Tumbling Window + 10 秒 grace → `hot-score-window-v3` 保存 `HotScoreAggregate(channelId,deltaScore)` → suppress 窗口最终结果 → 确定性 `windowEventId=articleId:windowStart:windowEnd` → Redis Lua 原子检查 `hot:article:applied` 并 ZINCRBY → 每分钟独立 Lua 衰减 ×0.99 → 每 30 分钟把 Redis 分数和 MySQL 业务计数写入完整快照。旧 `HotScoreRefreshEvent`、刷新 Consumer、刷新 Topic 的运行配置已移除；历史 Kafka Topic 和 Flyway V3/V5 表不自动删除。旧 `HotScoreCalculator` 保留在 common 供历史参考；当前增量权重集中于 `HotScoreDeltaCalculator`。

## 事件、窗口、权重

沿用 `leadnews.behavior.events` 和稳定 Streams application-id `leadnews-hot-score-stream-v1`、现有 state-dir。Extractor 优先解析 JSON 的 `occurredAt`；无效时回退 Kafka record timestamp，再回退 partition time。Kafka Timestamp 不用于正确事件的业务窗口归属。5 秒窗口用 `TimeWindows.ofSizeAndGrace(PT5S,PT10S)`。显式 Window State Store `hot-score-window-v3` 的 key 是 `Windowed<articleId>`，value 是 JSON Serde 的 `HotScoreAggregate`；Kafka Streams 自动维护 changelog。旧 V2 Store 的 Double changelog 与新类型不兼容，因此只改 Store 名，不改 application-id。suppression buffer 最多 10,000 条，满时关闭任务等待恢复，绝不提前输出非最终值。事件时间窗口要靠后续记录推进 stream time 才会关闭，因此空闲分区的输出可能晚于墙上时钟 15 秒。

VIEW 默认 +1、COMMENT 默认 +5，仍由 `leadnews.hot` 配置。LIKE/COLLECT 的首次、冷却内重试、取消热度由 Behavior Service Lua 的 `leadnews.behavior.*-heat` 配置决定，并随 immutable `BehaviorEvent.heatDelta` 进入 Kafka；Streams 对这两类事件直接求和，不重新判断“第一次”。点赞默认 +2/-1/+1，收藏默认 +5/-2/+2。Streams 不建无限 eventId 去重 Store；同一个 Kafka Topic 的 MySQL Consumer 用 `consumed_event` 严格幂等，两条消费链各有独立 offset。

## Redis 与衰减

沿用项目原有 Key：`hot:article:global`、`hot:article:channel:{id}`、`article:hot:meta:{id}`、`hot:article:channels:active`；新增 `hot:article:applied` ZSet。行为发生时先从 Redis metadata 获取频道 ID，缺失则经 Article 内部 batch API 补查已发布文章并缓存；同一份 immutable `channelId` 进入待发送事件与 Kafka，窗口关闭时不再查库。`hot:article:applied` 以 windowEventId 为 member、windowEnd 毫秒为 score。Lua 在同一原子执行中检查是否已应用、更新 global/channel、裁剪 TopN、写 applied；重复窗口直接返回 0。低于 `minScore=0.01` 删除成员；阈值由原来的 1 降低，使首次互动留下的 1 分可按 1→0.99→0.9801 自然衰减。

`hot:article:applied` 默认保留 7 天，整点后第 15 分钟定时按 windowEnd 删除过期 ID。超过保留期才重放旧窗口时，Redis 已无法识别重复，可能再加一次；保留期必须大于实际 Kafka 重试/恢复范围。当前 Redis Compose 为单实例，Lua 多 Key 可用；若未来迁移 Redis Cluster，这些 Key 可能跨 hash slot，必须在迁移前统一 hash tag 或重新设计同槽键，不能直接复用现脚本。

每分钟的独立定时任务使用分钟级 Redis NX 锁避免多实例重复衰减；每个 ZSet 由 `hot-decay.lua` 原子读取/乘以 `coolingFactor=0.99`/删除低分成员。Streams 增量 Lua 与衰减 Lua 由 Redis 串行执行，不存在 Java GET→计算→ZADD 覆盖，但分钟边界先增量后衰减或反之会有可接受的微小差异。`HotScoreRedisWriter` 失败会记录异常并使 Streams 任务报错/恢复，不影响独立的 MySQL 行为消费。

## MySQL 快照与近似恢复

Flyway V7 新增 `article_hot_snapshot_batch` 和 `article_hot_snapshot`。每个批次先写 `BUILDING`，逐条保存当前有效 global/频道 TopN 的 `articleId`、`channelId`、`hotScore`、like/collect/comment/view count 与 `snapshotTime`，全部成功后才改为 `COMPLETE`；恢复永远只选择最新 COMPLETE 批次，因而不会把半成品当基线。周期由 `leadnews.hot.snapshot-enabled`、`snapshot-interval`（默认 PT30M）和 `snapshot-retention-days` 配置。旧 `hot_score_checkpoint` 保留作历史迁移记录，不再承担当前恢复。

业务 count 和分数一起保存，是为了在 Redis 榜丢失后用“当前 MySQL count - 快照 count”估计快照后的热度变化。正向 like/collect 分别按首次权重 +2/+5，负向差值按取消权重 -1/-2；COMMENT/VIEW 只估算正向增长，使用当前系统权重。该算法只用于容灾，实时链路始终直接累加事件的 immutable `heatDelta`。

设快照分数为 S，距快照 m 分钟，计数差值估算热度为 D。恢复分数为 `S * 0.99^m + D * 0.99^(m/2)`：S 在整个区间内存在所以衰减完整时间；D 实际散布在区间内但缺少精确时间，因此用区间中点近似。计数净差无法还原“100 次点赞、90 次取消”这样的真实路径，所以这是有意的近似恢复；核心关系和计数仍由 MySQL 严格恢复，派生榜单的误差会随实时新事件和衰减逐渐淡化。

恢复分页读取快照（每页 200），批量写独立临时榜 `hot:article:global:recovering:{id}` 与频道临时榜。开始时 Lua 仅在正式 global 为空时以 NX 获取 `hot:article:recovery:flag`。恢复期间，窗口增量 Lua 发现该 flag 后把新分数写入 recovery delta ZSet，而不是正式榜。完成后一个 Redis Lua 将每个 delta 合并到对应临时榜、裁剪低分和 TopN、删除旧正式 key、RENAME 临时 key，并在最后释放 flag；因此旧恢复值不会覆盖恢复期间的新实时热度。异常或 10 分钟所有权过期时切换会拒绝执行，正常窗口写入恢复到正式榜。

当前 Docker Redis 是单实例，上述多 key Lua 具备原子性；Redis Cluster 会受跨 slot 限制，迁移前必须统一 hash tag 或重构切换协议。没有 COMPLETE 快照时不伪造榜单，只等待后续实时事件。恢复是破坏性运维动作，正式榜非空时自动拒绝，避免误覆盖健康数据。

Kafka Streams 重启时从 changelog 恢复窗口 State Store，并从未提交 offset 继续消费；窗口末尾 + grace 内的迟到记录仍可聚合，超过时由 Streams 丢弃。`exactly_once_v2` 只覆盖 Kafka offset、State Store/changelog 与 Kafka 内部事务，不覆盖 Redis。若 ZINCRBY 成功但 Streams 提交前宕机，重放的相同 windowEventId 由 Redis Lua 跳过；Redis applied 标记过期或 Redis 榜与 applied 数据不一致时仍可能漂移。不同窗口的 deltaScore 加法可交换，但与分钟衰减的先后顺序可造成轻微差异。MySQL 关系和绝对计数由独立严格消费者的 eventId、relationVersion、countVersion 保护。升级前已有不带 channelId/countVersion 的 pending/Kafka 旧事件不能由新 Consumer 正常处理，应在部署前确认 lag/pending 为零或做受控迁移。
