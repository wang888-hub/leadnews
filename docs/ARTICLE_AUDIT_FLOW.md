# 文章审核链路（阶段 10）

## 设计结论

原项目采用“同步敏感词 + OCR/第三方审核 + 直接改状态”的实现。本项目保留敏感词前置和人工兜底思想，但改为可恢复异步链路：提交事务同时递增 `audit_version` 并写 `wm_news_audit_task`；Dispatcher 投递 Kafka；Consumer 先 CAS 领取任务，再执行 DFA 和 Qwen 多模态审核。原项目代码仅只读参考，没有复制。

`DRAFT/REJECTED -> AUDITING -> APPROVED|REJECTED|MANUAL_REVIEW`。提交事务直接把文章置为 AUDITING、递增 `auditVersion` 并创建 AuditTask；不存在依赖扫描补建任务的 SUBMITTED 窗口。自动通过后复用阶段 5 的即时/定时发布流程；人工通过也复用同一路径。确定性的规则/置信度结果可直接进入人工审核；AI 基础设施异常先做有限任务级重试，耗尽后才进入 `MANUAL_REVIEW`，绝不默认通过。

## 决策顺序

1. DFA trie 扫描标题、标签和正文文本；命中即 `DFA/REJECT`，不调用 AI。
2. 只提取 content 中 `type=text` 的文本；所有正文图和封面 URL 必须能映射到稿件关联的 `wm_material.object_key`。
3. AI Service 只从可信 MinIO bucket/prefix 读取对象字节，不接受业务侧任意 URL；限制正文 20,000 字、最多 5 图和单图大小/MIME。
4. Qwen 返回固定结构：`decision/riskLevel/reason/confidence/riskTags`。PASS 需置信度 `>=0.85`，REJECT 需 `>=0.90`，否则人工复核。
5. 每个最终结果以 `news_id + audit_version + audit_stage` 唯一记录。最终事务同时校验文章 `auditVersion` 以及任务的 `RUNNING + attemptNo`，旧稿结果和同稿旧执行实例均无法覆盖新状态。

## 一致性与恢复

- 提交和任务写入同一 MySQL 事务，避免“文章 AUDITING 但无任务”。AuditTask 本身同时承担审核事实和 Kafka 投递前可靠存储，不另建 audit outbox。
- 任务执行状态为 `PENDING -> RUNNING -> SUCCESS|FAILED|STALE`，Kafka 投递使用独立 `dispatch_status=PENDING|SENT`，不再把 SENT 混入任务生命周期。发送成功只更新 dispatchStatus/lastDispatchedAt；只有 Consumer claim 才进入 RUNNING。
- Kafka key 使用 newsId；Consumer group 为 `leadnews-wemedia-audit-v1`，有限重试后进入 `leadnews.wemedia.audit.DLT`。
- `event_id`、`(news_id,audit_version)` 唯一约束防重复建任务；Consumer 对 eventId 执行单条 SQL `PENDING -> RUNNING, attempt_no+1` CAS，防止 Kafka 重复消息和多实例竞争重复调用 AI。
- `auditVersion` 仅在用户修改后重新提交时递增；`attemptNo` 仅在 claim 成功时递增，并作为当前执行 fencing token。最终 Task/Article/audit_record 本地事务必须同时匹配当前 attemptNo；旧实例迟到结果更新行数为 0。
- 已知 AI 异常在 `attemptNo < 3` 时把同一个任务、eventId CAS 回 PENDING，并重置 dispatchStatus 供 Dispatcher 重发。第三次仍失败则任务 FAILED、文章 MANUAL_REVIEW 并写 AUTO_AUDIT_EXHAUSTED 记录。
- RUNNING 超过 90 秒视为疑似僵尸，每 30 秒扫描一次。恢复按 `id + RUNNING + attemptNo` CAS 回 PENDING 并重置投递状态；不创建新任务、不递增 auditVersion。90 秒来自 AI 单次 30 秒、一次内部 retry（最大 60 秒）再加退避、本地处理和安全余量。
- 审核轨迹统一存放在 `wm_news_audit_record`。Admin 原有 `ad_audit_record` 只为阶段 4 向后兼容，详情展示以统一轨迹为准。

## 可观测性与隐私

指标包括 `audit.dfa.reject`、`audit.ai.pass`、`audit.ai.reject`、`audit.ai.review`、`audit.ai.failure` 和 AI latency。日志仅记录 newsId、version、错误类别、模型元数据和 traceId，不记录正文、完整 Prompt、图片/base64、API Key 或 Provider 原始敏感响应。

## 已验证

- DFA 命中稿件 15：`REJECTED/DFA_REJECTED`，AI 调用增量为 0。
- 文本稿件 18：Qwen `PASS/LOW`（0.98），最终发布；图片稿件 19 从 MinIO 读取字节，Qwen `PASS/LOW`（1.0），最终发布。
- 稿件 20 的版本 2 指向不存在 objectKey，记录 `AI/REVIEW/ServiceUnavailable`；管理员可驳回、重新提交，也可人工通过并发布。
- 重放稿件 19 的已完成事件及稿件 20 的旧版本事件后，状态和审核记录数不变。
- 并发提交稿件 21 返回一次 HTTP 200、一次 HTTP 409，数据库只有一个版本 1 任务。
