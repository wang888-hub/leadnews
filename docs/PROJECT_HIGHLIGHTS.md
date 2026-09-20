# 项目亮点

## 1. Redis Lua + Kafka 行为链路

- **问题**：并发点赞存在 check-then-act 竞态；Redis 成功后 Kafka 失败又会丢持久化事件。
- **设计**：Lua 原子更新关系、计数和 pending；Kafka Consumer 以 `eventId` 唯一键幂等落库，VIEW 批量聚合。
- **理由**：Redis 承担实时状态，MySQL 保留持久真相；允许至少一次投递，但不允许重复副作用。
- **验证**：100 个同用户并发 LIKE 在 8 个请求放行时仅 1 次状态变化；Kafka 恢复后 pending 归零且 VIEW 精确 +1。

## 2. Kafka Streams + Redis ZSet 热榜

- **问题**：Lua 已更新计数，Streams 若再累加行为事件会因重放或双写放大。
- **设计**：5 秒窗口把事件聚合成刷新信号，再读取绝对计数计算 ZSet 分值；XXL-Job 用同一公式离线重建并原子 rename。
- **理由**：事件负责“何时刷新”，绝对值负责“刷新成什么”，实时与离线能够互相校准。
- **验证**：一次新 VIEW 只让 counter +1，热点分值约 1.23 秒可见。

## 3. DFA + Qwen + 人工三级审核

- **问题**：模型有成本、延迟、幻觉和解析失败风险，旧审核消息还可能覆盖编辑后的新稿件。
- **设计**：DFA 确定性短路；Qwen 多模态输出经 schema/置信度校验；异常或低置信转人工；`auditVersion` + CAS 隔离旧结果。
- **理由**：AI 是增强能力，不拥有最终业务真相；失败绝不推断 PASS。
- **验证**：DFA 命中未增加 AI 调用；文本/图片真实审核成功；缺失图片安全转人工；旧消息重放不改状态。

## 4. 发布状态机与 MinIO 补偿

- **问题**：MySQL、Freemarker、MinIO 和后续事件无法组成可靠长事务。
- **设计**：CAS 推进 `PUBLISHING`，固定 `article/{id}/index.html`，成功才置 `PUBLISHED`；失败保留状态并由 XXL-Job 精确补偿。
- **理由**：确定性 objectKey 和显式失败状态让重试幂等、可审计，不需要伪装成强一致事务。
- **验证**：MinIO 故障时文章未误标发布；恢复后 606 ms 重试成功，重复执行仍是同一 objectKey。

## 5. Elasticsearch Outbox

- **问题**：同步写 ES 会把文章发布可用性绑定到搜索集群，并存在数据库/索引双写丢失。
- **设计**：文章事务内写 MySQL outbox，Kafka 异步更新 ES；Consumer 有重试和幂等，MySQL 始终是事实源。
- **理由**：搜索投影可重建，发布主链路不应依赖它即时可用。
- **验证**：ES 停止期间文章仍发布、outbox 保持 PENDING；恢复后约 22.6 秒转 SUCCESS 并可检索。

## 6. Sentinel 无副作用限流

- **问题**：高成本 AI 和热点 API 需要分级保护，且被拒请求不能产生 Redis/Kafka/Provider 副作用。
- **设计**：Gateway Route、API Group 与 Article 热点参数规则分层，Nacos 动态持久化，统一 429 + traceId。
- **理由**：限流放在业务逻辑前；总体容量、接口成本和单参数热点是不同维度。
- **验证**：Behavior 20 次为 8 个 200/12 个 429且增量=8；AI QPS=0 时 5/5 返回 429、Provider 指标不增加。

## 7. 可取消 SSE 续写

- **问题**：流式 Provider 慢且昂贵，客户端断开可能泄漏连接/许可；同步校验错误曾被内容协商放大为 500。
- **设计**：`meta/chunk/done/error` 契约、Abort/cancel 传播、Semaphore bulkhead；SSE 专用异常 advice 保留真实 HTTP 状态；结果不自动保存。
- **理由**：续写只是用户可编辑的建议，资源生命周期必须与连接一致。
- **验证**：合法调用 29 chunks + done，正文哈希不变；非法 targetLength 返回 400、Provider 调用 0；取消后下一请求成功。

## 8. AI Summary 精确恢复

- **问题**：摘要失败不应回滚发布；全量 backfill 会误触其他文章并增加调用费用。
- **设计**：独立任务/outbox、`summaryVersion` CAS、有限重试与受保护的单 articleId FAILED 重开。
- **理由**：把 AI 可用性从核心发布剥离，同时保留最小作用域的运维恢复入口。
- **验证**：短文下限修复后文章 17 一次 Provider 操作、6.213 秒转 SUCCESS；文章 12/14 全程未变化。
