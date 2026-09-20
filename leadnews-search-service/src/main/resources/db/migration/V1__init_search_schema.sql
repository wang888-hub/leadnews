CREATE TABLE IF NOT EXISTS search_history (
  id BIGINT NOT NULL AUTO_INCREMENT, user_id BIGINT NOT NULL, keyword VARCHAR(100) NOT NULL,
  created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id), UNIQUE KEY uk_search_history_user_keyword (user_id,keyword),
  KEY idx_search_history_user_time (user_id,updated_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS search_sync_failure (
  id BIGINT NOT NULL AUTO_INCREMENT, event_id VARCHAR(36) NOT NULL, article_id BIGINT NOT NULL,
  event_type VARCHAR(16) NOT NULL, error_message VARCHAR(500) NULL,
  created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id), UNIQUE KEY uk_search_sync_failure_event (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
