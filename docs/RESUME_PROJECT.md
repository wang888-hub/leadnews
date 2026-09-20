# 简历项目描述

## 技术栈

- **后端基础**：Java 21、Spring Boot 3.5.16、Spring Cloud 2025.0.3、Spring Cloud Alibaba 2025.0.0.0、MyBatis-Plus 3.5.17、OpenFeign、Flyway、Validation。
- **网关与治理**：Spring Cloud Gateway、JWT、Nacos 3.0.3、Sentinel 1.8.9、TraceId/MDC。
- **数据与中间件**：MySQL 8.4.11、Redis 7.4.11、Kafka 3.9.1 KRaft、Kafka Streams、Elasticsearch 8.17.10、MinIO、XXL-Job 3.4.2、Freemarker。
- **AI**：Spring AI 1.1.2、Spring AI Alibaba 1.1.2.2、Qwen `qwen3.8-max`、结构化输出、SSE。
- **前端与工程化**：Vue 3、TypeScript、Vite、Pinia、Element Plus、Axios、pnpm、Nginx、Docker Compose、Maven Wrapper。

## 项目核心功能

- 用户端：登录、频道与文章列表、文章详情、浏览、点赞/取消、全文搜索、高亮、联想、历史、热点与 AI 摘要。
- 自媒体端：登录、稿件与素材管理、草稿编辑、提交审核、定时发布、审核轨迹、AI SSE 续写与取消。
- 管理端：登录、频道管理、人工审核、通过/驳回和审核轨迹。
- 后端链路：稿件三级审核、Freemarker + MinIO 静态发布、Redis Lua 高频行为、Kafka 最终一致性、ES 异步索引、实时/离线热点、Sentinel 动态限流及故障补偿。

## A. 100～150 字项目简介

基于 Java 21、Spring Cloud 2025 和 Vue 3 从零重构新闻微服务平台，覆盖内容生产、审核、发布、行为、搜索、热点与 AI 增强。围绕跨组件一致性，采用 Redis Lua、Kafka、Outbox、CAS、版本号和 XXL-Job 补偿；以 DFA、Qwen 和人工兜底控制 AI 风险，并通过真实故障注入验证恢复收敛。

## B. 简历标准版（6 条）

- 基于 Redis Lua 原子更新点赞关系/计数/pending，结合 Kafka 与 eventId 唯一约束幂等落库；100 个同用户并发 LIKE 最终仅一次状态变化。
- 以 MySQL Outbox + Kafka 同步 Elasticsearch，隔离搜索故障与发布；ES 恢复后约 22.6 秒 PENDING 自动收敛并可检索。
- 设计 CAS 发布状态机、确定性 MinIO objectKey 与 XXL-Job 补偿；MinIO 恢复后精确重试约 606 ms，未误标 PUBLISHED。
- 构建 DFA + Qwen 多模态 + 人工三级审核，以 `auditVersion` 防旧结果覆盖，AI 异常统一进入人工而非默认通过。
- 实现异步 AI 摘要和可取消 SSE 续写，以 `summaryVersion`、Semaphore 与单文章 FAILED retry 控制重复、资源和调用范围。
- 使用 Sentinel Route/API Group/热点参数限流；实测 Behavior 被放行 8 次时 Redis/MySQL 增量严格为 8，429 无副作用。

## C. 详细版（8 条）

- 重建 Gateway + 8 个领域服务和 common/model/feign-api，以 JWT 信任边界、Header 清洗、internal 隔离和单向依赖解决旧工程重复网关与边界分散。
- 用 Redis Lua 同成同败地维护 LIKE/UNLIKE/VIEW 实时状态和 pending，Kafka Consumer 以 eventId 幂等、VIEW 批量持久化；Kafka 中断恢复后 pending=0、VIEW 精确 +1。
- 采用 Kafka Streams 5 秒窗口聚合刷新信号，读取绝对 counter 计算 Redis ZSet，避免重放双计数；一次行为约 1.23 秒反映到热点。
- 设计稿件审核/发布状态机，以 CAS、唯一键、事务任务、固定 MinIO objectKey 和补偿处理并发审核、重复发布及对象存储故障。
- 以 MySQL Outbox + Kafka + ES 8 Java Client 实现检索投影，支持高亮、联想、历史；ES 不可用时 Search 503 而 Article 不受影响。
- 设计 DFA + Qwen 结构化多模态 + 人工兜底，校验枚举/置信度并使用 auditVersion 隔离旧消息，避免 AI 失败转化为内容安全漏洞。
- 以异步任务生成摘要，summaryVersion CAS 防旧结果覆盖，并增加单 articleId 精确恢复；真实一次调用 6.213 秒恢复文章 17且未触发 12/14。
- 实现 SSE `meta/chunk/done/error`、Abort 传播与并发隔离；合法 smoke 29 chunks + done，数据库哈希不变，非法输入 400 且 Provider 调用 0。

## 可安全引用的环境限定指标

单台 Windows 开发机、localhost、短时且受 Sentinel 约束：Article detail 3.40 RPS、P95 45.21 ms、P99 53.22 ms；Search 2.88 RPS、P95 48.35 ms、P99 51.70 ms；九 JVM 本轮 72 个样本 Full GC=0。不得写成 2,000+ 业务 QPS、生产 SLA、百万并发或生产集群高可用。

## 代码讲解索引

每项亮点的源码位置、完整调用链、状态变化、异常恢复和主要代码摘录见 [RESUME_CODE_WALKTHROUGH.md](RESUME_CODE_WALKTHROUGH.md)。面试时建议先讲业务问题，再顺着该文档打开对应类，而不是只背技术名词。
