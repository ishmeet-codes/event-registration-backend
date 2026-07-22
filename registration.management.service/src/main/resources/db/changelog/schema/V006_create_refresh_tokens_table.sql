--liquibase formatted sql

--changeset ishmeet:V006_create_refresh_tokens_table

------------------------------------------------------------
-- Refresh Tokens
------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id BIGINT,
    token TEXT UNIQUE,
    expires_at TIMESTAMP,
    revoked BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);