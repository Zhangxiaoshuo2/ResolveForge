# 阶段二：Hybrid 检索与 Agent 协同

- 完成 Query → Lucene BM25 + 真/假可替换 Embedding 余弦召回 → 阈值过滤 → RRF → TopK Evidence。
- 缺配置或向量异常时明确 BM25_ONLY；显式 Vector 不可用时 503。无候选返回 NO_RELEVANT_EVIDENCE，不用随机或哈希向量兜底。
- 原生 Toolkit 增加 searchPolicy，结构化返回 Evidence；用户身份仍由 RuntimeContext 注入。保持原生 Loop，没有生产意图路由或固定工具顺序。
- 政策内容只保存通用条款，不存具体用户订单。P0 身份、业务工具、任务状态实现未修改。
- P1 新增确定性测试验证纯政策、组合工具、多轮、非法访问、无答案、引用、缓存变化、损坏向量、RRF 及 HTTP 协议。最终完整套件 28 项通过，其中 P0 12 项保留。

关键路径（相对项目根）：

| 路径 | 职责 |
|---|---|
| `src/main/java/dev/resolveforge/rag/PolicySearchService.java` | 实际模式、两路召回、阈值、Evidence |
| `src/main/java/dev/resolveforge/rag/RrfFusion.java` | 排名融合与去重 |
| `src/main/java/dev/resolveforge/rag/PolicyTools.java` | 原生 searchPolicy 工具 |
| `src/main/java/dev/resolveforge/rag/KnowledgeController.java` | 搜索与状态调试 API |
| `src/main/java/dev/resolveforge/agent/AgentService.java` | 注册工具与证据边界提示 |
| `src/test/java/dev/resolveforge/PolicyAgentApiTest.java` | 原生执行链 Fake 集成测试 |
| `src/test/java/dev/resolveforge/RagMechanicsTest.java` | 检索、缓存及错误机制测试 |

Fake Model/Fake Embedding 仅用于机制测试；实际效果以真实 Provider 评测报告为准。
