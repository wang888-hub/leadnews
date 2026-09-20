# Stage 15 performance suite

工具为 Node.js 内置 `fetch`，无新增 npm 依赖。每个正式持续压测默认先预热 30 秒，再测试至少 60 秒；JSON 保存到 `performance/results/`。所有请求只经过 Gateway 51601。

示例：

```powershell
node performance/scripts/http-load.mjs --scenario=article-c5 --url=http://localhost:51601/api/article/13 --concurrency=5 --warmup=30 --duration=60 --out=performance/results/article-c5.json
$token=node performance/scripts/login-token.mjs user
node performance/scripts/http-load.mjs --scenario=behavior-view --url=http://localhost:51601/api/behavior/articles/13/view --method=POST --token=$token --concurrency=10 --warmup=0 --requests=100 --out=performance/results/behavior-view.json
```

持续测试记录 concurrency、总请求、200、429、5xx、连接错误、RPS、P50/P95/P99/max；固定请求数模式用于幂等/副作用断言。`jvm-sample.ps1` 从阶段 14 PID 文件读取进程，只采集这些 Java 服务的 RSS、线程、累计 CPU 和 `jstat -gcutil`。

写测试必须使用 `stage15_` 可识别数据，并在测试前后按业务关系精确比对；禁止全库/全缓存清理。故障注入只能 stop/start Compose 中命名明确的单个项目容器，必须等待 healthy 并检查收敛。
