# 阶段 1：模拟售后业务

已实现：Spring Boot 4.0.4 + Java 21 + Maven；默认文件 H2；Flyway 建表与演示数据；订单/物流 REST API；Bearer 用户认证和内部业务服务认证；SQL 同时按订单号及当前用户筛选。

非本人订单与不存在订单都返回相同的 `404 ORDER_NOT_FOUND`，避免暴露他人订单是否存在。请求体、文本和用户提供的 `X-User-Id` 无法覆盖已认证身份。

关键索引：

| 文件 | 职责 |
|---|---|
| `src/main/java/dev/resolveforge/commerce/CommerceController.java` | 订单和物流 GET API |
| `src/main/java/dev/resolveforge/commerce/CommerceRepository.java` | 参数化 SQL、订单归属过滤 |
| `src/main/java/dev/resolveforge/api/IdentityService.java` | 用户 Token 与服务 Token 校验 |
| `src/main/resources/db/migration/V1__commerce_and_channel.sql` | 订单与物流表（无渠道表） |
| `src/main/resources/db/migration/V2__demo_orders.sql` | alice/bob 测试订单 |
| `src/main/resources/application-postgres.yml` | 可选 PostgreSQL 配置 |

验证：阶段编译通过；首轮自动化 HTTP 测试共 9 项通过，其中覆盖 API 的正常状态、归属、未知订单及认证。见 `03_tests.log`。PostgreSQL 实例尚未验证，不以 H2 兼容模式等同于 PostgreSQL 实测。
