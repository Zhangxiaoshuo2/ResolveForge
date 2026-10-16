package dev.resolveforge.rag;

import dev.resolveforge.api.IdentityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {
    public record SearchRequest(@NotBlank @Size(max=1000) String query,@Min(1) @Max(20) Integer topK,PolicySearchService.Mode mode) {}
    private final IdentityService identity;
    private final PolicySearchService search;
    private final PolicyIndex index;
    public KnowledgeController(IdentityService identity,PolicySearchService search,PolicyIndex index) {this.identity=identity;this.search=search;this.index=index;}
    @PostMapping("/search")
    public PolicySearchService.Result search(@RequestHeader(value="Authorization",required=false) String authorization,@Valid @RequestBody SearchRequest request) {
        identity.authenticate(authorization);return search.search(request.query(),request.topK(),request.mode());
    }
    @GetMapping("/status")
    public PolicyIndex.Status status(@RequestHeader(value="Authorization",required=false) String authorization) {identity.authenticate(authorization);return index.status();}
}
