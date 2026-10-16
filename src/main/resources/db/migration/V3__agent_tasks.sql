CREATE TABLE agent_tasks (
  task_id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(64) NOT NULL,
  session_id VARCHAR(80) NOT NULL,
  status VARCHAR(20) NOT NULL CHECK (status IN ('RUNNING','SUCCEEDED','FAILED')),
  reply TEXT,
  error_code VARCHAR(80),
  started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  finished_at TIMESTAMP
);
CREATE INDEX idx_task_session ON agent_tasks(user_id, session_id, started_at);
