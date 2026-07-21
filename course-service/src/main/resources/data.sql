-- E2E/수동 테스트용 시드 데이터.
-- 근거/시나리오: course-service/.claude/task/course-data.sql, course-scenario.md 참고.
-- instructor_id=100 강좌 3건(PUBLIC/PRIVATE/삭제됨) + instructor_id=200 대조군 1건.

-- 1) instructor_id=100, PUBLIC, 미삭제 -> instructor.suspended 이벤트 수신 시 PRIVATE로 바뀌어야 하는 정상 케이스
INSERT INTO courses (id, instructor_id, title, description, status, created_at)
VALUES (9001, 100, '스프링 기초', '스프링 부트 입문 강좌', 'PUBLIC', CURRENT_TIMESTAMP);

-- 2) instructor_id=100, 이미 PRIVATE, 미삭제 -> 이벤트와 무관하게 그대로 PRIVATE 유지되어야 함
INSERT INTO courses (id, instructor_id, title, description, status, created_at)
VALUES (9002, 100, '스프링 심화(비공개 draft)', '아직 공개 안 한 강좌', 'PRIVATE', CURRENT_TIMESTAMP);

-- 3) instructor_id=100, PUBLIC이지만 soft-delete됨 -> deleted_at IS NULL 조건에서 제외되어야 함
INSERT INTO courses (id, instructor_id, title, description, status, created_at, deleted_at)
VALUES (9003, 100, '삭제된 강좌', '탈퇴/삭제 처리된 강좌', 'PUBLIC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 4) instructor_id=200 (다른 강사), PUBLIC, 미삭제 -> 대조군. instructor_id=100 이벤트로 영향받으면 안 됨
INSERT INTO courses (id, instructor_id, title, description, status, created_at)
VALUES (9004, 200, '다른 강사의 공개 강좌', '영향받으면 안 되는 대조군', 'PUBLIC', CURRENT_TIMESTAMP);


-- 2026-07-21: member-service 강사 5명(id=10~14)에 연결된 강좌 데이터.
-- 강사별 강좌 수는 10~50개 범위에서 배정(합계 130개). status는 대략 4개 중 1개꼴로 PRIVATE로 섞어
-- 강사 정지 이벤트 검증 시 PUBLIC -> PRIVATE 전환이 실제로 의미있게 보이도록 구성했다.
-- instructor_id=10: 강좌 15개
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20001, 10, '강사10의 강좌 1', 'instructor_id=10 강사가 개설한 1번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20002, 10, '강사10의 강좌 2', 'instructor_id=10 강사가 개설한 2번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20003, 10, '강사10의 강좌 3', 'instructor_id=10 강사가 개설한 3번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20004, 10, '강사10의 강좌 4', 'instructor_id=10 강사가 개설한 4번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20005, 10, '강사10의 강좌 5', 'instructor_id=10 강사가 개설한 5번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20006, 10, '강사10의 강좌 6', 'instructor_id=10 강사가 개설한 6번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20007, 10, '강사10의 강좌 7', 'instructor_id=10 강사가 개설한 7번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20008, 10, '강사10의 강좌 8', 'instructor_id=10 강사가 개설한 8번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20009, 10, '강사10의 강좌 9', 'instructor_id=10 강사가 개설한 9번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20010, 10, '강사10의 강좌 10', 'instructor_id=10 강사가 개설한 10번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20011, 10, '강사10의 강좌 11', 'instructor_id=10 강사가 개설한 11번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20012, 10, '강사10의 강좌 12', 'instructor_id=10 강사가 개설한 12번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20013, 10, '강사10의 강좌 13', 'instructor_id=10 강사가 개설한 13번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20014, 10, '강사10의 강좌 14', 'instructor_id=10 강사가 개설한 14번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20015, 10, '강사10의 강좌 15', 'instructor_id=10 강사가 개설한 15번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
-- instructor_id=11: 강좌 32개
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20016, 11, '강사11의 강좌 1', 'instructor_id=11 강사가 개설한 1번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20017, 11, '강사11의 강좌 2', 'instructor_id=11 강사가 개설한 2번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20018, 11, '강사11의 강좌 3', 'instructor_id=11 강사가 개설한 3번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20019, 11, '강사11의 강좌 4', 'instructor_id=11 강사가 개설한 4번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20020, 11, '강사11의 강좌 5', 'instructor_id=11 강사가 개설한 5번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20021, 11, '강사11의 강좌 6', 'instructor_id=11 강사가 개설한 6번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20022, 11, '강사11의 강좌 7', 'instructor_id=11 강사가 개설한 7번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20023, 11, '강사11의 강좌 8', 'instructor_id=11 강사가 개설한 8번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20024, 11, '강사11의 강좌 9', 'instructor_id=11 강사가 개설한 9번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20025, 11, '강사11의 강좌 10', 'instructor_id=11 강사가 개설한 10번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20026, 11, '강사11의 강좌 11', 'instructor_id=11 강사가 개설한 11번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20027, 11, '강사11의 강좌 12', 'instructor_id=11 강사가 개설한 12번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20028, 11, '강사11의 강좌 13', 'instructor_id=11 강사가 개설한 13번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20029, 11, '강사11의 강좌 14', 'instructor_id=11 강사가 개설한 14번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20030, 11, '강사11의 강좌 15', 'instructor_id=11 강사가 개설한 15번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20031, 11, '강사11의 강좌 16', 'instructor_id=11 강사가 개설한 16번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20032, 11, '강사11의 강좌 17', 'instructor_id=11 강사가 개설한 17번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20033, 11, '강사11의 강좌 18', 'instructor_id=11 강사가 개설한 18번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20034, 11, '강사11의 강좌 19', 'instructor_id=11 강사가 개설한 19번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20035, 11, '강사11의 강좌 20', 'instructor_id=11 강사가 개설한 20번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20036, 11, '강사11의 강좌 21', 'instructor_id=11 강사가 개설한 21번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20037, 11, '강사11의 강좌 22', 'instructor_id=11 강사가 개설한 22번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20038, 11, '강사11의 강좌 23', 'instructor_id=11 강사가 개설한 23번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20039, 11, '강사11의 강좌 24', 'instructor_id=11 강사가 개설한 24번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20040, 11, '강사11의 강좌 25', 'instructor_id=11 강사가 개설한 25번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20041, 11, '강사11의 강좌 26', 'instructor_id=11 강사가 개설한 26번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20042, 11, '강사11의 강좌 27', 'instructor_id=11 강사가 개설한 27번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20043, 11, '강사11의 강좌 28', 'instructor_id=11 강사가 개설한 28번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20044, 11, '강사11의 강좌 29', 'instructor_id=11 강사가 개설한 29번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20045, 11, '강사11의 강좌 30', 'instructor_id=11 강사가 개설한 30번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20046, 11, '강사11의 강좌 31', 'instructor_id=11 강사가 개설한 31번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20047, 11, '강사11의 강좌 32', 'instructor_id=11 강사가 개설한 32번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
-- instructor_id=12: 강좌 10개
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20048, 12, '강사12의 강좌 1', 'instructor_id=12 강사가 개설한 1번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20049, 12, '강사12의 강좌 2', 'instructor_id=12 강사가 개설한 2번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20050, 12, '강사12의 강좌 3', 'instructor_id=12 강사가 개설한 3번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20051, 12, '강사12의 강좌 4', 'instructor_id=12 강사가 개설한 4번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20052, 12, '강사12의 강좌 5', 'instructor_id=12 강사가 개설한 5번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20053, 12, '강사12의 강좌 6', 'instructor_id=12 강사가 개설한 6번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20054, 12, '강사12의 강좌 7', 'instructor_id=12 강사가 개설한 7번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20055, 12, '강사12의 강좌 8', 'instructor_id=12 강사가 개설한 8번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20056, 12, '강사12의 강좌 9', 'instructor_id=12 강사가 개설한 9번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20057, 12, '강사12의 강좌 10', 'instructor_id=12 강사가 개설한 10번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
-- instructor_id=13: 강좌 47개
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20058, 13, '강사13의 강좌 1', 'instructor_id=13 강사가 개설한 1번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20059, 13, '강사13의 강좌 2', 'instructor_id=13 강사가 개설한 2번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20060, 13, '강사13의 강좌 3', 'instructor_id=13 강사가 개설한 3번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20061, 13, '강사13의 강좌 4', 'instructor_id=13 강사가 개설한 4번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20062, 13, '강사13의 강좌 5', 'instructor_id=13 강사가 개설한 5번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20063, 13, '강사13의 강좌 6', 'instructor_id=13 강사가 개설한 6번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20064, 13, '강사13의 강좌 7', 'instructor_id=13 강사가 개설한 7번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20065, 13, '강사13의 강좌 8', 'instructor_id=13 강사가 개설한 8번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20066, 13, '강사13의 강좌 9', 'instructor_id=13 강사가 개설한 9번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20067, 13, '강사13의 강좌 10', 'instructor_id=13 강사가 개설한 10번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20068, 13, '강사13의 강좌 11', 'instructor_id=13 강사가 개설한 11번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20069, 13, '강사13의 강좌 12', 'instructor_id=13 강사가 개설한 12번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20070, 13, '강사13의 강좌 13', 'instructor_id=13 강사가 개설한 13번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20071, 13, '강사13의 강좌 14', 'instructor_id=13 강사가 개설한 14번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20072, 13, '강사13의 강좌 15', 'instructor_id=13 강사가 개설한 15번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20073, 13, '강사13의 강좌 16', 'instructor_id=13 강사가 개설한 16번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20074, 13, '강사13의 강좌 17', 'instructor_id=13 강사가 개설한 17번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20075, 13, '강사13의 강좌 18', 'instructor_id=13 강사가 개설한 18번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20076, 13, '강사13의 강좌 19', 'instructor_id=13 강사가 개설한 19번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20077, 13, '강사13의 강좌 20', 'instructor_id=13 강사가 개설한 20번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20078, 13, '강사13의 강좌 21', 'instructor_id=13 강사가 개설한 21번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20079, 13, '강사13의 강좌 22', 'instructor_id=13 강사가 개설한 22번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20080, 13, '강사13의 강좌 23', 'instructor_id=13 강사가 개설한 23번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20081, 13, '강사13의 강좌 24', 'instructor_id=13 강사가 개설한 24번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20082, 13, '강사13의 강좌 25', 'instructor_id=13 강사가 개설한 25번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20083, 13, '강사13의 강좌 26', 'instructor_id=13 강사가 개설한 26번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20084, 13, '강사13의 강좌 27', 'instructor_id=13 강사가 개설한 27번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20085, 13, '강사13의 강좌 28', 'instructor_id=13 강사가 개설한 28번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20086, 13, '강사13의 강좌 29', 'instructor_id=13 강사가 개설한 29번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20087, 13, '강사13의 강좌 30', 'instructor_id=13 강사가 개설한 30번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20088, 13, '강사13의 강좌 31', 'instructor_id=13 강사가 개설한 31번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20089, 13, '강사13의 강좌 32', 'instructor_id=13 강사가 개설한 32번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20090, 13, '강사13의 강좌 33', 'instructor_id=13 강사가 개설한 33번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20091, 13, '강사13의 강좌 34', 'instructor_id=13 강사가 개설한 34번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20092, 13, '강사13의 강좌 35', 'instructor_id=13 강사가 개설한 35번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20093, 13, '강사13의 강좌 36', 'instructor_id=13 강사가 개설한 36번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20094, 13, '강사13의 강좌 37', 'instructor_id=13 강사가 개설한 37번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20095, 13, '강사13의 강좌 38', 'instructor_id=13 강사가 개설한 38번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20096, 13, '강사13의 강좌 39', 'instructor_id=13 강사가 개설한 39번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20097, 13, '강사13의 강좌 40', 'instructor_id=13 강사가 개설한 40번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20098, 13, '강사13의 강좌 41', 'instructor_id=13 강사가 개설한 41번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20099, 13, '강사13의 강좌 42', 'instructor_id=13 강사가 개설한 42번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20100, 13, '강사13의 강좌 43', 'instructor_id=13 강사가 개설한 43번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20101, 13, '강사13의 강좌 44', 'instructor_id=13 강사가 개설한 44번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20102, 13, '강사13의 강좌 45', 'instructor_id=13 강사가 개설한 45번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20103, 13, '강사13의 강좌 46', 'instructor_id=13 강사가 개설한 46번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20104, 13, '강사13의 강좌 47', 'instructor_id=13 강사가 개설한 47번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
-- instructor_id=14: 강좌 26개
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20105, 14, '강사14의 강좌 1', 'instructor_id=14 강사가 개설한 1번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20106, 14, '강사14의 강좌 2', 'instructor_id=14 강사가 개설한 2번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20107, 14, '강사14의 강좌 3', 'instructor_id=14 강사가 개설한 3번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20108, 14, '강사14의 강좌 4', 'instructor_id=14 강사가 개설한 4번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20109, 14, '강사14의 강좌 5', 'instructor_id=14 강사가 개설한 5번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20110, 14, '강사14의 강좌 6', 'instructor_id=14 강사가 개설한 6번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20111, 14, '강사14의 강좌 7', 'instructor_id=14 강사가 개설한 7번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20112, 14, '강사14의 강좌 8', 'instructor_id=14 강사가 개설한 8번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20113, 14, '강사14의 강좌 9', 'instructor_id=14 강사가 개설한 9번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20114, 14, '강사14의 강좌 10', 'instructor_id=14 강사가 개설한 10번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20115, 14, '강사14의 강좌 11', 'instructor_id=14 강사가 개설한 11번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20116, 14, '강사14의 강좌 12', 'instructor_id=14 강사가 개설한 12번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20117, 14, '강사14의 강좌 13', 'instructor_id=14 강사가 개설한 13번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20118, 14, '강사14의 강좌 14', 'instructor_id=14 강사가 개설한 14번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20119, 14, '강사14의 강좌 15', 'instructor_id=14 강사가 개설한 15번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20120, 14, '강사14의 강좌 16', 'instructor_id=14 강사가 개설한 16번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20121, 14, '강사14의 강좌 17', 'instructor_id=14 강사가 개설한 17번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20122, 14, '강사14의 강좌 18', 'instructor_id=14 강사가 개설한 18번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20123, 14, '강사14의 강좌 19', 'instructor_id=14 강사가 개설한 19번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20124, 14, '강사14의 강좌 20', 'instructor_id=14 강사가 개설한 20번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20125, 14, '강사14의 강좌 21', 'instructor_id=14 강사가 개설한 21번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20126, 14, '강사14의 강좌 22', 'instructor_id=14 강사가 개설한 22번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20127, 14, '강사14의 강좌 23', 'instructor_id=14 강사가 개설한 23번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20128, 14, '강사14의 강좌 24', 'instructor_id=14 강사가 개설한 24번째 강좌', 'PRIVATE', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20129, 14, '강사14의 강좌 25', 'instructor_id=14 강사가 개설한 25번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);
INSERT INTO courses (id, instructor_id, title, description, status, created_at) VALUES (20130, 14, '강사14의 강좌 26', 'instructor_id=14 강사가 개설한 26번째 강좌', 'PUBLIC', CURRENT_TIMESTAMP);

-- 다음 auto-increment는 기존 9001~9004 테스트 데이터와도 겹치지 않도록 20231부터 시작
ALTER TABLE courses ALTER COLUMN id RESTART WITH 20231;