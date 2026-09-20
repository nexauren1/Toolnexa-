PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS plans (
  code TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  price_usd TEXT NOT NULL,
  billing_interval TEXT NOT NULL,
  paypal_plan_id TEXT,
  active INTEGER NOT NULL DEFAULT 1,
  sort_order INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS paypal_orders (
  paypal_order_id TEXT PRIMARY KEY,
  plan_code TEXT,
  status TEXT,
  amount_usd TEXT,
  currency TEXT NOT NULL DEFAULT 'USD',
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  raw_json TEXT
);

CREATE TABLE IF NOT EXISTS paypal_subscriptions (
  paypal_subscription_id TEXT PRIMARY KEY,
  plan_code TEXT NOT NULL,
  firebase_uid TEXT,
  status TEXT,
  payer_email TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  raw_json TEXT
);

CREATE TABLE IF NOT EXISTS paypal_webhook_events (
  event_id TEXT PRIMARY KEY,
  event_type TEXT NOT NULL,
  verified INTEGER NOT NULL DEFAULT 0,
  received_at TEXT NOT NULL,
  raw_json TEXT NOT NULL
);

INSERT OR IGNORE INTO plans
  (code, name, price_usd, billing_interval, paypal_plan_id, active, sort_order)
VALUES
  ('free', 'Free', '0.00', 'month', NULL, 1, 1);

INSERT OR IGNORE INTO plans
  (code, name, price_usd, billing_interval, paypal_plan_id, active, sort_order)
VALUES
  ('pro', 'Pro', '5.00', 'month', NULL, 1, 2);
