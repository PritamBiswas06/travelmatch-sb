-- TravelMatch Travel Squad V1
-- The application also runs with spring.jpa.hibernate.ddl-auto=update.
-- Run this migration once on an existing production database if schema
-- changes are managed manually.

CREATE TABLE IF NOT EXISTS travel_squads (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500) NULL,
    destination VARCHAR(120) NOT NULL,
    start_date DATE NULL,
    end_date DATE NULL,
    max_members INT NOT NULL DEFAULT 6,
    creator_id BIGINT NOT NULL,
    travel_plan_id BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_squad_creator (creator_id),
    KEY idx_squad_status_created (status, created_at),
    KEY idx_squad_destination (destination),
    CONSTRAINT fk_squad_creator FOREIGN KEY (creator_id) REFERENCES `user` (id),
    CONSTRAINT fk_squad_travel_plan FOREIGN KEY (travel_plan_id) REFERENCES travel_plan (id)
);

CREATE TABLE IF NOT EXISTS travel_squad_members (
    id BIGINT NOT NULL AUTO_INCREMENT,
    squad_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    joined_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_squad_member_user (squad_id, user_id),
    KEY idx_squad_member_squad_status (squad_id, status),
    KEY idx_squad_member_user_status (user_id, status),
    CONSTRAINT fk_squad_member_squad FOREIGN KEY (squad_id) REFERENCES travel_squads (id),
    CONSTRAINT fk_squad_member_user FOREIGN KEY (user_id) REFERENCES `user` (id)
);

CREATE TABLE IF NOT EXISTS travel_squad_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    squad_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content VARCHAR(1500) NOT NULL,
    message_time DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_squad_message_squad_time (squad_id, message_time),
    KEY idx_squad_message_sender (sender_id),
    CONSTRAINT fk_squad_message_squad FOREIGN KEY (squad_id) REFERENCES travel_squads (id),
    CONSTRAINT fk_squad_message_sender FOREIGN KEY (sender_id) REFERENCES `user` (id)
);
