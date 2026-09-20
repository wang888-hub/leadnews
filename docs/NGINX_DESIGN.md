# Nginx 前端验收设计

## 文件与端口

- Compose：`docker-compose.frontend.yml`
- 主配置：`docker/nginx/nginx.conf`
- SPA/代理公共片段：`docker/nginx/leadnews-spa.inc`
- App/Wemedia/Admin：Host 18080/18081/18082

启动前必须先完成 `corepack pnpm -r build` 并确保 Windows Gateway 51601 已 UP：

```powershell
docker compose -f docker-compose.frontend.yml config --quiet
docker compose -f docker-compose.frontend.yml up -d
docker compose -f docker-compose.frontend.yml ps
```

停止只影响该 Nginx：

```powershell
docker compose -f docker-compose.frontend.yml down
```

## 路由与缓存

- `/` 使用 `try_files ... /index.html` 支持 Vue Router history fallback。
- `index.html` 使用 `no-cache, no-store, must-revalidate`，避免版本入口长期缓存。
- 带 hash 的 `/assets/` 使用一年 `immutable` 缓存。
- `/api/` 反代 `host.docker.internal:51601`，不暴露业务服务端口。

## SSE

自媒体续写路径单独关闭 `proxy_buffering`、proxy cache 与 gzip，使用 HTTP/1.1 和 90 秒读取超时。阶段 14 真实验收得到 23 个 chunk、1 个 done，首块 757 ms、总计 2456 ms；AbortController 取消成功，调用前后数据库正文一致。普通代理若开启缓冲，会把多个 token 合并后才交给浏览器，因此 SSE 必须保持独立 location。
