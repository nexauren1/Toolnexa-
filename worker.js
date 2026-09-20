const JSON_HEADERS = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store"
};

const cors = {
  "access-control-allow-origin": "*",
  "access-control-allow-methods": "GET,POST,OPTIONS",
  "access-control-allow-headers": "content-type,x-toolnexa-version"
};

function json(data, status = 200, extra = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      ...JSON_HEADERS,
      ...cors,
      ...extra
    }
  });
}

async function ensurePlans(env) {
  await env.DB.batch([
    env.DB.prepare(
      `CREATE TABLE IF NOT EXISTS plans (
        id TEXT PRIMARY KEY,
        name TEXT NOT NULL,
        price_usd REAL NOT NULL DEFAULT 0,
        interval TEXT NOT NULL,
        active INTEGER NOT NULL DEFAULT 1,
        created_at INTEGER NOT NULL
      )`
    ),
    env.DB.prepare(
      `CREATE TABLE IF NOT EXISTS paypal_orders (
        id TEXT PRIMARY KEY,
        plan_id TEXT NOT NULL,
        paypal_order_id TEXT UNIQUE NOT NULL,
        status TEXT NOT NULL,
        amount_usd REAL NOT NULL,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
      )`
    ),
    env.DB.prepare(
      `CREATE TABLE IF NOT EXISTS paypal_subscriptions (
        id TEXT PRIMARY KEY,
        plan_id TEXT NOT NULL,
        paypal_subscription_id TEXT UNIQUE NOT NULL,
        status TEXT NOT NULL,
        created_at INTEGER NOT NULL,
        updated_at INTEGER NOT NULL
      )`
    ),
    env.DB.prepare(
      `INSERT OR IGNORE INTO plans
        (id, name, price_usd, interval, active, created_at)
        VALUES (?, ?, ?, ?, 1, ?)`
    ).bind(
      "free",
      "Free",
      0,
      "month",
      Date.now()
    ),
    env.DB.prepare(
      `INSERT OR IGNORE INTO plans
        (id, name, price_usd, interval, active, created_at)
        VALUES (?, ?, ?, ?, 1, ?)`
    ).bind(
      "pro",
      "Pro",
      5,
      "month",
      Date.now()
    )
  ]);
}

async function paypalToken(env) {
  const credentials = btoa(
    env.PAYPAL_CLIENT_ID +
    ":" +
    env.PAYPAL_CLIENT_SECRET
  );

  const response = await fetch(
    "https://api-m.sandbox.paypal.com/v1/oauth2/token",
    {
      method: "POST",
      headers: {
        "authorization": "Basic " + credentials,
        "content-type": "application/x-www-form-urlencoded"
      },
      body: "grant_type=client_credentials"
    }
  );

  if (!response.ok) {
    throw new Error("PayPal authentication failed");
  }

  const data = await response.json();
  return data.access_token;
}

async function createPaypalOrder(env, request) {
  const body = await request.json();
  const planId = body.plan_id || "pro";

  if (planId !== "pro") {
    return json(
      { error: "Only the Pro plan is currently paid." },
      400
    );
  }

  const token = await paypalToken(env);

  const response = await fetch(
    "https://api-m.sandbox.paypal.com/v2/checkout/orders",
    {
      method: "POST",
      headers: {
        "authorization": "Bearer " + token,
        "content-type": "application/json"
      },
      body: JSON.stringify({
        intent: "CAPTURE",
        purchase_units: [
          {
            reference_id: "toolnexa-pro",
            description: "ToolNexa Pro",
            amount: {
              currency_code: "USD",
              value: "5.00"
            }
          }
        ],
        application_context: {
          brand_name: "ToolNexa",
          user_action: "PAY_NOW",
          return_url: "toolnexa://paypal/success",
          cancel_url: "toolnexa://paypal/cancel"
        }
      })
    }
  );

  const data = await response.json();

  if (!response.ok) {
    return json(
      {
        error: "PayPal could not create the order.",
        details: data
      },
      502
    );
  }

  const approval = data.links?.find(
    link => link.rel === "approve"
  );

  await env.DB.prepare(
    `INSERT OR REPLACE INTO paypal_orders
      (id, plan_id, paypal_order_id, status, amount_usd,
       created_at, updated_at)
      VALUES (?, ?, ?, ?, ?, ?, ?)`
  ).bind(
    crypto.randomUUID(),
    "pro",
    data.id,
    data.status || "CREATED",
    5,
    Date.now(),
    Date.now()
  ).run();

  return json({
    order_id: data.id,
    status: data.status,
    approval_url: approval?.href || null
  });
}

