-- TravelMatch SMS provider audit. Stores masked recipients and provider references only;
-- never store OTP values, SMS bodies, or provider credentials here.
CREATE TABLE IF NOT EXISTS sms_delivery_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider VARCHAR(30) NOT NULL,
    recipient_masked VARCHAR(24) NOT NULL,
    delivery_status VARCHAR(20) NOT NULL,
    provider_message_id VARCHAR(100) NULL,
    provider_error_code VARCHAR(40) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_sms_delivery_created_at (created_at),
    INDEX idx_sms_delivery_status (delivery_status)
);
