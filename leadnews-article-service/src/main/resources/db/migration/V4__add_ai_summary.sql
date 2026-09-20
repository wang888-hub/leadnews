ALTER TABLE ap_article ADD COLUMN summary_status VARCHAR(16) NOT NULL DEFAULT 'NONE' AFTER summary,
 ADD COLUMN summary_version BIGINT NOT NULL DEFAULT 0 AFTER summary_status,
 ADD COLUMN summary_model VARCHAR(64) NULL AFTER summary_version,
 ADD COLUMN summary_generated_time DATETIME(3) NULL AFTER summary_model;
CREATE TABLE article_ai_summary_task (
 id BIGINT NOT NULL AUTO_INCREMENT, article_id BIGINT NOT NULL, summary_version BIGINT NOT NULL,
 event_id VARCHAR(36) NOT NULL, status VARCHAR(16) NOT NULL DEFAULT 'PENDING', retry_count INT NOT NULL DEFAULT 0,
 next_retry_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), started_time DATETIME(3) NULL,
 last_error VARCHAR(500) NULL, trace_id VARCHAR(64) NULL,
 created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id), UNIQUE KEY uk_summary_article_version(article_id,summary_version), UNIQUE KEY uk_summary_event(event_id),
 KEY idx_summary_dispatch(status,next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
