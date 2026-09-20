const PAYPAL_BASE =
  "https://api-m.sandbox.paypal.com";

const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store"
};

let billingSchemaPromise = null;
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
          paypal_plan_configured:
            !!(
              env.PAYPAL_PRO_PLAN_ID
            ),
          paypal_webhook_configured:
            !!(
              env.PAYPAL_WEBHOOK_ID
            ),
          timestamp: new Date().toISOString()
        });
      }

      if (
        url.pathname === "/api/plans" &&
        request.method === "GET"
      ) {
        await ensureBillingSchema(env.DB);

        const result = await env.DB
          .prepare(
            "SELECT code, name, price_usd, " +
            "billing_interval, paypal_plan_id, active " +
            "FROM plans WHERE active = 1 " +
            "ORDER BY sort_order"
          )
          .all();

        return json({
          ok: true,
          plans: result.results || []
        });
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

        return accountStatus(
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
        url.pathname === "/api/paypal/return" &&
        request.method === "GET"
      ) {
        return paypalReturnPage(url);
      }

      if (
        url.pathname === "/api/paypal/cancel" &&
        request.method === "GET"
      ) {
        return paypalCancelPage();
      }

      if (
        url.pathname === "/api/paypal/webhook" &&
        request.method === "POST"
      ) {
        await ensureBillingSchema(
          env.DB
        );

        return recordWebhook(
          request,
          env
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
  if (!billingSchemaPromise) {
    billingSchemaPromise = db.batch([
      db.prepare(
        "CREATE TABLE IF NOT EXISTS plans (" +
        "code TEXT PRIMARY KEY, name TEXT NOT NULL, " +
        "price_usd TEXT NOT NULL, billing_interval TEXT NOT NULL, " +
        "paypal_plan_id TEXT, active INTEGER NOT NULL DEFAULT 1, " +
        "sort_order INTEGER NOT NULL DEFAULT 0)"
      ),
      db.prepare(
        "CREATE TABLE IF NOT EXISTS paypal_orders (" +
        "paypal_order_id TEXT PRIMARY KEY, plan_code TEXT, " +
        "status TEXT, amount_usd TEXT, " +
        "currency TEXT NOT NULL DEFAULT 'USD', " +
        "created_at TEXT NOT NULL, updated_at TEXT NOT NULL, " +
        "raw_json TEXT)"
      ),
      db.prepare(
        "CREATE TABLE IF NOT EXISTS paypal_subscriptions (" +
        "paypal_subscription_id TEXT PRIMARY KEY, " +
        "plan_code TEXT NOT NULL, firebase_uid TEXT, " +
        "status TEXT, payer_email TEXT, created_at TEXT NOT NULL, " +
        "updated_at TEXT NOT NULL, raw_json TEXT)"
      ),
      db.prepare(
        "CREATE TABLE IF NOT EXISTS paypal_webhook_events (" +
        "event_id TEXT PRIMARY KEY, event_type TEXT NOT NULL, " +
        "verified INTEGER NOT NULL DEFAULT 0, " +
        "received_at TEXT NOT NULL, raw_json TEXT NOT NULL)"
      ),
      db.prepare(
        "INSERT OR IGNORE INTO plans " +
        "(code,name,price_usd,billing_interval,paypal_plan_id,active,sort_order) " +
        "VALUES ('free','Free','0.00','month',NULL,1,1)"
      ),
      db.prepare(
        "INSERT OR IGNORE INTO plans " +
        "(code,name,price_usd,billing_interval,paypal_plan_id,active,sort_order) " +
        "VALUES ('pro','Pro','5.00','month',NULL,1,2)"
      )
    ]).catch(error => {
      billingSchemaPromise = null;
      throw error;
    });
  }

  return billingSchemaPromise;
}

async function backgroundRemover(
  request,
  env
) {
  if (!env.IMAGES) {
    return json(
      {
        ok: false,
        error: "images_binding_missing"
      },
      503
    );
  }

  const contentType =
    request.headers.get("content-type") ||
    "";

  if (!contentType.startsWith("image/")) {
    return json(
      {
        ok: false,
        error: "invalid_image_type",
        message:
          "Envie uma imagem JPG, PNG, WebP ou outro formato compatível."
      },
      415
    );
  }

  const length =
    Number(
      request.headers.get(
        "content-length"
      ) || "0"
    );

  if (
    length > 20 * 1024 * 1024
  ) {
    return json(
      {
        ok: false,
        error: "image_too_large",
        message:
          "A imagem deve ter no máximo 20 MB."
      },
      413
    );
  }

  if (!request.body) {
    return json(
      {
        ok: false,
        error: "empty_body"
      },
      400
    );
  }

  const result =
    await env.IMAGES
      .input(request.body)
      .transform({
        segment: "foreground"
      })
      .output({
        format: "image/png"
      });

  return result.response({
    headers: {
      "content-type": "image/png",
      "content-disposition":
        "attachment; filename=\"toolnexa-background-removed.png\"",
      "cache-control": "no-store",
      "x-toolnexa-engine":
        "Cloudflare Images / BiRefNet",
      ...corsHeaders()
    }
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

  const plan =
    await env.DB
      .prepare(
        "SELECT code, name, price_usd, paypal_plan_id " +
        "FROM plans WHERE code = 'pro' AND active = 1"
      )
      .first();

  const paypalPlanId =
    env.PAYPAL_PRO_PLAN_ID ||
    plan?.paypal_plan_id;

  if (!paypalPlanId) {
    return json(
      {
        ok: false,
        error:
          "paypal_plan_not_configured",
        message:
          "O plano Pro do PayPal Sandbox ainda precisa do seu PayPal Plan ID."
      },
      503
    );
  }

  const accessToken =
    await paypalAccessToken(
      env.PAYPAL_CLIENT_ID,
      env.PAYPAL_CLIENT_SECRET
    );

  const response =
    await fetch(
      PAYPAL_BASE +
        "/v1/billing/subscriptions",
      {
        method: "POST",
        headers: {
          "accept": "application/json",
          "content-type":
            "application/json",
          "authorization":
            "Bearer " +
            accessToken,
          "PayPal-Request-Id":
            crypto.randomUUID()
        },
        body: JSON.stringify({
          plan_id:
            paypalPlanId,
          custom_id:
            user.uid,
          application_context: {
            brand_name:
              "ToolNexa",
            user_action:
              "SUBSCRIBE_NOW",
            return_url:
              new URL(
                "/api/paypal/return",
                request.url
              ).toString(),
            cancel_url:
              new URL(
                "/api/paypal/cancel",
                request.url
              ).toString()
          }
        })
      }
    );

  const payload =
    await response
      .json()
      .catch(
        () => ({})
      );

  if (!response.ok) {
    return json(
      {
        ok: false,
        error:
          "paypal_create_subscription_failed",
        status:
          response.status,
        details:
          payload
      },
      502
    );
  }

  const approvalUrl =
    (payload.links || [])
      .find(
        link =>
          link.rel === "approve"
      )
      ?.href || null;

  const now =
    new Date().toISOString();

  await env.DB
    .prepare(
      "INSERT OR REPLACE INTO paypal_subscriptions " +
      "(paypal_subscription_id, plan_code, firebase_uid, status, " +
      "payer_email, created_at, updated_at, raw_json) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
    )
    .bind(
      payload.id,
      "pro",
      user.uid,
      payload.status ||
        "APPROVAL_PENDING",
      null,
      now,
      now,
      JSON.stringify(payload)
    )
    .run();

  return json({
    ok: true,
    subscription_id:
      payload.id,
    status:
      payload.status,
    approval_url:
      approvalUrl
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
        "PayPal Sandbox secrets are not configured."
      );

    error.code =
      "paypal_secrets_missing";

    error.publicMessage =
      "PayPal Sandbox não está configurado no Worker.";

    error.status = 503;

    throw error;
  }

  const encoded =
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
        method: "POST",
        headers: {
          "accept":
            "application/json",
          "accept-language":
            "en_US",
          "content-type":
            "application/x-www-form-urlencoded",
          "authorization":
            "Basic " +
            encoded
        },
        body:
          "grant_type=client_credentials"
      }
    );

  const payload =
    await response
      .json()
      .catch(
        () => ({})
      );

  if (!response.ok) {
    const error =
      new Error(
        "PayPal token request failed."
      );

    error.code =
      "paypal_token_failed";

    error.publicMessage =
      "O PayPal Sandbox recusou a autenticação.";

    error.status = 502;

    console.error(
      "PayPal token error",
      response.status,
      payload
    );

    throw error;
  }

  return payload.access_token;
}

async function recordWebhook(
  request,
  env
) {
  const raw =
    await request.text();

  let payload = {};

  try {
    payload =
      JSON.parse(raw);
  } catch (_) {
    return json(
      {
        ok: false,
        error: "invalid_json"
      },
      400
    );
  }

  const verified =
    await verifyPaypalWebhook(
      request,
      env,
      payload
    );

  const eventId =
    payload.id ||
    crypto.randomUUID();

  const receivedAt =
    new Date().toISOString();

  await env.DB
    .prepare(
      "INSERT OR IGNORE INTO paypal_webhook_events " +
      "(event_id, event_type, verified, received_at, raw_json) " +
      "VALUES (?, ?, ?, ?, ?)"
    )
    .bind(
      eventId,
      payload.event_type ||
        "UNKNOWN",
      verified ? 1 : 0,
      receivedAt,
      raw
    )
    .run();

  if (!verified) {
    return json(
      {
        ok: false,
        error:
          "paypal_webhook_unverified"
      },
      400
    );
  }

  await applyPaypalWebhook(
    env.DB,
    payload
  );

  return json({
    ok: true,
    received: true,
    verified: true
  });
}

async function verifyPaypalWebhook(
  request,
  env,
  payload
) {
  const webhookId =
    env.PAYPAL_WEBHOOK_ID;

  if (!webhookId) {
    const error =
      new Error(
        "PAYPAL_WEBHOOK_ID is missing."
      );

    error.code =
      "paypal_webhook_id_missing";

    error.publicMessage =
      "O Webhook ID do PayPal ainda não está configurado no Worker.";

    error.status = 503;

    throw error;
  }

  const headers = request.headers;

  const transmissionId =
    headers.get(
      "paypal-transmission-id"
    );

  const transmissionTime =
    headers.get(
      "paypal-transmission-time"
    );

  const certUrl =
    headers.get(
      "paypal-cert-url"
    );

  const authAlgo =
    headers.get(
      "paypal-auth-algo"
    );

  const transmissionSig =
    headers.get(
      "paypal-transmission-sig"
    );

  if (
    !transmissionId ||
    !transmissionTime ||
    !certUrl ||
    !authAlgo ||
    !transmissionSig
  ) {
    return false;
  }

  const accessToken =
    await paypalAccessToken(
      env.PAYPAL_CLIENT_ID,
      env.PAYPAL_CLIENT_SECRET
    );

  const response =
    await fetch(
      PAYPAL_BASE +
        "/v1/notifications/" +
        "verify-webhook-signature",
      {
        method: "POST",
        headers: {
          "accept":
            "application/json",
          "content-type":
            "application/json",
          "authorization":
            "Bearer " +
            accessToken
        },
        body: JSON.stringify({
          transmission_id:
            transmissionId,
          transmission_time:
            transmissionTime,
          cert_url:
            certUrl,
          auth_algo:
            authAlgo,
          transmission_sig:
            transmissionSig,
          webhook_id:
            webhookId,
          webhook_event:
            payload
        })
      }
    );

  const result =
    await response
      .json()
      .catch(
        () => ({})
      );

  if (!response.ok) {
    console.error(
      "PayPal webhook verification failed",
      response.status,
      result
    );

    return false;
  }

  return (
    result.verification_status ===
    "SUCCESS"
  );
}

async function applyPaypalWebhook(
  db,
  payload
) {
  const type =
    String(
      payload.event_type ||
      ""
    );

  const resource =
    payload.resource ||
    {};

  const subscriptionId =
    resource.id ||
    resource.billing_agreement_id ||
    resource.subscription_id ||
    null;

  if (!subscriptionId) {
    return;
  }

  const status =
    webhookSubscriptionStatus(
      type,
      resource
    );

  if (!status) {
    return;
  }

  await db
    .prepare(
      "UPDATE paypal_subscriptions " +
      "SET status = ?, updated_at = ?, raw_json = ? " +
      "WHERE paypal_subscription_id = ?"
    )
    .bind(
      status,
      new Date().toISOString(),
      JSON.stringify(payload),
      subscriptionId
    )
    .run();
}

function webhookSubscriptionStatus(
  type,
  resource
) {
  if (
    type ===
      "BILLING.SUBSCRIPTION.ACTIVATED" ||
    type ===
      "BILLING.SUBSCRIPTION.RE-ACTIVATED"
  ) {
    return "ACTIVE";
  }

  if (
    type ===
      "BILLING.SUBSCRIPTION.CANCELLED"
  ) {
    return "CANCELLED";
  }

  if (
    type ===
      "BILLING.SUBSCRIPTION.SUSPENDED"
  ) {
    return "SUSPENDED";
  }

  if (
    type ===
      "BILLING.SUBSCRIPTION.EXPIRED"
  ) {
    return "EXPIRED";
  }

  if (
    type ===
      "BILLING.SUBSCRIPTION.PAYMENT.FAILED"
  ) {
    return "PAYMENT_FAILED";
  }

  if (
    type ===
      "BILLING.SUBSCRIPTION.UPDATED"
  ) {
    const resourceStatus =
      String(
        resource.status ||
        ""
      ).toUpperCase();

    if (
      resourceStatus ===
      "ACTIVE"
    ) {
      return "ACTIVE";
    }

    if (
      resourceStatus ===
      "CANCELLED"
    ) {
      return "CANCELLED";
    }

    if (
      resourceStatus ===
      "SUSPENDED"
    ) {
      return "SUSPENDED";
    }

    if (
      resourceStatus ===
      "EXPIRED"
    ) {
      return "EXPIRED";
    }
  }

  return null;
}

async function accountStatus(
  env,
  user
) {
  await ensureBillingSchema(
    env.DB
  );

  const subscription =
    await env.DB
      .prepare(
        "SELECT paypal_subscription_id, " +
        "plan_code, status, created_at, updated_at " +
        "FROM paypal_subscriptions " +
        "WHERE firebase_uid = ? " +
        "ORDER BY updated_at DESC " +
        "LIMIT 1"
      )
      .bind(
        user.uid
      )
      .first();

  const isPro =
    subscription?.status ===
    "ACTIVE";

  const code =
    isPro
      ? "pro"
      : "free";

  const plan =
    await env.DB
      .prepare(
        "SELECT code, name, price_usd, " +
        "billing_interval, active " +
        "FROM plans " +
        "WHERE code = ? LIMIT 1"
      )
      .bind(code)
      .first();

  return json({
    ok: true,
    account: {
      uid: user.uid,
      plan: plan || {
        code,
        name:
          isPro
            ? "Pro"
            : "Free",
        price_usd:
          isPro
            ? "5.00"
            : "0.00",
        billing_interval:
          "month",
        active: 1
      },
      subscription:
        subscription || null
    }
  });
}

function paypalReturnPage(
  url
) {
  const id =
    escapeHtml(
      url.searchParams.get(
        "subscription_id"
      ) || ""
    );

  const note = id
    ? "ID da subscrição: " +
      id
    : "A confirmação final deve ser validada pelo servidor.";

  return new Response(
    pageHtml(
      "Subscrição recebida",
      "O PayPal Sandbox devolveu o controlo ao ToolNexa.",
      note
    ),
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
    pageHtml(
      "Pagamento cancelado",
      "A subscrição Pro não foi concluída.",
      "Pode voltar ao ToolNexa e tentar novamente."
    ),
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

function pageHtml(
  title,
  subtitle,
  note
) {
  return "<!doctype html>" +
    "<html lang=\"pt\"><head>" +
    "<meta charset=\"utf-8\">" +
    "<meta name=\"viewport\" " +
    "content=\"width=device-width,initial-scale=1\">" +
    "<title>" +
    escapeHtml(title) +
    " · ToolNexa</title>" +
    "<style>" +
    "body{margin:0;background:#f4f7fb;" +
    "font-family:Inter,system-ui,sans-serif;color:#111827}" +
    "main{max-width:560px;margin:10vh auto;padding:24px}" +
    ".card{background:#fff;border:1px solid #d9e1ec;" +
    "border-radius:24px;padding:28px;box-shadow:0 16px 45px " +
    "rgba(16,24,40,.08)}" +
    "h1{margin:0 0 10px;font-size:30px}" +
    "p{color:#667085;line-height:1.6}" +
    ".brand{font-weight:800;color:#2563eb}" +
    "</style></head><body><main><section class=\"card\">" +
    "<div class=\"brand\">ToolNexa</div>" +
    "<h1>" +
    escapeHtml(title) +
    "</h1><p>" +
    escapeHtml(subtitle) +
    "</p><p>" +
    escapeHtml(note) +
    "</p></section></main></body></html>";
}

function escapeHtml(value) {
  return String(value)
    .replaceAll(
      "&",
      "&amp;"
    )
    .replaceAll(
      "<",
      "&lt;"
    )
    .replaceAll(
      ">",
      "&gt;"
    )
    .replaceAll(
      '"',
      "&quot;"
    )
    .replaceAll(
      "'",
      "&#39;"
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
