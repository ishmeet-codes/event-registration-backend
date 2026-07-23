--liquibase formatted sql

--changeset ishmeet:V019_seed_participation_categories

INSERT INTO participation_categories
(
    code,
    display_name,
    min_participants,
    max_participants,
    active
)
VALUES
    ('SOLO',     'Solo',        1,  1,  TRUE),
    ('DUO',      'Duo',         2,  2,  TRUE),
    ('GROUP_4',  'Group of 4',  4,  4,  TRUE),
    ('GROUP_6',  'Group of 6',  6,  6,  TRUE),
    ('GROUP_8',  'Group of 8',  8,  8,  TRUE),
    ('GROUP_12', 'Group of 12',12, 12, TRUE)
    ON CONFLICT (code) DO NOTHING;