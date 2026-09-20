const PAYPAL_BASE =
  "https://api-m.sandbox.paypal.com";

const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store"
};

let firebaseKeysCache = null;
let firebaseKeysFetchedAt = 0;

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: corsHeaders()
      });
    }

    try {
      if (
        url.pathname === "/" ||
        url.pathname === "/api/health"
      ) {
        return json({
          ok: true,
          service: "ToolNexa API",
          worker: "toolnexa",
          paypal: env.PAYPAL_ENV || "sandbox",
          workers_ai: !!env.AI,
          images_binding: !!env.IMAGES,
          database_binding: !!env.DB,
          paypal_client_configured:
            !!(
              env.PAYPAL_CLIENT_ID &&
              env.PAYPAL_CLIENT_SECRET
            ),
          timestamp: new Date().toISOString()
        });
      }

      if (
        url.pathname === "/api/plans" &&
        request.method === "GET"
      ) {
        return listPlans(
          env
        );
      }

      if (
        url.pathname ===
          "/api/paypal/return" &&
        request.method === "GET"
      ) {
        return paypalReturnPage(
          url
        );
      }

      if (
        url.pathname ===
          "/api/paypal/cancel" &&
        request.method === "GET"
      ) {
        return paypalCancelPage();
      }

      if (
        url.pathname === "/api/account" &&
        request.method === "GET"
      ) {
        const user =
          await requireFirebaseUser(
            request,
            env
          );

        return entitlement(
          env,
          user
        );
      }

      if (
        url.pathname ===
          "/api/tools/background-remover" &&
        request.method === "POST"
      ) {
        await requireFirebaseUser(
          request,
          env
        );

        return backgroundRemover(
          request,
          env
        );
      }

      if (
        url.pathname ===
          "/api/tools/background-remover/ai-background" &&
        request.method === "POST"
      ) {
        const user =
          await requireFirebaseUser(
            request,
            env
          );

        return aiBackground(
          request,
          env,
          user
        );
      }

      if (
        url.pathname ===
          "/api/entitlement" &&
        request.method === "GET"
      ) {
        const user =
          await requireFirebaseUser(
            request,
            env
          );

        return entitlement(
          env,
          user
        );
      }

      if (
        url.pathname ===
          "/api/paypal/create-subscription" &&
        request.method === "POST"
      ) {
        const user =
          await requireFirebaseUser(
            request,
            env
          );

        return createSubscription(
          request,
          env,
          user
        );
      }

      if (
        url.pathname ===
          "/api/paypal/activate-subscription" &&
        request.method === "POST"
      ) {
        const user =
          await requireFirebaseUser(
            request,
            env
          );

        return activateSubscription(
          request,
          env,
          user
        );
      }

      return json(
        {
          ok: false,
          error: "not_found"
        },
        404
      );
    } catch (error) {
      console.error(
        "ToolNexa Worker error",
        error
      );

      return json(
        {
          ok: false,
          error:
            error?.code ||
            "internal_error",
          message:
            error?.publicMessage ||
            "Ocorreu um erro no servidor."
        },
        error?.status || 500
      );
    }
  }
};

function corsHeaders() {
  return {
    "access-control-allow-origin": "*",
    "access-control-allow-methods":
      "GET,POST,OPTIONS",
    "access-control-allow-headers":
      "Authorization,Content-Type"
  };
}

function json(data, status = 200) {
  return new Response(
    JSON.stringify(data),
    {
      status,
      headers: {
        ...JSON_HEADERS,
        ...corsHeaders()
      }
    }
  );
}

