# 阶段 15 性能与可靠性测试报告

测试时间：2026-09-10。以下结果只代表单台 Windows 开发电脑、localhost Gateway 和本地 Docker Desktop，不代表生产容量。

## 环境与方法

| 项目 | 实际值 |
| --- | --- |
| OS | Windows 11 家庭中文版 10.0.26200 |
| CPU | AMD Ryzen 7 7840HS，8C/16T |
| 宿主内存 | 27.82 GiB；基线采集时可用 5.79 GiB |
| Docker Desktop | Engine 28.1.1，16 vCPU，13.55 GiB 内存上限 |
| JDK | Temurin 21.0.12.1，默认 G1 |
| JVM | 初始堆约 448 MiB，MaxHeap 约 6.96 GiB/进程，未显式固定 Xmx |
| 测试工具 | Node.js 24.20.0 内置 fetch；脚本位于 `performance/scripts` |
| 拓扑 | Client → Gateway 51601 → Windows JVM 服务 → Docker 基础设施 |

正式持续测试使用 30 秒 warm-up + 60 秒记录。行为幂等为固定请求数的状态验证，不作为持续吞吐基准；测试前等待 Sentinel 一秒窗口复位，并用 `stage15_` 专用文章 14。Sentinel 保持真实规则：Article route 20、public-read 15、Behavior/write 8、Search 5、AI group 1、articleDetail 单参数 5 QPS。

## HTTP 性能结果

| Scenario | Concurrency | Requests | Success | 429 | 5xx | RPS（总请求） | P50 | P95 | P99 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| Article 正常区间（250 ms think） | 1 | 205 | 205 | 0 | 0 | 3.40 | 37.76 ms | 45.21 ms | 53.22 ms |
| Article 闭环饱和 | 1 | 3,977 | 296 | 3,681 | 0 | 66.28 | 15.26 ms* | 29.16 ms* | 38.73 ms* |
| Article 闭环饱和 | 5 | 126,759 | 295 | 126,464 | 0 | 2,112.58 | 2.13 ms* | 3.14 ms* | 8.20 ms* |
| Search 正常区间（300 ms think） | 1 | 173 | 173 | 0 | 0 | 2.88 | 39.02 ms | 48.35 ms | 51.70 ms |
| Search 闭环饱和 | 1 | 4,014 | 300 | 3,714 | 0 | 66.89 | 30.45 ms** | 37.21 ms** | 40.06 ms** |

\* Article 饱和行的延迟包含大量快速 429，只用于观察 Sentinel，不可当作业务成功延迟。单参数规则约放行 5 QPS；5 并发后 99.77% 为 429，因此按约束停止 10/20 并发。

\** Search 饱和行展示放行请求延迟；规则稳定放行 300/60s，即约 5 QPS。总请求 RPS 不能描述为 Search 业务吞吐。

Search 的普通关键字、channel filter、TIME/RELEVANCE 与高亮契约均先做功能检查并返回 200。Gateway 总延迟可测；当前没有独立 ES query timer，因此不虚构 ES 内部延迟。

## Behavior、Lua、Kafka 与热点

| 场景 | 请求 | 200 | 429 | 最终结果 |
| --- | ---: | ---: | ---: | --- |
| 同一用户并发 LIKE | 100 | 8 | 92 | 仅 1 次状态变化；likeCount +1、LIKE event 1 |
| 100 个不同合成测试身份 LIKE | 100 | 8 | 92 | likeCount +8、active relation +8、LIKE event +8 |
| 100 个不同身份 VIEW | 100 | 8 | 92 | Redis/MySQL viewCount 均 +8，pending=0 |
| 同一用户混合 LIKE/UNLIKE | 100 | 8 | 92 | 4 POST/4 DELETE，5 次真实状态变化；最终 counter=active relation=8，非负 |

100 并发已产生 92% 预期限流，故没有执行 200 并发。Kafka `leadnews-behavior-persistence` 三分区 lag 最终均为 0；Redis pending 在正常链路为 0。一次新 VIEW 使 counter 精确 +1，热点 ZSet 分值约 1.23 秒内变化，Streams 没有二次写 counter。

## SSE / AI（温和测试）

| 并发 | 放行 | 429 | Chunks | First token | Total |
| ---: | ---: | ---: | --- | --- | --- |
| 1 | 1 | 0 | 28 | 1,571 ms | 3,152 ms |
| 2 | 1 | 1 | 31 | 563 ms | 2,585 ms |
| 4 | 1 | 3 | 33 | 579 ms | 2,767 ms |

复验并发 4 时 AI 请求指标只从 5 增至 6，证明 3 个 429 未进入 Provider。取消测试在首块 2,154 ms 后 Abort 成功；随后 27 chunks/done 的请求成功，permit 已释放。以上延迟主要包含外部 Qwen，不代表 Java 服务纯处理能力。

## JVM / GC / 数据库观察

60 秒 Article 低速读期间采集 72 个 JVM 样本：所有九服务 Full GC 均为 0。峰值：Article RSS 559.29 MiB/141 threads，Behavior 537.13 MiB/142 threads，Gateway 471.62 MiB/178 threads；其余服务峰值 RSS 350–481 MiB。Article 在采样窗累计 CPU 6.64 秒，Behavior 5.97 秒，Gateway 1.40 秒。没有线程持续爆炸或 Full GC 证据。

MySQL `Threads_connected=81`、`Max_used_connections=81`；slow query log 为 OFF，`Slow_queries=0`。Article detail 的文章主键和内容 `uk_article_content_article` 均为 const lookup。Search history 按 userId 使用索引但排序显示 filesort；当前每用户只有极少记录，尚无真实慢查询证据，因此不增加索引。

## 瓶颈与优化结论

- 首个可见上限是 Sentinel 本地功能阈值，不是 CPU、ES 或数据库极限。
- 九个 JVM 都继承约 6.96 GiB 理论 MaxHeap，合计承诺远高于宿主内存；实际 RSS 本轮可控，但长期开发建议启动脚本显式限制堆并做更长 soak test。
- 81 个 MySQL 连接对本地环境偏高，未来可按服务负载缩小 Hikari pool，但本轮无连接耗尽/慢 SQL，故不盲改。
- SSE 参数校验错误在 `text/event-stream` 内容协商下返回 HTTP 500，而不是预期 400/error event，是功能缺陷，不是性能优化。
- 本阶段没有业务性能参数修改，因此没有伪造 before/after。唯一修改是修复测试采样脚本，不能计作系统优化。
