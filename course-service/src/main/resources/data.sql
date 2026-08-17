-- E2E/수동 테스트용 시드 데이터.
-- 근거/시나리오: course-service/.claude/task/course-data.sql, course-scenario.md 참고.
-- instructor_id=100 강좌 3건(PUBLIC/PRIVATE/삭제됨) + instructor_id=200 대조군 1건.

-- 1) instructor_id=100, PUBLIC, 미삭제 -> instructor.suspended 이벤트 수신 시 PRIVATE로 바뀌어야 하는 정상 케이스
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes)
VALUES (9001, 100, '스프링 기초', '스프링 부트 입문 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'BEGINNER', 180);

-- 2) instructor_id=100, 이미 PRIVATE, 미삭제 -> 이벤트와 무관하게 그대로 PRIVATE 유지되어야 함
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes)
VALUES (9002, 100, '스프링 심화(비공개 draft)', '아직 공개 안 한 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'BACKEND', 'PRACTICAL', 360);

-- 3) instructor_id=100, PUBLIC이지만 soft-delete됨 -> deleted_at IS NULL 조건에서 제외되어야 함
INSERT INTO courses (id, instructor_id, title, description, status, created_at, deleted_at, category, difficulty, duration_minutes)
VALUES (9003, 100, '삭제된 강좌', '탈퇴/삭제 처리된 강좌', 'PUBLIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'BACKEND', 'ADVANCED', 600);

-- 4) instructor_id=200 (다른 강사), PUBLIC, 미삭제 -> 대조군. instructor_id=100 이벤트로 영향받으면 안 됨
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes)
VALUES (9004, 200, '다른 강사의 공개 강좌', '영향받으면 안 되는 대조군', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'BEGINNER', 180);