async function ensureBillingSchema(db) {
  if (!db) {
    const error =
      new Error(
        "D1 billing database is missing."
      );
    error.code =
      "billing_database_missing";
    error.publicMessage =
      "O banco de billing do ToolNexa não está ligado ao Worker.";
    error.status = 503;
    throw error;
  }

  await db.batch([
    db.prepare(
      "CREATE TABLE IF NOT EXISTS plans (" +
      "plan_id TEXT PRIMARY KEY, name TEXT NOT NULL, " +
      "price_usd TEXT NOT NULL DEFAULT '0.00', duration_days INTEGER, " +
      "billing_interval TEXT NOT NULL DEFAULT 'NONE', description TEXT NOT NULL DEFAULT '', " +
      "includes TEXT NOT NULL DEFAULT '[]', paypal_product_id TEXT, paypal_plan_id TEXT, " +
      "active INTEGER NOT NULL DEFAULT 1, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)"
    ),
    db.prepare(
      "CREATE TABLE IF NOT EXISTS entitlements (" +
      "firebase_uid TEXT PRIMARY KEY, plan TEXT NOT NULL DEFAULT 'FREE', starts_at INTEGER, " +
      "expires_at INTEGER, status TEXT NOT NULL DEFAULT 'ACTIVE', source TEXT NOT NULL DEFAULT 'SYSTEM', " +
      "updated_at INTEGER NOT NULL, last_subscription_id TEXT)"
    ),
    db.prepare(
      "CREATE TABLE IF NOT EXISTS paypal_subscriptions (" +
      "id INTEGER PRIMARY KEY AUTOINCREMENT, firebase_uid TEXT NOT NULL, " +
      "subscription_id TEXT NOT NULL UNIQUE, plan_id TEXT NOT NULL, paypal_plan_id TEXT NOT NULL, " +
      "status TEXT NOT NULL DEFAULT 'APPROVAL_PENDING', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, " +
      "approved_at INTEGER, current_period_end INTEGER, next_billing_time INTEGER, payer_id TEXT)"
    )
  ]);

  const planColumns =
    await tableColumns(
      db,
      "plans"
    );

  if (
    planColumns.has("code") &&
    !planColumns.has("plan_id")
  ) {
    await rebuildLegacyPlans(
      db
    );
  }

  const subscriptionColumns =
    await tableColumns(
      db,
      "paypal_subscriptions"
    );

  if (
    subscriptionColumns.has(
      "paypal_subscription_id"
    ) &&
    !subscriptionColumns.has(
      "subscription_id"
    )
  ) {
    await rebuildLegacySubscriptions(
      db
    );
  }

  await db.batch([
    db.prepare(
      "CREATE INDEX IF NOT EXISTS idx_entitlements_plan " +
      "ON entitlements(plan)"
    ),
    db.prepare(
      "CREATE INDEX IF NOT EXISTS idx_entitlements_status " +
      "ON entitlements(status)"
    ),
    db.prepare(
      "CREATE INDEX IF NOT EXISTS idx_paypal_subscriptions_uid " +
      "ON paypal_subscriptions(firebase_uid)"
    ),
    db.prepare(
      "CREATE INDEX IF NOT EXISTS idx_paypal_subscriptions_status " +
      "ON paypal_subscriptions(status)"
    )
  ]);

  const now =
    nowSeconds();

  const definitions = {
    FREE: {
      name: "Free",
      priceUsd: "0.00",
      durationDays: null,
      billingInterval: "NONE",
      description:
        "Acesso gratuito às ferramentas disponíveis no plano Free.",
      includes: [
        "Ferramentas Free",
        "Recursos gratuitos do ToolNexa"
      ]
    },
    PRO: {
      name: "Pro",
      priceUsd: "5.00",
      durationDays: 30,
      billingInterval: "MONTH",
      description:
        "Mais ferramentas e recursos do ToolNexa por assinatura mensal.",
      includes: [
        "Tudo do Free",
        "Ferramentas Pro",
        "Novos recursos Pro"
      ]
    }
  };

  for (
    const [planId, definition]
    of Object.entries(
      definitions
    )
  ) {
    await db.prepare(
      "INSERT OR IGNORE INTO plans (" +
      "plan_id, name, price_usd, duration_days, billing_interval, description, " +
      "includes, active, created_at, updated_at) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?)"
    )
      .bind(
        planId,
        definition.name,
        definition.priceUsd,
        definition.durationDays,
        definition.billingInterval,
        definition.description,
        JSON.stringify(
          definition.includes
        ),
        now,
        now
      )
      .run();

    await db.prepare(
      "UPDATE plans SET name = ?, price_usd = ?, duration_days = ?, billing_interval = ?, " +
      "description = ?, includes = ?, active = 1, updated_at = ? WHERE plan_id = ?"
    )
      .bind(
        definition.name,
        definition.priceUsd,
        definition.durationDays,
        definition.billingInterval,
        definition.description,
        JSON.stringify(
          definition.includes
        ),
        now,
        planId
      )
      .run();
  }
}

async function tableColumns(
  db,
  tableName
) {
  const result =
    await db.prepare(
      "PRAGMA table_info(" +
      tableName +
      ")"
    ).all();

  return new Set(
    (result.results || [])
      .map(
        row =>
          row.name
      )
  );
}

async function rebuildLegacyPlans(
  db
) {
  const now =
    nowSeconds();

  await db.prepare(
    "CREATE TABLE plans_v2 (" +
    "plan_id TEXT PRIMARY KEY, name TEXT NOT NULL, price_usd TEXT NOT NULL DEFAULT '0.00', " +
    "duration_days INTEGER, billing_interval TEXT NOT NULL DEFAULT 'NONE', " +
    "description TEXT NOT NULL DEFAULT '', includes TEXT NOT NULL DEFAULT '[]', " +
    "paypal_product_id TEXT, paypal_plan_id TEXT, active INTEGER NOT NULL DEFAULT 1, " +
    "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL)"
  ).run();

  await db.prepare(
    "INSERT INTO plans_v2 (" +
    "plan_id, name, price_usd, billing_interval, paypal_plan_id, active, created_at, updated_at) " +
    "SELECT UPPER(code), name, price_usd, billing_interval, paypal_plan_id, active, ?, ? FROM plans"
  )
    .bind(
      now,
      now
    )
    .run();

  await db.prepare(
    "DROP TABLE plans"
  ).run();

  await db.prepare(
    "ALTER TABLE plans_v2 RENAME TO plans"
  ).run();
}

async function rebuildLegacySubscriptions(
  db
) {
  const now =
    nowSeconds();

  await db.prepare(
    "CREATE TABLE paypal_subscriptions_v2 (" +
    "id INTEGER PRIMARY KEY AUTOINCREMENT, firebase_uid TEXT NOT NULL, subscription_id TEXT NOT NULL UNIQUE, " +
    "plan_id TEXT NOT NULL, paypal_plan_id TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'APPROVAL_PENDING', " +
    "created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, approved_at INTEGER, " +
    "current_period_end INTEGER, next_billing_time INTEGER, payer_id TEXT)"
  ).run();

  await db.prepare(
    "INSERT INTO paypal_subscriptions_v2 (" +
    "firebase_uid, subscription_id, plan_id, paypal_plan_id, status, created_at, updated_at) " +
    "SELECT s.firebase_uid, s.paypal_subscription_id, UPPER(s.plan_code), " +
    "COALESCE(p.paypal_plan_id, ''), COALESCE(s.status, 'APPROVAL_PENDING'), " +
    "COALESCE(unixepoch(s.created_at), ?), COALESCE(unixepoch(s.updated_at), ?) " +
    "FROM paypal_subscriptions s LEFT JOIN plans p ON UPPER(p.plan_id) = UPPER(s.plan_code)"
  )
    .bind(
      now,
      now
    )
    .run();

  await db.prepare(
    "DROP TABLE paypal_subscriptions"
  ).run();

  await db.prepare(
    "ALTER TABLE paypal_subscriptions_v2 RENAME TO paypal_subscriptions"
  ).run();
}

