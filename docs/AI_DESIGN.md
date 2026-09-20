# AI 基础服务设计（阶段 9）

阶段 11 新增真实文章摘要与 SSE 续写，继续复用同一 `AiModelClient`、timeout/retry/bulkhead、Key 隔离和安全日志；领域流程见 `AI_SUMMARY_DESIGN.md`、`AI_CONTINUATION_DESIGN.md`。

## 架构与版本

调用边界固定为 `Business Service -> OpenFeign -> leadnews-ai-service -> AiApplicationService -> AiModelClient -> SpringAiQwenModelClient -> Spring AI ChatModel -> DashScope`。业务模块不依赖 DashScope SDK。Spring AI 为 1.1.2，Spring AI Alibaba 为 1.1.2.2，模型为 `qwen3.8-max`。

当前实际 Provider Adapter 是 Spring AI Alibaba `DashScopeChatModel`，并启用 `multi-model=true`。2026-09-08 实测 text、structured、PNG bytes vision、stream 均成功，未出现 Adapter 与 `qwen3.8-max` 的多模态兼容问题。因此没有启用 fallback。若将来确认 Alibaba Adapter 的请求格式与新模型不兼容，仅允许在 AI Service 内改用 Spring AI OpenAI-compatible Adapter；`AiModelClient`、Feign 和业务层均不得变化，且切换前后都要真实验证。

## 能力

- Text：统一系统 Prompt，返回 model/content/requestId/latency/nullable usage，不暴露 Provider 响应。
- Structured Output：使用 `BeanOutputConverter` 的 schema 指令，再做 DTO 反序列化和 Jakarta Validation；普通文本、缺字段、未知枚举或非法 confidence 抛 502，绝不默认 PASS。
- Vision：业务层只传 `AiImageInput`。支持 jpeg/png/webp 的 bytes 或 allowlist URL，必须二选一；默认单图、5 MiB。URL 仅允许 http/https 和显式 host allowlist，避免任意 SSRF。
- Streaming：独立 SSE internal endpoint；基于 `ChatModel.stream()` 按 chunk 转发，客户端取消会触发 Reactor `doFinally` 并释放并发许可。

## 韧性、安全与观测

同步调用使用虚拟线程 Future + 30 秒 call timeout；stream timeout 默认 60 秒；超时映射 504。额外 retry 默认 1 次，仅连接/Socket 超时、429 和 5xx；400、认证和参数错误不重试。Semaphore 默认最多 4 个并发模型调用，满载立即 503。生产环境需避免与 Spring AI 内建 retry 叠加扩大总尝试次数。

API Key 只从精确环境变量 `API-KEY` 读取，Nacos 也仅保存 `${API-KEY:}`。缺失 Key 时，启动早期只排除会强制校验 Key 的 DashScope 自动配置，并提供明确的 not-configured Adapter；服务本身仍启动，`aiProvider` health 为 `NOT_CONFIGURED`，真实调用返回 503。日志只记录 traceId、model、capability、latency、结果码、长度、图片数量、requestId 和 nullable token usage；不记录正文、Prompt、base64、Provider 原始异常或 Key。Provider health 只检查配置存在性，不发模型请求。

## 阶段 10 正式审核能力

新增内部 `POST /internal/ai/article-audit`。输入为审核版本、标题/标签、纯文本正文和可信 MinIO objectKey；AI Service 自行读取图片字节并执行结构化多模态调用。Windows 下带连字符环境变量不会依赖宽松命名推断：启动期直接读取精确 `API-KEY` 并只映射到内存属性，Key 仍不进入配置文件、命令行或日志。详细流程见 `ARTICLE_AUDIT_FLOW.md`。

## 实测结果

- Text：成功，model `qwen3.8-max`，latency 2922 ms，totalTokens 93。
- Structured：DTO 成功，decision/riskLevel/reason/confidence/riskTags 均有效。
- Vision：Spring AI Alibaba + PNG bytes 成功，model `qwen3.8-max`，latency 932 ms，totalTokens 149。
- Streaming：44 个 SSE data 事件、41 个非空 chunk；first token 1009 ms，total 3453 ms。

首次调用曾出现宿主执行沙箱 `Permission denied: getsockopt`，移至允许外网的本任务 Java 进程后立即成功；这不是模型、Key、区域或 Adapter 故障。官方默认 DashScope endpoint 可用，未覆盖 `AI_BASE_URL`。

## 后续边界

阶段 10 应在 Wemedia 应用层通过 Feign 调 AI Service，并将任何结构化解析/Provider 异常转人工 REVIEW；不得直接注入 ChatModel。阶段 11 的摘要和续写继续复用通用 chat/stream 抽象，并单独设计公开业务鉴权、配额和取消语义。本阶段没有修改 `wm_news`、没有 audit record、没有摘要或正式续写 API。
