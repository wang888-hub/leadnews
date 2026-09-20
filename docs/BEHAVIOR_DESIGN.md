# 高频行为设计

## 原项目分析

原项目 `heima-leadnews-behavior` 将点赞、取消点赞、阅读分别保存在 Redis Hash，Java 先读取再判断、随后写 Redis，并把计数增量发送到热点文章 Topic。收藏位于 Article 模块，Kafka Streams 还承担热点计算。主要问题是“判断 + 修改”不是原子操作，并发点赞可能重复加分；Kafka send 没有 ACK 失败补偿；事件没有 eventId 和数据库消费日志，至少一次投递会重复累计；Redis JSON 行为记录和文章热点计算耦合，边界不清晰。

## 新方案

当前 Behavior Service 实现 LIKE、UNLIKE、COLLECT、UNCOLLECT、VIEW；COMMENT 仍属预留。Gateway 注入 userId，客户端不能提交 userId。点赞/收藏使用三态 Hash（1 当前有效、2 已取消且在冷却期、nil 未有效且不在冷却期）及每文章冷却 ZSet，不使用 HEXPIRE。Lua 原子更新单用户状态、文章计数、关系版本和 Redis pending outbox，立即返回实时值；Kafka 异步传递包含 `heatDelta` 的统一 `BehaviorEvent`。旧点赞 Set 的成员在访问时惰性迁移，并由低频有界 SCAN 迁移任务逐步排空，不再写入旧 Set。

默认 1 小时冷却：首次点赞 +2，取消 -1，冷却内重新点赞 +1；首次收藏 +5，取消 -2，冷却内重新收藏 +2。Lua 对过期的 state=2 做惰性删除；独立清理任务通过活跃文章索引与有界批次，原子确认 state 仍为 2 才删除，避免覆盖已经重新互动的状态。行为请求 Lua 仅做单用户 O(1) 操作；清理与旧 Set 迁移任务不属于请求路径。

MySQL Consumer 使用两条互相独立的单调版本线：`relationVersion` 只保护单用户点赞/收藏关系，`countVersion` 只保护文章最终计数。Redis Lua 在同一次原子行为变更中递增相应文章的 countVersion，并把最终 `liked/collected`、最终 `likeCount/collectCount` 及两个版本写入 immutable pending 事件。Consumer 在同一 MySQL 本地事务中先以 `(consumer_group,event_id)` 去重，再分别执行关系 CAS 和计数 CAS；计数通过事件携带的绝对值覆盖，禁止 `count = count + delta`。因此乱序事件不会用旧绝对值覆盖新计数，`heatDelta` 也从不参与数据库业务计数。

点赞关系的 MySQL UPSERT 只有真实状态迁移才更新 like_count，因此 Redis 丢失后从 MySQL 预热不会造成再次点赞放大。UNLIKE 在 Lua 和 SQL 两层均使用下限 0。VIEW 不做用户去重：Consumer 只把唯一事件持久化为待聚合状态，1 秒事务批量锁定最多 1000 条、按 articleId 汇总更新、再标记已应用；没有不可恢复的进程内聚合。

## 状态、失败与一致性

- 实时真相：Redis；持久化事实：MySQL `article_behavior_stat`、`article_like`、`article_collect`、`behavior_event_log`。
- Redis 成功、Kafka 失败：事件仍在 pending Hash，5 秒周期重发，ACK 后删除。
- Consumer 失败：抛出异常，由 `DefaultErrorHandler + FixedBackOff(1s, 2)` 有限重试，耗尽后发布 DLT并写 `behavior_failed_event`。
- Redis 不可用：写行为返回 HTTP 503；Article 详情通过 Behavior 的 MySQL fallback 返回 `behaviorRealtime=false`。
- Redis 恢复或 Key 缺失：初始化 Lua 用 `SETNX/HSETNX` 从 MySQL 同时预热计数和 countVersion；单用户关系及 relationVersion 由独立 Lua 原子预热。已存在的实时值不会被数据库旧值覆盖。
- 周期 reconciliation 只补齐缺失计数，不覆盖存在的实时值，避免把尚未消费的 Redis 增量回滚。

热点计算只消费 `BehaviorEvent.heatDelta`，与上述 MySQL 严格业务计数同步解耦；完整链路见 `HOT_ARTICLE_DESIGN.md`。
