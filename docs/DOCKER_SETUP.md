# Docker Desktop 基础设施说明

更新时间：2026-09-02

## 1. 前置条件

- Windows 10/11、WSL 2 后端和 Docker Desktop 4.41.2 或更高兼容版本。
- Docker Engine 28.1.1、Docker Compose 2.35.1 已完成本阶段验证。
- 建议为 Docker Desktop 分配至少 8 GB 内存；同时运行 IDEA 和全部服务时建议 10–12 GB。本机验证时 Docker 可用内存约 13.5 GiB。
- Host 端口 3306 和 9200 已被其他程序占用，因此本项目固定使用 MySQL 3307 和 Elasticsearch 9201；不要为了恢复默认端口关闭已有程序。

## 2. 固定版本与选择原因

| 组件 | 镜像 | 选择原因 |
| --- | --- | --- |
| MySQL | `mysql:8.4.11` | MySQL 8.4 LTS 系列，避免继续使用原项目旧版本 |
| Redis | `redis:7.4.11-alpine` | 稳定的 7.4 系列，镜像较轻 |
| Nacos | `nacos/nacos-server:v3.0.3` | 与 Spring Cloud Alibaba 2025.0.0.0 管理的 Nacos Client 3.0.3 对齐 |
| Kafka | `apache/kafka:3.9.1` | Apache 官方镜像，使用 KRaft，无 ZooKeeper |
| Elasticsearch | `docker.elastic.co/elasticsearch/elasticsearch:8.17.10` | 8.17 稳定线，后续 Java Client 可使用同代或更新的兼容客户端 |
| MinIO | `quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z` | 固定官方发布标签，不使用 `latest` |
| MinIO Client | `minio/mc:RELEASE.2025-08-13T08-35-41Z` | 固定初始化客户端版本 |
| XXL-Job Admin | `xuxueli/xxl-job-admin:3.4.2` | 官方发布镜像和对应数据库脚本 |

Sentinel Dashboard 未加入 Compose：当前没有与本项目基线同等可靠、持续维护的官方容器镜像。后续需要时使用官方 Sentinel 1.8.9 Jar 本地启动，不采用来源不明的第三方镜像。

参考：

- Nacos 3 Docker 部署与端口：https://nacos.io/en/docs/v3.0/quickstart/quick-start-docker/
- Apache Kafka 官方 Docker 文档：https://kafka.apache.org/39/getting-started/docker/
- Elasticsearch Java Client 兼容说明：https://www.elastic.co/docs/reference/elasticsearch/clients/java
- MySQL 官方镜像：https://hub.docker.com/_/mysql
- Redis 官方镜像：https://hub.docker.com/_/redis
- XXL-Job Admin 官方镜像：https://hub.docker.com/r/xuxueli/xxl-job-admin

## 3. 端口与容器

| 服务 | Container Name | Windows / IDEA 地址 | Docker 内部地址 |
| --- | --- | --- | --- |
| MySQL | `leadnews-mysql` | `localhost:3307` | `mysql:3306` |
| Redis | `leadnews-redis` | `localhost:6379` | `redis:6379` |
| Nacos Console | `leadnews-nacos` | `http://localhost:8080` | `http://nacos:8080` |
| Nacos HTTP API | `leadnews-nacos` | `localhost:8848` | `nacos:8848` |
| Nacos gRPC | `leadnews-nacos` | `localhost:9848` | `nacos:9848` |
| Kafka | `leadnews-kafka` | `localhost:9092` | `kafka:29092` |
| Elasticsearch | `leadnews-elasticsearch` | `http://localhost:9201` | `http://elasticsearch:9200` |
| MinIO API | `leadnews-minio` | `http://localhost:9000` | `http://minio:9000` |
| MinIO Console | `leadnews-minio` | `http://localhost:9001` | `http://minio:9001` |
| XXL-Job Admin | `leadnews-xxl-job-admin` | `http://localhost:8088/xxl-job-admin/` | `http://xxl-job-admin:8080/xxl-job-admin/` |

Nacos 9849 是服务端节点之间的通信端口，单机开发不需要暴露。容器之间禁止使用 `localhost`；`localhost` 只供 Windows 上运行的 IDEA 服务使用。

## 4. Network 与 Volume

- Network：`leadnews-network`。
- Volumes：`leadnews-mysql-data`、`leadnews-redis-data`、`leadnews-nacos-data`、`leadnews-nacos-logs`、`leadnews-kafka-data`、`leadnews-elasticsearch-data`、`leadnews-minio-data`、`leadnews-xxl-job-logs`。
- `docker compose stop` 或普通 `docker compose down` 不删除命名 Volume，数据会保留。
- `docker compose down -v` 会永久删除本项目 Volume 和全部本地数据，只能在明确确认不再需要数据时手工执行。

## 5. 环境变量

复制模板：

```powershell
Copy-Item .env.example .env
```

