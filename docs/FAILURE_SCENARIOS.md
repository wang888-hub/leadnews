# 阶段 9 AI 故障场景

## 阶段 15 真实故障注入结果（2026-09-10）

| Dependency | Action | Observed / Expected | Recovery & convergence | Result |
| --- | --- | --- | --- | --- |
| Redis | `docker stop leadnews-redis` | Behavior write 503；Article 200 且 realtime=false；Hot 200/degraded=true；Search 200 | start 后 healthy；Article 自动预热并 realtime=true | PASS |
| Elasticsearch | stop ES 后发布 stage15 稿件 | Search 503；Article/Wemedia 200；发布成功；search outbox=PENDING | healthy 后约 22.6 秒 Search=200，outbox=SUCCESS，文章可检索 | PASS |
| Kafka | stop Kafka 后 VIEW + submit | VIEW 200、Redis pending=1；稿件 SUBMITTED，事件未丢 | healthy 后约 2.45 秒稿件 PUBLISHED，pending=0，VIEW MySQL +1 | PASS |
| AI Service | 停止本任务 AI JVM | SSE 200 + error event；普通 Article/Wemedia 200；审核转 MANUAL_REVIEW；人工通过仍 PUBLISHED | AI 恢复 UP；已耗尽 3 次的摘要保持 FAILED，未自动重开 | PARTIAL：需精确恢复能力 |
| MinIO | stop MinIO 后提交稿件 | 稿件/文章为 PUBLISH_FAILED，retry=1、objectKey 为空，没有误标 PUBLISHED | healthy 后精确重试 606 ms，最终 PUBLISHED；固定 `article/18/index.html`，重复发布幂等 | PASS |
| Behavior restart | 安全停止/启动本机 JVM | Nacos 注销后重新注册 | 22.12 秒 UP，pending/计数保留 | PASS |
| Article restart | 同上 | Nacos 注销/注册 | 26.80 秒 UP | PASS |
| Search restart | 同上 | 即时检查早于注册完成，最终健康检查 9/9 | 约 17 秒 UP，搜索 200 | PASS |
| Schedule restart | 同上 | Nacos 注销/注册 | 20.87 秒 UP | PASS |

所有 Docker 故障均只 stop/start 明确的本项目容器，未删除容器、Topic、索引、Bucket 或 Volume。没有执行 flushall、truncate、drop 或全量数据清理。

## 阶段 14 本地拓扑故障

- 端口已占用：启动脚本直接失败并要求指定其他端口，不终止已有进程。阶段 14 实际因 5173/5174 被占用改用 5176/5177/5179。
- Java 已启动但 Nacos 少实例：运行 `scripts/health-check.ps1`，检查对应 `.stage14-logs`；只重启缺失服务，不整体重建 Docker。
- Vite 可用、Nginx API 失败：先确认 Windows Gateway 51601，再检查 Docker 到 `host.docker.internal` 的可达性。
- Nginx SSE 一次性返回：确认续写 location 位于普通 `/api/` 之前，且 `proxy_buffering off`、`gzip off`、`proxy_cache off`。
- MySQL/Kafka 等容器健康但 Windows Java 失败：Windows 只使用 Host 端口；容器内部 service name 只供容器使用。
- 401、403、409、429、503 分别代表未认证、角色不符、状态冲突、Sentinel 限流、依赖能力不可用；前端不得把它们统一显示为网络错误。Like/View 只有成功响应后才更新本地状态，AI 503 不影响稿件编辑。

## 阶段 12 Sentinel / Nacos

- Nacos 暂时不可用：客户端继续使用最后加载规则，JWT 与业务请求不逐次依赖 Nacos，恢复后自动重订阅。
- Gateway Block：直接 429 JSON，不进入 Behavior Lua/Kafka 或 AI Provider。
- Article Block：相同 articleId 独立限流，其他参数值保有配额；Route 仍负责总体容量。
- Sentinel 不替代既有降级：AI 审核仍转人工、热点 Redis 仍降级最新文章、ES 异常仍返回 503。

