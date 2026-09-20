ALTER TABLE ap_article
  ADD COLUMN static_object_key VARCHAR(500) NULL AFTER static_url,
  ADD COLUMN publish_status VARCHAR(30) NOT NULL DEFAULT 'APPROVED' AFTER status,
  ADD COLUMN publish_retry_count INT NOT NULL DEFAULT 0 AFTER publish_status,
  ADD COLUMN published_time DATETIME(3) NULL AFTER publish_retry_count,
  ADD COLUMN publish_started_time DATETIME(3) NULL AFTER published_time,
  ADD COLUMN last_error VARCHAR(500) NULL AFTER publish_started_time,
  ADD KEY idx_article_publish_recovery(publish_status, publish_started_time);

UPDATE ap_article
SET publish_status = 'PUBLISHED', published_time = COALESCE(publish_time, updated_time)
WHERE status = 'PUBLISHED';
