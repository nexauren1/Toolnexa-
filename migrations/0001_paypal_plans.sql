CREATE TABLE IF NOT EXISTS plans (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  price_usd REAL NOT NULL DEFAULT 0,
  interval TEXT NOT NULL,
  active INTEGER NOT NULL DEFAULT 1,
  created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS paypal_orders (
  id TEXT PRIMARY KEY,
  plan_id TEXT NOT NULL,
  paypal_order_id TEXT UNIQUE NOT NULL,
  status TEXT NOT NULL,
  amount_usd REAL NOT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS paypal_subscriptions (
  id TEXT PRIMARY KEY,
  plan_id TEXT NOT NULL,
  paypal_subscription_id TEXT UNIQUE NOT NULL,
  status TEXT NOT NULL,
  created_at INTEGER NOT NULL,
  updated_at INTEGER NOT NULL
);

INSERT OR IGNORE INTO plans
  (id, name, price_usd, interval, active, created_at)
VALUES
  ("free", "Free", 0, "month", 1, unixepoch());

INSERT OR IGNORE INTO plans
  (id, name, price_usd, interval, active, created_at)
VALUES
  ("pro", "Pro", 5, "month", 1, unixepoch());