- `API-KEY` 缺失：服务仍启动，provider capability 为 `NOT_CONFIGURED`，调用明确返回 503；不返回假成功。
- Provider 连接失败、429、5xx：仅按配置做有限 retry（默认额外 1 次），耗尽映射 503。400、401/403、无效参数不重试。
- Call/stream timeout：取消 Future/Flux，映射 504 或流错误，并在 `doFinally` 释放并发许可。
- Structured output 为普通文本、缺字段、未知 decision 或 confidence 越界：converter + validation；失败映射 502，绝不推断 PASS。Markdown code fence 已验证可解析。
- 并发上限耗尽：立即 503，不建立无界等待队列。
- 图片 MIME/大小/来源不合法：调用 Provider 前返回 400；URL host 必须显式允许。
- Provider health 不做真实模型调用；真实 smoke 由 internal endpoint 手动触发，避免 health probe 产生费用。
# 阶段 10 审核故障

- AI Key 缺失、超时、限流、Provider 失败、结构化解析失败、图片不存在/MIME 或大小不合法：统一记录错误类别并转 `MANUAL_REVIEW`，不自动 PASS。
- Kafka 发送失败：任务保留 PENDING 并指数/定时重试；Consumer 异常有限重试后进入 DLT。
- Consumer 崩溃：过期 RUNNING 任务回到可投递状态；已完成事件重放由 CAS/唯一键忽略。
- 编辑重提：新 `audit_version` 是唯一有效版本，旧版本结果/消息即使延迟到达也不能改变当前状态。

# 阶段 11 摘要与续写故障

- 摘要 enqueue、Kafka、AI、解析或回写失败均不改变 PUBLISHED；任务有限重试，耗尽保留 FAILED 供 XXL/backfill 恢复。
- 重复事件由任务终态忽略；旧版本由 summaryVersion CAS 标记 STALE；超时 RUNNING 可恢复。
- SSE Provider 失败发结构化 error；断开传播 cancel 并释放 bulkhead，后续请求可继续成功。
- Key 缺失时 AI Service 保持健康但能力 503，不生成假摘要或假续写。

## 阶段 15.5 可靠性缺口收尾

- SSE 同步校验/权限/状态错误：`Accept: text/event-stream` 时由专用 advice 返回真实 4xx/5xx 和一个可解析 `error` frame，避免 JSON converter 二次内容协商形成 500。正常流中 Provider 异常仍保持既有 HTTP 200 + `error(50320)` 降级语义。
- 非法 targetLength、空输入和非法路径变量均在 Provider 调用之前失败。阶段 15.5 真实 Gateway 验证 targetLength=80 返回 HTTP 400，响应含业务码 40000 和 traceId；AI Service 在该请求前后均未注册 `ai.continuation.requests` 指标，证明调用数为 0。
- 摘要耗尽重试：调用受保护的 `POST /internal/articles/{id}/summary/retry`，只把该文章当前版本的 FAILED 文章/任务双重 CAS 重置为 PENDING。无需全量 backfill；SUCCESS/处理中重复调用为 no-op，旧 summaryVersion 仍由 Consumer CAS 隔离。
- 首次文章 17 恢复 smoke 精确隔离了文章 12/14，但三次既有自动尝试均因短正文与固定 80 字下限冲突而返回 50210，最终保持 FAILED。已统一 AI Prompt 与 Article 回写的短文自适应下限；该修复不放宽 160 字上限，也不改变正常长度文章的 80 字下限。
- 修复后的最后一轮文章 17 恢复只调用 Provider 1 次，6.213 秒内完成 `PENDING/PENDING/0 -> GENERATING/RUNNING/0 -> SUCCESS/SUCCESS/0`，生成 27 字摘要；文章 12/14 的 FAILED、summaryVersion 和 retryCount 全程不变。没有执行 backfill，也没有第三轮人工重开。
