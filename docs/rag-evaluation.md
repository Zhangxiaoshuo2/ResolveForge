# P1 检索与 Agent 评测

日期：2026-10-16（Asia/Shanghai）。固定语料为 5 份虚构演示政策、19 个 Chunk；语料哈希 `b70e1cd63d71e8f3f9b5733bc6ef75729436997e3553aa0c89a3aae306c808c7`。政策不代表真实平台规则。

## 评测集与口径

`src/test/resources/evaluation/retrieval-cases.json` 包含 **35 条**人工编写用例：27 条有答案、8 条知识库无答案。每条包含 id、category、query、expectedSourceIds、expectedChunkIds。覆盖同义表达、具体条款、条件限制、订单/政策混合问题及会员积分、关税、征信等未覆盖主题。自动化测试检查标签存在且与真实 Chunk/Source 对应。

- Chunk Recall@K：对每条有答案用例，计算 TopK 覆盖的期望 Chunk 数 / 期望 Chunk 总数，再对 27 条取均值；多标签用例按覆盖比例计算。
- MRR：每条有答案用例的首个相关 Chunk 的倒数排名均值；未命中记 0。本次请求 Top5，所以为 Top5 截断口径。
- Source Recall@5：同样方法，对 sourceId 而非 chunkId 统计。
- 无答案识别：8 条无答案用例中返回 `NO_RELEVANT_EVIDENCE` 且 evidence=[] 的数量。
- 有答案漏答：27 条有答案用例中错误返回 NO_RELEVANT_EVIDENCE 的数量，单列而不隐藏。

结果来自实际 HTTP 检索 API；显式请求 BM25、VECTOR、HYBRID。向量真实来自阿里云百炼 **text-embedding-v4，1024 维**，非 Fake、随机或哈希向量。Vector/Hybrid 复用同一查询向量缓存以保证比较一致并避免重复调用；文档向量来自实际 API 并保存在 H2。

默认配置在运行前确定：RecallK=12、TopK=5、BM25 最低分 1.2、词法覆盖率 0.35、余弦最低相似度 0.60、RRF K=60、融合最低分 0。未在看到指标后更改阈值或增加意图路由。

## 实际检索结果

PowerShell 7 和 Windows PowerShell 5.1 分别运行完整评测，结果一致。没有发生降级；没有以 BM25 结果填充 Vector/Hybrid 指标。

| 模式 | Chunk Recall@3 | Chunk Recall@5 | MRR@5 | Source Recall@5 | 无答案正确 | 有答案漏答 |
|---|---|---|---|---|---|---|
| BM25-only | 0.5556 | 0.5556 | 0.5741 | 0.6481 | 8/8 | 8/27 |
| Vector（真实百炼） | 0.9815 | 1.0000 | 0.9259 | 1.0000 | 8/8 | 0/27 |
| Hybrid（真实百炼 + RRF） | 1.0000 | 1.0000 | 0.9259 | 1.0000 | 8/8 | 0/27 |

逐条期望和命中见 `output/20261016_P1开发记录_v01/20261016_检索评测_v01.md`（PowerShell 7）与 `20261016_检索评测_v02.md`（Windows PowerShell 5.1）。原始 JSON 及运行日志仅保留本地。

BM25-only 漏答用例为 R01、R04、R13、R15、R18、R20、R23、R26，多为同义表达或混合问题。当前词法覆盖率门槛偏向拒绝弱证据，降级可用不代表效果等同 Hybrid。Vector 在一条多目标用例上 Top3 未覆盖全部期望 Chunk，Top5 已覆盖；RRF 将该批 Top3 覆盖提升至 1.0000，但 MRR 没有提升，不能声称所有指标均优于 Vector。

这只是小型、由开发者同时维护政策与标注的**开发集**，没有独立留出集、真实客服流量、盲审或置信区间。100% Recall 不代表生产问题准确率；没有证据证明对任意未知问题都能正确拒答。检索相关性也不自动证明答案忠实、适用条件正确或引用完整。

## 确定性自动化验证

- 原有 P0 12 项场景保留；仅将 Toolkit schema 断言由 2 个工具更新为 3 个，并为新工具断言 query 参数。
- Fake Model 与明确手工构造的 Fake Embedding 基向量，只用于原生执行链、并发身份、缓存、维度、融合、无答案状态与错误边界验证。
- Native ReActAgent 测试覆盖纯政策、订单+政策、物流+政策、条件限制、无答案、非法访问、多轮及不同用户隔离。
- Embedding HTTP 测试检查响应 index 重排、模型/维度不符、非数字/零向量及协议错误；缓存测试检查重启复用、正文变更、模型/维度变化及损坏缓存重建。
- Fake 指标不作为上述真实检索表的数据来源。最终测试与打包结果以 `docs/verification.md` 的 P1 记录为准。

## 真实 Agent 验证方法

`scripts/Test-P1Agent.ps1` 使用真实 DeepSeek `deepseek-chat`，每次执行 10 次合成对话，覆盖七个验收类别，并额外检查用户伪装工具指令的样例。

工具名称从每个 taskId 的开始/结束日志区间提取；引用与同一认证会话在该请求结束前已经检索到的 Chunk 对照，不能利用未来轮次的证据补证。日志只记录工具/模式/来源等元数据，不记录完整私有对话或内部推理。原始合成问答仅在本地评测输出中保存，并排除 Git。

审计结果只证明工具被调用、引用标识有来源；仍逐条检查业务事实、条件限制、无答案拒答、无越权数据及未声称执行退款。不是独立模型裁判或全面安全基准。最终实际结果与细节见本轮真实 Agent Markdown 报告及 `docs/verification.md`。

最终生产 JAR 复测运行 `20261016204520`：**10 次对话完成，预期工具缺失 0，未检索来源引用 0**。A01/A04 为一般政策，A02 组合订单与政策，A03/A08 组合物流与政策，A05 无答案拒答，A06 越权拒绝，A07/A08 多轮，A09 用户隔离，A10 用户伪装工具指令样例。人工逐条核对了订单/物流事实、条件限制及未执行退款。记录见 `output/20261016_P1开发记录_v01/20261016_真实Agent评测_v02.md`。

`v01` 为首次运行的历史报告，工具聚合曾以整个会话为单位，不能用来声称某一轮具体调用了什么；最终 `v02` 按 taskId 边界审计，以此为准。首次答复中出现“未满 72 小时即正常”的过度推断，增加阈值不等于正常的提示约束后复测，最终答复保留了条件不足与询问。单次复测不是长期稳定性证明。

## 重复运行

```powershell
# 已启动服务并导入 .env；可先检查 /api/knowledge/status
./scripts/Evaluate-Rag.ps1
./scripts/Test-P1Agent.ps1 -RuntimeLog output/20261016_P1开发记录_v01/agent-runtime.log
```

缺少或不可用的真实 Embedding 会让 Vector 测试不执行或失败，Hybrid 降级结果不会统计成 Hybrid 指标。更换供应商、模型、维度、政策或阈值后应重新评测。PostgreSQL 尚未实测，Chatwoot、HITL、退款执行及任务恢复不属于 P1。
