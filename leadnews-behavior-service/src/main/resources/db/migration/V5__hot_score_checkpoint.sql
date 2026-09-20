CREATE TABLE hot_score_checkpoint (
 article_id BIGINT NOT NULL PRIMARY KEY,
 channel_id BIGINT NOT NULL,
 hot_score DOUBLE NOT NULL,
 snapshot_time DATETIME(3) NOT NULL,
 KEY idx_hot_checkpoint_channel (channel_id,hot_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
