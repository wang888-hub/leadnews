# Windows 本地开发手册

## 拓扑边界

- Docker Desktop：MySQL、Redis、Nacos、Kafka、Elasticsearch、MinIO、XXL-Job Admin。
- Windows JVM / IDEA：Gateway 51601；User 51801；Article 51802；Wemedia 51803；Behavior 51804；Search 51805；Schedule 51806；Admin 51807；AI 51808。
- 前端开发：默认 5173/5174/5175；所有 `/api` 只经 Gateway。
- 生产前端验收：Nginx 18080/18081/18082，仍只反代 Windows Gateway。

Java 在 Windows 访问 Docker 必须使用 `localhost` 和 Host 映射端口（MySQL 3307、Redis 6379、Nacos 8848、Kafka 9092、ES 9201、MinIO 9000、XXL-Job 8088）。Docker 容器之间使用 Compose service name；两套地址不可混用。

## 首次准备

```powershell
cd D:\Toutiao\leadnews-ai-platform
Copy-Item .env.example .env
$env:JAVA_HOME='D:\jdkk'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd clean verify
cd frontend
corepack pnpm install --frozen-lockfile
corepack pnpm -r typecheck
corepack pnpm --filter @leadnews/shared test
corepack pnpm -r build
```

`API-KEY` 不写入 `.env.example` 或源码。需要 AI 真调用时，在启动 Java 前仅设置当前 PowerShell 进程或 Windows 用户级环境变量。启动脚本不会从 `.env` 导入该变量，以免把密钥文件化。

## 每日启动顺序

```powershell
.\scripts\start-infra.ps1
.\scripts\check-infra.ps1
.\scripts\start-backend.ps1 -JavaHome D:\jdkk
.\scripts\health-check.ps1
.\scripts\start-frontend.ps1
```

脚本拒绝覆盖已占用端口。阶段 14 验收时 5173/5174 被用户应用占用，实际改用 5176/5177/5179，没有终止用户进程。Java PID、启动时间、可执行文件与 JAR 路径保存在 `.stage14-runtime`，日志在 `.stage14-logs`；停止脚本只处理匹配记录的本项目进程。

## URL

- Gateway：`http://localhost:51601`
- Vite：`http://localhost:5173`、`5174`、`5175`（或启动时指定的替代端口）
- Nginx：`http://localhost:18080`、`18081`、`18082`
- Nacos Console：`http://localhost:8080`
- MinIO Console：`http://localhost:9001`
- XXL-Job Admin：`http://localhost:8088/xxl-job-admin/`

## 健康检查与停止

```powershell
.\scripts\check-infra.ps1
.\scripts\health-check.ps1
.\scripts\stop-frontend.ps1
.\scripts\stop-backend.ps1
docker compose -f docker-compose.frontend.yml down
```

最后一条只删除 production 前端验收 Nginx；日常无需停止基础设施。需要停止基础设施但保留数据时使用 `docker compose stop`，不要执行带 `-v` 的命令。
