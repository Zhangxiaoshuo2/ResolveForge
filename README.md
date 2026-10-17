# ResolveForge · 智能售后客服 Agent（P1）

Java 21 + Spring Boot 4.0.4 + AgentScope Java 2.0.4 + Maven。默认使用文件 H2，在 Windows 主机运行。P1 不依赖 Chatwoot、Docker、Redis 或前端。

保留 P0 原生 ReActAgent 工具循环、订单/物流 HTTP 工具、多轮上下文、用户隔离及基础任务状态。P1 增加演示政策知识库、Lucene 中文 BM25、真实 Embedding 缓存、余弦召回及 RRF 融合，Agent 可自主组合三个工具回答售后咨询。

Fake Model / Fake Embedding 只存在于自动化测试源码。未配置聊天模型 Key 时 `/api/chat` 返回 `503 MODEL_NOT_CONFIGURED`；未配置独立 Embedding Key 时仍可使用 BM25-only，不会标记为 Hybrid 成功。P0 已完成真实 DeepSeek 联调；P1 的实际评测结果见 `docs/rag-evaluation.md`。

源码仓库：[Zhangxiaoshuo2/ResolveForge](https://github.com/Zhangxiaoshuo2/ResolveForge)。每轮开发完成后验证，提交和推送前按 `AGENTS.md` 确认；真实密钥和本地运行数据不进入仓库。

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

真实联调入口会顺序运行订单、物流、多轮追问、非法访问、无效订单六次对话，并保存实际回复到 `output/真实模型联调/live-时间.json`。实际回答和 `tool_call` 日志需要一起审核；HTTP 200 本身不证明工具选对或答案正确。

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

## P1：政策知识检索与业务工具协同

知识库是 `src/main/resources/knowledge/` 中的五份**虚构演示政策**，共 19 个条款 Chunk，不代表真实平台规则，不保存私人订单事实。政策支持退款条件、退换货、发货时效、物流异常及签收处理；不会执行退款或更改订单。

在 `.env` 单独配置 Embedding，不复用 DeepSeek Key：

```dotenv
EMBEDDING_API_KEY=你的百炼密钥
EMBEDDING_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
EMBEDDING_MODEL=text-embedding-v4
EMBEDDING_DIMENSIONS=1024
```

密钥地域须匹配 base URL，其他兼容服务可调整地址和模型。首次启动生成文档向量并缓存在本地 H2；以后启动根据内容哈希、模型、服务地址和维度复用。更换模型或维度会生成新的索引向量，旧缓存不会混入本次检索。改文档后重启即可重新加载，未变条款不重复调用 Embedding。

新增接口仍需原有 Bearer Token：

| 接口 | 用途 |
|---|---|
| `GET /api/knowledge/status` | Chunk 数、向量是否可用、模型、维度、缓存命中及重建数量 |
| `POST /api/knowledge/search` | 检索调试，返回结构化 Evidence、实际 mode、fallbackReason |

检索请求示例：

```json
{"query":"未发货订单可以退款吗？","topK":5,"mode":"AUTO"}
```

支持 AUTO、BM25、VECTOR、HYBRID。缺少 Key 或向量故障时，AUTO/HYBRID 返回实际 `BM25_ONLY` 和降级原因；显式 VECTOR 在不可用时返回 503。阈值过滤后没有结果时返回 `NO_RELEVANT_EVIDENCE`，不生成政策结论。

默认参数在 `.env.example` 中：TopK=5，RecallK=12，BM25 最低分数 1.2，词法覆盖率 0.35，余弦最低相似度 0.60，RRF K=60。`score` 分别为 BM25、余弦或 RRF 分数，不是置信概率。调参后需重新评测。

`api.http` 包含知识查询、RF1001 订单+政策、RF1003 物流+政策及无答案问题。Agent 按 `[DEMO-REFUND#unshipped-p1]` 引用实际检索条款；条件不足应询问，无证据应说明。工具选择由原生 ReActAgent 和模型完成，没有固定顺序或独立意图路由器。

启动服务后，在另一个 PowerShell 终端执行 35 条检索用例：

```powershell
./scripts/Evaluate-Rag.ps1
```

标注位于 `src/test/resources/evaluation/retrieval-cases.json`。报告包含 Chunk Recall@3/5、MRR、Source Recall@5 和无答案识别。没有可用真实 Embedding 时只报告实际 BM25 结果，不制造 Vector/Hybrid 比较数据。

文档统一保存在按主题命名的目录中，不使用日期前缀或版本后缀。检索报告默认写入 `output/P1开发记录/检索评测.md`，真实 Agent 报告默认写入同目录的 `真实Agent评测.md`。已有同名报告时，脚本在调用 API 前停止；再次评测可使用 `-ReportName 检索评测_阈值调整.md` 或 `-ReportName 真实Agent评测_模型切换.md` 按用途命名，保留原报告。运行时间和运行标识仍保留在正文或本地原始数据中。

真实 Agent 工具和引用审计需要读取本机运行日志：

```powershell
# 先创建目录；终端一启动并记录日志
New-Item -ItemType Directory -Path output/P1开发记录 -Force
./scripts/Start-Agent.ps1 *> output/P1开发记录/agent-runtime.log
# 终端二运行，RuntimeLog 须指向上述文件
./scripts/Test-P1Agent.ps1 -RuntimeLog output/P1开发记录/agent-runtime.log
```

真实脚本只使用合成场景，原始回复和日志保留本地、排除 Git；脱敏评测 Markdown 可提交。工具/引用审计不能代替事实、条件和拒答质量检查。原有 `Test-Api.ps1 -RealModel` 仍可验证 P0 场景。Windows PowerShell 5.1 响应使用原始字节进行 UTF-8 解码。

## 当前边界

- 已完成 P0 六次真实 DeepSeek 对话和 P1 十次真实组合咨询复测；更换模型、供应商或提示后仍需重新检查工具选择和事实回复，当前结果不代表任意输入都正确。
- 会话上下文使用原生内存 StateStore，重启丢失；数据库中的任务历史仍保留。没有任务恢复能力，进程异常退出可能留下 RUNNING 记录。
- 任务 SUCCEEDED 表示本轮正常生成回复，不代表订单存在或业务操作成功。工具查询失败会反馈给模型，事实拒答质量仍需真实模型验证。
- 原生循环最多 6 轮，达到上限记 FAILED；整体超时默认 90 秒，超时返回 504。上下文未做自动淘汰或压缩，适用于当前单机演示规模。
- P1 只加入 RAG 与业务工具协同；Chatwoot、Webhook、HITL、退款执行、任务恢复、复杂 Workflow、分布式任务、Multi-Agent、Skills 均未实现。

架构见 `docs/architecture.md`，代码调用关系见 `docs/implementation-notes.md`，RAG 设计和评测见 `docs/rag-design.md`、`docs/rag-evaluation.md`，验证证据见 `docs/verification.md`。P1 总结和代码索引位于 `output/P1开发记录/`。完成 P1 后停止，不自动进入 P2。
