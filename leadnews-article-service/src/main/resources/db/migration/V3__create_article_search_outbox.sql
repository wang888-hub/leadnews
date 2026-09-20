CREATE TABLE IF NOT EXISTS article_search_sync (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(36) NOT NULL,
  article_id BIGINT NOT NULL,
  event_type VARCHAR(16) NOT NULL,
  trace_id VARCHAR(64) NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  last_error VARCHAR(500) NULL,
  created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id), UNIQUE KEY uk_article_search_event (event_id),
  KEY idx_article_search_pending (status,next_retry_time), KEY idx_article_search_article (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
