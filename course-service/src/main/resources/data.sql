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
-- 2026-08-18: 제목과 설명이 "강사10의 강좌 1" 같은 기계 생성 문자열이라 봇의 본문 검색에 노이즈였다.
-- id, instructor_id, status, category, difficulty는 위 시나리오가 의존하므로 그대로 두고
-- 제목/설명을 카테고리·난이도에 맞는 실제 문장으로 바꾸고, duration_minutes도 세 값 고정에서 벗어나게 했다.
-- instructor_id=10: 강좌 15개
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20001, 10, '렌더링 성능과 번들 최적화', '코드 스플리팅과 메모이제이션으로 초기 로딩과 재렌더를 줄입니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'ADVANCED', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20002, 10, '안드로이드 앱 만들기 첫걸음', '액티비티와 레이아웃으로 첫 화면을 구성하고 실행합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'BEGINNER', 210);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20003, 10, '컨테이너 이미지 최적화', '멀티스테이지 빌드와 레이어 캐시로 이미지 크기를 줄입니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'PRACTICAL', 330);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20004, 10, '침해 사고 대응과 포렌식', '침해 흔적을 수집하고 복구까지의 절차를 수행합니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'SECURITY', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20005, 10, '데이터 분석을 위한 통계 기초', '평균과 분산, 분포 개념으로 데이터를 요약합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20006, 10, 'ETL 파이프라인 구축 실무', '원천 데이터를 정제해 분석용 테이블로 적재합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'PRACTICAL', 390);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20007, 10, 'MLOps 파이프라인 구축', '학습부터 배포, 재학습까지 자동화합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'ADVANCED', 660);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20008, 10, '프로덕트 오너의 하루', '백로그 관리부터 스프린트 운영까지 역할을 이해합니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'PRODUCT', 'BEGINNER', 150);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20009, 10, '사용성 테스트 설계와 진행', '과제를 주고 관찰해 개선점을 찾습니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20010, 10, '마이크로서비스 분해와 통신 설계', '서비스 경계를 나누고 동기와 비동기 통신, 장애 격리를 설계합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20011, 10, '웹 화면 만들기 첫걸음', 'HTML 구조와 CSS 레이아웃으로 반응형 페이지를 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20012, 10, '앱 네트워크 통신과 데이터 저장', 'REST 호출과 로컬 DB를 연결해 오프라인에서도 동작하게 만듭니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'MOBILE', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20013, 10, '쿠버네티스 클러스터 운영', '스케줄링과 오토스케일, 롤아웃 전략을 다룹니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'ADVANCED', 660);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20014, 10, '정보보안 기초 개념', '기밀성과 무결성, 가용성 관점에서 위협을 이해합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20015, 10, 'SQL 심화 쿼리와 윈도우 함수', '순위와 누적 집계를 쿼리 한 번으로 계산합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'PRACTICAL', 360);
-- instructor_id=11: 강좌 32개
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20016, 11, '레이크하우스 아키텍처 설계', '데이터 레이크와 웨어하우스의 장점을 합칩니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20017, 11, '인공지능 개념 잡기', '머신러닝과 딥러닝, 생성 AI의 관계를 정리합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'BEGINNER', 150);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20018, 11, '사용자 인터뷰와 니즈 발굴', '편향 없는 질문으로 진짜 문제를 찾습니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20019, 11, '디자인 시스템 거버넌스', '컴포넌트 변경 절차와 버전 관리를 정립합니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'DESIGN', 'ADVANCED', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20020, 11, '자바 문법과 객체지향 첫걸음', '자바 기본 문법과 클래스, 상속, 인터페이스를 예제로 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'BEGINNER', 200);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20021, 11, '상태 관리와 데이터 흐름 설계', '전역 상태와 서버 상태를 구분해 화면 갱신을 예측 가능하게 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'PRACTICAL', 330);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20022, 11, '모바일 앱 성능 튜닝', '렌더링 지연과 메모리 누수를 프로파일러로 찾아 개선합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'ADVANCED', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20023, 11, '리눅스 명령어와 서버 기초', '파일 권한과 프로세스, 로그 확인 등 기본 조작을 익힙니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'DEVOPS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20024, 11, '웹 취약점 실습 OWASP Top 10', 'SQL 인젝션과 XSS 등 대표 취약점을 직접 재현하고 막습니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'PRACTICAL', 390);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20025, 11, '예측 모델을 활용한 수요 분석', '시계열 특성을 반영해 수요를 예측하고 검증합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'ADVANCED', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20026, 11, '데이터 엔지니어링 개요', '수집과 저장, 가공의 전체 흐름을 조망합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20027, 11, '모델 학습과 하이퍼파라미터 튜닝', '교차 검증으로 과적합을 줄이고 성능을 올립니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'AI_ML', 'PRACTICAL', 390);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20028, 11, '플랫폼 제품 전략', '여러 이해관계자가 얽힌 플랫폼의 성장 구조를 설계합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20029, 11, 'UX 디자인 개념 잡기', '사용자 흐름과 정보 구조의 기본을 이해합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20030, 11, 'JPA와 데이터 접근 계층 설계', '연관관계 매핑과 지연 로딩, N+1 문제 해결을 실습합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20031, 11, '웹 접근성과 국제화', '스크린 리더 대응과 다국어 처리를 제품 수준으로 구현합니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'FRONTEND', 'ADVANCED', 480);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20032, 11, 'iOS 앱 개발 입문', 'SwiftUI로 화면을 선언적으로 구성하는 방법을 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'BEGINNER', 210);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20033, 11, '인프라를 코드로 관리하기', 'Terraform으로 클라우드 자원을 선언적으로 관리합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'PRACTICAL', 390);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20034, 11, '제로 트러스트 아키텍처', '경계 대신 검증 기반으로 접근 통제를 재설계합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'ADVANCED', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20035, 11, '파이썬 데이터 분석 입문', '판다스로 데이터를 불러와 정제하고 집계합니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'BEGINNER', 240);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20036, 11, '데이터 모델링과 스타 스키마', '팩트와 디멘션으로 분석하기 좋은 구조를 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'PRACTICAL', 330);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20037, 11, 'LLM 파인튜닝과 평가', '도메인 데이터로 모델을 조정하고 품질을 측정합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'ADVANCED', 720);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20038, 11, '사용자 문제 정의와 가설 세우기', '관찰한 문제를 검증 가능한 가설로 바꿉니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20039, 11, '디자인 핸드오프와 개발 협업', '스펙과 토큰을 정리해 구현 오차를 줄입니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'DESIGN', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20040, 11, '이벤트 기반 아키텍처와 메시지 브로커', '발행과 구독 모델, 중복과 순서 문제를 실제 브로커로 다룹니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'ADVANCED', 660);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20041, 11, '자바스크립트 기본기 다지기', '변수와 함수, 배열 메서드, 비동기 처리를 예제로 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'BEGINNER', 210);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20042, 11, '앱 아키텍처와 상태 관리', 'MVVM으로 화면과 비즈니스 로직을 분리합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'PRACTICAL', 330);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20043, 11, '서비스 메시와 트래픽 제어', '사이드카 프록시로 재시도와 서킷 브레이킹을 적용합니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'DEVOPS', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20044, 11, '암호화 기초와 해시', '대칭키와 공개키, 해시 함수의 쓰임을 구분합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'BEGINNER', 200);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20045, 11, 'A/B 테스트 설계와 해석', '표본 크기와 유의성을 따져 실험 결과를 판단합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'PRACTICAL', 330);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20046, 11, '분산 처리 엔진 튜닝', '파티션과 셔플을 조정해 처리 시간을 줄입니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'ADVANCED', 660);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20047, 11, '파이썬으로 시작하는 데이터 전처리', '결측 처리와 인코딩 등 학습 전 준비를 익힙니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'AI_ML', 'BEGINNER', 210);
-- instructor_id=12: 강좌 10개
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20048, 12, '우선순위 결정과 로드맵 관리', '임팩트와 비용을 견줘 무엇을 먼저 할지 정합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'PRACTICAL', 330);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20049, 12, '브랜드 경험 설계', '접점 전반에서 일관된 인상을 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'ADVANCED', 480);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20050, 12, '데이터베이스와 SQL 입문', '테이블 설계와 SELECT, JOIN을 실습하며 기본 쿼리를 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20051, 12, 'Next.js로 만드는 서버 렌더링 앱', '라우팅과 데이터 페칭, SSR과 정적 생성을 상황에 맞게 씁니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'FRONTEND', 'PRACTICAL', 390);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20052, 12, '대규모 앱 모듈화 전략', '기능별 모듈 분리로 빌드 시간과 팀 간 충돌을 줄입니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20053, 12, 'Git과 협업 워크플로', '브랜치 전략과 코드 리뷰 흐름을 팀 기준으로 정리합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'BEGINNER', 150);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20054, 12, 'API 보안과 접근 제어', '토큰 검증과 권한 모델로 리소스 접근을 통제합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'PRACTICAL', 330);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20055, 12, '인과 추론과 준실험 설계', '무작위 배정이 어려운 상황에서 효과를 추정합니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20056, 12, '파이썬으로 데이터 수집하기', 'API와 크롤링으로 원천 데이터를 모읍니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'BEGINNER', 210);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20057, 12, '자연어 처리 실무', '토큰화와 임베딩으로 텍스트 분류기를 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'PRACTICAL', 360);
-- instructor_id=13: 강좌 47개
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20058, 13, 'B2B SaaS 제품 관리', '계약과 온보딩, 고객 성공까지 고려해 제품을 운영합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'ADVANCED', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20059, 13, '피그마 기본 조작', '프레임과 컴포넌트로 화면을 빠르게 그립니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'BEGINNER', 150);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20060, 13, '테스트 코드로 지키는 서버 품질', 'JUnit과 Mockito로 단위와 통합 테스트를 작성하고 CI에 연결합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'PRACTICAL', 330);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20061, 13, '마이크로 프론트엔드 구성', '여러 팀이 독립 배포하는 프론트엔드 구조를 설계합니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'FRONTEND', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20062, 13, '코틀린 문법 기초', '널 안전성과 확장 함수 등 코틀린 핵심 문법을 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20063, 13, '무중단 배포 전략', '블루그린과 카나리로 서비스 중단 없이 배포합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20064, 13, '클라우드 보안 아키텍처', '계정 분리와 최소 권한으로 클라우드 환경을 설계합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20065, 13, '데이터 시각화 첫걸음', '목적에 맞는 차트를 골라 메시지를 전달합니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'BEGINNER', 150);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20066, 13, 'dbt로 관리하는 변환 로직', 'SQL 변환을 버전 관리하고 테스트합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20067, 13, 'AI 에이전트 설계와 도구 연동', '계획 수립과 도구 호출로 다단계 작업을 수행하게 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20068, 13, '요구사항 문서 작성 기초', '개발자가 바로 착수할 수 있는 명세를 씁니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'BEGINNER', 200);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20069, 13, '인터랙션과 마이크로 애니메이션', '상태 변화를 자연스럽게 전달합니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'DESIGN', 'PRACTICAL', 330);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20070, 13, '대규모 트래픽을 견디는 서버 설계', '부하 분산과 커넥션 풀, 백프레셔로 병목을 제거합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'ADVANCED', 720);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20071, 13, 'Vue.js 입문', '컴포넌트와 양방향 바인딩으로 간단한 할 일 앱을 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'BEGINNER', 200);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20072, 13, '푸시 알림과 백그라운드 작업', '알림 채널과 백그라운드 스케줄링을 실제 앱에 붙입니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20073, 13, 'SRE와 장애 대응 체계', 'SLO와 에러 버짓, 사후 분석 문화를 정착시킵니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'DEVOPS', 'ADVANCED', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20074, 13, '안전한 비밀번호와 계정 관리', '저장 방식과 다중 인증으로 계정 탈취를 막습니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'BEGINNER', 150);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20075, 13, '고객 행동 분석과 코호트', '유입부터 이탈까지 구간별 지표를 나눠 봅니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20076, 13, '실시간 집계와 정확히 한 번 처리', '중복 없이 정확한 집계를 보장하는 방법을 다룹니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20077, 13, '회귀와 분류 모델 이해', '대표 알고리즘의 동작과 평가 지표를 익힙니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'AI_ML', 'BEGINNER', 240);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20078, 13, '실험 기반 제품 개선', '가설을 실험으로 검증하고 결과를 의사결정에 씁니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20079, 13, '복잡한 데이터 화면 설계', '대시보드와 테이블에서 정보를 효과적으로 배치합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20080, 13, 'HTTP와 서버 동작 원리', '요청과 응답, 상태 코드, 헤더가 어떻게 오가는지 이해합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'BEGINNER', 150);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20081, 13, '프론트엔드 테스트 자동화', '컴포넌트 테스트와 E2E 시나리오로 회귀를 막습니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'FRONTEND', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20082, 13, '앱 보안과 데이터 보호', '키 저장소와 통신 암호화로 민감 정보를 지킵니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'ADVANCED', 480);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20083, 13, '클라우드 입문 AWS 기초', 'EC2와 S3, VPC 등 핵심 서비스의 개념을 잡습니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'BEGINNER', 240);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20084, 13, '시크릿 관리와 키 순환', '자격 증명을 안전하게 보관하고 주기적으로 교체합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20085, 13, '대규모 로그 분석과 세그먼트', '수억 건 로그에서 의미 있는 사용자 군을 뽑아냅니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'ADVANCED', 480);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20086, 13, '데이터베이스와 데이터 웨어하우스 차이', '운영계와 분석계의 목적과 구조를 비교합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'BEGINNER', 150);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20087, 13, '이미지 분류 모델 만들기', '전이 학습으로 적은 데이터에서도 성능을 냅니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'PRACTICAL', 330);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20088, 13, '가격 정책과 수익 모델 설계', '과금 구조를 실험하며 수익성을 개선합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'ADVANCED', 480);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20089, 13, '색과 타이포그래피 기초', '가독성과 위계를 만드는 기본 원칙을 익힙니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'DESIGN', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20090, 13, 'REST API 설계와 문서화', '리소스 모델과 오류 응답 규약을 정하고 OpenAPI로 문서화합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20091, 13, '브라우저 렌더링 원리와 디버깅', '레이아웃과 페인트 과정을 이해하고 프로파일러로 병목을 찾습니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'ADVANCED', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20092, 13, '모바일 UI 컴포넌트 다루기', '목록과 탭, 다이얼로그 등 기본 컴포넌트를 조합합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'BEGINNER', 150);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20093, 13, '로그 수집과 중앙화', '구조화 로그를 모아 검색 가능한 형태로 저장합니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'DEVOPS', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20094, 13, '모의 해킹과 취약점 진단', '시나리오 기반 진단으로 실제 위험도를 평가합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'ADVANCED', 660);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20095, 13, '지표 설계와 KPI 이해', '무엇을 측정할지 정하고 지표 정의를 문서화합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20096, 13, '스트리밍 데이터 수집', 'Kafka로 이벤트를 받아 실시간으로 적재합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20097, 13, '모델 경량화와 추론 최적화', '양자화와 배치 추론으로 비용과 지연을 줄입니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'AI_ML', 'ADVANCED', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20098, 13, '애자일 협업 기초', '스프린트와 회고로 팀의 리듬을 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'BEGINNER', 150);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20099, 13, '접근성을 고려한 화면 설계', '대비와 포커스 순서로 누구나 쓰게 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20100, 13, '분산 트랜잭션과 데이터 일관성', '사가 패턴과 아웃박스로 서비스 간 정합성을 맞춥니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'ADVANCED', 660);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20101, 13, '타입스크립트 시작하기', '타입 주석과 인터페이스로 자바스크립트 코드를 안전하게 만듭니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'FRONTEND', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20102, 13, '앱 배포와 스토어 심사 대응', '서명과 빌드 변형, 심사 리젝 대응 절차를 정리합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20103, 13, '대규모 인프라 비용 최적화', '자원 사용 패턴을 분석해 낭비를 줄입니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'ADVANCED', 480);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20104, 13, '개인정보 보호와 컴플라이언스 입문', '수집과 보관, 파기 절차의 기본 규칙을 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'BEGINNER', 180);
-- instructor_id=14: 강좌 26개
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20105, 14, 'BI 도구로 만드는 운영 대시보드', '자동 갱신되는 대시보드로 지표를 공유합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'PRACTICAL', 390);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20106, 14, '데이터 카탈로그와 계보 관리', '데이터 출처와 영향 범위를 추적합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'ADVANCED', 480);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20107, 14, '프롬프트 엔지니어링 기초', '목적에 맞는 지시문 작성법을 예제로 익힙니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20108, 14, '그로스 전략과 퍼널 개선', '유입부터 전환까지 단계별 이탈을 줄입니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'PRODUCT', 'PRACTICAL', 390);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20109, 14, '디자인 리서치 심화', '정성과 정량을 결합해 인사이트를 도출합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'ADVANCED', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20110, 14, 'Node.js 백엔드 시작하기', 'Express로 라우팅과 미들웨어를 구성해 첫 서버를 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'BEGINNER', 210);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20111, 14, '디자인 시스템을 코드로 구현하기', '재사용 컴포넌트와 토큰으로 일관된 UI를 유지합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20112, 14, '크로스플랫폼 네이티브 연동', '플랫폼 채널로 네이티브 기능을 직접 호출합니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'MOBILE', 'ADVANCED', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20113, 14, '네트워크 기초와 DNS', 'IP와 포트, 도메인 이름 해석 과정을 이해합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20114, 14, '보안 코드 리뷰 실무', '위험한 패턴을 찾아내는 리뷰 체크리스트를 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'SECURITY', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20115, 14, '지표 체계 설계와 데이터 거버넌스', '조직 전체가 같은 정의를 쓰도록 지표를 정리합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'ADVANCED', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20116, 14, '배치 처리 기본 개념', '스케줄과 멱등성 등 배치 설계의 기본을 익힙니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20117, 14, '벡터 검색과 임베딩 활용', '문서를 벡터로 바꿔 의미 기반 검색을 구현합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20118, 14, '조직 확장과 제품 운영 체계', '팀이 커져도 흔들리지 않는 운영 규칙을 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'PRODUCT', 'ADVANCED', 540);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20119, 14, '와이어프레임과 프로토타입', '아이디어를 빠르게 화면으로 옮겨 검증합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'BEGINNER', 200);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20120, 14, '트랜잭션과 동시성 제어', '격리 수준과 락을 이해하고 재고 차감 같은 경쟁 상황을 다룹니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'BACKEND', 'PRACTICAL', 390);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20121, 14, '실시간 협업 기능 구현', '웹소켓과 충돌 해결 전략으로 동시 편집을 구현합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'FRONTEND', 'ADVANCED', 660);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20122, 14, 'React Native 시작하기', '자바스크립트로 iOS와 안드로이드 화면을 함께 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'MOBILE', 'BEGINNER', 240);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20123, 14, '모니터링과 알림 구성', '지표를 수집해 대시보드와 임계값 알림을 만듭니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DEVOPS', 'PRACTICAL', 360);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20124, 14, '보안 아키텍처 위협 모델링', '설계 단계에서 공격 경로를 식별하고 대응책을 세웁니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'SECURITY', 'ADVANCED', 480);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20125, 14, '스프레드시트 데이터 정리 실무', '피벗과 함수로 반복 집계를 자동화합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ANALYSIS', 'BEGINNER', 150);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20126, 14, '데이터 파이프라인 모니터링', '지연과 실패를 감지해 재처리 기준을 세웁니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DATA_ENGINEERING', 'PRACTICAL', 300);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20127, 14, '추천 시스템 고도화', '실시간 피드백을 반영해 추천 품질을 개선합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'AI_ML', 'ADVANCED', 600);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20128, 14, '제품 지표 읽는 법', '활성 사용자와 리텐션 지표를 해석합니다.', 'PRIVATE', CURRENT_TIMESTAMP, 'PRODUCT', 'BEGINNER', 180);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20129, 14, '서비스 흐름 개선과 정보 구조 재설계', '화면 이동을 단순화해 이탈을 줄입니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'DESIGN', 'PRACTICAL', 390);
INSERT INTO courses (id, instructor_id, title, description, status, created_at, category, difficulty, duration_minutes) VALUES (20130, 14, '도메인 주도 설계 실전', '애그리거트와 바운디드 컨텍스트로 복잡한 도메인을 정리합니다.', 'PUBLIC', CURRENT_TIMESTAMP, 'BACKEND', 'ADVANCED', 600);

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
-- 2026-08-18: 강좌마다 강의 3개 + 미션 1개를 부여한다.
-- courses에 status='PUBLIC'을 직접 INSERT하는 방식이라 Course.publish()의 "강의와 미션을 1개 이상 포함해야
-- 공개할 수 있습니다" 검증을 거치지 않았고, 그 결과 시드 강좌는 강의도 미션도 없는 상태였다.
-- 이 때문에 PRIVATE 시드 강좌를 API로 공개할 수 없어 course.published 이벤트를 시드만으로 트리거하지 못했다.
INSERT INTO lectures (id, course_id, title, status, content_url, content_type, sort_order, created_at)
SELECT (c.id * 10) + r.x, c.id, CONCAT(c.title, ' - ', r.x, '강'), 'PUBLIC',
       CONCAT('https://cdn.example.com/lectures/', c.id, '/', r.x, '.mp4'), 'mp4',
       r.x, CURRENT_TIMESTAMP
FROM courses c CROSS JOIN SYSTEM_RANGE(1, 3) r;

INSERT INTO missions (id, course_id, title, status, content, sort_order, created_at)
SELECT (c.id * 10) + 4, c.id, CONCAT(c.title, ' 실습 과제'), 'PUBLIC',
       CONCAT(c.title, '에서 배운 내용을 적용한 결과물을 제출하세요.'), 4, CURRENT_TIMESTAMP
FROM courses c;

-- 위에서 쓴 id는 (강좌 id * 10 + 1~4) 규칙이라 최대 300304다. 그보다 위에서 auto-increment를 시작한다.
ALTER TABLE lectures ALTER COLUMN id RESTART WITH 400000;
ALTER TABLE missions ALTER COLUMN id RESTART WITH 400000;
