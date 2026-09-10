-- ==============================================================================
-- Migration V036: Create Email Campaign Management Tables
-- ==============================================================================

-- 1. Create email_campaigns table
CREATE TABLE IF NOT EXISTS email_campaigns (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    template_id BIGINT,
    template_version INT DEFAULT 1,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    audience_type VARCHAR(50) NOT NULL DEFAULT 'SCHOOL_STAFF',
    audience_criteria_json TEXT,
    total_recipients INT DEFAULT 0,
    valid_recipients_count INT DEFAULT 0,
    sent_count INT DEFAULT 0,
    failed_count INT DEFAULT 0,
    delivered_count INT DEFAULT 0,
    bounced_count INT DEFAULT 0,
    scheduled_at TIMESTAMP,
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 2. Create email_campaign_recipients table
CREATE TABLE IF NOT EXISTS email_campaign_recipients (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255),
    school_name VARCHAR(255),
    recipient_type VARCHAR(50),
    person_id BIGINT,
    school_id BIGINT,
    school_staff_id BIGINT,
    participant_id BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'VALID',
    error_message TEXT,
    provider_message_id VARCHAR(255),
    sent_at TIMESTAMP,
    delivered_at TIMESTAMP,
    failed_at TIMESTAMP,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_campaign_recipients_campaign FOREIGN KEY (campaign_id)
        REFERENCES email_campaigns(id) ON DELETE CASCADE
);

-- 3. Create email_automations table
CREATE TABLE IF NOT EXISTS email_automations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    trigger_type VARCHAR(100) NOT NULL,
    template_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    target_roles_csv VARCHAR(255),
    conditions_json TEXT,
    created_by_user_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

-- 4. Create Performance Indexes
CREATE INDEX IF NOT EXISTS idx_email_campaigns_status_scheduled ON email_campaigns(status, scheduled_at);
CREATE INDEX IF NOT EXISTS idx_email_campaign_recipients_camp_status ON email_campaign_recipients(campaign_id, status);
CREATE INDEX IF NOT EXISTS idx_email_campaign_recipients_email ON email_campaign_recipients(email);
CREATE INDEX IF NOT EXISTS idx_email_automations_trigger ON email_automations(trigger_type, active);
