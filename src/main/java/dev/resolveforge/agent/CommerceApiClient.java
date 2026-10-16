package dev.resolveforge.agent;

import dev.resolveforge.config.AppProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CommerceApiClient {
    private final RestClient client;
    public CommerceApiClient(AppProperties p) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        client = RestClient.builder().baseUrl(p.commerce().baseUrl()).requestFactory(factory)
                .defaultHeader("X-Service-Token", p.auth().serviceToken()).build();
    }
    public String query(String userId, String orderNo, boolean logistics) {
        if (orderNo == null || !orderNo.matches("[A-Za-z0-9-]{1,40}")) return "{\"code\":\"INVALID_ORDER_NO\"}";
        try {
            return client.get().uri("/api/commerce/orders/{orderNo}" + (logistics ? "/logistics" : ""), orderNo)
                    .header("X-User-Id", userId).exchange((request, response) -> {
                        if (response.getStatusCode().value() == 404) return "{\"code\":\"ORDER_NOT_FOUND\",\"message\":\"订单不存在或不属于当前用户\"}";
                        if (!response.getStatusCode().is2xxSuccessful()) throw new IllegalStateException("commerce_http_" + response.getStatusCode().value());
                        return new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    });
        } catch (RestClientException ex) {
            throw new IllegalStateException("commerce_unavailable");
        }
    }
}
