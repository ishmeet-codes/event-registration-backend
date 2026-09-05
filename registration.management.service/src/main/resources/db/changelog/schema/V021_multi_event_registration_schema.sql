--liquibase formatted sql

--changeset agent:V021_multi_event_registration_schema

------------------------------------------------------------
-- 1. Refactor registrations table (Remove single event_id)
------------------------------------------------------------
ALTER TABLE registrations DROP CONSTRAINT IF EXISTS uq_registration_school_event;
ALTER TABLE registrations DROP CONSTRAINT IF EXISTS fk_registrations_event_id;
ALTER TABLE registrations DROP COLUMN IF EXISTS event_id;

------------------------------------------------------------
-- 2. Create registration_events join table
------------------------------------------------------------
CREATE TABLE registration_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    registration_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_registration_event UNIQUE (registration_id, event_id),
    CONSTRAINT fk_registration_events_registration FOREIGN KEY (registration_id) REFERENCES registrations(id) ON DELETE CASCADE,
    CONSTRAINT fk_registration_events_event FOREIGN KEY (event_id) REFERENCES events(id)
);

------------------------------------------------------------
-- 3. Create registration_participant_events join table
------------------------------------------------------------
CREATE TABLE registration_participant_events (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    participant_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_participant_event UNIQUE (participant_id, event_id),
    CONSTRAINT fk_participant_events_participant FOREIGN KEY (participant_id) REFERENCES participants(id) ON DELETE CASCADE,
    CONSTRAINT fk_participant_events_event FOREIGN KEY (event_id) REFERENCES events(id)
);
