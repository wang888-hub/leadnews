# 部署拓扑

## 本地开发拓扑

```text
Browser
  ├─ Vite 5173/5174/5175 ── /api proxy ─┐
  └─ Nginx 18080/18081/18082 ───────────┤
                                         v
Windows JVM: Gateway 51601
  ├─ User 51801       ├─ Behavior 51804   ├─ Admin 51807
  ├─ Article 51802    ├─ Search 51805     ├─ AI 51808
  └─ Wemedia 51803    └─ Schedule 51806
                     │ localhost host ports
                     v
Docker Desktop: MySQL / Redis / Nacos / Kafka / ES / MinIO / XXL-Job
```

Nacos 注册实例地址固定为 `127.0.0.1`，因为调用方同样是 Windows JVM。Nginx 容器使用 `host.docker.internal:51601` 访问宿主 Gateway。所有浏览器业务请求保持同源 `/api`，前端不直连 518xx。

## Production dist 验收

`docker-compose.frontend.yml` 只创建 `leadnews-frontend-nginx`，固定镜像 `nginx:1.28.0-alpine`，只读挂载三个 `dist` 与 Nginx 配置。它不包含 Java 服务、不加入基础设施 Compose，也不持有业务秘密。18080/18081/18082 分别对应 App、Wemedia、Admin。

这是一套本机 production-build 验收拓扑，不等同于公网生产部署。公网部署还需要 TLS、可信反向代理、域名/CSP、密钥管理、网络隔离、集中日志和备份恢复设计；本阶段不声称已经完成这些生产能力。
