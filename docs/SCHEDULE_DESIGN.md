# 动态发布调度

MySQL `leadnews_schedule.schedule_task` 是事实来源；Redis ZSet `schedule:publish:future` 只作时间索引。member 为 taskId，score 为 executeTime epoch millis。唯一键 `(task_type,business_id)` 防止重复任务。

状态枚举：WAITING、READY、RUNNING、SUCCESS、FAILED、CANCELLED。Worker 从到期 ZSet 取 taskId，并用数据库状态 CAS 获取执行权；成功置 SUCCESS，失败有限次数重排。多实例只有一个能 claim。

启动时从 MySQL WAITING/READY/FAILED 重建 ZSet，所以 Redis 丢失不会丢任务。Schedule 只通过 OpenFeign 调用 Article publish，不访问 Article DB、模板或 MinIO。

精确动态时间由 Schedule worker 负责，不为每篇文章创建 XXL-Job。Schedule 每秒只从 Redis ZSet 有限读取最多 100 个已经到期的 taskId；成功原子移除 ZSet member 的实例才把任务交给有界线程池。线程池默认 core=4、max=8、queue=200，各任务继续通过 MySQL 状态 CAS 独立领取，所以单篇文章失败不会终止或阻塞整批文章。

线程池拒绝任务时立即把 taskId 放回 ZSet。服务启动时分页从 MySQL 恢复全部 WAITING/READY 任务；运行期间每分钟恢复未来一分钟内缺失的任务，并把超过 5 分钟的 RUNNING Lease 恢复为 WAITING。因此 Redis 仍是高频到期索引，MySQL 仍是可靠事实源。所有批大小、线程数、队列容量、扫描/对账/Lease 时间均可用 `leadnews.schedule.*` 或对应环境变量配置。

XXL handler `articlePublishCompensation` 仅周期扫描 Article 侧的 PUBLISH_FAILED 与超时 PUBLISHING；用 `XXL_JOB_ENABLED` 开启并配置 5–10 分钟固定频率。XXL-Job 不参与每篇文章的准点触发。

XXL-Job 执行器日志目录由 `XXL_JOB_EXECUTOR_LOG_PATH` 配置，本地默认 `./logs/xxl-job`，避免 Windows 根盘目录权限问题。管理端与执行器必须配置相同的 `XXL_JOB_ACCESS_TOKEN`；示例文件仅提供占位值。阶段 5 已验证执行器 `leadnews-article-publish` 向 Admin 注册返回 `code=200, msg=Success`，验证后已停止测试启动的 Article 进程。
