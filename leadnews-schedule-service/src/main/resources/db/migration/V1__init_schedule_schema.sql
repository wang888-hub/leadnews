CREATE TABLE IF NOT EXISTS schedule_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_type VARCHAR(40) NOT NULL,
  business_id BIGINT NOT NULL,
  execute_time DATETIME(3) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
  retry_count INT NOT NULL DEFAULT 0,
  payload JSON NULL,
  last_error VARCHAR(500) NULL,
  started_time DATETIME(3) NULL,
  created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY(id),
  UNIQUE KEY uk_schedule_type_business(task_type,business_id),
  KEY idx_schedule_due(status,execute_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
