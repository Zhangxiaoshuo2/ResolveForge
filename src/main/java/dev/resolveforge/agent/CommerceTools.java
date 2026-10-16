package dev.resolveforge.agent;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommerceTools {
    private static final Logger log = LoggerFactory.getLogger(CommerceTools.class);
    private final CommerceApiClient client;
    public CommerceTools(CommerceApiClient client) { this.client = client; }
    @Tool(name="queryOrder", description="查询当前已认证用户的订单商品、金额、订单状态。必须提供订单号。", readOnly=true, concurrencySafe=true)
    public String queryOrder(@ToolParam(name="orderNo", description="订单号，例如 RF1003") String orderNo, RuntimeContext context) {
        return query(orderNo, context, false);
    }
    @Tool(name="queryLogistics", description="查询当前已认证用户订单的物流、快递单号和最新运输或签收进度。", readOnly=true, concurrencySafe=true)
    public String queryLogistics(@ToolParam(name="orderNo", description="订单号，例如 RF1003") String orderNo, RuntimeContext context) {
        return query(orderNo, context, true);
    }
    private String query(String orderNo, RuntimeContext context, boolean logistics) {
        String tool = logistics ? "queryLogistics" : "queryOrder";
        long start = System.nanoTime();
        try {
            if (context == null || context.getUserId() == null) throw new IllegalStateException("missing_identity");
            String result = client.query(context.getUserId(), orderNo, logistics);
            log.info("tool_call tool={} user={} session={} durationMs={} outcome={}", tool, context.getUserId(), context.getSessionId(),
                    (System.nanoTime()-start)/1_000_000, result.contains("ORDER_NOT_FOUND") ? "not_found" : "ok");
            return result;
        } catch (RuntimeException ex) {
            log.warn("tool_failed tool={} type={}", tool, ex.getClass().getSimpleName());
            return "{\"code\":\"COMMERCE_UNAVAILABLE\",\"message\":\"业务查询失败，请稍后重试，不得编造订单或物流\"}";
        }
    }
}
