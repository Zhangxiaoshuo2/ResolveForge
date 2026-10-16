package dev.resolveforge.commerce;

import dev.resolveforge.api.IdentityService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/commerce/orders")
public class CommerceController {
    private final CommerceRepository repository;
    private final IdentityService identity;
    public CommerceController(CommerceRepository repository, IdentityService identity) { this.repository = repository; this.identity = identity; }
    @GetMapping("/{orderNo}")
    public CommerceRepository.Order order(@PathVariable String orderNo,
            @RequestHeader(value="Authorization", required=false) String authorization,
            @RequestHeader(value="X-Service-Token", required=false) String serviceToken,
            @RequestHeader(value="X-User-Id", required=false) String userId) {
        return repository.order(user(authorization, serviceToken, userId), orderNo);
    }
    @GetMapping("/{orderNo}/logistics")
    public CommerceRepository.Logistics logistics(@PathVariable String orderNo,
            @RequestHeader(value="Authorization", required=false) String authorization,
            @RequestHeader(value="X-Service-Token", required=false) String serviceToken,
            @RequestHeader(value="X-User-Id", required=false) String userId) {
        return repository.logistics(user(authorization, serviceToken, userId), orderNo);
    }
    private String user(String authorization, String serviceToken, String userId) {
        return serviceToken != null ? identity.commerceIdentity(serviceToken, userId) : identity.authenticate(authorization);
    }
}