-- 2026-07-21: member-service 강사 5명(id=10~14)에 연결된 강좌 데이터.
-- 강사별 강좌 수는 10~50개 범위에서 배정(합계 130개). status는 대략 4개 중 1개꼴로 PRIVATE로 섞어
-- 강사 정지 이벤트 검증 시 PUBLIC -> PRIVATE 전환이 실제로 의미있게 보이도록 구성했다.
-- instructor_id=10: 강좌 15개
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20001, 10, '강사10의 강좌 1', 'instructor_id=10 강사가 개설한 1번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20002, 10, '강사10의 강좌 2', 'instructor_id=10 강사가 개설한 2번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20003, 10, '강사10의 강좌 3', 'instructor_id=10 강사가 개설한 3번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20004, 10, '강사10의 강좌 4', 'instructor_id=10 강사가 개설한 4번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'SECURITY', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20005, 10, '강사10의 강좌 5', 'instructor_id=10 강사가 개설한 5번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20006, 10, '강사10의 강좌 6', 'instructor_id=10 강사가 개설한 6번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20007, 10, '강사10의 강좌 7', 'instructor_id=10 강사가 개설한 7번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20008, 10, '강사10의 강좌 8', 'instructor_id=10 강사가 개설한 8번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'PRODUCT', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20009, 10, '강사10의 강좌 9', 'instructor_id=10 강사가 개설한 9번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20010, 10, '강사10의 강좌 10', 'instructor_id=10 강사가 개설한 10번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20011, 10, '강사10의 강좌 11', 'instructor_id=10 강사가 개설한 11번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20012, 10, '강사10의 강좌 12', 'instructor_id=10 강사가 개설한 12번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'MOBILE', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20013, 10, '강사10의 강좌 13', 'instructor_id=10 강사가 개설한 13번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20014, 10, '강사10의 강좌 14', 'instructor_id=10 강사가 개설한 14번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20015, 10, '강사10의 강좌 15', 'instructor_id=10 강사가 개설한 15번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'PRACTICAL', 360);
-- instructor_id=11: 강좌 32개
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20016, 11, '강사11의 강좌 1', 'instructor_id=11 강사가 개설한 1번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20017, 11, '강사11의 강좌 2', 'instructor_id=11 강사가 개설한 2번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20018, 11, '강사11의 강좌 3', 'instructor_id=11 강사가 개설한 3번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20019, 11, '강사11의 강좌 4', 'instructor_id=11 강사가 개설한 4번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'DESIGN', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20020, 11, '강사11의 강좌 5', 'instructor_id=11 강사가 개설한 5번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20021, 11, '강사11의 강좌 6', 'instructor_id=11 강사가 개설한 6번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20022, 11, '강사11의 강좌 7', 'instructor_id=11 강사가 개설한 7번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20023, 11, '강사11의 강좌 8', 'instructor_id=11 강사가 개설한 8번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'DEVOPS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20024, 11, '강사11의 강좌 9', 'instructor_id=11 강사가 개설한 9번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20025, 11, '강사11의 강좌 10', 'instructor_id=11 강사가 개설한 10번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20026, 11, '강사11의 강좌 11', 'instructor_id=11 강사가 개설한 11번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20027, 11, '강사11의 강좌 12', 'instructor_id=11 강사가 개설한 12번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'AI_ML', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20028, 11, '강사11의 강좌 13', 'instructor_id=11 강사가 개설한 13번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20029, 11, '강사11의 강좌 14', 'instructor_id=11 강사가 개설한 14번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20030, 11, '강사11의 강좌 15', 'instructor_id=11 강사가 개설한 15번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20031, 11, '강사11의 강좌 16', 'instructor_id=11 강사가 개설한 16번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'FRONTEND', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20032, 11, '강사11의 강좌 17', 'instructor_id=11 강사가 개설한 17번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20033, 11, '강사11의 강좌 18', 'instructor_id=11 강사가 개설한 18번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20034, 11, '강사11의 강좌 19', 'instructor_id=11 강사가 개설한 19번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20035, 11, '강사11의 강좌 20', 'instructor_id=11 강사가 개설한 20번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20036, 11, '강사11의 강좌 21', 'instructor_id=11 강사가 개설한 21번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20037, 11, '강사11의 강좌 22', 'instructor_id=11 강사가 개설한 22번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20038, 11, '강사11의 강좌 23', 'instructor_id=11 강사가 개설한 23번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20039, 11, '강사11의 강좌 24', 'instructor_id=11 강사가 개설한 24번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'DESIGN', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20040, 11, '강사11의 강좌 25', 'instructor_id=11 강사가 개설한 25번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20041, 11, '강사11의 강좌 26', 'instructor_id=11 강사가 개설한 26번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20042, 11, '강사11의 강좌 27', 'instructor_id=11 강사가 개설한 27번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20043, 11, '강사11의 강좌 28', 'instructor_id=11 강사가 개설한 28번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'DEVOPS', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20044, 11, '강사11의 강좌 29', 'instructor_id=11 강사가 개설한 29번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20045, 11, '강사11의 강좌 30', 'instructor_id=11 강사가 개설한 30번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20046, 11, '강사11의 강좌 31', 'instructor_id=11 강사가 개설한 31번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20047, 11, '강사11의 강좌 32', 'instructor_id=11 강사가 개설한 32번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'AI_ML', 'BEGINNER', 180);
-- instructor_id=12: 강좌 10개
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20048, 12, '강사12의 강좌 1', 'instructor_id=12 강사가 개설한 1번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20049, 12, '강사12의 강좌 2', 'instructor_id=12 강사가 개설한 2번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20050, 12, '강사12의 강좌 3', 'instructor_id=12 강사가 개설한 3번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20051, 12, '강사12의 강좌 4', 'instructor_id=12 강사가 개설한 4번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'FRONTEND', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20052, 12, '강사12의 강좌 5', 'instructor_id=12 강사가 개설한 5번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20053, 12, '강사12의 강좌 6', 'instructor_id=12 강사가 개설한 6번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20054, 12, '강사12의 강좌 7', 'instructor_id=12 강사가 개설한 7번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20055, 12, '강사12의 강좌 8', 'instructor_id=12 강사가 개설한 8번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20056, 12, '강사12의 강좌 9', 'instructor_id=12 강사가 개설한 9번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20057, 12, '강사12의 강좌 10', 'instructor_id=12 강사가 개설한 10번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'PRACTICAL', 360);
-- instructor_id=13: 강좌 47개
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20058, 13, '강사13의 강좌 1', 'instructor_id=13 강사가 개설한 1번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20059, 13, '강사13의 강좌 2', 'instructor_id=13 강사가 개설한 2번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20060, 13, '강사13의 강좌 3', 'instructor_id=13 강사가 개설한 3번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20061, 13, '강사13의 강좌 4', 'instructor_id=13 강사가 개설한 4번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'FRONTEND', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20062, 13, '강사13의 강좌 5', 'instructor_id=13 강사가 개설한 5번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20063, 13, '강사13의 강좌 6', 'instructor_id=13 강사가 개설한 6번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20064, 13, '강사13의 강좌 7', 'instructor_id=13 강사가 개설한 7번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20065, 13, '강사13의 강좌 8', 'instructor_id=13 강사가 개설한 8번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20066, 13, '강사13의 강좌 9', 'instructor_id=13 강사가 개설한 9번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20067, 13, '강사13의 강좌 10', 'instructor_id=13 강사가 개설한 10번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20068, 13, '강사13의 강좌 11', 'instructor_id=13 강사가 개설한 11번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20069, 13, '강사13의 강좌 12', 'instructor_id=13 강사가 개설한 12번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'DESIGN', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20070, 13, '강사13의 강좌 13', 'instructor_id=13 강사가 개설한 13번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20071, 13, '강사13의 강좌 14', 'instructor_id=13 강사가 개설한 14번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20072, 13, '강사13의 강좌 15', 'instructor_id=13 강사가 개설한 15번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20073, 13, '강사13의 강좌 16', 'instructor_id=13 강사가 개설한 16번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'DEVOPS', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20074, 13, '강사13의 강좌 17', 'instructor_id=13 강사가 개설한 17번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20075, 13, '강사13의 강좌 18', 'instructor_id=13 강사가 개설한 18번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20076, 13, '강사13의 강좌 19', 'instructor_id=13 강사가 개설한 19번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20077, 13, '강사13의 강좌 20', 'instructor_id=13 강사가 개설한 20번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'AI_ML', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20078, 13, '강사13의 강좌 21', 'instructor_id=13 강사가 개설한 21번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20079, 13, '강사13의 강좌 22', 'instructor_id=13 강사가 개설한 22번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20080, 13, '강사13의 강좌 23', 'instructor_id=13 강사가 개설한 23번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20081, 13, '강사13의 강좌 24', 'instructor_id=13 강사가 개설한 24번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'FRONTEND', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20082, 13, '강사13의 강좌 25', 'instructor_id=13 강사가 개설한 25번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20083, 13, '강사13의 강좌 26', 'instructor_id=13 강사가 개설한 26번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20084, 13, '강사13의 강좌 27', 'instructor_id=13 강사가 개설한 27번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20085, 13, '강사13의 강좌 28', 'instructor_id=13 강사가 개설한 28번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20086, 13, '강사13의 강좌 29', 'instructor_id=13 강사가 개설한 29번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20087, 13, '강사13의 강좌 30', 'instructor_id=13 강사가 개설한 30번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20088, 13, '강사13의 강좌 31', 'instructor_id=13 강사가 개설한 31번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20089, 13, '강사13의 강좌 32', 'instructor_id=13 강사가 개설한 32번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'DESIGN', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20090, 13, '강사13의 강좌 33', 'instructor_id=13 강사가 개설한 33번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20091, 13, '강사13의 강좌 34', 'instructor_id=13 강사가 개설한 34번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20092, 13, '강사13의 강좌 35', 'instructor_id=13 강사가 개설한 35번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20093, 13, '강사13의 강좌 36', 'instructor_id=13 강사가 개설한 36번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'DEVOPS', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20094, 13, '강사13의 강좌 37', 'instructor_id=13 강사가 개설한 37번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20095, 13, '강사13의 강좌 38', 'instructor_id=13 강사가 개설한 38번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20096, 13, '강사13의 강좌 39', 'instructor_id=13 강사가 개설한 39번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20097, 13, '강사13의 강좌 40', 'instructor_id=13 강사가 개설한 40번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'AI_ML', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20098, 13, '강사13의 강좌 41', 'instructor_id=13 강사가 개설한 41번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20099, 13, '강사13의 강좌 42', 'instructor_id=13 강사가 개설한 42번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20100, 13, '강사13의 강좌 43', 'instructor_id=13 강사가 개설한 43번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20101, 13, '강사13의 강좌 44', 'instructor_id=13 강사가 개설한 44번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'FRONTEND', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20102, 13, '강사13의 강좌 45', 'instructor_id=13 강사가 개설한 45번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20103, 13, '강사13의 강좌 46', 'instructor_id=13 강사가 개설한 46번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20104, 13, '강사13의 강좌 47', 'instructor_id=13 강사가 개설한 47번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'BEGINNER', 180);
-- instructor_id=14: 강좌 26개
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20105, 14, '강사14의 강좌 1', 'instructor_id=14 강사가 개설한 1번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20106, 14, '강사14의 강좌 2', 'instructor_id=14 강사가 개설한 2번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20107, 14, '강사14의 강좌 3', 'instructor_id=14 강사가 개설한 3번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20108, 14, '강사14의 강좌 4', 'instructor_id=14 강사가 개설한 4번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'PRODUCT', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20109, 14, '강사14의 강좌 5', 'instructor_id=14 강사가 개설한 5번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20110, 14, '강사14의 강좌 6', 'instructor_id=14 강사가 개설한 6번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20111, 14, '강사14의 강좌 7', 'instructor_id=14 강사가 개설한 7번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20112, 14, '강사14의 강좌 8', 'instructor_id=14 강사가 개설한 8번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'MOBILE', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20113, 14, '강사14의 강좌 9', 'instructor_id=14 강사가 개설한 9번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20114, 14, '강사14의 강좌 10', 'instructor_id=14 강사가 개설한 10번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20115, 14, '강사14의 강좌 11', 'instructor_id=14 강사가 개설한 11번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20116, 14, '강사14의 강좌 12', 'instructor_id=14 강사가 개설한 12번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20117, 14, '강사14의 강좌 13', 'instructor_id=14 강사가 개설한 13번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20118, 14, '강사14의 강좌 14', 'instructor_id=14 강사가 개설한 14번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20119, 14, '강사14의 강좌 15', 'instructor_id=14 강사가 개설한 15번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20120, 14, '강사14의 강좌 16', 'instructor_id=14 강사가 개설한 16번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'BACKEND', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20121, 14, '강사14의 강좌 17', 'instructor_id=14 강사가 개설한 17번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20122, 14, '강사14의 강좌 18', 'instructor_id=14 강사가 개설한 18번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20123, 14, '강사14의 강좌 19', 'instructor_id=14 강사가 개설한 19번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20124, 14, '강사14의 강좌 20', 'instructor_id=14 강사가 개설한 20번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'SECURITY', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20125, 14, '강사14의 강좌 21', 'instructor_id=14 강사가 개설한 21번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20126, 14, '강사14의 강좌 22', 'instructor_id=14 강사가 개설한 22번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20127, 14, '강사14의 강좌 23', 'instructor_id=14 강사가 개설한 23번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20128, 14, '강사14의 강좌 24', 'instructor_id=14 강사가 개설한 24번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP, 'PRODUCT', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20129, 14, '강사14의 강좌 25', 'instructor_id=14 강사가 개설한 25번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20130, 14, '강사14의 강좌 26', 'instructor_id=14 강사가 개설한 26번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'ADVANCED', 600);

