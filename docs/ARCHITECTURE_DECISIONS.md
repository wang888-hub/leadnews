# 架构决策记录

## ADR-01：不用分布式事务

- **Context**：发布跨 MySQL、MinIO、Kafka、ES、AI，无法可靠纳入一个长事务。
- **Decision**：MySQL 真相源，组合本地事务、CAS、outbox/任务、幂等和补偿。
- **Alternatives**：Seata/TCC、同步级联调用、人工修库。
- **Trade-offs**：接受短暂不一致并增加状态表；换来故障隔离、可观察和精确恢复。

## ADR-02：行为先写 Redis 再 Kafka

- **Context**：LIKE/VIEW 高频且要求即时反馈，直接同步多表写放大明显。
- **Decision**：Lua 原子写实时关系/计数/pending，再发 Kafka 幂等落 MySQL。
- **Alternatives**：数据库同步写、分布式锁、只写 Kafka。
- **Trade-offs**：低延迟且可削峰；需处理 Redis 故障、pending 重投和数据校准。

## ADR-03：搜索采用 Outbox

- **Context**：同步写 ES 会将发布可用性绑定搜索集群并产生双写窗口。
- **Decision**：文章事务写 outbox，Kafka 异步更新 ES，MySQL 为权威源。
- **Alternatives**：同步双写、定时全表扫描、CDC。
- **Trade-offs**：发布解耦且事件不丢；搜索存在短暂延迟，需要重试/重建。

## ADR-04：Kafka Streams + Redis ZSet 热点

- **Context**：需要秒级响应行为变化，也需要离线校准；直接事件累加会在重放时漂移。
- **Decision**：Streams 5 秒窗口发刷新信号，读取绝对计数计算 ZSet；XXL-Job 同公式原子重建。
- **Alternatives**：普通 Consumer 逐事件 ZINCRBY、纯定时批处理、ES 聚合。
- **Trade-offs**：低延迟且防双计数；依赖 Redis counter，并要维护实时/离线一致公式。

## ADR-05：AI 审核失败转人工

- **Context**：模型会超时、限流、幻觉或解析失败，错误 PASS 风险不可接受。
- **Decision**：DFA 先行；Qwen schema + 置信度门槛；任何不确定或失败进入 `MANUAL_REVIEW`。
- **Alternatives**：失败默认通过、失败默认拒绝、完全人工。
- **Trade-offs**：安全与可审计优先；牺牲部分自动化率并保留人工成本。

## ADR-06：AI Summary 异步

- **Context**：摘要是增强字段，Provider 延迟/费用不应阻塞文章发布。
- **Decision**：发布后任务/outbox 触发，summaryVersion CAS；有限自动重试后支持单文章精确恢复。
- **Alternatives**：发布事务同步调用、前端每次即时生成、仅全量 backfill。
- **Trade-offs**：核心发布可用；摘要会暂时缺失并需要任务运维。

## ADR-07：SSE 而不是 WebSocket

- **Context**：续写是单请求、服务端单向增量响应，不需要持久双向会话。
- **Decision**：POST fetch + SSE 事件流，支持 AbortController/cancel 和 Nginx 禁缓冲。
- **Alternatives**：WebSocket、轮询、一次性 HTTP 响应。
- **Trade-offs**：协议更简单且首块更早；需手写 POST 流解析和专用错误契约。

## ADR-08：Java 本地运行，中间件 Docker

- **Context**：最终开发方式要求 Windows IDEA 调试，同时淘汰 VMware/固定 IP 和手工中间件。
- **Decision**：Gateway + 8 服务运行 JDK 21；七项基础设施 Docker；Nginx 只验收 production dist。
- **Alternatives**：全 Docker、全宿主安装、继续虚拟机。
- **Trade-offs**：调试直接且基础设施可复现；需严格区分 Host/容器地址，当前不是生产部署。
