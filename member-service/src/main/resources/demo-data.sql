-- Demo seed data for local H2 initialization.
-- Password for demo instructors: password123

INSERT INTO members (
    id,
    role,
    email,
    password,
    deleted,
    created_at,
    updated_at,
    profile_name,
    profile_image_url,
    profile_introduction
) VALUES
(
    1,
    'INSTRUCTOR',
    'instructor.kim@example.com',
    '$2a$10$dXJ3SW6G7P50lGm9kkj9ze9SCqHPLSElPvG7xGxYEOTc8xWYR3nTa',
    FALSE,
    CURRENT_TIMESTAMP,
    NULL,
    '김도현',
    'https://cdn.example.com/profiles/instructor-kim.png',
    '백엔드 아키텍처와 도메인 모델링을 강의하는 10년 차 Java 개발자입니다.'
),
(
    2,
    'INSTRUCTOR',
    'instructor.lee@example.com',
    '$2a$10$dXJ3SW6G7P50lGm9kkj9ze9SCqHPLSElPvG7xGxYEOTc8xWYR3nTa',
    FALSE,
    CURRENT_TIMESTAMP,
    NULL,
    '이서연',
    'https://cdn.example.com/profiles/instructor-lee.png',
    '서비스 운영, 품질 자동화, 콘텐츠 기획을 함께 다루는 LXP 실무 강사입니다.'
);

INSERT INTO courses (
    id,
    instructor_id,
    title,
    description,
    thumbnail_url,
    status,
    created_at,
    updated_at,
    deleted_at
) VALUES
(1, 1, 'Java 백엔드 입문', 'Spring Boot 기반 REST API 개발의 핵심 흐름을 익히는 입문 과정입니다.', 'https://cdn.example.com/courses/course-01.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(2, 1, 'Spring Security 실전', '인증과 인가, JWT 기반 보안 구성을 실습 중심으로 다룹니다.', 'https://cdn.example.com/courses/course-02.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(3, 1, 'JPA 도메인 모델링', '엔티티 관계와 값 객체를 활용해 유지보수 가능한 도메인을 설계합니다.', 'https://cdn.example.com/courses/course-03.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(4, 1, '테스트 주도 개발', 'JUnit과 Mockito를 활용해 서비스와 컨트롤러 테스트를 작성합니다.', 'https://cdn.example.com/courses/course-04.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(5, 1, '클린 코드 리팩터링', '읽기 좋은 Java 코드를 만들기 위한 리팩터링 패턴을 학습합니다.', 'https://cdn.example.com/courses/course-05.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(6, 1, '데이터베이스 설계 기초', '관계형 데이터 모델링과 인덱스 설계의 기본기를 익힙니다.', 'https://cdn.example.com/courses/course-06.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(7, 1, 'REST API 설계', '일관된 리소스 모델과 오류 응답을 갖춘 API 설계를 연습합니다.', 'https://cdn.example.com/courses/course-07.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(8, 1, '마이크로서비스 개요', '서비스 분리, 통신, 배포 전략을 큰 그림으로 이해합니다.', 'https://cdn.example.com/courses/course-08.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(9, 1, 'Docker 배포 입문', 'Spring Boot 애플리케이션을 컨테이너로 패키징하고 실행합니다.', 'https://cdn.example.com/courses/course-09.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(10, 1, '운영 로그와 모니터링', '로그 구조화와 기본 모니터링 지표를 활용한 운영 방식을 다룹니다.', 'https://cdn.example.com/courses/course-10.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(11, 2, '프론트엔드 협업 API', '프론트엔드와 원활히 협업하기 위한 API 계약과 문서를 작성합니다.', 'https://cdn.example.com/courses/course-11.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(12, 2, '구독 서비스 도메인', '구독, 결제, 환불 플로우를 도메인 관점에서 모델링합니다.', 'https://cdn.example.com/courses/course-12.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(13, 2, '결제 시스템 시뮬레이션', '외부 결제 연동을 더미 어댑터로 안전하게 모사합니다.', 'https://cdn.example.com/courses/course-13.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(14, 2, 'LXP 콘텐츠 운영', '강좌, 강의, 미션을 운영 관점에서 구성하는 방법을 학습합니다.', 'https://cdn.example.com/courses/course-14.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(15, 2, '관리자 기능 설계', '운영자 권한과 관리 화면에 필요한 백엔드 기능을 설계합니다.', 'https://cdn.example.com/courses/course-15.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(16, 2, '성능 튜닝 첫걸음', 'N+1 문제와 페이징, 캐시 전략의 기초를 살펴봅니다.', 'https://cdn.example.com/courses/course-16.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(17, 2, '예외 처리와 검증', '도메인 예외와 Bean Validation을 활용해 안정적인 입력 처리를 구현합니다.', 'https://cdn.example.com/courses/course-17.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(18, 2, 'CI 품질 게이트', '테스트, PMD, JaCoCo를 활용해 자동 품질 검사를 구성합니다.', 'https://cdn.example.com/courses/course-18.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(19, 2, '팀 프로젝트 설계 리뷰', '요구사항을 기능 단위로 쪼개고 리뷰 가능한 설계 문서를 만듭니다.', 'https://cdn.example.com/courses/course-19.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL),
(20, 2, '실전 포트폴리오 완성', '학습한 내용을 종합해 데모 가능한 LXP 백엔드를 완성합니다.', 'https://cdn.example.com/courses/course-20.png', 'PUBLIC', CURRENT_TIMESTAMP, NULL, NULL);

INSERT INTO lectures (
    id,
    course_id,
    title,
    status,
    content_url,
    content_type,
    sort_order,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    (c.id * 10) + r.x,
    c.id,
    CONCAT('강의 ', r.x, ' - ', c.title),
    'PUBLIC',
    CONCAT('https://cdn.example.com/lectures/course-', c.id, '/lecture-', r.x, '.mp4'),
    'mp4',
    r.x,
    CURRENT_TIMESTAMP,
    NULL,
    NULL
FROM courses c
CROSS JOIN SYSTEM_RANGE(1, 5) r;

INSERT INTO missions (
    id,
    course_id,
    title,
    status,
    content,
    sort_order,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    (c.id * 10) + 1,
    c.id,
    CONCAT(c.title, ' 미션 1'),
    'PUBLIC',
    CONCAT(c.title, '에서 배운 내용을 바탕으로 결과물을 제출하고 핵심 개념을 짧게 설명하세요.'),
    6,
    CURRENT_TIMESTAMP,
    NULL,
    NULL
FROM courses c;

INSERT INTO missions (
    id,
    course_id,
    title,
    status,
    content,
    sort_order,
    created_at,
    updated_at,
    deleted_at
)
SELECT
    (c.id * 10) + 2,
    c.id,
    CONCAT(c.title, ' 미션 2'),
    'PUBLIC',
    CONCAT(c.title, '의 실무 적용 시나리오를 작성하고 개선 포인트를 정리하세요.'),
    7,
    CURRENT_TIMESTAMP,
    NULL,
    NULL
FROM courses c
WHERE MOD(c.id, 2) = 0;

ALTER TABLE members ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE courses ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE lectures ALTER COLUMN id RESTART WITH 1000;
ALTER TABLE missions ALTER COLUMN id RESTART WITH 1000;
