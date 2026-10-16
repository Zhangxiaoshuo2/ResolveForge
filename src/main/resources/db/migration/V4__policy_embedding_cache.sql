CREATE TABLE policy_embedding_cache (
  cache_key VARCHAR(64) PRIMARY KEY,
  provider_identity VARCHAR(500) NOT NULL,
  model_name VARCHAR(150) NOT NULL,
  dimensions INTEGER NOT NULL,
  chunk_id VARCHAR(200) NOT NULL,
  document_hash VARCHAR(64) NOT NULL,
  content_hash VARCHAR(64) NOT NULL,
  vector_json TEXT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
