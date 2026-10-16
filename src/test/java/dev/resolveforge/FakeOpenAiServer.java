package dev.resolveforge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/** Scripted protocol fixture, NOT an LLM or evidence of autonomous reasoning. Test source only. */
final class FakeOpenAiServer implements AutoCloseable {
    private final HttpServer server;
    private final ObjectMapper json = new ObjectMapper();
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor();
    final List<JsonNode> requests = new CopyOnWriteArrayList<>();
    private static final Pattern ORDER = Pattern.compile("RF[0-9]{4}");
    FakeOpenAiServer() {
        this(false);
    }
    FakeOpenAiServer(boolean policyScenario) {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.setExecutor(executor);
            server.createContext("/chat/completions", exchange -> {
                try {
                    JsonNode request = json.readTree(exchange.getRequestBody());
                    requests.add(request);
                    var messages = request.path("messages");
                    String lastUser = "";
                    String order = null;
                    for (var msg : messages) {
                        if ("user".equals(msg.path("role").asText())) {
                            lastUser = msg.path("content").asText();
                            var matcher = ORDER.matcher(lastUser);
                            while (matcher.find()) order = matcher.group();
                        }
                    }
                    if (lastUser.contains("模拟模型故障")) {
                        byte[] error = "{\"error\":{\"message\":\"fixture rejected\",\"type\":\"invalid_request_error\"}}".getBytes(StandardCharsets.UTF_8);
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(401, error.length);
                        exchange.getResponseBody().write(error);
                        return;
                    }
                    if (lastUser.contains("模拟模型超时")) {
                        try { Thread.sleep(6000); } catch (InterruptedException ex) { Thread.currentThread().interrupt(); return; }
                    }
                    var last = messages.get(messages.size()-1);
                    Map<String,Object> message;
                    String finish;
                    if (request.path("tools").isEmpty()) {
                        message = Map.of("role", "assistant", "content", "[FAKE MODEL] 原生循环上限总结");
                        finish = "stop";
                    } else if (lastUser.contains("持续调用工具")) {
                        message = Map.of("role", "assistant", "content", "", "tool_calls", List.of(Map.of("id", "loop_"+requests.size(),
                                "type", "function", "function", Map.of("name", "queryOrder", "arguments", "{\"orderNo\":\"RF1001\"}"))));
                        finish = "tool_calls";
                    } else if (policyScenario) {
                        message=policyMessage(messages,lastUser,order);
                        finish=message.containsKey("tool_calls")?"tool_calls":"stop";
                    } else if ("tool".equals(last.path("role").asText())) {
                        message = Map.of("role", "assistant", "content", "[FAKE MODEL] 查询结果："+last.path("content").asText());
                        finish = "stop";
                    } else if (order != null) {
                        String tool = lastUser.contains("物流") || lastUser.contains("到哪") ? "queryLogistics" : "queryOrder";
                        message = Map.of("role", "assistant", "content", "", "tool_calls", List.of(Map.of("id", "call_fixture_"+requests.size(),
                                "type", "function", "function", Map.of("name", tool, "arguments", json.writeValueAsString(Map.of("orderNo", order))))));
                        finish = "tool_calls";
                    } else {
                        message = Map.of("role", "assistant", "content", "[FAKE MODEL] 请提供订单号");
                        finish = "stop";
                    }
                    byte[] body = json.writeValueAsBytes(Map.of("id", "chatcmpl-fixture", "object", "chat.completion", "created", 1,
                            "model", "fake-test-model", "choices", List.of(Map.of("index", 0, "message", message, "finish_reason", finish)),
                            "usage", Map.of("prompt_tokens", 20, "completion_tokens", 10, "total_tokens", 30)));
                    exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                } finally { exchange.close(); }
            });
            server.start();
        } catch (IOException ex) { throw new IllegalStateException(ex); }
    }
    String baseUrl() { return "http://127.0.0.1:"+server.getAddress().getPort(); }
    private Map<String,Object> policyMessage(JsonNode messages,String user,String order) throws IOException {
        var used=new java.util.HashSet<String>();var outputs=new StringBuilder();
        for(var msg:messages) {
            if("user".equals(msg.path("role").asText())) {used.clear();outputs.setLength(0);}
            for(var tool:msg.path("tool_calls")) used.add(tool.path("function").path("name").asText());
            if("tool".equals(msg.path("role").asText())) outputs.append(msg.path("content").asText()).append('\n');
        }
        var calls=new java.util.ArrayList<Map<String,Object>>();
        if(order!=null) {
            String tool=user.contains("物流")?"queryLogistics":"queryOrder";
            if(!used.contains(tool)) calls.add(Map.of("id","fixture_business_"+java.util.UUID.randomUUID(),"type","function","function",Map.of("name",tool,"arguments",json.writeValueAsString(Map.of("orderNo",order)))));
        }
        boolean policy=user.contains("退款")||user.contains("退货")||user.contains("政策")||user.contains("怎么办")||user.contains("会员积分");
        if(policy&&!used.contains("searchPolicy")) calls.add(Map.of("id","fixture_policy_"+java.util.UUID.randomUUID(),"type","function","function",Map.of("name","searchPolicy","arguments",json.writeValueAsString(Map.of("query",user)))));
        if(!calls.isEmpty()) return Map.of("role","assistant","content","","tool_calls",calls);
        String answer;
        if(outputs.toString().contains("ORDER_NOT_FOUND")) answer="[FAKE MODEL] 订单不存在或不属于当前用户";
        else if(outputs.toString().contains("NO_RELEVANT_EVIDENCE")) answer="[FAKE MODEL] 没有可靠政策依据";
        else {
            var ids=Pattern.compile("\"chunkId\"\\s*:\\s*\"([^\"]+)\"").matcher(outputs);
            var citations=new StringBuilder();while(ids.find()) citations.append('[').append(ids.group(1)).append(']');
            answer="[FAKE MODEL] "+outputs+citations;
        }
        return Map.of("role","assistant","content",answer);
    }
    @Override public void close() { server.stop(0); executor.shutdownNow(); }
}
