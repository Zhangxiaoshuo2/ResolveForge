# RAG 检索评测实际结果

Corpus hash: b70e1cd63d71e8f3f9b5733bc6ef75729436997e3553aa0c89a3aae306c808c7
Chunks: 19; embedding model: text-embedding-v4; dimensions: 1024; vector available: True
Cases: 35; evaluated via actual HTTP API. Scores are not LLM answer-quality scores.

| Mode | Executed | Recall@3 | Recall@5 | MRR | Source Recall@5 | No-answer correct | False abstentions |
|---|---|---|---|---|---|---|---|
| BM25 | yes | 0.5556 | 0.5556 | 0.5741 | 0.6481 | 8/8 | 8 |
| VECTOR | yes | 0.9815 | 1.0000 | 0.9259 | 1.0000 | 8/8 | 0 |
| HYBRID | yes | 1.0000 | 1.0000 | 0.9259 | 1.0000 | 8/8 | 0 |

Recall denominators contain answerable cases only; multiple expected chunks use per-query fractional recall. MRR uses the first relevant chunk. No-answer cases are evaluated separately. This is a small, authored development set without a held-out test set.

| Case | Mode | Code | Expected chunks | Retrieved chunks |
|---|---|---|---|---|
| R01 | BM25_ONLY | NO_RELEVANT_EVIDENCE | DEMO-REFUND#unshipped-p1 |  |
| R02 | BM25_ONLY | OK | DEMO-REFUND#unshipped-p1 | DEMO-REFUND#unshipped-p1, DEMO-SHIPPING#late-dispatch-p1, DEMO-REFUND#shipped-p1, DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#signed-missing-p1 |
| R03 | BM25_ONLY | OK | DEMO-REFUND#timing-p1 | DEMO-REFUND#timing-p1, DEMO-RETURN#inspection-p1 |
| R04 | BM25_ONLY | NO_RELEVANT_EVIDENCE | DEMO-REFUND#timing-p1 |  |
| R05 | BM25_ONLY | OK | DEMO-REFUND#shipped-p1 | DEMO-REFUND#unshipped-p1 |
| R06 | BM25_ONLY | OK | DEMO-REFUND#shipped-p1 | DEMO-REFUND#shipped-p1, DEMO-SHIPPING#late-dispatch-p1, DEMO-RECEIPT#after-receipt-p1, DEMO-SHIPPING#eta-p1, DEMO-LOGISTICS#damage-transit-p1 |
| R07 | BM25_ONLY | OK | DEMO-RETURN#seven-days-p1 | DEMO-RETURN#seven-days-p1, DEMO-RETURN#exclusions-p1, DEMO-SHIPPING#dispatch-p1, DEMO-RETURN#shipping-fee-p1, DEMO-REFUND#amount-p1 |
| R08 | BM25_ONLY | OK | DEMO-RETURN#exclusions-p1 | DEMO-RETURN#exclusions-p1, DEMO-RETURN#seven-days-p1, DEMO-RETURN#shipping-fee-p1 |
| R09 | BM25_ONLY | OK | DEMO-RETURN#exclusions-p1 | DEMO-RETURN#exclusions-p1, DEMO-RETURN#seven-days-p1 |
| R10 | BM25_ONLY | OK | DEMO-RETURN#exclusions-p1 | DEMO-RETURN#exclusions-p1, DEMO-RETURN#seven-days-p1, DEMO-REFUND#amount-p1, DEMO-RETURN#shipping-fee-p1 |
| R11 | BM25_ONLY | OK | DEMO-RETURN#quality-p1 | DEMO-RETURN#quality-p1, DEMO-RETURN#shipping-fee-p1 |
| R12 | BM25_ONLY | OK | DEMO-RETURN#shipping-fee-p1 | DEMO-RETURN#quality-p1, DEMO-RETURN#shipping-fee-p1 |
| R13 | BM25_ONLY | NO_RELEVANT_EVIDENCE | DEMO-RETURN#shipping-fee-p1 |  |
| R14 | BM25_ONLY | OK | DEMO-RETURN#shipping-fee-p1 | DEMO-RETURN#shipping-fee-p1, DEMO-RETURN#quality-p1, DEMO-RECEIPT#wrong-items-p1, DEMO-RECEIPT#damage-p1, DEMO-LOGISTICS#damage-transit-p1 |
| R15 | BM25_ONLY | NO_RELEVANT_EVIDENCE | DEMO-RETURN#inspection-p1 |  |
| R16 | BM25_ONLY | OK | DEMO-SHIPPING#dispatch-p1 | DEMO-SHIPPING#dispatch-p1 |
| R17 | BM25_ONLY | OK | DEMO-SHIPPING#late-dispatch-p1 | DEMO-SHIPPING#dispatch-p1 |
| R18 | BM25_ONLY | NO_RELEVANT_EVIDENCE | DEMO-REFUND#unshipped-p1, DEMO-SHIPPING#late-dispatch-p1 |  |
| R19 | BM25_ONLY | OK | DEMO-SHIPPING#eta-p1 | DEMO-SHIPPING#eta-p1, DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#damage-transit-p1 |
| R20 | BM25_ONLY | NO_RELEVANT_EVIDENCE | DEMO-LOGISTICS#stalled-p1 |  |
| R21 | BM25_ONLY | OK | DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#lost-p1 | DEMO-LOGISTICS#stalled-p1, DEMO-RECEIPT#damage-p1 |
| R22 | BM25_ONLY | OK | DEMO-LOGISTICS#lost-p1 | DEMO-RETURN#shipping-fee-p1, DEMO-RECEIPT#damage-p1 |
| R23 | BM25_ONLY | NO_RELEVANT_EVIDENCE | DEMO-LOGISTICS#signed-missing-p1 |  |
| R24 | BM25_ONLY | OK | DEMO-RECEIPT#damage-p1 | DEMO-RECEIPT#damage-p1 |
| R25 | BM25_ONLY | OK | DEMO-RECEIPT#damage-p1 | DEMO-RECEIPT#damage-p1, DEMO-LOGISTICS#stalled-p1, DEMO-SHIPPING#dispatch-p1 |
| R26 | BM25_ONLY | NO_RELEVANT_EVIDENCE | DEMO-RECEIPT#wrong-items-p1 |  |
| R27 | BM25_ONLY | OK | DEMO-RECEIPT#after-receipt-p1, DEMO-RETURN#seven-days-p1 | DEMO-RETURN#seven-days-p1 |
| R28 | BM25_ONLY | NO_RELEVANT_EVIDENCE |  |  |
| R29 | BM25_ONLY | NO_RELEVANT_EVIDENCE |  |  |
| R30 | BM25_ONLY | NO_RELEVANT_EVIDENCE |  |  |
| R31 | BM25_ONLY | NO_RELEVANT_EVIDENCE |  |  |
| R32 | BM25_ONLY | NO_RELEVANT_EVIDENCE |  |  |
| R33 | BM25_ONLY | NO_RELEVANT_EVIDENCE |  |  |
| R34 | BM25_ONLY | NO_RELEVANT_EVIDENCE |  |  |
| R35 | BM25_ONLY | NO_RELEVANT_EVIDENCE |  |  |
| R01 | VECTOR | OK | DEMO-REFUND#unshipped-p1 | DEMO-REFUND#unshipped-p1, DEMO-REFUND#shipped-p1, DEMO-SHIPPING#late-dispatch-p1, DEMO-RETURN#inspection-p1, DEMO-RECEIPT#wrong-items-p1 |
| R02 | VECTOR | OK | DEMO-REFUND#unshipped-p1 | DEMO-REFUND#unshipped-p1, DEMO-REFUND#shipped-p1, DEMO-SHIPPING#late-dispatch-p1, DEMO-RETURN#exclusions-p1, DEMO-RETURN#inspection-p1 |
| R03 | VECTOR | OK | DEMO-REFUND#timing-p1 | DEMO-REFUND#timing-p1, DEMO-RETURN#inspection-p1 |
| R04 | VECTOR | OK | DEMO-REFUND#timing-p1 | DEMO-REFUND#timing-p1 |
| R05 | VECTOR | OK | DEMO-REFUND#shipped-p1 | DEMO-REFUND#shipped-p1, DEMO-REFUND#unshipped-p1, DEMO-RECEIPT#after-receipt-p1 |
| R06 | VECTOR | OK | DEMO-REFUND#shipped-p1 | DEMO-REFUND#shipped-p1, DEMO-SHIPPING#eta-p1, DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#lost-p1, DEMO-LOGISTICS#damage-transit-p1 |
| R07 | VECTOR | OK | DEMO-RETURN#seven-days-p1 | DEMO-RETURN#seven-days-p1, DEMO-RETURN#exclusions-p1, DEMO-RETURN#quality-p1, DEMO-RECEIPT#after-receipt-p1, DEMO-RETURN#shipping-fee-p1 |
| R08 | VECTOR | OK | DEMO-RETURN#exclusions-p1 | DEMO-RETURN#seven-days-p1, DEMO-RETURN#exclusions-p1, DEMO-RETURN#quality-p1, DEMO-REFUND#amount-p1 |
| R09 | VECTOR | OK | DEMO-RETURN#exclusions-p1 | DEMO-RETURN#exclusions-p1, DEMO-RETURN#seven-days-p1, DEMO-RETURN#shipping-fee-p1, DEMO-REFUND#unshipped-p1, DEMO-RECEIPT#after-receipt-p1 |
| R10 | VECTOR | OK | DEMO-RETURN#exclusions-p1 | DEMO-RETURN#exclusions-p1, DEMO-RETURN#seven-days-p1, DEMO-REFUND#unshipped-p1, DEMO-REFUND#amount-p1, DEMO-RETURN#shipping-fee-p1 |
| R11 | VECTOR | OK | DEMO-RETURN#quality-p1 | DEMO-RETURN#quality-p1 |
| R12 | VECTOR | OK | DEMO-RETURN#shipping-fee-p1 | DEMO-RETURN#shipping-fee-p1, DEMO-REFUND#amount-p1, DEMO-RECEIPT#wrong-items-p1, DEMO-REFUND#unshipped-p1 |
| R13 | VECTOR | OK | DEMO-RETURN#shipping-fee-p1 | DEMO-RETURN#shipping-fee-p1, DEMO-RECEIPT#wrong-items-p1, DEMO-REFUND#amount-p1, DEMO-REFUND#unshipped-p1, DEMO-LOGISTICS#damage-transit-p1 |
| R14 | VECTOR | OK | DEMO-RETURN#shipping-fee-p1 | DEMO-RETURN#shipping-fee-p1, DEMO-RECEIPT#wrong-items-p1, DEMO-LOGISTICS#damage-transit-p1, DEMO-REFUND#amount-p1 |
| R15 | VECTOR | OK | DEMO-RETURN#inspection-p1 | DEMO-RETURN#inspection-p1, DEMO-REFUND#timing-p1, DEMO-LOGISTICS#stalled-p1, DEMO-RETURN#quality-p1, DEMO-SHIPPING#dispatch-p1 |
| R16 | VECTOR | OK | DEMO-SHIPPING#dispatch-p1 | DEMO-SHIPPING#dispatch-p1, DEMO-LOGISTICS#stalled-p1, DEMO-REFUND#timing-p1, DEMO-SHIPPING#late-dispatch-p1, DEMO-RECEIPT#after-receipt-p1 |
| R17 | VECTOR | OK | DEMO-SHIPPING#late-dispatch-p1 | DEMO-SHIPPING#late-dispatch-p1, DEMO-LOGISTICS#stalled-p1, DEMO-SHIPPING#dispatch-p1, DEMO-LOGISTICS#lost-p1, DEMO-REFUND#shipped-p1 |
| R18 | VECTOR | OK | DEMO-REFUND#unshipped-p1, DEMO-SHIPPING#late-dispatch-p1 | DEMO-REFUND#unshipped-p1, DEMO-SHIPPING#late-dispatch-p1, DEMO-REFUND#shipped-p1, DEMO-RETURN#inspection-p1, DEMO-RECEIPT#wrong-items-p1 |
| R19 | VECTOR | OK | DEMO-SHIPPING#eta-p1 | DEMO-SHIPPING#eta-p1, DEMO-LOGISTICS#stalled-p1, DEMO-SHIPPING#dispatch-p1 |
| R20 | VECTOR | OK | DEMO-LOGISTICS#stalled-p1 | DEMO-LOGISTICS#stalled-p1 |
| R21 | VECTOR | OK | DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#lost-p1 | DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#lost-p1, DEMO-LOGISTICS#damage-transit-p1, DEMO-RECEIPT#damage-p1, DEMO-RECEIPT#wrong-items-p1 |
| R22 | VECTOR | OK | DEMO-LOGISTICS#lost-p1 | DEMO-LOGISTICS#lost-p1, DEMO-RECEIPT#wrong-items-p1, DEMO-LOGISTICS#damage-transit-p1, DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#signed-missing-p1 |
| R23 | VECTOR | OK | DEMO-LOGISTICS#signed-missing-p1 | DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#signed-missing-p1, DEMO-LOGISTICS#lost-p1, DEMO-SHIPPING#eta-p1, DEMO-REFUND#shipped-p1 |
| R24 | VECTOR | OK | DEMO-RECEIPT#damage-p1 | DEMO-RECEIPT#damage-p1, DEMO-LOGISTICS#damage-transit-p1, DEMO-RECEIPT#wrong-items-p1 |
| R25 | VECTOR | OK | DEMO-RECEIPT#damage-p1 | DEMO-RETURN#quality-p1, DEMO-RECEIPT#damage-p1 |
| R26 | VECTOR | OK | DEMO-RECEIPT#wrong-items-p1 | DEMO-RECEIPT#damage-p1, DEMO-RECEIPT#wrong-items-p1, DEMO-LOGISTICS#damage-transit-p1, DEMO-RETURN#quality-p1 |
| R27 | VECTOR | OK | DEMO-RECEIPT#after-receipt-p1, DEMO-RETURN#seven-days-p1 | DEMO-RECEIPT#after-receipt-p1, DEMO-RETURN#inspection-p1, DEMO-REFUND#unshipped-p1, DEMO-RETURN#seven-days-p1 |
| R28 | VECTOR | NO_RELEVANT_EVIDENCE |  |  |
| R29 | VECTOR | NO_RELEVANT_EVIDENCE |  |  |
| R30 | VECTOR | NO_RELEVANT_EVIDENCE |  |  |
| R31 | VECTOR | NO_RELEVANT_EVIDENCE |  |  |
| R32 | VECTOR | NO_RELEVANT_EVIDENCE |  |  |
| R33 | VECTOR | NO_RELEVANT_EVIDENCE |  |  |
| R34 | VECTOR | NO_RELEVANT_EVIDENCE |  |  |
| R35 | VECTOR | NO_RELEVANT_EVIDENCE |  |  |
| R01 | HYBRID | OK | DEMO-REFUND#unshipped-p1 | DEMO-REFUND#unshipped-p1, DEMO-REFUND#shipped-p1, DEMO-SHIPPING#late-dispatch-p1, DEMO-RETURN#inspection-p1, DEMO-RECEIPT#wrong-items-p1 |
| R02 | HYBRID | OK | DEMO-REFUND#unshipped-p1 | DEMO-REFUND#unshipped-p1, DEMO-REFUND#shipped-p1, DEMO-SHIPPING#late-dispatch-p1, DEMO-RETURN#seven-days-p1, DEMO-LOGISTICS#stalled-p1 |
| R03 | HYBRID | OK | DEMO-REFUND#timing-p1 | DEMO-REFUND#timing-p1, DEMO-RETURN#inspection-p1 |
| R04 | HYBRID | OK | DEMO-REFUND#timing-p1 | DEMO-REFUND#timing-p1 |
| R05 | HYBRID | OK | DEMO-REFUND#shipped-p1 | DEMO-REFUND#unshipped-p1, DEMO-REFUND#shipped-p1, DEMO-RECEIPT#after-receipt-p1 |
| R06 | HYBRID | OK | DEMO-REFUND#shipped-p1 | DEMO-REFUND#shipped-p1, DEMO-SHIPPING#eta-p1, DEMO-RECEIPT#after-receipt-p1, DEMO-LOGISTICS#damage-transit-p1, DEMO-SHIPPING#late-dispatch-p1 |
| R07 | HYBRID | OK | DEMO-RETURN#seven-days-p1 | DEMO-RETURN#seven-days-p1, DEMO-RETURN#exclusions-p1, DEMO-RETURN#shipping-fee-p1, DEMO-RETURN#quality-p1, DEMO-SHIPPING#dispatch-p1 |
| R08 | HYBRID | OK | DEMO-RETURN#exclusions-p1 | DEMO-RETURN#exclusions-p1, DEMO-RETURN#seven-days-p1, DEMO-RETURN#quality-p1, DEMO-RETURN#shipping-fee-p1, DEMO-REFUND#amount-p1 |
| R09 | HYBRID | OK | DEMO-RETURN#exclusions-p1 | DEMO-RETURN#exclusions-p1, DEMO-RETURN#seven-days-p1, DEMO-RETURN#shipping-fee-p1, DEMO-REFUND#unshipped-p1, DEMO-RECEIPT#after-receipt-p1 |
| R10 | HYBRID | OK | DEMO-RETURN#exclusions-p1 | DEMO-RETURN#exclusions-p1, DEMO-RETURN#seven-days-p1, DEMO-REFUND#amount-p1, DEMO-RETURN#shipping-fee-p1, DEMO-REFUND#unshipped-p1 |
| R11 | HYBRID | OK | DEMO-RETURN#quality-p1 | DEMO-RETURN#quality-p1, DEMO-RETURN#shipping-fee-p1 |
| R12 | HYBRID | OK | DEMO-RETURN#shipping-fee-p1 | DEMO-RETURN#shipping-fee-p1, DEMO-RETURN#quality-p1, DEMO-REFUND#amount-p1, DEMO-RECEIPT#wrong-items-p1, DEMO-REFUND#unshipped-p1 |
| R13 | HYBRID | OK | DEMO-RETURN#shipping-fee-p1 | DEMO-RETURN#shipping-fee-p1, DEMO-RECEIPT#wrong-items-p1, DEMO-REFUND#amount-p1, DEMO-REFUND#unshipped-p1, DEMO-LOGISTICS#damage-transit-p1 |
| R14 | HYBRID | OK | DEMO-RETURN#shipping-fee-p1 | DEMO-RETURN#shipping-fee-p1, DEMO-RECEIPT#wrong-items-p1, DEMO-LOGISTICS#damage-transit-p1, DEMO-RETURN#quality-p1, DEMO-RECEIPT#damage-p1 |
| R15 | HYBRID | OK | DEMO-RETURN#inspection-p1 | DEMO-RETURN#inspection-p1, DEMO-REFUND#timing-p1, DEMO-LOGISTICS#stalled-p1, DEMO-RETURN#quality-p1, DEMO-SHIPPING#dispatch-p1 |
| R16 | HYBRID | OK | DEMO-SHIPPING#dispatch-p1 | DEMO-SHIPPING#dispatch-p1, DEMO-LOGISTICS#stalled-p1, DEMO-REFUND#timing-p1, DEMO-SHIPPING#late-dispatch-p1, DEMO-RECEIPT#after-receipt-p1 |
| R17 | HYBRID | OK | DEMO-SHIPPING#late-dispatch-p1 | DEMO-SHIPPING#dispatch-p1, DEMO-SHIPPING#late-dispatch-p1, DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#lost-p1, DEMO-REFUND#shipped-p1 |
| R18 | HYBRID | OK | DEMO-REFUND#unshipped-p1, DEMO-SHIPPING#late-dispatch-p1 | DEMO-REFUND#unshipped-p1, DEMO-SHIPPING#late-dispatch-p1, DEMO-REFUND#shipped-p1, DEMO-RETURN#inspection-p1, DEMO-RECEIPT#wrong-items-p1 |
| R19 | HYBRID | OK | DEMO-SHIPPING#eta-p1 | DEMO-SHIPPING#eta-p1, DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#damage-transit-p1, DEMO-SHIPPING#dispatch-p1 |
| R20 | HYBRID | OK | DEMO-LOGISTICS#stalled-p1 | DEMO-LOGISTICS#stalled-p1 |
| R21 | HYBRID | OK | DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#lost-p1 | DEMO-LOGISTICS#stalled-p1, DEMO-RECEIPT#damage-p1, DEMO-LOGISTICS#lost-p1, DEMO-LOGISTICS#damage-transit-p1, DEMO-RECEIPT#wrong-items-p1 |
| R22 | HYBRID | OK | DEMO-LOGISTICS#lost-p1 | DEMO-LOGISTICS#lost-p1, DEMO-RETURN#shipping-fee-p1, DEMO-RECEIPT#damage-p1, DEMO-RECEIPT#wrong-items-p1, DEMO-LOGISTICS#damage-transit-p1 |
| R23 | HYBRID | OK | DEMO-LOGISTICS#signed-missing-p1 | DEMO-LOGISTICS#stalled-p1, DEMO-LOGISTICS#signed-missing-p1, DEMO-LOGISTICS#lost-p1, DEMO-SHIPPING#eta-p1, DEMO-REFUND#shipped-p1 |
| R24 | HYBRID | OK | DEMO-RECEIPT#damage-p1 | DEMO-RECEIPT#damage-p1, DEMO-LOGISTICS#damage-transit-p1, DEMO-RECEIPT#wrong-items-p1 |
| R25 | HYBRID | OK | DEMO-RECEIPT#damage-p1 | DEMO-RECEIPT#damage-p1, DEMO-RETURN#quality-p1, DEMO-LOGISTICS#stalled-p1, DEMO-SHIPPING#dispatch-p1 |
| R26 | HYBRID | OK | DEMO-RECEIPT#wrong-items-p1 | DEMO-RECEIPT#damage-p1, DEMO-RECEIPT#wrong-items-p1, DEMO-LOGISTICS#damage-transit-p1, DEMO-RETURN#quality-p1 |
| R27 | HYBRID | OK | DEMO-RECEIPT#after-receipt-p1, DEMO-RETURN#seven-days-p1 | DEMO-RETURN#seven-days-p1, DEMO-RECEIPT#after-receipt-p1, DEMO-RETURN#inspection-p1, DEMO-REFUND#unshipped-p1 |
| R28 | HYBRID | NO_RELEVANT_EVIDENCE |  |  |
| R29 | HYBRID | NO_RELEVANT_EVIDENCE |  |  |
| R30 | HYBRID | NO_RELEVANT_EVIDENCE |  |  |
| R31 | HYBRID | NO_RELEVANT_EVIDENCE |  |  |
| R32 | HYBRID | NO_RELEVANT_EVIDENCE |  |  |
| R33 | HYBRID | NO_RELEVANT_EVIDENCE |  |  |
| R34 | HYBRID | NO_RELEVANT_EVIDENCE |  |  |
| R35 | HYBRID | NO_RELEVANT_EVIDENCE |  |  |