async function ensureEntitlement(
  db,
  uid
) {
  const existing =
    await db.prepare(
      "SELECT firebase_uid, plan, starts_at, expires_at, status, source, updated_at, " +
      "last_subscription_id FROM entitlements WHERE firebase_uid = ?"
    )
      .bind(
        uid
      )
      .first();

  if (
    existing
  ) {
    return existing;
  }

  const timestamp =
    nowSeconds();

  await db.prepare(
    "INSERT OR IGNORE INTO entitlements (" +
    "firebase_uid, plan, starts_at, expires_at, status, source, updated_at, last_subscription_id) " +
    "VALUES (?, 'FREE', ?, NULL, 'ACTIVE', 'SYSTEM', ?, NULL)"
  )
    .bind(
      uid,
      timestamp,
      timestamp
    )
    .run();

  return db.prepare(
    "SELECT firebase_uid, plan, starts_at, expires_at, status, source, updated_at, last_subscription_id " +
    "FROM entitlements WHERE firebase_uid = ?"
  )
    .bind(
      uid
    )
    .first();
}

function planRank(
  plan
) {
  return String(
    plan ||
    "FREE"
  ).toUpperCase() === "PRO"
    ? 1
    : 0;
}

function hasPlanAccess(
  currentPlan,
  requiredPlan
) {
  return (
    planRank(
      currentPlan
    ) >=
    planRank(
      requiredPlan
    )
  );
}

async function getPlan(
  db,
  planId
) {
  return db.prepare(
    "SELECT * FROM plans WHERE plan_id = ? AND active = 1"
  )
    .bind(
      String(
        planId
      ).toUpperCase()
    )
    .first();
}

function parsePlanIncludes(
  value
) {
  try {
    const parsed =
      JSON.parse(
        value ||
        "[]"
      );

    return Array.isArray(
      parsed
    )
      ? parsed
      : [];
  } catch (_) {
    return [];
  }
}

async function updateEntitlementFromSubscription(
  db,
  uid,
  subscription
) {
  const status =
    String(
      subscription?.status ||
      ""
    ).toUpperCase();

  const nextBillingTime =
    isoToSeconds(
      subscription?.billing_info
        ?.next_billing_time
    );

  const startTime =
    isoToSeconds(
      subscription?.start_time
    ) ||
    nowSeconds();

  const payerId =
    subscription?.subscriber
      ?.payer_id ||
    null;

  const local =
    await db.prepare(
      "SELECT plan_id FROM paypal_subscriptions WHERE subscription_id = ? " +
      "AND firebase_uid = ?"
    )
      .bind(
        subscription?.id,
        uid
      )
      .first();

  if (
    !local
  ) {
    return null;
  }

  const now =
    nowSeconds();

  await db.prepare(
    "UPDATE paypal_subscriptions SET status = ?, updated_at = ?, " +
    "approved_at = CASE WHEN ? IN ('APPROVED', 'ACTIVE') THEN COALESCE(approved_at, ?) " +
    "ELSE approved_at END, current_period_end = ?, next_billing_time = ?, payer_id = ? " +
    "WHERE subscription_id = ? AND firebase_uid = ?"
  )
    .bind(
      status,
      now,
      status,
      now,
      nextBillingTime,
      nextBillingTime,
      payerId,
      subscription?.id,
      uid
    )
    .run();

  const active =
    (
      status === "ACTIVE" ||
      status === "APPROVED"
    ) &&
    nextBillingTime !== null &&
    nextBillingTime > now;

  const cancelledWithTime =
    status === "CANCELLED" &&
    nextBillingTime !== null &&
    nextBillingTime > now;

  const entitlement =
    await ensureEntitlement(
      db,
      uid
    );

  if (
    (
      active ||
      cancelledWithTime
    ) &&
    hasPlanAccess(
      local.plan_id,
      "PRO"
    )
  ) {
    await db.prepare(
      "UPDATE entitlements SET plan = ?, starts_at = ?, expires_at = ?, " +
      "status = 'ACTIVE', source = 'PAYPAL', updated_at = ?, last_subscription_id = ? " +
      "WHERE firebase_uid = ?"
    )
      .bind(
        String(
          local.plan_id
        ).toUpperCase(),
        startTime,
        nextBillingTime,
        now,
        subscription?.id,
        uid
      )
      .run();
  } else {
    await db.prepare(
      "UPDATE entitlements SET plan = 'FREE', starts_at = NULL, expires_at = NULL, " +
      "status = 'ACTIVE', source = 'SYSTEM', updated_at = ?, last_subscription_id = ? " +
      "WHERE firebase_uid = ?"
    )
      .bind(
        now,
        subscription?.id ||
          null,
        uid
      )
      .run();
  }

  return db.prepare(
    "SELECT firebase_uid, plan, starts_at, expires_at, status, source, updated_at, last_subscription_id " +
    "FROM entitlements WHERE firebase_uid = ?"
  )
    .bind(
      uid
    )
    .first();
}

