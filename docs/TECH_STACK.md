# 技术栈与版本基线

更新时间：2026-09-02

## 版本清单

| 技术 | 锁定版本 | 本阶段用途 |
| --- | --- | --- |
| JDK | 21 | 编译目标与运行基线 |
| Spring Boot | 3.5.16 | 服务基础框架与测试依赖管理 |
| Spring Cloud | 2025.0.3 | Gateway 等 Cloud 组件的统一 BOM |
| Spring Cloud Alibaba | 2025.0.0.0 | 后续 Nacos、Sentinel 的统一 BOM 预留 |
| MyBatis-Plus | 3.5.17 | 后续数据访问层版本预留 |
| Spring AI | 1.1.2 | AI 能力 BOM 预留，本阶段不引入运行时 Starter |
| Spring AI Alibaba | 1.1.2.2 | AI Alibaba BOM 预留，本阶段不引入运行时 Starter |
| Lombok | 1.18.42 | 基础编译期工具 |
| Maven Compiler Plugin | 3.14.1 | 使用 `release=21` 统一编译 |
| Maven Wrapper | 3.9.10 | 固定团队构建工具版本 |
| Maven Enforcer Plugin | 3.6.2 | 强制 Maven `[3.9,4.0)`、Java `[21,22)` |

所有版本集中定义在父 `pom.xml` 的 `properties` 和 `dependencyManagement` 中，子模块不得重复声明 Spring Cloud、Spring Cloud Alibaba、Spring AI 或 MyBatis-Plus 版本。

## 兼容关系与选择依据

- Spring Cloud 官方兼容表将 `2025.0.x` 对应到 Spring Boot `3.5.x`；因此采用 Spring Boot 3.5.16 与 Spring Cloud 2025.0.3 的同代稳定组合。
- Spring Cloud Alibaba 官方版本说明将 `2025.0.x` 对应到 Spring Cloud 2025.0.x、Spring Boot 3.5.x，并要求 JDK 17 及以上；JDK 21 满足该基线。
- Spring AI 官方支持矩阵将 Spring AI `1.1.x` 对应到 Spring Boot `3.5.x`。
- Spring AI Alibaba 1.1.2.2 对齐 Spring AI 1.1.2；未选择官方已标记存在依赖问题的 1.1.2.1。
- MyBatis-Plus 3.5.17 提供 Spring Boot 3 专用 Starter；本阶段仅统一管理版本，服务尚不连接数据库。
- JDK 21 是长期支持版本。父工程通过 `maven.compiler.release=21` 强制所有含 Java 源码的模块输出 Java 21 字节码，即使执行 Maven 的本机 JDK 更高也不会改变目标版本。

官方依据：

- Spring Boot 3.5.16 发布说明：https://spring.io/blog/2026/08/20/spring-boot-3-5-16-available-now
- Spring Boot 系统要求：https://docs.spring.io/spring-boot/3.5/system-requirements.html
- Spring Cloud 版本兼容表：https://spring.io/projects/spring-cloud
- Spring Cloud 2025.0.3 发布说明：https://spring.io/blog/2026/03/25/spring-cloud-2025-0-3-aka-northfields-has-been-released
- Spring Cloud Alibaba 版本说明：https://github.com/alibaba/spring-cloud-alibaba/wiki/Version-Notes
- Spring AI 支持策略：https://github.com/spring-projects/spring-ai/wiki/Support
- Spring AI Alibaba 1.1.2.2 发布说明：https://github.com/alibaba/spring-ai-alibaba/releases/tag/v1.1.2.2
- MyBatis-Plus 安装文档：https://baomidou.com/getting-started/install/

## 模块依赖边界

- `leadnews-model`：仅放跨模块的数据模型；不依赖任何服务模块。
- `leadnews-common`：仅放通用基础能力；不依赖服务模块或 `leadnews-feign-api`。
- `leadnews-feign-api`：后续放跨服务契约，只依赖 `leadnews-model`，不得依赖服务实现。
- Gateway 和各业务服务可以依赖公共模块；服务之间不直接建立 Maven 依赖，跨服务调用通过契约模块完成。
- `leadnews-ai-service` 是独立服务边界。本阶段不依赖 Spring AI Starter，避免未使用的模型客户端和自动配置进入构建。

该方向保证依赖从服务层单向指向公共层，避免循环依赖。

## 后续组件兼容注意事项

- Nacos：在后续阶段使用 Spring Cloud Alibaba 2025.0.x 对应 Starter，并优先采用 Spring Boot Config Data 的 `spring.config.import`；不要恢复旧项目的 `bootstrap.yml` 隐式加载方式。Nacos Server/Client 的精确版本应在部署阶段依据该发行版依赖表再次核验。
- Sentinel：Gateway 是 WebFlux 技术栈，后续网关限流必须选用响应式适配方式；不要把 Servlet Web Starter 引入 Gateway。
- OpenFeign：后续仅使用 Spring Cloud BOM 管理的 `spring-cloud-starter-openfeign`，不得在子模块覆盖 Feign 或 LoadBalancer 版本；接口契约放在 `leadnews-feign-api`。
- Gateway：当前使用 `spring-cloud-starter-gateway-server-webflux`，后续继续保持响应式依赖边界，避免同时引入 `spring-boot-starter-web`。
- Spring AI：开始 AI 阶段前再次核验所选模型 Starter与 Spring AI Alibaba 1.1.2.2 的坐标；密钥必须通过环境变量或外部配置注入，不进入仓库。
- Spring Cloud Alibaba 2025.0.0.0 的基线发布早于 Spring Cloud 2025.0.3；二者属于同一 2025.0 发布列车，但在真正启用 Nacos/Sentinel 时仍需补充启动与集成测试，不能只以 BOM 可解析作为运行兼容结论。

## 构建环境

- 项目要求 Maven `[3.9,4.0)` 和 Java `[21,22)`，由 Maven Enforcer 在构建开始时校验。
- Maven Wrapper 固定为 3.9.10；Windows 推荐 `.\mvnw.cmd clean verify`，Linux/macOS 推荐 `./mvnw clean verify`。
- 本机验证使用 Eclipse Temurin JDK 21.0.12.1，`mvn -version` 与 Wrapper 均确认 Maven 实际运行于 Java 21，而不仅是编译目标为 21。
- 不修改 Windows 全局 `JAVA_HOME`。项目使用当前终端/IDEA 的项目级 JDK 21 配置。
- `.mvn/maven.config` 将本项目依赖缓存放在 `.mvn/repository`，避免 Windows 中文用户目录在部分 Maven 插件中出现路径编码问题；该目录已加入 `.gitignore`。
