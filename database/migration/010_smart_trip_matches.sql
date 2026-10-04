-- Smart Trip Match persistence.
-- The application also uses spring.jpa.hibernate.ddl-auto=update, so this
-- migration documents the production schema for deployments that run SQL
-- migrations separately.

CREATE TABLE IF NOT EXISTS smart_trip_matches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    plan_a_id BIGINT NOT NULL,
    plan_b_id BIGINT NOT NULL,
    score INT NOT NULL,
    dismissed_by_a BOOLEAN NOT NULL DEFAULT FALSE,
    dismissed_by_b BOOLEAN NOT NULL DEFAULT FALSE,
    viewed_by_a BOOLEAN NOT NULL DEFAULT FALSE,
    viewed_by_b BOOLEAN NOT NULL DEFAULT FALSE,
    notified_at DATETIME NULL,
    created_at DATETIME NULL,
    updated_at DATETIME NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_smart_trip_match_plans UNIQUE (plan_a_id, plan_b_id),
    CONSTRAINT fk_smart_match_plan_a FOREIGN KEY (plan_a_id) REFERENCES travel_plan(id),
    CONSTRAINT fk_smart_match_plan_b FOREIGN KEY (plan_b_id) REFERENCES travel_plan(id),
    INDEX idx_smart_match_plan_a (plan_a_id),
    INDEX idx_smart_match_plan_b (plan_b_id),
    INDEX idx_smart_match_created (created_at)
);
