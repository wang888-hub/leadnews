# 阶段 4 API

> 阶段 13 前端统一通过 `VITE_API_BASE_URL` 访问 Gateway；三个 SPA 不直连 518xx 服务端口。开发模式由 Vite 将同源 `/api` 代理到 Gateway，生产模式使用部署入口同源地址。前端响应模型、错误 UX、POST SSE 解析及页面对应关系见 `FRONTEND_DESIGN.md`；真实浏览器、Gateway API、SSE 完成/取消与不落库均已验收。

## 阶段 12 限流契约

Gateway Route/API Group 或 Article 热点参数触发时返回 HTTP 429 JSON。通用错误码为 `42900`，热点文章为 `42901`，均有 timestamp、traceId 和 `data:null`。认证和匿名访问语义不变。

阶段 5 新增 `GET /static/article/{id}`（公开只读 HTML，ETag/304）、`POST /api/wemedia/materials`（multipart 图片）、`DELETE /api/wemedia/materials/{id}`。内部新增 Article publish、Schedule task create 和 WmNews publish status 同步契约；Gateway 仍不开放 `/internal/**`。

更新时间：2026-09-05。外部入口为 Gateway `http://localhost:51601`。除登录白名单外均使用 `Authorization: Bearer <JWT>`；统一响应包含 code、message、data、timestamp、traceId。

## User

| 方法 | 路径 | 身份 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/user/login` | 白名单 | phone/password BCrypt 登录，签发 APP_USER JWT |
| GET | `/api/user/me` | APP_USER | 当前用户资料 |
| PUT | `/api/user/me` | APP_USER | 修改 name/image/sex |

## Behavior

| 方法 | 路径 | 认证 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/behavior/articles/{articleId}/like` | APP_USER | Lua 幂等点赞，返回 liked/likeCount/viewCount/changed |
| DELETE | `/api/behavior/articles/{articleId}/like` | APP_USER | Lua 幂等取消，计数不低于 0 |
| POST | `/api/behavior/articles/{articleId}/view` | APP_USER | Lua 增加实时浏览量 |

`GET /api/article/{id}` 增加 `likeCount`、`viewCount`、`commentCount`、`collectCount`、`liked`、`behaviorRealtime`。Redis 不可用时返回 MySQL 持久值且 `behaviorRealtime=false`；行为写接口返回 HTTP 503，不会误报成功。

## Wemedia

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/wemedia/login` | 独立自媒体账号登录 |
| POST | `/api/wemedia/news` | 创建自己的 DRAFT |
| PUT / GET / DELETE | `/api/wemedia/news/{id}` | 更新、详情、逻辑删除；校验所有权/状态 |
| GET | `/api/wemedia/news?page=1&size=10` | 自己的稿件分页 |
| POST | `/api/wemedia/news/{id}/submit` | DRAFT/REJECTED 提交至 MANUAL_REVIEW |
| POST | `/api/wemedia/news/{id}/ai/continue` | 自有 DRAFT/REJECTED 稿件真实 SSE 续写；不落库。同步校验/权限/状态失败返回真实 HTTP 4xx，正文为含统一错误码与 traceId 的 SSE `error` frame |

阶段 11：文章详情新增 `summary/summaryStatus/summaryGenerated/summaryGeneratedTime`。内部 `POST /internal/articles/summary/backfill?page=1&size=20` 分页补齐历史已发布文章摘要，不经 Gateway。

阶段 15.5：内部 `POST /internal/articles/{articleId}/summary/retry` 仅精确重开该文章当前版本的 FAILED 摘要任务；要求可信内部标记，不经 Gateway。响应含 `articleId/summaryVersion/summaryStatus/reopened`，SUCCESS 或已在处理中为幂等 no-op；不存在为 404，非 PUBLISHED 为 409。
| GET | `/api/wemedia/materials` | 自己的素材元数据；不含上传 |

稿件请求使用强类型 title、content、layout、channelId、labels、coverImages、publishTime、materialIds；content item 只接受 text/image。

## Article

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/article/channels` | 只返回 ENABLED 频道 |
| GET | `/api/article?channelId=&publishTime=&page=&size=` | 正式文章分页过滤 |
| GET | `/api/article/{id}` | 元数据、正文、summary/staticUrl |
| GET | `/api/article/{id}/comments?page=1&size=20` | 公开分页读取可见评论 |
| POST | `/api/article/{id}/comments` | APP_USER 对已发布文章发表评论；正文 1～500 字 |

