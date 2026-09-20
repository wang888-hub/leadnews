# 阶段 13 前端设计

## 架构与版本

前端位于 `frontend/`，由 pnpm workspace 管理三个独立 Vue SPA：用户端 `leadnews-app`、创作端 `leadnews-wemedia`、管理端 `leadnews-admin`。`packages/shared` 只承载真实复用的响应类型、JWT 状态、Axios client、路由守卫、状态映射、安全高亮和 POST SSE parser，不共享业务页面。

本机基线为 Node `24.20.0`、pnpm `11.19.0`。固定版本：Vue `3.5.42`、Vue Router `5.3.1`、Pinia `4.0.3`、Axios `1.20.0`、Element Plus `2.14.5`、Vite `7.3.1`、TypeScript `5.9.3`、Vitest `3.2.4`、vue-tsc `3.2.0`。Vite 7 要求 Node 20.19+ 或 22.12+，当前 Node 满足；Vite 7.3 仍为官方支持分支。所有版本精确锁定，未使用 `latest` 范围。

TypeScript 使用 registry 精确版本 `5.9.3`；先前网络恢复用的本地 tarball 已移除。pnpm workspace 通过 `allowBuilds.esbuild: true` 仅允许 esbuild `0.27.7` 执行必要安装脚本，不改变全局安全策略。

## 路由与认证

- 用户端：`/`、`/login`、`/article/:id`、`/search`、403、404。
- 创作端：`/login`、`/`、`/news`、`/news/new`、`/news/:id`、`/materials`、错误页。
- 管理端：`/login`、`/audit`、`/audit/:id`、`/channels`、错误页。
- 所有页面组件 dynamic import。Pinia `auth` store 只在 localStorage 保存 token、user、userType，不保存密码。守卫分别约束 `APP_USER`、`WEMEDIA`、`ADMIN`，真正权限仍由 Gateway/后端决定。

## HTTP、错误与 TraceId

Axios 统一读取 `VITE_API_BASE_URL`，开发值为 `http://localhost:51601`，所有业务只经 Gateway。请求拦截器发送标准 `Authorization: Bearer`；响应层分别处理 400/401/403/409/429/500/502/503。401 清理登录态并携 redirect 跳登录；其他状态保留具体业务消息。`ApiError` 保留 traceId 供问题详情扩展，但默认不暴露长编号。

## 功能设计

用户端实现频道、分页文章、详情内容块、一次性 view、like/unlike、热点及 degraded 开发标记、AI Summary 状态、搜索排序、安全高亮片段、300 ms 联想和 AbortController 竞态取消、登录历史删除/清空。静态 URL 作为新窗口阅读/降级入口，不使用 iframe。

创作端实现稿件列表与状态映射、块编辑器、素材上传/列表/选择/删除、即时/定时发布输入、提交审核、业务级审核轨迹，以及 POST Fetch streaming 的 SSE 续写。AbortController 可停止；chunk 仅进入“AI 建议内容”，必须点击插入并再次保存才写入稿件。

管理端实现待人工审核列表/详情、DFA→AI→MANUAL 轨迹、通过二次确认、拒绝必填原因，以及频道 CRUD/启停。页面不会展示 provider requestId、usage、prompt、异常栈或密钥。

## UI 与安全

三端统一墨蓝/琥珀视觉、Segoe UI/微软雅黑字体、Loading/Empty 状态和响应式断点。未引入图片素材或额外 UI 库。正文和 AI 输出使用文本插值；搜索高亮只解析 `<em>` 边界并把其余内容作为 text node，未使用 `v-html`。JWT 不进入 URL 或日志；上传只走 Wemedia API；前端无 AI Key、数据库或 MinIO 密钥。

## 测试与构建

共享 Vitest 已覆盖错误状态映射、认证清理、稿件状态、安全高亮和 SSE block parser。三个应用的脚本均为 `typecheck`、`test`、`build`。截至 checkpoint，lockfile 已成功生成，但宿主网络无法完成 `@esbuild/win32-x64` tarball 下载，故尚未执行 typecheck/build/浏览器/真实后端验收；不得将源码存在等同于验收通过。
