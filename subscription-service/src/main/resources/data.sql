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
