--liquibase formatted sql

--changeset ishmeet:V020_update_registration_status_enum

ALTER TYPE registration_status ADD VALUE IF NOT EXISTS 'PENDING';
ALTER TYPE registration_status ADD VALUE IF NOT EXISTS 'CANCELLED';
ALTER TYPE registration_status ADD VALUE IF NOT EXISTS 'COMPLETED';
