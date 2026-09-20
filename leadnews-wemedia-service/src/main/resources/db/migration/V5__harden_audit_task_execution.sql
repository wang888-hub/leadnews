ALTER TABLE wm_news_audit_task
  CHANGE COLUMN retry_count dispatch_retry_count INT NOT NULL DEFAULT 0,
  CHANGE COLUMN next_retry_time next_dispatch_time DATETIME(3) NULL,
  CHANGE COLUMN started_time started_at DATETIME(3) NULL,
  ADD COLUMN attempt_no INT NOT NULL DEFAULT 0 AFTER status,
  ADD COLUMN dispatch_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' AFTER attempt_no,
  ADD COLUMN last_dispatched_at DATETIME(3) NULL AFTER next_dispatch_time,
  ADD COLUMN finished_at DATETIME(3) NULL AFTER started_at;

UPDATE wm_news_audit_task
SET dispatch_status = CASE WHEN status IN ('SENT','RUNNING','SUCCESS','STALE') THEN 'SENT' ELSE 'PENDING' END,
    status = CASE WHEN status = 'SENT' THEN 'PENDING' ELSE status END;

ALTER TABLE wm_news_audit_task
  DROP INDEX idx_audit_task_dispatch,
  DROP INDEX idx_audit_task_recovery,
  ADD KEY idx_audit_task_dispatch(status,dispatch_status,next_dispatch_time),
  ADD KEY idx_audit_task_recovery(status,started_at);

ALTER TABLE wm_news_audit_record
  ADD COLUMN attempt_no INT NULL AFTER audit_version;
