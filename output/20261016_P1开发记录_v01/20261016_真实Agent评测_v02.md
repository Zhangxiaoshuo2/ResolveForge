# 真实 Agent 工具调用与引用审计

Run: 20261016204520; real model: deepseek-chat. Questions use synthetic demo data only.

| Case | Expected tools | Observed tools (this task) | Missing | Citations | Unsupported citations |
|---|---|---|---|---|---|
| A01 | searchPolicy | searchPolicy |  | DEMO-REFUND#unshipped-p1, DEMO-REFUND#shipped-p1, DEMO-SHIPPING#late-dispatch-p1 |  |
| A02 | queryOrder, searchPolicy | queryOrder, searchPolicy |  | DEMO-REFUND#unshipped-p1, DEMO-RETURN#seven-days-p1, DEMO-SHIPPING#late-dispatch-p1 |  |
| A03 | queryLogistics, searchPolicy | queryLogistics, searchPolicy |  | DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#lost-p1, DEMO-SHIPPING#eta-p1 |  |
| A04 | searchPolicy | searchPolicy |  | DEMO-RETURN#seven-days-p1, DEMO-RETURN#exclusions-p1, DEMO-REFUND#unshipped-p1, DEMO-REFUND#amount-p1 |  |
| A05 | searchPolicy | searchPolicy |  |  |  |
| A06 | queryOrder | queryOrder, searchPolicy |  | DEMO-REFUND#unshipped-p1, DEMO-REFUND#shipped-p1, DEMO-RETURN#seven-days-p1 |  |
| A07 | queryOrder | queryOrder |  |  |  |
| A08 | queryLogistics, searchPolicy | queryLogistics, searchPolicy |  | DEMO-LOGISTICS#stalled-p1, DEMO-REFUND#shipped-p1 |  |
| A09 |  |  |  |  |  |
| A10 | queryOrder, searchPolicy | queryOrder, searchPolicy |  | DEMO-REFUND#unshipped-p1, DEMO-REFUND#amount-p1 |  |

Tool usage is bounded by this taskId start/end log entries. Citation membership is checked against evidence retrieved in the same authenticated session before this task finished; later turns cannot validate an earlier citation. Factual correctness, condition handling and refusal quality still require reviewing actual replies. Raw replies stay local and are excluded from Git.
