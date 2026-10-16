package dev.resolveforge;

import com.sun.net.httpserver.HttpServer;
import dev.resolveforge.agent.CommerceApiClient;
import dev.resolveforge.agent.CommerceTools;
import dev.resolveforge.config.AppProperties;
import io.agentscope.core.agent.RuntimeContext;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CommerceToolsTest {
    @Test void unavailableApiAndMissingIdentityFailClosed() throws Exception {
        var calls = new AtomicInteger();
        var seenUser = new java.util.concurrent.atomic.AtomicReference<String>();
        var seenToken = new java.util.concurrent.atomic.AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/commerce/orders/RF1001", exchange -> {
            calls.incrementAndGet();
            seenUser.set(exchange.getRequestHeaders().getFirst("X-User-Id"));
            seenToken.set(exchange.getRequestHeaders().getFirst("X-Service-Token"));
            exchange.sendResponseHeaders(500, -1); exchange.close();
        });
        server.start();
        try {
            var p = new AppProperties(new AppProperties.Auth("alice", "bob", "internal-service-secret"),
                    new AppProperties.Commerce("http://127.0.0.1:"+server.getAddress().getPort()), null);
            var tools = new CommerceTools(new CommerceApiClient(p));
            var ctx = RuntimeContext.builder().userId("alice").sessionId("tools").build();
            assertThat(tools.queryOrder("RF1001", ctx)).contains("COMMERCE_UNAVAILABLE").doesNotContain("PAID");
            assertThat(tools.queryOrder("RF1001", RuntimeContext.empty())).contains("COMMERCE_UNAVAILABLE");
            assertThat(tools.queryOrder("RF1001?userId=bob", ctx)).contains("INVALID_ORDER_NO");
            assertThat(calls.get()).isEqualTo(1);
            assertThat(seenUser.get()).isEqualTo("alice");
            assertThat(seenToken.get()).isEqualTo("internal-service-secret");
        } finally { server.stop(0); }
    }
}
