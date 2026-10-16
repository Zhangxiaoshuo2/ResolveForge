package dev.resolveforge.api;

import dev.resolveforge.agent.ChatExecutionService;
import dev.resolveforge.agent.TaskRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChatController {
    public record ChatRequest(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,64}") String sessionId,
                              @NotBlank @Size(max=4000) String message) {}
    private final ChatExecutionService execution;
    private final TaskRepository tasks;
    private final IdentityService identity;
    public ChatController(ChatExecutionService execution, IdentityService identity, TaskRepository tasks) {
        this.execution = execution; this.identity = identity; this.tasks = tasks;
    }
    @PostMapping("/api/chat")
    public ChatExecutionService.Result chat(@RequestHeader(value="Authorization", required=false) String authorization, @Valid @RequestBody ChatRequest request) {
        String userId = identity.authenticate(authorization);
        return execution.chat(userId, request.sessionId(), request.message());
    }
    @GetMapping("/api/tasks/{taskId}")
    public TaskRepository.Task task(@RequestHeader(value="Authorization", required=false) String authorization, @PathVariable String taskId) {
        return tasks.get(identity.authenticate(authorization), taskId);
    }
    @GetMapping("/api/tasks")
    public java.util.List<TaskRepository.Task> tasks(@RequestHeader(value="Authorization", required=false) String authorization, @RequestParam String sessionId) {
        return tasks.list(identity.authenticate(authorization), sessionId);
    }
    @GetMapping("/health")
    public java.util.Map<String,String> health() { return java.util.Map.of("status", "UP"); }
}