模板包含 MySQL、Redis、Nacos 和 MinIO 的本地开发默认值。它们不是生产密码；共享环境必须全部更换。真实 `.env` 已被 Git 忽略，不要把密码、Token 或 API Key 写入 YAML、Java 源码或提交记录。

Nacos 3 第一次启动后需在 Console 执行管理员初始化。本阶段已使用 `.env` 中的本地开发密码完成初始化；如果删除 Nacos 数据卷重新开始，浏览器会再次显示初始化流程。

## 6. 启动、停止与查看

```powershell
docker compose config
docker compose up -d
docker compose ps
```

停止但保留容器和数据：

```powershell
docker compose stop
```

删除项目容器和 Network、保留命名 Volume：

```powershell
docker compose down
```

清理镜像和 Volume 不属于日常操作。不得用通配符删除用户已有 Docker 资源；确需清理时先通过 `docker compose ps` 和 `docker volume ls` 核对精确对象。

## 7. 初始化内容

- MySQL 使用 UTF8MB4、`utf8mb4_0900_ai_ci` 和 `+08:00` 时区。
- 初始化 schema：`leadnews_user`、`leadnews_article`、`leadnews_wemedia`、`leadnews_behavior`、`leadnews_admin`、`leadnews_schedule`、`xxl_job`。
- 当前不创建业务表；仅从 XXL-Job 3.4.2 官方脚本初始化 8 张管理表及本地管理员。
- Redis 开启 AOF 并使用命名 Volume；尚未设计业务 Key。
- Elasticsearch 使用单节点、关闭本地开发安全认证、JVM 固定为 512 MB；尚未创建文章索引。
- MinIO 初始化私有 Bucket `leadnews`；后续业务阶段再决定是否拆分 material、article、avatar。

## 8. Kafka listeners 原理

Kafka 客户端连接 bootstrap 地址后，会继续使用 Broker 返回的元数据地址。如果只通端口但 Broker 返回容器名，Windows Java 客户端会在第二次连接时失败。因此配置两套 listener：

- `EXTERNAL://localhost:9092`：Windows IDEA 使用，Broker 元数据继续返回 `localhost:9092`。
- `INTERNAL://kafka:29092`：Compose Network 内的容器使用，Broker 元数据返回 `kafka:29092`。
- Controller 使用 `kafka:29093`，不暴露到 Host。

本阶段已用临时 Topic 完成双向交叉测试：外部 listener 生产、内部 listener 消费成功；内部 listener 生产、外部 listener 消费成功。元数据验证显示 Broker 为 `localhost:9092`，测试 Topic 随后已删除。

## 9. Healthcheck 与资源限制

- MySQL：`mysqladmin ping`。
- Redis：带密码 `PING`。
- Nacos 3：Console 端口 `/v3/console/health/readiness`。Nacos 3 的 Console 与客户端 API 分离在 8080 和 8848，不能继续使用旧版 v1 路径。
- Kafka：通过 Broker 列举 Topic。
- Elasticsearch：等待单节点达到 yellow 或 green。
- MinIO：`mc ready local`。
- XXL-Job：检查 Admin HTTP 页面。

JVM 限制：Nacos 384 MB、Kafka 384 MB、Elasticsearch 512 MB、XXL-Job 256–384 MB。加上 MySQL、Redis、MinIO 与 Docker 本身，建议 Docker Desktop 分配 10–12 GB；8 GB 是可运行下限。

## 10. 常见问题

- Host 3306/9200 不可用：本项目已固定改为 3307/9201，IDEA 配置必须使用实际 Host 端口。
- Nacos 一直 `starting`：确认健康检查使用 `http://127.0.0.1:8080/v3/console/health/readiness`，并查看 `docker logs leadnews-nacos`；不要整体重建其他服务。
- Kafka 首次可连、随后失败：检查客户端是否错误使用 Docker 内部地址，以及 `advertised.listeners` 是否仍返回 `localhost:9092`。
- XXL-Job 无法启动：先确认 MySQL healthy 且 `xxl_job` 中有 8 张基础表。
- Windows 挂载配置出现权限提示：以容器启动参数为最终服务器配置来源；不要在 Windows 上用 Linux 权限命令修改整个项目目录。
- Docker Desktop 宿主机异常：先修复 Docker Desktop 本身，再继续服务验证；不要以删除所有 Volume 或镜像作为第一处理手段。

## 11. 阶段 14 架构边界

基础设施 Compose 继续只承载七项中间件。九个 Java 业务服务最终在 Windows JVM/IDEA 运行，不添加 Java Dockerfile 或 Compose service。另有 `docker-compose.frontend.yml` 仅用于 Nginx 挂载三个 production dist 并通过 `host.docker.internal:51601` 访问宿主 Gateway；它与基础设施生命周期相互独立。

阶段 14 复验时基础设施全部 Ready，Java 使用 Host 映射端口；不要把 Docker 内部的 `mysql:3306`、`kafka:29092` 等地址写进 Windows profile，也不要让容器使用 `localhost` 访问宿主 Gateway。
