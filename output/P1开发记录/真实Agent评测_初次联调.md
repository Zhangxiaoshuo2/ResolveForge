# 真实 Agent 工具调用与引用审计

Run: 20261016204025; real model: deepseek-chat. Questions use synthetic demo data only.

| Case | Expected tools | Observed tools (session) | Missing | Citations | Unsupported citations |
|---|---|---|---|---|---|
| A01 | searchPolicy | searchPolicy |  | DEMO-REFUND#unshipped-p1, DEMO-REFUND#shipped-p1, DEMO-REFUND#amount-p1 |  |
| A02 | queryOrder, searchPolicy | searchPolicy, queryOrder |  | DEMO-REFUND#unshipped-p1, DEMO-REFUND#amount-p1 |  |
| A03 | queryLogistics, searchPolicy | queryLogistics, searchPolicy |  | DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#lost-p1, DEMO-SHIPPING#eta-p1 |  |
| A04 | searchPolicy | searchPolicy |  | DEMO-RETURN#seven-days-p1, DEMO-RETURN#exclusions-p1, DEMO-REFUND#amount-p1 |  |
| A05 | searchPolicy | searchPolicy |  |  |  |
| A06 | queryOrder | queryOrder, searchPolicy |  | DEMO-REFUND#unshipped-p1, DEMO-RETURN#seven-days-p1, DEMO-REFUND#amount-p1, DEMO-REFUND#timing-p1 |  |
| A07 | queryOrder | queryOrder, queryLogistics, searchPolicy |  |  |  |
| A08 | queryLogistics, searchPolicy | queryOrder, queryLogistics, searchPolicy |  | DEMO-REFUND#shipped-p1, DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#lost-p1, DEMO-SHIPPING#eta-p1 |  |
| A09 |  |  |  |  |  |
| A10 | queryOrder, searchPolicy | queryOrder, searchPolicy |  | DEMO-REFUND#unshipped-p1, DEMO-REFUND#amount-p1 |  |

Citation membership is checked against searchPolicy log evidence in the same authenticated session. Multi-turn rows share session history. Factual correctness, condition handling and refusal quality still require reviewing actual replies. Raw replies stay local and are excluded from Git.
