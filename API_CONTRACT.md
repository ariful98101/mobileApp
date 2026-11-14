# MFS SMS Webhook API Contract (WordPress plugin <-> Android app)

This is the shared contract. The WordPress plugin implements the server side exactly as
specified here; the Android app calls it exactly as specified here. Do not change field
names or auth scheme independently on either side.

## Auth

Every device is issued a per-device API key by the WP admin (generated in the plugin's
"Devices" admin screen). The Android app stores: `server_base_url`, `api_key`, `device_id`
(a UUID generated once on first launch and persisted).

All requests send header:
```
X-MFS-API-Key: <the device's api key>
X-MFS-Device-Id: <the device's uuid>
Content-Type: application/json
```

## 1. Connection test

`GET {server_base_url}/wp-json/mfs/v1/ping`

Headers: as above.

Response 200:
```json
{ "success": true, "device_label": "Shop Counter Phone", "server_time": "2026-11-14T10:00:00+06:00" }
```

Response 401/403 on bad/revoked key:
```json
{ "success": false, "error_code": "invalid_api_key", "message": "..." }
```

## 2. Submit an SMS

`POST {server_base_url}/wp-json/mfs/v1/sms-webhook`

Body:
```json
{
  "method": "bkash",
  "sender": "16247",
  "customer_phone": "01812345678",
  "trx_id": "9H7A6B5C4D",
  "amount": 500.00,
  "raw_message": "You have received Tk 500.00 from 01812345678. TrxID 9H7A6B5C4D at 14/11/2026 09:45",
  "received_at": "2026-11-14T09:45:03+06:00",
  "client_submission_id": "local-room-row-uuid-or-int"
}
```

Field notes:
- `method`: one of `bkash`, `rocket`, `nagad` (lowercase).
- `sender`: the SMS sender ID/shortcode as read from the SMS.
- `customer_phone`: extracted from the SMS text if present (bKash/Nagad personal-number
  received-money SMS include the payer's number); null if not extractable.
- `trx_id`: extracted transaction/reference ID. Required — this is half of the match key.
- `amount`: extracted numeric amount if parseable, else null.
- `raw_message`: full original SMS body, unmodified (server keeps this for manual review/audit).
- `received_at`: ISO8601 with offset, device-local time SMS arrived.
- `client_submission_id`: any string the app uses locally to correlate the row in its own
  Room DB with the server's response, for updating local status. Echoed back unchanged.

Response 200 (always 200 if the payload was accepted and stored, matched or not):
```json
{
  "success": true,
  "client_submission_id": "local-room-row-uuid-or-int",
  "matched": true,
  "payment_id": 123,
  "order_id": 456,
  "message": "Matched to order #456 and moved to on-hold."
}
```
or, if no matching pending payment exists yet (still stored for the 2-min cron to retry):
```json
{
  "success": true,
  "client_submission_id": "...",
  "matched": false,
  "payment_id": null,
  "order_id": null,
  "message": "Stored. No pending payment matched this TrxID yet; will keep retrying."
}
```

Response 4xx (validation/auth failure — app marks the row FAILED and shows Retry):
```json
{ "success": false, "client_submission_id": "...", "error_code": "missing_trx_id", "message": "..." }
```

Idempotency: the server treats `(method, trx_id)` as a dedupe key. Re-submitting the same
trx_id (e.g. after a retry that actually succeeded server-side but the response was lost)
must not create a duplicate SMS log row or double-match — the server returns the same
result as the original successful submission.

## 3. Matching semantics (server-side, for both the webhook and the 2-min cron)

A "pending payment" is created when a customer submits phone+TrxID on the checkout
verification page. It lives in the plugin's own custom DB table and is **not** a
WooCommerce order status change yet — the WooCommerce order stays in a quiet
pre-payment status that normal WooCommerce admin screens / order emails / stock
reduction do not act on.

A match occurs when a pending payment's `(method, trx_id)` — and, where available,
`customer_phone` — equals an SMS log row's `(method, trx_id[, sender phone])`.

On match:
1. Mark the pending payment row `status = matched`, store the linked SMS row id.
2. Move the WooCommerce order to `on-hold` (this is the only point at which the order
   becomes visible/actionable in standard WooCommerce admin/order list/emails).
3. Record trx_id + matched SMS raw text as order meta for audit.

The 2-minute WP-Cron event re-runs this matching sweep over all still-pending rows (covers
the case where the SMS arrives before the customer finishes typing their TrxID, or a
webhook call was dropped). The webhook call above also attempts an immediate match so the
customer's checkout page updates fast instead of waiting up to 2 minutes.

Pending rows older than a configurable timeout (default 60 minutes) are marked `expired`
and the WooCommerce order is cancelled.

## 4. Error codes (non-exhaustive, extend as needed but keep both sides in sync)

- `invalid_api_key`, `missing_api_key`, `device_revoked`
- `missing_trx_id`, `invalid_method`, `invalid_payload`
- `rate_limited`
