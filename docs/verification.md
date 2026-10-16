# 验证记录（P0 + P1，2026-10-16）

## P1 最终构建与检索验证

基线 `cf4d10ac4d2c2f4bf5e76e9564dd24f074a44b76`，分支 `codex/p1`。保留 P0 身份校验、CommerceTools、ChatExecutionService、TaskRepository 的实现与状态语义。

| 检查 | 实际结果 | 证据 |
|---|---|---|
| `mvn -B -ntp package` | **28 tests，0 failures，0 errors，0 skipped；BUILD SUCCESS** | `output/20261016_P1开发记录_v01/16_final-build.log` |
| P0 原有 12 项 | 11 AgentApiTest + 1 CommerceToolsTest 均通过 | `target/surefire-reports/`；Toolkit schema 仅适配第三个工具 |
| P1 原生 Agent + Fake Provider | PolicyAgentApiTest 7 项通过 | 同上，Fake 只验证执行机制 |
| 缓存、BM25、RRF、降级与数值边界 | RagMechanicsTest 7 项通过 | 同上，使用明确 Fake 基向量 |
| Embedding HTTP 协议 | EmbeddingHttpTest 1 项通过 | 同上，不使用真实凭据 |
| 评测标签有效性 | RetrievalDatasetTest 1 项通过，35 条用例 | 同上 |
| 真实百炼 Embedding | HTTP 200，text-embedding-v4，1024 维；19 个当前 Chunk 向量可用 | 实际接口及索引状态；非 Fake |
| 真实检索比较 | BM25、Vector、Hybrid 全部实际执行，无降级 | `20261016_检索评测_v01.md`、`20261016_检索评测_v02.md` |
| PowerShell 兼容性 | PowerShell 7 / Windows PowerShell 5.1 评测指标一致 | 修复根 JSON 数组嵌套问题后均完成 35 条 |
| 生产包内容 | 包含 5 份知识文档和 PolicySearchService，不包含 Fake 测试类 | `jar tf target/resolveforge-0.1.0.jar` |
| 生产 JAR 重启复用向量 | cacheHits=19、embeddedChunks=0、vectorAvailable=true | `17_packaged-runtime.log`、本地 `rag-warm-status.json` |
| 生产 JAR + 真实 DeepSeek + 真实百炼 | 10 次对话完成，必要工具缺失=0，未检索引用=0；逐条审阅事实与拒答 | `20261016_真实Agent评测_v02.md`，运行 `20261016204520` |
| PostgreSQL | 未执行，未计入上述 28 项 | 显式 `postgres-it` 仍需要外部实例/Docker |

真实检索结果：Hybrid Recall@3=1.0000、Recall@5=1.0000、MRR@5=0.9259；无答案识别 8/8。BM25-only Recall@3/5=0.5556，8 条有答案用例漏答，降级质量明显有限。详细指标、分母、多目标标签口径及开发集限制见 `docs/rag-evaluation.md`，不能将小集结果解释为生产准确率。

本轮真实 Provider 与 Fake Provider 严格分开：默认自动化测试不访问外部模型，真实脚本依赖 `.env` 的独立 Embedding Key 和 LLM Key。源代码、测试与文档不包含实际密钥；向量缓存、原始合成对话、运行日志均排除 Git。

### P1 真实 Agent 最终核对

最终复测直接运行打包后的 JAR；聊天模型为 DeepSeek `deepseek-chat`，政策向量为真实百炼 `text-embedding-v4` / 1024 维，所有实际政策工具调用均报告 HYBRID。

- 未发货退款咨询：检索后引用 DEMO-REFUND#unshipped-p1 等实际条款，说明 PAID/UNSHIPPED 条件。
- RF1001 订单+政策：核对机械键盘、399.00、PAID，说明无需先退货，未声称已执行退款。
- RF1003 物流+政策：核对 DEMO-SF1003、IN_TRANSIT、杭州转运中心，说明 72 小时是核查触发条件，不把停滞认定为丢件或自动退款；时间不足时询问。
- 所有商品无条件退款：说明七天期限、商品完好条件和定制/生鲜/已兑换数字商品的排除条款，不泛化。
- 会员积分问题：实际工具 NO_RELEVANT_EVIDENCE，明确无可靠政策依据，没有生成不存在的条款。
- alice 文字声称 bob：RF2001 仍返回统一不可访问语义，无商品/金额泄漏。
- 多轮：RF1003 首问后，不重复订单号的物流/政策追问仍正确衔接；bob 使用相同 sessionId 时被要求提供订单号，没有继承 alice 事实。
- 伪装工具指令：本次样例未诱导模型跳过工具或声称退款已执行。只证明该样例，不宣称普遍抵御提示注入。

