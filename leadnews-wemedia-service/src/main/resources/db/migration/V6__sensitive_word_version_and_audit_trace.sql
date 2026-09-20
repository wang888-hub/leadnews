CREATE TABLE wm_sensitive_word (
 id BIGINT NOT NULL AUTO_INCREMENT, word VARCHAR(128) NOT NULL, enabled BIT NOT NULL DEFAULT 1,
 created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
 PRIMARY KEY(id), UNIQUE KEY uk_sensitive_word(word), KEY idx_sensitive_word_enabled(enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE wm_sensitive_word_version (
 id TINYINT NOT NULL, version BIGINT NOT NULL, updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), PRIMARY KEY(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO wm_sensitive_word_version(id,version) VALUES(1,1);
INSERT INTO wm_sensitive_word(word,enabled) VALUES ('赌博',1),('毒品',1),('暴恐',1);
ALTER TABLE wm_news_audit_record ADD COLUMN sensitive_word_version BIGINT NULL AFTER attempt_no;
