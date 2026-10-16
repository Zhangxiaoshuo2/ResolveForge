# ResolveForge · 智能售后客服 Agent（P0）

Java 21 + Spring Boot 4.0.4 + AgentScope Java 2.0.4 + Maven。默认使用文件 H2，在 Windows 主机运行。按本轮调整后的范围，P0 不依赖 Chatwoot、Docker、Redis 或前端。

已实现原生 ReActAgent 工具循环、订单/物流 HTTP 工具、多轮上下文、用户隔离及每轮请求的基础任务状态。2026-10-16 已通过真实 DeepSeek `deepseek-chat` 的订单、物流、多轮、越权和无效订单联调。Fake Model 只存在于自动化测试源码；未配置 API Key 时 `/api/chat` 返回 `503 MODEL_NOT_CONFIGURED`。

源码仓库：[Zhangxiaoshuo2/ResolveForge](https://github.com/Zhangxiaoshuo2/ResolveForge)。每轮开发完成后验证、提交并推送，规则保存在 `AGENTS.md`；真实密钥和本地运行数据不进入仓库。

## Windows 快速启动

已检测本机 JDK 21.0.10、Maven 3.9.14、Git 2.45.1；不需要安装其他组件即可运行 H2 模式。

```powershell
Set-Location 'C:/Users/Lenovo/Desktop/ResolveForge'
./scripts/Test-Environment.ps1
./scripts/Initialize-Local.ps1
./scripts/Start-Agent.ps1
```

若 PowerShell 执行策略阻止脚本，仅对本次进程调用：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/Start-Agent.ps1
```

脚本兼容 Windows PowerShell 5.1 / PowerShell 7，中文脚本保存为 UTF-8 BOM，HTTP JSON 响应按原始字节以 UTF-8 解码。不修改系统执行策略、PATH 或注册表。`Initialize-Local.ps1` 自动生成随机本地认证 Token 和数据库密码，现有 `.env` 与 IntelliJ 私有环境文件均保留。

另开终端验证业务 API：

```powershell
./scripts/Test-Api.ps1
```

该脚本验证订单、物流、越权与无效订单；默认不调用真实模型。

## 接入真实模型

编辑项目根目录 `.env`，填入支持 OpenAI Chat Completions 及 tool calling 的模型配置。例如 DeepSeek：

```dotenv
MODEL_API_KEY=填写自己的密钥
MODEL_BASE_URL=https://api.deepseek.com
MODEL_ENDPOINT_PATH=/chat/completions
MODEL_NAME=deepseek-chat
MODEL_TIMEOUT_SECONDS=90
```

其他兼容服务按其文档调整地址、模型名和 endpoint。例如 `/v1/chat/completions` 应放在 `MODEL_ENDPOINT_PATH`；不要同时在 base URL 和 path 重复 `/v1`。本项目显式设置 `stream(false)`，使用非流式 tool calling。密钥只由 `.env` 导入当前进程环境，不出现在源码或启动参数中。

修改 `.env` 后停止旧服务并重新运行 `Start-Agent.ps1`。在 IDEA 中可直接调试 `ResolveForgeApplication`：工作目录设为项目根目录，并在运行配置中导入 `.env`；若 IDEA 没有 EnvFile 功能，手动填写运行配置的环境变量。Spring Boot 本身不会自动读取 `.env`。

```powershell
./scripts/Test-Api.ps1 -RealModel
```

真实联调入口会顺序运行订单、物流、多轮追问、非法访问、无效订单六次对话，并保存实际回复到 `output/YYYYMMDD_真实模型联调_v01/live-时间.json`。实际回答和 `tool_call` 日志需要一起审核；HTTP 200 本身不证明工具选对或答案正确。

## IntelliJ HTTP Client

打开根目录 `api.http`，选择 **local** 环境，逐条运行。初始化脚本已生成不受版本控制的 `http-client.private.env.json`。若之后改了 `.env` 中用户 Token 或端口，同步修改这个私有文件；不要将其提交。

| 接口 | 作用 |
|---|---|
| `GET /health` | 进程存活检查，不代表模型或业务依赖全部可用 |
| `GET /api/commerce/orders/{orderNo}` | 查询当前用户订单 |
| `GET /api/commerce/orders/{orderNo}/logistics` | 查询当前用户物流 |
| `POST /api/chat` | 同步执行一轮 Agent 请求 |
| `GET /api/tasks/{taskId}` | 当前用户的单个任务 |
| `GET /api/tasks?sessionId=demo-multi` | 当前用户会话最近 20 个任务 |

除 `/health` 外，以上接口使用 `Authorization: Bearer <用户 Token>`。调试阶段只映射 alice、bob 两个演示用户。工具访问业务 API 则使用独立的服务 Token，由服务端注入 `X-User-Id`，模型无法选择身份。

对话请求：

```json
{"sessionId":"demo-multi","message":"查一下 RF1003 订单"}
```

响应形状（UUID 和回复以实际运行为准）：

```json
{"taskId":"生成的 UUID","userId":"alice","sessionId":"demo-multi","reply":"模型基于工具结果生成的回复"}
```

继续追问 `它的物流呢？` 时沿用同一 Token 与 `sessionId`。相同 `sessionId` 的 alice 与 bob 上下文互相隔离。不要在请求中传 `userId` 来模拟登录。

## 演示数据

| 订单号 | 归属 | 商品 | 订单状态 | 物流状态 |
|---|---|---|---|---|
| RF1001 | alice | 机械键盘 | PAID | NOT_SHIPPED |
| RF1002 | alice | 无线鼠标 | UNSHIPPED | NOT_SHIPPED |
| RF1003 | alice | 显示器 | SHIPPED | IN_TRANSIT |
| RF1004 | alice | USB 扩展坞 | DELIVERED | SIGNED |
| RF2001 | bob | 降噪耳机 | DELIVERED | SIGNED |
| RF9999 | 不存在 | — | — | — |

以上均为固定的合成数据，非实时物流。Flyway 首次启动初始化，后续启动不覆盖订单和任务记录。H2 文件位于 `output/20261016_P0本地数据_v01/`；停机后可保留该目录作为本地数据库备份。

## 自动化测试与打包

```powershell
mvn -B -ntp test
mvn -B -ntp package
```

默认测试启动真实 HTTP 服务、真实原生 ReActAgent、Fake OpenAI 协议端点和内存 H2，验证工具执行和上下文传递。Fake 端点按测试规则返回 tool_calls，**不属于真实 LLM 自主决策验证**。生产代码没有 Fake Model 或自实现 Agent Loop。

打包结果为 `target/resolveforge-0.1.0.jar`。直接 `java -jar` 前也必须导入环境变量；推荐使用 `Start-Agent.ps1` 以避免漏配。

## 可选 PostgreSQL

H2 以 PostgreSQL 兼容模式运行；真实 PostgreSQL 尚未实测。安装或已有 Docker 后可运行：

```powershell
docker compose --env-file .env up -d postgres
```

在 `.env` 新增 `DB_URL=jdbc:postgresql://localhost:5433/resolveforge` 后执行：

```powershell
./scripts/Start-Agent.ps1 -Profile postgres
mvn -B -ntp -Ppostgres-it verify
```

`postgres-it` 显式执行 Testcontainers PostgreSQL 迁移与订单归属验证；缺少 Docker 时该检查失败，不会当作通过。切换数据库不会迁移原 H2 中的任务记录。Windows 主机访问容器数据库用 `localhost:5433`；未来容器访问主机服务才用 `host.docker.internal`。

## 当前边界

- 已通过一次完整的真实 DeepSeek 六轮联调；更换模型、供应商或提示后仍需重新检查工具选择和事实回复，当前结果不代表任意输入都正确。
- 会话上下文使用原生内存 StateStore，重启丢失；数据库中的任务历史仍保留。没有任务恢复能力，进程异常退出可能留下 RUNNING 记录。
- 任务 SUCCEEDED 表示本轮正常生成回复，不代表订单存在或业务操作成功。工具查询失败会反馈给模型，事实拒答质量仍需真实模型验证。
- 原生循环最多 6 轮，达到上限记 FAILED；整体超时默认 90 秒，超时返回 504。上下文未做自动淘汰或压缩，适用于当前单机演示规模。
- Chatwoot、Webhook 去重、HITL、退款执行、RAG、复杂 Workflow、分布式任务、Multi-Agent、Skills 均不属于调整后的 P0。

架构见 `docs/architecture.md`，代码调用关系见 `docs/implementation-notes.md`，验证证据见 `docs/verification.md`。各阶段实现总结和关键代码索引位于 `output/20261016_P0开发记录_v01/`。
