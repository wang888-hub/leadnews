# STAR 项目案例

## 1. Kafka 故障恢复
- **Situation**：Redis 行为写入后 Kafka 被停止，数据库可能落后。
- **Task**：事件不丢，恢复后不重复计数。
- **Action**：Lua 同步写 pending；eventId 唯一键幂等消费；恢复后观察 lag、pending 与 MySQL。
- **Result**：pending 由 1 归零，VIEW 精确 +1；稿件事件约 2.45 秒收敛。
- **Reflection**：至少一次不是问题，缺乏幂等和可观察待办才是问题。

## 2. Redis 故障降级
- **Situation**：停止 Redis 后，实时行为、文章计数和热榜受到不同影响。
- **Task**：不能伪造写成功，读链路尽量可用。
- **Action**：行为写明确 503；Article 降级 MySQL 并标记 `realtime=false`；Hot 返回降级结果，恢复后自动预热。
- **Result**：Redis 恢复 healthy 后 Article 自动回到 `realtime=true`，未直接修改数据。
- **Reflection**：降级策略必须按读写语义区分，写失败不能用假成功掩盖。

## 3. ES Outbox 恢复
- **Situation**：ES 停止期间提交并发布测试稿件。
- **Task**：发布不依赖 ES，同时确保恢复后可检索。
- **Action**：发布事务留存 search outbox，Search 返回 503，Consumer 保留 PENDING 并重试。
- **Result**：Article/Wemedia 正常；恢复后约 22.6 秒 outbox SUCCESS，文章可检索。
- **Reflection**：可重建投影不应成为核心写链路的同步依赖。

## 4. MinIO Publish Failed 恢复
- **Situation**：发布中停止 MinIO，不能留下误标已发布的半成品。
- **Task**：保存失败证据并幂等恢复。
- **Action**：CAS 状态机、成功后才 PUBLISHED、确定性 objectKey、XXL 精确补偿。
- **Result**：先为 PUBLISH_FAILED 且 objectKey 为空；恢复后 606 ms 成功，固定为 `article/18/index.html`。
- **Reflection**：显式失败状态比回滚成草稿更利于审计和恢复。

## 5. SSE 500 根因与修复
- **Situation**：`text/event-stream` 请求的非法参数被 JSON 异常处理器二次内容协商成 500。
- **Task**：恢复真实状态码并确保 Provider 零调用。
- **Action**：增加仅匹配 SSE 的 advice，输出统一 error frame、业务码和 traceId，并补契约测试。
- **Result**：targetLength=80 返回 400；AI 指标无新增，Provider 调用 0。
- **Reflection**：流式接口既有 HTTP 建连语义，也有流内错误语义，不能套普通 JSON 异常处理。

## 6. AI Summary FAILED 精确恢复
- **Situation**：文章 17 自动三次失败；全量 backfill 会触发文章 12/14 和额外费用。
- **Task**：只恢复文章 17，并解决短文与固定 80 字下限冲突。
- **Action**：双 CAS 单文章重开；统一 Prompt/回写的短文自适应下限，保留 160 上限和正常文本 80 下限。
- **Result**：一次 Provider 操作、6.213 秒转 SUCCESS、摘要 27 字；12/14 状态、版本和重试数不变。
- **Reflection**：恢复入口必须最小作用域；校验规则也必须与业务输入尺度一致。

## 7. 热点双计数风险
- **Situation**：行为链路已更新 Redis counter，Streams 再按事件增量会翻倍。
- **Task**：快速更新榜单且支持消息重放和离线校准。
- **Action**：事件只作为 refresh signal，评分读取绝对计数；离线任务同公式、临时 Key 原子替换。
- **Result**：一次 VIEW counter 仅 +1，热点约 1.23 秒可见。
- **Reflection**：事件表达“发生变化”与表达“权威数值”必须明确区分。

## 8. 识别虚假高吞吐
- **Situation**：5 并发 Article 得到 2,112.58 总 RPS，看似亮眼。
- **Task**：判断能否作为简历性能结论。
- **Action**：拆分成功/429/5xx并结合 Sentinel 参数 5 QPS 分析。
- **Result**：99.77% 为快速 429，拒绝宣传；改用 205/205、P95 45.21 ms 的正常区间数据。
- **Reflection**：性能报告的第一责任是解释指标语义，而不是挑最大数字。
