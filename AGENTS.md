# ResolveForge 项目协作约定

- 项目位置：`C:/Users/Lenovo/Desktop/ResolveForge`。
- GitHub 仓库：`https://github.com/Zhangxiaoshuo2/ResolveForge`。
- 每轮开发完成后先完成适当验证；提交和推送前说明影响范围并等待用户确认，遵循用户最新协作约定。遇到远程变化先获取并检查，不强制推送或覆盖他人提交。
- 提交源码、配置示例、文档和简短阶段总结；禁止提交 `.env`、真实密钥、私有 HTTP Client 环境、本地数据库、构建产物、运行日志及原始真实模型输出。
- 新建分支默认使用 `codex/` 前缀；保留正在工作的分支，除非任务要求另建分支。
- Windows + PowerShell + Java 21 + Maven；不得未经确认安装软件或修改系统级配置。
- 当前阶段为 P1：从 P0 提交 `cf4d10ac4d2c2f4bf5e76e9564dd24f074a44b76` 建立 `codex/p1`，实现 Hybrid RAG 与业务工具协同。保留 Java 21、Spring Boot 4.0.4、AgentScope Java 2.0.4 原生 ReActAgent/Toolkit 及 H2，不修改 P0 身份校验和任务状态语义。
- P1 完成后停止，不自动进入 P2；不加入退款执行、HITL、状态恢复、Chatwoot、复杂 Workflow、Multi-Agent、Skills 或复杂前端。
- 真实向量默认百炼 text-embedding-v4 / 1024 维；凭据与聊天模型独立。允许缺配置或服务故障降级为 BM25-only，必须记录实际模式；不得用 Fake/随机/哈希向量冒充真实 Hybrid 效果。
- 区分 Fake Model 测试与真实模型验证。每阶段生成简短实现总结与关键代码索引，默认位于 `output/中文任务名/`；使用 `/` 书写本地路径。
- 文档文件名和文档目录使用清晰的主题名称，禁止日期前缀和版本后缀。不同线程使用独立主题目录；需保留多份报告时以用途或运行环境区分，不覆盖已有文档。日期、运行标识和依赖版本可作为正文元数据保留。
