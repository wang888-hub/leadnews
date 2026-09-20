# 点赞链路一致性（2026-09-17）

## 边界与版本

- `eventId` 是单次有效操作的 UUID；Redis pending 保存 Lua 生成的不可变 JSON，补偿原样重发，不再次执行 Lua。
- `relationVersion` 的范围是 `(articleId,userId)`。Redis 初始化时从 MySQL `article_like.relation_version` 读取；每次关系真正变化时才在 Lua 内递增。旧版本不能覆盖新关系。
- `likeCount/collectCount` 是操作完成后的 Redis 绝对计数。每篇文章的点赞与收藏分别有单调 `countVersion`；它与单用户 `relationVersion` 是两条独立版本线，不能混用。Consumer 只在 incoming countVersion 更大时用事件绝对计数覆盖 MySQL。
- `heatDelta` 由行为发生时的 Lua 根据三态关系和冷却期决定；Kafka Streams 只按 articleId 和事件时间窗口累加这个值，不重新猜测首次或重复行为。

## 顺序与失败窗口

```text
HTTP POST/DELETE like
  -> Java 生成 eventId 和 occurredAt
  -> 初始化 Redis 缺失的计数/关系/关系版本
  -> Lua 原子检查当前关系
     -> 状态无变化：返回 changed=0，不递增版本、不写 pending、不发 Kafka
     -> 状态变化：更新三态 Hash/cooldown ZSet -> 更新 likeCount
        -> 分别 INCR relationVersion 与文章 countVersion
        -> HSET pending[eventId] = 完整绝对状态事件 JSON
  -> Producer 从 pending 读取原 JSON 并发送 Kafka
     -> Broker ACK 成功才 HDEL pending[eventId]
     -> 发送失败保留 pending；5 秒补偿任务重发同一 JSON
  -> Kafka Consumer（record ACK 模式、auto commit=false）
     -> MySQL 事务：INSERT IGNORE consumed_event(group,eventId)
        -> 已存在：跳过业务
        -> 首次：INSERT IGNORE behavior_event_log
           -> relationVersion CAS 保存绝对 liked 状态
           -> countVersion CAS 保存绝对 likeCount
     -> MySQL COMMIT -> listener 返回 -> Spring Kafka 提交 offset
```

数据库异常会回滚 `consumed_event`、事件日志、关系和统计，并抛出给 Spring Kafka 原有重试/DLT 处理器；不会在事务内手动提交 offset。若数据库提交后、Kafka offset 提交前进程中断，同 eventId 再投递时 `consumed_event` 唯一键使业务成为 no-op。多实例同时扫描 pending 可能重复发送，但事件幂等表消除重复业务效果；Redis pending 发送 ACK 前丢失不影响已写入 Kafka 的事件。

`consumed_event` 默认保留 30 天（`BEHAVIOR_CONSUMED_EVENT_RETENTION_DAYS`，下限 7 天），每天 03:00 按 `consumed_time` 索引最多删除 10 批、每批 500 条。保留期需要持续大于 Kafka retention 与运维重放窗口；如扩大 Kafka 保留期，必须同步提高该配置。

## 现存限制

- Redis 与 Kafka 之间没有跨系统原子事务；Redis pending 依赖 Redis 持久化和定时补偿。Redis 数据整体丢失且尚未发送 Kafka 的事件仍可能丢失。
- `relationVersion` 抵御同一用户关系乱序，`countVersion` 独立抵御文章绝对计数乱序。二者都由同一次 Redis Lua 原子生成，但分别控制自己的 MySQL 字段。
- V7 之前产生的无 countVersion LIKE/COLLECT 消息无法安全覆盖绝对计数；新 Consumer 将其拒绝并交给 DLT 审计。升级前应确认旧 pending/lag 已清空，或对 DLT 作人工核对，不能盲目重放。
- 当前 DLT 策略在有限重试后保存失败消息并交由人工恢复；这与无限期不提交 offset 不同。恢复时必须保持原 eventId 和版本。
