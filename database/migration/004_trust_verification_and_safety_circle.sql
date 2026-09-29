-- TravelMatch Trust Center V1. Apply once to the existing MySQL database.
-- Verification state is stored separately from User. A provider must confirm identity/selfie status;
-- never set these status columns directly from a client request.
CREATE TABLE IF NOT EXISTS trust_verification (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    phone_number VARCHAR(24) NULL,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified_at DATETIME NULL,
    phone_otp_hash VARCHAR(100) NULL,
    phone_otp_expiry DATETIME NULL,
    phone_otp_last_sent_at DATETIME NULL,
    phone_otp_attempts INT NOT NULL DEFAULT 0,
    identity_status VARCHAR(24) NOT NULL DEFAULT 'NOT_STARTED',
    selfie_status VARCHAR(24) NOT NULL DEFAULT 'NOT_STARTED',
    emergency_sharing_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    profile_discoverable BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uk_trust_verification_user UNIQUE (user_id),
    CONSTRAINT fk_trust_verification_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS safety_circle_contact (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    name VARCHAR(80) NOT NULL,
    phone VARCHAR(24) NULL,
    email VARCHAR(180) NULL,
    relationship VARCHAR(40) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_safety_contact_owner (owner_id, created_at),
    CONSTRAINT fk_safety_contact_owner FOREIGN KEY (owner_id) REFERENCES `user`(id) ON DELETE CASCADE
);
