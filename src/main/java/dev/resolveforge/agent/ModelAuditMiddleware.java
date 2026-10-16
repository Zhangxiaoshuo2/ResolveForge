package dev.resolveforge.agent;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

public class ModelAuditMiddleware implements MiddlewareBase {
    private static final Logger log = LoggerFactory.getLogger(ModelAuditMiddleware.class);
    @Override
    public Flux<AgentEvent> onModelCall(Agent agent, RuntimeContext ctx, ModelCallInput input,
                                       Function<ModelCallInput, Flux<AgentEvent>> next) {
        return Flux.defer(() -> {
            long start = System.nanoTime();
            log.info("model_call user={} session={}", ctx.getUserId(), ctx.getSessionId());
            return next.apply(input).doOnError(ex -> log.warn("model_failed session={} type={}", ctx.getSessionId(), ex.getClass().getSimpleName()))
                    .doFinally(signal -> log.info("model_finished session={} signal={} durationMs={}", ctx.getSessionId(), signal, (System.nanoTime()-start)/1_000_000));
        });
    }
}
