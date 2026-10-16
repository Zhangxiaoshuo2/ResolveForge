package dev.resolveforge.agent;

import dev.resolveforge.api.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatExecutionService {
    public record Result(String taskId, String userId, String sessionId, String reply) {}
    private static final Logger log = LoggerFactory.getLogger(ChatExecutionService.class);
    private final AgentService agent;
    private final TaskRepository tasks;
    public ChatExecutionService(AgentService agent, TaskRepository tasks) { this.agent = agent; this.tasks = tasks; }
    public Result chat(String user, String session, String message) {
        String task = tasks.start(user, session);
        log.info("task_started task={} user={} session={}", task, user, session);
        try {
            String reply = agent.chat(user, "debug:"+session, message);
            tasks.finish(task, "SUCCEEDED", reply, null);
            log.info("task_finished task={} status=SUCCEEDED", task);
            return new Result(task, user, session, reply);
        } catch (RuntimeException ex) {
            String code = ex instanceof ApiException api ? api.code() : "INTERNAL_ERROR";
            tasks.finish(task, "FAILED", null, code);
            log.warn("task_finished task={} status=FAILED code={}", task, code);
            throw ex;
        }
    }
}
