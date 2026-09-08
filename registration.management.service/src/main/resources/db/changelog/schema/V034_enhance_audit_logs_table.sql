--liquibase formatted sql

--changeset agent:V034_enhance_audit_logs_table

------------------------------------------------------------
-- Enhance audit_logs table for comprehensive audit management
------------------------------------------------------------
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS module VARCHAR(50);
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS description TEXT;
ALTER TABLE audit_logs ADD COLUMN IF NOT EXISTS request_id VARCHAR(100);

-- Make user_id nullable for unauthenticated events (e.g., failed logins, access denied)
ALTER TABLE audit_logs ALTER COLUMN user_id DROP NOT NULL;

-- Convert action and status to VARCHAR for flexible audit action support
ALTER TABLE audit_logs ALTER COLUMN action TYPE VARCHAR(100) USING action::text;
ALTER TABLE audit_logs ALTER COLUMN status TYPE VARCHAR(50) USING status::text;

-- Additional composite performance indexes
CREATE INDEX IF NOT EXISTS idx_audit_logs_user_created ON audit_logs(user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_module_created ON audit_logs(module, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity_lookup ON audit_logs(entity_name, entity_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_security ON audit_logs(action, created_at);
