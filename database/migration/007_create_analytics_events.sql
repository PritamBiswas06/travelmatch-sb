-- TravelMatch analytics events. Raw events are purged after 365 days by the application scheduler.
-- Does not store passwords, JWTs, IP addresses, private message bodies, or URL query strings.
CREATE TABLE IF NOT EXISTS analytics_events (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  event_type VARCHAR(40) NOT NULL,
  page_path VARCHAR(180) NULL,
  event_label VARCHAR(80) NULL,
  session_id VARCHAR(64) NOT NULL,
  created_at DATETIME NOT NULL,
  INDEX idx_analytics_created_at (created_at),
  INDEX idx_analytics_user_created (user_id, created_at),
  INDEX idx_analytics_session (session_id),
  INDEX idx_analytics_event_type (event_type, created_at),
  CONSTRAINT fk_analytics_event_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);
