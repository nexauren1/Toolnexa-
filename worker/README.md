# ToolNexa Cloudflare Worker

The native Android app talks to this Worker API.

## Bindings

- AI — Workers AI binding for future ToolNexa AI tools.
- IMAGES — Cloudflare Images binding. Background removal uses
  segment=foreground, powered by Cloudflare BiRefNet through Workers AI.
- DB — D1 database used only for billing and PayPal records.

## Existing Cloudflare secrets

- PAYPAL_CLIENT_ID
- PAYPAL_CLIENT_SECRET

## Optional

- PAYPAL_PRO_PLAN_ID — PayPal Sandbox subscription Plan ID for the Pro plan.

The Worker automatically creates the D1 billing tables and seeds the Free and
Pro plans the first time /api/plans or a PayPal route is called.

No user table and no credit system are created here.
