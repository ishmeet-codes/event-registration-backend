--liquibase formatted sql

--changeset ishmeet:V014_create_audit_logs_table

------------------------------------------------------------
-- Audit Logs
------------------------------------------------------------
CREATE TABLE audit_logs (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT NOT NULL,
    entity_name VARCHAR(50),
    entity_id BIGINT NOT NULL,
    action audit_action NOT NULL,
    old_value JSONB,
    new_value JSONB,
    status audit_status NOT NULL,
    error_message TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id)
);