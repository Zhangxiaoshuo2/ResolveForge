package dev.resolveforge.api;

import dev.resolveforge.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class IdentityService {
    private final AppProperties.Auth auth;
    public IdentityService(AppProperties p) { auth = p.auth(); }
    public String authenticate(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7);
            if (same(token, auth.aliceToken())) return "alice";
            if (same(token, auth.bobToken())) return "bob";
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "需要有效的用户 Token");
    }
    public String commerceIdentity(String serviceToken, String userId) {
        if (!same(serviceToken, auth.serviceToken()) || !("alice".equals(userId) || "bob".equals(userId))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "业务服务认证失败");
        }
        return userId;
    }
    public static boolean same(String actual, String expected) {
        return actual != null && expected != null && !expected.isBlank()
                && MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8));
    }
}
