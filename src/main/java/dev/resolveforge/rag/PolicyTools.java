package dev.resolveforge.rag;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PolicyTools {
    private static final Logger log=LoggerFactory.getLogger(PolicyTools.class);
    private final PolicySearchService search;
    public PolicyTools(PolicySearchService search) {this.search=search;}
    @Tool(name="searchPolicy",description="检索 ResolveForge 虚构演示售后政策（退款、退换货、发货、物流异常、签收）。政策结论必须查询本工具，并以检索到的 chunkId 引用；无可靠证据时不能编造。它不查询个人订单、不执行退款。",readOnly=true,concurrencySafe=true)
    public PolicySearchService.Result searchPolicy(@ToolParam(name="query",description="需核实的政策问题，包含必要适用条件；不是订单事实查询") String query,RuntimeContext context) {
        long start=System.nanoTime();
        try {
            if(context==null || context.getUserId()==null) throw new IllegalStateException("missing_identity");
            var result=search.search(query,null,PolicySearchService.Mode.AUTO);
            log.info("tool_call tool=searchPolicy user={} session={} mode={} sources={} durationMs={} outcome={}",context.getUserId(),context.getSessionId(),
                    result.mode(),result.evidence().stream().map(PolicySearchService.Evidence::chunkId).toList(),(System.nanoTime()-start)/1_000_000,result.code());
            return result;
        } catch(Exception ex) {
            log.warn("tool_failed tool=searchPolicy type={}",ex.getClass().getSimpleName());
            return new PolicySearchService.Result("POLICY_UNAVAILABLE","UNAVAILABLE",null,java.util.List.of());
        }
    }
}
