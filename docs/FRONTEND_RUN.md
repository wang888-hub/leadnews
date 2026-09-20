# 前端运行说明

## 前置条件

- Node.js 20.19+、22.12+ 或当前已验证的 24.x；本机为 24.20.0。
- pnpm 11.19.0，通过 `corepack pnpm` 调用；不要升级到 pnpm 12。
- Gateway 位于 `http://localhost:51601`，真实联调时按所需页面启动对应业务服务。

## 安装与校验

```powershell
cd D:\Toutiao\leadnews-ai-platform\frontend
corepack pnpm install --frozen-lockfile
corepack pnpm -r typecheck
corepack pnpm --filter @leadnews/shared test
corepack pnpm -r build
```

生产构建分别输出到三个项目自己的 `dist/`，默认不提交。`node_modules/`、`dist/`、`.env.local` 已忽略。

## 启动

```powershell
corepack pnpm --filter leadnews-app dev
corepack pnpm --filter leadnews-wemedia dev
corepack pnpm --filter leadnews-admin dev
```

默认端口：用户端 5173、创作端 5174、管理端 5175。开发环境统一设置 `VITE_API_BASE_URL=/`，由各自 Vite `/api` 代理转发至 Gateway `http://localhost:51601`，避免浏览器跨域预检；不直连业务服务。若端口被占用，可用 `pnpm --filter <app> exec vite --port <空闲端口> --strictPort` 临时验收，不关闭用户程序。生产环境复制 `.env.production.example` 并按同源部署入口设置，不得加入 JWT、AI Key、数据库密码或 MinIO Secret。

## 联调服务范围

- 用户端：Gateway、User、Article、Behavior、Search；摘要展示还需要阶段 11 数据已生成。
- 创作端：Gateway、Wemedia、Article；审核/续写需要 Kafka、AI 及相关服务。
- 管理端：Gateway、Admin、Wemedia、Article。

依赖安装、typecheck、测试、production build 与真实联调均已通过。

## 阶段 14 启动脚本与 Nginx

项目根目录可使用 `.\scripts\start-frontend.ps1` 同时启动三个 Vite，并用 `.\scripts\stop-frontend.ps1` 只停止脚本记录的进程。端口冲突时使用 `-AppPort`、`-WemediaPort`、`-AdminPort` 显式指定空闲端口。

production dist 验收使用独立 `docker-compose.frontend.yml`：

```powershell
docker compose -f docker-compose.frontend.yml up -d
docker compose -f docker-compose.frontend.yml ps
docker compose -f docker-compose.frontend.yml down
```

入口为 18080/18081/18082。Nginx 对 SPA history fallback、入口/静态资产缓存、Gateway 反代和 SSE 禁止缓冲均已真实验证；详见 `NGINX_DESIGN.md`。这不是 Java 服务容器化方案。
