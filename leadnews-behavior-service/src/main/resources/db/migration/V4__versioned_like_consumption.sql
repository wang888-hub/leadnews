ALTER TABLE article_like ADD COLUMN relation_version BIGINT NOT NULL DEFAULT 0;
CREATE TABLE consumed_event (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 consumer_group VARCHAR(100) NOT NULL,
 event_id VARCHAR(64) NOT NULL,
 event_type VARCHAR(50) NOT NULL,
 article_id BIGINT NOT NULL,
 consumed_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 UNIQUE KEY uk_consumer_event (consumer_group,event_id),
 KEY idx_consumed_time (consumed_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
