package dev.resolveforge.agent;

import dev.resolveforge.api.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository {
    public record Task(String taskId, String sessionId, String status, String reply, String errorCode,
                       String startedAt, String finishedAt) {}
    private final JdbcTemplate jdbc;
    private final RowMapper<Task> mapper = (rs, n) -> new Task(rs.getString("task_id"), rs.getString("session_id"),
            rs.getString("status"), rs.getString("reply"), rs.getString("error_code"),
            rs.getTimestamp("started_at").toLocalDateTime().toString(),
            rs.getTimestamp("finished_at") == null ? null : rs.getTimestamp("finished_at").toLocalDateTime().toString());
    public TaskRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public String start(String user, String session) {
        String id = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO agent_tasks(task_id,user_id,session_id,status) VALUES (?,?,?,'RUNNING')", id, user, session);
        return id;
    }
    public void finish(String id, String status, String reply, String code) {
        jdbc.update("UPDATE agent_tasks SET status=?,reply=?,error_code=?,finished_at=CURRENT_TIMESTAMP WHERE task_id=? AND status='RUNNING'", status, reply, code, id);
    }
    public Task get(String user, String id) {
        var results = jdbc.query("SELECT * FROM agent_tasks WHERE user_id=? AND task_id=?", mapper, user, id);
        if (results.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "任务不存在或不属于当前用户");
        return results.getFirst();
    }
    public List<Task> list(String user, String session) {
        return jdbc.query("SELECT * FROM agent_tasks WHERE user_id=? AND session_id=? ORDER BY started_at DESC,task_id DESC LIMIT 20", mapper, user, session);
    }
}