引用审计以本轮 taskId 日志边界识别工具，以该用户会话在本轮结束前已有的证据核对引用；不存在用后续轮次补证。A07 只观察到订单工具，A08 才组合物流与政策工具。应用仍用 P0 的正常回复/拒答 SUCCEEDED 语义，订单不可访问不等于任务执行失败。

本轮修复了：政策字符串的二次序列化、Fake 工具调用 ID 重复导致的测试干扰、PowerShell 5.1 根 JSON 数组嵌套和审计跨未来轮次混入的问题。真实答复中一处把“未满核查时限”推成“正常”的措辞已通过提示约束修正，最终复测未再出现该表述。详细原始日志仅留本地，不提交。

以下为 P0 历史验证，仍保留以便追溯；P1 不实现 Chatwoot、HITL、退款执行、任务恢复或 P2 功能。

---

## P0 历史记录

最终验收范围采用用户本轮调整：独立 Agent + 轻量本地数据库 + REST/IntelliJ HTTP Client，Chatwoot 暂缓。

## 实际运行结果

| 验证 | 结果 | 证据 |
|---|---|---|
| Java 21 + Boot 4.0.4 + AgentScope 2.0.4 编译 | 通过 | `output/20261016_P0开发记录_v01/02_compile.log` |
| `mvn -B -ntp package` | 通过，含测试及可执行 JAR | `output/20261016_P0开发记录_v01/09_final-build.log` |
| Agent HTTP 集成测试 | 11 项通过 | `target/surefire-reports/dev.resolveforge.AgentApiTest.txt` |
| 工具故障与可信身份测试 | 1 项通过 | `target/surefire-reports/dev.resolveforge.CommerceToolsTest.txt` |
| Windows PowerShell 5.1 初始化和启动脚本 | 通过，H2 启动成功 | `output/20261016_P0开发记录_v01/10_start-script.log` |
| 本地真实 HTTP 业务冒烟 | 通过 | `output/20261016_P0开发记录_v01/11_local-smoke.log` |
| 文件 H2 重启持久化 | 通过，重启后原 FAILED 任务可查 | 同上 |
| 未配置模型行为 | 503，FAILED/MODEL_NOT_CONFIGURED | `05_runtime.log`，重启后任务已复查 |
| 真实 LLM 工具选择与回复 | **真实 DeepSeek 六次对话通过** | `output/20261016_真实模型联调_v01/20261016_真实模型联调总结_v01.md` |
| PostgreSQL | **未执行：当前无实例/Docker** | `PostgresIT` 是显式可选测试，未计入 12 项 |
| Chatwoot/Webhook | **不在调整后的 P0 范围** | 无适配代码、无运行依赖 |

总计 **12 tests，0 failures，0 errors，0 skipped**。这里的 0 skipped 仅指默认测试集合，不表示 PostgreSQL 测试已执行。最终生产 JAR 为 `target/resolveforge-0.1.0.jar`，已检查包含应用入口，不包含 FakeOpenAiServer、AgentApiTest 或 Chatwoot 适配类。

## 三个业务场景

1. **订单**：HTTP 请求查询 RF1001，真实 ReActAgent 接收 Fake 模型 tool_calls，原生 Toolkit 执行 queryOrder，业务 HTTP 返回机械键盘/PAID/399，第二次模型请求包含工具结果，任务 SUCCEEDED。
2. **物流**：查询 RF1003，工具为 queryLogistics，返回 IN_TRANSIT、杭州转运中心、DEMO-SF1003。
3. **多轮**：同一用户、同一 sessionId 先查询 RF1003，再问“它的物流呢”；断言下一次模型请求包含前轮历史，物流工具仍查询 RF1003。另一用户或另一 sessionId 无法继承该上下文。

上述模型是 test 源码中的规则端点，工具选择和省略订单号后的解析由测试规则产生。这证明原生循环、HTTP 工具和上下文传递可用，**不构成真实模型决策与自然语言理解的证据**。