async function syncUserSubscription(
  db,
  env,
  uid
) {
  const current =
    await ensureEntitlement(
      db,
      uid
    );

  if (
    !current.last_subscription_id
  ) {
    return current;
  }

  try {
    const accessToken =
      await paypalAccessToken(
        env.PAYPAL_CLIENT_ID,
        env.PAYPAL_CLIENT_SECRET
      );

    const response =
      await fetch(
        PAYPAL_BASE +
          "/v1/billing/subscriptions/" +
          encodeURIComponent(
            current.last_subscription_id
          ),
        {
          method:
            "GET",
          headers: {
            accept:
              "application/json",
            authorization:
              "Bearer " +
              accessToken
          }
        }
      );

    const data =
      await response
        .json()
        .catch(
          () => ({})
        );

    if (
      !response.ok
    ) {
      console.error(
        "PayPal subscription lookup failed",
        response.status,
        data
      );
      return current;
    }

    return (
      await updateEntitlementFromSubscription(
        db,
        uid,
        data
      )
    ) ||
      current;
  } catch (
    error
  ) {
    console.error(
      "PayPal entitlement sync failed",
      error
    );
    return current;
  }
}

async function entitlement(
  env,
  user
) {
  await ensureBillingSchema(
    env.DB
  );

  let current =
    await ensureEntitlement(
      env.DB,
      user.uid
    );

  const now =
    nowSeconds();

  if (
    current.plan !== "FREE" &&
    current.expires_at !== null &&
    Number(
      current.expires_at
    ) <= now
  ) {
    await env.DB.prepare(
      "UPDATE entitlements SET plan = 'FREE', starts_at = NULL, expires_at = NULL, " +
      "status = 'ACTIVE', source = 'SYSTEM', updated_at = ? WHERE firebase_uid = ?"
    )
      .bind(
        now,
        user.uid
      )
      .run();
  }

  current =
    await syncUserSubscription(
      env.DB,
      env,
      user.uid
    );

  const currentPlanCode =
    String(
      current.plan ||
      "FREE"
    ).toUpperCase();

  const plan =
    await getPlan(
      env.DB,
      currentPlanCode
    );

  return json({
    ok:
      true,
    plan:
      currentPlanCode,
    planName:
      plan?.name ||
      (
        currentPlanCode ===
        "PRO"
          ? "Pro"
          : "Free"
      ),
    priceUsd:
      plan?.price_usd ||
      (
        currentPlanCode ===
        "PRO"
          ? "5.00"
          : "0.00"
      ),
    status:
      current.status ||
      "ACTIVE",
    isPro:
      hasPlanAccess(
        currentPlanCode,
        "PRO"
      ) &&
      current.status ===
        "ACTIVE",
    expiresAt:
      current.expires_at
        ? Number(
            current.expires_at
          )
        : null,
    subscriptionId:
      current.last_subscription_id ||
      null
  });
}

async function listPlans(
  env
) {
  await ensureBillingSchema(
    env.DB
  );

  const result =
    await env.DB
      .prepare(
        "SELECT plan_id, name, price_usd, duration_days, billing_interval, description, " +
        "includes, paypal_product_id, paypal_plan_id, active FROM plans " +
        "WHERE active = 1 ORDER BY CASE plan_id WHEN 'FREE' THEN 1 WHEN 'PRO' THEN 2 ELSE 3 END"
      )
      .all();

  return json({
    ok:
      true,
    plans:
      (result.results || [])
        .map(
          plan => ({
            id:
              plan.plan_id,
            name:
              plan.name,
            priceUsd:
              Number(
                plan.price_usd
              ),
            durationDays:
              plan.duration_days === null
                ? null
                : Number(
                    plan.duration_days
                  ),
            billingInterval:
              plan.billing_interval,
            description:
              plan.description,
            includes:
              parsePlanIncludes(
                plan.includes
              ),
            active:
              Number(
                plan.active
              ) === 1,
            hasPayPalPlan:
              Boolean(
                plan.paypal_plan_id
              )
          })
        )
  });
}