async function capturePaypalOrder(env, request) {
  const body = await request.json();
  const orderId = String(body.order_id || "");

  if (!orderId) {
    return json(
      { error: "order_id is required." },
      400
    );
  }

  const token = await paypalToken(env);

  const response = await fetch(
    "https://api-m.sandbox.paypal.com/v2/checkout/orders/" +
      encodeURIComponent(orderId) +
      "/capture",
    {
      method: "POST",
      headers: {
        "authorization": "Bearer " + token,
        "content-type": "application/json"
      }
    }
  );

  const data = await response.json();

  if (!response.ok) {
    return json(
      {
        error: "PayPal could not capture the order.",
        details: data
      },
      502
    );
  }

  await env.DB.prepare(
    `UPDATE paypal_orders
     SET status = ?, updated_at = ?
     WHERE paypal_order_id = ?`
  ).bind(
    data.status || "COMPLETED",
    Date.now(),
    orderId
  ).run();

  return json({
    order_id: orderId,
    status: data.status
  });
}

async function removeBackground(env, request) {
  const contentType =
    request.headers.get("content-type") || "";

  if (!contentType.startsWith("image/")) {
    return json(
      {
        error:
          "Send the image as the raw request body with an image/* content type."
      },
      415
    );
  }

  if (!request.body) {
    return json(
      { error: "Image body is missing." },
      400
    );
  }

  const contentLength = Number(
    request.headers.get("content-length") || "0"
  );

  if (contentLength > 20 * 1024 * 1024) {
    return json(
      { error: "Maximum image size is 20 MB." },
      413
    );
  }

  try {
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
        ...cors,
        "cache-control": "no-store",
        "content-disposition":
          "inline; filename=\"toolnexa-background-removed.png\""
      }
    });
  } catch (error) {
    console.error(
      "background_remover_failed",
      error
    );

    return json(
      {
        error:
          "Background removal failed. Please try another image."
      },
      500
    );
  }
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: cors
      });
    }

    const url = new URL(request.url);

    try {
      if (url.pathname === "/api/health") {
        await ensurePlans(env);

        return json({
          ok: true,
          service: "toolnexa",
          version: "1.8.0",
          ai: "cloudflare-workers-ai",
          background_removal: "birefnet"
        });
      }

      if (url.pathname === "/api/plans") {
        await ensurePlans(env);

        const result = await env.DB.prepare(
          "SELECT id, name, price_usd, interval, active FROM plans WHERE active = 1 ORDER BY price_usd"
        ).all();

        return json({
          plans: result.results
        });
      }

      if (
        url.pathname ===
        "/api/tools/background-remover"
      ) {
        if (request.method !== "POST") {
          return json(
            { error: "Method not allowed." },
            405
          );
        }

        return removeBackground(
          env,
          request
        );
      }

      if (
        url.pathname ===
        "/api/paypal/create-order"
      ) {
        if (request.method !== "POST") {
          return json(
            { error: "Method not allowed." },
            405
          );
        }

        await ensurePlans(env);
        return createPaypalOrder(
          env,
          request
        );
      }

      if (
        url.pathname ===
        "/api/paypal/capture-order"
      ) {
        if (request.method !== "POST") {
          return json(
            { error: "Method not allowed." },
            405
          );
        }

        await ensurePlans(env);
        return capturePaypalOrder(
          env,
          request
        );
      }

      return json(
        {
          error: "Not found",
          service: "ToolNexa API"
        },
        404
      );
    } catch (error) {
      console.error(
        "worker_request_failed",
        error
      );

      return json(
        {
          error: "Internal server error."
        },
        500
      );
    }
  }
};
