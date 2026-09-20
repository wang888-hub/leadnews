# 可用于简历的阶段 15 数据

以下表述必须保留“本机开发环境”和 Sentinel 条件，不得外推为生产容量。

- 在 Windows 11、Ryzen 7 7840HS 8C/16T、27.82 GiB、Docker Desktop 13.55 GiB、JDK 21 的单机环境中，Gateway Article detail 在约 3.4 RPS 正常区间完成 205/205 请求，P95 45.21 ms、P99 53.22 ms，无 5xx。
- Search 在约 2.88 RPS 正常区间完成 173/173 请求，P95 48.35 ms、P99 51.70 ms；5 QPS Sentinel 下 60 秒精确放行约 300 个请求，其余返回 429、无 5xx。
- 100 个并发同用户 LIKE 在 Sentinel 放行 8 个请求的情况下最终仅产生一次状态变化；Redis count、MySQL relation/aggregate 与事件日志一致。
- 100 个不同测试身份 LIKE/VIEW 各有 8 个请求放行、92 个 429；最终 Redis/MySQL 增量均严格为 8，证明被限流请求无副作用。
- Kafka 中断时 Behavior pending 保留 1 条；恢复后 pending 归零、VIEW 增量精确为 1，稿件审核/发布约 2.45 秒收敛。
- 一次 Behavior 事件到 Redis 热点 ZSet 更新在本机约 1.23 秒内可见，且 article counter 只增加一次。
- AI SSE 并发 4 时 Gateway 仅放行 1 个、3 个返回 429，AI 请求指标只增加 1；成功流首 Token 约 0.58–1.57 秒、总时长约 2.6–3.2 秒。
- 60 秒读测试的 72 个 JVM 样本中九服务 Full GC 均为 0；Article/Gateway 峰值 RSS 约 559/472 MiB。
- Redis、Elasticsearch、Kafka、AI、MinIO 单点故障均按既有状态机降级或保留可恢复记录；Redis/ES/Kafka/MinIO 恢复后核心数据收敛。

## 不可写入简历的结论

- 不可写“支持 2,000+ QPS”：该数字几乎全部是 Gateway 快速返回的 429。
- 不可写“支持百万并发”或“达到生产级 SLA”：测试为 localhost 单机、60 秒窗口，没有网络、集群、长时 soak 或容量冗余。
- 不可把 Qwen 首 Token/总时延描述为 Java 服务吞吐。
- 不可声称 ES 内部 P99、Kafka producer latency 或数据库 slow-query 分布；当前没有对应精细 timer/slow log 数据。
- 不可声称所有 AI 失败自动恢复：摘要任务耗尽最大重试后需要 backfill，而现有 backfill 缺少按 articleId 精确恢复入口。

## 阶段 15.5 更正（不改动阶段 15 原始数据）

阶段 15 的原始性能与故障注入结果保持不变。其后阶段 15.5 已增加受保护的单 articleId 摘要精确重试，解决“只能全量 backfill”的运维缺口；这不代表摘要无限自动重试，达到上限后仍需要显式运维调用。SSE 非法参数的 `text/event-stream` 内容协商 500 也已修复为带业务码和 traceId 的 HTTP 400 SSE error frame。

阶段 15.5 最终真实 smoke：合法 SSE 为 29 chunks、done=1、首块 2424 ms、总计 3568 ms且正文不落库；短文下限修复后，文章 17 精确恢复在 6.213 秒内一次 Provider 操作成功，文章 12/14 未触发。这些是功能恢复验证，不是新的性能基准，不替换上面的阶段 15 原始数据。