async function createSubscription(
  request,
  env,
  user
) {
  await ensureBillingSchema(
    env.DB
  );

  const body =
    await request
      .json()
      .catch(
        () => ({})
      );

  const planId =
    typeof body.planId === "string"
      ? body.planId
          .trim()
          .toUpperCase()
      : "PRO";

  if (
    planId !== "PRO"
  ) {
    return json(
      {
        error:
          "Escolha um plano mensal válido."
      },
      400
    );
  }

  const current =
    await syncUserSubscription(
      env.DB,
      env,
      user.uid
    );

  if (
    current.plan !== "FREE" &&
    current.status === "ACTIVE" &&
    Number(
      current.expires_at ||
      0
    ) > nowSeconds()
  ) {
    return json(
      {
        error:
          "Esta conta já tem um plano pago ativo.",
        plan:
          current.plan,
        expiresAt:
          Number(
            current.expires_at
          )
      },
      409
    );
  }

  const plan =
    await getPlan(
      env.DB,
      planId
    );

  if (
    !plan
  ) {
    return json(
      {
        error:
          "Plano não encontrado."
      },
      404
    );
  }

  let paypalPlanId =
    String(
      plan.paypal_plan_id ||
      ""
    ).trim();

  if (!paypalPlanId) {
    paypalPlanId =
      await ensurePayPalBillingPlan(
        env,
        plan
      );
  }

  const accessToken =
    await paypalAccessToken(
      env.PAYPAL_CLIENT_ID,
      env.PAYPAL_CLIENT_SECRET
    );

  const origin =
    new URL(
      request.url
    ).origin;

  const payload = {
    plan_id:
      paypalPlanId,
    ...(user.email
      ? {
          subscriber: {
            email_address:
              user.email
          }
        }
      : {}),
    application_context: {
      brand_name:
        "ToolNexa",
      locale:
        "pt-PT",
      shipping_preference:
        "NO_SHIPPING",
      user_action:
        "SUBSCRIBE_NOW",
      return_url:
        origin +
        "/api/paypal/return",
      cancel_url:
        origin +
        "/api/paypal/cancel"
    }
  };

  const response =
    await fetch(
      PAYPAL_BASE +
        "/v1/billing/subscriptions",
      {
        method:
          "POST",
        headers: {
          authorization:
            "Bearer " +
            accessToken,
          "content-type":
            "application/json",
          accept:
            "application/json",
          "paypal-request-id":
            "toolnexa-subscription-" +
            crypto.randomUUID()
        },
        body:
          JSON.stringify(
            payload
          )
      }
    );

  const data =
    await response
      .json()
      .catch(
        () => ({})
      );

  if (
    !response.ok
  ) {
    console.error(
      "PayPal subscription creation failed",
      response.status,
      data
    );

    return json(
      {
        ok:
          false,
        error:
          "paypal_create_subscription_failed",
        message:
          paypalFailureMessage(
            data,
            "O PayPal Sandbox recusou a criação da assinatura."
          ),
        paypal_error:
          data?.name ||
          null,
        paypal_debug_id:
          data?.debug_id ||
          null,
        details:
          data?.details ||
          []
      },
      502
    );
  }

  const approvalUrl =
    (data.links || [])
      .find(
        link =>
          link.rel ===
          "approve"
      )
      ?.href ||
    null;

  if (
    !data.id ||
    !approvalUrl
  ) {
    return json(
      {
        ok:
          false,
        error:
          "paypal_incomplete_subscription",
        message:
          "O PayPal devolveu uma assinatura incompleta."
      },
      502
    );
  }

  const timestamp =
    nowSeconds();

  await env.DB
    .prepare(
      "INSERT INTO paypal_subscriptions (" +
      "firebase_uid, subscription_id, plan_id, paypal_plan_id, status, created_at, updated_at) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?)"
    )
    .bind(
      user.uid,
      data.id,
      planId,
      paypalPlanId,
      data.status ||
        "APPROVAL_PENDING",
      timestamp,
      timestamp
    )
    .run();

  return json({
    ok:
      true,
    subscriptionId:
      data.id,
    approvalUrl,
    plan:
      planId,
    amount:
      Number(
        plan.price_usd
      ),
    currency:
      "USD",
    billingInterval:
      plan.billing_interval
  });
}

async function paypalAccessToken(
  clientId,
  clientSecret
) {
  if (
    !clientId ||
    !clientSecret
  ) {
    const error =
      new Error(
        "As credenciais do PayPal não estão configuradas."
      );
    error.code =
      "paypal_credentials_missing";
    error.publicMessage =
      "O PayPal Sandbox não está configurado no Worker.";
    error.status = 503;
    throw error;
  }

  const basic =
    btoa(
      clientId +
      ":" +
      clientSecret
    );

  const response =
    await fetch(
      PAYPAL_BASE +
        "/v1/oauth2/token",
      {
        method:
          "POST",
        headers: {
          authorization:
            "Basic " +
            basic,
          "content-type":
            "application/x-www-form-urlencoded",
          accept:
            "application/json"
        },
        body:
          "grant_type=client_credentials"
      }
    );

  const data =
    await response
      .json()
      .catch(
        () => ({})
      );

  if (
    !response.ok ||
    !data.access_token
  ) {
    const error =
      new Error(
        paypalFailureMessage(
          data,
          "Não foi possível autenticar no PayPal Sandbox."
        )
      );
    error.code =
      "paypal_authentication_failed";
    error.publicMessage =
      error.message;
    error.status = 502;
    error.paypalDebugId =
      data?.debug_id ||
      null;
    throw error;
  }

  return data.access_token;
}

async function createPayPalProduct(
  env,
  accessToken,
  plan
) {
  const response =
    await fetch(
      PAYPAL_BASE +
        "/v1/catalogs/products",
      {
        method:
          "POST",
        headers: {
          authorization:
            "Bearer " +
            accessToken,
          "content-type":
            "application/json",
          accept:
            "application/json",
          "paypal-request-id":
            "toolnexa-product-" +
            plan.plan_id.toLowerCase() +
            "-" +
            crypto.randomUUID()
        },
        body:
          JSON.stringify({
            name:
              "ToolNexa " +
              plan.name,
            description:
              plan.description,
            type:
              "SERVICE",
            category:
              "SOFTWARE"
          })
      }
    );

  const data =
    await response
      .json()
      .catch(
        () => ({})
      );

  if (
    !response.ok ||
    !data.id
  ) {
    const error =
      new Error(
        paypalFailureMessage(
          data,
          "Não foi possível criar o produto do ToolNexa no PayPal."
        )
      );
    error.code =
      "paypal_product_creation_failed";
    error.publicMessage =
      error.message;
    error.status = 502;
    error.paypalDebugId =
      data?.debug_id ||
      null;
    throw error;
  }

  return data.id;
}