-- 2026-08-16: 커리큘럼 추천봇 데모용 강좌 데이터.
-- 위 20001~20130은 강사 정지 이벤트 검증용 더미라 제목/설명으로는 검색이 되지 않는다.
-- 봇은 제목과 설명을 어휘 단위로 매칭해 강의를 찾으므로, 실제 검색어와 맞물리는 문장이 필요하다.
-- 10개 카테고리 x 입문/실전/심화 3단계 = 30건이며 모두 PUBLIC(봇 인덱스 대상)이다.
-- 같은 단계 안에서도 소요 시간을 다르게 두어 "더 짧은 강의" 같은 재검색 요청을 시연할 수 있게 했다.

-- 백엔드 개발
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30001, 10, '웹과 REST API 기초', 'HTTP, REST, 데이터베이스 등 백엔드 개발에 필요한 기본 개념을 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30002, 10, 'Spring Boot 실전 API 개발', 'Spring Boot와 JPA로 인증과 데이터베이스를 포함한 REST API를 구현합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'PRACTICAL', 480);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30003, 10, '확장 가능한 백엔드 아키텍처', '캐시, 메시지 큐, 분산 트레이싱을 적용해 대규모 트래픽을 처리합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'ADVANCED', 720);

-- 프론트엔드 개발
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30004, 10, 'HTML CSS 자바스크립트 시작하기', '웹 페이지 구조와 스타일, 기본 자바스크립트 문법을 처음부터 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'BEGINNER', 240);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30005, 10, 'React로 만드는 실무 웹 애플리케이션', 'React 컴포넌트와 상태 관리, API 연동으로 실제 화면을 구현합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'PRACTICAL', 420);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30006, 10, '프론트엔드 성능 최적화와 렌더링', '번들 크기와 렌더링 병목을 측정하고 웹 성능 지표를 개선합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'ADVANCED', 540);

