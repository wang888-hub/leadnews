# MySQL 与 MinIO 最终一致性

不使用 Seata/TCC。Article 先 CAS 进入 PUBLISHING，再渲染并 PUT 确定性对象，最后 CAS 写入 objectKey 并成为 PUBLISHED。

- MinIO 失败：记录脱敏 lastError、retryCount+1 并置 PUBLISH_FAILED，完整异常写日志。
- MinIO 成功、DB 更新失败：重试覆盖相同 objectKey，不产生重复对象。
- PUBLISHING 崩溃：超过 timeout 后允许补偿重新 claim。
- Feign 超时/重复调度/重复审核：状态 CAS、文章与任务业务唯一键、确定性 objectKey 共同幂等。
- 已 PUBLISHED 直接返回；超过 max-retries 保持 PUBLISH_FAILED，禁止无限重试。

本地验收实际停止 MinIO 后确认 PUBLISH_FAILED/retryCount=1，恢复后重试成功；模拟超时 PUBLISHING 后也重新 claim 成功。