async function createPayPalBillingPlan(
  accessToken,
  plan,
  productId
) {
  const response =
    await fetch(
      PAYPAL_BASE +
        "/v1/billing/plans",
      {
        method:
          "POST",
        headers: {
          authorization:
            "Bearer " +
            accessToken,
          "content-type":
            "application/json",
          accept:
            "application/json",
          "paypal-request-id":
            "toolnexa-plan-" +
            plan.plan_id.toLowerCase() +
            "-" +
            crypto.randomUUID()
        },
        body:
          JSON.stringify({
            product_id:
              productId,
            name:
              "ToolNexa " +
              plan.name +
              " Monthly",
            description:
              plan.description,
            status:
              "ACTIVE",
            billing_cycles: [
              {
                frequency: {
                  interval_unit:
                    "MONTH",
                  interval_count:
                    1
                },
                tenure_type:
                  "REGULAR",
                sequence:
                  1,
                total_cycles:
                  0,
                pricing_scheme: {
                  fixed_price: {
                    value:
                      Number(
                        plan.price_usd
                      ).toFixed(2),
                    currency_code:
                      "USD"
                  }
                }
              }
            ],
            payment_preferences: {
              auto_bill_outstanding:
                true,
              payment_failure_threshold:
                2
            }
          })
      }
    );

  const data =
    await response
      .json()
      .catch(
        () => ({})
      );

  if (
    !response.ok ||
    !data.id
  ) {
    const error =
      new Error(
        paypalFailureMessage(
          data,
          "Não foi possível criar o plano Pro do ToolNexa no PayPal."
        )
      );
    error.code =
      "paypal_plan_creation_failed";
    error.publicMessage =
      error.message;
    error.status = 502;
    error.paypalDebugId =
      data?.debug_id ||
      null;
    throw error;
  }

  return data.id;
}

async function ensurePayPalBillingPlan(
  env,
  plan
) {
  const savedPlanId =
    String(
      plan.paypal_plan_id ||
      ""
    ).trim();

  if (
    savedPlanId
  ) {
    return savedPlanId;
  }

  const accessToken =
    await paypalAccessToken(
      env.PAYPAL_CLIENT_ID,
      env.PAYPAL_CLIENT_SECRET
    );

  let productId =
    String(
      plan.paypal_product_id ||
      ""
    ).trim();

  if (!productId) {
    productId =
      await createPayPalProduct(
        env,
        accessToken,
        plan
      );

    await env.DB.prepare(
      "UPDATE plans SET " +
      "paypal_product_id = ?, updated_at = ? " +
      "WHERE plan_id = ?"
    )
      .bind(
        productId,
        nowSeconds(),
        plan.plan_id
      )
      .run();
  }

  const paypalPlanId =
    await createPayPalBillingPlan(
      accessToken,
      plan,
      productId
    );

  await env.DB.prepare(
    "UPDATE plans SET " +
    "paypal_plan_id = ?, updated_at = ? " +
    "WHERE plan_id = ?"
  )
    .bind(
      paypalPlanId,
      nowSeconds(),
      plan.plan_id
    )
    .run();

  return paypalPlanId;
}

async function getPayPalSubscription(
  env,
  accessToken,
  subscriptionId
) {
  const response =
    await fetch(
      PAYPAL_BASE +
        "/v1/billing/subscriptions/" +
        encodeURIComponent(
          subscriptionId
        ),
      {
        method:
          "GET",
        headers: {
          authorization:
            "Bearer " +
            accessToken,
          accept:
            "application/json"
        }
      }
    );

  const data =
    await response
      .json()
      .catch(
        () => ({})
      );

  if (
    !response.ok
  ) {
    const error =
      new Error(
        paypalFailureMessage(
          data,
          "Não foi possível verificar a assinatura no PayPal."
        )
      );
    error.code =
      "paypal_subscription_lookup_failed";
    error.publicMessage =
      error.message;
    error.status = 502;
    throw error;
  }

  return data;
}

async function activatePayPalSubscription(
  env,
  accessToken,
  subscriptionId
) {
  const response =
    await fetch(
      PAYPAL_BASE +
        "/v1/billing/subscriptions/" +
        encodeURIComponent(
          subscriptionId
        ) +
        "/activate",
      {
        method:
          "POST",
        headers: {
          authorization:
            "Bearer " +
            accessToken,
          "content-type":
            "application/json",
          accept:
            "application/json"
        },
        body:
          JSON.stringify({
            reason:
              "Customer approved the ToolNexa subscription."
          })
      }
    );

  if (
    response.status ===
      204 ||
    response.ok
  ) {
    return;
  }

  const data =
    await response
      .json()
      .catch(
        () => ({})
      );

  throw new Error(
    paypalFailureMessage(
      data,
      "Não foi possível ativar a assinatura."
    )
  );
}

