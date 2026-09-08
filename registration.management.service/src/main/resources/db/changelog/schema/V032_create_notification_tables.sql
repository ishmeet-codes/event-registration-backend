--liquibase formatted sql

--changeset agent:V032_create_notification_tables

-- Create Notification Templates Table
CREATE TABLE IF NOT EXISTS notification_templates (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(150) NOT NULL,
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    subject VARCHAR(255),
    body TEXT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    reference_type VARCHAR(50),
    reference_id BIGINT,
    idempotency_key VARCHAR(255),
    retry_count INT NOT NULL DEFAULT 0,
    max_retries INT NOT NULL DEFAULT 3,
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    read_at TIMESTAMP,
    failure_reason TEXT,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create Notification Preferences Table
CREATE TABLE IF NOT EXISTS notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    event_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    registration_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    checkin_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create Notification Delivery Logs Table
CREATE TABLE IF NOT EXISTS notification_logs (
    id BIGSERIAL PRIMARY KEY,
    notification_id BIGINT,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    recipient_email VARCHAR(150),
    recipient_phone VARCHAR(50),
    attempts INT NOT NULL DEFAULT 1,
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_logs_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE SET NULL
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_status ON notifications(recipient_user_id, status);
CREATE INDEX IF NOT EXISTS idx_notifications_recipient_created ON notifications(recipient_user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_notifications_scheduled ON notifications(status, scheduled_at) WHERE status = 'PENDING';
CREATE INDEX IF NOT EXISTS idx_notifications_idempotency ON notifications(idempotency_key) WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_notification_templates_code ON notification_templates(code);
CREATE INDEX IF NOT EXISTS idx_notification_logs_notification ON notification_logs(notification_id);
