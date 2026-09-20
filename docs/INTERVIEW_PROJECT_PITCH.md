# 面试项目讲稿

## 30 秒版

这是一个用 Java 21、Spring Cloud 2025 和 Vue 3 重构的新闻平台。重点不是 CRUD，而是跨组件故障下的数据正确性：发布用状态机、CAS、outbox 和补偿；行为用 Redis Lua、Kafka 和幂等；AI 审核用 DFA、Qwen 和人工兜底，摘要与 SSE 有独立失败边界。最后我通过真实故障注入验证恢复收敛。

## 1 分钟版

旧教学项目已有新闻业务和若干中间件，但技术栈老、网关重复、一致性与恢复路径薄弱。我没有复制代码，而是重建 Gateway 和八个领域服务。核心发布链路用 MySQL 状态机、确定性 MinIO objectKey 和任务补偿；点赞浏览用 Redis Lua 原子更新、Kafka 异步持久化和 eventId 去重；搜索用 outbox 与发布解耦。AI 方面以 DFA 先筛、Qwen 结构化多模态审核、低置信或异常转人工，摘要失败不影响发布，SSE 续写可取消且不自动保存。项目做过 Redis、Kafka、ES、AI、MinIO 真实故障测试，所有性能数据都保留单机和 Sentinel 边界。

## 3 分钟版

这个项目是一个资讯微服务平台，覆盖用户、自媒体创作、审核、发布、行为、搜索、热点、后台管理和 AI 增强。重构的原因不是简单追新版本，而是旧项目的多个网关、RabbitMQ/Kafka 混用、固定虚拟机地址和薄弱一致性不适合作为完整工程展示。所以我使用 Java 21、Spring Boot 3.5、Spring Cloud 2025 重建模块，基础设施放 Docker Desktop，Java 服务保留在 Windows JVM/IDEA。

整体入口是 Gateway，它校验 JWT、删除客户端伪造用户头、拒绝 internal 路径，并用 Sentinel 做 Route、API Group 和热点参数限流。业务服务间通过 OpenFeign 或 Kafka，不能直接 Maven 依赖。

最难的三个问题，第一是发布一致性。审核通过后用 CAS 进入发布中，Freemarker 生成静态页并写固定 MinIO objectKey，成功才置 PUBLISHED；搜索和摘要由 outbox/任务异步触发，失败有显式状态和 XXL 补偿。第二是高频行为。Lua 原子维护关系、计数和 pending，Kafka 用 eventId 幂等落库；Streams 不再累加事件，而是读取绝对计数刷新热点，避免双计数。第三是 AI 的不确定性。DFA 先处理确定规则，Qwen 输出必须结构化校验并达到置信门槛，否则转人工；auditVersion 和 summaryVersion 阻止旧结果覆盖。

整个系统没有使用分布式事务，而是组合本地事务、CAS、唯一键、outbox、Kafka、重试、确定性对象键、版本和补偿来获得可恢复最终一致性。AI 只是增强能力：审核失败转人工，摘要失败不改变已发布状态，续写只返回建议且取消会释放资源。

可靠性不是靠看 health 声明的。我实际停止 Redis、Kafka、ES、AI 和 MinIO，核对 HTTP 语义、pending/outbox、状态机、lag 和恢复后的数据。例如 ES 恢复后约 22.6 秒可检索，MinIO 恢复精确重试约 606 ms。当前结果来自本地单机短时测试，不代表生产容量；生产化还需要服务身份、集群、集中可观测、备份和长时 soak。

## 5 分钟版提纲

1. 30 秒讲业务和重构动机。
2. 45 秒讲 Gateway + 8 服务、公共契约和 Windows/Docker 拓扑。
3. 60 秒讲审核到发布、MinIO、搜索 outbox、摘要任务。
4. 45 秒讲 Lua、Kafka、pending、eventId、Streams 绝对计数。
5. 45 秒讲 AI schema/置信度/人工、summaryVersion、SSE cancel。
6. 30 秒讲 Sentinel、JWT 信任边界和 secret 外置。
7. 30 秒讲故障注入和真实性能口径。
8. 15 秒主动交代非生产边界并邀请追问。

收尾句：我能为每条关键链路回答谁是真相源、哪里会重复、失败留下什么证据、如何只恢复目标数据，以及如何证明最终收敛。
