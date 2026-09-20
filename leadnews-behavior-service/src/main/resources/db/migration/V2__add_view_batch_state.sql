ALTER TABLE behavior_event_log ADD COLUMN stat_applied TINYINT NOT NULL DEFAULT 1 AFTER processed_time, ADD KEY idx_view_pending (behavior_type, stat_applied, id);