async function activateSubscription(
  request,
  env,
  user
) {
  await ensureBillingSchema(
    env.DB
  );

  const body =
    await request
      .json()
      .catch(
        () => ({})
      );

  const subscriptionId =
    typeof body.subscriptionId === "string"
      ? body.subscriptionId.trim()
      : "";

  if (
    !subscriptionId
  ) {
    return json(
      {
        error:
          "subscription_id_required"
      },
      400
    );
  }

  const local =
    await env.DB
      .prepare(
        "SELECT * FROM paypal_subscriptions " +
        "WHERE subscription_id = ? AND firebase_uid = ?"
      )
      .bind(
        subscriptionId,
        user.uid
      )
      .first();

  if (
    !local
  ) {
    return json(
      {
        error:
          "subscription_not_found",
        message:
          "A assinatura não foi encontrada para esta conta."
      },
      404
    );
  }

  const accessToken =
    await paypalAccessToken(
      env.PAYPAL_CLIENT_ID,
      env.PAYPAL_CLIENT_SECRET
    );

  let subscription =
    await getPayPalSubscription(
      env,
      accessToken,
      subscriptionId
    );

  if (
    subscription.plan_id !==
    local.paypal_plan_id
  ) {
    return json(
      {
        error:
          "subscription_plan_mismatch",
        message:
          "A assinatura não corresponde ao plano Pro do ToolNexa."
      },
      400
    );
  }

  if (
    subscription.status ===
    "APPROVED"
  ) {
    try {
      await activatePayPalSubscription(
        env,
        accessToken,
        subscriptionId
      );

      subscription =
        await getPayPalSubscription(
          env,
          accessToken,
          subscriptionId
        );
    } catch (
      error
    ) {
      console.error(
        "PayPal activation failed",
        error
      );

      if (
        subscription.status !==
        "ACTIVE"
      ) {
        return json(
          {
            error:
              "paypal_activation_failed",
            message:
              error?.message ||
              "PayPal ainda não ativou a assinatura."
          },
          502
        );
      }
    }
  }

  const updated =
    await updateEntitlementFromSubscription(
      env.DB,
      user.uid,
      subscription
    );

  if (
    !updated
  ) {
    return json(
      {
        error:
          "entitlement_update_failed",
        message:
          "Não foi possível atualizar o acesso do utilizador."
      },
      500
    );
  }

  if (
    updated.plan !==
    local.plan_id
  ) {
    return json(
      {
        ok:
          false,
        error:
          "subscription_not_active",
        message:
          "A assinatura ainda não está ativa.",
        status:
          subscription.status
      },
      409
    );
  }

  return json({
    ok:
      true,
    plan:
      updated.plan,
    status:
      updated.status,
    expiresAt:
      updated.expires_at
        ? Number(
            updated.expires_at
          )
        : null,
    subscriptionId:
      subscriptionId
  });
}

function isoToSeconds(
  value
) {
  if (
    !value
  ) {
    return null;
  }

  const time =
    Date.parse(
      value
    );

  return Number.isFinite(
    time
  )
    ? Math.floor(
        time / 1000
      )
    : null;
}

function paypalFailureMessage(
  payload,
  fallback
) {
  if (
    !payload
  ) {
    return fallback;
  }

  if (
    typeof payload.message ===
      "string" &&
    payload.message.trim()
  ) {
    return payload.message.trim();
  }

  const details =
    Array.isArray(
      payload.details
    )
      ? payload.details
      : [];

  const descriptions =
    details
      .map(
        item =>
          item?.description ||
          item?.issue ||
          ""
      )
      .filter(
        Boolean
      );

  if (
    descriptions.length
  ) {
    return descriptions.join(
      " "
    );
  }

  if (
    typeof payload.name ===
      "string" &&
    payload.name.trim()
  ) {
    return payload.name.trim();
  }

  return fallback;
}

function paypalReturnPage(
  url
) {
  const id =
    url.searchParams.get(
      "subscription_id"
    ) ||
    url.searchParams.get(
      "token"
    ) ||
    "";

  const safeId =
    String(
      id
    ).replace(
      /[^a-zA-Z0-9_-]/g,
      ""
    );

  const deepLink =
    "toolnexa://paypal/complete" +
    "?subscription_id=" +
    encodeURIComponent(
      safeId
    );

  const link =
    safeId
      ? "<a href='" +
        escapeHtml(
          deepLink
        ) +
        "' style='display:inline-block;padding:14px 18px;border-radius:12px;" +
        "background:#2563eb;color:#fff;text-decoration:none'>Voltar ao ToolNexa</a>"
      : "<p>Assinatura não identificada.</p>";

  return new Response(
    "<!doctype html>" +
    "<html lang='pt'><head>" +
    "<meta charset='utf-8'>" +
    "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
    "<title>ToolNexa</title></head>" +
    "<body style='font-family:system-ui;padding:32px;max-width:560px;margin:auto'>" +
    "<h1>Pagamento recebido</h1>" +
    "<p>Volta ao ToolNexa para concluir a ativação do teu plano.</p>" +
    link +
    "</body></html>",
    {
      headers: {
        "content-type":
          "text/html; charset=utf-8",
        "cache-control":
          "no-store"
      }
    }
  );
}

function paypalCancelPage() {
  return new Response(
    "<!doctype html>" +
    "<html lang='pt'><head>" +
    "<meta charset='utf-8'>" +
    "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
    "<title>ToolNexa</title></head>" +
    "<body style='font-family:system-ui;padding:32px;max-width:560px;margin:auto'>" +
    "<h1>Assinatura cancelada</h1>" +
    "<p>Nenhuma nova assinatura foi ativada.</p>" +
    "<a href='toolnexa://paypal/cancel' style='display:inline-block;padding:14px 18px;" +
    "border-radius:12px;background:#2563eb;color:#fff;text-decoration:none'>" +
    "Voltar ao ToolNexa</a>" +
    "</body></html>",
    {
      headers: {
        "content-type":
          "text/html; charset=utf-8",
        "cache-control":
          "no-store"
      }
    }
  );
}

