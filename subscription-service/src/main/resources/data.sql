-- Demo subscription for local H2 API verification.
INSERT INTO subscriptions (
    id,
    member_id,
    price,
    parent_id,
    subscription_start_at,
    generation,
    valid_until,
    activated_at,
    suspended_at,
    cancelled_at,
    created_at,
    updated_at
) VALUES (
    1,
    1,
    0,
    0,
    CURRENT_TIMESTAMP,
    1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP),
    CURRENT_TIMESTAMP,
    NULL,
    NULL,
    CURRENT_TIMESTAMP,
    NULL
);

ALTER TABLE subscriptions ALTER COLUMN id RESTART WITH 1000;

-- 아래부터는 E2E/수동 테스트용 시드 데이터.
-- 근거/시나리오: subscription-service/.claude/task/subscription-data.sql, subscription-scenario.md 참고.
-- 환불 대상 판정(SubscriptionService.isEligibleForRefund): price>0 AND 해당 회원의 유료 구독이 정확히 1건 AND
--   유효(activated && !suspended && !expired) AND activated_at 기준 14일(REFUND_PERIOD_DAYS) 이내

-- 2001) member_id=502, 무료 구독 이미 보유 -> member.registered 이벤트의 멱등성(이미 있으면 재생성 안 함) 검증용
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    2001, 502, 0, 0, CURRENT_TIMESTAMP, 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, NULL, NULL, CURRENT_TIMESTAMP
);

-- 2002) member_id=503, 활성 유료 구독 -> member.suspended 이벤트로 suspended_at이 채워져야 함
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    2002, 503, 9900, 0, DATEADD('DAY', -10, CURRENT_TIMESTAMP), 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), DATEADD('DAY', -10, CURRENT_TIMESTAMP), NULL, NULL, CURRENT_TIMESTAMP
);

-- 2003) member_id=504, 활성 유료 구독 1건, activated_at 5일 전(14일 이내) -> 환불 대상(핵심 버그 재현용)
--   올바른 동작(processMemberWithdrawal): requestRefund() 호출, suspended_at은 채워지면 안 됨(환불 요청 상태)
--   현재 버그(suspendActiveSubscriptions 오호출): suspended_at이 그냥 채워짐
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    2003, 504, 9900, 0, DATEADD('DAY', -5, CURRENT_TIMESTAMP), 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), DATEADD('DAY', -5, CURRENT_TIMESTAMP), NULL, NULL, CURRENT_TIMESTAMP
);

-- 2004) member_id=505, 무료 구독 -> 환불 대상 아님(price=0) -> withdrawn 시 양쪽 경로(정지/올바른 처리) 결과 동일해야 하는 대조군
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    2004, 505, 0, 0, CURRENT_TIMESTAMP, 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, NULL, NULL, CURRENT_TIMESTAMP
);

-- 2005) member_id=506, 유료 구독이지만 activated_at 20일 전(14일 초과) -> 환불 기간 지남 -> 대조군(단순 정지가 맞는 케이스)
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    2005, 506, 9900, 0, DATEADD('DAY', -20, CURRENT_TIMESTAMP), 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), DATEADD('DAY', -20, CURRENT_TIMESTAMP), NULL, NULL, CURRENT_TIMESTAMP
);

-- member_id=501은 의도적으로 시드하지 않는다 -> member.registered 이벤트의 "신규 생성" 정상 케이스(구독 부재) 검증용


-- 2026-07-21: E2E 연관관계 테스트용 구독권 10건. member_id=20~29는 member-service의
-- 실제 시드 회원(member-service/src/main/resources/data.sql, E2E 연관관계용 추가분)을 그대로 참조한다.
-- price=0(무료)/9900(유료) 혼합, activated_at을 다양하게 배치해 정지/탈퇴(환불 대상 여부) 시나리오를 폭넓게 커버한다.

INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    5000, 20, 0, 0, CURRENT_TIMESTAMP, 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, NULL, NULL, CURRENT_TIMESTAMP
);
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    5001, 21, 9900, 0, DATEADD('DAY', -5, CURRENT_TIMESTAMP), 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), DATEADD('DAY', -5, CURRENT_TIMESTAMP), NULL, NULL, CURRENT_TIMESTAMP
);
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    5002, 22, 9900, 0, DATEADD('DAY', -20, CURRENT_TIMESTAMP), 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), DATEADD('DAY', -20, CURRENT_TIMESTAMP), NULL, NULL, CURRENT_TIMESTAMP
);
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    5003, 23, 9900, 0, DATEADD('DAY', -10, CURRENT_TIMESTAMP), 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), DATEADD('DAY', -10, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, NULL, CURRENT_TIMESTAMP
);
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    5004, 24, 0, 0, CURRENT_TIMESTAMP, 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, NULL, NULL, CURRENT_TIMESTAMP
);
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    5005, 25, 9900, 0, DATEADD('DAY', -3, CURRENT_TIMESTAMP), 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), DATEADD('DAY', -3, CURRENT_TIMESTAMP), NULL, NULL, CURRENT_TIMESTAMP
);
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    5006, 26, 9900, 0, DATEADD('DAY', -30, CURRENT_TIMESTAMP), 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), DATEADD('DAY', -30, CURRENT_TIMESTAMP), NULL, NULL, CURRENT_TIMESTAMP
);
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    5007, 27, 0, 0, CURRENT_TIMESTAMP, 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, NULL, NULL, CURRENT_TIMESTAMP
);
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    5008, 28, 9900, 0, DATEADD('DAY', -1, CURRENT_TIMESTAMP), 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), DATEADD('DAY', -1, CURRENT_TIMESTAMP), NULL, NULL, CURRENT_TIMESTAMP
);
INSERT INTO subscriptions (
    id, member_id, price, parent_id, subscription_start_at, generation,
    valid_until, activated_at, suspended_at, cancelled_at, created_at
) VALUES (
    5009, 29, 9900, 0, DATEADD('DAY', -13, CURRENT_TIMESTAMP), 1,
    DATEADD('MONTH', 1, CURRENT_TIMESTAMP), DATEADD('DAY', -13, CURRENT_TIMESTAMP), NULL, NULL, CURRENT_TIMESTAMP
);

-- 기존 2001~2005 및 이번 5000~5009 시드 이후 auto-increment는 겹치지 않도록 재조정
ALTER TABLE subscriptions ALTER COLUMN id RESTART WITH 6000;