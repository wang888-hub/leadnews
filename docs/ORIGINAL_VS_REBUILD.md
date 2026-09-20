# 原项目与重构项目对照

## 边界声明

原项目 `D:\Toutiao\project\heima-leadnews` 始终只读。新项目依据领域分析重新实现，没有复制业务代码。下表区分“原项目已有思路”和“本次新增工程能力”，避免把教学项目已有功能包装成重构创新。

| 维度 | 原项目 | 新项目 | 重构价值 |
| --- | --- | --- | --- |
| 技术基线 | 旧版 Spring Cloud/Java 生态 | Java 21、Boot 3.5、Cloud 2025、Vue 3 | 统一受管理版本与现代 API |
| 工程结构 | 多套公共包与三个重复网关 | 单 Gateway、公共/模型/Feign 契约、8 服务 | 减少重复与循环依赖 |
| 配置发现 | Nacos 与固定 VM 地址混用 | Nacos + localhost/Docker service name 分层 | 去除 VMware 与固定 IP |
| 消息 | RabbitMQ 与 Kafka 并存 | Kafka/Kafka Streams 单一主线 | 简化运维与语义 |
| 发布 | 已有 Freemarker、MinIO、延时任务思路 | 显式 CAS 状态机、确定性 objectKey、失败状态和精确补偿 | 可观察、可恢复、幂等 |
| 行为 | Redis/Kafka 但原子性和幂等薄弱 | Lua + pending + eventId 唯一键 + VIEW 批量 | 重复与故障下可收敛 |
| 搜索 | ES 7.2 旧客户端，Mongo 历史/联想 | ES 8 Java API + MySQL 历史 + outbox | 发布解耦、减少存储种类 |
| 热点 | Kafka Streams + Redis/定时任务基础 | 变化信号 + 绝对计数 + 统一评分 + 原子重建 | 避免双计数并可校准 |
| 审核 | DFA 基础能力 | DFA + Qwen 多模态 + 人工兜底 + auditVersion | AI 异常不误判，旧结果不覆盖 |
| AI 内容 | 无完整 Spring AI 业务闭环 | 摘要任务、精确恢复、SSE 续写/cancel | AI 与核心发布隔离 |
| 网关安全 | 多网关、边界分散 | JWT、可信头清洗、internal 404 | 身份入口统一 |
| 流控 | 缺乏可验收的动态治理 | Sentinel Route/API/热点 + Nacos datasource | 动态规则与无副作用 429 |
| 前端 | 三套构建产物/旧技术实现 | 三套 Vue 3 + TS 源码与 shared 包 | 类型、错误和 SSE 契约统一 |
| 验证 | 侧重功能演示 | 全量构建、真实链路、故障注入、受限压测 | 结论可复查且有边界 |

## 保留的合理领域思想

用户、自媒体、文章、行为、搜索、调度和管理边界；Freemarker 静态化、MinIO、ES、Kafka、Kafka Streams、XXL-Job、DFA 等技术方向来自原系统分析。重构贡献在于重新实现、收敛技术栈并补足状态、幂等、安全、AI 和恢复机制。

## 有意未做

未复制旧表与全部接口，未引入 RabbitMQ、MongoDB、IK、Seata、Kubernetes；未实现评论域；未把 Java 服务容器化。当前 internal header 也没有被描述成生产级服务身份认证。
