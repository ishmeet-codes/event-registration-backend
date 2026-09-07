--liquibase formatted sql

--changeset assistant:V031_make_entity_id_nullable_in_audit_logs

------------------------------------------------------------
-- Make entity_id NULLABLE in audit_logs table
------------------------------------------------------------
ALTER TABLE audit_logs ALTER COLUMN entity_id DROP NOT NULL;