## 真实 DeepSeek 联调（后续完成）

2026-10-16 17:32（Asia/Shanghai），使用 `.env` 中配置的真实 `deepseek-chat`，请求实际发送至 `https://api.deepseek.com/chat/completions`；本机 H2 与真实 HTTP 业务 API 提供数据。验证运行标识：`20261016173228`。六次对话均完成，日志包含六次实际工具调用，未使用 Fake Model。

| 对话 | 实际工具 | 已核对回复 |
|---|---|---|
| RF1001 订单 | queryOrder | 机械键盘、399.00、PAID |
| RF1003 物流 | queryLogistics | 顺丰、DEMO-SF1003、运输中、杭州转运中心 |
| 多轮首问 RF1003 | queryOrder | 显示器、1599.00、SHIPPED |
| 同会话“它的物流到哪里了” | queryLogistics | 正确沿用 RF1003 及其物流 |
| alice 文本声称 bob，查 RF2001 | queryOrder / not_found | 未泄露商品或金额；回复不存在或不属于当前账户 |
| RF9999 无效订单 | queryOrder / not_found | 回复不存在或不属于当前账户 |

首次运行发现 Windows PowerShell 5.1 对 JSON 响应的编码误判。已修复脚本，第二次完整运行中文正常，并逐项核对事实及工具日志。真实 API 输出和运行日志保留在本地，Git 中只保存此脱敏总结；该结果仅支持上述已运行场景，不代表任意输入均正确。

## 安全与错误覆盖

- alice 访问 bob 的 RF2001：订单和物流均 404；与 RF9999 响应一致。
- 文本声称“我是 bob”或仅伪造 `X-User-Id`，均不能切换身份。
- 模型可见 schema 不包含 userId 或 RuntimeContext，只有 orderNo。
- alice/bob 并发使用同一 sessionId，各自只能读取本人数据。
- 无效 Token、空消息、非法 sessionId 被拒绝；其他用户任务详情返回 404。
- 业务 API 500 或缺少可信上下文：工具反馈 COMMERCE_UNAVAILABLE。
- 模型 401：HTTP 502、任务 FAILED/AGENT_FAILED。
- 模型持续调用工具：原生执行 6 轮并做一次上限总结，应用按 GenerateReason 返回 502/AGENT_ITERATION_LIMIT，任务 FAILED。
- 故意延迟模型：5 秒测试总超时返回 504/AGENT_TIMEOUT；同一会话随后正常请求成功，验证取消后可再次使用。

## 已处理的开发问题与剩余限制

- Windows PowerShell 5.1 的 Split 重载与无 BOM 中文脚本解析问题已修复；脚本真实运行通过。
- 扩展测试发现原生上限总结不能直接视作成功；现已检查原生终止原因，测试通过。
- Windows 运行中 JAR 曾导致 repackage 重命名失败；停止该测试 JVM 后完整 package 成功，失败日志保留在 `08_verify.log`。打包前应停止以同一 JAR 运行的服务。
- Flyway 针对 H2 2.4.240 打印“验证版本较新”提示；本机首次迁移、重启校验和数据查询都已通过。没有据此推断所有 H2/PostgreSQL SQL 均兼容。
- SDK 在取消模型请求时可能输出 Reactor `onErrorDropped` 诊断。本轮验证 HTTP 超时、任务终态及后续会话均正确；未修改全局 Reactor hooks 来隐藏这些诊断。
- 任务历史持久化；会话状态仍在内存，重启清空。异常退出可能留下 RUNNING 记录，未实现自动恢复。
- 真实 LLM 已完成上述场景验证；真实 PostgreSQL 尚未验证。HITL、退款、RAG、分布式任务均未实现。

## 可重复命令

```powershell
Set-Location 'C:/Users/Lenovo/Desktop/ResolveForge'
./scripts/Initialize-Local.ps1
mvn -B -ntp package
./scripts/Start-Agent.ps1
# 在另一终端执行
./scripts/Test-Api.ps1
# 填好 .env 并重启服务后才执行
./scripts/Test-Api.ps1 -RealModel
```

真实模型验收需同时检查 `tool_call` 日志和事实回复。六次真实对话的输出由脚本保存到 `output/YYYYMMDD_真实模型联调_v01/`；原始 JSON 和日志不提交到仓库。不把未执行的 PostgreSQL 等项目列为通过。
