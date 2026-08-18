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
    -- [변경] {noop} 평문 대신 BCrypt 해시 사용
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
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
    -- [변경] {noop} 평문 대신 BCrypt 해시 사용
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
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
    -- [변경] {noop} 평문 대신 BCrypt 해시 사용
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false,
    NULL,
    CURRENT_TIMESTAMP,
    NULL,
    NULL,
    NULL,
    NULL
);

-- 2026-07-21: E2E 연관관계 테스트용 강사 5명 + 구독권 보유 가능 회원 10명 추가 시드.
-- 강사(id=10~14)는 course-service의 courses.instructor_id가 그대로 참조한다.
-- 회원(id=20~29)은 subscription-service의 subscriptions.member_id가 그대로 참조한다.

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    10, 'INSTRUCTOR', 'instructor10@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, '강사1', NULL, 'E2E 테스트용 강사 계정 1'
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    11, 'INSTRUCTOR', 'instructor11@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, '강사2', NULL, 'E2E 테스트용 강사 계정 2'
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    12, 'INSTRUCTOR', 'instructor12@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, '강사3', NULL, 'E2E 테스트용 강사 계정 3'
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    13, 'INSTRUCTOR', 'instructor13@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, '강사4', NULL, 'E2E 테스트용 강사 계정 4'
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    14, 'INSTRUCTOR', 'instructor14@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, '강사5', NULL, 'E2E 테스트용 강사 계정 5'
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    20, 'MEMBER', 'member20@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    21, 'MEMBER', 'member21@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    22, 'MEMBER', 'member22@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    23, 'MEMBER', 'member23@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    24, 'MEMBER', 'member24@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    25, 'MEMBER', 'member25@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    26, 'MEMBER', 'member26@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    27, 'MEMBER', 'member27@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    28, 'MEMBER', 'member28@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL
);

INSERT INTO members (
    id, role, email, password, deleted, suspended_at, created_at, updated_at,
    profile_name, profile_image_url, profile_introduction
) VALUES (
    29, 'MEMBER', 'member29@lxp.local',
    '$2y$10$IzeB2ksrc1z0YaLARmh1K.Jo47Y1FyYIL1AgTGk7hyflw.3oRt2qG',
    false, NULL,
    CURRENT_TIMESTAMP, NULL, NULL, NULL, NULL
);

ALTER TABLE members ALTER COLUMN id RESTART WITH 1000;