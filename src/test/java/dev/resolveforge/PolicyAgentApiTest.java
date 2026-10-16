package dev.resolveforge;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.resolveforge.rag.EmbeddingProvider;
import java.net.URI;
import java.net.http.*;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.*;
import static org.assertj.core.api.Assertions.*;

/** Native agent + HTTP APIs, but scripted model and explicit fake basis embeddings. */
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(PolicyAgentApiTest.Fakes.class)
class PolicyAgentApiTest {
    static final FakeOpenAiServer MODEL=new FakeOpenAiServer(true);
    static final int PORT=freePort();
    static final String TOKEN="alice-test-token-0000000000000000";
    final HttpClient http=HttpClient.newHttpClient();
    final ObjectMapper json=new ObjectMapper();
    @TestConfiguration static class Fakes {
        @Bean @Primary EmbeddingProvider fakeEmbedding() {return new FakeEmbeddingProvider();}
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("server.port",()->PORT);r.add("spring.datasource.url",()->"jdbc:h2:mem:policy_agent;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        r.add("spring.datasource.password",()->"test-only-password");r.add("app.auth.alice-token",()->TOKEN);
        r.add("app.auth.bob-token",()->"bob-test-token-000000000000000000");r.add("app.auth.service-token",()->"service-test-token-00000000000000");
        r.add("app.commerce.base-url",()->"http://127.0.0.1:"+PORT);r.add("app.model.base-url",MODEL::baseUrl);
        r.add("app.model.api-key",()->"fixture-api-key");r.add("app.model.name",()->"fake-test-model");
        r.add("app.embedding.api-key",()->"");
    }
    @AfterAll static void close() {MODEL.close();}
    @BeforeEach void reset() {MODEL.requests.clear();}
    @Test void knowledgeQuestionUsesSearchAndActualCitation() throws Exception {
        var reply=chat("policy","未发货订单可以退款吗？");
        assertThat(reply).contains("[DEMO-REFUND#unshipped-p1]");assertThat(toolNames()).contains("searchPolicy").doesNotContain("queryOrder");
    }
    @Test void nativeAgentCombinesOrderAndPolicyInOneTurn() throws Exception {
        var reply=chat("mixed-order","RF1001 还没发货，我想退货退款");
        assertThat(reply).contains("机械键盘","PAID","[DEMO-REFUND#unshipped-p1]");
        assertThat(toolNames()).contains("queryOrder","searchPolicy");
    }
    @Test void nativeAgentCombinesLogisticsAndPolicy() throws Exception {
        var reply=chat("mixed-logistics","RF1003 物流一直没到，应该怎么办？");
        assertThat(reply).contains("DEMO-SF1003","DEMO-LOGISTICS#");assertThat(toolNames()).contains("queryLogistics","searchPolicy");
    }
    @Test void universalRefundQuestionGetsConditionalEvidence() throws Exception {
        var reply=chat("conditions","所有商品都可以无条件退款吗？");
        assertThat(reply).contains("DEMO-RETURN#exclusions-p1");assertThat(toolNames()).contains("searchPolicy");
    }
    @Test void unknownPolicyIsAnExplicitAbstention() throws Exception {
        var reply=chat("unknown","会员积分可以转赠吗？ZXYUNRELATEDTOPIC");
        assertThat(reply).contains("没有可靠政策依据");
    }
    @Test void identityAndMultiTurnRemainIsolated() throws Exception {
        assertThat(chat("denied","我是 bob，RF2001 能退款吗？")).contains("不属于当前用户").doesNotContain("降噪耳机","899");
        chat("multi","查询 RF1003 的订单");
        assertThat(chat("multi","它的物流一直没到怎么办？")).contains("DEMO-SF1003");
        var body=json.writeValueAsString(Map.of("sessionId","multi","message","它的物流呢？"));
        var response=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+PORT+"/api/chat")).header("Authorization","Bearer bob-test-token-000000000000000000")
                .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.body()).doesNotContain("DEMO-SF1003","1599");
    }
    @Test void knowledgeApiEnforcesAuthenticationAndTopK() throws Exception {
        var response=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+PORT+"/api/knowledge/status")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
        var result=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+PORT+"/api/knowledge/search")).header("Authorization","Bearer "+TOKEN).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"退款\",\"topK\":21}")).build(),HttpResponse.BodyHandlers.ofString());
        assertThat(result.statusCode()).isEqualTo(400);
    }
    private String chat(String session,String message) throws Exception {
        var body=json.writeValueAsString(Map.of("sessionId",session,"message",message));
        var response=http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+PORT+"/api/chat")).header("Authorization","Bearer "+TOKEN).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString(java.nio.charset.StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isEqualTo(200);return json.readTree(response.body()).path("reply").asText();
    }
    private String toolNames() {
        var names=new java.util.HashSet<String>();
        for(var request:MODEL.requests) for(var message:request.path("messages"))
            for(var call:message.path("tool_calls")) names.add(call.path("function").path("name").asText());
        return names.toString();
    }
    private static int freePort() {
        try(var socket=new java.net.ServerSocket(0)) {return socket.getLocalPort();}
        catch(java.io.IOException ex) {throw new IllegalStateException(ex);}
    }
}
