--liquibase formatted sql

--changeset ishmeet:V030_add_email_to_participants_table.sql

------------------------------------------------------------
-- Add email to participants table
------------------------------------------------------------
ALTER TABLE participants ADD COLUMN email VARCHAR(120);
