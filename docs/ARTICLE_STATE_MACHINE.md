# WmNews 状态机

更新时间：2026-09-05

```text
DRAFT ──提交──> SUBMITTED ──进入人工队列──> MANUAL_REVIEW
  ^                                           │       │
  │                                           │通过   │驳回(reason必填)
  └────────────── 修改后可再次提交 <── REJECTED      APPROVED
```

| 当前状态 | 合法目标 | 操作者 | 失败条件 |
| --- | --- | --- | --- |
| DRAFT | SUBMITTED | 稿件所有者 | 非所有者、并发修改 |
| REJECTED | SUBMITTED | 稿件所有者 | 非所有者、并发修改 |
| SUBMITTED | MANUAL_REVIEW | Wemedia 应用服务 | CAS 未命中 |
| MANUAL_REVIEW | APPROVED | Admin 经内部 Feign | 非 Admin/非内部调用、已被另一审核抢占 |
| MANUAL_REVIEW | REJECTED | Admin 经内部 Feign | reason 为空、非 Admin/非内部调用、CAS 未命中 |

仅 DRAFT/REJECTED 可修改或删除。状态更新使用 `UPDATE ... WHERE status = expected`，因此 approve 与 reject 并发时只有一个能成功。APPROVED 的重复 approve 返回已有稿件快照；Article 再以 `wm_news_id` 唯一约束返回同一 articleId，形成跨服务重试幂等。

枚举还预留 AUTO_REVIEW、PUBLISHED、FAILED。未来 AI 阶段在 SUBMITTED 后插入规则/AI 审核，低置信或异常转 MANUAL_REVIEW；发布阶段再从 APPROVED 演进到发布中/成功/失败语义。阶段 4 不调用 AI、不静态化，也不创建搜索索引。
# 阶段 10 审核状态扩展

稿件提交状态现为 `DRAFT|REJECTED -> AUDITING -> APPROVED|REJECTED|MANUAL_REVIEW`，提交事务同步递增 `audit_version` 并创建 AuditTask，不再留下已提交但任务尚未创建的中间窗口。发布侧继续使用 `WAITING/PUBLISHING/PUBLISHED/PUBLISH_FAILED`。只有 `DRAFT/REJECTED` 可再次提交；自动结果同时以 auditVersion 和 AuditTask attemptNo CAS，旧稿消息与旧执行实例均不能回写。
