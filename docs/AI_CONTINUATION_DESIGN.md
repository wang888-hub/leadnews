# 自媒体 AI 流式续写设计

`POST /api/wemedia/news/{id}/ai/continue` 接收 `instruction/targetLength/currentContent`，返回 `text/event-stream`。仅已鉴权 WEMEDIA 作者可操作自己的 DRAFT/REJECTED；越权 403、不可编辑 409、越界输入 400。

该能力遵循 draft-only：输出只返回客户端，不更新 `wm_news`，不提交、审核或发布。输出仍是不可信普通文本；服务端转义可执行 script 标签，前端仍需按文本插入并由编辑确认。

Prompt 位于 `article-continuation-system.st`、`article-continuation-user.st`，要求延续风格、不重复、服从 instruction/长度、只输出正文。Wemedia 用 WebClient 订阅 AI Service 的真实 Spring AI Flux。

SSE 顺序：`meta(requestId/model)`、多个 `chunk(text)`、`done(finishReason/chunkCount)`；失败发送 `error(code/message)`。客户端断开会取消上游 Flux，AI 层 `doFinally` 在 complete/error/cancel 均释放 Semaphore。默认流超时 60 秒、并发 4，并限制 instruction、输入和目标长度以控制成本。

## 阶段 15.5：SSE 请求期错误契约

阶段 15 实测发现，`Accept: text/event-stream` 下的同步参数、权限或状态异常先进入 JSON 全局异常处理器，随后因端点只生产 SSE 而再次发生内容协商失败，最终把原本的 400 放大为 500。阶段 15.5 增加仅作用于 `WemediaController` 且仅匹配 `text/event-stream` 的异常处理器。

请求尚未建立成功的同步错误现在保留真实 HTTP 状态（400/401/403/404/409/429/5xx），响应媒体类型仍为 `text/event-stream`，正文是单个 `error` frame，其 `data` 为统一 `ResponseResult` JSON，包含业务错误码和 traceId。这样同时满足 HTTP 语义和 SSE 客户端可解析性；JSON 端点仍由原全局异常处理器处理。

正常流的 `meta/chunk/done/error`、客户端取消和不落库语义未改变。输入校验发生在构造 WebClient Provider 请求之前，因此非法 targetLength、空 instruction/currentContent 或非法 articleId 不会调用 AI Provider。
