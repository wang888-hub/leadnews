# 文章发布链路

## 原项目对照

原项目在 Article 中用 Freemarker 生成页面并通过公共 `FileStorageService` 上传 MinIO，`ap_article.static_url` 保存结果；自媒体用 `publishTime` 创建延迟任务，Schedule 以 Redis ZSet/List 加数据库 task 表调度。它证明了模块边界，但静态化示例存在测试代码承载业务、随机/扁平对象名、Redis 状态偏重、发布状态与失败恢复不完整等问题。

新项目保留 Freemarker、MinIO、动态任务和 Article/Schedule 职责分离；改为 MySQL 为任务真相源、Redis 仅作时间索引，发布使用 CAS、确定性对象键、有限重试与超时恢复。未复制原项目业务代码。

## 当前流程

`WmNews MANUAL_REVIEW → APPROVED` 后创建或确认唯一 Article。`publishTime` 为空或已到期时立即调用发布；未来时间则 Article 为 WAITING，并创建唯一 `schedule_task`。

发布状态：`APPROVED/WAITING → PUBLISHING → PUBLISHED`；失败为 `PUBLISHING → PUBLISH_FAILED`。超时 PUBLISHING 或未超过次数的失败记录可重新 claim。WmNews 同步演进为 `APPROVED → PUBLISHING → PUBLISHED/PUBLISH_FAILED`。

ArticlePublishService 查询内容 DTO，通过 `article.ftl` 渲染，上传 `article/{articleId}/index.html`，保存 objectKey、兼容 URL与发布时间。文本使用 Freemarker HTML escape；图片 URL 仅作为 `img src` 输出。未来支持富文本时必须引入独立白名单净化，不得关闭转义。
