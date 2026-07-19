-- Demo members for local H2 API verification.
-- Instructor is seeded with a fixed id(2) so that other services (e.g. course-service)
-- can seed demo data referencing this instructor's id.
INSERT INTO members (
    id,
    role,
    email,
    password,
    deleted,
    suspended_at,
    created_at,
    updated_at,
    profile_name,
    profile_image_url,
    profile_introduction
) VALUES (
    1,
    'ADMIN',
    'admin@lxp.local',
    '{noop}placeholder-encoded-password',
    false,
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    NULL,
    NULL,
    NULL
);

INSERT INTO members (
    id,
    role,
    email,
    password,
    deleted,
    suspended_at,
    created_at,
    updated_at,
    profile_name,
    profile_image_url,
    profile_introduction
) VALUES (
    2,
    'INSTRUCTOR',
    'instructor@lxp.local',
    '{noop}placeholder-encoded-password',
    false,
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    '데모 강사',
    NULL,
    '데모용 강사 계정입니다.'
);

INSERT INTO members (
    id,
    role,
    email,
    password,
    deleted,
    suspended_at,
    created_at,
    updated_at,
    profile_name,
    profile_image_url,
    profile_introduction
) VALUES (
    3,
    'MEMBER',
    'member@lxp.local',
    '{noop}placeholder-encoded-password',
    false,
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    NULL,
    NULL,
    NULL
);

ALTER TABLE members ALTER COLUMN id RESTART WITH 1000;