async function requireFirebaseUser(
  request,
  env
) {
  const header =
    request.headers.get(
      "authorization"
    ) || "";

  if (
    !header.startsWith(
      "Bearer "
    )
  ) {
    const error =
      new Error(
        "Authentication required."
      );
    error.code =
      "auth_required";
    error.publicMessage =
      "Inicie sessão no ToolNexa para usar esta função.";
    error.status = 401;
    throw error;
  }

  const token =
    header.slice(7).trim();

  if (!token) {
    const error =
      new Error(
        "Empty token."
      );
    error.code =
      "auth_required";
    error.status = 401;
    throw error;
  }

  const payload =
    await verifyFirebaseToken(
      token,
      env.FIREBASE_PROJECT_ID
    );

  return {
    uid:
      payload.sub,
    email:
      payload.email ||
      null
  };
}

async function verifyFirebaseToken(
  token,
  projectId
) {
  if (!projectId) {
    const error =
      new Error(
        "FIREBASE_PROJECT_ID is missing."
      );
    error.code =
      "firebase_project_missing";
    error.status = 500;
    throw error;
  }

  const parts =
    token.split(".");

  if (
    parts.length !== 3
  ) {
    return authInvalid();
  }

  let header;
  let payload;

  try {
    header =
      JSON.parse(
        decodeBase64Url(
          parts[0]
        )
      );
    payload =
      JSON.parse(
        decodeBase64Url(
          parts[1]
        )
      );
  } catch (_) {
    return authInvalid();
  }

  if (
    header.alg !==
      "RS256" ||
    !header.kid
  ) {
    return authInvalid();
  }

  const now =
    Math.floor(
      Date.now() /
        1000
    );

  if (
    !payload.sub ||
    payload.aud !==
      projectId ||
    payload.iss !==
      "https://securetoken.google.com/" +
        projectId ||
    typeof payload.exp !==
      "number" ||
    payload.exp <= now ||
    typeof payload.iat !==
      "number" ||
    payload.iat >
      now + 300
  ) {
    return authInvalid();
  }

  const keys =
    await firebaseJwks();

  let jwk =
    keys.find(
      key =>
        key.kid ===
        header.kid
    );

  if (!jwk) {
    firebaseKeysCache =
      null;

    const refreshed =
      await firebaseJwks(
        true
      );

    jwk =
      refreshed.find(
        key =>
          key.kid ===
          header.kid
      );
  }

  if (!jwk) {
    return authInvalid();
  }

  return verifyJwtSignature(
    jwk,
    parts,
    payload
  );
}

async function verifyJwtSignature(
  jwk,
  parts,
  payload
) {
  const key =
    await crypto.subtle
      .importKey(
        "jwk",
        jwk,
        {
          name:
            "RSASSA-PKCS1-v1_5",
          hash:
            "SHA-256"
        },
        false,
        ["verify"]
      );

  const signature =
    bytesFromBase64Url(
      parts[2]
    );

  const data =
    new TextEncoder()
      .encode(
        parts[0] +
          "." +
          parts[1]
      );

  const valid =
    await crypto.subtle.verify(
      {
        name:
          "RSASSA-PKCS1-v1_5"
      },
      key,
      signature,
      data
    );

  if (!valid) {
    return authInvalid();
  }

  return payload;
}

async function firebaseJwks(
  force = false
) {
  const now =
    Date.now();

  if (
    !force &&
    firebaseKeysCache &&
    now -
      firebaseKeysFetchedAt <
      6 * 60 * 60 * 1000
  ) {
    return firebaseKeysCache;
  }

  const response =
    await fetch(
      "https://www.googleapis.com/service_accounts/v1/jwk/" +
      "securetoken@system.gserviceaccount.com"
    );

  if (!response.ok) {
    const error =
      new Error(
        "Firebase public keys unavailable."
      );
    error.code =
      "firebase_keys_unavailable";
    error.status = 503;
    throw error;
  }

  const payload =
    await response.json();

  firebaseKeysCache =
    payload.keys ||
    [];

  firebaseKeysFetchedAt =
    now;

  return firebaseKeysCache;
}

function authInvalid() {
  const error =
    new Error(
      "Invalid Firebase ID token."
    );

  error.code =
    "auth_invalid";

  error.publicMessage =
    "A sessão do ToolNexa expirou ou é inválida.";

  error.status = 401;

  throw error;
}

function decodeBase64Url(
  value
) {
  return new TextDecoder()
    .decode(
      bytesFromBase64Url(
        value
      )
    );
}

function bytesFromBase64Url(
  value
) {
  const normalized =
    value
      .replaceAll(
        "-",
        "+"
      )
      .replaceAll(
        "_",
        "/"
      );

  const padded =
    normalized +
    "=".repeat(
      (4 -
        normalized.length %
          4) %
        4
    );

  const raw =
    atob(padded);

  const bytes =
    new Uint8Array(
      raw.length
    );

  for (
    let i = 0;
    i < raw.length;
    i++
  ) {
    bytes[i] =
      raw.charCodeAt(i);
  }

  return bytes;
}
