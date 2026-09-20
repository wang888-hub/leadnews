# Redis 行为数据设计

| Key | 类型 | 内容 | TTL |
| --- | --- | --- | --- |
| `article:like:user:{articleId}` | Set | member=userId | 无 |
| `article:counter:{articleId}` | Hash | likeCount/viewCount/commentCount/collectCount | 无 |
| `article:like:initialized:{articleId}:{userId}` | String | 单用户关系已从 MySQL 初始化标记 | 无 |
| `behavior:event:pending` | Hash | field=eventId，value=BehaviorEvent JSON | ACK 后删除 |

LIKE Lua：若 member 不存在，SADD、HINCRBY likeCount +1、HSET pending；否则不修改。UNLIKE Lua：存在才 SREM、计数减 1、写 pending；计数小于等于 0 时固定为 0。VIEW Lua：HINCRBY viewCount +1 并在同一脚本写 pending。

计数预热 Lua 仅在 counter Hash 不存在时一次性写入 MySQL 值，避免并发请求互相覆盖。点赞关系按 article/user 通过初始化标记 Lua 原子装载。Redis 写失败不返回成功；Article 详情捕获 Redis 数据访问异常，读取 MySQL 持久化统计及点赞关系。

这些 Key 不存 Article JSON。阶段 8 新增 `hot:article:global`、`hot:article:channel:{channelId}` ZSet，以及只含 channelId/publishTimeEpoch 的 `article:hot:meta:{articleId}` Hash。榜单使用绝对 ZADD 并裁剪 TopN；离线任务通过 `:rebuild:{timestamp}` 临时 Key 和 RENAME 切换。阶段 6 counter 仍是实时计数唯一写入点。
