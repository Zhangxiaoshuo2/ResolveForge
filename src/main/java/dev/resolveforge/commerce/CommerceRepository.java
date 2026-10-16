package dev.resolveforge.commerce;

import dev.resolveforge.api.ApiException;
import java.math.BigDecimal;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CommerceRepository {
    public record Order(String orderNo, String productName, BigDecimal amount, String status, String createdAt) {}
    public record Logistics(String orderNo, String carrier, String trackingNo, String status, String latestEvent, String updatedAt) {}
    private final JdbcTemplate jdbc;
    public CommerceRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public Order order(String userId, String orderNo) {
        valid(orderNo);
        var rows = jdbc.query("SELECT * FROM commerce_orders WHERE order_no=? AND user_id=?",
                (rs, n) -> new Order(rs.getString("order_no"), rs.getString("product_name"), rs.getBigDecimal("amount"),
                        rs.getString("status"), rs.getTimestamp("created_at").toLocalDateTime().toString()), orderNo, userId);
        if (rows.isEmpty()) throw notFound();
        return rows.getFirst();
    }
    public Logistics logistics(String userId, String orderNo) {
        valid(orderNo);
        var rows = jdbc.query("SELECT l.* FROM commerce_logistics l JOIN commerce_orders o ON o.order_no=l.order_no WHERE o.order_no=? AND o.user_id=?",
                (rs, n) -> new Logistics(rs.getString("order_no"), rs.getString("carrier"), rs.getString("tracking_no"),
                        rs.getString("status"), rs.getString("latest_event"), rs.getTimestamp("updated_at").toLocalDateTime().toString()), orderNo, userId);
        if (rows.isEmpty()) throw notFound();
        return rows.getFirst();
    }
    public static void valid(String orderNo) {
        if (orderNo == null || !orderNo.matches("[A-Za-z0-9-]{1,40}"))
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ORDER_NO", "订单号格式无效");
    }
    private ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "订单不存在或不属于当前用户");
    }
}