-- 모바일 개발
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30007, 11, '모바일 앱 개발 입문', '안드로이드와 iOS 앱의 구조, 화면 구성, 배포 흐름을 이해합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'BEGINNER', 200);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30008, 11, 'Flutter로 만드는 크로스플랫폼 앱', 'Flutter 위젯과 상태 관리로 안드로이드와 iOS 앱을 함께 개발합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'PRACTICAL', 450);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30009, 11, '모바일 앱 성능과 오프라인 동기화', '네트워크 캐시, 로컬 데이터베이스, 백그라운드 동기화를 설계합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'ADVANCED', 600);

-- 데브옵스 인프라
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30010, 11, 'Docker와 컨테이너 기초', '컨테이너 개념과 이미지 빌드, docker compose 사용법을 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'BEGINNER', 150);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30011, 11, 'CI/CD 파이프라인 구축 실무', 'GitHub Actions로 빌드와 테스트, 배포를 자동화하는 파이프라인을 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30012, 11, '쿠버네티스 운영과 관측성', '쿠버네티스 클러스터를 운영하고 Prometheus와 Grafana로 모니터링합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'ADVANCED', 660);

-- 보안
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30013, 12, '개발자를 위한 웹 보안 기초', 'XSS, CSRF, SQL 인젝션 등 대표적인 웹 취약점과 방어 방법을 이해합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'BEGINNER', 120);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30014, 12, '인증과 인가 실무 OAuth2와 JWT', 'OAuth2 인증 흐름과 JWT 토큰 기반 인가를 직접 구현하고 검증합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'PRACTICAL', 390);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30015, 12, '보안 취약점 진단과 사고 대응', '취약점 스캐닝과 침투 테스트 시나리오, 보안 사고 대응 절차를 다룹니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'ADVANCED', 540);