评论创建结果真实写入 Article schema；当前最小闭环不包含回复、评论点赞或 AI 评论审核。前端仅使用 Vue 文本插值展示评论内容，不按 HTML 渲染。

## Admin

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/admin/login` | 管理员登录 |
| GET | `/api/admin/audit/news` | MANUAL_REVIEW 分页 |
| GET | `/api/admin/audit/news/{id}` | 审核详情 |
| POST | `/api/admin/audit/news/{id}/approve` | CAS 通过并幂等创建正式文章 |
| POST | `/api/admin/audit/news/{id}/reject` | reason 必填并记录审核 |
| GET / POST | `/api/admin/channels` | 管理员频道列表/创建 |
| PUT / DELETE | `/api/admin/channels/{id}` | 更新/逻辑删除 |
| PATCH | `/api/admin/channels/{id}/status?status=ENABLED|DISABLED` | 状态切换 |

## 内部契约

`/internal/wemedia/news/**` 供 Admin 获取/审核稿件；`/internal/articles` 供 Admin 幂等创建文章及管理频道；既有 `/internal/users/ping` 供阶段 3 链路检查。Gateway 不路由 `/internal/**`，并且服务端要求可信内部标记与正确 userType。为兼容默认 Feign HTTP client，频道状态的内部契约使用 POST，外部 Admin API 仍为 PATCH。

未引入 springdoc，避免在 Boot 3.5 主链路之外增加兼容风险；本文件是阶段 4 的固定 API 基线。
# 阶段 7 Search API

- `GET /api/search/articles?keyword=&channelId=&page=1&size=10&sort=RELEVANCE|TIME`（匿名可用，Token 可选）
- `GET /api/search/suggestions?prefix=&size=10`（匿名可用）
- `GET /api/search/history`（APP 登录）
- `DELETE /api/search/history`（APP 登录，清空本人）
- `DELETE /api/search/history/{id}`（APP 登录，只能删除本人记录）
- `POST /internal/search/rebuild`（仅服务内部，不经 Gateway）

# 阶段 8 Hot Article API

- `GET /api/article/hot?channelId=&limit=20`：匿名，limit 1..50；Redis 故障/空榜时返回最新文章并标记 `degraded=true`。
- `POST /internal/articles/batch`：body `{"articleIds":[...]}`，最大 50，只返回 PUBLISHED。
- `GET /internal/articles/hot-metadata?page=&size=&publishedAfter=`：离线重建分页元数据。
- `DELETE /internal/articles/{id}/hot-cache`：下架/删除清理钩子，不经 Gateway。
# 阶段 9 AI Internal API

以下接口只供服务间调用和开发验收，不加入 Gateway 公网路由：

- `POST /internal/ai/chat`、`/internal/ai/test/chat`：返回 model/content/requestId/latencyMs/usage。
- `POST /internal/ai/structured`、`/internal/ai/test/structured`：返回经过 schema 转换和 Bean Validation 的 `StructuredModerationResult`。
- `POST /internal/ai/vision`、`/internal/ai/test/vision`：输入 prompt、mimeType，以及 bytes（JSON base64）或 allowlist URL 二选一。
- `POST /internal/ai/stream`、`/internal/ai/test/stream`：响应 `text/event-stream`。

Key 缺失、Provider 不可用/容量满分别返回 503 类业务错误，call timeout 返回 504，结构化解析失败返回 502，图片约束错误返回 400。响应不包含 API Key 或 Provider 内部对象。

# 阶段 10 文章审核 API

- `POST /api/wemedia/news/{id}/submit` 现在返回 `SUBMITTED`，审核异步执行；详情中的 `auditVersion`、`auditSource`、`auditTrail` 可查询进度和历史。
- Admin 待审列表只包含 `MANUAL_REVIEW`；既有 approve/reject API 继续使用，人工通过后复用文章创建、静态化和定时发布。
- `POST /internal/ai/article-audit` 仅供 Feign 内部调用，不经 Gateway。请求不得包含任意公网图片 URL，只传可信 objectKey。
