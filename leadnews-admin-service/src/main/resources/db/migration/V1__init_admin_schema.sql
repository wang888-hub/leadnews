CREATE TABLE IF NOT EXISTS ad_user (
  id BIGINT NOT NULL AUTO_INCREMENT, name VARCHAR(64) NOT NULL, password_hash VARCHAR(100) NOT NULL, nickname VARCHAR(64) NOT NULL,
  image VARCHAR(500) NULL, phone VARCHAR(20) NULL, status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', email VARCHAR(128) NULL,
  login_time DATETIME(3) NULL, created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0, deleted TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY(id), UNIQUE KEY uk_ad_user_name(name), UNIQUE KEY uk_ad_user_phone(phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS ad_audit_record (
  id BIGINT NOT NULL AUTO_INCREMENT, wm_news_id BIGINT NOT NULL, reviewer_id BIGINT NOT NULL,
  decision VARCHAR(20) NOT NULL, reason VARCHAR(500) NULL, reviewed_time DATETIME(3) NOT NULL,
  created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY(id), UNIQUE KEY uk_audit_news(wm_news_id), KEY idx_audit_reviewer_time(reviewer_id,reviewed_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO ad_user(name,password_hash,nickname,phone) VALUES
('admin_demo','$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy','演示管理员','13700000000')
ON DUPLICATE KEY UPDATE nickname=VALUES(nickname);
