PRAGMA foreign_keys = ON;

DROP TABLE IF EXISTS paypal_subscriptions;
DROP TABLE IF EXISTS entitlements;
DROP TABLE IF EXISTS plans;

CREATE TABLE plans (
    plan_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    price_usd TEXT NOT NULL DEFAULT '0.00',
    duration_days INTEGER,
    billing_interval TEXT NOT NULL DEFAULT 'NONE',
    description TEXT NOT NULL DEFAULT '',
    includes TEXT NOT NULL DEFAULT '[]',
    paypal_product_id TEXT,
    paypal_plan_id TEXT,
    active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE entitlements (
    firebase_uid TEXT PRIMARY KEY,
    plan TEXT NOT NULL DEFAULT 'FREE',
    starts_at INTEGER,
    expires_at INTEGER,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    source TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_at INTEGER NOT NULL,
    last_subscription_id TEXT
);

CREATE TABLE paypal_subscriptions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    firebase_uid TEXT NOT NULL,
    subscription_id TEXT NOT NULL UNIQUE,
    plan_id TEXT NOT NULL,
    paypal_plan_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'APPROVAL_PENDING',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    approved_at INTEGER,
    current_period_end INTEGER,
    next_billing_time INTEGER,
    payer_id TEXT
);

CREATE INDEX idx_entitlements_plan
    ON entitlements(plan);

CREATE INDEX idx_entitlements_status
    ON entitlements(status);

CREATE INDEX idx_paypal_subscriptions_uid
    ON paypal_subscriptions(firebase_uid);

CREATE INDEX idx_paypal_subscriptions_status
    ON paypal_subscriptions(status);

INSERT INTO plans (
    plan_id,
    name,
    price_usd,
    duration_days,
    billing_interval,
    description,
    includes,
    paypal_product_id,
    paypal_plan_id,
    active,
    created_at,
    updated_at
) VALUES (
    'FREE',
    'Free',
    '0.00',
    NULL,
    'NONE',
    'Acesso gratuito às ferramentas disponíveis no plano Free.',
    '["Ferramentas Free","Recursos gratuitos do ToolNexa"]',
    NULL,
    NULL,
    1,
    unixepoch(),
    unixepoch()
);

INSERT INTO plans (
    plan_id,
    name,
    price_usd,
    duration_days,
    billing_interval,
    description,
    includes,
    paypal_product_id,
    paypal_plan_id,
    active,
    created_at,
    updated_at
) VALUES (
    'PRO',
    'Pro',
    '5.00',
    30,
    'MONTH',
    'Mais ferramentas e recursos do ToolNexa por assinatura mensal.',
    '["Tudo do Free","Ferramentas Pro","Novos recursos Pro"]',
    NULL,
    NULL,
    1,
    unixepoch(),
    unixepoch()
);

-- O Free não precisa de PayPal.
-- O Pro começa com os IDs do PayPal vazios.
-- O Worker cria automaticamente o produto e o plano mensal
-- no PayPal Sandbox na primeira tentativa de assinatura e
-- grava os IDs gerados em paypal_product_id e paypal_plan_id.
-- Não é necessário preencher um P-... manualmente.

SELECT
    plan_id,
    name,
    price_usd,
    duration_days,
    billing_interval,
    paypal_product_id,
    paypal_plan_id,
    active
FROM plans
ORDER BY
    CASE plan_id
        WHEN 'FREE' THEN 1
        WHEN 'PRO' THEN 2
        ELSE 3
    END;
