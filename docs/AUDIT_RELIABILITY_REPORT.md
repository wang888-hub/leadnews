# 文章自动审核可靠性报告

## 任务、投递与并发

1. 提交入口是 `WemediaService.submit`。
2. Article 进入 `AUDITING`、`auditVersion+1` 与 AuditTask 插入处于同一 MySQL `@Transactional`。
3. AuditTask 字段为 id/eventId/newsId/auditVersion/status/attemptNo/dispatchStatus/dispatchRetryCount/nextDispatchTime/lastDispatchedAt/startedAt/finishedAt/lastError 及审计时间。
4. V4 保留 `event_id`、`(news_id,audit_version)` 两个唯一约束。
5. `AuditTaskDispatcher` 只扫描已有 PENDING 任务并发 Kafka；发送成功仅标 SENT，不改 RUNNING。
6. `AuditTaskMapper.claim` 是单 SQL `PENDING -> RUNNING, attempt_no=attempt_no+1` CAS。
7. 重复 Kafka 消息和并发实例只有一个 affectedRows=1，其余直接返回。
8. attemptNo 只在成功 claim 时增加；审核侧 capacity/circuit 尚未调用 AI 时延期会原子回退该次占用，不消耗执行预算。
9. auditVersion 只在稿件修改后重新提交时增加，任务重试不增加。
10. RUNNING timeout 为 90 秒。
11. Recovery 每 30 秒扫描，使用 status+attemptNo CAS 恢复原任务。
12. maxAttempts 为 3，耗尽后 Task FAILED、Article MANUAL_REVIEW。

## DFA 多实例更新

13. `SensitiveWordRegistry.initialize` 从 MySQL enabled 词构建首个 Trie。
14. 全局版本在 `wm_sensitive_word_version`；实例本地版本在 `AtomicLong`。
15. `SensitiveWordDictionaryService` 一次批量事务只递增一次版本，提交后通过 Redis Pub/Sub 广播版本号。
16. 未使用同 consumer group 的 Kafka 广播，因为组内一条消息只交付一个实例。
17. 每次先创建完整不可变 `SensitiveWordMatcher`，完成后 `AtomicReference.set`。
18. AtomicReference 位于 `SensitiveWordRegistry.matcher`；业务先取得 Snapshot，因此切换前请求可完成旧版本。
19. 每 30 秒从 MySQL 校验全局版本，弥补 Redis 通知丢失；MySQL 是最终真相源。
20. V6 为 `wm_news_audit_record` 增加 `sensitive_word_version`，DFA/AI 最终记录保存实际快照版本。

## 图片与模型输入

21. 上传端限制 5MB、真实魔数/MIME，并在写入 MinIO 前通过 ImageReader 仅读头部校验默认最多 2000 万像素；文章关联阶段限制最多 5 张，AI 拉取层再次去重并限制 5 张。无法由当前 JVM ImageIO 验证尺寸的格式会拒绝上传；原图不修改。
22. 审核图最长边默认 1280，统一 JPEG，初始质量 0.8，超限时有限降质到 0.3。
23. MinIO 每张图单独打开/关闭流，使用有界 `readNBytes`；处理完当前原图即释放，仅累计压缩小图，不再 `readAllBytes` 五张原图。
24. 单原图 5MB、单审核图 512KB、审核图总量 2MB、估算请求 3MB；Base64 按 4/3 计入。
25. 最多 5 张压缩图作为一次 `structuredVision` 请求发送，不拆成多次 Provider 调用。

## 输出校验与决策

26. DTO 是 `ArticleAuditResult(decision enum,riskLevel enum,reason,confidence,riskTags)`，没有 articleId。
27. Bean Validation 与 `AiAuditResultValidator` 双层校验 confidence 必须在 [0,1]。
28. 枚举由 Jackson/Java enum 限制；Validator 额外拒绝 PASS+HIGH、REJECT+LOW、空/过长 reason 和异常 tags。
29. 最终状态只由 `AuditDecisionPolicy` 决定，模型信号不直接映射数据库状态。
30. PASS/REJECT 阈值分别位于 `audit.ai.auto-pass-confidence-threshold`（0.85）和 auto-reject（0.90）。
31. `AuditThresholdCalibrator` 可读取 aiDecision/confidence/manualDecision CSV，扫描阈值并输出覆盖率、人工率、误通过/误拒绝率与 confidence 分桶准确率。

## 超时、熔断、降级与隔离

32. Provider 单次 timeout 30 秒。
33. maxRetries=1，即首次加最多一次，仅 timeout、连接错误和部分 5xx/429 等瞬时错误重试；一次任务 attempt 内完成。timeout 分支已有独立“两次调用封顶”测试。
34. `AuditAiGuard` 为轻量 CLOSED/OPEN/HALF_OPEN 熔断器：连续 5 次远端失败 OPEN 30 秒，参数全部配置化。
35. HALF_OPEN 默认只允许 1 个探测；成功 CLOSED，失败重新 OPEN。
36. 人工积压以 MANUAL_REVIEW 文章数实时统计，默认上限 500。
37. 熔断时普通稿件保持 AUDITING，Task 延迟 30 秒回 PENDING。
38. 高风险/紧急标签稿件仅在人工未满时转 MANUAL_REVIEW，否则同样延期；基础设施错误绝不直接拒稿。
39. Wemedia 审核实例 `Semaphore(4)`，AI Service 原有 `AiCallExecutor` 也保持 Provider 并发隔离。
40. 第 5 个请求最多等待 50ms；失败归类 AI_CAPACITY_BUSY，Task 延迟 2 秒、回退未执行 attempt，JVM 不建无界队列。
41. AI_CAPACITY_BUSY、AI_REMOTE_FAILURE、INVALID_AI_RESPONSE 使用不同异常路径；Provider 50312/50210 也映射到对应类别。非法输出最多两轮任务执行后转人工。
42. 最终结果仍由 `AuditTaskCoordinator.complete` 本地事务按 Task RUNNING+attemptNo 与 Article auditVersion+AUDITING 双 CAS，随后写 audit_record；任一步异常回滚。

## 验证与边界

43. 新增 Validator、阈值统计、熔断/并发隔离、不可变 DFA 快照、上传像素、超过五图、缩放压缩、timeout 有限重试和 V6 migration contract 测试；原有提交回滚、唯一约束、并发 claim、恢复、fencing、旧版本与最终事务测试继续保留。
44. 最终 JDK 21 全量 `clean verify` 退出码 0；Wemedia 39 项、AI 18 项测试均为 0 failure/0 error，详见 `PROGRESS.md`。
45. 已知边界：熔断为单实例状态而非全局状态；高优先级目前依赖稿件标签；Redis Pub/Sub 不持久化但 30 秒 MySQL 校验兜底；标准 ImageIO 对超大压缩炸弹仍会在解码当前单图时占用内存，已用 5MB 源文件和 1280 输出限制降低风险；阈值工具需业务提供带人工终局标签的 CSV。
