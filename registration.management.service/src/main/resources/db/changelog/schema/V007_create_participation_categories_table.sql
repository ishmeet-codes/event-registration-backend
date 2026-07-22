--liquibase formatted sql

--changeset ishmeet:V007_create_participation_categories_table

------------------------------------------------------------
-- Participation Categories
------------------------------------------------------------

CREATE TABLE participation_categories (
    code VARCHAR(50) PRIMARY KEY,
    display_name VARCHAR(50),
    min_participants SMALLINT,
    max_participants SMALLINT,
    active BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);