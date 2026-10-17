# 主要类、调用关系与框架边界

## 版本核对

固定 `io.agentscope:agentscope-core:2.0.4` 与 `io.agentscope:agentscope-extensions-model-openai:2.0.4`。AgentScope 官方父 POM 要求最低 Java 17，本项目 Java 21；官方 dependency BOM 的 Spring Boot 版本为 4.0.4，starter 聚合模块局部版本为 4.0.3。本项目没有依赖 Spring Boot starter，直接使用框架组件并由 Spring 管理业务服务，选 Boot 4.0.4 后完成编译与 HTTP 测试。[父 POM](https://github.com/agentscope-ai/agentscope-java/blob/v2.0.4/pom.xml)、[依赖 BOM](https://github.com/agentscope-ai/agentscope-java/blob/v2.0.4/agentscope-dependencies-bom/pom.xml)。

依赖树实测 SDK 为 2.0.4、Reactor 3.8.4、SDK Jackson 2.21.1。Boot MVC 使用自己的默认 JSON converter；本项目以字符串消费业务 API JSON，不把 SDK 的 Jackson 内部类型暴露为 HTTP DTO。依赖树证据：`output/P0开发记录/06_dependencies.log`。

## 源码索引

路径均相对项目根目录 `C:/Users/Lenovo/Desktop/ResolveForge`。

| 位置 | 实现职责与下游 |
|---|---|
| `src/main/java/dev/resolveforge/ResolveForgeApplication.java` | 应用入口与配置扫描 |
| `src/main/java/dev/resolveforge/config/AppProperties.java` | 绑定 env 配置，校验三类 Token 必须独立且不少于 24 字符 |
| `src/main/java/dev/resolveforge/api/IdentityService.java` | 用户认证、内部业务调用认证、常量时间 Token 比较 |
| `src/main/java/dev/resolveforge/api/ChatController.java` | 校验 HTTP 参数，获得可信用户，进入 ChatExecutionService |
| `src/main/java/dev/resolveforge/agent/ChatExecutionService.java` | 创建任务，调用 AgentService，记录终态 |
| `src/main/java/dev/resolveforge/agent/TaskRepository.java` | 任务持久化与按用户查询 |
| `src/main/java/dev/resolveforge/agent/AgentService.java` | 构建 singleton ReActAgent、Toolkit、模型、StateStore；传入 RuntimeContext |
| `src/main/java/dev/resolveforge/agent/ModelAuditMiddleware.java` | 原生模型调用钩子，不拦截或重写循环 |
| `src/main/java/dev/resolveforge/agent/CommerceTools.java` | 声明 queryOrder/queryLogistics，注入可信上下文 |
| `src/main/java/dev/resolveforge/agent/CommerceApiClient.java` | 通过 HTTP 调用 `/api/commerce/orders/...` |
| `src/main/java/dev/resolveforge/commerce/CommerceController.java` | 对业务请求认证后调用 CommerceRepository |
| `src/main/java/dev/resolveforge/commerce/CommerceRepository.java` | 参数化 SQL，订单号和归属一起过滤 |
| `src/main/java/dev/resolveforge/api/ApiException.java` / `src/main/java/dev/resolveforge/api/ApiErrors.java` | 稳定错误码和 HTTP 状态，不向客户端泄露内部异常 |
| `src/main/resources/db/migration/` | schema、合成订单和任务表 |
| `scripts/`、`api.http` | PowerShell 初始化/启动/测试、IntelliJ 请求用例 |

调用主线：`ChatController → ChatExecutionService → AgentService → 原生 ReActAgent → Toolkit → CommerceTools → CommerceApiClient → HTTP → CommerceController → CommerceRepository`。

## 原生能力与自行实现

| 能力 | 原生 AgentScope | 本项目 |
|---|---|---|
| 推理与工具迭代 | ReActAgent | 设置提示、6 轮上限和总超时；检查终止原因 |
| 工具 schema / 分发 | Toolkit、@Tool、@ToolParam | 订单、物流及 searchPolicy 三个工具 |
| Tool Calling 协议 | OpenAIChatModel | 环境变量、base URL、endpoint、模型名 |
| 多轮历史 | InMemoryAgentStateStore | 从认证身份生成 RuntimeContext 的 user/session |
| 同会话执行协调 | 框架原生会话机制 | 无自建 Agent Loop 或分布式锁 |
| 模型观测钩子 | MiddlewareBase.onModelCall | 仅记录必要日志 |
| 售后权限 | — | Token 认证、可信身份注入、SQL 归属过滤 |
| 每轮请求任务记录 | — | 基础 RUNNING/SUCCEEDED/FAILED 表与查询 API |

P1 在原有 AgentService 中注册 PolicyTools，并更新证据与引用提示；没有修改 P0 身份校验、CommerceTools 或任务状态实现。没有引入 HarnessAgent、Skills、Multi-Agent、人工审批、退款工具或任务恢复。

新增 `src/main/java/dev/resolveforge/rag/` 提供 Markdown 加载、中文 BM25、独立真实 Embedding Provider、H2 向量缓存、余弦召回、RRF、结构化 Evidence、searchPolicy 和调试 REST 接口。类索引与设计取舍详见 `docs/rag-design.md`。三个工具均由框架原生 Toolkit 分发，无额外意图路由或固定调度顺序。

## 本轮验证与未验证

默认自动化测试使用原生 ReActAgent 和真实 HTTP 业务 API；测试端点返回 Fake OpenAI 响应，P1 检索机制测试另用明确 Fake 基向量，测试类不会进入生产 JAR。最终 package 共 28 项通过，包含 P0 原有 12 项。P0 的真实 DeepSeek 六次对话保留为历史记录；P1 另行完成真实百炼 35 条检索比较和生产 JAR 上真实 DeepSeek 10 次对话。真实 PostgreSQL 仍未验证，Chatwoot 不在本轮范围。完整结果见 `docs/verification.md`。

真实模型后续验收必须检查日志中实际的 queryOrder/queryLogistics 调用、对应返回事实、多轮订单号衔接及越权拒绝，不能只判断回复是否自然。`scripts/Test-Api.ps1 -RealModel` 已提供入口，不会在缺少 Key 时自动降级为 Fake Model。脚本使用原始 HTTP 响应字节进行 UTF-8 解码，避免 Windows PowerShell 5.1 对无 charset 的 JSON 响应产生中文乱码。
