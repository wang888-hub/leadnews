CREATE TABLE article_hot_snapshot (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 snapshot_id VARCHAR(64) NOT NULL,
 article_id BIGINT NOT NULL,
 channel_id BIGINT NOT NULL,
 hot_score DOUBLE NOT NULL,
 like_count BIGINT NOT NULL,
 collect_count BIGINT NOT NULL,
 comment_count BIGINT NOT NULL,
 view_count BIGINT NOT NULL,
 snapshot_time DATETIME(3) NOT NULL,
 UNIQUE KEY uk_hot_snapshot_article(snapshot_id,article_id),
 KEY idx_hot_snapshot_time(snapshot_time),
 KEY idx_hot_snapshot_id(snapshot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE article_hot_snapshot_batch (
 snapshot_id VARCHAR(64) NOT NULL PRIMARY KEY,
 snapshot_time DATETIME(3) NOT NULL,
 status VARCHAR(16) NOT NULL,
 article_count INT NOT NULL DEFAULT 0,
 KEY idx_hot_snapshot_batch_time(status,snapshot_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE article_behavior_stat
 ADD COLUMN like_count_version BIGINT NOT NULL DEFAULT 0,
 ADD COLUMN collect_count_version BIGINT NOT NULL DEFAULT 0;