-- 데이터 분석
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30016, 12, '엑셀로 시작하는 데이터 분석', '함수와 피벗 테이블, 차트를 활용해 업무 데이터를 정리하고 해석합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30017, 12, '실무 SQL과 대시보드 만들기', 'SQL로 데이터를 추출하고 핵심 지표를 보여주는 대시보드를 설계합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'PRACTICAL', 420);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30018, 12, '통계 기반 데이터 분석과 실험 설계', '가설 검정과 회귀 분석, A/B 테스트 설계로 의사결정을 지원합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'ADVANCED', 600);

-- 데이터 엔지니어링
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30019, 13, '데이터 파이프라인 입문', '데이터 수집과 적재, 변환으로 이어지는 ETL의 기본 흐름을 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'BEGINNER', 210);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30020, 13, 'Airflow로 배치 파이프라인 운영', 'Airflow DAG를 작성하고 스케줄링과 재처리, 모니터링을 구성합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'PRACTICAL', 480);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30021, 13, '대용량 데이터 처리와 스트리밍', 'Spark와 Kafka로 대용량 배치와 실시간 스트리밍 파이프라인을 구축합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'ADVANCED', 720);

-- AI 머신러닝
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30022, 13, '머신러닝 기초와 파이썬', '파이썬과 사이킷런으로 분류와 회귀 모델의 기본 원리를 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'BEGINNER', 240);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30023, 13, '딥러닝 모델 학습과 평가', '파이토치로 신경망을 학습시키고 성능을 평가해 모델을 개선합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'PRACTICAL', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30024, 13, 'LLM 활용과 RAG 시스템 구축', '대규모 언어 모델과 벡터 검색을 결합한 RAG 서비스를 설계하고 구현합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'ADVANCED', 780);

-- 프로덕트
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30025, 14, '프로덕트 매니지먼트 기초', '고객 문제 정의부터 요구사항 정리와 제품 지표까지 기본기를 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30026, 14, '데이터 기반 제품 개선', '제품 데이터를 분석해 개선 가설을 세우고 실험으로 검증합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'PRACTICAL', 330);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30027, 14, '제품 전략과 로드맵 수립', '사업 목표와 고객 가치를 연결한 제품 전략과 로드맵을 설계합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'ADVANCED', 480);

-- 디자인 UX
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30028, 14, 'UI 디자인과 피그마 기초', '피그마로 화면을 설계하고 디자인 시스템의 기본 개념을 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'BEGINNER', 150);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30029, 14, '사용자 리서치와 UX 개선', '사용자 인터뷰와 사용성 테스트 결과로 화면 흐름을 개선합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (30030, 14, '디자인 시스템 구축과 운영', '컴포넌트와 디자인 토큰, 가이드라인을 정의해 조직 전체에 정착시킵니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'ADVANCED', 540);

-- 다음 auto-increment는 기존 9001~9004, 20001~20130, 30001~30030 데이터와 겹치지 않도록 시작 값을 올린다
ALTER TABLE courses ALTER COLUMN id RESTART WITH 30031;