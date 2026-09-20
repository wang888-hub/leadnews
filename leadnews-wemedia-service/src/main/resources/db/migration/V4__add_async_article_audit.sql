ALTER TABLE wm_news
  ADD COLUMN audit_version BIGINT NOT NULL DEFAULT 0 AFTER article_id,
  ADD COLUMN audit_source VARCHAR(32) NULL AFTER audit_version;

CREATE TABLE wm_news_audit_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  news_id BIGINT NOT NULL,
  audit_version BIGINT NOT NULL,
  event_id VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  last_error VARCHAR(500) NULL,
  next_retry_time DATETIME(3) NULL,
  started_time DATETIME(3) NULL,
  created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id),
  UNIQUE KEY uk_audit_task_news_version(news_id,audit_version),
  UNIQUE KEY uk_audit_task_event(event_id),
  KEY idx_audit_task_dispatch(status,next_retry_time),
  KEY idx_audit_task_recovery(status,started_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE wm_news_audit_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  news_id BIGINT NOT NULL,
  audit_version BIGINT NOT NULL,
  audit_stage VARCHAR(20) NOT NULL,
  decision VARCHAR(20) NOT NULL,
  risk_level VARCHAR(20) NULL,
  reason VARCHAR(500) NOT NULL,
  confidence DECIMAL(6,5) NULL,
  risk_tags JSON NULL,
  matched_words JSON NULL,
  model VARCHAR(100) NULL,
  provider_request_id VARCHAR(128) NULL,
  latency_ms BIGINT NULL,
  error_code VARCHAR(64) NULL,
  reviewer_id BIGINT NULL,
  reviewed_time DATETIME(3) NULL,
  created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id),
  UNIQUE KEY uk_audit_record_stage(news_id,audit_version,audit_stage),
  KEY idx_audit_record_news(news_id,audit_version,created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
