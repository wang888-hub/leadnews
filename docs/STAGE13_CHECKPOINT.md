# 阶段 13 Checkpoint（2026-09-09）

## 最终状态（2026-09-10）

阶段 13 已从本 checkpoint 完整恢复并完成。依赖、三前端 typecheck、shared 测试（5/5）、三前端 production build、真实 Gateway API、三个浏览器端登录与核心页面、Qwen SSE 完成/取消/不自动保存均已验收。开发模式修复为同源 `/api` + Vite Gateway 代理，并修复 SSE 基址 `/` 与 `/api` 拼接成协议相对 URL 的问题。401/403/409/429/503 已分别真实或针对性验证；Behavior 突发 20 次为 8 个 200、12 个 429。最终源码和 dist 安全扫描为 0 命中。本任务 Java/Vite 进程已停止，Docker 保持运行。以下旧暂停记录仅作审计历史，不再作为恢复指令。

## 2026-09-09 第二次安全暂停点（以本节为准）

- 依赖阻塞已解除。pnpm 固定为 `11.19.0`；项目级 `pnpm-workspace.yaml` 仅允许 `esbuild` build script；`esbuild`/Windows binary 均为 `0.27.7`。
- TypeScript 已恢复为 registry 固定依赖 `5.9.3`，临时 `vendor/typescript-5.9.3.tgz` 已移除。
- 已真实通过：`pnpm -r typecheck`、shared 5 个 Vitest、`pnpm -r build`；三个前端均生成 dist。
- 已通过 Gateway 真实验证用户/自媒体/管理员登录，以及频道、文章列表/详情、浏览、点赞/取消、搜索、联想、历史、热点、素材列表、稿件列表、待审核列表、频道管理；无令牌 `/api/user/me` 返回 401。
- 浏览器联调发现 Gateway 对跨域预检返回 403。已做最小开发环境修复：三个 Vite 配置增加 `/api` 到 `http://localhost:51601` 的代理，三个 `.env.development` 改为 `VITE_API_BASE_URL=/`。修复已写入文件，但尚未重启 Vite、重新 typecheck/build 或浏览器复验。
- 宿主机 5173 被用户已有 EchoMind 页面占用，未停止。用户端本次临时运行在 5176；自媒体端/管理端为 5174/5175。
- 本任务启动的三个 Vite 进程和八个 Java 服务进程均已安全停止；Docker 基础设施未停止、未修改。
- 下一次第一步：执行 `cd D:\Toutiao\leadnews-ai-platform\frontend; corepack pnpm -r typecheck; corepack pnpm --filter @leadnews/shared test; corepack pnpm -r build`，随后在不占用 5173 的前提下启动用户端（可继续临时用 5176）并复验浏览器登录。
- 后续未完成：13B/13C/13D 浏览器全链路、SSE streaming/cancel/no-auto-save、401/403/409/429/503 差异 UX、源码与 dist 最终安全扫描、文档最终化。不要重复安装依赖或重建页面。

## 状态

阶段 13 因宿主 npm 大包下载异常暂停，当前约完成 70% 的源码实现、30% 的验收。三个前端文件均语法闭合，未修改后端；但依赖实体未安装完成，因此尚不能确认 TypeScript 编译、production build、浏览器与真实 API 全链路通过。

## 已完成文件与能力

- Workspace：`frontend/package.json`、`pnpm-workspace.yaml`、`pnpm-lock.yaml`、`vendor/typescript-5.9.3.tgz`。
- Shared：`packages/shared` 下的 types/auth/request/router/status/highlight/sse、Vitest 测试与 tsconfig。
- 用户端：package/config/env、全局样式、API、router、App、登录、首页、详情、搜索、403/404。
- 创作端：package/config/env、API、router、App、登录、Dashboard、稿件列表、块编辑器、素材、SSE 续写和错误页。
- 管理端：package/config/env、API、router、App、登录、审核列表/详情、频道管理和错误页。
- 文档：`FRONTEND_DESIGN.md`、`FRONTEND_RUN.md`；`.gitignore` 已补前端输出。

核心实现均调用真实 `/api/**`，没有 Mock 数据，没有业务服务直连。Axios 区分 401/403/409/429/503；SSE 为 POST fetch streaming + AbortController，且不自动保存。搜索高亮不用 `v-html`。

## 当前阻塞与诊断

本机 Node 24.20.0、npm/pnpm 11.19.0 正常。npm metadata 查询可用，固定版本已确定。pnpm 的依赖供应链校验和 lockfile 生成成功；`typescript-5.9.3.tgz` 经镜像以约 10 KiB/s 完整下载并改为本地 file dependency。

后续唯一明确缺失的大包是 `@esbuild/win32-x64@0.27.7`（约 4.5 MiB）：官方源和 npmmirror 均反复报 fetch code 23/timeout，直接 curl 速度一度预计数十分钟，已中止并删除不完整临时文件。不要重复下载 Linux/macOS 可选二进制；优先在网络恢复后执行普通 frozen install。

`node_modules` 当前是不完整的安装中间态且已被 gitignore。旧 lockfile 备份和不完整 esbuild tarball已删除；最终 `pnpm-lock.yaml` 与当前 package manifests 一致。

## 未完成清单

1. 完成依赖安装。
2. `pnpm typecheck`，按报错最小修复类型/模板。
3. `pnpm test`，补充 router guard 与 AbortController cancellation 测试（当前已有基础 SSE/parser/auth/error/status/highlight 测试）。
4. `pnpm build`，确认三个 dist。
5. 逐子阶段启动 dev server 并做非浏览器 HTTP 编译检查。
6. 启动必要 Java 服务，经 Gateway 真实验证 13B、13C、13D；不要为了验证频繁调用 Qwen。
7. 若用户明确允许浏览器自动化，补桌面 1440×900、移动 390×844 与关键操作验证；当前没有执行浏览器测试。
8. 根据真实响应校准可能的 Material 字段名、搜索历史字段和 SSE chunk data key。
9. 更新 `docs/API.md`、README、PROGRESS 最终完成记录，并做 secret/旧 IP/Mock 扫描。

## 恢复第一条命令

```powershell
cd D:\Toutiao\leadnews-ai-platform\frontend
pnpm install --frozen-lockfile --registry=https://registry.npmjs.org --fetch-timeout=300000 --network-concurrency=1
```

若仍只卡 `@esbuild/win32-x64@0.27.7`，先验证该单包下载，不要更换 Vue/Vite 大版本，不要重新创建三个项目。安装成功后立即执行 `pnpm typecheck`。

## 禁止重复与边界

- 不要重做三个应用骨架、shared 类型、Axios/auth/SSE/highlight 或现有页面。
- 不要修改阶段 0~12 后端契约迁就页面，除非真实响应证明后端缺陷。
- 不要改 Docker Compose、部署 Nginx、进入阶段 14。
- 不要声称尚未执行的 typecheck、build、浏览器、401/403/409/429/503 或真实 API 验收成功。
- 原项目 `D:\Toutiao\project\heima-leadnews` 检查时为 Git clean，始终只读。
