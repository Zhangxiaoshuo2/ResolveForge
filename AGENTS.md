# ResolveForge 项目协作约定

- 项目位置：`C:/Users/Lenovo/Desktop/ResolveForge`。
- GitHub 仓库：`https://github.com/Zhangxiaoshuo2/ResolveForge`。
- 用户已授权：每轮开发完成后，先完成适当验证，再提交并推送到该仓库；不需要重复询问提交/推送许可。遇到远程变化先获取并检查，不强制推送或覆盖他人提交。
- 提交源码、配置示例、文档和简短阶段总结；禁止提交 `.env`、真实密钥、私有 HTTP Client 环境、本地数据库、构建产物、运行日志及原始真实模型输出。
- 新建分支默认使用 `codex/` 前缀；保留正在工作的分支，除非任务要求另建分支。
- Windows + PowerShell + Java 21 + Maven；不得未经确认安装软件或修改系统级配置。
- P0 为独立 Spring Boot + AgentScope Java 2.0.4 原生 ReActAgent/Toolkit，默认 H2；Chatwoot 暂缓。不得自行进入 P1 或加入 HITL、RAG、退款、复杂 Workflow、Multi-Agent、Skills。
- 区分 Fake Model 测试与真实模型验证。每阶段生成简短实现总结与关键代码索引，位于 `output/YYYYMMDD_中文任务名_vNN/`；使用 `/` 书写本地路径。
