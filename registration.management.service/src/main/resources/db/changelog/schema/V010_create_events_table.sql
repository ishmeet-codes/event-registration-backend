--liquibase formatted sql

--changeset ishmeet:V010_create_events_table

------------------------------------------------------------
-- Events
------------------------------------------------------------
CREATE TABLE events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    participation_category_code VARCHAR(50) NOT NULL,
    event_name VARCHAR(150) NOT NULL,
    description TEXT NOT NULL,
    venue VARCHAR(120)NOT NULL,
    registration_deadline TIMESTAMP NOT NULL,
    event_date DATE NOT NULL,
    start_time TIME,
    end_time TIME,
    max_registrations INTEGER NOT NULL,
    active BOOLEAN,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_events_participation_category FOREIGN KEY (participation_category_code) REFERENCES participation_categories(code),
    CONSTRAINT fk_events_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_events_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);