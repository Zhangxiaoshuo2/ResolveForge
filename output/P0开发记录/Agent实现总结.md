# 阶段 2：原生 Agent 与任务状态

已实现：`ReActAgent` + `Toolkit` + `OpenAIChatModel`，无手写推理循环；注册 `queryOrder`、`queryLogistics`；工具经 HTTP 查询业务 API；原生 `RuntimeContext(userId,sessionId)` 和 `InMemoryAgentStateStore` 管理多轮上下文；每轮持久化 RUNNING/SUCCEEDED/FAILED 基础状态。

关键索引：

| 文件 | 职责 |
|---|---|
| `src/main/java/dev/resolveforge/agent/AgentService.java` | 原生组件组装与调用、超时 |
| `src/main/java/dev/resolveforge/agent/CommerceTools.java` | 两个工具及注入的可信 RuntimeContext |
| `src/main/java/dev/resolveforge/agent/CommerceApiClient.java` | HTTP 业务访问，Agent 不依赖业务 Repository |
| `src/main/java/dev/resolveforge/agent/ModelAuditMiddleware.java` | 原生模型调用钩子，记录耗时及错误类型 |
| `src/main/java/dev/resolveforge/agent/ChatExecutionService.java` | 每轮请求与任务状态衔接 |
| `src/main/java/dev/resolveforge/agent/TaskRepository.java` | 状态和回复持久化、按用户查询 |
| `src/main/java/dev/resolveforge/api/ChatController.java` | `/api/chat`、`/api/tasks` |
| `src/test/java/dev/resolveforge/AgentApiTest.java` | 真实 HTTP 与原生工具循环集成测试 |
| `src/test/java/dev/resolveforge/FakeOpenAiServer.java` | 明确标记的测试协议端点，仅存在 test 源码 |

验证：首轮 9 项测试全部通过，包含订单工具、物流工具、多轮历史传递、用户/会话隔离、并发身份隔离、模型报错及任务状态。Fake Model 用规则返回预设 tool_calls，只能证明执行链与历史传递，不能证明真实模型的自主决策。

边界：P0 不实现会话重启恢复、HITL、退款、RAG、分布式调度或 Chatwoot；任务记录持久化不等于任务可恢复。
